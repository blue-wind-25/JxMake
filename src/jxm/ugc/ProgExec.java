/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.PrintStream;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.tool.*;
import jxm.tool.fwc.*;
import jxm.xb.*;


public class ProgExec {

    private static enum ExecMode {
        Single,
        MultipleStart,
        MultipleNext,
        MultipleDone
    }

    @FunctionalInterface
    static private interface GenericExecFunc {
        abstract public boolean handle() throws Exception;
    }

    @FunctionalInterface
    static private interface SpecificCommandHandler {
        @SuppressWarnings("unchecked")
        abstract public int handle(final String curCmdStr, final int curCmdIdx) throws Exception;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public final static String CMD_NoOperation    = "no_operation";

    public final static String CMD_ChipErase      = "chip_erase";

    public final static String CMD_ReadFlash      = "read_flash";
    public final static String CMD_WriteFlash     = "write_flash"; // It is the same as CMD_WriteFlashASec for targets that support it
    public final static String CMD_WriteFlashBSec = "write_flash_boot_section";
    public final static String CMD_WriteFlashASec = "write_flash_application_section";
    public final static String CMD_VerifyFlash    = "verify_flash";

    public final static String CMD_ReadEEPROM     = "read_eeprom";
    public final static String CMD_WriteEEPROM    = "write_eeprom";
    public final static String CMD_WriteEEPROMEmd = "write_embedded_eeprom";

    public final static String CMD_ReadFuses      = "read_fuses";
    public final static String CMD_WriteFuses     = "write_fuses";
    public final static String CMD_WriteFusesEmd  = "write_embedded_fuses";

    public final static String CMD_ReadLockBits   = "read_lockbits";
    public final static String CMD_WriteLockBits  = "write_lockbits";

    public final static String CMD_PIC_SetEICS_ES = "pic_set_eicsp_extra_speed";
    public final static String CMD_PIC_ProgramPE  = "pic_program_pe";

    public final static String CMD_PIC16_Recover  = "pic16_recover";
    public final static String CMD_PIC16_Unbrick  = "pic16_unbrick";

    public final static String CMD_JTAG_PlaySVF   = "play_svf";

    // ##### ??? TODO : Add more commands ??? #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    private static class ResultBuffer extends ArrayList<Long> {
        void addU08(final int value)
        { super.add( (value < 0) ? - 1 : (value & 0x000000FFL) ); }

        void addU16(final long value)
        { super.add( (value < 0) ? - 1 : (value & 0x0000FFFFL) ); }

        void addU32(final long value)
        { super.add( (value < 0) ? - 1 : (value & 0xFFFFFFFFL) ); }
    }

    private       USB2GPIO            _usb2gpio          = null;

    private       ProgISP             _isp               = null;
    private       ProgTPI             _tpi               = null;
    private       ProgUPDI            _updi              = null;
    private       ProgPDI             _pdi               = null;
    private       ProgLGT8            _lgt8              = null;
    private       ProgSWIM            _swim              = null;
    private       ProgSWD             _swd               = null;
    private       ProgPIC             _pic               = null;
    private       ProgJTAG            _jtag              = null;

    private       ProgBootAVR109      _avr109            = null;
    private       ProgBootSTK500      _stk500            = null;
    private       ProgBootSTK500v2    _stk500v2          = null;
    private       ProgBootChip45      _chip45            = null;
    private       ProgBootTSB         _tsb               = null;
    private       ProgBootURCLOCK     _urclock           = null;
    private       ProgBootSTM32Serial _stm32Ser          = null;
    /* ##### !!! TODO !!! #####
    private       ProgBootSTM32DFU    _stm32DFU          = null;
    private       ProgBootLUFAHID     _lufaHID           = null;
    //*/
    private       ProgBootLUFAPrinter _lufaPRN           = null;
    /* ##### !!! TODO !!! #####
    private       ProgBootAVRDFU      _avrDFU            = null;
    //*/
    private       ProgBootOpenBLT     _oblt              = null;
    private       ProgBootSAMBA       _samba             = null;
    /* ##### !!! TODO !!! #####
    private       ProgBootUSBasp      _usbasp            = null;
    //*/

    private       String              _device            = null;
    private       int                 _vid               = -1;
    private       int                 _pid               = -1;
    private       String              _serialNumber      = null;
    private       int                 _speed             = -1;
    private       int                 _magicBaudrate     = -1;
    private       int[]               _mcuSig            = null;
    private       int                 _rs485Address      = -1;
    private       long                _extraAddress      = -1;
    private       int                 _extraSize1        = -1;
    private       int                 _extraSize2        = -1;
    private       String              _extraName1        = null;
    private       String              _extraName2        = null;
    private       String              _curPassword       = null;
    private       String              _newPassword       = null;
    private       String              _userCBName        = null;
    private       int[]               _u08Key            = null;
    private       long[]              _u32Key            = null;
    private       int                 _idxDefMultidropID = -1;
    private       long[]              _multidropIDs      = null;

    private       byte[]              _fwDataBuff        = null;
    private       int                 _fwStartAddress    = 0;
    private       int                 _fwLength          = 0;

    private       byte[]              _cfDataBuff        = null;
    private       int                 _cfStartAddress    = 0;
    private       int                 _cfLength          = 0;

    private       byte[]              _epDataBuff        = null;
    private       int                 _epStartAddress    = 0;
    private       int                 _epLength          = 0;

    private       PrintStream         _printStream       = null;
    private       IntConsumer         _progressCB        = null;

    private       ExecMode            _execMode          = ExecMode.MultipleDone;
    private final ResultBuffer        _resultBuffer      = new ResultBuffer();

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static byte _byteDecode(final String str)
    { return (byte) _intDecode(str); }

    private static byte _byteDecode(final String[] str, final int idx)
    { return (byte) _intDecode(str, idx); }

    private static int _intDecode(final String str)
    { return Integer.decode(str); }

    private static int _intDecode(final String[] str, final int idx)
    { return Integer.decode(str[idx]); }

    private static long _longDecode(final String str)
    { return Long.decode(str); }

