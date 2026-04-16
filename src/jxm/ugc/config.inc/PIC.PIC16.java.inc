/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from '../ProgPIC16.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../../README' file for more details).
 */

/***** PCPP_END *****/


// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'PIC16*()' functions ??? #####


public static Config PIC16F5x(final int flashWSize)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.HVSimple);

    // Replace some values ​​for PIC commands
    config.cmdPIC.BeginExtTimedProg = 0b001000;
    config.cmdPIC.EndExtTimedProg   = 0b001110;

    // Set the command sequence and values
    final int cmemABeg = (flashWSize > 512) ? 0xFFE : 0x3FE;

    config.baseProgSpec.configMemAddressBeg    = cmemABeg;
    config.baseProgSpec.deviceIDWordLocOfsset  = 0;

    config.baseProgSpec.flashAddressPreInc     = 1;

    config.baseProgSpec.cmdChipErase_Unlock    = null;

    config.baseProgSpec.cmdChipErase_Flash     = new long[] {
        // Erase flash
        CmdSeq.CS_ResetAddress     ,
        CmdSeq.BulkEraseProgMem    ,
        CmdSeq.CS_DelayMS      , 20
    };

    config.baseProgSpec.cmdChipErase_EEPROM    = null;

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.CS_NotSupported;

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginExtTimedProg;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 2;
    config.baseProgSpec.cmdWriteFlashEnd       = CmdSeq.EndExtTimedProg;
    config.baseProgSpec.cntWriteFlashMulti     = 1;

    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 0;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginExtTimedProg;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 2;
    config.baseProgSpec.cmdWriteConfigEnd      = CmdSeq.EndExtTimedProg;

    // Set the memory specification
    config.baseProgSpec.flashOAddressEmptyVal  = 0x0F;

    config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.eraseBlockSize          = 64;             // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 2;              // NOTE : This PIC16 writes word by word (two bytes each)

    config.memoryEEPROM.totalSize              = 0;

    config.memoryConfigBytes.addressBeg        = cmemABeg;
    config.memoryConfigBytes.addressEnd        = config.memoryConfigBytes.addressBeg + 2;
    config.memoryConfigBytes.address           = new long[] { cmemABeg       };
    config.memoryConfigBytes.size              = new int [] { 2              };
    config.memoryConfigBytes.bitMask           = new long[] { 0b000000001111 };
    /*
    config.memoryConfigBytes.addressMulFW      = 8;
    //*/
    final int cmemAOfs = (flashWSize > 512) ? 0x1000 : 0x1C00; // It is embedded at address 0x1FFE in the HEX file
    //*
    config.memoryConfigBytes.addressMulFW      = 1;
    config.memoryConfigBytes.addressOfsFW      = cmemAOfs;
    //*/

    // Return the configuration object
    return config;
}

