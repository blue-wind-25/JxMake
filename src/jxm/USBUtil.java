/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;
import java.util.List;

import net.codecrete.usb.*;

import jxm.xb.*;


public class USBUtil  {

    public static class USBDevice {

        final int             vid;
        final int             pid;

        final String          manufacturerName;
        final String          productName;
        final String          serialNumber;

        final int             classCode;
        final int             subclassCode;
        final int             protocolCode;

        final String          usbVersion;
        final String          deviceVersion;

        final int              numOfInterfaces;
        final int[]            numOfAlternates;
        final ArrayList<int[]> numOfEndpoints;

        public USBDevice(
            final int              vid             ,
            final int              pid             ,

            final String           manufacturerName,
            final String           productName     ,
            final String           serialNumber    ,

            final int              classCode       ,
            final int              subclassCode    ,
            final int              protocolCode    ,

            final String           usbVersion      ,
            final String           deviceVersion   ,

            final int              numOfInterfaces ,
            final int[]            numOfAlternates ,
            final ArrayList<int[]> numOfEndpoints
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

            this.numOfInterfaces  = numOfInterfaces;
            this.numOfAlternates  = numOfAlternates;
            this.numOfEndpoints   = numOfEndpoints;
        }

    } // class USBDevice

    public static ArrayList<USBDevice> getDevices()
    {
        final ArrayList<USBDevice> devices = new ArrayList<>();

        for( final UsbDevice uDev : Usb.getDevices() ) {

            final int              numOfInterfaces = uDev.getInterfaces().size();
            final int[]            numOfAlternates = new int[numOfInterfaces];
            final ArrayList<int[]> numOfEndpoints  = new ArrayList<>();

            for(int i = 0; i < numOfInterfaces; ++i) {

                numOfAlternates[i] = uDev.getInterface(i).getAlternates().size();

                final int[] _numOfEndpoints =  new int[ numOfAlternates[i] ];
                for(int j = 0; j < numOfAlternates[i]; ++j) {
                    _numOfEndpoints[j] =  uDev.getInterface(i).getAlternate(j).getEndpoints().size();
                }
                numOfEndpoints.add(_numOfEndpoints);

            } // for

            devices.add( new USBDevice(
                uDev.getVendorId     ()           ,
                uDev.getProductId    ()           ,
                uDev.getManufacturer ()           ,
                uDev.getProduct      ()           ,
                uDev.getSerialNumber ()           ,
                uDev.getClassCode    ()           ,
                uDev.getSubclassCode ()           ,
                uDev.getProtocolCode ()           ,
                uDev.getUsbVersion   ().toString(),
                uDev.getDeviceVersion().toString(),
                numOfInterfaces                   ,
                numOfAlternates                   ,
                numOfEndpoints
            ) );

        } // for

        return devices;
    }

} // class USBUtil
