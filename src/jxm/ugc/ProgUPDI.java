/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.PrintStream;
import java.io.Serializable;

import java.util.Arrays;
import java.util.function.IntConsumer;

import com.fazecast.jSerialComm.*;

import jxm.*;
import jxm.annotation.*;
import jxm.tool.*;
import jxm.xb.*;


/*
 * This class is written partially based on the algorithms and information found from:
 *
 *     pymcuprog
 *     Python MCU programmer
 *     https://github.com/microchip-pic-avr-tools/pymcuprog
 *     MIT License
 *
 *     Adafruit_AVRProg
 *     https://github.com/adafruit/Adafruit_AVRProg
 *     Copyright (C) 2019 ladyada & brandanlane
 *     MIT License
 *
 *     portaprog
 *     https://gitlab.com/bradanlane/portaprog
 *     Copyright (C) 2020 bradanlane
 *     MIT License
 */
public class ProgUPDI implements IProgCommon {

    /*
     * Transfer speed:
     *     # Using USB_ISS            : not supported
     *     # Using JxMake DASA        : up to ~7150 ... ~26400 bytes per second (depending on the baudrate)
     *     # Using JxMake USB-GPIO    : up to ~7450 ... ~34900 bytes per second (depending on the baudrate)
     *     # Using JxMake USB-GPIO II : up to ~7450 ... ~40300 bytes per second (depending on the baudrate)
     */

    private static final String ProgClassName = "ProgUPDI";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final byte FlashMemory_EmptyValue = (byte) 0xFF;

    @SuppressWarnings("serial")
    public static class Config extends SerializableDeepClone<Config> {

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        // NOTE : The default values below are for (almost all?) AVR MCUs that can be programmed using UPDI

        public static class MemoryAVRBase implements Serializable {
            @DataFormat.Hex04 public int NVM    = 0x1000;
            @DataFormat.Hex04 public int SYSCFG = 0x0F00;
            @DataFormat.Hex04 public int OCD    = 0x0F80;
        }

        public static class MemorySignature implements Serializable {
            @DataFormat.Hex04 public int address = 0x1100;
                              public int    size = 3;
        }

        // NOTE : These default values apply to megaAVR 0-series and tinyAVR 0/1/2-series devices
        public static class MemorySignatureExt implements Serializable {
            @DataFormat.Hex04 public int address_SerNum       = 0x1100 + 0x03;
                              public int    size_SerNum       = 10;

            @DataFormat.Hex04 public int address_OscCal16     = 0x1100 + 0x18;
            @DataFormat.Hex02 public int bitMask_OscCal16     = 0x7F;
            @DataFormat.Hex04 public int address_OscCal16TCal = 0x1100 + 0x19;
            @DataFormat.Hex02 public int bitMask_OscCal16TCal = 0x0F;

            @DataFormat.Hex04 public int address_OscCal20     = 0x1100 + 0x1A;
            @DataFormat.Hex02 public int bitMask_OscCal20     = 0x7F;
            @DataFormat.Hex04 public int address_OscCal20TCal = 0x1100 + 0x1B;
            @DataFormat.Hex02 public int bitMask_OscCal20TCal = 0x0F;

            @DataFormat.Hex04 public int address_Osc16Err3V   = 0x1100 + 0x22;
            @DataFormat.Hex02 public int bitMask_Osc16Err3V   = 0xFF;
            @DataFormat.Hex04 public int address_Osc16Err5V   = 0x1100 + 0x23;
            @DataFormat.Hex02 public int bitMask_Osc16Err5V   = 0xFF;

            @DataFormat.Hex02 public int address_Osc20Err3V   = 0x1100 + 0x24;
            @DataFormat.Hex04 public int bitMask_Osc20Err3V   = 0xFF;
            @DataFormat.Hex04 public int address_Osc20Err5V   = 0x1100 + 0x25;
            @DataFormat.Hex02 public int bitMask_Osc20Err5V   = 0xFF;

            @DataFormat.Hex02 public int bitSize_TempSense    = 8;
            @DataFormat.Hex04 public int address_TempSense0   = 0x1100 + 0x20;
            @DataFormat.Hex02 public int bitMask_TempSense0   = 0xFF;
            @DataFormat.Hex04 public int address_TempSense1   = 0x1100 + 0x21;
            @DataFormat.Hex02 public int bitMask_TempSense1   = 0xFF;
        }

        public static class MemoryFlash implements Serializable {
            @DataFormat.Hex04 public int   address       = 0;
                              public int   totalSize     = 0;
                              public int   pageSize      = 0;
                              public int   numPages      = 0;

                              public int[] readDataBuff  = null;
        }

        public static class MemoryEEPROM implements Serializable {
            @DataFormat.Hex04 public int address   = 0x1400;
                              public int totalSize = 0;
                              public int pageSize  = 0;
                              public int numPages  = 0;
        }

        // NOTE : These default values apply to megaAVR 0-series and tinyAVR 0/1/2-series devices
        public static class MemoryFuse implements Serializable {
                              //                                 0           1           2           3   4   5           6           7           8           9
                              //                                 wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     append      bootend
            @DataFormat.Hex04 public int[] address = new int[] { 0x1280    , 0x1281    , 0x1282    , -1, -1, 0x1285    , 0x1286    , 0x1287    , 0x1288    , -1 };
            @DataFormat.Hex04 public int[] size    = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0 };
            @DataFormat.Hex04 public int[] bitMask = new int[] { 0b11111111, 0b11111111, 0b10000011, -1, -1, 0b11001101, 0b00000111, 0b11111111, 0b11111111, -1 };
                              //                                                                             │
                              //                                                                             └→ be careful with this one
                              //                                                                                if the MCU shared the nRST and UPDI pin
            @DataFormat.Hex04 public int[] clrMask = null; // Optional 'always-clear-value' mask
            @DataFormat.Hex04 public int[] setMask = null; // Optional 'always-set-value'   mask
        }

        // NOTE : These default values apply to megaAVR 0-series and tinyAVR 0/1/2-series devices
        public static class MemoryLockBits implements Serializable {
            @DataFormat.Hex04 public int  address    = 0x128A;
                              public int  size       = 1;
            @DataFormat.Hex02 public int  bitMask    = 0xFF;
            @DataFormat.Hex08 public long bitMaskExt = -1;     // Optional bitmask for combined bytes - applies only when 'size' > 1
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final MemoryAVRBase      memoryAVRBase      = new MemoryAVRBase     ();
        public final MemorySignature    memorySignature    = new MemorySignature   ();
        public final MemorySignatureExt memorySignatureExt = new MemorySignatureExt();
        public final MemoryFlash        memoryFlash        = new MemoryFlash       ();
        public final MemoryEEPROM       memoryEEPROM       = new MemoryEEPROM      ();
        public final MemoryFuse         memoryFuse         = new MemoryFuse        ();
        public final MemoryLockBits     memoryLockBits     = new MemoryLockBits    ();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean addrIsFlash(final int address)
        { return (address >= memoryFlash.address) && (address < memoryFlash.address +  memoryFlash.totalSize); }

        public boolean addrIsEEPROM(final int address)
        { return (address >= memoryEEPROM.address) && (address < memoryEEPROM.address +  memoryEEPROM.totalSize); }

        public boolean addrIsFuseOrLockBits(final int address)
        {
            for(int a : memoryFuse.address) {
                if(a >= 0 && a == address) return true;
            }

            return (memoryLockBits.address == address);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'ATmega*()' functions ??? #####


public static Config ATmega4808()
{
    final Config config = new Config();

    config.memoryFlash.address    = 0x4000;
    config.memoryFlash.totalSize  = 49152;
    config.memoryFlash.pageSize   =   128;
    config.memoryFlash.numPages   =   384;

    //                                          0           1           2           3   4   5           6           7           8           9
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     append      bootend
    config.memoryFuse.bitMask[5]  =                                                         0b11001001                                          ;
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b01111100, -1, -1, 0b00110110, 0b11111000, 0b00000000, 0b00000000, -1 };

    config.memoryEEPROM.totalSize = 256;
    config.memoryEEPROM.pageSize  =  64;
    config.memoryEEPROM.numPages  =   4;

    return config;
}

public static Config ATmega4809        () { return ATmega4808(); }
public static Config ThinaryNanoEvery  () { return ATmega4808(); }

public static Config ArduinoNanoEvery  () { return ATmega4809(); }
public static Config ArduinoUnoWiFiR2  () { return ATmega4809(); }
public static Config ArduinoUnoWiFiRev2() { return ATmega4809(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
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
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!


public static Config _AVR_Dx_Ex_Sx_generic(final int memorySignature_address)
{
    final Config config = new Config();

    if(memorySignature_address >= 0) config.memorySignature.address = memorySignature_address;

    config.memorySignatureExt.address_SerNum       = config.memorySignature.address + 0x10;
    config.memorySignatureExt.size_SerNum          = 16;

    config.memorySignatureExt.address_OscCal16     = -1;
    config.memorySignatureExt.bitMask_OscCal16     =  0;
    config.memorySignatureExt.address_OscCal16TCal = -1;
    config.memorySignatureExt.bitMask_OscCal16TCal =  0;

    config.memorySignatureExt.address_OscCal20     = -1;
    config.memorySignatureExt.bitMask_OscCal20     =  0;
    config.memorySignatureExt.address_OscCal20TCal = -1;
    config.memorySignatureExt.bitMask_OscCal20TCal =  0;

    config.memorySignatureExt.address_Osc16Err3V   = -1;
    config.memorySignatureExt.bitMask_Osc16Err3V   =  0;
    config.memorySignatureExt.address_Osc16Err5V   =  0;
    config.memorySignatureExt.bitMask_Osc16Err5V   =  0;

    config.memorySignatureExt.address_Osc20Err3V   = -1;
    config.memorySignatureExt.bitMask_Osc20Err3V   =  0;
    config.memorySignatureExt.address_Osc20Err5V   = -1;
    config.memorySignatureExt.bitMask_Osc20Err5V   =  0;

    config.memorySignatureExt.bitSize_TempSense    = 16;
    config.memorySignatureExt.address_TempSense0   = config.memorySignature.address + 0x04;
    config.memorySignatureExt.bitMask_TempSense0   = 0xFFFF;
    config.memorySignatureExt.address_TempSense1   = config.memorySignature.address + 0x06;
    config.memorySignatureExt.bitMask_TempSense1   = 0xFFFF;

    config.memoryFlash.address       = 0x800000;

    config.memoryLockBits.address    = 0x1040;
    config.memoryLockBits.size       = 4;
    config.memoryLockBits.bitMaskExt = 0xFFFFFFFFL;

    return config;
}

public static Config _AVR_Dx_Ex_Sx_generic()
{ return _AVR_Dx_Ex_Sx_generic(-1); }

/*
 * ######################################## !!! WARNING !!! ########################################
 *
 * On AVR DA, DU, EB, and Sx series, UPDI will be permanently disable if:
 *     # FUSE.PDICFG[15:0] is set to 0xB452, and
 *     # LOCK[31:0] is written with any value other than 0x5CC5C55C
 *
 * This configuration is IRREVERSIBLE!
 *
 * ######################################## !!! WARNING !!! ########################################
 */

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_DA_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9   A           B
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize        pdicfg0     pdicfg1
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, 0x105A    , 0x105B     };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, 1         , 1          };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001111, -1, -1, 0b11101101, 0b00000111, 0b11111111, 0b11111111, -1, 0b11110011, 0b11111111 };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b11110000, -1, -1, 0b00010010, 0b11111000, 0b00000000, 0b00000000, -1, 0b00001100, 0b00000000 };
    //                                                                                                                                          │           │
    //                                                                                                        be VERY careful with these ones ←─┴───────────┘

    /*
    // ##### ??? TODO : Disable this feature because it CAN BE EXTREMELY dangerous on this MCU ??? #####
    config.memoryLockBits.address = 0;
    config.memoryLockBits.size    = 0;
    config.memoryLockBits.bitMask = 0;
    //*/

    return config;
}

public static Config AVR32DA()
{
    final Config config = _AVR_DA_generic();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize = 512;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 512;

    return config;
}

public static Config AVR64DA()
{
    final Config config = AVR32DA();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

public static Config AVR128DA()
{
    final Config config = AVR32DA();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    256;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_DB_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001111, -1, -1, 0b11101001, 0b00011111, 0b11111111, 0b11111111, -1, };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b11110000, -1, -1, 0b00010110, 0b11100000, 0b00000000, 0b00000000, -1, };

    return config;
}

public static Config AVR32DB()
{
    final Config config = _AVR_DB_generic();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize = 512;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 512;

    return config;
}

public static Config AVR64DB()
{
    final Config config = AVR32DB();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

public static Config AVR128DB()
{
    final Config config = AVR32DB();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    256;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_DD_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001111, -1, -1, 0b11111001, 0b00011111, 0b11111111, 0b11111111, -1, };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b11110000, -1, -1, 0b00000110, 0b11100000, 0b00000000, 0b00000000, -1, };
    //                                                                                      │
    //                                                                                      └→ be careful with this one

    return config;
}

public static Config AVR16DD()
{
    final Config config = _AVR_DD_generic();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    32;

    config.memoryEEPROM.totalSize = 256;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 256;

    return config;
}

public static Config AVR32DD()
{
    final Config config = AVR16DD();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =    64;

    return config;
}

