/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.Arrays;
import java.util.List;

import javax.swing.UIManager;

import jxm.xb.*;

import static jxm.gcomp.se.JxMakeRootPane_Action.*;
import static jxm.tool.I18N._T;


public class Texts {

    public static final String _JxMakeWarningPrefix    = _T("JxMake WARNING: ");
    public static final String _JxMakeErrorPrefix      = _T("JxMake ERROR: ");

    public static final String _WarningSuppressedError = _T("[WARNING:SUPPRESSED ERROR]: ");

    public static final String _MainThread             = _T("[MAIN-THREAD]");
    public static final String _WorkerThread           = _T("[WORKER-THREAD]");

    public static final String _RTExecErrorPrefix      = _T("execution error:");

    public static final String _fmt_YYYYMMDD_hhmm      = "%04d-%02d-%02d %02d:%02d";
    public static final String _fmt_YYYYMMDD_hhmmss    = "%04d-%02d-%02d %02d:%02d:%02d";
    public static final String _fmt_YYYYMMDD_hhmmssddd = "%04d-%02d-%02d %02d:%02d:%02d.%03d";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String IMsg_JxMakeBuiltInfo                 = _T("Compiled and built using Java %s\n\n%s\n");

    public static final String IMsg_CJFDeletingDestDir              = _T("Deleting the destination directory '%s'\n");
    public static final String IMsg_CJFCreatingDestDir              = _T("Creating the destination directory '%s'\n");
    public static final String IMsg_CJFWritingFile                  = "    " + _T("Writing file '%s'\n");

    public static final String IMsg_IntentionalStackDumpBeg         = "\n========== INTENTIONAL STACK DUMP ==========";
    public static final String IMsg_IntentionalStackDumpEnd         = "============================================\n";

    public static final String IMsg_ArgParserUnknownJDKVersion      = _T("<unknown_version>");
    public static final String IMsg_ArgParserNoArgNoDefSpecFile     = _T("no command line arguments specified and no '%s' in the current directory");
    public static final String IMsg_ArgParserOptionCReqArg          = _T("option '-C' requires an argument");
    public static final String IMsg_ArgParserOptionFReqArg          = _T("option '-f' requires an argument");
    public static final String IMsg_ArgParserOptionEReqArg          = _T("option '-e' requires an argument");
    public static final String IMsg_ArgParserErrorOptEOptF          = _T("option '-e' cannot be used together with option '-f'");
    public static final String IMsg_ArgParserInvalidOption          = _T("invalid option '%s'");
    public static final String IMsg_ArgParserInvalidArgument        = _T("invalid argument '%s'");
    public static final String IMsg_ArgParserVarAssignReqArg        = _T("variable assigment '%s=' requires an argument");

    public static final String IMsg_SReadSkipLoadJxMakeLib          = _T("SRead: Skip loading JxMake library <%s> (it is already included once) ...\n");
    public static final String IMsg_SReadSkipLoadJxMakePCLib        = _T("SRead: Skip loading precompiled JxMake library <%s> because it is not up to date\n");
    public static final String IMsg_SReadLoadJxMakePCLib            = _T("SRead: Loading precompiled JxMake library <%s> ...\n");
    public static final String IMsg_SReadLoadJxMakePCLibDone        = _T("SRead: The precompiled JxMake library was loaded successfully");
    public static final String IMsg_SReadLoadJxMakePCLibFail        = _T("SRead: The precompiled JxMake library did not load successfully");
    public static final String IMsg_SReadLoadJxMakePCLibSupErr      = _T("failed loading precompiled library file <%s>");
    public static final String IMsg_SReadInvalidJxMakeLib           = _T("SRead: Invalid JxMake library %s ...\n");
    public static final String IMsg_SReadLoadJxMakeLib              = _T("SRead: Loading JxMake library <%s> ...\n");

    public static final String IMsg_DeletingProjectCacheTmpDir      = _T("\nDeleting project cache/temporary directory '%s'\n\n");
    public static final String IMsg_ClearingProjectCacheDir         = _T("\nClearing the project cache directory '%s'\n\n");

    public static final String IMsg_DeletingCompiledJxMakeSpecCache = _T("\nDeleting compiled JxMake specification file cache '%s'\n");

    public static final String IMsg_XLoadJxMakeCacheInvalid         = _T("XLoad: the JxMake specification cache file has an invalid header or an incompatible version\n");
    public static final String IMsg_XLoadJxMakeCacheLoadFail        = _T("XLoad: failed to load the JxMake specification cache file\n");
    public static final String IMsg_XLoadJxMakeCacheLoadDone        = _T("XLoad: the JxMake specification cache file has been successfully loaded\n");
    public static final String IMsg_XLoadJxMakeNoSpecCachePath      = _T("XLoad: unable to get the JxMake specification cache file path\n");
    public static final String IMsg_XLoadJxMakeSpecCacheNoDF        = _T("XLoad: the dependency list file of the JxMake specification cache file is not found\n");
    public static final String IMsg_XLoadJxMakeSpecCacheDFLoadFail  = _T("XLoad: the dependency list file of the JxMake specification cache file has been successfully loaded\n");
    public static final String IMsg_XLoadJxMakeSpecCacheDFLoadDone  = _T("XLoad: the dependency list file of the JxMake specification cache file has been successfully loaded\n");
    public static final String IMsg_XLoadJxMakeSpecCacheDFOutOfDate = _T("XLoad: the JxMake specification cache file is out of date\n");
    public static final String IMsg_XLoadJxMakeSpecCacheDFUpToDate  = _T("XLoad: the JxMake specification cache file is up to date\n");

    public static final String IMsg_XSaveJxMakeCacheSaveFail        = _T("XSave: failed to save the JxMake specification cache file\n");
    public static final String IMsg_XSaveJxMakeCacheSaveDone        = _T("XSave: the JxMake specification cache file has been successfully saved\n");
    public static final String IMsg_XSaveJxMakeNoSpecCachePath      = _T("XSave: unable to get the JxMake specification cache file path\n");
    public static final String IMsg_XSaveJxMakeSpecCacheDFSaveFail  = _T("XSave: failed to save the dependency list file of the JxMake specification cache file\n");
    public static final String IMsg_XSaveJxMakeSpecCacheDFSaveDone  = _T("XSave: the dependency list file of the JxMake specification cache file has been successfully saved\n");

    public static final String IMsg_HTTPDownloaderError             = _T("\nERROR %d\n");
    public static final String IMsg_HTTPDownloaderESizeBytes        = _T("=== %d bytes\n");
    public static final String IMsg_HTTPDownloaderPSizeBytes        = _T("+++ %d/%d bytes\n");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String WMsg_DeprecatedSVar0     = _T("the special-variable '%s' is deprecated; no replacement has been specified");
    public static final String WMsg_DeprecatedSVar1     = _T("the special-variable '%s' is deprecated; use '%s' instead");

    public static final String WMsg_DeprecatedSVarSCut0 = _T("the special-variable-shortcut '%s' is deprecated; no replacement has been specified");
    public static final String WMsg_DeprecatedSVarSCut1 = _T("the special-variable-shortcut '%s' is deprecated; use '%s' instead");

    public static final String WMsg_DeprecatedUsrVar0   = _T("the user-defined variable '%s' is deprecated; no replacement has been specified");
    public static final String WMsg_DeprecatedUsrVar1   = _T("the user-defined variable '%s' is deprecated; use '%s' instead");

    public static final String WMsg_DeprecateArgVarIgn  = _T("the variable '%s' comes from a function call parameter, it cannot be deprecated");

    public static final String WMsg_SetDeprecatedUsrVar = _T("assigning to a deprecated user-defined variable '%s'");
    public static final String WMsg_DelDeprecatedUsrVar = _T("deleting a deprecated user-defined variable '%s'");

    public static final String WMsg_DeprecatedBIFunc0   = _T("the built-in function '$%s' is deprecated; no replacement has been specified");
    public static final String WMsg_DeprecatedBIFunc1   = _T("the built-in function '$%s' is deprecated; use '$%s' instead");

    public static final String WMsg_DeprecatedUsrFunc0  = _T("the user-defined function '%s()' is deprecated; no replacement has been specified");
    public static final String WMsg_DeprecatedUsrFunc1  = _T("the user-defined function '%s()' is deprecated; use '%s()' instead");

    public static final String WMsg_EvalVarNotExist     = _T("evaluating a non-existent variable '%s'");

    public static final String WMsg_EvalInvRefVar0      = _T("evaluating by dereferencing an invalid/non-existent reference via a non-existent variable '%s'");
    public static final String WMsg_EvalInvRefVar1      = _T("evaluating by dereferencing an invalid/non-existent reference via the reference variable '%s'");
    public static final String WMsg_EvalInvRefVarExt0   = _T("evaluating by dereferencing an invalid/non-existent reference to '%s' via a non-existent variable '%s'");
    public static final String WMsg_EvalInvRefVarExt1   = _T("evaluating by dereferencing an invalid/non-existent reference to '%s' via the reference variable '%s'");

    public static final String WMsg_AsgnInvRefVar0      = _T("assigning to by dereferencing an invalid/non-existent reference via a non-existent variable '%s'");
    public static final String WMsg_AsgnInvRefVar1      = _T("assigning to by dereferencing an invalid/non-existent reference via the reference variable '%s'");
    public static final String WMsg_AsgnInvRefVarExt0   = _T("assigning to by dereferencing an invalid/non-existent reference to '%s' via a non-existent variable '%s'");
    public static final String WMsg_AsgnInvRefVarExt1   = _T("assigning to by dereferencing an invalid/non-existent reference to '%s' via the reference variable '%s'");

    public static final String WMsg_CnvStringInteger    = _T("the string '%s' cannot be parsed to an integer value");
    public static final String WMsg_CnvStringBoolean    = _T("the string '%s' cannot be parsed to a boolean value");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

  //public static final String EMsg_AppRestartError         = _T("\n### APPLICATION RESTART ERROR ###\n");

    public static final String EMsg_NotImplementedYet       = _T("not implemented yet");

    public static final String EMsg_UnknownCompileError     = _T("unknown compile error");
    public static final String EMsg_PrematureEOF            = _T("premature end of file");
    public static final String EMsg_PrematureEOL            = _T("premature end of line");
    public static final String EMsg_SyntaxError             = _T("syntax error");
    public static final String EMsg_UnsupportedPragma       = _T("unsupported pragma '%s'");
    public static final String EMsg_JxMakeInvalidVer        = _T("invalid JxMake version specifier '%s'");
    public static final String EMsg_JxMakeMinVer            = _T("JxMake version >= %s is required");
    public static final String EMsg_JxMakeMaxVer            = _T("JxMake version <= %s is required");
    public static final String EMsg_UnexpectedExtraToken    = _T("unexpected extra token '%s'");
    public static final String EMsg_UnexpectedToken         = _T("unexpected token '%s'");
    public static final String EMsg_UnexpectedTokenAfrLocal = _T("unexpected token '%s' after 'local'");
    public static final String EMsg_UnexpectedTokenAfrConst = _T("unexpected token '%s' after 'const'");
    public static final String EMsg_UnexpectedKeyword       = _T("unexpected keyword '%s'");
    public static final String EMsg_UnclosedRQStr           = _T("unclosed raw-quoted string");
    public static final String EMsg_UnclosedSQStr           = _T("unclosed single-quoted string");
    public static final String EMsg_UnclosedDQStr           = _T("unclosed double-quoted string");
    public static final String EMsg_UnclosedSetCreateOper   = _T("unclosed set creation operator '{ ... }'");
    public static final String EMsg_UnbalancedMLCMarker     = _T("unbalanced number of '(*' and '*)'");
    public static final String EMsg_UnbalancedLRParentheses = _T("unbalanced number of '(' and ')'");
    public static final String EMsg_UnbalancedLRSqrBracket  = _T("unbalanced number of '[' and ']'");
    public static final String EMsg_LabelExist              = _T("a label specifier with the same value ('%s') is already defined in the function/target body'");
    public static final String EMsg_MacroDefExist           = _T("a macro with the same name ('%s') is already defined in '%s:%d:%d'");
    public static final String EMsg_MacroDefNotExist        = _T("using a non-existent macro '%s'");
    public static final String EMsg_IllegalMacroDef         = _T("macro definition statement is not allowed here");
    public static final String EMsg_IllegalMacroUnDef       = _T("macro undefinition statement is not allowed here");
  //public static final String EMsg_IllegalMacroUseInMacro  = _T("using a macro from within the definition of another macro is not allowed");
    public static final String EMsg_IllegalMacroUseInMacro  = _T("using a macro from within the definition of another macro consecutively is not allowed");
    public static final String EMsg_IllegalGlobalDepLoad    = _T("loading global dependency list is not allowed here");
    public static final String EMsg_IllegalFuncDef          = _T("function definition is not allowed here");
    public static final String EMsg_IllegalTargetDef        = _T("target definition is not allowed here");
    public static final String EMsg_IllegalVarAssign        = _T("illegal variable assignment");
    public static final String EMsg_IllegalJxMakeTargetRun  = _T("jxmake statement is not allowed here");
    public static final String EMsg_IllegalJxWaitTargetRun  = _T("jxwait statement is not allowed here");
    public static final String EMsg_IllegalCFOperSQStr      = _T("the combining and flattening operators cannot be used with single-quoted strings");
    public static final String EMsg_IllegalSetCreateOperUse = _T("using the set creation operator '{ ... }' is not allowed for this term");
    public static final String EMsg_InvalidDQStrMLBeg       = _T("invalid opening token for a multiline double-quoted string");
    public static final String EMsg_InvalidDQStrMLEnd       = _T("invalid closing token for a multiline double-quoted string");
    public static final String EMsg_InvalidEscSeqOctValue   = _T("invalid octal value escape sequence '%s'");
    public static final String EMsg_InvalidEscSeqDecValue   = _T("invalid decimal value escape sequence '%s'");
    public static final String EMsg_InvalidEscSeqHexValue   = _T("invalid hexadecimal value escape sequence '%s'");
    public static final String EMsg_InvalidEscSeqUniValue   = _T("invalid Unicode value escape sequence '%s'");
    public static final String EMsg_InvalidLabelSpecifier   = _T("invalid label specifier");
    public static final String EMsg_InvalidALExprUnexpected = _T("invalid arithmetic and logic expression: unexpected token '%s'");
    public static final String EMsg_InvalidALExprOperator   = _T("invalid arithmetic and logic expression: operator preceded by another operator");
    public static final String EMsg_InvalidALExprOperand    = _T("invalid arithmetic and logic expression: operand preceded by another operand");
    public static final String EMsg_InvalidALExprTernary    = _T("invalid arithmetic and logic expression: unbalanced number of '?' and ':'");
    public static final String EMsg_InvalidALInlFuncArgCnt  = _T("invalid arithmetic and logic expression: inline function '%s' expects %d argument(s) but got %d argument(s)");
    public static final String EMsg_InvalidALOperand        = _T("invalid arithmetic and logic expression: invalid operand '%s'");
    public static final String EMsg_InvalidAssignEcho       = _T("invalid echo[ln] statement (function '%s' does not return any value)");
    public static final String EMsg_InvalidAssignType       = _T("invalid variable assignment type '%s'");
    public static final String EMsg_InvalidAssignTypeEval   = _T("invalid variable assignment type (only ':=' is supported by the 'eval' statement)");
    public static final String EMsg_InvalidAssignFRet       = _T("invalid variable assignment (function '%s' does not return any value)");
    public static final String EMsg_InvalidAssignConstVar   = _T("invalid variable assignment (only ':=' is supported when defining a constant variable)");
    public static final String EMsg_InvalidAssignConstVarNV = _T("invalid variable assignment (defining a constant variable without a value is not allowed)");
    public static final String EMsg_InvalidAssignConstLocal = _T("invalid variable assignment (a constant variable inside a function/target body must be defined as 'local')");
    public static final String EMsg_InvalidAssignFLazy      = _T("invalid variable assignment (lazy assignment using the result of a function call is not supported)");
    public static final String EMsg_InvalidVarName          = _T("invalid variable name '%s'");
    public static final String EMsg_InvalidSVarName         = _T("invalid/unknown special-variable name '%s'");
    public static final String EMsg_InvalidRegExp           = _T("invalid regular expression");
    public static final String EMsg_InvalidPartXIndexFor    = _T("invalid '$part%s()' index for '%s<%s>'");
    public static final String EMsg_InvalidFuncName         = _T("invalid/unknown function name '%s'");
    public static final String EMsg_InvalidFuncNonOptParam  = _T("invalid function parameter '%s': all parameters after the first optional parameter must also be optionals");
    public static final String EMsg_InvalidTargetName       = _T("invalid target name '%s'");
    public static final String EMsg_InvalidTargetNameSet    = _T("invalid target name set '%s': the splitting process does not produce exactly one set");
    public static final String EMsg_InvalidConditionExpr    = _T("invalid condition expression '%s'");
    public static final String EMsg_InvalidTermExpr         = _T("invalid term expression '%s'");
    public static final String EMsg_InvalidForLoopVAT       = _T("invalid for-loop variable assignment type '%s' (only ':=' is supported)");
    public static final String EMsg_InvalidExtraDepLoc      = _T("got an 'extradep' statement after other statement(s)");
    public static final String EMsg_InvalidShellCommandLoc  = _T("shell/operating system commands must be put inside user defined functions");
    public static final String EMsg_InvalidShellCommandPCnt = _T("special shell/operating system command '%s' expects %d argument(s) but got %d argument(s)");
    public static final String EMsg_InvalidShellCommandPMC1 = _T("special shell/operating system command '%s' expects at least 1 argument but got 0 argument");
    public static final String EMsg_EmptyShellCommand       = _T("empty shell/operating system command statement");
    public static final String EMsg_DQStrNotAllowed         = _T("double-quoted string is not allowed here");
    public static final String EMsg_DQStrNComFlatNotAllowed = _T("non-combined/flattened double-quoted string is not allowed here");
    public static final String EMsg_DQStrNFlatNotAllowed    = _T("non-flattened double-quoted string is not allowed here");
    public static final String EMsg_FCallNotAllowed         = _T("function call is not allowed here");
    public static final String EMsg_FollowFCallNotAllowed   = _T("function call cannot be followed by more expression(s)");
    public static final String EMsg_SubFuncAsArgNoRetVal    = _T("function '%s' cannot be used as a function call argument (it does not return any value)");
    public static final String EMsg_TooManyNumArg           = _T("too many number of arguments in function call '%s' (given %d; required %d; optional %d)");
    public static final String EMsg_TooFewNumArg            = _T("too few number of arguments in function call '%s' (given %d; required %d; optional %d)");
    public static final String EMsg_NoAnotherOneLineIf      = _T("another one-line if statement cannot follow a one-line if statement");
    public static final String EMsg_TokenNoFollowOneLineIf  = _T("token '%s' cannot follow a one-line if statement");

