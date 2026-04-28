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
        // Prepare the PnPUtil command
        //     /add-driver : adds the driver to the store
        //     /install    : installs it onto matching devices
        final String pnpCommand = "pnputil.exe /add-driver \"" + infPath + "\" /install";

        // Wrap the command in PowerShell's Start-Process to trigger UAC
        //     FilePath     : The program to run
        //     ArgumentList : The flags for pnputil
        //     Verb RunAs   : THIS TRIGGERS THE UAC PROMPT
        final String psCommand = String.format(
            "Start-Process -FilePath 'pnputil.exe' -ArgumentList '/add-driver \"%s\" /install' -Verb RunAs -Wait",
            infPath
        );

        final ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe"            ,
            "-NoProfile"                ,
            "-ExecutionPolicy", "Bypass",
            "-Command",
            psCommand
        );

        try {
            // Redirect errors to standard out so we can see them if needed
            pb.redirectErrorStream(true);

            // Start the process and wait for the PowerShell wrapper to finish, not necessarily the
            // elevated pnputil process unless -Wait is used in PS
            return pb.start().waitFor();

        }
        catch(final IOException | InterruptedException e) {
            throw e;
        }

        return -1;
    }

    public static int installDriver(final String infPath)
    {
        try {
            return WindowsDriverInstaller.installDriver(infPath);
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            return -1;
        }
    }

} // WindowsDriverInstaller