public static Config AVR64DD()
{
    final Config config = AVR16DD();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_DU_generic()
{
    // NOTE : AVR DU MCUs use a different 'Config.memorySignature.address'
    final Config config = _AVR_Dx_Ex_Sx_generic(0x1080);

    //                                          0           1           2           3   4   5           6           7           8           9   A           B
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize        pdicfg0     pdicfg1
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, 0x105A    , 0x105B     };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, 1         , 1          };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001111, -1, -1, 0b11111011, 0b00001111, 0b11111111, 0b11111111, -1, 0b11110011, 0b11111111 };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, 0b11110000, -1, -1, 0b00000100, 0b11110000, 0b00000000, 0b00000000, -1, 0b00001100, 0b00000000 };
    //                                                                                      │                                                   │           │
    //                                                          be careful with this one ←──┘                 be VERY careful with these ones ←─┴───────────┘

    /*
    // ##### ??? TODO : Disable this feature because it CAN BE EXTREMELY dangerous on this MCU ??? #####
    config.memoryLockBits.address = 0;
    config.memoryLockBits.size    = 0;
    config.memoryLockBits.bitMask = 0;
    //*/

    return config;
}

public static Config AVR16DU()
{
    final Config config = _AVR_DU_generic();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    32;

    config.memoryEEPROM.totalSize = 256;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 256;

    return config;
}

public static Config AVR32DU()
{
    final Config config = AVR16DU();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =    64;

    return config;
}

public static Config AVR64DU()
{
    final Config config = AVR16DU();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_EA_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1 };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0 };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001000, -1, -1, 0b11111001, 0b00000111, 0b11111111, 0b11111111, -1 };
    config.memoryFuse.setMask     = new int[] { 0b00000000, 0b00000000, 0b11110111, -1, -1, 0b00000110, 0b11111000, 0b00000000, 0b00000000, -1 };
    //                                                                                      │
    //                                                                                      └→ be careful with this one

    return config;
}

public static Config AVR16EA()
{
    final Config config = _AVR_EA_generic();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =    64;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize = 512;
    config.memoryEEPROM.pageSize  =   8;
    config.memoryEEPROM.numPages  =  64;

    return config;
}

public static Config AVR32EA()
{
    final Config config = AVR16EA();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   512;

    return config;
}

public static Config AVR64EA()
{
    final Config config = AVR16EA();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   128;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_EB_generic()
{
    final Config config = _AVR_Dx_Ex_Sx_generic();

    //                                          0           1           2           3   4   5           6           7           8           9   A           B
    //                                          wdtcfg      bodcfg      osccfg              syscfg0     syscfg1     codesize    bootsize        pdicfg0     pdicfg1
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , 0x1052    , -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, 0x105A    , 0x105B     };
    config.memoryFuse.size        = new int[] { 1         , 1         , 1         ,  0,  0, 1         , 1         , 1         , 1         ,  0, 1         , 1          };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, 0b00001000, -1, -1, 0b11111011, 0b00000111, 0b11111111, 0b11111111, -1, 0b11110011, 0b11111111 };
    config.memoryFuse.setMask     = new int[] { 0b00000000, 0b00000000, 0b11110111, -1, -1, 0b00000100, 0b11111000, 0b00000000, 0b00000000, -1, 0b00001100, 0b00000000 };
    //                                                                                      │                                                   │           │
    //                                                          be careful with this one ←──┘                 be VERY careful with these ones ←─┴───────────┘

    /*
    // ##### ??? TODO : Disable this feature because it CAN BE EXTREMELY dangerous on this MCU ??? #####
    config.memoryLockBits.address = 0;
    config.memoryLockBits.size    = 0;
    config.memoryLockBits.bitMask = 0;
    //*/

    return config;
}

public static Config AVR16EB()
{
    final Config config = _AVR_EB_generic();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =    64;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize = 512;
    config.memoryEEPROM.pageSize  =   8; // ##### !!! TODO : VERIFY !!! #####
    config.memoryEEPROM.numPages  =  64;

    return config;
}

