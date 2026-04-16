/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.fl;


import jxm.*;
import jxm.ugc.*;

import static jxm.ugc.ARMCortexMThumb.Label_Start;
import static jxm.ugc.ARMCortexMThumb.Label_NMI_Handler;
import static jxm.ugc.ARMCortexMThumb.Label_HardFault_Handler;
import static jxm.ugc.ARMCortexMThumb.Reg;


public class SWDAsmARMCommon {

     public static void putStdPreamble(final ARMCortexMThumb __ASM__, final long STACK_TOP) throws JXMAsmError
     {
        __ASM__

        // Vector table
        .$_word(STACK_TOP              ) // SP
        .$_word(Label_Start            ) // PC [Reset]
        .$_word(Label_NMI_Handler      ) //    [NMI_Handler]
        .$_word(Label_HardFault_Handler) //    [HardFault_Handler]

        // Interrupt handlers
        .function_NMI_Handler()
            .$b_dot()

        .function_HardFault_Handler()
            .$b_dot()

        ;;;
     }

     public static void putStdEntryPoint(final ARMCortexMThumb __ASM__, final long SRAM_START) throws JXMAsmError
     {
        __ASM__

        /*
         * The entry point
         *
         * Input    : r0 -> dstAddr (in flash or SRAM) ; if in SRAM, must be at least (SRAM_START + PROG_AREA_SIZE + 128)
         * Input    : r1 -> srcAddr (in flash or SRAM) ; if in SRAM, must be at least (SRAM_START + PROG_AREA_SIZE + 128)
         * Input    : r2 -> datSize
         * Input    : r3 -> sigAddr (in          SRAM) ;             must be at least (SRAM_START + PROG_AREA_SIZE      )
         *                  { ... } (extra parameters in SRAM after sigAddr)
         *                           ===== please refer to the 'SWDAsmARM_*.*flProgram_*' functions! =====
         *
         * Internal : r2 -> 0x00000001
         * Internal : r7 -> return value from function call
         * Internal : r8 -> srcAddr + datSize
         *
         * Internal : r4 -> work/scratch/temporary
         * Internal : r5 -> work/scratch/temporary
         * Internal : r6 -> work/scratch/temporary
         *
         * Some loader program may also use r9, ... as work/scratch/temporary.
         */
        .function_Start()
            // Disable interrupts
            .$cpsid_i(                   )

            // Clear the signal
            .$movs   (Reg.R4, 0x00       )
            .$str    (Reg.R4, Reg.R3     )

            // r8 = srcAddr + datSize
            .$mov    (Reg.R8, Reg.R1     )
            .$add    (Reg.R8, Reg.R2     )

            // r2 = 0x00000001
            .$movs   (Reg.R2, 0x01       )

            // Check if it is a read or write operation (in this case flash address always < SRAM address
            .$ldr    (Reg.R4, SRAM_START )
            .$cmp    (Reg.R0, Reg.R4     )
            .$bls    ("nvm_write_start"  )

        ;;;
     }

} // class SWDAsmARMCommon

