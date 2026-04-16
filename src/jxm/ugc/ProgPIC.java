/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;

import java.util.Arrays;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.annotation.*;
import jxm.tool.fwc.*;
import jxm.ugc.pe.*;
import jxm.xb.*;


/*
 * This class and its related classes are written partially based on the algorithms and information found from:
 *
 *     PIC10F200⁄202⁄204⁄206 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/40001239F.pdf
 *
 *     PIC10F220/222 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/40001270F.pdf
 *
 *     PIC10(L)F320/322 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/PIC10%28L%29F320-322-Data-Sheet-40001585E.pdf
 *
 *     PIC12F508⁄509⁄16F505 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/41236E.pdf
 *
 *     PIC12F629/675 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/41190G.pdf
 *
 *     PIC12F609/615/617 and PIC12HV609/615 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/41302D.pdf
 *
 *     PIC16F5x Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/41213D.pdf
 *
 *     PIC16F627A/628A/648A Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/40044G.pdf
 *
 *     PIC16F630⁄676 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/40039F.pdf
 *
 *     PIC16F688 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/41203E.pdf
 *
 *     PIC16F7x Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/30325b.pdf
 *
 *     PIC16F7x Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/30325b.pdf
 *
 *     PIC16F8x Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/30430D.pdf
 *
 *     PIC16F84A Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/35007C.pdf
 *
 *     PIC16F87/88 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/30487D.pdf
 *
 *     PIC16F87x Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/30292D.pdf
 *
 *     PIC16F87xA Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/39582C.pdf
 *
 *     PIC16(L)F1503 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/40001607D.pdf
 *
 *     PIC16(L)F1508/9 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/PIC16-L-F1508-9-Microcontroller-Data-Sheet-DS40001609.pdf
 *
 *     PIC12F1501/16F1503/7/8/9 High-Temperature Microcontrollers Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/PIC12F1501-16F1503-7-8-9-High-Temp-Data-Sheet.pdf
 *
 *     PIC16(L)F1933 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/41575C.pdf
 *
 *     PIC16(L)F1934⁄6⁄7 DataSheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/41364E.pdf
 *
 *     PIC16F15213/14/23/24/43/44 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/PIC16F15213-14-23-24-43-44-Microcontroller-Data-Sheet-40002195.pdf
 *
 *     PIC16(L)F18425/45 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/DataSheets/PIC16-L-F18425-45-Microcontroller-Data-Sheet-DS40002002.pdf
 *
 *     PIC18F46J50 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/39931d.pdf
 *
 *     PIC18F2420⁄2520⁄4420⁄4520 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/PIC18F2420-2520-4420-4520-28-40-44-Pin-Microcontrollers-with-XLP-Technology-30009613F.pdf
 *
 *     PIC18(L)F2x/45K50 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/PIC18F2X_45K50-30000684B.pdf
 *
 *     PIC24FJ64GA004 Family Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/39881e.pdf
 *
 *     PIC24FJ64GB004 Family Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/39940d.pdf
 *
 *     PIC24FJ128GC010 Family Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU16/ProductDocuments/DataSheets/30009312d.pdf
 *
 *     PIC24FJ256GB210 Family Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU16/ProductDocuments/DataSheets/39975a.pdf
 *
 *     PIC24FV32KA304 Family Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/30009995e.pdf
 *
 *     PIC24HJ32GP202/204 and PIC24HJ16GP304 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/70289J.pdf
 *
 *     dsPIC30F Family Datasheet
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/70046E.pdf
 *
 *     dsPIC30F1010⁄202x SMPS Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/70000178d.pdf
 *
 *     dsPIC30F3010/3011 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/70141F.pdf
 *
 *     dsPIC30F4011/4012 Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/70135G.pdf
 *
 *     dsPIC33EPxxGS202 Family Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/dsPIC33EPXXGS202-Family-Data-Sheet-DS70005208E.pdf
 *
 *     PIC32Mx1xx/2xx Datasheet
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU32/ProductDocuments/DataSheets/PIC32MX1XX2XX283644-PIN_Datasheet_DS60001168L.pdf
 *
 *     ----------------------------------------------------------------------------------------------------
 *
 *     In-Circuit Serial Programming (ICSP) Guide
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/30277d.pdf
 *
 *     PIC10F200/202/204/206 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/ProgrammingSpecifications/41228F.pdf
 *
 *     PIC10F220⁄222 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41266C.pdf
 *
 *     PIC10(L)F320/322 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41572D.pdf
 *
 *     PIC12F508/509 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41227E.pdf
 *
 *     PIC12F629/675/PIC16F630/676 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41191D.pdf
 *
 *     PIC16F54 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41207D.pdf
 *
 *     PIC16F627A/628A/648A Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41196g.pdf
 *
 *     PIC12F629/675/PIC16F630/676 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41191D.pdf
 *
 *     PIC12F6xx⁄16F6xx Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/40001204J.pdf
 *
 *     PIC12F609/610/615/616/617 and PIC12HV609/610/616/615 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41284E.pdf
 *
 *     PIC16F7x Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/30324b.pdf
 *
 *     PIC16F8x Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/30262e.pdf
 *
 *     PIC16F87x Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/39025f.pdf
 *
 *     PIC16F87xA Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/39589C.pdf
 *
 *     PIC16F88x Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/ProgrammingSpecifications/41287D.pdf
 *
 *     PIC16F87/88 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/39607c.pdf
 *
 *     PIC12(L)F1501/PIC16(L)F150x Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/ProgrammingSpecifications/41573C.pdf
 *
 *     PIC16F193x/LF193x/PIC16F194x/LF194x/PIC16LF190x Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41397B.pdf
 *
 *     PIC16F152xx Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/ProgrammingSpecifications/PIC16F152XX-Family-Programming-Specification-40002149.pdf
 *
 *     PIC16(L)F184xx Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/ProgrammingSpecifications/PIC16L_F184XXProgramming_DS40001970A.pdf
 *
 *     PIC18Fxx2⁄xx8 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/39576c.pdf
 *
 *     PIC18F2xxx/4xxx Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/ProgrammingSpecifications/30009622M.pdf
 *
 *     PIC18F2XJxx/4XJxx Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/30009687F.pdf
 *
 *     PIC18F97J94 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/30000677C.pdf
 *
 *     PIC18(L)F2x/45K50 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/41630B.pdf
 *
 *     PIC18FxxK80 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/39972b.pdf
 *
 *     PIC18-Q20 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/ProgrammingSpecifications/PIC18-Q20-Family-Programming-Specification-DS40002327.pdf
 *
 *     PIC18-Q41 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/ProgrammingSpecifications/PIC18-Q41-Family-Programming-Specification-DS40002143.pdf
 *
 *     PIC18-Q71 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU08/ProductDocuments/ProgrammingSpecifications/PIC18-Q71-Family-Programming-Specification-DS40002306.pdf
 *
 *     PIC24FxxKA1xx/FVxxKA3xx Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU16/ProductDocuments/ProgrammingSpecifications/PIC24FXXKA1XX-KA3XX-Flash-Programming-Specifications-DS30009919.pdf
 *
 *     PIC24FJxxxGA0xx Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/39768d.pdf
 *
 *     PIC24FJ64GA1/GB0 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/30009934c.pdf
 *
 *     PIC24FJxxxDA1/DA2/GB2/GA3/GC0 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/39970e.pdf
 *
 *     dsPIC30F Flash Microcontroller Programming Specification (with STDP)
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/70102c.pdf
 *
 *     dsPIC30F Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/70102K.pdf
 *
 *     dsPIC30F SMPS Flash Microcontroller Programming Specification
 *     https://www.microchip.com/content/dam/mchp/documents/OTH/ProductDocuments/ProgrammingSpecifications/70284C.pdf
 *
 *     dsPIC33F/PIC24H Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/70152H.pdf
 *
 *     dsPIC33 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/ReferenceManuals/dsPIC33-PIC24-FRM-Flash-Programming-DS70000609E.pdf
 *
 *     dsPIC33EPxxGS202 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/70005192b.pdf
 *
 *     PIC32 Flash Microcontroller Programming Specification
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU32/ProductDocuments/ProgrammingSpecifications/PIC32-Flash-Programming-Specification-DS60001145.pdf
 *
 * ~~~ Last accessed & checked on 2024-12-15 ~~~
 */
public abstract class ProgPIC implements IProgCommon {

    /*
     * Transfer speed:
     *     # Using USB_ISS            : not supported
     *     # Using JxMake DASA        : not supported
     *     # Using JxMake USB-GPIO    : up to ~210 ... ~10200 bytes per second (depending on the target and operation);
     *                                  for some PIC MCUs that support EICSP, if the programmer uses it, it will be faster
     *                                  (and in some cases much, much faster)
     *     # Using JxMake USB-GPIO II : similar to 'JxMake USB-GPIO'           (depending on the target and operation);
     *                                  for some PIC MCUs that support EICSP, if the programmer uses it, it will be faster
     *                                  (and in some cases much, much faster)
     */

    /*
     * ######################################### !!! WARNING !!! #########################################
     * 1. Ensure that the selected high voltage (Vpp) matches the one required by the MCU!
     *    OVER-VOLTAGE WILL PERMANENTLY DAMAGE YOUR MCU!
     * 2. The initialization sequence for some PIC MCUs may actually corrupt other PIC MCUs (accidentally
     *    erasing their flash and/or configuration bits)!
     * 3. Some PIC MCUs can only be programmed using hardware-assisted bit-banging and thus the programmer
     *    will ignore the user-supplied PGC frequency in 'begin()'.
     * ######################################### !!! WARNING !!! #########################################
     */

    protected static final String ProgClassName = "ProgPIC";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final byte FlashMemory_EmptyValue = (byte) 0xFF;

    public static enum Part {

          PIC16 , // NOTE : PIC10 and PIC12 use the same ICSP protocol as PIC16
          PIC18 , // NOTE : It seems that the ICSP protocol for the PIC18FxQx MCUs is similar to the ICSP protocol for the PIC24 MCUs
          PIC24 ,
        dsPIC30 ,
        dsPIC33 ,
          PIC32M, // NOTE : It seems that all MIPS-based PIC32s have ICSP but not all ARM-based PIC32s have ICSP;
                  //        therefore, all ARM-based PIC32s should be programmed using ProgSWD

        // An invalid part
        _INVALID_

    } // enum Part

    public static enum SubPart {

        /*
         * [✔] Support is       implemented (possibly partially) and     tested with at least one MCU of the family.
         * [⁉] Support is       implemented (possibly partially) but not tested yet (and may not even be enabled).
         * [✸] Support is being implemented (possibly partially) and     tested with at least one MCU of the family.
         * [✘] Support is not   implemented at all at this time.
         */

