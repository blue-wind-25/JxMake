/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.UnsupportedEncodingException;

import java.util.Arrays;

import com.fazecast.jSerialComm.*;

import jxm.*;


public abstract class USB2GPIO {

    // NOTE : Although the IO buffers are declared as int[], they really are used to transfer UInt8 values

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static SysUtil.StringPrintStream _spsNotifyError = null;

    public static boolean redirectNotifyErrorToString(final boolean redirect)
    {
        // Disable redirection
        if(!redirect) {
            if(_spsNotifyError != null) _spsNotifyError = null;
            return true;
        }

        // Enable redirection
        try {
            _spsNotifyError = new SysUtil.StringPrintStream();
        }
        catch(final Exception e) {
            e.printStackTrace();
            _spsNotifyError = null;
        }

        return _spsNotifyError != null;
    }

    public static boolean isNotifyErrorRedirectedToString()
    { return _spsNotifyError != null; }

    // NOTE : This function will also delete the accumulated string
    public static String getNotifyErrorRedirectionString()
    {
        if(_spsNotifyError == null) return null;

        String string;

        try {
            string = _spsNotifyError.string();
        }
        catch(final Exception e) {
            e.printStackTrace();
            string = null;
        }

        _spsNotifyError.clear();

        return string;
    }

    public static boolean notifyError(final String errMsg, final Object... args)
    {
        ( (_spsNotifyError != null) ? _spsNotifyError.printStream() : SysUtil.stdDbg() ).println( String.format(errMsg, args) );

        return false; // Return false for convenience as most functions that call this function will also return false
    }

    // This class is used in multipart command sections to prevent writing duplicated cleanup code if an error occurs
    @SuppressWarnings("serial")
    public static class TansmitError extends Exception {

        public TansmitError()
        { super(); }

        public TansmitError(final String msg)
        { super(msg); }

        public static void notifyError(final String errMsg, final Object... args)
        { USB2GPIO.notifyError(errMsg, args); }

        public static void throwTansmitError(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError();
        }

        public static void throwTansmitError_spiTransfer(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("spi.transfer");
        }

        public static void throwTansmitError_usrtConfig(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("usrt.config");
        }

        public static void throwTansmitError_usrtTx(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("usrt.tx");
        }

        public static void throwTansmitError_usrtRx(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("usrt.rx");
        }

        public static void throwTansmitError_usrtTxRx(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("usrt.tx-rx");
        }

        public static void throwTansmitError_uartConfig(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("uart.config");
        }

        public static void throwTansmitError_uartTx(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("uart.tx");
        }

        public static void throwTansmitError_uartRx(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("uart.rx");
        }

        public static void throwTansmitError_uartTxRx(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("uart.tx-rx");
        }

        public static void throwTansmitError_readTimeout(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("read.timeout");
        }

        public static void throwTansmitError_readValueFailed(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("read.readValueFailed");
        }

        public static void throwTansmitError_readInvalidValue(final String errMsg, final Object... args) throws TansmitError
        {
            notifyError(errMsg, args);
            throw new TansmitError("read.invalidValue");
        }

    } // class TansmitError

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String DevSubClassNameSystem = "System";

    public static final String DevSubClassNameBBSPI  = "BB-SPI";
    public static final String DevSubClassNameHWSPI  = "HW-SPI";

    public static final String DevSubClassNameBBUSRT = "BB-USRT";
    public static final String DevSubClassNameHWUSRT = "HW-USRT";

    public static final String DevSubClassNameBBUART = "BB-UART";
    public static final String DevSubClassNameHWUART = "HW-UART";

    public static enum ImplMode {
        BitBang,
        Hardware,
        NotSupported
    }

    public static enum OperationalMode {
        Master,
        Slave,
        Both
    }

    public static enum DuplexMode {
        Full,
        Half,
        Both
    }

    public static final int UndeterminedFrequency =  0;
    public static final int MinimumFrequency      = -1;
    public static final int MaximumFrequency      = -2;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final int[] _undeterminedClkDivs  = new int[] { 0                     };
    protected static final int[] _undeterminedClkFreqs = new int[] { UndeterminedFrequency };

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ///// Generic GPIO & Devices ///////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum SPIMode {