public static Config PIC16F54() { return PIC16F5x( 512); }
public static Config PIC16F57() { return PIC16F5x(2048); }
public static Config PIC16F59() { return PIC16F57(    ); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC16F630()
{
    /*
     * !!! WARNING !!! !!! WARNING !!! !!! WARNING !!!
     *
     * It may not be possible to fully recover the MCU when all these conditions are met:
     *     1. nCPD and nCP are enabled.
     *     2. MCLRE is disabled.
     *     3. The PGD, PGC, and nMCLR pins are used as digital I/O.
     *
     * !!! WARNING !!! !!! WARNING !!! !!! WARNING !!!
     */

    // Instantiate the configuration object
    final Config config = new Config(Mode.HVSimple);

    // Set the command sequence and values
    config.baseProgSpec.configMemAddressBeg    = 0x2000;
    config.baseProgSpec.deviceIDWordLocOfsset  = 6;

    final long viOC = CmdSeq.VI(0);
    final long viCW = CmdSeq.VI(1);

    config.baseProgSpec.cmdChipErase_Flash     = XCom.arrayConcatCopy(
        new long[] {
            // Read the OSCCAL value
            CmdSeq.CS_SetAddress      , 0x03FF                        ,
            CmdSeq.ReadDataFromProgMem, viOC                          ,
            // Read the configuration word
            CmdSeq.CS_SetAddress      , 0x2007                        ,
            CmdSeq.ReadDataFromProgMem, viCW
        }                                                             ,
            // Print the original OSCCAL value and configuration word
            CmdSeq.CS_PrintlnUHex04("[BEFORE] OSCCAL      = 0x", viOC), // Example: 0x3434 (0b11010000110100) - RETLW <k>
            CmdSeq.CS_PrintlnUHex04("[BEFORE] CONFIG WORD = 0x", viCW), // Example: 0x11E4 (0b01000111100100)
        //*
            // Check if both the OSCCAL value and configuration word can be properly read
        new long[] {
            CmdSeq.CS_ErrIfCmpEQ      , viOC, 0x3FFF                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viOC, 0x0000                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viCW, 0x3FFF                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viCW, 0x0000
        },
        //*/
        new long[] {
            // Save the BG bits from the configuration word
            CmdSeq.CS_BitwiseAND      , viCW, 0b11000000000000, viCW  ,
            CmdSeq.CS_BitwiseOR       , viCW, 0b00111111111111, viCW  ,
            //*
            // Erase flash
            CmdSeq.BulkEraseProgMem                                   ,
            CmdSeq.CS_DelayMS         , 20                            ,
            // Restore the OSCCAL value
            CmdSeq.CS_SetAddress      , 0x03FF                        ,
            CmdSeq.LoadDataForProgMem , viOC                          ,
            CmdSeq.BeginIntTimedProg                                  ,
            CmdSeq.CS_DelayMS         , 20                            ,
            // Restore the BG bits
            CmdSeq.CS_SetAddress      , 0x2007                        ,
            CmdSeq.LoadDataForProgMem , viCW                          ,
            CmdSeq.BeginIntTimedProg                                  ,
            CmdSeq.CS_DelayMS         , 20                            ,
            //*/
            // Read the OSCCAL value again
            CmdSeq.CS_SetAddress      , 0x03FF                        ,
            CmdSeq.ReadDataFromProgMem, viOC                          ,
            // Read the configuration word again
            CmdSeq.CS_SetAddress      , 0x2007                        ,
            CmdSeq.ReadDataFromProgMem, viCW                          ,
        }                                                             ,
            // Print the OSCCAL value and configuration word again
            CmdSeq.CS_PrintlnUHex04("[AFTER ] OSCCAL      = 0x", viOC),
            CmdSeq.CS_PrintlnUHex04("[AFTER ] CONFIG WORD = 0x", viCW),
            // Check if both the OSCCAL value and configuration word can be properly read
        new long[] {
            CmdSeq.CS_ErrIfCmpEQ      , viOC, 0x3FFF                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viOC, 0x0000                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viCW, 0x3FFF                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viCW, 0x0000                  ,
            // Done
            CmdSeq.CS_NOP
        }
    );

    config.baseProgSpec.cmdChipErase_EEPROM    = new long[] {
        // Erase EEPROM
        CmdSeq.BulkEraseDataMem,
        CmdSeq.CS_DelayMS      , 20
    };

    config.baseProgSpec.cmdChipErase_Recover   = XCom.arrayConcatCopy(
        config.baseProgSpec.cmdChipErase_Flash ,
        config.baseProgSpec.cmdChipErase_EEPROM
    );

    /*
     * !!! WARNING !!! !!! WARNING !!! !!! WARNING !!!
     *
     * This command sequence will cause the OSCCAL value and BG bits to be lost permanently!
     *
     * !!! WARNING !!! !!! WARNING !!! !!! WARNING !!!
     */
    config.baseProgSpec.cmdChipErase_Unbrick   = new long[] {
        CmdSeq.LoadConfiguration, 0x3FFF,
        // Erase flash
        CmdSeq.BulkEraseProgMem         ,
        CmdSeq.CS_DelayMS       , 20    ,
        // Erase EEPROM
        CmdSeq.BulkEraseDataMem         ,
        CmdSeq.CS_DelayMS       , 20
    };

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.LoadConfiguration;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.LoadDataForDataMem;
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.ReadDataFromDataMem;

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 3;
    config.baseProgSpec.cntWriteFlashMulti     = 1;

    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 6;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 10;

    // Set the memory specification
    config.memoryFlash.totalSize               = 1024 * 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.totalSize              -= 2;        // NOTE : The last word is the OSCCAL value
    config.memoryFlash.eraseBlockSize          = 64;       // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 2;        // NOTE : This PIC16 writes word by word (two bytes each)

    config.memoryEEPROM.totalSize              = 128;
    config.memoryEEPROM.writeBlockSizeE        = 0;        // NOTE : PIC16 does not support EICSP
    config.memoryEEPROM.addressBeg             = 0x2100;
    config.memoryEEPROM.addressEnd             = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;

    config.memoryConfigBytes.addressBeg        = 0x2000;
    config.memoryConfigBytes.addressEnd        = config.memoryConfigBytes.addressBeg + 8;
    config.memoryConfigBytes.address           = new long[] { 0x2007          , 0x2000          , 0x2001          , 0x2002          , 0x2003           };
    config.memoryConfigBytes.addressFW         = new long[] { 0x200E          , 0x2000          , 0x2002          , 0x2004          , 0x2006           };
    config.memoryConfigBytes.size              = new int [] { 2               , 2               , 2               , 2               , 2                };
    config.memoryConfigBytes.bitMask           = new long[] { 0b11000111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111 };
    config.memoryConfigBytes.orgMask           = new long[] { 0b11000000000000, 0b00000000000000, 0b00000000000000, 0b00000000000000, 0b00000000000000 };
    config.memoryConfigBytes.addressMulFW      = 2;
    config.memoryConfigBytes.addressOfsFW      = 0;
    config.memoryConfigBytes.maxTotalSize      = 16;

    config.memoryConfigBytes.prepadSizeFW      =          config.memoryConfigBytes.maxTotalSize;
    config.memoryConfigBytes.prepadAddrFW      = 0x2000 * config.memoryConfigBytes.addressMulFW;
    config.memoryConfigBytes.prepadByteFW      = FlashMemory_EmptyValue;

    // Return the configuration object
    return config;
}

public static Config PIC16F676()
{ return PIC16F630(); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC16F7x(final int flashWSize)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.HVSimple);

    // Replace some values ​​for PIC commands
    config.cmdPIC.EndProgramming = 0b001110;

    // Set the command sequence and values
    config.baseProgSpec.configMemAddressBeg    = 0x2000;
    config.baseProgSpec.deviceIDWordLocOfsset  = 6;

    config.baseProgSpec.cmdChipErase_Unlock    = null;

    config.baseProgSpec.cmdChipErase_Flash     = new long[] {
        // Erase flash
        CmdSeq.CS_ResetAddress     ,
        CmdSeq.BulkEraseProgMem    ,
        CmdSeq.CS_DelayMS      , 50
    };

    config.baseProgSpec.cmdChipErase_EEPROM    = null;

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.LoadConfiguration;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.CS_NotSupported;

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginProgramming;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 1;
    config.baseProgSpec.cmdWriteFlashEnd       = CmdSeq.EndProgramming;
    config.baseProgSpec.cntWriteFlashMulti     = 2;

    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 0;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginEraseProgCycle;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 20;

    // Set the memory specification
    config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.eraseBlockSize          = 64;             // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 2          * 2; // NOTE : This PIC16 writes two words at a time (two bytes each)

    config.memoryEEPROM.totalSize              = 0;

    config.memoryConfigBytes.addressBeg        = 0x2000;
    config.memoryConfigBytes.addressEnd        = config.memoryConfigBytes.addressBeg + 8;
    config.memoryConfigBytes.address           = new long[] { 0x2007           };
    config.memoryConfigBytes.size              = new int [] { 2                };
    config.memoryConfigBytes.bitMask           = new long[] { 0b00000001011111 };

    // Return the configuration object
    return config;
}