    public static final String EMsg_UnknownRuntimeError     = _T("unknown runtime error");
    public static final String EMsg_NoProjectTmpDir         = _T("the project cache/temporary directory is not defined");
    public static final String EMsg_NoJxMakeUDataDir        = _T("the JxMake user data directory is not defined");
    public static final String EMsg_NoJxMakeUToolsDir       = _T("the JxMake user tools directory is not defined");
    public static final String EMsg_NoJxMakeExeDir          = _T("the JxMake executable directory is not defined");
    public static final String EMsg_RegExpExecError         = _T("regular expression " + _RTExecErrorPrefix + " %s");
    public static final String EMsg_PostfixEvaluationError  = _T("postfix expression evaluation error");
    public static final String EMsg_NestedPJxMake           = _T("a '+jxmake' statement cannot be executed from within another '+jxmake' statement");
    public static final String EMsg_CannotGetOrgJavaCommand = _T("cannot get the original Java command");

    public static final String EMsg_InvalidConstVarContext  = _T("defining a constant variable is not allowed in this context");
    public static final String EMsg_InvalidConstVarModify   = _T("modifying a constant variable is not allowed");
    public static final String EMsg_InvalidConstVarDelete   = _T("deleting a constant variable is not allowed");

    public static final String EMsg_EvalRegExpVarNConst     = _T("evaluateRegExp(): this variable is not a constant");
    public static final String EMsg_MultipleRegExp          = _T("executing this expression will produce multiple regular expressions");
    public static final String EMsg_BothFlagsSet            = _T("both the 'concat' and 'onlyIfNotSet' flags are set");

    public static final String EMsg_LabelNotExist           = _T("jumping to a non-existent label specifier '%s'");

    public static final String EMsg_PartOptArgNonUFunc      = _T("passing partial optional arguments can only be done when calling user-defined functions");

    public static final String EMsg_UserFuncDefExist        = _T("a user-defined function with the same name ('%s') is already defined in '%s:%d:%d'");
    public static final String EMsg_UserFuncDefNotExist     = _T("calling a non-existent user-defined function '%s'");
    public static final String EMsg_UserFuncDefNoPrevDef    = _T("superseding a non-existent user-defined function '%s'");
    public static final String EMsg_UserFuncDefNonSuper     = _T("trying to call the origin user-defined function from a non-superseding user-defined function '%s'");
    public static final String EMsg_UserFuncTooManyNumArg   = _T("too many number of arguments in user-defined function call '%s'");
    public static final String EMsg_UserFuncTooFewNumArg    = _T("too few number of arguments in user-defined function call '%s'");
    public static final String EMsg_UserFuncNoCallOther     = _T("a user-defined function called using '$exec()' is not allowed to call other user-defined functions");
    public static final String EMsg_UserFuncNoShellOper     = _T("only a user-defined function called using '$exec()' is allowed to execute shell/operating system commands");

    public static final String EMsg_TargetDefNameEmpty      = _T("target name evaluates to an empty string");
    public static final String EMsg_TargetDefExist          = _T("a target with the same name ('%s') is already defined in '%s:%d:%d'");
    public static final String EMsg_TargetDefNotDefined     = _T("target '%s' is not defined");
    public static final String EMsg_TargetDefPreqNotDefined = _T("the prerequisite target is not defined");
    public static final String EMsg_TargetExtraDepEvalError = _T("target '%s' extra dependency item '%s' evaluation error: %s");
    public static final String EMsg_TargetPreqNameTNoPRule  = _T("target '%s' prerequisite '%s' name evaluation error: the target name contains no pattern rule (glob)");
    public static final String EMsg_TargetPreqNameEvalError = _T("target '%s' prerequisite '%s' name evaluation error: %s");
    public static final String EMsg_TargetPreqExecError     = _T("target '%s' prerequisite '%s' " + _RTExecErrorPrefix + " %s");
    public static final String EMsg_TargetPreqNameInvalidSV = _T("target '%s' prerequisite '%s' : only '$[target]' can be used in prerequisite list");
    public static final String EMsg_TargetPreqNameSelfRecur = _T("target '%s' prerequisite '%s' : self-recursion detected");

    public static final String EMsg_LoadDepDataLevelOrRec   = _T("dependency nesting level is too deep or dependency recursion was detected '%s' -> '%s'");
    public static final String EMsg_GenDepDataFailed        = _T("failed generating dependency data for '%s': unknown error");

    public static final String EMsg_ShellCmdEvalToEmpty     = _T("the shell/operating system command evaluates to an empty expression");

    public static final String EMsg_AltGLibCInvalidPath     = _T("$alt_glibc_for(): the path for %s is not valid");

    public static final String EMsg_getenv_NumDefValLarger  = _T("$getenv(): the number of default values (%d) is larger than the number of environment variable names (%d)");

    public static final String EMsg_prep_NumIdxRepNotSame   = _T("$part_replace(): the number of indexes (%d) is not the same with the number of replacement values (%d)");

    public static final String EMsg_xxx_EmptyValueStr       = _T("$%s(): empty value for %s");
    public static final String EMsg_xxx_EmptyPathStr        = _T("$%s(): empty path for %s");
    public static final String EMsg_xxx_EmptyRegExpStr      = _T("$%s(): empty regular expression for %s");

    public static final String EMsg_to_cp_MustBeOneChar     = _T("$to_cp(): the input value can only contain one character");

    public static final String EMsg_xxfile_NumFileNotSame   = _T("$%sfile(): the number of source files (%d) is not the same with the number of destination files (%d)");

    public static final String EMsg_gzip_NumFileNotSame     = _T("$%s(): the number of input files (%d) is not the same with the number of output files (%d)");

    public static final String EMsg_cpdir_rec_NumDirNotSame = _T("$cpdir_rec(): the number of source directories (%d) and/or the number of destination directories (%d) and/or the number of flags (%d) are not the same");

    public static final String EMsg_WriteMultipleDstFile    = _T("$%s(): cannot write to more than one destination file");

    public static final String EMsg_lookup_KeyValueSizeDiff = _T("$lookup(): the number of keys (%d) is not the same as the number of values (%d)");

    public static final String EMsg_cmp_InvalidCmpOper      = _T("$cmp(): invalid comparison operator '%s'");

    public static final String EMsg_printf_Error            = _T("$[v][s]printf(): error");
    public static final String EMsg_printf_InvalidFSpec     = _T("$[v][s]printf(): invalid format specifier '%s'");
    public static final String EMsg_printf_NumFSAGreater    = _T("$[v][s]printf(): the number of format specifiers (%d) is greater than the number of arguments (%d)");
    public static final String EMsg_printf_NumFSALess       = _T("$[v][s]printf(): the number of format specifiers (%d) is less than the number of arguments (%d)");

    public static final String EMsg_xxx_SerialCPError       = _T("$%sserial_%s(): execution error");

    public static final String EMsg_xxx_EmptyXMLFrameStr    = _T("$xmlframe_%s(): empty %s");

    public static final String EMsg_InvalidRuleLine         = _T("\n'%s':\ninvalid Arduino 'boards.txt' decoding rule line:\n'%s'");
    public static final String EMsg_InvalidRuleLineUserIgn  = _T(">>> error in the user Arduino 'boards.txt' decoding rule file '%s'<<<\n>>> the user rule file is ignored <<<\n");
    public static final String EMsg_InvalidRuleLineDefError = _T(">>> error in the default Arduino 'boards.txt' decoding rule file <<<\n");

    public static final String EMsg_UnresolvedABoardsTxtID  = _T("unresolved Arduino 'boards.txt' ID (name) '%s'");
    public static final String EMsg_UnresolvedABoardsTxtRef = _T("unresolved Arduino 'boards.txt' reference '%s'");

    public static final String EMsg_gdl_NumFileDirNotSame   = _T("$gdl_xxx(): the number of output depedency file names (%d) is not the same with the number of source directories (%d)");

    public static final String EMsg_IndexOutOfRange         = _T("index %d is out of range");
    public static final String EMsg_InvalidIndex            = _T("index %d is invalid");
    public static final String EMsg_InvalidReference        = _T("invalid reference '%s'");
    public static final String EMsg_DuplicatedReference     = _T("duplicated reference '%s'");

    public static final String EMsg_InvalidBinFileFormat    = _T("'%s': invalid '%s' file format");
    public static final String EMsg_InvalidBinFileEOF       = _T("'%s': premature end of file for '%s' file format");

    public static final String EMsg_InvalidRecLineDFormat   = _T("'%s':%d:invalid '%s' record line data format");
    public static final String EMsg_InvalidRecLineStartCode = _T("'%s':%d:invalid '%s' record line start code");
    public static final String EMsg_InvalidRecLineRecType   = _T("'%s':%d:invalid '%s' record line record type");
    public static final String EMsg_InvalidRecLineByteCount = _T("'%s':%d:invalid '%s' record line byte count");
    public static final String EMsg_InvalidRecLineAddresss  = _T("'%s':%d:invalid '%s' record line address");
    public static final String EMsg_InvalidRecLineChecksum  = _T("'%s':%d:invalid '%s' record line checksum");

    public static final String EMsg_SyntaxErrorCommandFLine = _T("%s:%d: command syntax error '%s'");
    public static final String EMsg_UnsupportedCommandFLine = _T("%s:%d: unsupported command '%s'");
    public static final String EMsg_UnsuppoParmCommandFLine = _T("%s:%d: unsupported parameter(s) in command '%s'");
    public static final String EMsg_TooManyParmCommandFLine = _T("%s:%d: too many parameters in command '%s'");
    public static final String EMsg_InvalidParmCommandFLine = _T("%s:%d: invalid parameter(s) in command '%s'");
    public static final String EMsg_InvalidSHdrCommandFLine = _T("%s:%d: stored header is invalid for command '%s'");
    public static final String EMsg_InvalidSTrlCommandFLine = _T("%s:%d: stored trailer is invalid for command '%s'");

    public static final String EMsg_AddrSpcInvalid          = _T("'%s':invalid address space size (%d) for '%s' format");
    public static final String EMsg_AddrSpcTooLargeFFormat  = _T("'%s':address space is too large for the '%s' format");
    public static final String EMsg_AddrSpcTooLargeFSetting = _T("'%s':address space is too large for the selected '%s' setting");

    public static final String EMsg_ProgExecError           = _T("ProgExec:%s: ERROR: %s()");
    public static final String EMsg_ProgExecError_ByteNEQ   = _T(": mismatched byte @%08X [%02X->%02X]");
    public static final String EMsg_ProgExecUnspProgConfigN = _T("ProgExec:%s: unsupported programmer configuration name '%s'");
    public static final String EMsg_ProgExecInvlProgConfigN = _T("ProgExec:%s: invalid programmer configuration name '%s'");
    public static final String EMsg_ProgExecInvlBackend     = _T("ProgExec:%s: invalid backend '%s'");
    public static final String EMsg_ProgExecInvlProgrammer  = _T("ProgExec:%s: invalid programmer '%s'");
    public static final String EMsg_ProgExecInvldCommand    = _T("ProgExec:%s: invalid command '%s'");
    public static final String EMsg_ProgExecMsgTimeSpeed    = _T(">>> %d bytes in %.3f seconds (%.3f bytes/second) <<<");

    public static final String EMsg_ProgExecMsgChipErase    = _T("Erasing Chip");
    public static final String EMsg_ProgExecMsgReadFlash    = _T("Reading Flash");
    public static final String EMsg_ProgExecMsgWriteFlash   = _T("Writing Flash");
    public static final String EMsg_ProgExecMsgVerifyFlash  = _T("Verifying Flash");
    public static final String EMsg_ProgExecMsgReadEEPROM   = _T("Reading EEPROM");
    public static final String EMsg_ProgExecMsgWriteEEPROM  = _T("Writing EEPROM");
    public static final String EMsg_ProgExecMsgReadFuses    = _T("Reading Fuses");
    public static final String EMsg_ProgExecMsgWriteFuses   = _T("Writing Fuses");
    public static final String EMsg_ProgExecMsgReadLockBit  = _T("Reading Lock Bits");
    public static final String EMsg_ProgExecMsgWriteLockBit = _T("Writing Lock Bits");
    public static final String EMsg_ProgExecMsgProgramPE    = _T("Programming PE");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String EMsg_JDL_UnsupportedCls      = _T("deserialization of class '%s' is not supported");
    public static final String EMsg_JDL_MismatchedSVUID     = _T("mismatched 'JxMake_SerialVersionUID' (0x%016X != 0x%016X)");
    public static final String EMsg_JDL_TokenNotBoolean     = _T("cannot parse a non-boolean token into a boolean");
    public static final String EMsg_JDL_TokenNotNumber      = _T("cannot parse a non-number token into a number");
    public static final String EMsg_JDL_TokenNotString      = _T("cannot parse a non-string token into a string");
    public static final String EMsg_JDL_TokenNotNull        = _T("cannot parse a non-null token into a null");
    public static final String EMsg_JDL_TypeNotArray        = _T("cannot deserialize a non-array element into an array");
    public static final String EMsg_JDL_TypeNotObject       = _T("cannot deserialize a non-object element into an object");
    public static final String EMsg_JDL_MismatchedCls       = _T("cannot deserialize class '%s' into class '%s'");
    public static final String EMsg_JDL_FieldNotExist       = _T("cannot deserialize a non existent field '%s' into class '%s'");
  //public static final String EMsg_JDL_OnlyAryAndObjRootEl = _T("a root element that is not an array or object cannot be deserialized");
    public static final String EMsg_JDL_OnlyObjRootEl       = _T("a root element that is not an object cannot be deserialized");

