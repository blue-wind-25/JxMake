/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.util.EnumSet;
import java.util.Set;
import java.util.Stack;

import java.util.regex.Pattern;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import jxm.annotation.*;

import static jxm.gcomp.se.JxMakeTokenMaker_Constants.*;
import static jxm.gcomp.se.JxMakeTokenMaker_Utils.*;
import static jxm.gcomp.se.JxMakeTheme.TType;


/*
 * https://github.com/bobbylight/RSyntaxTextArea/wiki/Adding-Syntax-Highlighting-for-a-new-Language
 */
public class JxMakeTokenMaker extends AbstractTokenMaker {

    @Override
    public TokenMap getWordsToHighlight()
    {
        final TokenMap tokenMap = new TokenMap();

        _Keywords       .forEach(tokenMap::put);
        _SVars          .forEach(tokenMap::put);
        _SVarSCuts      .forEach(tokenMap::put);
        _ANSIEscapeCodes.forEach(tokenMap::put);
        _OptionsKey1    .forEach(tokenMap::put);
        _OptionsKey2    .forEach(tokenMap::put);

        return tokenMap;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Segment _gtl_segment;
    private int     _gtl_newOfs;

    private char[]  _gtl_array;
    private int     _gtl_curPos;
    private int     _gtl_endPos;
    private int     _gtl_endPosSave;

    private int     _gtl_curTok;
    private int     _gtl_detPos;     // WARNING : Only use '_gtl_detPos_storeIfValid()' to store values from 'JxMakeTokenMaker_Utils.*()' functions into this variable!
    private int     _gtl_detPosSave;

    private boolean _gtl_lineContinue;
    private int     _gtl_nestedMLComment;

    private boolean _inStringML     = false;
    private int     _inEvalStatment = -1;

    private void _gtl_curTokReset()  // WARNING : Internally invoked by '_ctxPop()' and '_ctxClear()'!
    { _gtl_curTok = TType.Null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _gtl_detPos_storeIfValid(final int res)
    {
        if(res >= 0) _gtl_detPos = res;

        return res;
    }

    private int _gtl_detPos_WholeWord()
    { return _gtl_detPos_storeIfValid( _detect(_pmd_WholeWord   , _gtl_array, _gtl_curPos, _gtl_endPos) ); }

    private int _gtl_detPos_SimpleString()
    { return _gtl_detPos_storeIfValid( _detect(_pmd_SimpleString, _gtl_array, _gtl_curPos, _gtl_endPos) ); }

    private int _gtl_detPos_SystemIncludeString()
    { return _gtl_detPos_storeIfValid( _detect(_pmd_SysIncString, _gtl_array, _gtl_curPos, _gtl_endPos) ); }

    private int _gtl_detPos_SymbolName()
    { return _gtl_detPos_storeIfValid( _detect(_pmd_SymbolName  , _gtl_array, _gtl_curPos, _gtl_endPos) ); }

    private int _gtl_detPos_DecNum()
    { return _gtl_detPos_storeIfValid( _detect(_pmd_DecNum      , _gtl_array, _gtl_curPos, _gtl_endPos) ); }

    private int _gtl_detPos_HexNum()
    { return _gtl_detPos_storeIfValid( _detect(_pmd_HexNum      , _gtl_array, _gtl_curPos, _gtl_endPos) ); }

    private int _gtl_detPos_Generic(final Pattern... detect)
    {
        for(final Pattern p : detect) {
            final int res = _gtl_detPos_storeIfValid( _detect(p, _gtl_array, _gtl_curPos, _gtl_endPos) );
            if(res != -1) return res;
        }

        return -1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Token _addEmptyToken(final int tokenType, final int startOffset)
    {
        super.addToken( new char[] { ' ' }, 0, -1, _inStringML ? TType.String : tokenType, startOffset );

        return firstToken;
    }

    private Token _addEmptyToken(final int startOffset)
    { return _addEmptyToken(_gtl_curTok, startOffset); }

    /*
    private Token _addEmptyTokenCP()
    { return _addEmptyToken(_gtl_curTok, _gtl_newOfs + _gtl_curPos); }
    */

    private Token _addEmptyTokenEP()
    { return _addEmptyToken(_gtl_curTok, _gtl_newOfs + _gtl_endPos); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _addToken(final int start, final int end, final int tokenType, final int startOffset)
    { super.addToken(_gtl_segment, start, end, tokenType, startOffset); }

    private void _addTokenCP(final int start, final int end, final int tokenType)
    { _addToken(start, end, tokenType, _gtl_newOfs + _gtl_curPos); }

    /*
    private void _addTokenEP(final int start, final int end, final int tokenType)
    { _addToken(start, end, tokenType, _gtl_newOfs + _gtl_endPos); }
    */

    private void _addTokenCP_DP(final int start, final int end, final int tokenType)
    {
        _addTokenCP(start, end, tokenType);
        _gtl_curPos = _gtl_detPos;
    }

    private void _addTokenCP_DP(final int start, final int end)
    { _addTokenCP_DP(start, end, _gtl_curTok); }

    private void _addTokenCP_EP(final int start, final int end, final int tokenType)
    {
        _addTokenCP(start, end, tokenType);
        _gtl_curPos = _gtl_endPos;
    }

    private void _addTokenCP_EP(final int start, final int end)
    { _addTokenCP_EP(start, end, _gtl_curTok); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _wwLenLimit(final int maxLen)
    {
        final int len = _wwLen();

        if(len > maxLen) _gtl_detPos -= (len - maxLen);
    }

    private void _wwLenLimitSave(final int maxLen)
    {
        _gtl_detPosSave = _gtl_detPos;

        _wwLenLimit(maxLen);
    }

    // NOTE : This function always returns false for ease of use
    private boolean _wwLenRestore()
    {
        _gtl_detPos = _gtl_detPosSave;

        return false;
    }

    private int _wwLen()
    { return (_gtl_detPos >= 0) ? (_gtl_detPos - _gtl_curPos) : -1; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _wwIsChar()
    { return _wwLen() == 1; }

    private boolean _wwIsWord()
    { return _wwLen() > 1; }

    private boolean _wwIsOper()
    { return _wwLen() >= 1 && _wwLen() <= 3; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * These functions EXTRACT a token from a sequence of characters in the line buffer using the
     * current position '_gtl_curPos' and the position after the detected token '_gtl_detPos'.
     *
     * The length of the token in characters is '_gtl_detPos - _gtl_curPos'.
     *
     * These functions DO NOT MODIFY '_gtl_curPos' or '_gtl_detPos'.
     */

    private char _wwChar()
    { return _wwIsChar() ? _gtl_array[_gtl_curPos] : 0; }

    private String _wwWord()
    { return _wwIsWord() ? ( new String(_gtl_array, _gtl_curPos, _gtl_detPos - _gtl_curPos) ) : null; }

    private String _wwOper()
    { return _wwIsOper() ? ( new String(_gtl_array, _gtl_curPos, _gtl_detPos - _gtl_curPos) ) : null;}

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * These functions EXTRACT a token from a sequence of characters in the line buffer using the
     * current position '_gtl_curPos' and the position after the detected token '_gtl_detPos'.
     *
     * These functions forcibly limit the resulting token length to a maximum of 'maxDetLen'; hence,
     * the length of the token in characters is 'MIN(maxDetLen, _gtl_detPos - _gtl_curPos)'.
     *
     * To achieve this constraint, these functions MODIFY '_gtl_detPos'. Therefore:
     *     # These functions may be called multiple times IN SEQUENCE, as long as the 'maxDetLen'
     *       in the later call is LESS THAN OR EQUAL to that in previous calls.
     *     # After calling these functions, other '_ww*()' functions must not be called without
     *       first RESTARTING the detection process by calling the '_gtl_detPos_WholeWord()' function.
     */

    private char _wwChar(final int maxDetLen)
    { _wwLenLimit(maxDetLen); return _wwChar(); }

    private String _wwWord(final int maxDetLen)
    { _wwLenLimit(maxDetLen); return _wwWord(); }

    private String _wwOper(final int maxDetLen)
    { _wwLenLimit(maxDetLen); return _wwOper(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * These functions EXTRACT a token from a sequence of characters in the line buffer using the
     * current position '_gtl_curPos' and the position after the detected token '_gtl_detPos'.
     *
     * These functions forcibly limit the resulting token length to a maximum of 'maxDetLen'; hence,
     * the length of the token in characters is 'MIN(maxDetLen, _gtl_detPos - _gtl_curPos)'.
     *
     * To enforce this constraint, these functions SAVE and then MODIFY '_gtl_detPos'. Therefore:
     *     # These functions may be called multiple times IN SEQUENCE, as long as the 'maxDetLen'
     *       in the later call is LESS THAN OR EQUAL to that in previous calls.
     *     # After calling these functions, other '_ww*()' functions must not be called without
     *       first either:
     *           # RESTARTING the detection process by calling the '_gtl_detPos_WholeWord()' function.
     *           # RESTORING '_gtl_detPos' by calling the '_wwLenRestore()' function.
     */

    private char _wwCharSv(final int maxDetLen)
    { _wwLenLimitSave(maxDetLen); return _wwChar(); }

    private String _wwWordSv(final int maxDetLen)
    { _wwLenLimitSave(maxDetLen); return _wwWord(); }

    private String _wwOperSv(final int maxDetLen)
    { _wwLenLimitSave(maxDetLen); return _wwOper(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _wwChar_impl(final char ref, final boolean sv)
    {
        final int save = _gtl_detPos;

        if(sv) _wwLenLimitSave(1);
        else   _wwLenLimit    (1);
        if( _wwChar() == ref ) return true;

        _gtl_detPos = save;
        return false;
    }

    private boolean _wwWord_impl(final String ref, final boolean sv)
    {
        final int save = _gtl_detPos;

        if(sv) _wwLenLimitSave( ref.length() );
        else   _wwLenLimit    ( ref.length() );
        if( ref.equals( _wwWord() ) ) return true;

        _gtl_detPos = save;
        return false;
    }

    private boolean _wwOper_impl(final String ref, final boolean sv)
    {
        final int save = _gtl_detPos;

        if(sv) _wwLenLimitSave( ref.length() );
        else   _wwLenLimit    ( ref.length() );
        if( ref.equals( _wwOper() ) ) return true;

        _gtl_detPos = save;
        return false;
    }

    private boolean _wwOper_impl(final char ref, final boolean sv)
    {
        final int save = _gtl_detPos;

        if(sv) _wwLenLimitSave(1);
        else   _wwLenLimit    (1);
        if( _wwChar() == ref ) return true;

        _gtl_detPos = save;
        return false;
    }

    /*
     * These functions MATCH the given reference token with the token extracted from a sequence of
     * characters in the line buffer using the current position '_gtl_curPos' and the position after
     * the detected token '_gtl_detPos'.
     *
     * These functions forcibly limit the extracted token length to a maximum of 'ref.length'; hence,
     * the length of the extracted token in characters is 'MIN(ref.length, _gtl_detPos - _gtl_curPos)'.
     *
     * To achieve this constraint, these functions MODIFY '_gtl_detPos' before comparing and restore
     * '_gtl_detPos' if there is no match. Therefore, if these functions return true (which indicates
     * a match):
     *     # These functions may be called multiple times IN SEQUENCE, as long as the length of the
     *       reference token in the later call is LESS THAN OR EQUAL to that in previous calls.
     *     # After calling these functions, other '_ww*()' functions must not be called without
     *       first RESTARTING the detection process by calling the '_gtl_detPos_WholeWord()' function.
     */

    private boolean _wwChar(final char ref)
    { return _wwChar_impl(ref, false); }

    private boolean _wwWord(final String ref)
    { return _wwWord_impl(ref, false); }

    private boolean _wwOper(final String ref)
    { return _wwOper_impl(ref, false); }

    private boolean _wwOper(final char ref)
    { return _wwOper_impl(ref, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * These functions MATCH the given reference token with the token extracted from a sequence of
     * characters in the line buffer using the current position '_gtl_curPos' and the position after
     * the detected token '_gtl_detPos'.
     *
     * These functions forcibly limit the extracted token length to a maximum of 'ref.length'; hence,
     * the length of the extracted token in characters is 'MIN(ref.length, _gtl_detPos - _gtl_curPos)'.
     *
     * To achieve this constraint, these functions SAVE and then MODIFY '_gtl_detPos' before comparing
     * and restore '_gtl_detPos' if there is no match. Therefore, if these functions return true (which
     * indicates a match):
     *     # These functions may be called multiple times IN SEQUENCE, as long as the length of the
     *       reference token in the later call is LESS THAN OR EQUAL to that in previous calls.
     *     # After calling these functions, other '_ww*()' functions must not be called without
     *       first either:
     *           # RESTARTING the detection process by calling the '_gtl_detPos_WholeWord()' function.
     *           # RESTORING '_gtl_detPos' by calling the '_wwLenRestore()' function.
     */

    private boolean _wwCharSv(final char ref)
    { return _wwChar_impl(ref, true); }

    private boolean _wwWordSv(final String ref)
    { return _wwWord_impl(ref, true); }

    private boolean _wwOperSv(final String ref)
    { return _wwOper_impl(ref, true); }

    private boolean _wwOperSv(final char ref)
    { return _wwOper_impl(ref, true); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static enum Context {
        __NONE__              ,

        OptionDef_Key1        ,
        OptionDef_Key2        ,
        OptionDef_Specifier   ,

        Include_Specifier     ,

        Pragma_Key            ,
        Pragma_Specifier      ,

        VariableEval          ,
        RVariableEval         ,
        SVariableEval         ,
        SVariableEvalCommon   ,
        VariableEvalN         ,

        RegExpr1              ,
        RegExpr2              ,
        RegExpr2_PlainRVarEval,

        RQString              ,
        SQString              ,
        DQString              ,
        RVariableEvalDQS      ,
        SVariableEvalDQS      ,

        BFunctionCall         ,
        UFunctionCall1        ,

        VarAssign             ,

        EvalStatement2

    }; // enum Context

    private Stack<Context> _contextStack = new Stack<>();

    private boolean _ctxEmpty()
    { return _contextStack.isEmpty(); }

    private void _ctxClear()
    {
        _contextStack.clear();
        _gtl_curTokReset();
    }

    private void _ctxPush(final Context ctx)
    {
        _contextStack.push(ctx);
        _gtl_curTokReset();
    }

    private void _ctxPop(final int n)
    {
        for(int i = 0; i < n; ++i) _contextStack.pop();
        _gtl_curTokReset();
    }

    private void _ctxPop()
    { _ctxPop(1); }

    private void _ctxPopPush(final int n, final Context ctx)
    {
        _ctxPop(n);
        _contextStack.push(ctx);
    }

    private void _ctxPopPush(final Context ctx)
    {
        _ctxPop();
        _contextStack.push(ctx);
    }

    private boolean _ctxIs(final Context context)
    {
        if( _contextStack.isEmpty() ) return context == Context.__NONE__;

        return _contextStack.peek() == context;
    }

    public boolean _ctxIsBetween(final Context fromInclusive, final Context toInclusive)
    {
        if( _contextStack.isEmpty() ) return false;

        return _contextStack.peek().compareTo(fromInclusive) >= 0 && _contextStack.peek().compareTo(toInclusive) <= 0;
    }

    private boolean _ctxIn(final Set<Context> ctxSet)
    {
        if( _contextStack.isEmpty() ) return ctxSet.contains(Context.__NONE__);

        return ctxSet.contains( _contextStack.peek() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Set<Context> _ctxSupport_Keyword      = EnumSet.of(Context.__NONE__, Context.VarAssign                                               );
    private static final Set<Context> _ctxSupport_Bool         = EnumSet.of(Context.__NONE__, Context.VarAssign, Context.EvalStatement2                       );
    private static final Set<Context> _ctxSupport_Number       = EnumSet.of(Context.__NONE__, Context.VarAssign, Context.EvalStatement2, Context.VariableEvalN);
    private static final Set<Context> _ctxSupport_StringEval   = EnumSet.of(Context.__NONE__, Context.VarAssign                                               );
    private static final Set<Context> _ctxSupport_StringMLEval = EnumSet.of(Context.__NONE__, Context.VarAssign                                               );
    private static final Set<Context> _ctxSupport_PROperEval   = EnumSet.of(Context.__NONE__, Context.VarAssign,                         Context.DQString     );
    private static final Set<Context> _ctxSupport_SROperEvalS  = EnumSet.of(Context.__NONE__                                                                  );
    private static final Set<Context> _ctxSupport_SROperEvalD  = EnumSet.of(Context.__NONE__, Context.VarAssign                                               );
    private static final Set<Context> _ctxSupport_VariableEval = EnumSet.of(Context.__NONE__, Context.VarAssign, Context.EvalStatement2                       );
    private static final Set<Context> _ctxSupport_FunctionEval = EnumSet.of(Context.__NONE__, Context.VarAssign                                               );

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # The '_ctxChkd_*()'     functions perform token detection only.
     *        # The '_ctxTask_*()'     functions may perform token detection and always perform token storage.
     *        # The '_ctxTask_inc_*()' functions are the same as the '_ctxTask_*()' functions but are meant
     *                                 to be called only from inside the other '_ctxTask_*()' functions.
     */

    // NOTE : This function always returns true for ease of use
    private boolean _ctxTask_ERROR()
    {
        if(_gtl_curPos <= _gtl_segment.offset) {
            // ##### !!! TODO : 'ct' and 'lt' are always 'null' - VERIFY !!! #####
            Token ct = firstToken;
            Token lt = null;
            while(ct != null) {
                if( ct.isPaintable() ) lt = ct;
                ct = ct.getNextToken();
            }
            if(lt != null) lt.setType(TType.ErrorText);
            else           _addEmptyToken(TType.ErrorText, _gtl_newOfs + _gtl_curPos);
        }
        else {
            _addTokenCP_EP(_gtl_curPos, _gtl_endPos - 1, TType.ErrorText);
        }
        _ctxClear();

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxChkd_Whitespaces()
    {
        if( _gtl_detPos_storeIfValid( _scanws(_gtl_array, _gtl_curPos, _gtl_endPos) ) >= 0 ) {
            _gtl_curTok = TType.Whitespace;
            return true;
        }

        return false;
    }

    private void _ctxTask_inc_CombineNL()
    {
        _gtl_detPos = _gtl_curPos + 1;
        if(_gtl_curPos == _gtl_endPos - 2) ++_gtl_detPos;

        _addTokenCP_DP(_gtl_curPos, _gtl_endPos - 1, true ? TType.CombineNL : TType.ErrorText);

        if(_gtl_endPosSave > _gtl_endPos) {
            _gtl_detPos = _gtl_endPosSave;
            _gtl_endPos = _gtl_endPosSave;
            _addTokenCP_DP(_gtl_curPos, _gtl_endPos - 1);
        }
        else {
            _addEmptyTokenEP();
        }

        _gtl_lineContinue = false;
    }

    private boolean _ctxTask_CombineNL()
    {
        if(   ( !_gtl_lineContinue                  )
           || (    (_gtl_curPos != _gtl_endPos - 2)
                && (_gtl_detPos != _gtl_endPos - 1)
              )
        ) return false;

        _gtl_curTok = TType.CombineNL;

        _ctxTask_inc_CombineNL();

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxChkd_CommentEval()
    {
        /*
        <context name="CommentEval">
            <StringDetect String="(*" attribute="Comment" context="MLComment" beginRegion="MLComment"/>
            <DetectChar   char="#"    attribute="Comment" context="Comment"                          />
        </context>
        */

        // Detect multi-line comment - opening marker
        if( _gtl_detPos_storeIfValid( _detect("(*", _gtl_array, _gtl_curPos, _gtl_endPos) ) >= 0 ) {
           _gtl_curTok          = TType.Comment;
           _gtl_nestedMLComment = 1;
           return true;
        }

        // Detect single-line comment
        if( _gtl_detPos_storeIfValid( _detect('#' , _gtl_array, _gtl_curPos, _gtl_endPos) ) >= 0 ) {
           _gtl_curTok          = TType.Comment;
           _gtl_nestedMLComment = 0;
           return true;
        }

        // Nothing detected
        return false;
    }

    private void _ctxTask_Comment()
    {
        /*
        <context name="Comment" attribute="Comment" lineEndContext="#pop">
            <LineContinue attribute="CombineNL" context="#stay"/>
        </context>
        */

        if(!_gtl_lineContinue) {
            _addTokenCP_EP(_gtl_curPos, _gtl_endPos - 1, TType.Comment);
            _ctxClear();
            return;
        }

        _gtl_detPos = _gtl_endPos - 1;
        _addTokenCP_DP(_gtl_curPos, _gtl_endPos - 2, TType.Comment);

        _ctxTask_inc_CombineNL();
    }

    private void _ctxTask_MLComment()
    {
        /*
        <context name="MLComment" attribute="Comment" lineEndContext="#stay">
            <RegExpr      String="#.*\(\*" attribute="Comment" context="#stay"                          />
            <StringDetect String="(*"      attribute="Comment" context="MLComment"                      />
            <StringDetect String="*)"      attribute="Comment" context="#pop"      endRegion="MLComment"/>
        </context>
        */

        // Detect multi-line comment - take account for nested opening markers
        int locDetPosOpen  = _gtl_detPos;
        int locDetPosClose = -1;

        while(true) {

            // Detect multi-line comment - nested opening marker
            final int posMLCOpen = (locDetPosOpen < 0) ? _gtl_curPos : locDetPosOpen;
          //final int locMLCOpen = _findfw(_pmd_MLCOpen, _gtl_array, posMLCOpen, _gtl_endPos);
            final int locMLCOpen = _findfw('#', "(*", _gtl_array, posMLCOpen, _gtl_endPos);

            if(locMLCOpen  >= 0) {
                ++_gtl_nestedMLComment;
                locDetPosOpen  = locMLCOpen;
            }

            // Detect multi-line comment - closing marker
            final int posMLCClose = (locDetPosClose < 0) ? ( (locDetPosOpen < 0) ? _gtl_curPos
                                                                                 : locDetPosOpen
                                                           )
                                                         : locDetPosClose;
            final int locMLCClose = _findfw("*)", _gtl_array, posMLCClose, _gtl_endPos);
            if(locMLCClose >= 0) {
                --_gtl_nestedMLComment;
                locDetPosClose = locMLCClose;
            }

            // Break if no more opening or closing marker is found
            if(locMLCOpen < 0 && locMLCClose < 0) break;

        } // while

        _gtl_detPos = (_gtl_nestedMLComment > 0) ? -1 : locDetPosClose;

        // Closing marker not found
        if(_gtl_detPos < 0) {
            _addTokenCP_EP(_gtl_curPos, _gtl_endPos - 1, TType.Comment);
        }
        // Closing marker found
        else {
            _gtl_detPos -= 2; _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Comment); // Put the text
            _gtl_detPos += 2; _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Comment); // Put the closing marker
            _ctxClear();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_Whitespaces()
    {
        if( !_ctxChkd_Whitespaces() ) return false;

        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Whitespace);
        _gtl_curTokReset();

        return true;
    }

    private boolean _ctxTask_ForceNL(final boolean stay)
    {
        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <DetectChar char=";" attribute="ForceNL" context="#pop"/>
            ...
        </context>
        */

        if( _wwChar(';') ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.ForceNL);
            if(!stay) _ctxClear();
            return true;
        }

        return false;
    }

    private boolean _ctxTask_ForceNL()
    { return _ctxTask_ForceNL(false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_DotWord()
    {
        if( !_ctxEmpty() ) return false;

        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <RegExpr String="\.\$(?=&symname;\b)" attribute="MacroUseMark" context="MacroUsage"/>
            ...
        </context>
        */
        if( ".$".equals( _wwWord() ) ) {
            /*
            <context name="MacroUsage" attribute="MacroUse" lineEndContext="#pop">
            </context>
            */
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.MacroUseMark);

            if( _gtl_detPos_SymbolName() < 0 ) return _ctxTask_ERROR();
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.MacroUse);

            _ctxClear();

            return true;
        }

        if( _wwChar() != '.' ) return false;
        boolean inFront = true;
        for(int i = _gtl_detPos - 2; i > 0; --i) {
            if(_gtl_array[i] == '\n') break;
            if( !RSyntaxUtilities.isWhitespace(_gtl_array[i]) ) {
                inFront = false;
                break;
            }
        }
        if(!inFront) return false;

        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.MacroDef);

        _gtl_detPos_WholeWord();
        final String wwWord = _wwWord();
        if(wwWord == null) return _ctxTask_ERROR();

        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <RegExpr String="\.option\b"     attribute="MacroDef" context="OptionDef"/>
            ...
        </context>
        */
        if( "option".equals(wwWord) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.MacroDef);
            _ctxPush(Context.OptionDef_Key1);
            return true;
        }

        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <RegExpr String="\.undefmacro\b" attribute="MacroDef" context="#stay"    />
            <RegExpr String="\.macro\b"      attribute="MacroDef" context="#stay"    />
            <RegExpr String="\.endmacro\b"   attribute="MacroDef" context="#stay"    />
            ...
        </context>
        */
        if( "undefmacro".equals(wwWord) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.MacroDef);
            _ctxClear();
            return true;
        }
        if( "macro".equals(wwWord) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.MacroDef);
            _ctxClear();
            return true;
        }
        if( "endmacro".equals(wwWord) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.MacroDef);
            _ctxClear();
            return true;
        }

        return _ctxTask_ERROR();
    }

    private boolean _ctxTask_OptionDef()
    {
        /*
        <context name="OptionDef" lineEndContext="#pop">
            <LineContinue                  attribute="CombineNL" context="#stay"     />
            <keyword      String="Options" attribute="Keyword"   context="#stay"     />
            <DetectChar   char="&apos;"    attribute="ODString"  context="ODStringSQ"/>
            <DetectChar   char="&quot;"    attribute="ODString"  context="ODStringDQ"/>
        </context>
        <context name="ODStringSQ" attribute="ODString" lineEndContext="#pop">
            <LineContinue               attribute="CombineNL" context="#stay"/>
            <DetectChar   char="&apos;" attribute="ODString"  context="#pop" />
        </context>
        <context name="ODStringDQ" attribute="ODString" lineEndContext="#pop">
            <LineContinue               attribute="CombineNL" context="#stay"/>
            <DetectChar   char="&quot;" attribute="ODString"  context="#pop" />
        </context>
        */

        if( _ctxIs(Context.OptionDef_Key1) ) {
            _gtl_detPos_WholeWord();
            if( !_OptionsKey1.containsKey( _wwWord() ) ) return _ctxTask_ERROR();

            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Keyword);

            _ctxPush(Context.OptionDef_Key2);

            return true;
        }

        if( _ctxIs(Context.OptionDef_Key2) ) {
            _gtl_detPos_WholeWord();
            final String ww = _wwWord();
            if( !_OptionsKey2.containsKey(ww) ) return _ctxTask_ERROR();

            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Keyword);

            final Integer pc = _OptionsKey2PC.get(ww);
            if(pc != null) _ctxPush(Context.OptionDef_Specifier);
            else           _ctxClear();

            return true;
        }

        if( _ctxIs(Context.OptionDef_Specifier) ) {
            if( _gtl_detPos_SimpleString() < 0 ) return _ctxTask_ERROR();

            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.ODString);
            _ctxClear();

            return true;
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_CaretWord()
    {
        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <RegExpr String="&rvmark;" attribute="RefVarMark" context="#stay"/>
            ...
        </context>
        */

        if( _wwChar() == '^' ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RefVarMark);

            if( _gtl_detPos_SymbolName() < 0 ) return _ctxTask_ERROR();
            _addTokenCP_DP( _gtl_curPos, _gtl_detPos - 1,  _ctxEmpty() ? TType.NormalText : TType.RVariableEval );
            _gtl_curTokReset();

            return true;
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_Keyword()
    {
        if(_inEvalStatment != -1) return false;

        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <RegExpr String="\+(?=jxmake\s+)" attribute="Keyword" context="#stay"/>
            <keyword String="Keywords"        attribute="Keyword" context="#stay"/>
            ...
        </context>
        */

        if( !_ctxIn(_ctxSupport_Keyword) ) return false;

        if( _ctxEmpty() && _wwChar('+') ) {
            final int curPos = _gtl_curPos;

            _gtl_curPos = _gtl_detPos;
            _gtl_detPos_WholeWord();
            if( !"jxmake".equals( _wwWord() ) ) {
                _gtl_curPos = curPos;
                return false;
            }

            _gtl_curPos = curPos;
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Keyword);
            _gtl_curTokReset();

            return true;
        }

        /*
        // NOTE : Only partially implemented because of inherent tokenizer limitations!
        <RegExpr String="\beval\b" attribute="Keyword" context="EvalStatement1"/>
        */

        final String wwWord = _wwWord();
        if( _Keywords.containsKey(wwWord) ) {

            for(int i = _gtl_detPos - 1; i > 0; --i) {
                if(_gtl_array[i] == '\n') break;
                if( !RSyntaxUtilities.isWhitespace(_gtl_array[i]) ) return false;
            }

            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Keyword);

            /*
            <RegExpr String="\becho(?:ln)\b" attribute="Keyword" context="VarAssign"/>
            */
            if( "echo".equals(wwWord) || "echoln".equals(wwWord) ) _ctxPush(Context.VarAssign);
            else                                                   _gtl_curTokReset();

            /*
            // NOTE : Only partially implemented because of inherent tokenizer limitations!
            <RegExpr String="\beval\b" attribute="Keyword" context="EvalStatement1"/>
            */
            if( "eval".equals(wwWord) ) _inEvalStatment = 0;

            return true;
        }

        return false;
    }

    private boolean _ctxTask_SVarShortcuts()
    {
        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <keyword String="SVarShortcuts" attribute="SVarShortcut" context="#stay"/>
            ...
        </context>
        */

        if( !_SVarSCuts.containsKey( _wwWord() ) ) return false;

        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVarShortcut);
        _gtl_curTokReset();

        return true;
    }

    private boolean _ctxTask_ANSIEscapeCodes()
    {
        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <keyword String="ANSIEscapeCodes" attribute="ANSIEscapeCode" context="#stay"/>
            ...
        </context>
        */

        if( !_ANSIEscapeCodes.containsKey( _wwWord() ) ) return false;

        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.ANSIEscapeCode);
        _gtl_curTokReset();

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_ColonWord()
    {
        if( !_ctxEmpty() ) return false;

        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <RegExpr String=":::s?include(_once)?(?=\s+&libname;)\b" attribute="SKeyword" context="IncludeLib"/>
            <RegExpr String=":::s?include(_once)?\b"                 attribute="SKeyword" context="#stay"     />
            <RegExpr String=":::pragma\b"                            attribute="SKeyword" context="Pragma"    />
            ...
        </context>
        */

        if( !":::".equals( _wwWord() ) ) return false;

        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SKeyword);

        _gtl_detPos_WholeWord();
        final String wwWord = _wwWord();

        if( "include".equals(wwWord) || "sinclude".equals(wwWord) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SKeyword);
            _ctxPush(Context.Include_Specifier);
            return true;
        }

        if( "pragma".equals(wwWord) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SKeyword);
            _ctxPush(Context.Pragma_Key);
            return true;
        }

        return _ctxTask_ERROR();
    }

    private boolean _ctxTask_Include()
    {
        /*
        <context name="IncludeLib" lineEndContext="#pop">
            <RegExpr String="&libname;" attribute="IncLibName" context="#pop"/>
        </context>
        */

        if( _ctxIs(Context.Include_Specifier) ) {

            if( _gtl_detPos_SimpleString() >= 0 ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.IncLibName);
                currentToken.setHyperlink(true);
                _ctxClear();
                return true;
            }

            if( _gtl_detPos_SystemIncludeString() >= 0 ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.IncLibName);
                _ctxClear();
                return true;
            }

            return _ctxTask_ERROR();

        }

        return false;
    }

    private boolean _ctxTask_Pragma()
    {
        /*
        <context name="Pragma" lineEndContext="#pop">
            <RegExpr String="&symname;" attribute="Keyword" context="#pop"/>
        </context>
        */

        if( _ctxIs(Context.Pragma_Key) ) {
            if( _gtl_detPos_SymbolName() < 0 ) return _ctxTask_ERROR();

            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Keyword);
            _ctxPush(Context.Pragma_Specifier);

            return true;
        }

        if( _ctxIs(Context.Pragma_Specifier) ) {

            while(true) {

                boolean res = false;

                _ctxTask_Whitespaces();

                if( _gtl_detPos_DecNum() >= 0 ) {
                    _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Number);
                    res = true;
                }

                if( _gtl_detPos_WholeWord() >= 0 ) {
                    _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.NormalText);
                    res = true;
                }

                if(!res) {
                    _ctxClear();
                    return true;
                }

            } // while

        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_Bool()
    {
        /*
        <context name="BoolEval" lineEndContext="#pop">
            <StringDetect String="true"  attribute="Number" context="#stay"/>
            <StringDetect String="false" attribute="Number" context="#stay"/>
        </context>
        */

        if( !_ctxIn(_ctxSupport_Bool) ) return false;

        final String wwWord = _wwWord();
        if(wwWord == null) return false;

        if( "true".equals(wwWord) || "false".equals(wwWord) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Number);
            _gtl_curTokReset();
            return true;
        }

        return false;
    }

    private boolean _ctxTask_Number()
    {
        /*
        ...
        <RegExpr String="\b&decnum;\b" attribute="Number" context="#stay"/>
        <RegExpr String="\b&hexnum;\b" attribute="Number" context="#stay"/>
        ...
        */

        if( !_ctxIn(_ctxSupport_Number) ) return false;

        if( _gtl_detPos_DecNum() >= 0 ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Number);
            _gtl_curTokReset();
            return true;
        }

        if( _gtl_detPos_HexNum() >= 0 ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Number);
            _gtl_curTokReset();
            return true;
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_StringEval()
    {
        /*
        <context name="StringEval" lineEndContext="#pop">
            <IncludeRules                                  context="StringEvalCommon"/>
            <DetectChar   char="&#96;"  attribute="String" context="RQString"        />
            <DetectChar   char="&apos;" attribute="String" context="SQString"        />
            <DetectChar   char="&quot;" attribute="String" context="DQString"        />
        </context>
        */
        if( _ctxIn(_ctxSupport_StringEval) ) {

            if( _ctxTask_inc_StringEvalCommon() ) return true;

            final char wwChar = _wwCharSv(1);
            if(wwChar == '`') {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _ctxPush(Context.RQString);
                return true;
            }
            if(wwChar == '\'') {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _ctxPush(Context.SQString);
                return true;
            }
            if(wwChar == '"') {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _ctxPush(Context.DQString);
                return true;
            }

            return _wwLenRestore();
        }

        /*
        <context name="RQString" attribute="String" lineEndContext="#pop">
            <LineContinue              attribute="CombineNL" context="#stay"/>
            <DetectChar   char="&#96;" attribute="String"    context="#pop" />
        </context>
        */
        if( _ctxIs(Context.RQString) ) {
            if( _wwChar('`') ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _ctxPop();
            }
            else {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _gtl_curTokReset();
            }
            return true;
        }

        /*
        <context name="SQString" attribute="String" lineEndContext="#pop">
            <LineContinue               attribute="CombineNL" context="#stay"         />
            <IncludeRules                                     context="SQStringCommon"/>
            <DetectChar   char="&apos;" attribute="String"    context="#pop"          />
        </context>
        */
        if( _ctxIs(Context.SQString) ) {

            if( _ctxTask_inc_SQStringCommon() ) return true;

            if( _wwChar('\'') ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _ctxPop();
            }
            else {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _gtl_curTokReset();
            }

            return true;
        }

        /*
        <context name="DQString" attribute="String" lineEndContext="#pop">
            <LineContinue               attribute="CombineNL" context="#stay"         />
            <IncludeRules                                     context="DQStringCommon"/>
            <DetectChar   char="&quot;" attribute="String"    context="#pop"          />
        </context>
        */
        if( _ctxIs(Context.DQString) ) {

            if( _ctxTask_inc_DQStringCommon() ) return true;

            if( _wwChar('"') ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _ctxPop();
            }
            else {
                if( !RSyntaxUtilities.isLetterOrDigit( _wwOper().charAt(0) ) ) _gtl_detPos = _gtl_curPos + 1;
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.String);
                _gtl_curTokReset();
            }

            return true;
        }

        if( _ctxIsBetween(Context.RVariableEvalDQS, Context.SVariableEvalDQS) ) {
            if( _ctxTask_inc_VariableEvalDQS() ) return true;
        }

        return false;
    }

