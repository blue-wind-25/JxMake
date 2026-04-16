/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.io.IOError;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxm.*;
import jxm.xb.*;


public class StdIO {

    private static final Pattern _pmGet_printf_fspec = Pattern.compile("%%|%[0 #+-]?[0-9*]*\\.?\\d*?.");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_printf(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean vectorArg, final boolean printToConsole) throws JXMException
    {
        // Determine the number of argument(s)
        final int givenArgCnt = vectorArg ? evalVals.get(1).size() : ( evalVals.size() - 1 );

        // Extract the format and argument(s)
        String format   = "";
        String argStr[] = new String[givenArgCnt];
        int    cntArg   = 0;

        if(vectorArg) {
            // Get the format string
            format = XCom.flatten( evalVals.get(0), "" );
            // Get the argument(s)
            for(final XCom.VariableStore varStore : evalVals.get(1) ) {
                argStr[cntArg++] = varStore.value;
            }
        }
        else {
            for(final XCom.VariableValue varValue : evalVals) {
                // Flatten the value into one string
                final String strCombine = XCom.flatten(varValue, "");
                // Store the combined string
                if(cntArg == 0) format             = strCombine;
                else            argStr[cntArg - 1] = strCombine;
                ++cntArg;
            }
            --cntArg; // Exclude the format string
        }

        // Convert the argument(s) to their proper type(s)
        Object  argObj[] = new Object[givenArgCnt];
        Matcher matcher  = _pmGet_printf_fspec.matcher(format);
        int     cntFSpec = 0;

        while( matcher.find() ) {

            // Get the format specifier string
            final String fspec = matcher.group();

            // Check the index
            if(cntFSpec >= cntArg) {
                if( !fspec.equals("%%") ) ++cntFSpec;
                continue;
            }

            // Get the argument string
            final String str = argStr[cntFSpec];

            // Convert the argument based on the specifier string
            switch( fspec.charAt( fspec.length() - 1) ) {
                case '%' : /* Nothing to do in this case */                                                  break;
                case 's' : argObj[cntFSpec] =                                     str          ; ++cntFSpec; break;
                case 'c' : argObj[cntFSpec] =                                     str.charAt(0); ++cntFSpec; break;
                case 'b' : argObj[cntFSpec] = XCom.toBoolean(execBlock, execData, str)         ; ++cntFSpec; break;
                case 'd' : argObj[cntFSpec] = XCom.toLong   (execBlock, execData, str)         ; ++cntFSpec; break;
                case 'u' : argObj[cntFSpec] = XCom.toLong   (execBlock, execData, str)         ; ++cntFSpec; break;
                case 'x' : argObj[cntFSpec] = XCom.toLong   (execBlock, execData, str)         ; ++cntFSpec; break;
                case 'X' : argObj[cntFSpec] = XCom.toLong   (execBlock, execData, str)         ; ++cntFSpec; break;
                default  : throw XCom.newJXMRuntimeError(Texts.EMsg_printf_InvalidFSpec, fspec);
            } // switch

        } // while matcher.find

        // Check the number of format specifiers and the number of arguments
        if(cntFSpec > cntArg) throw XCom.newJXMRuntimeError(Texts.EMsg_printf_NumFSAGreater, cntFSpec, cntArg);
        if(cntFSpec < cntArg) throw XCom.newJXMRuntimeError(Texts.EMsg_printf_NumFSALess,    cntFSpec, cntArg);

        // Format and print to console or store as the function call result
        try {
            final String resStr = String.format(format, argObj);
            if(printToConsole)
                SysUtil.stdOut().print(resStr);
            else
                retVal.add( new XCom.VariableStore(true, resStr) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError(Texts.EMsg_printf_Error);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static XCom.Mutex _readLineMutex = new XCom.Mutex();

    public static void _execute_read_line(final XCom.VariableValue retVal) throws JXMException
    {
        // Lock mutex to ensure that only one read can be performed at a time
        _readLineMutex.lock();

        try {
            // Perform read
            retVal.add( new XCom.VariableStore( true, SysUtil.stdCon().readLine() ) );
        }
        catch(final IOError e) {
            // Unlock mutex
            _readLineMutex.unlock();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Unlock mutex
        _readLineMutex.unlock();
    }

    public static void _execute_read_pswd(final XCom.VariableValue retVal) throws JXMException
    {
        // Lock mutex to ensure that only one read can be performed at a time
        _readLineMutex.lock();

        try {
            // Perform read
            retVal.add( new XCom.VariableStore( true, new String( SysUtil.stdCon().readPassword() ) ) );
        }
        catch(final IOError e) {
            // Unlock mutex
            _readLineMutex.unlock();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Unlock mutex
        _readLineMutex.unlock();
    }

} // class StdIO
