/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.stm32xf;


import jxm.*;
import jxm.ugc.*;
import jxm.xb.*;

import static jxm.ugc.SWDExecInstOpcode.XVI;

import static jxm.ugc.stm32xf.STM32QSPI.InstMode;
import static jxm.ugc.stm32xf.STM32QSPI.AddrMode;
import static jxm.ugc.stm32xf.STM32QSPI.AddrSize;
import static jxm.ugc.stm32xf.STM32QSPI.DataMode;
import static jxm.ugc.stm32xf.STM32QSPI.QSPICmd_Common;


/*
 * This class class is written partially based on the algorithms and information found from:
 *
 *     W25Q80BV
 *     3V 8M-Bit Serial Flash Memory with Dual and Quad SPI
 *     https://www.winbond.com/resource-files/w25q80bv%20revk%2020151203.pdf
 *
 *     W25Q16DV
 *     3V 16M-Bit Serial Flash Memory with Dual and Quad SPI
 *     https://www.winbond.com/resource-files/w25q16dv%20revk%2005232016%20doc.pdf
 *
 *     W25Q32FV
 *     3V 32M-Bit Serial Flash Memory with Dual/Quad SPI & QPI
 *     https://www.winbond.com/resource-files/w25q32fv%20revj%2006032016.pdf
 *
 *     W25Q64FV
 *     3V 64M-Bit Serial Flash Memory with Dual/Quad SPI & QPI
 *     https://www.winbond.com/resource-files/w25q64fv%20revs%2007182017.pdf
 *
 *     W25Q128FV
 *     3V 128M-Bit Serial Flash Memory with Dual/Quad SPI & QPI
 *     https://www.winbond.com/resource-files/w25q128fv%20rev.m%2005132016%20kms.pdf
 *
 *     W25Q256FV
 *     3V 256M-Bit Serial Flash Memory with Dual/Quad SPI & QPI
 *     https://www.winbond.com/resource-files/w25q256fv%20revi%2002262016%20kms.pdf
 *
 *     W25Q256JV
 *     3V 256M-Bit Serial Flash Memory with Dual/Quad SPI
 *     https://www.winbond.com/resource-files/W25Q256JV%20SPI%20RevL%2010182022%20Plus.pdf
 *
 *     W25Q256JV-DTR
 *     3V 256M-Bit Serial Flash Memory with Dual/Quad SPI, QPI & DTR
 *     https://www.winbond.com/resource-files/W25Q256JV%20DTR%20RevI%2010182022%20Plus.pdf
 *
 *     W25Q512JV
 *     3V 256M-Bit Serial Flash Memory with Dual/Quad SPI
 *     https://www.winbond.com/resource-files/W25Q512JV%20SPI%20RevG%2001122022Plus.pdf
 *
 *     W25Q512JV-DTR
 *     3V 256M-Bit Serial Flash Memory with Dual/Quad SPI, QPI & DTR
 *     https://www.winbond.com/resource-files/W25Q512JV%20DTR%20RevD%2006292020%20133.pdf
 *
 *     W25Q01JV
 *     3V 1G-Bit Serial Flash Memory with Dual/Quad SPI
 *     https://www.winbond.com/resource-files/W25Q01JV%20SPI%20RevE%2003042024%20Plus.pdf
 *
 *     W25Q01JV-DTR
 *     3V 1G-Bit Serial Flash Memory with Dual/Quad SPI, QPI & DTR
 *     https://www.winbond.com/resource-files/W25Q01JV_DTR%20RevE%2003042024.pdf
 *
 *     W25Q02JV-DTR
 *     3V 2G-Bit Serial Flash Memory with Dual/Quad SPI, QPI & DTR
 *     https://www.winbond.com/resource-files/W25Q02JV_DTR%20RevC%2003292023.pdf
 *
 * ~~~ Last accessed & checked on 2024-06-25 ~~~
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * Please refer to the comment block before the 'STM32QSPI' class definition in the 'STM32QSPI.java' file for more details and information.
 */
public class STM32QSPI_Flash_W25Q implements STM32_ExternalFlash {

