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


// ##### !!! TODO : VERIFY !!! #####

/*
 * NOTE : # This file is used by more than one 'ProgBoot*' implementations. However, many AN3155
 *          implementations can only handle a maximum of 256 flash pages. Therefore, if both the
 *          physical page size and erase page size are specified in the datasheet or reference manual,
 *          the larger value must be assigned to 'config.memoryFlash.pageSize'.
 *        # The function naming convention is:
 *              'STM32_<flash_total_size>_<flash_page_size>()'
 *          Example:
 *              'STM32_64k_1k()'
 *          indicates 65,536 bytes total flash with 1,024-byte physical/erase page size.
 */

private static Config _genConfig(final int flashSize_kB, final int pageSize_byte)
{
    final Config config = new Config();

    config.memoryFlash.totalSize = flashSize_kB * 1024;
    config.memoryFlash.pageSize  = pageSize_byte;
    config.memoryFlash.numPages  = config.memoryFlash.totalSize / config.memoryFlash.pageSize;

    return config;
}

public static Config STM32_8k_128B  () { return _genConfig(   8,        128); }
public static Config STM32_8k_256B  () { return _genConfig(   8,        256); }
public static Config STM32_8k_512B  () { return _genConfig(   8,        512); }
public static Config STM32_8k_1k    () { return _genConfig(   8,   1 * 1024); }
public static Config STM32_8k_2k    () { return _genConfig(   8,   2 * 1024); }

public static Config STM32_16k_128B () { return _genConfig(  16,        128); }
public static Config STM32_16k_256B () { return _genConfig(  16,        256); }
public static Config STM32_16k_512B () { return _genConfig(  16,        512); }
public static Config STM32_16k_1k   () { return _genConfig(  16,   1 * 1024); }
public static Config STM32_16k_2k   () { return _genConfig(  16,   2 * 1024); }

public static Config STM32_32k_128B () { return _genConfig(  32,        128); }
public static Config STM32_32k_256B () { return _genConfig(  32,        256); }
public static Config STM32_32k_512B () { return _genConfig(  32,        512); }
public static Config STM32_32k_1k   () { return _genConfig(  32,   1 * 1024); }
public static Config STM32_32k_2k   () { return _genConfig(  32,   2 * 1024); }
public static Config STM32_32k_4k   () { return _genConfig(  32,   4 * 1024); }

public static Config STM32_64k_256B () { return _genConfig(  64,        256); }
public static Config STM32_64k_512B () { return _genConfig(  64,        512); }
public static Config STM32_64k_1k   () { return _genConfig(  64,   1 * 1024); }
public static Config STM32_64k_2k   () { return _genConfig(  64,   2 * 1024); }
public static Config STM32_64k_4k   () { return _genConfig(  64,   4 * 1024); }

public static Config STM32_128k_512B() { return _genConfig( 128,        512); }
public static Config STM32_128k_1k  () { return _genConfig( 128,   1 * 1024); }
public static Config STM32_128k_2k  () { return _genConfig( 128,   2 * 1024); }
public static Config STM32_128k_4k  () { return _genConfig( 128,   4 * 1024); }
public static Config STM32_128k_8k  () { return _genConfig( 128,   8 * 1024); }

public static Config STM32_256k_1k  () { return _genConfig( 256,   1 * 1024); }
public static Config STM32_256k_2k  () { return _genConfig( 256,   2 * 1024); }
public static Config STM32_256k_4k  () { return _genConfig( 256,   4 * 1024); }
public static Config STM32_256k_8k  () { return _genConfig( 256,   8 * 1024); }
public static Config STM32_256k_16k () { return _genConfig( 256,  16 * 1024); }
public static Config STM32_256k_32k () { return _genConfig( 256,  32 * 1024); }

public static Config STM32_512k_2k  () { return _genConfig( 512,   2 * 1024); }
public static Config STM32_512k_4k  () { return _genConfig( 512,   4 * 1024); }
public static Config STM32_512k_8k  () { return _genConfig( 512,   8 * 1024); }
public static Config STM32_512k_16k () { return _genConfig( 512,  16 * 1024); }
public static Config STM32_512k_32k () { return _genConfig( 512,  32 * 1024); }

public static Config STM32_1M_4k    () { return _genConfig(1024,   4 * 1024); }
public static Config STM32_1M_8k    () { return _genConfig(1024,   8 * 1024); }
public static Config STM32_1M_16k   () { return _genConfig(1024,  16 * 1024); }
public static Config STM32_1M_32k   () { return _genConfig(1024,  32 * 1024); }
public static Config STM32_1M_64k   () { return _genConfig(1024,  64 * 1024); }

public static Config STM32_2M_8k    () { return _genConfig(1024,   8 * 1024); }
public static Config STM32_2M_16k   () { return _genConfig(1024,  16 * 1024); }
public static Config STM32_2M_32k   () { return _genConfig(1024,  32 * 1024); }
public static Config STM32_2M_64k   () { return _genConfig(1024,  64 * 1024); }
public static Config STM32_2M_128k  () { return _genConfig(1024, 128 * 1024); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
