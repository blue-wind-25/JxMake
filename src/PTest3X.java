/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


import java.util.function.IntConsumer;

import jxm.*;
import jxm.tool.fwc.*;
import jxm.ugc.*;
import jxm.xb.*;

import static jxm.ugc.ProgPIC.SubPart;
import static jxm.ugc.ProgPIC.Mode;


//
// Test class (the test application entry point)
//
public class PTest3X {

    private static void _printFWC(final String title, final FWComposer fwc, final int fwStartAddress, final int fwLength)
    {
        int i = 0;
        int z = 0;

        SysUtil.stdDbg().printf("%s\n", title);

        if(fwc != null) {
            SysUtil.stdDbg().printf("Blk# Address  Size\n");
            for( final FWBlock f : fwc.fwBlocks() ) {
                SysUtil.stdDbg().printf( "[%02d] %08X %04X (%d)\n", i++, f.startAddress(), f.length(), f.length() );
                z += f.length();
            }
            SysUtil.stdDbg().println();
        }
        else {
            z = fwLength;
        }

        if(fwLength < 0) return;

        SysUtil.stdDbg().printf ("[Σ∊] %08X %04X (%d)\n", fwStartAddress, z       , z       );
        SysUtil.stdDbg().printf ("[Σ∪] %08X %04X (%d)\n", fwStartAddress, fwLength, fwLength);
        SysUtil.stdDbg().println();
    }

