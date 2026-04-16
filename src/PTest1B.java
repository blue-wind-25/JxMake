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

/*
// ##### !!! TODO : EXPERIMENT !!! #####
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import com.pty4j.*;

import java.util.Comparator;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
//*/


//
// Test class (the test application entry point)
//
public class PTest1B {

    public static void main(final String[] args)
    {
        /*
        final ArrayList<Field> vkFields = new ArrayList<>();

        for( final Field field : KeyEvent.class.getDeclaredFields() ) {
            if( field.getName().startsWith("VK_") && field.getType() == int.class ) vkFields.add(field);
        }

        vkFields.sort( Comparator.comparing(Field::getName) );

        for(final Field field : vkFields) {
            try {
                final int value = field.getInt(null);
                SysUtil.stdDbg().printf( "public static final int %-28s = KeyEvent.%-28s; // %5d (0x%04X) \n", field.getName(), field.getName(), value, value );
            }
            catch(final IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        //*/

        /*
        // ##### !!! TODO : EXPERIMENT !!! #####
        try {
            final PtyProcessBuilder pb = new PtyProcessBuilder();

            pb.setEnvironment( Map.of(
                "TERM"     , "xterm-256color",
                "LS_COLORS", "rs=0:di=38;5;27:ln=38;5;51:mh=44;38;5;15:pi=40;38;5;11:so=38;5;13:do=38;5;5:bd=48;5;232;38;5;11:cd=48;5;232;38;5;3:or=48;5;232;38;5;9:mi=05;48;5;232;38;5;15:su=48;5;196;38;5;15:sg=48;5;11;38;5;16:ca=48;5;196;38;5;226:tw=48;5;10;38;5;16:ow=48;5;10;38;5;21:st=48;5;21;38;5;15:ex=38;5;34:*.tar=38;5;9:*.tgz=38;5;9:*.arc=38;5;9:*.arj=38;5;9:*.taz=38;5;9:*.lha=38;5;9:*.lz4=38;5;9:*.lzh=38;5;9:*.lzma=38;5;9:*.tlz=38;5;9:*.txz=38;5;9:*.tzo=38;5;9:*.t7z=38;5;9:*.zip=38;5;9:*.z=38;5;9:*.Z=38;5;9:*.dz=38;5;9:*.gz=38;5;9:*.lrz=38;5;9:*.lz=38;5;9:*.lzo=38;5;9:*.xz=38;5;9:*.bz2=38;5;9:*.bz=38;5;9:*.tbz=38;5;9:*.tbz2=38;5;9:*.tz=38;5;9:*.deb=38;5;9:*.rpm=38;5;9:*.jar=38;5;9:*.war=38;5;9:*.ear=38;5;9:*.sar=38;5;9:*.rar=38;5;9:*.alz=38;5;9:*.ace=38;5;9:*.zoo=38;5;9:*.cpio=38;5;9:*.7z=38;5;9:*.rz=38;5;9:*.cab=38;5;9:*.jpg=38;5;13:*.jpeg=38;5;13:*.gif=38;5;13:*.bmp=38;5;13:*.pbm=38;5;13:*.pgm=38;5;13:*.ppm=38;5;13:*.tga=38;5;13:*.xbm=38;5;13:*.xpm=38;5;13:*.tif=38;5;13:*.tiff=38;5;13:*.png=38;5;13:*.svg=38;5;13:*.svgz=38;5;13:*.mng=38;5;13:*.pcx=38;5;13:*.mov=38;5;13:*.mpg=38;5;13:*.mpeg=38;5;13:*.m2v=38;5;13:*.mkv=38;5;13:*.webm=38;5;13:*.ogm=38;5;13:*.mp4=38;5;13:*.m4v=38;5;13:*.mp4v=38;5;13:*.vob=38;5;13:*.qt=38;5;13:*.nuv=38;5;13:*.wmv=38;5;13:*.asf=38;5;13:*.rm=38;5;13:*.rmvb=38;5;13:*.flc=38;5;13:*.avi=38;5;13:*.fli=38;5;13:*.flv=38;5;13:*.gl=38;5;13:*.dl=38;5;13:*.xcf=38;5;13:*.xwd=38;5;13:*.yuv=38;5;13:*.cgm=38;5;13:*.emf=38;5;13:*.axv=38;5;13:*.anx=38;5;13:*.ogv=38;5;13:*.ogx=38;5;13:*.aac=38;5;45:*.au=38;5;45:*.flac=38;5;45:*.mid=38;5;45:*.midi=38;5;45:*.mka=38;5;45:*.mp3=38;5;45:*.mpc=38;5;45:*.ogg=38;5;45:*.ra=38;5;45:*.wav=38;5;45:*.axa=38;5;45:*.oga=38;5;45:*.spx=38;5;45:*.xspf=38;5;45:"
            ) );
          //pb.setCommand    ( new String[] { "/bin/bash", "-c", "echo $TERM" } );
            pb.setCommand    ( new String[] { "/usr/bin/ls", "--color=auto", "-l" } );
            pb.setDirectory  ( SysUtil.getCWD() );

            final PtyProcess pc = pb.start();

            final InputStream       isStdOut   = pc.getInputStream();
            final InputStreamReader isrStdOut  = new InputStreamReader(isStdOut, SysUtil._CharEncoding);
            final BufferedReader    procStdOut = new BufferedReader(isrStdOut, 10 * 1024 * 1024);

            String line;
            while(true) {
                line = procStdOut.readLine();
                if(line == null) break;
                System.out.println(line);
            }

            final int res = pc.waitFor();

            procStdOut.close();
        }
        catch(final Exception e) {
            e.printStackTrace();
            SysUtil.systemExitError();
        }
        //*/

        // Instantiate the firmware composer
        final FWComposer fwc = new FWComposer();

        // Instantiate the programmer
              ProgBootAVR109.Config      avr109Config   = new ProgBootAVR109.Config();
              ProgBootAVR109             avr109         = null;

              ProgBootSTK500.Config      stk500Config   = new ProgBootSTK500.Config();
              ProgBootSTK500             stk500         = null;

              ProgBootSTK500v2.Config    stk500v2Config = new ProgBootSTK500v2.Config();
              ProgBootSTK500v2           stk500v2       = null;

              ProgBootChip45.Config      chip45Config   = new ProgBootChip45.Config();
              ProgBootChip45             chip45         = null;

              ProgBootTSB.Config         tsbConfig      = new ProgBootTSB.Config();
              ProgBootTSB                tsb            = null;

              ProgBootURCLOCK.Config     urclockConfig  = new ProgBootURCLOCK.Config();
              ProgBootURCLOCK            urclock        = null;

              ProgBootLUFAPrinter        luflpt         = null;

              ProgBootSTM32Serial.Config s32serConfig   = new ProgBootSTM32Serial.Config();
              ProgBootSTM32Serial        s32ser         = null;

              ProgBootOpenBLT.Config     obltConfig     = new ProgBootOpenBLT.Config();
              ProgBootOpenBLT            oblt           = null;

              ProgBootSAMBA.Config       sambaConfig    = new ProgBootSAMBA.Config();
              ProgBootSAMBA              samba          = null;

        final IntConsumer stdPP = ProgressCB.getStdProgressPrinter( SysUtil.stdDbg() );

        try {

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            // Enable printing all exception stack trace on error if asked
            if( ( new ArgParser(args) ).enableAllExceptionStackTrace() ) XCom.setEnableAllExceptionStackTrace(true);

            // Test 'ProgBootAVR109' - ATmega32U4/ATmega328P
            if(!true) {

/*
avrdude -c avr109 -P /dev/ttyACM0 -p m32u4 -D -U flash:w:PTest1B.VUARTLoopback-ATmega32U4.hex:i
*/

                final int ATmega32U4 = 0; // Caterina
                final int ATmega328P = 1; // XBoot

                final int SelMCU   = ATmega32U4;

                avr109Config = (SelMCU == ATmega32U4) ? ProgBootAVR109.Config.ATmega32U4()
                             : (SelMCU == ATmega328P) ? ProgBootAVR109.Config.ATmega328P()
                             :                          null;

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(avr109Config) ); SysUtil.systemExit();

                avr109 = new ProgBootAVR109(avr109Config);

                //*
                final String hex = (SelMCU == ATmega32U4) ? "../src/1-TestData/PTest1B.VUARTLoopback-ATmega32U4.hex"
                                 : (SelMCU == ATmega328P) ? "../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex"
                                 :                          null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( avr109._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !avr109.begin(
                      (SelMCU == ATmega32U4) ? "/dev/ttyACM0"
                    : (SelMCU == ATmega328P) ? "/dev/ttyUSB0"
                    :                          null
                    ,
                      (SelMCU == ATmega32U4) ? 0
                    : (SelMCU == ATmega328P) ? -57600
                    :                          0
                ) ) throw XCom.newException("ERR: avr109.begin()");

                //*
                if( !avr109.readSignature() ) throw XCom.newException("ERR: avr109.readSignature()");

                final int[] sigBytes = (SelMCU == ATmega32U4) ? new int[] { 0x1E, 0x95, 0x87 }
                                     : (SelMCU == ATmega328P) ? new int[] { 0x1E, 0x95, 0x0F }
                                     :                          null;

                if( !avr109.verifySignature(sigBytes) ) throw XCom.newException("ERR: avr109.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : avr109.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !avr109.chipErase() ) throw XCom.newException("ERR: avr109.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( avr109._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !avr109.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: avr109.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = avr109.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: avr109.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: avr109.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], avr109.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 16, avr109Config.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !avr109.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: avr109.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : avr109.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( avr109.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, avr109Config.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = avr109.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: avr109.readEEPROM()");

                            if( !avr109.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: avr109.writeEEPROM()");

                            final int v1 = avr109.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: avr109.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                } // if
                //*/

                if( !avr109.end() ) throw XCom.newException("ERR: avr109.end()");

            } // if

            // Test 'ProgBootSTK500' - ATmega328P - 8MHz
            if(!true) {

/*
avrdude -c arduino -P /dev/ttyUSB0 -b 57600 -p m328p -D -U flash:w:PTest1B.Blink-ATmega328P.ino.hex:i

curl http://10.0.0.111/console/baud?rate=57600
avrdude -c arduino -P net:10.0.0.111:2323 -b 57600 -p m328p -D -U flash:w:PTest1B.Blink-ATmega328P.ino.hex:i
*/

                /*
                stk500Config.memoryFlash.totalSize  = 32768;
                stk500Config.memoryFlash.pageSize   =   128;
                stk500Config.memoryFlash.numPages   =   256;

                stk500Config.memoryEEPROM.totalSize =  1024;
                */

                stk500Config = ProgBootSTK500.Config.ATmega328P();

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(stk500Config) ); SysUtil.systemExit();

                stk500 = new ProgBootSTK500(stk500Config);

                //*
                final String hex = "../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex";
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( stk500._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                final int    mode = 0;
                final String port = (mode == 0) ? "/dev/ttyUSB0"
                                  : (mode == 1) ? "jxm:/dev/ttyACM0:/dev/ttyACM1"
                                  : (mode == 2) ? "net:10.0.0.111:2323:console/baud?rate=%d"
                                  :               null;

                if( !stk500.begin(port, 57600) ) throw XCom.newException("ERR: stk500.begin()");

                //*
                if( !stk500.readSignature() ) throw XCom.newException("ERR: stk500.readSignature()");

                final int[] sigBytes = new int[] { 0x1E, 0x95, 0x0F };
                if( !stk500.verifySignature(sigBytes) ) throw XCom.newException("ERR: stk500.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : stk500.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !stk500.chipErase() ) throw XCom.newException("ERR: stk500.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( stk500._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !stk500.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: stk500.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = stk500.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: stk500.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: stk500.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], stk500.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 16, stk500Config.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !stk500.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: stk500.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : stk500.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( stk500.config().memoryEEPROM.totalSize > 0 ) {
                    // WARNING : Optiboot does not support EEPROM reading and writing!

                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, stk500Config.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = stk500.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: stk500.readEEPROM()");

                            if( !stk500.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: stk500.writeEEPROM()");

                            final int v1 = stk500.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: stk500.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                }
                //*/

                if( !stk500.end() ) throw XCom.newException("ERR: stk500.end()");

            } // if

            // Test 'ProgBootSTK500v2' - ATmega2560/ATmega328P
            if(!true) {

/*
avrdude -c wiring -P /dev/ttyACM0 -b 115200 -p atmega2560 -D -U flash:w:PTest1B.Blink-ATmega2560.ino.hex:i
*/

                /*
                stk500v2Config.memoryFlash.totalSize  = 262144;
                stk500v2Config.memoryFlash.pageSize   =    256;
                stk500v2Config.memoryFlash.numPages   =   1024;

                stk500v2Config.memoryEEPROM.totalSize =   4096;
                */

                final int ATmega2560 = 0; // Wiring
                final int ATmega328P = 1; // Joede

                final int SelMCU   = ATmega2560;

                stk500v2Config = (SelMCU == ATmega2560) ? ProgBootSTK500v2.Config.ATmega2560()
                               : (SelMCU == ATmega328P) ? ProgBootSTK500v2.Config.ATmega328P()
                               :                          null;

              //SysUtil.stdDbg().println( SerializableDeepClone.toJSON(stk500v2Config) ); SysUtil.systemExit();

                stk500v2 = new ProgBootSTK500v2(stk500v2Config);

                //*
                final String hex = (SelMCU == ATmega2560) ? "../src/1-TestData/PTest1B.Blink-ATmega2560.ino.hex"
                                 : (SelMCU == ATmega328P) ? "../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex"
                                 :                          null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( stk500v2._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !stk500v2.begin(
                      (SelMCU == ATmega2560) ? "/dev/ttyACM0"
                    : (SelMCU == ATmega328P) ? "/dev/ttyUSB0"
                    :                          null
                    ,
                      (SelMCU == ATmega2560) ? 115200
                    : (SelMCU == ATmega328P) ?  57600
                    :                          0
                ) ) throw XCom.newException("ERR: stk500v2.begin()");

                //*
                if( !stk500v2.readSignature() ) throw XCom.newException("ERR: stk500v2.readSignature()");

                final int[] sigBytes = (SelMCU == ATmega2560) ? new int[] { 0x1E, 0x98, 0x01 }
                                     : (SelMCU == ATmega328P) ? new int[] { 0x1E, 0x95, 0x0F }
                                     :                          null;

                if( !stk500v2.verifySignature(sigBytes) ) throw XCom.newException("ERR: stk500v2.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : stk500v2.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
            if(SelMCU != ATmega328P) {
                SysUtil.stdDbg().println("Erasing Chip");
                if( !stk500v2.chipErase() ) throw XCom.newException("ERR: stk500v2.chipErase()");
                SysUtil.stdDbg().println();
            }

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( stk500v2._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !stk500v2.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: stk500v2.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = stk500v2.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: stk500v2.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: stk500v2.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], stk500v2.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 1, stk500v2Config.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !stk500v2.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: stk500v2.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : stk500v2.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( stk500v2.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, 1, stk500v2Config.memoryEEPROM.totalSize - 2, stk500v2Config.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {

                        for(final int ea : eAddr) {

                            final int v0 = stk500v2.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: stk500v2.readEEPROM()");

                            if( !stk500v2.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: stk500v2.writeEEPROM()");

                            final int v1 = stk500v2.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: stk500v2.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for

                        if(eRep < 1) SysUtil.stdDbg().println("---------------");

                    } // for

                    SysUtil.stdDbg().println();
                } // if
                //*/

                if( !stk500v2.end() ) throw XCom.newException("ERR: stk500v2.end()");

            } // if

            // Test 'ProgBootChip45' - ATmega328P/ATmega2560
            if(!true) {

/*
avrdude -p atmega2560 -P usb -c usbasp -B 5 -e -U flash:w:chip45boot3_atmega2560_uart0_rs485_pb7_0x25.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0xFE:m
avrdude -p atmega2560 -P usb -c usbasp -B 5 -D -U flash:w:../src/1-TestData/PTest1B.Blink-ATmega2560.ino.hex:i
*/

                final int C45_ATmega328P          = 0;
                final int C45_ATmega328P_RS485    = 1;
                final int C45_ATmega2560          = 2;
                final int C45_ATmega2560_RS485    = 3;
                final int C45_ATmega2560_XTEA     = 4;
                final int C45_ATmega2560_XTEA_NRD = 5;

                final int C45_SelMCU              = C45_ATmega2560;

                chip45Config = (C45_SelMCU == C45_ATmega328P         ) ? ProgBootChip45.Config.ATmega328P()
                             : (C45_SelMCU == C45_ATmega328P_RS485   ) ? ProgBootChip45.Config.ATmega328P()
                             : (C45_SelMCU == C45_ATmega2560         ) ? ProgBootChip45.Config.ATmega2560()
                             : (C45_SelMCU == C45_ATmega2560_RS485   ) ? ProgBootChip45.Config.ATmega2560()
                             : (C45_SelMCU == C45_ATmega2560_XTEA    ) ? ProgBootChip45.Config.ATmega2560()
                             : (C45_SelMCU == C45_ATmega2560_XTEA_NRD) ? ProgBootChip45.Config.ATmega2560()
                             :                                           null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(chip45Config);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootChip45.Config.class) ) );
                SysUtil.systemExit();
                //*/

                final long[] xteaKey = new long[]{ 0xFEDCBA98L, 0x76543210L, 0x01234567L, 0x89ABCDEFL };
                /*
                final XTEA  xtea = new XTEA(xteaKey);
              //final int[] test = new int[] { 'T', 'e', 's', 't', 'T', 'e', 's', 't', 'T', 'e', 's', 't', 'T', 'e', 's', 't' };
                final int[] test = new int[] { 0x80, 0x04, 0x02, 0x01, 0x80, 0x40, 0x20, 0x10 };
                final int[] eres = xtea.encrypt(test);
                for(int i = 0; i < test.length; ++i) SysUtil.stdDbg().printf( "%02X ", eres[i] );
                SysUtil.stdDbg().println();
                // 5D FD 94 57 3A 30 BD 3D 5D FD 94 57 3A 30 BD 3D
                // 17 B5 C5 90 A4 AF 97 37
                SysUtil.systemExit();
                //*/

                chip45 = (C45_SelMCU == C45_ATmega328P         ) ? new ProgBootChip45B2(chip45Config, false)
                       : (C45_SelMCU == C45_ATmega328P_RS485   ) ? new ProgBootChip45B2(chip45Config, true )
                       : (C45_SelMCU == C45_ATmega2560         ) ? new ProgBootChip45B3(chip45Config, -1   )
                       : (C45_SelMCU == C45_ATmega2560_RS485   ) ? new ProgBootChip45B3(chip45Config, 0x25 )
                       : (C45_SelMCU == C45_ATmega2560_XTEA    ) ? new ProgBootChip45B3(chip45Config, -1   )
                       : (C45_SelMCU == C45_ATmega2560_XTEA_NRD) ? new ProgBootChip45B3(chip45Config, -1   )
                       :                                           null;

                //*
                final String hex = (C45_SelMCU == C45_ATmega328P         ) ? "../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex"
                                 : (C45_SelMCU == C45_ATmega328P_RS485   ) ? "../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex"
                                 : (C45_SelMCU == C45_ATmega2560         ) ? "../src/1-TestData/PTest1B.Blink-ATmega2560.ino.hex"
                                 : (C45_SelMCU == C45_ATmega2560_RS485   ) ? "../src/1-TestData/PTest1B.Blink-ATmega2560.ino.hex"
                                 : (C45_SelMCU == C45_ATmega2560_XTEA    ) ? "../src/1-TestData/PTest1B.Blink-ATmega2560.ino.hex"
                                 : (C45_SelMCU == C45_ATmega2560_XTEA_NRD) ? "../src/1-TestData/PTest1B.Blink-ATmega2560.ino.hex"
                                 :                                           null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( chip45._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !chip45.begin(
                      (C45_SelMCU == C45_ATmega328P         ) ? "/dev/ttyUSB0"
                    : (C45_SelMCU == C45_ATmega328P_RS485   ) ? "/dev/ttyUSB0"
                    : (C45_SelMCU == C45_ATmega2560         ) ? "/dev/ttyUSB0"
                    : (C45_SelMCU == C45_ATmega2560_RS485   ) ? "/dev/ttyUSB0"
                    : (C45_SelMCU == C45_ATmega2560_XTEA    ) ? "/dev/ttyUSB0"
                    : (C45_SelMCU == C45_ATmega2560_XTEA_NRD) ? "/dev/ttyUSB0"
                    :                                           null
                    ,
                      (C45_SelMCU == C45_ATmega328P         ) ? 38400
                    : (C45_SelMCU == C45_ATmega328P_RS485   ) ? 38400
                    : (C45_SelMCU == C45_ATmega2560         ) ? 57600
                    : (C45_SelMCU == C45_ATmega2560_RS485   ) ? 57600
                    : (C45_SelMCU == C45_ATmega2560_XTEA    ) ? 57600
                    : (C45_SelMCU == C45_ATmega2560_XTEA_NRD) ? 57600
                    :                                           19200
                ) ) throw XCom.newException("ERR: chip45.begin()");

                if(chip45 instanceof ProgBootChip45B3) {
                    if(C45_SelMCU == C45_ATmega2560_XTEA || C45_SelMCU == C45_ATmega2560_XTEA_NRD) {
                        ( (ProgBootChip45B3) chip45 ).enableXTEA(xteaKey);
                    }
                }

                /*
                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( chip45._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !chip45.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: chip45.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                // WARNING : # All  Chip45Boot2 implementations do not support flash reading!
                //           # Some Chip45Boot3 implementations do not support EEPROM reading and writing!
                final boolean noReadFlash = ( (chip45 instanceof ProgBootChip45B2)                                            ) ||
                                            ( (chip45 instanceof ProgBootChip45B3) && (C45_SelMCU == C45_ATmega2560_XTEA_NRD) );

                //*
                if(!noReadFlash) {
                    SysUtil.stdDbg().println("Verifying Flash");
                    SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                    final long tv1     = SysUtil.getNS();
                    final int  verBPos = chip45.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                    final long tv2     = SysUtil.getNS();
                    SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                    if(verBPos < 0) throw XCom.newException("ERR: chip45.verifyFlash()");
                    if(verBPos < fwLength) throw XCom.newException("ERR: chip45.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], chip45.config().memoryFlash.readDataBuff[verBPos]);
                    SysUtil.stdDbg().println();
                }
                //*/

                /*
                if(!noReadFlash) {
                    final int rdFlashSize = Math.min(1024 * 16, chip45Config.memoryFlash.totalSize);
                    SysUtil.stdDbg().println("Reading Flash");
                    SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                    final long tr1 = SysUtil.getNS();
                    if( !chip45.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: chip45.readFlash()");
                    final long tr2 = SysUtil.getNS();
                    SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                    if(!true) {
                        for(final int b : chip45.config().memoryFlash.readDataBuff) {
                            SysUtil.stdDbg().printf("%02X ", b);
                        }
                    }
                    SysUtil.stdDbg().println();
                }
                //*/

                /*
                if( chip45.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, chip45Config.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = chip45.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: chip45.readEEPROM()");

                            if( !!true && !chip45.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: chip45.writeEEPROM()");

                            final int v1 = chip45.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: chip45.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                }
                //*/

                if( !chip45.end() ) throw XCom.newException("ERR: chip45.end()");

            } // if

            // Test 'ProgBootTSB' - ATmega328P/ATtiny13
            if(!true) {

/*
avrdude -p atmega328p -P usb -c usbasp -B 5 -e -U flash:w:tsb20200727_atmega328p_uart0_8mhz_19200.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDE:m -U efuse:w:0x06:m
avrdude -p atmega328p -P usb -c usbasp -B 5 -D -U flash:w:../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex:i

avrdude -p t13 -P usb -c usbasp -B 5 -e -U flash:w:tsb20170626_attiny13_bitbang_pb3_pb4_autobaud.hex:i -U lfuse:w:0x7A:m -U hfuse:w:0xED:m
avrdude -p t13 -P usb -c usbasp -B 5 -D -U flash:w:../src/1-TestData/PTest1X.Blink-ATtiny13-Minimal.cpp.hex:i
*/

                final int TSB_ATmega328P = 0;
                final int TSB_ATtiny13   = 1;

                final int TSB_SelMCU     = TSB_ATtiny13;

                tsbConfig = (TSB_SelMCU == TSB_ATmega328P) ? ProgBootTSB.Config.ATmega328P()
                          : (TSB_SelMCU == TSB_ATtiny13  ) ? ProgBootTSB.Config.ATtiny13  ()
                          :                                  null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(tsbConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootTSB.Config.class) ) );
                SysUtil.systemExit();
                //*/

                tsb = new ProgBootTSB(tsbConfig);

                //*
                final String hex = (TSB_SelMCU == TSB_ATmega328P) ? "../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex"
                                 : (TSB_SelMCU == TSB_ATtiny13  ) ? "../src/1-TestData/PTest1X.Blink-ATtiny13-Minimal.cpp.hex"
                                 :                                  null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( tsb._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if(!!true) {
                    tsb.setPassword("PASSWORD", "PASSWORD");
                }

                if( !tsb.begin(
                      (TSB_SelMCU == TSB_ATmega328P) ? "/dev/ttyUSB0"
                    : (TSB_SelMCU == TSB_ATtiny13  ) ? "/dev/ttyUSB0"
                    :                                  null
                    ,
                      (TSB_SelMCU == TSB_ATmega328P) ? 19200
                    : (TSB_SelMCU == TSB_ATtiny13  ) ?  9600
                    :                                   9600
                ) ) throw XCom.newException("ERR: tsb.begin()");

                //*
                if( !tsb.readSignature() ) throw XCom.newException("ERR: tsb.readSignature()");

                final int[] sigBytes = (TSB_SelMCU == TSB_ATmega328P) ? new int[] { 0x1E, 0x95, 0x0F }
                                     : (TSB_SelMCU == TSB_ATtiny13  ) ? new int[] { 0x1E, 0x90, 0x07 }
                                     :                                      null;

                if( !tsb.verifySignature(sigBytes) ) throw XCom.newException("ERR: tsb.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : tsb.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( tsb._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !tsb.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: tsb.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                /*
                if( !tsb.readFlash(0, 256, stdPP) ) throw XCom.newException("ERR: tsb.readFlash()");
                for(int x = 0; x < 256; ++x) SysUtil.stdDbg().printf("%02X ", tsb.config().memoryFlash.readDataBuff[x]);
                SysUtil.stdDbg().println();

                if( !tsb.readFlash(128, 128, stdPP) ) throw XCom.newException("ERR: tsb.readFlash()");
                for(int x = 0; x < 128; ++x) SysUtil.stdDbg().printf("%02X ", tsb.config().memoryFlash.readDataBuff[x]);
                SysUtil.stdDbg().println();

                if( !tsb.readFlash(0, -1, stdPP) ) throw XCom.newException("ERR: tsb.readFlash()");
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = tsb.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: tsb.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: tsb.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], tsb.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
              //final int rdFlashSize = Math.min(1024 * 16, tsbConfig.memoryFlash.totalSize);
              //final int rdFlashSize = tsbConfig.memoryFlash.pageSize * 2;
                final int rdFlashSize = fwLength;
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !tsb.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: tsb.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : tsb.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/
                /*
                if(!!true) {
                    for(final byte b : fwDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b & 0xFF);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( tsb.config().memoryEEPROM.totalSize > 0 ) {
                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, tsbConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = tsb.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: tsb.readEEPROM()");

                            if( !!true && !tsb.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: tsb.writeEEPROM()");

                            final int v1 = tsb.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: tsb.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                }
                //*/

                if( !tsb.end() ) throw XCom.newException("ERR: tsb.end()");

            } // if

            // Test 'ProgBootURCLOCK' - ATmega8A[11.0592MHz] / ATmega328P[8MHz] / ATmega1284P[11.0592MHz]
            if(!true) {

/*
avrdude -c urclock -P /dev/ttyUSB0 -b 57600 -p m328p -D -U flash:w:PTest1B.Blink-ATmega328P.ino.hex:i

avrdude -c urclock -P /dev/ttyUSB0 -b 57600 -p m328p -x showall
ffffffffffff 2024-04-28 08.19 PTest1B.Blink-ATmega328P.ino.hex 1016 store 31328 meta 40 boot 384 u8.0 weU-jPr-c vector 25 (SPM_Ready) ATmega328P

---

avrdude -c urclock -P /dev/ttyUSB0 -b 57600 -p m328p -D -U flash:w:PTest1B.Blink-ATmega328P.ino.hex:i -xnometadata

avrdude -c urclock -P /dev/ttyUSB0 -b 57600 -p m328p -x showall
ffffffffffff 0000-00-00 00.00  application 0 store 0 meta 1 boot 384 u8.0 weU-jPr-c vector 25 (SPM_Ready) ATmega328P
*/

                final int UB_ATmega8A    = 0;
                final int UB_ATmega328P  = 1;
                final int UB_ATmega1284P = 2;

                final int UB_SelMCU      = UB_ATmega8A;

                urclockConfig = (UB_SelMCU == UB_ATmega8A   ) ? ProgBootURCLOCK.Config.ATmega8A   ()
                              : (UB_SelMCU == UB_ATmega328P ) ? ProgBootURCLOCK.Config.ATmega328P ()
                              : (UB_SelMCU == UB_ATmega1284P) ? ProgBootURCLOCK.Config.ATmega1284P()
                              :                                 null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(urclockConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootURCLOCK.Config.class) ) );
                SysUtil.systemExit();
                //*/

                urclock = new ProgBootURCLOCK(urclockConfig);

                //*
                final String hex = (UB_SelMCU == UB_ATmega8A   ) ? "../src/1-TestData/PTest1B.Blink-ATmega8A.c.hex"
                                 : (UB_SelMCU == UB_ATmega328P ) ? "../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex"
                                 : (UB_SelMCU == UB_ATmega1284P) ? "../hardware/JxMake_USB_GPIO_II/Firmware/Firmware-JxMakeHVA_II.hex"
                                 :                                 null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( urclock._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !urclock.begin(
                    "/dev/ttyUSB0",
                      (UB_SelMCU == UB_ATmega8A   ) ? 460800 // Reliable high-speed auto-baudrate is possible due to the 11.0592 MHz crystal
                    : (UB_SelMCU == UB_ATmega328P ) ?  57600 // Only lower baudrates are feasible due to the 8 MHz crystal
                    : (UB_SelMCU == UB_ATmega1284P) ? 460800 // Reliable high-speed auto-baudrate is possible due to the 11.0592 MHz crystal
                    :                                      0
                ) ) throw XCom.newException("ERR: urclock.begin()");

                //*
                if( !urclock.readSignature() ) throw XCom.newException("ERR: urclock.readSignature()");

                final int[] sigBytes = new int[] {    // MCU IDs as defined in avrdude.conf
                      (UB_SelMCU == UB_ATmega8A   ) ? 0x2D // ATmega8 (use the bootloader binary for ATmega8 instead of ATmega8A, as no downloadable precompiled version with LED support is available for the latter)
                    : (UB_SelMCU == UB_ATmega328P ) ? 0x77 // ATmega328P
                    : (UB_SelMCU == UB_ATmega1284P) ? 0x8D // ATmega1284P
                    :                                 0
                };

                if( !urclock.verifySignature(sigBytes) ) throw XCom.newException("ERR: urclock.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : urclock.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !urclock.chipErase() ) throw XCom.newException("ERR: urclock.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( urclock._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !urclock.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: urclock.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = urclock.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: urclock.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: urclock.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], urclock.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(1024 * 16, urclockConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !urclock.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: urclock.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : urclock.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                /*
                if( urclock.config().memoryEEPROM.totalSize > 0 ) {
                    // WARNING : Not all URBOOTs support EEPROM reading and writing!

                    SysUtil.stdDbg().println("Testing EEPROM");

                    final int[] eAddr = new int[] { 0, urclockConfig.memoryEEPROM.totalSize - 1 };

                    for(int eRep = 0; eRep < 2; ++eRep) {
                        for(final int ea : eAddr) {

                            final int v0 = urclock.readEEPROM(ea);
                            if(v0 < 0) throw XCom.newException("ERR: urclock.readEEPROM()");

                            if( !urclock.writeEEPROM( ea, (byte) (v0 + 1) ) ) throw XCom.newException("ERR: urclock.writeEEPROM()");

                            final int v1 = urclock.readEEPROM(ea);
                            if(v1 < 0) throw XCom.newException("ERR: urclock.readEEPROM()");

                            SysUtil.stdDbg().printf("[%04X] %02X -> %02X\n", ea, v0, v1);

                        } // for
                    } // for

                    SysUtil.stdDbg().println();
                }
                //*/

                if( !urclock.end() ) throw XCom.newException("ERR: urclock.end()");

            } // if

            // Test 'ProgBootLUFAPrinter' - AT90USB162
            if(!true) {

                // ##### !!! TODO : Add more MCU tests !!! #####

                final int AT90USB162 = 0;

                final int SelMCU     = AT90USB162;

                luflpt = new ProgBootLUFAPrinter();

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

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( luflpt._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;

              //java.util.Arrays.fill( fwDataBuff, (byte) 0xFF );
                //*/

                /*
                for(final String pn : ProgBootLUFAPrinter.listPrinters() ) {
                    SysUtil.stdDbg().printf("[%s]\n", pn);
                }
                //*/

                if( !luflpt.begin("LUFA_BootloaderLPT") ) throw XCom.newException("ERR: luflpt.begin()");

                /*
                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( luflpt._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !luflpt.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: luflpt.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                if( !luflpt.end() ) throw XCom.newException("ERR: luflpt.end()");

            } // if

            // Test 'ProgBootSTM32Serial' - STM32F103/STM32F411/STM32G431/STM32H750
            if(!true) {

                // ##### !!! TODO : Add more MCU tests !!! #####

                final int STM32F103 = 0;
                final int STM32F411 = 1;
                final int STM32G431 = 2;
                final int STM32H750 = 3;

                final int SelMCU    = STM32F103;

                s32serConfig = (SelMCU == STM32F103) ? ProgBootSTM32Serial.Config.STM32_64k_1k ()
                             : (SelMCU == STM32F411) ? ProgBootSTM32Serial.Config.STM32_128k_2k()
                             : (SelMCU == STM32G431) ? ProgBootSTM32Serial.Config.STM32_128k_2k()
                             : (SelMCU == STM32H750) ? ProgBootSTM32Serial.Config.STM32_128k_2k()
                             :                         null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(s32serConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootSTM32Serial.Config.class) ) );
                SysUtil.systemExit();
                //*/

                s32ser = new ProgBootSTM32Serial(s32serConfig);

                //*
                final String hex = (SelMCU == STM32F103) ? "../src/1-TestData/PTest2X.Blink-STM32F103C8T6.cpp.hex"
                                 : (SelMCU == STM32F411) ? "../src/1-TestData/PTest2X.Blink-STM32F411CEU6.cpp.hex"
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

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( s32ser._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !s32ser.begin("/dev/ttyUSB0", 57600, -1) ) throw XCom.newException("ERR: s32ser.begin()");

                //*
                if( !s32ser.readSignature() ) throw XCom.newException("ERR: s32ser.readSignature()");

                final int[] sigBytes = (SelMCU == STM32F103) ? new int[] { 0x04, 0x10 }
                                     : (SelMCU == STM32F411) ? new int[] { 0x04, 0x31 }
                                     : (SelMCU == STM32G431) ? new int[] { 0x04, 0x68 }
                                     : (SelMCU == STM32H750) ? new int[] { 0x04, 0x50 }
                                     :                         null;

                if( !s32ser.verifySignature(sigBytes) ) throw XCom.newException("ERR: s32ser.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : s32ser.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !s32ser.chipErase() ) throw XCom.newException("ERR: s32ser.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( s32ser._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !s32ser.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: s32ser.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = s32ser.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: s32ser.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: s32ser.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], s32ser.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.max(1024 * 4, s32serConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !s32ser.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: s32ser.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : s32ser.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                if( !s32ser.end() ) throw XCom.newException("ERR: s32ser.end()");

            } // if

            // Test 'ProgBootOpenBLT' - STM32F103
            if(!true) {

                // ##### !!! TODO : Add more MCU tests !!! #####

                final int STM32F103_NoKey     = 0;
                final int STM32F103_WithKey   = 1;
                final int STM32F103_WithKeyCS = 2;

                final int SelMCU              = STM32F103_WithKeyCS;

                obltConfig = (SelMCU == STM32F103_NoKey    ) ? ProgBootOpenBLT.Config.STM32_64k_1k()
                           : (SelMCU == STM32F103_WithKey  ) ? ProgBootOpenBLT.Config.STM32_64k_1k()
                           : (SelMCU == STM32F103_WithKeyCS) ? ProgBootOpenBLT.Config.STM32_64k_1k()
                           :                                   null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(obltConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootOpenBLT.Config.class) ) );
                SysUtil.systemExit();
                //*/

                oblt = new ProgBootOpenBLT(obltConfig);

                //*
                final String hex = (SelMCU == STM32F103_NoKey    ) ? "../src/1-TestData/PTest2X.Blink-STM32F103C8T6-Ofs8k-VBC.cpp.hex"
                                 : (SelMCU == STM32F103_WithKey  ) ? "../src/1-TestData/PTest2X.Blink-STM32F103C8T6-Ofs8k-VBC.cpp.hex"
                                 : (SelMCU == STM32F103_WithKeyCS) ? "../src/1-TestData/PTest2X.Blink-STM32F103C8T6-Ofs8k-VBC.cpp.hex"
                                 :                                   null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( oblt._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                int baudrate = 57600;
                if(SelMCU == STM32F103_WithKeyCS) baudrate = -baudrate;

                if(!true) {

                    // NOTE : Using fixed values

                    final long flashBA = (SelMCU == STM32F103_NoKey    ) ? 0x08000000L
                                       : (SelMCU == STM32F103_WithKey  ) ? 0x08000000L
                                       : (SelMCU == STM32F103_WithKeyCS) ? 0x08000000L
                                       :                                   0L;

                    final int  blSize  = (SelMCU == STM32F103_NoKey    ) ? 0x2000
                                       : (SelMCU == STM32F103_WithKey  ) ? 0x2000
                                       : (SelMCU == STM32F103_WithKeyCS) ? 0x2000
                                       :                                   0;

                    final int  vtSize  = (SelMCU == STM32F103_NoKey    ) ? 0x010C // From BOOT_FLASH_VECTOR_TABLE_CS_OFFSET, as defined in OpenBLT
                                       : (SelMCU == STM32F103_WithKey  ) ? 0x010C // ---
                                       : (SelMCU == STM32F103_WithKeyCS) ? 0x010C // ---
                                       :                                   0;

                    if(SelMCU == STM32F103_WithKey || SelMCU == STM32F103_WithKeyCS) oblt.setKey( new int[] { 0x54 } );

                    if( !oblt.begin("/dev/ttyUSB0", baudrate, flashBA, blSize, vtSize) ) throw XCom.newException("ERR: oblt.begin()");

                }
                else {

                    // NOTE : Using user callback

                    if( !oblt.begin("/dev/ttyUSB0", baudrate, (final int requestType, final long[] requestParam) -> {

                        if(requestType == ProgBootOpenBLT.UserCallback.REQ_FLASH_BASE_ADDR) {
                            return   (SelMCU == STM32F103_NoKey    ) ? new long[] { 0x08000000L }
                                   : (SelMCU == STM32F103_WithKey  ) ? new long[] { 0x08000000L }
                                   : (SelMCU == STM32F103_WithKeyCS) ? new long[] { 0x08000000L }
                                   :                                   null;
                        }

                        if(requestType == ProgBootOpenBLT.UserCallback.REQ_BOOTLOADER_SIZE) {
                            return   (SelMCU == STM32F103_NoKey    ) ? new long[] { 0x2000 }
                                   : (SelMCU == STM32F103_WithKey  ) ? new long[] { 0x2000 }
                                   : (SelMCU == STM32F103_WithKeyCS) ? new long[] { 0x2000 }
                                   :                                   null;
                        }


                        if(requestType == ProgBootOpenBLT.UserCallback.REQ_UPROG_VTAB_SIZE) {
                            return   (SelMCU == STM32F103_NoKey    ) ? new long[] { 0x010C } // From BOOT_FLASH_VECTOR_TABLE_CS_OFFSET, as defined in OpenBLT
                                   : (SelMCU == STM32F103_WithKey  ) ? new long[] { 0x010C } // ---
                                   : (SelMCU == STM32F103_WithKeyCS) ? new long[] { 0x010C } // ---
                                   :                                   null;
                        }

                        if(requestType == ProgBootOpenBLT.UserCallback.REQ_KEY_FROM_SEED) {
                            return   (SelMCU == STM32F103_NoKey    ) ? null
                                   : (SelMCU == STM32F103_WithKey  ) ? new long[] { requestParam[0] - 1 }
                                   : (SelMCU == STM32F103_WithKeyCS) ? new long[] { requestParam[0] - 1 }
                                   :                                   null;
                        }

                        return null;

                    } ) ) throw XCom.newException("ERR: oblt.begin()");

                } // if

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !oblt.chipErase() ) throw XCom.newException("ERR: oblt.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( oblt._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !oblt.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: oblt.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = oblt.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: oblt.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: oblt.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], oblt.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.min(fwLength, obltConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !oblt.readFlash(fwStartAddress, rdFlashSize, stdPP) ) throw XCom.newException("ERR: oblt.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : oblt.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                if( !oblt.end() ) throw XCom.newException("ERR: oblt.end()");

            } // if

            // Test 'ProgBootSAMBA' - AT91SAM3X8E/ATSAMD21G18A
            if(!true) {

                final int SMB_AT91SAM3X8E  = 0;
                final int SMB_ATSAMD21G18A = 1;

                final int SMB_SelMCU       = SMB_AT91SAM3X8E;

                sambaConfig = (SMB_SelMCU == SMB_AT91SAM3X8E ) ? ProgBootSAMBA.Config.ATSAM3X8   ()
                            : (SMB_SelMCU == SMB_ATSAMD21G18A) ? ProgBootSAMBA.Config.ATSAMD21x18()
                            :                                    null;

                /*
                final String jsonStr = SerializableDeepClone.toJSON(sambaConfig);
                SysUtil.stdDbg().println(jsonStr);
                SysUtil.stdDbg().println( SerializableDeepClone.toJSON( SerializableDeepClone.fromJSON(jsonStr, ProgBootSAMBA.Config.class) ) );
                SysUtil.systemExit();
                //*/

                samba = new ProgBootSAMBA(sambaConfig);

                //*
                final String hex = (SMB_SelMCU == SMB_AT91SAM3X8E ) ? "../src/1-TestData/PTest2X.Blink-ATSAM3X8E.cpp.hex"
                                 : (SMB_SelMCU == SMB_ATSAMD21G18A) ? "../src/1-TestData/PTest2X.Blink-ATSAMD21G18A.cpp.hex"
                                 :                                    null;
                fwc.loadIntelHexFile(hex);
                int i = 0;
                SysUtil.stdDbg().printf("Blk# Addr Size\n");
                for( final FWBlock f : fwc.fwBlocks() ) {
                    SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
                }
                SysUtil.stdDbg().println();

                final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( samba._flashMemoryEmptyValue() );
                final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                final int    fwLength       =       fwDataBuff.length;
                //*/

                if( !samba.begin("/dev/ttyACM0", 115200, !true ? 1200 : 0) ) throw XCom.newException("ERR: samba.begin()");

                //*
                if( !samba.readSignature() ) throw XCom.newException("ERR: samba.readSignature()");

                final int[] sigBytes = (SMB_SelMCU == SMB_AT91SAM3X8E ) ? new int[] { 0x28, 0x5E, 0x0A, 0x60 }
                                     : (SMB_SelMCU == SMB_ATSAMD21G18A) ? new int[] { 0x10, 0x01, 0x00, 0x05 }
                                     :                                    null;

                if( !samba.verifySignature(sigBytes) ) throw XCom.newException("ERR: samba.verifySignature()");

                SysUtil.stdDbg().print("MCU Signature         = ");
                for( final int s : samba.mcuSignature() ) SysUtil.stdDbg().printf("%02X ", s);
                SysUtil.stdDbg().println("\n");
                //*/

                /*
                SysUtil.stdDbg().println("Erasing Chip");
                if( !samba.chipErase() ) throw XCom.newException("ERR: samba.chipErase()");
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("Writing Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString( samba._flashMemoryAlignWriteSize(fwLength) ) );
                final long tw1 = SysUtil.getNS();
                if( !samba.writeFlash(fwDataBuff, fwStartAddress, fwLength, stdPP) ) throw XCom.newException("ERR: samba.writeFlash()");
                final long tw2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tw2 - tw1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tw2 - tw1) );
                SysUtil.stdDbg().println();
                //*/

                //*
                SysUtil.stdDbg().println("Verifying Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(fwLength) );
                final long tv1     = SysUtil.getNS();
                final int  verBPos = samba.verifyFlash(fwDataBuff, fwStartAddress, fwLength, stdPP);
                final long tv2     = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", fwDataBuff.length, (tv2 - tv1) * 0.000000001, 1000000000.0 * fwDataBuff.length / (tv2 - tv1) );
                if(verBPos < 0) throw XCom.newException("ERR: samba.verifyFlash()");
                if(verBPos < fwLength) throw XCom.newException("ERR: samba.verifyFlash() : mismatched byte @%08X [%02X->%02X]", fwStartAddress + verBPos, fwDataBuff[verBPos], samba.config().memoryFlash.readDataBuff[verBPos]);
                SysUtil.stdDbg().println();
                //*/

                /*
                final int rdFlashSize = Math.max(1024 * 4, sambaConfig.memoryFlash.totalSize);
                SysUtil.stdDbg().println("Reading Flash");
                SysUtil.stdDbg().println( ProgressCB.getStdProgressInfoString(rdFlashSize) );
                final long tr1 = SysUtil.getNS();
                if( !samba.readFlash(0, rdFlashSize, stdPP) ) throw XCom.newException("ERR: samba.readFlash()");
                final long tr2 = SysUtil.getNS();
                SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", rdFlashSize, (tr2 - tr1) * 0.000000001, 1000000000.0 * rdFlashSize / (tr2 - tr1) );
                if(!true) {
                    for(final int b : samba.config().memoryFlash.readDataBuff) {
                        SysUtil.stdDbg().printf("%02X ", b);
                    }
                }
                SysUtil.stdDbg().println();
                //*/

                if( !samba.end() ) throw XCom.newException("ERR: samba.end()");

            } // if

        } // try
        catch(final Exception e) {
            if(avr109   != null) avr109  .end();
            if(stk500   != null) stk500  .end();
            if(stk500v2 != null) stk500v2.end();
            if(chip45   != null) chip45  .end();
            if(tsb      != null) tsb     .end();
            if(urclock  != null) urclock .end();
            if(luflpt   != null) luflpt  .end();
            if(s32ser   != null) s32ser  .end();
            if(oblt     != null) oblt    .end();
            if(samba    != null) samba   .end();
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class PTest1B
