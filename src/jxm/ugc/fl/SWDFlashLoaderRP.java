/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.fl;


import jxm.*;
import jxm.ugc.*;

import static jxm.ugc.ARMCortexMThumb.CoProc.*;
import static jxm.ugc.ARMCortexMThumb.CPU.*;
import static jxm.ugc.ARMCortexMThumb.CReg.*;
import static jxm.ugc.ARMCortexMThumb.Reg.*;
import static jxm.ugc.ARMCortexMThumb.SYSm.*;


/*
 * This class is written based on the algorithms and information found from:
 *
 *     RP2040 Datasheet
 *     https://datasheets.raspberrypi.com/rp2040/rp2040-datasheet.pdf
 *
 *     RP2350 Datasheet
 *     https://datasheets.raspberrypi.com/rp2350/rp2350-datasheet.pdf
 *
 *     RISC-V External Debug Support Version 0.13.2
 *     https://riscv.org/wp-content/uploads/2019/03/riscv-debug-release.pdf
 *
 * ~~~ Last accessed & checked on 2024-12-03 ~~~
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * Please refer to the comment block before the 'ProgSWD' class definition in the 'ProgSWD.java' file for more details and information.
 */
public class SWDFlashLoaderRP extends SWDFlashLoader {

    // ##### !!! TODO : It seems that if the transfer size is too large, the data read/written using SWD will be corrupted !!! #####
    protected static final int  _rp2040_maxSWDTransferSize         = 1024;

    protected static final XVI  _rp2040_xvi_TMP_0                  = XVI._0000;
    protected static final XVI  _rp2040_xvi_TMP_1                  = XVI._0001;

    protected static final XVI  _rp2040_xvi_STACK_TOP              = XVI._0003;

    protected static final XVI  _rp2040_xvi_CONNECT_INTERNAL_FLASH = XVI._0100;
    protected static final XVI  _rp2040_xvi_FLASH_EXIT_XIP         = XVI._0101;
    protected static final XVI  _rp2040_xvi_FLASH_RANGE_ERASE      = XVI._0102;
    protected static final XVI  _rp2040_xvi_FLASH_RANGE_PROGRAM    = XVI._0103;
    protected static final XVI  _rp2040_xvi_FLASH_FLUSH_CACHE      = XVI._0104;
    protected static final XVI  _rp2040_xvi_FLASH_ENTER_CMD_XIP    = XVI._0105;
    protected static final XVI  _rp2040_xvi_DEBUG_TRAMPOLINE       = XVI._0106;
    protected static final XVI  _rp2040_xvi_DEBUG_TRAMPOLINE_END   = XVI._0107;

    protected static final XVI  _rp2040_xvi_FLASH_DST_ADDR         = XVI._0200;
    protected static final XVI  _rp2040_xvi_SignalWorkerCommand    = XVI._0201;
    protected static final XVI  _rp2040_xvi_SignalJobState         = XVI._0202;

    protected static final long _rp2040_BOOTROM_MAGIC              = 0x0001754DL;
    protected static final long _rp2040_BOOTROM_MAGIC_MASK         = 0x00FFFFFFL;
    protected static final long _rp2040_BOOTROM_MAGIC_ADDR         = 0x00000010L;

    protected static final long _rp2040_DHCSR                      = 0xE000EDF0L;
    protected static final long _rp2040_DHCSR___S_HALT             = 0x00020000L;

    protected static int _rp2xxx_romFTag(final char c1, final char c2)
    { return  ( ( (int) c2 ) << 8 ) | ( (int) c1 ); }

    protected static final int[] _rp2040_rft_BOOTROM_FUNCTION = new int[] {
        _rp2xxx_romFTag('I', 'F'), // void _connect_internal_flash(void)
        _rp2xxx_romFTag('E', 'X'), // void _flash_exit_xip(void)
        _rp2xxx_romFTag('R', 'E'), // void _flash_range_erase(uint32_t addr, size_t count, uint32_t block_size, uint8_t block_cmd)
        _rp2xxx_romFTag('R', 'P'), // void _flash_range_program(uint32_t addr, const uint8_t *data, size_t count)
        _rp2xxx_romFTag('F', 'C'), // void _flash_flush_cache(void)
        _rp2xxx_romFTag('C', 'X'), // void _flash_enter_cmd_xip(void)
        _rp2xxx_romFTag('D', 'T'), // _debug_trampoline
        _rp2xxx_romFTag('D', 'E')  // _debug_trampoline_end
    };

    protected static final XVI[] _rp2040_xvi_BOOTROM_FUNCTION = new XVI[] {
         _rp2040_xvi_CONNECT_INTERNAL_FLASH,
         _rp2040_xvi_FLASH_EXIT_XIP        ,
         _rp2040_xvi_FLASH_RANGE_ERASE     ,
         _rp2040_xvi_FLASH_RANGE_PROGRAM   ,
         _rp2040_xvi_FLASH_FLUSH_CACHE     ,
         _rp2040_xvi_FLASH_ENTER_CMD_XIP   ,
         _rp2040_xvi_DEBUG_TRAMPOLINE      ,
         _rp2040_xvi_DEBUG_TRAMPOLINE_END
     };

    protected static final long[][] _fl_rp2040_instruction_WaitAndCheckROMFunctionCall = new SWDExecInstBuilder() {{
        unhaltCore          ( false             , true                                                     );
        rdBusLoopWhileCmpNEQ( _rp2040_DHCSR     , _rp2040_DHCSR___S_HALT, _rp2040_DHCSR___S_HALT           ); // ##### !!! TODO : Use timeout !!! #####
        haltCore            ( false                                                                        );
        rdCRegErrIfCmpNEQ   ( ProgSWD.CoreReg.PC, 0xFFFFFFFFL           , _rp2040_xvi_DEBUG_TRAMPOLINE_END );
    }}.link();

    protected static final long[][] _fl_rp2040_instruction_PreCallROMFunctions = new SWDExecInstBuilder() {{
        haltCore            ( false                                                  );
        wrCReg              ( ProgSWD.CoreReg.PC, _rp2040_xvi_DEBUG_TRAMPOLINE       );
        wrCReg              ( ProgSWD.CoreReg.SP, _rp2040_xvi_STACK_TOP              );
        wrCReg              ( ProgSWD.CoreReg.R7, _rp2040_xvi_CONNECT_INTERNAL_FLASH );
        appendPrelinkedInst ( _fl_rp2040_instruction_WaitAndCheckROMFunctionCall     );
        wrCReg              ( ProgSWD.CoreReg.PC, _rp2040_xvi_DEBUG_TRAMPOLINE       );
        wrCReg              ( ProgSWD.CoreReg.SP, _rp2040_xvi_STACK_TOP              );
        wrCReg              ( ProgSWD.CoreReg.R7, _rp2040_xvi_FLASH_EXIT_XIP         );
        appendPrelinkedInst ( _fl_rp2040_instruction_WaitAndCheckROMFunctionCall     );
    }}.link();

    protected static final long[][] _fl_rp2040_instruction_PostCallROMFunctions = new SWDExecInstBuilder() {{
        wrCReg              ( ProgSWD.CoreReg.PC, _rp2040_xvi_DEBUG_TRAMPOLINE    );
        wrCReg              ( ProgSWD.CoreReg.SP, _rp2040_xvi_STACK_TOP           );
        wrCReg              ( ProgSWD.CoreReg.R7, _rp2040_xvi_FLASH_FLUSH_CACHE   );
        appendPrelinkedInst ( _fl_rp2040_instruction_WaitAndCheckROMFunctionCall  );
        wrCReg              ( ProgSWD.CoreReg.PC, _rp2040_xvi_DEBUG_TRAMPOLINE    );
        wrCReg              ( ProgSWD.CoreReg.SP, _rp2040_xvi_STACK_TOP           );
        wrCReg              ( ProgSWD.CoreReg.R7, _rp2040_xvi_FLASH_ENTER_CMD_XIP );
        appendPrelinkedInst ( _fl_rp2040_instruction_WaitAndCheckROMFunctionCall  );
    }}.link();

