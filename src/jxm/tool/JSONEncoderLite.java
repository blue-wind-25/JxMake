/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

import java.util.HashMap;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


/*
 * This class is written specifically to serialize non-subclasses and subclasses (of superclasses)
 * that inherit the 'jxm.SerializableDeepClone' and 'java.util.ArrayList<? extends jxm.SerializableDeepClone>'
 * classes directly.
 */
public class JSONEncoderLite {

    public static final String CLASS_NAME_KEY = "@class";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final StringBuilder _tokSB = new StringBuilder();

    private JSONEncoderLite()
    {}

    private void _println()
    { _tokSB.append('\n'); }

    private void _printf(final int level, final String format, final Object... args)
    {
        for(int i = 0; i < level; ++i) _tokSB.append("    ");

        _tokSB.append( String.format(format, args) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private HashMap<Object, String> _serializeObject_list = new HashMap<>();

    private void _serialize_primitive(final int level, final String name, final Object object, final Class<?> clazz, final DataFormat.StringFormatWrapper format) throws Exception
    {
             if(name  != null) _printf(level, "\"%s\" : ", name);
        else if(level >  0   ) _printf(level, ""               );

        String fstr;
        try {
                 if( clazz.getName().equals("java.lang.Byte" ) ) fstr = ( clazz.cast(object).equals( (byte ) - 1 ) ) ? format.formatMinus1 : format.formatNormal;
            else if( clazz.getName().equals("java.lang.Short") ) fstr = ( clazz.cast(object).equals( (short) - 1 ) ) ? format.formatMinus1 : format.formatNormal;
            else if( clazz.getName().equals("java.lang.Long" ) ) fstr = ( clazz.cast(object).equals( (long ) - 1 ) ) ? format.formatMinus1 : format.formatNormal;
            else                       /* java.lang.Integer */   fstr = ( clazz.cast(object).equals(         - 1 ) ) ? format.formatMinus1 : format.formatNormal;
        }
        catch(final Exception e) {
            fstr = format.formatNormal;
        }

        _printf( 0, fstr, clazz.cast(object) );
    }

    private void _serializeArray_elements(final int level, final Object object, final Class<?> clazz, final DataFormat.StringFormatWrapper format, final boolean inline) throws Exception
    {
        final int len = Array.getLength(object);

        if(level > 0) _printf(level, "");

        for(int i = 0; i < len; ++i) {
            _serialize_primitive( 0, null, Array.get(object, i), clazz, format );
            _printf( 0, (i < len - 1) ? ", " : (inline ? " " : "\n") );
        }
    }

    private void _serializeArray(final int level_, final Field owner, final String name, final Object object) throws Exception
    {
        // Check if the array is null
        if(object == null) {
            if(name != null) _printf(level_, "\"%s\" : null", name);
            else             _printf(level_, "null\n"             );
            return;
        }

        // Get the class
        final Class<?> clazz    = object.getClass();
        final String   typeName = clazz.getName();
        final boolean  isObject = typeName.equals("[Ljava.lang.Object;");

        // Determine whether to print the element(s) inline
        final boolean inline = (!isObject) && ( Array.getLength(object) <= 8 );
              int     level  = inline ? 0 : level_ + 1;

        // Print the opening tag (and optionally the name)
        if(name != null) _printf(level_, "\"%s\" : [", name);
        else             _printf(level_, "["               );
                         _printf(0, inline ? " " : "\n"    );

        // Print the element(s)
        switch( typeName.substring(1) ) {
            case "Z" /* boolean */ : _serializeArray_elements( level, object, Boolean  .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatBoolean  ), inline ); break;
            case "B" /* byte    */ : _serializeArray_elements( level, object, Byte     .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatByte     ), inline ); break;
            case "S" /* short   */ : _serializeArray_elements( level, object, Short    .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatShort    ), inline ); break;
            case "I" /* int     */ : _serializeArray_elements( level, object, Integer  .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatInteger  ), inline ); break;
            case "J" /* long    */ : _serializeArray_elements( level, object, Long     .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatLong     ), inline ); break;
            case "F" /* float   */ : _serializeArray_elements( level, object, Float    .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatFloat    ), inline ); break;
            case "D" /* double  */ : _serializeArray_elements( level, object, Double   .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatDouble   ), inline ); break;
            case "C" /* char    */ : _serializeArray_elements( level, object, Character.class, DataFormat.getStringFormat(owner, DataFormat.DefFormatCharacter), inline ); break;
            default                : {
                                         if( typeName.equals("[Ljava.lang.Object;") || typeName.equals("[Ljava.lang.String;") ) {
                                             // Serialize an array of objects
                                             final int len = Array.getLength(object);
                                             for(int i = 0; i < len; ++i) {
                                                 if( _serializeObject(level, owner, null, Array.get(object, i) ) ) {
                                                     // ##### !!! TODO : Verify this !!! #####
                                                     if(i < len - 1) _printf(level, ",\n");
                                                   //if(i < len - 1) _printf(level, "," );
                                                   //                _printf(0    , "\n");
                                                 }
                                             }
                                         }
                                         else {
                                             // Unsupported types
                                             throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_SerAoOUnsupported, typeName);
                                         }
                                     }
                                     break;
        } // switch

        // Print the closing tag
        _printf( level - 1, (owner != null) ? "]" : "]\n" );
    }

    private boolean _serializeObject(final int level_, final Field owner, final String name, final Object object) throws Exception
    {
        // Check if the same field has been serialized
        // ##### !!! TODO : Verify this !!! #####
        if(object != null) {
            final String chkName = ( (owner == null) ? "null" : owner. getType().getName() ) + ":" + object.getClass().getName();
            final String refName = _serializeObject_list.get("" + object);
            if(refName != null) {
                if( refName.equals(chkName) ) return false;
                final String[] chk = XCom.re_splitColon(chkName);
                final String[] ref = XCom.re_splitColon(refName);
                if( Class.forName(chk[0]).isAssignableFrom( Class.forName(ref[0]) ) && chk[1].equals(ref[1]) ) return false;
            }
            else {
                _serializeObject_list.put("" + object, chkName);
            }
        }

        // Check if the object is null
        if(object == null) {
            if(name != null) _printf(level_, "\"%s\" : null", name);
            else             _printf(level_, "null\n"             );
            return true;
        }

        // Save the level and get the class
              int      level = level_;
        final Class<?> clazz = object.getClass();

        // Check the super class
        final Class<?> superClazz            = clazz.getSuperclass();
        final String   superClazzName        = superClazz.getName();
        final boolean  superClassIsArrayList = superClazzName.equals("java.util.ArrayList");

        if(    superClazzName != null
           && !superClassIsArrayList
           && !superClazzName.equals("java.lang.Object"         )
           && !superClazzName.equals("jxm.SerializableDeepClone")
        ) {
            // Allow one further subclassing from 'jxm.SerializableDeepClone'
            final String superSuperClazzName = superClazz.getSuperclass().getName();
            if( !superSuperClazzName.equals("jxm.SerializableDeepClone") ) {
                // Allow one further interfacing from 'Serializable'
                final Class<?>[] interfaces     = superClazz.getInterfaces();
                final String     interface0Name = (interfaces.length == 1) ? interfaces[0].getSimpleName() : "";
                if( !interface0Name.equals("Serializable") ) throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_OnlyNSubAndSDCSuper);
            }
        }

        if(superClassIsArrayList) {
            // Check the generic type
            final ParameterizedType paramSuperClazz   = (ParameterizedType) clazz.getGenericSuperclass();
            final Class<?>          genClazz          = (Class<?>         ) paramSuperClazz.getActualTypeArguments()[0];
            final Class<?>          genSuperClazz     = genClazz.getSuperclass();
            final String            gensuperClazzName = genSuperClazz.getName();
            if( !gensuperClazzName.equals("jxm.SerializableDeepClone") ) throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_OnlyNSubAndSDCSuper);
        }

        // Print the opening tag (and optionally the name)
        if(name != null) _printf(level, "\"%s\" : {\n", name);
        else             _printf(level, "{\n"               );
        ++level;

        // Get the field(s)
        final Field[] fields = clazz.getFields();

        // Get and print the class name
        final String clazzName = clazz.getName();
        _printf(level, "\"" + JSONEncoderLite.CLASS_NAME_KEY + "\" : \"%s\"", clazzName);
        _printf(0, ( superClassIsArrayList || (fields.length > 0) ) ? ",\n" : "\n");

        // Print the element(s)
        if(superClassIsArrayList) {
            /*
            final Method mLen = java.util.ArrayList.class.getDeclaredMethod("size"          );
            final Method mGet = java.util.ArrayList.class.getDeclaredMethod("get", int.class);
            for( int i = 0; i < (int) mLen.invoke(object); ++i ) {
                SysUtil.stdDbg().println( mGet.invoke(object, i) );
            }
            */
            // Check for fields and write the serial version UID if it exists
            String svuid     = "";
            for(int i = 0; i < fields.length; ++i) {
                final Field  field     = fields[i];
                final String fieldName = field.getName();
                if( !fieldName.equals(SysUtil.JxMakeSerialVersionUID_FieldName) ) {
                    throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_ClsAryLstWithAddFld);
                }
                else {
                    final long                           value  = field.getLong(object);
                    final DataFormat.StringFormatWrapper sfmtwr = DataFormat.getStringFormat(field, DataFormat.DefFormatLong);
                    final String                         format = (value == -1) ? sfmtwr.formatMinus1 : sfmtwr.formatNormal;
                    svuid = String.format(format, value);
                    svuid = String.format("[%s:%s]", SysUtil.JxMakeSerialVersionUID_FieldName, svuid);
                }
            }
            // Serialize the contents
            final Method toArray = java.util.ArrayList.class.getDeclaredMethod("toArray");
            _serializeArray( level, null, "@java.util.ArrayList" + svuid + "#add", toArray.invoke(object) );
        }

        // Print the field(s)
        else {

            for(int i = 0; i < fields.length; ++i) {

                // Get the field and field name
                final Field  field     = fields[i];
                final String fieldName = field.getName();

                // Get the field class
                final Class<?> ftype     = field.getType();
                final String   ftypeName = ftype.getName();

                // Get and check the modifiers
                final int     modifiers  = field.getModifiers();
                final boolean isNull     = field.get(object) == null;
                final boolean isWritable = ( ftype.isArray() || ftype.isAnonymousClass() || ftype.isLocalClass() || ftype.isMemberClass() ) && !isNull;

                if( fieldName.equals(SysUtil.JxMakeSerialVersionUID_FieldName) ) {
                    // The serial version UID (if it exists) must be public, static, and final
                    if( !Modifier.isPublic(modifiers) ) throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_SVUIDNotPubStaFinal);
                    if( !Modifier.isStatic(modifiers) ) throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_SVUIDNotPubStaFinal);
                    if( !Modifier.isFinal (modifiers) ) throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_SVUIDNotPubStaFinal);
                }
                else {
                    // Other fields must be public, non-static, and non-final
                    if( !Modifier.isPublic(modifiers)                ) throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_ClsWithNonPubField );
                    if(  Modifier.isStatic(modifiers)                ) throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_ClsWithStaticField );
                    if(  Modifier.isFinal (modifiers) && !isWritable ) throw XCom.newJXMRuntimeError(Texts.EMsg_JEL_ClsWithNonWrtFinFld);
                }

                // Flag
                boolean serialized = true;

                // Process enum
                if( ftype.isEnum() ) {
                    final Object enumVal = field.get(object);
                    if(enumVal != null) _printf(level, "\"%s\" : \"%s\"", fieldName, enumVal.toString() );
                    else                _printf(level, "\"%s\" : null"  , fieldName                     );
                }

                // Process array
                else if( ftype.isArray() ) {
                    _serializeArray( level, field, fieldName, field.get(object) );
                }

                // Process other types
                else {

                    switch(ftypeName) {
                        case "java.lang.Boolean"       : /* FALLTHROUGH */
                        case "boolean"                 : _serialize_primitive(level, fieldName, field.get(object), Boolean  .class, DataFormat.getStringFormat(field, DataFormat.DefFormatBoolean  ) ); break;

                        case "java.lang.Byte"          : /* FALLTHROUGH */
                        case "byte"                    : _serialize_primitive(level, fieldName, field.get(object), Byte     .class, DataFormat.getStringFormat(field, DataFormat.DefFormatByte     ) ); break;

                        case "java.lang.Short"         : /* FALLTHROUGH */
                        case "short"                   : _serialize_primitive(level, fieldName, field.get(object), Short    .class, DataFormat.getStringFormat(field, DataFormat.DefFormatShort    ) ); break;

                        case "java.lang.Integer"       : /* FALLTHROUGH */
                        case "int"                     : _serialize_primitive(level, fieldName, field.get(object), Integer  .class, DataFormat.getStringFormat(field, DataFormat.DefFormatInteger  ) ); break;

                        case "java.lang.Long"          : /* FALLTHROUGH */
                        case "long"                    : _serialize_primitive(level, fieldName, field.get(object), Long     .class, DataFormat.getStringFormat(field, DataFormat.DefFormatLong     ) ); break;

                        case "java.lang.Float"         : /* FALLTHROUGH */
                        case "float"                   : _serialize_primitive(level, fieldName, field.get(object), Float    .class, DataFormat.getStringFormat(field, DataFormat.DefFormatFloat    ) ); break;

                        case "java.lang.Double"        : /* FALLTHROUGH */
                        case "double"                  : _serialize_primitive(level, fieldName, field.get(object), Double   .class, DataFormat.getStringFormat(field, DataFormat.DefFormatDouble   ) ); break;

                        case "java.lang.Character"     : /* FALLTHROUGH */
                        case "char"                    : _serialize_primitive(level, fieldName, field.get(object), Character.class, DataFormat.getStringFormat(field, DataFormat.DefFormatCharacter) ); break;

                        case "java.lang.String"        : /* FALLTHROUGH */
                        case "java.util.regex.Pattern" : {
                                                             final Object obj    = field.get(object);
                                                             final String objStr = (obj != null) ? ( "\"" + obj.toString() + "\"" ) : null;
                                                             _printf(level, "\"%s\" : %s", fieldName, objStr );
                                                         }
                                                         break;

                        default                        : serialized = _serializeObject( level, field, fieldName, field.get(object) );
                                                         break;

                    } // switch

                }

                // Print the comma as needed
                // ##### !!! TODO : Verify this !!! #####
                if(serialized) _printf( 0, (i < fields.length - 1) ? ",\n" : "\n" );

            } // for

        } // if

        // Print the closing tag
        --level;
        _printf( level, (owner != null) ? "}" : "}\n" );

        // Done
        return true;
    }

    private void _serialize(final Object object) throws Exception
    {
        _serializeObject_list.clear();

        final Class<?> clazz = object.getClass();

             if( clazz.isArray      () ) _serializeArray (0, null, null, object);
        else if( clazz.isMemberClass() ) _serializeObject(0, null, null, object);
        else throw XCom.newJXMRuntimeError( Texts.EMsg_JEL_UnsupportedCls, clazz.getName() );
    }

    private String _getResult()
    { return _tokSB.toString(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static String serialize(final Object object) throws Exception
    {
        final JSONEncoderLite jel = new JSONEncoderLite();

        jel._serialize(object);

        return jel._getResult();
    }

} // class JSONEncoderLite
