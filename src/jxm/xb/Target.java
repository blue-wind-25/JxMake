/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jxm.*;
import jxm.dl.*;


public class Target extends ContainerBlock implements XCom.LabelMapOwner {

    public static String DepForMarker = "\0:\0";
    public static String DepForAll    = "\0*\0";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int _NoPrerequisite = -1;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private       float             _matchScore = 0.0f;

    private final XCom.ReadVarSpec  _targetRVS;

    private       Pattern           _targetGlobRegex;
    private       Matcher           _targetGlobMatcher;

    private       String            _targetNameEvaled;
    private final Stack<String>     _targetNameEvaledStack;

    private final XCom.ReadVarSpecs _preqNames;
    private final ArrayList<String> _preqNamesEvaled;
    private final ArrayList<String> _preqNamesMoreUpToDate;

    private final ArrayList<String> _extraDepsAutoDetect;
    private final XCom.ReadVarSpecs _extraDepsManual;
    private final ArrayList<String> _extraDepsManualEvaled;
    private final ArrayList<String> _extraDepsEffective;

    private final ArrayList<String> _combinedPreqList;

    private final ArrayList<String> _sourceFilePaths;
    private final ArrayList<String> _absCIncludePaths;
    private final ArrayList<String> _absJavaClassPaths;

    private       XCom.LabelMap     _labelMap = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Target(final String path, final int lNum, final int cNum, final String name, final XCom.ReadVarSpec targetRVS, final XCom.ReadVarSpecs preqNames, final boolean allocExecBlocksElem)
    {
        super(path, lNum, cNum, name, allocExecBlocksElem ? 1 : -1);

        _targetRVS             = targetRVS;

        _targetGlobRegex       = null;
        _targetGlobMatcher     = null;

        _targetNameEvaled      = null;
        _targetNameEvaledStack = new Stack<>();

        _preqNames             = XCom.substEmptyReadVarSpecs(preqNames);
        _preqNamesEvaled       = new ArrayList<>();
        _preqNamesMoreUpToDate = new ArrayList<>();

        _extraDepsAutoDetect   = new ArrayList<>();
        _extraDepsManual       = new XCom.ReadVarSpecs();
        _extraDepsManualEvaled = new ArrayList<>();
        _extraDepsEffective    = new ArrayList<>();

        _combinedPreqList      = new ArrayList<>();

        _sourceFilePaths       = new ArrayList<>();
        _absCIncludePaths      = new ArrayList<>();
        _absJavaClassPaths     = new ArrayList<>();
    }

    public Target(final String path, final int lNum, final int cNum, final String name, final XCom.ReadVarSpec targetRVS, final XCom.ReadVarSpecs preqNames)
    { this(path, lNum, cNum, name, targetRVS, preqNames, true); }

    public boolean canAddExtraDeps()
    { return getExecBlocks().isEmpty(); }

    public void addExtraDeps(final XCom.ReadVarSpecs extraDeps)
    {
        if(extraDeps == null) return;

        _extraDepsManual.addAll(extraDeps);
    }

    public void addExtraDep(final XCom.ReadVarSpec extraDep)
    {
        if(extraDep == null) return;

        _extraDepsManual.add(extraDep);
    }

    public void addExtraDep(final String extraDep)
    {
        if( extraDep == null || extraDep.isEmpty() ) return;

        _extraDepsManual.add( new XCom.ReadVarSpec( extraDep.charAt(0) != '$', null, extraDep, null, null ) );
    }

    @Override
    public void putLabel(final String label)
    { _labelMap = XCom.LabelMap.putLabel( _labelMap, label, getExecBlocks().size() ); }