    public static Specifier _getSpecifier_rp2040(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        // Calculate the stack top address
        final long stackTopAddress = config.memorySRAM.address + config.memorySRAM.totalSize - 256;

        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Generate the instructions to initialize the BOOTROM functions
        {
                // For convenience
                final XVI xvi_BOOTROM_ROM_FUNC_TABLE = _rp2040_xvi_TMP_0;
                final XVI xvi_BOOTROM_ENTRY_TAG      = _rp2040_xvi_TMP_1;
                // Check the BOOTROM magic number
                ib.rdBusErrIfCmpNEQ( _rp2040_BOOTROM_MAGIC_ADDR       , _rp2040_BOOTROM_MAGIC_MASK     , _rp2040_BOOTROM_MAGIC           );
            for(int i = 0; i < _rp2040_rft_BOOTROM_FUNCTION.length; ++i) {
                final long faMask = ( (_rp2040_xvi_BOOTROM_FUNCTION[i] != _rp2040_xvi_DEBUG_TRAMPOLINE    ) &&
                                      (_rp2040_xvi_BOOTROM_FUNCTION[i] != _rp2040_xvi_DEBUG_TRAMPOLINE_END)
                                    ) ? 0xFFFFL : 0xFFFEL;
               // Look-up the BOOTROM functions
                ib.rdBus16Str      ( _rp2040_BOOTROM_MAGIC_ADDR + 0x04, 0xFFFFL                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                ib.label           ( "loop_#" + i                                                                                        );
                    ib.rdBus16Str  ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0xFFFFL                        , xvi_BOOTROM_ENTRY_TAG           );
                    ib.jmpIfNEQ    ( xvi_BOOTROM_ENTRY_TAG            , _rp2040_rft_BOOTROM_FUNCTION[i], "skip_#" + i                    );
                    ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0002L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                    ib.rdBus16Str  ( xvi_BOOTROM_ROM_FUNC_TABLE       , faMask                         , _rp2040_xvi_BOOTROM_FUNCTION[i] );
                    ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0002L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                    ib.jmp         ( "done_#" + i                                                                                        );
                ib.label           ( "skip_#" + i                                                                                        );
                    ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0004L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                ib.jmp             ( "loop_#" + i                                                                                        );
                ib.label           ( "done_#" + i                                                                                        );
                // Set the stack top address
                ib.mov             ( stackTopAddress                  , _rp2040_xvi_STACK_TOP                                            );
            };
        }

        final long[][] instruction_InitializeBootROMFunctions = ib.link();

        // Generate the instructions to erase the entire flash memory
        ib.appendPrelinkedInst( _fl_rp2040_instruction_PreCallROMFunctions  );
        {
            ib.wrCReg             ( ProgSWD.CoreReg.PC, _rp2040_xvi_DEBUG_TRAMPOLINE   );
            ib.wrCReg             ( ProgSWD.CoreReg.SP, _rp2040_xvi_STACK_TOP          );
            ib.wrCReg             ( ProgSWD.CoreReg.R7, _rp2040_xvi_FLASH_RANGE_ERASE  );
            ib.wrCReg             ( ProgSWD.CoreReg.R0, 0x00000000L                    );
            ib.wrCReg             ( ProgSWD.CoreReg.R1, config.memoryFlash.totalSize   );
            ib.wrCReg             ( ProgSWD.CoreReg.R2, 0x00010000L /* 65536 */        );
            ib.wrCReg             ( ProgSWD.CoreReg.R3, 0x000000D8L /* BLOCK_ERASE */  );
            ib.appendPrelinkedInst( _fl_rp2040_instruction_WaitAndCheckROMFunctionCall );
        }
        ib.appendPrelinkedInst( _fl_rp2040_instruction_PostCallROMFunctions );

        final long[][] instruction_EraseFlash = ib.link();

        // Generate the instructions to write the flash memory
        final int writeCount = Math.min(config.memoryFlash.pageSize, _rp2040_maxSWDTransferSize);

        ib.appendPrelinkedInst( _fl_rp2040_instruction_PreCallROMFunctions  );
        {
            // Put the multi-threading prefix code
            ib.macro_mtRdWrFlsEprPrefix( config.memoryFlash.address, _rp2040_xvi_FLASH_DST_ADDR, _rp2040_xvi_SignalWorkerCommand, _rp2040_xvi_SignalJobState );
            // Write the flash
            ib.wrCReg             ( ProgSWD.CoreReg.PC, _rp2040_xvi_DEBUG_TRAMPOLINE    );
            ib.wrCReg             ( ProgSWD.CoreReg.SP, _rp2040_xvi_STACK_TOP           );
            ib.wrCReg             ( ProgSWD.CoreReg.R7, _rp2040_xvi_FLASH_RANGE_PROGRAM );
            ib.wrCReg             ( ProgSWD.CoreReg.R0, _rp2040_xvi_FLASH_DST_ADDR      );
            ib.wrCReg             ( ProgSWD.CoreReg.R1, config.memorySRAM.address       );
            ib.wrCReg             ( ProgSWD.CoreReg.R2, writeCount                      );
            ib.appendPrelinkedInst( _fl_rp2040_instruction_WaitAndCheckROMFunctionCall  );
            // Put the multi-threading suffix code
            ib.macro_mtRdWrFlsEprSuffix( config.memoryFlash.address, _rp2040_xvi_FLASH_DST_ADDR, _rp2040_xvi_SignalWorkerCommand, _rp2040_xvi_SignalJobState );
        }
        ib.appendPrelinkedInst( _fl_rp2040_instruction_PostCallROMFunctions );

        if(ProgSWDLowLevel.USE_MULTI_CMD_FOR_WR_CORE_MEM && ProgSWDLowLevel.USE_MULTI_CMD_FOR_RD_CORE_MEM) {
            // Without this verify after write will fail!
            // ##### ??? TODO : WHY ??? SWD clock is too fast ??? #####
            if(!true) {
                ib.unhaltCore( true , true );
                ib.haltCore  ( false       );
            }
            else {
                // ##### ??? TODO : It this better than the above one ??? #####
                // ##### ??? TODO : Is the breakpoint address correct ??? #####
                // ##### ??? TODO : Refactor with the one for RP2350  ??? #####
                ib.haltCore  ( true                                                            );
                ib.wrtBits   ( 0xE0002008L, (0b11L << 30) | ( (0x10000100 >>> 2) << 2 ) | 0b1L ); // Set a breakpoint at 0x10000100 (the start of the main flash image)
                ib.setBits   ( 0xE0002000L, 0b00000011                                         ); // ---
                ib.unhaltCore( false      , true                                               );
                ib.delayMS   ( 500                                                             );
                ib.haltCore  ( false                                                           );
                ib.modBits   ( 0xE0002000L, 0b00000011 , 0b00000010                            ); // Clear the breakpoint
                ib.wrtBits   ( 0xE0002008L, 0xDFFFFFFCL                                        ); // ---
            }
            /*
            // ##### ??? TODO : WHY THE CODE BELOW DOES NOT WORK ??? #####
                                                ib.delayMS            ( 100                                                       );
                                                ib.haltCore           ( true                                                      );
                                                ib.appendPrelinkedInst( _fl_rp2040_instruction_PreCallROMFunctions                );
                                                ib.appendPrelinkedInst( _fl_rp2040_instruction_PostCallROMFunctions               );
                                                ib.delayMS            ( 100                                                       );
            for(int i = 0; i < 256 / 4; ++i)    ib.rdBusSB            ( config.memoryFlash.address +        i * 4, 0xFFFFFFFFL, i );
            for(int i = 0; i < 256 / 4; ++i)    ib.wrBusSB            ( config.memorySRAM .address + 1024 + i * 4             , i );
                                                /*
                                                 *     $adr ( Reg.R7, "boot2_exit"               );
                                                 *     $movs( Reg.R6, 1                          );
                                                 *     $orrs( Reg.R7, Reg.R6                     );
                                                 *     $mov ( Reg.LR, Reg.R7                     );
                                                 *     $ldri( Reg.R7, ( 0x20000000L + 1024 ) | 1 );
                                                 *     $bx  ( Reg.R7                             );
                                                 *     $bkpt( 0                                  );
                                                 *     $bkpt( 0                                  );
                                                 * label    ( "boot2_exit"                       );
                                                 *     $bkpt( 0                                  );
                                                 *     $bkpt( 0                                  );
                                                 *     $bkpt( 0                                  );
                                                 * /
                                                ib.wrBus              ( config.memorySRAM .address +        0 * 4, 0x2601A703L    );
                                                ib.wrBus              ( config.memorySRAM .address +        1 * 4, 0x46BE4337L    );
                                                ib.wrBus              ( config.memorySRAM .address +        2 * 4, 0x47384F03L    );
                                                ib.wrBus              ( config.memorySRAM .address +        3 * 4, 0xBE00BE00L    );
                                                ib.wrBus              ( config.memorySRAM .address +        4 * 4, 0xBE00BE00L    );
                                                ib.wrBus              ( config.memorySRAM .address +        5 * 4, 0x46C0BE00L    );
                                                ib.wrBus              ( config.memorySRAM .address +        6 * 4, 0x20000401L    );
                                                ib.wrCReg             ( ProgSWD.CoreReg.PC, config.memorySRAM.address             );
                                                ib.wrCReg             ( ProgSWD.CoreReg.SP, _rp2040_xvi_STACK_TOP                 );
                                                ib.unhaltCore         ( false, true                                               );
                                                ib.delayMS            ( 800                                                       );
                                              //ib.lpWaitBKPT         (                                                           );
                                                ib.haltCore           ( false                                                     );
            */
        }

        final long[][] instruction_WriteFlash = ib.link();

         // Instantiate and return the specifier
        return new Specifier(

            0x00                                  , // flashMemoryEmptyValue
            _rp2040_maxSWDTransferSize            , // wrMaxSWDTransferSize
            _rp2040_maxSWDTransferSize            , // rdMaxSWDTransferSize
            true                                  , // supportDirectFlashRead
            false                                 , // supportDirectEEPROMRead

            instruction_InitializeBootROMFunctions, // instruction_InitializeSystemOnce
            null                                  , // instruction_UninitializeSystemExit
            null                                  , // instruction_IsFlashLocked
            null                                  , // instruction_UnlockFlash
            instruction_EraseFlash                , // instruction_EraseFlash
            null                                  , // instruction_EraseFlashPages
            instruction_WriteFlash                , // instruction_WriteFlash
            null                                  , // instruction_ReadFlash
            null                                  , // instruction_IsEEPROMLocked
            null                                  , // instruction_UnlockEEPROM
            null                                  , // instruction_WriteEEPROM
            null                                  , // instruction_ReadEEPROM
            _rp2040_xvi_FLASH_DST_ADDR            , // instruction_xviFlashEEPROMAddress
            XVI._NA_                              , // instruction_xviFlashEEPROMReadSize
            _rp2040_xvi_SignalWorkerCommand       , // instruction_xviSignalWorkerCommand
            _rp2040_xvi_SignalJobState            , // instruction_xviSignalJobState
            null                                  , // instruction_dataBuffFlash
            null                                  , // instruction_dataBuffEEPROM

            null                                  , // flProgram
            null                                  , // elProgram
            -1                                    , // addrProgStart
            config.memorySRAM.address             , // addrProgBuffer
            -1                                    , // addrProgSignal
            null                                  , // wrProgExtraParams
            null                                  , // rdProgExtraParams

            _fl_dummy_instruction_WriteFLB        , // instruction_WriteFLB
            _fl_dummy_instruction_ReadFLB(0x00)   , // instruction_ReadFLB
            _fl_dummy_xviFLB                      , // instruction_xviFLB_DoneRead
            _fl_dummy_xviFLB                      , // instruction_xviFLB_FDirty
            _fl_dummy_xviFLB                      , // instruction_xviFLB_LBDirty
            _fl_dummy_dataBuffFLB                   // instruction_dataBuffFLB

        );
    }

