/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgDSPIC30.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'dsPIC30*()' functions ??? #####

// ##### !!! TODO : dsPIC30F SMPS MCUs do not support HVP and use LVEntrySeq with different keys for ICSP and EICSP !!! #####


public static Config dsPIC30Fx0xx(final int flashKSize, final int eepromBSize)
{
    // Instantiate the configuration object
    final Config config = new Config(SubPart.F, true ? Mode.HVPulsedNOP : Mode.HVSimple);

    config.memoryFlash.totalSize           = flashKSize * 1024 * 3;
    config.memoryFlash.writeBlockSize      = 32                * 3; // NOTE : dsPIC30 writes 32 words at once (96 bytes)
    config.memoryFlash.eraseBlockSize      = 32                * 3; // NOTE : It does not matter as long as it is multiple of 'writeBlockSize' and smaller than the 'totalSize'

    config.memoryEEPROM.totalSize          = eepromBSize;
    config.memoryEEPROM.writeBlockSizeE    = 16                * 2; // NOTE : dsPIC30 writes 16 words at once (32 bytes) - EICSP mode only
    config.memoryEEPROM.addressBeg         = 0x7FFC00;
    config.memoryEEPROM.addressEnd         = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;
    config.memoryEEPROM.addressMulFW       = 2;
    config.memoryEEPROM.addressOfsFW       = 0;

    config.memoryPE.address                = 0x800000;
    config.memoryPE.totalSize              = 736 * 3;
    config.memoryPE.pageSize               = config.memoryPE.totalSize;
    config.memoryPE.numPages               =  1;
    config.memoryPE.saveWordOffset         = -1;
    config.memoryPE.saveWordCount          =  0;

    config.cmdPE.ERASEP                    = 0x9; // This device needs erase before writing
    config.cmdPE.ERASED                    = 0x8; // ---

    config.cmdPE.waitDelay_MS_EraseProgMem = 3;
    config.cmdPE.waitDelay_MS_WriteProgMem = 3;

    config.cmdPE.waitDelay_MS_EraseDataMem = 3;
    config.cmdPE.waitDelay_MS_WriteDataMem = 3;

    // Return the configuration object
    return config;
}

public static Config dsPIC30F3010()
{
    final Config config   = dsPIC30Fx0xx(8, 1024);
    final int    cwAStart = 0xF80000;

    config.memoryConfigBytes.addressBeg   = cwAStart;
    config.memoryConfigBytes.addressEnd   = config.memoryConfigBytes.addressBeg + 14 * 2;
                                                    //   FOSC                FWDT                FBORPOR             FBS                 FSS                 FGS                 FICD
    config.memoryConfigBytes.address      = new long[] { cwAStart + 0x0    , cwAStart + 0x2    , cwAStart + 0x4    , cwAStart + 0x6    , cwAStart + 0x8    , cwAStart + 0xA    , cwAStart + 0xC     };
    config.memoryConfigBytes.size         = new int [] { 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                  };
    config.memoryConfigBytes.bitMask      = new long[] { 0b1100011100011111, 0b1000000000111111, 0b1000011110110011, 0b0011000100001111, 0b0011001100001111, 0b0000000000000111, 0b1100000000000011 };
    config.memoryConfigBytes.orgMask      = new long[] { 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0011000100001111, 0b0011001100001110, 0b0000000000000100, 0b0000000000000000 };
    config.memoryConfigBytes.maxTotalSize = 14 * 2;
    config.memoryConfigBytes.addressMulFW = 2;
    config.memoryConfigBytes.addressOfsFW = 0;

    return config;
}

public static Config dsPIC30F3011()
{ return dsPIC30F3010(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
