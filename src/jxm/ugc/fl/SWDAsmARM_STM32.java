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
 * Please refer to the comment block before the 'SWDFlashLoaderSTM32' class definition in the 'SWDFlashLoaderSTM32.java'
 * file for more details and information.
 */
public class SWDAsmARM_STM32 extends SWDAsmARMCommon {

    /*
     * Almost all STM32 MCUs have at least 4kB of SRAM with up to 8kB    flash page size.
     * Some       STM32 MCUs have only     2kB of SRAM with much smaller flash page size.
     *
     * According to the datasheets, the flash page size is always less than half the SRAM size,
     * therefore, using 1024 as PROG_AREA_SIZE should be a safe choice.
     */
    protected static final long SRAM_START         = ProgSWD.DefaultCortexM_SRAMStart;
    protected static final long PROG_AREA_SIZE     = ProgSWD.DefaultCortexM_LoaderProgramSize;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static enum ProgramVariant {
        OneBankFlash_OneKeySet,
        TwoBankFlash_OneKeySet,

        OneBankFlash_TwoKeySet
    }

    /*
     * Extra parameters in SRAM after sigAddr:
     *                                             1       2       3       4    5    6      7      8      9     10
     *     ProgramVariant.OneBankFlash_OneKeySet { KEYR  , Key1  , Key2  , CR , SR                                  }
     *     ProgramVariant.TwoBankFlash_OneKeySet { KEYR1 , Key11 , Key12 , CR1, SR1, KEYR2, Key21, Key22, CR2 , SR2 } // #### !!! TODO : NOT IMPLEMENTED YET !!! ###
     *
     *                                             1       2       3       4        5        6            7      8
     *     ProgramVariant.OneBankFlash_TwoKeySet { PEKEYR, PEKey1, PEKey2, PRGKEYR, PRGKey1, PRGKey2    , PECR, SR  }
     */

    // Location of FPEC register adresses (32-bit offset from sigAddr)
    protected static final int  FLASH_1B1K_KEYR    =  1;
    protected static final int  FLASH_1B1K_CR      =  4;
    protected static final int  FLASH_1B1K_SR      =  5;

    protected static final int  FLASH_2B1K_KEYR1   =  1;
    protected static final int  FLASH_2B1K_CR1     =  4;
    protected static final int  FLASH_2B1K_SR1     =  5;
    protected static final int  FLASH_2B1K_KEYR2   =  6;
    protected static final int  FLASH_2B1K_CR2     =  9;
    protected static final int  FLASH_2B1K_SR2     = 10;

    protected static final int  FLASH_1B2K_PEKEYR  =  1;
    protected static final int  FLASH_1B2K_PRGKEYR =  4;
    protected static final int  FLASH_1B2K_PECR    =  7;
    protected static final int  FLASH_1B2K_SR      =  8;

    // Location of flash unlock keys (32-bit offset from sigAddr)
    protected static final int  FLASH_1B1K_KEY1    =  2;
    protected static final int  FLASH_1B1K_KEY2    =  3;

    protected static final int  FLASH_2B1K_KEY11   =  2;
    protected static final int  FLASH_2B1K_KEY12   =  3;
    protected static final int  FLASH_2B1K_KEY21   =  7;
    protected static final int  FLASH_2B1K_KEY22   =  8;

    protected static final int  FLASH_1B2K_PEKEY1  =  2;
    protected static final int  FLASH_1B2K_PEKEY2  =  3;
    protected static final int  FLASH_1B2K_PRGKEY1 =  5;
    protected static final int  FLASH_1B2K_PRGKEY2 =  6;

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Single-word
    protected static void _flProgram_stm32xxx_flash_read_loop(final ARMCortexMThumb __ASM__) throws JXMAsmError
    {
        __ASM__

        /*
         * Flash read loop
         */
        .label("nvm_read_loop")
            // Load the word in flash at address [r1] and store it to SRAM at address [r0]
            .$ldr    (Reg.R4, Reg.R1                 )
            .$str    (Reg.R4, Reg.R0                 )

            // Move to the next word
            .$inc4s  (Reg.R0                         )
            .$inc4s  (Reg.R1                         )

            // Loop back if not all bytes have been read
            .$cmp    (Reg.R8, Reg.R1                 )
            .$bne    ("nvm_read_loop"                )

            // Set the signal
            .$str    (Reg.R2, Reg.R3                 )

            // Loop forever here
            .$b_dot  (                               )

        ;;;
    }

