/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;
import java.util.List;

import net.codecrete.usb.*;


public class USBUtil  {

    public static class USBDevice {

        final int     vid;
        final int     pid;

        final String  manufacturerName;
        final String  productName;
        final String  serialNumber;

        final int     classCode;
        final int     subclassCode;
        final int     protocolCode;

        final String  usbVersion;
        final String  deviceVersion;

        final int     numOfInterfaces;
        final int[]   numOfAlternates;
        final int[][] numOfEndpoints;

        public USBDevice(
            final int     vid             ,
            final int     pid             ,

            final String  manufacturerName,
            final String  productName     ,
            final String  serialNumber    ,

            final int     classCode       ,
            final int     subclassCode    ,
            final int     protocolCode    ,

            final String  usbVersion      ,
            final String  deviceVersion   ,

            final int     numOfInterfaces ,
            final int[]   numOfAlternates ,
            final int[][] numOfEndpoints
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

        public String dump(final int devIdx)
        {
            final StringBuilder sb = new StringBuilder();

            sb.append( String.format("==== Device #%d (%04X:%04X) ====%n", devIdx, vid, pid) );

            sb.append( String.format("Manufacturer name    = %s%n"  , manufacturerName) );
            sb.append( String.format("Product      name    = %s%n"  , productName     ) );
            sb.append( String.format("Serial       number  = %s%n"  , serialNumber    ) );
            sb.append( String.format("Class        code    = %02X%n", classCode       ) );
            sb.append( String.format("Subclass     code    = %02X%n", subclassCode    ) );
            sb.append( String.format("Protocol     code    = %02X%n", protocolCode    ) );
            sb.append( String.format("USB          version = %s%n"  , usbVersion      ) );
            sb.append( String.format("Device       version = %s%n"  , deviceVersion   ) );
            sb.append( String.format("Number of interfaces = %d%n"  , numOfInterfaces ) );

            sb.append("Endpoints:\n");
            for(int i = 0; i < numOfInterfaces; ++i) {
                sb.append( String.format("  Interface #%d%n", i) );
                for(int j = 0; j < numOfAlternates[i]; ++j) {
                    sb.append( String.format("    Alternate #%d = %d endpoint(s)%n", j, numOfEndpoints[i][j] ) );
                }
            }

            return sb.toString();
        }

    } // class USBDevice

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<USBDevice> getDevices()
    {
        final ArrayList<USBDevice> devices = new ArrayList<>();

        for( final UsbDevice uDev : Usb.getDevices() ) {

            final int     numOfInterfaces = uDev.getInterfaces().size();
            final int[]   numOfAlternates = new int[numOfInterfaces];
            final int[][] numOfEndpoints  = new int[numOfInterfaces][];

            for(int i = 0; i < numOfInterfaces; ++i) {

                numOfAlternates[i] = uDev.getInterface(i).getAlternates().size();

                numOfEndpoints[i] = new int[ numOfAlternates[i] ];
                for(int j = 0; j < numOfAlternates[i]; ++j) {
                    numOfEndpoints[i][j] =  uDev.getInterface(i).getAlternate(j).getEndpoints().size();
                }

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

    public static void dumpDevices(final ArrayList<USBDevice> devices)
    {
        if(devices == null) return;

        int devIdx = 0;
        for(final USBDevice ud : devices) {
            SysUtil.stdDbg().println( ud.dump(devIdx++) );
        }
    }

} // class USBUtil
