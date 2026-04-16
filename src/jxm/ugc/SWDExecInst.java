/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


public class SWDExecInst extends SWDExecInstOpcode {

    private final String          ProgClassName;

    private final ProgSWDLowLevel _prog;
    private       long[]          _swdExecVar = new long[INS_EXEC_VAR_BUFF_SIZE_TOTAL];

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @package_private static final long  ARG_MAP_TYPE_NULL    = 0x00000000L;
    @package_private static final long  ARG_MAP_TYPE_XVI     = 0x01000000L;
    @package_private static final long  ARG_MAP_TYPE_STRING  = 0x02000000L;
    @package_private static final long  ARG_MAP_TYPE_NUMBER  = 0x03000000L;

    public           static final long  DEF_WHILE_TIMEOUT_MS = 25 * 60 * 1000; // 25 minutes

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private       long                    _whileTimeoutMS   = DEF_WHILE_TIMEOUT_MS;

    private final Stack    <Integer     > _indexStack       = new Stack<>();
    private final Stack    <Long        > _xviStack         = new Stack<>();
    private final Stack    <int[]       > _dbStack          = new Stack<>();

    private final int[]                   _scratchBuff      = new int[8 * 1024 * 1024]; // Scratch buffer

    private       int                     _printfArgMapCtr  = 0;
    private final HashMap  <Long, Object> _printfArgMap     = new HashMap<>();

    private final ArrayList<long[]      > _loaderProgram    = new ArrayList<>();
    private       long[]                  _curLoaderProgram = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ProgSWD _progSWD()
    { return (ProgSWD) _prog; }

    public void _printfClearAllArgs()
    {
        _printfArgMapCtr = 0;
        _printfArgMap.clear();
    }

    @package_private
    long _printfPutArg(final long type, final Object obj)
    {
        // ##### !!! TODO : Optimize it by checking if the same object is already in the map !!! #####

        final long curIdx = type | (_printfArgMapCtr++);

        _printfArgMap.put(curIdx, obj);

        return curIdx;
    }

    @package_private
    Object _printfGetArg(final long argIndex)
    { return _printfArgMap.get(argIndex); }

    @package_private
    int _lpPut(final long[] loaderProgram)
    {
        _loaderProgram.add(loaderProgram);
        return _loaderProgram.size() - 1;
    }

