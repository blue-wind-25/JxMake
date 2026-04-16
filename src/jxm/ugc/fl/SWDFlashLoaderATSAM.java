/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.fl;


import jxm.*;
import jxm.ugc.*;
import jxm.xb.*;


/*
 * This class is written based on the algorithms and information found from:
 *
 *     SAM D21/DA1 Family Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU32/ProductDocuments/DataSheets/SAM-D21-DA1-Family-Data-Sheet-DS40001882H.pdf
 *
 *     SAM3X/SAM3A Series Datasheet
 *     https://ww1.microchip.com/downloads/en/devicedoc/atmel-11057-32-bit-cortex-m3-microcontroller-sam3x-sam3a_datasheet.pdf
 *
 *     AT02333
 *     Safe and Secure Bootloader Implementation for SAM3/4 Atmel 32-bit Microcontroller
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ApplicationNotes/ApplicationNotes/Atmel-42141-SAM-AT02333-Safe-and-Secure-Bootloader-Implementation-for-SAM3-4_Application-Note.pdf
 *
 *     AT03974
 *     SMART ARM-based Microcontrollers - Read While Write EEPROM
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ApplicationNotes/ApplicationNotes/Atmel-42413-SMART-ARM-Read-While-Write-EEPROM-AT03974_Application-Note.pdf
 *
 * ~~~ Last accessed & checked on 2024-07-31 ~~~
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * Please refer to the comment block before the 'ProgSWD' class definition in the 'ProgSWD.java' file for more details and information.
 */
public class SWDFlashLoaderATSAM extends SWDFlashLoader {

    protected static final XVI  _atsamdx_xvi_TMP_0               = XVI._0000;
    protected static final XVI  _atsamdx_xvi_TMP_1               = XVI._0001;
    protected static final XVI  _atsamdx_xvi_TMP_2               = XVI._0002;
    protected static final XVI  _atsamdx_xvi_TMP_3               = XVI._0003;
    protected static final XVI  _atsamdx_xvi_TMP_4               = XVI._0004;
    protected static final XVI  _atsamdx_xvi_TMP_5               = XVI._0005;
    protected static final XVI  _atsamdx_xvi_TMP_6               = XVI._0006;
    protected static final XVI  _atsamdx_xvi_TMP_7               = XVI._0007;
    protected static final XVI  _atsamdx_xvi_TMP_8               = XVI._0008;

    protected static final XVI  _atsamdx_xvi_NVM_CMD_status      = XVI._0100;

    protected static final XVI  _atsamdx_xvi_WR_FLASH_commitPage = XVI._0101;
    protected static final XVI  _atsamdx_xvi_WR_FLASH_pgAddrMask = XVI._0102;

    protected static final XVI  _atsamdx_xvi_FLASH_DST_ADDR      = XVI._0200;
    protected static final XVI  _atsamdx_xvi_SignalWorkerCommand = XVI._0201;
    protected static final XVI  _atsamdx_xvi_SignalJobState      = XVI._0202;

    protected static final XVI  _atsamdx_xvi_FLB_DoneRead        = XVI._0500;
    protected static final XVI  _atsamdx_xvi_FLB_FDirty          = XVI._0501;
    protected static final XVI  _atsamdx_xvi_FLB_LBDirty         = XVI._0502;

    protected static final long _atsamdx_PAC1                    = 0x41000000L;
    protected static final long _atsamdx_DSU_STATUSB_STATUSA     = 0x41002000L; // <8_bit_reserved> <8_bit_STATUSB> <8_bit_STATUSA> <8_bit_CTRL>
    protected static final long _atsamdx_DSU_CTRL_EXT            = 0x41002100L;

    protected static final long _atsamdx_NVMCTRL_CTRLA           = 0x41004000L;
    protected static final long _atsamdx_NVMCTRL_CTRLB           = 0x41004004L;
    protected static final long _atsamdx_NVMCTRL_PARAM           = 0x41004008L;
    protected static final long _atsamdx_NVMCTRL_INTFLAG         = 0x41004014L;
    protected static final long _atsamdx_NVMCTRL_STATUS          = 0x41004018L;
    protected static final long _atsamdx_NVMCTRL_ADDR            = 0x4100401CL;
    protected static final long _atsamdx_NVMCTRL_LOCK            = 0x41004020L;

    protected static final long _atsamdx_NVMCTRL_CTRLA_CMDEX     = 0x0000A500L;
    protected static final long _atsamdx_NVMCTRL_CTRLA_ER        = 0x02;
    protected static final long _atsamdx_NVMCTRL_CTRLA_WP        = 0x04;
    protected static final long _atsamdx_NVMCTRL_CTRLA_EAR       = 0x05;
    protected static final long _atsamdx_NVMCTRL_CTRLA_WAP       = 0x06;
    protected static final long _atsamdx_NVMCTRL_CTRLA_LR        = 0x40;
    protected static final long _atsamdx_NVMCTRL_CTRLA_UR        = 0x41;
    protected static final long _atsamdx_NVMCTRL_CTRLA_PBC       = 0x44;
    protected static final long _atsamdx_NVMCTRL_CTRLA_SSB       = 0x45;

    protected static final long _atsamdx_NVMCTRL_CTRLB_MANW      = 0x00000080L;

    protected static long _atsamdx_nvmCmd(final long cmd)
    { return _atsamdx_NVMCTRL_CTRLA_CMDEX | (cmd & 0x0000007FL); }

    protected static final long[][] _atsamdx_instruction_CheckNVMCmd = new SWDExecInstBuilder() {{
            rdBusLoopWhileCmpNEQ( _atsamdx_NVMCTRL_INTFLAG   , 0x00000001L                 , 0x00000001L                 ); // ##### !!! TODO : Use timeout !!! #####
            rdBusStr            ( _atsamdx_NVMCTRL_STATUS    , 0x0000001CL                 , _atsamdx_xvi_NVM_CMD_status );
            jmpIfEQ             ( _atsamdx_xvi_NVM_CMD_status, 0x00000000L                 , "check_nvm_cmd_ok"          );
            wrBus               ( _atsamdx_NVMCTRL_STATUS    , _atsamdx_xvi_NVM_CMD_status                               );
            exitErr             ( _atsamdx_xvi_NVM_CMD_status                                                            );
        label                   ( "check_nvm_cmd_ok"                                                                     );
    }}.link();

    protected static final long[][] _fl_atsamdx_instruction_ClearAllFLBFlags = new SWDExecInstBuilder() {{
        // Clear all FLB flags
        mov( 0, _atsamdx_xvi_FLB_DoneRead );
        mov( 0, _atsamdx_xvi_FLB_FDirty   );
        mov( 0, _atsamdx_xvi_FLB_LBDirty  );
    }}.link();

    private static void _atsamdx_putInstChkNCommit(final SWDExecInstBuilder ib)
    {
            // Check if the MCU requires manual page-commit
            ib.rdBusStr           ( _atsamdx_NVMCTRL_CTRLB          , 0xFFFFFFFFL                     , _atsamdx_xvi_WR_FLASH_commitPage );
            ib.bwAND              ( _atsamdx_xvi_WR_FLASH_commitPage, _atsamdx_NVMCTRL_CTRLB_MANW     , _atsamdx_xvi_WR_FLASH_commitPage );
    }

