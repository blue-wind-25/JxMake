/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.lang.reflect.Method;

import java.math.BigInteger;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jxm.*;
import jxm.xb.*;


/*
 * This class is written mostly based on the information found from:
 *
 *     Serial Vector Format Specification - JTAG Boundary Scan Revision E.pdf
 *     https://www.asset-intertech.com/wp-content/uploads/2020/09/svf-serial-vector-format-specification-jtag-boundary-scan-revision-e.pdf
 *
 *     SVF Tutorial: Header and Trailing Registers Explained
 *     https://www.isabekov.pro/svf-tutorial
 *
 *     SVF and XSVF File Formats for Xilinx Devices (ISE Tools)
 *     https://docs.amd.com/v/u/en-US/xapp503
 *
 * ~~~ Last accessed & checked on 2025-02-15 ~~~
 */
public class JTAGBitstream {

    private static class _EnumValues {

        private List<String> _vals = null;

        private < E extends Enum<E> > void _initVals(final Class<E> enumClass)
        {
            if(_vals != null) return;

            try {
                final Method nameMethod = enumClass.getMethod("name");

                _vals = Arrays.asList( Arrays.stream( enumClass.getEnumConstants() )
                    .filter ( v -> v != Enum.valueOf(enumClass, "__NS__") )
                    .map    ( v -> { try {
                                         return nameMethod.invoke(v);
                                     }
                                     catch(final Exception e) {
                                         return null;
                                     }
                                   }
                            )
                    .toArray( String[]::new )
                );
             }
             catch(final Exception e) {}
        }

        public < E extends Enum<E> > List<String> vals(final Class<E> enumClass)
        {
            _initVals(enumClass);
            return _vals;
        }

    } // class _EnumValues

    private static interface _EnumIdx {

        public abstract int idx();

    } // interface _EnumIdx

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum Command {

        FREQUENCY   , /* FREQUENCY [cycles HZ]                                                                                            */
        TRST        , /* TRST trst_mode                                                                                                   */
        ENDDR       , /* ENDDR stable_state                                                                                               */
        ENDIR       , /* ENDIR stable_state                                                                                               */
        STATE       , /* STATE [pathstate...] stable_state                                                                                */
        SDR_TDI     , /* SDR length [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)] --- will include values from header and trailer */
        SDR_TDO     , /* ---                                                                                                              */
        SIR_TDI     , /* SIR length [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)] --- will include values from header and trailer */
        SIR_TDO     , /* ---                                                                                                              */
        RUNTEST1    , /* RUNTEST [run_state] run_count run_clk [ min_time SEC [MAXIMUM max_time SEC] ] [ENDSTATE end_state]               */
        RUNTEST2    , /* RUNTEST [run_state]                     min_time SEC [MAXIMUM max_time SEC]   [ENDSTATE end_state]               */

        // ##### ??? TODO : Add more command to support other SVF-like files, maybe even conditional and loop ??? #####

        __DIS_PBAR__, /* DIS PBAR              --- disable the progress bar display                                                       */
        __PRINT__   , /* PRINT ["user_string"] --- print message; IT WILL MESS WITH THE PROGRESS BAR DISPLAY, use __NO_PCB__ as needed!   */
        __TAG__     , /* TAG ["user_string"]   --- tag the next commands until cleared; the user string will be printed on error          */
        __EXIT__    , /* EXIT                  --- exit the flow immediately; it helps with debugging                                     */

    } // enum Command

    public static enum TRSTMode {

        __NS__ , // Not specified

        ON     ,
        OFF    ,
        Z      ,
        ABSENT

        ;

        private static final _EnumValues _evs = new _EnumValues();

        public static List<String> vals()
        { return _evs.vals(TRSTMode.class); }

    } // enum TRSTMode

    public static enum StableState implements _EnumIdx {

        // !!! NOTE : Ensure the indexes (if specified) match the '_seqTMS7' array layout defined in '../ugc/ProgJTAG.java' !!!

        __NS__ (-1), // Not specified

        IRPAUSE( 5),
        DRPAUSE( 3),
        RESET  ( 0),
        IDLE   ( 1)

        ;

        private final int _idx;

        private StableState(final int idx)
        { _idx = idx; }

        @Override
        public int idx()
        { return _idx; }

        public static PathState toPS(final StableState ss)
        { return PathState.valueOf( ss.name() ); }

        public PathState toPS()
        { return toPS(this); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static final _EnumValues _evs = new _EnumValues();

        public static List<String> vals()
        { return _evs.vals(StableState.class); }

    } // enum StableState

    public static enum PathState implements _EnumIdx {

        // !!! NOTE : Ensure the indexes (if specified) match the '_seqTMS7' array layout defined in '../ugc/ProgJTAG.java' !!!

        __NS__   ( -1                        ), // Not specified

        RESET    ( StableState.RESET  .idx() ),
        IDLE     ( StableState.IDLE   .idx() ),
        DRSELECT ( -1                        ),
        DRCAPTURE( -1                        ),
        DRSHIFT  (  2                        ),
        DRPAUSE  ( StableState.DRPAUSE.idx() ),
        DREXIT1  ( -1                        ),
        DREXIT2  ( -1                        ),
        DRUPDATE ( -1                        ),
        IRSELECT ( -1                        ),
        IRCAPTURE( -1                        ),
        IRSHIFT  (  4                        ),
        IRPAUSE  ( StableState.IRPAUSE.idx() ),
        IREXIT1  ( -1                        ),
        IREXIT2  ( -1                        ),
        IRUPDATE ( -1                        )

        ;

        private final int _idx;

        private PathState(final int idx)
        { _idx = idx; }

