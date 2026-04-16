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


public static Config ATxmega16A4U()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   256;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize =  1024;
    config.memoryEEPROM.pageSize  =    32;
    config.memoryEEPROM.numPages  =    32;

    return config;
}

public static Config ATxmega32A4U()
{
    final Config config = ATxmega16A4U();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   128;

    return config;
}

public static Config ATxmega64A4U()
{
    final Config config = ATxmega16A4U();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   256;

    return config;
}

public static Config ATxmega128A4U()
{
    final Config config = ATxmega16A4U();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