public static Config AVR32EB()
{
    final Config config = AVR16EB();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   512;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config _AVR_SD_generic()
{
    // NOTE : AVR Sx MCUs use a different 'Config.memorySignature.address'
    final Config config = _AVR_Dx_Ex_Sx_generic(0x1080);

    //                                          0           1           2   3   4   5           6           7           8           9   A           B
    //                                          wdtcfg      bodcfg                  syscfg0     syscfg1     codesize    bootsize        pdicfg0     pdicfg1
    config.memoryFuse.address     = new int[] { 0x1050    , 0x1051    , -1, -1, -1, 0x1055    , 0x1056    , 0x1057    , 0x1058    , -1, 0x105A    , 0x105B     };
    config.memoryFuse.size        = new int[] { 1         , 1         ,  0,  0,  0, 1         , 1         , 1         , 1         ,  0, 1         , 1          };
    config.memoryFuse.bitMask     = new int[] { 0b11111111, 0b11111111, -1, -1, -1, 0b11000011, 0b11011111, 0b11111111, 0b11111111, -1, 0b11110011, 0b11111111 };
    config.memoryFuse.clrMask     = new int[] { 0b00000000, 0b00000000, -1, -1, -1, 0b00111100, 0b00100000, 0b00000000, 0b00000000, -1, 0b00001100, 0b00000000 };
    //                                                                                                                                  │           │
    //                                                                                                be VERY careful with these ones ←─┴───────────┘

    /*
    // ##### ??? TODO : Disable this feature because it CAN BE EXTREMELY dangerous on this MCU ??? #####
    config.memoryLockBits.address = 0;
    config.memoryLockBits.size    = 0;
    config.memoryLockBits.bitMask = 0;
    //*/

    return config;
}

public static Config AVR32SD()
{
    final Config config = _AVR_SD_generic();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.pageSize   =   512;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize = 256;
    config.memoryEEPROM.pageSize  =   1; // NOTE : The EEPROM in this MCU series is not paged
    config.memoryEEPROM.numPages  = 256;

    return config;
}

public static Config AVR64SD()
{
    final Config config = AVR32SD();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.numPages   =   128;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // UPDI PHY characters
    private static final int UPDI_PHY_BREAK = 0x00;
    private static final int UPDI_PHY_SYNCH = 0x55;
    private static final int UPDI_PHY_ACK   = 0x40;

    // UPDI instructions
    private static int UPDI_CMD_LDS   (final int a, final int b ) { return 0x00 | ( (a << 2) & 0x0C ) | (b  & 0x03); } // 16-bit or 24-bit address
    private static int UPDI_CMD_STS   (final int a, final int b ) { return 0x40 | ( (a << 2) & 0x0C ) | (b  & 0x03); } // 16-bit or 24-bit address
    private static int UPDI_CMD_LD    (final int p, final int ab) { return 0x20 | ( (p << 2) & 0x0C ) | (ab & 0x03); }
    private static int UPDI_CMD_ST    (final int p, final int ab) { return 0x60 | ( (p << 2) & 0x0C ) | (ab & 0x03); } // 16-bit or 24-bit address for UPDI_PTR_ADDRESS
    private static int UPDI_CMD_LDCS  (final int a              ) { return 0x80 | (  a       & 0x0F )              ; }
    private static int UPDI_CMD_STCS  (final int a              ) { return 0xC0 | (  a       & 0x0F )              ; }
    private static int UPDI_CMD_REPEAT(             final int b ) { return 0xA0                       | (b  & 0x03); }
    private static int UPDI_CMD_KEY   (final int s, final int c ) { return 0xE0 | ( (s << 2) & 0x04 ) | (c  & 0x03); }

    private static final int   UPDI_ADDRESS_08      = 0x00; // Address size (a)
    private static final int   UPDI_ADDRESS_16      = 0x01; // ---
    private static final int   UPDI_ADDRESS_24      = 0x02; // ---

    private static final int   UPDI_DATA_08         = 0x00; // Data size (b)
    private static final int   UPDI_DATA_16         = 0x01; // ---
    private static final int   UPDI_DATA_24         = 0x02; // ---

    private static final int   UPDI_PTR             = 0x00; // Pointer access (p)
    private static final int   UPDI_PTR_INC         = 0x01; // ---
    private static final int   UPDI_PTR_ADDRESS     = 0x02; // ---

    private static final int   UPDI_SEND_KEY        = 0x00; // System information block selector (s)
    private static final int   UPDI_RECEIVE_SIB     = 0x01; // ---

    private static final int   UPDI_KEY_064         = 0x00; // Key size (c)
    private static final int   UPDI_KEY_128         = 0x01; // ---
    private static final int   UPDI_KEY_256         = 0x02; // ---

    private static final int   UPDI_SIB_08B         = 0x00; // SIB size (c)
    private static final int   UPDI_SIB_16B         = 0x01; // ---
    private static final int   UPDI_SIB_32B         = 0x02; // ---

    private static final int   UPDI_MAX_REPEAT_SIZE = 0x01FF;

    private static final int[] KEY_NVMPROG          = new int[] { 0x20, 0x67, 0x6F, 0x72, 0x50, 0x4D, 0x56, 0x4E }; // "NVMProg " in reverse
    private static final int[] KEY_CHIP_ERASE       = new int[] { 0x65, 0x73, 0x61, 0x72, 0x45, 0x4D, 0x56, 0x4E }; // "NVMErase" in reverse
    private static final int[] KEY_USERROW_WRITE    = new int[] { 0x65, 0x74, 0x26, 0x73, 0x55, 0x4D, 0x56, 0x4E }; // "NVMUs&te" in reverse

    // UPDI registers
    private static final int UPDI_CS_STATUSA     = 0x00;
    private static final int UPDI_CS_STATUSB     = 0x01;
    private static final int UPDI_CS_CTRLA       = 0x02;
    private static final int UPDI_CS_CTRLB       = 0x03;
    private static final int UPDI_ASI_KEY_STATUS = 0x07;
    private static final int UPDI_ASI_RESET_REQ  = 0x08;
    private static final int UPDI_ASI_CTRLA      = 0x09;
    private static final int UPDI_ASI_SYS_CTRLA  = 0x0A;
    private static final int UPDI_ASI_SYS_STATUS = 0x0B;
    private static final int UPDI_ASI_CRC_STATUS = 0x0C;

    // STATUSA bits
    private static final int STATUSA_UPDIREV_MASK = 0xF0;

    // STATUSB bits
    private static final int STATUSB_PSEIG_MASK = 0x07;

    // CTRLA bits
    private static final int CTRLA_IBDLY      = 0x80;
    private static final int CTRLA_PARD       = 0x20;
    private static final int CTRLA_DTD        = 0x10;
    private static final int CTRLA_RSD        = 0x08;
    private static final int CTRLA_GTVAL_2    = 0x04;
    private static final int CTRLA_GTVAL_1    = 0x02;
    private static final int CTRLA_GTVAL_0    = 0x00;
    private static final int CTRLA_GTVAL_MASK = CTRLA_GTVAL_2 | CTRLA_GTVAL_1 | CTRLA_GTVAL_0;
    private static final int CTRLA_GTVAL_128B = 0x00;
    private static final int CTRLA_GTVAL_064B = 0x01;
    private static final int CTRLA_GTVAL_032B = 0x02;
    private static final int CTRLA_GTVAL_016B = 0x03;
    private static final int CTRLA_GTVAL_008B = 0x04;
    private static final int CTRLA_GTVAL_004B = 0x05;
    private static final int CTRLA_GTVAL_002B = 0x06;
    private static final int CTRLA_GTVAL_000B = 0x07; // NOTE : According to the datasheet, it is not recommended to use this value

    // CTRLB bits
    private static final int CTRLB_NACKDIS  = 0x10;
    private static final int CTRLB_CCDETDIS = 0x08;
    private static final int CTRLB_UPDIDIS  = 0x04;

    // ASI_KEY_STATUS bits
    private static final int ASI_KEY_STATUS_UROWWRITE = 0x20;
    private static final int ASI_KEY_STATUS_NVMPROG   = 0x10;
    private static final int ASI_KEY_STATUS_CHIPERASE = 0x08;

    // ASI_RESET_REQ bits
    private static final int ASI_RESET_REQ_RSTREQ = 0x59;

    // ASI_CTRLA bits
    private static final int ASI_CTRLA_UPDICLKSEL_1    = 0x02;
    private static final int ASI_CTRLA_UPDICLKSEL_0    = 0x01;
    private static final int ASI_CTRLA_UPDICLKSEL_MASK = ASI_CTRLA_UPDICLKSEL_1 | ASI_CTRLA_UPDICLKSEL_0;
    private static final int ASI_CTRLA_UPDICLKSEL_32M  = 0x00;
    private static final int ASI_CTRLA_UPDICLKSEL_16M  = 0x01;
    private static final int ASI_CTRLA_UPDICLKSEL_08M  = 0x02;
    private static final int ASI_CTRLA_UPDICLKSEL_04M  = 0x03;

    // ASI_SYS_CTRLA bits
    private static final int ASI_SYS_CTRLA_UROWWRITE_FINAL = 0x02;
    private static final int ASI_SYS_CTRLA_CLKREQ          = 0x01;

    // ASI_SYS_STATUS bits
    private static final int ASI_SYS_STATUS_RSTSYS     = 0x20;
    private static final int ASI_SYS_STATUS_INSLEEP    = 0x10;
    private static final int ASI_SYS_STATUS_NVMPROG    = 0x08;
    private static final int ASI_SYS_STATUS_UROWPROG   = 0x04;
    private static final int ASI_SYS_STATUS_LOCKSTATUS = 0x01;

    // ASI_CRC_STATUS bits
    private static final int ASI_CRC_STATUS_MASK                = 0x07;
    private static final int ASI_CRC_STATUS_NOT_ENABLED         = 0x00;
    private static final int ASI_CRC_STATUS_ENABLED_BUSY        = 0x01;
    private static final int ASI_CRC_STATUS_ENABLED_DONE_OK     = 0x02;
    private static final int ASI_CRC_STATUS_ENABLED_DONE_FAILED = 0x04;

    // ##### !!! TODO : Verify that NVM commands work properly in all versions !!! #####

    // NVM statuses (specified in version selectors)
    private static final int UPDI_NVM0xxxxx_STATUS_UPDI_WRITE_ERROR = 0x04; // 0
    private static final int UPDI_NVMx2xxxx_STATUS_UPDI_WRITE_ERROR = 0x30; //   2
    private static final int UPDI_NVMxx3456_STATUS_UPDI_WRITE_ERROR = 0x70; //     3 4 5 6

    private static final int UPDI_NVM023x5x_STATUS_EEPROM_BUSY      = 0x02; // 0 2 3   5
    private static final int UPDI_NVMxxx4x6_STATUS_EEPROM_BUSY      = 0x01; //       4   6

    private static final int UPDI_NVM023x5x_STATUS_FLASH_BUSY       = 0x01; // 0 2 3   5
    private static final int UPDI_NVMxxx4x6_STATUS_FLASH_BUSY       = 0x02; //       4   6

    // NVM controller registers (specified in version ranges)
    private static final int UPDI_NVM06CTRL_CTRLA    = 0x00; // 0 2 3 4 5 6
    private static final int UPDI_NVM06CTRL_CTRLB    = 0x01; // 0 2 3 4 5 6
    private static final int UPDI_NVM46CTRL_CTRLC    = 0x02; //       4 5 6
    private static final int UPDI_NVM02CTRL_STATUS   = 0x02; // 0 2
    private static final int UPDI_NVM35CTRL_STATUS   = 0x06; //     3 4 5
    private static final int UPDI_NVM66CTRL_STATUS   = 0x07; //           6

    private static final int UPDI_NVM02CTRL_INTCTRL  = 0x03; // 0 2
    private static final int UPDI_NVM36CTRL_INTCTRL  = 0x04; //     3 4 5 6 ; INTCTRLA  in v6
    private static final int UPDI_NVM02CTRL_INTFLAGS = 0x04; // 0 2
    private static final int UPDI_NVM36CTRL_INTFLAGS = 0x05; //     3 4 5 6 ; INTFLAGSA in v6
    private static final int UPDI_NVM02CTRL_DATAL    = 0x06; // 0 2
    private static final int UPDI_NVM36CTRL_DATAL    = 0x08; //     3 4 5 6 ; DATA0     in v6
    private static final int UPDI_NVM02CTRL_DATAH    = 0x07; // 0 2
    private static final int UPDI_NVM36CTRL_DATAH    = 0x09; //     3 4 5 6 ; DATA1     in v6
    private static final int UPDI_NVM66CTRL_DATAE    = 0x0A; //           6 ; DATA2     in v6

    private static final int UPDI_NVM02CTRL_ADDRL    = 0x08; // 0 2
    private static final int UPDI_NVM36CTRL_ADDRL    = 0x0C; //     3 4 5 6 ; ADDR0     in v2+
    private static final int UPDI_NVM02CTRL_ADDRH    = 0x09; // 0 2
    private static final int UPDI_NVM36CTRL_ADDRH    = 0x0D; //     3 4 5 6 ; ADDR1     in v2+

    private static final int UPDI_NVM22CTRL_ADDR2    = 0x0A; //   2
    private static final int UPDI_NVM36CTRL_ADDR2    = 0x0E; //     3 4 5 6
    private static final int UPDI_NVM22CTRL_ADDR3    = 0x0B; //   2
    private static final int UPDI_NVM36CTRL_ADDR3    = 0x0F; //     3 4 5 6

    // CTRLA commands (specified in individual versions)
    private static final int UPDI_NVM0CTRL_CTRLA_NOP                      = 0x00;
    private static final int UPDI_NVM0CTRL_CTRLA_WRITE_PAGE               = 0x01;
    private static final int UPDI_NVM0CTRL_CTRLA_ERASE_PAGE               = 0x02;
    private static final int UPDI_NVM0CTRL_CTRLA_ERASE_WRITE_PAGE         = 0x03;
    private static final int UPDI_NVM0CTRL_CTRLA_PAGE_BUFFER_CLEAR        = 0x04;
    private static final int UPDI_NVM0CTRL_CTRLA_CHIP_ERASE               = 0x05;
    private static final int UPDI_NVM0CTRL_CTRLA_ERASE_EEPROM             = 0x06;
    private static final int UPDI_NVM0CTRL_CTRLA_WRITE_FUSE               = 0x07;

    private static final int UPDI_NVM2CTRL_CTRLA_NOCMD                    = 0x00;
    private static final int UPDI_NVM2CTRL_CTRLA_NOOP                     = 0x01;
    private static final int UPDI_NVM2CTRL_CTRLA_FLASH_WRITE              = 0x02;
    private static final int UPDI_NVM2CTRL_CTRLA_FLASH_PAGE_ERASE         = 0x08;
    private static final int UPDI_NVM2CTRL_CTRLA_EEPROM_WRITE             = 0x12;
    private static final int UPDI_NVM2CTRL_CTRLA_EEPROM_ERASE_WRITE       = 0x13;
    private static final int UPDI_NVM2CTRL_CTRLA_EEPROM_BYTE_ERASE        = 0x18;
    private static final int UPDI_NVM2CTRL_CTRLA_CHIP_ERASE               = 0x20;
    private static final int UPDI_NVM2CTRL_CTRLA_EEPROM_ERASE             = 0x30;

    private static final int UPDI_NVM3CTRL_CTRLA_NOCMD                    = 0x00;
    private static final int UPDI_NVM3CTRL_CTRLA_NOOP                     = 0x01;
    private static final int UPDI_NVM3CTRL_CTRLA_FLASH_PAGE_WRITE         = 0x04;
    private static final int UPDI_NVM3CTRL_CTRLA_FLASH_PAGE_ERASE_WRITE   = 0x05;
    private static final int UPDI_NVM3CTRL_CTRLA_FLASH_PAGE_ERASE         = 0x08;
    private static final int UPDI_NVM3CTRL_CTRLA_FLASH_PAGE_BUFFER_CLEAR  = 0x0F;
    private static final int UPDI_NVM3CTRL_CTRLA_EEPROM_PAGE_WRITE        = 0x14;
    private static final int UPDI_NVM3CTRL_CTRLA_EEPROM_PAGE_ERASE_WRITE  = 0x15;
    private static final int UPDI_NVM3CTRL_CTRLA_EEPROM_PAGE_ERASE        = 0x17;
    private static final int UPDI_NVM3CTRL_CTRLA_EEPROM_PAGE_BUFFER_CLEAR = 0x1F;
    private static final int UPDI_NVM3CTRL_CTRLA_CHIP_ERASE               = 0x20;
    private static final int UPDI_NVM3CTRL_CTRLA_EEPROM_ERASE             = 0x30;

    private static final int UPDI_NVM4CTRL_CTRLA_NOCMD                    = 0x00;
    private static final int UPDI_NVM4CTRL_CTRLA_NOOP                     = 0x01;
    private static final int UPDI_NVM4CTRL_CTRLA_FLASH_WRITE              = 0x02;
    private static final int UPDI_NVM4CTRL_CTRLA_FLASH_PAGE_ERASE         = 0x08;
    private static final int UPDI_NVM4CTRL_CTRLA_EEPROM_WRITE             = 0x12;
    private static final int UPDI_NVM4CTRL_CTRLA_EEPROM_ERASE_WRITE       = 0x13;
    private static final int UPDI_NVM4CTRL_CTRLA_EEPROM_BYTE_ERASE        = 0x18;
    private static final int UPDI_NVM4CTRL_CTRLA_CHIP_ERASE               = 0x20;
    private static final int UPDI_NVM4CTRL_CTRLA_EEPROM_ERASE             = 0x30;

    private static final int UPDI_NVM5CTRL_CTRLA_NOCMD                    = 0x00;
    private static final int UPDI_NVM5CTRL_CTRLA_NOP                      = 0x01;
    private static final int UPDI_NVM5CTRL_CTRLA_FLASH_PAGE_WRITE         = 0x04;
    private static final int UPDI_NVM5CTRL_CTRLA_FLASH_PAGE_ERASE_WRITE   = 0x05;
    private static final int UPDI_NVM5CTRL_CTRLA_FLASH_PAGE_ERASE         = 0x08;
    private static final int UPDI_NVM5CTRL_CTRLA_FLASH_PAGE_BUFFER_CLEAR  = 0x0F;
    private static final int UPDI_NVM5CTRL_CTRLA_EEPROM_PAGE_WRITE        = 0x14;
    private static final int UPDI_NVM5CTRL_CTRLA_EEPROM_PAGE_ERASE_WRITE  = 0x15;
    private static final int UPDI_NVM5CTRL_CTRLA_EEPROM_PAGE_ERASE        = 0x17;
    private static final int UPDI_NVM5CTRL_CTRLA_EEPROM_PAGE_BUFFER_CLEAR = 0x1F;
    private static final int UPDI_NVM5CTRL_CTRLA_CHIP_ERASE               = 0x20;
    private static final int UPDI_NVM5CTRL_CTRLA_EEPROM_ERASE             = 0x30;

    private static final int UPDI_NVM6CTRL_CTRLA_NOCMD                    = 0x00;
    private static final int UPDI_NVM6CTRL_CTRLA_NOP                      = 0x01;
    private static final int UPDI_NVM6CTRL_CTRLA_FLASH_WRITE              = 0x02;
    private static final int UPDI_NVM6CTRL_CTRLA_FLASH_PAGE_ERASE         = 0x08;
    private static final int UPDI_NVM6CTRL_CTRLA_EEPROM_WRITE             = 0x12;
    private static final int UPDI_NVM6CTRL_CTRLA_EEPROM_ERASE_WRITE       = 0x13;
    private static final int UPDI_NVM6CTRL_CTRLA_EEPROM_BYTE_ERASE        = 0x18;
    private static final int UPDI_NVM6CTRL_CTRLA_CHIP_ERASE               = 0x20;
    private static final int UPDI_NVM6CTRL_CTRLA_EEPROM_ERASE             = 0x30;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private class DeviceDetails {

        public final int[] family           = new int[10];
        public final int[] nvm_version      = new int[ 4];
        public       int   nvm_rev          = -1;
        public final int[] ocd_version      = new int[ 4];
        public       int   dbg_osc_freq     = -1;

        public       int   dev_rev          = -1;
        public       int   updi_rev         = -1;

        public final int[] signature_bytes  = new int[ 3];
        public final int[] serial_number    = new int[16];

        public       int   osc_cal_16       = -1;
        public       int   osc_cal_16_tcal  = -1;
        public       int   osc_cal_20       = -1;
        public       int   osc_cal_20_tcal  = -1;
        public       int   osc_err_16_3v    = -1;
        public       int   osc_err_16_5v    = -1;
        public       int   osc_err_20_3v    = -1;
        public       int   osc_err_20_5v    = -1;
        public       int   temp_sense_cal_0 = -1;
        public       int   temp_sense_cal_1 = -1;

        public int nvmVer()
        { return (nvm_rev >= 0) ? nvm_rev : _refNVMRevision; }

        // These functions are specified in individual versions
        public boolean nvm0() { return nvmVer() == 0; } // megaAVR 0-Series   tinyAVR 0/1/2-Series
        public boolean nvm1() { return nvmVer() == 1; } // N/A
        public boolean nvm2() { return nvmVer() == 2; } // AVR DA   AVR DB?   AVR DD?
        public boolean nvm3() { return nvmVer() == 3; } //          AVR EA
        public boolean nvm4() { return nvmVer() == 4; } //          AVR DU
        public boolean nvm5() { return nvmVer() == 5; } //          AVR EB?
        public boolean nvm6() { return nvmVer() == 6; } //          AVR SD
                                                        //          AVR LA?

        // These functions are specified in version selectors
        public boolean nvm02    () { return nvm0() || nvm2()                                        ; }
        public boolean nvm023456() { return nvm0() || nvm2() || nvm3() || nvm4() || nvm5() || nvm6(); }
        public boolean nvm0235  () { return nvm0() || nvm2() || nvm3()           || nvm5()          ; }
        public boolean nvm035   () { return nvm0() ||           nvm3() ||           nvm5()          ; }
        public boolean nvm23456 () { return           nvm2() || nvm3() || nvm4() || nvm5() || nvm6(); }
        public boolean nvm246   () { return           nvm2() ||           nvm4()           || nvm6(); }
        public boolean nvm345   () { return                     nvm3() || nvm4() || nvm5()          ; }
        public boolean nvm3456  () { return                     nvm3() || nvm4() || nvm5() || nvm6(); }

        // Dump the values
        private void _printfValue(final PrintStream ps, final int digitCount, final int v)
        {
            if(v < 0) ps.printf("N/A");
            else      ps.printf("%0" + digitCount + "X ", v);
        }

        public void dump(final PrintStream ps, final int bitSize_TempSense)
        {

            ps.println("[DeviceDetails]\n");

            ps.print("    SIB.Family_ID        = "); for(final int v : family         ) { _printfValue(ps,  2, v               ); } ps.println();
            ps.print("    SIB.NVM_VERSION      = "); for(final int v : nvm_version    ) { _printfValue(ps,  2, v               ); } ps.println();
            ps.print("    SIB.NVM_REVISION     = ");                                      _printfValue(ps,  2, nvm_rev         );   ps.println();
            ps.print("    SIB.OCD_VERSION      = "); for(final int v : ocd_version    ) { _printfValue(ps,  2, v               ); } ps.println();
            ps.print("    SIB.DBG_OSC_FREQ     = ");                                      _printfValue(ps,  2, dbg_osc_freq    );   ps.println();

            ps.println();

            ps.print("    SYSCFG.REVID         = ");                                      _printfValue(ps,  2, dev_rev         );   ps.println();
            ps.print("    STATUSA.UPDIREV      = ");                                      _printfValue(ps,  2, updi_rev        );   ps.println();

            ps.println();

            ps.print("    SIGROW.DEVICEID      = "); for(final int v : signature_bytes) { _printfValue(ps,  2, v               ); } ps.println();
            ps.print("    SIGROW.SERNUM        = "); for(final int v : serial_number  ) { _printfValue(ps,  2, v               ); } ps.println();

            ps.println();

            ps.print("    SIGROW.OSCCAL16M0    = ");                                      _printfValue(ps,  2, osc_cal_16      );   ps.println();
            ps.print("    SIGROW.OSCCAL16MTCAL = ");                                      _printfValue(ps,  2, osc_cal_16_tcal );   ps.println();
            ps.print("    SIGROW.OSCCAL20M0    = ");                                      _printfValue(ps,  2, osc_cal_20      );   ps.println();
            ps.print("    SIGROW.OSCCAL20MTCAL = ");                                      _printfValue(ps,  2, osc_cal_20_tcal );   ps.println();

            ps.print("    SIGROW.OSC16ERR3V    = ");                                      _printfValue(ps,  2, osc_err_16_3v   );   ps.println();
            ps.print("    SIGROW.OSC16ERR5V    = ");                                      _printfValue(ps,  2, osc_err_16_5v   );   ps.println();
            ps.print("    SIGROW.OSC20ERR3V    = ");                                      _printfValue(ps,  2, osc_err_20_3v   );   ps.println();
            ps.print("    SIGROW.OSC20ERR5V    = ");                                      _printfValue(ps,  2, osc_err_20_5v   );   ps.println();

            final int dc = 2 * bitSize_TempSense / 8;
            ps.print("    SIGROW.TEMPSENSE0    = ");                                      _printfValue(ps, dc, temp_sense_cal_0);   ps.println();
            ps.print("    SIGROW.TEMPSENSE1    = ");                                      _printfValue(ps, dc, temp_sense_cal_1);   ps.println();

            ps.println();
        }

    } // class DeviceDetails

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int UPDI_WAIT_UNLOCKED_TIMEOUT_MS    =  1000;
    private static final int UPDI_WAIT_FLASH_READY_TIMEOUT_MS = 10000;

    private static final int UPDI_CTRLA_VALUE                 = CTRLA_IBDLY | CTRLA_GTVAL_016B; // Enable inter-byte delay and reduce the guard time value

    private final USB2GPIO      _usb2gpio;
    private final Config        _config;

    private       int           _refNVMRevision = -1;
    private       DeviceDetails _deviceDetails  = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final int[] _oneByte_rdBuff = new int[1];
    private final int[] _oneByte_wrBuff = new int[1];

    private boolean _updi_rx_ack()
    {
        if( !_usb2gpio.uartRx(_oneByte_rdBuff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);

        return _oneByte_rdBuff[0] == UPDI_PHY_ACK;
    }

    private boolean _updi_tx_rx_cmd_lds_addrXX_dataXX(final int address, final int[] data, final int cmdAddress, final int cmdData)
    {
        // Send the command
        final int[] buff = (cmdAddress == UPDI_ADDRESS_16)
                         ? new int[] { UPDI_PHY_SYNCH, UPDI_CMD_LDS(UPDI_ADDRESS_16, cmdData), (address & 0xFF), (address >> 8) & 0xFF                         }
                         : new int[] { UPDI_PHY_SYNCH, UPDI_CMD_LDS(UPDI_ADDRESS_24, cmdData), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF };

        if( !_usb2gpio.uartTx_discardSerialLoopback(buff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        // Receive the byte(s)
        return _usb2gpio.uartRx(data);
    }

    private boolean _updi_tx_rx_cmd_lds_addr16_data8(final int address, final int[] data)
    { return _updi_tx_rx_cmd_lds_addrXX_dataXX(address, data, UPDI_ADDRESS_16, UPDI_DATA_08); }

    private boolean _updi_tx_rx_cmd_lds_addr16_data16(final int address, final int[] data)
    { return _updi_tx_rx_cmd_lds_addrXX_dataXX(address, data, UPDI_ADDRESS_16, UPDI_DATA_16); }

    private boolean _updi_tx_rx_cmd_lds_addr24_data8(final int address, final int[] data)
    { return _updi_tx_rx_cmd_lds_addrXX_dataXX(address, data, UPDI_ADDRESS_24, UPDI_DATA_08); }

    private boolean _updi_tx_rx_cmd_lds_addr24_data16(final int address, final int[] data)
    { return _updi_tx_rx_cmd_lds_addrXX_dataXX(address, data, UPDI_ADDRESS_24, UPDI_DATA_16); }

    private boolean _updi_tx_rx_cmd_lds_addrNV_data8(final int address, final int[] data)
    {
             if( _deviceDetails.nvm0    () ) return _updi_tx_rx_cmd_lds_addr16_data8(address, data);
        else if( _deviceDetails.nvm23456() ) return _updi_tx_rx_cmd_lds_addr24_data8(address, data);
        else                                 return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName); // Error for now
    }

    private boolean _updi_tx_rx_cmd_lds_addrNV_data16(final int address, final int[] data)
    {
             if( _deviceDetails.nvm0    () ) return _updi_tx_rx_cmd_lds_addr16_data16(address, data);
        else if( _deviceDetails.nvm23456() ) return _updi_tx_rx_cmd_lds_addr24_data16(address, data);
        else                                 return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName); // Error for now
    }

    private boolean _updi_tx_cmd_sts_addrXX_dataXX(final int address, final int[] data, final int cmdAddress, final int cmdData)
    {
        // Send the command
        final int[] buff = (cmdAddress == UPDI_ADDRESS_16)
                         ? new int[] { UPDI_PHY_SYNCH, UPDI_CMD_STS(UPDI_ADDRESS_16, cmdData), (address & 0xFF), (address >> 8) & 0xFF                         }
                         : new int[] { UPDI_PHY_SYNCH, UPDI_CMD_STS(UPDI_ADDRESS_24, cmdData), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF };

        if( !_usb2gpio.uartTx_discardSerialLoopback(buff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        if( !_updi_rx_ack() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);

        // Send the byte(s)
        if(cmdData == UPDI_DATA_08) {
            for(int i = 0; i < data.length; ++i) {
                if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] { data[i] } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
                if( !_updi_rx_ack() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);
            }
        }
        // Send the word(s)
        else {
            for(int i = 0; i < data.length; i += 2) {
                if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] { data[i], data[i + 1] } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
                if( !_updi_rx_ack() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);
            }
        }

        // Done
        return true;
    }

    private boolean _updi_tx_cmd_sts_addr16_data8(final int address, final int[] data)
    { return _updi_tx_cmd_sts_addrXX_dataXX(address, data, UPDI_ADDRESS_16, UPDI_DATA_08); }

    private boolean _updi_tx_cmd_sts_addr16_data16(final int address, final int[] data)
    { return _updi_tx_cmd_sts_addrXX_dataXX(address, data, UPDI_ADDRESS_16, UPDI_DATA_16); }

    private boolean _updi_tx_cmd_sts_addr24_data8(final int address, final int[] data)
    { return _updi_tx_cmd_sts_addrXX_dataXX(address, data, UPDI_ADDRESS_24, UPDI_DATA_08); }

    private boolean _updi_tx_cmd_sts_addr24_data16(final int address, final int[] data)
    { return _updi_tx_cmd_sts_addrXX_dataXX(address, data, UPDI_ADDRESS_24, UPDI_DATA_16); }

    private boolean _updi_tx_cmd_sts_addrNV_data8(final int address, final int[] data)
    {
             if( _deviceDetails.nvm0    () ) return _updi_tx_cmd_sts_addr16_data8(address, data);
        else if( _deviceDetails.nvm23456() ) return _updi_tx_cmd_sts_addr24_data8(address, data);
        else                                 return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName); // Error for now
    }

    private boolean _updi_tx_cmd_sts_addrNV_data16(final int address, final int[] data)
    {
             if( _deviceDetails.nvm0    () ) return _updi_tx_cmd_sts_addr16_data16(address, data);
        else if( _deviceDetails.nvm23456() ) return _updi_tx_cmd_sts_addr24_data16(address, data);
        else                                 return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName); // Error for now
    }

    private boolean _updi_tx_rx_cmd_ld_ptr_inc_dataXX(final int[] data, final int cmdData)
    {
        // Send the command
        if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] {
            UPDI_PHY_SYNCH, UPDI_CMD_LD(UPDI_PTR_INC, cmdData)
        } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        // Receive the byte(s)
        return _usb2gpio.uartRx(data);
    }

    private boolean _updi_tx_rx_cmd_ld_ptr_inc_data8(final int[] data)
    { return _updi_tx_rx_cmd_ld_ptr_inc_dataXX(data, UPDI_DATA_08); }

    private boolean _updi_tx_rx_cmd_ld_ptr_inc_data16(final int[] data)
    { return _updi_tx_rx_cmd_ld_ptr_inc_dataXX(data, UPDI_DATA_16); }

    private boolean _updi_tx_cmd_st_ptr_addrXX(final int address, final int cmdAddress)
    {
        final int[] buff = (cmdAddress == UPDI_ADDRESS_16)
                         ? new int[] { UPDI_PHY_SYNCH, UPDI_CMD_ST(UPDI_PTR_ADDRESS, UPDI_ADDRESS_16), (address & 0xFF), (address >> 8) & 0xFF                         }
                         : new int[] { UPDI_PHY_SYNCH, UPDI_CMD_ST(UPDI_PTR_ADDRESS, UPDI_ADDRESS_24), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF };

        if( !_usb2gpio.uartTx_discardSerialLoopback(buff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        return _updi_rx_ack();
    }

    private boolean _updi_tx_cmd_st_ptr_addr16(final int address)
    { return _updi_tx_cmd_st_ptr_addrXX(address, UPDI_ADDRESS_16); }

    private boolean _updi_tx_cmd_st_ptr_addr24(final int address)
    { return _updi_tx_cmd_st_ptr_addrXX(address, UPDI_ADDRESS_24); }

    private boolean _updi_tx_cmd_st_ptr_addrNV(final int address)
    {
             if( _deviceDetails.nvm0    () ) return _updi_tx_cmd_st_ptr_addr16(address);
        else if( _deviceDetails.nvm23456() ) return _updi_tx_cmd_st_ptr_addr24(address);
        else                                 return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName); // Error for now
    }

    private boolean _updi_tx_cmd_st_ptr_inc_dataXX_noACK(final int[] data, final int cmdData)
    {
        // Disable ACKs
        if( !_updi_disableACKs() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_DisACKs, ProgClassName);

        // Send the command
        if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] { UPDI_PHY_SYNCH, UPDI_CMD_ST(UPDI_PTR_INC, cmdData) } ) ) {
            // Try to recover and re-enable ACKs
            if( _updiRecover() >= 0 ) {
                if( !_updi_enableACKs() ) USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_EnACKs, ProgClassName);
            }
            // Exit error
            return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
        }

        // Send the data
        if( !_usb2gpio.uartTx_discardSerialLoopback(data) ) {
            // Try to recover and re-enable ACKs
            if( _updiRecover() >= 0 ) {
                if( !_updi_enableACKs() ) USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_EnACKs, ProgClassName);
            }
            // Exit error
            return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
        }

        // Enable ACKs
        if( !_updi_enableACKs() ) {
            // Try to recover and re-enable ACKs
            if( _updiRecover() >= 0 ) {
                if( !_updi_enableACKs() ) USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_EnACKs, ProgClassName);
            }
            // Exit error
            return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }

        // Done
        return true;
    }

    private boolean _updi_tx_cmd_st_ptr_inc_dataXX_withACK(final int[] data, final int cmdData)
    {
        // Send the first part
        final int[] buff = (cmdData == UPDI_DATA_08)
                         ? new int[] { UPDI_PHY_SYNCH, UPDI_CMD_ST(UPDI_PTR_INC, cmdData), data[0]          }
                         : new int[] { UPDI_PHY_SYNCH, UPDI_CMD_ST(UPDI_PTR_INC, cmdData), data[0], data[1] };

        if( !_usb2gpio.uartTx_discardSerialLoopback(buff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        if( !_updi_rx_ack() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);

        // Send the remaining parts(s)
        if(cmdData == UPDI_DATA_08) {
            for(int i = 1; i < data.length; ++i) {
                if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] { data[i] } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
                if( !_updi_rx_ack() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);
            }
        }
        else {
            for(int i = 2; i < data.length; i += 2) {
                if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] { data[i], data[i + 1] } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
                if( !_updi_rx_ack() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);
            }
        }

        // Done
        return true;
    }

    private boolean _updi_tx_cmd_st_ptr_inc_data8(final int[] data, final boolean disableACKs)
    {
        return disableACKs ? _updi_tx_cmd_st_ptr_inc_dataXX_noACK  (data, UPDI_DATA_08)
                           : _updi_tx_cmd_st_ptr_inc_dataXX_withACK(data, UPDI_DATA_08);
    }

    private boolean _updi_tx_cmd_st_ptr_inc_data16(final int[] data, final boolean disableACKs)
    {
        return disableACKs ? _updi_tx_cmd_st_ptr_inc_dataXX_noACK  (data, UPDI_DATA_16)
                           : _updi_tx_cmd_st_ptr_inc_dataXX_withACK(data, UPDI_DATA_16);
    }

    private boolean _updi_tx_cmd_st_ptr_data8(final int data)
    {
        // Send the data
        if( !_usb2gpio.uartTx_discardSerialLoopback(
            new int[] { UPDI_PHY_SYNCH, UPDI_CMD_ST(UPDI_PTR, UPDI_DATA_08), data }
        ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        if( !_updi_rx_ack() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);

        // Done
        return true;
    }

    private int _updi_tx_rx_cmd_ldcs(final int address)
    {
        if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] { UPDI_PHY_SYNCH, UPDI_CMD_LDCS(address) } ) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
            return -1;
        }

        if( !_usb2gpio.uartRx(_oneByte_rdBuff) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Rx, ProgClassName);
            return -1;
        }

        return _oneByte_rdBuff[0];
    }

    private boolean _updi_tx_cmd_stcs(final int address, final int value)
    { return _usb2gpio.uartTx_discardSerialLoopback( new int[] { UPDI_PHY_SYNCH, UPDI_CMD_STCS(address), value } ); }

    private boolean _updi_tx_cmd_repeat8(final int repeat_)
    {
        final int repeat = repeat_ - 1;

        return _usb2gpio.uartTx_discardSerialLoopback( new int[] {
            UPDI_PHY_SYNCH, UPDI_CMD_REPEAT(UPDI_DATA_08), (repeat & 0xFF)
        } );
    }

    private boolean _updi_tx_cmd_repeat16(final int repeat_)
    {
        final int repeat = repeat_ - 1;

        return _usb2gpio.uartTx_discardSerialLoopback( new int[] {
            UPDI_PHY_SYNCH, UPDI_CMD_REPEAT(UPDI_DATA_16), (repeat & 0xFF), (repeat >> 8) & 0xFF
        } );
    }

    private boolean _updi_tx_cmd_key(final int[] keyBytes)
    {
        final int keySize = (keyBytes.length ==  8) ? UPDI_KEY_064
                          : (keyBytes.length == 16) ? UPDI_KEY_128
                          : (keyBytes.length == 32) ? UPDI_KEY_256
                          : -1;

        if(keySize < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_InvKeyZ, ProgClassName);

        if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] { UPDI_PHY_SYNCH, UPDI_CMD_KEY(UPDI_SEND_KEY, keySize) } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        return _usb2gpio.uartTx_discardSerialLoopback(keyBytes);
    }

    private int[] _updi_tx_rx_cmd_sib()
    {
        if( !_usb2gpio.uartTx_discardSerialLoopback( new int[] { UPDI_PHY_SYNCH, UPDI_CMD_KEY(UPDI_RECEIVE_SIB, UPDI_SIB_16B) } ) ) return null;

        final int[] sibBytes = new int[16];

        return _usb2gpio.uartRx(sibBytes) ? sibBytes : null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _updi_read_revision_number()
    {
        final int res = _updi_tx_rx_cmd_ldcs(UPDI_CS_STATUSA);
        if(res < 0) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);
            return -1;
        }

        return res & STATUSA_UPDIREV_MASK;
    }

    private boolean _updi_check()
    { return _updi_read_revision_number() != 0; }

    private boolean _updi_apply_reset()
    {
        // Apply reset
        if(true) {
            if( !_updi_tx_cmd_stcs(UPDI_ASI_RESET_REQ, ASI_RESET_REQ_RSTREQ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

            final int res = _updi_tx_rx_cmd_ldcs(UPDI_ASI_SYS_STATUS);
            if(res < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);

            if( (res & ASI_SYS_STATUS_RSTSYS) == 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Reset, ProgClassName);
        }

        // Delay for a while
        SysUtil.sleepMS(100);

        // Release reset
        for(int i = 0; i < 10; ++i) {
            if( !_updi_tx_cmd_stcs(UPDI_ASI_RESET_REQ, 0x00) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

            final int res = _updi_tx_rx_cmd_ldcs(UPDI_ASI_SYS_STATUS);
            if(res < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);

            if( (res & ASI_SYS_STATUS_RSTSYS) == 0 ) return true;
        }

        // Not done
        return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Reset, ProgClassName);
    }

    private boolean _updi_device_locked()
    {
        final int res = _updi_tx_rx_cmd_ldcs(UPDI_ASI_SYS_STATUS);
        if(res < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);

        return (res & ASI_SYS_STATUS_LOCKSTATUS) != 0;
    }

    private boolean _updi_wait_unlocked()
    {
        // Wait for unlock
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(UPDI_WAIT_UNLOCKED_TIMEOUT_MS);

        while(true) {

            final int res = _updi_tx_rx_cmd_ldcs(UPDI_ASI_SYS_STATUS);
            if(res < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Unlock, ProgClassName);

            if( (res & ASI_SYS_STATUS_LOCKSTATUS) == 0 ) return true;

            if( tms.timeout() ) break;

        } // while

        // Not done
        return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Unlock, ProgClassName);
    }

    private boolean _updi_nvmprog_enabled()
    {
        final int res = _updi_tx_rx_cmd_ldcs(UPDI_ASI_SYS_STATUS);
        if(res < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);

        return (res & ASI_SYS_STATUS_NVMPROG) != 0;
    }

    private boolean _updi_nvmprog_enabled_ext()
    {
        // NOTE : Some AVR series seem to have a slower response after unlocking before NVM programming is enabled;
        //        therefore, the status must be checked multiple times

        for(int i = 0; i < 10; ++i) {
            if( _updi_nvmprog_enabled() ) return true;
            SysUtil.sleepMS(10);
        }

        return false;
    }

    private boolean _updi_enable_nvmprog()
    {
        // Check if already enabled
        if( _updi_nvmprog_enabled_ext() ) {
            USB2GPIO.TansmitError.notifyError(Texts.ProgXXX_FailUPDI_EnProgD, ProgClassName);
            return true;
        }

        // Send the NVMPROG key
        if( !_updi_tx_cmd_key(KEY_NVMPROG) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        // Check the key status
        final int res = _updi_tx_rx_cmd_ldcs(UPDI_ASI_KEY_STATUS);
        if(res < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);

        if( (res & ASI_KEY_STATUS_NVMPROG) == 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_KeyNDec, ProgClassName);

        // Apply reset and wait for the device to unlock
        if( !_updi_apply_reset  () ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Reset   , ProgClassName);
        if( !_updi_wait_unlocked() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_WaitUlck, ProgClassName);

        // Done
        return _updi_nvmprog_enabled_ext();
    }

    private boolean _updi_unlock_device() // Use the erase key to perform a full erase (and thus unlock); it will also enable NVM programming
    {
        // Send the special unlock handshake if required and if the programmer supports it
        if( _deviceDetails.nvm6() && (_usb2gpio instanceof USB_GPIO) ) {
            // ##### !!! TODO : VERIFY - IS THIS THE CORRECT METHOD !!! #####
            // Initialize the SPI, as its SS line is used to drive the MCU's RESET pin
            _usb2gpio.spiEnd();
            if( !_usb2gpio.spiBegin(
                USB2GPIO.SPIMode._0, USB2GPIO.SSMode.ActiveLow, _usb2gpio.spiClkFreqToClkDiv(8000)
            ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSPI, ProgClassName);
            _usb2gpio.spiDeselectSlave();
            // (1) Drive the UPDI line low and hold it low
            _usb2gpio.rawSerialPort().flushIOBuffers();
            _usb2gpio.rawSerialPort().setBreak();
            // (2) Pull the RESET line low for the minimum reset pulse width and release it
            _usb2gpio.spiSelectSlave();
            SysUtil.sleepMS(100);
            _usb2gpio.spiDeselectSlave();
            _usb2gpio.spiEnd();
            // (3) Wait for at least 40 ms so the device can reach an initial state
            SysUtil.sleepMS(100);
            // (4) Release the UPDI line to its UART IDLE state
            _usb2gpio.rawSerialPort().clearBreak();
            // (5) Issue a negative pulse to the UPDI line
            _usb2gpio.rawSerialPort().setBreak();
            _usb2gpio.rawSerialPort().clearBreak();
            // (6) Send a CHIP ERASE key to unlock the device and poll for successful completion
            // NOTE : Implemented below
            // --- Steps 4 to 6 must be completed within a 64mS time window ---
        }

        // Send the CHIP_ERASE key
        if( !_updi_tx_cmd_key(KEY_CHIP_ERASE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

        final int res = _updi_tx_rx_cmd_ldcs(UPDI_ASI_KEY_STATUS);
        if(res < 0) return  USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);

        if( (res & ASI_KEY_STATUS_CHIPERASE) == 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_ChErase, ProgClassName);

        // Set flag
        _chipErased = true;

        // Enable NVM programming
        return _updi_enable_nvmprog();
    }

    private boolean _updi_disableACKs()
    { return _updi_tx_cmd_stcs(UPDI_CS_CTRLA, UPDI_CTRLA_VALUE | CTRLA_RSD); }

    private boolean _updi_enableACKs()
    { return _updi_tx_cmd_stcs(UPDI_CS_CTRLA, UPDI_CTRLA_VALUE); }

    private boolean _updi_disable()
    { return _updi_tx_cmd_stcs(UPDI_CS_CTRLB, CTRLB_UPDIDIS); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _updi_read_addrXX_dataXX(final int address, final int size, final int addrSize, final int dataSize)
    {
        // Check the size
        if(size > UPDI_MAX_REPEAT_SIZE + 1) return null;

        // Set the address pointer
             if(addrSize == 16) { if( !_updi_tx_cmd_st_ptr_addr16(address) ) return null; }
        else if(addrSize == 24) { if( !_updi_tx_cmd_st_ptr_addr24(address) ) return null; }
        else                    {                                            return null; }

        // Set the repeat counter as needed
        if(size > 1) {
            if( !_updi_tx_cmd_repeat16(size) ) return null;
        }

        // Read the data
        final int[] buff = new int[size * dataSize / 8];

             if(dataSize ==  8) { if( !_updi_tx_rx_cmd_ld_ptr_inc_data8 (buff) ) return null; }
        else if(dataSize == 16) { if( !_updi_tx_rx_cmd_ld_ptr_inc_data16(buff) ) return null; }
        else                    {                                                return null; }

        // Combine the bytes as needed
        final int[] rbuff = new int[size];

        if(dataSize == 16) {
            for(int i = 0; i < size; ++i) {
                final int ch1 = buff[i * 2 + 1];
                final int ch0 = buff[i * 2 + 0];
                rbuff[i] = (ch1 << 8) | ch0;
            }
        }
        else {
            System.arraycopy(buff, 0, rbuff, 0, buff.length);
        }

        // Done
        return rbuff;
    }

    private int[] _updi_read_addr16_data8(final int address, final int size)
    { return _updi_read_addrXX_dataXX(address, size, 16, 8); }

    private int[] _updi_read_addr24_data8(final int address, final int size)
    { return _updi_read_addrXX_dataXX(address, size, 24, 8); }

    private int[] _updi_read_addrNV_data8(final int address, final int size)
    {
             if( _deviceDetails.nvm0    () ) return _updi_read_addr16_data8(address, size);
        else if( _deviceDetails.nvm23456() ) return _updi_read_addr24_data8(address, size);
        else                                 return null; // Error for now
    }

    private int[] _updi_read_addr16_data16(final int address, final int size)
    { return _updi_read_addrXX_dataXX(address, size, 16, 16); }

    private int[] _updi_read_addr24_data16(final int address, final int size)
    { return _updi_read_addrXX_dataXX(address, size, 24, 16); }

    private int[] _updi_read_addrNV_data16(final int address, final int size)
    {
             if( _deviceDetails.nvm0    () ) return _updi_read_addr16_data16(address, size);
        else if( _deviceDetails.nvm23456() ) return _updi_read_addr24_data16(address, size);
        else                                 return null; // Error for now
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _updi_wait_nvmctl_ready()
    {
        // Wait until the NVM controller is ready
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(UPDI_WAIT_FLASH_READY_TIMEOUT_MS);

        while(true) {

            final int address  = _config.memoryAVRBase.NVM + (
                                       _deviceDetails.nvm02 () ? UPDI_NVM02CTRL_STATUS
                                     : _deviceDetails.nvm345() ? UPDI_NVM35CTRL_STATUS
                                     :                           UPDI_NVM66CTRL_STATUS
                                 );

            final int errorBit = _deviceDetails.nvm0() ? UPDI_NVM0xxxxx_STATUS_UPDI_WRITE_ERROR
                               : _deviceDetails.nvm2() ? UPDI_NVMx2xxxx_STATUS_UPDI_WRITE_ERROR
                               :                         UPDI_NVMxx3456_STATUS_UPDI_WRITE_ERROR;

            final int busyBit  = _deviceDetails.nvm035() ? (UPDI_NVM023x5x_STATUS_EEPROM_BUSY | UPDI_NVM023x5x_STATUS_FLASH_BUSY)
                               :                           (UPDI_NVMxxx4x6_STATUS_EEPROM_BUSY | UPDI_NVMxxx4x6_STATUS_FLASH_BUSY);

            if( !_updi_tx_rx_cmd_lds_addrNV_data8(address, _oneByte_rdBuff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);

            if( ( _oneByte_rdBuff[0] & errorBit ) != 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
            if( ( _oneByte_rdBuff[0] & busyBit  ) == 0 ) return true;

            if( tms.timeout() ) break;

        } // while

        // Not done
        return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);
    }

    private boolean _updi_exec_nvm_cmd(final int cmd)
    { return _updi_tx_cmd_sts_addrNV_data8(_config.memoryAVRBase.NVM + UPDI_NVM06CTRL_CTRLA, new int[] { cmd } ); }

    private boolean _updi_write_nvm(final int address, final byte[] data, final int offset, final int size, final boolean disableACKs)
    {
        // Wait until the NVM controller is ready
        if( !_updi_wait_nvmctl_ready() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);

        // Check if it is in the EEPROM address space
        final boolean isEEPROM = _config.addrIsEEPROM(address) || _config.addrIsFuseOrLockBits(address);

        // Clear the page buffer
        if( _deviceDetails.nvm0() ) {
            if( !_updi_exec_nvm_cmd(           UPDI_NVM0CTRL_CTRLA_PAGE_BUFFER_CLEAR                                                     ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm2() ) {
            if( !_updi_exec_nvm_cmd(isEEPROM ? UPDI_NVM2CTRL_CTRLA_EEPROM_ERASE_WRITE       : UPDI_NVM2CTRL_CTRLA_FLASH_WRITE            ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm3() ) {
            if( !_updi_exec_nvm_cmd(isEEPROM ? UPDI_NVM3CTRL_CTRLA_EEPROM_PAGE_BUFFER_CLEAR : UPDI_NVM3CTRL_CTRLA_FLASH_PAGE_BUFFER_CLEAR) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm4() ) {
            if( !_updi_exec_nvm_cmd(isEEPROM ? UPDI_NVM4CTRL_CTRLA_EEPROM_ERASE_WRITE       : UPDI_NVM4CTRL_CTRLA_FLASH_WRITE            ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm5() ) {
            if( !_updi_exec_nvm_cmd(isEEPROM ? UPDI_NVM5CTRL_CTRLA_EEPROM_PAGE_BUFFER_CLEAR : UPDI_NVM5CTRL_CTRLA_FLASH_PAGE_BUFFER_CLEAR) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm6() ) {
            if( !_updi_exec_nvm_cmd(isEEPROM ? UPDI_NVM6CTRL_CTRLA_EEPROM_ERASE_WRITE       : UPDI_NVM6CTRL_CTRLA_FLASH_WRITE            ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else {
            // Error for now
            return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName);
        }

        // Wait until the NVM controller is ready again
        if( !_updi_wait_nvmctl_ready() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);

        // Load the page buffer
        final int[] buff = USB2GPIO.ba2ia(data, offset, size);

        if(isEEPROM) {
            // NOTE : EEPROM memory is most likely written in byte mode
            if( !_updi_tx_cmd_st_ptr_addrNV   (address          ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
            if( !_updi_tx_cmd_repeat16        (buff.length      ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
            if( !_updi_tx_cmd_st_ptr_inc_data8(buff, disableACKs) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
        }
        else {
            // NOTE : Flash memory is always written in word mode (should be slightly faster than byte mode)
            if( !_updi_tx_cmd_st_ptr_addrNV    (address          ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
            if( !_updi_tx_cmd_repeat16         (buff.length / 2  ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
            if( !_updi_tx_cmd_st_ptr_inc_data16(buff, disableACKs) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);
        }

        if( !_updi_wait_nvmctl_ready() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);

        // Write the page
        if( _deviceDetails.nvm0() ) {
            if( !_updi_exec_nvm_cmd(isEEPROM ? UPDI_NVM0CTRL_CTRLA_ERASE_WRITE_PAGE        : UPDI_NVM0CTRL_CTRLA_WRITE_PAGE      ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm2() ) {
            if( !_updi_exec_nvm_cmd(           UPDI_NVM2CTRL_CTRLA_NOCMD                                                         ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm3() ) {
            if( !_updi_exec_nvm_cmd(isEEPROM ? UPDI_NVM3CTRL_CTRLA_EEPROM_PAGE_ERASE_WRITE : UPDI_NVM3CTRL_CTRLA_FLASH_PAGE_WRITE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm4() ) {
            if( !_updi_exec_nvm_cmd(           UPDI_NVM4CTRL_CTRLA_NOCMD                                                         ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm5() ) {
            if( !_updi_exec_nvm_cmd(isEEPROM ? UPDI_NVM5CTRL_CTRLA_EEPROM_PAGE_ERASE_WRITE : UPDI_NVM5CTRL_CTRLA_FLASH_PAGE_WRITE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm6() ) {
            if( !_updi_exec_nvm_cmd(           UPDI_NVM6CTRL_CTRLA_NOCMD                                                         ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else {
            // Error for now
            return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName);
        }

        // Wait until the NVM controller is ready again
        if( !_updi_wait_nvmctl_ready() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);

        // Done
        return true;
    }

    private int[] _updi_read_nvm(final int address, final int size)
    {
        if( _deviceDetails.nvm0() ) {
            return _updi_read_addrNV_data8(address, size);
        }

        else {
            final int[] buff = _updi_read_addr24_data16(address, size / 2);
            final int[] res  = new int[size];
            for(int i = 0; i < size; i += 2) {
                res[i + 0] = (buff[i / 2]     ) & 0xFF;
                res[i + 1] = (buff[i / 2] >> 8) & 0xFF;
            }
            return res;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private DeviceDetails _getDeviceDetails()
    {
        // If it is already available, simply return it
        if(_deviceDetails != null) return _deviceDetails;

        // Error if NVM programming is not enabled
        if( !_updi_nvmprog_enabled() ) return null;

        // Get the device details
        try {

            // Send the command and get the response
            final int[] res = _updi_tx_rx_cmd_sib();
            if(res == null) return null;

            // Parse the response
            _deviceDetails = new DeviceDetails();

            for(int i =  0; i <  7; ++i) _deviceDetails.family     [i     ] = res[ i];
            for(int i =  8; i < 11; ++i) _deviceDetails.nvm_version[i -  8] = res[ i];
            for(int i = 11; i < 14; ++i) _deviceDetails.ocd_version[i - 11] = res[ i];
                                         _deviceDetails.dbg_osc_freq        = res[15];
                                         _deviceDetails.nvm_rev             = _deviceDetails.nvm_version[2] -  ( (int) '0' );

            if(_refNVMRevision >= 0 && _refNVMRevision != _deviceDetails.nvm_rev) return null; // Verify that the values are the same

            // Read the signature
            final int[] sig = _updi_read_addrNV_data8(_config.memorySignature.address, _config.memorySignature.size);
            if(sig == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);

            for(int i = 0; i < 3; ++i) _deviceDetails.signature_bytes[i] = sig[i];

            // Read the device revision
            final int[] drv = _updi_read_addrNV_data8(_config.memoryAVRBase.SYSCFG, 1);
            if(drv == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);

            _deviceDetails.dev_rev = drv[0];

            // Read the UPDI revision
            _deviceDetails.updi_rev = _updi_read_revision_number();
            if(_deviceDetails.updi_rev < 0) USB2GPIO.TansmitError.throwTansmitError_readInvalidValue(Texts.ProgXXX_FailUPDI_RInvVal, ProgClassName);

            // Read the serial number
            if(_config.memorySignatureExt.address_SerNum >= 0) {
                final int[] snb = _updi_read_addrNV_data8(_config.memorySignatureExt.address_SerNum, _config.memorySignatureExt.size_SerNum);
                final int   ofs = _deviceDetails.serial_number.length - _config.memorySignatureExt.size_SerNum;
                if(snb == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                Arrays.fill(_deviceDetails.serial_number, 0);
                for(int i = 0; i < _config.memorySignatureExt.size_SerNum; ++i) _deviceDetails.serial_number[i + ofs] = snb[i];
            }

            // Read the oscillator calibration values
            if(_config.memorySignatureExt.address_OscCal16 >= 0) {
                final int[] val = _updi_read_addrNV_data8(_config.memorySignatureExt.address_OscCal16, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.osc_cal_16 = val[0] & _config.memorySignatureExt.bitMask_OscCal16;
            }

            if(_config.memorySignatureExt.address_OscCal16TCal >= 0) {
                final int[] val = _updi_read_addrNV_data8(_config.memorySignatureExt.address_OscCal16TCal, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.osc_cal_16_tcal = val[0] & _config.memorySignatureExt.bitMask_OscCal16TCal;
            }

            if(_config.memorySignatureExt.address_OscCal20 >= 0) {
                final int[] val = _updi_read_addrNV_data8(_config.memorySignatureExt.address_OscCal20, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.osc_cal_20 = val[0] & _config.memorySignatureExt.bitMask_OscCal20;
            }

            if(_config.memorySignatureExt.address_OscCal20TCal >= 0) {
                final int[] val = _updi_read_addrNV_data8(_config.memorySignatureExt.address_OscCal20TCal, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.osc_cal_20_tcal = val[0] & _config.memorySignatureExt.bitMask_OscCal20TCal;
            }

            // Read the oscillator calibration values
            if(_config.memorySignatureExt.address_Osc16Err3V >= 0) {
                final int[] val = _updi_read_addrNV_data8(_config.memorySignatureExt.address_Osc16Err3V, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.osc_err_16_3v = val[0] & _config.memorySignatureExt.bitMask_Osc16Err3V;
            }

            if(_config.memorySignatureExt.address_Osc16Err5V >= 0) {
                final int[] val = _updi_read_addrNV_data8(_config.memorySignatureExt.address_Osc16Err5V, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.osc_err_16_5v = val[0] & _config.memorySignatureExt.bitMask_Osc16Err5V;
            }

            if(_config.memorySignatureExt.address_Osc20Err3V >= 0) {
                final int[] val = _updi_read_addrNV_data8(_config.memorySignatureExt.address_Osc20Err3V, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.osc_err_20_3v = val[0] & _config.memorySignatureExt.bitMask_Osc20Err3V;
            }

            if(_config.memorySignatureExt.address_Osc20Err5V >= 0) {
                final int[] val = _updi_read_addrNV_data8(_config.memorySignatureExt.address_Osc20Err5V, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.osc_err_20_5v = val[0] & _config.memorySignatureExt.bitMask_Osc20Err5V;
            }

            // Read the tempereture sensor calibration values
            if(_config.memorySignatureExt.address_TempSense0 >= 0) {
                final int[] val = (_config.memorySignatureExt.bitSize_TempSense == 16)
                                ? _updi_read_addrNV_data16(_config.memorySignatureExt.address_TempSense0, 1)
                                : _updi_read_addrNV_data8 (_config.memorySignatureExt.address_TempSense0, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.temp_sense_cal_0 = val[0] & _config.memorySignatureExt.bitMask_TempSense0;
            }

            if(_config.memorySignatureExt.address_TempSense1 >= 0) {
                final int[] val = (_config.memorySignatureExt.bitSize_TempSense == 16)
                                ? _updi_read_addrNV_data16(_config.memorySignatureExt.address_TempSense1, 1)
                                : _updi_read_addrNV_data8 (_config.memorySignatureExt.address_TempSense1, 1);
                if(val == null) USB2GPIO.TansmitError.throwTansmitError_readValueFailed(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                _deviceDetails.temp_sense_cal_1 = val[0] & _config.memorySignatureExt.bitMask_TempSense1;
            }

        } // try
        catch(final USB2GPIO.TansmitError e) {
            // Clear the device details in case of error
            _deviceDetails = null;
            // Notify error
            USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_GetDevD, ProgClassName);
        }

        // Done
        return _deviceDetails;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int UPDI_SAFE_BAUDRATE       = 19200;
    private static final int UPDI_CONNECT_RETRY_COUNT =    16;
    private static final int UPDI_INIT_RETRY_COUNT    =     8;

    private boolean _inProgMode = false;
    private boolean _chipErased = false;
    private int     _updiClkSel = ASI_CTRLA_UPDICLKSEL_04M;

    public ProgUPDI(final USB2GPIO usb2gpio, final Config config) throws Exception
    {
        // Store the objects
        _usb2gpio = usb2gpio;
        _config   = config.deepClone();

        // Check the configuration values
        if(_config.memorySignature.address <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFSigAddr , ProgClassName);
        if(_config.memorySignature.size    <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFSigSize , ProgClassName);

        if(_config.memoryFlash.address   <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFAddress , ProgClassName);
        if(_config.memoryFlash.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFTotSize , ProgClassName);
        if(_config.memoryFlash.pageSize  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSize, ProgClassName);
        if(_config.memoryFlash.numPages  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFNumPages, ProgClassName);

        if(_config.memoryFlash.pageSize * _config.memoryFlash.numPages != _config.memoryFlash.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSpec, ProgClassName);

        if(_config.memoryEEPROM.address >= 0 || _config.memoryEEPROM.totalSize > 0) {
            if(_config.memoryEEPROM.address   <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEAddress , ProgClassName);
            if(_config.memoryEEPROM.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMETotSize , ProgClassName);
            if(_config.memoryEEPROM.pageSize  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEPageSize, ProgClassName);
            if(_config.memoryEEPROM.numPages  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMENumPages, ProgClassName);

            if(_config.memoryEEPROM.pageSize * _config.memoryEEPROM.numPages != _config.memoryEEPROM.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEPageSpec, ProgClassName);
        }

            if(_config.memoryFuse.size.length    != _config.memoryFuse.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFSizeL  , ProgClassName);
            if(_config.memoryFuse.bitMask.length != _config.memoryFuse.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFBitMskL, ProgClassName);
        if(_config.memoryFuse.clrMask != null) {
            if(_config.memoryFuse.clrMask.length != _config.memoryFuse.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFClrMskL, ProgClassName);
        }
        if(_config.memoryFuse.setMask != null) {
            if(_config.memoryFuse.setMask.length != _config.memoryFuse.address.length) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFSetMskL, ProgClassName);
        }
        for(int i = 0; i < _config.memoryFuse.address.length; ++i) {
                if(_config.memoryFuse.address[i] <  0) continue;
                if(_config.memoryFuse.address[i] == 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFAddress, ProgClassName);
                if(_config.memoryFuse.size   [i] != 1) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFSize   , ProgClassName);
                if(_config.memoryFuse.bitMask[i] <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFBitMask, ProgClassName);
            if(_config.memoryFuse.clrMask != null) {
                if(_config.memoryFuse.clrMask[i] <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFClrMask, ProgClassName);
            }
            if(_config.memoryFuse.setMask != null) {
                if(_config.memoryFuse.setMask[i] <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFSetMask, ProgClassName);
            }
        }

        if(_config.memoryLockBits.address != 0 || _config.memoryLockBits.size != 0 || _config.memoryLockBits.bitMask != 0) {
            if(_config.memoryLockBits.address <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMLBAddress, ProgClassName);
            if(_config.memoryLockBits.size    <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMLBSize   , ProgClassName);
            if(_config.memoryLockBits.bitMask <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMLBBitMask, ProgClassName);
            if(_config.memoryLockBits.size    >  1) {
            if(_config.memoryLockBits.size    >  7) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMLBSize   , ProgClassName);
               if(_config.memoryLockBits.bitMaskExt <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMLBBitMskX, ProgClassName);
            }
        }
    }

    public Config config()
    { return _config; }

    private boolean _updiBreak()
    {
        /*
         * Send UPDI_PHY_BREAK using the raw serial port object for more reliability.
         *
         * NOTE : # According to the datasheet, to ensure that a UPDI_PHY_BREAK is successfully received by the
         *          UPDI in all cases, the programmer must send two consecutive BREAK characters.
         *        # Assuming the slowest UPDI clock speed of 4MHz, the maximum length of the 8-bit SYNCH pattern
         *          value that can fit in 16 bits is 65535 * 250nS ~= 16.4mS per byte, resulting in approximately
         *          2.05mS per bit. This leads to a worst-case BREAK frame duration of 2.05mS * 12 bits ~= 24.6mS.
         */
        _usb2gpio.rawSerialPort().flushIOBuffers();

        _usb2gpio.rawSerialPort().setBreak      (); SysUtil.sleepMS(25);
        _usb2gpio.rawSerialPort().clearBreak    ();

        SysUtil.sleepMS(250); // Delay long enough to ensure the High-Voltage UPDI has completed its work

        _usb2gpio.rawSerialPort().setBreak      (); SysUtil.sleepMS(25);
        _usb2gpio.rawSerialPort().clearBreak    ();

        _usb2gpio.rawSerialPort().flushIOBuffers();

        // Wait for a while
        SysUtil.sleepMS(5);

        // Restore the UPDI clock frequency
        _updi_tx_cmd_stcs(UPDI_ASI_CTRLA, _updiClkSel);

        // Check if connected
        if( _updi_check() ) return true;

        // Wait for a while
        SysUtil.sleepMS(100);

        // Not connected
        return false;
    }

    private int _updiRecover()
    {
        // Save the baudrate
        final int baudrate = _usb2gpio.rawSerialPort().getBaudRate();

        // Set to the safe baudrate
        _usb2gpio.rawSerialPort().setBaudRate(UPDI_SAFE_BAUDRATE);

        // Recover UPDI - send UPDI_BREAK(s) and check if it is recovered
        int     errorSignature = -1;
        boolean updiConnected  = false;

        for(int retry = 0; retry < UPDI_CONNECT_RETRY_COUNT; ++retry) {
            if( _updiBreak() ) {
                errorSignature = _updi_tx_rx_cmd_ldcs(UPDI_CS_STATUSB);
                if(errorSignature >= 0) {
                    updiConnected = true;
                    break;
                }
            }
        }

        // Restore the baudrate
        _usb2gpio.rawSerialPort().setBaudRate(baudrate);

        // Notify error as needed
        if(!updiConnected) USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Recover, ProgClassName);

        // Return the result
        return updiConnected ? errorSignature : -1;
    }

    public boolean begin(final int baudrate)
    {
        // ##### ??? TODO : Add TX->RX test for UPDI ??? #####

        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flag
        _chipErased = false;

        // Enable mode
        if(_usb2gpio instanceof USB_GPIO) {
            if( !( (USB_GPIO) _usb2gpio ).pcf8574Enable_UPDI() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitPCF8574, ProgClassName);
        }

        // Initialize the UART using the safe baudrate
        for(int i = 0; i < 2; ++i) {
            // Use hardware UART mode
            if( _usb2gpio.uartSetImplMode(USB2GPIO.ImplMode.Hardware) ) {
                // Initialize the UART
                if( _usb2gpio.uartBegin(USB2GPIO.UXRTMode._8E2, UPDI_SAFE_BAUDRATE) ) {
                    break;
                }
                // Error initializing the UART
                else {
                    // Exit if this is the 2nd initialization attempt
                    if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitUART, ProgClassName);
                }
            }
            // Error selecting hardware UART mode
            else {
                // Exit if this is the 2nd initialization attempt
                if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailSelHWUART, ProgClassName);
            }
            // Uninitialize the UART and try again
            _usb2gpio.uartEnd();
        }

        /*
         * ----------------------------------------------------------------------------------------------------
         * The default value for FT232 latency timer is 16mS.
         * ----------------------------------------------------------------------------------------------------
         * On Windows, this value may be customised by adding or changing the following entries in the
         * 'FTDIPORT.INF' file of the driver before installation:
         *     [FtdiPort232.NT.HW.AddReg]
         *     HKR,,"LatencyTimer",0x00010001,2
         * This above example will set the default latency timer value to 2mS. The valid range for the latency
         * timer is 1mS - 255mS, although 1mS is not recommended as this is the same as the USB frame length.
         *
         * It is also possible to change the latency after installation:
         *     1. Start the Windows Device Manager while your FTDI USB->Serial cable is attached. Look for its
         *        corresponding USB Serial Port under Ports (COM and LPT).
         *     2. Right click on it, and select Properties from the popup menu. Next, click the Port Settings
         *        tab and click the Advanced… button.
         *     3. In the dialog which pops up, lower the Latency Timer (mS) value from its default of 16 to 1
         *        and click OK.
         * After you disconnect/reconnect your device, the new Latency Timer value will take effect.
         * ----------------------------------------------------------------------------------------------------
         * On Linux, this value may be customised by executing this command as root:
         *     echo 1 > /sys/bus/usb-serial/devices/ttyUSB0/latency_timer
         * This above example will set the default latency timer value to 1mS.
         * ----------------------------------------------------------------------------------------------------
         * On MacOS, it is much more complicated. Please refer to:
         *     https://openbci.com/forum/index.php?p=/discussion/3108/driver-latency-timer-fix-for-macos-11-m1-m2
         *     https://docs.openbci.com/Troubleshooting/FTDI_Fix_Mac
         *     https://www.mattkeeter.com/blog/2022-05-31-xmodem/#ftdi
         *     https://gist.github.com/mkeeter/c43c3990ecdb8dcb6547ac3dbac8e881
         * ----------------------------------------------------------------------------------------------------
         */

        /*
         * ##### !!! TODO !!! #####
         *
         * megaAVR 0-Series     and AVR-DA/DB : HV-UPDI is NOT SUPPORTED           (<=0)
         * tinyAVR 0/1/2-Series               : HV-UPDI on UPDI pin                (  1)
         * AVR-DD/DU            and AVR-Ex    : HV-UPDI on nRST pin                (  2)
         * AVR-Sx                             : HV-UPDI on nRST pin with handshake (  3)
         * AVR-Lx                             : ??? ??? ???
         *
         * NOTE : See '../../../hardware/JxMake_USB_GPIO-Protocol_Manual.txt' for more details.
         */

        // Trigger High-Voltage UPDI using PGD and PGC
        if(_usb2gpio instanceof USB_GPIO) {
            // The trigger key
            // NOTE : Please refer to '../../../hardware/JxMake_USB_GPIO-Protocol_Manual.txt' for the protocol details.
            final int[] triggerKey = { 0x48, 0x56, 0x2D, 0x55, 0x50, 0x44, 0x49, 0x3A, 0x4E }; // HV-UPDI:N"
            /* ##### !!! TODO : How to select between these two? !!! #####
             *    KEY_NVMPROG    : 0x4E ('N')
             *    KEY_CHIP_ERASE : 0x45 ('E')
             * Add 'public int Config.hvUPDIMode'?
             */
            // Initialize the SPI
            if( _usb2gpio.spiBegin( USB2GPIO.SPIMode._0, USB2GPIO.SSMode.ActiveLow, _usb2gpio.spiClkFreqToClkDiv(8000) ) ) {
                // Send the trigger key
                _usb2gpio.spiTransferIgnoreSS(triggerKey);
                // Uninitialize the SPI
                _usb2gpio.spiEnd();
                // Delay long enough to ensure the High-Voltage UPDI has completed its work
                SysUtil.sleepMS(100);
            }
        }

        // Send UPDI_BREAK(s) and check if it is connected
        boolean updiConnected = false;

        for(int retry = 0; retry < UPDI_CONNECT_RETRY_COUNT; ++retry) {
            if( _updiBreak() ) {
                updiConnected = true;
                break;
            }
        }

        if(!updiConnected) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Connect, ProgClassName);

        // Intialize
        try {

            // Disable the collision detection
            if( !_updi_tx_cmd_stcs(UPDI_CS_CTRLB, CTRLB_CCDETDIS) ) USB2GPIO.TansmitError.throwTansmitError_uartTx(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

            // Enable inter-byte delay and reduce the guard time value (refer to the UPDI_CTRLA_VALUE constant definition)
            if( !_updi_tx_cmd_stcs(UPDI_CS_CTRLA, UPDI_CTRLA_VALUE) ) USB2GPIO.TansmitError.throwTansmitError_uartTx(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

            // Set the UPDI clock frequency
            _updiClkSel = (baudrate > 900000) ? ASI_CTRLA_UPDICLKSEL_32M
                        : (baudrate > 450000) ? ASI_CTRLA_UPDICLKSEL_16M
                        : (baudrate > 225000) ? ASI_CTRLA_UPDICLKSEL_08M
                        :                       ASI_CTRLA_UPDICLKSEL_04M;

            if( !_updi_tx_cmd_stcs(UPDI_ASI_CTRLA, _updiClkSel) ) USB2GPIO.TansmitError.throwTansmitError_uartTx(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

            // Change the UART baudrate to the requested baudrate
            if( !_usb2gpio.uartChangeBaudrate(baudrate) ) USB2GPIO.TansmitError.throwTansmitError_uartConfig(Texts.ProgXXX_FailUARTConfig, ProgClassName);

            // Check the UPDI connection
            if( !_updi_check() ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailUPDI_RInvVal, ProgClassName);

            // Retrieve the NVM revision early and store it for use in subsequent functions and for
            // later comparison inside '_getDeviceDetails()'
            final int[] sib = _updi_tx_rx_cmd_sib();

            if(sib != null) _refNVMRevision = sib[10] - ( (int) '0' );

            // Enable NVM programming
            if( !_updi_enable_nvmprog() ) {
                // Report an error if NVM programming cannot be enabled and the device is not locked
                if( !_updi_device_locked() ) {
                    USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailUPDI_EnProg, ProgClassName);
                }
                // Otherwise, notify the user and attempt to unlock the device
                else {
                    USB2GPIO.TansmitError.notifyError(Texts.ProgXXX_FailUPDI_DevLockd, ProgClassName);
                    if( !_updi_unlock_device() ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailUPDI_Unlock, ProgClassName);
                }
            }

            // Get the device details
            if( _getDeviceDetails() == null ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailUPDI_GetDDet, ProgClassName);

        } // try
        catch(final USB2GPIO.TansmitError e) {
            // Disable UPDI
            _updi_disable();
            // Uninitialize the UART
            _usb2gpio.uartEnd();
            // Notify error
            return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Init, ProgClassName);
        }

        // Set flag
        _inProgMode = true;

        // Done
        return true;
    }

    @Override
    public boolean end()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Reset the device, disable UPDI, and uninitialize the UART
        final boolean resCommitEEPROM =  commitEEPROM();
        final boolean resUPDIReset    = _updi_apply_reset();
        final boolean resUPDIDisable  = _updi_disable();
        final boolean resUARTEnd      = _usb2gpio.uartEnd();

        // Disable mode
        boolean resDisMode = true;

        if(_usb2gpio instanceof USB_GPIO) {
            resDisMode = ( (USB_GPIO) _usb2gpio ).pcf8574Disable();
        }

        // Clear flag
        _inProgMode = false;

        // Check for error(s)
        if(!resCommitEEPROM || !resUPDIReset || !resUPDIDisable || !resUARTEnd || !resDisMode) {
            if(!resCommitEEPROM) USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmEEPROM, ProgClassName);
            if(!resUPDIReset   ) USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Reset   , ProgClassName);
            if(!resUPDIDisable ) USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Disable , ProgClassName);
            if(!resUARTEnd     ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitUART   , ProgClassName);
            if(!resDisMode     ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPCF8574, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean dumpDeviceDetails(final PrintStream ps)
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return false;
        }

        // Dump the device details
        _deviceDetails.dump(ps, _config.memorySignatureExt.bitSize_TempSense);

        // Done
        return true;
    }

    @Override
    public boolean supportSignature()
    { return true; }

    @Override
    public boolean readSignature()
    {
        // Error if not in programming mode or if the device details are not available
        if(!_inProgMode || _deviceDetails == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // NOTE : There is no need to actually read the signature here because it can be found in the device details

        // Done
        return true;
    }

    @Override
    public boolean verifySignature(final int[] signatureBytes)
    {
        // Error if not in programming mode or if the device details are not available
        if(!_inProgMode || _deviceDetails == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Compare the signature
        return Arrays.equals(_deviceDetails.signature_bytes, signatureBytes);
    }

    @Override
    public int[] mcuSignature()
    {
        // Error if not in programming mode or if the device details are not available
        if(!_inProgMode || _deviceDetails == null) return null;

        // Return the signature
        return _deviceDetails.signature_bytes;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : # If the device is     locked, this function erases flash, EEPROM, and lock bits
    //        # If the device is not locked, this function erases flash only
    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // If the device is locked, unlock (and erase) it
        if( _updi_device_locked() ) return _updi_unlock_device();

        // Wait until the NVM controller is ready
        if( !_updi_wait_nvmctl_ready() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);

        // Erase chip
        if( _deviceDetails.nvm0() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM0CTRL_CTRLA_CHIP_ERASE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm2() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM2CTRL_CTRLA_CHIP_ERASE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm3() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM3CTRL_CTRLA_CHIP_ERASE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm4() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM4CTRL_CTRLA_CHIP_ERASE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm5() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM5CTRL_CTRLA_CHIP_ERASE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm6() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM6CTRL_CTRLA_CHIP_ERASE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else {
            // Error for now
            return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName);
        }

        // Wait until the NVM controller is ready again
        if( !_updi_wait_nvmctl_ready() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);

        // Remove command
        if( _deviceDetails.nvm0() ) {
            // Nothing to do here!
        }
        else if( _deviceDetails.nvm2() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM2CTRL_CTRLA_NOCMD) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm3() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM3CTRL_CTRLA_NOCMD) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm4() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM4CTRL_CTRLA_NOCMD) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm5() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM5CTRL_CTRLA_NOCMD) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else if( _deviceDetails.nvm6() ) {
            if( !_updi_exec_nvm_cmd(UPDI_NVM6CTRL_CTRLA_NOCMD) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
        }
        else {
            // Error for now
            return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMRevU, ProgClassName);
        }

        // Set flag
        _chipErased = true;

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

    @Override
    public int _flashMemoryAlignWriteSize(final int numBytes)
    { return USB2GPIO.alignWriteSize(numBytes, _config.memoryFlash.pageSize); }

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
        final int sa = (startAddress < 0) ? 0                             : startAddress;
        final int nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_even(sa, nb, _config.memoryFlash.totalSize, ProgClassName) ) return -1;

        // Prepare the result buffer
        if(_config.memoryFlash.readDataBuff == null || _config.memoryFlash.readDataBuff.length != numBytes) {
            _config.memoryFlash.readDataBuff = new int[numBytes];
        }

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Determine the number of chunks
        final int     ChunkSize  = _config.memoryFlash.pageSize;

        final boolean notAligned = (numBytes % ChunkSize) != 0;
        final int     numChunks  = (numBytes / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        int rdbIdx = 0;
        int verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int   numReads = Math.min(ChunkSize, numBytes - rdbIdx);
            final int[] cbytes   = _updi_read_nvm(_config.memoryFlash.address + sa + c * ChunkSize, numReads);

            if(cbytes == null) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                return -1;
            }

            // Process the chunk bytes
            for(int b = 0; b < numReads; b += 2) {

                // Store the bytes to the result buffer
                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b    ];
                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b + 1];

                // Compare the bytes as needed
                if(refData != null && verIdx < refData.length) {
                    if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                    ++verIdx;
                    if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                    ++verIdx;
                }

                // Call the progress callback function for the current value
                pcb.callProgressCallbackCurrent(progressCallback, nb);

            } // for b

        } // for c

        /*
        for(int addr = sa; addr < (sa + nb); addr += 2) {

            // Process in word
            final int v[] = _updi_read_addrNV_data8(_config.memoryFlash.address + addr, 2);
            if(v == null) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                return -1;
            }

            // Store the bytes to the result buffer
            _config.memoryFlash.readDataBuff[rdbIdx++] = v[0];
            _config.memoryFlash.readDataBuff[rdbIdx++] = v[1];

            // Compare the bytes as needed
            if(refData != null && verIdx < refData.length) {
                if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                ++verIdx;
                if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                ++verIdx;
            }

            // Call the progress callback function for the current value
            pcb.callProgressCallbackCurrent(progressCallback, nb);

        } // for addr
        */

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return numBytes;
    }

    @Override
    public boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(null, startAddress, numBytes, progressCallback) == numBytes; }

    private boolean _writeFlashPage(final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_pageSize(sa, nb, _config.memoryFlash.pageSize, ProgClassName) ) return false;

        // Get the number of pages to be written and the current page address
        final int numPages = nb / _config.memoryFlash.pageSize;
              int cpgAddr  = sa;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Clear flag
        _chipErased = false;

        // Write the pages
        int datIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            // ##### !!! TODO : Skip writing the page if it is blank (its contents are all 'FlashMemory_EmptyValue') !!! #####

            // Write the page
            // ##### ??? TODO : Is it always OK to disable ACKs ??? #####
            final boolean disableACKs = true; // Disabling ACKs may improve performance up to ~2.5 times

            if( !_updi_write_nvm(_config.memoryFlash.address + cpgAddr, data, datIdx, _config.memoryFlash.pageSize, disableACKs) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, _config.memoryFlash.pageSize / 2);

            // Increment the counters
            cpgAddr += _config.memoryFlash.pageSize;
            datIdx  += _config.memoryFlash.pageSize;

        } // for p

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
        final int sa = (startAddress < 0) ? 0                             : startAddress;
        final int nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer(data, sa, nb, _config.memoryFlash.pageSize, _config.memoryFlash.totalSize, FlashMemory_EmptyValue, ProgClassName);
        if(anbr == null) return false;

        // Write flash
        return _writeFlashPage(anbr.buff, sa, anbr.nb, progressCallback);
    }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[]     _eepromBuffer = null;
    private boolean[] _eepromFDirty = null;

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
            // Not paged
            if(_config.memoryEEPROM.pageSize == 1) {
                _eepromBuffer = new int[_config.memoryEEPROM.totalSize];
                for(int i = 0; i < _config.memoryEEPROM.totalSize; ++i) {
                    // Read the byte
                    final int value[] = _updi_read_addrNV_data8(_config.memoryEEPROM.address + i, 1);
                    if(value == null) {
                        USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_RdByte, ProgClassName);
                        return -1;
                    }
                    // Save the byte
                    _eepromBuffer[i] = value[0];
                }
            }
            // Paged
            else {
                _eepromBuffer = _updi_read_addrNV_data8(_config.memoryEEPROM.address, _config.memoryEEPROM.totalSize);
                if(_eepromBuffer == null) {
                    USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
                    return -1;
                }
            }
            // Mark everything as not dirty
            _eepromFDirty = new boolean[_config.memoryEEPROM.totalSize];
            Arrays.fill(_eepromFDirty, false);
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

    public boolean commitEEPROM()
    {
        // Simply exit if there is no EEPROM buffer
        if(_eepromBuffer == null) return true;

        // Write only the dirty page(s)
        for(int i = 0; i < _config.memoryEEPROM.totalSize; i += _config.memoryEEPROM.pageSize) {

            // Skip if the page is not dirty
            boolean pageDirty = false;

            for(int b = 0; b < _config.memoryEEPROM.pageSize; ++b) {
                if(_eepromFDirty[i + b]) {
                    pageDirty = true;
                    break;
                }
            }

            if(!pageDirty) continue;

            // Not paged
            if(_config.memoryEEPROM.pageSize == 1) {
                for(int j = i; j < i + _config.memoryEEPROM.pageSize; ++j) {
                    // Do not disable ACKs when writing just one byte as it can cause even more overhead
                    if( !_updi_write_nvm(
                        _config.memoryEEPROM.address + j, new byte[] { (byte) _eepromBuffer[j] }, 0, 1, false
                    ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
                }
            }
            // Paged
            else {
                // Write the page
                final byte[] buff = USB2GPIO.ia2ba(_eepromBuffer, i, _config.memoryEEPROM.pageSize);

                if( !_updi_write_nvm(
                    _config.memoryEEPROM.address + i, buff, 0, _config.memoryEEPROM.pageSize, false
                ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
            }

        } // for

        // Mark everything as not dirty
        Arrays.fill(_eepromFDirty, false);

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _writeFuseAndLockBits(final int address, final int value)
    {
        if( _deviceDetails.nvm0() ) {
            // Wait until the NVM controller is ready
            if( !_updi_wait_nvmctl_ready() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);

            // Write the low address
            _oneByte_wrBuff[0] = address & 0xFF;
            if( !_updi_tx_cmd_sts_addrNV_data8(_config.memoryAVRBase.NVM + UPDI_NVM02CTRL_ADDRL, _oneByte_wrBuff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

            // Write the high address
            _oneByte_wrBuff[0] = (address >> 8) & 0xFF;
            if( !_updi_tx_cmd_sts_addrNV_data8(_config.memoryAVRBase.NVM + UPDI_NVM02CTRL_ADDRH, _oneByte_wrBuff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

            // Write the fuse value
            _oneByte_wrBuff[0] = value;
            if( !_updi_tx_cmd_sts_addrNV_data8(_config.memoryAVRBase.NVM + UPDI_NVM02CTRL_DATAL, _oneByte_wrBuff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_Tx, ProgClassName);

            // Execute the command to write the fuse
            if( !_updi_exec_nvm_cmd(UPDI_NVM0CTRL_CTRLA_WRITE_FUSE) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);

            // Wait until the NVM controller is ready
            if( !_updi_wait_nvmctl_ready() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_NVMNotR, ProgClassName);

            // Done
            return true;
        }

        else {
            return _updi_write_nvm( address, new byte[] { (byte) value }, 0, 1, false );
        }
    }

    public int[] readFuses()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return null;
        }

        // Read the bytes
        final int[] fuses = new int[_config.memoryFuse.address.length];

        for(int i = 0; i < fuses.length; ++i) {
            if(_config.memoryFuse.address[i] >= 0) {
                if( !_updi_tx_rx_cmd_lds_addrNV_data8(_config.memoryFuse.address[i], _oneByte_rdBuff) ) return null;
                fuses[i] = _oneByte_rdBuff[0] & _config.memoryFuse.bitMask[i];
            }
            else {
                fuses[i] = -1;
            }
        }

        // Return the bytes
        return fuses;
    }

    public boolean writeFuses(final int[] values)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Read the original fuses
        final int[] origFuses = readFuses();
        if(origFuses == null) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);

        // Write the bytes as needed
        for(int i = 0; i < _config.memoryFuse.address.length; ++i) {

            // Skip fuses that are unused or that the user does not want to change
            if(_config.memoryFuse.address[i] < 0 || values[i] < 0) continue;

            // Compute the new value
            int newValue = values[i] & _config.memoryFuse.bitMask[i];

            // Skip fuses with unchanged values
            if(newValue == origFuses[i]) continue;

            /*
            SysUtil.stdDbg().printf("#%d @%04X : %02X -> %02X\n", i, _config.memoryFuse.address[i], origFuses[i], newValue);
            //*/

            // Apply masks to adjust the new value
            if(_config.memoryFuse.clrMask != null) { newValue &= ~_config.memoryFuse.clrMask[i]; }
            if(_config.memoryFuse.setMask != null) { newValue |=  _config.memoryFuse.setMask[i]; }

            // Write the fuse
            if( !_writeFuseAndLockBits(_config.memoryFuse.address[i], newValue) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : On newer AVR MCUs, the UPDI lock bits span more than one byte

    @Override
    public long readLockBits()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Silently ignore if the feature is disabled
        if(_config.memoryLockBits.size <= 0) return 0;

        // Read the lock bits
        if(_config.memoryLockBits.size == 1) {
            // Read the byte
            if( !_updi_tx_rx_cmd_lds_addrNV_data8(_config.memoryLockBits.address, _oneByte_rdBuff) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_TxRx, ProgClassName);
                return -1;
            }
            // Return the byte
            return _oneByte_rdBuff[0] & _config.memoryLockBits.bitMask;
        }
        else {
            // Read the bytes
            final int[] res = _updi_read_addrNV_data8(_config.memoryLockBits.address, _config.memoryLockBits.size);
            if(res == null) return -1;
            /*
            SysUtil.stdDbg().printf("LOCK = %02X %02X %02X %02X\n", res[0], res[1], res[2], res[3] ); // 5C C5 C5 5C
            //*/
            // Combine the bytes
            long lock = 0;
            for(int i = _config.memoryLockBits.size - 1; i >= 0; --i) {
                lock |= (res[i] & _config.memoryLockBits.bitMask);
                if(i != 0) lock <<= 8;
            }
            /*
            SysUtil.stdDbg().printf("LOCK = %08X\n", lock); // 5CC5C55C
            //*/
            // Return the combined bytes
            return lock & _config.memoryLockBits.bitMaskExt;
        }
    }

    @Override
    public boolean writeLockBits(final long value)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Silently ignore if the feature is disabled
        if(_config.memoryLockBits.size <= 0) return true;

        // Read the original lock bits
        final long origLockBits = readLockBits();
        if(origLockBits < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);

        // Read the lock bits
        if(_config.memoryLockBits.size == 1) {
            // Write the lock bits as needed
            final long newLockBits = value & _config.memoryLockBits.bitMask;
            if(newLockBits != origLockBits) {
                if( !_writeFuseAndLockBits( _config.memoryLockBits.address, (int) newLockBits ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailUPDI_CmdErr, ProgClassName);
            }
            // Done
            return true;
        }

        else {
            // Write the lock bits as needed
            final long newLockBits = value & _config.memoryLockBits.bitMaskExt;
            if(newLockBits != origLockBits) {
                // Extract the bytes
                final byte[] lock = new byte[_config.memoryLockBits.size];
                for(int i = 0; i < _config.memoryLockBits.size; ++i) {
                    lock[i] = (byte) ( ( value >> (i * 8) ) & 0xFF );
                }
                /*
                SysUtil.stdDbg().printf("LOCK = %02X %02X %02X %02X\n", lock[0], lock[1], lock[2], lock[3] ); // 5C C5 C5 5C
                //*/
                // ##### !!! TODO : VERIFY - IS THIS THE CORRECT METHOD !!! #####
                // Write the lock bits
                return _updi_write_nvm( _config.memoryLockBits.address, lock, 0, _config.memoryLockBits.size, false );
            }
            // Done
            return true;
        }
    }

} // class ProgUPDI
