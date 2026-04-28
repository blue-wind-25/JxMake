/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import jxm.*;
import jxm.xb.*;


/*
 * A simple assembler and linker for 16-bit PIC EPC MCUs such as PIC24, dsPIC30, etc.)
 * It is intended to be used to assemble and link simple programs such as programming executive.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 *     # Compared to XC16 (and also XC-DSC), this class has slight differences in results and usage
 *       conventions:
 *           # All '.B' instruction suffixes are replaced by the '_b' suffix on the instruction names.
 *             The same convention also applies to the other suffixes. The '.w' suffixes are implicit,
 *             so none of the instruction names will include the '_w' suffixes.
 *           # All '#<value>' expressions are replaced by the function call 'lit(<value>)'.
 *           # All '[<*Wx*>]' expressions are replaced by the function call '*ind*(*)':
 *                 # [  Wx   ] :    ind   (Wx    )
 *                 # [  Wx-- ] :    ind_mm(Wx    )
 *                 # [  Wx++ ] :    ind_pp(Wx    )
 *                 # [--Wx   ] : mm_ind   (Wx    )
 *                 # [++Wx   ] : pp_ind   (Wx    )
 *                 # [  Wx+N ] :    ind   (Wx, N )
 *                 # [  Wx+Wb] :    ind   (Wx, Wb)
 *           # For instructions with relative addresses, such as '$bra' and '$rcall', it is possible to
 *             write "+<offset>" and "-<offset>" for the labels. Although using the special symbol for
 *             the current address ("." or "$") is not supported, the current address can be referred
 *             to by using "-2" as the offset. Please note that using offset labels with instructions
 *             that require absolute addresses will cause undefined behavior.
 *           # The 'NOPR' instruction is assembled as 0xFFFFFF instead of 0xFF0000.
 *           # Most of the DSP-related and some of the more MCU-specific instructions are not available
 *             for use!
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written based on the information found from:
 *
 *     PIC Instruction Listings - PIC24 and dsPIC 16-bit Microcontrollers
 *     https://en.wikipedia.org/wiki/PIC_instruction_listings#PIC24_and_dsPIC_16-bit_microcontrollers
 *
 *     16-Bit MCU and DSC Programmer's Reference Manual
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/70000157g.pdf
 *
 *     dsPIC30F/33F Programmer's Reference Manual
 *     https://ww1.microchip.com/downloads/en/devicedoc/70157c.pdf
 *
 *     XC16 Assembler, Linker, and Utilities User's Guide
 *     https://ww1.microchip.com/downloads/en/DeviceDoc/50002106C.pdf
 *
 * ~~~ Last accessed & checked on 2024-11-09 ~~~
 */
public class PIC16BitEPC {

    private static final String ClassName             = "PIC16BitEPC";

