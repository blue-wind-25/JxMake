/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.stm32xf;


import jxm.*;
import jxm.ugc.*;
import jxm.ugc.fl.*;
import jxm.xb.*;

import static jxm.ugc.SWDExecInstOpcode.XVI;
import static jxm.xb.XCom._BV;
import static jxm.xb.XCom._BVs;


/*
 * Please refer to the comment block before the 'STM32QSPI' class definition in the 'STM32QSPI.java' file for more details and information.
 *
 * NOTE : This class should also works for STM32H742, STM32H743, and STM32H753.
 */
public class STM32QSPI_H750 extends STM32QSPI {

    protected static final String QSPIClassName = "STM32QSPI_H750";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * STM32H7XX M
     * https://stm32-base.org/boards/STM32H750VBT6-STM32H7XX-M.html
     * https://stm32-base.org/assets/pdf/boards/original-schematic-STM32H7XX-M.pdf
     *
     *     STM32                 W25Q64FV
     *     PB02 (QSPI_CLK    )   CLK   (CLK)
     *     PB06 (QSPI_BK1_NCS)   nCS   (nCS)
     *     PD11 (QSPI_BK1_IO0)   DI    (IO0)
     *     PD12 (QSPI_BK1_IO1)   DO    (IO1)
     *     PE02 (QSPI_BK1_IO2)   nWP   (IO2)
     *     PD13 (QSPI_BK1_IO3)   nHOLD (IO3)
     *
     * NOTE : The pin assignment below should also be usable for other boards.
     */
    public static class StdSTM32QSPIPin extends STM32QSPIPin {
        @Override protected STM32GPIO _gpio_CLK() { return STM32GPIO.B( 2, 0x09); }
        @Override protected STM32GPIO _gpio_NCS() { return STM32GPIO.B( 6, 0x0A); }
        @Override protected STM32GPIO _gpio_IO0() { return STM32GPIO.D(11, 0x09); }
        @Override protected STM32GPIO _gpio_IO1() { return STM32GPIO.D(12, 0x09); }
        @Override protected STM32GPIO _gpio_IO2() { return STM32GPIO.E( 2, 0x09); }
        @Override protected STM32GPIO _gpio_IO3() { return STM32GPIO.D(13, 0x09); }
    }

    public static class QSPICmd extends QSPICmd_Common {

