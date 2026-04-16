/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;

import jxm.*;


public class ExecState {

    private final ShellOper           _shellOper;

    private       ArrayList<String>   _cmdTargetNames;

    private final Option.OptionStack  _optionStack;

    private final XCom.VariableMap    _argMap;
    private final XCom.VariableStack  _argStack;

    private final XCom.VariableMap    _varMap;
    private final XCom.VariableStack  _varStack;

    private final XCom.ProgStateStack _progStateStack;
    private final XCom.TargetStack    _targetStack;

    private       String              _lastSupErr;
    private       int                 _exitCode;

    private       boolean             _cmdEcho;
    private       boolean             _cmdStreaming;
    private       boolean             _cmdStdErrChk;
    private       boolean             _cmdStdOutChk;
    private       boolean             _enableDummyTP;
    private       String              _jxmakefile;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ExecState(final ShellOper shellOper)
    {
        _shellOper      = shellOper;

        _cmdTargetNames = null;

        _optionStack    = new Option.OptionStack ();

        _argMap         = new XCom.VariableMap   ();
        _argStack       = new XCom.VariableStack ();

        _varMap         = new XCom.VariableMap   ();
        _varStack       = new XCom.VariableStack ();

        _progStateStack = new XCom.ProgStateStack();
        _targetStack    = new XCom.TargetStack   ();

        _lastSupErr     = null;
        _exitCode       = 0;

        _cmdEcho        = true;
        _cmdStreaming   = false;
        _cmdStdErrChk   = false;
        _cmdStdOutChk   = false;
        _enableDummyTP  = false;
        _jxmakefile     = null;
    }

    public void setCmdTargetNames(final ArrayList<String> targetNames)
    { _cmdTargetNames = targetNames; }

