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
import jxm.xb.*;
import jxm.tool.fwc.*;


/*
 * Please refer to the comment block before the 'ProgPIC' class definition in the 'ProgPIC.java' file for more details and information.
 */
public class ProgPIC_SIX_REGOUT {

    @SuppressWarnings("serial")
    public static class MemoryPE implements Serializable {
        @DataFormat.Hex08 public long address        = -1;
                          public int  totalSize      =  0;
                          public int  pageSize       =  0;
                          public int  numPages       =  0;

        @DataFormat.Hex08 public long saveWordOffset = -1;
                          public int  saveWordCount  =  0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int Cmd_SIX    = 0b0000;
    private static int Cmd_REGOUT = 0b0001;

    private static class Cmd {
        public final int cmd4;
        public final int data24;

        public Cmd(final int cmd4_, final int data24_)
        {
            cmd4   = cmd4_;
            data24 = data24_;
        }
    }

    private final ArrayList<Cmd>     _cmdSeq    = new ArrayList<>();
    private final ArrayList<Integer> _idxREGOUT = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _c4d24Mode = false;
    private boolean _extraNOPs = false;

    private int[]   _buff2     = new int[2];
    private int[]   _buff3     = new int[3];
    private int[]   _buff4     = new int[4];
    private int[]   _buff6     = new int[6];