        AK, //                                                                             [✘]dsPIC33
        C , //                   [✘]PIC16 [✘]PIC18
        CE, //          [✘]PIC12 [✘]PIC16
        CH, //                                                                             [✘]dsPIC33
        CK, //                                              [✘]PIC32C                      [✘]dsPIC33
        CM, //                                              [✘]PIC32C
        CR, //          [✘]PIC12 [✘]PIC16
        CX, //                                              [✘]PIC32C
        CZ, //                                              [✘]PIC32C
        EP, //                                     [✘]PIC24                                [✔]dsPIC33
        EV, //                                                                             [⁉]dsPIC33
        F , // [✔]PIC10 [✔]PIC12 [✔]PIC16 [✔]PIC18                              [✔]dsPIC30
        FJ, //                            [✔]PIC18 [✔]PIC24                                [✘]dsPIC33
        FK, //                            [✔]PIC18 [⁉]PIC24
        FQ, //                            [✘]PIC18
        FV, //                                     [✘]PIC24
        HJ, //                                     [⁉]PIC24
        HV, //          [✘]PIC12 [✘]PIC16
        LF, //                   [✘]PIC16
        MK, //                                                        [✘]PIC32M
        MM, //                                                        [✘]PIC32M
        MX, //                                                        [✘]PIC32M
        MZ, //                                                        [✘]PIC32M

        // An invalid sub-part
        _INVALID_

    } // enum SubPart

    public static enum Mode {

        HVSimple        , // High voltage programming without additional protocols                    ; PGD=0 and PGC=0 before nMCLR->Vpp
        HVSimple1       , // High voltage programming without additional protocols                    ; PGD=1 and PGC=1 before nMCLR->Vpp ; for dsPIC30 EICSP mode only?
        HVPulsedNOP     , // High voltage programming with    pulsed nMCLR->Vpp                                                           ; for dsPIC30  ICSP mode only?
        HVEntrySeq      , // High voltage programming with    32 bits entry sequence

        LVSimple        , // Low  voltage programming with    PGM pin

        LVEntrySeqM0M32 , // Low  voltage programming with    mode 0 MSB first 32 bits entry sequence ; long       nMCLR pulse before key
        LVEntrySeqM0M32S, // Low  voltage programming with    mode 0 MSB first 32 bits entry sequence ; very short nMCLR pulse before key ; for dsPIC33 (E)ICSP mode only?
        LVEntrySeqM1L32 , // Low  voltage programming with    mode 1 LSB first 32 bits entry sequence ; long       nMCLR pulse before key

        LVEntrySeqM1M32K, // Low  voltage programming with    mode 1 MSB first 32 bits entry sequence ; nMCLR is kept low after key
        LVEntrySeqM1L33K, // Low  voltage programming with    mode 1 LSB first 33 bits entry sequence ; nMCLR is kept low after key       ; for some PIC/10/12/16 MCUs only?

        Default         , // Use the default mode from the class

        // An invalid mode
        _INVALID_

        ;

        // Shorthands for common low-voltage programming modes with entry sequences
        // ##### ??? TODO : Find better names for them ??? #####

        public static final Mode LVEntrySeq    = LVEntrySeqM0M32;  // Most PIC MCUs use this
        public static final Mode LVEntrySeqS   = LVEntrySeqM0M32S;
        public static final Mode LVEntrySeq1L  = LVEntrySeqM1L32;

        public static final Mode LVEntrySeq1K  = LVEntrySeqM1M32K;
        public static final Mode LVEntrySeq1LK = LVEntrySeqM1L33K;

    } // enum Mode

    @SuppressWarnings("serial")
    public static class Config extends SerializableDeepClone<Config> {

        // NOTE : No '__0_JxMake_SerialVersionUID__' field here because it will be included in the subclasses

        public static class BaseProgSpec implements Serializable {
                              public Part    part     = Part   ._INVALID_;
                              public SubPart subPart  = SubPart._INVALID_;
                              public Mode    mode     = Mode   ._INVALID_;
            @DataFormat.Hex08 public long    entrySeq = -1               ; // Low voltage ICSP entry code/key sequence
        }

        public static class MemoryFlash implements Serializable {
            @DataFormat.Hex08 public long  address         =  0;
                              public int   totalSize       =  0;
                              public int   eraseBlockSize  =  0;
                              public int   writeBlockSize  =  0;
                              public int   eraseBlockSizeE = -1; // Optional: if this value is '> 0', it will be copied to 'eraseBlockSize' when in EICSP mode
                              public int   writeBlockSizeE = -1; // Optional: if this value is '> 0', it will be copied to 'writeBlockSize' when in EICSP mode

                              public int[] readDataBuff    = null;
        }

        public static class MemoryEEPROM implements Serializable {
            @DataFormat.Hex08 public long  addressBeg      = -1;
            @DataFormat.Hex08 public long  addressEnd      = -1;
                              public int   totalSize       = -1;
                              public int   writeBlockSizeE = -1; // Only used when in EICSP mode
                              public int   addressMulFW    =  1;
            @DataFormat.Hex08 public long  addressOfsFW    =  0;
        }

        public static class MemoryConfigBytes implements Serializable {
            @DataFormat.Hex08 public long   addressBeg   = -1;
            @DataFormat.Hex08 public long   addressEnd   = -1;
            @DataFormat.Hex08 public long[] address      = null;
            @DataFormat.Hex08 public long[] addressFW    = null; // Optional 'address-in-firmware'    value
            @DataFormat.Dec08 public int [] size         = null;
            @DataFormat.Hex08 public long[] bitMask      = null;
            @DataFormat.Hex08 public long[] orgMask      = null; // Optional 'reserve-original-value' mask
            @DataFormat.Hex08 public long[] clrMask      = null; // Optional     'always-clear-value' mask
            @DataFormat.Hex08 public long[] setMask      = null; // Optional     'always-set-value'   mask
                              public int    maxTotalSize =  0;   // Optional: if this value is '<= 0' then the total size will be calculated using 'address.length'
                              public int    addressMulFW =  1;
            @DataFormat.Hex08 public long   addressOfsFW =  0;

                              public int    prepadFWSize = -1;   // Optional - prepad the configuration data if specified
                              public int    prepadFWBegA = -1;   // ---
            @DataFormat.Hex02 public byte   prepadValue  =  0;   // ---

                              public int    prepadSizeFW = -1;   // Optional - prepad the configuration data if specified
                              public int    prepadAddrFW = -1;   // ---
            @DataFormat.Hex02 public byte   prepadByteFW =  0;   // ---

        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final BaseProgSpec      baseProgSpec;
        public final MemoryFlash       memoryFlash;
        public final MemoryEEPROM      memoryEEPROM;
        public final MemoryConfigBytes memoryConfigBytes;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public Config(final BaseProgSpec baseProgSpec_)
        {
            // Create/store the objects
            baseProgSpec      = baseProgSpec_;
            memoryFlash       = new MemoryFlash      ();
            memoryEEPROM      = new MemoryEEPROM     ();
            memoryConfigBytes = new MemoryConfigBytes();
        }

    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     *   1111110000000000
     *   5432109876543210
     *
     * 0 ------LSW0------
     * 1 --MSB1----MSB0--
     * 2 ------LSW1------
     */

    public static void packWords_w24x2_to_w16x3(final int[] src2, final int[] dst3)
    {
        dst3[0] =   (src2[0] & 0x00FFFF)                                          ;
        dst3[1] = ( (src2[1] & 0xFF0000) >>> 8 ) | ( (src2[0] & 0xFF0000) >>> 16 );
        dst3[2] =   (src2[1] & 0x00FFFF)                                          ;
    }

    public static void unpackWords_w16x3_to_w24x2(final int[] src3, final int[] dst2)
    {
        dst2[0] = ( (src3[1] & 0x00FF) << 16 ) | src3[0];
        dst2[1] = ( (src3[1] & 0xFF00) <<  8 ) | src3[2];
    }

    public static void unpackWords_w16x3_to_w24x2(final int src3_0, final int src3_1, final int src3_2, final int[] dst2)
    {
        dst2[0] = ( (src3_1 & 0x00FF) << 16 ) | src3_0;
        dst2[1] = ( (src3_1 & 0xFF00) <<  8 ) | src3_2;
    }

    /*
     *   1111110000000000
     *   5432109876543210
     *
     * 0 ------LSW0------
     * 1 --MSB1----MSB0--
     * 2 ------LSW1------
     *
     * 3 ------LSW2------
     * 4 --MSB3----MSB2--
     * 5 ------LSW3------
     */

    public static void packWords_w24x4_to_w16x6(final int[] src4, final int[] dst6)
    {
        dst6[0] =   (src4[0] & 0x00FFFF)                                          ;
        dst6[1] = ( (src4[1] & 0xFF0000) >>> 8 ) | ( (src4[0] & 0xFF0000) >>> 16 );
        dst6[2] =   (src4[1] & 0x00FFFF)                                          ;
        dst6[3] =   (src4[2] & 0x00FFFF)                                          ;
        dst6[4] = ( (src4[3] & 0xFF0000) >>> 8 ) | ( (src4[2] & 0xFF0000) >>> 16 );
        dst6[5] =   (src4[3] & 0x00FFFF)                                          ;
    }

    public static void unpackWords_w16x6_to_w24x4(final int[] src6, final int[] dst4)
    {
        dst4[0] = ( (src6[1] & 0x00FF) << 16 ) | src6[0];
        dst4[1] = ( (src6[1] & 0xFF00) <<  8 ) | src6[2];
        dst4[2] = ( (src6[4] & 0x00FF) << 16 ) | src6[3];
        dst4[3] = ( (src6[4] & 0xFF00) <<  8 ) | src6[5];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Depending on the MCU PIC, it can be called configuration bits, configuration bytes,
     *          configuration words, or others.
     *        # To simplify things, ProgPIC will refer to them all as 'configuration bytes (of varying
     *          sizes)'.
     */

    protected abstract int _picxx_minSANBAlignSize();
    protected abstract int _picxx_configByteSize();

    protected abstract int _picxx_readDeviceIDFull();
    protected abstract int _picxx_readDeviceID();

    protected abstract boolean _picxx_chipErase();

    protected abstract boolean _picxx_readFlash(final long address, final int[] dstBuff);
    protected abstract boolean _picxx_writeFlash(final boolean firstCall, final long address, final int[] srcBuff);

    protected abstract boolean _picxx_supportsEEPROMAutoErase();
    protected abstract boolean _picxx_readEntireEEPROM(final int[] dstBuff);
    protected abstract boolean _picxx_writeEntireEEPROM(final int[] srcBuff, final boolean[] fDirty, final boolean eepromErased);

    protected abstract long _picxx_readConfigByte(final long address);
    protected abstract boolean _picxx_writeConfigBytes(final long[] refBuff, final long[] newBuff);

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract ProgPIC_EICSP.CmdPE _picxx_pe_cmdPE();

