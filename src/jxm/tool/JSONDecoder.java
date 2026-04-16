/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.IOException;
import java.io.PrintStream;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jxm.*;
import jxm.xb.*;


/*
 * This class is written mostly based on the algorithms and information found from:
 *
 *     Writing a Simple JSON Parser from Scratch in C++
 *     Written by Kishore Ganesh (kishore2912000@gmail.com) on April 30, 2021
 *     https://kishoreganesh.com/post/writing-a-json-parser-in-cplusplus
 *
 * ~~~ Last accessed & checked on 2024-04-03 ~~~
 *
 * This class is written specifically to deserialize the JSON string from the 'JSONEncoderLite' class.
 */
public class JSONDecoder {

    @SuppressWarnings("serial")
    public static class JSONArray extends ArrayList<JSONValue> {}

    @SuppressWarnings("serial")
    public static class JSONObject extends HashMap<String, JSONValue> {}

    private static enum JSONValueType {
        _Boolean,
        _Long,
        _Double,
        _String,
        _Object,
        _Array,
        _Null
    }

    public static class JSONValue {

        public JSONValueType type;
        public Object        value;

        public JSONValue(JSONValueType type_, Object value_)
        {
            type  = type_;
            value = value_;
        }

        public boolean isBoolean() { return type  == JSONValueType._Boolean; }
        public boolean isLong   () { return type  == JSONValueType._Long;    }
        public boolean isDouble () { return type  == JSONValueType._Double;  }
        public boolean isNumber () { return isLong() || isDouble();          }
        public boolean isString () { return type  == JSONValueType._String;  }
        public boolean isObject () { return type  == JSONValueType._Object;  }
        public boolean isArray  () { return type  == JSONValueType._Array;   }
        public boolean isNull   () { return type  == JSONValueType._Null;    }

        public Boolean    getBoolean() { return isBoolean() ? ( (Boolean   ) value ) : null; }
        public Long       getLong   () { return isLong   () ? ( (Long      ) value ) : null; }
        public Double     getDouble () { return isDouble () ? ( (Double    ) value ) : null; }
        public String     getString () { return isString () ? ( (String    ) value ) : null; }
        public JSONObject getObject () { return isObject () ? ( (JSONObject) value ) : null; }
        public JSONArray  getArray  () { return isArray  () ? ( (JSONArray ) value ) : null; }

        public String getString(final boolean convertAsNeeded)
        {
            if( isString() ) return getString();

            if(!convertAsNeeded) return null;

            if( isBoolean() ) return String.valueOf( getBoolean() );
            if( isLong   () ) return String.valueOf( getLong   () );
            if( isDouble () ) return String.valueOf( getDouble () );

            return null;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private PrintStream _ps = null;

        private void _println()
        { _ps.println(); }

        private void _printf(final int level, final String format, final Object... args)
        {
            for(int i = 0; i < level; ++i) _ps.print("   ");

            _ps.printf(format, args);
        }

        private void _dump_impl(final PrintStream ps, final int level, final boolean afterKey)
        {
            _ps = ps;

            switch(type) {

                case _Object:
                    if(true) {
                        _printf(afterKey ? 0 : level, "{\n");
                        final Iterator< Map.Entry<String, JSONValue> > it = ( (JSONObject) value ).entrySet().iterator();
                        while( it.hasNext() ) {
                            final Map.Entry<String, JSONValue> entry = it.next();
                            _printf( level + 1, "%s = ", entry.getKey() );
                            entry.getValue()._dump_impl(_ps, level + 1, true);
                        }
                        _printf(level, "}\n");
                    }
                    break;

                case _Array:
                    if(true) {
                        _printf(afterKey ? 0 : level, "[\n");
                        for( final JSONValue it : (JSONArray) value ) {
                            it._dump_impl(_ps, level + 1, false);
                        }
                        _printf(level, "]\n");
                    }
                    break;

                case _Null    : _printf( afterKey ? 0 : level, "null\n"                    ); break;
                case _Boolean : _printf( afterKey ? 0 : level, "%b\n"    , (Boolean) value ); break;
                case _Long    : _printf( afterKey ? 0 : level, "%d\n"    , (Long   ) value ); break;
                case _Double  : _printf( afterKey ? 0 : level, "%f\n"    , (Double ) value ); break;
                case _String  : _printf( afterKey ? 0 : level, "\"%s\"\n", (String ) value ); break;

            } // switch

            _ps = null;
        }

        public void dump(final PrintStream ps)
        { _dump_impl(ps, 0, false); }

    } // class JSONValue

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static enum TokenType {
        CURLY_OPEN,
        CURLY_CLOSE,
        ARRAY_OPEN,
        ARRAY_CLOSE,
        COLON,
        COMMA,
        BOOLEAN,
        NUMBER,
        STRING,
        NULL
    }

