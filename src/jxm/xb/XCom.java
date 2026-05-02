/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.File;
import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.security.NoSuchAlgorithmException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import java.util.concurrent.Semaphore;

import java.util.function.BiFunction;
import java.util.function.Function;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.nio.file.attribute.FileTime;

import jxm.*;


public class XCom {

    public static class IntegerRef extends Object {
        private int _value;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public IntegerRef()
        { _value = 0; }

        public IntegerRef(final int value)
        { _value = value; }

        public IntegerRef(final Integer value)
        { _value = value; }

        public IntegerRef(final IntegerRef ref)
        { _value = ref._value; }

        public int get()
        { return _value; }

        public void set(final int value)
        { _value = value; }

        public void set(final Integer value)
        { _value = value; }

        public void set(final IntegerRef ref)
        { _value = ref._value; }

        public void inc()
        { _value += 1; }

        public void inc(final Integer value)
        { _value += value; }

        public void dec()
        { _value -= 1; }

        public void dec(final Integer value)
        { _value -= value; }

        @Override
        public String toString()
        { return String.valueOf(_value); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static IntegerRef valueOf(final int value)
        { return new IntegerRef(value); }

        public static IntegerRef valueOf(final Integer value)
        { return new IntegerRef( value.intValue() ); }

        public static IntegerRef valueOf(final String value)
        { return new IntegerRef( Integer.valueOf(value) ); }

        public static IntegerRef valueOf(final String value, final int radix)
        { return new IntegerRef( Integer.valueOf(value, radix) ); }
    }

    public static class DoubleRef extends Object {
        private double _value;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public DoubleRef()
        { _value = 0; }

        public DoubleRef(final double value)
        { _value = value; }

        public DoubleRef(final DoubleRef ref)
        { _value = ref._value; }

        public double get()
        { return _value; }

        public void set(final double value)
        { _value = value; }

        public void set(final Double value)
        { _value = value; }

        public void set(final DoubleRef ref)
        { _value = ref._value; }

        @Override
        public String toString()
        { return String.valueOf(_value); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static DoubleRef valueOf(final double value)
        { return new DoubleRef(value); }

        public static DoubleRef valueOf(final Double value)
        { return new DoubleRef( value.intValue() ); }

        public static DoubleRef valueOf(final String value)
        { return new DoubleRef( Double.parseDouble(value) ); }
    }

    public static class Pair<T, U> {
        private final T _first;
        private final U _second;

        public Pair(final T first, final U second)
        {
            this._first  = first;
            this._second = second;
        }

        public T first () { return _first ; }
        public U second() { return _second; }

        @Override
        public boolean equals(final Object o)
        {
            if( !(o instanceof Pair) ) return false;

            final Pair<?, ?> p = (Pair<?, ?>) o;

            return _first.equals(p._first) && _second.equals(p._second);
        }

        @Override
        public int hashCode()
        { return _first.hashCode() * 31 + _second.hashCode(); }
    }

    public static class MultiMap<K, V> {

        private final HashMap< K, ArrayList<V> > _map = new HashMap<>();

        public void put(final K key, final V value)
        { _map.computeIfAbsent( key, k -> new ArrayList<>() ).add(value); }

        public List<V> get(final K key)
        { return _map.getOrDefault( key, new ArrayList<>() ) ; }

        public void remove(final K key)
        { _map.remove(key); }

        public void remove(final K key, final V value)
        {
            final ArrayList<V> values = _map.get(key);

            if(values != null) {
                values.remove(value);
                if( values.isEmpty() ) _map.remove(key);
            }
        }

        public void clear()
        { _map.clear(); }

        public int size()
        { return _map.size(); }

        public boolean isEmpty()
        { return _map.isEmpty(); }

        public boolean containsKey(final K key)
        { return _map.containsKey(key); }

        public Set<K> keySet()
        { return _map.keySet(); }

        public Set< Map.Entry< K, ArrayList<V> > > entrySet()
        { return _map.entrySet(); }

    } // class MultiMap

    @FunctionalInterface
    public static interface TriConsumer<T, U, V> {

        public void accept(final T t, final U u, final V v);

        public default TriConsumer<T, U, V> andThen(final TriConsumer<? super T, ? super U, ? super V> after)
        {
            return (t, u, v) -> {
                this.accept(t, u, v);
                after.accept(t, u, v);
            };
        }

    } // interface TriConsumer

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("this-escape")
    public static class TimeoutMS {
        private final long _timeoutValue_MS;
        private       long _previousTime_MS;

        public TimeoutMS(final long timeoutValue_MS)
        {
            _timeoutValue_MS = timeoutValue_MS;
            reset();
        }

        public void reset()
        { _previousTime_MS = SysUtil.getMS(); }