    protected static void _flProgram_stm32xxx_flash_write_suffix(final ARMCortexMThumb __ASM__) throws JXMAsmError
    {
        __ASM__

        /*
         * Flash write suffix code
         */
            // Loop back if not all bytes have been written
            .$cmp    (Reg.R8, Reg.R1                 )
            .$bne    ("nvm_write_loop"               )

            // Set the signal
            .$str    (Reg.R2, Reg.R3                 )

            // Loop forever here
            .$b_dot  (                               )

        .label("nvm_write_error")
            // Set the signal
            .$str    (Reg.R5, Reg.R3                 )

            // Loop forever here
            .$b_dot  (                               )

        ;;;
    }

    protected static void _flProgram_stm32xxx_flash_write_wait_busy(final ARMCortexMThumb __ASM__, final int FLASH_SR, final long SR_BUSY_BITS) throws JXMAsmError
    {
        __ASM__

        /*
         * Wait for the flash operation to complete
         *
         * Output : r7 -> the status bits
         */
        .function("nvm_write_wait_busy")
            .$push   (Reg.R4, Reg.R5                 )
            .$ldr    (Reg.R4, Reg.R3, 4 * FLASH_SR   )
            .$ldri   (Reg.R5, SR_BUSY_BITS           )

        .label("nvm_write_wait_busy_loop")
            .$ldr    (Reg.R7, Reg.R4                 )
            .$tst    (Reg.R7, Reg.R5                 )
            .$bne    ("nvm_write_wait_busy_loop"     )

            .$pop    (Reg.R4, Reg.R5                 )
            .$bx     (Reg.LR                         )

        ;;;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static void _flProgram_stm32fxx_flash_write_prefix_1b1k(final ARMCortexMThumb __ASM__, final int FLASH_KEYR, final int FLASH_KEY1, final int FLASH_KEY2, final int FLASH_CR, final long CR_PG_BITS, final long SR_ERROR_BITS) throws JXMAsmError
    {
        __ASM__

        /*
         * Flash write prefix code
         */
        .label("nvm_write_start")
            // Wait until the flash is ready
            .$bl     ("nvm_write_wait_busy"          )

            //*
            // Check the error bits
            .$ldr    (Reg.R5, -100                   )
            /*
            .$movs   (Reg.R5, Reg.R7                 ) // Return the negated actual error bits
            .$negs   (Reg.R5, Reg.R5                 )
            //*/
            .$ldri   (Reg.R6, SR_ERROR_BITS          )
            .$tst    (Reg.R7, Reg.R6                 )
            .$bne    ("nvm_write_error"              )

            // Unlock the flash
            .$ldr    (Reg.R4, Reg.R3, 4 * FLASH_KEYR )
            .$ldr    (Reg.R5, Reg.R3, 4 * FLASH_KEY1 )
            .$ldr    (Reg.R6, Reg.R3, 4 * FLASH_KEY2 )
            .$str    (Reg.R5, Reg.R4                 )
            .$str    (Reg.R6, Reg.R4                 )
            .$bl     ("nvm_write_wait_busy"          )

            // Enable the programming bit(s) in the flash control register
            .$ldr    (Reg.R4, Reg.R3, 4 * FLASH_CR   )
            .$ldri   (Reg.R5, CR_PG_BITS             )
            .$str    (Reg.R5, Reg.R4                 )
            .$bl     ("nvm_write_wait_busy"          )
            //*/

        ;;;;
    }

    // Half-word
    protected static void _flProgram_stm32fxx_hw_1b1k(final ARMCortexMThumb __ASM__, final long CR_PG_BITS, final long SR_BUSY_BITS, final long SR_ERROR_BITS) throws JXMAsmError
    {
        /*
         * Use the common flash read code
         */
        _flProgram_stm32xxx_flash_read_loop(__ASM__);

        /*
         * Use the common 1B-1K flash write prefix code
         */
        _flProgram_stm32fxx_flash_write_prefix_1b1k(__ASM__, FLASH_1B1K_KEYR, FLASH_1B1K_KEY1, FLASH_1B1K_KEY2, FLASH_1B1K_CR, CR_PG_BITS, SR_ERROR_BITS);

        __ASM__

        /*
         * Flash write loop
         */
        .label("nvm_write_loop")
            // Load the word in SRAM at address [r1]
            .$ldr    (Reg.R4, Reg.R1                 )

            // Store the lower half-word to flash at address [r0]
            .$strh   (Reg.R4, Reg.R0                 )
            .$dsb_sy (                               )
            .$bl     ("nvm_write_wait_busy"          )

            // Check the error bits
            .$ldr    (Reg.R5, -200                   )
            /*
            .$movs   (Reg.R5, Reg.R7                 ) // Return the negated actual error bits
            .$negs   (Reg.R5, Reg.R5                 )
            //*/
            .$ldri   (Reg.R6, SR_ERROR_BITS          )
            .$tst    (Reg.R7, Reg.R6                 )
            .$bne    ("nvm_write_error"              )

            // Increment the flash address by 2
            .$inc2s  (Reg.R0                         )

            // Store the upper half-word to flash at address [r0]
            .$lsrs   (Reg.R4, Reg.R4, 16             )
            .$strh   (Reg.R4, Reg.R0                 )
            .$dsb_sy (                               )
            .$bl     ("nvm_write_wait_busy"          )

            // Check the error bits
            .$ldr    (Reg.R5, -300                   )
            /*
            .$movs   (Reg.R5, Reg.R7                 ) // Return the negated actual error bits
            .$negs   (Reg.R5, Reg.R5                 )
            //*/
            .$ldri   (Reg.R6, SR_ERROR_BITS          )
            .$tst    (Reg.R7, Reg.R6                 )
            .$bne    ("nvm_write_error"              )

            // Increment the flash address by 2
            .$inc2s  (Reg.R0                         )

            // Move to the next word in SRAM
            .$inc4s  (Reg.R1                         )

        ;;;

        /*
         * Use the common flash write suffix code
         */
        _flProgram_stm32xxx_flash_write_suffix(__ASM__);

        /*
         * Use the common flash wait busy code
         */
        _flProgram_stm32xxx_flash_write_wait_busy(__ASM__, FLASH_1B1K_SR, SR_BUSY_BITS);
    }

    // Double-word
    protected static void _flProgram_stm32fxx_dw_1b1k(final ARMCortexMThumb __ASM__, final long CR_PG_BITS, final long SR_BUSY_BITS, final long SR_ERROR_BITS) throws JXMAsmError
    {
        /*
         * Use the common flash read code
         */
        _flProgram_stm32xxx_flash_read_loop(__ASM__);

        /*
         * Use the common 1B-1K flash write prefix code
         */
        _flProgram_stm32fxx_flash_write_prefix_1b1k(__ASM__, FLASH_1B1K_KEYR, FLASH_1B1K_KEY1, FLASH_1B1K_KEY2, FLASH_1B1K_CR, CR_PG_BITS, SR_ERROR_BITS);

        __ASM__

        /*
         * Flash write loop
         */
        .label("nvm_write_loop")
            // Load the lower word in SRAM at address [r1 + 0] and store it to flash at address [r0 + 0]
            .$ldr    (Reg.R4, Reg.R1, 0              )
            .$str    (Reg.R4, Reg.R0, 0              )

            // Load the upper word in SRAM at address [r1 + 4] and store it to flash at address [r0 + 4]
            .$ldr    (Reg.R4, Reg.R1, 4              )
            .$str    (Reg.R4, Reg.R0, 4              )

            // Wait until the flash is ready
            .$bl     ("nvm_write_wait_busy"          )

            // Check the error bits
            .$ldr    (Reg.R5, -200                   )
            /*
            .$movs   (Reg.R5, Reg.R7                 ) // Return the negated actual error bits
            .$negs   (Reg.R5, Reg.R5                 )
            //*/
            .$ldri   (Reg.R6, SR_ERROR_BITS          )
            .$tst    (Reg.R7, Reg.R6                 )
            .$bne    ("nvm_write_error"              )

            // Move to the next double word
            .$inc8s  (Reg.R0                         )
            .$inc8s  (Reg.R1                         )

        ;;;

        /*
         * Use the common flash write suffix code
         */
        _flProgram_stm32xxx_flash_write_suffix(__ASM__);

        /*
         * Use the common flash wait busy code
         */
        _flProgram_stm32xxx_flash_write_wait_busy(__ASM__, FLASH_1B1K_SR, SR_BUSY_BITS);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static void _flProgram_stm32lxx_flash_write_prefix_1b2k(final ARMCortexMThumb __ASM__, final int FLASH_KEYR1, final int FLASH_KEY11, final int FLASH_KEY12, final int FLASH_KEYR2, final int FLASH_KEY21, final int FLASH_KEY22, final int FLASH_CR, final long CR_PG_BITS, final long SR_ERROR_BITS) throws JXMAsmError
    {
        __ASM__

        /*
         * Flash write prefix code
         */
        .label("nvm_write_start")
            // Wait until the flash is ready
            .$bl     ("nvm_write_wait_busy"          )

            // Check the error bits
            .$ldr    (Reg.R5, -100                   )
            /*
            .$movs   (Reg.R5, Reg.R7                 ) // Return the negated actual error bits
            .$negs   (Reg.R5, Reg.R5                 )
            //*/
            .$ldri   (Reg.R6, SR_ERROR_BITS          )
            .$tst    (Reg.R7, Reg.R6                 )
            .$bne    ("nvm_write_error"              )

            // Unlock the flash
            .$ldr    (Reg.R4, Reg.R3, 4 * FLASH_KEYR1)
            .$ldr    (Reg.R5, Reg.R3, 4 * FLASH_KEY11)
            .$ldr    (Reg.R6, Reg.R3, 4 * FLASH_KEY12)
            .$str    (Reg.R5, Reg.R4                 )
            .$str    (Reg.R6, Reg.R4                 )
            .$bl     ("nvm_write_wait_busy"          )

            .$ldr    (Reg.R4, Reg.R3, 4 * FLASH_KEYR2)
            .$ldr    (Reg.R5, Reg.R3, 4 * FLASH_KEY21)
            .$ldr    (Reg.R6, Reg.R3, 4 * FLASH_KEY22)
            .$str    (Reg.R5, Reg.R4                 )
            .$str    (Reg.R6, Reg.R4                 )
            .$bl     ("nvm_write_wait_busy"          )

            // Enable the programming bit(s) in the flash control register
            .$ldr    (Reg.R4, Reg.R3, 4 * FLASH_CR   )
            .$ldri   (Reg.R5, CR_PG_BITS             )
            .$str    (Reg.R5, Reg.R4                 )
            .$bl     ("nvm_write_wait_busy"          )

        ;;;;
    }

    // Single-word
    protected static void _flProgram_stm32lxx_sw_1b2k(final ARMCortexMThumb __ASM__, final long CR_PG_BITS, final long SR_BUSY_BITS, final long SR_ERROR_BITS) throws JXMAsmError
    {
        /*
         * Use the common flash read code
         */
        _flProgram_stm32xxx_flash_read_loop(__ASM__);

        /*
         * Use the common 1B-2K flash write prefix code
         */
        _flProgram_stm32lxx_flash_write_prefix_1b2k(__ASM__, FLASH_1B2K_PEKEYR, FLASH_1B2K_PEKEY1, FLASH_1B2K_PEKEY2, FLASH_1B2K_PRGKEYR, FLASH_1B2K_PRGKEY1, FLASH_1B2K_PRGKEY2, FLASH_1B2K_PECR, CR_PG_BITS, SR_ERROR_BITS);

        __ASM__

        /*
         * Flash write loop
         */
        .label("nvm_write_loop")
            // Load the word in SRAM at address [r1] and store it to flash at address [r0]
            .$ldmia  (Reg.R1.wb, Reg.R4              )
            .$stmia  (Reg.R0.wb, Reg.R4              )

            // Loop back if not all bytes have been written
            .$cmp    (Reg.R8, Reg.R1                 )
            .$bne    ("nvm_write_loop"               )

            // Wait until the flash is ready
            .$bl     ("nvm_write_wait_busy"          )

            // Check the error bits
            .$ldr    (Reg.R5, -200                   )
            /*
            .$movs   (Reg.R5, Reg.R7                 ) // Return the negated actual error bits
            .$negs   (Reg.R5, Reg.R5                 )
            //*/
            .$ldri   (Reg.R6, SR_ERROR_BITS          )
            .$tst    (Reg.R7, Reg.R6                 )
            .$bne    ("nvm_write_error"              )

            // Set the signal
            .$str    (Reg.R2, Reg.R3                 )

            // Loop forever here
            .$b_dot  (                               )

        .label("nvm_write_error")
            // Set the signal
            .$str    (Reg.R5, Reg.R3                 )

            // Loop forever here
            .$b_dot  (                               )

        ;;;

        /*
         * Use the common flash wait busy code
         */
        _flProgram_stm32xxx_flash_write_wait_busy(__ASM__, FLASH_1B2K_SR, SR_BUSY_BITS);
    }

    // Single-word
    protected static void _elProgram_stm32lxx_sw_1b2k(final ARMCortexMThumb __ASM__, final long CR_PG_BITS, final long SR_BUSY_BITS, final long SR_ERROR_BITS) throws JXMAsmError
    {
        /*
         * Use the common flash read code (it is also usable for EEPROM)
         */
        _flProgram_stm32xxx_flash_read_loop(__ASM__);

        /*
         * Use the common 1B-2K flash write prefix code (it is also usable for EEPROM)
         */
        _flProgram_stm32lxx_flash_write_prefix_1b2k(__ASM__, FLASH_1B2K_PEKEYR, FLASH_1B2K_PEKEY1, FLASH_1B2K_PEKEY2, FLASH_1B2K_PRGKEYR, FLASH_1B2K_PRGKEY1, FLASH_1B2K_PRGKEY2, FLASH_1B2K_PECR, CR_PG_BITS, SR_ERROR_BITS);

        __ASM__

        /*
         * EEPROM write loop
         */
        .label("nvm_write_loop")
            // Load the word in SRAM at address [r1] and store it to EEPROM at address [r0]
            .$ldmia  (Reg.R1.wb, Reg.R4              )
            .$stmia  (Reg.R0.wb, Reg.R4              )

            // Wait until the EEPROM is ready
            .$bl     ("nvm_write_wait_busy"          )

            // Check the error bits
            .$ldr    (Reg.R5, -200                   )
            /*
            .$movs   (Reg.R5, Reg.R7                 ) // Return the negated actual error bits
            .$negs   (Reg.R5, Reg.R5                 )
            //*/
            .$ldri   (Reg.R6, SR_ERROR_BITS          )
            .$tst    (Reg.R7, Reg.R6                 )
            .$bne    ("nvm_write_error"              )

            // Loop back if not all bytes have been written
            .$cmp    (Reg.R8, Reg.R1                 )
            .$bne    ("nvm_write_loop"               )

            // Set the signal
            .$str    (Reg.R2, Reg.R3                 )

            // Loop forever here
            .$b_dot  (                               )

        .label("nvm_write_error")
            // Set the signal
            .$str   (Reg.R5, Reg.R3                 )

            // Loop forever here
            .$b_dot (                               )

        ;;;

        /*
         * Use the common flash wait busy code (the same code can also be used for EEPROM)
         */
        _flProgram_stm32xxx_flash_write_wait_busy(__ASM__, FLASH_1B2K_SR, SR_BUSY_BITS);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Flash-word
    protected static void _flProgram_stm32hxx_fw_1b1k(final ARMCortexMThumb __ASM__, final int fwSize, final long CR_PG_BITS, final long SR_BUSY_BITS, final long SR_ERROR_BITS) throws JXMAsmError
    {
        /*
         * Use the common flash read code
         */
        _flProgram_stm32xxx_flash_read_loop(__ASM__);

        /*
         * Use the common 1B-1K flash write prefix code (from STM32FXX)
         */
        _flProgram_stm32fxx_flash_write_prefix_1b1k(__ASM__, FLASH_1B1K_KEYR, FLASH_1B1K_KEY1, FLASH_1B1K_KEY2, FLASH_1B1K_CR, CR_PG_BITS, SR_ERROR_BITS);

        __ASM__

        /*
         * Flash write loop
         */
        .label("nvm_write_loop")

            // Load the words in SRAM at address [r1 + n] and store them to flash at address [r0 + n]
            .$ldri   (Reg.R9, fwSize / 4             ) // Data are processed in words (32 bits) (4 bytes)
        .label("nvm_write_fw_loop")
            .$dsb_sy (                               )
            .$ldr_pst(Reg.R4, Reg.R1, 4              )
            .$str_pst(Reg.R4, Reg.R0, 4              )
            .$dsb_sy (                               )
            .$dec1w  (Reg.R9                         )
            .$tst    (Reg.R9, Reg.R9                 )
            .$bne    ("nvm_write_fw_loop"            )

            // Wait until the flash is ready
            .$bl     ("nvm_write_wait_busy"          )

            // Check the error bits
            .$ldr    (Reg.R5, -200                   )
            /*
            .$movs   (Reg.R5, Reg.R7                 ) // Return the negated actual error bits
            .$negs   (Reg.R5, Reg.R5                 )
            //*/
            .$ldri   (Reg.R6, SR_ERROR_BITS          )
            .$tst    (Reg.R7, Reg.R6                 )
            .$bne    ("nvm_write_error"              )

        ;;;

        /*
         * Use the common flash write suffix code
         */
        _flProgram_stm32xxx_flash_write_suffix(__ASM__);

        /*
         * Use the common flash wait busy code
         */
        _flProgram_stm32xxx_flash_write_wait_busy(__ASM__, FLASH_1B1K_SR, SR_BUSY_BITS);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Make all 'e/flProgram_stm32*()' functions able to use custom 'sramStart'? !!! ####

    // Bits (STM32F1X)
    protected static final long F1_CR_PG_BITS      = 0x00000001L; /*                   [0]PG   */
    protected static final long F1_SR_BUSY_BITS    = 0x00000001L; /*                   [0]BUSY */
    protected static final long F1_SR_ERROR_BITS   = 0x00000014L; /* [4]WRPRT [2]PGERR         */

    public static long[] flProgram_stm32f1x(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        return ( new ARMCortexMThumb(CPU.M0) {{

            putStdPreamble  (this, SRAM_START + PROG_AREA_SIZE);
            putStdEntryPoint(this, SRAM_START                 );

            _flProgram_stm32fxx_hw_1b1k(this, F1_CR_PG_BITS, F1_SR_BUSY_BITS, F1_SR_ERROR_BITS);

        }} ).link(SRAM_START, dumpDisassemblyAndArray);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Bits (STM32F2X)
    protected static final long F2_CR_PG_BITS      = 0x00000101L; /*          [9:8]PSIZE                                         [0]PG */
    protected static final long F2_SR_BUSY_BITS    = 0x00010000L; /* [16]BUSY                                                          */
    protected static final long F2_SR_ERROR_BITS   = 0x000000F0L; /*                     [7]PGSERR [6]PGPERR [5]PGAERR [4]WRPERR       */

    public static long[] flProgram_stm32f2x(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        return ( new ARMCortexMThumb(CPU.M0) {{

            putStdPreamble  (this, SRAM_START + PROG_AREA_SIZE);
            putStdEntryPoint(this, SRAM_START                 );

            _flProgram_stm32fxx_hw_1b1k(this, F2_CR_PG_BITS, F2_SR_BUSY_BITS, F2_SR_ERROR_BITS);

        }} ).link(SRAM_START, dumpDisassemblyAndArray);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Bits (STM32L4X)
    protected static final long L4_CR_PG_BITS      = 0x00000001L; /*                                                                      [0]PG */
    protected static final long L4_SR_BUSY_BITS    = 0x00010000L; /* [16]BUSY                                                                   */
    protected static final long L4_SR_ERROR_BITS   = 0x000000FAL; /*          [7]PGSERR [6]SIZERR [5]PGAERR [4]WRPERR [3]PROGERR [1]OPERR       */

    public static long[] flProgram_stm32l4x(final long sramStart, final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        return ( new ARMCortexMThumb(CPU.M0) {{

            putStdPreamble  (this, sramStart + PROG_AREA_SIZE);
            putStdEntryPoint(this, sramStart                 );

            _flProgram_stm32fxx_dw_1b1k(this, L4_CR_PG_BITS, L4_SR_BUSY_BITS, L4_SR_ERROR_BITS);

        }} ).link(sramStart, dumpDisassemblyAndArray);
    }

    public static long[] flProgram_stm32l4x(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    { return flProgram_stm32l4x(SRAM_START, dumpDisassemblyAndArray); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Bits (STM32L*X)
    protected static final long LX_CR_PG_BITS_F    = 0x00000408L; /*                           [10]FPRG                               [3]PROG         */
    protected static final long LX_SR_BUSY_BITS_F  = 0x00000001L; /*                                                                          [0]BUSY */
    protected static final long LX_SR_ERROR_BITS_F = 0x00030700L; /* [17]FWWERR [16]NOTZEROERR [10]SIZERR [9]PGAERR [8]WRPERR                         */

    protected static final long L0_CR_PG_BITS_E    = 0x00000110L; /*                                                [8]FIX    [4]DATA                 */
    protected static final long L0_SR_BUSY_BITS_E  = 0x00000001L; /*                                                                          [0]BUSY */
    protected static final long L0_SR_ERROR_BITS_E = 0x00020500L; /* [17]FWWERR                [10]SIZERR           [8]WRPERR                         */

    protected static final long L1_CR_PG_BITS_E    = 0x00000100L; /*                                                [8]FTDW                           */
    protected static final long L1_SR_BUSY_BITS_E  = 0x00000001L; /*                                                                          [0]BUSY */
    protected static final long L1_SR_ERROR_BITS_E = 0x00000500L; /*                           [10]SIZERR           [8]WRPERR                         */

    public static long[] flProgram_stm32lx(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        return ( new ARMCortexMThumb(CPU.M0) {{

            putStdPreamble  (this, SRAM_START + PROG_AREA_SIZE);
            putStdEntryPoint(this, SRAM_START                 );

            // ##### ??? TODO : Unset FPRG and PROG after writing ??? #####
            _flProgram_stm32lxx_sw_1b2k(this, LX_CR_PG_BITS_F, LX_SR_BUSY_BITS_F, LX_SR_ERROR_BITS_F);

        }} ).link(SRAM_START, dumpDisassemblyAndArray);
    }

    public static long[] elProgram_stm32l0x(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        return ( new ARMCortexMThumb(CPU.M0) {{

            putStdPreamble  (this, SRAM_START + PROG_AREA_SIZE);
            putStdEntryPoint(this, SRAM_START                 );

            // ##### ??? TODO : Unset FIX and DATA after writing ??? #####
            _elProgram_stm32lxx_sw_1b2k(this, L0_CR_PG_BITS_E, L0_SR_BUSY_BITS_E, L0_SR_ERROR_BITS_E);

        }} ).link(SRAM_START, dumpDisassemblyAndArray);
    }

    public static long[] elProgram_stm32l1x(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        return ( new ARMCortexMThumb(CPU.M0) {{

            putStdPreamble  (this, SRAM_START + PROG_AREA_SIZE);
            putStdEntryPoint(this, SRAM_START                 );

            // ##### ??? TODO : Unset FTDW after writing ??? #####
            _elProgram_stm32lxx_sw_1b2k(this, L1_CR_PG_BITS_E, L1_SR_BUSY_BITS_E, L1_SR_ERROR_BITS_E);

        }} ).link(SRAM_START, dumpDisassemblyAndArray);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Bits (STM32H7X)
    protected static final long H7_CR_PG_BITS      = 0x00000032L; /*                                                                 [5:4]PSIZE       [1]PG */
    protected static final long H7_SR_BUSY_BITS    = 0x00000004L; /*                                                                            [2]QW       */
    protected static final long H7_SR_ERROR_BITS   = 0x07EE0000L; /* [26:21][DBECC|SNECC|RDS|RDP|OP|INC]ERR [19:16][STRB|PGS|WRP]ERR                        */

    public static long[] flProgram_stm32h7x(final boolean varAB, final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        return ( new ARMCortexMThumb(CPU.M3) {{

            putStdPreamble  (this, SRAM_START + PROG_AREA_SIZE);
            putStdEntryPoint(this, SRAM_START                 );

            _flProgram_stm32hxx_fw_1b1k( this, varAB ? 16 : 32, SWDFlashLoaderSTM32._stm32h7x_flash_cr(varAB, H7_CR_PG_BITS), H7_SR_BUSY_BITS, H7_SR_ERROR_BITS );

        }} ).link(SRAM_START, dumpDisassemblyAndArray);
    }

} // class SWDAsmARM_STM32
