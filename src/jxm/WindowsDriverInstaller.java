/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Base64;
import java.util.HashMap;

import java.util.concurrent.TimeUnit;

import jxm.*;
import jxm.xb.*;


/*
 * WINDOWS 7 / 8 / 8.1 COMPATIBILITY NOTE
 *
 * This class requires PowerShell 5.0 or later for the following features:
 *     - New-SelfSignedCertificate  (requires PowerShell 4.0+, available on Win 8.1+)
 *     - New-FileCatalog            (requires PowerShell 5.0+                       )
 *     - Set-AuthenticodeSignature  (requires PowerShell 5.0+                       )
 *
 * PowerShell 5.1 is NOT pre-installed on Windows 7, 8, or 8.1.
 * Users on these systems must manually install Windows Management Framework (WMF) 5.1:
 *     https://www.microsoft.com/en-us/download/details.aspx?id=54616
 *
 * Additionally, on Windows 7 the pnputil /install flag is not supported. The driver
 * will be staged into the driver store but will NOT be automatically installed onto
 * already-connected devices - the user must replug the device.
 */
public class WindowsDriverInstaller {

    public static final int RETCODE_OK           =  0;
    public static final int RETCODE_EXCEPTION    = -1;
    public static final int RETCODE_INVALID_PATH = -2;
    public static final int RETCODE_PH_NULL      = -3;
    public static final int RETCODE_UAC_DECLINED = -4;
    public static final int RETCODE_TIMEOUT      = -5;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _getEncodedCommand(final String script)
    {
        final byte[] utf16Bytes = script.getBytes(StandardCharsets.UTF_16LE);

        return Base64.getEncoder().encodeToString(utf16Bytes);
    }

    // Runs an encoded PowerShell command and captures its output and exit code - returns a pair of [exitCode, outputLog]
    private static XCom.Pair<Integer, String> _runCommand(final String psCommand, final HashMap<String, String> extraEnv, final int waitTimeMinutes) throws IOException, InterruptedException
    {
        final ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe"            ,
            "-NoProfile"                ,
            "-ExecutionPolicy", "Bypass",
            "-EncodedCommand" , _getEncodedCommand(psCommand)
        );

        pb.redirectErrorStream(true);
        if(extraEnv != null) pb.environment().putAll(extraEnv);

        final Process               proc = pb.start();
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();

        try(
            final InputStream is = proc.getInputStream()
        ) {
            final byte[] data = new byte[4096];
                  int    len;
            while( ( len = is.read(data, 0, data.length) ) != -1 ) buff.write(data, 0, len);
        }

        if( !proc.waitFor(waitTimeMinutes, TimeUnit.MINUTES) ) {
            proc.destroyForcibly(); // Kill the PowerShell wrapper
            return new XCom.Pair<Integer, String>( RETCODE_TIMEOUT, String.format(Texts.EMsg_WDriverInstallTimeoutMN, waitTimeMinutes) );
        }

        final int    exitCode = proc.exitValue();
        final String log      = new String( buff.toByteArray(), StandardCharsets.UTF_8 ).trim();

