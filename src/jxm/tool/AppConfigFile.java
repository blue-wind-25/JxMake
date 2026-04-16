/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import java.lang.ReflectiveOperationException;

import java.util.HashMap;
import java.util.Map;

import java.security.NoSuchAlgorithmException;

import jxm.*;
import jxm.xb.*;


/*
 * Class T must:
 *     1. Be publicly accessible.
 *     2. Implement Serializable.
 *     3. Provide the following methods:
 *            a. public static void writeObjectData(final Object instance, final OutputStreamWriter osw) throws IOException;
 *            b. public static Object readObjectData(final InputStreamReader isr) throws IOException;
 */
public class AppConfigFile<T extends Serializable> {

    private final Class<T> _type;
    private final File     _file;

    public AppConfigFile(final Class<T> type, final String filePath)
    {
        _type = type;
        _file = new File(filePath);
    }

    public boolean fileIs(final String filePath)
    { return _file.getAbsolutePath().equals(filePath); }

    public void save(final T instance) throws IOException
    {
        try(final OutputStreamWriter osw = new OutputStreamWriter( new FileOutputStream(_file) ) ) {
            _type.getMethod( "writeObjectData", Object.class, OutputStreamWriter.class ).invoke(null, instance, osw);
        }
        catch(final ReflectiveOperationException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public T load() throws IOException
    {
        try( final InputStreamReader isr = new InputStreamReader( new FileInputStream(_file) ) ) {
            return (T) _type.getMethod("readObjectData", InputStreamReader.class).invoke(null, isr);
        }
        catch(final ReflectiveOperationException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String DEFAULT_KEY_NAME = "DEFAULT";

    @SuppressWarnings("serial")
    private static final HashMap<Character, String> KEY_NAME_REPLACEMENTS = new HashMap<Character, String>() {{
        put(' '     , "_"      );  // Space
        put('\t'    , "&HRTAB&");  // Horizontal tab
        put('\u000B', "&VRTAB&");  // Vertical tab
        put('.'     , "&_DOT_&");  // Dot
        put('/'     , "&SLASH&");  // Slash
        put('\\'    , "&BSLSH&");  // Backslash
        put(':'     , "&COLON&");  // Colon
        put('*'     , "&ASTER&");  // Asterisk
        put('?'     , "&QUEST&");  // Question mark
        put('"'     , "&DQUOT&");  // Double quote
        put('\''    , "&SQUOT&");  // Single quote
        put('`'     , "&BTICK&");  // Backtick
        put('<'     , "&LESTH&");  // Less than
        put('>'     , "&GRETH&");  // Greater than
        put('|'     , "&VRBAR&");  // Vertical bar (pipe character)
        put('&'     , "&AMPER&");  // Ampersand
    }};

    private static boolean _isSpecialSpace(final char c)
    {
        return c == '\u2007'  // Figure space
            || c == '\u202F'  // Narrow NBSP
            || c == '\u200B'  // Zero-width space
            || c == '\u205F'  // Medium mathematical space
            || c == '\u3000'; // Ideographic space
    }

    public static String makeFileNameKeyFor(final String prefix, final String filePath) throws NoSuchAlgorithmException
    {
        if( DEFAULT_KEY_NAME.equals(filePath) ) {
            return prefix + DEFAULT_KEY_NAME;
        }

        final String absPath = SysUtil.resolveAbsolutePath(filePath);

        final StringBuilder safeName = new StringBuilder();

        for( int i = 0; i < absPath.length(); ++i ) {

            final char ch = absPath.charAt(i);

            if( KEY_NAME_REPLACEMENTS.containsKey(ch) ) {
                safeName.append( KEY_NAME_REPLACEMENTS.get(ch) );
            }
            else if( Character.isWhitespace(ch) || _isSpecialSpace(ch) ) {
                final String hex = String.format( "%04X", (int) ch );
                safeName.append("&U").append(hex).append("&");
            }
            else {
                safeName.append(ch);
            }

        } // for

        safeName.append('_').append( SysUtil.computeStringHash(absPath).substring(0, 8) );

        return prefix + safeName.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean skipMarkerLines(final BufferedReader br, final String strMarkerLines) throws IOException
    {
        final String[] markerLines = strMarkerLines.split("\n", -1);

        // Drop trailing empty strings
        int markerLineCount = markerLines.length;

        while( markerLineCount > 1 && markerLines[markerLineCount - 1].isEmpty()
                                   && markerLines[markerLineCount - 2].isEmpty() ) --markerLineCount;

        for(int i = 0; i < markerLineCount; ++i) {
            if( br.readLine() == null ) return false;
        }

        return true;
    }

} // class AppConfigFile
