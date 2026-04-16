/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.fl;


import jxm.*;
import jxm.annotation.*;
import jxm.ugc.*;
import jxm.xb.*;


/*
 * This class is written based on the algorithms and information found from:
 *
 *     PM0059
 *     STM32F205/215, STM32F207/217 Flash Memory Programming Manual
 *     https://www.st.com/resource/en/programming_manual/pm0059-stm32f205215-stm32f207217-flash-programming-manual-stmicroelectronics.pdf
 *
 *     PM0068
 *     STM32F10XXX XL-Density Flash Programming Manual
 *     https://www.st.com/resource/en/programming_manual/pm0068-stm32f10xxx-xldensity-flash-programming-stmicroelectronics.pdf
 *
 *     PM0075
 *     STM32F10xxx Flash Memory Programming Manual
 *     https://www.st.com/resource/en/programming_manual/pm0075-stm32f10xxx-flash-memory-microcontrollers-stmicroelectronics.pdf
 *
 *     RM0008
 *     STM32F101xx, STM32F102xx, STM32F103xx, STM32F105xx and STM32F107xx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/cd00171190-stm32f101xx-stm32f102xx-stm32f103xx-stm32f105xx-and-stm32f107xx-advanced-arm-based-32-bit-mcus-stmicroelectronics.pdf
 *
 *     RM0033
 *     STM32F205xx, STM32F207xx, STM32F215xx and STM32F217xx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0033-stm32f205xx-stm32f207xx-stm32f215xx-and-stm32f217xx-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0038
 *     STM32L100xx, STM32L151xx, STM32L152xx and STM32L162xx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0038-stm32l100xx-stm32l151xx-stm32l152xx-and-stm32l162xx-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0090
 *     STM32F405/415, STM32F407/417, STM32F427/437 and STM32F429/439 Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/dm00031020-stm32f405-415-stm32f407-417-stm32f427-437-and-stm32f429-439-advanced-arm-based-32-bit-mcus-stmicroelectronics.pdf
 *
 *     RM0316
 *     STM32F303xB/C/D/E, STM32F303x6/8, STM32F328x8, STM32F358xC, STM32F398xE Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0316-stm32f303xbcde-stm32f303x68-stm32f328x8-stm32f358xc-stm32f398xe-advanced-armbased-mcus-stmicroelectronics.pdf
 *
 *     RM0360
 *     STM32F030x4/x6/x8/xC and STM32F070x6/xB Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0360-stm32f030x4x6x8xc-and-stm32f070x6xb-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0367
 *     Ultra-Low-Power STM32L0x3 Advanced ARM(R)-Based 32-bit MCUs Reference manual
 *     https://www.st.com/resource/en/reference_manual/rm0367-ultralowpower-stm32l0x3-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0377
 *     Ultra-Low-Power STM32L0x1 Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0377-ultralowpower-stm32l0x1-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0383
 *     STM32F411xC/E Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0383-stm32f411xce-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0386
 *     STM32F469xx and STM32F479xx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0386-stm32f469xx-and-stm32f479xx-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0394
 *     STM32L41xxx/42xxx/43xxx/44xxx/45xxx/46xxx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0394-stm32l41xxx42xxx43xxx44xxx45xxx46xxx-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0399
 *     STM32H745/755 and STM32H747/757 Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0399-stm32h745755-and-stm32h747757-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0431
 *     STM32F72xxx and STM32F73xxx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0431-stm32f72xxx-and-stm32f73xxx-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0433
 *     STM32H742, STM32H743/753 and STM32H750 Value Line Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0433-stm32h742-stm32h743753-and-stm32h750-value-line-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0438
 *     STM32L552xx and STM32L562xx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/dm00346336-stm32l552xx-and-stm32l562xx-advanced-arm-based-32-bit-mcus-stmicroelectronics.pdf
 *
 *     RM0440
 *     STM32G4 Series Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0440-stm32g4-series-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0444
 *     STM32G0x1 Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0444-stm32g0x1-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0453
 *     STM32WL5x Advanced ARM(R)-Based 32-Bit MCUs Reference Manual (with Sub-GHz Radio Solution)
 *     https://www.st.com/resource/en/reference_manual/rm0453-stm32wl5x-advanced-armbased-32bit-mcus-with-subghz-radio-solution-stmicroelectronics.pdf
 *
 *     RM0454
 *     STM32G0x0 Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0454-stm32g0x0-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0455
 *     STM32H7A3/7B3 and STM32H7B0 Value Line Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0455-stm32h7a37b3-and-stm32h7b0-value-line-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0461
 *     STM32WLEx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual (with Sub-GHz Radio Solution)
 *     https://www.st.com/resource/en/reference_manual/rm0461-stm32wlex-advanced-armbased-32bit-mcus-with-subghz-radio-solution-stmicroelectronics.pdf
 *
 *     RM0468
 *     STM32H723/733, STM32H725/735 and STM32H730 Value Line Advanced ARM(R)-Based 32-Bit MCUs Reference Manual
 *     https://www.st.com/resource/en/reference_manual/rm0468-stm32h723733-stm32h725735-and-stm32h730-value-line-advanced-armbased-32bit-mcus-stmicroelectronics.pdf
 *
 *     RM0493
 *     STM32WBA5xxx Advanced ARM(R)-Based 32-Bit MCUs Reference Manual (with Multiprotocol Wireless Bluetooth(R) Low-Energy and IEEE802.15.4)
 *     https://www.st.com/resource/en/reference_manual/dm00821869.pdf
 *
 * ~~~ Last accessed & checked on 2024-07-31 ~~~
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * Please refer to the comment block before the 'ProgSWD' class definition in the 'ProgSWD.java' file for more details and information.
 */
public class SWDFlashLoaderSTM32 extends SWDFlashLoader {

    /* ##### !!! TODO !!! ####
     *    Review all '_getSpecifier_stm32*()' functions and check if their names need to be more specific
     *    to the MCU family names.
     */

    /*
     * NOTE : # For now, each flash bank for devices with multiple flash banks needs to be programmed
     *          separately (each using a different ProgSWD.Config).
     *        # For some devices, it is possible to erase all flash banks simultaneously (again, most
     *          likely using a different ProgSWD.Config than the one used to program the flash banks).
     *        # Please refer to the comments on each section for more information.
     */

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final XVI _fl_stm32fxx_xvi_FLB_DoneRead = XVI._0500;
    protected static final XVI _fl_stm32fxx_xvi_FLB_FDirty   = XVI._0501;
    protected static final XVI _fl_stm32fxx_xvi_FLB_LBDirty  = XVI._0502;

    protected static final long[][] _fl_stm32fxx_instruction_ClearAllFLBFlags = new SWDExecInstBuilder() {{
            // Clear all FLB flags
            mov( 0, _fl_stm32fxx_xvi_FLB_DoneRead );
            mov( 0, _fl_stm32fxx_xvi_FLB_FDirty   );
            mov( 0, _fl_stm32fxx_xvi_FLB_LBDirty  );
    }}.link();

    protected static final long[][] _fl_stm32fxx_instruction_EnsureRDP2NotActivated = new SWDExecInstBuilder() {{
            // Make sure that RDP LEVEL 2 is NEVER ACTIVATED!
            // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
            ldrDB   ( 0                 ,  XVI.transitory(0)                    );
            bwAND   (  XVI.transitory(0), 0xFF              , XVI.transitory(0) );
            jmpIfNEQ(  XVI.transitory(0), 0xCC              , "flb_not_RDP2"    );
            strDB   ( 0x00              , 0                                     );
        label       ( "flb_not_RDP2"                                            );
    }}.link();

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*            SRAM         Flash
     * STM32F0X   0x20000000   0x08000000
     * STM32F1X   0x20000000   0x08000000
     * STM32F3X   0x20000000   0x08000000
     * STM32XL    0x20000000   0x08000000 0x08080000
     *
     * NOTE : # For STM32XL devices, flash banks 0 and 1 must be erased     separately!
     *        # For STM32XL devices, flash banks 0 and 1 must be programmed separately!
     */

    protected static final long _stm32f1x_PROG_AREA_SIZE = ProgSWD.DefaultCortexM_LoaderProgramSize; // NOTE : Ensure that this value is the same as the value used in the assembly program!

    protected static final XVI  _fl_stm32f1x_xvi_TMP_0   = XVI._0000;
    protected static final XVI  _fl_stm32f1x_xvi_TMP_1   = XVI._0001;
    protected static final XVI  _fl_stm32f1x_xvi_TMP_2   = XVI._0002;
    protected static final XVI  _fl_stm32f1x_xvi_TMP_3   = XVI._0003;
    protected static final XVI  _fl_stm32f1x_xvi_TMP_4   = XVI._0004;
    protected static final XVI  _fl_stm32f1x_xvi_TMP_5   = XVI._0005;
    protected static final XVI  _fl_stm32f1x_xvi_TMP_6   = XVI._0006;
    protected static final XVI  _fl_stm32f1x_xvi_TMP_7   = XVI._0007;

    protected static final long _stm32f1x_flashKEYR      = 0x40022004L;
    protected static final long _stm32f1x_flashOPTKEYR   = 0x40022008L;
    protected static final long _stm32f1x_flashKey1      = 0x45670123L;
    protected static final long _stm32f1x_flashKey2      = 0xCDEF89ABL;

    protected static final long _stm32f1x_flashSR        = 0x4002200CL;
    protected static final long _stm32f1x_flashCR        = 0x40022010L;

    protected static final long _stm32f1x_flashBank1AOfs = 0x00000040L;

    protected static final long[] _fl_stm32f1x_flProgram;
    static {
        try                      { _fl_stm32f1x_flProgram = SWDAsmARM_STM32.flProgram_stm32f1x(false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);         }
    }

    private static void _stm32f1x_putInstXCV(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final XVI xviSrc, final long storeMask, final XVI xviStoreIdx, final XVI xviTmp0, final XVI xviTmp1, final XVI xviTmp2, final boolean hw)
    {
        final String lbl0xFF = ib.uniqueLabelCounter("_stm32f1x_xcv_oxff_%=");
        final String lblDone = ib.uniqueLabelCounter("_stm32f1x_xcv_done_%=");

             // Extract the value
             ib.bwAND      ( xviSrc     , hw ? 0x00FF0000L : 0x000000FFL, xviTmp0 );
             ib.bwRSH      ( xviTmp0    , hw ? 16          : 0          , xviTmp0 );

             // Extract the complemented value
             ib.bwAND      ( xviSrc     , hw ? 0xFF000000L : 0x0000FF00L, xviTmp1 );
             ib.bwRSH      ( xviTmp1    , hw ? 24          : 8          , xviTmp1 );

             // Check if both values are 0xFF
             ib.bwAND      ( xviTmp0    , xviTmp1                       , xviTmp2 );
             ib.jmpIfEQ    ( xviTmp2    ,      0x000000FFL              , lbl0xFF );

             // Invert the complemented value
             ib.bwXOR      ( xviTmp1    ,      0x000000FFL              , xviTmp1 );

             // Force the value to 'storeMask' if both values ​​are not the same (in accordance with the statement from the datasheet)
             ib.jmpIfNEQ   ( xviTmp0    , xviTmp1                       , lbl0xFF );

             // Store the value
             ib.bwAND      ( xviTmp0    , storeMask                     , xviTmp0 );
             ib.strDB      ( xviTmp0    , xviStoreIdx                             );
             ib.jmp        ( lblDone                                              );
        ib.label           ( lbl0xFF                                              );
             ib.strDB      ( storeMask  , xviStoreIdx                             );

             // Increment the store index
        ib.label           ( lblDone                                              );
             ib.inc1       ( xviStoreIdx                                          );
    }

    private static void _stm32f1x_putInstW16x2(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final long address, final XVI xviStoreIdx, final XVI xviTmp0, final XVI xviTmp1)
    {
        // NOTE : # Before the first call to this function, 'ib.wrBusSet16Bit()' must be executed first.
        //        # After  the last  call to this function, 'ib.wrBusSet32Bit()' must be executed immediately.

        // Load the 1st byte
        ib.ldrDB      ( xviStoreIdx, xviTmp0                                 );
        ib.inc1       ( xviStoreIdx                                          );

        // Load the 2nd byte
        ib.ldrDB      ( xviStoreIdx, xviTmp1                                 );
        ib.inc1       ( xviStoreIdx                                          );

        // Make sure the value is within the range
        ib.bwAND      ( xviTmp0    , 0xFF   , xviTmp0                        );
        ib.bwAND      ( xviTmp1    , 0xFF   , xviTmp1                        );

        // Combine the bytes
        ib.bwLSH      ( xviTmp1    , 16     , xviTmp1                        );
        ib.bwOR       ( xviTmp0    , xviTmp1, xviTmp1                        );
        /*
        ib.debugPrintf( swdExecInst, "[0x%08X] = 0x%08X\n", address, xviTmp1 );
        //*/

        // Write the bytes
        ib.wrBus16x2  ( address    , xviTmp1                                 );
    }

