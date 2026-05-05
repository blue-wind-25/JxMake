/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;

import java.util.Arrays;
import java.util.function.IntConsumer;

import com.fazecast.jSerialComm.*;

import jxm.*;
import jxm.annotation.*;
import jxm.annotation.*;
import jxm.tool.*;
import jxm.xb.*;


/*
 * This class is written partially based on the algorithms and information found from:
 *
 *     XMEGA A MANUAL
 *     http://ww1.microchip.com/downloads/en/DeviceDoc/doc8077.pdf
 *
 *     AVR-1612 : PDI Programming Driver
 *     https://ww1.microchip.com/downloads/en/Appnotes/doc8282.pdf
 *
 *     AVR-1612 : PDI Programming Driver - Coding
 *     https://community.element14.com/products/devtools/technicallibrary/w/documents/11423/avr1612-pdi-programming-driver-coding
 *     https://community.element14.com/cfs-file/__key/communityserver-wikis-components-files/00-00-00-01-46/Atmel_2D00_megaAVR_2D00_ATmega48_2D00_Development-Kits_2D00_AVR-JTAGICE-mkII_2D00_Design-Elements_2D00_Application-Library_2D00_Atmel.Application_5F00_Library_5F00_14.zip
 *     Copyright (C) 2009 Atmel Corporation. All rights reserved.
 *         Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *             1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *             2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 *                documentation and/or other materials provided with the distribution.
 *             3. The name of Atmel may not be used to endorse or promote products derived from this software without specific prior written permission.
 *             4. This software may only be redistributed and used in connection with an Atmel AVR product.
 *         THIS SOFTWARE IS PROVIDED BY ATMEL "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *         MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT ARE EXPRESSLY AND SPECIFICALLY DISCLAIMED. IN NO EVENT SHALL ATMEL BE
 *         LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *         SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 *         IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *         ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *     LUFA Library AVRISP mkII Project
 *     https://github.com/abcminiuser/lufa/tree/master/Projects/AVRISP-MKII
 *     Copyright (C) Dean Camera, 2021
 *     https://github.com/abcminiuser/lufa/blob/master/LUFA/License.txt
 *
 *     USBasp Mods for PDI programming
 *     https://skeatz.github.io/FabPDI/usbasp-mods.html
 *     https://github.com/skeatz/FabPDI
 *     Copyright (C) 2017 Steven Chew
 *     MIT License
 *
 *     PDI Patch for USBasp and AVRDUDE
 *     https://github.com/blue-wind-25/PDI-Patch-for-USBasp-and-AVRDUDE
 *     https://github.com/blue-wind-25/PDI-Patch-for-USBasp-and-AVRDUDE/blob/main/usbasp.2011-05-28/usbasp-pdi-usbaspfirmware-20120816-FIXED.diff
 */
public class ProgPDI implements IProgCommon {

    /*
     * Transfer speed:
     *     # Using USB_ISS            : not supported
     *     # Using JxMake DASA        : not supported
     *     # Using JxMake USB-GPIO    : up to ~2100 ...  ~6000 bytes per second (depending on the baudrate, target, operation, and communication reliability);
     *                                  it is recommended to use the standard 115200 baud
     *     # Using JxMake USB-GPIO II : up to ~5200 ... ~12400 bytes per second (depending on the baudrate, target, operation, and communication reliability);
     *                                  it is recommended to use the standard 921600 baud
     */

    /*
     * ##### !!! WARNING !!! #####
     * This programmer is currently not very reliable when:
     *     # Using hardware version 1 (the older 'JxMake USB GPIO'    module) at any baud rate.
     *     # Using hardware version 2 (the newer 'JxMake USB GPIO II' module) at low baud rates.
     */

    private static final String ProgClassName = "ProgPDI";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final byte FlashMemory_EmptyValue = (byte) 0xFF;

    @SuppressWarnings("serial")
    public static class Config extends SerializableDeepClone<Config> {

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        // NOTE : The default values below are for (almost all?) AVR MCUs that can be programmed using PDI

        public static class MemoryAVRBase implements Serializable {
            @DataFormat.Hex08 public int DATA = 0x01000000;
            @DataFormat.Hex08 public int MCU  = 0x00000090;
            @DataFormat.Hex08 public int NVM  = 0x000001C0;
        }

        public static class MemorySignature implements Serializable {
            @DataFormat.Hex08 public int address = 0x01000090;
                              public int size    = 3;
        }

        public static class MemoryFlash implements Serializable {
            @DataFormat.Hex06 public int   address      = 0x800000;
                              public int   totalSize    = 0;
                              public int   pageSize     = 0;
                              public int   numPages     = 0;

                              public int[] readDataBuff = null;
        }

        public static class MemoryEEPROM implements Serializable {
            @DataFormat.Hex06 public int address   = 0x8C0000;
                              public int totalSize = 0;
                              public int pageSize  = 0;
                              public int numPages  = 0;
        }

        public static class MemoryFuse implements Serializable {
                              //                                 0         1         2         3   4         5         6
                              //                                 jtaguid   *         *             *         *
            @DataFormat.Hex06 public int[] address = new int[] { 0x8F0020, 0x8F0021, 0x8F0022, -1, 0x8F0024, 0x8F0025, -1 };
            @DataFormat.Dec08 public int[] size    = new int[] { 1       , 1       , 1       ,  0, 1       , 1       ,  0 };
            @DataFormat.Hex06 public int[] bitMask = new int[] { 0xFF    , 0xFF    , 0x63    , -1, 0x1F    , 0x3F    , -1 };
                              //                                                                   │
                              //                                                                   └→ be careful with this one
        }

        public static class MemoryLockBits implements Serializable {
            @DataFormat.Hex04 public int address = 0x128A;
                              public int size    = 1;
            @DataFormat.Hex02 public int bitMask = 0xFF;
        }

        // ##### ??? TODO : Add other memories ('apptable' and 'usersig') ??? #####

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final MemoryAVRBase   memoryAVRBase   = new MemoryAVRBase  ();
        public final MemorySignature memorySignature = new MemorySignature();
        public final MemoryFlash     memoryFlash     = new MemoryFlash    ();
        public final MemoryEEPROM    memoryEEPROM    = new MemoryEEPROM   ();
        public final MemoryFuse      memoryFuse      = new MemoryFuse     ();
        public final MemoryLockBits  memoryLockBits  = new MemoryLockBits ();

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'ATxmega*()' functions ??? #####


public static Config ATxmega16D4()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   256;
    config.memoryFlash.numPages   =    64;

    config.memoryEEPROM.totalSize = 1024;
    config.memoryEEPROM.pageSize  =   32;
    config.memoryEEPROM.numPages  =   32;

    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // PDI instructions
    private static int PDI_CMD_LDS   (final int a, final int b ) { return 0x00 | ( (a << 2) & 0x0C ) | (b  & 0x03); } // 16/24/32-bit address
    private static int PDI_CMD_STS   (final int a, final int b ) { return 0x40 | ( (a << 2) & 0x0C ) | (b  & 0x03); } // 16/24/32-bit address
    private static int PDI_CMD_LD    (final int p, final int ab) { return 0x20 | ( (p << 2) & 0x0C ) | (ab & 0x03); }
    private static int PDI_CMD_ST    (final int p, final int ab) { return 0x60 | ( (p << 2) & 0x0C ) | (ab & 0x03); } // 16/24/32-bit address for PDI_PTR_ADDRESS
    private static int PDI_CMD_LDCS  (final int a              ) { return 0x80 | (  a       & 0x0F )              ; }
    private static int PDI_CMD_STCS  (final int a              ) { return 0xC0 | (  a       & 0x0F )              ; }
    private static int PDI_CMD_REPEAT(             final int b ) { return 0xA0                       | (b  & 0x03); }
    private static int PDI_CMD_KEY   (                         ) { return 0xE0                                    ; }

