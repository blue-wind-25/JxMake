/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.nio.CharBuffer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;

import jxm.annotation.*;


@package_private
class JxMakeTokenMaker_Utils {

    public static int _detect(final char detect, final char[] data, final int curPos, final int endPos)
    {
        if(curPos < 0) return -1;

        if(curPos >= endPos) return -1;

        return (detect == data[curPos]) ? (curPos + 1) : -1;
    }

    public static int _scanws(final char[] data, final int curPos, final int endPos)
    {
        if(curPos < 0) return -1;

        final int max = Math.min(curPos + data.length, endPos);
              int idx = curPos;

        while( idx < max && RSyntaxUtilities.isWhitespace(data[idx]) ) ++idx;

        return (idx > curPos) ? idx : -1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int _detect(final String detect, final char[] data, final int curPos, final int endPos)
    {
        if(curPos < 0) return -1;

        final int max = Math.min(curPos + data.length, endPos);
        final int len = detect.length();

        if(curPos + len > max) return -1;

        for(int i = 0; i < len; ++i) {
            if( data[curPos + i] != detect.charAt(i) ) return -1;
        }

        return curPos + len;
    }

    public static int _findfw(final String detect, final char[] data, final int curPos, final int endPos)
    {
        if(curPos < 0) return -1;

        final int len      = detect.length();
        final int maxStart = Math.min(endPos - len, data.length - len);

        for(int pos = curPos; pos <= maxStart; ++pos) {
            boolean match = true;
            for(int i = 0; i < len; ++i) {
                if( data[pos + i] != detect.charAt(i) ) {
                    match = false;
                    break;
                }
            }
            if(match) return pos + len;
        }

        return -1;
    }

    public static int _findfw(final Character negativeLookbehindChar, final String detect, final char[] data, final int curPos, final int endPos)
    {
        if(curPos < 0) return -1;

        final int len     = detect.length();
        final int maxStart = Math.min(endPos - len, data.length - len);

        for(int pos = curPos; pos <= maxStart; ++pos) {

            boolean match = true;

            for(int i = 0; i < len; ++i) {

                if( data[pos + i] != detect.charAt(i) ) {
                    match = false;
                    break;
                }

            } // for

            if(match) {
                if(negativeLookbehindChar != null) {
                    int chkPos = pos - 1;
                    while(chkPos >= 0 && data[chkPos] == negativeLookbehindChar) --chkPos;
                    if(chkPos < pos - 1) continue; // Match is preceded by one or more 'negativeLookbehindChar' - reject
                }
                return pos + len;
            }

        } // for

        return -1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String  _ent_symname      = "[_a-zA-Z]\\w*";
    public static String  _ent_decnum       = "[+-]?[0-9]+";
    public static String  _ent_hexnum       = "[+-]?0[xX][0-9a-fA-F]+";
    public static String  _ent_hexchr       = "[0-9a-fA-F]";
    public static String  _ent_rvmark       = "[\\^](?=" + _ent_symname + ")";

    /*
    private static Pattern _pmd_MLCOpen     = Pattern.compile("(?<!#)\\(\\*"   ); // ##### !!! TODO : Java does not support "(?<!#.*?)\\(\\*" !!! ###
    private static Pattern _pmd_MLCClose    = Pattern.compile("(?<!#)\\*\\)"   ); // ##### !!! TODO : Java does not support "(?<!#.*?)\\*\\)" !!! ###
    */

    public static Pattern _pmd_WholeWord    = Pattern.compile("[\\p{Alnum}_]+|[^\\p{Alnum}_\\s]+");

    public static Pattern _pmd_SimpleString = Pattern.compile("\"[^\"]*\"|'[^']*'");
    public static Pattern _pmd_SysIncString = Pattern.compile("<[^>]*>");

    public static Pattern _pmd_SymbolName   = Pattern.compile("\\b" + _ent_symname + "\\b");
    public static Pattern _pmd_DecNum       = Pattern.compile("\\b" + _ent_decnum  + "\\b");
    public static Pattern _pmd_HexNum       = Pattern.compile("\\b" + _ent_hexnum  + "\\b");
    public static Pattern _pmd_RVMark       = Pattern.compile(_ent_rvmark);

    public static Pattern _pmd_ESeqSimple   = Pattern.compile("\\\\[tvrnfb'\"\\\\$~^\\-+?]");
    public static Pattern _pmd_ESeqOct3     = Pattern.compile("\\\\o[0-7]{3}");
    public static Pattern _pmd_ESeqDec3     = Pattern.compile("\\\\d[0-9]{3}");
    public static Pattern _pmd_ESeqHex2     = Pattern.compile("\\\\x" + _ent_hexchr + "{2}");
    public static Pattern _pmd_ESeqUni4     = Pattern.compile("\\\\u" + _ent_hexchr + "{4}");
    public static Pattern _pmd_ESeqUni6     = Pattern.compile("\\\\U" + _ent_hexchr + "{6}");

    public static Pattern _pmd_UFuncName    = Pattern.compile(
                                                  (             _ent_symname         ) + "|" +
                                                  ( "\\$\\{"  + _ent_symname + "\\}" ) + "|" +
                                                  ( "\\$\\["  + _ent_symname + "\\]" ) + "|" +
                                                  ( "([\"'])" + _ent_symname + "\\1" )
                                              );

    public static Pattern _pmd_CmdEval      = Pattern.compile("([-+?]|-\\+|\\+-)?@" + _ent_symname);

    public static Pattern _pmd_LineBreak    = Pattern.compile("\\R");
    public static Pattern _pmd_LocalIncVal  = Pattern.compile("\\s*:::s?include\\s+'([^']+)'.*");

    public static int _detect(final Pattern detect, final char[] data, final int curPos, final int endPos)
    {
        if(curPos < 0) return -1;

        final int max       = Math.min(curPos + data.length, endPos);
        final int available = max - curPos;

        if(available <= 0) return -1;

        final CharSequence segment = CharBuffer.wrap(data, curPos, available);
        final Matcher      matcher = detect.matcher(segment);

        if( matcher.lookingAt() ) return curPos + matcher.end();

        return -1;
    }

    public static int _findfw(final Pattern detect, final char[] data, final int curPos, final int endPos)
    {
        if(curPos < 0) return -1;

        final int max       = Math.min(curPos + data.length, endPos);
        final int available = max - curPos;

        if(available <= 0) return -1;

        final CharSequence segment = CharBuffer.wrap(data, curPos, available);
        final Matcher      matcher = detect.matcher(segment);

        if( matcher.find() ) return curPos + matcher.end();

        return -1;
    }

} // class JxMakeTokenMaker_Utils