    protected abstract byte[] _picxx_pe_checkAdjustPE(final byte[] data);

    protected abstract int[] _picxx_pe_readSavedWords();
    protected abstract boolean _picxx_pe_writeSavedWords(final int[] srcBuff);

    protected abstract boolean _picxx_pe_eraseArea();
    protected abstract boolean _picxx_pe_writeData(final boolean firstCall, final long address, final int[] srcBuff);
    protected abstract boolean _picxx_pe_readData(final long address, final int[] dstBuff);

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected int SPI_CLKDIV_HI_FREQ = USB2GPIO.UndeterminedFrequency; // Clock divider value for high  frequency (used for most             operations)
    protected int SPI_CLKDIV_LO_FREQ = USB2GPIO.UndeterminedFrequency; // Clock divider value for low   frequency (used for some delicate    operations)
    protected int SPI_CLKDIV_XW_FREQ = USB2GPIO.UndeterminedFrequency; // Clock divider value for extra frequency (used for some EICSP write operations)
    protected int SPI_CLKDIV_XR_FREQ = USB2GPIO.UndeterminedFrequency; // Clock divider value for extra frequency (used for some EICSP read  operations)

    protected int SPI_CLKFRQ_HI_FREQ = USB2GPIO.UndeterminedFrequency; // ---
    protected int SPI_CLKFRQ_LO_FREQ = USB2GPIO.UndeterminedFrequency; // ---
    protected int SPI_CLKFRQ_XW_FREQ = USB2GPIO.UndeterminedFrequency; // The realized frequency of the corresponding clock divider above
    protected int SPI_CLKFRQ_XR_FREQ = USB2GPIO.UndeterminedFrequency; // ---

    private boolean _pic_initialize(final int pgcFreq)
    {
        // Initialize the SPI clock divider
        final int[] clkSpecHI = _usb2gpio.spiGetClkSpec( pgcFreq                            );
        final int[] clkSpecLO = _usb2gpio.spiGetClkSpec( _usb2gpio.spiGetFastestBBClkFreq() );

        SPI_CLKDIV_HI_FREQ = clkSpecHI[1];
        SPI_CLKDIV_LO_FREQ = clkSpecLO[1];
        SPI_CLKDIV_XW_FREQ = SPI_CLKDIV_HI_FREQ; // By default, set this to the same value as the one for high frequency
        SPI_CLKDIV_XR_FREQ = SPI_CLKDIV_HI_FREQ; // ---

        // Save the realized frequencies
        SPI_CLKFRQ_HI_FREQ = clkSpecHI[0];
        SPI_CLKFRQ_LO_FREQ = clkSpecLO[0];
        SPI_CLKFRQ_XW_FREQ = SPI_CLKFRQ_HI_FREQ; // By default, set this to the same value as the one for high frequency
        SPI_CLKFRQ_XR_FREQ = SPI_CLKFRQ_HI_FREQ; // ---

        // Initialize the SPI
        // WARNING : If the underlying SPI system uses bit banging, then the clock divider is ignored (not used)
        for(int i = 0; i < 2; ++i) {
            // Initialize the SPI
            // @@@ @@@ @@@
            if( _usb2gpio.spiBegin( _pic_spiMode(), USB2GPIO.SSMode.ActiveLow, SPI_CLKDIV_HI_FREQ ) ) break;
            // Error initializing the SPI - exit if this is the 2nd initialization attempt
            if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSPI, ProgClassName);
            // Uninitialize the SPI and try again
            _usb2gpio.spiEnd();
        }

        // Initialize the UART using the standard mode and baudrate
        for(int i = 0; i < 2; ++i) {
            // Initialize the UART
            if( _usb2gpio.uartBegin(USB2GPIO.UXRTMode._8N1, 115200) ) break;
            // Error initializing the UART - exit if this is the 2nd initialization attempt
            if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitUART, ProgClassName);
            // Uninitialize the UART and try again
            _usb2gpio.uartEnd();
        }

        // Bring all signals to their inactive states
        _pic_nmmclr_1();
        _pic_pgm_0   ();

        SysUtil.sleepMS(5);

        // Done
        return true;
    }