    private static final int   PDI_ADDRESS_08      = 0x00; // Address size (a)
    private static final int   PDI_ADDRESS_16      = 0x01; // ---
    private static final int   PDI_ADDRESS_24      = 0x02; // ---
    private static final int   PDI_ADDRESS_32      = 0x03; // ---

    private static final int   PDI_DATA_08         = 0x00; // Data size (b)
    private static final int   PDI_DATA_16         = 0x01; // ---
    private static final int   PDI_DATA_24         = 0x02; // ---
    private static final int   PDI_DATA_32         = 0x03; // ---

    private static final int   PDI_PTR             = 0x00; // Pointer access (p)
    private static final int   PDI_PTR_INC         = 0x01; // ---
    private static final int   PDI_PTR_ADDRESS     = 0x02; // ---

    private static final int   PDI_MAX_REPEAT_SIZE = 0xFF;

    private static final int[] KEY_NVMPROG         = new int[] { 0xFF, 0x88, 0xD8, 0xCD, 0x45, 0xAB, 0x89, 0x12 };

    // PDI registers
    private static final int PDI_CS_STATUS = 0x00;
    private static final int PDI_CS_RESET  = 0x01;
    private static final int PDI_CS_CTRL   = 0x02;

    // STATUS bits
    private static final int PDI_STATUS_NVMEN = 0x02;

    // RESET signature
    private static final int PDI_RESET_SIGNATURE = 0x59;

    // CTRL bits
    private static final int CTRL_GTVAL_2    = 0x04;
    private static final int CTRL_GTVAL_1    = 0x02;
    private static final int CTRL_GTVAL_0    = 0x01;
    private static final int CTRL_GTVAL_MASK = CTRL_GTVAL_2 | CTRL_GTVAL_1 | CTRL_GTVAL_0;
    private static final int CTRL_GTVAL_128B = 0x00;
    private static final int CTRL_GTVAL_064B = 0x01;
    private static final int CTRL_GTVAL_032B = 0x02;
    private static final int CTRL_GTVAL_016B = 0x03;
    private static final int CTRL_GTVAL_008B = 0x04;
    private static final int CTRL_GTVAL_004B = 0x05;
    private static final int CTRL_GTVAL_002B = 0x06;

    // NVM controller register offsets
    private static final int PDI_NVMCTRL_ADDR0  = 0x00;
    private static final int PDI_NVMCTRL_ADDR1  = 0x01;
    private static final int PDI_NVMCTRL_ADDR2  = 0x02;
    private static final int PDI_NVMCTRL_DATA0  = 0x04;
    private static final int PDI_NVMCTRL_DATA1  = 0x05;
    private static final int PDI_NVMCTRL_DATA2  = 0x06;
    private static final int PDI_NVMCTRL_CMD    = 0x0A;
    private static final int PDI_NVMCTRL_CTRLA  = 0x0B;
    private static final int PDI_NVMCTRL_STATUS = 0x0F;

    // CTRLA bits
    private static final int PDI_NVMCTRL_CTRLA_CMDEX = 0x01;

    // STATUS bits
    private static final int PDI_NVMCTRL_STATUS_NVMBUSY = 0x80;
    private static final int PDI_NVMCTRL_STATUS_FBUSY   = 0x40;

    // NVM commands
    private static final int NVM_CMD_NOP                               = 0x00;
    private static final int NVM_CMD_CHIP_ERASE                        = 0x40;
    private static final int NVM_CMD_READ_NVM                          = 0x43;

    private static final int NVM_CMD_LOAD_FLASH_PAGE_BUFFER            = 0x23;
    private static final int NVM_CMD_ERASE_FLASH_PAGE_BUFFER           = 0x26;
    private static final int NVM_CMD_ERASE_FLASH_PAGE                  = 0x2B;
    private static final int NVM_CMD_WRITE_FLASH_PAGE                  = 0x2E;
    private static final int NVM_CMD_ERASE_AND_WRITE_FLASH_PAGE        = 0x2F;
    private static final int NVM_CMD_FLASH_CRC                         = 0x78;

    private static final int NVM_CMD_ERASE_APP_SECTION                 = 0x20;
    private static final int NVM_CMD_ERASE_APP_SECTION_PAGE            = 0x22;
    private static final int NVM_CMD_WRITE_APP_SECTION_PAGE            = 0x24;
    private static final int NVM_CMD_ERASE_AND_WRITE_APP_SECTION_PAGE  = 0x25;
    private static final int NVM_CMD_APP_SECTION_CRC                   = 0x38;

    private static final int NVM_CMD_ERASE_BOOT_SECTION                = 0x68;
    private static final int NVM_CMD_ERASE_BOOT_SECTION_PAGE           = 0x2A;
    private static final int NVM_CMD_WRITE_BOOT_SECTION_PAGE           = 0x2C;
    private static final int NVM_CMD_ERASE_AND_WRITE_BOOT_SECTION_PAGE = 0x2D;
    private static final int NVM_CMD_BOOT_SECTION_CRC                  = 0x39;

    private static final int NVM_CMD_READ_USER_SIGN                    = 0x03;
    private static final int NVM_CMD_ERASE_USER_SIGN                   = 0x18;
    private static final int NVM_CMD_WRITE_USER_SIGN                   = 0x1A;
    private static final int NVM_CMD_READ_CALIB_ROW                    = 0x02;

    private static final int NVM_CMD_READ_FUSE                         = 0x07;
    private static final int NVM_CMD_WRITE_FUSE                        = 0x4C;
    private static final int NVM_CMD_WRITE_LOCK_BITS                   = 0x08;

    private static final int NVM_CMD_LOAD_EEPROM_PAGE_BUFFER           = 0x33;
    private static final int NVM_CMD_ERASE_EEPROM_PAGE_BUFFER          = 0x36;
    private static final int NVM_CMD_ERASE_EEPROM                      = 0x30;
    private static final int NVM_CMD_ERASE_EEPROM_PAGE                 = 0x32;
    private static final int NVM_CMD_WRITE_EEPROM_PAGE                 = 0x34;
    private static final int NVM_CMD_ERASE_AND_WRITE_EEPROM_PAGE       = 0x35;
    private static final int NVM_CMD_READ_EEPROM                       = 0x06;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int PDI_WAIT_NVM_CTL_BUSY_TIMEOUT_MS     = 1000;
    private static final int PDI_WAIT_NVM_PROG_ENABLED_TIMEOUT_MS = 1000;

