/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.StringReader;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxm.*;
import jxm.xb.*;


/*
 * This class is written specifically to deserialize the INI text from the 'INIEncoderLite' class.
 */
public class INIDecoder {

    private static class INIData {

              String fileName           = null;

              String className          = null;
              long   classVersion       = -1;
              int    classLNum          = -1;

        final ArrayList<String[]> key   = new ArrayList<>();
        final ArrayList<String[]> value = new ArrayList<>();
        final ArrayList<Integer > lNum  = new ArrayList<>();

    } // class INIData

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmClassNameVer = Pattern.compile("^\\[CLASS:([^]@]+)(?:@([^]]*))?\\]$");
    private static final Pattern _pmKeyValPair   = Pattern.compile("^([^=]*)=(.+)$"                     );

    public synchronized static INIData decode(final String iniString, final String fileName) throws Exception
    {
        // Prepare the data instance
        final INIData iniData = new INIData();

        iniData.fileName = fileName;

        // Shortcuts
        final HashMap<String, String> sCuts = new HashMap<>();

        // Process the lines
        final BufferedReader br = new BufferedReader( new StringReader(iniString) );
              String         pl = null;
              int            ln = 0;

        while(true) {

            // Read one line
            String line = br.readLine();
            if(line == null) break;

            ++ln;

            // Remove comment
            final int cIdx = line.indexOf(";");
            if(cIdx >= 0) line = line.substring(0, cIdx);

            // Remove all whitespaces
            line = XCom.sm_deserStringDQ_removeAllNQWhitespaces(line);
            if( line.isEmpty() ) continue;

            // Combine line as needed
            if(pl != null) {
                line = pl + line;
                pl   = null;
            }

            if( line.endsWith("\\") ) {
                pl = line.substring( 0, line.length() - 1 );
                continue;
            }

            // Get and store the class name and version (optional)
            if(iniData.className == null) {
                final Matcher cnv = _pmClassNameVer.matcher(line);
                if( !cnv.find() ) throw XCom.newJXMRuntimeError(Texts.EMsg_IDL_NoStartClassName, iniData.fileName, ln);

                iniData.className = cnv.group(1);
                if( cnv.group(2) != null ) iniData.classVersion = Long.decode( cnv.group(2) );

                iniData.classLNum = ln;
            }

            // Get and store the key-value pairs
            else {
                // Extract the key-value pairs
                final Matcher kvp = _pmKeyValPair.matcher(line);
                if( !kvp.find() ) {
                    throw XCom.newJXMRuntimeError(
                        _pmClassNameVer.matcher(line).find() ? Texts.EMsg_IDL_OneClassName : Texts.EMsg_IDL_InvalidKeyValPair,
                        iniData.fileName, ln
                    );
                }

                String key = kvp.group(1);
                String val = kvp.group(2);

                // Check for shortcut definition
                if( key.startsWith(INIEncoderLite.SCutDefMarker) ) {
                    final String scName = key.substring( INIEncoderLite.SCutDefMarker.length() );
                    sCuts.put( scName, Matcher.quoteReplacement(val) );
                    continue;
                }

                // Expand (replace) shortcut(s)
                val = XCom.re_replace(val, INIEncoderLite.SCutExpandRegex, sCuts);

                // Split the key-value pairs
                final String[] keyParts = XCom.re_splitDot  (key);
                final String[] valParts = XCom.re_splitComma(val);

                // Translate symbol strings into their corresponding values as needed
                if( valParts[0].contains(INIEncoderLite.ClassNameMarker) ) {
                    // Split the class name and the first value (because 'sm_deserStringDQ_removeAllNQWhitespaces()' remove all non-quoted whitespaces)
                    //     <class_name><ClassNameMarker><first_value>
                    final String[] splitDDD = valParts[0].split(INIEncoderLite.ClassNameMarker, 2);
                    final Class<?> clazz    = Class.forName(splitDDD[0]);
                    valParts[0] = splitDDD[1];
                    // Translate all symbols that start with <TranslateMarker>
                    for(int i = 0; i < valParts.length; ++i) {
                        if( valParts[i].startsWith(INIEncoderLite.TranslateMarker) ) {
                            final String constName = valParts[i].substring(1);
                            if( !constName.contains(INIEncoderLite.FunctionMarker) ) {
                                valParts[i] = clazz.getField(constName).get(null).toString();
                            }
                            else {
                                final String[] fp = XCom.re_splitColon(constName);
                                if(fp.length == 2) {
                                     valParts[i] = (String) clazz.getDeclaredMethod(fp[0], String.class).invoke(null, fp[1]);
                                }
                            }
                        }
                    } // for
                }

                // Store the key-value pairs
                iniData.key  .add(keyParts);
                iniData.value.add(valParts);
                iniData.lNum .add(ln      );
            }

        } // while true

        // Return the data instance
        return iniData;
    }

    public synchronized static INIData decode(final String iniString) throws Exception
    { return decode(iniString, null); }

