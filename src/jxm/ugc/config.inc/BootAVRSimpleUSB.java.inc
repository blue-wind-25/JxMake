/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgBootAVR109.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'xxx*()' functions ??? #####


public static Config ATmega8U2()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =  128;
    config.memoryFlash.numPages   =   64;

    config.memoryEEPROM.totalSize =  512;

    return config;
}

public static Config ATmega16U2()
{
    final Config config = ATmega8U2();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.numPages   =   128;

    config.memoryEEPROM.totalSize =   512;

    return config;
}

public static Config ATmega32U2()
{
    final Config config = ATmega16U2();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega16U4           () { return ATmega16U2(); }
public static Config ATmega32U4           () { return ATmega32U2(); }

public static Config ArduinoLeonardo      () { return ATmega32U4(); }
public static Config SparkFunProMicro     () { return ATmega32U4(); }
public static Config AdafruitTrinket      () { return ATmega32U4(); }
public static Config AdafruitItsyBitsy32U4() { return ATmega32U4(); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config AT90USB82()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =  128;
    config.memoryFlash.numPages   =   64;

    config.memoryEEPROM.totalSize =  512;

    return config;
}

public static Config AT90USB162()
{
    final Config config = AT90USB82();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.numPages   =   128;


    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config AT90USB646()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  2048;

    return config;
}

public static Config AT90USB1286()
{
    final Config config = AT90USB646();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    config.memoryEEPROM.totalSize =   4096;

    return config;
}

public static Config AT90USB647   () { return AT90USB646 (); }
public static Config AT90USB1287  () { return AT90USB1286(); }

public static Config DTAVRInoduino() { return AT90USB1286(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