    private static class Token {

        public TokenType type;
        public String    value;

        public String toString()
        {
            switch(type) {
                case NULL        : /* FALLTHROUGH */
                case CURLY_OPEN  : /* FALLTHROUGH */
                case CURLY_CLOSE : /* FALLTHROUGH */
                case ARRAY_CLOSE : /* FALLTHROUGH */
                case COLON       : /* FALLTHROUGH */
                case COMMA       : return type.name();
                case BOOLEAN     : /* FALLTHROUGH */
                case NUMBER      : /* FALLTHROUGH */
                case STRING      : return String.format( "%s[%s]", type.name(), value );
            }

            return "";
        }

    } // class Token

    private static class Tokenizer {

        private final char[] _stream;
        private       int    _curPos;

        public Tokenizer(final String jsonString)
        {
            _stream = jsonString.toCharArray();
            _curPos = 0;
        }

        private int _tellg()
        { return _curPos; }

        private void _seekg(final int newAbsPos)
        { _curPos = newAbsPos; }

        private boolean _eof()
        { return _curPos >= _stream.length; }

        private char _peekch()
        { return _stream[_curPos]; }

        private char _getch()
        { return _stream[_curPos++]; }

        private void _discardch(final int len)
        { _curPos += len; }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final StringBuilder _tokSB     = new StringBuilder();
        private       int           _tokPrvPos = 0;

        private char _getCharSkipWS() throws IOException
        {
            char ch = ' ';

            while(ch == ' ' || ch == '\n') {
                ch = _getch();
                if( _eof() ) {
                    if(ch == ' ' || ch == '\n') return ' ';
                    break;
                }
            }

            return ch;
        }

        private String _getString() throws IOException
        {
            _tokSB.setLength(0);

            boolean done = false;

            while(!done) {

                final char ch = _getch();

                switch(ch) {
                    case '\\' : _tokSB.append('\\');
                                _tokSB.append( _getch() );
                                break;
                    case '%'  : _tokSB.append("%%");
                                break;
                    case '"'  : done = true;
                                break;
                    default   : _tokSB.append(ch);
                                break;
                } // switch

            } // while

            return String.format( _tokSB.toString() );
        }

        private String _getNumber(final char fch) throws IOException
        {
            _tokSB.setLength(0);

            if(fch != '+') _tokSB.append(fch);

            boolean allowFlt = true;
            boolean allowHex = false;

            if(fch == '0') {
                final char nch = Character.toLowerCase( _peekch() );
                if(nch == 'b' || nch == 'x') {
                    _tokSB.append(nch);
                    _discardch(1);
                    allowFlt = false;
                    allowHex = (nch == 'x');
                }
            }

            boolean done = false;

            while(!done) {

                final int  prvPos = _tellg();
                final char ch     = _getch();
                final char uh     = Character.toUpperCase(ch);

                if(ch >= '0' && ch <= '9') {
                    _tokSB.append(ch);
                }
                else if( allowHex && (uh >= 'A' && uh <= 'F') ) {
                    _tokSB.append(uh);
                }
                else if( allowFlt && (uh == '.' || uh == 'E' || uh == '-' || uh == '+') ) {
                    _tokSB.append(uh);
                }
                else {
                    _seekg(prvPos);
                    break;
                }

            } // while

            return _tokSB.toString();
        }

        public void rollbackToken()
        { _seekg(_tokPrvPos); }

