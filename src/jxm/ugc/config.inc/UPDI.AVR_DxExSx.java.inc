/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgUPDI.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!


public static Config _AVR_Dx_Ex_Sx_generic(final int memorySignature_address)
{
    final Config config = new Config();

    if(memorySignature_address >= 0) config.memorySignature.address = memorySignature_address;

    config.memorySignatureExt.address_SerNum       = config.memorySignature.address + 0x10;
    config.memorySignatureExt.size_SerNum          = 16;

    config.memorySignatureExt.address_OscCal16     = -1;
    config.memorySignatureExt.bitMask_OscCal16     =  0;
    config.memorySignatureExt.address_OscCal16TCal = -1;
    config.memorySignatureExt.bitMask_OscCal16TCal =  0;

    config.memorySignatureExt.address_OscCal20     = -1;
    config.memorySignatureExt.bitMask_OscCal20     =  0;
    config.memorySignatureExt.address_OscCal20TCal = -1;
    config.memorySignatureExt.bitMask_OscCal20TCal =  0;

    config.memorySignatureExt.address_Osc16Err3V   = -1;
    config.memorySignatureExt.bitMask_Osc16Err3V   =  0;
    config.memorySignatureExt.address_Osc16Err5V   =  0;
    config.memorySignatureExt.bitMask_Osc16Err5V   =  0;

    config.memorySignatureExt.address_Osc20Err3V   = -1;
    config.memorySignatureExt.bitMask_Osc20Err3V   =  0;
    config.memorySignatureExt.address_Osc20Err5V   = -1;
    config.memorySignatureExt.bitMask_Osc20Err5V   =  0;

    config.memorySignatureExt.bitSize_TempSense    = 16;
    config.memorySignatureExt.address_TempSense0   = config.memorySignature.address + 0x04;
    config.memorySignatureExt.bitMask_TempSense0   = 0xFFFF;
    config.memorySignatureExt.address_TempSense1   = config.memorySignature.address + 0x06;
    config.memorySignatureExt.bitMask_TempSense1   = 0xFFFF;

    config.memoryFlash.address       = 0x800000;

    config.memoryLockBits.address    = 0x1040;
    config.memoryLockBits.size       = 4;
    config.memoryLockBits.bitMaskExt = 0xFFFFFFFFL;

    return config;
}

public static Config _AVR_Dx_Ex_Sx_generic()
{ return _AVR_Dx_Ex_Sx_generic(-1); }

/*
 * ######################################## !!! WARNING !!! ########################################
 *
 * On AVR DA, DU, EB, and Sx series, UPDI will be permanently disable if:
 *     # FUSE.PDICFG[15:0] is set to 0xB452, and
 *     # LOCK[31:0] is written with any value other than 0x5CC5C55C
 *
 * This configuration is IRREVERSIBLE!
 *
 * ######################################## !!! WARNING !!! ########################################
 */

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_DA_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9   A           B
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize        pdicfg0     pdicfg1
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, 0x105A    , 0x105B     };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, 1         , 1          };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001111, -1, -1, 0b11101101, 0b00000111, 0b11111111, 0b11111111, -1, 0b11110011, 0b11111111 };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b11110000, -1, -1, 0b00010010, 0b11111000, 0b00000000, 0b00000000, -1, 0b00001100, 0b00000000 };
    //                                                                                                                                          │           │
    //                                                                                                        be VERY careful with these ones ←─┴───────────┘

    /*
    // ##### ??? TODO : Disable this feature because it CAN BE EXTREMELY dangerous on this MCU ??? #####
    config.memoryLockBits.address = 0;
    config.memoryLockBits.size    = 0;
    config.memoryLockBits.bitMask = 0;
    //*/

    return config;
}

public static Config AVR32DA()
{
    final Config config = _AVR_DA_generic();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize = 512;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 512;

    return config;
}

public static Config AVR64DA()
{
    final Config config = AVR32DA();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

public static Config AVR128DA()
{
    final Config config = AVR32DA();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    256;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_DB_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001111, -1, -1, 0b11101001, 0b00011111, 0b11111111, 0b11111111, -1, };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b11110000, -1, -1, 0b00010110, 0b11100000, 0b00000000, 0b00000000, -1, };

    return config;
}

public static Config AVR32DB()
{
    final Config config = _AVR_DB_generic();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize = 512;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 512;

    return config;
}

public static Config AVR64DB()
{
    final Config config = AVR32DB();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

public static Config AVR128DB()
{
    final Config config = AVR32DB();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    256;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_DD_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001111, -1, -1, 0b11111001, 0b00011111, 0b11111111, 0b11111111, -1, };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b11110000, -1, -1, 0b00000110, 0b11100000, 0b00000000, 0b00000000, -1, };
    //                                                                                      │
    //                                                                                      └→ be careful with this one

    return config;
}

public static Config AVR16DD()
{
    final Config config = _AVR_DD_generic();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    32;

    config.memoryEEPROM.totalSize = 256;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 256;

    return config;
}

public static Config AVR32DD()
{
    final Config config = AVR16DD();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =    64;

    return config;
}

