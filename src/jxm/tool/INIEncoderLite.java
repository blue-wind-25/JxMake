/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Pattern;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


/*
 * This class is written specifically to serialize non-subclasses and subclasses (of superclasses)
 * that inherit the 'jxm.SerializableDeepClone' class directly.
 *
 * NOTE : This class is significantly more limited in the types it can serialize but offers minor
 *        additional features, such as string translation, compared to the 'JSONEncoderLite' class.
 */
public class INIEncoderLite {

    public static final String  ClassNameMarker = ":::";
    public static final String  TranslateMarker = "$";
    public static final String  FunctionMarker  = ":";

    public static final String  SCutDefMarker   = "#define:::";
    public static final Pattern SCutExpandRegex = Pattern.compile("(?<!\\\\)#\\{(.*?)\\}"); // '#{...}' but not '\#{...}'

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private       String                        _sectionID = null;
    private final LinkedHashMap<String, String> _kvPairMap = new LinkedHashMap<>();
    private final StringBuilder                 _iniText   = new StringBuilder();

    private INIEncoderLite()
    {}

    private void _kvPut(final String key, final String value)
    { _kvPairMap.put(key, value); }

    private void _kvPut(final String key, final String valueFormat, final Object... args)
    { _kvPairMap.put( key, String.format(valueFormat, args) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private HashMap<Object, String> _serializeObject_list = new HashMap<>();

    private static final int MaxArrayInlineCnt = 8;

    private String _serialize_primitive(
        final String                           combinedFieldName,
        final Object                           object,
        final Class<?>                         clazz,
        final DataFormat.StringFormatWrapper   format,
        final DataFormat.StringValueTranslator st
    ) throws Exception
    {
        String fstr;

        try {
            final String clazzName = clazz.getName();
                 if( clazzName.equals( "java.lang.Byte"  ) ) fstr = ( clazz.cast(object).equals( (byte ) - 1 ) ) ? format.formatMinus1 : format.formatNormal;
            else if( clazzName.equals( "java.lang.Short" ) ) fstr = ( clazz.cast(object).equals( (short) - 1 ) ) ? format.formatMinus1 : format.formatNormal;
            else if( clazzName.equals( "java.lang.Long"  ) ) fstr = ( clazz.cast(object).equals( (long ) - 1 ) ) ? format.formatMinus1 : format.formatNormal;
            else                    /* java.lang.Integer */  fstr = ( clazz.cast(object).equals(         - 1 ) ) ? format.formatMinus1 : format.formatNormal;
        }
        catch(final Exception e) {
            fstr = format.formatNormal;
        }

        final String res = String.format( fstr, clazz.cast(object) );
        final String trn = (st != null) ? st.translate(combinedFieldName, res) : res;

        if(combinedFieldName != null) _kvPut(combinedFieldName, trn);

        return trn;
    }

    private String _serializeArray_elements(
        final String                           combinedFieldName,
        final Object                           object,
        final Class<?>                         clazz,
        final DataFormat.StringFormatWrapper   format,
        final String                           cn,
        final DataFormat.StringValueTranslator st,
        final boolean                          inline
    ) throws Exception
    {
        // Prepare the string builder
        final StringBuilder elmStr = new StringBuilder();

        // Process the elements
        final int     len = Array.getLength(object);
              int     cnt = 0;
              boolean ml  = false;
              boolean ht  = false;

        for(int i = 0; i < len; ++i) {

            if(i != 0) elmStr.append(", ");

            if(!inline && cnt >= MaxArrayInlineCnt) {
                elmStr.append("\\\n");
                cnt = 0;
                ml  = true;
            }

            final String res = _serialize_primitive( null, Array.get(object, i), clazz, format, st );
            if( res.startsWith(TranslateMarker) ) ht = true;

            elmStr.append(res);
            ++cnt;

        } // for

        // Put the <class_name> and <ClassNameMarker> as needed
        if(st != null && ht) {
            final String cnStr = cn + ' ' + ClassNameMarker;
            if(!inline && ml) {
                final int    totElm = ml ? MaxArrayInlineCnt : cnt;
                final int    totLen = st.translate(null, "").length() * totElm + 2 * (totElm - 1) + 2;
                elmStr.insert( 0, String.format("%-" + totLen + "s\\\n", cnStr) );
            }
            else {
                elmStr.insert( 0, String.format("%-" + ( elmStr.length() + 1 ) + "s\\\n", cnStr) );
            }
        }

        // Put the key-value pairs
        if(combinedFieldName != null) _kvPut( combinedFieldName, elmStr.toString() );

        // Return the string
        return elmStr.toString();
    }

    private void _serializeArray(
        final String                           combinedFieldName,
        final Field                            owner,
        final Object                           object,
        final String                           cn,
        final DataFormat.StringValueTranslator st
    ) throws Exception
    {
        // Check if the array is null
        if(object == null) {
            _kvPut(combinedFieldName, "null");
            return;
        }

        // Get the class
        final Class<?> clazz    = object.getClass();
        final String   typeName = clazz.getName();

        // Determine whether to print the element(s) inline
        final boolean inline = ( Array.getLength(object) <= MaxArrayInlineCnt );

        // Print the element(s)
        switch( typeName.substring(1) ) {

            case "Z" /* boolean */ : _serializeArray_elements( combinedFieldName, object, Boolean  .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatBoolean  ), cn, st, inline ); break;
            case "B" /* byte    */ : _serializeArray_elements( combinedFieldName, object, Byte     .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatByte     ), cn, st, inline ); break;
            case "S" /* short   */ : _serializeArray_elements( combinedFieldName, object, Short    .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatShort    ), cn, st, inline ); break;
            case "I" /* int     */ : _serializeArray_elements( combinedFieldName, object, Integer  .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatInteger  ), cn, st, inline ); break;
            case "J" /* long    */ : _serializeArray_elements( combinedFieldName, object, Long     .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatLong     ), cn, st, inline ); break;
            case "F" /* float   */ : _serializeArray_elements( combinedFieldName, object, Float    .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatFloat    ), cn, st, inline ); break;
            case "D" /* double  */ : _serializeArray_elements( combinedFieldName, object, Double   .class, DataFormat.getStringFormat(owner, DataFormat.DefFormatDouble   ), cn, st, inline ); break;
            case "C" /* char    */ : _serializeArray_elements( combinedFieldName, object, Character.class, DataFormat.getStringFormat(owner, DataFormat.RawFormatCharacter), cn, st, inline ); break;

            default                : throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_SerAoOUnsupported);

        } // switch
    }

    private void _serializeObject(final String prefixName, final Field owner, final String name, final Object object) throws Exception
    {
        // Check if the same field has been serialized
        // ##### !!! TODO : Verify this !!! #####
        if(object != null) {
            final String chkName = ( (owner == null) ? "null" : owner. getType().getName() ) + ":" + object.getClass().getName();
            final String refName = _serializeObject_list.get("" + object);
            if(refName != null) {
                if( refName.equals(chkName) ) return;
                final String[] chk = XCom.re_splitColon(chkName);
                final String[] ref = XCom.re_splitColon(refName);
                if( Class.forName(chk[0]).isAssignableFrom( Class.forName(ref[0]) ) && chk[1].equals(ref[1]) ) return;
            }
            else {
                _serializeObject_list.put("" + object, chkName);
            }
        }

        // Check if the object is null
        if(object == null) {
            if(name == null) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_NullFieldName);
            _kvPut( ( (prefixName != null) ? (prefixName + '.'): "" ) + name, "null" );
        }

        // Get the class
        final Class<?> clazz = object.getClass();

        // Check the super class
        final Class<?> superClazz     = clazz.getSuperclass();
        final String   superClazzName = superClazz.getName();

        if(    superClazzName != null
           && !superClazzName.equals("java.lang.Object"         )
           && !superClazzName.equals("jxm.SerializableDeepClone")
        ) {
            // Allow one further subclassing from 'jxm.SerializableDeepClone'
            final String superSuperClazzName = superClazz.getSuperclass().getName();
            if( !superSuperClazzName.equals("jxm.SerializableDeepClone") ) {
                // Allow one further interfacing from 'Serializable'
                final Class<?>[] interfaces     = superClazz.getInterfaces();
                final String     interface0Name = (interfaces.length == 1) ? interfaces[0].getSimpleName() : "";
                if( !interface0Name.equals("Serializable") ) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_OnlyNSubAndSDCSuper);
            }
        }

        // Get the field(s)
        final Field[] fields = clazz.getFields();

        // Get and save the section identifier (for the top-most class only)
        if(_sectionID == null) {

            // Find the serial version UID field
            String svUID = null;

            for(int i = 0; i < fields.length; ++i) {

                // Check if it is the serial version UID
                final Field  field     = fields[i];
                final String fieldName = field.getName();

                if( !fieldName.equals(SysUtil.JxMakeSerialVersionUID_FieldName) ) continue;

                // The serial version UID (if it exists) must be public, static, and final
                final int modifiers  = field.getModifiers();

                if( !Modifier.isPublic(modifiers) ) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_SVUIDNotPubStaFinal);
                if( !Modifier.isStatic(modifiers) ) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_SVUIDNotPubStaFinal);
                if( !Modifier.isFinal (modifiers) ) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_SVUIDNotPubStaFinal);

                // The serial version UID (if it exists) must be of type 'long'
                final Class<?> ftype     = field.getType();
                final String   ftypeName = ftype.getName();

                if( !ftypeName.equals("java.lang.Long") && !ftypeName.equals("long") ) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_SVUIDNotTypeLong);

                // Save the serial version UID
                svUID = _serialize_primitive( null, field.get(object), Long.class, DataFormat.getStringFormat(field, DataFormat.DefFormatLong), null );

            } // for

            // Get the section identifier
            _sectionID = (svUID == null) ? String.format( "[CLASS:%s]"   , clazz.getName()        )
                                         : String.format( "[CLASS:%s@%s]", clazz.getName(), svUID );

        } // if

        // Print the field(s)
        for(int i = 0; i < fields.length; ++i) {

            // Get the field and field name
            final Field  field     = fields[i];
            final String fieldName = field.getName();

            // Get the field class
            final Class<?> ftype     = field.getType();
            final String   ftypeName = ftype.getName();

            // Skip if it is the serial version UID
            if( fieldName.equals(SysUtil.JxMakeSerialVersionUID_FieldName) ) continue;

            // Fields must be public, non-static, and non-final
            final int     modifiers  = field.getModifiers();
            final boolean isNull     = field.get(object) == null;
            final boolean isWritable = ( ftype.isArray() || ftype.isAnonymousClass() || ftype.isLocalClass() || ftype.isMemberClass() ) && !isNull;

            if( !Modifier.isPublic(modifiers)                ) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_ClsWithNonPubField );
            if(  Modifier.isStatic(modifiers)                ) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_ClsWithStaticField );
            if(  Modifier.isFinal (modifiers) && !isWritable ) throw XCom.newJXMRuntimeError(Texts.EMsg_IEL_ClsWithNonWrtFinFld);

            // Generate the combined field name
            final String combinedFieldName = ( (prefixName != null) ? (prefixName + '.'): "" ) + fieldName;

            // Get the string translator and reset it
            final XCom.Pair<String, DataFormat.StringValueTranslator> pair = DataFormat.getStringTranslator(field);
            final String                                              cn   = (pair != null) ? pair.first () : null;
            final DataFormat.StringValueTranslator                    st   = (pair != null) ? pair.second() : null;

            if(st != null) st.reset();

            // Process enum
            if( ftype.isEnum() ) {
                final Object enumVal = field.get(object);
                 _kvPut( combinedFieldName, enumVal.toString() );
            }

            // Process array
            else if( ftype.isArray() ) {
                _serializeArray( combinedFieldName, field, field.get(object), cn, st );
            }

            // Process other types
            else {

                // Process the value
                switch(ftypeName) {
                    case "java.lang.Boolean"       : /* FALLTHROUGH */
                    case "boolean"                 : _serialize_primitive(combinedFieldName, field.get(object), Boolean  .class, DataFormat.getStringFormat(field, DataFormat.DefFormatBoolean  ), st ); break;

                    case "java.lang.Byte"          : /* FALLTHROUGH */
                    case "byte"                    : _serialize_primitive(combinedFieldName, field.get(object), Byte     .class, DataFormat.getStringFormat(field, DataFormat.DefFormatByte     ), st ); break;

                    case "java.lang.Short"         : /* FALLTHROUGH */
                    case "short"                   : _serialize_primitive(combinedFieldName, field.get(object), Short    .class, DataFormat.getStringFormat(field, DataFormat.DefFormatShort    ), st ); break;

                    case "java.lang.Integer"       : /* FALLTHROUGH */
                    case "int"                     : _serialize_primitive(combinedFieldName, field.get(object), Integer  .class, DataFormat.getStringFormat(field, DataFormat.DefFormatInteger  ), st ); break;

                    case "java.lang.Long"          : /* FALLTHROUGH */
                    case "long"                    : _serialize_primitive(combinedFieldName, field.get(object), Long     .class, DataFormat.getStringFormat(field, DataFormat.DefFormatLong     ), st ); break;

                    case "java.lang.Float"         : /* FALLTHROUGH */
                    case "float"                   : _serialize_primitive(combinedFieldName, field.get(object), Float    .class, DataFormat.getStringFormat(field, DataFormat.DefFormatFloat    ), st ); break;

                    case "java.lang.Double"        : /* FALLTHROUGH */
                    case "double"                  : _serialize_primitive(combinedFieldName, field.get(object), Double   .class, DataFormat.getStringFormat(field, DataFormat.DefFormatDouble   ), st ); break;

                    case "java.lang.Character"     : /* FALLTHROUGH */
                    case "char"                    : _serialize_primitive(combinedFieldName, field.get(object), Character.class, DataFormat.getStringFormat(field, DataFormat.RawFormatCharacter), st ); break;

                    case "java.lang.String"        : /* FALLTHROUGH */
                    case "java.util.regex.Pattern" : {
                                                         final Object obj    = field.get(object);
                                                         final String objStr = (obj != null) ? XCom.re_serStringDQ( obj.toString() ) : null;
                                                         _kvPut(combinedFieldName, objStr);
                                                     }
                                                     break;

                    default                        : _serializeObject( combinedFieldName, field, fieldName, field.get(object) );
                                                     break;

                } // switch

                // Put the <class_name> and <ClassNameMarker> as needed
                if(st != null) {
                    final Iterator< Map.Entry<String, String> > iterator = _kvPairMap.entrySet().iterator();
                          String                                lastKey  = null;
                          String                                lastVal  = null;
                    while( iterator.hasNext() ) {
                        final Map.Entry<String, String> entry = iterator.next();
                        lastKey = entry.getKey  ();
                        lastVal = entry.getValue();
                    }
                    if( lastVal.startsWith(TranslateMarker) ) _kvPairMap.put(lastKey, cn + ' ' + ClassNameMarker + ' ' + lastVal);
                }
            }

        } // for
    }

    private void _serialize(final Object object) throws Exception
    {
        _serializeObject_list.clear();

        final Class<?> clazz = object.getClass();

        if( !clazz.isMemberClass() ) throw XCom.newJXMRuntimeError( Texts.EMsg_IEL_UnsupportedCls, clazz.getName() );

        _serializeObject(null, null, null, object);
    }

    private String _getResult()
    {
        // Put the section identifier
        _iniText.append( String.format("%s\n\n", _sectionID) );

        // Get the longest key length
        int maxKeyLen = 0;

        for( final String key : _kvPairMap.keySet() ) {
            maxKeyLen = Math.max( maxKeyLen, key.length() );
        }

        // Put the key-value pairs
        final String format =  "%-" + maxKeyLen + "s = %s\n";
        final String lsPad  = String.format( "\n%" + (maxKeyLen + 3) + "s", "");
              String prvFMN = null;

        for( Map.Entry<String, String> entry : _kvPairMap.entrySet() ) {
            final String key    = entry.getKey();
            final String curFMN = XCom.re_splitDot(key)[0];

            if( prvFMN != null && !prvFMN.equals(curFMN) ) _iniText.append('\n');
            prvFMN = curFMN;

            final String strVal = entry.getValue();
            final String repVal = (strVal == null) ? "null" : strVal.replace("\n", lsPad);

            _iniText.append( String.format( format, entry.getKey(), repVal ) );
        }

        // Return the result
        return _iniText.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static String serialize(final Object object) throws Exception
    {
        final INIEncoderLite iel = new INIEncoderLite();

        iel._serialize(object);

        return iel._getResult();
    }

} // class INIEncoderLite
