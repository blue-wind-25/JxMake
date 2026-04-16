/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;
import java.util.Map;

import java.util.regex.Pattern;

import jxm.xb.*;


public class XBBuilder {

    private static enum State {
        ClearBegin,      // Begin processing a new statement with    clearing '_trTokens' first
        Begin,           // Begin processing a new statement without clearing '_trTokens' first
        BeginEOL_OLI,    // Consume an EOL and go to 'State.ClearBegin' (for expressions that can be used together with a one-line if statement)
        BeginEOL,        // Consume an EOL and go to 'State.ClearBegin' (for other expressions)

        Option_RHS,      // Get the right hand side of an option

        MacroDef_Name,   // Get the macro name
        MacroDef_Body,   // Get the macro body
        MacroDef_Use,    // Use the macro definition
        MacroUnDef_Name, // Get the macro name to be undefined

        FuncDef_Name,    // Get the function name         of a function definition
        FuncDef_Pars,    // Get the function parameter(s) of a function definition
        FuncDef_Body,    // Create a 'Function' instance, store it, push the '_xbStack', and continue parsing the function body using the other available 'State.*'

        Return_RHS,      // Process the right hand side of a function return statement

        TargetDef_Name,  // Get the target name (not a function call)
        TargetDef_NameF, // Get the target name (    a function call)
        TargetDef_Preqs, // Get the target prerequisite(s)
        TargetDef_EDeps, // Get the target extra dependency item(s)

        ALO_Args,        // Process the arguments of an arithmetic and logic operation
        Eval_RHS,        // Process the right hand side of an eval statement

        VarAssign_AT,    // Get the assignment type     of a variable assigment statement
        VarAssign_RHS,   // Process the right hand side of a variable assigment statement

        VarUnset,        // Get the variable name of an unset     statement
        VarDeprecate,    // Get the variable name of a  deprecate statement

        FuncCall_Args,   // Process the arguments of a function call statement

        Label_RHS,       // Process the right hand side of a label      statement
        GoTo_RHS,        // Process the right hand side of a      go-to statement
        SGoTo_RHS,       // Process the right hand side of a safe-go-to statement

        If_RHS,          // Process the right hand side of an   if  statement and continue parsing the block body using the other available 'State.*'
        Elif_RHS,        // Process the right hand side of an elif  statement and continue parsing the block body using the other available 'State.*'
        Else_RHS,        // Process the right hand side of an else  statement and continue parsing the block body using the other available 'State.*'

        While_RHS,       // Process the right hand side of a while        statement and continue parsing the block body using the other available 'State.*'
        RepeatUntil_RHS, // Process the right hand side of a repeat-until statement after        parsing the block body using the other available 'State.*'
        DoWhilst_RHS,    // Process the right hand side of a do-whilst    statement after        parsing the block body using the other available 'State.*'
        For_RHS,         // Process the right hand side of a for          statement and continue parsing the block body using the other available 'State.*'
        ForEach_RHS,     // Process the right hand side of a for-each     statement and continue parsing the block body using the other available 'State.*'

        Command_RHS,     // Process the right hand side of a shell/operating system commands statement

        JxMake_RHS,      // Process the right hand side of a  jxmake statement
        PJxMake_RHS,     // Process the right hand side of a +jxmake statement

        DepLoad_RHS,     // Process the right hand side of a  depload statement
        SDepLoad_RHS     // Process the right hand side of a sdepload statement
    }

    private State             _state        = State.Begin;
    private int               _cntMLComment = 0;

    private XCom.FuncSpec     _funcSpec     = null;
    private XCom.ALOperSpec   _alOperSpec   = null;
    private byte              _shellCmd_SOF = 0;

    private boolean           _inOneLineIf  = false;
    private int               _transVarCnt  = 0;
    private int               _tmpVarCnt    = 0;

    private TokenReader.Token _curMDefToken = null;

    private TokenReader.Token _curFDefToken = null;
    private boolean           _curFDefSuper = false;
    private String            _curFDefDepre = null;
    private boolean           _curFDefComma = false;
    private boolean           _curFOptParam = true;

    private TokenReader.Token _curTDefToken = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private       String                       _mainJXMSpecFile_absPath = null;

    private                 TokenReader.Token  _lastToken               = null;
    private                 TokenReader.Token  _errorToken              = null;
    private final ArrayList<TokenReader.Token> _trTokens                = new ArrayList<>();

    private final XBStack                      _xbStack                 = new XBStack    ();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final SpecReader.MacroDefs         _macroDefs   = new SpecReader.MacroDefs();
    private       ArrayList<TokenReader.Token> _macroTokens = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XBBuilder(final String mainJXMSpecFile_absPath)
    { _mainJXMSpecFile_absPath = mainJXMSpecFile_absPath; }

    public String mainJXMSpecFile_absPath()
    { return _mainJXMSpecFile_absPath; }

    public boolean error()
    { return _errorToken != null; }

    public TokenReader.Token errorToken()
    { return _errorToken; }

    public SpecReader.MacroDefs macroDefs()
    { return _macroDefs; }

    public void addMacroDefs(final SpecReader.MacroDefs macroDefs) throws JXMRuntimeError
    {
        if(macroDefs == null) return;

        for( final Map.Entry< String, ArrayList<TokenReader.Token> > entry : macroDefs.entrySet() ) {

            // Check if a macro with the same name already exists
            final ArrayList<TokenReader.Token> chkTokens = _macroDefs.get( entry.getKey() );

            if(chkTokens != null) {
                // If it already exist check if it has the same body contents
                final ArrayList<TokenReader.Token> newTokens = entry.getValue();
                if( chkTokens.size() != newTokens.size() ) {
                    throw XCom.newJXMRuntimeError( Texts.EMsg_MacroDefExist, newTokens.get(0).path, newTokens.get(0).lNum, newTokens.get(0).cNum );
                }
                for( int i = 0; i < chkTokens.size(); ++i ) {
                    if( !chkTokens.get(i).equals( newTokens.get(i) ) ) {
                        throw XCom.newJXMRuntimeError( Texts.EMsg_MacroDefExist, newTokens.get(0).path, newTokens.get(0).lNum, newTokens.get(0).cNum );
                    }
                }
                // It has the same body contents, ignore it
                return;
            }

            // Store the macro definition
            _macroDefs.put( entry.getKey(), entry.getValue() );

        } // for
    }