public static Config PIC16F73() { return PIC16F7x(4096); }
public static Config PIC16F74() { return PIC16F73(    ); }
public static Config PIC16F76() { return PIC16F7x(8192); }
public static Config PIC16F77() { return PIC16F76(    ); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC16F8x(final int flashWSize, final boolean varA)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.HVSimple);

        // Set the command sequence and values
        config.baseProgSpec.configMemAddressBeg    = 0x2000;
        config.baseProgSpec.deviceIDWordLocOfsset  = 6;

        config.baseProgSpec.cmdChipErase_Unlock    = new long[] {
            // Disable code protection
            CmdSeq.LoadConfiguration  , 0x3FFF,
            CmdSeq.IncrementAddress           ,
            CmdSeq.IncrementAddress           ,
            CmdSeq.IncrementAddress           ,
            CmdSeq.IncrementAddress           ,
            CmdSeq.IncrementAddress           ,
            CmdSeq.IncrementAddress           ,
            CmdSeq.IncrementAddress           ,
            CmdSeq.BulkEraseSetup1            ,
            CmdSeq.BulkEraseSetup2            ,
            CmdSeq.BeginEraseProgCycle        ,
            CmdSeq.CS_DelayMS         , 20    ,
            CmdSeq.BulkEraseSetup1            ,
            CmdSeq.BulkEraseSetup2            ,
            CmdSeq.CS_DelayMS         , 20
        };

    if(varA) { // 8xA
        config.baseProgSpec.cmdChipErase_Flash     = new long[] {
            // Erase flash
            CmdSeq.CS_ResetAddress            ,
            CmdSeq.LoadDataForProgMem , 0x3FFF,
            CmdSeq.BulkEraseProgMem           ,
            CmdSeq.BeginProgOnlyCycle         ,
            CmdSeq.CS_DelayMS         , 20
        };
    }
    else {  // 8x
        config.baseProgSpec.cmdChipErase_Flash     = new long[] {
            // Erase flash
            CmdSeq.CS_ResetAddress            ,
            CmdSeq.LoadDataForProgMem , 0x3FFF,
            CmdSeq.BulkEraseSetup1            ,
            CmdSeq.BulkEraseSetup2            ,
            CmdSeq.BeginEraseProgCycle        ,
            CmdSeq.CS_DelayMS         , 20    ,
            CmdSeq.BulkEraseSetup1            ,
            CmdSeq.BulkEraseSetup2
        };
    }

    if(varA) { // 8xA
        config.baseProgSpec.cmdChipErase_EEPROM    = new long[] {
            // Erase EEPROM
            CmdSeq.CS_ResetAddress            ,
            CmdSeq.LoadDataForDataMem , 0x3FFF,
            CmdSeq.BulkEraseDataMem           ,
            CmdSeq.BeginProgOnlyCycle         ,
            CmdSeq.CS_DelayMS         , 20
        };
    }
    else { // 8x
        config.baseProgSpec.cmdChipErase_Flash     = new long[] {
            // Erase flash
            CmdSeq.CS_ResetAddress            ,
            CmdSeq.LoadDataForDataMem , 0x3FFF,
            CmdSeq.BulkEraseSetup1            ,
            CmdSeq.BulkEraseSetup2            ,
            CmdSeq.BeginEraseProgCycle        ,
            CmdSeq.CS_DelayMS         , 20    ,
            CmdSeq.BulkEraseSetup1            ,
            CmdSeq.BulkEraseSetup2
        };
    }

        config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.LoadConfiguration;
        config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;

        config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
        config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.LoadDataForDataMem;
        config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
        config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.ReadDataFromDataMem;

        config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginProgOnlyCycle;
        config.baseProgSpec.cmdWriteFlashDelayMS   = 8;
        config.baseProgSpec.cntWriteFlashMulti     = 1;

        config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.BeginEraseProgCycle;
        config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.BeginProgOnlyCycle;
        config.baseProgSpec.cmdWriteEEPROMDelayMS  = 8;

        config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginEraseProgCycle;
        config.baseProgSpec.cmdWriteConfigDelayMS  = 20;

        // Set the memory specification
        config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
        config.memoryFlash.eraseBlockSize          = 64;             // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
        config.memoryFlash.writeBlockSize          = 2;              // NOTE : This PIC16 writes word by word (two bytes each)

        config.memoryEEPROM.totalSize              = 64;
        config.memoryEEPROM.writeBlockSizeE        = 0;              // NOTE : PIC16 does not support EICSP
        config.memoryEEPROM.addressBeg             = 0x2100;
        config.memoryEEPROM.addressEnd             = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;

        config.memoryConfigBytes.addressBeg        = 0x2000;
        config.memoryConfigBytes.addressEnd        = config.memoryConfigBytes.addressBeg + 8;
        config.memoryConfigBytes.address           = new long[] { 0x2007          , 0x2000          , 0x2001          , 0x2002          , 0x2003           };
        config.memoryConfigBytes.addressFW         = new long[] { 0x200E          , 0x2000          , 0x2002          , 0x2004          , 0x2006           };
        config.memoryConfigBytes.size              = new int [] { 2               , 2               , 2               , 2               , 2                };
        config.memoryConfigBytes.bitMask           = new long[] { 0b11111111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111 };
        config.memoryConfigBytes.addressMulFW      = 2;
        config.memoryConfigBytes.addressOfsFW      = 0;
        config.memoryConfigBytes.maxTotalSize      = 16;

        config.memoryConfigBytes.prepadSizeFW      =          config.memoryConfigBytes.maxTotalSize;
        config.memoryConfigBytes.prepadAddrFW      = 0x2000 * config.memoryConfigBytes.addressMulFW;
        config.memoryConfigBytes.prepadByteFW      = FlashMemory_EmptyValue;


    // Return the configuration object
    return config;
}

