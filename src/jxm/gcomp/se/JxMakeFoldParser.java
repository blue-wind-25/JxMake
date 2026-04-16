/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldType;

import jxm.annotation.*;
import jxm.xb.*;

import static jxm.gcomp.se.JxMakeTheme.TType;


/*
 * https://github.com/bobbylight/RSyntaxTextArea/wiki/Adding-Code-Folding-for-a-new-Language
 *
 * https://github.com/bobbylight/RSyntaxTextArea/blob/master/RSyntaxTextArea/src/main/java/org/fife/ui/rsyntaxtextarea/folding/HtmlFoldParser.java
 * https://github.com/bobbylight/RSyntaxTextArea/blob/master/RSyntaxTextArea/src/main/java/org/fife/ui/rsyntaxtextarea/folding/LatexFoldParser.java
 * https://github.com/bobbylight/RSyntaxTextArea/blob/master/RSyntaxTextArea/src/main/java/org/fife/ui/rsyntaxtextarea/folding/LispFoldParser.java
 * https://github.com/bobbylight/RSyntaxTextArea/blob/master/RSyntaxTextArea/src/main/java/org/fife/ui/rsyntaxtextarea/folding/PythonFoldParser.java
 * https://github.com/bobbylight/RSyntaxTextArea/blob/master/RSyntaxTextArea/src/main/java/org/fife/ui/rsyntaxtextarea/folding/XmlFoldParser.java
 */
@package_private
class JxMakeFoldParser implements FoldParser {

    @SuppressWarnings("serial")
    private static final Map<String, String> BlockMarker = Collections.unmodifiableMap( new HashMap<String, String>() {{
        put("macro"   , "endmacro"   );
        put("function", "endfunction");
        put("target"  , "endtarget"  );
        put("if"      , "endif"      );
        put("for"     , "endfor"     );
        put("foreach" , "endforeach" );
        put("while"   , "endwhile"   );
        put("repeat"  , "until"      );
        put("do"      , "whilst"     );
        put("loop"    , "endloop"    );
    }} );

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ArrayList<Fold> _folds         = new ArrayList<>();
    private final Stack<String>   _expectedStack = new Stack    <>();
    private       Fold            _currentFold   = null;

    private void _push(final RSyntaxTextArea textArea, final int foldType, final Token token, final String closingWord) throws BadLocationException
    {
        if(_currentFold == null) {
            _currentFold = new Fold( foldType, textArea, token.getOffset() );
            _folds.add(_currentFold);
        }
        else {
            _currentFold = _currentFold.createChild( foldType, token.getOffset() );
        }

        _expectedStack.push(closingWord);
    }

    private void _pop(final Token token) throws BadLocationException
    {
        final Fold parentFold = _currentFold.getParent();

        _expectedStack.pop();

        _currentFold.setEndOffset( token.getOffset() );

        if( _currentFold.isOnSingleLine() && !_currentFold.removeFromParent() ) _folds.remove( _folds.size() - 1 );

        _currentFold = parentFold;
    }

    @Override
    public List<Fold> getFolds(final RSyntaxTextArea textArea)
    {
        final int lineCount = textArea.getLineCount();

        _folds.clear();
        _expectedStack.clear();

        _currentFold = null;

        try {

            for(int line = 0; line < lineCount; ++line) {

                Token t = textArea.getTokenListForLine(line);

                while( t != null && t.isPaintable() ) {

                    switch( t.getType() ) {

                        case TType.Comment: {
                            // Get token text
                            final String chk = t.getLexeme();
                            // Checking for opening marker '(*'
                            if( chk.trim().startsWith("(*") ) {
                                _push(textArea, FoldType.COMMENT, t, "*)");
                            }
                            // Checking for closing marker '*)'
                            else if(
                                   _currentFold != null      && !_expectedStack.isEmpty()
                                && chk.trim().endsWith("*)") && _expectedStack.peek().equals("*)")
                            ) {
                                _pop(t);
                            }
                        }
                        break;

                        case TType.StringMLMarker: {
                            // Get token text
                            final String chk = t.getLexeme();
                            // Checking for opening marker '[["'
                            if( chk.trim().equals("[[\"") ) {
                                _push(textArea, FoldType.COMMENT, t, "\"]]");
                            }
                            // Checking for closing marker '"]]'
                            else if(
                                   _currentFold != null      && !_expectedStack.isEmpty()
                                && chk.trim().endsWith("\"]]") && _expectedStack.peek().equals("\"]]")
                            ) {
                                _pop(t);
                            }
                        }
                        break;

                        case TType.MacroDef: /* FALLTHROUGH */
                        case TType.Keyword: {
                            // Checking for opening words
                            final String closingWord = BlockMarker.get( t.getLexeme() );
                            if(closingWord != null) {
                                _push(textArea, FoldType.CODE, t, closingWord);
                            }
                            // Checking for closing words
                            else if(
                                   _currentFold != null && !_expectedStack.isEmpty()
                                && _expectedStack.peek().equals( t.getLexeme() )
                            ) {
                                _pop(t);
                            }
                        }
                        break;

                        case TType.SOperator: {
                            // Checking for character ':' for one line 'if <condition1> : ...' statement
                            if(
                                   _currentFold != null        && !_expectedStack.isEmpty()
                                && ":".equals( t.getLexeme() ) && _expectedStack.peek().equals("endif")
                            ) {
                                _pop(t);
                            }
                        }
                        break;

                    } // switch

                    // Get the next token
                    t = t.getNextToken();

                } // while

            } // for

        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return _folds;
    }

} // class JxMakeFoldParser
