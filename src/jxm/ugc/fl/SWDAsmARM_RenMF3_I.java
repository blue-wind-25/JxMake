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
 * Please refer to the comment block before the 'SWDFlashLoaderRenMF3' class definition in the 'SWDFlashLoaderRenMF3.java'
 * file for more details and information.
 */
public class SWDAsmARM_RenMF3_I extends SWDFlashLoader {

    protected static final long SRAM_START                   = ProgSWD.DefaultCortexM_SRAMStart;
    protected static final long PROG_AREA_SIZE               = ProgSWD.DefaultCortexM_LoaderProgramSize;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final XVI  _mf3_xvi_LoaderProgramFlash  = XVI._0200;
    protected static final XVI  _mf3_xvi_LoaderProgramEEPROM = XVI._0201;
    protected static final XVI  _mf3_xvi_LoaderProgramFRun   = XVI._0202;

    protected static final XVI  _mf3_xvi_FLASH_DST_ADDR      = XVI._0203; // NOTE : It is also used for EEPROM
    protected static final XVI  _mf3_xvi_FLASH_READ_SIZE     = XVI._0204; // ---
    protected static final XVI  _mf3_xvi_SignalWorkerCommand = XVI._0205;
    protected static final XVI  _mf3_xvi_SignalJobState      = XVI._0206;

