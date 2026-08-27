/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Path;

import jxm.xb.*;


// Base class for installing self-signed, catalog-signed Windows drivers (WinUSB / HID / CDC-ACM)
// for USB devices identified by VID/PID. Two backends are provided:
//     - WindowsDriverInstaller_PS1 : Drives powershell.exe / pnputil.exe as external processes.
//                                     Works on Windows 7 and later (see that class for details).
//     - WindowsDriverInstaller_FFM : Calls the relevant native Win32 APIs directly using the
//                                     Java 25+ Foreign Function and Memory (FFM) API. Requires
//                                     Windows 10 or later, and a JAR built with Java 25+.
// Use create() to obtain an instance appropriate for the current JVM/OS - callers should not
// instantiate the backends directly.
public abstract class WindowsDriverInstaller {

    public static final int RETCODE_OK           =  0;
    public static final int RETCODE_EXCEPTION    = -1;
    public static final int RETCODE_INVALID_PATH = -2;
    public static final int RETCODE_PH_NULL      = -3;
    public static final int RETCODE_UAC_DECLINED = -4;
    public static final int RETCODE_TIMEOUT      = -5;

    protected static final String PROVIDER_NAME = "JxMake_WindowsDriverInstaller";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Factory method - always prefer the Java 25+ FFM backend over the PowerShell backend, since
    // it avoids spawning powershell.exe/UAC-elevated child processes for every operation. The FFM
    // class name is looked up by string (never referenced by type) because a JAR built with an
    // older Java version will not contain that class at all - see the 'ExcludeFFM' Makefile logic.
    public static WindowsDriverInstaller create()
    {
        try {

            final Class<?> ffmClass = Class.forName("jxm.WindowsDriverInstaller_FFM");
            final Object   instance = ffmClass.getDeclaredConstructor().newInstance();

            if( (instance instanceof WindowsDriverInstaller wdi) && wdi.isUsable() ) return wdi;

        }
        catch(final Throwable ignored) {
            // Broad catch is intentional : this must also catch UnsupportedClassVersionError and
            // other LinkageErrors (JAR built by a newer javac than this JVM supports), not just
            // ClassNotFoundException (class excluded from an older-Java build of this JAR)
        }

        return new WindowsDriverInstaller_PS1();
    }

    // Returns true if this backend can actually operate on the current JVM/OS combination
    public abstract boolean isUsable();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Checks if a certificate with the specified providerName exists in the Trusted Root and Trusted Publisher stores
    public abstract XCom.Pair<Integer, String> isProviderAlreadyTrusted(final String providerName);

    // Creates a self-signed certificate and installs it into Root and TrustedPublisher stores using system tools
    public abstract XCom.Pair<Integer, String> createAndTrustProvider(final String providerName);

    // Creates a .cat file for the INF and signs it using the self-signed cert
    public abstract XCom.Pair<Integer, String> createAndSignCatalog(final String infPath, final String providerName);

