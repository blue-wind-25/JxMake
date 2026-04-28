/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


/*
 * A simple assembler and linker for ARM Cortex-M processors:
 *     # It only supports Thumb instructions (you can still call ARM instructions using 'bx' and 'blx' as usual
 *       if the MCU supports ARM mode).
 *     # It only supports a limited set of opcodes:
 *           # All   ARMv6-M          opcodes (Cortex-M0 /M0+/M1).
 *           # A few ARMv7-M          opcodes (Cortex-M3 /M4 /M7).
 *           # Some  ARMv8-M Baseline opcodes (Cortex-M23       ).
 *       Support for more opcodes/architectures/processors may be added in the future.
 *     # It is intended to be used to assemble and link simple programs such as flash loader and stub programs.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 *     # Compared to GNU GAS (part of GCC), this class exhibits some differences in opcode and instruction
 *       naming conventions:
 *           # The '.N' suffix will never be implemented because by default all Thumb 1 instructions is 16-bit
 *             wide.
 *           # The '.W' suffix will automatically applied if possible. In case of overloading function names
 *             and/or signatures conflict then the use of '_w' suffix in the function name becomes mandatory
 *             (if this option is also not feasible or suitable, then the functions will have custom suffixes).
 *           # The instruction writeback specifier 'Rn!' must be written as 'Rn.wb'; for example:
 *                 # ldmia r0!, {    r2, r3, r4, r5}   =>   $ldmia(R0.wb,     R2, R3, R4, R5)
 *                 # stmia r0!, {r0, r2, r3, r4, r5}   =>   $stmia(R0.wb, R0, R2, R3, R4, R5)
 *           # The instruction memory specifier must be written plainly and linearly, without any explicit
 *             specifier; for example:
 *                 # ldr r0, [sp        ]           =>   $ldr    (R0, SP           )
 *                 # ldr r0, [r2, 4 * 31]           =>   $ldr    (R0, R2   , 4 * 31)
 *                 # ldr r0, [r2, 4 * -1]!          =>   $ldr    (R0, R2.wb, 4 * -1)   =>   Notice the '.wb'
 *                 # ldr r0, [r2        ], 4 * -1   =>   $ldr_pst(R0, R2   , 4 * -1)   =>   Notice the '_pst'
 *                 # ldr r0, [r1, r2    ]           =>   $ldr    (R0, R1   , R2    )
 *       NOTE : This linear writing style is possible because Thumb instruction formats are quite simple and
 *              straightforward.
 *
 *     # Compared to GNU GAS (part of GCC), the IT block implementation in this class (ARMCortexMThumb)
 *       is significantly simpler
 *           # The instructions' condition-specifiers (<C>) cannot be explicitly written; they are
 *             automatically inferred from the preceding IT instruction. As a result, conditions are not
 *             double-checked against the IT instruction.
 *           # The <S> suffixes cannot be removed from the instructions; they are automatically retained or
 *             omitted based on the size of the resulting opcode (16-bit vs 32-bit).
 *           # If precise control over the <S> suffix retention (or removal) is required, it is strongly
 *             recommended to verify the generated opcodes via disassembly. Use the 'setARMObjDumpBinary()'
 *             function to specify the disassembler program.
 *       NOTE : The ARMCortexMThumbC class provides a more complete IT block implementation. See the notes
 *              below for more details.
 *
 *     # Compared to GNU GAS (part of GCC), this class may choose slightly different opcodes and padding,
 *       such as:
 *           # Using one T1 MOV R8, R8 (0x46C0)               vs   using one T1 NOP   (0xBF00    ) for no-operation.
 *           # Using one T1 NOP        (0xBF00) for padding   vs   using two zeroes   (0x0000    ) for padding.
 *           # Using two T1 NOP        (0xBF00)               vs   using one T2 NOP.W (0xF3AF8000) for padding.
 *       NOTE : This class may also pad more aggressively than GNU GAS.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 *    # The ARMCortexMThumbC class provides a better and more complete IT block implementation. However,
 *      when compared to GNU GAS (part of GCC), it is still simpler:
 *           # All normal (non IT block) instructions implemented by the ARMCortexMThumb class, should
 *             have their corresponding non-flag-updating '*<C>' instructions implemented by the this
 *             class.
 *           # However, only a very few of the flag-updating '*S<C>' instructions are implemented.
 *           # Please prefer to use the ARMCortexMThumb class instead of the ARMCortexMThumbC class if
 *             you are not going to use IT blocks.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written based on the information found from:
 *
 *     ARM(R) v6-M Architecture Reference Manual
 *     https://developer.arm.com/documentation/ddi0419/latest
 *     https://documentation-service.arm.com/static/5f8ff05ef86e16515cdbf826
 *
 *     ARM(R) v7-M Architecture Reference Manual
 *     https://developer.arm.com/documentation/ddi0403/latest
 *     https://documentation-service.arm.com/static/606dc36485368c4c2b1bf62f
 *
 *     ARM(R) v8-M Architecture Reference Manual
 *     https://developer.arm.com/documentation/ddi0553/latest
 *     https://documentation-service.arm.com/static/65816177b52744113be5e971
 *
 * ~~~ Last accessed & checked on 2024-05-27 ~~~
 */
public class ARMCortexMThumb {

    private static final String  ClassName            = "ARMCortexMThumb";

    private static final boolean ForceDumpInsertion   = false;
    private static final boolean ForceDumpDisassembly = false;
    private static final boolean ForceDumpArray       = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public  static final String Label_NMI_Handler       = "NMI_Handler";
    public  static final String Label_HardFault_Handler = "HardFault_Handler";
    public  static final String Label_Start             = "_start";

    private static interface OCPF {
        public long apply(final OpCode opcode, final long address, final int thumbBit) throws JXMAsmError;
    };

    private static class OpCodeParam {

        public final long   paramDataAddress;    // Immediate address
        public final String paramLabel;          // Label     address and hints
        public final int    paramBitShiftPos;    // The amount of (left) shift to be applied to the parameter when combining it with the opcode when linking
        public final OCPF   paramFunc;           // Opcode parameter processor function

        public final int    valueBitShiftFactor; // When resolving the address,  passed to '_checkUOffset()' if positive, to '_checkSOffset()' if negative
        public final int    valueMaxNumBits;     // ---

        public OpCodeParam(final long paramDataAddress_, final String paramLabel_, final int paramBitShiftPos_, final int valueBitShiftFactor_, final int valueMaxNumBits_)
        {
            paramDataAddress    = paramDataAddress_;
            paramLabel          = paramLabel_;
            paramBitShiftPos    = paramBitShiftPos_;
            paramFunc           = null;

            valueBitShiftFactor = valueBitShiftFactor_;
            valueMaxNumBits     = valueMaxNumBits_;
        }

        public OpCodeParam(final String paramLabel_, final int paramBitShiftPos_, final int valueBitShiftFactor_, final int valueMaxNumBits_)
        {
            paramDataAddress    = -1;
            paramLabel          = paramLabel_;
            paramBitShiftPos    = paramBitShiftPos_;
            paramFunc           = null;

            valueBitShiftFactor = valueBitShiftFactor_;
            valueMaxNumBits     = valueMaxNumBits_;
        }

        public OpCodeParam(final String paramLabel_, final OCPF paramFunc_)
        {
            paramDataAddress    = -1;
            paramLabel          = paramLabel_;
            paramBitShiftPos    = 0;
            paramFunc           = paramFunc_;

            valueBitShiftFactor = 0;
            valueMaxNumBits     = 0;
        }

    } // class OpCodeParam

    private static class OpCode {

        public final String        mString;

        public       long          address;

        public final int           bitSize;
        public       long          opcodeValue;
        public       OpCodeParam   opcodeParam;

        public       boolean       swapHWords;

        public OpCode(final OpCodeBuffer buffer, final String mString_, final int bitSize_, final long opcodeValue_)
        {
            mString     = mString_;

            address     = buffer.getNextAddress();

            bitSize     = Math.abs(bitSize_);
            opcodeValue = opcodeValue_;
            opcodeParam = null;

            swapHWords  = (bitSize_ == 32);

            if(ForceDumpInsertion) SysUtil.stdDbg().printf("[%02d-bit] [T^%08X] %08X\n", bitSize, address, opcodeValue);
        }

        public OpCode(final OpCodeBuffer buffer, final String mString_, final int bitSize_, final long opcodeValue_, final String paramLabel, final OCPF paramFunc)
        {
            mString     = mString_;

            address     = buffer.getNextAddress();

            bitSize     = Math.abs(bitSize_);
            opcodeValue = opcodeValue_;
            opcodeParam = new OpCodeParam(paramLabel, paramFunc);

            swapHWords  = (bitSize_ == 32);

            if(ForceDumpInsertion) SysUtil.stdDbg().printf( "[%02d-bit] [T^%08X] %08X [%s][%s]\n", bitSize, address, opcodeValue, paramLabel, paramFunc.toString() );
        }

        public OpCode(final OpCodeBuffer buffer, final String mString_, final int bitSize_, final long opcodeValue_, final long paramDataAddress, final String paramLabel, final int paramBitShiftPos, final int valueBitShiftFactor, final int valueMaxNumBits)
        {
            mString     = mString_;

            address     = buffer.getNextAddress();

            bitSize     = Math.abs(bitSize_);
            opcodeValue = opcodeValue_;
            opcodeParam = new OpCodeParam(paramDataAddress, paramLabel, paramBitShiftPos, valueBitShiftFactor, valueMaxNumBits);

            swapHWords  = (bitSize_ == 32);

            if(ForceDumpInsertion) SysUtil.stdDbg().printf("[%02d-bit] [T^%08X] %08X [%s][D^%08X][<<%02d] [%+03d:%+03d]\n", bitSize, address, opcodeValue, paramLabel, paramDataAddress, paramBitShiftPos, valueBitShiftFactor, valueMaxNumBits);
        }

        public OpCode(final OpCodeBuffer buffer, final String mString_, final int bitSize_, final long opcodeValue_, final String paramLabel, final int paramBitShiftPos, final int valueBitShiftFactor, final int valueMaxNumBits)
        {
            mString     = mString_;

            address     = buffer.getNextAddress();

            bitSize     = Math.abs(bitSize_);
            opcodeValue = opcodeValue_;
            opcodeParam = new OpCodeParam(paramLabel, paramBitShiftPos, valueBitShiftFactor, valueMaxNumBits);

            swapHWords  = (bitSize_ == 32);

            if(ForceDumpInsertion) SysUtil.stdDbg().printf("[%02d-bit] [T^%08X] %08X [%s][<<%02d] [%+03d:%+03d]\n", bitSize, address, opcodeValue, paramLabel, paramBitShiftPos, valueBitShiftFactor, valueMaxNumBits);
        }

        public OpCode(final OpCodeBuffer buffer, final String mString, final int bitSize, final long opcodeValue, final String paramLabel)
        { this(buffer, mString, bitSize, opcodeValue, paramLabel, 0, 0, 0); }

    } // class OpCode

    @SuppressWarnings("serial")
    private class OpCodeBuffer extends ArrayList<OpCode> {

        public long getPrevAddress()
        {
            if( this.isEmpty() ) return -1;

            final OpCode loc = this.get( this.size() - 1 );

            return loc.address;
        }

        public long getNextAddress()
        {
            if( this.isEmpty() ) return 0;

            final OpCode loc = this.get( this.size() - 1 );

            return loc.address + (loc.bitSize / 8);
        }

    } // class OpCodeBuffer

    private static enum Arch {

        /*
         * ARM Cortex-M Instruction Variations
         * https://en.wikipedia.org/wiki/ARM_Cortex-M
         *
         * Cortex-M1 Code Compatibility with Cortex-M0 and Cortex-M0+
         * https://developer.arm.com/documentation/ka001145/latest
         *
         * Introduction to the ARMv8-M Architecture and Its Programmers Model User Guide
         * https://developer.arm.com/documentation/107656/0101/Introduction-to-Armv8-M-architecture
         */

        ARMv6M_Reduced,    // ARMv6-M   Reduced  - ARMv6-M  without power management instructions (SEV, WFE, and WFI); they actually exist, but are executed as NOP instructions
        ARMv6M,            // ARMv6-M
        ARMv7M,            // ARMv7-M
        ARMv7EM,           // ARMv7E-M           - ARMv7-M  with    saturating and SIMD instructions
        ARMv8M_Baseline,   // ARMv8-M   Baseline - ARMv6-M  with    some additional instructions
        ARMv8M_Mainline,   // ARMv8-M   Mainline - ARMv7-M  with    some additional instructions
        ARMv8p1M_Mainline  // ARMv8.1-M Mainline - ARMv7E-M with    some additional instructions and optional support for 128-bit operations

    } // enum Arch

    private static enum IT {

        ___(""   ,    1,    0,    0,    0),
        T__("t"  ,  255,    1,    0,    0),
        E__("e"  , -255,    1,    0,    0),
        TT_("tt" ,  255,  255,    1,    0),
        ET_("et" , -255,  255,    1,    0),
        TE_("te" ,  255, -255,    1,    0),
        EE_("ee" , -255, -255,    1,    0),
        TTT("ttt",  255,  255,  255,    1),
        ETT("ett", -255,  255,  255,    1),
        TET("tet",  255, -255,  255,    1),
        EET("eet", -255, -255,  255,    1),
        TTE("tte",  255,  255, -255,    1),
        ETE("ete", -255,  255, -255,    1),
        TEE("tee",  255, -255, -255,    1),
        EEE("eee", -255, -255, -255,    1)

        ;

        private final String _str;
        private final int[]  _masks;

        private IT(final String str, final int m3, final int m2, final int m1, final int m0)
        {
            _str   = str;
            _masks = new int[] { m0, m1, m2, m3 };
        }

        private int _mask(final Cond firstCond, final LinkedList<Boolean> block)
        {
            // Put the first block entry
            block.add(true);

            // Generate the mask and put the remaining block entries
            int mask = 0;

            for(int i = 0; i <= 3; ++i) {
                // Generate the mask
                     if(_masks[i] ==  255) mask |= ( ( firstCond.value & 0x01) ) << i;
                else if(_masks[i] == -255) mask |= ( (~firstCond.value & 0x01) ) << i;
                else                       mask |= ( _masks[i]                 ) << i;
                // Put the block entry
                     if(_masks[i] ==  255) block.add(true );
                else if(_masks[i] == -255) block.add(false);
            }

            // Return the mask
            return mask;
        }

    } // enum IT

    protected static interface ITCondDispatch { public ARMCortexMThumb dispatch() throws JXMAsmError; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum CPU {

        M0    ("ARM Cortex-M0"  , Arch.ARMv6M           ),
        M0plus("ARM Cortex-M0+" , Arch.ARMv6M           ),
        M1    ("ARM Cortex-M1"  , Arch.ARMv6M_Reduced   ),
        M3    ("ARM Cortex-M3"  , Arch.ARMv7M           ),
        M4    ("ARM Cortex-M4"  , Arch.ARMv7EM          ),
        M7    ("ARM Cortex-M7"  , Arch.ARMv7EM          ),
        M23   ("ARM Cortex-M23" , Arch.ARMv8M_Baseline  ),
        M33   ("ARM Cortex-M33" , Arch.ARMv8M_Mainline  ),
        M35P  ("ARM Cortex-M35P", Arch.ARMv8M_Mainline  ),
        M52   ("ARM Cortex-M52" , Arch.ARMv8p1M_Mainline),
        M55   ("ARM Cortex-M55" , Arch.ARMv8p1M_Mainline),
        M85   ("ARM Cortex-M85" , Arch.ARMv8p1M_Mainline)

        ;