        public Token getToken() throws IOException
        {
            if( _eof() ) return null;

            _tokPrvPos = _tellg();
            final char ch = _getCharSkipWS();
            if(ch == ' ') return null;

            final Token token = new Token();

            switch(ch) {
                case '{' : token.type  = TokenType.CURLY_OPEN;
                           break;
                case '}' : token.type  = TokenType.CURLY_CLOSE;
                           break;
                case '[' : token.type  = TokenType.ARRAY_OPEN;
                           break;
                case ']' : token.type  = TokenType.ARRAY_CLOSE;
                           break;
                case ':' : token.type  = TokenType.COLON;
                           break;
                case ',' : token.type  = TokenType.COMMA;
                           break;
                case 'n' : token.type  = TokenType.NULL;
                           _discardch(3);
                           break;
                case 'f' : token.type  = TokenType.BOOLEAN;
                           token.value = "false";
                           _discardch(4);
                           break;
                case 't' : token.type  = TokenType.BOOLEAN;
                           token.value = "true";
                           _discardch(3);
                           break;
                case '"' : token.type  = TokenType.STRING;
                           token.value = _getString();
                           break;
                default  : if( ch == '-' || ch == '+' || (ch >= '0' && ch <= '9') ) {
                               token.type  = TokenType.NUMBER;
                               token.value = _getNumber(ch);
                           }
                           else {
                               throw XCom.newIOException(Texts.EMsg_UnexpectedToken, "" + ch);
                           }
                           break;
            } // switch

            return token;
        }

    } // class Tokenizer

    private static class Parser {

        private final Tokenizer _tokenizer;
        private       JSONValue _root;

        public Parser(final String jsonString)
        {
            _tokenizer = new Tokenizer(jsonString);
            _root      = null;
        }

        public JSONValue parse() throws IOException
        {
            while(true) {

                final Token token = _tokenizer.getToken();
                if(token == null) break;

                switch(token.type) {

                    case CURLY_OPEN:
                        if(_root == null) _root = _parseObject();
                        else              throw XCom.newIOException( Texts.EMsg_UnexpectedExtraToken, token.toString() );
                        break;

                    case ARRAY_OPEN:
                        if(_root == null) _root = _parseArray();
                        else              throw XCom.newIOException( Texts.EMsg_UnexpectedExtraToken, token.toString() );
                        break;

                    default:
                        throw XCom.newIOException( Texts.EMsg_UnexpectedToken, token.toString() );

                } // switch

            } // while

            return _root;
        }

        private JSONValue _parseObject() throws IOException
        {
            JSONObject object = new JSONObject();

            while(true) {

                final Token tokenKey = _tokenizer.getToken();
                if(tokenKey        == null                 ) throw XCom.newIOException( Texts.EMsg_PrematureEOF                         );
                if(tokenKey.type   == TokenType.CURLY_CLOSE) break; // Check if the object is empty
                if(tokenKey.type   != TokenType.STRING     ) throw XCom.newIOException( Texts.EMsg_UnexpectedToken, tokenKey.toString() );

                final Token tokenColon = _tokenizer.getToken();
                if(tokenColon      == null                 ) throw XCom.newIOException( Texts.EMsg_PrematureEOF                         );
                if(tokenColon.type != TokenType.COLON      ) throw XCom.newIOException( Texts.EMsg_UnexpectedToken, tokenKey.toString() );

                final Token tokenValue = _tokenizer.getToken();
                if(tokenValue      == null                 ) throw XCom.newIOException( Texts.EMsg_PrematureEOF                         );

                switch(tokenValue.type) {
                    case CURLY_OPEN : object.put( tokenKey.value, _parseObject () );
                                      break;
                    case ARRAY_OPEN : object.put( tokenKey.value, _parseArray  () );
                                      break;
                    case BOOLEAN    : _tokenizer.rollbackToken();
                                      object.put( tokenKey.value, _parseBoolean() );
                                      break;
                    case NUMBER     : _tokenizer.rollbackToken();
                                      object.put( tokenKey.value, _parseNumber () );
                                      break;
                    case STRING     : _tokenizer.rollbackToken();
                                      object.put( tokenKey.value, _parseString () );
                                      break;
                    case NULL       : _tokenizer.rollbackToken();
                                      object.put( tokenKey.value, _parseNull   () );
                                      break;
                    default         : throw XCom.newIOException( Texts.EMsg_UnexpectedToken, tokenValue.toString() );
                } // switch

                final Token chkTok = _tokenizer.getToken();

                if(chkTok == null) {
                    throw XCom.newIOException(Texts.EMsg_PrematureEOF);
                }
                else if(chkTok.type == TokenType.COMMA) {
                    if( _tokenizer.getToken() == null ) throw XCom.newIOException(Texts.EMsg_PrematureEOF);
                    _tokenizer.rollbackToken();
                    continue;
                }
                else if(chkTok.type == TokenType.CURLY_CLOSE) {
                    break;
                }
                else {
                    throw XCom.newIOException( Texts.EMsg_UnexpectedToken, chkTok.toString() );
                }

            } // while

            return new JSONValue(JSONValueType._Object, object);
        }

