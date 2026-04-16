/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import com.fazecast.jSerialComm.*;

import jxm.*;
import jxm.xb.*;


/*
 * Please refer to '../../../docs/txt/en_US/14-Simple-Programmer-Hardware.txt' (and its translations) for the connection diagram of some simple programmers.
 *
 * WARNING : Due to the way the USB-ISS communication protocol is designed, bit banging using USB-ISS is (probably) not very stable!
 */
public class USB_ISS extends USB2GPIO {

    private static final String DevClassName       = "USB-ISS:";

    private static final String DevClassNameBBSPI  = DevClassName + USB2GPIO.DevSubClassNameBBSPI;  // Master mode only - full        duplex
    private static final String DevClassNameBBUSRT = DevClassName + USB2GPIO.DevSubClassNameBBUSRT; // Master mode only -        half duplex
    private static final String DevClassNameBBUART = null;                                          // Not supported!

    private static final String DevClassNameHWSPI  = null;                                          // Not supported!
    private static final String DevClassNameHWUSRT = null;                                          // Not supported!
    private static final String DevClassNameHWUART = null;                                          // Not supported!

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int TX_RX_TIMEOUT_MS       = 1000;

    private static final int CMD_USB_ISS            = 0x5A;
    private static final int CMD_ISS_VERSION        = 0x01;
    private static final int CMD_ISS_MODE           = 0x02;
    private static final int CMD_GET_SER_NUM        = 0x03;

    private static final int CMD_RES_ACK            = 0xFF;

    private static final int ISS_MODE_IO_MODE       = 0x00;
    private static final int ISS_MODE_IO_CHANGE     = 0x10;
    private static final int ISS_MODE_I2C_S_20KHZ   = 0x20;
    private static final int ISS_MODE_I2C_S_50KHZ   = 0x30;
    private static final int ISS_MODE_I2C_S_100KHZ  = 0x40;
    private static final int ISS_MODE_I2C_S_400KHZ  = 0x50;
    private static final int ISS_MODE_I2C_H_100KHZ  = 0x60;
    private static final int ISS_MODE_I2C_H_400KHZ  = 0x70;
    private static final int ISS_MODE_I2C_H_1000KHZ = 0x80;
    private static final int ISS_MODE_SPI_MODE      = 0x90;
    private static final int ISS_MODE_SERIAL        = 0x01;

    private static final int IO_MODE_SETPINS        = 0x63;
    private static final int IO_MODE_GETPINS        = 0x64;
    private static final int IO_MODE_GETAD          = 0x65;

    private static final int IO_MODE_0_BITS         = 0b00;       // Output logic 0
    private static final int IO_MODE_1_BITS         = 0b01;       // Output logic 1
    private static final int IO_MODE_I_BITS         = 0b10;       // Digital input
    private static final int IO_MODE_A_BITS         = 0b11;       // Analog  input

    private static final int IO_MODE_ALL_I          = 0b10101010; // All digital inputs

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static byte[] _buildCmdBytes(final int numBytes, final int... bytes)
    {
        final byte[] cmd = new byte[numBytes];

        for(int i = 0; i < numBytes; ++i) cmd[i] = (byte) bytes[i];

        return cmd;
    }

    private static boolean _checkAck(final int res)
    { return res == CMD_RES_ACK; }

    private static boolean _checkAck(final int[] res)
    { return res[0] == CMD_RES_ACK && (res.length == 1 || res[1] == 0x00); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private SerialPort _ttyPort = null;

    private boolean _writeNCmd_impl(final int numBytes, final int[] bytes)
    {
        // Prepare the buffer
        final byte[] buf = _buildCmdBytes(numBytes, bytes);
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
            if(cnt > 0) { tms.reset();                      }
            else        { if( tms.timeout() ) return false; }
        }

        // Ensure all the bytes are written
      //_ttyPort.flushIOBuffers();
        while( _ttyPort.bytesAwaitingWrite() > 0 );

        // Done
        return true;
    }

    private boolean _writeNCmd(final int numBytes, final int... bytes)
    { return _writeNCmd_impl(numBytes, bytes); }

    private boolean _writeCmd(final int... bytes)
    { return _writeNCmd_impl(bytes.length, bytes); }

