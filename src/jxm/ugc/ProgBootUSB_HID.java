/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * ################################## !!! WARNING !!! ##################################
 * This source code file relies on the Foreign Function and Memory (FFM) API; therefore,
 * it is excluded from the build when using Java versions earlier than 23.
 * ################################## !!! WARNING !!! ##################################
 */


package jxm.ugc;


import java.util.List;

import net.codecrete.usb.*;

import jxm.*;
import jxm.xb.*;


/*
 * Base class for all 'ProgBoot*' subclasses that interface with bootloaders using the USB HID protocol.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written partially based on the algorithms and information found from:
 *
 *     USB Device Class Definition for Human Interface Devices v1.11
 *     https://www.usb.org/sites/default/files/documents/hid1_11.pdf
 *
 *     USB Human Interface Devices Usage Tables v1.6
 *     https://www.usb.org/sites/default/files/hut1_6.pdf
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * NOTE : On Windows, you may need to use a custom INF file or Zadig to assign WinUSB as the device
 *        driver.
 */
public abstract class ProgBootUSB_HID extends ProgBootUSB {

    /*
     * Transfer speed depends on the underlying protocol and the MCU's flash page size.
     */

    private static final String DevClassName = "ProgBootUSB_HID";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    protected static class HIDError extends Exception {

        public HIDError(final String errMsg, final Object... args)
        { super( String.format("%s: " + errMsg, ProgClassName, args) ); }

    } // class HIDError

    protected static void _throwHIDError(final String errMsg, final Object... args) throws HIDError
    { throw new HIDError(errMsg, args); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : These are the default values for raw HID devices
    private static final int DEF_HID_FN_DESC_bFDescriptorType   = 0x21; // HID functional descriptor - bDescriptorType
    private static final int DEF_HID_IF_DESC_bInterfaceClass    = 0x03; // HID interface  descriptor - bInterfaceClass
    private static final int DEF_HID_IF_DESC_bInterfaceSubClass = 0x00; // HID interface  descriptor - bInterfaceSubClass
    private static final int DEF_HID_IF_DESC_bInterfaceProtocol = 0x00; // HID interface  descriptor - bInterfaceProtocol

    protected static List<UsbDevice> _hidGetAllDevices(final int vid, final int pid, final int bFDescriptorType, final int bInterfaceClass, final int bInterfaceSubClass, final int bInterfaceProtocol)
    {
        return Usb.findDevices( (final UsbDevice device) -> {
            /*
            SysUtil.stdDbg().printf("%04X %04X %d %d\n", device.getVendorId(), device.getProductId(), getDescriptorOffset( device.getConfigurationDescriptor(), bFDescriptorType ), getInterfaceNumber(device, bInterfaceClass, bInterfaceSubClass, bInterfaceProtocol) );
            //*/
            return (
                ( device.getVendorId() == vid )
                &&
                ( device.getProductId() == pid )
                &&
                ( _getDescriptorOffset( device.getConfigurationDescriptor(), bFDescriptorType ) > 0 )
                &&
                ( _getInterfaceNumber(device, bInterfaceClass, bInterfaceSubClass, bInterfaceProtocol) >= 0 )
            );
        } );
    }

    protected static List<UsbDevice> _hidGetAllDevices(final int vid, final int pid, final int bInterfaceSubClass, final int bInterfaceProtocol)
    {
        return _hidGetAllDevices(
            vid,
            pid,
            DEF_HID_FN_DESC_bFDescriptorType,
            DEF_HID_IF_DESC_bInterfaceClass,
            bInterfaceSubClass,
            bInterfaceProtocol
        );
    }

    protected static List<UsbDevice> _hidGetAllDevices(final int vid, final int pid)
    { return _hidGetAllDevices(vid, pid, DEF_HID_IF_DESC_bInterfaceSubClass, DEF_HID_IF_DESC_bInterfaceProtocol); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected int _hidGetInterfaceNumber()
    { return _getInterfaceNumber(_device, DEF_HID_IF_DESC_bInterfaceClass, DEF_HID_IF_DESC_bInterfaceSubClass, DEF_HID_IF_DESC_bInterfaceProtocol); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : These are the default values for standard HID devices
    private static final int HID_REQ_GetReport = 0x01;
    private static final int HID_REQ_SetReport = 0x09;

    protected static UsbControlTransfer _hidUsbControlTransfer(final int request, final int value, final int index)
    { return new UsbControlTransfer(UsbRequestType.CLASS, UsbRecipient.INTERFACE, request, value, index); }

    protected UsbControlTransfer _hidUsbControlTransfer(final int request, final int value)
    { return _hidUsbControlTransfer(request, value, _interfaceNumber); } // NOTE : wIndex is often used to specify the interface number

    protected byte[] _hidRawControlRead(final int request, final int value, final int length)
    { return _device.controlTransferIn( _hidUsbControlTransfer(request, value), length ); }

    protected byte[] _hidRawControlRead(final int value, final int length)
    { return _hidRawControlRead(HID_REQ_GetReport, value, length); };

    protected void _hidRawControlWrite(final int request, final int value, final byte[] buff)
    { _device.controlTransferOut( _hidUsbControlTransfer(request, value), buff ); }

    protected void _hidRawControlWrite(final int value, final byte[] buff)
    { _hidRawControlWrite(HID_REQ_SetReport, value, buff); };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Implement generator/parser for standard HID reports ??? #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected ProgBootUSB_HID(final String progClassName, final Config config) throws Exception
    {
        // Process the superclass
        super(progClassName, config);
    }

} // class ProgBootUSB_HID