    private static final int W25Q_WRITE_ENABLE             = 0x06; // Set WEL bit; must be set before any write/program/erase
    private static final int W25Q_ENABLE_VOLATILE_SR       = 0x50; // Enable volatile SR write
    private static final int W25Q_WRITE_DISABLE            = 0x04; // Reset WEL bit (the default state after power-up)
    private static final int W25Q_READ_SR1                 = 0x05; // Read  status register 1
    private static final int W25Q_READ_SR2                 = 0x35; // Read  status register 2
    private static final int W25Q_READ_SR3                 = 0x15; // Read  status register 3
    private static final int W25Q_WRITE_SR1                = 0x01; // Write status register 1
    private static final int W25Q_WRITE_SR2                = 0x31; // Write status register 2
    private static final int W25Q_WRITE_SR3                = 0x11; // Write status register 3
    private static final int W25Q_READ_EXT_ADDR_REG        = 0xC8; // Read  extended address register (only in 3-byte mode)
    private static final int W25Q_WRITE_EXT_ADDR_REG       = 0xC8; // Write extended address register (only in 3-byte mode)
    private static final int W25Q_ENABLE_4B_MODE           = 0xB7; // Enable  4-byte mode (> 128MB)
    private static final int W25Q_DISABLE_4B_MODE          = 0xE9; // Disable 4-byte mode (<=128MB)
    private static final int W25Q_READ_DATA                = 0x03; // Read data by standard SPI
    private static final int W25Q_READ_DATA_4B             = 0x13; // Read data by standard SPI in 4-byte mode
    private static final int W25Q_FAST_READ                = 0x0B; // Fast read
    private static final int W25Q_FAST_READ_4B             = 0x0C; // Fast read in 4-byte mode
    private static final int W25Q_FAST_READ_DUAL_OUT       = 0x3B; // Fast read in dual-SPI OUTPUT
    private static final int W25Q_FAST_READ_DUAL_OUT_4B    = 0x3C; // Fast read in dual-SPI OUTPUT in 4-byte mode
    private static final int W25Q_FAST_READ_QUAD_OUT       = 0x6B; // Fast read in quad-SPI OUTPUT
    private static final int W25Q_FAST_READ_QUAD_OUT_4B    = 0x6C; // Fast read in quad-SPI OUTPUT in 4-byte mode
    private static final int W25Q_FAST_READ_DUAL_IO        = 0xBB; // Fast read in dual-SPI I/O (address is transmitted by both lines)
    private static final int W25Q_FAST_READ_DUAL_IO_4B     = 0xBC; // Fast read in dual-SPI I/O in 4-byte mode
    private static final int W25Q_FAST_READ_QUAD_IO        = 0xEB; // Fast read in quad-SPI I/O (address is transmitted by quad lines)
    private static final int W25Q_FAST_READ_QUAD_IO_4B     = 0xEC; // Fast read in quad-SPI I/O in 4-byte mode
    private static final int W25Q_SET_BURST_WRAP           = 0x77; // Set burst with wrap
    private static final int W25Q_PAGE_PROGRAM             = 0x02; // Program page by single SPI line (256 bytes)
    private static final int W25Q_PAGE_PROGRAM_4B          = 0x12; // Program page by single SPI in 4-byte mode
    private static final int W25Q_PAGE_PROGRAM_QUAD_INP    = 0x32; // Program page by quad   SPI lines (256 bytes)
    private static final int W25Q_PAGE_PROGRAM_QUAD_INP_4B = 0x34; // Program page by quad   SPI in 4-byte mode
    private static final int W25Q_SECTOR_ERASE             = 0x20; // Set all  4KB sector with = 0xFF
    private static final int W25Q_SECTOR_ERASE_4B          = 0x21; // Set all  4KB sector with = 0xFF in 4-byte mode
    private static final int W25Q_32KB_BLOCK_ERASE         = 0x52; // Set all 32KB block  with = 0xFF
    private static final int W25Q_64KB_BLOCK_ERASE         = 0xD8; // Set all 64KB block  with = 0xFF
    private static final int W25Q_64KB_BLOCK_ERASE_4B      = 0xDC; // Set all 64KB sector with = 0xFF in 4-byte mode
    private static final int W25Q_CHIP_ERASE               = 0xC7; // Fill all the chip   with = 0xFF
    private static final int W25Q_ERASEPROG_SUSPEND        = 0x75; // Suspend erase/program operation (only if SUS=0 and BYSY=1)
    private static final int W25Q_ERASEPROG_RESUME         = 0x7A; // Resume erase/program operation  (only if SUS=1 and BUSY=0)
    private static final int W25Q_POWERDOWN                = 0xB9; // Power down the chip
    private static final int W25Q_POWERUP                  = 0xAB; // Release power-down (power-up)
    private static final int W25Q_DEVID                    = 0xAB; // Read Device ID (same as W25Q_POWERUP)
    private static final int W25Q_FULLID                   = 0x90; // Read manufacturer ID & device ID
    private static final int W25Q_FULLID_DUAL_IO           = 0x92; // Read manufacturer ID & device ID by dual I/O
    private static final int W25Q_FULLID_QUAD_IO           = 0x94; // Read manufacturer ID & device ID by quad I/O
    private static final int W25Q_READ_UID                 = 0x4B; // Read unique chip 64-bit ID
    private static final int W25Q_READ_JEDEC_ID            = 0x9F; // Read JEDEC-standard ID
    private static final int W25Q_READ_SFDP                = 0x5A; // Read SFDP register parameters
    private static final int W25Q_ERASE_SECURITY_REG       = 0x44; // Erase security registers
    private static final int W25Q_PROG_SECURITY_REG        = 0x42; // Program security registers
    private static final int W25Q_READ_SECURITY_REG        = 0x48; // Read security registers
    private static final int W25Q_IND_BLOCK_LOCK           = 0x36; // Enable  block/sector protection
    private static final int W25Q_IND_BLOCK_UNLOCK         = 0x39; // Disable block/sector protection
    private static final int W25Q_READ_BLOCK_LOCK          = 0x3D; // Check block/sector protection
    private static final int W25Q_GLOBAL_LOCK              = 0x7E; // Global read-only protection enable
    private static final int W25Q_GLOBAL_UNLOCK            = 0x98; // Global read-only protection disable
    private static final int W25Q_ENABLE_RESET             = 0x66; // Enable software reset
    private static final int W25Q_RESET                    = 0x99; // Software reset

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final STM32QSPI          _qspi;
    private final SWDExecInstBuilder _ib;
    private final SWDExecInst        _swdExecInst;

