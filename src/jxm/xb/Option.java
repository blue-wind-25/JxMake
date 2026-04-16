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
import java.util.HashSet;

import jxm.*;


public class Option extends ExecBlock {

    public static enum Type {
        warning
    }

    public static enum Mode {
        push,
        pop,
        disable(true),
        enable (true)

        ;

        public final boolean hasSpec;

        private Mode(                ) { hasSpec = false; }
        private Mode(final boolean hs) { hasSpec = hs;    }
    }

    public static enum Spec {
        __none__,

        inv_ref_var,
        var_not_exist,

        cnv_integer,
        cnv_boolean
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class OptionStack {

        private static boolean _globalEnableWarnEvalInvRefVar    = false;
        private static boolean _globalEnableWarnEvalVarNotExist  = false;

        private static boolean _globalEnableWarnCnvStringInteger = false;
        private static boolean _globalEnableWarnCnvStringBoolean = false;

        public static void setGlobalEnableWarnEvalInvRefVar(final boolean enable)
        { _globalEnableWarnEvalInvRefVar = enable; }

        public static void setGlobalEnableWarnEvalVarNotExist(final boolean enable)
        { _globalEnableWarnEvalVarNotExist = enable; }

        public static void setGlobalEnableWarnCnvStringInteger(final boolean enable)
        { _globalEnableWarnCnvStringInteger = enable; }

        public static void setGlobalEnableWarnCnvStringBoolean(final boolean enable)
        { _globalEnableWarnCnvStringBoolean = enable; }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final Stack< HashSet<Spec> > _warningDisabled = new Stack<>();
        private final Stack< HashSet<Spec> > _warningEnabled  = new Stack<>();

        private void _warningPush()
        {
            final HashSet<Spec> prevD = _warningDisabled.empty() ? null : _warningDisabled.peek();
            final HashSet<Spec> prevE = _warningEnabled .empty() ? null : _warningEnabled .peek();

            _warningDisabled.push( new HashSet<>() );
            _warningEnabled .push( new HashSet<>() );

            if(prevD != null) _warningDisabled.peek().addAll(prevD);
            if(prevE != null) _warningEnabled .peek().addAll(prevE);
        }

        private void _warningPop()
        {
            _warningDisabled.pop();
            _warningEnabled .pop();

            if( _warningDisabled.empty() && _warningEnabled.empty() ) _warningPush();
        }

        private void _warningDisable(final Spec spec)
        {
            _warningDisabled.peek().add   (spec);
            _warningEnabled .peek().remove(spec);
        }

        private void _warningEnable(final Spec spec)
        {
            _warningDisabled.peek().remove(spec);
            _warningEnabled .peek().add   (spec);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public OptionStack()
        { _warningPush(); }

        public boolean enableWarnEvalInvRefVar()
        {
            if( _warningDisabled.peek().contains(Spec.inv_ref_var) ) return false;
            if( _warningEnabled .peek().contains(Spec.inv_ref_var) ) return true;

            return _globalEnableWarnEvalInvRefVar;
        }

        public boolean enableWarnEvalVarNotExist()
        {
            if( _warningDisabled.peek().contains(Spec.var_not_exist) ) return false;
            if( _warningEnabled .peek().contains(Spec.var_not_exist) ) return true;

            return _globalEnableWarnEvalVarNotExist;
        }

        public boolean enableWarnCnvStringInteger()
        {
            if( _warningDisabled.peek().contains(Spec.cnv_integer) ) return false;
            if( _warningEnabled .peek().contains(Spec.cnv_integer) ) return true;

            return _globalEnableWarnCnvStringInteger;
        }

        public boolean enableWarnCnvStringBoolean()
        {
            if( _warningDisabled.peek().contains(Spec.cnv_boolean) ) return false;
            if( _warningEnabled .peek().contains(Spec.cnv_boolean) ) return true;

            return _globalEnableWarnCnvStringBoolean;
        }

        public OptionStack deepClone()
        {
            final OptionStack newOptionStack = new OptionStack();

            for(final HashSet<Spec> refHashSet : _warningDisabled) {
                final HashSet<Spec> newHashSet = new HashSet<>();
                for(final Spec spec : refHashSet) newHashSet.add(spec);
                newOptionStack._warningDisabled.push(newHashSet);
            }

            for(final HashSet<Spec> refHashSet : _warningEnabled) {
                final HashSet<Spec> newHashSet = new HashSet<>();
                for(final Spec spec : refHashSet) newHashSet.add(spec);
                newOptionStack._warningEnabled.push(newHashSet);
            }

            return newOptionStack;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Type _type;
    private final Mode _mode;
    private final Spec _spec;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public Option(final Type type, final Mode mode, final Spec spec)
    {
        super(null, 0, 0);

        _type = type;
        _mode = mode;
        _spec = spec;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Execute based on the type
        switch(_type) {

            case warning:
                // Execute based on the mode
                switch(_mode) {
                    case push    : execData.execState.optionStack()._warningPush   (     ); break;
                    case pop     : execData.execState.optionStack()._warningPop    (     ); break;
                    case disable : execData.execState.optionStack()._warningDisable(_spec); break;
                    case enable  : execData.execState.optionStack()._warningEnable (_spec); break;
                } // switch _mode
                break;

        } // switch _type

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        dos.writeUTF( _type.name() );
        dos.writeUTF( _mode.name() );
        dos.writeUTF( _spec.name() );
    }

    public static Option loadFromStream(final DataInputStream dis) throws Exception
    {
        return new Option(
            Type.valueOf( dis.readUTF() ),
            Mode.valueOf( dis.readUTF() ),
            Spec.valueOf( dis.readUTF() )
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class Option
