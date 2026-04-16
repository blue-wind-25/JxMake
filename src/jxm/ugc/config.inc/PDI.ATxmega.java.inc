/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgPDI.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'ATxmega*()' functions ??? #####


public static Config ATxmega16D4()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   256;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize = 1024;
    config.memoryEEPROM.pageSize  =   32;
    config.memoryEEPROM.numPages  =   32;

    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