public static Config AVR64DD()
{
    final Config config = AVR16DD();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_DU_generic()
{
    // NOTE : AVR DU MCUs use a different 'Config.memorySignature.address'
    final Config config = _AVR_Dx_Ex_Sx_generic(0x1080);

    //                                          0           1           2           3   4   5           6           7           8           9   A           B
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize        pdicfg0     pdicfg1
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, 0x105A    , 0x105B     };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, 1         , 1          };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001111, -1, -1, 0b11111011, 0b00001111, 0b11111111, 0b11111111, -1, 0b11110011, 0b11111111 };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b11110000, -1, -1, 0b00000100, 0b11110000, 0b00000000, 0b00000000, -1, 0b00001100, 0b00000000 };
    //                                                                                      │                                                   │           │
    //                                                          be careful with this one ←──┘                 be VERY careful with these ones ←─┴───────────┘

    /*
    // ##### ??? TODO : Disable this feature because it CAN BE EXTREMELY dangerous on this MCU ??? #####
    config.memoryLockBits.address = 0;
    config.memoryLockBits.size    = 0;
    config.memoryLockBits.bitMask = 0;
    //*/

    return config;
}

public static Config AVR16DU()
{
    final Config config = _AVR_DU_generic();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    32;

    config.memoryEEPROM.totalSize = 256;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 256;

    return config;
}

public static Config AVR32DU()
{
    final Config config = AVR16DU();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =    64;

    return config;
}

public static Config AVR64DU()
{
    final Config config = AVR16DU();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_EA_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1 };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0 };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001000, -1, -1, 0b11111001, 0b00000111, 0b11111111, 0b11111111, -1 };
    config.memoryFuse.setMask     = new int[] { 0b00000000, 0b00000000, 0b11110111, -1, -1, 0b00000110, 0b11111000, 0b00000000, 0b00000000, -1 };
    //                                                                                      │
    //                                                                                      └→ be careful with this one

    return config;
}

public static Config AVR16EA()
{
    final Config config = _AVR_EA_generic();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =    64;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize = 512;
    config.memoryEEPROM.pageSize  =   8;
    config.memoryEEPROM.numPages  =  64;

    return config;
}

public static Config AVR32EA()
{
    final Config config = AVR16EA();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   512;

    return config;
}

public static Config AVR64EA()
{
    final Config config = AVR16EA();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   128;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_EB_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9   A           B
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize        pdicfg0     pdicfg1
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, 0x105A    , 0x105B     };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, 1         , 1          };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001000, -1, -1, 0b11111011, 0b00000111, 0b11111111, 0b11111111, -1, 0b11110011, 0b11111111 };
    config.memoryFuse.setMask     = new int[] { 0b00000000, 0b00000000, 0b11110111, -1, -1, 0b00000100, 0b11111000, 0b00000000, 0b00000000, -1, 0b00001100, 0b00000000 };
    //                                                                                      │                                                   │           │
    //                                                          be careful with this one ←──┘                 be VERY careful with these ones ←─┴───────────┘

    /*
    // ##### ??? TODO : Disable this feature because it CAN BE EXTREMELY dangerous on this MCU ??? #####
    config.memoryLockBits.address = 0;
    config.memoryLockBits.size    = 0;
    config.memoryLockBits.bitMask = 0;
    //*/

    return config;
}

public static Config AVR16EB()
{
    final Config config = _AVR_EB_generic();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =    64;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize = 512;
    config.memoryEEPROM.pageSize  =   8; // ##### !!! TODO : VERIFY !!! #####
    config.memoryEEPROM.numPages  =  64;

    return config;
}

public static Config AVR32EB()
{
    final Config config = AVR16EB();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   512;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_SD_generic()
{
    // NOTE : AVR Sx MCUs use a different 'Config.memorySignature.address'
    final Config config = _AVR_Dx_Ex_Sx_generic(0x1080);

    //                                          0           1           2   3   4   5           6           7           8           9   A           B
    //                                          wdtcfg      bodcfg                  syscfg0     syscfg1     codesize    bootsize        pdicfg0     pdicfg1
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , -1, -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, 0x105A    , 0x105B     };
    config.memoryFuse.size        = new int[] { 1         , 1         ,  0,  0,  0, 1         , 1         , 1         , 1         ,  0, 1         , 1          };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, -1, -1, -1, 0b11000011, 0b11011111, 0b11111111, 0b11111111, -1, 0b11110011, 0b11111111 };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, -1, -1, -1, 0b00111100, 0b00100000, 0b00000000, 0b00000000, -1, 0b00001100, 0b00000000 };
    //                                                                                                                                  │           │
    //                                                                                                be VERY careful with these ones ←─┴───────────┘

    /*
    // ##### ??? TODO : Disable this feature because it CAN BE EXTREMELY dangerous on this MCU ??? #####
    config.memoryLockBits.address = 0;
    config.memoryLockBits.size    = 0;
    config.memoryLockBits.bitMask = 0;
    //*/

    return config;
}

public static Config AVR32SD()
{
    final Config config = _AVR_SD_generic();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize = 256;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 256;

    return config;
}

public static Config AVR64SD()
{
    final Config config = AVR32SD();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
