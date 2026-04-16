/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgSWIM.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'STM8*()' functions ??? #####


public static ConfigSTM8S STM8S003()
{
    final ConfigSTM8S config = new ConfigSTM8S();

    config.memoryFlash.address    = 0x8000;
    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.address   = 0x4000;
    config.memoryEEPROM.totalSize = 128;
    config.memoryEEPROM.pageSize  =  64;
    config.memoryEEPROM.numPages  =   2;

    return config;
}

public static ConfigSTM8S STM8S103()
{
    final ConfigSTM8S config = new ConfigSTM8S();

    config.memoryFlash.address    = 0x8000;
    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.address   = 0x4000;
    config.memoryEEPROM.totalSize = 640;
    config.memoryEEPROM.pageSize  =  64;
    config.memoryEEPROM.numPages  =  10;

    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