        /*
         *  CPHA=0                              SPI Mode   Data Is Shifted-Out On                Data Is Sampled On
         *           ──┐                ┌──
         *  SS#        ╎                │
         *             └────────────────┘
         *              ┌┐┌┐┌┐┌┐┌┐┌┐┌┐┌┐
         *  SCLK        ║╎║╎║╎║╎║╎║╎║╎║╎        0          falling SCLK, and when SS activates   rising  SCLK
         *  CPOL=0   ───┘└┘└┘└┘└┘└┘└┘└┘└───
         *           ──┬─┬─┬─┬─┬─┬─┬─┬─┬───
         *  DATA      Z│7│6│5│4│3│2│1│0│Z
         *           ──┴─┴─┴─┴─┴─┴─┴─┴─┴───
         *           ───┐┌┐┌┐┌┐┌┐┌┐┌┐┌┐┌───
         *  SCLK        ║╎║╎║╎║╎║╎║╎║╎║╎        2          rising  SCLK, and when SS activates   falling SCLK
         *  CPOL=1      └┘└┘└┘└┘└┘└┘└┘└┘
         *
         *
         *  CPHA=1
         *           ──┐                ┌──
         *  SS#        │                │
         *             └────────────────┘
         *              ┌┐┌┐┌┐┌┐┌┐┌┐┌┐┌┐
         *  SCLK        ╎║╎║╎║╎║╎║╎║╎║╎║        1          rising  SCLK                          falling SCLK
         *  CPOL=0   ───┘└┘└┘└┘└┘└┘└┘└┘└───
         *           ──┬┬─┬─┬─┬─┬─┬─┬─┬─┬──
         *  DATA      Z││7│6│5│4│3│2│1│0│Z
         *           ──┴┴─┴─┴─┴─┴─┴─┴─┴─┴──
         *           ───┐┌┐┌┐┌┐┌┐┌┐┌┐┌┐┌───
         *  SCLK        ╎║╎║╎║╎║╎║╎║╎║╎║        3          falling SCLK                          rising  SCLK
         *  CPOL=1      └┘└┘└┘└┘└┘└┘└┘└┘
         */

        _0(false, false),
        _1(false, true ),
        _2(true , false),
        _3(true , true )

        ;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final boolean cpol;
        public final boolean cpha;