    public STM32QSPI_Flash_W25Q(final STM32QSPI qspi, final XVI xviCurrentOperationInitialized, final XVI xviFlashAddress) throws JXMFatalLogicError
    {
        _qspi                      = qspi;
        _ib                        = qspi.swdExecInstBuilder();
        _swdExecInst               = qspi.swdExecInst();

        _xviW25Q_CurOperInitFlag   = xviCurrentOperationInitialized;
        _xviW25Q_FlashAddress      = xviFlashAddress;

        _instruction_initFlash     = _instruction_initFlash    ();
        _instruction_softwareReset = _instruction_softwareReset();

        _instruction_chipErase     = _instruction_chipErase    ();

        _instruction_readPage      = _instruction_readPage     ();
        _instruction_readPageDMA   = _instruction_readPageDMA  ();
        _instruction_readPageMMap  = _instruction_readPageMMap ();

        _instruction_writePage     = _instruction_writePage    ();
        _instruction_writePageDMA  = _instruction_writePageDMA ();
        _instruction_writePageMMap = _instruction_writePageMMap();

        _instruction_endMMap       = _instruction_endMMap      ();
    }

    @Override
    public  boolean mmapEnabled()
    { return _qspi.mmapEnabled(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final long  _TIMEOUT_DEF_WAIT_MS     = 500;
    private static final long  _TIMEOUT_ERASE_MS        = SWDExecInst.DEF_WHILE_TIMEOUT_MS; // Use the default timeout (it should be enough even for the larger flash size)

    private static final int[] _readSRx                 = new int[] { W25Q_READ_SR1 , W25Q_READ_SR2 , W25Q_READ_SR3  };
    private static final int[] _writeSRx                = new int[] { W25Q_WRITE_SR1, W25Q_WRITE_SR2, W25Q_WRITE_SR3 };

    private static final XVI   _xviW25Q_LargeSize       = XVI._0000;

    private static final XVI   _xviW25Q_Status_BUSY     = XVI._0001;
    private static final XVI   _xviW25Q_Status_WEL      = XVI._0002;
    private static final XVI   _xviW25Q_Status_QE       = XVI._0003;
    private static final XVI   _xviW25Q_Status_SUS      = XVI._0004;
    private static final XVI   _xviW25Q_Status_ADS      = XVI._0005;
    private static final XVI   _xviW25Q_Status_ADP      = XVI._0006;

    private static       XVI   _xviW25Q_CurOperInitFlag = XVI._NA_;
    private static       XVI   _xviW25Q_FlashAddress    = XVI._NA_;

    // NOTE : This instruction writes to SB!
    private XVI _pi_readFullID() throws JXMFatalLogicError
    {
        final XVI xviDummyFlag = _ib.xviInternal();

        _ib.mov( 0, xviDummyFlag );

        return _qspi.geninstruction_qspiReceiveSB(
            _qspi.newQSPICmd(InstMode._1Line, W25Q_FULLID, AddrMode._1Line, AddrSize._24Bits, DataMode._1Line, 2),
            xviDummyFlag
        );
    }

    // NOTE : This instruction writes to SB!
    private XVI _pi_readJEDECID() throws JXMFatalLogicError
    {
        final XVI xviDummyFlag = _ib.xviInternal();

        _ib.mov( 0, xviDummyFlag );

        return _qspi.geninstruction_qspiReceiveSB(
            _qspi.newQSPICmd(InstMode._1Line, W25Q_READ_JEDEC_ID, DataMode._1Line, 3),
            xviDummyFlag
        );
    }

    // NOTE : This instruction writes to SB!
    private XVI _pi_readStatusReg(final int index) throws JXMFatalLogicError
    {
        final XVI xviDummyFlag = _ib.xviInternal();

        _ib.mov( 0, xviDummyFlag );

        return _qspi.geninstruction_qspiReceiveSB(
            _qspi.newQSPICmd(InstMode._1Line, _readSRx[index - 1], DataMode._1Line, 1),
            xviDummyFlag
        );
    }

    // NOTE : This instruction clobbers SB!
    private void _pi_writeStatusReg(final int index, final XVI value) throws JXMFatalLogicError
    {
        final XVI xviDummyFlag = _ib.xviInternal();

        _ib.mov        ( 0, xviDummyFlag );

        _pi_waitBusy   (                 );
        _pi_writeEnable( true            );
        _ib.strSB      ( value, 0        );

        _qspi.geninstruction_qspiTransmitSB(
            _qspi.newQSPICmd(InstMode._1Line, _writeSRx[index - 1], DataMode._1Line, 1),
            xviDummyFlag
        );
    }

    // NOTE : This instruction writes to SB!
    private void _pi_readAllStatus() throws JXMFatalLogicError
    {
        final XVI xviTmp0 = _ib.xviInternal();

        _pi_readStatusReg( 1                                         );
        _ib.ldrSB        ( 0      , xviTmp0                          );
        _ib.bwAND        ( xviTmp0, 0b00000001, _xviW25Q_Status_BUSY );
        _ib.bwAND        ( xviTmp0, 0b00000010, _xviW25Q_Status_WEL  );

        _pi_readStatusReg( 2                                         );
        _ib.ldrSB        ( 0      , xviTmp0                          );
        _ib.bwAND        ( xviTmp0, 0b00000010, _xviW25Q_Status_QE   );
        _ib.bwAND        ( xviTmp0, 0b10000000, _xviW25Q_Status_SUS  );

        _pi_readStatusReg( 3                                         );
        _ib.ldrSB        ( 0      , xviTmp0                          );
        _ib.bwAND        ( xviTmp0, 0b00000001, _xviW25Q_Status_ADS  );
        _ib.bwAND        ( xviTmp0, 0b00000010, _xviW25Q_Status_ADP  );
    }

    // NOTE : This instruction clobbers SB!
    private void _pi_waitBusy(final long timeoutMS) throws JXMFatalLogicError
    {
        final XVI xviTmp0 = _ib.xviInternal();
        final XVI xviTmpB = _ib.xviInternal();
        final XVI xviTmpC = _ib.xviInternal();

        final String lblBusyLoop = _ib.uniqueLabelCounter("w25q_busy_loop_%=");
        final String lblBusyEnd  = _ib.uniqueLabelCounter("w25q_busy_end_%=" );

            _ib.getMS          ( xviTmpB                                                );
        _ib.label              ( lblBusyLoop                                            );
            // Check !BUSY
            _pi_readStatusReg  ( 1                                                      );
            _ib.ldrSB          ( 0                   , xviTmp0                          );
            _ib.bwAND          ( xviTmp0             , 0b00000001, _xviW25Q_Status_BUSY );
            _ib.jmpIfZero      ( _xviW25Q_Status_BUSY, lblBusyEnd                       );
            // Check for timeout
            _ib.getMS          ( xviTmpC                                                );
            _ib.sub            ( xviTmpC             , xviTmpB   , xviTmpC              );
            _ib.errIfCmpGT_tout( xviTmpC             , timeoutMS                        );
            // Wait !BUSY
            _ib.jmp            ( lblBusyLoop                                            );
        _ib.label              ( lblBusyEnd                                             );
    }

    private void _pi_waitBusy() throws JXMFatalLogicError
    { _pi_waitBusy(_TIMEOUT_DEF_WAIT_MS); }

    private void _pi_writeEnable(final boolean enable) throws JXMFatalLogicError
    {
        _qspi.geninstruction_qspiCommand(
            _qspi.newQSPICmd(InstMode._1Line, enable ? W25Q_WRITE_ENABLE : W25Q_WRITE_DISABLE)
        );

        _ib.delayMS( 1                      );
        _ib.mov    ( 1, _xviW25Q_Status_WEL );
    }

    private void _pi_set4ByteMode(final boolean enable) throws JXMFatalLogicError
    {
        _pi_waitBusy(   );

        _qspi.geninstruction_qspiCommand(
            _qspi.newQSPICmd(InstMode._1Line, enable ? W25Q_ENABLE_4B_MODE : W25Q_DISABLE_4B_MODE)
        );

        _ib.delayMS ( 1 );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final long[][] _instruction_initFlash;
    private final long[][] _instruction_softwareReset;

    private final long[][] _instruction_chipErase;

    private final long[][] _instruction_readPage;
    private final long[][] _instruction_readPageDMA;
    private final long[][] _instruction_readPageMMap;

    private final long[][] _instruction_writePage;
    private final long[][] _instruction_writePageDMA;
    private final long[][] _instruction_writePageMMap;

    private final long[][] _instruction_endMMap;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : This instruction clobbers SB!
    private long[][] _instruction_initFlash() throws JXMFatalLogicError
    {
        final XVI xviTmp0 = _ib.xviInternal();
        final XVI xviTmp1 = _ib.xviInternal();

        final String lblNLarge = _ib.uniqueLabelCounter("w25q_nlarge_%=");
        final String lblSmall  = _ib.uniqueLabelCounter("w25q_small_%=" );
        final String lblADP    = _ib.uniqueLabelCounter("w25q_adp_%="   );
        final String lblADS    = _ib.uniqueLabelCounter("w25q_ads_%="   );
        final String lblQE     = _ib.uniqueLabelCounter("w25q_qe_%="    );

            // Read and check the full ID
            _pi_readFullID    (                                                    );
            _ib.ldrSB         ( 0                  , xviTmp0                       );
            _ib.bwAND         ( xviTmp0            , 0x000000FFL       , xviTmp1   );
            _ib.errIfCmpNEQ   ( xviTmp1            , 0x000000EFL                   );
            _ib.bwAND         ( xviTmp0            , 0x0000FF00L       , xviTmp1   );
            _ib.errIfZero     ( xviTmp1                                            );

            // Read and check the JEDEC ID
            _pi_readJEDECID   (                                                    );
            _ib.ldrSB         ( 0                  , xviTmp0                       );
            _ib.bwAND         ( xviTmp0            , 0x000000FFL       , xviTmp1   );
            _ib.errIfCmpNEQ   ( xviTmp1            , 0x000000EFL                   );
            _ib.bwAND         ( xviTmp0            , 0x0000FF00L       , xviTmp1   );
            _ib.errIfZero     ( xviTmp1                                            );
            _ib.bwAND         ( xviTmp0            , 0x00FF0000L       , xviTmp1   );
            _ib.errIfZero     ( xviTmp1                                            );

            // Check if the flash size is larger than 128MB
            _ib.mov           ( 0                  , _xviW25Q_LargeSize            );
            _ib.bwRSH         ( xviTmp1            , 16                , xviTmp1   );
            _ib.jmpIfLTE      ( xviTmp1            , 0x18              , lblNLarge );
            _ib.mov           ( 1                  , _xviW25Q_LargeSize            );
        _ib.label             ( lblNLarge                                          );

            // Read all status
            _pi_readAllStatus (                                                    );

            // Initialize large flash
            _ib.jmpIfZero     ( _xviW25Q_LargeSize , lblSmall                      );
            // Set 'Power-Up Address Mode' to 4 bytes as needed
            _ib.jmpIfNotZero  ( _xviW25Q_Status_ADP, lblADP                        );
            _pi_readStatusReg ( 3                                                  );
            _ib.ldrSB         ( 0                  , xviTmp0                       );
            _ib.bwOR          ( xviTmp0            , 0b00000010        , xviTmp0   );
            _pi_writeStatusReg( 3                  , xviTmp0                       );
        _ib.label             ( lblADP                                             );
            // Set 'Current Address Mode' to 4 bytes as needed
            _ib.jmpIfNotZero  ( _xviW25Q_Status_ADS, lblADS                        );
            _pi_set4ByteMode  ( true                                               );
        _ib.label             ( lblADS                                             );
        _ib.label             ( lblSmall                                           );

            // Enable QSPI mode as needed
            _ib.jmpIfNotZero  ( _xviW25Q_Status_QE , lblQE                         );
            _pi_readStatusReg ( 2                                                  );
            _ib.ldrSB         ( 0                  , xviTmp0                       );
            _ib.bwOR          ( xviTmp0            , 0b00000010        , xviTmp0   );
            _pi_writeStatusReg( 2                  , xviTmp0                       );
        _ib.label             ( lblQE                                              );

            // Read all status again
            _pi_readAllStatus (                                                   );

        /*                                            ✓      ✓
         * name           read   qread  page   erase  chip   device_id    page    erase     flash
         *                cmd    cmd    prog   cmd    erase               size    size      size
         *                              cmd           cmd
         *
         * w25q80bv       0x03   0x00   0x02   0xd8   0xc7   0x001440ef   0x100   0x10000   0x00100000
         * w25q16jv       0x03   0x00   0x02   0xd8   0xc7   0x001540ef   0x100   0x10000   0x00200000
         * w25q16jv       0x03   0x00   0x02   0xd8   0xc7   0x001570ef   0x100   0x10000   0x00200000   (QPI/DTR mode)
         * w25q32fv/jv    0x03   0xeb   0x02   0xd8   0xc7   0x001640ef   0x100   0x10000   0x00400000
         * w25q32fv       0x03   0xeb   0x02   0xd8   0xc7   0x001660ef   0x100   0x10000   0x00400000   (QPI     mode)
         * w25q32jv       0x03   0x00   0x02   0xd8   0xc7   0x001670ef   0x100   0x10000   0x00400000
         * w25q64fv/jv    0x03   0xeb   0x02   0xd8   0xc7   0x001740ef   0x100   0x10000   0x00800000
         * w25q64fv       0x03   0xeb   0x02   0xd8   0xc7   0x001760ef   0x100   0x10000   0x00800000   (QPI     mode)
         * w25q64jv       0x03   0x00   0x02   0xd8   0xc7   0x001770ef   0x100   0x10000   0x00800000
         * w25q128fv/jv   0x03   0xeb   0x02   0xd8   0xc7   0x001840ef   0x100   0x10000   0x01000000
         * w25q128fv      0x03   0xeb   0x02   0xd8   0xc7   0x001860ef   0x100   0x10000   0x01000000   (QPI     mode)
         * w25q128jv      0x03   0x00   0x02   0xd8   0xc7   0x001870ef   0x100   0x10000   0x01000000
         * w25q256fv/jv   0x03   0xeb   0x02   0xd8   0xc7   0x001940ef   0x100   0x10000   0x02000000
         * w25q256fv      0x03   0xeb   0x02   0xd8   0xc7   0x001960ef   0x100   0x10000   0x02000000   (QPI     mode)
         * w25q256jv      0x03   0x00   0x02   0xd8   0xc7   0x001970ef   0x100   0x10000   0x02000000
         * w25q512jv      0x03   0x00   0x02   0xd8   0xc7   0x002040ef   0x100   0x10000   0x04000000
         * w25q01jv       0x13   0x00   0x12   0xdc   0xc7   0x002140ef   0x100   0x10000   0x08000000
         * w25q01jv-dtr   0x03   0xeb   0x02   0xd8   0xc7   0x002170ef   0x100   0x10000   0x08000000
         * w25q02jv-dtr   0x03   0xeb   0x02   0xd8   0xc7   0x002270ef   0x100   0x10000   0x10000000
         */

        // Done
        return _ib.link();
    }

    // NOTE : This instruction clobbers SB!
    // NOTE : After reset, the flash must be reinitialized!
    private long[][] _instruction_softwareReset() throws JXMFatalLogicError
    {
        _pi_waitBusy();

        _qspi.geninstruction_qspiCommand( _qspi.newQSPICmd(InstMode._1Line, W25Q_ENABLE_RESET) ); _ib.delayMS ( 1 );
        _qspi.geninstruction_qspiCommand( _qspi.newQSPICmd(InstMode._1Line, W25Q_RESET       ) ); _ib.delayMS ( 5 );

        return _ib.link();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : This instruction clobbers SB!
    private long[][] _instruction_chipErase() throws JXMFatalLogicError
    {
        _pi_waitBusy   (                   );
        _pi_writeEnable( true              );

        _qspi.geninstruction_qspiCommand( _qspi.newQSPICmd(InstMode._1Line, W25Q_CHIP_ERASE) );

        _pi_waitBusy   ( _TIMEOUT_ERASE_MS );

        return _ib.link();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int ReadPage_Loop = 0;
    private static int ReadPage_DMA  = 1;
    private static int ReadPage_MMap = 2;

    // NOTE : This instruction writes to DB!
    private long[][] _instruction_readPage_impl(final int readPageMode) throws JXMFatalLogicError
    {
        final String lblSmall = _ib.uniqueLabelCounter("w25q_rp_small_%=" );
        final String lblDone  = _ib.uniqueLabelCounter("w25q_rp_done_%="  );

        final QSPICmd_Common cmdL = _qspi.newQSPICmd(
                                        InstMode._1Line , W25Q_FAST_READ_QUAD_IO_4B ,
                                        AddrMode._4Lines, AddrSize._32Bits          , _xviW25Q_FlashAddress.xviEnc(),
                                        DataMode._4Lines, _qspi.flashPageSizeBytes(), 6
                                    );

        final QSPICmd_Common cmdS = _qspi.newQSPICmd(
                                        InstMode._1Line , W25Q_FAST_READ_QUAD_IO    ,
                                        AddrMode._4Lines, AddrSize._24Bits          , _xviW25Q_FlashAddress.xviEnc(),
                                        DataMode._4Lines, _qspi.flashPageSizeBytes(), 6
                                    );

                                                    _pi_waitBusy                         (                                );

                                                    _ib.jmpIfZero                        ( _xviW25Q_LargeSize , lblSmall  );
            if(readPageMode == ReadPage_Loop  )     _qspi.geninstruction_qspiReceiveDB   ( cmdL, _xviW25Q_CurOperInitFlag ); // >  128MB
            if(readPageMode == ReadPage_DMA   )     _qspi.geninstruction_qspiReceiveDMA  ( cmdL, _xviW25Q_CurOperInitFlag ); // ---
            if(readPageMode == ReadPage_MMap  )     _qspi.geninstruction_qspiMemoryMapped( cmdL                           ); // ---
                                                    _ib.jmp                              ( lblDone                        );
                                                _ib.label                                ( lblSmall                       );
            if(readPageMode == ReadPage_Loop  )     _qspi.geninstruction_qspiReceiveDB   ( cmdS, _xviW25Q_CurOperInitFlag ); // <= 128MB
            if(readPageMode == ReadPage_DMA   )     _qspi.geninstruction_qspiReceiveDMA  ( cmdS, _xviW25Q_CurOperInitFlag ); // ---
            if(readPageMode == ReadPage_MMap  )     _qspi.geninstruction_qspiMemoryMapped( cmdS                           ); // ---
                                                _ib.label                                ( lblDone                        );

            if(readPageMode == ReadPage_Loop  )     _ib.unpackDB_32_4X8                  (                                ); // Because 'geninstruction_qspiReceiveDB()' reads in multiple of 32 bits

        return _ib.link();
    }

    // NOTE : This instruction writes to DB!
    private long[][] _instruction_readPage() throws JXMFatalLogicError
    { return _instruction_readPage_impl(ReadPage_Loop); }

    // NOTE : This instruction writes to DB!
    private long[][] _instruction_readPageDMA() throws JXMFatalLogicError
    { return _instruction_readPage_impl(ReadPage_DMA ); }

    // NOTE : This instruction writes to DB!
    private long[][] _instruction_readPageMMap() throws JXMFatalLogicError
    { return _instruction_readPage_impl(ReadPage_MMap); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int WritePage_Loop = 0;
    private static int WritePage_DMA  = 1;
    private static int WritePage_MMap = 2;

    // NOTE : This instruction reads from DB!
    private long[][] _instruction_writePage_impl(final int writePageMode) throws JXMFatalLogicError
    {
        // Writing using memory-mapped mode is not supported by QSPI
        if(writePageMode == WritePage_MMap) return null;

        final String lblSmall = _ib.uniqueLabelCounter("w25q_wp_small_%=" );
        final String lblDone  = _ib.uniqueLabelCounter("w25q_wp_done_%="  );

        final QSPICmd_Common cmdL = _qspi.newQSPICmd(
                                        InstMode._1Line , W25Q_PAGE_PROGRAM_QUAD_INP_4B,
                                        AddrMode._1Line , AddrSize._32Bits             , _xviW25Q_FlashAddress.xviEnc(),
                                        DataMode._4Lines, _qspi.flashPageSizeBytes()
                                    );

        final QSPICmd_Common cmdS = _qspi.newQSPICmd(
                                        InstMode._1Line , W25Q_PAGE_PROGRAM_QUAD_INP   ,
                                        AddrMode._1Line , AddrSize._24Bits             , _xviW25Q_FlashAddress.xviEnc(),
                                        DataMode._4Lines, _qspi.flashPageSizeBytes()
                                    );

            if(writePageMode == WritePage_Loop)     _ib.packDB_4X8_32                    (                                ); // Because 'geninstruction_qspiTransmitDB()' reads in multiple of 32 bits

                                                    _pi_waitBusy                         (                                );
                                                    _pi_writeEnable                      ( true                           );
                                                    _pi_waitBusy                         (                                );

                                                    _ib.jmpIfZero                        ( _xviW25Q_LargeSize , lblSmall  );
            if(writePageMode == WritePage_Loop)     _qspi.geninstruction_qspiTransmitDB  ( cmdL, _xviW25Q_CurOperInitFlag ); // >  128MB
            if(writePageMode == WritePage_DMA )     _qspi.geninstruction_qspiTransmitDMA ( cmdL, _xviW25Q_CurOperInitFlag ); // ---
                                                    _ib.jmp                              ( lblDone                        );
                                                 _ib.label                               ( lblSmall                       );
            if(writePageMode == WritePage_Loop)     _qspi.geninstruction_qspiTransmitDB  ( cmdS, _xviW25Q_CurOperInitFlag ); // <= 128MB
            if(writePageMode == WritePage_DMA )     _qspi.geninstruction_qspiTransmitDMA ( cmdS, _xviW25Q_CurOperInitFlag ); // ---
                                                 _ib.label                               ( lblDone                        );
                                                    _pi_waitBusy                         (                                );

        return _ib.link();
    }

    // NOTE : This instruction reads from DB!
    private long[][] _instruction_writePage() throws JXMFatalLogicError
    { return _instruction_writePage_impl(WritePage_Loop); }

    // NOTE : This instruction reads from DB!
    private long[][] _instruction_writePageDMA() throws JXMFatalLogicError
    { return _instruction_writePage_impl(WritePage_DMA); }

    // NOTE : This instruction reads from DB!
    private long[][] _instruction_writePageMMap() throws JXMFatalLogicError
    { return _instruction_writePage_impl(WritePage_MMap); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long[][] _instruction_endMMap() throws JXMFatalLogicError
    {
        _qspi.geninstruction_qspiAbort();

        return _ib.link();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public SWDExecInstBuilder swdExecInstBuilder()
    { return _ib; }

    @Override
    public long[][] instruction_resetHaltInitClock()
    { return _qspi._instruction_resetHaltInitClock; }

    @Override
    public long[][] instruction_initGPIO_XF()
    { return _qspi._instruction_initGPIO_XF; }

    @Override
    public long[][] instruction_initFlash()
    { return _instruction_initFlash; }

    @Override
    public long[][] instruction_softwareReset()
    { return _instruction_softwareReset; }

    @Override
    public long[][] instruction_chipErase()
    { return _instruction_chipErase; }

    @Override
    public long[][] instruction_readPage()
    { return _instruction_readPage; }

    @Override
    public long[][] instruction_readPageDMA()
    { return _instruction_readPageDMA; }

    @Override
    public long[][] instruction_readPageMMap()
    { return _instruction_readPageMMap; }

    @Override
    public long[][] instruction_writePage()
    { return _instruction_writePage; }

    @Override
    public long[][] instruction_writePageDMA()
    { return _instruction_writePageDMA; }

    @Override
    public long[][] instruction_writePageMMap()
    { return _instruction_writePageMMap; }

    @Override
    public long[][] instruction_endMMap()
    { return _instruction_endMMap; }

    @Override
    public long[][] instruction_deinitAll()
    { return _qspi._instruction_deinitAll; }

} // class STM32QSPI_Flash_W25Q
