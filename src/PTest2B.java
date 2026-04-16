/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * !!! WARNING: This file will not compile with Java versions earlier than 22 !!!
 */


import java.util.function.IntConsumer;

import jxm.*;
import jxm.tool.fwc.*;
import jxm.ugc.*;
import jxm.xb.*;


//
// Test class (the test application entry point)
//
public class PTest2B {

    public static void main(final String[] args)
    {
        // Instantiate the firmware composer
        final FWComposer fwc = new FWComposer();

        // Instantiate the programmer
              ProgBootUSBasp  .Config usbaspConfig   = new ProgBootUSBasp  .Config();
              ProgBootUSBasp          usbasp         = null;

              ProgBootLUFAHID .Config lufhidConfig   = new ProgBootLUFAHID .Config();
              ProgBootLUFAHID         lufhid         = null;

              ProgBootAVRDFU  .Config avrdfuConfig   = new ProgBootAVRDFU  .Config();
              ProgBootAVRDFU          avrdfu         = null;

              ProgBootSTM32DFU.Config s32dfuConfig   = new ProgBootSTM32DFU.Config();
              ProgBootSTM32DFU        s32dfu         = null;

        final IntConsumer stdPP = ProgressCB.getStdProgressPrinter( SysUtil.stdDbg() );

        try {

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            // Enable printing all exception stack trace on error if asked
            if( ( new ArgParser(args) ).enableAllExceptionStackTrace() ) XCom.setEnableAllExceptionStackTrace(true);

            // Test 'ProgBootUSBasp' - ATmega8A
            if(!true) {

                // ##### !!! TODO : Add more MCU tests !!! #####

                final int ATmega8A = 0;

                final int SelMCU   = ATmega8A;

                usbaspConfig = (SelMCU == ATmega8A) ? ProgBootUSBasp.Config.ATmega8A()
                             :                        null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(usbaspConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootUSBasp.Config.class) ) );
                SysUtil.systemExit();
                //*/

                usbasp = new ProgBootUSBasp(usbaspConfig);

                //*
                final String hex = (SelMCU == ATmega8A) ? "../src/1-TestData/PTest1B.Blink-ATmega8A.c.hex"
                                 :                        null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( usbasp._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !usbasp.begin() ) throw XCom.newException("ERR: usbasp.begin()");

                //*
                if( !usbasp.readSignature() ) throw XCom.newException("ERR: usbasp.readSignature()");

                final int[] sigBytes = (SelMCU == ATmega8A) ? new int[] { 0x1E, 0x93, 0x07 }
                                     :                        null;

                //if( !usbasp.verifySignature(sigBytes) ) throw XCom.newException("ERR: usbasp.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : usbasp.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !usbasp.chipErase() ) throw XCom.newException("ERR: usbasp.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( usbasp._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !usbasp.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: usbasp.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = usbasp.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: usbasp.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: usbasp.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], usbasp.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 4, usbaspConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !usbasp.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: usbasp.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : usbasp.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( usbasp.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, usbaspConfig.memoryEEPROM.totalSize / 2 - 1, usbaspConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = usbasp.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: usbasp.readEEPROM()");

                            if( !usbasp.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: usbasp.writeEEPROM()");

                            final int v1 = usbasp.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: usbasp.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                } // if
                //*/

                if( !usbasp.end() ) throw XCom.newException("ERR: usbasp.end()");

            } // if

