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


/*
 * Base class for installing self-signed, catalog-signed Windows drivers (WinUSB/HID/ CDC-ACM)
 * for USB devices identified by VID/PID. Two backends are provided:
 *     - WindowsDriverInstaller_PS1 : Drives powershell.exe / pnputil.exe as external processes.
 *                                    Works on Windows 7 and later (see that class for details).
 *     - WindowsDriverInstaller_FFM : Calls the relevant native Win32 APIs directly using the
 *                                    Java 25+ Foreign Function and Memory (FFM) API. Requires
 *                                    Windows 10 or later, and a JAR built with Java 25+.
 *
 * Use create() to obtain an instance appropriate for the current JVM/OS - callers should not
 * instantiate the backends directly.
 */
public abstract class WindowsDriverInstaller {

    public static final int RETCODE_OK           =  0;
    public static final int RETCODE_EXCEPTION    = -1;
    public static final int RETCODE_INVALID_PATH = -2;
    public static final int RETCODE_PH_NULL      = -3;
    public static final int RETCODE_UAC_DECLINED = -4;
    public static final int RETCODE_TIMEOUT      = -5;

    protected static final String PROVIDER_NAME = "JxMake_WindowsDriverInstaller";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * Factory method - always prefer the Java 25+ FFM backend over the PowerShell backend, since
     * it avoids spawning powershell.exe/UAC-elevated child processes for every operation. The FFM
     * class name is looked up by string (never referenced by type) because a JAR built with an
     * older Java version will not contain that class at all - see the 'ExcludeFFM' Makefile logic.
     */
    public static WindowsDriverInstaller create()
    {
        try {

            final Class<?> ffmClass = Class.forName("jxm.WindowsDriverInstaller_FFM");
            final Object   instance = ffmClass.getDeclaredConstructor().newInstance();

            if( (instance instanceof WindowsDriverInstaller) ) {
                final WindowsDriverInstaller wdiInst = (WindowsDriverInstaller) instance;
                if( wdiInst.isUsable() ) return wdiInst;
            }

        }
        catch(final Throwable ignored) {
            // Catch ClassNotFoundException, UnsupportedClassVersionError, and other LinkageErrors
        }

        final WindowsDriverInstaller wdiInst = new WindowsDriverInstaller_PS1();

        return wdiInst.isUsable() ? wdiInst : null;
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

    // Installs a local INF file using PnPUtil, elevated as required. Only stages the driver into the
    // driver store (pnputil's /install flag is never passed - see each backend's implementation for
    // why); an already-connected matching device needs a replug to pick up the newly staged driver.
    // NOTE : Use absolute paths for INF files to avoid "File not found" errors in elevated contexts
    public abstract XCom.Pair<Integer, String> installDriver(final String infPath);

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Generates a formatted WinUSB INF string for a specific hardware ID. catalogFileName must be the exact
    // basename (e.g. "drv_1234_5678_9042380952345.cat") of the .cat file that will sit alongside this INF -
    // see _saveInfToFileAndSign() below, since Windows requires CatalogFile= to name a real file in the same
    // directory as the INF, and rejects the package (signature not found) otherwise
    public static String generateWinUSBInf(final String vid, final String pid, final String catalogFileName)
    {
        final String hwId = String.format( "USB\\VID_%s&PID_%s", vid.toUpperCase(), pid.toUpperCase() );

        return
            "[Version]                                                                       \r\n" +
            "Signature   = \"$Windows NT$\"                                                  \r\n" +
            "Class       = USBDevice                                                         \r\n" +
            "ClassGUID   = {88BAE032-5A81-49f0-BC3D-A4FF138216D6}                            \r\n" +
            "Provider    = %ManufacturerName%                                                \r\n" +
            "CatalogFile = " + catalogFileName + "                                           \r\n" +
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

    // Generates a formatted HID INF string for a specific hardware ID. catalogFileName must be the exact
    // basename of the .cat file that will sit alongside this INF - see generateWinUSBInf() above
    public static String generateHIDInf(final String vid, final String pid, final String catalogFileName)
    {
        final String hwId = String.format( "USB\\VID_%s&PID_%s", vid.toUpperCase(), pid.toUpperCase() );

        return
            "[Version]                                            \r\n" +
            "Signature   = \"$Windows NT$\"                       \r\n" +
            "Class       = HIDClass                               \r\n" +
            "ClassGUID   = {745a17a0-74d3-11d0-b6fe-00a0c90f57da} \r\n" +
            "Provider    = %ManufacturerName%                     \r\n" +
            "CatalogFile = " + catalogFileName + "                \r\n" +
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

    // Generates a formatted CDC-ACM INF string for a specific hardware ID. catalogFileName must be the exact
    // basename of the .cat file that will sit alongside this INF - see generateWinUSBInf() above
    public static String generateCDCACMInf(final String vid, final String pid, final String catalogFileName)
    {
        final String hwId = String.format( "USB\\VID_%s&PID_%s", vid.toUpperCase(), pid.toUpperCase() );

        return
            "[Version]                                                        \r\n" +
            "Signature   = \"$Windows NT$\"                                   \r\n" +
            "Class       = Ports                                              \r\n" +
            "ClassGUID   = {4d36e978-e325-11ce-bfc1-08002be10318}             \r\n" +
            "Provider    = %ManufacturerName%                                 \r\n" +
            "CatalogFile = " + catalogFileName + "                            \r\n" +
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

    // Generates a formatted multi-port CDC-ACM INF string for a specific hardware ID. catalogFileName must be
    // the exact basename of the .cat file that will sit alongside this INF - see generateWinUSBInf() above
    public static String generateMultiCDCACMInf(final String vid, final String pid, final int numInterfaces, final String catalogFileName)
    {
        final String        vidPid = String.format( "VID_%s&PID_%s", vid.toUpperCase(), pid.toUpperCase() );
        final StringBuilder sb     = new StringBuilder();

        // --- VERSION SECTION ---
        sb.append("[Version]                                                        \r\n");
        sb.append("Signature   = \"$Windows NT$\"                                   \r\n");
        sb.append("Class       = Ports                                              \r\n");
        sb.append("ClassGUID   = {4d36e978-e325-11ce-bfc1-08002be10318}             \r\n");
        sb.append("Provider    = %ManufacturerName%                                 \r\n");
        sb.append("CatalogFile = ").append(catalogFileName).append("                \r\n");
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

    // Reserves a uniquely-named temp .inf file and writes infTextForCatalog's result into it. The .inf's
    // basename (not chosen until Files.createTempFile returns) is what createAndSignCatalog() below will
    // derive the .cat file's name from, so infTextForCatalog is only called once that basename is known -
    // it must embed the given catalogFileName verbatim as this INF's CatalogFile= value (see
    // generateWinUSBInf() and friends), or Windows will reject the package as unsigned/mismatched
    private static String _saveInfToFile(final String vid, final String pid, final java.util.function.Function<String, String> infTextForCatalog)
    {
        try {
            final Path   tempPath        = Files.createTempFile("drv_" + vid + "_" + pid + "_", ".inf");
            final String catalogFileName = tempPath.getFileName().toString().replaceFirst("\\.inf$", ".cat");

            Files.write( tempPath, infTextForCatalog.apply(catalogFileName).getBytes(StandardCharsets.UTF_8) );

            return tempPath.toAbsolutePath().toString();

        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            return null;
        }
    }

    private XCom.Pair<Integer, String> _saveInfToFileAndSign(final String vid, final String pid, final java.util.function.Function<String, String> infTextForCatalog)
    {
        final String infPath = _saveInfToFile(vid, pid, infTextForCatalog);
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
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, catalogFileName -> generateWinUSBInf(vid, pid, catalogFileName) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public XCom.Pair<Integer, String> installHIDInf(final String vid, final String pid)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, catalogFileName -> generateHIDInf(vid, pid, catalogFileName) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public XCom.Pair<Integer, String> installCDCACMInf(final String vid, final String pid)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, catalogFileName -> generateCDCACMInf(vid, pid, catalogFileName) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public XCom.Pair<Integer, String> installMultiCDCACMInf(final String vid, final String pid, int numInterfaces)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, catalogFileName -> generateMultiCDCACMInf(vid, pid, numInterfaces, catalogFileName) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

} // WindowsDriverInstaller

/*
 * make jar JDK_VER=8 && make jar JDK_VER=25
 *
 * Java 8  : /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.161-0.b14.el7_4.x86_64/bin
 * Java 25 : /opt/openjdk-25_linux-x64_bin/jdk-25/bin
 */

/*
 * ===== CONTINUE HERE NEXT SESSION (left 2026-08-30, CI run [second dispatch after 90196100036]) =====
 *
 * Status: the FFM catalog-signing bug (the original point of this whole investigation) is FIXED
 * and CONFIRMED working end-to-end on CI - see "SECTION G - CI Investigation Log" at the end of
 * WindowsDriverInstaller_FFM-Win32API.txt for full detail. Two new, separate problems surfaced
 * right after that fix landed:
 *
 * 1. PARTIALLY FIXED, hypothesis REVISED after a second CI run. Dropping /install (commit
 *    ac29c9f) fixed PS1.installDriver - it now returns immediately instead of hanging. But
 *    FFM.installDriver STILL HUNG on the same run (thread dump again stuck in
 *    _shellExecuteElevatedAndWait, WindowsDriverInstaller_FFM.java ~line 472, before being
 *    manually canceled at ~89s) - so "/install causes the hang" was WRONG for FFM, or at least
 *    incomplete. Crucially, FFM's OWN createAndTrustProvider/createAndSignCatalog calls use the
 *    exact same ShellExecuteExW+WaitForSingleObject mechanism (via _runElevatedSelf) and completed
 *    fine in this same run - so the native elevation plumbing itself isn't broken; whatever hangs
 *    is specific to elevating "cmd.exe /c pnputil.exe /add-driver ...".
 *
 *    REVISED hypothesis: PS1's driver failed pnputil's signature check outright ("does not contain
 *    digital signature information" - no valid signature at all, rejected instantly, no dialog
 *    possible). FFM's driver, by contrast, had a validly self-signed (but non-WHQL / not
 *    Microsoft-verified) catalog - pnputil may raise a "Windows can't verify the publisher of this
 *    driver software" consent dialog specifically when it reaches a real-but-untrusted signature
 *    check, independent of /install. That would hang forever headless. NOT YET PROVEN - PS1 has
 *    never gotten a validly-signed driver far enough to test the same code path, since it's still
 *    blocked by item 2 below.
 *
 *    FIX ATTEMPTED (commit abbc84a): per explicit user decision, added the legacy
 *    `HKLM\SOFTWARE\Microsoft\Driver Signing\Policy` (REG_BINARY, first byte 0 = silently succeed)
 *    registry tweak to test-windows-driver-installer.yml's "Allow silent admin elevation" step,
 *    alongside the existing ConsentPromptBehaviorAdmin=0 tweak - CI-workflow-only, not something
 *    this library sets on a real user's machine. CONFIRMED INEFFECTIVE by a second live CI run:
 *    FFM.installDriver hung again at the identical spot (~89s before manual cancellation). Per the
 *    user's explicit fallback plan, pivoted to item 2 next instead of further guessing here - this
 *    registry tweak is left in place (harmless either way) but item 1 remains genuinely OPEN.
 *
 * 2. FIX ATTEMPT 1 FAILED, FIX ATTEMPT 2 MADE (commit 5f03680). PS1.createAndSignCatalog was
 *    failing with "Certificate not found" - createAndTrustProvider's elevated child creates+trusts
 *    the cert successfully, but createAndSignCatalog's own, separately-elevated child couldn't find
 *    it via Cert:\CurrentUser\My moments later ("Cert:\CurrentUser\My contains: []" - genuinely
 *    empty, confirmed by the diagnostic added in commit 2bc4b01).
 *
 *    Attempt 1 (commit 0eb40d4): user pointed out that an earlier iteration of this backend, before
 *    `-DeleteKey` was added to Remove-Item calls (commit 2a2cd6a), did not exhibit this symptom.
 *    Dropped -DeleteKey from both call sites. CONFIRMED INEFFECTIVE by a live CI run: with
 *    -DeleteKey gone, Cert:\CurrentUser\My was still completely empty from createAndSignCatalog's
 *    point of view ("It does not make sense" - user's words) - -DeleteKey was never the cause.
 *
 *    That same failing run's OTHER diagnostic output gave the real lead:
 *    PS1.isProviderAlreadyTrusted (after trust) = [1] - i.e. Cert:\LocalMachine\Root/TrustedPublisher
 *    (machine-wide, not per-profile) DOES survive across the two independently-elevated
 *    "Start-Process -Verb RunAs" sessions, while Cert:\CurrentUser\My (per-profile) does not. Why
 *    that split happens on this runner is still not understood, but the pattern is now solid across
 *    two consecutive fix-attempt/CI-failure cycles.
 *
 *    Attempt 2 (commit 5f03680): stopped relying on Cert:\CurrentUser\My cross-session visibility
 *    entirely. createAndTrustProvider() now also exports the cert+private key as a
 *    password-protected PFX to a %TEMP% file (password derived deterministically from providerName
 *    via SHA-256, since the two methods share no state but that parameter); createAndSignCatalog()
 *    loads the cert straight from that PFX via `[X509Certificate2]::new(path, password)` instead of
 *    querying any store, and deletes the PFX afterward.
 *
 *    CONFIRMED FIXED (live CI, 2026-08-30): createAndTrustProvider -> createAndSignCatalog (cert
 *    loaded from PFX, catalog validly signed) -> installDriver all succeeded end-to-end
 *    ("Driver package added successfully"). Item 2 is DONE.
 *
 *    Bonus data point for item 1: PS1.installDriver, now handling a genuinely, validly signed
 *    (non-WHQL) driver for the first time, did NOT hang - returned immediately. This weakens the
 *    "unverified publisher consent dialog" theory for FFM's hang (item 1), since PS1 exercises the
 *    same pnputil /add-driver + non-WHQL-signed-catalog combination without hanging. FFM's hang is
 *    now more likely specific to FFM's own ShellExecuteExW/_shellExecuteElevatedAndWait plumbing.
 *    Item 1 remains OPEN and not yet further investigated.
 *
 *    FOLLOW-UP (commit cbe4ab4): since createAndSignCatalog() no longer reads Cert:\CurrentUser\My
 *    at all, the leftover cert+key createAndTrustProvider() writes there served no further purpose -
 *    restored -DeleteKey, but only as a SAME-SESSION delete (destroy the CurrentUser\My cert+key in
 *    createAndTrustProvider's own elevated session immediately after the PFX export, plus its
 *    stale-cleanup step at the method's top) - never a cross-session read, which was the actually-
 *    broken path attempt 1 disproved. Addresses two concerns raised by the user: certs no longer
 *    accumulate in the store across runs, and the private key can't be reused after one use. Believed
 *    safe without further CI verification since it doesn't touch the broken cross-session mechanism,
 *    but not yet explicitly confirmed by a run against this exact commit.
 *
 * 3. DONE (2026-08-30, commit 927b5ed): right-aligned every `\r\n" +` line terminator within each
 *    multi-line String.format(...)/concat block in WindowsDriverInstaller_PS1.java. Continuation
 *    backtick lines (New-SelfSignedCertificate, both Start-Process/-ArgumentList pairs, the
 *    Set-AuthenticodeSignature pipe line) were left untouched, exactly as instructed. Confirmed
 *    `grep -n '`[ \t]\+\\r\\n"' jxm/WindowsDriverInstaller_PS1.java` returns nothing, and both
 *    `make jar JDK_VER=8` and `make jar JDK_VER=25` compile clean.
 *
 * Once 1 and 2 are actually fixed and CI-reverified, update memory, and dispatch another
 * `backend=both, run_mutating=true` CI run to confirm everything end-to-end.
 *
 * -----
 *
 * After that works check if it is possible to replace signtool.exe with https://github.com/mtrojnar/osslsigncode.
 * Either make the JAR download and save/ache it somewhere using `tool/HTTPDownloader.java` and unzip using java
 * or using PowerShell or save it to `../3rd_party/app/osslsigncode` with the required license files, etc.
 * and make `make dist` copy the files to `../dist_build/jxmake_dist/apps/osslsigncode` - then WindowsDriverInstaller_FFM.java
 * will need to find the EXE in `apps/osslsigncode` (distribution mode) or `../3rd_party/app/osslsigncode` (testing mode).
 *
 * It may also a good idea to add a note somewhere so the user know how to get and extract signtool.exe if osslsigncode
 * does not work.
 */