    // Installs a local INF file using PnPUtil, elevated as required
    // NOTE : Use absolute paths for INF files to avoid "File not found" errors in elevated contexts
    public abstract XCom.Pair<Integer, String> installDriver(final String infPath);

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Generates a formatted WinUSB INF string for a specific hardware ID
    public static String generateWinUSBInf(final String vid, final String pid)
    {
        final String hwId = String.format( "USB\\VID_%s&PID_%s", vid.toUpperCase(), pid.toUpperCase() );

        return
            "[Version]                                                                       \r\n" +
            "Signature   = \"$Windows NT$\"                                                  \r\n" +
            "Class       = USBDevice                                                         \r\n" +
            "ClassGUID   = {88BAE032-5A81-49f0-BC3D-A4FF138216D6}                            \r\n" +
            "Provider    = %ManufacturerName%                                                \r\n" +
            "CatalogFile = WinUSB.cat                                                        \r\n" +
            "DriverVer   = 05/02/2026,1.0.0.0                                                \r\n" +
            "                                                                                \r\n" +
            "[Manufacturer]                                                                  \r\n" +
            "%ManufacturerName% = WinUSB_Device, NTamd64                                     \r\n" +
            "                                                                                \r\n" +
            "[WinUSB_Device.NTamd64]                                                         \r\n" +
            "%DeviceName% = WinUSB_Install, " + hwId + "                                     \r\n" +
            "                                                                                \r\n" +
            "[WinUSB_Install]                                                                \r\n" +
            "Include = winusb.inf                                                            \r\n" +
            "Needs   = WINUSB.NT                                                             \r\n" +
            "                                                                                \r\n" +
            "[WinUSB_Install.Services]                                                       \r\n" +
            "Include = winusb.inf                                                            \r\n" +
            "Needs   = WINUSB.NT.Services                                                    \r\n" +
            "                                                                                \r\n" +
            "[WinUSB_Install.HW]                                                             \r\n" +
            "AddReg = Dev_AddReg                                                             \r\n" +
            "                                                                                \r\n" +
            "[Dev_AddReg]                                                                    \r\n" +
            "; DeviceInterfaceGUIDs: Generic WinUSB access                                   \r\n" +
            "HKR,,DeviceInterfaceGUIDs,0x00010000,\"{dee82443-396a-4b21-822e-13c3a2f8b503}\" \r\n" +
            "                                                                                \r\n" +
            "[Strings]                                                                       \r\n" +
            "ManufacturerName = \"Generic USB Device\"                                       \r\n" +
            "DeviceName       = \"WinUSB Automated Driver\"                                  \r\n" ;
    }

    // Generates a formatted HID INF string for a specific hardware ID
    public static String generateHIDInf(final String vid, final String pid)
    {
        final String hwId = String.format( "USB\\VID_%s&PID_%s", vid.toUpperCase(), pid.toUpperCase() );

        return
            "[Version]                                            \r\n" +
            "Signature   = \"$Windows NT$\"                       \r\n" +
            "Class       = HIDClass                               \r\n" +
            "ClassGUID   = {745a17a0-74d3-11d0-b6fe-00a0c90f57da} \r\n" +
            "Provider    = %ManufacturerName%                     \r\n" +
            "CatalogFile = hid_device.cat                         \r\n" +
            "DriverVer   = 05/02/2026,1.0.0.0                     \r\n" +
            "                                                     \r\n" +
            "[Manufacturer]                                       \r\n" +
            "%ManufacturerName% = HID_Device, NTamd64             \r\n" +
            "                                                     \r\n" +
            "[HID_Device.NTamd64]                                 \r\n" +
            "%DeviceName% = HID_Install, " + hwId + "             \r\n" +
            "                                                     \r\n" +
            "[HID_Install.NT]                                     \r\n" +
            "Include = input.inf                                  \r\n" +
            "Needs   = HID_Inst.NT                                \r\n" +
            "                                                     \r\n" +
            "[HID_Install.NT.Services]                            \r\n" +
            "Include = input.inf                                  \r\n" +
            "Needs   = HID_Inst.NT.Services                       \r\n" +
            "                                                     \r\n" +
            "[Strings]                                            \r\n" +
            "ManufacturerName = \"Generic HID Device\"            \r\n" +
            "DeviceName       = \"HID Automated Driver\"          \r\n" ;
    }

