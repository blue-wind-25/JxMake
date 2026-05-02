/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;

import static jxm.ugc.USB2GPIO.IEVal;
import static jxm.xb.XCom._M2CC;
import static jxm.xb.XCom._X2CC;


/*
 * Please refer to the comment block before the 'ProgPIC' class definition in the 'ProgPIC.java' file for more details and information.
 */
public class ProgPIC16 extends ProgPIC {

    /*
     * ######################################### !!! WARNING !!! #########################################
     * 1. Due to the nature of the PIC10, PIC12, and PIC16 ICSP protocol, it can only be programmed easily
     *    and effectively using hardware-assisted bit-banging.
     * 2. It means that this programmer class ignores the user-supplied PGC frequency in 'begin()'.
     * ######################################### !!! WARNING !!! #########################################
     */

    public static class CmdSeq {
        // ##### ??? TODO : How to make these more user-friendly when specified using JSON string ??? #####

        /*
         * NOTE : # The value '_INVALID_' for mandatory fields means uninitialized, and it will cause the programmer
         *          class to throw an error upon instantiation. Therefore, in the case of these mandatory fields, if
         *          the commands are not supported, they must be set to the value 'CS_NotSupported'.
         *        # For optional fields, the value '_INVALID_' is synonymous with the value 'CS_NotSupported'.
         */

        //                       Command                      Parameter       Result
        public final static int _INVALID_           =   -1; // -               -

        public final static int CS_NotSupported     = 9999; // -               -

        public final static int CS_NOP              =    0; // -               -
        public final static int CS_DelayMS          =    1; // U14/VI          -

        /*
         * NOTE : # It is highly recommended to use the 'CS_ResetAddress' command instead of the 'ResetAddress' command
         *          when defining the command sequences for 'cmdChipErase_*[]' (even if the particular MCU does support
         *          the 'ResetAddress' command).
         *        # The 'CS_ResetAddress' command resets the PC to the same default address as the initial address when
         *          the MCU first enters programming mode. Please consult the particular MCU datasheet for more details.
         *        # The value of 'Config.BaseProgSpec.flashAddressPreInc' is ignored by both the 'CS_ResetAddress' and
         *          'CS_SetAddress' commands. It means that users must manually preincrement the address when defining
         *          the command sequences for 'cmdChipErase_*[]'.
         */
        public final static int CS_ResetAddress     =  100; // -        -        -
        public final static int CS_SetAddress       =  101; // U14/VI   -        -
        public final static int CS_IncAddress       =  102; // U14/VI   -        -

        public final static int CS_BitwiseAND       =  200; // U14/VI   U14/VI   VI
        public final static int CS_BitwiseOR        =  201; // U14/VI   U14/VI   VI

        public final static int CS_ErrIfCmpEQ       =  300; // U14/VI   U14/VI   -
        public final static int CS_ErrIfCmpNEQ      =  301; // U14/VI   U14/VI   -

        public final static int CS_Print2Chars      = 1000; // U14/VI   -        -
        public final static int CS_PrintSDecNN      = 1001; // U14/VI   -        -
        public final static int CS_PrintSDec03      = 1002; // U14/VI   -        -
        public final static int CS_PrintSDec05      = 1003; // U14/VI   -        -
        public final static int CS_PrintUDecNN      = 1004; // U14/VI   -        -
        public final static int CS_PrintUDec03      = 1005; // U14/VI   -        -
        public final static int CS_PrintUDec05      = 1006; // U14/VI   -        -
        public final static int CS_PrintUHexNN      = 1007; // U14/VI   -        -
        public final static int CS_PrintUHex02      = 1008; // U14/VI   -        -
        public final static int CS_PrintUHex04      = 1009; // U14/VI   -        -
        public final static int CS_PrintNL          = 1010; // -        -        -

        /*
         * NOTE : # Only these commands must be used when initializing all 'cmd*' fields, except when defining command
         *          sequences for 'cmdChipErase_*[]'!
         *        # Using the above 'CS_*' commands for 'cmd*' fields will cause undefined behavior!
         */
        public final static int LoadConfiguration   = 2000; // U14/VI   -        -
        public final static int LoadDataForProgMem  = 2001; // U14/VI   -        -
        public final static int LoadDataForDataMem  = 2002; // U14/VI   -        -
        public final static int IncrementAddress    = 2003; // -        -        -
        public final static int ResetAddress        = 2004; // -        -        -
        public final static int ReadDataFromProgMem = 2005; // -        -        VI
        public final static int ReadDataFromDataMem = 2006; // -        -        VI
        public final static int BeginErase          = 2007; // -        -        -
        public final static int BeginEraseProgCycle = 2008; // -        -        -
        public final static int BeginProgOnlyCycle  = 2009; // -        -        -
        public final static int BeginIntTimedProg   = 2010; // -        -        -
        public final static int BeginExtTimedProg   = 2011; // -        -        -
        public final static int EndExtTimedProg     = 2012; // -        -        -
        public final static int RowEraseProgMem     = 2013; // -        -        -
        public final static int BulkEraseProgMem    = 2014; // -        -        -
        public final static int BulkEraseDataMem    = 2015; // -        -        -
        public final static int BulkEraseSetup1     = 2016; // -        -        -
        public final static int BulkEraseSetup2     = 2017; // -        -        -
        public final static int ChipErase           = 2018; // -        -        -
        public final static int BeginProgramming    = 2019; // -        -        -
        public final static int EndProgramming      = 2020; // -        -        -

        // Aliases
        public final static int TogSelEvenRows1     = BulkEraseSetup1;
        public final static int TogSelEvenRows2     = BulkEraseSetup2;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static long VI(final long index)
        { return -(index + 2); }

        public static long[] CS_PrintString(final String str)
        {
            // Determine the size
                  int     len = str.length();
            final boolean odd = (len & 1) != 0;

            if(odd) --len;

            // Allocate the command buffer
            final long buf[] = new long[ len + (odd ? 2 : 0) ];
                  int  idx   = 0;

            // Store all characters (except the last character if the number of characters is odd)
            for(; idx < len; idx += 2) {
                buf[idx + 0] = CmdSeq.CS_Print2Chars;
                buf[idx + 1] = _M2CC( str.charAt(idx), str.charAt(idx + 1) );
            }

            // Store the last character (only if the number of characters is odd)
            if(odd) {
                buf[idx + 0] = CmdSeq.CS_Print2Chars;
                buf[idx + 1] = _M2CC( str.charAt(idx), (char) 0 );
            }

            // Return the command buffer
            return buf;
        }