        private CPU(final String name_, final Arch arch_)
        {
            name = name_;
            arch = arch_;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final String name;
        public final Arch   arch;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean cpuIsEqualTo(final CPU refCPU)
        { return this == refCPU; }

        public boolean cpuIsLowerThan(final CPU refCPU)
        { return this.ordinal() < refCPU.ordinal(); }

        public boolean cpuIsAtLeast(final CPU refCPU)
        { return this.ordinal() >= refCPU.ordinal(); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean archIsEqualTo(final Arch refArch)
        { return this.arch == refArch; }

        public boolean archIsLowerThan(final Arch refArch)
        { return this.arch.ordinal() < refArch.ordinal(); }

        public boolean archIsAtLeast(final Arch refArch)
        { return this.arch.ordinal() >= refArch.ordinal(); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // 'ARMv6-M' without SEV, WFE, and WFI
        public boolean supInstARMv6MReduced()
        { return this.archIsAtLeast(Arch.ARMv6M_Reduced); }

        // 'ARMv6-M' complete
        public boolean supInstARMv6MComplete()
        { return this.archIsAtLeast(Arch.ARMv6M); }

        // 'ARMv7-M' minimal ('ARMv8-M Baseline' only support a few 'ARMv7-M' opcodes)
        public boolean supInstARMv7MMinimal()
        { return this.archIsAtLeast(Arch.ARMv7M); }

        // 'ARMv7-M' complete
        public boolean supInstARMv7MComplete()
        { return this.archIsAtLeast(Arch.ARMv7M) && !this.archIsEqualTo(Arch.ARMv8M_Baseline); }

        // 'ARMv8-M Baseline' complete
        public boolean supInstARMv8MBaseline()
        { return this.archIsAtLeast(Arch.ARMv8M_Baseline); }

        // 'ARMv8-M Mainline' complete
        public boolean supInstARMv8MMainline()
        { return this.archIsAtLeast(Arch.ARMv8M_Mainline); }

    } //  enum CPU

    public static enum Shift {
        ASR, LSL, LSR, ROR, RRX
    };

    public static enum SYSm {

        APSR       (0b00000000),
        IAPSR      (0b00000001),
        EAPSR      (0b00000010),
        XPSR       (0b00000011),
        IPSR       (0b00000101),
        EPSR       (0b00000110),
        IEPSR      (0b00000111),
        MSP        (0b00001000),
        PSP        (0b00001001),
        MSPLIM     (0b00001010), // ARMv8-M and later only
        PSPLIM     (0b00001011), // ARMv8-M and later only
        PRIMASK    (0b00010000),
        BASEPRI    (0b00010001), // ARMv7-M and later only
        BASEPRI_MAX(0b00010010), // ARMv7-M and later only
        FAULTMASK  (0b00010011), // ARMv7-M and later only
        CONTROL    (0b00010100),
        PAC_KEY_P_0(0b00100000), // ARMv8-M and later only
        PAC_KEY_P_1(0b00100001), // ARMv8-M and later only
        PAC_KEY_P_2(0b00100010), // ARMv8-M and later only
        PAC_KEY_P_3(0b00100011), // ARMv8-M and later only
        PAC_KEY_U_0(0b00100100), // ARMv8-M and later only
        PAC_KEY_U_1(0b00100101), // ARMv8-M and later only
        PAC_KEY_U_2(0b00100110), // ARMv8-M and later only
        PAC_KEY_U_3(0b00100111)  // ARMv8-M and later only

        ;

        public final int value;

        private SYSm(final int value_) { value = value_; }

        private boolean _checkCPUSupport(final CPU cpu)
        {
            if(this != BASEPRI && this != BASEPRI_MAX && this != FAULTMASK) return true;
            return cpu.archIsAtLeast(Arch.ARMv7M);
        }

    } // enum SYSm

    public static enum Cond {

        EQ( 0), // 0b0000   Equal                            Z == 1
        NE( 1), // 0b0001   Not Equal                        Z == 0

        ZR( 0), // 0b0000   Zero     (Alias for Equal)       Z == 1
        NZ( 1), // 0b0001   Not Zero (Alias for Not Equal)   Z == 0

        HI( 8), // 0b1000   Unsigned Higher                  C == 1 && Z == 0
        HS( 2), // 0b0010   Unsigned Higher or Same          C == 1
        LO( 3), // 0b0011   Unsigned Lower                   C == 0
        LS( 9), // 0b1001   Unsigned Lower or Same           C == 0 || Z == 0

        GT(12), // 0b1100   Signed Greater than              Z == 0 && N == V
        GE(10), // 0b1010   Signed Greater than or Equal     N == V
        LT(11), // 0b1011   Signed Less than                 N != V
        LE(13), // 0b1101   Signed Less than or Equal        Z == 1 || N != V

        CS( 2), // 0b0010   Carry Set                        C == 1
        CC( 3), // 0b0011   Carry Clear                      C == 0
        MI( 4), // 0b0100   Minus, Negative                  N == 1
        PL( 5), // 0b0101   Plus , Positive/Zero             N == 0
        VS( 6), // 0b0110   Overflow                         V == 1
        VC( 7), // 0b0111   No Overflow                      V == 0

        _NONE_(-1)

        ;

        public final int value;

        private Cond(final int value_)
        { value = value_; }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private Cond itCond(final boolean state)
        {
            switch(this) {

                case EQ : return state ? EQ : NE;
                case NE : return state ? NE : EQ;

                case ZR : return state ? EQ : NE; // Alias for Equal
                case NZ : return state ? NE : EQ; // Alias for Not Equal

                case HI : return state ? HI : LS;
                case HS : return state ? HS : LO;
                case LO : return state ? LO : HS;
                case LS : return state ? LS : HI;

                case GT : return state ? GT : LE;
                case GE : return state ? GE : LT;
                case LT : return state ? LT : GE;
                case LE : return state ? LE : GT;

                case CS : return state ? CS : CC;
                case CC : return state ? CC : CS;
                case MI : return state ? MI : PL;
                case PL : return state ? PL : MI;
                case VS : return state ? VS : VC;
                case VC : return state ? VC : VS;

            } // switch

            return _NONE_;
        }

        private static Cond itCond(final Cond cond, final boolean state)
        { return cond.itCond(state); }

    } // enum Cond

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Undecorated and decorated registers
    @package_private
    static class RegGen {

        public final long  regNum;
        public final RegWB wb;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        protected static final long WBOffset = 0x00FF000000000000L;

        protected RegGen(final long regNum_, final RegWB regWB)
        {
            regNum = regNum_;
            wb     = regWB;
        }

        protected RegGen(final long regNum)
        { this( regNum, new RegWB(regNum + WBOffset) ); }

        protected boolean _isRegLow()
        { return regNum <= 7; }

        protected boolean _isRegHigh()
        { return regNum >= 8; }

        protected boolean _isWB()
        { return false; }

        protected RegGen _unWB()
        { return this; }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        protected String _name()
        {
            final boolean isWB  = _isWB();
            final String  wbStr = isWB ? "!" : "";
            final int     rNum  = (int) ( isWB ? (regNum - WBOffset) : regNum );

            switch(rNum) {
                case 10 : return "SL"        + wbStr;
                case 11 : return "FP"        + wbStr;
                case 12 : return "IP"        + wbStr;
                case 13 : return "SP"        + wbStr;
                case 14 : return "LR"        + wbStr;
                case 15 : return "PC"        + wbStr;
                default : return "R"  + rNum + wbStr;
            }
        }

    } // class RegGen

    // Undecorated registers only
    public static final class Reg extends RegGen {

        public static final Reg R0  = new Reg( 0);
        public static final Reg R1  = new Reg( 1);
        public static final Reg R2  = new Reg( 2);
        public static final Reg R3  = new Reg( 3);
        public static final Reg R4  = new Reg( 4);
        public static final Reg R5  = new Reg( 5);
        public static final Reg R6  = new Reg( 6);
        public static final Reg R7  = new Reg( 7);
        public static final Reg R8  = new Reg( 8);
        public static final Reg R9  = new Reg( 9);
        public static final Reg R10 = new Reg(10); public static final Reg SL = R10;
        public static final Reg R11 = new Reg(11); public static final Reg FP = R11;
        public static final Reg R12 = new Reg(12); public static final Reg IP = R12;
        public static final Reg R13 = new Reg(13); public static final Reg SP = R13;
        public static final Reg R14 = new Reg(14); public static final Reg LR = R14;
        public static final Reg R15 = new Reg(15); public static final Reg PC = R15;

        private Reg(final long regNum)
        { super(regNum); }

    } // class Reg

    // Decorated registers only - 'Rx!'
    @package_private
    static final class RegWB extends RegGen {

        @package_private
        RegWB(final long regNum)
        { super(regNum, null); }

        @Override
        protected boolean _isRegLow()
        { return false; }

        @Override
        protected boolean _isRegHigh()
        { return false; }

        @Override
        protected boolean _isWB()
        { return true; }

        @Override
        protected Reg _unWB()
        { return new Reg(regNum - WBOffset); }

    } // class RegWB

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum CoProc {

        P0 ( 0),
        P1 ( 1),
        P2 ( 2),
        P3 ( 3),
        P4 ( 4),
        P5 ( 5),
        P6 ( 6),
        P7 ( 7),
        P8 ( 8),
        P9 ( 9),
        P10(10),
        P11(11),
        P12(12),
        P13(13),
        P14(14),
        P15(15)

        ;

        public final int value;

        private CoProc(final int value_)
        { value = value_; }

    } // enum CoProc

    public static enum CReg {

        C0 ( 0),
        C1 ( 1),
        C2 ( 2),
        C3 ( 3),
        C4 ( 4),
        C5 ( 5),
        C6 ( 6),
        C7 ( 7),
        C8 ( 8),
        C9 ( 9),
        C10(10),
        C11(11),
        C12(12),
        C13(13),
        C14(14),
        C15(15)

        ;

        public final int value;

        private CReg(final int value_)
        { value = value_; }

    } // enum CReg

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final CPU                         _cpu;

    private final ArrayList <Byte           > _data       = new ArrayList<> ();
    private final HashMap   <Long  , Long   > _dataVM     = new HashMap  <> ();

    private final OpCodeBuffer                _opcode     = new OpCodeBuffer();
    private final HashMap   <String, OpCode > _labels     = new HashMap  <> ();
    private final HashMap   <String, Long   > _labelsAddr = new HashMap  <> ();
    private final HashMap   <String, Boolean> _lfuncs     = new HashMap  <> ();

    private       Cond                        _itFCond    = Cond._NONE_;
    private       String                      _itSCond    = null;
    private       int                         _itICond    = -1;
    private final LinkedList<Boolean        > _itBlock    = new LinkedList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ARMCortexMThumb(final CPU cpu)
    { _cpu = cpu; }

    public ARMCortexMThumb()
    { this(CPU.M0); }

    public long[] link(final long originAddress, final boolean dumpDisassemblyAndArray)
    {
        try {
            // Link and clear
            final long[] res = _link_impl(originAddress, dumpDisassemblyAndArray);
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

    public long[] link(final long originAddress)
    { return link(originAddress, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb _label(final String name_, final boolean isFunction) throws JXMAsmError
    {
        _MNEMONIC_STRING( "_label", name_, isFunction );

        if( _opcode.isEmpty() ) _errorInvalidTargetLabelLocation(_MSTR);

        final String name   = _checkTargetLabel(_MSTR, name_);
        final OpCode opcode = _opcode.get( _opcode.size() - 1 );

        if( _labels.get(name) != null ) _errorDuplicatedTargetLabel(_MSTR);

        _labels.put(name, opcode    );
        _lfuncs.put(name, isFunction);

        if(ForceDumpInsertion) SysUtil.stdDbg().printf("[%s] [T^%08X] %s\n", isFunction ? "FLABEL" : "JLABEL", (opcode == null) ? 0 : opcode.address + opcode.bitSize / 8, name);

        _MSTR = null;

        return this;
    }

    public ARMCortexMThumb label(final String name) throws JXMAsmError
    { return _label(name                   , false); }

    public long resolvedLabelAddress(final String name)
    {
        final Long addr = _labelsAddr.get(name);

        return (addr != null) ? addr : -1;
    }

    public ARMCortexMThumb function(final String name) throws JXMAsmError
    { return _label(name                   , true ); }

    public ARMCortexMThumb function_NMI_Handler() throws JXMAsmError
    { return _label(Label_NMI_Handler      , true ); }

    public ARMCortexMThumb function_HardFault_Handler() throws JXMAsmError
    { return _label(Label_HardFault_Handler, true ); }

    public ARMCortexMThumb function_Start() throws JXMAsmError
    { return _label(Label_Start            , true ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _errorInvalidInstruction(final String mString, final String ins) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidInstruction, ClassName, mString, ins, _cpu.name); }

    private void _errorInvalidInstructionForm(final String mString, final String ins) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidInstructionF, ClassName, mString, ins, _cpu.name); }

    private void _errorInvalidImmediateValue(final String mString, final long value) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidImmValue, ClassName, mString, value, _cpu.name); }

    private static void _errorInvalidConstantAfterFixup(final String mString, final long value) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidConstAFixup, ClassName, mString, value, value); }

    private static void _errorInvalidAlignmentFactor(final String mString, final int factor) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidAlignFactor, ClassName, mString, factor); }