        return new XCom.Pair<Integer, String>(exitCode, log);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Checks if a certificate with the specified providerName exists in the  Trusted Root and Trusted Publisher stores
    public static XCom.Pair<Integer, String> isProviderAlreadyTrusted(final String providerName)
    {
        try {

            final String psCommand = String.format(
                "$paths = 'Cert:\\LocalMachine\\Root','Cert:\\LocalMachine\\TrustedPublisher'      \r\n" +
                "$found = Get-ChildItem -Path $paths | Where-Object { $_.Subject -like '*CN=%s*' } \r\n" +
                "if($found) { exit 1 } else { exit 0 }                                             \r\n",
                providerName
            );

            return _runCommand(psCommand, null, 1);

        }
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return error
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, e.toString() );
        }
    }

    // Creates a self-signed certificate and installs it into Root and TrustedPublisher stores using system tools
    public static XCom.Pair<Integer, String> createAndTrustProvider(final String providerName)
    {
        final Path certFile = Paths.get( System.getProperty("java.io.tmpdir"), providerName + ".cer" );

        try {

            // Generate Self-Signed Cert via PowerShell, then install to Root and TrustedPublisher
            // via a UAC-elevated child process spawned with Start-Process -Verb RunAs
            final String psCommand = String.format(
                "$tmpOutLog = \"$env:TEMP\\cert_trust_%s_$PID.log\"                                          \r\n" +
                "$exitCode  = 0                                                                              \r\n" +
                "try {                                                                                       \r\n" +
                "    $script = \"                                                                            \r\n" +
                "        $cert = New-SelfSignedCertificate -Subject 'CN=%s' -Type CodeSigningCert            \r\n" +
                "                    -CertStoreLocation 'Cert:\\CurrentUser\\My';                            \r\n" +
                "        Export-Certificate -Cert $cert -FilePath '%s';                                      \r\n" +
                "        certutil.exe -addstore -f Root '%s' | Out-File `\"$tmpOutLog`\" -Append;            \r\n" +
                "        certutil.exe -addstore -f TrustedPublisher '%s' | Out-File `\"$tmpOutLog`\" -Append \r\n" +
                "    \"                                                                                      \r\n" +
                "    $processHandler = Start-Process -FilePath 'powershell.exe'                              \r\n" +
                "                          -ArgumentList \"-NoProfile -Command $script\"                     \r\n" +
                "                          -Verb RunAs -Wait -PassThru                                       \r\n" +
                "    if($processHandler) {                                                                   \r\n" +
                "        $exitCode = $processHandler.ExitCode                                                \r\n" +
                "    }                                                                                       \r\n" +
                "    else {                                                                                  \r\n" +
                "        $exitCode = %d                                                                      \r\n" +
                "    }                                                                                       \r\n" +
                "}                                                                                           \r\n" +
                "catch {                                                                                     \r\n" +
                "    $exitCode = %d                                                                          \r\n" +
                "}                                                                                           \r\n" +
                "Start-Sleep -Milliseconds 100                                                               \r\n" +
                "if(Test-Path $tmpOutLog) {                                                                  \r\n" +
                "    Get-Content $tmpOutLog -Raw   -ErrorAction SilentlyContinue                             \r\n" +
                "    Remove-Item $tmpOutLog -Force -ErrorAction SilentlyContinue                             \r\n" +
                "}                                                                                           \r\n" +
                "exit $exitCode                                                                              \r\n" ,
                providerName, providerName,
                certFile.toAbsolutePath(), certFile.toAbsolutePath(), certFile.toAbsolutePath(),
                RETCODE_PH_NULL, RETCODE_UAC_DECLINED
            );

            final XCom.Pair<Integer, String> result = _runCommand(psCommand, null, 5);

            return result;

        }
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return error
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, e.toString() );
        }
        finally {
            // Clean up the temporary certificate file
            try{
                Files.deleteIfExists(certFile);
            }
            catch(final Exception ignored) {}
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Creates a .cat file for the INF and signs it using the self-signed cert
    public static XCom.Pair<Integer, String> createAndSignCatalog(final String infPath, final String providerName)
    {
        // Catalog file must usually be in the same folder as INF
        final String catPath = infPath.substring( 0, infPath.lastIndexOf('.') ) + ".cat";

        try {

            // This PowerShell script:
            //     1. Locates the certificate we created earlier in the Personal store
            //     2. Uses New-FileCatalog to generate a Windows Catalog (v2.0) from the INF
            //     3. Uses Set-AuthenticodeSignature to sign that Catalog
            // All signing steps run in a UAC-elevated child process via Start-Process -Verb RunAs
            final String psCommand = String.format(
                "$tmpOutLog = \"$env:TEMP\\cat_sign_%s_$PID.log\"                                          \r\n" +
                "$exitCode  = 0                                                                            \r\n" +
                "try {                                                                                     \r\n" +
                "    $processHandler = Start-Process -FilePath 'powershell.exe'                            \r\n" +
                "                          -ArgumentList \"-NoProfile -Command `\"                         \r\n" +
                "                              $cert = Get-ChildItem Cert:\\CurrentUser\\My |              \r\n" +
                "                                  Where-Object { $_.Subject -like '*CN=%s*' } |           \r\n" +
                "                                  Select-Object -First 1;                                 \r\n" +
                "                              if(-not $cert) { throw 'Certificate not found' };           \r\n" +
                "                              New-FileCatalog -Path '%s' -CatalogFilePath '%s'            \r\n" +
                "                                  -CatalogVersion 2.0;                                    \r\n" +
                "                              Set-AuthenticodeSignature -FilePath '%s' -Certificate $cert \r\n" +
                "                                  | Out-File `\"$tmpOutLog`\"                             \r\n" +
                "                          `\"\"                                                           \r\n" +
                "                          -Verb RunAs -Wait -PassThru                                     \r\n" +
                "    if($processHandler) {                                                                 \r\n" +
                "        $exitCode = $processHandler.ExitCode                                              \r\n" +
                "    }                                                                                     \r\n" +
                "    else {                                                                                \r\n" +
                "        $exitCode = %d                                                                    \r\n" +
                "    }                                                                                     \r\n" +
                "}                                                                                         \r\n" +
                "catch {                                                                                   \r\n" +
                "    $exitCode = %d                                                                        \r\n" +
                "}                                                                                         \r\n" +
                "Start-Sleep -Milliseconds 100                                                             \r\n" +
                "if(Test-Path $tmpOutLog) {                                                                \r\n" +
                "    Get-Content $tmpOutLog -Raw   -ErrorAction SilentlyContinue                           \r\n" +
                "    Remove-Item $tmpOutLog -Force -ErrorAction SilentlyContinue                           \r\n" +
                "}                                                                                         \r\n" +
                "exit $exitCode                                                                            \r\n",
                providerName, providerName,
                infPath, catPath, catPath,
                RETCODE_PH_NULL, RETCODE_UAC_DECLINED
            );

            final XCom.Pair<Integer, String> result = _runCommand(psCommand, null, 5);

            return result;

        }
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return error
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Installs a local INF file using PnPUtil via PowerShell with UAC elevation
    // NOTE : Use absolute paths for INF files to avoid "File not found" errors in elevated shells
    public static XCom.Pair<Integer, String> installDriver(final String infPath)
    {
        try {

            // Validate input path
            final Path path = Paths.get(infPath);

            if( !infPath.toLowerCase().endsWith(".inf") || !path.isAbsolute() || !Files.exists(path) ) {
                return new XCom.Pair<Integer, String>( RETCODE_INVALID_PATH, String.format(Texts.EMsg_WDriverInstallInvInfPth, infPath) );
            }

            // Build the command
            final boolean isWin7    = System.getProperty("os.name").toLowerCase().contains("windows 7");
            final String  flag      = (isWin7 ? " " : " /install ");
            final String  psCommand =
                "$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()                  \r\n" +
                "$tmpOutLog      = \"$env:TEMP\\pnp_out_$PID.log\"                                                \r\n" +
                "$exitCode       = 0                                                                              \r\n" +
                "try {                                                                                            \r\n" +
                "    $processHandler = Start-Process -FilePath 'cmd.exe'                                              " +
                "                          -ArgumentList \"/v:on /c pnputil.exe /add-driver `\"$env:INF_PATH`\"" + flag +
                "                              > `\"$tmpOutLog`\" 2>&1 & exit !errorlevel!\"                          " +
                "                          -Verb RunAs -Wait -PassThru                                            \r\n" +
                "    if($processHandler) {                                                                        \r\n" +
                "        $exitCode = $processHandler.ExitCode                                                     \r\n" +
                "    }                                                                                            \r\n" +
                "    else {                                                                                       \r\n" +
                "        $exitCode = " + RETCODE_PH_NULL + "                                                      \r\n" +
                "    }                                                                                            \r\n" +
                "}                                                                                                \r\n" +
                "catch {                                                                                          \r\n" +
                "    $exitCode = " + RETCODE_UAC_DECLINED + "                                                     \r\n" +
                "}                                                                                                \r\n" +
                "Start-Sleep -Milliseconds 100                                                                    \r\n" +
                "if(Test-Path $tmpOutLog) {                                                                       \r\n" +
                "    Get-Content $tmpOutLog -Raw   -ErrorAction SilentlyContinue                                  \r\n" +
                "    Remove-Item $tmpOutLog -Force -ErrorAction SilentlyContinue                                  \r\n" +
                "}                                                                                                \r\n" +
                "exit $exitCode                                                                                   \r\n" ;

            final HashMap<String, String> env = new HashMap<>();
            env.put("INF_PATH", infPath);

            // Execute the command
            return _runCommand(psCommand, env, 5);

        }
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return error
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, e.toString() );
        }
    }

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

    // Generates a formatted CDC-ACM INF string for a specific hardware ID
    public static String generateMultiCDCACMInf(final String vid, final String pid, int numInterfaces)
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

    private static final String PROVIDER_NAME = "JxMake_WindowsDriverInstaller";

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

    private static XCom.Pair<Integer, String> _saveInfToFileAndSign(final String vid, final String pid, final String infText)
    {
        final String infPath = _saveInfToFile(vid, pid, infText);
        if(infPath == null) return new XCom.Pair<Integer, String>( RETCODE_INVALID_PATH, String.format(Texts.EMsg_WDriverInstallInvInfPth, "drv_" + vid + "_" + pid) );

        if( isProviderAlreadyTrusted(PROVIDER_NAME).first() != RETCODE_OK ) {
            final XCom.Pair<Integer, String> res = createAndTrustProvider(PROVIDER_NAME);
            if( res.first() != RETCODE_OK ) return res;
        }

        final XCom.Pair<Integer, String> res = createAndSignCatalog(infPath, PROVIDER_NAME);
        if( res.first() != RETCODE_OK ) return res;

        return new XCom.Pair<Integer, String>(RETCODE_OK, infPath);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Make these accessible from SysUtil ??? #####

    public static XCom.Pair<Integer, String> installWinUSBInf(final String vid, final String pid)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, generateWinUSBInf(vid, pid) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public static XCom.Pair<Integer, String> installHIDInf(final String vid, final String pid)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, generateHIDInf(vid, pid) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public static XCom.Pair<Integer, String> installCDCACMInf(final String vid, final String pid)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, generateCDCACMInf(vid, pid) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

    public static XCom.Pair<Integer, String> installMultiCDCACMInf(final String vid, final String pid, int numInterfaces)
    {
        final XCom.Pair<Integer, String> res = _saveInfToFileAndSign( vid, pid, generateMultiCDCACMInf(vid, pid, numInterfaces) );
        if( res.first() != RETCODE_OK ) return res;

        return installDriver( res.second() );
    }

} // WindowsDriverInstaller