        @Override
        public int idx()
        { return _idx; }

        public static PathState fromPS(final StableState ss)
        { return PathState.valueOf( ss.name() ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static final _EnumValues _evs = new _EnumValues();

        public static List<String> vals()
        { return _evs.vals(PathState.class); }

    } // enum PathState

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class CmdPar {

        public static final int TCK = 0;
        public static final int SCK = 1;

        public int           line;        /* Line number in the '_filePath'
                                           */

        public Command       cmd;         /* enum Command
                                           */

        public boolean       linked;      /* SDR_TDO   : 'true' if the TDO specifier is preceded by a TDI specifier in the command; 'false' otherwise
                                           * SIR_TDO   : 'true' if the TDO specifier is preceded by a TDI specifier in the command; 'false' otherwise
                                           *
                                           *              NOTE : If the appropriate header and/or trailer are currently defined, there
                                           *                     will be 2 to 3 consecutive SxR_TDO commands with this flag set to true.
                                           */

        public int[]         length;      /* SDR_TDI   : length...
                                           * SDR_TDO   : length...
                                           * SIR_TDI   : length...
                                           * SIR_TDO   : length...
                                           */

        public long[]        value;       /* FREQUENCY : cycles
                                           * SDR_TDI   : tdi...
                                           * SDR_TDO   : tdo...
                                           * SIR_TDI   : tdi...
                                           * SIR_TDO   : tdo...
                                           * RUNTEST1  : run_count run_clk min_time max_time
                                           * RUNTEST2  :                   min_time max_time
                                           */
                                           /* NOTE :
                                            *     run_clk  -> TCK = 0 ; SCK = 1
                                            *     min_time -> stored in uS
                                            *     max_time -> stored in uS
                                            */

        public long[]        mask;        /* SDR_TDI   : [mask...]
                                           * SDR_TDO   : [mask...]
                                           * SIR_TDI   : [mask...]
                                           * SIR_TDO   : [mask...]
                                           */

        public PathState[]   pathState;   /* STATE     : [path_state...]
                                           */

        public StableState[] stableState; /* ENDDR     : stable_state
                                           * ENDIR     : stable_state
                                           * STATE     : stable_state
                                           * RUNTEST1  : run_state end_state
                                           * RUNTEST2  : run_state end_state
                                           */

        public TRSTMode      trstMode;    /* TRST      : trst_mode
                                           */

        public String        string;      /* __PRINT__ : ["user_string"]
                                           * __TAG__   : ["user_string"]
                                           */

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public CmdPar(final int lNum, final Command cmd_)
        {
            line        = lNum;
            cmd         = cmd_;
            linked      = false;
            length      = null;
            value       = null;
            mask        = null;
            pathState   = null;
            stableState = null;
            trstMode    = TRSTMode.__NS__;
            string      = null;
        }

        public CmdPar(final int lNum, final Command cmd_, final String string_)
        {
            this(lNum, cmd_);

            string = string_;
        }

        public CmdPar(final int lNum, final Command cmd_, final long value_)
        {
            this(lNum, cmd_);

            value = new long[] { value_ };
        }

        public CmdPar(final int lNum, final Command cmd_, final StableState stableState_)
        {
            this(lNum, cmd_);

            stableState = new StableState[] { stableState_ };
        }

        public CmdPar(final int lNum, final Command cmd_, final StableState stableState1_, final StableState stableState2_)
        {
            this(lNum, cmd_);

            stableState = new StableState[] { stableState1_, stableState2_ };
        }

        public CmdPar(final int lNum, final Command cmd_, final int[] length_, final long[] value_, final long[] mask_, final boolean linked_)
        {
            this(lNum, cmd_);

            linked = linked_;
            length = length_;
            value  = value_;
            mask   = mask_;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public CmdPar(final int lNum, final TRSTMode trstMode_)
        {
            this(lNum, Command.TRST);

            trstMode = trstMode_;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public CmdPar(final int lNum, final PathState[] pathState_, final StableState stableState_)
        {
            this(lNum, Command.STATE);

            pathState   = pathState_;
            stableState = new StableState[] { stableState_ };
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public CmdPar(final int lNum, final StableState run_state_, final long run_count_, final int run_clk_, final long min_time_, final long max_time_, final StableState end_state_)
        {
            this(lNum, Command.RUNTEST1);

            value       = new long       [] { run_count_, run_clk_  , min_time_, max_time_ };
            stableState = new StableState[] { run_state_, end_state_                       };
        }

        public CmdPar(final int lNum, final StableState run_state_, final long min_time_, final long max_time_, final StableState end_state_)
        {
            this(lNum, Command.RUNTEST2);

            value       = new long       [] { min_time_ , max_time_  };
            stableState = new StableState[] { run_state_, end_state_ };
        }

    } // class CmdPar

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private       String            _filePath = null;
    private final ArrayList<CmdPar> _cmdPar   = new ArrayList<>();
    private       long              _bitCnt   = 0;
    private       int               _byteSize = 0;

    private void _accumulateBitCnt(final int bitLength)
    { _bitCnt += bitLength; }

    private void _accumulateBitCnt(final int[] bitLength)
    { for(final int bl : bitLength) _accumulateBitCnt(bl); }

    private void _calcEffByteSize()
    {
        _byteSize = (int) ( (_bitCnt + 7L) & ~7L );
        _byteSize = (_byteSize / 8 + 1) & ~1;
    }

    public int getEffByteSize()
    { return _byteSize; }

    public ArrayList<CmdPar> getCmdPar()
    { return _cmdPar; }

    public String getFilePath()
    { return _filePath; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _throwSyntaxError(final String filePath, final int lineNumber, final String lineString) throws IOException
    { throw XCom.newIOException(Texts.EMsg_SyntaxErrorCommandFLine, filePath, lineNumber, lineString); }

    private void _throwUnsupported(final String filePath, final int lineNumber, final String lineString) throws IOException
    { throw XCom.newIOException(Texts.EMsg_UnsupportedCommandFLine, filePath, lineNumber, lineString); }

    private void _throw_throwUnsupportedParam(final String filePath, final int lineNumber, final String lineString) throws IOException
    { throw XCom.newIOException(Texts.EMsg_UnsuppoParmCommandFLine, filePath, lineNumber, lineString); }

    private void _throwTooManyParam(final String filePath, final int lineNumber, final String lineString) throws IOException
    { throw XCom.newIOException(Texts.EMsg_TooManyParmCommandFLine, filePath, lineNumber, lineString); }

    private void _throwInvalidParam(final String filePath, final int lineNumber, final String lineString) throws IOException
    { throw XCom.newIOException(Texts.EMsg_InvalidParmCommandFLine, filePath, lineNumber, lineString); }

    private void _throwInvalidStoredHeader(final String filePath, final int lineNumber, final String lineString) throws IOException
    { throw XCom.newIOException(Texts.EMsg_InvalidSHdrCommandFLine, filePath, lineNumber, lineString); }

    private void _throwInvalidStoredTrailer(final String filePath, final int lineNumber, final String lineString) throws IOException
    { throw XCom.newIOException(Texts.EMsg_InvalidSTrlCommandFLine, filePath, lineNumber, lineString); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _hdrLen; private long[] _hdrTDI, _hdrTDIMask, _hdrTDO, _hdrTDOMask;
    private int[] _hirLen; private long[] _hirTDI, _hirTDIMask, _hirTDO, _hirTDOMask;

    private int[] _tdrLen; private long[] _tdrTDI, _tdrTDIMask, _tdrTDO, _tdrTDOMask;
    private int[] _tirLen; private long[] _tirTDI, _tirTDIMask, _tirTDO, _tirTDOMask;

    private void _reset_hdr()
    {
        _hdrLen     = null;
        _hdrTDI     = null;
        _hdrTDIMask = null;
        _hdrTDO     = null;
        _hdrTDOMask = null;
    }

    private void _reset_hir()
    {
        _hirLen     = null;
        _hirTDI     = null;
        _hirTDIMask = null;
        _hirTDO     = null;
        _hirTDOMask = null;
    }

    private void _reset_tdr()
    {
        _tdrLen     = null;
        _tdrTDI     = null;
        _tdrTDIMask = null;
        _tdrTDO     = null;
        _tdrTDOMask = null;
    }

    private void _reset_tir()
    {
        _tirLen     = null;
        _tirTDI     = null;
        _tirTDIMask = null;
        _tirTDO     = null;
        _tirTDOMask = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long _parseDec(final String str)
    { return Math.round( Double.parseDouble(str) ); }

    private long _parseDec1M(final String str)
    { return Math.round( Double.parseDouble(str) * 1000000.0 ); }

    private long _parseHex(final String str)
    { return ( new java.math.BigInteger(str, 16) ).longValue(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class _TDIOM {

        public ArrayList<Integer> tdiLen  = null;
        public ArrayList<Long   > tdiVal  = null;
        public ArrayList<Long   > tdiMask = null;

        public ArrayList<Integer> tdoLen  = null;
        public ArrayList<Long   > tdoVal  = null;
        public ArrayList<Long   > tdoMask = null;

        public int elemCount()
        {
            return Math.max(
                Math.max( (tdiVal != null) ? tdiVal.size() : 0, (tdiMask != null) ? tdiMask.size() : 0 ),
                Math.max( (tdoVal != null) ? tdoVal.size() : 0, (tdoMask != null) ? tdoMask.size() : 0 )
            );
        }

        public int [] a_tdiLen () { return (tdiLen  == null) ? null : tdiLen .stream().mapToInt (Integer:: intValue).toArray(); }
        public long[] a_tdiVal () { return (tdiVal  == null) ? null : tdiVal .stream().mapToLong(Long   ::longValue).toArray(); }
        public long[] a_tdiMask() { return (tdiMask == null) ? null : tdiMask.stream().mapToLong(Long   ::longValue).toArray(); }

        public int [] a_tdoLen () { return (tdoLen  == null) ? null : tdoLen .stream().mapToInt (Integer:: intValue).toArray(); }
        public long[] a_tdoVal () { return (tdoVal  == null) ? null : tdoVal .stream().mapToLong(Long   ::longValue).toArray(); }
        public long[] a_tdoMask() { return (tdoMask == null) ? null : tdoMask.stream().mapToLong(Long   ::longValue).toArray(); }

        public int [] a_tdxLen()
        {
            if( (tdiLen != null) && (tdoLen == null) ) return a_tdiLen();
            if( (tdiLen == null) && (tdoLen != null) ) return a_tdoLen();


            if( !tdiLen.equals(tdoLen) ) return null;

            return a_tdiLen();
        }

    } // class _TDIOM

    public static String[] _splitIntoChunks16RARev(final String str)
    {
        final ArrayList<String> chunks = new ArrayList<>();
              int               cIdx   = str.length();

        while(cIdx > 0) {
            final int sIdx = Math.max(0, cIdx - 16);
            chunks.add( 0, str.substring(sIdx, cIdx) );
            cIdx = sIdx;
        }

        Collections.reverse(chunks);

        return chunks.toArray( new String[0] );
    }

    private boolean _processLenTDx(final String value, final int split, final int lplen, final ArrayList<Integer> len, final ArrayList<Long> tdx)
    {
        final String[] chunks = _splitIntoChunks16RARev(value);

        // ##### !!! TODO : Remove leading zero(es) as needed !!! #####

        if(chunks.length != split) return false;

        for(int c = 0; c < split; ++c) {
            if(c == split - 1) {
              //if( chunks[c].length() * 4 < lplen ) return false;
                len.add( lplen );
            }
            else {
                len.add( 64 );
            }
                tdx.add( _parseHex( chunks[c] ) );
        }

        return true;
    }

    private boolean _processTDxMask(final String value, final int split, final ArrayList<Long> tdxMask)
    {
        final String[] chunks = _splitIntoChunks16RARev(value);

        // ##### !!! TODO : Remove leading zero(es) as needed !!! #####

        if(chunks.length != split) return false;

        for(int c = 0; c < split; ++c) {
            tdxMask.add( _parseHex( chunks[c] ) );
        }

        return true;
    }

    private _TDIOM _parseTDIOM(final String[] tokens, final int length)
    {
        // <cmd> <length> [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)]

        // Prepare the result storage
        final _TDIOM res = new _TDIOM();

        // Determine the number of split
        final int inc   = ( (length % 64) == 0 ) ? 0 : 1;
        final int split =   (length / 64) + inc;

        // Determine the length of the last part
        final int lplen = length - (split * 64) + 64;

        // Process the tokens
        int i = 2;

        while(i < tokens.length) {

            final String type  = tokens[i++];
            final String value = tokens[i++];

            switch(type) {

                case "TDI" : if(true) {
                        if(res.tdiLen == null) res.tdiLen = new ArrayList<>();
                        if(res.tdiVal == null) res.tdiVal = new ArrayList<>();
                        if( !_processLenTDx(value, split, lplen, res.tdiLen, res.tdiVal) ) return null;
                    }
                    break;

                case "SMASK" : if(true) {
                        if(res.tdiMask == null) res.tdiMask = new ArrayList<>();
                        if( !_processTDxMask(value, split, res.tdiMask) ) return null;
                    }
                    break;

                case "TDO" : if(true) {
                        if(res.tdoLen == null) res.tdoLen = new ArrayList<>();
                        if(res.tdoVal == null) res.tdoVal = new ArrayList<>();
                        if( !_processLenTDx(value, split, lplen, res.tdoLen, res.tdoVal) ) return null;
                    }
                    break;

                case "MASK" : if(true) {
                        if(res.tdoMask == null) res.tdoMask = new ArrayList<>();
                        if( !_processTDxMask(value, split, res.tdoMask) ) return null;
                    }
                    break;

                default:
                    return null;

            } // switch

        } // while

        // Return the result
        return res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _putCmd_DIS_PBAR(final int lNum)
    { _cmdPar.add( new CmdPar(lNum, Command.__DIS_PBAR__) ); }

    private void _putCmd_PRINT(final int lNum, final String string)
    { _cmdPar.add( new CmdPar(lNum, Command.__PRINT__, string) ); }

    private void _putCmd_TAG(final int lNum, final String string)
    { _cmdPar.add( new CmdPar(lNum, Command.__TAG__, string) ); }

    private void _putCmd_EXIT(final int lNum)
    { _cmdPar.add( new CmdPar(lNum, Command.__EXIT__) ); }

    private void _putCmd_FREQUENCY(final int lNum, final long cycles)
    { _cmdPar.add( new CmdPar(lNum, Command.FREQUENCY, cycles) ); }

    private void _putCmd_TRST(final int lNum, final TRSTMode trstMode)
    { _cmdPar.add( new CmdPar(lNum, trstMode) ); }

    private void _putCmd_ENDDR(final int lNum, final StableState stableState)
    { _cmdPar.add( new CmdPar(lNum, Command.ENDDR, stableState) ); }

    private void _putCmd_ENDIR(final int lNum, final StableState stableState)
    { _cmdPar.add( new CmdPar(lNum, Command.ENDIR, stableState) ); }

    private void _putCmd_STATE(final int lNum, final PathState[] pathState, final StableState stableState)
    { _cmdPar.add( new CmdPar(lNum, pathState, stableState) ); }

    private void _putCmd_RUNTEST1(final int lNum, final StableState run_state, final long run_count, final int run_clk, final long min_time, final long max_time, final StableState end_state)
    { _cmdPar.add( new CmdPar(lNum, run_state, run_count, run_clk, min_time, max_time, end_state) ); }

    private void _putCmd_RUNTEST2(final int lNum, final StableState run_state, final long min_time, final long max_time, final StableState end_state)
    { _cmdPar.add( new CmdPar(lNum, run_state, min_time, max_time, end_state) ); }

    private boolean _putCmd_SxR_TDx_impl(final int lNum, final Command cmd, final int[] length, final long[] value, final long[] mask, final boolean linked)
    {
        if(                length.length != value.length) return false;
        if(mask != null && length.length != mask .length) return false;

        _cmdPar.add( new CmdPar(lNum, cmd, length, value, mask, linked) );

        return true;
    }

    private boolean _putCmd_SxR_TDx_impl(final int lNum, final Command cmd, final int length_, final long[] value, final long[] mask, final boolean linked)
    {
        if(mask != null && value.length != mask.length) return false;

        final int[] length = new int[value.length];
        Arrays.fill(length, length_);

        return _putCmd_SxR_TDx_impl(lNum, cmd, length, value, mask, linked);
    }

    private boolean _putCmd_SDR_TDI(final int lNum, final int[] length, final long[] value, final long[] mask)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SDR_TDI, length, value, mask, false); }

    private boolean _putCmd_SDR_TDI(final int lNum, final int length, final long[] value, final long[] mask)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SDR_TDI, length, value, mask, false); }

    private boolean _putCmd_SDR_TDI(final int lNum, final int length, final long value, final long mask)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SDR_TDI, new int[] { length }, new long[] { value }, new long[] { mask }, false); }

    private boolean _putCmd_SDR_TDO(final int lNum, final int[] length, final long[] value, final long[] mask, final boolean linked)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SDR_TDO, length, value, mask, linked); }

    private boolean _putCmd_SDR_TDO(final int lNum, final int length, final long[] value, final long[] mask, final boolean linked)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SDR_TDO, length, value, mask, linked); }

