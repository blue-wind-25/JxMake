/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


import jxm.JxMake;


//
// JXM_Main class (the main application entry point)
//
public class JXM_Main {

    static {
        /*
         * Allow the 'Authorization' header to be set manually in java.net.http.HttpClient.
         * This header is restricted by default in JDK 11+ and stripped if set manually,
         * which breaks preemptive authentication.
         *
         * This property MUST be set before the HttpClient class is loaded/initialized.
         */
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Authorization");
    }

    public static void main(final String[] args)
    { JxMake.process(args); }

} // class JXM_Main