    // ##### !!! TODO : [SYSINFO_BASE + CHIP_ID/PLATFORM/GITREF_RP2040] -> emulate read-only fuses? !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * ##### ##### ##### !!! NOTICE !!! ##### ##### #####
     *     If the MCU is in USB bootloader mode:
     *         1. Use a much slower SWCLK frequency (~250kHz should work).
     *         2. Use 'rp235*_blm' instead of the normal 'rp235*' specifiers.
     * ##### ##### ##### !!! NOTICE !!! ##### ##### #####
     */

    // ##### !!! TODO : It seems that if the transfer size is too large, the data read/written using SWD will be corrupted !!! #####
    // ##### ??? TODO : It is has something to do with MEM-AP:CFG.TARINC (ADIv6.0 page C2-206); how about ADIv5.0/ADIv5.2  ??? #####
    protected static final int  _rp2350_maxSWDTransferSize            = 1024;

    protected static final XVI  _rp2350_xvi_TMP_0                     = XVI._0000;
    protected static final XVI  _rp2350_xvi_TMP_1                     = XVI._0001;
    protected static final XVI  _rp2350_xvi_TMP_2                     = XVI._0002;
    protected static final XVI  _rp2350_xvi_TMP_3                     = XVI._0003;
    protected static final XVI  _rp2350_xvi_TMP_4                     = XVI._0004;
    protected static final XVI  _rp2350_xvi_TMP_5                     = XVI._0005;
    protected static final XVI  _rp2350_xvi_TMP_6                     = XVI._0006;

    protected static final XVI  _rp2350_xvi_USR_EP                    = XVI._0050;

    protected static final XVI  _rp2350_xvi_DSCSR                     = XVI._0051;
    protected static final XVI  _rp2350_xvi_DSCSR_CHK                 = XVI._0052;

    protected static final XVI  _rp2350_xvi_BOOTROM_STATE_RESET       = XVI._0100;
    protected static final XVI  _rp2350_xvi_CONNECT_INTERNAL_FLASH    = XVI._0101;
    protected static final XVI  _rp2350_xvi_FLASH_ENTER_CMD_XIP       = XVI._0102;
    protected static final XVI  _rp2350_xvi_FLASH_EXIT_XIP            = XVI._0103;
    protected static final XVI  _rp2350_xvi_FLASH_FLUSH_CACHE         = XVI._0104;
    protected static final XVI  _rp2350_xvi_FLASH_RANGE_ERASE         = XVI._0105;
    protected static final XVI  _rp2350_xvi_FLASH_RANGE_PROGRAM       = XVI._0106;
    protected static final XVI  _rp2350_xvi_FLASH_RESET_ADDRESS_TRANS = XVI._0107;
    protected static final XVI  _rp2350_xvi_REBOOT                    = XVI._0108;

    protected static final XVI  _rp2350_xvi_FLASH_DST_ADDR            = XVI._0200;
    protected static final XVI  _rp2350_xvi_SignalWorkerCommand       = XVI._0201;
    protected static final XVI  _rp2350_xvi_SignalJobState            = XVI._0202;

    protected static final XVI  _rp2350_xvi_SRAMProgramInitRCP        = XVI._0300;
    protected static final XVI  _rp2350_xvi_SRAMProgramTrampoline     = XVI._0301;

    protected static final long _rp2350_BOOTROM_MAGIC                 = 0x0002754DL;
    protected static final long _rp2350_BOOTROM_MAGIC_MASK            = 0x00FFFFFFL;
    protected static final long _rp2350_BOOTROM_MAGIC_ADDR            = 0x00000010L;

