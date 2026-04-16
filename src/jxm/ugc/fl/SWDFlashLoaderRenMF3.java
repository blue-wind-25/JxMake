/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.fl;


import jxm.*;
import jxm.ugc.*;
import jxm.xb.*;

import static jxm.ugc.ARMCortexMThumb.CPU;
import static jxm.ugc.ARMCortexMThumb.Reg;
import static jxm.ugc.ProgSWD.CoreReg;
import static jxm.xb.XCom._BV;


/*
 * This class is written based on the algorithms and information found from:
 *
 *     Application Note
 *     Renesas RA Family Flash Memory Programming
 *     https://www.renesas.com/us/en/document/apn/flash-memory-programming
 *
 *     Renesas RA4M1 Group Datasheet
 *     https://www.renesas.com/us/en/document/dst/ra4m1-group-datasheet
 *
 *     Renesas RA4M1 Group User's Manual: Hardware
 *     https://www.renesas.com/us/en/document/mah/renesas-ra4m1-group-users-manual-hardware
 *
 * ~~~ Last accessed & checked on 2024-07-02 ~~~
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * Please refer to the comment block before the 'ProgSWD' class definition in the 'ProgSWD.java' file for more details and information.
 */
public class SWDFlashLoaderRenMF3 extends SWDAsmARM_RenMF3_I {

    protected static final XVI _rafam_xvi_TMP_0              = XVI._0000;
    protected static final XVI _rafam_xvi_TMP_1              = XVI._0001;
    protected static final XVI _rafam_xvi_TMP_2              = XVI._0002;
    protected static final XVI _rafam_xvi_TMP_3              = XVI._0003;
    protected static final XVI _rafam_xvi_TMP_4              = XVI._0004;
    protected static final XVI _rafam_xvi_TMP_5              = XVI._0005;
    protected static final XVI _rafam_xvi_TMP_6              = XVI._0006;

    /*
    protected static final XVI _rafam_xvi_SWDFrequency       = XVI._0100;
    //*/
    protected static final XVI _rafam_xvi_ChipInHSMode       = XVI._0101;
    protected static final XVI _rafam_xvi_FlashFrequency     = XVI._0102;

    protected static final XVI _rafam_xvi_FLB_DoneRead       = XVI._0500;
    protected static final XVI _rafam_xvi_FLB_FDirty         = XVI._0501;
    protected static final XVI _rafam_xvi_FLB_LBDirty        = XVI._0502;

    protected static final long[][] _rafam_instruction_ClearAllFLBFlags = new SWDExecInstBuilder() {{
        // Clear all FLB flags
        mov( 0, _rafam_xvi_FLB_DoneRead );
        mov( 0, _rafam_xvi_FLB_FDirty   );
        mov( 0, _rafam_xvi_FLB_LBDirty  );
    }}.link();

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final long _ra4m1_OFS1_z32_o0            = 0x00000404L;

    protected static final long _ra4m1_SYSTEM_SCKDIVCR_z32_o0 = 0x4001E020L;
    protected static final long _ra4m1_SYSTEM_SCKSCR_z08_o2   = 0x4001E026L;
    protected static final long _ra4m1_SYSTEM_HOCOCR_z08_o2   = 0x4001E036L;
    protected static final long _ra4m1_SYSTEM_MOCOCR_z08_o0   = 0x4001E038L;
    protected static final long _ra4m1_SYSTEM_OSCSF_z08_o0    = 0x4001E03CL;
    protected static final long _ra4m1_SYSTEM_OPCCR_z08_o0    = 0x4001E0A0L;
    protected static final long _ra4m1_SYSTEM_PRCR_z16_o2     = 0x4001E3FEL;

    protected static class RA4M1_OSMem {

        public final long[] SPEC;
        public final long[] SPEC_mask;
        public final long[] SPEC_set;

        protected RA4M1_OSMem(
            final long[] _SPEC     ,
            final long[] _SPEC_mask,
            final long[] _SPEC_set
        )
        {
            if(_SPEC == null || _SPEC_mask == null || _SPEC_set == null) {
                SPEC      = new long[0];
                SPEC_mask = new long[0];
                SPEC_set  = new long[0];
            }
            else {
                SPEC      = XCom.arrayCopy(_SPEC     );
                SPEC_mask = XCom.arrayCopy(_SPEC_mask);
                SPEC_set  = XCom.arrayCopy(_SPEC_set );
            }
        }

    } // class RA4M1_OSMem

    private static void _ra4m1x_putInstXCV(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final XVI xviSrc, final long storeMask, final XVI xviStoreIdx, final XVI xviTmp0)
    {
         ib.bwAND( xviSrc     , storeMask  , xviTmp0 );
         ib.strDB( xviTmp0    , xviStoreIdx          );
         ib.inc1 ( xviStoreIdx                       );
    }