        private JSONValue _parseArray() throws IOException
        {
            JSONArray array = new JSONArray();

            while(true) {

                final Token token  = _tokenizer.getToken();

                if(token      == null                 ) throw XCom.newIOException(Texts.EMsg_PrematureEOF);
                if(token.type == TokenType.ARRAY_CLOSE) break; // Check if the array is empty

                switch(token.type) {
                    case CURLY_OPEN : array.add( _parseObject () );
                                      break;
                    case ARRAY_OPEN : array.add( _parseArray  () );
                                      break;
                    case BOOLEAN    : _tokenizer.rollbackToken();
                                      array.add( _parseBoolean() );
                                      break;
                    case NUMBER     : _tokenizer.rollbackToken();
                                      array.add( _parseNumber () );
                                      break;
                    case STRING     : _tokenizer.rollbackToken();
                                      array.add( _parseString () );
                                      break;
                    case NULL       : _tokenizer.rollbackToken();
                                      array.add( _parseNull   () );
                                      break;
                    default         : throw XCom.newIOException( Texts.EMsg_UnexpectedToken, token.toString() );
                } // switch

                final Token chkTok = _tokenizer.getToken();

                if(chkTok == null) {
                    throw XCom.newIOException(Texts.EMsg_PrematureEOF);
                }
                else if(chkTok.type == TokenType.COMMA) {
                    if( _tokenizer.getToken() == null ) throw XCom.newIOException(Texts.EMsg_PrematureEOF);
                    _tokenizer.rollbackToken();
                    continue;
                }
                else if(chkTok.type == TokenType.ARRAY_CLOSE) {
                    break;
                }
                else {
                    throw XCom.newIOException( Texts.EMsg_UnexpectedToken, chkTok.toString() );
                }

            } // while

            return new JSONValue(JSONValueType._Array, array);
        }

        private JSONValue _parseBoolean() throws IOException
        {
            final Token token = _tokenizer.getToken();

            if(token.type != TokenType.BOOLEAN) throw XCom.newIOException(Texts.EMsg_JDL_TokenNotBoolean);

            return new JSONValue( JSONValueType._Boolean, Boolean.parseBoolean(token.value) );
        }

        private JSONValue _parseNumber() throws IOException
        {
            final Token token = _tokenizer.getToken();

            if(token.type != TokenType.NUMBER) throw XCom.newIOException(Texts.EMsg_JDL_TokenNotNumber);

            final Object res = XCom.parseNumberStr(token.value);

            if(res instanceof Double) {
                return new JSONValue( JSONValueType._Double, (Double) res );
            }
            else {
                return new JSONValue( JSONValueType._Long  , (Long  ) res );
            }

            /*
            final String  tokVal = token.value.toUpperCase();
            final int     tokLen = token.value.length();
            final boolean isBin  = (tokLen > 2) &&                     tokVal.startsWith("0B");
            final boolean isHex  = (tokLen > 2) &&                     tokVal.startsWith("0X");
            final boolean isOct  = (tokLen > 1) && !isBin && !isHex && tokVal.startsWith("0" );

            if( tokVal.contains(".") || ( !isHex && tokVal.contains("E") ) ) {
                return new JSONValue( JSONValueType._Double, Double.valueOf( Double.parseDouble(tokVal) ) );
            }
            else  {
                final int     radix  = isBin ?  2
                                     : isOct ?  8
                                     : isHex ? 16
                                     :         10;
                      String  valStr = isBin ? tokVal.substring(2)
                                     : isOct ? tokVal.substring(1)
                                     : isHex ? tokVal.substring(2)
                                     :         tokVal;
                      long    value  = 0;
                      boolean isNeg  = false;


                if( isBin && valStr.length() == 64 && valStr.startsWith("1") ) {
                    valStr = '0' + valStr.substring(1);
                    isNeg  = true;
                }
                else if( isOct && valStr.length() == 22 && valStr.startsWith("1") ) {
                    valStr = '0' + valStr.substring(1);
                    isNeg  = true;
                }
                else if( isHex && valStr.length() == 16 && valStr.startsWith("F") ) {
                    valStr = '7' + valStr.substring(1);
                    isNeg  = true;
                }

                          value  = Long.parseLong(valStr, radix);
                if(isNeg) value += Long.MIN_VALUE;

                return new JSONValue( JSONValueType._Long, Long.valueOf(value) );
            }
            */
        }

