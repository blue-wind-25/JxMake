/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from 'ProgBootSAMBA.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../README' file for more details).
 */

/***** PCPP_END *****/


private static class WordCopyARM {

    // NOTE : This class was adapted from the BOSSA source code

    private static long[] _opcode   = null;

    private static long   _start    = -1;
    private static long   _stack    = -1;
    private static long   _reset    = -1;
    private static long   _dst_addr = -1;
    private static long   _src_addr = -1;
    private static long   _words    = -1;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean build(final boolean dumpDisassemblyAndArray)
    {
        // Check if it is already built
        if(_opcode != null) return true;

        // Built the program
        try {

            // Create the program
            ARMCortexMThumb asm = new ARMCortexMThumb(CPU.M0) {{

              // start
                    $ldr  (Reg.R0   , "dst_addr");
                    $ldr  (Reg.R1   , "src_addr");
                    $ldr  (Reg.R2   , "words"   );
                    $b    ("check"              );

                label("copy");
                    $ldmia(Reg.R1.wb, Reg.R3    );
                    $stmia(Reg.R0.wb, Reg.R3    );
                    $subs (Reg.R2   , 1         );

                label("check");
                    $cmp  (Reg.R2   , 0         );
                    $bne  ("copy"               );

                    $ldr  (Reg.R0   , "reset"   );
                    $cmp  (Reg.R0   , 0         );
                    $bne  ("return"             );
                    $ldr  (Reg.R0   , "stack"   );
                    $mov  (Reg.SP   , Reg.R0    );

                label("return");
                    $bx   (Reg.LR               );

                $_align(2);

                label("stack");
                    $_word(0);
                label("reset");
                    $_word(0);
                label("dst_addr");
                    $_word(0);
                label("src_addr");
                    $_word(0);
                label("words");
                    $_word(0);

            }};

            // Link the program
            _opcode = asm.link(ProgSWD.DefaultCortexM_SRAMStart, dumpDisassemblyAndArray);

            if(dumpDisassemblyAndArray) {
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().printf( "### dst_addr = 0x%08X\n", asm.resolvedLabelAddress("dst_addr") );
                SysUtil.stdDbg().printf( "### reset    = 0x%08X\n", asm.resolvedLabelAddress("reset"   ) );
                SysUtil.stdDbg().printf( "### src_addr = 0x%08X\n", asm.resolvedLabelAddress("src_addr") );
                SysUtil.stdDbg().printf( "### stack    = 0x%08X\n", asm.resolvedLabelAddress("stack"   ) );
                SysUtil.stdDbg().printf( "### start    = 0x%08X\n", 0                                    );
                SysUtil.stdDbg().printf( "### words    = 0x%08X\n", asm.resolvedLabelAddress("words"   ) );
                SysUtil.stdDbg().println();
            }

            // Save the addresses
            _dst_addr = asm.resolvedLabelAddress("dst_addr");
            _reset    = asm.resolvedLabelAddress("reset"   );
            _src_addr = asm.resolvedLabelAddress("src_addr");
            _stack    = asm.resolvedLabelAddress("stack"   );
            _start    = 0;
            _words    = asm.resolvedLabelAddress("words"   );

            // Done
            return true;

        }
        catch(final JXMAsmError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long[] opcode  () { return _opcode;            }
    public static long   size    () { return _opcode.length * 4; } // In bytes

    public static long   start   () { return _start;             }
    public static long   stack   () { return _stack;             }
    public static long   reset   () { return _reset;             }
    public static long   dst_addr() { return _dst_addr;          }
    public static long   src_addr() { return _src_addr;          }
    public static long   words   () { return _words;             }

} // class WordCopyARM


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
