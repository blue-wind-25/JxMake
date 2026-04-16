/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.fl;


import jxm.*;
import jxm.ugc.*;
import jxm.ugc.stm32xf.*;
import jxm.xb.*;


/*
 * Please refer to the comment block before the 'ProgSWD' class definition in the 'ProgSWD.java' file for more details and information.
 */
public class SWDFlashLoaderSTM32_XF extends SWDFlashLoader {

    /*
     * NOTE : # For now, each flash bank for devices with multiple external flash banks needs to be programmed
     *          separately (each using a different ProgSWD.Config).
     *        # Please refer to the comments on each section for more information.
     */

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*            External Flash
     * STM32H7X   0x90000000 0x70000000

     * ##### !!! TODO : How about the external flash for STM32H7[23|25|30|33|35] ? !!! #####
     * ##### !!! TODO : How about the external flash for STM32H7[45|47|55|57   ] ? !!! #####
     * ##### !!! TODO : How about the external flash for STM32H7[A3|B0|B3      ] ? !!! #####
     * ##### !!! TODO : How about the external flash for others                ] ? !!! #####
     */

    protected static final XVI _stm32h7x_xvi_CurOperInitFlag     = XVI._0200;

    protected static final XVI _stm32h7x_xvi_FLASH_DST_ADDR      = XVI._0201;
    protected static final XVI _stm32h7x_xvi_SignalWorkerCommand = XVI._0202;
    protected static final XVI _stm32h7x_xvi_SignalJobState      = XVI._0203;