    @Override
    public int getLabel(final String label)
    { return XCom.LabelMap.getLabel(_labelMap, label); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public String targetName()
    { return getBlockName(); }

    public String targetNameEvaled()
    {
        if( !_targetNameEvaledStack.empty() ) {
            final String name = _targetNameEvaledStack.peek();
            if(name != null) return name;
        }

        return (_targetNameEvaled != null) ? _targetNameEvaled : getBlockName();
    }

    private void pushTargetNameEvaled(final XCom.ExecData execData)
    { _targetNameEvaledStack.push(_targetNameEvaled); }

    private void popTargetNameEvaled(final XCom.ExecData execData)
    { _targetNameEvaledStack.pop(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ReadVarSpecs preqNames()
    { return _preqNames; }

    public ArrayList<String> preqNamesEvaled()
    { return _preqNamesEvaled; }

    public ArrayList<String> preqNamesMoreUpToDate()
    { return _preqNamesMoreUpToDate; }

    public ArrayList<String> extraDepsAutoDetect()
    { return _extraDepsAutoDetect; }

    public XCom.ReadVarSpecs extraDepsManual()
    { return _extraDepsManual; }

    public ArrayList<String> extraDepsManualEvaled()
    { return _extraDepsManualEvaled; }

    public ArrayList<String> extraDepsEffective()
    { return _extraDepsEffective; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean globMatch(final String checkName)
    {
        // Get the original non-evaluated target name
        final String targetName = targetName();

        // Exit if it does not define a pattern rule
        boolean hasGlob = false;

        for(int i = 0; i < targetName.length(); ++i) {
            final char ch = targetName.charAt(i);
            if(ch == '?' || ch == '%') {
                hasGlob = true;
                break;
            }
        }

        if(!hasGlob) return false;

        // Convert the glob expression  to regular expression as needed
        if(_targetGlobRegex == null) _targetGlobRegex = XCom.globToRegExp(targetName);

        // Check if the given target name satisfies a full match
        _targetGlobMatcher = _targetGlobRegex.matcher(
            SysUtil.pathIsAbsolute(checkName) ? SysUtil.resolveRelativePath(checkName) : checkName
        );

        if( _targetGlobMatcher.matches() ) {
            // Got a full match, evaluate the target name
            _targetNameEvaled = _targetGlobMatcher.reset().replaceAll( XCom.globToReplacementStr(targetName, false) );
            return true;
        }

        // Clear the matcher
        _targetGlobMatcher = null;

        // No match
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class ExecTargetResult {
        public final XCom.ExecuteResult executeResult;
        public final      ExecBlock     errExecBlock;

        public ExecTargetResult(final XCom.ExecuteResult executeResult_, final ExecBlock errExecBlock_)
        {
            executeResult = executeResult_;
            errExecBlock  = errExecBlock_;
        }
    }

    public static ExecTargetResult execTargets(final String mainJXMSpecFile_absPath, final XCom.ExecData execData, final XCom.ExecBlocks execBlocks, final ArrayList<String> targetNames)
    {
        // Execute the targets
        for(final String targetName : targetNames) {

            // Try to find an exact match for the target instance
            Target target = execData.targetMap.get(targetName);

            // If there was no exact match, use glob matching
            if(target == null) {
                float matchScore = -Float.MAX_VALUE;
                for( final Map.Entry<String, Target> entry : execData.targetMap.entrySet() ) {
                    if( entry.getValue().globMatch(targetName) ) {
                        final Target foundTarget = entry.getValue();
                        if(foundTarget._matchScore > matchScore) {
                            target     = foundTarget;
                            matchScore = foundTarget._matchScore;
                        }
                    }
                }
                if(target != null) {
                    target = (Target) target.deepClone(); // Do a deep clone because the same rules can be used by other prerequisite(s)!
                }
            }

            // ##### ??? TODO : Should the deep clone be done unconditionally, even if it is an exact match ??? #####

            // Check if there was no match
            if(target == null) {
                return new ExecTargetResult(
                    XCom.ExecuteResult.Error,
                    VoidBlock.newErrorVoidBlock00(
                        ( (execBlocks == null) || execBlocks.isEmpty() ) ? mainJXMSpecFile_absPath : execBlocks.get(0).jxmSpecFile_absPath(),
                        XCom.errorString(Texts.EMsg_TargetDefNotDefined, targetName)
                    )
                );
            }

            // Execute the target's execution-blocks
            final XCom.ExecuteResult xres = target.executeBody(execData);

            switch(xres) {
                case Done            :                                break                                    ;
                case Done_NoUpdate   :                                break                                    ;
                case Error           :                                return new ExecTargetResult(xres, target);
                case SuppressedError : target.printSuppressedError(); break                                    ;
                case ProgramExit     :                                return new ExecTargetResult(xres, null  );
                default              :                                return new ExecTargetResult(xres, target); // NOTE : This should never got executed!
            } // switch

            // Check for errors that may have been missed
            if( target.isErrorBlockSet() ) return new ExecTargetResult(xres, target);

        } // for

        // Done
        return new ExecTargetResult(XCom.ExecuteResult.Done, null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _addToTargetMap(final XCom.ExecData execData, final String targetName, final Target targetInstane)
    {
        // Check if a target with the same name already exists
        final Target chk = execData.targetMap.get(targetName);

        if(chk != null) {
            setErrorFromString( JXMRuntimeError.PrefixString + Texts.EMsg_TargetDefExist, targetName, chk.jxmSpecFile_absPath(), chk.jxmSpecFileLNum(), chk.jxmSpecFileCNum() );
            return false;
        }

        // Calculate the match score
        int score = 0;
        int total = targetName.length();
        for(int i = 0; i < total; ++i) {
            switch( targetName.charAt(i) ) {
                case '?' : score -= 1; break;
                case '%' : score -= 2; break;
                default  : score += 1; break;
            }
        }

        targetInstane._matchScore = (float) score / (float) total;

        // Store to the target map
        execData.targetMap.put(targetName, targetInstane);

        // Done
        return true;
    }

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // NOTE : Instead of executing the execution-blocks, this function registers this target to the target map

        // Check if the block (target) name is a variable evaluation
        if( getBlockName().charAt(0) == '$' ) {

            // Evaluate the target name
            final XCom.VariableValue varVal = execData.execState.readVar(this, execData, _targetRVS, true);

            if( varVal.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_TargetDefNameEmpty);

            // If there is only one result, simply change the block name to the evaluated target name
            if( varVal.size() == 1 ) {

                final String targetName = varVal.get(0).value.trim();
                if( targetName.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_TargetDefNameEmpty);

                setBlockName(targetName);

            }
            // If there are multiple results, clone and store the targets
            else {

                for( final XCom.VariableStore item : varVal ) {

                    final String targetName = item.value.trim();
                    if( targetName.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_TargetDefNameEmpty);

                    final Target clonedTarget = (Target) this.deepClone(); // Do a deep clone so that each unique rule has its own unique instance!
                    clonedTarget.setBlockName(targetName);

                    if( !_addToTargetMap(execData, targetName, clonedTarget) ) return XCom.ExecuteResult.Error;

                } // for

                return XCom.ExecuteResult.Done;

            } // if
        }

        // Add to the target map
        return _addToTargetMap( execData, getBlockName(), this ) ? XCom.ExecuteResult.Done : XCom.ExecuteResult.Error;
    }

    public void execute(final XCom.ExecData execData, final String targetName, final ArrayList<String> preqNames, final ArrayList<String> extraDeps) throws JXMException
    {
        final XCom.ReadVarSpecs rvsPreqNames = (preqNames != null) ? new XCom.ReadVarSpecs() : null;
        final XCom.ReadVarSpecs rvsExtraDeps = (extraDeps != null) ? new XCom.ReadVarSpecs() : null;

        if(preqNames != null) { for(final String item : preqNames) rvsPreqNames.add( new XCom.ReadVarSpec( item.charAt(0) != '$', null, item, null, null ) ); }
        if(extraDeps != null) { for(final String item : extraDeps) rvsExtraDeps.add( new XCom.ReadVarSpec( item.charAt(0) != '$', null, item, null, null ) ); }

        final Target target = new Target(
            getPath(),
            getLNum(),
            getCNum(),
            targetName,
            new XCom.ReadVarSpec( targetName.charAt(0) != '$', null, targetName, null, null ),
            rvsPreqNames,
            false
        );

        target.addExtraDeps(rvsExtraDeps);

        target._labelMap = _labelMap;

        target.setExecBlocks( 0, getExecBlocks(0) );

        if( target.execute(execData) != XCom.ExecuteResult.Done ) {
            throw XCom.newJXMRuntimeError( target.getErrorString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class EPRes {
        final XCom.ExecuteResult xres;
        final int                preqExecCount;

        public EPRes(final XCom.ExecuteResult xres_, final int preqExecCount_)
        {
            xres          = xres_;
            preqExecCount = preqExecCount_;
        }
    }

    private static class EBRes {
        final XCom.ExecuteResult xres;
        final boolean            bodyExecuted;

        public EBRes(final XCom.ExecuteResult xres_, final boolean bodyExecuted_)
        {
            xres         = xres_;
            bodyExecuted = bodyExecuted_;
        }
    }

    private XCom.ExecuteResult _executeBody_evalPreqNames(final XCom.ExecData execData)
    {
        // Execution result
        XCom.ExecuteResult xres = XCom.ExecuteResult.Done;

        // A transient variable to hold the name of the prerequisite target or the extra dependency item
        // to be evaluated/executed immediately after this
        String curProcName = null;

        // Ensure exceptions are captured and converted into error message
        try {

            // Evaluate the prerequisite name(s)
            for(final XCom.ReadVarSpec readVarSpec : _preqNames) {

                // Save the current prerequisite name and evaluate the prerequisite name variable
                XCom.VariableValue varVal = null;
                if(readVarSpec.svarSpec == null) {
                    // Save the current prerequisite name
                    curProcName = readVarSpec.getVarName();
                    // Evaluate the prerequisite name variable
                    varVal = execData.execState.readVar(this, execData, readVarSpec, true);
                }
                else {
                    // Save the current prerequisite name
                    curProcName = readVarSpec.svarSpec.getStrName();
                    // Error if the special-variable is not '$[target]'
                    if( readVarSpec.svarSpec.svarName != XCom.SVarName.target ) {
                        throw XCom.newJXMRuntimeError( Texts.EMsg_TargetPreqNameInvalidSV, targetNameEvaled(), curProcName );
                    }
                    // Store the target name
                    varVal = new XCom.VariableValue();
                    varVal.add( new XCom.VariableStore( true, targetNameEvaled() ) );
                    // Evaluate the regular expression as needed
                    if(readVarSpec.regexp != null) {
                        //
                        final XCom.VariableStore refRegExp = new XCom.VariableStore(true, null); // The value is ignored; only the regular expression and replacement string will be used
                        refRegExp.regexp      = readVarSpec.regexp;
                        refRegExp.replacement = readVarSpec.replacement;
                        // Evaluate the regular expression
                        varVal.set( 0, varVal.get(0).evaluateRegExp(this, execData, refRegExp) );
                    }
                    // Check for self-recursion
                    if( varVal.get(0).value.equals( targetNameEvaled() ) ) {
                        throw XCom.newJXMRuntimeError( Texts.EMsg_TargetPreqNameSelfRecur, targetNameEvaled(), readVarSpec.svarSpec.svarName.svName() );
                    }
                }

                // Process the name(s)
                for(final XCom.VariableStore varStore : varVal) {
                    // Get the name
                    String name = varStore.value;
                    // Evaluate the glob expression as needed
                    if(_targetGlobMatcher != null) {
                        try {
                            name = _targetGlobMatcher.reset().replaceAll( XCom.globToReplacementStr(name, true) );
                        }
                        catch(final Exception e) {
                            // Print the stack trace if requested
                            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                            // Throw as a different exception
                            throw XCom.newJXMRuntimeError( Texts.EMsg_RegExpExecError, e.toString() );
                        }
                    }
                    // The target does not contain glob expression
                    else {
                        // Error if the prerequisite contains glob expression
                        if( name.indexOf('%') != -1 ) {
                            throw XCom.newJXMRuntimeError( Texts.EMsg_TargetPreqNameTNoPRule, targetNameEvaled(), name );
                        }
                    }
                    // Add the name
                    _preqNamesEvaled.add(name);
                }

            } // for readVarSpec

        } // try
        catch(final JXMException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set the error message
            setErrorFromString( XCom.errorString( Texts.EMsg_TargetPreqNameEvalError, targetNameEvaled(), curProcName, e.toString() ) );
            xres = XCom.ExecuteResult.Error;
        }

        // Return the result
        return xres;
    }

    private XCom.ExecuteResult _executeBody_getGlobalOrAutoDependencies(final XCom.ExecData execData, final String targetAbsPath)
    {
        // Try to get the dependencies from files loaded using 'depload' and 'sdepload' first
        final TreeSet<String> depFor = GlobalDepLoad.getDepFor(targetAbsPath);

        if(depFor != null) {
            // Clear the list first
            _extraDepsAutoDetect.clear();
            // Set the list from the found tree
            _extraDepsAutoDetect.addAll(depFor);
        }

        // If not found, recursively auto-detect all the dependencies for C, C++, and Java source code files
        else {

            // Clear the list first
            _extraDepsAutoDetect.clear();

            // Push the program state
            execData.execState.pushProgState(this);

            // Loop through the execution-blocks
            for( final ExecBlock item : getExecBlocks() ) {
                // Get the execution block and check if it needs to be executed
                final VarAssign varAssign = (item instanceof VarAssign) ? ( (VarAssign) item ) : null;
                if( varAssign == null || !varAssign.isPreTargetExec() ) break;
                // Execute the execution block
                try {
                    switch( varAssign.execute(execData) ) {
                        case Done            :                                                    break;
                        case Error           : setErrorBlock(item)                              ; break;
                        case SuppressedError : item.printSuppressedError()                      ; break;
                        default              : setErrorFromString(Texts.EMsg_UnknownRuntimeError); break; // NOTE : This should never got executed!
                    }
                }
                catch(final JXMException e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Set the error message and block
                    setErrorBlock(item);
                    if( !getErrorBlock().isErrorStringSet() ) getErrorBlock().setErrorFromString( e.toString() );
                }
            } // for

            // Get the values of the '__include_paths__' and '__class_paths__' variables
            final XCom.VariableValue vvCIncludePaths  = execData.execState.getVar( this, execData, XCom.genSVarName( XCom.SVarName.__include_paths__.name() ), true );
            final XCom.VariableValue vvJavaClassPaths = execData.execState.getVar( this, execData, XCom.genSVarName( XCom.SVarName.__class_paths__  .name() ), true );

            // Pop the program state
            execData.execState.popProgState();

            // Check for error
            if( isErrorStringSet() ) return XCom.ExecuteResult.Error;

            if( isErrorBlockSet() ) {
                setErrorFromBlock( getErrorBlock() );
                return XCom.ExecuteResult.Error;
            }

            // Generate a list of source (prerequisite) file paths
            _sourceFilePaths.clear();
            for(final String name : _preqNamesEvaled) {
                final String preqPath = SysUtil.resolveAbsolutePath(name);
                if( SysUtil.pathIsValidFile(preqPath) ) _sourceFilePaths.add(preqPath);
            }

            // Generate a list of C header paths
            _absCIncludePaths.clear();
            if(vvCIncludePaths != null) {
                for(final XCom.VariableStore item : vvCIncludePaths) {
                    final String p = SysUtil.resolveAbsolutePath(item.value);
                    if( SysUtil.pathIsValidDirectory(p) ) _absCIncludePaths.add(p);
                }
            }

            // Generate a list of Java class paths
            _absJavaClassPaths.clear();
            if(vvJavaClassPaths != null) {
                for(final XCom.VariableStore item : vvJavaClassPaths) {
                    final String p = SysUtil.resolveAbsolutePath(item.value);
                    if( SysUtil.pathIsValidDirectory(p) ) _absJavaClassPaths.add(p);
                }
            }

            // Load or generate the dependency list
            DepBuilder.loadBuildDepList(_extraDepsAutoDetect, _sourceFilePaths, _absCIncludePaths, _absJavaClassPaths);

        } // if

        // Done
        return XCom.ExecuteResult.Done;
    }

    private XCom.ExecuteResult _executeBody_getExtraDependencies(final XCom.ExecData execData)
    {
        // A transient variable to hold the name of the prerequisite target or the extra dependency item
        // to be evaluated/executed immediately after this
        String curProcName = null;

        // Clear the list first
        _extraDepsManualEvaled.clear();

        // Evaluate the extra dependency item(s)
        final ArrayList<String> depItems = new ArrayList<>();

        try {

            // Evaluate the extra dependency item(s)
            for(final XCom.ReadVarSpec readVarSpec : _extraDepsManual) {
                // Save the current extra dependency item
                curProcName = (readVarSpec.svarSpec != null) ? readVarSpec.svarSpec.getStrName()
                                                             : readVarSpec.getVarName();
                // Evaluate the dependency item variable
                final XCom.VariableValue varVal = execData.execState.readVar(this, execData, readVarSpec, true);
                // Add the name(s)
                for(final XCom.VariableStore varStore : varVal) {
                    depItems.add(varStore.value);
                }
            }

            // Store the extra dependency item(s)
            boolean depForceThis = false;
            boolean depForAll    = false;
            String  depForTarget = null;
            for(int i = 0; i < depItems.size(); ++i) {
                // Get the value
                final String value = depItems.get(i);
                if( value.isEmpty() ) continue;
                // Check if it is an indicator for the target name
                if( value.equals(DepForMarker) ) {
                    // Save the target name
                    depForTarget = depItems.get(++i);
                    depForAll    = depForTarget.equals(DepForAll);
                    continue;
                }
                // Check if it is an indicator to force the item to be store even if the file/path is non-existent
                if( value.equals("!") ) {
                    // Set flag
                    depForceThis = true;
                    continue;
                }
                // Skip if the target name does not match
                if( !depForAll && !_targetNameEvaled.endsWith(depForTarget) ) continue;
                // Store the name
                final String path = SysUtil.resolveAbsolutePath(value);
                if( depForceThis || SysUtil.pathIsValid(path) ) {
                    if( !_extraDepsManualEvaled.contains(path) ) _extraDepsManualEvaled.add(path);
                }
                // Reset flag
                depForceThis = false;
            }

        } // try
        catch(final JXMException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set the error message
            setErrorFromString( XCom.errorString( Texts.EMsg_TargetExtraDepEvalError, targetNameEvaled(), curProcName, e.toString() ) );
            return XCom.ExecuteResult.Error;
        }

        // Done
        return XCom.ExecuteResult.Done;
    }

    private boolean _executeBody_combineDependencies(final XCom.ExecData execData, final String targetAbsPath, final boolean targetAbsPathIsValid)
    {
        // Combine '_extraDepsAutoDetect' and '_extraDepsManualEvaled' into a new list, excluding everything
        // that is already inside '_preqNamesEvaled'
        _extraDepsEffective.clear();

        for(final String item : _extraDepsAutoDetect) {
            if( !_preqNamesEvaled.contains(item) ) _extraDepsEffective.add(item);
        }

        for(final String item : _extraDepsManualEvaled) {
            if( !_preqNamesEvaled.contains(item) ) _extraDepsEffective.add(item);
        }

        // Check if any of the extra dependency item(s) is more recent
        final boolean extraDepIsMoreRecent = targetAbsPathIsValid
                                           ? ( !SysUtil.isPathMoreRecent(targetAbsPath, _extraDepsEffective) )  // Check if the target is not more recent than the dependencies
                                           : ( execData.execState.progStateStackSize() != 0                  ); // Otherwise, only further examine non-root target

        // Create a combined dependency list
        _combinedPreqList.clear (                   );
        _combinedPreqList.addAll(_preqNamesEvaled   );
        _combinedPreqList.addAll(_extraDepsEffective);

        // Done
        return extraDepIsMoreRecent;
    }

    private EPRes _executeBody_execPrerequisites(final XCom.ExecData execData, final String targetAbsPath, final boolean targetAbsPathIsValid)
    {
        // Execution result
        XCom.ExecuteResult xres = XCom.ExecuteResult.Done;

        // A transient variable to hold the name of the prerequisite target or the extra dependency item
        // to be evaluated/executed immediately after this
        String curProcName = null;

        // The number of prerequisite(s) actually executed
        int preqExecCount = _preqNamesEvaled.isEmpty() ? _NoPrerequisite : 0;

        // Ensure exceptions are captured and converted into error message
        try {

            // Execute the prerequisite(s)
            for(final String preqName : _combinedPreqList) {

                // Save the current prerequisite name
                curProcName = preqName;

                // Get the prerequisite  target's execution-blocks
                Target preqTarget = execData.targetMap.get(curProcName);

                // If there was no exact match, use glob matching
                if(preqTarget == null) {
                    for( final Map.Entry<String, Target> entry : execData.targetMap.entrySet() ) {
                        if( entry.getValue().globMatch(curProcName) ) {
                            preqTarget = (Target) entry.getValue().deepClone(); // Do a deep clone because the same rules can be used by further prerequisite(s)!
                            break;
                        }
                    }
                }

                // Check if the prerequisite target name refers to a valid path
                final String  pathPreq              = SysUtil.resolveAbsolutePath(curProcName);
                      boolean preqTargetIsValidPath = false;

                if( SysUtil.pathIsValid(pathPreq) ) {
                    // Set flag
                    preqTargetIsValidPath = true;
                    // Increment the counter as needed
                    if(targetAbsPathIsValid) {
                        final boolean targetPathIsMoreRecent = SysUtil.isPathMoreRecent(targetAbsPath, pathPreq);
                        if(!targetPathIsMoreRecent) {
                            // Increment the counter
                            ++preqExecCount;
                            // Add the prerequisite target name to the list of prerequisite(s) that is/are more up to date than the target,
                            // excluding everything that is already inside or also inside '_extraDepsEffective'
                            if( !_preqNamesMoreUpToDate.contains(preqName) && !_extraDepsEffective.contains(preqName) ) _preqNamesMoreUpToDate.add(preqName);
                        }
                        else {
                            // Clear the target instance as needed
                            if( _preqNamesEvaled.isEmpty() ) preqTarget = null;
                        }
                    }
                }

                // Check if there was no match
                if(preqTarget == null) {
                    if(preqTargetIsValidPath) continue;
                    throw XCom.newJXMRuntimeError(Texts.EMsg_TargetDefPreqNotDefined);
                }

                // Push the program state
                execData.execState.pushProgState(preqTarget);

                // Execute the prerequisite target's execution-blocks
                xres = preqTarget.executeBody(execData);

                switch(xres) {

                    case Done:
                        /* FALLTHROUGH */
                    case Done_NoUpdate:
                        // Check for errors that may have been missed
                        if( isErrorBlockSet() ) {
                            setErrorFromBlock( getErrorBlock() );
                            return new EPRes(XCom.ExecuteResult.Error, preqExecCount);
                        }
                        break;

                    case Error:
                        setErrorFromString( XCom.errorString( Texts.EMsg_TargetPreqExecError, targetNameEvaled(), curProcName, preqTarget.sprintError() ) );
                        break;

                    case SuppressedError:
                        preqTarget.printSuppressedError();
                        xres = XCom.ExecuteResult.Done;
                        break;

                    case ProgramExit:
                        break;

                    default:
                        // NOTE : This should never got executed!
                        setErrorFromString( XCom.errorString( Texts.EMsg_TargetPreqExecError, targetNameEvaled(), curProcName, Texts.EMsg_UnknownRuntimeError ) );
                        xres = XCom.ExecuteResult.Error;
                        break;

                } // switch

                // Pop the program state
                execData.execState.popProgState();

                // Check if the prerequisite is not updated
                if(xres == XCom.ExecuteResult.Done_NoUpdate) {
                    xres = XCom.ExecuteResult.Done;
                }
                // The prerequisite is updated
                else if(xres == XCom.ExecuteResult.Done) {
                    // Increment the counter
                    ++preqExecCount;
                    // Add the prerequisite target name to the list of prerequisite(s) that is/are more up to date than the target,
                    // excluding everything that is already inside or also inside '_extraDepsEffective'
                    if( !_preqNamesMoreUpToDate.contains(preqName) && !_extraDepsEffective.contains(preqName) ) _preqNamesMoreUpToDate.add(preqName);
                }

                // Check for error
                if(xres != XCom.ExecuteResult.Done) return new EPRes(xres, preqExecCount);

            } // for _preqNamesEvaled

        } // try
        catch(final JXMException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set the error message
            setErrorFromString( XCom.errorString( Texts.EMsg_TargetPreqExecError, targetNameEvaled(), curProcName, e.toString() ) );
            xres = XCom.ExecuteResult.Error;
        }

        // Check for error
        if(xres != XCom.ExecuteResult.Done) return new EPRes(xres, preqExecCount);

        // Convert the contents of '_preqNamesEvaled' and '_preqNamesMoreUpToDate' to absolute paths as needed
        for(int i = 0; i < _preqNamesEvaled.size(); ++i) {
            final String path = SysUtil.resolveAbsolutePath( _preqNamesEvaled.get(i) );
            if( SysUtil.pathIsValid(path) ) _preqNamesEvaled.set(i, path);
        }

        for(int i = 0; i < _preqNamesMoreUpToDate.size(); ++i) {
            final String path = SysUtil.resolveAbsolutePath( _preqNamesMoreUpToDate.get(i) );
            if( SysUtil.pathIsValid(path) ) _preqNamesMoreUpToDate.set(i, path);
        }

        // Return the result
        return new EPRes(xres, preqExecCount);
    }

    private EBRes _executeBody_execBody(final XCom.ExecData execData, final int blockIndex, final boolean targetAbsPathIsValid, final int preqExecCount, final boolean extraDepIsMoreRecent)
    {
        // Execution result
        XCom.ExecuteResult xres = XCom.ExecuteResult.Done;

        // Execute the body
        /*
         * The target's body is executed if:
         *     # There is no prerequisite specified at all.
         *     # At least one of its prerequisite(s) is executed ('preqExecCount' is not zero).
         *     # At least one of the auto-detected and/or manually-added extra dependencies are more recent
         *       than the target (only if the target is a file).
         */

        boolean bodyExecuted = false;

        if(preqExecCount != 0 || extraDepIsMoreRecent) {

            if(targetAbsPathIsValid && preqExecCount == _NoPrerequisite) {
                // In this case, there is no need to execute the body if the target is a file and it exists
            }

            else {

                // Push the program state
                execData.execState.pushProgState(this);

                // Execute the body and set the flag
                xres = super.executeBody(execData, blockIndex);

                // Set flag as needed
                bodyExecuted = (xres != XCom.ExecuteResult.Done_NoUpdate);

                // Pop the program state
                execData.execState.popProgState();

            } // if

        } // if

        // Check for error
        if( isErrorBlockSet() ) {
            setErrorFromBlock( getErrorBlock() );
        }

        // Return the result
        return new EBRes(xres, bodyExecuted);
    }

    @Override
    public XCom.ExecuteResult executeBody(final XCom.ExecData execData, final int blockIndex)
    {
        // Execution result
        XCom.ExecuteResult xres = XCom.ExecuteResult.Done;

        // A transient variable to hold the name of the prerequisite target or the extra dependency item
        // to be evaluated/executed immediately after this
        String curProcName = null;

        // If '_targetNameEvaled' is null set it to the block name
        if(_targetNameEvaled == null) _targetNameEvaled = getBlockName();

        // Convert the content of '_targetNameEvaled' to absolute path as needed
        final String  targetAbsPath        = SysUtil.resolveAbsolutePath(_targetNameEvaled);
        final boolean targetAbsPathIsValid = SysUtil.pathIsValid(targetAbsPath);

        if(targetAbsPathIsValid) _targetNameEvaled = targetAbsPath;

        // Push the evaluated target name
        pushTargetNameEvaled(execData);

        // Evaluate the prerequisite name(s) =====
        _preqNamesEvaled.clear();
        _preqNamesMoreUpToDate.clear();

        if(xres == XCom.ExecuteResult.Done) {
            if(_preqNames != null) xres = _executeBody_evalPreqNames(execData);
        }

        // Get the global or auto-detected dependency items(s)
        if(xres == XCom.ExecuteResult.Done) {
            xres = _executeBody_getGlobalOrAutoDependencies(execData, targetAbsPath);
        }

        // Evaluate the extra dependency item(s)
        if(xres == XCom.ExecuteResult.Done) {
            if(_extraDepsManual != null) xres = _executeBody_getExtraDependencies(execData);
        }

        // Combine the dependencies
        final boolean extraDepIsMoreRecent = (xres == XCom.ExecuteResult.Done)
                                           ? _executeBody_combineDependencies(execData, targetAbsPath, targetAbsPathIsValid)
                                           : false;

        // Execute the prerequisite(s)
        int preqExecCount = 0;

        if(xres == XCom.ExecuteResult.Done) {
            final EPRes epRes = _executeBody_execPrerequisites(execData, targetAbsPath, targetAbsPathIsValid);
            xres          = epRes.xres;
            preqExecCount = epRes.preqExecCount;
        }

        // Execute the body
        boolean bodyExecuted = false;

        if(xres == XCom.ExecuteResult.Done) {
            final EBRes ebRes = _executeBody_execBody(execData, blockIndex, targetAbsPathIsValid, preqExecCount, extraDepIsMoreRecent);
            xres         = ebRes.xres;
            bodyExecuted = ebRes.bodyExecuted;
        }

        // Determine the execution result
        if(xres == XCom.ExecuteResult.Done || xres == XCom.ExecuteResult.Done_NoUpdate) {
            if(bodyExecuted) {
                xres = XCom.ExecuteResult.Done;
            }
            else {
                if(targetAbsPathIsValid) {
                    xres = (preqExecCount > 0 || extraDepIsMoreRecent) ? XCom.ExecuteResult.Done : XCom.ExecuteResult.Done_NoUpdate;
                }
                else {
                    xres = XCom.ExecuteResult.Done_NoUpdate;
                }
            }
        }

        // Pop the evaluated target name
        popTargetNameEvaled(execData);

        // Clear the evaluated target name at exit
        _targetNameEvaled = null;

        // Return the execution result
        return xres;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        if( !XCacheHelper.saveIfNull(dos, _targetRVS      ) ) XSaver.saveReadVarSpec (dos, _targetRVS      );
        if( !XCacheHelper.saveIfNull(dos, _preqNames      ) ) XSaver.saveReadVarSpecs(dos, _preqNames      );
        if( !XCacheHelper.saveIfNull(dos, _extraDepsManual) ) XSaver.saveReadVarSpecs(dos, _extraDepsManual);

        XSaver.saveLabelMap(dos, _labelMap);
    }

    public static Target loadFromStream(final DataInputStream dis) throws Exception
    {
        final ContainerBlock.LoadContainerData loadCD = loadCDFromStream(dis);

        if(loadCD.blockName == null || loadCD.arrayExecBlocks.length != 1) return null;

        final Target target = new Target(
            loadCD.loadPLC.path,
            loadCD.loadPLC.lNum,
            loadCD.loadPLC.cNum,
            loadCD.blockName,
            XCacheHelper.loadIfNull(dis) ? null : XLoader.loadReadVarSpec (dis),
            XCacheHelper.loadIfNull(dis) ? null : XLoader.loadReadVarSpecs(dis),
            false
        );

        target.addExtraDeps( XCacheHelper.loadIfNull(dis) ? null : XLoader.loadReadVarSpecs(dis) );

        target._labelMap = XLoader.loadLabelMap(dis);

        target.setExecBlocks( 0, loadCD.arrayExecBlocks[0] );

        return target;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Target(final Target refTarget)
    {
        super( refTarget.getPath(), refTarget.getLNum(), refTarget.getCNum(), refTarget.getBlockName(), -1 );

        _targetRVS             = refTarget._targetRVS;

        _targetGlobRegex       = refTarget._targetGlobRegex;
        _targetGlobMatcher     = refTarget._targetGlobMatcher;

        _targetNameEvaled      = refTarget._targetNameEvaled;
        _targetNameEvaledStack = XCom.deepClone_StringStack(refTarget._targetNameEvaledStack);

        _preqNames             = refTarget._preqNames;
        _preqNamesEvaled       = new ArrayList<>();
        _preqNamesMoreUpToDate = new ArrayList<>();

        _extraDepsAutoDetect   = new ArrayList<>();
        _extraDepsManual       = refTarget._extraDepsManual;
        _extraDepsManualEvaled = new ArrayList<>();
        _extraDepsEffective    = new ArrayList<>();

        _combinedPreqList      = new ArrayList<>();

        _sourceFilePaths       = new ArrayList<>();
        _absCIncludePaths      = new ArrayList<>();
        _absJavaClassPaths     = new ArrayList<>();

        _labelMap              = refTarget._labelMap;

        setExecBlocks( 0, refTarget.getExecBlocks(0).deepClone() );
    }

    @Override
    public ExecBlock deepClone()
    {
        // This class is not thread-safe; therefore, return a clone of this instance
        return new Target(this);
    }

} // class Target
