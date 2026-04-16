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
 *     nRF51 Series Reference Manual v3.0
 *     https://infocenter.nordicsemi.com/pdf/nRF51_RM_v3.0.pdf
 *
 *     nRF51822 Product Specification v3.1
 *     https://infocenter.nordicsemi.com/pdf/nRF51822_PS_v3.1.pdf
 *
 *     nRF52840 Product Specification v1.1
 *     https://infocenter.nordicsemi.com/pdf/nRF52840_PS_v1.1.pdf
 *
 *     nRF9160 Production Programming
 *     https://docs.nordicsemi.com/bundle/nan_041/page/APP/nan_production_programming/intro.html
 *
 *     nRF5340 Production Programming
 *     https://docs.nordicsemi.com/bundle/nan_042/page/APP/nan_production_programming/intro.html
 *
 *     nRF52832 Production Programming
 *     https://docs.nordicsemi.com/bundle/nwp_027/page/WP/nwp_027/intro.html
 *
 * ~~~ Last accessed & checked on 2024-06-01 ~~~
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * Please refer to the comment block before the 'ProgSWD' class definition in the 'ProgSWD.java' file for more details and information.
 */
public class SWDFlashLoaderNRF5 extends SWDFlashLoader {

    // ##### !!! TODO : It seems that if the transfer size is too large, the data read/written using SWD will be corrupted !!! #####
    protected static final int  _nrf5_wrMaxSWDTransferSize_NRF51 = 128; // Why using anything larger than 128 bytes would corrupt the TAR register???
    protected static final int  _nrf5_rdMaxSWDTransferSize_NRF51 = 512; // Why using anything larger than 512 bytes would corrupt the read data???

    // ##### !!! TODO : It seems that if the transfer size is too large, the data read/written using SWD will be corrupted !!! #####
    protected static final int  _nrf5_wrMaxSWDTransferSize_NRF52 = 128; // Why using anything larger than 128 bytes would corrupt the TAR register???
    protected static final int  _nrf5_rdMaxSWDTransferSize_NRF52 =   0; // Use the maximum transfer size

    protected static final XVI  _nrf5_xvi_TMP_0                  = XVI._0000;
    protected static final XVI  _nrf5_xvi_TMP_1                  = XVI._0001;
    protected static final XVI  _nrf5_xvi_TMP_2                  = XVI._0002;

    protected static final XVI  _nrf5_xvi_ERASEPAGE_begAddress   = _nrf5_xvi_TMP_0;
    protected static final XVI  _nrf5_xvi_ERASEPAGE_endAddress   = _nrf5_xvi_TMP_1;
    protected static final XVI  _nrf5_xvi_ERASEPAGE_pageSize     = _nrf5_xvi_TMP_2;

    protected static final XVI  _nrf5_xvi_ERASEALL_UICR_REGOUT0  = _nrf5_xvi_TMP_0;

    protected static final XVI  _nrf5_xvi_FLB_DoneRead           = XVI._0500;
    protected static final XVI  _nrf5_xvi_FLB_FDirty             = XVI._0501;
    protected static final XVI  _nrf5_xvi_FLB_LBDirty            = XVI._0502;

    protected static final long _nrf5_PROG_AREA_SIZE             = ProgSWD.DefaultCortexM_LoaderProgramSize; // NOTE : Ensure that this value is the same as the value used in the assembly program!

    protected static final long _nrf5_CODEPAGESIZE               = 0x10000010L;
    protected static final long _nrf5_CODENUMPAGES               = 0x10000014L;

    protected static final long _nrf5_WD_RR0                     = 0x40010600L;
    protected static final long _nrf5_WD_Reload                  = 0x6E524635L;

    protected static final long _nrf5_READY                      = 0x4001E400L;
    protected static final long _nrf5_CONFIG                     = 0x4001E504L;
    protected static final long _nrf5_ERASEPAGE                  = 0x4001E508L;
    protected static final long _nrf5_ERASEALL                   = 0x4001E50CL;
    protected static final long _nrf5_ERASEUICR                  = 0x4001E514L;

    protected static final long _nrf5_NRF51_FICR_CLENR0          = 0x10000028L;
    protected static final long _nrf5_NRF51_UICR_CLENR0          = 0x10001000L;

    protected static final long _nrf5_NRF52_UICR_REGOUT0         = 0x10001304L;

