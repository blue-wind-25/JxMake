/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;

import net.codecrete.usb.*;

import jxm.xb.*;


public class USBUtil  {

    public static class USBDevice {

        final int    vid;
        final int    pid;

        final String manufacturerName;
        final String productName;
        final String serialNumber;

        public USBDevice(
            final int    vid,
            final int    pid,
            final String manufacturerName,
            final String productName,
            final String serialNumber
        ) {
            this.vid              = vid;
            this.pid              = pid;

            this.manufacturerName = manufacturerName;
            this.productName      = productName;
            this.serialNumber     = serialNumber;
        }

    } // class USBDevice

} // class USBUtil