    // Generates a formatted CDC-ACM INF string for a specific hardware ID
    public static String generateCDCACMInf(final String vid, final String pid)
    {
        final String hwId = String.format( "USB\\VID_%s&PID_%s", vid.toUpperCase(), pid.toUpperCase() );

        return
            "[Version]                                                        \r\n" +
            "Signature   = \"$Windows NT$\"                                   \r\n" +
            "Class       = Ports                                              \r\n" +
            "ClassGUID   = {4d36e978-e325-11ce-bfc1-08002be10318}             \r\n" +
            "Provider    = %ManufacturerName%                                 \r\n" +
            "CatalogFile = cdc_acm.cat                                        \r\n" +
            "DriverVer   = 05/02/2026,1.0.0.0                                 \r\n" +
            "                                                                 \r\n" +
            "[Manufacturer]                                                   \r\n" +
            "%ManufacturerName% = DeviceList, NTamd64                         \r\n" +
            "                                                                 \r\n" +
            "[DeviceList.NTamd64]                                             \r\n" +
            "%DeviceName% = DriverInstall, " + hwId + "                       \r\n" +
            "                                                                 \r\n" +
            "[DriverInstall.NT]                                               \r\n" +
            "Include = mdmcpq.inf                                             \r\n" +
            "CopyFiles = FakeCopyFiles                                        \r\n" +
            "AddReg = DriverInstall.NT.AddReg                                 \r\n" +
            "                                                                 \r\n" +
            "[DriverInstall.NT.AddReg]                                        \r\n" +
            "HKR,,DevLoader,,*ntkern                                          \r\n" +
            "HKR,,NTMPDriver,,usbser.sys                                      \r\n" +
            "HKR,,EnumPropPages32,,\"MsPorts.dll,SerialPortPropPageProvider\" \r\n" +
            "                                                                 \r\n" +
            "[DriverInstall.NT.Services]                                      \r\n" +
            "AddService = usbser, 0x00000002, DriverService.NT                \r\n" +
            "                                                                 \r\n" +
            "[DriverService.NT]                                               \r\n" +
            "DisplayName = \"USB Serial Driver\"                              \r\n" +
            "ServiceType = 1                                                  \r\n" +
            "StartType = 3                                                    \r\n" +
            "ErrorControl = 1                                                 \r\n" +
            "ServiceBinary = %12%\\usbser.sys                                 \r\n" +
            "LoadOrderGroup = Base                                            \r\n" +
            "                                                                 \r\n" +
            "[Strings]                                                        \r\n" +
            "ManufacturerName = \"Generic Serial Device\"                     \r\n" +
            "DeviceName       = \"CDC-ACM Automated Driver\"                  \r\n" ;
    }

