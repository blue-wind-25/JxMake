/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;
import java.util.ArrayList;

import jxm.*;
import jxm.annotation.*;


/*
 * Please refer to the comment block before the 'ProgPIC' class definition in the 'ProgPIC.java' file for more details and information.
 */
public class ProgPIC_EICSP {

    @SuppressWarnings("serial")
    public static class CmdPE implements Serializable {

        @DataFormat.Hex02 public int SCHECK = 0x0; // Sanity check
        @DataFormat.Hex02 public int QVER   = 0xB; // Query the programming executive software version

        @DataFormat.Hex02 public int ERASEP =  -1; // Erase rows of code memory from specified address
        @DataFormat.Hex02 public int READP  = 0x2; // Read N 24-bit instruction words of code memory starting from specified address
        @DataFormat.Hex02 public int PROGP  = 0x5; // Program one row of code memory at the specified address, then verify

        @DataFormat.Hex02 public int ERASED =  -1; // Erase rows of  data EEPROM from specified address
        @DataFormat.Hex02 public int READD  = 0x1; // Read N 16-bit words of data EEPROM, configuration registers or device ID starting from specified address
        @DataFormat.Hex02 public int PROGD  = 0x4; // Program one row of data EEPROM at the specified address, then verify

        /*
         * Availability   Mnemonic   Opcode   Length   Timeout (mS)   Access
         *
         * Always         SCHECK     0x0      1        1
         * Always         QVER       0xB      1        1
         *
         *      Rare      ERASEP     0x9      3        5/row          Code memory
         * Always         READP      0x2      4        1/row          Code memory
         * Always         PROGP      0x5      99       5              Code memory
         *                           0x5      51       5              Code memory
         *
         * Always         QBLANK     0xA      3        300            Code memory
         *                           0xE      3        30/kB          Code memory
         *                           0xE      5        700            Code memory
         *
         *      Rare      ERASED     0x8      3        5/row          Data memory
         * If /w EEPROM   READD      0xE      4        1/word         Data memory            (also/or configuration register in some MCUs)
         *                           0x1      4        1/row          Data memory
         * If /w EEPROM   PROGD      0xF      19       5              Data memory
         *                           0x4      19       5              Data memory
         *
         * Some/Rare      READC      0x1      3        1              Device ID     register (or      configuration register in some MCUs)
         * Some/Rare      PROGC      0x4      4        5              Device ID     register (or      configuration register in some MCUs)
         *      Rare                 0x6      4        5              Configuration register
         *
         *      Rare      PROGW      0xD      4        5              Code memory
         *      Rare      PROG2W     0x3      6        5              Code memory
         */

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public int waitDelay_MS_EraseProgMem = 1;
        public int waitDelay_MS_WriteProgMem = 1;

        public int waitDelay_MS_EraseDataMem = 1;
        public int waitDelay_MS_WriteDataMem = 1;

    } // class CmdPE

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int CMD_WR_WAIT_US = 100;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ArrayList<Integer> _cmdSeq    = new ArrayList<>();

    private final int[]              _buff2     = new int[2];
    private final int[]              _buff3     = new int[3];

    private final ProgPIC            _progPIC;
    private final CmdPE              _cmdPE;

    private final USB_GPIO.SPIMode   _spiModeWr;
    private final USB_GPIO.SPIMode   _spiModeRd;

    private       int                _clkDiv    = -1;
    private       USB2GPIO.SPIMode   _spiMode   = USB2GPIO.SPIMode._3;

    private       boolean            _c4d24Mode = false;

    public ProgPIC_EICSP(final ProgPIC progPIC, USB_GPIO.SPIMode spiModeWr, USB_GPIO.SPIMode spiModeRd)
    {
        _progPIC   = progPIC;
        _cmdPE     = progPIC._picxx_pe_cmdPE();

        _spiModeWr = spiModeWr;
        _spiModeRd = spiModeRd;
    }

    public void setC4D24Mode(final boolean c4d24Mode)
    { _c4d24Mode = c4d24Mode; }

