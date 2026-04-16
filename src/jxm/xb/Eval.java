/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Stack;

import jxm.*;


public class Eval extends ExecBlock {

    private final String            _resVarName;
    private final XCom.PostfixTerms _postfixTerms;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public Eval(final String path, final int lNum, final int cNum, final String resVarName, final XCom.PostfixTerms postfixTerms)
    {
        super(path, lNum, cNum);

        _resVarName   = XCom.genRVarName(resVarName);
        _postfixTerms = postfixTerms;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _popVal_boolean(final Stack<Long> stackNum, final Stack<Boolean> stackBol)
    {
        stackNum.pop();
        return stackBol.pop();
    }

    private long _popVal(final XCom.PostfixOper opr, final Stack<Long> stackNum, final Stack<Boolean> stackBol)
    {
        if( opr.isLogicalOperator() ) {
            stackNum.pop();
            return stackBol.pop() ? 1 : 0;
        }

        stackBol.pop();
        return stackNum.pop();
    }

    private void _pushVal(final long value, final Stack<Long> stackNum, final Stack<Boolean> stackBol)
    {
        stackNum.push(  value                      );
        stackBol.push( (value != 0) ? true : false );
    }

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Prepare the execution variables
        final Stack<Long   > stackNum = new Stack<>();
        final Stack<Boolean> stackBol = new Stack<>();
        boolean              error    = false;

        /*
        InfixToPostfix.dumpPostfixTerms(_postfixTerms);
        //*/

        // Process the items
        for(final XCom.PostfixTerm item : _postfixTerms) {

            // Check if it is an operand
            if( item.isOperand() ) {
                // Get the string value
                final String strValue = ContainerBlock._evalBoolStr( XCom.flatten( execData.execState.readVar(this, execData, item.operand, true), "" ) );
                // Convert and store the value to stacks
                stackNum.push( XCom.toLong   (this, execData, strValue) );
                stackBol.push( XCom.toBoolean(this, execData, strValue) );
            }

            // It is an operator
            else {

                // Result variable
                long vres = 0;

                // Check if it is a unary operator or an inline function with 1 argument
                if( item.operator.isOneArg() ) {
                    // Get the operand from stack
                    final long v1 = _popVal(item.operator, stackNum, stackBol);
                    // Evaluate it
                    switch(item.operator) {
                        case u_p      : vres  =  +v1                  ; break;
                        case u_m      : vres  =  -v1                  ; break;
                        case u_abs    : vres  = ( v1 <  0 ) ? -v1 : v1; break;
                        case u_bw_not : vres  =  ~v1                  ;
                        break;

                        case u_lg_not : vres  = ( v1 == 0 ) ?  1  : 0 ; break;
                        case f_sgn    : vres  = (long) Math.signum(v1); break;
                        default       : error = true                  ; break;
                    }
                    // Store the result back to stack
                    if(!error) _pushVal(vres, stackNum, stackBol);
                }

                // It is a binary operator or an inline function with 2 arguments
                else if( item.operator.isTwoArg() ) {
                    // Get the operands from stack
                    final long v2 = _popVal(item.operator, stackNum, stackBol);
                    final long v1 = _popVal(item.operator, stackNum, stackBol);
                    // Evaluate them
                    switch(item.operator) {
                        case pow     : vres  = (long) Math.pow(v1, v2)                                ; break;
                        case mul     : vres  =   v1 *  v2                                             ; break;
                        case div     : vres  =   v1 /  v2                                             ; break;
                        case mod     : vres  =   v1 %  v2                                             ; break;
                        case add     : vres  =   v1 +  v2                                             ; break;
                        case sub     : vres  =   v1 -  v2                                             ; break;
                        case bw_shl  : vres  =   v1 << v2                                             ; break;
                        case bw_shr  : vres  =   v1 >> v2                                             ; break;
                        case twc     : vres  = ( v1 == v2               ) ? 0 : ( (v1 < v2) ? -1 : 1 ); break;
                        case lt      : vres  = ( v1 <  v2               ) ? 1 : 0                     ; break;
                        case lte     : vres  = ( v1 <= v2               ) ? 1 : 0                     ; break;
                        case gt      : vres  = ( v1 >  v2               ) ? 1 : 0                     ; break;
                        case gte     : vres  = ( v1 >= v2               ) ? 1 : 0                     ; break;
                        case eq      : vres  = ( v1 == v2               ) ? 1 : 0                     ; break;
                        case neq     : vres  = ( v1 != v2               ) ? 1 : 0                     ; break;
                        case bw_and  : vres  =   v1 &  v2                                             ; break;
                        case bw_xor  : vres  =   v1 ^  v2                                             ; break;
                        case bw_or   : vres  =   v1 |  v2                                             ; break;
                        case lg_and  : vres  = ( (v1 != 0) && (v2 != 0) ) ? 1 : 0                     ; break;
                        case lg_or   : vres  = ( (v1 != 0) || (v2 != 0) ) ? 1 : 0                     ; break;
                        case ternary : if(true) {
                                           // The ternary operator is parsed as if it were a sequence of two binary operators
                                           final boolean v0 = _popVal_boolean(stackNum, stackBol);
                                           vres = v0 ? v1 : v2;
                                       }
                                       break;
                        case f_min   : vres  = Math.min(v1, v2);                                      ; break;
                        case f_max   : vres  = Math.max(v1, v2);                                      ; break;
                        default      : error = true                                                   ; break;
                    }
                    // Store the result back to stack
                    if(!error) _pushVal(vres, stackNum, stackBol);
                }

                // Got an invalid term
                else {
                    error = true;
                }

            } // if

        } // for

        // Check for error
        if(error) {
            setErrorFromString(Texts.EMsg_PostfixEvaluationError);
            return XCom.ExecuteResult.Error;
        }

        // Get the evaluation result
        final String resStr = String.valueOf( stackNum.pop() );

        if( !stackNum.empty() ) {


            setErrorFromString(Texts.EMsg_PostfixEvaluationError);
            return XCom.ExecuteResult.Error;
        }

        // Store the evaluation result
        execData.execState.setVar( _resVarName, new XCom.VariableStore(true, resStr), false, false );

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveString      ( dos, XCom.trmRVarName(_resVarName) );
        XSaver.savePostfixTerms( dos, _postfixTerms                 );
    }

    public static Eval loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new Eval(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadString      (dis),
            XLoader.loadPostfixTerms(dis)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class Eval