    private boolean _putCmd_SDR_TDO(final int lNum, final int length, final long value, final long mask, final boolean linked)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SDR_TDO, new int[] { length }, new long[] { value }, new long[] { mask }, linked); }

    private boolean _putCmd_SIR_TDI(final int lNum, final int[] length, final long[] value, final long[] mask)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SIR_TDI, length, value, mask, false); }

    private boolean _putCmd_SIR_TDI(final int lNum, final int length, final long[] value, final long[] mask)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SIR_TDI, length, value, mask, false); }

    private boolean _putCmd_SIR_TDI(final int lNum, final int length, final long value, final long mask)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SIR_TDI, new int[] { length }, new long[] { value }, new long[] { mask }, false); }

    private boolean _putCmd_SIR_TDO(final int lNum, final int[] length, final long[] value, final long[] mask, final boolean linked)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SIR_TDO, length, value, mask, linked); }

    private boolean _putCmd_SIR_TDO(final int lNum, final int length, final long[] value, final long[] mask, final boolean linked)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SIR_TDO, length, value, mask, linked); }

    private boolean _putCmd_SIR_TDO(final int lNum, final int length, final long value, final long mask, final boolean linked)
    { return _putCmd_SxR_TDx_impl(lNum, Command.SIR_TDO, new int[] { length }, new long[] { value }, new long[] { mask }, linked); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadSVF(final String filePath) throws IOException
    {
        // Open the file and prepare the pattern
        final BufferedReader btr = new BufferedReader( new InputStreamReader ( new FileInputStream (filePath), SysUtil._CharEncoding ) );
        final Pattern        pat = Pattern.compile("[\\s\\t();]");
        final Pattern        sws = Pattern.compile("^[\\s\\t].*");
        final Pattern        ews = Pattern.compile(".*[\\s\\t]$");
        final Pattern        sdg = Pattern.compile("^[0-9].*"   );
        final Pattern        edg = Pattern.compile(".*[0-9]$"   );

        // Clear the data
        _cmdPar.clear();

        _bitCnt   = 0;
        _filePath = null;

        // Reset the header and trailer
        _reset_hdr(); _reset_hir();
        _reset_tdr(); _reset_tir();

        // Process the file
        int lNum = 0;

        while(true) {

            String origLine = "";
            String combLine = "";

            while( !combLine.trim().endsWith(";") ) { // Combine lines as needed

                // Read one line
                String orgLine = btr.readLine();

                String getLine = orgLine;
                if(getLine == null) {
                    combLine = null;
                    break;
                }
                ++lNum;

                // Strip the comment
                final int len  = getLine.length();
                final int idx1 = getLine.indexOf("//");
                final int idx2 = getLine.indexOf("!" );
                final int idx  = Math.min( (idx1 != -1) ? idx1 : len, (idx2 != -1) ? idx2 : len );

                if(idx != len) getLine = getLine.substring(0, idx).trim();

                if( getLine.isEmpty() ) continue;

                // Combine the line
                if( !origLine.isEmpty() ) {
                    if( !ews.matcher(origLine).matches() && !sws.matcher(orgLine).matches() ) origLine += ' ';
                }
                origLine += orgLine;

                if( !combLine.isEmpty() ) {
                    if( edg.matcher(combLine).matches() && !sdg.matcher(getLine).matches() ) {
                        combLine += ' ';
                    }
                    else if( !edg.matcher(combLine).matches() && sdg.matcher(getLine).matches() ) {
                        final int idxLP = combLine.lastIndexOf('(');
                        final int idxRP = combLine.lastIndexOf(')');
                        if( (idxLP < 0) || (idxLP > 0 && idxRP > 0 && idxLP < idxRP) ) combLine += ' ';
                    }
                }
                combLine += getLine;

            } // while

            if( combLine == null   ) break;
            if( combLine.isEmpty() ) continue;

            // Tokenize
            final String[] tokens = Arrays.stream( pat.split( combLine.toUpperCase() ) ).map    ( String::trim      )
                                                                                        .filter ( v -> !v.isEmpty() )
                                                                                        .toArray( String[]::new     );
            final int      tlen   = (tokens == null) ? 0 : tokens.length;

            if(tlen == 0) continue;

            /*
            for(final String s : tokens) SysUtil.stdDbg().printf("'%s'\n", s);
            //*/

            // Process according to the command token
            try {

                switch( tokens[0] ) {

                    case "FREQUENCY" : if(true) {
                            /* FREQUENCY [cycles HZ] */
                            if( tlen != 1 && tlen != 3               ) _throwSyntaxError(filePath, lNum, origLine);
                            if( tlen == 3 && !tokens[2].equals("HZ") ) _throwSyntaxError(filePath, lNum, origLine);
                            _putCmd_FREQUENCY( lNum, (tlen == 1) ? 0 : _parseDec( tokens[1] ) );
                        }
                        break;

                    case "TRST" : if(true) {
                            /* TRST trst_mode */
                            if( tlen != 2                              ) _throwSyntaxError (filePath, lNum, origLine);
                            if( !TRSTMode.vals().contains( tokens[1] ) ) _throwInvalidParam(filePath, lNum, origLine);
                            _putCmd_TRST( lNum, TRSTMode.valueOf( tokens[1] ) );
                        }
                        break;

                    case "ENDDR" : if(true) {
                            /* ENDDR stable_state */
                            if( tlen != 2                                 ) _throwSyntaxError (filePath, lNum, origLine);
                            if( !StableState.vals().contains( tokens[1] ) ) _throwInvalidParam(filePath, lNum, origLine);
                            _putCmd_ENDDR( lNum, StableState.valueOf( tokens[1] ) );
                        }
                        break;

                    case "ENDIR" : if(true) {
                            /* ENDIR stable_state */
                            if( tlen != 2                                 ) _throwSyntaxError (filePath, lNum, origLine);
                            if( !StableState.vals().contains( tokens[1] ) ) _throwInvalidParam(filePath, lNum, origLine);
                            _putCmd_ENDIR( lNum, StableState.valueOf( tokens[1] ) );
                        }
                        break;

                    case "STATE" : if(true) {
                            /* STATE [pathstate...] stable_state */
                            if( tlen < 2 ) _throwSyntaxError (filePath, lNum, origLine);
                            final PathState[] ps = (tlen > 2) ? ( new PathState[tlen - 2] ) : null;
                            if(ps != null) {
                                for(int p = 1; p < tlen - 2; ++p) {
                                    if( !PathState.vals().contains( tokens[p] ) ) _throwInvalidParam(filePath, lNum, origLine);
                                    ps[p - 1] = PathState.valueOf( tokens[p] );
                                }
                            }
                            _putCmd_STATE( lNum, ps, StableState.valueOf( tokens[tlen - 1] ) );
                        }
                        break;

                    case "HDR" : if(true) {
                            /* HDR length [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)] */
                            if(tlen < 2) _throwSyntaxError(filePath, lNum, origLine);
                            final int length = (int) _parseDec( tokens[1] );
                            _reset_hdr();
                            if(length != 0) {
                                final _TDIOM res = _parseTDIOM(tokens, length);
                                if(res == null)_throwInvalidParam(filePath, lNum, origLine);
                                                        _hdrLen     = res.a_tdxLen ();
                                if(res.tdiVal  != null) _hdrTDI     = res.a_tdiVal ();
                                if(res.tdiMask != null) _hdrTDIMask = res.a_tdiMask();
                                if(res.tdoVal  != null) _hdrTDO     = res.a_tdoVal ();
                                if(res.tdoMask != null) _hdrTDOMask = res.a_tdoMask();
                            }
                        }
                        break;

                    case "HIR" : if(true) {
                            /* HIR length [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)] */
                            if(tlen < 2) _throwSyntaxError(filePath, lNum, origLine);
                            final int length = (int) _parseDec( tokens[1] );
                            _reset_hir();
                            if(length != 0) {
                                final _TDIOM res = _parseTDIOM(tokens, length);
                                if(res == null)_throwInvalidParam(filePath, lNum, origLine);
                                                        _hirLen     = res.a_tdxLen ();
                                if(res.tdiVal  != null) _hirTDI     = res.a_tdiVal ();
                                if(res.tdiMask != null) _hirTDIMask = res.a_tdiMask();
                                if(res.tdoVal  != null) _hirTDO     = res.a_tdoVal ();
                                if(res.tdoMask != null) _hirTDOMask = res.a_tdoMask();
                            }
                        }
                        break;

                    case "TDR" : if(true) {
                            /* TDR length [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)] */
                            if(tlen < 2) _throwSyntaxError(filePath, lNum, origLine);
                            final int length = (int) _parseDec( tokens[1] );
                            _reset_tdr();
                            if(length != 0) {
                                final _TDIOM res = _parseTDIOM(tokens, length);
                                if(res == null)_throwInvalidParam(filePath, lNum, origLine);
                                                        _tdrLen     = res.a_tdxLen ();
                                if(res.tdiVal  != null) _tdrTDI     = res.a_tdiVal ();
                                if(res.tdiMask != null) _tdrTDIMask = res.a_tdiMask();
                                if(res.tdoVal  != null) _tdrTDO     = res.a_tdoVal ();
                                if(res.tdoMask != null) _tdrTDOMask = res.a_tdoMask();
                            }
                        }
                        break;

                    case "TIR" : if(true) {
                            /* TIR length [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)] */
                            if(tlen < 2) _throwSyntaxError(filePath, lNum, origLine);
                            final int length = (int) _parseDec( tokens[1] );
                            _reset_tir();
                            if(length != 0) {
                                final _TDIOM res = _parseTDIOM(tokens, length);
                                if(res == null)_throwInvalidParam(filePath, lNum, origLine);
                                                        _tirLen     = res.a_tdxLen ();
                                if(res.tdiVal  != null) _tirTDI     = res.a_tdiVal ();
                                if(res.tdiMask != null) _tirTDIMask = res.a_tdiMask();
                                if(res.tdoVal  != null) _tirTDO     = res.a_tdoVal ();
                                if(res.tdoMask != null) _tirTDOMask = res.a_tdoMask();
                            }
                        }
                        break;

                    case "SDR" : if(true) {
                            /* SDR length [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)] */
                            if(tlen   <  2) _throwSyntaxError (filePath, lNum, origLine);
                            final int length = (int) _parseDec( tokens[1] );
                            if(length <= 0) _throwInvalidParam(filePath, lNum, origLine);
                            final _TDIOM res = _parseTDIOM(tokens, length);
                            if(res == null)_throwInvalidParam (filePath, lNum, origLine);
                            if(res.tdiVal != null) {
                                if( _hdrLen != null ) {
                                    if( !_putCmd_SDR_TDI( lNum, _hdrLen        , _hdrTDI       , _hdrTDIMask            ) ) _throwInvalidStoredHeader (filePath, lNum, origLine);
                                    _accumulateBitCnt   (       _hdrLen                                                 );
                                }
                                    if( !_putCmd_SDR_TDI( lNum, res.a_tdxLen(), res.a_tdiVal(), res.a_tdiMask()         ) ) _throwInvalidParam        (filePath, lNum, origLine);
                                    _accumulateBitCnt   (       res.a_tdxLen()                                          );
                                if( _tdrLen != null ) {
                                    if( !_putCmd_SDR_TDI( lNum, _tdrLen        , _tdrTDI       , _tdrTDIMask            ) ) _throwInvalidStoredTrailer(filePath, lNum, origLine);
                                    _accumulateBitCnt   (       _tdrLen                                                 );
                                }
                            }
                            if(res.tdoVal != null) {
                                final boolean linked = (res.tdiVal != null);
                                if( _hdrLen != null ) {
                                    if( !_putCmd_SDR_TDO( lNum, _hdrLen       , _hdrTDO       , _hdrTDOMask    , linked ) ) _throwInvalidStoredHeader (filePath, lNum, origLine);
                                    _accumulateBitCnt   (       _hdrLen                                                 );
                                }
                                    if( !_putCmd_SDR_TDO( lNum, res.a_tdxLen(), res.a_tdoVal(), res.a_tdoMask(), linked ) ) _throwInvalidParam        (filePath, lNum, origLine);
                                    _accumulateBitCnt   (       res.a_tdxLen()                                          );
                                if( _tdrLen != null ) {
                                    if( !_putCmd_SDR_TDO( lNum, _tdrLen       , _tdrTDO       , _tdrTDOMask    , linked ) ) _throwInvalidStoredTrailer(filePath, lNum, origLine);
                                    _accumulateBitCnt   (       _tdrLen                                                 );
                                }
                            }
                        }
                        break;

                    case "SIR" : if(true) {
                            /* SIR length [TDI (tdi)] [TDO (tdo)] [MASK (mask)] [SMASK (smask)] */
                            if(tlen   <  2) _throwSyntaxError (filePath, lNum, origLine);
                            final int length = (int) _parseDec( tokens[1] );
                            if(length <= 0) _throwInvalidParam(filePath, lNum, origLine);
                            final _TDIOM res = _parseTDIOM(tokens, length);
                            if(res == null)_throwInvalidParam (filePath, lNum, origLine);
                            if(res.tdiVal != null) {
                                if( _hirLen != null ) {
                                    if( !_putCmd_SIR_TDI( lNum, _hirLen       , _hirTDI       , _hirTDIMask             ) ) _throwInvalidStoredHeader (filePath, lNum, origLine);
                                    _accumulateBitCnt   (       _hirLen                                                 );
                                }
                                    if( !_putCmd_SIR_TDI( lNum, res.a_tdxLen(), res.a_tdiVal(), res.a_tdiMask()         ) ) _throwInvalidParam        (filePath, lNum, origLine);
                                    _accumulateBitCnt   (       res.a_tdxLen()                                          );
                                if( _tirLen != null ) {
                                    if( !_putCmd_SIR_TDI( lNum, _tirLen       , _tirTDI       , _tirTDIMask             ) ) _throwInvalidStoredTrailer(filePath, lNum, origLine);
                                    _accumulateBitCnt   (       _tirLen                                                 );
                                }
                            }
                            if(res.tdoVal != null) {
                                final boolean linked = (res.tdiVal != null);
                                if( _hirLen != null ) {
                                    if( !_putCmd_SIR_TDO( lNum, _hirLen       , _hirTDO       , _hirTDOMask    , linked ) ) _throwInvalidStoredHeader (filePath, lNum, origLine);
                                    _accumulateBitCnt   (       _hirLen                                                 );
                                }
                                    if( !_putCmd_SIR_TDO( lNum, res.a_tdxLen(), res.a_tdoVal(), res.a_tdoMask(), linked ) ) _throwInvalidParam        (filePath, lNum, origLine);
                                    _accumulateBitCnt   (       res.a_tdxLen()                                          );
                                if( _tirLen != null ) {
                                    if( !_putCmd_SIR_TDO( lNum, _tirLen       , _tirTDO       , _tirTDOMask    , linked ) ) _throwInvalidStoredTrailer(filePath, lNum, origLine);
                                    _accumulateBitCnt   (       _tirLen                                                 );
                                }
                            }
                        }
                        break;

                    case "RUNTEST" : if(true) {
                            /* [1] RUNTEST [run_state] run_count run_clk [ min_time SEC [MAXIMUM max_time SEC] ] [ENDSTATE end_state] */
                            /* [2] RUNTEST [run_state]                     min_time SEC [MAXIMUM max_time SEC]   [ENDSTATE end_state] */
                            if( tlen < 3 ) _throwSyntaxError (filePath, lNum, origLine);
                            if( Arrays.stream(tokens).anyMatch( v -> v.equals("TCK") || v.equals("SCK") ) ) { // [1]
                                StableState run_state = StableState.__NS__;
                                long        run_count = -1;
                                int         run_clk   = -1;
                                long        min_time  = -1;
                                long        max_time  = -1;
                                StableState end_state = StableState.__NS__;
                                int         tidx      = 1;
                                     if( StableState.vals().contains( tokens[tidx] ) ) run_state = StableState.valueOf( tokens[tidx++] );
                                                                                       run_count = _parseDec          ( tokens[tidx++] );
                                     if( tokens[tidx  ].equals("TCK"    ) )            run_clk   = CmdPar.TCK;
                                else if( tokens[tidx  ].equals("SCK"    ) )            run_clk   = CmdPar.SCK;
                                else                                                   _throwInvalidParam(filePath, lNum, origLine);
                                              ++tidx;
                                if(tidx     < tlen    ) {
                                                                                       min_time  = _parseDec1M        ( tokens[tidx++] );
                                     if( !tokens[tidx++].equals("SEC"    ) )           _throwInvalidParam(filePath, lNum, origLine);
                                }
                                if(tidx     < tlen    ) {
                                     if( !tokens[tidx++].equals("MAXIMUM") )           _throwInvalidParam(filePath, lNum, origLine);
                                                                                       max_time  = _parseDec1M        ( tokens[tidx++] );
                                     if( !tokens[tidx++].equals("SEC"    ) )           _throwInvalidParam(filePath, lNum, origLine);
                                }
                                else {
                                                                                       max_time  = min_time;
                                }
                                if(tidx     < tlen    ) {
                                     if( StableState.vals().contains( tokens[tidx] ) ) end_state = StableState.valueOf( tokens[tidx++] );
                                     else                                              _throwInvalidParam(filePath, lNum, origLine);
                                }
                                if(tidx     < tlen    )                                _throwTooManyParam(filePath, lNum, origLine);
                                if(max_time < min_time)                                _throwInvalidParam(filePath, lNum, origLine);
                                _putCmd_RUNTEST1(lNum, run_state, run_count, run_clk, min_time, max_time, end_state);
                            }
                            else { // [2]
                                StableState run_state = StableState.__NS__;
                                long        min_time  = -1;
                                long        max_time  = -1;
                                StableState end_state = StableState.__NS__;
                                int         tidx      = 1;
                                     if( StableState.vals().contains( tokens[tidx] ) ) run_state = StableState.valueOf( tokens[tidx++] );
                                                                                       min_time  = _parseDec1M        ( tokens[tidx++] );
                                     if( !tokens[tidx++].equals("SEC"    ) )           _throwInvalidParam(filePath, lNum, origLine);
                                if(tidx     < tlen    ) {
                                     if( !tokens[tidx++].equals("MAXIMUM") )           _throwInvalidParam(filePath, lNum, origLine);
                                                                                       max_time  = _parseDec1M        ( tokens[tidx++] );
                                     if( !tokens[tidx++].equals("SEC"    ) )           _throwInvalidParam(filePath, lNum, origLine);
                                }
                                else {
                                                                                       max_time  = min_time;
                                }
                                if(tidx     < tlen    ) {
                                     if( StableState.vals().contains( tokens[tidx] ) ) end_state = StableState.valueOf( tokens[tidx++] );
                                     else                                              _throwInvalidParam(filePath, lNum, origLine);
                                }
                                if(tidx     < tlen    )                                _throwTooManyParam(filePath, lNum, origLine);
                                if(max_time < min_time)                                _throwInvalidParam(filePath, lNum, origLine);
                                _putCmd_RUNTEST2(lNum, run_state, min_time, max_time, end_state);
                            }
                        }
                        break;

                    /*
                     * ##### ??? TODO ??? #####
                     *     PIOMAP ( <direction logical_name>... ) ;
                     *           IN OUT INOUT
                     *     PIO (vector_string) ;
                     *           H L Z   U D X
                     * Use the GPIO?
                     */

                    ////////////////////////////////////////////////////////////////////////////////////////////////////

                    case "DIS" : if(true) {
                            /* DIS PBAR */
                            if( tlen != 2 || !"PBAR".equals(tokens[1]) ) _throwSyntaxError(filePath, lNum, origLine);
                            _putCmd_DIS_PBAR(lNum);
                        }
                        break;

                    case "PRINT" : /* FALLTHROUGH */
                    case "TAG"   : if(true) {
                            /* PRINT ["user_string"] */
                            /* TAG   ["user_string"] */
                            if(tlen == 1) {
                                _putCmd_TAG(lNum, null);
                            }
                            else {
                                 String str = pat.split(combLine, 2)[1].trim();
                                 if( !str.endsWith  (";" ) ) _throwSyntaxError(filePath, lNum, origLine);
                                 str = str.substring( 0, str.length() - 1 ).trim(); // Remove the ';'
                                 if( !str.startsWith("\"") ) _throwSyntaxError(filePath, lNum, origLine);
                                 if( !str.endsWith  ("\"") ) _throwSyntaxError(filePath, lNum, origLine);
                                 str = str.substring( 1, str.length() - 1 ); // Remove the '"'s
                                 if( tokens[0].equals("PRINT") ) _putCmd_PRINT( lNum, str.isEmpty() ? null : str );
                                 else                            _putCmd_TAG  ( lNum, str.isEmpty() ? null : str );
                            }
                        }
                        break;

                    case "EXIT" : if(true) {
                            /* EXIT */
                            if(tlen != 1) _throwSyntaxError(filePath, lNum, origLine);
                            _putCmd_EXIT(lNum);
                        }
                        break;

                    default:
                        _throwUnsupported(filePath, lNum, origLine);

                } // switch

            }
            catch(final IOException ioe) {
                throw ioe;
            }
            catch(final Exception e) {
                _throwSyntaxError( filePath, lNum, e.toString() );
            }

        } // while

        // Calculate the byte size of the effective stream data
        _calcEffByteSize();

        // Save the file path
        _filePath = filePath;

        // Close the file
        btr.close();
    }

} // class JTAGBitstream

