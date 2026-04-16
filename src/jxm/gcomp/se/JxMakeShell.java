/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.function.Consumer;
import java.util.function.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Collectors;

import java.util.concurrent.TimeUnit;

import jxm.*;
import jxm.gcomp.*;
import jxm.tool.*;
import jxm.xb.*;

import static jxm.tool.CommandShell.*;
import static jxm.tool.CommandSpecifier.*;
import static jxm.tool.I18N._T;


public class JxMakeShell {

    private final JxMakeConsole                        _console;

    private final StringBuilder                        _sb       = new StringBuilder();

    private final HashMap<String, String>              _varMap   = new HashMap<>();
    private       boolean                              _cmdEcho  = true;

    private final HashMap< String, ArrayList<String> > _aliasMap = new HashMap<>();

    @SuppressWarnings("this-escape")
    public JxMakeShell(final JxMakeConsole console)
    {
        _console = console;

        _loadAllAliases();

        resetState();
    }

    private void _saveAllAliases()
    {
        try(
            final BufferedWriter bw = new BufferedWriter( new FileWriter( JxMakeRootPane.getPath_consoleAlias() ) )
        ) {

            for(final Map.Entry< String, ArrayList<String> > entry : _aliasMap.entrySet() ) {

                bw.write( entry.getKey() );
                bw.write('\n');

                for( final String value : entry.getValue() ) {
                    bw.write(value);
                    bw.write('\n');
                } // for

                // Add a blank line to end the current alias group
                bw.write('\n');

            } // for

        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    private void _loadAllAliases()
    {
        _aliasMap.clear(); // reset before loading

        try (
            final BufferedReader br = new BufferedReader( new FileReader(JxMakeRootPane.getPath_consoleAlias() ) )
        ) {

            String            currentKey    = null;
            ArrayList<String> currentValues = null;
            String            line;

            while( ( line = br.readLine() ) != null ) {

                if( line.isEmpty() ) {
                    // A blank line means the end of the current alias group
                    if( currentKey != null && currentValues != null ) _aliasMap.put(currentKey, currentValues);
                    currentKey    = null;
                    currentValues = null;
                }
                else {
                    if(currentKey == null) {
                        // The first non-empty line is the alias key
                        currentKey = line;
                        currentValues = new ArrayList<>();
                    }
                    else {
                        // Subsequent non-empty lines are the values
                        currentValues.add(line);
                    }
                }

            } // while

            // Handle case where file does not end with blank line
            if( currentKey != null && currentValues != null ) _aliasMap.put(currentKey, currentValues);

        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    private String[] _processAlias(final String[] command)
    {
        final ArrayList<String> av = _aliasMap.get( command[0] );

        if(av == null) return command;

        final String[] merged = new String[ av.size() + command.length - 1 ];

        for(int i = 0; i < av.size()     ; ++i) merged[            i    ] = av.get(i);
        for(int i = 1; i < command.length; ++i) merged[av.size() + i - 1] = command[i];

        return merged;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String OVA_LITERAL   = ":=" ;
    private static final String OVA_CMDSUBS_B = "<-" ; // stdout and stderr
    private static final String OVA_CMDSUBS_1 = "<-1"; // stdout
    private static final String OVA_CMDSUBS_2 = "<-2"; //            stderr

    private static final String OVA_VAREXPN   = "$"  ;

    public static boolean isCmdSubs(final String str)
    { return str.startsWith(OVA_CMDSUBS_B); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _sanitizePath(final String path_) throws IOException
    {
        if(path_ == null) return "";

        String path = SysUtil.normalizeDirectorySeparators(path_);

        if( !path.endsWith(SysUtil._InternalDirSepStr) ) path += SysUtil._InternalDirSep;
        if( !path.endsWith("\n"                      ) ) path += '\n';

        return path;
    }

    private static String _resolvePath(final String path) throws IOException
    { return SysUtil.resolveAbsolutePath(path, true); }

    private static String[] _cmdArrayRemoveNLeft(final int N, final String[] command)
    {
        if(N < 0 || N >= command.length) return null;

        return Arrays.copyOfRange(command, N, command.length);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _EXEC_FUNC_NAME_ = null;

    private static String _msgVarOpr1(final String opr)
    { return "<var> " + opr + " ..."; }

    private static String _msgVarOpr2(final String opr)
    { return "<var1> <var2> " + opr + " ..."; }

    private void _errorBlacklisted(final String name) throws IllegalArgumentException
    { throw XCom.newSecurityException(Texts.ScrEdt_CCBlkListOSC, name); }

    private void _errorMissingOption() throws IllegalArgumentException
    { throw XCom.newIllegalArgumentException(Texts.ScrEdt_CCErrMissOpt, _EXEC_FUNC_NAME_); }

    private void _errorTooManyOptions() throws IllegalArgumentException
    { throw XCom.newIllegalArgumentException(Texts.ScrEdt_CCErrTManyOpt, _EXEC_FUNC_NAME_); }

    private void _errorInvalidOption(final String name) throws IllegalArgumentException
    { throw XCom.newIllegalArgumentException(Texts.ScrEdt_CCErrInvlOpt, _EXEC_FUNC_NAME_, name); }

    private void _errorConflictingOption(final String name) throws IllegalArgumentException
    { throw XCom.newIllegalArgumentException(Texts.ScrEdt_CCErrCnflOpt, _EXEC_FUNC_NAME_, name); }

    private void _errorInvalidVariableName(final String name) throws IllegalArgumentException
    { throw XCom.newIllegalArgumentException(Texts.ScrEdt_CCErrInvlVarN, name); }

    private void _errorReservedVariableName(final String name) throws IllegalArgumentException
    { throw XCom.newIllegalArgumentException(Texts.ScrEdt_CCErrRsrvVarN, name); }

    private void _errorNotSupported(final String detail) throws IllegalArgumentException
    { throw XCom.newIllegalArgumentException(Texts.ScrEdt_CCErrUnsupCmd, _EXEC_FUNC_NAME_, detail); }

    private void _errorSyntaxError(final String detail) throws IllegalArgumentException
    { throw XCom.newIllegalArgumentException(Texts.ScrEdt_CCErrStxError, _EXEC_FUNC_NAME_, detail); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void resetState()
    {
        _cmdEcho = true;

        _resetDelims();
    }

    public boolean getCmdEchoState()
    { return _cmdEcho; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _restrictVarName(final String name) throws IllegalArgumentException
    {
        if( !XCom.isSymbolName(name) ) _errorInvalidVariableName(name);

        switch(name) {

            case "cwd" : /* FALLTHROUGH */
            case "ptd" : /* FALLTHROUGH */
            case "uhd" : /* FALLTHROUGH */
            case "jdd" : /* FALLTHROUGH */
            case "jtd" : /* FALLTHROUGH */
            case "jxd" : /* FALLTHROUGH */
            case "udd" : /* FALLTHROUGH */
            case "add" : /* FALLTHROUGH */

            case "msf" : /* FALLTHROUGH */
            case "pex" : _errorReservedVariableName(name);

        } // switch

        return name;
    }

    private void _delVar(final String name)
    { _varMap.remove(name); }

    private void _putVar(final String name, final String value)
    { _varMap.put( name.trim(), (value == null) ? "" : value ); }

    private String _getSpcVar(final String name) throws IOException
    {
        switch(name) {

            case "cwd" : return _sanitizePath( SysUtil.getCWD               () );
            case "ptd" : return _sanitizePath( SysUtil.getSavedProjectTmpDir() );
            case "uhd" : return _sanitizePath( SysUtil.getUHD               () );
            case "jdd" : return _sanitizePath( SysUtil.getJDD               () );
            case "jtd" : return _sanitizePath( SysUtil.getJTD               () );
            case "jxd" : return _sanitizePath( SysUtil.getJXD               () );
            case "udd" : return _sanitizePath( SysUtil.getUDD               () );
            case "add" : return _sanitizePath( SysUtil.getADD               () );

            case "msf" : return JxMakeRootPane.getPath_mainScriptFile()      + '\n';
            case "pex" : return String.valueOf( _console.getLastExitCode() ) + '\n';

        } // switch

        return null;
    }

    private String _getVar(final String name) throws IllegalArgumentException, IOException
    {
        if( !XCom.isSymbolName(name) ) _errorInvalidVariableName(name);

        final String spc = _getSpcVar(name);
        if(spc != null) return spc;

        final String val = _varMap.get( name.trim() );

        return (val == null) ? "" : val;
    }

    private String[] _expandVar(final String[] rhs) throws IOException
    {
        final String[] res = new String[rhs.length];

        for( int i = 0; i < rhs.length; ++i ) {

            final String token = rhs[i];

            if(token != null) {

                // If it starts with OVA_VAREXPN, it is a variable name
                if( token.startsWith(OVA_VAREXPN) ) {
                    final String varName = token.substring(1);
                    res[i] = _getVar(varName);
                    continue;
                }

                // If it starts with a backslash followed by OVA_VAREXPN, it is not a variable name
                if( token.startsWith('\\' + OVA_VAREXPN) ) {
                    res[i] = token.substring(1);
                    continue;
                }

            }

            res[i] = token;

        } // for

        return res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _stdinEchoEn      = false;
    private String  _captureVarName1  = null;
    private String  _captureVarName2  = null;
    private int     _captureVarStream = -1;

    public boolean stdinEchoEnabled()
    { return _stdinEchoEn; }

    public boolean isCaptureRequired()
    { return _captureVarName1 != null || _captureVarName2 != null; }

    public void acceptCaptureResult(final String[] result)
    {
        final String stdout = result[0];
        final String stderr = result[1];

        // From stdout & stderr
        if(_captureVarStream <= 0) {
            if(_captureVarName1 != null && _captureVarName2 != null) {
                 _putVar( _captureVarName1, XCom.stripTrailingNewlines(stdout         ) );
                 _putVar( _captureVarName2, XCom.stripTrailingNewlines(         stderr) );
            }
            else {
                 _putVar( _captureVarName1, XCom.stripTrailingNewlines(stdout + stderr) );
            }
        }

        // From stdout only
        else if(_captureVarStream == 1) {
            if(_captureVarName1 != null && _captureVarName2 != null) {
                 _putVar( _captureVarName1, XCom.stripTrailingNewlines(stdout         ) );
                 _putVar( _captureVarName2, ""                                          );
            }
            else {
                 _putVar( _captureVarName1, XCom.stripTrailingNewlines(stdout         ) );
            }
        }

        // From stderr only
        else if(_captureVarStream == 2) {
            if(_captureVarName1 != null && _captureVarName2 != null) {
                 _putVar( _captureVarName1, ""                                          );
                 _putVar( _captureVarName2, XCom.stripTrailingNewlines(         stderr) );
            }
            else {
                 _putVar( _captureVarName1, XCom.stripTrailingNewlines(         stderr) );
            }
        }

        _captureVarName1 = null;
        _captureVarName2 = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String NULL_STRING          = "" + null;

    private static final String DEF_ROW_DELIM        = "\\n";
    private static final String DEF_ROW_CONCAT       = "\n";

    private static final String DEF_COL_DELIM        = "\\s+";
    private static final String DEF_COL_CONCAT       = " ";

    private static final String DELIMITER_CANDIDATES = " \n\r\t,;:|#@/\\-_=+()[]{}<>";

    private Pattern _reRowDelim = null;
    private Pattern _reColDelim = null;

    private String  _stRowDelim = null;
    private String  _stColDelim = null;

    private String _extractDelimiterRepresentativeChar(final Pattern p, final String fallback)
    {
        // Try probing against delimiter candidates
        final Matcher m = p.matcher(DELIMITER_CANDIDATES);

        if( m.find() ) return "" + m.group().charAt(0);

        // Parse the regex string
        final String regexStr = p.pattern();

        if( regexStr.contains("|") ) {
            final String first = regexStr.split("\\|", 2)[0];
            return _extractDelimiterRepresentativeChar( Pattern.compile(first), fallback );
        }

        if( regexStr.startsWith("[") && regexStr.endsWith("]") ) {
            return regexStr.substring(1, 2);
        }

        switch(regexStr) {

            case "\\n"  : return "\n";
            case "\\r"  : return "\r";
            case "\\t"  : return "\t";
            case "\\s"  : /* FALLTHROUGH */
            case "\\s+" : return " ";

        } // switch

        return fallback;
    }

    private void _setRowDelim(final String regexStr, final String delimStr)
    {
        _reRowDelim = Pattern.compile(regexStr);
        _stRowDelim = (delimStr != null) ? delimStr : _extractDelimiterRepresentativeChar(_reRowDelim, "\n");
    }

    private void _setRowDelim()
    { _setRowDelim(DEF_ROW_DELIM, DEF_ROW_CONCAT); }

    private String[] _splitRows(final String txt)
    { return _reRowDelim.split(txt, -1);}

    private void _setColDelim(final String regexStr, final String delimStr)
    {
        _reColDelim = Pattern.compile(regexStr);
        _stColDelim = (delimStr != null) ? delimStr : _extractDelimiterRepresentativeChar(_reColDelim, " ");
    }

    private void _setColDelim()
    { _setColDelim(DEF_COL_DELIM, DEF_COL_CONCAT); }

    private String[] _splitCols(final String row)
    {
        final String trow = row.trim();
        return trow.isEmpty() ? new String[0] : _reColDelim.split(row);
    }

    private void _resetDelims()
    {
        _setRowDelim(); // Set the default row delimiter
        _setColDelim(); // Set the default column delimiter
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_deansi(final String[] command)
    {
        _EXEC_FUNC_NAME_ = "deansi";

        _sb.setLength(0);

        boolean outNL = true;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-n" : outNL = true ; break;
                case "-N" : outNL = false; break;

                default   :
                    _sb.append( XCom.stripAllANSIEscapeCode( command[i] ) );
                    if(outNL) _sb.append('\n'); // Do not use '_stRowDelim' here
                    break;

            } // switch


        } // for

        return _sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_rdelim(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "rdelim";

        if(command.length > 4 + 1) _errorTooManyOptions();

        String regexDelim = DEF_ROW_DELIM;
        String plainConca = DEF_ROW_CONCAT;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-d" : regexDelim = DEF_ROW_DELIM;
                            plainConca = DEF_ROW_CONCAT;
                            break;

                case "-s" : regexDelim = Pattern.quote( command[++i] );
                            break;

                case "-r" : regexDelim = command[++i];
                            break;

                case "-c" : plainConca = command[++i];
                            break;

                default   : _errorInvalidOption(str); break;

            } // switch

        } // for

        _sb.setLength(0);

        if(command.length > 1)  {
            _setRowDelim(regexDelim, plainConca);
        }

        else {

            _sb.append( String.format(
                "-r \"%s\" -c \"%s\"",
                XCom.escapeControlChars( _reRowDelim.pattern() ),
                XCom.escapeControlChars( _stRowDelim           )
            ) );

            _sb.append('\n'); // Do not use '_stRowDelim' here
        }

        return _sb.toString();
/*
rdelim -s # -c "\t$\n"
rdelim
rspan -f  1 -t -1 "1#2#3#4#5"
rdelim -d
*/
    }

    private String _exec_cdelim(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "cdelim";

        if(command.length > 4 + 1) _errorTooManyOptions();

        String regexDelim = DEF_COL_DELIM;
        String plainConca = DEF_COL_CONCAT;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-d" : regexDelim = DEF_COL_DELIM;
                            plainConca = DEF_COL_CONCAT;
                            break;

                case "-s" : regexDelim = Pattern.quote( command[++i] );
                            break;

                case "-r" : regexDelim = command[++i];
                            break;

                case "-c" : plainConca = command[++i];
                System.out.println(plainConca);
                            break;

                default   : _errorInvalidOption(str); break;

            } // switch

        } // for

        _sb.setLength(0);

        if(command.length > 1)  {
            _setColDelim(regexDelim, plainConca);
        }

        else {

            _sb.append( String.format(
                "-r \"%s\" -c \"%s\"",
                XCom.escapeControlChars( _reColDelim.pattern() ),
                XCom.escapeControlChars( _stColDelim           )
            ) );

            _sb.append('\n'); // Do not use '_stRowDelim' here
        }

        return _sb.toString();
/*
cdelim -s | -c "\$"
cspan -f  1 -t  3 "A1|B1|C1|D1|E1\nA2|B2|C2|D2|E2\nA3|B3|C3|D3|E3\n"
cspan -f  3 -t  1 "A1|B1|C1|D1|E1\nA2|B2|C2|D2|E2\nA3|B3|C3|D3|E3\n"
cdelim -d

cdelim -s | -c "-"
rdelim -s # -c "\t$\n"
cspan -f  1 -t  3 "A1|B1|C1|D1|E1#A2|B2|C2|D2|E2#A3|B3|C3|D3|E3#"
cspan -f  3 -t  1 "A1|B1|C1|D1|E1#A2|B2|C2|D2|E2#A3|B3|C3|D3|E3#"
cdelim -d
rdelim -d
*/
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_rspan(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "rspan";

        if(command.length < 5 + 1) _errorMissingOption();

        int    f   =  1;
        int    t   = -1;
        String txt = null;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-f" : f = Integer.parseInt( command[++i] ); break;
                case "-t" : t = Integer.parseInt( command[++i] ); break;

                default   : txt = command[i]                    ; break;

            } // switch

        } // for

        final String[] rows = _splitRows(txt);

        if(f < 0) f = Math.max(0              , rows.length + f);
        else      f = Math.min(rows.length - 1, f - 1          );

        if(t < 0) t = Math.max(0              , rows.length + t);
        else      t = Math.min(rows.length - 1, t - 1          );

        _sb.setLength(0);

        if(f <= t) {
            for(int i = f; i <= t; ++i) {
                if( _sb.length() > 0 ) _sb.append( _stRowDelim );
                                       _sb.append( rows[i]     );

            }
        }
        else {
            for(int i = f; i >= t; --i) {
                if( _sb.length() > 0 ) _sb.append( _stRowDelim );
                                       _sb.append( rows[i]     );
            }
        }

        if( _sb.length() > 0 ) _sb.append(_stRowDelim);

        return _sb.toString();

/*
rspan -f  1 -t -1 "1\n2\n3\n4\n5"
rspan -f -1 -t  1 "1\n2\n3\n4\n5"

rspan -f  1 -t  3 "1\n2\n3\n4\n5"
rspan -f  3 -t  1 "1\n2\n3\n4\n5"

rspan -f -3 -t -1 "1\n2\n3\n4\n5"
rspan -f -1 -t -3 "1\n2\n3\n4\n5"
*/
    }

    private String _exec_cspan(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "cspan";

        if(command.length < 5 + 1) _errorMissingOption();

        int    f   =  1;
        int    t   = -1;
        String txt = null;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-f" : f = Integer.parseInt( command[++i] ); break;
                case "-t" : t = Integer.parseInt( command[++i] ); break;

                default   : txt = command[i]                    ; break;

            } // switch

        } // for

        _sb.setLength(0);

        for( final String row : _splitRows(txt) ) {

            final String[] cols = _splitCols(row);
                  int      fc   = f;
                  int      tc   = t;

            if(fc < 0) fc = Math.max(0              , cols.length + fc);
            else       fc = Math.min(cols.length - 1, fc - 1          );

            if(tc < 0) tc = Math.max(0              , cols.length + tc);
            else       tc = Math.min(cols.length - 1, tc - 1          );

            if(fc < 0 || tc < 0) continue;

            if( _sb.length() > 0 ) _sb.append(_stRowDelim);

            if(fc <= tc) {
                for(int i = fc; i <= tc; ++i) {
                                _sb.append( cols[i]     );
                    if(i != tc) _sb.append( _stColDelim );
                }
            }
            else {
                for(int i = fc; i >= tc; --i) {
                                _sb.append( cols[i]     );
                    if(i != tc) _sb.append( _stColDelim );
                }
            }

        } // for

        if( _sb.length() > 0 ) _sb.append(_stRowDelim);

        return _sb.toString();

/*
cspan -f  1 -t -1 "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n"
cspan -f -1 -t  1 "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n"

cspan -f  1 -t  3 "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n"
cspan -f  3 -t  1 "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n"

cspan -f -3 -t -1 "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n"
cspan -f -1 -t -3 "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n"

A <- ls -l
A <- deansi $A
cspan -f 1 -t 1 $A
cspan -f 3 -t 3 $A
*/
    }

    private String _exec_rmerge(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "rmerge";

        if(command.length < 2 + 1) _errorMissingOption();

        _sb.setLength(0);

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String txt = command[i];

            for( final String row : _splitRows(txt) ) {

                if( _sb.length() > 0 ) _sb.append(_stRowDelim);
                                       _sb.append(row        );

            } // for

        } // for

        if( _sb.length() > 0 ) _sb.append(_stRowDelim);

        return _sb.toString();

/*
A := "1\n2\n3"
B := "4\n5"
C <- echo $A $B
echo $C

D <- rmerge $A $B
echo $D
*/
    }

    private String _exec_cmerge(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "cmerge";

        if(command.length < 2 + 1) _errorMissingOption();

        final ArrayList<String[][]> textRC  = new ArrayList<>();
        final int[]                 textCol = new int[command.length - 1];
              int                   maxRow  = 0;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String     text   = command[i];
            final String[]   rows   = _splitRows(text);
            final String[][] cols   = new String[rows.length][];
                  int        maxCol = 0;

            for(int r = 0; r < rows.length; ++r) {
                cols[r] = _splitCols( rows[r] );
                maxCol  = Math.max(maxCol, cols[r].length);
            }

            textRC.add(cols);
            textCol[i - 1] = Math.max(textCol[i - 1], maxCol     );
            maxRow         = Math.max(maxRow        , rows.length);

        } // for

        _sb.setLength(0);

        for(int r = 0; r < maxRow; ++r) {

            if( _sb.length() > 0 ) _sb.append(_stRowDelim);

            for( int t = 0; t < textRC.size(); ++t )  {

                final String[][] rc   = textRC.get(t);
                final String[]   cols = (r >= rc.length) ? null : rc[r];

                if(cols == null) {
                    for(int c = 0; c < textCol[t]; ++c) {
                        if(t > 0 || c > 0) _sb.append(_stColDelim);
                                           _sb.append(NULL_STRING);
                    } // for c
                }
                else {

                    for(int c = 0; c < textCol[t]; ++c) {
                        final String cStr = (c >= cols.length) ? NULL_STRING : cols[c];
                        if(t > 0 || c > 0) _sb.append(_stColDelim);
                                           _sb.append(cStr       );
                    } // for c
                }

            } // for t

        } // for r

        if( _sb.length() > 0 ) _sb.append(_stRowDelim);

        return _sb.toString();

/*
cmerge "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3"   "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3"
cmerge "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n" "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3\n"

cmerge "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3"   "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3"   "#1 #1 #1 #1 #1\n#2 #2 #2 #2 #2"
cmerge "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n" "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3\n" "#1 #1 #1 #1 #1\n#2 #2 #2 #2 #2\n"

cmerge "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3"   "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3"   "#1 #1 #1 #1 #1\n#2 #2 #2 #2 #2\n#3 #3 #3 #3 #3"
cmerge "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3"   "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3"   " #1 #1 #1\n#2 #2 #2 #2 #2\n#3 #3 #3 #3\n#X #X #X"
*/
    }

    private String _exec_xjoin_impl(final String[] command_, final int mode) throws IllegalArgumentException
    {
        if( command_.length < 4 + 1 ) _errorMissingOption();

        // Check for flag(s) and remove them
        final Set <String> flags      = new HashSet<>( Arrays.asList("-i", "-k", "-u", "-l") );
        final List<String> args       = new ArrayList<>( Arrays.asList(command_) );
        final Set <String> foundFlags = new HashSet<>(args);

        foundFlags.retainAll(flags);
        final boolean caseInsensitive = foundFlags.contains("-i");
        final boolean printKeyOCase   = foundFlags.contains("-k");
        final boolean printKeyUCase   = foundFlags.contains("-u");
        final boolean printKeyLCase   = foundFlags.contains("-l");

        final List<String> activeFlags = new ArrayList<>();
        if(printKeyOCase) activeFlags.add("-k");
        if(printKeyUCase) activeFlags.add("-u");
        if(printKeyLCase) activeFlags.add("-l");
        if( activeFlags.size() > 1 ) _errorConflictingOption( String.join("/", activeFlags) );

        args.removeAll(flags);
        final String[] command = args.toArray( new String[0] );

        if( command.length < 4 + 1 || (command.length % 2) == 0 ) _errorMissingOption();

        // Extract the data
        final int                                    expectedSize = (command.length - 1) / 2;
        final ArrayList< HashMap<String, String[]> > textMap      =                     new ArrayList<>(expectedSize)       ;
        final ArrayList< HashMap<String, String[]> > textMapUCase = caseInsensitive  ?  new ArrayList<>(expectedSize) : null;
        final int[]                                  textNumCol   =                     new int        [expectedSize]       ;
        final ArrayList< Integer                   > keyCol       =                     new ArrayList<>(expectedSize)       ;
        final HashSet  < String                    > allKeys      = (mode != 0      ) ? new HashSet  <>(            ) : null;

        for(int i = 1; i < command.length; i += 2) { // Options -> alternate : key column - text

            final int                       kCol   = Integer.parseInt( command[i] ) - 1;

            final String                    text     = command[i + 1];
            final String[]                  rows     = _splitRows(text);
            final HashMap<String, String[]> map      =                   new HashMap<>(rows.length)       ;
            final HashMap<String, String[]> mapUCase = caseInsensitive ? new HashMap<>(rows.length) : null;
                  int                       maxCol   = 0;

            for(int r = 0; r < rows.length; ++r) {

                final String[] cols = _splitCols( rows[r] );

                if(kCol < cols.length) {
                    final String key      = cols[kCol];
                    final String keyUCase = key.toUpperCase();
                                        map     .put(                   key     , cols );
                    if(caseInsensitive) mapUCase.put(                   keyUCase, cols );
                    if(mode != 0      ) allKeys .add( caseInsensitive ? keyUCase : key );
                }

                if(cols.length > maxCol) maxCol = cols.length;

            } // for r

                                textMap     .add(map     );
            if(caseInsensitive) textMapUCase.add(mapUCase);
                                keyCol      .add(kCol    );

            textNumCol[ (i - 1) / 2 ] = maxCol;

        } // for i

        Set<String> effKey = allKeys;

        if(mode == 0) {
            // Inner join
            final ArrayList< HashMap<String, String[]> > selMap = caseInsensitive ? textMapUCase : textMap;
            effKey = new HashSet<>( selMap.get(0).keySet() );
            for( int t = 1; t < selMap.size(); ++t ) effKey.retainAll( selMap.get(t).keySet() );
        }
        else if(mode == 1) {
            // Outer join - nothing to do here
        }
        else if(mode == 2) {
            // Difference join
            final ArrayList< HashMap<String, String[]> > selMap       = caseInsensitive ? textMapUCase : textMap;
            final Set      < String                    > intersection = new HashSet<>( selMap.get(0).keySet() );
            for( int t = 1; t < selMap.size(); ++t ) intersection.retainAll( selMap.get(t).keySet() );
            effKey.removeAll(intersection);
        }

        // Perform the join
        _sb.setLength(0);

        for(final String key : effKey) {

            if( _sb.length() > 0 ) _sb.append(_stRowDelim);

            // Put the key
            if(printKeyUCase) {
                _sb.append( key.toUpperCase() );
            }
            else if(printKeyLCase) {
                _sb.append( key.toLowerCase() );
            }
            else {
                if(caseInsensitive) {
                    String origKey = null;
                    for( final HashMap<String, String[]> map : textMap ) {
                        for( final String k : map.keySet() ) {
                            if( !k.equalsIgnoreCase(key) ) continue;
                            origKey = k;
                            break;
                        } // for
                        if(origKey != null) break;
                    } // for
                    _sb.append( (origKey != null) ? origKey : key );
                }
                else {
                    _sb.append(key);
                }
            }

            // Put the value
            for(int t = 0; t < textMap.size(); ++t) {

                final HashMap<String, String[]> map  = caseInsensitive ? textMapUCase.get(t) : textMap.get(t);
                final String[]                  cols = map.get(key);
                final int                       kCol = keyCol.get(t);
                final int                       numC = textNumCol[t];

                if(cols == null) {
                    for(int c = 0; c < numC; ++c) {
                        if(c == kCol) continue; // Skip key column
                        _sb.append(_stColDelim).append(NULL_STRING);
                    } // for c
                }
                else {
                    for(int c = 0; c < numC; ++c) {
                        if(c == kCol) continue; // Skip key column
                        _sb.append(_stColDelim).append( (c >= cols.length) ? NULL_STRING : cols[c] );
                    } // for c
                }

            } // for t

        } // for key

        if( _sb.length() > 0 ) _sb.append(_stRowDelim);

        return _sb.toString();
    }

    private String _exec_icjoin(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "icjoin";

        return _exec_xjoin_impl(command, 0);
/*
icjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3"
icjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3"

icjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9"
icjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9"

icjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3" 3 "M1 N1 K1\nM2 N2 K2\nM3 N3 K3"
icjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3" 3 "M1 N1 K1\nM2 N2 K2\nM3 N3 K3"

icjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9" 3 "M1 N1 K1\nM2 N2 K2\nM9 N9 K9"
icjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9" 3 "M1 N1 K1\nM2 N2 K2\nM9 N9 K9"
*/
    }

    private String _exec_ocjoin(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "ocjoin";

        return _exec_xjoin_impl(command, 1);
/*
ocjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3"
ocjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3"

ocjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9"
ocjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9"

ocjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3" 3 "M1 N1 K1\nM2 N2 K2\nM3 N3 K3"
ocjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3" 3 "M1 N1 K1\nM2 N2 K2\nM3 N3 K3"

ocjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9" 3 "M1 N1 K1\nM2 N2 K2\nM9 N9 K9"
ocjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9" 3 "M1 N1 K1\nM2 N2 K2\nM9 N9 K9"
*/
    }

    private String _exec_dcjoin(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "dcjoin";

        return _exec_xjoin_impl(command, 2);
/*
dcjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3"
dcjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3"

dcjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9"
dcjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9"

dcjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3" 3 "M1 N1 K1\nM2 N2 K2\nM3 N3 K3"
dcjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX3 Y3 K3" 3 "M1 N1 K1\nM2 N2 K2\nM3 N3 K3"

dcjoin    1 "K1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9" 3 "M1 N1 K1\nM2 N2 K2\nM9 N9 K9"
dcjoin -i 1 "k1 A1 B1\nK2 A2 B2\nK3 A3 B3" 3 "X1 Y1 K1\nX2 Y2 K2\nX9 Y9 K9" 3 "M1 N1 K1\nM2 N2 K2\nM9 N9 K9"
*/
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_fgrid(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "fgrid";

        if(command.length < 1 + 1) _errorMissingOption();

        // Process the arguments
        boolean alignLeft = true;

        _sb.setLength(0);

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-l" : alignLeft = true ; break;
                case "-r" : alignLeft = false; break;

                default   : if( _sb.length() > 0 ) _sb.append(_stRowDelim);
                                                   _sb.append(str        );
                            break;

            } // switch

        } // for i

        if( _sb.length() == 0 ) return "";

        // Extract the columns
        final String[]   rows   = _splitRows( _sb.toString() );
        final String[][] cols   = new String[rows.length][];
              int        colCnt = 0;

        for(int r = 0; r < rows.length; ++r) {

            cols[r] = _splitCols( rows[r] );
            colCnt  = Math.max( colCnt, cols[r].length );

        } // for i

        // Determine the size of each column
        final int[] colLen = new int[colCnt];

        for(int r = 0; r < rows.length; ++r) {

            for(int c = 0; c < colCnt; ++c) {

                if( c >= cols[r].length ) {
                    colLen[c] = Math.max( colLen[c], NULL_STRING.length() );
                    continue;
                }

                colLen[c] = Math.max( colLen[c], cols[r][c].length() );

            } // for c

        } // for r

        // Generate the format
        final String[] colFmt = new String[colCnt];

        for(int c = 0; c < colCnt; ++c) {

            colFmt[c] = "%" + (alignLeft ? -colLen[c] : colLen[c]) + "s";

        } // for c

        // Process the columns
        _sb.setLength(0);

        for(int r = 0; r < rows.length; ++r) {

            if( _sb.length() > 0 ) _sb.append(_stRowDelim);

            for(int c = 0; c < colCnt; ++c) {

                _sb.append( String.format( colFmt[c], ( c >= cols[r].length ) ? NULL_STRING : cols[r][c] ) );
                _sb.append( _stColDelim );

            } // for c

        } // for r

        if( _sb.length() > 0 ) _sb.append(_stRowDelim);

        return _sb.toString();

/*
fgrid    "A1 B1 C1 D1 E1\nA22 B2 C2 D222 E2\nA3 B3 C3 D3 E3" "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3"
fgrid -l "A1 B1 C1 D1 E1\nA22 B2 C2 D222 E2\nA3 B3 C3 D3 E3" "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3"
fgrid -r "A1 B1 C1 D1 E1\nA22 B2 C2 D222 E2\nA3 B3 C3 D3 E3" "V1 W1 X1 Y1 Z1\nV2 W2 X2 Y2 Z2\nV3 W3 X3 Y3 Z3"
*/
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_filter(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "filter";

        if(command.length < 2 + 1) _errorMissingOption ();
        if(command.length > 2 + 1) _errorTooManyOptions();

        final String pat = command[1]; // Start from index 1 to skip the command string
        final String txt = command[2]; // ---

        _sb.setLength(0);

        // Precompute matcher logic
        Pattern regexPattern   = null;
        String  replacement    = null;
        boolean isRegexMatch   = false;
        boolean isRegexReplace = false;
        boolean requiresGroups = false;
        boolean wholeTextMode  = false;

        final Function<String, Integer> flagMapper = f -> {
            int bits = 0;
            if( f.contains("i") ) bits |= Pattern.CASE_INSENSITIVE;
            if( f.contains("u") ) bits |= Pattern.UNICODE_CASE;
            if( f.contains("U") ) bits |= Pattern.UNICODE_CHARACTER_CLASS;
            if( f.contains("c") ) bits |= Pattern.CANON_EQ;
            if( f.contains("s") ) bits |= Pattern.DOTALL;
            if( f.contains("m") ) bits |= Pattern.MULTILINE;
            if( f.contains("l") ) bits |= Pattern.LITERAL;
            if( f.contains("x") ) bits |= Pattern.COMMENTS;
            return bits;
        };

        if( !pat.startsWith("/") || pat.indexOf('/', 1) == -1 ) {
            // Plain string - nothing to do here
        }
        else {
            final String[] parts = pat.split("/", -1);
            if(parts.length == 3) {
                // "/pattern/flags"
                final String regex    = parts[1];
                final String flags    = parts[2];
                final int    flagBits = flagMapper.apply(flags);
                regexPattern  = Pattern.compile(regex, flagBits);
                isRegexMatch  = true;
                wholeTextMode = flags.contains("M");
            }
            else if(parts.length == 4) {
                // "/pattern/replacement/flags"
                final String regex    = parts[1];
                final String flags    = parts[3];
                final int    flagBits = flagMapper.apply(flags);
                regexPattern   = Pattern.compile(regex, flagBits);
                replacement    = parts[2];
                isRegexReplace = true;
                requiresGroups = replacement.contains("$");
                wholeTextMode  = flags.contains("M");
            }
            else {
                _errorInvalidOption(pat);
            }
        }

        for( final String row : wholeTextMode ? ( new String[] { txt } ) : _splitRows(txt) ) {

            if(regexPattern == null) {
                // Plain string
                if( row.contains(pat) ) _sb.append(row).append(_stRowDelim);
            }

            else if(isRegexMatch) {
                final Matcher m = regexPattern.matcher(row);
                if( m.find() ) _sb.append(row).append(_stRowDelim);
            }

            else if(isRegexReplace) {
                final Matcher m = regexPattern.matcher(row);

                if(requiresGroups) {
                    if( regexPattern.matcher("").groupCount() == 0 ) {
                        _sb.append("").append(_stRowDelim);
                    }
                    else {
                        final String replaced = m.replaceAll(replacement);
                        _sb.append(replaced).append(_stRowDelim);
                    }
                }
                else {
                    final String replaced = m.replaceAll(replacement);
                    _sb.append(replaced).append(_stRowDelim);
                }
            }

        } // for

        return _sb.toString();

/*
filter abc "123\n456\n123 abc\nabc 456\nabc\n789\n"

filter abc "123\n456\n123 aBc\nabc 456\naXc\n789\n"
filter /a[bx]c/i "123\n456\n123 aBc\nabc 456\naXc\n789\n"

filter /a[bx]c/#/ "123\n456\n123 aBc\nabc 456\naXc\n789\n"
filter /a[bx]c/#/i "123\n456\n123 aBc\nabc 456\naXc\n789\n"

filter /(.*?a)[bx](c.*?)/$1#$2/ "123\n456\n123 aBc\nabc 456\naXc\n789\n"
filter /(.*?a)[bx](c.*?)/$1#$2/iu "123\n456\n123 aBc\nabc 456\naXc\n789\n"


filter /789/M "123\n456\n123"
filter /789/M "123\n789\n123"

filter /(.*?)789\n(.*?)/$1$2/M "123\n456\n123"
filter /(.*?)789\n(.*?)/$1$2/M "123\n789\n123"

filter /(789)/$1/m "123\n789\n123"
filter /(789)/$2/m "123\n789\n123"
*/
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_printf(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "printf";

        if(command.length < 1 + 1) _errorMissingOption ();

        final String fmt = command[1]; // Start from index 1 to skip the command string

        return AutoPrintf.format(
            fmt,
            (command.length <= 2) ? ( new String[0] ) : Arrays.copyOfRange(command, 2, command.length)
        );

/*
printf "Sequential : %d %f %s %%\n" 42 3.14 hello
printf "Indexed    : %2$s %1$d %1$f %% %n" 42 hello
*/
    }

    private String _exec_cprintf(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "cprintf";

        if(command.length < 2 + 1) _errorMissingOption ();
        if(command.length > 2 + 1) _errorTooManyOptions();

        final String fmt = command[1]; // Start from index 1 to skip the command string
        final String txt = command[2];

        _sb.setLength(0);

        for( final String row : _splitRows(txt) ) {

            final String[] cols = _splitCols(row);

            _sb.append( AutoPrintf.format(fmt, cols) );

        } // for

        return _sb.toString();

/*
cprintf '%10s %10s %10s %10s %10s %n' "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3"
cprintf '%10s %10s %10s %10s %10s %n' "A1 B1 C1 D1 E1\nA2 B2 C2 D2 E2\nA3 B3 C3 D3 E3\n"
*/
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_cmdecho(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "cmdecho";

        if(command.length < 1 + 1) _errorMissingOption();

        _sb.setLength(0);

        boolean outNL = true;
        boolean prcES = true;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "on"    : _cmdEcho = true ; break;
                case "off"   : _cmdEcho = false; break;

                case "state" : _sb.append(_cmdEcho ? "on\n" : "off\n"); break;

                default      : _errorInvalidOption(str); break;

            } // switch

        } // for

        return _sb.toString();
    }

    private String _exec_echo(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "echo";

        _sb.setLength(0);

        boolean outNL = true;
        boolean prcES = false;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-n" : outNL = true ; break;
                case "-N" : outNL = false; break;

                case "-e" : prcES = true ; break;
                case "-E" : prcES = false; break;

                default   :
                    /*
                    if( str.length() > 1 && str.startsWith("-") ) _errorInvalidOption(str);
                    //*/

                    if(prcES) _sb.append( XCom.unescapeString(str) );
                    else      _sb.append(                     str  );
                    if(outNL) _sb.append( '\n'                     ); // Do not use '_stRowDelim' here

                    break;

            } // switch

        } // for

        return _sb.toString();
    }

