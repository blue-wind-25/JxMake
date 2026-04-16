/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.Arrays;

import com.fazecast.jSerialComm.*;

import jxm.*;
import jxm.xb.*;


/*
 * Please refer to '../../../docs/txt/en_US/14-Simple-Programmer-Hardware.txt' (and its translations) for the connection diagram of some simple programmers.
 */
public class DASA extends USB2GPIO {

    private static final String DevClassName       = "DASA:";

    private static final String DevClassNameBBSPI  = DevClassName + USB2GPIO.DevSubClassNameBBSPI;  // Master mode only - full        duplex
    private static final String DevClassNameBBUSRT = DevClassName + USB2GPIO.DevSubClassNameBBUSRT; // Master mode only -        half duplex
    private static final String DevClassNameBBUART = null;                                          // Not supported!

    private static final String DevClassNameHWSPI  = null;                                          // Not supported!
    private static final String DevClassNameHWUSRT = null;                                          // Not supported!
    private static final String DevClassNameHWUART = DevClassName + USB2GPIO.DevSubClassNameHWUART; // Master mode only - full & half duplex

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private SerialPort _ttyPort = null;

    private void _mosi(final boolean value)
    {
        switch(_mosiPin) {
            case DTR : if(value) _ttyPort.clearDTR  ();
                       else      _ttyPort.setDTR    ();
                       break;
            case RTS : if(value) _ttyPort.clearRTS  ();
                       else      _ttyPort.setRTS    ();
                       break;
            case TXD : if(value) _ttyPort.clearBreak();
                       else      _ttyPort.setBreak  ();
                       break;
            default  : break;
        }
    }

    private int _miso()
    {
        switch(_misoPin) {
            case CTS : return _ttyPort.getCTS() ? 0 : 1;
            case DCD : return _ttyPort.getDCD() ? 0 : 1;
            case DSR : return _ttyPort.getDSR() ? 0 : 1;
            case RI  : return _ttyPort.getRI () ? 0 : 1;
            default  : break;
        }

        return 0;
    }

    private void _sclk(final boolean value)
    {
        switch(_sclkPin) {
            case DTR : if(_cpol ^ value) _ttyPort.clearDTR  ();
                       else              _ttyPort.setDTR    ();
                       break;
            case RTS : if(_cpol ^ value) _ttyPort.clearRTS  ();
                       else              _ttyPort.setRTS    ();
                       break;
            case TXD : if(_cpol ^ value) _ttyPort.clearBreak();
                       else              _ttyPort.setBreak  ();
                       break;
            default  : break;
        }
    }