        private JSONValue _parseString() throws IOException
        {
            final Token token = _tokenizer.getToken();

            if(token.type != TokenType.STRING) throw XCom.newIOException(Texts.EMsg_JDL_TokenNotString);

            return new JSONValue(JSONValueType._String, token.value);
        }

        private JSONValue _parseNull() throws IOException
        {
            final Token token = _tokenizer.getToken();

            if(token.type != TokenType.NULL) throw XCom.newIOException(Texts.EMsg_JDL_TokenNotNull);

            return new JSONValue(JSONValueType._Null, null);
        }

    } // class Parser

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static JSONValue decode(final String jsonString) throws IOException
    {
        final Parser parser = new Parser(jsonString);

        return parser.parse();
    }

    public synchronized static JSONValue decode(final Path jsonFilePath) throws IOException
    { return decode( SysUtil.readTextFileAsString(jsonFilePath) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static <A, T> void _deserializeArray_elements(final A targetArray, final JSONArray jsonArray, final Class<T> clazz) throws Exception
    {
        for(int i = 0; i < jsonArray.size(); ++i) {
            final JSONValue v = jsonArray.get(i);
            Array.set( targetArray, i, clazz.cast( clazz.getMethod("valueOf", String.class).invoke( null, v.getString(true) ) ) );
        }
    }

    private static <F, T> void _deserializeArray(final Field field, final F fieldObj, final JSONValue jsonValue) throws Exception
    {
        // Check if the JSON value really is an array
        if( !jsonValue.isArray() && !jsonValue.isNull() ) throw XCom.newJXMRuntimeError(Texts.EMsg_JDL_TypeNotArray);

        // Check if the array shall be set to null
        if( jsonValue.isNull() )  {
            field.set(fieldObj, null);
            return;
        }

        // Store the elements
        final String ftypeName = field.getType().getName();

        switch( ftypeName.substring(1) ) {

              case "Z" /* boolean */ : if(true) { final boolean[] array = new boolean[ jsonValue.getArray().size() ];
                                                  _deserializeArray_elements(array, jsonValue.getArray(), Boolean  .class);
                                                  field.set(fieldObj, array);
                                                } break;

              case "B" /* byte    */ : if(true) { final byte   [] array = new byte   [ jsonValue.getArray().size() ];
                                                  _deserializeArray_elements(array, jsonValue.getArray(), Byte     .class);
                                                  field.set(fieldObj, array);
                                                } break;

              case "S" /* short   */ : if(true) { final short  [] array = new short  [ jsonValue.getArray().size() ];
                                                  _deserializeArray_elements(array, jsonValue.getArray(), Short    .class);
                                                  field.set(fieldObj, array);
                                                } break;

              case "I" /* int     */ : if(true) { final int    [] array = new int    [ jsonValue.getArray().size() ];
                                                  _deserializeArray_elements(array, jsonValue.getArray(), Integer  .class);
                                                  field.set(fieldObj, array);
                                                } break;

              case "J" /* long    */ : if(true) { final long   [] array = new long   [ jsonValue.getArray().size() ];
                                                  _deserializeArray_elements(array, jsonValue.getArray(), Long     .class);
                                                  field.set(fieldObj, array);
                                                } break;

              case "F" /* float   */ : if(true) { final float  [] array = new float  [ jsonValue.getArray().size() ];
                                                  _deserializeArray_elements(array, jsonValue.getArray(), Float    .class);
                                                  field.set(fieldObj, array);
                                                } break;

              case "D" /* double  */ : if(true) { final double [] array = new double [ jsonValue.getArray().size() ];
                                                  _deserializeArray_elements(array, jsonValue.getArray(), Double   .class);
                                                  field.set(fieldObj, array);
                                                } break;

              case "C" /* char    */ : if(true) { final char   [] array = new char   [ jsonValue.getArray().size() ];
                                                  _deserializeArray_elements(array, jsonValue.getArray(), Character.class);
                                                  field.set(fieldObj, array);
                                                } break;

              default                : throw XCom.newJXMRuntimeError(Texts.EMsg_JDL_UnsupportedCls, ftypeName);

        } // switch
    }

    @SuppressWarnings("unchecked")
    private static <F, T> void _deserializeObject(final Field field, final F fieldObj, final T targetObj, final JSONValue jsonValue) throws Exception
    {
        // Check if the JSON value really is an object
        if( !jsonValue.isObject() && !jsonValue.isNull() ) throw XCom.newJXMRuntimeError(Texts.EMsg_JDL_TypeNotObject);

        // Check if the object shall be set to null
        if( jsonValue.isNull() )  {
            field.set(fieldObj, null);
            return;
        }

        // Get and check the object and target class names
        final JSONObject obj          = jsonValue.getObject();
        final String     objClassName = obj.get(JSONEncoderLite.CLASS_NAME_KEY).getString();
        final Class<?>   trgClass     = targetObj.getClass();
        final String     trgClassName = trgClass.getName();

        if( !trgClassName.equals(objClassName) ) {
            // Allow one subclassing from 'jxm.SerializableDeepClone'
            final String objSuperClassName      = Class.forName(objClassName     ).getSuperclass().getName();
            final String objSuperSuperClassName = Class.forName(objSuperClassName).getSuperclass().getName();
            if( !trgClassName.equals(objSuperClassName) || !"jxm.SerializableDeepClone".equals(objSuperSuperClassName) ) {
                throw XCom.newJXMRuntimeError(Texts.EMsg_JDL_MismatchedCls, objClassName, trgClassName);
            }
        }

        // Store the fields
        final Iterator< Map.Entry<String, JSONValue> > it = obj.entrySet().iterator();

        while( it.hasNext() ) {

            // Get the field name
            final Map.Entry<String, JSONValue> objEntry     = it.next();
            final String                       objFieldName = objEntry.getKey();
            final JSONValue                    objValue     = objEntry.getValue();

            // Skip if it is the CLASS_NAME_KEY
            if( JSONEncoderLite.CLASS_NAME_KEY.equals(objFieldName) ) continue;

            // Check if it is a method call of a specified class
            if( objFieldName.startsWith("@") && objFieldName.indexOf("#") > 0 ) {
                // Get the class name and method
                final String[] omStr      = objFieldName.substring(1).split("#");
                      String   className  = omStr[0];
                final String   methodName = omStr[1];
                // Get and check the serial version UID if it present
                final String[] svuidStr   = className.split("\\[" + SysUtil.JxMakeSerialVersionUID_FieldName + ":");
                if( svuidStr.length == 2 && svuidStr[1].endsWith("]") ) {
                    // Extract the class name
                    className = svuidStr[0];
                    // Extract and check the serial version UID
                    final long ref = Class.forName(objClassName).getField(SysUtil.JxMakeSerialVersionUID_FieldName).getLong(targetObj);
                    final long chk = Long.decode( svuidStr[1].substring( 0, svuidStr[1].length() - 1 ) );
                    if(ref != chk) throw XCom.newJXMRuntimeError( Texts.EMsg_JDL_MismatchedSVUID, chk, ref );
                }
                // Get the method and generic type
                final Method            m                 = Class.forName(className).getDeclaredMethod(methodName, Object.class);
                final ParameterizedType paramSuperClazz   = (ParameterizedType) trgClass.getGenericSuperclass();
                final Class<?>          genClazz          = (Class<?>         ) paramSuperClazz.getActualTypeArguments()[0];
                // Deserialize the elements
                final JSONArray jsonArray = objValue.getArray();
                for(int i = 0; i < jsonArray.size(); ++i) {
                    // Get the element
                    final JSONValue v = jsonArray.get(i);
                    // If the element is null, pass the null to the method
                    if( v.isNull() ) {
                        final Object NULL = null;
                        m.invoke(targetObj, NULL);
                    }
                    // Deserialize the element and pass the element to the method
                    else {
                        final Object t = genClazz.getDeclaredConstructor().newInstance();
                        _deserializeObject(null, null, t, v);
                        m.invoke(targetObj, t);
                    }
                }
                continue;
            }

            // Find the target field
            final Field trgField = trgClass.getField(objFieldName);
            if(trgField == null) throw XCom.newJXMRuntimeError(Texts.EMsg_JDL_FieldNotExist, objFieldName, trgClassName);

            // Check the serial version UID if it exists
            if( objFieldName.equals(SysUtil.JxMakeSerialVersionUID_FieldName) ) {
                if( objValue.getLong() != trgField.getLong(targetObj) ) {
                    throw XCom.newJXMRuntimeError( Texts.EMsg_JDL_MismatchedSVUID, objValue.getLong(), trgField.getLong(targetObj) );
                }
                continue;
            }

            // Get the target field class
            final Class<?> trgFieldType     = trgField.getType();
            final String   trgFieldTypeName = trgFieldType.getName();

            // Process enum
            if( trgFieldType.isEnum() ) {
                if( !objValue.isNull() ) trgField.set( targetObj, Enum.valueOf( (Class<Enum>) trgFieldType, objValue.getString() ) );
                else                     trgField.set( targetObj, null                                                             );
            }

            // Process array
            else if( trgFieldType.isArray() ) {
                _deserializeArray(trgField, targetObj, objValue);
            }

            // Process other types
            else {

                switch(trgFieldTypeName) {

                    case "java.lang.Boolean"       : /* FALLTHROUGH */
                    case "boolean"                 : trgField.setBoolean( targetObj, objValue.getBoolean()                       ); break;

                    case "java.lang.Byte"          : /* FALLTHROUGH */
                    case "byte"                    : trgField.setByte   ( targetObj, objValue.getLong   ().byteValue ()          ); break;

                    case "java.lang.Short"         : /* FALLTHROUGH */
                    case "short"                   : trgField.setShort  ( targetObj, objValue.getLong   ().shortValue()          ); break;

                    case "java.lang.Integer"       : /* FALLTHROUGH */
                    case "int"                     : trgField.setInt    ( targetObj, objValue.getLong   ().intValue  ()          ); break;

                    case "java.lang.Long"          : /* FALLTHROUGH */
                    case "long"                    : trgField.setLong   ( targetObj, objValue.getLong   ()                       ); break;

                    case "java.lang.Float"         : /* FALLTHROUGH */
                    case "float"                   : trgField.setFloat  ( targetObj, objValue.getDouble ().floatValue()          ); break;

                    case "java.lang.Double"        : /* FALLTHROUGH */
                    case "double"                  : trgField.setDouble ( targetObj, objValue.getDouble ()                       ); break;

                    case "java.lang.Character"     : /* FALLTHROUGH */
                    case "char"                    : trgField.setChar   ( targetObj, objValue.getString ().charAt(0)             ); break;

                    case "java.lang.String"        : trgField.set       ( targetObj, objValue.getString ()                       ); break;


                    case "java.util.regex.Pattern" : {
                                                         Object objRPattern = objValue.isNull()
                                                                            ? null
                                                                            : java.util.regex.Pattern.compile( objValue.getString () );
                                                         trgField.set       ( targetObj, objRPattern                                 );
                                                     }
                                                     break;

                    default                        : _deserializeObject ( trgField, targetObj, trgField.get(targetObj), objValue ); break;

                } // switch

            }

        } // while
    }

    public synchronized static <T> T deserialize(final String jsonString, final Class<T> rootType) throws Exception
    {
        // Decode the JSON first
        final JSONValue jsonValue = decode(jsonString);

        // Check if the root element is an object
        if( !jsonValue.isObject() ) throw XCom.newJXMRuntimeError(Texts.EMsg_JDL_OnlyObjRootEl);

        // Create a new object
        final T obj = rootType.getDeclaredConstructor().newInstance();

        // Set the value to the newly created object
        _deserializeObject(null, null, obj, jsonValue);

        // Return the object
        return obj;
    }

} // class JSONDecoder
