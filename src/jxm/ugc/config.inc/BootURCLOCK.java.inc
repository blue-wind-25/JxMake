/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgBootURCLOCK.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'xxx*()' functions ??? #####

// ##### ??? TODO : How to combine this with 'BootAVRSimpleUART.java.inc' ??? #####


public static Config ATmega8A()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    config.mcuInfo.numVectors     =   19;
    config.mcuInfo.totalSRAM      = 1024;

    return config;
}

public static Config ATmega16A()
{
    final Config config = ATmega8A();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;

    config.mcuInfo.numVectors     =    21;

    return config;
}

public static Config ATmega32A()
{
    final Config config = ATmega16A();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    config.mcuInfo.numVectors     =    26;
    config.mcuInfo.totalSRAM      =  2048;

    return config;
}

public static Config ATmega64A()
{
    final Config config = ATmega32A();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;

    config.memoryEEPROM.totalSize =  2048;

    config.mcuInfo.numVectors     =    35;
    config.mcuInfo.totalSRAM      =  4096;

    return config;
}

public static Config ATmega128A()
{
    final Config config = ATmega64A();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    config.memoryEEPROM.totalSize =   4096;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega48P()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 4096;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =   64;

    config.memoryEEPROM.totalSize =  256;

    config.mcuInfo.numVectors     =   26;
    config.mcuInfo.totalSRAM      =  512;

    return config;
}

public static Config ATmega88P()
{
    final Config config = ATmega48P();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    config.mcuInfo.totalSRAM      = 1024;

    return config;
}

public static Config ATmega168P()
{
    final Config config = ATmega88P();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;

    return config;
}

public static Config ATmega328P()
{
    final Config config = ATmega168P();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    config.mcuInfo.totalSRAM      =  2048;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega164P()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;
    config.memoryFlash.numPages   =   128;

    config.memoryEEPROM.totalSize =   512;

    config.mcuInfo.numVectors     =    35;
    config.mcuInfo.totalSRAM      =  1024;

    return config;
}

public static Config ATmega324P()
{
    final Config config = ATmega164P();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    config.mcuInfo.totalSRAM      =  2048;

    return config;
}

public static Config ATmega644P()
{
    final Config config = ATmega324P();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;

    config.memoryEEPROM.totalSize =  2048;

    config.mcuInfo.totalSRAM      =  4096;

    return config;
}

public static Config ATmega1284P()
{
    final Config config = ATmega644P();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    config.memoryEEPROM.totalSize =   4096;

    config.mcuInfo.totalSRAM      =  16384;

    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