    private static final int PDI_RETRY_COUNT_SET_RESET_STATE      =   16;
    private static final int PDI_RETRY_COUNT_READ_NVM_REG         =    8;
    private static final int PDI_RETRY_COUNT_WRITE_NVM_REG        =    8;
    private static final int PDI_RETRY_COUNT_EN_DIS_NVM_PROG      =    8;
    private static final int PDI_RETRY_COUNT_READ_MEMORY          =    8;
    private static final int PDI_RETRY_COUNT_WRITE_MEMORY         =    8;

    private static final int PDI_RETRY_COUNT_VERIFY_ERROR         =    8;
    private static final int PDI_RETRY_COUNT_READ_FLASH           =   32;
    private static final int PDI_RETRY_COUNT_READ_EEPROM          =   64;
    private static final int PDI_RETRY_COUNT_READ_FUSE            =   64;
    private static final int PDI_RETRY_COUNT_READ_LOCK_BITS       =   64;

    private final USB2GPIO _usb2gpio;
    private final Config   _config;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final int[] _oneByte_rdBuff = new int[1];

    private boolean _pdi_write(final int[] data, boolean disableTxOnExit)
    {
        // Empty any old data
        _usb2gpio.rawSerialPort().flushIOBuffers();

        // Enable Tx
        if( !_usb2gpio.usrtEnableTx() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_EnTx, ProgClassName);

        //*
        // Disable Tx after transmitting as needed
        if(disableTxOnExit) {
          //if(data.length > 255) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_DsTxAftrN, ProgClassName, data.length);
            if(data.length > 255) {
                // Use multiple writes if the number of bytes exceeds the limit of 'uint8_t'
                int[] _data = data;
                while(_data.length > 255) {
                    final int[] front = Arrays.copyOfRange(_data, 0  , 255         );
                                _data = Arrays.copyOfRange(_data, 255, _data.length);
                    if( !_pdi_write(front, false) ) return false;
                }
                return _pdi_write(_data);
            }
            else {
                if( !_usb2gpio.usrtDisableTxAfter(data.length) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_DsTxAfter, ProgClassName);
            }
        }
        //*/

        // Send the data
        final boolean res = _usb2gpio.usrtTx_discardSerialLoopback(data);

        /*
        // Disable Tx as needed
        if(disableTxOnExit) {
            if( !_usb2gpio.usrtDisableTx() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_DsTx, ProgClassName);
        }
        //*/

        // Return the result
        return res;
    }

    private boolean _pdi_write(final int[] data)
    { return _pdi_write(data, true); }

    private boolean _pdi_read(final int[] data)
    { return _usb2gpio.usrtRx(data); }

    private void _pdi_send_break()
    {
        // Try to recover by sending multiple BREAKs
        for(int i = 0; i < 3; ++i) {
            _usb2gpio.rawSerialPort().flushIOBuffers();
            _usb2gpio.rawSerialPort().setBreak      (); SysUtil.sleepMS(2);
            _usb2gpio.rawSerialPort().clearBreak    (); SysUtil.sleepMS(2);
        }
    }