    protected static final long _rp2350_BOOTROM_VERSION_MASK          = 0xFF000000L;
    protected static final long _rp2350_BOOTROM_VERSION_SHIFT         = 24;
    protected static final long _rp2350_BOOTROM_VERSION_ADDR          = 0x00000010L;

    protected static final long _rp2350_DHCSR                         = 0xE000EDF0L;
    protected static final long _rp2350_DHCSR___S_HALT                = 0x00020000L;

    protected static final long _rp2350_ACCESSCTRL_LOCK               = 0x40060000L;
    protected static final long _rp2350_ACCESSCTRL_LOCK___DEBUG       = 0x00000008L;
    protected static final long _rp2350_ACCESSCTRL_CFGRESET           = 0x40060008L;
    protected static final long _rp2350_ACCESSCTRL_CFGRESET__RESET    = 0xACCE0001L;

    protected static final long _rp2350_CPACR                         = 0xE000ED88L;
    protected static final long _rp2350_CPACR___CP7                   = 0x0000C000L;

    protected static final long _rp2350_DSCSR                         = 0xE000EE08L;
    protected static final long _rp2350_DSCSR___CDSKEY                = 0x00020000L;
    protected static final long _rp2350_DSCSR___CDS                   = 0x00010000L;

    protected static final int[] _rp2350_rft_BOOTROM_FUNCTION = new int[] {
        _rp2xxx_romFTag('S', 'R'), // void bootrom_state_reset(uint32_t flags)
        _rp2xxx_romFTag('I', 'F'), // void connect_internal_flash(void)
        _rp2xxx_romFTag('C', 'X'), // void flash_enter_cmd_xip(void)
        _rp2xxx_romFTag('E', 'X'), // void flash_exit_xip(void)
        _rp2xxx_romFTag('F', 'C'), // void flash_flush_cache(void)
        _rp2xxx_romFTag('R', 'E'), // void flash_range_erase(uint32_t addr, size_t count, uint32_t block_size, uint8_t block_cmd)
        _rp2xxx_romFTag('R', 'P'), // void flash_range_program(uint32_t addr, const uint8_t *data, size_t count)
        _rp2xxx_romFTag('R', 'A'), // void flash_reset_address_trans(void)
        _rp2xxx_romFTag('R', 'B')  // int reboot(uint32_t flags, uint32_t delay_ms, uint32_t p0, uint32_t p1)
    };

    protected static final XVI[] _rp2350_xvi_BOOTROM_FUNCTION = new XVI[] {
         _rp2350_xvi_BOOTROM_STATE_RESET      , // NOTE : This function does not exist on pre-production RP2350 ROM (A0)
         _rp2350_xvi_CONNECT_INTERNAL_FLASH   ,
         _rp2350_xvi_FLASH_ENTER_CMD_XIP      ,
         _rp2350_xvi_FLASH_EXIT_XIP           ,
         _rp2350_xvi_FLASH_FLUSH_CACHE        ,
         _rp2350_xvi_FLASH_RANGE_ERASE        ,
         _rp2350_xvi_FLASH_RANGE_PROGRAM      ,
         _rp2350_xvi_FLASH_RESET_ADDRESS_TRANS, // NOTE : This function does not exist on pre-production RP2350 ROM (A0)
         _rp2350_xvi_REBOOT
    };

    protected static void _buildLoaderProgram_putStdPreamble(final ARMCortexMThumb __ASM__, final long stackTopAddress) throws JXMAsmError
    {
        __ASM__

        // Vector table
        .$_word(stackTopAddress                        ) // SP
        .$_word(ARMCortexMThumb.Label_Start            ) // PC [Reset]
        .$_word(ARMCortexMThumb.Label_NMI_Handler      ) //    [NMI_Handler]
        .$_word(ARMCortexMThumb.Label_HardFault_Handler) //    [HardFault_Handler]

        // Interrupt handlers
        .function_NMI_Handler()
            .$b_dot()

        .function_HardFault_Handler()
            .$b_dot()

        // The entry point
        .function_Start()

        ;;;
    }

    protected static ARMCortexMThumb _buildLoaderProgram_initRCP(final long stackTopAddress) throws JXMAsmError
    {
        /*
         * R0 ... R1  : clobbered
         * R2 ... R15 : not used
         */

        return new ARMCortexMThumb(M33) {{

                // Put the standard preamble
                _buildLoaderProgram_putStdPreamble(this, stackTopAddress);

                // Enable RCP
                $ldri( R0 , _rp2350_CPACR       );
                $ldri( R1 , _rp2350_CPACR___CP7 );
                $str ( R1 , R0                  );

                // Initialize the canary seeds only once
                $mrc ( P7 , 1  , R15, C0, C0, 0 ); // NOTE : Must use R15 here to specify APSR_nzcv
                $bmi ( "skip_ics"               );
                $mcrr( P7 , 8  , R0 , R0, C0    );
                $mcrr( P7 , 8  , R0 , R0, C1    );

                // Signal all PEs
                $sev (                          );

                // Done
            label    ( "skip_ics"               );

                // Halt execution and enter debug state
                $bkpt( 0                        );

        }};
    }

    // Initialize the RCP again here, just in case
    protected static ARMCortexMThumb _buildLoaderProgram_trampoline(final long stackTopAddress) throws JXMAsmError
    {
        /*
         * R7          : function address 1 (no function call will be made if this register is 0).
         * R8          : function address 2 (no function call will be made if this register is 0).
         * R9          : function address 3 (no function call will be made if this register is 0).
         *
         * R0 ... R3   : function call arguments (the same arguments will be used when calling all the above functions;
         *               therefore, only one of the functions can actually have arguments).
         *
         * R0          : function call return value (only function in R9 can return a value).
         *
         * R4          : not used
         * R5          : clobbered
         * R6          : not used
         * R10 ... R12 : not used
         * R13 ... R15 : clobbered
         */

        return new ARMCortexMThumb(M33) {{

                // Put the standard preamble
                _buildLoaderProgram_putStdPreamble(this, stackTopAddress);

                // Clear the stack limits
                $movs( R5    , 0                  );
                $msr ( MSPLIM, R5                 );
                $msr ( PSPLIM, R5                 );

                // Save the stack top
                $mov ( R5    , SP                 );

            label    ( "trampoline_beg"           );

                // Call the function in R7
                $ands( R7    , R7                 ); // Skip if R7 is zero
                $bzr ( "skip_func_r7"             ); // ---
                $mov ( SP    , R5                 ); // Set the stack pointer with the stack top value
                $push( R0, R1, R2, R3, R5, R8, R9 ); // Save the registers
                $blx ( R7                         ); // Call the function
                $pop ( R0, R1, R2, R3, R5, R8, R9 ); // Restore the registers
            label    ( "skip_func_r7"             );

                // Call the function in R8
                $mov ( R7    , R8                 ); // Skip if R8 is zero
                $ands( R7    , R7                 ); // ---
                $bzr ( "skip_func_r8"             ); // ---
                $mov ( SP    , R5                 ); // Set the stack pointer with the stack top value
                $push( R0, R1, R2, R3, R5,     R9 ); // Save the registers
                $blx ( R7                         ); // Call the function
                $pop ( R0, R1, R2, R3, R5,     R9 ); // Restore the registers
            label    ( "skip_func_r8"             );

                // Call the function in R9
                $mov ( R7    , R9                 ); // Skip if R9 is zero
                $ands( R7    , R7                 ); // ---
                $bzr ( "skip_func_r9"             ); // ---
                $mov ( SP    , R5                 ); // Set the stack pointer with the stack top value
                $blx ( R7                         ); // Call the function
            label    ( "skip_func_r9"             );

                // Clear R7, R8, and R9
                $movs( R7    , 0                  );
                $mov ( R8    , R7                 );
                $mov ( R9    , R7                 );

                // Break and wait here
            label    ( "trampoline_end"           );
                $bkpt( 0                          ); // Halt execution and enter debug state
                $b   ( "trampoline_beg"           ); // Go back to the beginning of the trampoline
        }};
    }