    private static void _atsamdx_putInstGenPAdrMsk(final SWDExecInstBuilder ib, final int pageSize)
    {
            // Prepare the page address mask
            ib.mov                ( pageSize                        , _atsamdx_xvi_WR_FLASH_pgAddrMask                                   );
            ib.sub                ( _atsamdx_xvi_WR_FLASH_pgAddrMask, 1                               , _atsamdx_xvi_WR_FLASH_pgAddrMask );
            ib.bwNOT              ( _atsamdx_xvi_WR_FLASH_pgAddrMask, _atsamdx_xvi_WR_FLASH_pgAddrMask                                   );
    }

    private static void _atsamdx_putInstCommitPage(final SWDExecInstBuilder ib, final int pageSize, final long cmdCommit)
    {
            // Manually commit the page as needed
            ib.jmpIfEQ            ( _atsamdx_xvi_WR_FLASH_commitPage, 0x00000000L                     , "wf_no_commit"                   );
            ib.bwAND              ( _atsamdx_xvi_FLASH_DST_ADDR     , _atsamdx_xvi_WR_FLASH_pgAddrMask, _atsamdx_xvi_FLASH_DST_ADDR      );
            ib.bwRSH              ( _atsamdx_xvi_FLASH_DST_ADDR     , 1                               , _atsamdx_xvi_FLASH_DST_ADDR      );
            ib.add                ( _atsamdx_xvi_FLASH_DST_ADDR     , pageSize / 2                    , _atsamdx_xvi_FLASH_DST_ADDR      );
            ib.sub                ( _atsamdx_xvi_FLASH_DST_ADDR     , 1                               , _atsamdx_xvi_FLASH_DST_ADDR      );
            ib.wrBus              ( _atsamdx_NVMCTRL_ADDR           , _atsamdx_xvi_FLASH_DST_ADDR                                        );
            ib.wrBus              ( _atsamdx_NVMCTRL_CTRLA          , _atsamdx_nvmCmd(cmdCommit)                                         );
            ib.jmp                ( "wf_check"                                                                                           );
        ib.label                  ( "wf_no_commit"                                                                                       );
            ib.delayUS            ( 200                                                                                                  );
        ib.label                  ( "wf_check"                                                                                           );
            ib.appendPrelinkedInst( _atsamdx_instruction_CheckNVMCmd                                                                     );
    }

