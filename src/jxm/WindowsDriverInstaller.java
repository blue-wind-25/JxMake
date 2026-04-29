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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.util.Base64;

import java.util.concurrent.TimeUnit;

import jxm.*;
import jxm.xb.*;


public class WindowsDriverInstaller {

    public static final int RETCODE_EXCEPTION    = -1;
    public static final int RETCODE_INVALID_PATH = -2;
    public static final int RETCODE_PH_NULL      = -3;
    public static final int RETCODE_UAC_DECLINED = -4;
    public static final int RETCODE_TIMEOUT      = -5;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Global buffer for the last driver installation attempt
    private String _lastDriverInstallLog = "";

    private static String _getEncodedCommand(final String script)
    {
        final byte[] utf16Bytes = script.getBytes(StandardCharsets.UTF_16LE);

        return Base64.getEncoder().encodeToString(utf16Bytes);
    }

    // Installs a local INF file using PnPUtil via PowerShell with UAC elevation
    // NOTE : Use absolute paths for INF files to avoid "File not found" errors in elevated shells
    private int _installDriver(final String infPath) throws IOException, InterruptedException
    {
        _lastDriverInstallLog = "";

        // Validate input path
        final Path path = Paths.get(infPath);

        if( !infPath.toLowerCase().endsWith(".inf") || !path.isAbsolute() || !Files.exists(path) ) {
            _lastDriverInstallLog = String.format(Texts.EMsg_WDriverInstallInvInfPth, infPath);
            return RETCODE_INVALID_PATH;
        }

        // Build the command
        final String psCommand =
            "$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()                     \r\n" +
            "$tmpOutLog      = \"$env:TEMP\\pnp_out_$PID.log\"                                                   \r\n" +
            "$exitCode       = 0                                                                                 \r\n" +
            "try {                                                                                               \r\n" +
            "    $processHandler = Start-Process -FilePath 'cmd.exe'                                                 " +
            "                          -ArgumentList \"/v:on /c pnputil.exe /add-driver `\"$env:INF_PATH`\" /install " +
            "                              > `\"$tmpOutLog`\" 2>&1 & exit !errorlevel!\"                             " +
            "                          -Verb RunAs -Wait -PassThru                                               \r\n" +
            "    if($processHandler) {                                                                           \r\n" +
            "        $exitCode = $processHandler.ExitCode                                                        \r\n" +
            "    }                                                                                               \r\n" +
            "    else {                                                                                          \r\n" +
            "        $exitCode = " + RETCODE_PH_NULL + "                                                         \r\n" +
            "    }                                                                                               \r\n" +
            "}                                                                                                   \r\n" +
            "catch {                                                                                             \r\n" +
            "    $exitCode = " + RETCODE_UAC_DECLINED + "                                                        \r\n" +
            "}                                                                                                   \r\n" +
            "Start-Sleep -Milliseconds 100                                                                       \r\n" +
            "if(Test-Path $tmpOutLog) {                                                                          \r\n" +
            "    Get-Content $tmpOutLog -Raw   -ErrorAction SilentlyContinue                                     \r\n" +
            "    Remove-Item $tmpOutLog -Force -ErrorAction SilentlyContinue                                     \r\n" +
            "}                                                                                                   \r\n" +
            "exit $exitCode                                                                                      \r\n" ;

        // Execute the command
        final ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe"            ,
            "-NoProfile"                ,
            "-ExecutionPolicy", "Bypass",
            "-EncodedCommand" , _getEncodedCommand(psCommand)
        );

        pb.environment().put("INF_PATH", infPath);
        pb.redirectErrorStream(true);

        final Process               proc = pb.start();
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();

        try(
            final InputStream is = proc.getInputStream()
        ) {
            final byte[] data = new byte[4096];
                  int    len;
            while( ( len = is.read(data, 0, data.length) ) != -1 ) buff.write(data, 0, len);
        }

        // Wait for up to some minutes for the user to handle the UAC and the install to finish
        final int waitTime = 5;

        if( !proc.waitFor(waitTime, TimeUnit.MINUTES) ) {
            proc.destroyForcibly(); // Kill the PowerShell wrapper
            _lastDriverInstallLog = String.format(Texts.EMsg_WDriverInstallTimeoutMN, waitTime);
            return RETCODE_TIMEOUT;
        }

        final int exitCode = proc.exitValue();

        // Store the log and return the exit code
        _lastDriverInstallLog = new String( buff.toByteArray(), StandardCharsets.UTF_8 ).trim();

        return exitCode;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static XCom.Pair<Integer, String> installDriver(final String infPath)
    {
        try {
            final WindowsDriverInstaller wdi = new WindowsDriverInstaller();
            final int                    res = wdi._installDriver(infPath);

            return new XCom.Pair<Integer, String>(res, wdi._lastDriverInstallLog);
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

} // WindowsDriverInstaller