    private void _ssel(final boolean value)
    {
        switch(_sselPin) {
            case DTR : if(_invertSS ^ value) _ttyPort.setDTR    ();
                       else                  _ttyPort.clearDTR  ();
                       break;
            case RTS : if(_invertSS ^ value) _ttyPort.setRTS    ();
                       else                  _ttyPort.clearRTS  ();
                       break;
            case TXD : if(_invertSS ^ value) _ttyPort.setBreak  ();
                       else                  _ttyPort.clearBreak();
                       break;
            default  : break;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int DEFAULT_BAUDRATE = 1000000;

    public DASA(final String serialDevice) throws Exception
    {
        /*
        SysUtil.stdDbg().println( "### Undetermined = " + spiClkFreqToClkDiv(UndeterminedFrequency) );
        SysUtil.stdDbg().println( "### Minimum      = " + spiClkFreqToClkDiv(MinimumFrequency) );
        SysUtil.stdDbg().println( "### Maximum      = " + spiClkFreqToClkDiv(MaximumFrequency) );
        SysUtil.stdDbg().println( "### " );
        SysUtil.stdDbg().println( "### 16000000     = " + spiClkFreqToClkDiv(16000000) );
        SysUtil.stdDbg().println( "###  8000000     = " + spiClkFreqToClkDiv( 8000000) );
        SysUtil.stdDbg().println( "###  6000000     = " + spiClkFreqToClkDiv( 6000000) );
        SysUtil.stdDbg().println( "###  4000000     = " + spiClkFreqToClkDiv( 4000000) );
        SysUtil.stdDbg().println( "###      500     = " + spiClkFreqToClkDiv(     500) );
        SysUtil.stdDbg().println( "###      250     = " + spiClkFreqToClkDiv(     250) );
        SysUtil.stdDbg().println( "###      100     = " + spiClkFreqToClkDiv(     100) );
        SysUtil.stdDbg().println( "### " );
        SysUtil.stdDbg().println( "### USRT Minimum   : " + usrtGetMinimumBaudrate() );
        SysUtil.stdDbg().println( "### USRT Maximum   : " + usrtGetMaximumBaudrate() );
        SysUtil.stdDbg().println( "### USRT Standards : " + Arrays.toString( usrtGetStandardBaudrates() ) );
        SysUtil.stdDbg().println( "### " );
        SysUtil.stdDbg().println( "### UART Minimum   : " + uartGetMinimumBaudrate() );
        SysUtil.stdDbg().println( "### UART Maximum   : " + uartGetMaximumBaudrate() );
        SysUtil.stdDbg().println( "### UART Standards : " + Arrays.toString( uartGetStandardBaudrates() ) );
        SysUtil.stdDbg().println( "### " );
        //*/

        // Open the device
        _ttyPort = SerialPort.getCommPort(serialDevice);

        _ttyPort.setFlowControl​    (SerialPort.FLOW_CONTROL_DISABLED);
        _ttyPort.setBaudRate​       (DEFAULT_BAUDRATE);
        _ttyPort.setNumDataBits​    (8);
        _ttyPort.setParity​         (SerialPort.NO_PARITY);
        _ttyPort.setNumStopBits​    (SerialPort.ONE_STOP_BIT);
        _ttyPort.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 1000, 1000);
        _ttyPort.clearBreak        ();
        _ttyPort.setDTR            ();
        _ttyPort.setRTS            ();
        _ttyPort.openPort          ();

        // Reset the configuration data to their uninitialized values
        _resetSPIConfigData();
    }

    @Override
    public SerialPort rawSerialPort()
    { return _ttyPort; }

    @Override
    public void shutdown()
    { _ttyPort.closePort(); }

    @Override
    public void resetAndShutdown()
    { shutdown(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : A generic USB to serial converter module does not have a hardware SPI, hence, this class implements SPI bit banging

    private boolean          _initialized   = false; // A flag that indicates if the system has been initialized
    private boolean          _slaveSelected = false; // A flag that indicates if the slave  has been selected

    private UXRTOutputSignal _mosiPin;               // MOSI pin
    private UXRTInputSignal  _misoPin;               // MISO pin
    private UXRTOutputSignal _sclkPin;               // SCK  pin
    private UXRTOutputSignal _sselPin;               // SS   pin

    private boolean          _invertSS;              // Invert SS

    private boolean          _cpol;                  // Clock polarity
    private boolean          _cpha;                  // Clock phase

    private void _resetSPIConfigData()
    {
        _mosiPin  = UXRTOutputSignal.__INVALID__;
        _misoPin  = UXRTInputSignal .__INVALID__;
        _sclkPin  = UXRTOutputSignal.__INVALID__;
        _sselPin  = UXRTOutputSignal.__INVALID__;

        _invertSS = false; // Set as active low by default

        _cpol     = false; // Set as SPI mode 0 by default
        _cpha     = false; // ---
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean spiIsImplModeSupported(final ImplMode implMode)
    {
        // Only bit banging mode is supported
        return implMode == ImplMode.BitBang;
    }

    @Override
    public boolean spiSetImplMode(final ImplMode implMode)
    {
        // Error if already initialized
        if(_initialized) return false;

        // Only bit banging mode is supported
        return implMode == ImplMode.BitBang;
    }

    @Override
    public ImplMode spiGetImplMode()
    { return ImplMode.BitBang; }

    @Override
    public boolean spiIsOperationalModeSupported(final OperationalMode operationalMode)
    {
        // Only master mode is supported
        return operationalMode == OperationalMode.Master;
    }

    @Override
    public boolean spiIsDuplexModeSupported(final DuplexMode duplexMode)
    {
        // Only full duplex mode is supported
        return duplexMode == DuplexMode.Full;
    }

    @Override
    public int[] spiGetSupportedClkDivs()
    { return _undeterminedClkDivs; }

    /*
     * NOTE : # Only bit banging SPI is supported.
     *        # Bit banging SPI cannot guarantee the clock frequency.
     */
    @Override
    public int[] spiGetSupportedClkFreqs()
    { return _undeterminedClkFreqs; }

    public boolean spiBegin(final SPIMode spiMode, final SSMode ssMode, final UXRTOutputSignal mosiPin, final UXRTInputSignal misoPin, final UXRTOutputSignal sckPin, final UXRTOutputSignal ssPin)
    {
        // Error if already initialized
        if(_initialized) return false;

        // Error if the IO pins overlap
        if(mosiPin == sckPin || mosiPin == ssPin || sckPin == ssPin) return false;

        // Error if the MISO pin use RXD
        if(misoPin == UXRTInputSignal.RXD) return false;

        // Save configuration
        _mosiPin  = mosiPin;
        _misoPin  = misoPin;
        _sclkPin  = sckPin;
        _sselPin  = ssPin;

        _invertSS = (ssMode == SSMode.ActiveHigh);

        _cpol     = spiMode.cpol;
        _cpha     = spiMode.cpha;

        // Set all signals to their inactive (idle) states
        _mosi(false);
        _sclk(false);
        _ssel(false);

        // Set flag
        _initialized = true;

        // Done
        return true;
    }

    @Override
    public boolean spiBegin(final SPIMode spiMode, final SSMode ssMode, final int clkDiv)
    {
        // NOTE : SPI bit banging does not support (ignore) the specified clock divider

        // Initialize using the default IO pin names
        return spiBegin(spiMode, ssMode, UXRTOutputSignal.RTS, UXRTInputSignal.CTS, UXRTOutputSignal.DTR, UXRTOutputSignal.TXD);
    }

    @Override
    public boolean spiEnd()
    {
        // Error if not initialized
        if(!_initialized) return false;

        // Deselect the slave
        spiDeselectSlave();

        // Reset the configuration data to their uninitialized values
        _resetSPIConfigData();

        // Clear flag
        _initialized = false;

        // Done
        return true;
    }

    @Override
    public boolean spiSelectSlave()
    {
        // Error if not initialized or already selected
        if(!_initialized || _slaveSelected) return false;

        // Set the MOSI and SCK pins to output with their inactive (idle) values
        _mosi(false);
        _sclk(false);

        SysUtil.sleepMS(1);

        // Select the slave
        _ssel(true);

        SysUtil.sleepMS(1);

        // Set flag
        _slaveSelected = true;

        // Done
        return true;
    }

    @Override
    public boolean spiDeselectSlave()
    {
        // Error if not initialized or not selected
        if(!_initialized || !_slaveSelected) return false;

        // Set the MOSI and SCK pins to output with their inactive (idle) values
        _mosi(false);
        _sclk(false);

        SysUtil.sleepMS(1);

        // Deselect the slave
        _ssel(false);

        SysUtil.sleepMS(1);

        // Clear flag
        _slaveSelected = false;

        // Done
        return true;
    }

    @Override
    public boolean spiPulseSlaveSelect(final int postDeselectDelayTime_MS, final int postReselectDelayTime_MS)
    {
        // Error if not initialized or not selected
        if(!_initialized || !_slaveSelected) return false;

        // Deselect the slave
        _mosi(false);
        _sclk(false);
        _ssel(false);

        SysUtil.sleepMS(postDeselectDelayTime_MS);

        // Reselect the slave
        _ssel(true);

        SysUtil.sleepMS(postReselectDelayTime_MS);

        // Done
        return true;
    }

    @Override
    public boolean spiTransfer(final int[] ioBuff)
    {
        // Error if not initialized or not selected
        if(!_initialized || !_slaveSelected) return false;

        // Perform the transfer
        return spiTransferIgnoreSS(ioBuff);
    }

    @Override
    public boolean spiTransferIgnoreSS(final int[] ioBuff)
    {
        // Error if not initialized
        if(!_initialized) return false;

        // Loop through the values
        for(int i = 0; i < ioBuff.length; ++i) {

            // Loop through the bits
            for(int b = 0; b < 8; ++b) {

                // Get the MOSI bit value to be sent (MSB first)
                final boolean mosiBitValue = (ioBuff[i] & 0b10000000) != 0;

                // Shift the bit
                ioBuff[i] = (ioBuff[i] << 1) & 0xFF;

                // SPI mode 1 and 3
                if(_cpha) {
                    _sclk(true);                     // Make the SCK active
                    _mosi(mosiBitValue);             // Output the bit
                    _sclk(false);                    // Make the SCK inactive
                    ioBuff[i] = ioBuff[i] | _miso(); // Read the response and store the bit that has been received (MSB first)
                }
                // SPI mode 0 and 2
                else {
                    _mosi(mosiBitValue);             // Output the bit
                    _sclk(true);                     // Make the SCK active
                    ioBuff[i] = ioBuff[i] | _miso(); // Read the response and store the bit that has been received (MSB first)
                    _sclk(false);                    // Make the SCK inactive
                }

            } // for b

        } // for i

        // Done
        return true;
    }

    @Override
    public boolean spiTransfer_w16Nd_r16dN(final SPIMode spiModeW, final int delayUs25AfterW, final int sizeW, final SPIMode spiModeR, final int delayUs10InterR, final int dummyValueR, final int[] ioBuff)
    {
        // NOTE : SPI bit banging does not support this feature
        return false;
    }

    @Override
    public boolean spiTransferIgnoreSS_w16Nd_r16dN(final SPIMode spiModeW, final int delayUs25AfterW, final int sizeW, final SPIMode spiModeR, final int delayUs10InterR, final int dummyValueR, final int[] ioBuff)
    {
        // NOTE : SPI bit banging does not support this feature
        return false;
    }

    @Override
    public boolean spiSetClkDiv(final int clkDiv)
    {
        // NOTE : SPI bit banging does not support (ignore) the specified clock divider
        return true;
    }

    @Override
    public boolean spiSetSPIMode(final SPIMode spiMode)
    {
        _cpol = spiMode.cpol;
        _cpha = spiMode.cpha;

        return true;
    }

    @Override
    public boolean spiSetBreak(final boolean mosi, final boolean sclk)
    {
        // NOTE : SPI bit banging does not support set break
        return false;
    }

    @Override
    public int spiSetBreakExt(final boolean mosi, final boolean sclk)
    {
        // NOTE : SPI bit banging does not support set break
        return -1;
    }

    @Override
    public boolean spiClrBreak()
    {
        // NOTE : SPI bit banging does not support clear break
        return false;
    }

    @Override
    public boolean spiXBTransfer(final IEVal iMOSI, final IEVal iSCLK, final int iDelayMS, final IEVal eMOSI, final IEVal eSCLK, final int[] ioBuff)
    {
        // NOTE : SPI bit banging does not support this feature
        return false;
    }

    @Override
    public boolean spiXBTransferIgnoreSS(final IEVal iMOSI, final IEVal iSCLK, final int iDelayMS, final IEVal eMOSI, final IEVal eSCLK, final int[] ioBuff)
    {
        // NOTE : SPI bit banging does not support this feature
        return false;
    }

    @Override
    public boolean spiXBSpecial(final int type)
    {
        // NOTE : SPI bit banging does not support this feature
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : # A generic USB to serial converter module does not have a hardware USRT, hence, this class implements bit banging USRT
    //        # The bit banging USRT utilizes some of the bit banging SPI code

    private static final int RX_WAIT_START_BIT_TIMEOUT_MS = 1000;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _parityEven  = false;
    private boolean _parityOdd   = false;

    private boolean _twoStopBits = false;

    private void _resetUSRTConfigData()
    {
        _resetSPIConfigData();

        _parityEven  = false;
        _parityOdd   = false;

        _twoStopBits = false;
    }

    private void _txd(final boolean value)
    { _mosi(value); }

    private boolean _rxd()
    { return _miso() != 0; }

    private void _xck(final boolean value)
    { _sclk(value); }

    private void _xss(final boolean value)
    { _ssel(value); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean usrtIsImplModeSupported(final ImplMode implMode)
    {
        // Only bit banging mode is supported
        return implMode == ImplMode.BitBang;
    }

    @Override
    public boolean usrtSetImplMode(final ImplMode implMode)
    {
        // Error if already initialized
        if(_initialized) return false;

        // Only bit banging mode is supported
        return implMode == ImplMode.BitBang;
    }

    @Override
    public ImplMode usrtGetImplMode()
    { return ImplMode.BitBang; }

    @Override
    public boolean usrtIsXMegaPDIModeSupported()
    { return false; }

    @Override
    public boolean usrtIsOperationalModeSupported(final OperationalMode operationalMode)
    {
        // Only master mode is supported
        return operationalMode == OperationalMode.Master;
    }

    @Override
    public boolean usrtIsDuplexModeSupported(final DuplexMode duplexMode)
    {
        // Only half duplex mode is supported
        return duplexMode == DuplexMode.Half;
    }

    @Override
    public boolean usrtIsPulsingXckSupported()
    {
        // Bit banging mode always support manually pulsing the Xck line on request
        return true;
    }

    @Override
    public boolean usrtIsEnablingDisablingTxSupported()
    { return false; }

    @Override
    public int usrtGetMinimumBaudrate()
    { return UndeterminedFrequency; }

    @Override
    public int usrtGetMaximumBaudrate()
    { return UndeterminedFrequency; }

    /*
     * NOTE : # Only bit banging USRT is supported.
     *        # Bit banging USRT cannot guarantee the baudrate.
     */
    @Override
    public int[] usrtGetStandardBaudrates()
    { return _undeterminedClkFreqs; }

    public boolean usrtBegin(final UXRTMode uxrtMode, final SSMode ssMode, final UXRTOutputSignal txPin, final UXRTInputSignal rxPin, final UXRTOutputSignal xckPin, final UXRTOutputSignal ssPin)
    {
        // Error if already initialized
        if(_initialized) return false;

        // Initialize the IO
        if( !spiBegin(USB2GPIO.SPIMode._0, ssMode, txPin, rxPin, xckPin, ssPin) ) return false;

        // Save the configuration data
        _parityEven  = (uxrtMode == UXRTMode._8E1) || (uxrtMode == UXRTMode._8E2);
        _parityOdd   = (uxrtMode == UXRTMode._8O1) || (uxrtMode == UXRTMode._8O2);
        _twoStopBits = (uxrtMode == UXRTMode._8N2) || (uxrtMode == UXRTMode._8E2) || (uxrtMode == UXRTMode._8O2);

        // Done
        return true;
    }

    @Override
    public boolean usrtBegin(final UXRTMode uxrtMode, final SSMode ssMode, final int baudrate)
    {
        // NOTE : Bit banging USRT does not support (ignore) the specified baudrate

        // Initialize using the default IO pin names
        return usrtBegin(uxrtMode, ssMode, UXRTOutputSignal.RTS, UXRTInputSignal.CTS, UXRTOutputSignal.DTR, UXRTOutputSignal.TXD);
    }

    @Override
    public boolean usrtBegin_PDI(final UXRTMode uxrtMode, final SSMode ssMode, final int baudrate)
    { return false; }

    @Override
    public boolean usrtEnd()
    {
        // Error if not initialized
        if(!_initialized) return false;

        // Deselect the slave
        usrtDeselectSlave();

        // Reset the configuration data to their uninitialized values
        _resetUSRTConfigData();

        // Clear flag
        _initialized = false;

        // Done
        return true;
    }

    @Override
    public boolean usrtSelectSlave()
    { return spiSelectSlave(); }

    @Override
    public boolean usrtDeselectSlave()
    { return spiDeselectSlave(); }

    @Override
    public boolean usrtEnableTx()
    { return false; }

    @Override
    public boolean usrtDisableTx()
    { return false; }

    @Override
    public boolean usrtDisableTxAfter(final int nb)
    { return false; }

    @Override
    public boolean usrtPulseXck(final int count, final boolean txValue)
    {
        // Error if not initialized or not selected
        if(!_initialized || !_slaveSelected) return false;

        // Set the Tx pin signal to the specified value
        _txd(txValue);

        // Pulse the clock
        for(int i = 0; i < count; ++i) {
            _xck(false);
            _xck(true );
        }

        // Done
        return true;
    }

    @Override
    public boolean usrtChangeBaudrate(final int baudrate)
    {
        // NOTE : Bit banging USRT does not support (ignore) the specified baudrate
        return true;
    }

    @Override
    public boolean usrtTx(final int[] buff)
    {
        // Error if not initialized or not selected
        if(!_initialized || !_slaveSelected) return false;

        // Loop through the values
        for(int i = 0; i < buff.length; ++i) {

            // Send the start bit
            _xck(false);
            _txd(false);
            _xck(true );

            // Loop through the bits
            int     value  = buff[i];
            boolean parity = false;

            for(int b = 0; b < 8; ++b) {

                // Get the bit value to be sent (LSB first)
                final boolean db = (value & 0b00000001) != 0;

                value = (value >> 1) & 0xFF;

                // Calculate the parity
                parity ^= db;

                // Send the data bit
                _xck(false);
                _txd(db   );
                _xck(true );

            } // for b

            // Send the parity bit as needed
            if(_parityOdd) parity = !parity;

            if(_parityEven || _parityOdd) {
                _xck(false );
                _txd(parity);
                _xck(true  );
            }

            // Send the stop bit(s)
                _xck(false);
                _txd(true );
                _xck(true );
            if(_twoStopBits) {
                _xck(false);
                _txd(true );
                _xck(true );
            }

        } // for i

        // Done
        return true;
    }

    @Override
    public boolean usrtRx(final int[] buff)
    {
        // Error if not initialized or not selected
        if(!_initialized || !_slaveSelected) return false;

        // Loop as many as the number of requested values
        for(int i = 0; i < buff.length; ++i) {

            // Receive the start bit
            final XCom.TimeoutMS tms = new XCom.TimeoutMS(RX_WAIT_START_BIT_TIMEOUT_MS);

            while(true) {
                _xck(false);
                _xck(true );
                if( !_rxd() ) break;
                if( tms.timeout() ) {
                    USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, DevClassNameBBUSRT);
                    return false;
                }
            }

            // Loop through the bits
            int     value  = 0;
            boolean parity = false;

            for(int b = 0; b < 8; ++b) {

                // Receive the data bit
                _xck(false);
                _xck(true );
                final boolean db = _rxd();

                // Store the bit that has been received (LSB first)
                value = (value >> 1) & 0xFF;

                if(db) value = value | 0b10000000;

                // Calculate the parity
                parity ^= db;

            } // for b

            buff[i] = value; // Store the value

            // Receive and check the parity bit as needed
            if(_parityEven || _parityOdd) {
                // Receive the parity bit
                _xck(false);
                _xck(true );
                final boolean pb = _rxd();
                // Update and compare the parity bit
                if(_parityOdd) parity = !parity;
                if(pb != parity) {
                    USB2GPIO.notifyError(Texts.PDevXXX_ParityError, DevClassNameBBUSRT);
                    return false;
                }
            }

            // Receive the stop bit(s)
            if(true) {
                _xck(false);
                _xck(true );
                final boolean ob = _rxd();
                if(ob != true) {
                    USB2GPIO.notifyError(Texts.PDevXXX_StopBit1Error, DevClassNameBBUSRT);
                    return false;
                }
            }
            if(_twoStopBits) {
                _xck(false);
                _xck(true );
                final boolean ob = _rxd();
                if(ob != true) {
                    USB2GPIO.notifyError(Texts.PDevXXX_StopBit2Error, DevClassNameBBUSRT);
                    return false;
                }
            }

        } // for i

        // Done
        return true;
    }

    @Override
    public boolean usrtTx_discardSerialLoopback(final int[] buff)
    {
        // WARNING : Bit banging USRT mode does not support this feature!
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int TX_RX_TIMEOUT_MS = 1000;

    @Override
    public boolean uartIsImplModeSupported(final ImplMode implMode)
    {
        // Only hardware mode is supported
        return implMode == ImplMode.Hardware;
    }

    @Override
    public boolean uartSetImplMode(final ImplMode implMode)
    {
        // Error if already initialized
        if(_initialized) return false;

        // Only hardware mode is supported
        return implMode == ImplMode.Hardware;
    }

    @Override
    public ImplMode uartGetImplMode()
    { return ImplMode.Hardware; }

    @Override
    public boolean uartIsOperationalModeSupported(final OperationalMode operationalMode)
    {
        // Only master mode is supported
        return operationalMode == OperationalMode.Master;
    }

    @Override
    public boolean uartIsDuplexModeSupported(final DuplexMode duplexMode)
    {
        // Both full duplex mode and half duplex mode are supported
        return true;
    }

    @Override
    public boolean uartIsEnablingDisablingTxSupported()
    { return false; }

    @Override
    public int uartGetMinimumBaudrate()
    { return 300; }

    @Override
    public int uartGetMaximumBaudrate()
    { return 921600; }

    /*
     * NOTE : # Only hardware UART is supported.
     *        # These baudrates should be supported by most USB to serial adapter.
     */
    @Override
    public int[] uartGetStandardBaudrates()
    { return new int[] { 300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600 }; }

    @Override
    public boolean uartBegin(final UXRTMode uxrtMode, final int baudrate)
    {
        // Error if already initialized
        if(_initialized) return false;

        // Apply the settings
        USB2GPIO.ttyPortApplyConfig(_ttyPort, uxrtMode, baudrate);

        // Set flag
        _initialized = true;

        // Done
        return true;
    }

    @Override
    public boolean uartEnd()
    {
        // Error if not initialized
        if(!_initialized) return false;

        // Restore the settings
        _ttyPort.setBaudRate​   (DEFAULT_BAUDRATE);
        _ttyPort.setParity​     (SerialPort.NO_PARITY);
        _ttyPort.setNumStopBits(SerialPort.ONE_STOP_BIT);

        // Clear flag
        _initialized = false;

        // Done
        return true;
    }

    @Override
    public boolean uartEnableTx()
    { return false; }

    @Override
    public boolean uartDisableTx()
    { return false; }

    @Override
    public boolean uartDisableTxAfter(final int nb)
    { return false; }

    @Override
    public boolean uartChangeBaudrate(final int baudrate)
    {
        // Error if not initialized
        if(!_initialized) return false;

        // Restore the settings
        _ttyPort.setBaudRate​(baudrate);

        // Done
        return true;
    }

    @Override
    public boolean uartTx(final int[] buff)
    {
        // Error if not initialized
        if(!_initialized) return false;

        // Prepare the buffer
        final byte[] buf = USB2GPIO.ia2ba(buff);
              int    len = buf.length;
              int    ofs = 0;

        // Write the command byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        while(len != 0) {
            // Write some byte(s)
            final int cnt = _ttyPort.writeBytes(buf, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                                  }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_TxTimeout, DevClassNameHWUART); }
        }

        // Ensure all the bytes are written
      //_ttyPort.flushIOBuffers();
        while( _ttyPort.bytesAwaitingWrite() > 0 );

        // Done
        return true;
    }

    @Override
    public boolean uartRx(final int[] buff)
    {
        // Prepare the buffer
        final byte[] buf = new byte[buff.length];
              int    len = buf.length;
              int    ofs = 0;

        // Read the response byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        while(len != 0) {
            // Read some byte(s)
            final int cnt = _ttyPort.readBytes​(buf, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                                  }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, DevClassNameHWUART); }
        }

        // Convert to integer(s)
        USB2GPIO.ba2ia(buff, buf);

        // Done
        return true;
    }