    public Option.OptionStack optionStack()
    { return _optionStack; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ExecState(final ExecState refExecState)
    {
        // Clone instances
        _shellOper      = refExecState._shellOper     .deepClone();

        _cmdTargetNames = refExecState._cmdTargetNames;

        _optionStack    = refExecState._optionStack   .deepClone();

        _argMap         = refExecState._argMap        .deepClone();
        _argStack       = refExecState._argStack      .deepClone();

        _varMap         = refExecState._varMap        .deepClone();
        _varStack       = refExecState._varStack      .deepClone();

        _progStateStack = refExecState._progStateStack.deepClone();
        _targetStack    = refExecState._targetStack   .deepClone();

        _lastSupErr     = refExecState._lastSupErr;
        _exitCode       = refExecState._exitCode;

        _cmdEcho        = refExecState._cmdEcho;
        _cmdStreaming   = refExecState._cmdStreaming;
        _cmdStdErrChk   = refExecState._cmdStdErrChk;
        _cmdStdOutChk   = refExecState._cmdStdOutChk;
        _enableDummyTP  = refExecState._enableDummyTP;
        _jxmakefile     = refExecState._jxmakefile;
    }

    public ExecState deepClone()
    { return new ExecState(this); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean cboInCallStack(final ContainerBlock blockObject)
    { return _progStateStack.cboInCallStack(blockObject); }

    public ShellOper getShellOper()
    { return _shellOper; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setCmdEcho(final boolean cmdEcho)
    { _cmdEcho = cmdEcho; }

    public boolean getCmdEcho()
    { return _cmdEcho; }

    public void setCmdStreaming(final boolean cmdStreaming)
    { _cmdStreaming = cmdStreaming; }

    public boolean getCmdStreaming()
    { return _cmdStreaming; }

    public void setCmdStdErrChk(final boolean cmdStdErrChk)
    { _cmdStdErrChk = cmdStdErrChk; }

    public boolean getCmdStdErrChk()
    { return _cmdStdErrChk; }

    public void setCmdStdOutChk(final boolean cmdStdOutChk)
    { _cmdStdOutChk = cmdStdOutChk; }

    public boolean getCmdStdOutChk()
    { return _cmdStdOutChk; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setEnableDummyTargetPreq(final boolean enableDummyTP)
    {
        _enableDummyTP = enableDummyTP;
        _shellOper.setEnableDummyTargetPreq(enableDummyTP);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setLastSupError(final String errStr)
    {
        if(_lastSupErr == null) _lastSupErr =                      errStr;
        else                    _lastSupErr = _lastSupErr + '\n' + errStr;
    }

    public String getLastSupError()
    {
        final String errMsg = (_lastSupErr != null) ? _lastSupErr : "";
        _lastSupErr = null;

         return errMsg;
    }

    public void setExitCode(int exitCode)
    { _exitCode = exitCode; }

    public int getExitCode()
    { return _exitCode; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setJxMakeFile(final String jxmakefile)
    { _jxmakefile = jxmakefile; }

    public String getJxMakeFile()
    { return _jxmakefile; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private XCom.VariableValue setGlobalVar(final String name, final XCom.VariableValue value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    {
        // Fatal error if both flags are set
        if(concat && onlyIfNotSet) throw XCom.newJXMFatalLogicError(Texts.EMsg_BothFlagsSet); // NOTE : This should never got executed!

        // Get the variable value from the global variable map
        XCom.VariableValue varVal = _varMap.get(name);

        // The variable exists
        if(varVal != null) {
            // Return if the 'onlyIfNotSet' flag is set
            if(onlyIfNotSet) return varVal;
            // Check the '_const' flag
            if( varVal.isConstantVariable() ) throw XCom.newJXMRuntimeError(Texts.EMsg_InvalidConstVarModify);
            // Check if the variable is deprecated
            if( varVal.getDeprecated() != null ) SysUtil.printfSimpleWarning(Texts.WMsg_SetDeprecatedUsrVar, name);
        }
        // The variable does not exist
        else {
            // Create a new variable and store it to the global variable map
            varVal = new XCom.VariableValue();
            _varMap.put(name, varVal);
        }

        // Cerate a new instance if the destination refers to the same instance as the source
        if(varVal == value) {
            varVal = new XCom.VariableValue();
            _varMap.put(name, varVal);
        }

        // Clear the variable if the 'concat' flag is not set
        if(!concat) varVal.clear();

        // Store the new value(s)
        for(final XCom.VariableStore item : value) varVal.add(item);

        // Return the variable instance
        return varVal;
    }

    private XCom.VariableValue setGlobalVar(final String name, final XCom.VariableValue value) throws JXMException
    { return setGlobalVar(name, value, false, false); }

    public XCom.VariableValue setGlobalVar(final String name, final XCom.VariableStore value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    { return setGlobalVar(name, new XCom.VariableValue(value), concat, onlyIfNotSet); }

    private XCom.VariableValue setGlobalVar(final String name, final XCom.VariableStore value) throws JXMException
    { return setGlobalVar(name, value, false, false); }

    private void delGlobalVar(final String name) throws JXMException
    {
        // Get the variable value from the global variable map
        XCom.VariableValue varVal = _varMap.get(name);

        // The variable exists in the global variable map
        if(varVal != null) {
            // Check the '_const' flag
            if( varVal.isConstantVariable() ) throw XCom.newJXMRuntimeError(Texts.EMsg_InvalidConstVarDelete);
            // Check if the variable is deprecated
            if( varVal.getDeprecated() != null ) SysUtil.printfSimpleWarning(Texts.WMsg_DelDeprecatedUsrVar, name);
            // Delete the variable
            _varMap.remove(name);
        }
    }

    private void depGlobalVar(final String name, final String replacement) throws JXMException
    {
        // Get the variable value from the global variable map
        XCom.VariableValue varVal = _varMap.get(name);

        // The variable exists in the global variable map
        if(varVal != null) {
            // Deprecate the variable
            varVal.setDeprecated(replacement);
        }
    }

    private boolean hasGlobalVar(final String name)
    { return _varMap.containsKey(name); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private XCom.VariableValue setLocalVar(final String name, final XCom.VariableValue value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    {
        // Fatal error if both flags are set
        if(concat && onlyIfNotSet) throw XCom.newJXMFatalLogicError(Texts.EMsg_BothFlagsSet); // NOTE : This should never got executed!

        // Get the variable value from the local variable map (stack top)
        XCom.VariableMap   varMap = _varStack.peek();
        XCom.VariableValue varVal = varMap.get(name);

        // The variable exists in the local variable map
        if(varVal != null) {
            // Return if the 'onlyIfNotSet' flag is set
            if(onlyIfNotSet) return varVal;
            // Check the '_const' flag
            if( varVal.isConstantVariable() ) throw XCom.newJXMRuntimeError(Texts.EMsg_InvalidConstVarModify);
            // Check if the variable is deprecated
            if( varVal.getDeprecated() != null ) SysUtil.printfSimpleWarning(Texts.WMsg_SetDeprecatedUsrVar, name);
        }
        // The variable does not exist in the local variable map
        else {
            // Get from the argument variable map first
            if( !_argStack.empty() ) {
                // Get the variable value from the argument variable map
                final XCom.VariableValue refVarVal = _argStack.peek().get(name);
                // Check if the variable exist in the argument variable map
                if( refVarVal != null && !refVarVal.isEmpty() ) {
                    // Return if the 'onlyIfNotSet' flag is set
                    if(onlyIfNotSet) return refVarVal;
                    // Otherwise, clone it and store it to the local variable map
                    varVal = new XCom.VariableValue();
                    for(final XCom.VariableStore item : refVarVal) varVal.add(item);
                    varMap.put(name, varVal);
                }
            }
            /*
            if(varVal == null) {
                for(int i = _varStack.size() - 1; i >= 0; --i) {
                    XCom.VariableMap   varMapX = _varStack.get(i);
                    XCom.VariableValue varValX = varMapX.get(name);
                    if(varValX != null) {
                        varVal = varValX;
                        break;
                    }
                }
            }
            */
            // Get from the global variable map next
            if(varVal == null) {
                // Get the variable value from the global variable map
                final XCom.VariableValue refVarVal = _varMap.get(name);
                // Check if the variable exist in the global variable map
                if( refVarVal != null && !refVarVal.isEmpty() ) {
                    // Return if the 'onlyIfNotSet' flag is set
                    if(onlyIfNotSet) return refVarVal;
                    // Otherwise, clone it and store it to the local variable map
                    varVal = new XCom.VariableValue();
                    for(final XCom.VariableStore item : refVarVal) varVal.add(item);
                    varMap.put(name, varVal);
                }
            }
        }

        // The variable does not exist in both the local and global variable maps
        if(varVal == null) {
            // Create a new variable and store it to the local variable map
            varVal = new XCom.VariableValue();
            varMap.put(name, varVal);
        }

        // Cerate a new instance if the destination refers to the same instance as the source
        if(varVal == value) {
            varVal = new XCom.VariableValue();
            varMap.put(name, varVal);
        }

        // Clear the variable if the 'concat' flag is not set
        if(!concat) varVal.clear();

        // Store the new value(s)
        if(value != null) {
            for(final XCom.VariableStore item : value) varVal.add(item);
        }

        // Return the variable instance
        return varVal;
    }

    private XCom.VariableValue setLocalVar(final String name, final XCom.VariableValue value) throws JXMException
    { return setLocalVar(name, value, false, false); }

    private XCom.VariableValue setLocalVar(final String name, final XCom.VariableStore value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    { return setLocalVar(name, new XCom.VariableValue(value), concat, onlyIfNotSet); }

    private XCom.VariableValue setLocalVar(final String name, final XCom.VariableStore value) throws JXMException
    { return setLocalVar(name, value, false, false); }

    protected void delLocalVar(final String name) throws JXMException
    {
        // Get the variable value from the local variable map (stack top)
        XCom.VariableMap   varMap = _varStack.peek();
        XCom.VariableValue varVal = varMap.get(name);

        // The variable exists in the local variable map
        if(varVal != null) {
            // Check the '_const' flag
            if( varVal.isConstantVariable() ) throw XCom.newJXMRuntimeError(Texts.EMsg_InvalidConstVarDelete);
            // Check if the variable is deprecated
            if( varVal.getDeprecated() != null ) SysUtil.printfSimpleWarning(Texts.WMsg_DelDeprecatedUsrVar, name);
            // Delete the variable
            varMap.remove(name);
        }
    }

    protected void depLocalVar(final String name, final String replacement) throws JXMException
    {
        // Get the variable value from the local variable map (stack top)
        XCom.VariableMap   varMap = _varStack.peek();
        XCom.VariableValue varVal = varMap.get(name);

        // The variable exists in the local variable map
        if(varVal != null) {
            // Deprecate the variable
            varVal.setDeprecated(replacement);
        }
    }

    private boolean hasLocalVar(final String name)
    { return !_varStack.empty() && _varStack.peek().containsKey(name); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void clearAllArgVars()
    { _argMap.clear(); }

    protected XCom.VariableValue setArgVar(final String name, final XCom.VariableValue value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    {
        // Fatal error if both flags are set
        if(concat && onlyIfNotSet) throw XCom.newJXMFatalLogicError(Texts.EMsg_BothFlagsSet); // NOTE : This should never got executed!

        // Get the variable value from the argument variable map (stack top)
        XCom.VariableMap   varMap = _argStack.peek();
        XCom.VariableValue varVal = varMap.get(name);

        // The variable exists
        if(varVal != null) {
            // Return if the 'onlyIfNotSet' flag is set and the variable is not empty
            if( onlyIfNotSet && !varVal.isEmpty() ) return varVal;
        }
        // The variable does not exist
        else {
            // Create a new variable and store it to the argument variable map
            varVal = new XCom.VariableValue();
            varMap.put(name, varVal);
        }

        // Cerate a new instance if the destination refers to the same instance as the source
        if(varVal == value) {
            varVal = new XCom.VariableValue();
            varMap.put(name, varVal);
        }

        // Clear the variable if the 'concat' flag is not set
        if(!concat) varVal.clear();

        // Store the new value(s)
        for(final XCom.VariableStore item : value) varVal.add(item);

        // Return the variable instance
        return varVal;
    }

    private XCom.VariableValue setArgVar(final String name, final XCom.VariableValue value) throws JXMException
    { return setArgVar(name, value, false, false); }

    private XCom.VariableValue setArgVar(final String name, final XCom.VariableStore value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    { return setArgVar(name, new XCom.VariableValue(value), concat, onlyIfNotSet); }

    private XCom.VariableValue setArgVar(final String name, final XCom.VariableStore value) throws JXMException
    { return setArgVar(name, value, false, false); }

    private void delArgVar(final String name) throws JXMException
    {
        // Get the variable value from the argument variable map (stack top)
        XCom.VariableMap   varMap = _argStack.peek();
        XCom.VariableValue varVal = varMap.get(name);

        // The variable exists in the local variable map
        if(varVal != null) {
            // Check the '_const' flag
            if( varVal.isConstantVariable() ) throw XCom.newJXMRuntimeError(Texts.EMsg_InvalidConstVarDelete);
            // Delete the variable
            varMap.remove(name);
        }
    }

    private boolean hasArgVarPlaceholder(final String name)
    { return !_argStack.empty() && _argStack.peek().containsKey(name); }

    private boolean hasArgVar(final String name)
    { return hasArgVarPlaceholder(name) && !_argStack.peek().get(name).isEmpty(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private XCom.VariableValue setVar(final String name, XCom.VariableValue value, final boolean concat, final boolean onlyIfNotSet, final boolean const_) throws JXMException
    {
        // Try to set it to the local variable map first
        if( hasLocalVar(name) ) {
            return setLocalVar(name, value, concat, onlyIfNotSet);
        }

        // Try to set it to the argument variable map next
        if( hasArgVarPlaceholder(name) ) {
            return setArgVar(name, value, concat, onlyIfNotSet);
        }

        // Set it to the global variable map last
        return setGlobalVar(name, value, concat, onlyIfNotSet);
    }

    private XCom.VariableValue setVar(final String name, XCom.VariableValue value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    { return setVar(name, value, concat, onlyIfNotSet, false); }

    private XCom.VariableValue setVar(final String name, final XCom.VariableValue value) throws JXMException
    { return setVar(name, value, false, false); }

    protected XCom.VariableValue setVar(final String name, final XCom.VariableStore value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    { return setVar(name, new XCom.VariableValue(value), concat, onlyIfNotSet); }

    private XCom.VariableValue setVar(final String name, final XCom.VariableStore value) throws JXMException
    { return setVar(name, value, false, false); }

    private void _delVar(final String name) throws JXMException
    {
        // Try to delete from the local variable map first
        if( hasLocalVar(name) ) {
            delLocalVar(name);
            return;
        }

        // Try to delete from the argument variable map next
        if( hasArgVarPlaceholder(name) ) {
            delArgVar(name);
            return;
        }

        // Delete from the global variable map last
        delGlobalVar(name);
    }

    private void _depVar(final String name, final String replacement) throws JXMException
    {
        // Try to deprecate from the local variable map first
        if( hasLocalVar(name) ) {
            depLocalVar(name, replacement);
            return;
        }

        // Try to deprecate from the argument variable map next
        if( hasArgVarPlaceholder(name) ) {
            // Variables that come from function call parameters cannot be deprecated
            SysUtil.printfSimpleWarning(Texts.WMsg_DeprecateArgVarIgn, name);
            return;
        }

        // Deprecate from the global variable map last
        depGlobalVar(name, replacement);
    }

    private boolean hasVar(final String name)
    {
        if( hasLocalVar(name) ) return true;
        if( hasArgVar  (name) ) return true;

        return hasGlobalVar(name);
    }

    private String _hasVarExt(final String name)
    {
        if( hasLocalVar(name) ) return "l";
        if( hasArgVar  (name) ) return "a";

        return hasGlobalVar(name) ? "g" : "";
    }

    private String _getReferencedVarName(final ExecBlock execBlock, final XCom.ExecData execData, final String refVarName, final boolean forVarAssign)
    {
        final String refVarName_ = XCom.extractRefVarName(refVarName);

        try {
            // Get the referenced name
            final XCom.VariableValue orgVarVal    = _evalVar( execBlock, execData, new XCom.VariableStore(false, refVarName_) );
            final String             orgFtStr     = XCom.flatten(orgVarVal, "");
            final String             orgVarName   = XCom.genRVarName(orgFtStr);
            final boolean            orgNameValid = XCom.isSymbolName(orgFtStr);
            // Print warning as needed
            if( execData.execState.optionStack().enableWarnEvalInvRefVar() ) {
                if( orgVarVal.size() != 1 || !hasVar(orgVarName) || !orgNameValid ) {
                    if( orgVarName.isEmpty() ) {
                        if( !hasVar(refVarName_) ) {
                            SysUtil.printfWarning(
                                execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(),
                                forVarAssign ? Texts.WMsg_AsgnInvRefVar0 : Texts.WMsg_EvalInvRefVar0, refVarName
                            );
                        }
                        else {
                            SysUtil.printfWarning(
                                execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(),
                                forVarAssign ? Texts.WMsg_AsgnInvRefVar1 : Texts.WMsg_EvalInvRefVar1, refVarName
                            );
                        }
                    }
                    else {
                        if( !hasVar(refVarName_) ) {
                            SysUtil.printfWarning(
                                execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(),
                                forVarAssign ? Texts.WMsg_AsgnInvRefVarExt0 : Texts.WMsg_EvalInvRefVarExt0, orgVarName, refVarName
                            );
                        }
                        else {
                            SysUtil.printfWarning(
                                execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(),
                                forVarAssign ? Texts.WMsg_AsgnInvRefVarExt1 : Texts.WMsg_EvalInvRefVarExt1, orgVarName, refVarName
                            );
                        }
                    }
                }
            }
            // Return the referenced name
            if(!orgNameValid) return "";
            return forVarAssign ? XCom.trmRVarName(orgVarName) : orgVarName;
        }
        catch(final JXMException e) {
            // Print warning as needed
            if( execData.execState.optionStack().enableWarnEvalInvRefVar() ) {
                if( !hasVar(refVarName_) ) {
                    SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_EvalInvRefVar0, refVarName );
                }
                else {
                    SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_EvalInvRefVar1, refVarName );
                }
            }
            // Return a special marker name
            return "!!!___this_reference_variable_contains_an_invalid_reference___!!!";
        }
    }

    private XCom.VariableValue _getReferencedVarValue(final ExecBlock execBlock, final XCom.ExecData execData, final String refVarName)
    {
        // Evaluate the real variable name
        try {
            // Get the real variable name
            final String orgVarName = _getReferencedVarName(execBlock, execData, refVarName, false);
            // Return an empty value if the name is empty
            if( orgVarName.isEmpty() ) return XCom.VarVal_EmptyValue;
            // Evaluate and return the value
            return _evalVar( execBlock, execData, new XCom.VariableStore(false, orgVarName) );
        }
        catch(final JXMException e) {
            // Print warning as needed
            if( execData.execState.optionStack().enableWarnEvalInvRefVar() ) {
                if( !hasVar( XCom.extractRefVarName(refVarName) ) ) {
                    SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_EvalInvRefVar0, refVarName );
                }
                else {
                    SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_EvalInvRefVar1, refVarName );
                }
            }
            // Return an empty value
            return XCom.VarVal_EmptyValue;
        }
    }

    public XCom.VariableValue getVar(final ExecBlock execBlock, final XCom.ExecData execData, final String name, final boolean evaluated)
    {
        // Check for indirect (reference) variable name
        if( name.startsWith("${^") ) {
            // Check if the 'evaluated' flag is not set, simply return the original string
            if(!evaluated) return new XCom.VariableValue( new XCom.VariableStore(false, name) );
            // Evaluate the real variable value
            return _getReferencedVarValue(execBlock, execData, name);
        }

        // Get the variable value
        XCom.VariableValue varVal = null;

        if(                   !_varStack.empty() ) varVal = _varStack.peek().get(name); // Try to get from the local    variable map first
        if( varVal == null && !_argStack.empty() ) varVal = _argStack.peek().get(name); // Try to get from the argument variable map next
        if( varVal == null                       ) varVal =          _varMap.get(name); // Try to get from the global   variable map last

        // Check if the variable is deprecated
        final String depreFor = (varVal == null) ? null : varVal.getDeprecated();
        if(depreFor != null) {
            if( depreFor.isEmpty() ) {
                SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_DeprecatedUsrVar0, name);
            }
            else {
                SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_DeprecatedUsrVar1, name, depreFor);
            }
        }

        // Return the variable value
        return varVal;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final long USerialNumberMMask = SysUtil.random64();
    private static       long _uSerialNumberCnt  = 0;

    private synchronized static String _newUSerialNumber()
    {
        ++_uSerialNumberCnt;
        return String.format("%016x", _uSerialNumberCnt ^ USerialNumberMMask);
    }

    private void setSVar(final String name, XCom.VariableValue value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    { setVar( XCom.genSVarName(name), value, concat, onlyIfNotSet ); }

    private void setSVar(final String name, XCom.VariableValue value) throws JXMException
    { setSVar(name, value, false, false); }

    private void setSVar(final String name, final XCom.VariableStore value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    { setSVar(name, new XCom.VariableValue(value), concat, onlyIfNotSet); }

    private void setSVar(final String name, final XCom.VariableStore value) throws JXMException
    { setSVar(name, value, false, false);  }

    private XCom.VariableValue getSVar(final ExecBlock execBlock, final XCom.ExecData execData, final XCom.SVarSpec svarSpec) throws JXMException
    {
        // Create a new variable value
        XCom.VariableValue varVal = new XCom.VariableValue();

        // Simply return the value if the variable has a constant value
        if( !svarSpec.isEvaluated() ) {
            // NOTE : Currently, 'constVal' always composed of one string
            varVal.add( new XCom.VariableStore(true, svarSpec.constVal) );
            return varVal;
        }

        // Check if the variable has an automatic value
        final int NumOfDummyPreq = 7;

        if(svarSpec.autoVal) {

            // Process based on the name
            switch(svarSpec.svarName) {

                // '$[cmdecho]'
                case cmdecho:
                    varVal.add( new XCom.VariableStore( true, getCmdEcho() ? XCom.Str_T : XCom.Str_F ) );
                    break;

                // '$[cmdstreaming]'
                case cmdstreaming:
                    varVal.add( new XCom.VariableStore( true, getCmdStreaming() ? XCom.Str_T : XCom.Str_F ) );
                    break;

                // '$[lserr]'
                case lserr:
                    varVal.add( new XCom.VariableStore( true, getLastSupError() ) );
                    break;

                // '$[jxmakefile]'
                case jxmakefile:
                    varVal.add( new XCom.VariableStore( true, getJxMakeFile() ) );
                    break;

                // '$[function]'
                case function:
                    varVal.add( new XCom.VariableStore( true, activeFunctionName() ) );
                    break;

                // '$[cmdtargets]'
                case cmdtargets:
                    for(final String name : _cmdTargetNames) {
                        varVal.add( new XCom.VariableStore(true, name) );
                    }
                    break;

                // '$[usn]'
                case usn:
                    varVal.add( new XCom.VariableStore( true, _newUSerialNumber() ) );
                    break;

                // '$[target]'
                case target:
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) varVal.add( new XCom.VariableStore(true, "DUMMY_TARGET.obj") );
                        break;
                    }
                    // Return the block name
                    else {
                        final Target target = _targetStack.peek();
                        varVal.add( new XCom.VariableStore( true, target.targetNameEvaled() ) );
                    }
                    break;

                // '$[preq^]'
                case preqCount:
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) varVal.add( new XCom.VariableStore( true, String.valueOf(NumOfDummyPreq) ) );
                        break;
                    }
                    // Return the number of the evaluated prerequisite(s)
                    else {
                        final Target target = _targetStack.peek();
                        varVal.add( new XCom.VariableStore( true, String.valueOf( target.preqNamesEvaled().size() ) ) );
                    }
                    break;

                // '$[preq*]'
                case preqAll:
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) {
                            for(int i = 1; i <= NumOfDummyPreq; ++i) varVal.add( new XCom.VariableStore(true, "P" + String.valueOf(i) + ".cpp") );
                        }
                        break;
                    }
                    // Return the all the name(s) of the evaluated prerequisite(s)
                    else {
                        final Target target = _targetStack.peek();
                        for( final String name : target.preqNamesEvaled() ) {
                            varVal.add( new XCom.VariableStore(true, name) );
                        }
                    }
                    break;

                // '$[preq?]'
                case preqMoreRecent:
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) {
                            for(int i = 2; i <= NumOfDummyPreq; i += 2) varVal.add( new XCom.VariableStore(true, "P" + String.valueOf(i) + ".cpp") );
                        }
                        break;
                    }
                    // Return the all the name(s) of the evaluated prerequisite(s) that is/are more up to date than the target
                    else {
                        final Target target = _targetStack.peek();
                        for( final String name : target.preqNamesMoreUpToDate() ) {
                            varVal.add( new XCom.VariableStore(true, name) );
                        }
                    }
                    break;

                // '$[preqN]'
                case preqN:
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) {
                            if(svarSpec.autoIndex <= NumOfDummyPreq) {
                                varVal.add( new XCom.VariableStore(true, "P" + String.valueOf(svarSpec.autoIndex) + ".cpp") );
                            }
                        }
                        break;
                    }
                    // Return the name of the evaluated prerequisite using an index
                    else {
                        final Target            target = _targetStack.peek();
                        final ArrayList<String> enames = target.preqNamesEvaled();
                        if( svarSpec.autoIndex <= enames.size() ) {
                            varVal.add( new XCom.VariableStore( true, enames.get(svarSpec.autoIndex - 1) ) );
                        }
                    }
                    break;

                // '$[preq:V]'
                case preqV:
                    // Evaluate the index variable
                    final long idx = XCom.toLong(
                                         execBlock,
                                         execData,
                                         XCom.flatten(
                                             _evalVar( execBlock, execData, new XCom.VariableStore(false, svarSpec.constVal) ),
                                              ""
                                         )
                                     );
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) {
                            if(idx >= 1 && idx <= NumOfDummyPreq) {
                                varVal.add( new XCom.VariableStore(true, "P" + String.valueOf(idx) + ".cpp") );
                            }
                        }
                        break;
                    }
                    // Return the name of the evaluated prerequisite using an index
                    else {
                        final Target            target = _targetStack.peek();
                        final ArrayList<String> enames = target.preqNamesEvaled();
                        if( idx >= 1 && idx <= enames.size() ) {
                            varVal.add( new XCom.VariableStore( true, enames.get( (int) idx - 1 ) ) );
                        }
                    }
                    break;

                // '$[preq+]'
                case preqXManual:
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) {
                            for(int i = 1; i <= NumOfDummyPreq; ++i) varVal.add( new XCom.VariableStore(true, "M" + String.valueOf(i) + ".cpp") );
                        }
                        break;
                    }
                    // Return the all the name(s) of the evaluated prerequisite(s)
                    else {
                        final Target target = _targetStack.peek();
                        for( final String name : target.extraDepsManualEvaled() ) {
                            varVal.add( new XCom.VariableStore(true, name) );
                        }
                    }
                    break;

                // '$[preq%]'
                case preqXAutoDet:
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) {
                            for(int i = 1; i <= NumOfDummyPreq; ++i) varVal.add( new XCom.VariableStore(true, "A" + String.valueOf(i) + ".cpp") );
                        }
                        break;
                    }
                    // Return the all the name(s) of the evaluated prerequisite(s)
                    else {
                        final Target target = _targetStack.peek();
                        for( final String name : target.extraDepsAutoDetect() ) {
                            varVal.add( new XCom.VariableStore(true, name) );
                        }
                    }
                    break;

                // '$[preq~]'
                case preqEffective:
                    // Check if the program state stack is empty
                    if( _targetStack.empty() ) {
                        if(_enableDummyTP) {
                            for(int i = 1; i <= NumOfDummyPreq; ++i) varVal.add( new XCom.VariableStore(true, "E" + String.valueOf(i) + ".cpp") );
                        }
                        break;
                    }
                    // Return the all the name(s) of the evaluated prerequisite(s)
                    else {
                        final Target target = _targetStack.peek();
                        for( final String name : target.extraDepsEffective() ) {
                            varVal.add( new XCom.VariableStore(true, name) );
                        }
                    }
                    break;

                // '$[excode]'
                case excode:
                    varVal.add( new XCom.VariableStore( true, _shellOper.getExitCode(this) ) );
                    break;

                // '$[stderr]'
                case stderr:
                    varVal.add( new XCom.VariableStore( true, _shellOper.getStderrText(this) ) );
                    break;

                // '$[stdout]'
                case stdout:
                    varVal.add( new XCom.VariableStore( true, _shellOper.getStdoutText(this) ) );
                    break;

                // Invalid variable name
                default:
                    throw XCom.newJXMFatalLogicError( Texts.EMsg_InvalidSVarName, svarSpec.svarName.name() ); // NOTE : This should never got executed!

            } // switch svarSpec.svarName

            // Return the value
            return varVal;

        } // if

        // Get the variable as if it were a normal variable
        return getVar( execBlock, execData, XCom.genSVarName( svarSpec.svarName.name() ), true );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _pushXVarStack(final XCom.VariableStack varStack)
    {
        // Create a new variable map
        final XCom.VariableMap newVarMap = new XCom.VariableMap();

        // If the stack is empty, simply store the new variable map to the stack
        if( varStack.empty() ) {
            varStack.push(newVarMap);
        }
        // Otherwise, clone the contents of the variable map on the stack top
        else {
            // Clone the contents
            for( final Map.Entry<String, XCom.VariableValue> entry : varStack.peek().entrySet() ) {
                // Get the name and value
                final String             name  = entry.getKey  ();
                final XCom.VariableValue value = entry.getValue();
                // Clone the value(s)
                final XCom.VariableValue varVal = new XCom.VariableValue();
                for(final XCom.VariableStore item : value) varVal.add(item);
              //for(final XCom.VariableStore item : value) varVal.add( item.deepClone() );
                // Store the name and value
                newVarMap.put(name, varVal);
            }
            // Store the new variable map to the stack
            varStack.push(newVarMap);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void pushVarStack()
    { _pushXVarStack(_varStack); }

    private void popVarStack()
    { _varStack.pop(); }

    private int varStackSize()
    { return _varStack.size(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void pushArgStack()
    {  _pushXVarStack(_argStack); }

    private void popArgStack()
    { _argStack.pop(); }

    private int argStackSize()
    { return _argStack.size(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void pushProgState(final ContainerBlock blockObject)
    {
        _progStateStack.push( new XCom.ProgState(blockObject) );

        if(blockObject instanceof Target) _targetStack.push( (Target) blockObject );

        pushArgStack();
        pushVarStack();
    }

    protected void popProgState()
    {
        popVarStack();
        popArgStack();

        final XCom.ProgState progState = _progStateStack.pop();

        if(progState.blockObject instanceof Target) _targetStack.pop();
    }

    protected int progStateStackSize()
    { return _progStateStack.size(); }

    protected String activeBlockName()
    {
        if( _progStateStack.empty() ) return null;

        return _progStateStack.peek().blockObject.getBlockName();
    }

    private String activeFunctionName()
    {
        if( _progStateStack.empty() ) return null;

        final ContainerBlock blockObject = _progStateStack.peek().blockObject;

        return (blockObject instanceof Function) ? blockObject.getBlockName() : null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private XCom.VariableValue _evalVar_part(final ExecBlock execBlock, final XCom.ExecData execData, final XCom.VariableValue varVal, final XCom.VariableStore refRegExp) throws JXMException
    {
        // Evaluate the variable recursively
        XCom.VariableValue evalVal = new XCom.VariableValue();

        for(final XCom.VariableStore part : varVal) {
            // If the part is a variable, evaluate it
            if( part.isVar() ) {
                for( final XCom.VariableStore eval : _evalVar(execBlock, execData, part) ) {
                    evalVal.add( eval.evaluateRegExp(execBlock, execData, refRegExp) );
                }
                continue;
            }
            // Otherwise, simply store it
            else {
                evalVal.add( part.evaluateRegExp(execBlock, execData).evaluateRegExp(execBlock, execData, refRegExp) );
            }
        }

        // Return the fully evaluated variable
        return evalVal;
    }

    private XCom.VariableValue _evalVar(final ExecBlock execBlock, final XCom.ExecData execData, final XCom.VariableStore varStore) throws JXMException
    {
        // Try to get the special-variable or special-variable-shortcut specification
        final XCom.SVarSpec svarSpec = XCom.getSVarSpec(varStore.value);

        // Get the variable value
        final XCom.VariableValue varVal = (svarSpec != null) ? getSVar(execBlock, execData, svarSpec            )
                                                             : getVar (execBlock, execData, varStore.value, true);

        // Return an empty value as needed
        if(varVal == null) {
            // Print warning as needed
            if( execData.execState.optionStack().enableWarnEvalVarNotExist() ) {
                SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_EvalVarNotExist, varStore.value );
            }
            // Return an empty value
            return XCom.VarVal_EmptyValue; // @@@ XCom.VarVal_EmptyString
        }

        // Evaluate the variable recursively
        return _evalVar_part(execBlock, execData, varVal, varStore);
    }

    protected XCom.VariableValue readVar(final ExecBlock execBlock, final XCom.ExecData execData, final XCom.ReadVarSpec rvarSpec, final boolean evaluated) throws JXMException
    {
        // Simply return the value if the variable contains a constant value
        if( rvarSpec.constant ) return rvarSpec.getConstValue();

        // Check if the 'evaluated' flag is not set
        if(!evaluated) {
            // Check if it is a special-variable or special-variable-shortcut
            if( rvarSpec.isSVar() ) {
                // Get the special-variable or special-variable-shortcut specification
                final XCom.SVarSpec svarSpec = rvarSpec.getSVarSpec();
                // Check if it is a writable special-variable-shortcut
                if( svarSpec.isWritableSCut() ) {
                    return new XCom.VariableValue( new XCom.VariableStore(false, rvarSpec.getVarName() ) );
                }
                // Other variable types
                final XCom.VariableValue varVal = getSVar(execBlock, execData, svarSpec);
                if(rvarSpec.regexp != null) {
                    for(final XCom.VariableStore part : varVal) {
                        part.regexp      = rvarSpec.regexp;
                        part.replacement = rvarSpec.replacement;
                    }
                }
                return varVal;
            }
            // It is a normal variable
            return new XCom.VariableValue( new XCom.VariableStore(
                rvarSpec.constant,
                rvarSpec.getVarName(),
                rvarSpec.regexp,
                rvarSpec.replacement
            ) );
        }

        // Prepare the reference regular expression
        XCom.VariableStore refRegExp = null;

        if(rvarSpec.regexp != null) {
            refRegExp = new XCom.VariableStore(true, null); // The value is ignored; only the regular expression and replacement string will be used
            refRegExp.regexp      = rvarSpec.regexp;
            refRegExp.replacement = rvarSpec.replacement;
        }

        // Get the variable value
        final boolean isSVar = rvarSpec.isSVar();
        final boolean isCP   = ( isSVar && rvarSpec.getSVarSpec().svarName == XCom.SVarName.__class_paths__ );

        XCom.VariableValue varVal = isSVar ? getSVar( execBlock, execData, rvarSpec.getSVarSpec()       )
                                           : getVar ( execBlock, execData, rvarSpec.getVarName (), true );
        if(varVal == null) {
            if(isCP) {
                // If reading class paths, ensure the project root directory is included in the search path
                return XCom.VarVal_DotString;
            }
            else {
                // Print warning as needed
                if( execData.execState.optionStack().enableWarnEvalVarNotExist() ) {
                    SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_EvalVarNotExist, rvarSpec.getVarName() );
                }
            }
            return XCom.VarVal_EmptyValue; // @@@ XCom.VarVal_EmptyString
        }

        // Evaluate the variable recursively
        final XCom.VariableValue eVarVal = _evalVar_part(execBlock, execData, varVal, refRegExp);

        if(isCP) {
            // If reading class paths, ensure the project root directory is included in the search path
            if( !eVarVal.containsConst(".") ) eVarVal.add(0, XCom.VarStr_DotString);
        }

        // Return the evaluated value(s)
        return eVarVal;
    }

    private XCom.VariableValue _setVar(final String name, final boolean local, final boolean const_, final XCom.VariableValue value, final boolean concat, final boolean onlyIfNotSet) throws JXMException
    {
        // Check if the variable a writable special-variable-shortcut
        if( XCom.isWritableSVarSCut(name) ) {
            // Defining a constant variable is not allowed in this context
            if(const_) throw XCom.newJXMRuntimeError(Texts.EMsg_InvalidConstVarContext);
            // Set the variable and return the instance
            if(local) return setLocalVar( XCom.genSVarName(name), value, concat, onlyIfNotSet );
            else      return setVar     ( XCom.genSVarName(name), value, concat, onlyIfNotSet );
        }

        // It is a normal variable
        final String varName = XCom.genRVarName(name);

        if(value == null) {
            // Defining a constant variable is not allowed in this context
            if(const_) throw XCom.newJXMRuntimeError(Texts.EMsg_InvalidConstVarContext);
            // Set the variable and return the instance
            if(local) return setLocalVar(varName, XCom.VarVal_EmptyValue, concat, onlyIfNotSet); // @@@ XCom.VarVal_EmptyString
            else      return setVar     (varName, XCom.VarVal_EmptyValue, concat, onlyIfNotSet); // @@@ XCom.VarVal_EmptyString
        }

        // Set the variable and return the instance
        if(local) return setLocalVar(varName, value, concat, onlyIfNotSet);
        else      return setVar     (varName, value, concat, onlyIfNotSet);
    }

    private boolean _hasVar(final String name, final boolean local)
    {
        // Determine the effective variable name
        final String varName = XCom.isWritableSVarSCut(name) ? XCom.genSVarName(name)
                                                             : XCom.genRVarName(name);

        // Check if the variable already exists
        return local ? hasLocalVar(varName) : hasVar(varName);
    }

    // <var_name>   = ...
    // <var_name>  := ...
    protected void assignVar(final ExecBlock execBlock, final XCom.ExecData execData, final String name, final boolean local, final boolean const_, final XCom.ReadVarSpecs rvarSpecs, final boolean direct) throws JXMException
    {
        // Check if this is actually an 'echo' or 'echoln' statement
        if( name.equals(XCom.SVar_Echo) || name.equals(XCom.SVar_Echoln) ) {
            String str = "";
            for(final XCom.ReadVarSpec item : rvarSpecs) str += XCom.flatten( readVar(execBlock, execData, item, true), "" );
            if( name.equals(XCom.SVar_Echoln) ) SysUtil.stdOut().println(str);
            else                                SysUtil.stdOut().print  (str);
            return;
        }

        // Evaluate the real variable name as needed
        final String rname = XCom.isPlainRefVarName(name) ? _getReferencedVarName( execBlock, execData, XCom.genRVarName(name), true ) : name;

        if( rname.isEmpty() ) return; // Simply return if the name is empty

        // Check if the right hand side is empty
        if( rvarSpecs.isEmpty() ) {
            _setVar(rname, local, const_, XCom.VarVal_EmptyValue, false, false); // @@@ XCom.VarVal_EmptyString
            return;
        }

        // Read the variable values
        final ArrayList<XCom.VariableValue> valVals = new ArrayList<>();

        for(final XCom.ReadVarSpec item : rvarSpecs) {
            final XCom.VariableValue varVal = readVar(execBlock, execData, item, direct);
            valVals.add(varVal);
        }

        // Assign the variable values
        XCom.VariableValue dstVarVal = null;

        for(int i = 0; i < valVals.size(); ++i) {
            dstVarVal = _setVar( rname, local, const_, valVals.get(i), (i != 0), false ); // Set the first item, concatenate the remaining items
        }

        // Set it as a constant variable as needed
        if(const_ && dstVarVal != null) dstVarVal.setConstantVariable();
    }

    // <var_name>  += ...
    // <var_name> :+= ...
    protected void assignVarConcat(final ExecBlock execBlock, final XCom.ExecData execData, final String name, final boolean local, final XCom.ReadVarSpecs rvarSpecs, final boolean direct) throws JXMException
    {
        // Evaluate the real variable name as needed
        final String rname = XCom.isPlainRefVarName(name) ? _getReferencedVarName( execBlock, execData, XCom.genRVarName(name), true ) : name;

        if( rname.isEmpty() ) return; // Simply return if the name is empty

        // Concatenate the variable value
        for(final XCom.ReadVarSpec item : rvarSpecs) {
            final XCom.VariableValue varVal = readVar(execBlock, execData, item, direct);
            _setVar(rname, local, false, varVal, true, false);
        }
    }

    // <var_name>  ?= ...
    // <var_name> :?= ...
    protected void assignVarIfNotSet(final ExecBlock execBlock, final XCom.ExecData execData, final String name, final boolean local, final XCom.ReadVarSpecs rvarSpecs, final boolean direct) throws JXMException
    {
        // Evaluate the real variable name as needed
        final String rname = XCom.isPlainRefVarName(name) ? _getReferencedVarName( execBlock, execData, XCom.genRVarName(name), true ) : name;

        if( rname.isEmpty() ) return; // Simply return if the name is empty

        // Clone the inherited value as needed
        if( local && !hasLocalVar(rname) ) {
            final String             rvarName = XCom.genRVarName(rname);
            final XCom.SVarSpec      svarSpec = XCom.getSVarSpec(rname);
                  XCom.VariableValue varVal   = (svarSpec != null) ? getSVar(execBlock, execData, svarSpec        )
                                                                   : getVar (execBlock, execData, rvarName, direct);
          if( ( varVal != null && !varVal.isEmpty() ) || rvarSpecs.isEmpty() ) {
                // Set the local variable
                setLocalVar( (svarSpec != null) ? XCom.genSVarName(rname) : rvarName, varVal, false, false );
            }
        }

        // Assign the variable value as needed
        if( !_hasVar(rname, local) ) assignVar(execBlock, execData, rname, local, false, rvarSpecs, direct);
    }

    protected void unsetVar(final String name) throws JXMException
    { _delVar( XCom.genRVarName(name) ); }

    protected void deprecateVar(final String name, final String replacement) throws JXMException
    { _depVar( XCom.genRVarName(name), replacement ); }

    public String hasVarExt(final String name) throws JXMException
    { return _hasVarExt( XCom.genRVarName(name) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.VariableValue setCBVar(final String name, final XCom.VariableValue value) throws JXMException
    { return setVar(name, value, false, false); }

    public void delCBVar(final String name) throws JXMException
    { delGlobalVar(name); }

} // class ExecState