    public XCom.ExecBlocks execBlocks()
    { return _xbStack.rootExecBlocks(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean inMLComment()
    { return _cntMLComment != 0; }

    public boolean inOneLineIf()
    { return _inOneLineIf; }

    public boolean done()
    {
        if(_errorToken != null) return false;

        if( inMLComment() ) return _setError(_lastToken, Texts.EMsg_PrematureEOF);

        if( !_xbStack.inAnyBlock() ) return true;

        return _setError(_lastToken, Texts.EMsg_PrematureEOF);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _setError(final TokenReader.Token token)
    {
        _errorToken = token;

        return false;
    }

    protected boolean _setError(final TokenReader.Token token, final String errMsg, final Object... args)
    {
        _errorToken      = token;
        _errorToken.eStr = XCom.errorString(errMsg, args);

        return false;
    }

    protected boolean _setError(final TokenReader.Token token, final TokenReader.Token tokenPLCRef, final String errMsg, final Object... args)
    {
        _errorToken      = token;
        _errorToken.path = tokenPLCRef.path;
        _errorToken.lNum = tokenPLCRef.lNum;
        _errorToken.cNum = tokenPLCRef.cNum;
        _errorToken.eStr = XCom.errorString(errMsg, args);

        return false;
    }

    protected boolean _setErrorExt(final TokenReader.Token token, final String errMsg1, final String errMsg2, final Object... args)
    {
        _errorToken      = token;
        _errorToken.eStr = XCom.errorString( token.tStr.isEmpty() ? errMsg1 : errMsg2, args );

        return false;
    }

    protected boolean _setErrorExt(final TokenReader.Token token, final TokenReader.Token tokenPLCRef, final String errMsg1, final String errMsg2, final Object... args)
    {
        _errorToken      = token;
        _errorToken.path = tokenPLCRef.path;
        _errorToken.lNum = tokenPLCRef.lNum;
        _errorToken.cNum = tokenPLCRef.cNum;
        _errorToken.eStr = XCom.errorString( token.tStr.isEmpty() ? errMsg1 : errMsg2, args );

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // This hash should prevent clashes of temporary variable names when loading compiled library files
    private String _specFilePathHash = null;

    /*
    // This stored last-processed-token should prevent clashes of temporary variable names when compiling
    // library files from sources with combined lines
    private TokenReader.Token _lastProcessedToken = null;
    //*/

    private String _getTransientVarName()
  //{ return XCom.RVar_NamePrefix + String.valueOf(_transVarCnt++); }
    { return XCom.RVar_NamePrefix + _specFilePathHash + ':' + String.valueOf(_transVarCnt++); }

    private String _getTemporaryVarName()
    { return XCom.TVar_NamePrefix + String.valueOf(_tmpVarCnt++); }

    private void _resetTemporaryVarNameCounter(final TokenReader.Token token)
    {
        /*
        final TokenReader.Token prevToken = _lastProcessedToken;
        _lastProcessedToken = token;

        if(prevToken != null) {
            if( prevToken.path.equals(token.path) && prevToken.lNum == token.lNum ) {
                if( token.path.equals(SysUtil.getUHD() + "/Projects/JxMake/src/0-JxMake/lib/C++XBuildTool_RISCVGCC.jxm") ) {
                    SysUtil.stdDbg().printf("### %03d %s\n    %03d %s\n    %d\n\n", prevToken.lNum, prevToken.path, token.lNum, token.path, _tmpVarCnt);
                }
                return;
            }
        }
        else {
            return;
        }

        _tmpVarCnt          = 0;
        _lastProcessedToken = null;
        //*/

        _tmpVarCnt = 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _processToken_errorIfPrevTokenIsKeywordConst(final TokenReader.Token token)
    {
        if( !_trTokens.isEmpty() && _trTokens.get( _trTokens.size() - 1 ).tStr.equals("const") ) {
            return _setError(token, Texts.EMsg_UnexpectedTokenAfrConst, token.tStr);
        }

        return true;
    }

    private boolean _processToken_errorIfPrevTokenIsKeywordLocalOrConst(final TokenReader.Token token)
    {
        if( !_trTokens.isEmpty() ) {
            final String str = _trTokens.get( _trTokens.size() - 1 ).tStr;
            if( str.equals("local") ) return _setError(token, Texts.EMsg_UnexpectedTokenAfrLocal, token.tStr);
            if( str.equals("const") ) return _setError(token, Texts.EMsg_UnexpectedTokenAfrConst, token.tStr);
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final XCom.FuncSpec _fsGCWD = XCom.getFuncSpec("$cwd"       );
    private static final XCom.FuncSpec _fsGPTD = XCom.getFuncSpec("$ptd"       );
    private static final XCom.FuncSpec _fsPCat = XCom.getFuncSpec("$cat_path"  );
    private static final XCom.FuncSpec _fsPRel = XCom.getFuncSpec("$rel_path"  );
    private static final XCom.FuncSpec _fsPSep = XCom.getFuncSpec("$path_ndsep");

    protected static XCom.ReadVarSpec _getRVarSpecFromToken(final XBBuilder xbb, final TokenReader.Token token)
    {
        // Function call is not allowed here
        if( XCom.isFunctionName(token.tStr) ) {
            xbb._setError(token, Texts.EMsg_FCallNotAllowed);
            return null;
        }

        // Compile the regular expression if it exists
        Pattern regexp      = null;
        String  replacement = null;

        if( !token.tRX1.isEmpty() ) {
            try {
                regexp      = Pattern.compile(token.tRX1);
                replacement =                 token.tRX2 ;
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Set the error message and token
                xbb._setError(token, Texts.EMsg_InvalidRegExp);
                return null;
            }
        }

        // Get the special-variable or special-variable-shortcut specification
        final XCom.SVarSpec svarSpec = XCom.getSVarSpec(token.tStr);
        final boolean       svarEval = (svarSpec != null) ? svarSpec.isEvaluated() : false;

        if( svarSpec == null && token.isSVarSpec() ) {
            xbb._setError(token, Texts.EMsg_InvalidSVarName, token.tStr);
            return null;
        }

        // Check if the variable needs to be evaluated (probably recursively)
        final boolean evaluated = !token.fPSP && ( svarEval || token.isRVarSpec() );

        // Create and store the variable specification
        if(token.fPSP || svarSpec == null) {
            // Check if the token specifies a null argument
            final boolean isNullArg = !token.fPSP && token.tStr.equals("?");
            // This is not a special-variable or a special-variable-shortcut
            return isNullArg ? new XCom.ReadVarSpec(true      , null, XCom.Str_NullArgument, null  , null       )  // It is a null argument
                             : new XCom.ReadVarSpec(!evaluated, null, token.tStr           , regexp, replacement); // Others
        }
        else {
            if(svarSpec.constVal == null) {
                // This is a non-constant special-variable or special-variable-shortcut
                return new XCom.ReadVarSpec(!evaluated, svarSpec, null, regexp, replacement);
            }
            else {
                if(svarSpec.svarName == XCom.SVarName.preqV) {
                    // The non-constant special variable '$[preq:V]' uses the 'SVarSpec.constVal' field to store the index variable name
                    return new XCom.ReadVarSpec(!evaluated, svarSpec, svarSpec.constVal, regexp, replacement);
                }
                else {
                    // This is a constant special-variable or special-variable-shortcut
                    // NOTE : If the constant is not a compile-time constant, pass the 'SVarSpec' instance so that the new 'ReadVarSpec'
                    //        instance can be saved and loaded to/from a binary file properly.
                    return new XCom.ReadVarSpec(
                        !evaluated, !svarSpec.svarName.isCompileTimeConstant() ? svarSpec : null, svarSpec.constVal, regexp, replacement
                    );
                }
            }
        }
    }

    private boolean _storeRVarSpec(final XCom.ReadVarSpecs rvarSpecs, final TokenReader.Token token)
    {
        // Create and check the variable specification
        final XCom.ReadVarSpec rvs = _getRVarSpecFromToken(this, token);

        if(rvs == null) return false;

        /*
         * Check if there is an attached function call shortcut in this token
         *     ~${<var_name>} : expand to '$path_ndsep( $cat_path( $cwd(), ${<var_name>} ) )'
         *     ^${<var_name>} : expand to '$path_ndsep( $cat_path( $ptd(), ${<var_name>} ) )'
         *     -${<var_name>} : expand to '$path_ndsep( $rel_path(         ${<var_name>} ) )'
        */
        if(token.cFCS != 0) {
            // Create a <readVar_token> to read the temporary variable '<tmpVar_Name>'
            final String            tmpVar_Name   = _getTemporaryVarName();
            final TokenReader.Token readVar_token = new TokenReader.Token( token.path, token.lNum, token.cNum, XCom.genRVarName(tmpVar_Name), "", "", null, false );
            // Create an execution block for '<tmpVar_Name> := $cwd/ptd(...)' as needed
            final XCom.FuncSpec fsFDir = (token.cFCS == '~') ? _fsGCWD
                                       : (token.cFCS == '^') ? _fsGPTD
                                       :                       null;
            if(fsFDir != null) {
                /*
                SysUtil.stdDbg().printf( "### %s := %s\n", tmpVar_Name, '$' + fsFDir.fnName.toString() + "()" );
                //*/
                _xbStack.activeExecBlocks().add( new VarAssign(
                    token.path, token.lNum, token.cNum, tmpVar_Name, _xbStack.inFunctionTargetBlock(), false, XCom.ASNSpec_direct, fsFDir, XCom.RVSpcs_EmptyList
                ) );
            }
            // Create an execution block for '<tmpVar_Name> := $cat_path( ${<tmpVar_Name>}, <token> )' as needed
            if(token.cFCS != '-') {
                /*
                SysUtil.stdDbg().printf( "### %s := $cat_path(%s, %s)\n", tmpVar_Name, readVar_token.tStr, token.tStr );
                //*/
                final XCom.ReadVarSpecs irvSpecs = new XCom.ReadVarSpecs();
                if( !_storeRVarSpec(irvSpecs, readVar_token) ) return false;
                irvSpecs.add(rvs);
                _xbStack.activeExecBlocks().add( new VarAssign(
                    token.path, token.lNum, token.cNum, tmpVar_Name, _xbStack.inFunctionTargetBlock(), false, XCom.ASNSpec_direct, _fsPCat, irvSpecs
                ) );
            }
            // Create an execution block for '<tmpVar_Name> := $rel_path( <token> )' as needed
            else { // token.cFCS == '-'
                /*
                SysUtil.stdDbg().printf( "### %s := $rel_path(%s)\n", tmpVar_Name, token.tStr );
                //*/
                final XCom.ReadVarSpecs irvSpecs = new XCom.ReadVarSpecs();
                irvSpecs.add(rvs);
                _xbStack.activeExecBlocks().add( new VarAssign(
                    token.path, token.lNum, token.cNum, tmpVar_Name, _xbStack.inFunctionTargetBlock(), false, XCom.ASNSpec_direct, _fsPRel, irvSpecs
                ) );
            }
            // Create an execution block for '<tmpVar_Name> := $path_ndsep( ${<tmpVar_Name>} )'
            if(true) {
                /*
                SysUtil.stdDbg().printf( "### %s := $path_ndsep(%s)\n", tmpVar_Name, readVar_token.tStr );
                //*/
                final XCom.ReadVarSpecs irvSpecs = new XCom.ReadVarSpecs();
                if( !_storeRVarSpec(irvSpecs, readVar_token) ) return false;
                _xbStack.activeExecBlocks().add( new VarAssign(
                    token.path, token.lNum, token.cNum, tmpVar_Name, _xbStack.inFunctionTargetBlock(), false, XCom.ASNSpec_direct, _fsPSep, irvSpecs
                ) );
            }
            // Create and store a variable specification to read the transient variable
            if( !_storeRVarSpec(rvarSpecs, readVar_token) ) return false;
        }

        // There is no attached function call shortcut, store the variable specification as is
        else {
            rvarSpecs.add(rvs);
        }

        // Done
        return true;
    }

    private boolean _gatherAndStoreSetCreationOperator(final int begIdx, final int endIdx, final XCom.ReadVarSpecs rvarSpecs, final boolean useTransientVariable)
    {
        // Get the temporary/transient variable name
        final String ttxVarName = useTransientVariable ? _getTransientVarName() : _getTemporaryVarName();

        // Gather the right hand side of the statement
        final XCom.ReadVarSpecs rvs = new XCom.ReadVarSpecs();
        for(int i = begIdx; i < endIdx; ++i) {
            if( !_storeRVarSpec( rvs, _trTokens.get(i) ) ) return false;
        }

        // Create an execution block for '<ttx_var> := ...'
        final TokenReader.Token nt = _trTokens.get(begIdx);
        _xbStack.activeExecBlocks().add( new VarAssign(
            nt.path, nt.lNum, nt.cNum, ttxVarName, _xbStack.inFunctionTargetBlock(), false, XCom.ASNSpec_direct, null, rvs
        ) );

        // Create and store a token to read the transient/temporary variable
        if( !_storeRVarSpec( rvarSpecs, _genTokenReadTtxVar(nt, ttxVarName) ) ) return false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _splitAndStoreDQString(final TokenReader.Token token)
    {
        // Split the double-quoted string into parts and check for error
        final ArrayList<TokenReader.Token> tokens = TokenReader.splitDQString(token);

        if(tokens == null) return _setError(token, Texts.EMsg_SyntaxError);

        // Determine which array to be used for storing the parts
        final boolean                      dqp_cf   = (token.mDQP != TokenReader.Token.DQPMode.None);
        final ArrayList<TokenReader.Token> trTokens = !dqp_cf ? _trTokens : ( new ArrayList<>() );

        // Store the parts
        for(final TokenReader.Token t : tokens) {
            // Check for error state in the token
            if( t.isError() ) return _setError(t);
            // Store the token
            trTokens.add( TokenReader.unquoteSQString(t) );
        }

        // Process ."..." and :"..."
        if(dqp_cf) {
            // Get the transient variable name
            final String transVarName = _getTransientVarName();
            // Create a token to read the transient variable
            final TokenReader.Token readTransVar = new TokenReader.Token( token.path, token.lNum, token.cNum, XCom.genRVarName(transVarName), "", "", null, false );
            // Create an execution block for '<trans_var> = ...'
            if(true /* TokenReader.Token.DQPMode.Combine || TokenReader.Token.DQPMode.Flatten */) {
                // Gather the right hand side of the assigment
                final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                for(int i = 0; i < trTokens.size(); ++i) {
                    if( !_storeRVarSpec( rvarSpecs, trTokens.get(i) ) ) return false;
                }
                // Create and store a new execution block
                _xbStack.activeExecBlocks().add( new VarAssign(
                    token.path, token.lNum, token.cNum, transVarName, false, false, XCom.ASNSpec_lazy, null, rvarSpecs
                ) );
            }
            // Create an execution block for '<trans_var> := $flatten(<trans_var>)'
            if(token.mDQP == TokenReader.Token.DQPMode.Flatten) {
                // Gather the right hand side of the assigment
                final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                if( !_storeRVarSpec(rvarSpecs, readTransVar) ) return false;
                // Create and store a new execution block
                _xbStack.activeExecBlocks().add( new VarAssign(
                    token.path, token.lNum, token.cNum, transVarName, false, false, XCom.ASNSpec_direct, XCom.getFuncSpec("$flatten"), rvarSpecs
                ) );
            }
            // Store the token
            _trTokens.add(readTransVar);
        }

        // Done
        return true;
    }

    private boolean _genericStoreToken(final TokenReader.Token token)
    {
        // Check for double-quoted string
        if( token.isDQString() ) {
            if( !_splitAndStoreDQString(token) ) return false;
        }

        // Store the token as is
        else {
            _trTokens.add( TokenReader.unquoteSQString(token) );
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private TokenReader.Token _genTokenReadTtxVar(final EvalSFCResult esfcr)
    { return new TokenReader.Token(esfcr.rToken.path, esfcr.rToken.lNum, esfcr.rToken.cNum, esfcr.ttxVarName, "", "", null, false); }

    private TokenReader.Token _genTokenReadTtxVar(final TokenReader.Token refToken, final String ttxVarName)
    { return new TokenReader.Token(refToken.path, refToken.lNum, refToken.cNum, XCom.genRVarName(ttxVarName), "", "", null, false); }

    private boolean _chkVarName_storeXB_localDecl(final TokenReader.Token token, final String varName, final TokenReader.Token varNameToken)
    {
        // Error if the variable name is not a valid symbol name or the variable name is a keyword
        if( !XCom.isSymbolName(varName) ) return _setError(varNameToken, Texts.EMsg_SyntaxError             );
        if(  XCom.isKeyword   (varName) ) return _setError(varNameToken, Texts.EMsg_UnexpectedToken, varName);

        // Create and store a new execution block
        _xbStack.activeExecBlocks().add( new VarAssign(
            token.path, token.lNum, token.cNum, varName, true, false, XCom.ASNSpec_lazy_ifNotSet, null, XCom.RVSpcs_Empty
        ) );

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class EvalSFCResult {
        public final String            ttxVarName; // Name of the temporary or transient variable
        public final XCom.IntegerRef   lIdx;
        public final XCom.IntegerRef   rIdx;
        public final TokenReader.Token rToken;

        public EvalSFCResult(final String ttxVarName_, final XCom.IntegerRef lIdx_, final XCom.IntegerRef rIdx_, final TokenReader.Token rToken_)
        {
            ttxVarName = ttxVarName_;
            lIdx       = lIdx_;
            rIdx       = rIdx_;
            rToken     = rToken_;
        }
    }

    private boolean _checkFuncCallParentheses(final XCom.IntegerRef begIdx_, final XCom.IntegerRef endIdx_)
    {
        // Get the indexes
        final int begIdx = begIdx_.get();
        final int endIdx = endIdx_.get();

        // Check the number of tokens
        if( begIdx + 1 >= endIdx ) return _setError( _trTokens.get( Math.min( _trTokens.size() - 1, endIdx + 1 ) ), Texts.EMsg_PrematureEOL );

        // Check if the next token is the opening '('
        int pcnt = _trTokens.get(begIdx).tStr.equals("(") ? 1 : 0;
        if(pcnt == 0) return _setError( _trTokens.get(begIdx), Texts.EMsg_SyntaxError );

        // Find the index of the closing ')'
        int lIdx = begIdx;
        int rIdx = -1;
        for(int j = begIdx + 1; j < endIdx; ++j) {

            final TokenReader.Token nt = _trTokens.get(j);

                 if( !nt.fPSP && nt.tStr.equals("(") ) ++pcnt;
            else if( !nt.fPSP && nt.tStr.equals(")") ) --pcnt;

            if(pcnt == 0) {
                rIdx = j;
                break;
            }

        } // for

        if(rIdx < lIdx) return _setError( _trTokens.get(endIdx - 1), Texts.EMsg_UnbalancedLRParentheses );

        // Update the values
        begIdx_.set(lIdx);
        endIdx_.set(rIdx);

        // Done
        return true;
    }

    private boolean _gatherAndStoreFuncCallArgs(final int begIdx, final int endIdx, final XCom.FuncSpec funcSpec, final XCom.ReadVarSpecs rvarSpecs)
    {
        // Gather the arguments
        boolean expectComma = false;

        for(int i = begIdx; i < endIdx; ++i) {
            // Get and check the token
            final TokenReader.Token t = _trTokens.get(i);
            if(expectComma) {
                // Error if the token is not ','
                if( !t.tStr.equals(",") ) return _setError(t, Texts.EMsg_UnexpectedToken, t.tStr);
                // Reset flag
                expectComma = false;
            }
            else {
                // Error if the token is ',' (not a string ',')
                if( !t.fPSP && t.tStr.equals(",") ) return _setError(t, Texts.EMsg_UnexpectedToken, t.tStr);
                // Determine if it is a set creation operator
                int brcIdx = -1;
                if( t.tStr.equals("{") ) {
                    // Find the closing '}'
                    for(int j = i + 1; j < endIdx; ++j) {
                        if( _trTokens.get(j).tStr.equals("}") ) {
                            brcIdx = j;
                            break;
                        }
                    }
                    if(brcIdx <= i) return _setError(t, Texts.EMsg_UnclosedSetCreateOper);
                }
                // Check if it is a set creation operator
                if(brcIdx > 0) {
                    // The set creation operator cannot be used to specify the user-defined function name
                    if( (i == begIdx) && ( (funcSpec.fnName == XCom.FuncName.call) || (funcSpec.fnName == XCom.FuncName.exec) ) ) {
                        return _setError(t, Texts.EMsg_IllegalSetCreateOperUse, t.tStr);
                    }
                    // Evaluate the set creation operator
                    if( !_gatherAndStoreSetCreationOperator(i + 1, brcIdx, rvarSpecs, false) ) return false;
                    // Adjust the index
                    i = brcIdx;
                }
                // Check if it is a function call
                else if( XCom.isFunctionName(t.tStr) ) {
                    // Evaluate the function call
                    final EvalSFCResult esfcr = _evalSubFuncCall(t, i + 1, endIdx);
                    if(esfcr == null) return false;
                    // Create and store a token to read the temporary variable
                    if( !_storeRVarSpec( rvarSpecs, _genTokenReadTtxVar(esfcr) ) ) return false;
                    // Adjust the index
                    i = esfcr.rIdx.get();
                }
                // It is not a function call
                else {
                    // Store the token
                    if( !_storeRVarSpec( rvarSpecs, _trTokens.get(i) ) ) return false;
                }
                // Set flag
                expectComma = true;
            }
        }

        if( !expectComma && rvarSpecs.size() != 0 ) {
            final TokenReader.Token t = _trTokens.get(endIdx - 1);
            return _setError(t, Texts.EMsg_UnexpectedToken, t.tStr);
        }

        // Check the number of arguments
        int argCnt = rvarSpecs.size();

        argCnt -= funcSpec.reqCnt;
        if(argCnt < 0) return _setError( _trTokens.get(endIdx - 1) , Texts.EMsg_TooFewNumArg, funcSpec.functionName(), rvarSpecs.size(), funcSpec.reqCnt, funcSpec.optCnt );

        if(funcSpec.optCnt >= 0) {
            argCnt -= funcSpec.optCnt;
            if(argCnt > 0) return _setError( _trTokens.get(endIdx - 1) , Texts.EMsg_TooManyNumArg, funcSpec.functionName(), rvarSpecs.size(), funcSpec.reqCnt, funcSpec.optCnt );
        }

        // Done
        return true;
    }

    private EvalSFCResult _evalSubFuncCall(final TokenReader.Token fToken, final int begIdx, final int endIdx, final boolean useTransientVariable)
    {
        // Get the function specification
        final XCom.FuncSpec funcSpec = XCom.getFuncSpec(fToken.tStr);

        if(funcSpec == null) {
            _setError( fToken, Texts.EMsg_InvalidFuncName, XCom.normalizeFunctionName(fToken.tStr) );
            return null;
        }

        if(!funcSpec.retVal) {
            _setError( fToken, Texts.EMsg_SubFuncAsArgNoRetVal, XCom.normalizeFunctionName(fToken.tStr) );
            return null;
        }

        // Check the '(' and ')'
        final XCom.IntegerRef lIdx = new XCom.IntegerRef(begIdx);
        final XCom.IntegerRef rIdx = new XCom.IntegerRef(endIdx);
        if( !_checkFuncCallParentheses(lIdx, rIdx) ) return null;

        // Gather the function call arguments
        final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
        if( !_gatherAndStoreFuncCallArgs( lIdx.get() + 1, rIdx.get(), funcSpec, rvarSpecs ) ) return null;

        // Get the transient/temporary variable name
        final String ttxVarName = useTransientVariable ? _getTransientVarName() : _getTemporaryVarName();

        // Create an execution block for '<ttx_var> := $func(...)'
        final TokenReader.Token nt = _trTokens.get( rIdx.get() );
        _xbStack.activeExecBlocks().add( new VarAssign(
            nt.path, nt.lNum, nt.cNum, ttxVarName, _xbStack.inFunctionTargetBlock(), false, XCom.ASNSpec_direct, funcSpec, rvarSpecs
        ) );

        // Done
        return new EvalSFCResult( XCom.genRVarName(ttxVarName), lIdx, rIdx, nt );
    }

    private EvalSFCResult _evalSubFuncCall(final TokenReader.Token fToken, final int begIdx, final int endIdx)
    { return _evalSubFuncCall(fToken, begIdx, endIdx, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private TokenReader.Token _processFunCallToTtxVar_generic(final TokenReader.Token token, final String errMsg, final boolean pushElseBlockEarly, final boolean condValCanEmpty, final boolean useTransientVariable)
    {
        // Check if the token is a function call
        if( XCom.isFunctionName(token.tStr) ) {
            // Push the 'else' block early as needed
            if(pushElseBlockEarly) _xbStack.addElseBlockAndPush();
            // If it is a function call, evaluate the function call
            final EvalSFCResult esfcr = _evalSubFuncCall( token, 0, _trTokens.size(), useTransientVariable );
            if(esfcr == null) return null;
            // Remove the consumed tokens
            _trTokens.subList( 0, esfcr.rIdx.get() + 1 ).clear();
            // Create and return a token to read the temporary variable
            return _genTokenReadTtxVar(esfcr);
        }
        // The token is not a function call
        else {
            // Check if the token is valid
            if( !XCom.isValidConditionValue(token, condValCanEmpty) ) {
                _setErrorExt(token, Texts.EMsg_PrematureEOL, errMsg, token.tStr);
                return null;
            }
            // Return back the token
            return token;
        }
    }

    private TokenReader.Token _processFunCallToTtxVar_forCond(final TokenReader.Token token, final boolean pushElseBlockEarly, final boolean condValCanEmpty, final boolean useTransientVariable)
    { return _processFunCallToTtxVar_generic(token, Texts.EMsg_InvalidConditionExpr, pushElseBlockEarly, condValCanEmpty, useTransientVariable); }

    private TokenReader.Token _processFunCallToTtxVar_forCond(final TokenReader.Token token, final boolean useTransientVariable)
    { return _processFunCallToTtxVar_forCond(token, false, false, useTransientVariable); }

    private TokenReader.Token _processFunCallToTtxVar_forTerm(final TokenReader.Token token, final boolean pushElseBlockEarly, final boolean condValCanEmpty, final boolean useTransientVariable)
    { return _processFunCallToTtxVar_generic(token, Texts.EMsg_InvalidTermExpr, pushElseBlockEarly, condValCanEmpty, useTransientVariable); }

    private TokenReader.Token _processFunCallToTtxVar_forTerm(final TokenReader.Token token, final boolean useTransientVariable)
    { return _processFunCallToTtxVar_forTerm(token, false, false, useTransientVariable); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private TokenReader.Token _processFunCallToTmpVar_forCond(final TokenReader.Token token, final boolean pushElseBlockEarly, final boolean condValCanEmpty)
    { return _processFunCallToTtxVar_forCond(token, pushElseBlockEarly, condValCanEmpty, false); }

    private TokenReader.Token _processFunCallToTmpVar_forCond(final TokenReader.Token token)
    { return _processFunCallToTmpVar_forCond(token, false, false); }

    private TokenReader.Token _processFunCallToTmpVar_forTerm(final TokenReader.Token token, final boolean pushElseBlockEarly, final boolean condValCanEmpty)
    { return _processFunCallToTtxVar_forTerm(token, pushElseBlockEarly, condValCanEmpty, false); }

    private TokenReader.Token _processFunCallToTmpVar_forTerm(final TokenReader.Token token)
    { return _processFunCallToTmpVar_forTerm(token, false, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private TokenReader.Token _processFunCallToTransVar_forCond(final TokenReader.Token token, final boolean pushElseBlockEarly, final boolean condValCanEmpty)
    { return _processFunCallToTtxVar_forCond(token, pushElseBlockEarly, condValCanEmpty, true); }

    private TokenReader.Token _processFunCallToTransVar_forCond(final TokenReader.Token token)
    { return _processFunCallToTransVar_forCond(token, false, false); }

    private TokenReader.Token _processFunCallToTransVar_forTerm(final TokenReader.Token token, final boolean pushElseBlockEarly, final boolean condValCanEmpty)
    { return _processFunCallToTtxVar_forTerm(token, pushElseBlockEarly, condValCanEmpty, true); }

    private TokenReader.Token _processFunCallToTransVar_forTerm(final TokenReader.Token token)
    { return _processFunCallToTransVar_forTerm(token, false, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _processRHS_forReadVarSpecs(final int trTokenSize, final XCom.ReadVarSpecs rvarSpecs, boolean enableSetCreationOperator)
    {
        // Check if it is a set creation operator
        final boolean ftIsCBr = ( enableSetCreationOperator && (trTokenSize > 2) && _trTokens.get(0).tStr.equals("{") );
        final boolean isFTSet = ( ftIsCBr                   && _trTokens.get(trTokenSize - 1).tStr.equals("}")        );

        if(ftIsCBr && !isFTSet) return _setError( _trTokens.get (_trTokens.size() - 1 ), Texts.EMsg_UnclosedSetCreateOper );

        // Check if it is a function call
        final boolean isFCall = ( (trTokenSize > 2) && XCom.isFunctionName( _trTokens.get(0).tStr ) );

        // Evaluate the set creation operator
        if(isFTSet) {
            // Evaluate the set creation operator
            if( !_gatherAndStoreSetCreationOperator(1, trTokenSize - 1, rvarSpecs, true) ) return false;
        }

        // Evaluate the function call
        else if(isFCall) {
            // Evaluate the function call
            final EvalSFCResult esfcr = _evalSubFuncCall( _trTokens.get(0), 1, trTokenSize, true );
            if(esfcr == null) return false;
            if( trTokenSize > esfcr.rIdx.get() + 1 ) return _setError( _trTokens.get( esfcr.rIdx.get() + 1 ), Texts.EMsg_FollowFCallNotAllowed );
            // Create and store a token to read the temporary variable
            if( !_storeRVarSpec( rvarSpecs, _genTokenReadTtxVar(esfcr) ) ) return false;
        }

        // Gather the right hand side of the statement
        else {
            for(int i = 0; i < trTokenSize; ++i) {
                if( !_storeRVarSpec( rvarSpecs, _trTokens.get(i) ) ) return false;
            }
        }

        // Done
        return true;
    }

    private boolean _processRHS_forReadVarSpecs(final int trTokenSize, final XCom.ReadVarSpecs rvarSpecs)
    { return _processRHS_forReadVarSpecs(trTokenSize, rvarSpecs, true); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Pattern _pmMacroUse = Pattern.compile("^\\.\\$.+$");

    public void storeLibExecBlocks(final XCom.ExecBlocks execBlocks)
    { for(final ExecBlock xb : execBlocks) _xbStack.activeExecBlocks().add(xb); }

    @SuppressWarnings("fallthrough")
    public boolean processToken(final SpecReader specReader, final TokenReader.Token token) throws JXMException
    {
        // Update the path hash
        _specFilePathHash = specReader.getSpecFilePathHash();

        // Check for error state in the token
        if( token.isError() ) return _setError(token);

        // Save the token as the last token
        _lastToken = token;

        // Handle multiline comment
        if( token.tStr.equals("(*") ) {
            ++_cntMLComment;
            return true;
        }
        if( token.tStr.equals("*)") ) {
            --_cntMLComment;
            if(_cntMLComment < 0) return _setError(token, Texts.EMsg_UnbalancedMLCMarker);
            return true;
        }
        if( inMLComment() ) return true;

        // An indicator flag that indicates if the value the '_inOneLineIf' flag should be checked
        boolean check_inOneLineIf = false;

        // Process according to the current state
        switch(_state) {

            // ===== Begin =====
            case ClearBegin:
                // Clear the tokens
                _trTokens.clear();
                // Reset the temporary variable name counter
                _resetTemporaryVarNameCounter(token);
                // Change state
                _state = State.Begin;
                /* FALLTHROUGH */
            case Begin:
                // Check if it is a keyword
                if( XCom.isKeyword(token.tStr) ) {
                    switch(token.tStr) {
                        case "jxmake":
                            /* FALLTHROUGH */
                        case "+jxmake":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently processing a function definition
                            if( _curFDefToken != null ) return _setError(token, Texts.EMsg_IllegalJxMakeTargetRun);
                            // Error if not currently processing a target definition
                            if( _curTDefToken == null ) return _setError(token, Texts.EMsg_IllegalJxMakeTargetRun);
                            // Change state
                            _state = ( token.tStr.charAt(0) == '+' ) ? State.PJxMake_RHS : State.JxMake_RHS;
                            break;
                        case "jxwait":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently processing a function definition
                            if( _curFDefToken != null ) return _setError(token, Texts.EMsg_IllegalJxWaitTargetRun);
                            // Error if not currently processing a target definition
                            if( _curTDefToken == null ) return _setError(token, Texts.EMsg_IllegalJxWaitTargetRun);
                            // Create and store a new execution block
                            _xbStack.activeExecBlocks().add( new TargetWait(
                                token.path, token.lNum, token.cNum
                            ) );
                            // Change state
                            _state = State.BeginEOL_OLI;
                            break;
                        case "depload":
                            /* FALLTHROUGH */
                        case "sdepload":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently processing a function or a target definition
                            if( _curFDefToken != null ) return _setError(token, Texts.EMsg_IllegalGlobalDepLoad);
                            if( _curTDefToken != null ) return _setError(token, Texts.EMsg_IllegalGlobalDepLoad);
                            // Change state
                            _state = token.tStr.equals("depload") ? State.DepLoad_RHS : State.SDepLoad_RHS;
                            break;
                        case "echo":
                            /* FALLTHROUGH */
                        case "echoln":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Store the variable name and assigment type
                            // NOTE : The system implements 'echo' and 'echoln' statements as assignments to special variables
                            _trTokens.add( TokenReader.newConstStringToken(token, token.tStr.equals("echoln") ? XCom.SVar_Echoln : XCom.SVar_Echo) );
                            _trTokens.add( TokenReader.newConstStringToken(token, ":="                                                           ) );
                            // Change state
                            _state = State.VarAssign_RHS;
                            break;
                        case "local":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently not inside a function or target body
                            if( !_xbStack.inFunctionBlock() && !_xbStack.inTargetBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Store the keyword
                            _trTokens.add(token);
                            break;
                        case "const":
                            // Error if the previous token is the "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordConst(token) ) return false;
                            // Store the keyword
                            _trTokens.add(token);
                            break;
                        case "unset":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Change state
                            _state = State.VarUnset;
                            break;
                        case "deprecate":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Change state
                            _state = State.VarDeprecate;
                            break;
                        case "function":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Error if already processing a function definition
                            if( _curFDefToken != null ) return _setError(token, Texts.EMsg_IllegalFuncDef);
                            // Error if already processing a target definition
                            if( _curTDefToken != null ) return _setError(token, Texts.EMsg_IllegalFuncDef);
                            // Change state
                            _state = State.FuncDef_Name;
                            break;
                        case "endfunction":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if not currently processing a function definition
                            if( _curFDefToken == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Clear the saved function token, the "supersede" flag, and "deprecated" marker
                            _curFDefToken = null;
                            _curFDefSuper = false;
                            _curFDefDepre = null;
                            // Pop the stack
                            if( _xbStack.popFuncDef() == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "return":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently not inside a function body
                            if( !_xbStack.inFunctionBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.Return_RHS;
                            break;
                        case "target":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Error if already processing a function definition
                            if( _curFDefToken != null ) return _setError(token, Texts.EMsg_IllegalTargetDef);
                            // Error if already processing a target definition
                            if( _curTDefToken != null ) return _setError(token, Texts.EMsg_IllegalTargetDef);
                            // Change state
                            _state = State.TargetDef_Name;
                            break;
                        case "endtarget":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if not currently processing a target definition
                            if( _curTDefToken == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Clear the saved target token
                            _curTDefToken = null;
                            // Pop the stack
                            if( _xbStack.popTargetDef() == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "extradep":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if not currently processing a target definition
                            if( _curTDefToken == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Error if no longer can add extra dependencies ('extradep' statements must be located at the
                            // beginning of the target body before any other statements)
                            if( !_xbStack.activeParentTargetBlock().canAddExtraDeps() ) return _setError(token, Texts.EMsg_InvalidExtraDepLoc, token.tStr);
                            // Change state
                            _state = State.TargetDef_EDeps;
                            break;
                        case "label":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Error if currently not inside a function or target body
                            if( !_xbStack.inFunctionBlock() && !_xbStack.inTargetBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.Label_RHS;
                            break;
                        case "goto":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently not inside a function or target body
                            if( !_xbStack.inFunctionBlock() && !_xbStack.inTargetBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.GoTo_RHS;
                            break;
                        case "sgoto":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently not inside a function or target body
                            if( !_xbStack.inFunctionBlock() && !_xbStack.inTargetBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.SGoTo_RHS;
                            break;
                        case "if":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Change state
                            _state = State.If_RHS;
                            break;
                        case "elif":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently not inside an if block or inside an else block
                            if( !_xbStack.inIfBlock() || _xbStack.inElseBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.Elif_RHS;
                            break;
                        case "else":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently not inside an if block or inside an else block
                            if( !_xbStack.inIfBlock() || _xbStack.inElseBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.Else_RHS;
                            break;
                        case "endif":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Pop the stack
                            if( _xbStack.popIfBlock() == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "for":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Change state
                            _state = State.For_RHS;
                            break;
                        case "endfor":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Pop the stack
                            if( _xbStack.popForBlock() == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "foreach":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Change state
                            _state = State.ForEach_RHS;
                            break;
                        case "endforeach":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Pop the stack
                            if( _xbStack.popForEachBlock() == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "while":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Change state
                            _state = State.While_RHS;
                            break;
                        case "endwhile":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Pop the stack
                            if( _xbStack.popWhileBlock() == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "do":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Create and store a new execution block
                            _xbStack.addRepeatBlockAndPush( new Repeat(
                                token.path, token.lNum, token.cNum, true
                            ) );
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "whilst":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Change state
                            _state = State.DoWhilst_RHS;
                            break;
                        case "repeat":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Create and store a new execution block
                            _xbStack.addRepeatBlockAndPush( new Repeat(
                                token.path, token.lNum, token.cNum, false
                            ) );
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "until":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Change state
                            _state = State.RepeatUntil_RHS;
                            break;
                        case "loop":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if a one-line if statement is in progress
                            if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                            // Create and store a new execution block
                            _xbStack.addForeverLoopBlockAndPush( new ForeverLoop(
                                token.path, token.lNum, token.cNum
                            ) );
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "endloop":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Pop the stack
                            if( _xbStack.popForeverLoopBlock() == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Change state
                            _state = State.BeginEOL;
                            break;
                        case "continue":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently not inside a loop block
                            if( !_xbStack.inLoopBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Create and store a new execution block
                            _xbStack.activeExecBlocks().add( new LoopContinue(
                                token.path, token.lNum, token.cNum
                            ) );
                            // Change state
                            _state = State.BeginEOL_OLI;
                            break;
                        case "break":
                            // Error if the previous token is the "local" or "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                            // Error if currently not inside a loop block
                            if( !_xbStack.inLoopBlock() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            // Create and store a new execution block
                            _xbStack.activeExecBlocks().add( new LoopBreak(
                                token.path, token.lNum, token.cNum
                            ) );
                            // Change state
                            _state = State.BeginEOL_OLI;
                            break;
                        case "eval":
                            // Error if the previous token is the "const" keyword
                            if( !_processToken_errorIfPrevTokenIsKeywordConst(token) ) return false;
                            // Change state
                            _state = State.Eval_RHS;
                            break;
                        default:
                            // Check for arithmetic and logic Operations
                            if( ( _alOperSpec = XCom.getALOperSpec(token.tStr) ) != null ) {
                                // Error if the previous token is the "const" keyword
                                if( !_processToken_errorIfPrevTokenIsKeywordConst(token) ) return false;
                                // Change state
                                _state = State.ALO_Args;
                            }
                            // Unexpected keyword
                            else {
                                return _setError(token, Texts.EMsg_UnexpectedKeyword, token.tStr);
                            }
                    } // switch token.tStr
                }
                // Check for function call
                else if( XCom.isFunctionName(token.tStr) ) {
                    // Error if the previous token is the "local" or "const" keyword
                    if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                    // Get the function specification
                    _funcSpec = XCom.getFuncSpec(token.tStr);
                    if(_funcSpec == null) return _setError( token, Texts.EMsg_InvalidFuncName, XCom.normalizeFunctionName(token.tStr) );
                    // Change state
                    _state = State.FuncCall_Args;
                }
                // Check for variable assigment (check if the token is a valid symbol name)
                else if( XCom.isSymbolName(token.tStr, true) ) {
                    // Error if the token is a keyword
                    if( XCom.isKeyword(token.tStr) ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                    // Store the variable name
                    _trTokens.add(token);
                    // Change state
                    _state = State.VarAssign_AT;
                }
                // Other tokens
                else {
                    // Check for shell/operating system commands statement
                    _shellCmd_SOF = 0;
                    switch(token.tStr) {
                        case   "@" : _shellCmd_SOF =        (ShellOper.SOF_Execute                                                           ); break;
                        case  "?@" : _shellCmd_SOF =        (                        ShellOper.SOF_PrintCommand                              ); break;
                        case  "-@" : _shellCmd_SOF = (byte) (ShellOper.SOF_Execute                              | ShellOper.SOF_SuppressError); break;
                        case  "+@" : _shellCmd_SOF = (byte) (ShellOper.SOF_Execute | ShellOper.SOF_PrintCommand                              ); break;
                        case "-+@" : _shellCmd_SOF = (byte) (ShellOper.SOF_Execute | ShellOper.SOF_PrintCommand | ShellOper.SOF_SuppressError); break;
                    }
                    if(_shellCmd_SOF != 0) {
                        // Error if the previous token is the "local" or "const" keyword
                        if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                        // Error if a one-line if statement is in progress
                        if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                        // Error if not currently processing a function definition
                        if( _curFDefToken == null ) return _setError(token, Texts.EMsg_InvalidShellCommandLoc);
                        // Store dummy tokens so that the same state processing code (for 'VarAssign_RHS') can be used
                        _trTokens.add( TokenReader.newConstStringToken(token, ""  ) );
                        _trTokens.add( TokenReader.newConstStringToken(token, ":=") );
                        // Change state
                        _state = State.Command_RHS;
                    }
                    // Check if it is an option definition
                    else if( token.tStr.equals(".option") ) {
                        // Error if there was a previous token
                        if( !_trTokens.isEmpty() ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                        // Change state
                        _state = State.Option_RHS;
                    }
                    // Check if it is a macro definition
                    else if( token.tStr.equals(".macro") ) {
                        // Error if the previous token is the "local" or "const" keyword
                        if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                        // Error if a one-line if statement is in progress
                        if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                        // Change state
                        _state = State.MacroDef_Name;
                    }
                    // Check if it is a macro undefinition
                    else if( token.tStr.equals(".undefmacro") ) {
                        // Error if the previous token is the "local" or "const" keyword
                        if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                        // Error if a one-line if statement is in progress
                        if(_inOneLineIf) return _setError(token, Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                        // Change state
                        _state = State.MacroUnDef_Name;
                    }
                    // Check if it is a macro usage
                    else if( _pmMacroUse.matcher(token.tStr).matches() ) {
                        // Error if the previous token is the "local" or "const" keyword
                        if( !_processToken_errorIfPrevTokenIsKeywordLocalOrConst(token) ) return false;
                        // Get the macro name
                        final String macroName = token.tStr.substring(2); // Discard the '.$'
                        // Get the macro definition
                        _macroTokens = _macroDefs.get(macroName);
                        if(_macroTokens == null) return _setError(token, Texts.EMsg_MacroDefNotExist, token.tStr);
                        // Change state
                        _state = State.MacroDef_Use;
                    }
                    // Got a premature end of line or syntax error
                    else {
                        if( token.isEOL() ) {
                            if( !_trTokens.isEmpty() ) return _setError(token, Texts.EMsg_PrematureEOL);
                        }
                        else {
                            return _setError(token, Texts.EMsg_SyntaxError);
                        }
                    }
                }
                break;
            case BeginEOL_OLI:
                // Set flag
                check_inOneLineIf = true;
                /* FALLTHROUGH */
            case BeginEOL:
                // Error if the token is not EOL
                if( !token.isEOL() ) return _setError(token, Texts.EMsg_UnexpectedExtraToken, token.tStr);
                // Change state
                _state = State.ClearBegin;
                break;

            // ===== Option definition =====
            case Option_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get and check the option type
                    final String      typeStr = TokenReader.strPopFirst(_trTokens);
                          Option.Type type    = null;
                    switch(typeStr) {
                        case "warning" : type = Option.Type.warning; break;
                        default        : return _setError(token, Texts.EMsg_UnexpectedToken, typeStr);
                    }
                    // Get and check the option mode
                    final String      modeStr = TokenReader.strPopFirst(_trTokens);
                          Option.Mode mode    = null;
                    switch(modeStr) {
                        case "push"    : mode = Option.Mode.push   ; break;
                        case "pop"     : mode = Option.Mode.pop    ; break;
                        case "disable" : mode = Option.Mode.disable; break;
                        case "enable"  : mode = Option.Mode.enable ; break;
                        default        : return _setError(token, Texts.EMsg_UnexpectedToken, modeStr);
                    }
                    // Get and check the option specification
                    final String      specStr = mode.hasSpec ? TokenReader.strPopFirst(_trTokens) : null;
                          Option.Spec spec    = Option.Spec.__none__;
                    if(specStr != null) {
                        switch(specStr) {
                            case "inv-ref-var"   : spec = Option.Spec.inv_ref_var  ; break;
                            case "var-not-exist" : spec = Option.Spec.var_not_exist; break;
                            case "cnv-integer"   : spec = Option.Spec.cnv_integer  ; break;
                            case "cnv-boolean"   : spec = Option.Spec.cnv_boolean  ; break;
                            default              : return _setError(token, Texts.EMsg_UnexpectedToken, specStr);
                        }
                    }
                    // Check for extra token
                    if( !_trTokens.isEmpty() ) return _setError( token, Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                    // Create and store a new execution block
                    _xbStack.activeExecBlocks().add( new Option(
                        type, mode, spec
                    ) );
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== Macro definition =====
            case MacroDef_Name:
                /* FALLTHROUGH */
            case MacroUnDef_Name:
                // Get the macro name
                if(_curMDefToken == null) {
                    // Error if the token is not a valid symbol name or the token is a keyword
                    if( !XCom.isSymbolName(token.tStr) ) return _setError(token, Texts.EMsg_SyntaxError                );
                    if(  XCom.isKeyword   (token.tStr) ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                    // Store the macro token
                    _curMDefToken = token;
                }
                // Error if the token is not EOL
                else {
                    // Error if the token is not EOL
                    if( !token.isEOL() ) return _setError(token, Texts.EMsg_UnexpectedExtraToken, token.tStr);
                    // Macro definition
                    if(_state == State.MacroDef_Name) {
                        // Change state
                        _state = State.MacroDef_Body;
                    }
                    // Macro undefinition
                    else {
                        // Remove the macro definition
                        _macroDefs.remove(_curMDefToken.tStr);
                        // Clear the saved macro token
                        _curMDefToken = null;
                        // Change state
                        _state = State.Begin;
                    }
                }
                break;
            case MacroDef_Body:
                if( token.tStr.equals(".macro") ) {
                    return _setError(token, Texts.EMsg_IllegalMacroDef);
                }
                else if( token.tStr.equals(".undefmacro") ) {
                    return _setError(token, Texts.EMsg_IllegalMacroUnDef);
                }
                // Check for '.endmacro'
                else if( token.tStr.equals(".endmacro") ) {
                    // Check if a macro with the same name already exists
                    final ArrayList<TokenReader.Token> chkTokens = _macroDefs.get(_curMDefToken.tStr);
                    if(chkTokens != null) {
                        final boolean           cValid = !chkTokens.isEmpty();
                        final boolean           tValid = !_trTokens.isEmpty();
                        final TokenReader.Token mToken = cValid ? chkTokens.get(0)
                                                       : tValid ? _trTokens.get(0)
                                                       : token;
                        return _setError(_curMDefToken, Texts.EMsg_MacroDefExist, _curMDefToken.tStr, mToken.path, mToken.lNum - 1, mToken.cNum);
                    }
                    // Store the macro tokens
                    final ArrayList<TokenReader.Token> mTokens = new ArrayList<>();
                    _macroDefs.put(_curMDefToken.tStr, mTokens);
                    mTokens.addAll(_trTokens);
                    // Clear the saved macro token
                    _curMDefToken = null;
                    // Clear the tokens
                    _trTokens.clear();
                    // Change state
                    _state = State.BeginEOL;
                }
                // Other tokens
                else {
                    // Using macro from another macro
                    if( token.tStr.startsWith(".$") ) {
                        // Get the macro name
                        final String macroName = token.tStr.substring(2); // Discard the '.$'
                        // Get the macro definition
                        final ArrayList<TokenReader.Token> subMacroTokens = _macroDefs.get(macroName);
                        if(subMacroTokens == null) return _setError(token, Texts.EMsg_MacroDefNotExist, token.tStr);
                        // Check if the submacro tokens also call other macros
                        for(final TokenReader.Token chk : subMacroTokens) {
                            if( chk.tStr.startsWith(".$") ) return _setError(token, Texts.EMsg_IllegalMacroUseInMacro);
                        }
                        // Store the macro tokens
                        _trTokens.addAll(subMacroTokens);
                    }
                    // Store the token as is
                    else {
                        _trTokens.add(token);
                    }
                }
                break;
            case MacroDef_Use:
                // Error if the token is not EOL
                if( !token.isEOL() ) return _setError(token, Texts.EMsg_UnexpectedExtraToken, token.tStr);
                // Inject the macro tokens
                specReader.injectMacroTokens(_macroTokens);
                // Clear the variable
                _macroTokens = null;
                // Change state
                _state = State.Begin;
                break;

            // ===== Function definition =====
            case FuncDef_Name:
                // Get the function name
                if(_curFDefToken == null) {
                    // Error if the token is not a valid symbol name or the token is a keyword
                    if( !XCom.isSymbolName(token.tStr) ) return _setError(token, Texts.EMsg_SyntaxError                );
                    if(  XCom.isKeyword   (token.tStr) ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                    // Store the function token
                    _curFDefToken = token;
                    // Reset flags
                    _curFDefComma = false;
                    _curFOptParam = false;
                }
                // Check the '('
                else {
                    // Error if the token is not '('
                    if( !token.tStr.equals("(") ) return _setError(token, Texts.EMsg_SyntaxError);
                    // Change state
                    _state = State.FuncDef_Pars;
                }
                break;
            case FuncDef_Pars:
                if(_curFDefComma) {
                    // Reset flag
                    _curFDefComma = false;
                    // Check for ')'
                    if( token.tStr.equals(")") ) {
                        // Change state
                        _state = State.FuncDef_Body;
                    }
                    // Error if the token is not ','
                    else if( !token.tStr.equals(",") ) {
                        return _setError(token, Texts.EMsg_SyntaxError);
                    }
                }
                else {
                    // Set flag
                    _curFDefComma = true;
                    // Check for ')'
                    if( token.tStr.equals(")") ) {
                        // Change state
                        _state = State.FuncDef_Body;
                    }
                    // Check and store the token
                    else {
                        // Check if the parameter is optional
                        final boolean curIsOpt = (token.tStr.charAt(0) == '?');
                        if(curIsOpt) token.tStr = token.tStr.substring(1);
                        // Error if a non-optional parameter is specified after the previous one is an optional parameter
                        if(_curFOptParam && ! curIsOpt) return _setError(token, Texts.EMsg_InvalidFuncNonOptParam, token.tStr);
                        // Set flag
                        if(curIsOpt) _curFOptParam = true;
                        // Error if the token is not a valid symbol name or the token is a keyword
                        if( !XCom.isSymbolName(token.tStr) ) return _setError(token, Texts.EMsg_SyntaxError                );
                        if(  XCom.isKeyword   (token.tStr) ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                        // Store the token
                        if(curIsOpt) token.tStr = '?' + token.tStr;
                        _trTokens.add(token);
                    }
                }
                break;
            case FuncDef_Body:
                // Check if the token is 'supersede'
                if( !_curFDefSuper && _curFDefDepre == null && token.tStr.equals("supersede") ) {
                    _curFDefSuper = true;
                    break;
                }
                // Check if the token is 'deprecated'
                else if(_curFDefDepre == null) {
                    if( token.tStr.equals("deprecated") ) {
                        _curFDefDepre = "";
                        break;
                    }
                }
                else if(_curFDefDepre != null) {
                    if( _curFDefDepre.equals("") ) {
                        // Check if the token is 'by'
                        if(token.tStr.equals("by") ) {
                            if( !"".equals(_curFDefDepre) ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            _curFDefDepre = "<__BY__>";
                            break;
                        }
                    }
                    else if( _curFDefDepre.equals("<__BY__>") ) {
                        // Check the replacement function name
                        if(                    token.isEOL() ) return _setError(token, Texts.EMsg_PrematureEOL               );
                        if( !XCom.isSymbolName(token.tStr)   ) return _setError(token, Texts.EMsg_SyntaxError                );
                        if(  XCom.isKeyword   (token.tStr)   ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                        // Store the replacement function name
                        _curFDefDepre = token.tStr;
                        break;
                    }
                }
                // Error if the token is not EOL
                if( !token.isEOL() ) return _setError(token, Texts.EMsg_UnexpectedExtraToken, token.tStr);
                // Gather the parameter name(s)
                final ArrayList<String> parNames = new ArrayList<>();
                for(final TokenReader.Token t : _trTokens) parNames.add(t.tStr);
                // Create and store a new execution block
                _xbStack.addFuncDefAndPush( new Function(
                    _curFDefToken.path, _curFDefToken.lNum, _curFDefToken.cNum, _curFDefToken.tStr, parNames, _curFDefSuper, _curFDefDepre
                ) );
                // Change state
                _state = State.ClearBegin;
                break;

            // ===== Function return statement =====
            case Return_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get and check the number of tokens
                    final int trTokenSize = _trTokens.size();
                    // Create and process the variable specification list
                    final XCom.ReadVarSpecs rvarSpecs = (trTokenSize >= 1) ? new XCom.ReadVarSpecs() : XCom.RVSpcs_EmptyString;
                    if( !_processRHS_forReadVarSpecs(trTokenSize, rvarSpecs) ) return false;
                    // Create and store a new execution block
                    _xbStack.activeExecBlocks().add( new Return(
                        token.path, token.lNum, token.cNum, rvarSpecs
                    ) );
                    // Set flag
                    check_inOneLineIf = true;
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== Target definition =====
            case TargetDef_Name:
                // Get the function name
                if(_curTDefToken == null) {
                    // Error if the token is a keyword
                    if( XCom.isKeyword(token.tStr) ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                    // Check if the token is a double-quoted string
                    if( token.isDQString() ) {
                        // Error if is not with the combining operator ."..." or the flattening operator :"..."
                        if(token.mDQP != TokenReader.Token.DQPMode.Combine && token.mDQP != TokenReader.Token.DQPMode.Flatten) return _setError(token, Texts.EMsg_DQStrNComFlatNotAllowed);
                        // Process the double-quoted string
                        if( !_splitAndStoreDQString(token) ) return false;
                        if( _trTokens.size() != 1 ) return _setError(token, Texts.EMsg_InvalidTargetNameSet, token.tStr);
                        // Get the flattenen double-quoted string
                        _curTDefToken = _trTokens.remove(0);
                    }
                    // Check if it is a function call
                    else if( XCom.isFunctionName( token.tStr ) ) {
                        // Error if the list is not empty
                        if( !_trTokens.isEmpty() ) return _setError(token, Texts.EMsg_SyntaxError);
                        // Store the token
                        _trTokens.add(token);
                        // Change state
                        _state = State.TargetDef_NameF;
                    }
                    // Store the target token as is
                    else {
                        _curTDefToken = TokenReader.unquoteSQString(token);
                    }
                }
                // Check for EOL
                else if( token.isEOL() ) {
                    // Check if the target name token is a variable
                    XCom.ReadVarSpec targetRVS = null;
                    if( _curTDefToken.tStr.charAt(0) == '$' ) {
                        final XCom.ReadVarSpecs rvs = new XCom.ReadVarSpecs();
                        if( !_storeRVarSpec( rvs, _curTDefToken ) ) return false;
                        targetRVS = rvs.get(0);
                    }
                    // Create and store a new execution block
                    _xbStack.addTargetDefAndPush( new Target(
                        _curTDefToken.path, _curTDefToken.lNum, _curTDefToken.cNum, _curTDefToken.tStr, targetRVS, null
                    ) );
                    // Change state
                    _state = State.ClearBegin;
                }
                // Check the ':'
                else {
                    // Error if the token is not ':'
                    if( !token.tStr.equals(":") ) return _setError(token, Texts.EMsg_SyntaxError);
                    // Change state
                    _state = State.TargetDef_Preqs;
                }
                break;
            case TargetDef_NameF:
                // Check for EOL or ':'
                if( token.isEOL() || token.tStr.equals(":") ) {
                    // Evaluate the function call
                    if(true) {
                        // Evaluate the function call
                        final EvalSFCResult esfcr = _evalSubFuncCall( _trTokens.get(0), 1, _trTokens.size() );
                        if(esfcr == null) return false;
                        if( _trTokens.size() > esfcr.rIdx.get() + 1 ) return _setError( _trTokens.get( esfcr.rIdx.get() + 1 ), Texts.EMsg_FollowFCallNotAllowed );
                        // Create and store a token to read the temporary variable
                        _curTDefToken = _genTokenReadTtxVar(esfcr);
                        // Clear the tokens
                        _trTokens.clear();
                    }
                    // Inject token
                    if( token.isEOL() ) specReader.injectToken_EOL  (token);
                    else                specReader.injectToken_Colon(token);
                    // Change state
                    _state = State.TargetDef_Name;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;
            case TargetDef_Preqs:
                /* FALLTHROUGH */
            case TargetDef_EDeps:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get and check the number of tokens
                    int trTokenSize = _trTokens.size();
                    if(trTokenSize <= 0) return _setError(token, Texts.EMsg_PrematureEOL);
                    // Gather the extra dependency item(s) or prerequisite list
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    if( !_processRHS_forReadVarSpecs(trTokenSize, rvarSpecs) ) return false;
                    // Process extra dependency item(s)
                    if(_state == State.TargetDef_EDeps) {
                        // Process the target name
                        final boolean insertStar = ( rvarSpecs.size() < 3 ) || !rvarSpecs.get(1).value.equals(":");
                        if(insertStar) {
                            rvarSpecs.add( 0, new XCom.ReadVarSpec(true, null, Target.DepForMarker, null, null) );
                            rvarSpecs.add( 1, new XCom.ReadVarSpec(true, null, Target.DepForAll   , null, null) );
                        }
                        else {
                            final XCom.ReadVarSpec depForTarget = rvarSpecs.get(0);
                            rvarSpecs.set( 0, new XCom.ReadVarSpec(true, null, Target.DepForMarker, null, null) );
                            rvarSpecs.set( 1, depForTarget                                                      );
                        }
                        // Add the extra dependency item(s)
                        _xbStack.activeParentTargetBlock().addExtraDeps(rvarSpecs);
                    }
                    // Create and store a new execution block
                    else {
                        // Check if the target name token is a variable
                        XCom.ReadVarSpec targetRVS = null;
                        if( _curTDefToken.tStr.charAt(0) == '$' ) {
                            final XCom.ReadVarSpecs rvs = new XCom.ReadVarSpecs();
                            if( !_storeRVarSpec( rvs, _curTDefToken ) ) return false;
                            targetRVS = rvs.get(0);
                        }
                        // Create and store a new execution block
                        _xbStack.addTargetDefAndPush( new Target(
                            _curTDefToken.path, _curTDefToken.lNum, _curTDefToken.cNum, _curTDefToken.tStr, targetRVS, rvarSpecs
                        ) );
                    }
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    // Error if the token is a keyword
                    if( XCom.isKeyword(token.tStr) ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                    // Error if the token is a double-quoted string
                    if( token.isDQString() ) {
                        // Error if is not with the flattening operator :"..."
                        if(token.mDQP != TokenReader.Token.DQPMode.Flatten) return _setError(token, Texts.EMsg_DQStrNFlatNotAllowed);
                        // Process the double-quoted string
                        if( !_splitAndStoreDQString(token) ) return false;
                    }
                    // Store the token as is
                    else {
                        _trTokens.add( TokenReader.unquoteSQString(token) );
                    }
                }
                break;

            // ===== Arithmetic and logic operation =====
            case ALO_Args:
                // Check for EOL
                if( token.isEOL() ) {
                    // Check for 'local'
                    final boolean local = ( TokenReader.tokMatchPopFirst(_trTokens, token, "local", false) != null );
                    // Get the tokens
                    final boolean           zerIVal = (_alOperSpec.srcPCount == 0);
                    final boolean           oneIVal = (_alOperSpec.srcPCount >= 1);
                    final boolean           twoIVal = (_alOperSpec.srcPCount == 2);
                    final TokenReader.Token valRTok =           TokenReader.tokPopFirst(_trTokens, token, true);
                    final TokenReader.Token com1Tok = oneIVal ? TokenReader.tokPopFirst(_trTokens, token, true) : TokenReader.NullToken;
                    final TokenReader.Token val1Tok = oneIVal ? TokenReader.tokPopFirst(_trTokens, token, true) : TokenReader.NullToken;
                    final TokenReader.Token com2Tok = twoIVal ? TokenReader.tokPopFirst(_trTokens, token, true) : TokenReader.NullToken;
                    final TokenReader.Token val2Tok = twoIVal ? TokenReader.tokPopFirst(_trTokens, token, true) : TokenReader.NullToken;
                    // Check the destination variable name
                        if( !XCom.isSymbolName(valRTok.tStr) ) return _setErrorExt(valRTok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError                  );
                        if(  XCom.isKeyword   (valRTok.tStr) ) return _setErrorExt(valRTok, Texts.EMsg_PrematureEOL, Texts.EMsg_UnexpectedToken, valRTok.tStr);
                    // Check the 1st comma and the 1st source variable name
                    if(oneIVal) {
                        if( !com1Tok.tStr.equals       (","    ) ) return _setErrorExt(com1Tok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError);
                        if( !XCom.isValidConditionValue(val1Tok) ) return _setErrorExt(val1Tok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError);
                    }
                    // Check the 2nd command and the 2nd source variable name
                    if(twoIVal) {
                        if( !com2Tok.tStr.equals       (","    ) ) return _setErrorExt(com2Tok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError);
                        if( !XCom.isValidConditionValue(val2Tok) ) return _setErrorExt(val2Tok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError);
                    }
                    // Check for extra token(s)
                    if( !_trTokens.isEmpty() ) return _setError( _trTokens.get(0), Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                    // Save the destination variable name
                    final String resVarName = valRTok.tStr;
                    // Create a variable specification list
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    if(!zerIVal) {
                        rvarSpecs.add(null);
                    }
                    else {
                        valRTok.tStr = XCom.genRVarName(valRTok.tStr);
                        if( !_storeRVarSpec(rvarSpecs, valRTok) ) return false;
                    }
                        if( !_storeRVarSpec(rvarSpecs, val1Tok) ) return false;
                        if( !_storeRVarSpec(rvarSpecs, val2Tok) ) return false;
                    // Create and store a new execution block for the 'local' declaration statement
                    if(local) {
                        if( !_chkVarName_storeXB_localDecl( token, resVarName, valRTok ) ) return false;
                    }
                    // Create and store a new execution block for the arithmetic and logic operation statement
                    _xbStack.activeExecBlocks().add( new ALOperation(
                        token.path, token.lNum, token.cNum, resVarName, _alOperSpec.operName, rvarSpecs.get(0), rvarSpecs.get(1), rvarSpecs.get(2)
                    ) );
                    // Set flag
                    check_inOneLineIf = true;
                    // Change state
                    _state = State.ClearBegin;
                    break;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== Variable assigment and shell/operating system command =====
            case VarAssign_AT:
                // Check for EOL
                if( token.isEOL() ) {
                    // RHS index
                    int rhsSIdx = 0;
                    // Check for 'local'
                    final boolean isLocal = _trTokens.get(rhsSIdx).tStr.equals("local");
                    if(isLocal) ++rhsSIdx;
                    // Check for 'const'
                    final boolean isConst = _trTokens.get(rhsSIdx).tStr.equals("const");
                    if(isConst) ++rhsSIdx;
                    // Check for a local declaration without a value
                    if(isLocal) {
                        // Error if defining a constant variable without a value
                        if(isConst) return _setError(token, Texts.EMsg_InvalidAssignConstVarNV);
                        // Check the number of tokens
                        final int trTokenSize = _trTokens.size();
                        if(trTokenSize < 2) return _setError(token, Texts.EMsg_PrematureEOL);
                        if(trTokenSize > 2) return _setError(token, Texts.EMsg_SyntaxError );
                        // Check the variable name and the create and store a new execution block
                        if( !_chkVarName_storeXB_localDecl( token, _trTokens.get(rhsSIdx).tStr, _trTokens.get(rhsSIdx) ) ) return false;
                        // Change state
                        _state = State.ClearBegin;
                    }
                    // Got a premature end of line
                    else {
                        return _setError(token, Texts.EMsg_PrematureEOL);
                    }
                }
                // Not EOL
                else {
                    // Store the assigment type
                    _trTokens.add(token);
                    // Change state
                    _state = State.VarAssign_RHS;
                }
                break;
            case VarAssign_RHS:
                /* FALLTHROUGH */
            case Command_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get and check the number of tokens
                    int trTokenSize = _trTokens.size();
                    if(trTokenSize < 2) return _setError(token, Texts.EMsg_PrematureEOL);
                    // RHS index
                    int rhsSIdx = 0;
                    // Check for 'local'
                    final boolean isLocal = _trTokens.get(rhsSIdx).tStr.equals("local");
                    if(isLocal) ++rhsSIdx;
                    // Check for 'const'
                    final boolean isConst = _trTokens.get(rhsSIdx).tStr.equals("const");
                    if(isConst) ++rhsSIdx;
                    // Iniside a function/target body a constant variable must be local
                    if( (_curFDefToken != null || _curTDefToken != null) && isConst && !isLocal ) {
                        return _setError(token, Texts.EMsg_InvalidAssignConstLocal);
                    }
                    // Get the variable name and assignment type
                    final TokenReader.Token tokVarName = _trTokens.get(rhsSIdx++);
                    final TokenReader.Token tokAsnType = _trTokens.get(rhsSIdx++);
                    final String            varName    = tokVarName.tStr;
                    final String            asnType    = tokAsnType.tStr;
                    // Check if the variable assigment is legal (the destination variable is writable)
                    if( !XCom.isLegalVarAssignment(varName, tokVarName.tRX1) ) return _setError(token, Texts.EMsg_IllegalVarAssign);
                    // Interpret the assignment type
                    final XCom.ASNSpec asnSpec = XCom.getASNSpec(asnType);
                    if(asnSpec == null) {
                        if( asnType.charAt(0) == '$'   ) return _setError(tokAsnType, Texts.EMsg_SyntaxError             );
                        if( XCom.isSymbolName(asnType) ) return _setError(tokAsnType, Texts.EMsg_SyntaxError             );
                        if( XCom.isKeyword   (asnType) ) return _setError(tokAsnType, Texts.EMsg_UnexpectedToken, asnType);
                        return _setError(tokAsnType, Texts.EMsg_InvalidAssignType, asnType);
                    }
                    // Lazy variable assigment is not supported for a constant variable
                    if(isConst && asnSpec.numCode != XCom.ASNSpecNumCode_direct) return _setError(token, Texts.EMsg_InvalidAssignConstVar);
                    // Check if it is an assignment from a function call
                    XCom.FuncSpec funcSpec = null;
                    if( trTokenSize > (isLocal ? 3 : 2) ) {
                        final TokenReader.Token chkToken = _trTokens.get(rhsSIdx);
                        final String            chkStr   = chkToken.tStr;
                        if( XCom.isFunctionName(chkStr) ) {
                            // Lazy variable assigment is not supported for result(s) from function call
                            if(!asnSpec.direct) return _setError(token, chkToken, Texts.EMsg_InvalidAssignFLazy);
                            // Get the function specification
                            funcSpec = XCom.getFuncSpec(chkStr);
                            if(funcSpec == null) return _setError( token, chkToken, Texts.EMsg_InvalidFuncName, XCom.normalizeFunctionName(chkStr) );
                            // Ensure the function has return value
                            if(!funcSpec.retVal) {
                                if( !varName.equals(XCom.SVar_Echo) && !varName.equals(XCom.SVar_Echoln) )
                                    return _setError(token, chkToken, Texts.EMsg_InvalidAssignFRet, chkStr);
                                else
                                    return _setError(token, chkToken, Texts.EMsg_InvalidAssignEcho, chkStr);
                            }
                            // Check the '(' and ')'
                            final XCom.IntegerRef lIdx = new XCom.IntegerRef(rhsSIdx + 1);
                            final XCom.IntegerRef rIdx = new XCom.IntegerRef(trTokenSize);
                            if( !_checkFuncCallParentheses(lIdx, rIdx) ) return false;
                            // Check if there are more token(s) after the closing ')'
                            if( rIdx.get() < trTokenSize - 1 ) return _setError( _trTokens.get( rIdx.get() + 1 ), Texts.EMsg_FollowFCallNotAllowed );
                            // Adjust the index and size
                            rhsSIdx += 2;  // Skip the function name and opening '('
                            --trTokenSize; // Skip the                   closing ')'
                        }
                    }
                    // Create a variable specification list
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    // Gather the right hand side of the assigment
                    if(funcSpec == null) {
                        for(int i = rhsSIdx; i < trTokenSize; ++i) {
                            if( !_storeRVarSpec( rvarSpecs, _trTokens.get(i) ) ) return false;
                        }
                    }
                    // Gather the function call arguments
                    else {
                        // Function call is not allowed in shell/operating system commands statement
                        if(_state == State.Command_RHS) return _setError(token, Texts.EMsg_FCallNotAllowed);
                        // Gather the function call arguments
                        if( !_gatherAndStoreFuncCallArgs(rhsSIdx, trTokenSize, funcSpec, rvarSpecs) ) return false;
                    }
                    // Create and store a new execution block
                    if(_state == State.Command_RHS) {
                        // Check if the list is empty
                        if( rvarSpecs.isEmpty() ) return _setError(token, Texts.EMsg_EmptyShellCommand);
                        // Create and store a new execution block
                        _xbStack.activeExecBlocks().add( new ShellCmdDef(
                            token.path, token.lNum, token.cNum, rvarSpecs, _shellCmd_SOF
                        ) );
                    }
                    else { // State.VarAssign_RHS
                        // Except for 'echo', 'echoln', and function call, raise error if there is nothing after the assignment type
                        if( funcSpec == null && rvarSpecs.isEmpty() ) {
                            if( !varName.equals(XCom.SVar_Echo) && !varName.equals(XCom.SVar_Echoln) ) return _setError(token, Texts.EMsg_PrematureEOL);
                        }
                        // Create and store a new execution block
                        _xbStack.activeExecBlocks().add( new VarAssign(
                            token.path, token.lNum, token.cNum, varName, isLocal, isConst, asnSpec, funcSpec, rvarSpecs
                        ) );
                    }
                    // Set flag
                    check_inOneLineIf = true;
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== Variable deletion and deprecation =====
            case VarUnset:
                /* FALLTHROUGH */
            case VarDeprecate:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get the variable name
                    final TokenReader.Token varName = TokenReader.tokPopFirst(_trTokens, token, true);
                    // Check the variable name
                    if( !XCom.isSymbolName(varName.tStr) ) return _setErrorExt(varName, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError                  );
                    if(  XCom.isKeyword   (varName.tStr) ) return _setErrorExt(varName, Texts.EMsg_PrematureEOL, Texts.EMsg_UnexpectedToken, varName.tStr);
                    // Check for the replacement as needed
                    String repName = null;
                    if( _state == State.VarDeprecate && !_trTokens.isEmpty() ) {
                        // Check for 'by'
                        final String byStr = TokenReader.strPopFirst(_trTokens);
                        if( !byStr.equals("by") ) return _setError(token, Texts.EMsg_UnexpectedToken, byStr);
                        // Get the replacement name
                        repName = TokenReader.strPopFirst(_trTokens);
                        if( repName.isEmpty() ) return _setError(token, Texts.EMsg_PrematureEOL);
                        // Check the replacement name
                        if( !XCom.isSymbolName(repName) ) return _setError(token, Texts.EMsg_SyntaxError             );
                        if(  XCom.isKeyword   (repName) ) return _setError(token, Texts.EMsg_UnexpectedToken, repName);
                    }
                    // Check for extra token(s)
                    if( !_trTokens.isEmpty() ) return _setError( _trTokens.get(0), Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                    // Create and store a new execution block for the variable deletion
                    if(_state == State.VarUnset) {
                        _xbStack.activeExecBlocks().add( new VarDelete(
                            token.path, token.lNum, token.cNum, varName.tStr
                        ) );
                    }
                    // Create and store a new execution block for the variable deprecation
                    else {
                        _xbStack.activeExecBlocks().add( new VarDeprecate(
                            token.path, token.lNum, token.cNum, varName.tStr, repName
                        ) );
                    }
                    // Set flag
                    check_inOneLineIf = true;
                    // Change state
                    _state = State.Begin;
                    break;
                }
                // Not EOL
                else {
                    // Store the assigment type
                    _trTokens.add(token);
                }
                break;

            // ===== Function call =====
            case FuncCall_Args:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get the number of tokens
                    final int trTokenSize = _trTokens.size();
                    // Check the '(' and ')'
                    final XCom.IntegerRef lIdx = new XCom.IntegerRef(0          );
                    final XCom.IntegerRef rIdx = new XCom.IntegerRef(trTokenSize);
                    if( !_checkFuncCallParentheses(lIdx, rIdx) ) return false;
                    // Create a variable specification list
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    // Gather the function call arguments
                    if( !_gatherAndStoreFuncCallArgs( lIdx.get() + 1, rIdx.get(), _funcSpec, rvarSpecs ) ) return false;
                    // Check if there are more token(s) after the closing ')'
                    if( rIdx.get() < trTokenSize - 1 ) return _setError( _trTokens.get( rIdx.get() + 1 ), Texts.EMsg_FollowFCallNotAllowed );
                    // Create and store a new execution block
                    _xbStack.activeExecBlocks().add( new FuncCall(
                        token.path, token.lNum, token.cNum, _funcSpec, rvarSpecs
                    ) );
                    // Set flag
                    check_inOneLineIf = true;
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== 'label'statement =====
            case Label_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get and check the label specifier
                    final TokenReader.Token lblTok = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !lblTok.isSQString() ) {
                        if( !XCom.isSymbolName(lblTok.tStr) ) return _setErrorExt(lblTok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError                 );
                        if(  XCom.isKeyword   (lblTok.tStr) ) return _setErrorExt(lblTok, Texts.EMsg_PrematureEOL, Texts.EMsg_UnexpectedToken, lblTok.tStr);
                    }
                    else {
                        lblTok.tStr = TokenReader.unquoteSQString(lblTok).tStr;
                    }
                    // Get and check the ':' token
                    final TokenReader.Token colTok = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !colTok.tStr.equals(":") ) return _setErrorExt(colTok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError);
                    // Check for extra token(s)
                    if( !_trTokens.isEmpty() ) return _setError( token, Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                    // Set the label
                    if( _xbStack.peekFunctionBlock() != null ) _xbStack.peekFunctionBlock().putLabel(lblTok.tStr);
                    else                                       _xbStack.peekTargetBlock  ().putLabel(lblTok.tStr);
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    // Check for double-quoted string
                    if( token.isDQString() ) {
                        if( !_splitAndStoreDQString(token) ) return false;
                    }
                    // Store the token as is
                    else {
                        _trTokens.add(token);
                    }
                }
                break;

            // ===== '[s]go-to'statement =====
            case GoTo_RHS:
                /* FALLTHROUGH */
            case SGoTo_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get and check the number of tokens
                    final int trTokenSize = _trTokens.size();
                    if(trTokenSize <= 0) return _setError(token, Texts.EMsg_PrematureEOL);
                    // Get the label specifier
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    if( !_processRHS_forReadVarSpecs(trTokenSize, rvarSpecs, false) ) return false;
                    // Check the number for specifications
                    if( rvarSpecs.size() != 1 ) return _setError(token, Texts.EMsg_InvalidLabelSpecifier);
                    // Create and store a new execution block
                    _xbStack.activeExecBlocks().add( new GoTo(
                        token.path, token.lNum, token.cNum, rvarSpecs.get(0), (_state == State.SGoTo_RHS)
                    ) );
                    // Set flag
                    check_inOneLineIf = true;
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== 'if'/'elif'/'while'/'do-whilst'/'repeat-until' block statement =====
            case If_RHS:
                /* FALLTHROUGH */
            case Elif_RHS:
                /* FALLTHROUGH */
            case While_RHS:
                /* FALLTHROUGH */
            case DoWhilst_RHS:
                /* FALLTHROUGH */
            case RepeatUntil_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // A flag to indicate that the 'else' block is already pushed
                    boolean elseBlockIsAlreadyPushed = false;
                    // Get the '!', '&', or '!&'
                    final boolean isNeg = TokenReader.strMatchPopFirst(_trTokens, "!");
                    final boolean isStr = TokenReader.strMatchPopFirst(_trTokens, "&");
                    // Process the 1st value
                    final boolean           forElifRHS  = (_state == State.Elif_RHS);
                    final boolean           forWhileRHS = (_state == State.While_RHS);
                    final TokenReader.Token val1TokOrg  = TokenReader.tokPopFirst(_trTokens, token, true);
                    final TokenReader.Token val1Tok     = _processFunCallToTtxVar_forCond(val1TokOrg, forElifRHS, false, forWhileRHS);
                    if(val1Tok == null) return false;
                    if(forElifRHS && val1Tok != val1TokOrg) elseBlockIsAlreadyPushed = true;
                    // Check for one-line if statement
                    _inOneLineIf = (_state == State.If_RHS) ? TokenReader.strMatchPopFirst(_trTokens, ":") : false;
                    // Get the comparison type
                    XCom.CompareType cmpType = _inOneLineIf ? null
                                                            : XCom.getCompareType(
                                                                  (isNeg || isStr) ? null : TokenReader.strPopFirst(_trTokens)
                                                              );
                    final boolean    ctNull  = (cmpType == null);
                    if(ctNull) cmpType = isNeg ? (isStr ? XCom.CompareType.eq_str  : XCom.CompareType.eq )
                                               : (isStr ? XCom.CompareType.neq_str : XCom.CompareType.neq);
                    // Get the 2nd value
                    TokenReader.Token val2TokGet = _inOneLineIf ? null
                                                                : ( (isNeg || isStr) ? null : TokenReader.tokPopFirst(_trTokens, token, false) );
                    if(val2TokGet == null) {
                        if(!ctNull) return _setError(token, Texts.EMsg_PrematureEOL);
                        val2TokGet = TokenReader.newConstStringToken(token, isStr ? "" : "0");
                    }
                    // Process the 2nd value
                    final TokenReader.Token val2Tok = _processFunCallToTtxVar_forCond(val2TokGet, !elseBlockIsAlreadyPushed && forElifRHS, true, forWhileRHS);
                    if(val2Tok == null) return false;
                    if(forElifRHS && val2Tok != val2TokGet) elseBlockIsAlreadyPushed = true;
                    // Check for one-line if statement
                    if(_state == State.If_RHS) {
                        final boolean chk = TokenReader.strMatchPopFirst(_trTokens, ":");
                        if(_inOneLineIf && chk) return _setError( token, Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                        _inOneLineIf |= chk;
                    }
                    // Check for extra token(s)
                    if( !_trTokens.isEmpty() ) return _setError( token, Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                    // Create a variable specification list
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    if( !_storeRVarSpec(rvarSpecs, val1Tok) ) return false;
                    if( !_storeRVarSpec(rvarSpecs, val2Tok) ) return false;
                    // Create and store a new execution block
                    switch(_state) {
                        // ===== If statement =====
                        case If_RHS:
                            _xbStack.addIfBlockAndPush( new IfElse(
                                token.path, token.lNum, token.cNum, rvarSpecs.get(0), cmpType, rvarSpecs.get(1)
                            ) );
                            break;
                        // ===== Elif statement =====
                        case Elif_RHS:
                            _xbStack.addElifBlockAndPush(
                                new IfElse(
                                    token.path, token.lNum, token.cNum, rvarSpecs.get(0), cmpType, rvarSpecs.get(1)
                                )
                                ,
                                elseBlockIsAlreadyPushed
                            );
                            break;
                        // ===== While statement =====
                        case While_RHS:
                            _xbStack.addWhileBlockAndPush( new While(
                                token.path, token.lNum, token.cNum, rvarSpecs.get(0), cmpType, rvarSpecs.get(1)
                            ) );
                            break;
                        // ===== Repeat-Until/Do-Whilst statement =====
                        case RepeatUntil_RHS:
                            /* FALLTHROUGH */
                        case DoWhilst_RHS:
                            // Store the condition
                            final Repeat repeat = _xbStack.peekRepeatBlock();
                            if(repeat != null) repeat.setCondition( rvarSpecs.get(0), cmpType, rvarSpecs.get(1) );
                            // Pop the stack
                            if( _xbStack.popRepeatBlock() == null ) return _setError(token, Texts.EMsg_UnexpectedToken, token.tStr);
                            break;
                    }
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    // Check if the statement is a one-line if
                    if( _state == State.If_RHS && token.tStr.equals(":") ) {
                        // Error if a one-line if statement is already in progress
                        if(_inOneLineIf) return _setError(token, Texts.EMsg_NoAnotherOneLineIf);
                        // Add the ':' token
                        _trTokens.add(token);
                        // Inject an EOL token
                        specReader.injectToken_EOL(token);
                    }
                    // Process the token normally
                    else {
                        if( !_genericStoreToken(token) ) return false;
                    }
                }
                break;

            // ===== 'else' block statement =====
            case Else_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Check for extra token(s)
                    if( !_trTokens.isEmpty() ) return _setError( token, Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                    // Store a new execution block
                    _xbStack.addElseBlockAndPush();
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== 'for' block statement =====
            case For_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Check for 'local'
                    final TokenReader.Token tokLocal = ( _trTokens.size() > 0 ) ? _trTokens.get(0) : null;
                    final boolean           local    = ( TokenReader.tokMatchPopFirst(_trTokens, token, "local", false) != null );
                    // Error if 'local' is not inside a function or target body
                    if( local && !_xbStack.inFunctionBlock() && !_xbStack.inTargetBlock() ) return _setError(tokLocal, Texts.EMsg_UnexpectedToken, tokLocal.tStr);
                    // Get and check the iterator variable name
                    final TokenReader.Token valITok = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !XCom.isSymbolName(valITok.tStr) ) return _setErrorExt(valITok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError                  );
                    if(  XCom.isKeyword   (valITok.tStr) ) return _setErrorExt(valITok, Texts.EMsg_PrematureEOL, Texts.EMsg_UnexpectedToken, valITok.tStr);
                    // Get and check the iterator variable assignment type
                    final TokenReader.Token asnTok = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !asnTok.tStr.equals(":=") ) return _setErrorExt(asnTok, Texts.EMsg_PrematureEOL, Texts.EMsg_InvalidForLoopVAT, asnTok.tStr);
                    // Get and check the begin value
                    final TokenReader.Token valBTok = _processFunCallToTmpVar_forTerm( TokenReader.tokPopFirst(_trTokens, token, true) );
                    if(valBTok == null) return false;
                    // Get and check the 'to' token
                    final TokenReader.Token toTok = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !toTok.tStr.equals("to") ) return _setErrorExt(toTok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError);
                    // Get and check the end value
                    final TokenReader.Token valETok = _processFunCallToTmpVar_forTerm( TokenReader.tokPopFirst(_trTokens, token, true) );
                    if(valETok == null) return false;
                    // Get and check the 'step' token
                    final TokenReader.Token stepTok = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !stepTok.tStr.equals("step") ) return _setErrorExt(stepTok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError);
                    // Get and check the step value
                    final TokenReader.Token valSTok = _processFunCallToTmpVar_forTerm( TokenReader.tokPopFirst(_trTokens, token, true) );
                    if(valSTok == null) return false;
                    // Check for extra token(s)
                    if( !_trTokens.isEmpty() ) return _setError( token, Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                    // Create a variable specification list
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    if( !_storeRVarSpec(rvarSpecs, valBTok) ) return false;
                    if( !_storeRVarSpec(rvarSpecs, valETok) ) return false;
                    if( !_storeRVarSpec(rvarSpecs, valSTok) ) return false;
                    // Create and store a new execution block for the 'local' declaration statement
                    if(local) {
                        if( !_chkVarName_storeXB_localDecl( token, valITok.tStr, valITok ) ) return false;
                    }
                    // Create and store a new execution block for the 'for' block
                    _xbStack.addForBlockAndPush( new For(
                        token.path, token.lNum, token.cNum, valITok.tStr, rvarSpecs.get(0), rvarSpecs.get(1), rvarSpecs.get(2)
                    ) );
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== 'for-each' block statement =====
            case ForEach_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Check for 'local'
                    final TokenReader.Token tokLocal = ( _trTokens.size() > 0 ) ? _trTokens.get(0) : null;
                    final boolean           local    = ( TokenReader.tokMatchPopFirst(_trTokens, token, "local", false) != null );
                    // Error if 'local' is not inside a function or target body
                    if( local && !_xbStack.inFunctionBlock() && !_xbStack.inTargetBlock() ) return _setError(tokLocal, Texts.EMsg_UnexpectedToken, tokLocal.tStr);
                    // Get the index and value variable name
                    TokenReader.Token tokIdx = null;
                    TokenReader.Token tokVal = null;
                    if( _trTokens.size() > 3 && _trTokens.get(1).tStr.equals(",") ) {
                        // Get the index variable name
                        tokIdx = TokenReader.tokPopFirst(_trTokens, token, true);
                        if( !XCom.isSymbolName(tokIdx.tStr) ) return _setErrorExt(tokIdx, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError                 );
                        if(  XCom.isKeyword   (tokIdx.tStr) ) return _setErrorExt(tokIdx, Texts.EMsg_PrematureEOL, Texts.EMsg_UnexpectedToken, tokIdx.tStr);
                        // Discard the ','
                        _trTokens.remove(0);
                        // Get the value variable name
                        tokVal = TokenReader.tokPopFirst(_trTokens, token, true);
                        if( !XCom.isSymbolName(tokVal.tStr) ) return _setErrorExt(tokVal, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError                 );
                        if(  XCom.isKeyword   (tokVal.tStr) ) return _setErrorExt(tokVal, Texts.EMsg_PrematureEOL, Texts.EMsg_UnexpectedToken, tokVal.tStr);
                    }
                    else {
                        // Get the value variable name
                        tokVal = TokenReader.tokPopFirst(_trTokens, token, true);
                        if( !XCom.isSymbolName(tokVal.tStr) ) return _setErrorExt(tokVal, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError                 );
                        if(  XCom.isKeyword   (tokVal.tStr) ) return _setErrorExt(tokVal, Texts.EMsg_PrematureEOL, Texts.EMsg_UnexpectedToken, tokVal.tStr);
                    }
                    // Get and check the 'in' token
                    final TokenReader.Token tokIn = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !tokIn.tStr.equals("in") ) return _setErrorExt(tokIn, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError);
                    // Get and check the set value
                    TokenReader.Token tokSet = null;
                    XCom.ReadVarSpecs tokRVS = null;
                    if( TokenReader.tokMatchFirst(_trTokens, "{") ) {
                        // Get and check the number of tokens for { ... }
                        int trTokenSize = TokenReader.tokMatchIndex(_trTokens, "}");
                        if(trTokenSize <= 0) return _setError( _trTokens.get(0), Texts.EMsg_UnclosedSetCreateOper );
                        ++trTokenSize;
                        // Create and process the variable specification list
                        tokRVS = new XCom.ReadVarSpecs();
                        if( !_processRHS_forReadVarSpecs(trTokenSize, tokRVS) ) return _setError( _trTokens.get(trTokenSize - 1), Texts.EMsg_SyntaxError );
                        if( tokRVS.size() != 1                                ) return _setError( _trTokens.get(trTokenSize - 1), Texts.EMsg_SyntaxError );
                        // Remove the tokens that were processed by the above code
                        _trTokens.subList(0, trTokenSize).clear();
                    }
                    else {
                        // Process using simpler method
                        tokSet = _processFunCallToTmpVar_forTerm( TokenReader.tokPopFirst(_trTokens, token, true) );
                        if(tokSet == null) return _setErrorExt( TokenReader.tokPopFirst(_trTokens, token, true), Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError );
                    }
                    if(tokSet != null && tokRVS != null) return _setError(token, Texts.EMsg_SyntaxError ); // Error if both variants exist
                    // Check for 'skip'
                    final boolean skip = ( TokenReader.tokMatchPopFirst(_trTokens, token, "skip", false) != null );
                    // Get and check the skip value as needed
                    final TokenReader.Token tokSkip = skip ? _processFunCallToTmpVar_forTerm( TokenReader.tokPopFirst(_trTokens, token, true) )
                                                           :  TokenReader.newConstStringToken(token, "0");
                    if(tokSkip == null) return false;
                    // Check for 'step'
                    final boolean step = ( TokenReader.tokMatchPopFirst(_trTokens, token, "step", false) != null );
                    // Get and check the step value as needed
                    final TokenReader.Token tokStep = step ? _processFunCallToTmpVar_forTerm( TokenReader.tokPopFirst(_trTokens, token, true) )
                                                           :  TokenReader.newConstStringToken(token, "1");
                    if(tokStep == null) return false;
                    // Check for extra token(s)
                    if( !_trTokens.isEmpty() ) return _setError( token, Texts.EMsg_UnexpectedExtraToken, _trTokens.get(0).tStr );
                    // Create a variable specification list
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    if(tokRVS != null) { rvarSpecs.add( tokRVS.get(0) );                         }
                    else               { if( !_storeRVarSpec(rvarSpecs, tokSet ) ) return false; }
                                         if( !_storeRVarSpec(rvarSpecs, tokSkip) ) return false;
                                         if( !_storeRVarSpec(rvarSpecs, tokStep) ) return false;
                    // Create and store a new execution block for the 'local' declaration statement
                    if(local) {
                        if( tokIdx != null && !_chkVarName_storeXB_localDecl( token, tokIdx.tStr, tokIdx ) ) return false;
                        if(                   !_chkVarName_storeXB_localDecl( token, tokVal.tStr, tokVal ) ) return false;
                    }
                    // Create and store a new execution block for the 'for-each' block
                    _xbStack.addForEachBlockAndPush( new ForEach(
                        token.path, token.lNum, token.cNum, (tokIdx != null) ? tokIdx.tStr : null, tokVal.tStr, rvarSpecs.get(0), rvarSpecs.get(1), rvarSpecs.get(2)
                    ) );
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== 'jxmake'/'depload'/'sdepload' statement =====
            case JxMake_RHS:
                /* FALLTHROUGH */
            case PJxMake_RHS:
                /* FALLTHROUGH */
            case DepLoad_RHS:
                /* FALLTHROUGH */
            case SDepLoad_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Get and check the number of tokens
                    final int trTokenSize = _trTokens.size();
                    if(trTokenSize <= 0) return _setError(token, Texts.EMsg_PrematureEOL);
                    // Gather prerequisite list or file name parts
                    final XCom.ReadVarSpecs rvarSpecs = new XCom.ReadVarSpecs();
                    for(int i = 0; i < trTokenSize; ++i) {
                        if( !_storeRVarSpec( rvarSpecs, _trTokens.get(i) ) ) return false;
                    }
                    // Create and store a new execution block
                    if(_state == State.JxMake_RHS || _state == State.PJxMake_RHS) {
                        _xbStack.activeExecBlocks().add( new TargetRun(
                            token.path, token.lNum, token.cNum, rvarSpecs, _state == State.PJxMake_RHS
                        ) );
                    }
                    else {
                        _xbStack.activeExecBlocks().add( new DepLoad(
                            token.path, token.lNum, token.cNum, rvarSpecs, _state == State.SDepLoad_RHS
                        ) );
                    }
                    // Set flag
                    check_inOneLineIf = true;
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== 'eval' statement =====
            case Eval_RHS:
                // Check for EOL
                if( token.isEOL() ) {
                    // Check for 'local'
                    final boolean local = ( TokenReader.tokMatchPopFirst(_trTokens, token, "local", false) != null );
                    // Get and check the number of tokens
                    final int trTokenSize = _trTokens.size();
                    if(trTokenSize < 3) return _setError(token, Texts.EMsg_PrematureEOL);
                    // Check the destination variable name
                    final TokenReader.Token valRTok = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !XCom.isSymbolName(valRTok.tStr) ) return _setErrorExt(valRTok, Texts.EMsg_PrematureEOL, Texts.EMsg_SyntaxError                  );
                    if(  XCom.isKeyword   (valRTok.tStr) ) return _setErrorExt(valRTok, Texts.EMsg_PrematureEOL, Texts.EMsg_UnexpectedToken, valRTok.tStr);
                    // Get the assigment type
                    final TokenReader.Token valASN = TokenReader.tokPopFirst(_trTokens, token, true);
                    if( !valASN.tStr.equals(":=") ) return _setErrorExt(valASN, Texts.EMsg_PrematureEOL, Texts.EMsg_InvalidAssignTypeEval);
                    // Convert infox to postfix
                    final XCom.PostfixTerms pts = InfixToPostfix.process(this, _trTokens);
                    if(pts == null) return false;
                    // Create and store a new execution block for the 'local' declaration statement
                    if(local) {
                        if( !_chkVarName_storeXB_localDecl( token, valRTok.tStr, valRTok ) ) return false;
                    }
                    // Create and store a new execution block for the 'eval' statement
                    _xbStack.activeExecBlocks().add( new Eval(
                        token.path, token.lNum, token.cNum, valRTok.tStr, pts
                    ) );
                    // Set flag
                    check_inOneLineIf = true;
                    // Change state
                    _state = State.ClearBegin;
                }
                // Other types of tokens
                else {
                    if( !_genericStoreToken(token) ) return false;
                }
                break;

            // ===== Invalid state =====
            default:
                return _setError(token, Texts.EMsg_UnknownCompileError);

        } // switch _state

        // Check if it is the time to inject an 'endif' followed by an 'EOL' tokens
        if(check_inOneLineIf && _inOneLineIf) {
            // Reset flag
            _inOneLineIf = false;
            // Inject tokens
            specReader.injectToken_endif(token);
            specReader.injectToken_EOL  (token);
        }

        // Done
        return true;
    }

} // class XBBuilder