    protected static final long[][] _fl_rp2350_instruction_EnterSecureState = new SWDExecInstBuilder() {{
        // Error if ACCESSCTRL.LOCK.DEBUG is not 0
        rdBusErrIfCmpNEQ    ( _rp2350_ACCESSCTRL_LOCK    , _rp2350_ACCESSCTRL_LOCK___DEBUG        , 0                     );
        //*
        // Reset all ACCESSCTRL configuration
        // ##### !!! TODO : This does not always work when the MCU is in bootloader mode ??? WHY ??? SWD clock is too fast ??? !!! #####
        wrtBits             ( _rp2350_ACCESSCTRL_CFGRESET, _rp2350_ACCESSCTRL_CFGRESET__RESET                             );
        //*/
        // Ensure DSCSR.CDS is 1
        rdsBits             ( _rp2350_DSCSR                                                       , _rp2350_xvi_DSCSR     );
        bwAND               ( _rp2350_xvi_DSCSR          , _rp2350_DSCSR___CDS                    , _rp2350_xvi_DSCSR_CHK );
        jmpIfNotZero        ( _rp2350_xvi_DSCSR_CHK      , "already_in_secure_state"                                      );
            bwAND           ( _rp2350_xvi_DSCSR          , (~_rp2350_DSCSR___CDSKEY) & 0xFFFFFFFFL, _rp2350_xvi_DSCSR     );
            bwOR            ( _rp2350_xvi_DSCSR          ,   _rp2350_DSCSR___CDS                  , _rp2350_xvi_DSCSR     );
            wrtBits         ( _rp2350_DSCSR                                                       , _rp2350_xvi_DSCSR     );
            rdBusErrIfCmpNEQ( _rp2350_DSCSR              , _rp2350_DSCSR___CDS                    , _rp2350_DSCSR___CDS   );
        label               ( "already_in_secure_state"                                                                   );
    }}.link();