    // Generates a formatted multi-port CDC-ACM INF string for a specific hardware ID
    public static String generateMultiCDCACMInf(final String vid, final String pid, final int numInterfaces)
    {
        final String        vidPid = String.format( "VID_%s&PID_%s", vid.toUpperCase(), pid.toUpperCase() );
        final StringBuilder sb     = new StringBuilder();

        // --- VERSION SECTION ---
        sb.append("[Version]                                                        \r\n");
        sb.append("Signature   = \"$Windows NT$\"                                   \r\n");
        sb.append("Class       = Ports                                              \r\n");
        sb.append("ClassGUID   = {4d36e978-e325-11ce-bfc1-08002be10318}             \r\n");
        sb.append("Provider    = %ManufacturerName%                                 \r\n");
        sb.append("CatalogFile = multi_cdc.cat                                      \r\n");
        sb.append("DriverVer   = 05/02/2026,1.0.0.0                                 \r\n");
        sb.append("                                                                 \r\n");

        // --- MANUFACTURER SECTION ---
        sb.append("[Manufacturer]                                                   \r\n");
        sb.append("%ManufacturerName% = DeviceList, NTamd64                         \r\n");
        sb.append("                                                                 \r\n");

        // --- DEVICE LIST SECTION ---
        sb.append("[DeviceList.NTamd64]                                             \r\n");
        for( int i = 0; i < numInterfaces; ++i) {
            // CDC usually pairs two interfaces (Management + Data) - target the first interface of each pair (MI_00, MI_02, etc.)
            final String hwId = String.format("USB\\%s&MI_%02d", vidPid, i * 2);
            sb.append( String.format("%%DeviceName.%d%% = DriverInstall, %s \r\n", i, hwId) );
        }
        sb.append("                                                                 \r\n");

        // --- INSTALLATION SECTION ---
        sb.append("[DriverInstall.NT]                                               \r\n");
        sb.append("Include = mdmcpq.inf                                             \r\n");
        sb.append("CopyFiles = FakeCopyFiles                                        \r\n");
        sb.append("AddReg = DriverInstall.NT.AddReg                                 \r\n");
        sb.append("                                                                 \r\n");

        sb.append("[DriverInstall.NT.AddReg]                                        \r\n");
        sb.append("HKR,,DevLoader,,*ntkern                                          \r\n");
        sb.append("HKR,,NTMPDriver,,usbser.sys                                      \r\n");
        sb.append("HKR,,EnumPropPages32,,\"MsPorts.dll,SerialPortPropPageProvider\" \r\n");
        sb.append("                                                                 \r\n");

        // --- SERVICE SECTION ---
        sb.append("[DriverInstall.NT.Services]                                      \r\n");
        sb.append("AddService = usbser, 0x00000002, DriverService.NT                \r\n");
        sb.append("                                                                 \r\n");

        sb.append("[DriverService.NT]                                               \r\n");
        sb.append("DisplayName = \"USB Serial Driver\"                              \r\n");
        sb.append("ServiceType = 1                                                  \r\n");
        sb.append("StartType = 3                                                    \r\n");
        sb.append("ErrorControl = 1                                                 \r\n");
        sb.append("ServiceBinary = %12%\\usbser.sys                                 \r\n");
        sb.append("LoadOrderGroup = Base                                            \r\n");
        sb.append("                                                                 \r\n");

        // --- STRINGS SECTION ---
        sb.append("[Strings]                                                        \r\n");
        sb.append("ManufacturerName = \"Generic Multi-Serial Device\"               \r\n");
        for(int i = 0; i < numInterfaces; ++i) {
            sb.append( String.format("DeviceName.%d = \"CDC-ACM Port %d\" \r\n", i, i) );
        }

        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _saveInfToFile(final String vid, final String pid, final String infText)
    {
        try {
            final Path tempPath = Files.createTempFile("drv_" + vid + "_" + pid + "_", ".inf");

            Files.write( tempPath, infText.getBytes(StandardCharsets.UTF_8) );

            return tempPath.toAbsolutePath().toString();

        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            return null;
        }
    }

    private XCom.Pair<Integer, String> _saveInfToFileAndSign(final String vid, final String pid, final String infText)
    {
        final String infPath = _saveInfToFile(vid, pid, infText);
        if(infPath == null) return new XCom.Pair<Integer, String>( RETCODE_INVALID_PATH, String.format(Texts.EMsg_WDriverInstallInvInfPth, "drv_" + vid + "_" + pid) );

        if( isProviderAlreadyTrusted(PROVIDER_NAME).first() == RETCODE_OK ) {
            final XCom.Pair<Integer, String> res = createAndTrustProvider(PROVIDER_NAME);
            if( res.first() != RETCODE_OK ) return res;
        }

        final XCom.Pair<Integer, String> res = createAndSignCatalog(infPath, PROVIDER_NAME);
        if( res.first() != RETCODE_OK ) return res;

        return new XCom.Pair<Integer, String>(RETCODE_OK, infPath);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Make these accessible from SysUtil ??? #####

    public XCom.Pair<Integer, String> installWinUSBInf(final String vid, final String pid)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, generateWinUSBInf(vid, pid) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public XCom.Pair<Integer, String> installHIDInf(final String vid, final String pid)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, generateHIDInf(vid, pid) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public XCom.Pair<Integer, String> installCDCACMInf(final String vid, final String pid)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, generateCDCACMInf(vid, pid) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public XCom.Pair<Integer, String> installMultiCDCACMInf(final String vid, final String pid, int numInterfaces)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, generateMultiCDCACMInf(vid, pid, numInterfaces) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

} // WindowsDriverInstaller