        public boolean timeout()
        { return ( SysUtil.getMS() - _previousTime_MS ) > _timeoutValue_MS; }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class Mutex extends Semaphore {
        public Mutex()
        { super(1); }

        public void lock()
        { acquireUninterruptibly(); }

        public void unlock()
        { release(); }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum ExecuteResult {
        Done,            // Execution done
        Done_NoUpdate,   // Execution done without any update (for target execution only)
        Error,           // Execution error
        SuppressedError, // Suppressed error; print the error message to stderr
        ProgramExit,     // The function '$exit()' is called
        FunctionReturn,  // The 'return' statement is executed
        LoopContinue,    // Loop only - continue
        LoopBreak,       // Loop only - break
        GoTo             // Go-to statement
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    protected static class LabelMap extends HashMap<String, Integer> {
        public static LabelMap putLabel(final LabelMap labelMap, final String label, final int pos)
        {
            final LabelMap _labelMap = (labelMap != null) ? labelMap : new LabelMap();

            if( _labelMap.get(label) != null ) return null;

            _labelMap.put(label, pos);

            return _labelMap;
        }

        public static int getLabel(final LabelMap labelMap, final String label)
        {
            if(labelMap == null) return -1;

            final Integer pos = labelMap.get(label);

            return (pos == null) ? -1 : pos.intValue();
        }
    };

    protected static interface LabelMapOwner {
        public void putLabel(final String label);
        public int getLabel(final String label);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class VariableStore {
        public boolean constant;    // If this flag is 'true', it indicates that this variable is a constant and will *never* need to be evaluated
        public String  value;       // Value of the variable

        public Pattern regexp;      // Regular expression instance
        public String  replacement; // Replacement value

        public VariableStore()
        {
            constant    = true;
            value       = null;
            regexp      = null;
            replacement = null;
        }

        public VariableStore(final boolean constant_, final String value_)
        {
            constant    = constant_;
            value       = value_;
            regexp      = null;
            replacement = null;
        }

        public VariableStore(final boolean constant_, final String value_, final String regexp_, final String replacement_)
        {
            constant    = constant_;
            value       = value_;
            regexp      = Pattern.compile(regexp_);
            replacement = replacement_;
        }

        public VariableStore(final boolean constant_, final String value_, final Pattern regexp_, final String replacement_)
        {
            constant    = constant_;
            value       = value_;
            regexp      = regexp_;
            replacement = replacement_;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private VariableStore(final VariableStore refVariableStore)
        {
            constant    = refVariableStore.constant;
            value       = refVariableStore.value;
            regexp      = refVariableStore.regexp;
            replacement = refVariableStore.replacement;
        }

        public VariableStore deepClone()
        { return new VariableStore(this); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean equals(final VariableStore refVariableStore)
        {
            if( constant    != refVariableStore.constant                    ) return false;

            if( value       == null && refVariableStore.value       != null ) return false;
            if( value       != null && refVariableStore.value       == null ) return false;
            if( regexp      == null && refVariableStore.regexp      != null ) return false;
            if( regexp      != null && refVariableStore.regexp      == null ) return false;
            if( replacement == null && refVariableStore.replacement != null ) return false;
            if( replacement != null && refVariableStore.replacement == null ) return false;

            if( value       != null && refVariableStore.value       != null && !value           .equals( refVariableStore       .value       ) ) return false;
            if( regexp      != null && refVariableStore.regexp      != null && !regexp.pattern().equals( refVariableStore.regexp.pattern()   ) ) return false;
            if( replacement != null && refVariableStore.replacement != null && !replacement     .equals( refVariableStore       .replacement ) ) return false;

            return true;
        }

        public boolean isVar()
        { return !constant && ( value.charAt(0) == '$' || isWritableSVarSCut(value) ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final Pattern _pmSplitRepStr = Pattern.compile("(?:\\\\\\$\\{[_a-zA-Z][_a-zA-Z0-9]*\\})");

        private String _expandRepStr(final ExecBlock execBlock, final ExecData execData, final String repStr) throws JXMException
        {
            // Simply return the original string if there is no '\${...}' found
            if( repStr.indexOf("\\${") == -1 ) return repStr;

            // Split the string
            final Matcher           matcher = _pmSplitRepStr.matcher(repStr);
            final ArrayList<String> parts   = new ArrayList<>();

            int idx = 0;
            while( matcher.find() ) {
                parts.add( repStr.substring( idx,             matcher.start() ) );
                parts.add( repStr.substring( matcher.start(), matcher.end  () ) );
                idx = matcher.end();
            }
            if( idx < repStr.length() ) parts.add( repStr.substring( idx, repStr.length() ) );

            // Evaluate and combine the string
            final StringBuilder sb = new StringBuilder();

            for(final String p : parts) {
                // If the string is empty, skip it
                if( p.isEmpty() ) continue;
                // If the first character is not '\' then simply store the part
                if( p.charAt(0) != '\\' ) {
                    sb.append(p);
                    continue;
                }
                // Otherwise, evaluate as variable and store
                sb.append(
                    flatten( execData.execState.readVar( execBlock, execData, new ReadVarSpec(false, null, p.substring(1), null, null), true ), "" )
                );
            }
            // Return the evaluated string
            return sb.toString();
        }

        public VariableStore evaluateRegExp(final ExecBlock execBlock, final ExecData execData) throws JXMException
        {
            // Check if the regular expression instance is available
            if(regexp != null) {
                // Error if this value is not a constant
                if(!constant) throw newJXMFatalLogicError(Texts.EMsg_EvalRegExpVarNConst); // NOTE : This should never got executed!
                // Get the replacement string
                String repStr = (replacement != null) ? replacement : "";
                // Perform regular expression replace all and return the result
                return new VariableStore( true, regexp.matcher(value).replaceAll( _expandRepStr(execBlock, execData, repStr) ) );
            }

            // No regular expression instance available, simply return the original value
            return new VariableStore(constant, value);
        }

        public VariableStore evaluateRegExp(final ExecBlock execBlock, final ExecData execData, final VariableStore ref) throws JXMException
        {
            // Check if the regular expression instance is available
            if(ref != null && ref.regexp != null) {
                // Error if this value is not a constant
                if(!constant) throw newJXMFatalLogicError(Texts.EMsg_EvalRegExpVarNConst); // NOTE : This should never got executed!
                // Get the replacement string
                String repStr = (ref.replacement != null) ? ref.replacement : "";
                // Perform regular expression replace all and return the result
                return new VariableStore( true, ref.regexp.matcher(value).replaceAll( _expandRepStr(execBlock, execData, repStr) ) );
            }

            // No regular expression instance available, simply return the original value
            return new VariableStore(constant, value);
        }
    }

    @SuppressWarnings("serial")
    public static class VariableValue extends ArrayList<VariableStore> {
        private boolean _const  = false;
        private String  _depFor = null;

        public VariableValue()
        { super(); }

        @SuppressWarnings("this-escape")
        public VariableValue(final VariableStore value)
        {
            super();
            add(value);
        }

        public void setDeprecated(final String replacement)
        { _depFor = (replacement == null) ? "" : replacement; }

        public String getDeprecated()
        { return _depFor; }

        public void setConstantVariable()
        { _const = true; }

        public boolean isConstantVariable()
        { return _const; }

        public boolean contains(final VariableStore refVariableStore)
        {
            for(final VariableStore item : this) {
                if( item.equals(refVariableStore) ) return true;
            }
            return false;
        }

        public boolean containsConst(final String refValue)
        {
            for(final VariableStore item : this) {
                if( !item.constant               ) continue;
                if(  item.value       == null    ) continue;
                if( !item.value.equals(refValue) ) continue;
                if(  item.regexp      != null    ) continue;
                if(  item.replacement != null    ) continue;
                return true;
            }
            return false;
        }

        public ArrayList<String> cnvValuesToStringArray()
        {
            final ArrayList<String> astr = new ArrayList<>();

            for(VariableStore varStore : this) astr.add(varStore.value);

            return astr;
        }
    };

    @SuppressWarnings("serial")
    public static class VariableMap extends HashMap<String, VariableValue> {
        public VariableMap deepClone()
        {
            final VariableMap varMap = new VariableMap();

            for( final Map.Entry<String, XCom.VariableValue> varVal : this.entrySet() ) {
                final XCom.VariableValue newVarVal = new XCom.VariableValue();
                for( final XCom.VariableStore varStr : varVal.getValue() ) {
                    newVarVal.add( varStr.deepClone() );
                }
                varMap.put( varVal.getKey(), newVarVal );
            }

            return varMap;
        }
    };

    @SuppressWarnings("serial")
    public static class VariableStack extends Stack<VariableMap> {
        public VariableStack deepClone()
        {
            final VariableStack varStk = new VariableStack();

            for( final XCom.VariableMap varMap : this ) {
                final XCom.VariableMap newVarMap = new XCom.VariableMap();
                for( final Map.Entry<String, XCom.VariableValue> varVal : varMap.entrySet() ) {
                    final XCom.VariableValue newVarVal = new XCom.VariableValue();
                    for( final XCom.VariableStore varStr : varVal.getValue() ) {
                        newVarVal.add( varStr.deepClone() );
                    }
                    newVarMap.put( varVal.getKey(), newVarVal );
                }
                varStk.push(newVarMap);
            }

            return varStk;
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class ReadVarSpec extends VariableStore {
        /*
        // ===== Derived from VariableStore =====

               public boolean constant;    // If this flag is 'false', it indicates that this variable is *not* a constant and will need to be evaluated (probably recursively)

               public String  value;       // Value                       (only if 'constant'  is true              ); or
                                           // Variable name               (only if 'svarSpec'  is null              )

               public Pattern regexp;      // Regular expression instance (only if 'value' specifies a variable name)
               public String  replacement; // Replacement value           (only if 'value' specifies a variable name)
        */

        public final SVarSpec svarSpec;    // Specification for special-variable or special-variable-shortcut

        public ReadVarSpec(final boolean constant_, final SVarSpec svarSpec_, final String val_varName_, final Pattern regexp_, final String replacement_)
        {
            // The field 'ReadVarSpec.svarSpec' and 'ReadVarSpec.value' are mutually exclusive, except when:
            //     # It is a constant but not a compile-time constant.
            //     # It is for the special variable '$[preq:V]' which uses the 'SVarSpec.constVal' field to store the index variable name.
            assert (
                   ( svarSpec_ == null                                               || val_varName_ == null )
                || ( svarSpec_ != null && !svarSpec_.svarName.isCompileTimeConstant()                        )
                || ( svarSpec_ != null &&  svarSpec_.svarName == XCom.SVarName.preqV && val_varName_ != null )
            );

            constant    = constant_;
            svarSpec    = svarSpec_;
            value       = val_varName_;
            regexp      = regexp_;
            replacement = replacement_;
        }

        public VariableValue getConstValue()
        {
            final VariableValue varVal = new VariableValue();
            varVal.add( new VariableStore(true, value) );

            return varVal;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean equals(final ReadVarSpec refReadVarSpec)
        {
            if( !super.equals(refReadVarSpec) ) return false;

            if(svarSpec == null && refReadVarSpec.svarSpec != null) return false;
            if(svarSpec != null && refReadVarSpec.svarSpec == null) return false;
            if(svarSpec == null && refReadVarSpec.svarSpec == null) return true;

            return svarSpec.equals(refReadVarSpec.svarSpec);
        }

        public boolean isSVar()
        { return svarSpec != null; }

        public SVarSpec getSVarSpec()
        { return svarSpec; }

        public String getVarName()
        { return isSVar() ? svarSpec.svarName.name() : value; }
    }

    @SuppressWarnings("serial")
    public static class ReadVarSpecs extends ArrayList<ReadVarSpec> {};

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int ASNSpecNumCode_lazy            = 10;
    public static final int ASNSpecNumCode_lazy_concat     = 11;
    public static final int ASNSpecNumCode_lazy_ifNotSet   = 12;
    public static final int ASNSpecNumCode_direct          = 20;
    public static final int ASNSpecNumCode_direct_concat   = 21;
    public static final int ASNSpecNumCode_direct_ifNotSet = 22;

    public static class ASNSpec {
        public final int     numCode;
        public final boolean direct;
        public final boolean concat;
        public final boolean ifNotSet;

        public ASNSpec(final int numCode_, final boolean direct_, final boolean concat_, final boolean ifNotSet_)
        {
            numCode  = numCode_;
            direct   = direct_;
            concat   = concat_;
            ifNotSet = ifNotSet_;
        }
    }

    public static final ASNSpec ASNSpec_lazy            = new ASNSpec(ASNSpecNumCode_lazy           , false, false, false);
    public static final ASNSpec ASNSpec_lazy_concat     = new ASNSpec(ASNSpecNumCode_lazy_concat    , false, true , false);
    public static final ASNSpec ASNSpec_lazy_ifNotSet   = new ASNSpec(ASNSpecNumCode_lazy_ifNotSet  , false, false, true );
    public static final ASNSpec ASNSpec_direct          = new ASNSpec(ASNSpecNumCode_direct         , true , false, false);
    public static final ASNSpec ASNSpec_direct_concat   = new ASNSpec(ASNSpecNumCode_direct_concat  , true , true , false);
    public static final ASNSpec ASNSpec_direct_ifNotSet = new ASNSpec(ASNSpecNumCode_direct_ifNotSet, true , false, true );

    public static ASNSpec getASNSpec(final String asnStr)
    {
        switch(asnStr) {
            case   "=" : return ASNSpec_lazy;
            case  "+=" : return ASNSpec_lazy_concat;
            case  "?=" : return ASNSpec_lazy_ifNotSet;
            case  ":=" : return ASNSpec_direct;
            case "+:=" : return ASNSpec_direct_concat;
            case "?:=" : return ASNSpec_direct_ifNotSet;
            case ":+=" : return ASNSpec_direct_concat;
            case ":?=" : return ASNSpec_direct_ifNotSet;
            default    : return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum CompareType {
        eq_str,
        neq_str,
        eq,
        neq,
        lt,
        lte,
        gt,
        gte
    }

    public static CompareType getCompareType(final String cmpStr)
    {
        if(cmpStr == null) return null;

        switch(cmpStr) {
            case  "&==" : return CompareType.eq_str;
            case  "&!=" : return CompareType.neq_str;
            case   "==" : return CompareType.eq;
            case   "!=" : return CompareType.neq;
            case   "<"  : return CompareType.lt;
            case   "<=" : return CompareType.lte;
            case   ">"  : return CompareType.gt;
            case   ">=" : return CompareType.gte;
            default     : return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class ProgState {
        public final ContainerBlock blockObject; // The block object (function or target) that owns this ProgState instance

        public ProgState(final ContainerBlock blockObject_)
        { blockObject = blockObject_; }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private ProgState(final ProgState refProgState)
        { blockObject = refProgState.blockObject; }

        public ProgState deepClone()
        { return new ProgState(this); }
    }

    @SuppressWarnings("serial")
    public static class ProgStateStack extends Stack<ProgState> {
        private Stack<String> _ftStack = new Stack<>();

        @Override
        public ProgState push(final ProgState item)
        {
            super.push(item);

            if( item.blockObject.getBlockName() != null ) _ftStack.push( item.blockObject.getBlockName() );

            return item;
        }

        @Override
        public ProgState pop()
        {
            final ProgState item = super.pop();

            if( item.blockObject.getBlockName() != null ) _ftStack.pop();

            return item;
        }

        public boolean cboInCallStack(final ContainerBlock blockObject)
        {
            if( blockObject.getBlockName() == null ) return false;

            return _ftStack.contains( blockObject.getBlockName() );
        }

        public ProgStateStack deepClone()
        {
            final ProgStateStack prsStk = new ProgStateStack();

            for( final XCom.ProgState prgSta : this ) {
                prsStk.push( prgSta.deepClone() );
            }

            prsStk._ftStack.addAll(_ftStack);

            return prsStk;
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class TargetStack extends Stack<Target>{
        public TargetStack deepClone()
        {
            final TargetStack trgStk = new TargetStack();

            for( final Target trg : this ) {
                trgStk.push( (Target) trg.deepClone() );
            }

            return trgStk;
        }
    }

    @SuppressWarnings("serial")
    public static class ExecBlocks extends ArrayList<ExecBlock> {
        public ExecBlocks deepClone()
        {
            final ExecBlocks excBlk = new ExecBlocks();

            for( final ExecBlock item : this ) {
                excBlk.add( item.deepClone() );
            }

            return excBlk;
        }
    };

    @SuppressWarnings("serial")
    public static class FunctionMap extends HashMap<String, jxm.xb.Function> {
        public FunctionMap deepClone()
        {
            final FunctionMap funMap = new FunctionMap();

            for( final Map.Entry<String, jxm.xb.Function> item : this.entrySet() ) {
                funMap.put( item.getKey(), (jxm.xb.Function) item.getValue().deepClone() );
            }

            return funMap;
        }
    };

    @SuppressWarnings("serial")
    public static class TargetMap extends HashMap<String, Target>{
        public TargetMap deepClone()
        {
            final TargetMap trgMap = new TargetMap();

            for( final Map.Entry<String, Target> item : this.entrySet() ) {
                trgMap.put( item.getKey(), (Target) item.getValue().deepClone() );
            }

            return trgMap;
        }
    };

    public static class ExecData {
        // Operational manager for shell/operating system commands
        public final ShellOper   shellOper;
        public       boolean     enShellOper; // Will be set to true when calling a function via '$exec' so that shell/operating system commands can be executed

        // Program execution state
        public final ExecState   execState;
        public final boolean     inThread;

        // These maps will be populated after the root execution-blocks are executed
        public final FunctionMap functionMap;
        public final TargetMap   targetMap;

        // The latest file time
        public final FileTime    latestFileTime;

        public ExecData(final FileTime latestFileTime_)
        {
            shellOper      = new ShellOper();
            enShellOper    = false;

            execState      = new ExecState(shellOper);
            inThread       = false;

            functionMap    = new FunctionMap();
            targetMap      = new TargetMap  ();

            latestFileTime = latestFileTime_;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private ExecData(final ExecData refExecData, final boolean inThread_)
        {
            targetMap      = refExecData.targetMap  .deepClone();
            functionMap    = refExecData.functionMap.deepClone();

            execState      = refExecData.execState  .deepClone();
            inThread       = inThread_;

            enShellOper    = refExecData.enShellOper;
            shellOper      = execState.getShellOper(); // Get the instance from the cloned 'execState'

            latestFileTime = refExecData.latestFileTime;
        }

        public ExecData deepClone(final boolean inThread)
        { return new ExecData(this, inThread); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String        CallbackFunc_Prefix    = "cb:";

    public static final long[]        LongArray_NoValue      = new long  [] {};
    public static final String[]      StringArray_NoValue    = new String[] {};

    public static final VariableValue VarVal_EmptyValue      = new VariableValue      (                          );

    public static final VariableStore VarStr_DotString       = new VariableStore      (true, "."                 );
    public static final VariableValue VarVal_DotString       = new VariableValue      (VarStr_DotString          );

    public static final VariableStore VarStr_EmptyString     = new VariableStore      (true, ""                  );
    public static final int           IHC_VarStr_EmptyString = System.identityHashCode(VarStr_EmptyString        );

    public static final VariableValue VarVal_EmptyString     = new VariableValue      (VarStr_EmptyString        );
    public static final int           IHC_VarVal_EmptyString = System.identityHashCode(VarVal_EmptyString        );

    public static final ReadVarSpec   RVSpec_EmptyString     = new ReadVarSpec        (true, null, "", null, null);
    public static final int           IHC_RVSpec_EmptyString = System.identityHashCode(RVSpec_EmptyString        );

    public static final ReadVarSpecs  RVSpcs_Empty           = new ReadVarSpecs       (                          );

    @SuppressWarnings("serial")
    public static final ReadVarSpecs  RVSpcs_EmptyString     = new ReadVarSpecs       (                          ) {{ add(RVSpec_EmptyString); }};
    public static final int           IHC_RVSpcs_EmptyString = System.identityHashCode(RVSpcs_EmptyString        );

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final ReadVarSpecs RVSpcs_EmptyList = new ReadVarSpecs();

    // Use this function for a little bit of optimization when storing a possibly empty 'ReadVarSpecs' instance
    public static ReadVarSpecs substEmptyReadVarSpecs(final ReadVarSpecs rvarSpecs)
    { return ( rvarSpecs == null || !rvarSpecs.isEmpty() ) ? rvarSpecs : XCom.RVSpcs_EmptyList; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final boolean isPlainRefVarName(final String str)
    { return str.charAt(0) == '^'; }

    public static final String extractPlainRefVarName(final String str)
    { return str.substring(1); }

    public static final String extractRefVarName(final String str)
    { return str.substring(0, 2) + str.substring(3); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final HashSet<String> _keywordList = new HashSet<>();

    public static boolean isKeyword(final String symName)
    {
        // Initialize the map once and only once
        if( _keywordList.isEmpty() ) {
            _keywordList.add("jxmake"     );
            _keywordList.add("+jxmake"    );
            _keywordList.add("jxwait"     );
            _keywordList.add("depload"    );
            _keywordList.add("sdepload"   );
            _keywordList.add("echo"       );
            _keywordList.add("echoln"     );
            _keywordList.add("local"      );
            _keywordList.add("const"      );
            _keywordList.add("unset"      );
            _keywordList.add("function"   );
            _keywordList.add("endfunction");
            _keywordList.add("supersede"  );
            _keywordList.add("__origin__" );
            _keywordList.add("target"     );
            _keywordList.add("endtarget"  );
            _keywordList.add("extradep"   );
            _keywordList.add("label"      );
            _keywordList.add("goto"       );
            _keywordList.add("sgoto"      );
            _keywordList.add("if"         );
            _keywordList.add("elif"       );
            _keywordList.add("else"       );
            _keywordList.add("endif"      );
            _keywordList.add("for"        );
            _keywordList.add("to"         );
            _keywordList.add("step"       );
            _keywordList.add("endfor"     );
            _keywordList.add("foreach"    );
            _keywordList.add("in"         );
            _keywordList.add("skip"       );
            _keywordList.add("endforeach" );
            _keywordList.add("while"      );
            _keywordList.add("endwhile"   );
            _keywordList.add("do"         );
            _keywordList.add("whilst"     );
            _keywordList.add("repeat"     );
            _keywordList.add("until"      );
            _keywordList.add("loop"       );
            _keywordList.add("endloop"    );
            _keywordList.add("continue"   );
            _keywordList.add("break"      );
            _keywordList.add("return"     );
            _keywordList.add("inc"        );
            _keywordList.add("dec"        );
            _keywordList.add("add"        );
            _keywordList.add("sub"        );
            _keywordList.add("mul"        );
            _keywordList.add("div"        );
            _keywordList.add("mod"        );
            _keywordList.add("abs"        );
            _keywordList.add("neg"        );
            _keywordList.add("shl"        );
            _keywordList.add("shr"        );
            _keywordList.add("min"        );
            _keywordList.add("max"        );
            _keywordList.add("not"        );
            _keywordList.add("and"        );
            _keywordList.add("or"         );
            _keywordList.add("xor"        );
            _keywordList.add("eval"       );
            _keywordList.add("deprecate"  );
            _keywordList.add("deprecated" );
            _keywordList.add("by"         );
        }

        // Check if the given symbol name is a keyword
        return _keywordList.contains( isPlainRefVarName(symName) ? extractPlainRefVarName(symName) : symName );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String  _reStrSymbolNameUnicode = "(?:\\p{Alpha}|_)(?:\\p{Alnum}|_)*";

  //public static final Pattern _pmSymbolName = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*$");
    public static final Pattern _pmSymbolName = Pattern.compile('^' + _reStrSymbolNameUnicode + '$', Pattern.UNICODE_CHARACTER_CLASS);

    public static boolean isSymbolName(final String str)
    { return _pmSymbolName.matcher(str).matches(); }

    public static boolean isSymbolName(final String str, final boolean forRegVarName)
    {
        if( forRegVarName && isPlainRefVarName(str) ) return isSymbolName( extractPlainRefVarName(str) );
        else                                          return isSymbolName(                        str  );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum FuncName {
        call,
        exec,
        add_target,
        add_extradep,
        has_var,
        nop,
        alt_glibc_for,
        sh_delay,
        sh_restore,
        cmd_echo_off,
        cmd_echo_on,
        cmd_streaming_off,
        cmd_streaming_on,
        cmd_stderr_chk_off,
        cmd_stderr_chk_on,
        cmd_stdout_chk_off,
        cmd_stdout_chk_on,
        cmd_clear_state,
        silent_sem,
        restore_sem,
        micros,
        millis,
        datetime,
        sleep,
        getenv,
        clear_project,
        exit,
        cwd,
        ptd,
        uhd,
        jdd,
        jtd,
        jxd,
        set_rwx3,
        cat_path,
        cat_paths,
        abs_path,
        rel_path,
        valid_path,
        uptodate_path,
        newer_path,
        same_path,
        path_is_abs,
        path_is_rel,
        path_is_file,
        path_is_directory,
        path_is_symlink,
        path_is_readable,
        path_is_writable,
        path_is_executable,
        path_last_part,
        path_rm_last_part,
        path_ndsep,
        symlink_target,
        symlink_real_apath,
        symlink_resolve,
        dir_name,
        file_name,
        file_ext,
        file_mime_type,
        touch,
        rmfile,
        rmfiles,
        rmfiles_rec,
        cpfile,
        mvfile,
        mkdir,
        rmdir,
        rmdir_rec,
        cpdir_rec,
        lsdir,
        lsdir_rec,
        srfile_rec,
        srdir_rec,
        tzstdir_rec,
        untzst_rec,
        txzdir_rec,
        untxz_rec,
        tbz2dir_rec,
        untbz2_rec,
        tgzdir_rec,
        untgz_rec,
        tzipdir_rec,
        untzip_rec,
        untar_rec,
        unzip_rec,
        zst,
        unzst,
        xz,
        unxz,
        bzip2,
        bunzip2,
        gzip,
        gunzip,
        put_file,
        get_file,
        get_file_nel,
        md2_sum_file,
        md5_sum_file,
        sha1_sum_file,
        sha2_224sum_file,
        sha2_256sum_file,
        sha2_384sum_file,
        sha2_512sum_file,
        sha3_224sum_file,
        sha3_256sum_file,
        sha3_384sum_file,
        sha3_512sum_file,
        to_cp,
        to_ch,
        ucase,
        lcase,
        tcase,
        ltrim,
        rtrim,
        strim,
        strlen,
        substr,
        str_replace,
        str_fidx,
        str_lidx,
        str_begwith,
        str_endwith,
        str_rmfchr,
        str_rmlchr,
        re_from_glob,
        re_match,
        re_split,
        re_replace,
        re_quote,
        re_quote_repval,
        color_udiff,
        explode,
        sfchars,
        sfspaces,
        sfdots,
        sfpipes,
        sflines,
        sfdseps,
        sfpseps,
        implode,
        flatten,
        ftchars,
        ftspaces,
        ftdots,
        ftpipes,
        ftlines,
        ftdseps,
        ftpseps,
        part_count,
        partn,
        partnm,
        part_fidx,
        part_lidx,
        part_remove,
        part_insert,
        part_replace,
        contains,
        lookup,
        unique,
        erase_if,
        erase_ifnot,
        erase_ifempty,
        repeat,
        stack,
        series,
        interleave,
        ftt_stack,
        sort_ascending,
        sort_descending,
        map_new,
        map_new_from,
        map_delete,
        map_clear,
        map_put,
        map_add,
        map_remove,
        map_get,
        map_keys,
        map_num_keys,
        map_num_vals,
        map_to_edata,
        map_from_edata,
        map_valid_handle,
        nmap_from_json,
        nmap_delete,
        stk_new,
        stk_delete,
        stk_clear,
        stk_push,
        stk_peek,
        stk_pop,
        stk_num_elems,
        stk_to_edata,
        stk_from_edata,
        stk_valid_handle,
        bse_new,
        bse_delete,
        bse_size,
        bse_cursor,
        bse_seek_abs,
        bse_seek_beg,
        bse_seek_end,
        bse_seek_cur,
        bse_truncate,
        bse_save_file,
        bse_load_file,
        bse_save_base64str,
        bse_load_base64str,
        bse_set_be,
        bse_set_le,
        bse_wr_byte,
        bse_rd_byte,
        bse_wr_uint08,
        bse_wr_sint08,
        bse_rd_uint08,
        bse_rd_sint08,
        bse_wr_uint16,
        bse_wr_sint16,
        bse_rd_uint16,
        bse_rd_sint16,
        bse_wr_uint32,
        bse_wr_sint32,
        bse_rd_uint32,
        bse_rd_sint32,
        bse_wr_uint64,
        bse_wr_sint64,
        bse_rd_uint64,
        bse_rd_sint64,
        bse_wr_flt32,
        bse_rd_flt32,
        bse_wr_dbl64,
        bse_rd_dbl64,
        bse_wr_utf8,
        bse_rd_utf8,
        bse_wr_utf16,
        bse_rd_utf16,
        bse_wr_utf32,
        bse_rd_utf32,
        bse_range_clr,
        bse_range_cpy,
        bse_range_cut,
        bse_range_pst,
        bse_range_ovr,
        bse_valid_handle,
        fwc_new,
        fwc_delete,
        fwc_load_raw_bin,
        fwc_load_elf_bin,
        fwc_load_intel_hex,
        fwc_load_moto_srec,
        fwc_load_tektx_hex,
        fwc_load_mos_tech,
        fwc_load_titxt_hex,
        fwc_load_ascii_hex,
        fwc_load_vl_vmem,
        fwc_save_raw_bin,
        fwc_save_intel_hex,
        fwc_save_moto_srec,
        fwc_save_tektx_hex,
        fwc_save_mos_tech,
        fwc_save_titxt_hex,
        fwc_clear,
        fwc_min_start_addr,
        fwc_max_final_addr,
        fwc_equals,
        fwc_compose,
        cmp,
        not,
        and,
        or,
        csel,
        printf,
        sprintf,
        vprintf,
        vsprintf,
        read_line,
        read_pswd,
        resolve_ip,
        http_head,
        http_get,
        http_post,
        http_download,
        http_set_auth,
        http_clr_auth,
        ssl_trust_all,
        gh_get_tags,
        gh_get_assets,
        ls_serial_ports,
      //set_baudrate,
        mcu_reset,
        mcu_bootloader,
        mcu_prog_cfg_json,
        mcu_prog_cfg_ini,
        mcu_prog_exec,
        serial_console,
        tcp_serial_console,
        serial_plotter,
        tcp_serial_plotter,
        xmlframe_file,
        xmlframe_string,
        xml_escape,
        aboard_from_file,
        aboard_selector,
        aboard_getselconf,
        esp_st_decoder,
        gdl_cpp_include,
        gdl_cpp_module,
        gdl_java_import,
        pp_java_scf,

        // Special tester
        __dummy_dep_1__,
        __dummy_dep_2__,
        __dummy_rep_2__,

        // End marker
        __END__
    }

    public static class FuncSpec {
        public final FuncName fnName; // The function name (in enum)
        public final FuncName fnDfNm; // A non null value indicates that this function has been deprecated by the one specified here
        public final boolean  retVal; // A flag that indicates if the function returns a value
        public final int      reqCnt; // The number of required parameter(s)
        public final int      optCnt; // The number of optional parameter(s); -1 means variadic number of parameters

        public final boolean  supErr; // A flag that indicate if error should be ignored

        public FuncSpec(final FuncName fnName_, final boolean retVal_, final int reqCnt_, final int optCnt_)
        { this(fnName_, retVal_, reqCnt_, optCnt_, null); }

        public FuncSpec(final FuncName fnName_, final boolean retVal_, final int reqCnt_, final int optCnt_, final FuncName fnDfNm_)
        {
            fnName = fnName_;
            fnDfNm = fnDfNm_;
            retVal = retVal_;
            reqCnt = reqCnt_;
            optCnt = optCnt_;
            supErr = false;
        }

        public FuncSpec(final FuncSpec funcSpec, boolean supErr_)
        {
            fnName = funcSpec.fnName;
            fnDfNm = funcSpec.fnDfNm;
            retVal = funcSpec.retVal;
            reqCnt = funcSpec.reqCnt;
            optCnt = funcSpec.optCnt;
            supErr = supErr_;
        }

        public String functionName()
        { return '$' + fnName.name(); }
    }

    private static final Pattern                   _pmFuncName = Pattern.compile("^-?\\$[_a-zA-Z][_a-zA-Z0-9]*$");
    private static final HashMap<String, FuncSpec> _funcList   = new HashMap<>();

    public static String normalizeFunctionName(final String funcName)
    {
        // The function '$call' and '$exec' cannot be preceded  by the '-' character and thus cannot be normalized
        if( funcName.equals("-$call") || funcName.equals("-$exec") ) return funcName;

        // Return the normalized function name
        return ( funcName.charAt(0) != '-' ) ? funcName : funcName.substring( 1, funcName.length() );
    }

    public static FuncSpec getFuncSpec(final String funcName)
    {
        // Initialize the map once and only once
        if( _funcList.isEmpty() ) {
            _funcList.put( "$call"              , new FuncSpec(FuncName.call              , true , 1, -1) );
            _funcList.put( "$exec"              , new FuncSpec(FuncName.exec              , true , 1, -1) );
            _funcList.put( "$add_target"        , new FuncSpec(FuncName.add_target        , false, 2,  2) );
            _funcList.put( "$add_extradep"      , new FuncSpec(FuncName.add_extradep      , false, 2,  1) );
            _funcList.put( "$has_var"           , new FuncSpec(FuncName.has_var           , true , 1,  0) );
            _funcList.put( "$nop"               , new FuncSpec(FuncName.nop               , false, 0, -1) );
            _funcList.put( "$alt_glibc_for"     , new FuncSpec(FuncName.alt_glibc_for     , false, 3,  0) );
            _funcList.put( "$sh_delay"          , new FuncSpec(FuncName.sh_delay          , false, 0,  0) );
            _funcList.put( "$sh_restore"        , new FuncSpec(FuncName.sh_restore        , false, 0,  0) );
            _funcList.put( "$cmd_echo_off"      , new FuncSpec(FuncName.cmd_echo_off      , false, 0,  0) );
            _funcList.put( "$cmd_echo_on"       , new FuncSpec(FuncName.cmd_echo_on       , false, 0,  0) );
            _funcList.put( "$cmd_streaming_off" , new FuncSpec(FuncName.cmd_streaming_off , false, 0,  0) );
            _funcList.put( "$cmd_streaming_on"  , new FuncSpec(FuncName.cmd_streaming_on  , false, 0,  0) );
            _funcList.put( "$cmd_stderr_chk_off", new FuncSpec(FuncName.cmd_stderr_chk_off, false, 0,  0) );
            _funcList.put( "$cmd_stderr_chk_on" , new FuncSpec(FuncName.cmd_stderr_chk_on , false, 0,  0) );
            _funcList.put( "$cmd_stdout_chk_off", new FuncSpec(FuncName.cmd_stdout_chk_off, false, 0,  0) );
            _funcList.put( "$cmd_stdout_chk_on" , new FuncSpec(FuncName.cmd_stdout_chk_on , false, 0,  0) );
            _funcList.put( "$cmd_clear_state"   , new FuncSpec(FuncName.cmd_clear_state   , false, 0,  0) );
            _funcList.put( "$silent_sem"        , new FuncSpec(FuncName.silent_sem        , false, 0,  0) );
            _funcList.put( "$restore_sem"       , new FuncSpec(FuncName.restore_sem       , false, 0,  0) );
            _funcList.put( "$micros"            , new FuncSpec(FuncName.micros            , true , 0,  0) );
            _funcList.put( "$millis"            , new FuncSpec(FuncName.millis            , true , 0,  0) );
            _funcList.put( "$datetime"          , new FuncSpec(FuncName.datetime          , true , 0,  1) );
            _funcList.put( "$sleep"             , new FuncSpec(FuncName.sleep             , false, 1,  0) );
            _funcList.put( "$getenv"            , new FuncSpec(FuncName.getenv            , true , 1,  1) );
            _funcList.put( "$clear_project"     , new FuncSpec(FuncName.clear_project     , false, 0,  1) );
            _funcList.put( "$exit"              , new FuncSpec(FuncName.exit              , false, 1,  0) );
            _funcList.put( "$cwd"               , new FuncSpec(FuncName.cwd               , true , 0,  0) );
            _funcList.put( "$ptd"               , new FuncSpec(FuncName.ptd               , true , 0,  0) );
            _funcList.put( "$uhd"               , new FuncSpec(FuncName.uhd               , true , 0,  0) );
            _funcList.put( "$jdd"               , new FuncSpec(FuncName.jdd               , true , 0,  0) );
            _funcList.put( "$jtd"               , new FuncSpec(FuncName.jtd               , true , 0,  0) );
            _funcList.put( "$jxd"               , new FuncSpec(FuncName.jxd               , true , 0,  0) );
            _funcList.put( "$set_rwx3"          , new FuncSpec(FuncName.set_rwx3          , false, 2,  0) );
            _funcList.put( "$cat_path"          , new FuncSpec(FuncName.cat_path          , true , 2,  0) );
            _funcList.put( "$cat_paths"         , new FuncSpec(FuncName.cat_paths         , true , 2, -1) );
            _funcList.put( "$abs_path"          , new FuncSpec(FuncName.abs_path          , true , 1,  0) );
            _funcList.put( "$rel_path"          , new FuncSpec(FuncName.rel_path          , true , 1,  0) );
            _funcList.put( "$valid_path"        , new FuncSpec(FuncName.valid_path        , true , 1,  0) );
            _funcList.put( "$uptodate_path"     , new FuncSpec(FuncName.uptodate_path     , true , 1,  0) );
            _funcList.put( "$newer_path"        , new FuncSpec(FuncName.newer_path        , true , 2,  0) );
            _funcList.put( "$same_path"         , new FuncSpec(FuncName.same_path         , true , 2,  0) );
            _funcList.put( "$path_is_abs"       , new FuncSpec(FuncName.path_is_abs       , true , 1,  0) );
            _funcList.put( "$path_is_rel"       , new FuncSpec(FuncName.path_is_rel       , true , 1,  0) );
            _funcList.put( "$path_is_file"      , new FuncSpec(FuncName.path_is_file      , true , 1,  0) );
            _funcList.put( "$path_is_directory" , new FuncSpec(FuncName.path_is_directory , true , 1,  0) );
            _funcList.put( "$path_is_symlink"   , new FuncSpec(FuncName.path_is_symlink   , true , 1,  0) );
            _funcList.put( "$path_is_readable"  , new FuncSpec(FuncName.path_is_readable  , true , 1,  0) );
            _funcList.put( "$path_is_writable"  , new FuncSpec(FuncName.path_is_writable  , true , 1,  0) );
            _funcList.put( "$path_is_executable", new FuncSpec(FuncName.path_is_executable, true , 1,  0) );
            _funcList.put( "$path_last_part"    , new FuncSpec(FuncName.path_last_part    , true , 1,  0) );
            _funcList.put( "$path_rm_last_part" , new FuncSpec(FuncName.path_rm_last_part , true , 1,  0) );
            _funcList.put( "$path_ndsep"        , new FuncSpec(FuncName.path_ndsep        , true , 1,  0) );
            _funcList.put( "$symlink_target"    , new FuncSpec(FuncName.symlink_target    , true , 1,  0) );
            _funcList.put( "$symlink_real_apath", new FuncSpec(FuncName.symlink_real_apath, true , 1,  0) );
            _funcList.put( "$symlink_resolve"   , new FuncSpec(FuncName.symlink_resolve   , true , 1,  0) );
            _funcList.put( "$dir_name"          , new FuncSpec(FuncName.dir_name          , true , 1,  0) );
            _funcList.put( "$file_name"         , new FuncSpec(FuncName.file_name         , true , 1,  1) );
            _funcList.put( "$file_ext"          , new FuncSpec(FuncName.file_ext          , true , 1,  0) );
            _funcList.put( "$file_mime_type"    , new FuncSpec(FuncName.file_mime_type    , true , 1,  0) );
            _funcList.put( "$touch"             , new FuncSpec(FuncName.touch             , false, 1,  0) );
            _funcList.put( "$rmfile"            , new FuncSpec(FuncName.rmfile            , false, 1,  0) );
            _funcList.put( "$rmfiles"           , new FuncSpec(FuncName.rmfiles           , false, 2,  0) );
            _funcList.put( "$rmfiles_rec"       , new FuncSpec(FuncName.rmfiles_rec       , false, 2,  0) );
            _funcList.put( "$cpfile"            , new FuncSpec(FuncName.cpfile            , false, 2,  2) );
            _funcList.put( "$mvfile"            , new FuncSpec(FuncName.mvfile            , false, 2,  1) );
            _funcList.put( "$mkdir"             , new FuncSpec(FuncName.mkdir             , false, 1,  0) );
            _funcList.put( "$rmdir"             , new FuncSpec(FuncName.rmdir             , false, 1,  0) );
            _funcList.put( "$rmdir_rec"         , new FuncSpec(FuncName.rmdir_rec         , false, 1,  0) );
            _funcList.put( "$cpdir_rec"         , new FuncSpec(FuncName.cpdir_rec         , false, 3,  0) );
            _funcList.put( "$lsdir"             , new FuncSpec(FuncName.lsdir             , true , 1,  1) );
            _funcList.put( "$lsdir_rec"         , new FuncSpec(FuncName.lsdir_rec         , true , 1,  2) );
            _funcList.put( "$srfile_rec"        , new FuncSpec(FuncName.srfile_rec        , true , 2,  1) );
            _funcList.put( "$srdir_rec"         , new FuncSpec(FuncName.srdir_rec         , true , 2,  1) );
            _funcList.put( "$tzstdir_rec"       , new FuncSpec(FuncName.tzstdir_rec       , true , 2,  2) );
            _funcList.put( "$untzst_rec"        , new FuncSpec(FuncName.untzst_rec        , true , 2,  1) );
            _funcList.put( "$txzdir_rec"        , new FuncSpec(FuncName.txzdir_rec        , true , 2,  2) );
            _funcList.put( "$untxz_rec"         , new FuncSpec(FuncName.untxz_rec         , true , 2,  1) );
            _funcList.put( "$tbz2dir_rec"       , new FuncSpec(FuncName.tbz2dir_rec       , true , 2,  2) );
            _funcList.put( "$untbz2_rec"        , new FuncSpec(FuncName.untbz2_rec        , true , 2,  1) );
            _funcList.put( "$tgzdir_rec"        , new FuncSpec(FuncName.tgzdir_rec        , true , 2,  2) );
            _funcList.put( "$untgz_rec"         , new FuncSpec(FuncName.untgz_rec         , true , 2,  1) );
            _funcList.put( "$tzipdir_rec"       , new FuncSpec(FuncName.tzipdir_rec       , true , 2,  2) );
            _funcList.put( "$untzip_rec"        , new FuncSpec(FuncName.untzip_rec        , true , 2,  1) );
            _funcList.put( "$untar_rec"         , new FuncSpec(FuncName.untar_rec         , true , 2,  1) );
            _funcList.put( "$unzip_rec"         , new FuncSpec(FuncName.unzip_rec         , true , 2,  1) );
            _funcList.put( "$zst"               , new FuncSpec(FuncName.zst               , true , 2,  1) );
            _funcList.put( "$unzst"             , new FuncSpec(FuncName.unzst             , true , 2,  1) );
            _funcList.put( "$xz"                , new FuncSpec(FuncName.xz                , true , 2,  1) );
            _funcList.put( "$unxz"              , new FuncSpec(FuncName.unxz              , true , 2,  1) );
            _funcList.put( "$bzip2"             , new FuncSpec(FuncName.bzip2             , true , 2,  1) );
            _funcList.put( "$bunzip2"           , new FuncSpec(FuncName.bunzip2           , true , 2,  1) );
            _funcList.put( "$gzip"              , new FuncSpec(FuncName.gzip              , true , 2,  1) );
            _funcList.put( "$gunzip"            , new FuncSpec(FuncName.gunzip            , true , 2,  1) );
            _funcList.put( "$put_file"          , new FuncSpec(FuncName.put_file          , true , 2,  0) );
            _funcList.put( "$get_file"          , new FuncSpec(FuncName.get_file          , true , 1,  1) );
            _funcList.put( "$get_file_nel"      , new FuncSpec(FuncName.get_file_nel      , true , 1,  2) );
            _funcList.put( "$md2_sum_file"      , new FuncSpec(FuncName.md2_sum_file      , true , 1,  0) );
            _funcList.put( "$md5_sum_file"      , new FuncSpec(FuncName.md5_sum_file      , true , 1,  0) );
            _funcList.put( "$sha1_sum_file"     , new FuncSpec(FuncName.sha1_sum_file     , true , 1,  0) );
            _funcList.put( "$sha2_224sum_file"  , new FuncSpec(FuncName.sha2_224sum_file  , true , 1,  0) );
            _funcList.put( "$sha2_256sum_file"  , new FuncSpec(FuncName.sha2_256sum_file  , true , 1,  0) );
            _funcList.put( "$sha2_384sum_file"  , new FuncSpec(FuncName.sha2_384sum_file  , true , 1,  0) );
            _funcList.put( "$sha2_512sum_file"  , new FuncSpec(FuncName.sha2_512sum_file  , true , 1,  0) );
            _funcList.put( "$sha3_224sum_file"  , new FuncSpec(FuncName.sha3_224sum_file  , true , 1,  0) );
            _funcList.put( "$sha3_256sum_file"  , new FuncSpec(FuncName.sha3_256sum_file  , true , 1,  0) );
            _funcList.put( "$sha3_384sum_file"  , new FuncSpec(FuncName.sha3_384sum_file  , true , 1,  0) );
            _funcList.put( "$sha3_512sum_file"  , new FuncSpec(FuncName.sha3_512sum_file  , true , 1,  0) );
            _funcList.put( "$to_cp"             , new FuncSpec(FuncName.to_cp             , true , 1,  0) );
            _funcList.put( "$to_ch"             , new FuncSpec(FuncName.to_ch             , true , 1,  0) );
            _funcList.put( "$ucase"             , new FuncSpec(FuncName.ucase             , true , 1,  0) );
            _funcList.put( "$lcase"             , new FuncSpec(FuncName.lcase             , true , 1,  0) );
            _funcList.put( "$tcase"             , new FuncSpec(FuncName.tcase             , true , 1,  0) );
            _funcList.put( "$ltrim"             , new FuncSpec(FuncName.ltrim             , true , 1,  0) );
            _funcList.put( "$rtrim"             , new FuncSpec(FuncName.rtrim             , true , 1,  0) );
            _funcList.put( "$strim"             , new FuncSpec(FuncName.strim             , true , 1,  0) );
            _funcList.put( "$strlen"            , new FuncSpec(FuncName.strlen            , true , 1,  0) );
            _funcList.put( "$substr"            , new FuncSpec(FuncName.substr            , true , 3,  0) );
            _funcList.put( "$str_replace"       , new FuncSpec(FuncName.str_replace       , true , 3,  0) );
            _funcList.put( "$str_fidx"          , new FuncSpec(FuncName.str_fidx          , true , 2,  1) );
            _funcList.put( "$str_lidx"          , new FuncSpec(FuncName.str_lidx          , true , 2,  1) );
            _funcList.put( "$str_begwith"       , new FuncSpec(FuncName.str_begwith       , true , 2,  0) );
            _funcList.put( "$str_endwith"       , new FuncSpec(FuncName.str_endwith       , true , 2,  0) );
            _funcList.put( "$str_rmfchr"        , new FuncSpec(FuncName.str_rmfchr        , true , 1,  0) );
            _funcList.put( "$str_rmlchr"        , new FuncSpec(FuncName.str_rmlchr        , true , 1,  0) );
            _funcList.put( "$re_from_glob"      , new FuncSpec(FuncName.re_from_glob      , true , 1,  0) );
            _funcList.put( "$re_match"          , new FuncSpec(FuncName.re_match          , true , 2,  0) );
            _funcList.put( "$re_split"          , new FuncSpec(FuncName.re_split          , true , 2,  0) );
            _funcList.put( "$re_replace"        , new FuncSpec(FuncName.re_replace        , true , 3,  0) );
            _funcList.put( "$re_quote"          , new FuncSpec(FuncName.re_quote          , true , 1,  0) );
            _funcList.put( "$re_quote_repval"   , new FuncSpec(FuncName.re_quote_repval   , true , 1,  0) );
            _funcList.put( "$color_udiff"       , new FuncSpec(FuncName.color_udiff       , true , 1,  0) );
            _funcList.put( "$explode"           , new FuncSpec(FuncName.explode           , true , 1,  1) );
            _funcList.put( "$sfchars"           , new FuncSpec(FuncName.sfchars           , true , 1,  0) );
            _funcList.put( "$sfspaces"          , new FuncSpec(FuncName.sfspaces          , true , 1,  0) );
            _funcList.put( "$sfdots"            , new FuncSpec(FuncName.sfdots            , true , 1,  0) );
            _funcList.put( "$sfpipes"           , new FuncSpec(FuncName.sfpipes           , true , 1,  0) );
            _funcList.put( "$sflines"           , new FuncSpec(FuncName.sflines           , true , 1,  0) );
            _funcList.put( "$sfdseps"           , new FuncSpec(FuncName.sfdseps           , true , 1,  0) );
            _funcList.put( "$sfpseps"           , new FuncSpec(FuncName.sfpseps           , true , 1,  0) );
            _funcList.put( "$implode"           , new FuncSpec(FuncName.implode           , true , 1,  1) );
            _funcList.put( "$flatten"           , new FuncSpec(FuncName.flatten           , true , 1,  0) );
            _funcList.put( "$ftchars"           , new FuncSpec(FuncName.ftchars           , true , 1,  0) );
            _funcList.put( "$ftspaces"          , new FuncSpec(FuncName.ftspaces          , true , 1,  0) );
            _funcList.put( "$ftdots"            , new FuncSpec(FuncName.ftdots            , true , 1,  0) );
            _funcList.put( "$ftpipes"           , new FuncSpec(FuncName.ftpipes           , true , 1,  0) );
            _funcList.put( "$ftlines"           , new FuncSpec(FuncName.ftlines           , true , 1,  0) );
            _funcList.put( "$ftdseps"           , new FuncSpec(FuncName.ftdseps           , true , 1,  0) );
            _funcList.put( "$ftpseps"           , new FuncSpec(FuncName.ftpseps           , true , 1,  0) );
            _funcList.put( "$part_count"        , new FuncSpec(FuncName.part_count        , true , 1,  0) );
            _funcList.put( "$partn"             , new FuncSpec(FuncName.partn             , true , 2,  1) );
            _funcList.put( "$partnm"            , new FuncSpec(FuncName.partnm            , true , 3,  0) );
            _funcList.put( "$part_fidx"         , new FuncSpec(FuncName.part_fidx         , true , 2,  1) );
            _funcList.put( "$part_lidx"         , new FuncSpec(FuncName.part_lidx         , true , 2,  1) );
            _funcList.put( "$part_remove"       , new FuncSpec(FuncName.part_remove       , true , 2,  0) );
            _funcList.put( "$part_insert"       , new FuncSpec(FuncName.part_insert       , true , 3,  0) );
            _funcList.put( "$part_replace"      , new FuncSpec(FuncName.part_replace      , true , 3,  0) );
            _funcList.put( "$contains"          , new FuncSpec(FuncName.contains          , true , 2,  0) );
            _funcList.put( "$lookup"            , new FuncSpec(FuncName.lookup            , true , 3,  0) );
            _funcList.put( "$unique"            , new FuncSpec(FuncName.unique            , true , 1,  0) );
            _funcList.put( "$erase_if"          , new FuncSpec(FuncName.erase_if          , true , 2,  1) );
            _funcList.put( "$erase_ifnot"       , new FuncSpec(FuncName.erase_ifnot       , true , 2,  1) );
            _funcList.put( "$erase_ifempty"     , new FuncSpec(FuncName.erase_ifempty     , true , 1,  0) );
            _funcList.put( "$repeat"            , new FuncSpec(FuncName.repeat            , true , 2,  0) );
            _funcList.put( "$stack"             , new FuncSpec(FuncName.stack             , true , 2, -1) );
            _funcList.put( "$series"            , new FuncSpec(FuncName.series            , true , 2, -1) );
            _funcList.put( "$interleave"        , new FuncSpec(FuncName.interleave        , true , 2, -1) );
            _funcList.put( "$ftt_stack"         , new FuncSpec(FuncName.ftt_stack         , true , 2, -1) );
            _funcList.put( "$sort_ascending"    , new FuncSpec(FuncName.sort_ascending    , true , 1,  2) );
            _funcList.put( "$sort_descending"   , new FuncSpec(FuncName.sort_descending   , true , 1,  2) );
            _funcList.put( "$map_new"           , new FuncSpec(FuncName.map_new           , true , 0,  0) );
            _funcList.put( "$map_new_from"      , new FuncSpec(FuncName.map_new_from      , true , 1,  2) );
            _funcList.put( "$map_delete"        , new FuncSpec(FuncName.map_delete        , false, 1,  0) );
            _funcList.put( "$map_clear"         , new FuncSpec(FuncName.map_clear         , false, 1,  0) );
            _funcList.put( "$map_put"           , new FuncSpec(FuncName.map_put           , false, 3,  1) );
            _funcList.put( "$map_add"           , new FuncSpec(FuncName.map_add           , false, 3,  1) );
            _funcList.put( "$map_remove"        , new FuncSpec(FuncName.map_remove        , false, 2,  0) );
            _funcList.put( "$map_get"           , new FuncSpec(FuncName.map_get           , true , 2,  0) );
            _funcList.put( "$map_keys"          , new FuncSpec(FuncName.map_keys          , true , 1,  0) );
            _funcList.put( "$map_num_keys"      , new FuncSpec(FuncName.map_num_keys      , true , 1,  0) );
            _funcList.put( "$map_num_vals"      , new FuncSpec(FuncName.map_num_vals      , true , 2,  0) );
            _funcList.put( "$map_to_edata"      , new FuncSpec(FuncName.map_to_edata      , true , 1,  0) );
            _funcList.put( "$map_from_edata"    , new FuncSpec(FuncName.map_from_edata    , true , 1,  0) );
            _funcList.put( "$map_valid_handle"  , new FuncSpec(FuncName.map_valid_handle  , true , 1,  0) );
            _funcList.put( "$nmap_from_json"    , new FuncSpec(FuncName.nmap_from_json    , true , 1,  0) );
            _funcList.put( "$nmap_delete"       , new FuncSpec(FuncName.nmap_delete       , false, 1,  0) );
            _funcList.put( "$stk_new"           , new FuncSpec(FuncName.stk_new           , true , 0,  0) );
            _funcList.put( "$stk_delete"        , new FuncSpec(FuncName.stk_delete        , false, 1,  0) );
            _funcList.put( "$stk_clear"         , new FuncSpec(FuncName.stk_clear         , false, 1,  0) );
            _funcList.put( "$stk_push"          , new FuncSpec(FuncName.stk_push          , false, 2,  0) );
            _funcList.put( "$stk_peek"          , new FuncSpec(FuncName.stk_peek          , true , 1,  0) );
            _funcList.put( "$stk_pop"           , new FuncSpec(FuncName.stk_pop           , true , 1,  0) );
            _funcList.put( "$stk_num_elems"     , new FuncSpec(FuncName.stk_num_elems     , true , 1,  0) );
            _funcList.put( "$stk_to_edata"      , new FuncSpec(FuncName.stk_to_edata      , true , 1,  0) );
            _funcList.put( "$stk_from_edata"    , new FuncSpec(FuncName.stk_from_edata    , true , 1,  0) );
            _funcList.put( "$stk_valid_handle"  , new FuncSpec(FuncName.stk_valid_handle  , true , 1,  0) );
            _funcList.put( "$bse_new"           , new FuncSpec(FuncName.bse_new           , true , 0,  0) );
            _funcList.put( "$bse_delete"        , new FuncSpec(FuncName.bse_delete        , false, 1,  0) );
            _funcList.put( "$bse_size"          , new FuncSpec(FuncName.bse_size          , true , 1,  0) );
            _funcList.put( "$bse_cursor"        , new FuncSpec(FuncName.bse_cursor        , true , 1,  0) );
            _funcList.put( "$bse_seek_abs"      , new FuncSpec(FuncName.bse_seek_abs      , false, 2,  0) );
            _funcList.put( "$bse_seek_beg"      , new FuncSpec(FuncName.bse_seek_beg      , false, 2,  0) );
            _funcList.put( "$bse_seek_end"      , new FuncSpec(FuncName.bse_seek_end      , false, 2,  0) );
            _funcList.put( "$bse_seek_cur"      , new FuncSpec(FuncName.bse_seek_cur      , false, 2,  0) );
            _funcList.put( "$bse_truncate"      , new FuncSpec(FuncName.bse_truncate      , false, 1,  0) );
            _funcList.put( "$bse_save_file"     , new FuncSpec(FuncName.bse_save_file     , false, 2,  0) );
            _funcList.put( "$bse_load_file"     , new FuncSpec(FuncName.bse_load_file     , false, 2,  0) );
            _funcList.put( "$bse_save_base64str", new FuncSpec(FuncName.bse_save_base64str, true , 1,  0) );
            _funcList.put( "$bse_load_base64str", new FuncSpec(FuncName.bse_load_base64str, false, 2,  0) );
            _funcList.put( "$bse_set_be"        , new FuncSpec(FuncName.bse_set_be        , false, 1,  0) );
            _funcList.put( "$bse_set_le"        , new FuncSpec(FuncName.bse_set_le        , false, 1,  0) );
            _funcList.put( "$bse_wr_byte"       , new FuncSpec(FuncName.bse_wr_byte       , false, 2,  0) );
            _funcList.put( "$bse_rd_byte"       , new FuncSpec(FuncName.bse_rd_byte       , true , 1,  0) );
            _funcList.put( "$bse_wr_uint08"     , new FuncSpec(FuncName.bse_wr_uint08     , false, 2,  0) );
            _funcList.put( "$bse_wr_sint08"     , new FuncSpec(FuncName.bse_wr_sint08     , false, 2,  0) );
            _funcList.put( "$bse_rd_uint08"     , new FuncSpec(FuncName.bse_rd_uint08     , true , 1,  0) );
            _funcList.put( "$bse_rd_sint08"     , new FuncSpec(FuncName.bse_rd_sint08     , true , 1,  0) );
            _funcList.put( "$bse_wr_uint16"     , new FuncSpec(FuncName.bse_wr_uint16     , false, 2,  0) );
            _funcList.put( "$bse_wr_sint16"     , new FuncSpec(FuncName.bse_wr_sint16     , false, 2,  0) );
            _funcList.put( "$bse_rd_uint16"     , new FuncSpec(FuncName.bse_rd_uint16     , true , 1,  0) );
            _funcList.put( "$bse_rd_sint16"     , new FuncSpec(FuncName.bse_rd_sint16     , true , 1,  0) );
            _funcList.put( "$bse_wr_uint32"     , new FuncSpec(FuncName.bse_wr_uint32     , false, 2,  0) );
            _funcList.put( "$bse_wr_sint32"     , new FuncSpec(FuncName.bse_wr_sint32     , false, 2,  0) );
            _funcList.put( "$bse_rd_uint32"     , new FuncSpec(FuncName.bse_rd_uint32     , true , 1,  0) );
            _funcList.put( "$bse_rd_sint32"     , new FuncSpec(FuncName.bse_rd_sint32     , true , 1,  0) );
            _funcList.put( "$bse_wr_uint64"     , new FuncSpec(FuncName.bse_wr_uint64     , false, 2,  0) );
            _funcList.put( "$bse_wr_sint64"     , new FuncSpec(FuncName.bse_wr_sint64     , false, 2,  0) );
            _funcList.put( "$bse_rd_uint64"     , new FuncSpec(FuncName.bse_rd_uint64     , true , 1,  0) );
            _funcList.put( "$bse_rd_sint64"     , new FuncSpec(FuncName.bse_rd_sint64     , true , 1,  0) );
            _funcList.put( "$bse_wr_flt32"      , new FuncSpec(FuncName.bse_wr_flt32      , false, 2,  0) );
            _funcList.put( "$bse_rd_flt32"      , new FuncSpec(FuncName.bse_rd_flt32      , true , 1,  0) );
            _funcList.put( "$bse_wr_dbl64"      , new FuncSpec(FuncName.bse_wr_dbl64      , false, 2,  0) );
            _funcList.put( "$bse_rd_dbl64"      , new FuncSpec(FuncName.bse_rd_dbl64      , true , 1,  0) );
            _funcList.put( "$bse_wr_utf8"       , new FuncSpec(FuncName.bse_wr_utf8       , false, 2,  1) );
            _funcList.put( "$bse_rd_utf8"       , new FuncSpec(FuncName.bse_rd_utf8       , true , 1,  1) );
            _funcList.put( "$bse_wr_utf16"      , new FuncSpec(FuncName.bse_wr_utf16      , false, 2,  1) );
            _funcList.put( "$bse_rd_utf16"      , new FuncSpec(FuncName.bse_rd_utf16      , true , 1,  1) );
            _funcList.put( "$bse_wr_utf32"      , new FuncSpec(FuncName.bse_wr_utf32      , false, 2,  1) );
            _funcList.put( "$bse_rd_utf32"      , new FuncSpec(FuncName.bse_rd_utf32      , true , 1,  1) );
            _funcList.put( "$bse_range_clr"     , new FuncSpec(FuncName.bse_range_clr     , false, 2,  0) );
            _funcList.put( "$bse_range_cpy"     , new FuncSpec(FuncName.bse_range_cpy     , true , 2,  0) );
            _funcList.put( "$bse_range_cut"     , new FuncSpec(FuncName.bse_range_cut     , true , 2,  0) );
            _funcList.put( "$bse_range_pst"     , new FuncSpec(FuncName.bse_range_pst     , false, 2,  0) );
            _funcList.put( "$bse_range_ovr"     , new FuncSpec(FuncName.bse_range_ovr     , false, 2,  0) );
            _funcList.put( "$bse_valid_handle"  , new FuncSpec(FuncName.bse_valid_handle  , true , 1,  0) );
            _funcList.put( "$fwc_new"           , new FuncSpec(FuncName.fwc_new           , true , 0,  0) );
            _funcList.put( "$fwc_delete"        , new FuncSpec(FuncName.fwc_delete        , false, 1,  0) );
            _funcList.put( "$fwc_load_raw_bin"  , new FuncSpec(FuncName.fwc_load_raw_bin  , true , 1,  1) );
            _funcList.put( "$fwc_load_elf_bin"  , new FuncSpec(FuncName.fwc_load_elf_bin  , true , 1,  3) );
            _funcList.put( "$fwc_load_intel_hex", new FuncSpec(FuncName.fwc_load_intel_hex, true , 1,  1) );
            _funcList.put( "$fwc_load_moto_srec", new FuncSpec(FuncName.fwc_load_moto_srec, true , 1,  1) );
            _funcList.put( "$fwc_load_tektx_hex", new FuncSpec(FuncName.fwc_load_tektx_hex, true , 1,  1) );
            _funcList.put( "$fwc_load_mos_tech" , new FuncSpec(FuncName.fwc_load_mos_tech , true , 1,  1) );
            _funcList.put( "$fwc_load_titxt_hex", new FuncSpec(FuncName.fwc_load_titxt_hex, true , 1,  1) );
            _funcList.put( "$fwc_load_ascii_hex", new FuncSpec(FuncName.fwc_load_ascii_hex, true , 1,  1) );
            _funcList.put( "$fwc_load_vl_vmem"  , new FuncSpec(FuncName.fwc_load_vl_vmem  , true , 1,  1) );
            _funcList.put( "$fwc_save_raw_bin"  , new FuncSpec(FuncName.fwc_save_raw_bin  , false, 2,  1) );
            _funcList.put( "$fwc_save_intel_hex", new FuncSpec(FuncName.fwc_save_intel_hex, false, 2,  0) );
            _funcList.put( "$fwc_save_moto_srec", new FuncSpec(FuncName.fwc_save_moto_srec, false, 2,  0) );
            _funcList.put( "$fwc_save_tektx_hex", new FuncSpec(FuncName.fwc_save_tektx_hex, false, 2,  0) );
            _funcList.put( "$fwc_save_mos_tech" , new FuncSpec(FuncName.fwc_save_mos_tech , false, 2,  0) );
            _funcList.put( "$fwc_save_titxt_hex", new FuncSpec(FuncName.fwc_save_titxt_hex, false, 2,  0) );
            _funcList.put( "$fwc_delete"        , new FuncSpec(FuncName.fwc_delete        , false, 1,  0) );
            _funcList.put( "$fwc_clear"         , new FuncSpec(FuncName.fwc_clear         , false, 1,  0) );
            _funcList.put( "$fwc_min_start_addr", new FuncSpec(FuncName.fwc_min_start_addr, true , 1,  0) );
            _funcList.put( "$fwc_max_final_addr", new FuncSpec(FuncName.fwc_max_final_addr, true , 1,  0) );
            _funcList.put( "$fwc_equals"        , new FuncSpec(FuncName.fwc_equals        , true , 2,  1) );
            _funcList.put( "$fwc_compose"       , new FuncSpec(FuncName.fwc_compose       , true , 2,  1) );
            _funcList.put( "$cmp"               , new FuncSpec(FuncName.cmp               , true , 3,  0) );
            _funcList.put( "$not"               , new FuncSpec(FuncName.not               , true , 1,  0) );
            _funcList.put( "$and"               , new FuncSpec(FuncName.and               , true , 1, -1) );
            _funcList.put( "$or"                , new FuncSpec(FuncName.or                , true , 1, -1) );
            _funcList.put( "$csel"              , new FuncSpec(FuncName.csel              , true , 3,  0) );
            _funcList.put( "$printf"            , new FuncSpec(FuncName.printf            , false, 1, -1) );
            _funcList.put( "$sprintf"           , new FuncSpec(FuncName.sprintf           , true , 1, -1) );
            _funcList.put( "$vprintf"           , new FuncSpec(FuncName.vprintf           , false, 2,  0) );
            _funcList.put( "$vsprintf"          , new FuncSpec(FuncName.vsprintf          , true , 2,  0) );
            _funcList.put( "$read_line"         , new FuncSpec(FuncName.read_line         , true , 0,  0) );
            _funcList.put( "$read_pswd"         , new FuncSpec(FuncName.read_pswd         , true , 0,  0) );
            _funcList.put( "$resolve_ip"        , new FuncSpec(FuncName.resolve_ip        , true , 1,  0) );
            _funcList.put( "$http_head"         , new FuncSpec(FuncName.http_head         , true , 1,  2) );
            _funcList.put( "$http_get"          , new FuncSpec(FuncName.http_get          , true , 1,  2) );
            _funcList.put( "$http_post"         , new FuncSpec(FuncName.http_post         , true , 2,  2) );
            _funcList.put( "$http_download"     , new FuncSpec(FuncName.http_download     , true , 1,  4) );
            _funcList.put( "$http_set_auth"     , new FuncSpec(FuncName.http_set_auth     , false, 2,  0) );
            _funcList.put( "$http_clr_auth"     , new FuncSpec(FuncName.http_clr_auth     , false, 0,  0) );
            _funcList.put( "$ssl_trust_all"     , new FuncSpec(FuncName.ssl_trust_all     , false, 1,  0) );
            _funcList.put( "$gh_get_tags"       , new FuncSpec(FuncName.gh_get_tags       , true , 1,  0) );
            _funcList.put( "$gh_get_assets"     , new FuncSpec(FuncName.gh_get_assets     , true , 1,  0) );
            _funcList.put( "$ls_serial_ports"   , new FuncSpec(FuncName.ls_serial_ports   , true , 0,  0) );
          //_funcList.put( "$set_baudrate"      , new FuncSpec(FuncName.set_baudrate      , true , 2,  0) );
            _funcList.put( "$mcu_reset"         , new FuncSpec(FuncName.mcu_reset         , false, 1,  1) );
            _funcList.put( "$mcu_bootloader"    , new FuncSpec(FuncName.mcu_bootloader    , false, 1,  1) );
            _funcList.put( "$mcu_prog_cfg_json" , new FuncSpec(FuncName.mcu_prog_cfg_json , true , 1,  0) );
            _funcList.put( "$mcu_prog_cfg_ini"  , new FuncSpec(FuncName.mcu_prog_cfg_ini  , true , 1,  0) );
            _funcList.put( "$mcu_prog_exec"     , new FuncSpec(FuncName.mcu_prog_exec     , true , 6,  0) );
            _funcList.put( "$serial_console"    , new FuncSpec(FuncName.serial_console    , true , 2,  2) );
            _funcList.put( "$tcp_serial_console", new FuncSpec(FuncName.tcp_serial_console, true , 2,  2) );
            _funcList.put( "$serial_plotter"    , new FuncSpec(FuncName.serial_plotter    , false, 2,  2) );
            _funcList.put( "$tcp_serial_plotter", new FuncSpec(FuncName.tcp_serial_plotter, false, 2,  2) );
            _funcList.put( "$xmlframe_file"     , new FuncSpec(FuncName.xmlframe_file     , true , 1,  0) );
            _funcList.put( "$xmlframe_string"   , new FuncSpec(FuncName.xmlframe_string   , true , 1,  0) );
            _funcList.put( "$xml_escape"        , new FuncSpec(FuncName.xml_escape        , true , 1,  0) );
            _funcList.put( "$aboard_from_file"  , new FuncSpec(FuncName.aboard_from_file  , true , 1,  0) );
            _funcList.put( "$aboard_selector"   , new FuncSpec(FuncName.aboard_selector   , true , 1,  1) );
            _funcList.put( "$aboard_getselconf" , new FuncSpec(FuncName.aboard_getselconf , true , 1,  0) );
            _funcList.put( "$esp_st_decoder"    , new FuncSpec(FuncName.esp_st_decoder    , true , 3,  0) );
            _funcList.put( "$gdl_cpp_include"   , new FuncSpec(FuncName.gdl_cpp_include   , false, 4,  0) );
            _funcList.put( "$gdl_cpp_module"    , new FuncSpec(FuncName.gdl_cpp_module    , false, 3,  0) );
            _funcList.put( "$gdl_java_import"   , new FuncSpec(FuncName.gdl_java_import   , false, 3,  0) );
            _funcList.put( "$pp_java_scf"       , new FuncSpec(FuncName.pp_java_scf       , false, 4,  0) );

            _funcList.put( "$__dummy_dep_1__"   , new FuncSpec(FuncName.__dummy_dep_1__   , false, 0,  0, FuncName.__END__        ) );
            _funcList.put( "$__dummy_dep_2__"   , new FuncSpec(FuncName.__dummy_dep_2__   , false, 0,  0, FuncName.__dummy_rep_2__) );
            _funcList.put( "$__dummy_rep_2__"   , new FuncSpec(FuncName.__dummy_rep_2__   , false, 0,  0                          ) );
        }

        // The function specification to be returned
        FuncSpec fs = null;

        // Check if the function call begins with the '-' character
        if( funcName.charAt(0) == '-' ) {
            // The function '$call' and '$exec' cannot be preceded  by the '-' character
            final String fn = funcName.substring( 1, funcName.length() );
            if( fn.equals("$call") || fn.equals("$exec") ) return null;
            // Search the function specification
            fs = _funcList.get(fn);
            // Modify the function specification as needed
            fs = (fs != null) ? ( new FuncSpec(fs, true) ) : null;
        }
        // Normal function call
        else {
            // Search the function specification
            fs = _funcList.get(funcName);
        }

        // Check if the function is deprecated
        if(fs != null && fs.fnDfNm != null) {
            if(fs.fnDfNm == FuncName.__END__) {
                SysUtil.printfSimpleWarning( Texts.WMsg_DeprecatedBIFunc0, fs.fnName.name() );
            }
            else {
                SysUtil.printfSimpleWarning( Texts.WMsg_DeprecatedBIFunc1, fs.fnName.name(), fs.fnDfNm.name() );
            }
        }

        // Return the function specification
        return fs;
    }

    public static boolean isFunctionName(final String str)
    { return _pmFuncName.matcher(str).matches(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum SVarName {
        __jxm_ver_major__  (true                       ), // Special-variable-shortcut - read only constant - will never change after compilation
        __jxm_ver_minor__  (true                       ), // Special-variable-shortcut - read only constant - will never change after compilation
        __jxm_ver_patch__  (true                       ), // Special-variable-shortcut - read only constant - will never change after compilation
        __jxm_ver_value__  (true                       ), // Special-variable-shortcut - read only constant - will never change after compilation
        __jxm_ver_devel__  (true                       ), // Special-variable-shortcut - read only constant - will never change after compilation

        __os_name__        (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_name_actual__ (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_windows__     (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_posix__       (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_linux__       (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_bsd__         (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_mac__         (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_cygwin__      (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_mingw__       (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_msys__        (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_posix_compat__(                           ), // Special-variable-shortcut - read only constant - system dependent

        __os_arch__        (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_bit_count__   (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_32bit__       (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_64bit__       (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_be__          (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_le__          (                           ), // Special-variable-shortcut - read only constant - system dependent

        __os_dsep_char__   (                           ), // Special-variable-shortcut - read only constant - system dependent
        __os_psep_char__   (                           ), // Special-variable-shortcut - read only constant - system dependent

        __re_all__         (                           ), // Special-variable-shortcut - read only constant - will never change

        __include_paths__  (                           ), // Special-variable-shortcut - read and write
        __class_paths__    (                           ), // Special-variable-shortcut - read and write

        cmdecho            (          "$[cmdecho]"     ), // Special-variable          - read only          - automatic value
        cmdstreaming       (          "$[cmdstreaming]"), // Special-variable          - read only          - automatic value
        lserr              (          "$[lserr]"       ), // Special-variable          - read only          - automatic value
        function           (          "$[function]"    ), // Special-variable          - read only          - automatic value
        jxmakefile         (          "$[jxmakefile]"  ), // Special-variable          - read only          - automatic value

        cmdtargets         (          "$[cmdtargets]"  ), // Special-variable          - read only          - automatic value

        usn                (          "$[usn]"         ), // Special-variable          - read only          - automatic value

        target             (          "$[target]"      ), // Special-variable          - read only          - automatic value
        preqCount          (          "$[preq^]"       ), // Special-variable          - read only          - automatic value
        preqAll            (          "$[preq*]"       ), // Special-variable          - read only          - automatic value
        preqMoreRecent     (          "$[preq?]"       ), // Special-variable          - read only          - automatic value
        preqN              ("preq"  /*"$[preqN]" */    ), // Special-variable          - read only          - automatic value
        preqV              ("preq:" /*"$[preq:V]"*/    ), // Special-variable          - read only          - automatic value

        preqXManual        (          "$[preq+]"       ), // Special-variable          - read only          - automatic value
        preqXAutoDet       (          "$[preq%]"       ), // Special-variable          - read only          - automatic value
        preqEffective      (          "$[preq~]"       ), // Special-variable          - read only          - automatic value

        excode             (          "$[excode]"      ), // Special-variable          - read only          - automatic value
        stdout             (          "$[stdout]"      ), // Special-variable          - read only          - automatic value
        stderr             (          "$[stderr]"      ), // Special-variable          - read only          - automatic value

        // Selected color theme
        __c_use_dark__     (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_use_light__    (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent

        // ANSI escape code
        __c_clrscr__       (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_black__        (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_dgray__        (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_lgray__        (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_red__          (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_green__        (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_yellow__       (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_blue__         (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_magenta__      (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_cyan__         (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_white__        (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent
        __c_reset__        (                           ), // Special-variable-shortcut - read only constant - command-line-option dependent

        // Special tester
        __dummy_vdep_1__("$[__dummy_vdep_1__]"),
        __dummy_vdep_2__("$[__dummy_vdep_2__]"),
        __dummy_vrep_2__("$[__dummy_vrep_2__]"),

        __dummy_sdep_1__(                     ),
        __dummy_sdep_2__(                     ),
        __dummy_srep_2__(                     ),

        // End marker
        __END__()

        ;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final String  _svName;
        private final boolean _ctConst; // A flag that indicates if it is a compile-time constant

        private SVarName(                     ) { _svName = null;   _ctConst = false  ; }
        private SVarName(final String  svName ) { _svName = svName; _ctConst = false  ; }
        private SVarName(final boolean ctConst) { _svName = null;   _ctConst = ctConst; }

        public String svName(                         ) { return (_svName != null ) ? _svName : name();            }
        public String svName(final int    autoIndex   ) { return "$[" + _svName + String.valueOf(autoIndex) + "]"; }
        public String svName(final String indexVarName) { return "$[" + _svName + trmRVarName(indexVarName) + "]"; }

        public boolean isCompileTimeConstant()
        { return _ctConst; }
    }

    public static class SVarSpec {
        public final SVarName svarName;  // The special-variable or special-variable-shortcut name (in enum)
        public final SVarName svarDfNm;  // A non null value indicates that this variable has been deprecated by the one specified here

        public final String   constVal;  // The constant value of the variable
                                         //     # A non-null value means the variable is static and read only (if 'autoVal' is true, it is a read only index variable name)
                                         //     # A     null value means the variable is stored in ExecState like other variables and is writable
        public final boolean  autoVal;   // A flag to indicate if the value is automatic; for '$[...]'
        public final int      autoIndex; // Index for '$[preqN]'

        public SVarSpec(final SVarName svarName_, final String constVal_)
        { this(svarName_, constVal_, null); }

        public SVarSpec(final SVarName svarName_, final String constVal_, final SVarName svarDfNm_)
        {
            svarName  = svarName_;
            svarDfNm  = svarDfNm_;
            constVal  = constVal_;
            autoVal   = false;
            autoIndex = -1;
        }

        public SVarSpec(final SVarName svarName_, final int autoIndex_, final String autoVar_)
        { this(svarName_, autoIndex_, autoVar_, null); }

        public SVarSpec(final SVarName svarName_, final int autoIndex_, final String autoVar_, final SVarName svarDfNm_)
        {
            svarName  = svarName_;
            svarDfNm  = svarDfNm_;
            constVal  = autoVar_;
            autoVal   = true;
            autoIndex = autoIndex_;
        }

        public boolean equals(final SVarSpec refSVarSpec)
        {
            if( svarName  != refSVarSpec.svarName      ) return false;
            if( svarDfNm  != refSVarSpec.svarDfNm      ) return false;
            if( !constVal.equals(refSVarSpec.constVal) ) return false;
            if( autoVal   != refSVarSpec.autoVal       ) return false;
            if( autoIndex != refSVarSpec.autoIndex     ) return false;
            return true;
        }

        public boolean isEvaluated()
        { return autoVal || (constVal == null); }

        public boolean isWritableSCut()
        { return (constVal == null) && (!autoVal); }

        public String getStrName()
        { return svarName.name(); }
    }

    private static final Pattern                   _pmPreqN  = Pattern.compile("\\$\\[preq([1-9][0-9]*)\\]");
    private static final Pattern                   _pmPreqV  = Pattern.compile("\\$\\[preq:([_a-zA-Z][_a-zA-Z0-9]*)\\]");
    private static final HashMap<String, SVarSpec> _svarList = new HashMap<>();

    // https://stackoverflow.com/a/14652763
    private static final Pattern                   _pmANSIEC = Pattern.compile("\033\\[[;\\d]*[ -/]*[@-~]");

    // https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_(Control_Sequence_Introducer)_sequences
    private static final String   _AC_clrscr    = "\033[2J\033[1;1H";

    // https://en.wikipedia.org/wiki/ANSI_escape_code#3-bit_and_4-bit
    private static       int      _AC_SelCIdx   = 0;
    private static final String[] _AC_c_black   = { "\033[0;30;40m", "\033[0;97;107m" };
    private static final String[] _AC_c_dgray   = { "\033[0;90;40m", "\033[0;37;107m" };
    private static final String[] _AC_c_lgray   = { "\033[0;37;40m", "\033[0;90;107m" };
    private static final String[] _AC_c_red     = { "\033[0;91;40m", "\033[0;31;107m" };
    private static final String[] _AC_c_green   = { "\033[0;92;40m", "\033[0;32;107m" };
    private static final String[] _AC_c_yellow  = { "\033[0;93;40m", "\033[0;33;107m" };
    private static final String[] _AC_c_blue    = { "\033[0;94;40m", "\033[0;34;107m" };
    private static final String[] _AC_c_magenta = { "\033[0;95;40m", "\033[0;35;107m" };
    private static final String[] _AC_c_cyan    = { "\033[0;96;40m", "\033[0;36;107m" };
    private static final String[] _AC_c_white   = { "\033[0;97;40m", "\033[0;30;107m" };
    private static final String   _AC_c_reset   =   "\033[0m";

    private static       boolean  _disANSIEscCd = false;

    public  static       String AC_clrscr   () { return _disANSIEscCd ? "" : _AC_clrscr;                 }
    public  static       String AC_c_black  () { return _disANSIEscCd ? "" : _AC_c_black  [_AC_SelCIdx]; }
    public  static       String AC_c_dgray  () { return _disANSIEscCd ? "" : _AC_c_dgray  [_AC_SelCIdx]; }
    public  static       String AC_c_lgray  () { return _disANSIEscCd ? "" : _AC_c_lgray  [_AC_SelCIdx]; }
    public  static       String AC_c_red    () { return _disANSIEscCd ? "" : _AC_c_red    [_AC_SelCIdx]; }
    public  static       String AC_c_green  () { return _disANSIEscCd ? "" : _AC_c_green  [_AC_SelCIdx]; }
    public  static       String AC_c_yellow () { return _disANSIEscCd ? "" : _AC_c_yellow [_AC_SelCIdx]; }
    public  static       String AC_c_blue   () { return _disANSIEscCd ? "" : _AC_c_blue   [_AC_SelCIdx]; }
    public  static       String AC_c_magenta() { return _disANSIEscCd ? "" : _AC_c_magenta[_AC_SelCIdx]; }
    public  static       String AC_c_cyan   () { return _disANSIEscCd ? "" : _AC_c_cyan   [_AC_SelCIdx]; }
    public  static       String AC_c_white  () { return _disANSIEscCd ? "" : _AC_c_white  [_AC_SelCIdx]; }
    public  static       String AC_c_reset  () { return _disANSIEscCd ? "" : _AC_c_reset;                }

    public static void initColorSelection(final boolean useLightColorTheme)
    { _AC_SelCIdx = useLightColorTheme ? 1 : 0; }

    public static boolean useLightColorTheme()
    { return _AC_SelCIdx == 1; }

    public static void setDisableANSIEscapeCode(final boolean disable)
    { _disANSIEscCd = disable; }

    public static String stripANSIEscapeCodeAsNeeded(final String str)
    {
        if(!_disANSIEscCd) return str;

        return _pmANSIEC.matcher(str).replaceAll("");
    }

    private static void _initSVarList()
    {
        // Generate the list
        _svarList.put( "__jxm_ver_major__"  , new SVarSpec( SVarName.__jxm_ver_major__  ,     String.valueOf( SysUtil.jxmVerMajor(    )                 ) ) );
        _svarList.put( "__jxm_ver_minor__"  , new SVarSpec( SVarName.__jxm_ver_minor__  ,     String.valueOf( SysUtil.jxmVerMinor    ()                 ) ) );
        _svarList.put( "__jxm_ver_patch__"  , new SVarSpec( SVarName.__jxm_ver_patch__  ,     String.valueOf( SysUtil.jxmVerPatch    ()                 ) ) );
        _svarList.put( "__jxm_ver_value__"  , new SVarSpec( SVarName.__jxm_ver_value__  ,     String.valueOf( SysUtil.jxmVerValue    ()                 ) ) );
        _svarList.put( "__jxm_ver_devel__"  , new SVarSpec( SVarName.__jxm_ver_devel__  ,                     SysUtil.jxmVerDevel    ()                   ) );

        _svarList.put( "__os_name__"        , new SVarSpec( SVarName.__os_name__        ,                     SysUtil.osName         ()                   ) );
        _svarList.put( "__os_name_actual__" , new SVarSpec( SVarName.__os_name_actual__ ,                     SysUtil.osNameActual   ()                   ) );

        _svarList.put( "__os_windows__"     , new SVarSpec( SVarName.__os_windows__     ,                   ( SysUtil.osIsWindows    () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_posix__"       , new SVarSpec( SVarName.__os_posix__       ,                   ( SysUtil.osIsPOSIX      () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_linux__"       , new SVarSpec( SVarName.__os_linux__       ,                   ( SysUtil.osIsLinux      () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_bsd__"         , new SVarSpec( SVarName.__os_bsd__         ,                   ( SysUtil.osIsBSD        () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_mac__"         , new SVarSpec( SVarName.__os_mac__         ,                   ( SysUtil.osIsMac        () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_cygwin__"      , new SVarSpec( SVarName.__os_cygwin__      ,                   ( SysUtil.osIsCygwin     () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_mingw__"       , new SVarSpec( SVarName.__os_mingw__       ,                   ( SysUtil.osIsMinGW      () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_msys__"        , new SVarSpec( SVarName.__os_msys__        ,                   ( SysUtil.osIsMSys       () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_posix_compat__", new SVarSpec( SVarName.__os_posix_compat__,                   ( SysUtil.osIsPOSIXCompat() ? Str_T : Str_F ) ) );

        _svarList.put( "__os_arch__"        , new SVarSpec( SVarName.__os_arch__        ,                     SysUtil.osArch         ()                   ) );
        _svarList.put( "__os_bit_count__"   , new SVarSpec( SVarName.__os_bit_count__   ,     String.valueOf( SysUtil.osBitCount     ()                 ) ) );
        _svarList.put( "__os_32bit__"       , new SVarSpec( SVarName.__os_32bit__       ,                   ( SysUtil.osIs32Bit      () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_64bit__"       , new SVarSpec( SVarName.__os_64bit__       ,                   ( SysUtil.osIs64Bit      () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_be__"          , new SVarSpec( SVarName.__os_be__          ,                   ( SysUtil.osIsBE         () ? Str_T : Str_F ) ) );
        _svarList.put( "__os_le__"          , new SVarSpec( SVarName.__os_le__          ,                   ( SysUtil.osIsLE         () ? Str_T : Str_F ) ) );

        _svarList.put( "__os_dsep_char__"   , new SVarSpec( SVarName.__os_dsep_char__   ,     File.separator                                              ) );
        _svarList.put( "__os_psep_char__"   , new SVarSpec( SVarName.__os_psep_char__   ,     File.pathSeparator                                          ) );

        _svarList.put( "__re_all__"         , new SVarSpec( SVarName.__re_all__         ,     ".+"                                                        ) );

        _svarList.put( "__include_paths__"  , new SVarSpec( SVarName.__include_paths__  ,     null                                                        ) );
        _svarList.put( "__class_paths__"    , new SVarSpec( SVarName.__class_paths__    ,     null                                                        ) );

        _svarList.put( "$[cmdecho]"         , new SVarSpec( SVarName.cmdecho            , -1, null                                                        ) );
        _svarList.put( "$[cmdstreaming]"    , new SVarSpec( SVarName.cmdstreaming       , -1, null                                                        ) );
        _svarList.put( "$[lserr]"           , new SVarSpec( SVarName.lserr              , -1, null                                                        ) );
        _svarList.put( "$[jxmakefile]"      , new SVarSpec( SVarName.jxmakefile         , -1, null                                                        ) );
        _svarList.put( "$[function]"        , new SVarSpec( SVarName.function           , -1, null                                                        ) );

        _svarList.put( "$[cmdtargets]"      , new SVarSpec( SVarName.cmdtargets         , -1, null                                                        ) );

        _svarList.put( "$[usn]"             , new SVarSpec( SVarName.usn                , -1, null                                                        ) );

        _svarList.put( "$[target]"          , new SVarSpec( SVarName.target             , -1, null                                                        ) );
        _svarList.put( "$[preq^]"           , new SVarSpec( SVarName.preqCount          , -1, null                                                        ) );
        _svarList.put( "$[preq*]"           , new SVarSpec( SVarName.preqAll            , -1, null                                                        ) );
        _svarList.put( "$[preq?]"           , new SVarSpec( SVarName.preqMoreRecent     , -1, null                                                        ) );
        _svarList.put( "$[preq+]"           , new SVarSpec( SVarName.preqXManual        , -1, null                                                        ) );
        _svarList.put( "$[preq%]"           , new SVarSpec( SVarName.preqXAutoDet       , -1, null                                                        ) );
        _svarList.put( "$[preq~]"           , new SVarSpec( SVarName.preqEffective      , -1, null                                                        ) );

        _svarList.put( "$[excode]"          , new SVarSpec( SVarName.excode             , -1, null                                                        ) );
        _svarList.put( "$[stderr]"          , new SVarSpec( SVarName.stderr             , -1, null                                                        ) );
        _svarList.put( "$[stdout]"          , new SVarSpec( SVarName.stdout             , -1, null                                                        ) );

        _svarList.put( "__c_use_dark__"     , new SVarSpec( SVarName.__c_use_dark__     ,                   ( useLightColorTheme     () ? Str_F : Str_T ) ) );
        _svarList.put( "__c_use_light__"    , new SVarSpec( SVarName.__c_use_light__    ,                   ( useLightColorTheme     () ? Str_T : Str_F ) ) );

        _svarList.put( "__c_clrscr__"       , new SVarSpec( SVarName.__c_clrscr__       ,     AC_clrscr   ()                                              ) );
        _svarList.put( "__c_black__"        , new SVarSpec( SVarName.__c_black__        ,     AC_c_black  ()                                              ) );
        _svarList.put( "__c_dgray__"        , new SVarSpec( SVarName.__c_dgray__        ,     AC_c_dgray  ()                                              ) );
        _svarList.put( "__c_lgray__"        , new SVarSpec( SVarName.__c_lgray__        ,     AC_c_lgray  ()                                              ) );
        _svarList.put( "__c_red__"          , new SVarSpec( SVarName.__c_red__          ,     AC_c_red    ()                                              ) );
        _svarList.put( "__c_green__"        , new SVarSpec( SVarName.__c_green__        ,     AC_c_green  ()                                              ) );
        _svarList.put( "__c_yellow__"       , new SVarSpec( SVarName.__c_yellow__       ,     AC_c_yellow ()                                              ) );
        _svarList.put( "__c_blue__"         , new SVarSpec( SVarName.__c_blue__         ,     AC_c_blue   ()                                              ) );
        _svarList.put( "__c_magenta__"      , new SVarSpec( SVarName.__c_magenta__      ,     AC_c_magenta()                                              ) );
        _svarList.put( "__c_cyan__"         , new SVarSpec( SVarName.__c_cyan__         ,     AC_c_cyan   ()                                              ) );
        _svarList.put( "__c_white__"        , new SVarSpec( SVarName.__c_white__        ,     AC_c_white  ()                                              ) );
        _svarList.put( "__c_reset__"        , new SVarSpec( SVarName.__c_reset__        ,     AC_c_reset  ()                                              ) );

        _svarList.put( "$[__dummy_vdep_1__]", new SVarSpec( SVarName.__dummy_vdep_1__   , "DUMMY VDEP1", SVarName.__END__          ) );
        _svarList.put( "$[__dummy_vdep_2__]", new SVarSpec( SVarName.__dummy_vdep_2__   , "DUMMY VDEP2", SVarName.__dummy_vrep_2__ ) );
        _svarList.put( "$[__dummy_vrep_2__]", new SVarSpec( SVarName.__dummy_vrep_2__   , "DUMMY VREP2"                            ) );

        _svarList.put( "__dummy_sdep_1__"   , new SVarSpec( SVarName.__dummy_sdep_1__   , "DUMMY SDEP1", SVarName.__END__          ) );
        _svarList.put( "__dummy_sdep_2__"   , new SVarSpec( SVarName.__dummy_sdep_2__   , "DUMMY SDEP2", SVarName.__dummy_srep_2__ ) );
        _svarList.put( "__dummy_srep_2__"   , new SVarSpec( SVarName.__dummy_srep_2__   , "DUMMY SREP2"                            ) );
    }

    public static SVarSpec getSVarSpec(final String svarName)
    {
        // Initialize the map once and only once
        if( _svarList.isEmpty() ) _initSVarList();

        // Check for '$[preqN]' first
        if(true) {
            final Matcher matcherN = _pmPreqN.matcher(svarName);
            if( matcherN.matches() ) {
                // Create the special-variable specification
                final int      ix = Integer.parseInt( matcherN.group(1) );
                final SVarSpec vs = new SVarSpec(SVarName.preqN, ix, null);
                // Check if the special-variable or is deprecated
                if(vs.svarDfNm != null) {
                    if(vs.svarDfNm  == SVarName.__END__) {
                        SysUtil.printfSimpleWarning( Texts.WMsg_DeprecatedSVar0, vs.svarName.svName(ix) );
                    }
                    else {
                        SysUtil.printfSimpleWarning( Texts.WMsg_DeprecatedSVar1, vs.svarName.svName(ix), vs.svarDfNm.svName(ix) );
                    }
                }
                // Return the special-variable specification
                return vs;
            }
        }

        // Check for '$[preq:V]' next
        if(true) {
            final Matcher matcherV = _pmPreqV.matcher(svarName);
            if( matcherV.matches() ) {
                // Create the special-variable specification
                final String   ix = matcherV.group(1);
                final SVarSpec vs = new SVarSpec( SVarName.preqV, -1, genRVarName(ix) );
                // Check if the special-variable or is deprecated
                if(vs.svarDfNm != null) {
                    if(vs.svarDfNm  == SVarName.__END__) {
                        SysUtil.printfSimpleWarning( Texts.WMsg_DeprecatedSVar0, vs.svarName.svName(ix) );
                    }
                    else {
                        SysUtil.printfSimpleWarning( Texts.WMsg_DeprecatedSVar1, vs.svarName.svName(ix), vs.svarDfNm.svName(ix) );
                    }
                }
                // Return the special-variable specification
                return vs;
            }
        }

        // Search the special-variable or special-variable-shortcut specification
        final SVarSpec vs = _svarList.get(svarName);

        // Check if the special-variable or special-variable-shortcut is deprecated
        if(vs != null && vs.svarDfNm != null) {
            final boolean isSCut = ( vs.svarName.svName().charAt(0) == '_' );
            if(vs.svarDfNm  == SVarName.__END__) {
                SysUtil.printfSimpleWarning( isSCut ? Texts.WMsg_DeprecatedSVarSCut0 : Texts.WMsg_DeprecatedSVar0, vs.svarName.svName() );
            }
            else {
                SysUtil.printfSimpleWarning( isSCut ? Texts.WMsg_DeprecatedSVarSCut1 : Texts.WMsg_DeprecatedSVar1, vs.svarName.svName(), vs.svarDfNm.svName() );
            }
        }

        // Return the special-variable or special-variable-shortcut specification
        return vs;
    }

    public static boolean isWritableSVarSCut(final String svarName)
    {
        // Initialize the map once and only once
        if( _svarList.isEmpty() ) _initSVarList();

        // Get the special-variable or special-variable-shortcut specification
        final SVarSpec svarSpec = _svarList.get(svarName);
        if(svarSpec == null) return false;

        // Check if it is a writable special-variable-shortcut
        return svarSpec.isWritableSCut();
    }

    public static boolean isPreTargetExecSVarSCut(final String svarName)
    { return svarName.equals("__include_paths__") || svarName.equals("__class_paths__"); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmTTVar         = Pattern.compile("^\\$\\{__[rt]var__:([a-f0-9]+:)?\\d+\\}$");

    public  static final String  RVar_NamePrefix  = "__rvar__:"; // Transient variable
    public  static final String  TVar_NamePrefix  = "__tvar__:"; // Temporary variable

    public  static final String  SVar_NamePrefix  = "__svar__:";
    public  static final String  SVar_Echo        = SVar_NamePrefix + "echo";
    public  static final String  SVar_Echoln      = SVar_NamePrefix + "echoln";

    public  static final String  Str_NullArgument = "\0?\0";

    public  static final String  Str_F            = "0";
    public  static final String  Str_T            = "1";

    public static boolean isLegalVarAssignment(final String varName, final String tRX1)
    {
        if( !tRX1.isEmpty() ) return false;

        final SVarSpec svarSpec = getSVarSpec(varName);
        if(svarSpec == null) return true;

        return (svarSpec.constVal == null) && (!svarSpec.autoVal);
    }

    public static boolean isValidConditionValue(final TokenReader.Token token, final boolean canEmpty)
    {
        // Initialize the map once and only once
        if( _svarList.isEmpty() ) _initSVarList();

        // Check if the token is a pure-static-string constant
        if(token.fPSP) return true;

        // Get the string
        final String str = token.tStr;

        // Check if the string is a valid conditional value
        if( str == null         ) return canEmpty;
        if( str.isEmpty()       ) return canEmpty;

        if( isKeyword     (str) ) return false;
        if( isFunctionName(str) ) return false;

        if( str.charAt(0) == '$' ) {
            if( str.length()       <  4         ) return false;
            if( str.charAt(1)      == '{'       ) return isSymbolName( trmRVarName(str), true ) || _pmTTVar.matcher(str).matches();
            if( _svarList.get(str) != null      ) return true;
            if( _pmPreqN.matcher(str).matches() ) return true;
            if( _pmPreqV.matcher(str).matches() ) return true;
            return false;
        }

        return true;
    }

    public static boolean isValidConditionValue(final TokenReader.Token token)
    { return isValidConditionValue(token, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum ALOperName {
        inc,
        dec,
        add,
        sub,
        mul,
        div,
        mod,
        abs,
        neg,
        shl,
        shr,
        min,
        max,
        not,
        and,
        or,
        xor
    }

    public static class ALOperSpec {
        public final ALOperName operName;  // The arithmetic/logic operations name (in enum)
        public final int        srcPCount; // The number of source parameters

        public ALOperSpec(final ALOperName operName_, final int srcPCount_)
        {
            operName  = operName_;
            srcPCount = srcPCount_;
        }
    }

    private static final HashMap<String, ALOperSpec> _operList = new HashMap<>();

    public static ALOperSpec getALOperSpec(final String operName)
    {
        // Initialize the map once and only once
        if( _operList.isEmpty() ) {
            _operList.put( "inc", new ALOperSpec(ALOperName.inc, 0) );
            _operList.put( "dec", new ALOperSpec(ALOperName.dec, 0) );
            _operList.put( "add", new ALOperSpec(ALOperName.add, 2) );
            _operList.put( "sub", new ALOperSpec(ALOperName.sub, 2) );
            _operList.put( "mul", new ALOperSpec(ALOperName.mul, 2) );
            _operList.put( "div", new ALOperSpec(ALOperName.div, 2) );
            _operList.put( "mod", new ALOperSpec(ALOperName.mod, 2) );
            _operList.put( "abs", new ALOperSpec(ALOperName.abs, 1) );
            _operList.put( "neg", new ALOperSpec(ALOperName.neg, 1) );
            _operList.put( "shl", new ALOperSpec(ALOperName.shl, 2) );
            _operList.put( "shr", new ALOperSpec(ALOperName.shr, 2) );
            _operList.put( "min", new ALOperSpec(ALOperName.min, 2) );
            _operList.put( "max", new ALOperSpec(ALOperName.max, 2) );
            _operList.put( "not", new ALOperSpec(ALOperName.not, 1) );
            _operList.put( "and", new ALOperSpec(ALOperName.and, 2) );
            _operList.put( "or",  new ALOperSpec(ALOperName.or , 2) );
            _operList.put( "xor", new ALOperSpec(ALOperName.xor, 2) );
        }

        // Search and return the arithmetic/logic-operation specification
        return _operList.get(operName);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum PostfixOper {
        u_p     (true , 14, "⊕"     ),
        u_m     (true , 14, "⊖"     ),
        u_abs   (true , 14, "@"     ),
        u_bw_not(true , 14, "~"     ),
        u_lg_not(true , 14, "!"     ),
        pow     (false, 13, "**"    ),
        mul     (false, 12, "*"     ),
        div     (false, 12, "/"     ),
        mod     (false, 12, "%"     ),
        add     (false, 11, "+"     ),
        sub     (false, 11, "-"     ),
        bw_shl  (false, 10, "<<"    ),
        bw_shr  (false, 10, ">>"    ),
        twc     (false,  9, "<=>"   ),
        lt      (false,  8, "<"     ),
        lte     (false,  8, "<="    ),
        gt      (false,  8, ">"     ),
        gte     (false,  8, ">="    ),
        eq      (false,  7, "=="    ),
        neq     (false,  7, "!="    ),
        bw_and  (false,  6, "&"     ),
        bw_xor  (false,  5, "^"     ),
        bw_or   (false,  4, "|"     ),
        lg_and  (false,  3, "&&"    ),
        lg_or   (false,  2, "||"    ),

        __ter_qm(false,  1, "?"     ), // The ternary operator is parsed as if it were a sequence of two binary operators
        __ter_cl(false,  1, ":"     ),
        ternary (false,  1, "?:"    ),

        f_sgn   (           "sgn", 1),
        f_min   (           "min", 2),
        f_max   (           "max", 2),
        __f_end (),
        __comma (),

        __lparen(),
        __rparen(),

        __none__()

        ;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final boolean unary; // A flag that indicates if this operator is a unary   operator

        public final int     oprec; // Out-stack precedence level
        public final int     iprec; // In-stack  precedence level

        public final String  label; // String label of this operator

        public final int     ifrac; // Required number of arguments of this inline function operator; zero means this operator is not an inline function

        private PostfixOper()
        {
            unary = false;
            oprec = 0;
            iprec = 0;
            label = null;
            ifrac = 0;
        }

        private PostfixOper(final String label_, final int ifrac_)
        {
            unary = false;
            oprec = 0;
            iprec = 0;
            label = label_;
            ifrac = ifrac_;
        }

        private PostfixOper(final boolean unary_, final int precedence_, final String label_)
        {
            unary = unary_;
            oprec = precedence_ + 1;
            iprec = precedence_;
            label = label_;
            ifrac = 0;
        }

        public boolean isUnaryOperator()
        { return (ifrac == 0) &&  unary; }

        public boolean isBinaryOperator()
        { return (ifrac == 0) && !unary; }

        public boolean isLogicalOperator()
        { return (this == u_lg_not) || (this == lg_and) || (this == lg_or); }

        public boolean isInlineFunctionBegin()
        { return (ifrac != 0); }

        public boolean isInlineFunctionBegin(final int reqArgCnt)
        { return (ifrac == reqArgCnt); }

        public boolean isOneArg()
        { return isUnaryOperator () || isInlineFunctionBegin(1); }

        public boolean isTwoArg()
        { return isBinaryOperator() || isInlineFunctionBegin(2); }
    }

    public static class PostfixTerm {
        public final PostfixOper operator;
        public final ReadVarSpec operand;

        public PostfixTerm(final PostfixOper operator_)
        {
            operator = operator_;
            operand  = null;
        }

        public PostfixTerm(final ReadVarSpec operand_)
        {
            operator = PostfixOper.__none__;
            operand  = operand_;
        }

        public boolean isOperator()
        { return operand == null; }

        public boolean isOperand()
        { return operand != null; }
    }

    @SuppressWarnings("serial")
    public static class PostfixTerms extends ArrayList<PostfixTerm> {};

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String trmRVarName(final String symbolName) // It actually can also be used to trim special variable names
    {
        // Remove the '${' and '}' as needed
        return ( symbolName.charAt(0) != '$' ) ? symbolName : symbolName.substring( 2, symbolName.length() - 1 );
    }

    public static String genRVarName(final String symbolName)
    { return "${" + symbolName + "}"; }

    public static String genSVarName(final String symbolName)
    { return XCom.SVar_NamePrefix + symbolName; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Pattern _pmIsNumeric     = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?");
    public static Pattern _pmIsPlainDecInt = Pattern.compile("\\d+");
    public static Pattern _pmIsPlainHexInt = Pattern.compile("0[xX][0-9a-zA-Z]+");

    public static boolean isNumeric(final String str)
    { return _pmIsNumeric.matcher(str).matches(); }

    public static boolean isPlainDecInteger(final String str)
    { return _pmIsPlainDecInt.matcher(str).matches(); }

    public static boolean isPlainHexInteger(final String str)
    { return _pmIsPlainHexInt.matcher(str).matches(); }

    public static boolean isPowerOfTwo(final short n) { return ( n & (n - 1) ) == 0; }
    public static boolean isPowerOfTwo(final int   n) { return ( n & (n - 1) ) == 0; }
    public static boolean isPowerOfTwo(final long  n) { return ( n & (n - 1) ) == 0; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Long toLong(final ExecBlock execBlock, final XCom.ExecData execData, final String str)
    {
        try {
            return Long.decode(str);
        }
        catch(final Exception e) {
            if( execData.execState.optionStack().enableWarnCnvStringInteger() ) {
                SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(),Texts.WMsg_CnvStringInteger, str );
            }
        }

        return Long.valueOf(0);
    }

    public static Boolean toBoolean(final ExecBlock execBlock, final XCom.ExecData execData, final String str)
    {
        final String strLC = str.toLowerCase();

        if( strLC.equals("true" ) ) return Boolean.TRUE;
        if( strLC.equals("false") ) return Boolean.FALSE;

        try {
            return ( Long.decode(str) != 0 ) ? Boolean.TRUE : Boolean.FALSE;
        }
        catch(final Exception e) {
            if( execData.execState.optionStack().enableWarnCnvStringBoolean() ) {
                SysUtil.printfWarning( execBlock.getPath(), execBlock.getLNum(), execBlock.getCNum(), Texts.WMsg_CnvStringBoolean, str );
            }
        }

        return Boolean.FALSE;
    }

    public static Object parseNumberStr(final String valueStr)
    {
        final String  strVal = valueStr.toUpperCase();
        final int     strLen = valueStr.length();
        final boolean isBin  = (strLen > 2) &&                     strVal.startsWith("0B");
        final boolean isHex  = (strLen > 2) &&                     strVal.startsWith("0X");
        final boolean isOct  = (strLen > 1) && !isBin && !isHex && strVal.startsWith("0" );

        if( strVal.contains(".") || ( !isHex && strVal.contains("E") ) ) {
            return Double.valueOf( Double.parseDouble(strVal) );
        }
        else  {
            final int     radix  = isBin ?  2
                                 : isOct ?  8
                                 : isHex ? 16
                                 :         10;
                  String  valStr = isBin ? strVal.substring(2)
                                 : isOct ? strVal.substring(1)
                                 : isHex ? strVal.substring(2)
                                 :         strVal;
                  long    value  = 0;
                  boolean isNeg  = false;


            if( isBin && valStr.length() == 64 && valStr.startsWith("1") ) {
                valStr = '0' + valStr.substring(1);
                isNeg  = true;
            }
            else if( isOct && valStr.length() == 22 && valStr.startsWith("1") ) {
                valStr = '0' + valStr.substring(1);
                isNeg  = true;
            }
            else if( isHex && valStr.length() == 16 && valStr.startsWith("F") ) {
                valStr = '7' + valStr.substring(1);
                isNeg  = true;
            }

                      value  = Long.parseLong(valStr, radix);
            if(isNeg) value += Long.MIN_VALUE;

            return Long.valueOf(value);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String[] stringArray(final String... strings)
    { return strings; }

    public static List<String> stringList(final String... strings)
    { return Arrays.asList(strings); }

    public static String nqStringEllipsis(final String str)
    { return (str == null) ? "<null>" : str; }

    public static String nqStringEllipsis(final String str, final int maxLen)
    {
        return nqStringEllipsis(
            ( str == null || str.length() < maxLen ) ? str : ( str.substring(0, maxLen - 3) + "..." )
        );
    }

    public static String sqStringEllipsis(final String str)
    {
        if(str == null) return "<null>";

        return str.isEmpty() ? "''" : ( "'" + str + "'" );
    }

    public static String sqStringEllipsis(final String str, final int maxLen)
    {
        return sqStringEllipsis(
            ( str == null || str.length() < maxLen - 2 ) ? str : ( str.substring(0, maxLen - 5) + "..." )
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmGetLTrim = Pattern.compile("^\\s+");
    private static final Pattern _pmGetRTrim = Pattern.compile("\\s+$");

    public static String re_ltrim(final String str)
    { return _pmGetLTrim.matcher(str).replaceAll(""); }

    public static String re_rtrim(final String str)
    { return _pmGetRTrim.matcher(str).replaceAll(""); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmTrailingSps = Pattern.compile("[ \\t]+(?=\\n)|[ \\t]+$");
    private static final Pattern _pmWhitespaces = Pattern.compile("\\s+"                   );
    private static final Pattern _pmWhitespace  = Pattern.compile("\\s"                    );
    private static final Pattern _pmTab         = Pattern.compile("\\t"                    );
    private static final Pattern _pmDot         = Pattern.compile("\\."                    );
    private static final Pattern _pmComma       = Pattern.compile(","                      );
    private static final Pattern _pmColon       = Pattern.compile(":"                      );
    private static final Pattern _pmSemicolon   = Pattern.compile(";"                      );
    private static final Pattern _pmDash        = Pattern.compile("\\-"                    );
    private static final Pattern _pmTrimWsOnly  = Pattern.compile("^\\s*|\\s*$"            );
    private static final Pattern _pmMultiWssDss = Pattern.compile("[\\s\\-]+"              );

    public static String[] re_splitPreserveTrailing(final String str, final Pattern re)
    { return re.split(str, -1); }

    public static String[] re_splitWhitespacesPreserveTrailing(final String str)
    { return re_splitPreserveTrailing(str, _pmWhitespaces); }

    public static String[] re_split(final String str, final Pattern re)
    { return re.split(str); }

    public static String[] re_splitWhitespaces(final String str)
    { return re_split(str, _pmWhitespaces); }

    public static String[] re_splitWhitespace(final String str)
    { return re_split(str, _pmWhitespace); }

    public static String[] re_splitTab(final String str)
    { return re_split(str, _pmTab); }

    public static String[] re_splitDot(final String str)
    { return re_split(str, _pmDot); }

    public static String[] re_splitComma(final String str)
    { return re_split(str, _pmComma); }

    public static String[] re_splitColon(final String str)
    { return re_split(str, _pmColon); }

    public static String[] re_splitSemicolon(final String str)
    { return re_split(str, _pmSemicolon); }

    public static String[] re_splitDash(final String str)
    { return re_split(str, _pmDash); }

    public static String re_replace(final String str, final Pattern re, final String replacement)
    { return re.matcher(str).replaceAll(replacement); }

    public static String re_replace(final String str, final Pattern re, final Function<Matcher, String> callback)
    {
        // NOTE: You may need to use 'Matcher.quoteReplacement()' on the replacement string within the callback function

        final StringBuffer res = new StringBuffer(); // NOTE : In Java 8, 'appendReplacement()' accepts only 'StringBuffer'
        final Matcher      m   = re.matcher(str);

        while( m.find() ) {
            m.appendReplacement( res, callback.apply(m) );
        }
        m.appendTail(res);

        return res.toString();
    }

    public static String re_replace(final String str, final Pattern re, final Map<String, String> repMap) throws IllegalArgumentException
    {
        // NOTE: You may need to use 'Matcher.quoteReplacement()' on the replacement string within the replacement map

        return re_replace( str, re, (final Matcher m) -> {
            if(repMap == null) throw new IllegalArgumentException( m.group(1) );
            final String res = repMap.get( m.group(1) );
            if(res == null) throw new IllegalArgumentException( m.group(1) );
            return res;
        } );
    }

    public static String re_removeAllTrailingSpaces(final String str) // NOTE : This will only remove trailing spaces (' ') and tabs ('\t')
    { return _pmTrailingSps.matcher(str).replaceAll(""); }

    public static String re_removeAllWhitespaces(final String str)
    { return _pmWhitespaces.matcher(str).replaceAll(""); }

    public static String re_trimWhitespacesOnly(final String str)
    { return _pmTrimWsOnly.matcher(str).replaceAll(""); }

    public static String re_replaceMultipleWhitespacesAndDash(final String str, final String rep)
    { return _pmMultiWssDss.matcher(str).replaceAll(rep); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmEscape = Pattern.compile("\\\\");
    private static final Pattern _pmDQ     = Pattern.compile("\""  );
    private static final Pattern _pmSQ     = Pattern.compile("'"   );
    private static final Pattern _pmBQ     = Pattern.compile("`"   );

    private static String _re_serStringDQ_impl(final String str, final boolean alwaysQuote)
    {
        final String  res   = _pmDQ.matcher( _pmEscape.matcher(str).replaceAll("\\\\\\\\") ).replaceAll("\\\\\"");
        final boolean quote = alwaysQuote || _pmWhitespace.matcher(res).find();

        return quote ? ('"' + res + '"') : res;
    }

    public static String re_serStringDQ(final String str) // It only escapes " and \
    { return _re_serStringDQ_impl(str, false); }

    public static String re_quoteStringDQ(final String str) // It only escapes " and \
    { return _re_serStringDQ_impl(str, true ); }

    private static String _re_serStringSQ_impl(final String str, final boolean alwaysQuote)
    {
        final String  res   = _pmSQ.matcher( _pmEscape.matcher(str).replaceAll("\\\\\\\\") ).replaceAll("\\\\'");
        final boolean quote = alwaysQuote || _pmWhitespace.matcher(res).find();

        return quote ? ('\'' + res + '\'') : res;
    }

    public static String re_serStringSQ(final String str) // It only escapes ' and \
    { return _re_serStringSQ_impl(str, false); }

    public static String re_quoteStringSQ(final String str) // It only escapes ' and \
    { return _re_serStringSQ_impl(str, true ); }

    private static String _re_serStringBQ_impl(final String str, final boolean alwaysQuote)
    {
        final String  res   = _pmBQ.matcher( _pmEscape.matcher(str).replaceAll("\\\\\\\\") ).replaceAll("\\\\`");
        final boolean quote = alwaysQuote || _pmWhitespace.matcher(res).find();

        return quote ? ('`' + res + '`') : res;
    }

    public static String re_serStringBQ(final String str) // It only escapes ` and \
    { return _re_serStringBQ_impl(str, false); }

    public static String re_quoteStringBQ(final String str) // It only escapes ` and \
    { return _re_serStringBQ_impl(str, true ); }

    public static String re_quoteDQIfContainsWhitespace(final String str)
    { return ( _pmWhitespace.matcher(str).find() ) ? re_quoteStringDQ(str) : str; }

    public static String re_quoteSQIfContainsWhitespace(final String str)
    { return ( _pmWhitespace.matcher(str).find() ) ? re_quoteStringSQ(str) : str; }

    public static String re_quoteBQIfContainsWhitespace(final String str)
    { return ( _pmWhitespace.matcher(str).find() ) ? re_quoteStringBQ(str) : str; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int _findClosingQuote(final String str, final int start)
    {
        final int n = str.length();
              int i = start;

        while(i < n) {

            final char c = str.charAt(i);

            if(c == '\\') {
                // Skip this backslash and the character following it
                i += 2;
                continue;
            }

            if(c == '"') return i;

            ++i;

        } // while

        return -1; // No closing quote found
    }

    public static String sm_deserStringDQ_removeAllNQWhitespaces(final String str) // It only unescapes \" and \\
    {
        final StringBuilder res  = new StringBuilder();
        final int           n    = str.length();
              int           i    = 0;
              boolean       inDQ = false;

        while(i < n) {

            final char c = str.charAt(i);

            if(inDQ) {
                // Inside a valid quoted segment, preserve all characters (including whitespace)
                if(c == '\\') {
                    // Process escape sequences (\" and \\)
                    if(i + 1 < n) {
                        final char next = str.charAt(i + 1);
                        if(next == '"' || next == '\\') {
                            res.append(next);
                            i += 2;
                            continue;
                        }
                    }
                    // If no valid escape follows, output the backslash itself
                    res.append(c);
                    ++i;
                }
                // An unescaped quote ends the quoted segment (do not output the delimiter)
                else if(c == '"') {
                    inDQ = false;
                    ++i;
                }
                // Append other characters as-is
                else {
                    res.append(c);
                    ++i;
                }
            }
            else {
                // Outside a quoted segment, skip any whitespace
                if( Character.isWhitespace(c) ) {
                    ++i;
                }
                // Process escape sequences (\" and \\)
                else if(c == '\\') {
                    if(i + 1 < n) {
                        final char next = str.charAt(i + 1);
                        if(next == '"' || next == '\\') {
                            res.append(next);
                            i += 2;
                            continue;
                        }
                    }
                    // If no valid escape follows, output the backslash itself
                    res.append(c);
                    ++i;
                }
                // Check for a double quote
                else if(c == '"') {
                    // Look ahead for a matching unescaped double quote
                    if( _findClosingQuote(str, i + 1) != -1 ) {
                        // Valid quoted segment found, skip the opening delimiter
                        inDQ = true;
                        ++i;
                    }
                    else {
                        // Skip unmatched quote
                        ++i;
                    }
                }
                // Append other characters as-is (whitespace was already skipped)
                else {
                    res.append(c);
                    ++i;
                }
            }

        } // while

        return res.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmAnyNewLine = Pattern.compile("\r\n|\n\r|\r|\n");

    public static String escapeHTML(final String str)
    {
        if(str == null) return "";

        final String escaped = str.replace("&" , "&amp;" )
                                  .replace("<" , "&lt;"  )
                                  .replace(">" , "&gt;"  )
                                  .replace("\"", "&quot;")
                                  .replace("'" , "&#39;" );

        return _pmAnyNewLine.matcher(escaped).replaceAll("<br>");
    }

    public static String unescapeString(final String str) // It unescapes (almost?) all escapes supported by Java
    {
        if( str == null   ) return null;
        if( str.isEmpty() ) return "";

        final int           len = str.length();
        final StringBuilder sb  = new StringBuilder(len);

        for(int i = 0; i < len; ++i) {

            final char ch = str.charAt(i);

            if(ch == '\\') {

                char nch = (i == len - 1) ? '\\' : str.charAt(i + 1);

                if( (nch == 'u') && (i < len - 5) ) { // Unicode escape
                    final int code = Integer.parseInt( str.substring(i + 2, i + 6), 16 );
                    sb.append( Character.toChars(code) );
                    i += 5;
                }
                else if( (nch == 'x') && (i < len - 3) ) { // Hexadecimal escape
                    final int code = Integer.parseInt( str.substring(i + 2, i + 4), 16 );
                    sb.append( (char) code );
                    i += 3;
                }
                else if( (nch >= '0') && nch <= '7' ) { // Octal escape
                    final StringBuilder code = new StringBuilder();
                    while( (i < len - 1) && (nch >= '0') && (nch <= '7') ) {
                        ++i;
                        code.append(nch);
                        nch = str.charAt(i + 1);
                    }
                    sb.append( (char) Integer.parseInt( code.toString(), 8 ) );
                }
                else {
                    switch(nch) {
                        case '\\': sb.append('\\'); break;
                        case 'b' : sb.append('\b'); break;
                        case 'f' : sb.append('\f'); break;
                        case 'n' : sb.append('\n'); break;
                        case 'r' : sb.append('\r'); break;
                        case 't' : sb.append('\t'); break;
                        case '\"': sb.append('\"'); break;
                        case '\'': sb.append('\''); break;
                        default  : sb.append(ch  ); continue;
                    }
                    ++i;
                }

            }
            else {
                sb.append(ch);
            }

        } // for

        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : Must be used together with 'unescapeNewLine()' below for symmetry
    public static String escapeNewLine(final String str)
    {
        final StringBuilder sb = new StringBuilder();

        for( int i = 0; i < str.length(); ++i ) {
            final char c = str.charAt(i);
            if(c == '\n') {
                sb.append("\\n");
            }
            else if(c == '\\') {
                if( i + 1 < str.length() && str.charAt(i + 1) == 'n' ) {
                    sb.append("\\\\n");
                    ++i;
                }
                else {
                    sb.append('\\');
                }
            }
            else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    // NOTE : Must be used together with 'escapeNewLine()' above for symmetry
    public static String unescapeNewLine(final String str)
    {
        StringBuilder sb = new StringBuilder();

        for( int i = 0; i < str.length(); ++i ) {
            final char c = str.charAt(i);
            if(c == '\\') {
                if( i + 1 < str.length() ) {
                    final char next = str.charAt(i + 1);
                    if(next == 'n') {
                        sb.append('\n');
                        ++i;
                        continue;
                    }
                    else if(next == '\\') {
                        if( i + 2 < str.length() && str.charAt(i + 2) == 'n' ) {
                            sb.append("\\n");
                            i += 2;
                            continue;
                        }
                    }
                }
            }
            sb.append(c);
        }

        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : Use this function before printing a string to the console
    public static String escapeControlChars(final String str)
    {
        final StringBuilder sb  = new StringBuilder();
        final int           len = str.length();

        for(int i = 0; i < len; ++i) {

            final char c = str.charAt(i);

            switch(c) {

                /*
                case '\\'     :
                    if(i + 1 < len) {
                        if( str.charAt(i + 1) == '\\' ) { sb.append("\\\\"); ++i; }
                        else                              sb.append(c);
                    }
                    break;
                */

                case '\n'     : sb.append("\\n"  ); break;
                case '\r'     : sb.append("\\r"  ); break;
                case '\t'     : sb.append("\\t"  ); break;
                case '\u000B' : sb.append("\\v"  ); break;
                case '\f'     : sb.append("\\f"  ); break;
                case '\u001B' : sb.append("\\x1B"); break;

                case '\\'     : sb.append("\\\\" ); break;

                default       : sb.append(c      ); break;

            } // case

        } // for

        return sb.toString();
    }

    public static String escapeControlCharsAndUseNBSP(final String str)
    { return escapeControlChars(str).replace(" " , "\u00A0"); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmTrailingNewLine = Pattern.compile("(?:\r\n|\n\r|\r|\n)+$");

    public static String stripTrailingNewlines(final String str)
    {
        if(str == null) return null;

        return _pmTrailingNewLine.matcher(str).replaceAll("");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmAllANSI = Pattern.compile(
           "(?:\\u001B\\[[0-9;?]*[ -/]*[@-~])"   // CSI + SGR
        + "|(?:\\u001B\\][\\s\\S]*?\\u0007)"     // OSC terminated by BEL
        + "|(?:\\u001B\\][\\s\\S]*?\\u001B\\\\)" // OSC terminated by ESC \
        + "|(?:\\u001B\\([ -/]*[@-~])"           // Charset select ESC (
        + "|(?:\\u001B\\)[ -/]*[@-~])"           // Charset select ESC )
        + "|(?:\\u001B[@-Z\\\\-_])"              // Single-char escapes (e.g., ESC c, ESC =, ESC >)
    );

    public static String stripAllANSIEscapeCode(final String str)
    { return _pmAllANSI.matcher(str).replaceAll(""); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<String> extract_kv(final String str, final String sep)
    {
        final ArrayList<String> res = new ArrayList<>();

        if( !sep.equals("") ) {
            final int idx = str.indexOf(sep);
            if( idx     > 0            ) res.add( str.substring( 0      , idx          ) );
            if( idx + 1 < str.length() ) res.add( str.substring( idx + 1, str.length() ) );
        }

        if( res.isEmpty() ) res.add(str);

        return res;
    }

    public static ArrayList<String> explode(final String str, final String sep)
    {
        final ArrayList<String> res = new ArrayList<>();

        if( sep.equals("") ) {
            for( final int cp : str.codePoints().toArray() ) res.add( new String( Character.toChars(cp) ) );
        }
        else {
            final String[] tokens = str.split( Pattern.quote(sep) );
            for(final String t : tokens) res.add(t);
        }

        return res;
    }

    public static void explode(final VariableValue retVal, final VariableValue parts, final String sep)
    {
        if( sep.equals("") ) {
            for(final VariableStore item : parts) {
                for( final int cp : item.value.codePoints().toArray() ) retVal.add( new VariableStore( true, new String( Character.toChars(cp) ) ) );
              //for( final char ch : item.value.toCharArray() ) retVal.add( new VariableStore( true, String.valueOf(ch) ) );
            }
        }
        else {
            for(final VariableStore item : parts) {
                final String[] tokens = item.value.split( Pattern.quote(sep) );
                for(final String t : tokens) retVal.add( new VariableStore(true, t) );
            }
        }
    }

    public static String flatten(final ArrayList<String> aryStr, final String separator) throws JXMException
    {
        final StringBuilder sb = new StringBuilder();

        for(final String item : aryStr) {
            if( sb.length() != 0 ) sb.append(separator);
                                   sb.append(item     );
        }

        return sb.toString();
    }

    public static String flatten(final VariableValue varVal, final String separator) throws JXMException
    {
        final StringBuilder sb = new StringBuilder();

        for(final VariableStore item : varVal) {
            if( sb.length() != 0 ) sb.append(separator );
                                   sb.append(item.value);
        }

        return sb.toString();
    }

    public static HashSet<String> toHashSet(final VariableValue varVal) throws JXMException
    {
        final HashSet<String> ts = new HashSet<>();

        for(final VariableStore item : varVal) ts.add(item.value);

        return ts;
    }

    public static TreeSet<String> toTreeSet(final VariableValue varVal) throws JXMException
    {
        final TreeSet<String> ts = new TreeSet<>();

        for(final VariableStore item : varVal) ts.add(item.value);

        return ts;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String globToRegExpStr(final String globStr)
    {
        final StringBuilder regexpSB = new StringBuilder();
        final int           max      = globStr.length();
              int           cnt      = 0;

        while(cnt < max) {

            // Get a chracter
            final char ch = globStr.charAt(cnt++);

            // Check if it is a '\'
            if(ch == '\\') {
                // Get the next character
                final char nch = (cnt < max) ? globStr.charAt(cnt) : ' ';
                // If the next character is a '?' or '%', put it as it is
                if(nch == '?' || nch == '%') {
                    regexpSB.append(nch);
                    ++cnt;
                }
                // Other characters, escape the '\' character
                else {
                    regexpSB.append('\\');
                    regexpSB.append('\\');
                }
            }
            // Check if it is a '?'
            else if(ch == '?') {
                regexpSB.append("([^.]?)");
            }
            // Check if it is a '%'
            else if(ch == '%') {
                regexpSB.append("([^.]+)");
            }
            // Escape these characters
            else if( ".^$*+?()[{|".indexOf(ch) != -1 ) {
                regexpSB.append('\\');
                regexpSB.append(ch  );
            }
            // Add other characters as is
            else {
                regexpSB.append(ch);
            }

        } // while cnt < max

        return regexpSB.toString();
    }

    public static Pattern globToRegExp(final String globStr)
    { return Pattern.compile( globToRegExpStr(globStr) ); }

    public static String globToReplacementStr(final String globStr, boolean preqPart)
    {

        final char          QM    = preqPart ? 0 : '?'; // Disable '?' when processing the prerequisite part
        final StringBuilder repSB = new StringBuilder();
        final int           max   = globStr.length();
              int           cnt   = 0;
              int           idx   = 1;

        while(cnt < max) {

            // Get a chracter
            final char ch = globStr.charAt(cnt++);

            // Check if it is a '\'
            if(ch == '\\') {
                // Get the next character
                final char nch = (cnt < max) ? globStr.charAt(cnt) : ' ';
                // Check if the next character is a '?' or '%'
                if(nch == QM || nch == '%') {
                    repSB.append(nch);
                    ++cnt;
                }
                // Other characters, escape the '\' character
                else {
                    repSB.append('\\');
                    repSB.append('\\');
                }
            }
            // Check if it is a '?' or '%'
            else if(ch == QM || ch == '%') {
                // Process the explicit part index when processing the prerequisite part
                if(preqPart) {
                    // Get the next character
                    final char nch = (cnt < max) ? globStr.charAt(cnt) : ' ';
                    // Check if the next character is a digit
                    if( Character.isDigit(nch) ) {
                        idx = Character.getNumericValue(nch);
                        ++cnt;
                    }
                }
                // Add the placeholder
                repSB.append( '$'                   );
                repSB.append( String.valueOf(idx++) );
            }
            // Add other characters as is
            else {
                repSB.append(ch);
            }

        } // while cnt < max

        return repSB.toString();
    }

    public static String reReplaceTokens(final String subject, final Pattern tokenPattern, final Function<String, String> cnvFunc)
    {
        // NOTE: This function is similar to 're_replace()', but it should be better suited for simple token substitutions

        /*
         * The code in this function is developed based on the information from:
         *     https://www.baeldung.com/java-regex-token-replacement
         */

        final StringBuilder output    = new StringBuilder();
        final Matcher       matcher   = tokenPattern.matcher(subject);
              int           lastIndex = 0;

        while( matcher.find() ) {
            output.append( subject, lastIndex, matcher.start() ).append( cnvFunc.apply( matcher.group() ) );
            lastIndex = matcher.end();
        }

        if( lastIndex < subject.length() ) output.append( subject, lastIndex, subject.length() );

        return output.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Stack<String> deepClone_StringStack(final Stack<String> refObj)
    {
        if(refObj == null) return null;

        final Stack<String> newObj = new Stack<>();

        for(final String item : refObj) newObj.push(item);

        return newObj;
    }

    public static ArrayList<String> deepClone_ArrayList(final ArrayList<String> refObj)
    {
        if(refObj == null) return null;

        final ArrayList<String> newObj = new ArrayList<>();

        newObj.addAll(refObj);

        return newObj;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean arrayAllElementsEqual(final boolean[] array)
    { for(int i = 1; i < array.length; ++i) { if(array[0] != array[i]) return false; } return true; }

    public static boolean arrayAllElementsEqual(final byte[] array)
    { for(int i = 1; i < array.length; ++i) { if(array[0] != array[i]) return false; } return true; }

    public static boolean arrayAllElementsEqual(final char[] array)
    { for(int i = 1; i < array.length; ++i) { if(array[0] != array[i]) return false; } return true; }

    public static boolean arrayAllElementsEqual(final short[] array)
    { for(int i = 1; i < array.length; ++i) { if(array[0] != array[i]) return false; } return true; }

    public static boolean arrayAllElementsEqual(final int[] array)
    { for(int i = 1; i < array.length; ++i) { if(array[0] != array[i]) return false; } return true; }

    public static boolean arrayAllElementsEqual(final long[] array)
    { for(int i = 1; i < array.length; ++i) { if(array[0] != array[i]) return false; } return true; }

    public static boolean arrayAllElementsEqual(final float[] array)
    { for(int i = 1; i < array.length; ++i) { if(array[0] != array[i]) return false; } return true; }

    public static boolean arrayAllElementsEqual(final double[] array)
    { for(int i = 1; i < array.length; ++i) { if(array[0] != array[i]) return false; } return true; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean[] arrayCopy(final boolean[] array)
    { return Arrays.copyOf(array, array.length); }

    public static byte[] arrayCopy(final byte[] array)
    { return Arrays.copyOf(array, array.length); }

    public static char[] arrayCopy(final char[] array)
    { return Arrays.copyOf(array, array.length); }

    public static short[] arrayCopy(final short[] array)
    { return Arrays.copyOf(array, array.length); }

    public static int[] arrayCopy(final int[] array)
    { return Arrays.copyOf(array, array.length); }

    public static long[] arrayCopy(final long[] array)
    { return Arrays.copyOf(array, array.length); }

    public static float[] arrayCopy(final float[] array)
    { return Arrays.copyOf(array, array.length); }

    public static double[] arrayCopy(final double[] array)
    { return Arrays.copyOf(array, array.length); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static byte[] arrayCopy(final byte[] srcArray, final int srcOffset, final int len)
    {
        final byte[] bytes = new byte[len];
        for(int i = 0; i < len; ++i) bytes[i] = srcArray[srcOffset + i];
        return bytes;
    }

    public static byte[] arrayCopy(final byte[] srcArray, final int srcOffset)
    { return arrayCopy(srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final byte[] dstArray, final int dstOffset, final byte[] srcArray, final int srcOffset, final int len)
    { for(int i = 0; i < len; ++i) dstArray[dstOffset + i] = srcArray[srcOffset + i]; }

    public static void arrayCopy(final byte[] dstArray, final int dstOffset, final byte[] srcArray, final int srcOffset)
    { arrayCopy(dstArray, dstOffset, srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final byte[] dstArray, final int dstOffset, final byte[] srcArray)
    { arrayCopy(dstArray, dstOffset, srcArray, 0, srcArray.length); }

    public static void arrayCopy(final byte[] dstArray, final byte[] srcArray, final int srcOffset, final int len)
    { arrayCopy(dstArray, 0, srcArray, srcOffset, len); }

    public static void arrayCopy(final byte[] dstArray, final byte[] srcArray, final int srcOffset)
    { arrayCopy(dstArray, 0, srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final byte[] dstArray, final byte[] srcArray)
    { arrayCopy(dstArray, 0, srcArray, 0, srcArray.length); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int[] arrayCopy(final int[] srcArray, final int srcOffset, final int len)
    {
        final int[] ints = new int[len];
        for(int i = 0; i < len; ++i) ints[i] = srcArray[srcOffset + i];
        return ints;
    }

    public static int[] arrayCopy(final int[] srcArray, final int srcOffset)
    { return arrayCopy(srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final int[] dstArray, final int dstOffset, final int[] srcArray, final int srcOffset, final int len)
    { for(int i = 0; i < len; ++i) dstArray[dstOffset + i] = srcArray[srcOffset + i]; }

    public static void arrayCopy(final int[] dstArray, final int dstOffset, final int[] srcArray, final int srcOffset)
    { arrayCopy(dstArray, dstOffset, srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final int[] dstArray, final int dstOffset, final int[] srcArray)
    { arrayCopy(dstArray, dstOffset, srcArray, 0, srcArray.length); }

    public static void arrayCopy(final int[] dstArray, final int[] srcArray, final int srcOffset, final int len)
    { arrayCopy(dstArray, 0, srcArray, srcOffset, len); }

    public static void arrayCopy(final int[] dstArray, final int[] srcArray, final int srcOffset)
    { arrayCopy(dstArray, 0, srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final int[] dstArray, final int[] srcArray)
    { arrayCopy(dstArray, 0, srcArray, 0, srcArray.length); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long[] arrayCopy(final long[] srcArray, final int srcOffset, final int len)
    {
        final long[] longs = new long[len];
        for(int i = 0; i < len; ++i) longs[i] = srcArray[srcOffset + i];
        return longs;
    }

    public static long[] arrayCopy(final long[] srcArray, final int srcOffset)
    { return arrayCopy(srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final long[] dstArray, final int dstOffset, final long[] srcArray, final int srcOffset, final int len)
    { for(int i = 0; i < len; ++i) dstArray[dstOffset + i] = srcArray[srcOffset + i]; }

    public static void arrayCopy(final long[] dstArray, final int dstOffset, final long[] srcArray, final int srcOffset)
    { arrayCopy(dstArray, dstOffset, srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final long[] dstArray, final int dstOffset, final long[] srcArray)
    { arrayCopy(dstArray, dstOffset, srcArray, 0, srcArray.length); }

    public static void arrayCopy(final long[] dstArray, final long[] srcArray, final int srcOffset, final int len)
    { arrayCopy(dstArray, 0, srcArray, srcOffset, len); }

    public static void arrayCopy(final long[] dstArray, final long[] srcArray, final int srcOffset)
    { arrayCopy(dstArray, 0, srcArray, srcOffset, srcArray.length - srcOffset); }

    public static void arrayCopy(final long[] dstArray, final long[] srcArray)
    { arrayCopy(dstArray, 0, srcArray, 0, srcArray.length); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int[] arrayConcatCopy(final int[]... arrays)
    {
        int totLength = 0;
        for(final int[] a : arrays) totLength += a.length;

        final int[] res = new int[totLength];
              int   idx = 0;
        for(final int[] a : arrays) {
            for(final int v : a) res[idx++] = v;
        }

        return res;
    }

    public static long[] arrayConcatCopy(final long[]... arrays)
    {
        int totLength = 0;
        for(final long[] a : arrays) totLength += a.length;

        final long[] res = new long[totLength];
              int    idx = 0;
        for(final long[] a : arrays) {
            for(final long v : a) res[idx++] = v;
        }

        return res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long[][] arrayCopy(final long[][] array)
    {
        final long[][] res = new long[ array.length][];

        int idx = 0;
        for(final long[] v : array) res[idx++] = arrayCopy(v);

        return res;
    }

    public static long[][] arrayConcatCopy(final long[][]... arrays)
    {
        int totLength = 0;
        for(final long[][] a : arrays) totLength += a.length;

        final long[][] res = new long[totLength][];
              int      idx = 0;
        for(final long[][] a : arrays) {
            for(final long[] s : a) res[idx++] = arrayCopy(s);
        }

        return res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static <T> boolean _arrayCompareExt_impl(final boolean exitOnFirstMismatch, final String a1Name, final String a2Name, final String fmtLen, final String fmtValue, final T[] a1, final T[] a2)
    {
        if(a1.length != a2.length) {
            SysUtil.stdDbg().printf("\n" + a1Name + ".length (" + fmtLen + ") != " + a2Name + ".length (" + fmtLen + ")\n\n", a1.length, a2.length);
            return false;
        }

        boolean first = true;
        boolean same  = true;

        for(int i = 0; i < a1.length; ++i) {
            if( !a1[i].equals(a2[i]) ) {
                if(first) SysUtil.stdDbg().println();
                SysUtil.stdDbg().printf(a1Name + "[" + fmtLen + "] (" + fmtValue + ") != " + a2Name + "[" + fmtLen + "] (" + fmtValue + ")\n", i, a1[i], i, a2[i]);
                first = false;
                same  = false;
                if(exitOnFirstMismatch) break;
            }
        }

        if(!same) SysUtil.stdDbg().println();

        return same;
    }

    public static boolean arrayCompareExt(final boolean exitOnFirstMismatch, final String a1Name, final String a2Name, final String fmtLen, final String fmtValue, final int[] a1, final int[] a2)
    {
        return _arrayCompareExt_impl(
            exitOnFirstMismatch, a1Name, a2Name, fmtLen, fmtValue,
            Arrays.stream(a1).boxed().toArray(Integer[]::new),
            Arrays.stream(a2).boxed().toArray(Integer[]::new)
        );
    }

    public static boolean arrayCompareExt(final boolean exitOnFirstMismatch, final String a1Name, final String a2Name, final String fmtLen, final String fmtValue, final long[] a1, final long[] a2)
    {
        return _arrayCompareExt_impl(
            exitOnFirstMismatch, a1Name, a2Name, fmtLen, fmtValue,
            Arrays.stream(a1).boxed().toArray(Integer[]::new),
            Arrays.stream(a2).boxed().toArray(Integer[]::new)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String base64StringFromByteArray(final byte[] bytes)
    { return Base64.getEncoder().encodeToString(bytes); }

    public static byte[] byteArrayFromBase64String(final String str)
    { return Base64.getDecoder().decode(str); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int log2(long value)
    {
        // Based on 'https://stackoverflow.com/a/3305710'

        int log = 0;

        if( ( value & 0xFFFFFFFF00000000L ) !=     0 ) { value >>>= 32; log  = 32; }
        if(   value                         >= 65536 ) { value >>>= 16; log += 16; }
        if(   value                         >=   256 ) { value >>>=  8; log +=  8; }
        if(   value                         >=    16 ) { value >>>=  4; log +=  4; }
        if(   value                         >=     4 ) { value >>>=  2; log +=  2; }

        return log + (int) (value >>> 1);
    }

    public static int log2(int value)
    {
        // Based on 'https://stackoverflow.com/a/3305710'

        int log = 0;

        if( ( value & 0xFFFF0000 ) !=   0 ) { value >>>= 16; log  = 16; }
        if(   value                >= 256 ) { value >>>=  8; log +=  8; }
        if(   value                >=  16 ) { value >>>=  4; log +=  4; }
        if(   value                >=   4 ) { value >>>=  2; log +=  2; }

        return log + (value >>> 1);
    }

    public static int parityU32(long x)
    {
        long y = x ^ (x >>  1);
             y = y ^ (y >>  2);
             y = y ^ (y >>  4);
             y = y ^ (y >>  8);
             y = y ^ (y >> 16);

        return ( (y & 1) == 0 ) ? 0x00 : 0x01;
    }

    public static long _BV(final long value, final int pos)
    { return value << pos; }

    public static long _BV(final int pos)
    { return _BV(0x00000001L, pos); }

    public static long _BVs(final int... pos)
    {
        long res = 0;
        for(final int p : pos) res |= _BV(0x00000001L, p);
        return res;
    }

    public static int _RU04(final int value)
    { return Integer.reverse(value) >>> (Integer.SIZE - 4); }

    public static int _RU06(final int value)
    { return Integer.reverse(value) >>> (Integer.SIZE - 6); }

    public static int _RU08(final int value)
    { return Integer.reverse(value) >>> (Integer.SIZE - Byte.SIZE); }

    public static int _RU16(final int value)
    { return Integer.reverse(value) >>> (Integer.SIZE - Short.SIZE); }

    public static int _RU24(final int value)
    { return Integer.reverse(value) >>> (Integer.SIZE - 24); }

    public static long _RU32(final long value)
    { return Long.reverse(value) >>> (Long.SIZE - Integer.SIZE); }

    public static int _SE16(final int value)
    {
        return   ( (value & 0xFF00) >>> 8 )
               | (  value & 0x00FF  <<  8 );
    }

    public static int _SE24(final int value)
    {
        return   ( (value & 0xFF0000) >>> 16 )
               | (  value & 0x00FF00         )
               | ( (value & 0x0000FF) <<  16 );
    }

    public static long _SE32(final long value)
    {
        return   ( (value & 0xFF000000) >>> 24 )
               | ( (value & 0x00FF0000) >>>  8 )
               | ( (value & 0x0000FF00) <<   8 )
               | ( (value & 0x000000FF) <<  24 );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int _M2CC(final char b, final char a)
    { return (b << 16) | a; }

    public static int _M2CC(final String s2cc)
    {
        final int len = s2cc.length();

        if(len > 2) return -1;

             if(len == 2) return _M2CC( s2cc.charAt(0), s2cc.charAt(1) );
        else if(len == 1) return _M2CC( s2cc.charAt(0), (char) 0       );
        else              return 0;
    }

    public static int _M2CCStr(final String str)
     {
        final char[] res = new char[2]; // Store converted values
              int    idx  = 0;

        for(int i = 0; i < str.length() && idx < 2; ++i) {
            if( str.charAt(i) == '\\' && i + 5 < str.length() && str.charAt(i + 1) == 'u' ) {
                // Extract the hexadecimal value and convert it into a normal character
                res[idx++] = (char) Integer.parseInt( str.substring(i + 2, i + 6), 16 );
                i += 5;
            }
            else {
                // Append the normal character as-is
                res[idx++] = str.charAt(i);
            }

        } // for

        return _M2CC(res[0], res[1]);
    }

    public static char[] _X2CC(final int v2cc)
    {
        return new char[] {
            (char) ( (v2cc >>> 16) & 0xFFFF ),
            (char) ( (v2cc >>>  0) & 0xFFFF )
        };
    }

    public static String _X2CCStr(final int v2cc)
    {
        final char[]   cc  = _X2CC(v2cc);
        final String[] res = new String[2];

        for(int i = 0; i < 2; ++i) {
            final boolean escape =  !Character.isDefined   (cc[i])  // Undefined Unicode characters
                                 ||  Character.isSurrogate (cc[i])  // Part of a surrogate pair
                                 ||  Character.isSpaceChar (cc[i])  // Whitespace-like space characters
                                 ||  Character.isWhitespace(cc[i])  // Tabs, newlines, and other space characters
                                 ||  Character.isISOControl(cc[i])  // Non-printable control characters
                                 ||                 '\\' == cc[i] ; // Backslash
            res[i] = escape ? String.format ( "\\u%04X", (int) cc[i] )
                            : String.valueOf( cc[i]                  );
        }

        return res[0] + res[1];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long _M4CC(final char d, final char c, final char b, final char a)
    { return (d << 48) | (c << 32) | (b << 16) | a; }

    public static long _M4CC(final String s4cc)
    {
        final int len = s4cc.length();

        if(len > 4) return -1;

             if(len == 4) return _M4CC( s4cc.charAt(0), s4cc.charAt(1), s4cc.charAt(2), s4cc.charAt(3) );
        else if(len == 3) return _M4CC( s4cc.charAt(0), s4cc.charAt(1), s4cc.charAt(2), (char) 0       );
        else if(len == 2) return _M4CC( s4cc.charAt(0), s4cc.charAt(1), (char) 0      , (char) 0       );
        else if(len == 1) return _M4CC( s4cc.charAt(0), (char) 0      , (char) 0      , (char) 0       );
        else              return 0;
    }

    public static char[] _X4CC(final long v2cc)
    {
        return new char[] {
            (char) ( (v2cc >>> 48) & 0xFFFF ),
            (char) ( (v2cc >>> 32) & 0xFFFF ),
            (char) ( (v2cc >>> 16) & 0xFFFF ),
            (char) ( (v2cc >>>  0) & 0xFFFF )
        };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String uintNNbinStr(final long value)
    { return Long.toBinaryString(value); }

    public static String uint08binStr(final long value)
    { return String.format( "%8s", Long.toBinaryString(value) ).replace(" ", "0"); }

    public static String uint16binStr(final long value)
    { return String.format( "%16s", Long.toBinaryString(value) ).replace(" ", "0"); }

    public static String uint24binStr(final long value)
    { return String.format( "%24s", Long.toBinaryString(value) ).replace(" ", "0"); }

    public static String uint32binStr(final long value)
    { return String.format( "%32s", Long.toBinaryString(value) ).replace(" ", "0"); }

    public static String uint64binStr(final long value)
    { return String.format( "%64s", Long.toBinaryString(value) ).replace(" ", "0"); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static <C, N extends Number> String strMatchClassConst(
        final String                           debugInfo,
        final Class<C>                         hostingClazz,
        final String                           constValue,
        final Class<N>                         numberType,
        final String                           outFrontMarker,
        final String                           outFormat,
        final BiFunction<Long, Double, String> custom
    )
    {
        for(final Field field : hostingClazz.getDeclaredFields() ) {

            // Skip if it is not a 'public static final field'
            final int modifiers  = field.getModifiers();

            if( !Modifier.isPublic(modifiers) ) continue;
            if( !Modifier.isStatic(modifiers) ) continue;
            if( !Modifier.isFinal (modifiers) ) continue;

            // Process based on the types
            try {

                boolean fp   = false;
                long    refL =  0;
                long    valL = -1;
                double  refD =  0;
                double  valD = -1;

                if(numberType == Byte.class) {
                    refL = ( (Number) field.get(null)                 ).byteValue  ();
                    valL = ( (Long  ) XCom.parseNumberStr(constValue) ).byteValue  ();
                }
                else if(numberType == Short.class) {
                    refL = ( (Number) field.get(null)                 ).shortValue ();
                    valL = ( (Long  ) XCom.parseNumberStr(constValue) ).shortValue ();
                }
                else if(numberType == Integer.class) {
                    refL = ( (Number) field.get(null)                 ).intValue   ();
                    valL = ( (Long  ) XCom.parseNumberStr(constValue) ).intValue   ();
                }
                else if(numberType == Long.class) {
                    refL = ( (Number) field.get(null)                 ).longValue  ();
                    valL = ( (Long  ) XCom.parseNumberStr(constValue) ).longValue  ();
                }
                else if(numberType == Float.class) {
                    refD = ( (Number) field.get(null)                 ).floatValue ();
                    valD = ( (Double) XCom.parseNumberStr(constValue) ).floatValue ();
                    fp   = true;
                }
                else if(numberType == Double.class) {
                    refD = ( (Number) field.get(null)                 ).doubleValue();
                    valD = ( (Double) XCom.parseNumberStr(constValue) ).doubleValue();
                    fp   = true;
                }

                // Returns the formatted field name if it matches
                if( fp ? (refD == valD) : (refL == valL) ) return String.format( outFormat, outFrontMarker + field.getName() );

                // Try to use the custom function
                if(custom != null) {
                    final String res = custom.apply(fp ? null : valL, fp ? valD : null);
                    if( res != null && !res.isEmpty() ) return res;
                }
            }
            catch(final Exception e) {
                if(debugInfo != null) {
                    SysUtil.stdDbg().printf( "XCom.strMatchClassConst():[%s]\n%s\n", debugInfo, SysUtil.stringFromStackTrace(e) );
                }
            }

        } // for

        // Return null if no match is found
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean _enableAllExceptionStackTrace = false;

    public static void setEnableAllExceptionStackTrace(final boolean enable)
    { _enableAllExceptionStackTrace = enable; }

    public static boolean enableAllExceptionStackTrace()
    { return _enableAllExceptionStackTrace; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String errorString(final String errMsg, final Object... args)
    { return (args == null) ? errMsg : String.format(errMsg, args); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Exception newException(final String errMsg)
    { return new Exception( errorString("%s", errMsg) ); }

    public static Exception newException(final String errMsg, final Object... args)
    { return new Exception( errorString(errMsg, args) ); }

    public static IOException newIOException(final String errMsg)
    { return new IOException( errorString("%s", errMsg) ); }

    public static IOException newIOException(final String errMsg, final Object... args)
    { return new IOException( errorString(errMsg, args) ); }

    public static SecurityException newSecurityException(final String errMsg)
    { return new SecurityException( errorString("%s", errMsg) ); }

    public static SecurityException newSecurityException(final String errMsg, final Object... args)
    { return new SecurityException( errorString(errMsg, args) ); }

    public static IllegalArgumentException newIllegalArgumentException(final String errMsg)
    { return new IllegalArgumentException( errorString("%s", errMsg) ); }

    public static IllegalArgumentException newIllegalArgumentException(final String errMsg, final Object... args)
    { return new IllegalArgumentException( errorString(errMsg, args) ); }

    public static NoSuchAlgorithmException newNoSuchAlgorithmException(final String errMsg)
    { return new NoSuchAlgorithmException( errorString("%s", errMsg) ); }

    public static NoSuchAlgorithmException newNoSuchAlgorithmException(final String errMsg, final Object... args)
    { return new NoSuchAlgorithmException( errorString(errMsg, args) ); }

    public static JXMException newJXMException(final String errMsg)
    { return new JXMException( errorString("%s", errMsg) ); }

    public static JXMException newJXMException(final String errMsg, final Object... args)
    { return new JXMException( errorString(errMsg, args) ); }

    public static JXMFatalInitError newJXMFatalInitError(final String errMsg)
    { return new JXMFatalInitError( errorString("%s", errMsg) ); }

    public static JXMFatalInitError newJXMFatalInitError(final String errMsg, final Object... args)
    { return new JXMFatalInitError( errorString(errMsg, args) ); }

    public static JXMFatalLogicError newJXMFatalLogicError(final String errMsg)
    { return new JXMFatalLogicError( errorString("%s", errMsg) ); }

    public static JXMFatalLogicError newJXMFatalLogicError(final String errMsg, final Object... args)
    { return new JXMFatalLogicError( errorString(errMsg, args) ); }

    public static JXMRuntimeError newJXMRuntimeError(final String errMsg)
    { return new JXMRuntimeError( errorString("%s", errMsg) ); }

    public static JXMRuntimeError newJXMRuntimeError(final String errMsg, final Object... args)
    { return new JXMRuntimeError( errorString(errMsg, args) ); }

    public static JXMAsmError newJXMAsmError(final String errMsg)
    { return new JXMAsmError( errorString("%s", errMsg) ); }

    public static JXMAsmError newJXMAsmError(final String errMsg, final Object... args)
    { return new JXMAsmError( errorString(errMsg, args) ); }

    public static <ExceptionT extends Exception> ExceptionT newTException(Class<ExceptionT> clazz, final String errMsg)
    {
        try {
            return clazz.getDeclaredConstructor(String.class).newInstance( errorString("%s", errMsg) );
        }
        catch(final Exception e) {}

        return null;
    }

    public static <ExceptionT extends Exception> ExceptionT newTException(Class<ExceptionT> clazz, final String errMsg, final Object... args)
    {
        try {
            return clazz.getDeclaredConstructor(String.class).newInstance( errorString(errMsg, args) );
        }
        catch(final Exception e) {}

        return null;
    }

} // class XCom
