/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxm.*;
import jxm.xb.*;


public class StrUtil {

    private static final String  _psNL    = "[\\n\\r]*";

    private static final Pattern _pmIndex = Pattern.compile("^Index.+?"    + _psNL, Pattern.CASE_INSENSITIVE);
    private static final Pattern _pmLine  = Pattern.compile("^====+"       + _psNL                          );
    private static final Pattern _pmFile  = Pattern.compile("^[-+]{3} .+?" + _psNL                          );
    private static final Pattern _pmHunk  = Pattern.compile("^@@ .+? @@"   + _psNL                          );
    private static final Pattern _pmOld   = Pattern.compile("^-.*?"        + _psNL                          );
    private static final Pattern _pmNew   = Pattern.compile("^\\+.*?"      + _psNL                          );

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_to_cp(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            if( item.value.codePointCount​( 0, item.value.length() ) != 1 ) throw XCom.newJXMRuntimeError(Texts.EMsg_to_cp_MustBeOneChar);
            retVal.add( new XCom.VariableStore( true, String.valueOf( item.value.codePointAt​(0) ) ) );
        }
    }

    public static void _execute_to_ch(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final int cp = XCom.toLong(execBlock, execData, item.value).intValue();
                retVal.add( new XCom.VariableStore( true, new String( Character.toChars(cp) ) ) );
            }
        }
        catch(final IllegalArgumentException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final Pattern _pmGetFCAndRC = Pattern.compile("\\b(\\p{Alpha})(\\p{Alpha}*)", Pattern.UNICODE_CHARACTER_CLASS);

    public static void _execute_ucase(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, item.value.toUpperCase() ) );
        }
    }

    public static void _execute_lcase(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, item.value.toLowerCase() ) );
        }
    }

    public static void _execute_tcase(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        final StringBuffer res = new StringBuffer(); // NOTE : In Java 8, 'appendReplacement()' accepts only 'StringBuffer'

        for( final XCom.VariableStore item : evalVals.get(0) ) {
            final Matcher matcher = _pmGetFCAndRC.matcher( item.value.toLowerCase() );
            while(matcher.find() ) matcher.appendReplacement( res, Matcher.quoteReplacement( matcher.group(1).toUpperCase() + matcher.group(2) ) );
            retVal.add( new XCom.VariableStore( true, matcher.appendTail(res).toString() ) );
            res.setLength(0);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_ltrim(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, XCom.re_ltrim(item.value) ) );
        }
    }

    public static void _execute_rtrim(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, XCom.re_rtrim(item.value) ) );
        }
    }

    public static void _execute_strim(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, item.value.trim() ) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_strlen(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, String.valueOf( item.value.length() ) ) );
        }
    }

    public static void _execute_substr(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the arguments
        final ArrayList<String> str = new ArrayList<>();
        final ArrayList<Long  > beg = new ArrayList<>();
        final ArrayList<Long  > end = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(0) ) str.add(                                  item.value  );
        for( final XCom.VariableStore item : evalVals.get(1) ) beg.add( XCom.toLong(execBlock, execData, item.value) );
        for( final XCom.VariableStore item : evalVals.get(2) ) end.add( XCom.toLong(execBlock, execData, item.value) );

        final int cnt = Math.max( str.size(), Math.max( beg.size(), end.size() ) );

        // Get the substring(s)
        for(int i = 0; i < cnt; ++i) {
            final String s = ( i < str.size() ) ? str.get(i) : str.get( str.size() - 1 );
            final Long   b = ( i < beg.size() ) ? beg.get(i) : beg.get( beg.size() - 1 );
            final Long   e = ( i < end.size() ) ? end.get(i) : end.get( end.size() - 1 );
            retVal.add( new XCom.VariableStore(
                true,
                s.substring(
                    Math.max( 0         ,                                         b.intValue() - 1 ),
                    Math.min( s.length(), e.intValue() <= 0 ? Integer.MAX_VALUE : e.intValue()     )
                )
            ) );
        }
    }

    public static void _execute_str_replace(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        final int cnt = Math.max( evalVals.get(0).size(), Math.max( evalVals.get(1).size(), evalVals.get(2).size() ) );

        for(int i = 0; i < cnt; ++i) {
            final String s = ( i < evalVals.get(0).size() ) ? evalVals.get(0).get(i).value : evalVals.get(0).get( evalVals.get(0).size() - 1 ).value;
            final String c = ( i < evalVals.get(1).size() ) ? evalVals.get(1).get(i).value : evalVals.get(1).get( evalVals.get(1).size() - 1 ).value;
            final String r = ( i < evalVals.get(2).size() ) ? evalVals.get(2).get(i).value : evalVals.get(2).get( evalVals.get(2).size() - 1 ).value;
            retVal.add( new XCom.VariableStore( true, s.replace(c, r) ) );
        }
    }

    public static void _execute_str_xidx(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean lastOccurrence) throws JXMException
    {
        // Get the arguments
        final ArrayList<String> str = new ArrayList<>();
        final ArrayList<String> chk = new ArrayList<>();
        final ArrayList<Long  > frm = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(0) ) str.add(item.value);
        for( final XCom.VariableStore item : evalVals.get(1) ) chk.add(item.value);

        if( evalVals.size() == 3 ) {
            for( final XCom.VariableStore item : evalVals.get(2) ) frm.add( XCom.toLong(execBlock, execData, item.value) );
        }
        else {
            if(lastOccurrence) {
                for(final String s : str) frm.add( Long.valueOf( s.length() ) );
            }
            else {
                frm.add( Long.valueOf(1) );
            }
        }

        final int cnt = Math.max( Math.max( str.size(), chk.size() ), frm.size() );

        // Get the index(es)
        for(int i = 0; i < cnt; ++i) {
            final String s = ( i < str.size() ) ? str.get(i) : str.get( str.size() - 1 );
            final String c = ( i < chk.size() ) ? chk.get(i) : chk.get( chk.size() - 1 );
            final Long   f = ( i < frm.size() ) ? frm.get(i) : frm.get( frm.size() - 1 );
            retVal.add( new XCom.VariableStore(
                true,
                String.valueOf(
                    1 + ( lastOccurrence ? s.lastIndexOf​( c, Math.min( s.length(), f.intValue() <= 0 ? Integer.MAX_VALUE : f.intValue()     ) )
                                         : s.    indexOf( c, Math.max( 0         ,                                         f.intValue() - 1 ) )
                        )
                )
            ) );
        }
    }

    public static void _execute_str_xxxwith(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final boolean endsWith) throws JXMException
    {
        // Get the arguments
        final ArrayList<String> str = new ArrayList<>();
        final ArrayList<String> chk = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(0) ) str.add(item.value);
        for( final XCom.VariableStore item : evalVals.get(1) ) chk.add(item.value);

        final int cnt = Math.max( str.size(), chk.size() );

        // Get the index(es)
        for(int i = 0; i < cnt; ++i) {
            final String s = ( i < str.size() ) ? str.get(i) : str.get( str.size() - 1 );
            final String c = ( i < chk.size() ) ? chk.get(i) : chk.get( chk.size() - 1 );
            retVal.add( new XCom.VariableStore(
                true,
                String.valueOf( ( endsWith ? s.endsWith(c) : s.startsWith​(c) ) ? XCom.Str_T : XCom.Str_F )
            ) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_str_rmxchr(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final boolean lastChar) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            if( item.value.length() <= 1 ) {
                retVal.add( new XCom.VariableStore(true, "") );
            }
            else {
                retVal.add( new XCom.VariableStore(
                    true,
                    lastChar ? item.value.substring( 0, item.value.length() - 1 )
                             : item.value.substring( 1, item.value.length()     )
                ) );
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_re_from_glob(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals)
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, XCom.globToRegExpStr(item.value) ) );
        }
    }

    public static void _execute_re_match(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the arguments and the number of items
        final XCom.VariableValue subject = evalVals.get(0);
        final XCom.VariableValue pattern = evalVals.get(1);
        final int                cnt     = Math.max( subject.size(), pattern.size() );

        if( subject.isEmpty() ) return;

        // Perform regular expression matching
        for(int i = 0; i < cnt; ++i) {
            // Get the arguments
            final String s = ( i < subject.size() ) ? subject.get(i).value : subject.get( subject.size() - 1 ).value;
            final String p = ( i < pattern.size() ) ? pattern.get(i).value : pattern.get( pattern.size() - 1 ).value;
            if( p.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyRegExpStr, "re_match", "<regexp_value>");
            // Perform matching
            final Matcher m = ReCache._reGetMatcher(p, s);
            retVal.add( new XCom.VariableStore( true, m.matches() ? XCom.Str_T : XCom.Str_F ) );
        }
    }

    public static void _execute_re_split(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the arguments and the number of items
        final XCom.VariableValue subject = evalVals.get(0);
        final XCom.VariableValue pattern = evalVals.get(1);
        final int                cnt     = Math.max( subject.size(), pattern.size() );

        if( subject.isEmpty() ) return;

        // Perform regular expression splitting
        for(int i = 0; i < cnt; ++i) {
            // Get the arguments
            final String s = ( i < subject.size() ) ? subject.get(i).value : subject.get( subject.size() - 1 ).value;
            final String p = ( i < pattern.size() ) ? pattern.get(i).value : pattern.get( pattern.size() - 1 ).value;
            if( p.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyRegExpStr, "re_split", "<regexp_value>");
            // Perform splitting
            final Matcher m = ReCache._reGetMatcher(p, s);
            while( m.find() ) {
                if( m.groupCount() != 0 ) {
                    for(int j = 1; j <= m.groupCount(); ++j) {
                        retVal.add( new XCom.VariableStore( true, m.group(j) ) );
                    }
                }
                else {
                    retVal.add( new XCom.VariableStore( true, m.group() ) );
                }
            }
        }
    }

    public static void _execute_re_replace(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the arguments and the number of items
        final XCom.VariableValue subject = evalVals.get(0);
        final XCom.VariableValue pattern = evalVals.get(1);
        final XCom.VariableValue replace = evalVals.get(2);
        final int                cnt     = Math.max( subject.size(), pattern.size() );

        if( subject.isEmpty() ) return;

        // Perform regular expression matching
        for(int i = 0; i < cnt; ++i) {
            // Get the arguments
            final String s = ( i < subject.size() ) ? subject.get(i).value : subject.get( subject.size() - 1 ).value;
            final String p = ( i < pattern.size() ) ? pattern.get(i).value : pattern.get( pattern.size() - 1 ).value;
            final String r = ( i < replace.size() ) ? replace.get(i).value : replace.get( replace.size() - 1 ).value;
            if( p.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyRegExpStr, "re_replace", "<regexp_value>");
            // Perform matching
            final Matcher m = ReCache._reGetMatcher(p, s);
            retVal.add( new XCom.VariableStore( true, m.replaceAll(r) ) );
        }
    }

    public static void _execute_re_quote(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, Pattern.quote(item.value) ) );
        }
    }

    public static void _execute_re_quote_repv(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, Matcher.quoteReplacement(item.value) ) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _execute_color_udiff_impl(final XCom.VariableValue retVal, final StringBuilder sb, final String line)
    {
        final boolean hasNL = line.endsWith("\n") || line.endsWith("\r");

             if( _pmIndex.matcher(line).matches() ) sb.append( XCom.AC_c_magenta() );
        else if( _pmLine .matcher(line).matches() ) sb.append( XCom.AC_c_magenta() );
        else if( _pmFile .matcher(line).matches() ) sb.append( XCom.AC_c_white  () );
        else if( _pmHunk .matcher(line).matches() ) sb.append( XCom.AC_c_cyan   () );
        else if( _pmOld  .matcher(line).matches() ) sb.append( XCom.AC_c_red    () );
        else if( _pmNew  .matcher(line).matches() ) sb.append( XCom.AC_c_green  () );
        else                                        sb.append( XCom.AC_c_lgray  () );
                                                    sb.append( line                );
             if( !hasNL                           ) sb.append( "\n"                );
                                                    sb.append( XCom.AC_c_reset  () );

        retVal.add( new XCom.VariableStore( true, sb.toString() ) );
        sb.setLength(0);
    }

    public static void _execute_color_udiff(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals)
    {
        final StringBuilder sb = new StringBuilder();

        if( evalVals.get(0).size() == 1 ) {
            for( final String item : evalVals.get(0).get(0).value.split("\\r?\\n", -1) ) _execute_color_udiff_impl(retVal, sb, item);
            return;
        }

        for( final XCom.VariableStore item : evalVals.get(0) ) _execute_color_udiff_impl(retVal, sb, item.value);
    }

} // class StrUtil