    public static Specifier _getSpecifier_atsamdx(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Generate the instructions to erase the entire flash memory
        {
            //*
            // Enable access to the DSU
            ib.wrBus               ( _atsamdx_PAC1               , 0x00000001L              );
            // Perform chip erase
            ib.wrBus               ( _atsamdx_DSU_CTRL_EXT       , 0x00000010L              );
            ib.rdBusLoopWhileCmpNEQ( _atsamdx_DSU_STATUSB_STATUSA, 0x00000100L, 0x00000100L ); // ##### !!! TODO : Use timeout !!! #####
            //*/
            /*
            ib.rdBusStr            ( _atsamdx_NVMCTRL_PARAM, 0xFFFFFFFFL       , _atsamdx_xvi_TMP_0 );
            ib.bwAND               ( _atsamdx_xvi_TMP_0    , 0x00030000L       , _atsamdx_xvi_TMP_1 );
            ib.bwRSH               ( _atsamdx_xvi_TMP_1    , 16                , _atsamdx_xvi_TMP_1 );
            ib.mov                 ( 8                     , _atsamdx_xvi_TMP_2                     );
            ib.bwLSH               ( _atsamdx_xvi_TMP_2    , _atsamdx_xvi_TMP_1, _atsamdx_xvi_TMP_2 );
            ib.debugPrintlnUDecNN  ( _atsamdx_xvi_TMP_2                                             ); // Page size
            ib.bwAND               ( _atsamdx_xvi_TMP_0    , 0x0000FFFFL       , _atsamdx_xvi_TMP_2 );
            ib.debugPrintlnUDecNN  ( _atsamdx_xvi_TMP_2                                             ); // Number of pages
            //*/
        }

        final long[][] instruction_EraseFlash = ib.link();

        // Generate the instructions to erase only part of flash memory
        if( (config.memoryFlash.partEraseAddressBeg >= 0) && (config.memoryFlash.partEraseSize > 0) )
        {
            // For convenience
            final XVI xviBegAddress = _atsamdx_xvi_TMP_0;
            final XVI xviEndAddress = _atsamdx_xvi_TMP_1;
            // Erase the pages
            final long flashMemBeg = (config.memoryFlash.partEraseAddressBeg                                   ) / 2;
            final long flashMemEnd = (config.memoryFlash.partEraseAddressBeg + config.memoryFlash.partEraseSize) / 2;
            final long flashPgSize = (config.memoryFlash.pageSize                                              ) / 2;
            ib.mov                    ( flashMemBeg                     , xviBegAddress                                             );
            ib.mov                    ( flashMemEnd                     , xviEndAddress                                             );
            ib.label                  ( "efp_loop"                                                                                  );
                ib.wrBus              ( _atsamdx_NVMCTRL_ADDR           , xviBegAddress                                             );
                ib.wrBus              ( _atsamdx_NVMCTRL_CTRLA          , _atsamdx_nvmCmd(_atsamdx_NVMCTRL_CTRLA_ER)                );
                ib.appendPrelinkedInst( _atsamdx_instruction_CheckNVMCmd                                                            );
                ib.add                ( xviBegAddress                   , config.memoryFlash.pageSize               , xviBegAddress );
            ib.jmpIfLT                ( xviBegAddress                   , xviEndAddress                             , "efp_loop"    );
        }

        final long[][] instruction_EraseFlashPages = ib.link();

        // Generate the instructions to write the flash memory
        final int writeCount = config.memoryFlash.pageSize;
        {
                // For convenience
                final XVI xviDataCount = _atsamdx_xvi_TMP_0;
                // Check if the MCU requires manual page-commit
                _atsamdx_putInstChkNCommit ( ib                                                                                                                                           );
                // Clear the data counter and prepare the page address mask
                ib.mov                     ( 0x00000000L                     , xviDataCount                                                                                               );
                _atsamdx_putInstGenPAdrMsk ( ib                              , config.memoryFlash.pageSize                                                                                );
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( -1                              , _atsamdx_xvi_FLASH_DST_ADDR                , _atsamdx_xvi_SignalWorkerCommand, _atsamdx_xvi_SignalJobState );
                // Clears the page buffer as needed
                ib.jmpIfNEQ                ( xviDataCount                    , 0x00000000L                                , "wf_no_clear"                                                 );
                ib.wrBus                   ( _atsamdx_NVMCTRL_CTRLA          , _atsamdx_nvmCmd(_atsamdx_NVMCTRL_CTRLA_PBC)                                                                );
                ib.appendPrelinkedInst     ( _atsamdx_instruction_CheckNVMCmd                                                                                                             );
            ib.label                       ( "wf_no_clear"                                                                                                                                );
                // Write the bytes
                ib.wrCMem                  ( _atsamdx_xvi_FLASH_DST_ADDR     , writeCount                               /*, 0 */                                                          );
                ib.add                     ( xviDataCount                    , writeCount                                 , xviDataCount                                                  );
                // Check if not all bytes for the current page have been written
                ib.jmpIfNEQ                ( xviDataCount                    , config.memoryFlash.pageSize                , "wf_page_not_done"                                            );
                // Manually commit the page as needed
                _atsamdx_putInstCommitPage ( ib                              , config.memoryFlash.pageSize                , _atsamdx_NVMCTRL_CTRLA_WP                                     );
                // Clear the data counter
                ib.mov                     ( 0x00000000L                     , xviDataCount                                                                                               );
            ib.label                       ( "wf_page_not_done"                                                                                                                           );
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( -1                              , _atsamdx_xvi_FLASH_DST_ADDR                , _atsamdx_xvi_SignalWorkerCommand, _atsamdx_xvi_SignalJobState );
        }

        final long[][] instruction_WriteFlash = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */
        final long adr_USER_ROW_L = 0x00804000L;
        final long adr_USER_ROW_H = 0x00804004L;

        final long msk_USER_ROW_L = 0b11111110000000011111111101110111L;
        final long msk_USER_ROW_H = 0b11111111111111110000000111111111L;

        // Generate the instructions to write the user row
        {
                // For convenience
                final XVI xvi_Tmp0     = _atsamdx_xvi_TMP_0;
                final XVI xvi_URL      = _atsamdx_xvi_TMP_1;
                final XVI xvi_URH      = _atsamdx_xvi_TMP_2;
                final XVI xvi_LOCK     = _atsamdx_xvi_TMP_3;
                final XVI xvi_WDT      = _atsamdx_xvi_TMP_4;
                final XVI xvi_BOD33    = _atsamdx_xvi_TMP_5;
                final XVI xvi_EEPROM   = _atsamdx_xvi_TMP_6;
                final XVI xvi_BOOTPROT = _atsamdx_xvi_TMP_7;
                final XVI xvi_StoreIdx = _atsamdx_xvi_TMP_8;
                // Simpy exit if the FLB data was not dirty
              //ib.bwOR                   ( _atsamdx_xvi_FLB_FDirty, _atsamdx_xvi_FLB_LBDirty, xvi_Tmp0                          );
              //ib.jmpIfZero              ( xvi_Tmp0               , "flb_not_dirty"                                             );
                ib.jmpIfZero              ( _atsamdx_xvi_FLB_FDirty, "flb_not_dirty"                                             ); // Ignore the lock bits
                // Set the store index to zero
                ib.mov                    ( 0                      , xvi_StoreIdx                                                );
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Ignore the lock bits
                ib.inc1                   ( xvi_StoreIdx                                                                         );
                // Read LOCK from fuse
                ib.ldrDB                  ( xvi_StoreIdx           , xvi_LOCK                                                    );
                ib.inc1                   ( xvi_StoreIdx                                                                         );
                // Read WDT ... from fuse
                ib.ldrDB                  ( xvi_StoreIdx           , xvi_WDT                                                     );
                ib.inc1                   ( xvi_StoreIdx                                                                         );
                // Read BOD33 ... from fuse
                ib.ldrDB                  ( xvi_StoreIdx           , xvi_BOD33                                                   );
                ib.inc1                   ( xvi_StoreIdx                                                                         );
                // Read EEPROM from fuse
                ib.ldrDB                  ( xvi_StoreIdx           , xvi_EEPROM                                                  );
                ib.inc1                   ( xvi_StoreIdx                                                                         );
                // Read BOOTPROT from fuse
                ib.ldrDB                  ( xvi_StoreIdx           , xvi_BOOTPROT                                                );
                ib.inc1                   ( xvi_StoreIdx                                                                         );
                // Read the user row
                ib.rdBusStr               ( adr_USER_ROW_L         , (~msk_USER_ROW_L) & 0xFFFFFFFFL, xvi_URL                    ); // URL = 0b0000000........000000000.000.000
                ib.rdBusStr               ( adr_USER_ROW_H         , (~msk_USER_ROW_H) & 0xFFFFFFFFL, xvi_URH                    ); // URH = 0b0000000000000000.......000000000
                // Modify the high part of the user row
                ib.bwLSH                  ( xvi_LOCK               , 16                             , xvi_Tmp0                   );
                ib.bwOR                   ( xvi_Tmp0               , xvi_URH                        , xvi_URH                    ); // URH = 0bLLLLLLLLLLLLLLLL.......000000000
                ib.bwAND                  ( xvi_BOD33              , 0b0000001000000000             , xvi_Tmp0                   );
                ib.bwRSH                  ( xvi_Tmp0               , 1                              , xvi_Tmp0                   );
                ib.bwOR                   ( xvi_Tmp0               , xvi_URH                        , xvi_URH                    ); // URH = 0bLLLLLLLLLLLLLLLL.......B00000000
                ib.bwAND                  ( xvi_WDT                , 0b0111111110000000             , xvi_Tmp0                   );
                ib.bwRSH                  ( xvi_Tmp0               , 7                              , xvi_Tmp0                   );
                ib.bwOR                   ( xvi_Tmp0               , xvi_URH                        , xvi_URH                    ); // URH = 0bLLLLLLLLLLLLLLLL.......BWWWWWWWW
                // Modify the low part of the user row
                ib.bwAND                  ( xvi_WDT                , 0b0000000001111111             , xvi_Tmp0                   );
                ib.bwLSH                  ( xvi_Tmp0               , 25                             , xvi_Tmp0                   );
                ib.bwOR                   ( xvi_Tmp0               , xvi_URL                        , xvi_URL                    ); // URL = 0bWWWWWWW........000000000.000.000
                ib.bwAND                  ( xvi_BOD33              , 0b0000000111111111             , xvi_Tmp0                   );
                ib.bwLSH                  ( xvi_Tmp0               , 8                              , xvi_Tmp0                   );
                ib.bwOR                   ( xvi_Tmp0               , xvi_URL                        , xvi_URL                    ); // URL = 0bWWWWWWW........BBBBBBBBB.000.000
                ib.bwAND                  ( xvi_EEPROM             , 0b0000000000000111             , xvi_Tmp0                   );
                ib.bwLSH                  ( xvi_Tmp0               , 4                              , xvi_Tmp0                   );
                ib.bwOR                   ( xvi_Tmp0               , xvi_URL                        , xvi_URL                    ); // URL = 0bWWWWWWW........BBBBBBBBB.EEE.000
                ib.bwAND                  ( xvi_BOOTPROT           , 0b0000000000000111             , xvi_Tmp0                   );
                ib.bwOR                   ( xvi_Tmp0               , xvi_URL                        , xvi_URL                    ); // URL = 0bWWWWWWW........BBBBBBBBB.EEE.PPP
                /*
                ib.debugPrintlnUBin32     ( xvi_URH                                                                              );
                ib.debugPrintlnUBin32     ( xvi_URL                                                                              );
                ib.debugPrintlnUHex08     ( xvi_URH                                                                              );
                ib.debugPrintlnUHex08     ( xvi_URL                                                                              );
                //*/
                // Prepare the page address mask
                _atsamdx_putInstGenPAdrMsk( ib                     , config.memoryFlash.pageSize                                 );
                // Check if the MCU requires manual page-commit
                _atsamdx_putInstChkNCommit( ib                                                                                   );
                // Clear the prepare the page address mask
                //*
                // Erase the user row
                ib.wrBus                  ( _atsamdx_NVMCTRL_ADDR  , adr_USER_ROW_L / 2                                          );
                ib.wrBus                  ( _atsamdx_NVMCTRL_CTRLA , _atsamdx_nvmCmd(_atsamdx_NVMCTRL_CTRLA_EAR)                 );
                ib.appendPrelinkedInst    ( _atsamdx_instruction_CheckNVMCmd                                                     );
                // Write the user row
            if(!true) {
                // ----- The default values for ATSAMD21* -----
                ib.wrBus                  ( adr_USER_ROW_L         , 0xD8E0C7FAL                                                 );
                ib.wrBus                  ( adr_USER_ROW_H         , 0xFFFFFC5DL                                                 );
            }
            else {
                ib.wrBus                  ( adr_USER_ROW_L         , xvi_URL                                                     );
                ib.wrBus                  ( adr_USER_ROW_H         , xvi_URH                                                     );
            }
            for(int i = 1; i <= config.memoryFlash.pageSize / 4 - 2; ++i) {
                ib.wrBus                  ( adr_USER_ROW_H + i * 4 , 0xFFFFFFFFL                                                 );
            }
                // Manually commit the page as needed
                ib.mov                    ( adr_USER_ROW_L         , _atsamdx_xvi_FLASH_DST_ADDR                                 );
                _atsamdx_putInstCommitPage( ib                     , config.memoryFlash.pageSize    , _atsamdx_NVMCTRL_CTRLA_WAP );
                //*/
                // Clear all flags
            ib.label                  ( "flb_not_dirty"                                                                          );
                ib.appendPrelinkedInst( _fl_atsamdx_instruction_ClearAllFLBFlags                                                 );
        }

        final long[][] instruction_WriteFLB = ib.link();

        // Generate the instructions to read the user row
        {
                // For convenience
                final XVI xvi_Tmp0     = _atsamdx_xvi_TMP_0;
                final XVI xvi_Tmp1     = _atsamdx_xvi_TMP_1;
                final XVI xvi_URL      = _atsamdx_xvi_TMP_2;
                final XVI xvi_URH      = _atsamdx_xvi_TMP_3;
                final XVI xvi_StoreIdx = _atsamdx_xvi_TMP_4;
                // Simpy exit if the FLB data was already read
                ib.jmpIfNotZero      ( _atsam3x_xvi_FLB_DoneRead, "flb_done_read"                     );
                // Read the user row
                /*
                ib.rdBusStr          ( adr_USER_ROW_L           , 0xFFFFFFFFL              , xvi_URL  );
                ib.rdBusStr          ( adr_USER_ROW_H           , 0xFFFFFFFFL              , xvi_URH  );
                ib.debugPrintlnUBin32( xvi_URH                                                        );
                ib.debugPrintlnUBin32( xvi_URL                                                        );
                ib.debugPrintlnUHex08( xvi_URH                                                        );
                ib.debugPrintlnUHex08( xvi_URL                                                        );
                //*/
                ib.rdBusStr          ( adr_USER_ROW_L           , msk_USER_ROW_L           , xvi_URL  );
                ib.rdBusStr          ( adr_USER_ROW_H           , msk_USER_ROW_H           , xvi_URH  );
                // Set the store index to zero
                ib.mov               ( 0                        , xvi_StoreIdx                        );
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Store 0x00 as lock bits
                ib.strDB             ( 0x00                     , xvi_StoreIdx                        );
                ib.inc1              ( xvi_StoreIdx                                                   );
                // Store LOCK as fuse
                ib.bwRSH             ( xvi_URH                  , 16                       , xvi_Tmp0 );
                ib.strDB             ( xvi_Tmp0                 , xvi_StoreIdx                        );
                ib.inc1              ( xvi_StoreIdx                                                   );
                // Store WDT ... as fuse
                ib.bwAND             ( xvi_URH                  , 0x000000FFL              , xvi_Tmp1 );
                ib.bwLSH             ( xvi_Tmp1                 ,  7                       , xvi_Tmp1 );
                ib.bwRSH             ( xvi_URL                  , 25                       , xvi_Tmp0 );
                ib.bwOR              ( xvi_Tmp1                 , xvi_Tmp0                 , xvi_Tmp0 );
                ib.strDB             ( xvi_Tmp0                 , xvi_StoreIdx                        );
                ib.inc1              ( xvi_StoreIdx                                                   );
                // Store BOD33 ... as fuse
                ib.bwAND             ( xvi_URH                  , 0x00000100L              , xvi_Tmp1 );
                ib.bwLSH             ( xvi_Tmp1                 , 1                        , xvi_Tmp1 );
                ib.bwAND             ( xvi_URL                  , 0x0001FF00L              , xvi_Tmp0 );
                ib.bwRSH             ( xvi_Tmp0                 , 8                        , xvi_Tmp0 );
                ib.bwOR              ( xvi_Tmp1                 , xvi_Tmp0                 , xvi_Tmp0 );
                ib.strDB             ( xvi_Tmp0                 , xvi_StoreIdx                        );
                ib.inc1              ( xvi_StoreIdx                                                   );
                // Store EEPROM as fuse
                ib.bwAND             ( xvi_URL                  , 0x00000070L              , xvi_Tmp0 );
                ib.bwRSH             ( xvi_Tmp0                 , 4                        , xvi_Tmp0 );
                ib.strDB             ( xvi_Tmp0                 , xvi_StoreIdx                        );
                ib.inc1              ( xvi_StoreIdx                                                   );
                // Store BOOTPROT as fuse
                ib.bwAND             ( xvi_URL                  , 0x00000007L              , xvi_Tmp0 );
                ib.strDB             ( xvi_Tmp0                 , xvi_StoreIdx                        );
                ib.inc1              ( xvi_StoreIdx                                                   );
                // Set flag
                ib.mov               ( 1                        , _atsamdx_xvi_FLB_DoneRead           );
            ib.label                 ( "flb_done_read"                                                );
        }

        final long[][] instruction_ReadFLB = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Instantiate and return the specifier
        return new Specifier(

            0xFF                                    , // flashMemoryEmptyValue
            0                                       , // wrMaxSWDTransferSize
            0                                       , // rdMaxSWDTransferSize
            true                                    , // supportDirectFlashRead
            false                                   , // supportDirectEEPROMRead

            _fl_atsamdx_instruction_ClearAllFLBFlags, // instruction_InitializeSystemOnce
            null                                    , // instruction_UninitializeSystemExit
            null                                    , // instruction_IsFlashLocked
            null                                    , // instruction_UnlockFlash
            instruction_EraseFlash                  , // instruction_EraseFlash
            instruction_EraseFlashPages             , // instruction_EraseFlashPages
            instruction_WriteFlash                  , // instruction_WriteFlash
            null                                    , // instruction_ReadFlash
            null                                    , // instruction_IsEEPROMLocked
            null                                    , // instruction_UnlockEEPROM
            null                                    , // instruction_WriteEEPROM
            null                                    , // instruction_ReadEEPROM
            _atsamdx_xvi_FLASH_DST_ADDR             , // instruction_xviFlashEEPROMAddress
            XVI._NA_                                , // instruction_xviFlashEEPROMReadSize
            _atsamdx_xvi_SignalWorkerCommand        , // instruction_xviSignalWorkerCommand
            _atsamdx_xvi_SignalJobState             , // instruction_xviSignalJobState
            new int[config.memoryFlash.pageSize]    , // instruction_dataBuffFlash
            null                                    , // instruction_dataBuffEEPROM

            null                                    , // flProgram
            null                                    , // elProgram
            -1                                      , // addrProgStart
            -1                                      , // addrProgBuffer
            -1                                      , // addrProgSignal
            null                                    , // wrProgExtraParams
            null                                    , // rdProgExtraParams

            instruction_WriteFLB                    , // instruction_WriteFLB
            instruction_ReadFLB                     , // instruction_ReadFLB
            _atsamdx_xvi_FLB_DoneRead               , // instruction_xviFLB_DoneRead
            _atsamdx_xvi_FLB_FDirty                 , // instruction_xviFLB_FDirty
            _atsamdx_xvi_FLB_LBDirty                , // instruction_xviFLB_LBDirty
            new int[1 + 5]                            // instruction_dataBuffFLB

        );

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // ##### ??? TODO : Add a custom command for '_atsamdx_NVMCTRL_CTRLA_SSB' ??? #####
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final XVI  _atsam3x_xvi_TMP_0               = XVI._0000;
    protected static final XVI  _atsam3x_xvi_TMP_1               = XVI._0001;
    protected static final XVI  _atsam3x_xvi_TMP_2               = XVI._0002;
    protected static final XVI  _atsam3x_xvi_TMP_3               = XVI._0003;
    protected static final XVI  _atsam3x_xvi_TMP_4               = XVI._0004;
    protected static final XVI  _atsam3x_xvi_TMP_5               = XVI._0005;
    protected static final XVI  _atsam3x_xvi_TMP_6               = XVI._0006;

    protected static final XVI  _atsam3x_xvi_WR_FLASH_EEFC_Base  = XVI._0100;
    protected static final XVI  _atsam3x_xvi_WR_FLASH_EEFC_FMR   = XVI._0101;
    protected static final XVI  _atsam3x_xvi_WR_FLASH_EEFC_FCR   = XVI._0102;
    protected static final XVI  _atsam3x_xvi_WR_FLASH_EEFC_FSR   = XVI._0103;
    protected static final XVI  _atsam3x_xvi_WR_FLASH_EEFC_FRR   = XVI._0104;
    protected static final XVI  _atsam3x_xvi_WR_FLASH_FMR_Value  = XVI._0105;
    protected static final XVI  _atsam3x_xvi_WR_FLASH_FSR_Value  = XVI._0106;
    protected static final XVI  _atsam3x_xvi_WR_FLASH_FRR_Value  = XVI._0107;
    protected static final XVI  _atsam3x_xvi_WR_FLASH_dataCount  = XVI._0108;

    protected static final XVI  _atsam3x_xvi_FLASH_DST_ADDR      = XVI._0200;
    protected static final XVI  _atsam3x_xvi_SignalWorkerCommand = XVI._0201;
    protected static final XVI  _atsam3x_xvi_SignalJobState      = XVI._0202;

    protected static final XVI  _atsam3x_xvi_FLB_DoneRead        = XVI._0500;
    protected static final XVI  _atsam3x_xvi_FLB_FDirty          = XVI._0501;
    protected static final XVI  _atsam3x_xvi_FLB_LBDirty         = XVI._0502;

    protected static final long _atsam3x_EEFC0                   = 0x400E0A00L;
    protected static final long _atsam3x_EEFC1                   = 0x400E0C00L;

    protected static final long _atsam3x_EEFC_FMR_OFFSET         = 0x00;
    protected static final long _atsam3x_EEFC_FCR_OFFSET         = 0x04;
    protected static final long _atsam3x_EEFC_FSR_OFFSET         = 0x08;
    protected static final long _atsam3x_EEFC_FRR_OFFSET         = 0x0C;

    protected static final long _atsam3x_EEFC_CMD_KEY            = 0x5A;

    protected static final long _atsam3x_EEFC_CMD_WP             = 0x01;
    protected static final long _atsam3x_EEFC_CMD_EA             = 0x05;
    protected static final long _atsam3x_EEFC_CMD_SLB            = 0x08;
    protected static final long _atsam3x_EEFC_CMD_CLB            = 0x09;
    protected static final long _atsam3x_EEFC_CMD_GLB            = 0x0A;
    protected static final long _atsam3x_EEFC_CMD_SGPB           = 0x0B;
    protected static final long _atsam3x_EEFC_CMD_CGPB           = 0x0C;
    protected static final long _atsam3x_EEFC_CMD_GGPB           = 0x0D;
    protected static final long _atsam3x_EEFC_CMD_STUI           = 0x0E;
    protected static final long _atsam3x_EEFC_CMD_SPUI           = 0x0F;

    protected static long _atsam3x_eefcCmd(final long cmd, final long arg)
    { return (_atsam3x_EEFC_CMD_KEY << 24) | (arg << 8) | cmd; }

    protected static final long[][] _atsam3x_instruction_CalcEEFCAddress = new SWDExecInstBuilder() {{
        add( _atsam3x_xvi_WR_FLASH_EEFC_Base, _atsam3x_EEFC_FMR_OFFSET, _atsam3x_xvi_WR_FLASH_EEFC_FMR );
        add( _atsam3x_xvi_WR_FLASH_EEFC_Base, _atsam3x_EEFC_FCR_OFFSET, _atsam3x_xvi_WR_FLASH_EEFC_FCR );
        add( _atsam3x_xvi_WR_FLASH_EEFC_Base, _atsam3x_EEFC_FSR_OFFSET, _atsam3x_xvi_WR_FLASH_EEFC_FSR );
        add( _atsam3x_xvi_WR_FLASH_EEFC_Base, _atsam3x_EEFC_FRR_OFFSET, _atsam3x_xvi_WR_FLASH_EEFC_FRR );
    }}.link();

    // ##### !!! TODO : Is it OK to use this for both before and after the EEFC command? !!! #####
    protected static final long[][] _atsam3x_instruction_WaitEEFCReady = new SWDExecInstBuilder() {{
        // ##### !!! TODO : Use timeout !!! #####
        label       ( "eefc_wait"                                                                                                   );
            rdBusStr( _atsam3x_xvi_WR_FLASH_EEFC_FSR , 0x00000001L                                , _atsam3x_xvi_WR_FLASH_FSR_Value );
            jmpIfEQ ( _atsam3x_xvi_WR_FLASH_FSR_Value, 0x00000001L                                , "eefc_ready"                    );
            wrBus   ( _atsam3x_xvi_WR_FLASH_EEFC_FCR , _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_SPUI, 0)                                  );
            jmp     ( "eefc_wait"                                                                                                   );
        label       ( "eefc_ready"                                                                                                  );
    }}.link();

