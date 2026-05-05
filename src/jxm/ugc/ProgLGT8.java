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
import jxm.xb.*;


/*
 * This class and its related classes are written partially based on the algorithms and information found from:
 *
 *     LGT8F328P ISP programming
 *     https://ceptimus.co.uk/index.php/2022/06/29/lgt8f328p-isp-programming
 *
 *     LGTISP--LGT单片机下载器 - 立创开源硬件平台
 *     https://oshwhub.com/brother_yan/LGTISP
 *
 *     ----------------------------------------------------------------------------------------------------
 *
 *     LGTISP
 *     LGT8Fx8P ISP Download Protocol Implementation
 *     https://github.com/brother-yan/LGTISP/tree/master
 *     The 2-Clause BSD License (http://www.opensource.org/licenses/bsd-license.php)
 *
 *     LGT8Fx8p ISP protocol implementation
 *     https://github.com/SuperUserNameMan/LGTISP
 *     The 2-Clause BSD License (http://www.opensource.org/licenses/bsd-license.php)
 *
 *     LarduinoISP
 *     https://github.com/LGTMCU/LarduinoISP
 *     The 2-Clause BSD License (http://www.opensource.org/licenses/bsd-license.php)
 *
 *     Arduino Hardware Support Package for LGT8F's
 *     https://github.com/DavidGuo-CS/OSOYOO_Arduino/tree/main
 *     Copyright (C) 2017 OSOYOO
 *     MIT License
 *
 * ~~~ Last accessed & checked on 2024-10-07 ~~~
 */
public class ProgLGT8 implements IProgCommon {

    /*
     * ######################################### !!! WARNING !!! #########################################
     * Due to the nature of the LGT8 SWD protocol, it can only be programmed easily and effectively using
     * hardware-assisted bit-banging.
     * ######################################### !!! WARNING !!! #########################################
     */

    /*
     * ######################################### !!! WARNING !!! #########################################
     * Once a newly programmed LGT8 MCU is powered-down, its flash memory is automatically locked.
     * Therefore, verifying a program will be practically impossible after a power-cycle.
     * ######################################### !!! WARNING !!! #########################################
     */

    /*
     * Transfer speed:
     *     # Using USB_ISS            : not supported
     *     # Using JxMake DASA        : not supported
     *     # Using JxMake USB-GPIO    : up to   ~75 ...   ~90 bytes per second (depending on the target and operation); when SWD_USE_XB_FAST is 'false'
     *                                  up to ~1190 ... ~1570 bytes per second (depending on the target and operation); when SWD_USE_XB_FAST is 'true'
     *     # Using JxMake USB-GPIO II : up to   ~70 ...   ~90 bytes per second (depending on the target and operation); when SWD_USE_XB_FAST is 'false'
     *                                  up to ~1480 ... ~1950 bytes per second (depending on the target and operation); when SWD_USE_XB_FAST is 'true'
     */

    private static final String ProgClassName = "ProgLGT8";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final byte FlashMemory_EmptyValue = (byte) 0xFF;

