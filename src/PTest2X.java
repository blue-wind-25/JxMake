/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


import java.util.function.IntConsumer;

import jxm.*;
import jxm.tool.fwc.*;
import jxm.ugc.*;
/*
import jxm.ugc.stm32xf.*;
//*/
import jxm.xb.*;


//
// Test class (the test application entry point)
//
public class PTest2X {

    private static void _printIDCODE(final String mcuName, final long idcode)
    {
        /*
         * Bit#   31...28    27...20   19...17   16    15...12   11...1          0
         *        REVISION   PARTNO    0...0     MIN   VERSION   DESIGNER        RAO
         *        0x?        0x??      0b000     0b?   0x?       0b???????????   0b?
         */
        SysUtil.stdDbg().printf( "----- %s -----\n" , mcuName                      );
        SysUtil.stdDbg().printf( "REVISION = %02X\n", (idcode & 0xF0000000L) >> 28 );
        SysUtil.stdDbg().printf( "PARTNO   = %02X\n", (idcode & 0x0FF00000L) >> 20 );
        SysUtil.stdDbg().printf( "MIN      = %02X\n", (idcode & 0x00010000L) >> 16 );
        SysUtil.stdDbg().printf( "VERSION  = %02X\n", (idcode & 0x0000F000L) >> 12 );
        SysUtil.stdDbg().printf( "DESIGNER = %03X\n", (idcode & 0x00000FFEL) >>  1 );
        SysUtil.stdDbg().printf( "RAO      = %02X\n", (idcode & 0x00000001L) >>  0 );
        SysUtil.stdDbg().printf( "\n"                                              );
    }