    private String _exec_echo_noNL(final String command) throws IllegalArgumentException
    { return _exec_echo( new String[] { null, "-N", command } ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    final static DateTimeFormatter _dtfDate     = DateTimeFormatter.ofPattern("yyyy-MM-dd"         );
    final static DateTimeFormatter _dtfTime     = DateTimeFormatter.ofPattern("HH:mm:ss"           );
    final static DateTimeFormatter _dtfDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String _exec_date(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "date";

        DateTimeFormatter dtf = _dtfDateTime;
        boolean           utc = false;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-s"  : dtf = null        ; break;
                case "-d"  : dtf = _dtfDate    ; break;
                case "-t"  : dtf = _dtfTime    ; break;
                case "-dt" : dtf = _dtfDateTime; break;

                case "-u"  : utc = true        ; break;
                case "-l"  : utc = false       ; break;

                default    : _errorInvalidOption(str); break;

            } // switch

        } // for

        _sb.setLength(0);

        if(dtf == null) _sb.append( String.valueOf( Instant.now().getEpochSecond() )                                 );
        else            _sb.append( ZonedDateTime.now( utc ? ZoneId.of("UTC") : ZoneId.systemDefault() ).format(dtf) );

        _sb.append('\n'); // Do not use '_stRowDelim' here

        return _sb.toString();
    }