    public static Specifier _getSpecifier_stm32f1x_impl(final ProgSWD.Config config, final SWDExecInst swdExecInst, final boolean flashBank1, final long maskUSER)
    {
        // Determine the register addresses
        final long flashKEYR = _stm32f1x_flashKEYR + (flashBank1 ? _stm32f1x_flashBank1AOfs : 0);
        final long flashCR   = _stm32f1x_flashCR   + (flashBank1 ? _stm32f1x_flashBank1AOfs : 0);
        final long flashSR   = _stm32f1x_flashSR   + (flashBank1 ? _stm32f1x_flashBank1AOfs : 0);

        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Generate the instructions to check if the flash memory is locked
        {
            ib.rdBusRetCmpEQ       ( flashCR  , 0x00000080L        , 0x00000080L ); // Read  LOCK
        }

        final long[][] instructionIsFlashLocked = ib.link();

        // Generate the instructions to unlock the flash memory
        {
            ib.rdBusLoopWhileCmpNEQ( flashSR  , 0x00000001L        , 0x00000000L ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
            ib.wrBus               ( flashKEYR, _stm32f1x_flashKey1              ); // Write KEY1
            ib.wrBus               ( flashKEYR, _stm32f1x_flashKey2              ); // Write KEY2
            ib.rdBusLoopWhileCmpNEQ( flashSR  , 0x00000001L        , 0x00000000L ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
        }

        final long[][] instruction_UnlockFlash = ib.link();

        // Generate the instructions to erase the entire flash memory
        {
            ib.rdBusLoopWhileCmpNEQ( flashSR  , 0x00000001L        , 0x00000000L ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
            ib.wrBus               ( flashCR  , 0x00000004L                      ); // Write MER
            ib.wrBus               ( flashCR  , 0x00000044L                      ); // Write MER STRT
            ib.rdBusLoopWhileCmpNEQ( flashSR  , 0x00000001L        , 0x00000000L ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
        }

        final long[][] instruction_EraseFlash = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */
        final long adr_USER_RDP    = 0x1FFFF800L;
        final long adr_Data1_Data0 = 0x1FFFF804L;
        final long adr_WRP1_WRP0   = 0x1FFFF808L;
        final long adr_WRP3_WRP2   = 0x1FFFF80CL;

        // Generate the instructions to write the option bytes
        {
                // For convenience
                final XVI xvi_Tmp0     = _fl_stm32f1x_xvi_TMP_0;
                final XVI xvi_Tmp1     = _fl_stm32f1x_xvi_TMP_1;
                final XVI xvi_StoreIdx = _fl_stm32f1x_xvi_TMP_2;
                // Simpy exit if the FLB data was not dirty
                ib.bwOR                ( _fl_stm32fxx_xvi_FLB_FDirty     , _fl_stm32fxx_xvi_FLB_LBDirty, xvi_Tmp0           );
                ib.jmpIfZero           ( xvi_Tmp0                        , "flb_not_dirty"                                  );
                // Unlock option bytes
                ib.rdBusLoopWhileCmpNEQ( _stm32f1x_flashSR               , 0x00000001L                 , 0x00000000L        ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _stm32f1x_flashKEYR             , _stm32f1x_flashKey1                              ); // Write KEY1
                ib.wrBus               ( _stm32f1x_flashKEYR             , _stm32f1x_flashKey2                              ); // Write KEY2
                ib.rdBusLoopWhileCmpNEQ( _stm32f1x_flashSR               , 0x00000001L                 , 0x00000000L        ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _stm32f1x_flashOPTKEYR          , _stm32f1x_flashKey1                              ); // Write KEY1
                ib.wrBus               ( _stm32f1x_flashOPTKEYR          , _stm32f1x_flashKey2                              ); // Write KEY2
                ib.rdBusLoopWhileCmpNEQ( _stm32f1x_flashSR               , 0x00000001L                 , 0x00000000L        ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
                //*
                // Erase option bytes
                ib.wrBus               ( _stm32f1x_flashCR               , 0x00000220L                                      ); // Write OPTWRE      OPTER
                ib.wrBus               ( _stm32f1x_flashCR               , 0x00000260L                                      ); // Write OPTWRE STRT OPTER
                ib.rdBusLoopWhileCmpNEQ( _stm32f1x_flashSR               , 0x00000001L                 , 0x00000000L        ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
                //*/
                //*
                // Enable option bytes programming
                ib.wrBus               ( _stm32f1x_flashCR               , 0x00000210L                                      ); // Write OPTWRE      OPTPG
                ib.rdBusLoopWhileCmpNEQ( _stm32f1x_flashSR               , 0x00000001L                 , 0x00000000L        ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
                // Set the store index to zero
                ib.mov                 ( 0                               , xvi_StoreIdx                                     );
                // Write the option bytes
                ib.wrBusSet16Bit       (                                                                                    );
            if(!true) {
                // ----- The default values for STM32F103* -----
                ib.wrBus16x2           ( adr_USER_RDP                    , 0x00FF00A5L                                      );
                ib.wrBus16x2           ( adr_Data1_Data0                 , 0x00FF00FFL                                      );
                ib.wrBus16x2           ( adr_WRP1_WRP0                   , 0x00FF00FFL                                      );
                ib.wrBus16x2           ( adr_WRP3_WRP2                   , 0x00FF00FFL                                      );
            }
            else {
                // ----- Make sure that RDP LEVEL 2 is NEVER ACTIVATED! -----
                ib.appendPrelinkedInst ( _fl_stm32fxx_instruction_EnsureRDP2NotActivated                                    );
                // ----- Apply the mask to USER
                ib.ldrDB               ( 1                               , xvi_Tmp0                                         );
                ib.bwOR                ( xvi_Tmp0                        , (~maskUSER) & 0xFF          , xvi_Tmp0           );
                ib.strDB               ( xvi_Tmp0                        , 1                                                );
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // ----- Write the option bytes -----
                _stm32f1x_putInstW16x2 ( ib, swdExecInst, adr_USER_RDP   , xvi_StoreIdx                , xvi_Tmp0, xvi_Tmp1 );
                _stm32f1x_putInstW16x2 ( ib, swdExecInst, adr_Data1_Data0, xvi_StoreIdx                , xvi_Tmp0, xvi_Tmp1 );
                _stm32f1x_putInstW16x2 ( ib, swdExecInst, adr_WRP1_WRP0  , xvi_StoreIdx                , xvi_Tmp0, xvi_Tmp1 );
                _stm32f1x_putInstW16x2 ( ib, swdExecInst, adr_WRP3_WRP2  , xvi_StoreIdx                , xvi_Tmp0, xvi_Tmp1 );
            }
                //*/
                ib.wrBusSet32Bit       (                                                                                    );
                ib.rdBusLoopWhileCmpNEQ( _stm32f1x_flashSR               , 0x00000001L                 , 0x00000000L        ); // Wait  BSY      // ##### !!! TODO : Use timeout !!! #####
                // Clear all flags
            ib.label                   ( "flb_not_dirty"                                                                    );
                ib.appendPrelinkedInst ( _fl_stm32fxx_instruction_ClearAllFLBFlags                                          );
        }

        final long[][] instruction_WriteFLB = ib.link();

        // Generate the instructions to read the option bytes
        {
                // For convenience
                final XVI xvi_Tmp0        = _fl_stm32f1x_xvi_TMP_0;
                final XVI xvi_Tmp1        = _fl_stm32f1x_xvi_TMP_1;
                final XVI xvi_Tmp2        = _fl_stm32f1x_xvi_TMP_2;
                final XVI xvi_StoreIdx    = _fl_stm32f1x_xvi_TMP_3;
                final XVI xvi_USER_RDP    = _fl_stm32f1x_xvi_TMP_4;
                final XVI xvi_Data1_Data0 = _fl_stm32f1x_xvi_TMP_5;
                final XVI xvi_WRP1_WRP0   = _fl_stm32f1x_xvi_TMP_6;
                final XVI xvi_WRP3_WRP2   = _fl_stm32f1x_xvi_TMP_7;
                // Simpy exit if the FLB data was already read
                ib.jmpIfNotZero      ( _fl_stm32fxx_xvi_FLB_DoneRead   , "flb_done_read"                                                    );
                // Read the data
                ib.rdsBits           ( adr_USER_RDP                    , xvi_USER_RDP                                                       );
                ib.rdsBits           ( adr_Data1_Data0                 , xvi_Data1_Data0                                                    );
                ib.rdsBits           ( adr_WRP1_WRP0                   , xvi_WRP1_WRP0                                                      );
                ib.rdsBits           ( adr_WRP3_WRP2                   , xvi_WRP3_WRP2                                                      );
                /*
                ib.debugPrintlnUHex08( xvi_USER_RDP                                                                                         );
                ib.debugPrintlnUHex08( xvi_Data1_Data0                                                                                      );
                ib.debugPrintlnUHex08( xvi_WRP1_WRP0                                                                                        );
                ib.debugPrintlnUHex08( xvi_WRP3_WRP2                                                                                        );
                //*/
                // Set the store index to zero
                ib.mov               ( 0                               , xvi_StoreIdx                                                       );
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Extract the RDP as lock bits
                _stm32f1x_putInstXCV ( ib, swdExecInst, xvi_USER_RDP   , 0x000000FFL    , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2, false );
                // Extract the USER as fuse and apply the mask
                _stm32f1x_putInstXCV ( ib, swdExecInst, xvi_USER_RDP   , 0x000000FFL    , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2, true  );
                ib.ldrDB             ( xvi_StoreIdx                    , -1             , xvi_Tmp0                                          );
                ib.bwAND             ( xvi_Tmp0                        , maskUSER       , xvi_Tmp0                                          );
                ib.strDB             ( xvi_Tmp0                        , xvi_StoreIdx   , -1                                                );
                // Extract the Data0 as fuse
                _stm32f1x_putInstXCV ( ib, swdExecInst, xvi_Data1_Data0, 0x000000FFL    , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2, false );
                // Extract the Data1 as fuse
                _stm32f1x_putInstXCV ( ib, swdExecInst, xvi_Data1_Data0, 0x000000FFL    , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2, true  );
                // Extract the WRP0 as fuse
                _stm32f1x_putInstXCV ( ib, swdExecInst, xvi_WRP1_WRP0  , 0x000000FFL    , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2, false );
                // Extract the WRP1 as fuse
                _stm32f1x_putInstXCV ( ib, swdExecInst, xvi_WRP1_WRP0  , 0x000000FFL    , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2, true  );
                // Extract the WRP2 as fuse
                _stm32f1x_putInstXCV ( ib, swdExecInst, xvi_WRP3_WRP2  , 0x000000FFL    , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2, false );
                // Extract the WRP3 as fuse
                _stm32f1x_putInstXCV ( ib, swdExecInst, xvi_WRP3_WRP2  , 0x000000FFL    , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2, true  );
                // Set flag
                ib.mov               ( 1                               , _fl_stm32fxx_xvi_FLB_DoneRead                                      );
            ib.label                 ( "flb_done_read"                                                                                      );
        }

        final long[][] instruction_ReadFLB = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Instantiate and return the specifier
        return new Specifier(

            0xFF                                                                                , // flashMemoryEmptyValue
            0                                                                                   , // wrMaxSWDTransferSize
            0                                                                                   , // rdMaxSWDTransferSize
            false                                                                               , // supportDirectFlashRead
            false                                                                               , // supportDirectEEPROMRead

            _fl_stm32fxx_instruction_ClearAllFLBFlags                                           , // instruction_InitializeSystemOnce
            null                                                                                , // instruction_UninitializeSystemExit
            instructionIsFlashLocked                                                            , // instruction_IsFlashLocked
            instruction_UnlockFlash                                                             , // instruction_UnlockFlash
            instruction_EraseFlash                                                              , // instruction_EraseFlash
            null                                                                                , // instruction_EraseFlashPages
            null                                                                                , // instruction_WriteFlash
            null                                                                                , // instruction_ReadFlash
            null                                                                                , // instruction_IsEEPROMLocked
            null                                                                                , // instruction_UnlockEEPROM
            null                                                                                , // instruction_WriteEEPROM
            null                                                                                , // instruction_ReadEEPROM
            XVI._NA_                                                                            , // instruction_xviFlashEEPROMAddress
            XVI._NA_                                                                            , // instruction_xviFlashEEPROMReadSize
            XVI._NA_                                                                            , // instruction_xviSignalWorkerCommand
            XVI._NA_                                                                            , // instruction_xviSignalJobState
            null                                                                                , // instruction_dataBuffFlash
            null                                                                                , // instruction_dataBuffEEPROM

            _fl_stm32f1x_flProgram                                                              , // flProgram
            null                                                                                , // elProgram
            config.memorySRAM.address                                                           , // addrProgStart
            config.memorySRAM.address + _stm32f1x_PROG_AREA_SIZE + 128                          , // addrProgBuffer
            config.memorySRAM.address + _stm32f1x_PROG_AREA_SIZE                                , // addrProgSignal
            new long[] { flashKEYR, _stm32f1x_flashKey1, _stm32f1x_flashKey2, flashCR, flashSR }, // wrProgExtraParams
            null                                                                                , // rdProgExtraParams

            instruction_WriteFLB                                                                , // instruction_WriteFLB
            instruction_ReadFLB                                                                 , // instruction_ReadFLB
            _fl_stm32fxx_xvi_FLB_DoneRead                                                       , // instruction_xviFLB_DoneRead
            _fl_stm32fxx_xvi_FLB_FDirty                                                         , // instruction_xviFLB_FDirty
            _fl_stm32fxx_xvi_FLB_LBDirty                                                        , // instruction_xviFLB_LBDirty
            new int[1 + 7]                                                                        // instruction_dataBuffFLB

        );
    }

    public static Specifier _getSpecifier_stm32f0x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32f1x_impl(config, swdExecInst, false, 0b01110111); }

    public static Specifier _getSpecifier_stm32f1x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32f1x_impl(config, swdExecInst, false, 0b00000111); }

    public static Specifier _getSpecifier_stm32f3x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32f1x_impl(config, swdExecInst, false, 0b01110111); }

    // ##### !!! TODO : How to make it automatic between bank 0 and bank 1 (just like the 'atsam3x' driver)? !!! #####
    public static Specifier _getSpecifier_stm32xl_bank0(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32f1x_impl(config, swdExecInst, false, 0b00001111); }

    // ##### !!! TODO : How to make it automatic between bank 0 and bank 1 (just like the 'atsam3x' driver)? !!! #####
    public static Specifier _getSpecifier_stm32xl_bank1(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32f1x_impl(config, swdExecInst, true , 0b00001111); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*            SRAM         Flash        OTP Flash    External Flash
     * STM32F2X   0x20000000   0x08000000   0x1FFF7800
     * STM32F4X   0x20000000   0x08000000   0x1FFF7800   [<info exists QUADSPI> 0x90000000 ; <CR> 0xA0001000]
     * STM32F7X   0x20000000   0x08000000   0x1FF0F000
     *
     * NOTE : For these devices, it is possible to erase flash banks 0 and 1 simultaneously.
     *
     * ##### !!! TODO : How about the OTP flash                             !!! #####
     * ##### !!! TODO : How about the optional external flash for STM32F4X? !!! #####
     */

    protected static final long _stm32f2x_PROG_AREA_SIZE = ProgSWD.DefaultCortexM_LoaderProgramSize; // NOTE : Ensure that this value is the same as the value used in the assembly program!

    protected static final XVI  _fl_stm32f2x_xvi_TMP_0   = XVI._0000;
    protected static final XVI  _fl_stm32f2x_xvi_TMP_1   = XVI._0001;
    protected static final XVI  _fl_stm32f2x_xvi_TMP_2   = XVI._0002;
    protected static final XVI  _fl_stm32f2x_xvi_TMP_3   = XVI._0002;

    protected static final long _stm32f2x_flashKEYR      = 0x40023C04L;
    protected static final long _stm32f2x_flashKey1      = 0x45670123L;
    protected static final long _stm32f2x_flashKey2      = 0xCDEF89ABL;

    protected static final long _stm32f2x_flashOPTKEYR   = 0x40023C08L;
    protected static final long _stm32f2x_flashOptKey1   = 0x08192A3BL;
    protected static final long _stm32f2x_flashOptKey2   = 0x4C5D6E7FL;

    protected static final long _stm32f2x_flashSR        = 0x40023C0CL;
    protected static final long _stm32f2x_flashCR        = 0x40023C10L;

    protected static final long _stm32f2x_flashOPTCR     = 0x40023C14L;
    protected static final long _stm32f2x_flashOPTCR1    = 0x40023C18L; // STM32F4X and STM32F7X only
    protected static final long _stm32f2x_flashOPTCR2    = 0x40023C1CL; //              STM32F7X only

    protected static final long[][] _fl_stm32f2x_instruction_IsFlashLocked = new SWDExecInstBuilder() {{
        rdBusRetCmpEQ       ( _stm32f2x_flashCR  , 0x80000000L        , 0x80000000L ); // Read  LOCK
    }}.link();

    protected static final long[][] _fl_stm32f2x_instruction_UnlockFlash = new SWDExecInstBuilder() {{
        rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR  , 0x00010000L        , 0x00000000L ); // Wait  BSY           // ##### !!! TODO : Use timeout !!! #####
        wrBus               ( _stm32f2x_flashKEYR, _stm32f2x_flashKey1              ); // Write KEY1
        wrBus               ( _stm32f2x_flashKEYR, _stm32f2x_flashKey2              ); // Write KEY2
        rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR  , 0x00010000L        , 0x00000000L ); // Wait  BSY           // ##### !!! TODO : Use timeout !!! #####
    }}.link();

    protected static final long[][] _fl_stm32f2x_instruction_EraseFlash = new SWDExecInstBuilder() {{
        rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR  , 0x00010000L        , 0x00000000L ); // Wait  BSY           // ##### !!! TODO : Use timeout !!! #####
        wrBus               ( _stm32f2x_flashCR  , 0x00000004L                      ); // Write MER
        wrBus               ( _stm32f2x_flashCR  , 0x00010004L                      ); // Write MER STRT
        rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR  , 0x00010000L        , 0x00000000L ); // Wait  BSY           // ##### !!! TODO : Use timeout !!! #####
    }}.link();

    protected static final long[][] _fl_stm32f2x_instruction_EraseFlash_DB = new SWDExecInstBuilder() {{ // With a dual bank flash
        rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR  , 0x00010000L        , 0x00000000L ); // Wait  BSY           // ##### !!! TODO : Use timeout !!! #####
        wrBus               ( _stm32f2x_flashCR  , 0x00000804L                      ); // Write MER MER1
        wrBus               ( _stm32f2x_flashCR  , 0x00010804L                      ); // Write MER MER1 STRT
        rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR  , 0x00010000L        , 0x00000000L ); // Wait  BSY           // ##### !!! TODO : Use timeout !!! #####
    }}.link();

    protected static final long[] _fl_stm32f2x_flProgram;
    static {
        try                      { _fl_stm32f2x_flProgram = SWDAsmARM_STM32.flProgram_stm32f2x(false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);         }
    }

    public static Specifier _getSpecifier_stm32f2x_impl(final ProgSWD.Config config, final SWDExecInst swdExecInst, final long maskOPTCR, final long maskOPTCR1, final long maskOPTCR2)
    {
        // Determine the number of fuse(s)
        int fuseCnt = 1;
        if(maskOPTCR1 != -1) ++fuseCnt;
        if(maskOPTCR2 != -1) ++fuseCnt;

        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */

        // Generate the instructions to write the option bytes
        {
                // For convenience
                final XVI xvi_Tmp0   = _fl_stm32f2x_xvi_TMP_0;
                final XVI xvi_OPTCR  = _fl_stm32f2x_xvi_TMP_1;
                final XVI xvi_OPTCR1 = _fl_stm32f2x_xvi_TMP_2;
                final XVI xvi_OPTCR2 = _fl_stm32f2x_xvi_TMP_3;
                // Simpy exit if the FLB data was not dirty
                ib.bwOR                ( _fl_stm32fxx_xvi_FLB_FDirty, _fl_stm32fxx_xvi_FLB_LBDirty, xvi_Tmp0    );
                ib.jmpIfZero           ( xvi_Tmp0                   , "flb_not_dirty"                           );
                // ----- Make sure that RDP LEVEL 2 is NEVER ACTIVATED! -----
                ib.appendPrelinkedInst ( _fl_stm32fxx_instruction_EnsureRDP2NotActivated                        );
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Load OPTCR, OPTCR1, and OPTCR2 from fuses
                ib.ldrDB               ( 1                         , xvi_OPTCR                                  );
                ib.bwAND               ( xvi_OPTCR                 , maskOPTCR                    , xvi_OPTCR   );
            if(maskOPTCR1 != -1) {
                ib.ldrDB               ( 2                         , xvi_OPTCR1                                 );
                ib.bwAND               ( xvi_OPTCR1                , maskOPTCR1                   , xvi_OPTCR1  );
            }
            if(maskOPTCR2 != -1) {
                ib.ldrDB               ( 3                         , xvi_OPTCR2                                 );
                ib.bwAND               ( xvi_OPTCR2                , maskOPTCR2                   , xvi_OPTCR2  );
            }
                // Load RDP from lock bits and combine it to OPTCR
                ib.bwAND               ( xvi_OPTCR                 , 0xFFFF00FFL                  , xvi_OPTCR   ); // Clear RDP from the OPTCR
                ib.ldrDB               ( 0                         , xvi_Tmp0                                   );
                ib.bwLSH               ( xvi_Tmp0                  , 8                            , xvi_Tmp0    );
                ib.bwOR                ( xvi_Tmp0                  , xvi_OPTCR                    , xvi_OPTCR   );
                /*
                ib.debugPrintlnUHex08  ( xvi_OPTCR                                                              );
                ib.debugPrintlnUHex08  ( xvi_OPTCR1                                                             );
                ib.debugPrintlnUHex08  ( xvi_OPTCR2                                                             );
                //*/
                // Unlock option bytes
                //*
                ib.rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR          , 0x00010000L                 , 0x00000000L ); // Wait  BSY       // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( _stm32f2x_flashOPTKEYR     , _stm32f2x_flashOptKey1                    ); // Write KEY1
                ib.wrBus               ( _stm32f2x_flashOPTKEYR     , _stm32f2x_flashOptKey2                    ); // Write KEY2
                ib.rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR          , 0x00010000L                 , 0x00000000L ); // Wait  BSY       // ##### !!! TODO : Use timeout !!! #####
                ib.rdBusErrIfCmpNEQ    ( _stm32f2x_flashOPTCR       , 0x00000001L                 , 0x00000000L ); // Check OPTLOCK
                // Write the option bytes
            if(maskOPTCR2 != -1) {
                ib.wrtBits             ( _stm32f2x_flashOPTCR2      , xvi_OPTCR2                                );
            }
            if(maskOPTCR1 != -1) {
                ib.wrtBits             ( _stm32f2x_flashOPTCR1      , xvi_OPTCR1                                );
            }
                ib.wrtBits             ( _stm32f2x_flashOPTCR       , xvi_OPTCR                                 );
                ib.bwOR                ( xvi_OPTCR                  , 0x00000002L                 , xvi_Tmp0    ); // Set OPTSTRT
                ib.wrtBits             ( _stm32f2x_flashOPTCR       , xvi_Tmp0                                  );
                ib.rdBusLoopWhileCmpNEQ( _stm32f2x_flashSR          , 0x00010000L                 , 0x00000000L ); // Wait  BSY       // ##### !!! TODO : Use timeout !!! #####
                //*/
                // Clear all flags
            ib.label                   ( "flb_not_dirty"                                                        );
                ib.appendPrelinkedInst ( _fl_stm32fxx_instruction_ClearAllFLBFlags                              );
        }

        final long[][] instruction_WriteFLB = ib.link();

        // Generate the instructions to read the option bytes
        {
                // For convenience
                final XVI xvi_Tmp0   = _fl_stm32f2x_xvi_TMP_0;
                final XVI xvi_OPTCR  = _fl_stm32f2x_xvi_TMP_1;
                final XVI xvi_OPTCR1 = _fl_stm32f2x_xvi_TMP_2;
                final XVI xvi_OPTCR2 = _fl_stm32f2x_xvi_TMP_3;
                // Simpy exit if the FLB data was already read
                ib.jmpIfNotZero      ( _fl_stm32fxx_xvi_FLB_DoneRead, "flb_done_read"               );
                // Read the data
                ib.rdsBits           ( _stm32f2x_flashOPTCR         , xvi_OPTCR                     );
                ib.bwAND             ( xvi_OPTCR                    , maskOPTCR  , xvi_OPTCR        );
            if(maskOPTCR1 != -1) {
                ib.rdsBits           ( _stm32f2x_flashOPTCR1        , xvi_OPTCR1                    );
                ib.bwAND             ( xvi_OPTCR1                   , maskOPTCR1 , xvi_OPTCR1       );
            }
            if(maskOPTCR2 != -1) {
                ib.rdsBits           ( _stm32f2x_flashOPTCR2        , xvi_OPTCR2                    );
                ib.bwAND             ( xvi_OPTCR2                   , maskOPTCR2 , xvi_OPTCR2       );
            }
                /*
                ib.debugPrintlnUHex08( xvi_OPTCR                                                    );
                ib.debugPrintlnUHex08( xvi_OPTCR1                                                   );
                ib.debugPrintlnUHex08( xvi_OPTCR2                                                   );
                //*/
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Store the RDP as lock bits
                ib.bwAND             ( xvi_OPTCR                    , 0x0000FF00L, xvi_Tmp0         );
                ib.bwRSH             ( xvi_Tmp0                     , 8          , xvi_Tmp0         );
                ib.strDB             ( xvi_Tmp0                     , 0                             );
                // Remove RDP, OPTSTRT, and OPTLOCK from OPTCR
                ib.bwAND             ( xvi_OPTCR                    , 0xFFFF00FCL, xvi_OPTCR        );
                // Store OPTCR, OPTCR1, and OPTCR2 as fuses
                ib.strDB             ( xvi_OPTCR                    , 1                             );
            if(maskOPTCR1 != -1) {
                ib.strDB             ( xvi_OPTCR1                   , 2                             );
            }
            if(maskOPTCR2 != -1) {
                ib.strDB             ( xvi_OPTCR2                   , 3                             );
            }
                // Set flag
                ib.mov               ( 1                            , _fl_stm32fxx_xvi_FLB_DoneRead );
            ib.label                 ( "flb_done_read"                                              );
        }

        final long[][] instruction_ReadFLB = ib.link();

        // Instantiate and return the specifier
        final boolean dualBank = config.memoryFlash.dualBank;

        return new Specifier(

            0xFF                                                                                                              , // flashMemoryEmptyValue
            0                                                                                                                 , // wrMaxSWDTransferSize
            0                                                                                                                 , // rdMaxSWDTransferSize
            false                                                                                                             , // supportDirectFlashRead
            false                                                                                                             , // supportDirectEEPROMRead

                       _fl_stm32fxx_instruction_ClearAllFLBFlags                                                              , // instruction_InitializeSystemOnce
                       null                                                                                                   , // instruction_UninitializeSystemExit
                       _fl_stm32f2x_instruction_IsFlashLocked                                                                 , // instruction_IsFlashLocked
                       _fl_stm32f2x_instruction_UnlockFlash                                                                   , // instruction_UnlockFlash
            dualBank ? _fl_stm32f2x_instruction_EraseFlash_DB : _fl_stm32f2x_instruction_EraseFlash                           , // instruction_EraseFlash
                       null                                                                                                   , // instruction_EraseFlashPages
                       null                                                                                                   , // instruction_WriteFlash
                       null                                                                                                   , // instruction_ReadFlash
                       null                                                                                                   , // instruction_IsEEPROMLocked
                       null                                                                                                   , // instruction_UnlockEEPROM
                       null                                                                                                   , // instruction_WriteEEPROM
                       null                                                                                                   , // instruction_ReadEEPROM
                       XVI._NA_                                                                                               , // instruction_xviFlashEEPROMAddress
                       XVI._NA_                                                                                               , // instruction_xviFlashEEPROMReadSize
                       XVI._NA_                                                                                               , // instruction_xviSignalWorkerCommand
                       XVI._NA_                                                                                               , // instruction_xviSignalJobState
                       null                                                                                                   , // instruction_dataBuffFlash
                       null                                                                                                   , // instruction_dataBuffEEPROM

            _fl_stm32f2x_flProgram                                                                                            , // flProgram
            null                                                                                                              , // elProgram
            config.memorySRAM.address                                                                                         , // addrProgStart
            config.memorySRAM.address + _stm32f2x_PROG_AREA_SIZE + 128                                                        , // addrProgBuffer
            config.memorySRAM.address + _stm32f2x_PROG_AREA_SIZE                                                              , // addrProgSignal
            new long[] { _stm32f2x_flashKEYR, _stm32f2x_flashKey1, _stm32f2x_flashKey2, _stm32f2x_flashCR, _stm32f2x_flashSR }, // wrProgExtraParams
            null                                                                                                              , // rdProgExtraParams

            instruction_WriteFLB                                                                                              , // instruction_WriteFLB
            instruction_ReadFLB                                                                                               , // instruction_ReadFLB
            _fl_stm32fxx_xvi_FLB_DoneRead                                                                                     , // instruction_xviFLB_DoneRead
            _fl_stm32fxx_xvi_FLB_FDirty                                                                                       , // instruction_xviFLB_FDirty
            _fl_stm32fxx_xvi_FLB_LBDirty                                                                                      , // instruction_xviFLB_LBDirty
            new int[1 + fuseCnt]                                                                                                // instruction_dataBuffFLB

       );
    }

    public static Specifier _getSpecifier_stm32f2x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32f2x_impl(config, swdExecInst, 0b00001111111111111111111111101100L, -1, -1); }

    public static Specifier _getSpecifier_stm32f4x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        final boolean dualBank = swdExecInst._progSWD().stm32DeviceID() == 0x419; /* STM32F4[2|3]x */

        return _getSpecifier_stm32f2x_impl(
            config                                                                              ,
            swdExecInst                                                                         ,
            dualBank ? 0b11001111111111111111111111111100L : 0b00001111111111111111111111101100L,
            dualBank ? 0b00001111111111110000000000000000L : -1                                 ,
            -1
        );
    }

    public static Specifier _getSpecifier_stm32f6x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32f2x_impl(config, swdExecInst, 0b11001111111111111111111111111100L, 0b00001111111111110000000000000000L, -1); }

    public static Specifier _getSpecifier_stm32f7x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32f2x_impl(config, swdExecInst, 0b11000000111111111111111111111111L, 0b11111111111111111111111111111111L, 0b10000000000000000000000011111111L); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*            SRAM         Flash                                         EEPROM
     * STM32L0X   0x20000000   0x08000000 [<category 5 device> 0x08018000]   0x08080000
     * STM32L1X   0x20000000   0x08000000 [<category 5 device> 0x08018000]   0x08080000
     *
     * NOTE : For these devices, flash write operations must be done in half-pages.
     */

    protected static final XVI  _stm32lx_xvi_TMP_0        = XVI._0000;
    protected static final XVI  _stm32lx_xvi_TMP_1        = XVI._0001;
    protected static final XVI  _stm32lx_xvi_TMP_2        = XVI._0002;
    protected static final XVI  _stm32lx_xvi_TMP_3        = XVI._0003;
    protected static final XVI  _stm32lx_xvi_TMP_4        = XVI._0004;
    protected static final XVI  _stm32lx_xvi_TMP_5        = XVI._0005;
    protected static final XVI  _stm32lx_xvi_TMP_6        = XVI._0006;
    protected static final XVI  _stm32lx_xvi_TMP_7        = XVI._0007;
    protected static final XVI  _stm32lx_xvi_TMP_8        = XVI._0008;
    protected static final XVI  _stm32lx_xvi_TMP_9        = XVI._0009;
    protected static final XVI  _stm32lx_xvi_TMP_10       = XVI._0010;
    protected static final XVI  _stm32lx_xvi_TMP_11       = XVI._0011;
    protected static final XVI  _stm32lx_xvi_TMP_12       = XVI._0012;
    protected static final XVI  _stm32lx_xvi_TMP_13       = XVI._0013;

    protected static final XVI  _stm32lx_xvi_WRP2_L_NZ    = XVI._0100;
    protected static final XVI  _stm32lx_xvi_WRP2_H_NZ    = XVI._0101;
    protected static final XVI  _stm32lx_xvi_WRP3_L_NZ    = XVI._0102;
    protected static final XVI  _stm32lx_xvi_WRP3_H_NZ    = XVI._0103;
    protected static final XVI  _stm32lx_xvi_WRP4_L_NZ    = XVI._0104;
    protected static final XVI  _stm32lx_xvi_WRP4_H_NZ    = XVI._0105;

    protected static final long _stm32lx_PROG_AREA_SIZE   = ProgSWD.DefaultCortexM_LoaderProgramSize; // NOTE : Ensure that this value is the same as the value used in the assembly program!

    protected static final long _stm32l0x_flashBASE       = 0x40022000L;
    protected static final long _stm32l1x_flashBASE       = 0x40023C00L;

    protected static final long _stm32lx_flashPEKEYR_ofs  = 0x0C;
    protected static final long _stm32lx_flashPEKey1      = 0x89ABCDEFL;
    protected static final long _stm32lx_flashPEKey2      = 0x02030405L;

    protected static final long _stm32lx_flashPRGKEYR_ofs = 0x10;
    protected static final long _stm32lx_flashPRGKey1     = 0x8C9DAEBFL;
    protected static final long _stm32lx_flashPRGKey2     = 0x13141516L;

    protected static final long _stm32lx_flashOPTKEYR_ofs = 0x14;
    protected static final long _stm32lx_flashOPTKey1     = 0xFBEAD9C8L;
    protected static final long _stm32lx_flashOPTKey2     = 0x24252627L;

    protected static final long _stm32lx_flashSR_ofs      = 0x18;
    protected static final long _stm32lx_flashPECR_ofs    = 0x04;

    protected static final long[] _fl_stm32l0x_flProgram;
    static {
        try                      { _fl_stm32l0x_flProgram = SWDAsmARM_STM32.flProgram_stm32lx (false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);         }
    }

    protected static final long[] _fl_stm32l0x_elProgram;
    static {
        try                      { _fl_stm32l0x_elProgram = SWDAsmARM_STM32.elProgram_stm32l0x(false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);         }
    }

    protected static final long[] _fl_stm32l1x_elProgram;
    static {
        try                      { _fl_stm32l1x_elProgram = SWDAsmARM_STM32.elProgram_stm32l1x(false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);         }
    }

    private static void _stm32flx_putInstXCV(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final XVI xviSrc, final long storeMask, final XVI xviStoreIdx, final XVI xviTmp0, final XVI xviTmp1, final XVI xviTmp2)
    {
        final String lbl0xFF = ib.uniqueLabelCounter("_stm32flx_xcv_oxff_%=");
        final String lblDone = ib.uniqueLabelCounter("_stm32flx_xcv_done_%=");

             // Extract the value
             ib.bwAND      ( xviSrc     , 0x0000FFFFL, xviTmp0 );

             // Extract the complemented value
             ib.bwAND      ( xviSrc     , 0xFFFF0000L, xviTmp1 );
             ib.bwRSH      ( xviTmp1    , 16         , xviTmp1 );

             // Check if both values are 0xFFFF
             ib.bwAND      ( xviTmp0    , xviTmp1    , xviTmp2 );
             ib.jmpIfEQ    ( xviTmp2    , 0x0000FFFFL, lbl0xFF );

             // Invert the complemented value
             ib.bwXOR      ( xviTmp1    , 0x0000FFFFL, xviTmp1 );

             // Force the value to 'storeMask' if both values ​​are not the same (in accordance with the statement from the datasheet)
             ib.jmpIfNEQ   ( xviTmp0    , xviTmp1    , lbl0xFF );

             // Store the value
             ib.bwAND      ( xviTmp0    , storeMask  , xviTmp0 );
             ib.strDB      ( xviTmp0    , xviStoreIdx          );
             ib.jmp        ( lblDone                           );
        ib.label           ( lbl0xFF                           );
             ib.strDB      ( storeMask  , xviStoreIdx          );

             // Increment the store index
        ib.label           ( lblDone                           );
             ib.inc1       ( xviStoreIdx                       );
    }

    private static void _stm32flx_putInstGCV(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final XVI xviDst, final XVI xviStoreIdx, final XVI xviTmp0, final XVI xviTmp1)
    {
        if(xviStoreIdx != XVI._NA_) {
            // Load the value
            ib.ldrDB( xviStoreIdx             , xviTmp0 );
            ib.bwAND( xviTmp0    , 0x0000FFFFL, xviTmp0 );
            // Increment the store index
            ib.inc1 ( xviStoreIdx                       );
        }
        else {
            // Copy the value
            ib.mov  ( xviDst                  , xviTmp0 );
        }

            // Generate the complemented value
            ib.bwLSH( xviTmp0    , 16         , xviTmp1 );
            ib.bwNOT( xviTmp1                 , xviTmp1 );
            ib.bwAND( xviTmp1    , 0xFFFF0000L, xviTmp1 );

            // Combine and store the values
            ib.bwOR ( xviTmp0    , xviTmp1    , xviDst  );
    }

    private static void _stm32flx_putInstWCV(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final long flashBASE, final XVI xviChkNZ, final long dstAddress, final XVI xviValue)
    {
        final String lblSkip = ib.uniqueLabelCounter("_stm32flx_wcv_skip_%=");

        if(xviChkNZ != XVI._NA_) {
            // Simply exit if the option byte is not available
            ib.jmpIfZero           ( xviChkNZ                        , lblSkip                  );
        }

            // Write the value
            ib.wrtBits             ( dstAddress                      , xviValue                 );
            ib.rdBusLoopWhileCmpNEQ( flashBASE + _stm32lx_flashSR_ofs, 0x00000001L, 0x00000000L ); // Wait BSY   // ##### !!! TODO : Use timeout !!! #####

        ib.label                   ( lblSkip                                                    );
    }

    public static Specifier _getSpecifier_stm32lx_impl(final long flashBASE, final ProgSWD.Config config, final SWDExecInst swdExecInst, final boolean l1x)
    {
        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Generate the instructions to check if the flash memory is locked
        {
                ib.rdBusRetCmpEQ       ( flashBASE + _stm32lx_flashPECR_ofs   , 0x00000002L          , 0x00000002L       ); // Read  PRG_LOCK
        }

        final long[][] instructionIsFlashLocked = ib.link();

        // Generate the instructions to unlock the flash memory
        {
                // Check if the flash memory is already unlocked
                ib.rdBusStr            ( flashBASE + _stm32lx_flashPECR_ofs   , 0x00000002L          ,_stm32lx_xvi_TMP_0 ); // Read  PRG_LOCK
                ib.jmpIfEQ             ( _stm32lx_xvi_TMP_0                   , 0x00000000L          , "uf_unlocked"     ); // Check PRG_LOCK
                // Unlock FLASH_PECR
                ib.wrBus               ( flashBASE + _stm32lx_flashPEKEYR_ofs , _stm32lx_flashPEKey1                     ); // Write PEKEY1
                ib.wrBus               ( flashBASE + _stm32lx_flashPEKEYR_ofs , _stm32lx_flashPEKey2                     ); // Write PEKEY2
                ib.rdBusErrIfCmpEQ     ( flashBASE + _stm32lx_flashPECR_ofs   , 0x00000001L          , 0x00000001L       ); // Check PE_LOCK
                // Unlock the flash memory
                ib.wrBus               ( flashBASE + _stm32lx_flashPRGKEYR_ofs, _stm32lx_flashPRGKey1                    ); // Write PRGKEY1
                ib.wrBus               ( flashBASE + _stm32lx_flashPRGKEYR_ofs, _stm32lx_flashPRGKey2                    ); // Write PRGKEY2
                ib.rdBusErrIfCmpEQ     ( flashBASE + _stm32lx_flashPECR_ofs   , 0x00000002L          , 0x00000002L       ); // Check PRG_LOCK
            ib.label                   ( "uf_unlocked"                                                                   );
        }

        final long[][] instruction_UnlockFlash = ib.link();

        // Generate the instructions to erase the entire flash memory
        {
            // ##### !!! TODO : VERIFY !!! #####
            final long flashMemEnd = config.memoryFlash.address + config.memoryFlash.totalSize;
            for(long pageAddress = config.memoryFlash.address; pageAddress < flashMemEnd; pageAddress += config.memoryFlash.pageSize) {
                ib.wrBus               ( flashBASE + _stm32lx_flashPECR_ofs   , 0x00000208L                              ); // Write ERASE and PROG
                ib.rdBusLoopWhileCmpNEQ( flashBASE + _stm32lx_flashSR_ofs     , 0x00000001L          , 0x00000000L       ); // Wait  BSY            // ##### !!! TODO : Use timeout !!! #####
                ib.wrBus               ( pageAddress                          , 0                                        ); // Write dummy data
                ib.rdBusLoopWhileCmpNEQ( flashBASE + _stm32lx_flashSR_ofs     , 0x00000001L          , 0x00000000L       ); // Wait  BSY            // ##### !!! TODO : Use timeout !!! #####
            }
            // ##### ??? TODO : Unset ERASE and PROG after erasing ??? #####
        }

        final long[][] instruction_EraseAllFlashPages = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */

        final long adrl0_OPTR_L     = 0x1FF80000L;
        final long adrl0_OPTR_H     = 0x1FF80004L;
        final long adrl0_WRPROT1_L  = 0x1FF80008L;
        final long adrl0_WRPROT1_H  = 0x1FF8000CL;
        final long adrl0_WRPROT2_L  = 0x1FF80010L;

        final long adrl1_SPRMOD_RDP = 0x1FF80000L;
        final long adrl1_0X00_USER  = 0x1FF80004L;
        final long adrl1_WRP1_L     = 0x1FF80008L;
        final long adrl1_WRP1_H     = 0x1FF8000CL;
        final long adrl1_WRP2_L     = 0x1FF80010L;
        final long adrl1_WRP2_H     = 0x1FF80014L;
        final long adrl1_WRP3_L     = 0x1FF80018L;
        final long adrl1_WRP3_H     = 0x1FF8001CL;
        final long adrl1_WRP4_L     = 0x1FF80080L;
        final long adrl1_WRP4_H     = 0x1FF80084L;

        // Generate the instructions to write the option bytes
        {
                // For convenience
                final XVI xvi_Tmp0         = _stm32lx_xvi_TMP_0;
                final XVI xvi_Tmp1         = _stm32lx_xvi_TMP_1;
                final XVI xvi_StoreIdx     = _stm32lx_xvi_TMP_2;
                final XVI xvi_0_OPTR_L     = _stm32lx_xvi_TMP_3;  // STM32L0x
                final XVI xvi_0_OPTR_H     = _stm32lx_xvi_TMP_4;  // ---
                final XVI xvi_0_WRPROT1_L  = _stm32lx_xvi_TMP_5;  // ---
                final XVI xvi_0_WRPROT1_H  = _stm32lx_xvi_TMP_6;  // ---
                final XVI xvi_0_WRPROT2_L  = _stm32lx_xvi_TMP_7;  // ---
                final XVI xvi_1_SPRMOD_RDP = _stm32lx_xvi_TMP_3;  // STM32L1x
                final XVI xvi_1_0X00_USER  = _stm32lx_xvi_TMP_4;  // ---
                final XVI xvi_1_WRP1_L     = _stm32lx_xvi_TMP_5;  // ---
                final XVI xvi_1_WRP1_H     = _stm32lx_xvi_TMP_6;  // ---
                final XVI xvi_1_WRP2_L     = _stm32lx_xvi_TMP_7;  // ---
                final XVI xvi_1_WRP2_H     = _stm32lx_xvi_TMP_8;  // ---
                final XVI xvi_1_WRP3_L     = _stm32lx_xvi_TMP_9;  // ---
                final XVI xvi_1_WRP3_H     = _stm32lx_xvi_TMP_10; // ---
                final XVI xvi_1_WRP4_L     = _stm32lx_xvi_TMP_11; // ---
                final XVI xvi_1_WRP4_H     = _stm32lx_xvi_TMP_12; // ---
                // Simpy exit if the FLB data was not dirty
                ib.bwOR               ( _fl_stm32fxx_xvi_FLB_FDirty          , _fl_stm32fxx_xvi_FLB_LBDirty, xvi_Tmp0                    );
                ib.jmpIfZero          ( xvi_Tmp0                             , "flb_not_dirty"                                           );
                // ----- Make sure that RDP LEVEL 2 is NEVER ACTIVATED! -----
                ib.appendPrelinkedInst( _fl_stm32fxx_instruction_EnsureRDP2NotActivated                                                  );
                // Set the store index to two
                ib.mov                ( 2                                    , xvi_StoreIdx                                              );
            if(l1x) { // STM32L1x
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Generate SPRMOD_RDP from lock bits and fuse
                ib.ldrDB              ( 0                                    , xvi_Tmp0                                                  );
                ib.ldrDB              ( 1                                    , xvi_Tmp1                                                  );
                ib.bwAND              ( xvi_Tmp0                             , 0x000000FFL          , xvi_Tmp0                           );
                ib.bwAND              ( xvi_Tmp1                             , 0x0000FF00L          , xvi_Tmp1                           );
                ib.bwOR               ( xvi_Tmp0                             , xvi_Tmp1             , xvi_1_SPRMOD_RDP                   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_SPRMOD_RDP     , XVI._NA_    , xvi_Tmp0, xvi_Tmp1   );
                // Generate 0X00_USER from fuse
                ib.ldrDB              ( xvi_StoreIdx                         , xvi_Tmp0                                                  ); // Clear bits [15:08]
                ib.bwAND              ( xvi_Tmp0                             , 0x000000FFL          , xvi_Tmp0                           ); // ---
                ib.strDB              ( xvi_Tmp0                             , xvi_StoreIdx                                              ); // ---
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_0X00_USER      , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                // Generate WRPROTx from fuses
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_WRP1_L         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_WRP1_H         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_WRP2_L         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_WRP2_H         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_WRP3_L         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_WRP3_H         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_WRP4_L         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_1_WRP4_H         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                /*
                ib.debugPrintlnUHex08 ( xvi_1_SPRMOD_RDP                                                                               );
                ib.debugPrintlnUHex08 ( xvi_1_0X00_USER                                                                                );
                ib.debugPrintlnUHex08 ( xvi_1_WRP1_L                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_1_WRP1_H                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_1_WRP2_L                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_1_WRP2_H                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_1_WRP3_L                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_1_WRP3_H                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_1_WRP4_L                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_1_WRP4_H                                                                                   );
                ib.debugPrintlnUDecNN ( _stm32lx_xvi_WRP2_L_NZ                                                                         );
                ib.debugPrintlnUDecNN ( _stm32lx_xvi_WRP2_H_NZ                                                                         );
                ib.debugPrintlnUDecNN ( _stm32lx_xvi_WRP3_L_NZ                                                                         );
                ib.debugPrintlnUDecNN ( _stm32lx_xvi_WRP3_H_NZ                                                                         );
                ib.debugPrintlnUDecNN ( _stm32lx_xvi_WRP4_L_NZ                                                                         );
                ib.debugPrintlnUDecNN ( _stm32lx_xvi_WRP4_H_NZ                                                                         );
                //*/
            }
            else { // STM32L0x
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Generate OPTR_L from lock bits and fuse
                ib.ldrDB              ( 0                                    , xvi_Tmp0                                                  );
                ib.ldrDB              ( 1                                    , xvi_Tmp1                                                  );
                ib.bwAND              ( xvi_Tmp0                             , 0x000000FFL          , xvi_Tmp0                           );
                ib.bwAND              ( xvi_Tmp1                             , 0x0000FF00L          , xvi_Tmp1                           );
                ib.bwOR               ( xvi_Tmp0                             , xvi_Tmp1             , xvi_0_OPTR_L                       );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_0_OPTR_L         , XVI._NA_    , xvi_Tmp0, xvi_Tmp1   );
                // Generate OPTR_H from fuse
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_0_OPTR_H         , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                // Generate WRPROTx from fuses
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_0_WRPROT1_L      , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_0_WRPROT1_H      , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                _stm32flx_putInstGCV  ( ib, swdExecInst                      , xvi_0_WRPROT2_L      , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1   );
                /*
                ib.debugPrintlnUHex08 ( xvi_0_OPTR_L                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_0_OPTR_H                                                                                   );
                ib.debugPrintlnUHex08 ( xvi_0_WRPROT1_L                                                                                );
                ib.debugPrintlnUHex08 ( xvi_0_WRPROT1_H                                                                                );
                ib.debugPrintlnUHex08 ( xvi_0_WRPROT2_L                                                                                );
                ib.debugPrintlnUDecNN ( _stm32lx_xvi_WRP2_L_NZ                                                                         );
                //*/
            }
                //*
                // Unlock FLASH_PECR
                ib.wrBus              ( flashBASE + _stm32lx_flashPEKEYR_ofs , _stm32lx_flashPEKey1                                      ); // Write PEKEY1
                ib.wrBus              ( flashBASE + _stm32lx_flashPEKEYR_ofs , _stm32lx_flashPEKey2                                      ); // Write PEKEY2
                ib.rdBusErrIfCmpEQ    ( flashBASE + _stm32lx_flashPECR_ofs   , 0x00000001L          , 0x00000001L                        ); // Check PE_LOCK
                // Unlock option bytes
                ib.wrBus              ( flashBASE + _stm32lx_flashOPTKEYR_ofs, _stm32lx_flashOPTKey1                                     ); // Write OPTKEY1
                ib.wrBus              ( flashBASE + _stm32lx_flashOPTKEYR_ofs, _stm32lx_flashOPTKey2                                     ); // Write OPTKEY2
                ib.rdBusErrIfCmpEQ    ( flashBASE + _stm32lx_flashPECR_ofs   , 0x00000004L          , 0x00000004L                        ); // Check OPT_LOCK
                //*/
            if(l1x) { // STM32L1x
                /*
                 * ----- The default values for STM32L151* -----
                 * FF5500AA
                 * FF870078
                 * FFFF0000
                 * FFFF0000
                 * 00000000
                 * 00000000
                 * 00000000
                 * 00000000
                 * 00000000
                 * 00000000
                 */
                // Write the data
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, XVI._NA_                        , adrl1_SPRMOD_RDP, xvi_1_SPRMOD_RDP );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, XVI._NA_                        , adrl1_0X00_USER , xvi_1_0X00_USER  );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, XVI._NA_                        , adrl1_WRP1_L    , xvi_1_WRP1_L     );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, XVI._NA_                        , adrl1_WRP1_H    , xvi_1_WRP1_H     );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, _stm32lx_xvi_WRP2_L_NZ          , adrl1_WRP2_L    , xvi_1_WRP2_L     );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, _stm32lx_xvi_WRP2_H_NZ          , adrl1_WRP2_H    , xvi_1_WRP2_H     );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, _stm32lx_xvi_WRP3_L_NZ          , adrl1_WRP3_L    , xvi_1_WRP3_L     );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, _stm32lx_xvi_WRP3_H_NZ          , adrl1_WRP3_H    , xvi_1_WRP3_H     );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, _stm32lx_xvi_WRP4_L_NZ          , adrl1_WRP4_L    , xvi_1_WRP4_L     );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, _stm32lx_xvi_WRP4_H_NZ          , adrl1_WRP4_H    , xvi_1_WRP4_H     );
            }
            else { // STM32L0x
                /*
                 * ----- The default values for STM32L011* -----
                 * FF5500AA
                 * 7F8F8070
                 * FFFF0000
                 * FFFF0000
                 * 00000000
                 */
                // Write the data
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, XVI._NA_                        , adrl0_OPTR_L     , xvi_0_OPTR_L    );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, XVI._NA_                        , adrl0_OPTR_H     , xvi_0_OPTR_H    );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, XVI._NA_                        , adrl0_WRPROT1_L  , xvi_0_WRPROT1_L );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, XVI._NA_                        , adrl0_WRPROT1_H  , xvi_0_WRPROT1_H );
                _stm32flx_putInstWCV  ( ib, swdExecInst, flashBASE, _stm32lx_xvi_WRP2_L_NZ          , adrl0_WRPROT2_L  , xvi_0_WRPROT2_L );
            }
                // Clear all flags
            ib.label                  ( "flb_not_dirty"                                                                                  );
                ib.appendPrelinkedInst( _fl_stm32fxx_instruction_ClearAllFLBFlags                                                        );
        }

        final long[][] instruction_WriteFLB = ib.link();

        // Generate the instructions to read the option bytes
        {
                // For convenience
                final XVI xvi_Tmp0         = _stm32lx_xvi_TMP_0;
                final XVI xvi_Tmp1         = _stm32lx_xvi_TMP_1;
                final XVI xvi_Tmp2         = _stm32lx_xvi_TMP_2;
                final XVI xvi_StoreIdx     = _stm32lx_xvi_TMP_3;
                final XVI xvi_0_OPTR_L     = _stm32lx_xvi_TMP_4;  // STM32L0x
                final XVI xvi_0_OPTR_H     = _stm32lx_xvi_TMP_5;  // ---
                final XVI xvi_0_WRPROT1_L  = _stm32lx_xvi_TMP_6;  // ---
                final XVI xvi_0_WRPROT1_H  = _stm32lx_xvi_TMP_7;  // ---
                final XVI xvi_0_WRPROT2_L  = _stm32lx_xvi_TMP_8;  // ---
                final XVI xvi_1_SPRMOD_RDP = _stm32lx_xvi_TMP_4;  // STM32L1x
                final XVI xvi_1_0X00_USER  = _stm32lx_xvi_TMP_5;  // ---
                final XVI xvi_1_WRP1_L     = _stm32lx_xvi_TMP_6;  // ---
                final XVI xvi_1_WRP1_H     = _stm32lx_xvi_TMP_7;  // ---
                final XVI xvi_1_WRP2_L     = _stm32lx_xvi_TMP_8;  // ---
                final XVI xvi_1_WRP2_H     = _stm32lx_xvi_TMP_9;  // ---
                final XVI xvi_1_WRP3_L     = _stm32lx_xvi_TMP_10; // ---
                final XVI xvi_1_WRP3_H     = _stm32lx_xvi_TMP_11; // ---
                final XVI xvi_1_WRP4_L     = _stm32lx_xvi_TMP_12; // ---
                final XVI xvi_1_WRP4_H     = _stm32lx_xvi_TMP_13; // ---
                // Simpy exit if the FLB data was already read
                ib.jmpIfNotZero      ( _fl_stm32fxx_xvi_FLB_DoneRead    , "flb_done_read"                                              );
                // Set the store index to zero
                ib.mov               ( 0                                , xvi_StoreIdx                                                 );
            if(l1x) { // STM32L1x
                // Read the data
                ib.rdsBits           ( adrl1_SPRMOD_RDP                 , xvi_1_SPRMOD_RDP                                             );
                ib.rdsBits           ( adrl1_0X00_USER                  , xvi_1_0X00_USER                                              );
                ib.rdsBits           ( adrl1_WRP1_L                     , xvi_1_WRP1_L                                                 );
                ib.rdsBits           ( adrl1_WRP1_H                     , xvi_1_WRP1_H                                                 );
                ib.rdsBits           ( adrl1_WRP2_L                     , xvi_1_WRP2_L    , 0x00000000L, _stm32lx_xvi_WRP2_L_NZ        );
                ib.rdsBits           ( adrl1_WRP2_H                     , xvi_1_WRP2_H    , 0x00000000L, _stm32lx_xvi_WRP2_H_NZ        );
                ib.rdsBits           ( adrl1_WRP3_L                     , xvi_1_WRP3_L    , 0x00000000L, _stm32lx_xvi_WRP3_L_NZ        );
                ib.rdsBits           ( adrl1_WRP3_H                     , xvi_1_WRP3_H    , 0x00000000L, _stm32lx_xvi_WRP3_H_NZ        );
                ib.rdsBits           ( adrl1_WRP4_L                     , xvi_1_WRP4_L    , 0x00000000L, _stm32lx_xvi_WRP4_L_NZ        );
                ib.rdsBits           ( adrl1_WRP4_H                     , xvi_1_WRP4_H    , 0x00000000L, _stm32lx_xvi_WRP4_H_NZ        );
                /*
                ib.debugPrintlnUHex08( xvi_1_SPRMOD_RDP                                                                                );
                ib.debugPrintlnUHex08( xvi_1_0X00_USER                                                                                 );
                ib.debugPrintlnUHex08( xvi_1_WRP1_L                                                                                    );
                ib.debugPrintlnUHex08( xvi_1_WRP1_H                                                                                    );
                ib.debugPrintlnUHex08( xvi_1_WRP2_L                                                                                    );
                ib.debugPrintlnUHex08( xvi_1_WRP2_H                                                                                    );
                ib.debugPrintlnUHex08( xvi_1_WRP3_L                                                                                    );
                ib.debugPrintlnUHex08( xvi_1_WRP3_H                                                                                    );
                ib.debugPrintlnUHex08( xvi_1_WRP4_L                                                                                    );
                ib.debugPrintlnUHex08( xvi_1_WRP4_H                                                                                    );
                ib.debugPrintlnUDecNN( _stm32lx_xvi_WRP2_L_NZ                                                                          );
                ib.debugPrintlnUDecNN( _stm32lx_xvi_WRP2_H_NZ                                                                          );
                ib.debugPrintlnUDecNN( _stm32lx_xvi_WRP3_L_NZ                                                                          );
                ib.debugPrintlnUDecNN( _stm32lx_xvi_WRP3_H_NZ                                                                          );
                ib.debugPrintlnUDecNN( _stm32lx_xvi_WRP4_L_NZ                                                                          );
                ib.debugPrintlnUDecNN( _stm32lx_xvi_WRP4_H_NZ                                                                          );
                //*/
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Extract the RDP as lock bits
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_SPRMOD_RDP, 0x000000FFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                // Extract the SPRMOD as fuse
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_SPRMOD_RDP, 0x0000FF00L     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                // Extract the USER as fuse
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_0X00_USER , 0x000000FFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                // Extract the WRPROTx as fuses
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_WRP1_L    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_WRP1_H    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_WRP2_L    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_WRP2_H    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_WRP3_L    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_WRP3_H    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_WRP4_L    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_1_WRP4_H    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
            }
            else { // STM32L0x
                // Read the data
                ib.rdsBits           ( adrl0_OPTR_L                     , xvi_0_OPTR_L                                                 );
                ib.rdsBits           ( adrl0_OPTR_H                     , xvi_0_OPTR_H                                                 );
                ib.rdsBits           ( adrl0_WRPROT1_L                  , xvi_0_WRPROT1_L                                              );
                ib.rdsBits           ( adrl0_WRPROT1_H                  , xvi_0_WRPROT1_H                                              );
                ib.rdsBits           ( adrl0_WRPROT2_L                  , xvi_0_WRPROT2_L , 0x00000000L, _stm32lx_xvi_WRP2_L_NZ        );
                /*
                ib.debugPrintlnUHex08( xvi_0_OPTR_L                                                                                    );
                ib.debugPrintlnUHex08( xvi_0_OPTR_H                                                                                    );
                ib.debugPrintlnUHex08( xvi_0_WRPROT1_L                                                                                 );
                ib.debugPrintlnUHex08( xvi_0_WRPROT1_H                                                                                 );
                ib.debugPrintlnUHex08( xvi_0_WRPROT2_L                                                                                 );
                ib.debugPrintlnUDecNN( _stm32lx_xvi_WRP2_L_NZ                                                                          );
                //*/
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Extract the RDP as lock bits
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_0_OPTR_L    , 0x000000FFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                // Extract the <various> as fuses
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_0_OPTR_L    , 0x0000FF00L     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_0_OPTR_H    , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                // Extract the WRPROTx as fuses
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_0_WRPROT1_L , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_0_WRPROT1_H , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
                _stm32flx_putInstXCV ( ib, swdExecInst, xvi_0_WRPROT2_L , 0x0000FFFFL     , xvi_StoreIdx, xvi_Tmp0, xvi_Tmp1, xvi_Tmp2 );
            }
                // Set flag
                ib.mov               ( 1                                , _fl_stm32fxx_xvi_FLB_DoneRead                                );
            ib.label                 ( "flb_done_read"                                                                                 );
        }

        final long[][] instruction_ReadFLB = ib.link();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Instantiate and return the specifier
        return new Specifier(

            0x00                                                                                   , // flashMemoryEmptyValue
            0                                                                                      , // wrMaxSWDTransferSize
            0                                                                                      , // rdMaxSWDTransferSize
            false                                                                                  , // supportDirectFlashRead
            false                                                                                  , // supportDirectEEPROMRead

            _fl_stm32fxx_instruction_ClearAllFLBFlags                                              , // instruction_InitializeSystemOnce
            null                                                                                   , // instruction_UninitializeSystemExit
            instructionIsFlashLocked                                                               , // instruction_IsFlashLocked
            instruction_UnlockFlash                                                                , // instruction_UnlockFlash
            instruction_EraseAllFlashPages                                                         , // instruction_EraseFlash
            null                                                                                   , // instruction_EraseFlashPages
            null                                                                                   , // instruction_WriteFlash
            null                                                                                   , // instruction_ReadFlash
            null                                                                                   , // instruction_IsEEPROMLocked
            null                                                                                   , // instruction_UnlockEEPROM
            null                                                                                   , // instruction_WriteEEPROM
            null                                                                                   , // instruction_ReadEEPROM
            XVI._NA_                                                                               , // instruction_xviFlashEEPROMAddress
            XVI._NA_                                                                               , // instruction_xviFlashEEPROMReadSize
            XVI._NA_                                                                               , // instruction_xviSignalWorkerCommand
            XVI._NA_                                                                               , // instruction_xviSignalJobState
            null                                                                                   , // instruction_dataBuffFlash
            null                                                                                   , // instruction_dataBuffEEPROM

                                           _fl_stm32l0x_flProgram                                  , // flProgram
            l1x ? _fl_stm32l1x_elProgram : _fl_stm32l0x_elProgram                                  , // elProgram
            config.memorySRAM.address                                                              , // addrProgStart
            config.memorySRAM.address + _stm32lx_PROG_AREA_SIZE + 128                              , // addrProgBuffer
            config.memorySRAM.address + _stm32lx_PROG_AREA_SIZE                                    , // addrProgSignal
            new long[] {                                                                             // wrProgExtraParams
                flashBASE + _stm32lx_flashPEKEYR_ofs , _stm32lx_flashPEKey1 , _stm32lx_flashPEKey2 , // ---
                flashBASE + _stm32lx_flashPRGKEYR_ofs, _stm32lx_flashPRGKey1, _stm32lx_flashPRGKey2, // ---
                flashBASE + _stm32lx_flashPECR_ofs   ,                                               // ---
                flashBASE + _stm32lx_flashSR_ofs                                                     // ---
            }                                                                                      , // ---
            null                                                                                   , // rdProgExtraParams

            instruction_WriteFLB                                                                   , // instruction_WriteFLB
            instruction_ReadFLB                                                                    , // instruction_ReadFLB
            _fl_stm32fxx_xvi_FLB_DoneRead                                                          , // instruction_xviFLB_DoneRead
            _fl_stm32fxx_xvi_FLB_FDirty                                                            , // instruction_xviFLB_FDirty
            _fl_stm32fxx_xvi_FLB_LBDirty                                                           , // instruction_xviFLB_LBDirty
            new int[1 + (l1x ? 10 : 5)]                                                              // instruction_dataBuffFLB

       );
    }

    public static Specifier _getSpecifier_stm32l0x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32lx_impl(_stm32l0x_flashBASE, config, swdExecInst, false); }

    public static Specifier _getSpecifier_stm32l1x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32lx_impl(_stm32l1x_flashBASE, config, swdExecInst, true ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final XVI  _stm32l4x_xvi_TMP_0                 = XVI._0000;
    protected static final XVI  _stm32l4x_xvi_TMP_1                 = XVI._0001;
    protected static final XVI  _stm32l4x_xvi_TMP_2                 = XVI._0002;
    protected static final XVI  _stm32l4x_xvi_TMP_3                 = XVI._0003;

    protected static final long _stm32l4x_PROG_AREA_SIZE            = ProgSWD.DefaultCortexM_LoaderProgramSize; // NOTE : Ensure that this value is the same as the value used in the assembly program!

    protected static final long _stm32l4x__c_g_l_u_wba__flashBASE   = 0x40022000L;
    protected static final long _stm32l4x__wb135_wl5_wle__flashBASE = 0x58004000L;

    protected static class STM32L4X_FRegs {

        public final long   ACR;
        public final long   KEYR;
        public final long   OPTKEYR;
        public final long   SR;
        public final long   CR;
        public final long   CR_LOCK;

        public final long   OPTR;
        public final int    OPTR_mask;     // Without the RDP
        public final long[] OPT_SPEC;
        public final long[] OPT_SPEC_mask;

        public final long   Key1;
        public final long   Key2;
        public final long   OptKey1;
        public final long   OptKey2;

        protected STM32L4X_FRegs(
            final long   _BASE         ,
            final int    _ACR          ,
            final int    _KEYR         ,
            final int    _OPTKEYR      ,
            final int    _SR           ,
            final int    _CR           ,
            final int    _CR_LOCK      ,
            final int    _OPTR         ,
            final int    _OPTR_mask    ,
            final int [] _OPT_SPEC     ,
            final long[] _OPT_SPEC_mask
        )
        {
                ACR           = (_ACR     < 0) ? -1 : (_BASE + _ACR    );
                KEYR          = (_KEYR    < 0) ? -1 : (_BASE + _KEYR   );
                OPTKEYR       = (_OPTKEYR < 0) ? -1 : (_BASE + _OPTKEYR);
                SR            = (_SR      < 0) ? -1 : (_BASE + _SR     );
                CR            = (_CR      < 0) ? -1 : (_BASE + _CR     );
                CR_LOCK       = (_CR_LOCK < 0) ? -1 : (_BASE + _CR_LOCK);

                OPTR          = (_OPTR    < 0) ? -1 : (_BASE + _OPTR   );
                OPTR_mask     = (_OPTR    < 0) ?  0 : (_OPTR_mask      );

            if(_OPT_SPEC == null || _OPT_SPEC_mask == null) {
                OPT_SPEC      = new long[0];
                OPT_SPEC_mask = new long[0];
            }
            else {
                OPT_SPEC      =       new long[_OPT_SPEC.length];
                OPT_SPEC_mask = XCom.arrayCopy(_OPT_SPEC_mask  );
                for(int i = 0; i < OPT_SPEC.length; ++i) OPT_SPEC[i] = _BASE + _OPT_SPEC[i];
            }

                Key1          = 0x45670123L;
                Key2          = 0xCDEF89ABL;
                OptKey1       = 0x08192A3BL;
                OptKey2       = 0x4C5D6E7FL;
        }

    } // class STM32L4X_FRegs

    protected static class STM32L4X_FRegs_Std extends STM32L4X_FRegs {
        public STM32L4X_FRegs_Std(final long flashBASE, final int OPTR_mask_, final int[] OPT_SPEC_, final long[] OPT_SPEC_mask_)
        { super(flashBASE, 0x00, 0x08, 0x0C, 0x10, 0x14, -1  , 0x20, OPTR_mask_, OPT_SPEC_, OPT_SPEC_mask_); }
    }

    protected static class STM32L4X_FRegs_WLCPU2 extends STM32L4X_FRegs {
        public STM32L4X_FRegs_WLCPU2(final long flashBASE, final int OPTR_mask_, final int[] OPT_SPEC_, final long[] OPT_SPEC_mask_)
        { super(flashBASE, 0x00, 0x08, 0x10, 0x60, 0x64, 0x14, 0x20, OPTR_mask_, OPT_SPEC_, OPT_SPEC_mask_); }
    }

    protected static class STM32L4X_FRegs_L5NS extends STM32L4X_FRegs {
        public STM32L4X_FRegs_L5NS(final long flashBASE, final int OPTR_mask_, final int[] OPT_SPEC_, final long[] OPT_SPEC_mask_)
        { super(flashBASE, 0x00, 0x08, 0x10, 0x20, 0x28, -1  , 0x40, OPTR_mask_, OPT_SPEC_, OPT_SPEC_mask_); }
    }

    protected static class STM32L4X_FRegs_L5S extends STM32L4X_FRegs {
        public STM32L4X_FRegs_L5S(final long flashBASE, final int OPTR_mask_, final int[] OPT_SPEC_, final long[] OPT_SPEC_mask_)
        { super(flashBASE, 0x00, 0x0C, 0x10, 0x24, 0x2C, -1  , 0x40, OPTR_mask_, OPT_SPEC_, OPT_SPEC_mask_); }
    }

    private static void _stm32fl4x_putInstXCV(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final XVI xviSrc, final long storeMask, final XVI xviStoreIdx, final XVI xviTmp0)
    {
         ib.bwAND( xviSrc     , storeMask  , xviTmp0 );
         ib.strDB( xviTmp0    , xviStoreIdx          );
         ib.inc1 ( xviStoreIdx                       );
    }

    protected static long[][] _fl_stm32l4x_instruction_IsFlashLocked(final STM32L4X_FRegs _stm32l4xFRegs)
    {
        return new SWDExecInstBuilder() {{
            rdBusRetCmpEQ       ( _stm32l4xFRegs.CR  , 0x80000000L        , 0x80000000L ); // Read  LOCK
        }}.link();
    }

    protected static long[][] _fl_stm32l4x_instruction_UnlockFlash(final STM32L4X_FRegs _stm32l4xFRegs)
    {
        return new SWDExecInstBuilder() {{
            rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR  , 0x00010000L        , 0x00000000L ); // Wait  BSY            // ##### !!! TODO : Use timeout !!! #####
            wrBus               ( _stm32l4xFRegs.KEYR, _stm32l4xFRegs.Key1              ); // Write KEY1
            wrBus               ( _stm32l4xFRegs.KEYR, _stm32l4xFRegs.Key2              ); // Write KEY2
            rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR  , 0x00010000L        , 0x00000000L ); // Wait  BSY            // ##### !!! TODO : Use timeout !!! #####
        }}.link();
    }

    protected static long[][] _fl_stm32l4x_instruction_EraseFlash(final STM32L4X_FRegs _stm32l4xFRegs)
    {
        return new SWDExecInstBuilder() {{
            rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR  , 0x00010000L        , 0x00000000L ); // Wait  BSY            // ##### !!! TODO : Use timeout !!! #####
            wrBus               ( _stm32l4xFRegs.CR  , 0x00000004L                      ); // Write MER1
            wrBus               ( _stm32l4xFRegs.CR  , 0x00010004L                      ); // Write MER1 STRT
            rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR  , 0x00010000L        , 0x00000000L ); // Wait  BSY            // ##### !!! TODO : Use timeout !!! #####
        }}.link();
    }

    protected static long[][] _fl_stm32l4x_instruction_EraseFlash_DB(final STM32L4X_FRegs _stm32l4xFRegs)
    {
        return new SWDExecInstBuilder() {{ // With a dual bank flash
            rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR  , 0x00010000L        , 0x00000000L ); // Wait  BSY            // ##### !!! TODO : Use timeout !!! #####
            wrBus               ( _stm32l4xFRegs.CR  , 0x00000804L                      ); // Write MER1 MER2
            wrBus               ( _stm32l4xFRegs.CR  , 0x00010804L                      ); // Write MER1 MER2 STRT
            rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR  , 0x00010000L        , 0x00000000L ); // Wait  BSY            // ##### !!! TODO : Use timeout !!! #####
        }}.link();
    }

    protected static long[][] _fl_stm32l4x_instruction_WriteFLB(final STM32L4X_FRegs _stm32l4xFRegs, final SWDExecInst swdExecInst)
    {
        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */

        final XVI xvi_Tmp0  = _stm32l4x_xvi_TMP_0;
        final XVI xvi_Tmp1  = _stm32l4x_xvi_TMP_1;
        final XVI xvi_OPTR  = _stm32l4x_xvi_TMP_2;
        final XVI xvi_SPECn = _stm32l4x_xvi_TMP_3;

        return new SWDExecInstBuilder() {{
                // Simpy exit if the FLB data was not dirty
                bwOR                ( _fl_stm32fxx_xvi_FLB_FDirty, _fl_stm32fxx_xvi_FLB_LBDirty    , xvi_Tmp0    );
                jmpIfZero           ( xvi_Tmp0                   , "flb_not_dirty"                               );
                // ----- Make sure that RDP LEVEL 2 is NEVER ACTIVATED! -----
                appendPrelinkedInst ( _fl_stm32fxx_instruction_EnsureRDP2NotActivated                            );
                //*
                // Unlock option bytes
                rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR          , 0x00010000L                     , 0x00000000L ); // Wait  BSY   // ##### !!! TODO : Use timeout !!! #####
                wrBus               ( _stm32l4xFRegs.KEYR        , _stm32l4xFRegs.Key1                           ); // Write KEY1
                wrBus               ( _stm32l4xFRegs.KEYR        , _stm32l4xFRegs.Key2                           ); // Write KEY2
                rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR          , 0x00010000L                     , 0x00000000L ); // Wait  BSY   // ##### !!! TODO : Use timeout !!! #####
                wrBus               ( _stm32l4xFRegs.OPTKEYR     , _stm32l4xFRegs.OptKey1                        ); // Write OPTKEY1
                wrBus               ( _stm32l4xFRegs.OPTKEYR     , _stm32l4xFRegs.OptKey2                        ); // Write OPTKEY2
                rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR          , 0x00010000L                     , 0x00000000L ); // Wait  BSY   // ##### !!! TODO : Use timeout !!! #####
                rdBusErrIfCmpEQ     ( _stm32l4xFRegs.CR          , 0x40000000L                     , 0x40000000L ); // Check OPT_LOCK
                //*/
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Generate and write OPTR from lock bits and fuse
                ldrDB               ( 0                          , xvi_Tmp0                                      ); // Generate
                ldrDB               ( 1                          , xvi_Tmp1                                      ); // ---
                bwAND               ( xvi_Tmp0                   , 0x000000FFL                     , xvi_Tmp0    ); // ---
                bwAND               ( xvi_Tmp1                   ,  _stm32l4xFRegs.OPTR_mask       , xvi_Tmp1    ); // ---
                bwOR                ( xvi_Tmp0                   , xvi_Tmp1                        , xvi_Tmp1    ); // ---
                rdsBits             ( _stm32l4xFRegs.OPTR        , xvi_OPTR                                      ); // Keep all reserved bits at their original values
                bwAND               ( xvi_OPTR                   , ~_stm32l4xFRegs.OPTR_mask       , xvi_OPTR    ); // ---
                bwOR                ( xvi_Tmp1                   , xvi_OPTR                        , xvi_OPTR    ); // ---
                /*
                debugPrintlnUHex08  ( xvi_OPTR                                                                   );
                //*/
                wrtBits             ( _stm32l4xFRegs.OPTR        , xvi_OPTR                                      ); // Write
                // Generate and write the others from fuses
            for(int i = 0; i < _stm32l4xFRegs.OPT_SPEC.length; ++i) {
                ldrDB               ( i + 2                      , xvi_Tmp0                                      ); // Generate
                bwAND               ( xvi_Tmp0                   ,  _stm32l4xFRegs.OPT_SPEC_mask[i], xvi_Tmp0    ); // ---
                rdsBits             ( _stm32l4xFRegs.OPT_SPEC[i] , xvi_SPECn                                     ); // Keep all reserved bits at their original values
                bwAND               ( xvi_SPECn                  , ~_stm32l4xFRegs.OPT_SPEC_mask[i], xvi_SPECn   ); // ---
                bwOR                ( xvi_Tmp0                   , xvi_SPECn                       , xvi_SPECn   ); // ---
                /*
                debugPrintlnUHex08  ( xvi_SPECn                                                                  );
                //*/
                wrtBits             ( _stm32l4xFRegs.OPT_SPEC[i] , xvi_SPECn                                     ); // Write
            }
                //*
                // Start the option bytes programming
                setBits             ( _stm32l4xFRegs.CR          ,                                   0x00020000L ); // Write OPTSTRT
                rdBusLoopWhileCmpNEQ( _stm32l4xFRegs.SR          , 0x00010000L                     , 0x00000000L ); // Wait  BSY   // ##### !!! TODO : Use timeout !!! #####
                //*/
                // Clear all flags
            label                   ( "flb_not_dirty"                                                            );
                appendPrelinkedInst ( _fl_stm32fxx_instruction_ClearAllFLBFlags                                  );
        }}.link();
    }

    protected static long[][] _fl_stm32l4x_instruction_ReadFLB(final STM32L4X_FRegs _stm32l4xFRegs, final SWDExecInst swdExecInst)
    {
        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */

        final XVI xvi_Tmp0     = _stm32l4x_xvi_TMP_0;
        final XVI xvi_StoreIdx = _stm32l4x_xvi_TMP_1;
        final XVI xvi_OPTR     = _stm32l4x_xvi_TMP_2;
        final XVI xvi_SPECn    = _stm32l4x_xvi_TMP_3;

        return new SWDExecInstBuilder() {{
                // Simpy exit if the FLB data was already read
                jmpIfNotZero         ( _fl_stm32fxx_xvi_FLB_DoneRead, "flb_done_read"                                         );
                // Set the store index to zero
                mov                  ( 0                            , xvi_StoreIdx                                            );
                // Read the OPTR
                rdsBits              ( _stm32l4xFRegs.OPTR          , xvi_OPTR                                                );
                /*
                debugPrintlnUHex08   ( xvi_OPTR                                                                               );
                //*/
                // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
                // Extract the RDP from OPTR as lock bits
                _stm32fl4x_putInstXCV( this, swdExecInst, xvi_OPTR  , 0x000000FFL                    , xvi_StoreIdx, xvi_Tmp0 );
                // Extract the rest from OPTR as fuse
                _stm32fl4x_putInstXCV( this, swdExecInst, xvi_OPTR  , _stm32l4xFRegs.OPTR_mask       , xvi_StoreIdx, xvi_Tmp0 );
                // Read and extract the others as fuses
            for(int i = 0; i < _stm32l4xFRegs.OPT_SPEC.length; ++i) {
                rdsBits              ( _stm32l4xFRegs.OPT_SPEC[i]   , xvi_SPECn                                               );
                /*
                debugPrintlnUHex08   ( xvi_SPECn                                                                              );
                //*/
                _stm32fl4x_putInstXCV( this, swdExecInst, xvi_SPECn , _stm32l4xFRegs.OPT_SPEC_mask[i], xvi_StoreIdx, xvi_Tmp0 );
            }
                // Set flag
                mov                  ( 1                            , _fl_stm32fxx_xvi_FLB_DoneRead                           );
            label                    ( "flb_done_read"                                                                        );
        }}.link();
    }

    protected static final long[] _fl_stm32l4x_flProgram_std;
    static {
        try                      { _fl_stm32l4x_flProgram_std = SWDAsmARM_STM32.flProgram_stm32l4x(false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);         }
    }

    protected static final long[] _fl_stm32l4x_flProgram(final long sramStart)
    {
        try                      { return SWDAsmARM_STM32.flProgram_stm32l4x(sramStart, false); }
        catch(final Exception e) { throw SysUtil.systemExitError_runtimeException(e);           }
    }

    protected static Specifier _getSpecifier_stm32l4x_impl(final ProgSWD.Config config, final SWDExecInst swdExecInst, final STM32L4X_FRegs flashRegs, final int wrMaxSWDTransferSize, final int rdMaxSWDTransferSize)
    {
        final long[]         flProgram                 = (config.memorySRAM.address == ProgSWD.DefaultCortexM_SRAMStart)
                                                       ? _fl_stm32l4x_flProgram_std
                                                       : _fl_stm32l4x_flProgram(config.memorySRAM.address);

        final boolean        dualBank                  = config.memoryFlash.dualBank;
        final boolean        noFLB                     = (flashRegs.OPT_SPEC == null || flashRegs.OPT_SPEC_mask == null);

        final long[][]       instruction_IsFlashLocked =            _fl_stm32l4x_instruction_IsFlashLocked(flashRegs)                                                              ;
        final long[][]       instruction_UnlockFlash   =            _fl_stm32l4x_instruction_UnlockFlash  (flashRegs)                                                              ;
        final long[][]       instruction_EraseFlash    = dualBank ? _fl_stm32l4x_instruction_EraseFlash_DB(flashRegs) : _fl_stm32l4x_instruction_EraseFlash(flashRegs             );
        final long[][]       instruction_WriteFLB      = noFLB    ? null                                              : _fl_stm32l4x_instruction_WriteFLB  (flashRegs, swdExecInst);
        final long[][]       instruction_ReadFLB       = noFLB    ? null                                              : _fl_stm32l4x_instruction_ReadFLB   (flashRegs, swdExecInst);

        return new Specifier(

            0xFF                                                                                     , // flashMemoryEmptyValue
            wrMaxSWDTransferSize                                                                     , // wrMaxSWDTransferSize
            rdMaxSWDTransferSize                                                                     , // rdMaxSWDTransferSize
            false                                                                                    , // supportDirectFlashRead
            false                                                                                    , // supportDirectEEPROMRead

            _fl_stm32fxx_instruction_ClearAllFLBFlags                                                , // instruction_InitializeSystemOnce
            null                                                                                     , // instruction_UninitializeSystemExit
            instruction_IsFlashLocked                                                                , // instruction_IsFlashLocked
            instruction_UnlockFlash                                                                  , // instruction_UnlockFlash
            instruction_EraseFlash                                                                   , // instruction_EraseFlash
            null                                                                                     , // instruction_EraseFlashPages
            null                                                                                     , // instruction_WriteFlash
            null                                                                                     , // instruction_ReadFlash
            null                                                                                     , // instruction_IsEEPROMLocked
            null                                                                                     , // instruction_UnlockEEPROM
            null                                                                                     , // instruction_WriteEEPROM
            null                                                                                     , // instruction_ReadEEPROM
            XVI._NA_                                                                                 , // instruction_xviFlashEEPROMAddress
            XVI._NA_                                                                                 , // instruction_xviFlashEEPROMReadSize
            XVI._NA_                                                                                 , // instruction_xviSignalWorkerCommand
            XVI._NA_                                                                                 , // instruction_xviSignalJobState
            null                                                                                     , // instruction_dataBuffFlash
            null                                                                                     , // instruction_dataBuffEEPROM

            flProgram                                                                                , // flProgram
            null                                                                                     , // elProgram
            config.memorySRAM.address                                                                , // addrProgStart
            config.memorySRAM.address + _stm32l4x_PROG_AREA_SIZE + 128                               , // addrProgBuffer
            config.memorySRAM.address + _stm32l4x_PROG_AREA_SIZE                                     , // addrProgSignal
            new long[] { flashRegs.KEYR, flashRegs.Key1, flashRegs.Key2, flashRegs.CR, flashRegs.SR }, // wrProgExtraParams
            null                                                                                     , // rdProgExtraParams

            noFLB ? null     : instruction_WriteFLB                                                  , // instruction_WriteFLB
            noFLB ? null     : instruction_ReadFLB                                                   , // instruction_ReadFLB
            noFLB ? XVI._NA_ : _fl_stm32fxx_xvi_FLB_DoneRead                                         , // instruction_xviFLB_DoneRead
            noFLB ? XVI._NA_ : _fl_stm32fxx_xvi_FLB_FDirty                                           , // instruction_xviFLB_FDirty
            noFLB ? XVI._NA_ : _fl_stm32fxx_xvi_FLB_LBDirty                                          , // instruction_xviFLB_LBDirty
            noFLB ? null     : new int[1 + 1 + flashRegs.OPT_SPEC.length]                              // instruction_dataBuffFLB

       );
    }

    protected static Specifier _getSpecifier_stm32l4x_impl(final ProgSWD.Config config, final SWDExecInst swdExecInst, final STM32L4X_FRegs flashRegs)
    { return _getSpecifier_stm32l4x_impl(config, swdExecInst, flashRegs, 0, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*            SRAM         Flash                   OTP Flash    EEPROM       External Flash
     * STM32L4X   0x20000000   0x08000000              0x1FFF7000   0x08080000   [<info exists QUADSPI> 0x90000000 ; <CR> 0xA0001000] [<info exists OCTOSPI1> 0x90000000 ; <CR> 0xA0001000] [<info exists OCTOSPI2> 0x70000000 ; <CR> 0xA0001400]
     * STM32L5X   0x20000000   0x08000000 0x08040000   0x0BFA0000   0x08080000
     *
     * NOTE : For these devices, it is possible to erase flash banks 0 and 1 simultaneously.
     *
     * ##### !!! TODO : How about the OTP flash                             !!! #####
     * ##### !!! TODO : How about the optional external flash for STM32L4X? !!! #####
     */

    public static Specifier _getSpecifier_stm32l4x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        return _getSpecifier_stm32l4x_impl( config, swdExecInst, new STM32L4X_FRegs_Std(
            _stm32l4x__c_g_l_u_wba__flashBASE,
            0b00001111100011110111011100000000,
            //           PCROP1SR     PCROP1ER     WRP1AR       WRP1BR
            new int [] { 0x24       , 0x28       , 0x2C       , 0x30        },
            new long[] { 0x0000FFFFL, 0x8000FFFFL, 0x00FF00FFL, 0x007F007FL }
        ) );
    }

    public static Specifier _getSpecifier_stm32l5x_ns(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
         return _getSpecifier_stm32l4x_impl( config, swdExecInst, new STM32L4X_FRegs_L5NS(
            _stm32l4x__c_g_l_u_wba__flashBASE,
            0b10011111011111110111011100000000,
            //           NSBOOTADD0R  NSBOOTADD1R  WRP1AR       WRP1BR       WRP2AR       WRP2BR       PRIVCFGR
            new int [] { 0x44       , 0x48       , 0x58       , 0x5C       , 0x68       , 0x6C       , 0xC4        },
            new long[] { 0xFFFFFF80L, 0xFFFFFF80L, 0x007F007FL, 0x007F007FL, 0x007F007FL, 0x007F007FL, 0x00000001L }
         ) );
    }

    public static Specifier _getSpecifier_stm32l5x_s(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
         return _getSpecifier_stm32l4x_impl( config, swdExecInst, new STM32L4X_FRegs_L5NS(
            _stm32l4x__c_g_l_u_wba__flashBASE,
            0b10011111011111110111011100000000,
            //           NSBOOTADD0R  NSBOOTADD1R  SECBOOTADD0R SECWM1R1     SECWM1R2     WRP1AR       WRP1BR       SECWM2R1     SECWM2R2     WRP2AR       WRP2BR       SECBB1R1     SECBB1R2     SECBB1R3     SECBB1R4     SECBB2R1     SECBB2R2     SECBB2R3     SECBB2R4     SECHDPCR     PRIVCFGR
            new int [] { 0x44       , 0x48       , 0x4C       , 0x50       , 0x54       , 0x58       , 0x5C       , 0x60       , 0x64       , 0x68       , 0x6C       , 0x80       , 0x84       , 0x88       , 0x8C       , 0xA0       , 0xA4       , 0xA8       , 0xAC       , 0xC0       , 0xC4        },
            new long[] { 0xFFFFFF80L, 0xFFFFFF80L, 0xFFFFFF81L, 0x007F007FL, 0x807F0000L, 0x007F007FL, 0x007F007FL, 0x007F007FL, 0x807F0000L, 0x007F007FL, 0x007F007FL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0x00000003L, 0x00000001L }
         ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*            SRAM         Flash        OTP Flash
     * STM32G0X   0x20000000   0x08000000   0x1FFF7000
     * STM32G4X   0x20000000   0x08000000   0x1FFF7000
     * STM32WLX   0x20008000   0x08000000   0x1FFF7000
     *
     * NOTE : For these devices, it is possible to erase flash banks 0 and 1 simultaneously.
     *
     * ##### !!! TODO : How about the OTP flash !!! #####
     */

    public static Specifier _getSpecifier_stm32g0x0(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        // ##### !!! TODO : It seems that if the transfer size is too large, the data read/written using SWD will be corrupted !!! #####
        final int _stm32g0x_wrMaxSWDTransferSize = 128; // Why using anything larger than 128 bytes would corrupt the TAR register or stall the process???
        final int _stm32g0x_rdMaxSWDTransferSize = 512; // Why using anything larger than 512 bytes would corrupt the read data???

        return _getSpecifier_stm32l4x_impl( config, swdExecInst, new STM32L4X_FRegs_Std(
            _stm32l4x__c_g_l_u_wba__flashBASE,
            0b00000111011111110110000000000000,
            //           WRP1AR       WRP1BR       WRP2AR       WRP2BR
            new int [] { 0x2C       , 0x30       , 0x4C       , 0x50        },
            new long[] { 0x007F007FL, 0x007F007FL, 0x007F007FL, 0x007F007FL }
        ), _stm32g0x_wrMaxSWDTransferSize, _stm32g0x_rdMaxSWDTransferSize );
    }

    public static Specifier _getSpecifier_stm32g4x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        final int     devID = swdExecInst._progSWD().inProgMode() ? swdExecInst._progSWD().stm32DeviceID() : 0x468;
        final boolean cat2  = (devID == 0x468);
        final boolean cat3  = (devID == 0x469);
        final boolean cat4  = (devID == 0x479);

        if(cat2) {
            return _getSpecifier_stm32l4x_impl( config, swdExecInst, new STM32L4X_FRegs_Std(
                _stm32l4x__c_g_l_u_wba__flashBASE,
                0b01111111110111110111011100000000,
                //           PCROP1SR     PCROP1ER     WRP1AR       WRP1BR       PCROP2SR     PCROP2ER     WRP2AR       WRP2BR       SEC1R        SEC2R
                new int [] { 0x24       , 0x28       , 0x2C       , 0x30       , 0x44       , 0x48,        0x4C       , 0x50       , 0x70       , 0x74        },
                new long[] { 0x00007FFFL, 0x80007FFFL, 0x007F007FL, 0x007F007FL, 0x00007FFFL, 0x00007FFFL, 0x007F007FL, 0x007F007FL, 0x0001007FL, 0x0000007FL }
            ) );
        }

        if(cat3) {
            return _getSpecifier_stm32l4x_impl( config, swdExecInst, new STM32L4X_FRegs_Std(
                _stm32l4x__c_g_l_u_wba__flashBASE,
                0b01111111100011110111011100000000,
                //           PCROP1SR     PCROP1ER     WRP1AR       WRP1BR       SEC1R
                new int [] { 0x24       , 0x28       , 0x2C       , 0x30       , 0x70        },
                new long[] { 0x00007FFFL, 0x80007FFFL, 0x007F007FL, 0x007F007FL, 0x0001007FL }
            ) );
        }

        if(cat4) {
            return _getSpecifier_stm32l4x_impl( config, swdExecInst, new STM32L4X_FRegs_Std(
                _stm32l4x__c_g_l_u_wba__flashBASE,
                0b01111111110011110111011100000000,
                //           PCROP1SR     PCROP1ER     WRP1AR       WRP1BR       SEC1R
                new int [] { 0x24       , 0x28       , 0x2C       , 0x30       , 0x70        },
                new long[] { 0x0000FFFFL, 0x8000FFFFL, 0x00FF00FFL, 0x007F007FL, 0x000100FFL }
            ) );
        }

        return null; // Unknown/unsupported device category
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Specifier _getSpecifier_stm32wlex(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        return _getSpecifier_stm32l4x_impl( config, swdExecInst, new STM32L4X_FRegs_Std(
            _stm32l4x__wb135_wl5_wle__flashBASE,
            0b01001111100011110111111100000000,
            //           PCROP1ASR    PCROP1AER    WRP1AR       WRP1BR       PCROP1BSR    PCROP1BER
            new int [] { 0x24       , 0x28       , 0x2C       , 0x30       , 0x34       , 0x38,       },
            new long[] { 0x000000FFL, 0x800000FFL, 0x007F007FL, 0x007F007FL, 0x000000FFL, 0x000000FFL }

        ) );
    }

    /*
     * ##### !!! TODO : Add support for more MCUs! !!! #####
     *
     * [ ] STM32C0X  0x01800 0x20000000 | stm32l4x 0x08000000 !0x1FFF7000
     * [ ] STM32U5X  0x10000 0x20000000 | stm32l4x 0x08000000
     * [ ] STM32WBAX 0x10000 0x20000000 | stm32l4x 0x08000000 !0x0FF90000
     * [ ] STM32WBX  0x10000 0x20000000 | stm32l4x 0x08000000 !0x1FFF7000
     */

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*            SRAM         Flash                     External Flash
     * STM32H7X   0x20000000   0x08000000 [0x08100000]   [<info exists QUADSPI> 0x90000000 ; <CR> 0x52005000] [<info exists OCTOSPI1> 0x90000000 ; <CR> 0x52005000] [<info exists OCTOSPI2> 0x70000000 ; <CR> 0x5200A000]
     *
     * NOTE : # For STM32H[4|5|A|B]X devices, flash banks 0 and 1 must be erased     separately!
     *        # For STM32H[4|5|A|B]X devices, flash banks 0 and 1 must be programmed separately!
     *        # For these devices, flash write operations must be done in flash-words:
     *            STM32H7[2|3]X => one flash-word is 32 bytes
     *            STM32H7[4|5]X => one flash-word is 32 bytes
     *            STM32H7[A|B]X => one flash-word is 16 bytes
     */

    // ##### !!! TODO : It seems that if the transfer size is too large, the data read/written using SWD will be corrupted !!! #####
    public    static final int  _stm32h7x_wrMaxSWDTransferSize = 128; // Why using anything larger than 128 bytes would corrupt the TAR register???
    public    static final int  _stm32h7x_rdMaxSWDTransferSize = 512; // Why using anything larger than 512 bytes would corrupt the read data???

    protected static final XVI  _stm32h7x_xvi_TMP_0            = XVI._0000;
    protected static final XVI  _stm32h7x_xvi_TMP_1            = XVI._0001;
    protected static final XVI  _stm32h7x_xvi_TMP_2            = XVI._0002;

    protected static final long _stm32h7x_PROG_AREA_SIZE       = ProgSWD.DefaultCortexM_LoaderProgramSize; // NOTE : Ensure that this value is the same as the value used in the assembly program!

    protected static final long _stm32h7x_flashRegBaseB0       = 0x52002000L;
    protected static final long _stm32h7x_flashRegBaseB1       = 0x52002100L;

    protected static class STM32H7X_FRegs {

        public final long   KEYR;
        public final long   SR;
        public final long   CR;

        public final long   OPTKEYR;
        public final long   OPTSR;
        public final long   OPTCR;
        public final long   OPTCCR;

        public final long[] OPT_SPEC;
        public final long[] OPT_SPEC_mask; // Without the RDP
        public final int    RDP_index;
        public final int    RDP_shift;

        public final long   Key1;
        public final long   Key2;
        public final long   OptKey1;
        public final long   OptKey2;

        protected STM32H7X_FRegs(
            final long   _BASE         ,
            final int [] _OPT_SPEC     ,
            final long[] _OPT_SPEC_mask,
            final int    _RDP_index    ,
            final int    _RDP_shift
        )
        {
                KEYR          = _BASE + 0x004;
                SR            = _BASE + 0x010;
                CR            = _BASE + 0x00C;

                OPTKEYR       = _BASE + 0x008;
                OPTSR         = _BASE + 0x01C;
                OPTCR         = _BASE + 0x018;
                OPTCCR        = _BASE + 0x024;

            if(_OPT_SPEC == null || _OPT_SPEC_mask == null) {
                OPT_SPEC      = new long[0];
                OPT_SPEC_mask = new long[0];
                RDP_index     = -1;
                RDP_shift     = -1;
            }
            else {
                OPT_SPEC      =       new long[_OPT_SPEC.length];
                OPT_SPEC_mask = XCom.arrayCopy(_OPT_SPEC_mask  );
                RDP_index     = _RDP_index;
                RDP_shift     = _RDP_shift;
                for(int i = 0; i < OPT_SPEC.length; ++i) OPT_SPEC[i] = _BASE + _OPT_SPEC[i];
            }

                Key1          = 0x45670123L;
                Key2          = 0xCDEF89ABL;
                OptKey1       = 0x08192A3BL;
                OptKey2       = 0x4C5D6E7FL;
        }

    } // class STM32H7X_FRegs

    protected static final long _stm32h7x_2345_flash_cr(final long cmd, final long snb)
    { return cmd | (snb << 8); }

    protected static final long _stm32h7x_ab_flash_cr(final long cmd, final long snb)
    { return (cmd & 0xFFFFFF0FL) | ( (cmd & 0b11000000) >> 2 ) | (snb << 6); }

    private static void _stm32h7x_putInstXCV(final SWDExecInstBuilder ib, final SWDExecInst swdExecInst, final XVI xviSrc, final long storeMask, final XVI xviStoreIdx, final XVI xviTmp0)
    {
         ib.bwAND( xviSrc     , storeMask  , xviTmp0 );
         ib.strDB( xviTmp0    , xviStoreIdx          );
         ib.inc1 ( xviStoreIdx                       );
    }

    @package_private
    static final long _stm32h7x_flash_cr(final boolean varAB, final long cmd, final long snb)
    {
        // STM32H7[2|3|4|5]X : [10:8]SNB [7]START [6]FW [5:4]PSIZE
        // STM32H7[A|B]X     : [12:6]SNB                [5]START [4]FW
        return varAB ? _stm32h7x_ab_flash_cr  (cmd, snb)
                     : _stm32h7x_2345_flash_cr(cmd, snb);
    }

    @package_private
    static final long _stm32h7x_flash_cr(final boolean varAB, final long cmd)
    { return _stm32h7x_flash_cr(varAB, cmd, 0); }

    protected static final long[] _fl_stm32h7_2345_x_flProgram;
    static {
        try                      { _fl_stm32h7_2345_x_flProgram = SWDAsmARM_STM32.flProgram_stm32h7x(false, false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);         }
    }

    protected static final long[] _fl_stm32h7_ab_x_flProgram;
    static {
        try                      { _fl_stm32h7_ab_x_flProgram   = SWDAsmARM_STM32.flProgram_stm32h7x(true , false); }
        catch(final Exception e) { throw SysUtil.systemExitError_staticInitializationBlock(e);         }
    }

    protected static long[][] _fl_stm32h7x_instruction_WriteFLB(final STM32H7X_FRegs fRegs, final SWDExecInst swdExecInst)
    {
        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */

        final XVI xvi_Tmp0  = _stm32h7x_xvi_TMP_0;
        final XVI xvi_Tmp1  = _stm32h7x_xvi_TMP_1;
        final XVI xvi_SPECn = _stm32h7x_xvi_TMP_2;

        return new SWDExecInstBuilder() {{
                    // Simpy exit if the FLB data was not dirty
                    bwOR                ( _fl_stm32fxx_xvi_FLB_FDirty, _fl_stm32fxx_xvi_FLB_LBDirty, xvi_Tmp0    );
                    jmpIfZero           ( xvi_Tmp0                   , "flb_not_dirty"                           );
                    // ----- Make sure that RDP LEVEL 2 is NEVER ACTIVATED! -----
                    appendPrelinkedInst ( _fl_stm32fxx_instruction_EnsureRDP2NotActivated                        );
                    //*
                    // Unlock option bytes
                    wrBus               ( fRegs.OPTKEYR              , fRegs.OptKey1                             ); // Write OPTKEY1
                    wrBus               ( fRegs.OPTKEYR              , fRegs.OptKey2                             ); // Write OPTKEY2
                    rdBusErrIfCmpEQ     ( fRegs.OPTCR                , 0x00000001L                 , 0x00000001L ); // Check OPTLOCK
                    //*/
                    // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
            for(int i = 0; i < fRegs.OPT_SPEC.length; ++i) {
                if(i == fRegs.RDP_index) {
                    // Generate RDP from lock bits
                    ldrDB               ( 0                          , xvi_Tmp1                                  );
                    bwLSH               ( xvi_Tmp1                   , fRegs.RDP_shift             , xvi_Tmp1    );
                    // Generate the rest from the fuse
                    ldrDB               ( i + 1                      , xvi_Tmp0                                  );
                    bwAND               ( xvi_Tmp0                   ,  fRegs.OPT_SPEC_mask[i]     , xvi_Tmp0    );
                    // Combine the bits
                    bwOR                ( xvi_Tmp1                   , xvi_Tmp0                    , xvi_Tmp0    );
                }
                else {
                    // Generate from the fuse
                    ldrDB               ( i + 1                      , xvi_Tmp0                                  );
                    bwAND               ( xvi_Tmp0                   ,  fRegs.OPT_SPEC_mask[i]     , xvi_Tmp0    );
                }
                    // Keep all reserved bits at their original values
                    rdsBits             ( fRegs.OPT_SPEC[i]          , xvi_SPECn                                 ); // Keep all reserved bits at their original values
                    bwAND               ( xvi_SPECn                  , ~fRegs.OPT_SPEC_mask[i]     , xvi_SPECn   ); // ---
                    bwOR                ( xvi_Tmp0                   , xvi_SPECn                   , xvi_SPECn   ); // ---
                    /*
                    debugPrintlnUHex08  ( xvi_SPECn                                                              );
                    //*/
                    // Write the option byte
                    wrtBits             ( fRegs.OPT_SPEC[i]          , xvi_SPECn                                 );
            } // for
                    //*
                    // Start the option bytes programming
                    setBits             ( fRegs.OPTCCR               ,                               0x40000000L ); // Write CLR_OPTCHANGEERR
                    setBits             ( fRegs.OPTCR                ,                               0x00000002L ); // Write OPTSTRT
                    rdBusLoopWhileCmpNEQ( fRegs.OPTSR                , 0x00000001L                 , 0x00000000L ); // Wait  OPT_BUSY   // ##### !!! TODO : Use timeout !!! #####
                    rdBusErrIfCmpEQ     ( fRegs.OPTCCR               , 0x40000000L                 , 0x40000000L ); // Check CLR_OPTCHANGEERR
                    setBits             ( fRegs.OPTCR                ,                               0x00000001L ); // Write OPTLOCK
                    //*/
                    // Clear all flags
                label                   ( "flb_not_dirty"                                                        );
                    appendPrelinkedInst ( _fl_stm32fxx_instruction_ClearAllFLBFlags                              );

        }}.link();
    }

    protected static long[][] _fl_stm32h7x_instruction_ReadFLB(final STM32H7X_FRegs fRegs, final SWDExecInst swdExecInst)
    {
        /*
         * Please refer to the 'Fuses_and_LockBits.txt' file for the full layout details.
         */

        final XVI xvi_Tmp0     = _stm32h7x_xvi_TMP_0;
        final XVI xvi_StoreIdx = _stm32h7x_xvi_TMP_1;
        final XVI xvi_SPECn    = _stm32h7x_xvi_TMP_2;

        return new SWDExecInstBuilder() {{
                    // Simpy exit if the FLB data was already read
                    jmpIfNotZero        ( _fl_stm32fxx_xvi_FLB_DoneRead, "flb_done_read"                                       );
                    // Set the store index to one
                    mov                 ( 1                            , xvi_StoreIdx                                          );
                    // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses.
            for(int i = 0; i < fRegs.OPT_SPEC.length; ++i) {
                    rdsBits             ( fRegs.OPT_SPEC[i]            , xvi_SPECn                                             );
                    /*
                    debugPrintlnUHex08  ( xvi_SPECn                                                                            );
                    //*/
                if(i == fRegs.RDP_index) {
                    // Extract the RDP as lock bits
                    bwRSH               ( xvi_SPECn                    , fRegs.RDP_shift               , xvi_Tmp0              );
                    bwAND               ( xvi_Tmp0                     , 0x000000FFL                   , xvi_Tmp0              );
                    strDB               ( xvi_Tmp0                     , 0                                                     );
                    // Extract the rest as fuse
                    _stm32h7x_putInstXCV( this, swdExecInst, xvi_SPECn , fRegs.OPT_SPEC_mask[i]       , xvi_StoreIdx, xvi_Tmp0 );
                }
                else {
                    // Extract the as fuse
                    _stm32h7x_putInstXCV( this, swdExecInst, xvi_SPECn , fRegs.OPT_SPEC_mask[i]       , xvi_StoreIdx, xvi_Tmp0 );
                }
            } // for
                    // Set flag
                    mov                 ( 1                            , _fl_stm32fxx_xvi_FLB_DoneRead                         );
                label                   ( "flb_done_read"                                                                      );
        }}.link();
    }

    public static Specifier _getSpecifier_stm32h7x_impl(final ProgSWD.Config config, final SWDExecInst swdExecInst, final boolean varAB, final STM32H7X_FRegs fRegs)
    {
        // Prepare the instruction builder
        final SWDExecInstBuilder ib = new SWDExecInstBuilder();

        // Generate the instructions to check if the flash memory is locked
        {
            ib.rdBusRetCmpEQ       ( fRegs.CR  , 0x00000001L, 0x00000001L ); // Read  LOCK
        }

        final long[][] instructionIsFlashLocked = ib.link();

        // Generate the instructions to unlock the flash memory
        {
            ib.rdBusLoopWhileCmpNEQ( fRegs.SR  , 0x00000004L, 0x00000000L ); // Wait  QW                      // ##### !!! TODO : Use timeout !!! #####
            ib.wrBus               ( fRegs.KEYR, fRegs.Key1               ); // Write KEY1
            ib.wrBus               ( fRegs.KEYR, fRegs.Key2               ); // Write KEY2
            ib.rdBusLoopWhileCmpNEQ( fRegs.SR  , 0x00000004L, 0x00000000L ); // Wait  QW                      // ##### !!! TODO : Use timeout !!! #####
        }

        final long[][] instruction_UnlockFlash = ib.link();

        // Generate the instructions to erase the entire flash memory
        final long flash_cr_val1 = _stm32h7x_flash_cr(varAB, 0x00000038L);
        final long flash_cr_val2 = _stm32h7x_flash_cr(varAB, 0x000000B8L);
        {
            ib.rdBusLoopWhileCmpNEQ( fRegs.SR, 0x00000004L  , 0x00000000L ); // Wait  QW                      // ##### !!! TODO : Use timeout !!! #####
            ib.wrBus               ( fRegs.CR, flash_cr_val1              ); // Write PSIZE and BER
            ib.wrBus               ( fRegs.CR, flash_cr_val2              ); // Write PSIZE and BER and START
            ib.rdBusLoopWhileCmpNEQ( fRegs.SR, 0x00000004L  , 0x00000000L ); // Wait  QW                      // ##### !!! TODO : Use timeout !!! #####
        }

        final long[][] instruction_EraseAllFlashPages = ib.link();

        // Generate the instructions to write and read the option bytes
        final boolean  noFLB                = (fRegs == null || fRegs.OPT_SPEC == null || fRegs.OPT_SPEC_mask == null);

        final long[][] instruction_WriteFLB = noFLB ? null : _fl_stm32h7x_instruction_WriteFLB(fRegs, swdExecInst);
        final long[][] instruction_ReadFLB  = noFLB ? null : _fl_stm32h7x_instruction_ReadFLB (fRegs, swdExecInst);

        // Instantiate and return the specifier
        return new Specifier(

            0xFF                                                                 , // flashMemoryEmptyValue
            _stm32h7x_wrMaxSWDTransferSize                                       , // wrMaxSWDTransferSize
            _stm32h7x_rdMaxSWDTransferSize                                       , // rdMaxSWDTransferSize
            false                                                                , // supportDirectFlashRead
            false                                                                , // supportDirectEEPROMRead

            _fl_stm32fxx_instruction_ClearAllFLBFlags                            , // instruction_InitializeSystemOnce
            null                                                                 , // instruction_UninitializeSystemExit
            instructionIsFlashLocked                                             , // instruction_IsFlashLocked
            instruction_UnlockFlash                                              , // instruction_UnlockFlash
            instruction_EraseAllFlashPages                                       , // instruction_EraseFlash
            null                                                                 , // instruction_EraseFlashPages
            null                                                                 , // instruction_WriteFlash
            null                                                                 , // instruction_ReadFlash
            null                                                                 , // instruction_IsEEPROMLocked
            null                                                                 , // instruction_UnlockEEPROM
            null                                                                 , // instruction_WriteEEPROM
            null                                                                 , // instruction_ReadEEPROM
            XVI._NA_                                                             , // instruction_xviFlashEEPROMAddress
            XVI._NA_                                                             , // instruction_xviFlashEEPROMReadSize
            XVI._NA_                                                             , // instruction_xviSignalWorkerCommand
            XVI._NA_                                                             , // instruction_xviSignalJobState
            null                                                                 , // instruction_dataBuffFlash
            null                                                                 , // instruction_dataBuffEEPROM

            varAB ? _fl_stm32h7_ab_x_flProgram : _fl_stm32h7_2345_x_flProgram    , // flProgram
            null                                                                 , // elProgram
            config.memorySRAM.address                                            , // addrProgStart
            config.memorySRAM.address + _stm32h7x_PROG_AREA_SIZE + 128           , // addrProgBuffer
            config.memorySRAM.address + _stm32h7x_PROG_AREA_SIZE                 , // addrProgSignal
            new long[] { fRegs.KEYR, fRegs.Key1, fRegs.Key2, fRegs.CR, fRegs.SR }, // wrProgExtraParams
            null                                                                 , // rdProgExtraParams

            noFLB ? null     : instruction_WriteFLB                              , // instruction_WriteFLB
            noFLB ? null     : instruction_ReadFLB                               , // instruction_ReadFLB
            noFLB ? XVI._NA_ : _fl_stm32fxx_xvi_FLB_DoneRead                     , // instruction_xviFLB_DoneRead
            noFLB ? XVI._NA_ : _fl_stm32fxx_xvi_FLB_FDirty                       , // instruction_xviFLB_FDirty
            noFLB ? XVI._NA_ : _fl_stm32fxx_xvi_FLB_LBDirty                      , // instruction_xviFLB_LBDirty
            noFLB ? null     : new int[1 + fRegs.OPT_SPEC.length]                  // instruction_dataBuffFLB

        );
    }

    // STM32H7[2|3]X
    public static Specifier _getSpecifier_stm32h72x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        return _getSpecifier_stm32h7x_impl(config, swdExecInst, false, new STM32H7X_FRegs(
            _stm32h7x_flashRegBaseB0,
            //           OPTSR        BOOT         PRAR         SCAR         WPSN
            new int [] { 0x020      , 0x044      , 0x02C      , 0x034      , 0x03C       },
            new long[] { 0x203E00DCL, 0xFFFFFFFFL, 0x8FFF0FFFL, 0x8FFF0FFFL, 0x000000FFL },
            0,
            8
        ) );
    }

    public static Specifier _getSpecifier_stm32h73x(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32h72x(config, swdExecInst); }

    // STM32H7[4|5]X - Bank 0
    // ##### !!! TODO : How to make it automatic between bank 0 and bank 1 (just like the 'atsam3x' driver)? !!! #####
    public static Specifier _getSpecifier_stm32h74x_bank0(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        return _getSpecifier_stm32h7x_impl(config, swdExecInst, false, new STM32H7X_FRegs(
            _stm32h7x_flashRegBaseB0,
            //           OPTSR        BOOT/BOOT7   BOOT4        PRAR1        SCAR1        WPSN1
            new int [] { 0x020      , 0x044      , 0x04C      , 0x02C      , 0x034      , 0x03C       },
            new long[] { 0xA3FE00FCL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0x8FFF0FFFL, 0x8FFF0FFFL, 0x000000FFL },
            0,
            8
        ) );
    }

    public static Specifier _getSpecifier_stm32h75x_bank0(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32h74x_bank0(config, swdExecInst); }

    // STM32H7[4|5]X - Bank 1
    // ##### !!! TODO : How to make it automatic between bank 0 and bank 1 (just like the 'atsam3x' driver)? !!! #####
    public static Specifier _getSpecifier_stm32h74x_bank1(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        return _getSpecifier_stm32h7x_impl(config, swdExecInst, false, new STM32H7X_FRegs(
            _stm32h7x_flashRegBaseB1,
            //           OPTSR        BOOT/BOOT7   BOOT4        PRAR2        SCAR2        WPSN2
            new int [] { 0x020      , 0x044      , 0x04C      , 0x02C      , 0x034      , 0x03C       },
            new long[] { 0xA3FE00FCL, 0xFFFFFFFFL, 0xFFFFFFFFL, 0x8FFF0FFFL, 0x8FFF0FFFL, 0x000000FFL },
            0,
            8
        ) );
    }

    public static Specifier _getSpecifier_stm32h75x_bank1(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32h74x_bank1(config, swdExecInst); }

    // STM32H7[A|B]X - Bank 0
    // ##### !!! TODO : How to make it automatic between bank 0 and bank 1 (just like the 'atsam3x' driver)? !!! #####
    public static Specifier _getSpecifier_stm32h7ax_bank0(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        return _getSpecifier_stm32h7x_impl(config, swdExecInst, true , new STM32H7X_FRegs(
            _stm32h7x_flashRegBaseB0,
            //           OPTSR        BOOT         OTPBL        PRAR1        SCAR1        WPSGN1
            new int [] { 0x020      , 0x044      , 0x06C      , 0x02C      , 0x034      , 0x03C       },
            new long[] { 0xA03F00DCL, 0xFFFFFFFFL, 0x0000FFFFL, 0x8FFF0FFFL, 0x8FFF0FFFL, 0xFFFFFFFFL },
            0,
            8
        ) );
    }

    public static Specifier _getSpecifier_stm32h7bx_bank0(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32h7ax_bank0(config, swdExecInst); }

    // STM32H7[A|B]X - Bank 1
    // ##### !!! TODO : How to make it automatic between bank 0 and bank 1 (just like the 'atsam3x' driver)? !!! #####
    public static Specifier _getSpecifier_stm32h7ax_bank1(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    {
        return _getSpecifier_stm32h7x_impl(config, swdExecInst, true , new STM32H7X_FRegs(
            _stm32h7x_flashRegBaseB1,
            //           OPTSR        BOOT         OTPBL        PRAR2        SCAR2        WPSGN2
            new int [] { 0x020      , 0x044      , 0x06C      , 0x02C      , 0x034      , 0x03C       },
            new long[] { 0xA03F00DCL, 0xFFFFFFFFL, 0x0000FFFFL, 0x8FFF0FFFL, 0x8FFF0FFFL, 0xFFFFFFFFL },
            0,
            8
        ) );
    }

    public static Specifier _getSpecifier_stm32h7bx_bank1(final ProgSWD.Config config, final SWDExecInst swdExecInst)
    { return _getSpecifier_stm32h7ax_bank1(config, swdExecInst); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * ##### !!! TODO : Add support for more MCUs! !!! #####
     */

} // class SWDFlashLoaderSTM32
