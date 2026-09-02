/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jxm.xb.*;


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

        // Short one-line label for UI pickers, e.g. "0483:DF11 STMicroelectronics STM32 BOOTLOADER"
        public String label()
        { return String.format("%04X:%04X %s %s", vid, pid, manufacturerName, productName); }

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

    // NOTE : Use reflection to ensure this class can be compiled both with older and newer Java!

    private static boolean _reflectionInitialized = false;
    private static boolean _reflectionAvailable   = false;

    private static Method _usb_getDevices;

    private static Method _usbDevice_getVendorId;
    private static Method _usbDevice_getProductId;
    private static Method _usbDevice_getManufacturer;
    private static Method _usbDevice_getProduct;
    private static Method _usbDevice_getSerialNumber;
    private static Method _usbDevice_getClassCode;
    private static Method _usbDevice_getSubclassCode;
    private static Method _usbDevice_getProtocolCode;
    private static Method _usbDevice_getUsbVersion;
    private static Method _usbDevice_getDeviceVersion;
    private static Method _usbDevice_getInterfaces;
    private static Method _usbDevice_getInterface;

    private static Method _usbInterface_getAlternates;
    private static Method _usbInterface_getAlternate;

    private static Method _usbAlternateInterface_getEndpoints;

    private static synchronized boolean _ensureReflectionInitialized()
    {
        if(_reflectionInitialized) return _reflectionAvailable;

        _reflectionInitialized = true;
        _reflectionAvailable   = false;

        try {

            final Class<?> usb_class                   = Class.forName("net.codecrete.usb.Usb"                   );
            final Class<?> usbDevice_class             = Class.forName("net.codecrete.usb.UsbDevice"             );
            final Class<?> usbInterface_class          = Class.forName("net.codecrete.usb.UsbInterface"          );
            final Class<?> usbAlternateInterface_class = Class.forName("net.codecrete.usb.UsbAlternateInterface" );

            _usb_getDevices                     = usb_class                  .getMethod("getDevices");

            _usbDevice_getVendorId              = usbDevice_class            .getMethod("getVendorId"                );
            _usbDevice_getProductId             = usbDevice_class            .getMethod("getProductId"               );
            _usbDevice_getManufacturer          = usbDevice_class            .getMethod("getManufacturer"            );
            _usbDevice_getProduct               = usbDevice_class            .getMethod("getProduct"                 );
            _usbDevice_getSerialNumber          = usbDevice_class            .getMethod("getSerialNumber"            );
            _usbDevice_getClassCode             = usbDevice_class            .getMethod("getClassCode"               );
            _usbDevice_getSubclassCode          = usbDevice_class            .getMethod("getSubclassCode"            );
            _usbDevice_getProtocolCode          = usbDevice_class            .getMethod("getProtocolCode"            );
            _usbDevice_getUsbVersion            = usbDevice_class            .getMethod("getUsbVersion"              );
            _usbDevice_getDeviceVersion         = usbDevice_class            .getMethod("getDeviceVersion"           );
            _usbDevice_getInterfaces            = usbDevice_class            .getMethod("getInterfaces"              );
            _usbDevice_getInterface             = usbDevice_class            .getMethod("getInterface"    , int.class);

            _usbInterface_getAlternates         = usbInterface_class         .getMethod("getAlternates"              );
            _usbInterface_getAlternate          = usbInterface_class         .getMethod("getAlternate"    , int.class);

            _usbAlternateInterface_getEndpoints = usbAlternateInterface_class.getMethod("getEndpoints"               );

            _reflectionAvailable = true;

        }
        catch(final Throwable t) {
            // Library not present, or its class files cannot be loaded/parsed by this JVM - USB
            // enumeration is disabled, getDevices() will simply report no devices
            _reflectionAvailable = false;
            if( XCom.enableAllExceptionStackTrace() ) t.printStackTrace();
        }

        return _reflectionAvailable;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<USBDevice> getDevices()
    {
        final ArrayList<USBDevice> devices = new ArrayList<>();

        if( !_ensureReflectionInitialized() ) return devices;

        try {

            final Collection<?> uDevs = (Collection<?>) _usb_getDevices.invoke(null);

            for(final Object uDev : uDevs) {

                final List<?> uInterfaces      = (List<?>) _usbDevice_getInterfaces.invoke(uDev);
                final int     numOfInterfaces  = uInterfaces.size();
                final int[]   numOfAlternates  = new int[numOfInterfaces];
                final int[][] numOfEndpoints   = new int[numOfInterfaces][];

                for(int i = 0; i < numOfInterfaces; ++i) {

                    final Object  uInterface = _usbDevice_getInterface.invoke(uDev, i);
                    final List<?> uAlternates = (List<?>) _usbInterface_getAlternates.invoke(uInterface);

                    numOfAlternates[i] = uAlternates.size();
                    numOfEndpoints [i] = new int[ numOfAlternates[i] ];

                    for(int j = 0; j < numOfAlternates[i]; ++j) {

                        final Object  uAlternate = _usbInterface_getAlternate.invoke(uInterface, j);
                        final List<?> uEndpoints = (List<?>) _usbAlternateInterface_getEndpoints.invoke(uAlternate);
                        numOfEndpoints[i][j] = uEndpoints.size();

                    } // for

                } // for

                devices.add( new USBDevice(
                    (Integer)       _usbDevice_getVendorId     .invoke(uDev),
                    (Integer)       _usbDevice_getProductId    .invoke(uDev),
                    (String )       _usbDevice_getManufacturer .invoke(uDev),
                    (String )       _usbDevice_getProduct      .invoke(uDev),
                    (String )       _usbDevice_getSerialNumber .invoke(uDev),
                    (Integer)       _usbDevice_getClassCode    .invoke(uDev),
                    (Integer)       _usbDevice_getSubclassCode .invoke(uDev),
                    (Integer)       _usbDevice_getProtocolCode .invoke(uDev),
                    String.valueOf( _usbDevice_getUsbVersion   .invoke(uDev) ),
                    String.valueOf( _usbDevice_getDeviceVersion.invoke(uDev) ),
                    numOfInterfaces,
                    numOfAlternates,
                    numOfEndpoints
                ) );

            } // for

        }
        catch(final Throwable t) {
            // Any reflective failure at call time (e.g. an API shape change) also degrades to "no devices"
            // rather than crashing the caller
            devices.clear();
            if( XCom.enableAllExceptionStackTrace() ) t.printStackTrace();
        }

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