    protected static final long[][] _fl_atsam3x_instruction_ClearAllFLBFlags = new SWDExecInstBuilder() {{
        // Clear all FLB flags
        mov( 0, _atsam3x_xvi_FLB_DoneRead );
        mov( 0, _atsam3x_xvi_FLB_FDirty   );
        mov( 0, _atsam3x_xvi_FLB_LBDirty  );
    }}.link();

    public static Specifier _getSpecifier_atsam3x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Generate the instructions to erase the entire flash memory
        {
            final long[] eefcBases = new long[] { _atsam3x_EEFC0, _atsam3x_EEFC1 };
            for(final long eefcBase : eefcBases) {
                ib.mov                ( eefcBase                            , _atsam3x_xvi_WR_FLASH_EEFC_Base           );
                ib.appendPrelinkedInst( _atsam3x_instruction_CalcEEFCAddress                                            );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                              );
                ib.wrBus              ( _atsam3x_xvi_WR_FLASH_EEFC_FCR      , _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_EA, 0) );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                              );
            }
        }
        final long[][] instruction_EraseFlash = ib.link();

        // Generate the instructions to write the flash memory
        final int  writeCount = config.memoryFlash.pageSize;
        final long page0Addr  = config.memoryFlash.memBankSpec.address[0];
        final long page1Addr  = config.memoryFlash.memBankSpec.address[1];
        {
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( -1                                       , _atsam3x_xvi_FLASH_DST_ADDR           , _atsam3x_xvi_SignalWorkerCommand, _atsam3x_xvi_SignalJobState );
                // Determine the EEFC base registers address
                ib.jmpIfGTE                ( _atsam3x_xvi_FLASH_DST_ADDR              , page1Addr                             , "wf_page1"                                                    );
                ib.mov                     ( _atsam3x_EEFC0                           , _atsam3x_xvi_WR_FLASH_EEFC_Base                                                                       );
                ib.jmp                     ( "wf_set_reg"                                                                                                                                     );
            ib.label                       ( "wf_page1"                                                                                                                                       );
                ib.mov                     ( _atsam3x_EEFC1                           , _atsam3x_xvi_WR_FLASH_EEFC_Base                                                                       );
            ib.label                       ( "wf_set_reg"                                                                                                                                     );
                // Determine the EEFC register addresses
                ib.appendPrelinkedInst     ( _atsam3x_instruction_CalcEEFCAddress                                                                                                             );
                // Set the FWS field in FMR
                ib.rdBusStr                ( _atsam3x_xvi_WR_FLASH_EEFC_FMR           , 0xFFFFF0FFL                           , _atsam3x_xvi_WR_FLASH_FMR_Value                               );
                ib.bwOR                    ( _atsam3x_xvi_WR_FLASH_FMR_Value          , 0x00000600L                           , _atsam3x_xvi_WR_FLASH_FMR_Value                               );
                ib.wrBus                   ( _atsam3x_xvi_WR_FLASH_EEFC_FMR           , _atsam3x_xvi_WR_FLASH_FMR_Value                                                                       );
                // Write the bytes
                ib.wrCMem                  ( _atsam3x_xvi_FLASH_DST_ADDR              , writeCount                          /*, 0 */                                                          );
                ib.add                     ( _atsam3x_xvi_WR_FLASH_dataCount          , writeCount                            , _atsam3x_xvi_WR_FLASH_dataCount                               );
                ib.jmpIfNEQ                ( _atsam3x_xvi_WR_FLASH_dataCount          , config.memoryFlash.pageSize           , "wf_page_not_done"                                            );
                // Commit the page (use '_atsam3x_xvi_WR_FLASH_dataCount as a temporary' variable to generate the command)
                ib.jmpIfGTE                ( _atsam3x_xvi_FLASH_DST_ADDR              , page1Addr                             , "wf_com_page1"                                                );
                ib.sub                     ( _atsam3x_xvi_FLASH_DST_ADDR              , page0Addr                             , _atsam3x_xvi_FLASH_DST_ADDR                                   );
                ib.jmp                     ( "wf_com_pageN"                                                                                                                                   );
            ib.label                       ( "wf_com_page1"                                                                                                                                   );
                ib.sub                     ( _atsam3x_xvi_FLASH_DST_ADDR              , page1Addr                             , _atsam3x_xvi_FLASH_DST_ADDR                                   );
            ib.label                       ( "wf_com_pageN"                                                                                                                                   );
                ib.bwRSH                   ( _atsam3x_xvi_FLASH_DST_ADDR              , XCom.log2(config.memoryFlash.pageSize), _atsam3x_xvi_FLASH_DST_ADDR                                   );
                ib.bwLSH                   ( _atsam3x_xvi_FLASH_DST_ADDR              , 8                                     , _atsam3x_xvi_FLASH_DST_ADDR                                   );
                ib.mov                     ( _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_WP, 0), _atsam3x_xvi_WR_FLASH_dataCount                                                                       );
                ib.bwOR                    ( _atsam3x_xvi_WR_FLASH_dataCount          , _atsam3x_xvi_FLASH_DST_ADDR           , _atsam3x_xvi_WR_FLASH_dataCount                               );
                ib.appendPrelinkedInst     ( _atsam3x_instruction_WaitEEFCReady                                                                                                               );
                ib.wrBus                   ( _atsam3x_xvi_WR_FLASH_EEFC_FCR           , _atsam3x_xvi_WR_FLASH_dataCount                                                                       );
                ib.appendPrelinkedInst     ( _atsam3x_instruction_WaitEEFCReady                                                                                                               );
                // Clear the data counter
                ib.mov                     ( 0x00000000L                              , _atsam3x_xvi_WR_FLASH_dataCount                                                                       );
            ib.label                       ( "wf_page_not_done"                                                                                                                               );
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( -1                                       , _atsam3x_xvi_FLASH_DST_ADDR           , _atsam3x_xvi_SignalWorkerCommand, _atsam3x_xvi_SignalJobState );
        }

        final long[][] instruction_WriteFlash = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */

        // Generate the instructions to write the GPNVM and lock bits
        {
                // For convenience
                final XVI  xvi_Tmp0     = _atsam3x_xvi_TMP_0;
                final XVI  xvi_Tmp1     = _atsam3x_xvi_TMP_1;
                final XVI  xvi_GPNVM    = _atsam3x_xvi_TMP_2;
                final XVI  xvi_LB0      = _atsam3x_xvi_TMP_3;
                final XVI  xvi_LB1      = _atsam3x_xvi_TMP_4;
                final XVI  xvi_LBN      = _atsam3x_xvi_TMP_5;
                final XVI  xvi_StoreIdx = _atsam3x_xvi_TMP_6; // These three variables are used mutually exclusively
                final XVI  xvi_PgCnt    = _atsam3x_xvi_TMP_6; // ---
                final XVI  xvi_BmCnt    = _atsam3x_xvi_TMP_6; // ---
                final long cmd_SLBx     = _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_SLB , 0);
                final long cmd_CLBx     = _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_CLB , 0);
                final long cmd_SGPBx    = _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_SGPB, 0);
                final long cmd_CGPBx    = _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_CGPB, 0);
                // --------------------------------------------------------------------
                // ----- Prepare the instructions to write flash bank N lock bits -----
                // --------------------------------------------------------------------
                ib.appendPrelinkedInst( _atsam3x_instruction_CalcEEFCAddress                                                     );
                ib.mov                ( 0x00000000L                         , xvi_PgCnt                                          );
            ib.label                  ( "flb_fbnlb_loop"                                                                         );
                ib.mov                ( 0x00000001L                         , xvi_Tmp0                                           );
                ib.bwLSH              ( xvi_Tmp0                            , xvi_PgCnt                      , xvi_Tmp0          );
                ib.bwAND              ( xvi_LBN                             , xvi_Tmp0                       , xvi_Tmp0          );
                ib.jmpIfZero          ( xvi_Tmp0                            , "flb_fbnlb_zero"                                   );
                ib.mov                ( cmd_SLBx                            , xvi_Tmp0                                           );
                ib.jmp                ( "flb_fbnlb_cmdg"                                                                         );
            ib.label                  ( "flb_fbnlb_zero"                                                                         );
                ib.mov                ( cmd_CLBx                            , xvi_Tmp0                                           );
            ib.label                  ( "flb_fbnlb_cmdg"                                                                         );
                ib.mul                ( xvi_PgCnt                           , 16384 / 256                    , xvi_Tmp1          ); // Convert sector number to page number
                ib.bwLSH              ( xvi_Tmp1                            , 8                              , xvi_Tmp1          ); // Store the page number
                ib.bwOR               ( xvi_Tmp0                            , xvi_Tmp1                       , xvi_Tmp0          ); // ---
                /*
                ib.debugPrintf        ( swdExecInst, "[%08X] = %08X\n"      , _atsam3x_xvi_WR_FLASH_EEFC_FCR , xvi_Tmp0          );
                //*/
                //*
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                       );
                ib.wrBus              ( _atsam3x_xvi_WR_FLASH_EEFC_FCR      , xvi_Tmp0                                           );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                       );
                //*/
                ib.inc1               ( xvi_PgCnt                                                                                );
                ib.jmpIfLTE           ( xvi_PgCnt                           , 15                             , "flb_fbnlb_loop"  );
                final long[][] _instruction_WriteFlashBankNLockBits = ib.link();
                // --------------------------------------------------------------------
                // --------------------------------------------------------------------
                // --------------------------------------------------------------------
                // ----- Generate the instructions to write the GPNVM and lock bits -----
                // Simpy exit if the FLB data was not dirty
                ib.bwOR               ( _atsam3x_xvi_FLB_FDirty             , _atsam3x_xvi_FLB_LBDirty       , xvi_Tmp0          );
                ib.jmpIfZero          ( xvi_Tmp0                            , "flb_not_dirty"                                    );
                // Set the store index to zero
                ib.mov                ( 0                                   , xvi_StoreIdx                                       );
                // Clear all GPNVM bits
                ib.mov                ( 0                                   , xvi_GPNVM                                          );
                // NOTE    : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // WARNING : SECURITY_BIT can only be erased by asserting the ERASE pin to high!
                // Generate GPNVM[0] (the SECURITY_BIT) from lock bits
                ib.ldrDB              ( xvi_StoreIdx                        , xvi_Tmp0                                           );
                ib.bwAND              ( xvi_Tmp0                            , 0x00000001L                    , xvi_Tmp0          );
                ib.bwOR               ( xvi_Tmp0                            , xvi_GPNVM                      , xvi_GPNVM         );
                ib.inc1               ( xvi_StoreIdx                                                                             );
                // Generate GPNVM[2:1] from fuse
                ib.ldrDB              ( xvi_StoreIdx                        , xvi_Tmp0                                           );
                ib.bwAND              ( xvi_Tmp0                            , 0x00000003L                    , xvi_Tmp0          );
                ib.bwLSH              ( xvi_Tmp0                            , 1                              , xvi_Tmp0          );
                ib.bwOR               ( xvi_Tmp0                            , xvi_GPNVM                      , xvi_GPNVM         );
                ib.inc1               ( xvi_StoreIdx                                                                             );
                // Get flash bank 0 lock bits from fuse
                ib.ldrDB              ( xvi_StoreIdx                        , xvi_LB0                                            );
                ib.inc1               ( xvi_StoreIdx                                                                             );
                // Get flash bank 1 lock bits from fuse
                ib.ldrDB              ( xvi_StoreIdx                        , xvi_LB1                                            );
                ib.inc1               ( xvi_StoreIdx                                                                             );
                /*
                ib.debugPrintlnUHex08 ( xvi_GPNVM                                                                                );
                ib.debugPrintlnUHex08 ( xvi_LB0                                                                                  );
                ib.debugPrintlnUHex08 ( xvi_LB1                                                                                  );
                //*/
                // Write flash bank 0 lock bits
                ib.mov                ( xvi_LB0                             , xvi_LBN                                            );
                ib.mov                ( _atsam3x_EEFC0                      , _atsam3x_xvi_WR_FLASH_EEFC_Base                    );
                ib.appendPrelinkedInst( _instruction_WriteFlashBankNLockBits                                                     );
                // Write flash bank 1 lock bits
                ib.mov                ( xvi_LB1                             , xvi_LBN                                            );
                ib.mov                ( _atsam3x_EEFC1                      , _atsam3x_xvi_WR_FLASH_EEFC_Base                    );
                ib.appendPrelinkedInst( _instruction_WriteFlashBankNLockBits                                                     );
                // Write GPNVM bits
                // NOTE : Write them in reverse because once the SECURITY_BIT is set, the SWD connection will be lost!
                ib.mov                ( _atsam3x_EEFC0                      , _atsam3x_xvi_WR_FLASH_EEFC_Base                    );
                ib.appendPrelinkedInst( _atsam3x_instruction_CalcEEFCAddress                                                     );
                ib.mov                ( 0x00000002L                         , xvi_BmCnt                                          );
            ib.label                  ( "flb_gpnvm_loop"                                                                         );
                ib.mov                ( 0x00000001L                         , xvi_Tmp0                                           );
                ib.bwLSH              ( xvi_Tmp0                            , xvi_BmCnt                      , xvi_Tmp0          );
                ib.bwAND              ( xvi_GPNVM                           , xvi_Tmp0                       , xvi_Tmp0          );
                ib.jmpIfZero          ( xvi_Tmp0                            , "flb_gpnvm_zero"                                   );
                ib.mov                ( cmd_SGPBx                           , xvi_Tmp0                                           ); // !!! WARNING !!!
                ib.jmp                ( "flb_gpnvm_cmdg"                                                                         );
            ib.label                  ( "flb_gpnvm_zero"                                                                         );
                ib.mov                ( cmd_CGPBx                           , xvi_Tmp0                                           );
            ib.label                  ( "flb_gpnvm_cmdg"                                                                         );
                ib.bwLSH              ( xvi_BmCnt                           , 8                              , xvi_Tmp1          ); // Store the number of the GPNVM bit
                ib.bwOR               ( xvi_Tmp0                            , xvi_Tmp1                       , xvi_Tmp0          ); // ---
                /*
                ib.debugPrintf        ( swdExecInst, "[%08X] = %08X\n"      , _atsam3x_xvi_WR_FLASH_EEFC_FCR , xvi_Tmp0          );
                //*/
                //*
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                       );
                ib.wrBus              ( _atsam3x_xvi_WR_FLASH_EEFC_FCR      , xvi_Tmp0                                           );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                       );
                //*/
                ib.dec1               ( xvi_BmCnt                                                                                );
                ib.jmpIfGTE           ( xvi_BmCnt                           , 0                              , "flb_gpnvm_loop"  );
                // Clear all flags
            ib.label                  ( "flb_not_dirty"                                                                          );
                ib.appendPrelinkedInst( _fl_atsam3x_instruction_ClearAllFLBFlags                                                 );
        }

        final long[][] instruction_WriteFLB = ib.link();

        // Generate the instructions to read the GPNVM and lock bits
        {
                // For convenience
                final XVI xvi_Tmp0     = _atsam3x_xvi_TMP_0;
                final XVI xvi_GPNVM    = _atsam3x_xvi_TMP_1;
                final XVI xvi_LB0      = _atsam3x_xvi_TMP_2;
                final XVI xvi_LB1      = _atsam3x_xvi_TMP_3;
                final XVI xvi_StoreIdx = _atsam3x_xvi_TMP_4;
                // Simpy exit if the FLB data was already read
                ib.jmpIfNotZero       ( _atsam3x_xvi_FLB_DoneRead           , "flb_done_read"                                                              );
                // Set the store index to zero
                ib.mov                ( 0                                   , xvi_StoreIdx                                                                 );
                // Read GPNVM bits
                ib.mov                ( _atsam3x_EEFC0                      , _atsam3x_xvi_WR_FLASH_EEFC_Base                                              );
                ib.appendPrelinkedInst( _atsam3x_instruction_CalcEEFCAddress                                                                               );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                                                 );
                ib.wrBus              ( _atsam3x_xvi_WR_FLASH_EEFC_FCR      , _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_GGPB, 0)                                  );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                                                 );
                ib.rdBusStr           ( _atsam3x_xvi_WR_FLASH_EEFC_FRR      , 0xFFFFFFFFL                                , _atsam3x_xvi_WR_FLASH_FRR_Value );
                ib.mov                ( _atsam3x_xvi_WR_FLASH_FRR_Value     , xvi_GPNVM                                                                    );
                // Read flash bank 0 lock bits
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                                                 );
                ib.wrBus              ( _atsam3x_xvi_WR_FLASH_EEFC_FCR      , _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_GLB , 0)                                  );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                                                 );
                ib.rdBusStr           ( _atsam3x_xvi_WR_FLASH_EEFC_FRR      , 0xFFFFFFFFL                                , _atsam3x_xvi_WR_FLASH_FRR_Value );
                ib.mov                ( _atsam3x_xvi_WR_FLASH_FRR_Value     , xvi_LB0                                                                      );
                // Read flash bank 1 lock bits
                ib.mov                ( _atsam3x_EEFC1                      , _atsam3x_xvi_WR_FLASH_EEFC_Base                                              );
                ib.appendPrelinkedInst( _atsam3x_instruction_CalcEEFCAddress                                                                               );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                                                 );
                ib.wrBus              ( _atsam3x_xvi_WR_FLASH_EEFC_FCR      , _atsam3x_eefcCmd(_atsam3x_EEFC_CMD_GLB , 0)                                  );
                ib.appendPrelinkedInst( _atsam3x_instruction_WaitEEFCReady                                                                                 );
                ib.rdBusStr           ( _atsam3x_xvi_WR_FLASH_EEFC_FRR      , 0xFFFFFFFFL                                , _atsam3x_xvi_WR_FLASH_FRR_Value );
                ib.mov                ( _atsam3x_xvi_WR_FLASH_FRR_Value     , xvi_LB1                                                                      );
                /*
                ib.debugPrintlnUHex08 ( xvi_GPNVM                                                                                                          );
                ib.debugPrintlnUHex08 ( xvi_LB0                                                                                                            );
                ib.debugPrintlnUHex08 ( xvi_LB1                                                                                                            );
                //*/
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Store GPNVM[0] (the SECURITY_BIT) as lock bits
                ib.bwAND              ( xvi_GPNVM                           , 0x00000001L                                , xvi_Tmp0                        );
                ib.strDB              ( xvi_Tmp0                            , xvi_StoreIdx                                                                 );
                ib.inc1               ( xvi_StoreIdx                                                                                                       );
                // Store GPNVM[2:1] as fuse
                ib.bwRSH              ( xvi_GPNVM                           , 1                                          , xvi_Tmp0                        );
                ib.strDB              ( xvi_Tmp0                            , xvi_StoreIdx                                                                 );
                ib.inc1               ( xvi_StoreIdx                                                                                                       );
                // Store flash bank 0 lock bits as fuse
                ib.bwAND              ( xvi_LB0                             , 0x0000FFFFL                                , xvi_Tmp0                        );
                ib.strDB              ( xvi_Tmp0                            , xvi_StoreIdx                                                                 );
                ib.inc1               ( xvi_StoreIdx                                                                                                       );
                // Store flash bank 1 lock bits as fuse
                ib.bwAND              ( xvi_LB1                             , 0x0000FFFFL                                , xvi_Tmp0                        );
                ib.strDB              ( xvi_Tmp0                            , xvi_StoreIdx                                                                 );
                ib.inc1               ( xvi_StoreIdx                                                                                                       );
                // Set flag
                ib.mov                ( 1                                   , _atsam3x_xvi_FLB_DoneRead                                                    );
            ib.label                  ( "flb_done_read"                                                                                                    );
        }

        final long[][] instruction_ReadFLB = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Instantiate and return the specifier
        return new Specifier(

            0xFF                                    , // flashMemoryEmptyValue
            0                                       , // wrMaxSWDTransferSize
            0                                       , // rdMaxSWDTransferSize
            true                                    , // supportDirectFlashRead
            false                                   , // supportDirectEEPROMRead

            _fl_atsam3x_instruction_ClearAllFLBFlags, // instruction_InitializeSystemOnce
            null                                    , // instruction_UninitializeSystemExit
            null                                    , // instruction_IsFlashLocked
            null                                    , // instruction_UnlockFlash
            instruction_EraseFlash                  , // instruction_EraseFlash
            null                                    , // instruction_EraseFlashPages
            instruction_WriteFlash                  , // instruction_WriteFlash
            null                                    , // instruction_ReadFlash
            null                                    , // instruction_IsEEPROMLocked
            null                                    , // instruction_UnlockEEPROM
            null                                    , // instruction_WriteEEPROM
            null                                    , // instruction_ReadEEPROM
            _atsam3x_xvi_FLASH_DST_ADDR             , // instruction_xviFlashEEPROMAddress
            XVI._NA_                                , // instruction_xviFlashEEPROMReadSize
            _atsam3x_xvi_SignalWorkerCommand        , // instruction_xviSignalWorkerCommand
            _atsam3x_xvi_SignalJobState             , // instruction_xviSignalJobState
            new int[config.memoryFlash.pageSize]    , // instruction_dataBuffFlash
            null                                    , // instruction_dataBuffEEPROM

            null                                    , // flProgram
            null                                    , // elProgram
            -1                                      , // addrProgStart
            -1                                      , // addrProgBuffer
            -1                                      , // addrProgSignal
            null                                    , // wrProgExtraParams
            null                                    , // rdProgExtraParams

            instruction_WriteFLB                    , // instruction_WriteFLB
            instruction_ReadFLB                     , // instruction_ReadFLB
            _atsam3x_xvi_FLB_DoneRead               , // instruction_xviFLB_DoneRead
            _atsam3x_xvi_FLB_FDirty                 , // instruction_xviFLB_FDirty
            _atsam3x_xvi_FLB_LBDirty                , // instruction_xviFLB_LBDirty
            new int[1 + 3]                            // instruction_dataBuffFLB

        );
    }

} // class SWDFlashLoaderATSAM
