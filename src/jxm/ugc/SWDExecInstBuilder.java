/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.ArrayList;
import java.util.HashMap;

import java.util.regex.Pattern;

import jxm.*;
import jxm.xb.*;


public class SWDExecInstBuilder extends SWDExecInstOpcode {

    private final ArrayList<long[]         > _instBuff       = new ArrayList<>(); // The instruction buffer
    private final ArrayList<Boolean        > _linkedState    = new ArrayList<>(); // A list of flags that indicate whether elements from the above list are already prelinked

    private final HashMap  <String, Short  > _labels         = new HashMap  <>(); // A list of labels and their target indexes to the instruction buffer elements
    private final ArrayList<String         > _jumpTarget     = new ArrayList<>(); // A list of jump targets (target labels) that will be resolved using the above map

    private       int                        _xviInternalCtr = 0;                 // A counter for generating unique internal XVI variables
    private       HashMap  <String, Integer> _uniqueLabelCtr = new HashMap  <>(); // A map of counters for generating unique names for labels that end with '%='

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private SWDExecInstBuilder _appendInstructions(final long[][] instructions, final boolean alreadyLinked)
    {
        for(final long[] instruction : instructions) {
            _instBuff   .add( XCom.arrayCopy(instruction) );
            _linkedState.add( alreadyLinked               );
        }

        return this;
    }

    private SWDExecInstBuilder _addInst(final long... values)
    {
        _instBuff   .add(values);
        _linkedState.add(false );

        return this;
    }

    private SWDExecInstBuilder _addInst(final int inst, final XVI[] xvi)
    {
        final long[] values = new long[xvi.length + 1];

                                            values[    0] = inst;
        for(int i = 0; i < xvi.length; ++i) values[i + 1] = xvi[i].value();

        return _addInst(values);
    }

    private short _addJumpTarget(final String label)
    {
        // Translate '%=' and add the jump target
        _jumpTarget.add( _translateLabel(label) );

        // Return the jump target index
        return (short) ( _jumpTarget.size() - 1 );
    }

    private long[][] _link_impl() throws Exception
    {
        // Return null if no instruction exists
        if( _instBuff.size() == 0 ) return null;

        // Prepare the result buffer
        final long[][] buffer = new long[ _instBuff.size() ][];

        // Process the instructions
        for(int i = 0; i < buffer.length; ++i) {

            // Get the instruction
            final long[] inst = _instBuff.get(i);

            // Resolve the relative jump index as needed
            if( !_linkedState.get(i) ) {
                if(inst[0] >= __INS_J__BEG__ && inst[0] <= __INS_J__END__) {
                    // Get and check the jump target index
                    final int jtIdx = (int) inst[inst.length - 1];
                    if( jtIdx < 0 || jtIdx > _jumpTarget.size() ) throw XCom.newJXMFatalLogicError(Texts.EMsg_IndexOutOfRange, jtIdx);
                    // Get and check the jump target
                    final String jtStr = _jumpTarget.get(jtIdx);
                    if(jtStr == null) throw XCom.newJXMFatalLogicError(Texts.EMsg_InvalidIndex, jtIdx);
                    // Get the jump target absolute position
                    final Short jtPos = _labels.get(jtStr);
                    if(jtPos == null) throw XCom.newJXMFatalLogicError(Texts.EMsg_InvalidReference, jtStr);
                    // Store the relative jump index
                    inst[inst.length - 1] = _labels.get(jtStr).longValue() - i - 1;
                }
            }

            // Store the instruction
            buffer[i] = inst;

        } // for

        // Return the result buffer
        return buffer;
    }

