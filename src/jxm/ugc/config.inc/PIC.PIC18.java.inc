/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgPIC18.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'PIC18*()' functions ??? #####


public static Config PIC18F2520()
{
    // Instantiate the configuration object
    final Config config = new Config(SubPart.F, Mode.LVSimple, REG_PIC_A6_A9_AA_A8, true);

    // Set the specification
    config.memoryFlash.totalSize        = 16384 * 2;
    config.memoryFlash.eraseBlockSize   =    32 * 2;
    config.memoryFlash.writeBlockSize   =    16 * 2;

    config.memoryEEPROM.totalSize       = 256;
    config.memoryEEPROM.writeBlockSizeE = 0;   // NOTE : PIC18 does not support EICSP
    config.memoryEEPROM.addressEnd      = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;

    config.memoryConfigBytes.addressEnd = config.memoryConfigBytes.addressBeg + 14 - 1;
    config.memoryConfigBytes.address    = new long[] { -1, 0x300001  , 0x300002  , 0x300003  , -1, 0x300005  , 0x300006  , -1, 0x300008  , 0x300009  , 0x30000A  , 0x30000B  , 0x30000C  , 0x30000D   };
    config.memoryConfigBytes.size       = new int [] { -1, 1         , 1         , 1         , -1, 1         , 1         , -1, 1         , 1         , 1         , 1         , 1         , 1          };
    config.memoryConfigBytes.bitMask    = new long[] { -1, 0b11001111, 0b00011111, 0b00011111, -1, 0b10000111, 0b11000101, -1, 0b00001111, 0b11000000, 0b00001111, 0b11100000, 0b00001111, 0b01000000 };

    // Return the configuration object
    return config;
}

public static Config PIC18F4520()
{ return PIC18F2520(); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC18FxxJxx(final int flashKSize, final boolean f46j11)
{
    // Instantiate the configuration object
    final Config config = new Config(SubPart.FJ, Mode.LVEntrySeq, REG_PIC_A6_A9_AA_A8, true);

    // Set the specification
    config.baseProgSpec.bulkEraseNbD    = true;

    config.memoryFlash.totalSize        = flashKSize * 2 * 1024;
    config.memoryFlash.eraseBlockSize   =        512 * 2;
    config.memoryFlash.writeBlockSize   =         32 * 2;

    config.memoryEEPROM.addressBeg      = -1; // NOTE : This PIC18 series does not have embedded EEPROM
    config.memoryEEPROM.addressEnd      = -1;
    config.memoryEEPROM.totalSize       = -1;

    final long cb0bitMask = !f46j11 ? 0b11101111 : 0b11100001;
    final long cb1bitMask = !f46j11 ? 0b00000111 : 0b00000100;
    final long cb6bitMask = (flashKSize * 2 > 32) ? 0b11111111
                          : (flashKSize * 2 > 16) ? 0b11011111
                          :                         0b11001111;

    /*
     * NOTE : # The configuration register address of this PIC18 series starts at address 0x300000.
     *          These registers are implemented as volatile memory registers. The data for these
     *          registers is taken from the four flash configuration words located at the end of the
     *          program memory space.
     *        # In the HEX file, the data for these registers is embedded at the end of the program
     *          space and thus will be programmed simultaneously when programming the flash.
     *
     * ##### ??? TODO : Is there a better method ??? #####
     */
    final long nb = 8;
    final long sa = config.memoryFlash.address + config.memoryFlash.totalSize - nb;
    config.memoryConfigBytes.addressBeg = sa;
    config.memoryConfigBytes.addressEnd = sa + nb - 1;
    config.memoryConfigBytes.address    = new long[] { sa + 0    , sa + 1    , sa + 2    , sa + 3    , sa + 4    , sa + 5    , sa + 6    , sa + 7     };
    config.memoryConfigBytes.size       = new int [] { 1         , 1         , 1         , 1         , 1         , 1         , 1         , 1          };
    config.memoryConfigBytes.bitMask    = new long[] { cb0bitMask, cb1bitMask, 0b11011111, 0b00001111, 0b11111111, 0b00001001, cb6bitMask, 0b00000001 };
    config.memoryConfigBytes.orgMask    = new long[] { 0b00000000, 0b11110000, 0b00000000, 0b11110000, 0b00000000, 0b11110000, 0b00000000, 0b11110000 };

    /*
    config.memoryConfigBytes.addressEnd     = config.memoryConfigBytes.addressBeg + 8 - 1;
    config.memoryConfigBytes.address        = new long[] { 0x300000  , 0x300001  , 0x300002  , 0x300003  , 0x300004  , 0x300005  , 0x300006  , 0x300007   };
  //config.memoryConfigBytes.addressMulFW   = 0;
  //config.memoryConfigBytes.addressOfsFW   = 0x00003FF8L;
    */

    // Return the configuration object
    return config;
}

public static Config PIC18F24J11() { return PIC18FxxJxx( 8, true ); }
public static Config PIC18F25J11() { return PIC18FxxJxx(16, true ); }
public static Config PIC18F26J11() { return PIC18FxxJxx(32, true ); }
public static Config PIC18F44J11() { return PIC18FxxJxx( 8, true ); }
public static Config PIC18F45J11() { return PIC18FxxJxx(16, true ); }
public static Config PIC18F46J11() { return PIC18FxxJxx(32, true ); }

public static Config PIC18F24J50() { return PIC18FxxJxx( 8, false); }
public static Config PIC18F25J50() { return PIC18FxxJxx(16, false); }
public static Config PIC18F26J50() { return PIC18FxxJxx(32, false); }
public static Config PIC18F44J50() { return PIC18FxxJxx( 8, false); }
public static Config PIC18F45J50() { return PIC18FxxJxx(16, false); }
public static Config PIC18F46J50() { return PIC18FxxJxx(32, false); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC18F25K50()
{
    // Instantiate the configuration object
    final Config config = new Config(SubPart.FK, Mode.LVEntrySeq, REG_PIC_A6_A9_AA_A8, true);

    // Set the specification
    config.memoryFlash.totalSize        = 16384 * 2;
    config.memoryFlash.eraseBlockSize   =    32 * 2;
    config.memoryFlash.writeBlockSize   =    32 * 2;

    config.memoryEEPROM.totalSize       = 256;
    config.memoryEEPROM.writeBlockSizeE = 0;   // NOTE : PIC18 does not support EICSP
    config.memoryEEPROM.addressEnd      = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;

    config.memoryConfigBytes.addressEnd = config.memoryConfigBytes.addressBeg + 14 - 1;
    config.memoryConfigBytes.address    = new long[] { 0x300000  , 0x300001  , 0x300002  , 0x300003  , -1, 0x300005  , 0x300006  , -1, 0x300008  , 0x300009  , 0x30000A  , 0x30000B  , 0x30000C  , 0x30000D   };
    config.memoryConfigBytes.size       = new int [] { 1         , 1         , 1         , 1         , -1, 1         , 1         , -1, 1         , 1         , 1         , 1         , 1         , 1          };
    config.memoryConfigBytes.bitMask    = new long[] { 0b00111011, 0b11101111, 0b01011111, 0b00111111, -1, 0b11010011, 0b11100101, -1, 0b00001111, 0b11000000, 0b00001111, 0b11100000, 0b00001111, 0b01000000 };

    // Return the configuration object
    return config;
}

public static Config PIC18F45K50()
{ return PIC18F25K50(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
