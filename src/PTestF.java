/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


import java.util.ArrayList;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.tool.fwc.*;
import jxm.ugc.*;


//
// Test class (the test application entry point)
//
public class PTestF {

    public static void main(final String[] args)
    {
        // Instantiate the firmware composer
        final FWComposer fwc = new FWComposer();

        try {

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            // MCU configuration data
            final String[]  mcuConfig    = new String[] {
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgISP-ATmega328P.json"         ), /* 0 */
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgISP-AT89S8253.json"          ), /* 1 */
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgTPI-ATtiny10.json"           ), /* 2 */
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgUPDI-ATmega4808.json"        ), /* 3 */
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgUPDI-ATtiny3226.json"        ), /* 4 */
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgPDI-ATxmega16D4.json"        ), /* 5 */
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgBootAVR109-ATmega32U4.json"  ), /* 6 */
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgBootSTK500-ATmega328P.json"  ), /* 7 */
                                               SysUtil.readTextFileAsString("1-TestData/PTestF.ProgBootSTK500v2-ATmega2560.json")  /* 8 */
                                          };

            final String[]  mcuSignature = new String[] {
                                               "0x1E, 0x95, 0x0F", /* 0 */
                                               "0x1E, 0x73"      , /* 1 */
                                               "0x1E, 0x90, 0x03", /* 2 */
                                               "0x1E, 0x96, 0x50", /* 3 */
                                               "0x1E, 0x95, 0x27", /* 4 */
                                               "0x1E, 0x94, 0x42", /* 5 */
                                               "0x1E, 0x95, 0x87", /* 6 */
                                               "0x1E, 0x95, 0x0F", /* 7 */
                                               "0x1E, 0x98, 0x01"  /* 8 */
                                          };

            final boolean[] mcuHasEEPROM = new boolean[] {
                                               true , /* 0 */
                                               false, /* 1 */
                                               false, /* 2 */
                                               true , /* 3 */
                                               true , /* 4 */
                                               true , /* 5 */
                                               true , /* 6 */
                                               true , /* 7 */
                                               true   /* 8 */
                                           };

            final int XPrgATmega328P  = 0;
            final int XPrgAT89S8253   = 1;
            final int XPrgATtiny10    = 2;
            final int XPrgATmega4808  = 3;
            final int XPrgATtiny3226  = 4;
            final int XPrgATxmega16D4 = 5;
            final int BootATmega32U4  = 6;
            final int BootATmega328P  = 7;
            final int BootATmega2560  = 8;

            // Backend specification
            final String[][] backendSpec = new String[][] {
                                               null                                                                      , /* 0 */
                                               new String[] { "USB_ISS"        , "/dev/ttyACM0", null          , null   }, /* 1 */
                                               new String[] { "JXMAKE_DASA"    , "/dev/ttyUSB0", null          , null   }, /* 2 */
                                               new String[] { "JXMAKE_USB_GPIO", "/dev/ttyACM0", "/dev/ttyACM1", "true" }  /* 3 */
                                           };

            final int[]      backendMRF  = new int[] {
                                               Integer.MAX_VALUE, /* 0 */
                                               128              , /* 1 */
                                               256              , /* 2 */
                                               Integer.MAX_VALUE  /* 3 */
                                           };

            final int None            = 0;
            final int USB_ISS         = 1;
            final int JxMake_DASA     = 2;
            final int JxMake_USB_GPIO = 3;

            // Programmer specification
            final String[][] programmerSpec = new String[][] {
                                                  new String[] { "ProgISP"         , mcuConfig[XPrgATmega328P ], null          , "500000" }, /* 0 */
                                                  new String[] { "ProgISP"         , mcuConfig[XPrgAT89S8253  ], null          , "125000" }, /* 1 */
                                                  new String[] { "ProgTPI"         , mcuConfig[XPrgATtiny10   ], null          , "128000" }, /* 2 */
                                                  new String[] { "ProgUPDI"        , mcuConfig[XPrgATmega4808 ], null          , "115200" }, /* 3 */
                                                  new String[] { "ProgUPDI"        , mcuConfig[XPrgATtiny3226 ], null          , "115200" }, /* 4 */
                                                  new String[] { "ProgPDI"         , mcuConfig[XPrgATxmega16D4], null          , "115200" }, /* 5 */
                                                  new String[] { "ProgBootAVR109"  , mcuConfig[BootATmega32U4 ], "/dev/ttyACM0",   "1200" }, /* 6 */
                                                  new String[] { "ProgBootSTK500"  , mcuConfig[BootATmega328P ], "/dev/ttyUSB0",  "57600" }, /* 7 */
                                                  new String[] { "ProgBootSTK500v2", mcuConfig[BootATmega2560 ], "/dev/ttyACM0", "115200" }  /* 8 */
                                              };

            final boolean[]  programerSFLB  = new boolean[] {
                                                  true , /* 0 */
                                                  false, /* 1 */
                                                  true , /* 2 */
                                                  true , /* 3 */
                                                  true , /* 4 */
                                                  true , /* 5 */
                                                  false, /* 6 */
                                                  false, /* 7 */
                                                  false  /* 8 */
                                              };

            // Firmware specification
            final String[] firmwareSpec = new String[] {
                                             "../src/1-TestData/PTest1X.Blink-ATmega328P.cpp.hex"    , /* 0 */
                                             "../src/1-TestData/PTest1X.Blink-AT89S8253.cpp.hex"     , /* 1 */
                                             "../src/1-TestData/PTest1X.Blink-ATtiny10.cpp.hex"      , /* 2 */
                                             "../src/1-TestData/PTest1X.Blink-ATmega4808.cpp.hex"    , /* 3 */
                                             "../src/1-TestData/PTest1X.Blink-ATtiny3226.cpp.hex"    , /* 4 */
                                             "../src/1-TestData/PTest1X.Blink-ATxmega16D4.cpp.hex"   , /* 5 */
                                             "../src/1-TestData/PTest1B.VUARTLoopback-ATmega32U4.hex", /* 6 */
                                             "../src/1-TestData/PTest1B.Blink-ATmega328P.ino.hex"    , /* 7 */
                                             "../src/1-TestData/PTest1B.Blink-ATmega2560.ino.hex"      /* 8 */
                                          };

            // Select the backend and target
            final int SelBackend = JxMake_USB_GPIO;
                                  /*
                                   None
                                   USB_ISS
                                   JxMake_DASA
                                   JxMake_USB_GPIO
                                  */

            final int SelTarget  = XPrgATmega328P;
                                  /*
                                   XPrgATmega328P
                                   XPrgAT89S8253
                                   XPrgATtiny10
                                   XPrgATmega4808
                                   XPrgATtiny3226
                                   XPrgATxmega16D4
                                   BootATmega32U4
                                   BootATmega328P
                                   BootATmega2560
                                  */

            // Load the firmware
            fwc.loadIntelHexFile(firmwareSpec[SelTarget]);
            int i = 0;
            SysUtil.stdDbg().printf("%s\n", firmwareSpec[SelTarget]);
            SysUtil.stdDbg().printf("Blk# Addr Size\n");
            for( final FWBlock f : fwc.fwBlocks() ) {
                SysUtil.stdDbg().printf( "[%02d] %04X %04X\n", i++, f.startAddress(), f.length() );
            }
            SysUtil.stdDbg().println();

            // Instantiate the programmer executor
            final ProgExec progExec = new ProgExec(backendSpec[SelBackend], programmerSpec[SelTarget], mcuSignature[SelTarget]);

            progExec.setProgressOutputStream( SysUtil.stdDbg() );

            // Execute commands
            long[] res = null;

            if(programerSFLB[SelTarget]) {

                //*
                    res = progExec.execute( new String[] {
                        ProgExec.CMD_ReadLockBits
                    }, fwc, -1, true );
                    for(final long v : res) SysUtil.stdDbg().printf("%02X\n", v);
                    if(res.length > 0) SysUtil.stdDbg().println();

                if(!true) {
                    res = progExec.execute( new String[] {
                        ProgExec.CMD_WriteLockBits, String.valueOf(res[0]),
                        ProgExec.CMD_ReadLockBits
                    }, fwc, -1, true );
                    for(final long v : res) SysUtil.stdDbg().printf("%02X\n", v);
                    if(res.length > 0) SysUtil.stdDbg().println();
                }
                //*/

                //*
                    res = progExec.execute( new String[] {
                        ProgExec.CMD_ReadFuses
                    }, fwc, -1, true );
                    for(final long v : res) {
                        if(v < 0) SysUtil.stdDbg().printf("NA\n"     );
                        else      SysUtil.stdDbg().printf("%02X\n", v);
                    }
                    if(res.length > 0) SysUtil.stdDbg().println();

                if(!true) {
                    final String[] cmds = new String[1 + res.length + 1];
                          int      idx  = 0;
                                            cmds[idx++] = ProgExec.CMD_WriteFuses;
                    for(final long v : res) cmds[idx++] = String.valueOf(v);
                                            cmds[idx  ] = ProgExec.CMD_ReadFuses;
                    res = progExec.execute(cmds, fwc, -1, true );
                    for(final long v : res) {
                        if(v < 0) SysUtil.stdDbg().printf("NA\n"     );
                        else      SysUtil.stdDbg().printf("%02X\n", v);
                    }
                    if(res.length > 0) SysUtil.stdDbg().println();
                }
                //*/

            } // if

            /*
            if(mcuHasEEPROM[SelTarget]) {
                res = progExec.execute( new String[] {
                    ProgExec.CMD_ReadEEPROM , "0x0001",
                    ProgExec.CMD_ReadEEPROM , "0x0002",
                    ProgExec.CMD_ReadEEPROM , "0x0003"
                }, fwc, -1, true );
                for(final long v : res) SysUtil.stdDbg().printf("%02X\n", v);
                if(res.length > 0) SysUtil.stdDbg().println();

                res = progExec.execute( new String[] {
                    ProgExec.CMD_WriteEEPROM, "0x0001", String.valueOf( res[0] + 1 ),
                    ProgExec.CMD_WriteEEPROM, "0x0002", String.valueOf( res[1] + 1 ),
                    ProgExec.CMD_WriteEEPROM, "0x0003", String.valueOf( res[2] + 1 ),
                    ProgExec.CMD_ReadEEPROM , "0x0001",
                    ProgExec.CMD_ReadEEPROM , "0x0002",
                    ProgExec.CMD_ReadEEPROM , "0x0003"
                }, fwc, -1, true );
                for(final long v : res) SysUtil.stdDbg().printf("%02X\n", v);
                if(res.length > 0) SysUtil.stdDbg().println();
            }
            //*/

            res = progExec.execute( new String[] {
                /*
                ProgExec.CMD_ChipErase  ,
                ProgExec.CMD_WriteFlash ,
                //*/
                //*
                ProgExec.CMD_VerifyFlash,
                //*/
                ProgExec.CMD_NoOperation
            }, fwc, -1, true  );
            for(final long v : res) SysUtil.stdDbg().printf("%02X\n", v);
            if(res.length > 0) SysUtil.stdDbg().println();

            /*
            res = progExec.execute( new String[] {
                ProgExec.CMD_ReadFlash, "0x0000", String.valueOf( Integer.min( backendMRF[SelBackend], progExec.totalFlashMemorySize() ) )
            }, fwc, -1, true  );
            if(res.length > 0) SysUtil.stdDbg().printf("##### Read %d bytes from flash #####\n\n", res.length);
            //*/

            res = progExec.execute( new String[] {
                ProgExec.CMD_NoOperation
            }, fwc, -1, false );

            progExec.shutdown();

        } // try
        catch(final Exception e) {
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class PTestF
