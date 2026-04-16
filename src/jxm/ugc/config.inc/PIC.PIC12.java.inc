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

// ##### ??? TODO : Add more specific-part 'PIC12*()' functions ??? #####


public static Config PIC12F50x(final int flashWSize)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.HVSimple);

    // Replace some values ​​for PIC commands
    config.cmdPIC.EndProgramming = 0b001110;

    // Set the command sequence and values
    final int fadrPInc = 1;

    final int cmemSize = (flashWSize > 512) ? 1024   : 512   ;
    final int cmemABeg = (flashWSize > 512) ? 0x0400 : 0x0200;
    final int cmemAEnd = cmemABeg + cmemSize - 1;

    config.baseProgSpec.configMemAddressBeg    = cmemABeg;
    config.baseProgSpec.deviceIDWordLocOfsset  = 0;

    config.baseProgSpec.flashAddressPreInc     = fadrPInc;

    final long viOC  = CmdSeq.VI(0);
    final long viOCB = CmdSeq.VI(1);

    config.baseProgSpec.cmdChipErase_Flash     = XCom.arrayConcatCopy(
        new long[] {
            // Read the OSCCAL value
            CmdSeq.CS_SetAddress      , flashWSize - 1                     ,
            CmdSeq.CS_IncAddress      , fadrPInc                           ,
            CmdSeq.ReadDataFromProgMem, viOC                               ,
            // Read the backup OSCCAL value
            CmdSeq.CS_SetAddress      , cmemABeg + 4                       ,
            CmdSeq.CS_IncAddress      , fadrPInc                           ,
            CmdSeq.ReadDataFromProgMem, viOCB
        }                                                                  ,
            // Print the original OSCCAL value and backup OSCCAL value
            CmdSeq.CS_PrintlnUHex04("[BEFORE] OSCCAL          = 0x", viOC ), // Example: 0x03C0 (0b00001111000000) - MOVLW <k>
            CmdSeq.CS_PrintlnUHex04("[BEFORE] OSCCAL (BACKUP) = 0x", viOCB), // Example: 0x03C0 (  0b001111000000)
        //*
            // Check if both the OSCCAL value and backup OSCCAL value can be properly read
        new long[] {
            CmdSeq.CS_ErrIfCmpEQ      , viOC , 0x0FFF                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOC , 0x0000                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOCB, 0x0FFF                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOCB, 0x0000
        }                                                                  ,
        //*/
        new long[] {
            //*
            // Erase flash
            CmdSeq.BulkEraseProgMem                                        ,
            CmdSeq.CS_DelayMS         , 20                                 ,
            // Restore the OSCCAL value
            CmdSeq.CS_SetAddress      , flashWSize - 1                     ,
            CmdSeq.CS_IncAddress      , fadrPInc                           ,
            CmdSeq.LoadDataForProgMem , viOC                               ,
            CmdSeq.BeginProgramming                                        ,
            CmdSeq.CS_DelayMS         , 20                                 ,
            CmdSeq.EndProgramming                                          ,
            // Restore the backup OSCCAL value
            CmdSeq.CS_SetAddress      , cmemABeg + 4                       ,
            CmdSeq.CS_IncAddress      , fadrPInc                           ,
            CmdSeq.LoadDataForProgMem , viOCB                              ,
            CmdSeq.BeginProgramming                                        ,
            CmdSeq.CS_DelayMS         , 20                                 ,
            CmdSeq.EndProgramming                                          ,
            //*/
            // Read the OSCCAL value
            CmdSeq.CS_SetAddress      , flashWSize - 1                     ,
            CmdSeq.CS_IncAddress      , fadrPInc                           ,
            CmdSeq.ReadDataFromProgMem, viOC                               ,
            // Read the backup OSCCAL value
            CmdSeq.CS_SetAddress      , cmemABeg + 4                       ,
            CmdSeq.CS_IncAddress      , fadrPInc                           ,
            CmdSeq.ReadDataFromProgMem, viOCB
        }                                                                  ,
            // Print the OSCCAL value and backup OSCCAL value again
            CmdSeq.CS_PrintlnUHex04("[AFTER ] OSCCAL          = 0x", viOC ),
            CmdSeq.CS_PrintlnUHex04("[AFTER ] OSCCAL (BACKUP) = 0x", viOCB),
            // Check if both the OSCCAL value and backup OSCCAL value can be properly read
        new long[] {
            CmdSeq.CS_ErrIfCmpEQ      , viOC , 0x0FFF                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOC , 0x0000                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOCB, 0x0FFF                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOCB, 0x0000                      ,
            // Done
            CmdSeq.CS_NOP
        }
    );

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.CS_NotSupported;

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginProgramming;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 3;
    config.baseProgSpec.cmdWriteFlashEnd       = CmdSeq.EndProgramming;
    config.baseProgSpec.cntWriteFlashMulti     = 1;

    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 0;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginProgramming;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 6;
    config.baseProgSpec.cmdWriteConfigEnd      = CmdSeq.EndProgramming;

    // Set the memory specification
    config.baseProgSpec.flashOAddressEmptyVal  = 0x0F;

    config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC12 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.totalSize              -= 2;              // NOTE : The last word is the OSCCAL value
    config.memoryFlash.eraseBlockSize          = 64;             // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 2;              // NOTE : This PIC12 writes word by word (two bytes each)

    config.memoryEEPROM.totalSize              = 0;

    /*
     * NOTE : By convention:
     *            # The configuration bits data is stored at the logical address location of 0xFFF within the HEX file.
     *            # The user ID data            is stored at the logical address location starting from (cmemABeg * 2) within the HEX file.
     *
     * ##### ??? TODO : Is there a better method ??? #####
     */
    final long cwHEXFileAddr = (0xFFF * 2) - cmemABeg;
    config.memoryConfigBytes.addressBeg        = cmemABeg;
    config.memoryConfigBytes.addressEnd        = 0x1000; // config.memoryConfigBytes.addressBeg + cmemSize;
    config.memoryConfigBytes.address           = new long[] { cmemAEnd      , cmemABeg + 0  , cmemABeg + 1  , cmemABeg + 2  , cmemABeg + 3   };
    config.memoryConfigBytes.addressFW         = new long[] { cwHEXFileAddr , cmemABeg + 0  , cmemABeg + 2  , cmemABeg + 4  , cmemABeg + 6   };
    config.memoryConfigBytes.size              = new int [] { 2             , 2             , 2             , 2             , 2              };
    config.memoryConfigBytes.bitMask           = new long[] { 0b000000011111, 0b111111111111, 0b111111111111, 0b111111111111, 0b111111111111 };
    config.memoryConfigBytes.addressMulFW      = 2;
    config.memoryConfigBytes.addressOfsFW      = 0;
    config.memoryConfigBytes.maxTotalSize      = (int) (
                                                    ( config.memoryConfigBytes.addressEnd - config.memoryConfigBytes.addressBeg )
                                                    * config.memoryConfigBytes.addressMulFW
                                                 );
    config.memoryConfigBytes.prepadSizeFW      =            config.memoryConfigBytes.maxTotalSize;
    config.memoryConfigBytes.prepadAddrFW      = cmemABeg * config.memoryConfigBytes.addressMulFW;
    config.memoryConfigBytes.prepadByteFW      = FlashMemory_EmptyValue;

    // Return the configuration object
    return config;
}

