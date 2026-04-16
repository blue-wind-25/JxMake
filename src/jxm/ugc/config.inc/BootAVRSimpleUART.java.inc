/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgBootSTK500.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'xxx*()' functions ??? #####


public static Config ATtiny13()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 1024;
    config.memoryFlash.pageSize   =   32;
    config.memoryFlash.numPages   =   32;

    config.memoryEEPROM.totalSize =   64;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega8A()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    return config;
}

public static Config ATmega16A()
{
    final Config config = ATmega8A();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;

    return config;
}

public static Config ATmega32A()
{
    final Config config = ATmega16A();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    return config;
}

public static Config ATmega64A()
{
    final Config config = ATmega32A();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;

    config.memoryEEPROM.totalSize =  2048;

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

    return config;
}

public static Config ATmega88P()
{
    final Config config = ATmega48P();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

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

    return config;
}

public static Config ArduinoUnoR3     () { return ATmega328P(); }
public static Config ArduinoUnoRev3   () { return ATmega328P(); }
public static Config ArduinoNano      () { return ATmega328P(); }
public static Config ArduinoProMini168() { return ATmega168P(); }
public static Config ArduinoProMini328() { return ATmega328P(); }
public static Config ArduinoProMini   () { return ATmega328P(); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega640()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  4096;

    return config;
}

public static Config ATmega1280()
{
    final Config config = ATmega640();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    return config;
}

public static Config ATmega2560()
{
    final Config config = ATmega1280();

    config.memoryFlash.totalSize  = 262144;
    config.memoryFlash.numPages   =   1024;

    return config;
}

public static Config ArduinoMega1280() { return ATmega1280(); }
public static Config ArduinoMega2560() { return ATmega2560(); }
public static Config ArduinoMega    () { return ATmega2560(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