    public static void main(final String[] args)
    {
        /*
        _printIDCODE("STM32F103CBT6", 0x1BA01477L);
        _printIDCODE("STM32F411CEU6", 0x2BA01477L);
        _printIDCODE("STM32G431CBT6", 0x2BA01477L);
        _printIDCODE("STM32H750VBT6", 0x6BA02477L);
        _printIDCODE("STM32L011D4P6", 0x0BC11477L);
        _printIDCODE("RP2040"       , 0x0BC12477L);
        _printIDCODE("NRF51822"     , 0x0BB11477L);
        _printIDCODE("NRF52840"     , 0x2BA01477L);
        _printIDCODE("ATSAMD21G18A" , 0x0BC11477L);
        _printIDCODE("ATSAM3X8E"    , 0x2BA01477L);
        _printIDCODE("R7FA4M1"      , 0x5BA02477L);
        //*/

        // Instantiate the firmware composer
        final FWComposer fwc = new FWComposer();

        // Instantiate the programmer
              USB2GPIO        usb2gpio   = null;

              ProgSWIM.Config swimConfig = null;
              ProgSWIM        swim       = null;

              ProgSWD.Config  swdConfig  = new ProgSWD.Config();
              ProgSWD         swd        = null;

        final IntConsumer     stdPP      = ProgressCB.getStdProgressPrinter( SysUtil.stdDbg() );

        try {

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            jxm.ugc.fl.SWDFlashLoader._testAllBuiltInFlashLoaderClasses();

            // Select the backend
            final int                       backend           = 0; // 0 : JxMake USB-GPIO Module
                                                                   // 1 : JxMake DASA
                                                                   // 2 : USB-ISS

            final boolean                   bootloaderMode    = false; // NOTE : It is currently only used by RP2350
                  boolean                   allowBB           = false;
                  ProgSWD.SWDClockFrequency swdClockFrequency = ProgSWD.SWDClockFrequency._8MHz;

            switch(backend) {

                // Use JxMake USB-GPIO Module
                case 0:
                    final boolean enDM_Error       = true;
                    final boolean enDM_Warning     = true;
                    final boolean enDM_Notice      = false;
                    final boolean enDM_Information = false;
                    if(true) {
                        if(!true) {
                            final USB_GPIO test = !true ? USB_GPIO.autoConnectFirst() : new USB_GPIO("/dev/ttyACM0", "/dev/ttyACM1");
                            SysUtil.stdDbg().print("USB-GPIO Version = ");
                            for( final int v : test.getVersion(true) ) SysUtil.stdDbg().printf("%02X ", v);
                            SysUtil.stdDbg().println();
                            test.reset();
                            SysUtil.stdDbg().print("USB-GPIO Version = ");
                            for( final int v : test.getVersion(true) ) SysUtil.stdDbg().printf("%02X ", v);
                            SysUtil.stdDbg().println();
                            test.resetAndShutdown();
                            SysUtil.systemExit();
                        }

                        //*
                        final USB_GPIO dev = true ? USB_GPIO.autoConnectFirst() : new USB_GPIO("/dev/ttyACM0", "/dev/ttyACM1");

                        dev.setAutoNotifyErrorMessage(true);
                        dev.enableDebugMessage(enDM_Error, enDM_Warning, enDM_Notice, enDM_Information);

                        SysUtil.stdDbg().print("USB-GPIO Ping    = "); SysUtil.stdDbg().printf( "%b\n", dev.ping() );
                        SysUtil.stdDbg().print("USB-GPIO Version = "); for( final int v : dev.getVersion() ) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();
                        SysUtil.stdDbg().println();

                        usb2gpio = dev;
                        allowBB  = false;
                        //*/
                    }
                    break;

                // Use JxMake DASA
                case 1:
                    if(true) {
                        final DASA dev = new DASA("/dev/ttyUSB0");
                        usb2gpio = dev;
                        allowBB  = true;
                    }
                    break;

                // Use USB-ISS
                case 2:
                    if(true) {
                        final USB_ISS dev = new USB_ISS("/dev/ttyACM0");
                        SysUtil.stdDbg().print("USB-ISS Version       = "); for( final int v : dev.getVersion     () ) SysUtil.stdDbg().printf("%02X ", v); SysUtil.stdDbg().println();
                        SysUtil.stdDbg().print("USB-ISS Serial Number = "); for( final int v : dev.getSerialNumber() ) SysUtil.stdDbg().printf("%c"   , v); SysUtil.stdDbg().println();
                        SysUtil.stdDbg().println();
                        usb2gpio = dev;
                        allowBB  = true;
                    }
                    break;

                // Invalid index
                default:
                    SysUtil.systemExitError();
                    break;

            } // switch

            // Test 'USB2GPIO' and 'ProgSWIM' - STM8S003F3P6/STM8S103F3P6/STM8L151K4T6
            if(!true) {

                final int STM8S003  = 0;
                final int STM8S103  = 1;
                final int STM8L151  = 2;

                final int SelTarget = STM8S103;
                                     /*
                                      STM8S003
                                      STM8S103
                                      STM8L151
                                     */

                switch(SelTarget) {

                    case STM8S003:
                        /*
                        swimConfig = new ProgSWIM.ConfigSTM8S();

                        swimConfig.memoryFlash.address    = 0x8000;
                        swimConfig.memoryFlash.totalSize  = 8192;
                        swimConfig.memoryFlash.pageSize   =   64;
                        swimConfig.memoryFlash.numPages   =  128;

                        swimConfig.memoryEEPROM.address   = 0x4000;
                        swimConfig.memoryEEPROM.totalSize = 128;
                        swimConfig.memoryEEPROM.pageSize  =  64;
                        swimConfig.memoryEEPROM.numPages  =   2;
                        */

                        swimConfig = ProgSWIM.ConfigSTM8S.STM8S003();
                        break;

                    case STM8S103:
                        /*
                        swimConfig = new ProgSWIM.ConfigSTM8S();

                        swimConfig.memoryFlash.address    = 0x8000;
                        swimConfig.memoryFlash.totalSize  = 8192;
                        swimConfig.memoryFlash.pageSize   =   64;
                        swimConfig.memoryFlash.numPages   =  128;

                        swimConfig.memoryEEPROM.address   = 0x4000;
                        swimConfig.memoryEEPROM.totalSize = 640;
                        swimConfig.memoryEEPROM.pageSize  =  64;
                        swimConfig.memoryEEPROM.numPages  =  10;
                        */

                        swimConfig = ProgSWIM.ConfigSTM8S.STM8S103();
                        break;

                    case STM8L151:
                        /*
                        swimConfig = new ProgSWIM.ConfigSTM8L(true);

                        swimConfig.memoryFlash.address    = 0x8000;
                        swimConfig.memoryFlash.totalSize  = 16384;
                        swimConfig.memoryFlash.pageSize   =   128;
                        swimConfig.memoryFlash.numPages   =   128;

                        swimConfig.memoryEEPROM.address   = 0x1000;
                        swimConfig.memoryEEPROM.totalSize = 1024;
                        swimConfig.memoryEEPROM.pageSize  =  128;
                        swimConfig.memoryEEPROM.numPages  =    8;
                        */

                        swimConfig = ProgSWIM.ConfigSTM8L.STM8L151();
                        break;

                } // switch

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(swimConfig) ); SysUtil.systemExit();

                swim = new ProgSWIM(usb2gpio, swimConfig);

                //*
                final String[] hexFiles = new String[] {
                                               "../src/1-TestData/PTest2X.Blink-STM8S003F3P6.cpp.hex", /* 0 */
                                               "../src/1-TestData/PTest2X.Blink-STM8S103F3P6.cpp.hex", /* 1 */
                                               "../src/1-TestData/PTest2X.Blink-STM8L151K4T6.cpp.hex"  /* 2 */
                                           };
                final String   hex      = hexFiles[SelTarget];
                fwc.loadIntelHexFile(hex);

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( swim._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;

                int i = 0;
                int z = 0;
                SysUtil.stdDbg().printf("Blk# Address  Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %08X %04X (%d)\n", i++, f.startAddress(), f.length(), f.length() );
                    z += f.length();
                }
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().printf ("[Σ∊] %08X %04X (%d)\n", fwStartAddress, z       , z       );
                SysUtil.stdDbg().printf ("[Σ∪] %08X %04X (%d)\n", fwStartAddress, fwLength, fwLength);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println();
                //*/

                /*
                final int tsa = 0xC0;
                for(i = tsa; i < tsa + 32; ++i) System.out.printf("0x%02X, ", fwDataBuff[i]);
                System.out.println();
                //*/

                if( !swim.begin() ) throw XCom.newException("ERR: swim.begin()");

                //*
                // STM8S003 = 00 00 FF 00 FF 00 FF 00 FF 00 FF
                // STM8S103 = 00 00 FF 00 FF 00 FF 00 FF 00 FF
                // STM8L151 = AA NA 00 NA NA NA NA NA 00 00 00 00 00
                int[] fuses = swim.readFuses();
                if(fuses == null) throw XCom.newException("ERR: swim.readFuses()");

                if(!true) {
                  //fuses = swimConfig.memoryOptionBytes.defaultValues;
                    if( !swim.writeFuses(fuses) ) throw XCom.newException("ERR: swim.writeFuses()");
                    fuses = swim.readFuses();
                    if(fuses == null) throw XCom.newException("ERR: swim.readFuses()");
                }

                SysUtil.stdDbg().print("Fuses                 = ");
                for(final int f : fuses) {
                    if(f < 0) SysUtil.stdDbg().printf("NA "     );
                    else      SysUtil.stdDbg().printf("%02X ", f);
                }
                SysUtil.stdDbg().println("\n");
                //*/

                //*
                long lbs = swim.readLockBits();
                if(lbs < 0) throw XCom.newException("ERR: swim.readLockBits()");

                if(!true) {
                    if( !swim.writeLockBits(lbs) ) throw XCom.newException("ERR: swim.writeLockBits()");
                    lbs = swim.readLockBits();
                    if(lbs < 0) throw XCom.newException("ERR: swim.readLockBits()");
                }

                SysUtil.stdDbg().print("Lock Bits             = ");
                SysUtil.stdDbg().printf("%02X\n", lbs);
                SysUtil.stdDbg().println();
                //*/

/*
git clone https://github.com/vdudouyt/stm8flash.git
cd stm8flash
make

../0_excluded_directory/temporary/SWIM-SWD/stm8flash -c stlinkv2 -p stm8s003f3 -u
../0_excluded_directory/temporary/SWIM-SWD/stm8flash -c stlinkv2 -p stm8s103f3 -u
../0_excluded_directory/temporary/SWIM-SWD/stm8flash -c stlinkv2 -p stm8l151k4 -u


-----

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink-dap.cfg -c "transport select swim" -c "adapter speed 1000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm8s003.cfg -c "init" -c "halt" -c "wait_halt" -c "load_image PTest2X.Blink-STM8S003F3P6.cpp.hex 0x0000 ihex 0x8000" -c "sleep 10" -c "reset run" -c "sleep 10" -c "shutdown"

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink-dap.cfg -c "transport select swim" -c "adapter speed 1000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm8s103.cfg -c "init" -c "halt" -c "wait_halt" -c "load_image PTest2X.Blink-STM8S103F3P6.cpp.hex 0x0000 ihex 0x8000" -c "sleep 10" -c "reset run" -c "sleep 10" -c "shutdown"

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink-dap.cfg -c "transport select swim" -c "adapter speed 1000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm8l.cfg -c "init" -c "halt" -c "wait_halt" -c "load_image PTest2X.Blink-STM8L151K4T6.cpp.hex 0x0000 ihex 0x8000" -c "sleep 10" -c "reset run" -c "sleep 10" -c "shutdown"
*/

                /*
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println("Erasing Chip");
                final long te1 = SysUtil.getNS();
                if( !swim.chipErase() ) throw XCom.newException("ERR: swim.chipErase()");
                final long te2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", swimConfig.memoryFlash.totalSize, (te2 - te1) * 0.000000001, 1000000000.0 * swimConfig.memoryFlash.totalSize / (te2 - te1) );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( swim._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !swim.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: swim.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();

                int[] chkFuses = swim.readFuses();
                if(chkFuses == null) throw XCom.newException("ERR: swim.readFuses()");
                SysUtil.stdDbg().print("Fuses                 = ");
                for(final int f : chkFuses) {
                    if(f < 0) SysUtil.stdDbg().printf("NA "     );
                    else      SysUtil.stdDbg().printf("%02X ", f);
                }
                SysUtil.stdDbg().println("\n");
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = swim.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: swim.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: swim.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], swim.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 16, swimConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !swim.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: swim.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : swim.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( swim.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, swimConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = swim.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: swim.readEEPROM()");

                            if( !swim.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: swim.writeEEPROM()");

                            final int v1 = swim.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: swim.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                } // if
                //*/

                if( !swim.end() ) throw XCom.newException("ERR: swim.end()");

                usb2gpio.shutdown();

            } // if

            // Test 'USB2GPIO' and 'ProgSWD' - STM32F103CBT6/STM32F411CEU6/STM32G070RBT6/STM32G431CBT6/STM32H750VBT6/STM32L011D4P6/STM32L151C8T6/STM32L431C8T6/STM32WLE5JC/RP2040/NRF51822/NRF52840/ATSAMD21G18A/ATSAM3X8E/R7FA4M1AB3CFM#AA0

            if(!true) {

                ARMCortexMThumb.setARMObjDumpBinary( SysUtil.getUHD() + "/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/bin/arm-none-eabi-objdump" );

                final int STM32F103   =  0;
                final int STM32F411   =  1;
                final int STM32G070   =  2;
                final int STM32G431   =  3;
                final int STM32H750   =  4;
                final int STM32H750XF =  5;
                final int STM32L011   =  6;
                final int STM32L151   =  7;
                final int STM32L431   =  8;
                final int STM32WLE5   =  9;
                final int RP2040      = 10;
                final int RP2350      = 11;
                final int NRF51822    = 12;
                final int NRF52840    = 13;
                final int ATSAMD21    = 14;
                final int ATSAM3X8    = 15;
                final int R7FA4M1     = 16;

                final int SelTarget = STM32F103;
                                     /*
                                       0 - STM32F103
                                       1 - STM32F411
                                       2 - STM32G070
                                       3 - STM32G431
                                       4 - STM32H750
                                       5 - STM32H750XF (external flash)
                                       6 - STM32L011
                                       7 - STM32L151
                                       8 - STM32L431
                                       9 - STM32WLE5JC
                                      10 - RP2040
                                      11 - RP2350
                                      12 - NRF51822
                                      13 - NRF52840
                                      14 - ATSAMD21
                                      15 - ATSAM3X8
                                      16 - R7FA4M1
                                     */

                int fuseHexLen  = 2;

                switch(SelTarget) {

                    // ##### !!! TODO : Move them as #include "config.inc/SWD.*.java.inc" !!! #####

                    case STM32F103:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 20480;

                        swdConfig.memoryFlash.driverName                    = "stm32f1x";
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 65536;
                        swdConfig.memoryFlash.pageSize                      =  1024;
                        swdConfig.memoryFlash.numPages                      =    64;
                        break;

                    case STM32F411:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 131072;

                        swdConfig.memoryFlash.driverName                    = "stm32f4x";
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 262144;
                        swdConfig.memoryFlash.pageSize                      =   2048;
                        swdConfig.memoryFlash.numPages                      =    128;

                        fuseHexLen                                          = 8;
                        break;

                    case STM32G070:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 36864;

                        swdConfig.memoryFlash.driverName                    = "stm32g0x0";
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 131072;
                        swdConfig.memoryFlash.pageSize                      =   2048;
                        swdConfig.memoryFlash.numPages                      =     64;

                        fuseHexLen                                          = 8;
                        break;

                    case STM32G431:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 131072;

                        swdConfig.memoryFlash.driverName                    = "stm32g4x";
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 131072;
                        swdConfig.memoryFlash.pageSize                      =   2048;
                        swdConfig.memoryFlash.numPages                      =     64;

                        fuseHexLen                                          = 8;
                        break;

                    case STM32H750:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 524288;

                        swdConfig.memoryFlash.driverName                    = "stm32h75x_bank0";
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 131072;
                        swdConfig.memoryFlash.pageSize                      =   2048;
                        swdConfig.memoryFlash.numPages                      =     64;

                        fuseHexLen                                          = 8;
                        break;

                    case STM32H750XF:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 524288;

                        swdConfig.memoryFlash.driverName                    = "stm32h750_w25q";
                        swdConfig.memoryFlash.address                       = 0x90000000L;
                        swdConfig.memoryFlash.totalSize                     = 8388608;
                        swdConfig.memoryFlash.pageSize                      =     256;
                        swdConfig.memoryFlash.numPages                      =   32768;
                        break;

                    case STM32L011:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 2048;

                        swdConfig.memoryFlash.driverName                    = "stm32l0x";
                        swdConfig.memoryFlash.wrHalfPage                    = true;
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 16384;
                        swdConfig.memoryFlash.pageSize                      =   128;
                        swdConfig.memoryFlash.numPages                      =   128;

                        swdConfig.memoryEEPROM.address                      = 0x08080000L;
                        swdConfig.memoryEEPROM.totalSize                    = 512;
                        swdConfig.memoryEEPROM.pageSize                     =   4 * 8; // Increase the size for better performance
                        swdConfig.memoryEEPROM.numPages                     = 128 / 8;

                        fuseHexLen                                          = 4;
                        break;

                    case STM32L151:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 10240;

                        swdConfig.memoryFlash.driverName                    = "stm32l1x";
                        swdConfig.memoryFlash.wrHalfPage                    = true;
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 65536;
                        swdConfig.memoryFlash.pageSize                      =   256;
                        swdConfig.memoryFlash.numPages                      =   256;

                        swdConfig.memoryEEPROM.address                      = 0x08080000L;
                        swdConfig.memoryEEPROM.totalSize                    = 4096;
                        swdConfig.memoryEEPROM.pageSize                     =    4 * 8; // Increase the size for better performance
                        swdConfig.memoryEEPROM.numPages                     = 1024 / 8;

                        // ##### !!! TODO : Does this MCU really need a lower SWD clock frequency? !!! #####
                        swdClockFrequency                                   = ProgSWD.SWDClockFrequency._4MHz;

                        fuseHexLen                                          = 4;
                        break;

                    case STM32L431:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 65536;
                        swdConfig.memorySRAM.memBankSpec.numBanks           = 2;
                                                                                                                      // Original = 0x10000000
                        swdConfig.memorySRAM.memBankSpec.address            = new long[] { swdConfig.memorySRAM.address, swdConfig.memorySRAM.address + 0xC000 };
                        swdConfig.memorySRAM.memBankSpec.size               = new int [] { 49152                       , 16384                                 };
                        swdConfig.memorySRAM.memBankSpec.contiguousAddress  = swdConfig.memorySRAM.address;

                        swdConfig.memoryFlash.driverName                    = "stm32l4x";
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 65536;
                        swdConfig.memoryFlash.pageSize                      =  2048;
                        swdConfig.memoryFlash.numPages                      =    32;

                        fuseHexLen                                          = 8;
                        break;

                    case STM32WLE5:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 65536 / 2;

                        // ##### ??? TODO : Is it better to always use SRAM2 because some devices do not have SRAM1 ??? #####
                        swdConfig.memorySRAM.address                        = 0x20008000L;
                        swdConfig.memorySRAM.totalSize                      = 65536 / 2;

                        swdConfig.memoryFlash.driverName                    = "stm32wlex";
                        swdConfig.memoryFlash.address                       = 0x08000000L;
                        swdConfig.memoryFlash.totalSize                     = 262144;
                        swdConfig.memoryFlash.pageSize                      =   2048;
                        swdConfig.memoryFlash.numPages                      =    128;

                        fuseHexLen                                          = 8;
                        break;

                    case RP2040:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 270336;

                        swdConfig.memoryFlash.driverName                    = "rp2040";
                        swdConfig.memoryFlash.address                       = 0x10000000L;
                        swdConfig.memoryFlash.totalSize                     = 2097152    ;
                        swdConfig.memoryFlash.pageSize                      =    256 * 16; // Use 4KB sector erase (0x1000)
                        swdConfig.memoryFlash.numPages                      =   8192 / 16;
                        break;

                    case RP2350:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 532480;

                        swdConfig.memoryFlash.driverName                    = "rp2350";
                        swdConfig.memoryFlash.address                       = 0x10000000L;
                        swdConfig.memoryFlash.totalSize                     = 4194304    ;
                        swdConfig.memoryFlash.pageSize                      =    256 * 16; // Use 4KB sector erase (0x1000)
                        swdConfig.memoryFlash.numPages                      =  16384 / 16;

                    if(bootloaderMode) {
                        swdConfig.memoryFlash.driverName                    = "rp2350_blm";
                        swdClockFrequency                                   = ProgSWD.SWDClockFrequency._250kHz;
                    }
                        break;

                    case NRF51822:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 16384;
                        swdConfig.memorySRAM.memBankSpec.numBanks           = 2;

                        swdConfig.memorySRAM.memBankSpec.address            = new long[] { swdConfig.memorySRAM.address, swdConfig.memorySRAM.address + 0x2000 };
                        swdConfig.memorySRAM.memBankSpec.size               = new int [] { 8192                        , 8192                                  };
                        swdConfig.memorySRAM.memBankSpec.contiguousAddress  = swdConfig.memorySRAM.address;

                        swdConfig.memoryFlash.driverName                    = "nrf51";
                        swdConfig.memoryFlash.address                       = 0x00000000L;
                        swdConfig.memoryFlash.totalSize                     = 262144;
                        swdConfig.memoryFlash.pageSize                      =   1024;
                        swdConfig.memoryFlash.numPages                      =    256;
                    if(!true) {
                        // NOTE : # For testing only!
                        //        # Do not set these if you want to also erase UICR because UICR can only be erased by ERASEALL (full chip erase)!
                        swdConfig.memoryFlash.partEraseAddressBeg           = 2048;
                        swdConfig.memoryFlash.partEraseSize                 = 262144 - 2048;
                    }

                        fuseHexLen                                          = 8;
                        break;

                    case NRF52840:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 262144;

                        swdConfig.memoryFlash.driverName                    = "nrf52";
                        swdConfig.memoryFlash.address                       = 0x00000000L;
                        swdConfig.memoryFlash.totalSize                     = 1048576;
                        swdConfig.memoryFlash.pageSize                      =    4096;
                        swdConfig.memoryFlash.numPages                      =     256;
                        /*
                         * Erase the application program only (preserve the MBR, SoftDevice, application data, bootloader,
                         * MBR parameter storage, and bootloader settings)
                         *
                         * https://infocenter.nordicsemi.com/index.jsp?topic=%2Fsdk_nrf5_v17.0.2%2Flib_bootloader.html
                         */
                        swdConfig.memoryFlash.partEraseAddressBeg           = 0x00027000L;
                        swdConfig.memoryFlash.partEraseSize                 = 0x000ED000L - 0x00027000L;

                        fuseHexLen                                          = 8;
                        break;

                    case ATSAMD21:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 32768;

                        swdConfig.memoryFlash.driverName                    = "atsamdx";
                        swdConfig.memoryFlash.address                       = 0x00000000L;
                        swdConfig.memoryFlash.totalSize                     = 262144;
                        swdConfig.memoryFlash.pageSize                      =     64;
                        swdConfig.memoryFlash.numPages                      =   4096;
                        /*
                         * Erase the application program only (preserve the bootloader)
                         *
                         * https://learn.adafruit.com/programming-microcontrollers-using-openocd-on-raspberry-pi/wiring-and-test
                         * https://forum.seeedstudio.com/t/how-to-unbrick-a-dead-xiao-using-st-link-and-openocd/255562
                         */
                        swdConfig.memoryFlash.partEraseAddressBeg           = 0x00002000L;
                        swdConfig.memoryFlash.partEraseSize                 = 0x00040000L - 0x00002000L;

                        fuseHexLen                                          = 4;
                        break;

                    case ATSAM3X8:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 98304;
                        swdConfig.memorySRAM.memBankSpec.numBanks           = 2;
                        swdConfig.memorySRAM.memBankSpec.address            = new long[] { swdConfig.memorySRAM.address, swdConfig.memorySRAM.address + 0x080000 };
                        swdConfig.memorySRAM.memBankSpec.size               = new int [] { 65536                       , 32768                                   };
                        swdConfig.memorySRAM.memBankSpec.contiguousAddress  = 0x20070000L;

                        swdConfig.memoryFlash.driverName                    = "atsam3x";
                        swdConfig.memoryFlash.address                       = 0x000080000L;
                        swdConfig.memoryFlash.totalSize                     = 524288;
                        swdConfig.memoryFlash.pageSize                      =    256;
                        swdConfig.memoryFlash.numPages                      =   2048;
                        swdConfig.memoryFlash.memBankSpec.numBanks          = 2;
                        swdConfig.memoryFlash.memBankSpec.address           = new long[] { 0x000080000L, 0x0000C0000L, };
                        swdConfig.memoryFlash.memBankSpec.size              = new int [] { 262144      , 262144        };
                        swdConfig.memoryFlash.memBankSpec.contiguousAddress = 0x000080000L;

                        fuseHexLen                                          = 4;
                        break;

                    case R7FA4M1:
                        swdConfig.memorySRAM.address                        = ProgSWD.DefaultCortexM_SRAMStart;
                        swdConfig.memorySRAM.totalSize                      = 32768;

                        swdConfig.memoryFlash.driverName                    = "renesas_ra4m1";
                        swdConfig.memoryFlash.address                       = 0x00000000L;
                        swdConfig.memoryFlash.totalSize                     = 262144;
                        swdConfig.memoryFlash.pageSize                      =   2048;
                        swdConfig.memoryFlash.numPages                      =    128;
                    if(true) {
                        // Erase the application program only (preserve the bootloader)
                        swdConfig.memoryFlash.partEraseAddressBeg           = 0x00004000L;
                        swdConfig.memoryFlash.partEraseSize                 = 0x00040000L - 0x00004000L;
                    }
                    else {
                        // NOTE : For testing only
                        swdConfig.memoryFlash.partEraseAddressBeg           = swdConfig.memoryFlash.totalSize - swdConfig.memoryFlash.pageSize * 1;
                        swdConfig.memoryFlash.partEraseSize                 = swdConfig.memoryFlash.pageSize;
                    }

                        swdConfig.memoryEEPROM.address                      = 0x40100000L;
                        swdConfig.memoryEEPROM.totalSize                    = 8192;
                        swdConfig.memoryEEPROM.pageSize                     = 1024;
                        swdConfig.memoryEEPROM.numPages                     =    8;

                        fuseHexLen                                          = 8;

                        // ##### !!! TODO : Does this MCU really need a lower SWD clock frequency? !!! #####
                        swdClockFrequency                                   = ProgSWD.SWDClockFrequency._4MHz;
                        break;

                } // switch

                swd = new ProgSWD(usb2gpio, swdConfig);

                //*
                // ##### !!! TODO !!!                                                                                                     #####
                // ##### Why the RP2040 firmware built using JxMake works if uploaded using OpenOCD but does not work if uploaded here??? #####
                final String   rp2040Hex = true
                                         ? "../src/1-TestData/PTest2X.WiFiBlink-RP2040-SDK150-CMake.cpp.hex"   // Built using Pico SDK and CMake
                                         : "../src/1-TestData/PTest2X.WiFiBlink-RP2040-SDK150-JxMake.cpp.hex"; // Built using Pico SDK and JxMake
                final String   rp2350Hex = "../src/1-TestData/PTest2X.Blink-RP2350-SDK200-CMake.m33.c.hex";    // Built using Pico SDK and CMake
                final String[] hexFiles  = new String[] {
                                               "../src/1-TestData/PTest2X.Blink-STM32F103C8T6.cpp.hex", /*  0 */
                                               "../src/1-TestData/PTest2X.Blink-STM32F411CEU6.cpp.hex", /*  1 */
                                               "../src/1-TestData/PTest2X.Blink-STM32G070RBT6.ino.hex", /*  2 */
                                               "../src/1-TestData/PTest2X.Blink-STM32G431CBT6.ino.hex", /*  3 */
                                               "../src/1-TestData/PTest2X.Blink-STM32H750VBT6.cpp.hex", /*  4 */
                                               "../src/1-TestData/PTest2X.Blink-STM32H750VBT6.exf.hex", /*  5 */
                                               "../src/1-TestData/PTest2X.Blink-STM32L011D4P6.ino.hex", /*  6 */
                                               "../src/1-TestData/PTest2X.Blink-STM32L151C8T6.c.hex"  , /*  7 */
                                               "../src/1-TestData/PTest2X.Blink-STM32L431C8T6.ino.hex", /*  8 */
                                               "../src/1-TestData/PTest2X.Blink-STM32WLE5JC.ino.hex"  , /*  9 */
                                               rp2040Hex                                              , /* 10 */
                                               rp2350Hex                                              , /* 11 */
                                               "../src/1-TestData/PTest2X.Blink-NRF51822.cpp.hex"     , /* 12 */
                                               "../src/1-TestData/PTest2X.Blink-NRF52840.cpp.hex"     , /* 13 */
                                               "../src/1-TestData/PTest2X.Blink-ATSAMD21G18A.cpp.hex" , /* 14 */
                                               "../src/1-TestData/PTest2X.Blink-ATSAM3X8E.cpp.hex"    , /* 15 */
                                               "../src/1-TestData/PTest2X.Blink-UNO_R4_Minima.ino.hex"  /* 16 */
                                           };

                final String   hex       = hexFiles[SelTarget];
                fwc.loadIntelHexFile(hex);

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( swd._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;

                int i = 0;
                int z = 0;
                SysUtil.stdDbg().printf("Blk# Address  Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %08X %04X (%d)\n", i++, f.startAddress(), f.length(), f.length() );
                    z += f.length();
                }
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().printf ("[Σ∊] %08X %04X (%d)\n", fwStartAddress, z       , z       );
                SysUtil.stdDbg().printf ("[Σ∪] %08X %04X (%d)\n", fwStartAddress, fwLength, fwLength);
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println();
                //*/

                /*
                 * RP2040 - Core 0    : 0x01002927L     RP2350 - Core 0 : 0x00040927L ; Mem-AP offset : 0x00002000L
                 *          Core 1    : 0x11002927L              Core 1 : 0x00040927L ; Mem-AP offset : 0x00004000L
                 *          Rescue DP : 0xF1002927L              RP-AP  : 0x00040927L ; Mem-AP offset : 0x00080000L
                 */
                final long[][] multidropIDs = new long[][] {
                                                  null                                    ,  //  0 - STM32F103CBT6
                                                  null                                    ,  //  1 - STM32F411CEU6
                                                  null                                    ,  //  2 - STM32G070RBT6
                                                  null                                    ,  //  3 - STM32G431CBT6
                                                  null                                    ,  //  4 - STM32H750VBT6
                                                  null                                    ,  //  5 - STM32H750VBT6 - external flash
                                                  null                                    ,  //  6 - STM32L011D4P6
                                                  null                                    ,  //  7 - STM32L151C8T6
                                                  null                                    ,  //  8 - STM32L431C8T6
                                                  null                                    ,  //  9 - STM32WLE5JC
                                                  new long[] {  0x01002927L, 0x11002927L  }, // 10 - RP2040
                                                  new long[] { -0x00040927L, 0x00002000L,    // 11 - RP2350 (use negative numbers to indicate that it uses Mem-AP offsets)
                                                               -0x00040927L, 0x00004000L  }, // ---
                                                  //*/
                                                  null                                    ,  // 12 - NRF51822
                                                  null                                    ,  // 13 - NRF52840
                                                  null                                    ,  // 14 - ATSAMD21G18A
                                                  null                                    ,  // 15 - ATSAM3X8E
                                                  null                                       // 16 - R7FA4M1AB3CFM#AA0
                                              };

                if( !swd.begin( multidropIDs[SelTarget], 0, allowBB, swdClockFrequency ) ) throw XCom.newException("ERR: swd.begin()");

                //*
                if( !swd.readSignature() ) throw XCom.newException("ERR: swd.readSignature()");

                final int[][] sigBytesA = new int[][] {
                                              //                  Original                               Clone (ignore)
                                              !true ? new int[] { 0x1B, 0xA0, 0x14, 0x77 } : new int[] { -1, -1, -1, -1 }, //  0 - STM32F103CBT6 (Cortex-M3          )
                                                      new int[] { 0x2B, 0xA0, 0x14, 0x77 }                               , //  1 - STM32F411CEU6 (Cortex-M4  with FPU)
                                                      new int[] { 0x0B, 0xC1, 0x14, 0x77 }                               , //  2 - STM32G070RBT6 (Cortex-M0+         )
                                                      new int[] { 0x2B, 0xA0, 0x14, 0x77 }                               , //  3 - STM32G431CBT6 (Cortex-M4  with FPU)
                                                      new int[] { 0x6B, 0xA0, 0x24, 0x77 }                               , //  4 - STM32H750VBT6 (Cortex-M7  with FPU)
                                                      new int[] { 0x6B, 0xA0, 0x24, 0x77 }                               , //  5 - STM32H750VBT6 (Cortex-M7  with FPU) - external flash
                                                      new int[] { 0x0B, 0xC1, 0x14, 0x77 }                               , //  6 - STM32L011D4P6 (Cortex-M0+         )
                                                      new int[] { 0x2B, 0xA0, 0x14, 0x77 }                               , //  7 - STM32L151C8T6 (Cortex-M3          )
                                                      new int[] { 0x2B, 0xA0, 0x14, 0x77 }                               , //  8 - STM32L431C8T6 (Cortex-M4  with FPU)
                                                      new int[] { 0x6B, 0xA0, 0x24, 0x77 }                               , //  9 - STM32WLE5JC   (Cortex-M4  with DSP)
                                                      new int[] { 0x0B, 0xC1, 0x24, 0x77 }                               , // 10 - RP2040        (Cortex-M0+         )
                                                      new int[] { 0x4C, 0x01, 0x34, 0x77 }                               , // 11 - RP2350        (Cortex-M33         )
                                                      new int[] { 0x0B, 0xB1, 0x14, 0x77 }                               , // 12 - NRF51822      (Cortex-M0          )
                                                      new int[] { 0x2B, 0xA0, 0x14, 0x77 }                               , // 13 - NRF52840      (Cortex-M4  with FPU)
                                                      new int[] { 0x0B, 0xC1, 0x14, 0x77 }                               , // 14 - ATSAMD21G18A  (Cortex-M0+         )
                                                      new int[] { 0x2B, 0xA0, 0x14, 0x77 }                               , // 15 - ATSAM3X8E     (Cortex-M3          )
                                                      new int[] { 0x5B, 0xA0, 0x24, 0x77 }                                 // 16 - R7FA4M1       (Cortex-M4  with FPU)
                                          };

                final int[]   sigBytes  = sigBytesA[SelTarget];

                if( !swd.verifySignature(sigBytes) ) throw XCom.newException("ERR: swd.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : swd.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");

                SysUtil.stdDbg().printf( "ARM CPUID             = %08X\n", swd.armCPUID() );
                SysUtil.stdDbg().println();
                //*/

                /*
                SysUtil.stdDbg().printf( "STM32 Device ID       = %03X\n", swd.stm32DeviceID() );
                SysUtil.stdDbg().println();
                //*/
                /*
                SysUtil.stdDbg().printf( "NRF5 Part Code        = %08X\n", swd.nrf5PartCode() );
                SysUtil.stdDbg().println();
                //*/
                /*
                SysUtil.stdDbg().printf( "SAMD Device ID        = %08X\n", swd.samdDeviceID() );
                SysUtil.stdDbg().println();
                //*/
                /*
                SysUtil.stdDbg().printf( "SAM3 Chip ID          = %08X\n", swd.sam3ChipID() );
                SysUtil.stdDbg().println();
                //*/
                /*
                SysUtil.stdDbg().printf( "Renesas FMIFRT        = %08X\n", swd.renFMIFRT() );
                SysUtil.stdDbg().println();
                //*/

                /*
                SysUtil.stdDbg().printf( "##### execCustomInstruction(\"nrf52_test\") : %d\n\n", swd.execCustomInstruction("nrf52_test") );
                SysUtil.systemExit();
                //*/

                if(true) {
                    long[] fuses = swd.readFuses();
                    if(fuses == null) throw XCom.newException("ERR: swd.readFuses()");

                    final String fmtFuseXX = (fuseHexLen == 8) ? "%08X "
                                           : (fuseHexLen == 4) ? "%04X "
                                           :                     "%02X ";

                    final int    fmtFuseLN = (fuseHexLen == 8) ?  8
                                           : (fuseHexLen == 4) ? 15
                                           :                     25;

                    SysUtil.stdDbg().print("Fuses     (before)    = ");
                    for(int f = 0; f < fuses.length; ++f) {
                        SysUtil.stdDbg().printf(fmtFuseXX, fuses[f]);
                        if( (fmtFuseLN != 0) &&  ( (f + 1) % fmtFuseLN == 0 ) ) SysUtil.stdDbg().printf("\n                        ");
                    }
                    SysUtil.stdDbg().println();

                    if(!true) {
                        if(!true) {
                            if(false                 ) java.util.Arrays.fill(fuses, 0xFF);
                            if(SelTarget == STM32F103) { ++fuses[1]; ++fuses[2];                } // Modify Data0            and Data1
                            if(SelTarget == STM32F411) { fuses[0] ^= 0x00000004L;               } // Modify BOR_LEV
                            if(SelTarget == STM32G070) { fuses[0] ^= 0x00400000L;               } // Modify RAM_PARITY_CHECK
                            if(SelTarget == STM32G431) { fuses[0] ^= 0x00000100L;               } // Modify BOR_LEV
                            if(SelTarget == STM32H750) { fuses[0] ^= 0x00000004L;               } // Modify BOR_LEV
                            if(SelTarget == STM32L011) { fuses[1] ^= 0x0001;                    } // Modify BOR_LEV
                            if(SelTarget == STM32L151) { fuses[1] ^= 0x0001;                    } // Modify BOR_LEV
                            if(SelTarget == STM32L431) { fuses[0] ^= 0x00000100L;               } // Modify BOR_LEV
                            if(SelTarget == STM32WLE5) { fuses[0] ^= 0x00000200L;               } // Modify BOR_LEV
                            if(SelTarget == NRF51822 ) { fuses[4] = fuses[35] = 0xA0A0A0A0L;    } // Modify Customer00       and Customer31 /* WARNING : On NRF51x, UICR can only be erased by ERASEALL (full chip erase)! */
                            if(SelTarget == NRF52840 ) { ++fuses[5]; ++fuses[36];               } // Modify Customer00       and Customer31
                            if(SelTarget == ATSAMD21 ) { fuses[2] ^= 0x0001;                    } // Modify BOD33.Level
                            if(SelTarget == ATSAM3X8 ) { fuses[0] = 1; fuses[2] ^= 0x00008000L; } // Modify boot selection   and flash bank 1 lock bits
                            if(SelTarget == R7FA4M1  ) { fuses[0] ^= 0x00000004L;               } // Modify IWDTTOPS
                        }
                        if( !swd.writeFuses(fuses) ) throw XCom.newException("ERR: swd.writeFuses()");
                        fuses = swd.readFuses();
                        if(fuses == null) throw XCom.newException("ERR: swd.readFuses()");
                    }

                    SysUtil.stdDbg().print("Fuses     (after )    = ");
                    for(int f = 0; f < fuses.length; ++f) {
                        SysUtil.stdDbg().printf(fmtFuseXX, fuses[f]);
                        if( (fmtFuseLN != 0) &&  ( (f + 1) % fmtFuseLN == 0 ) ) SysUtil.stdDbg().printf("\n                        ");
                    }
                    SysUtil.stdDbg().println("\n");
                }

                if(true) {
                    long lbs = swd.readLockBits();
                    if(lbs < 0) throw XCom.newException("ERR: swd.readLockBits()");

                    SysUtil.stdDbg().printf("Lock Bits (before)    = %02X\n", lbs);

                    if(!true) {
                        if(!true) {
                            if(SelTarget == STM32F103) lbs = 0xA5; // Unlock
                            if(SelTarget == STM32F411) lbs = 0xAA; // Unlock
                            if(SelTarget == STM32G070) lbs = 0xAA; // Unlock
                            if(SelTarget == STM32G431) lbs = 0xAA; // Unlock
                            if(SelTarget == STM32H750) lbs = 0xAA; // Unlock
                            if(SelTarget == STM32L011) lbs = 0xAA; // Unlock
                            if(SelTarget == STM32L151) lbs = 0xAA; // Unlock
                            if(SelTarget == STM32L431) lbs = 0xAA; // Unlock
                            if(SelTarget == STM32WLE5) lbs = 0xAA; // Unlock
                            if(SelTarget == NRF51822 ) lbs = 0x03; // Unlock
                            if(SelTarget == NRF52840 ) lbs = 0xFF; // Unlock   /* WARNING : APPROTECT.PALL can only be erased by using the CTRL-AP! */
                            if(SelTarget == ATSAMD21 ) { /* Not supported */ }
                            if(SelTarget == ATSAM3X8 ) lbs = 0x00; // Unlock   /* WARNING : SECURITY_BIT can only be erased by asserting the ERASE pin to high! */
                            if(SelTarget == R7FA4M1  ) { /* Not supported */ }
                        }
                        if( !swd.writeLockBits(lbs) ) throw XCom.newException("ERR: swd.writeLockBits()");
                        lbs = swd.readLockBits();
                        if(lbs < 0) throw XCom.newException("ERR: swd.readLockBits()");
                    }

                    SysUtil.stdDbg().printf("Lock Bits (after )    = %02X\n", lbs);
                    SysUtil.stdDbg().println();
                }

/*
~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "set CPUTAPID 0" -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32f1x.cfg -c "init" -c "stm32f1x unlock 0" -c "stm32f1x options_read 0" -c "reset" -c "exit"

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "set CPUTAPID 0" -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32f1x.cfg -c "program PTest2X.Blink-STM32F103C8T6.cpp.hex verify reset exit"

-----

~/0-JxMake/tools/rp2040openocd/bin/openocd -f ~/0-JxMake/tools/rp2040openocd/share/openocd/scripts/interface/cmsis-dap.cfg -c "transport select swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/rp2040openocd/share/openocd/scripts/target/rp2040.cfg -c "program PTest2X.WiFiBlink-RP2040-SDK150-CMake.cpp.hex verify reset exit"

~/0-JxMake/tools/rp2040openocd/bin/openocd -f ~/0-JxMake/tools/rp2040openocd/share/openocd/scripts/interface/cmsis-dap.cfg -c "transport select swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/rp2040openocd/share/openocd/scripts/target/rp2040.cfg -c "program PTest2X.WiFiBlink-RP2040-SDK150-JxMake.cpp.hex verify reset exit"

-----

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/nrf51.cfg -c "init" -c "halt" -c "nrf51 mass_erase" -c "program PTest2X.Blink-NRF51822.cpp.hex verify reset exit 0x00000000"

WARNING : NRF52 !!! UICR.REGOUT0 !!!
~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/nrf52.cfg -c "init" -c "halt" -c "program PTest2X.Blink-NRF52840.cpp.hex verify reset exit 0x00027000"

-----

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32g4x.cfg -c "program PTest2X.Blink-STM32G431CBT6.ino.hex verify reset exit"

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32g0x.cfg -c "program PTest2X.Blink-STM32G070RBT6.ino.hex verify reset exit"

-----

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32l0.cfg -c "program PTest2X.Blink-STM32L011D4P6.ino.hex verify reset exit"
~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32l0.cfg -c "init" -c "halt" -c "wait_halt" -c "stm32lx mass_erase 0" -c "sleep 200" -c "reset run" -c "shutdown"

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32l1.cfg -c "program PTest2X.Blink-STM32L151C8T6.cpp.hex verify reset exit"

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32l4x.cfg -c "program PTest2X.Blink-STM32L431C8T6.ino.hex verify reset exit"

-----

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32h7x.cfg -c "program PTest2X.Blink-STM32H750VBT6.cpp.hex verify reset exit"

-----

~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32wlx.cfg -c "init" -c "reset halt" -c "stm32l4x unlock 0" -c "exit"
~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32wlx.cfg -c "init" -c "reset halt" -c "stm32l4x mass_erase 0" -c "exit"
~/0-JxMake/tools/xpackopenocd/bin/openocd -f ~/0-JxMake/tools/xpackopenocd/scripts/interface/stlink.cfg -c "transport select hla_swd" -c "adapter speed 2000" -f ~/0-JxMake/tools/xpackopenocd/scripts/target/stm32wlx.cfg -c "program PTest2X.Blink-STM32WLE5JC.ino.hex verify reset exit"

-----

/opt/RFP_CLI_Linux_V31500_x64/rfp-cli -device ra -port /dev/ttyACM0 -p ../0_excluded_directory/personal/Firmware/Bootloaders/UNO_R4/dfu_minima.hex
*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                final long te1 = SysUtil.getNS();
                if( !swd.chipErase() ) throw XCom.newException("ERR: swd.chipErase()");
                final long te2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", swdConfig.memoryFlash.totalSize, (te2 - te1) * 0.000000001, 1000000000.0 * swdConfig.memoryFlash.totalSize / (te2 - te1) );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                final long tw1 = SysUtil.getNS();
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( swd._flashMemoryAlignWriteSize(fwLength) ) );
                if( !swd.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: swd.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/
                /*
                final byte[] wrDataBuff     = new byte[swdConfig.memoryFlash.pageSize];
                final int    wrFlashSize    = swdConfig.memoryFlash.pageSize;
                final int    wrStartAddress = 1 * (swdConfig.memoryFlash.totalSize - wrFlashSize * 1);
                java.util.Arrays.fill( wrDataBuff, (byte) 0xFF );
                wrDataBuff[0              ] = (byte) 0x11;
                wrDataBuff[1              ] = (byte) 0x22;
                wrDataBuff[2              ] = (byte) 0x33;
                wrDataBuff[3              ] = (byte) 0x44;
                wrDataBuff[4              ] = (byte) 0x55;
                wrDataBuff[5              ] = (byte) 0x66;
                wrDataBuff[6              ] = (byte) 0x77;
                wrDataBuff[7              ] = (byte) 0x88;
                wrDataBuff[wrFlashSize - 8] = (byte) 0x88;
                wrDataBuff[wrFlashSize - 7] = (byte) 0x77;
                wrDataBuff[wrFlashSize - 6] = (byte) 0x66;
                wrDataBuff[wrFlashSize - 5] = (byte) 0x55;
                wrDataBuff[wrFlashSize - 4] = (byte) 0x44;
                wrDataBuff[wrFlashSize - 3] = (byte) 0x33;
                wrDataBuff[wrFlashSize - 2] = (byte) 0x22;
                wrDataBuff[wrFlashSize - 1] = (byte) 0x11;
                if( !swd.writeFlash(wrDataBuff, wrStartAddress, wrFlashSize, null) ) throw XCom.newException("ERR: swd.writeFlash()");
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = swd.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: swd.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: swd.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], swd.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 4, swdConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !swd.readFlash(fwStartAddress, rdFlashSize, stdPP) ) throw XCom.newException("ERR: swd.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(true) {
                    int j = 0;
                    for(final int b : swd.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                        if(++j >= 64) {
                            j = 0;
                            SysUtil.stdDbg().println();
                        }
                    }
                }
                SysUtil.stdDbg().println();
                //*/
                /*
                final int rdFlashSize    = swdConfig.memoryFlash.pageSize;
              //final int rdStartAddress = 1 * ( 0x4000 + swdConfig.memoryFlash.pageSize * (30 + 1) );
                final int rdStartAddress = 1 * (swdConfig.memoryFlash.totalSize - rdFlashSize * 1);
                if( !swd.readFlash(rdStartAddress, rdFlashSize, null) ) throw XCom.newException("ERR: swd.readFlash()");
                for(final int b : swd.config().memoryFlash.readDataBuff) SysUtil.stdDbg().printf("%02X ", b);
                SysUtil.stdDbg().println();
                //*/

                /*
                if( swd.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, swdConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = swd.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: swd.readEEPROM()");

                            if( !swd.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: swd.writeEEPROM()");

                            final int v1 = swd.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: swd.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for
                } // if
                //*/

                /*
                if( swd.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM (with Multiple Commits)");

                    final int[] eAddr = new int[] { 0, swdConfig.memoryEEPROM.totalSize / 2 - 1, swdConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 3; ++eRep) {

                        for(final int ea : eAddr) {

                            final int v0 = swd.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: swd.readEEPROM()");

                            if( !swd.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: swd.writeEEPROM()");

                            final int v1 = swd.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: swd.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for

                        if( !swd.commitEEPROM() ) throw XCom.newException("ERR: swd.commitEEPROM()");

                    } // for
                } // if
                //*/

                if( !swd.end() ) throw XCom.newException("ERR: swd.end()");

            } // if

        } // try
        catch(final Exception e) {
            if(swim     != null) swim    .end();
            if(swd      != null) swd     .end();
            if(usb2gpio != null) usb2gpio.resetAndShutdown();
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class PTest2X