    public void adjustFlashBlockSize(final ProgPIC.Config config)
    {
        if(config.memoryFlash.eraseBlockSizeE > 0) config.memoryFlash.eraseBlockSize = config.memoryFlash.eraseBlockSizeE;
        if(config.memoryFlash.writeBlockSizeE > 0) config.memoryFlash.writeBlockSize = config.memoryFlash.writeBlockSizeE;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _spiPrepWr()
    {
        if(!_c4d24Mode) {
            if(_clkDiv != _progPIC.SPI_CLKDIV_XW_FREQ) {
                _clkDiv = _progPIC.SPI_CLKDIV_XW_FREQ;
                if( !_progPIC._usb2gpio.spiSetClkDiv(_clkDiv) ) return false;
            }
        }

        if(_spiMode != _spiModeWr) {
            _spiMode = _spiModeWr;
            if( !_progPIC._usb2gpio.spiSetSPIMode(_spiMode) ) return false;
        }

        return true;
    }

    private boolean _spiPrepRd(final int clkDiv)
    {
        if(!_c4d24Mode) {
            if(_clkDiv != clkDiv) {
                _clkDiv = clkDiv;
                if( !_progPIC._usb2gpio.spiSetClkDiv(_clkDiv) ) return false;
            }
        }

        if(_spiMode != _spiModeRd) {
            _spiMode = _spiModeRd;
            if( !_progPIC._usb2gpio.spiSetSPIMode(_spiMode) ) return false;
        }

        return true;
    }

    private boolean _spiPrepRd()
    { return _spiPrepRd(_progPIC.SPI_CLKDIV_HI_FREQ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int badr(final int address24) { return address24 * 3 / 2; } // Convert to byte (memory     ) address
    public int cadr(final int address24) { return address24 * 2 / 3; } // Convert to code (instruction) address

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int packBytes_b8x6_to_w16x3(final int[] src, final int srcIdx_, final int[] dst3)
    {
        int srcIdx = srcIdx_;

        for(int i = 0; i < 2; ++i) {
            _buff2[i] = ( src[srcIdx + 0]       )
                      | ( src[srcIdx + 1] <<  8 )
                      | ( src[srcIdx + 2] << 16 );
            srcIdx += 3;
        }

        ProgPIC.packWords_w24x2_to_w16x3(_buff2, dst3);

        return srcIdx;
    }

    public int unpackBytes_w16x3_to_b8x6(final int src3_0, final int src3_1, final int src3_2, final int[] dst, final int dstIdx_)
    {
        ProgPIC.unpackWords_w16x3_to_w24x2(src3_0, src3_1, src3_2, _buff2);

        int dstIdx = dstIdx_;

        for(int i = 0; i < 2; ++i) {
            dst[dstIdx++] = (_buff2[i] & 0x0000FF)       ;
            dst[dstIdx++] = (_buff2[i] & 0x00FF00) >>>  8;
            dst[dstIdx++] = (_buff2[i] & 0xFF0000) >>> 16;
        }

        return dstIdx;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _resPass(final int Cmd)
    { return 0x1000 | (Cmd << 8); }

    private int _resFail(final int Cmd)
    { return 0x2000 | (Cmd << 8); }

    private int _resNAK(final int Cmd)
    { return 0x3000 | (Cmd << 8); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _ssPutCmd(final int cmd, final int length)
    {
        // 1111111100000000
        // 7654321076543210
        // OOOOLLLLLLLLLLLL

        if(_c4d24Mode) { _cmdSeq.add(7); } _cmdSeq.add( cmd << 4 | (length & 0x0F00) >>> 8 );
        if(_c4d24Mode) { _cmdSeq.add(7); } _cmdSeq.add(             length & 0x00FF        );

        return _cmdSeq.size() - (_c4d24Mode ? 4 : 2);
    }

    private int _ssPutDat(final int dat)
    {
        if(_c4d24Mode) { _cmdSeq.add(7); } _cmdSeq.add( (dat & 0xFF00) >>> 8 );
        if(_c4d24Mode) { _cmdSeq.add(7); } _cmdSeq.add(  dat & 0x00FF        );

        return _cmdSeq.size() - (_c4d24Mode ? 4 : 2);
    }

    private int _ssPutRdW(final int count)
    {
        for(int i = 0; i < count; ++ i) {
            if(_c4d24Mode) { _cmdSeq.add(7); } _cmdSeq.add(0xFF);
            if(_c4d24Mode) { _cmdSeq.add(7); } _cmdSeq.add(0xFF);
        }

        return _cmdSeq.size() - count * (_c4d24Mode ? 4 : 2);
    }

    private int[] _ssGetCmdSeq()
    {
        // Error if there is no instruction
        if( _cmdSeq.size() == 0 ) return null;

        // Copy the sequence
        final int[] seq = new int[ _cmdSeq.size() ];

        for(int i = 0; i < _cmdSeq.size(); ++i) seq[i] = _cmdSeq.get(i);

        // Clear the instruction buffer
        _cmdSeq.clear();

        // Return the sequence
        return seq;
    }

    private int[] _ssExeCmdSeq(final int sleepUS)
    {
        // Get and execute the sequence
        final int[] seq = _ssGetCmdSeq();

        if(_c4d24Mode) {
            if( !_progPIC._usb2gpio.spiXBTransferIgnoreSS(seq) ) return null;
        }
        else {
            if( !_progPIC._usb2gpio.spiTransferIgnoreSS(seq) ) return null;
        }

        // Wait for a a while as needed
        if(sleepUS > 0) SysUtil.sleepUS(sleepUS);

        // Return the sequence
        return seq;
    }

    private int[] _ssExeCmdSeqWr(final int sleepUS)
    {
        if( !_spiPrepWr() ) return null;

        return _ssExeCmdSeq(sleepUS);
    }

    private int[] _ssExeCmdSeqWr()
    { return _ssExeCmdSeqWr(CMD_WR_WAIT_US); }

    private int[] _ssExeCmdSeqRd()
    {
        if( !_spiPrepRd() ) return null;

        return _ssExeCmdSeq(0);
    }

    private int[] _ssExeCmdSeqWrRd(final int numOfWrWords)
    {
        // ##### !!! TODO : Why it does not work with pure hardware SPI ??? !!! #####

        if( !_spiPrepRd(_progPIC.SPI_CLKDIV_XR_FREQ) ) return null;

        final int[] seq = _ssGetCmdSeq();

        if( !_progPIC._usb2gpio.spiTransferIgnoreSS_w16Nd_r16dN(_spiModeWr, CMD_WR_WAIT_US / 25, numOfWrWords * 2, _spiModeRd, 1, 0xFF, seq) ) return null;

        return seq;
    }

    private int _ssDecCmdSeq_U16(final int[] buff, final int pos)
    {
        return _c4d24Mode ? ( (buff[pos + 1] << 8) | buff[pos + 3] )
                          : ( (buff[pos    ] << 8) | buff[pos + 1] );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean ssSanityCheck()
    {
        if(true) {
            // Send SCHECK
            _ssPutCmd(_cmdPE.SCHECK, 1);

            if( _ssExeCmdSeqWr() == null ) return false;

            // Read the response
            final int idx1 = _ssPutRdW(1);
            final int idx2 = _ssPutRdW(1);

            final int[] seq = _ssExeCmdSeqRd();
            if(seq == null) return false;

            /*
            SysUtil.stdDbg().printf( "%04X %04X\n", _ssDecCmdSeq_U16(seq, idx1), _ssDecCmdSeq_U16(seq, idx2) );
            //*/

            // Parse and check the response
            if( _ssDecCmdSeq_U16(seq, idx1) != _resPass(_cmdPE.SCHECK) || _ssDecCmdSeq_U16(seq, idx2) != 0x0002 ) return false;
        }

        if(true) {
            // Send QVER
            _ssPutCmd(true ? _cmdPE.QVER : 0x0F, 1);

            if( _ssExeCmdSeqWr() == null ) return false;

            // Read the response
            final int idx1 = _ssPutRdW(1);
            final int idx2 = _ssPutRdW(1);

            final int[] seq = _ssExeCmdSeqRd();
            if(seq == null) return false;

            /*
            SysUtil.stdDbg().printf( "%04X %04X\n", _ssDecCmdSeq_U16(seq, idx1), _ssDecCmdSeq_U16(seq, idx2) );
            //*/
            /*
            SysUtil.stdDbg().printf( "%04X %04X\n", _ssDecCmdSeq_U16(seq, idx1) & 0xFF00, _resPass(_cmdPE.QVER) );
            //*/

            // Parse and check the response
            if( ( _ssDecCmdSeq_U16(seq, idx1) & 0xFF00 ) != _resPass(_cmdPE.QVER) || _ssDecCmdSeq_U16(seq, idx2) != 0x0002 ) return false;
        }

        // Done
        return true;
    }

    public boolean xbSanityCheck()
    {
        final boolean c4d24Mode = _c4d24Mode;

        _c4d24Mode = true;
        final boolean res = ssSanityCheck();

        _c4d24Mode = c4d24Mode;

        return res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean ssReadCodeMemory(final int address24_, final int[] dstBuff)
    {
        // Check whether to use '_ssExeCmdSeqWrRd()', which in turn uses 'spiTransferIgnoreSS_w16Nd_r16dN()'
        final boolean USE_W16ND_R16DN = (!_c4d24Mode                                                                ) &&
                                        (_progPIC.SPI_CLKFRQ_XR_FREQ >  _progPIC.SPI_CLKFRQ_HI_FREQ                 ) &&
                                        (_progPIC.SPI_CLKFRQ_XR_FREQ >= _progPIC._usb2gpio.spiGetFastestBBClkFreq() );

        // Determine some parameters
        final int address24 = cadr(address24_);
        final int instCnt   = dstBuff.length / 3;

        // Send READP
        _ssPutCmd(_cmdPE.READP, 4   );
        _ssPutDat(instCnt           );
        _ssPutDat(address24 >>> 16  );
        _ssPutDat(address24 & 0xFFFF);

        if(!USE_W16ND_R16DN) { // Execute the READP as needed
            if( _ssExeCmdSeqWr() == null ) return false;
        }

        // Read the response
        final int pdatCnt = instCnt * 3 / 2;
        final int idx1    = _ssPutRdW(1      );
        final int idx2    = _ssPutRdW(1      );
        final int idx3    = _ssPutRdW(pdatCnt);

        final int[] seq = USE_W16ND_R16DN ? _ssExeCmdSeqWrRd(4)
                                          : _ssExeCmdSeqRd  ( );
        if(seq == null) return false;

        /*
        SysUtil.stdDbg().printf( "%04X %04X\n", _ssDecCmdSeq_U16(seq, idx1), _ssDecCmdSeq_U16(seq, idx2) );
        //*/

        // Parse and check the response
        if( _ssDecCmdSeq_U16(seq, idx1) != _resPass(_cmdPE.READP) || _ssDecCmdSeq_U16(seq, idx2) != (2 + pdatCnt) ) return false;

        int sidx = idx3;
        int didx = 0;

        for(int i = 0; i < instCnt / 2; ++i) {

            final int v0 = _ssDecCmdSeq_U16(seq, sidx); sidx += (_c4d24Mode ? 4 : 2);
            final int v1 = _ssDecCmdSeq_U16(seq, sidx); sidx += (_c4d24Mode ? 4 : 2);
            final int v2 = _ssDecCmdSeq_U16(seq, sidx); sidx += (_c4d24Mode ? 4 : 2);

            didx = unpackBytes_w16x3_to_b8x6(v0, v1, v2, dstBuff, didx);

        } // for

        // Done
        return true;
    }

    public boolean ssWriteCodeMemory(final int address24_, final int[] srcBuff, final boolean noERASEP)
    {
        // Determine some parameters
        final int address24 = cadr(address24_);
        final int instCnt   = (srcBuff.length == 192) ? 64  // 0x40
                            : (srcBuff.length ==  96) ? 32  // 0x20
                            :                            0;
        final int len       = (instCnt        ==  64) ? 99  // 0x63
                            : (instCnt        ==  32) ? 51  // 0x33
                            :                            0;
        if(instCnt <= 0 || len <= 0) return false;

        // Send ERASEP as needed
        if(true && !noERASEP && _cmdPE.ERASEP >= 0) {

            // Send the command
            _ssPutCmd( _cmdPE.ERASEP, 3              );
            _ssPutDat( (1 << 8) | address24 >>> 16   );
            _ssPutDat(            address24 & 0xFFFF );

            if( _ssExeCmdSeqWr(_cmdPE.waitDelay_MS_EraseProgMem * 1000) == null ) return false;

            // Read the response
            final int idx1 = _ssPutRdW(1);
            final int idx2 = _ssPutRdW(1);

            final int[] seq = _ssExeCmdSeqRd();
            if(seq == null) return false;

            /*
            SysUtil.stdDbg().printf( "ERASEP: [%06X] %04X %04X\n", address24, _ssDecCmdSeq_U16(seq, idx1), _ssDecCmdSeq_U16(seq, idx2) );
            //*/

            // Parse and check the response
            if( _ssDecCmdSeq_U16(seq, idx1) != _resPass(_cmdPE.ERASEP) || _ssDecCmdSeq_U16(seq, idx2) != 0x0002 ) return false;

        } // if

        /*
        if(true) {
            SysUtil.stdDbg().printf( "DUMMY PROGP: %06X %02X | %d\n", address24, len,  instCnt / 2 * 3 * 2 );
            return true;
        }
        //*/

        // Send PROGP
        _ssPutCmd(_cmdPE.PROGP, len );
        _ssPutDat(address24 >>> 16  );
        _ssPutDat(address24 & 0xFFFF);

        // Send the data
        int sidx = 0;

        for(int i = 0; i < instCnt / 2; ++i) {

            sidx = packBytes_b8x6_to_w16x3(srcBuff, sidx, _buff3);

            _ssPutDat(_buff3[0]);
            _ssPutDat(_buff3[1]);
            _ssPutDat(_buff3[2]);

        } // for

        if( _ssExeCmdSeqWr(_cmdPE.waitDelay_MS_WriteProgMem * 1000) == null ) return false;

        // Read the response
        final int idx1 = _ssPutRdW(1);
        final int idx2 = _ssPutRdW(1);

        final int[] seq = _ssExeCmdSeqRd();
        if(seq == null) return false;

        /*
        SysUtil.stdDbg().printf( "PROGP: %04X %04X\n", _ssDecCmdSeq_U16(seq, idx1), _ssDecCmdSeq_U16(seq, idx2) );
        //*/

        // Parse and check the response
        if( _ssDecCmdSeq_U16(seq, idx1) != _resPass(_cmdPE.PROGP) || _ssDecCmdSeq_U16(seq, idx2) != 0x0002 ) return false;

        // Done
        return true;
    }

    public boolean ssWriteCodeMemory(final int address24_, final int[] srcBuff)
    { return ssWriteCodeMemory(address24_, srcBuff, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean ssReadEntireDataMemory(final int address24_, final int[] dstBuff)
    {
        // Determine some parameters
        final int MaxReadSize = 2 * 1024; // The maximum read size is 1024 words (2048 bytes)

        final int readSize    = Math.min(dstBuff.length, MaxReadSize) / 2;
        final int readCount   = dstBuff.length / 2 / readSize;

        if(readCount * readSize * 2 != dstBuff.length) return false;

        // Read the data
        int address24 = address24_;
        int didx      = 0;

        for(int r = 0; r < readCount; ++r) {

            // Send READD
            _ssPutCmd(_cmdPE.READD, 4   );
            _ssPutDat(readSize          );
            _ssPutDat(address24 >>> 16  );
            _ssPutDat(address24 & 0xFFFF);

            if( _ssExeCmdSeqWr() == null ) return false;

            address24 += (readSize * 2);

            // Read the response
            final int idx1 = _ssPutRdW(1       );
            final int idx2 = _ssPutRdW(1       );
            final int idx3 = _ssPutRdW(readSize);

            final int[] seq = _ssExeCmdSeqRd();
            if(seq == null) return false;

            /*
            SysUtil.stdDbg().printf( "%04X %04X\n", _ssDecCmdSeq_U16(seq, idx1), _ssDecCmdSeq_U16(seq, idx2) );
            //*/

            // Parse and check the response
            if( _ssDecCmdSeq_U16(seq, idx1) != _resPass(_cmdPE.READD) || _ssDecCmdSeq_U16(seq, idx2) != (2 + readSize) ) return false;

            int sidx = idx3;

            for(int i = 0; i < readSize; ++i) {

                final int v = _ssDecCmdSeq_U16(seq, sidx); sidx += (_c4d24Mode ? 4 : 2);

                dstBuff[didx++] = v &   0xFF;
                dstBuff[didx++] = v >>> 8   ;

            } // for

        } // for

        // Done
        return true;
    }

    public boolean ssWriteEntireDataMemory(final int address24_, final int[] srcBuff, final int rowSize)
    {
        // Determine some parameters
        final int writeSize  = Math.min(srcBuff.length, rowSize) / 2;
        final int writeCount = srcBuff.length / 2 / writeSize;

        if(writeCount * writeSize * 2 != srcBuff.length) return false;

        final int len        = (rowSize == 32) ? 19
                             : (rowSize ==  2) ?  4
                             :                    0;

        if(len <= 0) return false;

        // Read the data
        int address24 = address24_;
        int sidx      = 0;

        for(int w = 0; w < writeCount; ++w) {

            // Send ERASED as needed
            if(true && _cmdPE.ERASED >= 0) {

                // Send the command
                _ssPutCmd( _cmdPE.ERASED, 3              );
                _ssPutDat( (1 << 8) | address24 >>> 16   );
                _ssPutDat(            address24 & 0xFFFF );

                if( _ssExeCmdSeqWr(_cmdPE.waitDelay_MS_EraseDataMem * 1000) == null ) return false;

                // Read the response
                final int idx1 = _ssPutRdW(1);
                final int idx2 = _ssPutRdW(1);

                final int[] seq = _ssExeCmdSeqRd();
                if(seq == null) return false;

                /*
                SysUtil.stdDbg().printf( "%04X %04X\n", _ssDecCmdSeq_U16(seq, idx1), _ssDecCmdSeq_U16(seq, idx2) );
                //*/

                // Parse and check the response
                if( _ssDecCmdSeq_U16(seq, idx1) != _resPass(_cmdPE.ERASED) || _ssDecCmdSeq_U16(seq, idx2) != 0x0002 ) return false;

            } // if

            /*
            if(true) continue;
            //*/

            // Send PROGD
            _ssPutCmd(_cmdPE.PROGD, len );
            _ssPutDat(address24 >>> 16  );
            _ssPutDat(address24 & 0xFFFF);

            address24 += (writeSize * 2);

            // Send the data
            for(int i = 0; i < writeSize; ++i) {
                _ssPutDat( ( srcBuff[sidx + 1] << 8 ) | srcBuff[sidx] );
                sidx += 2;
            }

            if( _ssExeCmdSeqWr(_cmdPE.waitDelay_MS_WriteDataMem * 1000) == null ) return false;

            // Read the response
            final int idx1 = _ssPutRdW(1);
            final int idx2 = _ssPutRdW(1);

            final int[] seq = _ssExeCmdSeqRd();
            if(seq == null) return false;

            /*
            SysUtil.stdDbg().printf( "%04X %04X\n", _ssDecCmdSeq_U16(seq, idx1), _ssDecCmdSeq_U16(seq, idx2) );
            //*/

            // Parse and check the response
            if( _ssDecCmdSeq_U16(seq, idx1) != _resPass(_cmdPE.PROGD) || _ssDecCmdSeq_U16(seq, idx2) != 0x0002 ) return false;

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int ssReadU16Memory(final int address24)
    {
        final int[] buff = new int[6];

        if( !ssReadCodeMemory( badr(address24), buff ) ) return -1;

        return (buff[1] << 8) | buff[0];
    }

    public int ssReadDeviceID(final int address24)
    {
        final int res = ssReadU16Memory(address24);

        return (res < 0 || res == 0xFFFF) ? 0 : res; // Programming executive might not support reading device ID properly
    }

} // class ProgPIC_EICSP
