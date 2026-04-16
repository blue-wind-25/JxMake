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


public static ConfigSTM8L STM8L151()
{
    final ConfigSTM8L config = new ConfigSTM8L(true);

    config.memoryFlash.address    = 0x8000;
    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;
    config.memoryFlash.numPages   =   128;

    config.memoryEEPROM.address   = 0x1000;
    config.memoryEEPROM.totalSize = 1024;
    config.memoryEEPROM.pageSize  =  128;
    config.memoryEEPROM.numPages  =    8;

    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
