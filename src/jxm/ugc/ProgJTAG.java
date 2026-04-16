/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.ArrayList;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.xb.*;
import jxm.tool.*;

import static jxm.tool.JTAGBitstream.Command;
import static jxm.tool.JTAGBitstream.TRSTMode;
import static jxm.tool.JTAGBitstream.StableState;
import static jxm.tool.JTAGBitstream.PathState;
import static jxm.tool.JTAGBitstream.CmdPar;


/*
 * This class and its related classes are written partially based on the algorithms and information found from:
 *
 *     Joint Test Action Group (JTAG) Protocol
 *     https://piembsystech.com/joint-test-action-group-jtag-protocol
 *
 *     The JTAG Test Access Port (TAP) State Machine
 *     https://www.allaboutcircuits.com/technical-articles/jtag-test-access-port-tap-state-machine
 *
 *     Diving into JTAG Protocol Part 1 - Overview.
 *     https://medium.com/@aliaksandr.kavalchuk/diving-into-jtag-protocol-part-1-overview-fbdc428d3a16
 *
 *     Diving into JTAG Protocol Part 2 - Debugging
 *     https://medium.com/@aliaksandr.kavalchuk/diving-into-jtag-protocol-part-2-debugging-56a566db3cf8
 *
 *     Diving into JTAG Protocol Part 3 - Boundary Scan
 *     https://medium.com/@aliaksandr.kavalchuk/diving-into-jtag-part-3-boundary-scan-17f9975ecc59
 *
 *     Diving into JTAG Protocol Part 4 - BSDL
 *     https://medium.com/@aliaksandr.kavalchuk/diving-into-jtag-protocol-part-4-bsdl-29fc4081502c
 *
 *     Example Showing JTAG Operation
 *     https://vlsitutorials.com/example-showing-jtag-operation
 *
 * ~~~ Last accessed & checked on 2025-02-21 ~~~
 */
public class ProgJTAG {

    /*
     * Transfer speed:
     *     # Using USB_ISS            : not supported
     *     # Using JxMake DASA        : not supported
     *     # Using JxMake USB-GPIO    : not supported
     *     # Using JxMake USB-GPIO II : up to ~3400 ... ~21500 bytes per second (depending on the clock rate and how the JTAG bitstream is structured)
     */

    protected static final String ProgClassName = "ProgJTAG";

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected final USB2GPIO _usb2gpio;

    public ProgJTAG(final USB2GPIO usb2gpio) throws Exception
    {
        // Store the object
        _usb2gpio = usb2gpio;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _inProgMode       = false;
    private int     _tckDefaultClkDiv = -1;

    public boolean begin(final int tckDefaultFreq)
    {
        // Check if the implementation supports JTAG
        if( !_usb2gpio.jtagIsModeSupported() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailJTAG_NS, ProgClassName);

        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flag and data
        _inProgMode       = false;
        _tckDefaultClkDiv = -1;

        // Get the JTAG clock divider
        final int jtagClkDiv = _usb2gpio.jtagClkFreqToClkDiv(tckDefaultFreq);

        // Initialize the JTAG
        for(int i = 0; i < 2; ++i) {
            // Initialize the JTAG
            if( _usb2gpio.jtagBegin(jtagClkDiv) ) break;
            // Error initializing the JTAG - exit if this is the 2nd initialization attempt
            if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitJTAG, ProgClassName);
            // Uninitialize the JTAG and try again
            _usb2gpio.jtagEnd();
            SysUtil.sleepMS(250);
        }

        // Set flag and data
        _inProgMode       = true;
        _tckDefaultClkDiv = jtagClkDiv;

        // Done
        return true;
    }

    public boolean end()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Uninitialize the JTAG
        final boolean resJTAGEnd = _usb2gpio.jtagEnd();

        // Clear flag and data
        _inProgMode       = false;
        _tckDefaultClkDiv = -1;