    private static long _longDecode(final String[] str, final int idx)
    { return Long.decode(str[idx]); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static <T> String _getProgConfigBasicXXXStr(final String typeName, final Class<T> clazz, final String configName_) throws Exception
    {
        final String FuncName   = "getProgConfigBasic" + typeName + "Str";
        final String configName = XCom.re_replaceMultipleWhitespacesAndDash(configName_, "");

        final Method method     = clazz.getMethod("serialize", Object.class);

        // Check for specific part request
        if( configName.indexOf(":") > 0 ) {
            // Get the programmer name and part name
            final String[] ppStr = configName.split(":");
            // Generate the configuration string
            final String progName = ppStr[0].toUpperCase();
            final String partName = ppStr[1].toUpperCase();
            if(ppStr.length == 2) {
                try {
                    switch(progName) {

                        case "ISP"         :                             return (String) method.invoke( null, ProgISP            .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "TPI"         :                             return (String) method.invoke( null, ProgTPI            .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "UPDI"        :                             return (String) method.invoke( null, ProgUPDI           .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "PDI"         :                             return (String) method.invoke( null, ProgPDI            .Config     .class.getMethod(ppStr[1]).invoke(null) );

                        case "LGT8"        :                             return (String) method.invoke( null, ProgLGT8           .Config     .class.getMethod(ppStr[1]).invoke(null) );

                        case "SWIM"        :
                                 if( partName.startsWith("STM8S"  ) )    return (String) method.invoke( null, ProgSWIM           .ConfigSTM8S.class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("STM8AF" ) )    return (String) method.invoke( null, ProgSWIM           .ConfigSTM8S.class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("STM8L"  ) )    return (String) method.invoke( null, ProgSWIM           .ConfigSTM8L.class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("STM8AL" ) )    return (String) method.invoke( null, ProgSWIM           .ConfigSTM8L.class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("STM8T"  ) )    return (String) method.invoke( null, ProgSWIM           .ConfigSTM8L.class.getMethod(ppStr[1]).invoke(null) );
                            else                                         break;

                        case "SWD"         :                             return (String) method.invoke( null, ProgSWD            .Config     .class.getMethod(ppStr[1]).invoke(null) );

                        case "PIC"         :
                                 if( partName.startsWith("PIC10"  ) )    return (String) method.invoke( null, ProgPIC16          .Config     .class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("PIC12"  ) )    return (String) method.invoke( null, ProgPIC16          .Config     .class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("PIC16"  ) )    return (String) method.invoke( null, ProgPIC16          .Config     .class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("PIC18"  ) )    return (String) method.invoke( null, ProgPIC18          .Config     .class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("PIC24"  ) )    return (String) method.invoke( null, ProgPIC24          .Config     .class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("DSPIC30") )    return (String) method.invoke( null, ProgDSPIC30        .Config     .class.getMethod(ppStr[1]).invoke(null) );
                            else if( partName.startsWith("DSPIC33") )    return (String) method.invoke( null, ProgDSPIC33        .Config     .class.getMethod(ppStr[1]).invoke(null) );
                          //else if( partName.startsWith("PIC32M" ) )    return (String) method.invoke( null, ProgPIC32M         .Config     .class.getMethod(ppStr[1]).invoke(null) );
                            else                                         break;

                        case "AVR109"      :                             return (String) method.invoke( null, ProgBootAVR109     .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "STK500"      :                             return (String) method.invoke( null, ProgBootSTK500     .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "STK500V2"    :                             return (String) method.invoke( null, ProgBootSTK500v2   .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "CHIP45"      :                             return (String) method.invoke( null, ProgBootChip45     .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "TSB"         :                             return (String) method.invoke( null, ProgBootTSB        .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "URCLOCK"     :                             return (String) method.invoke( null, ProgBootURCLOCK    .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "STM32SERIAL" :                             return (String) method.invoke( null, ProgBootSTM32Serial.Config     .class.getMethod(ppStr[1]).invoke(null) );
                        /* ##### !!! TODO !!! #####
                        case "STM32DFU"    :                             return (String) method.invoke( null, ProgBootSTM32DFU   .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "LUFAHID"     :                             return (String) method.invoke( null, ProgBootLUFAHID    .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "AVRDFU"      :                             return (String) method.invoke( null, ProgBootAVRDFU     .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        //*/
                        case "OPENBLT"     :                             return (String) method.invoke( null, ProgBootOpenBLT    .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        case "SAMBA"       :                             return (String) method.invoke( null, ProgBootSAMBA      .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        /* ##### !!! TODO !!! #####
                        case "USBASP"      :                             return (String) method.invoke( null, ProgBootUSBasp     .Config     .class.getMethod(ppStr[1]).invoke(null) );
                        //*/

                        default            :                             return (String) method.invoke( null, Class.forName(ppStr[0])              .getMethod(ppStr[1]).invoke(null) );

                    } // switch
                }

                catch(final NoSuchMethodException e) {
                    // Unsupported programmer name and/or part name
                    throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecUnspProgConfigN, FuncName, configName_);
                }
            } // if
            // Invalid programmer name and/or part name
            throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecInvlProgConfigN, FuncName, configName_);
        } // if

        // Generic part request
        switch( configName.toUpperCase() ) {

            case "ISP"         : return (String) method.invoke( null, new ProgISP            .Config     ()                               );
            case "TPI"         : return (String) method.invoke( null, new ProgTPI            .Config     ()                               );
            case "UPDI"        : return (String) method.invoke( null, new ProgUPDI           .Config     ()                               );
            case "PDI"         : return (String) method.invoke( null, new ProgPDI            .Config     ()                               );

            case "LGT8"        : return (String) method.invoke( null, new ProgLGT8           .Config     ()                               );

            case "SWIM"        : return (String) method.invoke( null, new ProgSWIM           .Config     ()                               );
            case "SWIMSTM8S"   : return (String) method.invoke( null, new ProgSWIM           .ConfigSTM8S()                               );
            case "SWIMSTM8L"   : return (String) method.invoke( null, new ProgSWIM           .ConfigSTM8L()                               );

            case "SWD"         : return (String) method.invoke( null, new ProgSWD            .Config     ()                               );

            case "PIC10"       : /* FALLTHROUGH */
            case "PIC12"       : /* FALLTHROUGH */
            case "PIC16"       : return (String) method.invoke( null, new ProgPIC16          .Config     ()                               );

            case "PIC18"       : return (String) method.invoke( null, new ProgPIC18          .Config     ()                               );
            case "PIC24"       : return (String) method.invoke( null, new ProgPIC24          .Config     ()                               );
            case "DSPIC30"     : return (String) method.invoke( null, new ProgDSPIC30        .Config     ()                               );
            case "DSPIC33"     : return (String) method.invoke( null, new ProgDSPIC33        .Config     ()                               );
          //case "PIC32M"      : return (String) method.invoke( null, new ProgPIC32M         .Config     ()                               );

            case "AVR109"      : return (String) method.invoke( null, new ProgBootAVR109     .Config     ()                               );
            case "STK500"      : return (String) method.invoke( null, new ProgBootSTK500     .Config     ()                               );
            case "STK500V2"    : return (String) method.invoke( null, new ProgBootSTK500v2   .Config     ()                               );
            case "CHIP45"      : return (String) method.invoke( null, new ProgBootChip45     .Config     ()                               );
            case "TSB"         : return (String) method.invoke( null, new ProgBootTSB        .Config     ()                               );
            case "URCLOCK"     : return (String) method.invoke( null, new ProgBootURCLOCK    .Config     ()                               );
            case "STM32SERIAL" : return (String) method.invoke( null, new ProgBootSTM32Serial.Config     ()                               );
            /* ##### !!! TODO !!! #####
            case "STM32DFU"    : return (String) method.invoke( null, new ProgBootSTM32DFU   .Config     ()                               );
            case "LUFAHID"     : return (String) method.invoke( null, new ProgBootLUFAHID    .Config     ()                               );
            case "AVRDFU"      : return (String) method.invoke( null, new ProgBootAVRDFU     .Config     ()                               );
            //*/
            case "OPENBLT"     : return (String) method.invoke( null, new ProgBootOpenBLT    .Config     ()                               );
            case "SAMBA"       : return (String) method.invoke( null, new ProgBootSAMBA      .Config     ()                               );
            /* ##### !!! TODO !!! #####
            case "USBASP"      : return (String) method.invoke( null, new ProgBootUSBasp     .Config     ()                               );
            //*/

            default            : return (String) method.invoke( null, Class.forName(configName).getDeclaredConstructor().newInstance()    );

            /*
            default:
                // Invalid configuration name
                throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecInvlProgConfigN, FuncName, configName_);
            */

        } // switch
    }

    public static String getProgConfigBasicJSONStr(final String configName_) throws Exception
    { return _getProgConfigBasicXXXStr("JSON", JSONEncoderLite.class, configName_); }

    public static String getProgConfigBasicINIStr(final String configName_) throws Exception
    { return _getProgConfigBasicXXXStr("INI" , INIEncoderLite.class , configName_); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ProgExec(final String[] backendSpec, final String[] programmerSpec, final String mcuSignatureBytes) throws Exception
    {
        final String FuncName = "ProgExec";

        // Process the backend specification
        if( backendSpec != null && backendSpec.length > 0 && !"".equals(backendSpec[0]) )  {
            switch( XCom.re_replaceMultipleWhitespacesAndDash(backendSpec[0], "_").toUpperCase() ) {

                case "USB_ISS":
                    _usb2gpio = new USB_ISS(backendSpec[1]);
                    break;

                case "DASA"       : /* FALLTHROUGH */
                case "JXMAKE_DASA": /* FALLTHROUGH */
                    _usb2gpio = new DASA(backendSpec[1]);
                    break;

                case "USB_GPIO"       : /* FALLTHROUGH */
                case "JXMAKE_USB_GPIO": /* FALLTHROUGH */
                    if( backendSpec[1].equals("") && !backendSpec[2].equals("") ) {
                        _usb2gpio = USB_GPIO.autoConnectNth( Integer.valueOf(backendSpec[2]) );
                    }
                    else {
                        _usb2gpio = new USB_GPIO(backendSpec[1], backendSpec[2]);
                    }
                    ( (USB_GPIO) _usb2gpio ).setAutoNotifyErrorMessage( Boolean.valueOf(backendSpec[3]) );
                  //( (USB_GPIO) _usb2gpio ).enableDebugMessage(true, true, true, true);
                    break;

                default:
                    throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecInvlBackend, FuncName, backendSpec[0]);

            } // switch
        } // if

        // Process the programmer specification
        switch( XCom.re_replaceMultipleWhitespacesAndDash(programmerSpec[0], "").toUpperCase() ) {

            case "PROGISP":
                    _isp               = new ProgISP         ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgISP         .Config.class)                );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGTPI":
                    _tpi               = new ProgTPI         ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgTPI         .Config.class)                );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGUPDI":
                    _updi              = new ProgUPDI        ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgUPDI        .Config.class)                );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGPDI":
                    _pdi               = new ProgPDI         ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgPDI         .Config.class)                );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGLGT8":
                    _lgt8              = new ProgLGT8        ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgLGT8        .Config.class)                );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]); // ---
                break;