    @SuppressWarnings("serial")
    public static class Config extends SerializableDeepClone<Config> {

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        public static class MemoryFlash implements Serializable {
            public int   totalSize    = 0;
            public int   pageSize     = 0;
            public int   numPages     = 0;

            public int[] readDataBuff = null;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final MemoryFlash memoryFlash = new MemoryFlash();

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'LGT8*()' functions ??? #####


public static Config LGT8F328P()
{
    // Instantiate the configuration object
    final Config config = new Config();

    // Set the values
    config.memoryFlash.totalSize = 32768;
    config.memoryFlash.pageSize  =   128;
    config.memoryFlash.numPages  =   256;

    // Return the configuration object
    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final USB2GPIO _usb2gpio;
    private final Config   _config;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _inProgMode = false;
    private boolean _chipErased = false;

    public ProgLGT8(final USB2GPIO usb2gpio, final Config config) throws Exception
    {
        // Store the objects
        _usb2gpio = usb2gpio;
        _config   = config.deepClone();

        // Check the configuration values
        if(_config.memoryFlash.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFTotSize , ProgClassName);
        if(_config.memoryFlash.pageSize  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSize, ProgClassName);
        if(_config.memoryFlash.numPages  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFNumPages, ProgClassName);

        if(_config.memoryFlash.pageSize * _config.memoryFlash.numPages != _config.memoryFlash.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSpec, ProgClassName);
    }

    public Config config()
    { return _config; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean SWD_USE_XB      = true;              // !!! WARNING : If this is set to 'false', the entire process will become extremely slow !!!!
    public static boolean SWD_USE_XB_FAST = true & SWD_USE_XB;

    private boolean _swd_idle(final int cnt)
    {
        if(SWD_USE_XB) {
            final int   num = cnt / 8;
            final int   ext = cnt - num * 8;
            final int[] seq = new int[ (num * 2) + ( (ext > 0) ? 2 : 0 ) ];
                  int   idx = 0;
            for(int i = 0; i < num; ++i) {
                seq[idx++] = 7;
                seq[idx++] = 0xFF;
            }
            if(ext > 0) {
                seq[idx++] = ext - 1;
                seq[idx++] = 0xFF;
            }
            if( !_usb2gpio.spiXBTransfer(seq) ) return false;
        }

        else {
            for(int i = 0; i < cnt; ++i) {
                if( !_usb2gpio.spiSetBreak(true, false) ) return false;
                if( !_usb2gpio.spiSetBreak(true, true ) ) return false;
            }
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _swd_writeByte(final boolean start, final int value, final boolean stop)
    {
        if(SWD_USE_XB) {
                final int[] seq = new int[ (start ? 2 : 0) + 2 + 2 ];
                      int   idx = 0;
                if(start) {
                    seq[idx++] = 0; seq[idx++] = 0;
                }
                    seq[idx++] = 7; seq[idx++] = XCom._RU08(value);
                    seq[idx++] = 0; seq[idx++] = stop ? 1 : 0;
                return _usb2gpio.spiXBTransfer(seq);
        }

        else {
            if(start) {
                if( !_usb2gpio.spiSetBreak(false, false) ) return false;
                if( !_usb2gpio.spiSetBreak(false, true ) ) return false;
            }

            for(int i = 0; i < 8; ++i) {
                final boolean bit = ( value & (0b00000001 << i) ) != 0;
                if( !_usb2gpio.spiSetBreak(bit  , false) ) return false;
                if( !_usb2gpio.spiSetBreak(bit  , true ) ) return false;
            }

                if( !_usb2gpio.spiSetBreak(stop , false) ) return false;
                if( !_usb2gpio.spiSetBreak(stop , true ) ) return false;
        }

        return true;
    }

    private boolean _swd_write1Byte(final int value)
    { return _swd_writeByte(true, value, true); }

    private boolean _swd_write2Bytes(final int value1, final int value2)
    {
        if(SWD_USE_XB) {
            return _usb2gpio.spiXBTransfer( new int[] {
                                       0, 0,
                7, XCom._RU08(value1), 0, 0,
                7, XCom._RU08(value2), 0, 1
            } );
        }

        else {
            if( !_swd_writeByte(true , value1, false) ) return false;
            if( !_swd_writeByte(false, value2, true ) ) return false;
            return true;
        }
    }

    private boolean _swd_write4Bytes(final int value1, final int value2, final int value3, final int value4)
    {
        if(SWD_USE_XB) {
            return _usb2gpio.spiXBTransfer( new int[] {
                                       0, 0,
                7, XCom._RU08(value1), 0, 0,
                7, XCom._RU08(value2), 0, 0,
                7, XCom._RU08(value3), 0, 0,
                7, XCom._RU08(value4), 0, 1
            } );
        }

        else {
            if( !_swd_writeByte(true , value1, false) ) return false;
            if( !_swd_writeByte(false, value2, false) ) return false;
            if( !_swd_writeByte(false, value3, false) ) return false;
            if( !_swd_writeByte(false, value4, true ) ) return false;
            return true;
        }
    }

    private boolean _swd_write5Bytes(final int value1, final int value2, final int value3, final int value4, final int value5)
    {
        if(SWD_USE_XB) {
            return _usb2gpio.spiXBTransfer( new int[] {
                                       0, 0,
                7, XCom._RU08(value1), 0, 0,
                7, XCom._RU08(value2), 0, 0,
                7, XCom._RU08(value3), 0, 0,
                7, XCom._RU08(value4), 0, 0,
                7, XCom._RU08(value5), 0, 1
            } );
        }

        else {
            if( !_swd_writeByte(true , value1, false) ) return false;
            if( !_swd_writeByte(false, value2, false) ) return false;
            if( !_swd_writeByte(false, value3, false) ) return false;
            if( !_swd_writeByte(false, value4, false) ) return false;
            if( !_swd_writeByte(false, value5, true ) ) return false;
            return true;
        }
    }

    private boolean _swd_write6Bytes(final int value1, final int value2, final int value3, final int value4, final int value5, final int value6)
    {
        if(SWD_USE_XB) {
            return _usb2gpio.spiXBTransfer( new int[] {
                                       0, 0,
                7, XCom._RU08(value1), 0, 0,
                7, XCom._RU08(value2), 0, 0,
                7, XCom._RU08(value3), 0, 0,
                7, XCom._RU08(value4), 0, 0,
                7, XCom._RU08(value5), 0, 0,
                7, XCom._RU08(value6), 0, 1
            } );
        }

        else {
            if( !_swd_writeByte(true , value1, false) ) return false;
            if( !_swd_writeByte(false, value2, false) ) return false;
            if( !_swd_writeByte(false, value3, false) ) return false;
            if( !_swd_writeByte(false, value4, false) ) return false;
            if( !_swd_writeByte(false, value5, false) ) return false;
            if( !_swd_writeByte(false, value6, true ) ) return false;
            return true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _swd_readByte(final boolean start, final boolean stop)
    {
        if(SWD_USE_XB) {
                final int[] seq = new int[ (start ? 2 : 0) + 2 + 2 ];
                      int   idx = 0;
                if(start) {
                    seq[idx++] = 0; seq[idx++] = 0;
                }
                    seq[idx++] = 7; seq[idx++] = 0xFF;
                    seq[idx++] = 0; seq[idx++] = stop ? 1 : 0;
                if( !_usb2gpio.spiXBTransfer(seq) ) return -1;

                return XCom._RU08(seq[start ? 3 : 1]);
        }

        else {
            if(start) {
                if( !_usb2gpio.spiSetBreak(false, false) ) return -1;
                if( !_usb2gpio.spiSetBreak(false, true ) ) return -1;
            }

            int value = 0;
            for(int i = 0; i < 8; ++i) {
                final int v1 = _usb2gpio.spiSetBreakExt(true , false);
                final int v2 = _usb2gpio.spiSetBreakExt(true , true );
                if(v1 < 0 || v2 < 0) return -1;
                if(v1 != 0) value |= (0b00000001 << i);
            }

                if( !_usb2gpio.spiSetBreak(stop , false) ) return -1;
                if( !_usb2gpio.spiSetBreak(stop , true ) ) return -1;

            return value;
        }
    }

    private int _swd_read1Byte()
    { return _swd_readByte(true, true); }

    private int[] _swd_read2Bytes()
    {
        if(SWD_USE_XB) {
            final int[] seq = new int[] {
                         0, 0,
                7, 0xFF, 0, 0,
                7, 0xFF, 0, 1
            };
            if( !_usb2gpio.spiXBTransfer(seq) ) return null;

            return new int[] {
                XCom._RU08(seq[3]),
                XCom._RU08(seq[7])
            };
        }

        else {
            return new int[] {
                _swd_readByte(true , false),
                _swd_readByte(false, true )
            };
        }
    }

    private int[] _swd_read3Bytes()
    {
        if(SWD_USE_XB) {
            final int[] seq = new int[] {
                         0, 0,
                7, 0xFF, 0, 0,
                7, 0xFF, 0, 0,
                7, 0xFF, 0, 1
            };
            if( !_usb2gpio.spiXBTransfer(seq) ) return null;

            return new int[] {
                XCom._RU08(seq[ 3]),
                XCom._RU08(seq[ 7]),
                XCom._RU08(seq[11])
            };
        }

        else {
            return new int[] {
                _swd_readByte(true , false),
                _swd_readByte(false, false),
                _swd_readByte(false, true )
            };
        }
    }

    private int[] _swd_read4Bytes()
    {
        if(SWD_USE_XB) {
            final int[] seq = new int[] {
                         0, 0,
                7, 0xFF, 0, 0,
                7, 0xFF, 0, 0,
                7, 0xFF, 0, 0,
                7, 0xFF, 0, 1
            };
            if( !_usb2gpio.spiXBTransfer(seq) ) return null;

            return new int[] {
                XCom._RU08(seq[ 3]),
                XCom._RU08(seq[ 7]),
                XCom._RU08(seq[11]),
                XCom._RU08(seq[15])
            };
        }

        else {
            return new int[] {
                _swd_readByte(true , false),
                _swd_readByte(false, false),
                _swd_readByte(false, false),
                _swd_readByte(false, true )
            };
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _swd_init()
    {
        // Initialize SPI
        for(int i = 0; i < 2; ++i) {
            // Initialize the SPI
            if( _usb2gpio.spiBegin(USB2GPIO.SPIMode._2, USB2GPIO.SSMode.ActiveLow, 0) ) break;
            // Error initializing the SPI - exit if this is the 2nd initialization attempt
            if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSPI, ProgClassName);
            // Uninitialize the SPI and try again
            _usb2gpio.spiEnd();
        }

        // Select the target
        if( !_usb2gpio.spiSelectSlave() ) return false;
        SysUtil.sleepMS(10);

        // Initialize SWD
        if( !_usb2gpio.spiSetBreak(true, true) ) return false;
        if( !_swd_idle(80) ) return false;

        // Done
        return true;
    }

    private boolean _swd_uninit()
    {
        // Halt CPU and lock flash after reset
        if( !_swd_write2Bytes(0xB1, 0x0D) ) return false;
        if( !_swd_idle       (2         ) ) return false;
        SysUtil.sleepUS(200);

        // Software reset
        if( !_swd_write2Bytes(0xB1, 0x0C) ) return false;
        if( !_swd_idle       (40        ) ) return false;

        // Unselect the target
        if( !_usb2gpio.spiDeselectSlave() ) return false;
        SysUtil.sleepMS(10);

        // Uninitialize SPI
        if( !_usb2gpio.spiEnd() ) return false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _swd_readSWDID()
    {
        int[] res;

        if(        !_swd_write1Byte(0xAE)           ) return null;
        if(        !_swd_idle      (4   )           ) return null;
        if( ( res = _swd_read4Bytes(    ) ) == null ) return null;
        if(        !_swd_idle      (4   )           ) return null;

        return res;
    }

    private int[] _swd_readGUID()
    {
        int[] res;

        if(        !_swd_idle      (10  )           ) return null;
        if(        !_swd_write1Byte(0xA8)           ) return null;
        if(        !_swd_idle      (4   )           ) return null;
        if( ( res = _swd_read4Bytes(    ) ) == null ) return null;
        if(        !_swd_idle      (4   )           ) return null;
        return res;
    }

    private boolean _swd_SWDEN()
    {
        if( !_swd_write5Bytes(0xD0, 0xAA, 0x55, 0xAA, 0x55) ) return false;
        if( !_swd_idle       (4                           ) ) return false;

        return true;
    }

    private boolean _swd_unlock0()
    {
        if( !_swd_write5Bytes(0xF0, 0x54, 0x51, 0x4A, 0x4C) ) return false;
        if( !_swd_idle       (4                           ) ) return false;

        return true;
    }

    private boolean _swd_unlock1()
    {
        if( !_swd_write5Bytes(0xF0, 0x00, 0x00, 0x00, 0x00) ) return false;
        if( !_swd_idle       (4                           ) ) return false;

        return true;
    }

    private boolean _swd_unlock2()
    {
        if( !_swd_write5Bytes(0xF0, 0x43, 0x40, 0x59, 0x5D) ) return false;
        if( !_swd_idle       (4                           ) ) return false;

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _swd_EEE_CSEQ(final int ctrl, final int addr16)
    {
        if( !_swd_write4Bytes( 0xB2, addr16 & 0xFF, ( (ctrl & 0x03) << 6 ) | ( (addr16 >>> 8) & 0x3F ), 0xC0 | (ctrl >>> 2) ) ) return false;
        if( !_swd_idle       ( 4                                                                                            ) ) return false;

        return true;
    }

    private boolean _swd_EEE_DSEQ(final int[] data8x4)
    {
        if( !_swd_write5Bytes( 0xB2, data8x4[0], data8x4[1], data8x4[2], data8x4[3] ) ) return false;
        if( !_swd_idle       ( 4                                                    ) ) return false;

        return true;
    }

    private boolean _swd_EEE_write(final int addr16, final int[] data8x4)
    {
        if( !_swd_EEE_DSEQ(data8x4     ) ) return false;
        if( !_swd_EEE_CSEQ(0x86, addr16) ) return false;
        if( !_swd_EEE_CSEQ(0xC6, addr16) ) return false;
        if( !_swd_EEE_CSEQ(0x86, addr16) ) return false;

        return true;
    }

    private int[] _swd_EEE_read(final int addr16)
    {
        int[] res;

        if(        !_swd_EEE_CSEQ  (0xC0, addr16)           ) return null;
        if(        !_swd_EEE_CSEQ  (0xE0, addr16)           ) return null;

        if(        !_swd_write1Byte(0xAA        )           ) return null;
        if( ( res = _swd_read4Bytes(            ) ) == null ) return null;
        if(        !_swd_idle      ( 4          )           ) return null;

        return res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final int[] _cmdSeqBuf = new int[4096];
    private       int   _cmdSeqIdx = 0;

    private int[] _xbseqGetBuffer()
    {
        final int len = _cmdSeqIdx;

        _cmdSeqIdx = 0;

        return XCom.arrayCopy(_cmdSeqBuf, 0, len);
    }

    private int[] _xbseqExecute()
    {
        final int[] seq = _xbseqGetBuffer();

        if( !_usb2gpio.spiXBTransfer(seq) ) return null;

        return seq;
    }

   ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _xbseqPut_idle(final int cnt)
    {
        final int   num = cnt / 8;
        final int   ext = cnt - num * 8;

        for(int i = 0; i < num; ++i) {
            _cmdSeqBuf[_cmdSeqIdx++] = 7;
            _cmdSeqBuf[_cmdSeqIdx++] = 0xFF;
        }

        if(ext > 0) {
            _cmdSeqBuf[_cmdSeqIdx++] = ext - 1;
            _cmdSeqBuf[_cmdSeqIdx++] = 0xFF;
        }
    }

    private void _xbseqPut_write1Byte(final int value)
    {
                                                                                                                   _cmdSeqBuf[_cmdSeqIdx++] = 0;
                                                                                                                   _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value ); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 1;
    }

    private void _xbseqPut_write4Bytes(final int value1, final int value2, final int value3, final int value4)
    {
                                                                                                                   _cmdSeqBuf[_cmdSeqIdx++] = 0;
                                                                                                                   _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value1); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value2); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value3); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value4); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 1;
    }

    private void _xbseqPut_write5Bytes(final int value1, final int value2, final int value3, final int value4, final int value5)
    {
                                                                                                                   _cmdSeqBuf[_cmdSeqIdx++] = 0;
                                                                                                                   _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value1); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value2); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value3); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value4); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = XCom._RU08(value5); _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 1;
    }

    private int _xbseqPut_read4Bytes()
    {
        final int prvIdx = _cmdSeqIdx;
                                                                                                                   _cmdSeqBuf[_cmdSeqIdx++] = 0;
                                                                                                                   _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = 0xFF              ; _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = 0xFF              ; _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = 0xFF              ; _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 0;
        _cmdSeqBuf[_cmdSeqIdx++] = 7; _cmdSeqBuf[_cmdSeqIdx++] = 0xFF              ; _cmdSeqBuf[_cmdSeqIdx++] = 0; _cmdSeqBuf[_cmdSeqIdx++] = 1;

        return prvIdx;
    }

    private void _xbseqPut_EEE_CSEQ(final int ctrl, final int addr16)
    {
        _xbseqPut_write4Bytes( 0xB2, addr16 & 0xFF, ( (ctrl & 0x03) << 6 ) | ( (addr16 >>> 8) & 0x3F ), 0xC0 | (ctrl >>> 2) );
        _xbseqPut_idle       ( 4                                                                                            );
    }

    private void _xbseqPut_EEE_DSEQ(final int[] data8x4)
    {
        _xbseqPut_write5Bytes( 0xB2, data8x4[0], data8x4[1], data8x4[2], data8x4[3] );
        _xbseqPut_idle       ( 4                                                    );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _xbputSeq_EEE_write(final int addr16, final int[] data8x4)
    {
        _xbseqPut_EEE_DSEQ(data8x4     );
        _xbseqPut_EEE_CSEQ(0x86, addr16);
        _xbseqPut_EEE_CSEQ(0xC6, addr16);
        _xbseqPut_EEE_CSEQ(0x86, addr16);
    }

    private int _xbputSeq_EEE_read(final int addr16)
    {
                           _xbseqPut_EEE_CSEQ  (0xC0, addr16);
                           _xbseqPut_EEE_CSEQ  (0xE0, addr16);

                           _xbseqPut_write1Byte(0xAA        );
        final int resIdx = _xbseqPut_read4Bytes(            );
                           _xbseqPut_idle      ( 4          );

        return resIdx;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _swdid = null;
    private int[] _guid  = null;

    private boolean _swd_chipErase()
    {
        if(_chipErased) return true; // Exit if the device is already erased

        if( !_swd_EEE_CSEQ(0x00, 1) ) return false;
        if( !_swd_EEE_CSEQ(0x98, 1) ) return false;
        if( !_swd_EEE_CSEQ(0x9A, 1) ) return false; // Erase the entire chip
        SysUtil.sleepMS(200);

        if( !_swd_EEE_CSEQ(0x8A, 1) ) return false;
        SysUtil.sleepMS(20);

        if( !_swd_EEE_CSEQ(0x88, 1) ) return false;
        if( !_swd_EEE_CSEQ(0x00, 1) ) return false;

        _chipErased = true; // Set flag

        return true;
    }

    /*
     * If the first 1kB of the flash is blank:
     *     # Calling this function with 'eraseFirst1kB' set to 'false' will enable reading the flash only after the first 1kB.
     *
     * If the first 1kB of the flash is not blank:
     *     # Calling this function with 'eraseFirst1kB' set to 'true' will erase the first 1kB of the flash and enable reading the entire flash.
     *
     * ----------------------------------------------------------------------------------------------------
     *
     * Outside the above condition the entire flash cannot be read!
     *
     * !!! It means verifying a program that starts from 0x00 will be practically impossible after a power-cycle !!!
     */
    private boolean _swd_chipUnprotect(final boolean eraseFirst1kB)
    {
            if( !_swd_EEE_CSEQ(0x00, 1) ) return false;
            if( !_swd_EEE_CSEQ(0x98, 1) ) return false;
        if(eraseFirst1kB) {
            if( !_swd_EEE_CSEQ(0x92, 1) ) return false; // Erase the first 1kB
            SysUtil.sleepMS(200);
        }

            if( !_swd_EEE_CSEQ(0x9E, 1) ) return false; // Unlock
            SysUtil.sleepMS(200);

            if( !_swd_EEE_CSEQ(0x8A, 1) ) return false;
            SysUtil.sleepMS(20);

            if( !_swd_EEE_CSEQ(0x88, 1) ) return false;
            if( !_swd_EEE_CSEQ(0x00, 1) ) return false;

        return true;
    }

    private boolean _swd_unlockChip(final boolean chipErase, final boolean eraseFirst1kB)
    {
        // Enable SWD
        if( !_swd_SWDEN() ) return false;

        // Re-read SWDID
        _swdid = _swd_readSWDID();

        if(_swdid == null) return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_RdSWDID, ProgClassName);

        /*
         * { 0x3E, 0xA2, 0x50, 0xE9 } indicates that this is the first SWD operation
         * { 0x3F, 0xA2, 0x50, 0xE9 } indicates that the SWD unlock operation has been performed before
         */
        if(_swdid[0] != 0x3E && _swdid[0] != 0x3F) return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_InvDev, ProgClassName);
        if(_swdid[0] == 0x3F && !chipErase       ) return true ; // Already unlocked

        // Unlock
        if( !_swd_unlock0() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_Unlock0, ProgClassName);

        if(chipErase) {
            if( !_swd_chipErase() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_ErsChip, ProgClassName);
        }
        else {
            if( !_swd_chipUnprotect(eraseFirst1kB) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_UPrChip, ProgClassName);
        }

        if( !_swd_unlock1    (                                  ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_Unlock1, ProgClassName);
        if( !_swd_write6Bytes(0xB1, 0x3D, 0x60, 0x0C, 0x00, 0x0F) ) return false;
        if( !_swd_idle       (40                                ) ) return false;
        if( !_swd_unlock2    (                                  ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_Unlock2, ProgClassName);

        if( !_swd_idle       (40                                ) ) return false;
        if( !_swd_write4Bytes(0xB1, 0x0C, 0x00, 0x17            ) ) return false;
        if( !_swd_idle       (40                                ) ) return false;

        _swdid[0] = 0x3F;

        if(!eraseFirst1kB) return true;

        //*
        // Read and check flag
        int[] flag;

        if(         !_swd_write1Byte(0xA9)           ) return false;
        if(         !_swd_idle      (4   )           ) return false;
        if( ( flag = _swd_read2Bytes(    ) ) == null ) return false;
        if(         !_swd_idle      (4   )           ) return false;

        if(flag[1] == 0x20) {
            if( !_swd_write6Bytes(0xB1, 0x3D, 0x20, 0x0C, 0x00, 0x0F) ) return false;
            if( !_swd_idle       (40                                ) ) return false;
        }
        else if(flag[1] != 0x60) {
            return false;
        }

        if( !_swd_write2Bytes(0xB1, 0x0D) ) return false;
        if( !_swd_idle       (2         ) ) return false;
        //*/

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _swd_writeFlashPage(final int addr16_, final int[] buff)
    {
        int addr16 = addr16_ / 4;

        if( !_swd_EEE_CSEQ(0x00, addr16) ) return false;
        if( !_swd_EEE_CSEQ(0x84, addr16) ) return false;
        if( !_swd_EEE_CSEQ(0x86, addr16) ) return false;

        for(int i = 0; i < buff.length; i += 4) {
            if( !_swd_EEE_write( addr16, new int[] {
                buff[i + 0],
                buff[i + 1],
                buff[i + 2],
                buff[i + 3]
            } ) ) return false;
            ++addr16;
        }

        if( !_swd_EEE_CSEQ(0x82, addr16 - 1) ) return false;
        if( !_swd_EEE_CSEQ(0x80, addr16 - 1) ) return false;
        if( !_swd_EEE_CSEQ(0x00, addr16 - 1) ) return false;

        return true;
    }

    private boolean _swd_readFlashPage(final int addr16_, final int[] buff)
    {
        int addr16 = addr16_ / 4;

        if( !_swd_EEE_CSEQ(0x00, 0x01) ) return false;

        for(int i = 0; i < buff.length; i += 4) {

            final int[] res = _swd_EEE_read(addr16);
            if(res == null) return false;

            ++addr16;

            buff[i + 0] = res[0];
            buff[i + 1] = res[1];
            buff[i + 2] = res[2];
            buff[i + 3] = res[3];
        }

        if( !_swd_EEE_CSEQ(0x00, 0x01) ) return false;

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _xb_writeFlashPage_fast(final int addr16_, final int[] buff)
    {
        // Use the standard function if XB fast mode is not enabled
        if(!SWD_USE_XB_FAST) return _swd_writeFlashPage(addr16_, buff);

        // Generate the command sequence
        int addr16 = addr16_ / 4;

        _xbseqPut_EEE_CSEQ(0x00, addr16);
        _xbseqPut_EEE_CSEQ(0x84, addr16);
        _xbseqPut_EEE_CSEQ(0x86, addr16);

        for(int i = 0; i < buff.length; i += 4) {
            _xbputSeq_EEE_write( addr16++, new int[] { buff[i + 0], buff[i + 1], buff[i + 2], buff[i + 3] } );
        }

        _xbseqPut_EEE_CSEQ(0x82, addr16 - 1);
        _xbseqPut_EEE_CSEQ(0x80, addr16 - 1);
        _xbseqPut_EEE_CSEQ(0x00, addr16 - 1);

        // Execute the command sequence
        return _xbseqExecute() != null;
    }

    private boolean _xb_readFlashPage_fast(final int addr16_, final int[] buff)
    {
        // Use the standard function if XB fast mode is not enabled
        if(!SWD_USE_XB_FAST) return _swd_readFlashPage(addr16_, buff);

        // Generate the command sequence
        final int[] resIdx = new int[buff.length / 4];
              int   addr16 = addr16_ / 4;

        _xbseqPut_EEE_CSEQ(0x00, 0x01);

        for(int i = 0; i < buff.length; i += 4) {
            resIdx[i / 4] = _xbputSeq_EEE_read(addr16++);
        }

        _xbseqPut_EEE_CSEQ(0x00, 0x01);

        // Execute the command sequence
        final int[] seq = _xbseqExecute();

        if(seq == null) return false;

        // Parse the result(s)
        for(int i = 0; i < buff.length; i += 4) {
            buff[i + 0] = XCom._RU08( seq[ resIdx[i / 4] +  3 ] );
            buff[i + 1] = XCom._RU08( seq[ resIdx[i / 4] +  7 ] );
            buff[i + 2] = XCom._RU08( seq[ resIdx[i / 4] + 11 ] );
            buff[i + 3] = XCom._RU08( seq[ resIdx[i / 4] + 15 ] );
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean begin()
    {
        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flag
        _chipErased = false;

        // Enable mode
        if(_usb2gpio instanceof USB_GPIO) {
            if( !( (USB_GPIO) _usb2gpio ).pcf8574Enable_LGT8() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitPCF8574, ProgClassName);
        }

        // Initialize SWD
        if( !_swd_init() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_Init, ProgClassName);

        // Read SWDID
        _swdid = _swd_readSWDID();

        if(_swdid == null) {
            _swd_uninit();
            return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_RdSWDID, ProgClassName);
        }

        // Read GUID
        _guid = _swd_readGUID();

        if(_guid == null) {
            _swd_uninit();
            return USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_RdGUID, ProgClassName);
        }

        /*
        for(final int i : _swdid) SysUtil.stdDbg().printf("%02X ", i); SysUtil.stdDbg().println(); // 3E A2 50 E9
        for(final int i : _guid ) SysUtil.stdDbg().printf("%02X ", i); SysUtil.stdDbg().println(); // 01 5D EE FF
        //*/

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

        // Uninitialize SWD
        final boolean resUninit = _swd_uninit();

        // Disable mode
        boolean resDisMode = true;

        if(_usb2gpio instanceof USB_GPIO) {
            resDisMode = ( (USB_GPIO) _usb2gpio ).pcf8574Disable();
        }

        // Clear flag
        _inProgMode = false;

        // Check for error(s)
        if(!resUninit || !resDisMode) {
            if(!resUninit ) USB2GPIO.notifyError(Texts.ProgXXX_FailLGT8_Uninit  , ProgClassName);
            if(!resDisMode) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPCF8574, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _mcuSignature = null;

    @Override
    public boolean supportSignature()
    { return true; }

    @Override
    public boolean readSignature()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Copy the SWID as the signature
        _mcuSignature = new int[] {
            _swdid[1],
            _swdid[2],
            _swdid[3]
        };

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

    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // Unlock the chip
        if( !_swd_unlockChip(true, false) ) return false;

        // Call the implementation function, just in case the above unlock does not actually perform a chip erase
        if( !_swd_chipErase() ) return false;

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
    { return 0; }

    @Override
    public byte _eepromMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    @Override
    public int[] _readDataBuff()
    { return _config.memoryFlash.readDataBuff; }

    private int _verifyReadFlashPage(final byte[] refData, final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_pageSize(sa, nb, _config.memoryFlash.pageSize, ProgClassName) ) return -1;

        // Get the number of pages to be written and the current page address
        final int numPages = nb / _config.memoryFlash.pageSize;
              int cpgAddr  = sa;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Write the pages
        final int[] pbytes = new int[_config.memoryFlash.pageSize];
              int   rbdIdx = 0;
              int   verIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            // Read the page
            if( !_xb_readFlashPage_fast(cpgAddr, pbytes) ) return -1;

            // Process the bytes
            for(int b = 0; b < _config.memoryFlash.pageSize; b += 2) {

                // Store the bytes to the result buffer
                data[rbdIdx + b    ] = (byte) pbytes[b    ];
                data[rbdIdx + b + 1] = (byte) pbytes[b + 1];

                /*
                SysUtil.stdDbg().printf("%02X\n", pbytes[b    ]);
                SysUtil.stdDbg().printf("%02X\n", pbytes[b + 1]);
                //*/

                // Compare the bytes as needed
                if(refData != null && verIdx < refData.length) {
                    if( data[verIdx] != refData[verIdx] ) return verIdx;
                    ++verIdx;
                    if( data[verIdx] != refData[verIdx] ) return verIdx;
                    ++verIdx;
                }

                // Call the progress callback function for the current value
                pcb.callProgressCallbackCurrent(progressCallback, nb);

            } // for b

            // Increment the counters
            cpgAddr += _config.memoryFlash.pageSize;
            rbdIdx  += _config.memoryFlash.pageSize;

        } // for p

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return nb;
    }

    private int _verifyReadFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

      //for( final int i : _swd_readSWDID() ) SysUtil.stdDbg().printf("%02X ", i); SysUtil.stdDbg().println(); // 3E A2 50 E9

        // Unlock the chip
        if( !_swd_unlockChip(false, false) ) return -1;

      //for( final int i : _swd_readSWDID() ) SysUtil.stdDbg().printf("%02X ", i); SysUtil.stdDbg().println(); // 3E A2 50 E9

        // Determine the start address and number of bytes
        final int sa = (startAddress < 0) ? 0                             : startAddress;
        final int nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer( new byte[] { FlashMemory_EmptyValue }, sa, nb, _config.memoryFlash.pageSize, _config.memoryFlash.totalSize, FlashMemory_EmptyValue, ProgClassName );
        if(anbr == null) return -1;

        // Read flash
        final int res = _verifyReadFlashPage(refData, anbr.buff, sa, anbr.nb, progressCallback);

        _config.memoryFlash.readDataBuff = USB2GPIO.ba2ia(anbr.buff, 0, numBytes);

        // Return the result
        return (res < anbr.nb) ? res : numBytes;
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
            if( !_xb_writeFlashPage_fast( cpgAddr, USB2GPIO.ba2ia(data, datIdx, _config.memoryFlash.pageSize) ) ) return false;

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

        // Unlock the chip
        if( !_swd_unlockChip(false, true) ) {
            if( !_swd_unlockChip(true, false) ) return false;
        }

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

    /*
     * WARNING : # All LGT8 MCUs do not have real EEPROM!
     *           # All LGT8 MCUs do not have fuses!
     *           # All LGT8 MCUs do not have lock bits!
     */

    @Override
    public int readEEPROM(final int address)
    {
        USB2GPIO.notifyError(Texts.ProgXXX_ENotAvailable, ProgClassName);
        return -1;
    }

    @Override
    public boolean writeEEPROM(final int address, final byte data)
    {
        USB2GPIO.notifyError(Texts.ProgXXX_ENotAvailable, ProgClassName);
        return false;
    }

    @Override
    public long readLockBits()
    {
        USB2GPIO.notifyError(Texts.ProgXXX_LBNotAvailable, ProgClassName);
        return -1;
    }

    @Override
    public boolean writeLockBits(final long value)
    {
        USB2GPIO.notifyError(Texts.ProgXXX_LBNotAvailable, ProgClassName);
        return false;
    }

} // class ProgLGT8