    protected static Specifier _getSpecifier_stm32h7x_w25q_impl(final STM32_ExternalFlash xflash, final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMFatalLogicError
    {
        // Prepare the instruction builder
        final SWDExecInstBuilder ib = xflash.swdExecInstBuilder();

        // Generate the instructions to initialize the system once
        final long[][] instruction_InitializeSystemOnce = XCom.arrayConcatCopy(
            xflash.instruction_resetHaltInitClock(),
            xflash.instruction_initGPIO_XF       (),
            xflash.instruction_initFlash         ()
        );

        // Generate the instructions to uninitialize on exit
        final long[][] instruction_UninitializeSystemExit = XCom.arrayConcatCopy(
            xflash.instruction_softwareReset(),
            xflash.instruction_deinitAll    ()
        );

        // Generate the instructions to erase the entire flash memory
        {
            // Clear the flag
            ib.mov                ( 0, _stm32h7x_xvi_CurOperInitFlag );
            // Put the chip erase code
            ib.appendPrelinkedInst( xflash.instruction_chipErase()   );
        }

        final long[][] instruction_EraseFlash = ib.link();

        // Generate the instructions to write the flash memory
        if( xflash.mmapEnabled() && xflash.instruction_writePageMMap() != null ) {
                // Clear the flag
                ib.mov                     ( 0, _stm32h7x_xvi_CurOperInitFlag   );
                // Put the memory-mapped write flash initialization code
                ib.appendPrelinkedInst     ( xflash.instruction_writePageMMap() );
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( -1, _stm32h7x_xvi_FLASH_DST_ADDR, _stm32h7x_xvi_SignalWorkerCommand, _stm32h7x_xvi_SignalJobState );
                // Write the flash memory
                ib.wrCMem(_stm32h7x_xvi_FLASH_DST_ADDR, config.memoryFlash.pageSize, SWDFlashLoaderSTM32._stm32h7x_rdMaxSWDTransferSize);
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( -1, _stm32h7x_xvi_FLASH_DST_ADDR, _stm32h7x_xvi_SignalWorkerCommand, _stm32h7x_xvi_SignalJobState );
                // Put the memory-mapped uninitialization code
                ib.appendPrelinkedInst     ( xflash.instruction_endMMap()       );
        }
        else {
                // Clear the flag
                ib.mov                     ( 0, _stm32h7x_xvi_CurOperInitFlag  );
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( config.memoryFlash.address, _stm32h7x_xvi_FLASH_DST_ADDR, _stm32h7x_xvi_SignalWorkerCommand, _stm32h7x_xvi_SignalJobState );
                // Put the write flash code
            if( xflash.instruction_writePageDMA() != null )
                ib.appendPrelinkedInst     ( xflash.instruction_writePageDMA() );
            else
                ib.appendPrelinkedInst     ( xflash.instruction_writePage   () );
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( config.memoryFlash.address, _stm32h7x_xvi_FLASH_DST_ADDR, _stm32h7x_xvi_SignalWorkerCommand, _stm32h7x_xvi_SignalJobState );
        }

        final long[][] instruction_WriteFlash = ib.link();

        // Generate the instructions to read the flash memory
        if( xflash.mmapEnabled() && xflash.instruction_readPageMMap() != null ) {
                // Clear the flag
                ib.mov                     ( 0, _stm32h7x_xvi_CurOperInitFlag  );
                // Put the memory-mapped read flash initialization code
                ib.appendPrelinkedInst     ( xflash.instruction_readPageMMap() );
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( -1, _stm32h7x_xvi_FLASH_DST_ADDR, _stm32h7x_xvi_SignalWorkerCommand, _stm32h7x_xvi_SignalJobState                 );
                // Read the flash memory
                ib.rdCMem                  (     _stm32h7x_xvi_FLASH_DST_ADDR, config.memoryFlash.pageSize, SWDFlashLoaderSTM32._stm32h7x_rdMaxSWDTransferSize );
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( -1, _stm32h7x_xvi_FLASH_DST_ADDR, _stm32h7x_xvi_SignalWorkerCommand, _stm32h7x_xvi_SignalJobState                 );
                // Put the memory-mapped uninitialization code
                ib.appendPrelinkedInst     ( xflash.instruction_endMMap()      );
        }
        else {
                // Clear the flag
                ib.mov                     ( 0, _stm32h7x_xvi_CurOperInitFlag  );
                // Put the multi-threading prefix code
                ib.macro_mtRdWrFlsEprPrefix( config.memoryFlash.address, _stm32h7x_xvi_FLASH_DST_ADDR, _stm32h7x_xvi_SignalWorkerCommand, _stm32h7x_xvi_SignalJobState );
                // Put the read flash code
            if( xflash.instruction_readPageDMA() != null )
                ib.appendPrelinkedInst     ( xflash.instruction_readPageDMA()  );
            else
                ib.appendPrelinkedInst     ( xflash.instruction_readPage   ()  );
                // Put the multi-threading suffix code
                ib.macro_mtRdWrFlsEprSuffix( config.memoryFlash.address, _stm32h7x_xvi_FLASH_DST_ADDR, _stm32h7x_xvi_SignalWorkerCommand, _stm32h7x_xvi_SignalJobState );
        }

        final long[][] instruction_ReadFlash = ib.link();

        // Instantiate and return the specifier
        return new Specifier(

            // NOTE : 'STM32_ExternalFlash' class implementation will handle the maximum SWD transfer size when writing and reading

            0x00                                , // flashMemoryEmptyValue
            0                                   , // wrMaxSWDTransferSize
            0                                   , // rdMaxSWDTransferSize
            false                               , // supportDirectFlashRead
            false                               , // supportDirectEEPROMRead

            instruction_InitializeSystemOnce    , // instruction_InitializeSystemOnce
            instruction_UninitializeSystemExit  , // instruction_UninitializeSystemExit
            null                                , // instruction_IsFlashLocked
            null                                , // instruction_UnlockFlash
            instruction_EraseFlash              , // instruction_EraseFlash
            null                                , // instruction_EraseFlashPages
            instruction_WriteFlash              , // instruction_WriteFlash
            instruction_ReadFlash               , // instruction_ReadFlash
            null                                , // instruction_IsEEPROMLocked
            null                                , // instruction_UnlockEEPROM
            null                                , // instruction_WriteEEPROM
            null                                , // instruction_ReadEEPROM
            _stm32h7x_xvi_FLASH_DST_ADDR        , // instruction_xviFlashEEPROMAddress
            XVI._NA_                            , // instruction_xviFlashEEPROMReadSize
            _stm32h7x_xvi_SignalWorkerCommand   , // instruction_xviSignalWorkerCommand
            _stm32h7x_xvi_SignalJobState        , // instruction_xviSignalJobState
            new int[config.memoryFlash.pageSize], // instruction_dataBuffFlash
            null                                , // instruction_dataBuffEEPROM

            null                                , // flProgram
            null                                , // elProgram
            -1                                  , // addrProgStart
            -1                                  , // addrProgBuffer
            -1                                  , // addrProgSignal
            null                                , // wrProgExtraParams
            null                                , // rdProgExtraParams

            _fl_dummy_instruction_WriteFLB      , // instruction_WriteFLB
            _fl_dummy_instruction_ReadFLB(0x00) , // instruction_ReadFLB
            _fl_dummy_xviFLB                    , // instruction_xviFLB_DoneRead
            _fl_dummy_xviFLB                    , // instruction_xviFLB_FDirty
            _fl_dummy_xviFLB                    , // instruction_xviFLB_LBDirty
            _fl_dummy_dataBuffFLB                 // instruction_dataBuffFLB

        );
    }

    /*
     * STM32H750 with W25Q* external flash
     */
    public static Specifier _getSpecifier_stm32h750_w25q(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMFatalLogicError
    {
        final STM32_ExternalFlash qspi = new STM32QSPI_Flash_W25Q(
                                             new STM32QSPI_H750(swdExecInst, config.memoryFlash.totalSize, config.memoryFlash.pageSize, true),
                                             _stm32h7x_xvi_CurOperInitFlag,
                                             _stm32h7x_xvi_FLASH_DST_ADDR
                                         );

        return _getSpecifier_stm32h7x_w25q_impl(qspi, config, swdExecInst);
    }

    public static Specifier _getSpecifier_stm32h742_w25q(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMFatalLogicError
    { return _getSpecifier_stm32h750_w25q(config, swdExecInst); }

    public static Specifier _getSpecifier_stm32h743_w25q(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMFatalLogicError
    { return _getSpecifier_stm32h750_w25q(config, swdExecInst); }

    public static Specifier _getSpecifier_stm32h753_w25q(final ProgSWD.Config config, final SWDExecInst swdExecInst) throws JXMFatalLogicError
    { return _getSpecifier_stm32h750_w25q(config, swdExecInst); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * ##### !!! TODO : Add support for more MCUs! !!! #####
     */

} // class SWDFlashLoaderSTM32_XF


    /*
                // ##### !!! TODO : Remove this later !!! #####

                // ##### EXPERIMENT : STM32H750 with W25Q64FV #####
                // ##### EXPERIMENT : STM32H750 with W25Q64FV #####
                // ##### EXPERIMENT : STM32H750 with W25Q64FV #####
                if(true) {
                    final int                 wrMaxSWDTransferSize = 128;
                    final int                 rdMaxSWDTransferSize = 512;
                    final long                flSz = 64L * 1024L * 1024L / 8L;
                    final int                 pgSz = 256;
                    final STM32_ExternalFlash qspi = new STM32QSPI_Flash_W25Q( new STM32QSPI_H750( swd.swdExecInst(), flSz, pgSz, true ), XVI._0100, XVI._0200 );
                    final int[]               dBuf = new int[pgSz];

                                        swd.swdExecInst( qspi.instruction_resetHaltInitClock(), dBuf );
                                        swd.swdExecInst( qspi.instruction_initGPIO_XF       (), dBuf );
                                        swd.swdExecInst( qspi.instruction_initFlash         (), dBuf );

                    if(!true         ) {
                                        SysUtil.stdDbg().println("Erasing Chip");
                                        final long tv1 = SysUtil.getNS();
                                        swd.swdExecInst( qspi.instruction_chipErase         (), dBuf );
                                        final long tv2 = SysUtil.getNS();
                                        SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n\n", flSz, (tv2 - tv1) * 0.000000001, 1000000000.0 * flSz / (tv2 - tv1) );
                    }

                    final int     rwPos = 1;
                    final boolean wrPg  = false;

                    swd.xviSet(XVI._0200, pgSz * rwPos);

                    java.util.Arrays.fill(dBuf, 0xFF);
                    if(wrPg) {
                        dBuf[  0] = 1; dBuf[  1] = 2; dBuf[  2] = 3; dBuf[  3] = 4;
                        dBuf[  4] = 5; dBuf[  5] = 6; dBuf[  6] = 7; dBuf[  7] = 8;
                        dBuf[255] = 9;
                    }

                    if( true &&  wrPg) {
                                        swd.xviSet(XVI._0100, 0);
                                        SysUtil.stdDbg().println("Writing Flash Page (DMA)");
                                        final long tv1 = SysUtil.getNS();
                                        swd.swdExecInst( qspi.instruction_writePageDMA      (), dBuf );
                                        final long tv2 = SysUtil.getNS();
                                        SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n\n", pgSz, (tv2 - tv1) * 0.000000001, 1000000000.0 * pgSz / (tv2 - tv1) );
                    }

                    if( true         ) {
                                        swd.swdExecInst( qspi.instruction_softwareReset     (), dBuf );
                                        swd.swdExecInst( qspi.instruction_initFlash         (), dBuf );
                    }

                    if( true ||  wrPg) {
                                        swd.xviSet(XVI._0100, 0);
                                        SysUtil.stdDbg().println("Reading Flash Page (DMA)");
                                        final long tv1 = SysUtil.getNS();
                                        swd.swdExecInst( qspi.instruction_readPageDMA       (), dBuf );
                                        final long tv2 = SysUtil.getNS();
                                        SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n\n", pgSz, (tv2 - tv1) * 0.000000001, 1000000000.0 * pgSz / (tv2 - tv1) );
                    }

                        for(int idx = 0; idx < dBuf.length; ++idx) {
                            SysUtil.stdDbg().printf("%02X ", dBuf[idx]);
                            if( (idx + 1) % 32 == 0 ) SysUtil.stdDbg().println();
                        }
                        SysUtil.stdDbg().println();

                    if( true         ) {
                                        swd.xviSet(XVI._0100, 0);
                                        swd.swdExecInst( qspi.instruction_readPageMMap      (), dBuf );
                        SysUtil.stdDbg().println("Reading Flash Page (Memory Mapped)");
                        final long tv1 = SysUtil.getNS();
                                        swd.swdRdBuff(0x90000000L + pgSz * rwPos, dBuf, dBuf.length, rdMaxSWDTransferSize);
                        final long tv2 = SysUtil.getNS();
                        SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n\n", pgSz, (tv2 - tv1) * 0.000000001, 1000000000.0 * pgSz / (tv2 - tv1) );
                                        swd.swdExecInst( qspi.instruction_endMMap           (), dBuf );
                        for(int idx = 0; idx < dBuf.length; ++idx) {
                            SysUtil.stdDbg().printf("%02X ", dBuf[idx]);
                            if( (idx + 1) % 32 == 0 ) SysUtil.stdDbg().println();
                        }
                        SysUtil.stdDbg().println();
                    }

                                        swd.swdExecInst( qspi.instruction_deinitAll         (), dBuf );
                }
                // ##### EXPERIMENT : STM32H750 with W25Q64FV #####
                // ##### EXPERIMENT : STM32H750 with W25Q64FV #####
                // ##### EXPERIMENT : STM32H750 with W25Q64FV #####
    //*/


    /*
                // ##### !!! TODO : Remove this later !!! #####

                final long[][] inst = new SWDExecInstBuilder() {{
                    mov(1,  XVI._0000);
                    mov(2,  XVI._0001);
                    mov(3,  XVI._0002);

                    push(XVI._0002);
                    push(XVI._0000, XVI._0001);
                    debugPrintlnUDecNN(XVI._0000);
                    debugPrintlnUDecNN(XVI._0001);
                    debugPrintlnUDecNN(XVI._0002);
                    debugPrintln();

                    mov(7,  XVI._0000);
                    mov(8,  XVI._0001);
                    mov(9,  XVI._0002);
                    debugPrintlnUDecNN(XVI._0000);
                    debugPrintlnUDecNN(XVI._0001);
                    debugPrintlnUDecNN(XVI._0002);
                    debugPrintln();

                    pop (XVI._0000, XVI._0001);
                    pop (XVI._0002);
                    debugPrintlnUDecNN(XVI._0000);
                    debugPrintlnUDecNN(XVI._0001);
                    debugPrintlnUDecNN(XVI._0002);
                    debugPrintln();

                }}.link();
                swd.swdExecInst(inst);
    //*/