    private void _clear()
    {
        _instBuff   .clear();
        _linkedState.clear();

        _jumpTarget .clear();
        _labels     .clear();

        _xviInternalCtr = 0;
        _uniqueLabelCtr.clear();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder appendPrelinkedInst(final long[][] instructions)
    { return _appendInstructions(instructions, true); }

    public long[][] link()
    {
        try {
            // Link and clear
            final long[][] res = _link_impl();
            _clear();
            // Return the result
            return res;
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XVI xviInternal()
    { return XVI.internal(_xviInternalCtr++); }

    // Call this function once for every name for before every instruction block that require a unique numbering from '%='
    public String uniqueLabelCounter(final String name)
    {
        final Integer ctr = _uniqueLabelCtr.get(name);

        if(ctr == null) _uniqueLabelCtr.put    (name, 0      );
        else            _uniqueLabelCtr.replace(name, ctr + 1);

        return name;
    }

    // Call this function (instead of the one above) if you need the fully translated label name
    public String uniqueLabelName(final String name)
    { return _translateLabel( uniqueLabelCounter(name) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmUniqueLabelCtr = Pattern.compile("%=$");

    private String _translateLabel(final String name)
    { return _pmUniqueLabelCtr.matcher(name).replaceAll( String.format( "%05d", _uniqueLabelCtr.get(name) ) ); }

    public SWDExecInstBuilder label(final String name_)
    {
        // Translate '%='
        final String name =_translateLabel(name_);

        // Check for duplicated label
        if( _labels.get(name) != null ) {
            // Create a new exception object
            final Exception e = XCom.newJXMFatalLogicError(Texts.EMsg_DuplicatedReference, name);
            // Notify error
            if( !XCom.enableAllExceptionStackTrace() ) SysUtil.stdErr().println( e.getMessage() );
            SysUtil.systemExitError( XCom.enableAllExceptionStackTrace() ? e : null );
        }

        // Put the label
        _labels.put( name, (short) _instBuff.size() );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder nop(final Object... unused)
    { return _addInst( INS_NOP ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder getSWDFrequency(final XVI dst_variable_index)
    { return _addInst( INS_GET_SWD_FREQUENCY_VI, dst_variable_index.value() ); }

    public SWDExecInstBuilder setSWDFrequency(final XVI src_variable_index)
    { return _addInst( INS_SET_SWD_FREQUENCY_VI, src_variable_index.value() ); }

    public SWDExecInstBuilder setSWDFrequency(final long immValue)
    {
               mov            ( immValue, XVI.transitory(0) );
        return setSWDFrequency(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder swdLineReset()
    { return _addInst( INS_SWD_LINE_RESET ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder getCore_S_LOCKUP(final XVI dst_variable_index)
    { return _addInst( INS_GET_CORE_S_LOCKUP_VI, dst_variable_index.value() ); }

    public SWDExecInstBuilder getCore_S_SLEEP(final XVI dst_variable_index)
    { return _addInst( INS_GET_CORE_S_SLEEP_VI, dst_variable_index.value() ); }

    public SWDExecInstBuilder getCore_S_HALT(final XVI dst_variable_index)
    { return _addInst( INS_GET_CORE_S_HALT_VI, dst_variable_index.value() ); }

    public SWDExecInstBuilder getCore_IsRunning(final XVI dst_variable_index)
    { return _addInst( INS_GET_CORE_IS_RUNNING_VI, dst_variable_index.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder getDPVersion(final XVI dst_variable_index)
    { return _addInst( INS_GET_DP_VERSION_VI, dst_variable_index.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder exitRet(final XVI valVarIndex)
    { return _addInst( INS_EXIT_RET_VI, valVarIndex.value() ); }

    public SWDExecInstBuilder exitRet(final long immValue)
    { return _addInst( INS_EXIT_RET_IM, immValue ); }

    public SWDExecInstBuilder exitErr(final XVI valVarIndex)
    { return _addInst( INS_EXIT_ERR_VI, valVarIndex.value() ); }

    public SWDExecInstBuilder exitErr(final long immValue)
    { return _addInst( INS_EXIT_ERR_IM, immValue ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder exitRetIfCmpEQ(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_EXIT_RET_IF_CMP_EQ_VI_VI, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder exitRetIfCmpEQ(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_EXIT_RET_IF_CMP_EQ_VI_IM, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder exitRetIfCmpNEQ(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_EXIT_RET_IF_CMP_NEQ_VI_VI, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder exitRetIfCmpNEQ(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_EXIT_RET_IF_CMP_NEQ_VI_IM, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder exitRetIfZero(final XVI chkVarIndex)
    { return exitRetIfCmpEQ( chkVarIndex, 0 ); }

    public SWDExecInstBuilder exitRetIfNotZero(final XVI chkVarIndex)
    { return exitRetIfCmpNEQ( chkVarIndex, 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder delayUS(final int delayTime)
    { return _addInst( INS_DELAY_US_IM, delayTime ); }

    public SWDExecInstBuilder delayMS(final int delayTime)
    { return _addInst( INS_DELAY_MS_IM, delayTime ); }

    public SWDExecInstBuilder threadYield()
    { return _addInst( INS_THREAD_YIELD ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder getUS(final XVI dstVarIndex)
    { return _addInst( INS_GET_US_VI, dstVarIndex.value() ); }

    public SWDExecInstBuilder getMS(final XVI dstVarIndex)
    { return _addInst( INS_GET_MS_VI, dstVarIndex.value() ); }

    public SWDExecInstBuilder setWhileTimeoutMS(final XVI srcVarIndex)
    { return _addInst( INS_SET_WHILE_TIMEOUT_MS_VI, srcVarIndex.value() ); }

    public SWDExecInstBuilder setWhileTimeoutMS(final long immValue)
    { return _addInst( INS_SET_WHILE_TIMEOUT_MS_IM, immValue ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder haltCore(final boolean reset)
    { return _addInst( INS_HALT_CORE_IM, reset ? 1 : 0 ); }

    public SWDExecInstBuilder unhaltCore(final boolean reset, final boolean enableDebug)
    { return _addInst( INS_UNHALT_CORE_IM_IM, reset ? 1 : 0, enableDebug ? 1 : 0 ); }

    public SWDExecInstBuilder haltAllCores()
    { return _addInst( INS_HALT_ALL_CORES ); }

    public SWDExecInstBuilder resetUnhaltAllCores()
    { return _addInst( INS_RESET_UNHALT_ALL_CORES ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder wrCReg(final int regIndex, final XVI srcVarIndex)
    { return _addInst( INS_WR_CREG_IM_VI, regIndex, srcVarIndex.value() ); }

    public SWDExecInstBuilder wrCReg(final int regIndex, final long immValue)
    { return _addInst( INS_WR_CREG_IM_IM, regIndex, immValue ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder rdCReg(final int regIndex, final XVI dstVarIndex)
    { return _addInst( INS_RD_CREG_VI, regIndex, dstVarIndex.value() ); }

    public SWDExecInstBuilder rdCRegErrIfCmpEQ(final int regIndex, final long andBitmask, final XVI refVarIndex)
    { return _addInst( INS_RD_CREG_ERR_CMP_EQ_IM_VI, regIndex, andBitmask, refVarIndex.value() ); }

    public SWDExecInstBuilder rdCRegErrIfCmpNEQ(final int regIndex, final long andBitmask, final XVI refVarIndex)
    { return _addInst( INS_RD_CREG_ERR_CMP_NEQ_IM_VI, regIndex, andBitmask, refVarIndex.value() ); }

    public SWDExecInstBuilder rdCRegErrIfCmpEQ(final int regIndex, final long andBitmask, final long immValue)
    {
               mov              ( immValue,             XVI.transitory(0) );
        return rdCRegErrIfCmpEQ ( regIndex, andBitmask, XVI.transitory(0) );
    }

    public SWDExecInstBuilder rdCRegErrIfCmpNEQ(final int regIndex, final long andBitmask, final long immValue)
    {
               mov              ( immValue,             XVI.transitory(0) );
        return rdCRegErrIfCmpNEQ( regIndex, andBitmask, XVI.transitory(0) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder wrCMem(final XVI adrVarIndex, final XVI cntVarIndex, final long transferSize)
    { return _addInst( INS_WR_CMEM_VI_VI, adrVarIndex.value(), cntVarIndex.value(), transferSize ); }

    public SWDExecInstBuilder wrCMem(final XVI adrVarIndex, final long dataCount, final long transferSize)
    { return _addInst( INS_WR_CMEM_VI_IM, adrVarIndex.value(), dataCount, transferSize ); }

    public SWDExecInstBuilder wrCMem(final long address, final XVI cntVarIndex, final long transferSize)
    { return _addInst( INS_WR_CMEM_IM_VI, address, cntVarIndex.value(), transferSize ); }

    public SWDExecInstBuilder wrCMem(final long address, final long dataCount, final long transferSize)
    { return _addInst( INS_WR_CMEM_IM_IM, address, dataCount, transferSize ); }

    public SWDExecInstBuilder wrCMem(final XVI adrVarIndex, final XVI cntVarIndex)
    { return wrCMem( adrVarIndex, cntVarIndex, 0 ); }

    public SWDExecInstBuilder wrCMem(final XVI adrVarIndex, final long dataCount)
    { return wrCMem( adrVarIndex, dataCount, 0 ); }

    public SWDExecInstBuilder wrCMem(final long address, final XVI cntVarIndex)
    { return wrCMem( address, cntVarIndex, 0 ); }

    public SWDExecInstBuilder wrCMem(final long address, final long dataCount)
    { return wrCMem( address, dataCount, 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder rdCMem(final XVI adrVarIndex, final XVI cntVarIndex, final long transferSize)
    { return _addInst( INS_RD_CMEM_VI_VI, adrVarIndex.value(), cntVarIndex.value(), transferSize ); }

    public SWDExecInstBuilder rdCMem(final XVI adrVarIndex, final long dataCount, final long transferSize)
    { return _addInst( INS_RD_CMEM_VI_IM, adrVarIndex.value(), dataCount, transferSize ); }

    public SWDExecInstBuilder rdCMem(final long address, final XVI cntVarIndex, final long transferSize)
    { return _addInst( INS_RD_CMEM_IM_VI, address, cntVarIndex.value(), transferSize ); }

    public SWDExecInstBuilder rdCMem(final long address, final long dataCount, final long transferSize)
    { return _addInst( INS_RD_CMEM_IM_IM, address, dataCount, transferSize ); }

    public SWDExecInstBuilder rdCMem(final XVI adrVarIndex, final XVI cntVarIndex)
    { return rdCMem( adrVarIndex, cntVarIndex, 0 ); }

    public SWDExecInstBuilder rdCMem(final XVI adrVarIndex, final long dataCount)
    { return rdCMem( adrVarIndex, dataCount, 0 ); }

    public SWDExecInstBuilder rdCMem(final long address, final XVI cntVarIndex)
    { return rdCMem( address, cntVarIndex, 0 ); }

    public SWDExecInstBuilder rdCMem(final long address, final long dataCount)
    { return rdCMem( address, dataCount, 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder wrBus(final long address, final XVI srcVarIndex)
    { return _addInst( INS_WR_BUS_IM_VI, address, srcVarIndex.value() ); }

    public SWDExecInstBuilder wrBus(final long address, final long immValue)
    { return _addInst( INS_WR_BUS_IM_IM, address, immValue ); }

    public SWDExecInstBuilder wrBus(final XVI adrVarIndex, final XVI srcVarIndex)
    { return _addInst( INS_WR_BUS_VI_VI, adrVarIndex.value(), srcVarIndex.value() ); }

    public SWDExecInstBuilder wrBus(final XVI adrVarIndex, final long immValue)
    { return _addInst( INS_WR_BUS_VI_IM, adrVarIndex.value(), immValue ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder wrBusSet16Bit()
    { return _addInst( INS_SET_WR_BUS16 ); }

    public SWDExecInstBuilder wrBusSet32Bit()
    { return _addInst( INS_SET_WR_BUS32 ); }

    public SWDExecInstBuilder wrBus16x1(final long address, final XVI srcVarIndex)
    { return _addInst( INS_WR_BUS16X1_IM_VI, address, srcVarIndex.value() ); }

    public SWDExecInstBuilder wrBus16x2(final long address, final XVI srcVarIndex)
    { return _addInst( INS_WR_BUS16X2_IM_VI, address, srcVarIndex.value() ); }

    public SWDExecInstBuilder wrBus16x1(final long address, final long immValue)
    {
               mov      ( immValue, XVI.transitory(0) );
        return wrBus16x1( address , XVI.transitory(0) );
    }

    public SWDExecInstBuilder wrBus16x2(final long address, final long immValue)
    {
               mov      ( immValue, XVI.transitory(0) );
        return wrBus16x2( address , XVI.transitory(0) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder rdBusRetVal(final long address, final long andBitmask)
    { return _addInst( INS_RD_BUS_RET_VAL_IM, address, andBitmask ); }

    public SWDExecInstBuilder rdBusRetCmpEQ(final long address, final long andBitmask, final XVI refVarIndex)
    { return _addInst( INS_RD_BUS_RET_CMP_EQ_IM_VI, address, andBitmask, refVarIndex.value() ); }

    public SWDExecInstBuilder rdBusRetCmpEQ(final long address, final long andBitmask, final long refImmValue)
    { return _addInst( INS_RD_BUS_RET_CMP_EQ_IM_IM, address, andBitmask, refImmValue ); }

    public SWDExecInstBuilder rdBusRetCmpNEQ(final long address, final long andBitmask, final XVI refVarIndex)
    { return _addInst( INS_RD_BUS_RET_CMP_NEQ_IM_VI, address, andBitmask, refVarIndex.value() ); }

    public SWDExecInstBuilder rdBusRetCmpNEQ(final long address, final long andBitmask, final long refImmValue)
    { return _addInst( INS_RD_BUS_RET_CMP_NEQ_IM_IM, address, andBitmask, refImmValue ); }

    public SWDExecInstBuilder rdBusLoopWhileCmpEQ(final long address, final long andBitmask, final XVI refVarIndex)
    { return _addInst( INS_RD_BUS_WHILE_EQ_IM_VI, address, andBitmask, refVarIndex.value() ); }

    public SWDExecInstBuilder rdBusLoopWhileCmpEQ(final long address, final long andBitmask, final long refImmValue)
    { return _addInst( INS_RD_BUS_WHILE_EQ_IM_IM, address, andBitmask, refImmValue ); }

    public SWDExecInstBuilder rdBusLoopWhileCmpNEQ(final long address, final long andBitmask, final XVI refVarIndex)
    { return _addInst( INS_RD_BUS_WHILE_NEQ_IM_VI, address, andBitmask, refVarIndex.value() ); }

    public SWDExecInstBuilder rdBusLoopWhileCmpNEQ(final long address, final long andBitmask, final long refImmValue)
    { return _addInst( INS_RD_BUS_WHILE_NEQ_IM_IM, address, andBitmask, refImmValue ); }

    public SWDExecInstBuilder rdBusLoopWhileCmpEQ(final long address, final long andBitmask, final XVI refVarIndex, final long timeoutMS)
    {
                           setWhileTimeoutMS  ( timeoutMS                        );
                           rdBusLoopWhileCmpEQ( address, andBitmask, refVarIndex );
        if(timeoutMS >= 0) setWhileTimeoutMS  ( -1                               ); // Restore the default timeout as needed

        return this;
    }

    public SWDExecInstBuilder rdBusLoopWhileCmpEQ(final long address, final long andBitmask, final long refImmValue, final long timeoutMS)
    {
                           setWhileTimeoutMS  ( timeoutMS                        );
                           rdBusLoopWhileCmpEQ( address, andBitmask, refImmValue );
        if(timeoutMS >= 0) setWhileTimeoutMS  ( -1                               ); // Restore the default timeout as needed

        return this;
    }

    public SWDExecInstBuilder rdBusLoopWhileCmpNEQ(final long address, final long andBitmask, final XVI refVarIndex, final long timeoutMS)
    {
                           setWhileTimeoutMS   ( timeoutMS                        );
                           rdBusLoopWhileCmpNEQ( address, andBitmask, refVarIndex );
        if(timeoutMS >= 0) setWhileTimeoutMS   ( -1                               ); // Restore the default timeout as needed

        return this;
    }

    public SWDExecInstBuilder rdBusLoopWhileCmpNEQ(final long address, final long andBitmask, final long refImmValue, final long timeoutMS)
    {
                           setWhileTimeoutMS   ( timeoutMS                        );
                           rdBusLoopWhileCmpNEQ( address, andBitmask, refImmValue );
        if(timeoutMS >= 0) setWhileTimeoutMS   ( -1                               ); // Restore the default timeout as needed

        return this;
    }

    public SWDExecInstBuilder rdBusErrIfCmpEQ(final long address, final long andBitmask, final XVI refVarIndex)
    { return _addInst( INS_RD_BUS_ERR_CMP_EQ_IM_VI, address, andBitmask, refVarIndex.value() ); }

    public SWDExecInstBuilder rdBusErrIfCmpEQ(final long address, final long andBitmask, final long refImmValue)
    { return _addInst( INS_RD_BUS_ERR_CMP_EQ_IM_IM, address, andBitmask, refImmValue ); }

    public SWDExecInstBuilder rdBusErrIfCmpNEQ(final long address, final long andBitmask, final XVI refVarIndex)
    { return _addInst( INS_RD_BUS_ERR_CMP_NEQ_IM_VI, address, andBitmask, refVarIndex.value() ); }

    public SWDExecInstBuilder rdBusErrIfCmpNEQ(final long address, final long andBitmask, final long refImmValue)
    { return _addInst( INS_RD_BUS_ERR_CMP_NEQ_IM_IM, address, andBitmask, refImmValue ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder rdBusStr(final long address, final long andBitmask, final XVI dstVarIndex)
    { return _addInst( INS_RD_BUS_STR_IM_VI, address, andBitmask, dstVarIndex.value() ); }

    public SWDExecInstBuilder rdBusStrH16(final long address, final long andBitmask, final XVI dstVarIndex)
    { return _addInst( INS_RD_BUS_STR_H16_IM_VI, address, andBitmask, dstVarIndex.value() ); }

    public SWDExecInstBuilder rdBusStrL16(final long address, final long andBitmask, final XVI dstVarIndex)
    { return _addInst( INS_RD_BUS_STR_L16_IM_VI, address, andBitmask, dstVarIndex.value() ); }

    public SWDExecInstBuilder rdBusStrL16(final long address, final long andBitmask, final XVI dstVarIndexH, final XVI dstVarIndexL)
    { return _addInst( INS_RD_BUS_STR_HL16_IM_VI, address, andBitmask, dstVarIndexH.value(), dstVarIndexL.value() ); }

    public SWDExecInstBuilder rdBusStr(final long address, final long andBitmask, final XVI dstVarIndex, final long errImmValue, final XVI sttVarIndex)
    { return _addInst( INS_RD_BUS_STR_IM_VI_TRY_CHK, address, andBitmask, dstVarIndex.value(), errImmValue, sttVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder rdBusStr(final XVI address, final long andBitmask, final XVI dstVarIndex)
    { return _addInst( INS_RD_BUS_STR_VI_VI, address.value(), andBitmask, dstVarIndex.value() ); }

    public SWDExecInstBuilder rdBusStrH16(final XVI address, final long andBitmask, final XVI dstVarIndex)
    { return _addInst( INS_RD_BUS_STR_H16_VI_VI, address.value(), andBitmask, dstVarIndex.value() ); }

    public SWDExecInstBuilder rdBusStrL16(final XVI address, final long andBitmask, final XVI dstVarIndex)
    { return _addInst( INS_RD_BUS_STR_L16_VI_VI, address.value(), andBitmask, dstVarIndex.value() ); }

    public SWDExecInstBuilder rdBusStrL16(final XVI address, final long andBitmask, final XVI dstVarIndexH, final XVI dstVarIndexL)
    { return _addInst( INS_RD_BUS_STR_HL16_VI_VI, address.value(), andBitmask, dstVarIndexH.value(), dstVarIndexL.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder rdBus16Str(final long address, final long andBitmask, final XVI dstVarIndex)
    { return _addInst( INS_RD_BUS_16_STR_IM_VI, address, andBitmask, dstVarIndex.value() ); }

    public SWDExecInstBuilder rdBus16Str(final XVI address, final long andBitmask, final XVI dstVarIndex)
    { return _addInst( INS_RD_BUS_16_STR_VI_VI, address.value(), andBitmask, dstVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder mdBus(final long address, final long clrBitmask, final XVI setVarIndex)
    { return _addInst( INS_MD_BUS_IM_IM_VI, address        , clrBitmask         , setVarIndex.value() ); }

    public SWDExecInstBuilder mdBus(final long address, final long clrBitmask, final long setBitmask)
    { return _addInst( INS_MD_BUS_IM_IM_IM, address        , clrBitmask         , setBitmask          ); }

    public SWDExecInstBuilder mdBus(final long address, final XVI clrVarIndex, final XVI setVarIndex)
    { return _addInst( INS_MD_BUS_IM_VI_VI, address        , clrVarIndex.value(), setVarIndex.value() ); }

    public SWDExecInstBuilder mdBus(final long address, final XVI clrVarIndex, final long setBitmask)
    { return _addInst( INS_MD_BUS_IM_VI_IM, address        , clrVarIndex.value(), setBitmask          ); }

    public SWDExecInstBuilder mdBus(final XVI address, final long clrBitmask, final XVI setVarIndex)
    { return _addInst( INS_MD_BUS_VI_IM_VI, address.value(), clrBitmask         , setVarIndex.value() ); }

    public SWDExecInstBuilder mdBus(final XVI address, final long clrBitmask, final long setBitmask)
    { return _addInst( INS_MD_BUS_VI_IM_IM, address.value(), clrBitmask         , setBitmask          ); }

    public SWDExecInstBuilder mdBus(final XVI address, final XVI clrVarIndex, final XVI setVarIndex)
    { return _addInst( INS_MD_BUS_VI_VI_VI, address.value(), clrVarIndex.value(), setVarIndex.value() ); }

    public SWDExecInstBuilder mdBus(final XVI address, final XVI clrVarIndex, final long setBitmask)
    { return _addInst( INS_MD_BUS_VI_VI_IM, address.value(), clrVarIndex.value(), setBitmask          ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder wrBusDB(final long address, final long immValue)
    { return _addInst( INS_WR_BUS_DB_IM_IM, address, immValue ); }

    public SWDExecInstBuilder wrBusDB(final long address, final XVI ofsVarIndex)
    { return _addInst( INS_WR_BUS_DB_IM_VI, address, ofsVarIndex.value() ); }

    public SWDExecInstBuilder wrBusDB(final XVI adrVarIndex, final long immValue)
    { return _addInst( INS_WR_BUS_DB_VI_IM, adrVarIndex.value(), immValue ); }

    public SWDExecInstBuilder wrBusDB(final XVI adrVarIndex, final XVI ofsVarIndex)
    { return _addInst( INS_WR_BUS_DB_VI_VI, adrVarIndex.value(), ofsVarIndex.value() ); }

    public SWDExecInstBuilder wrBusSB(final long address, final long immValue)
    { return _addInst( INS_WR_BUS_SB_IM_IM, address, immValue ); }

    public SWDExecInstBuilder wrBusSB(final long address, final XVI ofsVarIndex)
    { return _addInst( INS_WR_BUS_SB_IM_VI, address, ofsVarIndex.value() ); }

    public SWDExecInstBuilder wrBusSB(final XVI adrVarIndex, final long immValue)
    { return _addInst( INS_WR_BUS_SB_VI_IM, adrVarIndex.value(), immValue ); }

    public SWDExecInstBuilder wrBusSB(final XVI adrVarIndex, final XVI ofsVarIndex)
    { return _addInst( INS_WR_BUS_SB_VI_VI, adrVarIndex.value(), ofsVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder rdBusDB(final long address, final long andBitmask, final long immValue)
    { return _addInst( INS_RD_BUS_STR_DB_IM_IM, address, andBitmask, immValue ); }

    public SWDExecInstBuilder rdBusDB(final long address, final long andBitmask, final XVI ofsVarIndex)
    { return _addInst( INS_RD_BUS_STR_DB_IM_VI, address, andBitmask, ofsVarIndex.value() ); }

    public SWDExecInstBuilder rdBusDB(final XVI adrVarIndex, final long andBitmask, final long immValue)
    { return _addInst( INS_RD_BUS_STR_DB_VI_IM, adrVarIndex.value(), andBitmask, immValue ); }

    public SWDExecInstBuilder rdBusDB(final XVI adrVarIndex, final long andBitmask, final XVI ofsVarIndex)
    { return _addInst( INS_RD_BUS_STR_DB_VI_VI, adrVarIndex.value(), andBitmask, ofsVarIndex.value() ); }

    public SWDExecInstBuilder rdBusSB(final long address, final long andBitmask, final long immValue)
    { return _addInst( INS_RD_BUS_STR_SB_IM_IM, address, andBitmask, immValue ); }

    public SWDExecInstBuilder rdBusSB(final long address, final long andBitmask, final XVI ofsVarIndex)
    { return _addInst( INS_RD_BUS_STR_SB_IM_VI, address, andBitmask, ofsVarIndex.value() ); }

    public SWDExecInstBuilder rdBusSB(final XVI adrVarIndex, final long andBitmask, final long immValue)
    { return _addInst( INS_RD_BUS_STR_SB_VI_IM, adrVarIndex.value(), andBitmask, immValue ); }

    public SWDExecInstBuilder rdBusSB(final XVI adrVarIndex, final long andBitmask, final XVI ofsVarIndex)
    { return _addInst( INS_RD_BUS_STR_SB_VI_VI, adrVarIndex.value(), andBitmask, ofsVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder wrRawMemAP(final long ap, final long bank_offset, final long immValue)
    { return _addInst( INS_WR_RAW_MEMAP_IM_IM_IM, ap, bank_offset, immValue ); }

    public SWDExecInstBuilder wrRawMemAP(final long ap, final long bank_offset, final XVI srcVarIndex)
    { return _addInst( INS_WR_RAW_MEMAP_IM_IM_VI, ap, bank_offset, srcVarIndex.value() ); }

    public SWDExecInstBuilder rdRawMemAP(final long ap, final long bank_offset, final XVI dstVarIndex)
    { return _addInst( INS_RD_RAW_MEMAP_STR_IM_IM_VI, ap, bank_offset, dstVarIndex.value() ); }

    // !!! WARNING : DPv3 only !!!
    public SWDExecInstBuilder wrRawMemAP(final long address28_4, final long immValue)
    { return _addInst( INS_WR_RAW_MEMAP3_IM_IM_IM, address28_4, immValue ); }

    // !!! WARNING : DPv3 only !!!
    public SWDExecInstBuilder wrRawMemAP(final long address28_4, final XVI srcVarIndex)
    { return _addInst( INS_WR_RAW_MEMAP3_IM_IM_VI, address28_4, srcVarIndex.value() ); }

    // !!! WARNING : DPv3 only !!!
    public SWDExecInstBuilder rdRawMemAP(final long address28_4, final XVI dstVarIndex)
    { return _addInst( INS_RD_RAW_MEMAP3_STR_IM_IM_VI, address28_4, dstVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Read and store
    public SWDExecInstBuilder rdsBits(final long address, final XVI dstVarIndex)
    { return rdBusStr( address, 0xFFFFFFFFL, dstVarIndex ); }

    public SWDExecInstBuilder rdsBits(final long address, final XVI dstVarIndex, final long errImmValue, final XVI sttVarIndex)
    { return rdBusStr( address, 0xFFFFFFFFL, dstVarIndex, errImmValue, sttVarIndex ); }

    public SWDExecInstBuilder rdsBitsDB(final long address, final XVI ofsVarIndex)
    { return rdBusDB( address, 0xFFFFFFFFL, ofsVarIndex ); }

    public SWDExecInstBuilder rdsBitsSB(final long address, final XVI ofsVarIndex)
    { return rdBusSB( address, 0xFFFFFFFFL, ofsVarIndex ); }

    // Read and discard
    public SWDExecInstBuilder rddBits(final long address)
    { return rdBusStr( address, 0xFFFFFFFFL, XVI.transitory(0) ); }

    // Read and loop while zero/unset (logical OR of the bits in the bitmask)
    public SWDExecInstBuilder rdlBitsWhileZero(final long address, final long bitmask)
    { return rdBusLoopWhileCmpEQ( address, bitmask, 0 ); }

    public SWDExecInstBuilder rdlBitsWhileUnset(final long address, final long bitmask)
    { return rdBusLoopWhileCmpEQ( address, bitmask, 0 ); }

    public SWDExecInstBuilder rdlBitsUntilNotZero(final long address, final long bitmask)
    { return rdBusLoopWhileCmpEQ( address, bitmask, 0 ); }

    public SWDExecInstBuilder rdlBitsUntilSet(final long address, final long bitmask)
    { return rdBusLoopWhileCmpEQ( address, bitmask, 0 ); }

    public SWDExecInstBuilder rdlBitsWhileZeroEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpEQ( address, bitmask, 0, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsWhileUnsetEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpEQ( address, bitmask, 0, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsUntilNotZeroEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpEQ( address, bitmask, 0, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsUntilSetEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpEQ( address, bitmask, 0, timeoutMS ); }

    // Read and loop while not zero/set (logical OR of the bits in the bitmask)
    public SWDExecInstBuilder rdlBitsWhileNotZero(final long address, final long bitmask)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, 0 ); }

    public SWDExecInstBuilder rdlBitsWhileSet(final long address, final long bitmask)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, 0 ); }

    public SWDExecInstBuilder rdlBitsUntilZero(final long address, final long bitmask)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, 0 ); }

    public SWDExecInstBuilder rdlBitsUntilUnset(final long address, final long bitmask)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, 0 ); }

    public SWDExecInstBuilder rdlBitsWhileNotZeroEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, 0, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsWhileSetEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, 0, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsUntilZeroEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, 0, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsUntilUnsetEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, 0, timeoutMS ); }

    // Read and loop while not equal
    public SWDExecInstBuilder rdlBitsWhileNotEqual(final long address, final long bitmask)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, bitmask ); }

    public SWDExecInstBuilder rdlBitsUntilEqual(final long address, final long bitmask)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, bitmask ); }

    public SWDExecInstBuilder rdlBitsWhileNotEqual(final long address, final long bitmask, final long chkBitmask)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, chkBitmask ); }

    public SWDExecInstBuilder rdlBitsUntilEqual(final long address, final long bitmask, final long chkBitmask)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, chkBitmask ); }

    public SWDExecInstBuilder rdlBitsWhileNotEqualEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, bitmask, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsUntilEqualEx(final long address, final long bitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, bitmask, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsWhileNotEqualEx(final long address, final long bitmask, final long chkBitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, chkBitmask, timeoutMS ); }

    public SWDExecInstBuilder rdlBitsUntilEqualEx(final long address, final long bitmask, final long chkBitmask, final long timeoutMS)
    { return rdBusLoopWhileCmpNEQ( address, bitmask, chkBitmask, timeoutMS ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder clrBits(final long address, final long clrBitmask)
    { return mdBus( address, clrBitmask , 0); }

    public SWDExecInstBuilder clrBits(final long address, final XVI clrVarIndex)
    { return mdBus( address, clrVarIndex, 0); }

    public SWDExecInstBuilder clrBits(final XVI address, final long clrBitmask)
    { return mdBus( address, clrBitmask , 0); }

    public SWDExecInstBuilder clrBits(final XVI address, final XVI clrVarIndex)
    { return mdBus( address, clrVarIndex, 0); }

    public SWDExecInstBuilder setBits(final long address, final long setBitmask)
    { return mdBus( address, 0 , setBitmask); }

    public SWDExecInstBuilder setBits(final long address, final XVI setVarIndex)
    { return mdBus( address, 0, setVarIndex); }

    public SWDExecInstBuilder setBits(final XVI address, final long setBitmask)
    { return mdBus( address, 0, setBitmask); }

    public SWDExecInstBuilder setBits(final XVI address, final XVI setVarIndex)
    { return mdBus( address, 0, setVarIndex); }

    public SWDExecInstBuilder modBits(final long address, final long clrBitmask, final XVI setVarIndex)
    { return mdBus( address, clrBitmask , setVarIndex ); }

    public SWDExecInstBuilder modBits(final long address, final long clrBitmask, final long setBitmask)
    { return mdBus( address, clrBitmask , setBitmask  ); }

    public SWDExecInstBuilder modBits(final long address, final XVI clrVarIndex, final XVI setVarIndex)
    { return mdBus( address, clrVarIndex, setVarIndex ); }

    public SWDExecInstBuilder modBits(final long address, final XVI clrVarIndex, final long setBitmask)
    { return mdBus( address, clrVarIndex, setBitmask  ); }

    public SWDExecInstBuilder modBits(final XVI address, final long clrBitmask, final XVI setVarIndex)
    { return mdBus( address, clrBitmask , setVarIndex ); }

    public SWDExecInstBuilder modBits(final XVI address, final long clrBitmask, final long setBitmask)
    { return mdBus( address, clrBitmask , setBitmask  ); }

    public SWDExecInstBuilder modBits(final XVI address, final XVI clrVarIndex, final XVI setVarIndex)
    { return mdBus( address, clrVarIndex, setVarIndex ); }

    public SWDExecInstBuilder modBits(final XVI address, final XVI clrVarIndex, final long setBitmask)
    { return mdBus( address, clrVarIndex, setBitmask  ); }

    public SWDExecInstBuilder wrtBits(final long address, final XVI srcVarIndex)
    { return wrBus( address, srcVarIndex ); }

    public SWDExecInstBuilder wrtBits(final long address, final long immValue)
    { return wrBus( address, immValue    ); }

    public SWDExecInstBuilder wrtBits(final XVI address, final XVI srcVarIndex)
    { return wrBus( address, srcVarIndex ); }

    public SWDExecInstBuilder wrtBits(final XVI address, final long immValue)
    { return wrBus( address, immValue    ); }

    public SWDExecInstBuilder wrtBitsDB(final long address, final XVI ofsVarIndex)
    { return wrBusDB( address, ofsVarIndex ); }

    public SWDExecInstBuilder wrtBitsSB(final long address, final XVI ofsVarIndex)
    { return wrBusSB( address, ofsVarIndex ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder mov(final XVI srcVarIndex, final XVI dstVarIndex)
    { return _addInst( INS_VI_STR_VI, srcVarIndex.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder mov(final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_IM_STR_VI, immValue, dstVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder push(final XVI... srcVarIndex)
    { return _addInst( INS_PUSH_VI, srcVarIndex ); }

    public SWDExecInstBuilder pop(final XVI... dstVarIndex)
    { return _addInst( INS_POP_VI, dstVarIndex ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder strDB(final XVI srcVarIndex, final XVI ofsVarIndex)
    { return _addInst( INS_VI_STR_DB_VI, srcVarIndex.value(), ofsVarIndex.value() ); }

    public SWDExecInstBuilder strDB(final XVI srcVarIndex, final long immVarIndex)
    {
               mov  ( immVarIndex, XVI.transitory(0) );
        return strDB( srcVarIndex, XVI.transitory(0) );
    }

    public SWDExecInstBuilder strDB(final XVI srcVarIndex, final XVI ofsVarIndex, final int ofsVarIndexMod)
    {
                add ( ofsVarIndex, ofsVarIndexMod, XVI.transitory(1) );
        return strDB( srcVarIndex,                 XVI.transitory(1) );
    }

    public SWDExecInstBuilder strDB(final long immValue, final XVI ofsVarIndex)
    { return _addInst( INS_IM_STR_DB_VI, immValue, ofsVarIndex.value() ); }

    public SWDExecInstBuilder strDB(final long immValue, final long immVarIndex)
    {
               mov  ( immVarIndex, XVI.transitory(0) );
        return strDB( immValue   , XVI.transitory(0) );
    }

    public SWDExecInstBuilder strDB(final long immValue, final XVI ofsVarIndex, final int ofsVarIndexMod)
    {
                add ( ofsVarIndex, ofsVarIndexMod  , XVI.transitory(1) );
        return strDB( immValue   , XVI.transitory(1)                   );
    }

    public SWDExecInstBuilder ldrDB(final XVI ofsVarIndex, final XVI dstVarIndex)
    { return _addInst( INS_DB_VI_STR_VI, ofsVarIndex.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder ldrDB(final XVI ofsVarIndex, final int ofsVarIndexMod, final XVI dstVarIndex)
    {
                add ( ofsVarIndex      , ofsVarIndexMod, XVI.transitory(1) );
        return ldrDB( XVI.transitory(1), dstVarIndex                       );
    }

    public SWDExecInstBuilder ldrDB(final long ofsValue, final XVI dstVarIndex)
    { return _addInst( INS_DB_IM_STR_VI, ofsValue, dstVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder strSB(final XVI srcVarIndex, final XVI ofsVarIndex)
    { return _addInst( INS_VI_STR_SB_VI, srcVarIndex.value(), ofsVarIndex.value() ); }

    public SWDExecInstBuilder strSB(final XVI srcVarIndex, final long immVarIndex)
    {
               mov  ( immVarIndex, XVI.transitory(0) );
        return strSB( srcVarIndex, XVI.transitory(0) );
    }

    public SWDExecInstBuilder strSB(final XVI srcVarIndex, final XVI ofsVarIndex, final int ofsVarIndexMod)
    {
                add ( ofsVarIndex, ofsVarIndexMod, XVI.transitory(1) );
        return strSB( srcVarIndex,                 XVI.transitory(1) );
    }

    public SWDExecInstBuilder strSB(final long immValue, final XVI ofsVarIndex)
    { return _addInst( INS_IM_STR_SB_VI, immValue, ofsVarIndex.value() ); }

    public SWDExecInstBuilder strSB(final long immValue, final long immVarIndex)
    {
               mov  ( immVarIndex, XVI.transitory(0) );
        return strSB( immValue   , XVI.transitory(0) );
    }

    public SWDExecInstBuilder strSB(final long immValue, final XVI ofsVarIndex, final int ofsVarIndexMod)
    {
                add ( ofsVarIndex, ofsVarIndexMod  , XVI.transitory(1) );
        return strSB( immValue   , XVI.transitory(1)                   );
    }

    public SWDExecInstBuilder ldrSB(final XVI ofsVarIndex, final XVI dstVarIndex)
    { return _addInst( INS_SB_VI_STR_VI, ofsVarIndex.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder ldrSB(final XVI ofsVarIndex, final int ofsVarIndexMod, final XVI dstVarIndex)
    {
                add ( ofsVarIndex      , ofsVarIndexMod, XVI.transitory(1) );
        return ldrSB( XVI.transitory(1), dstVarIndex                       );
    }

    public SWDExecInstBuilder ldrSB(final long ofsValue, final XVI dstVarIndex)
    { return _addInst( INS_SB_IM_STR_VI, ofsValue , dstVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder packDB_4X8_32()
    { return _addInst( INS_PACK_DB_4X8_32 ); }

    public SWDExecInstBuilder unpackDB_32_4X8()
    { return _addInst( INS_UNPACK_DB_32_4X8 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder pushDB()
    { return _addInst( INS_PUSH_DB ); }

    public SWDExecInstBuilder popDB()
    { return _addInst( INS_POP_DB ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder add(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_ADD_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder add(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_ADD_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    public SWDExecInstBuilder sub(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_SUB_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder sub(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_SUB_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    public SWDExecInstBuilder mul(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_MUL_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder mul(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_MUL_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    public SWDExecInstBuilder div(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_DIV_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder div(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_DIV_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    public SWDExecInstBuilder mod(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_MOD_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder mod(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_MOD_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder inc1(final XVI varIndex)
    { return add( varIndex, 1, varIndex ); }

    public SWDExecInstBuilder inc2(final XVI varIndex)
    { return add( varIndex, 2, varIndex ); }

    public SWDExecInstBuilder inc4(final XVI varIndex)
    { return add( varIndex, 4, varIndex ); }

    public SWDExecInstBuilder inc8(final XVI varIndex)
    { return add( varIndex, 8, varIndex ); }

    public SWDExecInstBuilder dec1(final XVI varIndex)
    { return sub( varIndex, 1, varIndex ); }

    public SWDExecInstBuilder dec2(final XVI varIndex)
    { return sub( varIndex, 2, varIndex ); }

    public SWDExecInstBuilder dec4(final XVI varIndex)
    { return sub( varIndex, 4, varIndex ); }

    public SWDExecInstBuilder dec8(final XVI varIndex)
    { return sub( varIndex, 8, varIndex ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder bwNOT(final XVI srcVarIndex, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_NOT_VI, srcVarIndex.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder bwAND(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_AND_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder bwAND(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_AND_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    public SWDExecInstBuilder bwOR(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_OR_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder bwOR(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_OR_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    public SWDExecInstBuilder bwXOR(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_XOR_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder bwXOR(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_XOR_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    public SWDExecInstBuilder bwLSH(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_LSH_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder bwLSH(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_LSH_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    public SWDExecInstBuilder bwRSH(final XVI srcVarIndex1, final XVI srcVarIndex2, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_RSH_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), dstVarIndex.value() ); }

    public SWDExecInstBuilder bwRSH(final XVI srcVarIndex, final long immValue, final XVI dstVarIndex)
    { return _addInst( INS_VI_BW_RSH_VI_IM, srcVarIndex.value(), immValue, dstVarIndex.value() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder errIfCmpEQ(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_ERR_CMP_EQ_VI_IM, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder errIfCmpEQ(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_ERR_CMP_EQ_VI_VI, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder errIfCmpNEQ(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_ERR_CMP_NEQ_VI_IM, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder errIfCmpNEQ(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_ERR_CMP_NEQ_VI_VI, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder errIfCmpGT(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_ERR_CMP_GT_VI_IM, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder errIfCmpGT(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_ERR_CMP_GT_VI_VI, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder errIfCmpGT_tout(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_ERR_CMP_GT_VI_IM_TOUT, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder errIfCmpGT_tout(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_ERR_CMP_GT_VI_VI_TOUT, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder errIfCmpGTE(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_ERR_CMP_GTE_VI_IM, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder errIfCmpGTE(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_ERR_CMP_GTE_VI_VI, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder errIfCmpLT(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_ERR_CMP_LT_VI_IM, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder errIfCmpLT(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_ERR_CMP_LT_VI_VI, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder errIfCmpLTE(final XVI chkVarIndex, final long immValue)
    { return _addInst( INS_ERR_CMP_LTE_VI_IM, chkVarIndex.value(), immValue ); }

    public SWDExecInstBuilder errIfCmpLTE(final XVI chkVarIndex, final XVI refVarIndex)
    { return _addInst( INS_ERR_CMP_LTE_VI_VI, chkVarIndex.value(), refVarIndex.value() ); }

    public SWDExecInstBuilder errIfZero(final XVI chkVarIndex)
    { return errIfCmpEQ( chkVarIndex, 0 ); }

    public SWDExecInstBuilder errIfNotZero(final XVI chkVarIndex)
    { return errIfCmpNEQ( chkVarIndex, 0 ); }

    public SWDExecInstBuilder errIfLTZero(final XVI chkVarIndex)
    { return errIfCmpLT( chkVarIndex, 0 ); }

    public SWDExecInstBuilder errIfLTEZero(final XVI chkVarIndex)
    { return errIfCmpLTE( chkVarIndex, 0 ); }

    public SWDExecInstBuilder errIfGTZero(final XVI chkVarIndex)
    { return errIfCmpGT( chkVarIndex, 0 ); }

    public SWDExecInstBuilder errIfGTEZero(final XVI chkVarIndex)
    { return errIfCmpGTE( chkVarIndex, 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder jmp(final String dstLabel)
    { return _addInst( INS_J, _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfEQ(final XVI srcVarIndex1, final XVI srcVarIndex2, final String dstLabel)
    { return _addInst( INS_J_CMP_EQ_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfEQ(final XVI srcVarIndex, final long immValue, final String dstLabel)
    { return _addInst( INS_J_CMP_EQ_VI_IM, srcVarIndex.value(), immValue, _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfNEQ(final XVI srcVarIndex1, final XVI srcVarIndex2, final String dstLabel)
    { return _addInst( INS_J_CMP_NEQ_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfNEQ(final XVI srcVarIndex, final long immValue, final String dstLabel)
    { return _addInst( INS_J_CMP_NEQ_VI_IM, srcVarIndex.value(), immValue, _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfGT(final XVI srcVarIndex1, final XVI srcVarIndex2, final String dstLabel)
    { return _addInst( INS_J_CMP_GT_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfGT(final XVI srcVarIndex, final long immValue, final String dstLabel)
    { return _addInst( INS_J_CMP_GT_VI_IM, srcVarIndex.value(), immValue, _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfGTE(final XVI srcVarIndex1, final XVI srcVarIndex2, final String dstLabel)
    { return _addInst( INS_J_CMP_GTE_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfGTE(final XVI srcVarIndex, final long immValue, final String dstLabel)
    { return _addInst( INS_J_CMP_GTE_VI_IM, srcVarIndex.value(), immValue, _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfLT(final XVI srcVarIndex1, final XVI srcVarIndex2, final String dstLabel)
    { return _addInst( INS_J_CMP_LT_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfLT(final XVI srcVarIndex, final long immValue, final String dstLabel)
    { return _addInst( INS_J_CMP_LT_VI_IM, srcVarIndex.value(), immValue, _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfLTE(final XVI srcVarIndex1, final XVI srcVarIndex2, final String dstLabel)
    { return _addInst( INS_J_CMP_LTE_VI_VI, srcVarIndex1.value(), srcVarIndex2.value(), _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder jmpIfLTE(final XVI srcVarIndex, final long immValue, final String dstLabel)
    { return _addInst( INS_J_CMP_LTE_VI_IM, srcVarIndex.value(), immValue, _addJumpTarget(dstLabel) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder jmpIfZero(final XVI srcVarIndex, final String dstLabel)
    { return jmpIfEQ( srcVarIndex, 0, dstLabel ); }

    public SWDExecInstBuilder jmpIfNotZero(final XVI srcVarIndex, final String dstLabel)
    { return jmpIfNEQ( srcVarIndex, 0, dstLabel ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder call(final String dstLabel)
    { return _addInst( INS_CALL, _addJumpTarget(dstLabel) ); }

    public SWDExecInstBuilder ret()
    { return _addInst( INS_RETURN ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # The 'lpStore()' and 'lpLinkAndStore()' instructions will internally generate 'mov()' instructions.
     *        # It means they must be executed before any other instructions that use the program.
     *        # Please pay attention to where the 'link()' call will be made and where the resulting linked instruction
     *          sequence will actually be executed. It is recommended to put the linked instruction sequence containing
     *          the calls to these two functions as part of 'Specifier.instruction_InitializeSystemOnce'
     */

    // Store program
    public SWDExecInstBuilder lpStore(final SWDExecInst swdExecInst, final long[] loaderProgram, final XVI dstVarIndex)
    { return mov( swdExecInst._lpPut(loaderProgram), dstVarIndex ); }

    // Link and store program
    public SWDExecInstBuilder lpLinkAndStore(final SWDExecInst swdExecInst, final ARMCortexMThumb __ASM__, final long originAddress, final boolean dumpDisassemblyAndArray, final XVI dstVarIndex) throws JXMAsmError
    { return lpStore( swdExecInst, __ASM__.link(originAddress, dumpDisassemblyAndArray), dstVarIndex ); }

    // Load program and set VTOR & XPSR
    public SWDExecInstBuilder lpLoad(final XVI lpiVarIndex, final XVI apsVarIndex, final long armVTORAddress)
    { return _addInst( INS_LP_LOAD_VI_VI, lpiVarIndex.value(), apsVarIndex.value(), armVTORAddress ); }

    // Load program and set VTOR & XPSR
    public SWDExecInstBuilder lpLoad(final XVI lpiVarIndex, final XVI apsVarIndex)
    { return lpLoad( lpiVarIndex, apsVarIndex, 0xE000ED08L ); }

    // Load program and set VTOR & XPSR
    public SWDExecInstBuilder lpLoad(final XVI lpiVarIndex, final long addrProgStart, final long armVTORAddress)
    {
               mov   ( addrProgStart, XVI.transitory(0)                 );
        return lpLoad( lpiVarIndex  , XVI.transitory(0), armVTORAddress );
    }

    // Load program and set VTOR & XPSR
    public SWDExecInstBuilder lpLoad(final XVI lpiVarIndex, final long addrProgStart)
    { return lpLoad( lpiVarIndex, addrProgStart, 0xE000ED08L ); }

    // Execute program (set PC & SP and unhalt core with debugging enabled)
    public SWDExecInstBuilder lpExecute()
    { return _addInst( INS_LP_EXECUTE ); }

    // Continue program (either after manual core halt or a BKPT instruction)
    public SWDExecInstBuilder lpContinue()
    { return _addInst( INS_LP_CONTINUE ); }

    // Wait until core is halted by a BKPT instruction or other means
    public SWDExecInstBuilder lpWaitBKPT(final long timeoutMS)
    {
        final String lblWaitLoop  = uniqueLabelName("lpWaitBKPT_wait_loop_%=");
        final String lblWaitEnd   = uniqueLabelName("lpWaitBKPT_wait_end_%=" );

        final XVI    xviBeginTime = XVI.transitory(0);
        final XVI    xviCheckTime = XVI.transitory(1);
        final XVI    xviReadState = XVI.transitory(2);

        // Wait (with timeout) until the core is no longer running
        getMS                ( xviBeginTime                             );
        label                ( lblWaitLoop                              );
            getCore_IsRunning( xviReadState                             );
            jmpIfZero        ( xviReadState, lblWaitEnd                 );
            getMS            ( xviCheckTime                             );
            sub              ( xviCheckTime, xviBeginTime, xviCheckTime );
            errIfCmpGT       ( xviCheckTime, timeoutMS                  );
            jmp              ( lblWaitLoop                              );
        label                ( lblWaitEnd                               );

        // Error if the core is locked-up
        getCore_S_LOCKUP     ( xviReadState                             );
        errIfNotZero         ( xviReadState                             );

        // Error if the core is sleeping
        getCore_S_SLEEP      ( xviReadState                             );
        errIfNotZero         ( xviReadState                             );

        // Error if the core is not halted
        getCore_S_HALT       ( xviReadState                             );
        errIfZero            ( xviReadState                             );

        return this;
    }

    // Wait until core is halted by a BKPT instruction or other means
    public SWDExecInstBuilder lpWaitBKPT()
    { return lpWaitBKPT( SWDExecInst.DEF_WHILE_TIMEOUT_MS ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /* ##### !!! TODO !!! #####
     * Add pseudo instructions that combine:
     *     'rdBusStr(...)' and 'rdsBits(...)'
     * and
     *     'debugPrintln*(...)'
     */

    public SWDExecInstBuilder debugPrintln()
    { return _addInst( INS_DEBUG_PRINTLN ); }

    public SWDExecInstBuilder debugPrintlnSDecNN(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_SDEC_NN, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnSDec03(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_SDEC_03, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnSDec05(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_SDEC_05, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnSDec08(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_SDEC_08, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnSDec10(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_SDEC_10, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUDecNN(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UDEC_NN, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUDec03(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UDEC_03, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUDec05(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UDEC_05, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUDec08(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UDEC_08, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUDec10(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UDEC_10, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUBinNN(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UBIN_NN, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUBin08(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UBIN_08, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUBin16(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UBIN_16, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUBin24(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UBIN_24, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUBin32(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UBIN_32, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUOctNN(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UOCT_NN, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUOct03(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UOCT_03, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUOct06(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UOCT_06, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUOct08(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UOCT_08, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUOct11(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UOCT_11, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUHexNN(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UHEX_NN, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUHex02(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UHEX_02, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUHex04(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UHEX_04, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUHex06(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UHEX_06, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnUHex08(final XVI srcVarIndex)
    { return _addInst( INS_DEBUG_PRINTLN_UHEX_08, srcVarIndex.value() ); }

    public SWDExecInstBuilder debugPrintlnSDecNN(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnSDecNN(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnSDec03(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnSDec03(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnSDec05(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnSDec05(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnSDec08(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnSDec08(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnSDec10(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnSDec10(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUDecNN(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUDecNN(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUDec03(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUDec03(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUDec05(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUDec05(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUDec08(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUDec08(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUDec10(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUDec10(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUOctNN(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUOctNN(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUOct03(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUOct03(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUOct06(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUOct06(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUOct08(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUOct08(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUOct11(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUOct11(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUHexNN(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUHexNN(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUHex02(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUHex02(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUHex04(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUHex04(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUHex06(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUHex06(           XVI.transitory(0) );
    }

    public SWDExecInstBuilder debugPrintlnUHex08(final long immValue)
    {
               mov               ( immValue, XVI.transitory(0) );
        return debugPrintlnUHex08(           XVI.transitory(0) );
    }

    // WARNING : This instruction incurs a lot of overhead!
    public SWDExecInstBuilder debugPrintf(final SWDExecInst swdExecInst, final String format, final Object... args)
    {
        final long[] params = new long[1 + 1 + 1 + args.length];
              int    parIdx = 0;

        params[parIdx++] = INS_DEBUG_PRINTF;
        params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_STRING, format);

        for(final Object obj : args) {
                 if( obj instanceof XVI       ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_XVI   ,                             obj   );
            else if( obj instanceof String    ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_STRING,                             obj   );
            else if( obj instanceof Character ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_NUMBER, Character.valueOf( (char  ) obj ) );
            else if( obj instanceof Byte      ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_NUMBER, Byte     .valueOf( (byte  ) obj ) );
            else if( obj instanceof Short     ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_NUMBER, Short    .valueOf( (short ) obj ) );
            else if( obj instanceof Integer   ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_NUMBER, Integer  .valueOf( (int   ) obj ) );
            else if( obj instanceof Long      ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_NUMBER, Long     .valueOf( (long  ) obj ) );
            else if( obj instanceof Float     ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_NUMBER, Float    .valueOf( (float ) obj ) );
            else if( obj instanceof Double    ) params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_NUMBER, Double   .valueOf( (double) obj ) );
            else                                params[parIdx++] = swdExecInst._printfPutArg(SWDExecInst.ARG_MAP_TYPE_NULL  , null                              );
        }

        return _addInst(params);
    }

    public SWDExecInstBuilder debugPrintfln(final SWDExecInst swdExecInst, final String format, final Object... args)
    { return debugPrintf(swdExecInst, format + '\n', args); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SWDExecInstBuilder macro_mtRdWrFlsEprPrefix(final long baseFlashEEPROMAddress, final XVI xviFlashEEPROMAddress, final XVI xviSignalWorkerCommand, final XVI xviSignalJobState)
    {
            label          ( "_mt_rwfe_start"                                                              );
                // Tell the main thread that the job is in progress
                mov        ( XVI_SIGNAL_JOB_IN_PROGRESS, xviSignalJobState                                 );
                // Wait for the EXIT or EXECUTE command
                jmpIfEQ    ( xviSignalWorkerCommand    , XVI_SIGNAL_WORKER_EXIT   , "_mt_rwfe_exit"        );
                jmpIfEQ    ( xviSignalWorkerCommand    , XVI_SIGNAL_WORKER_EXECUTE, "_mt_rwfe_execute"     );
            label          ( "_mt_rwfe_yield"                                                              );
                threadYield(                                                                               );
            jmp            ( "_mt_rwfe_start"                                                              );
            label          ( "_mt_rwfe_execute"                                                            );

        if(baseFlashEEPROMAddress > 0) {
                // Fix the address as needed
                jmpIfLT    ( xviFlashEEPROMAddress     , baseFlashEEPROMAddress   , "_mt_rwfe_addr_ok"     );
                sub        ( xviFlashEEPROMAddress     , baseFlashEEPROMAddress   ,  xviFlashEEPROMAddress );
            label          ( "_mt_rwfe_addr_ok"                                                            );
        }

        return this;
    }

    public SWDExecInstBuilder macro_mtRdWrFlsEprSuffix(final long baseFlashEEPROMAddress, final XVI xviFlashAddress, final XVI xviSignalWorkerCommand, final XVI xviSignalJobState)
    {
                // Tell the main thread that the job is complete
                mov        ( XVI_SIGNAL_JOB_COMPLETE   , xviSignalJobState                                 );
                // Wait for the EXIT or WAIT command
            label          ( "_mt_rwfe_wait"                                                               );
                jmpIfEQ    ( xviSignalWorkerCommand    , XVI_SIGNAL_WORKER_EXIT   , "_mt_rwfe_exit"        );
                jmpIfNEQ   ( xviSignalWorkerCommand    , XVI_SIGNAL_WORKER_WAIT   , "_mt_rwfe_wait"        );
                // Loop back to the top
            jmp            ( "_mt_rwfe_yield"                                                              );
            label          ( "_mt_rwfe_exit"                                                               );

        return this;
    }

} // class SWDExecInstBuilder
