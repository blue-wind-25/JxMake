/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;
import java.util.Stack;

import jxm.xb.*;


public class InfixToPostfix  {

    /*
     * The algorithm used in this class is developed based on the information from:
     *     https://www.geeksforgeeks.org/convert-infix-expression-to-postfix-expression
     *     https://www.geeksforgeeks.org/infix-to-postfix-using-different-precedence-values-for-in-stack-and-out-stack
     */


    private static void _storeOperator(final XCom.PostfixTerms postfixTerms, final XCom.PostfixOper operator)
    {
        switch(operator) {
            case    __ter_qm :                                                                   ; break;
            case    __ter_cl : postfixTerms.add( new XCom.PostfixTerm(XCom.PostfixOper.ternary) ); break;
            default          : postfixTerms.add( new XCom.PostfixTerm(operator                ) ); break;
        }
    }

    private static boolean _popLParen(final XBBuilder xbb, final TokenReader.Token token, final XCom.PostfixTerms postfixTerms, final Stack<XCom.PostfixOper> operStack, final Stack<Integer> ifaCntStack)
    {
        // Loop until the operator '(' is found
        while(true) {

            // If the stack is empty, it means the number of '(' and ')' is unbalanced
            if( operStack.isEmpty() ) {
                xbb._setError(token, Texts.EMsg_UnbalancedLRParentheses);
                return false;
            }

            // Break if an operator '(' has been found in the stack
            if( operStack.peek() == XCom.PostfixOper.__lparen ) {
                operStack.pop();
                break;
            }

            // Check if it is an inline function call end operator ']'
            if( operStack.peek() == XCom.PostfixOper.__f_end ) {
                // Pop the coresponding operator '...[' from stack
                if( !_popLSqrBracket(xbb, token, postfixTerms, operStack, ifaCntStack) ) return false;
            }
            // Check if the part is an operator ','
            else if( operStack.peek() == XCom.PostfixOper.__comma ) {
                // Error because operator ',' cannot appear here
                xbb._setError(token, Texts.EMsg_InvalidALExprUnexpected, ",");
                return false;
            }
            // Other tokens
            else {
                // Store the part as a postfix term
                _storeOperator( postfixTerms, operStack.pop() );
            }

        } // while

        // Done
        return true;
    }

    private static boolean _popLSqrBracket(final XBBuilder xbb, final TokenReader.Token token, final XCom.PostfixTerms postfixTerms, final Stack<XCom.PostfixOper> operStack, final Stack<Integer> ifaCntStack)
    {
        // Loop until the operator '...[' is found
        while(true) {

            // If the stack is empty, it means the number of '[' and ']' is unbalanced
            if( operStack.isEmpty() ) {
                xbb._setError(token, Texts.EMsg_UnbalancedLRSqrBracket);
                return false;
            }

            // Break if an inline function call begin operator '...[' has been found in the stack
            if( operStack.peek().isInlineFunctionBegin() ) {
                // Check if the counter match the number of arguments requested by the inline function
                if( ifaCntStack.peek() != operStack.peek().ifrac ) {
                    xbb._setError( token, Texts.EMsg_InvalidALInlFuncArgCnt, operStack.peek().label, operStack.peek().ifrac, ifaCntStack.peek() );
                    return false;
                }
                // Store the part as a postfix term
                _storeOperator( postfixTerms, operStack.pop() );
                // Pop the counter
                ifaCntStack.pop();
                // Increment the counter as needed
                if( !ifaCntStack.empty() ) ifaCntStack.push( ifaCntStack.pop() + 1 );
                // Break
                break;
            }

            // Check if the part is an operator ')'
            if( operStack.peek() == XCom.PostfixOper.__rparen ) {
                // Pop the coresponding operator '(' from stack
                if( !_popLParen(xbb, token, postfixTerms, operStack, ifaCntStack) ) return false;
            }
            // Check if the part is an operator ','
            else if( operStack.peek() == XCom.PostfixOper.__comma ) {
                // Discard the ','
                operStack.pop();
            }
            // Other tokens
            else {
                // Store the part as a postfix term
                _storeOperator( postfixTerms, operStack.pop() );
            }

        } // while

        // Done
        return true;
    }