    private int[] _readRes(final int numBytes)
    {
        // Prepare the buffer
        final byte[] buf = new byte[numBytes];
              int    len = buf.length;
              int    ofs = 0;

        // Read the response byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        while(len != 0) {
            // Read some byte(s)
            final int cnt = _ttyPort.readBytes​(buf, len, ofs);
            if(cnt < 0) return null; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                     }
            else        { if( tms.timeout() ) return null; }
        }

        // Convert to integer(s) and return the result
        final int[] res = new int[numBytes];

        USB2GPIO.ba2ia(res, buf);
      //for(int i = 0; i < numBytes; ++i) res[i] = buf[i] & 0xFF;

        return res;
    }

    private boolean _readAck(final int numBytes)
    { return _checkAck( _readRes(numBytes) ); }

    private boolean _readAck2()
    { return _readAck(2); }

    private boolean _readAck1()
    { return _readAck(1); }

    private boolean _setAllIOPinsMode(final int pinModes)
    {
        _writeCmd(CMD_USB_ISS, CMD_ISS_MODE, ISS_MODE_IO_MODE, pinModes);

        return _readAck2();
    }

    private boolean _setAllIOPinsToInputMode()
    { return _setAllIOPinsMode(IO_MODE_ALL_I); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public USB_ISS(final String serialDevice) throws Exception
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
        SysUtil.stdDbg().println( "### USRT Standards : " + java.util.Arrays.toString( usrtGetStandardBaudrates() ) );
        SysUtil.stdDbg().println( "### " );
        SysUtil.stdDbg().println( "### UART Minimum   : " + uartGetMinimumBaudrate() );
        SysUtil.stdDbg().println( "### UART Maximum   : " + uartGetMaximumBaudrate() );
        SysUtil.stdDbg().println( "### UART Standards : " + java.util.Arrays.toString( uartGetStandardBaudrates() ) );
        SysUtil.stdDbg().println( "### " );
        //*/

        // Open the device
        _ttyPort = SerialPort.getCommPort(serialDevice);

        _ttyPort.setFlowControl​          (SerialPort.FLOW_CONTROL_DISABLED);
        _ttyPort.setBaudRate​             (1000000);
        _ttyPort.setNumDataBits​          (8);
        _ttyPort.setParity​               (SerialPort.NO_PARITY);
        _ttyPort.setNumStopBits​          (SerialPort.ONE_STOP_BIT);
        _ttyPort.setComPortTimeouts​      (SerialPort.TIMEOUT_NONBLOCKING, 1000, 1000);
        _ttyPort.clearBreak              ();
        _ttyPort.setDTR                  ();
        _ttyPort.setRTS                  ();
      //_ttyPort.disableExclusiveLock    ();
      //_ttyPort.disablePortConfiguration();
        _ttyPort.openPort                ();

        // Set all IO pins to input mode
        if( !_setAllIOPinsToInputMode() ) {
            _ttyPort.closePort();
            throw XCom.newException(Texts.USBISS_InitFailAPIM);
        }

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

    public int[] getVersion()
    {
        if( !_writeCmd(CMD_USB_ISS, CMD_ISS_VERSION) ) return null;

        return _readRes(3);
    }

    public int[] getSerialNumber()
    {
        if( !_writeCmd(CMD_USB_ISS, CMD_GET_SER_NUM) ) return null;

        return _readRes(8);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : The SS pin on USB-ISS is not user-programmable, hence, this class implements SPI bit banging

    private boolean _initialized   = false; // A flag that indicates if the system has been initialized
    private boolean _slaveSelected = false; // A flag that indicates if the slave  has been selected

    private int     _mosiIOM_N;             // IO_MODE mask to initialize MOSI as output with an inactive value
    private int     _mosiIOM_A;             // IO_MODE mask to initialize MOSI as output with an   active value
    private int     _mosiIOM_I;             // IO_MODE mask to initialize MOSI as input
    private int     _mosiOut_N;             // SETPINS mask to set        MOSI to the            inactive value
    private int     _mosiOut_A;             // SETPINS mask to set        MOSI to the              active value

    private int     _misoIOM_N;             // IO_MODE mask to initialize MISO as output with an inactive value
    private int     _misoIOM_A;             // IO_MODE mask to initialize MISO as output with an   active value
    private int     _misoIOM_I;             // IO_MODE mask to initialize MISO as input
    private int     _misoInp_M;             // GETPINS mask to read       MISO

    private int     _sclkIOM_N;             // IO_MODE mask to initialize SCK  as output with an inactive value
    private int     _sclkIOM_A;             // IO_MODE mask to initialize SCK  as output with an   active value
    private int     _sclkIOM_I;             // IO_MODE mask to initialize SCK  as input
    private int     _sclkOut_N;             // SETPINS mask to set        SCK  to the            inactive value
    private int     _sclkOut_A;             // SETPINS mask to set        SCK  to the              active value

    private int     _sselIOM_N;             // IO_MODE mask to initialize SS   as output with an inactive value
    private int     _sselIOM_A;             // IO_MODE mask to initialize SS   as output with an   active value
    private int     _sselIOM_I;             // IO_MODE mask to initialize SS   as input
    private int     _sselOut_N;             // SETPINS mask to set        SS   to the            inactive value
    private int     _sselOut_A;             // SETPINS mask to set        SS   to the              active value

    private boolean _cpha;                  // A flag that when set indicates SPI mode 1 or 3

    private void _resetSPIConfigData()
    {
        _mosiIOM_N     = IO_MODE_A_BITS; // Set as output with the inactive value
        _mosiIOM_A     = IO_MODE_A_BITS; // Set as output with the   active value
        _mosiIOM_I     = IO_MODE_A_BITS; // Set as input
        _mosiOut_N     = 0;              // Set output to the inactive value
        _mosiOut_A     = 0;              // Set output to the   active value

        _misoIOM_N     = IO_MODE_A_BITS; // Set as output with the inactive value
        _misoIOM_A     = IO_MODE_A_BITS; // Set as output with the   active value
        _misoIOM_I     = IO_MODE_A_BITS; // Set as input
        _misoInp_M     = 0;              // The input mask

        _sclkIOM_N     = IO_MODE_A_BITS; // Set as output with the inactive value
        _sclkIOM_A     = IO_MODE_A_BITS; // Set as output with the   active value
        _sclkIOM_I     = IO_MODE_A_BITS; // Set as input
        _sclkOut_N     = 0;              // Set output to the inactive value
        _sclkOut_A     = 0;              // Set output to the   active value

        _sselIOM_N     = IO_MODE_A_BITS; // Set as output with the inactive value
        _sselIOM_A     = IO_MODE_A_BITS; // Set as output with the   active value
        _sselIOM_I     = IO_MODE_A_BITS; // Set as input
        _sselOut_N     = 0;              // Set output to the inactive value
        _sselOut_A     = 0;              // Set output to the   active value

        _cpha          = false;          // Set as SPI mode 0 or 2 by default
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

    public boolean spiBegin(final SPIMode spiMode, final SSMode ssMode, final int mosiIOPin, final int misoIOPin, final int sckIOPin, final int ssIOPin)
    {
        // Error if already initialized
        if(_initialized) return false;

        // Error if the IO pins overlap
        if(mosiIOPin == misoIOPin || mosiIOPin == sckIOPin || mosiIOPin == ssIOPin || misoIOPin == sckIOPin || misoIOPin == ssIOPin || sckIOPin == ssIOPin) return false;

        // Determine the IO mode mask for the MOSI pin
        if(mosiIOPin < 1 || mosiIOPin > 4) return false; // Error if IO pin number is out of range

        final int mosiIOVShiftFactor = mosiIOPin - 1;
        final int mosiIOMShiftFactor = mosiIOVShiftFactor * 2;

        _mosiIOM_N = IO_MODE_0_BITS << mosiIOMShiftFactor;
        _mosiIOM_A = IO_MODE_1_BITS << mosiIOMShiftFactor;
        _mosiIOM_I = IO_MODE_I_BITS << mosiIOMShiftFactor;

        _mosiOut_N = 0 << mosiIOVShiftFactor;
        _mosiOut_A = 1 << mosiIOVShiftFactor;

        // Determine the IO mode mask for the MISO pin
        if(misoIOPin < 1 || misoIOPin > 4) return false; // Error if IO pin number is out of range

        final int misoIOVShiftFactor = misoIOPin - 1;
        final int misoIOMShiftFactor = misoIOVShiftFactor * 2;

        _misoIOM_N = IO_MODE_0_BITS << misoIOMShiftFactor;
        _misoIOM_A = IO_MODE_1_BITS << misoIOMShiftFactor;
        _misoIOM_I = IO_MODE_I_BITS << misoIOMShiftFactor;

        _misoInp_M = 1 << misoIOVShiftFactor;

        // Determine the IO mode mask for the SCK pin
        if(sckIOPin < 1 || sckIOPin > 4) return false; // Error if IO pin number is out of range

        final int sckIOVShiftFactor = sckIOPin - 1;
        final int sckIOMShiftFactor = sckIOVShiftFactor * 2;

        _sclkIOM_N = (spiMode.cpol ? IO_MODE_1_BITS : IO_MODE_0_BITS) << sckIOMShiftFactor;
        _sclkIOM_A = (spiMode.cpol ? IO_MODE_0_BITS : IO_MODE_1_BITS) << sckIOMShiftFactor;
        _sclkIOM_I =                                  IO_MODE_I_BITS  << sckIOMShiftFactor;

        _sclkOut_N = (spiMode.cpol ? 1 : 0) << sckIOVShiftFactor;
        _sclkOut_A = (spiMode.cpol ? 0 : 1) << sckIOVShiftFactor;

        // Determine the IO mode mask for the SS in
        if(ssIOPin < 1 || ssIOPin > 4) return false; // Error if IO pin number is out of range

        final int ssIOVShiftFactor = ssIOPin - 1;
        final int ssIOMShiftFactor = ssIOVShiftFactor * 2;

        _sselIOM_N = (ssMode == SSMode.ActiveLow ? IO_MODE_1_BITS : IO_MODE_1_BITS) << ssIOMShiftFactor;
        _sselIOM_A = (ssMode == SSMode.ActiveLow ? IO_MODE_0_BITS : IO_MODE_0_BITS) << ssIOMShiftFactor;
        _sselIOM_I =                                                IO_MODE_I_BITS  << ssIOMShiftFactor;

        _sselOut_N = (ssMode == SSMode.ActiveLow ? 1 : 1) << ssIOVShiftFactor;
        _sselOut_A = (ssMode == SSMode.ActiveLow ? 0 : 0) << ssIOVShiftFactor;

        // Save the clock phase
        _cpha = spiMode.cpha;

        // Set flag
        _initialized = true;

        // Done
        return true;
    }

    @Override
    public boolean spiBegin(final SPIMode spiMode, final SSMode ssMode, final int clkDiv)
    {
        // NOTE : SPI bit banging does not support (ignore) the specified clock divider

        // Initialize using the default IO pin numbers
        return spiBegin(spiMode, ssMode, 2, 4, 3, 1);
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

        // Select the slave
        if( !_setAllIOPinsMode(_mosiIOM_I | _misoIOM_I | _sclkIOM_I | _sselIOM_A) ) return false;

        SysUtil.sleepMS(1);

        // Set the MOSI and SCK pins to output with their inactive (idle) values
        if( !_setAllIOPinsMode(_mosiIOM_N | _misoIOM_I | _sclkIOM_N | _sselIOM_A) ) return false;

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

        // Set the MOSI and SCK pins to input
        if( !_setAllIOPinsMode(_mosiIOM_I | _misoIOM_I | _sclkIOM_I | _sselIOM_A) ) return false;

        SysUtil.sleepMS(1);

        // Deselect the slave
        if( !_setAllIOPinsMode(_mosiIOM_I | _misoIOM_I | _sclkIOM_I | _sselIOM_N) ) return false;

        SysUtil.sleepMS(1);

        // Set all pins to input mode
        if( !_setAllIOPinsToInputMode() ) return false;

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
        if( !_writeCmd( IO_MODE_SETPINS, _mosiOut_N | _sclkOut_N | _sselOut_N ) ) return false;
        if( !_readAck1() ) return false;

        SysUtil.sleepMS(postDeselectDelayTime_MS);

        // Reselect the slave
        if( !_writeCmd( IO_MODE_SETPINS, _mosiOut_N | _sclkOut_N | _sselOut_A ) ) return false;
        if( !_readAck1() ) return false;

        SysUtil.sleepMS(postReselectDelayTime_MS);

        // Done
        return true;
    }

    private boolean _bbspiTransfer_very_slow_mosiBitOut(final int mosiBitValue)
    {
        if( !_writeCmd(IO_MODE_SETPINS, mosiBitValue | _sclkOut_N | _sselOut_A) ) return false;
        return _readAck1();
    }

    private boolean _bbspiTransfer_very_slow_sclkActive(final int mosiBitValue)
    {
        if( !_writeCmd(IO_MODE_SETPINS, mosiBitValue | _sclkOut_A | _sselOut_A) ) return false;
        return _readAck1();
    }

    private boolean _bbspiTransfer_very_slow_sclkInactive(final int mosiBitValue)
    {
        if( !_writeCmd(IO_MODE_SETPINS, mosiBitValue | _sclkOut_N | _sselOut_A) ) return false;
        return _readAck1();
    }

    private int _bbspiTransfer_very_slow_misoBitInp()
    {
        if( !_writeCmd(IO_MODE_GETPINS) ) return -1;
        return ( ( _readRes(1)[0] & _misoInp_M ) != 0 ) ? 1 : 0;
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
                final int mosiBitValue = ( (ioBuff[i] & 0b10000000) != 0 ) ? _mosiOut_A : _mosiOut_N;

                // Shift the bit
                ioBuff[i] = (ioBuff[i] << 1) & 0xFF;

                // SPI mode 1 and 3
                if(_cpha) {
                    // Make the SCK active
                    if( !_bbspiTransfer_very_slow_sclkActive(mosiBitValue) ) return false;
                    // Output the bit
                    if( !_bbspiTransfer_very_slow_mosiBitOut(mosiBitValue) ) return false;
                    // Make the SCK inactive
                    if( !_bbspiTransfer_very_slow_sclkInactive(mosiBitValue) ) return false;
                    // Read the response and store the bit that has been received (MSB first)
                    final int misoVal = _bbspiTransfer_very_slow_misoBitInp();
                    if(misoVal < 0) return false;
                    ioBuff[i] = ioBuff[i] | misoVal;
                }
                // SPI mode 0 and 2
                else {
                    // Output the bit
                    if( !_bbspiTransfer_very_slow_mosiBitOut(mosiBitValue) ) return false;
                    // Make the SCK active
                    if( !_bbspiTransfer_very_slow_sclkActive(mosiBitValue) ) return false;
                    // Read the response and store the bit that has been received (MSB first)
                    final int misoVal = _bbspiTransfer_very_slow_misoBitInp();
                    if(misoVal < 0) return false;
                    ioBuff[i] = ioBuff[i] | misoVal;
                    // Make the SCK inactive
                    if( !_bbspiTransfer_very_slow_sclkInactive(mosiBitValue) ) return false;
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
        // NOTE : USB-ISS does not support changing the SPI mode
        return false;
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

    // NOTE : # USB-ISS does not have a hardware USRT, hence, this class implements bit banging USRT
    //        # The bit banging USRT utilizes some of the bit banging SPI code

    private static final int RX_WAIT_START_BIT_TIMEOUT_MS = 1000;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _parityEven  = false;
    private boolean _parityOdd   = false;

    private boolean _twoStopBits = false;

    private int     _txdPinVal   = 0;
    private int     _xckPinVal   = 0;
    private int     _xssPinVal   = 0;

    private void _resetUSRTConfigData()
    {
        _resetSPIConfigData();

        _parityEven  = false;
        _parityOdd   = false;

        _twoStopBits = false;
    }

    private boolean _txd(final boolean value)
    {
        _txdPinVal = value ? _mosiOut_A : _mosiOut_N;

        if( !_writeCmd(IO_MODE_SETPINS, _txdPinVal | _xckPinVal | _xssPinVal) ) return false;

        return _readAck1();
    }

    private int _rxd()
    { return _bbspiTransfer_very_slow_misoBitInp(); }


    private boolean _xck(final boolean value)
    {
        _xckPinVal = value ? _sclkOut_A : _sclkOut_N;

        if( !_writeCmd(IO_MODE_SETPINS, _txdPinVal | _xckPinVal | _xssPinVal) ) return false;

        return _readAck1();
    }

    private boolean _xss(final boolean value)
    {
        _xssPinVal = value ? _sselOut_A : _sselOut_N;

        if( !_writeCmd(IO_MODE_SETPINS, _txdPinVal | _xckPinVal | _xssPinVal) ) return false;

        return _readAck1();
    }

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

    public boolean usrtBegin(final UXRTMode uxrtMode, final SSMode ssMode, final int txIOPin, final int rxIOPin, final int xckIOPin, final int ssIOPin)
    {
        // Error if already initialized
        if(_initialized) return false;

        // Initialize the IO
        if( !spiBegin(USB2GPIO.SPIMode._0, ssMode, txIOPin, rxIOPin, xckIOPin, ssIOPin) ) return false;

        // Save configuration
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

        // Initialize using the default IO pin numbers
        return usrtBegin(uxrtMode, ssMode, 2, 4, 3, 1);
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
    {
        // Select the slave
        if( !spiSelectSlave() ) return false;

        // Save the initial values of the output pins
        _txdPinVal = _mosiOut_N;
        _xckPinVal = _sclkOut_N;
        _xssPinVal = _sselOut_A;

        // Done
        return true;
    }

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
        if( !_txd(txValue) ) return false;

        // Pulse the clock
        for(int i = 0; i < count; ++i) {
            if( !_xck(false) ) return false;
            if( !_xck(true ) ) return false;
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
            if( !_xck(false) ) return false;
            if( !_txd(false) ) return false;
            if( !_xck(true ) ) return false;

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
                if( !_xck(false) ) return false;
                if( !_txd(db   ) ) return false;
                if( !_xck(true ) ) return false;

            } // for b

            // Send the parity bit as needed
            if(_parityOdd) parity = !parity;

            if(_parityEven || _parityOdd) {
                if( !_xck(false ) ) return false;
                if( !_txd(parity) ) return false;
                if( !_xck(true  ) ) return false;
            }

            // Send the stop bit(s)
                if( !_xck(false) ) return false;
                if( !_txd(true ) ) return false;
                if( !_xck(true ) ) return false;
            if(_twoStopBits) {
                if( !_xck(false) ) return false;
                if( !_txd(true ) ) return false;
                if( !_xck(true ) ) return false;
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
                if( !_xck(false) ) return false;
                if( !_xck(true ) ) return false;
                final int rxd = _rxd();
                if(rxd <  0) return false;
                if(rxd == 0) break;
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
                if( !_xck(false) ) return false;
                if( !_xck(true ) ) return false;
                final int rxd = _rxd();
                if(rxd <  0) return false;
                final boolean db = _rxd() != 0;

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
                if( !_xck(false) ) return false;
                if( !_xck(true ) ) return false;
                final int rxd = _rxd();
                if(rxd <  0) return false;
                final boolean pb = _rxd() != 0;
                // Update and compare the parity bit
                if(_parityOdd) parity = !parity;
                if(pb != parity) {
                    USB2GPIO.notifyError(Texts.PDevXXX_ParityError, DevClassNameBBUSRT);
                    return false;
                }
            }

            // Receive the stop bit(s)
            if(true) {
                if( !_xck(false) ) return false;
                if( !_xck(true ) ) return false;
                final int rxd = _rxd();
                if(rxd <  0) return false;
                final boolean ob = _rxd() != 0;
                if(ob != true) {
                    USB2GPIO.notifyError(Texts.PDevXXX_StopBit1Error, DevClassNameBBUSRT);
                    return false;
                }
            }
            if(_twoStopBits) {
                if( !_xck(false) ) return false;
                if( !_xck(true ) ) return false;
                final int rxd = _rxd();
                if(rxd <  0) return false;
                final boolean ob = _rxd() != 0;
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

    // NOTE : The USB-ISS hardware UART implementation is too limited, hence, there will be no UART support here

    @Override
    public boolean uartIsImplModeSupported(final ImplMode implMode)
    { return false; }

    @Override
    public boolean uartSetImplMode(final ImplMode implMode)
    { return false; }

    @Override
    public ImplMode uartGetImplMode()
    { return ImplMode.NotSupported; }

    @Override
    public boolean uartIsOperationalModeSupported(final OperationalMode operationalMode)
    { return false; }

    @Override
    public boolean uartIsDuplexModeSupported(final DuplexMode duplexMode)
    { return false; }

    @Override
    public boolean uartIsEnablingDisablingTxSupported()
    { return false; }

    @Override
    public int uartGetMinimumBaudrate()
    { return UndeterminedFrequency; }

    @Override
    public int uartGetMaximumBaudrate()
    { return UndeterminedFrequency; }

    // NOTE : UART is not supported by USB-ISS.
    @Override
    public int[] uartGetStandardBaudrates()
    { return null; }

    @Override
    public boolean uartBegin(final UXRTMode uxrtMode, final int baudrate)
    { return false; }

    @Override
    public boolean uartEnd()
    { return false; }

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
    { return false; }

    @Override
    public boolean uartTx(final int[] buff)
    { return false; }

    @Override
    public boolean uartRx(final int[] buff)
    { return false; }

    @Override
    public boolean uartTx_discardSerialLoopback(final int[] buff)
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : The USB-ISS hardware does not support SWIM

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

    // NOTE : The USB-ISS hardware does not support JTAG

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

} // class USB_ISS