    protected static final long[][] _fl_nrf5_instruction_ClearAllFLBFlags = new SWDExecInstBuilder() {{
        // Clear all FLB flags
        mov( 0, _nrf5_xvi_FLB_DoneRead );
        mov( 0, _nrf5_xvi_FLB_FDirty   );
        mov( 0, _nrf5_xvi_FLB_LBDirty  );
    }}.link();

    protected static final long[][] _fl_nrf5_instruction_EraseFlashPages_impl = new SWDExecInstBuilder() {{
        label                   ( "efp_loop"                                                                                     );
            wrBus               ( _nrf5_CONFIG                  , 0x00000002L                                                    ); // Set   EEN
            rdBusLoopWhileCmpNEQ( _nrf5_READY                   , 0x00000001L                   , 0x00000001L                    ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
            wrBus               ( _nrf5_ERASEPAGE               , _nrf5_xvi_ERASEPAGE_begAddress                                 ); // Set   ERASEPAGE
            rdBusLoopWhileCmpNEQ( _nrf5_READY                   , 0x00000001L                   , 0x00000001L                    ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
            add                 ( _nrf5_xvi_ERASEPAGE_begAddress, _nrf5_xvi_ERASEPAGE_pageSize  , _nrf5_xvi_ERASEPAGE_begAddress );
        jmpIfLT                 ( _nrf5_xvi_ERASEPAGE_begAddress, _nrf5_xvi_ERASEPAGE_endAddress, "efp_loop"                     );
        wrBus                   ( _nrf5_CONFIG                  , 0x00000000L                                                    ); // Clear all
        rdBusLoopWhileCmpNEQ    ( _nrf5_READY                   , 0x00000001L                   , 0x00000001L                    ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
    }}.link();

    protected static final long[] _fl_nrf5x_flProgram;
    static {
        try                      { _fl_nrf5x_flProgram = SWDAsmARM_NRF5.flProgram_nrf5x(false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);  }
    }

    private static void _nrf5x_putInstRDS(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final long address, final long andBitmask, final XVI xviStoreIdx, final XVI xviTmp0)
    {
        ib.rdBusStr( address    , andBitmask, xviTmp0 );
        ib.strDB   ( xviTmp0    , xviStoreIdx         );
        ib.inc1    ( xviStoreIdx                      );
    }

    private static void _nrf5x_putInstWRI(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final long address, final XVI xviValue)
    {
        ib.wrBus      ( address    , xviValue                                                  );
        /*
        ib.debugPrintf( swdExecInst , "[??] [%08X] = %08X\n", address, xviValue );
        //*/
    }

    private static void _nrf5x_putInstWRI(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final long address, final long orBitmask, final XVI xviStoreIdx, final XVI xviTmp0)
    {
        ib.ldrDB      ( xviStoreIdx, xviTmp0                                                  );
        ib.bwOR       ( xviTmp0    , orBitmask, xviTmp0                                       );
        ib.wrBus      ( address    , xviTmp0                                                  );
        /*
        ib.debugPrintf( swdExecInst , "[%02d] [%08X] = %08X\n", xviStoreIdx, address, xviTmp0 );
        //*/
        ib.inc1       ( xviStoreIdx                                                           );
    }

    public static Specifier _getSpecifier_nrf5x_impl(final ProgSWD.Config config, final SWDExecInst swdExecInst, final int wrMaxSWDTransferSize, final int rdMaxSWDTransferSize, final boolean nrf52)
    {
        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Generate the instructions to erase the entire flash memory
        {
            if(nrf52) {
                // For now, do not perform mass erase on NRF52x
                //*
                final long flashMemBeg = config.memoryFlash.address;
                final long flashMemEnd = config.memoryFlash.address + config.memoryFlash.totalSize;
                final long flashPgSize = config.memoryFlash.pageSize;
              //SysUtil.stdDbg().printf("%08X %08X %08X\n", flashMemBeg, flashMemEnd, flashPgSize);
                ib.mov                ( flashMemBeg              , _nrf5_xvi_ERASEPAGE_begAddress                                   );
                ib.mov                ( flashMemEnd              , _nrf5_xvi_ERASEPAGE_endAddress                                   );
                ib.mov                ( flashPgSize              , _nrf5_xvi_ERASEPAGE_pageSize                                     );
                ib.appendPrelinkedInst( _fl_nrf5_instruction_EraseFlashPages_impl                                                   );
                //*/
                /*
                // ##### !!! TODO : Implement mass erase? !!! #####
                // Save UICR.REGOUT0
                ib.rdBusStr            ( _nrf5_NRF52_UICR_REGOUT0, 0xFFFFFFFFL                    , _nrf5_xvi_ERASEALL_UICR_REGOUT0 ); // Save  REGOUT0
                // Perform mass erase
                ib.wrBus               ( _nrf5_CONFIG            , 0x00000002L                                                      ); // Set   EEN
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _nrf5_ERASEALL          , 0x00000001L                                                      ); // Set   ERASEALL
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _nrf5_CONFIG            , 0x00000000L                                                      ); // Clear all
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
                // Restore UICR.REGOUT0
                ib.wrBus               ( _nrf5_CONFIG            , 0x00000001L                                                      ); // Set   WEN
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _nrf5_NRF52_UICR_REGOUT0, _nrf5_xvi_ERASEALL_UICR_REGOUT0                                  ); // Write REGOUT0
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _nrf5_CONFIG            , 0x00000000L                                                      ); // Clear all
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
                //*/
            }
            else {
                // Perform mass erase on NRF51x
                ib.wrBus               ( _nrf5_CONFIG            , 0x00000002L                                                      ); // Set   EEN
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _nrf5_ERASEALL          , 0x00000001L                                                      ); // Set   ERASEALL
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _nrf5_CONFIG            , 0x00000000L                                                      ); // Clear all
                ib.rdBusLoopWhileCmpNEQ( _nrf5_READY             , 0x00000001L                    , 0x00000001L                     ); // Wait  READY   // ##### !!! TODO : Use timeout !!! #####
            }
            /*
            ib.rdBusStr          ( _nrf5_CODEPAGESIZE      , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            ib.rdBusStr          ( _nrf5_CODENUMPAGES      , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            ib.rdBusStr          ( _nrf5_NRF52_UICR_REGOUT0, 0x00000007L, XVI._0255 );
            ib.debugPrintlnUDecNN( XVI._0255                                        );
            //*/
            /*
            ib.rdBusStr          ( _nrf5_NRF51_FICR_CLENR0 , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            ib.rdBusStr          ( _nrf5_NRF51_UICR_CLENR0 , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            ib.rdBusStr          ( 0x40000600L             , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI. _0255                                       );
            ib.rdBusStr          ( 0x40000604L             , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            ib.rdBusStr          ( 0x40000610L             , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            ib.rdBusStr          ( 0x40000614L             , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            //*/
        }

        final long[][] instruction_EraseFlash = ib.link();

        // Generate the instructions to erase only part of flash memory
        if( (config.memoryFlash.partEraseAddressBeg >= 0) && (config.memoryFlash.partEraseSize > 0) )
        {
            //*
            final long flashMemBeg = config.memoryFlash.partEraseAddressBeg;
            final long flashMemEnd = config.memoryFlash.partEraseAddressBeg + config.memoryFlash.partEraseSize;
            final long flashPgSize = config.memoryFlash.pageSize;
          //SysUtil.stdDbg().printf("%08X %08X %08X\n", flashMemBeg, flashMemEnd, flashPgSize);
            ib.mov                ( flashMemBeg, _nrf5_xvi_ERASEPAGE_begAddress );
            ib.mov                ( flashMemEnd, _nrf5_xvi_ERASEPAGE_endAddress );
            ib.mov                ( flashPgSize, _nrf5_xvi_ERASEPAGE_pageSize   );
            ib.appendPrelinkedInst( _fl_nrf5_instruction_EraseFlashPages_impl   );
            //*/
            /*
            ib.rdBusStr          ( _nrf5_CODEPAGESIZE      , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            ib.rdBusStr          ( _nrf5_CODENUMPAGES      , 0xFFFFFFFFL, XVI._0255 );
            ib.debugPrintlnUHex08( XVI._0255                                        );
            ib.rdBusStr          ( _nrf5_NRF52_UICR_REGOUT0, 0x00000007L, XVI._0255 );
            ib.debugPrintlnUDecNN( XVI._0255                                        );
            //*/
        }

        final long[][] instruction_EraseFlashPages = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */
        final long adr51_CLENR0         = 0x10001000L;
        final long adr51_RBPCONF        = 0x10001004L;
        final long adr51_XTALFREQ       = 0x10001008L;
        final long adr51_FWID           = 0x10001010L;
        final long adr51_BOOTLOADERADDR = 0x10001014L;

        final long adr52_PSELRESET0     = 0x10001200L;
        final long adr52_PSELRESET1     = 0x10001204L;
        final long adr52_APPROTECT      = 0x10001208L;
        final long adr52_NFCPINS        = 0x1000120CL;
        final long adr52_DEBUGCTRL      = 0x10001210L;
        final long adr52_REGOUT0        = _nrf5_NRF52_UICR_REGOUT0;

        final long adr5x_CUSTOMER00     = 0x10001080L;
        final int  cnt5x_CUSTOMERNN     = 32;

        // Generate the instructions to write the UICR
        {
                // For convenience
                final XVI xvi_Tmp0      = _nrf5_xvi_TMP_0;
                final XVI xvi_RBPCONF   = _nrf5_xvi_TMP_1; // NRF51x
                final XVI xvi_APPROTECT = _nrf5_xvi_TMP_1; // NRF52x
                final XVI xvi_StoreIdx  = _nrf5_xvi_TMP_2;
                // Simpy exit if the FLB data was not dirty
                ib.bwOR               ( _nrf5_xvi_FLB_FDirty  , _nrf5_xvi_FLB_LBDirty           , xvi_Tmp0               );
                ib.jmpIfZero          ( xvi_Tmp0              , "flb_not_dirty"                                          );
                //*
                // Erase UICR
                ib.wrtBits            ( _nrf5_CONFIG          , 0x00000002L                                              ); // Set   EEN
                ib.rdlBitsWhileUnset  ( _nrf5_READY           , 0x00000001L                                              ); // Wait  READY ##### !!! TODO : Use timeout !!! #####
                ib.wrtBits            ( _nrf5_ERASEUICR       , 0x00000001L                                              ); // Set   ERASEUICR
                ib.rdlBitsWhileUnset  ( _nrf5_READY           , 0x00000001L                                              ); // Wait  READY ##### !!! TODO : Use timeout !!! #####
                // Enable UICR programming
                ib.wrtBits            ( _nrf5_CONFIG          , 0x00000001L                                              ); // Set   WEN
                ib.rdlBitsWhileUnset  ( _nrf5_READY           , 0x00000001L                                              ); // Wait  READY ##### !!! TODO : Use timeout !!! #####
                //*/
                // Set the store index to zero
                ib.mov                ( 0                     , xvi_StoreIdx                                             );
            if(nrf52) {
                // NOTE    : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // WARNING : APPROTECT.PALL can only be erased by using the CTRL-AP!
                // ##### !!! TODO : Implement it later !!! #####
                // Generate APPROTECT from lock bits
                ib.mov                ( 0xFFFFFFFFL           , xvi_APPROTECT                                            ); // xvi_APPROTECT = 0b11111111111111111111111111111111
                ib.ldrDB              ( xvi_StoreIdx          , xvi_Tmp0                                                 ); // xvi_Tmp0      = 0b000000000000000000000000AAAAAAAA
                ib.jmpIfNotZero       ( xvi_Tmp0              , "flb_APPROTECT_not_zero"                                 );
                ib.mov                ( 0xFFFFFF00L           , xvi_APPROTECT                                            ); // xvi_APPROTECT = 0b11111111111111111111111100000000
            ib.label                  ( "flb_APPROTECT_not_zero"                                                         ); // !!! WARNING !!!
                ib.mov                ( 0xFFFFFFFFL           , xvi_APPROTECT                                            ); // NOTE : Disable changing PALL for now!
                ib.inc1               ( xvi_StoreIdx                                                                     );
                // Store the UICR
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr52_PSELRESET0        , 0x7FFFFFC0L  , xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr52_PSELRESET1        , 0x7FFFFFC0L  , xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr52_APPROTECT         , xvi_APPROTECT                         );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr52_NFCPINS           , 0xFFFFFFFEL  , xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr52_DEBUGCTRL         , 0xFFFF0000L  , xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr52_REGOUT0           , 0xFFFFFFF8L  , xvi_StoreIdx, xvi_Tmp0 );
            }
            else {
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Generate RBPCONF from lock bits
                ib.mov                ( 0xFFFF0000L           , xvi_RBPCONF                                              ); // xvi_RBPCONF = 0b11111111111111110000000000000000
                ib.ldrDB              ( xvi_StoreIdx          , xvi_Tmp0                                                 ); // xvi_Tmp0    = 0b000000000000000000000000000000BA
                ib.bwAND              ( xvi_Tmp0              , 0b0000000010                    , xvi_Tmp0               ); // xvi_Tmp0    = 0b000000000000000000000000000000B0
                ib.jmpIfZero          ( xvi_Tmp0              , "flb_RBPCONF_B_zero"                                     );
                ib.bwOR               ( xvi_RBPCONF           , 0b1111111100000000              , xvi_RBPCONF            ); // xvi_RBPCONF = 0b1111111111111111BBBBBBBB00000000
            ib.label                  ( "flb_RBPCONF_B_zero"                                                             );
                ib.ldrDB              ( xvi_StoreIdx          , xvi_Tmp0                                                 ); // xvi_Tmp0    = 0b000000000000000000000000000000BA
                ib.bwAND              ( xvi_Tmp0              , 0b0000000001                    , xvi_Tmp0               ); // xvi_Tmp0    = 0b0000000000000000000000000000000A
                ib.jmpIfZero          ( xvi_Tmp0              , "flb_RBPCONF_A_zero"                                     );
                ib.bwOR               ( xvi_RBPCONF           , 0b0000000011111111              , xvi_RBPCONF            ); // xvi_RBPCONF = 0b1111111111111111BBBBBBBBAAAAAAAA
            ib.label                  ( "flb_RBPCONF_A_zero"                                                             );
                ib.inc1               ( xvi_StoreIdx                                                                     );
                // Store the UICR
                // WARNING : On NRF51x, UICR can only be erased by ERASEALL (full chip erase)!
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr51_CLENR0            , 0x00000000L  , xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr51_RBPCONF           , xvi_RBPCONF                           );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr51_XTALFREQ          , 0xFFFFFF00L  , xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr51_FWID              , 0xFFFF0000L  , xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr51_BOOTLOADERADDR    , 0x00000000L  , xvi_StoreIdx, xvi_Tmp0 );
            }
            for(int i = 0; i < cnt5x_CUSTOMERNN; ++i) {
                _nrf5x_putInstWRI     ( ib, swdExecInst, adr5x_CUSTOMER00 + i * 4, 0x00000000L  , xvi_StoreIdx, xvi_Tmp0 );
            }
                ib.rdlBitsWhileUnset  ( _nrf5_READY           , 0x00000001L                                              ); // Wait  READY ##### !!! TODO : Use timeout !!! #####
                // Disable UICR programming
                ib.wrtBits            ( _nrf5_CONFIG          , 0x00000000L                                              ); // Clear all
                ib.rdlBitsWhileUnset  ( _nrf5_READY           , 0x00000001L                                              ); // Wait  READY ##### !!! TODO : Use timeout !!! #####
                // Clear all flags
            ib.label                  ( "flb_not_dirty"                                                                  );
                ib.appendPrelinkedInst( _fl_nrf5_instruction_ClearAllFLBFlags                                            );
        }

        final long[][] instruction_WriteFLB = ib.link();

        // Generate the instructions to read the UICR
        {
                // For convenience
                final XVI xvi_Tmp0     = _nrf5_xvi_TMP_0;
                final XVI xvi_Tmp1     = _nrf5_xvi_TMP_1;
                final XVI xvi_StoreIdx = _nrf5_xvi_TMP_2;
                // Simpy exit if the FLB data was already read
                ib.jmpIfNotZero  ( _nrf5_xvi_FLB_DoneRead, "flb_done_read"                                               );
                // Set the store index to zero
                ib.mov           ( 0                     , xvi_StoreIdx                                                  );
            if(nrf52) {
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Extract the APPROTECT as lock bits
                _nrf5x_putInstRDS( ib, swdExecInst       , adr52_APPROTECT         , 0x000000FFL, xvi_StoreIdx, xvi_Tmp0 );
                // Extract the others as fuses
                _nrf5x_putInstRDS( ib, swdExecInst       , adr52_PSELRESET0        , 0x8000003FL, xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstRDS( ib, swdExecInst       , adr52_PSELRESET1        , 0x8000003FL, xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstRDS( ib, swdExecInst       , adr52_NFCPINS           , 0x00000001L, xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstRDS( ib, swdExecInst       , adr52_DEBUGCTRL         , 0x0000FFFFL, xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstRDS( ib, swdExecInst       , adr52_REGOUT0           , 0x00000007L, xvi_StoreIdx, xvi_Tmp0 );
            }
            else {
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Extract the RBPCONF as lock bits
                ib.rdsBits       ( adr51_RBPCONF         , xvi_Tmp0                                                      ); // xvi_Tmp0 = 0b1111111111111111BBBBBBBBAAAAAAAA
                ib.bwAND         ( xvi_Tmp0              , 0x00000100L             , xvi_Tmp1                            ); // xvi_Tmp1 = 0b00000000000000000000000B00000000
                ib.bwRSH         ( xvi_Tmp1              , 7                       , xvi_Tmp1                            ); // xvi_Tmp1 = 0b000000000000000000000000000000B0
                ib.bwAND         ( xvi_Tmp0              , 0x00000001L             , xvi_Tmp0                            ); // xvi_Tmp0 = 0b0000000000000000000000000000000A
                ib.bwOR          ( xvi_Tmp0              , xvi_Tmp1                , xvi_Tmp0                            ); // xvi_Tmp0 = 0b000000000000000000000000000000BA
                ib.strDB         ( xvi_Tmp0              , xvi_StoreIdx                                                  );
                ib.inc1          ( xvi_StoreIdx                                                                          );
                // Extract the others as fuses
                _nrf5x_putInstRDS( ib, swdExecInst       , adr51_CLENR0            , 0xFFFFFFFFL, xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstRDS( ib, swdExecInst       , adr51_XTALFREQ          , 0x000000FFL, xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstRDS( ib, swdExecInst       , adr51_FWID              , 0x0000FFFFL, xvi_StoreIdx, xvi_Tmp0 );
                _nrf5x_putInstRDS( ib, swdExecInst       , adr51_BOOTLOADERADDR    , 0xFFFFFFFFL, xvi_StoreIdx, xvi_Tmp0 );
            }
            for(int i = 0; i < cnt5x_CUSTOMERNN; ++i) {
                _nrf5x_putInstRDS( ib, swdExecInst       , adr5x_CUSTOMER00 + i * 4, 0xFFFFFFFFL, xvi_StoreIdx, xvi_Tmp0 );
            }
                // Set flag
                ib.mov           ( 1                     , _nrf5_xvi_FLB_DoneRead                                        );
            ib.label             ( "flb_done_read"                                                                       );
        }

        final long[][] instruction_ReadFLB = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Instantiate the specifier
        final Specifier specifier = new Specifier(

            0xFF                                                                   , // flashMemoryEmptyValue
            wrMaxSWDTransferSize                                                   , // wrMaxSWDTransferSize
            rdMaxSWDTransferSize                                                   , // rdMaxSWDTransferSize
            false                                                                  , // supportDirectFlashRead
            false                                                                  , // supportDirectEEPROMRead

            _fl_nrf5_instruction_ClearAllFLBFlags                                  , // instruction_InitializeSystemOnce
            null                                                                   , // instruction_UninitializeSystemExit
            null                                                                   , // instruction_IsFlashLocked
            null                                                                   , // instruction_UnlockFlash
            instruction_EraseFlash                                                 , // instruction_EraseFlash
            instruction_EraseFlashPages                                            , // instruction_EraseFlashPages
            null                                                                   , // instruction_WriteFlash
            null                                                                   , // instruction_ReadFlash
            null                                                                   , // instruction_IsEEPROMLocked
            null                                                                   , // instruction_UnlockEEPROM
            null                                                                   , // instruction_WriteEEPROM
            null                                                                   , // instruction_ReadEEPROM
            XVI._NA_                                                               , // instruction_xviFlashEEPROMAddress
            XVI._NA_                                                               , // instruction_xviFlashEEPROMReadSize
            XVI._NA_                                                               , // instruction_xviSignalWorkerCommand
            XVI._NA_                                                               , // instruction_xviSignalJobState
            null                                                                   , // instruction_dataBuffFlash
            null                                                                   , // instruction_dataBuffEEPROM

            _fl_nrf5x_flProgram                                                    , // flProgram
            null                                                                   , // elProgram
            config.memorySRAM.address                                              , // addrProgStart
            config.memorySRAM.address + _nrf5_PROG_AREA_SIZE + 128                 , // addrProgBuffer
            config.memorySRAM.address + _nrf5_PROG_AREA_SIZE                       , // addrProgSignal
            new long[] { _nrf5_WD_RR0, _nrf5_WD_Reload, _nrf5_CONFIG, _nrf5_READY }, // wrProgExtraParams
            new long[] { _nrf5_WD_RR0, _nrf5_WD_Reload                            }, // rdProgExtraParams

            instruction_WriteFLB                                                   , // instruction_WriteFLB
            instruction_ReadFLB                                                    , // instruction_ReadFLB
            _nrf5_xvi_FLB_DoneRead                                                 , // instruction_xviFLB_DoneRead
            _nrf5_xvi_FLB_FDirty                                                   , // instruction_xviFLB_FDirty
            _nrf5_xvi_FLB_LBDirty                                                  , // instruction_xviFLB_LBDirty
            new int[1 + (nrf52 ? 5 : 4) + 32]                                        // instruction_dataBuffFLB
        );

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        /*
         * ##### !!! TODO : Implement chip unlock using CTRl-AP (add a custom command) !!! #####
         *
         * Read CTRL-AP:IDR and check if it is 0x02880000
         * Write 0 to CTRL-AP:ERASEALL
         * Write 1 to CTRL-AP:ERASEALL
         * Wait until CTRL-AP:ERASEALLSTATUS becomes 0
         * Write 1 to CTRL-AP:RESET
         * Write 0 to CTRL-AP:RESET
         * Write 0 to CTRL-AP:ERASEALL
         * Read CTRL-AP:APPROTECTSTATUS and check if it is disabled (0x00000001)
         */

        // Generate the instructions to read the UICR
        {
            /* ##### !!! TODO : COMPLETE AND IMPROVE THE TEST !!! #####
             * String[] paramName;
             * XVI   [] paramXVI;
             * int   [] dataBuffLB;
             */
            // ##### ##### ##### EXPERIMENT ##### ##### #####
            // For convenience
            final XVI xvi_Tmp0 = _nrf5_xvi_TMP_0;
            /*
             *  NRF52 CTRL-AP (AP #1)
             *
             *  RESET             0x00   Soft reset triggered through CTRL-AP
             *  ERASEALL          0x04   Erase all
             *  ERASEALLSTATUS    0x08   Status register for the ERASEALL operation
             *  APPROTECTSTATUS   0x0C   Status register for access port protection
             *  IDR               0xFC   CTRL-AP identification register, IDR
             */
            ib.resetUnhaltAllCores(                          );
            ib.debugPrintln       (                          );
            ib.rdRawMemAP         ( 1       , 0xFC, xvi_Tmp0 );
            ib.debugPrintlnUHex08 ( xvi_Tmp0                 ); // 02880000
            ib.rdRawMemAP         ( 1       , 0x0C, xvi_Tmp0 );
            ib.debugPrintlnUHex08 ( xvi_Tmp0                 ); // 00000001
            ib.wrRawMemAP         ( 1       , 0x00, 1        );
            ib.delayMS            ( 100                      );
            ib.wrRawMemAP         ( 1       , 0x00, 0        );
            ib.delayMS            ( 100                      );
            ib.debugPrintln       (                          );
            ib.exitRet            ( 0                        );
        }

        final long[][] instruction_nrf52_test = ib.link();

        // Add custom instructions
        specifier.addCustomInstruction( "nrf52_test", new Specifier.CustomInstruction(null, null, 0, instruction_nrf52_test) );

        // Return the specifier
        return specifier;
    }

    public static Specifier _getSpecifier_nrf51(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_nrf5x_impl(config, swdExecInst, _nrf5_wrMaxSWDTransferSize_NRF51, _nrf5_rdMaxSWDTransferSize_NRF51, false); }

    public static Specifier _getSpecifier_nrf52(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_nrf5x_impl(config, swdExecInst, _nrf5_wrMaxSWDTransferSize_NRF52, _nrf5_rdMaxSWDTransferSize_NRF52, true ); }

} // class SWDFlashLoaderNRF5
