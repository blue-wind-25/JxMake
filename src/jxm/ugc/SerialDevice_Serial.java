/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import com.fazecast.jSerialComm.*;

import jxm.*;
import jxm.xb.*;


public class SerialDevice_Serial extends SerialDevice {

    private SerialPort _ttyPort = null;

    public SerialDevice_Serial(final SerialPort ttyPort)
    { _ttyPort = ttyPort; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean setFlowControl​(final int newFlowControlSettings)
    { return _ttyPort.setFlowControl​(newFlowControlSettings); }

    @Override
    public boolean setBaudRate​(final int newBaudRate)
    { return _ttyPort.setBaudRate​(newBaudRate); }

    @Override
    public boolean setNumDataBits​(final int newDataBits)
    { return _ttyPort.setNumDataBits​(newDataBits); }

    @Override
    public boolean setParity​(final int newParity)
    { return _ttyPort.setParity​(newParity); }

    @Override
    public boolean setNumStopBits​(final int newStopBits)
    { return _ttyPort.setNumStopBits​(newStopBits); }

    @Override
    public boolean setComPortTimeouts​(final int newTimeoutMode, final int newReadTimeout, final int newWriteTimeout)
    { return _ttyPort.setComPortTimeouts​(newTimeoutMode, newReadTimeout, newWriteTimeout); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean openPort()
    { return _ttyPort.openPort(); }

    @Override
    public boolean closePort()
    { return _ttyPort.closePort(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int bytesAvailable()
    { return _ttyPort.bytesAvailable(); }

    @Override
    public int readBytes​(final byte[] buffer, final int bytesToRead)
    { return _ttyPort.readBytes​(buffer, bytesToRead); }

    @Override
    public int readBytes​(final byte[] buffer, final int bytesToRead, final int offset)
    { return _ttyPort.readBytes​(buffer, bytesToRead, offset); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int bytesAwaitingWrite()
    { return _ttyPort.bytesAwaitingWrite(); }

    @Override
    public int writeBytes​(final byte[] buffer, final int bytesToWrite)
    { return _ttyPort.writeBytes​(buffer, bytesToWrite); }

    @Override
    public int writeBytes​(final byte[] buffer, final int bytesToWrite, final int offset)
    { return _ttyPort.writeBytes​(buffer, bytesToWrite, offset); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean flushIOBuffers()
    { return _ttyPort.flushIOBuffers(); }

    @Override
    public boolean serialDrain()
    {
        while( _ttyPort.bytesAwaitingWrite() > 0 );

        do {
            _ttyPort.flushIOBuffers();
            SysUtil.sleepMS(100);
        } while( _ttyPort.bytesAvailable() > 0 );

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean setBreak()
    { return _ttyPort.setBreak(); }

    @Override
    public boolean clearBreak()
    { return _ttyPort.clearBreak(); }

    @Override
    public boolean setDTR()
    { return _ttyPort.setDTR(); }

    @Override
    public boolean clearDTR()
    { return _ttyPort.clearDTR(); }

    @Override
    public boolean setRTS()
    { return _ttyPort.setRTS(); }

    @Override
    public boolean clearRTS()
    { return _ttyPort.clearRTS(); }

    @Override
    public boolean getCTS()
    { return _ttyPort.getCTS(); }

    @Override
    public boolean getDCD()
    { return _ttyPort.getDCD(); }

    @Override
    public boolean getDSR()
    { return _ttyPort.getDSR(); }

    @Override
    public boolean getRI()
    { return _ttyPort.getRI(); }

    @Override
    public boolean pulse_DTR_RTS(final int pulseLenUS, final int waitAfterMS)
    {
        // Bring DTR/RTS to 1 to clear the reset capacitor
        _ttyPort.clearDTR(           );
        _ttyPort.clearRTS(           );
        SysUtil.sleepUS  (pulseLenUS );

        // Bring DTR/RTS to 0
        _ttyPort.setDTR  (           );
        _ttyPort.setRTS  (           );
        SysUtil.sleepMS  (waitAfterMS);

        // Flush the serial
        flushIOBuffers();

        // Done
        return true;
    }

} // class SerialDevice_Serial
