/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.*;
import java.util.*;


public class WindowsDriverInstaller {

    // Installs a local INF file using PnPUtil via PowerShell with UAC elevation.
    public boolean installDriver(final String infPath)
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
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-Command",
            psCommand
        );

        try {
            // Redirect errors to standard out so we can see them if needed
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Note: waitFor() here waits for the PowerShell wrapper to finish,
            // not necessarily the elevated pnputil process unless -Wait is used in PS.
            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static void main(String[] args) {
        WindowsDriverInstaller installer = new WindowsDriverInstaller();
        // Use absolute paths for INF files to avoid "File not found" errors in elevated shells
        boolean success = installer.installDriver("C:\\Drivers\\MyDevice.inf");
        System.out.println("Elevation request sent: " + success);
    }

} // WindowsDriverInstaller