    private String _exec_env(final String[] command)
    {
        _EXEC_FUNC_NAME_ = "env";

        _sb.setLength(0);

        int maxKeyLen = 0;
        for( final Map.Entry<String, String> entry : _console.getEffEnvVar().entrySet() ) {
            maxKeyLen = Math.max( maxKeyLen, entry.getKey().length() );
        }

        final String format = "%-" + maxKeyLen + "s";
        for( final Map.Entry<String, String> entry : _console.getEffEnvVar().entrySet() ) {

            final String keyStr = String.format( format, entry.getKey() );

                  String valStr = XCom.escapeControlChars( entry.getValue() );
                         valStr = valStr.contains("\\") ? ('"' + valStr + '"')
                                                        : XCom.re_quoteSQIfContainsWhitespace(valStr);

            _sb.append( keyStr      );
            _sb.append( ' '         ); // Do not use '_stColDelim' here
            _sb.append( OVA_LITERAL );
            _sb.append( ' '         ); // Do not use '_stColDelim' here
            _sb.append( valStr      );
            _sb.append( '\n'        ); // Do not use '_stRowDelim' here

        } // for

        return _sb.toString();
    }

    private String _exec_history(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "history";

        boolean clear = false;
        int     df    = -1;
        int     dl    = -1;

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-c"  : clear = true                            ; break;

                case "-df" : df    = Integer.parseInt( command[++i] ); break;
                case "-dl" : dl    = Integer.parseInt( command[++i] ); break;

                default    : _errorInvalidOption(str); break;

            } // switch

        } // for

        if(clear) {
             _console.commandHistory().clearAll();
             return "";
        }

        else if(df > 0 || dl > 0) {
             if(df > 0) _console.commandHistory().deleteFirstN(df);
             if(dl > 0) _console.commandHistory().deleteLastN (dl);
             return "";
        }

        // Print all history entries
        _sb.setLength(0);

        final List<String> history  = _console.commandHistory().getAll();
        final int          maxIndex = history.size();
        final int          width    = String.valueOf(maxIndex).length();
        final String       format   = "%0" + width + "d   ";

        for( int i = 0; i < history.size(); ++i ) {

            String valStr = XCom.escapeControlChars( history.get(i) );
                   valStr = valStr.contains("\\") ? ('"' + valStr + '"')
                                                  : XCom.re_quoteStringSQ(valStr);

            _sb.append( String.format(format, i + 1) );
            _sb.append( valStr                       );
            _sb.append( '\n'                         ); // Do not use '_stRowDelim' here

        } // for

        return _sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _prevDir = null;

    private String _exec_chdir(final String[] command) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "chdir";

        if(command.length < 1 + 1) _errorMissingOption ();
        if(command.length > 1 + 1) _errorTooManyOptions();

        String path = command[1]; // Use index 1 to skip the command string

        if( "-".equals(path) ) {
            if(_prevDir == null) return "";
            path = _prevDir;
        }

        _prevDir = SysUtil.getCWD();

        SysUtil.setCWD(path);

        return "";
    }

    private String _exec_mkdir(final String[] command) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "mkdir";

        if(command.length < 1 + 1) _errorMissingOption();

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String path = _resolvePath( command[i] );

            SysUtil.cu_mkdir(path);

        } // for

        return "";
    }

    private String _exec_rmdir(final String[] command) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "rmdir";

        if(command.length < 1 + 1) _errorMissingOption();

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String path = _resolvePath( command[i] );

            SysUtil.cu_rmdir_empty_recursive_strict(path);

        } // for

        return "";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_rmfile(final String[] command) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "rmfile";

        if(command.length < 1 + 1) _errorMissingOption();

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String path = _resolvePath( command[i] );

            SysUtil.cu_rmfile_strict(path);

        } // for

        return "";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_upfile(final String[] command) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "upfile";

        if(command.length < 1 + 1) _errorMissingOption();

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final Path path = Paths.get( _resolvePath( command[i] ) );

            if( Files.exists(path) ) {
                final FileTime now = FileTime.from( System.currentTimeMillis(), TimeUnit.MILLISECONDS );
                try {
                    Files.getFileAttributeView(path, BasicFileAttributeView.class).setTimes(now, now, now);
                }
                catch(final UnsupportedOperationException e) {
                    Files.setLastModifiedTime(path, now);
                }
            }

            else {
                Files.createFile(path);
            }

        } // for

        return "";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_ls(final String[] command, final boolean oneColumn) throws IOException
    {
        _EXEC_FUNC_NAME_ = "ls";

        boolean longMode = false;
        String  path     = "";

        for(int i = 1; i < command.length; ++i) { // Start from index 1 to skip the command string

            final String str = command[i];

            switch(str) {

                case "-s"  : longMode = false; break;
                case "-l"  : longMode = true ; break;

                default    : path     = str  ; break;

            } // switch

        } // for

        path = _resolvePath(path);

        _console.showGlobalWaitCursorFM();

        try {
            return CommandShell.ls(
                path, longMode, new ConfigLS.Default( oneColumn ? 0 : _console.getConsoleColumns() )
            );
        }
        finally {
            _console.showGlobalDefaultCursorFM();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_jxmakecmd(final String[] command) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "jxmakecmd";

        final ArrayList<String> cmd = SysUtil.getJavaCmd(false);

        for( int i = 0; i < cmd.size(); ++i ) {

            final String cmdStr = XCom.escapeControlChars( cmd.get(i) );

            cmd.set(
                i,
                cmdStr.contains("\\") ? ('"' + cmdStr + '"')
                                      : XCom.re_quoteSQIfContainsWhitespace(cmdStr)
            );

        } // for

        return String.join(" ", cmd) + '\n';  // Do not use '_stRowDelim' here
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _exec_alias(final String[] command) throws IllegalArgumentException
    {
        _EXEC_FUNC_NAME_ = "alias";

        boolean done = false;

        if(command.length > 1) {

            final String opt = command[1];

            switch(opt) {

                case "-c" : {
                        if(command.length > 1 + 1) _errorTooManyOptions();
                        _aliasMap.clear();
                        done = true;
                    }
                    break;

                case "-d" : {
                        if(command.length < 2 + 1) _errorMissingOption ();
                        if(command.length > 2 + 1) _errorTooManyOptions();
                        _aliasMap.remove( command[2] );
                        done = true;
                    }
                    break;

                case "-a" : {
                        if(command.length < 3 + 1) _errorMissingOption();
                        final ArrayList<String> values = new ArrayList<>();
                        for(int i = 3; i < command.length; ++i) values.add(command[i]);
                        _aliasMap.put( command[2], values );
                        done = true;
                    }
                    break;

                default   : _errorInvalidOption(opt); break;

            } // switch

        } // if

        if(done) {
            _saveAllAliases();
            return "";
        }

        // Print all aliases
        _sb.setLength(0);

        int keyWidth = 0;

        for( String key : _aliasMap.keySet() ) {

            if( key.length() > keyWidth ) keyWidth = key.length();

        } // for

        keyWidth += 3;

        final String fmt = "%-" + keyWidth + "s";

        for(final Map.Entry< String, ArrayList<String> > entry : _aliasMap.entrySet() ) {

            _sb.append( String.format( fmt, entry.getKey() ) );

            for(final String value : entry.getValue() ) {

                _sb.append(value      );
                _sb.append(_stColDelim);

            } // for

            _sb.append('\n'); // Do not use '_stRowDelim' here

        } // for

        return _sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Set<String> _BIC = new HashSet<>();

    static {

        // NOTE : Always keep this list synchronized with the actual command implementation
        //        in the '_execute_impl()' and '_translate_impl()' functions!

        _BIC.add("help"     );

        _BIC.add("clear"    );
        _BIC.add("reset"    );

        _BIC.add("deansi"   );

        _BIC.add("rdelim"   );
        _BIC.add("cdelim"   );

        _BIC.add("rspan"    );
        _BIC.add("cspan"    );
        _BIC.add("rmerge"   );
        _BIC.add("cmerge"   );
        _BIC.add("icjoin"   );
        _BIC.add("ocjoin"   );
        _BIC.add("dcjoin"   );
        _BIC.add("fgrid"    );
        _BIC.add("filter"   );
        _BIC.add("printf"   );
        _BIC.add("cprintf"  );

        _BIC.add("cmdecho"  );
        _BIC.add("echo"     );

        _BIC.add("date"     );
        _BIC.add("env"      );
        _BIC.add("history"  );

        _BIC.add("cwd"      );
        _BIC.add("ptd"      );
        _BIC.add("uhd"      );
        _BIC.add("jdd"      );
        _BIC.add("jtd"      );
        _BIC.add("jxd"      );
        _BIC.add("udd"      );
        _BIC.add("add"      );
        _BIC.add("msf"      );
        _BIC.add("pex"      );

        _BIC.add("chdir"    );
        _BIC.add("mkdir"    );
        _BIC.add("rmdir"    );

        _BIC.add("rmfile"   );
      //_BIC.add("rdfile"   ); // Commands processed via the '_translate_impl()' function are not directly executable built-in commands
      //_BIC.add("wrfile"   ); // ---
      //_BIC.add("mkfile"   ); // ---
        _BIC.add("upfile"   );

        _BIC.add("ls"       );
      //_BIC.add("lsrec"    ); // ---

      //_BIC.add("cprec"    ); // ---
      //_BIC.add("rmrec"    ); // ---
      //_BIC.add("mvrec"    ); // ---

        _BIC.add("jxmakecmd");
      //_BIC.add("jxmake"   ); // ---
      //_BIC.add("jxmsf"    ); // ---

        _BIC.add("alias"    );

    } // static

    public String _execute_impl(final String[] command_, final boolean canHaveVarAssign) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "_execute_impl()";

        // Check for possible variable assigment with literal value
        if(canHaveVarAssign) {
            // <variable_name_1> <variable_name_2> := ...
            if(command_.length >= 3) {
                final String opr = command_[2];
                if( opr.equals(OVA_LITERAL) ) {
                    if(command_.length > 3) _errorNotSupported( _msgVarOpr2(opr) ); // This construct is not supported in execution mode
                                            _errorSyntaxError ( _msgVarOpr2(opr) ); // Syntax error
                }
            }
            // <variable_name> := ...
            if(command_.length >= 2) {
                final String opr = command_[1];
                if( opr.equals(OVA_LITERAL) ) {
                    // Get the right-hand side
                    final String[] rhs = _cmdArrayRemoveNLeft(2, command_);
                    final String   var = _restrictVarName( command_[0] );
                    // Delete the variable
                    if(rhs == null) {
                        _delVar(var);
                    }
                    // Store the literal value in the variable
                    else {
                        _putVar( var, String.join("", rhs) );
                    }
                    // Return an empty string to indicate that the command has been executed
                    return "";
                }
            }
        }

        // Check for possible variable assigment with command substitution
        if(canHaveVarAssign) {
            // <variable_name_1> <variable_name_2> <- ...
            if(command_.length >= 3) {
                // The following three constructs are not supported as built-in commands
                final String opr = command_[2];
                if( opr.equals(OVA_CMDSUBS_1) ||
                    opr.equals(OVA_CMDSUBS_2) ||
                    opr.equals(OVA_CMDSUBS_B)
                ) {
                    final String[] rhs = _cmdArrayRemoveNLeft(3, command_);
                    if(rhs == null) _errorSyntaxError( _msgVarOpr2(opr) );
                    if( _BIC.contains( rhs[0] ) )_errorNotSupported( _msgVarOpr2(opr) );
                }
            }
            // <variable_name> <- ...
            if(command_.length >= 2) {
                // The following two constructs are not supported as built-in commands
                final String opr = command_[1];
                if( opr.equals(OVA_CMDSUBS_1) ||
                    opr.equals(OVA_CMDSUBS_2)
                ) {
                    final String[] rhs = _cmdArrayRemoveNLeft(3, command_);
                    if(rhs == null) _errorSyntaxError( _msgVarOpr2(opr) );
                    if( _BIC.contains( rhs[0] ) )_errorNotSupported( _msgVarOpr2(opr) );
                }
                // The following construct is supported as a built-in command
                if( opr.equals(OVA_CMDSUBS_B) ) {
                    // Get the right-hand side
                    final String[] rhs = _cmdArrayRemoveNLeft(2, command_);
                    if(rhs == null) _errorSyntaxError( _msgVarOpr1(opr) );
                    // Try to execute the right-hand side
                    final String res = _execute_impl(rhs, false);
                    if(res == null) return null; // Return null to indicate that the command is not a supported built-in command
                    // NOTE : Assume all built-in commands always write to stdout (never to stderr)
                    _putVar( _restrictVarName( command_[0] ), XCom.stripTrailingNewlines(res) );
                    // Return an empty string to indicate that the command has been executed
                    return "";
                }
            }
        }

        // Expand variable(s)
        final String[] command = _expandVar(command_);

        // Process normal command
        final String cmdStr = command[0];

        switch(cmdStr) {

            // NOTE : Keep this synchronized with the '_BIC' variable above!

            case "help"      : return _extraHelpStringB() + commandRegistry.generateHelpString() + _extraHelpStringE();

            case "clear"     : return _exec_echo_noNL( ANSIScreenBuffer.ASeq_ClearScreenAndScrollback );
            case "reset"     : return _exec_echo_noNL( ANSIScreenBuffer.ASeq_ResetToInitialState      );

            case "deansi"    : return _exec_deansi   ( command                                        );

            case "rdelim"    : return _exec_rdelim   ( command                                        );
            case "cdelim"    : return _exec_cdelim   ( command                                        );

            case "rspan"     : return _exec_rspan    ( command                                        );
            case "cspan"     : return _exec_cspan    ( command                                        );
            case "rmerge"    : return _exec_rmerge   ( command                                        );
            case "cmerge"    : return _exec_cmerge   ( command                                        );
            case "icjoin"    : return _exec_icjoin   ( command                                        );
            case "ocjoin"    : return _exec_ocjoin   ( command                                        );
            case "dcjoin"    : return _exec_dcjoin   ( command                                        );
            case "fgrid"     : return _exec_fgrid    ( command                                        );
            case "filter"    : return _exec_filter   ( command                                        );
            case "printf"    : return _exec_printf   ( command                                        );
            case "cprintf"   : return _exec_cprintf  ( command                                        );

            case "cmdecho"   : return _exec_cmdecho  ( command                                        );
            case "echo"      : return _exec_echo     ( command                                        );

            case "date"      : return _exec_date     ( command                                        );
            case "env"       : return _exec_env      ( command                                        );
            case "history"   : return _exec_history  ( command                                        );

            case "cwd"       : /* FALLTHROUGH */
            case "ptd"       : /* FALLTHROUGH */
            case "uhd"       : /* FALLTHROUGH */
            case "jdd"       : /* FALLTHROUGH */
            case "jtd"       : /* FALLTHROUGH */
            case "jxd"       : /* FALLTHROUGH */
            case "udd"       : /* FALLTHROUGH */
            case "add"       : /* FALLTHROUGH */
            case "msf"       : /* FALLTHROUGH */
            case "pex"       : return _getSpcVar     ( cmdStr                                         );

            case "chdir"     : return _exec_chdir    ( command                                        );
            case "mkdir"     : return _exec_mkdir    ( command                                        );
            case "rmdir"     : return _exec_rmdir    ( command                                        );

            case "rmfile"    : return _exec_rmfile   ( command                                        );
            case "upfile"    : return _exec_upfile   ( command                                        );

            case "ls"        : return _exec_ls       ( command, !canHaveVarAssign                     );

            case "jxmakecmd" : return _exec_jxmakecmd( command                                        );

            case "alias"     : return _exec_alias    ( command                                        );

        } // switch

        // Return null to indicate that the command is not a supported built-in command
        return null;
    }

    public String execute(final String[] command) throws IllegalArgumentException, IOException
    {
        _stdinEchoEn = false;

        return _execute_impl( _processAlias(command), true );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String[] _trans_CMD_READ_TEXT_FILE(final String[] rhs) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = CommandShell.CMD_READ_TEXT_FILE;

        if(rhs.length < 1) _errorMissingOption();

        for(int i = 0; i < rhs.length; ++i) rhs[i] = _resolvePath( rhs[i] );

        return CommandShell.cmdInvokeFunction(CommandShell.CMD_READ_TEXT_FILE, rhs);
    }

    private String[] _trans_CMD_WRITE_TEXT_FILE(final String[] rhs) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = CommandShell.CMD_WRITE_TEXT_FILE;

        if(rhs.length < 1) _errorMissingOption ();
        if(rhs.length > 1) _errorTooManyOptions();

        rhs[0] = _resolvePath( rhs[0] );

        _stdinEchoEn = true;

        return CommandShell.cmdInvokeFunction(CommandShell.CMD_WRITE_TEXT_FILE, rhs);
    }

    private String[] _trans_CMD_MAKE_TEXT_FILE(final String[] rhs) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = CommandShell.CMD_MAKE_TEXT_FILE;

        if(rhs.length < 1) _errorMissingOption ();
        if(rhs.length > 2) _errorTooManyOptions();

        rhs[0] = _resolvePath( rhs[0] );

        return CommandShell.cmdInvokeFunction(CommandShell.CMD_MAKE_TEXT_FILE, rhs);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String[] _trans_CMD_LIST_FULL(final String[] rhs, final boolean oneColumn) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = CommandShell.CMD_LIST_FULL;

        String mode = "-s";
        String path = "";

        for(int i = 0; i < rhs.length; ++i) {

            final String arg = rhs[i];

            switch(arg) {

                case "-s"  : /* FALLTHROUGH */
                case "-l"  : mode = arg; break;

                default    : path = arg; break;

            } // switch

        } // for

        path = _resolvePath(path);

        final String col = oneColumn ? "0" : ( "" + _console.getConsoleColumns() );

        return CommandShell.cmdInvokeFunction( CommandShell.CMD_LIST_FULL, new String[] { mode, col, path } );
    }

    private String[] _trans_CMD_COPY_FULL(final String[] rhs) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = CommandShell.CMD_COPY_FULL;

        if(rhs.length < 2) _errorMissingOption ();
        if(rhs.length > 2) _errorTooManyOptions();

        rhs[0] = _resolvePath( rhs[0] );
        rhs[1] = _resolvePath( rhs[1] );

        return CommandShell.cmdInvokeFunction(CommandShell.CMD_COPY_FULL, rhs);
    }

    private String[] _trans_CMD_REMOVE_FULL(final String[] rhs) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = CommandShell.CMD_REMOVE_FULL;

        if(rhs.length < 1) _errorMissingOption ();
        if(rhs.length > 1) _errorTooManyOptions();

        rhs[0] = _resolvePath( rhs[0] );

        return CommandShell.cmdInvokeFunction(CommandShell.CMD_REMOVE_FULL, rhs);
    }

    private String[] _trans_CMD_MOVE_FULL(final String[] rhs) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = CommandShell.CMD_MOVE_FULL;

        if(rhs.length < 2) _errorMissingOption();

        for(int i = 0; i < rhs.length; ++i) rhs[i] = _resolvePath( rhs[i] );

        return CommandShell.cmdInvokeFunction(CommandShell.CMD_MOVE_FULL, rhs);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String[] _trans_jxmake(final String[] command) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "jxmake";

        final ArrayList<String> cmd = SysUtil.getJavaCmd(false);

        cmd.addAll( Arrays.asList(command) );

        /*
        for(final String s : cmd) SysUtil.stdDbg().println(s);
        //*/

        return cmd.toArray( new String[0] );
    }

    private String[] _trans_jxmsf(final String[] command) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "jxmsf";

        final ArrayList<String> cmd = SysUtil.getJavaCmd(false);

        cmd.add("-f");
        cmd.add( JxMakeRootPane.getPath_mainScriptFile() );

        cmd.addAll( Arrays.asList(command) );

        /*
        for(final String s : cmd) SysUtil.stdDbg().println(s);
        //*/

        return cmd.toArray( new String[0] );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    private static final Set<String> BLACKLIST_OS_COMMAND = new HashSet<String>() {{

        // --- Windows ---
        add("cd"       );
        add("chdir"    ); // Alias of cd
        add("del"      );
        add("erase"    ); // Alias of del
        add("mkdir"    );
        add("md"       ); // Alias of mkdir
        add("rmdir"    );
        add("rd"       ); // Alias of rmdir
        add("copy"     );
        add("move"     );
        add("ren"      );
        add("rename"   ); // Alias of ren
        add("shutdown" );
        add("taskkill" );
        add("reg"      );
        add("net"      );
        add("netsh"    );
        add("sc"       );
        add("wmic"     );

        // --- POSIX ---
        add("cd"        );
        add("cp"        );
        add("ls"        );
        add("mkdir"     );
        add("mv"        );
        add("rm"        );
        add("rmdir"     );
        add("shutdown"  );
        add("reboot"    );
        add("halt"      );
        add("kill"      );
        add("pwd"       );
        add("su"        );
        add("sudo"      );
        add("ifconfig"  ); // BSD, MacOS, older Linux
        add("ip"        ); // Linux
        add("sysctl"    );
        add("service"   ); // Linux
        add("launchctl" ); // MacOS

        add("systemctl" ); // Linux
        add("init"      ); // BSD, Linux

        add("kldload"   ); // BSD
        add("kldunload" ); // BSD
        add("insmod"    ); // Linux
        add("rmmod"     ); // Linux
        add("modprobe"  ); // Linux
        add("kextload"  ); // MacOS
        add("kextunload"); // MacOS

    }};

    public String[] _translate_impl(final String[] command_, final boolean canHaveVarAssign) throws IllegalArgumentException, IOException
    {
        _EXEC_FUNC_NAME_ = "_translate_impl()";

        // Check for possible variable assigment with literal value
        if(canHaveVarAssign) {
            // <variable_name_1> <variable_name_2> := ...
            if(command_.length >= 3) {
                final String opr = command_[2];
                if( opr.equals(OVA_LITERAL) ) _errorNotSupported( _msgVarOpr2(opr) ); // This construct is not supported in translation mode
            }
            // <variable_name> := ...
            if(command_.length >= 2) {
                final String opr = command_[1];
                if( opr.equals(OVA_LITERAL) ) _errorNotSupported( _msgVarOpr1(opr) ); // This construct is not supported in translation mode
            }
        }

        // Check for possible variable assigment with command substitution
        if(canHaveVarAssign) {
            // <variable_name_1> <variable_name_2> <- ...
            if(command_.length >= 3) {
                // Get the capture mode
                final String  opr = command_[2];
                      boolean cap = false;
                     if( opr.equals(OVA_CMDSUBS_B) ) { _captureVarStream = -1; cap = true; }
                else if( opr.equals(OVA_CMDSUBS_1) ) { _captureVarStream =  1; cap = true; }
                else if( opr.equals(OVA_CMDSUBS_2) ) { _captureVarStream =  2; cap = true; }
                // Proceed only if a known capture mode was detected
                if(cap) {
                    // Get the right-hand side
                    final String[] rhs = _cmdArrayRemoveNLeft(3, command_);
                    if(rhs == null) _errorSyntaxError( _msgVarOpr2(opr) );
                    // Store the variable names
                    _captureVarName1 = _restrictVarName( command_[0] );
                    _captureVarName2 = _restrictVarName( command_[1] );
                    // Try to translate the right-hand side
                    return _translate_impl(rhs, false);
                }
            }
            // <variable_name> <- ...
            if(command_.length >= 2) {
                // Get the capture mode
                final String  opr = command_[1];
                      boolean cap = false;
                     if( opr.equals(OVA_CMDSUBS_B) ) { _captureVarStream = -1; cap = true; }
                else if( opr.equals(OVA_CMDSUBS_1) ) { _captureVarStream =  1; cap = true; }
                else if( opr.equals(OVA_CMDSUBS_2) ) { _captureVarStream =  2; cap = true; }
                // Proceed only if a known capture mode was detected
                if(cap) {
                    // Get the right-hand side
                    final String[] rhs = _cmdArrayRemoveNLeft(2, command_);
                    if(rhs == null) _errorSyntaxError( _msgVarOpr1(opr) );
                    // Store the variable names
                    _captureVarName1 = _restrictVarName( command_[0] );
                    _captureVarName2 = null;
                    // Try to translate the right-hand side
                    return _translate_impl(rhs, false);
                }
            }
        }

        // Expand variable(s)
        final String[] command = _expandVar(command_);

        // Process normal command
        final String   cmd = command[0];
        final String[] rhs = Arrays.copyOfRange(command, 1, command.length) ;

        switch(cmd) {

            case CommandShell.CMD_READ_TEXT_FILE  : return _trans_CMD_READ_TEXT_FILE (rhs                   );
            case CommandShell.CMD_WRITE_TEXT_FILE : return _trans_CMD_WRITE_TEXT_FILE(rhs                   );
            case CommandShell.CMD_MAKE_TEXT_FILE  : return _trans_CMD_MAKE_TEXT_FILE (rhs                   );

            case CommandShell.CMD_LIST_FULL       : return _trans_CMD_LIST_FULL      (rhs, !canHaveVarAssign);

            case CommandShell.CMD_COPY_FULL       : return _trans_CMD_COPY_FULL      (rhs                   );
            case CommandShell.CMD_REMOVE_FULL     : return _trans_CMD_REMOVE_FULL    (rhs                   );
            case CommandShell.CMD_MOVE_FULL       : return _trans_CMD_MOVE_FULL      (rhs                   );

            case "jxmake"                         : return _trans_jxmake             (rhs                   );
            case "jxmsf"                          : return _trans_jxmsf              (rhs                   );

        } // switch

        // Check for black-listed OS command
        if( BLACKLIST_OS_COMMAND.contains( cmd.toLowerCase() ) ) _errorBlacklisted(cmd);

        // Return the original command as is because it is not a supported built-in command
        return command;
    }

    public String[] translate(final String[] command) throws IllegalArgumentException, IOException
    {
        _stdinEchoEn = false;

        return _translate_impl( _processAlias(command), true );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final CommandRegistry commandRegistry = new CommandRegistry();

    static {

            commandRegistry.registerCommand( new CommandConfig( "help" , _T("Print this help")                ) );

            commandRegistry.registerCommand( new CommandConfig( "clear", _T("Clear screen and scroll buffer") ) );
            commandRegistry.registerCommand( new CommandConfig( "reset", _T("Reset the terminal")             ) );

        if(true) {
            final CommandConfig cc = new CommandConfig( "deansi", _T("Remove ANSI codes from each string") );
                cc.addOption( "-n"      , true, AssignmentStyle.NONE , _T("Append a newline after each string (default)") );
                cc.addOption( "-N"      , true, AssignmentStyle.NONE , _T("Do not append a newline after each string")    );
                cc.addOption( "<string>", true, AssignmentStyle.VALUE, _T("Input string to be de-ANSI-ed")                );
                cc.preselectOptions(2);
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( false, "rdelim", _T("Change/get the row (line) delimiter") );
            cc.addOption( "-d", false, AssignmentStyle.NONE , String.format(
                                                                  _T("Reset to the default delimiter ( `=\"%s\" `)"
                                                                   + " and concatenation string ( `=\"%s\" `)")    ,
                                                                  XCom.escapeControlCharsAndUseNBSP(DEF_ROW_DELIM ),
                                                                  XCom.escapeControlCharsAndUseNBSP(DEF_ROW_CONCAT)
                                                              )                                                               );
            cc.addOption( "-s", false, AssignmentStyle.SPACE, _T("Set the delimiter to the specified plain string")           );
            cc.addOption( "-r", false, AssignmentStyle.SPACE, _T("Set the delimiter to the specified regex pattern")          );
            cc.addOption( "-c", false, AssignmentStyle.SPACE, _T("Set the concatenation string to the specified plain value") );
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( false, "cdelim", _T("Change/get the column delimiter") );
            cc.addOption( "-d", false, AssignmentStyle.NONE , String.format(
                                                                  _T("Reset to the default delimiter ( `=\"%s\" `)"
                                                                   + " and concatenation string ( `=\"%s\" `)")    ,
                                                                  XCom.escapeControlCharsAndUseNBSP(DEF_COL_DELIM ),
                                                                  XCom.escapeControlCharsAndUseNBSP(DEF_COL_CONCAT)
                                                              )                                                               );
            cc.addOption( "-s", false, AssignmentStyle.SPACE, _T("Set the delimiter to the specified plain string" )          );
            cc.addOption( "-r", false, AssignmentStyle.SPACE, _T("Set the delimiter to the specified regex pattern")          );
            cc.addOption( "-c", false, AssignmentStyle.SPACE, _T("Set the concatenation string to the specified plain value") );
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( true, "rspan", _T("Extract rows (lines are separated by =rdelim `) from the given text;\nyou may need to @deansi the text first") );
                cc.addOption( "-f"    , false, AssignmentStyle.SPACE, _T("Starting row")          );
                cc.addOption( "-t"    , false, AssignmentStyle.SPACE, _T("Ending row")            );
                cc.addOption( "<text>", false, AssignmentStyle.VALUE, _T("Input text to extract") );
                cc.preselectOptions(0,  1, 2);
                cc.preselectValues (1, -1   );
                cc.addUsageExample("rspan -f -1 -t  1 \"1\\n2\\n3\\n4\\n5\"");
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, "cspan", _T("Extract columns (separated by =cdelim `) from the given text;\nyou may need to @deansi the text first") );
                cc.addOption( "-f"    , false, AssignmentStyle.SPACE, _T("Starting column")       );
                cc.addOption( "-t"    , false, AssignmentStyle.SPACE, _T("Ending column")         );
                cc.addOption( "<text>", false, AssignmentStyle.VALUE, _T("Input text to extract") );
                cc.preselectOptions(0,  1, 2);
                cc.preselectValues (1, -1   );
                cc.addUsageExample("cspan -f -1 -t  1 \"A1 B1 C1 D1 E1\\nA2 B2 C2 D2 E2\\nA3 B3 C3 D3 E3\\n\"");
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( false, "rmerge", _T("Merge rows (lines are separated by =rdelim `) from the given texts;\nyou may need to @deansi the texts first") );
                cc.addOption( "<text>", true, AssignmentStyle.VALUE, _T("Texts to merge") );
                cc.preselectOptions(0, 0);
                cc.addUsageExample("rmerge \"1\\n2\\n3\" \"4\\n5\"");
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( false, "cmerge", _T("Merge columns (separated by =cdelim `) from the given texts;\nyou may need to @deansi the texts first") );
                cc.addOption( "<text>", true, AssignmentStyle.VALUE, _T("Texts to merge") );
                cc.preselectOptions(0, 0);
                cc.addUsageExample("cmerge \"A1 B1 C1\\nA2 B2 C2\\nA3 B3 C3\" \"V1 W1 X1\\nV2 W2 X2\"");
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final Consumer<CommandConfig> _addCommonOpts = (cc) -> {
                cc.addOption( "-i"       , false, AssignmentStyle.NONE , _T("Perform case-insensitive key comparison") );
                cc.addOption(
                    "<key_print_mode>", false, AssignmentStyle.VALUE,
                    new String[] { "-k", "-u", "-l" },
                    new String[] {
                        _T("Key print mode (keys are always taken from the first text)"),
                        _T("Print keys in original case (default)"                     ),
                        _T("Print keys in uppercase"                                   ),
                        _T("Print keys in lowercase"                                   ),
                    }
                );
                cc.addOption( "<key_col>", true , AssignmentStyle.VALUE, _T("Index of the key column")                 );
                cc.addOption( "<text>"   , true , AssignmentStyle.VALUE, _T("Texts to be joined")                      );
                cc.preselectOptions(2, 3);
            };
            if(true) {
                final CommandConfig cc = new CommandConfig( false, "icjoin", _T("Perform an inner join on columns (separated by =cdelim `) from the given texts;\nyou may need to @deansi the texts first") );
                    _addCommonOpts.accept(cc);
                    cc.addUsageExample("icjoin    1 \"K1 A1 B1\\nK2 A2 B2\\nK3 A3 B3\" 3 \"X1 Y1 K1\\nX2 Y2 K2\\nX3 Y3 K3\"");
                    cc.addUsageExample("icjoin    1 \"K1 A1 B1\\nK2 A2 B2\\nK3 A3 B3\" 3 \"X1 Y1 K1\\nX2 Y2 K2\\nX9 Y9 K9\"");
                    cc.addUsageExample("icjoin -i 1 \"k1 A1 B1\\nk2 A2 B2\\nk3 A3 B3\" 3 \"X1 Y1 K1\\nX2 Y2 K2\\nX9 Y9 K9\"");
                commandRegistry.registerCommand(cc);
            }
            if(true) {
                final CommandConfig cc = new CommandConfig( false, "ocjoin", _T("Perform an outer join on columns (separated by =cdelim `) from the given texts;\nyou may need to @deansi the texts first") );
                    _addCommonOpts.accept(cc);
                    cc.addUsageExample("ocjoin 1 \"K1 A1 B1\\nK2 A2 B2\\nK3 A3 B3\" 3 \"X1 Y1 K1\\nX2 Y2 K2\\nX9 Y9 K9\"");
                commandRegistry.registerCommand(cc);
            }
            if(true) {
                final CommandConfig cc = new CommandConfig( false, "dcjoin", _T("Perform a difference join on columns (separated by =cdelim `) from the given texts;\nyou may need to @deansi the texts first") );
                    _addCommonOpts.accept(cc);
                    cc.addUsageExample("dcjoin 1 \"K1 A1 B1\\nK2 A2 B2\\nK3 A3 B3\" 3 \"X1 Y1 K1\\nX2 Y2 K2\\nX9 Y9 K9\"");
                commandRegistry.registerCommand(cc);
            }
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( false, "fgrid", _T("Arrange rows and columns into a neat grid;\nyou may need to @deansi the text first") );
                cc.addOption( "-l"    , false, AssignmentStyle.NONE , _T("Align columns to the left (default)") );
                cc.addOption( "-r"    , false, AssignmentStyle.NONE , _T("Align columns to the right")          );
                cc.addOption( "<text>", false, AssignmentStyle.VALUE, _T("Input text to process")               );
                cc.preselectOptions(2);
                cc.addUsageExample("fgrid -r \"A1 B11 C1\\nA2 B2 C2\\nA3 B3 C3\" \"V1 W1 X1\\nV2 W2 X22\\nV3 W3 X3\"");
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, "filter", _T("Filter rows (lines are separated by =rdelim `) using plain text or regular expression;\nsupports match and replace modes;\nyou may need to @deansi the text first") );
                cc.addOption( "<pattern>", false, AssignmentStyle.VALUE, _T("The pattern to be used, can be =plain text `, =/regex/flags `,\nor =/regex/replacement/flags `") );
                cc.addOption( "<text>"   , false, AssignmentStyle.VALUE, _T("Input text to process line by line")                                                             );
                cc.preselectOptions(0, 1);
                cc.addUsageExample("filter abc                        \"123\\n456\\n123 aBc\\nabc 456\\naXc\\n789\\n\"");
                cc.addUsageExample("filter /a[bx]c/i                  \"123\\n456\\n123 aBc\\nabc 456\\naXc\\n789\\n\"");
                cc.addUsageExample("filter /(.*?a)[bx](c.*?)/$1#$2/iu \"123\\n456\\n123 aBc\\nabc 456\\naXc\\n789\\n\"");
                cc.addUsageExample("filter /(.*?)789\\n(.*?)/$1$2/M    \"123\\n789\\n123\""                            );
                cc.addUsageExample("filter /(789)/$1/m                \"123\\n789\\n123\""                             );
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( false, "printf", _T("Format and print values") );
                cc.addOption( "<format>", false, AssignmentStyle.VALUE, _T("Format string")              );
                cc.addOption( "<value>" , true , AssignmentStyle.VALUE, _T("Values to format and print") );
                cc.preselectOptions(0);
                cc.addUsageExample("printf \"Sequential : %d %f %s %%\\n\" 42 3.14 hello");
                cc.addUsageExample("printf 'Indexed    : %2$s %1$d %1$f %% %n' 42 hello" );
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, "cprintf", _T("Format and print values from columns (see @cspan `) in text;\nyou may need to @deansi the text first") );
                cc.addOption( "<format>", false, AssignmentStyle.VALUE, _T("Format string")                  );
                cc.addOption( "<text>"  , false, AssignmentStyle.VALUE, _T("Input text to format and print") );
                cc.preselectOptions(0, 1);
                cc.addUsageExample("cprintf '%10s %10s %10s%n' \"A1 B1 C1\\nA2 B2 C2\\nA3 B3 C3\"");
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( true, "cmdecho", _T("Change/get the command echo setting") );
                cc.addOption(
                    "<mode>", false, AssignmentStyle.VALUE,
                    new String[] { "off", "on", "state" },
                    new String[] {
                        "{ off | on | state }"           ,
                        _T("Disable command echo")         ,
                        _T("Enable command echo (default)"),
                        _T("Query current echo state")
                    }
                );
                cc.preselectValues(1);
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( "echo", _T("Print a line of text for each string") );
                cc.addOption( "-n"      , true, AssignmentStyle.NONE , _T("Output the trailing newline (default)")                             );
                cc.addOption( "-N"      , true, AssignmentStyle.NONE , _T("Do not output the trailing newline")                                );
                cc.addOption( "-e"      , true, AssignmentStyle.NONE , _T("Enable interpretation of escape sequences; beware that the shell"
                                                                        + "\ninterprets them first (which may lead to double interpretation)") );
                cc.addOption( "-E"      , true, AssignmentStyle.NONE , _T("Disable interpretation of escape sequences (default); beware that"
                                                                        + "\nthe shell still interprets them.")                                );
                cc.addOption( "<string>", true, AssignmentStyle.VALUE, _T("String to be printed")                                              );
                cc.preselectOptions(4);
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( "date", _T("Print the current date and/or time") );
                cc.addOption(
                    "<mode>", false, AssignmentStyle.VALUE,
                    new String[] { "-s", "-d", "-t", "-dt" },
                    new String[] {
                        _T("Output mode")                                            ,
                        _T("Seconds since Unix Epoch")                               ,
                        _T("Date in 'yyyy-MM-dd' format")                            ,
                        _T("Time in 'HH:mm:ss' format")                              ,
                        _T("Date and time in 'yyyy-MM-dd HH:mm:ss' format (default)")
                    }
                );
                cc.addOption(
                    "<timezone>", false, AssignmentStyle.VALUE,
                    new String[] { "-u", "-l" },
                    new String[] {
                        _T("Time zone")                    ,
                        _T("Use UTC time zone")            ,
                        _T("Use local time zone (default)")
                    }
                );
                cc.preselectOptions(0, 1);
                cc.preselectValues (3, 1);
            commandRegistry.registerCommand(cc);
        }

            commandRegistry.registerCommand( new CommandConfig( "env", _T("Print all current environment variables") ) );

        if(true) {
            final CommandConfig cc = new CommandConfig(
                "history",
                _T("Print all history entries or delete them"),
                _T("If no option is specified, all history entries are printed")
            );
                cc.addOption( "-c" , false, AssignmentStyle.NONE , _T("Delete all history entries")              );
                cc.addOption( "-df", false, AssignmentStyle.SPACE, _T("Delete the first N entries from history") );
                cc.addOption( "-dl", false, AssignmentStyle.SPACE, _T("Delete the last N entries from history")  );
            commandRegistry.registerCommand(cc);
        }

            commandRegistry.registerCommand( new CommandConfig( "cwd", _T("Print the current working directory;\na special variable +"      + OVA_VAREXPN + "cwd is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "ptd", _T("Print the project temporary directory;\na special variable +"    + OVA_VAREXPN + "ptd is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "uhd", _T("Print the user home directory;\na special variable +"            + OVA_VAREXPN + "uhd is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "jdd", _T("Print the JxMake user data directory;\na special variable +"     + OVA_VAREXPN + "jdd is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "jtd", _T("Print the JxMake user tools directory;\na special variable +"    + OVA_VAREXPN + "jtd is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "jxd", _T("Print the JxMake executable directory;\na special variable +"    + OVA_VAREXPN + "jxd is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "udd", _T("Print the user dot-data directory;\na special variable +"        + OVA_VAREXPN + "udd is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "add", _T("Print the application dot-data directory;\na special variable +" + OVA_VAREXPN + "add is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "msf", _T("Print the main script file path;\na special variable +"          + OVA_VAREXPN + "msf is also available for use") ) );
            commandRegistry.registerCommand( new CommandConfig( "pex", _T("Print the the last process exit code;\na special variable +"     + OVA_VAREXPN + "pex is also available for use") ) );

        if(true) {
            final CommandConfig cc = new CommandConfig( true, "chdir", _T("Change the current working directory") );
                cc.addOption( "<path>", false, AssignmentStyle.VALUE , _T("Path to set as the new working directory") );
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, "mkdir", _T("Create directories (including parent directories as needed)") );
                cc.addOption( "<path>", true, AssignmentStyle.VALUE , _T("Path(s) of the directories to create") );
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, "rmdir", _T("Remove empty directories (including empty subdirectories as needed)") );
                cc.addOption( "<path>", true, AssignmentStyle.VALUE , _T("Path(s) of the directories to remove") );
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( true, "rmfile", _T("Remove file") );
                cc.addOption( "<path>", true, AssignmentStyle.VALUE , _T("Path(s) of the files to remove") );
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( true, CommandShell.CMD_READ_TEXT_FILE, _T("Read text file(s)") );
                cc.addOption( "<path>", false, AssignmentStyle.VALUE , _T("Path to the file(s) to read") );
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, CommandShell.CMD_WRITE_TEXT_FILE, _T("Write text file") );
                cc.addOption( "<path>", false, AssignmentStyle.VALUE , _T("Path to the file to write") );
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, CommandShell.CMD_MAKE_TEXT_FILE, _T("Create a text file from the given content") );
                cc.addOption( "<path>"   , false, AssignmentStyle.VALUE , _T("Path of the file to write")         );
                cc.addOption( "<content>", false, AssignmentStyle.VALUE , _T("Content to be written to the file") );
                cc.preselectOptions(0, 1);
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, "upfile", _T("Update the access and modification times of each file, or create an\nempty file if it does not exist") );
                cc.addOption( "<path>", true, AssignmentStyle.VALUE , _T("Path(s) to the file to update or create") );
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( "ls", _T("List directory contents") );
                cc.addOption( "-s"    , false, AssignmentStyle.NONE  , _T("Use short format (default)")      );
                cc.addOption( "-l"    , false, AssignmentStyle.NONE  , _T("Use long format to show details") );
                cc.addOption( "<path>", false, AssignmentStyle.VALUE , _T("Path to the directory to list")   );
                cc.preselectOptions(2);
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( CommandShell.CMD_LIST_FULL, _T("List directory contents (recursive)") );
                cc.addOption( "-s"    , false, AssignmentStyle.NONE  , _T("Use short format (default)")      );
                cc.addOption( "-l"    , false, AssignmentStyle.NONE  , _T("Use long format to show details") );
                cc.addOption( "<path>", false, AssignmentStyle.VALUE , _T("Path to the directory to list")   );
                cc.preselectOptions(2);
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig( true, CommandShell.CMD_COPY_FULL, _T("Copy file or directory (recursive)") );
                cc.addOption( "<src_path>", false, AssignmentStyle.VALUE , _T("Source path")      );
                cc.addOption( "<dst_path>", false, AssignmentStyle.VALUE , _T("Destination path") );
                cc.preselectOptions(0, 1);
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( true, CommandShell.CMD_REMOVE_FULL, _T("Remove file or directory (recursive)") );
                cc.addOption( "<path>", false, AssignmentStyle.VALUE , _T("Path to remove") );
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( CommandShell.CMD_MOVE_FULL, _T("Move file or directory (recursive)") );
                cc.addOption( "<src_path>", true , AssignmentStyle.VALUE , _T("Source path(s)")   );
                cc.addOption( "<dst_path>", false, AssignmentStyle.VALUE , _T("Destination path") );
                cc.preselectOptions(0, 1);
            commandRegistry.registerCommand(cc);
        }

            commandRegistry.registerCommand( new CommandConfig( "jxmakecmd", _T("Print the actual @jxmake command that will be executed") ) );

        if(true) {
            final CommandConfig cc = new CommandConfig( "jxmake", _T("Execute the @jxmake command;\nuse @jxmake :-h to display the usage and options") );
                cc.addOption( "<argument>", true, AssignmentStyle.VALUE , _T("Command argument(s)") );
                cc.addUsageExample("jxmake -f $msf my_target");
            commandRegistry.registerCommand(cc);
        }
        if(true) {
            final CommandConfig cc = new CommandConfig( "jxmsf", _T("Execute the @jxmake :-f +" + OVA_VAREXPN + "msf command; only valid if +" + OVA_VAREXPN + "msf is defined") );
                cc.addOption( "<argument>", true, AssignmentStyle.VALUE , _T("Command argument(s)") );
            commandRegistry.registerCommand(cc);
        }

        if(true) {
            final CommandConfig cc = new CommandConfig(
                "alias",
                _T("Print all aliases, add, delete, or clear them"),
                _T("If no option is specified, all aliases are printed")
            );
                cc.addOption( "-c"     , false, AssignmentStyle.NONE , _T("Delete all aliases")                             );
                cc.addOption( "-d"     , false, AssignmentStyle.SPACE, _T("Delete the alias with the specified name")       );
                cc.addOption( "-a"     , false, AssignmentStyle.SPACE, _T("Add a new alias with the specified name")        );
                cc.addOption( "<value>", true , AssignmentStyle.VALUE, _T("Value of the alias (used only with :-a `) only") );
                cc.addUsageExample("alias -a ll ls -l");
            commandRegistry.registerCommand(cc);
        }

    } // static

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _extraHelpStringB_cache = null;
    private static String _extraHelpStringE_cache = null;

    private static String _extraHelpStringB()
    {
        if(_extraHelpStringB_cache != null) return _extraHelpStringB_cache;

        _extraHelpStringB_cache = CommandRegistry.ATTR_COLOR_CAPTION1
                                + _T("\nCommands\n\n")
                                + CommandRegistry.ATTR_COLOR_RESET
                                ;

        return _extraHelpStringB_cache;
    }

    private static String _extraHelpStringE()
    {
        if(_extraHelpStringE_cache != null) return _extraHelpStringE_cache;

        final String        LT =     ' ' + OVA_LITERAL   + ' ' ;
        final String[]      CS = new String[] {
                                     ' ' + OVA_CMDSUBS_B + "  ",
                                     ' ' + OVA_CMDSUBS_1 + ' ' ,
                                     ' ' + OVA_CMDSUBS_2 + ' '
                                 };
        final String        C0 =     ' ' + OVA_CMDSUBS_B + ' ' ;
        final String        VX =           OVA_VAREXPN;

        final StringBuilder sb = new StringBuilder();

            sb.append( "\n"                                                      )
              .append( CommandRegistry.ATTR_COLOR_CAPTION2                       )
              .append( _T("Row (line) delimiters are used by these commands:\n") ) // NOTE : Synchronize this list with the actual command implementation!
              .append( CommandRegistry.ATTR_COLOR_COMMAND                        )
              .append( "    rspan\n"                                             )
              .append( "    cspan\n"                                             )
              .append( "    rmerge\n"                                            )
              .append( "    cmerge\n"                                            )
              .append( "    icjoin\n"                                            )
              .append( "    ocjoin\n"                                            )
              .append( "    dcjoin\n"                                            )
              .append( "    fgrid\n"                                             )
              .append( "    filter\n"                                            )
              .append( "    cprintf\n"                                           )
              .append( CommandRegistry.ATTR_COLOR_RESET                          )
              .append( "\n"                                                      )
              .append( CommandRegistry.ATTR_COLOR_CAPTION2                       )
              .append( _T("Column delimiters are used by these commands:\n")     ) // NOTE : Synchronize this list with the actual command implementation!
              .append( CommandRegistry.ATTR_COLOR_COMMAND                        )
              .append( "    cspan\n"                                             )
              .append( "    cmerge\n"                                            )
              .append( "    icjoin\n"                                            )
              .append( "    ocjoin\n"                                            )
              .append( "    dcjoin\n"                                            )
              .append( "    fgrid\n"                                             )
              .append( "    cprintf\n"                                           )
              .append( CommandRegistry.ATTR_COLOR_RESET                          )
              .append( "\n"                                                      )

              .append( CommandRegistry.ATTR_COLOR_CAPTION1                       )
              .append( _T("Variable assigment with literal value\n")             )
              .append( CommandRegistry.ATTR_COLOR_RESET                          )
              .append( "    <variable_name>"                                     )
              .append( CommandRegistry.ATTR_COLOR_OPERATOR                       )
              .append( LT                                                        )
              .append( CommandRegistry.ATTR_COLOR_RESET                          )
              .append( "value\n"                                                 )
              .append( "\n"                                                      )

              .append( CommandRegistry.ATTR_COLOR_CAPTION1                       )
              .append( _T("Variable assigment with command substitution"
                        + " (non‑chainable)\n")                                  )
              .append( CommandRegistry.ATTR_COLOR_RESET                          );
        for(int i = 0; i < CS.length; ++i) {
            sb.append( "    <variable_name>"                                     )
              .append( CommandRegistry.ATTR_COLOR_OPERATOR                       )
              .append( CS[i]                                                     )
              .append( CommandRegistry.ATTR_COLOR_COMMAND                        )
              .append( "<command> "                                              )
              .append( CommandRegistry.ATTR_COLOR_OPTION                         )
              .append( "[option]...\n"                                           )
              .append( CommandRegistry.ATTR_COLOR_RESET                          );
        } // for
            sb.append( "\n"                                                      );
        for(int i = 1; i < CS.length; ++i) {
            sb.append( String.format(
                           "    " + _T("Using %s means from %s only\n"),
                           CommandRegistry.ATTR_COLOR_OPERATOR +
                           CS[i].trim()                        +
                           CommandRegistry.ATTR_COLOR_RESET            ,
                           CommandRegistry.ATTR_COLOR_VALUE    +
                           ( (i == 1) ? "stdout" : "stderr" )  +
                           CommandRegistry.ATTR_COLOR_RESET
                       )                                                         );
        } // for
            sb.append( "\n"                                                      )
              .append( String.format(
                           "    " + _T("It is also possible to capture "
                                     + "%s and %s separately:\n\n"      ),
                           CommandRegistry.ATTR_COLOR_VALUE +
                           "stdout"                         +
                           CommandRegistry.ATTR_COLOR_RESET              ,
                           CommandRegistry.ATTR_COLOR_VALUE +
                           "stderr"                         +
                           CommandRegistry.ATTR_COLOR_RESET
                       )                                                         )
              .append( "    <var_for_stdout> <var_for_stderr>"                   )
              .append( CommandRegistry.ATTR_COLOR_OPERATOR                       )
              .append( C0                                                        )
              .append( CommandRegistry.ATTR_COLOR_COMMAND                        )
              .append( "<command> "                                              )
              .append( CommandRegistry.ATTR_COLOR_OPTION                         )
              .append( "[option]...\n"                                           )
              .append( CommandRegistry.ATTR_COLOR_RESET                          )
              .append( "\n"                                                      )

              .append( CommandRegistry.ATTR_COLOR_CAPTION1                       )
              .append( _T("Variable expansion\n")                                )
              .append( CommandRegistry.ATTR_COLOR_RESET                          )
              .append( CommandRegistry.ATTR_COLOR_OPERATOR                       )
              .append( "    " + VX                                               )
              .append( CommandRegistry.ATTR_COLOR_RESET                          )
              .append( "<variable_name>\n"                                       )
              .append( "\n"                                                      )

              .append( CommandRegistry.ATTR_COLOR_CAPTION2                       )
              .append( _T("Examples of Command Sequences:\n")                    )
              .append( CommandRegistry.ATTR_COLOR_RESET                          )
              .append( "    Val" + LT + "'Hello world'\n"                        )
              .append( "    Cmd" + LT + "echo\n"                                 )
              .append( "    Res" + C0 + VX + "Cmd " + VX + "Val\n"               )
              .append( "    echo " + VX + "Res\n"                                )
              .append( "\n"                                                      )
              .append( "    S" + LT + "\"\\n\"\n"                                )
              .append( "    A" + C0 + "ls -l\n"                                  )
              .append( "    B" + C0 + "deansi -N " + VX + "S -n " + VX + "A\n"   )
              .append( "    echo " + VX + "A " + VX + "B\n"                      )
              .append( "\n"                                                      )
              .append( "    A B" + C0                                            )
              .append(              "bash -c 'echo stdout; echo stderr >&2'\n"   )
              .append( "    echo " + VX + "A\n"                                  )
              .append( "    echo " + VX + "B\n"                                  )
              .append( "\n"                                                      )

              .append( CommandRegistry.ATTR_COLOR_RESET                          );

        _extraHelpStringE_cache = sb.toString();

        return _extraHelpStringE_cache;
    }

} // class JxMakeShell