    private boolean _pdi_tx_rx_cmd_lds_addrXX_dataXX(final int address, final int[] data, final int cmdAddress, final int cmdData)
    {
        // Send the command
        final int[] buff = (cmdAddress == PDI_ADDRESS_32) ? new int[] { PDI_CMD_LDS(PDI_ADDRESS_32, cmdData), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF, (address >> 24) & 0xFF }
                         : (cmdAddress == PDI_ADDRESS_24) ? new int[] { PDI_CMD_LDS(PDI_ADDRESS_24, cmdData), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF                         }
                         : (cmdAddress == PDI_ADDRESS_16) ? new int[] { PDI_CMD_LDS(PDI_ADDRESS_16, cmdData), (address & 0xFF), (address >> 8) & 0xFF                                                 }
                         :                                  new int[] { PDI_CMD_LDS(PDI_ADDRESS_08, cmdData), (address & 0xFF)                                                                        };

        if( !_pdi_write(buff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);

        // Receive the byte(s)
        return _pdi_read(data);
    }

    private boolean _pdi_tx_rx_cmd_lds_addr32_data8(final int address, final int[] data)
    { return _pdi_tx_rx_cmd_lds_addrXX_dataXX(address, data, PDI_ADDRESS_32, PDI_DATA_08); }

    private boolean _pdi_tx_cmd_sts_addrXX_dataXX(final int address, final int[] data, final int cmdAddress, final int cmdData)
    {
        // Send the command
        final int[] buff = (cmdAddress == PDI_ADDRESS_32) ? new int[] { PDI_CMD_STS(PDI_ADDRESS_32, cmdData), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF, (address >> 24) & 0xFF }
                         : (cmdAddress == PDI_ADDRESS_24) ? new int[] { PDI_CMD_STS(PDI_ADDRESS_24, cmdData), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF                         }
                         : (cmdAddress == PDI_ADDRESS_16) ? new int[] { PDI_CMD_STS(PDI_ADDRESS_16, cmdData), (address & 0xFF), (address >> 8) & 0xFF                                                 }
                         :                                  new int[] { PDI_CMD_STS(PDI_ADDRESS_08, cmdData), (address & 0xFF)                                                                        };

        if( !_pdi_write(buff, false) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);

        // Send the double-word(s)
        if(cmdData == PDI_DATA_32) {
            for(int i = 0; i < data.length; i += 4) {
                if( !_pdi_write( new int[] { data[i], data[i + 1], data[i + 2], data[i + 3] } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);
            }
        }
        // Send the triple-byte(s)
        else if(cmdData == PDI_DATA_24) {
            for(int i = 0; i < data.length; i += 3) {
                if( !_pdi_write( new int[] { data[i], data[i + 1], data[i + 2] } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);
            }
        }
        // Send the word(s)
        else if(cmdData == PDI_DATA_16) {
            for(int i = 0; i < data.length; i += 2) {
                if( !_pdi_write( new int[] { data[i], data[i + 1] } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);
            }
        }
        // Send the bytes(s)
        else {
            for(int i = 0; i < data.length; ++i) {
                if( !_pdi_write( new int[] { data[i] } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);
            }
        }

        // Done
        return true;
    }

    private boolean _pdi_tx_cmd_sts_addr32_data8(final int address, final int[] data)
    { return _pdi_tx_cmd_sts_addrXX_dataXX(address, data, PDI_ADDRESS_32, PDI_DATA_08); }

    private boolean _pdi_tx_rx_cmd_ld_ptr_inc_dataXX(final int[] data, final int cmdData)
    {
        // Send the command
        if( !_pdi_write( new int[] {
             PDI_CMD_LD(PDI_PTR_INC, cmdData)
        } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);

        // Receive the byte(s)
        return _pdi_read(data);
    }

    private boolean _pdi_tx_rx_cmd_ld_ptr_inc_data8(final int[] data)
    { return _pdi_tx_rx_cmd_ld_ptr_inc_dataXX(data, PDI_DATA_08); }

    private boolean _pdi_tx_rx_cmd_ld_ptr_inc_data16(final int[] data)
    { return _pdi_tx_rx_cmd_ld_ptr_inc_dataXX(data, PDI_DATA_16); }

    private boolean _pdi_tx_rx_cmd_ld_ptr_inc_data24(final int[] data)
    { return _pdi_tx_rx_cmd_ld_ptr_inc_dataXX(data, PDI_DATA_24); }

    private boolean _pdi_tx_rx_cmd_ld_ptr_inc_data32(final int[] data)
    { return _pdi_tx_rx_cmd_ld_ptr_inc_dataXX(data, PDI_DATA_32); }

    private boolean _pdi_tx_cmd_st_ptr_inc_dataXX(final int[] data, final int cmdData)
    {
        // Send the command
        if( !_pdi_write(
            new int[] { PDI_CMD_ST(PDI_PTR_INC, cmdData) },
            false
        ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);

        // Send the byte(s)
        return _pdi_write(data);
    }

    private boolean _pdi_tx_cmd_st_ptr_inc_data8(final int[] data)
    { return _pdi_tx_cmd_st_ptr_inc_dataXX(data, PDI_DATA_08); }

    private boolean _pdi_tx_cmd_st_ptr_inc_data16(final int[] data)
    { return _pdi_tx_cmd_st_ptr_inc_dataXX(data, PDI_DATA_16); }

    private boolean _pdi_tx_cmd_st_ptr_inc_data24(final int[] data)
    { return _pdi_tx_cmd_st_ptr_inc_dataXX(data, PDI_DATA_24); }
    private boolean _pdi_tx_cmd_st_ptr_inc_data32(final int[] data)
    { return _pdi_tx_cmd_st_ptr_inc_dataXX(data, PDI_DATA_32); }

    private boolean _updi_tx_cmd_st_ptr_addrXX(final int address, final int cmdAddress)
    {
        final int[] buff = (cmdAddress == PDI_ADDRESS_32) ? new int[] { PDI_CMD_ST(PDI_PTR_ADDRESS, PDI_ADDRESS_32), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF, (address >> 24) & 0xFF }
                         : (cmdAddress == PDI_ADDRESS_24) ? new int[] { PDI_CMD_ST(PDI_PTR_ADDRESS, PDI_ADDRESS_24), (address & 0xFF), (address >> 8) & 0xFF, (address >> 16) & 0xFF                         }
                         : (cmdAddress == PDI_ADDRESS_16) ? new int[] { PDI_CMD_ST(PDI_PTR_ADDRESS, PDI_ADDRESS_16), (address & 0xFF), (address >> 8) & 0xFF                                                 }
                         :                                  new int[] { PDI_CMD_ST(PDI_PTR_ADDRESS, PDI_ADDRESS_08), (address & 0xFF)                                                                        };


        return _pdi_write(buff);
    }

    private boolean _updi_tx_cmd_st_ptr_addr32(final int address)
    { return _updi_tx_cmd_st_ptr_addrXX(address, PDI_ADDRESS_32); }

    private int _pdi_tx_rx_cmd_ldcs(final int address)
    {
        if( !_pdi_write( new int[] { PDI_CMD_LDCS(address) } ) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);
            return -1;
        }

        if( !_pdi_read(_oneByte_rdBuff) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Rx, ProgClassName);
            return -1;
        }

        return _oneByte_rdBuff[0];
    }

    private boolean _pdi_tx_cmd_stcs(final int address, final int value)
    { return _pdi_write( new int[] { PDI_CMD_STCS(address), value } ); }

    private boolean _pdi_tx_cmd_repeat(final int repeat_)
    {
        final int repeat = repeat_ - 1;

        if( repeat < (1 << 8) ) {
            return _pdi_write( new int[] {
                PDI_CMD_REPEAT(PDI_DATA_08), (repeat & 0xFF)
            } );
        }
        else if( repeat < (1 << 16) ) {
            return _pdi_write( new int[] {
                PDI_CMD_REPEAT(PDI_DATA_16), (repeat & 0xFF), (repeat >> 8) & 0xFF
            } );
        }
        else if( repeat < (1 << 24) ) {
            return _pdi_write( new int[] {
                PDI_CMD_REPEAT(PDI_DATA_24), (repeat & 0xFF), (repeat >> 8) & 0xFF, (repeat >> 16) & 0xFF
            } );
        }
        else {
            return _pdi_write( new int[] {
                PDI_CMD_REPEAT(PDI_DATA_32), (repeat & 0xFF), (repeat >> 8) & 0xFF, (repeat >> 16) & 0xFF, (repeat >> 24) & 0xFF
            } );
        }
    }

    private boolean _pdi_tx_cmd_nvm_key()
    {
        if( !_pdi_write( new int[] { PDI_CMD_KEY() }, false ) ) return false;

        return _pdi_write(KEY_NVMPROG);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pdi_nvmctl_read_addr32_data8(final int address, final int[] data)
    {
        for(int r = 0; r < PDI_RETRY_COUNT_READ_NVM_REG; ++r) {
            if( _pdi_tx_rx_cmd_lds_addr32_data8(_config.memoryAVRBase.DATA + address, data) ) return true;
            _pdi_send_break();
        }

        return false;
    }

    private boolean _pdi_nvmctl_read_reg_XXX(final int offset, final int[] data)
    { return _pdi_nvmctl_read_addr32_data8(_config.memoryAVRBase.NVM + offset, data); }

    private boolean _pdi_nvmctl_read_reg_status(final int[] data)
    { return _pdi_nvmctl_read_reg_XXX(PDI_NVMCTRL_STATUS, data); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pdi_nvmctl_write_addr32_data8(final int address, final int[] data)
    {
        for(int r = 0; r < PDI_RETRY_COUNT_WRITE_NVM_REG; ++r) {
            if( _pdi_tx_cmd_sts_addr32_data8(_config.memoryAVRBase.DATA + address, data) ) return true;
            _pdi_send_break();
        }

        return false;
    }

    private boolean _pdi_nvmctl_write_reg_XXX(final int offset, final int[] data)
    { return _pdi_nvmctl_write_addr32_data8(_config.memoryAVRBase.NVM + offset, data); }

    private boolean _pdi_nvmctl_write_reg_cmd(final int[] data)
    { return _pdi_nvmctl_write_reg_XXX(PDI_NVMCTRL_CMD, data); }

    private boolean _pdi_nvmctl_write_reg_ctrla(final int[] data)
    { return _pdi_nvmctl_write_reg_XXX(PDI_NVMCTRL_CTRLA, data); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pdi_set_device_reset_state_true()
    {
        for(int r = 0; r < PDI_RETRY_COUNT_SET_RESET_STATE; ++r) {
            if( _pdi_tx_cmd_stcs(PDI_CS_RESET, PDI_RESET_SIGNATURE) ) return true;
            _pdi_send_break();
        }

        return false;
    }

    private int _pdi_set_device_reset_state_false()
    {
        for(int r = 0; r < PDI_RETRY_COUNT_SET_RESET_STATE; ++r) {
            if( _pdi_tx_cmd_stcs(PDI_CS_RESET, 0) ) {
                if( _pdi_tx_rx_cmd_ldcs(PDI_CS_RESET) == 0 ) return r;
            }
            _pdi_send_break();
        }

        return -1;
    }

    private boolean _pdi_set_device_reset_state(final boolean reset)
    {
        // Put the device into reset state
        if(reset) return _pdi_set_device_reset_state_true();

        // Pull the device out of reset state
        while(true) {

            // Pull the device out of reset state
            final int res = _pdi_set_device_reset_state_false();
            if(res < 0) break; // Check for error

            // ##### !!! TODO : Even with multiple resets-unresets, the device is not guaranteed to exit the reset state (silicon errata?) !!! #####

            // Put the device into reset state once more
            if( !_pdi_set_device_reset_state_true() ) return false;

            // If the previous operation is successful the first time, pull the device out of reset state for the last time
            if(res == 0) return _pdi_set_device_reset_state_false() >= 0;

        } // while

        // Not done
        return false;
    }

    private boolean _pdi_wait_nvmctl_busy()
    {
        // Wait until the NVM controller is ready
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(PDI_WAIT_NVM_CTL_BUSY_TIMEOUT_MS);

        while(true) {

            if( !_pdi_nvmctl_read_reg_status(_oneByte_rdBuff) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdNVMReg, ProgClassName);

            if( (_oneByte_rdBuff[0] & PDI_NVMCTRL_STATUS_NVMBUSY) == 0 ) return true;

            if( tms.timeout() ) break;

        } // while

        // Not done
        return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);
    }

    private boolean _pdi_wait_nvmprog_enabled_or_disabled(boolean enabled)
    {
        // Wait until the NVM controller is ready
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(PDI_WAIT_NVM_PROG_ENABLED_TIMEOUT_MS);

        while(true) {

            final int res = _pdi_tx_rx_cmd_ldcs(PDI_CS_STATUS);
            if(res < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_TxRx, ProgClassName);

            if(enabled) { if( (res & PDI_STATUS_NVMEN) != 0 ) return true; }
            else        { if( (res & PDI_STATUS_NVMEN) == 0 ) return true; }

            if( tms.timeout() ) break;

        } // while

        // Not done
        return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);
    }

    private boolean _pdi_enable_nvmprog_impl()
    {
        // Send the key
        if( !_pdi_tx_cmd_nvm_key() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);

        // Wait until the NVM controller is no longer busy
        if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

        // Wait until NVM programming is enabled
        if( !_pdi_wait_nvmprog_enabled_or_disabled(true) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotE, ProgClassName);

        // Done
        return true;
    }

    private boolean _pdi_enable_nvmprog()
    {
        for(int i = 0; i < PDI_RETRY_COUNT_EN_DIS_NVM_PROG; ++i) {
            if( _pdi_enable_nvmprog_impl() ) return true;
            _pdi_send_break();
        }

        return false;
    }

    private boolean _pdi_disable_nvmprog_impl()
    {
        // Clear the NVMEN bit
        if( !_pdi_tx_cmd_stcs(PDI_CS_STATUS, 0) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Tx, ProgClassName);

        // Wait until the NVM controller is no longer busy
        if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

        // Wait until NVM programming is disabled
        if( !_pdi_wait_nvmprog_enabled_or_disabled(false) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotD, ProgClassName);

        // Done
        return true;
    }

    private boolean _pdi_disable_nvmprog()
    {
        for(int i = 0; i < PDI_RETRY_COUNT_EN_DIS_NVM_PROG; ++i) {
            if( _pdi_disable_nvmprog_impl() ) return true;
            _pdi_send_break();
        }

        return false;
    }

    private boolean _pdi_reset_device()
    {
         // End PDI
        _pdi_disable_nvmprog();
        _pdi_set_device_reset_state(false);

        _usb2gpio.usrtEnd();
        _usb2gpio.rawSerialPort().closePort();

        _inProgMode = false;

         // Wait for a while
         SysUtil.sleepMS(500);

         // Begin PDI again
         _usb2gpio.rawSerialPort().openPort();

         if( !begin(_baudrate) ) return false;

         _pdi_send_break();

         // Done
         return readSignature();
    }

    private boolean _pdi_recover()
    {
        _pdi_send_break();
        if( readSignature() ) return true;

        return _pdi_reset_device();
    }

    private boolean _pdi_nvmctl_reset()
    {
        _pdi_send_break();

        if( !_pdi_nvmctl_write_reg_cmd(
            new int[] { NVM_CMD_NOP }
        ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _pdi_nvmctl_read_memory_impl(final int readCommandType, final int address, final int size)
    {
        // Wait until the NVM controller is no longer busy
        if( !_pdi_wait_nvmctl_busy() ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);
            return null;
        }

        // Send the read NVM command to the NVM controller
        if( !_pdi_nvmctl_write_reg_cmd( new int[] { readCommandType } ) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);
            return null;
        }

        // Load the PDI pointer register with the start address
        if( !_updi_tx_cmd_st_ptr_addr32(address) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrPtrAdr, ProgClassName);
            return null;
        }

        // Send the repeat command as needed
        if(size > 1) {
            if( !_pdi_tx_cmd_repeat(size) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrRepCnt, ProgClassName);
                return null;
            }
        }

        // Read the bytes
        final int[] buff = new int[size];

        if( !_pdi_tx_rx_cmd_ld_ptr_inc_data8(buff) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdNVMMem, ProgClassName);
            return null;
        }

        return buff;
    }

    private int[] _pdi_nvmctl_read_memory(final int readCommandType, final int address, final int size)
    {
        int[] cbytes = null;

        for(int r = 0; r < PDI_RETRY_COUNT_READ_MEMORY; ++r) {
            cbytes = _pdi_nvmctl_read_memory_impl(readCommandType, address, size);
            if(cbytes != null) return cbytes;
            _pdi_send_break();
        }

        return null;
    }

    private int[] _pdi_nvmctl_read_memory(final int address, final int size)
    { return _pdi_nvmctl_read_memory(NVM_CMD_READ_NVM, address, size); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pdi_nvmctl_write_memory_impl(final int writeCommandType, final int address, final int data)
    {
        // Wait until the NVM controller is no longer busy
        if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

        // Send the write command to the NVM controller
        if( !_pdi_nvmctl_write_reg_cmd( new int[] { writeCommandType } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);

        // Write the byte
        if( !_pdi_tx_cmd_sts_addr32_data8( address, new int[] { data } ) ) return false;

        // Wait until the NVM controller is no longer busy
        if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

        // Done
        return true;
    }

    private boolean _pdi_nvmctl_write_memory_impl(final int eraseBuffCommandType, final int writeBuffCommandType, final int writePageCommandType, final int address, final int[] data)
    {
        // Erase the page buffer if requested
        if(eraseBuffCommandType >= 0) {

            // Wait until the NVM controller is no longer busy
            if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

            // Send the erase buffer command to the NVM controller
            if( !_pdi_nvmctl_write_reg_cmd( new int[] { eraseBuffCommandType } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);

            // Execute the operation
            if( !_pdi_nvmctl_write_reg_ctrla( new int[] { PDI_NVMCTRL_CTRLA_CMDEX } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);

        } // if

        // Write the page buffer if requested
        if(writeBuffCommandType >= 0 && data.length > 0) {

            // Wait until the NVM controller is no longer busy
            if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

            // Send the write buffer command to the NVM controller
            if( !_pdi_nvmctl_write_reg_cmd( new int[] { writeBuffCommandType } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);

            // Load the PDI pointer register with the start address
            if( !_updi_tx_cmd_st_ptr_addr32(address) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrPtrAdr, ProgClassName);

            // Send the repeat command as needed
            if(data.length > 1) {
                if( !_pdi_tx_cmd_repeat(data.length) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrRepCnt, ProgClassName);
            }

            // Write the bytes
            if( !_pdi_tx_cmd_st_ptr_inc_data8(data) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdNVMMem, ProgClassName);

        } // if

        // Write the page if requested
        if(writePageCommandType >= 0) {

            // Wait until the NVM controller is no longer busy
            if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

            // Send the write page command to the NVM controller
            if( !_pdi_nvmctl_write_reg_cmd( new int[] { writePageCommandType } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);

            // Execute the operation
            if( !_pdi_tx_cmd_sts_addr32_data8( address, new int[] { 0x00 } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMMem, ProgClassName);

        } // if

        // Wait until the NVM controller is no longer busy
        if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

        // Done
        return true;
    }

    private boolean _pdi_nvmctl_write_memory(final int writeCommandType, final int address, final int data)
    {
        for(int r = 0; r < PDI_RETRY_COUNT_WRITE_MEMORY; ++r) {
            if( _pdi_nvmctl_write_memory_impl(writeCommandType, address, data) ) return true;
            _pdi_send_break();
        }

        return false;
    }

    private boolean _pdi_nvmctl_write_memory(final int eraseBuffCommandType, final int writeBuffCommandType, final int writePageCommandType, final int address, final int[] data)
    {
        for(int r = 0; r < PDI_RETRY_COUNT_WRITE_MEMORY; ++r) {
            if( _pdi_nvmctl_write_memory_impl(eraseBuffCommandType, writeBuffCommandType, writePageCommandType, address, data) ) return true;
            _pdi_send_break();
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int     _baudrate   = 0;

    private boolean _inProgMode = false;
    private boolean _chipErased = false;

    public ProgPDI(final USB2GPIO usb2gpio, final Config config) throws Exception
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

        for(int i = 0; i < _config.memoryFuse.address.length; ++i) {
            if(_config.memoryFuse.address[i] <  0) continue;
            if(_config.memoryFuse.size   [i] != 1) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFSize   , ProgClassName);
            if(_config.memoryFuse.bitMask[i] <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMCFBitMask, ProgClassName);
        }

        if(_config.memoryLockBits.address <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMLBAddress, ProgClassName);
        if(_config.memoryLockBits.size    != 1) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMLBSize   , ProgClassName);
        if(_config.memoryLockBits.bitMask <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMLBBitMask, ProgClassName);
    }

    public Config config()
    { return _config; }

    public boolean begin(final int baudrate)
    {
        // ##### ??? TODO : Add TX->RX test for PDI ??? #####

        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Fix the baudrate
             if(baudrate <=  86400) _baudrate =  57600;
        else if(baudrate <= 172800) _baudrate = 115200;
        else if(baudrate <= 345600) _baudrate = 230400;
        else if(baudrate <= 691200) _baudrate = 460800;
        else                        _baudrate = 921600;

        // Clear flag
        _chipErased = false;

        // Enable mode
        if(_usb2gpio instanceof USB_GPIO) {
            if( !( (USB_GPIO) _usb2gpio ).pcf8574Enable_PDI() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitPCF8574, ProgClassName);
        }

        // Initialize the USRT in PDI mode using the requested baudrate
        for(int i = 0; i < 2; ++i) {
            // Use hardware USRT mode
            if( _usb2gpio.usrtSetImplMode(USB2GPIO.ImplMode.Hardware) ) {
                // Check if the implementation supports ATxmega PDI mode
                if( !_usb2gpio.usrtIsXMegaPDIModeSupported() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_XmgPDINS, ProgClassName);
                // Check if the implementation supports enabling and disabling the Tx line on request
                if( !_usb2gpio.usrtIsEnablingDisablingTxSupported() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_TxEnDsNS, ProgClassName);
                // Initialize the USRT
                if( _usb2gpio.usrtBegin_PDI(USB2GPIO.UXRTMode._8E2, USB2GPIO.SSMode.ActiveLow, _baudrate) ) {
                    break;
                }
                // Error initializing the USRT
                else {
                    // Exit if this is the 2nd initialization attempt
                    if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitUSRT, ProgClassName);
                }
            }
            // Error selecting hardware USRT mode
            else {
                // Exit if this is the 2nd initialization attempt
                if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailSelHWUSRT, ProgClassName);
            }
            // Uninitialize the USRT and try again
            _usb2gpio.usrtEnd();
        }

        /*
        SysUtil.stdDbg().println("### TEST ###");
        if(true) {
            for(int i = 0; i < 100000; ++i) _pdi_write( new int[] { 0xAA } );
            _inProgMode = true;
            return true;
        }
        //*/

        // Intialize
        try {

            // Put the device in reset state
            if( !_pdi_set_device_reset_state(true) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailPDI_ResetStt, ProgClassName);

            // Set and verify the guard time
            if(true) {
                // Set the guard time
                int guardTime = CTRL_GTVAL_128B;
                if( USB_GPIO.isHWv2(_usb2gpio) ) {
                    switch(_baudrate) {
                        case  57600 : guardTime = CTRL_GTVAL_016B; break;
                        case 115200 : guardTime = CTRL_GTVAL_016B; break;
                        case 230400 : guardTime = CTRL_GTVAL_016B; break;
                        case 460800 : guardTime = CTRL_GTVAL_032B; break;
                        case 921600 : guardTime = CTRL_GTVAL_064B; break;
                    }
                }
                /*
                try {
                    if( ( (USB_GPIO) _usb2gpio ).isHWv2() ) switch(_baudrate) {
                        case  57600 : guardTime = CTRL_GTVAL_016B; break;
                        case 115200 : guardTime = CTRL_GTVAL_016B; break;
                        case 230400 : guardTime = CTRL_GTVAL_016B; break;
                        case 460800 : guardTime = CTRL_GTVAL_032B; break;
                        case 921600 : guardTime = CTRL_GTVAL_064B; break;
                    }
                }
                catch(final ClassCastException cce) {
                                      guardTime = CTRL_GTVAL_128B;
                }
                */
                if( !_pdi_tx_cmd_stcs(PDI_CS_CTRL, guardTime) ) USB2GPIO.TansmitError.throwTansmitError_usrtTx(Texts.ProgXXX_FailPDI_Tx, ProgClassName);
                // Check if it has been successfully modified
                final int res = _pdi_tx_rx_cmd_ldcs(PDI_CS_CTRL);
                if(res < 0) USB2GPIO.TansmitError.throwTansmitError_usrtTx(Texts.ProgXXX_FailPDI_TxRx, ProgClassName);
                if( (res & CTRL_GTVAL_MASK) != guardTime ) USB2GPIO.TansmitError.throwTansmitError_readInvalidValue(Texts.ProgXXX_FailPDI_RInvVal, ProgClassName);
                /*
                SysUtil.stdDbg().println( "@@@ SET GUARD TIME : " + res );
                //*/
            }

            // Enable NVM programming
            if( !_pdi_enable_nvmprog() ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailPDI_EnProg, ProgClassName);

        } // try
        catch(final USB2GPIO.TansmitError e) {

            // Uninitialize the USRT
            _usb2gpio.usrtEnd();
            // Notify error
            return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_Init, ProgClassName);
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

        // Commit EEPROM, disable NVM programming, pull out the device of reset state, and uninitialize the USRT
        final boolean resCommitEEPROM  =  commitEEPROM();
        final boolean resDisableNVMPrg = _pdi_disable_nvmprog();
        final boolean resPDIResetState = _pdi_set_device_reset_state(false);
        final boolean resUSRTEnd       = _usb2gpio.usrtEnd();

        // Disable mode
        boolean resDisMode = true;

        if(_usb2gpio instanceof USB_GPIO) {
            resDisMode = ( (USB_GPIO) _usb2gpio ).pcf8574Disable();
        }

        // Clear flag
        _inProgMode = false;

        // Check for error(s)
        if(!resCommitEEPROM || !resDisableNVMPrg || !resPDIResetState || !resUSRTEnd || !resDisMode) {
            if(!resCommitEEPROM ) USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMCEPRM , ProgClassName);
            if(!resDisableNVMPrg) USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotD  , ProgClassName);
            if(!resPDIResetState) USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_ResetStt , ProgClassName);
            if(!resUSRTEnd      ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitUSRT   , ProgClassName);
            if(!resDisMode      ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPCF8574, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _mcuSignature = null;

    @Override
    public boolean supportSignature()
    { return false; }

    @Override
    public boolean readSignature()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Read the signature
        _mcuSignature = _pdi_nvmctl_read_memory(_config.memorySignature.address, 3);

        // Done
        return _mcuSignature != null;
    }

    @Override
    public boolean verifySignature(final int[] signatureBytes)
    {
        // Error if the signature has not been read
        if(_mcuSignature == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Compare the signature
        return Arrays.equals(_mcuSignature, signatureBytes);
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

    // NOTE : This function erases flash, EEPROM, and lock bits
    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // Wait until the NVM controller is no longer busy
        if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

        // Send the chip erase command to the NVM controller
        if( !_pdi_nvmctl_write_reg_cmd( new int[] { NVM_CMD_CHIP_ERASE } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);

        // Execute the operation
        if( !_pdi_nvmctl_write_reg_ctrla( new int[] { PDI_NVMCTRL_CTRLA_CMDEX } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_WrNVMReg, ProgClassName);

        // Wait until the NVM controller is no longer busy
        if( !_pdi_wait_nvmctl_busy() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotR, ProgClassName);

        // Wait until NVM programming is enabled again
        if( !_pdi_wait_nvmprog_enabled_or_disabled(true) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMNotE, ProgClassName);

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
        final int     ChunkSize  = Math.max(2, _config.memoryFlash.pageSize / 4);

        final boolean notAligned = (numBytes % ChunkSize) != 0;
        final int     numChunks  = (numBytes / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        final boolean verify = (refData != null);
              int     rdbIdx = 0;
              int     verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads  = Math.min(ChunkSize, numBytes - rdbIdx);

            final int rdbIdxPrv = rdbIdx;
            final int verIdxPrv = verIdx;

            for(int r = 0; r < (verify ? PDI_RETRY_COUNT_VERIFY_ERROR : 1); ++ r) {

                // Read the chunk bytes
                int[] cbytes = null;

                if(verify) {
                    cbytes = _pdi_nvmctl_read_memory(_config.memoryFlash.address + sa + c * ChunkSize, numReads);
                    if(cbytes == null) {
                        USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdByte, ProgClassName);
                        return -1;
                    }
                }
                else {
                    for(int t = 0; t < PDI_RETRY_COUNT_READ_FLASH; ++t) {
                        // 1st read
                        if( !_pdi_nvmctl_reset() ) return -1;
                        final int[] _1bytes = _pdi_nvmctl_read_memory(_config.memoryFlash.address + sa + c * ChunkSize, numReads);
                        if(_1bytes == null) {
                            USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdByte, ProgClassName);
                            return -1;
                        }
                        // 2nd read
                        if( !_pdi_nvmctl_reset() ) return -1;
                        final int[] _2bytes = _pdi_nvmctl_read_memory(_config.memoryFlash.address + sa + c * ChunkSize, numReads);
                        if(_2bytes == null) {
                            USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdByte, ProgClassName);
                            return -1;
                        }
                        // Compare the bytes from the two reads
                        if( Arrays.equals(_1bytes, _2bytes) ) {
                            cbytes = _1bytes;
                            break;
                        }
                        else {
                            if(t == PDI_RETRY_COUNT_READ_FLASH - 1) return -1;
                            if( !_pdi_recover() ) return -1;
                        }
                    } // for
                }

                // Process the chunk bytes
                int verBadIdx = -1;

                for(int b = 0; b < numReads; b += 2) {

                    // Store the bytes to the result buffer
                    _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b    ];
                    _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b + 1];

                    // Compare the bytes as needed
                    if(verify && verIdx < refData.length) {
                        if( verBadIdx < 0 && _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) { verBadIdx = verIdx; break; }
                        ++verIdx;
                        if( verBadIdx < 0 && _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) { verBadIdx = verIdx; break; }
                        ++verIdx;
                    }

                } // for b

                if(verBadIdx == -1) {
                    // Call the progress callback function for the current value as many times as necessary
                    pcb.callProgressCallbackCurrentMulti(progressCallback, nb, numReads / 2);
                    // Verify OK
                    break;
                }
                else {
                    // Retry the verify as needed
                    if(r == PDI_RETRY_COUNT_VERIFY_ERROR - 1) {
                        return verBadIdx;
                    }
                    else {
                      //if( !_pdi_reset_device() ) return -1;
                        _pdi_send_break();
                        if( !readSignature() ) return -1;
                        rdbIdx = rdbIdxPrv;
                        verIdx = verIdxPrv;
                    }
                }

            } // for r

        } // for c

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return numBytes;
    }

    @Override
    public boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(null, startAddress, numBytes, progressCallback) == numBytes; }

    private boolean _writeFlashPage(final boolean bootSection, final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
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

        // Determine the write page command types
        final int writePageCommandType_app  = _chipErased ? NVM_CMD_WRITE_APP_SECTION_PAGE  : NVM_CMD_ERASE_AND_WRITE_APP_SECTION_PAGE ;
        final int writePageCommandType_boot = _chipErased ? NVM_CMD_WRITE_BOOT_SECTION_PAGE : NVM_CMD_ERASE_AND_WRITE_BOOT_SECTION_PAGE;

        // Write the pages
        int datIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            // ##### !!! TODO : Skip writing the page if it is blank (its contents are all 'FlashMemory_EmptyValue') !!! #####

            // Write the page
            if( !_pdi_nvmctl_write_memory(
                NVM_CMD_ERASE_FLASH_PAGE_BUFFER                                   ,
                NVM_CMD_LOAD_FLASH_PAGE_BUFFER                                    ,
                bootSection ? writePageCommandType_boot : writePageCommandType_app,
                _config.memoryFlash.address + cpgAddr                             ,
                USB2GPIO.ba2ia(data, datIdx, _config.memoryFlash.pageSize)
            ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMExErr, ProgClassName);

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

    public boolean writeFlash(final boolean bootSection, final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback)
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
        return _writeFlashPage(bootSection, anbr.buff, sa, anbr.nb, progressCallback);
    }

    @Override
    public boolean writeFlash(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return writeFlash(false, data, startAddress, numBytes, progressCallback); }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[]     _eepromBuffer = null;
    private boolean[] _eepromFDirty = null;

    private int[] _readEEPROM_impl(final int startAddress, final int numBytes)
    {
        // Prepare the result buffer
        final int[] eepromBuffer = new int[numBytes];

        // Determine the number of chunks
        final int ChunkSize  = Math.max(2, numBytes / 8);
        final int numChunks  = numBytes / ChunkSize;

        if( (numChunks * ChunkSize) != numBytes ) return null;

        // Read the bytes
        int rdbIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (EEPROM can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, numBytes - rdbIdx);

            for(int r = 0; r < PDI_RETRY_COUNT_READ_EEPROM; ++r) {

                // Read the chunk bytes
                if( !_pdi_nvmctl_reset() ) return null;

                final int[] cbytes = _pdi_nvmctl_read_memory(NVM_CMD_READ_EEPROM, startAddress + c * ChunkSize, numReads);

                if(cbytes == null) {
                    USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdByte, ProgClassName);
                    return null;
                }

                // Read the chunk bytes again for verification
                if( !_pdi_nvmctl_reset() ) return null;

                final int[] vbytes = _pdi_nvmctl_read_memory(NVM_CMD_READ_EEPROM, startAddress + c * ChunkSize, numReads);

                if(vbytes == null) {
                    USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdByte, ProgClassName);
                    return null;
                }

                // Compare the bytes from the two reads
                if( !Arrays.equals(cbytes, vbytes) ) {
                    if( !_pdi_recover() ) return null;
                    continue;
                }

                // Process the chunk bytes
                for(int b = 0; b < numReads; b += 2) {

                    // Store the bytes to the result buffer
                    eepromBuffer[rdbIdx++] = cbytes[b    ];
                    eepromBuffer[rdbIdx++] = cbytes[b + 1];

                } // for b

                // Done for the current chunk
                break;

            } // for r

        } // for c

        // Done
        return (rdbIdx < numBytes) ? null : eepromBuffer;
    }

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
            // Read the bytes
            _eepromBuffer = _readEEPROM_impl(_config.memoryEEPROM.address, _config.memoryEEPROM.totalSize);
            if(_eepromBuffer == null) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_RdByte, ProgClassName);
                return -1;
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

            // Write the page
            final int[] buff = Arrays.copyOfRange(_eepromBuffer, i, i + _config.memoryEEPROM.pageSize);

            if( !_pdi_nvmctl_write_memory(
                NVM_CMD_ERASE_EEPROM_PAGE_BUFFER   ,
                NVM_CMD_LOAD_EEPROM_PAGE_BUFFER    ,
                NVM_CMD_ERASE_AND_WRITE_EEPROM_PAGE,
                _config.memoryEEPROM.address + i   ,
                buff
            ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMExErr, ProgClassName);

        } // for

        // Mark everything as not dirty
        Arrays.fill(_eepromFDirty, false);

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

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
                for(int r = 0; r < PDI_RETRY_COUNT_READ_FUSE; ++r) {

                    // 1st read
                    if( !_pdi_nvmctl_reset() ) return null;
                    final int[] res1 = _pdi_nvmctl_read_memory(NVM_CMD_READ_FUSE, _config.memoryFuse.address[i], 1);
                    if(res1 == null) return null;

                    // 2nd read
                    if( !_pdi_nvmctl_reset() ) return null;
                    final int[] res2 = _pdi_nvmctl_read_memory(NVM_CMD_READ_FUSE, _config.memoryFuse.address[i], 1);
                    if(res2 == null) return null;

                    // 3rd read
                    if( !_pdi_nvmctl_reset() ) return null;
                    final int[] res3 = _pdi_nvmctl_read_memory(NVM_CMD_READ_FUSE, _config.memoryFuse.address[i], 1);
                    if(res3 == null) return null;

                    // Compare the results from the three reads
                    if(res1[0] == res2[0] && res1[0] == res3[0]) {
                        fuses[i] = res1[0] & _config.memoryFuse.bitMask[i];
                        break;
                    }
                    else {
                        if(r == PDI_RETRY_COUNT_READ_FUSE - 1) return null;
                        if( !_pdi_recover() ) return null;
                    }

                } // for
            }
            else {
                fuses[i] = -1;
            }

        } // for