    private boolean _pic_uninitialize()
    {
        // Uninitialize the UART and SPI
        final boolean resUARTEnd = _usb2gpio.uartEnd();
        final boolean resSPIEnd  = _usb2gpio.spiEnd ();

        // Check for error(s)
        if(!resUARTEnd || !resSPIEnd) {
            if(!resUARTEnd) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitUART, ProgClassName);
            if(!resSPIEnd ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitSPI , ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    private boolean _pic_nmmclr_0()
    { return _usb2gpio.spiSelectSlave(); }

    private boolean _pic_nmmclr_1()
    { return _usb2gpio.spiDeselectSlave(); }

    private boolean _pic_pgm_0()
    { return _usb2gpio.rawSerialPort().setBreak(); }

    private boolean _pic_pgm_1()
    { return _usb2gpio.rawSerialPort().clearBreak(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _pic_xbTransfer_c4_d24(final int[] ioBuff)
    {
        // Prepare the buffer and index
        final int[] trBuff = new int[ioBuff.length / 2 * 8];
              int   trIdx  = 0;

        // Generate the byte sequence
        for(int i = 0; i < ioBuff.length; i += 2) {
            final int cmd = XCom._RU04(ioBuff[i + 0]);
            final int dat = XCom._RU24(ioBuff[i + 1]);
            trBuff[trIdx++] = 3; trBuff[trIdx++] = cmd;
            trBuff[trIdx++] = 7; trBuff[trIdx++] = (dat >>> 16) & 0xFF;
            trBuff[trIdx++] = 7; trBuff[trIdx++] = (dat >>>  8) & 0xFF;
            trBuff[trIdx++] = 7; trBuff[trIdx++] = (dat >>>  0) & 0xFF;
            /*
            System.out.printf( "%01X %02X %02X %02X\n", cmd, (dat >>> 16) & 0xFF, (dat >>> 8) & 0xFF, (dat >>> 0) & 0xFF );
            //*/
        }

        // Transfer the byte sequence
        if( !_usb2gpio.spiXBTransferIgnoreSS(USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, 0, USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, trBuff) ) return false;

        // Process the response
        trIdx = 0;

        for(int i = 0; i < ioBuff.length; i += 2) {
            ioBuff[i + 0] = XCom._RU04(    trBuff[trIdx + 1]        );
            ioBuff[i + 1] = XCom._RU24(   (trBuff[trIdx + 3] << 16)
                                        | (trBuff[trIdx + 5] <<  8)
                                        | (trBuff[trIdx + 7] <<  0) );
            trIdx += 8;
        }

        // Done
        return true;
    }

    private boolean _pic_entryC32_m0_msb32()
    {
        // NOTE : Use bit banging for simplicity
        for(int i = 31; i >= 0; --i) {
            final boolean bit = ( (_config.baseProgSpec.entrySeq) & (0x01 << i) ) != 0;
            if( !_usb2gpio.spiSetBreak(bit, false) ) return false;
            if( !_usb2gpio.spiSetBreak(bit, true ) ) return false;
        }

        if( !_usb2gpio.spiSetBreak(false, false) ) return false;

        return true;
    }

    private boolean _pic_entryC32_m1_msb32()
    {
        // NOTE : Use bit banging for simplicity
        for(int i = 31; i >= 0; --i) {
            final boolean bit = ( (_config.baseProgSpec.entrySeq) & (0x01 << i) ) != 0;
            if( !_usb2gpio.spiSetBreak(bit, true ) ) return false;
            if( !_usb2gpio.spiSetBreak(bit, false) ) return false;
        }

        return true;
    }

    private boolean _pic_entryC32_m1_lsb32()
    {
        // NOTE : Use bit banging for simplicity
        for(int i = 0; i <= 31; ++i) {
            final boolean bit = ( (_config.baseProgSpec.entrySeq) & (0x01 << i) ) != 0;
            if( !_usb2gpio.spiSetBreak(bit, true ) ) return false;
            if( !_usb2gpio.spiSetBreak(bit, false) ) return false;
        }

        if( !_usb2gpio.spiSetBreak(false, true ) ) return false; // Send a 33rd dummy bit
        if( !_usb2gpio.spiSetBreak(false, false) ) return false;

        return true;
    }

    private boolean _pic_entryC32_m1_lsb33()
    {
        // NOTE : Use bit banging for simplicity
        for(int i = 0; i <= 31; ++i) {
            final boolean bit = ( (_config.baseProgSpec.entrySeq) & (0x01 << i) ) != 0;
            if( !_usb2gpio.spiSetBreak(bit, true ) ) return false;
            if( !_usb2gpio.spiSetBreak(bit, false) ) return false;
        }

        if( !_usb2gpio.spiSetBreak(false, true ) ) return false; // Send a 33rd dummy bit
        if( !_usb2gpio.spiSetBreak(false, false) ) return false;

        SysUtil.sleepMS(1);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int     _vpp_vdd_delayMS = 0;
    private boolean _vdd_turnOff     = false;

    // NOTE : By default use SPI mode 1; override this function if required
    protected USB2GPIO.SPIMode _pic_spiMode()
    { return USB2GPIO.SPIMode._1; }

    // NOTE : By default this function does nothing; override this function if required
    protected boolean _pic_enterPVM_mcuSpecific_extra()
    { return true; }

    protected boolean _pic_enterPVM()
    {
        // Set/clear the PGD and PGC line
        if(_config.baseProgSpec.mode == Mode.HVSimple1) {
            if( !_usb2gpio.spiSetBreak(true , true ) ) return false;
        }
        else {
            if( !_usb2gpio.spiSetBreak(false, false) ) return false;
        }
            SysUtil.sleepMS(10);

        /*
        // ##### !!! TODO : REMOVE THIS LATER !!! #####
        _pic_pgm_0(); _pic_nmmclr_0();
        _pic_pgm_1(); _pic_nmmclr_1();
        if( !_usb2gpio.spiSetBreak(false, true ) ) return false;
        if( !_usb2gpio.spiSetBreak(true , false) ) return false;
        for(int i = 0; i < 0x7FFFFFFF; ++i) SysUtil.sleepMS(1);
        //*/
        /*
        // ##### !!! TODO : REMOVE THIS LATER !!! #####
        for(int i = 0; i < 0x7FFFFFFF; ++i) {
            _pic_pgm_1(); _pic_nmmclr_0(); SysUtil.sleepMS(1);
            _pic_pgm_0(); _pic_nmmclr_1(); SysUtil.sleepMS(1);
        }
        //*/

        /*
        // ##### !!! TODO : REMOVE THIS LATER !!! #####
        if( !_usb2gpio.spiClrBreak() ) return false;
        _usb2gpio.spiSetClkDiv( _usb2gpio.spiClkFreqToClkDiv( _usb2gpio.spiGetSlowestBBClkFreq() ) );
        _usb2gpio.spiSetClkDiv( _usb2gpio.spiClkFreqToClkDiv( _usb2gpio.spiGetFastestBBClkFreq() ) );
        _usb2gpio.spiSetClkDiv(  8                                                                 );
        _usb2gpio.spiSetClkDiv( 15                                                                 );
        _usb2gpio.spiSetClkDiv( 101                                                                );
        _usb2gpio.spiSetClkDiv( 105                                                                );
        _usb2gpio.spiSetClkDiv( 111                                                                );
        for(int i = 0; i < 0x7FFFF10F; ++i) {
            _usb2gpio.spiTransferIgnoreSS( new int[] { 0b10101010 } );
        }
        //*/

        // Get the delay
        int vpp_vdd_delayMS = Math.abs(_vpp_vdd_delayMS);

        // Set the next delay to zero if it is negative
        if(_vpp_vdd_delayMS < 0) _vpp_vdd_delayMS = 0;

        // If the delay is not zero, assume that the PGM signal controls the Vdd (active low)
        // NOTE : # Assume that the PGM signal controls the Vdd (active low).
        //        # If the programmer does not support this feature then Vdd must be controlled manually!
        final boolean has_vpp_vdd_delay = (vpp_vdd_delayMS > 0);

        // Send the entry sequence
        switch(_config.baseProgSpec.mode) {

            case HVSimple  : /* FALLTHROUGH */
            case HVSimple1 : if(true) {
                    // Use Vdd control
                    if(has_vpp_vdd_delay) {
                        // Send the sequence
                        if( !_pic_pgm_1            () ) return false; SysUtil.sleepMS(500                 ); // Turn off Vdd
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(vpp_vdd_delayMS + 10); // Turn on  Vpp
                        if( !_pic_pgm_0            () ) return false; SysUtil.sleepMS(500                 ); // Turn on  Vdd
                        // Set the flag
                        _vdd_turnOff = true;
                    }
                    // Do not use Vdd control
                    else {
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(10);                   // Turn on  Vpp
                    }
                    /*
                    SysUtil.sleepMS(100000);
                    //*/
                }
                break;

            case HVPulsedNOP : if(true) {
                    // Use Vdd control as needed
                    if(has_vpp_vdd_delay) {
                        if( !_pic_pgm_1            () ) return false; SysUtil.sleepMS(500);                  // Turn off Vdd
                        if( !_pic_pgm_0            () ) return false; SysUtil.sleepMS(500);                  // Turn on  Vdd
                        // Set the flag
                        _vdd_turnOff = true;
                    }
                        // Use hardware-assisted bit-banging to send the entry sequence
                        if( !( (USB_GPIO) _usb2gpio ).spiXBSpecial_dsPIC30_HVICSP_EntrySequence() ) return false;
                }
                break;

            case HVEntrySeq : if(true) {
                    // Use Vdd control as needed
                    if(has_vpp_vdd_delay) {
                        if( !_pic_pgm_1            () ) return false; SysUtil.sleepMS(500);                  // Turn off Vdd
                        if( !_pic_pgm_0            () ) return false; SysUtil.sleepMS(500);                  // Turn on  Vdd
                        // Set the flag
                        _vdd_turnOff = true;
                    }
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(10);                   // Turn on  Vpp
                        if( !_pic_entryC32_m0_msb32() ) return false; SysUtil.sleepMS(30);                   // Send the key sequence
                }
                break;

            case LVSimple : if(true) {
                        if( !_pic_nmmclr_0() ) return false; SysUtil.sleepMS(10);                            // Assert   nMCLR
                        if( !_pic_pgm_1   () ) return false; SysUtil.sleepMS(10);                            // Assert   PGM
                        if( !_pic_nmmclr_1() ) return false; SysUtil.sleepMS(10);                            // Deassert nMCLR
                }
                break;

            case LVEntrySeqM0M32 : if(true) {
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(10);                   // Assert   nMCLR
                        if( !_pic_nmmclr_1         () ) return false; SysUtil.sleepMS(10);                   // Deassert nMCLR
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(10);                   // Assert   nMCLR
                        if( !_pic_entryC32_m0_msb32() ) return false; SysUtil.sleepMS(30);                   // Send the key sequence
                        if( !_pic_nmmclr_1         () ) return false; SysUtil.sleepMS(10);                   // Deassert nMCLR
                }
                break;

            case LVEntrySeqM0M32S : if(true) {
                        // Use hardware-assisted bit-banging to send the initial entry sequence
                        if( !( (USB_GPIO) _usb2gpio ).spiXBSpecial_dsPIC33_LVX_EntrySequence() ) return false;
                        SysUtil.sleepMS(10);
                        // Send the rest of the entry sequence
                        if( !_pic_entryC32_m0_msb32() ) return false; SysUtil.sleepMS(30);                   // Send the key sequence
                        if( !_pic_nmmclr_1         () ) return false; SysUtil.sleepMS(10);                   // Deassert nMCLR
                }
                break;

            case LVEntrySeqM1L32 : if(true) {
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(10);                   // Assert   nMCLR
                        if( !_pic_nmmclr_1         () ) return false; SysUtil.sleepMS(10);                   // Deassert nMCLR
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(10);                   // Assert   nMCLR
                        if( !_pic_entryC32_m1_lsb32() ) return false; SysUtil.sleepMS(30);                   // Send the key sequence
                        if( !_pic_nmmclr_1         () ) return false; SysUtil.sleepMS(10);                   // Deassert nMCLR
                }
                break;

            case LVEntrySeqM1M32K : if(true) {
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(10);                   // Assert   nMCLR
                        if( !_pic_entryC32_m1_msb32() ) return false; SysUtil.sleepMS(30);                   // Send the key sequence
                }
                break;

            case LVEntrySeqM1L33K : if(true) {
                        if( !_pic_nmmclr_0         () ) return false; SysUtil.sleepMS(10);                   // Assert   nMCLR
                        if( !_pic_entryC32_m1_lsb33() ) return false; SysUtil.sleepMS(30);                   // Send the key sequence
                }
                break;

        } // switch

        if( !_pic_enterPVM_mcuSpecific_extra() ) return false;

        // Only keep in hardware-assisted bit-banging SPI mode as needed
        if( !useHardwareAssistedBitBangingSPI() ) {
            if( !_usb2gpio.spiClrBreak() ) return false;
        }

        // Delay for a while
        SysUtil.sleepMS(250);

        // Done
        return true;
    }

    protected boolean _pic_exitPVM()
    {
        if( !_usb2gpio.spiSetBreak(false, false) ) return false;
        SysUtil.sleepMS(2);

        switch(_config.baseProgSpec.mode) {

            case HVSimple  : /* FALLTHROUGH */
            case HVSimple1 : if(true) {
                    // If the '_vdd_turnOff' flag is set, assume that the PGM signal controls the Vdd (active low)
                    if(_vdd_turnOff) {
                        // Send the sequence
                        if( !_pic_nmmclr_1() ) return false; SysUtil.sleepMS(100); // Turn off Vpp
                        if( !_pic_pgm_1   () ) return false; SysUtil.sleepMS(500); // Turn off Vdd
                        // Clear the flag
                        _vdd_turnOff = true;
                    }
                    // Otherwise, there is no need to control the Vdd
                    else {
                        if( !_pic_nmmclr_1() ) return false; SysUtil.sleepMS(10);  // Turn off Vpp
                    }
                }
                break;

            case HVPulsedNOP : if(true) {
                    // If the '_vdd_turnOff' flag is set, assume that the PGM signal controls the Vdd (active low)
                    if(_vdd_turnOff) {
                        if( !_pic_pgm_1   () ) return false; SysUtil.sleepMS(500); // Turn off Vdd
                    }
                        if( !_pic_nmmclr_1() ) return false; SysUtil.sleepMS(10);  // Turn off Vpp
                }
                break;

            case HVEntrySeq : if(true) {
                    // If the '_vdd_turnOff' flag is set, assume that the PGM signal controls the Vdd (active low)
                    if(_vdd_turnOff) {
                        if( !_pic_pgm_1   () ) return false; SysUtil.sleepMS(500); // Turn off Vdd
                    }
                        if( !_pic_nmmclr_1() ) return false; SysUtil.sleepMS(10);  // Turn off Vpp
                }
                break;

            case LVSimple   : if(true) {
                        if( !_pic_nmmclr_0() ) return false; SysUtil.sleepMS(10);  // Assert   nMCLR
                        if( !_pic_pgm_0   () ) return false; SysUtil.sleepMS(10);  // Deassert PGM
                        if( !_pic_nmmclr_1() ) return false; SysUtil.sleepMS(10);  // Deassert nMCLR
                }
                break;

            case LVEntrySeqM0M32  : /* FALLTHROUGH */
            case LVEntrySeqM0M32S : /* FALLTHROUGH */
            case LVEntrySeqM1L32  : if(true) {
                        if( !_pic_nmmclr_0() ) return false; SysUtil.sleepMS(10);  // Assert   nMCLR
                        if( !_pic_nmmclr_1() ) return false; SysUtil.sleepMS(10);  // Deassert nMCLR
                }
                break;

            case LVEntrySeqM1M32K : /* FALLTHROUGH */
            case LVEntrySeqM1L33K : if(true) {
                        if( !_pic_nmmclr_1() ) return false; SysUtil.sleepMS(10);  // Deassert nMCLR
                }
                break;

        } // switch

        if( !_usb2gpio.spiClrBreak() ) return false;

        // Delay for a while
        SysUtil.sleepMS(250);

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected final USB2GPIO _usb2gpio;
    protected final Config   _config;

    private         boolean  _inProgMode   = false;
    private         boolean  _flashErased  = false;
    private         boolean  _eepromErased = false;

    @SuppressWarnings("this-escape")
    protected ProgPIC(final USB2GPIO usb2gpio, final Config config) throws Exception
    {
        // Store the objects
        _usb2gpio = usb2gpio;
        _config   = config.deepClone();

        // Modify the configuration values as needed
        if(_config.baseProgSpec.mode == Mode.Default) {
            switch(_config.baseProgSpec.part) {

                case PIC16:
                    _config.baseProgSpec.mode     = Mode.HVSimple;
                    _config.baseProgSpec.entrySeq = -1;
                    break;

                case PIC18:
                    _config.baseProgSpec.mode     = Mode.LVSimple;
                    _config.baseProgSpec.entrySeq = -1;
                    break;

                case PIC24:
                    _config.baseProgSpec.mode     = Mode.LVEntrySeq;
                    _config.baseProgSpec.entrySeq = 0x4D434851L;      // NOTE : Do not use EICSP by default
                    break;

                case dsPIC30:
                    _config.baseProgSpec.mode     = Mode.HVPulsedNOP; // NOTE : Do not use EICSP by default
                    break;

                case dsPIC33:
                    _config.baseProgSpec.mode     = Mode.LVEntrySeqS;
                    _config.baseProgSpec.entrySeq = 0x4D434851L;      // NOTE : Do not use EICSP by default
                    break;

                case PIC32M:
                    // ##### !!! TODO !!! #####
                    // NOTE : NOT SUPPORTED FOR NOW !!!
                    throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSPartUns , ProgClassName);

            } // switch
        }

        // Check the configuration values
        if(_config.baseProgSpec.part    == Part   ._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSPart , ProgClassName);
        if(_config.baseProgSpec.subPart == SubPart._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSSPrt , ProgClassName);

        if(_config.baseProgSpec.mode    == Mode   ._INVALID_) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSMode , ProgClassName);
        if(_config.baseProgSpec.mode    == Mode   .Default  ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSMode , ProgClassName);

        if(_config.baseProgSpec.mode == Mode.HVEntrySeq || _config.baseProgSpec.mode == Mode.LVEntrySeq) {
            if(_config.baseProgSpec.entrySeq < 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSEntrySq , ProgClassName);
        }

        if(_config.memoryFlash.address        <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFAddress , ProgClassName);
        if(_config.memoryFlash.totalSize      <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFTotSize , ProgClassName);
        if(_config.memoryFlash.eraseBlockSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFEBufSize, ProgClassName);
        if(_config.memoryFlash.writeBlockSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFWBufSize, ProgClassName);

        if(_config.memoryEEPROM.addressBeg >= 0 || _config.memoryEEPROM.addressEnd >= 0 || _config.memoryEEPROM.totalSize > 0) {
            if(_config.memoryEEPROM.addressBeg      <  0                              ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEAddressB, ProgClassName);
            if(_config.memoryEEPROM.addressEnd      <= _config.memoryEEPROM.addressBeg) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEAddressE, ProgClassName);
            if(_config.memoryEEPROM.totalSize       <= 0                              ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMETotSize , ProgClassName);
            if(_config.memoryEEPROM.writeBlockSizeE <  0                              ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEWBufSizE, ProgClassName);
            if(_config.memoryEEPROM.addressMulFW    <  0                              ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEAdrMulFW, ProgClassName);
            if(_config.memoryEEPROM.addressOfsFW    <  0                              ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEAdrOfsFW, ProgClassName);

            if(_config.memoryEEPROM.addressEnd - _config.memoryEEPROM.addressBeg + 1 != _config.memoryEEPROM.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEAddrSpec, ProgClassName);
        }

        if(_config.memoryConfigBytes.addressBeg >= 0 || _config.memoryConfigBytes.addressEnd >= 0 || _config.memoryConfigBytes.address != null) {
                if(_config.memoryConfigBytes.addressBeg     <   0                                      ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBAddresB, ProgClassName);
                if(_config.memoryConfigBytes.addressEnd     <  _config.memoryConfigBytes.addressBeg    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBAddresE, ProgClassName);
                if(_config.memoryConfigBytes.size.length    != _config.memoryConfigBytes.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBSizeL  , ProgClassName);
                if(_config.memoryConfigBytes.bitMask.length != _config.memoryConfigBytes.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBBitMskL, ProgClassName);

            if(_config.memoryConfigBytes.addressFW != null) {
                if(_config.memoryConfigBytes.addressFW.length != _config.memoryConfigBytes.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBAddrFWL, ProgClassName);
            }
            if(_config.memoryConfigBytes.orgMask != null) {
                if(_config.memoryConfigBytes.orgMask.length   != _config.memoryConfigBytes.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBOrgMskL, ProgClassName);
            }
            if(_config.memoryConfigBytes.clrMask != null) {
                if(_config.memoryConfigBytes.clrMask.length   != _config.memoryConfigBytes.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBClrMskL, ProgClassName);
            }
            if(_config.memoryConfigBytes.setMask != null) {
                if(_config.memoryConfigBytes.setMask.length   != _config.memoryConfigBytes.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBSetMskL, ProgClassName);
            }
                if(_config.memoryConfigBytes.addressMulFW   <  0                                       ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBAdMulFW, ProgClassName);
                if(_config.memoryConfigBytes.addressOfsFW   <  0                                       ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBAdOfsFW, ProgClassName);

            for(int i = 0; i < _config.memoryConfigBytes.address.length; ++i) {
                    if(_config.memoryConfigBytes.address[i] <  0                                   ) continue;
                    if(_config.memoryConfigBytes.address[i] <  _config.memoryConfigBytes.addressBeg) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBAddress, ProgClassName);
                    if(_config.memoryConfigBytes.address[i] >  _config.memoryConfigBytes.addressEnd) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBAddress, ProgClassName);
                    if(_config.memoryConfigBytes.size   [i] != _picxx_configByteSize()             ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBSize   , ProgClassName);
                    if(_config.memoryConfigBytes.bitMask[i] <  0                                   ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBBitMask, ProgClassName);
                if(_config.memoryConfigBytes.orgMask != null) {
                    if(_config.memoryConfigBytes.orgMask[i] <  0                                   ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBOrgMask, ProgClassName);
                }
                if(_config.memoryConfigBytes.clrMask != null) {
                    if(_config.memoryConfigBytes.clrMask[i] <  0                                   ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBClrMask, ProgClassName);
                }
                if(_config.memoryConfigBytes.setMask != null) {
                    if(_config.memoryConfigBytes.setMask[i] <  0                                   ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBSetMask, ProgClassName);
                }
            }
        }
    }

    public Config config()
    { return _config; }

    // NOTE : By default use hardware SPI mode; override this function if required
    public boolean useHardwareAssistedBitBangingSPI()
    { return false; }

    // NOTE : By default use ICSP mode; override this function if required
    public boolean inSTDPMode()
    { return false; }

    // NOTE : By default use ICSP mode; override this function if required
    public boolean inEICSPMode()
    { return false; }

    protected boolean _begin(final int pgcFreq, final int vpp_vdd_delayMS)
    {
        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flags
        _flashErased  = false;
        _eepromErased = false;

        // Copy the delay
        _vpp_vdd_delayMS = vpp_vdd_delayMS;

        // Enable mode
        if(_usb2gpio instanceof USB_GPIO) {
            if( !( (USB_GPIO) _usb2gpio ).pcf8574Enable_ICSP() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitPCF8574, ProgClassName);
        }

        // Try to initialize using the fastest frequency first
        boolean initDone = false;

        // If the delay is negative, it means recovery mode
        if(_vpp_vdd_delayMS < 0) {

            // Initalize the system
            if( _pic_initialize( _usb2gpio.spiGetSlowestHWClkFreq() ) ) {
                // Enter program & verify mode
                if( !_pic_enterPVM() ) USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_EnterPVM, ProgClassName);
                // Assume good here
                initDone = true;
            }

        }

        // Otherwise, it means normal mode
        else {

            // Initialize the system
            if( _pic_initialize(pgcFreq) ) {

                // Enter program & verify mode
                if( !_pic_enterPVM() ) {
                    USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_EnterPVM, ProgClassName);
                }
                // Try to read and check the signature
                else {
                    // Read the signature
                    if( _readSignature_impl() ) {
                        // Check the signature
                        final int[] sig = mcuSignature();
                        if(sig != null && sig.length >= 2) {
                            if(sig[0] != 0x00 && sig[0] != 0xFF) {
                                // We should be good here
                                initDone = true;
                            }
                            else if( _picxx_readDeviceIDFull() == 0x00 ) {
                                // The device does not support MCU signature, assume good here
                                initDone = true;
                            }
                        }
                    }
                } // if

                // Uninitialize on error
                if(!initDone) {
                    _pic_exitPVM();
                    _pic_uninitialize();
                }

            } // if

        }