        // Check for error(s)
        if(!resJTAGEnd) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailUninitJTAG, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // 7-bit values to be clocked into TMS to move from one state to another (LSB first)
    private static int[][] _seqTMS7 = {
        /* --- from --- */
        /* 0 RESET      */ { 0b1111111, 0b0000000, 0b0010111, 0b0001010, 0b0011011, 0b0010110 },
        /* 1 IDLE       */ { 0b1111111, 0b0000000, 0b0100101, 0b0000101, 0b0101011, 0b0001011 },
        /* 2 DRSHIFT    */ { 0b1111111, 0b0110001, 0b0000000, 0b0000001, 0b0001111, 0b0101111 },
        /* 3 DRPAUSE    */ { 0b1111111, 0b0110000, 0b0100000, 0b0010111, 0b0011110, 0b0101111 },
        /* 4 IRSHIFT    */ { 0b1111111, 0b0110001, 0b0000111, 0b0010111, 0b0000000, 0b0000001 },
        /* 5 IRPAUSE    */ { 0b1111111, 0b0110000, 0b0011100, 0b0010111, 0b0011110, 0b0101111 }
        /* ---- to ---- */ /* RESET      IDLE       DRSHIFT    DRPAUSE    IRSHIFT    IRPAUSE */
                           /* 0          1          2          3          4          5       */
    };

    private static int _getSeqTMS7(final PathState from, final PathState to)
    { return _seqTMS7[ from.idx() ][ to.idx() ]; }

    private static int _getSeqTMS7(final StableState from, final PathState to)
    { return _seqTMS7[ from.idx() ][ to.idx() ]; }

    private static int _getSeqTMS7(final PathState from, final StableState to)
    { return _seqTMS7[ from.idx() ][ to.idx() ]; }