        private SPIMode(final boolean cpol_, final boolean cpha_)
        {
            cpol = cpol_;
            cpha = cpha_;
        }
    }

    public static enum SSMode { // NOTE : It is also used for USRT configuration
        ActiveLow,
        ActiveHigh
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract SerialPort rawSerialPort();

    public abstract void shutdown();
    public abstract void resetAndShutdown();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : # Not all modules have a compatible hardware SPI implementation!
    //           # Bit banging SPI from USB can be very slow and also does not support (ignore) the specified clock divider!

    public static enum IEVal {

         _X(0b00),
         _I(0b10),
         _A(0b11),
        ;

        private int _value;

        private IEVal(final int value)
        { _value = value; }

    } // enum IEVal

    public abstract boolean  spiIsImplModeSupported(final ImplMode implMode);
    public abstract boolean  spiSetImplMode(final ImplMode implMode);
    public abstract ImplMode spiGetImplMode();

    public abstract boolean spiIsOperationalModeSupported(final OperationalMode operationalMode);
    public abstract boolean spiIsDuplexModeSupported(final DuplexMode duplexMode);

    // NOTE : # These frequencies can be implemented using either hardware SPI or hardware-assisted bit-banging SPI.
    //        # These functions must return an array with values ​​ordered from low to high.
    public abstract int[] spiGetSupportedClkDivs();
    public abstract int[] spiGetSupportedClkFreqs();

    public int spiGetFastestHWClkFreq() { return UndeterminedFrequency   ; }
    public int spiGetSlowestHWClkFreq() { return UndeterminedFrequency   ; }
    public int spiGetFastestBBClkFreq() { return UndeterminedFrequency   ; }
    public int spiGetSlowestBBClkFreq() { return UndeterminedFrequency   ; }
    public int spiGetFastestClkFreq  () { return spiGetFastestHWClkFreq(); }
    public int spiGetSlowestClkFreq  () { return spiGetSlowestBBClkFreq(); }

    public int spiStdClkDivToClkFreq(final int clkDiv)
    {
        final int[] scd = spiGetSupportedClkDivs();

        if( clkDiv < 0                   ) return -1;
        if( clkDiv < scd[0             ] ) return -1;
        if( clkDiv > scd[scd.length - 1] ) return -1;

        return spiGetSupportedClkFreqs()[ clkDiv - scd[0] ];
    }

    // NOTE : # These frequencies should always be implemented using hardware-assisted bit-banging SPI
    //        # These functions must return an array with values ​​ordered from low to high.
    public int[] spiGetSupportedExtraClkDivs () { return null; }
    public int[] spiGetSupportedExtraClkFreqs() { return null; }

    public int spiGetFastestExtraBBClkFreq() { return UndeterminedFrequency;         }
    public int spiGetSlowestExtraBBClkFreq() { return UndeterminedFrequency;         }
    public int spiGetFastestExtraClkFreq  () { return spiGetFastestExtraBBClkFreq(); }
    public int spiGetSlowestExtraClkFreq  () { return spiGetSlowestExtraBBClkFreq(); }

    public int spiExtraClkDivToClkFreq(final int clkDiv)
    {
        final int[] scd = spiGetSupportedExtraClkDivs();

        if( clkDiv < 0                   ) return -1;
        if( clkDiv < scd[0             ] ) return -1;
        if( clkDiv > scd[scd.length - 1] ) return -1;

        return spiGetSupportedExtraClkFreqs()[ clkDiv - scd[0] ];
    }

    private int _spiClkFreqToClkDiv_impl(final int clkFreq, final boolean useExtraFreq)
    {
        final int[] scd = useExtraFreq ? spiGetSupportedExtraClkDivs () : spiGetSupportedClkDivs ();
        final int[] scf = useExtraFreq ? spiGetSupportedExtraClkFreqs() : spiGetSupportedClkFreqs();

        if(scd == null || scf == null || scd.length != scf.length) return -1;

        if(scf.length == 1 && scf[0] == UndeterminedFrequency) return scd[0];

        if(clkFreq == UndeterminedFrequency) return scd[0             ];
        if(clkFreq == MinimumFrequency     ) return scd[0             ];
        if(clkFreq == MaximumFrequency     ) return scd[scd.length - 1];

        for(int i = 0; i < scf.length; ++i) {
            if(scf[i] <= clkFreq) {
                if(i < scf.length - 1 && scf[i + 1] == scf[i]) ++i;
                return scd[i];
            }
        }

        return -1;
    }

    public int[] spiGetClkSpec(final int clkFreq)
    {
        if(clkFreq < 0) {
            final int divx = _spiClkFreqToClkDiv_impl(-clkFreq, true);
            return (divx < 0) ? new int[] { -1                           , -1   }
                              : new int[] { spiExtraClkDivToClkFreq(divx), divx };
        }

        final int divs  = _spiClkFreqToClkDiv_impl(clkFreq, false);
        final int divx  = _spiClkFreqToClkDiv_impl(clkFreq, true );

        final int freqs = spiStdClkDivToClkFreq  (divs);
        final int freqx = spiExtraClkDivToClkFreq(divx);

        if(freqs < 0 && freqx < 0) return new int[] { -1   , -1   };
        if(freqs > 0 && freqx < 0) return new int[] { freqs, divs };
        if(freqs < 0 && freqx > 0) return new int[] { freqx, divx };

        if( Math.abs(clkFreq - freqs) <= Math.abs(clkFreq - freqx) ) {
            return new int[] { freqs, divs };
        }
        else {
            return new int[] { freqx, divx };
        }
    }

    public int spiClkFreqToClkDiv(final int clkFreq)
    { return spiGetClkSpec(clkFreq)[1]; }

    public int spiClkDivToClkFreq(final int clkDiv)
    {
        final int sfr = spiStdClkDivToClkFreq(clkDiv);
        if(sfr > 0) return sfr;

        final int efr = spiExtraClkDivToClkFreq(clkDiv);
        if(efr > 0) return efr;

        return -1;
    }

    public abstract boolean spiBegin(final SPIMode spiMode, final SSMode ssMode, final int clkDiv);
    public abstract boolean spiEnd();

    public abstract boolean spiSelectSlave();
    public abstract boolean spiDeselectSlave();
    public abstract boolean spiPulseSlaveSelect(final int postDeselectDelayTime_MS, final int postReselectDelayTime_MS);

    public abstract boolean spiTransfer(final int[] ioBuff);
    public abstract boolean spiTransferIgnoreSS(final int[] ioBuff);

    public abstract boolean spiTransfer_w16Nd_r16dN(final SPIMode spiModeW, final int delayUs25AfterW, final int sizeW, final SPIMode spiModeR, final int delayUs10InterR, final int dummyValueR, final int[] ioBuff);
    public abstract boolean spiTransferIgnoreSS_w16Nd_r16dN(final SPIMode spiModeW, final int delayUs25AfterW, final int sizeW, final SPIMode spiModeR, final int delayUs10InterR, final int dummyValueR, final int[] ioBuff);

    public abstract boolean spiSetClkDiv(final int clkDiv);
    public abstract boolean spiSetSPIMode(final SPIMode spiMode);

    public abstract boolean spiSetBreak(final boolean mosi, final boolean sclk);
    public abstract int spiSetBreakExt(final boolean mosi, final boolean sclk);
    public abstract boolean spiClrBreak();

    public abstract boolean spiXBTransfer(final IEVal iMOSI, final IEVal iSCLK, final int iDelayMS, final IEVal eMOSI, final IEVal eSCLK, final int[] ioBuff);
    public abstract boolean spiXBTransferIgnoreSS(final IEVal iMOSI, final IEVal iSCLK, final int iDelayMS, final IEVal eMOSI, final IEVal eSCLK, final int[] ioBuff);
    public abstract boolean spiXBSpecial(final int type);

    public boolean spiPulseSlaveSelect()
    { return spiPulseSlaveSelect(1, 20); }

    public boolean spiXBTransfer(final int[] ioBuff)
    { return spiXBTransfer(USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, 0, USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, ioBuff); }

    public boolean spiXBTransferIgnoreSS(final int[] ioBuff)
    { return spiXBTransferIgnoreSS(USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, 0, USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, ioBuff); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum UXRTMode {
        _8N1, // 8 data bits, no   parity bit, one stop bit
        _8N2, // 8 data bits, no   parity bit, two stop bits

        _8E1, // 8 data bits, even parity bit, one stop bit
        _8E2, // 8 data bits, even parity bit, two stop bits

        _8O1, // 8 data bits, odd  parity bit, one stop bit
        _8O2  // 8 data bits, odd  parity bit, two stop bits
    }

    public static enum UXRTInputSignal {
        CTS, // Clear to Send
        DCD, // Data Carrier Detect
        DSR, // Data Set Ready
        RI , // Ring Indicator
        RXD, // Received Data (cannot be used for bit banging input)

        __INVALID__
    }

    public static enum UXRTOutputSignal {
        DTR, // Data Terminal Ready
        RTS, // Request to Send
        TXD, // Transmitted Data

        __INVALID__
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : # Not all modules have a compatible hardware USRT implementation!
    //           # Hardware USRT mode does not support manually pulsing the Xck line on request!
    //           # Not all implementation supports enabling and disabling the Tx line on request!
    //           # Bit banging USRT from USB can be very slow and also does not support (ignore) the specified baudrate!
    //           # Hardware-assisted bit banging USRT may not be able to accurately meet the specified baudrate!

    public abstract boolean  usrtIsImplModeSupported(final ImplMode implMode);
    public abstract boolean  usrtSetImplMode(final ImplMode implMode);
    public abstract ImplMode usrtGetImplMode();

    public abstract boolean usrtIsXMegaPDIModeSupported();

    public abstract boolean usrtIsOperationalModeSupported(final OperationalMode operationalMode);
    public abstract boolean usrtIsDuplexModeSupported(final DuplexMode duplexMode);

    public abstract boolean usrtIsPulsingXckSupported();
    public abstract boolean usrtIsEnablingDisablingTxSupported();

    public abstract int usrtGetMinimumBaudrate();
    public abstract int usrtGetMaximumBaudrate();
    public abstract int[] usrtGetStandardBaudrates();

    public abstract boolean usrtBegin(final UXRTMode uxrtMode, final SSMode ssMode, final int baudrate);
    public abstract boolean usrtBegin_PDI(final UXRTMode uxrtMode, final SSMode ssMode, final int baudrate);
    public abstract boolean usrtEnd();

    public abstract boolean usrtSelectSlave();
    public abstract boolean usrtDeselectSlave();

    public abstract boolean usrtEnableTx();
    public abstract boolean usrtDisableTx();
    public abstract boolean usrtDisableTxAfter(final int nb);

    public abstract boolean usrtPulseXck(final int count, final boolean txValue);

    public abstract boolean usrtChangeBaudrate(final int baudrate);

    public abstract boolean usrtTx(final int[] buff);
    public abstract boolean usrtRx(final int[] buff);

    public abstract boolean usrtTx_discardSerialLoopback(final int[] buff);

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : # Not all modules have a compatible hardware UART implementation!
    //           # Not all implementation supports enabling and disabling the Tx line on request!
    //           # Bit banging UART from USB will never work, hence, no implementation class should ever implement UART bit banging!
    //           # Hardware-assisted bit banging UART must be treated as if it were a hardware UART implementation!

    public abstract boolean  uartIsImplModeSupported(final ImplMode implMode);
    public abstract boolean  uartSetImplMode(final ImplMode implMode);
    public abstract ImplMode uartGetImplMode();

    public abstract boolean uartIsOperationalModeSupported(final OperationalMode operationalMode);
    public abstract boolean uartIsDuplexModeSupported(final DuplexMode duplexMode);

    public abstract boolean uartIsEnablingDisablingTxSupported();

    public abstract int uartGetMinimumBaudrate();
    public abstract int uartGetMaximumBaudrate();
    public abstract int[] uartGetStandardBaudrates();

    public abstract boolean uartBegin(final UXRTMode uxrtMode, final int baudrate);
    public abstract boolean uartEnd();

    public abstract boolean uartEnableTx();
    public abstract boolean uartDisableTx();
    public abstract boolean uartDisableTxAfter(final int nb);

    public abstract boolean uartChangeBaudrate(final int baudrate);

    public abstract boolean uartTx(final int[] buff);
    public abstract boolean uartRx(final int[] buff);

    public abstract boolean uartTx_discardSerialLoopback(final int[] buff);

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ///// Specialty Devices ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : Not all modules supports SWIM

    public abstract boolean  swimIsImplModeSupported(final ImplMode implMode);
    public abstract boolean  swimSetImplMode(final ImplMode implMode);
    public abstract ImplMode swimGetImplMode();

    public abstract boolean swimBegin();
    public abstract boolean swimEnd();

    public abstract boolean swimLineReset();

    public abstract boolean swimTransfer(final int[] ioBuff, final int len2X);

    public abstract boolean swimCmd_SRST();

    public abstract boolean swimCmd_ROTF(final int[] buff, final int address24);
    public abstract boolean swimCmd_WOTF(final int[] buff, final int address24);

    public int swimCmd_ROTF(final int address24)
    {
        final int[] buff = new int[1];
        if( !swimCmd_ROTF(buff, address24) ) return -1;
        return buff[0];
    }

    public boolean swimCmd_WOTF(final int address24, final int value)
    { return swimCmd_WOTF( new int[] { value }, address24 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : Not all modules supports JTAG

    public abstract boolean jtagIsModeSupported();

    public int jtagClkFreqToClkDiv(final int clkFreq)
    { return spiClkFreqToClkDiv(clkFreq); }

    public int jtagClkDivToClkFreq(final int clkDiv)
    { return spiClkDivToClkFreq(clkDiv); }

    public abstract boolean jtagBegin(final int clkDiv);
    public abstract boolean jtagEnd();

    public abstract boolean jtagSetClkDiv(final int clkDiv);

    public abstract boolean jtagSetReset(final boolean nRST, final boolean nTRST, final boolean TDI);

    public abstract boolean jtagTMS(final boolean nRST, final boolean nTRST, final boolean TDI, final int bitNumMinusOne, final int value);

    public abstract boolean jtagTransfer(boolean xUpdate, boolean drShift, boolean irShift, int bitCntLastMinusOne, final int[] ioBuff);

    public abstract boolean jtagXBTransfer(boolean xUpdate, boolean drShift, boolean irShift, final int[] ioBuff);

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ///// Utility Functions ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void ba2ia(final int[] ints, final byte[] bytes)
    { for(int i = 0; i < bytes.length; ++i) ints[i] = bytes[i] & 0xFF; }

    public static int[] ba2ia(final byte[] bytes)
    {
        final int[] ints = new int[bytes.length];
        for(int i = 0; i < bytes.length; ++i) ints[i] = bytes[i] & 0xFF;
        return ints;
    }

    public static int[] ba2ia(final byte[] bytes, final int offset, final int len)
    {
        final int[] ints = new int[len];
        for(int i = 0; i < len; ++i) ints[i] = bytes[offset + i] & 0xFF;
        return ints;
    }

    public static void ba2ia(final int[] ints, final byte[] bytes, final int offset, final int len)
    { for(int i = 0; i < len; ++i) ints[i] = bytes[offset + i] & 0xFF; }

    public static void ia2ba(final byte[] bytes, final int[] ints)
    { for(int i = 0; i < ints.length; ++i) bytes[i] = (byte) ints[i]; }

    public static byte[] ia2ba(final int[] ints)
    {
        final byte[] bytes = new byte[ints.length];
        for(int i = 0; i < ints.length; ++i) bytes[i] = (byte) ints[i];
        return bytes;
    }

    public static byte[] ia2ba(final int[] ints, final int offset, final int len)
    {
        final byte[] bytes = new byte[len];
        for(int i = 0; i < len; ++i) bytes[i] = (byte) ints[offset + i];
        return bytes;
    }

    public static void ia2ba(final byte[] bytes, final int[] ints, final int offset, final int len)
    { for(int i = 0; i < len; ++i) bytes[i] = (byte) ints[offset + i]; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String ba2str(final byte[] buff, final int offset, final int len)
    {
        if(buff == null) return null;

        try {
            return new String( Arrays.copyOfRange(buff, offset, offset + len), "US-ASCII" );
        }
        catch(final UnsupportedEncodingException e) {
            return null;
          //return "java.lang.String(): UnsupportedEncodingException";
        }
    }

    public static String ba2str(final byte[] buff, final int offset)
    { return ba2str(buff, offset, buff.length - offset); }

    public static String ba2str(final byte[] buff)
    { return ba2str(buff, 0, buff.length); }

    public static String ia2str(final int[] buff, final int offset, final int len)
    {
        if(buff == null) return null;

        try {
            return new String( ia2ba(buff, offset, len), "US-ASCII" );
        }
        catch(final UnsupportedEncodingException e) {
            return null;
          //return "java.lang.String(): UnsupportedEncodingException";
        }
    }

    public static String ia2str(final int[] buff, final int offset)
    { return (buff == null) ? null : ia2str(buff, offset, buff.length - offset); }

    public static String ia2str(final int[] buff)
    { return (buff == null) ? null : ia2str(buff, 0, buff.length); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void ttyPortApplyConfig(final SerialPort ttyPort, final UXRTMode uxrtMode, final int baudrate)
    {
        ttyPort.setBaudRate​(baudrate);

        switch(uxrtMode) {
            case _8N1 : ttyPort.setParity​(SerialPort.NO_PARITY  ); ttyPort.setNumStopBits​(SerialPort.ONE_STOP_BIT ); break;
            case _8N2 : ttyPort.setParity​(SerialPort.NO_PARITY  ); ttyPort.setNumStopBits​(SerialPort.TWO_STOP_BITS); break;
            case _8E1 : ttyPort.setParity​(SerialPort.EVEN_PARITY); ttyPort.setNumStopBits​(SerialPort.ONE_STOP_BIT ); break;
            case _8E2 : ttyPort.setParity​(SerialPort.EVEN_PARITY); ttyPort.setNumStopBits​(SerialPort.TWO_STOP_BITS); break;
            case _8O1 : ttyPort.setParity​(SerialPort.ODD_PARITY ); ttyPort.setNumStopBits​(SerialPort.ONE_STOP_BIT ); break;
            case _8O2 : ttyPort.setParity​(SerialPort.ODD_PARITY ); ttyPort.setNumStopBits​(SerialPort.TWO_STOP_BITS); break;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean checkStartAddressAndNumberOfBytes_even(final int sa, final int nb, final int flashTotalSize, final String progClassName)
    {
        if(sa + nb > flashTotalSize) return USB2GPIO.notifyError(Texts.ProgXXX_FSAddrNBytesOoR, progClassName);

        if( (sa & 0x01) != 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_SAddrNotEven , progClassName); // The start address   must be even (a multiple of 2)
        if( (nb & 0x01) != 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_NBytesNotEven, progClassName); // The number of bytes must be even (a multiple of 2)

        return true;
    }

    public static boolean checkStartAddressAndNumberOfBytes_mN(final int sa, final int nb, final int flashTotalSize, final int mN, final String progClassName)
    {
        if(sa + nb > flashTotalSize) return USB2GPIO.notifyError(Texts.ProgXXX_FSAddrNBytesOoR, progClassName);

        if( (sa % mN) != 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_SAddrNotMPN , progClassName, mN); // The start address   must be a multiple of 'mN'
        if( (nb % mN) != 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_NBytesNotMPN, progClassName, mN); // The number of bytes must be a multiple of 'mN'

        return true;
    }

    public static boolean checkStartAddressAndNumberOfBytes_pageSize(final int sa, final int nb, final int flashPageSize, final String progClassName)
    {
        if( (sa % flashPageSize) != 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_SAddrNotMPZ , progClassName); // The start address   must be a multiple of the page size
        if( (nb % flashPageSize) != 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_NBytesNotMPZ, progClassName); // The number of bytes must be a multiple of the page size

        return true;
    }

    public static int checkPageSize_chunkSize(final int flashPageSize, final int chunkSize, final String progClassName)
    {
        if( (flashPageSize % chunkSize) != 0 ) {
            USB2GPIO.notifyError(Texts.ProgXXX_CZNotDivPZwoRem , progClassName); // The chunk size must be able to divide the page size without remainder
            return -1;
        }

        return flashPageSize / chunkSize;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class ANBResult {
        public int    nb;
        public byte[] buff;

        public ANBResult()
        {
            nb   = 0;
            buff = null;
        }

        public ANBResult(final int nb_, final byte[] buff_)
        {
            nb   = nb_;
            buff = buff_;
        }
    }

    public static int alignWriteSize(final int numBytes, final int flashPageSize)
    {
        int nb = (numBytes / flashPageSize) * flashPageSize;

        if(nb < numBytes) nb += flashPageSize;

        return nb;
    }

    public static ANBResult alignNumberOfBytesAndPadBuffer(final byte[] data, final int sa, final int nb, final int flashPageSize, final int flashTotalSize, final byte padValue, final String progClassName)
    {
        // Prepare the result
        final ANBResult anbr = new ANBResult();

        anbr.nb   = nb;
        anbr.buff = data;

        // Align the number of bytes and pad the buffer as needed
        if(anbr.nb % flashPageSize != 0) {

            /*
            // Align the number of bytes
            anbr.nb = (anbr.nb / flashPageSize + 1) * flashPageSize;
            */

            // Align the number of bytes
            anbr.nb = (anbr.nb / flashPageSize) * flashPageSize;
            if(anbr.nb < nb) anbr.nb += flashPageSize;

            // Copy and pad the buffer
            anbr.buff = Arrays.copyOf(data, anbr.nb);

            Arrays.fill(anbr.buff, nb, anbr.nb, padValue);

        } // if

        // Check the start address and number of bytes
        if(sa + anbr.nb > flashTotalSize) {
            USB2GPIO.notifyError(Texts.ProgXXX_FSAddrNBytesOoR, progClassName);
            return null;
        }

        // Return the result
        return anbr;
    }

} // class USB2GPIO