    private static XCom.PostfixOper _checkForUnaryOper(final XCom.PostfixOper operator, final char ch0, final char ch1)
    {
        switch(ch0) {
            case '+':                return XCom.PostfixOper.u_p;
            case '-':                return XCom.PostfixOper.u_m;
            case '@':                return XCom.PostfixOper.u_abs;
            case '~':                return XCom.PostfixOper.u_bw_not;
            case '!': if(ch1 != '=') return XCom.PostfixOper.u_lg_not;
        }

        return operator;
    }

    public static XCom.PostfixTerms process(final XBBuilder xbb, final ArrayList<TokenReader.Token> tokens)
    {
        // Create a new instance to store the postfix terms
        final XCom.PostfixTerms postfixTerms = new XCom.PostfixTerms();

        // Prepare the state variables
        final int                     tokenSize    = tokens.size();

        final Stack<XCom.PostfixOper> operStack    = new Stack<>();
        final Stack<Integer         > ifaCntStack  = new Stack<>();

              boolean                 gotUOperator = true;
              boolean                 gotBOperator = true;
              boolean                 gotOperand   = false;
              int                     cntTernary   = 0;

        // Process the tokens
        for(int i = 0; i < tokenSize; ++i) {

            final TokenReader.Token token = tokens.get(i);
                  String            part  = token.tStr;

            while( !part.isEmpty() ) {

                // Prepare the transient variables
                String           operand  = null;
                XCom.PostfixOper operator = XCom.PostfixOper.__none__;

                // Check for unary operators that appear before the '(' operator
                if( !gotOperand && part.length() == 1 && (i + 1) < tokenSize && tokens.get(i + 1).tStr.equals("(") ) {
                    // Check for unary operators
                    operator = _checkForUnaryOper( operator, part.charAt(0), '\0' );
                    // Clear the part if a unary operator was found
                    if(operator != XCom.PostfixOper.__none__) part = "";
                }
                // Check for unary operators attached to operands
                else if( part.length() > 1 ) {
                    // Check for unary operators
                    operator = _checkForUnaryOper( operator, part.charAt(0), part.charAt(1) );
                    // Trim the part if a unary operator was found
                    if(operator.unary) part = part.substring( 1, part.length() );
                }

                // Check for binary operators and operand
                if( !part.isEmpty() && operator == XCom.PostfixOper.__none__ ) {
                    switch(part) {
                        case "**"   : operator = XCom.PostfixOper.pow     ; break;
                        case "*"    : operator = XCom.PostfixOper.mul     ; break;
                        case "/"    : operator = XCom.PostfixOper.div     ; break;
                        case "%"    : operator = XCom.PostfixOper.mod     ; break;
                        case "+"    : operator = XCom.PostfixOper.add     ; break;
                        case "-"    : operator = XCom.PostfixOper.sub     ; break;
                        case "<<"   : operator = XCom.PostfixOper.bw_shl  ; break;
                        case ">>"   : operator = XCom.PostfixOper.bw_shr  ; break;
                        case "<=>"  : operator = XCom.PostfixOper.twc     ; break;
                        case "<"    : operator = XCom.PostfixOper.lt      ; break;
                        case "<="   : operator = XCom.PostfixOper.lte     ; break;
                        case ">"    : operator = XCom.PostfixOper.gt      ; break;
                        case ">="   : operator = XCom.PostfixOper.gte     ; break;
                        case "=="   : operator = XCom.PostfixOper.eq      ; break;
                        case "!="   : operator = XCom.PostfixOper.neq     ; break;
                        case "&"    : operator = XCom.PostfixOper.bw_and  ; break;
                        case "^"    : operator = XCom.PostfixOper.bw_xor  ; break;
                        case "|"    : operator = XCom.PostfixOper.bw_or   ; break;
                        case "&&"   : operator = XCom.PostfixOper.lg_and  ; break;
                        case "||"   : operator = XCom.PostfixOper.lg_or   ; break;
                        case "("    : operator = XCom.PostfixOper.__lparen; break;
                        case ")"    : operator = XCom.PostfixOper.__rparen; break;
                        case ","    : operator = XCom.PostfixOper.__comma ; break;
                        case "sgn[" : operator = XCom.PostfixOper.f_sgn   ; break;
                        case "min[" : operator = XCom.PostfixOper.f_min   ; break;
                        case "max[" : operator = XCom.PostfixOper.f_max   ; break;
                        case "]"    : operator = XCom.PostfixOper.__f_end ; break;
                        case "?"    : operator = XCom.PostfixOper.__ter_qm; break;
                        case ":"    : operator = XCom.PostfixOper.__ter_cl; break;
                        default     : if( part.charAt( part.length() - 1 ) == '[' ) {
                                          xbb._setError(token, Texts.EMsg_InvalidALExprUnexpected, part);
                                          return null;
                                      }
                                      operand = part;
                                      break;
                    } // switch
                    // Clear the part
                    part = "";
                }

                // Check if it is an operand
                if(operand != null) {
                    // Check if it is a valid variable name
                    if( operand.indexOf("$") == 0 ) {
                        // Check if it is a regular variable
                        if( operand.indexOf("{") == 1 ) {
                            // Check if it is a valid symbol name
                            if( !XCom.isSymbolName( XCom.trmRVarName(operand) ) ) {
                                xbb._setError( token, Texts.EMsg_InvalidVarName, XCom.trmRVarName(operand) );
                                return null;
                            }
                        }
                        // Check if it is a special variable
                        else if( operand.indexOf("[") == 1 ) {
                            // Nothing to do here, the checking will be done by '_getRVarSpecFromToken()' later
                        }
                        // Invalid token
                        else {
                            xbb._setError(token, Texts.EMsg_InvalidALExprUnexpected, operand);
                            return null;
                        }
                    }
                    // Check if it is a valid plain integer number or the boolean value 'true' or 'false'
                    else {
                        if( !XCom.isPlainDecInteger(operand) && !XCom.isPlainHexInteger(operand) && !operand.equals("true") && !operand.equals("false") ) {
                            xbb._setError(token, Texts.EMsg_InvalidALOperand, operand);
                            return null;
                        }
                    }
                    // Error if an operand is preceded by another operand
                    if(gotOperand) {
                        xbb._setError(token, Texts.EMsg_InvalidALExprOperand);
                        return null;
                    }
                    // Create a variable specification list and store it
                    token.tStr = operand;
                    final XCom.ReadVarSpec rvs = XBBuilder._getRVarSpecFromToken(xbb, token);
                    if(rvs == null) return null;
                    // Store the part as a postfix term
                    postfixTerms.add( new XCom.PostfixTerm(rvs) );
                    // Update the flags
                    gotUOperator = false;
                    gotBOperator = false;
                    gotOperand   = true;
                    // Increment the counter as needed
                    if( !ifaCntStack.empty() ) ifaCntStack.push( ifaCntStack.pop() + 1 );
                    // Done for now if it did not get a unary operator
                    if(!operator.unary) continue;
                }

                // Check if it is an operator '('
                if(operator == XCom.PostfixOper.__lparen) {
                    // Store the part to stack
                    operStack.push(operator);
                    // Done for now
                    continue;
                }

                // Check if it is an operator ')'
                if(operator == XCom.PostfixOper.__rparen) {
                    // Pop the coresponding operator '(' from stack
                    if( !_popLParen(xbb, token, postfixTerms, operStack, ifaCntStack) ) return null;
                    // Done for now
                    continue;
                }

                // Check if it is an inline function call begin operator '...['
                if( operator.isInlineFunctionBegin() ) {
                    // Store the part to stack
                    operStack.push(operator);
                    // Push the counter
                    ifaCntStack.push( Integer.valueOf(0) );
                    // Done for now
                    continue;
                }

                // Check if it is an inline function call end operator ']'
                if(operator == XCom.PostfixOper.__f_end) {
                    // Pop the coresponding operator '...[' from stack
                    if( !_popLSqrBracket(xbb, token, postfixTerms, operStack, ifaCntStack) ) return null;
                    // Done for now
                    continue;
                }

                // Other operator
                if(operator != XCom.PostfixOper.__none__) {
                    // Error if a binary operator is preceded by any other operator
                    if( (gotUOperator || gotBOperator) && !operator.unary ) {
                        xbb._setError(token, Texts.EMsg_InvalidALExprOperator);
                        return null;
                    }
                    // The ternary operator is parsed as if it were a sequence of two binary operators with additional checkings
                    if(operator == XCom.PostfixOper.__ter_qm) {
                        ++cntTernary;
                    }
                    else if(operator == XCom.PostfixOper.__ter_cl) {
                        // Check if there are more ':' than '?'
                        if(--cntTernary < 0) {
                            xbb._setError(token, Texts.EMsg_InvalidALExprTernary);
                            return null;
                        }
                    }
                    // Pop the stack while it is not empty and the precedence of the current operator is less than or
                    // equal to the precedence of the operator on the stack top
                    while( !operStack.isEmpty() && !operStack.peek().isInlineFunctionBegin() && operator.oprec <= operStack.peek().iprec ) {
                        // Store the part as a postfix term
                        _storeOperator( postfixTerms, operStack.pop() );
                    }
                    operStack.push(operator);
                    // Decrement the counter as needed
                    if( !ifaCntStack.empty() && !operator.unary && operator != XCom.PostfixOper.__comma ) ifaCntStack.push( ifaCntStack.pop() - 1 );
                    // Update the flags
                    gotUOperator =  operator.unary;
                    gotBOperator = !operator.unary;
                    gotOperand   =  false;
                    // Done for now
                    continue;
                }

                // Error if it got here
                xbb._setError(token, Texts.EMsg_InvalidALExprUnexpected, token.tStr);
                return null;

            } // while

        } // for

        // Pop the remaining part(s) from the stack
        while( !operStack.isEmpty() ) {
            // If the part is an operator '(', it means the number of '(' and ')' is unbalanced
            if( operStack.peek() == XCom.PostfixOper.__lparen ) {
                xbb._setError( tokens.get( tokens.size() - 1 ), Texts.EMsg_UnbalancedLRParentheses );
                return null;
            }
            // If the part is an inline function call begin operator '...[', it means the number of '[' and ']' is unbalanced
            else if( operStack.peek().isInlineFunctionBegin() ) {
                xbb._setError( tokens.get( tokens.size() - 1 ), Texts.EMsg_UnbalancedLRSqrBracket );
                return null;
            }
            // Store the part as a postfix term
            _storeOperator( postfixTerms, operStack.pop() );
        }

        // Check if there are more '?' than ':'
        if(cntTernary > 0) {
            xbb._setError( tokens.get( tokens.size() - 1 ), Texts.EMsg_InvalidALExprTernary );
            return null;
        }

        // Return the instance
        return postfixTerms;
    }

    public static void dumpPostfixTerms(final XCom.PostfixTerms postfixTerms)
    {
        for(final XCom.PostfixTerm item : postfixTerms) {
            if(item.operator != XCom.PostfixOper.__none__) SysUtil.stdOut().printf("%s ", item.operator.label);
            else                                           SysUtil.stdOut().printf("%s ", item.operand .value);
        }

        SysUtil.stdOut().println();
    }

} // class InfixToPostfix