        // Return the bytes
        return fuses;
    }

    public boolean writeFuses(final int[] values)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Read the original fuses
        final int[] origFuses = readFuses();
        if(origFuses == null) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMExErr, ProgClassName);

        // Write the bytes as needed
        for(int i = 0; i < _config.memoryFuse.address.length; ++i) {

            // Skip fuses that are unused or that the user does not want to change
            if(_config.memoryFuse.address[i] < 0 || values[i] < 0) continue;

            // Compute the new value
            final int newValue = values[i] & _config.memoryFuse.bitMask[i];

            // Skip fuses with unchanged values
            if(newValue == origFuses[i]) continue; // Skip those with the same value

            // Write the fuse
            if( !_pdi_nvmctl_write_memory(
                                    // NOTE : According to the datasheet, all reserved bits must be set to 1
                NVM_CMD_WRITE_FUSE, _config.memoryFuse.address[i], newValue | ~_config.memoryFuse.bitMask[i]
            ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMExErr, ProgClassName);

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long readLockBits()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Read the lock bits
        for(int r = 0; r < PDI_RETRY_COUNT_READ_LOCK_BITS; ++r) {

            // 1st read
            if( !_pdi_nvmctl_reset() ) return -1;
            final int[] res1 = _pdi_nvmctl_read_memory(_config.memoryLockBits.address, 1);
            if(res1 == null) return -1;

            // 2nd read
            if( !_pdi_nvmctl_reset() ) return -1;
            final int[] res2 = _pdi_nvmctl_read_memory(_config.memoryLockBits.address, 1);
            if(res2 == null) return -1;

            // 3rd read
            if( !_pdi_nvmctl_reset() ) return -1;
            final int[] res3 = _pdi_nvmctl_read_memory(_config.memoryLockBits.address, 1);
            if(res3 == null) return -1;

            // Compare the results from the three reads
            if(res1[0] == res2[0] && res1[0] == res3[0]) {
                return res1[0] & _config.memoryLockBits.bitMask;
            }
            else {
                if( !_pdi_recover() ) return -1;
            }

        } // for

        // Error
        return -1;
    }

    @Override
    public boolean writeLockBits(final long value)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Read the original lock bits
        final long origLockBits = readLockBits();
        if(origLockBits < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMExErr, ProgClassName);

        // Write the lock bits as needed
        final long newLockBits = value & _config.memoryLockBits.bitMask;

        if(newLockBits != origLockBits) {
            if( !_pdi_nvmctl_write_memory( NVM_CMD_WRITE_LOCK_BITS, _config.memoryLockBits.address, (int) newLockBits ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailPDI_NVMExErr, ProgClassName);
        }

        // Done
        return true;
    }

} // class ProgPDI