    public static final String EMsg_JEL_UnsupportedCls      = _T("serialization of class '%s' is not supported");
    public static final String EMsg_JEL_OnlyNSubAndSDCSuper = _T("only non-subclasses and subclasses (of superclasses) that directly inherit from class 'jxm.SerializableDeepClone' and 'java.util.ArrayList<? extends jxm.SerializableDeepClone>' can be serialized");
    public static final String EMsg_JEL_SVUIDNotPubStaFinal = _T("JxMake_SerialVersionUID must be public, static, and final");
    public static final String EMsg_JEL_ClsWithNonPubField  = _T("classes with non-public field(s) cannot be serialized");
    public static final String EMsg_JEL_ClsWithStaticField  = _T("classes with static field(s) cannot be serialized");
    public static final String EMsg_JEL_ClsWithNonWrtFinFld = _T("classes with non-writeable final field(s) cannot be serialized");
    public static final String EMsg_JEL_ClsAryLstWithAddFld = _T("classes 'java.util.ArrayList<? extends jxm.SerializableDeepClone>' with additional fields cannot be serialized");
    public static final String EMsg_JEL_SerAoOUnsupported   = _T("serialization of an array of objects of type '%s' is not supported");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String EMsg_IDL_NoStartClassName    = _T("%s:%d: syntax error: the first non-comment line must be the section (class) name");
    public static final String EMsg_IDL_OneClassName        = _T("%s:%d: syntax error: the section (class) name can only appear once");
    public static final String EMsg_IDL_InvalidKeyValPair   = _T("%s:%d: syntax error: invalid key-value pair");
    public static final String EMsg_IDL_MismatchedClassName = _T("%s:%d: mismatched class: the INI data is for class '%s', but the expected class is '%s')");
    public static final String EMsg_IDL_MismatchedSVUID     = _T("%s:%d: mismatched JxMake_SerialVersionUID: the INI data is for version '0x%016X', but the expected version is '0x%016X')");
    public static final String EMsg_IDL_InvalidValue        = _T("%s:%d: invalid value for field '%s'");
    public static final String EMsg_IDL_InvalidValueExt     = _T("%s:%d: invalid value for field '%s': %s");

    public static final String EMsg_IEL_UnsupportedCls      = _T("serialization of class '%s' is not supported");
    public static final String EMsg_IEL_OnlyNSubAndSDCSuper = _T("only non-subclasses and subclasses (of superclasses) that directly inherit from class 'jxm.SerializableDeepClone' can be serialized");
    public static final String EMsg_IEL_SVUIDNotPubStaFinal = _T("JxMake_SerialVersionUID must be public, static, and final");
    public static final String EMsg_IEL_SVUIDNotTypeLong    = _T("JxMake_SerialVersionUID must be of type 'long'");
    public static final String EMsg_IEL_NullFieldName       = _T("null field name");
    public static final String EMsg_IEL_SerAoOUnsupported   = _T("serialization of an array of objects is not supported");
    public static final String EMsg_IEL_InvalidArrayElemTyp = _T("invalid array element type '%s'");
    public static final String EMsg_IEL_ClsWithNonPubField  = _T("classes with non-public field(s) cannot be serialized");
    public static final String EMsg_IEL_ClsWithStaticField  = _T("classes with static field(s) cannot be serialized");
    public static final String EMsg_IEL_ClsWithNonWrtFinFld = _T("classes with non-writeable final field(s) cannot be serialized");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String IMsg_JSP_GeneratedFile       = _T("/*\n"
                                                               + " * --- WARNING --- THIS IS A GENERATED FILE!\n"
                                                               + " *                 DO NOT EDIT!\n"
                                                               + " *\n"
                                                               + " * This file was generated from '%s'.\n"
                                                               + " */\n\n"                                       );

    public static final String EMsg_JSP_InvalidDirective    = _T("%s:%d: invalid directive '%s'");
    public static final String EMsg_JSP_InvalidNumOfTokens  = _T("%s:%d: invalid number of tokens for directive '%s'");
    public static final String EMsg_JSP_InvalidSymbolName   = _T("%s:%d: invalid symbol name '%s'");

    public static final String EMsg_JSP_XXXWithoutIf        = _T("%s:%d: '%s' without '#if'");
    public static final String EMsg_JSP_UnterminatedXXX     = _T("%s:%d: unterminated '%s'");

    public static final String EMsg_JSP_UndefinedSubst      = _T("%s:%d: undefined substitution marker '%s'");

    public static final String EMsg_JSP_UsingMLCUnsupported = _T("%s:%d: using multiline comments with directive lines is not supported");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String EMsg_WDriverInstallInvInfPth = _T("invalid INF path '%s'; it must be absolute and exist");
    public static final String EMsg_WDriverInstallTimeoutMN = _T("TIMEOUT: The driver installation took longer than %d minutes.\nThe driver may still be installing in the background.");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String JxMakeAboutStringText()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append( SysUtil.jxmVersionCopyright()                                                                        );
        sb.append( '\n'                                                                                                 );
        sb.append( "------------------------------------------------------------------------------------------------\n" );
        sb.append( '\n'                                                                                                 );
        sb.append( _T("JxMake contains modified code from these open source libraries:\n")                              );
        sb.append( '\n'                                                                                                 );
        sb.append( "    HTTP4J\n"                                                                                       );
        sb.append( "    https://github.com/IntellectualSites/HTTP4J\n"                                                  );
        sb.append( "        Licensed under the MIT License.\n"                                                          );
        sb.append( "        https://github.com/IntellectualSites/HTTP4J/blob/main/LICENSE\n"                            );
        sb.append( '\n'                                                                                                 );
        sb.append( "    Java Simple Magic\n"                                                                            );
        sb.append( "    https://github.com/j256/simplemagic\n"                                                          );
        sb.append( "        Licensed under the ISC License.\n"                                                          );
        sb.append( "        https://github.com/j256/simplemagic/blob/master/LICENSE.txt\n"                              );
        sb.append( '\n'                                                                                                 );
        sb.append( "    JTar\n"                                                                                         );
        sb.append( "    https://github.com/kamranzafar/jtar\n"                                                          );
        sb.append( "        Licensed under the Apache License version 2.0.\n"                                           );
        sb.append( "        https://github.com/kamranzafar/jtar/blob/master/LICENSE.txt\n"                              );
        sb.append( '\n'                                                                                                 );
        sb.append( "    Matthew J. Francis Java BZip2 Library\n"                                                        );
        sb.append( "    https://github.com/volueinsight/bzip2\n"                                                        );
        sb.append( "    https://github.com/fredcooke/maven-jbzip2\n"                                                    );
        sb.append( "    https://code.google.com/p/jbzip2\n"                                                             );
        sb.append( "        Licensed under the MIT License.\n"                                                          );
        sb.append( "        https://github.com/fredcooke/maven-jbzip2/blob/master/LICENCE\n"                            );
        sb.append( '\n'                                                                                                 );
        sb.append( "    XZ for Java\n"                                                                                  );
        sb.append( "    https://tukaani.org/xz/java.html\n"                                                             );
        sb.append( "    https://github.com/tukaani-project/xz-java\n"                                                   );
        sb.append( "    https://git.tukaani.org/?p=xz-java.git;a=summary\n"                                             );
        sb.append( "        Licensed under the BSD Zero Clause License (0BSD).\n"                                       );
        sb.append( "        https://github.com/tukaani-project/xz-java/blob/master/COPYING\n"                           );
        sb.append( '\n'                                                                                                 );
        sb.append( _T("JxMake linked against these open source libraries:\n")                                           );
        sb.append( '\n'                                                                                                 );
        sb.append( "    Jansi\n"                                                                                        );
        sb.append( "    https://fusesource.github.io/jansi\n"                                                           );
        sb.append( "    https://github.com/fusesource/jansi\n"                                                          );
        sb.append( "        Licensed under the Apache License version 2.0.\n"                                           );
        sb.append( "        https://github.com/fusesource/jansi/blob/master/license.txt\n"                              );
        sb.append( '\n'                                                                                                 );
        sb.append( "    jSerialComm - Platform-Independent Serial Port Access for Java\n"                               );
        sb.append( "    https://fazecast.github.io/jSerialComm\n"                                                       );
        sb.append( "    https://github.com/Fazecast/jSerialComm\n"                                                      );
        sb.append( "        Dual-licensed under:\n"                                                                     );
        sb.append( "            # The Apache License version 2; or\n"                                                   );
        sb.append( "            # The GNU Lesser General Public License (LGPL) version 3 or later.\n"                   );
        sb.append( "        https://github.com/Fazecast/jSerialComm/blob/master/LICENSE-APACHE-2.0\n"                   );
        sb.append( "        https://github.com/Fazecast/jSerialComm/blob/master/LICENSE-LGPL-3.0\n"                     );
        sb.append( '\n'                                                                                                 );
        sb.append( "    SpellChecker\n"                                                                                 );
        sb.append( "    https://github.com/bobbylight/SpellChecker\n"                                                   );
        sb.append( "        Licensed under the GNU Lesser General Public License (LGPL) version 2.1 or later.\n"        );
        sb.append( "        https://github.com/bobbylight/SpellChecker/blob/master/LICENSE.md\n"                        );
        sb.append( '\n'                                                                                                 );
        sb.append( "    RSTAUI\n"                                                                                       );
        sb.append( "    https://github.com/bobbylight/RSTAUI\n"                                                         );
        sb.append( "        Licensed under the Modified BSD License (BSD 3-Clause \"New\" or \"Revised\" License).\n"   );
        sb.append( "        https://github.com/bobbylight/RSTAUI/blob/master/RSTAUI/src/main/dist/RSTAUI.License.txt\n" );
        sb.append( '\n'                                                                                                 );
        sb.append( "    RSyntaxTextArea\n"                                                                              );
        sb.append( "    https://github.com/bobbylight/RSyntaxTextArea\n"                                                );
        sb.append( "        Licensed under the Modified BSD License (BSD 3-Clause \"New\" or \"Revised\" License).\n"   );
        sb.append( "        https://github.com/bobbylight/RSyntaxTextArea/blob/master/LICENSE.md\n"                     );
        sb.append( '\n'                                                                                                 );
        sb.append( "    AutoComplete\n"                                                                                 );
        sb.append( "    https://github.com/bobbylight/AutoComplete\n"                                                   );
        sb.append( "        Licensed under the Modified BSD License (BSD 3-Clause \"New\" or \"Revised\" License).\n"   );
        sb.append( "        https://github.com/bobbylight/RSyntaxTextArea/blob/master/LICENSE.md\n"                     );
        sb.append( '\n'                                                                                                 );
        sb.append( "    Java Does USB\n"                                                                                );
        sb.append( "    https://github.com/manuelbl/JavaDoesUSB\n"                                                      );
        sb.append( "        Licensed under the MIT License.\n"                                                          );
        sb.append( "        https://github.com/manuelbl/JavaDoesUSB/blob/main/LICENSE\n"                                );
        sb.append( '\n'                                                                                                 );
        sb.append( _T("JxMake loads these open source libraries dynamically (either directly or because\n"
                    + "they are dependencies of the dynamically loaded libraries):\n"                     )             );
        sb.append( '\n'                                                                                                 );
        sb.append( "    Pty4J - Pseudo Terminal (PTY) Implementation in Java\n"                                         );
        sb.append( "    https://github.com/JetBrains/pty4j\n"                                                           );
        sb.append( "        Licensed under the Eclipse Public License 1.0.\n"                                           );
        sb.append( "        https://github.com/JetBrains/pty4j/blob/master/LICENSE\n"                                   );
        sb.append( '\n'                                                                                                 );
        sb.append( "    Annotations for JVM-based Languages\n"                                                          );
        sb.append( "    https://github.com/JetBrains/java-annotations\n"                                                );
        sb.append( "        Licensed under the Apache License version 2.0.\n"                                           );
        sb.append( "        https://github.com/JetBrains/java-annotations/blob/master/LICENSE.txt\n"                    );
        sb.append( '\n'                                                                                                 );
        sb.append( "    Kotlin Programming Language\n"                                                                  );
        sb.append( "    https://github.com/JetBrains/kotlin\n"                                                          );
        sb.append( "        Licensed under the Apache License version 2.0.\n"                                           );
        sb.append( "        https://github.com/JetBrains/kotlin/blob/master/license/README.md\n"                        );
        sb.append( '\n'                                                                                                 );
        sb.append( "    SLF4J\n"                                                                                        );
        sb.append( "    https://github.com/qos-ch/slf4j\n"                                                              );
        sb.append( "        Licensed under the MIT License.\n"                                                          );
        sb.append( "        https://github.com/qos-ch/slf4j/blob/master/LICENSE.txt\n"                                  );
        sb.append( '\n'                                                                                                 );
        sb.append( "    Java Native Access (JNA)\n"                                                                     );
        sb.append( "    https://github.com/java-native-access/jna\n"                                                    );
        sb.append( "        Dual-licensed under:\n"                                                                     );
        sb.append( "            # The Apache License version 2; or\n"                                                   );
        sb.append( "            # The GNU Lesser General Public License (LGPL) version 2.1 or later.\n"                 );
        sb.append( "        https://github.com/java-native-access/jna/blob/master/AL2.0\n"                              );
        sb.append( "        https://github.com/java-native-access/jna/blob/master/LGPL2.1\n"                            );

