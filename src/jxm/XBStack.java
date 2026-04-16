/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;
import java.util.Stack;

import jxm.xb.*;


public class XBStack {

    public static enum ElemType {
        _function,
        _target,
        _if,
        _else,
        _for,
        _foreach,
        _while,
        _repeat,
        _foreverloop
    }

    private static class StackElem {
        final ElemType        type;        // Type of the this element
        final boolean         chainPop;    // A flag to indicate if stack pop should be chained until this flag is false
        final ExecBlock       parentBlock; // Parent execution-block of this element
        final XCom.ExecBlocks execBlocks;  // Execution-blocks of this element

        public StackElem(final ElemType type_, final boolean chainPop_, final ExecBlock parentBlock_, final XCom.ExecBlocks execBlocks_)
        {
            type        = type_;
            chainPop    = chainPop_;
            parentBlock = parentBlock_;
            execBlocks  = execBlocks_;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final XCom.ExecBlocks  _rootExecBlocks  = new XCom.ExecBlocks();

    private final Stack<StackElem> _stack           = new Stack<>();
    private       String           _curFuncName     = null;
    private       String           _curTargetName   = null;

    private       int              _curLoopBlockCnt = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XBStack()
    {}

    public XCom.ExecBlocks rootExecBlocks()
    { return _rootExecBlocks; }

    public XCom.ExecBlocks activeExecBlocks()
    { return _stack.empty() ? _rootExecBlocks : _stack.peek().execBlocks; }

    public ExecBlock activeParentBlock()
    { return _stack.empty() ? null : _stack.peek().parentBlock; }

    public Function activeParentFunctionBlock()
    { return (Function) activeParentBlock(); }

    public Target activeParentTargetBlock()
    { return (Target) activeParentBlock(); }

    public boolean inAnyBlock()
    { return !_stack.empty(); }

    public boolean inFunctionBlock()
    { return _curFuncName != null; }

    public boolean inTargetBlock()
    { return _curTargetName != null; }

    public boolean inFunctionTargetBlock()
    { return inFunctionBlock() || inTargetBlock(); }

    public boolean inIfBlock()
    { return inAnyBlock() && ( _stack.peek().type == ElemType._if ); }

    public boolean inElseBlock()
    { return inAnyBlock() && ( _stack.peek().type == ElemType._else ); }

    public boolean inIfElseBlock()
    { return inIfBlock() || inElseBlock(); }

    public boolean inLoopBlock()
    { return _curLoopBlockCnt > 0; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks push(final ElemType elemType, final boolean chainPop, final ExecBlock parent, final XCom.ExecBlocks execBlocks)
    {
        // Create a new element and store it to the stack
        _stack.push( new StackElem(
            elemType,
            (elemType == ElemType._else) ? true : chainPop, // 'else' is always chained
            parent,
            execBlocks
        ) );

        // Save the function name as needed
        if(elemType == ElemType._function) {
            // Error if it is already inside a function block
            if(_curFuncName != null) return null;
            // Store the function name
            final Function function = (Function) parent;
            _curFuncName = function.getBlockName();
        }

        // Save the target name as needed
        else if(elemType == ElemType._target) {
            // Error if it is already inside a target block
            if(_curTargetName != null) return null;
            // Store the target name
            final Target target = (Target) parent;
            _curTargetName = target.getBlockName();
        }

        // Return back the execution-blocks
        return execBlocks;
    }

    public XCom.ExecBlocks push(final ElemType elemType, final ExecBlock parent, final XCom.ExecBlocks execBlocks)
    { return push(elemType, false, parent, execBlocks); }

    public XCom.ExecBlocks pop(final ElemType elemType)
    {
        // The last element in the pop chain
        StackElem seLast = null;

        // Process while the stack is not empty;
        ElemType refElem = elemType;

        while( !_stack.empty() ) {
            // Check the element type
            if( _stack.peek().type != refElem ) return null;
            // Pop and break if the 'chainPop' flag is false
            seLast = _stack.pop();
            if(!seLast.chainPop) break;
            // Update the element type
            refElem = _stack.peek().type;
        }

        // Clear the function or target name as needed
        if(seLast != null) {
                  if(seLast.type == ElemType._function) _curFuncName   = null;
             else if(seLast.type == ElemType._target  ) _curTargetName = null;
        }

        // Return execution-blocks of the last element in the pop chain
        return (seLast != null) ? seLast.execBlocks : null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks addFuncDefAndPush(final Function function)
    {
        activeExecBlocks().add(function);
        return push( XBStack.ElemType._function, function, function.getExecBlocks(0) );
    }

    public XCom.ExecBlocks popFuncDef()
    { return pop(XBStack.ElemType._function); }

    public Function peekFunctionBlock()
    {
        if( _stack.empty() || _stack.peek().type != ElemType._function ) return null;

        return (Function) _stack.peek().parentBlock;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks addTargetDefAndPush(final Target target)
    {
        activeExecBlocks().add(target);
        return push( XBStack.ElemType._target, target, target.getExecBlocks(0) );
    }

    public XCom.ExecBlocks popTargetDef()
    { return pop(XBStack.ElemType._target); }

    public Target peekTargetBlock()
    {
        if( _stack.empty() || _stack.peek().type != ElemType._target ) return null;

        return (Target) _stack.peek().parentBlock;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks addIfBlockAndPush(final IfElse block)
    {
        activeExecBlocks().add(block);
        return push( XBStack.ElemType._if, false, block, block.getExecBlocks(0) );
    }

    public XCom.ExecBlocks addElifBlockAndPush(final IfElse block, final boolean elseBlockIsAlreadyPushed)
    {
        if(!elseBlockIsAlreadyPushed) addElseBlockAndPush();

        activeExecBlocks().add(block);
        return push( XBStack.ElemType._if, true, block, block.getExecBlocks(0) );
    }

    public XCom.ExecBlocks addElseBlockAndPush()
    {
        if( _stack.empty() ) return null;

        final IfElse block = (IfElse) _stack.peek().parentBlock;

        return push( XBStack.ElemType._else, block, block.getExecBlocks(1) );
    }

    public XCom.ExecBlocks popIfBlock()
    {
        if( _stack.empty() ) return null;

        return ( _stack.peek().type == ElemType._if ) ? pop(ElemType._if  )
                                                      : pop(ElemType._else);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks addForBlockAndPush(final For for_)
    {
        ++_curLoopBlockCnt;
        activeExecBlocks().add(for_);
        return push( XBStack.ElemType._for, for_, for_.getExecBlocks(0) );
    }

    public XCom.ExecBlocks popForBlock()
    {
        --_curLoopBlockCnt;
        return pop(XBStack.ElemType._for);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks addForEachBlockAndPush(final ForEach foreach_)
    {
        ++_curLoopBlockCnt;
        activeExecBlocks().add(foreach_);
        return push( XBStack.ElemType._foreach, foreach_, foreach_.getExecBlocks(0) );
    }

    public XCom.ExecBlocks popForEachBlock()
    {
        --_curLoopBlockCnt;
        return pop(XBStack.ElemType._foreach);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks addWhileBlockAndPush(final While while_)
    {
        ++_curLoopBlockCnt;
        activeExecBlocks().add(while_);
        return push( XBStack.ElemType._while, while_, while_.getExecBlocks(0) );
    }

    public XCom.ExecBlocks popWhileBlock()
    {
        --_curLoopBlockCnt;
        return pop(XBStack.ElemType._while);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks addRepeatBlockAndPush(final Repeat repeat_)
    {
        ++_curLoopBlockCnt;
        activeExecBlocks().add(repeat_);
        return push( XBStack.ElemType._repeat, repeat_, repeat_.getExecBlocks(0) );
    }

    public XCom.ExecBlocks popRepeatBlock()
    {
        --_curLoopBlockCnt;
        return pop(XBStack.ElemType._repeat);
    }

    public Repeat peekRepeatBlock()
    {
        if( _stack.empty() || _stack.peek().type != ElemType._repeat ) return null;

        return (Repeat) _stack.peek().parentBlock;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecBlocks addForeverLoopBlockAndPush(final ForeverLoop foreverloop_)
    {
        ++_curLoopBlockCnt;
        activeExecBlocks().add(foreverloop_);
        return push( XBStack.ElemType._foreverloop, foreverloop_, foreverloop_.getExecBlocks(0) );
    }

    public XCom.ExecBlocks popForeverLoopBlock()
    {
        --_curLoopBlockCnt;
        return pop(XBStack.ElemType._foreverloop);
    }

} // class XBStack