public static Config PIC16F83 () { return PIC16F8x( 512, false); }
public static Config PIC16F84 () { return PIC16F8x(1024, false); }
public static Config PIC16F84A() { return PIC16F8x(1024, true ); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC16F87x(final int flashWSize, final int eepromBSize)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.LVSimple);

    // Set the command sequence and values
    config.baseProgSpec.configMemAddressBeg    = 0x2000;
    config.baseProgSpec.deviceIDWordLocOfsset  = 6;

    config.baseProgSpec.cmdChipErase_Unlock    = new long[] {
        // Disable code protection
        CmdSeq.LoadConfiguration  , 0x3FFF,
        CmdSeq.IncrementAddress           ,
        CmdSeq.IncrementAddress           ,
        CmdSeq.IncrementAddress           ,
        CmdSeq.IncrementAddress           ,
        CmdSeq.IncrementAddress           ,
        CmdSeq.IncrementAddress           ,
        CmdSeq.IncrementAddress           ,
        CmdSeq.BulkEraseSetup1            ,
        CmdSeq.BulkEraseSetup2            ,
        CmdSeq.BeginEraseProgCycle        ,
        CmdSeq.CS_DelayMS         , 16    ,
        CmdSeq.BulkEraseSetup1            ,
        CmdSeq.BulkEraseSetup2
    };

    config.baseProgSpec.cmdChipErase_Flash     = new long[] {
        // Erase flash
        CmdSeq.CS_ResetAddress            ,
        CmdSeq.LoadDataForProgMem , 0x3FFF,
        CmdSeq.BulkEraseSetup1            ,
        CmdSeq.BulkEraseSetup2            ,
        CmdSeq.BeginEraseProgCycle        ,
        CmdSeq.CS_DelayMS         , 16    ,
        CmdSeq.BulkEraseSetup1            ,
        CmdSeq.BulkEraseSetup2
    };

    config.baseProgSpec.cmdChipErase_EEPROM    = new long[] {
        // Erase EEPROM
        CmdSeq.CS_ResetAddress            ,
        CmdSeq.LoadDataForDataMem , 0x3FFF,
        CmdSeq.BulkEraseSetup1            ,
        CmdSeq.BulkEraseSetup2            ,
        CmdSeq.BeginEraseProgCycle        ,
        CmdSeq.CS_DelayMS         , 16    ,
        CmdSeq.BulkEraseSetup1            ,
        CmdSeq.BulkEraseSetup2
    };

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.LoadConfiguration;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.LoadDataForDataMem;
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.ReadDataFromDataMem;

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginProgOnlyCycle;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 4;
    config.baseProgSpec.cntWriteFlashMulti     = 1;

    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.BeginEraseProgCycle;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.BeginProgOnlyCycle;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 8;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginEraseProgCycle;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 8;

    // Set the memory specification
    config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.eraseBlockSize          = 64;             // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 2;              // NOTE : This PIC16 writes word by word (two bytes each)

    config.memoryEEPROM.totalSize              = eepromBSize;
    config.memoryEEPROM.writeBlockSizeE        = 0;              // NOTE : PIC16 does not support EICSP
    config.memoryEEPROM.addressBeg             = 0x2100;
    config.memoryEEPROM.addressEnd             = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;

    config.memoryConfigBytes.addressBeg        = 0x2000;
    config.memoryConfigBytes.addressEnd        = config.memoryConfigBytes.addressBeg + 8;
    config.memoryConfigBytes.address           = new long[] { 0x2007          , 0x2000          , 0x2001          , 0x2002          , 0x2003           };
    config.memoryConfigBytes.addressFW         = new long[] { 0x200E          , 0x2000          , 0x2002          , 0x2004          , 0x2006           };
    config.memoryConfigBytes.size              = new int [] { 2               , 2               , 2               , 2               , 2                };
    config.memoryConfigBytes.bitMask           = new long[] { 0b11001111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111 };
    config.memoryConfigBytes.orgMask           = new long[] { 0b00100000000000, 0b00000000000000, 0b00000000000000, 0b00000000000000, 0b00000000000000 };
    config.memoryConfigBytes.addressMulFW      = 2;
    config.memoryConfigBytes.addressOfsFW      = 0;
    config.memoryConfigBytes.maxTotalSize      = 16;

    config.memoryConfigBytes.prepadSizeFW      =          config.memoryConfigBytes.maxTotalSize;
    config.memoryConfigBytes.prepadAddrFW      = 0x2000 * config.memoryConfigBytes.addressMulFW;
    config.memoryConfigBytes.prepadByteFW      = FlashMemory_EmptyValue;

    // Return the configuration object
    return config;
}

