/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgBootSAMBA.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'xxx*()' functions ??? #####


public static Config ATSAM3A4()
{
    final Config config = new Config();

    config.memoryFlash.totalSize       = 262144;
    config.memoryFlash.pageSize        =    256;
    config.memoryFlash.numPages        =   1024;

    config.memoryEEPROM.totalSize      = 0;

    //                                                3A4C
    config.mcuInfo.chipID              = new long[] { 0x283B0960L };

    config.appletInfo.flashDriver      = "EEFC";
    config.appletInfo.flashBase        = 0x00080000L;
    config.appletInfo.flashNumPlanes   =  2;
    config.appletInfo.flashNumLockRegs = 16;

    config.appletInfo.sramAppletStart  = 0x20001000L;
    config.appletInfo.sramAppletStack  = 0x20008000L;

    config.appletInfo.regBase          = 0x400E0A00L;
    config.appletInfo.regReset         = 0x400E1A00L;
    config.appletInfo.regResetCommand  = 0xA500000DL;

    config.appletInfo.canBrownout      = false;
    config.appletInfo.canBootFlash     = true;

    return config;
}

public static Config ATSAM3A8()
{
    final Config config = ATSAM3A4();

    config.memoryFlash.totalSize       = 524288;
    config.memoryFlash.numPages        =   2048;

    //                                                3A8C
    config.mcuInfo.chipID              = new long[] { 0x283E0A60L };

    config.appletInfo.flashNumLockRegs = 32;

    config.appletInfo.sramAppletStack  = 0x20010000L;

    return config;
}

public static Config ATSAM3X4()
{
    final Config config = ATSAM3A4();

    //                                                3X4C         3X4E
    config.mcuInfo.chipID              = new long[] { 0x284B0960L, 0x285B0960L };

    return config;
}

public static Config ATSAM3X8()
{
    final Config config = ATSAM3A8();

    //                                                3X8C         3X8E         3X8H
    config.mcuInfo.chipID              = new long[] { 0x284E0A60L, 0x285E0A60L, 0x286E0A60L };

    return config;
}

public static Config ArduinoDue  () { return ATSAM3X8(); }
public static Config DiymoreDueR3() { return ATSAM3X8(); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATSAMD21x15()
{
    final Config config = new Config();

    config.memoryFlash.totalSize       = 32768;
    config.memoryFlash.pageSize        =    64;
    config.memoryFlash.numPages        =   512;

    config.memoryEEPROM.totalSize      = 0;

    config.mcuInfo.deviceID            = new long[] {
                                             0x1001000DL, // E15A
                                             0x10010027L, // E15B
                                             0x10010056L, // E15B WLCSP
                                             0x10010063L, // E15C WLCSP
                                             0x10010008L, // G15A
                                             0x10010024L, // G15B
                                             0x10010003L, // J15A
                                             0x10010021L  // J15B
                                         };

    config.appletInfo.flashDriver      = "D2X";
    config.appletInfo.flashBase        = 0x00000000L;
    config.appletInfo.flashNumPlanes   =  1;
    config.appletInfo.flashNumLockRegs = 16;

    config.appletInfo.flashBLStart     = 0x00000000L; // NOTE : Please adjust this as required
    config.appletInfo.flashBLSize      = 0x2000;

    config.appletInfo.sramAppletStart  = 0x20000800L;
    config.appletInfo.sramAppletStack  = 0x20001000L;

    config.appletInfo.regBase          = 0x41004000L;
    config.appletInfo.regReset         = 0xE000ED0CL;
    config.appletInfo.regResetCommand  = 0x05FA0004L;

    config.appletInfo.canBrownout      = true;
    config.appletInfo.canBootFlash     = false;

    return config;
}

public static Config ATSAMD21x16()
{
    final Config config = ATSAMD21x15();

    config.memoryFlash.totalSize       = 65536;
    config.memoryFlash.numPages        =  1024;

    config.mcuInfo.deviceID            = new long[] {
                                             0x1001000CL, // E16A
                                             0x10010026L, // E16B
                                             0x10010055L, // E16B WLCSP
                                             0x10010062L, // E16C WLCSP
                                             0x10010007L, // G16A
                                             0x10010023L, // G16B
                                             0x10010002L, // J16A
                                             0x10010020L  // J16B
                                         };

    config.appletInfo.sramAppletStart  = 0x20001000L;
    config.appletInfo.sramAppletStack  = 0x20002000L;

    return config;
}

public static Config ATSAMD21x17()
{
    final Config config = ATSAMD21x15();

    config.memoryFlash.totalSize       = 131072;
    config.memoryFlash.numPages        =   2048;

    config.mcuInfo.deviceID            = new long[] {
                                             0x1001000BL, // E17A
                                             0x10010006L, // G17A
                                             0x10010010L, // G17A WLCSP
                                             0x10010001L  // J17A
                                         };

    config.appletInfo.sramAppletStart  = 0x20002000L;
    config.appletInfo.sramAppletStack  = 0x20004000L;

    return config;
}

public static Config ATSAMD21x18()
{
    final Config config = ATSAMD21x15();

    config.memoryFlash.totalSize       = 262144;
    config.memoryFlash.numPages        =   4096;

    config.mcuInfo.deviceID            = new long[] {
                                             0x1001000AL, // E18A
                                             0x10010005L, // G18A
                                             0x1001000FL, // G18A WLCSP
                                             0x10010000L  // J18A
                                         };

    config.appletInfo.sramAppletStart  = 0x20004000L;
    config.appletInfo.sramAppletStack  = 0x20008000L;

    return config;
}

public static Config ArduinoZero       () { return ATSAMD21x18(); }
public static Config ArduinoMKRZero    () { return ATSAMD21x18(); }

public static Config SeeeduinoXIAO     () { return ATSAMD21x18(); }
public static Config AdafruitFeatherM0 () { return ATSAMD21x18(); }
public static Config SparkFunSAMD21Mini() { return ATSAMD21x18(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