            // Test 'ProgBootLUFAHID' - AT90USB162
            if(!true) {

/*
avrdude -c usbasp -p at90usb162 -U flash:w:../test/src/cpp_atmega/LUFA/LUFA-210130_BootloaderHID_AT90USB162_8MHz.hex:i
*/

                // ##### !!! TODO : Add more MCU tests !!! #####

                final int AT90USB162 = 0;

                final int SelMCU     = AT90USB162;

                lufhidConfig = (SelMCU == AT90USB162) ? ProgBootLUFAHID.Config.AT90USB162()
                             :                          null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(lufhidConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootLUFAHID.Config.class) ) );
                SysUtil.systemExit();
                //*/

                lufhid = new ProgBootLUFAHID(lufhidConfig);

                //*
                final String hex = (SelMCU == AT90USB162) ? "../src/1-TestData/PTest2B.Blink-AT90USB162.cpp.hex"
                                 :                          null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( lufhid._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;

              //java.util.Arrays.fill( fwDataBuff, (byte) 0xFF );
                //*/

                if( !lufhid.begin() ) throw XCom.newException("ERR: lufhid.begin()");

                /*
                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( lufhid._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !lufhid.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: lufhid.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                if( !lufhid.end() ) throw XCom.newException("ERR: lufhid.end()");

            } // if

            // Test 'ProgBootAVRDFU' - AT90USB162
            if(!true) {

/*
avrdude -c usbasp -p at90usb162 -U flash:w:../test/src/cpp_atmega/LUFA/LUFA-210130_BootloaderDFU_AT90USB162_8MHz.hex:i
avrdude -c usbasp -p at90usb162 -D -U flash:w:../src/1-TestData/PTest2B.Blink-AT90USB162.cpp.hex:i
*/

                // ##### !!! TODO : Add more MCU tests !!! #####

                final int AT90USB162 = 0;

                final int SelMCU     = AT90USB162;

                avrdfuConfig = (SelMCU == AT90USB162) ? ProgBootAVRDFU.Config.AT90USB162()
                             :                          null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(avrdfuConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootAVRDFU.Config.class) ) );
                SysUtil.systemExit();
                //*/

                avrdfu = new ProgBootAVRDFU(avrdfuConfig);

                //*
                final String hex = (SelMCU == AT90USB162) ? "../src/1-TestData/PTest2B.Blink-AT90USB162.cpp.hex"
                                 :                          null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( avrdfu._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                final int pidLow = (SelMCU == AT90USB162) ? 0xFA
                                 :                          0;

                if( !avrdfu.begin(-1, pidLow) ) throw XCom.newException("ERR: avrdfu.begin()");

                //*
                if( !avrdfu.readSignature() ) throw XCom.newException("ERR: avrdfu.readSignature()");

                final int[] sigBytes = (SelMCU == AT90USB162) ? new int[] { 0x1E, 0x94, 0x82 }
                                     :                          null;

                if( !avrdfu.verifySignature(sigBytes) ) throw XCom.newException("ERR: avrdfu.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : avrdfu.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !avrdfu.chipErase() ) throw XCom.newException("ERR: avrdfu.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( avrdfu._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !avrdfu.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: avrdfu.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = avrdfu.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: avrdfu.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: avrdfu.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], avrdfu.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.max(1024 * 4, avrdfuConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !avrdfu.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: avrdfu.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : avrdfu.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( avrdfu.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, avrdfuConfig.memoryEEPROM.totalSize / 2 - 1, avrdfuConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = avrdfu.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: avrdfu.readEEPROM()");

                            if( !avrdfu.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: avrdfu.writeEEPROM()");

                            final int v1 = avrdfu.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: avrdfu.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                } // if
                //*/

                if( !avrdfu.end() ) throw XCom.newException("ERR: avrdfu.end()");

            } // if

            // Test 'ProgBootSTM32DFU' - STM32F411/STM32G431/STM32H750
            if(!true) {

                // ##### !!! TODO : Add more MCU tests !!! #####

                final int STM32F411 = 0;
                final int STM32G431 = 1;
                final int STM32H750 = 2;

                final int SelMCU    = STM32F411;

                s32dfuConfig = (SelMCU == STM32F411) ? ProgBootSTM32DFU.Config.STM32_128k_2k()
                             : (SelMCU == STM32G431) ? ProgBootSTM32DFU.Config.STM32_128k_2k()
                             : (SelMCU == STM32H750) ? ProgBootSTM32DFU.Config.STM32_128k_2k()
                             :                         null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(s32dfuConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootSTM32DFU.Config.class) ) );
                SysUtil.systemExit();
                //*/

                s32dfu = new ProgBootSTM32DFU(s32dfuConfig);

                //*
                final String hex = (SelMCU == STM32F411) ? "../src/1-TestData/PTest2X.Blink-STM32F411CEU6.cpp.hex"
                                 : (SelMCU == STM32G431) ? "../src/1-TestData/PTest2X.Blink-STM32G431CBT6.ino.hex"
                                 : (SelMCU == STM32H750) ? "../src/1-TestData/PTest2X.Blink-STM32H750VBT6.cpp.hex"
                                 :                         null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( s32dfu._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !s32dfu.begin() ) throw XCom.newException("ERR: s32dfu.begin()");

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !s32dfu.chipErase() ) throw XCom.newException("ERR: s32dfu.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( s32dfu._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !s32dfu.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: s32dfu.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = s32dfu.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: s32dfu.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: s32dfu.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], s32dfu.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.max(1024 * 4, s32dfuConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !s32dfu.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: s32dfu.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : s32dfu.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                if( !s32dfu.end() ) throw XCom.newException("ERR: s32dfu.end()");

            } // if

        } // try
        catch(final Exception e) {
            if(usbasp != null) usbasp.end();
            if(lufhid != null) lufhid.end();
            if(avrdfu != null) avrdfu.end();
            if(s32dfu != null) s32dfu.end();
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class PTest2B