    private static final boolean ForceDumpDisassembly = false;
    private static final boolean ForceDumpBytes       = false;
    private static final boolean NeverDumpBytes       = true;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum CPU {

          PIC24F(0, 0x0148), // Use 'e_flags' for PIC24FJ256DA210
          PIC24E(1, 0x015B), // Use 'e_flags' for PIC24EP512GU814
          PIC24H(2, 0x0126), // Use 'e_flags' for PIC24HJ256GP206A

        dsPIC30F(3, 0x001A), // Use 'e_flags' for dsPIC30F6015

        dsPIC33F(4, 0x013C), // Use 'e_flags' for dsPIC33FJ256MC710A
        dsPIC33E(5, 0x0154), // Use 'e_flags' for dsPIC33EP512MU814
        dsPIC33C(6, 0x0681), // Use 'e_flags' for dsPIC33CK1024MP710

        _ANY_   (7, 0x0000)  // NOTE : Only use this for testing purpose!

        ;

        public final int value;
        public final int e_flags;

        private CPU(final int value_, final int e_flags_)
        {
            value   = value_;
            e_flags = e_flags_;
        }

        public boolean isPIC24()
        { return (value == PIC24F.value) || (value == PIC24E.value) || (value == PIC24H.value); }

        // PIC24E, dsPIC33E, and dsPIC33C MCUs
        public boolean extendedInstructionSet()
        { return (value == PIC24E.value) || (value == dsPIC33E.value) || (value == dsPIC33C.value) || (value == _ANY_.value); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static final String IAM_DIV_REP = "#div$rep#";
        private static final String IAM_STD_DSP = "<std_dsp>";
        private static final String IAM_CPS_XXX = "<cps_xxx>";
        private static final String IAM_CPB_XXX = "<cpb_xxx>";

        // Instruction-availability matrix
        @SuppressWarnings("serial")
        private static final HashMap<String, Integer[]> _iaMap = new HashMap<String, Integer[]>() {{
            // NOTE : Unless specifically stated, a value of zero means the instruction (or instruction group) is not available,
            //        while a non-zero value means it is available.

            //                       #Index   0     1     2     3     4     5     6     7
            //                       Series   24F   24E   24H   30F   33F   33E   33C   ANY
            put( IAM_DIV_REP, new Integer[] {  17,   17,   17,   17,   17,   17,    5,   17 } ); // The value is for the '#<ulit14/ulit15>' of the 'REPEAT' instruction that precedes a 'DIV*' instruction

            //                       #Index   0     1     2     3     4     5     6     7
            //                       Series   24F   24E   24H   30F   33F   33E   33C   ANY
            put( "divf"     , new Integer[] {   0,    0,    0,    1,    1,    1,    1,    1 } );
            put( "divf2"    , new Integer[] {   0,    0,    0,    0,    0,    0,    1,    1 } );
            put( "div2"     , new Integer[] {   0,    0,    0,    0,    0,    0,    1,    1 } );
            put( "do"       , new Integer[] {   0,    0,    0,   14,   14,   15,   15,   15 } ); // The value differentiates between '#<ulit14>' and '#<ulit15>'
            put( "repeat"   , new Integer[] {  14,   15,   14,   14,   14,   15,   15,   15 } ); // The value differentiates between '#<ulit14>' and '#<ulit15>'
            put( "lac_d"    , new Integer[] {   0,    0,    0,    0,    0,    0,    1,    1 } );
            put( "sac_d"    , new Integer[] {   0,    0,    0,    0,    0,    0,    1,    1 } );

            //                       #Index   0     1     2     3     4     5     6     7
            //                       Series   24F   24E   24H   30F   33F   33E   33C   ANY
            put( "bra[oa]"  , new Integer[] {   0,    0,    0,    1,    1,    1,    1,    1 } );
            put( "bra[ob]"  , new Integer[] {   0,    0,    0,    1,    1,    1,    1,    1 } );
            put( "bra[sa]"  , new Integer[] {   0,    0,    0,    1,    1,    1,    1,    1 } );
            put( "bra[sb]"  , new Integer[] {   0,    0,    0,    1,    1,    1,    1,    1 } );

            //                       #Index   0     1     2     3     4     5     6     7
            //                       Series   24F   24E   24H   30F   33F   33E   33C   ANY
            put( IAM_STD_DSP, new Integer[] {   0,    0,    0,    1,    1,    1,    1,    1 } );
            put( IAM_CPS_XXX, new Integer[] {   1,    0,    1,    1,    1,    0,    0,    1 } );
            put( IAM_CPB_XXX, new Integer[] {   0,    1,    0,    0,    0,    1,    1,    1 } );
        }};

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public int chkSupInst(final String mString, final String insChk, final String insName) throws JXMAsmError
        {
            // By default, if there is no entry on the instruction-availability matrix, it means the instruction
            // (or instruction group) is available
            final Integer[] ia = _iaMap.get( insChk.toLowerCase() );

            if(ia == null) return 0;

            // Throw an error if the instruction (or instruction group) is not available for the selected CPU series
            if( ia[this.value] == 0 ) throw XCom.newJXMAsmError( Texts.ASM_InvalidInstruction, ClassName, mString, insName.toLowerCase(), this.name() );

            // Return the integer value
            return ia[this.value].intValue();
        }

        public int chkSupInst(final String mString, final String insChk, final String insName, final Cond cond) throws JXMAsmError
        { return chkSupInst( mString, insChk + '[' + cond.name() + ']', insName + '[' + cond.name() + ']' ); }

        public int chkSupInst(final String mString, final String insName) throws JXMAsmError
        { return chkSupInst( mString, insName.substring(1), insName ); }

        public int chkSupInst(final String mString, final String insName, final Cond cond) throws JXMAsmError
        { return chkSupInst( mString, insName.substring(1), insName, cond ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void chkSupExtInst(final String mString, final String insName) throws JXMAsmError
        {
            // Throw an error if extended instruction set is not available for the selected CPU series
            if( !extendedInstructionSet() ) throw XCom.newJXMAsmError( Texts.ASM_InvalidInstruction, ClassName, mString, insName.toLowerCase(), this.name() );
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void chkSupInstGroup_stdDSP(final String mString, final String insName) throws JXMAsmError
        { chkSupInst( mString, IAM_STD_DSP, insName + "[dsp]" ); }

        public void chkSupInstGroup_cpsXXX(final String mString, final String insName) throws JXMAsmError
        { chkSupInst( mString, IAM_CPS_XXX, insName ); }

        public void chkSupInstGroup_cpbXXX(final String mString, final String insName) throws JXMAsmError
        { chkSupInst( mString, IAM_CPB_XXX, insName ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public _Lit getDivRep() throws JXMAsmError
        { return Expr.lit( this.chkSupInst(null, IAM_DIV_REP, IAM_DIV_REP) ); };

    } // enum CPU

    public static enum Cond {

        OV (  0), // 0b0000   V == 1             ; Branch if     Overflow
        NOV(  8), // 0b1000   V == 0             ; Branch if Not Overflow

        C  (  1), // 0b0001   C == 1             ; Branch if     Carry
        NC (  9), // 0b1001   C == 0             ; Branch if Not Carry

        Z  (  2), // 0b0010   Z == 1             ; Branch if     Zero
        NZ ( 10), // 0b1010   Z == 0             ; Branch if Not Zero

        N  (  3), // 0b0011   N == 1             ; Branch if     Negative
        NN ( 11), // 0b1011   N == 0             ; Branch if Not Negative

        LT (  5), // 0b0101             N != V   ; Branch if Signed   Less    Than
        LE (  4), // 0b0100   Z == 1 || N != V   ; Branch if Signed   Less    Than or Equal
        LTU(  9), // 0b1001   C == 0             ; Branch if Unsigned Less    Than          (NC)
        LEU(  6), // 0b0110   Z == 1 || C == 0   ; Branch if Unsigned Less    Than or Equal

        GT ( 12), // 0b1100   Z == 0 && N == V   ; Branch if Signed   Greater Than
        GE ( 13), // 0b1101             N == V   ; Branch if Signed   Greater Than or Equal
        GTU( 14), // 0b1110   Z == 0 && C == 1   ; Branch if Unsigned Greater Than
        GEU(  1), // 0b0001   C == 1             ; Branch if Unsigned Greater Than or Equal (C )


        ANY(  7), // 0b0111   Unconditionally

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        __DSP_COND_START__(100),

        OA (112), // 0b1100   OA == 1            ; Branch if Overflow   Accumulator A
        OB (113), // 0b1101   OB == 1            ; Branch if Overflow   Accumulator B
        SA (114), // 0b1110   SA == 1            ; Branch if Saturation Accumulator A
        SB (115), // 0b1111   SB == 1            ; Branch if Saturation Accumulator B

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        _NONE_( -1)

        ;

        public final int value;

        private Cond(final int value_)
        { value = value_; }

    } // enum Cond

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Accumulator register
    private static final class _RAcc {
        public final int regNum;

        public _RAcc(final int regNum_)
        { regNum = regNum_; }
    }

    // Target-page register
    private static final class _RTP {
        public final int regNum;
        public final int ulitLen;

        public _RTP(final int regNum_, final int ulitLen_)
        {
            regNum  = regNum_;
            ulitLen = ulitLen_;
        }

        public String _name()
        {
            switch(regNum) {
                case 0  : return "DSRPAG";
                case 1  : return "DSWPAG";
                case 2  : return "TBLPAG";
                default : return "???PAG";
            }
        }
    }

    // WREG register
    private static final class _WRg {}

    // Wx register
    private static final class _RWx {
        public final int regNum;

        public _RWx(final int regNum_)
        { regNum = regNum_; }

        public boolean evenReg()
        { return (regNum & 1) == 0; }

        public boolean oddReg()
        { return (regNum & 1) != 0; }

        public boolean anyRegGTE2()
        { return regNum >= 2; }

        public boolean evenRegLTE12()
        { return evenReg() && regNum <= 12; }

        public String _name()
        {
            switch(regNum) {
                case 13 : return "RA"         ;
                case 14 : return "FP"         ;
                case 15 : return "SP"         ;
                default : return "W"  + regNum;
            }
        }
    }

    // Registers
    public static final class Reg {

        public static final _RAcc A = new _RAcc(0);
        public static final _RAcc B = new _RAcc(1);

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static final _RTP DSRPAG = new _RTP(0, 10);
        public static final _RTP DSWPAG = new _RTP(1,  9);
        public static final _RTP TBLPAG = new _RTP(2,  8);

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static final _WRg WREG = new _WRg();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static final _RWx NA  = new _RWx(-1);

        public static final _RWx W0  = new _RWx( 0);
        public static final _RWx W1  = new _RWx( 1);
        public static final _RWx W2  = new _RWx( 2);
        public static final _RWx W3  = new _RWx( 3);
        public static final _RWx W4  = new _RWx( 4);
        public static final _RWx W5  = new _RWx( 5);
        public static final _RWx W6  = new _RWx( 6);
        public static final _RWx W7  = new _RWx( 7);
        public static final _RWx W8  = new _RWx( 8);
        public static final _RWx W9  = new _RWx( 9);
        public static final _RWx W10 = new _RWx(10);
        public static final _RWx W11 = new _RWx(11);
        public static final _RWx W12 = new _RWx(12);
        public static final _RWx W13 = new _RWx(13); public static final _RWx RA = W13;
        public static final _RWx W14 = new _RWx(14); public static final _RWx FP = W14;
        public static final _RWx W15 = new _RWx(15); public static final _RWx SP = W15;

    } // class Reg

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Replacement for the '#<value>' and '[<*Wx*>]' expressions
    private static final class _Lit {

        public final int value;

        private _Lit(final int value_)
        { value = value_; }

    } // class _Lit

    private static final class _RWxMem {

        public final _RWx Wx;
        public final _RWx Wb;
        public final int  N;
        public final int  mode;

        private _RWxMem(final int mode_, final _RWx Wx_, final _RWx Wb_, final int N_)
        {
            mode = mode_;
            Wx   = Wx_;
            Wb   = (Wb_ == Reg.NA) ? Reg.W0 : Wb_;
            N    = N_;
        }

        public boolean direct()
        { return (mode == 0b000); }

        public boolean direct_evenReg()
        { return direct() && Wx.evenReg(); }

        public boolean direct_oddReg()
        { return direct() && Wx.oddReg(); }

        public boolean immOfs()
        { return (mode == 0b001) && (N != 0xFFFF); }

        public boolean rwxOfs()
        { return (mode == 0b110); }

        public boolean immOfs_rwxOfs()
        { return immOfs() || rwxOfs(); }

        public String toString()
        {
            switch(mode) {
                case 0b000 : return                  "W" + Wx.regNum                          ;

                case 0b001 : return (N == 0xFFFF) ? "[W" + Wx.regNum +                    "]" :
                                    (N >= 0     ) ? "[W" + Wx.regNum + "+"  +   N       + "]" :
                                    (N <  0     ) ? "[W" + Wx.regNum + "-"  + (-N)      + "]" :
                                                    "###N-ERROR###"                           ;
                case 0b010 : return                 "[W" + Wx.regNum +                  "--]" ;
                case 0b011 : return                 "[W" + Wx.regNum +                  "++]" ;
                case 0b100 : return               "[--W" + Wx.regNum +                    "]" ;
                case 0b101 : return               "[++W" + Wx.regNum +                    "]" ;
                case 0b110 : return                 "[W" + Wx.regNum + "+W" + Wb.regNum + "]" ;
                default    : return                 "###INVALID###"                           ;
            }
        }

    } // class _RWxMem

    public static final class Expr {

        public  static _Lit       lit   (final int  value              ) { return new _Lit(value);                         } // #value
        private static _Lit      _lit0  (                              ) { return new _Lit(0    );                         } // #0

        private static _RWxMem   _reg   (final _RWx reg                ) { return new _RWxMem(0b000, reg, Reg.NA, 0     ); } //    Wx
        public  static _RWxMem    ind   (final _RWx reg                ) { return new _RWxMem(0b001, reg, Reg.NA, 0xFFFF); } // [  Wx   ]
        public  static _RWxMem    ind   (final _RWx reg, final int  ofs) { return new _RWxMem(0b001, reg, Reg.NA, ofs   ); } // [  Wx+N ]
        public  static _RWxMem    ind_mm(final _RWx reg                ) { return new _RWxMem(0b010, reg, Reg.NA, 0     ); } // [  Wx-- ]
        public  static _RWxMem    ind_pp(final _RWx reg                ) { return new _RWxMem(0b011, reg, Reg.NA, 0     ); } // [  Wx++ ]
        public  static _RWxMem mm_ind   (final _RWx reg                ) { return new _RWxMem(0b100, reg, Reg.NA, 0     ); } // [--Wx   ]
        public  static _RWxMem pp_ind   (final _RWx reg                ) { return new _RWxMem(0b101, reg, Reg.NA, 0     ); } // [++Wx   ]
        public  static _RWxMem    ind   (final _RWx reg, final _RWx ofs) { return new _RWxMem(0b110, reg, ofs   , 0     ); } // [  Wx+Wo]

    } // class

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class _RA { // Resolve address

        public final String  mString;

        public final String  label;     // The target label
        public final int     location;  // The target location
        public final int     bitSize;
        public final boolean isSigned;  // If 'true' the resolved value will a signed value, otherwise it will be an unsigned value
        public final boolean isAbs;     // If 'true' the resolved value will an absolute address, otherwise it will be a relative offset
        public final boolean isOrgOfs;  // If 'true' the resolved value will be offseted by 'originAddress' by 'link()'
        public final int     valSF;     // Value shift factor for the resolved value
        public final boolean valSFSub2; // If 'true' the value will be subtracted by 2 before the shift

        public final int     msb8Pos;   // Location of the 4th byte of the instruction (optional)
        public final int     msb8PSF;   // Placement shift factor for the above value  (optional)

        public final int     lswNPos;   // Location of the 1st byte of the instruction
        public final int     lswPNSF1;  // Placement shift factor for the above value
        public final int     lswPNSF0;  // ---

        public _RA(final String mString_, final String label_, final int location_, final int bitSize_, final int valSF_, final int msb8Pos_, final int msb8PSF_, final int lswNPos_, final int lswPNSF1_, final int lswPNSF0_)
        {
            final int bz = Math.abs(bitSize_);

            mString   = mString_;

            label     = label_;
            location  = location_;
            bitSize   = (bz & 0xFF      );
            isSigned  = (bitSize_ < 0   );
            isAbs     = (bz & 0x01000000) != 0;
            isOrgOfs  = (bz & 0x02000000) != 0;
            valSF     = Math.abs(valSF_);
            valSFSub2 = (valSF_ < 0);

            msb8Pos   = msb8Pos_;
            msb8PSF   = msb8PSF_;

            lswNPos   = lswNPos_;
            lswPNSF1  = lswPNSF1_;
            lswPNSF0  = lswPNSF0_;
        }

        public _RA(final String mString_, final String label_, final int bitSize_, final int valSF_, final int msb8Pos_, final int msb8PSF_, final int lswNPos_, final int lswPNSF1_, final int lswPNSF0_)
        { this(mString_, label_, -1, bitSize_, valSF_, msb8Pos_, msb8PSF_, lswNPos_, lswPNSF1_, lswPNSF0_); }

        public _RA(final String mString_, final String label_, final int bitSize_, final int valSF_, final int lswNPos_, final int lswPNSF1_, final int lswPNSF0_)
        { this(mString_, label_, bitSize_, valSF_, -1, -1, lswNPos_, lswPNSF1_, lswPNSF0_); }

        public _RA(final String mString_, final int location_, final int bitSize_, final int valSF_, final int msb8Pos_, final int msb8PSF_, final int lswNPos_, final int lswPNSF1_, final int lswPNSF0_)
        { this(mString_, null, location_, bitSize_, valSF_, msb8Pos_, msb8PSF_, lswNPos_, lswPNSF1_, lswPNSF0_); }

        public _RA(final String mString_, final int location_, final int bitSize_, final int valSF_, final int lswNPos_, final int lswPNSF1_, final int lswPNSF0_)
        { this(mString_, location_, bitSize_, valSF_, -1, -1, lswNPos_, lswPNSF1_, lswPNSF0_); }

        public static int sig(final int bitSize)
        { return -bitSize; }

        public static int abs(final int bitSize)
        { return 0x01000000 | bitSize; }

        public static int cbo(final int bitSize) // Implies 'abs()'
        { return 0x03000000 | bitSize; }

        public static int vs2(final int valSF)
        { return -valSF; }

    } // class _RA

    @SuppressWarnings("serial")
    private class OpCodeBuffer extends ArrayList<Integer> {

        public int getCurAddress()
        { return this.isEmpty() ? 0 : this.size(); }

    } // class OpCodeBuffer

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final CPU                           _cpu;

    private final OpCodeBuffer                  _opcode   = new OpCodeBuffer   ();
    private final MultiHashMap<String, Integer> _labels   = new MultiHashMap<> ();
    private final ArrayList   <_RA            > _resolve  = new ArrayList   <> ();

    private       boolean                       _piRepeat = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public PIC16BitEPC(final CPU cpu)
    { _cpu = cpu; }

    public PIC16BitEPC()
    { this(CPU.PIC24F); }

    public int[] link(final int originAddress, final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        try {
            // Link and clear
            final int[] res = _link_impl(originAddress, dumpDisassemblyAndArray);
            _clear();
            // Return the result
            return res;
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return null;
        }
    }

    public int[] link(final int originAddress) throws JXMAsmError
    { return link(originAddress, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public  static final String Label_Start = "_start";

    public PIC16BitEPC label(final String name) throws JXMAsmError
    {
        _MNEMONIC_STRING( "_label", name );

        if( _opcode.isEmpty() ) _errorInvalidTargetLabelLocation(_MSTR);

        if( name.length() > 1 || name.charAt(0) < '0' || name.charAt(0) > '9' ) {
            if( _labels.get(name) != null ) _errorDuplicatedTargetLabel(_MSTR);
        }

        _labels.put( name, _opcode.getCurAddress() );

        _MSTR = null;

        return this;
    }

    public PIC16BitEPC label_Start() throws JXMAsmError
    { return label(Label_Start); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _errorIfPrevIsRepeat(final String mString) throws JXMAsmError
    { if(_piRepeat) throw XCom.newJXMAsmError(Texts.ASM_InvalidInstSeq, ClassName, mString, "$repeat(*)"); }

    private void _errorInvalidInstructionForm(final String mString, final String ins) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidInstructionF, ClassName, mString, ins, _cpu.name() ); }

    private void _errorInvalid_ImmediateValue(final String mString, final int value) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidImmValue, ClassName, mString, value, _cpu.name() ); }

    private static void _errorInvalidAlignmentFactor(final String mString, final int factor) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidAlignFactor, ClassName, mString, factor); }

    private static void _errorInvalidBitShiftFactor(final String mString, final int factor) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidBShiftFactor, ClassName, mString, factor); }

    private static void _errorAddressOffsetNotAligned2B(final String mString, final int offset) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_AddrOfstNotAlign2B, ClassName, mString, offset & 0xFFFFFFFFL, offset); }

    private static void _errorIfAddressOffsetNotAligned2B(final String mString, final int offset) throws JXMAsmError
    { if( (offset & 0x00000001) != 0 ) _errorAddressOffsetNotAligned2B(mString, offset); }

    private static void _errorAddressOffsetNotAligned4B(final String mString, final int offset) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_AddrOfstNotAlign4B, ClassName, mString, offset & 0xFFFFFFFFL, offset); }

    private static void _errorIfAddressOffsetNotAligned4B(final String mString, final int offset) throws JXMAsmError
    { if( (offset & 0x00000003) != 0 ) _errorAddressOffsetNotAligned4B(mString, offset); }

    private static void _errorAddressOffsetValueOutOfRange(final String mString, final int offset) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_AddrOfstVOutOfRange, ClassName, mString, offset & 0xFFFFFFFFL, offset); }

    private static void _errorInvalidRegisterNAI(final String mString, final _RWx reg) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidRegNAI, ClassName, mString, reg._name() ); }

    private static void _errorEmptyTargetLabel(final String mString) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_EmptyTargetLabel, ClassName, mString); }

    private static void _errorDuplicatedTargetLabel(final String mString) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_DuplicateTargetLabel, ClassName, mString); }

    private static void _errorInvalidTargetLabelLocation(final String mString) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidTargetLabelLc, ClassName, mString); }

    private static void _errorInvalidTargetLabel(final String mString, final String label) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidTargetLabel, ClassName, mString, label); }

    private static void _errorInvalidORGValue(final String mString, final int orgVal) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidORGValue, ClassName, mString, orgVal); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final StringBuilder _sbArrayToString = new StringBuilder();

    private static class AoS {
        public final String string;
        public AoS(final String string_) { string = string_; }
    };

    private static <T> AoS _arrayToString(final String format, final T[] values)
    {
        final boolean isString = values[0] instanceof String;

        _sbArrayToString.setLength(0);

        for(final T value : values) {
            if(isString) _sbArrayToString.append( '"'                          );
                         _sbArrayToString.append( String.format(format, value) );
            if(isString) _sbArrayToString.append( '"'                          );
                         _sbArrayToString.append( ','                          );
        }

        _sbArrayToString.deleteCharAt( _sbArrayToString.length() - 1 );

        return new AoS( _sbArrayToString.toString() );
    }

    private static AoS _arrayToString(final String format, final int[] values)
    { return _arrayToString( format, Arrays.stream(values).boxed().toArray(Integer[]::new) ); }

    private static AoS _a2s06X(final int[] values)
    { return _arrayToString("0x%06X", values); }

    private static AoS _a2s06X(final int value)
    { return _a2s06X( new int[] { value} ); }

    private static AoS _a2sS(final String[] values)
    { return _arrayToString("%s", values); }

    private static AoS _a2sS(final _RWx[] values)
    { return _arrayToString("%s", values); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final StringBuilder _sbMnemonicString = new StringBuilder();
    private       String        _MSTR             = null;

    @SuppressWarnings("rawtypes")
    private void _MNEMONIC_STRING(final Object... els_)
    {
        // Simply return if the string already exists
        if(_MSTR != null) return;

        // Clear the buffer
        _sbMnemonicString.setLength(0);

        // Get and adjust the arguments as needed
        Object[] els = els_;

        if( els_.length == 2 && els_[1].getClass().isArray() ) {
            final Object[] args = (Object[]) els_[1];
            els = new Object[1 + args.length];
            els[0] = els_[0];
            for(int i = 0; i < args.length; ++i) els[i + 1] = args[i];
        }

        // Build the string
        for(int i = 0; i < els.length; ++i) {

            final Object e = els[i];

            if(e instanceof AoS) {
                _sbMnemonicString.append( ( (AoS) e ).string );
            }
            else if(e instanceof String) {
                 if(i != 0) _sbMnemonicString.append('"');
                            _sbMnemonicString.append( e );
                 if(i != 0) _sbMnemonicString.append('"');
            }
            else if(e instanceof _RAcc) {
                final _RAcc _e = (_RAcc) e;
                _sbMnemonicString.append( (char) ('A' + _e.regNum) );
            }
            else if(e instanceof _RTP) {
                final _RTP _e = (_RTP) e;
                _sbMnemonicString.append( _e._name() );
            }
            else if(e instanceof _WRg) {
                _sbMnemonicString.append( "WREG" );
            }
            else if(e instanceof _RWx) {
                final _RWx _e = (_RWx) e;
                _sbMnemonicString.append( _e._name() );
            }
            else if(e instanceof _Lit) {
                final _Lit _e = (_Lit) e;
                _sbMnemonicString.append( "#"      );
                _sbMnemonicString.append( _e.value );
            }
            else if(e instanceof _RWxMem) {
                final _RWxMem _e = (_RWxMem) e;
                _sbMnemonicString.append( _e.toString() );
            }
            else if(e instanceof Enum) {
                final Enum _e = (Enum) e;
                _sbMnemonicString.append( _e.getClass().getSimpleName() );
                _sbMnemonicString.append( '.'                           );
                _sbMnemonicString.append( _e.name()                     );
            }
            else if(e != null) {
                _sbMnemonicString.append( e.toString() );
            }
            _sbMnemonicString.append( (i == 0) ? "(" : ( (e != null) ? "," : "" ) );

        } // for

        _sbMnemonicString.deleteCharAt( _sbMnemonicString.length() - 1 );
        if(els.length > 1) _sbMnemonicString.append(')');

        // Store the final string
        _MSTR = _sbMnemonicString.toString();
    }

    private String _obw(final boolean b)
    { return b ? "_b" : "_w"; }

    private String _ozc(final boolean z)
    { return z ? "_z" : "_c"; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _checkAOffset(final String mString, final int offset, final int bitShiftFactor) throws JXMAsmError
    {
             if(bitShiftFactor == 0) {                                                                                             }
        else if(bitShiftFactor == 1) { if( (offset & 0x00000001) != 0 ) _errorAddressOffsetNotAligned2B (mString, offset        ); }
        else if(bitShiftFactor == 2) { if( (offset & 0x00000003) != 0 ) _errorAddressOffsetNotAligned4B (mString, offset        ); }
        else                         {                                  _errorInvalidBitShiftFactor     (mString, bitShiftFactor); }
    }

    private static int _checkUOffset(final String mString, final int offset, final int bitShiftFactor, final int maxNumBits) throws JXMAsmError
    {
        _checkAOffset(mString, offset, bitShiftFactor);

        final int maxVal = ( 1 << (maxNumBits + bitShiftFactor) ) - (1 << bitShiftFactor);

        if(offset < 0     ) _errorAddressOffsetValueOutOfRange(mString, offset);
        if(offset > maxVal) _errorAddressOffsetValueOutOfRange(mString, offset);

        return (offset >>> bitShiftFactor) & ( ( 1 << (maxNumBits + bitShiftFactor) ) - 1 );
    }

    private static int _checkSOffset(final String mString, final int offset, final int bitShiftFactor, final int maxNumBits) throws JXMAsmError
    {
        _checkAOffset(mString, offset, bitShiftFactor);

        final int minVal = -( 1 << (maxNumBits + bitShiftFactor - 1) )                        ;
        final int maxVal =  ( 1 << (maxNumBits + bitShiftFactor - 1) ) - (1 << bitShiftFactor);

        if(offset < minVal || offset > maxVal) _errorAddressOffsetValueOutOfRange(mString, offset);

        return (offset >> bitShiftFactor) & ( ( 1 << (maxNumBits + bitShiftFactor) ) - 1 );
    }

    private static String _checkTargetLabel(final String mString, final String label_) throws JXMAsmError
    {
        final String label = (label_ == null) ? "" : label_.trim();

        if( label.isEmpty() )_errorEmptyTargetLabel(mString);

        return label;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _putOpCode24(final int opcodeValue)
    {
        _opcode.add( (opcodeValue & 0x0000FF) >>  0 );
        _opcode.add( (opcodeValue & 0x00FF00) >>  8 );
        _opcode.add( (opcodeValue & 0xFF0000) >> 16 );

        _MSTR     = null;
        _piRepeat = false;
    }

    private void _putOpCode24(final String mString, final int opcodeValue, final String paramLabel, final int bitSize)
    {
        _resolve.add( new _RA(mString, paramLabel, bitSize, 0, _opcode.getCurAddress(), 0, 0) );
        _putOpCode24(opcodeValue);
    }

    private void _putOpCode24Ex(final String mString, final int opcodeValue, final String paramLabel, final int bitSize)
    {
        _resolve.add( new _RA(mString, paramLabel, bitSize, 0, _opcode.getCurAddress() + 2, 0, _opcode.getCurAddress(), 0, 0 ) );
        _putOpCode24(opcodeValue);
    }

    private void _putOpCode24(final String mString, final int opcodeValue, final String paramLabel, final int bitSize, final int valSF)
    {
        _resolve.add( new _RA(mString, paramLabel, bitSize, valSF, _opcode.getCurAddress(), 0, 0) );
        _putOpCode24(opcodeValue);
    }

    private void _putOpCode24(final String mString, final int opcodeValue, final int paramLocation, final int bitSize)
    {
        _resolve.add( new _RA(mString, paramLocation, bitSize, 0, _opcode.getCurAddress(), 0, 0) );
        _putOpCode24(opcodeValue);
    }

    private void _putOpCode24(final String mString, final int opcodeValue, final int paramLocation, final int bitSize, final int valSF)
    {
        _resolve.add( new _RA(mString, paramLocation, bitSize, valSF, _opcode.getCurAddress(), 0, 0) );
        _putOpCode24(opcodeValue);
    }

    private void _putOpCode24(final String mString, final int opcodeValue, final String paramLabel, final int bitSize, final int valSF, final int lswPNSF0)
    {
        _resolve.add( new _RA(mString, paramLabel, bitSize, valSF, _opcode.getCurAddress(), 0, lswPNSF0) );
        _putOpCode24(opcodeValue);
    }

    private void _putOpCode48(final int opcodeValue)
    {
        _putOpCode24(opcodeValue);
        _putOpCode24(0          );
    }

    private void _putOpCode48(final int opcodeValue, final int uaddr)
    {
        _putOpCode24(opcodeValue);
        _putOpCode24(uaddr      );
    }

    private void _putOpCode48(final String mString, final int opcodeValue, final String paramLabel, final int bitSize)
    {
        _resolve.add( new _RA(mString, paramLabel, bitSize, 0, _opcode.getCurAddress() + 3, 0, _opcode.getCurAddress(), 0, 0 ) );
        _putOpCode48(opcodeValue);
    }

    private void _putOpCode48(final String mString, final int opcodeValue, final String paramLabel, final int bitSize, final int valSF)
    {
        _resolve.add( new _RA(mString, paramLabel, bitSize, valSF, _opcode.getCurAddress() + 3, 0, _opcode.getCurAddress(), 0, 0 ) );
        _putOpCode48(opcodeValue);
    }

    private void _putOpCode48(final String mString, final int opcodeValue, final int paramLocation, final int bitSize)
    {
        _resolve.add( new _RA(mString, paramLocation, bitSize, 0, _opcode.getCurAddress() + 3, 0, _opcode.getCurAddress(), 0, 0 ) );
        _putOpCode48(opcodeValue);
    }

    private void _putOpCode48(final String mString, final int opcodeValue, final int paramLocation, final int bitSize, final int valSF)
    {
        _resolve.add( new _RA(mString, paramLocation, bitSize, valSF, _opcode.getCurAddress() + 3, 0, _opcode.getCurAddress(), 0, 0 ) );
        _putOpCode48(opcodeValue);
    }

    private void _putOpCode48NoLSW(final String mString, final int opcodeValue, final String paramLabel, final int bitSize)
    {
        _resolve.add( new _RA(mString, paramLabel, bitSize, 0, _opcode.getCurAddress() + 3, 0, 0 ) );
        _putOpCode48(opcodeValue);
    }

    private void _putOpCode48NoLSW(final String mString, final int opcodeValue, final String paramLabel, final int bitSize, final int valSF)
    {
        _resolve.add( new _RA(mString, paramLabel, bitSize, valSF, _opcode.getCurAddress() + 3, 0, 0 ) );
        _putOpCode48(opcodeValue);
    }

    private void _putOpCode48NoLSW(final String mString, final int opcodeValue, final int paramLocation, final int bitSize)
    {
        _resolve.add( new _RA(mString, paramLocation, bitSize, 0, _opcode.getCurAddress() + 3, 0, 0) );
        _putOpCode48(opcodeValue);
    }

    private void _putOpCode48NoLSW(final String mString, final int opcodeValue, final int paramLocation, final int bitSize, final int valSF)
    {
        _resolve.add( new _RA(mString, paramLocation, bitSize, valSF, _opcode.getCurAddress() + 3, 0, 0) );
        _putOpCode48(opcodeValue);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // .org
    public PIC16BitEPC $_org(final int orgValue, final boolean useNOPR) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_org", _a2s06X(orgValue) );

        final int cnt = orgValue - ( _opcode.getCurAddress() * 2 / 3 );
        if(cnt < 0) _errorInvalidORGValue(_MSTR, orgValue);

        for(int i = 0; i < cnt / 2; ++i) {
            if(useNOPR) $nopr();
            else        $nop ();
        }

        return this;
    }

    public PIC16BitEPC $_org(final int orgValue) throws JXMAsmError
    { return $_org(orgValue, false); }

    // .align
    public PIC16BitEPC $_align(final int factor, final boolean useNOPR) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_align", factor );

        _errorIfAddressOffsetNotAligned2B(_MSTR, factor);

        if(factor <= 2) return this;

        final int factor1 = factor - 1;
        final long na     = _opcode.getCurAddress() * 2 / 3;
              long pad    = ( (na + factor1) & ~factor1 ) - na;

        /*
        SysUtil.stdDbg().printf("%d=>%d : na=%X (%d) ; pad=%d\n", factor, factor1, na, na, pad);
        //*/

        while(pad != 0) {
            if(useNOPR) $nopr();
            else        $nop ();
            pad -= 2;
        }

        return this;
    }

    public PIC16BitEPC $_align(final int factor) throws JXMAsmError
    { return $_align(factor, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // .word
    public PIC16BitEPC $_word(final String... labels) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_word", _a2sS(labels) );

        for(final String label_ : labels) {
            final String label = _checkTargetLabel(_MSTR, label_);
            _putOpCode24( _MSTR, 0, label, _RA.abs(16) );
        }

        _MSTR = null;

        return this;
    }

    // .word
    public PIC16BitEPC $_word(final int... values_) throws JXMAsmError
    {
        final int[] values = new int[values_.length];
        for(int i = 0; i < values_.length; ++i) values[i] = values_[i] & 0x00FFFF;

        _MNEMONIC_STRING( "$_word", _a2s06X(values) );

        for(final int value : values) _putOpCode24(value);

        _MSTR = null;

        return this;
    }

    // .pword
    public PIC16BitEPC $_pword(final String... labels) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_pword", _a2sS(labels) );

        for(final String label_ : labels) {
            final String label = _checkTargetLabel(_MSTR, label_);
            _putOpCode24Ex( _MSTR, 0, label, _RA.abs(24) );
        }

        _MSTR = null;

        return this;
    }

    // .pword
    public PIC16BitEPC $_pword(final int... values) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_pword", _a2s06X(values) );

        for(final int value : values) _putOpCode24(value);

        _MSTR = null;

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOP
    public PIC16BitEPC $nop() throws JXMAsmError
    {
        _putOpCode24( 0x00000000 );
        return this;
    }

    // NOPR
    public PIC16BitEPC $nopr() throws JXMAsmError
    {
        _putOpCode24( 0xFFFFFFFF );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // DO #<ulit14/ulit15>, <ulit16>
    public PIC16BitEPC $do(final _Lit cnt14_cnt15, final int absAddress16) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$do", cnt14_cnt15, absAddress16 );

        final int ln = _cpu.chkSupInst( _MSTR, "$do" );

        _errorIfPrevIsRepeat(_MSTR);
        _errorIfAddressOffsetNotAligned2B(_MSTR, absAddress16);

        _checkUOffset(_MSTR, cnt14_cnt15.value, 0, ln);
        _checkUOffset(_MSTR, absAddress16     , 0, 16);
        _putOpCode48NoLSW( _MSTR, 0b000010000000000000000000 | cnt14_cnt15.value, absAddress16, _RA.sig(16), _RA.vs2(1) );
        return this;
    }

    // DO #<ulit14/ulit15>, <label>
    public PIC16BitEPC $do(final _Lit cnt14_cnt15, final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$do", cnt14_cnt15, label );

        final int ln = _cpu.chkSupInst( _MSTR, "$do" );

        _errorIfPrevIsRepeat(_MSTR);

        _checkUOffset(_MSTR, cnt14_cnt15.value, 0, ln);
        _putOpCode48NoLSW( _MSTR, 0b000010000000000000000000 | cnt14_cnt15.value, label, _RA.sig(16), _RA.vs2(1) );
        return this;
    }

    // DO <Wn>, <ulit16>
    public PIC16BitEPC $do(final _RWx Wn, final int absAddress16) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$do", Wn, absAddress16 );

        _cpu.chkSupInst( _MSTR, "$do" );

        _errorIfPrevIsRepeat(_MSTR);
        _errorIfAddressOffsetNotAligned2B(_MSTR, absAddress16);

        _checkUOffset(_MSTR, absAddress16, 0, 16);
        _putOpCode48NoLSW( _MSTR, 0b000010001000000000000000 | Wn.regNum, absAddress16, _RA.sig(16), _RA.vs2(1) );
        return this;
    }

    // DO <Wn>, <label>
    public PIC16BitEPC $do(final _RWx Wn, final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$do", Wn, label );

        _cpu.chkSupInst( _MSTR, "$do" );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode48NoLSW( _MSTR, 0b000010001000000000000000 | Wn.regNum, label, _RA.sig(16), _RA.vs2(1) );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // REPEAT <Wn>
    public PIC16BitEPC $repeat(final _RWx Wn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$repeat", Wn );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24( 0b000010011000000000000000 | Wn.regNum) ;
        _piRepeat = true;
        return this;
    }

    // REPEAT #<ulit14/ulit15>
    public PIC16BitEPC $repeat(final _Lit cnt14_cnt15) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$repeat", cnt14_cnt15 );

        final int ln = _cpu.chkSupInst( _MSTR, "$repeat" );

        _errorIfPrevIsRepeat(_MSTR);

        _checkUOffset(_MSTR, cnt14_cnt15.value, 0, ln);
        _putOpCode24( 0b000010010000000000000000 | cnt14_cnt15.value );
        _piRepeat = true;
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // GOTO <Wa>
    public PIC16BitEPC $goto(final _RWx Wa) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$goto", Wa );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24(
            // NOTE : The opcode differs in PIC24E, dsPIC33E, and dsPIC33C MCUs
                                           // 0b00000001000001000000ssss   0b00000001010000000000ssss
            ( _cpu.extendedInstructionSet() ? 0b000000010000010000000000 : 0b000000010100000000000000 ) | Wa.regNum
        );
        return this;
    }

    // GOTO.L <Wa>
    public PIC16BitEPC $goto_l(final _RWx Wa) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$goto_l", Wa );

        _cpu.chkSupExtInst( _MSTR, "$goto_l" );
        if( Wa.oddReg() ) _errorInvalidRegisterNAI(_MSTR, Wa);

                   // 0b000000011wwww1000000ssss
        _putOpCode24( 0b000000011000010000000000 | ( (Wa.regNum + 1) << 11 ) | Wa.regNum );
        return this;
    }

    // GOTO <ulit24>
    public PIC16BitEPC $goto(final int absAddress24) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$goto", absAddress24 );

        _errorIfPrevIsRepeat(_MSTR);
        _errorIfAddressOffsetNotAligned2B(_MSTR, absAddress24);

        _checkUOffset(_MSTR, absAddress24, 1, 23);
        _putOpCode48( 0b000001000000000000000000 | (absAddress24 & 0x00FFFF), (absAddress24 & 0xFF0000) >> 16 );
        return this;
    }

    // GOTO <label>
    public PIC16BitEPC $goto(final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$goto", label );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode48(_MSTR, 0b000001000000000000000000, label, _RA.cbo(24), 0 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // CALL <Wa>
    public PIC16BitEPC $call(final _RWx Wa) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$call", Wa );

        _errorIfPrevIsRepeat(_MSTR);

                   // 0b00000001000000000000ssss
        _putOpCode24( 0b000000010000000000000000 | Wa.regNum );
        return this;
    }

    // CALL.L <Wa>
    public PIC16BitEPC $call_l(final _RWx Wa) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$call_l", Wa );

        _cpu.chkSupExtInst( _MSTR, "$call_l" );
        if( Wa.oddReg() ) _errorInvalidRegisterNAI(_MSTR, Wa);

                   // 0b000000011wwww0000000ssss
        _putOpCode24( 0b000000011000000000000000 | ( (Wa.regNum + 1) << 11 ) | Wa.regNum );
        return this;
    }

    // CALL <ulit24>
    public PIC16BitEPC $call(final int absAddress24) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$call", absAddress24 );

        _errorIfPrevIsRepeat(_MSTR);
        _errorIfAddressOffsetNotAligned2B(_MSTR, absAddress24);

        _checkUOffset(_MSTR, absAddress24, 1, 23);
        _putOpCode48( 0b000000100000000000000000 | (absAddress24 & 0x00FFFF), (absAddress24 & 0xFF0000) >> 16 );
        return this;
    }

    // CALL <label>
    public PIC16BitEPC $call(final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$call", label );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode48(_MSTR, 0b000000100000000000000000, label, _RA.cbo(24), 0 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BRA <Wa>
    public PIC16BitEPC $bra(final _RWx Wa) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$bra", Wa );

        _errorIfPrevIsRepeat(_MSTR);
        _putOpCode24(
            // NOTE : The opcode differs in PIC24E, dsPIC33E, and dsPIC33C MCUs
                                           // 0b00000001000001100000ssss   0b00000001011000000000ssss
            ( _cpu.extendedInstructionSet() ? 0b000000010000011000000000 : 0b000000010110000000000000 ) | Wa.regNum
        );
        return this;
    }

    // BRA <ulit16>
    public PIC16BitEPC $bra(final int absAddress16) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$bra", absAddress16 );

        _errorIfPrevIsRepeat(_MSTR);
        _errorIfAddressOffsetNotAligned2B(_MSTR, absAddress16);

         _checkUOffset(_MSTR, absAddress16, 0, 16);
        _putOpCode24( _MSTR, 0b001101110000000000000000, absAddress16, _RA.sig(16), _RA.vs2(1) );
        return this;
    }

    // BRA <label>
    public PIC16BitEPC $bra(final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$bra", label );

        _putOpCode24(_MSTR, 0b001101110000000000000000, label, _RA.sig(16), _RA.vs2(1) );
        return this;
    }

    // BRA <Cond>, <label>
    public PIC16BitEPC $bra(final Cond cond, final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$bra", cond, label );

        _cpu.chkSupInst( _MSTR, "$bra", cond );

        if(cond.value >= Cond.__DSP_COND_START__.value) {
            _putOpCode24(_MSTR, 0b000000000000000000000000 | ( (cond.value - Cond.__DSP_COND_START__.value) << 16 ), label, _RA.sig(16), _RA.vs2(1) );
        }
        else {
            _putOpCode24(_MSTR, 0b001100000000000000000000 | (  cond.value                                  << 16 ), label, _RA.sig(16), _RA.vs2(1) );
        }
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // RCALL <Wa>
    public PIC16BitEPC $rcall(final _RWx Wa) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rcall", Wa );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24(
            // NOTE : The opcode differs in PIC24E, dsPIC33E, and dsPIC33C MCUs
                                           // 0b00000001000000100000ssss   0b00000001001000000000ssss
            ( _cpu.extendedInstructionSet() ? 0b000000010000001000000000 : 0b000000010010000000000000 ) | Wa.regNum
        );
        return this;
    }

    // RCALL <ulit16>
    public PIC16BitEPC $rcall(final int absAddress16) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rcall", absAddress16 );

        _errorIfPrevIsRepeat(_MSTR);
        _errorIfAddressOffsetNotAligned2B(_MSTR, absAddress16);

         _checkUOffset(_MSTR, absAddress16, 0, 16);
        _putOpCode24( _MSTR, 0b000001110000000000000000, absAddress16, _RA.sig(16), _RA.vs2(1) );
        return this;
    }

    // RCALL <label>
    public PIC16BitEPC $rcall(final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rcall", label );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24(_MSTR, 0b000001110000000000000000, label, _RA.sig(16), _RA.vs2(1) );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // RETLW[.B] #<ulit10>, <Wn>
    private PIC16BitEPC _$retlw_impl(final boolean b, final _Lit value10, final _RWx Wn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$retlw" + _obw(b), value10, Wn );

        _errorIfPrevIsRepeat(_MSTR);

         _checkUOffset(_MSTR, value10.value, 0, 10);
        _putOpCode24( 0b000001010000000000000000 | (b ? 0b000000000100000000000000 : 0) | (value10.value << 4) | Wn.regNum );
        return this;
    }

    public PIC16BitEPC $retlw(final _Lit value10, final _RWx Wn) throws JXMAsmError
    { return _$retlw_impl(false, value10, Wn); }

    public PIC16BitEPC $retlw_b(final _Lit value10, final _RWx Wn) throws JXMAsmError
    { return _$retlw_impl(true , value10, Wn); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // RETURN
    public PIC16BitEPC $return() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$return" );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24( 0b000001100000000000000000 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // RETFIE
    public PIC16BitEPC $retfie() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$retfie" );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24( 0b000001100100000000000000 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // LAC    <Ws>    {, #<slit4>}, <Acc>
    // LAC [  <Ws>   ]{, #<slit4>}, <Acc>
    // LAC [  <Ws>++ ]{, #<slit4>}, <Acc>
    // LAC [  <Ws>-- ]{, #<slit4>}, <Acc>
    // LAC [++<Ws>   ]{, #<slit4>}, <Acc>
    // LAC [--<Ws>   ]{, #<slit4>}, <Acc>
    // LAC [<Ws>+<Wb>]{, #<slit4>}, <Acc>
    public PIC16BitEPC $lac(final _RWxMem Ws, final _Lit shift4, final _RAcc RAcc) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lac", Ws, shift4, RAcc );

        _cpu.chkSupInstGroup_stdDSP( _MSTR, "$lac" );
        if( Ws.immOfs() ) _errorInvalidInstructionForm( _MSTR, "$lac" );

        final int imm = _checkSOffset(_MSTR, shift4.value, 0, 4);
        _putOpCode24(
         // 0b11001010Awwwwrrrrgggssss
            0b110010100000000000000000 |
            (RAcc.regNum  << 15      ) |
            (Ws.Wb.regNum << 11      ) |
            (imm          <<  7      ) |
            (Ws.mode      <<  4      ) |
            (Ws.Wx.regNum            )
        );
        return this;
    }

    public PIC16BitEPC $lac(final _RWx Ws, final _Lit shift4, final _RAcc RAcc) throws JXMAsmError
    { return $lac( Expr._reg(Ws), shift4, RAcc ); }

    public PIC16BitEPC $lac(final _RWxMem Ws, final _RAcc RAcc) throws JXMAsmError
    { return $lac( Ws, Expr._lit0(), RAcc ); }

    public PIC16BitEPC $lac(final _RWx Ws, final _RAcc RAcc) throws JXMAsmError
    { return $lac( Expr._reg(Ws), Expr._lit0(), RAcc ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // LAC.D    <Ws>    {, #<slit4>}, <Acc>
    // LAC.D [  <Ws>   ]{, #<slit4>}, <Acc>
    // LAC.D [  <Ws>++ ]{, #<slit4>}, <Acc>
    // LAC.D [  <Ws>-- ]{, #<slit4>}, <Acc>
    // LAC.D [++<Ws>   ]{, #<slit4>}, <Acc>
    // LAC.D [--<Ws>   ]{, #<slit4>}, <Acc>
    // LAC.D [<Ws>+<Wb>]{, #<slit4>}, <Acc>
    public PIC16BitEPC $lac_d(final _RWxMem Ws, final _Lit shift4, final _RAcc RAcc) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lac_d", Ws, shift4, RAcc );

        _cpu.chkSupInst( _MSTR, "$lac_d" );
        if( Ws.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$lac_d" );
        if( Ws.direct_oddReg() ) _errorInvalidRegisterNAI(_MSTR, Ws.Wx);

        final int imm = _checkSOffset(_MSTR, shift4.value, 0, 4);
        _putOpCode24(
         // 0b11011011A0000rrrrpppssss
            0b110110110000000000000000 |
            (RAcc.regNum  << 15 )      |
            (imm          <<  7 )      |
            (Ws.mode      <<  4 )      |
            (Ws.Wx.regNum       )
        );
        return this;
    }

    public PIC16BitEPC $lac_d(final _RWx Ws, final _Lit shift4, final _RAcc RAcc) throws JXMAsmError
    { return $lac_d( Expr._reg(Ws), shift4, RAcc ); }

    public PIC16BitEPC $lac_d(final _RWxMem Ws, final _RAcc RAcc) throws JXMAsmError
    { return $lac_d( Ws, Expr._lit0(), RAcc ); }

    public PIC16BitEPC $lac_d(final _RWx Ws, final _RAcc RAcc) throws JXMAsmError
    { return $lac_d( Expr._reg(Ws), Expr._lit0(), RAcc ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SAC <Acc>{, #<slit4>},    <Ws>
    // SAC <Acc>{, #<slit4>}, [  <Ws>++ ]
    // SAC <Acc>{, #<slit4>}, [  <Ws>   ]
    // SAC <Acc>{, #<slit4>}, [  <Ws>-- ]
    // SAC <Acc>{, #<slit4>}, [++<Ws>   ]
    // SAC <Acc>{, #<slit4>}, [--<Ws>   ]
    // SAC <Acc>{, #<slit4>}, [<Ws>+<Wb>]
    public PIC16BitEPC $sac(final _RAcc RAcc, final _Lit shift4, final _RWxMem Ws) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sac", RAcc, shift4, Ws );

        _cpu.chkSupInstGroup_stdDSP( _MSTR, "$sac" );
        if( Ws.immOfs() ) _errorInvalidInstructionForm( _MSTR, "$sac" );

        final int imm = _checkSOffset(_MSTR, shift4.value, 0, 4);
        _putOpCode24(
         // 0b11001100Awwwwrrrrgggssss
            0b110011000000000000000000 |
            (RAcc.regNum  << 15      ) |
            (Ws.Wb.regNum << 11      ) |
            (imm          <<  7      ) |
            (Ws.mode      <<  4      ) |
            (Ws.Wx.regNum            )
        );
        return this;
    }

    public PIC16BitEPC $sac(final _RAcc RAcc, final _Lit shift4, final _RWx Ws) throws JXMAsmError
    { return $sac( RAcc, shift4, Expr._reg(Ws) ); }

    public PIC16BitEPC $sac(final _RAcc RAcc, final _RWxMem Ws) throws JXMAsmError
    { return $sac( RAcc, Expr._lit0(), Ws ); }

    public PIC16BitEPC $sac(final _RAcc RAcc, final _RWx Ws) throws JXMAsmError
    { return $sac( RAcc, Expr._lit0(), Expr._reg(Ws) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SAC.D <Acc>{, #<slit4>},    <Ws>
    // SAC.D <Acc>{, #<slit4>}, [  <Ws>++ ]
    // SAC.D <Acc>{, #<slit4>}, [  <Ws>   ]
    // SAC.D <Acc>{, #<slit4>}, [  <Ws>-- ]
    // SAC.D <Acc>{, #<slit4>}, [++<Ws>   ]
    // SAC.D <Acc>{, #<slit4>}, [--<Ws>   ]
    // SAC.D <Acc>{, #<slit4>}, [<Ws>+<Wb>]
    public PIC16BitEPC $sac_d(final _RAcc RAcc, final _Lit shift4, final _RWxMem Ws) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sac_d", RAcc, shift4, Ws );

        _cpu.chkSupInst( _MSTR, "$sac_d" );
        if( Ws.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$sac_d" );
        if( Ws.direct_oddReg() ) _errorInvalidRegisterNAI(_MSTR, Ws.Wx);

        final int imm = _checkSOffset(_MSTR, shift4.value, 0, 4);
        _putOpCode24(
         // 0b11011100A0qqqrrrr000dddd
            0b110111000000000000000000 |
            (RAcc.regNum  << 15      ) |
            (imm          <<  7      ) |
            (Ws.mode      << 11      ) |
            (Ws.Wx.regNum            )
        );
        return this;
    }

    public PIC16BitEPC $sac_d(final _RAcc RAcc, final _Lit shift4, final _RWx Ws) throws JXMAsmError
    { return $sac_d( RAcc, shift4, Expr._reg(Ws) ); }

    public PIC16BitEPC $sac_d(final _RAcc RAcc, final _RWxMem Ws) throws JXMAsmError
    { return $sac_d( RAcc, Expr._lit0(), Ws ); }

    public PIC16BitEPC $sac_d(final _RAcc RAcc, final _RWx Ws) throws JXMAsmError
    { return $sac_d( RAcc, Expr._lit0(), Expr._reg(Ws) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SAC.R <Acc>{, #<slit4>},    <Ws>
    // SAC.R <Acc>{, #<slit4>}, [  <Ws>++ ]
    // SAC.R <Acc>{, #<slit4>}, [  <Ws>   ]
    // SAC.R <Acc>{, #<slit4>}, [  <Ws>-- ]
    // SAC.R <Acc>{, #<slit4>}, [++<Ws>   ]
    // SAC.R <Acc>{, #<slit4>}, [--<Ws>   ]
    // SAC.R <Acc>{, #<slit4>}, [<Ws>+<Wb>]
    public PIC16BitEPC $sac_r(final _RAcc RAcc, final _Lit shift4, final _RWxMem Ws) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sac_r", RAcc, shift4, Ws );

        _cpu.chkSupInstGroup_stdDSP( _MSTR, "$sac_r" );

        final int imm = _checkSOffset(_MSTR, shift4.value, 0, 4);
        _putOpCode24(
         // 0b11001101Awwwwrrrrgggssss
            0b110011010000000000000000 |
            (RAcc.regNum  << 15      ) |
            (Ws.Wb.regNum << 11      ) |
            (imm          <<  7      ) |
            (Ws.mode      <<  4      ) |
            (Ws.Wx.regNum            )
        );
        return this;
    }

    public PIC16BitEPC $sac_r(final _RAcc RAcc, final _Lit shift4, final _RWx Ws) throws JXMAsmError
    { return $sac_r( RAcc, shift4, Expr._reg(Ws) ); }

    public PIC16BitEPC $sac_r(final _RAcc RAcc, final _RWxMem Ws) throws JXMAsmError
    { return $sac_r( RAcc, Expr._lit0(), Ws ); }

    public PIC16BitEPC $sac_r(final _RAcc RAcc, final _RWx Ws) throws JXMAsmError
    { return $sac_r( RAcc, Expr._lit0(), Expr._reg(Ws) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // EXCH <Ws>, <Wd>
    public PIC16BitEPC $exch(final _RWx Ws, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$exch", Ws, Wd );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24( 0b111111010000000000000000 | (Wd.regNum << 7) | Ws.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // MOV[.B] <f>
    // MOV[.B] <f>, WREG
    private PIC16BitEPC $_mov_f_wreg_impl(final boolean b, final int f, final _WRg WReg) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov" + _obw(b), f, WReg );

        if(!b) _errorIfAddressOffsetNotAligned2B(_MSTR, f);

         _checkUOffset(_MSTR, f, 0, 13);
        _putOpCode24(
                               0b101111111000000000000000       |
            (  b             ? 0b000000000100000000000000 : 0 ) |
            ( (WReg == null) ? 0b000000000010000000000000 : 0 ) |
            ( f                                               )
        );
        return this;
    }

    public PIC16BitEPC $mov  (final int f                 ) throws JXMAsmError { return $_mov_f_wreg_impl(false, f, null); }
    public PIC16BitEPC $mov_b(final int f                 ) throws JXMAsmError { return $_mov_f_wreg_impl(true , f, null); }
    public PIC16BitEPC $mov  (final int f, final _WRg WReg) throws JXMAsmError { return $_mov_f_wreg_impl(false, f, WReg); }
    public PIC16BitEPC $mov_b(final int f, final _WRg WReg) throws JXMAsmError { return $_mov_f_wreg_impl(true , f, WReg); }

    // MOV[.B] WREG, <f>
    private PIC16BitEPC $_mov_wreg_f_impl(final boolean b, final _WRg WReg, final int f) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov" + _obw(b), WReg, f );

        if(!b) _errorIfAddressOffsetNotAligned2B(_MSTR, f);

         _checkUOffset(_MSTR, f, 0, 13);
        _putOpCode24(
                 0b101101111010000000000000      |
            (b ? 0b000000000100000000000000 : 0) |
            ( f                                )
        );
        return this;
    }

    public PIC16BitEPC $mov  (final _WRg WReg, final int f) throws JXMAsmError { return $_mov_wreg_f_impl(false, WReg, f); }
    public PIC16BitEPC $mov_b(final _WRg WReg, final int f) throws JXMAsmError { return $_mov_wreg_f_impl(true , WReg, f); }

    // MOV <f>, <Wd>
    public PIC16BitEPC $mov(final int f, final _RWx Wd) throws JXMAsmError
    {
        // ERROR: _MNEMONIC_STRING("$mov.w", ...) does not match the method name "$mov". Per the class convention (line ~30), the ".w" suffix is implicit and must not appear in instruction names. Fix: change "$mov.w" to "$mov".
        _MNEMONIC_STRING( "$mov.w", f, Wd );

        _errorIfAddressOffsetNotAligned2B(_MSTR, f);

         final int imm = _checkUOffset(_MSTR, f, 1, 15);
        _putOpCode24( 0b100000000000000000000000 | (imm << 4) | Wd.regNum );
        return this;
    }

    // MOV <Ws>, <f>
    public PIC16BitEPC $mov(final _RWx Ws, final int f) throws JXMAsmError
    {
        // ERROR: _MNEMONIC_STRING("$mov.w", ...) does not match the method name "$mov". Per the class convention (line ~30), the ".w" suffix is implicit and must not appear in instruction names. Fix: change "$mov.w" to "$mov".
        _MNEMONIC_STRING( "$mov.w", Ws, f );

        _errorIfAddressOffsetNotAligned2B(_MSTR, f);

         final int imm = _checkUOffset(_MSTR, f, 1, 15);
        _putOpCode24( 0b100010000000000000000000 | (imm << 4) | Ws.regNum );
        return this;
    }

    // MOV.B #<ulit8>, <Wd>
    public PIC16BitEPC $mov_b(final _Lit value8, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov_b", value8, Wd );

         final int imm = _checkUOffset(_MSTR, value8.value, 0, 8);
        _putOpCode24( 0b101100111100000000000000 | (imm << 4) | Wd.regNum );
        return this;
    }

    // MOV #<ulit16/slit16>, <Wd>
    public PIC16BitEPC $mov(final _Lit value16, final _RWx Wd) throws JXMAsmError
    {
        // ERROR: _MNEMONIC_STRING("$mov.w", ...) does not match the method name "$mov". Per the class convention (line ~30), the ".w" suffix is implicit and must not appear in instruction names. Fix: change "$mov.w" to "$mov".
        _MNEMONIC_STRING( "$mov.w", value16, Wd );

         final int imm = (value16.value >= 0)
                       ? _checkUOffset(_MSTR, value16.value, 0, 16)
                       : _checkSOffset(_MSTR, value16.value, 0, 16);
        _putOpCode24( 0b001000000000000000000000 | (imm << 4) | Wd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private PIC16BitEPC $_mov_ws_rwxmem_impl(final boolean b, final _RWx Ws, final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov" + _obw(b), Ws, Wd );

        if(  Wd.immOfs() ) {
            if(!b) _errorIfAddressOffsetNotAligned2B(_MSTR, Wd.N);
            final int imm = _checkSOffset(_MSTR, Wd.N, b ? 0 : 1, 10);
            final int k2  = (imm & 0b1111000000) >>> 6;
            final int k1  = (imm & 0b0000111000) >>> 3;
            final int k0  = (imm & 0b0000000111) >>> 0;
            _putOpCode24(
                  // 0b10011kkkkBkkkddddkkkssss
                     0b100110000000000000000000      |
                (          k2 << 15                ) |
                (b ? 0b000000000100000000000000 : 0) |
                (          k1 << 11                ) |
                (Wd.Wx.regNum <<  7                ) |
                (          k0 <<  4                ) |
                (   Ws.regNum                      )
            );
        }
        else {
            _putOpCode24(
                  // 0b01111wwwwBhhhddddgggssss
                     0b011110000000000000000000      |
                (b ? 0b000000000100000000000000 : 0) |
                (Wd.Wb.regNum << 15                ) |
                (Wd.mode      << 11                ) |
                (Wd.Wx.regNum <<  7                ) |
                (Ws.regNum                         )
            );
        }
        return this;
    }

    private PIC16BitEPC $_mov_rwxmem_wd_impl(final boolean b, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov" + _obw(b), Ws, Wd );

        if(  Ws.immOfs() ) {
            if(!b) _errorIfAddressOffsetNotAligned2B(_MSTR, Ws.N);
            final int imm = _checkSOffset(_MSTR, Ws.N, b ? 0 : 1, 10);
            final int k2  = (imm & 0b1111000000) >>> 6;
            final int k1  = (imm & 0b0000111000) >>> 3;
            final int k0  = (imm & 0b0000000111) >>> 0;
            _putOpCode24(
                  // 0b10010kkkkBkkkddddkkkssss
                     0b100100000000000000000000      |
                (          k2 << 15                ) |
                (b ? 0b000000000100000000000000 : 0) |
                (          k1 << 11                ) |
                (   Wd.regNum <<  7                ) |
                (          k0 <<  4                ) |
                (Ws.Wx.regNum                      )
            );
        }
        else {
            _putOpCode24(
                  // 0b01111wwwwBhhhddddgggssss
                     0b011110000000000000000000      |
                (b ? 0b000000000100000000000000 : 0) |
                (Ws.Wb.regNum << 15                ) |
                (   Wd.regNum <<  7                ) |
                (Ws.mode      <<  4                ) |
                (Ws.Wx.regNum                      )
            );
        }
        return this;
    }

    private PIC16BitEPC $_mov_rwxmem_rwxmem_impl(final boolean b, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov" + _obw(b), Ws, Wd );

        if(Ws.Wb.regNum != Wd.Wb.regNum) _errorInvalidInstructionForm( _MSTR, "$mov" + _obw(b) );

        _putOpCode24(
              // 0b01111wwwwBhhhddddgggssss
                 0b011110000000000000000000      |
            (b ? 0b000000000100000000000000 : 0) |
            (Wd.Wb.regNum << 15                ) |
            (Wd.mode      << 11                ) |
            (Wd.Wx.regNum <<  7                ) |
            (Ws.mode      <<  4                ) |
            (Ws.Wx.regNum                      )
        );
        return this;
    }

    private PIC16BitEPC $_mov_ws_wd_impl(final boolean b, final _RWx Ws, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov" + _obw(b), Ws, Wd );

                   // 0b01111wwwwBhhhddddgggssss
        _putOpCode24( 0b011110000000000000000000 | (b ? 0b000000000100000000000000 : 0) | (Wd.regNum << 7) | Ws.regNum );
        return this;
    }

    // MOV[.B] Ws, [<Wd> + <slit10/slit9>]
    public PIC16BitEPC $mov  (final _RWx Ws, final _RWxMem Wd) throws JXMAsmError { return $_mov_ws_rwxmem_impl(false, Ws, Wd); }
    public PIC16BitEPC $mov_b(final _RWx Ws, final _RWxMem Wd) throws JXMAsmError { return $_mov_ws_rwxmem_impl(true , Ws, Wd); }

    // MOV[.B] [<Ws> + <slit10/slit9>], Wd
    public PIC16BitEPC $mov  (final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mov_rwxmem_wd_impl(false, Ws, Wd); }
    public PIC16BitEPC $mov_b(final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mov_rwxmem_wd_impl(true , Ws, Wd); }

    // MOV[.B] [  <Ws>   ], [  <Wd>   ]
    // MOV[.B] [  <Ws>++ ], [  <Wd>++ ]
    // MOV[.B] [  <Ws>-- ], [  <Wd>-- ]
    // MOV[.B] [++<Ws>   ], [++<Wd>   ]
    // MOV[.B] [--<Ws>   ], [--<Wd>   ]
    // MOV[.B] [<Ws>+<Wb>], [<Wd>+<Wb>]
    public PIC16BitEPC $mov  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_mov_rwxmem_rwxmem_impl(false, Ws, Wd); }
    public PIC16BitEPC $mov_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_mov_rwxmem_rwxmem_impl(true , Ws, Wd); }

    // MOV[.B] <Ws>, <Wd>
    public PIC16BitEPC $mov  (final _RWx Ws, final _RWx Wd) throws JXMAsmError { return $_mov_ws_wd_impl(false, Ws, Wd); }
    public PIC16BitEPC $mov_b(final _RWx Ws, final _RWx Wd) throws JXMAsmError { return $_mov_ws_wd_impl(true , Ws, Wd); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SWAP[.B] <Wd>
    public PIC16BitEPC $_swap_impl(final boolean b, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$swap" + _obw(b), Wd );

        _putOpCode24( 0b111111011000000000000000 | ( b ? 0b000000000100000000000000 : 0 ) | Wd.regNum );
        return this;
    }

    public PIC16BitEPC $swap  (final _RWx Wd) throws JXMAsmError { return $_swap_impl(false, Wd); }
    public PIC16BitEPC $swap_b(final _RWx Wd) throws JXMAsmError { return $_swap_impl(true , Wd); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // DAW.B <Wd>
    public PIC16BitEPC $daw_b(final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$daw_b", Wd );

        _putOpCode24( 0b111111010100000000000000 | Wd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ADD[.B] <f>
    // ADD[.B] <f>, WREG
    private PIC16BitEPC $_alu_f_wreg_impl(final String str, final int opcode, final boolean b, final int f, final _WRg WReg) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), f, WReg );

        if(!b) _errorIfAddressOffsetNotAligned2B(_MSTR, f);

        _checkUOffset(_MSTR, f, 0, 13);
        _putOpCode24(
                            // 0b.........BD.............
                               opcode                           |
            (  b             ? 0b000000000100000000000000 : 0 ) |
            ( (WReg == null) ? 0b000000000010000000000000 : 0 ) |
            ( f                                               )
        );
        return this;
    }

    public PIC16BitEPC $add  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("add", 0b101101000000000000000000, false, f, null); }
    public PIC16BitEPC $add_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("add", 0b101101000000000000000000, true , f, null); }
    public PIC16BitEPC $add  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("add", 0b101101000000000000000000, false, f, WReg); }
    public PIC16BitEPC $add_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("add", 0b101101000000000000000000, true , f, WReg); }

    // ADD[.B] #<ulit10/ulit8>, <Wd>
    private PIC16BitEPC $_alu_ulit10or8_wd_impl(final String str, final int opcode, final boolean b, final _Lit value10_value8, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), value10_value8, Wd );

        _checkUOffset(_MSTR, value10_value8.value, 0, b ? 8 : 10);
        _putOpCode24(
              // 0b.........B..........DDDD
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (value10_value8.value << 4         ) |
            (Wd.regNum                         )
        );
        return this;
    }

    public PIC16BitEPC $add  (final _Lit value10, final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("add", 0b101100000000000000000000, false, value10, Wd); }
    public PIC16BitEPC $add_b(final _Lit value8 , final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("add", 0b101100000000000000000000, true , value8 , Wd); }

    // ADD[.B] <Wb>, #<ulit5>,    <Wd>
    // ADD[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // ADD[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // ADD[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // ADD[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // ADD[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    private PIC16BitEPC $_alu_wb_ulit5_wd_impl(final String str, final int opcode, final boolean b, final _RWx Wb, final _Lit value5, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Wb, value5, Wd );

        _checkUOffset(_MSTR, value5.value, 0, 5);
        _putOpCode24(
              // 0b.....wwwwBqqqdddd..kkkkk
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wb.regNum << 15                   ) |
            (Wd.regNum <<  7                   ) |
            (value5.value                      )
        );
        return this;
    }

    private PIC16BitEPC $_alu_wb_ulit5_rwxmem_impl(final String str, final int opcode, final boolean b, final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Wb, value5, Wd );

        if( Wd.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$" + str + _obw(b) );

        _checkUOffset(_MSTR, value5.value, 0, 5);
        _putOpCode24(
              // 0b.....wwwwBqqqdddd..kkkkk
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wb.regNum    << 15                ) |
            (Wd.mode      << 11                ) |
            (Wd.Wx.regNum <<  7                ) |
            (value5.value                      )
        );
        return this;
    }

    public PIC16BitEPC $add  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("add", 0b010000000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $add_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("add", 0b010000000000000001100000, true , Wb, value5, Wd); }
    public PIC16BitEPC $add  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("add", 0b010000000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $add_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("add", 0b010000000000000001100000, true , Wb, value5, Wd); }

    // ADD[.B] <Wb>,    <Ws>   ,    <Wd>
    // ADD[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // ADD[.B] <Wb>, [  <Ws>  ],    <Wd>
    // ADD[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // ADD[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // ADD[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // ADD[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // ADD[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    private PIC16BitEPC $_alu_wb_ws_wd_impl(final String str, final int opcode, final boolean b, final _RWx Wb, final _RWx Ws, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Wb, Ws, Wd );

        _putOpCode24(
              // 0b.....wwwwBqqqddddpppssss
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wb.regNum << 15                   ) |
            (Wd.regNum <<  7                   ) |
            (Ws.regNum                         )
        );
        return this;
    }

    private PIC16BitEPC $_alu_wb_rwxmem_rwxmem_impl(final String str, final int opcode, final boolean b, final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Wb, Ws, Wd );

        if( Ws.immOfs_rwxOfs() || Wd.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$" + str + _obw(b) );

        _putOpCode24(
              // 0b.....wwwwBqqqddddpppssss
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wb.regNum    << 15                ) |
            (Wd.mode      << 11                ) |
            (Wd.Wx.regNum <<  7                ) |
            (Ws.mode      <<  4                ) |
            (Ws.Wx.regNum                      )
        );
        return this;
    }

    public PIC16BitEPC $add  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "add", 0b010000000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $add_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "add", 0b010000000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $add  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "add", 0b010000000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $add_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "add", 0b010000000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $add  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "add", 0b010000000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $add_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "add", 0b010000000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $add  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "add", 0b010000000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $add_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "add", 0b010000000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    // ADD <Acc>
    private PIC16BitEPC $_alu_acc_impl(final String str, final int opcode, final _RAcc acc) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str, acc );

        _cpu.chkSupInstGroup_stdDSP( _MSTR, "$add" );

        _putOpCode24( opcode | (acc.regNum << 15) );
        return this;
    }

    public PIC16BitEPC $add(final _RAcc acc) throws JXMAsmError
    { return $_alu_acc_impl("add", 0b110010110000000000000000, acc); }

    // ADD    <Ws>   , #<slit4>, <Acc>
    // ADD [  <Ws>  ], #<slit4>, <Acc>
    // ADD [  <Ws>++], #<slit4>, <Acc>
    // ADD [  <Ws>--], #<slit4>, <Acc>
    // ADD [++<Ws>  ], #<slit4>, <Acc>
    // ADD [--<Ws>  ], #<slit4>, <Acc>
    private PIC16BitEPC $_alu_rwxmem_slit4_acc_impl(final String str, final int opcode, final _RWxMem Ws, final _Lit slit4, final _RAcc acc) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str, Ws, slit4, acc );

        _cpu.chkSupInstGroup_stdDSP( _MSTR, "$" + str );

        if( Ws.immOfs() ) _errorInvalidInstructionForm( _MSTR, "$" + str );

        final int imm = _checkSOffset(_MSTR, slit4.value, 0, 4);
        _putOpCode24(
          // 0b........Awwwwrrrrgggssss
             opcode               |
            (acc.regNum   << 15 ) |
            (Ws.Wb.regNum << 11 ) |
            (imm          <<  7 ) |
            (Ws.mode      <<  4 ) |
            (Ws.Wx.regNum       )
        );
        return this;
    }

    public PIC16BitEPC $add(final _RWxMem Ws, final _Lit value4, final _RAcc acc) throws JXMAsmError { return $_alu_rwxmem_slit4_acc_impl( "add", 0b110010010000000000000000,           Ws , value4      , acc ); }
    public PIC16BitEPC $add(final _RWxMem Ws,                    final _RAcc acc) throws JXMAsmError { return $_alu_rwxmem_slit4_acc_impl( "add", 0b110010010000000000000000,           Ws , Expr._lit0(), acc ); }
    public PIC16BitEPC $add(final _RWx    Ws, final _Lit value4, final _RAcc acc) throws JXMAsmError { return $_alu_rwxmem_slit4_acc_impl( "add", 0b110010010000000000000000, Expr._reg(Ws), value4      , acc ); }
    public PIC16BitEPC $add(final _RWx    Ws,                    final _RAcc acc) throws JXMAsmError { return $_alu_rwxmem_slit4_acc_impl( "add", 0b110010010000000000000000, Expr._reg(Ws), Expr._lit0(), acc ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ADDC[.B] <f>
    // ADDC[.B] <f>, WREG
    public PIC16BitEPC $addc  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("addc", 0b101101001000000000000000, false, f, null); }
    public PIC16BitEPC $addc_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("addc", 0b101101001000000000000000, true , f, null); }
    public PIC16BitEPC $addc  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("addc", 0b101101001000000000000000, false, f, WReg); }
    public PIC16BitEPC $addc_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("addc", 0b101101001000000000000000, true , f, WReg); }

    // ADDC[.B] #<ulit10/ulit8>, <Wd>
    public PIC16BitEPC $addc  (final _Lit value10, final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("addc", 0b101100001000000000000000, false, value10, Wd); }
    public PIC16BitEPC $addc_b(final _Lit value8 , final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("addc", 0b101100001000000000000000, true , value8 , Wd); }

    // ADDC[.B] <Wb>, #<ulit5>,    <Wd>
    // ADDC[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // ADDC[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // ADDC[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // ADDC[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // ADDC[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    public PIC16BitEPC $addc  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("addc", 0b010010000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $addc_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("addc", 0b010010000000000001100000, true , Wb, value5, Wd); }
    public PIC16BitEPC $addc  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("addc", 0b010010000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $addc_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("addc", 0b010010000000000001100000, true , Wb, value5, Wd); }

    // ADDC[.B] <Wb>,    <Ws>   ,    <Wd>
    // ADDC[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // ADDC[.B] <Wb>, [  <Ws>  ],    <Wd>
    // ADDC[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // ADDC[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // ADDC[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // ADDC[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // ADDC[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $addc  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "addc", 0b010010000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $addc_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "addc", 0b010010000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $addc  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "addc", 0b010010000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $addc_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "addc", 0b010010000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $addc  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "addc", 0b010010000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $addc_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "addc", 0b010010000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $addc  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "addc", 0b010010000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $addc_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "addc", 0b010010000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SUB[.B] <f>
    // SUB[.B] <f>, WREG
    public PIC16BitEPC $sub  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("sub", 0b101101010000000000000000, false, f, null); }
    public PIC16BitEPC $sub_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("sub", 0b101101010000000000000000, true , f, null); }
    public PIC16BitEPC $sub  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("sub", 0b101101010000000000000000, false, f, WReg); }
    public PIC16BitEPC $sub_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("sub", 0b101101010000000000000000, true , f, WReg); }

    // SUB[.B] #<ulit10/ulit8>, <Wd>
    public PIC16BitEPC $sub  (final _Lit value10, final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("sub", 0b101100010000000000000000, false, value10, Wd); }
    public PIC16BitEPC $sub_b(final _Lit value8 , final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("sub", 0b101100010000000000000000, true , value8 , Wd); }

    // SUB[.B] <Wb>, #<ulit5>,    <Wd>
    // SUB[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // SUB[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // SUB[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // SUB[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // SUB[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    public PIC16BitEPC $sub  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("sub", 0b010100000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $sub_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("sub", 0b010100000000000001100000, true , Wb, value5, Wd); }
    public PIC16BitEPC $sub  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("sub", 0b010100000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $sub_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("sub", 0b010100000000000001100000, true , Wb, value5, Wd); }

    // SUB[.B] <Wb>,    <Ws>   ,    <Wd>
    // SUB[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // SUB[.B] <Wb>, [  <Ws>  ],    <Wd>
    // SUB[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // SUB[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // SUB[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // SUB[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // SUB[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $sub  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "sub", 0b010100000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $sub_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "sub", 0b010100000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $sub  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "sub", 0b010100000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $sub_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "sub", 0b010100000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $sub  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "sub", 0b010100000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $sub_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "sub", 0b010100000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $sub  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "sub", 0b010100000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $sub_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "sub", 0b010100000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    // SUB <Acc>
    public PIC16BitEPC $sub(final _RAcc acc) throws JXMAsmError
    { return $_alu_acc_impl("sub", 0b110010110011000000000000, acc); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SUBB[.B] <f>
    // SUBB[.B] <f>, WREG
    public PIC16BitEPC $subb  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("subb", 0b101101011000000000000000, false, f, null); }
    public PIC16BitEPC $subb_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("subb", 0b101101011000000000000000, true , f, null); }
    public PIC16BitEPC $subb  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("subb", 0b101101011000000000000000, false, f, WReg); }
    public PIC16BitEPC $subb_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("subb", 0b101101011000000000000000, true , f, WReg); }

    // SUBB[.B] #<ulit10/ulit8>, <Wd>
    public PIC16BitEPC $subb  (final _Lit value10, final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("subb", 0b101100011000000000000000, false, value10, Wd); }
    public PIC16BitEPC $subb_b(final _Lit value8 , final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("subb", 0b101100011000000000000000, true , value8 , Wd); }

    // SUBB[.B] <Wb>, #<ulit5>,    <Wd>
    // SUBB[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // SUBB[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // SUBB[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // SUBB[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // SUBB[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    public PIC16BitEPC $subb  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("subb", 0b010110000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $subb_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("subb", 0b010110000000000001100000, true , Wb, value5, Wd); }
    public PIC16BitEPC $subb  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("subb", 0b010110000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $subb_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("subb", 0b010110000000000001100000, true , Wb, value5, Wd); }

    // SUBB[.B] <Wb>,    <Ws>   ,    <Wd>
    // SUBB[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // SUBB[.B] <Wb>, [  <Ws>  ],    <Wd>
    // SUBB[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // SUBB[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // SUBB[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // SUBB[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // SUBB[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $subb  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "subb", 0b010110000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subb_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "subb", 0b010110000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subb  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subb", 0b010110000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subb_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subb", 0b010110000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subb  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subb", 0b010110000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $subb_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subb", 0b010110000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $subb  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subb", 0b010110000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $subb_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subb", 0b010110000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SUBR[.B] <f>
    // SUBR[.B] <f>, WREG
    public PIC16BitEPC $subr  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("subr", 0b101111010000000000000000, false, f, null); }
    public PIC16BitEPC $subr_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("subr", 0b101111010000000000000000, true , f, null); }
    public PIC16BitEPC $subr  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("subr", 0b101111010000000000000000, false, f, WReg); }
    public PIC16BitEPC $subr_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("subr", 0b101111010000000000000000, true , f, WReg); }

    // SUBR[.B] <Wb>, #<ulit5>,    <Wd>
    // SUBR[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // SUBR[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // SUBR[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // SUBR[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // SUBR[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    public PIC16BitEPC $subr  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("subr", 0b000100000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $subr_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("subr", 0b000100000000000001100000, true , Wb, value5, Wd); }
    public PIC16BitEPC $subr  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("subr", 0b000100000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $subr_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("subr", 0b000100000000000001100000, true , Wb, value5, Wd); }

    // SUBR[.B] <Wb>,    <Ws>   ,    <Wd>
    // SUBR[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // SUBR[.B] <Wb>, [  <Ws>  ],    <Wd>
    // SUBR[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // SUBR[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // SUBR[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // SUBR[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // SUBR[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $subr  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "subr", 0b000100000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subr_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "subr", 0b000100000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subr  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subr", 0b000100000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subr_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subr", 0b000100000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subr  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subr", 0b000100000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $subr_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subr", 0b000100000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $subr  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subr", 0b000100000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $subr_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subr", 0b000100000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SUBBR[.B] <f>
    // SUBBR[.B] <f>, WREG
    public PIC16BitEPC $subbr  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("subbr", 0b101111011000000000000000, false, f, null); }
    public PIC16BitEPC $subbr_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("subbr", 0b101111011000000000000000, true , f, null); }
    public PIC16BitEPC $subbr  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("subbr", 0b101111011000000000000000, false, f, WReg); }
    public PIC16BitEPC $subbr_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("subbr", 0b101111011000000000000000, true , f, WReg); }

    // SUBBR[.B] <Wb>, #<ulit5>,    <Wd>
    // SUBBR[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // SUBBR[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // SUBBR[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // SUBBR[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // SUBBR[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    public PIC16BitEPC $subbr  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("subbr", 0b000110000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $subbr_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("subbr", 0b000110000000000001100000, true , Wb, value5, Wd); }
    public PIC16BitEPC $subbr  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("subbr", 0b000110000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $subbr_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("subbr", 0b000110000000000001100000, true , Wb, value5, Wd); }

    // SUBBR[.B] <Wb>,    <Ws>   ,    <Wd>
    // SUBBR[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // SUBBR[.B] <Wb>, [  <Ws>  ],    <Wd>
    // SUBBR[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // SUBBR[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // SUBBR[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // SUBBR[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // SUBBR[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $subbr  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "subbr", 0b000110000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subbr_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "subbr", 0b000110000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subbr  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subbr", 0b000110000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subbr_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subbr", 0b000110000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $subbr  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subbr", 0b000110000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $subbr_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subbr", 0b000110000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $subbr  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subbr", 0b000110000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $subbr_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "subbr", 0b000110000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // AND[.B] <f>
    // AND[.B] <f>, WREG
    public PIC16BitEPC $and  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("and", 0b101101100000000000000000, false, f, null); }
    public PIC16BitEPC $and_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("and", 0b101101100000000000000000, true , f, null); }
    public PIC16BitEPC $and  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("and", 0b101101100000000000000000, false, f, WReg); }
    public PIC16BitEPC $and_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("and", 0b101101100000000000000000, true , f, WReg); }

    // AND[.B] #<ulit10/ulit8>, <Wd>
    public PIC16BitEPC $and  (final _Lit value10, final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("and", 0b101100100000000000000000, false, value10, Wd); }
    public PIC16BitEPC $and_b(final _Lit value8 , final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("and", 0b101100100000000000000000, true , value8 , Wd); }

    // AND[.B] <Wb>, #<ulit5>,    <Wd>
    // AND[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // AND[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // AND[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // AND[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // AND[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    public PIC16BitEPC $and  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("and", 0b011000000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $and_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("and", 0b011000000000000001100000, true , Wb, value5, Wd); }
    public PIC16BitEPC $and  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("and", 0b011000000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $and_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("and", 0b011000000000000001100000, true , Wb, value5, Wd); }

    // AND[.B] <Wb>,    <Ws>   ,    <Wd>
    // AND[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // AND[.B] <Wb>, [  <Ws>  ],    <Wd>
    // AND[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // AND[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // AND[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // AND[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // AND[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $and  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "and", 0b011000000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $and_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "and", 0b011000000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $and  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "and", 0b011000000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $and_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "and", 0b011000000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $and  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "and", 0b011000000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $and_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "and", 0b011000000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $and  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "and", 0b011000000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $and_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "and", 0b011000000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // IOR[.B] <f>
    // IOR[.B] <f>, WREG
    public PIC16BitEPC $ior  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("ior", 0b101101110000000000000000, false, f, null); }
    public PIC16BitEPC $ior_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("ior", 0b101101110000000000000000, true , f, null); }
    public PIC16BitEPC $ior  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("ior", 0b101101110000000000000000, false, f, WReg); }
    public PIC16BitEPC $ior_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("ior", 0b101101110000000000000000, true , f, WReg); }

    // IOR[.B] #<ulit10/ulit8>, <Wd>
    public PIC16BitEPC $ior  (final _Lit value10, final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("ior", 0b101100110000000000000000, false, value10, Wd); }
    public PIC16BitEPC $ior_b(final _Lit value8 , final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("ior", 0b101100110000000000000000, true , value8 , Wd); }

    // IOR[.B] <Wb>, #<ulit5>,    <Wd>
    // IOR[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // IOR[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // IOR[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // IOR[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // IOR[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    public PIC16BitEPC $ior  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ( "ior", 0b011100000000000001100000, false, Wb, value5, Wd ); }
    public PIC16BitEPC $ior_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ( "ior", 0b011100000000000001100000, true , Wb, value5, Wd ); }
    public PIC16BitEPC $ior  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl( "ior", 0b011100000000000001100000, false, Wb, value5, Wd ); }
    public PIC16BitEPC $ior_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl( "ior", 0b011100000000000001100000, true , Wb, value5, Wd ); }

    // IOR[.B] <Wb>,    <Ws>   ,    <Wd>
    // IOR[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // IOR[.B] <Wb>, [  <Ws>  ],    <Wd>
    // IOR[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // IOR[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // IOR[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // IOR[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // IOR[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $ior  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "ior", 0b011100000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $ior_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "ior", 0b011100000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $ior  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "ior", 0b011100000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $ior_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "ior", 0b011100000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $ior  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "ior", 0b011100000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $ior_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "ior", 0b011100000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $ior  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "ior", 0b011100000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $ior_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "ior", 0b011100000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // XOR[.B] <f>
    // XOR[.B] <f>, WREG
    public PIC16BitEPC $xor  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("xor", 0b101101101000000000000000, false, f, null); }
    public PIC16BitEPC $xor_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("xor", 0b101101101000000000000000, true , f, null); }
    public PIC16BitEPC $xor  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("xor", 0b101101101000000000000000, false, f, WReg); }
    public PIC16BitEPC $xor_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("xor", 0b101101101000000000000000, true , f, WReg); }

    // XOR[.B] #<ulit10/ulit8>, <Wd>
    public PIC16BitEPC $xor  (final _Lit value10, final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("xor", 0b101100101000000000000000, false, value10, Wd); }
    public PIC16BitEPC $xor_b(final _Lit value8 , final _RWx Wd) throws JXMAsmError { return $_alu_ulit10or8_wd_impl("xor", 0b101100101000000000000000, true , value8 , Wd); }

    // XOR[.B] <Wb>, #<ulit5>,    <Wd>
    // XOR[.B] <Wb>, #<ulit5>, [  <Wd>  ]
    // XOR[.B] <Wb>, #<ulit5>, [  <Wd>++]
    // XOR[.B] <Wb>, #<ulit5>, [  <Wd>--]
    // XOR[.B] <Wb>, #<ulit5>, [++<Wd>  ]
    // XOR[.B] <Wb>, #<ulit5>, [--<Wd>  ]
    public PIC16BitEPC $xor  (final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("xor", 0b011010000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $xor_b(final _RWx Wb, final _Lit value5, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ulit5_wd_impl    ("xor", 0b011010000000000001100000, true , Wb, value5, Wd); }
    public PIC16BitEPC $xor  (final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("xor", 0b011010000000000001100000, false, Wb, value5, Wd); }
    public PIC16BitEPC $xor_b(final _RWx Wb, final _Lit value5, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_ulit5_rwxmem_impl("xor", 0b011010000000000001100000, true , Wb, value5, Wd); }

    // XOR[.B] <Wb>,    <Ws>   ,    <Wd>
    // XOR[.B] <Wb>,    <Ws>   , [  <Wd>  ]
    // XOR[.B] <Wb>, [  <Ws>  ],    <Wd>
    // XOR[.B] <Wb>, [  <Ws>  ], [  <Wd>  ]
    // XOR[.B] <Wb>, [  <Ws>++], [  <Wd>++]
    // XOR[.B] <Wb>, [  <Ws>--], [  <Wd>--]
    // XOR[.B] <Wb>, [++<Ws>  ], [++<Wd>  ]
    // XOR[.B] <Wb>, [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $xor  (final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "xor", 0b011010000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $xor_b(final _RWx Wb, final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_ws_wd_impl        ( "xor", 0b011010000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $xor  (final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "xor", 0b011010000000000000000000, false, Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $xor_b(final _RWx Wb, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "xor", 0b011010000000000000000000, true , Wb,           Ws ,           Wd  ); }
    public PIC16BitEPC $xor  (final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "xor", 0b011010000000000000000000, false, Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $xor_b(final _RWx Wb, final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "xor", 0b011010000000000000000000, true , Wb, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $xor  (final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "xor", 0b011010000000000000000000, false, Wb,           Ws , Expr._reg(Wd) ); }
    public PIC16BitEPC $xor_b(final _RWx Wb, final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_wb_rwxmem_rwxmem_impl( "xor", 0b011010000000000000000000, true , Wb,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BSET[.B] <f>, #<ulit4/ulit3>
    private PIC16BitEPC $_alu_f_ulit4or3_impl(final String str, final int opcode, final boolean b, final int f_, final _Lit ulit4_ulit3_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), f_, ulit4_ulit3_ );

        if(!b) _errorIfAddressOffsetNotAligned2B(_MSTR, f_);

        final boolean adj   = (ulit4_ulit3_.value > 7);
        final int     f     = adj ? (f_ + 1) : f_;
        final int     ulit3 = adj ? (ulit4_ulit3_.value - 8) : ulit4_ulit3_.value;

        _checkUOffset(_MSTR, f    , 0, 13);
        _checkUOffset(_MSTR, ulit3, 0,  3);
        _putOpCode24(
         // 0b........bbbfffffffffffff
             opcode       |
            (ulit3 << 13) |
            (f          )
        );
        return this;
    }

    public PIC16BitEPC $bset  (final int f, final _Lit bit4) throws JXMAsmError { return $_alu_f_ulit4or3_impl("bset", 0b101010000000000000000000, false, f, bit4); }
    public PIC16BitEPC $bset_b(final int f, final _Lit bit3) throws JXMAsmError { return $_alu_f_ulit4or3_impl("bset", 0b101010000000000000000000, true , f, bit3); }

    // BSET[.B]    <Wd>   , #<ulit4/ulit3>
    // BSET[.B] [  <Wd>  ], #<ulit4/ulit3>
    // BSET[.B] [  <Wd>++], #<ulit4/ulit3>
    // BSET[.B] [  <Wd>--], #<ulit4/ulit3>
    // BSET[.B] [++<Wd>  ], #<ulit4/ulit3>
    // BSET[.B] [--<Wd>  ], #<ulit4/ulit3>
    private PIC16BitEPC $_alu_rwxmem_ulit4or3_impl(final String str, final int opcode, final boolean b, _RWxMem Wx, final _Lit ulit4_ulit3) throws JXMAsmError
    {
        final String name = ( !b && str.contains("_") ) ? str : ( str + _obw(b) );

        _MNEMONIC_STRING( "$" + name, Wx, ulit4_ulit3 );

        if( Wx.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$" + name );

        _checkUOffset(_MSTR, ulit4_ulit3.value, 0, b ? 3 : 4);
        _putOpCode24(
              // 0b........bbbb.B...pppssss
                 opcode                          |
            (b ? 0b000000000000010000000000 : 0) |
            (ulit4_ulit3.value << 12             ) |
            (Wx.mode         <<  4             ) |
            (Wx.Wx.regNum                      )
        );
        return this;

    }

    public PIC16BitEPC $bset  (_RWx    Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "bset", 0b101000000000000000000000, false, Expr._reg(Wd), bit4 ); }
    public PIC16BitEPC $bset_b(_RWx    Wd, final _Lit bit3) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "bset", 0b101000000000000000000000, true , Expr._reg(Wd), bit3 ); }
    public PIC16BitEPC $bset  (_RWxMem Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "bset", 0b101000000000000000000000, false,           Wd , bit4 ); }
    public PIC16BitEPC $bset_b(_RWxMem Wd, final _Lit bit3) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "bset", 0b101000000000000000000000, true ,           Wd , bit3 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BCLR[.B] <f>, #<ulit4/ulit3>
    public PIC16BitEPC $bclr  (final int f, final _Lit bit4) throws JXMAsmError { return $_alu_f_ulit4or3_impl("bclr", 0b101010010000000000000000, false, f, bit4); }
    public PIC16BitEPC $bclr_b(final int f, final _Lit bit3) throws JXMAsmError { return $_alu_f_ulit4or3_impl("bclr", 0b101010010000000000000000, true , f, bit3); }

    // BCLR[.B]    <Wd>   , #<ulit4/ulit3>
    // BCLR[.B] [  <Wd>  ], #<ulit4/ulit3>
    // BCLR[.B] [  <Wd>++], #<ulit4/ulit3>
    // BCLR[.B] [  <Wd>--], #<ulit4/ulit3>
    // BCLR[.B] [++<Wd>  ], #<ulit4/ulit3>
    // BCLR[.B] [--<Wd>  ], #<ulit4/ulit3>
    public PIC16BitEPC $bclr  (_RWx    Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "bclr", 0b101000010000000000000000, false, Expr._reg(Wd), bit4 ); }
    public PIC16BitEPC $bclr_b(_RWx    Wd, final _Lit bit3) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "bclr", 0b101000010000000000000000, true , Expr._reg(Wd), bit3 ); }
    public PIC16BitEPC $bclr  (_RWxMem Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "bclr", 0b101000010000000000000000, false,           Wd , bit4 ); }
    public PIC16BitEPC $bclr_b(_RWxMem Wd, final _Lit bit3) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "bclr", 0b101000010000000000000000, true ,           Wd , bit3 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BTG[.B] <f>, #<ulit4/ulit3>
    public PIC16BitEPC $btg  (final int f, final _Lit bit4) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btg", 0b101010100000000000000000, false, f, bit4); }
    public PIC16BitEPC $btg_b(final int f, final _Lit bit3) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btg", 0b101010100000000000000000, true , f, bit3); }

    // BTG[.B]    <Wd>   , #<ulit4/ulit3>
    // BTG[.B] [  <Wd>  ], #<ulit4/ulit3>
    // BTG[.B] [  <Wd>++], #<ulit4/ulit3>
    // BTG[.B] [  <Wd>--], #<ulit4/ulit3>
    // BTG[.B] [++<Wd>  ], #<ulit4/ulit3>
    // BTG[.B] [--<Wd>  ], #<ulit4/ulit3>
    public PIC16BitEPC $btg  (_RWx    Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btg", 0b101000100000000000000000, false, Expr._reg(Wd), bit4 ); }
    public PIC16BitEPC $btg_b(_RWx    Wd, final _Lit bit3) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btg", 0b101000100000000000000000, true , Expr._reg(Wd), bit3 ); }
    public PIC16BitEPC $btg  (_RWxMem Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btg", 0b101000100000000000000000, false,           Wd , bit4 ); }
    public PIC16BitEPC $btg_b(_RWxMem Wd, final _Lit bit3) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btg", 0b101000100000000000000000, true ,           Wd , bit3 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BTST[.B] <f>, #<ulit4/ulit3>
    public PIC16BitEPC $btst  (final int f, final _Lit bit4) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btst", 0b101010110000000000000000, false, f, bit4); }
    public PIC16BitEPC $btst_b(final int f, final _Lit bit3) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btst", 0b101010110000000000000000, true , f, bit3); }

    // BTST.C    <Ws>   , #<ulit4>
    // BTST.C [  <Ws>  ], #<ulit4>
    // BTST.C [  <Ws>++], #<ulit4>
    // BTST.C [  <Ws>--], #<ulit4>
    // BTST.C [++<Ws>  ], #<ulit4>
    // BTST.C [--<Ws>  ], #<ulit4>
    public PIC16BitEPC $btst_c(_RWx    Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btst_c", 0b101000110000000000000000, false, Expr._reg(Wd), bit4 ); }
    public PIC16BitEPC $btst_c(_RWxMem Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btst_c", 0b101000110000000000000000, false,           Wd , bit4 ); }

    // BTST.Z    <Ws>   , #<ulit4>
    // BTST.Z [  <Ws>  ], #<ulit4>
    // BTST.Z [  <Ws>++], #<ulit4>
    // BTST.Z [  <Ws>--], #<ulit4>
    // BTST.Z [++<Ws>  ], #<ulit4>
    // BTST.Z [--<Ws>  ], #<ulit4>
    public PIC16BitEPC $btst_z(_RWx    Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btst_z", 0b101000110000100000000000, false, Expr._reg(Wd), bit4 ); }
    public PIC16BitEPC $btst_z(_RWxMem Wd, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btst_z", 0b101000110000100000000000, false,           Wd , bit4 ); }

    // BTST.C    <Ws>   , <Wb>
    // BTST.C [  <Ws>  ], <Wb>
    // BTST.C [  <Ws>++], <Wb>
    // BTST.C [  <Ws>--], <Wb>
    // BTST.C [++<Ws>  ], <Wb>
    // BTST.C [--<Ws>  ], <Wb>
    private PIC16BitEPC $_alu_rwxmem_wb_impl(final String str, final int opcode, final boolean z, _RWxMem Ws, final _RWx Wb) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _ozc(z), Ws, Wb );

        if( Ws.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$" + str + _ozc(z) );

        _putOpCode24(
              // 0b........Zwwww....pppssss
                 opcode                          |
            (z ? 0b000000001000000000000000 : 0) |
            (Wb.regNum    << 11                ) |
            (Ws.mode      <<  4                ) |
            (Ws.Wx.regNum                      )
        );
        return this;
    }

    public PIC16BitEPC $btst_c(_RWx    Wd, final _RWx Wb) throws JXMAsmError { return $_alu_rwxmem_wb_impl( "btst_c", 0b101001010000000000000000, false, Expr._reg(Wd), Wb ); }
    public PIC16BitEPC $btst_c(_RWxMem Wd, final _RWx Wb) throws JXMAsmError { return $_alu_rwxmem_wb_impl( "btst_c", 0b101001010000000000000000, false,           Wd , Wb ); }

    // BTST.Z    <Ws>   , <Wb>
    // BTST.Z [  <Ws>  ], <Wb>
    // BTST.Z [  <Ws>++], <Wb>
    // BTST.Z [  <Ws>--], <Wb>
    // BTST.Z [++<Ws>  ], <Wb>
    // BTST.Z [--<Ws>  ], <Wb>
    public PIC16BitEPC $btst_z(_RWx    Wd, final _RWx Wb) throws JXMAsmError { return $_alu_rwxmem_wb_impl( "btst_z", 0b101001010000000000000000, true , Expr._reg(Wd), Wb ); }
    public PIC16BitEPC $btst_z(_RWxMem Wd, final _RWx Wb) throws JXMAsmError { return $_alu_rwxmem_wb_impl( "btst_z", 0b101001010000000000000000, true ,           Wd , Wb ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BSW.C    <Ws>   , <Wb>
    // BSW.C [  <Ws>  ], <Wb>
    // BSW.C [  <Ws>++], <Wb>
    // BSW.C [  <Ws>--], <Wb>
    // BSW.C [++<Ws>  ], <Wb>
    // BSW.C [--<Ws>  ], <Wb>
    public PIC16BitEPC $bsw_c(_RWx    Wd, final _RWx Wb) throws JXMAsmError { return $_alu_rwxmem_wb_impl( "bsw_c", 0b101011010000000000000000, false, Expr._reg(Wd), Wb ); }
    public PIC16BitEPC $bsw_c(_RWxMem Wd, final _RWx Wb) throws JXMAsmError { return $_alu_rwxmem_wb_impl( "bsw_c", 0b101011010000000000000000, false,           Wd , Wb ); }

    // BSW.Z    <Ws>   , <Wb>
    // BSW.Z [  <Ws>  ], <Wb>
    // BSW.Z [  <Ws>++], <Wb>
    // BSW.Z [  <Ws>--], <Wb>
    // BSW.Z [++<Ws>  ], <Wb>
    // BSW.Z [--<Ws>  ], <Wb>
    public PIC16BitEPC $bsw_z(_RWx    Wd, final _RWx Wb) throws JXMAsmError { return $_alu_rwxmem_wb_impl( "bsw_z", 0b101011010000000000000000, true , Expr._reg(Wd), Wb ); }
    public PIC16BitEPC $bsw_z(_RWxMem Wd, final _RWx Wb) throws JXMAsmError { return $_alu_rwxmem_wb_impl( "bsw_z", 0b101011010000000000000000, true ,           Wd , Wb ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BTSS[.B] <f>, #<ulit4/ulit3>
    public PIC16BitEPC $btss  (final int f, final _Lit bit4) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btss", 0b101011100000000000000000, false, f, bit4); }
    public PIC16BitEPC $btss_b(final int f, final _Lit bit3) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btss", 0b101011100000000000000000, true , f, bit3); }

    // BTSS    <Ws>   , #<ulit4>
    // BTSS [  <Ws>  ], #<ulit4>
    // BTSS [  <Ws>++], #<ulit4>
    // BTSS [  <Ws>--], #<ulit4>
    // BTSS [++<Ws>  ], #<ulit4>
    // BTSS [--<Ws>  ], #<ulit4>
    public PIC16BitEPC $btss(_RWx    Ws, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btss", 0b101001100000000000000000, false, Expr._reg(Ws), bit4 ); }
    public PIC16BitEPC $btss(_RWxMem Ws, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btss", 0b101001100000000000000000, false,           Ws , bit4 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BTSC[.B] <f>, #<ulit4/ulit3>
    public PIC16BitEPC $btsc  (final int f, final _Lit bit4) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btsc", 0b101011110000000000000000, false, f, bit4); }
    public PIC16BitEPC $btsc_b(final int f, final _Lit bit3) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btsc", 0b101011110000000000000000, true , f, bit3); }

    // BTSC    <Ws>   , #<ulit4>
    // BTSC [  <Ws>  ], #<ulit4>
    // BTSC [  <Ws>++], #<ulit4>
    // BTSC [  <Ws>--], #<ulit4>
    // BTSC [++<Ws>  ], #<ulit4>
    // BTSC [--<Ws>  ], #<ulit4>
    public PIC16BitEPC $btsc(_RWx    Ws, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btsc", 0b101001110000000000000000, false, Expr._reg(Ws), bit4 ); }
    public PIC16BitEPC $btsc(_RWxMem Ws, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btsc", 0b101001110000000000000000, false,           Ws , bit4 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // BTSTS[.B] <f>, #<ulit4/ulit3>
    public PIC16BitEPC $btsts  (final int f, final _Lit bit4) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btsts", 0b101011000000000000000000, false, f, bit4); }
    public PIC16BitEPC $btsts_b(final int f, final _Lit bit3) throws JXMAsmError { return $_alu_f_ulit4or3_impl("btsts", 0b101011000000000000000000, true , f, bit3); }

    // BTSTS.C    <Ws>   , #<ulit4>
    // BTSTS.C [  <Ws>  ], #<ulit4>
    // BTSTS.C [  <Ws>++], #<ulit4>
    // BTSTS.C [  <Ws>--], #<ulit4>
    // BTSTS.C [++<Ws>  ], #<ulit4>
    // BTSTS.C [--<Ws>  ], #<ulit4>
    public PIC16BitEPC $btsts_c(_RWx    Ws, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btsts_c", 0b101001000000000000000000, false, Expr._reg(Ws), bit4 ); }
    public PIC16BitEPC $btsts_c(_RWxMem Ws, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btsts_c", 0b101001000000000000000000, false,           Ws , bit4 ); }

    // BTSTS.Z    <Ws>   , #<ulit4>
    // BTSTS.Z [  <Ws>  ], #<ulit4>
    // BTSTS.Z [  <Ws>++], #<ulit4>
    // BTSTS.Z [  <Ws>--], #<ulit4>
    // BTSTS.Z [++<Ws>  ], #<ulit4>
    // BTSTS.Z [--<Ws>  ], #<ulit4>
    public PIC16BitEPC $btsts_z(_RWx    Ws, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btsts_z", 0b101001000000100000000000, false, Expr._reg(Ws), bit4 ); }
    public PIC16BitEPC $btsts_z(_RWxMem Ws, final _Lit bit4) throws JXMAsmError { return $_alu_rwxmem_ulit4or3_impl( "btsts_z", 0b101001000000100000000000, false,           Ws , bit4 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : BFEXT * (dsPIC33C) ??? #####
    // ##### ??? TODO : BFINS * (dsPIC33C) ??? #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // MUL[.B] <f>
    private PIC16BitEPC $_mul_f_impl(final boolean b, final int f) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mul" + _obw(b), f );

        if(!b) _errorIfAddressOffsetNotAligned2B(_MSTR, f);

         _checkUOffset(_MSTR, f, 0, 13);
        _putOpCode24(
                 0b101111000000000000000000      |
            (b ? 0b000000000100000000000000 : 0) |
            (f                                 )
        );
        return this;
    }

    public PIC16BitEPC $mul  (final int f) throws JXMAsmError { return $_mul_f_impl(false, f); }
    public PIC16BitEPC $mul_b(final int f) throws JXMAsmError { return $_mul_f_impl(true , f); }

    // MUL.SS <Wb>,    <Ws>   , <Wd>
    // MUL.SS <Wb>, [  <Ws>  ], <Wd>
    // MUL.SS <Wb>, [  <Ws>++], <Wd>
    // MUL.SS <Wb>, [  <Ws>--], <Wd>
    // MUL.SS <Wb>, [++<Ws>  ], <Wd>
    // MUL.SS <Wb>, [--<Ws>  ], <Wd>
    private PIC16BitEPC $_mul_wb_rwxmem_wd_impl(final String str, final int opcode, final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mul_" + str, Wb, Ws, Wd );

        if( Ws.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$mul_" + str);

        _putOpCode24(
          // 0b.........wwwwddddpppssss
             opcode              |
            (Wb.regNum    << 11) |
            (Wd.regNum    <<  7) |
            (Ws.mode      <<  4) |
            (Ws.Wx.regNum      )
        );
        return this;
    }

    public PIC16BitEPC $mul_ss(final _RWx Wb, final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_mul_wb_rwxmem_wd_impl( "ss", 0b101110011000000000000000, Wb, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $mul_ss(final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mul_wb_rwxmem_wd_impl( "ss", 0b101110011000000000000000, Wb,           Ws , Wd ); }


    // MUL.SU <Wb>, #<ulit5>, <Wd>
    private PIC16BitEPC $_mul_wb_ulit5_wd_impl(final String str, final int opcode, final _RWx Wb, final _Lit uint5, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mul_" + str, Wb, uint5, Wd );

        _checkUOffset(_MSTR, uint5.value, 0, 5);
        _putOpCode24(
          // 0b.........wwwwdddd..kkkkk
             opcode              |
            (Wb.regNum    << 11) |
            (Wd.regNum    <<  7) |
            (uint5.value       )
        );
        return this;
    }

    public PIC16BitEPC $mul_su(final _RWx Wb, final _Lit uint5, final _RWx Wd) throws JXMAsmError { return $_mul_wb_ulit5_wd_impl( "su", 0b101110010000000001100000, Wb, uint5, Wd ); }

    // MUL.SU <Wb>,    <Ws>   , <Wd>
    // MUL.SU <Wb>, [  <Ws>  ], <Wd>
    // MUL.SU <Wb>, [  <Ws>++], <Wd>
    // MUL.SU <Wb>, [  <Ws>--], <Wd>
    // MUL.SU <Wb>, [++<Ws>  ], <Wd>
    // MUL.SU <Wb>, [--<Ws>  ], <Wd>
    public PIC16BitEPC $mul_su(final _RWx Wb, final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_mul_wb_rwxmem_wd_impl( "su", 0b101110010000000000000000, Wb, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $mul_su(final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mul_wb_rwxmem_wd_impl( "su", 0b101110010000000000000000, Wb,           Ws , Wd ); }

    // MUL.US <Wb>,    <Ws>   , <Wd>
    // MUL.US <Wb>, [  <Ws>  ], <Wd>
    // MUL.US <Wb>, [  <Ws>++], <Wd>
    // MUL.US <Wb>, [  <Ws>--], <Wd>
    // MUL.US <Wb>, [++<Ws>  ], <Wd>
    // MUL.US <Wb>, [--<Ws>  ], <Wd>
    public PIC16BitEPC $mul_us(final _RWx Wb, final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_mul_wb_rwxmem_wd_impl( "us", 0b101110001000000000000000, Wb, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $mul_us(final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mul_wb_rwxmem_wd_impl( "us", 0b101110001000000000000000, Wb,           Ws , Wd ); }

    // MUL.UU <Wb>, #<ulit5>, <Wd>
    public PIC16BitEPC $mul_uu(final _RWx Wb, final _Lit uint5, final _RWx Wd) throws JXMAsmError { return $_mul_wb_ulit5_wd_impl( "uu", 0b101110000000000001100000, Wb, uint5, Wd ); }

    // MUL.UU <Wb>,    <Ws>   , <Wd>
    // MUL.UU <Wb>, [  <Ws>  ], <Wd>
    // MUL.UU <Wb>, [  <Ws>++], <Wd>
    // MUL.UU <Wb>, [  <Ws>--], <Wd>
    // MUL.UU <Wb>, [++<Ws>  ], <Wd>
    // MUL.UU <Wb>, [--<Ws>  ], <Wd>
    public PIC16BitEPC $mul_uu(final _RWx Wb, final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_mul_wb_rwxmem_wd_impl( "uu", 0b101110000000000000000000, Wb, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $mul_uu(final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mul_wb_rwxmem_wd_impl( "uu", 0b101110000000000000000000, Wb,           Ws , Wd ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : MUL.SS <Wb>, <*Ws*>, <Acc> (dsPIC33E, dsPIC33C) ??? #####

    // ##### ??? TODO : MUL.SU <Wb>, #<ulit5>, <Acc> (dsPIC33E, dsPIC33C) ??? #####
    // ##### ??? TODO : MUL.SU <Wb>, <*Ws*>  , <Acc> (dsPIC33E, dsPIC33C) ??? #####

    // ##### ??? TODO : MUL.US <Wb>, <*Ws*>, <Acc> (dsPIC33E, dsPIC33C) ??? #####

    // ##### ??? TODO : MUL.UU <Wb>, #<ulit5>, <Acc> (dsPIC33E, dsPIC33C) ??? #####
    // ##### ??? TODO : MUL.UU <Wb>, <*Ws*>  , <Acc> (dsPIC33E, dsPIC33C) ??? #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // MULW.SS <Wb>,    <Ws>   , <Wd>
    // MULW.SS <Wb>, [  <Ws>  ], <Wd>
    // MULW.SS <Wb>, [  <Ws>++], <Wd>
    // MULW.SS <Wb>, [  <Ws>--], <Wd>
    // MULW.SS <Wb>, [++<Ws>  ], <Wd>
    // MULW.SS <Wb>, [--<Ws>  ], <Wd>
    private PIC16BitEPC $_mulw_wb_rwxmem_wd_impl(final String str, final int opcode, final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mulw_" + str, Wb, Ws, Wd );

        _cpu.chkSupExtInst( _MSTR, "$mulw_" + str );
        if( Ws.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$mulw_" + str);
        if( !Wd.evenRegLTE12() ) _errorInvalidRegisterNAI(_MSTR, Wd);

        _putOpCode24(
          // 0b.........wwwwddd1pppssss
             0b000000000000000010000000 |
             opcode                     |
            (Wb.regNum    << 11)        |
            (Wd.regNum    <<  7)        |
            (Ws.mode      <<  4)        |
            (Ws.Wx.regNum      )
        );
        return this;
    }

    public PIC16BitEPC $mulw_ss(final _RWx Wb, final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_rwxmem_wd_impl( "ss", 0b101110011000000000000000, Wb, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $mulw_ss(final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_rwxmem_wd_impl( "ss", 0b101110011000000000000000, Wb,           Ws , Wd ); }

    // MULW.SU <Wb>, #<ulit5>, <Wd>
    private PIC16BitEPC $_mulw_wb_ulit5_wd_impl(final String str, final int opcode, final _RWx Wb, final _Lit uint5, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mulw_" + str, Wb, uint5, Wd );

        _cpu.chkSupExtInst( _MSTR, "$mulw_" + str );
        if( !Wd.evenRegLTE12() ) _errorInvalidRegisterNAI(_MSTR, Wd);

        _checkUOffset(_MSTR, uint5.value, 0, 5);
        _putOpCode24(
          // 0b.........wwwwddd1..kkkkk
             0b000000000000000010000000 |
             opcode                     |
            (Wb.regNum    << 11)        |
            (Wd.regNum    <<  7)        |
            (uint5.value       )
        );
        return this;
    }

    public PIC16BitEPC $mulw_su(final _RWx Wb, final _Lit uint5, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_ulit5_wd_impl( "su", 0b101110010000000001100000, Wb, uint5, Wd ); }

    // MULW.SU <Wb>,    <Ws>   , <Wd>
    // MULW.SU <Wb>, [  <Ws>  ], <Wd>
    // MULW.SU <Wb>, [  <Ws>++], <Wd>
    // MULW.SU <Wb>, [  <Ws>--], <Wd>
    // MULW.SU <Wb>, [++<Ws>  ], <Wd>
    // MULW.SU <Wb>, [--<Ws>  ], <Wd>
    public PIC16BitEPC $mulw_su(final _RWx Wb, final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_rwxmem_wd_impl( "su", 0b101110010000000000000000, Wb, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $mulw_su(final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_rwxmem_wd_impl( "su", 0b101110010000000000000000, Wb,           Ws , Wd ); }

    // MULW.US <Wb>,    <Ws>   , <Wd>
    // MULW.US <Wb>, [  <Ws>  ], <Wd>
    // MULW.US <Wb>, [  <Ws>++], <Wd>
    // MULW.US <Wb>, [  <Ws>--], <Wd>
    // MULW.US <Wb>, [++<Ws>  ], <Wd>
    // MULW.US <Wb>, [--<Ws>  ], <Wd>
    public PIC16BitEPC $mulw_us(final _RWx Wb, final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_rwxmem_wd_impl( "us", 0b101110001000000000000000, Wb, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $mulw_us(final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_rwxmem_wd_impl( "us", 0b101110001000000000000000, Wb,           Ws , Wd ); }

    // MULW.UU <Wb>, #<ulit5>, <Wd>
    public PIC16BitEPC $mulw_uu(final _RWx Wb, final _Lit uint5, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_ulit5_wd_impl( "uu", 0b101110000000000001100000, Wb, uint5, Wd ); }

    // MULW.UU <Wb>,    <Ws>   , <Wd>
    // MULW.UU <Wb>, [  <Ws>  ], <Wd>
    // MULW.UU <Wb>, [  <Ws>++], <Wd>
    // MULW.UU <Wb>, [  <Ws>--], <Wd>
    // MULW.UU <Wb>, [++<Ws>  ], <Wd>
    // MULW.UU <Wb>, [--<Ws>  ], <Wd>
    public PIC16BitEPC $mulw_uu(final _RWx Wb, final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_rwxmem_wd_impl( "uu", 0b101110000000000000000000, Wb, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $mulw_uu(final _RWx Wb, final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_mulw_wb_rwxmem_wd_impl( "uu", 0b101110000000000000000000, Wb,           Ws , Wd ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // TBLRDL[.B] [  <Ws>  ],    <Wd>
    // TBLRDL[.B] [  <Ws>  ], [  <Wd>  ]
    // TBLRDL[.B] [  <Ws>++], [  <Wd>++]
    // TBLRDL[.B] [  <Ws>--], [  <Wd>--]
    // TBLRDL[.B] [++<Ws>  ], [++<Wd>  ]
    // TBLRDL[.B] [--<Ws>  ], [--<Wd>  ]
    private PIC16BitEPC $_tblxxx_rwxmem_rwxmem_impl(final String str, final int opcode, final boolean b, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$tbl" + str + _obw(b), Ws, Wd );

        if( Ws.immOfs_rwxOfs() || Wd.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$tbl" + str + _obw(b) );

        _putOpCode24(
              // 0b.........Bqqqddddpppssss
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wd.mode      << 11                ) |
            (Wd.Wx.regNum <<  7                ) |
            (Ws.mode      <<  4                ) |
            (Ws.Wx.regNum                      )
        );
        return this;
    }

    public PIC16BitEPC $tblrdl  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "rdl", 0b101110100000000000000000, false, Ws, Expr._reg(Wd) ); }
    public PIC16BitEPC $tblrdl_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "rdl", 0b101110100000000000000000, true , Ws, Expr._reg(Wd) ); }
    public PIC16BitEPC $tblrdl  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "rdl", 0b101110100000000000000000, false, Ws,           Wd  ); }
    public PIC16BitEPC $tblrdl_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "rdl", 0b101110100000000000000000, true , Ws,           Wd  ); }

    // TBLRDH[.B] [  <Ws>  ],    <Wd>
    // TBLRDH[.B] [  <Ws>  ], [  <Wd>  ]
    // TBLRDH[.B] [  <Ws>++], [  <Wd>++]
    // TBLRDH[.B] [  <Ws>--], [  <Wd>--]
    // TBLRDH[.B] [++<Ws>  ], [++<Wd>  ]
    // TBLRDH[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $tblrdh  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "rdh", 0b101110101000000000000000, false, Ws, Expr._reg(Wd) ); }
    public PIC16BitEPC $tblrdh_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "rdh", 0b101110101000000000000000, true , Ws, Expr._reg(Wd) ); }
    public PIC16BitEPC $tblrdh  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "rdh", 0b101110101000000000000000, false, Ws,           Wd  ); }
    public PIC16BitEPC $tblrdh_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "rdh", 0b101110101000000000000000, true , Ws,           Wd  ); }

    // TBLWTL[.B] [  <Ws>  ],    <Wd>
    // TBLWTL[.B] [  <Ws>  ], [  <Wd>  ]
    // TBLWTL[.B] [  <Ws>++], [  <Wd>++]
    // TBLWTL[.B] [  <Ws>--], [  <Wd>--]
    // TBLWTL[.B] [++<Ws>  ], [++<Wd>  ]
    // TBLWTL[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $tblwtl  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "wtl", 0b101110110000000000000000, false, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $tblwtl_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "wtl", 0b101110110000000000000000, true , Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $tblwtl  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "wtl", 0b101110110000000000000000, false,           Ws , Wd ); }
    public PIC16BitEPC $tblwtl_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "wtl", 0b101110110000000000000000, true ,           Ws , Wd ); }

    // TBLWTH[.B] [  <Ws>  ],    <Wd>
    // TBLWTH[.B] [  <Ws>  ], [  <Wd>  ]
    // TBLWTH[.B] [  <Ws>++], [  <Wd>++]
    // TBLWTH[.B] [  <Ws>--], [  <Wd>--]
    // TBLWTH[.B] [++<Ws>  ], [++<Wd>  ]
    // TBLWTH[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $tblwth  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "wth", 0b101110111000000000000000, false, Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $tblwth_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "wth", 0b101110111000000000000000, true , Expr._reg(Ws), Wd ); }
    public PIC16BitEPC $tblwth  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "wth", 0b101110111000000000000000, false,           Ws , Wd ); }
    public PIC16BitEPC $tblwth_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_tblxxx_rwxmem_rwxmem_impl( "wth", 0b101110111000000000000000, true ,           Ws , Wd ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // MOV.D    <Ws>   , [  <Wd>  ]   ;   <Ws> must be even
    // MOV.D    <Ws>   , [  <Wd>++]   ;   <Ws> must be even
    // MOV.D    <Ws>   , [  <Wd>--]   ;   <Ws> must be even
    // MOV.D    <Ws>   , [++<Wd>  ]   ;   <Ws> must be even
    // MOV.D    <Ws>   , [--<Wd>  ]   ;   <Ws> must be even
    public PIC16BitEPC $mov_d(final _RWx Ws, final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov_d", Ws, Wd );

        _errorIfPrevIsRepeat(_MSTR);

        if( Wd.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$mov_d" );
        if( Ws.oddReg() ) _errorInvalidRegisterNAI(_MSTR, Ws);
        if( Wd.direct_oddReg() ) _errorInvalidRegisterNAI(_MSTR, Wd.Wx);

        _putOpCode24(
          // 0b1011111010qqqdddd000sss0
             0b101111101000000000000000  |
            (Wd.mode      << 11        ) |
            (Wd.Wx.regNum <<  7        ) |
            (Ws.regNum                 )
        );
        return this;
    }

    // MOV.D [  <Ws>  ], <Wd>   ;   <Wd> must be even
    // MOV.D [  <Ws>++], <Wd>   ;   <Wd> must be even
    // MOV.D [  <Ws>--], <Wd>   ;   <Wd> must be even
    // MOV.D [++<Ws>  ], <Wd>   ;   <Wd> must be even
    // MOV.D [--<Ws>  ], <Wd>   ;   <Wd> must be even
    public PIC16BitEPC $mov_d(final _RWxMem Ws, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov_d", Ws, Wd );

        _errorIfPrevIsRepeat(_MSTR);
        if( Ws.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$mov_d" );
        if( Ws.direct_oddReg() ) _errorInvalidRegisterNAI(_MSTR, Ws.Wx);
        if( Wd.oddReg() ) _errorInvalidRegisterNAI(_MSTR, Wd);

        _putOpCode24(
          // 0b1011111000000ddd0pppssss
             0b101111100000000000000000  |
            (Wd.regNum    << 7         ) |
            (Ws.mode      << 4         ) |
            (Ws.Wx.regNum              )
        );
        return this;
    }

    // MOV.D <Ws>, <Wd>   ;   <Ws> must be even   ;   <Wd> must be even
    public PIC16BitEPC $mov_d(final _RWx Ws, final _RWx Wd) throws JXMAsmError
    { return $mov_d( Expr._reg(Ws), Wd ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // MOVPAG #<ulit10> , DSRPAG
    // MOVPAG #<ulit9>  , DSWPAG
    // MOVPAG #<ulit8>  , TBLPAG
    public PIC16BitEPC $movpag(final _Lit val10_val9_val8, final _RTP RTP) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movpag", val10_val9_val8, RTP );

        _cpu.chkSupExtInst( _MSTR, "$movpag" );

        _checkUOffset(_MSTR, val10_val9_val8.value, 0, RTP.ulitLen);
        _putOpCode24(
          // 0b111111101100PPkkkkkkkkkk
             0b111111101100000000000000  |
            (RTP.regNum << 10      )     |
            (val10_val9_val8.value )
        );
        return this;
    }

    // MOVPAG <Ws>, DSRPAG
    // MOVPAG <Ws>, DSWPAG
    // MOVPAG <Ws>, TBLPAG
    public PIC16BitEPC $movpag(final _RWx Ws, final _RTP RTP) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movpag", Ws, RTP );

        _cpu.chkSupExtInst( _MSTR, "$movpag" );

        _putOpCode24(
          // 0b111111101101PP000000ssss
             0b111111101101000000000000  |
            (RTP.regNum << 10 )          |
            (Ws.regNum        )
        );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SL[.B] <f>
    // SL[.B] <f>, WREG
    public PIC16BitEPC $sl  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("sl", 0b110101000000000000000000, false, f, null); }
    public PIC16BitEPC $sl_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("sl", 0b110101000000000000000000, true , f, null); }
    public PIC16BitEPC $sl  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("sl", 0b110101000000000000000000, false, f, WReg); }
    public PIC16BitEPC $sl_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("sl", 0b110101000000000000000000, true , f, WReg); }

    // SL[.B]    <Ws>   ,    <Wd>
    // SL[.B]    <Ws>   , [  <Wd>  ]
    // SL[.B] [  <Ws>  ],    <Wd>
    // SL[.B] [  <Ws>  ], [  <Wd>  ]
    // SL[.B] [  <Ws>++], [  <Wd>++]
    // SL[.B] [  <Ws>--], [  <Wd>--]
    // SL[.B] [++<Ws>  ], [++<Wd>  ]
    // SL[.B] [--<Ws>  ], [--<Wd>  ]
    private PIC16BitEPC $_alu_ws_wd_impl(final String str, final int opcode, final boolean b, final _RWx Ws, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Ws, Wd );

        _putOpCode24(
              // 0b.........Bqqqddddpppssss
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wd.regNum << 7                    ) |
            (Ws.regNum                         )
        );
        return this;
    }

    private PIC16BitEPC $_alu_rwxmem_rwxmem_impl(final String str, final int opcode, final boolean b, final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Ws, Wd );

        if( Ws.immOfs_rwxOfs() || Wd.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$" + str + _obw(b) );

        _putOpCode24(
              // 0b.........Bqqqddddpppssss
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wd.mode      << 11                ) |
            (Wd.Wx.regNum <<  7                ) |
            (Ws.mode      <<  4                ) |
            (Ws.Wx.regNum                      )
        );
        return this;
    }

    public PIC16BitEPC $sl  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "sl", 0b110100000000000000000000, false, Ws           , Wd            ); }
    public PIC16BitEPC $sl_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "sl", 0b110100000000000000000000, true , Ws           , Wd            ); }
    public PIC16BitEPC $sl  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "sl", 0b110100000000000000000000, false, Expr._reg(Ws), Wd            ); }
    public PIC16BitEPC $sl_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "sl", 0b110100000000000000000000, true , Expr._reg(Ws), Wd            ); }
    public PIC16BitEPC $sl  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "sl", 0b110100000000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $sl_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "sl", 0b110100000000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $sl  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "sl", 0b110100000000000000000000, false, Ws           , Wd            ); }
    public PIC16BitEPC $sl_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "sl", 0b110100000000000000000000, true , Ws           , Wd            ); }

    // SL <Ws>, #<ulit4>, <Wd>
    private PIC16BitEPC $_alu_ws_ulit4_wd_impl(final String str, final int opcode, final _RWx Ws, final _Lit ulit4, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str, Ws, ulit4, Wd );

        _checkUOffset(_MSTR, ulit4.value, 0, 4);
        _putOpCode24(
          // 0b.........wwwwdddd...kkkk
             opcode           |
            (Ws.regNum << 11) |
            (Wd.regNum <<  7) |
            ulit4.value
        );
        return this;
    }

    public PIC16BitEPC $sl(final _RWx Ws, final _Lit shift4, final _RWx Wd) throws JXMAsmError
    { return $_alu_ws_ulit4_wd_impl("sl", 0b110111010000000001000000, Ws, shift4, Wd); }

    // SL <Ws>, <Wn>, <Wd>
    private PIC16BitEPC $_alu_ws_wn_wd_impl(final String str, final int opcode, final _RWx Ws, final _RWx Wn, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str, Ws, Wn, Wd );

        _putOpCode24(
          // 0b.........wwwwdddd...ssss
             opcode           |
            (Ws.regNum << 11) |
            (Wd.regNum <<  7) |
            (Wn.regNum      )
        );
        return this;
    }

    public PIC16BitEPC $sl(final _RWx Ws, final _RWx Wn, final _RWx Wd) throws JXMAsmError
    { return $_alu_ws_wn_wd_impl("sl", 0b110111010000000000000000, Ws, Wn, Wd); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // LSR[.B] <f>
    // LSR[.B] <f>, WREG
    public PIC16BitEPC $lsr  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("lsr", 0b110101010000000000000000, false, f, null); }
    public PIC16BitEPC $lsr_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("lsr", 0b110101010000000000000000, true , f, null); }
    public PIC16BitEPC $lsr  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("lsr", 0b110101010000000000000000, false, f, WReg); }
    public PIC16BitEPC $lsr_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("lsr", 0b110101010000000000000000, true , f, WReg); }

    // LSR[.B]    <Ws>   ,    <Wd>
    // LSR[.B]    <Ws>   , [  <Wd>  ]
    // LSR[.B] [  <Ws>  ],    <Wd>
    // LSR[.B] [  <Ws>  ], [  <Wd>  ]
    // LSR[.B] [  <Ws>++], [  <Wd>++]
    // LSR[.B] [  <Ws>--], [  <Wd>--]
    // LSR[.B] [++<Ws>  ], [++<Wd>  ]
    // LSR[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $lsr  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "lsr", 0b110100010000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $lsr_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "lsr", 0b110100010000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $lsr  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "lsr", 0b110100010000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $lsr_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "lsr", 0b110100010000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $lsr  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "lsr", 0b110100010000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $lsr_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "lsr", 0b110100010000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $lsr  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "lsr", 0b110100010000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $lsr_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "lsr", 0b110100010000000000000000, true , Ws           ,           Wd  ); }

    // LSR <Ws>, #<ulit4>, <Wd>
    public PIC16BitEPC $lsr(final _RWx Ws, final _Lit shift4, final _RWx Wd) throws JXMAsmError
    { return $_alu_ws_ulit4_wd_impl("lsr", 0b110111100000000001000000, Ws, shift4, Wd); }

    // LSR <Ws>, <Wn>, <Wd>
    public PIC16BitEPC $lsr(final _RWx Ws, final _RWx Wn, final _RWx Wd) throws JXMAsmError
    { return $_alu_ws_wn_wd_impl("lsr", 0b110111100000000000000000, Ws, Wn, Wd); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ASR[.B] <f>
    // ASR[.B] <f>, WREG
    public PIC16BitEPC $asr  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("asr", 0b110101011000000000000000, false, f, null); }
    public PIC16BitEPC $asr_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("asr", 0b110101011000000000000000, true , f, null); }
    public PIC16BitEPC $asr  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("asr", 0b110101011000000000000000, false, f, WReg); }
    public PIC16BitEPC $asr_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("asr", 0b110101011000000000000000, true , f, WReg); }

    // ASR[.B]    <Ws>   ,    <Wd>
    // ASR[.B]    <Ws>   , [  <Wd>  ]
    // ASR[.B] [  <Ws>  ],    <Wd>
    // ASR[.B] [  <Ws>  ], [  <Wd>  ]
    // ASR[.B] [  <Ws>++], [  <Wd>++]
    // ASR[.B] [  <Ws>--], [  <Wd>--]
    // ASR[.B] [++<Ws>  ], [++<Wd>  ]
    // ASR[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $asr  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "asr", 0b110100011000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $asr_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "asr", 0b110100011000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $asr  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "asr", 0b110100011000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $asr_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "asr", 0b110100011000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $asr  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "asr", 0b110100011000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $asr_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "asr", 0b110100011000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $asr  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "asr", 0b110100011000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $asr_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "asr", 0b110100011000000000000000, true , Ws           ,           Wd  ); }

    // ASR <Ws>, #<ulit4>, <Wd>
    public PIC16BitEPC $asr(final _RWx Ws, final _Lit shift4, final _RWx Wd) throws JXMAsmError
    { return $_alu_ws_ulit4_wd_impl("asr", 0b110111101000000001000000, Ws, shift4, Wd); }

    // ASR <Ws>, <Wn>, <Wd>
    public PIC16BitEPC $asr(final _RWx Ws, final _RWx Wn, final _RWx Wd) throws JXMAsmError
    { return $_alu_ws_wn_wd_impl("asr", 0b110111101000000000000000, Ws, Wn, Wd); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SFTAC <Acc>, #<slit6>
    public PIC16BitEPC $sftac(final _RAcc RAcc, final _Lit shift6) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sftac", RAcc, shift6 );

        _cpu.chkSupInstGroup_stdDSP( _MSTR, "$sftac" );

        if(shift6.value < -16 || shift6.value > 16) _errorAddressOffsetValueOutOfRange(_MSTR, shift6.value);
        _putOpCode24(
         // 0b11001000A000000001kkkkkk
            0b110010000000000001000000 |
            (RAcc.regNum << 15       ) |
            (shift6.value & 0x3F     )
        );
        return this;
    }

    // SFTAC <Acc>, <Wb>
    public PIC16BitEPC $sftac(final _RAcc RAcc, final _RWx Wb) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sftac", Wb );

        _cpu.chkSupInstGroup_stdDSP( _MSTR, "$sftac" );

        _putOpCode24(
         // 0b11001000A00000000000ssss
            0b110010000000000000000000 |
            (RAcc.regNum << 15       ) |
            (Wb.regNum               )
        );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // RLNC[.B] <f>
    // RLNC[.B] <f>, WREG
    public PIC16BitEPC $rlnc  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("rlnc", 0b110101100000000000000000, false, f, null); }
    public PIC16BitEPC $rlnc_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("rlnc", 0b110101100000000000000000, true , f, null); }
    public PIC16BitEPC $rlnc  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("rlnc", 0b110101100000000000000000, false, f, WReg); }
    public PIC16BitEPC $rlnc_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("rlnc", 0b110101100000000000000000, true , f, WReg); }

    // RLNC[.B]    <Ws>   ,    <Wd>
    // RLNC[.B]    <Ws>   , [  <Wd>  ]
    // RLNC[.B] [  <Ws>  ],    <Wd>
    // RLNC[.B] [  <Ws>  ], [  <Wd>  ]
    // RLNC[.B] [  <Ws>++], [  <Wd>++]
    // RLNC[.B] [  <Ws>--], [  <Wd>--]
    // RLNC[.B] [++<Ws>  ], [++<Wd>  ]
    // RLNC[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $rlnc  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "rlnc", 0b110100100000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $rlnc_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "rlnc", 0b110100100000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $rlnc  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlnc", 0b110100100000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $rlnc_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlnc", 0b110100100000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $rlnc  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlnc", 0b110100100000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $rlnc_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlnc", 0b110100100000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $rlnc  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlnc", 0b110100100000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $rlnc_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlnc", 0b110100100000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // RLC[.B] <f>
    // RLC[.B] <f>, WREG
    public PIC16BitEPC $rlc  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("rlc", 0b110101101000000000000000, false, f, null); }
    public PIC16BitEPC $rlc_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("rlc", 0b110101101000000000000000, true , f, null); }
    public PIC16BitEPC $rlc  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("rlc", 0b110101101000000000000000, false, f, WReg); }
    public PIC16BitEPC $rlc_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("rlc", 0b110101101000000000000000, true , f, WReg); }

    // RLC[.B]    <Ws>   ,    <Wd>
    // RLC[.B]    <Ws>   , [  <Wd>  ]
    // RLC[.B] [  <Ws>  ],    <Wd>
    // RLC[.B] [  <Ws>  ], [  <Wd>  ]
    // RLC[.B] [  <Ws>++], [  <Wd>++]
    // RLC[.B] [  <Ws>--], [  <Wd>--]
    // RLC[.B] [++<Ws>  ], [++<Wd>  ]
    // RLC[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $rlc  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "rlc", 0b110100101000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $rlc_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "rlc", 0b110100101000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $rlc  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlc", 0b110100101000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $rlc_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlc", 0b110100101000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $rlc  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlc", 0b110100101000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $rlc_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlc", 0b110100101000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $rlc  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlc", 0b110100101000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $rlc_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rlc", 0b110100101000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // RRNC[.B] <f>
    // RRNC[.B] <f>, WREG
    public PIC16BitEPC $rrnc  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("rrnc", 0b110101110000000000000000, false, f, null); }
    public PIC16BitEPC $rrnc_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("rrnc", 0b110101110000000000000000, true , f, null); }
    public PIC16BitEPC $rrnc  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("rrnc", 0b110101110000000000000000, false, f, WReg); }
    public PIC16BitEPC $rrnc_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("rrnc", 0b110101110000000000000000, true , f, WReg); }

    // RRNC[.B]    <Ws>   ,    <Wd>
    // RRNC[.B]    <Ws>   , [  <Wd>  ]
    // RRNC[.B] [  <Ws>  ],    <Wd>
    // RRNC[.B] [  <Ws>  ], [  <Wd>  ]
    // RRNC[.B] [  <Ws>++], [  <Wd>++]
    // RRNC[.B] [  <Ws>--], [  <Wd>--]
    // RRNC[.B] [++<Ws>  ], [++<Wd>  ]
    // RRNC[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $rrnc  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "rrnc", 0b110100110000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $rrnc_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "rrnc", 0b110100110000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $rrnc  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrnc", 0b110100110000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $rrnc_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrnc", 0b110100110000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $rrnc  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrnc", 0b110100110000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $rrnc_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrnc", 0b110100110000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $rrnc  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrnc", 0b110100110000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $rrnc_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrnc", 0b110100110000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // RRC[.B] <f>
    // RRC[.B] <f>, WREG
    public PIC16BitEPC $rrc  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("rrc", 0b110101111000000000000000, false, f, null); }
    public PIC16BitEPC $rrc_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("rrc", 0b110101111000000000000000, true , f, null); }
    public PIC16BitEPC $rrc  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("rrc", 0b110101111000000000000000, false, f, WReg); }
    public PIC16BitEPC $rrc_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("rrc", 0b110101111000000000000000, true , f, WReg); }

    // RRC[.B]    <Ws>   ,    <Wd>
    // RRC[.B]    <Ws>   , [  <Wd>  ]
    // RRC[.B] [  <Ws>  ],    <Wd>
    // RRC[.B] [  <Ws>  ], [  <Wd>  ]
    // RRC[.B] [  <Ws>++], [  <Wd>++]
    // RRC[.B] [  <Ws>--], [  <Wd>--]
    // RRC[.B] [++<Ws>  ], [++<Wd>  ]
    // RRC[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $rrc  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "rrc", 0b110100111000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $rrc_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "rrc", 0b110100111000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $rrc  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrc", 0b110100111000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $rrc_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrc", 0b110100111000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $rrc  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrc", 0b110100111000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $rrc_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrc", 0b110100111000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $rrc  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrc", 0b110100111000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $rrc_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "rrc", 0b110100111000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private PIC16BitEPC $_div_wm_wd_impl(final String str, final int opcode, final boolean W, final _RWx Wm, final _RWx Wn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$div_" + str, Wm, Wn );

        if( !W && Wm.oddReg() ) _errorInvalidRegisterNAI(_MSTR, Wm);
        if( !Wn.anyRegGTE2() ) _errorInvalidRegisterNAI(_MSTR, Wn);

        _putOpCode24(
                   // 0b.........ttttvvvvW..ssss
                      opcode                       |
            ( W ? 0 : 0b000000000000000001000000 ) |
            ( W ? 0 : ( (Wm.regNum + 1) << 11 )  ) |
            (            Wm.regNum      <<  7    ) |
            (            Wn.regNum               )
        );
        return this;
    }

    // DIV.S  <Wm>, <Wn>
    // DIV.SD <Wm>, <Wn>
    // ERROR: str="sw" causes helper $_div_wm_wd_impl to call _MNEMONIC_STRING("$div_sw", ...), but method name is "$div_s". Fix: change the str argument from "sw" to "s".
    public PIC16BitEPC $div_s (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $_div_wm_wd_impl("sw", 0b110110000000000000000000, true , Wm, Wn); }
    public PIC16BitEPC $div_sd(final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $_div_wm_wd_impl("sd", 0b110110000000000000000000, false, Wm, Wn); }

    // DIV.U  <Wm>, <Wn>
    // DIV.UD <Wm>, <Wn>
    // ERROR: str="uw" causes helper $_div_wm_wd_impl to call _MNEMONIC_STRING("$div_uw", ...), but method name is "$div_u". Fix: change the str argument from "uw" to "u".
    public PIC16BitEPC $div_u (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $_div_wm_wd_impl("uw", 0b110110001000000000000000, true , Wm, Wn); }
    public PIC16BitEPC $div_ud(final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $_div_wm_wd_impl("ud", 0b110110001000000000000000, false, Wm, Wn); }

    // DIVF <Wm>, <Wn>
    public PIC16BitEPC $divf(final _RWx Wm, final _RWx Wn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$divf", Wm, Wn );

        _cpu.chkSupInst( _MSTR, "$divf" );
        if( !Wn.anyRegGTE2() ) _errorInvalidRegisterNAI(_MSTR, Wn);

        _putOpCode24(
          // 0b110110010tttt0000000ssss
             0b110110010000000000000000  |
            (Wm.regNum << 11           ) |
            (Wn.regNum                 )
        );
        return this;
    }

    // [PI] : REPEAT #N ; DIV.S  <Wm>, <Wn>
    // [PI] : REPEAT #N ; DIV.SD <Wm>, <Wn>
    // [PI] : REPEAT #N ; DIV.U  <Wm>, <Wn>
    // [PI] : REPEAT #N ; DIV.UD <Wm>, <Wn>
    // [PI] : REPEAT #N ; DIVF   <Wm>, <Wn>
    public PIC16BitEPC $xdiv_s (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$div_s (Wm, Wn); }
    public PIC16BitEPC $xdiv_sd(final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$div_sd(Wm, Wn); }
    public PIC16BitEPC $xdiv_u (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$div_u (Wm, Wn); }
    public PIC16BitEPC $xdiv_ud(final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$div_ud(Wm, Wn); }
    public PIC16BitEPC $xdivf  (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$divf  (Wm, Wn); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private PIC16BitEPC $_div2_wm_wd_impl(final String str, final int opcode, final boolean W, final _RWx Wm, final _RWx Wn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$div2_" + str, Wm, Wn );

        _cpu.chkSupInst( _MSTR, "div2", "$div2_" + str );
        if( !W && Wm.oddReg() ) _errorInvalidRegisterNAI(_MSTR, Wm);
        if( !Wn.anyRegGTE2() ) _errorInvalidRegisterNAI(_MSTR, Wn);

        _putOpCode24(
                   // 0b.........ttttvvvvW..ssss
                      opcode                       |
            ( W ? 0 : 0b000000000000000001000000 ) |
            ( W ? 0 : ( (Wm.regNum + 1) << 11 )  ) |
            (            Wm.regNum      <<  7    ) |
            (            Wn.regNum               )
        );
        return this;
    }

    // DIV2.S  <Wm>, <Wn>
    // DIV2.SD <Wm>, <Wn>
    // ERROR: str="sw" causes helper $_div2_wm_wd_impl to call _MNEMONIC_STRING("$div2_sw", ...), but method name is "$div2_s". Fix: change the str argument from "sw" to "s".
    public PIC16BitEPC $div2_s (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $_div2_wm_wd_impl("sw", 0b110110000000000000100000, true , Wm, Wn); }
    public PIC16BitEPC $div2_sd(final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $_div2_wm_wd_impl("sd", 0b110110000000000000100000, false, Wm, Wn); }

    // DIV2.U  <Wm>, <Wn>
    // DIV2.UD <Wm>, <Wn>
    // ERROR: str="uw" causes helper $_div2_wm_wd_impl to call _MNEMONIC_STRING("$div2_uw", ...), but method name is "$div2_u". Fix: change the str argument from "uw" to "u".
    public PIC16BitEPC $div2_u (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $_div2_wm_wd_impl("uw", 0b110110001000000000100000, true , Wm, Wn); }
    public PIC16BitEPC $div2_ud(final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $_div2_wm_wd_impl("ud", 0b110110001000000000100000, false, Wm, Wn); }

    // DIVF2 <Wm>, <Wn>
    public PIC16BitEPC $divf2(final _RWx Wm, final _RWx Wn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$divf2", Wm, Wn );

        _cpu.chkSupInst( _MSTR, "$divf2" );
        if( !Wm.anyRegGTE2() ) _errorInvalidRegisterNAI(_MSTR, Wn);
        if( !Wn.anyRegGTE2() ) _errorInvalidRegisterNAI(_MSTR, Wn);

        _putOpCode24(
          // 0b110110010tttt0000010ssss
             0b110110010000000000100000  |
            (Wm.regNum << 11           ) |
            (Wn.regNum                 )
        );
        return this;
    }

    // [PI] : REPEAT #N ; DIV2.S  <Wm>, <Wn>
    // [PI] : REPEAT #N ; DIV2.SD <Wm>, <Wn>
    // [PI] : REPEAT #N ; DIV2.U  <Wm>, <Wn>
    // [PI] : REPEAT #N ; DIV2.UD <Wm>, <Wn>
    // [PI] : REPEAT #N ; DIVF2   <Wm>, <Wn>
    public PIC16BitEPC $xdiv2_s (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$div2_s (Wm, Wn); }
    public PIC16BitEPC $xdiv2_sd(final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$div2_sd(Wm, Wn); }
    public PIC16BitEPC $xdiv2_u (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$div2_u (Wm, Wn); }
    public PIC16BitEPC $xdiv2_ud(final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$div2_ud(Wm, Wn); }
    public PIC16BitEPC $xdivf2  (final _RWx Wm, final _RWx Wn) throws JXMAsmError { return $repeat( _cpu.getDivRep() ).$divf2  (Wm, Wn); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // FBCL    <Ws>   , <Wd>
    // FBCL [  <Ws>  ], <Wd>
    // FBCL [  <Ws>++], <Wd>
    // FBCL [  <Ws>--], <Wd>
    // FBCL [++<Ws>  ], <Wd>
    // FBCL [--<Ws>  ], <Wd>
    public PIC16BitEPC $fbcl(_RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "fbcl", 0b110111110000000000000000, false, Expr._reg(Ws), Expr._reg(Wd) ); }
    public PIC16BitEPC $fbcl(_RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "fbcl", 0b110111110000000000000000, false,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // FF1L    <Ws>   , <Wd>
    // FF1L [  <Ws>  ], <Wd>
    // FF1L [  <Ws>++], <Wd>
    // FF1L [  <Ws>--], <Wd>
    // FF1L [++<Ws>  ], <Wd>
    // FF1L [--<Ws>  ], <Wd>
    public PIC16BitEPC $ff1l(_RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "ff1l", 0b110011111000000000000000, false, Expr._reg(Ws), Expr._reg(Wd) ); }
    public PIC16BitEPC $ff1l(_RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "ff1l", 0b110011111000000000000000, false,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // FF1R    <Ws>   , <Wd>
    // FF1R [  <Ws>  ], <Wd>
    // FF1R [  <Ws>++], <Wd>
    // FF1R [  <Ws>--], <Wd>
    // FF1R [++<Ws>  ], <Wd>
    // FF1R [--<Ws>  ], <Wd>
    public PIC16BitEPC $ff1r(_RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "ff1r", 0b110011110000000000000000, false, Expr._reg(Ws), Expr._reg(Wd) ); }
    public PIC16BitEPC $ff1r(_RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "ff1r", 0b110011110000000000000000, false,           Ws , Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : FLIM   * (dsPIC33C) ??? #####
    // ##### ??? TODO : FLIM.V * (dsPIC33C) ??? #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // CP[.B] <f>
    private PIC16BitEPC $_cp_f_impl(final String str, final int opcode, final boolean b, final int f) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), f );

        if(!b) _errorIfAddressOffsetNotAligned2B(_MSTR, f);

        _checkUOffset(_MSTR, f, 0, 13);
        _putOpCode24(
              // 0b.........B.fffffffffffff
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (f                                 )
        );
        return this;
    }

    public PIC16BitEPC $cp  (final int f) throws JXMAsmError { return $_cp_f_impl("cp", 0b111000110000000000000000, false, f); }
    public PIC16BitEPC $cp_b(final int f) throws JXMAsmError { return $_cp_f_impl("cp", 0b111000110000000000000000, true , f); }

    // CP[.B] <Wb>, #<ulit5/ulit8>
    private PIC16BitEPC $_cp_ulit5or8_impl(final String str, final int opcode, final boolean b, final _RWx Wb, final _Lit ulit5_ulit8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Wb, ulit5_ulit8 );

        if( _cpu.extendedInstructionSet()  ) {
            // NOTE : The instruction is extended in PIC24E, dsPIC33E, and dsPIC33C MCUs
            _checkUOffset(_MSTR, ulit5_ulit8.value, 0, 8);
            _putOpCode24(
                  // 0b.........wwwwBkkk..kkkkk
                     opcode                          |
                (b ? 0b000000000000010000000000 : 0) |
                (Wb.regNum << 11                   ) |
                ( (ulit5_ulit8.value >>> 5) << 7   ) |
                (  ulit5_ulit8.value & 0b00011111  )
            );
        }
        else {
            _checkUOffset(_MSTR, ulit5_ulit8.value, 0, 5);
            _putOpCode24(
                  // 0b.........wwwwB.....kkkkk
                     opcode                          |
                (b ? 0b000000000000010000000000 : 0) |
                (Wb.regNum << 11                   ) |
                (ulit5_ulit8.value                 )
            );
        }
        return this;
    }

    public PIC16BitEPC $cp  (final _RWx Wb, final _Lit value5_value8) throws JXMAsmError { return $_cp_ulit5or8_impl("cp", 0b111000010000000001100000, false, Wb, value5_value8); }
    public PIC16BitEPC $cp_b(final _RWx Wb, final _Lit value5_value8) throws JXMAsmError { return $_cp_ulit5or8_impl("cp", 0b111000010000000001100000, true , Wb, value5_value8); }

    // CP[.B] <Wb>,    <Ws>
    // CP[.B] <Wb>, [  <Ws>  ]
    // CP[.B] <Wb>, [  <Ws>++]
    // CP[.B] <Wb>, [  <Ws>--]
    // CP[.B] <Wb>, [++<Ws>  ]
    // CP[.B] <Wb>, [--<Ws>  ]
    private PIC16BitEPC $_cp_wb_rwxmem(final String str, final int opcode, final boolean b, final _RWx Wb, final _RWxMem Ws) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Wb, Ws );

         if( Ws.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$" + str + _obw(b) );

        _putOpCode24(
              // 0b.........wwwwB...pppssss
                 opcode                          |
            (b ? 0b000000000000010000000000 : 0) |
            (Wb.regNum << 11                   ) |
            (Ws.mode   <<  4                   ) |
            (Ws.Wx.regNum                      )
        );
        return this;
    }

    public PIC16BitEPC $cp  (final _RWx Wb, final _RWxMem Ws) throws JXMAsmError { return $_cp_wb_rwxmem( "cp", 0b111000010000000000000000, false, Wb,           Ws  ); }
    public PIC16BitEPC $cp_b(final _RWx Wb, final _RWxMem Ws) throws JXMAsmError { return $_cp_wb_rwxmem( "cp", 0b111000010000000000000000, true , Wb,           Ws  ); }
    public PIC16BitEPC $cp  (final _RWx Wb, final _RWx    Ws) throws JXMAsmError { return $_cp_wb_rwxmem( "cp", 0b111000010000000000000000, false, Wb, Expr._reg(Ws) ); }
    public PIC16BitEPC $cp_b(final _RWx Wb, final _RWx    Ws) throws JXMAsmError { return $_cp_wb_rwxmem( "cp", 0b111000010000000000000000, true , Wb, Expr._reg(Ws) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // CP0[.B] <f>
    public PIC16BitEPC $cp0  (final int f) throws JXMAsmError { return $_cp_f_impl("cp0", 0b111000100000000000000000, false, f); }
    public PIC16BitEPC $cp0_b(final int f) throws JXMAsmError { return $_cp_f_impl("cp0", 0b111000100000000000000000, true , f); }

    // CP0[.B]    <Ws>
    // CP0[.B] [  <Ws>  ]
    // CP0[.B] [  <Ws>++]
    // CP0[.B] [  <Ws>--]
    // CP0[.B] [++<Ws>  ]
    // CP0[.B] [--<Ws>  ]
    private PIC16BitEPC $_cp0_impl(final boolean b, final _RWxMem Ws) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cp0" + _obw(b), Ws );

         if( Ws.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$cp0" + _obw(b) );

        _putOpCode24(
              // 0b.........0000B...pppssss
                 0b111000000000000000000000      |
            (b ? 0b000000000000010000000000 : 0) |
            (Ws.mode   <<  4                   ) |
            (Ws.Wx.regNum                      )
        );
        return this;
    }

    public PIC16BitEPC $cp0  (final _RWxMem Ws) throws JXMAsmError { return $_cp0_impl( false,           Ws  ); }
    public PIC16BitEPC $cp0_b(final _RWxMem Ws) throws JXMAsmError { return $_cp0_impl( true ,           Ws  ); }
    public PIC16BitEPC $cp0  (final _RWx    Ws) throws JXMAsmError { return $_cp0_impl( false, Expr._reg(Ws) ); }
    public PIC16BitEPC $cp0_b(final _RWx    Ws) throws JXMAsmError { return $_cp0_impl( true , Expr._reg(Ws) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // CPB[.B] <f>
    public PIC16BitEPC $cpb  (final int f) throws JXMAsmError { return $_cp_f_impl("cpb", 0b111000111000000000000000, false, f); }
    public PIC16BitEPC $cpb_b(final int f) throws JXMAsmError { return $_cp_f_impl("cpb", 0b111000111000000000000000, true , f); }

    // CPB[.B] <Wb>, #<ulit5>
    public PIC16BitEPC $cpb  (final _RWx Wb, final _Lit value5_value8) throws JXMAsmError { return $_cp_ulit5or8_impl("cpb", 0b111000011000000001100000, false, Wb, value5_value8); }
    public PIC16BitEPC $cpb_b(final _RWx Wb, final _Lit value5_value8) throws JXMAsmError { return $_cp_ulit5or8_impl("cpb", 0b111000011000000001100000, true , Wb, value5_value8); }

    // CPB[.B] <Wb>,    <Ws>
    // CPB[.B] <Wb>, [  <Ws>  ]
    // CPB[.B] <Wb>, [  <Ws>++]
    // CPB[.B] <Wb>, [  <Ws>--]
    // CPB[.B] <Wb>, [++<Ws>  ]
    // CPB[.B] <Wb>, [--<Ws>  ]
    public PIC16BitEPC $cpb  (final _RWx Wb, final _RWxMem Ws) throws JXMAsmError { return $_cp_wb_rwxmem( "cpb", 0b111000011000000000000000, false, Wb,           Ws  ); }
    public PIC16BitEPC $cpb_b(final _RWx Wb, final _RWxMem Ws) throws JXMAsmError { return $_cp_wb_rwxmem( "cpb", 0b111000011000000000000000, true , Wb,           Ws  ); }
    public PIC16BitEPC $cpb  (final _RWx Wb, final _RWx    Ws) throws JXMAsmError { return $_cp_wb_rwxmem( "cpb", 0b111000011000000000000000, false, Wb, Expr._reg(Ws) ); }
    public PIC16BitEPC $cpb_b(final _RWx Wb, final _RWx    Ws) throws JXMAsmError { return $_cp_wb_rwxmem( "cpb", 0b111000011000000000000000, true , Wb, Expr._reg(Ws) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // CPSEQ[.B] <Wb>, <Ws>
    private PIC16BitEPC $_cpsxx_wb_ws(final String str, final int opcode, final boolean b, final _RWx Wb, final _RWx Ws) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cps" + str + _obw(b), Wb, Ws );

        try {
            _cpu.chkSupInstGroup_cpsXXX( _MSTR, "$cps" + str + _obw(b) );
        }
        catch(final Exception e) {
            // NOTE : In PIC24E, dsPIC33E, and dsPIC33C MCUs, the 'CPSxx[.B]' instructions are extended to 'CPBxx[.B]'
            if( !_cpu.extendedInstructionSet() ) throw e;
            return $_cpbxx_wb_ws_label(str, opcode, b, Wb, Ws, "+2");
        }

        _putOpCode24(
              // 0b.........wwwwB...000ssss
                 opcode                          |
            (b ? 0b000000000000010000000000 : 0) |
            (Wb.regNum << 11                   ) |
            (Ws.regNum                         )
        );
        return this;
    }

    public PIC16BitEPC $cpseq  (final _RWx Wb, final _RWx Ws) throws JXMAsmError { return $_cpsxx_wb_ws("eq", 0b111001111000000000000000, false, Wb, Ws); }
    public PIC16BitEPC $cpseq_b(final _RWx Wb, final _RWx Ws) throws JXMAsmError { return $_cpsxx_wb_ws("eq", 0b111001111000000000000000, true , Wb, Ws); }

    // CPSNE[.B] <Wb>, <Ws>
    public PIC16BitEPC $cpsne  (final _RWx Wb, final _RWx Ws) throws JXMAsmError { return $_cpsxx_wb_ws("ne", 0b111001110000000000000000, false, Wb, Ws); }
    public PIC16BitEPC $cpsne_b(final _RWx Wb, final _RWx Ws) throws JXMAsmError { return $_cpsxx_wb_ws("ne", 0b111001110000000000000000, true , Wb, Ws); }

    // CPSLT[.B] <Wb>, <Ws>
    public PIC16BitEPC $cpslt  (final _RWx Wb, final _RWx Ws) throws JXMAsmError { return $_cpsxx_wb_ws("lt", 0b111001101000000000000000, false, Wb, Ws); }
    public PIC16BitEPC $cpslt_b(final _RWx Wb, final _RWx Ws) throws JXMAsmError { return $_cpsxx_wb_ws("lt", 0b111001101000000000000000, true , Wb, Ws); }

    // CPSGT[.B] <Wb>, <Ws>
    public PIC16BitEPC $cpsgt  (final _RWx Wb, final _RWx Ws) throws JXMAsmError { return $_cpsxx_wb_ws("gt", 0b111001100000000000000000, false, Wb, Ws); }
    public PIC16BitEPC $cpsgt_b(final _RWx Wb, final _RWx Ws) throws JXMAsmError { return $_cpsxx_wb_ws("gt", 0b111001100000000000000000, true , Wb, Ws); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // CPBEQ[.B] <Wb>, <Ws>, <label>
    private PIC16BitEPC $_cpbxx_wb_ws_label(final String str, final int opcode, final boolean b, final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cpb" + str + _obw(b), Wb, Ws, label );

        _cpu.chkSupInstGroup_cpbXXX( _MSTR, "$cpb" + str + _obw(b) );

        _putOpCode24( _MSTR,
              // 0b.........wwwwBnnnnnnssss
                 opcode                          |
            (b ? 0b000000000000010000000000 : 0) |
            (Wb.regNum << 11                   ) |
            (Ws.regNum                         )
        , label, _RA.sig(6), _RA.vs2(1), 4 );
        return this;
    }

    public PIC16BitEPC $cpbeq  (final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError { return $_cpbxx_wb_ws_label("eq", 0b111001111000000000000000, false, Wb, Ws, label); }
    public PIC16BitEPC $cpbeq_b(final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError { return $_cpbxx_wb_ws_label("eq", 0b111001111000000000000000, true , Wb, Ws, label); }

    // CPSNE[.B] <Wb>, <Ws>
    public PIC16BitEPC $cpbne  (final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError { return $_cpbxx_wb_ws_label("ne", 0b111001110000000000000000, false, Wb, Ws, label); }
    public PIC16BitEPC $cpbne_b(final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError { return $_cpbxx_wb_ws_label("ne", 0b111001110000000000000000, true , Wb, Ws, label); }

    // CPSLT[.B] <Wb>, <Ws>
    public PIC16BitEPC $cpblt  (final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError { return $_cpbxx_wb_ws_label("lt", 0b111001101000000000000000, false, Wb, Ws, label); }
    public PIC16BitEPC $cpblt_b(final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError { return $_cpbxx_wb_ws_label("lt", 0b111001101000000000000000, true , Wb, Ws, label); }

    // CPSGT[.B] <Wb>, <Ws>
    public PIC16BitEPC $cpbgt  (final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError { return $_cpbxx_wb_ws_label("gt", 0b111001100000000000000000, false, Wb, Ws, label); }
    public PIC16BitEPC $cpbgt_b(final _RWx Wb, final _RWx Ws, final String label) throws JXMAsmError { return $_cpbxx_wb_ws_label("gt", 0b111001100000000000000000, true , Wb, Ws, label); }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    // INC[.B] <f>
    // INC[.B] <f>, WREG
    public PIC16BitEPC $inc  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("inc", 0b111011000000000000000000, false, f, null); }
    public PIC16BitEPC $inc_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("inc", 0b111011000000000000000000, true , f, null); }
    public PIC16BitEPC $inc  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("inc", 0b111011000000000000000000, false, f, WReg); }
    public PIC16BitEPC $inc_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("inc", 0b111011000000000000000000, true , f, WReg); }

    // INC[.B]    <Ws>   ,    <Wd>
    // INC[.B]    <Ws>   , [  <Wd>  ]
    // INC[.B] [  <Ws>  ],    <Wd>
    // INC[.B] [  <Ws>  ], [  <Wd>  ]
    // INC[.B] [  <Ws>++], [  <Wd>++]
    // INC[.B] [  <Ws>--], [  <Wd>--]
    // INC[.B] [++<Ws>  ], [++<Wd>  ]
    // INC[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $inc  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "inc", 0b111010000000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $inc_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "inc", 0b111010000000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $inc  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc", 0b111010000000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $inc_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc", 0b111010000000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $inc  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc", 0b111010000000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $inc_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc", 0b111010000000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $inc  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc", 0b111010000000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $inc_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc", 0b111010000000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // INC2[.B] <f>
    // INC2[.B] <f>, WREG
    public PIC16BitEPC $inc2  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("inc2", 0b111011001000000000000000, false, f, null); }
    public PIC16BitEPC $inc2_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("inc2", 0b111011001000000000000000, true , f, null); }
    public PIC16BitEPC $inc2  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("inc2", 0b111011001000000000000000, false, f, WReg); }
    public PIC16BitEPC $inc2_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("inc2", 0b111011001000000000000000, true , f, WReg); }

    // INC2[.B]    <Ws>   ,    <Wd>
    // INC2[.B]    <Ws>   , [  <Wd>  ]
    // INC2[.B] [  <Ws>  ],    <Wd>
    // INC2[.B] [  <Ws>  ], [  <Wd>  ]
    // INC2[.B] [  <Ws>++], [  <Wd>++]
    // INC2[.B] [  <Ws>--], [  <Wd>--]
    // INC2[.B] [++<Ws>  ], [++<Wd>  ]
    // INC2[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $inc2  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "inc2", 0b111010001000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $inc2_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "inc2", 0b111010001000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $inc2  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc2", 0b111010001000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $inc2_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc2", 0b111010001000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $inc2  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc2", 0b111010001000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $inc2_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc2", 0b111010001000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $inc2  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc2", 0b111010001000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $inc2_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "inc2", 0b111010001000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // DEC[.B] <f>
    // DEC[.B] <f>, WREG
    public PIC16BitEPC $dec  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("dec", 0b111011010000000000000000, false, f, null); }
    public PIC16BitEPC $dec_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("dec", 0b111011010000000000000000, true , f, null); }
    public PIC16BitEPC $dec  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("dec", 0b111011010000000000000000, false, f, WReg); }
    public PIC16BitEPC $dec_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("dec", 0b111011010000000000000000, true , f, WReg); }

    // DEC[.B]    <Ws>   ,    <Wd>
    // DEC[.B]    <Ws>   , [  <Wd>  ]
    // DEC[.B] [  <Ws>  ],    <Wd>
    // DEC[.B] [  <Ws>  ], [  <Wd>  ]
    // DEC[.B] [  <Ws>++], [  <Wd>++]
    // DEC[.B] [  <Ws>--], [  <Wd>--]
    // DEC[.B] [++<Ws>  ], [++<Wd>  ]
    // DEC[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $dec  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "dec", 0b111010010000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $dec_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "dec", 0b111010010000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $dec  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec", 0b111010010000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $dec_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec", 0b111010010000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $dec  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec", 0b111010010000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $dec_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec", 0b111010010000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $dec  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec", 0b111010010000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $dec_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec", 0b111010010000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // DEC2[.B] <f>
    // DEC2[.B] <f>, WREG
    public PIC16BitEPC $dec2  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("dec2", 0b111011011000000000000000, false, f, null); }
    public PIC16BitEPC $dec2_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("dec2", 0b111011011000000000000000, true , f, null); }
    public PIC16BitEPC $dec2  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("dec2", 0b111011011000000000000000, false, f, WReg); }
    public PIC16BitEPC $dec2_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("dec2", 0b111011011000000000000000, true , f, WReg); }

    // DEC2[.B]    <Ws>   ,    <Wd>
    // DEC2[.B]    <Ws>   , [  <Wd>  ]
    // DEC2[.B] [  <Ws>  ],    <Wd>
    // DEC2[.B] [  <Ws>  ], [  <Wd>  ]
    // DEC2[.B] [  <Ws>++], [  <Wd>++]
    // DEC2[.B] [  <Ws>--], [  <Wd>--]
    // DEC2[.B] [++<Ws>  ], [++<Wd>  ]
    // DEC2[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $dec2  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "dec2", 0b111010011000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $dec2_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "dec2", 0b111010011000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $dec2  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec2", 0b111010011000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $dec2_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec2", 0b111010011000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $dec2  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec2", 0b111010011000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $dec2_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec2", 0b111010011000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $dec2  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec2", 0b111010011000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $dec2_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "dec2", 0b111010011000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NEG[.B] <f>
    // NEG[.B] <f>, WREG
    public PIC16BitEPC $neg  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("neg", 0b111011100000000000000000, false, f, null); }
    public PIC16BitEPC $neg_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("neg", 0b111011100000000000000000, true , f, null); }
    public PIC16BitEPC $neg  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("neg", 0b111011100000000000000000, false, f, WReg); }
    public PIC16BitEPC $neg_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("neg", 0b111011100000000000000000, true , f, WReg); }

    // NEG[.B]    <Ws>   ,    <Wd>
    // NEG[.B]    <Ws>   , [  <Wd>  ]
    // NEG[.B] [  <Ws>  ],    <Wd>
    // NEG[.B] [  <Ws>  ], [  <Wd>  ]
    // NEG[.B] [  <Ws>++], [  <Wd>++]
    // NEG[.B] [  <Ws>--], [  <Wd>--]
    // NEG[.B] [++<Ws>  ], [++<Wd>  ]
    // NEG[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $neg  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "neg", 0b111010100000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $neg_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "neg", 0b111010100000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $neg  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "neg", 0b111010100000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $neg_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "neg", 0b111010100000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $neg  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "neg", 0b111010100000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $neg_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "neg", 0b111010100000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $neg  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "neg", 0b111010100000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $neg_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "neg", 0b111010100000000000000000, true , Ws           ,           Wd  ); }

    // NEG <Acc>
    public PIC16BitEPC $neg(final _RAcc acc) throws JXMAsmError
    { return $_alu_acc_impl("neg", 0b110010110001000000000000, acc); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : NORM <Acc>, <*Wd*> (dsPIC33C) ??? #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : MAX    <Acc>    (dsPIC33C) ??? #####
    // ##### ??? TODO : MAX.V  <Acc>, * (dsPIC33C) ??? #####

    // ##### ??? TODO : MIN    <Acc>    (dsPIC33C) ??? #####
    // ##### ??? TODO : MIN.V  <Acc>, * (dsPIC33C) ??? #####

    // ##### ??? TODO : MINZ   <Acc>    (dsPIC33C) ??? #####
    // ##### ??? TODO : MINZ.V <Acc>, * (dsPIC33C) ??? #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // COM[.B] <f>
    // COM[.B] <f>, WREG
    public PIC16BitEPC $com  (final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("com", 0b111011101000000000000000, false, f, null); }
    public PIC16BitEPC $com_b(final int f                 ) throws JXMAsmError { return $_alu_f_wreg_impl("com", 0b111011101000000000000000, true , f, null); }
    public PIC16BitEPC $com  (final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("com", 0b111011101000000000000000, false, f, WReg); }
    public PIC16BitEPC $com_b(final int f, final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("com", 0b111011101000000000000000, true , f, WReg); }

    // COM[.B]    <Ws>   ,    <Wd>
    // COM[.B]    <Ws>   , [  <Wd>  ]
    // COM[.B] [  <Ws>  ],    <Wd>
    // COM[.B] [  <Ws>  ], [  <Wd>  ]
    // COM[.B] [  <Ws>++], [  <Wd>++]
    // COM[.B] [  <Ws>--], [  <Wd>--]
    // COM[.B] [++<Ws>  ], [++<Wd>  ]
    // COM[.B] [--<Ws>  ], [--<Wd>  ]
    public PIC16BitEPC $com  (final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "com", 0b111010101000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $com_b(final _RWx    Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "com", 0b111010101000000000000000, true , Ws           ,           Wd  ); }
    public PIC16BitEPC $com  (final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "com", 0b111010101000000000000000, false, Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $com_b(final _RWx    Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "com", 0b111010101000000000000000, true , Expr._reg(Ws),           Wd  ); }
    public PIC16BitEPC $com  (final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "com", 0b111010101000000000000000, false, Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $com_b(final _RWxMem Ws, final _RWx    Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "com", 0b111010101000000000000000, true , Ws           , Expr._reg(Wd) ); }
    public PIC16BitEPC $com  (final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "com", 0b111010101000000000000000, false, Ws           ,           Wd  ); }
    public PIC16BitEPC $com_b(final _RWxMem Ws, final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "com", 0b111010101000000000000000, true , Ws           ,           Wd  ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // CLR[.B] <f>
    // CLR[.B] WREG
    public PIC16BitEPC $clr  (final int f    ) throws JXMAsmError { return $_alu_f_wreg_impl("clr", 0b111011110000000000000000, false, f, null); }
    public PIC16BitEPC $clr_b(final int f    ) throws JXMAsmError { return $_alu_f_wreg_impl("clr", 0b111011110000000000000000, true , f, null); }
    public PIC16BitEPC $clr  (final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("clr", 0b111011110000000000000000, false, 0, WReg); }
    public PIC16BitEPC $clr_b(final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("clr", 0b111011110000000000000000, true , 0, WReg); }

    // CLR[.B]    <Wd>
    // CLR[.B] [  <Wd>  ]
    // CLR[.B] [  <Wd>++]
    // CLR[.B] [  <Wd>--]
    // CLR[.B] [++<Wd>  ]
    // CLR[.B] [--<Wd>  ]
    private PIC16BitEPC $_alu_wd_impl(final String str, final int opcode, final boolean b, final _RWx Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Wd );

        _putOpCode24(
              // 0b.........Bqqqdddd.......
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wd.regNum << 7                    )
        );
        return this;
    }


    private PIC16BitEPC $_alu_rwxmem_impl(final String str, final int opcode, final boolean b, final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + str + _obw(b), Wd );

        if( Wd.immOfs_rwxOfs() ) _errorInvalidInstructionForm( _MSTR, "$" + str + _obw(b) );

        _putOpCode24(
              // 0b.........Bqqqdddd.......
                 opcode                          |
            (b ? 0b000000000100000000000000 : 0) |
            (Wd.mode      << 11                ) |
            (Wd.Wx.regNum <<  7                )
        );
        return this;
    }

    public PIC16BitEPC $clr  (final _RWx    Wd) throws JXMAsmError { return $_alu_wd_impl    ("clr", 0b111010110000000000000000, false, Wd); }
    public PIC16BitEPC $clr_b(final _RWx    Wd) throws JXMAsmError { return $_alu_wd_impl    ("clr", 0b111010110000000000000000, true , Wd); }
    public PIC16BitEPC $clr  (final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_impl("clr", 0b111010110000000000000000, false, Wd); }
    public PIC16BitEPC $clr_b(final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_impl("clr", 0b111010110000000000000000, true , Wd); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SETM[.B] <f>
    // SETM[.B] WREG
    public PIC16BitEPC $setm  (final int f    ) throws JXMAsmError { return $_alu_f_wreg_impl("setm", 0b111011111000000000000000, false, f, null); }
    public PIC16BitEPC $setm_b(final int f    ) throws JXMAsmError { return $_alu_f_wreg_impl("setm", 0b111011111000000000000000, true , f, null); }
    public PIC16BitEPC $setm  (final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("setm", 0b111011111000000000000000, false, 0, WReg); }
    public PIC16BitEPC $setm_b(final _WRg WReg) throws JXMAsmError { return $_alu_f_wreg_impl("setm", 0b111011111000000000000000, true , 0, WReg); }

    // SETM[.B]    <Wd>
    // SETM[.B] [  <Wd>  ]
    // SETM[.B] [  <Wd>++]
    // SETM[.B] [  <Wd>--]
    // SETM[.B] [++<Wd>  ]
    // SETM[.B] [--<Wd>  ]
    public PIC16BitEPC $setm  (final _RWx    Wd) throws JXMAsmError { return $_alu_wd_impl    ("setm", 0b111010111000000000000000, false, Wd); }
    public PIC16BitEPC $setm_b(final _RWx    Wd) throws JXMAsmError { return $_alu_wd_impl    ("setm", 0b111010111000000000000000, true , Wd); }
    public PIC16BitEPC $setm  (final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_impl("setm", 0b111010111000000000000000, false, Wd); }
    public PIC16BitEPC $setm_b(final _RWxMem Wd) throws JXMAsmError { return $_alu_rwxmem_impl("setm", 0b111010111000000000000000, true , Wd); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // PUSH <f>
    public PIC16BitEPC $push(final int f) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$push", f );

        final int imm = _checkUOffset(_MSTR, f, 1, 15);
        _putOpCode24( 0b111110000000000000000000 | (imm << 1) );
        return this;
    }

    // PUSH    <Ws>
    // PUSH [  <Ws>   ]
    // PUSH [  <Ws>++ ]
    // PUSH [  <Ws>-- ]
    // PUSH [++<Ws>   ]
    // PUSH [--<Ws>   ]
    // PUSH [<Ws>+<Wb>]
    public PIC16BitEPC $push(final _RWxMem Ws) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$push", Ws );

        if( Ws.immOfs() ) _errorInvalidInstructionForm( _MSTR, "$push" );

        _putOpCode24(
          // 0b01111wwww00111111gggssss
             0b011110000001111110000000  |
            (Ws.Wb.regNum << 15        ) |
            (Ws.mode      <<  4        ) |
            (Ws.Wx.regNum              )
        );
        return this;
    }

    public PIC16BitEPC $push(final _RWx Ws) throws JXMAsmError
    { return $push( Expr._reg(Ws) ); }

    // PUSH.D <Ws>
    public PIC16BitEPC $push_d(final _RWx Ws) throws JXMAsmError
    {
        // ERROR: _MNEMONIC_STRING("push_d", ...) is missing the "$" prefix. Method name is "$push_d". Fix: change "push_d" to "$push_d".
        _MNEMONIC_STRING( "push_d", Ws);

        _piRepeat = false; // This instruction can be preceded by '$repeat()'
        return $mov_d( Ws, Expr.ind_pp(Reg.SP) );
    }

    // PUSH.S
    public PIC16BitEPC $push_s() throws JXMAsmError
    {
        _putOpCode24( 0b111111101010000000000000 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // POP <f>
    public PIC16BitEPC $pop(final int f) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$pop", f );

        final int imm = _checkUOffset(_MSTR, f, 1, 15);
        _putOpCode24( 0b111110010000000000000000 | (imm << 1) );
        return this;
    }

    // POP    <Wd>
    // POP [  <Wd>   ]
    // POP [  <Wd>++ ]
    // POP [  <Wd>-- ]
    // POP [++<Wd>   ]
    // POP [--<Wd>   ]
    // POP [<Wd>+<Wb>]
    public PIC16BitEPC $pop(final _RWxMem Wd) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$pop", Wd );

        if( Wd.immOfs() ) _errorInvalidInstructionForm( _MSTR, "$pop" );

        _putOpCode24(
          // 0b01111wwww0hhhdddd1001111
             0b011110000000000001001111  |
            (Wd.Wb.regNum << 15        ) |
            (Wd.mode      << 11        ) |
            (Wd.Wx.regNum <<  7        )
        );
        return this;
    }

    public PIC16BitEPC $pop(final _RWx Wd) throws JXMAsmError
    { return $pop( Expr._reg(Wd) ); }

    // POP.D <Wd>
    public PIC16BitEPC $pop_d(final _RWx Wd) throws JXMAsmError
    {
        // ERROR: _MNEMONIC_STRING("pop_d", ...) is missing the "$" prefix. Method name is "$pop_d". Fix: change "pop_d" to "$pop_d".
        _MNEMONIC_STRING( "pop_d", Wd );

        _piRepeat = false; // This instruction can be preceded by '$repeat()'
        return $mov_d( Expr.mm_ind(Reg.SP), Wd );
    }

    // POP.S
    public PIC16BitEPC $pop_s() throws JXMAsmError
    {
        _putOpCode24( 0b111111101000000000000000 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // CLRWDT
    public PIC16BitEPC $clrwdt() throws JXMAsmError
    {
        _putOpCode24( 0b111111100110000000000000 );
        return this;
    }

    // DISI #<ulit14>
    public PIC16BitEPC $disi(final _Lit cycles14) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$disi", cycles14 );

        _errorIfPrevIsRepeat(_MSTR);

        _checkUOffset(_MSTR, cycles14.value, 0, 14);
        _putOpCode24( 0b111111000000000000000000 | cycles14.value );
        return this;
    }

    // LNK #<ulit14>
    public PIC16BitEPC $lnk(final _Lit sfz14) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lnk", sfz14 );

        _errorIfPrevIsRepeat(_MSTR);
        _errorIfAddressOffsetNotAligned2B(_MSTR, sfz14.value);

        final int imm = _checkUOffset(_MSTR, sfz14.value, 1, 13);
        _putOpCode24( 0b111110100000000000000000 | (imm << 1) );
        return this;
    }

    // ULNK
    public PIC16BitEPC $ulnk() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ulnk" );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24( 0b111110101000000000000000 );
        return this;
    }

    // RESET
    public PIC16BitEPC $reset() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$reset" );

        _errorIfPrevIsRepeat(_MSTR);

        _putOpCode24( 0b111111100000000000000000 );
        return this;
    }

    // PWRSAV #<ulit1>
    public PIC16BitEPC $pwrsav(final _Lit en1) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$pwrsav", en1 );

        _errorIfPrevIsRepeat(_MSTR);

        _checkUOffset(_MSTR, en1.value, 0, 1);
        _putOpCode24( 0b111111100100000000000000 | en1.value );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ZE    <Ws>   , <Wd>
    // ZE [  <Ws>  ], <Wd>
    // ZE [  <Ws>++], <Wd>
    // ZE [  <Ws>--], <Wd>
    // ZE [++<Ws>  ], <Wd>
    // ZE [--<Ws>  ], <Wd>
    public PIC16BitEPC $ze(final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "ze", 0b111110111000000000000000, false, Ws, Wd            ); }
    public PIC16BitEPC $ze(final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "ze", 0b111110111000000000000000, false, Ws, Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // SE    <Ws>   , <Wd>
    // SE [  <Ws>  ], <Wd>
    // SE [  <Ws>++], <Wd>
    // SE [  <Ws>--], <Wd>
    // SE [++<Ws>  ], <Wd>
    // SE [--<Ws>  ], <Wd>
    public PIC16BitEPC $se(final _RWx    Ws, final _RWx Wd) throws JXMAsmError { return $_alu_ws_wd_impl        ( "se", 0b111110110000000000000000, false, Ws, Wd            ); }
    public PIC16BitEPC $se(final _RWxMem Ws, final _RWx Wd) throws JXMAsmError { return $_alu_rwxmem_rwxmem_impl( "se", 0b111110110000000000000000, false, Ws, Expr._reg(Wd) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String PIC_OBJDUMP_BINARY = null;

    public static void setPICObjDumpBinary(final String path)
    { PIC_OBJDUMP_BINARY = path; }

    public static int[] toArray24Bits(final int[] buff8)
    {
        final int[] res = new int[buff8.length / 3];
              int   idx = 0;

        for(int i = 0; i < buff8.length; i += 3) {
            res[idx++] = ( buff8[i + 2] << 16 ) | ( buff8[i + 1] << 8 ) | buff8[i + 0];
        }

        return res;
    }

    public static void dumpArray24Bits(final int[] buff24)
    {
        final String  INDENT1 = "    ";
        final String  INDENTM = INDENT1 + INDENT1 + INDENT1 + INDENT1;
              boolean nl      = false;

        SysUtil.stdDbg().println(INDENTM + "private static final int[] _PIC16BitEPC_program_dump = new int[] {");

        for(int i = 0; i < buff24.length; ++i) {

            nl = false;

            if( (i % 16) == 0 ) SysUtil.stdDbg().print(INDENT1 + INDENTM);

            SysUtil.stdDbg().printf( "0x%06X%s ", buff24[i], (i < buff24.length - 1) ? "," : "" );
            if( ( (i + 1) % 16 ) == 0 ) {
                SysUtil.stdDbg().println();
                nl = true;
            }

        } // for

        if(!nl) SysUtil.stdDbg().println();

        SysUtil.stdDbg().printf( INDENTM + "};\n\n"                                 );
        SysUtil.stdDbg().printf( INDENTM + "### .text = %d bytes ###\n", buff24.length * 3 );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _objWrU08(final java.io.OutputStream fos, final int value) throws Exception
    { fos.write( (byte) (value & 0xFF) ); }

    private void _objWrU16(final java.io.OutputStream fos, final int value) throws Exception
    {
         fos.write( new byte[] {
             (byte) ( (value >>> 0) & 0xFF ),
             (byte) ( (value >>> 8) & 0xFF )
         } );
    }

    private void _objWrU32(final java.io.OutputStream fos, final long value) throws Exception
    {
         fos.write( new byte[] {
             (byte) ( (value >>>  0) & 0xFF ),
             (byte) ( (value >>>  8) & 0xFF ),
             (byte) ( (value >>> 16) & 0xFF ),
             (byte) ( (value >>> 24) & 0xFF )
         } );
    }

    private void _writeObj(final String elf, final int[] res, final int e_flags, final int orgAddr) throws Exception
    {
        /*
         * This function is written based on the information found from:
         *
         *     https://en.wikipedia.org/wiki/Executable_and_Linkable_Format
         *     https://cnlelema.github.io/memo/en/codegen/basic-elf/symbol-table
         *     https://docs.oracle.com/cd/E23824_01/html/819-0690/chapter6-79797.html
         *
         * ~~~ Last accessed & checked on 2024-11-18 ~~~
         */

        // Generate the string-table data
        final java.nio.charset.Charset      chrs    = java.nio.charset.StandardCharsets.US_ASCII;
        final java.io.ByteArrayOutputStream baosStr = new java.io.ByteArrayOutputStream();

              int secCnt    = 0;
                                              baosStr.write( "\0"         .getBytes(chrs), 0,  1 ); ++secCnt;
        final int sfsSymTab = baosStr.size(); baosStr.write( ".symtab\0"  .getBytes(chrs), 0,  8 ); ++secCnt;
        final int sfsStrTab = baosStr.size(); baosStr.write( ".shstrtab\0".getBytes(chrs), 0, 10 ); ++secCnt;
        final int sfsText   = baosStr.size(); baosStr.write( ".text\0"    .getBytes(chrs), 0,  6 ); ++secCnt;
        final int sfsReset  = baosStr.size(); baosStr.write( "__reset\0"  .getBytes(chrs), 0,  8 );

        // Generate the symbol-table data
        final java.io.ByteArrayOutputStream baosSym  = new java.io.ByteArrayOutputStream();
        final int                           codeSize = res.length * 4 / 3;

        _objWrU32(baosSym, sfsText ); // st_name                                     ;   0x********
        _objWrU32(baosSym, orgAddr ); // st_value                                    ;   0x********
        _objWrU32(baosSym, codeSize); // st_size                                     ;   0x********
        _objWrU08(baosSym, 0x03    ); // st_info                                     ;   STB_LOCAL | STT_SECTION
        _objWrU08(baosSym, 0x00    ); // st_other                                    ;   STV_DEFAULT
        _objWrU16(baosSym, 0x0001  ); // st_shndx                                    ;   0x0001

        _objWrU32(baosSym, sfsReset); // st_name                                     ;   0x********
        _objWrU32(baosSym, orgAddr ); // st_value                                    ;   0x********
        _objWrU32(baosSym, codeSize); // st_size                                     ;   0x********
        _objWrU08(baosSym, 0x12    ); // st_info                                     ;   STB_GLOBAL | STT_FUNC
        _objWrU08(baosSym, 0x00    ); // st_other                                    ;   STV_DEFAULT
        _objWrU16(baosSym, 0x0001  ); // st_shndx                                    ;   0x0001

        // Open the file
        final java.io.FileOutputStream fos = new java.io.FileOutputStream(elf);

        // Determine the offset
        final int textStart = 0x00000034; // The size of the ELF header is fixed
        final int textSize  = codeSize;

        final int ssymStart = textStart + textSize;
        final int ssymSize  = baosSym.size();

        final int stabStart = ssymStart + ssymSize;
        final int stabSize  = baosStr.size();

        final int sechStart = stabStart + stabSize;

        // ELF header
        _objWrU32(fos, 0x464C457F); // e_ident[EI_MAG0      ] ... e_ident[EI_MAG3]   ;   Magic number (0x7F ELF)
        _objWrU08(fos, 0x01      ); // e_ident[EI_CLASS     ]                        ;   32-bit format
        _objWrU08(fos, 0x01      ); // e_ident[EI_DATA      ]                        ;   Little-endian format
        _objWrU08(fos, 0x01      ); // e_ident[EI_VERSION   ]                        ;   Version 1 (the original and current version of ELF)
        _objWrU08(fos, 0x00      ); // e_ident[EI_OSABI     ]                        ;   Target operating system ABI is 'System V'
        _objWrU08(fos, 0x00      ); // e_ident[EI_ABIVERSION]                        ;   Version 0
        _objWrU32(fos, 0x00000000); // e_ident[EI_PAD       ]                        ;   0x00000000000000
        _objWrU16(fos, 0x0000    ); // ---                                           ;   ---
        _objWrU08(fos, 0x00      ); // ---                                           ;   ---
        _objWrU16(fos, 0x0001    ); // e_type                                        ;   ET_REL (relocatable file)
        _objWrU16(fos, 0x0076    ); // e_machine                                     ;   Microchip Technology dsPIC30F Digital Signal Controller
        _objWrU32(fos, 0x00000001); // e_version                                     ;   Version 1 (the original version of ELF)
        _objWrU32(fos, 0x00000000); // e_entry                                       ;   0x00000000
        _objWrU32(fos, 0x00000000); // e_phoff                                       ;   0x00000000
        _objWrU32(fos, sechStart ); // e_shoff                                       ;   0x********
        _objWrU32(fos, e_flags   ); // e_flags                                       ;   0x********
        _objWrU16(fos, 0x0034    ); // e_ehsize                                      ;   0x0034 (size of this header)                  (32-bit)
        _objWrU16(fos, 0x0020    ); // e_phentsize                                   ;   0x0020 (size of a program header table entry) (32-bit)
        _objWrU16(fos, 0x0000    ); // e_phnum                                       ;   0x0000
        _objWrU16(fos, 0x0028    ); // e_shentsize                                   ;   0x0028 (size of a section header table entry) (32-bit)
        _objWrU16(fos, secCnt    ); // e_shnum                                       ;   0x****
        _objWrU16(fos, secCnt - 1); // e_shstrndx                                    ;   0x****

        // Put the text section data
        for(int i = 0; i < res.length; i += 3) {
            fos.write( (byte) res[i + 0] );
            fos.write( (byte) res[i + 1] );
            fos.write( (byte) res[i + 2] );
            fos.write( (byte) 0x00       );
        }

        // Put the symbol-table section data
        fos.write( baosSym.toByteArray() );

        // Put the string-table section data
        fos.write( baosStr.toByteArray() );

        // Put the section header for the NULL section
        _objWrU32(fos, 0x00000000); // sh_name                                       ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_type                                       ;   SHT_NULL
        _objWrU32(fos, 0x00000000); // sh_flags                                      ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_addr                                       ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_offset                                     ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_size                                       ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_link                                       ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_info                                       ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_addralign                                  ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_entsize                                    ;   0x00000000

        // Put the section header for the text section
        _objWrU32(fos, sfsText   ); // sh_name                                       ;   0x********
        _objWrU32(fos, 0x00000001); // sh_type                                       ;   SHT_PROGBITS
        _objWrU32(fos, 0x00000006); // sh_flags                                      ;   SHF_EXECINSTR | SHF_ALLOC
        _objWrU32(fos, orgAddr   ); // sh_addr                                       ;   0x********
        _objWrU32(fos, textStart ); // sh_offset                                     ;   0x********
        _objWrU32(fos, textSize  ); // sh_size                                       ;   0x********
        _objWrU32(fos, 0x00000000); // sh_link                                       ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_info                                       ;   0x00000000
        _objWrU32(fos, 0x00000002); // sh_addralign                                  ;   0x00000002
        _objWrU32(fos, 0x00000000); // sh_entsize                                    ;   0x00000000

        // Put the section header for the symbol-table section
        _objWrU32(fos, sfsSymTab ); // sh_name                                       ;   0x********
        _objWrU32(fos, 0x00000002); // sh_type                                       ;   SHT_SYMTAB
        _objWrU32(fos, 0x00000000); // sh_flags                                      ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_addr                                       ;   0x00000000
        _objWrU32(fos, ssymStart ); // sh_offset                                     ;   0x********
        _objWrU32(fos, ssymSize  ); // sh_size                                       ;   0x********
        _objWrU32(fos, secCnt - 1); // sh_link                                       ;   0x********
        _objWrU32(fos, 0x00000000); // sh_info                                       ;   0x00000000
        _objWrU32(fos, 0x00000004); // sh_addralign                                  ;   0x00000004
        _objWrU32(fos, 0x00000010); // sh_entsize                                    ;   0x00000010 (each entry is 16 bytes long)

        // Put the section header for the string-table section
        _objWrU32(fos, sfsStrTab ); // sh_name                                       ;   0x********
        _objWrU32(fos, 0x00000003); // sh_type                                       ;   SHT_STRTAB
        _objWrU32(fos, 0x00000000); // sh_flags                                      ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_addr                                       ;   0x00000000
        _objWrU32(fos, stabStart ); // sh_offset                                     ;   0x********
        _objWrU32(fos, stabSize  ); // sh_size                                       ;   0x********
        _objWrU32(fos, 0x00000000); // sh_link                                       ;   0x00000000
        _objWrU32(fos, 0x00000000); // sh_info                                       ;   0x00000000
        _objWrU32(fos, 0x00000001); // sh_addralign                                  ;   0x00000001
        _objWrU32(fos, 0x00000000); // sh_entsize                                    ;   0x00000000

        // Done
        fos.flush();
        fos.close();
    }

    private int[] _link_impl(final int originAddress, final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        // Resolve the target label(s)
        for(final _RA r : _resolve) {

            // Get the target label specifications
            final String  mString   = r.mString;
            final String  label     = r.label;
            final int     location  = r.location;
            final int     bitSize   = r.bitSize;
            final boolean isSigned  = r.isSigned;
            final boolean isAbs     = r.isAbs;
            final boolean isOrgOfs  = r.isOrgOfs;
            final int     valSF     = r.valSF;
            final boolean valSFSub2 = r.valSFSub2;
            final int     msb8Pos   = r.msb8Pos;
            final int     msb8PSF   = r.msb8PSF;
            final int     lswNPos   = r.lswNPos;
            final int     lswPNSF0  = r.lswPNSF0;
            final int     lswPNSF1  = r.lswPNSF1;

            // Get the target location
            int target = -1;

            if(label != null) {

                final char sign = label.charAt(0);

                if(sign != '+' && sign != '-') {

                    List<Integer> targetList = _labels.get(label);

                    if(targetList == null && label.length() == 2) {
                        final char ch0 = label.charAt(0);
                        final char ch1 = label.charAt(1);
                        targetList = _labels.get("" + ch0);
                        if(targetList != null) {
                            if(ch1 == 'b') {
                                target = -1;
                                for(final int ra : targetList) {
                                    if(ra < lswNPos) target = Math.max(target, ra);
                                }
                            }
                            else if(ch1 == 'f') {
                                target = 16777215;
                                for(final int ra : targetList) {
                                    if(ra > lswNPos) target = Math.min(target, ra);
                                }
                            }
                        }
                    }

                    if(targetList == null) _errorInvalidTargetLabel(mString, label);

                    if(target < 0) target = targetList.get(0);

                }

                else {

                    int ofs = Integer.decode( label.substring(1) );
                    _errorIfAddressOffsetNotAligned2B(mString, ofs);

                    ofs = ofs * 3 / 2;
                    if(sign == '-') ofs = -ofs;

                    target = lswNPos + 3 + ofs;

                }

            }
            else {

                target = location;

            }

            // Resolve the target address
            int address = (label != null) ? (target * 2 / 3) : target;

            if(isAbs) {
                if(isOrgOfs) address += originAddress;
            }
            else {
                address -= lswNPos * 2 / 3;
            }

            // Adjust the address
            if(valSFSub2) address-= 2;

            if(isSigned) address  >>= valSF;
            else         address >>>= valSF;

            // Check the target address
            //_errorIfAddressOffsetNotAligned2B(mString, address);

            if(isSigned) _checkSOffset(mString, address, 0, bitSize);
            else         _checkUOffset(mString, address, 0, bitSize);

            // Modify the most-significant byte
            if(msb8Pos >= 0) {
                if(msb8PSF  >= 0) _opcode.set( msb8Pos    , _opcode.get(msb8Pos    ) | ( ( (address & 0xFF0000) >> 16 ) << msb8PSF  ) );
            }

            // Modify the least-significant word
            if(lswNPos >= 0) {
                if(lswPNSF1 >= 0) _opcode.set( lswNPos + 1, _opcode.get(lswNPos + 1) | ( ( (address & 0x00FF00) >>  8 ) << lswPNSF1 ) );
                if(lswPNSF0 >= 0) _opcode.set( lswNPos + 0, _opcode.get(lswNPos + 0) | ( ( (address & 0x0000FF)       ) << lswPNSF0 ) );
            }

        } // for

        // Generate the final result
        final int[] res = _opcode.stream().mapToInt(i -> i).toArray();

        // Dump as needed
        if( (ForceDumpDisassembly || dumpDisassemblyAndArray) && PIC_OBJDUMP_BINARY != null ) {
            try {
                // Determine the appropriate 'e_flags'
                final int e_flags = ( _cpu.isPIC24() && PIC_OBJDUMP_BINARY.contains("xc-dsc") ) ? CPU._ANY_.e_flags : _cpu.e_flags;
                // Convert the temporary binary file to a temporary object file
                final String obj = SysUtil.resolvePath( "__PIC16BitEPC__.o"  , SysUtil.getRootTmpDir() );
                _writeObj(obj, res, e_flags, originAddress);
                // Disassemble the temporary object file
                SysUtil.stdDbg().println( SysUtil.execlp(
                    PIC_OBJDUMP_BINARY, "-EL", "-D", "-z", obj
                ) );
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                throw XCom.newJXMAsmError( e.getMessage() );
            }
        }

        // Dump as needd
        if( ForceDumpBytes || (dumpDisassemblyAndArray && !NeverDumpBytes) ) {
            for(int i = 0; i < res.length; i += 3) {
                SysUtil.stdDbg().printf( "%02x %02x %02x\n", res[i + 0], res[i + 1], res[i + 2]);
            }
        }

         // Return the final result
        return res;
    }

    private void _clear()
    {
        _opcode .clear();
        _labels .clear();
        _resolve.clear();

        _piRepeat = false;
    }

} // class PIC16BitEPC
