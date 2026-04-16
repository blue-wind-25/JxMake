/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.util.ArrayList;

import com.intellectualsites.http.*;

import jxm.*;
import jxm.xb.*;
import jxm.xb.fci.*;


public class SerialDevice_Network extends SerialDevice {

    private String _hostNameOrIP   = null;
    private int    _uploadPort     = -1;
    private String _urlSetBaudrate = null;

    public SerialDevice_Network(final String hostNameOrIP, final int uploadPort, final String urlSetBaudrate)
    {
        _hostNameOrIP   = hostNameOrIP;
        _uploadPort     = uploadPort;
        _urlSetBaudrate = urlSetBaudrate;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int SOCKET_TIMEOUT_MS_MIN = 1000;
    private static final int SOCKET_TIMEOUT_MS_STD = 5000;

    @Override
    public boolean setFlowControl​(final int newFlowControlSettings)
    {
        // NOTE : Flow control is not supported
        return newFlowControlSettings == SerialDevice.FLOW_CONTROL_DISABLED;
    }

    @Override
    public boolean setBaudRate​(final int newBaudRate)
    {
        // If setting the baudrate via URL is unsupported, simply return success
        if(_urlSetBaudrate == null) return true;

        // Generate the final URL
        final String url = String.format(_urlSetBaudrate, newBaudRate);

        // ##### !!! TODO : Retry on error or timeout? !!! #####

        // Create a new HTTP request instance
        final HttpClient.WrappedRequestBuilder req = HTTP._newHttpRequest( url, HTTP.RMethod.GET, new ArrayList<>() );

        if(req == null) return false;

        // Perform the HTTP HEAD/GET request
        final HTTP.ReqExWrapper rew = new HTTP.ReqExWrapper();
        final HttpResponse res = req.onException( e -> { rew.set(e); }  )
                                    .execute    ( SOCKET_TIMEOUT_MS_STD );

        // Process the response
        final XCom.VariableValue retVal = new XCom.VariableValue();

        try {
            HTTP._processHTTPResponse(retVal, rew, res, false);
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Report error
            return false;
        }

        // Print messages
        SysUtil.stdDbg().println();
        SysUtil.stdDbg().println(url);
        for(final XCom.VariableStore vs : retVal) SysUtil.stdDbg().println(vs.value);
        SysUtil.stdDbg().println();

        // Done
        return true;
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
        // NOTE : Parity is not supported
        return newParity == SerialDevice.NO_PARITY;
    }

    @Override
    public boolean setNumStopBits​(final int newStopBits)
    {
        // NOTE : Only 1 stop bit is supported
        return newStopBits == SerialDevice.ONE_STOP_BIT;
    }

    @Override
    public boolean setComPortTimeouts​(final int newTimeoutMode, final int newReadTimeout, final int newWriteTimeout)
    {
        // NOTE : Timeout configuration is not supported; this call is silently ignored
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Socket       _socket = null;
    private InputStream  _is     = null;
    private OutputStream _os     = null;

    @Override
    public boolean openPort()
    {
        try {
            // Open socket
            // ##### !!! TODO : Retry on error or timeout? !!! #####
            _socket = new Socket();
            _socket.connect( new InetSocketAddress(_hostNameOrIP, _uploadPort), SOCKET_TIMEOUT_MS_STD );
            _socket.setTcpNoDelay(true);
            // Get the strams
            _is = _socket.getInputStream();
            _os = _socket.getOutputStream();
            // Print messages
            SysUtil.stdDbg().println();
            SysUtil.stdDbg().println(_hostNameOrIP + ":" + _uploadPort);
            SysUtil.stdDbg().println();
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Report error
            return false;
        }

        // Done
        return true;
    }

    @Override
    public boolean closePort()
    {
        try {
            _socket.close();
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Report error
            return false;
        }

        _is = null;
        _os = null;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int bytesAvailable()
    {
        try {
            // Return the number of bytes available for reading
            return _is.available();
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Report error
            return -1;
        }
    }

    @Override
    public int readBytes​(final byte[] buffer, final int bytesToRead)
    { return readBytes​(buffer, bytesToRead, 0); }

    @Override
    public int readBytes​(final byte[] buffer, final int bytesToRead, final int offset)
    {
        int totalRead = 0;

        try {
            // Use the minimum timeout
            _socket.setSoTimeout(SOCKET_TIMEOUT_MS_MIN);
            // Read the bytes
            while(totalRead < bytesToRead) {
                /*
                SysUtil.stdDbg().printf("readBytes​(%d, %d)\n", bytesToRead, offset);
                //*/
                final int bytesRead = _is.read(buffer, offset + totalRead, bytesToRead - totalRead);
                if(bytesRead == -1) return -1;
                totalRead += bytesRead;
            }
        }
        catch(final SocketTimeoutException e) {
            // On timeout return 0, not error
            return 0;
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Report error
            return -1;
        }

        return totalRead;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int bytesAwaitingWrite()
    {
        // NOTE : Checking the number of bytes awaiting write is not supported
        return 0;
    }

    @Override
    public int writeBytes​(final byte[] buffer, final int bytesToWrite)
    { return writeBytes​(buffer, bytesToWrite, 0); }

    @Override
    public int writeBytes​(final byte[] buffer, final int bytesToWrite, final int offset)
    {
        try {
            /*
            SysUtil.stdDbg().printf("writeBytes​(%d, %d)\n", bytesToWrite, offset);
            for(int i = offset; i < offset + bytesToWrite; ++i)  SysUtil.stdDbg().printf("%02X ", buffer[i]);
             SysUtil.stdDbg().println();
            //*/
            // Write the bytes
            _os.write(buffer, offset, bytesToWrite);
            _os.flush();
            return bytesToWrite;
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Report error
            return -1;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean flushIOBuffers()
    {
        try {
            // Flush the output stream and drain the input stream
            _os.flush();
            while( _is.available() > 0 ) _is.read();
            return true;
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Report error
            return false;
        }
    }

    @Override
    public boolean serialDrain()
    {
        do {
            if( !flushIOBuffers() ) return false;
            SysUtil.sleepMS(100);
        } while( bytesAvailable() > 0 );

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean setBreak()
    {
        // NOTE : Setting or clearing control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean clearBreak()
    {
        // NOTE : Setting or clearing control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean setDTR()
    {
        // NOTE : Setting or clearing control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean clearDTR()
    {
        // NOTE : Setting or clearing control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean setRTS()
    {
        // NOTE : Setting or clearing control signals is not supported; this call is silently ignored
        return true;
    }

    @Override
    public boolean clearRTS()
    {
        // NOTE : Setting or clearing control signals is not supported; this call is silently ignored
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
        // NOTE : Pulsing control signals is not supported; this call is silently ignored
        return true;
    }

} // class SerialDevice_Network