        @Override
        public long funcModeBits() throws JXMFatalLogicError
        {
            switch(funcMode) {
                case _IndirectWrite : return 0x00000000L;
                case _IndirectRead  : return 0x04000000L;
                case _AutoPolling   : return 0x08000000L;
                case _MemoryMapped  : return 0x0C000000L;
                default             : throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvFuncMode, QSPIClassName);
            }
        }
        @Override
        public long instModeBits() throws JXMFatalLogicError
        {
            switch(instMode) {
                case _None   : return 0x00000000L;
                case _1Line  : return 0x00000100L;
                case _2Lines : return 0x00000200L;
                case _4Lines : return 0x00000300L;
                default      : throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvInstMode, QSPIClassName);
            }
        }

        @Override
        public long instSIOOBits()
        { return instSIOO ? 0x10000000L : 0; }

        @Override
        public long addrModeBits() throws JXMFatalLogicError
        {
            switch(addrMode) {
                case _None   : return 0x00000000L;
                case _1Line  : return 0x00000400L;
                case _2Lines : return 0x00000800L;
                case _4Lines : return 0x00000C00L;
                default      : throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvAddrMode, QSPIClassName);
            }
        }

        @Override
        public long addrSizeBits() throws JXMFatalLogicError
        {
            switch(addrSize) {
                case _None   : return 0x00000000L;
                case _08Bits : return 0x00000000L;
                case _16Bits : return 0x00001000L;
                case _24Bits : return 0x00002000L;
                case _32Bits : return 0x00003000L;
                default      : throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvAddrSize, QSPIClassName);
            }
        }

        @Override
        public long altbModeBits() throws JXMFatalLogicError
        {
            switch(altbMode) {
                case _None   : return 0x00000000L;
                case _1Line  : return 0x00004000L;
                case _2Lines : return 0x00008000L;
                case _4Lines : return 0x0000C000L;
                default      : throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvAltBMode, QSPIClassName);
            }
        }

        @Override
        public long altbSizeBits() throws JXMFatalLogicError
        {
            switch(altbSize) {
                case _None   : return 0x00000000L;
                case _08Bits : return 0x00000000L;
                case _16Bits : return 0x00010000L;
                case _24Bits : return 0x00020000L;
                case _32Bits : return 0x00030000L;
                default      : throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvAltBSize, QSPIClassName);
            }
        }

        @Override
        public long dataModeBits() throws JXMFatalLogicError
        {
            switch(dataMode) {
                case _None   : return 0x00000000L;
                case _1Line  : return 0x01000000L;
                case _2Lines : return 0x02000000L;
                case _4Lines : return 0x03000000L;
                default      : throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvDataMode, QSPIClassName);
            }
        }

        @Override
        public long dmyCycleBits()
        { return dummyCycles << 18; }

        @Override
        public long ddrModeBits()
        { return ddrMode ? 0x80000000L : 0; }

        @Override
        public long ddrHoldBits()
        { return ddrHoldHalfCycle ? 0x40000000L : 0; }

    } // class QSPICmd

    @Override
    public QSPICmd_Common newQSPICmd()
    { return new QSPICmd(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final long   FLASH_BASE         =                          0x08000000L;
    protected static final long   D1_DTCMRAM_BASE    =                          ProgSWD.DefaultCortexM_SRAMStart;
    protected static final long   PERIPH_BASE        =                          0x40000000L;
    protected static final long   QSPI_BASE          =                          0x90000000L;

    protected static final long   D1_APB1PERIPH_BASE =     PERIPH_BASE        + 0x10000000L;
    protected static final long   D1_AHB1PERIPH_BASE =     PERIPH_BASE        + 0x12000000L;

    protected static final long   D2_APB1PERIPH_BASE =     PERIPH_BASE        + 0x00000000L;
    protected static final long   D2_APB2PERIPH_BASE =     PERIPH_BASE        + 0x00010000L;
    protected static final long   D2_AHB1PERIPH_BASE =     PERIPH_BASE        + 0x00020000L;
    protected static final long   D2_AHB2PERIPH_BASE =     PERIPH_BASE        + 0x08020000L;

    protected static final long   D3_APB1PERIPH_BASE =     PERIPH_BASE        + 0x18000000L;
    protected static final long   D3_AHB1PERIPH_BASE =     PERIPH_BASE        + 0x18020000L;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final long   FLASH_R_BASE       =     D1_AHB1PERIPH_BASE + 0x00002000L;
    protected static final long   FLASH_ACR          =     FLASH_R_BASE       + 0x00000000L;

    protected static final long   QSPI_R_BASE        =     D1_AHB1PERIPH_BASE + 0x00005000L;
    protected static final long   QSPI_CR            =     QSPI_R_BASE        + 0x00000000L;
    protected static final long   QSPI_DCR           =     QSPI_R_BASE        + 0x00000004L;
    protected static final long   QSPI_SR            =     QSPI_R_BASE        + 0x00000008L;
    protected static final long   QSPI_FCR           =     QSPI_R_BASE        + 0x0000000CL;
    protected static final long   QSPI_DLR           =     QSPI_R_BASE        + 0x00000010L;
    protected static final long   QSPI_CCR           =     QSPI_R_BASE        + 0x00000014L;
    protected static final long   QSPI_AR            =     QSPI_R_BASE        + 0x00000018L;
    protected static final long   QSPI_ABR           =     QSPI_R_BASE        + 0x0000001CL;
    protected static final long   QSPI_DR            =     QSPI_R_BASE        + 0x00000020L;
    protected static final long   QSPI_PSMKR         =     QSPI_R_BASE        + 0x00000024L;
    protected static final long   QSPI_PSMAR         =     QSPI_R_BASE        + 0x00000028L;
    protected static final long   QSPI_PIR           =     QSPI_R_BASE        + 0x0000002CL;
    protected static final long   QSPI_LPTR          =     QSPI_R_BASE        + 0x00000030L;

    protected static final long   SYSCFG_BASE        =     D3_APB1PERIPH_BASE + 0x00000400L;
    protected static final long   SYSCFG_PWRCR       =     SYSCFG_BASE        + 0x0000002CL;

    protected static final long[] GPIOx_BASE         = new long[] {
                                                           D3_AHB1PERIPH_BASE + 0x00000000L, // A
                                                           D3_AHB1PERIPH_BASE + 0x00000400L, // B
                                                           D3_AHB1PERIPH_BASE + 0x00000800L, // C
                                                           D3_AHB1PERIPH_BASE + 0x00000C00L, // D
                                                           D3_AHB1PERIPH_BASE + 0x00001000L, // E
                                                           D3_AHB1PERIPH_BASE + 0x00001400L, // F
                                                           D3_AHB1PERIPH_BASE + 0x00001800L, // G
                                                           D3_AHB1PERIPH_BASE + 0x00001C00L, // H
                                                           D3_AHB1PERIPH_BASE + 0x00002000L, // I
                                                           D3_AHB1PERIPH_BASE + 0x00002400L, // J
                                                           D3_AHB1PERIPH_BASE + 0x00002800L  // K
                                                       };

    protected static final long   RCC_BASE           =     D3_AHB1PERIPH_BASE + 0x00004400L;
    protected static final long   RCC_CR             =     RCC_BASE           + 0x00000000L;
    protected static final long   RCC_CFGR           =     RCC_BASE           + 0x00000010L;
    protected static final long   RCC_D1CFGR         =     RCC_BASE           + 0x00000018L;
    protected static final long   RCC_D2CFGR         =     RCC_BASE           + 0x0000001CL;
    protected static final long   RCC_D3CFGR         =     RCC_BASE           + 0x00000020L;
    protected static final long   RCC_PLLCKSELR      =     RCC_BASE           + 0x00000028L;
    protected static final long   RCC_PLLCFGR        =     RCC_BASE           + 0x0000002CL;
    protected static final long   RCC_PLL1DIVR       =     RCC_BASE           + 0x00000030L;
    protected static final long   RCC_D1CCIPR        =     RCC_BASE           + 0x0000004CL;
    protected static final long   RCC_AHB3ENR        =     RCC_BASE           + 0x000000D4L;
    protected static final long   RCC_AHB4ENR        =     RCC_BASE           + 0x000000E0L;

    protected static final long   PWR_BASE           =     D3_AHB1PERIPH_BASE + 0x00004800L;
    protected static final long   PWR_CSR1           =     PWR_BASE           + 0x00000004L;
    protected static final long   PWR_CR3            =     PWR_BASE           + 0x0000000CL;
    protected static final long   PWR_D3CR           =     PWR_BASE           + 0x00000018L;


    protected static final long   MDMA_MTSIZE        =     32;
    protected static final long   MDMA_BASE          =     D1_AHB1PERIPH_BASE + 0x00000000L;
    protected static final long[] MDMAn_BASE         = new long[] {
                                                           MDMA_BASE + 0x00000040L, //  0
                                                           MDMA_BASE + 0x00000080L, //  1
                                                           MDMA_BASE + 0x000000C0L, //  2
                                                           MDMA_BASE + 0x00000100L, //  3
                                                           MDMA_BASE + 0x00000140L, //  4
                                                           MDMA_BASE + 0x00000180L, //  5
                                                           MDMA_BASE + 0x000001C0L, //  6
                                                           MDMA_BASE + 0x00000200L, //  7
                                                           MDMA_BASE + 0x00000240L, //  8
                                                           MDMA_BASE + 0x00000280L, //  9
                                                           MDMA_BASE + 0x000002C0L, // 10
                                                           MDMA_BASE + 0x00000300L, // 11
                                                           MDMA_BASE + 0x00000340L, // 12
                                                           MDMA_BASE + 0x00000380L, // 13
                                                           MDMA_BASE + 0x000003C0L, // 14
                                                           MDMA_BASE + 0x00000400L  // 15
                                                       };

    protected static long MDMAn_CISR  (final int n) { return MDMAn_BASE[n] + 0x40; }
    protected static long MDMAn_CIFCR (final int n) { return MDMAn_BASE[n] + 0x44; }
    protected static long MDMAn_CESR  (final int n) { return MDMAn_BASE[n] + 0x48; }
    protected static long MDMAn_CCR   (final int n) { return MDMAn_BASE[n] + 0x4C; }
    protected static long MDMAn_CTCR  (final int n) { return MDMAn_BASE[n] + 0x50; }
    protected static long MDMAn_CBNDTR(final int n) { return MDMAn_BASE[n] + 0x54; }
    protected static long MDMAn_CSAR  (final int n) { return MDMAn_BASE[n] + 0x58; }
    protected static long MDMAn_CDAR  (final int n) { return MDMAn_BASE[n] + 0x5C; }
    protected static long MDMAn_CBRUR (final int n) { return MDMAn_BASE[n] + 0x60; }
    protected static long MDMAn_CLAR  (final int n) { return MDMAn_BASE[n] + 0x64; }
    protected static long MDMAn_CTBR  (final int n) { return MDMAn_BASE[n] + 0x68; }
    protected static long MDMAn_CMAR  (final int n) { return MDMAn_BASE[n] + 0x70; }
    protected static long MDMAn_CMDR  (final int n) { return MDMAn_BASE[n] + 0x74; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected long GPIOx_MODER  (final int gpio               ) { return GPIOx_BASE[gpio] + 0x00;                         }
    protected long GPIOx_OTYPER (final int gpio               ) { return GPIOx_BASE[gpio] + 0x04;                         }
    protected long GPIOx_OSPEEDR(final int gpio               ) { return GPIOx_BASE[gpio] + 0x08;                         }
    protected long GPIOx_PUPDR  (final int gpio               ) { return GPIOx_BASE[gpio] + 0x0C;                         }
    protected long GPIOx_IDR    (final int gpio               ) { return GPIOx_BASE[gpio] + 0x10;                         }
    protected long GPIOx_ODR    (final int gpio               ) { return GPIOx_BASE[gpio] + 0x14;                         }
    protected long GPIOx_BSRR   (final int gpio               ) { return GPIOx_BASE[gpio] + 0x18;                         }
    protected long GPIOx_LCKR   (final int gpio               ) { return GPIOx_BASE[gpio] + 0x1C;                         }
    protected long GPIOx_AFR    (final int gpio, final int pin) { return GPIOx_BASE[gpio] + ( (pin <= 7) ? 0x20 : 0x24 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _put_instruction_initGPIO_QSPI(final STM32GPIO gpio)
    {
        final int pin1 =  gpio.pin            ;
        final int pin2 =  gpio.pin         * 2;
        final int pin4 = (gpio.pin & 0x07) * 4;
        final int afn  =  gpio.afn;

        _ib.setBits( GPIOx_OSPEEDR(gpio.gpio          ),                _BV(  3, pin2) ); // Set   OSPEEDRx to 3   (IO speed              = very high         )
        _ib.clrBits( GPIOx_PUPDR  (gpio.gpio          ), _BV( 3, pin2)                 ); // Clear PUPDRx          (IO pull-up/down       = disabled          )
        _ib.clrBits( GPIOx_OTYPER (gpio.gpio          ), _BV(    pin1)                 ); // Clear OTx             (IO output type        = push-pull         )
        _ib.modBits( GPIOx_AFR    (gpio.gpio, gpio.pin), _BV(15, pin4), _BV(afn, pin4) ); // Set   AFRx     to AFn (IO alternate function = QUADSPI           )
        _ib.modBits( GPIOx_MODER  (gpio.gpio          ), _BV( 3, pin2), _BV(  2, pin2) ); // Set   MODERx   to 2   (IO direction mode     = alternate function)
    }

    private void _put_instruction_deinitGPIO(final STM32GPIO gpio)
    {
        final int pin1 =  gpio.pin            ;
        final int pin2 =  gpio.pin         * 2;
        final int pin4 = (gpio.pin & 0x07) * 4;
        final int afn  =  gpio.afn;

        _ib.clrBits( GPIOx_MODER  (gpio.gpio          ), _BV( 3, pin2) ); // Clear MODERx   (IO direction mode     = input    )
        _ib.clrBits( GPIOx_AFR    (gpio.gpio, gpio.pin), _BV(15, pin4) ); // Clear AFRx     (IO alternate function = 0        )
        _ib.clrBits( GPIOx_OTYPER (gpio.gpio          ), _BV(    pin1) ); // Clear OTx      (IO output type        = push-pull)
        _ib.clrBits( GPIOx_PUPDR  (gpio.gpio          ), _BV( 3, pin2) ); // Clear PUPDRx   (IO pull-up/down       = disabled )
        _ib.clrBits( GPIOx_OSPEEDR(gpio.gpio          ), _BV( 3, pin2) ); // Clear OSPEEDRx (IO speed              = low      )
    }

    public void _put_instruction_qspiConfig(final QSPICmd_Common cmd) throws JXMFatalLogicError
    {

        if(cmd.dataMode != DataMode._None && cmd.funcMode != FuncMode._MemoryMapped) {
                        if( XVI.isXVIEnc(cmd.dataLength) ) {
                            final XVI xviDLen = _ib.xviInternal();
                            _ib.sub    ( XVI.xviDec(cmd.dataLength), 1      , xviDLen                  );
                            _ib.wrtBits( QSPI_DLR                  , xviDLen                           );
                        }
                        else {
                            // Configure the DLR register with the number of data to read or write
                            _ib.wrtBits( QSPI_DLR , cmd.dataLength - 1                                 );
                        }
        }

        if(cmd.instMode != InstMode._None) {
            if(cmd.altbMode != AltBMode._None) {
                            // Configure the ABR register with alternate bytes value
                            _ib.wrtBits( QSPI_ABR , cmd.altbValue                                      );
                if(cmd.addrMode != AddrMode._None) {
                            // Configure the CCR register with all the required communications parameters
                            _ib.wrtBits( QSPI_CCR , cmd.allConfigBits()                                );
                    if(cmd.funcMode != FuncMode._MemoryMapped) {
                        if( XVI.isXVIEnc(cmd.addrValue) ) {
                            // Configure the AR register with address value
                            _ib.wrtBits( QSPI_AR  , XVI.xviDec(cmd.addrValue)                          );
                        }
                        else {
                            // Configure the AR register with address value
                            _ib.wrtBits( QSPI_AR  , cmd.addrValue                                      );
                        }
                    }
                }
                else {
                            // Configure the CCR register with all the required communications parameters
                            _ib.wrtBits( QSPI_CCR , cmd.allConfigBits_wo_addrSize()                    );
                }
            }
            else {
                if(cmd.addrMode != AddrMode._None) {
                            // Configure the CCR register with all the required communications parameters
                            _ib.wrtBits( QSPI_CCR , cmd.allConfigBits_wo_altbSize()                    );
                    if(cmd.funcMode != FuncMode._MemoryMapped) {
                        if( XVI.isXVIEnc(cmd.addrValue) ) {
                            // Configure the AR register with address value
                            _ib.wrtBits( QSPI_AR  , XVI.xviDec(cmd.addrValue)                          );
                        }
                        else {
                            // Configure the AR register with address value
                            _ib.wrtBits( QSPI_AR  , cmd.addrValue                                      );
                        }
                    }
                }
                else {
                            // Configure the CCR register with all the required communications parameters
                            _ib.wrtBits( QSPI_CCR , cmd.allConfigBits_wo_addrSize_altbSize()           );
                }
            }
        }

        else {
            if(cmd.altbMode != AltBMode._None) {
                            // Configure the ABR register with alternate bytes value
                            _ib.wrtBits( QSPI_ABR , cmd.altbValue                                      );
                if(cmd.addrMode != AddrMode._None) {
                            // Configure the CCR register with all the required communications parameters
                            _ib.wrtBits( QSPI_CCR , cmd.allConfigBits_wo_instValue()                   );
                    if(cmd.funcMode != FuncMode._MemoryMapped) {
                        if( XVI.isXVIEnc(cmd.addrValue) ) {
                            // Configure the AR register with address value
                            _ib.wrtBits( QSPI_AR  , XVI.xviDec(cmd.addrValue)                          );
                        }
                        else {
                            // Configure the AR register with address value
                            _ib.wrtBits( QSPI_AR  , cmd.addrValue                                      );
                        }
                    }
                }
                else {
                            // Configure the CCR register with all the required communications parameters
                            _ib.wrtBits( QSPI_CCR , cmd.allConfigBits_wo_instValue_addrSize()          );
                }
            }
            else {
                if(cmd.addrMode != AddrMode._None) {
                            // Configure the CCR register with all the required communications parameters
                            _ib.wrtBits( QSPI_CCR , cmd.allConfigBits_wo_instValue_altbSize()          );
                    if(cmd.funcMode != FuncMode._MemoryMapped) {
                        if( XVI.isXVIEnc(cmd.addrValue) ) {
                            // Configure the AR register with address value
                            _ib.wrtBits( QSPI_AR  , XVI.xviDec(cmd.addrValue)                          );
                        }
                        else {
                            // Configure the AR register with address value
                            _ib.wrtBits( QSPI_AR  , cmd.addrValue                                      );
                        }
                    }
                }
                else {
                    if(cmd.dataMode != DataMode._None) {
                            // Configure the CCR register with all the required communications parameters
                            _ib.wrtBits( QSPI_CCR , cmd.allConfigBits_wo_instValue_addrSize_altbSize() );
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : It seems that if the transfer size is too large, the data read/written using SWD will be corrupted !!! #####
    private static final int MaxSWDWrSize = SWDFlashLoaderSTM32._stm32h7x_wrMaxSWDTransferSize;
    private static final int MaxSWDRdSize = SWDFlashLoaderSTM32._stm32h7x_rdMaxSWDTransferSize;

    public STM32QSPI_H750(final SWDExecInst swdExecInst, final STM32QSPIPin qspiPin, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped)
    { super(swdExecInst, qspiPin, flashCapacityBytes, flashPageSizeBytes, enableMemoryMapped); }

    public STM32QSPI_H750(final SWDExecInst swdExecInst, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped)
    { this( swdExecInst, new StdSTM32QSPIPin(), flashCapacityBytes, flashPageSizeBytes, enableMemoryMapped); }

    public STM32QSPI_H750(final SWDExecInst swdExecInst, final long flashCapacityBytes, final int flashPageSizeBytes)
    { this( swdExecInst, new StdSTM32QSPIPin(), flashCapacityBytes, flashPageSizeBytes, true); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Indirect write mode.
     */
    @Override
    public void geninstruction_qspiCommand(final QSPICmd_Common cmd) throws JXMFatalLogicError
    {
            _ib.rdlBitsUntilUnset      ( QSPI_SR  , _BV(5)   ); // Wait !BUSY   // ##### !!! TODO : Use timeout !!! #####

            _put_instruction_qspiConfig( cmd.indirectWrite() );

        if(cmd.dataMode == DataMode._None) {
            _ib.rdlBitsUntilSet        ( QSPI_SR  , _BV(1)   ); // Wait TCF     // ##### !!! TODO : Use timeout !!! #####
            _ib.wrtBits                ( QSPI_FCR , _BV(1)   ); // Set  CTCF
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Indirect write mode.
     */
    @Override
    public void geninstruction_qspiAbort() throws JXMFatalLogicError
    {
        _ib.setBits          ( QSPI_CR  , _BV(    1) ); // Set  ABORT
        _ib.rdlBitsUntilSet  ( QSPI_SR  , _BV(    1) ); // Wait TCF     // ##### !!! TODO : Use timeout !!! #####
        _ib.setBits          ( QSPI_FCR , _BV(    1) ); // Set  CTCF
        _ib.rdlBitsUntilUnset( QSPI_SR  , _BV(    5) ); // Wait !BUSY   // ##### !!! TODO : Use timeout !!! #####
        _ib.clrBits          ( QSPI_CCR , _BV(3, 26) ); // Clear FMODE
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Memory mapped mode.
     */
    @Override
    public void geninstruction_qspiMemoryMapped(final QSPICmd_Common cmd) throws JXMFatalLogicError
    {
        if(!_enableMemoryMapped) return;

        _ib.rdlBitsUntilUnset      ( QSPI_SR  , _BV(5)  ); // Wait !BUSY   // ##### !!! TODO : Use timeout !!! #####

        _ib.clrBits                ( QSPI_CR  , _BV(3)  ); // Clear TCEN
        _ib.clrBits                ( QSPI_LPTR, 0xFFFF  ); // Clear TIMEOUT

        _put_instruction_qspiConfig( cmd.memoryMapped() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Indirect read mode.
     *        # This instruction writes to DB when 'useSB' is 'false'.
     *        # This instruction writes to SB when 'useSB' is 'true'.
     */
    private XVI _geninstruction_qspiReceive_impl(final XVI xviCurOperInitFlag, final QSPICmd_Common cmdRef, final boolean useSB) throws JXMFatalLogicError
    {
        // NOTE : The parameter 'xviCurOperInitFlag' is not used by this function

        final XVI xviAddr = _ib.xviInternal();
        final XVI xviRCnt = _ib.xviInternal();
        final XVI xviDIdx = _ib.xviInternal();
        final XVI xviDId4 = _ib.xviInternal();

        final String lblLoop = _ib.uniqueLabelCounter("qspi_receive_loop_%=");

            // Read the address
            _ib.rdsBits        ( QSPI_AR          , xviAddr                );

            // Read the data length
        if(cmdRef == null)  {
            _ib.rdsBits        ( QSPI_DLR         , xviRCnt                );
            _ib.inc1           (                    xviRCnt                );
        }
        else {
            _ib.mov            ( cmdRef.dataLength, xviRCnt                );
        }

            // Configure the CCR register with functional as indirect read
            _ib.modBits        ( QSPI_CCR         , _BV(3, 26), _BV(1, 26) ); // Set  FMODE to 0b01

            // Start the transfer by re-writing the address in AR register
            _ib.wrtBits        ( QSPI_AR          , xviAddr                );

            // Read the bytes
            _ib.mov            ( 0                , xviDIdx                );
        _ib.label              ( lblLoop                                   );
          //_ib.rdlBitsUntilSet( QSPI_SR          , _BVs(3, 1)             ); // Wait FTF | TCF   // ##### !!! TODO : Use timeout !!! #####
            _ib.rdlBitsUntilSet( QSPI_SR          , _BV(    2)             ); // Wait FTF         // ##### !!! TODO : Use timeout !!! #####
            _ib.div            ( xviDIdx          , 4         , xviDId4    );
        if(!useSB) {
            _ib.rdsBitsDB      ( QSPI_DR          , xviDId4                );
        }
        else {
            _ib.rdsBitsSB      ( QSPI_DR          , xviDId4                );
        }
            _ib.inc4           ( xviDIdx                                   );
            _ib.jmpIfLT        ( xviDIdx          , xviRCnt   , lblLoop    );

        return xviDIdx;
    }

    /*
     * NOTE : # Indirect read mode.
     *        # This instruction writes to DB!
     */
    @Override
    public XVI geninstruction_qspiReceiveDB(final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    { return _geninstruction_qspiReceive_impl(xviCurOperInitFlag, null, false); }

    /*
     * NOTE : # Indirect read mode.
     *        # This instruction writes to SB!
     */
    @Override
    public XVI geninstruction_qspiReceiveSB(final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    { return _geninstruction_qspiReceive_impl(xviCurOperInitFlag, null, true ); }

    /*
     * NOTE : # Indirect read mode.
     *        # This instruction writes to DB!
     */
    @Override
    public XVI geninstruction_qspiReceiveDB(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    {
               geninstruction_qspiCommand      (cmd                           );
        return _geninstruction_qspiReceive_impl(xviCurOperInitFlag, cmd, false);
    }

    /*
     * NOTE : # Indirect read mode.
     *        # This instruction writes to SB!
     */
    @Override
    public XVI geninstruction_qspiReceiveSB(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    {
               geninstruction_qspiCommand      (cmd                           );
        return _geninstruction_qspiReceive_impl(xviCurOperInitFlag, cmd, true );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Indirect read mode using MDMA.
     *        # This instruction writes to DB!
     */
    private XVI _geninstruction_qspiReceiveDMA_impl(final XVI xviCurOperInitFlag, final QSPICmd_Common cmdRef) throws JXMFatalLogicError
    {
        final XVI xviFlag = xviCurOperInitFlag;

        final XVI xviAddr = _ib.xviInternal();
        final XVI xviRCnt = _ib.xviInternal();
        final XVI xviDIdx = _ib.xviInternal();

        final String lblSkip = "qspi_receive_dma_skip_init_%=";
        final String lblSkp0 = _ib.uniqueLabelName   (lblSkip                   );
        final String lblSkp1 = _ib.uniqueLabelName   (lblSkip                   );
        final String lblSkp2 = _ib.uniqueLabelName   (lblSkip                   );
        final String lblLoop = _ib.uniqueLabelCounter("qspi_receive_dma_loop_%=");

            // Read the address
        if(cmdRef == null)  {
            _ib.rdsBits          ( QSPI_AR          , xviAddr                                       );
        }
        else {
            _ib.add              ( xviAddr          , xviRCnt                     , xviAddr         );
            _ib.jmpIfNotZero     ( xviFlag          , lblSkp0                                       );
            _ib.rdsBits          ( QSPI_AR          , xviAddr                                       );
        _ib.label                ( lblSkp0                                                          );
        }

            // Read the data length
        if(cmdRef == null)  {
            _ib.rdsBits          ( QSPI_DLR         , xviRCnt                                       );
            _ib.inc1             (                    xviRCnt                                       );
        }
        else {
            _ib.mov              ( cmdRef.dataLength, xviRCnt                                       );
        }

            // Configure the CCR register with functional as indirect read
            _ib.modBits          ( QSPI_CCR         , _BV(      3, 26)            , _BV( 1, 26)     ); // Set   FMODE to 0b01
            // Start the transfer by re-writing the address in AR register
            _ib.wrtBits          ( QSPI_AR          , xviAddr                                       );

            // Configure the MDMA transfer - part 1
            _ib.jmpIfNotZero     ( xviFlag          , lblSkp1                                       );
            // Disable MDMA
            _ib.clrBits          ( MDMAn_CCR   (0)  , _BV(          0)                              ); // Clear EN
            // Disable source address increment
            _ib.clrBits          ( MDMAn_CTCR  (0)  , _BV(      3,  8) | _BV(3, 0)                  ); // Clear SINCOS SINC
            // Enable destination address increment (set it to one byte increment)
            _ib.modBits          ( MDMAn_CTCR  (0)  , _BV(      3, 10) | _BV(3, 2), _BV( 2,  2)     ); // Clear DINCOS      ; Set DINC to 2
        _ib.label                ( lblSkp1                                                        );

            // Configure the MDMA transfer - part 2
            _ib.jmpIfNotZero     ( xviFlag          , lblSkp2                                       );
            _ib.modBits          ( MDMAn_CBNDTR(0)  , _BV(0x00FFF, 20)            , _BV( 0, 20)     ); // Set   BRC  to 0
            _ib.clrBits          ( MDMAn_CIFCR (0)  , 0b00011111                                    ); // Clear TCIF BTIF BRTIF CTCIF TEIF
            _ib.clrBits          ( MDMAn_CLAR  (0)  , 0xFFFFFFFFL                                   ); // Clear LAR
            _ib.clrBits          ( MDMAn_CTBR  (0)  , _BV(         16)                              ); // Clear SBUS (QSPI_DR is connected to AXI bus)
            _ib.setBits          ( MDMAn_CTBR  (0)  ,                               _BV(    17)     ); // Set   DBUS (SRAM    is connected to AHB bus)
            _ib.wrtBits          ( MDMAn_CSAR  (0)  ,                               QSPI_DR         ); // Set   the source      address to QSPI_DR
        _ib.label                ( lblSkp2                                                        );
            _ib.wrtBits          ( MDMAn_CDAR  (0)  ,                               D1_DTCMRAM_BASE ); // Set   the destination address to SRAM
            _ib.modBits          ( MDMAn_CBNDTR(0)  , _BV(0x1FFFF,  0)            , xviRCnt         ); // Set   BNDT to the number of bytes

            // Enable MDMA
            _ib.setBits          ( MDMAn_CCR   (0)  ,                               _BV(     0)     ); // Set   EN

            // Read the bytes
            _ib.mov              ( 0                , xviDIdx                                       );
        _ib.label                ( lblLoop                                                        );
            // Start the MDMA transfer and wait until it is complete
            _ib.setBits          ( MDMAn_CCR   (0)  ,                               _BV(    16)     ); // Set   SWRQ
            _ib.rdlBitsUntilUnset( MDMAn_CISR  (0)  , _BV(         16)                              ); // Wait  !CRQA   // ##### !!! TODO : Use timeout !!! #####
            // Loop as needed
            _ib.add              ( xviDIdx          , MDMA_MTSIZE                 , xviDIdx         );
            _ib.jmpIfLT          ( xviDIdx          , xviRCnt                     , lblLoop         );

            // Disable MDMA
            _ib.clrBits          ( MDMAn_CCR   (0)  , _BV(          0)                              ); // Clear EN

            // Copy from SRAM to DB
            _ib.rdCMem           ( D1_DTCMRAM_BASE  , xviRCnt                     , MaxSWDRdSize    );

            // Set flag
            _ib.mov              ( 1                , xviFlag                                       );

        return xviDIdx;
    }

    /*
     * NOTE : # Indirect read mode using MDMA.
     *        # This instruction writes to DB!
     */
    @Override
    public XVI geninstruction_qspiReceiveDMA(final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    { return _geninstruction_qspiReceiveDMA_impl(xviCurOperInitFlag, null); }

    /*
     * NOTE : # Indirect read mode using MDMA.
     *        # This instruction writes to DB!
     */
    @Override
    public XVI geninstruction_qspiReceiveDMA(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    {
               geninstruction_qspiCommand         (cmd                    );
        return _geninstruction_qspiReceiveDMA_impl(xviCurOperInitFlag, cmd);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Indirect write mode.
     *        # This instruction reads from DB when 'useSB' is 'false'.
     *        # This instruction reads from SB when 'useSB' is 'true'.
     */
    private XVI _geninstruction_qspiTransmit_impl(final XVI xviCurOperInitFlag, final QSPICmd_Common cmdRef, final boolean useSB) throws JXMFatalLogicError
    {
        // NOTE : The parameter 'xviCurOperInitFlag' is not used by this function

        final XVI xviWCnt = _ib.xviInternal();
        final XVI xviDIdx = _ib.xviInternal();
        final XVI xviDId4 = _ib.xviInternal();

        final String lblLoop = _ib.uniqueLabelCounter("qspi_transmit_loop_%=");

            // Read the data length
        if(cmdRef == null)  {
            _ib.rdsBits        ( QSPI_DLR         , xviWCnt             );
            _ib.inc1           (                    xviWCnt             );
        }
        else {
            _ib.mov            ( cmdRef.dataLength, xviWCnt             );
        }
            // Configure the CCR register with functional as indirect write
            _ib.clrBits        ( QSPI_CCR         , _BV(3, 26)          ); // Clear FMODE

            // Write the bytes
            _ib.mov            ( 0                , xviDIdx             );
        _ib.label              ( lblLoop                       );
            _ib.rdlBitsUntilSet( QSPI_SR          , _BV(    2)          ); // Wait FTF   // ##### !!! TODO : Use timeout !!! #####
            _ib.div            ( xviDIdx          , 4         , xviDId4 );
        if(!useSB) {
            _ib.wrtBitsDB      ( QSPI_DR          , xviDId4             );
        }
        else {
            _ib.wrtBitsSB      ( QSPI_DR          , xviDId4             );
        }
            _ib.inc4           ( xviDIdx                                );
            _ib.jmpIfLT        ( xviDIdx          , xviWCnt   , lblLoop );
            _ib.rdlBitsUntilSet( QSPI_SR          , _BV(1)              ); // Wait TCF   // ##### !!! TODO : Use timeout !!! #####

        return xviDIdx;
    }

    /*
     * NOTE : # Indirect write mode.
     *        # This instruction reads from DB!
     */
    @Override
    public XVI geninstruction_qspiTransmitDB(final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    { return _geninstruction_qspiTransmit_impl(xviCurOperInitFlag, null, false); }

    /*
     * NOTE : # Indirect write mode.
     *        # This instruction reads from SB!
     */
    @Override
    public XVI geninstruction_qspiTransmitSB(final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    { return _geninstruction_qspiTransmit_impl(xviCurOperInitFlag, null, true ); }

    /*
     * NOTE : # Indirect write mode.
     *        # This instruction reads from DB!
     */
    @Override
    public XVI geninstruction_qspiTransmitDB(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    {
        if(cmd.dataMode   == DataMode._None) throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvDataMode  , QSPIClassName);
        if(cmd.dataLength <= 0             ) throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvDataLength, QSPIClassName);

               geninstruction_qspiCommand       (cmd                           );
        return _geninstruction_qspiTransmit_impl(xviCurOperInitFlag, cmd, false);
    }

    /*
     * NOTE : # Indirect write mode.
     *        # This instruction reads from SB!
     */
    @Override
    public XVI geninstruction_qspiTransmitSB(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    {
        if(cmd.dataMode   == DataMode._None) throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvDataMode  , QSPIClassName);
        if(cmd.dataLength <= 0             ) throw XCom.newJXMFatalLogicError(Texts.QSPICMD_InvDataLength, QSPIClassName);

               geninstruction_qspiCommand       (cmd                           );
        return _geninstruction_qspiTransmit_impl(xviCurOperInitFlag, cmd, true );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Indirect write mode using MDMA.
     *        # This instruction reads from DB!
     */
    private XVI _geninstruction_qspiTransmitDMA_impl(final XVI xviCurOperInitFlag, final QSPICmd_Common cmdRef) throws JXMFatalLogicError
    {
        final XVI xviFlag = xviCurOperInitFlag;

        final XVI xviWCnt = _ib.xviInternal();
        final XVI xviDIdx = _ib.xviInternal();

        final String lblSkip = "qspi_transit_dma_skip_init_%=";
        final String lblSkp1 = _ib.uniqueLabelName   (lblSkip                   );
        final String lblSkp2 = _ib.uniqueLabelName   (lblSkip                   );
        final String lblLoop = _ib.uniqueLabelCounter("qspi_transmit_dma_loop_%=");

            // Read the data length
        if(cmdRef == null)  {
            _ib.rdsBits          ( QSPI_DLR         , xviWCnt                                       );
            _ib.inc1             (                    xviWCnt                                       );
        }
        else {
            _ib.mov              ( cmdRef.dataLength, xviWCnt                                       );
        }

            // Copy from DB to SRAM
            _ib.wrCMem           ( D1_DTCMRAM_BASE  , xviWCnt                     , MaxSWDWrSize    );

            // Configure the CCR register with functional as indirect write
            _ib.clrBits         ( QSPI_CCR          , _BV(      3, 26)                              ); // Clear FMODE

            // Configure the MDMA transfer - part 1
            _ib.jmpIfNotZero     ( xviFlag          , lblSkp1                                       );
            // Disable MDMA
            _ib.clrBits          ( MDMAn_CCR   (0)  , _BV(          0)                              ); // Clear EN
            // Enable source address increment (set it to one byte increment)
            _ib.modBits          ( MDMAn_CTCR  (0)  , _BV(      3,  8) | _BV(3, 0), _BV( 2, 0)      ); // Clear SINCOS      ; Set SINC to 2
            // Disable destination address increment
            _ib.clrBits          ( MDMAn_CTCR  (0)  , _BV(      3, 10) | _BV(3, 2)                  ); // Clear DINCOS DINC
        _ib.label                ( lblSkp1                                                        );

            // Configure the MDMA transfer - part 2
            _ib.jmpIfNotZero     ( xviFlag          , lblSkp2                                       );
            _ib.modBits          ( MDMAn_CBNDTR(0)  , _BV(0x00FFF, 20)            , _BV( 0, 20)     ); // Set   BRC  to 0
            _ib.clrBits          ( MDMAn_CIFCR (0)  , 0b00011111                                    ); // Clear TCIF BTIF BRTIF CTCIF TEIF
            _ib.clrBits          ( MDMAn_CLAR  (0)  , 0xFFFFFFFFL                                   ); // Clear LAR
            _ib.setBits          ( MDMAn_CTBR  (0)  ,                               _BV(    16)     ); // Set   SBUS (SRAM    is connected to AHB bus)
            _ib.clrBits          ( MDMAn_CTBR  (0)  , _BV(         17)                              ); // Clear DBUS (QSPI_DR is connected to AXI bus)
            _ib.wrtBits          ( MDMAn_CDAR  (0)  ,                               QSPI_DR         ); // Set   the destination address to QSPI_DR
        _ib.label                ( lblSkp2                                                        );
            _ib.wrtBits          ( MDMAn_CSAR  (0)  ,                               D1_DTCMRAM_BASE ); // Set   the source      address to SRAM
            _ib.modBits          ( MDMAn_CBNDTR(0)  , _BV(0x1FFFF,  0)            , xviWCnt         ); // Set   BNDT to the number of bytes

            // Enable MDMA
            _ib.setBits          ( MDMAn_CCR   (0)  ,                               _BV(     0)     ); // Set   EN

            // Write the bytes
            _ib.mov              ( 0                , xviDIdx                                       );
        _ib.label                ( lblLoop                                                        );
            // Start the MDMA transfer and wait until it is complete
            _ib.setBits          ( MDMAn_CCR   (0)  ,                               _BV(    16)     ); // Set   SWRQ
            _ib.rdlBitsUntilUnset( MDMAn_CISR  (0)  , _BV(         16)                              ); // Wait  !CRQA   // ##### !!! TODO : Use timeout !!! #####
            // Loop as needed
            _ib.add              ( xviDIdx          , MDMA_MTSIZE                 , xviDIdx         );
            _ib.jmpIfLT          ( xviDIdx          , xviWCnt                     , lblLoop         );

            // Disable MDMA
            _ib.clrBits          ( MDMAn_CCR   (0)  , _BV(          0)                              ); // Clear EN

            // Set flag
            _ib.mov              ( 1                , xviFlag                                       );

        return xviDIdx;
    }

    @Override
    public XVI geninstruction_qspiTransmitDMA(final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    { return _geninstruction_qspiTransmitDMA_impl(xviCurOperInitFlag, null); }

    /*
     * NOTE : # Indirect write mode using MDMA.
     *        # This instruction reads from DB!
     */
    @Override
    public XVI geninstruction_qspiTransmitDMA(final QSPICmd_Common cmd, final XVI xviCurOperInitFlag) throws JXMFatalLogicError
    {
               geninstruction_qspiCommand          (cmd                    );
        return _geninstruction_qspiTransmitDMA_impl(xviCurOperInitFlag, cmd);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected long[][] _instruction_resetHaltInitClock(final STM32QSPIPin qspiPin, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped)
    {
        // Halt and reset
        _ib.haltCore              ( true                                                  );

        // Supply configuration update enable
        _ib.modBits               ( PWR_CR3      , 0b00000111     , 0b00000010            ); // Clear SCUEN BYPASS                 ; Set   LDOEN
        _ib.rdlBitsUntilSet       ( PWR_CSR1     , _BV(    13)                            ); // Wait  ACTVOSR   // ##### !!! TODO : Use timeout !!! #####

        // Configure main internal regulator output voltage
        _ib.clrBits               ( SYSCFG_PWRCR , 0b00000001                             ); // Clear ODEN
        _ib.rddBits               ( SYSCFG_PWRCR                                          ); // Delay using read and discard
        _ib.setBits               ( PWR_D3CR     ,                  _BV( 3, 14)           ); // Set   VOS to SCALE1
        _ib.rddBits               ( PWR_D3CR                                              ); // Delay using read and discard
        _ib.rdlBitsUntilSet       ( PWR_D3CR     , _BV(    13)                            ); // Wait  VOSRDY    // ##### !!! TODO : Use timeout !!! #####

        // Initialize HSI to 64MHz
        _ib.modBits               ( RCC_CR       , 0b00011001     , 0b00000001            ); // Clear HSIDIV                       ; Set   HSION
        _ib.rdlBitsUntilSet       ( RCC_CR       , 0b00000100                             ); // Wait  HSIRDY    // ##### !!! TODO : Use timeout !!! #####

        // Initialize PLL to use HSI to produce 480MHz clock (64 / 8 * 120 / 2)
        // M = 8; R = Q = P = 2; N = 120
        _ib.clrBits               ( RCC_CR       , _BV(    24)                            ); // Clear PLL1ON
        _ib.rdlBitsUntilUnset     ( RCC_CR       , _BV(    25)                            ); // Wait  !PLL1RDY  // ##### !!! TODO : Use timeout !!! #####
        _ib.modBits               ( RCC_PLLCKSELR, _BV(63,  4) | 3, _BV( 8,  4)           ); // Set   DIVM1 to 8                   ; Clear PLLSRC
        _ib.modBits               ( RCC_PLL1DIVR , 0x7F7FFFFFL    , 0x01020478L           ); // Set   DIVR1   DIVQ1   DIVP1   to 2 ; Set   DIVN1 to 120
        _ib.setBits               ( RCC_PLLCFGR  ,                  _BV( 3, 16)           ); // Set   DIVR1EN DIVQ1EN DIVP1EN
        _ib.setBits               ( RCC_CR       ,                  _BV(    24)           ); // Set   PLL1ON
        _ib.rdlBitsUntilSet       ( RCC_CR       , _BV(    25)                            ); // Wait  PLL1RDY   // ##### !!! TODO : Use timeout !!! #####

        // Set flash latency
        _ib.modBits               ( FLASH_ACR    , _BV(15,  0)    , _BV( 2,  0)           ); // Set   LATENCY to 2

        // Initialize the CPU, AHB, and APB buses clocks
        // ##### !!! TODO : Are the clock frequencies correct? !!! #####
        _ib.modBits /* D1PCLK1 */ ( RCC_D1CFGR   , _BV( 7,  4)    , _BV( 4,  4)           ); // Set   D1PPRE  to 4 (D1 domain APB3 prescaler is 2)
        _ib.modBits /* PCLK1   */ ( RCC_D2CFGR   , _BV( 7,  4)    , _BV( 4,  4)           ); // Set   D2PPRE1 to 4 (D2 domain APB1 prescaler is 2)
        _ib.modBits /* PCLK2   */ ( RCC_D2CFGR   , _BV( 7,  8)    , _BV( 4,  8)           ); // Set   D2PPRE2 to 4 (D2 domain APB2 prescaler is 2)
        _ib.modBits /* D3PCLK1 */ ( RCC_D3CFGR   , _BV( 7,  4)    , _BV( 4,  4)           ); // Set   D3PPRE  to 4 (D3 domain APB4 prescaler is 2)
        _ib.modBits /* HCLK    */ ( RCC_D1CFGR   , _BV(15,  0)    , _BV( 8,  0)           ); // Set   HPRE    to 8 (D1 domain AHB  prescaler is 2)
        _ib.clrBits /* SYSCLK  */ ( RCC_D1CFGR   , _BV(15,  8)                            ); // Set   D1CPRE  to 0 (D1 domain Core prescaler is 1)
        _ib.modBits               ( RCC_CFGR     , _BV( 7,  0)    , _BV( 3,  0)           ); // Set   SW      to 3 (PLL1 selected as system clock)
        _ib.rdlBitsWhileNotEqual  ( RCC_CFGR     , _BV( 7,  3)    , _BV( 3,  3)           ); // Wait  SWS       // ##### !!! TODO : Use timeout !!! #####

        // Initialize MDMA clock
        _ib.setBits               ( RCC_AHB3ENR  ,                  _BV(     0)           ); // Set   MDMAEN
        _ib.rddBits               ( RCC_AHB3ENR                                           ); // Delay using read and discard

        // Initialize QSPI clock
        // ##### !!! TODO : Is the clock frequency correct? !!! #####
        _ib.clrBits               ( RCC_D1CCIPR  , _BV( 3,  4)                            ); // Clear QSPISEL (D1 domain clock selected as QSPI clock)
        _ib.setBits               ( RCC_AHB3ENR  ,                  _BV(    14)           ); // Set   QSPIEN
        _ib.rddBits               ( RCC_AHB3ENR                                           ); // Delay using read and discard

        // Initialize GPIO clocks
        _ib.setBits               ( RCC_AHB4ENR  ,                  qspiPin.GPIOxENBits() ); // Set   GPIO*EN
        _ib.rddBits               ( RCC_AHB4ENR                                           ); // Delay using read and discard

        // Initialize MPU as needed
        if(enableMemoryMapped) put_instruction_initMPU_QSPI(_ib, QSPI_BASE, flashCapacityBytes);

        // Link and return the instructions
        return _ib.link();
    }

    @Override
    protected long[][] _instruction_initGPIO_XF(final STM32QSPIPin qspiPin, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped)
    {
        // Initialize GPIO to QSPI modes
        _put_instruction_initGPIO_QSPI(qspiPin.CLK);
        _put_instruction_initGPIO_QSPI(qspiPin.NCS);
        _put_instruction_initGPIO_QSPI(qspiPin.IO0);
        _put_instruction_initGPIO_QSPI(qspiPin.IO1);
        _put_instruction_initGPIO_QSPI(qspiPin.IO2);
        _put_instruction_initGPIO_QSPI(qspiPin.IO3);

        // Initialize MDMA
        _ib.clrBits          ( MDMAn_CCR   (0), _BV(      0)                            ); // Clear EN
        _ib.rdlBitsUntilUnset( MDMAn_CCR   (0), _BV(      0)                            ); // Wait  !EN    // ##### !!! TODO : Use timeout !!! #####

        _ib.wrtBits          ( MDMAn_CCR   (0), _BV(  3,  6)                            ); // SET PL to 3    ; Set BEX to LITTLE_ENDIANNESS_PRESERVE

        _ib.modBits          ( MDMAn_CTCR  (0), 0xFFFFFFFFL , _BVs(31             , 30) ); // Set   BWM SWRM ; Clear others
        _ib.modBits          ( MDMAn_CTCR  (0), _BV(127, 18), _BV (MDMA_MTSIZE - 1, 18) ); // Set   TLEN to (MDMA_MTSIZE - 1)

        _ib.wrtBits          ( MDMAn_CBNDTR(0), 0                                       ); // Clear BRC BRDUM BRSUM BNDT
        _ib.wrtBits          ( MDMAn_CBRUR (0), 0                                       ); // Clear DUV SUV

        _ib.wrtBits          ( MDMAn_CTBR  (0), 0                                       ); // Clear DBUS SBUS TSEL
        _ib.wrtBits          ( MDMAn_CLAR  (0), 0                                       ); // Clear LAR

        // Prepare the constants
        final long clrCPSS   = _BV(255, 24) | _BVs(7, 6, 4);
        final long setCPSS   = _BV(  1, 24) | _BV (      4);

        final long clrFZCSCM = _BV(31, 16) | _BV(7, 8) | _BV(0);
        final long setFZCSCM = _BV( XCom.log2(flashCapacityBytes) - 1, 16 );

        // Configure QSPI FIFO threshold
        _ib.modBits          ( QSPI_CR        , _BV(31, 8)  , _BV(3, 8)                 ); // Set FTHRES to 3 (FTF is set if there are 4 or more free/valid bytes)
        _ib.rdlBitsUntilUnset( QSPI_SR        , _BV(    5)                              ); // Wait !BUSY   // ##### !!! TODO : Use timeout !!! #####

        // Configure QSPI clock prescaler and sample shift
        _ib.modBits          ( QSPI_CR        , clrCPSS     , setCPSS                   ); // Set PRESCALER to 1   ; Clear FSEL DFM ; Set SSHIFT to  1

        // Configure QSPI flash size, CS high time and clock mode
        _ib.modBits          ( QSPI_DCR       , clrFZCSCM   , setFZCSCM                 ); // Set FSIZE to <fsize> ; Clear CSHT CKMODE

        // Enable QSPI
        _ib.setBits          ( QSPI_CR ,                      _BV(0)                    ); // Set EN

        // Link and return the instructions
        return _ib.link();
    }

    @Override
    protected long[][] _instruction_deinitAll(final STM32QSPIPin qspiPin, final long flashCapacityBytes, final int flashPageSizeBytes, final boolean enableMemoryMapped)
    {
        // Disable QSPI
        _ib.clrBits( QSPI_CR       , _BV( 0)               ); // Clear EN

        // Disable MDMA
        _ib.clrBits( MDMAn_CCR  (0), _BV( 0)               ); // Clear EN
        _ib.clrBits( MDMAn_CIFCR(0), 0b00011111            ); // Clear TCIF BTIF BRTIF CTCIF TEIF

        // Initialize GPIO to input modes
        _put_instruction_deinitGPIO(qspiPin.CLK);
        _put_instruction_deinitGPIO(qspiPin.NCS);
        _put_instruction_deinitGPIO(qspiPin.IO0);
        _put_instruction_deinitGPIO(qspiPin.IO1);
        _put_instruction_deinitGPIO(qspiPin.IO2);
        _put_instruction_deinitGPIO(qspiPin.IO3);

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // Disable MPU as needed
        if(enableMemoryMapped) put_instruction_disableMPU(_ib);

        // Deinitialize GPIO, QSPI, and MDMA clocks
        _ib.clrBits( RCC_AHB4ENR   , qspiPin.GPIOxENBits() ); // Clear GPIO*EN
        _ib.clrBits( RCC_AHB3ENR   , _BV(14)               ); // Clear QSPIEN
        _ib.clrBits( RCC_AHB3ENR   , _BV( 0)               ); // Clear MDMAEN

        // Link and return the instructions
        return _ib.link();
    }

} // class STM32QSPI_H750
