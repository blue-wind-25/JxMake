/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


import java.io.File;

import java.nio.file.Path;

import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.math.BigInteger;

/*
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.merge.MergeStrategy;
//*/

import com.intellectualsites.http.*;

import jxm.*;
import jxm.dl.*;
import jxm.tool.*;
import jxm.tool.fwc.*;
import jxm.xb.*;

import static jxm.tool.CommandShell.*;


//
// Test class (the test application entry point)
//
public class GTest2 {

    @SuppressWarnings("unchecked")
    public static void main(final String[] args)
    {
        try {

            // Initialize global
            JxMake.initializeGlobal();

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            // Test firmware composer - 'Intel Hex'
            if(!true) {
                final String srcPathPrefix = "../test/2-fwctestdata/";
                final String dstPathPrefix = SysUtil.getRootTmpDir() + "/";

                final FWComposer fwc0 = new FWComposer();
                final FWComposer fwc1 = new FWComposer();
                final FWComposer fwc2 = new FWComposer();

                final String[] fnms = true ? new String[] { "l", "`l", "s1", "s2", "x", "y", "z", "`z" } : new String[0];

                for(String fnm : fnms) {

                    SysUtil.stdDbg().printf("##### %s #####\n", fnm);

                    long   startAddressOffset = 0;
                    String fnPrefix           = "";
                    if( fnm.charAt(0) == '`' ) {
                        startAddressOffset = 1024 * 1024;
                        fnPrefix           = "`";
                        fnm                = fnm.substring(1);
                    }

                    final String sbf = srcPathPrefix +            fnm + ".bin";
                    final String shf = srcPathPrefix +            fnm + ".hex";
                    final String dbf = dstPathPrefix + fnPrefix + fnm + ".bin";
                    final String dhf = dstPathPrefix + fnPrefix + fnm + ".hex";

                    SysUtil.stdDbg().println(">>> RAWBIN & INTHEX");
                    fwc0.loadRawBinaryFile(sbf, startAddressOffset);
                    fwc1.loadRawBinaryFile(sbf, startAddressOffset);
                    fwc2.loadIntelHexFile (shf, startAddressOffset);
                    SysUtil.stdDbg().println( fwc0.equals(fwc1) );
                    SysUtil.stdDbg().println( fwc0.equals(fwc2) );
                    SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                    SysUtil.stdDbg().println();

                    fwc1.saveRawBinaryFile(dbf + ".1");
                    fwc2.saveRawBinaryFile(dbf + ".2");
                    fwc1.saveIntelHexFile (dhf + ".1");
                    fwc2.saveIntelHexFile (dhf + ".2");

                    fwc1.clear();
                    fwc2.clear();

                    SysUtil.stdDbg().println(">>> RAWBIN");
                    fwc1.loadRawBinaryFile(dbf + ".1");
                    fwc2.loadRawBinaryFile(dbf + ".2");
                    SysUtil.stdDbg().println( fwc0.equals(fwc1) );
                    SysUtil.stdDbg().println( fwc0.equals(fwc2) );
                    SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                    SysUtil.stdDbg().println();

                    SysUtil.stdDbg().println(">>> INTHEX");
                    fwc0.loadIntelHexFile(shf       );
                    fwc1.loadIntelHexFile(dhf + ".1");
                    fwc2.loadIntelHexFile(dhf + ".2");
                    SysUtil.stdDbg().println( fwc0.equals(fwc1) );
                    SysUtil.stdDbg().println( fwc0.equals(fwc2) );
                    SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                    SysUtil.stdDbg().println();

                    // diff -q ../test/2-fwctestdata/x.bin /tmp/__jxmake__/x.bin.1
                    // diff -q ../test/2-fwctestdata/x.bin /tmp/__jxmake__/x.bin.2

                } // for

                SysUtil.stdDbg().println("##### COMPOSE #####");

                // ##### !!! TODO : Add more tests? !!! #####

                fwc0.loadIntelHexFile(srcPathPrefix + "s1.hex");
                fwc1.loadIntelHexFile(srcPathPrefix + "s1.hex");
                fwc2.loadIntelHexFile(srcPathPrefix + "s1.hex");

                SysUtil.stdDbg().println( fwc0.compose( fwc1,  (fwc0.maxFinalAddress() + 1) ) );
                SysUtil.stdDbg().println( fwc0.compose( fwc2, -(fwc2.maxFinalAddress() + 1) ) );
                SysUtil.stdDbg().println();

                fwc0.saveRawBinaryFile(dstPathPrefix + "s1x3.bin");
                fwc0.saveIntelHexFile (dstPathPrefix + "s1x3.hex");

                fwc1.loadRawBinaryFile(dstPathPrefix + "s1x3.bin");
                fwc2.loadIntelHexFile (dstPathPrefix + "s1x3.hex");

                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                SysUtil.stdDbg().println();
            }

            // Test firmware composer - 'Motorola S-Record'
            if(!true) {
                final String srcPathPrefix = "../test/2-fwctestdata/";
                final String dstPathPrefix = SysUtil.getRootTmpDir() + "/";

                final String sbf  = srcPathPrefix + "s2.bin";
                final String ssf1 = srcPathPrefix + "s2.1.mot";
                final String ssf2 = srcPathPrefix + "s2.3.mot";
                final String dbf  = dstPathPrefix + "s2.bin";
                final String dsf1 = dstPathPrefix + "s2.1.mot";
                final String dsf2 = dstPathPrefix + "s2.3.mot";

                final FWComposer fwc0 = new FWComposer();
                final FWComposer fwc1 = new FWComposer();
                final FWComposer fwc2 = new FWComposer();

                final long startAddressOffset = 0 * 1024 * 1024;

                SysUtil.stdDbg().println("##### s2 #####");
                SysUtil.stdDbg().println(">>> RAWBIN & MTSREC");

                fwc0.loadRawBinaryFile      (sbf , startAddressOffset);
                fwc1.loadMotorolaSRecordFile(ssf1, startAddressOffset);
                fwc2.loadMotorolaSRecordFile(ssf2, startAddressOffset);
                SysUtil.stdDbg().println( fwc0.equals(fwc1) );
                SysUtil.stdDbg().println( fwc0.equals(fwc2) );
                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                SysUtil.stdDbg().println();

                fwc0.saveRawBinaryFile      (dbf );
                fwc1.saveMotorolaSRecordFile(dsf1);
                fwc2.saveMotorolaSRecordFile(dsf2);

                fwc0.clear();
                fwc1.clear();
                fwc2.clear();

                fwc0.loadRawBinaryFile      (dbf, startAddressOffset);
                fwc1.loadMotorolaSRecordFile(dsf1                   );
                fwc2.loadMotorolaSRecordFile(dsf2                   );
                SysUtil.stdDbg().println( fwc0.equals(fwc1) );
                SysUtil.stdDbg().println( fwc0.equals(fwc2) );
                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                SysUtil.stdDbg().println();
            }

            // Test firmware composer - 'Tektronix Hex'
            if(!true) {
                final String srcPathPrefix = "../test/2-fwctestdata/";
                final String dstPathPrefix = SysUtil.getRootTmpDir() + "/";

                final String shf  = srcPathPrefix + "x.hex";
                final String ssf1 = srcPathPrefix + "x.bin.ext.tek";
                final String ssf2 = srcPathPrefix + "x.hex.ext.tek";
                final String ssfU = srcPathPrefix + "u.tek";
                final String dsf1 = dstPathPrefix + "x.bin.ext.tek";
                final String dsf2 = dstPathPrefix + "x.hex.ext.tek";
                final String dsfU = dstPathPrefix + "u.ext.tek";

                final FWComposer fwcH = new FWComposer();
                final FWComposer fwc1 = new FWComposer();
                final FWComposer fwc2 = new FWComposer();
                final FWComposer fwcU = new FWComposer();

                final long startAddressOffset = 0 * 1024 * 1024;

                SysUtil.stdDbg().println("##### x & u #####");
                SysUtil.stdDbg().println(">>> TEKHEX & INTHEX");

                fwcH.loadIntelHexFile    (shf , startAddressOffset);
                fwc1.loadTektronixHexFile(ssf1, startAddressOffset);
                fwc2.loadTektronixHexFile(ssf2, startAddressOffset);
                fwcU.loadTektronixHexFile(ssfU, startAddressOffset);
                SysUtil.stdDbg().println( fwcH.equals(fwc1) );
                SysUtil.stdDbg().println( fwcH.equals(fwc2) );
                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                SysUtil.stdDbg().println();

                fwc1.saveTektronixHexFile(dsf1);
                fwc2.saveTektronixHexFile(dsf2);
                fwcU.saveTektronixHexFile(dsfU);

                fwc1.clear();
                fwc2.clear();
                fwcU.clear();

                fwc1.loadTektronixHexFile(dsf1);
                fwc2.loadTektronixHexFile(dsf2);
                fwcU.loadTektronixHexFile(dsfU);
                SysUtil.stdDbg().println( fwcH.equals(fwc1) );
                SysUtil.stdDbg().println( fwcH.equals(fwc2) );
                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                fwcH.loadTektronixHexFile(ssfU, startAddressOffset);
                SysUtil.stdDbg().println( fwcH.equals(fwcU) );
                SysUtil.stdDbg().println();
            }

            // Test firmware composer - 'MOS Technology'
            if(!true) {
                final String srcPathPrefix = "../test/2-fwctestdata/";
                final String dstPathPrefix = SysUtil.getRootTmpDir() + "/";

                final String ssf1 = srcPathPrefix + "v1.mos";
                final String ssf2 = srcPathPrefix + "v2.mos";
                final String dsf1 = dstPathPrefix + "v1.mos";
                final String dsf2 = dstPathPrefix + "v2.mos";
                final String dhf1 = dstPathPrefix + "v1.hex";
                final String dhf2 = dstPathPrefix + "v2.hex";

                final FWComposer fwc1 = new FWComposer();
                final FWComposer fwc2 = new FWComposer();

                final long startAddressOffset = 0 * 16 * 1024;

                SysUtil.stdDbg().println("##### v1 & v2 #####");
                SysUtil.stdDbg().println(">>> MOSTEC & INTHEX");

                fwc1.loadMOSTechnologyFile(ssf1, startAddressOffset);
                fwc2.loadMOSTechnologyFile(ssf2, startAddressOffset);
                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                SysUtil.stdDbg().println();

                fwc1.saveMOSTechnologyFile(dsf1);
                fwc2.saveMOSTechnologyFile(dsf2);
                fwc1.saveIntelHexFile     (dhf1);
                fwc2.saveIntelHexFile     (dhf2);

                final FWComposer fwc3 = new FWComposer();
                final FWComposer fwc4 = new FWComposer();

                fwc3.loadMOSTechnologyFile(dsf1);
                fwc4.loadMOSTechnologyFile(dsf2);
                SysUtil.stdDbg().println( fwc3.equals(fwc4) );
                SysUtil.stdDbg().println( fwc3.equals(fwc1) );
                SysUtil.stdDbg().println( fwc4.equals(fwc2) );
                SysUtil.stdDbg().println();

                fwc3.loadIntelHexFile(dhf1);
                fwc4.loadIntelHexFile(dhf2);
                SysUtil.stdDbg().println( fwc3.equals(fwc4) );
                SysUtil.stdDbg().println( fwc3.equals(fwc1) );
                SysUtil.stdDbg().println( fwc4.equals(fwc2) );
                SysUtil.stdDbg().println();
            }

            // Test firmware composer - 'Texas Instruments Text Hex'
            if(!true) {
                final String srcPathPrefix = "../test/2-fwctestdata/";
                final String dstPathPrefix = SysUtil.getRootTmpDir() + "/";

                final String ssf = srcPathPrefix + "w.tix";
                final String dsf = dstPathPrefix + "w.tix";
                final String dhf = dstPathPrefix + "w.hex";

                final FWComposer fwc1 = new FWComposer();

                final long startAddressOffset = 0 * 16 * 1024;

                SysUtil.stdDbg().println("##### w #####");
                SysUtil.stdDbg().println(">>> TIXHEX & INTHEX");

                fwc1.loadTITextHexFile(ssf, startAddressOffset);
                SysUtil.stdDbg().println();

                fwc1.saveTITextHexFile(dsf);
                fwc1.saveIntelHexFile (dhf);

                final FWComposer fwc2 = new FWComposer();
                final FWComposer fwc3 = new FWComposer();

                fwc2.loadTITextHexFile(ssf);
                fwc3.loadIntelHexFile (dhf);
                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                SysUtil.stdDbg().println( fwc1.equals(fwc3) );
                SysUtil.stdDbg().println();
            }

            // Test firmware composer - 'ASCII Hex' and 'Verilog VMem'
            if(!true) {
                final String srcPathPrefix = "../test/2-fwctestdata/";
                final String dstPathPrefix = SysUtil.getRootTmpDir() + "/";

                final String shf1 = srcPathPrefix + "v1.mos";
                final String saf1 = srcPathPrefix + "v1.asc";
                final String svf1 = srcPathPrefix + "v1.mem";

                final String shf2 = srcPathPrefix + "x.hex";
                final String saf2 = srcPathPrefix + "x.asc";
                final String svf2 = srcPathPrefix + "x.mem";

                final FWComposer fwc1 = new FWComposer();
                final FWComposer fwc2 = new FWComposer();
                final FWComposer fwc3 = new FWComposer();

                final long startAddressOffset = 0 * 16 * 1024;

                SysUtil.stdDbg().println("##### v1 #####");
                SysUtil.stdDbg().println(">>> ASCHEX & VLVMEM");

                fwc1.loadMOSTechnologyFile(shf1, startAddressOffset);
                fwc2.loadASCIIHexFile     (saf1, startAddressOffset);
                fwc3.loadVerilogVMemFile  (svf1, startAddressOffset);

                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                SysUtil.stdDbg().println( fwc1.equals(fwc3) );
                SysUtil.stdDbg().println( fwc2.equals(fwc3) );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println("##### x #####");
                SysUtil.stdDbg().println(">>> ASCHEX & VLVMEM");

                fwc1.loadIntelHexFile   (shf2, startAddressOffset);
                fwc2.loadASCIIHexFile   (saf2, startAddressOffset);
                fwc3.loadVerilogVMemFile(svf2, startAddressOffset);

                SysUtil.stdDbg().println( fwc1.equals(fwc2) );
                SysUtil.stdDbg().println( fwc1.equals(fwc3) );
                SysUtil.stdDbg().println( fwc2.equals(fwc3) );
                SysUtil.stdDbg().println();
            }

            // Test firmware composer - 'ELF'
            if(!true) {
                final String     srcPathPrefix = "../test/2-fwctestdata/e-";
                final String     dstPathPrefix = SysUtil.getRootTmpDir() + "/";
                final String[]   srcFiles      = new String[] {
                                                     /* 0 */ true ? "atmega"  : null,
                                                     /* 1 */ true ? "attiny0" : null,
                                                     /* 2 */ true ? "atxmega" : null,
                                                     /* 3 */ true ? "samd21"  : null,
                                                     /* 4 */ true ? "sam3x8"  : null,
                                                     /* 5 */ true ? "nrf51"   : null,
                                                     /* 6 */ true ? "nrf52"   : null,
                                                             null
                                                 };
                final long[]     binAddrOfs    = new long[] {
                                                     /* 0 */ 0          ,
                                                     /* 1 */ 0          ,
                                                     /* 2 */ 0          ,
                                                     /* 3 */ 0x00002000L,
                                                     /* 4 */ 0x00080000L,
                                                     /* 5 */ 0          ,
                                                     /* 6 */ 0          ,
                                                            -1
                                                 };
                final Object[]   isecRules     = new FWComposer.ELF_ISecRules[] {
                                                     /* 0 */        null                                                       ,
                                                     /* 1 */        null                                                       ,
                                                     /* 2 */        null                                                       ,
                                                     /* 3 */ true ? null : FWComposer.example_ELF_ISecRules_SAMD21(0x00002000L),
                                                     /* 4 */ true ? null : FWComposer.example_ELF_ISecRules_SAM3X8(0x00080000L),
                                                     /* 5 */        null                                                       ,
                                                     /* 6 */        null                                                       ,
                                                             null
                                                 };
                final FWComposer fwc1          = new FWComposer();
                final FWComposer fwc2          = new FWComposer();

                for(int idx = 0; idx < srcFiles.length; ++idx) {
                    final String sf = srcFiles[idx];
                    if(sf == null) continue;

                    final String elf = srcPathPrefix + sf + ".elf";
                    final String bin = srcPathPrefix + sf + ".bin";
                    final String hex = srcPathPrefix + sf + ".hex";
                    final String ref = SysUtil.pathIsValid(bin) ? bin : hex;
                                                   fwc1.loadELFBinaryFile( elf, 0              , (FWComposer.ELF_ISecRules) isecRules [idx] );
                    if( SysUtil.pathIsValid(bin) ) fwc2.loadRawBinaryFile( bin, binAddrOfs[idx]                                             );
                    else                           fwc2.loadIntelHexFile ( hex                                                              );
                    if(true) {
                        int s = 0;
                        for( final FWComposer fwc : new FWComposer[] { fwc1, fwc2 } ) {
                            final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( (byte) 0xFF );
                            final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                            final int    fwLength       =       fwDataBuff.length;
                                  int    i              = 0;
                                  int    z              = 0;
                            SysUtil.stdDbg().printf ( "----- %s -----\n", (s++ == 0) ? elf : ref );
                            SysUtil.stdDbg().printf("Blk# Address  Size\n");
                            for( final FWBlock f : fwc.fwBlocks() ) {
                                SysUtil.stdDbg().printf( "[%02d] %08X %04X (%d)\n", i++, f.startAddress(), f.length(), f.length() );
                                z += f.length();
                            }
                            SysUtil.stdDbg().println();
                            SysUtil.stdDbg().printf ("[Σ∊] %08X %04X (%d)\n", fwStartAddress, z       , z       );
                            SysUtil.stdDbg().printf ("[Σᶠ] %08X %04X (%d)\n", fwStartAddress, fwLength, fwLength);
                            SysUtil.stdDbg().println();
                            if(!true) {
                                for(int b = 0; b < fwLength; ++b) {
                                    SysUtil.stdDbg().printf( "%02X ", fwDataBuff[b] & 0xFF );
                                    if( (b + 1) % 64 == 0 ) SysUtil.stdDbg().println();
                                }
                                SysUtil.stdDbg().println();
                            }
                        }
                        if( fwc1.equals(fwc2, true) ) SysUtil.stdDbg().printf( "%s##### %sELF = REF%s #####%s\n\n", XCom.AC_c_white(), XCom.AC_c_green(), XCom.AC_c_white(), XCom.AC_c_reset() );
                        else                          SysUtil.stdDbg().printf( "%s##### %sELF ≠ REF%s #####%s\n\n", XCom.AC_c_white(), XCom.AC_c_red  (), XCom.AC_c_white(), XCom.AC_c_reset() );
                    }
                }
            }

            // Test 'ByteStreamEditor'
            if(!true) {
                final String                   handle = BSEList.bseNew( (byte) 0xFF );
                final BSEList.ByteStreamEditor bseObj = BSEList.bseGet(handle);

                SysUtil.stdDbg().println( BSEList.bseIsValidHandle(handle) );
                SysUtil.stdDbg().println();

                bseObj.dump();

                bseObj.seekBeg(10);
                bseObj.dump();

                bseObj.seekBeg(5);
                bseObj.dump();

                bseObj.truncate();
                bseObj.dump();

                bseObj.seekBeg(0);
                bseObj.truncate();
                bseObj.dump();

                SysUtil.stdDbg().println();

                bseObj.setBE();
                bseObj.wrSInt08(-1          );
                bseObj.wrUInt16( 65535      );
                bseObj.wrSInt08( 0          );
                bseObj.setLE();
                bseObj.wrSInt08(-1          );
                bseObj.wrUInt16( 65535      );

                bseObj.dump();
                bseObj.seekEnd(-1);
                bseObj.dump();
                bseObj.seekCur(-2);
                bseObj.dump();

                bseObj.seekBeg(0);
                bseObj.truncate();

                SysUtil.stdDbg().println();

                final BigInteger UInt64Max = new BigInteger("18446744073709551615", 10);

                bseObj.setBE   (                      );
                bseObj.wrByte  ( (byte) 49            );
                bseObj.wrByte  ('a'                   );
                bseObj.wrByte  ("A"                   );
                bseObj.wrByte  ('ö'                   );
                bseObj.wrSInt08(-1                    );
                bseObj.wrSInt08(-2                    );
                bseObj.wrSInt08(-3                    );
                bseObj.wrUInt08( 253                  );
                bseObj.wrUInt08( 254                  );
                bseObj.wrUInt08( 255                  );
                bseObj.wrSInt16(-1                    );
                bseObj.wrSInt16(-2                    );
                bseObj.wrSInt16(-3                    );
                bseObj.wrUInt16( 65533                );
                bseObj.wrUInt16( 65534                );
                bseObj.wrUInt16( 65535                );
                bseObj.wrSInt32(-1L                   );
                bseObj.wrSInt32(-2L                   );
                bseObj.wrSInt32(-3L                   );
                bseObj.wrUInt32( 4294967293L          );
                bseObj.wrUInt32( 4294967294L          );
                bseObj.wrUInt32( 4294967295L          );
                bseObj.wrSInt64(-1L                   );
                bseObj.wrSInt64(-2L                   );
                bseObj.wrSInt64(-3L                   );
                bseObj.wrUInt64("18446744073709551613");
                bseObj.wrUInt64("18446744073709551614");
                bseObj.wrUInt64(UInt64Max             );
                bseObj.wrUTF8  ("abc"                 );
                bseObj.wrUTF8  ("abc", false          );
                bseObj.wrFlt32 (-0.15625F             );
                bseObj.wrFlt32 ( 0.15625F             );
                bseObj.wrDbl64 (-0.15625              );
                bseObj.wrDbl64 ( 0.15625              );
                bseObj.wrUTF16 ("def"                 );
                bseObj.wrUTF16 ("def", false          );
                bseObj.wrUTF32 ("ghi"                 );
                bseObj.wrUTF32 ("ghi", false          );
                bseObj.setLE   (                      );
                bseObj.wrByte  ( (byte) 49            );
                bseObj.wrByte  ('a'                   );
                bseObj.wrByte  ("A"                   );
                bseObj.wrByte  ('ö'                   );
                bseObj.wrSInt08("-1"                  );
                bseObj.wrSInt08("-2"                  );
                bseObj.wrSInt08("-3"                  );
                bseObj.wrUInt08( 253                  );
                bseObj.wrUInt08( 254                  );
                bseObj.wrUInt08( 255                  );
                bseObj.wrSInt16("-1"                  );
                bseObj.wrSInt16("-2"                  );
                bseObj.wrSInt16("-3"                  );
                bseObj.wrUInt16( 65533                );
                bseObj.wrUInt16( 65534                );
                bseObj.wrUInt16( 65535                );
                bseObj.wrSInt32("-1"                  );
                bseObj.wrSInt32("-2"                  );
                bseObj.wrSInt32("-3"                  );
                bseObj.wrUInt32( 4294967293L          );
                bseObj.wrUInt32( 4294967294L          );
                bseObj.wrUInt32( 4294967295L          );
                bseObj.wrSInt64(-1L                   );
                bseObj.wrSInt64(-2L                   );
                bseObj.wrSInt64(-3L                   );
                bseObj.wrUInt64("18446744073709551613");
                bseObj.wrUInt64("18446744073709551614");
                bseObj.wrUInt64(UInt64Max             );
                bseObj.wrUTF8  ("abc"                 );
                bseObj.wrUTF8  ("abc", false          );
                bseObj.wrFlt32 ("-0.15625"            );
                bseObj.wrFlt32 ( "0.15625"            );
                bseObj.wrDbl64 ("-0.15625"            );
                bseObj.wrDbl64 ( "0.15625"            );
                bseObj.wrUTF16 ("def"                 );
                bseObj.wrUTF16 ("def", false          );
                bseObj.wrUTF32 ("ghi"                 );
                bseObj.wrUTF32 ("ghi", false          );

                final String FilePath = "/tmp/__jxmake__/bseObj.save.test.bin";
                bseObj.saveFile(FilePath);
                bseObj.seekBeg(0);
                bseObj.truncate();
                bseObj.loadFile(FilePath);

                bseObj.seekBeg(0);
                bseObj.setBE();
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF8  (     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF8  (false) );
                SysUtil.stdDbg().println( bseObj.rdFlt32 (     ) );
                SysUtil.stdDbg().println( bseObj.rdFlt32 (     ) );
                SysUtil.stdDbg().println( bseObj.rdDbl64 (     ) );
                SysUtil.stdDbg().println( bseObj.rdDbl64 (     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF16 (     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF16 (false) );
                SysUtil.stdDbg().println( bseObj.rdUTF32 (     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF32 (false) );

                SysUtil.stdDbg().println();
                SysUtil.stdDbg().println();

                bseObj.setLE();
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdByte  (     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt16(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt32(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdSInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt64(     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF8  (     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF8  (false) );
                SysUtil.stdDbg().println( bseObj.rdFlt32 (     ) );
                SysUtil.stdDbg().println( bseObj.rdFlt32 (     ) );
                SysUtil.stdDbg().println( bseObj.rdDbl64 (     ) );
                SysUtil.stdDbg().println( bseObj.rdDbl64 (     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF16 (     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF16 (false) );
                SysUtil.stdDbg().println( bseObj.rdUTF32 (     ) );
                SysUtil.stdDbg().println( bseObj.rdUTF32 (false) );

                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );
                SysUtil.stdDbg().println( bseObj.rdUInt08(     ) );

                SysUtil.stdDbg().println();

                bseObj.dump();
                SysUtil.stdDbg().println();

                BSEList.bseDelete(handle);
                SysUtil.stdDbg().println( BSEList.bseIsValidHandle(handle) );
                SysUtil.stdDbg().println();

                bseObj.seekBeg(0);
                bseObj.truncate();
                for(int i = 0; i <= 9; ++i) bseObj.wrUInt08(i);
                bseObj.dumpExt();
                bseObj.seekEnd( 0); bseObj.rangeClear(2); bseObj.dumpExt();
                bseObj.seekBeg( 0); bseObj.rangeClear(2); bseObj.dumpExt();
                bseObj.seekBeg( 3); bseObj.rangeClear(3); bseObj.dumpExt();
                bseObj.seekEnd(-2); bseObj.rangeClear(3); bseObj.dumpExt();

                SysUtil.stdDbg().println();

                bseObj.seekBeg(0);
                bseObj.truncate();
                for(int i = 0; i <= 9; ++i) bseObj.wrUInt08(i);
                bseObj.dumpExt("ORIGINAL");

                bseObj.seekCur(-7);
                final String ch1 = bseObj.rangeCopy(3);
                bseObj.dumpExt("seekCur(-7) ; ch1 = rangeCopy(3)");

                bseObj.seekCur(2);
                final String ch2 = bseObj.rangeCut(5);
                bseObj.dumpExt("seekCur(2) ; ch2 = rangeCut(5)");

                BSEList.bseGet(ch1).dumpExt("BUFFER-1");
                BSEList.bseGet(ch2).dumpExt("BUFFER-2");

                bseObj.seekEnd(0);
                bseObj.rangePaste(ch1);
                bseObj.dumpExt("seekEnd(0) ; rangePaste(ch1)");

                bseObj.seekBeg(1);
                bseObj.rangePaste(ch2);
                bseObj.dumpExt("seekBeg(1) ; rangePaste(ch2)");

                bseObj.seekEnd(0);
                bseObj.rangeOverwrite(ch1);
                bseObj.dumpExt("seekEnd(0) ; rangeOverwrite(ch1)");

                bseObj.seekBeg(0);
                bseObj.rangeOverwrite(ch2);
                bseObj.dumpExt("seekBeg(0) ; rangeOverwrite(ch2)");

                bseObj.seekEnd(-2);
                bseObj.rangeOverwrite(ch2);
                bseObj.dumpExt("seekEnd(-2) ; rangeOverwrite(ch2)");

                bseObj.seekCur(-3);
                bseObj.rangeClear(7);
                bseObj.dumpExt("seekCur(-3) ; rangeClear(7)");

                SysUtil.stdDbg().println();

                BSEList.bseDelete(handle);
                BSEList.bseDelete(ch1   );
                BSEList.bseDelete(ch2   );
            }

            // Test 'JTAGBitstream'
            if(true) {
                final JTAGBitstream jbs1 = new JTAGBitstream();
                jbs1.loadSVF("../src/1-TestData/PTest1X.Blink-ATmega32A.sig.svf");

                final JTAGBitstream jbs2 = new JTAGBitstream();
                jbs2.loadSVF("../src/1-TestData/PTest1X.Blink-ATmega32A.cpp.serial.svf");

                final JTAGBitstream jbs3 = new JTAGBitstream();
                jbs3.loadSVF("../src/1-TestData/PTest1X.Blink-ATmega32A.cpp.pgload.svf");
            }

            // Test 'JGit'
            if(!true) {
                // https://javadoc.io/static/org.eclipse.jgit/org.eclipse.jgit/5.13.0.202109080827-r/org/eclipse/jgit/api/package-summary.html
                final File repoDir =  new File("git_repo");
                /*
                Git.cloneRepository().setURI             ( "https://github.com/raspberrypi/pico-sdk.git" )
                                     .setBranch          ( "1.5.0"                                       ) // Clone the specified tag
                                   //.setNoTags          (                                               ) // Clone the latest version
                                     .setDirectory       ( repoDir                                       )
                                     .setCloneAllBranches( false                                         )
                                     .setCloneSubmodules ( false                                         )
                                     .setProgressMonitor ( new TextProgressMonitor()                     )
                                     .call               (                                               )
                                     .close              (                                               );
                //*/
                /*
                Git.open(repoDir).pull().call();
                //*/
                /*
                Git.open(repoDir).submoduleInit().call();
                //*/
                /*
                Git.open(repoDir).submoduleUpdate().setStrategy       ( MergeStrategy.RESOLVE     )
                                                   .setProgressMonitor( new TextProgressMonitor() )
                                                   .call              (                           );
                //*/
            }

            // Test 'GitHubUtil'
            if(!true) {
              //MapList.dump( GitHubUtil.extractTagsFromFile  ("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-tags.json"    ) );

                MapList.dump( GitHubUtil.extractAssetsFromFile("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-releases.json") );
                MapList.dump( GitHubUtil.extractAssetsFromFile("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-latest.json"  ) );

                SysUtil.stdDbg().println();
            }

            // Test 'JSONUtil'
            if(!true) {
                JSONUtil.nmapDump( JSONUtil.jsonFileToNestedMap("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-version.json"   ) );
                JSONUtil.nmapDump( JSONUtil.jsonFileToNestedMap("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-versions.json"  ) );

                JSONUtil.nmapDump( JSONUtil.jsonFileToNestedMap("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-rate_limit.json") );

                JSONUtil.nmapDump( JSONUtil.jsonFileToNestedMap("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-tags.json"      ) );

                JSONUtil.nmapDump( JSONUtil.jsonFileToNestedMap("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-releases.json"  ) );
                JSONUtil.nmapDump( JSONUtil.jsonFileToNestedMap("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-latest.json"    ) );

                JSONUtil.nmapDump( JSONUtil.jsonFileToNestedMap("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-commits.json"   ) );

                JSONUtil.nmapDump( JSONUtil.jsonFileToNestedMap("../0_excluded_directory/temporary/GitHubAPI-ExampleResults/GitHub-ref_tag.json"   ) );

                SysUtil.stdDbg().println();
            }

            // Test 'HttpClient'
            if(!true) {
                final HttpClient   c = HttpClient.newBuilder().withBaseURL     ( "https://httpbin.org"      )
                                                              .withEntityMapper( EntityMapper.newInstance() )
                                                              .build           (                            );

                final HttpResponse g = c.get("/get").execute(0);
                SysUtil.stdDbg().println( g.getStatusCode() );
                SysUtil.stdDbg().println( g.getStatus() );
                SysUtil.stdDbg().println( new String( g.getRawResponse(), SysUtil._CharEncoding ) );

                final String       s = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
                final HttpResponse p = c.post("/post").withInput( ()-> s )
                                                      .execute  ( 0      );
                SysUtil.stdDbg().println( p.getStatusCode() );
                SysUtil.stdDbg().println( p.getStatus() );
                SysUtil.stdDbg().println( new String( p.getRawResponse(), SysUtil._CharEncoding ) );
            }

            // Test 'HTTPDownloader'
            if(!true) {
                final HTTPDownloader htd = new HTTPDownloader("https://raw.githubusercontent.com/earlephilhower/arduino-pico/master/tools/uf2conv.py", "/tmp", null);
                if( htd.download() ) {
                    SysUtil.stdDbg().printf("\n\nDONE\n");
                }
                else {
                    SysUtil.stdDbg().printf( "\n\nERROR %d/%d\n", htd.getDownloadedSize(), htd.getRemoteSize() );
                }
                SysUtil.cu_rmfile( htd.getOutFilePath() );

                SysUtil.stdDbg().println();
            }

            // Test 'HTTPDownloader'
            if(!true) {
                TLAuthenticator.setAsDefault();
                TLAuthenticator.setServerAuth("admin", "mypass");

                SSLTrustAll.setSSLTrustAll(true);

                final HTTPDownloader htd = new HTTPDownloader("https://localhost/download.php", "/tmp", null);
                if( htd.download() ) {
                    SysUtil.stdDbg().printf("\n\nDONE\n");
                }
                else {
                    SysUtil.stdDbg().printf( "\n\nERROR %d/%d\n", htd.getDownloadedSize(), htd.getRemoteSize() );
                }
                SysUtil.cu_rmfile( htd.getOutFilePath() );

                SSLTrustAll.setSSLTrustAll(false);

                SysUtil.stdDbg().println();
            }

            // Test 'SerialConsole' with serial plotter
            if(!true) {
                final String[]      p  = { "tty", "/dev/ttyUSB0", "115200", "lf", "/tmp/test_sp.txt" };
                final SerialConsole sc = new SerialConsole(true);
                sc.showConsole(p);
                SysUtil.stdDbg().println();
                SysUtil.systemExit();
            }

            // Test 'ESP_STDecoder'
            if(!true) {
                final String content1 =
                      "--------------- CUT HERE FOR EXCEPTION DECODER ---------------\n"
                    + "\n"
                    + "Exception (29):\n"
                    + "epc1=0x4020620d epc2=0x00000000 epc3=0x00000000 excvaddr=0x00000000 depc=0x00000000\n"
                    + "\n"
                    + "ctx: cont\n"
                    + "sp: 3ffffde0 end: 3fffffd0 offset: 0150\n"
                    + "\n"
                    + ">>>stack>>>\n"
                    + "3fffff30 : 3fffdad0 3fff05bc 3fff054c 4020de99  \n"
                    + "3fffff40 : 00000000 00000000 3fff0564 40211018  \n"
                    + "3fffff50 : 00000000 0000214e 00000000 00000001  \n"
                    + "3fffff60 : 3fff01ec 00000000 3fffff80 3ffeff24  \n"
                    + "3fffff70 : 3fffdad0 3fff0598 3fff054c 4020f6d4  \n"
                    + "3fffff80 : 40218980 00000000 00001388 3ffeff24  \n"
                    + "3fffff90 : 3fffdad0 00000000 3fff2054 00000000  \n"
                    + "3fffffa0 : feefeffe 00000000 3ffeff60 3ffeff24  \n"
                    + "3fffffb0 : 3fffdad0 00000000 3ffeff60 40207757  \n"
                    + "3fffffc0 : feefeffe feefeffe 3fffdab0 40100bd1  \n"
                    + "<<<stack<<<\n"
                    + "\n"
                    + "\n"
                    + "--------------- CUT HERE FOR EXCEPTION DECODER ---------------\n";

                final String content2 =
                      "\n"
                    + "Guru Meditation Error: Core  0 panic'ed (Unhandled debug exception)\n"
                    + "Debug exception reason: Stack canary watchpoint triggered (Tmr Svc)\n"
                    + "Core 0 register dump:\n"
                    + "PC      : 0x40081708  PS      : 0x00060336  A0      : 0x3ffbc990  A1      : 0x3ffbc8d0\n"
                    + "A2      : 0x3ffb7dd0  A3      : 0x00000000  A4      : 0x3ffb7e18  A5      : 0x00000000\n"
                    + "A6      : 0x00000001  A7      : 0x00000018  A8      : 0x800894ac  A9      : 0x3ffbc970\n"
                    + "A10     : 0x00000000  A11     : 0x3ffc12c8  A12     : 0x3ffc12c8  A13     : 0x00000001\n"
                    + "A14     : 0x00060320  A15     : 0x00000000  SAR     : 0x00000002  EXCCAUSE: 0x00000001\n"
                    + "EXCVADDR: 0x00000000  LBEG    : 0x40001609  LEND    : 0x4000160d  LCOUNT  : 0x00000000\n"
                    + "\n"
                    + "Backtrace: 0x40081708:0x3ffbc8d0 0x3ffbc98d:0x3ffbc9d0 0x400d85b6:0x3ffbc9f0 0x40082926:0x3ffbca10 0x400822fe:0x3ffbca30 0x400dd591:0x3ffbcaa0 0x400dc019:0x3ffbcac0 0x400dc467:0x3ffbcae0 0x400dc5f5:0x3ffbcb50 0x400db105:0x3ffbcbd0 0x400db1b4:0x3ffbcc50 0x400da27e:0x3ffbccb0 0x400da7b1:0x3ffbccf0 0x401396b2:0x3ffbcd10 0x401398a6:0x3ffbcd60 0x401398f9:0x3ffbcd90 0x401017ec:0x3ffbcdb0 0x401019fa:0x3ffbcde0 0x400d9721:0x3ffbce10 0x400\n"
                    + "\n";

                final ArrayList<String> result1 = ESP_STDecoder.decode(
                    SysUtil.getUHD() + "/.arduino15/packages/esp8266/tools/xtensa-lx106-elf-gcc/3.1.0-gcc10.3-e5f9fec/bin/xtensa-lx106-elf-addr2line",
                    SysUtil.getUHD() + "/Projects/Shadow/WebServerCrash.elf",
                    content1
                );

                final ArrayList<String> result2 = ESP_STDecoder.decode(
                    SysUtil.getUHD() + "/.arduino15/packages/esp32/tools/xtensa-esp32-elf-gcc/esp-2021r2-patch5-8.4.0/bin/xtensa-esp32-elf-addr2line",
                    SysUtil.getUHD() + "/Projects/Shadow/WebServerCrash.elf",
                    content2
                );

                for(final String line : result1) SysUtil.stdDbg().println(line);
                SysUtil.stdDbg().println();

                for(final String line : result2) SysUtil.stdDbg().println(line);
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println();
            }

            // Test 'ArduinoBoardsTxt'
            if(!true) {
                /*
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../0_excluded_directory/temporary/Arduino-AVR-Boards.txt"        ).toEData() ).dump();
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../0_excluded_directory/temporary/Arduino-ESP8266-Boards.txt"    ).toEData() ).dump();
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../0_excluded_directory/temporary/Arduino-ESP32-Boards.txt"      ).toEData() ).dump();
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../0_excluded_directory/temporary/Arduino-RP2040-Boards.txt"     ).toEData() ).dump();
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../0_excluded_directory/temporary/Arduino-CoreSTM32-Boards.txt"  ).toEData() ).dump();
                SysUtil.systemExit(0);
                //*/

                /*
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../test/arduino_partial_boards_1.txt").toEData() ).dump();
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../test/arduino_partial_boards_2.txt").toEData() ).dump();
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../test/arduino_partial_boards_3.txt").toEData() ).dump();
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../test/arduino_partial_boards_4.txt").toEData() ).dump();
                ArduinoBoardsTxt.fromEData( ArduinoBoardsTxt.parseFromFile("../test/arduino_partial_boards_5.txt").toEData() ).dump();
                SysUtil.systemExit(0);
                //*/

                final int    psel = 14;
                final String path =   (psel ==  0) ? "../0_excluded_directory/temporary/Arduino-AVR-Boards.txt"
                                    : (psel ==  1) ? "../0_excluded_directory/temporary/Arduino-ESP8266-Boards.txt"
                                    : (psel ==  2) ? "../0_excluded_directory/temporary/Arduino-ESP32-Boards.txt"
                                    : (psel ==  3) ? "../0_excluded_directory/temporary/Arduino-RP2040-Boards.txt"
                                    : (psel ==  4) ? "../0_excluded_directory/temporary/Arduino-CoreSTM32-Boards.txt"
                                    : (psel == 10) ? "../test/arduino_partial_boards_1.txt"
                                    : (psel == 11) ? "../test/arduino_partial_boards_2.txt"
                                    : (psel == 12) ? "../test/arduino_partial_boards_3.txt"
                                    : (psel == 13) ? "../test/arduino_partial_boards_4.txt"
                                    : (psel == 14) ? "../test/arduino_partial_boards_5.txt"
                                    :                "";

                final ArduinoBoardsTxt.BoardDefList bdl = ArduinoBoardsTxt.parseFromFile(path);
                bdl.selectedBoardDefIndex = (psel ==  0) ? 16
                                          : (psel ==  1) ? 23
                                          : (psel ==  2) ? 70
                                          : (psel ==  3) ?  0
                                          : (psel ==  4) ? 13
                                          : (psel == 10) ?  4 //  1
                                          : (psel == 11) ?  2
                                          : (psel == 12) ?  3
                                          : (psel == 13) ?  2
                                          : (psel == 14) ?  6 // 10
                                          :                 0;

                final String bdlString1 = bdl.toEData();
                if(!true) {
                    SysUtil.stdDbg().println( ArduinoBoardsTxt.getSelectedBoardConfiguration(bdlString1) );
                    SysUtil.systemExit(0);
                }

                final String bdlString2 = ArduinoBoardsTxt.boardConfigurationGUI(null, bdlString1);
                if(bdlString2 != null) {
                    if( !bdlString1.equals(bdlString2) ) ArduinoBoardsTxt.boardConfigurationGUI("Custom Title", bdlString2);
                    final String mapHandle = ArduinoBoardsTxt.getSelectedBoardConfiguration(bdlString2);
                    SysUtil.stdDbg().println(mapHandle + "\n");
                    MapList.dump(mapHandle);
                }
                //*/

                SysUtil.stdDbg().println();
            }

            // Test 'GlobalDepLoad'
            if(!true) {
                GlobalDepLoad.globalLoadDepFile("../test/src/cpp/main.dep", false);

                GlobalDepLoad.dumpDepMap( GlobalDepLoad.getAllDeps() );

                SysUtil.stdDbg().println();
            }

            // Test 'DepBuilder'
            if(!true) {
                final String fn1 = SysUtil.resolvePath( "1.dep", SysUtil.getProjectTmpDir("dummy") );
                final String fn2 = SysUtil.resolvePath( "2.dep", SysUtil.getProjectTmpDir("dummy") );
                final String fn3 = SysUtil.resolvePath( "3.dep", SysUtil.getProjectTmpDir("dummy") );

                @SuppressWarnings("serial")
                final DepList depList1 = DepBuilder.buildDepList(
                                             "../test/src/cpp/main.cpp",
                                             new ArrayList<String>() {{
                                                 add("../test/src/cpp"    );
                                                 add("../test/src/cpp/sys");
                                             }},
                                             null
                                         );
                depList1.saveToFile(fn1);

                @SuppressWarnings("serial")
                final DepList depList2 = DepBuilder.buildDepList(
                                             "../test/src/java/main.java",
                                             null,
                                             new ArrayList<String>() {{
                                                 add("../test/src/java"    );
                                                 add("../test/src/java/sys");
                                             }}
                                         );
                depList2.saveToFile(fn2);

                @SuppressWarnings("serial")
                final DepList depList3 = DepBuilder.buildDepList(
                                             "../test/src/java/lib/lib.java",
                                             null,
                                             null
                                         );
                depList3.saveToFile(fn3);

                final DepList depListR = new DepList();

                depListR.loadFromFile(fn1);
                SysUtil.stdDbg().println( depListR.needsRebuilt() );
                for( final String path : depListR.getDepFiles() ) SysUtil.stdDbg().println(path);
                SysUtil.cu_touch( depListR.getDepFiles().get(0) );
                SysUtil.stdDbg().println( depListR.needsRebuilt() );
                SysUtil.stdDbg().println();

                depListR.loadFromFile(fn2);
                SysUtil.stdDbg().println( depListR.needsRebuilt() );
                for( final String path : depListR.getDepFiles() ) SysUtil.stdDbg().println(path);
                SysUtil.cu_touch( depListR.getDepFiles().get(0) );
                SysUtil.stdDbg().println( depListR.needsRebuilt() );
                SysUtil.stdDbg().println();

                depListR.loadFromFile(fn3);
                SysUtil.stdDbg().println( depListR.needsRebuilt() );
                for( final String path : depListR.getDepFiles() ) SysUtil.stdDbg().println(path);
                SysUtil.cu_touch( depListR.getDepFiles().get(0) );
                SysUtil.stdDbg().println( depListR.needsRebuilt() );
                SysUtil.stdDbg().println();

                SysUtil.stdDbg().println();
            }

            // Test 'DepReader_*.gdl*()'
            if(!true) {
                @SuppressWarnings("serial")
                final ArrayList<String> searchPaths = new ArrayList<String>() {{
                    add("sys");
                }};
                final String outDepFile = "deplist.d";
                SysUtil.setCWD("../test/src/java"); DepReader_Java.gdlJavaImport(outDepFile, ".", searchPaths     , "build"); GlobalDepLoad.dumpDepMap( GlobalDepLoad.loadDepFile(outDepFile, false) ); SysUtil.stdDbg().println();
                SysUtil.setCWD(          "../cpp"); DepReader_C   .gdlCppInclude(outDepFile, ".", searchPaths, "o", "build"); GlobalDepLoad.dumpDepMap( GlobalDepLoad.loadDepFile(outDepFile, false) ); SysUtil.stdDbg().println();
                SysUtil.setCWD(        "../cpp20"); DepReader_C   .gdlCppModule (outDepFile, ".",              "o"         ); GlobalDepLoad.dumpDepMap( GlobalDepLoad.loadDepFile(outDepFile, false) ); SysUtil.stdDbg().println();
                SysUtil.setCWD("../../../src"    );

                SysUtil.stdDbg().println();
            }

            // Test 'CommandShell.*'
            if(!true) {
                if(true) {
                    for(int i = 0; i < 2; ++i) {
                        SysUtil.stdOut().print( CommandShell.ls("/tmp/test", i == 1, new ConfigLS(160) {
                            @Override
                            public String getSpecialMark(final Path path, final BasicFileAttributes attrs)
                            { return null; }

                            @Override
                            public String getSpecialColor(final Path path, final BasicFileAttributes attrs, final String mark)
                            { return null; }
                        } ) );
                        SysUtil.stdOut().println();
                    }
                }
                if(true) {
                    SysUtil.stdOut().println( String.join( " ", SysUtil.getJavaCmd() ) );
                    SysUtil.stdOut().println( String.join( " ",
                        CommandShell.cmdInvokeFunction(CommandShell.CMD_READ_TEXT_FILE, "1-TestData/ANSI_Seq_Test2.txt")
                    ) );
                    SysUtil.stdOut().println();
                }
            }

            // Test AutoPrintf
            if(!true) {
                SysUtil.stdOut().print( AutoPrintf.format( "Sequential : %d %f %s %%\n"       , "42", "3.14", "hello" ) );
                SysUtil.stdOut().print( AutoPrintf.format( "Indexed    : %2$s %1$d %1$f %% %n", "42", "hello"         ) );

                SysUtil.stdOut().println();

                SysUtil.stdOut().print( AutoPrintf.format("Char : %1$c%n" , "65"                  ) );
                SysUtil.stdOut().print( AutoPrintf.format("Date : %1$tF%n", "2026-01-01T06:00:00Z") );
                SysUtil.stdOut().print( AutoPrintf.format("Num  : %1$d%n" , "123"                 ) );
                SysUtil.stdOut().print( AutoPrintf.format("Bool : %1$b%n" , "true"                ) );

                SysUtil.stdOut().println();
            }

        } // try
        catch(final Exception e) {
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class GTest2
