/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.time.temporal.TemporalAccessor;

import java.util.ArrayList;
import java.util.Date;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jxm.xb.*;


public class AutoPrintf {

    private static Pattern _reIndex = Pattern.compile("\\d+\\$");

    public static String format(final String format, final String... args)
    {
        if( _reIndex.matcher(format).find() ) return _formatSequential(format, args);
        else                                  return _formatIndexed   (format, args);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _formatIndexed(final String format, final String... args)
    {
        final ArrayList<Object> converted = new ArrayList<>();
              int               argIndex  = 0;

        for(int i = 0; i < format.length(); ++i) {

            if( format.charAt(i) != '%' ) continue;

            if( i + 1 < format.length() && format.charAt(i + 1) == '%' ) {
                ++i;
                continue;
            }

            int j = i + 1;
            while( j < format.length() && !_isFormatChar( format.charAt(j) ) ) ++j;
            if( j >= format.length() ) break;

            final char type = format.charAt(j);
            converted.add( _convertArg(args, argIndex++, type) );

            i = j;

        } // for

        return String.format( format, converted.toArray( new Object[0] ) );
    }

    private static String _formatSequential(final String format, final String... args)
    {
        final ArrayList<Object> converted = new ArrayList<>();
        final StringBuilder     getIdxBuf = new StringBuilder();
        final StringBuilder     fmtBuf    = new StringBuilder();

        for( int i = 0; i < format.length(); ++i ) {

            final char ch_i = format.charAt(i);
            if(ch_i != '%') {
                fmtBuf.append(ch_i);
                continue;
            }

            if( i + 1 < format.length() && format.charAt(i + 1) == '%' ) {
                fmtBuf.append("%%");
                ++i;
                continue;
            }

            int j = i + 1;
            getIdxBuf.setLength(0);
            while( j < format.length() ) {
                final char ch_j = format.charAt(j);
                if( ch_j != '-' && ch_j != '+' && !Character.isDigit(ch_j) ) break;
                getIdxBuf.append(ch_j);
                ++j;
            }

            final boolean isIndex   = ( j < format.length() ) && ( format.charAt(j) == '$' );
            final String  strIdxBuf = getIdxBuf.toString();
                  int     useIndex  = -1;
            if( isIndex && !strIdxBuf.isEmpty() ) {
                useIndex = Integer.parseInt(strIdxBuf) - 1;
                ++j;
            }

            fmtBuf.append('%');
            while( j < format.length() ) {
                final char ch_j = format.charAt(j);
                if( _isFormatChar(ch_j) ) break;
                fmtBuf.append(ch_j);
                ++j;
            }
            if( j >= format.length() ) break;

            final char type = format.charAt(j);
            fmtBuf.append(type);

            if( type != 'n' && ( useIndex < 0 || useIndex >= args.length ) ) {
                if( strIdxBuf.isEmpty() ) throw XCom.newIllegalArgumentException("□$");
                if( !isIndex            ) throw XCom.newIllegalArgumentException("∅$");
                if( useIndex < 0        ) throw XCom.newIllegalArgumentException("≺$");
                                        /*throw XCom.newIllegalArgumentException("∞$");*/
            }

            converted.add( _convertArg(args, useIndex, type) );

            i = j;

        } // for

        return String.format( fmtBuf.toString(), converted.toArray( new Object[0] ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String FORMAT_CHARS = "tcdoxfegabshn%";

    private static boolean _isFormatChar(final char c)
    { return FORMAT_CHARS.indexOf( Character.toLowerCase(c) ) >= 0; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final DateTimeFormatter[] DTF_PATTERNS = {

        // ISO 8601 variants (most specific built-ins first)
        DateTimeFormatter.ISO_DATE_TIME                             , // 2026-01-01T06:00:00Z             (date-time with optional offset, often UTC 'Z')
        DateTimeFormatter.ISO_OFFSET_DATE_TIME                      , // 2026-01-01T06:00:00+00:00        (date-time with offset only)
        DateTimeFormatter.ISO_ZONED_DATE_TIME                       , // 2026-01-01T06:00:00+00:00[GMT]   (date-time with zone ID)
        DateTimeFormatter.ISO_LOCAL_DATE_TIME                       , // 2026-01-01T06:00:00              (date-time without offset/zone)
        DateTimeFormatter.ISO_TIME                                  , // 06:00:00   or 06:00:00+01:00     (time with optional offset, no fractions)
        DateTimeFormatter.ISO_LOCAL_TIME                            , // 06:00:00   or 06:00:00.123456789 (time without offset/zone, fractions allowed)
        DateTimeFormatter.ISO_DATE                                  , // 2026-01-01 or 2026-01-01+01:00   (date with optional offset)
        DateTimeFormatter.ISO_LOCAL_DATE                            , // 2026-01-01                       (date only, no offset/zone)

        // RFC 1123 / HTTP style
        DateTimeFormatter.RFC_1123_DATE_TIME                        , // Thu, 01 Jan 2026 06:00:00 GMT (or Thu, 01 Jan 2026 06:00:00 +0000)

        // Common custom patterns (specific first)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"), // 2026-01-01 06:00:00.123456789 (or shorter fractions if fewer digits)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"   ), // 2026-01-01 06:00:00.123456
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"      ), // 2026-01-01 06:00:00.123
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"       ), // 2026-01-01 06:00:00+00:00     (or +01:00, -05:00)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxx"        ), // 2026-01-01 06:00:00+0000      (or +0100, -0500)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"        ), // 2026-01-01 06:00:00 +0000     (or +0100, -0500)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"        ), // 2026-01-01 06:00:00 GMT       (or PST, CET depending on zone)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss VV"       ), // 2026-01-01 06:00:00 UTC       (or Europe/Paris, America/New_York)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"          ), // 2026-01-01 06:00:00
        DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"        ), // 2026-01-01 06:00:00 AM        (or PM)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"             ), // 2026-01-01 06:00
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"          ), // 01/01/2026 06:00:00
        DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a"           ), // 01/01/2026 06:00 AM           (or PM)
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"             ), // 01/01/2026 06:00
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"          ), // 01/01/2026 06:00:00
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"             ), // 01/01/2026 06:00
        DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss"         ), // Jan 1, 2026 06:00:00
        DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"            ), // Jan 1, 2026 06:00
        DateTimeFormatter.ofPattern("MM/dd/yyyy"                   ), // 01/01/2026
        DateTimeFormatter.ofPattern("dd/MM/yyyy"                   ), // 01/01/2026
        DateTimeFormatter.ofPattern("MMM d, yyyy"                  ), // Jan 1, 2026
        DateTimeFormatter.ofPattern("MM-dd-yyyy"                   ), // 01-01-2026
        DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"          ), // 01-01-2026 06:00:00
        DateTimeFormatter.ofPattern("dd.MM.yyyy"                   ), // 01.01.2026
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"          ), // 01.01.2026 06:00:00

        // Extra patterns
        DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"            ), // 20260101060000123
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"               ), // 20260101060000
        DateTimeFormatter.ofPattern("yyyyMMdd"                     ), // 20260101
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"        ), // 2026-01-01T06:00:00
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"           ), // 2026-01-01T06:00
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"            ), // 20260101T060000
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z"  ), // Thu, 01 Jan 2026 06:00:00 +0000 (or +0100, -0500)
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z"  ), // Thu, 01 Jan 2026 06:00:00 GMT   (or PST, CET)
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss VV" ), // Thu, 01 Jan 2026 06:00:00 UTC   (or Europe/Paris, America/New_York)
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss"    ), // Thu, 01 Jan 2026 06:00:00
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"             ), // Thu, 01 Jan 2026
        DateTimeFormatter.ofPattern("dd-MMM-yyyy"                  ), // 01-Jan-2026
        DateTimeFormatter.ofPattern("dd-MMM-yy"                    ), // 01-Jan-26
        DateTimeFormatter.ofPattern("yyMMdd"                       ), // 260101
        DateTimeFormatter.ofPattern("yy-MM-dd"                     ), // 26-01-01
        DateTimeFormatter.ofPattern("HH:mm:ss"                     ), // 06:00:00
        DateTimeFormatter.ofPattern("HH:mm"                        ), // 06:00
        DateTimeFormatter.ofPattern("HHmmss"                       ), // 060000
        DateTimeFormatter.ofPattern("yyyy-MM"                      ), // 2026-01
        DateTimeFormatter.ofPattern("MM-yyyy"                      ), // 01-2026
        DateTimeFormatter.ofPattern("MMMM yyyy"                    ), // January 2026
        DateTimeFormatter.ofPattern("YYYY-'W'ww"                   ), // 2026-W01
        DateTimeFormatter.ofPattern("YYYY-'W'ww-u"                 ), // 2026-W01-4
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"           ), // Thursday, January 1, 2026
        DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"            ), // Thursday, Jan 1, 2026
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm:ss"  ), // Thursday, January 1, 2026 06:00:00
        DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy HH:mm:ss"   ), // Thursday, Jan 1, 2026 06:00:00
        DateTimeFormatter.ofPattern("yyyy"                         )  // 2026

    };

    private static Object _convertArg(final String[] args, final int idx, final char type)
    {
       if( idx < 0 || idx >= args.length ) return null;

        switch( Character.toLowerCase(type) ) {

            case 't': {
                try {
                    // First try epoch millis
                    return new Date( Long.parseLong( args[idx] ) );
                }
                catch(final NumberFormatException nfe) {
                    // Try each formatter
                    final String dtStr = args[idx];

                    for(final DateTimeFormatter fmt : DTF_PATTERNS) {

                        try {

                            final TemporalAccessor ta = fmt.parse(dtStr);

                            try {
                                final LocalDateTime ldt = LocalDateTime.from(ta);
                                return Date.from( ldt.atZone( ZoneId.systemDefault() ).toInstant() );
                            }
                            catch(final Exception ignored) {}

                            try {
                                final LocalDate ld = LocalDate.from(ta);
                                return Date.from( ld.atStartOfDay( ZoneId.systemDefault() ).toInstant() );
                            }
                            catch(final Exception ignored) {}

                            try {
                                final LocalTime lt = LocalTime.from(ta);
                                return Date.from( lt.atDate(LocalDate.now() ).atZone( ZoneId.systemDefault() ).toInstant() );
                            }

                            catch(final Exception ignored) {}

                        }
                        catch(final Exception ignored) {}

                    } // for
                }
                catch(final Exception ignored) {}

                // Fallback - epoch start
                return new Date(0);
            }

            case 'c': {
                try {
                    final String s = args[idx];
                    if( s.length() == 1 ) return s.charAt(0); // Single character string
                    try {
                        // Try to parse as integer code point
                        final int codePoint = Integer.parseInt(s);
                        return (char) codePoint;
                    }
                    catch(final NumberFormatException nfe) {
                        // Fallback - return the first character if available
                        return s.charAt(0);
                    }
                }
                catch(final Exception e) {
                    // Fallback - null character
                    return '\0';
                }
            }

            case 'd' : /* FALLTHROUGH */
            case 'o' : /* FALLTHROUGH */
            case 'x' : try { return Long   .parseLong   ( args[idx]            ); } catch(final Exception e) { return Long     .valueOf(0    ); }

            case 'f' : /* FALLTHROUGH */
            case 'e' : /* FALLTHROUGH */
            case 'g' : /* FALLTHROUGH */
            case 'a' : try { return Double .parseDouble ( args[idx]            ); } catch(final Exception e) { return Double   .valueOf(0.0  ); }

            case 'b' : try { return Boolean.parseBoolean( args[idx]            ); } catch(final Exception e) { return Boolean  .valueOf(false); }

            case 's' : try { return                       args[idx]             ; } catch(final Exception e) { return ""                      ; }
            case 'h' : try { return Integer.toHexString ( args[idx].hashCode() ); } catch(final Exception e) { return ""                      ; }

            default  : try { return                       args[idx]             ; } catch(final Exception e) { return ""                      ; }

        } // switch
    }

} // class AutoPrintf