    private boolean _ctxTask_inc_StringEvalCommon()
    {
        /*
        <context name="StringEvalCommon">
            <RegExpr String="\.(?=&quot;)" attribute="StringDQF" context="#stay"/>
            <RegExpr String=":(?=&quot;)"  attribute="StringDQF" context="#stay"/>
        </context>
        */

        final String wwOper = _wwOperSv(2);
        if(    ( wwOper           == null )
            || ( wwOper.length( ) != 2    )
            || ( wwOper.charAt(1) != '"'  )
        ) return _wwLenRestore();

        if( ".:".indexOf( wwOper.charAt(0) ) != -1 ) {
            --_gtl_detPos;
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.StringDQF);
            _gtl_curTokReset();
            return true;
        }

        return _wwLenRestore();
    }

    private boolean _ctxTask_inc_SQStringCommon()
    {
        /*
        <context name="SQStringCommon">
            <Detect2Chars char="\" char1="&apos;" attribute="EscapeSeq" context="#stay"/>
            <Detect2Chars char="\" char1="\"      attribute="EscapeSeq" context="#stay"/>
        </context>
        */

        final String wwOper = _wwOperSv(2);
        if(    ( wwOper           == null )
            || ( wwOper.length( ) != 2    )
            || ( wwOper.charAt(0) != '\\' )
        ) return _wwLenRestore();

        if( "'\\".indexOf( wwOper.charAt(1) ) != -1 ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.EscapeSeq);
            _gtl_curTokReset();
            return true;
        }

        return _wwLenRestore();
    }

    private boolean _ctxTask_inc_DQStringCommon()
    {
        /*
        <context name="DQStringCommon">
            <Detect2Chars char="\"      char1="t"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="v"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="r"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="n"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="f"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="b"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="&apos;" attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="&quot;" attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="\"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="$"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="~"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="^"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="-"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="+"      attribute="EscapeSeq" context="#stay"          />
            <Detect2Chars char="\"      char1="?"      attribute="EscapeSeq" context="#stay"          />
            <RegExpr      String="\\o[0-7]{3}"         attribute="EscapeSeq" context="#stay"          />
            <RegExpr      String="\\d[0-9]{3}"         attribute="EscapeSeq" context="#stay"          />
            <RegExpr      String="\\x&hexchr;{2}"      attribute="EscapeSeq" context="#stay"          />
            <RegExpr      String="\\u&hexchr;{4}"      attribute="EscapeSeq" context="#stay"          />
            <RegExpr      String="\\U&hexchr;{6}"      attribute="EscapeSeq" context="#stay"          />
            <IncludeRules                                                    context="VariableEvalDQS"/>
            <IncludeRules                                                    context="PROperEval"     />
        </context>
        */

        if( _ctxTask_inc_PROperEval() ) return true;

        if( _gtl_detPos_Generic(_pmd_ESeqSimple, _pmd_ESeqOct3, _pmd_ESeqDec3, _pmd_ESeqHex2, _pmd_ESeqUni4, _pmd_ESeqUni6) != -1 ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.EscapeSeq);
            _gtl_curTokReset();
            return true;
        }

        if( _ctxTask_inc_VariableEvalDQS() ) return true;

        return false;
    }

    private boolean _ctxTask_StringMLEval()
    {
        if( !_ctxIn(_ctxSupport_StringMLEval) ) return false;

        /*
        <context name="StringMLEval" lineEndContext="#stay">
            <StringDetect String="[[&quot;" attribute="StringMLMarker" context="DQStringML" beginRegion="DQStringML"/>
        </context>
        <context name="DQStringML" attribute="String" lineEndContext="#stay">
            <IncludeRules                                              context="DQStringCommon"                       />
            <StringDetect String="&quot;]]" attribute="StringMLMarker" context="#pop"           endRegion="DQStringML"/>
        </context>
        */
        final String wwOper = _wwOperSv(3);

        if( "[[\"".equals(wwOper) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.StringMLMarker);
            _inStringML = true;
            return true;
        }

        if( "\"]]".equals(wwOper) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.StringMLMarker);
            _inStringML = false;
            return true;
        }

        return _wwLenRestore();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    <context name="LabelEval" attribute="NormalText" lineEndContext="#pop">
        <LineContinue                        attribute="CombineNL" context="#stay"/>
        <RegExpr      String="\b&symname;\b"                       context="#stay"/>
        <RegExpr      String="\s+:\s*"       attribute="ROperator" context="#pop" />
    </context>
    */
    // NOTE : Not implemented because of inherent tokenizer limitations!

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_EvalStatement()
    {
        if(_inEvalStatment == -1) return false;

        /*
        <context name="EvalStatement1" lineEndContext="#pop">
            <LineContinue                    attribute="CombineNL" context="#stay"              />
            <IncludeRules                                          context="CommentEval"        />
            <DetectChar   char=";"           attribute="ForceNL"   context="#pop"               />
            <Detect2Chars char=":" char1="=" attribute="ROperator" context="#pop!EvalStatement2"/>
        </context>
        <context name="EvalStatement2" lineEndContext="#pop">
            <LineContinue                                                                                 attribute="CombineNL"  context="#stay"           />
            <IncludeRules                                                                                                        context="CommentEval"     />
            <DetectChar   char=";"                                                                        attribute="ForceNL"    context="#pop"            />
            <RegExpr      String="\b&decnum;\b"                                                           attribute="Number"     context="#stay"           />
            <RegExpr      String="\b&hexnum;\b"                                                           attribute="Number"     context="#stay"           />
            <keyword      String="SVarShortcuts"                                                          attribute="NormalText" context="#stay"           />
            <IncludeRules                                                                                                        context="BoolEval"        />
            <IncludeRules                                                                                                        context="VariableEval "   />
            <!-- Inline Function -->
            <RegExpr      String="\s+&unaryoper;*(?=&symname;&evalifbr;)"                                 attribute="SOperator"  context="EvalStatementIF1"/>
            <RegExpr      String="\s+(?:\](?=\s+)|\]$)"                                                   attribute="SOperator"  context="#stay"           />
            <DetectChar   char=","                                                                        attribute="SOperator"  context="#stay"           />
            <!-- Unary and Binary Operators -->
            <RegExpr      String="\s+(?:&lt;=&gt;|&lt;=|&gt;=|==|!=|)(?=\s+)"                             attribute="SOperator"  context="#stay"           />
            <RegExpr      String="\s+(?:\*{1,2}|[/%+-]|&lt;{1,2}|&gt;{1,2}|&amp;{1,2}|\^|\|{1,2})(?=\s+)" attribute="SOperator"  context="#stay"           />
            <RegExpr      String="(?:\s+|\()&unaryoper;+(?=\S+)"                                          attribute="SOperator"  context="#stay"           />
            <AnyChar      String="()"                                                                     attribute="SOperator"  context="#stay"           />
            <!-- Ternary Operator -->
            <RegExpr      String="\s+[?:](?=\s+)"                                                         attribute="SOperator"  context="#stay"           />
        </context>
        <context name="EvalStatementIF1" lineEndContext="#pop">
            <LineContinue                                  attribute="CombineNL"   context="#stay"                />
            <RegExpr      String="&symname;(?=&evalifbr;)" attribute="SOperatorIF" context="#pop!EvalStatementIF2"/>
        </context>
        <context name="EvalStatementIF2" lineEndContext="#pop">
            <LineContinue                                  attribute="CombineNL"   context="#stay"/>
            <RegExpr      String="&evalifbr;"              attribute="SOperator"   context="#pop" />
        </context>
        */
        // NOTE : Only partially implemented because of inherent tokenizer limitations!

        if( _EvalFunc.contains( _wwWord() ) ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.ROperator);
            _gtl_curTokReset();
            return true;
        }

        for(int i = 3; i > 0; --i) {

            final String wwOper = _wwOperSv(i);

            if( ":=".equals(wwOper) ) {
                if(_inEvalStatment != 0) return _wwLenRestore();
                _inEvalStatment = 1;
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.ROperator);
                _gtl_curTokReset();
                return true;
            }

            if( _EvalOper.contains(wwOper) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SOperator);
                _gtl_curTokReset();
                return true;
            }

           _wwLenRestore();

        } // for

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_CmdEval()
    {
        /*
        <context name="NormalText" attribute="NormalText" lineEndContext="#pop">
            ...
            <RegExpr    String="(?:[-+?]|-\+|\+-)(?=@)" firstNonSpace="true" attribute="CommandSpcXDir" context="CommandEvalSE"/>
            <DetectChar char="@"                        firstNonSpace="true" attribute="Command"        context="CommandEval"  />
            ...
        </context>
        */
        if( !_ctxEmpty() ) return false;

        /*
        <context name="CommandEvalSE" attribute="Command" lineEndContext="#pop">
            <LineContinue        attribute="CombineNL" context="#stay"           />
            <DetectChar char="@" attribute="Command"   context="#pop!CommandEval"/>
        </context>
        <context name="CommandEval" attribute="Command" lineEndContext="#pop">
            <LineContinue                 attribute="CombineNL" context="#stay"       />
            <IncludeRules                                       context="CommentEval" />
            <DetectChar   char=";"        attribute="ForceNL"   context="#pop"        />
            <RegExpr      String="[\r\n]" attribute="Command"   context="#pop"        />
            <StringDetect String="clrenv" attribute="Keyword"   context="#stay"       />
            <StringDetect String="delenv" attribute="Keyword"   context="#stay"       />
            <StringDetect String="setenv" attribute="Keyword"   context="#stay"       />
            <StringDetect String="addenv" attribute="Keyword"   context="#stay"       />
            <StringDetect String="sstdin" attribute="Keyword"   context="#stay"       />
            <StringDetect String="jxmake" attribute="Keyword"   context="#stay"       />
            <IncludeRules                                       context="StringEval"  />
            <IncludeRules                                       context="VariableEval"/>
            <IncludeRules                                       context="PROperEval"  />
            <IncludeRules                                       context="SROperEvalS" />
            <IncludeRules                                       context="SROperEvalD" />
        </context>
        */
        // NOTE : Only partially implemented because of inherent tokenizer limitations!

        if( _gtl_detPos_Generic(_pmd_CmdEval) == -1 ) return false;

        final String wwWord = _wwWord();

        if( wwWord.charAt(0) != '@' ) {
            final int detPos = _gtl_detPos;
                 if( wwWord.charAt(1) == '@' ) _gtl_detPos = _gtl_curPos + 1;
            else if( wwWord.charAt(2) == '@' ) _gtl_detPos = _gtl_curPos + 2;
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.CommandSpcXDir);
            _gtl_curPos = _gtl_detPos;
            _gtl_detPos = detPos;
        }

        if(true) {
            final int detPos = _gtl_detPos;
            _gtl_detPos = _gtl_curPos + 1;
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Command);
            _gtl_curPos = _gtl_detPos;
            _gtl_detPos = detPos;
        }

        final boolean spcCmd = _SpcCmds.contains( _wwWord() );
        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, spcCmd ? TType.Keyword : TType.Command);
        _gtl_curTokReset();

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_Operator()
    {
        //*
        if( !_ctxEmpty() ) return false;
        //*/

        /*
        if( _ctxIs(Context.VariableEvalN) ) return false;
        //*/

        /*
        <context name="OperatorEval" lineEndContext="#pop">
            <RegExpr      String="\s+=(?=\s)"    attribute="ROperator"  context="#pop!VarAssign"/>
            <RegExpr      String="\s+\+=(?=\s)"  attribute="ROperator"  context="#pop!VarAssign"/>
            <RegExpr      String="\s+\?=(?=\s)"  attribute="ROperator"  context="#pop!VarAssign"/>
            <RegExpr      String="\s+:=(?=\s)"   attribute="ROperator"  context="#pop!VarAssign"/>
            <RegExpr      String="\s+\+:=(?=\s)" attribute="ROperator"  context="#pop!VarAssign"/>
            <RegExpr      String="\s+\?:=(?=\s)" attribute="ROperator"  context="#pop!VarAssign"/>
            <RegExpr      String="\s+:\+=(?=\s)" attribute="ROperator"  context="#pop!VarAssign"/>
            <RegExpr      String="\s+:\?=(?=\s)" attribute="ROperator"  context="#pop!VarAssign"/>
            <Detect2Chars char="\" char1="?"     attribute="NormalText" context="#stay"         />
            <Detect2Chars char="\" char1="%"     attribute="NormalText" context="#stay"         />
            <RegExpr      String="\s+:\s"        attribute="SOperator"  context="#stay"         />
            <DetectChar   char="?"               attribute="SOperator"  context="#stay"         />
            <RegExpr      String="%[1-9]?"       attribute="SOperator"  context="#stay"         />
            <DetectChar   char="!"               attribute="SOperator"  context="#stay"         />
            <DetectChar   char="&amp;"           attribute="SOperator"  context="#stay"         />
            <DetectChar   char="("               attribute="ROperator"  context="#stay"         />
            <DetectChar   char=")"               attribute="ROperator"  context="#stay"         />
            <DetectChar   char=","               attribute="ROperator"  context="#stay"         />
            <RegExpr      String="\s+&lt;\s"     attribute="ROperator"  context="#stay"         />
            <RegExpr      String="\s+&lt;=\s"    attribute="ROperator"  context="#stay"         />
            <RegExpr      String="\s+&gt;\s"     attribute="ROperator"  context="#stay"         />
            <RegExpr      String="\s+&gt;=\s"    attribute="ROperator"  context="#stay"         />
            <RegExpr      String="\s+&amp;?==\s" attribute="ROperator"  context="#stay"         />
            <RegExpr      String="\s+&amp;?!=\s" attribute="ROperator"  context="#stay"         />
            <IncludeRules                                               context="PROperEval"    />
            <IncludeRules                                               context="SROperEvalS"   />
        </context>
        */

        for(int i = 3; i > 0; --i) {

            final String wwOper = _wwOperSv(i);

            if( _ROperator.containsKey(wwOper) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.ROperator);
                if( wwOper.indexOf('=') != -1 ) _ctxPush(Context.VarAssign);
                else                            _gtl_curTokReset();
                return true;
            }

            if( _SOperator.containsKey(wwOper) ) {
                if( wwOper.charAt(0) == '%' ) {
                    final char chk  = _gtl_array[_gtl_detPos];
                    if(chk >= '1' && chk <= '9') ++_gtl_detPos;
                }
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SOperator);
                _gtl_curTokReset();
                return true;
            }

            _wwLenRestore();

        } // for

        if( _ctxTask_inc_PROperEval () ) return true;
        if( _ctxTask_inc_SROperEvalS() ) return true;

        return false;
    }

    private boolean _ctxTask_inc_PROperEval()
    {
        if( !_ctxIn(_ctxSupport_PROperEval) ) return false;

        /*
        <context name="PROperEval" lineEndContext="#pop">
            <RegExpr String="[~\^\-](?=\$[{\[])" attribute="PROperEval" context="#stay"/>
        </context>
        */

        final String wwOper = _wwOperSv(3);
        if(    ( wwOper           == null                           )
            || ( wwOper.length( ) != 3                              )
            || ( wwOper.charAt(1) != '$'                            )
            || ( wwOper.charAt(2) != '{' && wwOper.charAt(2) != '[' )
        ) return _wwLenRestore();

        if( "~^-".indexOf( wwOper.charAt(0) ) != -1 ) {
            _gtl_detPos -= 2;
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.PROperEval);
            _gtl_curTokReset();
            return true;
        }

        return _wwLenRestore();
    }

    private boolean _ctxTask_inc_SROperEvalS()
    {
        if( !_ctxIn(_ctxSupport_SROperEvalS) ) return false;

        /*
        <context name="SROperEvalS" lineEndContext="#pop">
            <RegExpr String="(?:\{|\})(?=\s+|[,)]|$)" attribute="PROperEval" context="#stay"/>
        </context>
        */

        final char wwOper = _wwCharSv(1);

        if(wwOper != '{' && wwOper != '}') return _wwLenRestore();

        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.PROperEval);
        _gtl_curTokReset();

        return true;

        /*
        final String wwOper = _wwOper();
        if(wwOper == null) return false;

        final int wwLen = wwOper.length();
        if( wwLen > 1 && wwOper.charAt(1) != ',' ) return false;

        if( "{}".indexOf( wwOper.charAt(0) ) != -1 ) {
            if(wwLen > 1)  _gtl_detPos -= (wwLen - 1);
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.PROperEval);
            _gtl_curTokReset();
            return true;
        }

        return false;
        */
    }

    private boolean _ctxTask_SROperEvalD()
    {
        if( !_ctxIn(_ctxSupport_SROperEvalD) ) return false;

        /*
        <context name="SROperEvalD" lineEndContext="#pop">
            <RegExpr String="(?:\{\{|\}\})(?=\s+|[,)]|$)" attribute="PROperEval" context="#stay"/>
        </context>
        */

        final String wwOper = _wwOperSv(2);

        if( !"{{".equals(wwOper) && !"}}".equals(wwOper) ) return _wwLenRestore();

        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.PROperEval);
        _gtl_curTokReset();

        return true;

        /*
        final String wwOper = _wwOper();
        if(wwOper == null) return false;

        final int wwLen = wwOper.length();
        if( wwLen > 2 && wwOper.charAt(2) != ',' ) return false;

        if( wwOper.startsWith("{{") || wwOper.startsWith("}}") ) {
            if(wwLen > 2)  _gtl_detPos -= (wwLen - 2);
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.PROperEval);
            _gtl_curTokReset();
            return true;
        }

        return false;
        */
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_inc_RVariableEvalCommon()
    {
        /*
        <context name="RVariableEvalCommon">
            <RegExpr    String="&rvmark;"  attribute="RefVarMark"    context="#stay"   />
            <RegExpr    String="&symname;" attribute="RVariableEval" context="#stay"   />
            <DetectChar char="/"           attribute="RegExprM"      context="RegExpr1"/>
        </context>
        */

        if( _ctxTask_CaretWord() ) return true;

        if( _gtl_detPos_SymbolName() >= 0 ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
            _gtl_curTokReset();
            return true;
        }

      //_gtl_detPos_WholeWord();
        if( _wwChar(1) == '/' ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RegExprM);
            _ctxPush(Context.RegExpr1);
            return true;
        }

        return _ctxTask_ERROR();
    }

    private boolean _ctxTask_inc_SVariableEvalCommon()
    {
        /*
        <context name="SVariableEvalCommon">
            <keyword    String="SVars"           attribute="SVariableEval" context="#stay"   />
            <RegExpr    String="preq[\^*?+%~]"   attribute="SVariableEval" context="#stay"   />
            <RegExpr    String="preq[1-9][0-9]*" attribute="SVariableEval" context="#stay"   />
            <RegExpr    String="preq:&symname;"  attribute="SVariableEval" context="#stay"   />
            <DetectChar char="/"                 attribute="RegExprM"      context="RegExpr1"/>
        </context>
        */

        final String wwWord = _wwWord();

        if(wwWord != null) {

            if( JxMakeTokenMaker_Constants._SVars.containsKey(wwWord) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);
                _gtl_curTokReset();
                return true;
            }

            if( wwWord.startsWith("preq") ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);

                _gtl_detPos_WholeWord();
                final char wwChar = _wwChar(1);
                if(wwChar != ']') {
                    if( "^*?+%~".indexOf(wwChar) != -1 ) {
                        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);
                    }
                    else if(wwChar == ':') {
                        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);
                        if( _gtl_detPos_SymbolName() < 0 ) return _ctxTask_ERROR();
                        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);
                    }
                    else {
                        return _ctxTask_ERROR();
                    }
                }

                _gtl_curTokReset();
                return true;
            }

        }

        if( _wwChar(1) == '/' ) {
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RegExprM);
            _ctxPush(Context.RegExpr1);
            return true;
        }

        return _ctxTask_ERROR();
    }

    private boolean _ctxTask_VariableEval()
    {
        /*
        <context name="VariableEval" lineEndContext="#pop">
            <Detect2Chars char="$" char1="{" attribute="RVariableEval" context="RVariableEval"/>
            <Detect2Chars char="$" char1="[" attribute="SVariableEval" context="SVariableEval"/>
        </context>
        */
        if( _ctxIn(_ctxSupport_VariableEval) ) {
            final String wwOper = _wwOper(2);
            if( "${".equals(wwOper) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
                _ctxPush(Context.RVariableEval);
                return true;
            }
            if( "$[".equals(wwOper) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);
                _ctxPush(Context.SVariableEval);
                return true;
            }
        }

        /*
        <context name="RVariableEval" lineEndContext="#pop">
            <LineContinue                     attribute="CombineNL"     context="#stay"              />
            <RegExpr      String="\}(?!&lt;)" attribute="RVariableEval" context="#pop"               />
            <RegExpr      String="\}(?=&lt;)" attribute="RVariableEval" context="#pop!VariableEvalN" />
            <IncludeRules                                               context="RVariableEvalCommon"/>
        </context>
        */
        if( _ctxIs(Context.RVariableEval) ) {
            if( _wwOper("}<") ) {
                _addTokenCP   (_gtl_curPos    , _gtl_detPos - 2, TType.RVariableEval);
                _addTokenCP_DP(_gtl_curPos + 1, _gtl_detPos - 1, TType.VariableEvalN);
                _ctxPopPush(Context.VariableEvalN);
            }
            else if( _wwOper('}') ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
                _ctxClear();
            }
            else {
               _ctxTask_inc_RVariableEvalCommon();
            }
            return true;
        }

        /*
        <context name="SVariableEval" lineEndContext="#pop">
            <LineContinue                     attribute="CombineNL"     context="#stay"              />
            <RegExpr      String="\](?!&lt;)" attribute="SVariableEval" context="#pop"               />
            <RegExpr      String="\](?=&lt;)" attribute="SVariableEval" context="#pop!VariableEvalN" />
            <IncludeRules                                               context="SVariableEvalCommon"/>
        </context>
        */
        if( _ctxIs(Context.SVariableEval) ) {
            if( _wwOper("]<") ) {
                _addTokenCP   (_gtl_curPos    , _gtl_detPos - 2, TType.SVariableEval);
                _addTokenCP_DP(_gtl_curPos + 1, _gtl_detPos - 1, TType.VariableEvalN);
                _ctxPopPush(Context.VariableEvalN);
            }
            else if( _wwOper(']') ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);
                _ctxClear();
            }
            else {
                _ctxTask_inc_SVariableEvalCommon();
            }
            return true;
        }

        /*
        <context name="VariableEvalN" attribute="RVariableEval" lineEndContext="#pop">
            <LineContinue                       attribute="CombineNL"     context="#stay"/>
            <DetectChar   char="&lt;"           attribute="VariableEvalN" context="#stay"/>
            <DetectChar   char="&gt;"           attribute="VariableEvalN" context="#pop" />
            <DetectChar   char=","              attribute="VariableEvalN" context="#stay"/>
            <DetectChar   char=":"              attribute="VariableEvalN" context="#stay"/>
            <RegExpr      String="\b&decnum;\b" attribute="Number"        context="#stay"/>
            <RegExpr      String="\b&hexnum;\b" attribute="Number"        context="#stay"/>
        </context>
        */
        if( _ctxIs(Context.VariableEvalN) ) {
            final char wwChar = _wwChar(1);
            if(wwChar == '>') {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.VariableEvalN);
                _ctxClear();
            }
            else if(wwChar == ',' || wwChar == ':') {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.VariableEvalN);
                _gtl_curTokReset();
            }
            else {
                _gtl_detPos_WholeWord();
                if( _ctxTask_Number() ) {
                    // No further action required; the preceding function has already performed all necessary operations
                }
                else {
                    _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
                    _gtl_curTokReset();
                }
            }
            return true;
        }

        return false;
    }

    private boolean _ctxTask_inc_VariableEvalDQS()
    {
        /*
        <context name="VariableEvalDQS" lineEndContext="#pop">
            <Detect2Chars char="$" char1="{" attribute="RVariableEval" context="RVariableEvalDQS"/>
            <Detect2Chars char="$" char1="[" attribute="SVariableEval" context="SVariableEvalDQS"/>
        </context>
        */
       if( _ctxIs(Context.DQString) ) {
            final String wwOper = _wwOper(2);
            if( "${".equals(wwOper) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
                _ctxPush(Context.RVariableEvalDQS);
                return true;
            }
            if( "$[".equals(wwOper) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);
                _ctxPush(Context.SVariableEvalDQS);
                return true;
            }
        }

        /*
        <context name="RVariableEvalDQS" lineEndContext="#pop">
            <LineContinue          attribute="CombineNL"     context="#stay"              />
            <DetectChar   char="}" attribute="RVariableEval" context="#pop"               />
            <IncludeRules                                    context="RVariableEvalCommon"/>
        </context>
        */
        if( _ctxIs(Context.RVariableEvalDQS) ) {
            /*
            final String wwOper = _wwOper();
            if( "}".equals(wwOper) ) {
            */
            if( _wwOper('}') ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
                _ctxPop();
            }
            else {
               _ctxTask_inc_RVariableEvalCommon();
            }
            return true;
        }

        /*
        <context name="SVariableEvalDQS" lineEndContext="#pop">
            <LineContinue          attribute="CombineNL"     context="#stay"              />
            <DetectChar   char="]" attribute="SVariableEval" context="#pop"               />
            <IncludeRules                                    context="SVariableEvalCommon"/>
        </context>
        */
        if( _ctxIs(Context.SVariableEvalDQS) ) {
            /*
            final String wwOper = _wwOper();
            if( "]".equals(wwOper) ) {
            */
            if( _wwOper(']') ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SVariableEval);
                _ctxPop();
            }
            else {
                _ctxTask_inc_SVariableEvalCommon();
            }
            return true;
        }

        return false;
    }

    private boolean _ctxTask_RegExpr()
    {
        /*
        <context name="RegExpr1" attribute="RegExpr1" lineEndContext="#pop">
            <Detect2Chars char="\" char1="/" attribute="RegExpr1" context="#stay"        />
            <Detect2Chars char="\" char1="\" attribute="RegExpr1" context="#stay"        />
            <DetectChar   char="/"           attribute="RegExprM" context="#pop!RegExpr2"/>
        </context>
        */
        if( _ctxIs(Context.RegExpr1) ) {
            final String wwOper = _wwOper(2);
            if( "\\/".equals(wwOper) || "\\\\".equals(wwOper) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RegExpr1);
                _gtl_curTokReset();
                return true;
            }
            if( _wwChar(1) == '/' ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RegExprM);
                _ctxPopPush(Context.RegExpr2);
            }
            else {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RegExpr1);
                _gtl_curTokReset();
            }
            return true;
        }

        /*
        <context name="RegExpr2" attribute="RegExpr2" lineEndContext="#pop">
            <StringDetect String="\${"       attribute="RVariableEval" context="RegExpr2_PlainRVarEval"/>
            <Detect2Chars char="\" char1="/" attribute="RegExpr2"      context="#stay"                 />
            <Detect2Chars char="\" char1="\" attribute="RegExpr2"      context="#stay"                 />
            <DetectChar   char="/"           attribute="RegExprM"      context="#pop"                  />
        </context>
        */
        if( _ctxIs(Context.RegExpr2) ) {
            final String wwOper3 = _wwOper(3);
            if( "\\${".equals(wwOper3) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
                _ctxPush(Context.RegExpr2_PlainRVarEval);
                return true;
            }
            final String wwOper2 = _wwOper(2);
            if( "\\/".equals(wwOper2) || "\\\\".equals(wwOper2) ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RegExpr2);
                _gtl_curTokReset();
                return true;
            }
            if( _wwChar(1) == '/' ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RegExprM);
                _ctxPop();
            }
            else {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RegExpr2);
                _gtl_curTokReset();
            }
            return true;
        }

        /*
        <context name="RegExpr2_PlainRVarEval" attribute="RVariableEval" lineEndContext="#pop">
            <DetectChar char="}" attribute="RVariableEval" context="#pop"                  />
            <DetectChar char="/" attribute="ErrorText"     context="#pop#pop#pop!ErrorText"/>
        </context>
        */
        if( _ctxIs(Context.RegExpr2_PlainRVarEval) ) {
            final char wwChar = _wwChar(1);
            if(wwChar == '}') {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
                _ctxPop();
            }
            else if(wwChar == '/') {
                return _ctxTask_ERROR();
            }
            else {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.RVariableEval);
                _gtl_curTokReset();
            }
            return true;
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _ctxTask_FunctionEval()
    {
        /*
        <context name="FunctionEval" lineEndContext="#pop">
            <RegExpr String="-(?=\$&symname;\s*\()" attribute="SuppressError" context="#stay"          />
            <RegExpr String="\$(?=&symname;\s*\()"  attribute="BFunctionCall" context="FunctionEvalBeg"/>
        </context>
        <context name="FunctionEvalBeg" lineEndContext="#pop">
            <StringDetect String="call"         attribute="BFunctionCall" context="UFunctionCall1"/>
            <StringDetect String="exec"         attribute="BFunctionCall" context="UFunctionCall1"/>
            <StringDetect String="add_target"   attribute="BFunctionCall" context="BFunctionCall" />
            <StringDetect String="add_extradep" attribute="BFunctionCall" context="BFunctionCall" />
            ...
            <StringDetect String="pp_java_scf"  attribute="BFunctionCall" context="BFunctionCall" />
        </context>
        */
        if( _ctxIn(_ctxSupport_FunctionEval) ) {

            // Optional '-'
            if( _wwChar('-') ) {
                final int curPos = _gtl_curPos;

                _gtl_curPos = _gtl_detPos;
                _gtl_detPos_WholeWord();
                if( _wwCharSv(1) != '$' ) {
                    _gtl_curPos = curPos;
                    return _wwLenRestore();
                }

                _gtl_curPos = curPos;
                _gtl_detPos = curPos + 1;
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.SuppressError);
            }

            // Mandatory '$'
            _gtl_detPos_WholeWord();
            if( !_wwChar('$') ) return false;
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.BFunctionCall);

            // Mandatory '<function_name>'
            _gtl_curPos = _gtl_detPos;
            if( _gtl_detPos_SymbolName() < 0    ) return _ctxTask_ERROR(); // @@@
            if( !_BIFName.contains( _wwWord() ) ) return _ctxTask_ERROR();
            final boolean isCallExec = "call".equals( _wwWord() ) || "exec".equals( _wwWord() );
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.BFunctionCall);

            // Optional whitespace(s)
            _ctxTask_Whitespaces();

            // Mandatory '('
            _gtl_detPos_WholeWord();
            if( !_wwChar('(') ) return _ctxTask_ERROR();
            _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.ROperator);

            if(isCallExec) _ctxPush(Context.UFunctionCall1);
            else           _ctxClear();

            return true;
        }

        /*
        <context name="BFunctionCall" attribute="BFunctionCall" lineEndContext="#stay">
            <LineContinue          attribute="CombineNL"     context="#stay"        />
            <IncludeRules                                    context="CommentEval"  />
            <DetectChar   char=";" attribute="ForceNL"       context="#stay"        />
            <DetectChar   char=")" attribute="BFunctionCall" context="#pop#pop"     />
            <IncludeRules                                    context="XFunctionCall"/>
        </context>
        */
        // NOTE : Not implemented because of inherent tokenizer limitations!

        /*
        <context name="UFunctionCall1" attribute="BFunctionCall" lineEndContext="#stay">
            <LineContinue                                  attribute="CombineNL"     context="#stay"         />
            <IncludeRules                                                            context="CommentEval"   />
            <DetectChar   char=";"                         attribute="ForceNL"       context="#stay"         />
            <DetectChar   char=")"                         attribute="BFunctionCall" context="#pop#pop"      />
            <StringDetect String="__origin__"              attribute="Keyword"       context="UFunctionCall2"/>
            <RegExpr      String="&symname;"               attribute="UFunctionName" context="UFunctionCall2"/>
            <RegExpr      String="\$[\{\[]&symname;[\}\]]" attribute="UFunctionName" context="UFunctionCall2"/>
            <IncludeRules                                                            context="StringEvalUFN" />
        </context>
        <context name="StringEvalUFN" lineEndContext="#pop">
            <IncludeRules                                         context="StringEvalCommon"/>
            <DetectChar   char="&apos;" attribute="UFunctionName" context="SQStringUFN"     />
            <DetectChar   char="&quot;" attribute="UFunctionName" context="DQStringUFN"     />
        </context>
        <context name="SQStringUFN" attribute="UFunctionName" lineEndContext="#pop!UFunctionCall2">
            <LineContinue               attribute="CombineNL"     context="#stay"              />
            <IncludeRules                                         context="SQStringCommon"     />
            <DetectChar   char="&apos;" attribute="UFunctionName" context="#pop!UFunctionCall2"/>
        </context>
        <context name="DQStringUFN" attribute="UFunctionName" lineEndContext="#pop!UFunctionCall2">
            <LineContinue               attribute="CombineNL"     context="#stay"              />
            <IncludeRules                                         context="DQStringCommon"     />
            <DetectChar   char="&quot;" attribute="UFunctionName" context="#pop!UFunctionCall2"/>
        </context>
        */
        if( _ctxIs(Context.UFunctionCall1) ) {
            /*
            // NOTE : Not implemented because of inherent tokenizer limitations!
            if( _wwOper(')') ) {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.ROperator);
                _ctxPop();
            }
            else
            */
            if( _gtl_detPos_Generic(_pmd_UFuncName) != -1 ) {
                if( _wwWord("__origin__") ) _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.Keyword      );
                else                        _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.UFunctionName);
                _ctxClear();
            }
            else if( _ctxTask_ForceNL(true) ) {
                // No further action required; the preceding function has already performed all necessary operations
            }
            else {
                _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, TType.NormalText);
            }
            return true;
        }

        /*
        <context name="UFunctionCall2" attribute="BFunctionCall" lineEndContext="#stay">
            <LineContinue          attribute="CombineNL"     context="#stay"        />
            <IncludeRules                                    context="CommentEval"  />
            <DetectChar   char=";" attribute="ForceNL"       context="#stay"        />
            <DetectChar   char=")" attribute="BFunctionCall" context="#pop#pop#pop" />
            <IncludeRules                                    context="XFunctionCall"/>
        </context>
        */
        // NOTE : Not implemented because of inherent tokenizer limitations!

        return false;
    }

    /*
    <context name="XFunctionCall" lineEndContext="#pop">
        <LineContinue                          attribute="CombineNL"      context="#stay"       />
        <DetectChar   char=","                 attribute="ROperator"      context="#stay"       />
        <DetectChar   char="?"                 attribute="SOperator"      context="#stay"       />
        <IncludeRules                                                     context="BoolEval"    />
        <IncludeRules                                                     context="StringEval"  />
        <IncludeRules                                                     context="VariableEval"/>
        <IncludeRules                                                     context="PROperEval"  />
        <IncludeRules                                                     context="SROperEvalS" />
        <IncludeRules                                                     context="SROperEvalD" />
        <IncludeRules                                                     context="FunctionEval"/>
        <keyword      String="SVarShortcuts"   attribute="SVarShortcut"   context="#stay"       />
        <keyword      String="ANSIEscapeCodes" attribute="ANSIEscapeCode" context="#stay"       />
        <RegExpr      String="&symname;"       attribute="NormalText"     context="#stay"       />
        <RegExpr      String="\b&decnum;\b"    attribute="Number"         context="#stay"       />
        <RegExpr      String="\b&hexnum;\b"    attribute="Number"         context="#stay"       />
    </context>
    */
    // NOTE : Not implemented because of inherent tokenizer limitations!

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    <context name="VarAssign" lineEndContext="#pop">
        <LineContinue                          attribute="CombineNL"      context="#stay"       />
        <IncludeRules                                                     context="CommentEval" />
        <DetectChar   char=";"                 attribute="ForceNL"        context="#pop"        />
        <keyword      String="Keywords"        attribute="Keyword"        context="#stay"       />
        <keyword      String="SVarShortcuts"   attribute="SVarShortcut"   context="#stay"       />
        <keyword      String="ANSIEscapeCodes" attribute="ANSIEscapeCode" context="#stay"       />
        <RegExpr      String="\b&decnum;\b"    attribute="Number"         context="#stay"       />
        <RegExpr      String="\b&hexnum;\b"    attribute="Number"         context="#stay"       />
        <RegExpr      String=":(?=\[\[&quot;)" attribute="StringDQF"      context="#stay"       />
        <RegExpr      String=".(?=\[\[&quot;)" attribute="StringDQF"      context="#stay"       />
        <IncludeRules                                                     context="BoolEval"    />
        <IncludeRules                                                     context="StringEval"  />
        <IncludeRules                                                     context="StringMLEval"/>
        <IncludeRules                                                     context="VariableEval"/>
        <IncludeRules                                                     context="FunctionEval"/>
        <IncludeRules                                                     context="PROperEval"  />
        <IncludeRules                                                     context="SROperEvalD" />
    </context>
    */
    // NOTE : Not implemented because of inherent tokenizer limitations!

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String[] getLineCommentStartAndEnd(final int languageIndex)
    { return new String[] { "#", null }; }

    @Override
    public Token getTokenList(final Segment segment, final int startTokenType, final int startOffset)
    {
        // Reset the token list
        resetTokenList();

        // If the segment has zero length, add an empty token and return it
        //*
        if(segment.count == 0) {
            if(startTokenType == TType.Comment && _gtl_nestedMLComment == 0) return _addEmptyToken(TType.Null    , startOffset); // Ensure single-line comment is ended
            else                                                             return _addEmptyToken(startTokenType, startOffset);
        }
        //*/

        // Clear the context as needed
        if(startTokenType == TType.Null) {
            _inStringML     = false;
            _inEvalStatment = -1;
            _ctxClear();
        }

        // Prepare the variables
        _gtl_segment = segment;
        _gtl_newOfs  = startOffset - _gtl_segment.offset;

        _gtl_array   = _gtl_segment.array;
        _gtl_curPos  = _gtl_segment.offset;
        _gtl_endPos  = _gtl_segment.offset + _gtl_segment.count;

        _gtl_curTok  = (startTokenType == TType.Comment) ? TType.Comment : TType.Null; //startTokenType;
        _gtl_detPos  = -1;

        // Detect line continuation
        _gtl_lineContinue = false;
        _gtl_endPosSave   = _gtl_endPos;

        if(!_inStringML && _gtl_curPos < _gtl_endPos) {
            for(int i = _gtl_endPos - 1; i >= _gtl_curPos; --i) {
                if( RSyntaxUtilities.isWhitespace(_gtl_array[i]) ) continue; // Skip trailing whitespace
                _gtl_lineContinue = (_gtl_array[i] == '\\');
                _gtl_endPos       = i + 1;
                break;
            }
        }

        // Process the characters
        while(_gtl_curPos < _gtl_endPos) {

            // Process whitespaces within context
            if( !_ctxEmpty() ) _ctxTask_Whitespaces();

            // Detect the token type
            while(_gtl_curTok == TType.Null) {

                    // Detect whitespaces
                    if( _ctxChkd_Whitespaces() ) break;

                if(!_inStringML) {
                    // Detect single-line and multi-line comments
                    if( _ctxChkd_CommentEval() ) break;

                    // Combine line
                    if( _ctxTask_CombineNL() ) break;
                }

                // Detect a "whole word", defined as a leading sequence of either:
                //     # Alphanumeric characters and underscores
                //     # Or punctuation/symbol characters (excluding alphanumerics, underscores, and whitespace)
                _gtl_detPos_WholeWord();

                if(_gtl_detPos >= 0) {
                    do {

                        if(!_inStringML) {
                            if( _ctxTask_ForceNL        () ) break;

                            if( _ctxTask_DotWord        () ) break;
                            if( _ctxTask_OptionDef      () ) break;

                            if( _ctxTask_CaretWord      () ) break;

                            if( _ctxTask_Keyword        () ) break;
                            if( _ctxTask_SVarShortcuts  () ) break;
                            if( _ctxTask_ANSIEscapeCodes() ) break;

                            if( _ctxTask_ColonWord      () ) break;
                            if( _ctxTask_Include        () ) break;
                            if( _ctxTask_Pragma         () ) break;

                            if( _ctxTask_Bool           () ) break;
                            if( _ctxTask_Number         () ) break;

                            if( _ctxTask_StringEval     () ) break;
                        }
                            if( _ctxTask_StringMLEval   () ) break;

                            if( _ctxTask_EvalStatement  () ) break;

                        if(!_inStringML) {
                            if( _ctxTask_Operator       () ) break;
                            if( _ctxTask_SROperEvalD    () ) break;
                        }
                        if( _inStringML) {
                            if( _ctxTask_inc_PROperEval () ) break;

                        }
                            if( _ctxTask_VariableEval   () ) break;
                            if( _ctxTask_RegExpr        () ) break;
                        if(!_inStringML) {
                            if( _ctxTask_FunctionEval   () ) break;
                            if( _ctxTask_CmdEval        () ) break;
                        }

                        // Other words
                        _gtl_curTok = TType.NormalText;
                        break;

                    } while(false);

                    break;
                }

                // No valid token type was detected - error
                _gtl_detPos = _gtl_curPos + 1;
                _gtl_curTok = TType.ErrorText;
                break;

            } // while

            // Finish processing the token
            switch(_gtl_curTok) {

                // Null token
                case TType.Null:
                    break;

                // Comment tokens
                case TType.Comment:
                    if(_gtl_nestedMLComment == 0) _ctxTask_Comment  ();
                    else                          _ctxTask_MLComment();
                    break;

                // Normal text and whitespace tokens
                case TType.NormalText: /* FALLTHROUGH */
                case TType.Whitespace:
                    _addTokenCP_DP(_gtl_curPos, _gtl_detPos - 1, _inStringML ? TType.String : _gtl_curTok);
                  //_ctxClear();
                    _gtl_curTokReset();
                    break;

                // Other tokens
                default:
                    return firstToken;

            } // switch

            // Exit if the current token is not a null token
            if(_gtl_curTok != TType.Null) return firstToken;

        } // while

        // Add the trailing whitespace
        if(!_gtl_lineContinue && _gtl_endPosSave > _gtl_endPos) {
            _gtl_curPos = _gtl_endPos;
            _gtl_endPos = _gtl_endPosSave;
            _ctxTask_Whitespaces();
        }

        // Inform the caller that the last token on the current line is either a single‑line token or a multi‑line token
        if(_inStringML || _inEvalStatment != -1) _addEmptyTokenEP();
        else                                     addNullToken    ();

        // Return the first token in the linked list
        return firstToken;
    }

} // class JxMakeTokenMaker
