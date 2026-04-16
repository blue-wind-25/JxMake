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

// ##### ??? TODO : Add more specific-part 'ATtiny*()' functions ??? #####


public static Config ATtiny3226()
{
    final Config config = new Config();

    config.memorySignatureExt.address_Osc20Err3V = -1;
    config.memorySignatureExt.bitMask_Osc20Err3V =  0;
    config.memorySignatureExt.address_Osc20Err5V = -1;
    config.memorySignatureExt.bitMask_Osc20Err5V =  0;

    config.memoryFlash.address    = 0x8000;
    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.pageSize   =   128;
    config.memoryFlash.numPages   =   256;

    //                                          0           1           2           3   4   5           6           7           8           9
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     append      bootend
    config.memoryFuse.bitMask[5]  =                                                         0b11011101                                          ;
    config.memoryFuse.setMask     = new int[] { 0b00000000, 0b00000000, 0b01111100, -1, -1, 0b00100010, 0b11111000, 0b00000000, 0b00000000, -1 };
    //                                                                                      │
    //                                                                                      └→ be careful with this one

    config.memoryEEPROM.totalSize = 256;
    config.memoryEEPROM.pageSize  =  64;
    config.memoryEEPROM.numPages  =   4;

    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
