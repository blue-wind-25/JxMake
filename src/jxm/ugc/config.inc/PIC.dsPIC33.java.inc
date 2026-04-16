/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgDSPIC33.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'dsPIC33*()' functions ??? #####


public static Config dsPIC33EPxxGS202(final int flashBSize, final int cwAStart)
{
    // Instantiate the configuration object
    final Config config = new Config(SubPart.EP, Mode.LVEntrySeqS);

    // Set the specification
    /* NOTE: #  ICSP erases the entire device at once and writes  2 words at once.
     *       # EICSP erases 512 words at once         and writes 64 words at once.
     *
     * ##### ??? TODO : Exclude the last page from this specifier because it contains the configuration bits ??? #####
     */
    config.memoryFlash.totalSize          = flashBSize * 3;
    config.memoryFlash.writeBlockSize     =   2        * 3; // NOTE : In  ICSP mode, dsPIC33 writes 2 words at once (6 bytes)
    config.memoryFlash.eraseBlockSize     =   2        * 3; // NOTE : In  ICSP mode, it does not matter as long as it is multiple of 'writeBlockSize' and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSizeE    =  64        * 3; // NOTE : In EICSP mode, dsPIC33 writes  64 words at once ( 192 bytes)
    config.memoryFlash.eraseBlockSizeE    = 512        * 3; // NOTE : In EICSP mode, dsPIC33 erases 512 words at once (1536 bytes)

    config.memoryEEPROM.totalSize         = 0;

    config.memoryConfigBytes.addressBeg   = cwAStart;
    config.memoryConfigBytes.addressEnd   = config.memoryConfigBytes.addressBeg + 64 * 2;

    /*
     * NOTE : # The configuration bits of this dsPIC33 series are implemented as volatile memory.
     *          Each time the device is powered up, they will be loaded from a location at the end
     *          of the program memory space.
     *        # However, in the HEX file, the data are located at a DIFFERENT ADDRESS (higher than
     *          the end address of the program memory space. Therefore, they will NOT be programmed
     *          simultaneously when programming the flash.
     *
     * ##### ??? TODO : Is there a better method ??? #####
     */
    config.memoryConfigBytes.address       = new long[] { cwAStart + 0x00   , cwAStart + 0x10   , cwAStart + 0x14   , cwAStart + 0x18   , cwAStart + 0x1C   , cwAStart + 0x20   , cwAStart + 0x24   , cwAStart + 0x28   , cwAStart + 0x2C   , cwAStart + 0x30    };
    config.memoryConfigBytes.size          = new int [] { 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                  };
    config.memoryConfigBytes.bitMask       = new long[] { 0b1000111111101111, 0b0001111111111111, 0b1000000000000000, 0b0000000010000111, 0b0000000111100111, 0b0000001111111111, 0b0000000000000001, 0b0000000010100011, 0b0000000000000101, 0b0000000001110111 };
    config.memoryConfigBytes.clrMask       = new long[] { 0b0000000000000000, 0b0000000000000000, 0b1000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000 };
    config.memoryConfigBytes.setMask       = new long[] { 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000001, 0b0000000010000000, 0b0000000000000100, 0b0000000000000000 };
    config.memoryConfigBytes.maxTotalSize  = 64 * 3;  // NOTE : The configuration words are not contiguous within the address space!
    config.memoryConfigBytes.addressMulFW  = 2;
    config.memoryConfigBytes.addressOfsFW  = 0;

    config.memoryPE.address                = 0x800000;
    config.memoryPE.totalSize              = 1536 * 3;
    config.memoryPE.pageSize               =  512 * 3;
    config.memoryPE.numPages               =  3;
    config.memoryPE.saveWordOffset         = -1;
    config.memoryPE.saveWordCount          =  0;

    config.cmdPE.ERASEP                    = 0x9; // This device needs erase before writing

    config.cmdPE.READD                     = -1;  // Not available in this device
    config.cmdPE.PROGD                     = -1;  // ---

    config.cmdPE.waitDelay_MS_EraseProgMem = 25;
    config.cmdPE.waitDelay_MS_WriteProgMem =  1;

    // Return the configuration object
    return config;
}

public static Config dsPIC33EP16GS202() { return dsPIC33EPxxGS202( 5312 + 320, 0x2B80); }
public static Config dsPIC33EP32GS202() { return dsPIC33EPxxGS202(10944 + 320, 0x5780); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