            case "PROGSWIM":
                    _swim              = new ProgSWIM        ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgSWIM        .Config.class)                );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]); // ---
                break;

            case "PROGSWD":
                    _swd               = new ProgSWD         ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgSWD         .Config.class)                );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                if(programmerSpec.length > 4) {
                    _idxDefMultidropID = Integer.valueOf(programmerSpec[4]) - 1;
                    _multidropIDs      = new long[programmerSpec.length - 5];
                    for(int i = 0; i < _multidropIDs.length; ++i) _multidropIDs[i] = Long.decode(programmerSpec[i + 5]);
                }
                break;

            case "PROGPIC10": /* FALLTHROUGH */
            case "PROGPIC12": /* FALLTHROUGH */
            case "PROGPIC16":
                    _pic               = new ProgPIC16       ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgPIC16       .Config.class)                    );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGPIC18":
                    _pic               = new ProgPIC18       ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgPIC18       .Config.class)                    );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGPIC24":
                    _pic               = new ProgPIC24       ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgPIC24       .Config.class)                    );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGDSPIC30":
                    _pic               = new ProgDSPIC30        ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgDSPIC30     .Config.class)                    );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGDSPIC33":
                    _pic               = new ProgDSPIC33        ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgDSPIC33     .Config.class)                    );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            /*
            case "PROGPIC32M":
                    _pic               = new ProgPIC32M         ( _usb2gpio, SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgPIC32M      .Config.class)                    );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;
            */

            case "PROGJTAG":
                    _jtag              = new ProgJTAG           ( _usb2gpio );
                    _device            =                 programmerSpec[2] ; // NOTE : This parameter is not actually used by this programmer
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGBOOTAVR109":
                    _avr109            = new ProgBootAVR109     (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootAVR109     .Config.class)                    );
                    _device            =                 programmerSpec[2] ;
                    _magicBaudrate     = Integer.valueOf(programmerSpec[3]);
                    if(_magicBaudrate < 0) _magicBaudrate = ProgBootAVR109.DefMagicBaudrate;
                break;

            case "PROGBOOTSTK500":
                    _stk500            = new ProgBootSTK500     (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootSTK500     .Config.class)                    );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGBOOTSTK500V2":
                    _stk500v2          = new ProgBootSTK500v2   (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootSTK500v2   .Config.class)                    );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGBOOTCHIP45B2":
                    _rs485Address      = Integer.decode (programmerSpec[4]);
                    _chip45            = new ProgBootChip45B2   (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootChip45B2   .Config.class), _rs485Address > 0 );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGBOOTCHIP45B3":
                    _rs485Address      = Integer.decode (programmerSpec[4]);
                    _chip45            = new ProgBootChip45B3   (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootChip45B3   .Config.class), _rs485Address     );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                if(programmerSpec.length > 5) {
                    _u32Key            = new long[4];
                    for(int i = 0; i < 4; ++i) _u32Key[i] = Long.decode(programmerSpec[i + 5]);
                }
                break;

            case "PROGBOOTTSB":
                    _tsb               = new ProgBootTSB        (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootTSB        .Config.class)                    );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                if(programmerSpec.length > 4) {
                    _curPassword       =                 programmerSpec[4];
                }
                if(programmerSpec.length > 5) {
                    _newPassword       =                 programmerSpec[5];
                }
                break;

            case "PROGBOOTURCLOCK":
                    _urclock           = new ProgBootURCLOCK    (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootURCLOCK    .Config.class)                    );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                break;

            case "PROGBOOTSTM32SERIAL":
                    _stm32Ser          = new ProgBootSTM32Serial(            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootSTM32Serial.Config.class)                    );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                    _extraAddress      = Long   .decode (programmerSpec[4]);
                break;

            /* ##### !!! TODO !!! #####
            case "PROGBOOTSTM32DFU":
                    _stm32DFU          = new ProgBootSTM32DFU   (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootSTM32DFU   .Config.class)                    );
                if(programmerSpec.length > 2) {
                    _vid               = Integer.decode (programmerSpec[2]);
                }
                if(programmerSpec.length > 3) {
                    _pid               = Integer.decode (programmerSpec[3]);
                }
                if(programmerSpec.length > 4) {
                    _serialNumber      =                 programmerSpec[4] ;
                }
                break;

            case "PROGBOOTLUFAHID":
                    _lufaHID           = new ProgBootLUFAHID    (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootLUFAHID    .Config.class)                    );
                if(programmerSpec.length > 2) {
                    _vid               = Integer.decode (programmerSpec[2]);
                }
                if(programmerSpec.length > 3) {
                    _pid               = Integer.decode (programmerSpec[3]);
                }
                if(programmerSpec.length > 4) {
                    _serialNumber      =                 programmerSpec[4] ;
                }
                break;
            //*/

            case "PROGBOOTLUFAPRINTER":
                    _lufaPRN           = new ProgBootLUFAPrinter(                                                                                                                       );
                    _device            = programmerSpec[1] ;
                break;

            /* ##### !!! TODO !!! #####
            case "PROGBOOTAVRDFU":
                    _avrDFU            = new ProgBootAVRDFU     (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootAVRDFU     .Config.class)                    );
                if(programmerSpec.length > 2) {
                    _vid               = Integer.decode (programmerSpec[2]);
                }
                if(programmerSpec.length > 3) {
                    _pid               = Integer.decode (programmerSpec[3]);
                }
                if(programmerSpec.length > 4) {
                    _serialNumber      =                 programmerSpec[4] ;
                }
                break;
            //*/

            case "PROGBOOTOPENBLT":
                    _oblt              = new ProgBootOpenBLT    (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootOpenBLT    .Config.class)                    );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                    _extraAddress      = Long   .decode (programmerSpec[4]);
                    _extraSize1        = Integer.decode (programmerSpec[5]);
                    _extraSize2        = Integer.decode (programmerSpec[6]);
            if(programmerSpec.length > 7) {
                if( programmerSpec[7].startsWith(XCom.CallbackFunc_Prefix) ) {
                    _userCBName        = programmerSpec[7].substring( XCom.CallbackFunc_Prefix.length() );
                }
                else {
                    _u08Key            = new int[programmerSpec.length - 7];
                    for(int i = 0; i < _u08Key.length; ++i) _u08Key[i] = Integer.decode(programmerSpec[i + 7]);
                }
            }
                break;

            case "PROGBOOTSAMBA":
                    _samba             = new ProgBootSAMBA      (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootSAMBA      .Config.class)                    );
                    _device            =                 programmerSpec[2] ;
                    _speed             = Integer.valueOf(programmerSpec[3]);
                    _magicBaudrate     = Integer.valueOf(programmerSpec[4]);
                    if(_magicBaudrate < 0) _magicBaudrate = ProgBootSAMBA.DefMagicBaudrate;
                break;

            /* ##### !!! TODO !!! #####
            case "PROGBOOTUSBASP":
                    _usbasp            = new ProgBootUSBasp     (            SerializableDeepClone.fromPSpecStr(programmerSpec[1], ProgBootUSBasp     .Config.class)                    );
                if(programmerSpec.length > 2) {
                    _vid               = Integer.decode (programmerSpec[2]);
                }
                if(programmerSpec.length > 3) {
                    _pid               = Integer.decode (programmerSpec[3]);
                }
                if(programmerSpec.length > 4) {
                    _extraName1        =                 programmerSpec[4] ;
                }
                if(programmerSpec.length > 5) {
                    _extraName2        =                 programmerSpec[5] ;
                }
                if(programmerSpec.length > 6) {
                    _serialNumber      =                 programmerSpec[6] ;
                }
                break;
            //*/

            default:
                throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecInvlProgrammer, FuncName, programmerSpec[0]);

        } // switch

        // Process the signature bytes
        final ArrayList<String> sba = XCom.explode(mcuSignatureBytes, ",");

        _mcuSig = new int[ sba.size() ];

        for(int i = 0; i < _mcuSig.length; ++i) {
            _mcuSig[i] = _intDecode( sba.get(i).trim() );
        }
    }

    public void shutdown()
    { if(_usb2gpio != null) _usb2gpio.shutdown(); }

    public void resetAndShutdown()
    { if(_usb2gpio != null) _usb2gpio.resetAndShutdown(); }

    public void setProgressOutputStream(final PrintStream printStream)
    {
        _printStream = printStream;
        _progressCB  = (_printStream != null) ? ProgressCB.getStdProgressPrinter(_printStream) : null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int flashMemoryTotalSize() throws Exception
    {
             if(_isp      != null) return _isp     ._flashMemoryTotalSize();
        else if(_tpi      != null) return _tpi     ._flashMemoryTotalSize();
        else if(_updi     != null) return _updi    ._flashMemoryTotalSize();
        else if(_pdi      != null) return _pdi     ._flashMemoryTotalSize();
        else if(_lgt8     != null) return _lgt8    ._flashMemoryTotalSize();
        else if(_swim     != null) return _swim    ._flashMemoryTotalSize();
        else if(_swd      != null) return _swd     ._flashMemoryTotalSize();
        else if(_pic      != null) return _pic     ._flashMemoryTotalSize();
        else if(_avr109   != null) return _avr109  ._flashMemoryTotalSize();
        else if(_stk500   != null) return _stk500  ._flashMemoryTotalSize();
        else if(_stk500v2 != null) return _stk500v2._flashMemoryTotalSize();
        else if(_chip45   != null) return _chip45  ._flashMemoryTotalSize();
        else if(_tsb      != null) return _tsb     ._flashMemoryTotalSize();
        else if(_urclock  != null) return _urclock ._flashMemoryTotalSize();
        else if(_stm32Ser != null) return _stm32Ser._flashMemoryTotalSize();
        /* ##### !!! TODO !!! #####
        else if(_stm32DFU != null) return _stm32DFU._flashMemoryTotalSize();
        else if(_lufaHID  != null) return _lufaHID ._flashMemoryTotalSize();
        //*/
        else if(_lufaPRN  != null) return _lufaPRN ._flashMemoryTotalSize();
        /* ##### !!! TODO !!! #####
        else if(_avrDFU   != null) return _avrDFU  ._flashMemoryTotalSize();
        //*/
        else if(_oblt     != null) return _oblt    ._flashMemoryTotalSize();
        else if(_samba    != null) return _samba   ._flashMemoryTotalSize();
        /* ##### !!! TODO !!! #####
        else if(_usbasp   != null) return _usbasp  ._flashMemoryTotalSize();
        //*/
        else                       throw XCom.newJXMFatalLogicError(Texts.EMsg_ProgExecError, "flashMemoryTotalSize", "_flashMemoryTotalSize???");
    }

    public byte flashMemoryEmptyValue() throws Exception
    {
             if(_isp      != null) return _isp     ._flashMemoryEmptyValue();
        else if(_tpi      != null) return _tpi     ._flashMemoryEmptyValue();
        else if(_updi     != null) return _updi    ._flashMemoryEmptyValue();
        else if(_pdi      != null) return _pdi     ._flashMemoryEmptyValue();
        else if(_lgt8     != null) return _lgt8    ._flashMemoryEmptyValue();
        else if(_swim     != null) return _swim    ._flashMemoryEmptyValue();
        else if(_swd      != null) return _swd     ._flashMemoryEmptyValue();
        else if(_pic      != null) return _pic     ._flashMemoryEmptyValue();
        else if(_avr109   != null) return _avr109  ._flashMemoryEmptyValue();
        else if(_stk500   != null) return _stk500  ._flashMemoryEmptyValue();
        else if(_stk500v2 != null) return _stk500v2._flashMemoryEmptyValue();
        else if(_chip45   != null) return _chip45  ._flashMemoryEmptyValue();
        else if(_tsb      != null) return _tsb     ._flashMemoryEmptyValue();
        else if(_urclock  != null) return _urclock ._flashMemoryEmptyValue();
        else if(_stm32Ser != null) return _stm32Ser._flashMemoryEmptyValue();
        /* ##### !!! TODO !!! #####
        else if(_stm32DFU != null) return _stm32DFU._flashMemoryEmptyValue();
        else if(_lufaHID  != null) return _lufaHID ._flashMemoryEmptyValue();
        //*/
        else if(_lufaPRN  != null) return _lufaPRN ._flashMemoryEmptyValue();
        /* ##### !!! TODO !!! #####
        else if(_avrDFU   != null) return _avrDFU  ._flashMemoryEmptyValue();
        //*/
        else if(_oblt     != null) return _oblt    ._flashMemoryEmptyValue();
        else if(_samba    != null) return _samba   ._flashMemoryEmptyValue();
        /* ##### !!! TODO !!! #####
        else if(_usbasp   != null) return _usbasp  ._flashMemoryEmptyValue();
        //*/
        else                       throw XCom.newJXMFatalLogicError(Texts.EMsg_ProgExecError, "flashMemoryEmptyValue", "_flashMemoryEmptyValue???");
    }

    public int eepromMemoryTotalSize() throws Exception
    {
             if(_isp      != null) return _isp     ._eepromMemoryTotalSize();
        else if(_tpi      != null) return _tpi     ._eepromMemoryTotalSize();
        else if(_updi     != null) return _updi    ._eepromMemoryTotalSize();
        else if(_pdi      != null) return _pdi     ._eepromMemoryTotalSize();
        else if(_lgt8     != null) return _lgt8    ._eepromMemoryTotalSize();
        else if(_swim     != null) return _swim    ._eepromMemoryTotalSize();
        else if(_swd      != null) return _swd     ._eepromMemoryTotalSize();
        else if(_pic      != null) return _pic     ._eepromMemoryTotalSize();
        else if(_avr109   != null) return _avr109  ._eepromMemoryTotalSize();
        else if(_stk500   != null) return _stk500  ._eepromMemoryTotalSize();
        else if(_stk500v2 != null) return _stk500v2._eepromMemoryTotalSize();
        else if(_chip45   != null) return _chip45  ._eepromMemoryTotalSize();
        else if(_tsb      != null) return _tsb     ._eepromMemoryTotalSize();
        else if(_urclock  != null) return _urclock ._eepromMemoryTotalSize();
        else if(_stm32Ser != null) return _stm32Ser._eepromMemoryTotalSize();
        /* ##### !!! TODO !!! #####
        else if(_stm32DFU != null) return _stm32DFU._eepromMemoryTotalSize();
        else if(_lufaHID  != null) return _lufaHID ._eepromMemoryTotalSize();
        //*/
        else if(_lufaPRN  != null) return _lufaPRN ._eepromMemoryTotalSize();
        /* ##### !!! TODO !!! #####
        else if(_avrDFU   != null) return _avrDFU  ._eepromMemoryTotalSize();
        //*/
        else if(_oblt     != null) return _oblt    ._eepromMemoryTotalSize();
        else if(_samba    != null) return _samba   ._eepromMemoryTotalSize();
        /* ##### !!! TODO !!! #####
        else if(_usbasp   != null) return _usbasp  ._eepromMemoryTotalSize();
        //*/
        else                       throw XCom.newJXMFatalLogicError(Texts.EMsg_ProgExecError, "eepromMemoryTotalSize", "_eepromMemoryTotalSize???");
    }

    public byte eepromMemoryEmptyValue() throws Exception
    {
             if(_isp      != null) return _isp     ._eepromMemoryEmptyValue();
        else if(_tpi      != null) return _tpi     ._eepromMemoryEmptyValue();
        else if(_updi     != null) return _updi    ._eepromMemoryEmptyValue();
        else if(_pdi      != null) return _pdi     ._eepromMemoryEmptyValue();
        else if(_lgt8     != null) return _lgt8    ._eepromMemoryEmptyValue();
        else if(_swim     != null) return _swim    ._eepromMemoryEmptyValue();
        else if(_swd      != null) return _swd     ._eepromMemoryEmptyValue();
        else if(_pic      != null) return _pic     ._eepromMemoryEmptyValue();
        else if(_avr109   != null) return _avr109  ._eepromMemoryEmptyValue();
        else if(_stk500   != null) return _stk500  ._eepromMemoryEmptyValue();
        else if(_stk500   != null) return _stk500  ._eepromMemoryEmptyValue();
        else if(_chip45   != null) return _chip45  ._eepromMemoryEmptyValue();
        else if(_tsb      != null) return _tsb     ._eepromMemoryEmptyValue();
        else if(_urclock  != null) return _urclock ._eepromMemoryEmptyValue();
        else if(_stm32Ser != null) return _stm32Ser._eepromMemoryEmptyValue();
        /* ##### !!! TODO !!! #####
        else if(_stm32DFU != null) return _stm32DFU._eepromMemoryEmptyValue();
        else if(_lufaHID  != null) return _lufaHID ._eepromMemoryEmptyValue();
        //*/
        else if(_lufaPRN  != null) return _lufaPRN ._eepromMemoryEmptyValue();
        /* ##### !!! TODO !!! #####
        else if(_avrDFU   != null) return _avrDFU  ._eepromMemoryEmptyValue();
        //*/
        else if(_oblt     != null) return _oblt    ._eepromMemoryEmptyValue();
        else if(_samba    != null) return _samba   ._eepromMemoryEmptyValue();
        /* ##### !!! TODO !!! #####
        else if(_usbasp   != null) return _usbasp  ._eepromMemoryEmptyValue();
        //*/
        else                       throw XCom.newJXMFatalLogicError(Texts.EMsg_ProgExecError, "eepromMemoryEmptyValue", "_eepromMemoryEmptyValue???");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public long[] execute(final String[] commands, final String fwcHandle, final FWComposer fwc, final int flashMemoryEmptyValue_, final boolean continue_) throws Exception
    {
        // ProgJTAG is special
        if(_jtag != null) {
            _execProgJTAG(commands);
            return _resultBuffer.stream().mapToLong(i -> i).toArray();
        }

        // Get the empty value
        final byte fmEmptyValue = (flashMemoryEmptyValue_ < 0) ? flashMemoryEmptyValue() : ( (byte) flashMemoryEmptyValue_ );

        // Get the special firmware as needed
        if(fwc == null && fwcHandle != null) {
            // Extract the class name and function name
            final ArrayList<String> cnamePart = XCom.explode( fwcHandle, ".");
            final           String  fname     = cnamePart.get( cnamePart.size() - 1 );
            cnamePart.remove( cnamePart.size() - 1 );
            // Generate the full class name
            String className = XCom.flatten(cnamePart, ".");
            if( cnamePart.size() == 1 ) className = "jxm.ugc." + className;
            // Get the class
            final Class<?> clazz = Class.forName(className);
            // Invoke the function to get the firmware
            _fwDataBuff     = (byte[]) clazz.getMethod(fname).invoke(null);
            _fwLength       =          _fwDataBuff.length;
            _fwStartAddress = (int   ) clazz.getMethod(fname + "_startAddress").invoke(null);
            /*
            SysUtil.stdDbg().printf("##### %s : %d bytes at 0x%08X\n", fwcHandle, _fwLength, _fwStartAddress);
            //*/
        }

        // Process the firmware
        if(fwc != null) {

            if(_pic != null) {

                final ProgPIC.FWD fwd = _pic.fwDecompose(fwc);

                _fwDataBuff     = fwd.fwDataBuff;
                _fwLength       = fwd.fwLength;
                _fwStartAddress = fwd.fwStartAddress;

                _cfDataBuff     = fwd.cfDataBuff;
                _cfLength       = fwd.cfLength;
                _cfStartAddress = fwd.cfStartAddress;

                _epDataBuff     = fwd.epDataBuff;
                _epLength       = fwd.epLength;
                _epStartAddress = fwd.epStartAddress;

            }
            else {

                _fwDataBuff     =       fwc.getFlattenedBinaryData(fmEmptyValue);
                _fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
                _fwLength       =       _fwDataBuff.length;

            } // if

        } // if

        // ##### !!! TODO : Do not flatten but issue multiple write? !!! #####

        // Clear the result buffer
        _resultBuffer.clear();

        // Determine the execution mode
        if(!continue_) {
                 if(_execMode == ExecMode.MultipleStart) _execMode = ExecMode.MultipleDone;
            else if(_execMode == ExecMode.MultipleNext ) _execMode = ExecMode.MultipleDone;
            else                                         _execMode = ExecMode.Single;
        }
        else {
            if(_execMode != ExecMode.MultipleNext && _execMode != ExecMode.MultipleStart) _execMode = ExecMode.MultipleStart;
            else                                                                          _execMode = ExecMode.MultipleNext;
        }

        // Process according to the selected programmer
             if(_isp      != null) _execProgISP         (commands);
        else if(_tpi      != null) _execProgTPI         (commands);
        else if(_updi     != null) _execProgUPDI        (commands);
        else if(_pdi      != null) _execProgPDI         (commands);
        else if(_lgt8     != null) _execProgLGT8        (commands);
        else if(_swim     != null) _execProgSWIM        (commands);
        else if(_swd      != null) _execProgSWD         (commands);
        else if(_pic      != null) _execProgPIC         (commands);
        else if(_avr109   != null) _execProgBootAVR109  (commands);
        else if(_stk500   != null) _execProgBootSTK500  (commands);
        else if(_stk500v2 != null) _execProgBootSTK500v2(commands);
        else if(_chip45   != null) _execProgBootChip45  (commands);
        else if(_tsb      != null) _execProgBootTSB     (commands);
        else if(_urclock  != null) _execProgBootURCLOCK (commands);
        else if(_stm32Ser != null) _execProgBootSTM32Ser(commands);
        /* ##### !!! TODO !!! #####
        else if(_stm32DFU != null) _execProgBootSTM32Ser(commands);
        else if(_lufaHID  != null) _execProgBootLUFAHID (commands);
        //*/
        else if(_lufaPRN  != null) _execProgBootLUFAPRN (commands);
        /* ##### !!! TODO !!! #####
        else if(_avrDFU   != null) _execProgBootAVRDFU  (commands);
        //*/
        else if(_oblt     != null) _execProgBootOpenBLT (commands);
        else if(_samba    != null) _execProgBootSAMBA   (commands);
        /* ##### !!! TODO !!! #####
        else if(_usbasp   != null) _execProgBootUSBasp  (commands);
        //*/
        else                       throw XCom.newJXMFatalLogicError(Texts.EMsg_ProgExecError, "execute", "_execProg???");

        // Done
        return _resultBuffer.stream().mapToLong(i -> i).toArray();
    }

    public long[] execute(final String[] commands, final String fwcHandle, final FWComposer fwc, final int flashMemoryEmptyValue) throws Exception
    { return execute(commands, fwcHandle, fwc, flashMemoryEmptyValue, false    ); }

    public long[] execute(final String[] commands, final FWComposer fwc, final int flashMemoryEmptyValue, final boolean continue_) throws Exception
    { return execute(commands, null     , fwc, flashMemoryEmptyValue, continue_); }

    public long[] execute(final String[] commands, final FWComposer fwc, final int flashMemoryEmptyValue) throws Exception
    { return execute(commands, null     , fwc, flashMemoryEmptyValue, false    ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _println()
    { if(_printStream != null) _printStream.println(); }

    private void _println(final String str)
    { if(_printStream != null) _printStream.println(str); }

    private void _printf(final String format, final Object... args)
    { if(_printStream != null) _printStream.printf(format, args); }

    private void _printfln(final String format, final Object... args)
    { if(_printStream != null) _printStream.printf(format + '\n', args); }

    private void _println_progressInfoString(final int totalBytes)
    { _println( ProgressCB.getStdProgressInfoString(totalBytes) ); }

    private void _println_progressInfoStringPE(final int totalBytes)
    { _println( ProgressCB.getStdProgressInfoStringPE(totalBytes) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long _tv1 = 0;
    private long _tv2 = 0;

    private void _saveBegTime()
    { _tv1 = SysUtil.getNS(); }

    private void _saveEndTime()
    { _tv2 = SysUtil.getNS(); }

    private void _println_elapsedTime(final int totalBytes)
    {
        final double etime = (_tv2 - _tv1) * 0.000000001;
        final double bps   = _fwLength / etime;

        _printfln(Texts.EMsg_ProgExecMsgTimeSpeed, totalBytes, etime, bps);
        _println();
    }

    private void _println_elapsedTime()
    { _println_elapsedTime(_fwLength); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _exec_generic_wrapper(final String callerFuncName, final String calledFuncName, final String msgText, final GenericExecFunc genExecFunc, final int totalBytesForProgressCB) throws Exception
    {
        _println(msgText);
        if(totalBytesForProgressCB > 0) _println_progressInfoString(totalBytesForProgressCB);

        _saveBegTime();
        final boolean res = genExecFunc.handle();
        _saveEndTime();

        if(totalBytesForProgressCB > 0) _println_elapsedTime(totalBytesForProgressCB);

        if(!res) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, calledFuncName);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _exec_readAndVerifySignature(final String callerFuncName, final IProgCommon prog) throws Exception
    {
        if( prog.supportSignature() ) {

            if( !prog.readSignature() ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "readSignature");

            if( !prog.verifySignature(_mcuSig) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "verifySignature");

        }

        return 1 + 0; // This command takes no parameter
    }

    private int _exec_chipErase(final String callerFuncName, final IProgCommon prog) throws Exception
    {
        _println(Texts.EMsg_ProgExecMsgChipErase);

        _saveBegTime();
        final boolean res = prog.chipErase();
        _saveEndTime();

        _println_elapsedTime( prog._flashMemoryTotalSize() );

        if(!res) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "chipErase");

        return 1 + 0; // This command takes no parameter
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _exec_readFlash(final String callerFuncName, final IProgCommon prog, final String[] commands, final int curCmdIdx) throws Exception
    {
        final int startAddress = _intDecode(commands, curCmdIdx + 1);
        final int numBytes     = _intDecode(commands, curCmdIdx + 2);

        _println(Texts.EMsg_ProgExecMsgReadFlash);
        _println_progressInfoString(numBytes);

        _saveBegTime();
        final boolean res = prog.readFlash(startAddress, numBytes, _progressCB);
        _saveEndTime();

        _println_elapsedTime(numBytes);

        if(!res) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "readFlash");

        for( final int v : prog._readDataBuff() ) _resultBuffer.addU08(v);

        return 1 + 2; // This command takes two parameters
    }

    private int _exec_writeFlash(final String callerFuncName, final IProgCommon prog) throws Exception
    {
        _println(Texts.EMsg_ProgExecMsgWriteFlash);
        _println_progressInfoString( prog._flashMemoryAlignWriteSize(_fwLength) );

        _saveBegTime();
        final boolean res = prog.writeFlash(_fwDataBuff, _fwStartAddress, _fwLength, _progressCB);
        _saveEndTime();

        _println_elapsedTime();

        if(!res) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "writeFlash");

        return 1 + 0; // This command takes no parameter
    }

    private int _exec_verifyFlash(final String callerFuncName, final IProgCommon prog) throws Exception
    {
        _println(Texts.EMsg_ProgExecMsgVerifyFlash);
        _println_progressInfoString(_fwLength);

        _saveBegTime();
        final int verBPos = prog.verifyFlash(_fwDataBuff, _fwStartAddress, _fwLength, _progressCB);
        _saveEndTime();

        _println_elapsedTime();

        if(verBPos < 0) throw XCom.newJXMRuntimeError(
                            Texts.EMsg_ProgExecError,
                            callerFuncName, "verifyFlash"
                        );

        if(verBPos < _fwLength) throw XCom.newJXMRuntimeError(
                                    Texts.EMsg_ProgExecError + Texts.EMsg_ProgExecError_ByteNEQ,
                                    callerFuncName, "verifyFlash",
                                    _fwStartAddress + verBPos, _fwDataBuff[verBPos], prog._readDataBuff()[verBPos]
                                );

        return 1 + 0; // This command takes no parameter
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _exec_readEEPROM(final String callerFuncName, final IProgCommon prog, final String[] commands, final int curCmdIdx) throws Exception
    {
        _println(Texts.EMsg_ProgExecMsgReadEEPROM);

        final int res = prog.readEEPROM( _intDecode(commands, curCmdIdx + 1) );

        if(res < 0) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "readEEPROM");

        _resultBuffer.addU08(res);

        return 1 + 1; // This command takes one parameter
    }

    private int _exec_writeEEPROM(final String callerFuncName, final IProgCommon prog, final String[] commands, final int curCmdIdx) throws Exception
    {
        _println(Texts.EMsg_ProgExecMsgWriteEEPROM);

        final boolean res = prog.writeEEPROM( _intDecode(commands, curCmdIdx + 1), _byteDecode(commands, curCmdIdx + 2) );

        if(!res) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "writeEEPROM");

        return 1 + 2; // This command takes two parameters
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _exec_readLockBits(final String callerFuncName, final IProgCommon prog, final int curCmdIdx) throws Exception
    {
        _println(Texts.EMsg_ProgExecMsgReadLockBit);

        final long res = prog.readLockBits();

        if(res < 0) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "readLockBits");

        _resultBuffer.addU32(res);

        return 1 + 0; // This command takes no parameter
    }

    private int _exec_writeLockBits(final String callerFuncName, final IProgCommon prog, final String[] commands, final int curCmdIdx) throws Exception
    {
        _println(Texts.EMsg_ProgExecMsgWriteLockBit);

        final boolean res = prog.writeLockBits( _longDecode(commands, curCmdIdx + 1) );

        if(!res) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "writeLockBits");

        return 1 + 1; // This command takes one parameter
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _execProgCommon_and_uninitialize(final String callerFuncName, final String[] commands, final IProgCommon prog, final SpecificCommandHandler specificCommandHandler) throws Exception
    {
        try {

            // Read and verify signature
            _exec_readAndVerifySignature(callerFuncName, prog);

            // Simply exit if there is no command to execute
            if(commands == null) return;

            // Execute commands
            int i = 0;
            while(i < commands.length) {

                // Get the command
                final String cmd = commands[i];

                // Process the command
                switch(cmd) {

                    // ##### ??? TODO : Add a blacklist mechanism so that it is forced to use 'specificCommandHandler' ??? #####

                    case CMD_NoOperation   : i += 1                                                     ; break;

                    case CMD_ChipErase     : i += _exec_chipErase    (callerFuncName, prog             ); break;

                    case CMD_ReadFlash     : i += _exec_readFlash    (callerFuncName, prog, commands, i); break;
                    case CMD_WriteFlash    : i += _exec_writeFlash   (callerFuncName, prog             ); break;
                    case CMD_VerifyFlash   : i += _exec_verifyFlash  (callerFuncName, prog             ); break;

                    case CMD_ReadEEPROM    : i += _exec_readEEPROM   (callerFuncName, prog, commands, i); break;
                    case CMD_WriteEEPROM   : i += _exec_writeEEPROM  (callerFuncName, prog, commands, i); break;

                    case CMD_ReadLockBits  : i += _exec_readLockBits (callerFuncName, prog,           i); break;
                    case CMD_WriteLockBits : i += _exec_writeLockBits(callerFuncName, prog, commands, i); break;

                    // ##### ??? TODO : Add more commands ??? #####

                    default:
                        // Check if there are specific command handler that can handle the command
                        if(specificCommandHandler != null) {
                            final int res = specificCommandHandler.handle(cmd, i);
                            if(res >= 1) {
                                i += res;
                                continue;
                            }
                        }
                        // Invalid command
                        throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecInvldCommand, callerFuncName, cmd);

                } // switch

            } // for

            // Uninitialize as needed
            if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleDone) {
                if( !prog.end() ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, callerFuncName, "callerFuncName");
            }

        } // try
        catch(final Exception e) {
            // Uninitialize
            prog.end();
            _execMode = ExecMode.MultipleDone;
            // Exit error
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _execProgISP(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgISP";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_isp.begin(_speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _isp, new SpecificCommandHandler() {
            @Override
            public int handle(final String curCmdStr, final int curCmdIdx) throws Exception
            {
                switch(curCmdStr) {

                    case CMD_ReadFuses:
                        _exec_generic_wrapper(FuncName, "readFuses", Texts.EMsg_ProgExecMsgReadFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int resL = _isp.readLFuse(); if(resL < 0) return false;
                                final int resH = _isp.readHFuse(); if(resH < 0) return false;
                                final int resE = _isp.readEFuse(); if(resE < 0) return false;
                                _resultBuffer.addU08(resL);
                                _resultBuffer.addU08(resH);
                                _resultBuffer.addU08(resE);
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteFuses:
                        _exec_generic_wrapper(FuncName, "writeFuses", Texts.EMsg_ProgExecMsgWriteFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int valL = _intDecode(commands, curCmdIdx + 1);
                                final int valH = _intDecode(commands, curCmdIdx + 2);
                                final int valE = _intDecode(commands, curCmdIdx + 3);
                                if(valL >= 0) { if( !_isp.writeLFuse( (byte) valL ) ) return false; }
                                if(valH >= 0) { if( !_isp.writeHFuse( (byte) valH ) ) return false; }
                                if(valE >= 0) { if( !_isp.writeEFuse( (byte) valE ) ) return false; }
                                return true;
                            }
                        }, 0 );
                        return 1 + 3; // This command takes three parameters

                    // ##### ??? TODO : Handle more command(s) specific to ISP ??? #####

                } // switch

                // Invalid command
                return -1;
            }
        } );
    }

    private void _execProgTPI(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgTPI";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_tpi.begin(_speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _tpi, new SpecificCommandHandler() {
            @Override
            public int handle(final String curCmdStr, final int curCmdIdx) throws Exception
            {
                switch(curCmdStr) {

                    case CMD_ReadFuses:
                        _exec_generic_wrapper(FuncName, "readFuses", Texts.EMsg_ProgExecMsgReadFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int res = _tpi.readFuse();
                                if(res < 0) return false;
                                _resultBuffer.addU08(res);
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteFuses:
                        _exec_generic_wrapper(FuncName, "writeFuses", Texts.EMsg_ProgExecMsgWriteFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int val = _intDecode(commands, curCmdIdx + 1);
                                if(val >= 0) { if( !_tpi.writeFuse( (byte) val ) ) return false; }
                                return true;
                            }
                        }, 0 );
                        return 1 + 1; // This command takes one parameter

                    // ##### ??? TODO : Handle more command(s) specific to TPI ??? #####

                } // switch

                // Invalid command
                return -1;
            }
        } );
    }

    private void _execProgUPDI(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgUPDI";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_updi.begin(_speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _updi, new SpecificCommandHandler() {
            @Override
            public int handle(final String curCmdStr, final int curCmdIdx) throws Exception
            {
                switch(curCmdStr) {

                    case CMD_ReadFuses:
                        _exec_generic_wrapper(FuncName, "readFuses", Texts.EMsg_ProgExecMsgReadFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int[] res = _updi.readFuses();
                                if(res == null || res.length != _updi.config().memoryFuse.address.length) return false;
                                for(final int v : res) _resultBuffer.addU08(v);
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteFuses:
                        _exec_generic_wrapper(FuncName, "writeFuses", Texts.EMsg_ProgExecMsgWriteFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int[] vals = new int[_updi.config().memoryFuse.address.length];
                                for(int i = 0; i < vals.length; ++i) vals[i] = _intDecode(commands, curCmdIdx + 1 + i);
                                if( !_updi.writeFuses(vals) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + _updi.config().memoryFuse.address.length; // This command takes multiple parameters

                    // ##### ??? TODO : Handle more command(s) specific to UPDI ??? #####

                } // switch

                // Invalid command
                return -1;
            }
        } );
    }

    private void _execProgPDI(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgPDI";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_pdi.begin(_speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _pdi, new SpecificCommandHandler() {
            @Override
            public int handle(final String curCmdStr, final int curCmdIdx) throws Exception
            {
                switch(curCmdStr) {

                    case CMD_WriteFlashBSec:
                        _exec_generic_wrapper(FuncName, "writeFlash", Texts.EMsg_ProgExecMsgWriteFlash, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            { return _pdi.writeFlash(true , _fwDataBuff, _fwStartAddress, _fwLength, _progressCB); }
                        }, _pdi._flashMemoryAlignWriteSize(_fwLength) );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteFlashASec:
                        _exec_generic_wrapper(FuncName, "writeFlash", Texts.EMsg_ProgExecMsgWriteFlash, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            { return _pdi.writeFlash(false, _fwDataBuff, _fwStartAddress, _fwLength, _progressCB); }
                        }, _pdi._flashMemoryAlignWriteSize(_fwLength) );
                        return 1 + 0; // This command takes no parameter

                    case CMD_ReadFuses:
                        _exec_generic_wrapper(FuncName, "readFuses", Texts.EMsg_ProgExecMsgReadFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int[] res = _pdi.readFuses();
                                if(res == null || res.length != _pdi.config().memoryFuse.address.length) return false;
                                for(final int v : res) _resultBuffer.addU08(v);
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteFuses:
                        _exec_generic_wrapper(FuncName, "writeFuses", Texts.EMsg_ProgExecMsgWriteFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int[] vals = new int[_pdi.config().memoryFuse.address.length];
                                for(int i = 0; i < vals.length; ++i) vals[i] = _intDecode(commands, curCmdIdx + 1 + i);
                                if( !_pdi.writeFuses(vals) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + _pdi.config().memoryFuse.address.length; // This command takes multiple parameters

                    // ##### ??? TODO : Handle more command(s) specific to PDI ??? #####

                } // switch

                // Invalid command
                return -1;
            }
        } );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _execProgLGT8(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgLGT8";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_lgt8.begin() ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _lgt8, null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _execProgSWIM(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgSWIM";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_swim.begin() ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _swim, new SpecificCommandHandler() {
            @Override
            public int handle(final String curCmdStr, final int curCmdIdx) throws Exception
            {
                switch(curCmdStr) {

                    case CMD_ReadFuses:
                        _exec_generic_wrapper(FuncName, "readFuses", Texts.EMsg_ProgExecMsgReadFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int[] res = _swim.readFuses();
                                if(res == null || res.length != _swim.config().memoryOptionBytes.defaultValues.length) return false;
                                for(final int v : res) _resultBuffer.addU08(v);
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteFuses:
                        _exec_generic_wrapper(FuncName, "writeFuses", Texts.EMsg_ProgExecMsgWriteFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int[] vals = new int[_swim.config().memoryOptionBytes.defaultValues.length];
                                for(int i = 0; i < vals.length; ++i) vals[i] = _intDecode(commands, curCmdIdx + 1 + i);
                                if( !_swim.writeFuses(vals) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + _swim.config().memoryOptionBytes.defaultValues.length; // This command takes multiple parameters

                    // ##### ??? TODO : Handle more command(s) specific to SWIM ??? #####

                } // switch

                // Invalid command
                return -1;
            }
        } );
    }

    private void _execProgSWD(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgSWD";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            // ##### ??? TODO : Also export 'allowBitBangingSPI' as a parameter ??? #####
            if( !_swd.begin(_multidropIDs, _idxDefMultidropID, false, _speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _swd, new SpecificCommandHandler() {
            @Override
            public int handle(final String curCmdStr, final int curCmdIdx) throws Exception
            {
                switch(curCmdStr) {

                    case CMD_ReadFuses:
                        _exec_generic_wrapper(FuncName, "readFuses", Texts.EMsg_ProgExecMsgReadFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final long[] res = _swd.readFuses();
                                if( res == null || res.length != _swd.numberOfFuses() ) return false;
                                for(final long v : res) _resultBuffer.addU32(v);
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteFuses:
                        _exec_generic_wrapper(FuncName, "writeFuses", Texts.EMsg_ProgExecMsgWriteFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final long[] vals = new long[ _swd.numberOfFuses() ];
                                for(int i = 0; i < vals.length; ++i) vals[i] = _longDecode(commands, curCmdIdx + 1 + i);
                                if( !_swd.writeFuses(vals) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + _swd.numberOfFuses(); // This command takes multiple parameters

                    // ##### ??? TODO : Handle more command(s) specific to SWD ??? #####

                } // switch

                // Invalid command
                return -1;
            }
        } );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _execProgPIC(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgPIC";

        // Check for PIC16 recover command (execute and exit)
        if( commands[0].equals(CMD_PIC16_Recover) ) {
            // NOTE : For this function to work properly, the target must be started from a cold start state
            if( !( (ProgPIC16) _pic )._pic16_recover( (int) _longDecode(commands[1]) ) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "_pic16_recover");
            return;
        }

        // Check for PIC16 unbrick command (execute and exit)
        if( commands[0].equals(CMD_PIC16_Unbrick) ) {
            if( !( (ProgPIC16) _pic )._pic16_unbrick( (int) _longDecode(commands[1]) ) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "_pic16_unbrick");
            return;
        }

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_pic.begin(_speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _pic, new SpecificCommandHandler() {
            @Override
            public int handle(final String curCmdStr, final int curCmdIdx) throws Exception
            {
                switch(curCmdStr) {

                    case CMD_ReadFuses:
                        _exec_generic_wrapper(FuncName, "readFuses", Texts.EMsg_ProgExecMsgReadFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final long[] res = _pic.readFuses();
                                if(res == null || res.length != _pic.config().memoryConfigBytes.address.length) return false;
                                for(final long v : res) _resultBuffer.addU32(v);
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteFuses:
                        _exec_generic_wrapper(FuncName, "writeFuses", Texts.EMsg_ProgExecMsgWriteFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final long[] vals = new long[_pic.config().memoryConfigBytes.address.length];
                                for(int i = 0; i < vals.length; ++i) vals[i] = _longDecode(commands, curCmdIdx + 1 + i);
                                if( !_pic.writeFuses(vals) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + _pic.config().memoryConfigBytes.address.length; // This command takes multiple parameters

                    case CMD_WriteFusesEmd:
                        _exec_generic_wrapper(FuncName, "writeFuses", Texts.EMsg_ProgExecMsgWriteFuses, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                if( !_pic.writeFuses(_cfDataBuff, _cfStartAddress, _cfLength) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_WriteEEPROMEmd:
                        _exec_generic_wrapper(FuncName, "writeEEPROM", Texts.EMsg_ProgExecMsgWriteEEPROM, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                if( !_pic.writeEEPROM(_epDataBuff, _epStartAddress, _epLength) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                    case CMD_PIC_SetEICS_ES:
                        _exec_generic_wrapper(FuncName, "setEICSPExtraSpeed", Texts.EMsg_ProgExecMsgWriteEEPROM, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                final int pgcFreqExW = _intDecode(commands, curCmdIdx + 1);
                                final int pgcFreqExR = _intDecode(commands, curCmdIdx + 2);
                                if( !_pic.setEICSPExtraSpeed(pgcFreqExW, pgcFreqExR) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + 2; // This command takes two parameters

                    case CMD_PIC_ProgramPE:
                        _exec_generic_wrapper(FuncName, "programPE", Texts.EMsg_ProgExecMsgProgramPE, new GenericExecFunc() {
                            @Override
                            public boolean handle() throws Exception
                            {
                                _println_progressInfoStringPE(_fwDataBuff.length);
                                if( !_pic.programPE(_fwDataBuff, _progressCB) ) return false;
                                return true;
                            }
                        }, 0 );
                        return 1 + 0; // This command takes no parameter

                } // switch

                // Invalid command
                return -1;
            }
        } );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _execProgJTAG(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgJTAG";

        // Initialize
        if( !_jtag.begin(_speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");

        // Load and execute
        final JTAGBitstream jbs = new JTAGBitstream();

        // ##### !!! TODO : VERIFY !!! #####

        for(int i = 0; i < commands.length; i += 2) {

            // ##### ??? TODO : Add more commands ??? #####

            // Get and check the command
            final String cmd  = commands[i + 0];

            if( !CMD_JTAG_PlaySVF.equals(cmd) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecInvldCommand, FuncName, cmd);

            // Load the SVF file
            final String path = commands[i + 1];

            jbs.loadSVF(path);

            // Execute the JTAG bitstream
            final int numBytes = jbs.getEffByteSize();

            _println_progressInfoString(numBytes);

            _saveBegTime();
            final boolean res = _jtag.execute(jbs, _progressCB);
            _saveEndTime();

            _println_elapsedTime(numBytes);

            if(!res) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "execute");

        } // for

        // Uninitialize
        if( !_jtag.end() ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "end");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _execProgBootAVR109(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootAVR109";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_avr109.begin(_device, _magicBaudrate) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _avr109, null);

    }

    private void _execProgBootSTK500(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootSTK500";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_stk500.begin(_device, _speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _stk500, null);
    }

    private void _execProgBootSTK500v2(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootSTK500v2";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_stk500v2.begin(_device, _speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _stk500v2, null);
    }

    private void _execProgBootChip45(final String[] commands) throws Exception
    {
        final String FuncName = (_chip45 instanceof ProgBootChip45B2) ? "_execProgBootChip45B2"
                              : (_chip45 instanceof ProgBootChip45B3) ? "_execProgBootChip45B3"
                              :                                         "_execProgBootChip45B?";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_chip45.begin(_device, _speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            if( (_chip45 instanceof ProgBootChip45B3) && (_u32Key != null) ) {
                ( (ProgBootChip45B3) _chip45 ).enableXTEA(_u32Key);
            }
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _chip45, null);
    }

    private void _execProgBootTSB(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootTSB?";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            _tsb.setPassword(_curPassword, _newPassword);
            if( !_tsb.begin(_device, _speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _tsb, null);
    }

    private void _execProgBootURCLOCK(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootURCLOCK";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_urclock.begin(_device, _speed) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _urclock, null);
    }

    private void _execProgBootSTM32Ser(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootSTM32Ser";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_stm32Ser.begin(_device, _speed, _extraAddress) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _stm32Ser, null);
    }

    /* ##### !!! TODO !!! #####
    private void _execProgBootSTM32DFU(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootSTM32DFU";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if(_vid > 0 && _pid > 0) {
                if( !_stm32DFU.begin(_vid, _pid, _serialNumber) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }
            else {
                if( !_stm32DFU.begin(            _serialNumber) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _stm32DFU, null);
    }

    private void _execProgBootLUFAHID(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootLUFAHID";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if(_vid > 0 && _pid > 0) {
                if( !_lufaHID.begin(_vid, _pid) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }
            else {
                if( !_lufaHID.begin(          ) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _lufaHID, null);
    }
    //*/

    private void _execProgBootLUFAPRN(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootLUFAPRN";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_lufaPRN.begin(_device) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _lufaPRN, null);
    }

    /* ##### !!! TODO !!! #####
    private void _execProgBootAVRDFU(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootAVRDFU";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if(_vid > 0 && _pid > 0) {
                if( !_avrDFU.begin(_vid, _pid, _serialNumber) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }
            else {
                if( !_avrDFU.begin(            _serialNumber) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _avrDFU, null);
    }
    //*/

    private void _execProgBootOpenBLT(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootOpenBLT";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {

            if(_userCBName == null) {
                _oblt.setKey(_u08Key);
                if( !_oblt.begin(_device, _speed, _extraAddress, _extraSize1, _extraSize2) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }

            else {
                if( !_oblt.begin(_device, _speed, (final int requestType, final long[] requestParam) -> {
                    return XBExec.primaryXBExec().executeCallback(
                        _userCBName, new long[] { requestType }, requestParam
                    );
                } ) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }

        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _oblt, null);
    }

    private void _execProgBootSAMBA(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootSAMBA";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if( !_samba.begin(_device, _speed, _magicBaudrate) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _samba, null);
    }

    /* ##### !!! TODO !!! #####
    private void _execProgBootUSBasp(final String[] commands) throws Exception
    {
        final String FuncName = "_execProgBootUSBasp";

        // Initialize as needed
        if(_execMode == ExecMode.Single || _execMode == ExecMode.MultipleStart) {
            if(_vid > 0 && _pid > 0) {
                if( !_usbasp.begin(_vid, _pid, _extraName1, _extraName2, _serialNumber) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }
            else {
                if( !_usbasp.begin(                                                   ) ) throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecError, FuncName, "begin");
            }
        }

        // Execute the command(s) and uninitialize the system
        _execProgCommon_and_uninitialize(FuncName, commands, _usbasp, null);
    }
    //*/

} // class ProgExec