        if(!initDone) {
                 if( inSTDPMode () ) USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_InitSTDP , ProgClassName);
            else if( inEICSPMode() ) USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_InitEICSP, ProgClassName);
            else                     USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_InitICSP , ProgClassName);
            return false;
        }

        /*
        SysUtil.stdDbg().printf( "### %dMHz OK\n", _usb2gpio.spiGetSupportedClkFreqs()[SPI_CLKDIV_HI_FREQ] );
        //*/

        // Set flag
        _inProgMode = true;

        // Done
        return true;
    }

    // NOTE : For EICSP, 'pgcFreq' must result in hardware-assisted bit-banging SPI (hence, use a negative number)
    public boolean begin(final int pgcFreq)
    { return _begin(pgcFreq, 0); }

    @Override
    public boolean end()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Commit EEPROM, exit programming mode, and uninitialize all
        final boolean resCommitEEPROM  = commitEEPROM();
        final boolean resExitPVM       = _pic_exitPVM();
        final boolean resUninitSytem   = _pic_uninitialize();

        // Disable mode
        boolean resDisMode = true;

        if(_usb2gpio instanceof USB_GPIO) {
            resDisMode = ( (USB_GPIO) _usb2gpio ).pcf8574Disable();
        }

        // Clear flag and data
        _inProgMode   = false;

        _eepromBuffer = null;
        _eepromFDirty = null;

        // Check for error(s)
        if(!resCommitEEPROM || !resExitPVM || !resUninitSytem || !resDisMode) {
            if(!resCommitEEPROM) USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_CmEEPROM , ProgClassName);
            if(!resExitPVM     ) USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_ExitPVM  , ProgClassName);
            if(!resUninitSytem ) USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_UninitSys, ProgClassName);
            if(!resDisMode     ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPCF8574, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    /*
     * If you like, call this after calling 'begin()'
     *
     * NOTE : 1. 'pgcFreqWr' may  result in either hardware SPI or hardware-assisted bit-banging SPI.
     *        2. 'pgcFreqRd' must result in                        hardware-assisted bit-banging SPI (hence, use a negative number).
     */
    public boolean setEICSPExtraSpeed(final int pgcFreqWr, final int pgcFreqRd)
    {
        final int[] clkSpecXW = _usb2gpio.spiGetClkSpec(pgcFreqWr);
        final int[] clkSpecXR = _usb2gpio.spiGetClkSpec(pgcFreqRd);

        if( clkSpecXW[0] < 0 || clkSpecXR[0] < 0 ) return false;

        SPI_CLKDIV_XW_FREQ = clkSpecXW[1];
        SPI_CLKDIV_XR_FREQ = clkSpecXR[1];

        SPI_CLKFRQ_XW_FREQ = clkSpecXW[0];
        SPI_CLKFRQ_XR_FREQ = clkSpecXR[0];

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _mcuSignature = null;

    public boolean _readSignature_impl()
    {
        // Clear the signature buffer first
        _mcuSignature = null;

        // Generate signature bytes from the device ID
        final int devID = _picxx_readDeviceID();

        if(devID > 0x00FFFFFF) {
            _mcuSignature = new int[] {
                (devID >> 24) & 0xFF,
                (devID >> 16) & 0xFF,
                (devID >>  8) & 0xFF,
                (devID >>  0) & 0xFF
            };
        }
        else if(devID > 0x0000FFFF) {
            _mcuSignature = new int[] {
                (devID >> 16) & 0xFF,
                (devID >>  8) & 0xFF,
                (devID >>  0) & 0xFF
            };
        }
        else {
            _mcuSignature = new int[] {
                (devID >>  8) & 0xFF,
                (devID >>  0) & 0xFF
            };
        }

        // Done
        return true;
    }

    @Override
    public boolean supportSignature()
    { return false; }

    @Override
    public boolean readSignature()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Read the signature
        return _readSignature_impl();
    }

    @Override
    public boolean verifySignature(final int[] signatureBytes)
    {
        // Error if the signature has not been read
        if(_mcuSignature == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Compare the signature
        boolean ok = true;

        if(signatureBytes.length != _mcuSignature.length) ok = false;

        if(ok) {
            for(int i = 0; i < _mcuSignature.length; ++i) {
                if( signatureBytes[i] < 0                 ) continue; // Skip if the user does not want to check this byte
                if( signatureBytes[i] != _mcuSignature[i] ) { ok = false; break; }
            }
        }

        // Programming executive might not support reading device ID properly, so assume good here
        if(!ok) ok = inEICSPMode() && ( _picxx_readDeviceIDFull() == 0x00 );

        // Return the result
        return ok;
    }

    @Override
    public int[] mcuSignature()
    {
        // Error if the signature has not been read
        if(_mcuSignature == null) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return null;
        }

        // Return the signature
        return _mcuSignature;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_flashErased && _eepromErased) return true;

        // Perform chip erase
        if( !_picxx_chipErase() ) return false;

        // Set flags
        _flashErased  = true;
        _eepromErased = true;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int _flashMemoryTotalSize()
    { return _config.memoryFlash.totalSize; }

    @Override
    public byte _flashMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    public byte _flashMemoryEmptyValue(final int posIndex)
    { return FlashMemory_EmptyValue; }

    @Override
    public int _flashMemoryAlignWriteSize(final int numBytes)
    { return USB2GPIO.alignWriteSize(numBytes, _config.memoryFlash.writeBlockSize); }

    @Override
    public int _eepromMemoryTotalSize()
    { return _config.memoryEEPROM.totalSize; }

    @Override
    public byte _eepromMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    @Override
    public int[] _readDataBuff()
    { return _config.memoryFlash.readDataBuff; }

    private int _verifyReadFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Determine the start address and number of bytes
        long sa = (startAddress < 0) ? _config.memoryFlash.address   : startAddress;
        int  nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        if(sa >= _config.memoryFlash.address) sa -= _config.memoryFlash.address;

        //*
        // Check the start address and number of bytes
        final int _mn = _picxx_minSANBAlignSize();

        if( _mn == 2 ) {
            if( !USB2GPIO.checkStartAddressAndNumberOfBytes_even( (int) sa, nb, _config.memoryFlash.totalSize,      ProgClassName ) ) return -1;
        }
        else {
            if( !USB2GPIO.checkStartAddressAndNumberOfBytes_mN  ( (int) sa, nb, _config.memoryFlash.totalSize, _mn, ProgClassName ) ) return -1;
        }
        //*/

        // Prepare the result buffer
        if(_config.memoryFlash.readDataBuff == null || _config.memoryFlash.readDataBuff.length != numBytes) {
            _config.memoryFlash.readDataBuff = new int[nb];
        }

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Determine the chunk size
        final int ChunkSize = inEICSPMode()
                            ?                                              _config.memoryFlash.writeBlockSize
                            : Math.max(_config.memoryFlash.eraseBlockSize, _config.memoryFlash.writeBlockSize);

        // Determine the number of chunks
        final boolean notAligned = (nb % ChunkSize) != 0;
        final int     numChunks  = (nb / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        final boolean verify = (refData != null);
              int     rdbIdx = 0;
              int     verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, nb - rdbIdx);

            // Read the chunk bytes
            final int[] cbytes = new int[numReads];

            if( !_picxx_readFlash( (int) (_config.memoryFlash.address + sa + c * ChunkSize), cbytes ) ) return -1;

            // Process the chunk bytes
            for(int b = 0; b < numReads; b += 2) {

                // Store the bytes to the result buffer
                                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b    ];
                if(rdbIdx < nb) _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b + 1];

                // Compare the bytes as needed
                if(verify) {
                    for(int i = 0; i < 2; ++i) {
                        if(verIdx < refData.length) {
                            if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) {
                                if( refData[verIdx] == _flashMemoryEmptyValue() ) {
                                    if( _config.memoryFlash.readDataBuff[verIdx] != _flashMemoryEmptyValue(verIdx) ) return verIdx;
                                }
                                else {
                                    return verIdx;
                                }
                            }
                            ++verIdx;
                        }
                    }
                }

                // Call the progress callback function for the current value
                pcb.callProgressCallbackCurrent(progressCallback, nb);

            } // for b

        } // for c

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return nb;
    }

    @Override
    public boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(null, startAddress, numBytes, progressCallback) == numBytes; }

    private boolean _writeFlashPage(final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Clear flag
        _flashErased = false;

        // Determine the chunk size and the number of chunks
        final int ChunkSize = _config.memoryFlash.writeBlockSize;
        final int numChunks = (nb / ChunkSize);

        // Write the bytes
        int     datIdx    = 0;
        int     wrCnt     = 0;
        boolean firstCall = true;

        for(int c = 0; c < numChunks; ++c) {

            // Write the chunk bytes
            final int     cbytes[] = USB2GPIO.ba2ia(data, datIdx, ChunkSize);
                  boolean cblank   = true;

            for(final int b : cbytes) {
                if( b != (FlashMemory_EmptyValue & 0xFF) ) {
                    cblank = false;
                    break;
                }
            }

            if(!cblank) { // Skip writing the chunk if it is blank (its contents are all 'FlashMemory_EmptyValue')
                if( !_picxx_writeFlash( firstCall, (int) (_config.memoryFlash.address + sa + c * ChunkSize), cbytes ) ) return false;
                firstCall = false;
            }

            /*
            SysUtil.stdDbg().printf( "%03d (%03d) : %b\n", (int) (_config.memoryFlash.address + sa + c * ChunkSize), cbytes.length, cblank );
            //*/

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, ChunkSize / 2);

            // Increment the counters
            datIdx  += ChunkSize;

        } // for c

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return true;
    }

    @Override
    public boolean writeFlash(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Determine the start address and number of bytes
              long sa = (startAddress < 0) ? 0                             : startAddress;
        final int  nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        if(sa >= _config.memoryFlash.address) sa -= _config.memoryFlash.address;

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer(data, (int) sa, nb, _config.memoryFlash.writeBlockSize, _config.memoryFlash.totalSize, FlashMemory_EmptyValue, ProgClassName);
        if(anbr == null) return false;

        // Write flash
        return _writeFlashPage(anbr.buff, (int) sa, anbr.nb, progressCallback);
    }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _verifyReadPE_lastErrorOpcode = -1;

    private boolean _writePEPage(final byte[] data, final int nb, final IntConsumer progressCallback)
    {
        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Clear flag
        _flashErased = false;

        // Determine the chunk size and the number of chunks
        final int ChunkSize = _config.memoryFlash.writeBlockSize;
        final int numChunks = (nb / ChunkSize);

        // Write the bytes
        int datIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Write the chunk bytes
            final int cbytes[] = USB2GPIO.ba2ia(data, datIdx, ChunkSize);

            if( !_picxx_pe_writeData( c == 0, c * ChunkSize, cbytes ) ) return false;

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, ChunkSize / 2);

            // Increment the counters
            datIdx += ChunkSize;

        } // for c

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return true;
    }

    private int _verifyReadPE(final byte[] refData, final int nb, final int realDataLen, final IntConsumer progressCallback, final boolean doNotExitOnMismatch)
    {
        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Clear the last error opcode
        _verifyReadPE_lastErrorOpcode = -1;

        // Determine the chunk size and the number of chunks
        final int ChunkSize = _config.memoryFlash.writeBlockSize;
        final int numChunks = (nb / ChunkSize);

        // Verify the bytes
        int rdbIdx = 0;
        int verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, nb - rdbIdx);

            // Read the chunk bytes
            final int[] cbytes = new int[numReads];

            if( !_picxx_pe_readData(c * ChunkSize, cbytes) ) return -1;

            /*
            for(int z = 0; z < numReads; ++z) SysUtil.stdDbg().printf( "%06X %02X\n", 0x800000 + (verIdx + z) * 2 / 3, cbytes[z] );
            //*/

            // Process the chunk bytes
            for(int b = 0; b < numReads; b += 2) {

                // Increment the read index
                rdbIdx += 2;

                // Compare the bytes
                for(int i = 0; i < 2; ++i) {

                    /*
                    if(cbytes[b + i] == 0xCB) SysUtil.stdDbg().printf("[#CB] %06X\n", (c * ChunkSize + b) * 2 / 3); // [#CB] 0007F0
                    if(cbytes[b + i] == 0xBB) SysUtil.stdDbg().printf("[#BB] %06X\n", (c * ChunkSize + b) * 2 / 3); // [#BB] 0005BE
                    //*/

                    if(verIdx <= realDataLen) {

                        if( cbytes[b + i] != (refData[verIdx] & 0xFF) ) {

                            /*
                            // ##### @@@ @@@ @@@ #####
                            //if( (refData[verIdx] & 0xFF) != 0x00 )
                            SysUtil.stdDbg().printf( "[%04X] %02X -> %02X\n", verIdx, refData[verIdx], cbytes[b + i] );
                            //*/

                            if( refData[verIdx] == _flashMemoryEmptyValue() ) {
                                if( cbytes[b + i] != _flashMemoryEmptyValue(verIdx) ) {
                                    _verifyReadPE_lastErrorOpcode = cbytes[b + i];
                                    if(!doNotExitOnMismatch) return verIdx;
                                }
                            }
                            else {
                                _verifyReadPE_lastErrorOpcode = cbytes[b + i];
                                if(!doNotExitOnMismatch) return verIdx;
                            }
                        }

                        ++verIdx;

                    } // if

                } // for

                // Call the progress callback function for the current value
                pcb.callProgressCallbackCurrent(progressCallback, nb);

            } // for b

        } // for c

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return nb;
    }

    public boolean programPE(final byte[] data_, final IntConsumer progressCallback)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Read the saved words
        final int[] savedWords = _picxx_pe_readSavedWords();

        if(savedWords == null) return false;

        /*
        for(int i : savedWords) SysUtil.stdDbg().printf("%04X\n", i);
        //*/

        // Check and adjust the PE as needed
        final byte[] data = _picxx_pe_checkAdjustPE(data_);

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer(data, 0, data.length, _config.memoryFlash.writeBlockSize, 2048 * 3, FlashMemory_EmptyValue, ProgClassName);
        if(anbr == null) return false;

        // Check if exactly the same PE is already programmed
        if(progressCallback != null) SysUtil.stdDbg().print("<C>");
        if( _verifyReadPE(anbr.buff, anbr.nb, data.length, progressCallback, true) < 0 ) return false;

        if(_verifyReadPE_lastErrorOpcode < 0) return true;

        /*
        // ##### @@@ @@@ @@@ #####
        if(true) return true;
        //*/

        // Erase PE
        if( !_picxx_pe_eraseArea() ) return false;

        // Write PE
        if(progressCallback != null) SysUtil.stdDbg().print("<W>");
        if( !_writePEPage(anbr.buff, anbr.nb, progressCallback) ) return false;

        // Verify PE
        if(progressCallback != null) SysUtil.stdDbg().print("<V>");
        final int res = _verifyReadPE(anbr.buff, anbr.nb, data.length, progressCallback, false);

        if(res < 0) return false;

        if(res < data.length) {
            SysUtil.stdDbg().printf("\nPE[0x%06X] 0x%02X -> 0x%02X\n", res, data[res] & 0xFF, _verifyReadPE_lastErrorOpcode);
            return false;
        }

        // Write back the saved words
        if( !_picxx_pe_writeSavedWords(savedWords) ) return false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add more standard PEs for other MCUs ??? #####

    public static byte[] stdPE_dsPIC30F() throws JXMAsmError
    { return USB2GPIO.ia2ba( dsPIC30F_PE.getPE_dsPIC30F() ); }

    public static int stdPE_dsPIC30F_startAddress() throws JXMAsmError
    { return dsPIC30F_PE.getPE_dsPIC30F_startAddress(); }

    public static byte[] stdPE_dsPIC30F_SMPS() throws JXMAsmError
    { return USB2GPIO.ia2ba( dsPIC30F_PE.getPE_dsPIC30F_SMPS() ); }

    public static int stdPE_dsPIC30F_SMPS_startAddress() throws JXMAsmError
    { return dsPIC30F_PE.getPE_dsPIC30F_SMPS_startAddress(); }

    public static byte[] stdPE_dsPIC33EP_GS25() throws JXMAsmError
    { return USB2GPIO.ia2ba( dsPIC33E_L1_PE.getPE_dsPIC33EP_GS25() ); }

    public static int stdPE_dsPIC33EP_GS25_startAddress() throws JXMAsmError
    { return dsPIC33E_L1_PE.getPE_dsPIC33EP_GS25_startAddress(); }

    public static byte[] stdPE_dsPIC33EV_GM01() throws JXMAsmError
    { return USB2GPIO.ia2ba( dsPIC33E_L1_PE.getPE_dsPIC33EV_GM01() ); }

    public static int stdPE_dsPIC33EV_GM01_startAddress() throws JXMAsmError
    { return dsPIC33E_L1_PE.getPE_dsPIC33EV_GM01_startAddress(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[]     _eepromBuffer = null;
    private boolean[] _eepromFDirty = null;

    public boolean supportsEEPROMAutoErase()
    { return _picxx_supportsEEPROMAutoErase(); }

    @Override
    public int readEEPROM(final int address)
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Error if EEPROM is not available
        if(_config.memoryEEPROM.totalSize <= 0) {
            USB2GPIO.notifyError(Texts.ProgXXX_ENotAvailable, ProgClassName);
            return -1;
        }

        // Check the address
        if(address < 0 || address >= _config.memoryEEPROM.totalSize) {
            USB2GPIO.notifyError(Texts.ProgXXX_EAddrOoR, ProgClassName);
            return -1;
        }

        // Read the entire EEPROM as needed
        if(_eepromBuffer == null) {
            // Initialize the buffers and mark everything as not dirty
            _eepromBuffer = new int[_config.memoryEEPROM.totalSize];
            _eepromFDirty = new boolean[_config.memoryEEPROM.totalSize];
            Arrays.fill(_eepromFDirty, false);
            // Read the bytes
            //*
            if( !_picxx_readEntireEEPROM(_eepromBuffer) ) return -1;
            //*/
            /*
            final long tc1 = SysUtil.getNS();
            if( !_picxx_readEntireEEPROM(_eepromBuffer) ) return -1;
            final long tc2 = SysUtil.getNS();
            SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", _eepromBuffer.length, (tc2 - tc1) * 0.000000001, 1000000000.0 * _eepromBuffer.length / (tc2 - tc1) );
            //*/
        }

        // Return the byte
        return _eepromBuffer[address];
    }

    @Override
    public boolean writeEEPROM(final int address, final byte data)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Error if EEPROM is not available
        if(_config.memoryEEPROM.totalSize <= 0) return USB2GPIO.notifyError(Texts.ProgXXX_ENotAvailable, ProgClassName);

        // Check the address
        if(address < 0 || address >= _config.memoryEEPROM.totalSize) return USB2GPIO.notifyError(Texts.ProgXXX_EAddrOoR, ProgClassName);

        // The EEPROM must be written in page mode, hence, read the entire EEPROM first as needed
        if(_eepromBuffer == null) {
            if( readEEPROM(0) < 0 ) return false;
        }

        // Store the new byte to the buffer and mark the position as dirty
        final int newData = data & 0xFF;

        if(_eepromBuffer[address] != newData) {
            _eepromBuffer[address] = newData;
            _eepromFDirty[address] = true;
        }

        // Done
        return true;
    }

    public boolean writeEEPROM(final byte[] data, final int startAddress, final int numBytes)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Check the address and size
        if(startAddress                < _config.memoryEEPROM.addressBeg) return false;
        if(startAddress + numBytes - 1 > _config.memoryEEPROM.addressEnd) return false;

        if(numBytes                    > _config.memoryEEPROM.totalSize ) return false;

        // Write the values
        for(int i = 0; i < numBytes; ++i) {
            if( !writeEEPROM( (int) (startAddress - _config.memoryEEPROM.addressBeg + i), data[i] ) ) return false;
        }

        // Done
        return true;
    }

    public boolean commitEEPROM()
    {
        // Simply exit if there is no EEPROM buffer
        if(_eepromBuffer == null) return true;

        // Clear flag
        final boolean ce = _eepromErased;

        _eepromErased = false;

        // Write the EEPROM
        //*
        if( !_picxx_writeEntireEEPROM(_eepromBuffer, _eepromFDirty, ce) ) return false;
        //*/
        /*
        final long tc1 = SysUtil.getNS();
        if( !_picxx_writeEntireEEPROM(_eepromBuffer, _eepromFDirty, ce) ) return false;
        final long tc2 = SysUtil.getNS();
        SysUtil.stdDbg().printf( "### %d bytes in %.3f seconds (%.3f bytes/second) ###\n", _eepromBuffer.length, (tc2 - tc1) * 0.000000001, 1000000000.0 * _eepromBuffer.length / (tc2 - tc1) );
        //*/

        //*
        // Mark everything as not dirty
        Arrays.fill(_eepromFDirty, false);
        //*/

        //*
        // Clear the buffers
        _eepromBuffer = null;
        _eepromFDirty = null;
        //*/

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : Fuses are PIC configuration bits!

    protected long _getNewCWValue(final int i, final long[] refBuff, final long[] newBuff, final Config config)
    {
        // Get the new value
        long newVal = newBuff[i] & config.memoryConfigBytes.bitMask[i];

        // Modify the new value
        if(config.memoryConfigBytes.orgMask != null) { newVal &=             ~config.memoryConfigBytes.orgMask[i];
                                                       newVal |= refBuff[i] & config.memoryConfigBytes.orgMask[i]; }
        if(config.memoryConfigBytes.clrMask != null) { newVal &=             ~config.memoryConfigBytes.clrMask[i]; }
        if(config.memoryConfigBytes.setMask != null) { newVal |=              config.memoryConfigBytes.setMask[i]; }

        // Return the new value
        return newVal;
    }

    protected boolean _fusesInProgramMemorySpace()
    {
        // NOTE : # On some PIC MCUs, the configuration bits are part of the program memory space and are written
        //          together with the program.
        //        # Call this function if required.

        if(_config.memoryConfigBytes.addressBeg * _config.memoryConfigBytes.addressMulFW + _config.memoryConfigBytes.addressOfsFW >= _config.memoryFlash.address &&
           _config.memoryConfigBytes.addressEnd * _config.memoryConfigBytes.addressMulFW + _config.memoryConfigBytes.addressOfsFW <= _config.memoryFlash.address + _config.memoryFlash.totalSize - 1
        ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_CfBitsNOP, ProgClassName);
            return true;
        }

        return false;
    }

    public long[] readFuses()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return null;
        }

        // Read the bytes
        final long[] cbs = new long[_config.memoryConfigBytes.address.length];

        for(int i = 0; i < cbs.length; ++i) {
            if(_config.memoryConfigBytes.address[i] >= 0) {

                final long res = _picxx_readConfigByte(_config.memoryConfigBytes.address[i]);
                if(res < 0) return null;

                cbs[i] = res & _config.memoryConfigBytes.bitMask[i];

            }
            else {
                cbs[i] = -1;
            }
        }

        // Return the bytes
        return cbs;
    }

    public boolean writeFuses(final long[] values)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Read the original fuses
        final long[] origCBS = readFuses();

        if(origCBS == null) return USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_ReadCByte, ProgClassName);

        // Modify the bytes as needed
        final long[]  newCBS  = XCom.arrayCopy(values);
              boolean changed = false;

        for(int i = 0; i < _config.memoryConfigBytes.address.length; ++i) {
            if(_config.memoryConfigBytes.address[i] < 0 || values[i] < 0) {
                // Skip those that the user does not want to change
                newCBS[i] = (origCBS[i] < 0) ? 0 : origCBS[i];
            }
            else if(origCBS[i] != values[i]) {
                newCBS[i] = values[i];
                changed   = true;
                /*
                SysUtil.stdErr().printf("FUSE #%d CHANGED %02X -> %02X\n", i, origCBS[i], values[i]);
                //*/
            }
        }

        //for(int i = 0; i < _config.memoryConfigBytes.address.length; ++i) values[i] = newCBS[i];

        // Simply exit if nothing changes
        if(!changed) return true;

        // Write the bytes
        if( !_picxx_writeConfigBytes(origCBS, newCBS) ) return false;

        // Done
        return true;
    }

    public boolean writeFuses(final byte[] data, final int startAddress, final int numBytes)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Check the address and size
        final int  bsz          = _picxx_configByteSize();

        final long addressBeg   = _config.memoryConfigBytes.addressBeg;
        final long addressEnd   = addressBeg + (_config.memoryConfigBytes.addressEnd - addressBeg + 1) * bsz - 1;

        final int  maxTotalSize = (_config.memoryConfigBytes.maxTotalSize > 0) ?  _config.memoryConfigBytes.maxTotalSize
                                                                               : (_config.memoryConfigBytes.address.length * bsz);

        /*
        SysUtil.stdDbg().printf("### %06X | %06X | %b\n", startAddress               , addressBeg  , startAddress                <  addressBeg  );
        SysUtil.stdDbg().printf("### %06X | %06X | %b\n", startAddress + numBytes - 1, addressEnd  , startAddress + numBytes - 1 >  addressEnd  );
        SysUtil.stdDbg().printf("### %6d | %6d | %b\n"  , numBytes                   , maxTotalSize, numBytes                    >  maxTotalSize);
        //*/

        if(startAddress                <  addressBeg  ) return false;
        if(startAddress + numBytes - 1 >  addressEnd  ) return false;

        if(numBytes                    >  maxTotalSize) return false;
        if(numBytes % bsz              != 0           ) return false;

        // Copy the values
        final boolean useMap = _config.memoryConfigBytes.addressFW != null;
        final long[]  values = new long[_config.memoryConfigBytes.address.length];

        java.util.Arrays.fill(values, -1);

        for(int i = 0; i < numBytes; i += bsz) {

            final int a = startAddress + i;

            for(int j = 0; j < _config.memoryConfigBytes.address.length; ++j) {

                if(_config.memoryConfigBytes.address[j] < 0) continue;

                if(!useMap) {
                    if(_config.memoryConfigBytes.address  [j] != a) continue;
                }
                else {
                    if(_config.memoryConfigBytes.addressFW[j] != a) continue;
                }

                switch(bsz) {

                    case 1:
                        if(true) {
                            values[j] = (         data[i]           & 0xFF        );
                            /*
                            SysUtil.stdDbg().printf( "FuseByte[1] %06X : %02X\n", a, values[j] );
                            //*/
                        }
                        break;

                    case 2:
                        if(!useMap) {
                            values[j] = (        (data[i * bsz + 0] & 0xFF) <<  0 )
                                      | (        (data[i * bsz + 1] & 0xFF) <<  8 );
                            /*
                            SysUtil.stdDbg().printf( "FuseByte[2] %06X : %04X\n", a, values[j] );
                            //*/
                        }
                        else {
                            values[j] = (        (data[i       + 0] & 0xFF) <<  0 )
                                      | (        (data[i       + 1] & 0xFF) <<  8 );
                            /*
                            SysUtil.stdDbg().printf( "FuseByte[2] [%04X|%04X] %06X : %04X |%04X\n",
                                i, data.length - 2,
                                a,
                                values[j],
                                  ( ( data[data.length - 2] & 0xFF ) << 0 ) | ( ( data[data.length - 1] & 0xFF ) << 8 )
                            );
                            //*/
                        }
                        break;

                    case 4:
                        if(!useMap) {
                            values[j] = ( (long) (data[i * bsz + 0] & 0xFF) <<  0 )
                                      | ( (long) (data[i * bsz + 1] & 0xFF) <<  8 )
                                      | ( (long) (data[i * bsz + 2] & 0xFF) << 16 )
                                      | ( (long) (data[i * bsz + 3] & 0xFF) << 24 );
                            /*
                            SysUtil.stdDbg().printf( "FuseByte[4] %06X : %08X\n", a, values[j] );
                            //*/
                        }
                        else {
                            values[j] = ( (long) (data[i       + 0] & 0xFF) <<  0 )
                                      | ( (long) (data[i       + 1] & 0xFF) <<  8 )
                                      | ( (long) (data[i       + 2] & 0xFF) << 16 )
                                      | ( (long) (data[i       + 3] & 0xFF) << 24 );
                        }
                        break;

                    default:
                        // Invalid size
                        return false;

                } // switch

                /*
                System.out.printf("%08X = %08X\n", _config.memoryConfigBytes.address[j], values[j]);
                //*/

            } // for j

        } // for i

        // Write the fuses
        return writeFuses(values);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : Reading and writing lock bits are not supported by ProgPIC, all configuration are done via fuses!

    @Override
    public long readLockBits()
    { return -1; }

    @Override
    public boolean writeLockBits(final long value)
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class FWD {
        public FWComposer fw             = null;
        public byte[]     fwDataBuff     = null;
        public int        fwLength       =  0;
        public int        fwStartAddress = -1;

        public FWComposer cf             = null;
        public byte[]     cfDataBuff     = null;
        public int        cfLength       =  0;
        public int        cfStartAddress = -1;

        public FWComposer ep             = null;
        public byte[]     epDataBuff     = null;
        public int        epLength       =  0;
        public int        epStartAddress = -1;
    }

    /*
     * NOTE : # It is highly recommended to call this function instead of calling:
     *              <FWComposer_instance>.getFlattenedBinaryData( <ProgPICxx_instance>._flashMemoryEmptyValue() )
     *        # Override this function if required.
     */
    public FWD fwDecompose(final FWComposer fwc_) throws Exception
    {
        // Instantiate the holder object
        final FWD        fwd = new FWD();

        // Clone the firmware composer object so the original is kept pristine
        final FWComposer fwc = fwc_.deepClone();

        // Extract the firmware (program) data
        final FWComposer fwb = fwc.decomposeRange(
                                   _config.memoryFlash.address                                    ,
                                   _config.memoryFlash.address + _config.memoryFlash.totalSize - 1
                               );

        // Extract the configuration bytes data
        final FWComposer cfb = fwc.decomposeRange(
                                   _config.memoryConfigBytes.addressBeg * _config.memoryConfigBytes.addressMulFW + _config.memoryConfigBytes.addressOfsFW,
                                   _config.memoryConfigBytes.addressEnd * _config.memoryConfigBytes.addressMulFW + _config.memoryConfigBytes.addressOfsFW
                               );

        /*
        SysUtil.stdDbg().printf (">>> %04X => %04X\n", _config.memoryConfigBytes.addressBeg, _config.memoryConfigBytes.addressBeg * _config.memoryConfigBytes.addressMulFW + _config.memoryConfigBytes.addressOfsFW);
        SysUtil.stdDbg().printf (">>> %04X => %04X\n", _config.memoryConfigBytes.addressEnd, _config.memoryConfigBytes.addressEnd * _config.memoryConfigBytes.addressMulFW + _config.memoryConfigBytes.addressOfsFW);
        SysUtil.stdDbg().println(">>> " + cfb);
        //*/

        // Extract the EEPROM data
        final FWComposer epr = fwc.decomposeRange(
                                   _config.memoryEEPROM     .addressBeg * _config.memoryEEPROM     .addressMulFW + _config.memoryEEPROM     .addressOfsFW,
                                   _config.memoryEEPROM     .addressEnd * _config.memoryEEPROM     .addressMulFW + _config.memoryEEPROM     .addressOfsFW
                               );

        /*
        SysUtil.stdDbg().printf (">>> %04X => %04X\n", _config.memoryEEPROM.addressBeg, _config.memoryEEPROM.addressBeg * _config.memoryEEPROM.addressMulFW + _config.memoryEEPROM.addressOfsFW);
        SysUtil.stdDbg().printf (">>> %04X => %04X\n", _config.memoryEEPROM.addressEnd, _config.memoryEEPROM.addressEnd * _config.memoryEEPROM.addressMulFW + _config.memoryEEPROM.addressOfsFW);
        SysUtil.stdDbg().println(">>> " + cfb);
        //*/

        // Store the firmware (program) data (if it exists)
        if(fwb != null) {
            fwd.fw             =  fwb;
            fwd.fwDataBuff     =  fwb.getFlattenedBinaryData( _flashMemoryEmptyValue() );
            fwd.fwLength       =  fwd.fwDataBuff.length;
            fwd.fwStartAddress = (fwd.fwLength <= 0) ? -1 : ( (int) ( fwb.fwBlocks().get(0).startAddress()                                                                                    ) );
        }

        // Store the configuration bytes data (if it exists)
        if(cfb != null) {
            if(_config.memoryConfigBytes.prepadSizeFW > 0 && _config.memoryConfigBytes.prepadAddrFW >= 0) {
                // Prepad as needed --- a workaround when the location of the configuration bytes data can move
                // forward (depending on some factors) when the firmware of some PIC MCUs is loaded from a HEX
                // file and flattened
                final byte[] cBuff = cfb.getFlattenedBinaryData( _flashMemoryEmptyValue() );
                if(_config.memoryConfigBytes.prepadSizeFW > cBuff.length) {
                    final byte[] pBuff = new byte[_config.memoryConfigBytes.prepadSizeFW - cBuff.length];
                    Arrays.fill(pBuff, _config.memoryConfigBytes.prepadByteFW);
                    cfb.fwBlocks().add( 0, new FWBlock(pBuff, _config.memoryConfigBytes.prepadAddrFW) );
                }
            }
            fwd.cf             =  cfb;
            fwd.cfDataBuff     =  cfb.getFlattenedBinaryData( _flashMemoryEmptyValue() );
            fwd.cfLength       =  fwd.cfDataBuff.length;
            fwd.cfStartAddress = (fwd.cfLength <= 0) ? -1 : ( (int) ( cfb.fwBlocks().get(0).startAddress() / _config.memoryConfigBytes.addressMulFW  - _config.memoryConfigBytes.addressOfsFW ) );
        }

        // Store the EEPROM data (if it exists)
        if(epr != null) {
            fwd.ep             =  epr;
            fwd.epDataBuff     =  epr.getFlattenedBinaryData( _flashMemoryEmptyValue() );
            fwd.epLength       =  fwd.epDataBuff.length;
            fwd.epStartAddress = (fwd.epLength <= 0) ? -1 : ( (int) ( epr.fwBlocks().get(0).startAddress() / _config.memoryEEPROM     .addressMulFW - _config.memoryEEPROM     .addressOfsFW ) );
        }

        // If all lengths are zero, assume all data is firmware
        if(fwd.fwLength == 0 && fwd.cfLength == 0 && fwd.epLength == 0) {
            fwd.fw             =  fwc;
            fwd.fwDataBuff     =  fwc.getFlattenedBinaryData( _flashMemoryEmptyValue() );
            fwd.fwLength       =  fwd.fwDataBuff.length;
            fwd.fwStartAddress = (fwd.fwLength <= 0) ? -1 : ( (int) ( fwc.fwBlocks().get(0).startAddress()                                                                                    ) );
        }

        // Return the holder object
        return fwd;
    }

} // class ProgPIC