    private static int _getSeqTMS7(final StableState from, final StableState to)
    { return _seqTMS7[ from.idx() ][ to.idx() ]; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String           _tag;

    private int              _curTCKFreq;
    private TRSTMode         _trstMode;
    private PathState        _curState;

    private StableState      _endDR;
    private StableState      _endIR;

    private ArrayList<int[]> _resBuff = new ArrayList<>();

    private boolean _executeError(final JTAGBitstream jbs, final CmdPar cp)
    {
        if( !_moveCurStateTo(PathState.IDLE) ) {
                         USB2GPIO.notifyError( Texts.ProgXXX_FailJTAG_IDLE   , ProgClassName                                   );
        }

        if(_tag != null) USB2GPIO.notifyError( Texts.ProgXXX_FailJTAG_ExecTag, ProgClassName, jbs.getFilePath(), cp.line, _tag );
        else             USB2GPIO.notifyError( Texts.ProgXXX_FailJTAG_Exec   , ProgClassName, jbs.getFilePath(), cp.line       );

        return false;
    }

    private boolean _executeErrorInvalidTDO(final JTAGBitstream jbs, final CmdPar cp, final int bidx, final int got, final int ref)
    {
        if( !_moveCurStateTo(PathState.IDLE) ) {
                         USB2GPIO.notifyError( Texts.ProgXXX_FailJTAG_IDLE    , ProgClassName                                                   );
        }

        if(_tag != null) USB2GPIO.notifyError( Texts.ProgXXX_FailJTAG_TDOIvTag, ProgClassName, bidx, got, ref, jbs.getFilePath(), cp.line, _tag );
        else             USB2GPIO.notifyError( Texts.ProgXXX_FailJTAG_TDOIv   , ProgClassName, bidx, got, ref, jbs.getFilePath(), cp.line       );

        return false;
    }

    private boolean _trstValue()
    { return (_trstMode == TRSTMode.ON) ? false : true; }

    private int[] _splitVal(final int numBits, final long[] value_, final long[] mask_, final int idx)
    {
        final int   byteCount = (numBits + 7) / 8;
        final int[] split     = new int[byteCount];

              long  value     = ( (value_ != null) ? value_[idx] : 0x0000000000000000L )
                              & ( (mask_  != null) ? mask_ [idx] : 0xFFFFFFFFFFFFFFFFL );

        for(int i = 0; i < byteCount; ++i) {
            split[i] = (int) (value & 0x00000000000000FFL);
            value >>>= 8;
        }

        return split;
    }

    private boolean _moveCurStateTo(final PathState to)
    {
         if( !_usb2gpio.jtagTMS( true, _trstValue(), true, 7 - 1, _getSeqTMS7(_curState, to) ) ) return false;

         _curState = to;

         return true;
    }

    private boolean _moveCurStateTo(final StableState to)
    {
         if( !_usb2gpio.jtagTMS( true, _trstValue(), true, 7 - 1, _getSeqTMS7(_curState, to) ) ) return false;

         _curState = to.toPS();

         return true;
    }

    public boolean execute(final JTAGBitstream jbs, final IntConsumer progressCallback)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Clear the states
        _tag        = null;
        _curTCKFreq = _usb2gpio.jtagClkDivToClkFreq(_tckDefaultClkDiv);
        _trstMode   = TRSTMode   .__NS__;
        _curState   = PathState  .RESET;
        _endDR      = StableState.IDLE;
        _endIR      = StableState.IDLE;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();
        final int        nb  = jbs.getEffByteSize();

        // Process the commands
        boolean disPBAR        = false;
        boolean initPBAR       = false;
        boolean earlyExit      = false;
        boolean cmdTRSTAllowed = true;
        int     accBit         = 0;

        for( final CmdPar cp : jbs.getCmdPar() ) {

            if(earlyExit) break; // Check for early exit

            switch(cp.cmd) {

                case FREQUENCY : {
                        /* value : { cycles } */
                        if( cp.value[0] == 0 ) {
                            if( !_usb2gpio.jtagSetClkDiv(_tckDefaultClkDiv) ) return _executeError(jbs, cp);
                            _curTCKFreq = _usb2gpio.jtagClkDivToClkFreq(_tckDefaultClkDiv);
                        }
                        else {
                            final int clkDiv = _usb2gpio.jtagClkFreqToClkDiv( (int) cp.value[0] );
                            if( !_usb2gpio.jtagSetClkDiv(clkDiv) ) return _executeError(jbs, cp);
                            _curTCKFreq = _usb2gpio.jtagClkDivToClkFreq(clkDiv);
                        }
                    }
                    break;

                case TRST : {
                        /* trstMode : trst_mode */
                        // Error if 'ABSENT' is not the first command
                        if(cp.trstMode == TRSTMode.ABSENT && !cmdTRSTAllowed) return _executeError(jbs, cp);
                        // Clear flag as needed
                        cmdTRSTAllowed = (cp.trstMode != TRSTMode.ABSENT);
                        // Save the nTRST mode
                        _trstMode = cp.trstMode;
                        // Set the nTRST pin
                        _usb2gpio.jtagSetReset( true, _trstValue(), true ); // nTRST is active low
                    }
                    break;

                case ENDDR : {
                        /* stableState : { stable_state } */
                        // Error if the state is an IR state
                        if(cp.stableState[0] == StableState.IRPAUSE) return _executeError(jbs, cp);
                        // Save the state
                        _endDR = cp.stableState[0];
                    }
                    break;

                case ENDIR : {
                        /* stableState : { stable_state } */
                        // Error if the state is a DR state
                        if(cp.stableState[0] == StableState.DRPAUSE) return _executeError(jbs, cp);
                        // Save the state
                        _endIR = cp.stableState[0];
                    }
                    break;

                case STATE : {
                        /* pathState   : [ { path_state, ... } ] */
                        /* stableState :   { stable_state    }   */
                        // ##### ??? TODO : Is it really necessary to check the 'pathState' ??? #####
                        // Clear flag
                        cmdTRSTAllowed = false;
                        // Move the current state
                        if( !_moveCurStateTo( cp.stableState[0] ) ) return _executeError(jbs, cp);
                    }
                    break;

                case SDR_TDI : /* FALLTHROUGH */
                case SIR_TDI : /* FALLTHROUGH */
                case SDR_TDO : /* FALLTHROUGH */
                case SIR_TDO : {
                        /* length :   { length, ... } ] */
                        /* value  :   { tdx   , ... } ] */
                        /* mask   : [ { mask  , ... } ] */
                        // Clear flag
                        cmdTRSTAllowed = false;
                        // Determine the command parameters
                        final boolean     isDR    = (cp.cmd == Command.SDR_TDI) || (cp.cmd == Command.SDR_TDO);
                        final boolean     isOut   = (cp.cmd == Command.SDR_TDO) || (cp.cmd == Command.SIR_TDO);
                        final PathState   psShift = isDR ? PathState.DRSHIFT : PathState.IRSHIFT;
                        final StableState endDx   = isDR ? _endDR            : _endIR;
                        // Perform the transfer only if this is not a linked SxR_TDO command
                        if(!isOut || !cp.linked) {
                            // Clear the result buffer
                            _resBuff.clear();
                            // Ensure that it starts from the IDLE state
                            if( _curState != PathState.IDLE ) {
                                if( !_moveCurStateTo(PathState.IDLE) ) return _executeError(jbs, cp);
                            }
                            // Perform scan
                            /*
                            SysUtil.stdDbg().printf( "SCAN >>> %s [%d] linked=%b<<< \n", cp.cmd.name(), cp.length.length, cp.linked );
                            //*/
                            for(int i = 0; i < cp.length.length; ++i) {
                                // Split the value into bytes
                                final boolean last    = (i == cp.length.length - 1);
                                final int     numBits = cp.length[i];
                                final int[]   ioBuff  = isOut ? _splitVal(numBits, null    , null   , i)
                                                              : _splitVal(numBits, cp.value, cp.mask, i);
                                // Accumulate the number of bits
                                accBit += numBits;
                                // Determine the number of bits in the last byte
                                int bitCntLast = (ioBuff.length > 1) ? ( numBits % ( (ioBuff.length - 1) * 8) ) : numBits;
                                if(bitCntLast == 0) bitCntLast = 8;
                                // Determine the flags
                                final boolean xUpdate = last;
                                final boolean xrShift = _curState != psShift;
                                /*
                                SysUtil.stdDbg().printf(
                                    "last=%b numBits=%d -> bytes=%d bitCntLast=%d (%d) | xUpdate=%b %crShift=%b\n",
                                     last, numBits, ioBuff.length, bitCntLast, (last ? bitCntLast : 8) - 1, xUpdate, isDR ? 'd' : 'i', xrShift
                                );
                                //*/
                                // Perform transfer
                                if( !_usb2gpio.jtagTransfer(
                                    xUpdate, isDR & xrShift, !isDR & xrShift, (last ? bitCntLast : 8) - 1, ioBuff
                                ) ) return _executeError(jbs, cp);
                                // Update the current state
                                     if(xUpdate) _curState = PathState.IDLE;
                                else if(xrShift) _curState = psShift;
                                // Save the buffer to the result buffer
                                _resBuff.add(ioBuff);
                            } // for
                            // Ensure that it ends in the '_endDR'/'_endIR' state
                            if( _curState != endDx.toPS() ) {
                                if( !_moveCurStateTo(endDx) ) return _executeError(jbs, cp);
                            }
                        }
                        // Check the result
                        if(isOut) {
                            /*
                            SysUtil.stdDbg().printf( "PREV >>> %s [%d] : %d linked=%b<<< \n", cp.cmd.name(), cp.length.length, cp.length[0], cp.linked );
                            //*/
                            for(int i = 0; i < cp.length.length; ++i) {
                                // Get the values
                                final int   numBits = cp.length[i];
                                final int[] resBuff = _resBuff.get(i);
                                final int[] mskBuff = _splitVal(numBits, cp.mask , null, i);
                                final int[] refBuff = _splitVal(numBits, cp.value, null, i);
                                // Accumulate the number of bits
                                accBit += numBits;
                                // Compare the byte(s)
                                for(int c = 0; c < resBuff.length; ++c) {
                                    final int got = resBuff[c] & mskBuff[c];
                                    final int ref = refBuff[c] & mskBuff[c];
                                    /*
                                    SysUtil.stdDbg().printf("%02X %02X | %02X\n", got, mskBuff[c], ref);
                                    //*/
                                    if(got != ref) return _executeErrorInvalidTDO(jbs, cp, c, got, ref);
                                }
                            }
                        }
                        // Call the progress callback function for the current value
                        if(accBit > 0) {
                            final int rep = accBit / 16; // The 'ProgressCB' class processes increments by 2 bytes
                            if(rep > 0) {
                                if(!disPBAR) {
                                    if(!initPBAR) {
                                        initPBAR = true;
                                        pcb.callProgressCallbackInitial(progressCallback, nb);
                                    }
                                    pcb.callProgressCallbackCurrentMulti(progressCallback, nb, rep);
                                }
                                accBit %= 16; // Save the remaining number of bits not used for increments
                            }
                        }
                    }
                    break;

                case RUNTEST1 :  /* FALLTHROUGH */
                        /* stableState : { run_state, end_state                   } */
                        /* value       : { run_count, run_clk, min_time, max_time } */
                case RUNTEST2 : {
                        /* stableState : { run_state, end_state } */
                        /* value       : { min_time, max_time   } */
                        // Clear flag
                        cmdTRSTAllowed = false;
                        // Determine the number of uS and clock cycles
                        boolean useTCK      = false;
                        long    sleepTimeUS = 0;
                        long    numClocks   = 0;
                        if(cp.cmd == Command.RUNTEST1) {
                            if( cp.value[2] >= 0 && cp.value[3] >= 0 ) {
                                final long numClocksMin = Math.round( (cp.value[2] * _curTCKFreq) / 1000000.0 );
                                final long numClocksMax = Math.round( (cp.value[3] * _curTCKFreq) / 1000000.0 );
                                if( cp.value[0] < numClocksMin || cp.value[0] > numClocksMax ) return _executeError(jbs, cp);
                            }
                            useTCK      = (cp.value[1] == CmdPar.TCK);
                            sleepTimeUS = (cp.value[2] + cp.value[3] + 1) / 2;
                            numClocks   = cp.value[0];
                        }
                        else {
                            // ##### ??? TODO : Use the minimum time as much as possible ??? #####
                            sleepTimeUS = ( cp.value[0] + cp.value[1] ) / 2;
                            numClocks   = Math.round( (sleepTimeUS * _curTCKFreq) / 1000000.0 );
                        }
                        // Ensure that it runs in the 'run_state' state
                        if( cp.stableState[0] != StableState.__NS__ && _curState != cp.stableState[0].toPS() ) {
                            if( !_moveCurStateTo(cp.stableState[0]) ) return _executeError(jbs, cp);
                        }
                        // Perform dummy transfer to output the clocks
                        if(cp.cmd == Command.RUNTEST1 && useTCK) {
                            long numBytes =        numClocks / 8;
                            int  remBits  = (int) (numClocks % 8);
                            if(remBits != 0) ++numBytes;
                            else             remBits = 8;
                            while(numBytes > 0) {
                                final int nbs = (int) Math.min(numBytes, 128);
                                numBytes -= nbs;
                                if( !_usb2gpio.jtagTransfer(
                                    false, false, false,
                                    ( (numBytes == 0) ? remBits : 8 ) - 1,
                                    new int[nbs]
                                ) ) return _executeError(jbs, cp);
                            }
                        }
                        else { // RUNTEST2 || (Command.RUNTEST1 && !useTCK)
                            // ##### ?? TODO : Does TCK really needs to be pulsed here ??? #####
                            SysUtil.sleepUS(sleepTimeUS);
                        }
                        // Ensure that it ends in the 'end_state' state
                        if( cp.stableState[1] != StableState.__NS__ && _curState != cp.stableState[1].toPS() ) {
                            if( !_moveCurStateTo(cp.stableState[1]) ) return _executeError(jbs, cp);
                        }
                    }

                    break;

                /*
                 * ##### ??? TODO ??? #####
                 *     PIOMAP
                 *     PIO       // cmdTRSTAllowed = false;
                 */

                ////////////////////////////////////////////////////////////////////////////////////////////////////

                case __DIS_PBAR__ : {
                        disPBAR = true;
                    }
                    break;

                case __PRINT__ : {
                        /* string : [ "user_string" ] */
                        // Print the string
                        if(cp.string != null) SysUtil.stdDbg().print( XCom.unescapeString(cp.string) );
                    }
                    break;

                case __TAG__ : {
                        /* string : [ "user_string" ] */
                        // Store the string as the current tag
                        _tag = XCom. unescapeString(cp.string);
                    }
                    break;

                case __EXIT__ : {
                        earlyExit = true;
                    }
                    break;


             } // switch

        } // for

        // Call the progress callback function for the final value
        if(!disPBAR) pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return true;
    }

} // class ProgJTAG