    @package_private
    long[] _lpGet(final long index)
    { return _loaderProgram.get( (int) index ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @package_private
    SWDExecInst(final String progClassName, final ProgSWDLowLevel progSWD)
    {
        ProgClassName = progClassName;
        _prog         = progSWD;
    }

    @package_private synchronized long _xviGet(final int  xviIndex                     ) { return _swdExecVar[       xviIndex ];     }
    @package_private synchronized long _xviGet(final long xviIndex                     ) { return _swdExecVar[ (int) xviIndex ];     }
    @package_private synchronized void _xviSet(final int  xviIndex, final long xviValue) { _swdExecVar[       xviIndex ] = xviValue; }
    @package_private synchronized void _xviSet(final long xviIndex, final long xviValue) { _swdExecVar[ (int) xviIndex ] = xviValue; }

    @package_private
    long _exec(final long[][] instructions) throws USB2GPIO.TansmitError
    { return _exec(instructions, null); }

    @package_private
    long _exec(final long[][] instructions, final int[] dataBuff) throws USB2GPIO.TansmitError
    {
        /*
         * NOTE : This function is reentrant (therefore it can be used in a multi-threading environment) as long as
         *        'instruction' and 'dataBuff' arguments refer to different object instances.
         */

        _indexStack.clear();
        _xviStack  .clear();
        _dbStack   .clear();

        _curLoaderProgram = null;

        for(int idx = 0; idx < instructions.length; ++idx) {

            final long[] instruction = instructions[idx];
            final int    instCode    = (int) instruction[0];

            switch(instCode) {

                case INS_NOP : /* This instruction does nothing */ break;

                case INS_GET_SWD_FREQUENCY_VI : _xviSet( instruction[1], _prog._swdClockFrequency ); break;
                case INS_SET_SWD_FREQUENCY_VI : if(true) {
                        final int spiClkFrq = (int) _xviGet( instruction[1] );
                        if(spiClkFrq != _prog._swdClockFrequency) {
                            final int spiClkDiv = _prog._usb2gpio.spiClkFreqToClkDiv(spiClkFrq);
                            if( _prog._usb2gpio.spiSetClkDiv(spiClkDiv) ) {
                                _prog._swdClockFrequency = _prog._usb2gpio.spiGetSupportedClkFreqs()[spiClkDiv];
                              //SysUtil.stdDbg().printf("### %d => %d (%d)\n", spiClkFrq, spiClkDiv, _prog._swdClockFrequency);
                            }
                        }
                    }
                    break;

                case INS_SWD_LINE_RESET         : _prog.swdLineReset(); break;

                case INS_GET_CORE_S_LOCKUP_VI   : _xviSet( instruction[1], _prog._swd_core_S_LOCKUP() ? 1 : 0 ); break;
                case INS_GET_CORE_S_SLEEP_VI    : _xviSet( instruction[1], _prog._swd_core_S_SLEEP () ? 1 : 0 ); break;
                case INS_GET_CORE_S_HALT_VI     : _xviSet( instruction[1], _prog._swd_core_S_HALT  () ? 1 : 0 ); break;
                case INS_GET_CORE_IS_RUNNING_VI : _xviSet( instruction[1], _prog._swd_coreIsRunning() ? 1 : 0 ); break;

                case INS_GET_DP_VERSION_VI      : _xviSet( instruction[1], _prog._swd_dpVersion() ); break;

                case INS_EXIT_RET_VI : return _xviGet( instruction[1] );
                case INS_EXIT_RET_IM : return          instruction[1]  ;
                case INS_EXIT_ERR_VI : USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_ExecInst, ProgClassName, _xviGet( instruction[1] ) ); break;
                case INS_EXIT_ERR_IM : USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_ExecInst, ProgClassName,          instruction[1]   ); break;

                case INS_EXIT_RET_IF_CMP_EQ_VI_VI  : /* FALLTHROUGH */
                case INS_EXIT_RET_IF_CMP_EQ_VI_IM  : /* FALLTHROUGH */
                case INS_EXIT_RET_IF_CMP_NEQ_VI_VI : /* FALLTHROUGH */
                case INS_EXIT_RET_IF_CMP_NEQ_VI_IM : if(true) {
                        final long    chk =                                                                                           _xviGet( instruction[1] );
                        final long    ref = (instCode == INS_EXIT_RET_IF_CMP_EQ_VI_VI || instCode == INS_EXIT_RET_IF_CMP_NEQ_VI_VI) ? _xviGet( instruction[2] ) : instruction[2];
                        final boolean res = (chk == ref);
                        if(instCode == INS_EXIT_RET_IF_CMP_EQ_VI_VI || instCode == INS_EXIT_RET_IF_CMP_EQ_VI_IM) {
                            if( res) return chk;
                        }
                        else {
                            if(!res) return chk;
                        }
                    }
                    break;

                case INS_DELAY_US_IM  : SysUtil.sleepUS( instruction[1] ); break;
                case INS_DELAY_MS_IM  : SysUtil.sleepMS( instruction[1] ); break;
                case INS_THREAD_YIELD : Thread.yield()                   ; break;

                case INS_GET_US_VI    : _xviSet( instruction[1], SysUtil.getUS() ); break;
                case INS_GET_MS_VI    : _xviSet( instruction[1], SysUtil.getMS() ); break;

                case INS_SET_WHILE_TIMEOUT_MS_VI : /* FALLTHROUGH */
                case INS_SET_WHILE_TIMEOUT_MS_IM : if(true) {
                        final long value = (instCode == INS_SET_WHILE_TIMEOUT_MS_VI) ? _xviGet( instruction[1] ) : instruction[1];
                         _whileTimeoutMS = (value >= 0) ? value : DEF_WHILE_TIMEOUT_MS;
                    }
                    break;

                case INS_HALT_CORE_IM              : _prog._swd_haltCore  ( instruction[1] != 0                      ); break;
                case INS_UNHALT_CORE_IM_IM         : _prog._swd_unhaltCore( instruction[1] != 0, instruction[2] != 0 ); break;

                case INS_HALT_ALL_CORES            : _prog._swd_haltAllCores          (); break;
                case INS_RESET_UNHALT_ALL_CORES    : _prog._swd_resetAndUnhaltAllCores(); break;

                case INS_WR_CREG_IM_VI             : _prog._swdWrCoreReg( (int) instruction[1], _xviGet( instruction[2] ) ); break;
                case INS_WR_CREG_IM_IM             : _prog._swdWrCoreReg( (int) instruction[1],          instruction[2]   ); break;

                case INS_RD_CREG_VI                : _xviSet( instruction[2], _prog._swdRdCoreReg( (int) instruction[1] ) ); break;

                case INS_RD_CREG_ERR_CMP_EQ_IM_VI  : /* FALLTHROUGH */
                case INS_RD_CREG_ERR_CMP_NEQ_IM_VI : if(true) {
                        final long    value  = _prog._swdRdCoreReg( (int) instruction[1] ) & instruction[2];
                        final boolean cmpRes = ( instCode == INS_RD_CREG_ERR_CMP_EQ_IM_VI )
                                             ? ( value    == _xviGet( instruction[3] )    )
                                             : ( value    != _xviGet( instruction[3] )    );
                        if(cmpRes) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_RdRegVal, ProgClassName, value, instruction[1] );
                    }
                    break;

                case INS_WR_CMEM_VI_VI     : /* FALLTHROUGH */
                case INS_WR_CMEM_VI_IM     : /* FALLTHROUGH */
                case INS_WR_CMEM_IM_VI     : /* FALLTHROUGH */
                case INS_WR_CMEM_IM_IM     : /* FALLTHROUGH */
                case INS_RD_CMEM_VI_VI     : /* FALLTHROUGH */
                case INS_RD_CMEM_VI_IM     : /* FALLTHROUGH */
                case INS_RD_CMEM_IM_VI     : /* FALLTHROUGH */
                case INS_RD_CMEM_IM_IM     : if(true) {
                        final long address =         (instCode == INS_WR_CMEM_VI_VI || instCode == INS_WR_CMEM_VI_IM || instCode == INS_RD_CMEM_VI_VI || instCode == INS_RD_CMEM_VI_IM) ? _xviGet( instruction[1] ) : instruction[1]  ;
                        final int  count   = (int) ( (instCode == INS_WR_CMEM_VI_VI || instCode == INS_WR_CMEM_IM_VI || instCode == INS_RD_CMEM_VI_VI || instCode == INS_RD_CMEM_IM_VI) ? _xviGet( instruction[2] ) : instruction[2] );
                        final int  trfSize = (int) instruction[3];
                        if(instCode == INS_WR_CMEM_VI_VI || instCode == INS_WR_CMEM_VI_IM ||
                           instCode == INS_WR_CMEM_IM_VI || instCode == INS_WR_CMEM_IM_IM
                        ) {
                            _prog._swdWrCoreMem(address, dataBuff, count, trfSize);
                        }
                        else {
                            _prog._swdRdCoreMem(address, dataBuff, count, trfSize);
                        }
                    }
                    break;

                case INS_WR_BUS_IM_VI             : _prog._swdWrBus(          instruction[1]  , _xviGet( instruction[2] ) ); break;
                case INS_WR_BUS_IM_IM             : _prog._swdWrBus(          instruction[1]  ,          instruction[2]   ); break;
                case INS_WR_BUS_VI_VI             : _prog._swdWrBus( _xviGet( instruction[1] ), _xviGet( instruction[2] ) ); break;
                case INS_WR_BUS_VI_IM             : _prog._swdWrBus( _xviGet( instruction[1] ),          instruction[2]   ); break;

                case INS_SET_WR_BUS16             : if( !_prog._swd_MemAP_016bit() ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_InsSMAPDZ, ProgClassName, instCode, 16); break;
                case INS_SET_WR_BUS32             : if( !_prog._swd_MemAP_032bit() ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_InsSMAPDZ, ProgClassName, instCode, 32); break;

                case INS_WR_BUS16X1_IM_VI         : _prog._swdWrBus ( instruction[1], _xviGet( instruction[2] ) ); break;
                case INS_WR_BUS16X2_IM_VI         : _prog._swdWrBus2( instruction[1], _xviGet( instruction[2] ) ); break;

                case INS_RD_BUS_RET_VAL_IM        : return   ( _prog._swdRdBus( instruction[1] ) & instruction[2] )                                       ;
                case INS_RD_BUS_RET_CMP_EQ_IM_VI  : return ( ( _prog._swdRdBus( instruction[1] ) & instruction[2] ) == _xviGet( instruction[3] ) ) ? 1 : 0;
                case INS_RD_BUS_RET_CMP_EQ_IM_IM  : return ( ( _prog._swdRdBus( instruction[1] ) & instruction[2] ) ==          instruction[3]   ) ? 1 : 0;
                case INS_RD_BUS_RET_CMP_NEQ_IM_VI : return ( ( _prog._swdRdBus( instruction[1] ) & instruction[2] ) != _xviGet( instruction[3] ) ) ? 1 : 0;
                case INS_RD_BUS_RET_CMP_NEQ_IM_IM : return ( ( _prog._swdRdBus( instruction[1] ) & instruction[2] ) !=          instruction[3]   ) ? 1 : 0;

                case INS_RD_BUS_WHILE_EQ_IM_VI    : /* FALLTHROUGH */
                case INS_RD_BUS_WHILE_EQ_IM_IM    : /* FALLTHROUGH */
                case INS_RD_BUS_WHILE_NEQ_IM_VI   : /* FALLTHROUGH */
                case INS_RD_BUS_WHILE_NEQ_IM_IM   : if(true) {
                        final long    address = instruction[1];
                        final long    bitmask = instruction[2];
                        final long    refVal  = ( instCode == INS_RD_BUS_WHILE_EQ_IM_VI || instCode == INS_RD_BUS_WHILE_NEQ_IM_VI ) ? _xviGet( instruction[3] ) : instruction[3];
                        final boolean cmpEQ   = ( instCode == INS_RD_BUS_WHILE_EQ_IM_VI || instCode == INS_RD_BUS_WHILE_EQ_IM_IM  );
                        final long    begMS   = SysUtil.getMS();
                        while(true) {
                             final long    chkVal = _prog._swdRdBus(address) & bitmask;
                             final boolean cmpRes = (refVal == chkVal);
                             if( (cmpEQ && !cmpRes) || (!cmpEQ && cmpRes) ) break;
                             if( SysUtil.getMS() - begMS > _whileTimeoutMS) {
                                 USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_InsTOut, ProgClassName, instCode, _whileTimeoutMS );
                             }
                        }
                    }
                    break;

                case INS_RD_BUS_ERR_CMP_EQ_IM_VI  : /* FALLTHROUGH */
                case INS_RD_BUS_ERR_CMP_NEQ_IM_VI : if(true) {
                        final long    value  = _prog._swdRdBus( instruction[1] ) & instruction[2];
                        final boolean cmpRes = ( instCode == INS_RD_BUS_ERR_CMP_EQ_IM_VI )
                                             ? ( value    == _xviGet( instruction[3] )   )
                                             : ( value    != _xviGet( instruction[3] )   );
                        if(cmpRes) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_RdMemVal, ProgClassName, value, instruction[1] );
                    }
                    break;

