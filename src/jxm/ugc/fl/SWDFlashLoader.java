/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.fl;


import java.io.StringWriter;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;

import jxm.*;
import jxm.ugc.*;
import jxm.xb.*;


public class SWDFlashLoader extends SWDExecInstOpcode {

    // ##### !!! TODO : How about MCU with external flash? !!! #####

    /*
     * A flash/EEPROM loader program must be designed so that it accepts these input parameters:
     *     r0 : dstAddr (in flash/EEPROM or SRAM) ; if in SRAM, must be at least (SRAM_START + PROG_AREA_SIZE + 128)
     *     r1 : srcAddr (in flash/EEPROM or SRAM) ; if in SRAM, must be at least (SRAM_START + PROG_AREA_SIZE + 128)
     *     r2 : datSize
     *     r3 : sigAddr (in                 SRAM) ;             must be at least (SRAM_START + PROG_AREA_SIZE      )
     *
     * Up to 31 x 32-bit extra parameters can be put in SRAM after sigAddr.
     *
     * A flash/EEPROM loader program must set one of these value in [sigAddr] during its operation:
     *     0x00              : operation is in progress
     *     0x01              : operation is complete
     *     <negative_number> : error
     */

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int MaxSWDTransferSize = 1024 * 1024;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Specifier {

        public static class CustomInstruction {
            public final String[] paramName;
            public final XVI   [] paramXVI;
            public final int   [] dataBuffLB;
            public final long[][] instruction;

            public CustomInstruction(final String[] paramName_, final XVI[] paramXVI_, final int dataBuffLBSize_, final long[][] instruction_)
            {
                paramName   = paramName_;
                paramXVI    = paramXVI_;
                dataBuffLB  = (dataBuffLBSize_ <= 0) ? null : ( new int[dataBuffLBSize_] );
                instruction = instruction_;
            }
        }

        private final HashMap<String, CustomInstruction> _customInstruction = new HashMap<>();

        public void addCustomInstruction(final String name, final CustomInstruction customInstruction)
        { _customInstruction.put(name, customInstruction); }

        public CustomInstruction getCustomInstruction(final String name)
        { return _customInstruction.get(name); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final byte    flashMemoryEmptyValue;

        public final int     wrMaxSWDTransferSize;
        public final int     rdMaxSWDTransferSize;

        public final boolean supportDirectFlashRead;
        public final boolean supportDirectEEPROMRead;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final long[][] instruction_InitializeSystemOnce;
        public final long[][] instruction_UninitializeSystemExit;

        public final long[][] instruction_IsFlashLocked;
        public final long[][] instruction_UnlockFlash;
        public final long[][] instruction_EraseFlash;
        public final long[][] instruction_EraseFlashPages;
        public final long[][] instruction_WriteFlash;
        public final long[][] instruction_ReadFlash;

        public final long[][] instruction_IsEEPROMLocked;
        public final long[][] instruction_UnlockEEPROM;
        public final long[][] instruction_WriteEEPROM;
        public final long[][] instruction_ReadEEPROM;

        public final int      instruction_xviFlashEEPROMAddress;
        public final int      instruction_xviFlashEEPROMReadSize;
        public final int      instruction_xviSignalWorkerCommand;
        public final int      instruction_xviSignalJobState;
        public final int []   instruction_dataBuffFlash;
        public final int []   instruction_dataBuffEEPROM;

        public final long[][] instruction_WriteFLB;
        public final long[][] instruction_ReadFLB;
        public final XVI      instruction_xviFLB_DoneRead;
        public final XVI      instruction_xviFLB_FDirty;
        public final XVI      instruction_xviFLB_LBDirty;
        public final int []   instruction_dataBuffFLB; // NOTE : The first element always contains the lock bits while
                                                       //        the remaining elements always contain the fuses

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final long[]  flProgram;
        public final long[]  elProgram;

        public final long    addrProgStart;
        public final long    addrProgBuffer;
        public final long    addrProgSignal;
        public final long[]  rdProgExtraParams;
        public final long[]  wrProgExtraParams;

        public Specifier(
            final int      flashMemoryEmptyValue_             , // NOTE : Mostly it will be 0xFF, sometimes it can be 0x00

            final int      wrMaxSWDTransferSize_              , // NOTE : Set this to zero to use the maximum transfer size when writing (which is usually equal to the flash page size)
            final int      rdMaxSWDTransferSize_              , // NOTE : Set this to zero to use the maximum transfer size when reading (which is usually equal to the flash page size)

            final boolean  supportDirectFlashRead_            , // NOTE : Set this to true if the flash  memory can be read directly using SWD
            final boolean  supportDirectEEPROMRead_           , // NOTE : Set this to true if the EEPROM memory can be read directly using SWD

            ////////////////////////////////////////////////////////////////////////////////////////////////////

            final long[][] instruction_InitializeSystemOnce_  , // NOTE : If specified    , it will be executed once before any other flash-related instructions are executed
            final long[][] instruction_UninitializeSystemExit_, // NOTE : If specified    , it will be executed once before ending programming mode

            final long[][] instruction_IsFlashLocked_         , // NOTE : If not specified, the system will assume that the flash is always unlocked
            final long[][] instruction_UnlockFlash_           , // NOTE : If not specified, the system will assume that flash unlocking is not required
            final long[][] instruction_EraseFlash_            , // NOTE : If specified    , it will be used to erase the entire flash memory
            final long[][] instruction_EraseFlashPages_       , // NOTE : If specified    , it will be used to erase only part of flash memory
            final long[][] instruction_WriteFlash_            , // NOTE : If specified    , then it must be designed for multi-threading (see XVI_SIGNAL_*)
            final long[][] instruction_ReadFlash_             , // NOTE : If specified    , then it must be designed for multi-threading (see XVI_SIGNAL_*)

            final long[][] instruction_IsEEPROMLocked_        , // NOTE : If not specified, the system will assume that the EEPROM is always unlocked
            final long[][] instruction_UnlockEEPROM_          , // NOTE : If not specified, the system will assume that EEPROM unlocking is not required
            final long[][] instruction_WriteEEPROM_           , // NOTE : If specified    , then it must be designed for multi-threading (see XVI_SIGNAL_*)
            final long[][] instruction_ReadEEPROM_            , // NOTE : If specified    , then it must be designed for multi-threading (see XVI_SIGNAL_*)

            final XVI      instruction_xviFlashEEPROMAddress_ , // NOTE : Used by 'instruction_WriteFlash' and 'instruction_ReadFlash' and 'instruction_WriteEEPROM' and 'instruction_ReadEEPROM'
            final XVI      instruction_xviFlashEEPROMReadSize_, // NOTE : Used by                              'instruction_ReadFlash' and                               'instruction_ReadEEPROM' ; if not specified then reading will always be done in flash/EEPROM page size
            final XVI      instruction_xviSignalWorkerCommand_, // NOTE : Used by 'instruction_WriteFlash' and 'instruction_ReadFlash' and 'instruction_WriteEEPROM' and 'instruction_ReadEEPROM'
            final XVI      instruction_xviSignalJobState_     , // NOTE : Used by 'instruction_WriteFlash' and 'instruction_ReadFlash' and 'instruction_WriteEEPROM' and 'instruction_ReadEEPROM'
            final int []   instruction_dataBuffFlash_         , // NOTE : Used by 'instruction_WriteFlash' and 'instruction_ReadFlash'                                                            if 'addrProgBuffer' is not used
            final int []   instruction_dataBuffEEPROM_        , // NOTE : Used by                                                          'instruction_WriteEEPROM' and 'instruction_ReadEEPROM' if 'addrProgBuffer' is not used

            ////////////////////////////////////////////////////////////////////////////////////////////////////

            final long[]   flProgram_                         , // NOTE : If specified    , it will be used to read and write the flash  memory
            final long[]   elProgram_                         , // NOTE : If specified    , it will be used to read and write the EEPROM memory

            final long     addrProgStart_                     , // NOTE : Used by 'flProgram' and 'elProgram'
            final long     addrProgBuffer_                    , // NOTE : Used by 'flProgram' and 'elProgram' and 'instruction_WriteFlash' and 'instruction_ReadFlash' and 'instruction_WriteEEPROM' and 'instruction_ReadEEPROM'
            final long     addrProgSignal_                    , // NOTE : Used by 'flProgram' and 'elProgram'
            final long[]   wrProgExtraParams_                 , // NOTE : Used by 'flProgram' and 'elProgram' when writing the flash/EEPROM memory
            final long[]   rdProgExtraParams_                 , // NOTE : Used by 'flProgram' and 'elProgram' when reading the flash/EEPROM memory

            ////////////////////////////////////////////////////////////////////////////////////////////////////

            final long[][] instruction_WriteFLB_              , // NOTE : If specified, it will be used to write configuration bits (fuses) and security bits (lock bits)
            final long[][] instruction_ReadFLB_               , // NOTE : If specified, it will be used to read  configuration bits (fuses) and security bits (lock bits)
            final XVI      instruction_xviFLB_DoneRead_       , // NOTE : Used by 'instruction_ReadFLB'
            final XVI      instruction_xviFLB_FDirty_         , // NOTE : Used by 'instruction_WriteFLB'
            final XVI      instruction_xviFLB_LBDirty_        , // NOTE : Used by 'instruction_WriteFLB'
            final int []   instruction_dataBuffFLB_             // NOTE : Used by 'instruction_ReadFLB' and 'instruction_WriteFLB'

        ) {
            flashMemoryEmptyValue              = (byte) (flashMemoryEmptyValue_ & 0xFF);

            wrMaxSWDTransferSize               = (wrMaxSWDTransferSize_ > 0) ? wrMaxSWDTransferSize_ : MaxSWDTransferSize;
            rdMaxSWDTransferSize               = (rdMaxSWDTransferSize_ > 0) ? rdMaxSWDTransferSize_ : MaxSWDTransferSize;

            supportDirectFlashRead             = supportDirectFlashRead_;
            supportDirectEEPROMRead            = supportDirectEEPROMRead_;

            ////////////////////////////////////////////////////////////////////////////////////////////////////

            instruction_InitializeSystemOnce   = instruction_InitializeSystemOnce_;
            instruction_UninitializeSystemExit = instruction_UninitializeSystemExit_;

            instruction_IsFlashLocked          = instruction_IsFlashLocked_;
            instruction_UnlockFlash            = instruction_UnlockFlash_;
            instruction_EraseFlash             = instruction_EraseFlash_;
            instruction_EraseFlashPages        = instruction_EraseFlashPages_;
            instruction_WriteFlash             = instruction_WriteFlash_;
            instruction_ReadFlash              = instruction_ReadFlash_;

            instruction_IsEEPROMLocked         = instruction_IsEEPROMLocked_;
            instruction_UnlockEEPROM           = instruction_UnlockEEPROM_;
            instruction_WriteEEPROM            = instruction_WriteEEPROM_;
            instruction_ReadEEPROM             = instruction_ReadEEPROM_;

            instruction_xviFlashEEPROMAddress  = instruction_xviFlashEEPROMAddress_ .value();
            instruction_xviFlashEEPROMReadSize = instruction_xviFlashEEPROMReadSize_.value();
            instruction_xviSignalWorkerCommand = instruction_xviSignalWorkerCommand_.value();
            instruction_xviSignalJobState      = instruction_xviSignalJobState_     .value();
            instruction_dataBuffFlash          = instruction_dataBuffFlash_;
            instruction_dataBuffEEPROM         = instruction_dataBuffEEPROM_;

            ////////////////////////////////////////////////////////////////////////////////////////////////////

            flProgram                          = flProgram_;
            elProgram                          = elProgram_;

            addrProgStart                      = addrProgStart_;
            addrProgBuffer                     = addrProgBuffer_;
            addrProgSignal                     = addrProgSignal_;
            wrProgExtraParams                  = wrProgExtraParams_;
            rdProgExtraParams                  = rdProgExtraParams_;

            ////////////////////////////////////////////////////////////////////////////////////////////////////

            instruction_WriteFLB               = instruction_WriteFLB_;
            instruction_ReadFLB                = instruction_ReadFLB_;
            instruction_xviFLB_DoneRead        = instruction_xviFLB_DoneRead_;
            instruction_xviFLB_FDirty          = instruction_xviFLB_FDirty_;
            instruction_xviFLB_LBDirty         = instruction_xviFLB_LBDirty_;
            instruction_dataBuffFLB            = instruction_dataBuffFLB_;

        }

    } // class Specifier

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String[] _flBuiltInClasses = new String[] {
                                                  "jxm.ugc.fl.SWDFlashLoaderATSAM"   ,
                                                  "jxm.ugc.fl.SWDFlashLoaderNRF5"    ,
                                                  "jxm.ugc.fl.SWDFlashLoaderRenMF3"  ,
                                                  "jxm.ugc.fl.SWDFlashLoaderRP"      ,
                                                  "jxm.ugc.fl.SWDFlashLoaderSTM32"   ,
                                                  "jxm.ugc.fl.SWDFlashLoaderSTM32_XF"
                                              };

    public static void _testAllBuiltInFlashLoaderClasses() throws ClassNotFoundException
    { for(final String className : _flBuiltInClasses) Class.forName(className); }

    public static Specifier getSpecifierFor(final ProgSWDLowLevel.Config config, final SWDExecInst swdExecInst)
    {
        // Extract the class name and driver name
        final ArrayList<String> driverPart = XCom.explode( config.memoryFlash.driverName, ".");
        final           String  driverName = driverPart.get( driverPart.size() - 1 );

        driverPart.remove( driverPart.size() - 1 );

        // Determine the user-defined class name
        String userClassName = null;

        if( !driverPart.isEmpty() ) {
            try {
                userClassName = XCom.flatten(driverPart, ".");
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                return null;
            }
        }

        // Clear all INS_DEBUG_PRINTF arguments that may have been allocated by previous call to this function
        swdExecInst._printfClearAllArgs();

        // Prepare the class list
        final String[] classList = (userClassName != null) ? new String[] { userClassName } : _flBuiltInClasses;

        // Get the specifier
        final StringWriter sw = new StringWriter();
        final PrintWriter  pw = new PrintWriter(sw);

        for(final String className : classList) {
            // Skip null class name
            if(className == null) continue;
            try {
                // Get the class
                final Class<?> clazz = Class.forName(className);
                // Invoke the driver
                return (Specifier) clazz.getMethod("_getSpecifier_" + driverName, ProgSWDLowLevel.Config.class, SWDExecInst.class).invoke(null, config, swdExecInst);
            }
            catch(final Exception e) {
                // Accumulate the stack trace
                e.printStackTrace(pw);
            }
        }

        // Print the accumulated stack trace if requested
        if( XCom.enableAllExceptionStackTrace() ) SysUtil.stdErr().print( sw.toString() );

        // Not found
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final long[][] _fl_dummy_instruction_WriteFLB = new SWDExecInstBuilder() {{
        nop();
    }}.link();

    protected static final long[][] _fl_dummy_instruction_ReadFLB(final int flashMemoryEmptyValue)
    {
        return new SWDExecInstBuilder() {{
            strDB( flashMemoryEmptyValue, 0 );
            strDB( flashMemoryEmptyValue, 1 );
        }}.link();
    }

    protected static final XVI      _fl_dummy_xviFLB               = XVI._2047;

    protected static final int[]    _fl_dummy_dataBuffFLB          = new int[1 + 1];

} // class SWDFlashLoader
