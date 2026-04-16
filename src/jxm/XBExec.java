/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;

import java.nio.file.attribute.FileTime;

import jxm.xb.*;


public class XBExec {

    private final XCom.ExecData _execData;
    private       ExecBlock     _errorBlock = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static XBExec _primaryXBExec = null;

    public static XBExec primaryXBExec()
    { return _primaryXBExec; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XBExec(final FileTime latestFileTime)
    {
        _execData = new XCom.ExecData(latestFileTime);

        if(_primaryXBExec == null) _primaryXBExec = this;
    }

    public ExecState getExecState()
    { return _execData.execState;}

    public int getExitCode()
    { return _execData.execState.getExitCode(); }

    public ExecBlock getErrorBlock()
    { return _errorBlock; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean execute(final XBBuilder xbb, final ArrayList<String> targetNames)
    { return execute( xbb.mainJXMSpecFile_absPath(), xbb.execBlocks(), targetNames ); }

    public boolean execute(final String mainJXMSpecFile_absPath, final XCom.ExecBlocks execBlocks, final ArrayList<String> targetNames)
    {
        // A transient variable to hold a reference to the execution block instance to be executed
        // immediately after this (it is required in case an exception occurs)
        ExecBlock curExecBlock = null;

        // Store all the target names that were specified from the command line
        _execData.execState.setCmdTargetNames(targetNames);

        // Execute the root execution-blocks; the function and target maps will be populated after this execution is done
        try {
            // Clear the error block first
            _errorBlock = null;
            // Loop through the execution-blocks
            for(final ExecBlock item : execBlocks) {
                // Save a reference to the execution block instance to be executed immediately after this
                curExecBlock = item;
                // Execute the execution block
                switch( item.execute(_execData) ) {
                    case Done            :                              break       ;
                    case Error           : _errorBlock = item;        ; break       ;
                    case SuppressedError : item.printSuppressedError(); break       ;
                    case ProgramExit     :                              return true ;
                    default              : _errorBlock = item;          break       ; // NOTE : This should never got executed!
                } // switch
                // Check for error
                if(_errorBlock != null) break;
            } // for
        }
        catch(final JXMException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set the error message and block
            _errorBlock = curExecBlock;
            if( !_errorBlock.isErrorStringSet() ) _errorBlock.setErrorFromString( e.toString() );
        }

        // Execute the target(s) if there was no error
        if(_errorBlock == null && targetNames != null) {
            final Target.ExecTargetResult etr = Target.execTargets(mainJXMSpecFile_absPath, _execData, execBlocks, targetNames);
            if(etr.executeResult != XCom.ExecuteResult.Done || etr.errExecBlock != null) _errorBlock = etr.errExecBlock;
        }

        // Check for error
        if(_errorBlock != null) {
            if( !_errorBlock.isErrorStringSet() ) _errorBlock.setErrorFromString( XCom.errorString(Texts.EMsg_UnknownRuntimeError) );
            if( _execData.execState.getExitCode() == 0 ) _execData.execState.setExitCode(SysUtil.DefaultExitErrorCode);
            return false;
        }

        // Done
        return true;
    }

    public boolean printError()
    { return (_errorBlock != null) ? _errorBlock.printError() : false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    private <T> T[] _executeCallback_impl_gen(final Class<T> clazz, final String cbFuncName, final Class<?>[] paramTypes, final Object[] params)
    {
        // The 'FuncCall' block name
        String fcBlockName = "_fc_cbf_";

        // Create 'ReadVarSpecs' to store the the callback function name and function argument(s)
        final XCom.ReadVarSpecs readVarSpecs = new XCom.ReadVarSpecs();
              int               argCnt       = 1;

        readVarSpecs.add( new XCom.ReadVarSpec(true, null, cbFuncName, null, null) ); // Store the callback function name

        // Process the parameter(s)
        final ArrayList<String>             paramNames  = new ArrayList<>();
        final ArrayList<XCom.VariableValue> paramValues = new ArrayList<>();

        for(int i = 0; i < params.length; ++i) {

            // Create and store the parameter if it is present
            final Object             param       = params[i];
            final boolean            isLong      = paramTypes[i] == long  [].class;
            final boolean            isString    = paramTypes[i] == String[].class;

                  String             vvParamName = null;
                  XCom.VariableValue vvParamVVal = null;

            /*
                 if(isLong  ) SysUtil.stdDbg().printf("@@@ %d LONG\n"  , i);
            else if(isString) SysUtil.stdDbg().printf("@@@ %d STRING\n", i);
            else              SysUtil.stdDbg().printf("@@@ %d ???\n"   , i);
            //*/

            if(isLong) {
                final long[] lparam = (long[]) param;
                if(lparam != XCom.LongArray_NoValue) {
                    vvParamName = "_cbp_[long]:param" + (i + 1);
                    vvParamVVal = new XCom.VariableValue();
                    if(lparam != null) {
                        for(final long v : lparam) vvParamVVal.add( new XCom.VariableStore( true, String.valueOf(v) ) );
                    }
                }
            }
            else if(isString) {
                final String[] sparam = (String[]) param;
                if(sparam != XCom.StringArray_NoValue) {
                    vvParamName = "_cbp_[String]:param" + (i + 1);
                    vvParamVVal = new XCom.VariableValue();
                    if(sparam != null) {
                        for(final String v : sparam) vvParamVVal.add( new XCom.VariableStore(true, v) );
                    }
                }
            }
            else {
                // Invalid parameter type
                try {
                    throw XCom.newJXMFatalLogicError( String.format("_executeCallback_impl_gen(): ✖ paramTypes[%d]", i) );
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Print the error
                    SysUtil.printError( e.toString() );
                }
                return null;
            }

            // Append as an argument if the parameter exist
            if(vvParamName != null) {
                // Store the function argument
                readVarSpecs.add( new XCom.ReadVarSpec(false, null, vvParamName, null, null) );
                // Store the name and value
                paramNames .add(vvParamName);
                paramValues.add(vvParamVVal);
                // Append the name to the block name
                fcBlockName += "#" + vvParamName;
                // Increment the number of arguments
                ++argCnt;
            }

        } // for

        // Create and store the the 'FuncCall' block
        final XCom.FuncSpec funcSpec = new XCom.FuncSpec(XCom.FuncName.call, true, argCnt, 0);
        final FuncCall      funcCall = new FuncCall(fcBlockName, -1, -1, funcSpec, readVarSpecs);

        // Assume OK for now
        XCom.ExecuteResult executeResult = XCom.ExecuteResult.Done;

        // Set the argument(s)
        try {
            for( int i = 0; i < paramNames.size(); ++i ) {
                _execData.execState.setCBVar( paramNames.get(i), paramValues.get(i) );
            }
        }
        catch(final JXMException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set as error
            executeResult = XCom.ExecuteResult.Error;
        }

        // Execute the function
        if(executeResult != XCom.ExecuteResult.Error) {
            try {
                // Execute the function
                executeResult = funcCall.execute(_execData);
            }
            catch(final JXMException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Set the error message and exit
                if( !funcCall.isErrorStringSet() ) funcCall.setErrorFromString( XCom.errorString(Texts.EMsg_UnknownRuntimeError) );
                if( _execData.execState.getExitCode() == 0 ) _execData.execState.setExitCode(SysUtil.DefaultExitErrorCode);
                // Set as error
                executeResult = XCom.ExecuteResult.Error;
            }
        }

        // Delete the argument(s)
        try {
            for(String name : paramNames) _execData.execState.delCBVar(name);
        }
        catch(final JXMException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set as error
            executeResult = XCom.ExecuteResult.Error;
        }

        // Check for error
        if(executeResult != XCom.ExecuteResult.Done) return null;

        // Get the result
        final XCom.VariableValue retVar = funcCall.getReturnValue();

        if(retVar == null) return null;

        // Extract and return the result
        final T[] retVal = (T[]) java.lang.reflect.Array.newInstance( clazz, retVar.size() );

        if(clazz == Long.class) {
            for(int i = 0; i < retVal.length; ++i) retVal[i] = (T) ( Long.decode( retVar.get(i).value ) );
        }
        else if(clazz == String.class) {
            for(int i = 0; i < retVal.length; ++i) retVal[i] = (T) (              retVar.get(i).value   );
        }

        return retVal;
    }

    public String[] _executeCallback_impl_ret_string_array(final String cbFuncName, final Class<?>[] paramTypes, final Object[] params)
    { return _executeCallback_impl_gen(String.class, cbFuncName, paramTypes, params); }

    public long[] _executeCallback_impl_ret_long_array(final String cbFuncName, final Class<?>[] paramTypes, final Object[] params)
    {
        final Long[] res = _executeCallback_impl_gen(Long.class, cbFuncName, paramTypes, params);

        if(res == null) return null;

        return Arrays.stream(res).mapToLong(Long::longValue).toArray();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Class<?>[] _paramTypes(final Class<?> type1, final int numTypeN, final Class<?> typeN)
    {
        Class<?>[] paramTypes = new Class<?>[ ( (type1 != null) ? 1 : 0 ) + numTypeN ];

        if(type1 != null) {
            paramTypes[0] = type1;
            Arrays.fill( paramTypes, 1, paramTypes.length, typeN.getComponentType() );
        }
        else {
            Arrays.fill( paramTypes,                       typeN.getComponentType() );
        }

        return paramTypes;
    }

    private static Object[] _paramCombine(final Object param1, final Object[] params)
    {
        final Object[] cparams = new Object[1 + params.length];

        cparams[0] = param1;
        for(int i = 0; i < params.length; ++i) cparams[i + 1] = params[i];

        return cparams;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // String[]...
    public String[] executeCallback(final String cbFuncName, final String[]... params)
    { return _executeCallback_impl_ret_string_array( cbFuncName, _paramTypes( null          , params.length, params.getClass() ), params                        ); }

    // long[], String[]...
    public String[] executeCallback(final String cbFuncName, final long[] param1, final String[]... params)
    { return _executeCallback_impl_ret_string_array( cbFuncName, _paramTypes( long[].class  , params.length, params.getClass() ), _paramCombine(param1, params) ); }

    // long[]...
    public long[] executeCallback(final String cbFuncName, final long[]... params)
    { return _executeCallback_impl_ret_long_array  ( cbFuncName, _paramTypes( null          , params.length, params.getClass() ), params                        ); }

    // String[], long[]...
    public long[] executeCallback(final String cbFuncName, final String[] param1, final long[]... params)
    { return _executeCallback_impl_ret_long_array  ( cbFuncName, _paramTypes( String[].class, params.length, params.getClass() ), _paramCombine(param1, params) ); }

} // class XBExec