    public synchronized static INIData decode(final Path iniFilePath) throws Exception
    { return decode( SysUtil.readTextFileAsString(iniFilePath), iniFilePath.toString() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Object _parseNumber(final String valueStr) throws Exception
    { return XCom.parseNumberStr(valueStr); }

    private static String _deserializeString(final String valueStr)
    {
        // NOTE : Escape characters and quotes will have been handled by 'sm_deserStringDQ_removeAllNQWhitespaces()'

        if( valueStr == null || valueStr.equals("null") ) return null;

        return valueStr;
    }

    private static boolean _deserializePrimitive(final Object targetObj, final String trgFieldType, final Field trgField, final String valueStr) throws Exception
    {
        switch(trgFieldType) {

            case "java.lang.Boolean"       : /* FALLTHROUGH */
            case "boolean"                 : trgField.setBoolean( targetObj,    Boolean.parseBoolean(valueStr)                 ); break;

            case "java.lang.Byte"          : /* FALLTHROUGH */
            case "byte"                    : trgField.setByte   ( targetObj, ( (Long  ) _parseNumber(valueStr) ).byteValue  () ); break;

            case "java.lang.Short"         : /* FALLTHROUGH */
            case "short"                   : trgField.setShort  ( targetObj, ( (Long  ) _parseNumber(valueStr) ).shortValue () ); break;

            case "java.lang.Integer"       : /* FALLTHROUGH */
            case "int"                     : trgField.setInt    ( targetObj, ( (Long  ) _parseNumber(valueStr) ).intValue   () ); break;

            case "java.lang.Long"          : /* FALLTHROUGH */
            case "long"                    : trgField.setLong   ( targetObj, ( (Long  ) _parseNumber(valueStr) ).longValue  () ); break;

            case "java.lang.Float"         : /* FALLTHROUGH */
            case "float"                   : trgField.setFloat  ( targetObj, ( (Double) _parseNumber(valueStr) ).floatValue () ); break;

            case "java.lang.Double"        : /* FALLTHROUGH */
            case "double"                  : trgField.setDouble ( targetObj, ( (Double) _parseNumber(valueStr) ).doubleValue() ); break;

            case "java.lang.Character"     : /* FALLTHROUGH */
            case "char"                    : trgField.setChar   ( targetObj,                         valueStr.charAt(0)        ); break;

            case "java.lang.String"        : trgField.set       ( targetObj,      _deserializeString(valueStr)                 ); break;

            default                        : return false;

        } // switch

        return true;
    }

    private static <A, T> void _deserializeArray_elements(final A targetArray, final String[] valueStr, final Class<T> clazz) throws Exception
    {
        for(int i = 0; i < valueStr.length; ++i) {

            switch( clazz.getName() ) {

                case "java.lang.Boolean"       : /* FALLTHROUGH */
                case "boolean"                 : Array.set( targetArray, i,    Boolean.parseBoolean(valueStr[i])                 ); break;

                case "java.lang.Byte"          : /* FALLTHROUGH */
                case "byte"                    : Array.set( targetArray, i, ( (Long  ) _parseNumber(valueStr[i]) ).byteValue  () ); break;

                case "java.lang.Short"         : /* FALLTHROUGH */
                case "short"                   : Array.set( targetArray, i, ( (Long  ) _parseNumber(valueStr[i]) ).shortValue () ); break;

                case "java.lang.Integer"       : /* FALLTHROUGH */
                case "int"                     : Array.set( targetArray, i, ( (Long  ) _parseNumber(valueStr[i]) ).intValue   () ); break;

                case "java.lang.Long"          : /* FALLTHROUGH */
                case "long"                    : Array.set( targetArray, i, ( (Long  ) _parseNumber(valueStr[i]) ).longValue  () ); break;

                case "java.lang.Float"         : /* FALLTHROUGH */
                case "float"                   : Array.set( targetArray, i, ( (Double) _parseNumber(valueStr[i]) ).floatValue () ); break;

                case "java.lang.Double"        : /* FALLTHROUGH */
                case "double"                  : Array.set( targetArray, i, ( (Double) _parseNumber(valueStr[i]) ).doubleValue() ); break;

                case "java.lang.Character"     : /* FALLTHROUGH */
                case "char"                    : Array.set( targetArray, i,                         valueStr[i].charAt(0)        ); break;

                default                        : throw XCom.newJXMRuntimeError( Texts.EMsg_IEL_InvalidArrayElemTyp, clazz.getName() );

            } // switch

        } // for
    }

    private static void _deserializeArray(final Field trgField, final Object targetObj, final String[] valueStr) throws Exception
    {
        // Store the elements
        final String ftypeName = trgField.getType().getName();

        switch( ftypeName.substring(1) ) {

              case "Z" /* boolean */ : if(true) { final boolean[] array = new boolean[valueStr.length];
                                                  _deserializeArray_elements(array, valueStr, Boolean  .class);
                                                  trgField.set(targetObj, array);
                                                } break;

              case "B" /* byte    */ : if(true) { final byte   [] array = new byte   [valueStr.length];
                                                  _deserializeArray_elements(array, valueStr, Byte     .class);
                                                  trgField.set(targetObj, array);
                                                } break;

              case "S" /* short   */ : if(true) { final short  [] array = new short  [valueStr.length];
                                                  _deserializeArray_elements(array, valueStr, Short    .class);
                                                  trgField.set(targetObj, array);
                                                } break;

              case "I" /* int     */ : if(true) { final int    [] array = new int    [valueStr.length];
                                                  _deserializeArray_elements(array, valueStr, Integer  .class);
                                                  trgField.set(targetObj, array);
                                                } break;

              case "J" /* long    */ : if(true) { final long   [] array = new long   [valueStr.length];
                                                  _deserializeArray_elements(array, valueStr, Long     .class);
                                                  trgField.set(targetObj, array);
                                                } break;

              case "F" /* float   */ : if(true) { final float  [] array = new float  [valueStr.length];
                                                  _deserializeArray_elements(array, valueStr, Float    .class);
                                                  trgField.set(targetObj, array);
                                                } break;

              case "D" /* double  */ : if(true) { final double [] array = new double [valueStr.length];
                                                  _deserializeArray_elements( array, valueStr, Double   .class);
                                                  trgField.set(targetObj, array);
                                                } break;

              case "C" /* char    */ : if(true) { final char   [] array = new char   [valueStr.length];
                                                  _deserializeArray_elements(array, valueStr, Character.class);
                                                  trgField.set(targetObj, array);
                                                } break;

              default                : throw XCom.newJXMRuntimeError( Texts.EMsg_IEL_InvalidArrayElemTyp, ftypeName );

        } // switch
    }

    @SuppressWarnings("unchecked")
    private static <T> T _deserialize_impl(final INIData iniData, final Class<T> rootType) throws Exception
    {
        // Check the class name
        if( !rootType.getName().equals(iniData.className) ) {
            throw XCom.newJXMRuntimeError( Texts.EMsg_IDL_MismatchedClassName, iniData.fileName, iniData.classLNum, iniData.className, rootType.getName() );
        }

        // Check the class version
        if(iniData.classVersion >= 0) {
            final long ref = rootType.getField(SysUtil.JxMakeSerialVersionUID_FieldName).getLong(null);
            if(iniData.classVersion != ref) {
                throw XCom.newJXMRuntimeError( Texts.EMsg_IDL_MismatchedSVUID, iniData.fileName, iniData.classLNum, iniData.classVersion, ref );
            }
        }

        // Create a new object
        final T obj = rootType.getDeclaredConstructor().newInstance();

        // Change the field values
        for( int i = 0; i < iniData.lNum.size(); ++i ) {

            // Get the data
            final String[] key   = iniData.key  .get(i);
            final String[] value = iniData.value.get(i);
            final int      ln    = iniData.lNum .get(i);

            // Get the final field at the end of the chain
            Field  trgField  = null;
            Object targetObj = obj;

            for(int k = 0; k < key.length; ++k) {

                if(k != 0) targetObj = trgField.get(targetObj);

                trgField = targetObj.getClass().getField(key[k]);

            } // for

            // Get the field type
            final Class<?> trgFieldType = trgField.getType();
            final String   value0       = "null".equals(value[0]) ? null : value[0];

            // Process enum
            if( trgFieldType.isEnum() ) {
                try {
                    if(value0 != null) trgField.set( targetObj, Enum.valueOf( (Class<Enum>) trgFieldType, value0 ) );
                    else               trgField.set( targetObj, null                                               );
                }
                catch(final Exception e) {
                    throw XCom.newJXMRuntimeError( Texts.EMsg_IDL_InvalidValueExt, iniData.fileName, ln, String.join(".", key), e.toString() );
                }
            }

            // Process array
            else if( trgFieldType.isArray() ) {
                try {
                    if(value0 != null) _deserializeArray(trgField, targetObj, value);
                    else               trgField.set(targetObj, null);
                }
                catch(final Exception e) {
                    throw XCom.newJXMRuntimeError( Texts.EMsg_IDL_InvalidValueExt, iniData.fileName, ln, String.join(".", key), e.toString() );
                }
            }

            // Process other types
            else {
                if( !_deserializePrimitive(targetObj, trgFieldType.getName(), trgField, value0) ) {
                    throw XCom.newJXMRuntimeError( Texts.EMsg_IDL_InvalidValue, iniData.fileName, ln, String.join(".", key) );
                }
            }

        } // for

        // Return the object
        return obj;
    }

    public synchronized static <T> T deserialize(final String iniString, final Class<T> rootType) throws Exception
    { return _deserialize_impl( decode(iniString), rootType ); }

    public synchronized static <T> T deserialize(final Path iniFilePath, final Class<T> rootType) throws Exception
    { return _deserialize_impl( decode(iniFilePath), rootType ); }

} // class INIDecoder