        return sb.toString();
    }

    public static void printUsageHelp(
        final String   errMsg           ,
        final String[] progCmd          ,
        final String   defaultSpecFile  ,
        final String   extractDocDir    ,
        final String   extractLibDir    ,
        final String   extractABrdDecDir,
        final String   tmpDirRoot
    )
    {
        final String[] ufs = new String[] {
                                 SysUtil._JxMakeLanguageCountry  ,
                                 SysUtil._JxMakeLanguageCountryFB,
                                 SysUtil._FallbackLanguageCountry
                             };

        String textWithFormat = null;

        for(final String uf : ufs) {
            try {
                textWithFormat = SysUtil.loadTextResource( SysUtil.getUsageHelpFileURL_defLoc(uf) );
                if(textWithFormat != null) break;
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        }

                           SysUtil.stdOut().println();
        if(errMsg != null) SysUtil.stdOut().printf( _T("\nJxMake USAGE ERROR:\n    %s\n\n") , errMsg );
                           SysUtil.stdOut().println();

        SysUtil.stdOut().println( _T("Usage:") );
        int len = 0;
        for(final String pc : progCmd) len = Math.max( len, pc.length() );
        for(final String pc : progCmd) SysUtil.stdOut().printf("    %-" + len + "s [options] [target]... [var_name=value]...\n", pc);
        SysUtil.stdOut().println();
        SysUtil.stdOut().println();

        SysUtil.stdOut().printf(
            textWithFormat,
            defaultSpecFile,
            extractDocDir,
            extractLibDir,
            SysUtil.ArduinoBoardsTxtDecRuleFile,
            extractABrdDecDir,
            tmpDirRoot
        );
        SysUtil.stdOut().println();
        SysUtil.stdOut().println();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String KVT_CheckAll      = _T("Check All Boxes");
    public static final String KVT_UncheckAll    = _T("Uncheck All Boxes");
    public static final String KVT_InvertChecked = _T("Invert Checked Boxes");

    public static final String KVT_AddRow        = _T("Add Row");
    public static final String KVT_DelRow        = _T("Delete Current Row");
    public static final String KVT_DelRows       = _T("Delete Selected Rows");

    public static final String KVT_FilterKey     = _T("Filter by %s: ");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String SerCon_Err_NoTarget   = _T("no target specified");

    public static final String SerCon_Stt_Start      = _T("Start Data Acquiring");
    public static final String SerCon_Stt_Stop       = _T("Stop Data Acquiring");

    public static final String SerCon_ResetInfoMsg1  = _T("\n\nTrying to reset the attached MCU ...\n"
                                                        + "> It may not work for the Thinary ATmega4808 board due to a design\n"
                                                        + "> bug from the manufacturer!\n"                                      );
    public static final String SerCon_ResetInfoMsg2  = _T("\nIt will take a while for the MCU to finish resetting, especially\n"
                                                        + "for Arduino Mega 2560 boards; please be patient ...\n\n"             );

    public static final String SerCon_CNA_SerPort    = _T("SERIAL PORT");
    public static final String SerCon_CNA_TCPSerBrg  = _T("TCP <-> SERIAL BRIDGE");
    public static final String SerCon_CNA_NotConn    = _T("((( THE %s IS NOT CONNECTED! )))");

    public static final String SerCon_PNGImages      = _T("PNG Images (*.png)");
    public static final String SerCon_SaveImage      = _T("Save Image");
    public static final String SerCon_SaveImageAs    = _T("Save Image As");

    public static final String SerCon_ModeTitleP     = _T("Plotter");
    public static final String SerCon_ModeTitleC     = _T("Console");
    public static final String SerCon_WinTitleTTY    = _T("TTY Serial %s");
    public static final String SerCon_WinTitleTCP    = _T("TCP Serial %s");
    public static final String SerCon_ErrorTitle     = _T("Error");

    public static final String SerCon_TurnOnChannel  = _T("Turn On Channel #%d");
    public static final String SerCon_TurnOffChannel = _T("Turn Off Channel #%d");

    public static final String SerCon_XAxisVWPos     = _T("X Axis (View Window) Position");
    public static final String SerCon_XAxisVWSize    = _T("X Axis (View Window) Size");
    public static final String SerCon_YAxisScale     = _T("Y Axis Scale");

    public static final String SerCon_BtnCopy        = _T("Copy Output");
    public static final String SerCon_BtnCopyTT      = _T("Copy the Text Output to Clipboard");

    public static final String SerCon_BtnClear       = _T("Clear Output");
    public static final String SerCon_BtnClearTT     = _T("Clear the Text Output");

    public static final String SerCon_BtnReset       = _T("Try Reset MCU");
    public static final String SerCon_BtnResetTT     = _T("Try to Reset the Attached MCU Board");

    public static final String SerCon_TxtInputPHText = _T("Type message and press Enter to send");

    public static final String SerCon_SendMsgLnEMode = _T("Line-Ending Mode when Sending Messages");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String DocBro_Title    = _T("Documentation Browser");
    public static final String DocBro_BtnClose = _T("Close Window and Exit");

    public static final String DocBro_ErrNoDoc = _T("failed to get the list of documentation files");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String     HR                   = "<hr>";
    private static final String     BR1                  = "<br>";
    private static final String     BR2                  = "<br><br>";
    private static final String     HTML_BEGIN           = "<html><body>";
    private static final String     HTML_END             = BR2 + "</body></html>";

    private static final String     HTML_FONT_SMALL_B    = "<span style='font-size:80%'>";
    private static final String     HTML_FONT_SMALL_E    = "</span>";

    public  static final String     ScrEdt_FLDoNotModify = _T("# JxMake Script Editor Saved File State\n"
                                                             + "# DO NOT MODIFY THIS FILE!\n"
                                                             + "#\n\n"                                   );

    public  static final String     ScrEdt_EVDoNotModify = _T("# JxMake Script Editor Environment Variable State\n"
                                                            + "# DO NOT MODIFY THIS FILE!\n"
                                                            + "#\n\n"                                              );

    public  static final String     ScrEdt_Title         = _T("Script Editor");
    public  static final String     ScrEdt_About         = _T("About JxMake");

    public  static final String     ScrEdt_OpnRecMScript = _T("Open Recent Main Script");
    public  static final String     ScrEdt_OpnRecBtnClrL = _T("Clear List");

    public  static final String     ScrEdt_SelectDict    = _T("Select Dictionary");

    public  static final String     ScrEdt_Error         = _T("Error");
    public  static final String     ScrEdt_Information   = _T("Information");
    public  static final String     ScrEdt_Warning       = _T("Warning");
    public  static final String     ScrEdt_Question      = _T("Question");

    public  static final String     ScrEdt_Console       = _T("Console");
    public  static final String     ScrEdt_Untitled      = _T("<untitled>");

    public  static final String     ScrEdt_KSCTitle      = _T("Keyboard Shortcuts");
    public  static final String     ScrEdt_KSCBtnClose   = _T("Close Window and Exit");

    public  static final String     ScrEdt_EVETitle      = _T("Environment Variables");
    public  static final String     ScrEdt_EVESysTitle   = _T("System Environment Variables");
    public  static final String     ScrEdt_EVEUsrTitle   = _T("User Environment Variables");
    public  static final String     ScrEdt_EVEVarName    = _T("Name");
    public  static final String     ScrEdt_EVEVarValue   = _T("Value");
    public  static final String     ScrEdt_EVEDupKey     = HTML_BEGIN
                                                         + _T("Another key with the same name already exists.") + BR2
                                                         + _T("Do you want to overwrite its value with this one?")
                                                         + HTML_END;
    public  static final String     ScrEdt_EVEOK         = _T("&OK");
    public  static final String     ScrEdt_EVECancel     = _T("&Cancel");
    public  static final String     ScrEdt_EVESaveAsDef  = _T("&Save as Default");
    public  static final String     ScrEdt_EVEResetToDef = _T("&Reset to Default");

    public  static final String     ScrEdt_CCTitle       = _T("Command Composer");
    public  static final String     ScrEdt_CCCommand     = _T("Command: ");
    public  static final String     ScrEdt_CCPreview     = _T("Preview");
    public  static final String     ScrEdt_CCNoArg       = HTML_BEGIN
                                                         + "<h2><i>" + _T("This command has no arguments.") + "</i></h2>"
                                                         + HTML_END;
    public  static final String     ScrEdt_CCOptions     = _T("Arguments");
    public  static final String     ScrEdt_CCNotUsed     = _T("N/A");
    public  static final String     ScrEdt_CCPaste       = _T("&Paste");
    public  static final String     ScrEdt_CCPasteExec   = _T("Paste and E&xecute");
    public  static final String     ScrEdt_CCClose       = _T("&Close");

    public  static final String     ScrEdt_CCBlkListOSC  = _T("Execution of the OS command '%s' is not permitted");
    public  static final String     ScrEdt_CCErrMissOpt  = _T("%s: missing option(s)");
    public  static final String     ScrEdt_CCErrTManyOpt = _T("%s: too many options");
    public  static final String     ScrEdt_CCErrInvlOpt  = _T("%s: invalid option '%s'");
    public  static final String     ScrEdt_CCErrCnflOpt  = _T("%s: conflicting option '%s'");
    public  static final String     ScrEdt_CCErrInvlVarN = _T("invalid variable name '%s'");
    public  static final String     ScrEdt_CCErrRsrvVarN = _T("reserved variable name '%s'");
    public  static final String     ScrEdt_CCErrUnsupCmd = _T("%s: unsupported command '%s'");
    public  static final String     ScrEdt_CCErrStxError = _T("%s: command syntax error '%s'");

    public  static final String     ScrEdt_FindNone      = _T("Text not found");
    public  static final String     ScrEdt_FindMarked    = _T("Text found; occurrences marked: ");

    public  static final String     ScrEdt_tlbCmdCompose = _T("Command Composer");

    public  static final String     ScrEdt_tlbCopyWLW    = _T("<html>Copy as Plain Text without Line Wraps (<i>%s</i>)</html>");
    public  static final String     ScrEdt_tlbCopyAST    = _T("<html>Copy as Styled Text (<i>%s</i>)</html>");
    public  static final String     ScrEdt_tlbSndEOF     = _T("<html>Send End of File (<i>%s</i>)</html>");
    public  static final String     ScrEdt_tlbSndSIGINT  = _T("<html>Send Interrupt <b>^C</b> (<i>%s</i>)</html></html>");
    public  static final String     ScrEdt_tlbSndSIGKILL = _T("<html>Terminate Process (<i>%s</i>)");
    public  static final String     ScrEdt_tlbEnvVars    = _T("Environment Variables");
    public  static final String     ScrEdt_tlbFSizeInc   = _T("Increase Font Size");
    public  static final String     ScrEdt_tlbFSizeDec   = _T("Decrease Font Size");
    public  static final String     ScrEdt_tlbFSizeRst   = _T("Reset Font Size");
    public  static final String     ScrEdt_tlbTermReset  = _T("<html>Terminal Reset (<b>%s</b>)</html>");

    public  static final String     ScrEdt_ProcExitCode0 = _T("%s>>> [Process exited with code %d]%s\n");
    public  static final String     ScrEdt_ProcExitCodeN = _T("%s>>> [Process exited with code %d]%s\n");

    public  static final String     ScrEdt_NoTextToPrint = _T("There is no text to print.");

    public  static final String     ScrEdt_NoPrintAllCon = HTML_BEGIN
                                                         + _T("Cannot print the entire console.") + BR2
                                                         + _T("Please select some text first.")
                                                         + HTML_END;

    public  static final String     ScrEdt_ConsoleNA     = HTML_BEGIN
                                                         + "<div style='text-align:center;'>"
                                                         + _T("The console feature is disabled"             + BR1
                                                            + "because neither the required Pty4J library"  + BR1
                                                            + "nor the built-in Java ProcessBuilder"        + BR1
                                                            + "is available."                             ) + BR2
                                                         + "<i>" + _T("Please adjust your classpath,"       + BR1
                                                                    + "verify your installed libraries,"    + BR1
                                                                    + "or review your Java installation." )
                                                         + "</i>"
                                                         + "</div>"
                                                         + HTML_END;

    public  static final String     ScrEdt_OK_NoSaveAll  = HTML_BEGIN
                                                         + _T("Cannot save all files because the main script"  + BR1
                                                            + "file has no file path associated with it."    )
                                                         + HTML_END;

    public  static final String     ScrEdt_OK_NoReload   = HTML_BEGIN
                                                         + _T("Cannot reload the current file because"   + BR1
                                                            + "it has no file path associated with it.")
                                                         + HTML_END;

    private static final String     MSG_FILES_MODIFIED   = _T("One or more of the open files have been modified.");
    private static final String     MSG_FILE_MODIFIED    = _T("This file has been modified.");

    private static final String     WARN_DISCARD_FILE    = "<i>" + _T("Continuing will discard any changes made to the file!")      + "</i>";
    private static final String     WARN_DISCARD_FILES   = "<i>" + _T("Continuing will discard any changes made to the file(s)!")   + "</i>";
    private static final String     WARN_OVERWRITE_FILE  = "<i>" + _T("Continuing will overwrite any changes made to the file!")    + "</i>";
    private static final String     WARN_OVERWRITE_FILES = "<i>" + _T("Continuing will overwrite any changes made to the file(s)!") + "</i>";

    private static final String     MSG_SAVE_AS_MAIN_SF  = _T("Saving the main script file under a different name will reload all the"  + BR1
                                                            + "include file(s)."                                                      ) + BR2
                                                         + _T("Do you still want to save the main script file with a different name?");

    public  static final String     ScrEdt_YN_CrtNIFile  = HTML_BEGIN
                                                         + _T("The include file \"%s\" could not be found at \"%s\".") + BR2
                                                         + _T("Would you like to create a new empty file for it?")
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_ModByExt   = HTML_BEGIN
                                                         + _T("This file has been modified by an external program or process.") + BR2
                                                         + _T("Do you still want to save it?") + BR2
                                                         + WARN_OVERWRITE_FILE
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_ModByExtSS = HTML_BEGIN
                                                         + _T("One or more of the open files have been modified by an external") + BR2
                                                         + _T("program or process. " + "Do you still want to save them?") + BR2
                                                         + WARN_OVERWRITE_FILES
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_ModNewSS   = HTML_BEGIN
                                                         + MSG_FILES_MODIFIED + BR2
                                                         + _T("Do you still want to create a new main script file?") + BR2
                                                         + WARN_DISCARD_FILES
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_SAsMainNfo = HTML_BEGIN
                                                         + MSG_SAVE_AS_MAIN_SF
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_SAsMainMod = HTML_BEGIN
                                                         + MSG_FILES_MODIFIED + BR2
                                                         + MSG_SAVE_AS_MAIN_SF + BR2
                                                         + WARN_DISCARD_FILES
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_SAsFileNfo = HTML_BEGIN
                                                         + _T("Saving the script file under a different name will not make it"  + BR1
                                                            + "appear in the file tree unless it is also included by another"   + BR1
                                                            + "script file."                                                  ) + BR2
                                                         + _T("Do you still want to save the script file with "
                                                            + "a different name?"                              )
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_SAsFileOvr = HTML_BEGIN
                                                         + _T("A file with the same name already exists.") + BR2
                                                         + _T("Do you want to overwrite it?")
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_ModLoadSS  = HTML_BEGIN
                                                         + MSG_FILES_MODIFIED + BR2
                                                         + _T("Do you still want to load a new main script file?") + BR2
                                                         + WARN_DISCARD_FILES
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_ModReload  = HTML_BEGIN
                                                         + MSG_FILE_MODIFIED + BR2
                                                         + _T("Do you still want to reload it?") + BR2
                                                         + WARN_DISCARD_FILE
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_ModRldMnSS = HTML_BEGIN
                                                         + MSG_FILES_MODIFIED + BR2
                                                         + _T("Do you still want to reload the main script file?") + BR2
                                                         + WARN_DISCARD_FILES
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_ModQuitSS  = HTML_BEGIN
                                                         + MSG_FILES_MODIFIED + BR2
                                                         + _T("Are you sure you want to quit the editor?") + BR2
                                                         + WARN_DISCARD_FILES
                                                         + HTML_END;

    public  static final String     ScrEdt_YN_CmdCWClose = HTML_BEGIN
                                                         + _T("The command and/or its arguments have been modified.") + BR2
                                                         + _T("Are you sure you want to close the command composer window?")
                                                         + HTML_END;

    public static String ScrEdt_OverlayText(final boolean usePty4J)
    {
        return   HTML_BEGIN

               + "<b>" + _T("Shell Console for JxMake Script Editor") + "</b>"
               + BR2

               + HTML_FONT_SMALL_B
               + XCom.escapeHTML( SysUtil.jxmVersionCopyright() )
               + HTML_FONT_SMALL_E
               + BR1

               + "<b>" + ( (usePty4J) ? _T("Using Pty4J's ProcessBuilder.")
                                      : _T("Using Java's native ProcessBuilder.") + BR1 + _T("Some programs may produce output in a simpler format.")
                         )
               + "</b>"
               + BR2

               + HTML_FONT_SMALL_B
               + _T("This Shell Console offers a straightforward, minimalistic implementation focused on"      + BR1
                  + "essential functionality. It currently supports only a small set of features and built‑in" + BR1
                  + "commands. Any command not recognized as a built‑in is passed directly to the system and"  + BR1
                  + "executed as an external program."                                                        )
               + HTML_FONT_SMALL_E
               + BR2

               + "<b><i>" + _T("This overlay message will automatically disappear once the console is" + BR1
                             + "no longer empty"                                                      )
               + ".</i></b>"
               + HTML_END;
    }


    public static final String   ScrEdt_Examples = _T("Examples:");

    public static final String   ScrEdt_dlNA     = _T("Not Available");
    public static final String   ScrEdt_dlfmtSep = "|";
    public static final String   ScrEdt_dlfmtStr = "%-19s" + ScrEdt_dlfmtSep + "%-16s (%-13s) ⇐ %s";
    public static final String[] ScrEdt_dictList = new String[] {
        "DefaultEnglish_auto|Default English  (Automatic    ) ⇐ https://github.com/bobbylight/SpellChecker/tree/master/SpellChecker/src/main/dist/english_dic",
        "DefaultEnglish_US  |Default English  (United States) ⇐ https://github.com/bobbylight/SpellChecker/tree/master/SpellChecker/src/main/dist/english_dic",
        "DefaultEnglish_GB  |Default English  (Great Britain) ⇐ https://github.com/bobbylight/SpellChecker/tree/master/SpellChecker/src/main/dist/english_dic"
    };

    private static final MenuSpec[] ScrEdt_mnuFile       = new MenuSpec[] {
        new MenuSpec(ID_file          ,                     _T("File")                      , MenuSpec.VK_F                   , "#handle_mnuFile"          ),
      //---
        new MenuSpec(null             , "file_new"        , _T("New Main Script")           , MenuSpec.VK_N, KS_fileNew       , "#handle_mnuFileNew"       ),
        new MenuSpec(null             , "file_open"       , _T("Open Main Script...")       , MenuSpec.VK_O, KS_fileOpen      , "#handle_mnuFileOpen"      ),
        new MenuSpec(ID_fileOpenRecent, "file_open_recent", _T("Open Recent Main Script..."), MenuSpec.VK_R, KS_fileOpenRecent, "#handle_mnuFileOpenRecent"),
      /***/ MenuSpec.Separator                                                                                                                          ,
        new MenuSpec(null             , "file_save"       , _T("Save")                      , MenuSpec.VK_S, KS_fileSave      , "#handle_mnuFileSave"      ),
        new MenuSpec(null             , "file_save_as"    , _T("Save As...")                , MenuSpec.VK_A, KS_fileSaveAll   , "#handle_mnuFileSaveAs"    ),
        new MenuSpec(null             , "file_save_all"   , _T("Save All")                  , MenuSpec.VK_L, KS_fileSaveAs    , "#handle_mnuFileSaveAll"   ),
      /***/ MenuSpec.Separator                                                                                                                          ,
        new MenuSpec(null             , "file_reload"     , _T("Reload")                    , MenuSpec.VK_D                   , "#handle_mnuFileReload"    ),
        new MenuSpec(ID_filePrintFmt  , "file_print_fmt"  , _T("Print with Style...")       , MenuSpec.VK_P                   , "#handle_mnuFilePrintFmt"  ),
        new MenuSpec(ID_filePrintTxt  , "file_print_txt"  , _T("Print as Plain Text...")    , MenuSpec.VK_T                   , "#handle_mnuFilePrintTxt"  ),
      /***/ MenuSpec.Separator                                                                                                                          ,
        new MenuSpec(ID_fileQuit      , "file_quit"       , _T("Quit")                      , MenuSpec.VK_Q, KS_fileQuit      , "#handle_mnuFile_Quit"     ),
            null
    };

    private static final MenuSpec[] ScrEdt_mnuEdit       = new MenuSpec[] {
        new MenuSpec(ID_edit     ,                   _T("Edit")               , MenuSpec.VK_E                     , "#handle_mnuEdit"          ),
      //---
        new MenuSpec(ID_editUndo , "edit_undo"     , _T("Undo")               , MenuSpec.VK_U, KS_editUndo        , "#handle_mnuEdit_Undo"     ),
        new MenuSpec(ID_editRedo , "edit_redo"     , _T("Redo")               , MenuSpec.VK_D, KS_editRedo        , "#handle_mnuEdit_Redo"     ),
      /***/ MenuSpec.Separator                                                                                                                  ,
        new MenuSpec(ID_editClear, "edit_clear"    , _T("Clear")              , MenuSpec.VK_L                     , "#handle_mnuEdit_Clear"    ),
        new MenuSpec(ID_editCut  , "edit_cut"      , _T("Cut")                , MenuSpec.VK_T, KS_editCut         , "#handle_mnuEdit_Cut"      ),
        new MenuSpec(ID_editCopy , "edit_copy"     , _T("Copy")               , MenuSpec.VK_C, KS_editCopy        , "#handle_mnuEdit_Copy"     ),
        new MenuSpec(ID_editPaste, "edit_paste"    , _T("Paste")              , MenuSpec.VK_P, KS_editPaste       , "#handle_mnuEdit_Paste"    ),
      /***/ MenuSpec.Separator                                                                                                                  ,
        new MenuSpec(null        , "edit_copy_ast" , _T("Copy as Styled Text"), MenuSpec.VK_S, KS_copyAsStyledText, Action_copyAsStyledText    ),
      /***/ MenuSpec.Separator                                                                                                                  ,
        new MenuSpec(null        , "edit_indent"   , _T("Indent")             , MenuSpec.VK_I, KS_editIndent      , "#handle_mnuEdit_Indent"   ),
        new MenuSpec(null        , "edit_unindent" , _T("Unindent")           , MenuSpec.VK_U, KS_editUnindent    , "#handle_mnuEdit_Unindent" ),
      /***/ MenuSpec.Separator                                                                                                                  ,
        new MenuSpec(null        , "edit_comment"  , _T("Comment Line(s)")    , MenuSpec.VK_M, KS_editComment     , "#handle_mnuEditComment"   ),
        new MenuSpec(null        , "edit_uncomment", _T("Uncomment Line(s)")  , MenuSpec.VK_N, KS_editUncomment   , "#handle_mnuEditUncomment" ),
      /***/ MenuSpec.Separator                                                                                                                  ,
        new MenuSpec(null        , "edit_find"     , _T("Find...")            , MenuSpec.VK_F, KS_editFind        , "#handle_mnuEditFind"      ),
        new MenuSpec(null        , "edit_replace"  , _T("Replace...")         , MenuSpec.VK_R, KS_editReplace     , "#handle_mnuEditReplace"   ),
            null
    };

    private static final MenuSpec[] ScrEdt_mnuView       = new MenuSpec[] {
        new MenuSpec(ID_view        ,                    _T( "View")                   , MenuSpec.VK_V                                                     ),
      //---
        new MenuSpec(null           , "view_rehilite"  , _T( "Re-highlight All")       , MenuSpec.VK_H, KS_rehighlightAll  , Action_rehighlightAll         ),
      /***/ MenuSpec.Separator                                                                                                                              ,
        new MenuSpec(ID_viewLineWrap, "view_line_wrap" , _T("*Line Wrap")              , MenuSpec.VK_W, KS_toggleLineWrap  , Action_toggleLineWrap         ),
      /***/ MenuSpec.Separator                                                                                                                              ,
        new MenuSpec(null           , "view_fold_all"  , _T( "Fold All Nodes")         , MenuSpec.VK_F, KS_viewFoldAll     , "#handle_mnuView_FoldAll"     ),
        new MenuSpec(null           , "view_fold"      , _T( "Fold Nodes in Level 1")                                      , "#handle_mnuView_FoldLevel1"  ),
        new MenuSpec(null           , "view_fold"      , _T( "Fold Nodes in Level 2")                                      , "#handle_mnuView_FoldLevel2"  ),
        new MenuSpec(null           , "view_fold"      , _T( "Fold Nodes in Level 3")                                      , "#handle_mnuView_FoldLevel3"  ),
      /***/ MenuSpec.Separator                                                                                                                              ,
        new MenuSpec(null           , "view_unfold_all", _T( "Unfold All Nodes")       , MenuSpec.VK_U, KS_viewUnfoldAll   , "#handle_mnuView_UnfoldAll"   ),
        new MenuSpec(null           , "view_unfold"    , _T( "Unfold Nodes in Level 1")                                    , "#handle_mnuView_UnfoldLevel1"),
        new MenuSpec(null           , "view_unfold"    , _T( "Unfold Nodes in Level 2")                                    , "#handle_mnuView_UnfoldLevel2"),
        new MenuSpec(null           , "view_unfold"    , _T( "Unfold Nodes in Level 3")                                    , "#handle_mnuView_UnfoldLevel3"),
      /***/ MenuSpec.Separator                                                                                                                              ,
        new MenuSpec(null           , "view_inc_fsize" , _T( "Decrease Font Size")     , MenuSpec.VK_D, KS_decreaseFontSize, Action_decreaseFontSize       ),
        new MenuSpec(null           , "view_dec_fsize" , _T( "Increase Font Size")     , MenuSpec.VK_I, KS_increaseFontSize, Action_increaseFontSize       ),
        new MenuSpec(null           , "view_rst_fsize" , _T( "ResetFont Size")         , MenuSpec.VK_R, KS_resetFontSize   , Action_resetFontSize          ),
      /***/ MenuSpec.Separator                                                                                                                              ,
        new MenuSpec(ID_viewSpellChk, "view_spl_chk"   , _T("*Spell Checking")         , MenuSpec.VK_S, KS_viewSpellChk    , "#handle_mnuViewSpellCheck"   ),
        new MenuSpec(ID_viewChgDict , "view_chg_dic"   , _T( "Change Dictionary...")   , MenuSpec.VK_C, KS_viewChgDict     , "#handle_mnuViewChangeDict"   ),
            null
    };

    private static final MenuSpec[] ScrEdt_mnuHelp       = new MenuSpec[] {
        new MenuSpec(ID_help        ,                  _T("Help")                 , MenuSpec.VK_H                             ),
      //---
        new MenuSpec(null           , "help_kb_scuts", _T("Keyboard Shortcuts")   , MenuSpec.VK_S, "#handle_mnuHelpKbSCuts"   ),
        new MenuSpec(null           , "help_show_doc", _T("Documentation Browser"), MenuSpec.VK_D, "#handle_mnuHelpDocBrowser"),
      /***/ MenuSpec.Separator                                                                                                 ,
        new MenuSpec(ID_helpAbout   , "help_about"   , _T("About")                , MenuSpec.VK_A, "#handle_mnuHelpAbout"     ),
            null
    };

    public static final List< XCom.Pair<MenuSpec[], String> > ScrEdt_MenuSpecs = Arrays.asList(
        new XCom.Pair<>(ScrEdt_mnuFile, "ScrEdt_mnuFile"),
        new XCom.Pair<>(ScrEdt_mnuEdit, "ScrEdt_mnuEdit"),
        new XCom.Pair<>(ScrEdt_mnuView, "ScrEdt_mnuView"),
        null                                             ,
        new XCom.Pair<>(ScrEdt_mnuHelp, "ScrEdt_mnuHelp")
    );

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String USBISS_InitFailAPIM   = _T("USB-ISS: initialization failed (cannot set all pins to input mode)");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String PDevXXX_DevTimeoutCSt = _T("%s: timeout while waiting for the device to change state");
    public static final String PDevXXX_TxTimeout     = _T("%s: tx timeout");
    public static final String PDevXXX_RxTimeout     = _T("%s: rx timeout");
    public static final String PDevXXX_RxOverflow    = _T("%s: rx overflow");
    public static final String PDevXXX_ParityError   = _T("%s: parity error");
    public static final String PDevXXX_StopBit1Error = _T("%s: stop bit #1 error");
    public static final String PDevXXX_StopBit2Error = _T("%s: stop bit #2 error");
    public static final String PDevXXX_SystemError   = _T("%s: system error: %s");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String ProgXXX_FailInitPCF8574   = _T("%s: failed to initialize mode selection (PCF8574)");
    public static final String ProgXXX_FailUninitPCF8574 = _T("%s: failed to uninitialize mode selection (PCF8574)");

    public static final String ProgXXX_FailSelBBSPI      = _T("%s: failed to select bit banging SPI mode");
    public static final String ProgXXX_FailSelHWSPI      = _T("%s: failed to select hardware SPI mode");
    public static final String ProgXXX_FailInitSPI       = _T("%s: failed to initialize SPI");
    public static final String ProgXXX_FailUninitSPI     = _T("%s: failed to uninitialize SPI");
    public static final String ProgXXX_FailSPIConfig     = _T("%s: failed to set SPI configuration");

    public static final String ProgXXX_FailSelBBUSRT     = _T("%s: failed to select bit banging USRT mode");
    public static final String ProgXXX_FailSelHWUSRT     = _T("%s: failed to select hardware USRT mode");
    public static final String ProgXXX_FailInitUSRT      = _T("%s: failed to initialize USRT");
    public static final String ProgXXX_FailUninitUSRT    = _T("%s: failed to uninitialize USRT");
    public static final String ProgXXX_FailUSRTConfig    = _T("%s: failed to set USRT configuration");

    public static final String ProgXXX_FailSelBBUART     = _T("%s: failed to select bit banging UART mode");
    public static final String ProgXXX_FailSelHWUART     = _T("%s: failed to select hardware UART mode");
    public static final String ProgXXX_FailInitUART      = _T("%s: failed to initialize UART");
    public static final String ProgXXX_FailUninitUART    = _T("%s: failed to uninitialize UART");
    public static final String ProgXXX_FailUARTConfig    = _T("%s: failed to set UART configuration");

    public static final String ProgXXX_FailInitSWIM      = _T("%s: failed to initialize SWIM");
    public static final String ProgXXX_FailUninitSWIM    = _T("%s: failed to uninitialize SWIM");

    public static final String ProgXXX_FailInitJTAG      = _T("%s: failed to initialize JTAG");
    public static final String ProgXXX_FailUninitJTAG    = _T("%s: failed to uninitialize JTAG");

    public static final String ProgXXX_FailInitSerDev    = _T("%s: failed to initialize the serial device");

    public static final String ProgXXX_FailConnectBLTout = _T("%s: failed to connect to the bootloader - timeout");
    public static final String ProgXXX_FailInvalidBLIVal = _T("%s: failed to connect to the bootloader - invalid response");
    public static final String ProgXXX_FailBLComRxTrunc  = _T("%s: bootloader communication failure - truncated reception");
    public static final String ProgXXX_FailBLComFrameErr = _T("%s: bootloader communication failure - frame error");
    public static final String ProgXXX_FailBLComCRCTxErr = _T("%s: bootloader communication failure - CRC error during transmission");
    public static final String ProgXXX_FailBLComCRCRxErr = _T("%s: bootloader communication failure - CRC error during reception");
    public static final String ProgXXX_FailBLComInvHead  = _T("%s: bootloader communication failure - invalid header");
    public static final String ProgXXX_FailBLComInv485Ad = _T("%s: bootloader communication failure - invalid RS485 address");

    public static final String ProgXXX_USBDisconnectTout = _T("%s: timeout while waiting for the USB device to disconnect");

    public static final String ProgXXX_FailInitPrgDev    = _T("%s: failed to initialize the programmer device");
    public static final String ProgXXX_FailUninitPrgDev  = _T("%s: failed to uninitialize the programmer device");

    // ===== BEG : Static and dynamic text for bootloader-related messages =====
    public static final String ProgXXX_FailBLPrgCmdXErr  = _T("%s: command execution error");
    public static final String ProgXXX_FailBLExit        = _T("%s: failed to exit bootloader mode");

    private static String _ProgXXX_FailBLXErrI_impl(final String fmtBase, final String fmtDetail, final Object... args)
    { return fmtBase + String.format(fmtDetail + "\n", args); }

    public static String ProgXXX_FailBLPrgCmdXErrI(final String fmtDetail, final Object... args)
    { return _ProgXXX_FailBLXErrI_impl( _T("%s: command execution error: "), fmtDetail, args); }

    public static String ProgXXX_FailBLResultXErrI(final String fmtDetail, final Object... args)
    { return _ProgXXX_FailBLXErrI_impl( _T("%s: command result error: "), fmtDetail, args); }

    public static final String CmdXErr_BLStartAdrUnresol = _T("the bootloader start address could not be resolved");
    public static final String CmdXErr_BLInvalidValue    = _T("invalid '%s' value");

    public static final String CmdXErr_BLTimeout         = _T("command execution timeout");
    public static final String CmdXErr_BLInvalidSegDec   = _T("invalid segment description '%s'");
    public static final String CmdXErr_BLInvalidState    = _T("invalid state '%s'; expected '%s'");
    public static final String CmdXErr_BLInvalidStatus   = _T("invalid status '%s'; expected '%s'");
    public static final String CmdXErr_BLInvalidStaSts   = _T("invalid state/status '%s/%s'; expected '%s/%s'");
    public static final String CmdXErr_BLInvalidStsSta   = _T("invalid status/state '%s/%s'; expected '%s/%s'");
    public static final String CmdXErr_BLInvalidAddrNA   = _T("invalid address - memory at 0x%08X does not exist");
    public static final String CmdXErr_BLInvalidAddrWr   = _T("invalid address - memory at 0x%08X is not writable");
    public static final String CmdXErr_BLInvalidAddrEr   = _T("invalid address - memory at 0x%08X is not erasable");
    public static final String CmdXErr_BLInvalidDBlockSz = _T("invalid data block size %d; expected %d");

    // ----- ----- ----- ----- -----

    public static final String ProgXXX_InfoBLMetadataBeg = _T("\n------------------------------------------------------------\n"
                                                            + "[%s]\nBootloader metadata currently stored in MCU:\n"            );
    public static final String ProgXXX_InfoBLMetadataEnd = _T("⚠ Subject to change via chip erase & flash write.\n"
                                                            + "------------------------------------------------------------\n"  );
    public static String ProgXXX_InfoBLMSpecific(final String spaces, final int nameWidth, final String name, final String fmtDetail)
    { return String.format(spaces + "%-" + nameWidth + "s = %s\n", name, fmtDetail); }

    public static String ProgXXX_InfoBLMSpecific(final int nameWidth, final String name, final String fmtDetail)
    { return ProgXXX_InfoBLMSpecific("    ", nameWidth, name, fmtDetail); }

    public static final String CmdXInf_BLAddress         = _T("Address");
    public static final String CmdXInf_BLPStrStart       = _T("PStr-Start");
    public static final String CmdXInf_BLPStrSize        = _T("PStr-Size");
    public static final String CmdXInf_BLUplDate         = _T("Upload-Date");
    public static final String CmdXInf_BLDesc            = _T("Description");

    public static final String CmdXInf_BLBuildDate       = _T("Build-Date");
    public static final String CmdXInf_BLBuildState      = _T("Build-State");
    public static final String CmdXInf_BLFlashPageSize   = _T("Flash-Page-Size");
    public static final String CmdXInf_BLEEPROMSize      = _T("EEPROM-Size");
    public static final String CmdXInf_BLAppFlashEnd     = _T("App-Flash-End");
    public static final String CmdXInf_BLAppFlashSize    = _T("App-Flash-Size");
    public static final String CmdXInf_BLAppJumpMode     = _T("App-Jump-Mode");
    public static final String CmdXInf_BLAppJumpAddress  = _T("App-Jump-Addr");
    public static final String CmdXInf_BLTimeoutCounter  = _T("Timout-Counter");

    public static String ProgXXX_InfoBLMMod(final String prefix, final int nameWidth, final String name, final boolean set)
    { return String.format( prefix + " " + (set ? "SET" : "CLR") + " %-" + nameWidth + "s\n", name ); }
    // ===== END : Static and dynamic text for bootloader-related messages =====

    public static final String ProgXXX_InProgMode        = _T("%s: already in programming mode");
    public static final String ProgXXX_NotInProgMode     = _T("%s: not in programming mode");
    public static final String ProgXXX_FailEProgMode     = _T("%s: failed to enter programming mode");

    public static final String ProgXXX_FailSelSlave      = _T("%s: failed to select the target device");
    public static final String ProgXXX_FailDselSlave     = _T("%s: failed to deselect the target device");
    public static final String ProgXXX_FailPulseSS       = _T("%s: failed to pulse the select signal of the target device");
    public static final String ProgXXX_FailSPITrans      = _T("%s: SPI transfer failed");

    public static final String ProgXXX_PrinterBLBusy     = _T("%s: the printer bootloader has pending job(s) - please use your operating system's method to cancel all pending job(s) before proceeding");
    public static final String ProgXXX_PrinterBLTimeout  = _T("%s: the printer bootloader job has exceeded the timeout limit");

    public static final String ProgXXX_SAddrNotZero      = _T("%s: the start address must be zero");

    public static final String ProgXXX_SAddrNotEven      = _T("%s: the start address must be even (a multiple of 2)");
    public static final String ProgXXX_NBytesNotEven     = _T("%s: the number of bytes must be even (a multiple of 2)");

    public static final String ProgXXX_SAddrNotMPN       = _T("%s: the start address must be a multiple of %d");
    public static final String ProgXXX_NBytesNotMPN      = _T("%s: the number of bytes must be a multiple of %d ");

    public static final String ProgXXX_SAddrNotMPZ       = _T("%s: the start address must be a multiple of the page size");
    public static final String ProgXXX_NBytesNotMPZ      = _T("%s: the number of bytes must be a multiple of the page size");
    public static final String ProgXXX_NANotMultipleOfB  = _T("%s: '%s' is not a multiple of '%s'");

    public static final String ProgXXX_CZNotDivPZwoRem   = _T("%s: the chunk size must be able to divide the page size without remainder");

    public static final String ProgXXX_FSAddrNBytesOoR   = _T("%s: the flash start address, number of bytes, or both are out of range");
    public static final String ProgXXX_EAddrOoR          = _T("%s: the EEPROM address is out of range");
    public static final String ProgXXX_ENotAvailable     = _T("%s: EEPROM is not available for this MCU");

    public static final String ProgXXX_FNotAvailable     = _T("%s: this MCU does not support fuses");
    public static final String ProgXXX_LBNotAvailable    = _T("%s: this MCU does not support lock bits");

    public static final String ProgXXX_FailISP_UpXAddr   = _T("%s: failed to update the extended address byte");

    public static final String ProgXXX_FailTPI_MPsXckNS  = _T("%s: TPI the USRT implementation does not support manually pulsing xck line upon request");
    public static final String ProgXXX_FailTPI_Tx        = _T("%s: TPI tx failed");
    public static final String ProgXXX_FailTPI_Rx        = _T("%s: TPI rx failed");
    public static final String ProgXXX_FailTPI_Init      = _T("%s: TPI initialization failed");
    public static final String ProgXXX_FailTPI_RdByte    = _T("%s: TPI failed to read byte(s)");
    public static final String ProgXXX_FailTPI_RInvVal   = _T("%s: TPI read invalid value");
    public static final String ProgXXX_FailTPI_EnProg    = _T("%s: TPI failed to enable NVM programming mode");
    public static final String ProgXXX_FailTPI_DisProg   = _T("%s: TPI failed to disable NVM programming mode");
    public static final String ProgXXX_FailTPI_NVMRTOut  = _T("%s: TPI NVM not ready timeout");
    public static final String ProgXXX_FailTPI_NVMOTOut  = _T("%s: TPI NVM operation timeout");

    public static final String ProgXXX_FailUPDI_Tx       = _T("%s: UPDI tx failed");
    public static final String ProgXXX_FailUPDI_Rx       = _T("%s: UPDI rx failed");
    public static final String ProgXXX_FailUPDI_TxRx     = _T("%s: UPDI tx-rx failed");
    public static final String ProgXXX_FailUPDI_Timeout  = _T("%s: UPDI timeout");
    public static final String ProgXXX_FailUPDI_Connect  = _T("%s: UPDI connection failed");
    public static final String ProgXXX_FailUPDI_Init     = _T("%s: UPDI initialization failed");
    public static final String ProgXXX_FailUPDI_Disable  = _T("%s: UPDI mode deactivation failed");
    public static final String ProgXXX_FailUPDI_Unlock   = _T("%s: UPDI failed to unlock device");
    public static final String ProgXXX_FailUPDI_GetDDet  = _T("%s: UPDI failed to get device details");
    public static final String ProgXXX_FailUPDI_RdByte   = _T("%s: UPDI failed to read byte(s)");
    public static final String ProgXXX_FailUPDI_RInvVal  = _T("%s: UPDI read invalid value");
    public static final String ProgXXX_FailUPDI_GetDevD  = _T("%s: UPDI failed to obtain the device details");
    public static final String ProgXXX_FailUPDI_Reset    = _T("%s: UPDI reset failed");
    public static final String ProgXXX_FailUPDI_CmEEPROM = _T("%s: UPDI failed to commit EEPROM changes");
    public static final String ProgXXX_FailUPDI_ChErase  = _T("%s: UPDI chip erase failed");

    public static final String ProgXXX_FailUPDI_NVMRevU  = _T("%s: UPDI NVM revision is not supported");
    public static final String ProgXXX_FailUPDI_NVMNotR  = _T("%s: UPDI NVM is not ready");
    public static final String ProgXXX_FailUPDI_EnProgD  = _T("%s: UPDI NVM programming mode is already enabled");
    public static final String ProgXXX_FailUPDI_KeyNDec  = _T("%s: UPDI NVMPROG key is not successfully decoded");
    public static final String ProgXXX_FailUPDI_DevLockd = _T("%s: UPDI device is locked");
    public static final String ProgXXX_FailUPDI_EnProg   = _T("%s: UPDI failed to enable NVM programming mode");
    public static final String ProgXXX_FailUPDI_WaitUlck = _T("%s: UPDI failed to wait for unlock");
    public static final String ProgXXX_FailUPDI_InvKeyZ  = _T("%s: UPDI invalid KEY size");
    public static final String ProgXXX_FailUPDI_CmdErr   = _T("%s: UPDI command error");
    public static final String ProgXXX_FailUPDI_DisACKs  = _T("%s: UPDI failed to disable ACKs");
    public static final String ProgXXX_FailUPDI_EnACKs   = _T("%s: UPDI failed to enable ACKs");
    public static final String ProgXXX_FailUPDI_Recover  = _T("%s: UPDI error recovery failed");

    public static final String ProgXXX_FailPDI_XmgPDINS  = _T("%s: PDI the USRT implementation does not support ATxmega PDI mode");
    public static final String ProgXXX_FailPDI_TxEnDsNS  = _T("%s: PDI the USRT implementation does not support enabling/disabling tx line upon request");
    public static final String ProgXXX_FailPDI_EnTx      = _T("%s: PDI failed to enable tx");
    public static final String ProgXXX_FailPDI_DsTx      = _T("%s: PDI failed to disable tx");
  //public static final String ProgXXX_FailPDI_DsTxAftrN = _T("%s: PDI too many number of bytes specified for disable tx after (%d)");
    public static final String ProgXXX_FailPDI_DsTxAfter = _T("%s: PDI failed to disable tx after");
    public static final String ProgXXX_FailPDI_Tx        = _T("%s: PDI tx failed");
    public static final String ProgXXX_FailPDI_Rx        = _T("%s: PDI rx failed");
    public static final String ProgXXX_FailPDI_TxRx      = _T("%s: PDI tx-rx failed");
    public static final String ProgXXX_FailPDI_ResetStt  = _T("%s: PDI failed to set the device reset state");
    public static final String ProgXXX_FailPDI_ResetDev  = _T("%s: PDI failed to reset the device");
    public static final String ProgXXX_FailPDI_Init      = _T("%s: PDI initialization failed");
    public static final String ProgXXX_FailPDI_RdByte    = _T("%s: PDI failed to read byte(s)");
    public static final String ProgXXX_FailPDI_RInvVal   = _T("%s: PPI read invalid value");
    public static final String ProgXXX_FailPDI_NVMNotE   = _T("%s: PDI NVM programming is not enabled");
    public static final String ProgXXX_FailPDI_NVMNotD   = _T("%s: PDI NVM programming is not disabled");
    public static final String ProgXXX_FailPDI_NVMNotR   = _T("%s: PDI NVM is not ready");
    public static final String ProgXXX_FailPDI_NVMExErr  = _T("%s: PDI NVM command execution error");
    public static final String ProgXXX_FailPDI_NVMCEPRM  = _T("%s: PDI failed to commit EEPROM changes");
    public static final String ProgXXX_FailPDI_EnProg    = _T("%s: PDI failed to enable NVM programming mode");
    public static final String ProgXXX_FailPDI_RdNVMReg  = _T("%s: PDI failed to read NVM register");
    public static final String ProgXXX_FailPDI_WrNVMReg  = _T("%s: PDI failed to write NVM register");
    public static final String ProgXXX_FailPDI_RdNVMMem  = _T("%s: PDI failed to read NVM memory");
    public static final String ProgXXX_FailPDI_WrNVMMem  = _T("%s: PDI failed to write NVM memory");
    public static final String ProgXXX_FailPDI_WrPtrAdr  = _T("%s: PDI failed to write pointer address");
    public static final String ProgXXX_FailPDI_WrRepCnt  = _T("%s: PDI failed to write repeat count");

    public static final String ProgXXX_FailLGT8_Init     = _T("%s: LGT8-SWD initialization failed");
    public static final String ProgXXX_FailLGT8_Uninit   = _T("%s: LGT8-SWD uninitialization failed");
    public static final String ProgXXX_FailLGT8_InvDev   = _T("%s: LGT8-SWD invalid device");
    public static final String ProgXXX_FailLGT8_RdSWDID  = _T("%s: LGT8-SWD failed to read SWDID");
    public static final String ProgXXX_FailLGT8_RdGUID   = _T("%s: LGT8-SWD failed to read GUID");
    public static final String ProgXXX_FailLGT8_Unlock0  = _T("%s: LGT8-SWD failed to unlock 0");
    public static final String ProgXXX_FailLGT8_Unlock1  = _T("%s: LGT8-SWD failed to unlock 1");
    public static final String ProgXXX_FailLGT8_Unlock2  = _T("%s: LGT8-SWD failed to unlock 2");
    public static final String ProgXXX_FailLGT8_ErsChip  = _T("%s: LGT8-SWD failed to erase chip");
    public static final String ProgXXX_FailLGT8_UPrChip  = _T("%s: LGT8-SWD failed to unprotec chip");

    public static final String ProgXXX_FailSWD_InvMDID   = _T("%s: SWD invalid multidrop ID(s)");
    public static final String ProgXXX_FailSWD_Init      = _T("%s: SWD initialization failed");
    public static final String ProgXXX_FailSWD_LineReset = _T("%s: SWD line-reset failed");
    public static final String ProgXXX_FailSWD_Command   = _T("%s: SWD command failed");
    public static final String ProgXXX_FailSWD_RdParErr  = _T("%s: SWD read command parity error");
    public static final String ProgXXX_FailSWD_RdAckNOK  = _T("%s: SWD read command failed with status 0x%02X [%s]");
    public static final String ProgXXX_FailSWD_WrAckNOK  = _T("%s: SWD write command failed with status 0x%02X [%s]");
    public static final String ProgXXX_FailSWD_RdIDCODE  = _T("%s: SWD failed to read SW-DP register IDCODE");
    public static final String ProgXXX_FailSWD_RdCTRLST  = _T("%s: SWD failed to read SW-DP register CTRL/STAT");
    public static final String ProgXXX_FailSWD_RdDLCR    = _T("%s: SWD failed to read SW-DP register DLCR");
    public static final String ProgXXX_FailSWD_RdTRGTID  = _T("%s: SWD failed to read SW-DP register TARGETID");
    public static final String ProgXXX_FailSWD_RdDLPIDR  = _T("%s: SWD failed to read SW-DP register DLPIDR");
    public static final String ProgXXX_FailSWD_RdEVENST  = _T("%s: SWD failed to read SW-DP register EVENSTAT");
    public static final String ProgXXX_FailSWD_RdRESEND  = _T("%s: SWD failed to read SW-DP register RESEND");
    public static final String ProgXXX_FailSWD_RdRDBUFF  = _T("%s: SWD failed to read SW-DP register RDBUFF");
    public static final String ProgXXX_FailSWD_RdRegAP   = _T("%s: SWD failed to read MEM-AP register %s with status 0x%02X [%s]");
    public static final String ProgXXX_FailSWD_RdRegAPV  = _T("%s: SWD read invalid value 0x%08X from MEM-AP register %s");
    public static final String ProgXXX_FailSWD_RdRegVal  = _T("%s: SWD read invalid value 0x%08X from core register with index 0x%02X");
    public static final String ProgXXX_FailSWD_RdMemVal  = _T("%s: SWD read invalid value 0x%08X from memory with address 0x%08X");
    public static final String ProgXXX_FailSWD_WrABORT   = _T("%s: SWD failed to write SW-DP register ABORT");
    public static final String ProgXXX_FailSWD_WrCTRLST  = _T("%s: SWD failed to write SW-DP register CTRL/STAT");
    public static final String ProgXXX_FailSWD_WrSELECT  = _T("%s: SWD failed to write SW-DP register SELECT");
    public static final String ProgXXX_FailSWD_WrTRGSEL  = _T("%s: SWD failed to write SW-DP register TARGETSEL");
    public static final String ProgXXX_FailSWD_WrRegAP   = _T("%s: SWD failed to write MEM-AP register %s with status 0x%02X [%s]");
    public static final String ProgXXX_FailSWD_PUDbgIfc  = _T("%s: SWD failed to power up the debug interface");
    public static final String ProgXXX_FailSWD_RstUnhCr  = _T("%s: SWD failed to reset and un-halt core");
    public static final String ProgXXX_FailSWD_ExecInst  = _T("%s: SWD instruction-buffer execution error with status 0x%08X");
    public static final String ProgXXX_FailSWD_EFMemErs  = _T("%s: SWD erasing the entire flash memory using driver '%s' is not supported");
    public static final String ProgXXX_FailSWD_PFMemErs  = _T("%s: SWD erasing only part of flash memory using driver '%s' is not supported");
    public static final String ProgXXX_FailSWD_ErsFlash  = _T("%s: SWD failed to erase flash memory using driver '%s'");
    public static final String ProgXXX_FailSWD_RdFlashM  = _T("%s: SWD failed to read flash memory using driver '%s' with status %d");
    public static final String ProgXXX_FailSWD_WrFlashM  = _T("%s: SWD failed to write flash memory using driver '%s' with status %d");
    public static final String ProgXXX_FailSWD_RdEEPROM  = _T("%s: SWD failed to read EEPROM memory using driver '%s' with status %d");
    public static final String ProgXXX_FailSWD_WrEEPROM  = _T("%s: SWD failed to write EEPROM memory using driver '%s' with status %d");
    public static final String ProgXXX_FailSWD_CmEEPROM  = _T("%s: SWD failed to commit EEPROM changes");
    public static final String ProgXXX_FailSWD_CmFsLBs   = _T("%s: SWD failed to commit configuration bits (fuses) and security bits (lock bits) changes");
    public static final String ProgXXX_FailSWD_UninitSys = _T("%s: SWD failed to uninitialize sytem");
    public static final String ProgXXX_FailSWD_InsInvld  = _T("%s: SWD [_exec:%010o] invalid instruction");
    public static final String ProgXXX_FailSWD_InsCmpVal = _T("%s: SWD [_exec:%010o] compare value 0x%08X (%010d) %s 0x%08X (%010d)");
    public static final String ProgXXX_FailSWD_InsTOut   = _T("%s: SWD [_exec:%010o] timeout (execute/wait/loop time > %d milliseconds)");
    public static final String ProgXXX_FailSWD_InsSMAPDZ = _T("%s: SWD [_exec:%010o] failed to set the MEM-AP data type size to %d bits");

    public static final String ProgXXX_FailSWIM_Trans    = _T("%s: SWIM transfer failed");
    public static final String ProgXXX_FailSWIM_LineRst  = _T("%s: SWIM line-reset failed");
    public static final String ProgXXX_FailSWIM_RdParErr = _T("%s: SWIM read byte parity error");
    public static final String ProgXXX_FailSWIM_RdToMany = _T("%s: SWIM read got too many bytes");
    public static final String ProgXXX_FailSWIM_SetClkDv = _T("%s: SWIM failed to set clock divider");
    public static final String ProgXXX_FailSWIM_SetSWCSR = _T("%s: SWIM failed to set SWIM_CSR");
    public static final String ProgXXX_FailSWIM_SetDCSR2 = _T("%s: SWIM failed to set DM_CSR2");
    public static final String ProgXXX_FailSWIM_ResetMCU = _T("%s: SWIM failed to reset MCU");
    public static final String ProgXXX_FailSWIM_ULFlashM = _T("%s: SWIM failed to unlock flash memory");
    public static final String ProgXXX_FailSWIM_WiFlashP = _T("%s: SWIM failed to initiate flash memory page-write");
    public static final String ProgXXX_FailSWIM_WrFlashM = _T("%s: SWIM failed to write flash memory at chunk #%d with start address 0x%04X");
    public static final String ProgXXX_FailSWIM_RdFlashM = _T("%s: SWIM failed to read flash memory at chunk #%d with start address 0x%04X");
    public static final String ProgXXX_FailSWIM_ULEEPROM = _T("%s: SWIM failed to unlock EEPROM memory");
    public static final String ProgXXX_FailSWIM_WiEEPROM = _T("%s: SWIM failed to initiate EEPROM memory page-write");
    public static final String ProgXXX_FailSWIM_WrEEPROM = _T("%s: SWIM failed to write EEPROM memory at chunk #%d with start address 0x%04X");
    public static final String ProgXXX_FailSWIM_RdEEPROM = _T("%s: SWIM failed to read EEPROM memory at chunk #%d with start address 0x%04X");
    public static final String ProgXXX_FailSWIM_CmEEPROM = _T("%s: SWIM failed to commit EEPROM changes");
    public static final String ProgXXX_FailSWIM_ULOptByt = _T("%s: SWIM failed to unlock EEPROM byte memory");
    public static final String ProgXXX_FailSWIM_WiOptByt = _T("%s: SWIM failed to initiate option byte memory page-write");
    public static final String ProgXXX_FailSWIM_WrOptByt = _T("%s: SWIM failed to write option byte memory");
    public static final String ProgXXX_FailSWIM_RdOptByt = _T("%s: SWIM failed to read option byte memory");

    public static final String ProgXXX_FailPIC_InitSTDP  = _T("%s: PIC STDP initialization failed");
    public static final String ProgXXX_FailPIC_InitICSP  = _T("%s: PIC ICSP initialization failed");
    public static final String ProgXXX_FailPIC_InitEICSP = _T("%s: PIC EICSP initialization failed");
    public static final String ProgXXX_FailPIC_EnterPVM  = _T("%s: PIC failed to enter program & verify mode");
    public static final String ProgXXX_FailPIC_ExitPVM   = _T("%s: PIC failed to exit program & verify mode");
    public static final String ProgXXX_FailPIC_ReadCByte = _T("%s: PIC failed to read configuration bytes");
    public static final String ProgXXX_FailPIC_CmEEPROM  = _T("%s: PIC failed to commit EEPROM changes");
    public static final String ProgXXX_FailPIC_UninitSys = _T("%s: PIC failed to uninitialize system");
    public static final String ProgXXX_FailPIC_CmpEQ     = _T("%s: PIC invalid value 0x%04X == 0x%04X");
    public static final String ProgXXX_FailPIC_CmpNEQ    = _T("%s: PIC invalid value 0x%04X != 0x%04X");
    public static final String ProgXXX_FailPIC_OperICSP  = _T("%s: PIC operation failed (this operation is not supported in ICSP/STDP mode)");
    public static final String ProgXXX_FailPIC_OperEICSP = _T("%s: PIC operation failed (this operation is not supported in EICSP mode)");
    public static final String ProgXXX_FailPIC_CfBitsNOP = _T("%s: this operation is a NO-OPERATION in this MCU series because the configuration words/bits are part of the program memory space; therefore, direct modification of the configuration words/bits is not supported");

    public static final String ProgXXX_FailJTAG_NS       = _T("%s: JTAG the mode does not support JTAG mode");
    public static final String ProgXXX_FailJTAG_IDLE     = _T("%s: JTAG failed to transition to the IDLE state");
    public static final String ProgXXX_FailJTAG_Exec     = _T("%s: JTAG command execution failed for '%s' at line %d");
    public static final String ProgXXX_FailJTAG_ExecTag  = _T("%s: JTAG command execution failed for '%s' at line %d: '%s'");
    public static final String ProgXXX_FailJTAG_TDOIv    = _T("%s: JTAG received an invalid TDO value for byte #%d (0x%02X instead of 0x%02X) for '%s' at line %d");
    public static final String ProgXXX_FailJTAG_TDOIvTag = _T("%s: JTAG received an invalid TDO value for byte #%d (0x%02X instead of 0x%02X) for '%s' at line %d: '%s'");

    public static final String ProgXXX_FailSAMBAUnsupMCU = _T("%s: SAM-BA found unsupported MCU with %s '0x%08X'");
    public static final String ProgXXX_FailSAMBAAplBuild = _T("%s: SAM-BA failed to build the applet binary");
    public static final String ProgXXX_FailSAMBAAplInit  = _T("%s: SAM-BA failed to initialize the applet");
    public static final String ProgXXX_FailSAMBAAplConf  = _T("%s: SAM-BA failed to configure the applet");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String ProgXXX_InvBPSPartUns = _T("%s: unsupported configuration value for 'BaseProgSpec.part'");
    public static final String ProgXXX_InvBPSSPrtUns = _T("%s: unsupported configuration value for 'BaseProgSpec.subPart'");
    public static final String ProgXXX_InvBPSEICSP   = _T("%s: unsupported configuration value for 'BaseProgSpec.mode' and/or 'BaseProgSpec.entrySeq' (EICSP is not supported yet)");
    public static final String ProgXXX_InvBPSPart    = _T("%s: invalid configuration value for 'BaseProgSpec.part'");
    public static final String ProgXXX_InvBPSSPrt    = _T("%s: invalid configuration value for 'BaseProgSpec.subPart'");
    public static final String ProgXXX_InvBPSMode    = _T("%s: invalid configuration value for 'BaseProgSpec.mode'");
    public static final String ProgXXX_InvBPSEntrySq = _T("%s: invalid configuration value for 'BaseProgSpec.entrySeq'");
    public static final String ProgXXX_InvBPSForXXX  = _T("%s: invalid configuration value for 'BaseProgSpec.%s'");
    public static final String ProgXXX_InvBPSXXXXXX  = _T("%s: invalid configuration value for 'BaseProgSpec.%s %s BaseProgSpec.%s%s'");

    public static final String ProgXXX_InvBPSMFBAdrB = _T("%s: invalid configuration value 'BaseProgSpec.configMemAddressBeg != MemoryConfigBytes.addressBeg'");

    public static final String ProgXXX_InvMFSigAddr  = _T("%s: invalid configuration value for 'MemorySignature.address'");
    public static final String ProgXXX_InvMFSigSize  = _T("%s: invalid configuration value for 'MemorySignature.size'");

    public static final String ProgXXX_InvMRAddress  = _T("%s: invalid configuration value for 'MemorySRAM.address'");
    public static final String ProgXXX_InvMRTotSize  = _T("%s: invalid configuration value for 'MemorySRAM.totalSize'");
    public static final String ProgXXX_InvMRBankAddr = _T("%s: invalid configuration value for 'MemorySRAM.memBankSpec.address'");
    public static final String ProgXXX_InvMRBankSize = _T("%s: invalid configuration value for 'MemorySRAM.memBankSpec.size'");

    public static final String ProgXXX_InvMFAddress  = _T("%s: invalid configuration value for 'MemoryFlash.address'");
    public static final String ProgXXX_InvMFTotSize  = _T("%s: invalid configuration value for 'MemoryFlash.totalSize'");
    public static final String ProgXXX_InvMFNumWrWrs = _T("%s: invalid configuration value for 'MemoryFlash.numWordWrites'");
    public static final String ProgXXX_InvMFEBufSize = _T("%s: invalid configuration value for 'MemoryFlash.eraseBlockSize'");
    public static final String ProgXXX_InvMFWBufSize = _T("%s: invalid configuration value for 'MemoryFlash.writeBlockSize'");
    public static final String ProgXXX_InvMFPageSize = _T("%s: invalid configuration value for 'MemoryFlash.pageSize'");
    public static final String ProgXXX_InvMFNumPages = _T("%s: invalid configuration value for 'MemoryFlash.numPages'");
    public static final String ProgXXX_InvMFPgDrSize = _T("%s: invalid configuration value for 'MemoryFlash.paged_direct'");
    public static final String ProgXXX_InvMFNumPgsDr = _T("%s: invalid configuration value for 'MemoryFlash.numPages_direct'");
    public static final String ProgXXX_InvMFPrtEAddr = _T("%s: invalid configuration value for 'MemoryFlash.partEraseAddressBeg'");
    public static final String ProgXXX_InvMFPrtESize = _T("%s: invalid configuration value for 'MemoryFlash.partEraseSize'");
    public static final String ProgXXX_InvMFBankAddr = _T("%s: invalid configuration value for 'MemoryFlash.memBankSpec.address'");
    public static final String ProgXXX_InvMFBankSize = _T("%s: invalid configuration value for 'MemoryFlash.memBankSpec.size'");
    public static final String ProgXXX_InvMFPageSpec = _T("%s: invalid configuration value 'MemoryFlash.totalSize != MemoryFlash.pageSize * MemoryFlash.numPages'");
    public static final String ProgXXX_InvMFPgDrSpec = _T("%s: invalid configuration value 'MemoryFlash.totalSize != MemoryFlash.paged_direct * MemoryFlash.numPages_direct'");

    public static final String ProgXXX_InvPEAddress  = _T("%s: invalid configuration value for 'MemoryPE.address'");
    public static final String ProgXXX_InvPETotSize  = _T("%s: invalid configuration value for 'MemoryPE.totalSize'");
    public static final String ProgXXX_InvPEPageSize = _T("%s: invalid configuration value for 'MemoryPE.pageSize'");
    public static final String ProgXXX_InvPENumPages = _T("%s: invalid configuration value for 'MemoryPE.numPages'");
    public static final String ProgXXX_InvPESWOffset = _T("%s: invalid configuration value for 'MemoryPE.saveWordOffset'");
    public static final String ProgXXX_InvPESWCount  = _T("%s: invalid configuration value for 'MemoryPE.saveWordCount'");
    public static final String ProgXXX_InvPEPageSpec = _T("%s: invalid configuration value 'MemoryPE.totalSize != MemoryPE.pageSize * MemoryPE.numPages'");
    public static final String ProgXXX_InvPESWSpec   = _T("%s: invalid configuration value 'MemoryPE.address + MemoryPE.saveWordOffset + MemoryPE.saveWordCount * 2 >= MemoryPE.address + MemoryPE.totalSize'");

    public static final String ProgXXX_InvMEAddress  = _T("%s: invalid configuration value for 'MemoryEEPROM.address'");
    public static final String ProgXXX_InvMEAddressB = _T("%s: invalid configuration value for 'MemoryEEPROM.addressBeg'");
    public static final String ProgXXX_InvMEAddressE = _T("%s: invalid configuration value for 'MemoryEEPROM.addressEnd'");
    public static final String ProgXXX_InvMETotSize  = _T("%s: invalid configuration value for 'MemoryEEPROM.totalSize'");
    public static final String ProgXXX_InvMEWBufSizE = _T("%s: invalid configuration value for 'MemoryEEPROM.writeBlockSizeE'");
    public static final String ProgXXX_InvMEPageSize = _T("%s: invalid configuration value for 'MemoryEEPROM.pageSize'");
    public static final String ProgXXX_InvMENumPages = _T("%s: invalid configuration value for 'MemoryEEPROM.numPages'");
    public static final String ProgXXX_InvMEAdrMulFW = _T("%s: invalid configuration value for 'MemoryEEPROM.addressMulFW'");
    public static final String ProgXXX_InvMEAdrOfsFW = _T("%s: invalid configuration value for 'MemoryEEPROM.addressOfsFW'");
    public static final String ProgXXX_InvMEPageSpec = _T("%s: invalid configuration value 'MemoryEEPROM.totalSize != MemoryEEPROM.pageSize * MemoryEEPROM.numPages'");
    public static final String ProgXXX_InvMEAddrSpec = _T("%s: invalid configuration value 'MemoryEEPROM.totalSize != MemoryEEPROM.addressEnd - MemoryEEPROM.addressBeg + 1'");

    public static final String ProgXXX_InvMOAddress  = _T("%s: invalid configuration value for 'MemoryOptionBytes.address'");
    public static final String ProgXXX_InvMOTotSize  = _T("%s: invalid configuration value for 'MemoryOptionBytes.totalSize'");
    public static final String ProgXXX_InvMOPageSize = _T("%s: invalid configuration value for 'MemoryOptionBytes.pageSize'");
    public static final String ProgXXX_InvMODefVals  = _T("%s: invalid configuration value for 'MemoryOptionBytes.defaultValues'");
    public static final String ProgXXX_InvMOIdxROP   = _T("%s: invalid configuration value for 'MemoryOptionBytes.idx_ROP'");
    public static final String ProgXXX_InvMFBAddresB = _T("%s: invalid configuration value for 'MemoryConfigBytes.addressBeg'");
    public static final String ProgXXX_InvMFBAddresE = _T("%s: invalid configuration value for 'MemoryConfigBytes.addressEnd'");
    public static final String ProgXXX_InvMFBAddress = _T("%s: invalid configuration value for 'MemoryConfigBytes.address'");
    public static final String ProgXXX_InvMFBSize    = _T("%s: invalid configuration value for 'MemoryConfigBytes.size'");
    public static final String ProgXXX_InvMFBBitMask = _T("%s: invalid configuration value for 'MemoryConfigBytes.bitMask'");
    public static final String ProgXXX_InvMFBOrgMask = _T("%s: invalid configuration value for 'MemoryConfigBytes.orgMask'");
    public static final String ProgXXX_InvMFBClrMask = _T("%s: invalid configuration value for 'MemoryConfigBytes.clrMask'");
    public static final String ProgXXX_InvMFBSetMask = _T("%s: invalid configuration value for 'MemoryConfigBytes.setMask'");
    public static final String ProgXXX_InvMFBAdMulFW = _T("%s: invalid configuration value for 'MemoryConfigBytes.addressMulFW'");
    public static final String ProgXXX_InvMFBAdOfsFW = _T("%s: invalid configuration value for 'MemoryConfigBytes.addressOfsFW'");
    public static final String ProgXXX_InvMFBAddrFWL = _T("%s: invalid configuration value 'MemoryConfigBytes.addressFW.length != MemoryConfigBytes.address.length'");
    public static final String ProgXXX_InvMFBSizeL   = _T("%s: invalid configuration value 'MemoryConfigBytes.size.length != MemoryConfigBytes.address.length'");
    public static final String ProgXXX_InvMFBBitMskL = _T("%s: invalid configuration value 'MemoryConfigBytes.bitMask.length != MemoryConfigBytes.address.length'");
    public static final String ProgXXX_InvMFBOrgMskL = _T("%s: invalid configuration value 'MemoryConfigBytes.orgMask.length != MemoryConfigBytes.address.length'");
    public static final String ProgXXX_InvMFBClrMskL = _T("%s: invalid configuration value 'MemoryConfigBytes.clrMask.length != MemoryConfigBytes.address.length'");
    public static final String ProgXXX_InvMFBSetMskL = _T("%s: invalid configuration value 'MemoryConfigBytes.setMask.length != MemoryConfigBytes.address.length'");

    public static final String ProgXXX_InvMCFAddress = _T("%s: invalid configuration value for 'MemoryFuse.address'");
    public static final String ProgXXX_InvMCFSize    = _T("%s: invalid configuration value for 'MemoryFuse.size'");
    public static final String ProgXXX_InvMCFBitMask = _T("%s: invalid configuration value for 'MemoryFuse.bitMask'");
    public static final String ProgXXX_InvMCFClrMask = _T("%s: invalid configuration value for 'MemoryFuse.clrMask'");
    public static final String ProgXXX_InvMCFSetMask = _T("%s: invalid configuration value for 'MemoryFuse.setMask'");
    public static final String ProgXXX_InvMCFSizeL   = _T("%s: invalid configuration value 'MemoryFuse.size.length != MemoryFuse.address.length'");
    public static final String ProgXXX_InvMCFBitMskL = _T("%s: invalid configuration value 'MemoryFuse.bitMask.length != MemoryFuse.address.length'");
    public static final String ProgXXX_InvMCFClrMskL = _T("%s: invalid configuration value 'MemoryFuse.clrMask.length != MemoryFuse.address.length'");
    public static final String ProgXXX_InvMCFSetMskL = _T("%s: invalid configuration value 'MemoryFuse.setMask.length != MemoryFuse.address.length'");

    public static final String ProgXXX_InvMLBAddress = _T("%s: invalid configuration value for 'MemoryLockBits.address'");
    public static final String ProgXXX_InvMLBSize    = _T("%s: invalid configuration value for 'MemoryLockBits.size'");
    public static final String ProgXXX_InvMLBBitMask = _T("%s: invalid configuration value for 'MemoryLockBits.bitMask'");
    public static final String ProgXXX_InvMLBBitMskX = _T("%s: invalid configuration value for 'MemoryLockBits.bitMaskExt'");

    public static final String ProgXXX_InvMCBAddress = _T("%s: invalid configuration value for 'MemoryCalibrationByte.address'");
    public static final String ProgXXX_InvMCBSize    = _T("%s: invalid configuration value for 'MemoryCalibrationByte.size'");

    public static final String ProgXXX_InvMINumVecs  = _T("%s: invalid configuration value for 'MCUInfo.numVectors'");
    public static final String ProgXXX_InvMITotSRAM  = _T("%s: invalid configuration value for 'MCUInfo.totalSRAM'");

    public static final String ProgXXX_InvAIFlDriver = _T("%s: invalid configuration value for 'AppletInfo.flashDriver'");
    public static final String ProgXXX_InvAIFlBase   = _T("%s: invalid configuration value for 'AppletInfo.flashBase'");
    public static final String ProgXXX_InvAIFlNPlane = _T("%s: invalid configuration value for 'AppletInfo.flashNumPlanes'");
    public static final String ProgXXX_InvAIFlNLcReg = _T("%s: invalid configuration value for 'AppletInfo.flashNumLockRegs'");
    public static final String ProgXXX_InvAISApStart = _T("%s: invalid configuration value for 'AppletInfo.sramAppletStart'");
    public static final String ProgXXX_InvAISApStack = _T("%s: invalid configuration value for 'AppletInfo.sramAppletStack'");
    public static final String ProgXXX_InvAIRegBase  = _T("%s: invalid configuration value for 'AppletInfo.regBase'");
    public static final String ProgXXX_InvAIRegRst   = _T("%s: invalid configuration value for 'AppletInfo.regReset'");
    public static final String ProgXXX_InvAIRegRstCm = _T("%s: invalid configuration value for 'AppletInfo.regResetCommand'");

    public static final String ProgXXX_InvRegSTM8    = _T("%s: invalid configuration value for 'RegSTM8.%s'");

    public static final String ProgXXX_InvRegPIC     = _T("%s: invalid configuration value for 'RegPIC.%s'");
    public static final String ProgXXX_InvCmdPIC     = _T("%s: invalid configuration value for 'CmdPIC.%s'");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String QSPICMD_InvFuncMode   = _T("%s: invalid 'QSPICmd.funcMode' value");
    public static final String QSPICMD_InvInstMode   = _T("%s: invalid 'QSPICmd.instMode' value");
    public static final String QSPICMD_InvAddrMode   = _T("%s: invalid 'QSPICmd.addrMode' value");
    public static final String QSPICMD_InvAddrSize   = _T("%s: invalid 'QSPICmd.addrSize' value");
    public static final String QSPICMD_InvAltBMode   = _T("%s: invalid 'QSPICmd.altbMode' value");
    public static final String QSPICMD_InvAltBSize   = _T("%s: invalid 'QSPICmd.altbSize' value");
    public static final String QSPICMD_InvDataMode   = _T("%s: invalid 'QSPICmd.dataMode' value");
    public static final String QSPICMD_InvDataLength = _T("%s: invalid 'QSPICmd.dataLength' value");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String ASM_ProgramSizeTooLarge  = _T("%s: the resulting firmware size of %d bytes exceeds the maximum allowable size of %d bytes");

    public static final String ASM_InvalidInstruction   = _T("%s:'%s': instruction '%s' is not supported by '%s' CPU");
    public static final String ASM_InvalidInstructionF  = _T("%s:'%s': instruction '%s' in this form is not supported by '%s' CPU");
    public static final String ASM_InvalidInstSeq       = _T("%s:'%s': instruction '%s' can not be followed by this instruction");
    public static final String ASM_InvalidImmValue      = _T("%s:'%s': immediate value %d is not supported by '%s' CPU in this context");
    public static final String ASM_InvalidConstAFixup   = _T("%s:'%s': invalid constant 0x%08X (%d) after fixup");
    public static final String ASM_InvalidORGValue      = _T("%s:'%s': origin value 0x%08X is invalid in this context");

    public static final String ASM_InvalidAlignFactor   = _T("%s:'%s': invalid alignment factor %d");
    public static final String ASM_InvalidBShiftFactor  = _T("%s:'%s': invalid bit shift factor %d");
    public static final String ASM_InvalidShiftRegOpr   = _T("%s:'%s': invalid shifted-register operand '%s'");
    public static final String ASM_InvalidShiftRegExpr  = _T("%s:'%s': invalid shifted-register expression '%s %d'");

    public static final String ASM_InsLoc_IncorrectCond = _T("%s:'%s': incorrect #%d condition '%s' in IT block '%s'");
    public static final String ASM_InsLoc_OutsideIT     = _T("%s:'%s': instruction '%s' is not permitted outside an IT block");
    public static final String ASM_InsLoc_InsideIT      = _T("%s:'%s': instruction '%s' is not permitted inside an IT block");
    public static final String ASM_InsLoc_LastInsideIT  = _T("%s:'%s': instruction '%s' must be the last instruction in an IT block");

    public static final String ASM_AddrOfstNotAlign2B   = _T("%s:'%s': address/offset 0x%08X (%d) is not aligned to 2 bytes");
    public static final String ASM_AddrOfstNotAlign4B   = _T("%s:'%s': address/offset 0x%08X (%d) is not aligned to 4 bytes");
    public static final String ASM_AddrOfstNotAlign8B   = _T("%s:'%s': address/offset 0x%08X (%d) is not aligned to 8 bytes");
    public static final String ASM_AddrOfstNotAlign16B  = _T("%s:'%s': address/offset 0x%08X (%d) is not aligned to 16 bytes");
    public static final String ASM_AddrOfstVOutOfRange  = _T("%s:'%s': address/offset/value 0x%08X (%d) is out of range");

    public static final String ASM_InvalidRegNAI        = _T("%s:'%s': register '%s' cannot be used in this context");
    public static final String ASM_InvalidReg           = _T("%s:'%s': register '%s' cannot be used in this context; register '%s' required");
    public static final String ASM_InvalidRegL          = _T("%s:'%s': register '%s' cannot be used in this context; undecorated lo-register required");
    public static final String ASM_InvalidRegH          = _T("%s:'%s': register '%s' cannot be used in this context; undecorated hi-register required");
    public static final String ASM_InvalidRegInList     = _T("%s:'%s': register '%s' cannot be included in the list");
    public static final String ASM_InvalidRegNotInList  = _T("%s:'%s': register '%s' must be included in the list");
    public static final String ASM_InvalidRegLRPCInList = _T("%s:'%s': register 'LR & PC' cannot be both included in the list");

    public static final String ASM_EmptyTargetLabel     = _T("%s:'%s': empty target label");
    public static final String ASM_DuplicateTargetLabel = _T("%s:'%s': duplicated target label");
    public static final String ASM_InvalidTargetLabelLc = _T("%s:'%s': empty target location");
    public static final String ASM_InvalidTargetLabelFn = _T("%s:'%s': target label of this type cannot be used in this context");
    public static final String ASM_InvalidTargetLabel   = _T("%s:'%s': undefined target label '%s'");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    static {

        UIManager.put( "FileChooser.acceptAllFileFilterText"       , _T("All Files")                   );
        UIManager.put( "FileChooser.byDateText"                    , _T("Date")                        );
        UIManager.put( "FileChooser.byNameText"                    , _T("Name")                        );
        UIManager.put( "FileChooser.cancelButtonMnemonic"          , _T("C")                           );
        UIManager.put( "FileChooser.cancelButtonText"              , _T("Cancel")                      );
        UIManager.put( "FileChooser.cancelButtonToolTipText"       , _T("Cancel file selection")       );
        UIManager.put( "FileChooser.chooseButtonText"              , _T("Choose")                      );
        UIManager.put( "FileChooser.chooseButtonToolTipText"       , _T("Choose the selected file")    );
        UIManager.put( "FileChooser.createButtonText"              , _T("Create")                      );
        UIManager.put( "FileChooser.createButtonToolTipText"       , _T("Create a new file")           );
        UIManager.put( "FileChooser.desktopFolderToolTipText"      , _T("Desktop")                     );
        UIManager.put( "FileChooser.detailsViewButtonToolTipText"  , _T("Details")                     );
        UIManager.put( "FileChooser.directoryDescriptionText"      , _T("Directory")                   );
        UIManager.put( "FileChooser.directoryOpenButtonMnemonic"   , _T("O")                           );
        UIManager.put( "FileChooser.directoryOpenButtonText"       , _T("Open")                        );
        UIManager.put( "FileChooser.directoryOpenButtonToolTipText", _T("Open the selected directory") );
        UIManager.put( "FileChooser.fileDescriptionText"           , _T("File")                        );
        UIManager.put( "FileChooser.fileNameLabelMnemonic"         , _T("N")                           );
        UIManager.put( "FileChooser.fileNameLabelText"             , _T("File Name:")                  );
        UIManager.put( "FileChooser.fileSizeBytes"                 , _T("bytes")                       );
        UIManager.put( "FileChooser.fileSizeKiloBytes"             , _T("KB")                          );
        UIManager.put( "FileChooser.fileSizeMegaBytes"             , _T("MB")                          );
        UIManager.put( "FileChooser.filesOfTypeLabelMnemonic"      , _T("T")                           );
        UIManager.put( "FileChooser.filesOfTypeLabelText"          , _T("Files of Type:")              );
        UIManager.put( "FileChooser.helpButtonMnemonic"            , _T("H")                           );
        UIManager.put( "FileChooser.helpButtonText"                , _T("Help")                        );
        UIManager.put( "FileChooser.helpButtonToolTipText"         , _T("Show help information")       );
        UIManager.put( "FileChooser.homeFolderToolTipText"         , _T("Home")                        );
        UIManager.put( "FileChooser.listViewButtonToolTipText"     , _T("List")                        );
        UIManager.put( "FileChooser.lookInLabelMnemonic"           , _T("I")                           );
        UIManager.put( "FileChooser.lookInLabelText"               , _T("Look in:")                    );
        UIManager.put( "FileChooser.newFolderAccessibleName"       , _T("New Folder")                  );
        UIManager.put( "FileChooser.newFolderButtonText"           , _T("New Folder")                  );
        UIManager.put( "FileChooser.newFolderButtonToolTipText"    , _T("Create a new folder")         );
        UIManager.put( "FileChooser.newFolderErrorSeparator"       ,    ":"                            );
        UIManager.put( "FileChooser.newFolderErrorText"            , _T("Error creating new folder")   );
        UIManager.put( "FileChooser.newFolderExistsErrorText"      , _T("Folder already exists")       );
        UIManager.put( "FileChooser.newFolderPromptText"           , _T("Folder name:")                );
        UIManager.put( "FileChooser.newFolderTitleText"            , _T("Create New Folder")           );
        UIManager.put( "FileChooser.openButtonMnemonic"            , _T("O")                           );
        UIManager.put( "FileChooser.openButtonText"                , _T("Open")                        );
        UIManager.put( "FileChooser.openButtonToolTipText"         , _T("Open the selected file")      );
        UIManager.put( "FileChooser.openDialogTitleText"           , _T("Open")                        );
        UIManager.put( "FileChooser.openTitleText"                 , _T("Open")                        );
        UIManager.put( "FileChooser.refreshActionLabelText"        , _T("Refresh")                     );
        UIManager.put( "FileChooser.refreshButtonToolTipText"      , _T("Refresh")                     );
        UIManager.put( "FileChooser.rootsAccessibleName"           , _T("Roots")                       );
        UIManager.put( "FileChooser.saveButtonMnemonic"            , _T("S")                           );
        UIManager.put( "FileChooser.saveButtonText"                , _T("Save")                        );
        UIManager.put( "FileChooser.saveButtonToolTipText"         , _T("Save the current file")       );
        UIManager.put( "FileChooser.saveDialogFileNameLabelText"   , _T("File Name:")                  );
        UIManager.put( "FileChooser.saveDialogTitleText"           , _T("Save As")                     );
        UIManager.put( "FileChooser.saveTitleText"                 , _T("Save")                        );
        UIManager.put( "FileChooser.untitledFileName"              , _T("untitled")                    );
        UIManager.put( "FileChooser.untitledFolderName"            , _T("untitled folder")             );
        UIManager.put( "FileChooser.upFolderToolTipText"           , _T("Up One Level")                );
        UIManager.put( "FileChooser.updateButtonMnemonic"          , _T("U")                           );
        UIManager.put( "FileChooser.updateButtonText"              , _T("Update")                      );
        UIManager.put( "FileChooser.updateButtonToolTipText"       , _T("Update directory listing")    );
        UIManager.put( "FileChooser.useSystemExtensionHiding"      ,    Boolean.TRUE                   );
        UIManager.put( "FileChooser.viewMenuLabelText"             , _T("View")                        );

        UIManager.put( "OptionPane.cancelButtonMnemonic"           , _T("C")                           );
        UIManager.put( "OptionPane.cancelButtonText"               , _T("Cancel")                      );
        UIManager.put( "OptionPane.inputDialogTitle"               , _T("Input")                       );
        UIManager.put( "OptionPane.inputDialogTitleText"           , _T("Input")                       );
        UIManager.put( "OptionPane.messageDialogTitle"             , _T("Message")                     );
        UIManager.put( "OptionPane.messageDialogTitleText"         , _T("Message")                     );
        UIManager.put( "OptionPane.noButtonMnemonic"               , _T("N")                           );
        UIManager.put( "OptionPane.noButtonText"                   , _T("No")                          );
        UIManager.put( "OptionPane.okButtonMnemonic"               , _T("O")                           );
        UIManager.put( "OptionPane.okButtonText"                   , _T("OK")                          );
        UIManager.put( "OptionPane.titleText"                      , _T("Select an Option")            );
        UIManager.put( "OptionPane.yesButtonMnemonic"              , _T("Y")                           );
        UIManager.put( "OptionPane.yesButtonText"                  , _T("Yes")                         );

    } // static

} // class Texts
