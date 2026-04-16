/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import jxm.*;
import jxm.xb.*;


public abstract class SerialDevice {

    public static final int FLOW_CONTROL_DISABLED            = com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DISABLED;
    public static final int FLOW_CONTROL_XONXOFF_IN_ENABLED  = com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED;
    public static final int FLOW_CONTROL_XONXOFF_OUT_ENABLED = com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED;

    public static final int NO_PARITY                        = com.fazecast.jSerialComm.SerialPort.NO_PARITY;
    public static final int EVEN_PARITY                      = com.fazecast.jSerialComm.SerialPort.EVEN_PARITY;
    public static final int ODD_PARITY                       = com.fazecast.jSerialComm.SerialPort.ODD_PARITY;

    public static final int ONE_STOP_BIT                     = com.fazecast.jSerialComm.SerialPort.ONE_STOP_BIT;
    public static final int TWO_STOP_BITS                    = com.fazecast.jSerialComm.SerialPort.TWO_STOP_BITS;

    public static final int TIMEOUT_NONBLOCKING              = com.fazecast.jSerialComm.SerialPort.TIMEOUT_NONBLOCKING;
    public static final int TIMEOUT_READ_BLOCKING            = com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING;
    public static final int TIMEOUT_WRITE_BLOCKING           = com.fazecast.jSerialComm.SerialPort.TIMEOUT_WRITE_BLOCKING;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String JXM_PREFIX = "jxm:";
    private static final String NET_PREFIX = "net:";

    private static boolean _isJxmPort(final String portDescriptor)
    { return portDescriptor.startsWith(JXM_PREFIX); }

    private static boolean _isNetPort(final String portDescriptor)
    { return portDescriptor.startsWith(NET_PREFIX); }

    private static String[] _getJxmPort(final String portDescriptor)
    {
        final String[] parts = portDescriptor.substring( JXM_PREFIX.length() ).split(":", -1);

        return (parts.length == 2) ? parts : null;
    }

    private static String[] _getNetPort(final String portDescriptor)
    {
        final String[] parts = portDescriptor.substring( NET_PREFIX.length() ).split(":", -1);

        return (parts.length == 3) ? parts : null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * Port Descriptor for JxMake USB-GPIO Module
     *
     *     String format:
     *         jxm:<primary_serial_device>:<secondary_serial_device>
     *
     *     Examples:
     *         jxm:COM15:COM16
     *         jxm:/dev/ttyACM0:/dev/ttyACM1
     *         jxm:dev/cu.usbmodem141011:/dev/cu.usbmodemusbmodem14102
     *
     * Port Descriptor for Serial-over-Network Device
     *
     *     String format:
     *         net:<hostNameOrIP>:<uploadPort>:[urlSetBaudrate_printfFormat]
     *
     *     Examples:
     *         net:esp-link.local:2323:console/baud?rate=%d
     *         net:10.0.0.111:2323:console/baud?rate=%d
     *         net:192.168.0.1:2323:
     */
    public static SerialDevice getCommPort​(final String portDescriptor)
    {
        if( _isJxmPort(portDescriptor) ) {
            final String[] jxmPort = _getJxmPort(portDescriptor);
            return new SerialDevice_JxMakeUSBGPIO(jxmPort[0], jxmPort[1]);
        }

        if( _isNetPort(portDescriptor) ) {
            final String[] netPort        = _getNetPort(portDescriptor);
            final String   hostNameOrIP   = netPort[0].trim();
            final int      uploadPort     = Integer.parseInt( netPort[1].trim() );
            final String   sbURLFormat    = netPort[2].trim();
            final boolean  hasSBURL       = !sbURLFormat.isEmpty();
            final String   urlSetBaudrate = hasSBURL ? ("http://" + hostNameOrIP + "/" + sbURLFormat) : null;
            return new SerialDevice_Network(hostNameOrIP, uploadPort, urlSetBaudrate);
        }

        return new SerialDevice_Serial ( com.fazecast.jSerialComm.SerialPort.getCommPort(portDescriptor) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean waitForReEnumeration(final String portDescriptor, final int waitTimeoutMS, final int afterDelayMS)
    {
        // Simply return true if it is not a normal serial device
        if( _isJxmPort(portDescriptor) || _isNetPort(portDescriptor) ) {
            SysUtil.sleepMS(afterDelayMS);
            return true;
        }

        // Wait until the device disappear
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(waitTimeoutMS);

        while(true) {
            try {
                getCommPort​(portDescriptor);
            }
            catch(final Exception e) {
                break;
            }
            if( tms.timeout() ) return false;
        }

        // Wait until the device reappear
        while(true) {
            try {
                getCommPort​(portDescriptor);
                break;
            }
            catch(final Exception e) {}
            if( tms.timeout() ) return false;
        }

        // Wait for a while
        SysUtil.sleepMS(afterDelayMS);

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    public abstract boolean setFlowControl​(final int newFlowControlSettings);
    public abstract boolean setBaudRate​(final int newBaudRate);
    public abstract boolean setNumDataBits​(final int newDataBits);
    public abstract boolean setParity​(final int newParity);
    public abstract boolean setNumStopBits​(final int newStopBits);
    public abstract boolean setComPortTimeouts​(final int newTimeoutMode, final int newReadTimeout, final int newWriteTimeout);

    public abstract boolean openPort();
    public abstract boolean closePort();

    public abstract int bytesAvailable();
    public abstract int readBytes​(final byte[] buffer, final int bytesToRead);
    public abstract int readBytes​(final byte[] buffer, final int bytesToRead, final int offset);

    public abstract int bytesAwaitingWrite();
    public abstract int writeBytes​(final byte[] buffer, final int bytesToWrite);
    public abstract int writeBytes​(final byte[] buffer, final int bytesToWrite, final int offset);

    public abstract boolean flushIOBuffers();
    public abstract boolean serialDrain();

    public abstract boolean setBreak();
    public abstract boolean clearBreak();

    public abstract boolean setDTR();
    public abstract boolean clearDTR();

    public abstract boolean setRTS();
    public abstract boolean clearRTS();

    public abstract boolean getCTS();
    public abstract boolean getDCD();
    public abstract boolean getDSR();
    public abstract boolean getRI();

    public abstract boolean pulse_DTR_RTS(final int pulseLenUS, final int waitAfterMS);

} // class SerialDevice