    // cat ../../../../0_excluded_directory/personal/Renesas_MF3_Regs.txt | awk '{addr=$0; gsub(" ", "", addr); addr=substr(addr, 1, length(addr) - 1); getline; bofs=and(substr(addr, length(addr), 1), 3); getline; desc=$0; getline; name=$0; getline; bits=$0; getline; getline; fname=sprintf("%s_z%02d_o%d", name, bits, bofs); printf "    protected static final long _mf3_FLASH_%-17s = 0x%sL - %d; // %s\n", fname, addr, bofs, desc }'
    protected static final long _mf3_FLASH_DFLCTL_z08_o0     = 0x407EC090L - 0; // Data Flash Control Register
    protected static final long _mf3_FLASH_FENTRYR_z16_o2    = 0x407EFFB2L - 2; // Flash P/E Mode Entry Register
    protected static final long _mf3_FLASH_FPR_z08_o0        = 0x407EC180L - 0; // Protection Unlock Register
    protected static final long _mf3_FLASH_FPSR_z08_o0       = 0x407EC184L - 0; // Protection Unlock Status Register
    protected static final long _mf3_FLASH_FPMCR_z08_o0      = 0x407EC100L - 0; // Flash P/E Mode Control Register
    protected static final long _mf3_FLASH_FISR_z08_o0       = 0x407EC1D8L - 0; // Flash Initial Setting Register
    protected static final long _mf3_FLASH_FRESETR_z08_o0    = 0x407EC124L - 0; // Flash Reset Register
    protected static final long _mf3_FLASH_FASR_z08_o0       = 0x407EC104L - 0; // Flash Area Select Register
    protected static final long _mf3_FLASH_FCR_z08_o0        = 0x407EC114L - 0; // Flash Control Register
    protected static final long _mf3_FLASH_FEXCR_z08_o0      = 0x407EC1DCL - 0; // Flash Extra Area Control Register
    protected static final long _mf3_FLASH_FSARH_z16_o0      = 0x407EC110L - 0; // Flash Processing Start Address Register H
    protected static final long _mf3_FLASH_FSARL_z16_o0      = 0x407EC108L - 0; // Flash Processing Start Address Register L
    protected static final long _mf3_FLASH_FEARH_z16_o0      = 0x407EC120L - 0; // Flash Processing End Address Register H
    protected static final long _mf3_FLASH_FEARL_z16_o0      = 0x407EC118L - 0; // Flash Processing End Address Register L
    protected static final long _mf3_FLASH_FWBL0_z16_o0      = 0x407EC130L - 0; // Flash Write Buffer Register L0
    protected static final long _mf3_FLASH_FWBH0_z16_o0      = 0x407EC138L - 0; // Flash Write Buffer Register H0
    protected static final long _mf3_FLASH_FWBL1_z16_o0      = 0x407EC140L - 0; // Flash Write Buffer Register L1 (except RA2A1)
    protected static final long _mf3_FLASH_FWBH1_z16_o0      = 0x407EC144L - 0; // Flash Write Buffer Register H1 (except RA2A1)
    protected static final long _mf3_FLASH_FRBL0_z16_o0      = 0x407EC188L - 0; // Flash Read Buffer Register L0
    protected static final long _mf3_FLASH_FRBH0_z16_o0      = 0x407EC190L - 0; // Flash Read Buffer Register H0
    protected static final long _mf3_FLASH_FRBL1_z16_o0      = 0x407EC148L - 0; // Flash Read Buffer Register L1 (except RA2A1)
    protected static final long _mf3_FLASH_FRBH1_z16_o0      = 0x407EC14CL - 0; // Flash Read Buffer Register H1 (except RA2A1)
    protected static final long _mf3_FLASH_FSTATR00_z16_o0   = 0x407EC128L - 0; // Flash Status Register 00
    protected static final long _mf3_FLASH_FSTATR01_z16_o0   = 0x407EC13CL - 0; // Flash Status Register 01
    protected static final long _mf3_FLASH_FSTATR2_z16_o0    = 0x407EC1F0L - 0; // Flash Status Register 02
    protected static final long _mf3_FLASH_FSTATR1_z08_o0    = 0x407EC12CL - 0; // Flash Status Register1
    protected static final long _mf3_FLASH_FEAMH_z16_o0      = 0x407EC1E8L - 0; // Flash Error Address Monitor Register H
    protected static final long _mf3_FLASH_FEAML_z16_o0      = 0x407EC1E0L - 0; // Flash Error Address Monitor Register L
    protected static final long _mf3_FLASH_FSCMR_z16_o0      = 0x407EC1C0L - 0; // Flash Start-Up Setting Monitor Register
    protected static final long _mf3_FLASH_FAWSMR_z16_o0     = 0x407EC1C8L - 0; // Flash Access Window Start Address Monitor Register
    protected static final long _mf3_FLASH_FAWEMR_z16_o0     = 0x407EC1D0L - 0; // Flash Access Window End Address Monitor Register
    protected static final long _mf3_FLASH_FLWAITR_z08_o0    = 0x407EFFC0L - 0; // Flash Wait Cycle Register (RA2A1 Only)

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static void _buildLoaderProgram_FlashEEPROM_Write(final ARMCortexMThumb __ASM__) throws JXMAsmError
    {
        __ASM__

            //*
            // Perform write
            .$ldri (Reg.R0, _mf3_FLASH_FCR_z08_o0    ) // Set    OPST ; Set CMD to 0b0001
            .$ldri (Reg.R1, 0b10000001               ) // ---
            .$strb (Reg.R1, Reg.R0                   ) // ---
            .$ldri (Reg.R0, _mf3_FLASH_FSTATR1_z08_o0) // Wait   FRDY   // ##### !!! TODO : Use timeout; HOW? !!! #####
            .$ldri (Reg.R2, 0b01000000               ) // ---
        .label("wfe_wait_frdy")                        // ---
            .$ldrb (Reg.R1, Reg.R0                   ) // ---
            .$tst  (Reg.R1, Reg.R2                   ) // ---
            .$bzr  ("wfe_wait_frdy"                  ) // ---
            .$ldri (Reg.R0, _mf3_FLASH_FCR_z08_o0    ) // Clear  OPST CMD
            .$ldri (Reg.R1, 0                        ) // ---
            .$strb (Reg.R1, Reg.R0                   ) // ---
            .$ldri (Reg.R0, _mf3_FLASH_FSTATR1_z08_o0) // Wait  !FRDY   // ##### !!! TODO : Use timeout; HOW? !!! #####
            .$ldri (Reg.R2, 0b01000000               ) // ---
        .label("wfe_wait_nfrdy")                       // ---
            .$ldrb (Reg.R1, Reg.R0                   ) // ---
            .$tst  (Reg.R1, Reg.R2                   ) // ---
            .$bnz  ("wfe_wait_nfrdy"                 ) // ---
            .$ldri (Reg.R0, _mf3_FLASH_FSTATR2_z16_o0) // Get and check ILGLERR and PRGERR
            .$ldri (Reg.R2, 0b00010010               ) // ---
            .$ldrh (Reg.R1, Reg.R0                   ) // ---
            .$tst  (Reg.R1, Reg.R2                   ) // ---
            .$bzr  ("wfe_write_ok"                   ) // ---
            .$ldri (Reg.R7, -100                     ) // Set the error flag
            .$b    ("wfe_write_done"                 )
        .label("wfe_write_ok")
            //*/

            // Loop back if not all bytes have been written
            .$cmp  (Reg.R5, Reg.R6                   )
            .$blo  ("wfe_write_loop"                 )
            .$ldri (Reg.R7, 0                        ) // Clear the error flag

            // Break and wait here
        .label("wfe_write_done")
            .$bkpt (0                                )
            .$b    ("wfe_write_start"                )

        ;;;
    }

