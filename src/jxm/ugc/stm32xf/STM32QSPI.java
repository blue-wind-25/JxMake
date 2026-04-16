/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.stm32xf;


import jxm.*;
import jxm.ugc.*;

import static jxm.ugc.SWDExecInstOpcode.XVI;


/*
 * This class and its related classes are written partially based on the algorithms and information found from:
 *
 *     AN4760
 *     Quad-SPI Interface on STM32 Microcontrollers and Microprocessors
 *     https://www.st.com/resource/en/application_note/an4760-quadspi-interface-on-stm32-microcontrollers-and-microprocessors--stmicroelectronics.pdf
 *
 *     ----------------------------------------------------------------------------------------------------
 *
 *     STM32H7xx MCUs Startup Code
 *     https://github.com/elzoughby/STM32H7xx-Startup/tree/master
 *
 *     "Bare Metal" STM32 Programming (Part 12): Using Quad-SPI Flash Memory
 *     https://vivonomicon.com/2020/08/08/bare-metal-stm32-programming-part-12-using-quad-spi-flash-memory
 *     https://github.com/WRansohoff/STM32F723E_QSPI_Example
 *
 *     W25Qxxx QSPI STM32 Library
 *     https://github.com/Crazy-Geeks/STM32-W25Q-QSPI
 *
 * ~~~ Last accessed & checked on 2024-06-25 ~~~
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * Please refer to the comments blocks before the definition of 'ProgSWD' and 'SWDFlashLoaderSTM32' classes
 * in the 'ProgSWD.java' and 'SWDFlashLoaderSTM32.java' files for more details and information.
 */
public abstract class STM32QSPI extends ARMCortexMCommonInst {

    protected final SWDExecInstBuilder _ib = new SWDExecInstBuilder();

    protected final SWDExecInst        _swdExecInst;

    protected final long               _flashCapacityBytes;
    protected final int                _flashPageSizeBytes;

    protected final boolean            _enableMemoryMapped;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("this-escape")
    protected STM32QSPI(final SWDExecInst swdExecInst, final STM32QSPIPin qspiPin, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped)
    {
        _instruction_resetHaltInitClock = _instruction_resetHaltInitClock(qspiPin, flashCapacityBytes, flashPageSizeBytes, enableMemoryMapped);
        _instruction_initGPIO_XF        = _instruction_initGPIO_XF       (qspiPin, flashCapacityBytes, flashPageSizeBytes, enableMemoryMapped);

        _instruction_deinitAll          = _instruction_deinitAll         (qspiPin, flashCapacityBytes, flashPageSizeBytes, enableMemoryMapped);

        _swdExecInst                    = swdExecInst;

        _flashCapacityBytes             = flashCapacityBytes;
        _flashPageSizeBytes             = flashPageSizeBytes;
        _enableMemoryMapped             = enableMemoryMapped;
    }

    public long    flashCapacityBytes() { return _flashCapacityBytes; }
    public int     flashPageSizeBytes() { return _flashPageSizeBytes; }

