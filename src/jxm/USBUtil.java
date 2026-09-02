/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;

import net.codecrete.usb.*;

import jxm.xb.*;


public class USBUtil  {

    public static class USBDevice {

        final int    vid;
        final int    pid;

        final String manufacturerName;
        final String productName;
        final String serialNumber;

        final int    classCode;
        final int    subclassCode;
        final int    protocolCode;

        final String usbVersion;
        final String deviceVersion;

        public USBDevice(
            final int    vid,
            final int    pid,

            final String manufacturerName,
            final String productName,
            final String serialNumber,

            final int    classCode,
            final int    subclassCode,
            final int    protocolCode,

            final String usbVersion,
            final String deviceVersion
        ) {
            this.vid              = vid;
            this.pid              = pid;

            this.manufacturerName = manufacturerName;
            this.productName      = productName;
            this.serialNumber     = serialNumber;

            this.classCode        = classCode;
            this.subclassCode     = subclassCode;
            this.protocolCode     = protocolCode;

            this.usbVersion       = usbVersion;
            this.deviceVersion    = deviceVersion;
        }

    } // class USBDevice

    public static ArrayList<USBDevice> getDevices()
    {
        final ArrayList<USBDevice> devices = new ArrayList<>();

        for( final UsbDevice ud : Usb.getDevices() ) {

            devices.add( new USBDevice(
                ud.getVendorId     ()           ,
                ud.getProductId    ()           ,
                ud.getManufacturer ()           ,
                ud.getProduct      ()           ,
                ud.getSerialNumber ()           ,
                ud.getClassCode    ()           ,
                ud.getSubclassCode ()           ,
                ud.getProtocolCode ()           ,
                ud.getUsbVersion   ().toString(),
                ud.getDeviceVersion().toString()
            ) );

        } // for

        return devices;
    }

} // class USBUtil
