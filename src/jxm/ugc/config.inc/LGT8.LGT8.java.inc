/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgLGT8.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'LGT8*()' functions ??? #####


public static Config LGT8F328P()
{
    // Instantiate the configuration object
    final Config config = new Config();

    // Set the values
    config.memoryFlash.totalSize = 32768;
    config.memoryFlash.pageSize  =   128;
    config.memoryFlash.numPages  =   256;

    // Return the configuration object
    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