public static Config PIC16F870() { return PIC16F87x(2048,  64); }
public static Config PIC16F871() { return PIC16F870(         ); }
public static Config PIC16F872() { return PIC16F870(         ); }
public static Config PIC16F873() { return PIC16F87x(4096, 128); }
public static Config PIC16F874() { return PIC16F873(         ); }
public static Config PIC16F876() { return PIC16F87x(8192, 256); }
public static Config PIC16F877() { return PIC16F876(         ); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC16F87xA(final int flashWSize, final int eepromBSize)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.LVSimple);

    // Set the command sequence and values
    config.baseProgSpec.configMemAddressBeg    = 0x2000;
    config.baseProgSpec.deviceIDWordLocOfsset  = 6;

    config.baseProgSpec.cmdChipErase_Unlock    = new long[] {
        // Erase all
        CmdSeq.CS_ResetAddress        ,
        CmdSeq.ChipErase              ,
        CmdSeq.CS_DelayMS         , 20
    };

    config.baseProgSpec.cmdChipErase_Flash     = new long[] {
        // Erase flash
        CmdSeq.CS_ResetAddress        ,
        CmdSeq.BulkEraseProgMem       ,
        CmdSeq.BeginEraseProgCycle    ,
        CmdSeq.CS_DelayMS         , 20
    };

    config.baseProgSpec.cmdChipErase_EEPROM    = new long[] {
        // Erase EEPROM
        CmdSeq.CS_ResetAddress        ,
        CmdSeq.BulkEraseDataMem       ,
        CmdSeq.BeginProgOnlyCycle     ,
        CmdSeq.CS_DelayMS         , 20
    };

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.LoadConfiguration;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.LoadDataForDataMem;
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.ReadDataFromDataMem;

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginProgOnlyCycle;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 1;
    config.baseProgSpec.cmdWriteFlashEnd       = CmdSeq.EndProgramming;
    config.baseProgSpec.cntWriteFlashMulti     = 8;

    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.BeginEraseProgCycle;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.BeginProgOnlyCycle;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 8;
    config.baseProgSpec.cmdWriteEEPROMEnd      = CmdSeq.EndProgramming;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginEraseProgCycle;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 10;
    config.baseProgSpec.cmdWriteConfigEnd      = CmdSeq.EndProgramming;

    // Set the memory specification
    config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.eraseBlockSize          = 64;             // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 2          * 8; // NOTE : This PIC16 writes eight words at a time (two bytes each)

    config.memoryEEPROM.totalSize              = eepromBSize;
    config.memoryEEPROM.writeBlockSizeE        = 0;              // NOTE : PIC16 does not support EICSP
    config.memoryEEPROM.addressBeg             = 0x2100;
    config.memoryEEPROM.addressEnd             = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;

    config.memoryConfigBytes.addressBeg        = 0x2000;
    config.memoryConfigBytes.addressEnd        = config.memoryConfigBytes.addressBeg + 8;
    config.memoryConfigBytes.address           = new long[] { 0x2007          , 0x2000          , 0x2001          , 0x2002          , 0x2003           };
    config.memoryConfigBytes.addressFW         = new long[] { 0x200E          , 0x2000          , 0x2002          , 0x2004          , 0x2006           };
    config.memoryConfigBytes.size              = new int [] { 2               , 2               , 2               , 2               , 2                };
    config.memoryConfigBytes.bitMask           = new long[] { 0b10111111001111, 0b11111111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111 };
    config.memoryConfigBytes.addressMulFW      = 2;
    config.memoryConfigBytes.addressOfsFW      = 0;
    config.memoryConfigBytes.maxTotalSize      = 16;

    config.memoryConfigBytes.prepadSizeFW      =          config.memoryConfigBytes.maxTotalSize;
    config.memoryConfigBytes.prepadAddrFW      = 0x2000 * config.memoryConfigBytes.addressMulFW;
    config.memoryConfigBytes.prepadByteFW      = FlashMemory_EmptyValue;

    // Return the configuration object
    return config;
}

