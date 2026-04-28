/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import jxm.xb.*;


public class WindowsDriverInstaller {

    // Installs a local INF file using PnPUtil via PowerShell with UAC elevation
    // NOTE : Use absolute paths for INF files to avoid "File not found" errors in elevated shells
    public static int installDriver(final String infPath) throws IOException, InterruptedException
    {
        // # Use an environment variable to pass the path safely into the PowerShell sub-process.
        // # This prevents command injection and handles special characters (', $, &, etc.) perfectly.
        final String psCommand = "$p = Start-Process -FilePath 'pnputil.exe' -ArgumentList \"/add-driver `\"$env:INF_PATH`\" /install\" -Verb RunAs -Wait -PassThru; exit $p.ExitCode";

        final ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe"            ,
            "-NoProfile"                ,
            "-ExecutionPolicy", "Bypass",
            "-Command"                  ,
            psCommand
        );

        try {
            // Inject the path into the process environment
            pb.environment().put("INF_PATH", infPath);

            // Redirect errors to standard out so we can see them if needed
            pb.redirectErrorStream(true);

            // Start the process and wait for the PowerShell wrapper to finish
            return pb.start().waitFor();

        }
        catch(final IOException | InterruptedException e) {
            throw e;
        }
    }

    public static int installDriver_noexcept(final String infPath)
    {
        try {
            return WindowsDriverInstaller.installDriver(infPath);
        }
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return -1;
    }

} // WindowsDriverInstaller
