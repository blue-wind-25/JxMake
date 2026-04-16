/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgPIC24.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'PIC24*()' functions ??? #####


public static Config PIC24FJxxGB00x(final int flashKSize, final int cwAStart)
{
    // Instantiate the configuration object
    final Config config = new Config(SubPart.FJ, Mode.LVEntrySeq);

    // Set the specification
    config.memoryFlash.totalSize          = flashKSize * 1024 * 3;
    config.memoryFlash.writeBlockSize     = 64                * 3; // NOTE : PIC24 writes 64 words at once (192 bytes)
    config.memoryFlash.eraseBlockSize     = 64                * 3; // NOTE : It does not matter as long as it is multiple of 'writeBlockSize' and smaller than the 'totalSize'

    config.memoryEEPROM.totalSize         = 0;

    config.memoryConfigBytes.addressBeg   = cwAStart;
    config.memoryConfigBytes.addressEnd   = config.memoryConfigBytes.addressBeg + 8 * 2;

    config.memoryConfigBytes.address      = new long[] { cwAStart + 0xE    , cwAStart + 0xC    , cwAStart + 0xA    , cwAStart + 0x8     };
    config.memoryConfigBytes.size         = new int [] { 2                 , 2                 , 2                 , 2                  };
    config.memoryConfigBytes.bitMask      = new long[] { 0b0111101111011111, 0b1111111111110111, 0b1110111100111111, 0b0000000011111111 };
    config.memoryConfigBytes.maxTotalSize = 8 * 2;
    config.memoryConfigBytes.addressMulFW = 2;
    config.memoryConfigBytes.addressOfsFW = 0;

    config.memoryPE.address               = 0x800000;
    config.memoryPE.totalSize             = 1024 * 3;
    config.memoryPE.pageSize              =  512 * 3;
    config.memoryPE.numPages              =  2;
    config.memoryPE.saveWordOffset        = -1;
    config.memoryPE.saveWordCount         =  0;

    config.cmdPE.READD                    = -1; // Not available in this device
    config.cmdPE.PROGD                    = -1; // ---

    /*
    config.cmdPE.waitDelay_MS_EraseProgMem = 2;
    config.cmdPE.waitDelay_MS_WriteProgMem = 2;

    config.cmdPE.waitDelay_MS_EraseDataMem = 2;
    config.cmdPE.waitDelay_MS_WriteDataMem = 2;
    //*/

    // Return the configuration object
    return config;
}

public static Config PIC24FJ32GB00x() { return PIC24FJxxGB00x(11, 0x57F0); }
public static Config PIC24FJ64GB00x() { return PIC24FJxxGB00x(22, 0xABF0); }

////////////////////////////////////////////////////////////////////////////////////////////////////

/*
// PIC24FV32KA304
config.memoryEEPROM.totalSize       = 512;
config.memoryEEPROM.writeBlockSizeE = ?;
config.memoryEEPROM.addressBeg      = 0x7FFE00;
config.memoryEEPROM.addressEnd      = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;
config.memoryEEPROM.addressMulFW    = 2;
config.memoryEEPROM.addressOfsFW    = 0;
//*/


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