    public boolean mmapEnabled       () { return _enableMemoryMapped; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Because SWD reads  in 32 bits, the received    bytes will be packed/padded to multiple of 4 bytes.
     *        # Because SWD writes in 32 bits, the transmitted bytes must be packed/padded to multiple of 4 bytes.
     */

    public abstract void geninstruction_qspiCommand(final QSPICmd_Common cmd) throws JXMFatalLogicError;

    public abstract void geninstruction_qspiAbort() throws JXMFatalLogicError;

    public abstract void geninstruction_qspiMemoryMapped(final QSPICmd_Common cmd) throws JXMFatalLogicError;

    public abstract XVI geninstruction_qspiReceiveDB(final XVI xviCurOperInitFlag) throws JXMFatalLogicError;
    public abstract XVI geninstruction_qspiReceiveDB(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError;

    public abstract XVI geninstruction_qspiReceiveSB(final XVI xviCurOperInitFlag) throws JXMFatalLogicError;
    public abstract XVI geninstruction_qspiReceiveSB(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError;

    public abstract XVI geninstruction_qspiReceiveDMA(final XVI xviCurOperInitFlag) throws JXMFatalLogicError;
    public abstract XVI geninstruction_qspiReceiveDMA(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError;

    public abstract XVI geninstruction_qspiTransmitDB(final XVI xviCurOperInitFlag) throws JXMFatalLogicError;
    public abstract XVI geninstruction_qspiTransmitDB(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError;

    public abstract XVI geninstruction_qspiTransmitSB(final XVI xviCurOperInitFlag) throws JXMFatalLogicError;
    public abstract XVI geninstruction_qspiTransmitSB(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError;

    public abstract XVI geninstruction_qspiTransmitDMA(final XVI xviCurOperInitFlag) throws JXMFatalLogicError;
    public abstract XVI geninstruction_qspiTransmitDMA(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError;

    public SWDExecInstBuilder swdExecInstBuilder()
    { return _ib; }

    public SWDExecInst swdExecInst()
    { return _swdExecInst; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract long[][] _instruction_resetHaltInitClock(final STM32QSPIPin qspiPin, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped);
    protected abstract long[][] _instruction_initGPIO_XF(final STM32QSPIPin qspiPin, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped);

    protected abstract long[][] _instruction_deinitAll(final STM32QSPIPin qspiPin, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped);

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public final long[][] _instruction_resetHaltInitClock;
    public final long[][] _instruction_initGPIO_XF;

    public final long[][] _instruction_deinitAll;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum FuncMode { _None, _IndirectWrite, _IndirectRead, _AutoPolling, _MemoryMapped }

    public static enum InstMode { _None, _1Line, _2Lines, _4Lines }
    public static enum AddrMode { _None, _1Line, _2Lines, _4Lines }
    public static enum AltBMode { _None, _1Line, _2Lines, _4Lines }
    public static enum DataMode { _None, _1Line, _2Lines, _4Lines }

    public static enum AddrSize { _None, _08Bits, _16Bits, _24Bits, _32Bits }
    public static enum AltBSize { _None, _08Bits, _16Bits, _24Bits, _32Bits }

    public static abstract class QSPICmd_Common {

        public FuncMode funcMode         = FuncMode._None;

        public InstMode instMode         = InstMode._None;
        public long     instValue        = 0;
        public boolean  instSIOO         = false;

        public AddrMode addrMode         = AddrMode._None;
        public AddrSize addrSize         = AddrSize._None;
        public long     addrValue        = 0;

        public AltBMode altbMode         = AltBMode._None;
        public AltBSize altbSize         = AltBSize._None;
        public long     altbValue        = 0;

        public DataMode dataMode         = DataMode._None;
        public int      dataLength       = 0;

        public int      dummyCycles      = 0;

        public boolean  ddrMode          = false;
        public boolean  ddrHoldHalfCycle = false;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public abstract long funcModeBits() throws JXMFatalLogicError;
        public abstract long instModeBits() throws JXMFatalLogicError;
        public abstract long instSIOOBits();
        public abstract long addrModeBits() throws JXMFatalLogicError;
        public abstract long addrSizeBits() throws JXMFatalLogicError;
        public abstract long altbModeBits() throws JXMFatalLogicError;
        public abstract long altbSizeBits() throws JXMFatalLogicError;
        public abstract long dataModeBits() throws JXMFatalLogicError;
        public abstract long dmyCycleBits();
        public abstract long ddrModeBits ();
        public abstract long ddrHoldBits ();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public QSPICmd_Common indirectWrite()
        {
            funcMode = FuncMode._IndirectWrite;
            return this;
        }

        public QSPICmd_Common indirectRead()
        {
            funcMode = FuncMode._IndirectRead;
            return this;
        }

        public QSPICmd_Common autoPolling()
        {
            funcMode = FuncMode._AutoPolling;
            return this;
        }

        public QSPICmd_Common memoryMapped()
        {
            funcMode = FuncMode._MemoryMapped;
            return this;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public long allConfigBits() throws JXMFatalLogicError
        { return instValue | funcModeBits() | instModeBits() | instSIOOBits() | addrModeBits() | addrSizeBits() | altbModeBits() | altbSizeBits() | dataModeBits() | dmyCycleBits() | ddrModeBits() | ddrHoldBits(); }

        public long allConfigBits_wo_addrSize() throws JXMFatalLogicError
        { return instValue | funcModeBits() | instModeBits() | instSIOOBits() | addrModeBits() |                  altbModeBits() | altbSizeBits() | dataModeBits() | dmyCycleBits() | ddrModeBits() | ddrHoldBits(); }

        public long allConfigBits_wo_altbSize() throws JXMFatalLogicError
        { return instValue | funcModeBits() | instModeBits() | instSIOOBits() | addrModeBits() | addrSizeBits() | altbModeBits() |                  dataModeBits() | dmyCycleBits() | ddrModeBits() | ddrHoldBits(); }

        public long allConfigBits_wo_addrSize_altbSize() throws JXMFatalLogicError
        { return instValue | funcModeBits() | instModeBits() | instSIOOBits() | addrModeBits() |                  altbModeBits() |                  dataModeBits() | dmyCycleBits() | ddrModeBits() | ddrHoldBits(); }

        public long allConfigBits_wo_instValue() throws JXMFatalLogicError
        { return             funcModeBits() | instModeBits() | instSIOOBits() | addrModeBits() | addrSizeBits() | altbModeBits() | altbSizeBits() | dataModeBits() | dmyCycleBits() | ddrModeBits() | ddrHoldBits(); }

        public long allConfigBits_wo_instValue_addrSize() throws JXMFatalLogicError
        { return             funcModeBits() | instModeBits() | instSIOOBits() | addrModeBits() |                  altbModeBits() | altbSizeBits() | dataModeBits() | dmyCycleBits() | ddrModeBits() | ddrHoldBits(); }

        public long allConfigBits_wo_instValue_altbSize() throws JXMFatalLogicError
        { return             funcModeBits() | instModeBits() | instSIOOBits() | addrModeBits() | addrSizeBits() | altbModeBits() |                  dataModeBits() | dmyCycleBits() | ddrModeBits() | ddrHoldBits(); }

        public long allConfigBits_wo_instValue_addrSize_altbSize() throws JXMFatalLogicError
        { return             funcModeBits() | instModeBits() | instSIOOBits() | addrModeBits() |                  altbModeBits() |                  dataModeBits() | dmyCycleBits() | ddrModeBits() | ddrHoldBits(); }

    } // QSPICmd_Common

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract QSPICmd_Common newQSPICmd();

    public QSPICmd_Common newQSPICmd( // Instruction
            final InstMode instMode,   final long     instValue ,  final boolean instSIOO
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ; cmd.instSIOO = instSIOO;
        return cmd;
    }

    public QSPICmd_Common newQSPICmd( // Instruction
            final InstMode instMode,   final long     instValue
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ;
        return cmd;
    }

    public QSPICmd_Common newQSPICmd( // Instruction - Data
            final InstMode instMode,   final long     instValue ,  final boolean instSIOO,
            final DataMode dataMode,   final int      dataLength
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ; cmd.instSIOO = instSIOO;
            cmd.dataMode = dataMode; cmd.dataLength = dataLength;
        return cmd;
    }

    public QSPICmd_Common newQSPICmd( // Instruction - Data
            final InstMode instMode,   final long     instValue ,
            final DataMode dataMode,   final int      dataLength
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ;
            cmd.dataMode = dataMode; cmd.dataLength = dataLength;
        return cmd;
    }


    public QSPICmd_Common newQSPICmd( // Instruction - Address - Data - Dummy Cycles
            final InstMode instMode,   final long     instValue ,     final boolean instSIOO   ,
            final AddrMode addrMode,   final AddrSize addrSize  ,     final long    addrValue  ,
            final DataMode dataMode,   final int      dataLength,     final int     dummyCycles
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ; cmd.instSIOO    = instSIOO   ;
            cmd.addrMode = addrMode; cmd.addrSize   = addrSize  ; cmd.addrValue   = addrValue  ;
            cmd.dataMode = dataMode; cmd.dataLength = dataLength; cmd.dummyCycles = dummyCycles;
        return cmd;
    }

    public QSPICmd_Common newQSPICmd( // Instruction - Address - Data - Dummy Cycles
            final InstMode instMode,   final long     instValue ,
            final AddrMode addrMode,   final AddrSize addrSize  ,     final long    addrValue  ,
            final DataMode dataMode,   final int      dataLength,     final int     dummyCycles
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ;
            cmd.addrMode = addrMode; cmd.addrSize   = addrSize  ; cmd.addrValue   = addrValue  ;
            cmd.dataMode = dataMode; cmd.dataLength = dataLength; cmd.dummyCycles = dummyCycles;
        return cmd;
    }

    public QSPICmd_Common newQSPICmd( // Instruction - Address - Data
            final InstMode instMode,   final long     instValue ,   final boolean instSIOO ,
            final AddrMode addrMode,   final AddrSize addrSize  ,   final long    addrValue,
            final DataMode dataMode,   final int      dataLength
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ; cmd.instSIOO  = instSIOO ;
            cmd.addrMode = addrMode; cmd.addrSize   = addrSize  ; cmd.addrValue = addrValue;
            cmd.dataMode = dataMode; cmd.dataLength = dataLength;
        return cmd;
    }

    public QSPICmd_Common newQSPICmd( // Instruction - Address - Data
            final InstMode instMode,   final long     instValue ,
            final AddrMode addrMode,   final AddrSize addrSize  ,   final long    addrValue,
            final DataMode dataMode,   final int      dataLength
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ;
            cmd.addrMode = addrMode; cmd.addrSize   = addrSize  ; cmd.addrValue = addrValue;
            cmd.dataMode = dataMode; cmd.dataLength = dataLength;
        return cmd;
    }

    public QSPICmd_Common newQSPICmd( // Instruction - Address - Data
            final InstMode instMode,   final long     instValue ,
            final AddrMode addrMode,   final AddrSize addrSize  ,
            final DataMode dataMode,   final int      dataLength
    ) {
        final QSPICmd_Common cmd = newQSPICmd();
            cmd.instMode = instMode; cmd.instValue  = instValue ;
            cmd.addrMode = addrMode; cmd.addrSize   = addrSize  ;
            cmd.dataMode = dataMode; cmd.dataLength = dataLength;
        return cmd;
    }

} // class STM32QSPI