    @Override
    public boolean uartTx_discardSerialLoopback(final int[] buff)
    {
        // Error if not initialized
        if(!_initialized) return false;

        // Prepare the buffer
        final byte[] buf = USB2GPIO.ia2ba(buff);
              int    len = buf.length;
              int    ofs = 0;

        // Clear all the buffers
        // ##### !!! TODO : Is there a better way? !!! #####
        _ttyPort.flushIOBuffers();

        /*
        final byte[] discard = new byte[1];

        while( _ttyPort.bytesAwaitingWrite() > 0 ) {
            if( _ttyPort.bytesAvailable() > 0 ) _ttyPort.readBytes​(discard, 1);
        }
        while( _ttyPort.bytesAvailable() > 0 ) _ttyPort.readBytes​(discard, 1);
        */

        // Write the command byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        while(len != 0) {
            // Write some byte(s)
            final int cnt = _ttyPort.writeBytes(buf, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                                  }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_TxTimeout, DevClassNameHWUART); }
        }

        // Ensure all the bytes are written
      //_ttyPort.flushIOBuffers();
        while( _ttyPort.bytesAwaitingWrite() > 0 );

        // Read the echo
        final int[] chkb = new int[buff.length];

        if( !uartRx(chkb) ) return false;

        // Done
        return Arrays.equals(chkb, buff);
      //return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : The DASA hardware does not support SWIM

    @Override
    public boolean swimIsImplModeSupported(final ImplMode implMode)
    { return false; }

    @Override
    public boolean swimSetImplMode(final ImplMode implMode)
    { return false; }

    @Override
    public ImplMode swimGetImplMode()
    { return ImplMode.NotSupported; }

    @Override
    public boolean swimBegin()
    { return false; }

    @Override
    public boolean swimEnd()
    { return false; }

    @Override
    public boolean swimLineReset()
    { return false; }

    @Override
    public boolean swimTransfer(final int[] ioBuff, final int len2X)
    { return false; }

    @Override
    public boolean swimCmd_SRST()
    { return false; }

    @Override
    public boolean swimCmd_ROTF(final int[] buff, final int address24)
    { return false; }

    @Override
    public boolean swimCmd_WOTF(final int[] buff, final int address24)
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : The DASA hardware does not support JTAG

    @Override
    public boolean jtagIsModeSupported()
    { return false; }

    @Override
    public int jtagClkFreqToClkDiv(final int clkFreq)
    { return -1; }

    @Override
    public boolean jtagBegin(final int clkDiv)
    { return false; }

    @Override
    public boolean jtagEnd()
    { return false; }

    @Override
    public boolean jtagSetClkDiv(final int clkDiv)
    { return false; }

    @Override
    public boolean jtagSetReset(final boolean nRST, final boolean nTRST, final boolean TDI)
    { return false; }

    @Override
    public boolean jtagTMS(final boolean nRST, final boolean nTRST, final boolean TDI, final int bitNumMinusOne, final int value)
    { return false; }

    @Override
    public boolean jtagTransfer(boolean xUpdate, boolean drShift, boolean irShift, int bitCntLastMinusOne, final int[] ioBuff)
    { return false; }

    @Override
    public boolean jtagXBTransfer(boolean xUpdate, boolean drShift, boolean irShift, final int[] ioBuff)
    { return false; }

} // class DASA