    protected static final long[][] _ra4m1_instruction_InitializeSystem = new SWDExecInstBuilder() {{
            // Clear all flags
            appendPrelinkedInst( _rafam_instruction_ClearAllFLBFlags                                                 );
            // ===== Halt and reset =====
            haltCore           ( true                                                                                );
            /*
            // ===== Save the current SWD frequency =====
            getSWDFrequency    ( _rafam_xvi_SWDFrequency                                                             );
            //*/
            // ===== Set the flash frequency to zero =====
            mov                ( 0                            , _rafam_xvi_FlashFrequency                            );
            // ===== Use the high-speed on-chip oscillator (HOCO) if it is enabled =====
            rdsBits            ( _ra4m1_OFS1_z32_o0           , _rafam_xvi_TMP_0                                     );
            bwAND              ( _rafam_xvi_TMP_0             , 0x00000100L              , _rafam_xvi_TMP_1          );
            jmpIfNotZero       ( _rafam_xvi_TMP_1             , "hoco_disabled"                                      );
            // ----- Get the HOCO frequency
            bwAND              ( _rafam_xvi_TMP_0             , _BV(7         , 12)      , _rafam_xvi_TMP_1          ); // Get   HOCOFRQ1
            jmpIfEQ            ( _rafam_xvi_TMP_1             , _BV(0b000     , 12)      , "hoco_freq_24"            ); // Check HOCOFRQ1
            jmpIfEQ            ( _rafam_xvi_TMP_1             , _BV(0b010     , 12)      , "hoco_freq_32"            ); // ----
            jmpIfEQ            ( _rafam_xvi_TMP_1             , _BV(0b100     , 12)      , "hoco_freq_48"            ); // ----
            jmpIfEQ            ( _rafam_xvi_TMP_1             , _BV(0b101     , 12)      , "hoco_freq_64"            ); // ----
            exitErr            ( _rafam_xvi_TMP_1                                                                    );
        label                  ( "hoco_freq_24"                                                                      );
            mov                ( 24                           , _rafam_xvi_FlashFrequency                            );
            jmp                ( "hoco_freq_done"                                                                    );
        label                  ( "hoco_freq_32"                                                                      );
            mov                ( 32                           , _rafam_xvi_FlashFrequency                            );
            jmp                ( "hoco_freq_done"                                                                    );
        label                  ( "hoco_freq_48"                                                                      );
            mov                ( 48                           , _rafam_xvi_FlashFrequency                            );
            jmp                ( "hoco_freq_done"                                                                    );
        label                  ( "hoco_freq_64"                                                                      );
            mov                ( 64                           , _rafam_xvi_FlashFrequency                            );
        label                  ( "hoco_freq_done"                                                                    );
            // ----- Use HOCO as the flash interface, system, and peripheral module A-B-C-D clocks -----
            rdlBitsUntilSet    ( _ra4m1_SYSTEM_OSCSF_z08_o0   , 0b00000001                                           ); // Wait  HOCOSF   // ##### !!! TODO : Use timeout !!! #####
            modBits            ( _ra4m1_SYSTEM_PRCR_z16_o2    , _BV(0xFFFF    , 16)      , _BV(0xA501    , 16)       ); // Enable writing to registers related to the clock generation circuit
            clrBits            ( _ra4m1_SYSTEM_HOCOCR_z08_o2  , _BV(0b00000001, 16)                                  ); // Clear HCSTP
            clrBits            ( _ra4m1_SYSTEM_SCKSCR_z08_o2  , _BV(0b00000111, 16)                                  ); // Clear CKSEL
            modBits            ( _ra4m1_SYSTEM_SCKDIVCR_z32_o0, 0x77007777L              , 0x11001111L               ); // Set all clock frequencies to (HOCO / 2)
            modBits            ( _ra4m1_SYSTEM_PRCR_z16_o2    , _BV(0xFFFF    , 16)      , _BV(0xA500    , 16)       ); // Disable writing to registers related to the clock generation circuit
            div                ( _rafam_xvi_FlashFrequency    , 2                        , _rafam_xvi_FlashFrequency );
            // ----- Switch to high-speed operating mode -----
            modBits            ( _ra4m1_SYSTEM_PRCR_z16_o2    , _BV(0xFFFF    , 16)      , _BV(0xA502    , 16)       ); // Enable writing to registers related to the low power modes and the battery backup function
            clrBits            ( _ra4m1_SYSTEM_OPCCR_z08_o0   , 0b00000011                                           ); // Clear OPCM
            rdlBitsUntilUnset  ( _ra4m1_SYSTEM_OPCCR_z08_o0   , 0b00010000                                           ); // Wait  !OPCMTSF   // ##### !!! TODO : Use timeout !!! #####
            modBits            ( _ra4m1_SYSTEM_PRCR_z16_o2    , _BV(0xFFFF    , 16)      , _BV(0xA500    , 16)       ); // Disable writing to registers related to the low power modes and the battery backup function
            /*
            // Increase the SWD frequency
            setSWDFrequency    ( 8000000                                                                             );
            // Restore the SWD frequency
            setSWDFrequency    ( _rafam_xvi_SWDFrequency                                                             );
            //*/
            jmp                ( "init_freq_done"                                                                    );
            // ===== Otherwise, use the middle-speed on-chip oscillator (MOCO) =====
        label                  ( "hoco_disabled"                                                                     );
            // ----- Use MOCO as the flash interface, system, and peripheral module A-B-C-D clocks -----
            rdlBitsUntilSet    ( _ra4m1_SYSTEM_OSCSF_z08_o0   , 0b00001000                                           ); // Wait  MOSCSF   // ##### !!! TODO : Use timeout !!! #####
            modBits            ( _ra4m1_SYSTEM_PRCR_z16_o2    , _BV(0xFFFF    , 16)      , _BV(0xA501    , 16)       ); // Enable writing to registers related to the clock generation circuit
            clrBits            ( _ra4m1_SYSTEM_MOCOCR_z08_o0  ,     0b00000001                                       ); // Clear MCSTP
            modBits            ( _ra4m1_SYSTEM_SCKSCR_z08_o2  , _BV(0b00000111, 16)      , _BV(0b00000001, 16)       ); // Set   CKSEL to 1
            clrBits            ( _ra4m1_SYSTEM_SCKDIVCR_z32_o0, 0x77007777L                                          ); // Set all clock frequencies to (MOCO / 1)
            modBits            ( _ra4m1_SYSTEM_PRCR_z16_o2    , _BV(0xFFFF    , 16)      , _BV(0xA500    , 16)       ); // Disable writing to registers related to the clock generation circuit
            mov                ( 8                            , _rafam_xvi_FlashFrequency                            );
        label                  ( "init_freq_done"                                                                    );
            // ===== Check if the chip is in high-speed operating mode =====
            rdsBits            ( _ra4m1_SYSTEM_OPCCR_z08_o0   , _rafam_xvi_TMP_0                                     );
            bwAND              ( _rafam_xvi_TMP_0             , 0x00000003L              , _rafam_xvi_TMP_0          );
            mov                ( 0                            , _rafam_xvi_ChipInHSMode                              );
            jmpIfNotZero       ( _rafam_xvi_TMP_0             , "in_hs_mode"                                         );
            mov                ( 1                            , _rafam_xvi_ChipInHSMode                              );
        label                  ( "in_hs_mode"                                                                        );
    }}.link();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static Specifier _getSpecifier_renesas_mf3(final ProgSWD.Config config, final SWDExecInst swdExecInst, final long[][] instruction_InitializeSystemOnce, final boolean writeSize8, final RA4M1_OSMem osMem) throws JXMAsmError
    {
        // ##### ??? TODO : How if 'Access Window' is enabled ??? #####

        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Link and store the loader program
        _lpLinkAndStore(ib, swdExecInst, writeSize8, false);

        ib.appendPrelinkedInst(instruction_InitializeSystemOnce);

        final long[][] _instruction_InitializeSystemOnce = ib.link();

        // Generate the instructions to set the 'FlashIF' module clock
        {
                // ===== Enable access to the data flash =====
                // ##### !!! TODO : Enable only when accessing EEPROM? !!! #####
                ib.setBits         ( _mf3_FLASH_DFLCTL_z08_o0 , 0b00000001                   ); // Set DFLEN
                ib.delayMS         ( 1                                                       ); // Wait at least tDSTOP
                // ===== Set the frequency of the 'FlashIF' module clock =====
                ib.jmpIfEQ         ( _rafam_xvi_FlashFrequency, 32              , "ff_32"    );
                ib.jmpIfEQ         ( _rafam_xvi_FlashFrequency, 24              , "ff_24"    );
                ib.jmpIfEQ         ( _rafam_xvi_FlashFrequency, 16              , "ff_16"    );
                ib.jmpIfEQ         ( _rafam_xvi_FlashFrequency, 12              , "ff_12"    );
                ib.jmpIfEQ         ( _rafam_xvi_FlashFrequency,  8              , "ff_08"    );
                ib.exitErr         ( _rafam_xvi_FlashFrequency                               );
            ib.label               ( "ff_32"                                                 );
                ib.modBits         ( _mf3_FLASH_FISR_z08_o0   , 0b00011111      , 0b00011111 ); // Set PCKA to 0b11111
                ib.jmp             ( "ff_done"                                               );
            ib.label               ( "ff_24"                                                 );
                ib.modBits         ( _mf3_FLASH_FISR_z08_o0   , 0b00011111      , 0b00010111 ); // Set PCKA to 0b10111
                ib.jmp             ( "ff_done"                                               );
            ib.label               ( "ff_16"                                                 );
                ib.modBits         ( _mf3_FLASH_FISR_z08_o0   , 0b00011111      , 0b00001111 ); // Set PCKA to 0b01111
                ib.jmp             ( "ff_done"                                               );
            ib.label               ( "ff_12"                                                 );
                ib.modBits         ( _mf3_FLASH_FISR_z08_o0   , 0b00011111      , 0b00001011 ); // Set PCKA to 0b01011
                ib.jmp             ( "ff_done"                                               );
            ib.label               ( "ff_08"                                                 );
                ib.modBits         ( _mf3_FLASH_FISR_z08_o0   , 0b00011111      , 0b00000111 ); // Set PCKA to 0b00111
            ib.label               ( "ff_done"                                               );
                // ===== Set the wait cycle of the code flash read to zero
                ib.wrtBits         ( _mf3_FLASH_FLWAITR_z08_o0, 0                            );
        }

        final long[][] _instruction_SetFlashIFModuleClock = ib.link();

        // Generate the instructions to switch to code/data flash read mode
        {
            // ===== Switch to code flash read mode =====
            // ----- Step 1 -----
            ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0      , 0xA5                             ); // Unlock FPMCR protection
                                                                                                     // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b10010010                       ); // 1    0    0    1    0     0    1    0      (0x92)
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b01101101                       ); // ---                                        (0x6D)
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b10010010                       ); // ---                                        (0x92)
            ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0     , 0b00000001     , 0               ); // Check PERR
            ib.delayMS            ( 1                                                             ); // Wait at least tDIS

            // ----- Step 2 -----
            ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0      , 0xA5                             ); // Unlock FPMCR protection
                                                                                                     // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b00010010                       ); // 0    0    0    1    0     0    1    0      (0x12)
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b11101101                       ); // ---                                        (0xED)
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b00010010                       ); // ---                                        (0x12)
            ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0     , 0b00000001     , 0               ); // Check PERR
            // ----- Step 3 -----
            ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0      , 0xA5                             ); // Unlock FPMCR protection
                                                                                                     // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b00001000                       ); // 0    0    0    0    1     0    0    0      (0x08)
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b11110111                       ); // ---                                        (0xF7)
            ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0    , 0b00001000                       ); // ---                                        (0x08)
            ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0     , 0b00000001     , 0               ); // Check PERR
            ib.delayMS            ( 1                                                             ); // Wait at least tMS
            // ------ Step 4 -----
                                                                                                     // FEKEY[7:0] FENTRYD ----[6:1] FENTRY0
            ib.modBits            ( _mf3_FLASH_FENTRYR_z16_o2  , _BV(0xFFFF, 16), _BV(0xAA00, 16) ); // 0xAA       0       0b000000  0
            ib.rdlBitsWhileNotZero( _mf3_FLASH_FENTRYR_z16_o2  , _BV(0xFFFF, 16)                  ); // ##### !!! TODO : Use timeout !!! #####
            // ===== Set the frequency of the 'FlashIF' module clock =====
            ib.appendPrelinkedInst( _instruction_SetFlashIFModuleClock                            );
        }

        final long[][] _instruction_SwitchToCodeDataFlashReadMode = ib.link();

        // Generate the instructions to switch to code flash P/E mode
        {
                // ===== Switch to code flash P/E mode =====
                // ----- Step 1 -----
                                                                                                        // FEKEY[7:0] FENTRYD ----[6:1] FENTRY0
                ib.modBits            ( _mf3_FLASH_FENTRYR_z16_o2, _BV(0xFFFF, 16) , _BV(0xAA01, 16) ); // 0xAA       0       0b000000  1
                // ----- Step 2 -----
                ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0    , 0xA5                              ); // Unlock FPMCR protection
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b00010010                        ); // 0    0    0    1    0     0    1    0      (0x12)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b11101101                        ); // ---                                        (0xED)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b00010010                        ); // ---                                        (0x12)
                ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0   , 0b00000001      , 0               ); // Check PERR
                ib.delayMS            ( 1                                                            ); // Wait at least tDIS
                ib.jmpIfNotZero       ( _rafam_xvi_ChipInHSMode  , "in_hs_mode"                      );
                // ----- Middle-speed mode -----
                // ----- Step 3 -----
                ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0    , 0xA5                              ); // Unlock FPMCR protection
                                                                                                        // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b11010010                        ); // 1    1    0    1    0     0    1    0      (0xD2)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b00101101                        ); // ---                                        (0x2D)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b11010010                        ); // ---                                        (0xD2)
                ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0   , 0b00000001      , 0               ); // Check PERR
                // ----- Step 4 -----
                ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0    , 0xA5                              ); // Unlock FPMCR protection
                                                                                                        // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b11000010                        ); // 1    1    0    0    0     0    1    0      (0xC2)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b00111101                        ); // ---                                        (0x3D)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b11000010                        ); // ---                                        (0xC2)
                ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0   , 0b00000001      , 0               ); // Check PERR
                ib.jmp                ( "wait_tms"                                                   );
            ib.label                  ( "in_hs_mode"                                                 );
                // ----- High-speed mode -----
                // ----- Step 3 -----
                ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0    , 0xA5                              ); // Unlock FPMCR protection
                                                                                                        // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b10010010                        ); // 1    0    0    1    0     0    1    0      (0x92)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b01101101                        ); // ---                                        (0x6D)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b10010010                        ); // ---                                        (0x92)
                ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0   , 0b00000001      , 0               ); // Check PERR
                // ----- Step 4 -----
                ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0    , 0xA5                              ); // Unlock FPMCR protection
                                                                                                        // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b10000010                        ); // 1    0    0    0    0     0    1    0      (0x82)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b01111101                        ); // ---                                        (0x7D)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b10000010                        ); // ---                                        (0x82)
                ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0   , 0b00000001      , 0               ); // Check PERR
            ib.label                  ( "wait_tms"                                                   );
                ib.delayMS            ( 1                                                            ); // Wait at least tMS
                // ===== Set the frequency of the 'FlashIF' module clock =====
                ib.appendPrelinkedInst( _instruction_SetFlashIFModuleClock                           );
        }

        final long[][] _instruction_SwitchToCodeFlashPEMode = ib.link();

        // Generate the instructions to switch to data flash P/E mode
        {
                // ===== Switch to data flash P/E mode =====
                // ----- Step 1 -----
                                                                                                        // FEKEY[7:0] FENTRYD ----[6:1] FENTRY0
                ib.modBits            ( _mf3_FLASH_FENTRYR_z16_o2, _BV(0xFFFF, 16) , _BV(0xAA80, 16) ); // 0xAA       1       0b000000  0
                ib.delayMS            ( 1                                                            ); // Wait at least tDSTOP
                ib.jmpIfNotZero       ( _rafam_xvi_ChipInHSMode  , "in_hs_mode"                      );
                // ----- Middle-speed mode -----
                // ----- Step 2 -----
                ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0    , 0xA5                              ); // Unlock FPMCR protection
                                                                                                        // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b01010000                        ); // 0    1    0    1    0     0    0    0      (0x50)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b10101111                        ); // ---                                        (0xAF)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b01010000                        ); // ---                                        (0x50)
                ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0   , 0b00000001      , 0               ); // Check PERR
                ib.jmp                ( "dfpem_done"                                                 );
            ib.label                  ( "in_hs_mode"                                                 );
                // ----- High-speed mode -----
                // ----- Step 2 -----
                ib.wrtBits            ( _mf3_FLASH_FPR_z08_o0    , 0xA5                              ); // Unlock FPMCR protection
                                                                                                        // FMS2 LVPE ---- FMS1 RPDIS ---- FMS0 ----
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b00010000                        ); // 0    0    0    1    0     0    0    0      (0x10)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b11101111                        ); // ---                                        (0xEF)
                ib.wrtBits            ( _mf3_FLASH_FPMCR_z08_o0  , 0b00010000                        ); // ---                                        (0x10)
                ib.rdBusErrIfCmpNEQ   ( _mf3_FLASH_FPSR_z08_o0   , 0b00000001      , 0               ); // Check PERR
            ib.label                  ( "dfpem_done"                                                 );
                // ===== Set the frequency of the 'FlashIF' module clock =====
                ib.appendPrelinkedInst( _instruction_SetFlashIFModuleClock                           );
        }

        final long[][] _instruction_SwitchToDataFlashPEMode = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */

        // ##### !!! TODO : How about AWSC, AWS, OSISn, and OCDIDn? !!! #####

        final boolean noFLB = (osMem == null || osMem.SPEC == null || osMem.SPEC_mask == null || osMem.SPEC_set == null);

        // Generate the instructions to write the option settings
        if(!noFLB) {
            final long sramDataStartAddress = SRAM_START           + config.memoryFlash.pageSize;
            final long sramDataEndAddress   = sramDataStartAddress + config.memoryFlash.pageSize;
                // For convenience
                final XVI xvi_Tmp0     = _rafam_xvi_TMP_0;
                final XVI xvi_StoreIdx = _rafam_xvi_TMP_1;
                final XVI xvi_OPTn     = _rafam_xvi_TMP_2;
                // Simpy exit if the FLB data was not dirty
                ib.bwOR               ( _rafam_xvi_FLB_FDirty       , _rafam_xvi_FLB_FDirty , xvi_Tmp0         );
                ib.jmpIfZero          ( xvi_Tmp0                    , "flb_not_dirty"                          );
                // ===== Read the 1st page of the flash memory =====
                // ===== NOTE : The option settings are actually located in the flash memory =====
                // Switch to code flash read mode
                ib.appendPrelinkedInst( _instruction_SwitchToCodeDataFlashReadMode                             );
                // Read the the page
            for(int i = 0; i < config.memoryFlash.pageSize / 4; ++i) {
                ib.rdBusSB            ( i * 4                       , 0xFFFFFFFFL           , i                );
            }
                // ===== Modify the flash memory data =====
                // NOTE : The first element always contains the dummy lock bits while the remaining elements always contain the fuses.
            for(int i = 0; i < osMem.SPEC.length; ++i) {
                // Ignore the lock bits
                ib.ldrDB              ( i + 1                       , xvi_OPTn                                 );
                ib.bwAND              ( xvi_OPTn                    , osMem.SPEC_mask[i]    , xvi_OPTn         );
                ib.bwOR               ( xvi_OPTn                    , osMem.SPEC_set [i]    , xvi_OPTn         );
                /*
                ib.debugPrintlnUHex08 ( xvi_OPTn                                                               );
                //*/
                ib.strSB              ( xvi_OPTn                    , osMem.SPEC     [i] / 4                   );
            }
            /*
            for(int i = 0; i < config.memoryFlash.pageSize / 4; ++i) {
                ib.ldrSB              ( i                           , xvi_Tmp0                                 );
                ib.debugPrintlnUHex08 ( xvi_Tmp0                                                               );
            }
            //*/
                // ===== Write the modified flash memory data =====
                // !!! WARNING : Interruption in this process will corrupt the option settings and booloader !!!
                // Load the flash writer program
                ib.lpLoad             ( _mf3_xvi_LoaderProgramFlash , SRAM_START                               );
                // Switch to code flash P/E mode
                ib.appendPrelinkedInst( _instruction_SwitchToCodeFlashPEMode                                   );
                // Erase the 1st page of the flash memory
                //*
                // Set the begin address and end address
                ib.wrtBits            ( _mf3_FLASH_FSARH_z16_o0     , 0                                        );
                ib.wrtBits            ( _mf3_FLASH_FSARL_z16_o0     , 0                                        );
                ib.wrtBits            ( _mf3_FLASH_FEARH_z16_o0     , 0                                        );
                ib.wrtBits            ( _mf3_FLASH_FEARL_z16_o0     , config.memoryFlash.pageSize - 1          );
                // Perform block erase
                ib.wrtBits            ( _mf3_FLASH_FCR_z08_o0       , 0b10000100                               ); // Set    OPST ; Set CMD to 0b0100 (block erase)
                ib.rdlBitsUntilSet    ( _mf3_FLASH_FSTATR1_z08_o0   , 0b01000000                               ); // Wait   FRDY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrtBits            ( _mf3_FLASH_FCR_z08_o0       , 0b00000000                               ); // Clear  OPST CMD
                ib.rdlBitsUntilUnset  ( _mf3_FLASH_FSTATR1_z08_o0   , 0b01000000                               ); // Wait  !FRDY   // ##### !!! TODO : Use timeout !!! #####
                ib.rdsBits            ( _mf3_FLASH_FSTATR2_z16_o0   , _rafam_xvi_TMP_0                         ); // Get and check ILGLERR and ERERR
                ib.bwAND              ( _rafam_xvi_TMP_0            , 0b00010001            , _rafam_xvi_TMP_1 ); // ---
                ib.errIfNotZero       ( _rafam_xvi_TMP_1                                                       ); // ---
                // Set the high-part and low-part of the destination address
                ib.wrtBits            ( _mf3_FLASH_FSARH_z16_o0     , 0                                        );
                ib.wrtBits            ( _mf3_FLASH_FSARL_z16_o0     , 0                                        );
                // Copy the bytes to SRAM
            for(int i = 0; i < config.memoryFlash.pageSize / 4; ++i) {
                ib.ldrSB              ( i                           , xvi_Tmp0                                 );
                ib.wrBus              ( sramDataStartAddress + i * 4, xvi_Tmp0                                 );
            }
                // Execute the program
                ib.wrCReg             ( CoreReg.R5                  , sramDataStartAddress                     );
                ib.wrCReg             ( CoreReg.R6                  , sramDataEndAddress                       );
                ib.lpExecute          (                                                                        );
                // Wait until the program is complete and check for error
                ib.lpWaitBKPT         (                                                                        );
                ib.rdCRegErrIfCmpNEQ  ( CoreReg.R7                  , 0xFFFFFFFFL           , 0                );
                //*/
                // Clear all flags
            ib.label                  ( "flb_not_dirty"                                                        );
                ib.appendPrelinkedInst( _rafam_instruction_ClearAllFLBFlags                                    );
        }

        final long[][] instruction_WriteFLB = ib.link();

        // Generate the instructions to read the option settings
        if(!noFLB) {
                // For convenience
                final XVI xvi_Tmp0     = _rafam_xvi_TMP_0;
                final XVI xvi_StoreIdx = _rafam_xvi_TMP_1;
                final XVI xvi_OPTn     = _rafam_xvi_TMP_2;
                // Simpy exit if the FLB data was already read
                ib.jmpIfNotZero        ( _rafam_xvi_FLB_DoneRead, "flb_done_read"                                      );
                // Switch to code flash read mode
                 ib.appendPrelinkedInst( _instruction_SwitchToCodeDataFlashReadMode                                    );
                // Set the store index to one
                ib.mov                 ( 1                      , xvi_StoreIdx                                         );
                // NOTE : The first element always contains the dummy lock bits while the remaining elements always contain the fuses.
                // Store 0x00 as lock bits
                ib.strDB               ( 0x00                   , 0                                                    );
                // Store the fuses
            for(int i = 0; i < osMem.SPEC.length; ++i) {
                ib.rdsBits             ( osMem.SPEC[i]          , xvi_OPTn                                             );
                /*
                ib.debugPrintlnUHex08  ( xvi_OPTn                                                                      );
                //*/
                // Extract the as fuse
                _ra4m1x_putInstXCV     ( ib, swdExecInst        , xvi_OPTn, osMem.SPEC_mask[i], xvi_StoreIdx, xvi_Tmp0 );
            }
                // Set flag
                ib.mov                 ( 1                      , _rafam_xvi_FLB_DoneRead                              );
            ib.label                   ( "flb_done_read"                                                               );
        }

        final long[][] instruction_ReadFLB = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Generate the instructions to erase only part of flash memory
        if( (config.memoryFlash.partEraseAddressBeg >= 0) && (config.memoryFlash.partEraseSize > 0) )
        {
            // ===== Calculate the begin address and end address =====
            final long    flash512KB   = 512 * 1024;
            final long    flashMemBeg  = config.memoryFlash.partEraseAddressBeg;
            final long    flashMemEnd  = config.memoryFlash.partEraseAddressBeg + config.memoryFlash.partEraseSize - 1;
            final boolean flashTwoMCs  = (flashMemBeg < flash512KB) && (flashMemEnd >= flash512KB);
            final int     flashAPCnt   = flashTwoMCs ? 2 : 1;
            final long[]  flashMemBegH = new long[flashAPCnt];
            final long[]  flashMemBegL = new long[flashAPCnt];
            final long[]  flashMemEndH = new long[flashAPCnt];
            final long[]  flashMemEndL = new long[flashAPCnt];
            if(!flashTwoMCs) {
                flashMemBegH[0] = (  flashMemBeg     & 0xFFFF0000L) >> 16;
                flashMemBegL[0] = (  flashMemBeg     & 0x0000FFFFL) >>  0;
                flashMemEndH[0] = (  flashMemEnd     & 0xFFFF0000L) >> 16;
                flashMemEndL[0] = (  flashMemEnd     & 0x0000FFFFL) >>  0;
            }
            else {
                flashMemBegH[0] = (  flashMemBeg     & 0xFFFF0000L) >> 16;
                flashMemBegL[0] = (  flashMemBeg     & 0x0000FFFFL) >>  0;
                flashMemEndH[0] = ( (flash512KB - 1) & 0xFFFF0000L) >> 16;
                flashMemEndL[0] = ( (flash512KB - 1) & 0x0000FFFFL) >>  0;
                flashMemBegH[1] = (  flash512KB      & 0xFFFF0000L) >> 16;
                flashMemBegL[1] = (  flash512KB      & 0x0000FFFFL) >>  0;
                flashMemEndH[1] = (  flashMemEnd     & 0xFFFF0000L) >> 16;
                flashMemEndL[1] = (  flashMemEnd     & 0x0000FFFFL) >>  0;
            }
                // ===== Switch to code flash P/E mode =====
                ib.appendPrelinkedInst( _instruction_SwitchToCodeFlashPEMode                          );
                // ===== Execute the operation for each macrocell =====
            for(int i = 0; i < flashAPCnt; ++i) {
                // Set the begin address and end address
                ib.wrtBits            ( _mf3_FLASH_FSARH_z16_o0  , flashMemBegH[i]                    );
                ib.wrtBits            ( _mf3_FLASH_FSARL_z16_o0  , flashMemBegL[i]                    );
                ib.wrtBits            ( _mf3_FLASH_FEARH_z16_o0  , flashMemEndH[i]                    );
                ib.wrtBits            ( _mf3_FLASH_FEARL_z16_o0  , flashMemEndL[i]                    );
                /*
                // Perform blank check
                ib.wrtBits            ( _mf3_FLASH_FCR_z08_o0    , 0b10000011                         ); // Set    OPST ; Set CMD to 0b0011 (blank check)
                ib.rdlBitsUntilSet    ( _mf3_FLASH_FSTATR1_z08_o0, 0b01000000                         ); // Wait   FRDY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrtBits            ( _mf3_FLASH_FCR_z08_o0    , 0b00000000                         ); // Clear  OPST CMD
                ib.rdlBitsUntilUnset  ( _mf3_FLASH_FSTATR1_z08_o0, 0b01000000                         ); // Wait  !FRDY   // ##### !!! TODO : Use timeout !!! #####
                ib.rdsBits            ( _mf3_FLASH_FSTATR2_z16_o0, _rafam_xvi_TMP_0                   ); // Get and check ILGLERR and BCERR
                ib.bwAND              ( _rafam_xvi_TMP_0         , 0b00011000      , _rafam_xvi_TMP_1 ); // ---
                ib.errIfNotZero       ( _rafam_xvi_TMP_1                                              ); // ---
                //*/
                //*
                // Perform block erase
                ib.wrtBits            ( _mf3_FLASH_FCR_z08_o0    , 0b10000100                         ); // Set    OPST ; Set CMD to 0b0100 (block erase)
                ib.rdlBitsUntilSet    ( _mf3_FLASH_FSTATR1_z08_o0, 0b01000000                         ); // Wait   FRDY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrtBits            ( _mf3_FLASH_FCR_z08_o0    , 0b00000000                         ); // Clear  OPST CMD
                ib.rdlBitsUntilUnset  ( _mf3_FLASH_FSTATR1_z08_o0, 0b01000000                         ); // Wait  !FRDY   // ##### !!! TODO : Use timeout !!! #####
                ib.rdsBits            ( _mf3_FLASH_FSTATR2_z16_o0, _rafam_xvi_TMP_0                   ); // Get and check ILGLERR and ERERR
                ib.bwAND              ( _rafam_xvi_TMP_0         , 0b00010001      , _rafam_xvi_TMP_1 ); // ---
                ib.errIfNotZero       ( _rafam_xvi_TMP_1                                              ); // ---
                //*/
            }
        }

        final long[][] instruction_EraseFlashPages = ib.link();

        // Generate the instructions to write the flash memory
        {
            final long sramDataStartAddress = SRAM_START + config.memoryFlash.pageSize;
                // Load the flash writer program
                ib.lpLoad                  ( _mf3_xvi_LoaderProgramFlash, SRAM_START                                            );
                ib.mov                     ( 1                          , _mf3_xvi_LoaderProgramFRun                            );
                // Switch to code flash P/E mode
                ib.appendPrelinkedInst     ( _instruction_SwitchToCodeFlashPEMode                                               );
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( -1, _mf3_xvi_FLASH_DST_ADDR, _mf3_xvi_SignalWorkerCommand, _mf3_xvi_SignalJobState );
                // Calculate the high-part and low-part of the destination address
                ib.bwAND                   ( _mf3_xvi_FLASH_DST_ADDR    , 0xFFFF0000L                 , _rafam_xvi_TMP_1        ); // Address H
                ib.bwRSH                   ( _rafam_xvi_TMP_1           , 16                          , _rafam_xvi_TMP_1        ); // ---
                ib.bwAND                   ( _mf3_xvi_FLASH_DST_ADDR    , 0x0000FFFFL                 , _rafam_xvi_TMP_0        ); // Address L
                // Set the high-part and low-part of the destination address
                ib.wrtBits                 ( _mf3_FLASH_FSARH_z16_o0    , _rafam_xvi_TMP_1                                      );
                ib.wrtBits                 ( _mf3_FLASH_FSARL_z16_o0    , _rafam_xvi_TMP_0                                      );
                // Copy the bytes to SRAM
                ib.wrCMem                  ( sramDataStartAddress       , config.memoryFlash.pageSize/*, 0 */                   );
                // Execute the program
                ib.wrCReg                  ( CoreReg.R5                 , sramDataStartAddress                                  );
                ib.wrCReg                  ( CoreReg.R6                 , sramDataStartAddress + config.memoryFlash.pageSize    );
                ib.jmpIfZero               ( _mf3_xvi_LoaderProgramFRun , "wf_lp_continue"                                      );
                ib.mov                     ( 0                          , _mf3_xvi_LoaderProgramFRun                            );
                ib.lpExecute               (                                                                                    );
                ib.jmp                     ( "wf_lp_wait"                                                                       );
            ib.label                       ( "wf_lp_continue"                                                                   );
                ib.lpContinue              (                                                                                    );
            ib.label                       ( "wf_lp_wait"                                                                       );
                // Wait until the program is complete and check for error
                ib.lpWaitBKPT              (                                                                                    );
                ib.rdCRegErrIfCmpNEQ       ( CoreReg.R7                 , 0xFFFFFFFFL                 , 0                       );
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( -1, _mf3_xvi_FLASH_DST_ADDR, _mf3_xvi_SignalWorkerCommand, _mf3_xvi_SignalJobState );
        }

        final long[][] instruction_WriteFlash = ib.link();

        // Generate the instructions to read the flash memory
        {
            // Switch to code flash read mode
            ib.appendPrelinkedInst     ( _instruction_SwitchToCodeDataFlashReadMode                                         );
            // Put the multi-threading prefix code
            ib.macro_mtRdWrFlsEprPrefix( -1, _mf3_xvi_FLASH_DST_ADDR, _mf3_xvi_SignalWorkerCommand, _mf3_xvi_SignalJobState );
            // Read the flash memory
            ib.rdCMem                  (     _mf3_xvi_FLASH_DST_ADDR, _mf3_xvi_FLASH_READ_SIZE  /*, 0 */                    );
            // Put the multi-threading suffix code
            ib.macro_mtRdWrFlsEprSuffix( -1, _mf3_xvi_FLASH_DST_ADDR, _mf3_xvi_SignalWorkerCommand, _mf3_xvi_SignalJobState );
        }

        final long[][] instruction_ReadFlash = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Generate the instructions to write the EEPROM memory
        {
            final long sramDataStartAddress = SRAM_START + config.memoryEEPROM.pageSize;
                // Load the EEPROM writer program
                ib.lpLoad                  ( _mf3_xvi_LoaderProgramEEPROM, SRAM_START                                            );
                ib.mov                     ( 1                           , _mf3_xvi_LoaderProgramFRun                            );
                // Switch to data flash P/E mode
                ib.appendPrelinkedInst     ( _instruction_SwitchToDataFlashPEMode                                               );
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( -1, _mf3_xvi_FLASH_DST_ADDR , _mf3_xvi_SignalWorkerCommand, _mf3_xvi_SignalJobState );
                // Calculate the high-part and low-part of the destination begin address ; transform 0x40100000... -> 0xFE000000... (+0xBDF00000)
                final long eepromWrAddrOfs = 0xFE000000L - 0x40100000L;
                ib.add                     ( _mf3_xvi_FLASH_DST_ADDR     , eepromWrAddrOfs             , _rafam_xvi_TMP_6          );
                ib.bwAND                   ( _rafam_xvi_TMP_6            , 0xFFFF0000L                 , _rafam_xvi_TMP_1          ); // Begin address H
                ib.bwRSH                   ( _rafam_xvi_TMP_1            , 16                          , _rafam_xvi_TMP_1          ); // ---
                ib.bwAND                   ( _rafam_xvi_TMP_6            , 0x0000FFFFL                 , _rafam_xvi_TMP_0          ); // Begin address L
                // Calculate the high-part and low-part of the destination end address
                ib.add                     ( _rafam_xvi_TMP_6            , config.memoryEEPROM.pageSize, _rafam_xvi_TMP_6          );
                ib.dec1                    ( _rafam_xvi_TMP_6                                                                      );
                ib.bwAND                   ( _rafam_xvi_TMP_6            , 0xFFFF0000L                 , _rafam_xvi_TMP_3          ); // End   address H
                ib.bwRSH                   ( _rafam_xvi_TMP_3            , 16                          , _rafam_xvi_TMP_3          ); // ---
                ib.bwAND                   ( _rafam_xvi_TMP_6            , 0x0000FFFFL                 , _rafam_xvi_TMP_2          ); // End   address L
                // Set the high-part and low-part of the destination begin address
                ib.wrtBits                 ( _mf3_FLASH_FSARH_z16_o0     , _rafam_xvi_TMP_1                                        );
                ib.wrtBits                 ( _mf3_FLASH_FSARL_z16_o0     , _rafam_xvi_TMP_0                                        );
                // Set the high-part and low-part of the destination end address
                ib.wrtBits                 ( _mf3_FLASH_FEARH_z16_o0     , _rafam_xvi_TMP_3                                        );
                ib.wrtBits                 ( _mf3_FLASH_FEARL_z16_o0     , _rafam_xvi_TMP_2                                        );
                //*
                // Perform block erase
                /*
                 * ##### ??? TODO ??? #####
                 *     # Do not erase if this block has been erased by the previous loop!
                 *     # It will only happen if 'wrMaxSWDTransferSize' < 'config.memoryEEPROM.pageSize'.
                 */
                //
                // #####
                ib.wrtBits                 ( _mf3_FLASH_FCR_z08_o0       , 0b10000100                                              ); // Set    OPST ; Set CMD to 0b0100 (block erase)
                ib.rdlBitsUntilSet         ( _mf3_FLASH_FSTATR1_z08_o0   , 0b01000000                                              ); // Wait   FRDY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrtBits                 ( _mf3_FLASH_FCR_z08_o0       , 0b00000000                                              ); // Clear  OPST CMD
                ib.rdlBitsUntilUnset       ( _mf3_FLASH_FSTATR1_z08_o0   , 0b01000000                                              ); // Wait  !FRDY   // ##### !!! TODO : Use timeout !!! #####
                ib.rdsBits                 ( _mf3_FLASH_FSTATR2_z16_o0   , _rafam_xvi_TMP_4                                        ); // Get and check ILGLERR and ERERR
                ib.bwAND                   ( _rafam_xvi_TMP_4            , 0b00010001                  , _rafam_xvi_TMP_5          ); // ---
                ib.errIfNotZero            ( _rafam_xvi_TMP_5                                                                      ); // ---
                //*/
                // Set the high-part and low-part of the destination begin address
                ib.wrtBits                 ( _mf3_FLASH_FSARH_z16_o0     , _rafam_xvi_TMP_1                                        );
                ib.wrtBits                 ( _mf3_FLASH_FSARL_z16_o0     , _rafam_xvi_TMP_0                                        );
                // Copy the bytes to SRAM
                ib.wrCMem                  ( sramDataStartAddress        , config.memoryEEPROM.pageSize/*, 0 */                    );
                // Execute the program
                ib.wrCReg                  ( CoreReg.R5                  , sramDataStartAddress                                    );
                ib.wrCReg                  ( CoreReg.R6                  , sramDataStartAddress + config.memoryEEPROM.pageSize     );
                ib.jmpIfZero               ( _mf3_xvi_LoaderProgramFRun  , "we_lp_continue"                                        );
                ib.mov                     ( 0                           , _mf3_xvi_LoaderProgramFRun                              );
                ib.lpExecute               (                                                                                       );
                ib.jmp                     ( "we_lp_wait"                                                                          );
            ib.label                       ( "we_lp_continue"                                                                      );
                ib.lpContinue              (                                                                                       );
            ib.label                       ( "we_lp_wait"                                                                          );
                // Wait until the program is complete and check for error
                ib.lpWaitBKPT              (                                                                                       );
                ib.rdCRegErrIfCmpNEQ       ( CoreReg.R7                  , 0xFFFFFFFFL                 , 0                         );
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( -1, _mf3_xvi_FLASH_DST_ADDR , _mf3_xvi_SignalWorkerCommand, _mf3_xvi_SignalJobState   );
        }

        final long[][] instruction_WriteEEPROM = ib.link();

        // Generate the instructions to read the EEPROM memory
        {
            // Switch to data flash read mode
            ib.appendPrelinkedInst     ( _instruction_SwitchToCodeDataFlashReadMode                                         );
            // Put the multi-threading prefix code
            ib.macro_mtRdWrFlsEprPrefix( -1, _mf3_xvi_FLASH_DST_ADDR, _mf3_xvi_SignalWorkerCommand, _mf3_xvi_SignalJobState );
            // Read the EEPROM memory
            ib.rdCMem                  (     _mf3_xvi_FLASH_DST_ADDR, _mf3_xvi_FLASH_READ_SIZE  /*, 0 */                    );
            // Put the multi-threading suffix code
            ib.macro_mtRdWrFlsEprSuffix( -1, _mf3_xvi_FLASH_DST_ADDR, _mf3_xvi_SignalWorkerCommand, _mf3_xvi_SignalJobState );
        }

        final long[][] instruction_ReadEEPROM = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Instantiate and return the specifier
        return new Specifier(

            0xFF                                             , // flashMemoryEmptyValue
            0                                                , // wrMaxSWDTransferSize
            0                                                , // rdMaxSWDTransferSize
            false                                            , // supportDirectFlashRead
            false                                            , // supportDirectEEPROMRead

            _instruction_InitializeSystemOnce                , // instruction_InitializeSystemOnce
            null                                             , // instruction_UninitializeSystemExit
            null                                             , // instruction_IsFlashLocked
            null                                             , // instruction_UnlockFlash
            null                                             , // instruction_EraseFlash
            instruction_EraseFlashPages                      , // instruction_EraseFlashPages
            instruction_WriteFlash                           , // instruction_WriteFlash
            instruction_ReadFlash                            , // instruction_ReadFlash
            null                                             , // instruction_IsEEPROMLocked
            null                                             , // instruction_UnlockEEPROM
            instruction_WriteEEPROM                          , // instruction_WriteEEPROM
            instruction_ReadEEPROM                           , // instruction_ReadEEPROM
            _mf3_xvi_FLASH_DST_ADDR                          , // instruction_xviFlashEEPROMAddress
            _mf3_xvi_FLASH_READ_SIZE                         , // instruction_xviFlashEEPROMReadSize
            _mf3_xvi_SignalWorkerCommand                     , // instruction_xviSignalWorkerCommand
            _mf3_xvi_SignalJobState                          , // instruction_xviSignalJobState
            new int[config.memoryFlash.pageSize ]            , // instruction_dataBuffFlash
            new int[config.memoryEEPROM.pageSize]            , // instruction_dataBuffEEPROM

            null                                             , // flProgram
            null                                             , // elProgram
            -1                                               , // addrProgStart
            -1                                               , // addrProgBuffer
            -1                                               , // addrProgSignal
            null                                             , // wrProgExtraParams
            null                                             , // rdProgExtraParams

            noFLB ? null     : instruction_WriteFLB          , // instruction_WriteFLB
            noFLB ? null     : instruction_ReadFLB           , // instruction_ReadFLB
            noFLB ? XVI._NA_ : _rafam_xvi_FLB_DoneRead       , // instruction_xviFLB_DoneRead
            noFLB ? XVI._NA_ : _rafam_xvi_FLB_FDirty         , // instruction_xviFLB_FDirty
            noFLB ? XVI._NA_ : _rafam_xvi_FLB_LBDirty        , // instruction_xviFLB_LBDirty
            noFLB ? null     : new int[1 + osMem.SPEC.length]  // instruction_dataBuffFLB

        );
    }

    public static Specifier _getSpecifier_renesas_ra4m1(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMAsmError
    {
        return _getSpecifier_renesas_mf3(config, swdExecInst, _ra4m1_instruction_InitializeSystem, true, new RA4M1_OSMem(
            //           OFS0         OFS1         SECMPUPCS0   SECMPUPCE0   SECMPUPCS1   SECMPUPCE1   SECMPUS0     SECMPUE0     SECMPUS1     SECMPUE1     SECMPUS2     SECMPUE2     SECMPUS3     SECMPUE3     SECMPUAC
            new long[] { 0x00000400L, 0x00000404L, 0x00000408L, 0x0000040CL, 0x00000410L, 0x00000414L, 0x00000418L, 0x0000041CL, 0x00000420L, 0x00000424L, 0x00000428L, 0x0000042CL, 0x00000430L, 0x00000434L, 0x00000438L },
            new long[] { 0x5FFE5FFEL, 0x0000713CL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0x00FFFFFFL, 0x00FFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0x007FFFFFL, 0x007FFFFFL, 0x007FFFFFL, 0x007FFFFFL, 0x0000030FL },
            new long[] { 0xA001A001L, 0xFFFF8EC3L, 0x00000000L, 0x00000000L, 0x00000000L, 0x00000000L, 0x00000000L, 0x00000000L, 0x00000000L, 0x00000000L, 0x40000000L, 0x40000000L, 0x40000000L, 0x40000000L, 0xFFFFFCF0L }
        ) );
    }

} // class SWDFlashLoaderRenMF3
