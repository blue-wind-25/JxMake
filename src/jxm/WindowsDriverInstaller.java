/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.nio.charset.*;

import java.util.concurrent.TimeUnit;

import jxm.*;
import jxm.xb.*;


public class WindowsDriverInstaller {

    public static final int RETCODE_EXCEPTION = -1;
    public static final int RETCODE_TIMEOUT   = -2;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Global buffer for the last driver installation attempt
    private String _lastDriverInstallLog = "";

    // Installs a local INF file using PnPUtil via PowerShell with UAC elevation
    // NOTE : Use absolute paths for INF files to avoid "File not found" errors in elevated shells
    private int installDriver(final String infPath) throws IOException, InterruptedException
    {
        _lastDriverInstallLog = "";

        // Use 'cmd /c' inside Start-Process because pnputil.exe does not handle the '>' operator
        // 'cmd /c' allows us to redirect the elevated output to a temporary file.
        final String psCommand =
            "$tmp = \"$env:TEMP\\pnp_res_$PID.log\";                                                                " +
            "$p   = Start-Process -FilePath 'cmd.exe'                                                               " +
            "           -ArgumentList \"/c pnputil.exe /add-driver `\"$env:INF_PATH`\" /install > `\"$tmp`\" 2>&1\" " +
            "           -Verb RunAs -Wait -PassThru;                                                                " +
            "if(Test-Path $tmp) {                                                                                   " +
            "    Get-Content $tmp;                                                                                  " +
            "    Remove-Item $tmp                                                                                   " +
            "};                                                                                                     " +
            "if($p) { exit $p.ExitCode } else { exit 1 }                                                            ";

        final ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe"            ,
            "-NoProfile"                ,
            "-ExecutionPolicy", "Bypass",
            "-Command"                  ,
            psCommand
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
        _lastDriverInstallLog = new String( buff.toByteArray(), Charset.defaultCharset() ).trim();

        return exitCode;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static XCom.Pair<Integer, String> installDriver_noexcept(final String infPath)
    {
        try {
            final WindowsDriverInstaller wdi = new WindowsDriverInstaller();
            final int                    res = wdi.installDriver(infPath);

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
