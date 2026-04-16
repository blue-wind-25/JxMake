/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.fl;


import jxm.*;
import jxm.ugc.*;

import static jxm.ugc.ARMCortexMThumb.CPU;
import static jxm.ugc.ARMCortexMThumb.Reg;


/*
 * Please refer to the comment block before the 'SWDFlashLoaderNRF5' class definition in the 'SWDFlashLoaderNRF5.java'
 * file for more details and information.
 */
public class SWDAsmARM_NRF5 extends SWDAsmARMCommon {

    /*
     * All NRF5X MCUs should have at least 4kB of SRAM.
     *
     * According to the datasheets, the flash page size is always less than half the SRAM size,
     * therefore, using 1024 as PROG_AREA_SIZE should be a safe choice.
     */
    protected static final long SRAM_START       = ProgSWD.DefaultCortexM_SRAMStart;
    protected static final long PROG_AREA_SIZE   = ProgSWD.DefaultCortexM_LoaderProgramSize;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * Extra parameters in SRAM after sigAddr:
     *     { RR0, Reload, CONFIG, READY }
     */

    // Location of register adresses (32-bit offset from sigAddr)
    protected static final int  WDOG_RR0         = 1;

    protected static final int  FLASH_CONFIG     = 3;
    protected static final int  FLASH_READY      = 4;

    // Location of watchdog reload value (32-bit offset from sigAddr)
    protected static final int  WDOG_RELOAD      = 2;

    // Bits
    protected static final long CONFIG_WEN_BITS  = 0x00000001L; /* [1:0]WEN   */
    protected static final long READY_READY_BITS = 0x00000001L; /*   [0]READY */

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long[] flProgram_nrf5x(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        return ( new ARMCortexMThumb(CPU.M0) {{

            putStdPreamble  (this, SRAM_START + PROG_AREA_SIZE);
            putStdEntryPoint(this, SRAM_START                 );

            /*
             * Flash read loop
             */
            label("nvm_read_loop");
                // Reload the watchdog
                $ldr  (Reg.R4   , Reg.R3, 4 * WDOG_RR0   );
                $ldr  (Reg.R5   , Reg.R3, 4 * WDOG_RELOAD);
                $str  (Reg.R5   , Reg.R4                 );

                // Load the word in flash at address [r1] and store it to SRAM at address [r0]
                $ldmia(Reg.R1.wb, Reg.R4                 );
                $stmia(Reg.R0.wb, Reg.R4                 );

                // Loop back if not all bytes have been  written
                $cmp  (Reg.R8   , Reg.R1                 );
                $bne  ("nvm_read_loop"                   );

                // Set the signal
                $str  (Reg.R2   , Reg.R3                 );

                // Loop forever here
                $b_dot(                                  );

            /*
             * Flash write loop
             */
            label("nvm_write_start");
                // Wait until the flash is ready
                $bl   ("nvm_write_wait_busy"              );

                // Enable the programming bit(s) in the flash control register
                $ldr  (Reg.R4   , Reg.R3, 4 * FLASH_CONFIG);
                $ldri (Reg.R5   , CONFIG_WEN_BITS         );
                $str  (Reg.R5   , Reg.R4                  );
                $bl   ("nvm_write_wait_busy"              );

            label("nvm_write_loop");
                // Reload the watchdog
                $ldr  (Reg.R4   , Reg.R3, 4 * WDOG_RR0    );
                $ldr  (Reg.R5   , Reg.R3, 4 * WDOG_RELOAD );
                $str  (Reg.R5   , Reg.R4                  );

                // Load the word in SRAM at address [r1] and store it to flash at address [r0]
                $ldmia(Reg.R1.wb, Reg.R4                  );
                $stmia(Reg.R0.wb, Reg.R4                  );
                $bl   ("nvm_write_wait_busy"              );

                // Loop back if not all bytes have been written
                $cmp  (Reg.R8   , Reg.R1                  );
                $bne  ("nvm_write_loop"                   );

                // Disable the programming bit(s) in the flash control register
                $ldr  (Reg.R4   , Reg.R3, 4 * FLASH_CONFIG);
                $movs (Reg.R5   , 0x00                    );
                $str  (Reg.R5   , Reg.R4                  );
                $bl   ("nvm_write_wait_busy"              );

                // Set the signal
                $str  (Reg.R2   , Reg.R3                  );

                // Loop forever here
                $b_dot(                                   );

            /*
             * Wait for the flash operation to complete
             */
            function("nvm_write_wait_busy");
                $push (Reg.R4   , Reg.R5, Reg.R6          );
                $ldr  (Reg.R4   , Reg.R3, 4 * FLASH_READY );
                $ldri (Reg.R5   , READY_READY_BITS        );

            label("nvm_write_wait_busy_loop");
                $ldr  (Reg.R6   , Reg.R4                  );
                $tst  (Reg.R6   , Reg.R5                  );
                $beq  ("nvm_write_wait_busy_loop"         ); // ##### !!! TODO : Use timeout; HOW? !!! #####

                $pop  (Reg.R4   , Reg.R5, Reg.R6          );
                $bx   (Reg.LR                             );

        }} ).link(SRAM_START, dumpDisassemblyAndArray);

    }

} // class SWDAsmARM_NRF5