public static Config PIC16F873A() { return PIC16F87xA(4096, 128); }
public static Config PIC16F874A() { return PIC16F873A(         ); }
public static Config PIC16F876A() { return PIC16F87xA(8192, 256); }
public static Config PIC16F877A() { return PIC16F876A(         ); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC16F150x(final int flashWSize, final int rowSize, final boolean _89)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.LVEntrySeq1LK);

    // Set the command sequence and values
    config.baseProgSpec.configMemAddressBeg    = 0x8000;
    config.baseProgSpec.deviceIDWordLocOfsset  = 6;

    config.baseProgSpec.cmdChipErase_Unlock    = null;

    config.baseProgSpec.cmdChipErase_Flash     = new long[] {
        // Erase flash
        CmdSeq.CS_ResetAddress     ,
        CmdSeq.BulkEraseProgMem    ,
        CmdSeq.CS_DelayMS      , 50
    };

    config.baseProgSpec.cmdChipErase_EEPROM    = null;

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.LoadConfiguration;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;
    config.baseProgSpec.cmdResetAddress        = CmdSeq.ResetAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.CS_NotSupported;

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 3;
    config.baseProgSpec.cntWriteFlashMulti     = rowSize;

    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 0;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 5;

    // Set the memory specification
    config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.eraseBlockSize          = rowSize    * 2; // NOTE : This PIC16 erases N words at a time (N * 2 bytes)
    config.memoryFlash.writeBlockSize          = rowSize    * 2; // NOTE : This PIC16 writes N words at a time (N * 2 bytes)

    config.memoryEEPROM.totalSize              = 0;

    config.memoryConfigBytes.addressBeg        = 0x8000;
    config.memoryConfigBytes.addressEnd        = config.memoryConfigBytes.addressBeg + 9;
    config.memoryConfigBytes.address           = new long[] { 0x8007          , 0x8008           };
    config.memoryConfigBytes.size              = new int [] { 2               , 2                };
    config.memoryConfigBytes.bitMask           = _89
                                               ? new long[] { 0b11111011111111, 0b11111000000011 }
                                               : new long[] { 0b00111011111011, 0b10111000000011 };

    // Return the configuration object
    return config;
}

public static Config PIC16F1501() { return PIC16F150x(1024, 32, false); }
public static Config PIC16F1503() { return PIC16F150x(2048, 16, false); }
public static Config PIC16F1507() { return PIC16F1503(               ); }
public static Config PIC16F1508() { return PIC16F150x(4096, 32, true ); }
public static Config PIC16F1509() { return PIC16F150x(8192, 32, true ); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