                case INS_RD_BUS_ERR_CMP_EQ_IM_IM  : /* FALLTHROUGH */
                case INS_RD_BUS_ERR_CMP_NEQ_IM_IM : if(true) {
                        final long    value  = _prog._swdRdBus( instruction[1] ) & instruction[2];
                        final boolean cmpRes = ( instCode == INS_RD_BUS_ERR_CMP_EQ_IM_IM )
                                             ? ( value    == instruction[3]              )
                                             : ( value    != instruction[3]              );
                        if(cmpRes) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_RdMemVal, ProgClassName, value, instruction[1] );
                    }
                    break;

                case INS_RD_BUS_STR_IM_VI      : /* FALLTHROUGH */
                case INS_RD_BUS_STR_H16_IM_VI  : /* FALLTHROUGH */
                case INS_RD_BUS_STR_L16_IM_VI  : /* FALLTHROUGH */
                case INS_RD_BUS_STR_HL16_IM_VI : if(true) {
                        final long value = _prog._swdRdBus( instruction[1] ) & instruction[2];
                             if(instCode == INS_RD_BUS_STR_IM_VI     )   _xviSet( instruction[3],  value                     );
                        else if(instCode == INS_RD_BUS_STR_H16_IM_VI )   _xviSet( instruction[3], (value >> 16) & 0x0000FFFFL);
                        else if(instCode == INS_RD_BUS_STR_L16_IM_VI )   _xviSet( instruction[3],  value        & 0x0000FFFFL);
                        else if(instCode == INS_RD_BUS_STR_HL16_IM_VI) { _xviSet( instruction[3], (value >> 16) & 0x0000FFFFL);
                                                                         _xviSet( instruction[4],  value        & 0x0000FFFFL);
                                                                       }
                    }
                    break;

                case INS_RD_BUS_STR_VI_VI      : /* FALLTHROUGH */
                case INS_RD_BUS_STR_H16_VI_VI  : /* FALLTHROUGH */
                case INS_RD_BUS_STR_L16_VI_VI  : /* FALLTHROUGH */
                case INS_RD_BUS_STR_HL16_VI_VI : if(true) {
                        final long value = _prog._swdRdBus( _xviGet( instruction[1] ) ) & instruction[2];
                             if(instCode == INS_RD_BUS_STR_VI_VI     )   _xviSet( instruction[3],  value                     );
                        else if(instCode == INS_RD_BUS_STR_H16_VI_VI )   _xviSet( instruction[3], (value >> 16) & 0x0000FFFFL);
                        else if(instCode == INS_RD_BUS_STR_L16_VI_VI )   _xviSet( instruction[3],  value        & 0x0000FFFFL);
                        else if(instCode == INS_RD_BUS_STR_HL16_VI_VI) { _xviSet( instruction[3], (value >> 16) & 0x0000FFFFL);
                                                                         _xviSet( instruction[4],  value        & 0x0000FFFFL);
                                                                       }
                    }
                    break;

                case INS_RD_BUS_16_STR_IM_VI : /* FALLTHROUGH */
                case INS_RD_BUS_16_STR_VI_VI : if(true) {
                        final long address = (instCode == INS_RD_BUS_16_STR_IM_VI) ? instruction[1] : _xviGet( instruction[1] );
                        final int  index   = (int) instruction[3];
                        if( (address % 4) == 0 ) {
                            _xviSet(index, ( _prog._swdRdBus(address) & 0x0000FFFFL ) & instruction[2]                  );
                        }
                        else {
                            final long addressAligned = address & 0xFFFFFFFC;
                            _xviSet(index, ( ( _prog._swdRdBus(addressAligned) >> 16 ) & 0x0000FFFFL ) & instruction[2] );
                        }
                    }
                    break;

                case INS_RD_BUS_STR_IM_VI_TRY_CHK : if(true) {
                        // Suppress all error message notifications
                        _progSWD()._suppressAllErrorMessageNotifications();
                        // Read the value
                        try {
                            final long value = _prog._swdRdBus( instruction[1] ) & instruction[2];
                            _xviSet( instruction[3],  value                            ); // Store the read value
                            _xviSet( instruction[5], (value != instruction[4]) ? 1 : 0 ); // Mark as success if the read value is not the same as the specified error value
                        }
                        catch(final USB2GPIO.TansmitError e) {
                            _xviSet( instruction[3], instruction[4] ); // Store the specified error value
                            _xviSet( instruction[5], 0              ); // Mark as error
                            _prog._swd_reinit();
                        }
                        // Restore all error message notifications
                        _progSWD()._restoreAllErrorMessageNotifications();
                    }
                    break;

                // [address] = [address] & (~<clr_mask>) | <set_mask>
                case INS_MD_BUS_IM_IM_VI     : _prog._swdWrBus(          instruction[1]  , _prog._swdRdBus(          instruction[1]   ) & ( ( ~         instruction[2]   ) & 0xFFFFFFFFL ) | _xviGet( instruction[3] ) ); break;
                case INS_MD_BUS_IM_IM_IM     : _prog._swdWrBus(          instruction[1]  , _prog._swdRdBus(          instruction[1]   ) & ( ( ~         instruction[2]   ) & 0xFFFFFFFFL ) |          instruction[3]   ); break;
                case INS_MD_BUS_IM_VI_VI     : _prog._swdWrBus(          instruction[1]  , _prog._swdRdBus(          instruction[1]   ) & ( ( ~_xviGet( instruction[2] ) ) & 0xFFFFFFFFL ) | _xviGet( instruction[3] ) ); break;
                case INS_MD_BUS_IM_VI_IM     : _prog._swdWrBus(          instruction[1]  , _prog._swdRdBus(          instruction[1]   ) & ( ( ~_xviGet( instruction[2] ) ) & 0xFFFFFFFFL ) |          instruction[3]   ); break;

                // [address] = [address] & (~<clr_mask>) | <set_mask>
                case INS_MD_BUS_VI_IM_VI     : _prog._swdWrBus( _xviGet( instruction[1] ), _prog._swdRdBus( _xviGet( instruction[1] ) ) & ( ( ~         instruction[2]   ) & 0xFFFFFFFFL ) | _xviGet( instruction[3] ) ); break;
                case INS_MD_BUS_VI_IM_IM     : _prog._swdWrBus( _xviGet( instruction[1] ), _prog._swdRdBus( _xviGet( instruction[1] ) ) & ( ( ~         instruction[2]   ) & 0xFFFFFFFFL ) |          instruction[3]   ); break;
                case INS_MD_BUS_VI_VI_VI     : _prog._swdWrBus( _xviGet( instruction[1] ), _prog._swdRdBus( _xviGet( instruction[1] ) ) & ( ( ~_xviGet( instruction[2] ) ) & 0xFFFFFFFFL ) | _xviGet( instruction[3] ) ); break;
                case INS_MD_BUS_VI_VI_IM     : _prog._swdWrBus( _xviGet( instruction[1] ), _prog._swdRdBus( _xviGet( instruction[1] ) ) & ( ( ~_xviGet( instruction[2] ) ) & 0xFFFFFFFFL ) |          instruction[3]   ); break;

                case INS_WR_BUS_DB_IM_IM     : /* FALLTHROUGH */
                case INS_WR_BUS_DB_IM_VI     : /* FALLTHROUGH */
                case INS_WR_BUS_DB_VI_IM     : /* FALLTHROUGH */
                case INS_WR_BUS_DB_VI_VI     : /* FALLTHROUGH */
                case INS_WR_BUS_SB_IM_IM     : /* FALLTHROUGH */
                case INS_WR_BUS_SB_IM_VI     : /* FALLTHROUGH */
                case INS_WR_BUS_SB_VI_IM     : /* FALLTHROUGH */
                case INS_WR_BUS_SB_VI_VI     : if(true) {
                        final int     instCode_ =  instCode & 0xFFFFFFFE;
                        final boolean sb        = (instCode & 1) != 0;
                        final long    address   =         (instCode_ == INS_WR_BUS_DB_VI_IM || instCode_ == INS_WR_BUS_DB_VI_VI) ? _xviGet( instruction[1] ) : instruction[1]  ;
                        final int     offset    = (int) ( (instCode_ == INS_WR_BUS_DB_IM_VI || instCode_ == INS_WR_BUS_DB_VI_IM) ? _xviGet( instruction[2] ) : instruction[2] );
                        _prog._swdWrBus( address, sb ? _scratchBuff[offset] : dataBuff[offset] & 0xFFFFFFFFL );
                    }
                    break;

                case INS_RD_BUS_STR_DB_IM_IM : /* FALLTHROUGH */
                case INS_RD_BUS_STR_DB_IM_VI : /* FALLTHROUGH */
                case INS_RD_BUS_STR_DB_VI_IM : /* FALLTHROUGH */
                case INS_RD_BUS_STR_DB_VI_VI : /* FALLTHROUGH */
                case INS_RD_BUS_STR_SB_IM_IM : /* FALLTHROUGH */
                case INS_RD_BUS_STR_SB_IM_VI : /* FALLTHROUGH */
                case INS_RD_BUS_STR_SB_VI_IM : /* FALLTHROUGH */
                case INS_RD_BUS_STR_SB_VI_VI : if(true) {
                        final int     instCode_ =  instCode & 0xFFFFFFFE;
                        final boolean sb        = (instCode & 1) != 0;
                        final long    address   =         (instCode_ == INS_RD_BUS_STR_DB_VI_IM || instCode_ == INS_RD_BUS_STR_DB_VI_VI) ? _xviGet( instruction[1] ) : instruction[1]  ;
                        final int     offset    = (int) ( (instCode_ == INS_RD_BUS_STR_DB_IM_VI || instCode_ == INS_RD_BUS_STR_DB_VI_VI) ? _xviGet( instruction[3] ) : instruction[3] );
                        final int     value     = (int) ( _prog._swdRdBus(address) & instruction[2] );
                        if(sb) _scratchBuff[offset] = value;
                        else   dataBuff    [offset] = value;
                    }
                    break;

                case INS_WR_RAW_MEMAP_IM_IM_IM : /* FALLTHROUGH */
                case INS_WR_RAW_MEMAP_IM_IM_VI : if(true) {
                        final int  ap          = (int) instruction[1];
                        final int  bank_offset = (int) instruction[2];
                        final long value       = ( (instCode == INS_WR_RAW_MEMAP_IM_IM_VI) ? _xviGet( instruction[3] ) : instruction[3] );
                        _prog._swdWrRawMemAP(ap, bank_offset, value);
                    }
                    break;

                case INS_RD_RAW_MEMAP_STR_IM_IM_VI : if(true) {
                        final int ap          = (int) instruction[1];
                        final int bank_offset = (int) instruction[2];
                        _xviSet( instruction[3], _prog._swdRdRawMemAP(ap, bank_offset) );
                    }
                    break;

                case INS_WR_RAW_MEMAP3_IM_IM_IM : /* FALLTHROUGH */
                case INS_WR_RAW_MEMAP3_IM_IM_VI : if(true) {
                        final long value = (instCode == INS_WR_RAW_MEMAP_IM_IM_VI) ? _xviGet( instruction[2] ) : instruction[2];
                        _prog._swdWrRawMemAP3(instruction[1], value);
                    }
                    break;

                case INS_RD_RAW_MEMAP3_STR_IM_IM_VI : if(true) {
                        _xviSet( instruction[2], _prog._swdRdRawMemAP3(instruction[1]) );
                    }
                    break;

                case INS_VI_STR_VI : _xviSet( instruction[2], _xviGet( instruction[1] ) ); break;
                case INS_IM_STR_VI : _xviSet( instruction[2],          instruction[1]   ); break;

                case INS_PUSH_VI   : if(true) {
                        for(int i = 1; i < instruction.length; ++i) {
                            _xviStack.push( _xviGet( instruction[i] ) );
                        }
                    }
                    break;

                case INS_POP_VI    : if(true) {
                        for(int i = 1; i < instruction.length; ++i) {
                            _xviSet( instruction[instruction.length - i], _xviStack.pop() );
                        }
                    }
                    break;

                case INS_VI_STR_DB_VI     : dataBuff    [ (int) _xviGet( instruction[2] ) ] = (int) _xviGet( instruction[1] ); break;
                case INS_IM_STR_DB_VI     : dataBuff    [ (int) _xviGet( instruction[2] ) ] = (int)          instruction[1]  ; break;
                case INS_VI_STR_SB_VI     : _scratchBuff[ (int) _xviGet( instruction[2] ) ] = (int) _xviGet( instruction[1] ); break;
                case INS_IM_STR_SB_VI     : _scratchBuff[ (int) _xviGet( instruction[2] ) ] = (int)          instruction[1]  ; break;

                case INS_DB_VI_STR_VI     : _xviSet( instruction[2], dataBuff    [ (int) _xviGet( instruction[1] ) ] & 0xFFFFFFFFL ); break;
                case INS_DB_IM_STR_VI     : _xviSet( instruction[2], dataBuff    [ (int)          instruction[1]   ] & 0xFFFFFFFFL ); break;
                case INS_SB_VI_STR_VI     : _xviSet( instruction[2], _scratchBuff[ (int) _xviGet( instruction[1] ) ] & 0xFFFFFFFFL ); break;
                case INS_SB_IM_STR_VI     : _xviSet( instruction[2], _scratchBuff[ (int)          instruction[1]   ] & 0xFFFFFFFFL ); break;

                case INS_PACK_DB_4X8_32   : if(true) {
                        for(int i = 0; i < dataBuff.length; i += 4) {
                            final int b0 = dataBuff[i + 0];
                            final int b1 = dataBuff[i + 1];
                            final int b2 = dataBuff[i + 2];
                            final int b3 = dataBuff[i + 3];
                            final int pv = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
                            dataBuff[i / 4] = SysUtil.osIsLE() ? pv : Integer.reverseBytes(pv);
                        }
                    }
                    break;

                case INS_UNPACK_DB_32_4X8 : if(true) {
                        for(int i = dataBuff.length - 4; i >= 0; i -= 4) {
                            final int pv = SysUtil.osIsLE() ? dataBuff[i / 4] : Integer.reverseBytes(dataBuff[i / 4]);
                            dataBuff[i + 0] = (pv      ) & 0xFF;
                            dataBuff[i + 1] = (pv >>  8) & 0xFF;
                            dataBuff[i + 2] = (pv >> 16) & 0xFF;
                            dataBuff[i + 3] = (pv >> 24) & 0xFF;
                        }
                    }
                    break;

                case INS_PUSH_DB          : _dbStack.push( XCom.arrayCopy(dataBuff) )                     ; break;
                case INS_POP_DB           : XCom.arrayCopy( dataBuff, _dbStack.pop(), 0, dataBuff.length ); break;

                case INS_VI_ADD_VI_VI    : _xviSet( instruction[3],    _xviGet( instruction[1] )   +   _xviGet( instruction[2] ) ); break;
                case INS_VI_ADD_VI_IM    : _xviSet( instruction[3],    _xviGet( instruction[1] )   +            instruction[2]   ); break;
                case INS_VI_SUB_VI_VI    : _xviSet( instruction[3],    _xviGet( instruction[1] )   -   _xviGet( instruction[2] ) ); break;
                case INS_VI_SUB_VI_IM    : _xviSet( instruction[3],    _xviGet( instruction[1] )   -            instruction[2]   ); break;
                case INS_VI_MUL_VI_VI    : _xviSet( instruction[3],    _xviGet( instruction[1] )   *   _xviGet( instruction[2] ) ); break;
                case INS_VI_MUL_VI_IM    : _xviSet( instruction[3],    _xviGet( instruction[1] )   *            instruction[2]   ); break;
                case INS_VI_DIV_VI_VI    : _xviSet( instruction[3],    _xviGet( instruction[1] )   /   _xviGet( instruction[2] ) ); break;
                case INS_VI_DIV_VI_IM    : _xviSet( instruction[3],    _xviGet( instruction[1] )   /            instruction[2]   ); break;
                case INS_VI_MOD_VI_VI    : _xviSet( instruction[3],    _xviGet( instruction[1] )   %   _xviGet( instruction[2] ) ); break;
                case INS_VI_MOD_VI_IM    : _xviSet( instruction[3],    _xviGet( instruction[1] )   %            instruction[2]   ); break;

                case INS_VI_BW_NOT_VI    : _xviSet( instruction[2], ( ~_xviGet( instruction[1] ) ) &   0xFFFFFFFFL               ); break;
                case INS_VI_BW_AND_VI_VI : _xviSet( instruction[3],    _xviGet( instruction[1] )   &   _xviGet( instruction[2] ) ); break;
                case INS_VI_BW_AND_VI_IM : _xviSet( instruction[3],    _xviGet( instruction[1] )   &            instruction[2]   ); break;
                case INS_VI_BW_OR_VI_VI  : _xviSet( instruction[3],    _xviGet( instruction[1] )   |   _xviGet( instruction[2] ) ); break;
                case INS_VI_BW_OR_VI_IM  : _xviSet( instruction[3],    _xviGet( instruction[1] )   |            instruction[2]   ); break;
                case INS_VI_BW_XOR_VI_VI : _xviSet( instruction[3],    _xviGet( instruction[1] )   ^   _xviGet( instruction[2] ) ); break;
                case INS_VI_BW_XOR_VI_IM : _xviSet( instruction[3],    _xviGet( instruction[1] )   ^            instruction[2]   ); break;
                case INS_VI_BW_LSH_VI_VI : _xviSet( instruction[3],    _xviGet( instruction[1] )   <<  _xviGet( instruction[2] ) ); break;
                case INS_VI_BW_LSH_VI_IM : _xviSet( instruction[3],    _xviGet( instruction[1] )   <<           instruction[2]   ); break;
                case INS_VI_BW_RSH_VI_VI : _xviSet( instruction[3],    _xviGet( instruction[1] )   >>> _xviGet( instruction[2] ) ); break;
                case INS_VI_BW_RSH_VI_IM : _xviSet( instruction[3],    _xviGet( instruction[1] )   >>>          instruction[2]   ); break;

                case INS_ERR_CMP_EQ_VI_IM  : /* FALLTHROUGH */
                case INS_ERR_CMP_EQ_VI_VI  : /* FALLTHROUGH */
                case INS_ERR_CMP_NEQ_VI_IM : /* FALLTHROUGH */
                case INS_ERR_CMP_NEQ_VI_VI : if(true) {
                        final long    chk    = _xviGet( instruction[1] );
                        final long    ref    = (instCode == INS_ERR_CMP_EQ_VI_VI || instCode == INS_ERR_CMP_NEQ_VI_VI) ? _xviGet( instruction[2] ) : instruction[2];
                        final boolean cmpRes = (instCode == INS_ERR_CMP_EQ_VI_IM || instCode == INS_ERR_CMP_EQ_VI_VI)
                                             ? ( chk == ref )
                                             : ( chk != ref );
                        if(cmpRes) {
                            final String cmpStr = (instCode == INS_ERR_CMP_EQ_VI_IM || instCode == INS_ERR_CMP_EQ_VI_VI) ? "==" : "!=";
                            USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_InsCmpVal, ProgClassName, instCode, chk, chk, cmpStr, ref, ref );
                        }
                    }
                    break;

                case INS_ERR_CMP_GT_VI_IM      : /* FALLTHROUGH */
                case INS_ERR_CMP_GT_VI_VI      : /* FALLTHROUGH */
                case INS_ERR_CMP_GT_VI_IM_TOUT : /* FALLTHROUGH */
                case INS_ERR_CMP_GT_VI_VI_TOUT : /* FALLTHROUGH */
                case INS_ERR_CMP_GTE_VI_IM     : /* FALLTHROUGH */
                case INS_ERR_CMP_GTE_VI_VI     : if(true) {
                        final int     instCode_ =  instCode & 0xFFFFFFFE;
                        final boolean tout      = (instCode & 1) != 0;
                        final long    chk       = _xviGet( instruction[1] );
                        final long    ref       = (instCode_ == INS_ERR_CMP_GT_VI_VI || instCode_ == INS_ERR_CMP_GTE_VI_VI) ? _xviGet( instruction[2] ) : instruction[2];
                        final boolean cmpRes    = (instCode_ == INS_ERR_CMP_GT_VI_IM || instCode_ == INS_ERR_CMP_GT_VI_VI)
                                                ? ( chk >  ref )
                                                : ( chk >= ref );
                        if(cmpRes) {
                            final String cmpStr = (instCode_ == INS_ERR_CMP_GT_VI_IM || instCode_ == INS_ERR_CMP_GT_VI_VI) ? ">" : ">=";
                            if(!tout) {
                                USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_InsCmpVal, ProgClassName, instCode, chk, chk, cmpStr, ref, ref );
                            }
                            else {
                                USB2GPIO.TansmitError.notifyError      ( Texts.ProgXXX_FailSWD_InsCmpVal, ProgClassName, instCode, chk, chk, cmpStr, ref, ref );
                                USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_InsTOut  , ProgClassName, instCode, ref                        );
                            }
                        }
                    }
                    break;

                case INS_ERR_CMP_LT_VI_IM  : /* FALLTHROUGH */
                case INS_ERR_CMP_LT_VI_VI  : /* FALLTHROUGH */
                case INS_ERR_CMP_LTE_VI_IM : /* FALLTHROUGH */
                case INS_ERR_CMP_LTE_VI_VI : if(true) {
                        final long    chk    = _xviGet( instruction[1] );
                        final long    ref    = (instCode == INS_ERR_CMP_LT_VI_VI || instCode == INS_ERR_CMP_LTE_VI_VI) ? _xviGet( instruction[2] ) : instruction[2];
                        final boolean cmpRes = (instCode == INS_ERR_CMP_LT_VI_IM || instCode == INS_ERR_CMP_LT_VI_VI)
                                             ? ( chk <  ref )
                                             : ( chk <= ref );
                        if(cmpRes) {
                            final String cmpStr = (instCode == INS_ERR_CMP_LT_VI_IM || instCode == INS_ERR_CMP_LT_VI_VI) ? "<" : "<=";
                            USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_InsCmpVal, ProgClassName, instCode, chk, chk, cmpStr, ref, ref );
                        }
                    }
                    break;

                case INS_J               :                                                              idx += (int) instruction[1]; break;
                case INS_J_CMP_EQ_VI_VI  : if( _xviGet( instruction[1] ) == _xviGet( instruction[2] ) ) idx += (int) instruction[3]; break;
                case INS_J_CMP_EQ_VI_IM  : if( _xviGet( instruction[1] ) ==          instruction[2]   ) idx += (int) instruction[3]; break;
                case INS_J_CMP_NEQ_VI_VI : if( _xviGet( instruction[1] ) != _xviGet( instruction[2] ) ) idx += (int) instruction[3]; break;
                case INS_J_CMP_NEQ_VI_IM : if( _xviGet( instruction[1] ) !=          instruction[2]   ) idx += (int) instruction[3]; break;
                case INS_J_CMP_GT_VI_VI  : if( _xviGet( instruction[1] ) >  _xviGet( instruction[2] ) ) idx += (int) instruction[3]; break;
                case INS_J_CMP_GT_VI_IM  : if( _xviGet( instruction[1] ) >           instruction[2]   ) idx += (int) instruction[3]; break;
                case INS_J_CMP_GTE_VI_VI : if( _xviGet( instruction[1] ) >= _xviGet( instruction[2] ) ) idx += (int) instruction[3]; break;
                case INS_J_CMP_GTE_VI_IM : if( _xviGet( instruction[1] ) >=          instruction[2]   ) idx += (int) instruction[3]; break;
                case INS_J_CMP_LT_VI_VI  : if( _xviGet( instruction[1] ) <  _xviGet( instruction[2] ) ) idx += (int) instruction[3]; break;
                case INS_J_CMP_LT_VI_IM  : if( _xviGet( instruction[1] ) <           instruction[2]   ) idx += (int) instruction[3]; break;
                case INS_J_CMP_LTE_VI_VI : if( _xviGet( instruction[1] ) <= _xviGet( instruction[2] ) ) idx += (int) instruction[3]; break;
                case INS_J_CMP_LTE_VI_IM : if( _xviGet( instruction[1] ) <=          instruction[2]   ) idx += (int) instruction[3]; break;

                case INS_CALL            :  _indexStack.push(idx); idx += (int) instruction[1]  ; break;
                case INS_RETURN          :                         idx  =      _indexStack.pop(); break;

                case INS_LP_LOAD_VI_VI : if(true) {
                        // Get the parameters
                                   _curLoaderProgram = _lpGet( (int) _xviGet( instruction[1] ) );
                        final long addrProgStart     =               _xviGet( instruction[2] )  ;
                        final long armVTORAddress    =                        instruction[3]    ;
                        // Write the program to the SRAM
                        _prog._swdWrCoreMem(addrProgStart       , _curLoaderProgram);
                        // Set the VTOR and XPSR
                        _prog._swdWrCoreMem(armVTORAddress      , addrProgStart    );
                        _prog._swdWrCoreReg(ProgSWD.CoreReg.XPSR, 0x01000000L      ); // Ensure the core runs in Thumb mode
                    }
                    break;

                case INS_LP_EXECUTE : if(true) {
                        _prog._swdWrCoreReg  (ProgSWD.CoreReg.PC, _curLoaderProgram[1] & 0xFFFFFFFEL);
                        _prog._swdWrCoreReg  (ProgSWD.CoreReg.SP, _curLoaderProgram[0]              );
                        _prog._swd_unhaltCore(false             , true                              );
                    }
                    break;

                case INS_LP_CONTINUE : if(true) {
                        final long    address     = _prog._swdRdCoreReg(ProgSWD.CoreReg.PC);
                        final long    opcode      = ( (address % 4) == 0 ) ?   _prog._swdRdBus(address             )         & 0x0000FFFFL
                                                                           : ( _prog._swdRdBus(address & 0xFFFFFFFC) >> 16 ) & 0x0000FFFFL;
                        final boolean isBKPT      = (opcode == 0xBE00);
                        _prog._swdWrCoreReg  ( ProgSWD.CoreReg.PC, isBKPT ? (address + 2) : address );
                        _prog._swd_unhaltCore( false             , true                             );
                    }
                    break;

                case INS_DEBUG_PRINTLN         : SysUtil.stdDbg().printf (              "\n"                                            ); break;
                case INS_DEBUG_PRINTLN_SDEC_NN : SysUtil.stdDbg().printf (           "%+d\n", _xviGet( instruction[1] )                 ); break;
                case INS_DEBUG_PRINTLN_SDEC_03 : SysUtil.stdDbg().printf (         "%+04d\n", _xviGet( instruction[1] )                 ); break;
                case INS_DEBUG_PRINTLN_SDEC_05 : SysUtil.stdDbg().printf (         "%+06d\n", _xviGet( instruction[1] )                 ); break;
                case INS_DEBUG_PRINTLN_SDEC_08 : SysUtil.stdDbg().printf (         "%+09d\n", _xviGet( instruction[1] )                 ); break;
                case INS_DEBUG_PRINTLN_SDEC_10 : SysUtil.stdDbg().printf (        "%+011d\n", _xviGet( instruction[1] )                 ); break;
                case INS_DEBUG_PRINTLN_UDEC_NN : SysUtil.stdDbg().printf (            "%d\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UDEC_03 : SysUtil.stdDbg().printf (          "%03d\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UDEC_05 : SysUtil.stdDbg().printf (          "%05d\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UDEC_08 : SysUtil.stdDbg().printf (          "%08d\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UDEC_10 : SysUtil.stdDbg().printf (         "%010d\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UBIN_NN : SysUtil.stdDbg().println( XCom.uintNNbinStr( _xviGet( instruction[1] ) & 0xFFFFFFFFL ) ); break;
                case INS_DEBUG_PRINTLN_UBIN_08 : SysUtil.stdDbg().println( XCom.uint08binStr( _xviGet( instruction[1] ) & 0xFFFFFFFFL ) ); break;
                case INS_DEBUG_PRINTLN_UBIN_16 : SysUtil.stdDbg().println( XCom.uint16binStr( _xviGet( instruction[1] ) & 0xFFFFFFFFL ) ); break;
                case INS_DEBUG_PRINTLN_UBIN_24 : SysUtil.stdDbg().println( XCom.uint24binStr( _xviGet( instruction[1] ) & 0xFFFFFFFFL ) ); break;
                case INS_DEBUG_PRINTLN_UBIN_32 : SysUtil.stdDbg().println( XCom.uint32binStr( _xviGet( instruction[1] ) & 0xFFFFFFFFL ) ); break;
                case INS_DEBUG_PRINTLN_UOCT_NN : SysUtil.stdDbg().printf (            "%o\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UOCT_03 : SysUtil.stdDbg().printf (          "%03o\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UOCT_06 : SysUtil.stdDbg().printf (          "%06o\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UOCT_08 : SysUtil.stdDbg().printf (          "%08o\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UOCT_11 : SysUtil.stdDbg().printf (         "%011o\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UHEX_NN : SysUtil.stdDbg().printf (            "%X\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UHEX_02 : SysUtil.stdDbg().printf (          "%02X\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UHEX_04 : SysUtil.stdDbg().printf (          "%04X\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UHEX_06 : SysUtil.stdDbg().printf (          "%06X\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTLN_UHEX_08 : SysUtil.stdDbg().printf (          "%08X\n", _xviGet( instruction[1] ) & 0xFFFFFFFFL   ); break;
                case INS_DEBUG_PRINTF          : if(true) {
                        // ##### !!! TODO : Cast/convert the argument #i to the proper type according to the format !!! #####
                        final String   format = (String) _printfGetArg( instruction[1] );
                        final Object[] args   = new Object[instruction.length - 2];
                        for(int i = 2; i < instruction.length; ++i) {
                            final long argType = instruction[i] & 0xFFFF0000L;
                                 if(argType == ARG_MAP_TYPE_NULL  ) args[i - 2] = null;
                            else if(argType == ARG_MAP_TYPE_XVI   ) args[i - 2] = _xviGet( ( (XVI) _printfGetArg( instruction[i] ) ).value() );
                            else if(argType == ARG_MAP_TYPE_STRING) args[i - 2] =                  _printfGetArg( instruction[i] )            ;
                            else if(argType == ARG_MAP_TYPE_NUMBER) args[i - 2] =                  _printfGetArg( instruction[i] )            ;
                        }
                        SysUtil.stdDbg().printf(format, args);
                    }
                    break;

                default:
                    USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_InsInvld, ProgClassName, instCode);
                    break;

            } // switch

        } // for

        // The default return value of this function
        return Long.MIN_VALUE;
    }

} // class SWDExecInst
