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

// ##### ??? TODO : Add more specific-part 'PIC10*()' functions ??? #####


public static Config PIC10F2xx(final int flashWSize, final boolean _22x)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.HVSimple);

    // Replace some values ​​for PIC commands
    config.cmdPIC.EndProgramming = 0b001110;

    // Set the command sequence and values
    final int fadrPInc = 1;

    final int cmemSize = (flashWSize > 256) ? 512    : 256   ;
    final int cmemABeg = (flashWSize > 256) ? 0x0200 : 0x0100;
    final int cmemAEnd = cmemABeg + cmemSize - 1;

    config.baseProgSpec.configMemAddressBeg    = cmemABeg;
  //config.baseProgSpec.configMemOnlyUponEntry = true;
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
            CmdSeq.ReadDataFromProgMem, viOCB,
        }                                                                  ,
            // Print the original OSCCAL value and backup OSCCAL value
            CmdSeq.CS_PrintlnUHex04("[BEFORE] OSCCAL          = 0x", viOC ), // Example: 0x0C28 (0b110000101000) - MOVLW 0x28
            CmdSeq.CS_PrintlnUHex04("[BEFORE] OSCCAL (BACKUP) = 0x", viOCB), // Example: 0x0C28 (0b110000101000) - MOVLW 0x28
            // Check if both the OSCCAL value and backup OSCCAL value can be properly read
        new long[] {
            CmdSeq.CS_ErrIfCmpEQ      , viOC , 0x0FFF                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOC , 0x0000                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOCB, 0x0FFF                      ,
            CmdSeq.CS_ErrIfCmpEQ      , viOCB, 0x0000
        }                                                                  ,
        new long[] {
            // Ensure both OSCCAL value and backup OSCCAL value include the MOVLW instruction
            CmdSeq.CS_BitwiseAND      , viOC , 0b000011111111, viOC        ,
            CmdSeq.CS_BitwiseOR       , viOC , 0b110000000000, viOC        ,
            CmdSeq.CS_BitwiseAND      , viOCB, 0b000011111111, viOCB       ,
            CmdSeq.CS_BitwiseOR       , viOCB, 0b110000000000, viOCB       ,
            // Erase flash
            //*
            CmdSeq.CS_ResetAddress                                         ,
            CmdSeq.CS_IncAddress      , fadrPInc                           ,
            CmdSeq.CS_IncAddress      , cmemABeg                           ,
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
            // Read the OSCCAL value again
            CmdSeq.CS_SetAddress      , flashWSize - 1                     ,
            CmdSeq.CS_IncAddress      , fadrPInc                           ,
            CmdSeq.ReadDataFromProgMem, viOC                               ,
            // Read the backup OSCCAL value again
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
    config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC10 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.totalSize              -= 2;              // NOTE : The last word is the OSCCAL value
    config.memoryFlash.eraseBlockSize          = 64;             // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 2;              // NOTE : This PIC10 writes word by word (two bytes each)

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
    config.memoryConfigBytes.bitMask           = _22x
                                               ? new long[] { 0b000000011111, 0b111111111111, 0b111111111111, 0b111111111111, 0b111111111111 }
                                               : new long[] { 0b000000011100, 0b111111111111, 0b111111111111, 0b111111111111, 0b111111111111 };
    config.memoryConfigBytes.orgMask           = _22x
                                               ? new long[] { 0b111111100000, 0b000000000000, 0b000000000000, 0b000000000000, 0b000000000000 }
                                               : new long[] { 0b111111100011, 0b000000000000, 0b000000000000, 0b000000000000, 0b000000000000 };
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

public static Config PIC10F200() { return PIC10F2xx(256, false); }
public static Config PIC10F202() { return PIC10F2xx(512, false); }
public static Config PIC10F204() { return PIC10F2xx(256, false); }
public static Config PIC10F206() { return PIC10F2xx(512, false); }

public static Config PIC10F220() { return PIC10F2xx(256, true ); }
public static Config PIC10F222() { return PIC10F2xx(512, true ); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config PIC10F32x(final int flashWSize)
{
    // Instantiate the configuration object
    final Config config = new Config(Mode.LVEntrySeq1LK);

    // Set the command sequence and values
    config.baseProgSpec.configMemAddressBeg    = 0x2000;
    config.baseProgSpec.deviceIDWordLocOfsset  = 6;

    final long viOC  = CmdSeq.VI(0);
    final long viOCB = CmdSeq.VI(1);

    config.baseProgSpec.cmdChipErase_Flash     = new long[] {
        // Erase flash
        CmdSeq.LoadConfiguration, 0x3FFF,
        CmdSeq.BulkEraseProgMem ,
        CmdSeq.CS_DelayMS       , 10
    };

    config.baseProgSpec.cmdLoadConfiguration   = CmdSeq.LoadConfiguration;
    config.baseProgSpec.cmdIncrementAddress    = CmdSeq.IncrementAddress;
    config.baseProgSpec.cmdResetAddress        = CmdSeq.ResetAddress;

    config.baseProgSpec.cmdLoadDataForProgMem  = CmdSeq.LoadDataForProgMem;
    config.baseProgSpec.cmdLoadDataForDataMem  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdReadDataFromProgMem = CmdSeq.ReadDataFromProgMem;
    config.baseProgSpec.cmdReadDataFromDataMem = CmdSeq.CS_NotSupported;

    config.baseProgSpec.cmdWriteFlash          = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteFlashDelayMS   = 3;
    config.baseProgSpec.cntWriteFlashMulti     = 1;

    config.baseProgSpec.cmdWriteEEPROM         = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMNoErase  = CmdSeq.CS_NotSupported;
    config.baseProgSpec.cmdWriteEEPROMDelayMS  = 0;

    config.baseProgSpec.cmdWriteConfig         = CmdSeq.BeginIntTimedProg;
    config.baseProgSpec.cmdWriteConfigDelayMS  = 6;

    // Set the memory specification
    config.memoryFlash.totalSize               = flashWSize * 2; // NOTE : A PIC10 word is 12/14-bits wide and is stored as two bytes in the firmware file
    config.memoryFlash.eraseBlockSize          = 64;             // NOTE : It does not matter as long as it is multiple of two and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSize          = 32;             // NOTE : This PIC10 writes 32 words at a time (64 bytes)

    config.memoryEEPROM.totalSize              = 0;

    config.memoryConfigBytes.addressBeg        = 0x2000;
    config.memoryConfigBytes.addressEnd        = config.memoryConfigBytes.addressBeg + 8;
    config.memoryConfigBytes.address           = new long[] { 0x2007          , 0x2000          , 0x2001          , 0x2002          , 0x2003           };
    config.memoryConfigBytes.addressFW         = new long[] { 0x200E          , 0x2000          , 0x2002          , 0x2004          , 0x2006           };
    config.memoryConfigBytes.size              = new int [] { 2               , 2               , 2               , 2               , 2                };
    config.memoryConfigBytes.bitMask           = new long[] { 0b01111111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111, 0b11111111111111 };
    config.memoryConfigBytes.addressMulFW      = 2;
    config.memoryConfigBytes.addressOfsFW      = 0;
    config.memoryConfigBytes.maxTotalSize      = 16;

    config.memoryConfigBytes.prepadSizeFW      =          config.memoryConfigBytes.maxTotalSize;
    config.memoryConfigBytes.prepadAddrFW      = 0x2000 * config.memoryConfigBytes.addressMulFW;
    config.memoryConfigBytes.prepadByteFW      = FlashMemory_EmptyValue;

    // Return the configuration object
    return config;
}

public static Config PIC10F320() { return PIC10F32x(256); }
public static Config PIC10F322() { return PIC10F32x(512); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