    protected static Specifier _getSpecifier_rp235xx_impl(final ProgSWD.Config config, final SWDExecInst swdExecInst, final boolean bootloaderMode) throws JXMAsmError
    {
        // Calculate the SRAM algorithm call address, the data buffer address, and the stack top address
        final long progStartAddress  = config.memorySRAM.address;
        final long dataBufferAddress = config.memorySRAM.address + 1 * 1024;
        final long stackTopAddress   = config.memorySRAM.address + config.memorySRAM.totalSize - 256;

        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Link and store the loader program
        ib.lpLinkAndStore( swdExecInst, _buildLoaderProgram_initRCP   (stackTopAddress), progStartAddress, false, _rp2350_xvi_SRAMProgramInitRCP    );
        ib.lpLinkAndStore( swdExecInst, _buildLoaderProgram_trampoline(stackTopAddress), progStartAddress, false, _rp2350_xvi_SRAMProgramTrampoline );

        final long[][] instruction_StoreLoaderProgramIndexes = ib.link();

        // Generate the instructions to execute the BOOTROM functions
        {
            // Enter secure state
            ib.appendPrelinkedInst( _fl_rp2350_instruction_EnterSecureState                                  );
            // Initialize the redundancy coprocessor (RCP) again, just in case
            ib.lpLoad             ( _rp2350_xvi_SRAMProgramInitRCP   , progStartAddress                      );
            ib.lpExecute          (                                                                          );
            ib.lpWaitBKPT         (                                                                          );
            // Initialize
            ib.lpLoad             ( _rp2350_xvi_SRAMProgramTrampoline, progStartAddress                      );
            ib.wrCReg             ( ProgSWD.CoreReg.R7               , _rp2350_xvi_BOOTROM_STATE_RESET       );
            ib.wrCReg             ( ProgSWD.CoreReg.R0               , 0x0001 /* STATE_RESET_CURRENT_CORE */ );
            ib.wrCReg             ( ProgSWD.CoreReg.R8               , _rp2350_xvi_CONNECT_INTERNAL_FLASH    );
            ib.wrCReg             ( ProgSWD.CoreReg.R9               , _rp2350_xvi_FLASH_EXIT_XIP            );
            ib.lpExecute          (                                                                          );
            ib.lpWaitBKPT         (                                                                          );
        }

        final long[][] instruction_PreCallROMFunctions = ib.link();

        {
            // Uninitialize
            ib.wrCReg             ( ProgSWD.CoreReg.R7               , _rp2350_xvi_FLASH_FLUSH_CACHE         );
            ib.wrCReg             ( ProgSWD.CoreReg.R8               , _rp2350_xvi_FLASH_ENTER_CMD_XIP       );
            ib.wrCReg             ( ProgSWD.CoreReg.R9               , _rp2350_xvi_FLASH_RESET_ADDRESS_TRANS );
            ib.lpContinue         (                                                                          );
            ib.lpWaitBKPT         (                                                                          );
        }

        final long[][] instruction_PostCallROMFunctions = ib.link();

        // Generate the instructions to initialize the BOOTROM functions
        {
                // For convenience
                final XVI xvi_BOOTROM_VERSION        = _rp2350_xvi_TMP_0;
                final XVI xvi_BOOTROM_ROM_FUNC_TABLE = _rp2350_xvi_TMP_1;
                final XVI xvi_BOOTROM_ENTRY_TAG      = _rp2350_xvi_TMP_2;
                final XVI xvi_BOOTROM_ENTRY_FLAG     = _rp2350_xvi_TMP_3;
                final XVI xvi_BOOTROM_MATCH_FLAG     = _rp2350_xvi_TMP_4;
                final XVI xvi_BOOTROM_RISCV_FUNC     = _rp2350_xvi_TMP_5;
                final XVI xvi_BOOTROM_TMP_0          = _rp2350_xvi_TMP_6;
                // ===== Add the prelinked instructions to store the loader program indexes =====
                ib.appendPrelinkedInst ( instruction_StoreLoaderProgramIndexes                                                               );
                // ===== Check the BOOTROM magic number =====
                ib.rdBusErrIfCmpNEQ    ( _rp2350_BOOTROM_MAGIC_ADDR       , _rp2350_BOOTROM_MAGIC_MASK     , _rp2350_BOOTROM_MAGIC           );
                // ===== Read the BOOTROM version =====
                ib.rdBusStr            ( _rp2350_BOOTROM_VERSION_ADDR     , _rp2350_BOOTROM_VERSION_MASK   , xvi_BOOTROM_VERSION             );
                ib.bwRSH               ( xvi_BOOTROM_VERSION              , _rp2350_BOOTROM_VERSION_SHIFT  , xvi_BOOTROM_VERSION             );
                /*
                ib.debugPrintfln       ( swdExecInst                      , "BOOTROM_VERSION = %02X"       , xvi_BOOTROM_VERSION             );
                //*/
                // ===== Look-up the BOOTROM functions =====
            for(int i = 0; i < _rp2350_rft_BOOTROM_FUNCTION.length; ++i) {
                // ===== Clear the entry value first =====
                ib.mov                 ( 0                                                                 , _rp2350_xvi_BOOTROM_FUNCTION[i] );
                // ===== Look-up the BOOTROM function =====
                ib.rdBus16Str          ( _rp2350_BOOTROM_MAGIC_ADDR + 0x04, 0xFFFFL                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                ib.jmpIfLT             ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x7C00                         , "oldst_#" + i                   );
                // ===== New style =====
                ib.label               ( "newst_#" + i                                                                                       );
                    ib.label           ( "nloop_#" + i                                                                                       );
                        // Read the entry tag
                        ib.rdBus16Str  ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0xFFFFL                        , xvi_BOOTROM_ENTRY_TAG           );
                        ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0002L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                        // Exit the entry tag is zero
                        ib.jmpIfZero   ( xvi_BOOTROM_ENTRY_TAG                                             , "extst_#" + i                   );
                        // Read the entry flag
                        ib.rdBus16Str  ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0xFFFFL                        , xvi_BOOTROM_ENTRY_FLAG          );
                        ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0002L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                        // Skip the entry if the tag or flag does not match
                    /*
                    ib.bwAND           ( xvi_BOOTROM_ENTRY_TAG, 0xFF, xvi_BOOTROM_TMP_0      );
                    ib.bwRSH           ( xvi_BOOTROM_ENTRY_TAG, 8   , xvi_BOOTROM_MATCH_FLAG );
                    ib.debugPrintfln   (
                        swdExecInst,
                        "FOUND [%02X, %02X] @ %08X | %08X",
                        xvi_BOOTROM_TMP_0         ,
                        xvi_BOOTROM_MATCH_FLAG    ,
                        xvi_BOOTROM_ROM_FUNC_TABLE,
                        xvi_BOOTROM_ENTRY_FLAG
                     );
                    //*/
                        ib.bwAND       ( xvi_BOOTROM_ENTRY_FLAG           , 0x04 /* FUNC_ARM_SEC */        , xvi_BOOTROM_MATCH_FLAG          );
                        ib.jmpIfNEQ    ( xvi_BOOTROM_ENTRY_TAG            , _rp2350_rft_BOOTROM_FUNCTION[i], "nskip_#" + i                   );
                        ib.jmpIfZero   ( xvi_BOOTROM_MATCH_FLAG                                            , "nskip_#" + i                   );
                        // Seek to the entry value
                        ib.bwAND       ( xvi_BOOTROM_MATCH_FLAG           , 0x01 /* FUNC_RISCV */          , xvi_BOOTROM_RISCV_FUNC          );
                    ib.label           ( "nseek_#" + i                                                                                       );
                        ib.bwAND       ( xvi_BOOTROM_MATCH_FLAG           , 1                              , xvi_BOOTROM_TMP_0               );
                        ib.jmpIfNotZero( xvi_BOOTROM_TMP_0                ,                                  "dseek_#" + i                   );
                        ib.bwAND       ( xvi_BOOTROM_ENTRY_FLAG           , 1                              , xvi_BOOTROM_TMP_0               );
                        ib.jmpIfZero   ( xvi_BOOTROM_TMP_0                ,                                  "xnadd_#" + i                   );
                        ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0002L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                    ib.label           ( "xnadd_#" + i                                                                                       );
                        ib.bwRSH       ( xvi_BOOTROM_MATCH_FLAG           , 1                              , xvi_BOOTROM_MATCH_FLAG          );
                        ib.bwRSH       ( xvi_BOOTROM_ENTRY_FLAG           , 1                              , xvi_BOOTROM_ENTRY_FLAG          );
                    ib.jmp             ( "nseek_#" + i                                                                                       );
                    ib.label           ( "dseek_#" + i                                                                                       );
                        // Read and store the entry value
                        ib.jmpIfZero   ( xvi_BOOTROM_RISCV_FUNC                                            , "nrisc_#" + i                   );
                        ib.mov         ( xvi_BOOTROM_ROM_FUNC_TABLE                                        , _rp2350_xvi_BOOTROM_FUNCTION[i] );
                        ib.jmp         ( "rdone_#" + i                                                                                       );
                    ib.label           ( "nrisc_#" + i                                                                                       );
                        ib.rdBus16Str  ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0xFFFFL                        , _rp2350_xvi_BOOTROM_FUNCTION[i] );
                        ib.bwOR        ( _rp2350_xvi_BOOTROM_FUNCTION[i]  , 1                              , _rp2350_xvi_BOOTROM_FUNCTION[i] ); // Ensure the LSB is set (Thumb Mode) to avoid hardfault
                    ib.label           ( "rdone_#" + i                                                                                       );
                    /*
                    ib.debugPrintfln   (
                        swdExecInst,
                        "MATCH [%02X, %02X] [%c, %c] %08X @ %08X | %08X"         ,
                               ( (_rp2350_rft_BOOTROM_FUNCTION[i]      ) & 0xFF ),
                               ( (_rp2350_rft_BOOTROM_FUNCTION[i] >>> 8) & 0xFF ),
                        (char) ( (_rp2350_rft_BOOTROM_FUNCTION[i]      ) & 0xFF ),
                        (char) ( (_rp2350_rft_BOOTROM_FUNCTION[i] >>> 8) & 0xFF ),
                         _rp2350_xvi_BOOTROM_FUNCTION[i]                         ,
                         xvi_BOOTROM_ROM_FUNC_TABLE                              ,
                         xvi_BOOTROM_RISCV_FUNC
                    );
                    //*/
                    ib.jmp             ( "ndone_#" + i                                                                                       );
                        // Skip past this entry
                    ib.label           ( "nskip_#" + i                                                                                       );
                        ib.jmpIfZero   ( xvi_BOOTROM_ENTRY_FLAG                                            , "nloop_#" + i                   );
                        ib.bwAND       ( xvi_BOOTROM_ENTRY_FLAG           , 1                              , xvi_BOOTROM_MATCH_FLAG          );
                        ib.jmpIfZero   ( xvi_BOOTROM_MATCH_FLAG                                            , "snadd_#" + i                   );
                        ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0002L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                    ib.label           ( "snadd_#" + i                                                                                       );
                        ib.bwRSH       ( xvi_BOOTROM_ENTRY_FLAG           , 1                              , xvi_BOOTROM_ENTRY_FLAG          );
                        ib.jmp         ( "nskip_#" + i                                                                                       );
                ib.label               ( "ndone_#" + i                                                                                       );
                ib.jmp                 ( "extst_#" + i                                                                                       );
                // ===== Old style =====
                ib.label               ( "oldst_#" + i                                                                                       );
                    ib.label           ( "oloop_#" + i                                                                                       );
                        // Read the entry tag
                        ib.rdBus16Str  ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0xFFFFL                        , xvi_BOOTROM_ENTRY_TAG           );
                        ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0002L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                        // Exit the entry tag is zero
                        ib.jmpIfZero   ( xvi_BOOTROM_ENTRY_TAG                                             , "extst_#" + i                   );
                        // Read the entry value
                        ib.rdBus16Str  ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0xFFFFL                        , xvi_BOOTROM_ENTRY_FLAG          );
                        ib.add         ( xvi_BOOTROM_ROM_FUNC_TABLE       , 0x0002L                        , xvi_BOOTROM_ROM_FUNC_TABLE      );
                        // Check the next entry if the tag does not match
                    ib.jmpIfNEQ        ( xvi_BOOTROM_ENTRY_TAG            , _rp2350_rft_BOOTROM_FUNCTION[i], "oloop_#" + i                   );
                        // Store the entry value
                        ib.mov         ( xvi_BOOTROM_ENTRY_FLAG                                            , _rp2350_xvi_BOOTROM_FUNCTION[i] );
                        ib.bwOR        ( _rp2350_xvi_BOOTROM_FUNCTION[i]  , 1                              , _rp2350_xvi_BOOTROM_FUNCTION[i] ); // Ensure the LSB is set (Thumb Mode) to avoid hardfault
                ib.label               ( "extst_#" + i                                                                                       );
             }
                // ===== CSW.PROT must be set to 0b0100011 (0x23) to enable SRAM and FLASH access over SWD =====
                // ##### ??? TODO : Export 'enum MemAP' values to 'SWDExecInstBuilder' ??? #####
              //ib.wrRawMemAP          ( 0                                , 0x00                           , 0x23800052L                     );
                ib.rdRawMemAP          ( 0                                , 0x00                           , xvi_BOOTROM_TMP_0               );
                ib.bwAND               ( xvi_BOOTROM_TMP_0                , 0x80FFFFFFL                    , xvi_BOOTROM_TMP_0               );
                ib.bwOR                ( xvi_BOOTROM_TMP_0                , 0x23000000L                    , xvi_BOOTROM_TMP_0               );
                ib.wrRawMemAP          ( 0                                , 0x00                           , xvi_BOOTROM_TMP_0               );
                // ===== Enter secure state =====
                ib.appendPrelinkedInst ( _fl_rp2350_instruction_EnterSecureState                                                             );
                /*
                // ===== Print CSW =====
                ib.rdRawMemAP          ( 0                                , 0x00                           , xvi_BOOTROM_TMP_0               );
                ib.debugPrintfln       ( swdExecInst                      , "CSW = %08X"                   , xvi_BOOTROM_TMP_0               ); // 0x03800052
                // ===== Test SRAM access =====
                ib.haltCore            ( false                                                                                               );
                ib.rdBusStr            ( 0x20000000L                      , 0xFFFFFFFFL                    , xvi_BOOTROM_ENTRY_FLAG          );
                ib.debugPrintfln       ( swdExecInst                      , "@@@ %08X"                     , xvi_BOOTROM_ENTRY_FLAG          );
                ib.wrBus               ( 0x20000000L                      , 0xAA55AA55L                                                      );
                ib.rdBusStr            ( 0x20000000L                      , 0xFFFFFFFFL                    , xvi_BOOTROM_ENTRY_FLAG          );
                ib.debugPrintfln       ( swdExecInst                      , "@@@ %08X"                     , xvi_BOOTROM_ENTRY_FLAG          );
                //*/
            if(!bootloaderMode) {
                // ===== Initialize the redundancy coprocessor (RCP) =====
                ib.lpLoad              ( _rp2350_xvi_SRAMProgramInitRCP   , progStartAddress                                                 );
                ib.lpExecute           (                                                                                                     );
                ib.lpWaitBKPT          (                                                                                                     );
            }
            else {
                /*
                 * NOTE : # The MCU might need a manual reset after the end of the session!
                 *        # The address '0x000002F8L' is from:
                 *              https://github.com/raspberrypi/pico-bootrom-rp2350/releases/download/A2/arm-bootrom.dis
                 *                  ...
                 *                  000002f8 <native_dead>
                 *                  ...
                 *          It may need to be changed for other MCUs with different BOOTROM versions; maybe use 'xvi_BOOTROM_VERSION'
                 *          or search for the location of any 'BKPT' instruction in the BOOTROM?
                 */
                final long regLRValue = 0x000002F8L | 1 ;
                // ===== Reboot with 'PICOBOOT' and 'BOOTSEL USB' disabled
                ib.haltCore            ( false                                                                                               );
                ib.debugPrintfln       ( swdExecInst                      , "[BOOTROM] bootrom_state_reset(0x0001)"                          );
                ib.bwAND               ( _rp2350_xvi_BOOTROM_STATE_RESET  , 0xFFFFFFFEL                  , xvi_BOOTROM_TMP_0                 );
                ib.wrBus               ( 0xE000ED08L                      , 0x00000000L /* VTOR = 0x00000000 */                              );
                ib.wrCReg              ( ProgSWD.CoreReg.XPSR             , 0x01000000L /* Ensure the core runs in Thumb mode */             );
                ib.wrCReg              ( ProgSWD.CoreReg.MSPLIM_S         , 0x00000000L                                                      );
                ib.wrCReg              ( ProgSWD.CoreReg.PSPLIM_S         , 0x00000000L                                                      );
                ib.wrCReg              ( ProgSWD.CoreReg.SP               , stackTopAddress                                                  );
                ib.wrCReg              ( ProgSWD.CoreReg.PC               , xvi_BOOTROM_TMP_0                                                );
                ib.wrCReg              ( ProgSWD.CoreReg.LR               , regLRValue                                                       );
                ib.wrCReg              ( ProgSWD.CoreReg.R0               , 0x0001      /* STATE_RESET_CURRENT_CORE */                       );
                ib.unhaltCore          ( false                            , true                                                             );
                ib.lpWaitBKPT          (                                                                                                     );
                ib.debugPrintfln       ( swdExecInst                      , "[BOOTROM] reboot(0x0012, 1000, 25, 0x03)"                       );
                ib.bwAND               ( _rp2350_xvi_REBOOT               , 0xFFFFFFFEL                  , xvi_BOOTROM_TMP_0                 );
                ib.wrBus               ( 0xE000ED08L                      , 0x00000000L /* VTOR = 0x00000000 */                              );
                ib.wrCReg              ( ProgSWD.CoreReg.XPSR             , 0x01000000L /* Ensure the core runs in Thumb mode */             );
                ib.wrCReg              ( ProgSWD.CoreReg.MSPLIM_S         , 0x00000000L                                                      );
                ib.wrCReg              ( ProgSWD.CoreReg.PSPLIM_S         , 0x00000000L                                                      );
                ib.wrCReg              ( ProgSWD.CoreReg.SP               , stackTopAddress                                                  );
                ib.wrCReg              ( ProgSWD.CoreReg.PC               , xvi_BOOTROM_TMP_0                                                );
                ib.wrCReg              ( ProgSWD.CoreReg.LR               , regLRValue                                                       );
                ib.wrCReg              ( ProgSWD.CoreReg.R0               , 0x0012      /* REBOOT_TO_ARM | REBOOT_TYPE_BOOTSEL */            );
                ib.wrCReg              ( ProgSWD.CoreReg.R1               , 1000        /* Delay 1000mS */                                   );
                ib.wrCReg              ( ProgSWD.CoreReg.R2               , 25          /* GPIO 25 */                                        );
                ib.wrCReg              ( ProgSWD.CoreReg.R3               , 0x03        /* DISABLE_[PICOBOOT|MSD]_INTERFACE */               );
                ib.unhaltCore          ( false                            , true                                                             );
                ib.lpWaitBKPT          (                                                                                                     );
                ib.delayMS             ( 1000                                                                                                );
                ib.swdLineReset        (                                                                                                     );
                // ===== Enter secure state, initialize the redundancy coprocessor (RCP), and initialize flash =====
                ib.debugPrintfln       ( swdExecInst                      , "[INSTSEQ] _fl_rp2350_instruction_EnterSecureState[]"            );
                ib.debugPrintfln       ( swdExecInst                      , "[PROGRAM] _rp2350_xvi_SRAMProgramInitRCP"                       );
                ib.debugPrintfln       ( swdExecInst                      , "[BOOTROM] bootrom_state_reset(0x0001)"                          );
                ib.debugPrintfln       ( swdExecInst                      , "[BOOTROM] connect_internal_flash()"                             );
                ib.debugPrintfln       ( swdExecInst                      , "[BOOTROM] flash_exit_xip()"                                     );
                ib.appendPrelinkedInst ( instruction_PreCallROMFunctions                                                                     );
                ib.debugPrintfln       ( swdExecInst                      , "[BOOTROM] flash_flush_cache()"                                  );
                ib.debugPrintfln       ( swdExecInst                      , "[BOOTROM] flash_enter_cmd_xip()"                                );
                ib.debugPrintfln       ( swdExecInst                      , "[BOOTROM] flash_reset_address_trans()"                          );
                ib.appendPrelinkedInst ( instruction_PostCallROMFunctions                                                                    );
                ib.debugPrintln        (                                                                                                     );
                // ##### ??? TODO : Increase the MCU clock frequency and then the SWD clock frequency ??? #####
            }
        }

        final long[][] instruction_InitializeBootROMFunctions = ib.link();

        // Generate the instructions to erase the entire flash memory
        {
            // Add the prelinked instructions prefix to call ROM functions
            ib.appendPrelinkedInst( instruction_PreCallROMFunctions                   );
            // Erase the flash
            ib.wrCReg             ( ProgSWD.CoreReg.R7, _rp2350_xvi_FLASH_RANGE_ERASE );
            ib.wrCReg             ( ProgSWD.CoreReg.R0, 0x00000000L                   );
            ib.wrCReg             ( ProgSWD.CoreReg.R1, config.memoryFlash.totalSize  );
            ib.wrCReg             ( ProgSWD.CoreReg.R2, 0x00010000L /* 65536 */       );
            ib.wrCReg             ( ProgSWD.CoreReg.R3, 0x000000D8L /* BLOCK_ERASE */ );
            ib.lpContinue         (                                                   );
            ib.lpWaitBKPT         (                                                   );
            // Add the prelinked instructions postfix to call ROM functions
            ib.appendPrelinkedInst( instruction_PostCallROMFunctions                  );
        }

        final long[][] instruction_EraseFlash = ib.link();

        // Generate the instructions to write the flash memory
        final int writeCount = Math.min(config.memoryFlash.pageSize, _rp2350_maxSWDTransferSize);

        {
                // For convenience
                final XVI xvi_WF_FIRST = _rp2350_xvi_TMP_0;
                // Clear the flag
                ib.mov                ( 0                    , xvi_WF_FIRST                    );
                // Add the prelinked instructions prefix to call ROM functions
                ib.appendPrelinkedInst( instruction_PreCallROMFunctions                        );
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( config.memoryFlash.address, _rp2350_xvi_FLASH_DST_ADDR, _rp2350_xvi_SignalWorkerCommand, _rp2350_xvi_SignalJobState );
                // Get and save the user entry point address
                ib.jmpIfNotZero       ( xvi_WF_FIRST         , "done_get_ep"                   );
                ib.mov                ( 1                    , xvi_WF_FIRST                    );
                ib.rdsBits            ( dataBufferAddress + 4, _rp2350_xvi_USR_EP              );
                ib.bwOR               ( _rp2350_xvi_USR_EP, 1, _rp2350_xvi_USR_EP              ); // [31:1] -> BPADDR ; [0] -> BE
                /*
                ib.debugPrintfln      ( swdExecInst, "%08X"  , _rp2350_xvi_USR_EP              );
                //*/
        ib.label                      ( "done_get_ep"                                          );
                // Write the flash
                ib.wrCReg             ( ProgSWD.CoreReg.R7   , _rp2350_xvi_FLASH_RANGE_PROGRAM );
                ib.wrCReg             ( ProgSWD.CoreReg.R0   , _rp2040_xvi_FLASH_DST_ADDR      );
                ib.wrCReg             ( ProgSWD.CoreReg.R1   , dataBufferAddress               );
                ib.wrCReg             ( ProgSWD.CoreReg.R2   , writeCount                      );
                ib.lpContinue         (                                                        );
                ib.lpWaitBKPT         (                                                        );
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( config.memoryFlash.address, _rp2350_xvi_FLASH_DST_ADDR, _rp2350_xvi_SignalWorkerCommand, _rp2350_xvi_SignalJobState );
                // Add the prelinked instructions postfix to call ROM functions
                ib.appendPrelinkedInst( instruction_PostCallROMFunctions                       );
        }

        if(ProgSWDLowLevel.USE_MULTI_CMD_FOR_WR_CORE_MEM && ProgSWDLowLevel.USE_MULTI_CMD_FOR_RD_CORE_MEM) {
            // Without this verify after write will fail!
            // ##### ??? TODO : WHY ??? SWD clock is too fast ??? #####
            if(bootloaderMode) {
                ib.resetUnhaltAllCores();
                ib.haltAllCores       ();
                // ##### ??? TODO : Why if the MCU is in USB bootloader mode, the code below does not work ??? #####
            }
            else {
                // ##### ??? TODO : It this better than the above one ??? #####
                // ##### ??? TODO : Is the breakpoint address correct ??? #####
                // ##### ??? TODO : Refactor with the one for RP2040  ??? #####
                ib.haltCore  ( true                                 );
                ib.wrtBits   ( 0xE0002008L, _rp2350_xvi_USR_EP      ); // Set a breakpoint at '_rp2350_xvi_USR_EP' (the user program entry point)
                ib.setBits   ( 0xE0002000L, 0b00000011              ); // ---
                ib.unhaltCore( false      , true                    );
                ib.delayMS   ( 500                                  );
                ib.haltCore  ( false                                );
                ib.modBits   ( 0xE0002000L, 0b00000011 , 0b00000010 ); // Clear the breakpoint
                ib.wrtBits   ( 0xE0002008L, 0xDFFFFFFCL             ); // ---
            }
        }

        final long[][] instruction_WriteFlash = ib.link();

         // Instantiate and return the specifier
        return new Specifier(

            0x00                                  , // flashMemoryEmptyValue
            _rp2350_maxSWDTransferSize            , // wrMaxSWDTransferSize
            _rp2350_maxSWDTransferSize            , // rdMaxSWDTransferSize
            true                                  , // supportDirectFlashRead
            false                                 , // supportDirectEEPROMRead

            instruction_InitializeBootROMFunctions, // instruction_InitializeSystemOnce
            null                                  , // instruction_UninitializeSystemExit
            null                                  , // instruction_IsFlashLocked
            null                                  , // instruction_UnlockFlash
            instruction_EraseFlash                , // instruction_EraseFlash
            null                                  , // instruction_EraseFlashPages
            instruction_WriteFlash                , // instruction_WriteFlash
            null                                  , // instruction_ReadFlash
            null                                  , // instruction_IsEEPROMLocked
            null                                  , // instruction_UnlockEEPROM
            null                                  , // instruction_WriteEEPROM
            null                                  , // instruction_ReadEEPROM
            _rp2350_xvi_FLASH_DST_ADDR            , // instruction_xviFlashEEPROMAddress
            XVI._NA_                              , // instruction_xviFlashEEPROMReadSize
            _rp2350_xvi_SignalWorkerCommand       , // instruction_xviSignalWorkerCommand
            _rp2350_xvi_SignalJobState            , // instruction_xviSignalJobState
            null                                  , // instruction_dataBuffFlash
            null                                  , // instruction_dataBuffEEPROM

            null                                  , // flProgram
            null                                  , // elProgram
            -1                                    , // addrProgStart
            dataBufferAddress                     , // addrProgBuffer
            -1                                    , // addrProgSignal
            null                                  , // wrProgExtraParams
            null                                  , // rdProgExtraParams

            _fl_dummy_instruction_WriteFLB        , // instruction_WriteFLB
            _fl_dummy_instruction_ReadFLB(0x00)   , // instruction_ReadFLB
            _fl_dummy_xviFLB                      , // instruction_xviFLB_DoneRead
            _fl_dummy_xviFLB                      , // instruction_xviFLB_FDirty
            _fl_dummy_xviFLB                      , // instruction_xviFLB_LBDirty
            _fl_dummy_dataBuffFLB                   // instruction_dataBuffFLB

        );
    }

    // ##### !!! TODO : [SYSINFO_BASE + CHIP_ID/PLATFORM/GITREF_RP2350] -> emulate read-only fuses? !!! #####

    public static Specifier _getSpecifier_rp235xx(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMAsmError
    { return _getSpecifier_rp235xx_impl(config, swdExecInst, false); }

    public static Specifier _getSpecifier_rp235x(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMAsmError
    { return _getSpecifier_rp235xx_impl(config, swdExecInst, false); }

    public static Specifier _getSpecifier_rp2350(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMAsmError
    { return _getSpecifier_rp235xx_impl(config, swdExecInst, false); }

    public static Specifier _getSpecifier_rp235xx_blm(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMAsmError
    { return _getSpecifier_rp235xx_impl(config, swdExecInst, true ); }

    public static Specifier _getSpecifier_rp235x_blm(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMAsmError
    { return _getSpecifier_rp235xx_impl(config, swdExecInst, true ); }

    public static Specifier _getSpecifier_rp2350_blm(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMAsmError
    { return _getSpecifier_rp235xx_impl(config, swdExecInst, true ); }

} // class SWDFlashLoaderRP