    public static void main(final String[] args)
    {
        // Instantiate the firmware composer
        final FWComposer fwc = new FWComposer();

        final int[] srcBuff = new int[512];

        // Instantiate the programmer
              USB2GPIO       usb2gpio   = null;

              ProgPIC.Config picConfig  = null;
              ProgPIC        pic        = null;

        final IntConsumer    stdPP      = ProgressCB.getStdProgressPrinter( SysUtil.stdDbg() );

        try {

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            jxm.ugc.fl.SWDFlashLoader._testAllBuiltInFlashLoaderClasses();

            // NOTE : If the MCU supports PE and this value is set to 'true', this application will use EICSP.
            final boolean canUsePE  = !true;

            // NOTE : If the MCU supports PE and this value is set to 'true', this application will program the
            //        PE into the MCU ('canUsePE' must be set to 'false').
            final boolean programPE = !true && !canUsePE;

            // Always use JxMake USB-GPIO Module as the backend
            if(true) {
                final boolean  enDM_Error       = true;
                final boolean  enDM_Warning     = true;
                final boolean  enDM_Notice      = !false;
                final boolean  enDM_Information = !false;

                final USB_GPIO dev              = true ? USB_GPIO.autoConnectFirst() : new USB_GPIO("/dev/ttyACM0", "/dev/ttyACM1");

                dev.setAutoNotifyErrorMessage(true);
                dev.enableDebugMessage(enDM_Error, enDM_Warning, enDM_Notice, enDM_Information);

                SysUtil.stdDbg().print("USB-GPIO Ping    = "); SysUtil.stdDbg().printf( "%b\n", dev.ping() );
                SysUtil.stdDbg().print("USB-GPIO Version = "); for( final int v : dev.getVersion() ) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();
                SysUtil.stdDbg().println();

                usb2gpio = dev;
            }

            /*
             * Test 'USB2GPIO' and 'ProgPIC' - PIC10F220/PIC10F320
             *                               - PIC12F508/PIC12F675
             *                               - PIC16F54/PIC16F676/PIC16F73/PIC16F84A/PIC16F877A/PIC16F1503
             *                               - PIC18F4520/PIC18F24J50/PIC18F45K50
             *                               - PIC24FJ64GB002
             *                               - dsPIC30F3011
             *                               - dsPIC33EP16GS202
             */
            if(!true) {

                final int PIC10F220        =  0;
                final int PIC10F320        =  1;
                final int PIC12F508        =  2;
                final int PIC12F675        =  3;
                final int PIC16F54         =  4;
                final int PIC16F676        =  5;
                final int PIC16F73         =  6;
                final int PIC16F84A        =  7;
                final int PIC16F877A       =  8;
                final int PIC16F1503       =  9;
                final int PIC18F4520       = 10;
                final int PIC18F24J50      = 11;
                final int PIC18F45K50      = 12;
                final int PIC24FJ64GB002   = 13;
                final int dsPIC30F3011     = 14;
                final int dsPIC33EP16GS202 = 15;

                /*
                 * ######################################## !!! WARNING !!! ########################################
                 * The initialization sequence for some PIC MCUs may actually corrupt other PIC MCUs
                 * (accidentally erasing their flash/configuration bits)!
                 * ######################################## !!! WARNING !!! ########################################
                 */

                // ##### ??? TODO : PIC16F628 PIC16F688                      ??? #####

                // ##### !!! TODO : dsPIC30F1010/2020/2023                   !!! #####

                // ##### ??? TODO : PIC24FK* PIC24FV* PIC24H* PIC24E*        ??? #####
                // ##### ??? TODO : dsPIC33F* dsPIC33E*GM*/GS[78]* dsPIC33C* ??? #####

                final int    SelTarget = PIC12F675;
                                        /*
                                          0 - PIC10F220
                                          1 - PIC10F320
                                          2 - PIC12F508
                                          3 - PIC12F675
                                          4 - PIC16F54
                                          5 - PIC16F676
                                          6 - PIC16F73
                                          7 - PIC16F84A
                                          8 - PIC16F877A
                                          9 - PIC16F1503
                                         10 - PIC18F4520
                                         11 - PIC18F24J50
                                         12 - PIC18F45K50
                                         13 - PIC24FJ64GB002
                                         14 - dsPIC30F3011
                                         15 - dsPIC33EP16GS202
                                        */

                      int    pgcFreq    = 1000000;
                      int    pgcFreqExW = 0;
                      int    pgcFreqExR = 0;
                      int    fuseHexLen = 4;
                      String ripe       = null;
                      byte[] ripeStd    = null;
                      int    ripeStdSA  = 0;

                switch(SelTarget) {

                    case PIC10F220:
                        picConfig  = ProgPIC16.Config.PIC10F220();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC10F320:
                        picConfig  = ProgPIC16.Config.PIC10F320();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC12F508:
                        picConfig  = ProgPIC16.Config.PIC12F508();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC12F675:
                        picConfig  = ProgPIC16.Config.PIC12F675();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC16F54:
                        picConfig  = ProgPIC16.Config.PIC16F54();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC16F676:
                        picConfig  = ProgPIC16.Config.PIC16F676();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC16F73:
                        picConfig  = ProgPIC16.Config.PIC16F73();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC16F84A:
                        picConfig  = ProgPIC16.Config.PIC16F84A();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC16F877A:
                        picConfig  = ProgPIC16.Config.PIC16F877A();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC16F1503:
                        picConfig  = ProgPIC16.Config.PIC16F1503();
                        pic        = new ProgPIC16( usb2gpio, (ProgPIC16.Config) picConfig );
                        break;

                    case PIC18F4520:
                        picConfig  = ProgPIC18.Config.PIC18F4520();
                        pic        = new ProgPIC18( usb2gpio, (ProgPIC18.Config) picConfig );
                        fuseHexLen = 2;
                        break;

                    case PIC18F24J50:
                        picConfig  = ProgPIC18.Config.PIC18F24J50();
                        pic        = new ProgPIC18( usb2gpio, (ProgPIC18.Config) picConfig );
                        fuseHexLen = 2;
                        break;

                    case PIC18F45K50:
                        pgcFreq    = 4000000; // NOTE : It seems to require lower PGC frequency
                        picConfig  = ProgPIC18.Config.PIC18F45K50();
                        pic        = new ProgPIC18( usb2gpio, (ProgPIC18.Config) picConfig );
                        fuseHexLen = 2;
                        break;

                    case PIC24FJ64GB002:
                        /*
                         * https://ww1.microchip.com/downloads/en/DeviceDoc/RIPE_01b_000033.zip
                         */
                        picConfig  = ProgPIC24.Config.PIC24FJ64GB00x();
                        if(true && canUsePE) {
                            // NOTE : # Somehow, EICSP read only works using hardware-assisted bit-banging SPI.
                            //        # The standard PGC frequency must be <= 90kHz because of P21 is 8uS.
                            picConfig.baseProgSpec.entrySeq = 0x4D434850L;
                            pgcFreq                         =   -90000; // The standard PGC       frequency               (must be hardware-assisted bit-banging SPI)
                            pgcFreqExW                      =  2000000; // The extra    PGC write frequency can be higher (can  be hardware                      SPI)
                            pgcFreqExR                      = -2000000; // The extra    PGC read  frequency can be higher (must be hardware-assisted bit-banging SPI)
                        }
                        pic        = new ProgPIC24( usb2gpio, (ProgPIC24.Config) picConfig );
                        ripe       = SysUtil.resolvePath( "xsdk/pic/RIPE_01b_000033.hex", SysUtil.getUHD() );
                        break;

                    case dsPIC30F3011:
                        /*
                         * ICD3 and newer --- !!! IT DOES NOT WORK AT ALL !!!
                         *
                         * https://ww1.microchip.com/downloads/en/DeviceDoc/RIPE_02_000000.zip
                         *
                         * NOTE : There is no 0xBB at [0x05BE * 3 / 2] in the firmware !!!
                         */
                        /*
                         * ICD2 --- IT WORKS!
                         *
                         * https://www.microchip.com/en-us/tools-resources/archives/mplab-ecosystem
                         * https://ww1.microchip.com/downloads/en/DeviceDoc/MPLAB_IDE_8_92.zip
                         *
                         * C:\Program Files (x86)\Microchip\MPLAB IDE\ICD2\pe.hex
                         */
                        picConfig  = ProgDSPIC30.Config.dsPIC30F3011();
                        if(true && canUsePE) {
                            // NOTE : # Somehow, EICSP read only works using hardware-assisted bit-banging SPI.
                            //        # The standard PGC frequency must be <= 60kHz because of P11 is 10uS.
                            picConfig.baseProgSpec.mode = Mode.HVSimple1;
                            pgcFreq                     =  -60000; // The standard PGC       frequency               (must be hardware-assisted bit-banging SPI)
                            pgcFreqExW                  =  500000; // The extra    PGC write frequency can be higher (can  be hardware                      SPI)
                            pgcFreqExR                  = -500000; // The extra    PGC read  frequency can be higher (must be hardware-assisted bit-banging SPI)
                        }
                        pic        = new ProgDSPIC30( usb2gpio, (ProgDSPIC30.Config) picConfig );
                        ripe       = SysUtil.resolvePath( "xsdk/pic/RIPE_02_000000.hex", SysUtil.getUHD() ); // IT DOES NOT WORK!
                        ripe       = SysUtil.resolvePath( "xsdk/pic/pe.hex"            , SysUtil.getUHD() ); // IT WORKS!
                        if(true) {
                            ripeStd    = ProgPIC.stdPE_dsPIC30F();
                            ripeStdSA  = ProgPIC.stdPE_dsPIC30F_startAddress();
                        }
                        break;

                    case dsPIC33EP16GS202:
                        /*
                         * https://ww1.microchip.com/downloads/en/DeviceDoc/RIPE_17b_000049.zip
                         *
                         * https://microchip-pic-avr-solutions.github.io/dspic-pic24-ripe-file-documentation
                         * https://microchip-pic-avr-solutions.github.io/dspic-pic24-ripe-file-documentation/PE_Mapping.xlsx
                         */
                        picConfig  = ProgDSPIC33.Config.dsPIC33EP16GS202();
                        //*
                        if(true && canUsePE) {
                            // NOTE : # Somehow, EICSP read only works using hardware-assisted bit-banging SPI.
                            //        # The standard PGC frequency must be <= 60kHz because of P11 is 10uS.
                            picConfig.baseProgSpec.entrySeq = 0x4D434850L;
                            pgcFreq                         =  -60000; // The standard PGC       frequency               (must be hardware-assisted bit-banging SPI)
                            pgcFreqExW                      =  500000; // The extra    PGC write frequency can be higher (can  be hardware                      SPI)
                            pgcFreqExR                      = -500000; // The extra    PGC read  frequency can be higher (must be hardware-assisted bit-banging SPI)
                        }
                        pic        = new ProgDSPIC33( usb2gpio, (ProgDSPIC33.Config) picConfig );
                        ripe       = SysUtil.resolvePath( "xsdk/pic/RIPE_17b_000049.hex", SysUtil.getUHD() );
                        ripe       = SysUtil.resolvePath( "xsdk/pic/RIPE_17b_000050.hex", SysUtil.getUHD() );
                        if(true) {
                            ripeStd    = ProgPIC.stdPE_dsPIC33EP_GS25();
                            ripeStdSA  = ProgPIC.stdPE_dsPIC33EP_GS25_startAddress();
                        }
                        break;

                    // ##### !!! TODO : More configurations !!! #####

                } // switch

                //*
                final String[] hexFiles = new String[] {
                                               "../src/1-TestData/PTest3X.Blink-PIC10F220.asm.hex"     , /*  0 */
                                               "../src/1-TestData/PTest3X.Blink-PIC10F320.asm.hex"     , /*  1 */
                                               "../src/1-TestData/PTest3X.Blink-PIC12F508.asm.hex"     , /*  2 */
                                               "../src/1-TestData/PTest3X.Blink-PIC12F675.asm.hex"     , /*  3 */
                                               "../src/1-TestData/PTest3X.Blink-PIC16F54.asm.hex"      , /*  4 */
                                               "../src/1-TestData/PTest3X.Blink-PIC16F676.asm.hex"     , /*  5 */
                                               "../src/1-TestData/PTest3X.Blink-PIC16F73.asm.hex"      , /*  6 */
                                               "../src/1-TestData/PTest3X.Blink-PIC16F84A.asm.hex"     , /*  7 */
                                               "../src/1-TestData/PTest3X.Blink-PIC16F877A.asm.hex"    , /*  8 */
                                               "../src/1-TestData/PTest3X.Blink-PIC16F1503.asm.hex"    , /*  9 */
                                               "../src/1-TestData/PTest3X.Blink-PIC18F4520.asm.hex"    , /* 10 */
                                               "../src/1-TestData/PTest3X.Blink-PIC18F24J50.asm.hex"   , /* 11 */
                                               "../src/1-TestData/PTest3X.Blink-PIC18F45K50.asm.hex"   , /* 12 */
                                               "../src/1-TestData/PTest3X.Blink-PIC24FJ64GB002.c.hex"  , /* 13 */
                                               "../src/1-TestData/PTest3X.Blink-dsPIC30F3011.c.hex"    , /* 14 */
                                               "../src/1-TestData/PTest3X.Blink-dsPIC33EP16GS202.c.hex"  /* 15 */
                                           };

                final String   hex      = hexFiles[SelTarget];
                fwc.loadIntelHexFile(hex);

                final ProgPIC.FWD fwd = pic.fwDecompose(fwc);

                final byte[] fwDataBuff     = fwd.fwDataBuff;
                final int    fwLength       = fwd.fwLength;
                final int    fwStartAddress = fwd.fwStartAddress;

                final byte[] cfDataBuff     = fwd.cfDataBuff;
                final int    cfLength       = fwd.cfLength;
                final int    cfStartAddress = fwd.cfStartAddress;

                final byte[] epDataBuff     = fwd.epDataBuff;
                final int    epLength       = fwd.epLength;
                final int    epStartAddress = fwd.epStartAddress;
                      byte[] peDataBuff     = null;
                      int    peLength       = 0;
                      int    peStartAddress = 0;

                    _printFWC( "### ENTIRE USER FW ###", fwc   , (int) fwc.fwBlocks().get(0).startAddress(), -1       );
                    _printFWC( "CODE"                  , fwd.fw, fwStartAddress                            , fwLength );
                    _printFWC( "CONFIG"                , fwd.cf, cfStartAddress                            , cfLength );
                    _printFWC( "EEPROM"                , fwd.ep, epStartAddress                            , epLength );
                if(ripeStd != null) {
                    peDataBuff     = ripeStd;
                    peLength       = ripeStd.length;
                    peStartAddress = ripeStdSA;
                    _printFWC( "### STANDARD PE FW ###", null  , peStartAddress                            , peLength );
                }
                else if(ripe != null) {
                    if(!true) {
                        final FWComposer xwc = new FWComposer();
                        xwc.loadIntelHexFile( ripe           , -0x01000000L );
                        xwc.saveIntelHexFile( "/tmp/org0.hex", (byte) 0xFF  );
                    }
                    final FWComposer  xwc = new FWComposer (   ); xwc.loadIntelHexFile(ripe);
                    final ProgPIC.FWD xwd = pic.fwDecompose(xwc);
                    peDataBuff     = xwd.fwDataBuff;
                    peLength       = xwd.fwLength;
                    peStartAddress = xwd.fwStartAddress;
                    _printFWC( "### ENTIRE RIPE FW ###", xwc   , (int) xwc.fwBlocks().get(0).startAddress(), -1       );
                    _printFWC( "CODE"                  , xwd.fw, peStartAddress                            , peLength );
                    if(!true) {
                        for(int i = 0; i < peLength; ++i) {
                                                                 //SysUtil.stdDbg().printf("%02X ",  peDataBuff[i]  );
                            if( ( peDataBuff[i] & 0xFF ) == 0xCB ) SysUtil.stdDbg().printf("[#CB] %06X\n", i * 2 / 3); // [#CB] 0007F0
                            if( ( peDataBuff[i] & 0xFF ) == 0xBB ) SysUtil.stdDbg().printf("[#BB] %06X\n", i * 2 / 3); // [#BB] 0005BE
                            if( ( peDataBuff[i] & 0xFF ) == 0xDF ) SysUtil.stdDbg().printf("[#DF] %06X\n", i * 2 / 3); // [#DF] 000BFE
                        }
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if(true) {
                    SysUtil.stdDbg().println("Turn on Vdd now!\n(unless your programmer does it automatically)\n");
                    if( !( (ProgPIC16) pic )._pic16_recover(5000) ) throw XCom.newException("ERR: pic._pic16_recovery()" );
                    usb2gpio.shutdown();
                    SysUtil.stdDbg().println("DONE");
                    SysUtil.systemExit();
                }
                //*/

                /*
                if(true) {
                    SysUtil.stdDbg().println("Turn on Vdd now!\n(unless your programmer does it automatically)\n");
                    if( !( (ProgPIC16) pic )._pic16_unbrick(5000) ) throw XCom.newException("ERR: pic._pic16_recovery()" );
                    usb2gpio.shutdown();
                    SysUtil.stdDbg().println("DONE");
                    SysUtil.systemExit();
                }
                //*/

                if( !pic.begin(pgcFreq) ) throw XCom.newException("ERR: pic.begin()");

                if(pgcFreqExW != 0 && pgcFreqExR != 0) {
                    if( !pic.setEICSPExtraSpeed(pgcFreqExW, pgcFreqExR) ) throw XCom.newException("ERR: pic.setEICSPExtraSpeed()");
                }

                //*
                if( !pic.readSignature() ) throw XCom.newException("ERR: pic.readSignature()");

                final int[][] sigBytesA = new int[][] {
                                              new int[] { 0x00, 0x00 }, //  0 - PIC10F220      // NOTE : No device ID
                                              new int[] { 0x29, 0xA0 }, //  1 - PIC10F320
                                              new int[] { 0x00, 0x00 }, //  2 - PIC12F508      // NOTE : No device ID
                                              new int[] { 0x00, 0x00 }, //  3 - PIC12F675      // NOTE : No device ID
                                              new int[] { 0x00, 0x00 }, //  4 - PIC16F54       // NOTE : No device ID
                                              new int[] { 0x10, 0xE0 }, //  5 - PIC16F676
                                              new int[] { 0x06, 0x00 }, //  6 - PIC16F73
                                              new int[] { 0x05, 0x60 }, //  7 - PIC16F84A
                                              new int[] { 0x0E, 0x20 }, //  8 - PIC16F877A
                                              new int[] { 0x2C, 0xE0 }, //  9 - PIC16F1503
                                              new int[] { 0x10, 0x80 }, // 10 - PIC18F4520
                                              new int[] { 0x4C, 0x00 }, // 11 - PIC18F24J50
                                              new int[] { 0x5C, 0x00 }, // 12 - PIC18F45K50
                                              new int[] { 0x42, 0x07 }, // 13 - PIC24FJ64GB002
                                              new int[] { 0x01, 0xC1 }, // 14 - dsPIC30F3011
                                              new int[] { 0x6D, 0x01 }  // 15 - dsPIC33EP16GS202
                                          };

                final int[]   sigBytes  = sigBytesA[SelTarget];

                if( !pic.verifySignature(sigBytes) ) throw XCom.newException("ERR: pic.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : pic.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                boolean programChip = false;

                //*
                if( programPE && !pic.inEICSPMode() && peLength >= 0 ) {
                    SysUtil.stdDbg().println("Programming PE");
                    SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoStringPE( pic._flashMemoryAlignWriteSize(peLength) ) );
                    final long tx1 = SysUtil.getNS();
                    if( !pic.programPE(peDataBuff, stdPP) ) throw XCom.newException("ERR: pic.programPE()");
                    final long tx2 = SysUtil.getNS();
                    SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", peDataBuff.length, (tx2 - tx1) * 0.000000001, 1000000000.0 * peDataBuff.length / (tx2 - tx1) );
                    SysUtil.stdDbg().println();
                    if(true) {
                        pic     .end();
                        usb2gpio.shutdown();
                        SysUtil .systemExit();
                    }
                }
                //*/

                /*
                if(true) throw XCom.newException("### BREAK ###");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                final long te1 = SysUtil.getNS();
                if( !pic.chipErase() ) throw XCom.newException("ERR: pic.chipErase()");
                final long te2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", picConfig.memoryFlash.totalSize, (te2 - te1) * 0.000000001, 1000000000.0 * picConfig.memoryFlash.totalSize / (te2 - te1) );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( pic._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !pic.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: pic.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();

                programChip = true;
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = pic.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: pic.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: pic.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], pic.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = true ? fwLength : (picConfig.memoryFlash.writeBlockSize * 8);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !pic.readFlash(fwStartAddress, rdFlashSize, stdPP) ) throw XCom.newException("ERR: pic.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(true) {
                    int d = 0;
                    for(final int b : pic.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                        if(++d >= picConfig.memoryFlash.writeBlockSize * 8) {
                            d = 0;
                            SysUtil.stdDbg().println();
                        }
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                //*
                if( true && !pic.inEICSPMode() ) {
                    long[] fuses = pic.readFuses();
                    if(fuses == null) throw XCom.newException("ERR: pic.readFuses()");

                    final String fmtFuseXX = (fuseHexLen == 8) ? "%08X "
                                           : (fuseHexLen == 4) ? "%04X "
                                           :                     "%02X ";

                    final String fmtFuseSS = (fuseHexLen == 8) ? "---NA--- "
                                           : (fuseHexLen == 4) ? "-NA- "
                                           :                     "NA ";

                    final int    fmtFuseLN = (fuseHexLen == 8) ?  8
                                           : (fuseHexLen == 4) ? 15
                                           :                     25;

                    SysUtil.stdDbg().print("Fuses (before)        = ");
                    for(int f = 0; f < fuses.length; ++f) {
                        if(fuses[f] < 0) SysUtil.stdDbg().printf(fmtFuseSS          );
                        else             SysUtil.stdDbg().printf(fmtFuseXX, fuses[f]);
                        if( (fmtFuseLN != 0) &&  ( (f + 1) % fmtFuseLN == 0 ) ) SysUtil.stdDbg().printf("\n                        ");
                    }
                    SysUtil.stdDbg().println("\n");

                    if( true && (!true || programChip)  ) {
                        if(true && cfLength > 0) {
                            if(false) for(final byte i : cfDataBuff) SysUtil.stdDbg().printf(">>> %02X\n", i & 0xFF);
                            if( !pic.writeFuses(cfDataBuff, cfStartAddress, cfLength) ) throw XCom.newException("ERR: pic.writeFuses()");
                        }
                        else {
                            if(SelTarget == PIC10F220) {
                                fuses[0] = 0b000000011011; // Safe configuration bits
                            }
                            else if(SelTarget == PIC10F320) {
                                fuses[0] = 0b01100111100000; // Safe configuration bits
                            }
                            else if(SelTarget == PIC12F508) {
                                fuses[0] = 0b000000011010; // Use INTOSC
                            }
                            else if(SelTarget == PIC12F675) {
                                fuses[0] = fuses[0] & 0b11000000000000 | 0b00000110110101; // Safe configuration bits
                            }
                            else if(SelTarget == PIC16F54) {
                                fuses[0] = 0b000000001010; // Use HS oscillator
                              //fuses[0] = 0b000000001001; // Use XT oscillator
                            }
                            else if(SelTarget == PIC16F676) {
                                fuses[0] = 0b00000110100100; // Use internal oscillator
                              //fuses[0] = 0b00000000000101; // Code protect, disable nMCLR, and use internal oscillator
                            }
                            else if(SelTarget == PIC16F73) {
                                fuses[0] = 0b00000000000011; // Use RC oscillator
                            }
                            else if(SelTarget == PIC16F84A) {
                                fuses[0] = 0b11111111111110; // Use HS oscillator
                              //fuses[0] = 0b00000000001110; // Code protect and use HS oscillator
                            }
                            else if(SelTarget == PIC16F877A) {
                                fuses[0] = 0b10111110001011; // Use RC oscillator
                              //fuses[0] = 0b00111010001011; // Code protect and use RC oscillator
                            }
                            else if(SelTarget == PIC18F4520) {
                                fuses[1] = 0b00001000; // Use internal oscillator block; port function on RA6; port function on RA7
                                fuses[1] = 0b00001001; // Use internal oscillator block; CLKO function on RA6; port function on RA7
                                fuses[2] = 0b00000000; // Disable brown-out reset and enable power-up timer
                                fuses[3] = 0b00011110; // Disable watchdog
                            }
                            else if(SelTarget == PIC18F24J50) {
                                // NOTE : # In this MCU series, the configuration words/bits are part of the program memory space;
                                //          therefore, direct modification of the configuration words/bits is not supported!
                                //        # Due to the above statement, the modification below will not be written!
                                fuses[0] = 0b10101110; // No divide
                                fuses[1] = 0b00000111; // No CPU system clock divide
                                fuses[2] = 0b00010000; // INTOSC or INTRC (PLL always disabled); port function on RA6 and RA7
                            }
                            else if(SelTarget == PIC18F45K50) {
                                fuses[1] = 0b00001000; // Use internal oscillator block; port function on RA6; port function on RA7
                                fuses[1] = 0b00001001; // Use internal oscillator block; CLKO function on RA6; port function on RA7
                                fuses[2] = 0b01000000; // Disable brown-out reset and enable power-up timer
                                fuses[3] = 0b00111100; // Disable watchdog
                            }
                            else if(SelTarget == PIC24FJ64GB002) {
                                fuses[0] &= 0b1011111100111111; // Disable JTAG and watchdog
                            }
                            else if(SelTarget == dsPIC30F3011) {
                                fuses[0] = 0xC100; // Use internal fast RC oscillator
                                fuses[1] = 0x003F; // Disable watchdog
                                // The original fuses
                                if(false) {
                                    fuses[0] = 0xC100;
                                    fuses[1] = 0x803F;
                                    fuses[2] = 0x87B3;
                                    fuses[3] = 0x310F;
                                    fuses[4] = 0x330F;
                                    fuses[5] = 0x0007;
                                    fuses[6] = 0xC003;
                                }
                            }
                            else if(SelTarget == dsPIC33EP16GS202) {
                                fuses[3] = 0b0000000010000111; // Use fast RC oscillator with divide-by-N
                                fuses[4] = 0b0000000011000011; // PLL lock is disabled, clock switching and fail-safe clock disabled, allows multiple peripheral-pin-pelect reconfigurations, OSC2 is GPIO, primary oscillator is disabled
                                fuses[7] = 0b0000000010000011; // JTAG is disabled, communicates on PGEC1/PGED1
                            }
                            if( !pic.writeFuses(fuses) ) throw XCom.newException("ERR: pic.writeFuses()");
                        }
                        fuses = pic.readFuses();
                        if(fuses == null) throw XCom.newException("ERR: pic.readFuses()");
                    }

                    SysUtil.stdDbg().print("Fuses (after )        = ");
                    for(int f = 0; f < fuses.length; ++f) {
                        if(fuses[f] < 0) SysUtil.stdDbg().printf(fmtFuseSS          );
                        else             SysUtil.stdDbg().printf(fmtFuseXX, fuses[f]);
                        if( (fmtFuseLN != 0) &&  ( (f + 1) % fmtFuseLN == 0 ) ) SysUtil.stdDbg().printf("\n                        ");
                    }
                    SysUtil.stdDbg().println("\n");
                }
                //*/

                /*
                if( pic.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, picConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = pic.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: pic.readEEPROM()");

                            if( !pic.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: pic.writeEEPROM()");

                            final int v1 = pic.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: pic.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                }
                //*/
                /*
                if( pic.config().memoryEEPROM.totalSize > 0 && epLength > 0 ) {

                    // NOTE : Do not test multi-commit if the EEPROM is not auto-erase!
                    final boolean eeTestMultiCommit = true && pic.supportsEEPROMAutoErase();

                    SysUtil.stdDbg().println("Testing EEPROM (using the Embedded Data)");

                    if(true && eeTestMultiCommit) {
                        if( !pic.writeEEPROM( new byte[16], epStartAddress, 16 ) ) throw XCom.newException("ERR: pic.writeEEPROM()");
                        if( !pic.commitEEPROM() ) throw XCom.newException("ERR: pic.commitEEPROM()");
                        SysUtil.sleepMS(500);
                    }

                    SysUtil.stdDbg().print('"');
                    for(int eIdx = 0; eIdx < epLength; ++eIdx) SysUtil.stdDbg().printf( "%c", pic.readEEPROM(eIdx) );
                    SysUtil.stdDbg().print('"');
                    SysUtil.stdDbg().println();

                    if( true && !pic.writeEEPROM(epDataBuff, epStartAddress, epLength) ) throw XCom.newException("ERR: pic.writeEEPROM()");

                    if(true && eeTestMultiCommit) {
                        if( !pic.commitEEPROM() ) throw XCom.newException("ERR: pic.commitEEPROM()");
                        SysUtil.sleepMS(500);
                    }

                    SysUtil.stdDbg().print('"');
                    for(int eIdx = 0; eIdx < epLength; ++eIdx) SysUtil.stdDbg().printf( "%c", pic.readEEPROM(eIdx) );
                    SysUtil.stdDbg().print('"');
                    SysUtil.stdDbg().println();

                    SysUtil.stdDbg().printf("(%d bytes)\n", epLength);
                }
                //*/

                if( !pic.end() ) throw XCom.newException("ERR: pic.end()");

                usb2gpio.shutdown();

            } // if

        } // try
        catch(final Exception e) {
            if(pic      != null) pic     .end();
            if(usb2gpio != null) usb2gpio.resetAndShutdown();
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class PTest3X

