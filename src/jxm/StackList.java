/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.IOException;

import java.util.ArrayList;
import java.util.Stack;
import java.util.HashMap;

import jxm.xb.*;


public class StackList {

    //                            Handle  Value(s)
    private static final HashMap< String, Stack< ArrayList<String> > > _stackList      = new HashMap<> ();
    private static       long                                          _stackHandleCnt = 0;

    private static final long                                          StackIDMMask    = SysUtil.random32();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized static String _newStackHandle()
    { return SysUtil.createHandleID("stack", ++_stackHandleCnt, StackIDMMask); }

    public synchronized static boolean stackIsValidHandle(final String handle)
    { return SysUtil.isValidHandleID("stack", handle) && _stackList.containsKey(handle); }

    public synchronized static String stackNew()
    {
        final String                     handle   = _newStackHandle();
        final Stack< ArrayList<String> > newStack = new Stack<>();

        _stackList.put(handle, newStack);

        return handle;
    }

    public synchronized static boolean stackDelete(final String handle)
    { return _stackList.remove(handle) != null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean stackClear(final String handle)
    {
        final Stack< ArrayList<String> > stack = _stackList.get(handle);
        if(stack == null) return false;

        stack.clear();

        return true;
    }

    public synchronized static boolean stackPush(final String handle, final ArrayList<String> values)
    {
        final Stack< ArrayList<String> > stack = _stackList.get(handle);
        if(stack == null) return false;

        stack.push(values);

        return true;
    }

    public synchronized static ArrayList<String> stackPeek(final String handle)
    {
        final Stack< ArrayList<String> > stack = _stackList.get(handle);
        if(stack == null) return null;

        return stack.empty() ? null : stack.peek();
    }

    public synchronized static ArrayList<String> stackPop(final String handle)
    {
        final Stack< ArrayList<String> > stack = _stackList.get(handle);
        if(stack == null) return null;

        return stack.empty() ? null : stack.pop();
    }

    public synchronized static int stackNumElems(final String handle)
    {
        final Stack< ArrayList<String> > stack = _stackList.get(handle);
        if(stack == null) return 0;

        return stack.size();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static String toEData(final String handle) throws IOException
    {
        final Stack< ArrayList<String> > stack = _stackList.get(handle);
        if(stack == null) return null;

        return SerializableDeepClone.serializeToBase64String(stack);
    }

    public synchronized static String fromEData(final String edata) throws IOException, ClassNotFoundException
    {
        @SuppressWarnings("unchecked")
        final Stack< ArrayList<String> > stack = ( Stack< ArrayList<String> > ) SerializableDeepClone.deserializeFromBase64String(edata);

        final String handle = _newStackHandle();

        _stackList.put(handle, stack);

        return handle;
    }

} // class StackList
