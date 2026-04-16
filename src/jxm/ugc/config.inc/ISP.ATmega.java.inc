/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgISP.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'ATmega*()' functions ??? #####


public static Config ATmega328P()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.pageSize   =   128;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    return config;
}

public static Config ArduinoUnoR3  () { return ATmega328P(); }
public static Config ArduinoUnoRev3() { return ATmega328P(); }
public static Config ArduinoNano   () { return ATmega328P(); }
public static Config ArduinoProMini() { return ATmega328P(); }
public static Config ArduinoMicro  () { return ATmega328P(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
