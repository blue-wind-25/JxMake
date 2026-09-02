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

    /*
     * "java-does-usb" (net.codecrete.usb) is compiled for a class file version newer than what older JDKs'
     * javac can parse (it relies on the modern Java Foreign Function & Memory API). Since this source tree is
     * also compiled with older JDKs (see JxMakeFile's `make jar JDK_VER=8`), it cannot be referenced directly
     * via import/compile-time types here - doing so would break the older-JDK compile. All access to it is
     * therefore done purely via reflection, resolved lazily and cached; if the library's classes cannot be
     * loaded (missing jar, or a JDK too old to even parse its class files at runtime), USB enumeration is
     * silently disabled and getDevices() returns an empty list.
     */

    private static boolean reflectionInitialized;
    private static boolean reflectionAvailable;

    private static Method usb_getDevices;

    private static Method usbDevice_getVendorId;
    private static Method usbDevice_getProductId;
    private static Method usbDevice_getManufacturer;
    private static Method usbDevice_getProduct;
    private static Method usbDevice_getSerialNumber;
    private static Method usbDevice_getClassCode;
    private static Method usbDevice_getSubclassCode;
    private static Method usbDevice_getProtocolCode;
    private static Method usbDevice_getUsbVersion;
    private static Method usbDevice_getDeviceVersion;
    private static Method usbDevice_getInterfaces;
    private static Method usbDevice_getInterface;

    private static Method usbInterface_getAlternates;
    private static Method usbInterface_getAlternate;

    private static Method usbAlternateInterface_getEndpoints;

    private static synchronized boolean ensureReflectionInitialized()
    {
        if(reflectionInitialized) return reflectionAvailable;

        reflectionInitialized = true;
        reflectionAvailable   = false;

        try {

            final Class<?> usb_class                    = Class.forName("net.codecrete.usb.Usb"                   );
            final Class<?> usbDevice_class               = Class.forName("net.codecrete.usb.UsbDevice"             );
            final Class<?> usbInterface_class            = Class.forName("net.codecrete.usb.UsbInterface"          );
            final Class<?> usbAlternateInterface_class   = Class.forName("net.codecrete.usb.UsbAlternateInterface" );

            usb_getDevices                    = usb_class.getMethod("getDevices");

            usbDevice_getVendorId             = usbDevice_class.getMethod("getVendorId"     );
            usbDevice_getProductId            = usbDevice_class.getMethod("getProductId"    );
            usbDevice_getManufacturer         = usbDevice_class.getMethod("getManufacturer" );
            usbDevice_getProduct              = usbDevice_class.getMethod("getProduct"      );
            usbDevice_getSerialNumber         = usbDevice_class.getMethod("getSerialNumber" );
            usbDevice_getClassCode            = usbDevice_class.getMethod("getClassCode"    );
            usbDevice_getSubclassCode         = usbDevice_class.getMethod("getSubclassCode" );
            usbDevice_getProtocolCode         = usbDevice_class.getMethod("getProtocolCode" );
            usbDevice_getUsbVersion           = usbDevice_class.getMethod("getUsbVersion"   );
            usbDevice_getDeviceVersion        = usbDevice_class.getMethod("getDeviceVersion");
            usbDevice_getInterfaces           = usbDevice_class.getMethod("getInterfaces"   );
            usbDevice_getInterface            = usbDevice_class.getMethod("getInterface", int.class);

            usbInterface_getAlternates        = usbInterface_class.getMethod("getAlternates"       );
            usbInterface_getAlternate         = usbInterface_class.getMethod("getAlternate", int.class);

            usbAlternateInterface_getEndpoints = usbAlternateInterface_class.getMethod("getEndpoints");

            reflectionAvailable = true;

        }
        catch(final Throwable t) {
            // Library not present, or its class files cannot be loaded/parsed by this JVM - USB
            // enumeration is disabled, getDevices() will simply report no devices.
            reflectionAvailable = false;
        }

        return reflectionAvailable;
    }

    public static ArrayList<USBDevice> getDevices()
    {
        final ArrayList<USBDevice> devices = new ArrayList<>();

        if( !ensureReflectionInitialized() ) return devices;

        try {

            final Collection<?> uDevs = (Collection<?>) usb_getDevices.invoke(null);

            for(final Object uDev : uDevs) {

                final List<?> uInterfaces      = (List<?>) usbDevice_getInterfaces.invoke(uDev);
                final int     numOfInterfaces  = uInterfaces.size();
                final int[]   numOfAlternates  = new int[numOfInterfaces];
                final int[][] numOfEndpoints   = new int[numOfInterfaces][];

                for(int i = 0; i < numOfInterfaces; ++i) {

                    final Object  uInterface = usbDevice_getInterface.invoke(uDev, i);
                    final List<?> uAlternates = (List<?>) usbInterface_getAlternates.invoke(uInterface);

                    numOfAlternates[i] = uAlternates.size();

                    numOfEndpoints[i] = new int[ numOfAlternates[i] ];
                    for(int j = 0; j < numOfAlternates[i]; ++j) {
                        final Object  uAlternate = usbInterface_getAlternate.invoke(uInterface, j);
                        final List<?> uEndpoints = (List<?>) usbAlternateInterface_getEndpoints.invoke(uAlternate);
                        numOfEndpoints[i][j] = uEndpoints.size();
                    }

                } // for

                devices.add( new USBDevice(
                    (Integer) usbDevice_getVendorId     .invoke(uDev),
                    (Integer) usbDevice_getProductId    .invoke(uDev),
                    (String ) usbDevice_getManufacturer .invoke(uDev),
                    (String ) usbDevice_getProduct      .invoke(uDev),
                    (String ) usbDevice_getSerialNumber .invoke(uDev),
                    (Integer) usbDevice_getClassCode    .invoke(uDev),
                    (Integer) usbDevice_getSubclassCode .invoke(uDev),
                    (Integer) usbDevice_getProtocolCode .invoke(uDev),
                    String.valueOf( usbDevice_getUsbVersion   .invoke(uDev) ),
                    String.valueOf( usbDevice_getDeviceVersion.invoke(uDev) ),
                    numOfInterfaces,
                    numOfAlternates,
                    numOfEndpoints
                ) );

            } // for

        }
        catch(final Throwable t) {
            // Any reflective failure at call time (e.g. an API shape change) also degrades to "no devices"
            // rather than crashing the caller.
            devices.clear();
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