    private int _putCmd(final int cmd4, final int data24)
    {
        if( _cmdSeq.isEmpty() ) _idxREGOUT.clear();

        _cmdSeq.add( new Cmd(cmd4, data24) );

        return _c4d24Mode ?       ( ( _cmdSeq.size() - 1    ) * 2                  )
                          : (int) ( ( _cmdSeq.size() - 1.0f ) / 2.0f * 7.0f + 0.5f );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void checkMemoryPE(final MemoryPE memoryPE, final String ProgClassName) throws Exception
    {
        if(memoryPE.address < 0 && memoryPE.totalSize <= 0) return;

        if(memoryPE.address   <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvPEAddress , ProgClassName);
        if(memoryPE.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvPETotSize , ProgClassName);
        if(memoryPE.pageSize  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvPEPageSize, ProgClassName);
        if(memoryPE.numPages  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvPENumPages, ProgClassName);

        if(memoryPE.pageSize * memoryPE.numPages != memoryPE.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvPEPageSpec, ProgClassName);

        if(memoryPE.saveWordOffset >= 0 || memoryPE.saveWordCount > 0) {
            if(memoryPE.saveWordOffset <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvPESWOffset , ProgClassName);
            if(memoryPE.saveWordCount  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvPESWCount , ProgClassName);

            if( (memoryPE.address + memoryPE.saveWordOffset + memoryPE.saveWordCount * 2)
                >
                (memoryPE.address + memoryPE.totalSize)
            ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvPESWSpec, ProgClassName);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setC4D24Mode(final boolean c4d24Mode)
    { _c4d24Mode = c4d24Mode; }


    // NOTE : # Some MCUs need more NOPs in either all operations or some operations.
    //        # Call this function as required.
    public void setExtraNOPs(final boolean extraNOPs)
    { _extraNOPs = extraNOPs; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void putCmd_SIX   (final int data24) {                 _putCmd    (Cmd_SIX   , data24           )  ; }
    public void putCmd_NOP   (                ) {                  putCmd_SIX(            0x000000         )  ; }
    public void putCmd_GOTO15(final int addr15) {                  putCmd_SIX(            0x040000 | addr15)  ; }

    public void putCmd_REGOUT(                ) { _idxREGOUT.add( _putCmd    (Cmd_REGOUT, 0xFFFF00         ) ); }
    public int  idxREGOUT    (final int pos   ) { return _idxREGOUT.get(pos);                                   }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int[] getC4D24Seq()
    {
        // Error if there is no instruction
        if( _cmdSeq.size() == 0 ) return null;

        // Generate the data sequence
        final int[] datSeq = new int[ _cmdSeq.size() * 2 ];
              int   idx    = 0;

        for(int i = 0; i < _cmdSeq.size(); ++i) {
            datSeq[idx++] = _cmdSeq.get(i).cmd4;
            datSeq[idx++] = _cmdSeq.get(i).data24;
        }

        // Clear the instruction buffer
        _cmdSeq.clear();

        // Return the data sequence
        return datSeq;
    }

    public int decC4D24Seq_U16(final int[] buff, final int pos)
    {
        final int idx = idxREGOUT(pos);

        return buff[idx + 1] >>> 8;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int[] getCmdBytes()
    {
        // Error if there is no instruction
        if( _cmdSeq.size() == 0 ) return null;

        // Ensure that the number of instructions is even
        if( ( _cmdSeq.size() & 1 ) != 0 ) putCmd_NOP();

        // Generate the byte sequence
        final int[] cmdBytes = new int[ _cmdSeq.size() / 2 * 7 ];
              int   idx      = 0;

        for(int i = 0; i < _cmdSeq.size(); i += 2) {

            final int cmd0 = XCom._RU04( _cmdSeq.get(i + 0).cmd4   );
            final int dat0 = XCom._RU24( _cmdSeq.get(i + 0).data24 );

            final int cmd1 = XCom._RU04( _cmdSeq.get(i + 1).cmd4   );
            final int dat1 = XCom._RU24( _cmdSeq.get(i + 1).data24 );

            /*
            System.out.printf("%01X %06X | %01X %06X\n", cmd0, dat0, cmd1, dat1);
            //*/

            cmdBytes[idx++] =   ( (cmd0 <<   4)        )        | (dat0 >>> 20);
            cmdBytes[idx++] =   ( (dat0 >>> 12) & 0xFF )                       ;
            cmdBytes[idx++] =   ( (dat0 >>>  4) & 0xFF )                       ;
            cmdBytes[idx++] = ( ( (dat0       ) & 0x0F ) << 4 ) | (cmd1       );
            cmdBytes[idx++] =   ( (dat1 >>> 16) & 0xFF )                       ;
            cmdBytes[idx++] =   ( (dat1 >>>  8) & 0xFF )                       ;
            cmdBytes[idx++] =   ( (dat1       ) & 0xFF )                       ;

        } // for

        // Clear the instruction buffer
        _cmdSeq.clear();

        // Return the byte sequence
        return cmdBytes;
    }

    public int decCmdBytes_U16(final int[] buff, final int pos)
    {
        final int idx = idxREGOUT(pos);

        if( (idx % 7) == 0 ) {
            return XCom._RU24( ( (buff[idx + 0] & 0x0F) <<  20 ) |
                               ( (buff[idx + 1] & 0xFF) <<  12 ) |
                               ( (buff[idx + 2] & 0xFF) <<  4  ) |
                               ( (buff[idx + 3] & 0xF0) >>> 4  )
                             ) >>> 8;

        }
        else {
            return XCom._RU24( ( (buff[idx + 0] & 0xFF) <<  16 ) |
                               ( (buff[idx + 1] & 0xFF) <<   8 ) |
                               ( (buff[idx + 2] & 0xFF) <<   0 )
                             ) >>> 8;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int[] getCmdsBuff()
    { return _c4d24Mode ? getC4D24Seq() : getCmdBytes(); }

    public int decCmdsBuff_U16(final int[] buff, final int pos)
    { return _c4d24Mode ? decC4D24Seq_U16(buff, pos) : decCmdBytes_U16(buff, pos); }

    public boolean exeCmds(final ProgPIC progPIC, final int[] buff)
    {
        return _c4d24Mode ? progPIC._pic_xbTransfer_c4_d24       (buff)
                          : progPIC._usb2gpio.spiTransferIgnoreSS(buff);
    }

    public boolean exeCmds(final ProgPIC progPIC)
    {
        return _c4d24Mode ? progPIC._pic_xbTransfer_c4_d24       ( getC4D24Seq() )
                          : progPIC._usb2gpio.spiTransferIgnoreSS( getCmdBytes() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int  badr (final int  address24) { return address24 * 3 / 2;             } // Convert to byte (memory     ) address
    public long badr (final long address24) { return address24 * 3 / 2;             } // ---

    public int  cadr (final int  address24) { return address24 * 2 / 3;             } // Convert to code (instruction) address
    public long cadr (final long address24) { return address24 * 2 / 3;             } // ---

    public int  madrP(final int  address24) { return (address24 & 0xFF0000) >>> 16; } // Get the page address in byte (memory     ) addressing mode
    public int  madrW(final int  address24) { return  address24 & 0x00FFFF        ; } // Get the word address in byte (memory     ) addressing mode

    public int  cadrP(final int  address24) { return madrP( cadr(address24) );      } // Get the page address in code (instruction) addressing mode
    public int  cadrW(final int  address24) { return madrW( cadr(address24) );      } // Get the page address in code (instruction) addressing mode

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void packWords_w24x2_to_w16x3(final int[] src2, final int[] dst3)
    { ProgPIC.packWords_w24x2_to_w16x3(src2, dst3); }

    public void unpackWords_w16x3_to_w24x2(final int[] src3, final int[] dst2)
    { ProgPIC.unpackWords_w16x3_to_w24x2(src3, dst2); }

    public void packWords_w24x4_to_w16x6(final int[] src4, final int[] dst6)
    { ProgPIC.packWords_w24x4_to_w16x6(src4, dst6); }

    public void unpackWords_w16x6_to_w24x4(final int[] src6, final int[] dst4)
    { ProgPIC.unpackWords_w16x6_to_w24x4(src6, dst4); }

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

        packWords_w24x2_to_w16x3(_buff2, dst3);

        return srcIdx;
    }

    public int unpackBytes_w16x3_to_b8x6(final int[] src3, final int[] dst, final int dstIdx_)
    {
        unpackWords_w16x3_to_w24x2(src3, _buff2);

        int dstIdx = dstIdx_;

        for(int i = 0; i < 2; ++i) {
            dst[dstIdx++] = (_buff2[i] & 0x0000FF)       ;
            dst[dstIdx++] = (_buff2[i] & 0x00FF00) >>>  8;
            dst[dstIdx++] = (_buff2[i] & 0xFF0000) >>> 16;
        }

        return dstIdx;
    }

    public int packBytes_b8x12_to_w16x6(final int[] src, final int srcIdx_, final int[] dst6)
    {
        int srcIdx = srcIdx_;

        for(int i = 0; i < 4; ++i) {
            _buff4[i] = ( src[srcIdx + 0]       )
                      | ( src[srcIdx + 1] <<  8 )
                      | ( src[srcIdx + 2] << 16 );
            srcIdx += 3;
        }

        packWords_w24x4_to_w16x6(_buff4, dst6);

        return srcIdx;
    }

    public int unpackBytes_w16x6_to_b8x12(final int[] src6, final int[] dst, final int dstIdx_)
    {
        unpackWords_w16x6_to_w24x4(src6, _buff4);

        int dstIdx = dstIdx_;

        for(int i = 0; i < 4; ++i) {
            dst[dstIdx++] = (_buff4[i] & 0x0000FF)       ;
            dst[dstIdx++] = (_buff4[i] & 0x00FF00) >>>  8;
            dst[dstIdx++] = (_buff4[i] & 0xFF0000) >>> 16;
        }

        return dstIdx;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _rstPCAddr15  = 0x0000;
    private int _rstPCPreNOPs = 0;
    private int _rstPCPstNOPs = 1;

    public void setResetInternalPCAddress(final int rstPCAddr15, final int rstPCPreNOPs, final int rstPCPstNOPs)
    {
        _rstPCAddr15  = rstPCAddr15;
        _rstPCPreNOPs = rstPCPreNOPs;
        _rstPCPstNOPs = rstPCPstNOPs;
    }

    public void putCmds_resetInternalPC()
    {
        for(int i = 0; i < _rstPCPreNOPs; ++i) putCmd_NOP   (              ); // NOP
                                               putCmd_GOTO15( _rstPCAddr15 ); // GOTO <_rstPCAddr15>
        for(int i = 0; i < _rstPCPstNOPs; ++i) putCmd_NOP   (              ); // NOP
    }

    public boolean exeCmds_resetInternalPC(final ProgPIC progPIC)
    {
        putCmds_resetInternalPC();

        return exeCmds(progPIC);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    private void _putCmds_readU16Memory_pic24_init_impl(final ProgPIC progPIC, final int address24)
    {
        // Initialize TBLPAG, the read pointer (W6), and the write pointer (W7) for the TBLRD instruction
        //               ...xx.
        putCmd_SIX   ( 0x200000 | madrP(address24) << 4 ); // MOV    #<address[23:16]>, W0
        putCmd_SIX   ( 0x880190                         ); // MOV    W0               , TBLPAG
        //               .xxxx.
        putCmd_SIX   ( 0x200006 | madrW(address24) << 4 ); // MOV    #<address[15:00]>, W6
        putCmd_SIX   ( 0x207847                         ); // MOV    #VISI            , W7
        putCmd_NOP   (                                  ); // NOP
    }

    private void _putCmds_readU16Memory_pic24_read_impl(final ProgPIC progPIC)
    {
        // Read and clock out the value through the VISI register using the REGOUT command
        putCmd_SIX   ( 0xBA0BB6                         ); // TBLRDL [W6++]           , [W7]
        putCmd_NOP   (                                  ); // NOP
        putCmd_NOP   (                                  ); // NOP

        putCmd_REGOUT(                                  ); // Clock out the contents of the VISI register
        putCmd_NOP   (                                  ); // NOP
    }

    public int exeCmds_readU16Memory_pic24(final ProgPIC progPIC, final int address24)
    {
        // Initialize TBLPAG, the read pointer (W6), and the write pointer (W7) for the TBLRD instruction
        _putCmds_readU16Memory_pic24_init_impl(progPIC, address24);

        // Read and clock out the value through the VISI register using the REGOUT command
        _putCmds_readU16Memory_pic24_read_impl(progPIC);

        // Reset the device internal PC
        putCmds_resetInternalPC();

        // Execute the command sequence
        final int[] buff = getCmdsBuff();

        if( !exeCmds(progPIC, buff) ) return -1;

        // Decode and return the result
        return decCmdsBuff_U16(buff, 0);
    }

    public boolean exeCmds_readU16Memory_pic24(final ProgPIC progPIC, final int address24, final int[] dstBuff)
    {
        // Initialize TBLPAG, the read pointer (W6), and the write pointer (W7) for the TBLRD instruction
        _putCmds_readU16Memory_pic24_init_impl(progPIC, address24);

        // Read and clock out the value through the VISI register using the REGOUT command
        for(int i = 0; i < dstBuff.length; ++i) _putCmds_readU16Memory_pic24_read_impl(progPIC);

        // Reset the device internal PC
        putCmds_resetInternalPC();

        // Execute the command sequence
        final int[] buff = getCmdsBuff();

        if( !exeCmds(progPIC, buff) ) return false;

        // Decode the results
        for(int i = 0; i < dstBuff.length; ++i) dstBuff[i] = decCmdsBuff_U16(buff, i);

        // Done
        return true;
    }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public int exeCmds_readU16Memory_dspic30(final ProgPIC progPIC, final int address24)
    { return exeCmds_readU16Memory_pic24(progPIC, address24); }

    public boolean exeCmds_readU16Memory_dspic30(final ProgPIC progPIC, final int address24, final int[] dstBuff)
    { return exeCmds_readU16Memory_pic24(progPIC, address24, dstBuff); }

    // ----- ----- -----

    private void _putCmds_readU16Memory_dspic33ep_init_impl(final ProgPIC progPIC, final int address24)
    {
        // Initialize TBLPAG, the read pointer (W6), and the write pointer (W7) for the TBLRD instruction
        //               ...xx.
        putCmd_SIX   ( 0x200000 | madrP(address24) << 4 ); // MOV    #<address[23:16]>, W0
        putCmd_SIX   ( 0x8802A0                         ); // MOV    W0               , TBLPAG
        //               .xxxx.
        putCmd_SIX   ( 0x200006 | madrW(address24) << 4 ); // MOV    #<address[15:00]>, W6
        putCmd_SIX   ( 0x20F887                         ); // MOV    #VISI            , W7
        putCmd_NOP   (                                  ); // NOP
    }

    private void _putCmds_readU16Memory_dspic33ep_read_impl(final ProgPIC progPIC)
    {
        // Read and clock out the value through the VISI register using the REGOUT command
        putCmd_SIX   ( 0xBA0B96                         ); // TBLRDL [W6++]           , [W7]
        putCmd_NOP   (                                  ); // NOP
        putCmd_NOP   (                                  ); // NOP
        putCmd_NOP   (                                  ); // NOP
        putCmd_NOP   (                                  ); // NOP
        putCmd_NOP   (                                  ); // NOP

        putCmd_REGOUT(                                  ); // Clock out the contents of the VISI register
        putCmd_NOP   (                                  ); // NOP
    }

    public int exeCmds_readU16Memory_dspic33ep(final ProgPIC progPIC, final int address24)
    {
        // Initialize TBLPAG, the read pointer (W6), and the write pointer (W7) for the TBLRD instruction
        _putCmds_readU16Memory_dspic33ep_init_impl(progPIC, address24);

        // Read and clock out the value through the VISI register using the REGOUT command
        _putCmds_readU16Memory_dspic33ep_read_impl(progPIC);

        // Reset the device internal PC
        putCmds_resetInternalPC();

        // Execute the command sequence
        final int[] buff = getCmdsBuff();

        if( !exeCmds(progPIC, buff) ) return -1;

        // Decode and return the result
        return decCmdsBuff_U16(buff, 0);
    }

    public boolean exeCmds_readU16Memory_dspic33ep(final ProgPIC progPIC, final int address24, final int[] dstBuff)
    {
        // Initialize TBLPAG, the read pointer (W6), and the write pointer (W7) for the TBLRD instruction
        _putCmds_readU16Memory_dspic33ep_init_impl(progPIC, address24);

        // Read and clock out the value through the VISI register using the REGOUT command
        for(int i = 0; i < dstBuff.length; ++i) _putCmds_readU16Memory_dspic33ep_read_impl(progPIC);

        // Reset the device internal PC
        putCmds_resetInternalPC();

        // Execute the command sequence
        final int[] buff = getCmdsBuff();

        if( !exeCmds(progPIC, buff) ) return false;

        // Decode the results
        for(int i = 0; i < dstBuff.length; ++i) dstBuff[i] = decCmdsBuff_U16(buff, i);

        // Done
        return true;
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _exeCmds_writeU16MemoryPage_pic24_dspic30_impl(final ProgPIC progPIC, final int nvmcmd, final int address24, final int[] srcBuff)
    {
            // Initialize TBLPAG and the write pointer (W7) for the TBLWT instruction
            //            ...xx.
            putCmd_SIX( 0x200000 | madrP(address24) << 4 ); // MOV    #<address[23:16]>, W0
            putCmd_SIX( 0x880190                         ); // MOV    W0               , TBLPAG
            //            .xxxx.
            putCmd_SIX( 0x200007 | madrW(address24) << 4 ); // MOV    #<address[15:00]>, W7

            // Set the NVMCON register to program the memory word
            putCmd_SIX( 0x20000A | nvmcmd           << 4 ); // MOV    #<nvmcmd>        , W10
            putCmd_SIX( 0x883B0A                         ); // MOV    W10              , NVMCON

        for(int i = 0; i < srcBuff.length; ++i) {

            // Store the data to be written
            putCmd_SIX( 0x200006 | srcBuff[i]       << 4 ); // MOV    #<data>          , W6

            // Write the data to the write latch and increment the write pointer
            putCmd_NOP(                                  ); // NOP
            putCmd_SIX( 0xBB1B86                         ); // TBLWTL W6, [W7++]
            putCmd_NOP(                                  ); // NOP
            putCmd_NOP(                                  ); // NOP

        } // for

            // Initiate the write cycle
                 if(progPIC instanceof ProgPIC24  ) putCmds_initiateEWCycle_pic24  ();
            else if(progPIC instanceof ProgDSPIC30) putCmds_initiateEWCycle_dspic30();
            else                                    return false;

            if( !exeCmds(progPIC) ) return false;

            // Wait NVM
                 if(progPIC instanceof ProgPIC24  ) { if( !exeCmds_waitNVM_pic24  (progPIC) ) return false; }
            else if(progPIC instanceof ProgDSPIC30) { if( !exeCmds_waitNVM_dspic30(progPIC) ) return false; }
            else                                    return false;

            // Reset the device internal PC
            return exeCmds_resetInternalPC(progPIC);
    }

    // ----- ----- -----

    public boolean exeCmds_writeU16MemoryPage_pic24(final ProgPIC progPIC, final int nvmcmd, final int address24, final int[] srcBuff)
    { return _exeCmds_writeU16MemoryPage_pic24_dspic30_impl(progPIC, nvmcmd, address24, srcBuff); }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public boolean exeCmds_writeU16MemoryPage_dspic30(final ProgPIC progPIC, final int address24, final int[] srcBuff)
    { return _exeCmds_writeU16MemoryPage_pic24_dspic30_impl(progPIC, 0x4001, address24, srcBuff); }

    // ----- ----- -----

    public boolean exeCmds_writeU16MemoryPage_dspic33ep(final ProgPIC progPIC, final int address24, final int[] srcBuff)
    {
        // ##### !!! TODO : Verify if this function is actually not needed for dsPIC33EP MCUs !!! #####
        return false;
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void putCmds_initiateEWCycle_pic24()
    {
            putCmd_SIX( 0xA8E761 ); // BSET NVMCON, #WR
            putCmd_NOP(          ); // NOP
            putCmd_NOP(          ); // NOP
        if(_extraNOPs) {
            putCmd_NOP(          ); // NOP
            putCmd_NOP(          ); // NOP
        }
    }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public void putCmds_initiateEWCycle_dspic30()
    {
        // Unlock the NVMCON for programming
        putCmd_SIX( 0x200558 ); // MOV  #0x55 , W8
        putCmd_SIX( 0x883B38 ); // MOV  W8    , NVMKEY
        putCmd_SIX( 0x200AA9 ); // MOV  #0xAA , W9
        putCmd_SIX( 0x883B39 ); // MOV  W9    , NVMKEY

        // Initiate the write cycle
        putCmd_SIX( 0xA8E761 ); // BSET NVMCON, #WR
        putCmd_NOP(          ); // NOP
        putCmd_NOP(          ); // NOP
    }

    // ----- ----- -----

    public void putCmds_initiateEWCycle_dspic33ep()
    {
            // Unlock the NVMCON for programming
            putCmd_SIX( 0x200551 ); // MOV  #0x55 , W1
            putCmd_SIX( 0x883971 ); // MOV  W1    , NVMKEY
            putCmd_SIX( 0x200AA1 ); // MOV  #0xAA , W1
            putCmd_SIX( 0x883971 ); // MOV  W1    , NVMKEY

            // Initiate the write cycle
            putCmd_SIX( 0xA8E729 ); // BSET NVMCON, #WR
            putCmd_NOP(          ); // NOP
            putCmd_NOP(          ); // NOP
            putCmd_NOP(          ); // NOP
            putCmd_NOP(          ); // NOP
        if(_extraNOPs) {
            putCmd_NOP(          ); // NOP
            putCmd_NOP(          ); // NOP
        }
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean exeCmds_waitNVM_pic24(final ProgPIC progPIC)
    {
        // Wait NVM
        while(true) {

            // Poll the WR bit (bit 15 of NVMCON) until it is cleared by the hardware
            putCmd_SIX   ( 0x040200 ); // GOTO 0x200
            putCmd_NOP   (          ); // NOP
            putCmd_SIX   ( 0x803B02 ); // MOV  NVMCON, W2
            putCmd_SIX   ( 0x883C22 ); // MOV  W2    , VISI
            putCmd_NOP   (          ); // NOP
            putCmd_REGOUT(          ); // Clock out the contents of the VISI register
            putCmd_NOP   (          ); // NOP

            // Execute the command sequence
            final int[] buff = getCmdsBuff();

            if( !exeCmds(progPIC, buff) ) return false;

            // Decode and check the result
            final int nvmcon = decCmdsBuff_U16(buff, 0);

            if( (nvmcon & 0b1000000000000000) == 0 ) break;
            if( (nvmcon & 0b0010000000000000) != 0 ) return false;

        } // while

        // Done
        return true;
    }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public boolean exeCmds_waitNVM_dspic30(final ProgPIC progPIC)
    {
        // Wait for a while according to the specifications in the datasheet
        SysUtil.sleepMS(4);

        // End the write cycle
        putCmd_NOP(          ); // NOP
        putCmd_NOP(          ); // NOP
        putCmd_SIX( 0xA9E761 ); // BCLR NVMCON, #WR
        putCmd_NOP(          ); // NOP
        putCmd_NOP(          ); // NOP

        return exeCmds(progPIC);
    }

    // ----- ----- -----

    public boolean exeCmds_waitNVM_dspic33ep_and_resetInternalPC(final ProgPIC progPIC)
    {
        // Wait NVM
        while(true) {

            // Poll the WR bit (bit 15 of NVMCON) until it is cleared by the hardware
            putCmd_SIX   ( 0x803940 ); // MOV  NVMCON, W0
            putCmd_NOP   (          ); // NOP
            putCmd_SIX   ( 0x887C40 ); // MOV  W0    , VISI
            putCmd_NOP   (          ); // NOP
            putCmd_REGOUT(          ); // Clock out the contents of the VISI register
            putCmd_NOP   (          ); // NOP

            // Reset the device internal PC
            putCmds_resetInternalPC();

            // Execute the command sequence
            final int[] buff = getCmdsBuff();

            if( !exeCmds(progPIC, buff) ) return false;

            // Decode and check the result
            final int nvmcon = decCmdsBuff_U16(buff, 0);

            if( (nvmcon & 0b1000000000000000) == 0 ) break;
            if( (nvmcon & 0b0010000000000000) != 0 ) return false;

        } // while

        // Done
        return true;
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _exeCmds_writeCWMemory_pic24_dspic30_impl(final ProgPIC progPIC, final int nvmcmd, final int address24, final int value16)
    {
        // Initialize TBLPAG and the write pointer (W7) for the TBLWT instruction
        //            ...xx.
        putCmd_SIX( 0x200000 | madrP(address24) << 4 ); // MOV    #<address[23:16]>, W0
        putCmd_SIX( 0x880190                         ); // MOV    W0               , TBLPAG
        //            .xxxx.
        putCmd_SIX( 0x200007 | madrW(address24) << 4 ); // MOV    #<address[15:00]>, W7

        // Load the configuration register data to W6
        putCmd_SIX( 0x200006 | value16          << 4 ); // MOV    #<value[15:00]>  , W6

        // Set the NVMCON register to program the configuration word
        putCmd_SIX( 0x20000A | nvmcmd           << 4 ); // MOV    #<nvmcmd>        , W10
        putCmd_SIX( 0x883B0A                         ); // MOV    W10              , NVMCON

        // Write the configuration register data to the write latch and increment the write pointer
        putCmd_NOP(                                  ); // NOP
        putCmd_SIX( 0xBB1B86                         ); // TBLWTL W6, [W7++]
        putCmd_NOP(                                  ); // NOP
        putCmd_NOP(                                  ); // NOP

        // Initiate the write cycle
             if(progPIC instanceof ProgPIC24  ) putCmds_initiateEWCycle_pic24  ();
        else if(progPIC instanceof ProgDSPIC30) putCmds_initiateEWCycle_dspic30();
        else                                    return false;

        if( !exeCmds(progPIC) ) return false;

        // Wait NVM
             if(progPIC instanceof ProgPIC24  ) { if( !exeCmds_waitNVM_pic24  (progPIC) ) return false; }
        else if(progPIC instanceof ProgDSPIC30) { if( !exeCmds_waitNVM_dspic30(progPIC) ) return false; }
        else                                    return false;

        // Reset the device internal PC
        return exeCmds_resetInternalPC(progPIC);
    }

    // ----- ----- -----

    public boolean exeCmds_writeCWMemory_pic24(final ProgPIC progPIC, final int nvmcmd, final int address24, final int value16)
    { return _exeCmds_writeCWMemory_pic24_dspic30_impl(progPIC, nvmcmd, address24, value16); }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public boolean exeCmds_writeCWMemory_pic30(final ProgPIC progPIC, final int address24, final int value16)
    { return _exeCmds_writeCWMemory_pic24_dspic30_impl(progPIC, 0x4008, address24, value16); }

    // ----- ----- -----

    public boolean exeCmds_writeCWMemory_dspic33ep(final ProgPIC progPIC, final int address24, final int value16_1, final int value16_2)
    {
        // NOTE : The configuration words are not contiguous within the address space; therefore,
        //        theoretically, 'value16_2' should always be set to 0xFFFF.

        // Initialize TBLPAG
        //            ...xx.
        putCmd_SIX( 0x200FA0                         ); // MOV    #0xFA            , W0
        putCmd_SIX( 0x8802A0                         ); // MOV    W0               , TBLPAG

        // Load the two configuration words
        //            .xxxx.
        putCmd_SIX( 0x200000 | value16_1        << 4 ); // MOV    #<value16_1>     , W0
        putCmd_SIX( 0x2FFFF1                         ); // MOV    #0xFFFF          , W1
        putCmd_SIX( 0x200002 | value16_2        << 4 ); // MOV    #<value16_2>     , W2
        putCmd_SIX( 0x2FFFF3                         ); // MOV    #0xFFFF          , W3

        // Initialize the write pointer (W7) and load the write latches
        putCmd_SIX( 0xEB0380                         ); // CLR    W7
        putCmd_NOP(                                  ); // NOP
        putCmd_SIX( 0xBB0B80                         ); // TBLWTL W0               , [W7]
        putCmd_NOP(                                  ); // NOP
        putCmd_NOP(                                  ); // NOP
        putCmd_SIX( 0xBB9B81                         ); // TBLWTH W1               , [W7++]
        putCmd_NOP(                                  ); // NOP
        putCmd_NOP(                                  ); // NOP
        putCmd_SIX( 0xBB0B82                         ); // TBLWTL W2               , [W7]
        putCmd_NOP(                                  ); // NOP
        putCmd_NOP(                                  ); // NOP
        putCmd_SIX( 0xBB9B83                         ); // TBLWTH W3               , [W7++]
        putCmd_NOP(                                  ); // NOP
        putCmd_NOP(                                  ); // NOP

        // Set the NVMADRU/NVMADR register pair to point to the correct configuration word address
        //            .xxxx.
        putCmd_SIX( 0x200000 | madrW(address24) << 4 ); // MOV    #<address[15:00]>, W0
        //            ...xx.
        putCmd_SIX( 0x200001 | madrP(address24) << 4 ); // MOV    #<address[23:16]>, W1
        putCmd_SIX( 0x883950                         ); // MOV    W0               , NVMADR
        putCmd_SIX( 0x883961                         ); // MOV    W1               , NVMADRU

        // Set the NVMCON register to program two configuration words
        putCmd_SIX( 0x24001A                         ); // MOV    #0x4001          , W10
        putCmd_NOP(                                  ); // NOP
        putCmd_SIX( 0x88394A                         ); // MOV    W10              , NVMCON
        putCmd_NOP(                                  ); // NOP
        putCmd_NOP(                                  ); // NOP

        // Initiate the write cycle
        setExtraNOPs(true ); // NOTE : Needed as stated in the 'Flash Programming Specification' // ##### ??? TODO : IS IT REALLY ??? ####

        putCmds_initiateEWCycle_dspic33ep();

        setExtraNOPs(false);

        if( !exeCmds(progPIC) ) return false;

        // Wait NVM and reset the device internal PC
        return exeCmds_waitNVM_dspic33ep_and_resetInternalPC(progPIC);
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean exeCmds_chipErase_pic24(final ProgPIC progPIC, final int nvmcmd)
    {
        // Set the NVMCON to erase all program memory
        putCmd_SIX( 0x20000A | nvmcmd << 4 ); // MOV    #<nvmcmd>, W10
        putCmd_SIX( 0x883B0A               ); // MOV    W10      , NVMCON

        // Set TBLPAG and perform dummy table write to select what portions of memory are erased
        //            ...xx.
        putCmd_SIX( 0x200000               ); // MOV    #0x0000  , W0
        putCmd_SIX( 0x880190               ); // MOV    W0       , TBLPAG
        putCmd_SIX( 0xBB0800               ); // TBLWTL W0       , [W0]
        putCmd_NOP(                        ); // NOP
        putCmd_NOP(                        ); // NOP

        // Initiate the erase cycle
        putCmds_initiateEWCycle_pic24();

        if( !exeCmds(progPIC) ) return false;

        // Wait NVM
        if( !exeCmds_waitNVM_pic24(progPIC) ) return false;

        // Reset the device internal PC
        if( !exeCmds_resetInternalPC(progPIC) ) return false;

        // Done
        return true;
    }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public boolean exeCmds_chipErase_dspic30(final ProgPIC progPIC)
    {
        //*
        // Set the NVMCON to erase all program memory
        putCmd_SIX( 0x20000A | 0x407F << 4 ); // MOV #0x407F, W10
        putCmd_SIX( 0x883B0A               ); // MOV W10    , NVMCON

        // Initiate the erase cycle
        putCmds_initiateEWCycle_dspic30();

        if( !exeCmds(progPIC) ) return false;

        // Wait NVM
        if( !exeCmds_waitNVM_dspic30(progPIC) ) return false;

        // Reset the device internal PC
        if( !exeCmds_resetInternalPC(progPIC) ) return false;
        //*/

        /*
        // ##### ??? TODO : Is this part needed ??? #####
        // Set the NVMCON to erase all FBS, FSS, and FGS
        putCmd_SIX( 0x20000A | 0x406E << 4 ); // MOV #0x406E, W10
        putCmd_SIX( 0x883B0A               ); // MOV W10    , NVMCON

        // Initiate the erase cycle
        putCmds_initiateEWCycle_dspic30();

        if( !exeCmds(progPIC) ) return false;

        // Wait NVM
        if( !exeCmds_waitNVM_dspic30(progPIC) ) return false;

        // Reset the device internal PC
        if( !exeCmds_resetInternalPC(progPIC) ) return false;
        //*/

        // Done
        return true;
    }

    // ----- ----- -----

    public boolean exeCmds_chipErase_dspic33ep(final ProgPIC progPIC)
    {
        // Set the NVMCON to erase all program memory
        putCmd_SIX( 0x2400EA ); // MOV    #0x400E  , W10
        putCmd_SIX( 0x88394A ); // MOV    W10      , NVMCON
        putCmd_NOP(          ); // NOP
        putCmd_NOP(          ); // NOP

        // Initiate the erase cycle
        putCmds_initiateEWCycle_dspic33ep();

        if( !exeCmds(progPIC) ) return false;

        // Wait NVM and reset the device internal PC
        return exeCmds_waitNVM_dspic33ep_and_resetInternalPC(progPIC);
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean exeCmds_peErase_pic24(final ProgPIC progPIC, final int nvmcmd, final int startAddress24, final int incAddress, final int repCnt, final int waitEraseMS)
    {
            // Set the NVMCON to erase a page
            putCmd_SIX( 0x200000 | nvmcmd           << 4 ); // MOV    #<nvmcmd>        , W0
            putCmd_SIX( 0x883B00                         ); // MOV    W0               , NVMCON

            if( !exeCmds(progPIC) ) return false;

        // Erase the executive memory page by page
        int address24 = startAddress24;

        for(int r = 0; r < repCnt; ++r) {

            // Initialize the erase pointers to the page address
            //            ...xx.
            putCmd_SIX( 0x200000 | madrP(address24) << 4 ); // MOV    #<address[23:16]>, W0
            putCmd_SIX( 0x880190                         ); // MOV    W0               , TBLPAG
            //            .xxxx.
            putCmd_SIX( 0x200001 | madrW(address24) << 4 ); // MOV    #<address[15:00]>, W1
            putCmd_NOP(                                  ); // NOP

            // Initiate the erase cycle
            putCmd_SIX( 0xBB0881                         ); // TBLWTL W1               , [W1]
            putCmd_NOP(                                  ); // NOP
            putCmd_NOP(                                  ); // NOP

            putCmds_initiateEWCycle_pic24();

            if( !exeCmds(progPIC) ) return false;

            // Wait for a while
            if(waitEraseMS > 0) SysUtil.sleepMS(waitEraseMS);

            // Wait NVM
            if( !exeCmds_waitNVM_pic24(progPIC) ) return false;

            // Update the address
            address24 += incAddress;

        } // for

            // Reset the device internal PC
            if( !exeCmds_resetInternalPC(progPIC) ) return false;

       // Done
       return true;
    }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public boolean exeCmds_peErase_dspic30(final ProgPIC progPIC)
    {
        // Set the NVMCON to erase the programming executive
        putCmd_SIX( 0x24072A ); // MOV #0x4072, W10
        putCmd_SIX( 0x883B0A ); // MOV W10    , NVMCON

        // Initiate the write cycle
        putCmds_initiateEWCycle_dspic30();

        if( !exeCmds(progPIC) ) return false;

        // Wait NVM
        if( !exeCmds_waitNVM_dspic30(progPIC) ) return false;

        // Reset the device internal PC
        return exeCmds_resetInternalPC(progPIC);
    }

    // ----- ----- -----

    public boolean exeCmds_peErase_dspic33ep(final ProgPIC progPIC, final int startAddress24, final int incAddress, final int repCnt)
    {
        // ##### !!! TODO : VERIFY THE IMPLEMENTATION !!! #####

        // Erase the executive memory page by page
        int address24 = startAddress24;

        for(int r = 0; r < repCnt; ++r) {

            // Set the NVMADRU/NVMADR register pair to point to the correct address
            //            .xxxx.
            putCmd_SIX( 0x200000 | madrW(address24) << 4 ); // MOV    #<address[15:00]>, W0
            //            ...xx.
            putCmd_SIX( 0x200001 | madrP(address24) << 4 ); // MOV    #<address[23:16]>, W1
            putCmd_SIX( 0x883950                         ); // MOV    W0               , NVMADR
            putCmd_SIX( 0x883961                         ); // MOV    W1               , NVMADRU

            // Set the NVMCON to erase a page
            putCmd_SIX( 0x240030                         ); // MOV    #0x4003          , W0
            putCmd_SIX( 0x883940                         ); // MOV    W0               , NVMCON
            putCmd_NOP(                                  ); // NOP
            putCmd_NOP(                                  ); // NOP

            // Initiate the write cycle
            putCmds_initiateEWCycle_dspic33ep();

            if( !exeCmds(progPIC) ) return false;

            // Wait NVM and reset the device internal PC
            if( !exeCmds_waitNVM_dspic33ep_and_resetInternalPC(progPIC) ) return false;

            // Update the address
            address24 += incAddress;

        } // for

       // Done
       return true;
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean exeCmds_readCodeMemory_pic24(final ProgPIC progPIC, final ProgPIC.Config.MemoryFlash memoryFlash, final int address24_, final int[] dstBuff)
    {
        // Error if the address or number of bytes is not multiple of 'memoryFlash.writeBlockSize'
        if( address24_      % memoryFlash.writeBlockSize != 0 ) return false;
        if( dstBuff.length  % memoryFlash.writeBlockSize != 0 ) return false;

        /*
        SysUtil.stdDbg().printf("### RD CODE [%06X] : %04d\n", address24_, dstBuff.length);
        //*/

        // Determine the number of chunks
        final int ChunkSize = memoryFlash.writeBlockSize;
        final int numChunks = dstBuff.length / ChunkSize;

        // Read the chunks
        int address24 = address24_;

        for(int c = 0; c < numChunks; ++c) {

                    // ##### !!! TODO : Optimize speed !!! #####

                    // Initialize TBLPAG, the read pointer (W6), and the write pointer (W7) for the TBLRD instruction
                    //               ...xx.
                    putCmd_SIX   ( 0x200000 | cadrP(address24) << 4 ); // MOV      #<address[23:16]>, W0
                    putCmd_SIX   ( 0x880190                         ); // MOV      W0               , TBLPAG
                    //               .xxxx.
                    putCmd_SIX   ( 0x200006 | cadrW(address24) << 4 ); // MOV      #<address[15:00]>, W6
                    putCmd_SIX   ( 0x207847                         ); // MOV      #VISI            , W7
                    putCmd_NOP   (                                  ); // NOP

                    /*
                    putCmd_SIX   ( 0x200800 );
                    putCmd_SIX   ( 0x880190 );
                    putCmd_SIX   ( 0xEB0300 );
                    putCmd_SIX   ( 0x207847 );
                    putCmd_NOP   (          );
                    //*/

                    if( !exeCmds(progPIC) ) return false;

            int idx = 0;

            while(idx < ChunkSize) {

                for(int i = 0; i < 2; ++i ) {

                    // Read and clock out the values through the VISI register using the REGOUT commands
                    putCmd_SIX   ( 0xBA0B96                         ); // TBLRDL   [W6]             , [W7]
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_REGOUT(                                  ); // Clock out the contents of the VISI register
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_SIX   ( 0xBADBB6                         ); // TBLRDH.B [W6++]           , [W7++]
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_SIX   ( 0xBAD3D6                         ); // TBLRDH.B [++W6]           , [W7--]
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_REGOUT(                                  ); // Clock out the contents of the VISI register
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_SIX   ( 0xBA0BB6                         ); // TBLRDL   [W6++]           , [W7]
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_REGOUT(                                  ); // Clock out the contents of the VISI register
                    putCmd_NOP   (                                  ); // NOP

                } // for

                    // Reset the device internal PC
                    putCmds_resetInternalPC();

                    // Execute the command sequence
                    final int[] buff = getCmdsBuff();

                    if( !exeCmds(progPIC, buff) ) return false;

                    // Decode and store the results
                    for(int i = 0; i < 6; ++i) _buff6[i] = decCmdsBuff_U16(buff, i);

                // Unpack the bytes
                idx = unpackBytes_w16x6_to_b8x12(_buff6, dstBuff, c * ChunkSize + idx) - c * ChunkSize;

            } // while

            // Update the address
            address24 += ChunkSize;

        } // for

        // Done
        return true;
    }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public boolean exeCmds_readCodeMemory_dspic30(final ProgPIC progPIC, final ProgPIC.Config.MemoryFlash memoryFlash, final int address24, final int[] dstBuff)
    { return exeCmds_readCodeMemory_pic24(progPIC, memoryFlash, address24, dstBuff); }

    // ----- ----- -----

    public boolean exeCmds_readCodeMemory_dspic33ep(final ProgPIC progPIC, final ProgPIC.Config.MemoryFlash memoryFlash, final int address24_, final int[] dstBuff)
    {
        // Error if the address or number of bytes is not multiple of 'memoryFlash.writeBlockSize'
        if( address24_      % memoryFlash.writeBlockSize != 0 ) return false;
        if( dstBuff.length  % memoryFlash.writeBlockSize != 0 ) return false;

        // Determine the number of chunks
        final int ChunkSize = memoryFlash.writeBlockSize;
        final int numChunks = dstBuff.length / ChunkSize;

        // Read the chunks
        int address24 = address24_;

        for(int c = 0; c < numChunks; ++c) {

                    // ##### !!! TODO : Optimize speed !!! #####

                    // Initialize the TBLPAG register and the read rointer (W6) for the TBLRD instruction
                    //               ...xx.
                    putCmd_SIX   ( 0x200000 | cadrP(address24) << 4 ); // MOV      #<address[23:16]>, W0
                    putCmd_SIX   ( 0x8802A0                         ); // MOV      W0               , TBLPAG
                    //               .xxxx.
                    putCmd_SIX   ( 0x200006 | cadrW(address24) << 4 ); // MOV      #<address[15:00]>, W6

                    if( !exeCmds(progPIC) ) return false;

            int idx = 0;

            while(idx < ChunkSize) {

                    // Initialize the write pointer (W7) and store the next four locations of code memory to W0:W5
                    putCmd_SIX   ( 0xEB0380                         ); // CLR      W7
                    putCmd_NOP   (                                  ); // NOP

                for(int i = 0; i < 2; ++i ) {

                    putCmd_SIX   ( 0xBA1B96                         ); // TBLRDL   [W6]             , [W7++]
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_SIX   ( 0xBADBB6                         ); // TBLRDH.B [W6++]           , [W7++]
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_SIX   ( 0xBADBD6                         ); // TBLRDH.B [++W6]           , [W7++]
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_SIX   ( 0xBA1BB6                         ); // TBLRDL   [W6++]           , [W7++]
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP
                    putCmd_NOP   (                                  ); // NOP

                } // for

                    // Output W0:W5 using the VISI register and REGOUT command
                    // ##### ??? TODO : See below ??? #####
                for(int i = 0; i <= 5; ++i ) {

                    putCmd_SIX   ( 0x887C40 | i                     ); // MOV      Wn               , VISI
                    putCmd_NOP   (                                  ); // NOP

                    putCmd_REGOUT(                                  ); // Clock out the contents of the VISI register
                    putCmd_NOP   (                                  ); // NOP

                } // for

                    // Reset the device internal PC
                    putCmds_resetInternalPC();

                    // Execute the command sequence
                    final int[] buff = getCmdsBuff();

                    if( !exeCmds(progPIC, buff) ) return false;

                    // Decode and store the results
                    // ##### ??? TODO : Use '6' because the above one uses 'W0:W5' ??? #####
                    for(int i = 0; i < 3; ++i) _buff3[i] = decCmdsBuff_U16(buff, i);
                  //for(int i = 0; i < 6; ++i) _buff6[i] = decCmdsBuff_U16(buff, i);

                    // ##### ??? TODO : Use '6' because the above one uses 'W0:W5' ??? #####
                idx = unpackBytes_w16x3_to_b8x6 (_buff3, dstBuff, c * ChunkSize + idx) - c * ChunkSize;
              //idx = unpackBytes_w16x6_to_b8x12(_buff6, dstBuff, c * ChunkSize + idx) - c * ChunkSize;

            } // while

            // Update the address
            address24 += ChunkSize;

        } // for

        // Done
        return true;
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _exeCmds_writeCodeMemory_pic24_dspic30_impl(final ProgPIC progPIC, final ProgPIC.Config.MemoryFlash memoryFlash, final int nvmcmd, final int latchCnt, final boolean firstCall, final int address24_, final int[] srcBuff)
    {
        // Error if the address or number of bytes is not multiple of 'memoryFlash.writeBlockSize'
        if( address24_     % memoryFlash.writeBlockSize != 0 ) return false;
        if( srcBuff.length % memoryFlash.writeBlockSize != 0 ) return false;

        /*
        SysUtil.stdDbg().printf("### WR CODE [%06X] : %04d\n", address24_, srcBuff.length);
        //*/

        // Determine the number of chunks
        final int ChunkSize = memoryFlash.writeBlockSize;
        final int numChunks = srcBuff.length / ChunkSize;

        // Write the chunks
        int address24 = address24_;

        for(int c = 0; c < numChunks; ++c) {

            // ##### !!! TODO : Optimize speed !!! #####

            if( firstCall || (progPIC instanceof ProgDSPIC30) ) {
                        // Set the NVMCON to program 32/64 instruction words
                        putCmd_SIX( 0x20000A | nvmcmd           << 4 ); // MOV      #<nvmcmd>        , W10
                        putCmd_SIX( 0x883B0A                         ); // MOV      W10              , NVMCON
                        if( !exeCmds(progPIC) ) return false;
            }

            int idx = 0;

            while(idx < ChunkSize) {

                        // Initialize the write pointer (W7) for the TBLWT instruction
                        //            ...xx.
                        putCmd_SIX( 0x200000 | cadrP(address24) << 4 ); // MOV      #<address[23:16]>, W0
                        putCmd_SIX( 0x880190                         ); // MOV      W0               , TBLPAG
                        //            .xxxx.
                        putCmd_SIX( 0x200007 | cadrW(address24) << 4 ); // MOV      #<address[15:00]>, W7
                      //putCmd_NOP(                                  ); // NOP
                      //putCmd_NOP(                                  ); // NOP

                        if( !exeCmds(progPIC) ) return false;

                        /*
                        System.out.printf( "    #%d [%02X:%04X] (%03d:%05d)\n", c, (address24 & 0xFF0000) >>> 12, (address24 & 0x00FFFF), (address24 & 0xFF0000) >>> 12, (address24 & 0x00FFFF) );
                        //*/

                for(int i = 0; i < latchCnt; ++i) {

                        /*
                        int pidx = idx;
                        //*/

                        // Pack the bytes
                        idx = packBytes_b8x12_to_w16x6(srcBuff, c * ChunkSize + idx, _buff6) - c * ChunkSize;

                        // Load W0:W5 with the next 4 instruction words to program
                        putCmd_SIX( 0x200000 | (_buff6[0] << 4)      ); // MOV      #<LSW0>          , W0
                        putCmd_SIX( 0x200001 | (_buff6[1] << 4)      ); // MOV      #<MSB1:MSB0>     , W1
                        putCmd_SIX( 0x200002 | (_buff6[2] << 4)      ); // MOV      #<LSW1>          , W2
                        putCmd_SIX( 0x200003 | (_buff6[3] << 4)      ); // MOV      #<LSW2>          , W3
                        putCmd_SIX( 0x200004 | (_buff6[4] << 4)      ); // MOV      #<MSB3:MSB2>     , W4
                        putCmd_SIX( 0x200005 | (_buff6[5] << 4)      ); // MOV      #<LSW3>          , W5

                        /*
                        SysUtil.stdDbg().printf("%06X [%06X] : %04X %04X %04X\n", cadr(address24), address24 + pidx + 0, _buff6[0], _buff6[1], _buff6[2]);
                        SysUtil.stdDbg().printf("%06X [%06X] : %04X %04X %04X\n", cadr(address24), address24 + pidx + 6, _buff6[3], _buff6[4], _buff6[5]);
                        //*/

                        // Set the read pointer (W6) and load the (next set of) write latches
                        putCmd_SIX( 0xEB0300                         ); // CLR      W6
                        putCmd_NOP(                                  ); // NOP

                    for(int j = 0; j < 2; ++j) {

                        putCmd_SIX( 0xBB0BB6                         ); // TBLWTL   [W6++]           , [W7]
                        putCmd_NOP(                                  ); // NOP
                        putCmd_NOP(                                  ); // NOP
                        putCmd_SIX( 0xBBDBB6                         ); // TBLWTH.B [W6++]           , [W7++]
                        putCmd_NOP(                                  ); // NOP
                        putCmd_NOP(                                  ); // NOP
                        putCmd_SIX( 0xBBEBB6                         ); // TBLWTH.B [W6++]           , [++W7]
                        putCmd_NOP(                                  ); // NOP
                        putCmd_NOP(                                  ); // NOP
                        putCmd_SIX( 0xBB1BB6                         ); // TBLWTL   [W6++]           , [W7++]
                        putCmd_NOP(                                  ); // NOP
                        putCmd_NOP(                                  ); // NOP

                    } // for

                        if( !exeCmds(progPIC) ) return false;

                } // for

                        // Initiate the write cycle
                             if(progPIC instanceof ProgPIC24  ) putCmds_initiateEWCycle_pic24  ();
                        else if(progPIC instanceof ProgDSPIC30) putCmds_initiateEWCycle_dspic30();
                        else                                    return false;

                        if( !exeCmds(progPIC) ) return false;

                        // Wait NVM
                             if(progPIC instanceof ProgPIC24  ) { if( !exeCmds_waitNVM_pic24  (progPIC) ) return false; }
                        else if(progPIC instanceof ProgDSPIC30) { if( !exeCmds_waitNVM_dspic30(progPIC) ) return false; }
                        else                                    return false;

                        // Reset the device internal PC
                        if( !exeCmds_resetInternalPC(progPIC) ) return false;

            } // while

            // Update the address
            address24 += ChunkSize;

        } // for

        // Done
        return true;
    }

    // ----- ----- -----

    public boolean exeCmds_writeCodeMemory_pic24(final ProgPIC progPIC, final ProgPIC.Config.MemoryFlash memoryFlash, final int nvmcmd, final int latchCnt, final boolean firstCall, final int address24, final int[] srcBuff)
    { return _exeCmds_writeCodeMemory_pic24_dspic30_impl(progPIC, memoryFlash, nvmcmd, latchCnt, firstCall, address24, srcBuff); }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public boolean exeCmds_writeCodeMemory_dspic30(final ProgPIC progPIC, final ProgPIC.Config.MemoryFlash memoryFlash, final boolean firstCall, final int address24, final int[] srcBuff)
    { return _exeCmds_writeCodeMemory_pic24_dspic30_impl(progPIC, memoryFlash, 0x4001, 8, firstCall, address24, srcBuff); }

    // ----- ----- -----

    public boolean exeCmds_writeCodeMemory_dspic33ep(final ProgPIC progPIC, final ProgPIC.Config.MemoryFlash memoryFlash, final boolean firstCall, final int address24_, final int[] srcBuff)
    {
        // Error if the address or number of bytes is not multiple of 'memoryFlash.writeBlockSize'
        if( address24_     % memoryFlash.writeBlockSize != 0 ) return false;
        if( srcBuff.length % memoryFlash.writeBlockSize != 0 ) return false;

        // Determine the number of chunks
        final int ChunkSize = memoryFlash.writeBlockSize;
        final int numChunks = srcBuff.length / ChunkSize;

        // Write the chunks
        int address24 = address24_;

                // ##### ??? TODO : 'firstCall' ??? #####

                // ##### !!! TODO : Optimize speed !!! #####

                // Initialize the TBLPAG register and the read rointer (W6) for the TBLRD instruction
                putCmd_SIX( 0x200FA0                         ); // MOV      #0xFA            , W0
                putCmd_SIX( 0x8802A0                         ); // MOV      W0               , TBLPAG

                if( !exeCmds(progPIC) ) return false;

        for(int c = 0; c < numChunks; ++c) {

            int idx = 0;

            while(idx < ChunkSize) {

                // Pack the bytes
                idx = packBytes_b8x6_to_w16x3(srcBuff, c * ChunkSize + idx, _buff3) - c * ChunkSize;

                // Load the two packed instruction words
                putCmd_SIX( 0x200000 | _buff3[0]        << 4 ); // MOV      #<LSW0>          , W0
                putCmd_SIX( 0x200001 | _buff3[1]        << 4 ); // MOV      #<MSB1:MSB0>     , W1
                putCmd_SIX( 0x200002 | _buff3[2]        << 4 ); // MOV      #<LSW1>          , W2

                // Set the read pointer (W6) and write pointer (W7), and load the write latches
                putCmd_SIX( 0xEB0300                         ); // CLR      W6
                putCmd_NOP(                                  ); // NOP
                putCmd_SIX( 0xEB0380                         ); // CLR      W7
                putCmd_NOP(                                  ); // NOP
                putCmd_SIX( 0xBB0BB6                         ); // TBLWTL   [W6++]           , [W7]
                putCmd_NOP(                                  ); // NOP
                putCmd_NOP(                                  ); // NOP
                putCmd_SIX( 0xBBDBB6                         ); // TBLWTH.B [W6++]           , [W7++]
                putCmd_NOP(                                  ); // NOP
                putCmd_NOP(                                  ); // NOP
                putCmd_SIX( 0xBBEBB6                         ); // TBLWTH.B [W6++]           , [++W7]
                putCmd_NOP(                                  ); // NOP
                putCmd_NOP(                                  ); // NOP
                putCmd_SIX( 0xBB0B96                         ); // TBLWTL.W [W6]             , [W7++]
                putCmd_NOP(                                  ); // NOP
                putCmd_NOP(                                  ); // NOP

                // Set the NVMADRU/NVMADR register pair to point to the correct address
                //            .xxxx.
                putCmd_SIX( 0x200000 | cadrW(address24) << 4 ); // MOV      #<address[15:00]>, W0
                //            ...xx.
                putCmd_SIX( 0x200001 | cadrP(address24) << 4 ); // MOV      #<address[23:16]>, W1
                putCmd_SIX( 0x883950                         ); // MOV      W0               , NVMADR
                putCmd_SIX( 0x883961                         ); // MOV      W1               , NVMADRU

                // Set the NVMCON register to program 2 instruction words
                putCmd_SIX( 0x24001A                         ); // MOV      #0x4001,         , W10
                putCmd_NOP(                                  ); // NOP
                putCmd_SIX( 0x88394A                         ); // MOV      W10              , NVMCON
                putCmd_NOP(                                  ); // NOP
                putCmd_NOP(                                  ); // NOP

                // Initiate the write cycle
                putCmds_initiateEWCycle_dspic33ep();

                if( !exeCmds(progPIC) ) return false;

                // Wait NVM and reset the device internal PC
                if( !exeCmds_waitNVM_dspic33ep_and_resetInternalPC(progPIC) ) return false;

            } // while

            // Update the address
            address24 += ChunkSize;

        } // for

        // Done
        return true;
    }

    // ##### !!! TODO : Other dsPIC33 variants !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean _exeCmds_eraseDataMemory_pic24_dspic30_impl(final ProgPIC progPIC, final int nvmcmd, final int eraseBSize, final int rowTotal, final int address24)
    {
            // Initialize NVMADR and NVMADRU to erase data memory and initialize W7 for row address updates
            //            ...xx.
            putCmd_SIX( 0x200006 | madrP(address24) << 4 ); // MOV #<address[23:16]>, W6
            putCmd_SIX( 0x883B26                         ); // MOV W6               , NVMADRU
            //            .xxxx.
            putCmd_SIX( 0x200006 | madrW(address24) << 4 ); // MOV #<address[15:00]>, W6
            putCmd_SIX( 0x883B16                         ); // MOV W6               , NVMADR

            // Set W7 to the number of bytes that are simultaneously erased (16/32 bytes)
            putCmd_SIX( 0x200007 | eraseBSize       << 4 ); // MOV $<eraseBSize>    , W7

            if( !exeCmds(progPIC) ) return false;

        for(int i = 0; i < rowTotal; ++i) {

            // Set NVMCON to erase 1 row of data memory => 8/16 words (16/32 bytes) at once
            // ##### !!! TODO : PIC24 may only need to set this once !!! #####
            putCmd_SIX( 0x20000A | nvmcmd           << 4 ); // MOV #<nvmcmd>        , W10
            putCmd_SIX( 0x883B0A                         ); // MOV W10              , NVMCON

            // Initiate the write cycle
                 if(progPIC instanceof ProgPIC24  ) putCmds_initiateEWCycle_pic24  ();
            else if(progPIC instanceof ProgDSPIC30) putCmds_initiateEWCycle_dspic30();
            else                                    return false;

            if( !exeCmds(progPIC) ) return false;

            // Wait NVM
                 if(progPIC instanceof ProgPIC24  ) { if( !exeCmds_waitNVM_pic24  (progPIC) ) return false; }
            else if(progPIC instanceof ProgDSPIC30) { if( !exeCmds_waitNVM_dspic30(progPIC) ) return false; }
            else                                    return false;

            // Update the row address stored in NVMADR
            putCmd_SIX( 0x430307                         ); // ADD W6,              , W7, W6
            putCmd_SIX( 0x883B16                         ); // MOV W6               , NVMADR

            // Reset the device internal PC
            putCmds_resetInternalPC();

            if( !exeCmds(progPIC) ) return false;

        } // for

        // Done
        return true;
    }

    public boolean _exeCmds_writeDataMemory_pic24_dspic30_impl(final ProgPIC progPIC, final int nvmcmd, final int rowWSize, final int address24, final int[] srcBuff_)
    {
         // Combine bytes to words
        final int[] srcBuff = new int[srcBuff_.length / 2];

        for(int i = 0; i < srcBuff.length; ++i) {
            srcBuff[i] = srcBuff_[i * 2 + 0] | ( srcBuff_[i * 2 + 1] << 8 );
        }

                // ##### !!! TODO : Optimize speed (by using row?) !!! #####

                // Initialize the write pointer (W7) for the TBLWT instruction
                //            ...xx.
                putCmd_SIX( 0x200000 | madrP(address24) << 4 ); // MOV    #<address[23:16]>, W0
                putCmd_SIX( 0x880190                         ); // MOV    W0               , TBLPAG
                //            .xxxx.
                putCmd_SIX( 0x200007 | madrW(address24) << 4 ); // MOV    #<address[15:00]>, W7

                if( !exeCmds(progPIC) ) return false;

        // Write the data
        for(int i = 0; i < srcBuff.length; i += rowWSize) {

                // Set the NVMCON to program one data word
                // ##### !!! TODO : PIC24 may only need to set this once !!! #####
                putCmd_SIX( 0x20000A | nvmcmd           << 4 ); // MOV    #<nvmcmd>        , W10
                putCmd_SIX( 0x883B0A                         ); // MOV    W10              , NVMCON

            if( !exeCmds(progPIC) ) return false;

            for(int j = 0; j < rowWSize; ++j) {
                // Load W0 with the data word to program and load the write latch
                putCmd_SIX( 0x200000 | srcBuff[i + j]   << 4 ); // MOV    #<data[15:0]>, W0
                putCmd_SIX( 0xBB1B80                         ); // TBLWTL W0, [W7++]
                putCmd_NOP(                                  ); // NOP
                putCmd_NOP(                                  ); // NOP
            }

                // Initiate the write cycle
                     if(progPIC instanceof ProgPIC24  ) putCmds_initiateEWCycle_pic24  ();
                else if(progPIC instanceof ProgDSPIC30) putCmds_initiateEWCycle_dspic30();
                else                                    return false;

                if( !exeCmds(progPIC) ) return false;

                // Wait NVM
                     if(progPIC instanceof ProgPIC24  ) { if( !exeCmds_waitNVM_pic24  (progPIC) ) return false; }
                else if(progPIC instanceof ProgDSPIC30) { if( !exeCmds_waitNVM_dspic30(progPIC) ) return false; }
                else                                    return false;

                // Reset the device internal PC
                if( !exeCmds_resetInternalPC(progPIC) ) return false;

        } // for

        // Done
        return true;
    }

    // ----- ----- -----

    public boolean exeCmds_readDataMemory_pic24(final ProgPIC progPIC, final int address24, final int[] dstBuff_)
    {
        // Read the data
        final int[] dstBuff = new int[dstBuff_.length / 2];

        if( !exeCmds_readU16Memory_pic24(progPIC, address24, dstBuff) ) return false;

        // Extract words to bytes
        int idx = 0;

        for(final int v : dstBuff) {
            dstBuff_[idx++] = v & 0x00FF;
            dstBuff_[idx++] = v >>> 8;
        }

        // Done
        return true;
    }

    public boolean exeCmds_writeDataMemory_pic24(final ProgPIC progPIC, final int nvmcmd, final int address24, final int[] srcBuff, final boolean eraseBeforeWrite)
    {
        // ##### ??? TODO : 0x405A -> erase the data memory (by 8 words) ??? #####

        // Write the data memory (by one word)
        return _exeCmds_writeDataMemory_pic24_dspic30_impl(progPIC, nvmcmd, 1, address24, srcBuff);
    }

    // ##### !!! TODO : Other PIC24 variants !!! #####

    // ----- ----- -----

    public boolean exeCmds_readDataMemory_dspic30(final ProgPIC progPIC, final int address24, final int[] dstBuff)
    { return exeCmds_readDataMemory_pic24(progPIC, address24, dstBuff);}

    public boolean exeCmds_writeDataMemory_dspic30(final ProgPIC progPIC, final int address24, final int[] srcBuff, final boolean eraseBeforeWrite)
    {
        // Erase the data memory (by 16 words) as needed
        if(eraseBeforeWrite) {
            if( !_exeCmds_eraseDataMemory_pic24_dspic30_impl(progPIC, 0x4075, 32, srcBuff.length / 32, address24) ) return false;
        }

        //*
        // Write the data memory (by 16 words)
        return _exeCmds_writeDataMemory_pic24_dspic30_impl(progPIC, 0x4005, 16, address24, srcBuff);
        //*/

        /*
        // Write the data memory (by one word)
        return _exeCmds_writeDataMemory_pic24_dspic30_impl(progPIC, 0x4004, 1, address24, srcBuff);
        //*/
    }

    // ----- ----- -----

    // NOTE : It seems that none of the dsPIC33 MCUs have built-in EEPROM!

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ProgPIC.FWD fwDecompose(final ProgPIC.FWD fwd, final FWComposer fwc) throws Exception
    {
        // Simply exit of the decomposed data is null
        if(fwd == null) return null;

        // Process the flash data
        if(true) {
            // Check if the firmware data is padded
            final ArrayList<FWBlock> fwBlocks = fwc.fwBlocks();
                  boolean            all0x00  = true;
                  boolean            all0xFF  = true;
            for(final FWBlock fwb : fwBlocks) {
                if( fwb.startAddress() < fwd.fwStartAddress || fwb.finalAddress() >=  fwd.fwStartAddress + fwd.fwLength ) continue;
                for(int i = 0; i < fwb.length(); i += 4) {
                    if( fwb.data()[i + 3] != 0x00 ) all0x00 = false;
                    if( fwb.data()[i + 3] != 0xFF ) all0xFF = false;
                }
            }
            // Remove all padding bytes from the flattened data
            if(all0x00 || all0xFF) {
                // Remove the padding bytes
                final byte[]  fwDataBuff = new byte[fwd.fwLength * 3 / 4];
                      int     dstIdx     = 0;
                      int     srcIdx     = 0;
                while(srcIdx < fwd.fwLength) {
                    fwDataBuff[dstIdx++] = fwd.fwDataBuff[srcIdx++];
                    fwDataBuff[dstIdx++] = fwd.fwDataBuff[srcIdx++];
                    fwDataBuff[dstIdx++] = fwd.fwDataBuff[srcIdx++];
                                                          srcIdx++ ;
                }
                // Replace the flattened data
                fwd.fwDataBuff = fwDataBuff;
                fwd.fwLength   = fwDataBuff.length;
            }
        }

        // Process the EEPROM data
        if(true) {
            // Check if the firmware data is padded
            final ArrayList<FWBlock> fwBlocks = fwc.fwBlocks();
                  boolean            all0x00  = true;
                  boolean            all0xFF  = true;
            for(final FWBlock fwb : fwBlocks) {
                if( fwb.startAddress() < fwd.epStartAddress || fwb.finalAddress() >=  fwd.epStartAddress + fwd.epLength ) continue;
                for(int i = 0; i < fwb.length(); i += 4) {
                    if( fwb.data()[i + 2] != 0x00 ) all0x00 = false;
                    if( fwb.data()[i + 3] != 0x00 ) all0x00 = false;
                    if( fwb.data()[i + 3] != 0xFF ) all0xFF = false;
                    if( fwb.data()[i + 3] != 0xFF ) all0xFF = false;
                }
            }

            // Remove all padding bytes from the flattened data
            if(all0x00 || all0xFF) {
                // Remove the padding bytes
                final byte[]  epDataBuff = new byte[fwd.epLength / 2];
                      int     dstIdx     = 0;
                      int     srcIdx     = 0;
                while(srcIdx < fwd.epLength) {
                    epDataBuff[dstIdx++] = fwd.epDataBuff[srcIdx++];
                    epDataBuff[dstIdx++] = fwd.epDataBuff[srcIdx++];
                                                          srcIdx++ ;
                                                          srcIdx++ ;
                }
                // Replace the flattened data
                fwd.epDataBuff = epDataBuff;
                fwd.epLength   = epDataBuff.length;
            }
        }

        // Process the configuration byte data
        // NOTE : Do nothing here, because the configuration bytes data should be usable as it is

        // Return the decomposed data
        return fwd;
    }

} // class ProgPIC_SIX_REGOUT
