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
import java.util.Locale;

import java.util.concurrent.TimeUnit;

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
public class WindowsDriverInstaller_PS1 extends WindowsDriverInstaller {

    @Override
    public boolean isUsable()
    { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows"); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _getEncodedCommand(final String script)
    {
        final byte[] utf16Bytes = script.getBytes(StandardCharsets.UTF_16LE);

        return Base64.getEncoder().encodeToString(utf16Bytes);
    }

    // Runs an encoded PowerShell command and captures its output and exit code - returns a pair of [exitCode, outputLog]
    private static XCom.Pair<Integer, String> _runCommand(final String psCommand, final HashMap<String, String> extraEnv, final int waitTimeMinutes) throws IOException, InterruptedException
    {
        // With stdout redirected (as it is here), PowerShell serializes any progress-stream records - e.g. the
        // "Preparing modules for first use" record emitted the first time a session touches a lazily-loaded
        // module/provider such as the Cert: drive - as CLIXML text and merges it into the captured output
        // alongside the real command output. $ProgressPreference = 'SilentlyContinue' suppresses that stream
        // entirely so it can never pollute the returned log text.
        final String fullCommand = "$ProgressPreference = 'SilentlyContinue'\r\n" + psCommand;

        final ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe"            ,
            "-NoProfile"                ,
            "-ExecutionPolicy", "Bypass",
            "-EncodedCommand" , _getEncodedCommand(fullCommand)
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

    // Checks if a certificate with the specified providerName exists in the Trusted Root and Trusted Publisher stores
    @Override
    public XCom.Pair<Integer, String> isProviderAlreadyTrusted(final String providerName)
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
    @Override
    public XCom.Pair<Integer, String> createAndTrustProvider(final String providerName)
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
                // Distinguish an actual UAC decline (Win32 error 1223, ERROR_CANCELLED) from any other failure
                // via the numeric NativeErrorCode rather than parsing $_.Exception.Message, since that text is
                // localized to the user's OS UI language and cannot be matched reliably
                "catch {                                                                                     \r\n" +
                "    $nativeErr = $_.Exception.NativeErrorCode                                               \r\n" +
                "    if(-not $nativeErr -and $_.Exception.InnerException) {                                  \r\n" +
                "        $nativeErr = $_.Exception.InnerException.NativeErrorCode                            \r\n" +
                "    }                                                                                       \r\n" +
                "    $exitCode = if($nativeErr -eq 1223) { %d } else { %d }                                  \r\n" +
                "}                                                                                           \r\n" +
                "Start-Sleep -Milliseconds 100                                                               \r\n" +
                "if(Test-Path $tmpOutLog) {                                                                  \r\n" +
                "    Get-Content $tmpOutLog -Raw   -ErrorAction SilentlyContinue                             \r\n" +
                "    Remove-Item $tmpOutLog -Force -ErrorAction SilentlyContinue                             \r\n" +
                "}                                                                                           \r\n" +
                "exit $exitCode                                                                              \r\n" ,
                providerName, providerName,
                certFile.toAbsolutePath(), certFile.toAbsolutePath(), certFile.toAbsolutePath(),
                RETCODE_PH_NULL, RETCODE_UAC_DECLINED, RETCODE_EXCEPTION
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
            try {
                Files.deleteIfExists(certFile);
            }
            catch(final Exception ignored) {}
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Creates a .cat file for the INF and signs it using the self-signed cert
    @Override
    public XCom.Pair<Integer, String> createAndSignCatalog(final String infPath, final String providerName)
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
                "                                  -HashAlgorithm SHA256                                   \r\n" +
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
                // See createAndTrustProvider() above: use the numeric NativeErrorCode (1223 = ERROR_CANCELLED,
                // i.e. the user declined UAC), not the exception text, since that text is locale-dependent
                "catch {                                                                                   \r\n" +
                "    $nativeErr = $_.Exception.NativeErrorCode                                             \r\n" +
                "    if(-not $nativeErr -and $_.Exception.InnerException) {                                \r\n" +
                "        $nativeErr = $_.Exception.InnerException.NativeErrorCode                          \r\n" +
                "    }                                                                                     \r\n" +
                "    $exitCode = if($nativeErr -eq 1223) { %d } else { %d }                                \r\n" +
                "}                                                                                         \r\n" +
                "Start-Sleep -Milliseconds 100                                                             \r\n" +
                "if(Test-Path $tmpOutLog) {                                                                \r\n" +
                "    Get-Content $tmpOutLog -Raw   -ErrorAction SilentlyContinue                           \r\n" +
                "    Remove-Item $tmpOutLog -Force -ErrorAction SilentlyContinue                           \r\n" +
                "}                                                                                         \r\n" +
                "exit $exitCode                                                                            \r\n",
                providerName, providerName,
                infPath, catPath, catPath,
                RETCODE_PH_NULL, RETCODE_UAC_DECLINED, RETCODE_EXCEPTION
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
    @Override
    public XCom.Pair<Integer, String> installDriver(final String infPath)
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
                // Same rationale as createAndTrustProvider()/createAndSignCatalog(): key off the numeric
                // NativeErrorCode (1223 = ERROR_CANCELLED = UAC declined), not locale-dependent exception text
                "catch {                                                                                          \r\n" +
                "    $nativeErr = $_.Exception.NativeErrorCode                                                    \r\n" +
                "    if(-not $nativeErr -and $_.Exception.InnerException) {                                       \r\n" +
                "        $nativeErr = $_.Exception.InnerException.NativeErrorCode                                 \r\n" +
                "    }                                                                                            \r\n" +
                "    $exitCode = if($nativeErr -eq 1223) { " + RETCODE_UAC_DECLINED + " } else { " + RETCODE_EXCEPTION + " }  \r\n" +
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

} // WindowsDriverInstaller_PS1