    protected static ARMCortexMThumb _buildLoaderProgram_Flash(final boolean writeSize8) throws JXMAsmError
    {
        return new ARMCortexMThumb(CPU.M0) {{

            SWDAsmARMCommon.putStdPreamble(this, SRAM_START + PROG_AREA_SIZE);

            function_Start();

                /*
                 * The entry point
                 *
                 * Input    : r5 -> begSrcAddr (in SRAM )
                 * Input    : r6 -> endSrcAddr (in SRAM )
                 *
                 * Output   : r7 -> errorFlag
                 *
                 * Internal : r0 -> work/scratch/temporary
                 * Internal : r1 -> work/scratch/temporary
                 * Internal : r2 -> work/scratch/temporary
                 * Internal : r3 -> work/scratch/temporary
                 * Internal : r4 -> work/scratch/temporary
                 */
            label("wfe_write_start");

                // Get and store the bytes
            label("wfe_write_loop");
                $ldrh (Reg.R1, Reg.R5, 0                ); // Get word 0
                $ldrh (Reg.R2, Reg.R5, 2                ); // Get word 1
                $inc4s(Reg.R5                           );
            if(writeSize8) {
                $ldrh (Reg.R3, Reg.R5, 0                ); // Get word 2
                $ldrh (Reg.R4, Reg.R5, 2                ); // Get word 3
                $inc4s(Reg.R5                           );
            }
                $ldri (Reg.R0, _mf3_FLASH_FWBL0_z16_o0  );
                $strh (Reg.R1, Reg.R0                   ); // Store word 0
                $ldri (Reg.R0, _mf3_FLASH_FWBH0_z16_o0  );
                $strh (Reg.R2, Reg.R0                   ); // Store word 1
            if(writeSize8) {
                $ldri (Reg.R0, _mf3_FLASH_FWBL1_z16_o0  );
                $strh (Reg.R3, Reg.R0                   ); // Store word 2
                $ldri (Reg.R0, _mf3_FLASH_FWBH1_z16_o0  );
                $strh (Reg.R4, Reg.R0                   ); // Store word 3
            }

                // Write the bytes and break (halt the core) after completion
                _buildLoaderProgram_FlashEEPROM_Write(this);

        }};
    }

    protected static ARMCortexMThumb _buildLoaderProgram_EEPROM() throws JXMAsmError
    {
        return new ARMCortexMThumb(CPU.M0) {{

            SWDAsmARMCommon.putStdPreamble(this, SRAM_START + PROG_AREA_SIZE);

            function_Start();

                /*
                 * The entry point
                 *
                 * Input    : r5 -> begSrcAddr (in SRAM )
                 * Input    : r6 -> endSrcAddr (in SRAM )
                 *
                 * Output   : r7 -> errorFlag
                 *
                 * Internal : r0 -> work/scratch/temporary
                 * Internal : r1 -> work/scratch/temporary
                 * Internal : r2 -> work/scratch/temporary
                 */
            label("wfe_write_start");

                // Get and store the byte
            label("wfe_write_loop");
                $ldrb (Reg.R1, Reg.R5                   ); // Get byte
                $inc1s(Reg.R5                           );
                $ldri (Reg.R0, _mf3_FLASH_FWBL0_z16_o0  );
                $strh (Reg.R1, Reg.R0                   ); // Store byte

                // Write the bytes and break (halt the core) after completion
                _buildLoaderProgram_FlashEEPROM_Write(this);

        }};
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static void _lpLinkAndStore(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final boolean writeSize8, final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        ib.lpLinkAndStore( swdExecInst, _buildLoaderProgram_Flash (writeSize8), SRAM_START, dumpDisassemblyAndArray, _mf3_xvi_LoaderProgramFlash  );
        ib.lpLinkAndStore( swdExecInst, _buildLoaderProgram_EEPROM(          ), SRAM_START, dumpDisassemblyAndArray, _mf3_xvi_LoaderProgramEEPROM );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long[] flProgram_renesas_mf3(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    { return _buildLoaderProgram_Flash (true).link(SRAM_START, dumpDisassemblyAndArray); }

    public static long[] elProgram_renesas_mf3(final boolean dumpDisassemblyAndArray) throws JXMAsmError
    { return _buildLoaderProgram_EEPROM(    ).link(SRAM_START, dumpDisassemblyAndArray); }

} // class SWDAsmARM_RenMF3_I