        public static long[] CS_PrintlnString(final String str)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(str), new long[] { CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnSDecNN(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintSDecNN, value, CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnSDec03(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintSDec03, value, CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnSDec05(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintSDec05, value, CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnUDecNN(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintUDecNN, value, CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnUDec03(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintUDec03, value, CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnUDec05(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintUDec05, value, CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnUHexNN(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintUHexNN, value, CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnUHex02(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintUHex02, value, CmdSeq.CS_PrintNL } ); }

        public static long[] CS_PrintlnUHex04(final String prefix, final long value)
        { return XCom.arrayConcatCopy( CmdSeq.CS_PrintString(prefix), new long[] { CmdSeq.CS_PrintUHex04, value, CmdSeq.CS_PrintNL } ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // NOTE: Used directly or indirectly by the 'INIEncoderLite' and/or 'INIDecoder' class!
        public static String VI(final String strIndex)
        { return Long.toString( VI( (Long) XCom.parseNumberStr(strIndex) ) ); }

        // NOTE: Used directly or indirectly by the 'INIEncoderLite' and/or 'INIDecoder' class!
        public static String TC(final String str2CC)
        { return String.valueOf( XCom._M2CCStr(str2CC) ); }

        // NOTE: Used directly or indirectly by the 'INIEncoderLite' and/or 'INIDecoder' class!
        public static int getCmdSeqParamCount(final String name)
        {
            if(name == null) return 0;

            switch( name.trim() ) {

                // NOTE : Synchronize the number of parameters with the notes above!

                case "$CS_DelayMS"          : return 1;

                case "$CS_SetAddress"       : return 1;
                case "$CS_IncAddress"       : return 1;

                case "$CS_BitwiseAND"       : return 3;
                case "$CS_BitwiseOR"        : return 3;

                case "$CS_ErrIfCmpEQ"       : return 2;
                case "$CS_ErrIfCmpNEQ"      : return 2;

                case "$CS_Print2Chars"      : return 1;
                case "$CS_PrintSDecNN"      : return 1;
                case "$CS_PrintSDec03"      : return 1;
                case "$CS_PrintSDec05"      : return 1;
                case "$CS_PrintUDecNN"      : return 1;
                case "$CS_PrintUDec03"      : return 1;
                case "$CS_PrintUDec05"      : return 1;
                case "$CS_PrintUHexNN"      : return 1;
                case "$CS_PrintUHex02"      : return 1;
                case "$CS_PrintUHex04"      : return 1;
                case "$LoadConfiguration"   : return 1;
                case "$LoadDataForProgMem"  : return 1;
                case "$LoadDataForDataMem"  : return 1;
                case "$ReadDataFromProgMem" : return 1;
                case "$ReadDataFromDataMem" : return 1;

            } // switch

            return 0;
        }

    } // class CmdSeq

    // NOTE: Used directly or indirectly by the 'INIEncoderLite' and/or 'INIDecoder' class!
    public static class CmdSeqStringTranslator implements DataFormat.StringValueTranslator {

        private static int     _prevCmd_paramCount       = -1;
        private static boolean _prevCmdIs_CS_Print2Chars = false;

        @Override
        public void reset()
        {
            // Reset the state
            _prevCmd_paramCount       = -1;
            _prevCmdIs_CS_Print2Chars = false;
        }

        @Override
        public String translate(final String debugInfo, final String str)
        {
            // The maximum length of <TranslateMarker>'CmdSeq.*' and '@DataFormat.Hex16'
            final String tmarker = jxm.tool.INIEncoderLite.TranslateMarker;
            final String fmarker = jxm.tool.INIEncoderLite.FunctionMarker;
            final int    maxLen  = Math.max( tmarker.length() + 19, 2 + 16 );
            final String format  = "%-" + maxLen + 's';

            // Determine whether to translate or not
            // ##### !!! TODO : Do not translate if it is actually part of the previous instruction paramater(s) !!! #####
            final String  strTrim   = str.trim();
            final boolean translate = !strTrim.isEmpty();
            final boolean fullTrans = (_prevCmd_paramCount <= 0);

            --_prevCmd_paramCount;

            // Define the custom translation function
            final BiFunction<Long, Double, String> customTransFunc = (final Long valL, final Double valD) -> {
                // If 'valL' is null, there is nothing to translate.
                // Ignore 'valD' since 'CmdSeq.*' is never a floating-point value.
                if(valL == null) return null;
                // Check if the previous command is 'CS_Print2Chars'
                if(_prevCmdIs_CS_Print2Chars) {
                    return String.format( format, tmarker + "TC" + fmarker + XCom._X2CCStr( valL.intValue() ) );
                }
                // Check if it is a value produced from 'VI()'
                else if(valL <= -2) {
                    return String.format( format, tmarker + "VI" + fmarker + CmdSeq.VI(valL) );
                }
                // Return an empty string to ensure the lambda function has a return type of 'String'
                return "";
            };

            // Find the field name or function call
            String resTrans = null;

            if(translate) {
                if(fullTrans) {
                    resTrans = XCom.strMatchClassConst(debugInfo, CmdSeq.class, strTrim, Long.class, tmarker, format, customTransFunc);
                }
                else {
                    resTrans = customTransFunc.apply( ( (Long) XCom.parseNumberStr(strTrim) ).longValue(), null );
                    if( resTrans != null && resTrans.isEmpty() ) resTrans = null;
                }
            }

            // Update the state
            if(_prevCmd_paramCount < 0) _prevCmd_paramCount = CmdSeq.getCmdSeqParamCount(resTrans);

            _prevCmdIs_CS_Print2Chars = (resTrans != null) && resTrans.trim().equals( tmarker + "CS_Print2Chars" );

            // Return the result
            return (resTrans != null) ? resTrans : String.format(format, str);
        }
    }

    // NOTE: Used directly or indirectly by the 'INIEncoderLite' and/or 'INIDecoder' class!
    @Inherited @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD )
    @DataFormat.StringValueTranslatorDescriptor(translator = CmdSeqStringTranslator.class, className = "jxm.ugc.ProgPIC16$CmdSeq")
    public static @interface CST {}

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class Config extends ProgPIC.Config {

        /*
         * NOTE : In case of PIC10, PIC12, and PIC16 MCUs of which the address-reset command will reset the PC
         *        to the address of the configuration word, 'Config.MemoryConfigBytes.address[0]' must contain
         *        the address of that particular configuration word.
         *
         * ##### ??? TODO : How to handle an MCU of this type that has more than one configuration word ??? #####
         */

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        public static class CmdPIC implements Serializable {
            @DataFormat.Hex02 public int LoadConfiguration    = -1;
            @DataFormat.Hex02 public int LoadDataForProgMem   = -1;
            @DataFormat.Hex02 public int LoadDataForDataMem   = -1;
            @DataFormat.Hex02 public int IncrementAddress     = -1;
            @DataFormat.Hex02 public int ResetAddress         = -1;
            @DataFormat.Hex02 public int ReadDataFromProgMem  = -1;
            @DataFormat.Hex02 public int ReadDataFromDataMem  = -1;
            @DataFormat.Hex02 public int BeginErase           = -1;
            @DataFormat.Hex02 public int BeginEraseProgCycle  = -1;
            @DataFormat.Hex02 public int BeginProgOnlyCycle   = -1;
            @DataFormat.Hex02 public int BeginIntTimedProg    = -1;
            @DataFormat.Hex02 public int BeginExtTimedProg    = -1;
            @DataFormat.Hex02 public int EndExtTimedProg      = -1;
            @DataFormat.Hex02 public int RowEraseProgMem      = -1;
            @DataFormat.Hex02 public int BulkEraseProgMem     = -1;
            @DataFormat.Hex02 public int BulkEraseDataMem     = -1;
            @DataFormat.Hex02 public int BulkEraseSetup1      = -1;
            @DataFormat.Hex02 public int BulkEraseSetup2      = -1;
            @DataFormat.Hex02 public int ChipErase            = -1;
            @DataFormat.Hex02 public int BeginProgramming     = -1;
            @DataFormat.Hex02 public int EndProgramming       = -1;
        }

        public static class BaseProgSpec extends ProgPIC.Config.BaseProgSpec {
            @DataFormat.Hex08 public long    configMemAddressBeg    = -1;
                            //public boolean configMemOnlyUponEntry = false;            // Optional
                              public int     deviceIDWordLocOfsset  = -1;               // Optional

            @DataFormat.Hex02 public int     flashEAddressEmptyVal  = -1;
            @DataFormat.Hex02 public int     flashOAddressEmptyVal  = -1;
                              public int     flashAddressPreInc     = -1;               // Optional

            /*
             * NOTE : # When decoding INI strings (but not JSON), constant names from 'CmdSeq.*'
             *          can be used for these three arrays.
             *        # The '@CST' annotation is applied to enable the 'INIEncoderLite' class to
             *          serialize the values to 'CmdSeq.*' whenever possible.
             *        # Please refer to '../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations)
             *          for more details.
             */
       @CST @DataFormat.Hex16 public long[]  cmdChipErase_Unlock    = null;             // Optional
       @CST @DataFormat.Hex16 public long[]  cmdChipErase_Flash     = null;
       @CST @DataFormat.Hex16 public long[]  cmdChipErase_EEPROM    = null;             // Optional

            /*
             * NOTE : # When decoding INI strings (but not JSON), constant names from 'CmdSeq.*'
             *          can be used for these two arrays.
             *        # The '@CST' annotation is applied to enable the 'INIEncoderLite' class to
             *          serialize the values to 'CmdSeq.*' whenever possible.
             *        # Please refer to '../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations)
             *          for more details.
             */
       @CST @DataFormat.Hex16 public long[]  cmdChipErase_Recover   = null;             // Optional (required by some MCUs with nMCLR disabled                            )
       @CST @DataFormat.Hex16 public long[]  cmdChipErase_Unbrick   = null;             // Optional (required by some MCUs with nMCLR disabled and code protection enabled)

       @CST @DataFormat.Hex04 public int     cmdLoadConfiguration   = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdIncrementAddress    = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdResetAddress        = CmdSeq._INVALID_; // Optional

       @CST @DataFormat.Hex04 public int     cmdLoadDataForProgMem  = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdLoadDataForDataMem  = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdReadDataFromProgMem = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdReadDataFromDataMem = CmdSeq._INVALID_;

       @CST @DataFormat.Hex04 public int     cmdWriteFlash          = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdWriteFlashDelayMS   = -1;
       @CST @DataFormat.Hex04 public int     cmdWriteFlashEnd       = CmdSeq._INVALID_; // Optional
                              public int     cntWriteFlashMulti     = -1;

                              public boolean msbAlignEEPROMData     = false;
       @CST @DataFormat.Hex04 public int     cmdWriteEEPROM         = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdWriteEEPROMNoErase  = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdWriteEEPROMDelayMS  = -1;
       @CST @DataFormat.Hex04 public int     cmdWriteEEPROMEnd      = CmdSeq._INVALID_; // Optional

       @CST @DataFormat.Hex04 public int     cmdWriteConfig         = CmdSeq._INVALID_;
       @CST @DataFormat.Hex04 public int     cmdWriteConfigDelayMS  = -1;
       @CST @DataFormat.Hex04 public int     cmdWriteConfigEnd      = CmdSeq._INVALID_; // Optional
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final CmdPIC       cmdPIC;
        public final BaseProgSpec baseProgSpec;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private int _RU06(final int value)
        { return (value >= 0) ? XCom._RU06(value) : value; }

        public Config(final SubPart subPart, final Mode mode)
        {
            // Process the superclass
            super( new BaseProgSpec() );

            // Create/store the objects
            cmdPIC       = new CmdPIC();
            baseProgSpec = (BaseProgSpec) super.baseProgSpec;

            // NOTE : 1. The default values below are common for most PIC MCUs.
            //        2. Please override them as required.

            // Set default values ​​for PIC commands
            cmdPIC.LoadConfiguration    = 0b000000;
            cmdPIC.LoadDataForProgMem   = 0b000010;
            cmdPIC.LoadDataForDataMem   = 0b000011;
            cmdPIC.IncrementAddress     = 0b000110;
            cmdPIC.ResetAddress         = 0b010110;
            cmdPIC.ReadDataFromProgMem  = 0b000100;
            cmdPIC.ReadDataFromDataMem  = 0b000101;
            cmdPIC.BeginErase           = 0b001000; // It usually means       'Row Erase Program/Data Memory'
            cmdPIC.BeginEraseProgCycle  = 0b001000;
            cmdPIC.BeginProgOnlyCycle   = 0b011000;
            cmdPIC.BeginIntTimedProg    = 0b001000;
            cmdPIC.BeginExtTimedProg    = 0b011000;
            cmdPIC.EndExtTimedProg      = 0b001010;
            cmdPIC.RowEraseProgMem      = 0b010001;
            cmdPIC.BulkEraseProgMem     = 0b001001;
            cmdPIC.BulkEraseDataMem     = 0b001011;
            cmdPIC.BulkEraseSetup1      = 0b000001; // It is also referred as 'Toggle Select Even Rows 1'
            cmdPIC.BulkEraseSetup2      = 0b000111; // It is also referred as 'Toggle Select Even Rows 7'
            cmdPIC.ChipErase            = 0b011111;
            cmdPIC.BeginProgramming     = 0b001000;
            cmdPIC.EndProgramming       = 0b010111;

            // Set the standard configuration values
            baseProgSpec.part                  = Part.PIC16;
            baseProgSpec.subPart               = subPart;
            baseProgSpec.mode                  = mode;
            baseProgSpec.entrySeq              = ( mode == Mode.LVEntrySeqM0M32  || mode == Mode.LVEntrySeqM1L32 ||
                                                   mode == Mode.LVEntrySeqM1M32K || mode == Mode.LVEntrySeqM1L33K
                                                 ) ? 0x4D434850L : -1;

            baseProgSpec.flashEAddressEmptyVal = FlashMemory_EmptyValue & 0xFF;
            baseProgSpec.flashOAddressEmptyVal = 0x3F;

            // NOTE : The PIC16 assembler typically works with 14-bit words, so addresses in the firmware
            //        need to be multiplied by two
            memoryEEPROM.addressMulFW          = 2;
            memoryEEPROM.addressOfsFW          = 0;

            memoryConfigBytes.addressMulFW     = 2;
            memoryConfigBytes.addressOfsFW     = 0;
        }

        public Config(final SubPart subPart)
        { this(subPart, Mode.Default); }

        public Config(final Mode mode)
        { this(SubPart.F, mode); }

        public Config()
        { this(SubPart.F, Mode.Default); }

        private void _init()
        {
            // Reverse the bit order of the PIC commands
            cmdPIC.LoadConfiguration    = _RU06(cmdPIC.LoadConfiguration   );
            cmdPIC.LoadDataForProgMem   = _RU06(cmdPIC.LoadDataForProgMem  );
            cmdPIC.LoadDataForDataMem   = _RU06(cmdPIC.LoadDataForDataMem  );
            cmdPIC.IncrementAddress     = _RU06(cmdPIC.IncrementAddress    );
            cmdPIC.ResetAddress         = _RU06(cmdPIC.ResetAddress        );
            cmdPIC.ReadDataFromProgMem  = _RU06(cmdPIC.ReadDataFromProgMem );
            cmdPIC.ReadDataFromDataMem  = _RU06(cmdPIC.ReadDataFromDataMem );
            cmdPIC.BeginErase           = _RU06(cmdPIC.BeginErase          );
            cmdPIC.BeginEraseProgCycle  = _RU06(cmdPIC.BeginEraseProgCycle );
            cmdPIC.BeginProgOnlyCycle   = _RU06(cmdPIC.BeginProgOnlyCycle  );
            cmdPIC.BeginIntTimedProg    = _RU06(cmdPIC.BeginIntTimedProg   );
            cmdPIC.BeginExtTimedProg    = _RU06(cmdPIC.BeginExtTimedProg   );
            cmdPIC.EndExtTimedProg      = _RU06(cmdPIC.EndExtTimedProg     );
            cmdPIC.RowEraseProgMem      = _RU06(cmdPIC.RowEraseProgMem     );
            cmdPIC.BulkEraseProgMem     = _RU06(cmdPIC.BulkEraseProgMem    );
            cmdPIC.BulkEraseDataMem     = _RU06(cmdPIC.BulkEraseDataMem    );
            cmdPIC.BulkEraseSetup1      = _RU06(cmdPIC.BulkEraseSetup1     );
            cmdPIC.BulkEraseSetup2      = _RU06(cmdPIC.BulkEraseSetup2     );
            cmdPIC.ChipErase            = _RU06(cmdPIC.ChipErase           );
            cmdPIC.BeginProgramming     = _RU06(cmdPIC.BeginProgramming    );
            cmdPIC.EndProgramming       = _RU06(cmdPIC.EndProgramming      );
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
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
    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected final Config _config16;

    public ProgPIC16(final USB2GPIO usb2gpio, final Config config) throws Exception
    {
        // Process the superclass
        super(usb2gpio, config);

        // Store the objects
        _config16 = (Config) super._config;

        _config16._init();

        // Check the configuration values
        // ##### !!! TODO : VERIFY AND IMPROVE !!! #####
        if(   _config16.baseProgSpec.part    != Part   .PIC16     ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSPart , ProgClassName);
        if(   _config16.baseProgSpec.subPart != SubPart.F         ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSSPrt , ProgClassName);
        if(   _config16.baseProgSpec.mode    != Mode   .HVSimple
           && _config16.baseProgSpec.mode    != Mode   .LVSimple
           && _config16.baseProgSpec.mode    != Mode   .LVEntrySeqM0M32
           && _config16.baseProgSpec.mode    != Mode   .LVEntrySeqM1L32
           && _config16.baseProgSpec.mode    != Mode   .LVEntrySeqM1M32K
           && _config16.baseProgSpec.mode    != Mode   .LVEntrySeqM1L33K) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSMode , ProgClassName);

        if(_config16.cmdPIC.LoadConfiguration    < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "LoadConfiguration"   );
        if(_config16.cmdPIC.LoadDataForProgMem   < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "LoadDataForProgMem"  );
        if(_config16.cmdPIC.LoadDataForDataMem   < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "LoadDataForDataMem"  );
        if(_config16.cmdPIC.IncrementAddress     < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "IncrementAddress"    );
        if(_config16.cmdPIC.ResetAddress         < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "ResetAddress"        );
        if(_config16.cmdPIC.ReadDataFromProgMem  < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "ReadDataFromProgMem" );
        if(_config16.cmdPIC.ReadDataFromDataMem  < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "ReadDataFromDataMem" );
        if(_config16.cmdPIC.BeginErase           < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BeginErase"          );
        if(_config16.cmdPIC.BeginEraseProgCycle  < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BeginEraseProgCycle" );
        if(_config16.cmdPIC.BeginProgOnlyCycle   < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BeginProgOnlyCycle"  );
        if(_config16.cmdPIC.BeginIntTimedProg    < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BeginIntTimedProg"   );
        if(_config16.cmdPIC.BeginExtTimedProg    < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BeginExtTimedProg"   );
        if(_config16.cmdPIC.EndExtTimedProg      < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "EndExtTimedProg"     );
        if(_config16.cmdPIC.RowEraseProgMem      < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "RowEraseProgMem"     );
        if(_config16.cmdPIC.BulkEraseProgMem     < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BulkEraseProgMem"    );
        if(_config16.cmdPIC.BulkEraseDataMem     < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BulkEraseDataMem"    );
        if(_config16.cmdPIC.BulkEraseSetup1      < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BulkEraseSetup1"     );
        if(_config16.cmdPIC.BulkEraseSetup2      < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BulkEraseSetup2"     );
        if(_config16.cmdPIC.ChipErase            < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "ChipErase"           );
        if(_config16.cmdPIC.BeginProgramming     < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "BeginProgramming"    );
        if(_config16.cmdPIC.EndProgramming       < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvCmdPIC, ProgClassName, "EndProgramming"      );

        if(_config16.baseProgSpec.configMemAddressBeg    <= 0               ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "configMemAddressBeg"   );
        if(_config16.baseProgSpec.flashEAddressEmptyVal  <= 0               ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "flashEAddressEmptyVal" );
        if(_config16.baseProgSpec.flashOAddressEmptyVal  <= 0               ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "flashOAddressEmptyVal" );

        if(_config16.baseProgSpec.configMemAddressBeg != _config.memoryConfigBytes.addressBeg) { // Cross check
            throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSMFBAdrB, ProgClassName);
        }

        if(_config16.baseProgSpec.cmdChipErase_Flash     == null            ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdChipErase_Flash"    );
        if(_config16.baseProgSpec.cmdLoadConfiguration   == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdLoadConfiguration"  );
        if(_config16.baseProgSpec.cmdIncrementAddress    == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdIncrementAddress"   );
        if(_config16.baseProgSpec.cmdLoadDataForProgMem  == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdLoadDataForProgMem" );
        if(_config16.baseProgSpec.cmdLoadDataForDataMem  == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdLoadDataForDataMem" );
        if(_config16.baseProgSpec.cmdReadDataFromProgMem == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdReadDataFromProgMem");
        if(_config16.baseProgSpec.cmdReadDataFromDataMem == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdReadDataFromDataMem");
        if(_config16.baseProgSpec.cmdWriteFlash          == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdWriteFlash"         );
        if(_config16.baseProgSpec.cmdWriteFlashDelayMS   <  0               ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdWriteFlashDelayMS"  );
        if(_config16.baseProgSpec.cntWriteFlashMulti     <= 0               ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cntWriteFlashMulti"    );
        if(_config16.baseProgSpec.cmdWriteEEPROM         == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdWriteEEPROM"        );
        if(_config16.baseProgSpec.cmdWriteEEPROMNoErase  == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdWriteEEPROMNoErase" );
        if(_config16.baseProgSpec.cmdWriteEEPROMDelayMS  <  0               ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdWriteEEPROMDelayMS" );
        if(_config16.baseProgSpec.cmdWriteConfig         == CmdSeq._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdWriteConfig"        );
        if(_config16.baseProgSpec.cmdWriteConfigDelayMS  <  0               ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSForXXX, ProgClassName, "cmdWriteConfigDelayMS" );
    }

    // WARNING : PIC16 can only be programmed in hardware-assisted bit-banging SPI mode!
    @Override
    public boolean useHardwareAssistedBitBangingSPI()
    { return true; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     *                           High     Low
     * 14-bit value to be sent : 00DCBA98 76543210
     *
     * Bytes to be sent to SPI : z0123456 789ABCDz
     *                           76543210 76543210
     *                           Byte #0  Byte #1
     */
    private static int _pic16_makeU14(final int data14)
    { return XCom._RU16(data14 << 1) & 0b0111111111111110; }

    private static int _pic16_makeU14(final int datah6, final int datal8)
    { return _pic16_makeU14( (datah6 << 8) | datal8 ); }
  //{ return ( XCom._RU08(datal8) << 7 ) | ( XCom._RU06(datah6 & 0x3F) << 1 ); }

    private static int[] _pic16_encodeU14_extract(final int u16)
    { return new int[] { (u16 >> 8) & 0xFF, u16 & 0xFF }; }

    private static int[] _pic16_encodeU14(final int data14)
    { return _pic16_encodeU14_extract( _pic16_makeU14(data14) ); }

    private static int[] _pic16_encodeU14(final int datah6, final int datal8)
    { return _pic16_encodeU14_extract( _pic16_makeU14(datah6, datal8) ); }

    /*
     *                           Byte #0  Byte #1
     *                           76543210 76543210
     * Bytes received from SPI : z0123456 789ABCDz
     *
     * Convert to 14-bit value : 00DCBA98 76543210
     *                           High     Low
     */
    private static int _pic16_decodeU14(final int datah8, final int datal8)
    { return ( XCom._RU16( (datal8 << 8) | datah8 ) & 0b0111111111111110 ) >>> 1; }
  //{ return ( ( ( XCom._RU08(datah8) << 8 ) | XCom._RU08(datal8) ) & 0b0111111111111110 ) >>> 1; }

    private static int _pic16_decodeU14(final int data16)
    { return _pic16_decodeU14( (data16 >>> 8) & 0xFF, data16 & 0xFF ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _cmdSeqVI = new int[32];

    private static class CmdSeqData {

        public static final int CMD_NOP            = -(0xFFFF + CmdSeq.CS_NOP         );
        public static final int CMD_DELAY_MS       = -(0xFFFF + CmdSeq.CS_DelayMS     );

        public static final int CMD_RESET_ADDRESS  = -(0xFFFF + CmdSeq.CS_ResetAddress);
        public static final int CMD_SET_ADDRESS    = -(0xFFFF + CmdSeq.CS_SetAddress  );
        public static final int CMD_INC_ADDRESS    = -(0xFFFF + CmdSeq.CS_IncAddress  );

        public static final int CMD_BITWISE_AND    = -(0xFFFF + CmdSeq.CS_BitwiseAND  );
        public static final int CMD_BITWISE_OR     = -(0xFFFF + CmdSeq.CS_BitwiseOR   );

        public static final int CMD_ERR_IF_CMP_EQ  = -(0xFFFF + CmdSeq.CS_ErrIfCmpEQ  );
        public static final int CMD_ERR_IF_CMP_NEQ = -(0xFFFF + CmdSeq.CS_ErrIfCmpNEQ );

        public static final int CMD_PRINT_2CHARS   = -(0xFFFF + CmdSeq.CS_Print2Chars );
        public static final int CMD_PRINT_SDECNN   = -(0xFFFF + CmdSeq.CS_PrintSDecNN );
        public static final int CMD_PRINT_SDEC03   = -(0xFFFF + CmdSeq.CS_PrintSDec03 );
        public static final int CMD_PRINT_SDEC05   = -(0xFFFF + CmdSeq.CS_PrintSDec05 );
        public static final int CMD_PRINT_UDECNN   = -(0xFFFF + CmdSeq.CS_PrintUDecNN );
        public static final int CMD_PRINT_UDEC03   = -(0xFFFF + CmdSeq.CS_PrintUDec03 );
        public static final int CMD_PRINT_UDEC05   = -(0xFFFF + CmdSeq.CS_PrintUDec05 );
        public static final int CMD_PRINT_UHEXNN   = -(0xFFFF + CmdSeq.CS_PrintUHexNN );
        public static final int CMD_PRINT_UHEX02   = -(0xFFFF + CmdSeq.CS_PrintUHex02 );
        public static final int CMD_PRINT_UHEX04   = -(0xFFFF + CmdSeq.CS_PrintUHex04 );
        public static final int CMD_PRINT_NL       = -(0xFFFF + CmdSeq.CS_PrintNL     );

        public final int cmdVal;
        public final int parCnt;
        public final int resCnt;

        public CmdSeqData(final int cmdVal_, final int parCnt_, final int resCnt_)
        {
            cmdVal = cmdVal_;
            parCnt = parCnt_;
            resCnt = resCnt_;
        }
    };

    private CmdSeqData _getCmdSeqData(final long index)
    {
        switch( (int) index ) {
            case CmdSeq.CS_NOP               : return new CmdSeqData(CmdSeqData.CMD_NOP                   , 0, 0);
            case CmdSeq.CS_DelayMS           : return new CmdSeqData(CmdSeqData.CMD_DELAY_MS              , 1, 0);

            case CmdSeq.CS_ResetAddress      : return new CmdSeqData(CmdSeqData.CMD_RESET_ADDRESS         , 0, 0);
            case CmdSeq.CS_SetAddress        : return new CmdSeqData(CmdSeqData.CMD_SET_ADDRESS           , 1, 0);
            case CmdSeq.CS_IncAddress        : return new CmdSeqData(CmdSeqData.CMD_INC_ADDRESS           , 1, 0);

            case CmdSeq.CS_BitwiseAND        : return new CmdSeqData(CmdSeqData.CMD_BITWISE_AND           , 2, 1);
            case CmdSeq.CS_BitwiseOR         : return new CmdSeqData(CmdSeqData.CMD_BITWISE_OR            , 2, 1);

            case CmdSeq.CS_ErrIfCmpEQ        : return new CmdSeqData(CmdSeqData.CMD_ERR_IF_CMP_EQ         , 2, 0);
            case CmdSeq.CS_ErrIfCmpNEQ       : return new CmdSeqData(CmdSeqData.CMD_ERR_IF_CMP_NEQ        , 2, 0);

            case CmdSeq.CS_Print2Chars       : return new CmdSeqData(CmdSeqData.CMD_PRINT_2CHARS          , 1, 0);
            case CmdSeq.CS_PrintSDecNN       : return new CmdSeqData(CmdSeqData.CMD_PRINT_SDECNN          , 1, 0);
            case CmdSeq.CS_PrintSDec03       : return new CmdSeqData(CmdSeqData.CMD_PRINT_SDEC03          , 1, 0);
            case CmdSeq.CS_PrintSDec05       : return new CmdSeqData(CmdSeqData.CMD_PRINT_SDEC05          , 1, 0);
            case CmdSeq.CS_PrintUDecNN       : return new CmdSeqData(CmdSeqData.CMD_PRINT_UDECNN          , 1, 0);
            case CmdSeq.CS_PrintUDec03       : return new CmdSeqData(CmdSeqData.CMD_PRINT_UDEC03          , 1, 0);
            case CmdSeq.CS_PrintUDec05       : return new CmdSeqData(CmdSeqData.CMD_PRINT_UDEC05          , 1, 0);
            case CmdSeq.CS_PrintUHexNN       : return new CmdSeqData(CmdSeqData.CMD_PRINT_UHEXNN          , 1, 0);
            case CmdSeq.CS_PrintUHex02       : return new CmdSeqData(CmdSeqData.CMD_PRINT_UHEX02          , 1, 0);
            case CmdSeq.CS_PrintUHex04       : return new CmdSeqData(CmdSeqData.CMD_PRINT_UHEX04          , 1, 0);
            case CmdSeq.CS_PrintNL           : return new CmdSeqData(CmdSeqData.CMD_PRINT_NL              , 0, 0);

            case CmdSeq.LoadConfiguration    : return new CmdSeqData(_config16.cmdPIC.LoadConfiguration   , 1, 0);
            case CmdSeq.LoadDataForProgMem   : return new CmdSeqData(_config16.cmdPIC.LoadDataForProgMem  , 1, 0);
            case CmdSeq.LoadDataForDataMem   : return new CmdSeqData(_config16.cmdPIC.LoadDataForDataMem  , 1, 0);
            case CmdSeq.IncrementAddress     : return new CmdSeqData(_config16.cmdPIC.IncrementAddress    , 0, 0);
            case CmdSeq.ResetAddress         : return new CmdSeqData(_config16.cmdPIC.ResetAddress        , 0, 0);
            case CmdSeq.ReadDataFromProgMem  : return new CmdSeqData(_config16.cmdPIC.ReadDataFromProgMem , 0, 1);
            case CmdSeq.ReadDataFromDataMem  : return new CmdSeqData(_config16.cmdPIC.ReadDataFromDataMem , 0, 1);
            case CmdSeq.BeginErase           : return new CmdSeqData(_config16.cmdPIC.BeginErase          , 0, 0);
            case CmdSeq.BeginEraseProgCycle  : return new CmdSeqData(_config16.cmdPIC.BeginEraseProgCycle , 0, 0);
            case CmdSeq.BeginProgOnlyCycle   : return new CmdSeqData(_config16.cmdPIC.BeginProgOnlyCycle  , 0, 0);
            case CmdSeq.BeginIntTimedProg    : return new CmdSeqData(_config16.cmdPIC.BeginIntTimedProg   , 0, 0);
            case CmdSeq.BeginExtTimedProg    : return new CmdSeqData(_config16.cmdPIC.BeginExtTimedProg   , 0, 0);
            case CmdSeq.EndExtTimedProg      : return new CmdSeqData(_config16.cmdPIC.EndExtTimedProg     , 0, 0);
            case CmdSeq.RowEraseProgMem      : return new CmdSeqData(_config16.cmdPIC.RowEraseProgMem     , 0, 0);
            case CmdSeq.BulkEraseProgMem     : return new CmdSeqData(_config16.cmdPIC.BulkEraseProgMem    , 0, 0);
            case CmdSeq.BulkEraseDataMem     : return new CmdSeqData(_config16.cmdPIC.BulkEraseDataMem    , 0, 0);
            case CmdSeq.BulkEraseSetup1      : return new CmdSeqData(_config16.cmdPIC.BulkEraseSetup1     , 0, 0);
            case CmdSeq.BulkEraseSetup2      : return new CmdSeqData(_config16.cmdPIC.BulkEraseSetup2     , 0, 0);
            case CmdSeq.ChipErase            : return new CmdSeqData(_config16.cmdPIC.ChipErase           , 0, 0);
            case CmdSeq.BeginProgramming     : return new CmdSeqData(_config16.cmdPIC.BeginProgramming    , 0, 0);
            case CmdSeq.EndProgramming       : return new CmdSeqData(_config16.cmdPIC.EndProgramming      , 0, 0);

            default                          : return null;
        }
    }

    private boolean _pic16_execCmdSeq(final long[] cmdSeq)
    {
        final boolean DEBUG = false;

        // Execute the command sequence
        int idx = 0;

        while(idx < cmdSeq.length) {

            // Get the command sequence data
            final CmdSeqData csd = _getCmdSeqData(cmdSeq[idx++]);

            if(csd == null) return false;

            // Extract the command and its specification
            final int   cmdVal = csd.cmdVal;
            final int   parCnt = csd.parCnt;
            final int   resCnt = csd.resCnt;
            final int[] parVal = (parCnt > 0) ? new int[parCnt] : null;
            final int[] resVal = (resCnt > 0) ? new int[resCnt] : null;

            // Gather the parameter(s) and results()
            for(int p = 0; p < parCnt; ++p) {
                final int par = (int) cmdSeq[idx++];
                parVal[p] = (par >= 0) ? par : _cmdSeqVI[ (int) CmdSeq.VI(par) ];
            }

            for(int r = 0; r < resCnt; ++r) {
                resVal[r] = (int) CmdSeq.VI(cmdSeq[idx++]);
            }

            // Process the command
            switch(cmdVal) {

                case CmdSeqData.CMD_NOP : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_NOP\n" );
                    }
                    break;

                case CmdSeqData.CMD_DELAY_MS : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_DELAY_MS [%d]\n", parVal[0] );
                        SysUtil.sleepMS(parVal[0]);
                    }
                    break;

                ////////////////////////////////////////////////////////////////////////////////////////////////////

                case CmdSeqData.CMD_RESET_ADDRESS : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_RESET_ADDRESS\n" );
                        if( !_pic16_resetAddress() ) return false;
                    }
                    break;

                case CmdSeqData.CMD_SET_ADDRESS : if(true) {
                        if( parVal[0] < _config16.baseProgSpec.configMemAddressBeg ) {
                            if(DEBUG) SysUtil.stdDbg().printf( "CMD_SET_ADDRESS:RI [%04X]\n", parVal[0] );
                            if( !_pic16_resetAddress()     ) return false;
                            if( !_pic16_incAddr(parVal[0]) ) return false;
                        }
                        else {
                            if(DEBUG) SysUtil.stdDbg().printf( "CMD_SET_ADDRESS:CW [%04X]\n", parVal[0] );
                            if( !_pic16_initConfigReadWriteAddress(parVal[0], false) ) return false;
                        }
                    }
                    break;

                case CmdSeqData.CMD_INC_ADDRESS : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_INC_ADDRESS [%d]\n", parVal[0] );
                        if( !_pic16_incAddr(parVal[0]) ) return false;
                    }
                    break;

                ////////////////////////////////////////////////////////////////////////////////////////////////////

                case CmdSeqData.CMD_BITWISE_AND : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_BITWISE_AND [%d] : [%d] [%d]\n", resVal[0], parVal[0], parVal[1] );
                        _cmdSeqVI[ resVal[0] ] = parVal[0] & parVal[1];
                    }
                    break;

                case CmdSeqData.CMD_BITWISE_OR : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_BITWISE_OR [%d] : [%d] [%d]\n", resVal[0], parVal[0], parVal[1] );
                        _cmdSeqVI[ resVal[0] ] = parVal[0] | parVal[1];
                    }
                    break;


                case CmdSeqData.CMD_ERR_IF_CMP_EQ : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_ERR_IF_CMP_EQ [%d] [%d]\n", parVal[0], parVal[1] );
                        if(parVal[0] == parVal[1]) {
                            SysUtil.stdDbg().printf(Texts.ProgXXX_FailPIC_CmpEQ, ProgClassName, parVal[0], parVal[1]);
                            SysUtil.stdDbg().println();
                            return false;
                        }
                    }
                    break;

                case CmdSeqData.CMD_ERR_IF_CMP_NEQ : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_ERR_IF_CMP_NEQ [%d] [%d]\n", parVal[0], parVal[1] );
                        if(parVal[0] != parVal[1]) {
                            SysUtil.stdDbg().printf(Texts.ProgXXX_FailPIC_CmpNEQ, ProgClassName, parVal[0], parVal[1]);
                            SysUtil.stdDbg().println();
                            return false;
                        }
                    }
                    break;

                ////////////////////////////////////////////////////////////////////////////////////////////////////

                case CmdSeqData.CMD_PRINT_2CHARS : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_2CHARS\n" );
                        final char[] s2cc = _X2CC(parVal[0]);
                        SysUtil.stdDbg().printf("%c%c", s2cc[0], s2cc[1]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_SDECNN : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_SDECNN\n" );
                        SysUtil.stdDbg().printf("%+d", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_SDEC03 : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_SDEC03\n" );
                        SysUtil.stdDbg().printf("%+04d", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_SDEC05 : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_SDEC05\n" );
                        SysUtil.stdDbg().printf("%+06d", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_UDECNN : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_UDECNN\n" );
                        SysUtil.stdDbg().printf("%d", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_UDEC03 : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_UDEC03\n" );
                        SysUtil.stdDbg().printf("%03d", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_UDEC05 : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_UDEC05\n" );
                        SysUtil.stdDbg().printf("%05d", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_UHEXNN : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_UHEXNN\n" );
                        SysUtil.stdDbg().printf("%X", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_UHEX02 : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_UHEX02\n" );
                        SysUtil.stdDbg().printf("%02X", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_UHEX04 : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_UHEX04\n" );
                        SysUtil.stdDbg().printf("%04X", parVal[0]);
                    }
                    break;

                case CmdSeqData.CMD_PRINT_NL : if(true) {
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PRINT_NL\n" );
                        SysUtil.stdDbg().println();
                    }
                    break;

                ////////////////////////////////////////////////////////////////////////////////////////////////////

                default : if(true) {
                        // Prepare the command buffer
                        final int[] buf = new int[2 + (parCnt + resCnt) * 4];
                              int   put = 0;
                        // Store the command
                        buf[put++] = 5;
                        buf[put++] = cmdVal;
                        // Store the parameters
                        for(int p = 0; p < parCnt; ++p) {
                            final int[] u16 = _pic16_encodeU14(parVal[p]);
                            buf[put++] = 7;
                            buf[put++] = u16[0];
                            buf[put++] = 7;
                            buf[put++] = u16[1];
                        }
                        // Prepare storage for the results
                        for(int r = 0; r < resCnt; ++r) {
                            buf[put++] = 7;
                            buf[put++] = 0xFF;
                            buf[put++] = 7;
                            buf[put++] = 0xFF;
                        }
                        // Execute the command
                        if(DEBUG) SysUtil.stdDbg().printf( "CMD_PIC [%02X] (%d) (%d) => %d\n", XCom._RU06(cmdVal), parCnt, resCnt, buf.length );
                        if( !_pic16_transfer(buf) ) return false;
                        // Get the result as needed
                        int get = 3;
                        for(int r = 0; r < resCnt; ++r) {
                            _cmdSeqVI[ resVal[r] ] = _pic16_decodeU14( buf[get + 2], buf[get] );
                            get += 4;
                        }
                    }
                    break;

            } // switch

        } // while

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic16_transfer(final int[] buff)
    { return _usb2gpio.spiXBTransferIgnoreSS(IEVal._X, IEVal._X, 0, IEVal._X, IEVal._X, buff); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic16_resetAddress()
    {
        if(_config16.baseProgSpec.cmdResetAddress != CmdSeq._INVALID_ && _config16.baseProgSpec.cmdResetAddress != CmdSeq.CS_NotSupported) {
            return _pic16_transfer( new int[] { 5, _getCmdSeqData(_config16.baseProgSpec.cmdResetAddress).cmdVal } );
        };

        if( !_pic_exitPVM () ) return false;
        if( !_pic_enterPVM() ) return false;

        return true;
    }

    private boolean _pic16_initFlashReadAddress(final long address)
    {
        if( !_pic16_resetAddress() ) return false;

        if(_config16.baseProgSpec.flashAddressPreInc > 0) {
            if( !_pic16_incAddr(_config16.baseProgSpec.flashAddressPreInc) ) return false;
        }

        _addrFRead = (int) address;

        if(_addrFRead > 0) {
            if( !_pic16_incAddr(_addrFRead) ) return false;
        }

        return true;
    }

    private boolean _pic16_initFlashWriteAddress(final long address)
    {
        if( !_pic16_resetAddress() ) return false;

        if(_config16.baseProgSpec.flashAddressPreInc > 0) {
            if( !_pic16_incAddr(_config16.baseProgSpec.flashAddressPreInc) ) return false;
        }

        _addrFWrite = (int) address;

        if(_addrFWrite > 0) {
            if( !_pic16_incAddr(_addrFWrite) ) return false;
        }

        return true;
    }

    private boolean _pic16_initConfigReadWriteAddress(final long address, final boolean flashAddressPreInc)
    {
        // ##### !!! TODO : VERIFY THIS !!! #####

        if(_config16.baseProgSpec.cmdLoadConfiguration != CmdSeq.CS_NotSupported) {
            // NOTE : This part of the function does not use 'Config.BaseProgSpec.flashAddressPreInc'!
            if( !_pic16_loadConfig(                                                              ) ) return false;
            if( !_pic16_incAddr   ( (int) (address - _config16.baseProgSpec.configMemAddressBeg) ) ) return false;
        }

        else {
            // Reset to the end of the memory map (the address of the configuration word)
            if( !_pic16_resetAddress() ) return false;
            // Increment the address only and only if:
            //     # The flag 'BaseProgSpec.configMemOnlyUponEntry' is not set.
            //     # The address is not the address of the first configuration word.
            if( /*!_config16.baseProgSpec.configMemOnlyUponEntry &&*/ address != _config16.memoryConfigBytes.address[0] ) {
                /*
                SysUtil.stdDbg().printf("[%06X]\n", address);
                //*/
                if( flashAddressPreInc && _config16.baseProgSpec.flashAddressPreInc > 0 ) {
                    if( !_pic16_incAddr(_config16.baseProgSpec.flashAddressPreInc) ) return false;
                }
                if( !_pic16_incAddr( (int) address ) ) return false;
            }
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic16_loadConfig()
    {
        if(_config16.baseProgSpec.cmdLoadConfiguration == CmdSeq.CS_NotSupported) return true;

        return _pic16_transfer( new int[] {
            5, _getCmdSeqData(_config16.baseProgSpec.cmdLoadConfiguration).cmdVal,
            7, 0b01111111, // 0x3FFF
            7, 0b11111110  // ---
        } );
    }

    private boolean _pic16_loadConfig(final int value)
    {
        if(_config16.baseProgSpec.cmdLoadConfiguration == CmdSeq.CS_NotSupported) return true;

        final int[] u16 = _pic16_encodeU14(value);

        return _pic16_transfer( new int[] {
            5, _getCmdSeqData(_config16.baseProgSpec.cmdLoadConfiguration).cmdVal,
            7, u16[0],
            7, u16[1]
        } );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic16_incAddr()
    { return _pic16_transfer( new int[] { 5, _getCmdSeqData(_config16.baseProgSpec.cmdIncrementAddress).cmdVal } ); }

    private boolean _pic16_incAddr(final int cnt_)
    {
        if(cnt_ <  0) return false;
        if(cnt_ == 1) return _pic16_incAddr();

        int cnt = cnt_;

        while(cnt > 0) {

            final int   inc = Math.min(32, cnt);
            final int[] buf = new int[inc * 2];

            for(int i = 0; i < inc; ++i) {
                buf[i * 2 + 0] = 5;
                buf[i * 2 + 1] = _getCmdSeqData(_config16.baseProgSpec.cmdIncrementAddress).cmdVal;
            }

            if( !_pic16_transfer(buf) ) return false;

            cnt -= inc;

        } // while

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _pic16_readDataFromXMem(final int cmd)
    {
        final int[] buf = new int[] {
            5, cmd,
            7, 0xFF,
            7, 0xFF
        };

        if( !_pic16_transfer(buf) ) return -1;

        return _pic16_decodeU14( buf[5], buf[3] );
    }

    private int[] _pic16_readDataFromXMemMulti(final int cmd, final int cnt)
    {
        // Send the commands
        final int[] buf = new int[ (2 + 6) * cnt ];
              int   idx = 0;

        for(int i = 0; i < cnt; ++i) {
            buf[idx++] = 5; buf[idx++] = cmd;
            buf[idx++] = 7; buf[idx++] = 0xFF;
            buf[idx++] = 7; buf[idx++] = 0xFF;
            buf[idx++] = 5; buf[idx++] = _getCmdSeqData(_config16.baseProgSpec.cmdIncrementAddress).cmdVal;
        }

        if( !_pic16_transfer(buf) ) return null;

        // Process and return the response
        final int[] res = new int[cnt];
                    idx = 3;
        for(int i = 0; i < cnt; ++i) {
            res[i]  = _pic16_decodeU14( buf[idx + 2], buf[idx] );
            idx    += 8;
        }

        return res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _pic16_readDataFromProgMem()
    { return _pic16_readDataFromXMem( _getCmdSeqData(_config16.baseProgSpec.cmdReadDataFromProgMem).cmdVal ); }

    private int[] _pic16_readDataFromProgMemMulti(final int cnt)
    { return _pic16_readDataFromXMemMulti( _getCmdSeqData(_config16.baseProgSpec.cmdReadDataFromProgMem).cmdVal, cnt ); }

    private boolean _pic16_writeDataToProgMem(final int datah6, final int datal8)
    {
        final int[] u16 = _pic16_encodeU14(datah6, datal8);

        final int[] buf = new int[] {
            5, _getCmdSeqData(_config16.baseProgSpec.cmdLoadDataForProgMem).cmdVal,
            7, u16[0],
            7, u16[1],
            5, _getCmdSeqData(_config16.baseProgSpec.cmdWriteFlash        ).cmdVal,
        };

        if( !_pic16_transfer(buf) ) return false;

        SysUtil.sleepMS(_config16.baseProgSpec.cmdWriteFlashDelayMS);

        if(_config16.baseProgSpec.cmdWriteFlashEnd != CmdSeq._INVALID_ && _config16.baseProgSpec.cmdWriteFlashEnd != CmdSeq.CS_NotSupported) {
            return _pic16_transfer( new int[] { 5, _getCmdSeqData(_config16.baseProgSpec.cmdWriteFlashEnd).cmdVal } );
        };

        return true;
    }

    private boolean _pic16_writeDataToProgMemMulti(final int[] datah6, final int[] datal8)
    {
        // Error if length is different
        if(datah6.length != datal8.length) return false;

        // Perform the write
        final int[] buf = new int[ (2 + 6) * datah6.length ];
              int   idx = 0;

        for(int i = 0; i < datah6.length; ++i) {
            final int[] u16 = _pic16_encodeU14(datah6[i], datal8[i]);
            buf[idx++] = 5; buf[idx++] =                           _getCmdSeqData(_config16.baseProgSpec.cmdLoadDataForProgMem).cmdVal;
            buf[idx++] = 7; buf[idx++] = u16[0];
            buf[idx++] = 7; buf[idx++] = u16[1];
            buf[idx++] = 5; buf[idx++] = (i < datah6.length - 1) ? _getCmdSeqData(_config16.baseProgSpec.cmdIncrementAddress  ).cmdVal
                                                                 : _getCmdSeqData(_config16.baseProgSpec.cmdWriteFlash        ).cmdVal;
        }

        if( !_pic16_transfer(buf) ) return false;

        SysUtil.sleepMS(_config16.baseProgSpec.cmdWriteFlashDelayMS);

        if(_config16.baseProgSpec.cmdWriteFlashEnd != CmdSeq._INVALID_ && _config16.baseProgSpec.cmdWriteFlashEnd != CmdSeq.CS_NotSupported) {
            if( !_pic16_transfer( new int[] { 5, _getCmdSeqData(_config16.baseProgSpec.cmdWriteFlashEnd).cmdVal } ) ) return false;
        };

        // Increment the address
        if( !_pic16_incAddr() ) return false;

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _pic16_readDataFromDataMem()
    {
        // Perform the read
        final int res = _pic16_readDataFromXMem( _getCmdSeqData(_config16.baseProgSpec.cmdReadDataFromDataMem).cmdVal );

        if(res < 0) return -1;

        // Modify and return the response
        return _config16.baseProgSpec.msbAlignEEPROMData ? (res >>> 6) : (res & 0xFF);
    }

    private int[] _pic16_readDataFromDataMemMulti(final int cnt)
    {
        // Perform the read
        final int[] res = _pic16_readDataFromXMemMulti( _getCmdSeqData(_config16.baseProgSpec.cmdReadDataFromDataMem).cmdVal, cnt );

        if(res == null) return null;

        // Modify and return the response
        for(int i = 0; i < res.length; ++i) res[i] = _config16.baseProgSpec.msbAlignEEPROMData ? (res[i] >>> 6) : (res[i] & 0xFF);

        return res;
    }

    private boolean _pic16_writeDataToDataMem(final int data8, final boolean eepromErased)
    {
        final int[] u16 = _pic16_encodeU14( _config16.baseProgSpec.msbAlignEEPROMData ? (data8 << 6) : data8 );

        final int[] buf = new int[] {
            5, _getCmdSeqData(               _config16.baseProgSpec.cmdLoadDataForDataMem                                        ).cmdVal,
            7, u16[0],
            7, u16[1],
            5, _getCmdSeqData(eepromErased ? _config16.baseProgSpec.cmdWriteEEPROMNoErase : _config16.baseProgSpec.cmdWriteEEPROM).cmdVal,
        };

        if( !_pic16_transfer(buf) ) return false;

        SysUtil.sleepMS(_config16.baseProgSpec.cmdWriteEEPROMDelayMS);

        if(_config16.baseProgSpec.cmdWriteEEPROMEnd != CmdSeq._INVALID_ && _config16.baseProgSpec.cmdWriteEEPROMEnd != CmdSeq.CS_NotSupported) {
            return _pic16_transfer( new int[] { 5, _getCmdSeqData(_config16.baseProgSpec.cmdWriteEEPROMEnd).cmdVal } );
        };

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _pic16_readDataFromConfigMem(final long address)
    {
        // Initialize the address
        if( !_pic16_initConfigReadWriteAddress(address, true) ) return -1;

        // Perform the read
        return _pic16_readDataFromProgMem();
    }

    private boolean _pic16_writeDataToConfigMem(final long address, final int data14)
    {
        /*
        SysUtil.stdDbg().printf("%04X = %04X\n", address, data14);
        //*/

        // Initialize the address
        if( !_pic16_initConfigReadWriteAddress(address, true) ) return false;

        /*
        if(true) return true;
        //*/

        // Perform the write
        final int[] u16 = _pic16_encodeU14(data14);

        final int[] buf = new int[] {
            5, _getCmdSeqData(_config16.baseProgSpec.cmdLoadDataForProgMem).cmdVal,
            7, u16[0],
            7, u16[1],
            5, _getCmdSeqData(_config16.baseProgSpec.cmdWriteConfig       ).cmdVal,
        };

        if( !_pic16_transfer(buf) ) return false;

        SysUtil.sleepMS(_config16.baseProgSpec.cmdWriteConfigDelayMS);

        /*
        SysUtil.stdDbg().printf( "#### %s %s\n", XCom.uint08binStr(u16[0]), XCom.uint08binStr(u16[1]) );
        //*/

        if(_config16.baseProgSpec.cmdWriteConfigEnd != CmdSeq._INVALID_ && _config16.baseProgSpec.cmdWriteConfigEnd != CmdSeq.CS_NotSupported) {
            return _pic16_transfer( new int[] { 5, _getCmdSeqData(_config16.baseProgSpec.cmdWriteConfigEnd).cmdVal } );
        };

        return true;
    }

    private boolean _pic16_writeDataToConfigMem(final long address, final long[] data14)
    {
        // Initialize the address
        if( !_pic16_initConfigReadWriteAddress(address, true) ) return false;

        /*
        if(true) return true;
        //*/

        // Perform the write
        final int[] buf =  new int[ (data14.length * 4 + 1) * 2 ];
              int   idx = 0;

        for(final long d : data14) {
            final int[] u16 = _pic16_encodeU14( (int) d );
            buf[idx++] = 5; buf[idx++] = _getCmdSeqData(_config16.baseProgSpec.cmdLoadDataForProgMem).cmdVal;
            buf[idx++] = 7; buf[idx++] = u16[0];
            buf[idx++] = 7; buf[idx++] = u16[1];
            buf[idx++] = 5; buf[idx++] = _getCmdSeqData(_config16.baseProgSpec.cmdIncrementAddress  ).cmdVal;
        }
            buf[idx++] = 5; buf[idx++] = _getCmdSeqData(_config16.baseProgSpec.cmdWriteConfig       ).cmdVal;

        /*
        if(true) return true;
        //*/

        if( !_pic16_transfer(buf) ) return false;

        SysUtil.sleepMS(_config16.baseProgSpec.cmdWriteConfigDelayMS);

        if(_config16.baseProgSpec.cmdWriteConfigEnd != CmdSeq._INVALID_ && _config16.baseProgSpec.cmdWriteConfigEnd != CmdSeq.CS_NotSupported) {
            return _pic16_transfer( new int[] { 5, _getCmdSeqData(_config16.baseProgSpec.cmdWriteConfigEnd).cmdVal } );
        };

        return true;
    }

    /*
    // Perform the write
    final int   cnt = 4;//(int) (_config16.memoryConfigBytes.addressEnd - _config16.memoryConfigBytes.addressBeg);
    final int[] buf =  new int[ (cnt * 4 + 1) * 2 ];
          int   idx = 0;

    for(int i = 0; i < cnt; ++i) {
        buf[idx++] = 5; buf[idx++] = _getCmdSeqData(_config16.baseProgSpec.cmdLoadDataForProgMem).cmdVal;
        buf[idx++] = 7; buf[idx++] = 0xFF;
        buf[idx++] = 7; buf[idx++] = 0xFF;
        buf[idx++] = 5; buf[idx++] = _getCmdSeqData(_config16.baseProgSpec.cmdIncrementAddress  ).cmdVal;
    }
        buf[idx++] = 5; buf[idx++] = _getCmdSeqData(_config16.baseProgSpec.cmdWriteConfig       ).cmdVal;

    for(int i = 0; i < _config16.memoryConfigBytes.address.length; ++i) {
        final int   addr = (int) (_config16.memoryConfigBytes.address[i] - _config16.memoryConfigBytes.addressBeg);
        final int[] u16  = _pic16_encodeU14( (int) data14[i] );
        if(addr * 8 >= buf.length) continue;
        //*
        SysUtil.stdDbg().printf( "[%04X->%d] = %04X | %02X %02X\n", _config16.memoryConfigBytes.address[i], addr, data14[i], u16[0], u16[1] );
        //* /
        buf[addr * 8 + 3] = u16[0];
        buf[addr * 8 + 5] = u16[1];
    }

    /*
    idx = 0;
    for(int i = 0; i < cnt; ++i) {
        SysUtil.stdDbg().printf("%d %02X\n", buf[idx++], buf[idx++]);
        SysUtil.stdDbg().printf("%d %02X\n", buf[idx++], buf[idx++]);
        SysUtil.stdDbg().printf("%d %02X\n", buf[idx++], buf[idx++]);
        SysUtil.stdDbg().printf("%d %02X\n", buf[idx++], buf[idx++]);
    }
        SysUtil.stdDbg().printf("%d %02X\n", buf[idx++], buf[idx++]);
    if(true) return true;
    //* /
    */

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic16_subPartIsNotSupported()
    {
        // For the time being, these subparts are not supported
        return _config16.baseProgSpec.subPart == SubPart.C  || _config16.baseProgSpec.subPart == SubPart.CE ||
               _config16.baseProgSpec.subPart == SubPart.CR || _config16.baseProgSpec.subPart == SubPart.HV ||
               _config16.baseProgSpec.subPart == SubPart.LF;
    }

    @Override
    protected int _picxx_minSANBAlignSize()
    { return 2; }

    @Override
    protected int _picxx_configByteSize()
    { return 2; }

    @Override
    public byte _flashMemoryEmptyValue(final int posIndex)
    {
        return (byte) ( ( (posIndex & 1) == 0 ) ? _config16.baseProgSpec.flashEAddressEmptyVal
                                                : _config16.baseProgSpec.flashOAddressEmptyVal
                      );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected int _picxx_readDeviceIDFull()
    {
        // Check for unsupported subparts
        if( _pic16_subPartIsNotSupported() ) return -1;

        // Check if the MCU does not have a device ID word
        if(_config16.baseProgSpec.deviceIDWordLocOfsset <= 0) return 0x00;

        // Set the address
        if( !_pic16_loadConfig(                                            ) ) return -1; // Address at (configMemAddressBeg                        )
        if( !_pic16_incAddr   (_config16.baseProgSpec.deviceIDWordLocOfsset) ) return -1; // Address at (configMemAddressBeg + deviceIDWordLocOfsset)

        // Read the device ID
        final int devID = _pic16_readDataFromProgMem();

        // Return the device ID
        return (devID == 0x00 || devID == 0x3FFF) ? 0x00 : devID;
    }

    @Override
    protected int _picxx_readDeviceID()
    {
        // Remove the device revision code
        return _picxx_readDeviceIDFull() & 0xFFE0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean _picxx_chipErase()
    {
        // Check for unsupported subparts
        if( _pic16_subPartIsNotSupported() ) return false;

        // Unlock (and most likely erase both flash and EEPROM) as needed
        if(_config16.baseProgSpec.cmdChipErase_Unlock != null) {
            if( !_pic16_execCmdSeq(_config16.baseProgSpec.cmdChipErase_Unlock) ) return false;
        }

        // Erase flash as needed
        if( !_pix16_blankCheckFlash() ) {
            if( !_pic16_execCmdSeq(_config16.baseProgSpec.cmdChipErase_Flash) ) return false;
        }

        // Erase EEPROM as needed
        if( _config16.baseProgSpec.cmdChipErase_EEPROM != null && !_pix16_blankCheckEEPROM() ) {
            if( !_pic16_execCmdSeq(_config16.baseProgSpec.cmdChipErase_EEPROM) ) return false;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # For this function to work properly, the target must be started from a cold start state.
     *        # Disconnect all external peripherals and the programmer from the host USB port. Make sure
     *          the target Vdd is off before reconnecting the programmer to the host USB port. Depending
     *          on the programmer, Vdd may be turned on automatically. If not, you will have to turn it
     *          on manually. Make sure to set ‘vpp_vdd_delay’ to at least a few seconds if you have to turn
     *          it on manually, so you will have enough time to turn on the Vdd.
     */
    public boolean _pic16_recover(final int vpp_vdd_delay_)
    {
        // Error if the command sequence for recovery is not available
        if(_config16.baseProgSpec.cmdChipErase_Recover == null) return false;

        // Initialize, execute the command sequence, and uninitialize
        final int vpp_vdd_delay = -Math.abs(vpp_vdd_delay_);

        if( !_begin           (0, vpp_vdd_delay                           ) ) return false;
        if( !_pic16_execCmdSeq(_config16.baseProgSpec.cmdChipErase_Recover) ) return false;
        if( !end              (                                           ) ) return false;

        // Done
        return true;
    }

    /*
     * !!! WARNING !!! !!! WARNING !!! !!! WARNING !!!
     *
     * Try using '_pic16_recover()' a few times before resorting to '_pic16_unbrick()'.
     *
     * # Using this function may cause (some/all) MCU factory configuration data to be lost permanently!
     * # In this case, you will need to restore the factory configuration data manually.
     *
     * !!! WARNING !!! !!! WARNING !!! !!! WARNING !!!
     *
     * ##### ??? TODO : Add a function/method so that users can manually write the lost factory configuration data ??? HOW ??? #####
     */
    public boolean _pic16_unbrick(final int vpp_vdd_delay_)
    {
        // Error if the command sequence for unbricking is not available
        if(_config16.baseProgSpec.cmdChipErase_Unbrick == null) return false;

        // Initialize, execute the command sequence, and uninitialize
        final int vpp_vdd_delay = -Math.abs(vpp_vdd_delay_);

        if( !_begin           (0, vpp_vdd_delay                           ) ) return false;
        if( !_pic16_execCmdSeq(_config16.baseProgSpec.cmdChipErase_Unbrick) ) return false;
        if( !end              (                                           ) ) return false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _addrFRead  = -1;
    private int _addrFWrite = -1;

    @Override
    protected boolean _picxx_readFlash(final long address_, final int[] dstBuff)
    {
        // Check for unsupported subparts
        if( _pic16_subPartIsNotSupported() ) return false;

        // Error if the address and length is not even
        if( (address_       & 1) != 0 ) return false;
        if( (dstBuff.length & 1) != 0 ) return false;

        final long address = address_ / 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file

        // Initialize the address as needed
        if(_addrFRead < 0 || _addrFRead > address) {
            if( !_pic16_initFlashReadAddress(address) ) return false;
        }

        // Pre-increment the address as needed
        else {
            final int delta = (int) (address - _addrFRead);
            if(delta < 0) return false;
            if(delta > 0) {
                if( !_pic16_incAddr(delta) ) return false;
            }
        }

        // Read the bytes
        final int max = dstBuff.length / 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
              int idx = 0;

        while(idx < max) {

            // Read the word(s) - with automatic address increment
            final int   len = Math.min(12, max - idx);
            final int[] res = _pic16_readDataFromProgMemMulti(len);

            if(res == null) return false;

            // Store the bytes
            for(int i = 0; i < len; ++i) {
                dstBuff[ (idx + i )* 2 + 0 ] = (res[i] >> 0) & 0xFF;
                dstBuff[ (idx + i) * 2 + 1 ] = (res[i] >> 8) & 0xFF;
            }

            // Update the index
            idx += len;

        } // while

        // Update the address
        _addrFRead += max;

        if(_addrFRead > _config16.memoryFlash.address + _config16.memoryFlash.totalSize - 1) _addrFRead = -1;

        // Done
        return true;
    }

    @Override
    protected boolean _picxx_writeFlash(final boolean firstCall, final long address_, final int[] srcBuff)
    {
        // Check for unsupported subparts
        if( _pic16_subPartIsNotSupported() ) return false;

        // Error if the address and length is not even
        if( (address_       & 1) != 0 ) return false;
        if( (srcBuff.length & 1) != 0 ) return false;

        final long address = address_ / 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file

        // Initialize the address as needed
        if(firstCall) {
            if( !_pic16_initFlashWriteAddress(address) ) return false;
        }

        // Pre-increment the address as needed
        else {
            final int delta = (int) (address - _addrFWrite);
            if(delta < 0) return false;
            if(delta > 0) {
                if( !_pic16_incAddr(delta) ) return false;
                _addrFWrite += delta;
            }
        }

        // Write multiple words at once
        if(_config16.baseProgSpec.cntWriteFlashMulti > 1) {

            // Prepare the buffer
            final int[] datah6 = new int[_config16.baseProgSpec.cntWriteFlashMulti];
            final int[] datal8 = new int[_config16.baseProgSpec.cntWriteFlashMulti];

            // Write the bytes
            final int max = srcBuff.length / 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
                  int idx = 0;

            while(idx < max) {

                // Prepare the word(s)
                for(int m = 0; m < _config16.baseProgSpec.cntWriteFlashMulti; ++m) {
                    datah6[m] = srcBuff[ (idx + m) * 2 + 1 ];
                    datal8[m] = srcBuff[ (idx + m) * 2 + 0 ];
                    /*
                    System.out.printf("%02X %02X\n", datal8[m], datah6[m]);
                    //*/
                }

                // Write the word(s) - with automatic address increment
                if( !_pic16_writeDataToProgMemMulti(datah6, datal8) ) return false;

                // Update the index
                idx += _config16.baseProgSpec.cntWriteFlashMulti;

            } // while

            // Update the address
            _addrFWrite += max;

        }

        // Write one word at a time
        else  {

            for(int i = 0; i < srcBuff.length; i += 2) { // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file

                // Write the bytes
                if( !_pic16_writeDataToProgMem(srcBuff[i + 1], srcBuff[i + 0]) ) return false;

                // Increment the address
                if( !_pic16_incAddr() ) return false;

            } // for

            // Update the address
            _addrFWrite += srcBuff.length / 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file

        }

        // Done
        return true;
    }

    private boolean _pix16_blankCheckFlash()
    {
        // Reset the address
        if( !_pic16_resetAddress() ) return false;

        // Check the bytes
        final int EVL = (_config16.baseProgSpec.flashOAddressEmptyVal << 8) | 0xFF;
        final int max =  _config16.memoryFlash.totalSize / 2; // NOTE : A PIC16 word is 12/14-bits wide and is stored as two bytes in the firmware file
              int idx = 0;

        while(idx < max) {

            // Read the word(s) - with automatic address increment
            final int   len = Math.min(12, max - idx);
            final int[] res = _pic16_readDataFromProgMemMulti(len);

            if(res == null) return false;

            // Check the word(s)
            for(int i = 0; i < len; ++i) {
                /*
                SysUtil.stdDbg().printf("[%03d] %04X | %04X\n", i, res[i], EVL );
                //*/
                if(res[i] != EVL) return false;
            }

            // Update the index
            idx += len;

        } // while

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean _picxx_supportsEEPROMAutoErase()
    { return _config16.baseProgSpec.cmdWriteEEPROMNoErase != _config16.baseProgSpec.cmdWriteEEPROM; }

    @Override
    protected boolean _picxx_readEntireEEPROM(final int[] dstBuff)
    {
        // Check for unsupported subparts
        if( _pic16_subPartIsNotSupported() ) return false;

        // Initialize/reset the address
        if(_config16.baseProgSpec.cmdReadDataFromDataMem == _config16.baseProgSpec.cmdReadDataFromProgMem) {
            if( !_pic16_initFlashReadAddress( (int) _config16.memoryEEPROM.addressBeg ) ) return false;
        }
        else {
            if( !_pic16_resetAddress() ) return false;
        }

        // Read the bytes
        final int max = dstBuff.length;
              int idx = 0;

        while(idx < max) {

            // Read the byte(s) - with automatic address increment
            final int   len = Math.min(12, max - idx);
            final int[] res = _pic16_readDataFromDataMemMulti(len);

            if(res == null) return false;

            // Store the byte(s)
            for(int i = 0; i < len; ++i)  dstBuff[idx + i] = res[i];

            // Update the index
            idx += len;

        } // while

        // Done
        return true;
    }

    @Override
    protected boolean _picxx_writeEntireEEPROM(final int[] srcBuff, final boolean[] fDirty, final boolean eepromErased)
    {
        // Check for unsupported subparts
        if( _pic16_subPartIsNotSupported() ) return false;

        // Initialize/reset the address
        if(_config16.baseProgSpec.cmdLoadDataForDataMem == _config16.baseProgSpec.cmdLoadDataForProgMem) {
            if( !_pic16_initFlashWriteAddress( (int) _config16.memoryEEPROM.addressBeg ) ) return false;
        }
        else {
            if( !_pic16_resetAddress() ) return false;
        }

        // Write the bytes
        for(int i = 0; i < srcBuff.length; ++i) {

            if(fDirty[i]) {
                // Only write the byte if it is dirty
                if( !_pic16_writeDataToDataMem(srcBuff[i], eepromErased) ) return false;
            }

            if( !_pic16_incAddr() ) return false;

        } // for

        // Done
        return true;
    }

    private boolean _pix16_blankCheckEEPROM()
    {
        // Reset the address
        if( !_pic16_resetAddress() ) return false;


        // Check the bytes
        final int max = _config16.memoryEEPROM.totalSize;
              int idx = 0;

        while(idx < max) {

            // Read the byte(s) - with automatic address increment
            final int   len = Math.min(12, max - idx);
            final int[] res = _pic16_readDataFromDataMemMulti(len);

            if(res == null) return false;

            // Check the byte(s)
            for(int i = 0; i < len; ++i) if(res[i] != 0xFF) return false;

            // Update the index
            idx += len;

        } // while

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected long _picxx_readConfigByte(final long address)
    {
        // Check for unsupported subparts
        if(   _config16.baseProgSpec.subPart == SubPart.C  || _config16.baseProgSpec.subPart == SubPart.CE
           || _config16.baseProgSpec.subPart == SubPart.CR || _config16.baseProgSpec.subPart == SubPart.HV
           || _config16.baseProgSpec.subPart == SubPart.LF
        ) return -1;

        // Read the configuration byte
        return _pic16_readDataFromConfigMem(address);
    }

    @Override
    protected boolean _picxx_writeConfigBytes(final long[] refBuff, final long[] newBuff)
    {
        // Check for unsupported subparts
        if( _pic16_subPartIsNotSupported() ) return false;

        // Check if the MCU requires multiple words to be written simultaneously and
        // if there are indeed multiple words to be written
        if(true && _config16.baseProgSpec.cntWriteFlashMulti > 1 && newBuff.length > 1) {

            // ##### !!! TODO : Is this correct for all kinds of PIC16F MCUs?                              !!! #####
            // ##### !!! TODO : How if '_config16.baseProgSpec.cntWriteFlashMulti < (newBuff.length - 1)'? !!! #####
            // ##### !!! TODO : How if there is more than one configuration word?                          !!! #####

            // ##### !!! TODO : How about PIC10F and PIC12F MCUs?                                          !!! #####

            // In this case, the 'Config' ensures that the first value is the configuration word;
            // only write it if it changes (it must be written on its own);
            final long newValCW = _getNewCWValue(0, refBuff, newBuff, _config16);

            if(newValCW != refBuff[0]) {
                /*
                SysUtil.stdErr().printf("### [%08X] : %04X => %04X\n", _config16.memoryConfigBytes.address[0], refBuff[0], newValCW);
                //*/
                if( !_pic16_writeDataToConfigMem( (int) _config16.memoryConfigBytes.address[0], (int) newValCW ) ) return false;
            }

            // The next values are the user IDs; only write them if any of them change
            boolean dirty = false;

            for(int i = 1; i < _config16.memoryConfigBytes.address.length; ++i) {

                // Skip those that are not used
                if(_config16.memoryConfigBytes.address[i] < 0) continue;

                // Checl the new value
                final long newVal = _getNewCWValue(i, refBuff, newBuff, _config16);

                if(newVal != refBuff[i]) {
                    dirty = true;
                    break;
                }

            } // for

            if(!dirty) return true;

            // Write the bytes
            return _pic16_writeDataToConfigMem( _config16.memoryConfigBytes.addressBeg, Arrays.copyOfRange(newBuff, 1, newBuff.length) );

        } // if

        // The MCU does not require multiple words to be written simultaneously;
        // write the configuration bytes one by one as needed
        for(int i = 0; i < _config16.memoryConfigBytes.address.length; ++i) {

            // Skip those that are not used
            if(_config16.memoryConfigBytes.address[i] < 0) continue;

            // Get the new value
            final long newVal = _getNewCWValue(i, refBuff, newBuff, _config16);

            // Skip those that are not changed
            if(newVal == refBuff[i]) continue;

            // Write the word
            /*
            SysUtil.stdErr().printf("### [%08X] : %04X => %04X\n", _config16.memoryConfigBytes.address[i], refBuff[i], newVal);
            //*/
            if( !_pic16_writeDataToConfigMem( (int) _config16.memoryConfigBytes.address[i], (int) newVal ) ) return false;

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : PIC16 does not support programming executive (PE).

    @Override
    protected ProgPIC_EICSP.CmdPE _picxx_pe_cmdPE()
    { return null; }

    @Override
    protected byte[] _picxx_pe_checkAdjustPE(final byte[] data)
    { return null; }

    @Override
    protected int[] _picxx_pe_readSavedWords()
    { return null; }

    @Override
    protected boolean _picxx_pe_writeSavedWords(final int[] srcBuff)
    { return false; }

    @Override
    protected boolean _picxx_pe_eraseArea()
    { return false; }

    @Override
    protected boolean _picxx_pe_writeData(final boolean firstCall, final long address, final int[] srcBuff)
    { return false; }

    @Override
    protected boolean _picxx_pe_readData(final long address, final int[] dstBuff)
    { return false; }

} // class ProgPIC16