public static Config PIC12F508() { return PIC12F50x( 512); }
public static Config PIC12F509() { return PIC12F50x(1024); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC12F629()
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.HVSimple);

    // Set the command sequence and values
    config.baseProgSpec.configMemAddressBeg    = 0x2000;
    config.baseProgSpec.deviceIDWordLocOfsset  = 0;

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
            CmdSeq.CS_PrintlnUHex04("[BEFORE] OSCCAL      = 0x", viOC), // Example: 0x3420 (0b11010000100000) - RETLW <k>
            CmdSeq.CS_PrintlnUHex04("[BEFORE] CONFIG WORD = 0x", viCW), // Example: 0x21FF (0b10000111111111)
        //*
        new long[] {
            // Check if both the OSCCAL value and configuration word can be properly read
            CmdSeq.CS_ErrIfCmpEQ      , viOC, 0x3FFF                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viOC, 0x0000                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viCW, 0x3FFF                  ,
            CmdSeq.CS_ErrIfCmpEQ      , viCW, 0x0000                  ,
        }                                                             ,
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

    config.baseProgSpec.cmdChipErase_EEPROM    = null; // NOTE : 'cmdChipErase_Flash' will also erase the EEPROM

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.LoadConfiguration;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.LoadDataForProgMem;  // NOTE : This PIC12 does not use 'LoadDataForDataMem'
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.ReadDataFromProgMem; // NOTE : This PIC12 does not use 'ReadDataFromDataMem'

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 3;
    config.baseProgSpec.cntWriteFlashMulti     = 1;

    config.baseProgSpec.msbAlignEEPROMData     = true;
    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 6;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 6;

    // Set the memory specification
    config.memoryFlash.totalSize               = 1024 * 2; // NOTE : A PIC12 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.totalSize              -= 2;        // NOTE : The last word is the OSCCAL value
    config.memoryFlash.eraseBlockSize          = 64;       // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 2;        // NOTE : This PIC12 writes word by word (two bytes each)

    config.memoryEEPROM.totalSize              = 128;
    config.memoryEEPROM.writeBlockSizeE        = 0;        // NOTE : PIC12 does not support EICSP
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

public static Config PIC12F675()
{ return PIC12F629(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
