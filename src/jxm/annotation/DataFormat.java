/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.annotation;


import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Field;

import java.util.HashMap;
import java.util.Map;

import jxm.*;
import jxm.xb.*;


public class DataFormat {

    public static class StringFormatWrapper {
        public final String formatNormal;
        public final String formatMinus1;

        public StringFormatWrapper(final String formatNormal_, final String formatMinus1_)
        {
            formatNormal = formatNormal_;
            formatMinus1 = formatMinus1_;
        }
    }

    public static StringFormatWrapper DefFormatBoolean   = new StringFormatWrapper(  "%b"  ,   "%b"  );
    public static StringFormatWrapper DefFormatByte      = new StringFormatWrapper(  "%d"  ,   "%d"  );
    public static StringFormatWrapper DefFormatShort     = new StringFormatWrapper(  "%d"  ,   "%d"  );
    public static StringFormatWrapper DefFormatInteger   = new StringFormatWrapper(  "%d"  ,   "%d"  );
    public static StringFormatWrapper DefFormatLong      = new StringFormatWrapper(  "%d"  ,   "%d"  );
    public static StringFormatWrapper DefFormatFloat     = new StringFormatWrapper(  "%f"  ,   "%f"  );
    public static StringFormatWrapper DefFormatDouble    = new StringFormatWrapper(  "%f"  ,   "%f"  );
    public static StringFormatWrapper DefFormatCharacter = new StringFormatWrapper("\"%c\"", "\"%c\"");

    public static StringFormatWrapper RawFormatCharacter = new StringFormatWrapper(  "%c"  ,   "%c"  );

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.ANNOTATION_TYPE)
    public static @interface StringFormat
    {
        public String formatNormal() default "";
        public String formatMinus1() default "";
    }

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "%-8d"   , formatMinus1 = "%-8d")
    public static @interface Dec08 {}

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "0x%02X" , formatMinus1 = "%-4d")
    public static @interface Hex02 {}

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "0x%04X" , formatMinus1 = "%-6d")
    public static @interface Hex04 {}

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "0x%06X" , formatMinus1 = "%-8d")
    public static @interface Hex06 {}

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "0x%08X" , formatMinus1 = "%-10d")
    public static @interface Hex08 {}

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "0x%010X", formatMinus1 = "%-10d")
    public static @interface Hex10 {}

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "0x%012X", formatMinus1 = "%-10d")
    public static @interface Hex12 {}

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "0x%014X", formatMinus1 = "%-10d")
    public static @interface Hex14 {}

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD          )
    @StringFormat(formatNormal = "0x%016X", formatMinus1 = "%-10d")
    public static @interface Hex16 {}

    public synchronized static StringFormatWrapper getStringFormat(final Field field, final StringFormatWrapper defaultFormat)
    {
        if(field == null) return defaultFormat;

        for( final Annotation da : field.getDeclaredAnnotations() ) {
            for( final Annotation a : da.annotationType().getAnnotations() ) {
                if(a instanceof StringFormat) return new StringFormatWrapper(
                    ( (StringFormat) a ).formatNormal(),
                    ( (StringFormat) a ).formatMinus1()
                );
            }
        }

        return defaultFormat;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static interface StringValueTranslator {
        // NOTE: Call this method once before translating a field's value(s)
        public void reset();

        // NOTE: Call this method as many times as needed to translate a field's value(s)
        public String translate(final String debugInfo, final String str);
    }

    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.ANNOTATION_TYPE)
    public static @interface StringValueTranslatorDescriptor
    {
        public String                                 className () default "";
        public Class<? extends StringValueTranslator> translator() default StringValueTranslator.class;
    }

    private static class _StringValueTranslatorCache
    {
        // NOTE: # Cache the instance to minimize unnecessary creation and destruction of 'StringValueTranslator' objects.
        //       # Use 'reset()' to reinitialize (reset) the instance.

        private static final Map<Class<? extends StringValueTranslator>, Object> _cache = new HashMap<>();

        public static <T extends StringValueTranslator> T getInstance(final Class<T> clazz) throws Exception
        {
            return clazz.cast( _cache.computeIfAbsent(
                clazz,
                key -> {
                    try {
                        return key.getDeclaredConstructor().newInstance();
                    }
                    catch(final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            ) );
        }

    } // class _StringValueTranslatorCache

    public synchronized static XCom.Pair<String, StringValueTranslator> getStringTranslator(final Field field)
    {
        if(field == null) return null;

        for( final Annotation da : field.getDeclaredAnnotations() ) {
            for( final Annotation a : da.annotationType().getAnnotations() ) {
                if( !(a instanceof StringValueTranslatorDescriptor) ) continue;
                try {
                    final StringValueTranslatorDescriptor d = (StringValueTranslatorDescriptor) a;
                    return new XCom.Pair<>(
                        d.className(),
                        _StringValueTranslatorCache.getInstance( d.translator() )
                    );
                }
                catch(final Exception e) {
                    break;
                }
            } // for
        } // for

        return null;
    }

} // class DataFormat
