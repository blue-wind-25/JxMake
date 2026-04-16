/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import com.fazecast.jSerialComm.*;

import jxm.*;
import jxm.xb.*;


public class SerialDevice_JxMakeUSBGPIO extends SerialDevice {

    private String     _primarySerialDevice   = null;
    private String     _secondarySerialDevice = null;
    private int        _flowControl           = SerialDevice.FLOW_CONTROL_DISABLED;
    private int        _baudrate              = 9600;
    private int        _parity                = SerialDevice.NO_PARITY;
    private int        _stopBits              = SerialDevice.ONE_STOP_BIT;

    private USB_GPIO   _usbgpio               = null;
    private SerialPort _ttyPort               = null;

    public SerialDevice_JxMakeUSBGPIO(final String primarySerialDevice, final String secondarySerialDevice)
    {
        _primarySerialDevice   = primarySerialDevice;
        _secondarySerialDevice = secondarySerialDevice;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean setFlowControl​(final int newFlowControlSettings)
    {
        // NOTE : Only 'no flow control' and 'software flow control' are supported

        if( ( newFlowControlSettings !=  SerialDevice.FLOW_CONTROL_DISABLED                                                            ) &&
            ( newFlowControlSettings != (SerialDevice.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialDevice.FLOW_CONTROL_XONXOFF_OUT_ENABLED) )
        ) return false;

        _flowControl = newFlowControlSettings;

        if(_ttyPort == null) return true;

        return _ttyPort.setFlowControl​(newFlowControlSettings);
    }

    @Override
    public boolean setBaudRate​(final int newBaudRate)
    {
        _baudrate = newBaudRate;

        if(_ttyPort == null) return true;

        return _ttyPort.setBaudRate​(newBaudRate);
    }

    @Override
    public boolean setNumDataBits​(final int newDataBits)
    {
        // NOTE : Only 8 data bits are supported
        return newDataBits == 8;
    }

    @Override
    public boolean setParity​(final int newParity)
    {
        // NOTE : Only 'no parity', 'even parity', and 'odd parity' are supported

        if(newParity != SerialDevice.NO_PARITY && newParity != SerialDevice.EVEN_PARITY && newParity != SerialDevice.ODD_PARITY) return false;

        _parity = newParity;

        if(_ttyPort == null) return true;

        return _ttyPort.setParity​(newParity);
    }

    @Override
    public boolean setNumStopBits​(final int newStopBits)
    {
        // NOTE : Only one and two stop bits are supported

        if(newStopBits != SerialDevice.ONE_STOP_BIT && newStopBits != SerialDevice.TWO_STOP_BITS) return false;

        _stopBits = newStopBits;

        if(_ttyPort == null) return true;

        return _ttyPort.setNumStopBits​(newStopBits);
    }

    @Override
    public boolean setComPortTimeouts​(final int newTimeoutMode, final int newReadTimeout, final int newWriteTimeout)
    {
        // NOTE : Timeout configuration is not supported; this call is silently ignored
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : This implementation uses the SPI nSS line as the UART DTR signal

    @Override
    public boolean openPort()
    {
        // Error if already open
        if(_ttyPort != null) return false;

        // Instantiate the JxMake USB-GPIO module
        USB_GPIO usbgpio = null;

        try {
            usbgpio = new USB_GPIO(_primarySerialDevice, _secondarySerialDevice);
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Report error
            return false;
        }

        // Initialize the JxMake USB-GPIO module
        final boolean enDM_Error       =  true;
        final boolean enDM_Warning     =  true;
        final boolean enDM_Notice      =  true;
        final boolean enDM_Information = !true;

        usbgpio.setAutoNotifyErrorMessage(true);
        usbgpio.enableDebugMessage(enDM_Error, enDM_Warning, enDM_Notice, enDM_Information);

        // Initialize the USART
        USB2GPIO.UXRTMode uxrtMode = USB2GPIO.UXRTMode._8N1;

        if(_parity == SerialDevice.NO_PARITY) {
                 if(_stopBits == SerialDevice.ONE_STOP_BIT ) uxrtMode = USB2GPIO.UXRTMode._8N1;
            else if(_stopBits == SerialDevice.TWO_STOP_BITS) uxrtMode = USB2GPIO.UXRTMode._8N2;
        }
        else if(_parity == SerialDevice.EVEN_PARITY) {
                 if(_stopBits == SerialDevice.ONE_STOP_BIT ) uxrtMode = USB2GPIO.UXRTMode._8E1;
            else if(_stopBits == SerialDevice.TWO_STOP_BITS) uxrtMode = USB2GPIO.UXRTMode._8E2;
        }
        else if(_parity == SerialDevice.ODD_PARITY) {
                 if(_stopBits == SerialDevice.ONE_STOP_BIT ) uxrtMode = USB2GPIO.UXRTMode._8O1;
            else if(_stopBits == SerialDevice.TWO_STOP_BITS) uxrtMode = USB2GPIO.UXRTMode._8O2;
        }

        if( !usbgpio.uartBegin(uxrtMode, _baudrate) ) {
            usbgpio.resetAndShutdown();
            return false;
        }

        if( !usbgpio.spiBegin(USB2GPIO.SPIMode._0, USB2GPIO.SSMode.ActiveLow, 0) ) {
            usbgpio.uartEnd();
            usbgpio.resetAndShutdown();
            return false;
        }

        SysUtil.sleepMS(100);

        // Save the USB-GPIO module instance
        _usbgpio = usbgpio;
         usbgpio = null;

        // Get the secondary serial port
        _ttyPort = _usbgpio.rawSerialPort();

        _ttyPort.setFlowControl​(_flowControl);

        // Done
        return true;
    }

    @Override
    public boolean closePort()
    {
        if(_ttyPort != null) {

            _usbgpio.spiEnd();
            _usbgpio.uartEnd();
            _usbgpio.shutdown();

            _usbgpio = null;
            _ttyPort = null;

        } // if

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int bytesAvailable()
    { return (_ttyPort == null) ? - 1 : _ttyPort.bytesAvailable(); }

    @Override
    public int readBytes​(final byte[] buffer, final int bytesToRead)
    { return (_ttyPort == null) ? - 1 : _ttyPort.readBytes​(buffer, bytesToRead); }

    @Override
    public int readBytes​(final byte[] buffer, final int bytesToRead, final int offset)
    { return (_ttyPort == null) ? - 1 : _ttyPort.readBytes​(buffer, bytesToRead, offset); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int bytesAwaitingWrite()
    { return (_ttyPort == null) ? - 1 : _ttyPort.bytesAwaitingWrite(); }

    @Override
    public int writeBytes​(final byte[] buffer, final int bytesToWrite)
    { return (_ttyPort == null) ? - 1 : _ttyPort.writeBytes​(buffer, bytesToWrite); }

    @Override
    public int writeBytes​(final byte[] buffer, final int bytesToWrite, final int offset)
    { return (_ttyPort == null) ? - 1 : _ttyPort.writeBytes​(buffer, bytesToWrite, offset); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean flushIOBuffers()
    { return (_ttyPort == null) ? true : _ttyPort.flushIOBuffers(); }

    @Override
    public boolean serialDrain()
    {
        if(_ttyPort != null) {

            while( _ttyPort.bytesAwaitingWrite() > 0 );

            do {
                _ttyPort.flushIOBuffers();
                SysUtil.sleepMS(100);
            } while( _ttyPort.bytesAvailable() > 0 );

        } // if

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean setBreak()
    { return (_ttyPort == null) ? false : _ttyPort.setBreak(); }

    @Override
    public boolean clearBreak()
    { return (_ttyPort == null) ? false : _ttyPort.clearBreak(); }

    @Override
    public boolean setDTR()
    { return (_usbgpio == null) ? false : _usbgpio.spiSelectSlave(); }

    @Override
    public boolean clearDTR()
    { return (_usbgpio == null) ? false : _usbgpio.spiDeselectSlave(); }

    @Override
    public boolean setRTS()
    {
        // NOTE : Setting or clearing this control signal is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean clearRTS()
    {
        // NOTE : Setting or clearing this control signal is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean getCTS()
    {
        // NOTE : Reading control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean getDCD()
    {
        // NOTE : Reading control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean getDSR()
    {
        // NOTE : Reading control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean getRI()
    {
        // NOTE : Reading control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean pulse_DTR_RTS(final int pulseLenUS, final int waitAfterMS)
    {
        // Bring DTR/RTS to 1 to clear the reset capacitor
        clearDTR       (           );
        clearRTS       (           );
        SysUtil.sleepUS(pulseLenUS );

        // Bring DTR/RTS to 0
        setDTR         (           );
        setRTS         (           );
        SysUtil.sleepMS(waitAfterMS);

        // Flush the serial
        flushIOBuffers();

        // Done
        return true;
    }

} // class SerialDevice_JxMakeUSBGPIO