    private static void _errorInvalidBitShiftFactor(final String mString, final int factor) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidBShiftFactor, ClassName, mString, factor); }

    private static void _errorInvalidShiftedRegisterOperand(final String mString, final String operand) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidShiftRegOpr, ClassName, mString, operand); }

    private static void _errorInvalidShiftedRegisterExpression(final String mString, final String operand, final int imm5) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidShiftRegExpr, ClassName, mString, operand, imm5); }

    private static void _errorIncorrectConditionInITBlock(final String mString, final int index, final Cond cond, final String desc) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InsLoc_IncorrectCond, ClassName, mString, index, cond.name(), desc ); }

    private static void _errorInvalidInstructionOutsideITBlock(final String mString, final String operand) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InsLoc_OutsideIT, ClassName, mString, operand); }

    private static void _errorInvalidInstructionInsideITBlock(final String mString, final String operand) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InsLoc_InsideIT, ClassName, mString, operand); }

    private static void _errorInvalidInstructionNotLastInsideITBlock(final String mString, final String operand) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InsLoc_LastInsideIT, ClassName, mString, operand); }

    private static void _errorAddressOffsetNotAligned2B(final String mString, final long offset) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_AddrOfstNotAlign2B, ClassName, mString, offset & 0xFFFFFFFFL, offset); }

    private static void _errorAddressOffsetNotAligned4B(final String mString, final long offset) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_AddrOfstNotAlign4B, ClassName, mString, offset & 0xFFFFFFFFL, offset); }

    private static void _errorAddressOffsetNotAligned8B(final String mString, final long offset) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_AddrOfstNotAlign8B, ClassName, mString, offset & 0xFFFFFFFFL, offset); }

    private static void _errorAddressOffsetNotAligned16B(final String mString, final long offset) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_AddrOfstNotAlign16B, ClassName, mString, offset & 0xFFFFFFFFL, offset); }

    private static void _errorAddressOffsetValueOutOfRange(final String mString, final long offset) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_AddrOfstVOutOfRange, ClassName, mString, offset & 0xFFFFFFFFL, offset); }

    private static void _errorInvalidRegister(final String mString, final RegGen reg, final String regReg) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidReg, ClassName, mString, reg._name(), regReg ); }

    private static void _errorInvalidRegisterL(final String mString, final RegGen reg) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidRegL, ClassName, mString, reg._name() ); }

    private static void _errorInvalidRegisterH(final String mString, final RegGen reg) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidRegH, ClassName, mString, reg._name() ); }

    private static void _errorInvalidRegisterInList(final String mString, final RegGen reg) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidRegInList, ClassName, mString, reg._name() ); }

    private static void _errorInvalidRegisterNotInList(final String mString, final RegGen reg) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidRegNotInList, ClassName, mString, reg._name() ); }

    private static void _errorInvalidRegisterBothLRAndPCInList(final String mString) throws JXMAsmError
    { throw XCom.newJXMAsmError( Texts.ASM_InvalidRegLRPCInList, ClassName, mString ); }

    private static void _errorEmptyTargetLabel(final String mString) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_EmptyTargetLabel, ClassName, mString); }

    private static void _errorDuplicatedTargetLabel(final String mString) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_DuplicateTargetLabel, ClassName, mString); }

    private static void _errorInvalidTargetLabelLocation(final String mString) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidTargetLabelLc, ClassName, mString); }

    private static void _errorInvalidTargetLabelTypeFunction(final String mString, final String label) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidTargetLabelFn, ClassName, mString, label); }

    private static void _errorInvalidTargetLabel(final String mString, final String label) throws JXMAsmError
    { throw XCom.newJXMAsmError(Texts.ASM_InvalidTargetLabel, ClassName, mString, label); }

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

    private static AoS _arrayToString(final String format, final long[] values)
    { return _arrayToString( format, Arrays.stream(values).boxed().toArray(Long   []::new) ); }

    private static AoS _a2s02X(final int[] values)
    { return _arrayToString("0x%02X", values); }

    private static AoS _a2s04X(final int[] values)
    { return _arrayToString("0x%04X", values); }

    private static AoS _a2s06X(final int[] values)
    { return _arrayToString("0x%06X", values); }

    private static AoS _a2s08X(final long[] values)
    { return _arrayToString("0x%08X", values); }

    private static AoS _a2sS(final String[] values)
    { return _arrayToString("%s", values); }

    private static AoS _a2sS(final Reg[] values)
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
            else if(e instanceof Reg[]) {
                for(final Reg _e : (Reg[]) e) {
                    _sbMnemonicString.append( "Reg."     );
                    _sbMnemonicString.append( _e._name() );
                    _sbMnemonicString.append( ','        );
                }
                _sbMnemonicString.deleteCharAt( _sbMnemonicString.length() - 1 );

            }
            else if(e instanceof Reg) {
                final Reg _e = (Reg) e;
                _sbMnemonicString.append( "Reg."     );
                _sbMnemonicString.append( _e._name() );
            }
            else if(e instanceof Enum[]) {
                for(final Enum _e : (Enum[]) e) {
                    _sbMnemonicString.append( _e.getClass().getSimpleName() );
                    _sbMnemonicString.append( '.'                           );
                    _sbMnemonicString.append( _e.name()                     );
                    _sbMnemonicString.append( ','                           );
                }
                _sbMnemonicString.deleteCharAt( _sbMnemonicString.length() - 1 );

            }
            else if(e instanceof Enum) {
                final Enum _e = (Enum) e;
                _sbMnemonicString.append( _e.getClass().getSimpleName() );
                _sbMnemonicString.append( '.'                           );
                _sbMnemonicString.append( _e.name()                     );
            }
            else {
                _sbMnemonicString.append( e.toString() );
            }
            _sbMnemonicString.append( (i == 0) ? '(' : ',' );

        } // for

        _sbMnemonicString.deleteCharAt( _sbMnemonicString.length() - 1 );
        if(els.length > 1) _sbMnemonicString.append(')');

        // Store the final string
        _MSTR = _sbMnemonicString.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _checkAOffset(final String mString, final long offset, final int bitShiftFactor) throws JXMAsmError
    {
             if(bitShiftFactor == 0) {                                                                                               }
        else if(bitShiftFactor == 1) { if( (offset & 0x00000001L) != 0 ) _errorAddressOffsetNotAligned2B (mString, offset        ); }
        else if(bitShiftFactor == 2) { if( (offset & 0x00000003L) != 0 ) _errorAddressOffsetNotAligned4B (mString, offset        ); }
        else if(bitShiftFactor == 3) { if( (offset & 0x00000007L) != 0 ) _errorAddressOffsetNotAligned8B (mString, offset        ); }
        else if(bitShiftFactor == 4) { if( (offset & 0x0000000FL) != 0 ) _errorAddressOffsetNotAligned16B(mString, offset        ); }
        else                         {                                   _errorInvalidBitShiftFactor     (mString, bitShiftFactor); }
    }

    private static long _checkUOffset(final String mString, final long offset, final int bitShiftFactor, final int maxNumBits) throws JXMAsmError
    {
        _checkAOffset(mString, offset, bitShiftFactor);

        final long maxVal = ( 1L << (maxNumBits + bitShiftFactor) ) - (1L << bitShiftFactor);

        if(offset < 0     ) _errorAddressOffsetValueOutOfRange(mString, offset);
        if(offset > maxVal) _errorAddressOffsetValueOutOfRange(mString, offset);

        return (offset >>> bitShiftFactor) & ( ( 1L << (maxNumBits + bitShiftFactor) ) - 1L );
    }

    private static long _checkSOffset(final String mString, final long offset, final int bitShiftFactor, final int maxNumBits) throws JXMAsmError
    {
        _checkAOffset(mString, offset, bitShiftFactor);

        final long minVal = -( 1L << (maxNumBits + bitShiftFactor - 1) )                         ;
        final long maxVal =  ( 1L << (maxNumBits + bitShiftFactor - 1) ) - (1L << bitShiftFactor);

        if(offset < minVal || offset > maxVal) _errorAddressOffsetValueOutOfRange(mString, offset);

        // NOTE : If 'bitShiftFactor' is zero, ARM stores the sign bit in a location separate from the value!
        return (offset >> bitShiftFactor) & ( ( 1L << (maxNumBits + bitShiftFactor - 1) ) - 1L );
    }

    private static String _checkTargetLabel(final String mString, final String label_) throws JXMAsmError
    {
        final String label = (label_ == null) ? "" : label_.trim();

        if( label.isEmpty() )_errorEmptyTargetLabel(mString);

        return label;
    }

    private static void _checkReg_SP(final String mString, final RegGen reg) throws JXMAsmError
    { if(reg.regNum != Reg.SP.regNum) _errorInvalidRegister(mString, reg, "SP"); }

    private static void _checkReg_PC(final String mString, final RegGen reg) throws JXMAsmError
    { if(reg.regNum != Reg.PC.regNum) _errorInvalidRegister(mString, reg, "PC"); }

    private static void _checkReg_SP_PC(final String mString, final RegGen reg) throws JXMAsmError
    { if(reg.regNum != Reg.SP.regNum && reg.regNum != Reg.PC.regNum) _errorInvalidRegister(mString, reg, "SP/PC"); }

    private static void _checkReg_Not_SP(final String mString, final RegGen reg) throws JXMAsmError
    { if(reg.regNum == Reg.SP.regNum) _errorInvalidRegister(mString, reg, "not SP"); }

    private static void _checkReg_Not_PC(final String mString, final RegGen reg) throws JXMAsmError
    { if(reg.regNum == Reg.PC.regNum) _errorInvalidRegister(mString, reg, "not PC"); }

    private static void _checkReg_Not_SP_PC(final String mString, final RegGen reg) throws JXMAsmError
    { if(reg.regNum == Reg.SP.regNum || reg.regNum == Reg.PC.regNum) _errorInvalidRegister(mString, reg, "not SP/PC"); }

    private static void _checkReg_Low(final String mString, final RegGen reg) throws JXMAsmError
    { if( !reg._isRegLow () ) _errorInvalidRegisterL(mString, reg); }

    private static void _checkReg_High(final String mString, final RegGen reg) throws JXMAsmError
    { if( !reg._isRegHigh() ) _errorInvalidRegisterH(mString, reg); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _markPCRelLabel(final String label, final int offset)
    {
        final char pcf = (offset == 4) ? '4'
                       : (offset == 2) ? '2'
                       :                 ( ( _opcode.getNextAddress() & 0x03 ) == 0 ) ? '4' : '2';

        return "PC:" + pcf + ":" + label;
    }

    private String _markPCRelLabel(final String label)
    { return _markPCRelLabel(label, -1); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static long _swapU32___U16_U16(final long value)
    { return ( (value & 0xFFFF) << 16 ) | ( ( value >> 16) & 0xFFFF ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static long _encodeAddress20___S_IMM6_J1_J2_IMM11(final OpCode opcode, final long address) throws JXMAsmError
    {
        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *  ?  ?  ?  ?  ?  S  ?  ?  ?  ? ----- imm6 ------  ?  ? J1  ? J2 ------------ imm11 -------------
         *
         *  imm32 = SignExtend(S:J2:J1:imm6:imm11:0, 32);
         */

        final long adr   = _checkSOffset(opcode.mString, address, 1, 20);
        final long S     = ( adr & 0b00000000000010000000000000000000L ) >> 19;
        final long imm6  = ( adr & 0b00000000000001111110000000000000L ) >> 13;
        final long J2    = ( adr & 0b00000000000000000001000000000000L ) >> 12;
        final long J1    = ( adr & 0b00000000000000000000100000000000L ) >> 11;
        final long imm11 = ( adr & 0b00000000000000000000011111111111L ) >>  0;
        final long opar  = (S << 26) | (imm6 << 16) | (J1 << 13) | (J2 << 11) | imm11;

        return opar;
    }

    private static long _encodeAddress24___S_IMM10_J1_J2_IMM11(final OpCode opcode, final long address) throws JXMAsmError
    {
        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *  ?  ?  ?  ?  ?  S ----------- imm10 -----------  ?  ? J1  ? J2 ------------ imm11 -------------
         *
         *  I1    = ~(J1 ^ S)
         *  I2    = ~(J2 ^ S)
         *  imm32 = SignExtend(S:I1:I2:imm10:imm11:0, 32);
         */

        final long adr   = _checkSOffset(opcode.mString, address, 1, 24);
        final long S     = ( adr & 0b00000000100000000000000000000000L ) >> 23;
        final long I1    = ( adr & 0b00000000010000000000000000000000L ) >> 22;
        final long I2    = ( adr & 0b00000000001000000000000000000000L ) >> 21;
        final long imm10 = ( adr & 0b00000000000111111111100000000000L ) >> 11;
        final long imm11 = ( adr & 0b00000000000000000000011111111111L ) >>  0;
        final long J1    = (~I1 & 0x01L) ^ S;
        final long J2    = (~I2 & 0x01L) ^ S;
        final long opar  = (S << 26) | (imm10 << 16) | (J1 << 13) | (J2 << 11) | imm11;

        /*
        SysUtil.stdDbg().printf("%d [%08X] -> %d [%d] | %X %X %X %03X %03X [%08X] [%08X]\n", opcode.address, opcode.address, address, adr, S, J1, J2, imm10, imm11, opcode.opcodeValue, opar);
        //*/

        return opar;
    }

    private static long[] _encodeShiftAppliedToRegister___IMM_TYPE(final String mString, final Shift shift, final int imm5) throws JXMAsmError
    {
        if(imm5 < 0) {
            if(shift != Shift.RRX) _errorInvalidShiftedRegisterExpression( mString, shift.name(), imm5 );
        }
        else {
            if(shift == Shift.RRX) _errorInvalidShiftedRegisterExpression( mString, shift.name(), imm5 );
        }

        long imm  = 0;
        long type = 0;

        switch(shift) {
            case LSL : type = 0b00;
                       imm  = _checkUOffset(mString, imm5, 0, 5);
                       break;
            case LSR : type = (imm5 ==  0) ? 0b00 : 0b01;
                       imm  = (imm5 == 32) ? 0    : _checkUOffset(mString, imm5, 0, 5);
                       break;
            case ASR : type = (imm5 ==  0) ? 0b00 : 0b10;
                       imm  = (imm5 == 32) ? 0    : _checkUOffset(mString, imm5, 0, 5);
                       break;
            case ROR : type = (imm5 ==  0) ? 0b00 : 0b11;
                       imm  = _checkUOffset(mString, imm5, 0, 5);
                       break;
            case RRX : type = 0b11;
                       imm  = 0;
                       break;
       }

       return new long[] { imm, type };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static long _fixupConstant32___I_IMM3_ABCDEFGH(final long imm32)
    {
        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *                 i                                  - imm3 -              a  b  c  d  e  f  g  h
         */

         // Work buffer
         final long b3 = (imm32 & 0xFF000000L) >> 24;
         final long b2 = (imm32 & 0x00FF0000L) >> 16;
         final long b1 = (imm32 & 0x0000FF00L) >>  8;
         final long b0 = (imm32 & 0x000000FFL) >>  0;

               long i        = 0;
               long imm3     = 0;
               long abcdefgh = 0;

         /*           b3       b2       b1       b0
          * Check for 00000000 00000000 00000000 abcdefgh
          */
         if(b3 == 0 && b2 == 0 && b1 == 0) {
             i        = 0;
             imm3     = 0b000;
             abcdefgh = b0;
         }
         /*           b3       b2       b1       b0
          * Check for 00000000 abcdefgh 00000000 abcdefgh
          */
         else if(b3 == 0 && b2 == b0 && b1 == 0) {
             i        = 0;
             imm3     = 0b001;
             abcdefgh = b0;
         }
         /*           b3       b2       b1       b0
          * Check for abcdefgh 00000000 abcdefgh 00000000
          */
         else if(b3 == b1 && b2 == 0 && b0 == 0) {
             i        = 0;
             imm3     = 0b010;
             abcdefgh = b1;
         }
         /*           b3       b2       b1       b0
          * Check for abcdefgh abcdefgh abcdefgh abcdefgh
          */
         else if(b3 == b2 && b3 == b1 && b3 == b0) {
             i        = 0;
             imm3     = 0b011;
             abcdefgh = b0;
         }
         /*           b3       b2       b1       b0
          * Check for 1bcdefgh 00000000 00000000 00000000
          *           01bcdefg h0000000 00000000 00000000
          *           001bcdef gh000000 00000000 00000000
          *           001bcdef gh000000 00000000 00000000
          *           ...
          *           00000000 h0000000 000001bc defgh000
          *           00000000 h0000000 0000001b cdefgh00
          *           00000000 h0000000 00000001 bcdefgh0
          */
          else {
              // Check for match
              boolean match = false;
              for(int idx = 31; idx >= 8; --idx) {
                  // Caclulate the bitmasks
                  final long one = (    1L          <<  idx                      );
                  final long chk = ( ~( 0b11111111L << (idx - 7) ) & 0xFFFFFFFFL );
                  // Skip if it does not match the pattern
                  if( ( (imm32 & one) != one ) || ( (imm32 & chk) != 0 ) ) continue;
                  // It matches the pattern, calculate the result
                  final long i_imm3_a = 31 + 8 - idx;
                             i        = (i_imm3_a & 0b10000) >> 4;
                             imm3     = (i_imm3_a & 0b01110) >> 1;
                  final long a        = (i_imm3_a & 0b00001) << 7;
                  abcdefgh = a | ( ( imm32 >> (idx - 8 + 1) ) & 0b01111111L );
                  match    = true;
                  break;
              }
              // No match
              if(!match) return -1;
          }

         // Return the result
         return (i << 26) | (imm3 << 12) | abcdefgh;
    }

    private static long _splitImmediate12___I_IMM3_IMM8(final String mString, final long imm12) throws JXMAsmError
    {
        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *                 i                                  - imm3 -             -------- imm8 ---------
         */

        final long imm   = _checkUOffset(mString, imm12, 0, 12);
        final long i     = ( imm & 0b0000100000000000 ) >> 11;
        final long imm3  = ( imm & 0b0000011100000000 ) >>  8;
        final long imm8  = ( imm & 0b0000000011111111 ) >>  0;
        final long oimm  = (i << 26) | (imm3 << 12) | imm8;

        return oimm;
    }

    private static long _splitImmediate5___IMM3_IMM2(final String mString, final long imm5) throws JXMAsmError
    {
        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *                                                    - imm3 -             imm2
         */

        final long imm  = _checkUOffset(mString, imm5, 0, 5);
        final long imm3 = ( imm & 0b0000000000011100 ) >> 3;
        final long imm2 = ( imm & 0b0000000000000011 ) >> 0;
        final long oimm = (imm3 << 12) | (imm2 << 6);

        return oimm;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _errorIfOutsideITBlock(final String mString, final String operand) throws JXMAsmError
    { if(  _itBlock.isEmpty() ) _errorInvalidInstructionOutsideITBlock(mString, operand); }

    private void _errorIfInsideITBlock(final String mString, final String operand) throws JXMAsmError
    { if( !_itBlock.isEmpty() ) _errorInvalidInstructionInsideITBlock(mString, operand); }

    private void _errorIfNotLastInsideITBlock(final String mString, final String operand) throws JXMAsmError
    { if( !_itBlock.isEmpty() && _itBlock.size() != 1 ) _errorInvalidInstructionNotLastInsideITBlock(mString, operand); }

    protected boolean insideITBlock()
    { return !_itBlock.isEmpty(); }

    protected ARMCortexMThumb __itcd__(final String opcode, final Cond cond, final ITCondDispatch itcd, final Object... operands) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$" + opcode + cond.name().toLowerCase(), operands );

        if( _itBlock.isEmpty() ) _errorIfOutsideITBlock( _MSTR, "$*<c>" );

        if( _itFCond.itCond( _itBlock.get(0) ) != cond ) _errorIncorrectConditionInITBlock( _MSTR, _itICond + 1, cond, "IT" + _itSCond + " " + _itFCond.name() );

        return itcd.dispatch();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _putOpCode_removeITBlockItem()
    {
        if( _itBlock.isEmpty() ) return;

        _itBlock.removeFirst();
        ++_itICond;

        if( _itBlock.isEmpty() ) {
            _itFCond = Cond._NONE_;
            _itSCond = null;
            _itICond = -1;
        }
    }

    private void _putOpCodeIT(final long opcodeValue)
    {
        _opcode.add( new OpCode(_opcode, _MSTR, 16, opcodeValue) );
        _MSTR = null;
    }

    private void _putOpCode16(final long opcodeValue)
    {
        _opcode.add( new OpCode(_opcode, _MSTR, 16, opcodeValue) );
        _putOpCode_removeITBlockItem();
        _MSTR = null;
    }

    private void _putOpCode16(final long opcodeValue, final long paramDataAddress, final String paramLabel, final int paramBitShiftPos, final int valueBitShiftFactor, final int valueMaxNumBits)
    {
        _opcode.add( new OpCode(_opcode, _MSTR, 16, opcodeValue, paramDataAddress, paramLabel, paramBitShiftPos, valueBitShiftFactor, valueMaxNumBits) );
        _putOpCode_removeITBlockItem();
        _MSTR = null;
    }

    private void _putOpCode16(final long opcodeValue, final String paramLabel, final int paramBitShiftPos, final int valueBitShiftFactor, final int valueMaxNumBits)
    {
        _opcode.add( new OpCode(_opcode, _MSTR, 16, opcodeValue, paramLabel, paramBitShiftPos, valueBitShiftFactor, valueMaxNumBits) );
        _putOpCode_removeITBlockItem();
        _MSTR = null;
    }

    private void _putOpCode16(final long opcodeValue, final String paramLabel, final OCPF paramFunc)
    {
        _opcode.add( new OpCode(_opcode, _MSTR, 16, opcodeValue, paramLabel, paramFunc) );
        _putOpCode_removeITBlockItem();
        _MSTR = null;
    }

    private void _putOpCode32(final long opcodeValue)
    {
        _opcode.add( new OpCode(_opcode, _MSTR, 32, opcodeValue) );
        _putOpCode_removeITBlockItem();
        _MSTR = null;
    }

    private void _putOpCode32(final long opcodeValue, final String paramLabel, final OCPF paramFunc)
    {
        _opcode.add( new OpCode(_opcode, _MSTR, 32, opcodeValue, paramLabel, paramFunc) );
        _putOpCode_removeITBlockItem();
        _MSTR = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long _putData32(final long value)
    {
        // Check whether the same value is already stored
        final Long exVM = _dataVM.get(value);

        if(exVM != null) return exVM.longValue(); // Return the address of the stored value

        // Determine the address of the new value
        final long address = _data.size();

        // Store the value
        _data.add( (byte) ( (value >>  0) & 0xFF ) );
        _data.add( (byte) ( (value >>  8) & 0xFF ) );
        _data.add( (byte) ( (value >> 16) & 0xFF ) );
        _data.add( (byte) ( (value >> 24) & 0xFF ) );

        // Store the value map
        _dataVM.put(value, address);

        // Return the address of the newly stored value
        return address;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // .align
    public ARMCortexMThumb $_align(final int factor_) throws JXMAsmError
    {
        // ##### !!! TODO : Further verify this !!! #####

        if(factor_ <= 1) return this;

        final int factor  = 1 << (factor_);
        final int factor1 = factor - 1;

        final long na   = _opcode.getNextAddress();
              long pad  = ( (na + factor1) & ~factor1 ) - na;

        /*
        SysUtil.stdDbg().printf("%d=>%d : na=%X (%d) ; pad=%d\n", factor_, factor1, na, na, pad);
        //*/

        while(pad != 0) {
            if( (pad & 0x01) != 0 ) {
                $_byte(0);
                --pad;
            }
            else {
                $nop();
                pad -= 2;
            }
        }

        return this;
    }

    // .align 2
    public ARMCortexMThumb $_align2() throws JXMAsmError
    { return $_align(2); }

    // .align 4
    public ARMCortexMThumb $_align4() throws JXMAsmError
    { return $_align(4); }

    // .align 8
    public ARMCortexMThumb $_align8() throws JXMAsmError
    { return $_align(8); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // .byte
    public ARMCortexMThumb $_byte(final int... values) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_byte", _a2s02X(values) );

        for(final int value : values) {
            _checkUOffset(_MSTR, value, 0, 8);
            _opcode.add( new OpCode(_opcode, _MSTR, 8, value) );
        }

        _MSTR = null;

        return this;
    }

    // .short
    public ARMCortexMThumb $_short(final int... values) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_short", _a2s04X(values) );

        for(final int value : values) {
            _checkUOffset(_MSTR, value, 0, 16);
            _opcode.add( new OpCode(_opcode, _MSTR, 16, value) );
        }

        _MSTR = null;

        return this;
    }

    // .word
    public ARMCortexMThumb $_word(final long... values) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_word", _a2s08X(values) );

        for(final long value : values) {
            _checkUOffset(_MSTR, value, 0, 32);
            _opcode.add( new OpCode(_opcode, _MSTR, -32, value) ); // Use -32 so that the linker will not swap the half-words
        }

        _MSTR = null;

        return this;
    }

    // .word
    public ARMCortexMThumb $_word(final String... labels) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$_word", _a2sS(labels) );

        for(final String label_ : labels) {
            final String label = _checkTargetLabel(_MSTR, label_);
            _opcode.add( new OpCode(_opcode, _MSTR, -32, 0, label) ); // Use -32 so that the linker will not swap the half-words
        }

        _MSTR = null;

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ADCS <Rdn>, <Rm>
    public ARMCortexMThumb $adcs(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$adcs", Rdn, Rm );

        _checkReg_Low(_MSTR, Rdn);
        _checkReg_Low(_MSTR, Rm );
        _putOpCode16( 0b0100000101000000 | (Rm.regNum << 3) | Rdn.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ADDS <Rd>, <Rn>, #<imm3>
    public ARMCortexMThumb $adds(final Reg Rd, final Reg Rn, final int imm3) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$adds", Rd, Rn, imm3 );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rn);
        final long imm = _checkUOffset(_MSTR, imm3, 0, 3);
        _putOpCode16( 0b0001110000000000 | (imm << 6) | (Rn.regNum << 3) | Rd.regNum );
        return this;
    }

    // [T2] ADDS <Rdn>, #<imm8>
    public ARMCortexMThumb $adds(final Reg Rdn, final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$adds", Rdn, imm8 );

        _checkReg_Low(_MSTR, Rdn);
        final long imm = _checkUOffset(_MSTR, imm8, 0, 8);
        _putOpCode16( 0b0011000000000000 | (Rdn.regNum << 8) | imm );
        return this;
    }

    // [T1] ADDS <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $adds(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$adds", Rd, Rn, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0001100000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rd.regNum );
        return this;
    }

    // [T2] ADD <Rdn>, <Rm>
    // [T1] ADD <Rdm>,  SP , <Rdm>
    // [T2] ADD  SP  , <Rm>
    public ARMCortexMThumb $add(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$add", Rdn, Rm );

        _putOpCode16( 0b0100010000000000 | (Rm.regNum << 3) | ( (Rdn.regNum & 0b1000) << 4 ) | (Rdn.regNum & 0b0111) );
        return this;
    }

    // [T1] ADD <Rd>, SP, #<imm8>
    // [T2] ADD  SP , SP, #<imm7>
    public ARMCortexMThumb $add(final Reg Rd, final Reg Rn, final int imm7_imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$add", Rd, Rn, imm7_imm8 );

        if(Rn.regNum == Reg.PC.regNum) return _$adr(Rd, imm7_imm8);

        _checkReg_SP(_MSTR, Rn);

        if(Rd.regNum != Reg.SP.regNum) {
            // T1
            _checkReg_Low(_MSTR, Rd);
            final long imm = _checkUOffset(_MSTR, imm7_imm8, 2, 8);
            _putOpCode16( 0b1010100000000000 | (Rd.regNum << 8) | imm );
        }

        else {
            // T2
            final long imm = _checkUOffset(_MSTR, imm7_imm8, 2, 7);
            _putOpCode16( 0b1011000000000000 | imm );
        }

        return this;
    }

    // [PI] ADD SP, SP, #<imm7>
    public ARMCortexMThumb $add_sp(final int imm7) throws JXMAsmError
    { return $add(Reg.SP, Reg.SP, imm7); }

    // [T4] ADDW <Rd>, <Rn>, #<imm12> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $addw(final Reg Rd, final Reg Rn, final int imm12) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$addw", Rd, Rn, imm12 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstruction(_MSTR, "$addw");

        final long imm = _splitImmediate12___I_IMM3_IMM8(_MSTR, imm12);
        _putOpCode32( 0b11110010000000000000000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | imm );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ADR <Rd>, #<imm8>
    private ARMCortexMThumb _$adr(final Reg Rd, final int imm8) throws JXMAsmError
    {
        _checkReg_Low(_MSTR, Rd);
        final long imm = _checkUOffset(_MSTR, imm8, 2, 8);
        _putOpCode16( 0b1010000000000000 | (Rd.regNum << 8) | imm );
        return this;
    }

    // [T1] ADR <Rd>, <label>
    public ARMCortexMThumb $adr(final Reg Rd, final String label_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$adr", Rd, label_ );

        _checkReg_Low(_MSTR, Rd);
        final String label = _checkTargetLabel(_MSTR, label_);
        _putOpCode16( 0b1010000000000000 | (Rd.regNum << 8), _markPCRelLabel(label), 0, 2, 8 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ANDS <Rdn>, <Rm>
    public ARMCortexMThumb $ands(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ands", Rdn, Rm );

        _checkReg_Low(_MSTR, Rdn);
        _checkReg_Low(_MSTR, Rm );
        _putOpCode16( 0b0100000000000000 | (Rm.regNum << 3) | Rdn.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T2] ASR{S}.W <Rd>, <Rm>, #<imm5> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$asrs_w_impl(final boolean S, final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$asr{s}.w");

        final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
        if(S) _putOpCode32( 0b11101010010111110000000000100000L | ( (imm5 & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm5 & 0b00011) << 6 ) | Rm.regNum );
        else  _putOpCode32( 0b11101010010011110000000000100000L | ( (imm5 & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm5 & 0b00011) << 6 ) | Rm.regNum );
        return this;
    }

    // [T2] ASR{S}.W <Rd>, <Rn>, <Rm> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$asrs_w_impl(final boolean S, final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$asr{s}.w");

        if(S) _putOpCode32( 0b11111010010100001111000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        else  _putOpCode32( 0b11111010010000001111000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        return this;
    }

    // [T1] ASRS   <Rd>, <Rm>, #<imm5>
    // [T2] ASRS.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $asrs(final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$asrs", Rd, Rm, imm5 );

        // T2
        if( _cpu.supInstARMv7MComplete() && ( !Rd._isRegLow() || !Rm._isRegLow() ) ) {
            return _$asrs_w_impl(true, Rd, Rm, imm5);
        }

        // T1
        else {
            _checkReg_Low(_MSTR, Rd);
            _checkReg_Low(_MSTR, Rm);
            final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
            _putOpCode16( 0b0001000000000000 | (imm << 6) | (Rm.regNum << 3) | Rd.regNum );
            return this;
        }
    }

    // [T2] ASR.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $asr(final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$asr", Rd, Rm, imm5 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$asr.w");

        return _$asrs_w_impl(false, Rd, Rm, imm5);
    }

    // [T1] ASRS   <Rdn>, <Rm>
    // [T2] ASRS.W <Rd >, <Rn>, <Rm>
    public ARMCortexMThumb $asrs(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$asrs", Rdn, Rm );

        // T2
        if( _cpu.supInstARMv7MComplete() && ( !Rdn._isRegLow() || !Rm._isRegLow() ) ) {
            return _$asrs_w_impl(true, Rdn, Rdn, Rm);
        }

        // T1
        else {
            _checkReg_Low(_MSTR, Rdn);
            _checkReg_Low(_MSTR, Rm );
            _putOpCode16( 0b0100000100000000 | (Rm.regNum << 3) | Rdn.regNum );
            return this;
        }
    }

    // [T2] ASRS.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $asrs(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        if(Rd.regNum == Rn.regNum) return $asrs(Rd, Rm);

        _MNEMONIC_STRING( "$asrs", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$asrs.w");

        return _$asrs_w_impl(true, Rd, Rn, Rm);
    }

    // [T2] ASR.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $asr(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$asr", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$asr.w");

        return _$asrs_w_impl(false, Rd, Rn, Rm);
    }

    // [T2] ASR.W <Rdn>, <Rm>
    public ARMCortexMThumb $asr(final Reg Rdn, final Reg Rm) throws JXMAsmError
    { return $asr(Rdn, Rdn, Rm); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] B<C> #<imm8>
    private ARMCortexMThumb _$b_t1(final Cond cond, final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$b<c>", cond, imm8 );

        _errorIfInsideITBlock( _MSTR, "$b<c>" );

        final long imm = _checkSOffset(_MSTR, imm8, 1, 8);
        _putOpCode16( 0b1101000000000000 | (cond.value << 8) | imm );
        return this;
    }

    // [T1] B<C> <label>
    private ARMCortexMThumb _$b_t1(final Cond cond, final String label_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$b<c>", cond, label_ );

        _errorIfInsideITBlock( _MSTR, "$b<c>" );

        final String label = _checkTargetLabel(_MSTR, label_);
        _putOpCode16( 0b1101000000000000 | (cond.value << 8), _markPCRelLabel(label, 4), 0, -1, -8 );
        return this;
    }

    // [T2] B #<imm11>
    private ARMCortexMThumb _$b_t2(final int imm11) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$b", imm11 );

        _errorIfNotLastInsideITBlock( _MSTR, "$b" );

        final long imm = _checkSOffset(_MSTR, imm11, 1, 11);
        _putOpCode16( 0b1110000000000000 | imm );
        return this;
    }

    // [T2] B <label>
    public ARMCortexMThumb _$b_t2(final String label_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$b", label_ );

        _errorIfNotLastInsideITBlock( _MSTR, "$b" );

        final String label = _checkTargetLabel(_MSTR, label_);
        _putOpCode16( 0b1110000000000000, _markPCRelLabel(label, 4), 0, -1, -11 );
        return this;
    }

    // [T3] B<C>.W <label> ----- not available in 'ARMv8-M Baseline'
    private ARMCortexMThumb _$b_t3(final Cond cond, final String label_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$b<c>.w", cond, label_ );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$b<c>.w");

        _errorIfInsideITBlock( _MSTR, "$b<c>.w" );

        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *  1  1  1  1  0  S -- cond --- ----- imm6 ------  1  0 J1  0 J2 ------------ imm11 -------------
         */

        final String label = _checkTargetLabel(_MSTR, label_);

        _putOpCode32( 0b11110000000000001000000000000000L, _markPCRelLabel(label, 4), new OCPF() {
            @Override
            public long apply(final OpCode opcode, final long address, final int thumbBit) throws JXMAsmError
            {
                // NOTE : This instruction does not need the 'thumbBit' parameter
                return opcode.opcodeValue | (cond.value << 22) | _encodeAddress20___S_IMM6_J1_J2_IMM11(opcode, address);
            }
        } );

        return this;
    }

    // [T4] B.W <label>
    public ARMCortexMThumb _$b_t4(final String label_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$b.w", label_ );

        if( !_cpu.supInstARMv7MMinimal() ) _errorInvalidInstructionForm(_MSTR, "$b.w");

        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *  1  1  1  1  0  S ----------- imm10 -----------  1  0 J1  1 J2 ------------ imm11 -------------
         */

        final String label = _checkTargetLabel(_MSTR, label_);

        _putOpCode32( 0b11110000000000001001000000000000L, _markPCRelLabel(label, 4), new OCPF() {
            @Override
            public long apply(final OpCode opcode, final long address, final int thumbBit) throws JXMAsmError
            {
                // NOTE : This instruction does not need the 'thumbBit' parameter
                return opcode.opcodeValue | _encodeAddress24___S_IMM10_J1_J2_IMM11(opcode, address);
            }
        } );

        return this;
    }

    // [T2] B <label>
    public ARMCortexMThumb $b(final String label) throws JXMAsmError
    { return _$b_t2(label); }

    // [T4] B.W <label>
    public ARMCortexMThumb $b_w(final String label) throws JXMAsmError
    { return _$b_t4(label); }

    // [T2] B .
    public ARMCortexMThumb $b_dot() throws JXMAsmError
    { return _$b_t2(-4); }

    // [T1] BEQ <label>
    public ARMCortexMThumb $beq(final String label) throws JXMAsmError
    { return _$b_t1(Cond.EQ, label); }

    // [PI] BZR <label>
    public ARMCortexMThumb $bzr(final String label) throws JXMAsmError
    { return $beq(label); }

    // [T1] BNE <label>
    public ARMCortexMThumb $bne(final String label) throws JXMAsmError
    { return _$b_t1(Cond.NE, label); }

    // [PI] BNZ <label>
    public ARMCortexMThumb $bnz(final String label) throws JXMAsmError
    { return $bne(label); }

    // [T1] BCS <label>
    public ARMCortexMThumb $bcs(final String label) throws JXMAsmError
    { return _$b_t1(Cond.CS, label); }

    // [T1] BHS <label>
    public ARMCortexMThumb $bhs(final String label) throws JXMAsmError
    { return _$b_t1(Cond.HS, label); }

    // [T1] BCC <label>
    public ARMCortexMThumb $bcc(final String label) throws JXMAsmError
    { return _$b_t1(Cond.CC, label); }

    // [T1] BLO <label>
    public ARMCortexMThumb $blo(final String label) throws JXMAsmError
    { return _$b_t1(Cond.LO, label); }

    // [T1] BMI <label>
    public ARMCortexMThumb $bmi(final String label) throws JXMAsmError
    { return _$b_t1(Cond.MI, label); }

    // [T1] BPL <label>
    public ARMCortexMThumb $bpl(final String label) throws JXMAsmError
    { return _$b_t1(Cond.PL, label); }

    // [T1] BVS <label>
    public ARMCortexMThumb $bvs(final String label) throws JXMAsmError
    { return _$b_t1(Cond.VS, label); }

    // [T1] BVC <label>
    public ARMCortexMThumb $bvc(final String label) throws JXMAsmError
    { return _$b_t1(Cond.VC, label); }

    // [T1] BHI <label>
    public ARMCortexMThumb $bhi(final String label) throws JXMAsmError
    { return _$b_t1(Cond.HI, label); }

    // [T1] BLS <label>
    public ARMCortexMThumb $bls(final String label) throws JXMAsmError
    { return _$b_t1(Cond.LS, label); }

    // [T1] BGE <label>
    public ARMCortexMThumb $bge(final String label) throws JXMAsmError
    { return _$b_t1(Cond.GE, label); }

    // [T1] BLT <label>
    public ARMCortexMThumb $blt(final String label) throws JXMAsmError
    { return _$b_t1(Cond.LT, label); }

    // [T1] BGT <label>
    public ARMCortexMThumb $bgt(final String label) throws JXMAsmError
    { return _$b_t1(Cond.GT, label); }

    // [T1] BLE <label>
    public ARMCortexMThumb $ble(final String label) throws JXMAsmError
    { return _$b_t1(Cond.LE, label); }

    // [T3] BEQ.W <label>
    public ARMCortexMThumb $beq_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.EQ, label); }

    // [PI] BEQ.W <label>
    public ARMCortexMThumb $bzr_w(final String label) throws JXMAsmError
    { return $beq(label); }

    // [T3] BNE.W <label>
    public ARMCortexMThumb $bne_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.NE, label); }

    // [PI] BNE.W <label>
    public ARMCortexMThumb $bnz_w(final String label) throws JXMAsmError
    { return $bne(label); }

    // [T3] BCS.W <label>
    public ARMCortexMThumb $bcs_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.CS, label); }

    // [T3] BHS.W <label>
    public ARMCortexMThumb $bhs_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.HS, label); }

    // [T3] BCC.W <label>
    public ARMCortexMThumb $bcc_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.CC, label); }

    // [T3] BLO.W <label>
    public ARMCortexMThumb $blo_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.LO, label); }

    // [T3] BMI.W <label>
    public ARMCortexMThumb $bmi_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.MI, label); }

    // [T3] BPL.W <label>
    public ARMCortexMThumb $bpl_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.PL, label); }

    // [T3] BVS.W <label>
    public ARMCortexMThumb $bvs_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.VS, label); }

    // [T3] BVC.W <label>
    public ARMCortexMThumb $bvc_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.VC, label); }

    // [T3] BHI.W <label>
    public ARMCortexMThumb $bhi_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.HI, label); }

    // [T3] BLS.W <label>
    public ARMCortexMThumb $bls_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.LS, label); }

    // [T3] BGE.W <label>
    public ARMCortexMThumb $bge_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.GE, label); }

    // [T3] BLT.W <label>
    public ARMCortexMThumb $blt_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.LT, label); }

    // [T3] BGT.W <label>
    public ARMCortexMThumb $bgt_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.GT, label); }

    // [T3] BLE.W <label>
    public ARMCortexMThumb $ble_w(final String label) throws JXMAsmError
    { return _$b_t3(Cond.LE, label); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BICS <Rdn>, <Rm>
    public ARMCortexMThumb $bics(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$bics", Rdn, Rm );

        _checkReg_Low(_MSTR, Rdn);
        _checkReg_Low(_MSTR, Rm );
        _putOpCode16( 0b0100001110000000 | (Rm.regNum << 3) | Rdn.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BKPT #<imm8>
    public ARMCortexMThumb $bkpt(final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$bkpt", imm8 );

        final long imm = _checkUOffset(_MSTR, imm8, 0, 8);
        _putOpCode16( 0b1011111000000000 | imm );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BL <label>
    public ARMCortexMThumb $bl(final String label_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$bl", label_ );

        _errorIfNotLastInsideITBlock( _MSTR, "$bl" );

        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *  1  1  1  1  0  S ----------- imm10 -----------  1  1 J1  1 J2 ------------ imm11 -------------
         */

        final String label = _checkTargetLabel(_MSTR, label_);

        _putOpCode32( 0b11110000000000001101000000000000L, _markPCRelLabel(label, 4), new OCPF() {
            @Override
            public long apply(final OpCode opcode, final long address, final int thumbBit) throws JXMAsmError
            {
                // NOTE : This instruction does not need the 'thumbBit' parameter
                return opcode.opcodeValue | _encodeAddress24___S_IMM10_J1_J2_IMM11(opcode, address);
            }
        } );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BLX <Rm>
    public ARMCortexMThumb $blx(final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$blx", Rm );

        _errorIfNotLastInsideITBlock( _MSTR, "$blx" );

        _putOpCode16( 0b0100011110000000 | (Rm.regNum << 3) );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BX <Rm>
    public ARMCortexMThumb $bx(final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$bx", Rm );

        _errorIfNotLastInsideITBlock( _MSTR, "$bx" );

        _putOpCode16( 0b0100011100000000 | (Rm.regNum << 3) );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb $_cbz_cbnz(final boolean nz, final Reg Rn, final String label_) throws JXMAsmError
    {
        /*
         * 15 14 13 12 11 10 09 08 07 06 05 04 03 02 01 00
         *  1  0  1  1 nz  0  i  1 ---- imm5 ---- -- Rn --
         */

        _checkReg_Low(_MSTR, Rn);
        final String label = _checkTargetLabel(_MSTR, label_);
        _putOpCode16( (nz ? 0b1011100100000000 : 0b1011000100000000) | (Rn.regNum), _markPCRelLabel(label, 4), new OCPF() {
            @Override
            public long apply(final OpCode opcode, final long address, final int thumbBit) throws JXMAsmError
            {
                // NOTE : This instruction does not need the 'thumbBit' parameter
                final long adr   = _checkUOffset(opcode.mString, address, 1, 6);
                final long i    = ( adr & 0b0000000000100000 ) >> 5;
                final long imm5 = ( adr & 0b0000000000011111 ) >> 0;
                return opcode.opcodeValue | (i << 9) | (imm5 << 3);
            }
        } );
        return this;
    }

    // [T1] CBZ <Rn>, <label>
    public ARMCortexMThumb $cbz(final Reg Rn, final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cbz" , Rn, label );

        if( !_cpu.supInstARMv7MMinimal() ) _errorInvalidInstruction(_MSTR, "$cbz");

        _errorIfInsideITBlock( _MSTR, "$cbz" );

        return $_cbz_cbnz(false, Rn, label);
    }

    // [T1] CBNZ <Rn>, <label>
    public ARMCortexMThumb $cbnz(final Reg Rn, final String label) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cbnz" , Rn, label );

        if( !_cpu.supInstARMv7MMinimal() ) _errorInvalidInstruction(_MSTR, "$cbnz");

        _errorIfInsideITBlock( _MSTR, "$cbnz" );

        return $_cbz_cbnz(true , Rn, label);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] CLREX
    public ARMCortexMThumb $clrex() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$clrex" );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$clrex");

        _putOpCode32( 0b11110011101111111000111100101111L );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] CMN <Rn>, #<const> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $cmn(final Reg Rn, final long const_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cmn", Rn, const_ );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$cmn");

        final long imm = _fixupConstant32___I_IMM3_ABCDEFGH(const_);
        if(imm < 0) _errorInvalidConstantAfterFixup(_MSTR, const_);

        _putOpCode32( 0b11110001000100000000111100000000L | (Rn.regNum << 16) | imm );
        return this;
    }

    // [T1] CMN <Rn>, <Rm>
    public ARMCortexMThumb $cmn(final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cmn", Rn, Rm );

        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0100001011000000 | (Rm.regNum << 3) | Rn.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] CMP   <Rn>, #<imm8>
    // [T2] CMP.W <Rn>, #<const> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $cmp(final Reg Rn, final long imm8_const) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cmp", Rn, imm8_const );

        if( _cpu.supInstARMv7MComplete() && (imm8_const < 0 || imm8_const > 255) ) {

            final long imm = _fixupConstant32___I_IMM3_ABCDEFGH(imm8_const);
            if(imm < 0) _errorInvalidConstantAfterFixup(_MSTR, imm8_const);

            _putOpCode32( 0b11110001101100000000111100000000L | (Rn.regNum << 16) | imm );

            return this;

        } // if

        _checkReg_Low(_MSTR, Rn);
        final long imm = _checkUOffset(_MSTR, imm8_const, 0, 8);
        _putOpCode16( 0b0010100000000000 | (Rn.regNum << 8) | imm );
        return this;
    }

    // [T1] CMP <Rn>, <Rm>
    // [T2] CMP <Rn>, <Rm>
    public ARMCortexMThumb $cmp(final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cmp", Rn, Rm );

        // T1
        if(Rn.regNum <= 7 && Rm.regNum <= 7) {
            _checkReg_Low(_MSTR, Rn);
            _checkReg_Low(_MSTR, Rm);
            _putOpCode16( 0b0100001010000000 | (Rm.regNum << 3) | Rn.regNum );
        }

        // T2
        else {
            _putOpCode16( 0b0100010100000000 | ( ( Rn.regNum & 0b1000) << 4 ) | (Rm.regNum << 3) | (Rn.regNum & 0b0111) );
        }

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] CPSIE i
    public ARMCortexMThumb $cpsie_i() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cpsie_i" );

        _errorIfInsideITBlock( _MSTR, "$cpsie" );

        _putOpCode16( 0b1011011001100010 );
        return this;
    }

    // [T1] CPSID i
    public ARMCortexMThumb $cpsid_i() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$cpsid_i" );

        _errorIfInsideITBlock( _MSTR, "$cpsid" );

        _putOpCode16( 0b1011011001110010 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [PI] MOV <Rd>, <Rm>
    public ARMCortexMThumb $cpy(final Reg Rd, final Reg Rm) throws JXMAsmError
    { return $mov(Rd, Rm); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [PI] DEC1S <Rdn>
    public ARMCortexMThumb $dec1s(final Reg Rdn) throws JXMAsmError
    { return $subs(Rdn, 1); }

    // [PI] DEC2S <Rdn>
    public ARMCortexMThumb $dec2s(final Reg Rdn) throws JXMAsmError
    { return $subs(Rdn, 2); }

    // [PI] DEC4S <Rdn>
    public ARMCortexMThumb $dec4s(final Reg Rdn) throws JXMAsmError
    { return $subs(Rdn, 4); }

    // [PI] DEC8S <Rdn>
    public ARMCortexMThumb $dec8s(final Reg Rdn) throws JXMAsmError
    { return $subs(Rdn, 8); }

    // [PI] DEC16S <Rdn>
    public ARMCortexMThumb $dec16s(final Reg Rdn) throws JXMAsmError
    { return $subs(Rdn, 16); }

    // [PI] DEC1W <Rdn>
    public ARMCortexMThumb $dec1w(final Reg Rdn) throws JXMAsmError
    { return $subw(Rdn, Rdn, 1); }

    // [PI] DEC2W <Rdn>
    public ARMCortexMThumb $dec2w(final Reg Rdn) throws JXMAsmError
    { return $subw(Rdn, Rdn, 2); }

    // [PI] DEC4W <Rdn>
    public ARMCortexMThumb $dec4w(final Reg Rdn) throws JXMAsmError
    { return $subw(Rdn, Rdn, 4); }

    // [PI] DEC8W <Rdn>
    public ARMCortexMThumb $dec8w(final Reg Rdn) throws JXMAsmError
    { return $subw(Rdn, Rdn, 8); }

    // [PI] DEC16W <Rdn>
    public ARMCortexMThumb $dec16w(final Reg Rdn) throws JXMAsmError
    { return $subw(Rdn, Rdn, 16); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] DMB sy
    public ARMCortexMThumb $dmb_sy() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$dmb_sy" );

        _putOpCode32( 0b11110011101111111000111101011111L );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] DSB sy
    public ARMCortexMThumb $dsb_sy() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$dsb_sy" );

        _putOpCode32( 0b11110011101111111000111101001111L );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] EORS <Rdn>, <Rm>
    public ARMCortexMThumb $eors(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$eors", Rdn, Rm );

        _checkReg_Low(_MSTR, Rdn);
        _checkReg_Low(_MSTR, Rm );
        _putOpCode16( 0b0100000001000000 | (Rm.regNum << 3) | Rdn.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [PI] INC1S <Rdn>
    public ARMCortexMThumb $inc1s(final Reg Rdn) throws JXMAsmError
    { return $adds(Rdn, 1); }

    // [PI] INC2S <Rdn>
    public ARMCortexMThumb $inc2s(final Reg Rdn) throws JXMAsmError
    { return $adds(Rdn, 2); }

    // [PI] INC4S <Rdn>
    public ARMCortexMThumb $inc4s(final Reg Rdn) throws JXMAsmError
    { return $adds(Rdn, 4); }

    // [PI] INC8S <Rdn>
    public ARMCortexMThumb $inc8s(final Reg Rdn) throws JXMAsmError
    { return $adds(Rdn, 8); }

    // [PI] INC16S <Rdn>
    public ARMCortexMThumb $inc16s(final Reg Rdn) throws JXMAsmError
    { return $adds(Rdn, 16); }

    // [PI] INC1W <Rdn>
    public ARMCortexMThumb $inc1w(final Reg Rdn) throws JXMAsmError
    { return $addw(Rdn, Rdn, 1); }

    // [PI] INC2W <Rdn>
    public ARMCortexMThumb $inc2w(final Reg Rdn) throws JXMAsmError
    { return $addw(Rdn, Rdn, 2); }

    // [PI] INC4W <Rdn>
    public ARMCortexMThumb $inc4w(final Reg Rdn) throws JXMAsmError
    { return $addw(Rdn, Rdn, 4); }

    // [PI] INC8W <Rdn>
    public ARMCortexMThumb $inc8w(final Reg Rdn) throws JXMAsmError
    { return $addw(Rdn, Rdn, 8); }

    // [PI] INC16W <Rdn>
    public ARMCortexMThumb $inc16w(final Reg Rdn) throws JXMAsmError
    { return $addw(Rdn, Rdn, 16); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ISB sy
    public ARMCortexMThumb $isb_sy() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$isb_sy" );

        _putOpCode32( 0b11110011101111111000111101101111L );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] IT{x{y{z}}} <firstcond>
    protected ARMCortexMThumb _$it_impl(final Cond firstCond, final IT mask) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$it" + mask._str , firstCond );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstruction(_MSTR, "$it");

        _errorIfInsideITBlock( _MSTR, "$it{x{y{z}}}" );

        _itFCond = firstCond;
        _itSCond = mask.name();
        _itICond = 0;

        _putOpCodeIT( 0b1011111100000000 | (firstCond.value << 4) | mask._mask(firstCond, _itBlock) );

        return this;
    }

    public ARMCortexMThumb $it   (final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.___); }
    public ARMCortexMThumb $itt  (final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.T__); }
    public ARMCortexMThumb $ite  (final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.E__); }
    public ARMCortexMThumb $ittt (final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.TT_); }
    public ARMCortexMThumb $itet (final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.ET_); }
    public ARMCortexMThumb $itte (final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.TE_); }
    public ARMCortexMThumb $itee (final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.EE_); }
    public ARMCortexMThumb $itttt(final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.TTT); }
    public ARMCortexMThumb $itett(final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.ETT); }
    public ARMCortexMThumb $ittet(final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.TET); }
    public ARMCortexMThumb $iteet(final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.EET); }
    public ARMCortexMThumb $ittte(final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.TTE); }
    public ARMCortexMThumb $itete(final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.ETE); }
    public ARMCortexMThumb $ittee(final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.TEE); }
    public ARMCortexMThumb $iteee(final Cond firstCond) throws JXMAsmError { return _$it_impl(firstCond, IT.EEE); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [1] LDMIA <Rn>{!}, <registers>
    public ARMCortexMThumb $ldmia(final RegGen Rn_, final Reg... Regs) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldmia", Rn_, Regs );

        final boolean isWB = Rn_._isWB();
        final RegGen  Rn   = Rn_._unWB();

        _checkReg_Low(_MSTR, Rn);

        int mask = 0;

        for(final Reg R : Regs) {
            _checkReg_Low(_MSTR, R);
            if(isWB && Rn.regNum == R.regNum) _errorInvalidRegisterInList(_MSTR, Rn);
            mask |= (1 << R.regNum);
        }

        if( !isWB && ( mask & (1 << Rn.regNum) ) == 0 ) _errorInvalidRegisterNotInList(_MSTR, Rn);

        _putOpCode16( 0b1100100000000000 | (Rn.regNum << 8) | mask );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T4] LDR.W <Rt>, [<Rn>], ±#<imm8> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $ldr_pst(final Reg Rt, final Reg Rn, final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldr_pst", Rt, Rn, imm8 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$ldr.w");

        final boolean neg  = (imm8 < 0);
        final long    imm_ = Math.abs(imm8);
        final long    imm  = _checkUOffset(_MSTR, imm_, 0, 8);

                              // 111110000101nnnntttt1PUWiiiiiiii
        if(!neg) _putOpCode32( 0b11111000010100000000101100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
        else     _putOpCode32( 0b11111000010100000000100100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );

        return this;
    }

    // [T3] LDR.W <Rt>, [<Rn>, #<imm12>]  ----- not available in 'ARMv8-M Baseline'
    // [T4] LDR.W <Rt>, [<Rn>, -#<imm8>]  ----- not available in 'ARMv8-M Baseline'
    // [T4] LDR.W <Rt>, [<Rn>, ±#<imm8>]! ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$ldr_w_impl(final boolean isWB, final Reg Rt, final RegGen Rn, final int imm8_imm12) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$ldr.w");

        // T3
        if( !isWB && imm8_imm12 >= 0 && imm8_imm12 <= ( (1 << 12) - 1 ) ) {
            final long imm = _checkUOffset(_MSTR, imm8_imm12, 0, 12);
            _putOpCode32( 0b11111000110100000000000000000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
        }
        // T4
        else {
            final boolean neg  = (imm8_imm12 < 0);
            final long    imm_ = Math.abs(imm8_imm12);
            final long    imm  = _checkUOffset(_MSTR, imm_, 0, 8);
                                                // 111110000101nnnntttt1PUWiiiiiiii
                 if(!neg && !isWB) _putOpCode32( 0b11111000010100000000111000000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
            else if(!neg &&  isWB) _putOpCode32( 0b11111000010100000000111100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
            else if( neg && !isWB) _putOpCode32( 0b11111000010100000000110000000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
            else if( neg &&  isWB) _putOpCode32( 0b11111000010100000000110100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
        }

        return this;
    }

    // [T1] LDR <Rt>, [<Rn>, #<imm5> ]
    // [T2] LDR <Rt>, [ SP , #<imm8> ]
    public ARMCortexMThumb $ldr(final Reg Rt, final RegGen Rn_, final int imm5_imm8_imm12) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldr", Rt, Rn_, imm5_imm8_imm12 );

        final boolean isWB = Rn_._isWB();
        final RegGen  Rn   = Rn_._unWB();

        // T1/T3/T4
        if(Rn.regNum != Reg.SP.regNum) {
            // T3/T4
            if( _cpu.supInstARMv7MComplete() && ( isWB || !Rt._isRegLow() || !Rn._isRegLow() || imm5_imm8_imm12 < 0 || imm5_imm8_imm12 > ( ( (1 << 5) - 1 ) << 2 ) ) ) {
                return _$ldr_w_impl(isWB, Rt, Rn, imm5_imm8_imm12);
            }
            // T1
            else {
                _checkReg_Low(_MSTR, Rt);
                _checkReg_Low(_MSTR, Rn);
                final long imm = _checkUOffset(_MSTR, imm5_imm8_imm12, 2, 5);
                _putOpCode16( 0b0110100000000000 | (imm << 6) | (Rn.regNum << 3) | Rt.regNum );
            }
        }

        // T2
        else {
            _checkReg_Low(_MSTR, Rt);
            final long imm = _checkUOffset(_MSTR, imm5_imm8_imm12, 2, 8);
            _putOpCode16( 0b1001100000000000 | (Rt.regNum << 8) | imm );
        }

        return this;
    }

    // [T1] LDR <Rt>, [<Rn>]
    // [T2] LDR <Rt>, [ SP ]
    public ARMCortexMThumb $ldr(final Reg Rt, final Reg Rn) throws JXMAsmError
    { return $ldr(Rt, Rn, 0); }

    // [T1] LDR <Rt>, <label>
    public ARMCortexMThumb $ldr(final Reg Rt, final String label_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldr", Rt, label_ );

        _checkReg_Low(_MSTR, Rt);
        final String label = _checkTargetLabel(_MSTR, label_);
        _putOpCode16( 0b0100100000000000 | (Rt.regNum << 8), _markPCRelLabel(label), 0, 2, 8 );

        return this;
    }

    // [T2] LDR.W <Rt>, <label>
    public ARMCortexMThumb $ldr_w(final Reg Rt, final String label_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldr.w", Rt, label_ );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$ldr.w");

        final String label = _checkTargetLabel(_MSTR, label_);
        _putOpCode32( 0b11111000010111110000000000000000L, _markPCRelLabel(label), new OCPF() {
            @Override
            public long apply(final OpCode opcode, final long address, final int thumbBit) throws JXMAsmError
            {
                // NOTE : This instruction does not need the 'thumbBit' parameter
                final long U     = (address >= 0) ? 1 : 0;
                final long adr   = _checkUOffset( opcode.mString, Math.abs(address), 0, 12 );
                final long opar  = (U << 23) | adr;
                return opcode.opcodeValue | opar;
            }
        } );
        return this;
    }

    // [T1] LDR <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $ldr(final Reg Rt, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldr", Rt, Rn, Rm );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0101100000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    // [PI] LDR <Rt>, =#<imm32>
    public ARMCortexMThumb $ldr(final Reg Rt, final long imm32) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldr", Rt, imm32 );

        if( _cpu.supInstARMv7MComplete() ) {
            // Try to use MOV.W
            final long imm = _fixupConstant32___I_IMM3_ABCDEFGH( imm32);
            if(imm >= 0) return _$movx_w_impl_put32( false, Rt, imm );
            // Try to use MVN.W
            final long bwc = _fixupConstant32___I_IMM3_ABCDEFGH(~imm32);
            if(bwc >= 0) return _$mvnx_w_impl_put32( false, Rt, bwc );
            // Try to use MOVW
            if(imm32 >= 0 && imm32 <= 65535) return $movw( Rt, (int) imm32 );
        }

        _checkReg_Low(_MSTR, Rt);
        _putOpCode16( 0b0100100000000000 | (Rt.regNum << 8), _putData32(imm32), _markPCRelLabel(""), 0, 2, 8 );
        return this;
    }

    /*
     * [PI] ldri <Rd>, #<imm32>
     *
     * It will be converted to one of these instructions:
     *     [T2] MOV  <Rd>,  #<const>
     *     [T1] MOVS <Rd>,  #<imm8>
     *     [T2] MOVS <Rd>,  #<const>
     *     [PI] LDR  <Rt>, =#<imm32>
     */
    public ARMCortexMThumb $ldri(final Reg Rd, final long imm32) throws JXMAsmError
    {
        String eStt = "";
        String eMsg = "";

        // Try MOV first
        if( _cpu.supInstARMv7MComplete() ) {
            try {
                return $mov(Rd, imm32);
            }
            catch(final JXMAsmError e) {
                if( XCom.enableAllExceptionStackTrace() ) eStt += SysUtil.stringFromStackTrace(e);
                eMsg += e.getMessage();
            }
        }

        // Try MOVS next
        try {
            return $movs(Rd, imm32);
        }
        catch(final JXMAsmError e) {
            if( XCom.enableAllExceptionStackTrace() ) eStt += SysUtil.stringFromStackTrace(e);
            eMsg += e.getMessage();
        }

        // Try LDR last
        try {
            return $ldr(Rd, imm32);
        }
        catch(final JXMAsmError e) {
            if( XCom.enableAllExceptionStackTrace() ) eStt += SysUtil.stringFromStackTrace(e);
            eMsg += e.getMessage();
        }

        // Check for error
        if( !eMsg.isEmpty() ) {
            _MNEMONIC_STRING( "$ldri", Rd, imm32 );
            if( XCom.enableAllExceptionStackTrace() ) SysUtil.stdErr().print(eStt);
            _errorInvalidInstruction(_MSTR, "$ldri");
        }

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDRB <Rt>, [<Rn>, #<imm5>]
    public ARMCortexMThumb $ldrb(final Reg Rt, final Reg Rn, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrb", Rt, Rn, imm5 );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
        _putOpCode16( 0b0111100000000000 | (imm << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    // [T1] LDRB <Rt>, [<Rn>]
    public ARMCortexMThumb $ldrb(final Reg Rt, final Reg Rn) throws JXMAsmError
    { return $ldrb(Rt, Rn, 0); }

    // [T1] LDRB <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $ldrb(final Reg Rt, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrb", Rt, Rn, Rm );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0101110000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDRH <Rt>, [<Rn>, #<imm5>]
    public ARMCortexMThumb $ldrh(final Reg Rt, final Reg Rn, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrh", Rt, Rn, imm5 );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        final long imm = _checkUOffset(_MSTR, imm5, 1, 5);
        _putOpCode16( 0b1000100000000000 | (imm << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    // [T1] LDRH <Rt>, [<Rn>]
    public ARMCortexMThumb $ldrh(final Reg Rt, final Reg Rn) throws JXMAsmError
    { return $ldrh(Rt, Rn, 0); }

    // [T1] LDRH <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $ldrh(final Reg Rt, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrh", Rt, Rn, Rm );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0101101000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDRSB <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $ldrsb(final Reg Rt, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrsb", Rt, Rn, Rm );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0101011000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDRSH <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $ldrsh(final Reg Rt, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrsh", Rt, Rn, Rm );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0101111000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDA <Rt>, [<Rn>]
    public ARMCortexMThumb $lda(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lda", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$lda");

        _putOpCode32( 0b11101000110100000000111110101111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] LDAB <Rt>, [<Rn>]
    public ARMCortexMThumb $ldab(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldab", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$ldab");

        _putOpCode32( 0b11101000110100000000111110001111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] LDAH <Rt>, [<Rn>]
    public ARMCortexMThumb $ldah(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldah", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$ldah");

        _putOpCode32( 0b11101000110100000000111110011111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] LDAEX <Rt>, [<Rn>]
    public ARMCortexMThumb $ldaex(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldaex", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$ldaex");

        _putOpCode32( 0b11101000110100000000111111101111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] LDAEXB <Rt>, [<Rn>]
    public ARMCortexMThumb $ldaexb(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldaexb", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$ldaexb");

        _putOpCode32( 0b11101000110100000000111111001111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] LDAEXH <Rt>, [<Rn>]
    public ARMCortexMThumb $ldaexh(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldaexh", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$ldaexh");

        _putOpCode32( 0b11101000110100000000111111011111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] LDREX <Rt>, [<Rn>, #<imm8>]
    public ARMCortexMThumb $ldrex(final Reg Rt, final Reg Rn, final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrex", Rt, Rn, imm8 );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$ldrex");

        final long imm = _checkUOffset(_MSTR, imm8, 2, 8);

        _putOpCode32( 0b11101000010100000000111100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );

        return this;
    }

    // [T1] LDREX <Rt>, [<Rn>]
    public ARMCortexMThumb $ldrex(final Reg Rt, final Reg Rn) throws JXMAsmError
    { return $ldrex(Rt, Rn, 0); }

    // [T1] LDREXB <Rt>, [<Rn>]
    public ARMCortexMThumb $ldrexb(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrexb", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$ldrexb");

        _putOpCode32( 0b11101000110100000000111101001111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] LDREXH <Rt>, [<Rn>]
    public ARMCortexMThumb $ldrexh(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ldrexh", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$ldrexh");

        _putOpCode32( 0b11101000110100000000111101011111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T2] LSL{S}.W <Rd>, <Rm>, #<imm5> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$lsls_w_impl(final boolean S, final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsl{s}.w");

        final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
        if(S) _putOpCode32( 0b11101010010111110000000000000000L | ( (imm5 & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm5 & 0b00011) << 6 ) | Rm.regNum );
        else  _putOpCode32( 0b11101010010011110000000000000000L | ( (imm5 & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm5 & 0b00011) << 6 ) | Rm.regNum );
        return this;
    }

    // [T2] LSL{S}.W <Rd>, <Rn>, <Rm> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$lsls_w_impl(final boolean S, final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsl{s}.w");

        if(S) _putOpCode32( 0b11111010000100001111000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        else  _putOpCode32( 0b11111010000000001111000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        return this;
    }

    // [T1] LSLS   <Rd>, <Rm>, #<imm5>
    // [T2] LSLS.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $lsls(final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lsls", Rd, Rm, imm5 );

        // T2
        if( _cpu.supInstARMv7MComplete() && ( !Rd._isRegLow() || !Rm._isRegLow() ) ) {
            return _$lsls_w_impl(true, Rd, Rm, imm5);
        }

        // T1
        else {
            _checkReg_Low(_MSTR, Rd);
            _checkReg_Low(_MSTR, Rm);
            final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
            _putOpCode16( 0b0000000000000000 | (imm << 6) | (Rm.regNum << 3) | Rd.regNum );
            return this;
        }
    }

    // [T2] LSL.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $lsl(final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lsl", Rd, Rm, imm5 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsl.w");

        return _$lsls_w_impl(false, Rd, Rm, imm5);
    }

    // [T1] LSLS   <Rdn>, <Rm>
    // [T2] LSLS.W <Rd >, <Rn>, <Rm>
    public ARMCortexMThumb $lsls(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lsls", Rdn, Rm );

        // T2
        if( _cpu.supInstARMv7MComplete() && ( !Rdn._isRegLow() || !Rm._isRegLow() ) ) {
            return _$lsls_w_impl(true, Rdn, Rdn, Rm);
        }

        // T1
        else {
            _checkReg_Low(_MSTR, Rdn);
            _checkReg_Low(_MSTR, Rm );
            _putOpCode16( 0b0100000010000000 | (Rm.regNum << 3) | Rdn.regNum );
            return this;
        }
    }

    // [T2] LSLS.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $lsls(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        if(Rd.regNum == Rn.regNum) return $lsls(Rd, Rm);

        _MNEMONIC_STRING( "$lsls", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsls.w");

        return _$lsls_w_impl(true, Rd, Rn, Rm);
    }

    // [T2] LSL.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $lsl(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lsl", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsl.w");

        return _$lsls_w_impl(false, Rd, Rn, Rm);
    }

    // [T2] LSL.W <Rdn>, <Rm>
    public ARMCortexMThumb $lsl(final Reg Rdn, final Reg Rm) throws JXMAsmError
    { return $lsl(Rdn, Rdn, Rm); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T2] LSR{S}.W <Rd>, <Rm>, #<imm5> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$lsrs_w_impl(final boolean S, final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsr{s}.w");

        final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
        if(S) _putOpCode32( 0b11101010010111110000000000010000L | ( (imm5 & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm5 & 0b00011) << 6 ) | Rm.regNum );
        else  _putOpCode32( 0b11101010010011110000000000010000L | ( (imm5 & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm5 & 0b00011) << 6 ) | Rm.regNum );
        return this;
    }

    // [T2] LSR{S}.W <Rd>, <Rn>, <Rm> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$lsrs_w_impl(final boolean S, final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsr{s}.w");

        if(S) _putOpCode32( 0b11111010001100001111000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        else  _putOpCode32( 0b11111010001000001111000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        return this;
    }

    // [T1] LSRS   <Rd>, <Rm>, #<imm5>
    // [T2] LSRS.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $lsrs(final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lsrs", Rd, Rm, imm5 );

        // T2
        if( _cpu.supInstARMv7MComplete() && ( !Rd._isRegLow() || !Rm._isRegLow() ) ) {
            return _$lsrs_w_impl(true, Rd, Rm, imm5);
        }

        // T1
        else {
            _checkReg_Low(_MSTR, Rd);
            _checkReg_Low(_MSTR, Rm);
            final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
            _putOpCode16( 0b0000100000000000 | (imm << 6) | (Rm.regNum << 3) | Rd.regNum );
            return this;
        }
    }

    // [T2] LSR.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $lsr(final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lsr", Rd, Rm, imm5 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsr.w");

        return _$lsrs_w_impl(false, Rd, Rm, imm5);
    }

    // [T1] LSRS   <Rdn>, <Rm>
    // [T2] LSRS.W <Rd >, <Rn>, <Rm>
    public ARMCortexMThumb $lsrs(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lsrs", Rdn, Rm );

        // T2
        if( _cpu.supInstARMv7MComplete() && ( !Rdn._isRegLow() || !Rm._isRegLow() ) ) {
            return _$lsrs_w_impl(true, Rdn, Rdn, Rm);
        }

        // T1
        else {
            _checkReg_Low(_MSTR, Rdn);
            _checkReg_Low(_MSTR, Rm );
            _putOpCode16( 0b0100000011000000 | (Rm.regNum << 3) | Rdn.regNum );
            return this;
        }
    }

    // [T2] LSRS.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $lsrs(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        if(Rd.regNum == Rn.regNum) return $lsrs(Rd, Rm);

        _MNEMONIC_STRING( "$lsrs", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsrs.w");

        return _$lsrs_w_impl(true, Rd, Rn, Rm);
    }

    // [T2] LSR.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $lsr(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$lsr", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$lsr.w");

        return _$lsrs_w_impl(false, Rd, Rn, Rm);
    }

    // [T2] LSR.W <Rdn>, <Rm>
    public ARMCortexMThumb $lsr(final Reg Rdn, final Reg Rm) throws JXMAsmError
    { return $lsr(Rdn, Rdn, Rm); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T2] MOV{S}.W <Rd>, #<const> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb _$movx_w_impl_put32(final boolean S, final Reg Rd, final long fixup) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "mov{s}.w");

        if(S) _putOpCode32( 0b11110000010111110000000000000000L | (Rd.regNum << 8) | fixup );
        else  _putOpCode32( 0b11110000010011110000000000000000L | (Rd.regNum << 8) | fixup );
        return this;
    }

    // [T2] MOV{S}.W <Rd>, #<const>
    protected ARMCortexMThumb _$movx_w_impl(final boolean S, final Reg Rd, final long const_) throws JXMAsmError
    {
        final long imm = _fixupConstant32___I_IMM3_ABCDEFGH( const_);
        if(imm >= 0) return _$movx_w_impl_put32( S, Rd, imm );

        final long bwc = _fixupConstant32___I_IMM3_ABCDEFGH(~const_);
        if(bwc >= 0) return _$mvnx_w_impl_put32( S, Rd, bwc );

        _errorInvalidConstantAfterFixup(_MSTR, const_);
        return this;
    }

    // [T2] MOV.W <Rd>, #<const>
    public ARMCortexMThumb $mov(final Reg Rd, final long const_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov", Rd, const_ );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$mov");

        return _$movx_w_impl( false, Rd, const_ );
    }

    // [T1] MOVS   <Rd>, #<imm8>
    // [T2] MOVS.W <Rd>, #<const>
    public ARMCortexMThumb $movs(final Reg Rd, final long imm8_const) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movs", Rd, imm8_const );

        if( _cpu.supInstARMv7MComplete() && (imm8_const < 0 || imm8_const > 255) ) {
            return _$movx_w_impl( true, Rd, imm8_const );
        }

        _checkReg_Low(_MSTR, Rd);
        final long imm = _checkUOffset(_MSTR, imm8_const, 0, 8);
        _putOpCode16( 0b0010000000000000 | (Rd.regNum << 8) | imm8_const );
        return this;
    }

    // [T1] MOVS.W <Rd>, #<imm8>
    // [T2] MOVS.W <Rd>, #<const>
    public ARMCortexMThumb $movs_w(final Reg Rd, final long imm8_const) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movs.w", Rd, imm8_const );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$movs.w");

        return _$movx_w_impl( true, Rd, imm8_const );
    }

    // [T3] MOVW <Rd>, #<imm16>
    public ARMCortexMThumb $movw(final Reg Rd, final int imm16) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movw", Rd, imm16 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstruction(_MSTR, "$movw");

        final long imm  = _checkUOffset(_MSTR, imm16, 0, 16);
        final long imm4 = imm & 0b1111000000000000;
        final long i    = imm & 0b0000100000000000;
        final long imm3 = imm & 0b0000011100000000;
        final long imm8 = imm & 0b0000000011111111;
        _putOpCode32( 0b11110010010000000000000000000000L | (i << 15) | (imm4 << 4) | (imm3 << 4) | (Rd.regNum << 8) | imm8 );
        return this;
    }

    // [T2] MOVS   <Rd>, <Rm>
    // [T3] MOVS.W <Rd>, <Rm>
    public ARMCortexMThumb $movs(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movs", Rd, Rm );

        // T3 ----- not available in 'ARMv8-M Baseline'
        if( _cpu.supInstARMv7MComplete() && ( !Rd._isRegLow() || !Rm._isRegLow() ) ) {
            _checkReg_Not_SP_PC(_MSTR, Rd);
            _checkReg_Not_SP_PC(_MSTR, Rm);
            // !!! ERROR: Missing 'L' suffix - int literal 0b11101010010111110000000000000000 has bit 31 = 1, making it a negative int.
            // When widened to long for _putOpCode32(), Java sign-extends it to 0xFFFFFFFF_EA5F0000L (garbage in upper 32 bits).
            // Fix: append 'L' suffix: 0b11101010010111110000000000000000L
            _putOpCode32( 0b11101010010111110000000000000000 | (Rd.regNum << 8) | Rm.regNum );
        }

        // T2
        else {
            _errorIfInsideITBlock( _MSTR, "$movs" );

            _checkReg_Low(_MSTR, Rd);
            _checkReg_Low(_MSTR, Rm);
            _putOpCode16( 0b0000000000000000 | (Rm.regNum << 3) | Rd.regNum );
        }

        return this;
    }

    // [T3] MOVS.W <Rd>, <Rm>
    public ARMCortexMThumb $movs_w(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movs.w", Rd, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstruction(_MSTR, "$movw");

        _checkReg_Not_SP_PC(_MSTR, Rd);
        _checkReg_Not_SP_PC(_MSTR, Rm);
        _putOpCode32( 0b11101010010111110000000000000000 | (Rd.regNum << 8) | Rm.regNum );
        return this;
    }

    // [T1] MOV <Rd>, <Rm>
    public ARMCortexMThumb $mov(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov", Rd, Rm );

        if(Rd.regNum == Reg.PC.regNum) _errorIfNotLastInsideITBlock( _MSTR, "$mov" );

        _putOpCode16( 0b0100011000000000 | ( ( Rd.regNum & 0b1000) << 4 ) | (Rm.regNum << 3) | (Rd.regNum & 0b0111) );

        return this;
    }

    // [T3] MOV.W <Rd>, <Rm>
    public ARMCortexMThumb $mov_w(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov.w", Rd, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$mov.w");

        // !!! ERROR: Missing 'L' suffix - int literal 0b11101010010011110000000000000000 has bit 31 = 1, making it a negative int.
        // When widened to long for _putOpCode32(), Java sign-extends it to 0xFFFFFFFF_EA4F0000L (garbage in upper 32 bits).
        // Fix: append 'L' suffix: 0b11101010010011110000000000000000L
        _putOpCode32( 0b11101010010011110000000000000000 | (Rd.regNum << 8) | Rm.regNum );

        return this;
    }

    // [PI] MOVS <Rd>, <Rm>, <shift_operand> #<imm5>
    public ARMCortexMThumb $movs(final Reg Rd, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movs", Rd, Rm, shift, imm5 );

        switch(shift) {
            case ASR : return $asrs(Rd, Rm, imm5);
            case LSL : return $lsls(Rd, Rm, imm5);
            case LSR : return $lsrs(Rd, Rm, imm5);
            case ROR : return $rors(Rd, Rm, imm5);
              /* RRX : is not supported by here */
            default  : _errorInvalidShiftedRegisterOperand( _MSTR, shift.name() );
        }
        return this;
    }

    // [PI] MOV <Rd>, <Rm>, <shift_operand> #<imm5>
    public ARMCortexMThumb $mov(final Reg Rd, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov", Rd, Rm, shift, imm5 );

        switch(shift) {
            case ASR : return $asr(Rd, Rm, imm5);
            case LSL : return $lsl(Rd, Rm, imm5);
            case LSR : return $lsr(Rd, Rm, imm5);
            case ROR : return $ror(Rd, Rm, imm5);
              /* RRX : is not supported by here */
            default  : _errorInvalidShiftedRegisterOperand( _MSTR, shift.name() );
        }
        return this;
    }

    // [PI] MOVS <Rdm>, <Rdm>, <shift_operand> <Rs>
    public ARMCortexMThumb $movs(final Reg Rdm, final Shift shift, final Reg Rs) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movs", Rdm, shift, Rs );

        switch(shift) {
            case ASR : return $asrs(Rdm, Rs);
            case LSL : return $lsls(Rdm, Rs);
            case LSR : return $lsrs(Rdm, Rs);
            case ROR : return $rors(Rdm, Rs);
              /* RRX : is not supported by here */
            default  : _errorInvalidShiftedRegisterOperand( _MSTR, shift.name() );
        }

        return this;
    }

    // [PI] MOVS <Rd>, <Rm>, <shift_operand> <Rs>
    public ARMCortexMThumb $movs(final Reg Rd, final Reg Rm, final Shift shift, final Reg Rs) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movs", Rd, Rm, shift, Rs );

        switch(shift) {
            case ASR : return $asrs(Rd, Rm, Rs);
            case LSL : return $lsls(Rd, Rm, Rs);
            case LSR : return $lsrs(Rd, Rm, Rs);
            case ROR : return $rors(Rd, Rm, Rs);
              /* RRX : is not supported by here */
            default  : _errorInvalidShiftedRegisterOperand( _MSTR, shift.name() );
        }

        return this;
    }

    // [PI] MOV <Rd>, <Rm>, <shift_operand> <Rs>
    public ARMCortexMThumb $mov(final Reg Rd, final Reg Rm, final Shift shift, final Reg Rs) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov", Rd, Rm, shift, Rs );

        switch(shift) {
            case ASR : return $asr(Rd, Rm, Rs);
            case LSL : return $lsl(Rd, Rm, Rs);
            case LSR : return $lsr(Rd, Rm, Rs);
            case ROR : return $ror(Rd, Rm, Rs);
              /* RRX : is not supported by here */
            default  : _errorInvalidShiftedRegisterOperand( _MSTR, shift.name() );
        }

        return this;
    }

    // [PI] MOVS <Rd>, <Rm>, <shift_operand>
    public ARMCortexMThumb $movs(final Reg Rd, final Reg Rm, final Shift shift) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movs", Rd, Rm, shift );

        switch(shift) {
              /* ASR : is not supported by here */
              /* LSL : is not supported by here */
              /* LSR : is not supported by here */
              /* ROR : is not supported by here */
            case RRX : return $rrxs(Rd, Rm);
            default  : _errorInvalidShiftedRegisterOperand( _MSTR, shift.name() );
        }

        return this;
    }

    // [PI] MOV <Rd>, <Rm>, <shift_operand>
    public ARMCortexMThumb $mov(final Reg Rd, final Reg Rm, final Shift shift) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mov", Rd, Rm, shift );

        switch(shift) {
              /* ASR : is not supported by here */
              /* LSL : is not supported by here */
              /* LSR : is not supported by here */
              /* ROR : is not supported by here */
            case RRX : return $rrx(Rd, Rm);
            default  : _errorInvalidShiftedRegisterOperand( _MSTR, shift.name() );
        }

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MOVT <Rd>, <imm16>
    public ARMCortexMThumb $movt(final Reg Rd, final int imm16) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$movt", Rd, imm16 );

        if( !_cpu.supInstARMv7MMinimal() ) _errorInvalidInstruction(_MSTR, "$movt");

        final long imm  = _checkUOffset(_MSTR, imm16, 0, 16);
        final long imm4 = imm & 0b1111000000000000;
        final long i    = imm & 0b0000100000000000;
        final long imm3 = imm & 0b0000011100000000;
        final long imm8 = imm & 0b0000000011111111;
        _putOpCode32( 0b11110010110000000000000000000000L | (i << 15) | (imm4 << 4) | (imm3 << 4) | (Rd.regNum << 8) | imm8 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MCR  <CoProc>, #<opc1>, <Rt>, <CRn>, <CRm>, #<opc2>
    // [T2] MCR2 <CoProc>, #<opc1>, <Rt>, <CRn>, <CRm>, #<opc2>
    private ARMCortexMThumb _$mcr_impl(final String inst, final int opcode, final CoProc coproc, final int opc1_, final Reg Rt, final CReg CRn, final CReg CRm, final int opc2_) throws JXMAsmError
    {
        _MNEMONIC_STRING( inst, coproc, opc1_, Rt, CRn, CRm, opc2_ );

        if( !_cpu.supInstARMv8MMainline() ) _errorInvalidInstructionForm(_MSTR, inst);

        final long opc1 = _checkUOffset(_MSTR, opc1_, 0, 3);
        final long opc2 = _checkUOffset(_MSTR, opc2_, 0, 3);
        _putOpCode32(
            opcode               |
            (opc1         << 21) |
            (CRn.value    << 16) |
            (Rt.regNum    << 12) |
            (coproc.value <<  8) |
            (opc2         <<  5) |
            (CRm.value         )
        );
        return this;
    }

    public ARMCortexMThumb $mcr (final CoProc coproc, final int opc1, final Reg Rt, final CReg CRn, final CReg CRm, final int opc2) throws JXMAsmError
    { return _$mcr_impl("$mcr" , 0b11101110000000000000000000010000, coproc, opc1, Rt, CRn, CRm, opc2); }

    public ARMCortexMThumb $mcr2(final CoProc coproc, final int opc1, final Reg Rt, final CReg CRn, final CReg CRm, final int opc2) throws JXMAsmError
    { return _$mcr_impl("$mcr2", 0b11111110000000000000000000010000, coproc, opc1, Rt, CRn, CRm, opc2); }

    // [T1] MCRR  <CoProc>, #<opc1>, <Rt>, <Rt2>, <CRm>
    // [T2] MCRR2 <CoProc>, #<opc1>, <Rt>, <Rt2>, <CRm>
    private ARMCortexMThumb _$mcrr_impl(final String inst, final int opcode, final CoProc coproc, final int opc1_, final Reg Rt, final Reg Rt2, final CReg CRm) throws JXMAsmError
    {
        _MNEMONIC_STRING( inst, coproc, opc1_, Rt, Rt2, CRm );

        if( !_cpu.supInstARMv8MMainline() ) _errorInvalidInstructionForm(_MSTR, inst);

        final long opc1 = _checkUOffset(_MSTR, opc1_, 0, 4);
        _putOpCode32(
            opcode               |
            (Rt2.regNum   << 16) |
            (Rt.regNum    << 12) |
            (coproc.value <<  8) |
            (opc1         <<  4) |
            (CRm.value         )
        );
        return this;
    }

    public ARMCortexMThumb $mcrr (final CoProc coproc, final int opc1, final Reg Rt, final Reg Rt2, final CReg CRm) throws JXMAsmError
    { return _$mcrr_impl("$mcrr" , 0b11101100010000000000000000000000, coproc, opc1, Rt, Rt2, CRm); }

    public ARMCortexMThumb $mcrr2(final CoProc coproc, final int opc1, final Reg Rt, final Reg Rt2, final CReg CRm) throws JXMAsmError
    { return _$mcrr_impl("$mcrr2", 0b11111100010000000000000000000000, coproc, opc1, Rt, Rt2, CRm); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MRC  <CoProc>, #<opc1>, <Rt>, <CRn>, <CRm>, #<opc2>
    // [T2] MRC2 <CoProc>, #<opc1>, <Rt>, <CRn>, <CRm>, #<opc2>
    private ARMCortexMThumb _$mrc_impl(final String inst, final int opcode, final CoProc coproc, final int opc1_, final Reg Rt, final CReg CRn, final CReg CRm, final int opc2_) throws JXMAsmError
    {
        _MNEMONIC_STRING( inst, coproc, opc1_, Rt, CRn, CRm, opc2_ );

        if( !_cpu.supInstARMv8MMainline() ) _errorInvalidInstructionForm(_MSTR, inst);

        final long opc1 = _checkUOffset(_MSTR, opc1_, 0, 3);
        final long opc2 = _checkUOffset(_MSTR, opc2_, 0, 3);
        _putOpCode32(
            opcode               |
            (opc1         << 21) |
            (CRn.value    << 16) |
            (Rt.regNum    << 12) |
            (coproc.value <<  8) |
            (opc2         <<  5) |
            (CRm.value         )
        );
        return this;
    }

    public ARMCortexMThumb $mrc (final CoProc coproc, final int opc1, final Reg Rt, final CReg CRn, final CReg CRm, final int opc2) throws JXMAsmError
    { return _$mrc_impl("$mrc" , 0b11101110000100000000000000010000, coproc, opc1, Rt, CRn, CRm, opc2); }

    public ARMCortexMThumb $mrc2(final CoProc coproc, final int opc1, final Reg Rt, final CReg CRn, final CReg CRm, final int opc2) throws JXMAsmError
    { return _$mrc_impl("$mrc2", 0b11111110000100000000000000010000, coproc, opc1, Rt, CRn, CRm, opc2); }

    // [T1] MRRC  <CoProc>, #<opc1>, <Rt>, <Rt2>, <CRm>
    // [T2] MRRC2 <CoProc>, #<opc1>, <Rt>, <Rt2>, <CRm>
    private ARMCortexMThumb _$mrrc_impl(final String inst, final int opcode, final CoProc coproc, final int opc1_, final Reg Rt, final Reg Rt2, final CReg CRm) throws JXMAsmError
    {
        _MNEMONIC_STRING( inst, coproc, opc1_, Rt, Rt2, CRm );

        if( !_cpu.supInstARMv8MMainline() ) _errorInvalidInstructionForm(_MSTR, inst);

        final long opc1 = _checkUOffset(_MSTR, opc1_, 0, 4);
        _putOpCode32(
            opcode               |
            (Rt2.regNum   << 16) |
            (Rt.regNum    << 12) |
            (coproc.value <<  8) |
            (opc1         <<  4) |
            (CRm.value         )
        );
        return this;
    }

    public ARMCortexMThumb $mrrc (final CoProc coproc, final int opc1, final Reg Rt, final Reg Rt2, final CReg CRm) throws JXMAsmError
    { return _$mrrc_impl("$mrrc" , 0b11101100010100000000000000000000, coproc, opc1, Rt, Rt2, CRm); }

    public ARMCortexMThumb $mrrc2(final CoProc coproc, final int opc1, final Reg Rt, final Reg Rt2, final CReg CRm) throws JXMAsmError
    { return _$mrrc_impl("$mrrc2", 0b11111100010100000000000000000000, coproc, opc1, Rt, Rt2, CRm); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MRS <Rd>, <SYSm>
    public ARMCortexMThumb $mrs(final Reg Rd, final SYSm sysm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mrs", Rd, sysm );

        if( !sysm._checkCPUSupport(_cpu) ) _errorInvalidInstructionForm(_MSTR, "$mrs");

        _putOpCode32( 0b11110011111011111000000000000000L | (Rd.regNum << 8) | sysm.value );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MSR <SYSm>, <Rn>
    public ARMCortexMThumb $msr(final SYSm sysm, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$msr", sysm, Rn );

        if( !sysm._checkCPUSupport(_cpu) ) _errorInvalidInstructionForm(_MSTR, "$msr");

        _putOpCode32( 0b11110011100000001000100000000000L | (Rn.regNum << 16) | sysm.value );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MULS <Rdm>, <Rn>, <Rdm>
    public ARMCortexMThumb $muls(final Reg Rdm, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$muls", Rdm, Rn );

        _checkReg_Low(_MSTR, Rdm);
        _checkReg_Low(_MSTR, Rn );
        _putOpCode16( 0b0100001101000000 | (Rn.regNum << 3) | Rdm.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MVN{S}.W <Rd>, #<const> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb _$mvnx_w_impl_put32(final boolean S, final Reg Rd, final long fixup) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$mvn{s}.w");

        if(S) _putOpCode32( 0b11110000011111110000000000000000L | (Rd.regNum << 8) | fixup );
        else  _putOpCode32( 0b11110000011011110000000000000000L | (Rd.regNum << 8) | fixup );
        return this;
    }

    // [T1] MVN{S}.W <Rd>, #<const>
    protected ARMCortexMThumb _$mvnx_w_impl(final boolean S, final Reg Rd, final long const_) throws JXMAsmError
    {
        final long imm = _fixupConstant32___I_IMM3_ABCDEFGH( const_);
        if(imm >= 0) return _$mvnx_w_impl_put32( S, Rd, imm );

        final long bwc = _fixupConstant32___I_IMM3_ABCDEFGH(~const_);
        if(bwc >= 0) return _$movx_w_impl_put32( S, Rd, bwc );

        _errorInvalidConstantAfterFixup(_MSTR, const_);
        return this;
    }

    // [T1] MVN <Rd>, #<const>
    public ARMCortexMThumb $mvn(final Reg Rd, final long const_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mvn", Rd, const_ );

        return _$mvnx_w_impl( false, Rd, const_ );
    }

    // [T1] MVNS <Rd>, #<const>
    public ARMCortexMThumb $mvns(final Reg Rd, final long const_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mvns", Rd, const_ );

        return _$mvnx_w_impl( true, Rd, const_ );
    }

    // [T1] MVNS <Rd>, <Rm>
    public ARMCortexMThumb $mvns(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        // !!! ERROR: Wrong mnemonic string - says "$muls" but should be "$mvns". Fix: change "$muls" to "$mvns".
        _MNEMONIC_STRING( "$muls", Rd, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0100001111000000 | (Rm.regNum << 3) | Rd.regNum );
        return this;
    }

    // [T2] MVN{S}.W <Rd>, <Rm>, <shift_operand> {#<imm5>} ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$mvnx_w_shift_impl(final boolean S, final Reg Rd, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$mvn{s}.w");

        final long[] imm_type = _encodeShiftAppliedToRegister___IMM_TYPE(_MSTR, shift, imm5);

        long imm  = imm_type[0];
        long type = imm_type[1];

        if(S) _putOpCode32( 0b11101010011111110000000000000000L | ( (imm & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm & 0b00011) << 6 ) | (type << 4) | Rm.regNum );
        else  _putOpCode32( 0b11101010011011110000000000000000L | ( (imm & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm & 0b00011) << 6 ) | (type << 4) | Rm.regNum );

        return this;
    }

    // [T2] MVNS <Rd>, <Rm>, <shift_operand> #<imm5>
    public ARMCortexMThumb $mvns(final Reg Rd, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mvns", Rd, Rm, shift, imm5 );
        return _$mvnx_w_shift_impl(true, Rd, Rm, shift, imm5);
    }

    // [T2] MVNS <Rd>, <Rm>, <shift_operand>
    public ARMCortexMThumb $mvns(final Reg Rd, final Reg Rm, final Shift shift) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mvns", Rd, Rm, shift );
        return _$mvnx_w_shift_impl(true, Rd, Rm, shift, -1);
    }

    // [T2] MVN <Rd>, <Rm>, <shift_operand> #<imm5>
    public ARMCortexMThumb $mvn(final Reg Rd, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mvn", Rd, Rm, shift, imm5 );
        return _$mvnx_w_shift_impl(false, Rd, Rm, shift, imm5);
    }

    // [T2] MVN <Rd>, <Rm>, <shift_operand>
    public ARMCortexMThumb $mvn(final Reg Rd, final Reg Rm, final Shift shift) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$mvn", Rd, Rm, shift );
        return _$mvnx_w_shift_impl(false, Rd, Rm, shift, -1);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [PI] RSBS <Rd>, <Rm>, #0
    public ARMCortexMThumb $negs(final Reg Rd, final Reg Rm) throws JXMAsmError
    { return $rsbs(Rd, Rm, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] NOP
    // [PI] MOV r8, r8
    public ARMCortexMThumb $nop() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$nop" );

        if( _cpu.supInstARMv7MMinimal() ) {
            // T1
            _putOpCode16( 0b1011111100000000 );
        }
        else {
            // PI
            _putOpCode16( 0b0100011011000000 );
        }

        return this;
    }

    // [T2] NOP.W ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $nop_w() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$nop_w" );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$nop.w");

        _putOpCode32( 0b11110011101011111000000000000000L );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ORRS <Rdn>, <Rm>
    public ARMCortexMThumb $orrs(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$orrs", Rdn, Rm );

        _checkReg_Low(_MSTR, Rdn);
        _checkReg_Low(_MSTR, Rm );
        _putOpCode16( 0b0100001100000000 | (Rm.regNum << 3) | Rdn.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] POP   <registers>
    // [T2] POP.W <registers> ----- not available in 'ARMv8-M Baseline'
    // [T3] POP.W <Rt>        ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $pop(final Reg... Regs) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$pop", Regs );

        int mask = 0;

        if( _cpu.supInstARMv7MComplete() ) {
            for(final Reg R : Regs) mask |= (1 << R.regNum);
            if( (mask & 0b1111111100000000) != 0 ) {
                if( ( mask & (1 << Reg.LR.regNum) ) != 0 && ( mask & (1 << Reg.PC.regNum) ) != 0 ) _errorInvalidRegisterBothLRAndPCInList(_MSTR);
                if( ( mask & (1 << Reg.SP.regNum) ) != 0                                         ) _errorInvalidRegisterInList(_MSTR, Reg.SP);
                if( Regs.length > 1 ) {
                    // Cancel and use T1 below
                    if( (mask & 0b0111111100000000) == 0 ) {
                        mask = 0;
                    }
                    // T2
                    else{
                        _putOpCode32( 0b11101000101111010000000000000000L | mask );
                        return this;
                    }
                }
                else {
                    // Cancel and use T1 below
                    if( ( mask & (1 << Reg.PC.regNum) ) != 0 ) {
                        mask = 0;
                    }
                    // T3
                    else {
                        _putOpCode32( 0b11111000010111010000101100000100L | (Regs[0].regNum << 12) );
                        return this;
                    }
                }
            }
        }

        // T1
        if(mask == 0) {
            for(final Reg R : Regs) {
                if(R.regNum == Reg.PC.regNum) {
                    mask |= 0b0000000100000000;
                }
                else {
                    _checkReg_Low(_MSTR, R);
                    mask |= (1 << R.regNum);
                }
            }
        }

        _putOpCode16( 0b1011110000000000 | mask );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] PUSH   <registers>
    // [T2] PUSH.W <registers> ----- not available in 'ARMv8-M Baseline'
    // [T3] PUSH.W <Rt>        ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $push(final Reg... Regs) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$push", Regs );

        int mask = 0;

        if( _cpu.supInstARMv7MComplete() ) {
            for(final Reg R : Regs) mask |= (1 << R.regNum);
            if( (mask & 0b1111111100000000) != 0 ) {
                if( ( mask & (1 << Reg.SP.regNum) ) != 0 ) _errorInvalidRegisterInList(_MSTR, Reg.SP);
                if( ( mask & (1 << Reg.PC.regNum) ) != 0 ) _errorInvalidRegisterInList(_MSTR, Reg.PC);
                if( Regs.length > 1 ) {
                    // Cancel and use T1 below
                    if( (mask & 0b1011111100000000) == 0 ) {
                        mask = 0;
                    }
                    // T2
                    else{
                        _putOpCode32( 0b11101001001011010000000000000000L | mask );
                        return this;
                    }
                }
                else {
                    // Cancel and use T1 below
                    if( ( mask & (1 << Reg.LR.regNum) ) != 0 ) {
                        mask = 0;
                    }
                    // T3
                    else {
                        _putOpCode32( 0b11111000010011010000110100000100L | (Regs[0].regNum << 12) );
                        return this;
                    }
                }
            }
        }

        // T1
        if(mask == 0) {
            for(final Reg R : Regs) {
                if(R.regNum == Reg.LR.regNum) {
                    mask |= 0b0000000100000000;
                }
                else {
                    _checkReg_Low(_MSTR, R);
                    mask |= (1 << R.regNum);
                }
            }
        }

        _putOpCode16( 0b1011010000000000 | mask );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] REV <Rd>, <Rm>
    public ARMCortexMThumb $rev(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rev", Rd, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b1011101000000000 | (Rm.regNum << 3) | Rd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] REV16 <Rd>, <Rm>
    public ARMCortexMThumb $rev16(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rev16", Rd, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b1011101001000000 | (Rm.regNum << 3) | Rd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] REVSH <Rd>, <Rm>
    public ARMCortexMThumb $revsh(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$revsh", Rd, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b1011101011000000 | (Rm.regNum << 3) | Rd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T2] ROR{S}.W <Rd>, <Rm>, #<imm5> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$rors_w_impl(final boolean S, final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$ror{s}.w");

        final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
        if(S) _putOpCode32( 0b11101010010111110000000000110000L | ( (imm5 & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm5 & 0b00011) << 6 ) | Rm.regNum );
        else  _putOpCode32( 0b11101010010011110000000000110000L | ( (imm5 & 0b11100) << 10 ) | (Rd.regNum << 8) | ( (imm5 & 0b00011) << 6 ) | Rm.regNum );
        return this;
    }

    // [T2] ROR{S}.W <Rd>, <Rn>, <Rm> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$rors_w_impl(final boolean S, final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$ror{s}.w");

        if(S) _putOpCode32( 0b11111010011100001111000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        else  _putOpCode32( 0b11111010011000001111000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        return this;
    }

    // [T2] RORS.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $rors(final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rors", Rd, Rm, imm5 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$rors.w");

        return _$rors_w_impl(true, Rd, Rm, imm5);
    }

    // [T2] ROR.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $ror(final Reg Rd, final Reg Rm, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ror", Rd, Rm, imm5 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$ror.w");

        return _$rors_w_impl(false, Rd, Rm, imm5);
    }

    // [T1] RORS   <Rdn>, <Rm>
    // [T2] RORS.W <Rd >, <Rn>, <Rm>
    public ARMCortexMThumb $rors(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rors", Rdn, Rm );

        // T2
        if( _cpu.supInstARMv7MComplete() && ( !Rdn._isRegLow() || !Rm._isRegLow() ) ) {
            return _$rors_w_impl(true, Rdn, Rdn, Rm);
        }

        // T1
        else {
            _checkReg_Low(_MSTR, Rdn);
            _checkReg_Low(_MSTR, Rm );
            _putOpCode16( 0b0100000111000000 | (Rm.regNum << 3) | Rdn.regNum );
            return this;
        }
    }

    // [T2] RORS.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $rors(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        if(Rd.regNum == Rn.regNum) return $rors(Rd, Rm);

        _MNEMONIC_STRING( "$rors", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$rors.w");

        return _$rors_w_impl(true, Rd, Rn, Rm);
    }

    // [T2] ROR.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $ror(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$ror", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$ror.w");

        return _$rors_w_impl(false, Rd, Rn, Rm);
    }

    // [T2] ROR.W <Rdn>, <Rm>
    public ARMCortexMThumb $ror(final Reg Rdn, final Reg Rm) throws JXMAsmError
    { return $ror(Rdn, Rdn, Rm); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] RRX{S}.W <Rd>, <Rm> ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$rrxs_w_impl(final boolean S, final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstruction(_MSTR, "$rrx{s}.w");

        if(S) _putOpCode32( 0b11101010010111110000000000110000L | (Rd.regNum << 8) | Rm.regNum );
        else  _putOpCode32( 0b11101010010011110000000000110000L | (Rd.regNum << 8) | Rm.regNum );
        return this;
    }

    // [T1] RRXS <Rd>, <Rm>
    public ARMCortexMThumb $rrxs(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rrxs", Rd, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstruction(_MSTR, "$rrxs");

        return _$rrxs_w_impl(true, Rd, Rm);
    }

    // [T1] RRX <Rd>, <Rm>
    public ARMCortexMThumb $rrx(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rrx", Rd, Rm );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstruction(_MSTR, "$rrx");

        return _$rrxs_w_impl(false, Rd, Rm);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] RSBS <Rd>, <Rm>, #<imm12>
    public ARMCortexMThumb $rsbs(final Reg Rd, final Reg Rn, final int imm12) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$rsbs", Rd, Rn, imm12 );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rn);
        if(imm12 != 0) _errorInvalidImmediateValue(_MSTR, imm12);
        _putOpCode16( 0b0100001001000000 | (Rn.regNum << 3) | Rd.regNum );
        return this;
    }

    // [T1] RSBS <Rd>, <Rm>, #0
    public ARMCortexMThumb $rsbs0(final Reg Rd, final Reg Rn) throws JXMAsmError
    { return $rsbs(Rd, Rn, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SBCS <Rdn>, <Rm>
    public ARMCortexMThumb $sbcs(final Reg Rdn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sbcs", Rdn, Rm );

        _checkReg_Low(_MSTR, Rdn);
        _checkReg_Low(_MSTR, Rm );
        _putOpCode16( 0b0100000110000000 | (Rm.regNum << 3) | Rdn.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SDIV <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $sdiv(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sdiv", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MMinimal() ) _errorInvalidInstruction(_MSTR, "$sdiv");

        _putOpCode32( 0b11111011100100001111000011110000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SEV
    public ARMCortexMThumb $sev() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sev" );

        if( !_cpu.supInstARMv6MComplete() ) _errorInvalidInstruction(_MSTR, "$sev");

        _putOpCode16( 0b1011111101000000 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] STMIA <Rn>!, <registers>
    public ARMCortexMThumb $stmia(final RegWB Rn_, final Reg... Regs) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$stmia", Rn_, Regs );

        final boolean isWB = Rn_._isWB();
        final RegGen  Rn   = Rn_._unWB();

        if(!isWB) _errorInvalidInstructionForm(_MSTR, "$stmia");

        _checkReg_Low(_MSTR, Rn);

        int mask = 0;

        for(final Reg R : Regs) {
            _checkReg_Low(_MSTR, R);
            mask |= (1 << R.regNum);
        }

        _putOpCode16( 0b1100000000000000 | (Rn.regNum << 8) | mask );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T4] STR.W <Rt>, [<Rn>], ±#<imm8> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $str_pst(final Reg Rt, final Reg Rn, final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$str_pst", Rt, Rn, imm8 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$str.w");

        final boolean neg  = (imm8 < 0);
        final long    imm_ = Math.abs(imm8);
        final long    imm  = _checkUOffset(_MSTR, imm_, 0, 8);

                              // 111110000100nnnntttt1PUWiiiiiiii
        if(!neg) _putOpCode32( 0b11111000010000000000101100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
        else     _putOpCode32( 0b11111000010000000000100100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );

        return this;
    }

    // [T3] STR.W <Rt>, [<Rn>, #<imm12>]  ----- not available in 'ARMv8-M Baseline'
    // [T4] STR.W <Rt>, [<Rn>, -#<imm8>]  ----- not available in 'ARMv8-M Baseline'
    // [T4] STR.W <Rt>, [<Rn>, ±#<imm8>]! ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$str_w_impl(final boolean isWB, final Reg Rt, final RegGen Rn, final int imm8_imm12) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$str.w");

        // T3
        if( !isWB && imm8_imm12 >= 0 && imm8_imm12 <= ( (1 << 12) - 1 ) ) {
            final long imm = _checkUOffset(_MSTR, imm8_imm12, 0, 12);
            _putOpCode32( 0b11111000110000000000000000000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
        }
        // T4
        else {
            final boolean neg  = (imm8_imm12 < 0);
            final long    imm_ = Math.abs(imm8_imm12);
            final long    imm  = _checkUOffset(_MSTR, imm_, 0, 8);
                                                // 111110000100nnnntttt1PUWiiiiiiii
                 if(!neg && !isWB) _putOpCode32( 0b11111000010000000000111000000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
            else if(!neg &&  isWB) _putOpCode32( 0b11111000010000000000111100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
            else if( neg && !isWB) _putOpCode32( 0b11111000010000000000110000000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
            else if( neg &&  isWB) _putOpCode32( 0b11111000010000000000110100000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | imm );
        }

        return this;
    }

    // [T1] STR <Rt>, [<Rn>, #<imm5>]
    // [T2] STR <Rt>, [ SP , #<imm8>]
    public ARMCortexMThumb $str(final Reg Rt, final RegGen Rn_, final int imm5_imm8_imm12) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$str", Rt, Rn_, imm5_imm8_imm12 );

        final boolean isWB = Rn_._isWB();
        final RegGen  Rn   = Rn_._unWB();

        // T1/T3/T4
        if(Rn.regNum != Reg.SP.regNum) {
            // T3/T4
            if( _cpu.supInstARMv7MComplete() && ( isWB || !Rt._isRegLow() || !Rn._isRegLow() || imm5_imm8_imm12 < 0 || imm5_imm8_imm12 > ( ( (1 << 5) - 1 ) << 2 ) ) ) {
                return _$str_w_impl(isWB, Rt, Rn, imm5_imm8_imm12);
            }
            // T1
            else {
                _checkReg_Low(_MSTR, Rt);
                _checkReg_Low(_MSTR, Rn);
                final long imm = _checkUOffset(_MSTR, imm5_imm8_imm12, 2, 5);
                _putOpCode16( 0b0110000000000000 | (imm << 6) | (Rn.regNum << 3) | Rt.regNum );
            }
        }

        // T2
        else {
            _checkReg_Low(_MSTR, Rt);
            final long imm = _checkUOffset(_MSTR, imm5_imm8_imm12, 2, 8);
            _putOpCode16( 0b1001000000000000 | (Rt.regNum << 8) | imm );
        }

        return this;
    }

    // [T1] STR <Rt>, [<Rn>]
    // [T2] STR <Rt>, [ SP ]
    public ARMCortexMThumb $str(final Reg Rt, final Reg Rn) throws JXMAsmError
    { return $str(Rt, Rn, 0); }

    // [T1] STR <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $str(final Reg Rt, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$str", Rt, Rn, Rm );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0101000000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] STRB <Rt>, [<Rn>, #<imm5>]
    public ARMCortexMThumb $strb(final Reg Rt, final Reg Rn, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$strb", Rt, Rn, imm5 );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        final long imm = _checkUOffset(_MSTR, imm5, 0, 5);
        _putOpCode16( 0b0111000000000000 | (imm << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    // [T1] STRB <Rt>, [<Rn>]
    public ARMCortexMThumb $strb(final Reg Rt, final Reg Rn) throws JXMAsmError
    { return $strb(Rt, Rn, 0); }

    // [T1] STRB <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $strb(final Reg Rt, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$strb", Rt, Rn, Rm );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0101010000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] STRH <Rt>, [<Rn>, #<imm5>]
    public ARMCortexMThumb $strh(final Reg Rt, final Reg Rn, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$strh", Rt, Rn, imm5 );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        final long imm = _checkUOffset(_MSTR, imm5, 1, 5);
        _putOpCode16( 0b1000000000000000 | (imm << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    // [T1] STRH <Rt>, [<Rn>]
    public ARMCortexMThumb $strh(final Reg Rt, final Reg Rn) throws JXMAsmError
    { return $strh(Rt, Rn, 0); }

    // [T1] STRH <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $strh(final Reg Rt, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$strh", Rt, Rn, Rm );

        _checkReg_Low(_MSTR, Rt);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0101001000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rt.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] STL <Rt>, [<Rn>]
    public ARMCortexMThumb $stl(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$stl", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$stl");

        _putOpCode32( 0b11101000110000000000111110101111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] STLB <Rt>, [<Rn>]
    public ARMCortexMThumb $stlb(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$stlb", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$stlb");

        _putOpCode32( 0b11101000110000000000111110001111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] STLH <Rt>, [<Rn>]
    public ARMCortexMThumb $stlh(final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$stlh", Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$stlh");

        _putOpCode32( 0b11101000110000000000111110011111L | (Rn.regNum << 16) | (Rt.regNum << 12) );

        return this;
    }

    // [T1] STLEX <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $stlex(final Reg Rd, final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$stlex", Rd, Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$stlex");

        _putOpCode32( 0b11101000110000000000111111100000L | (Rn.regNum << 16) | (Rt.regNum << 12) | Rd.regNum );

        return this;
    }

    // [T1] STLEXB <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $stlexb(final Reg Rd, final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$stlexb", Rd, Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$stlexb");

        _putOpCode32( 0b11101000110000000000111111000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | Rd.regNum );

        return this;
    }

    // [T1] STLEXH <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $stlexh(final Reg Rd, final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$stlexh", Rd, Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$stlexh");

        _putOpCode32( 0b11101000110000000000111111010000L | (Rn.regNum << 16) | (Rt.regNum << 12) | Rd.regNum );

        return this;
    }

    // [T1] STREX <Rd>, <Rt>, [<Rn>, #<imm8>]
    public ARMCortexMThumb $strex(final Reg Rd, final Reg Rt, final Reg Rn, final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$strex", Rd, Rt, Rn, imm8 );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$strex");

        final long imm = _checkUOffset(_MSTR, imm8, 2, 8);

        _putOpCode32( 0b11101000010000000000000000000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | (Rd.regNum << 8) | imm );

        return this;
    }

    // [T1] STREX <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $strex(final Reg Rd, final Reg Rt, final Reg Rn) throws JXMAsmError
    { return $strex(Rd, Rt, Rn, 0); }

    // [T1] STREXB <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $strexb(final Reg Rd, final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$strexb", Rd, Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$strexb");

        _putOpCode32( 0b11101000110000000000111101000000L | (Rn.regNum << 16) | (Rt.regNum << 12) | Rd.regNum  );

        return this;
    }

    // [T1] STREXH <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $strexh(final Reg Rd, final Reg Rt, final Reg Rn) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$strexh", Rd, Rt, Rn );

        if( !_cpu.supInstARMv8MBaseline() ) _errorInvalidInstruction(_MSTR, "$strexh");

        _putOpCode32( 0b11101000110000000000111101010000L | (Rn.regNum << 16) | (Rt.regNum << 12) | Rd.regNum  );

        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SUBS <Rd>, <Rn>, #<imm3>
    public ARMCortexMThumb $subs(final Reg Rd, final Reg Rn, final int imm3) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$subs", Rd, Rn, imm3 );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rn);
        final long imm = _checkUOffset(_MSTR, imm3, 0, 3);
        _putOpCode16( 0b0001111000000000 | (imm << 6) | (Rn.regNum << 3) | Rd.regNum );
        return this;
    }

    // [T2] SUBS <Rdn>, #<imm8>
    public ARMCortexMThumb $subs(final Reg Rdn, final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$subs", Rdn, imm8 );

        _checkReg_Low(_MSTR, Rdn);
        final long imm = _checkUOffset(_MSTR, imm8, 0, 8);
        _putOpCode16( 0b0011100000000000 | (Rdn.regNum << 8) | imm );
        return this;
    }

    // [T1] SUBS <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $subs(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$subs", Rd, Rn, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rn);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b0001101000000000 | (Rm.regNum << 6) | (Rn.regNum << 3) | Rd.regNum );
        return this;
    }

    // [T1] SUB SP, SP, #<imm7>
    public ARMCortexMThumb $sub(final Reg Rd, final Reg Rn, final int imm7) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sub", Rd, Rn, imm7 );

        _checkReg_SP(_MSTR, Rd);
        _checkReg_SP(_MSTR, Rn);
        final long imm = _checkUOffset(_MSTR, imm7, 2, 7);
        _putOpCode16( 0b1011000010000000 | imm );
        return this;
    }

    // [PI] SUB SP, SP, #<imm7>
    public ARMCortexMThumb $sub_sp(final int imm7) throws JXMAsmError
    { return $sub(Reg.SP, Reg.SP, imm7); }

    // [T4] SUBW <Rd>, <Rn>, #<imm12> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $subw(final Reg Rd, final Reg Rn, final int imm12) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$subw", Rd, Rn, imm12 );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstruction(_MSTR, "$subw");

        final long imm = _splitImmediate12___I_IMM3_IMM8(_MSTR, imm12);
        _putOpCode32( 0b11110010101000000000000000000000L | (Rn.regNum << 16) | (Rd.regNum << 8) | imm );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SVC #<imm8>
    public ARMCortexMThumb $svc(final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$svc", imm8 );

        final long imm = _checkUOffset(_MSTR, imm8, 0, 8);
        _putOpCode16( 0b1101111100000000 | imm );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SXTB <Rd>, <Rm>
    public ARMCortexMThumb $sxtb(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sxtb", Rd, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b1011001001000000 | (Rm.regNum << 3) | Rd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SXTH <Rd>, <Rm>
    public ARMCortexMThumb $sxth(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$sxth", Rd, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b1011001000000000 | (Rm.regNum << 3) | Rd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] TST <Rn>, #<const> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $tst(final Reg Rn, final long const_) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$tst", Rn, const_ );

        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$tst");

        final long imm = _fixupConstant32___I_IMM3_ABCDEFGH( const_);
        if(imm < 0) _errorInvalidConstantAfterFixup(_MSTR, const_);

        _putOpCode32( 0b11110000000100000000111100000000L | (Rn.regNum << 16) | imm );
        return this;
    }

    // [T2] TST.W <Rn>, <Rm>, <shift_operand> {#<imm5>} ----- not available in 'ARMv8-M Baseline'
    protected ARMCortexMThumb _$tst_w_impl(final Reg Rn, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    {
        if( !_cpu.supInstARMv7MComplete() ) _errorInvalidInstructionForm(_MSTR, "$tst.w");

        final long[] imm_type = _encodeShiftAppliedToRegister___IMM_TYPE(_MSTR, shift, imm5);

        long imm  = _splitImmediate5___IMM3_IMM2( _MSTR, imm_type[0] );
        long type =                                     imm_type[1]  ;

        _putOpCode32( 0b11101010000100000000111100000000L | (Rn.regNum << 16) | (type << 4) | Rm.regNum | imm );

        return this;
    }

    // [T1] TST   <Rn>, <Rm>
    // [T2] TST.W <Rn>, <Rm> ----- not available in 'ARMv8-M Baseline'
    public ARMCortexMThumb $tst(final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        // !!! ERROR: Wrong mnemonic operand - uses Rm twice instead of Rn, Rm. Fix: change to _MNEMONIC_STRING( "$tst", Rn, Rm ).
        _MNEMONIC_STRING( "$tst", Rm, Rm );

        // T2
        if( _cpu.supInstARMv7MComplete() && ( !Rn._isRegLow() || !Rm._isRegLow() ) ) {
            return _$tst_w_impl(Rn, Rm, Shift.LSL, 0);
        }

        // T1
        else {
            _checkReg_Low(_MSTR, Rn);
            _checkReg_Low(_MSTR, Rm);
            _putOpCode16( 0b0100001000000000 | (Rm.regNum << 3) | Rn.regNum );
            return this;
        }
    }

    // [T2] TST.W <Rn>, <Rm>, <shift_operand> {#<imm5>}
    public ARMCortexMThumb $tst(final Reg Rn, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$tst", Rn, Rm, shift, imm5 );
        return _$tst_w_impl(Rn, Rm, shift, imm5);
    }

    // [T2] TST.W <Rn>, <Rm>, <shift_operand>
    public ARMCortexMThumb $tst(final Reg Rn, final Reg Rm, final Shift shift) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$tst", Rn, Rm, shift );
        return _$tst_w_impl(Rn, Rm, shift, -1);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] UDF #<imm8>
    public ARMCortexMThumb $udf(final int imm8) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$udf", imm8 );

        final long imm = _checkUOffset(_MSTR, imm8, 0, 8);
        _putOpCode16( 0b1101111000000000 | imm8 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] UDIV <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $udiv(final Reg Rd, final Reg Rn, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$udiv", Rd, Rn, Rm );

        if( !_cpu.supInstARMv7MMinimal() ) _errorInvalidInstruction(_MSTR, "$udiv");

        _putOpCode32( 0b11111011101100001111000011110000L | (Rn.regNum << 16) | (Rd.regNum << 8) | Rm.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] UXTB <Rd>, <Rm>
    public ARMCortexMThumb $uxtb(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$uxtb", Rd, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b1011001011000000 | (Rm.regNum << 3) | Rd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] UXTH <Rd>, <Rm>
    public ARMCortexMThumb $uxth(final Reg Rd, final Reg Rm) throws JXMAsmError
    {
        _MNEMONIC_STRING( "$uxth", Rd, Rm );

        _checkReg_Low(_MSTR, Rd);
        _checkReg_Low(_MSTR, Rm);
        _putOpCode16( 0b1011001010000000 | (Rm.regNum << 3) | Rd.regNum );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] WFE
    public ARMCortexMThumb $wfe() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$wfe" );

        if( !_cpu.supInstARMv6MComplete() ) _errorInvalidInstruction(_MSTR, "$wfe");

        _putOpCode16( 0b1011111100100000 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] WFI
    public ARMCortexMThumb $wfi() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$wfi" );

        if( !_cpu.supInstARMv6MComplete() ) _errorInvalidInstruction(_MSTR, "$wfi");

        _putOpCode16( 0b1011111100110000 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] YIELD
    public ARMCortexMThumb $yield() throws JXMAsmError
    {
        _MNEMONIC_STRING( "$yield" );

        _putOpCode16( 0b1011111100010000 );
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String ARM_OBJDUMP_BINARY = null;

    public static void setARMObjDumpBinary(final String path)
    { ARM_OBJDUMP_BINARY = path; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long[] _link_impl(final long originAddress, final boolean dumpDisassemblyAndArray) throws JXMAsmError
    {
        // Ensure the end address is word-aligned
        $_align(2);

        // Resolve the data address(es) and target label(s)
        final long endAddress = _opcode.getNextAddress();

        for(final OpCode opcode : _opcode) {

            // Resolve the opcode parameter if it present
            if(opcode.opcodeParam != null) {

                // Shortcut for convenience
                final OpCodeParam ocp = opcode.opcodeParam;

                // Get the target label
                      String  targetLabel = ocp.paramLabel;
                final boolean relToPC     = targetLabel.startsWith("PC:");
                final int     relToPCOfs  = relToPC ? Integer.valueOf( targetLabel.substring(3, 4) ) : 0;

                // Remove hints from the target label
                if(relToPC) targetLabel = targetLabel.substring(5);

                // Determine the Thumb bit
                final Boolean IsFunction = !targetLabel.isEmpty() ? _lfuncs.get(targetLabel)  : null ;
                final boolean isFunction = (IsFunction != null)   ? IsFunction.booleanValue() : false;
                final int     thumbBit   =  isFunction            ? 0x01                      : 0x00 ;

                // Determine the address
                long address = -1;

                if(ocp.paramDataAddress >= 0) {
                    // Resolve the address
                                address  = ocp.paramDataAddress + endAddress;
                    if(relToPC) address -= (opcode.address + relToPCOfs);
                    else        address += originAddress;
                }
                else if(ocp.paramLabel != null) {
                    // Get the target opcode
                    final OpCode target = _labels.get(targetLabel);
                    if(target == null) _errorInvalidTargetLabel(opcode.mString, targetLabel);
                    // Resolve the address
                  //SysUtil.stdDbg().printf("### %s [%b][%d] | %08X\n", ocp.paramLabel, relToPC, relToPCOfs, target.address);
                                address  = target.address + target.bitSize / 8;
                    if(relToPC) address -= (opcode.address + relToPCOfs);
                    else        address += originAddress;
                  //if(ocp.paramFunc != null) SysUtil.stdDbg().printf("[%b:%d] %08X -> %08X\n", relToPC, relToPCOfs, opcode.address, address);
                    _labelsAddr.put(targetLabel, target.address + target.bitSize / 8); // ##### !!! TODO : Further verify this !!! #####
                }

                // Check and shift the address
                if(ocp.paramFunc == null) {
                    if(ocp.valueBitShiftFactor != 0 || ocp.valueMaxNumBits != 0) {
                        if(ocp.valueBitShiftFactor < 0 || ocp.valueMaxNumBits < 0) {
                            address = _checkSOffset( opcode.mString, address, Math.abs(ocp.valueBitShiftFactor), Math.abs(ocp.valueMaxNumBits) );
                        }
                        else {
                            address = _checkUOffset( opcode.mString, address, Math.abs(ocp.valueBitShiftFactor), Math.abs(ocp.valueMaxNumBits) );
                        }
                    }
                }

                /*
                 * # BLX label always changes the state.
                 * # BX Rm and BLX Rm derive the target state from bit[0] of Rm:
                 *       # If bit[0] of Rm is 0, the processor changes to, or remains in, ARM state
                 *       # If bit[0] of Rm is 1, the processor changes to, or remains in, Thumb state
                 */
                 // ##### !!! TODO : How to ensure that 'thumbBit' is only applied when needed??? !!! #####
              //if(isFunction) SysUtil.stdDbg().println("### ### ### " + opcode.mString + " " + thumbBit);
                // Store the address to the opcode
                /*
                if(ocp.paramFunc != null) opcode.opcodeValue  = ocp.paramFunc.apply(opcode, address, thumbBit);
                else                      opcode.opcodeValue |= ( (address | thumbBit) << ocp.paramBitShiftPos );
                */
                if(ocp.paramFunc != null) {
                    opcode.opcodeValue  = ocp.paramFunc.apply(opcode, address, thumbBit);
                }
                else {
                    if(isFunction && opcode.bitSize == 16) {
                        if(    ( (opcode.opcodeValue & 0b1110000000000000) == 0b1110000000000000 ) // "$b"
                            || ( (opcode.opcodeValue & 0b1101000000000000) == 0b1101000000000000 ) // "$b<c>"
                          ) {
                              _errorInvalidTargetLabelTypeFunction(opcode.mString, targetLabel);
                        }
                    }
                    opcode.opcodeValue |= ( (address | thumbBit) << ocp.paramBitShiftPos );
                }

                // Clear the parameter(s)
                opcode.opcodeParam = null;

            } // if

            // Swap the opcode half-words as needed
            if(opcode.swapHWords) opcode.opcodeValue = _swapU32___U16_U16(opcode.opcodeValue);

        } // for

        // Combine all
        final ArrayList<Byte> prog = new ArrayList<> ();

        for(final OpCode opcode : _opcode) {
                prog.add( (byte) ( (opcode.opcodeValue >>  0) & 0xFF ) );
            if(opcode.bitSize >= 16) {
                prog.add( (byte) ( (opcode.opcodeValue >>  8) & 0xFF ) );
            }
            if(opcode.bitSize >= 32) {
                prog.add( (byte) ( (opcode.opcodeValue >> 16) & 0xFF ) );
                prog.add( (byte) ( (opcode.opcodeValue >> 24) & 0xFF ) );
            }
        }

        prog.addAll(_data);

        // Dump as needed
        if( (ForceDumpDisassembly || dumpDisassemblyAndArray) && ARM_OBJDUMP_BINARY != null ) {
            try {
                // Save the result to a temporary binary file
                final String                   bin = SysUtil.resolvePath( "__ARMCortexMThumb__.bin", SysUtil.getRootTmpDir() );
                final java.io.FileOutputStream fos = new java.io.FileOutputStream(bin);
                for(final byte b : prog) fos.write(b);
                fos.flush();
                fos.close();
                // Disassemble the temporary binary file
                SysUtil.stdDbg().println( SysUtil.execlp(
                    ARM_OBJDUMP_BINARY,
                    "-m", "arm", "-EL", "--disassembler-options=force-thumb", "--adjust-vma=" + originAddress,
                    "-b", "binary", "-D", "-z", bin
                ) );
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                throw XCom.newJXMAsmError( e.getMessage() );
            }
        }

        // Generate the final result
        final Iterator<Byte> it  = prog.iterator();
        final long[]         res = new long[ prog.size() / 4 ];
              int            idx = 0;

        while( it.hasNext() ) {
            final long b0 = it.next() & 0xFF;
            final long b1 = it.next() & 0xFF;
            final long b2 = it.next() & 0xFF;
            final long b3 = it.next() & 0xFF;
            res[idx++] = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
        }

        // Dump as needed
        if(ForceDumpArray || dumpDisassemblyAndArray) {
            final String  INDENT1 = "    ";
            final String  INDENTM = INDENT1 + INDENT1 + INDENT1 + INDENT1;
                  boolean nl      = false;
            SysUtil.stdDbg().println(INDENTM + "private static final long[] _ARMCortexMThumb_program_dump = new long[] {");
            for(int i = 0; i < res.length; ++i) {
                nl = false;
                if( (i % 8) == 0 ) SysUtil.stdDbg().print(INDENT1 + INDENTM);
                SysUtil.stdDbg().printf( "0x%08XL%s ", res[i], (i < res.length - 1) ? "," : "" );
                if( ( (i + 1) % 8 ) == 0 ) {
                    SysUtil.stdDbg().println();
                    nl = true;
                }
            }
            if(!nl) SysUtil.stdDbg().println();
            SysUtil.stdDbg().printf( INDENTM + "};\n\n"                                  );
            SysUtil.stdDbg().printf( INDENTM + "### .text = %d bytes ###\n", prog.size() );
        }

        // Return the final result
        return res;
    }

    private void _clear()
    {
        _data  .clear();
        _dataVM.clear();

        _opcode.clear();
        _labels.clear();
        _lfuncs.clear();

        _itFCond = Cond._NONE_;
        _itSCond = null;
        _itICond = -1;
        _itBlock.clear();
    }

} // class ARMCortexMThumb
