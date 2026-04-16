/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc.pe;


import jxm.*;
import jxm.ugc.*;
import jxm.xb.*;

import static jxm.ugc.PIC16BitEPC.Cond.*;
import static jxm.ugc.PIC16BitEPC.Reg.*;
import static jxm.ugc.PIC16BitEPC.Expr.*;


/*
 * This is an implementation of a programming executive for dsPIC30F* MCUs, partially following the protocol
 * outlined in the referenced documents below.
 *
 * Please note that it may not be fully compatible with Microchip's own implementation:
 *     1. Programmers adhering strictly to the PHY waveform protocol and timing may not work
 *        properly with this firmware.
 *     2. Some of the non-strictly required commands are not yet implemented.
 *     3. Some of the implemented commands are simplified (e.g., ignoring some parameters,
 *        minimal/no error checking, etc.).
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * dsPIC30F Family Datasheet
 * https://ww1.microchip.com/downloads/en/DeviceDoc/70046E.pdf
 *
 * dsPIC30F1010⁄202x SMPS Datasheet
 * https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/70000178d.pdf
 *
 * dsPIC30F3010/3011 Datasheet
 * https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ProductDocuments/DataSheets/70141F.pdf
 *
 * dsPIC30F Flash Microcontroller Programming Specification (with STDP)
 * https://ww1.microchip.com/downloads/en/DeviceDoc/70102c.pdf
 * https://ww1.microchip.com/downloads/en/DeviceDoc/70102K.pdf
 *
 * dsPIC30F SMPS Flash Microcontroller Programming Specification
 * https://www.microchip.com/content/dam/mchp/documents/OTH/ProductDocuments/ProgrammingSpecifications/70284C.pdf
 *
 * dsPIC30F/33F Programmer's Reference Manual
 * https://ww1.microchip.com/downloads/en/devicedoc/70157c.pdf
 *
 * ~~~ Last accessed & checked on 2024-11-03 ~~~
 */
public class dsPIC30F_PE extends PIC16BitEPC {

    private static final String ClassName = "dsPIC30F_PE";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

                                                 //   dsPIC30F   dsPIC30F-SMPS
    private static final int[] WREG0    = new int[] { 0x000   ,  0x000 };
    private static final int[] WREG1    = new int[] { 0x002   ,  0x002 };
    private static final int[] WREG2    = new int[] { 0x004   ,  0x004 };
    private static final int[] WREG3    = new int[] { 0x006   ,  0x006 };
    private static final int[] WREG4    = new int[] { 0x008   ,  0x008 };
    private static final int[] WREG5    = new int[] { 0x00A   ,  0x00A };
    private static final int[] WREG6    = new int[] { 0x00C   ,  0x00C };
    private static final int[] WREG7    = new int[] { 0x00E   ,  0x00E };
    private static final int[] WREG8    = new int[] { 0x010   ,  0x010 };
    private static final int[] WREG9    = new int[] { 0x012   ,  0x012 };
    private static final int[] WREG10   = new int[] { 0x014   ,  0x014 };
    private static final int[] WREG11   = new int[] { 0x016   ,  0x016 };
    private static final int[] WREG12   = new int[] { 0x018   ,  0x018 };
    private static final int[] WREG13   = new int[] { 0x01A   ,  0x01A };
    private static final int[] WREG14   = new int[] { 0x01C   ,  0x01C };
    private static final int[] WREG15   = new int[] { 0x01E   ,  0x01E };
    private static final int[] SPLIM    = new int[] { 0x020   ,  0x020 };
    private static final int[] ACCAL    = new int[] { 0x022   ,  0x022 };
    private static final int[] ACCAH    = new int[] { 0x024   ,  0x024 };
    private static final int[] ACCAU    = new int[] { 0x026   ,  0x026 };
    private static final int[] TBLPAG   = new int[] { 0x032   ,  0x032 };
    private static final int[] SR       = new int[] { 0x042   ,  0x042 };
    private static final int[] CORCON   = new int[] { 0x044   ,  0x044 };
    private static final int[] IFS0     = new int[] { 0x084   ,  0x084 };
    private static final int[] IEC0     = new int[] { 0x08C   ,  0x094 }; // @@@
    private static final int[] IPC0     = new int[] { 0x094   ,  0x0A4 }; // @@@
    private static final int[] IPC2     = new int[] { 0x098   ,  0x0A8 }; // @@@
    private static final int[] TMR1     = new int[] { 0x100   ,  0x100 };
    private static final int[] PR1      = new int[] { 0x102   ,  0x102 };
    private static final int[] T1CON    = new int[] { 0x104   ,  0x104 };
    private static final int[] ADPCFG   = new int[] { 0x2A8   ,  0x302 }; // @@@
    private static final int[] TRISB    = new int[] { 0x2C6   ,  0x2C6 };
    private static final int[] PORTB    = new int[] { 0x2C8   ,  0x2C8 };
    private static final int[] SPI1STAT = new int[] { 0x220   ,  0x240 }; // @@@
    private static final int[] SPI1CON  = new int[] { 0x222   ,  0x242 }; // @@@
    private static final int[] SPI1BUF  = new int[] { 0x224   ,  0x246 }; // @@@
    private static final int[] NVMCON   = new int[] { 0x760   ,  0x760 };
    private static final int[] NVMADR   = new int[] { 0x762   ,  0x762 };
    private static final int[] NVMADRU  = new int[] { 0x764   ,  0x764 };
    private static final int[] NVMKEY   = new int[] { 0x766   ,  0x766 };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * SRAM Allocation
     *
     * RAM_BEG                                                                                                    RAM_END
     * ↓                                                                                                          ↓
     * 000 001 ---------------------------------------- 128 129 ---------- 160 161 ------------------------------ 254 255
     * <buffer>---------------------------------------- <tmp_vars>-------- <stack>---------------------------------------
     */

    private static final int RAM_BEG     = 0x0800;
    private static final int RAM_LEN     = 0x0100;
    private static final int RAM_END     = RAM_BEG + RAM_LEN - 2;

    private static final int BUF_ADR     = RAM_BEG +   0;
    private static final int VAR_BEG     = RAM_BEG + 128;

    private static final int STK_BEG     = RAM_BEG + 160;
    private static final int STK_END     = RAM_END;

    private static final int TMP_DVAR0   = VAR_BEG +   0;
    private static final int TMP_AVAR1   = VAR_BEG +   2; // NOTE : These two variables must be in sequence
    private static final int TMP_AVAR2   = VAR_BEG +   4; //        because some parts of the code will access
    private static final int TMP_AVAR3   = VAR_BEG +   6; //        them using offset from the first one

    private static final int PE_START    = 0x800000;
    private static final int PE_MAX_SIZE = 736 * 3;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _t = -1;

    private dsPIC30F_PE()
    { super(CPU.dsPIC30F); }

    private void _delayUS(final int us) throws JXMAsmError
    {
        /*
         * Frequency of dsPIC30F MCUs internal RC oscillator:
         *     # Newer versions = 7.37MHz -> each cycle takes ~0.543uS
         *     # Older versions = 8   MHz -> each cycle takes ~0.500uS
         */
            $repeat     (           lit(us * 2)                                     );
            $nop        (                                                           );
    }

    private void _delayMS(final int ms) throws JXMAsmError
    { _delayUS(ms *1000); }

    private void _spiWaitRX() throws JXMAsmError
    {
        label("0");
            $btss       (           SPI1STAT[_t]    , lit(0)                        );
            $bra        (           "0b"                                            );
    }

    private void _spiReadToW0() throws JXMAsmError
    {
            _spiWaitRX  (                                                           );
            $mov        (           SPI1BUF[_t]     , W0                            );
    }

    private void _spiReadToW9() throws JXMAsmError
    {
            _spiWaitRX  (                                                           );
            $mov        (           SPI1BUF[_t]     , W9                            );
    }

    private int[] _getPE(final int type) throws JXMAsmError
    {
        // ===== ===== ===== Save the type ===== ===== =====
        _t = type;

        // ===== ===== ===== Generate the firmware ===== ===== =====

        // ----- ----- ----- Reset vector ----- ----- -----
        $_org(0x0000);
            $goto       ( Label_Start                 );

        // ----- ----- ----- Main program ----- ----- -----
        $_org(0x0080, true);
        label_Start();
            $mov        (   lit(TMP_DVAR0)  , W10                                   ); // W10      = #TMP_DVAR0
            $mov        (   lit(TMP_AVAR1)  , W11                                   ); // W11      = #TMP_AVAR1
            $mov        (   lit(TMP_AVAR2)  , W12                                   ); // W12      = #TMP_AVAR2

            $mov        (   lit(STK_BEG)    , W15                                   ); // W15 (SP) = #STK_BEG

            $mov        (   lit(STK_END)    , W0                                    ); //
            $mov        (   W0              , SPLIM[_t]                             ); // SPLIM    = #STK_END

            _delayMS    (   1                                                       ); // Delay ~1.09mS (or ~1mS)

            $bclr       (   CORCON[_t]      , lit(7)                                ); // Accumulator A    saturation disabled
            $bclr       (   CORCON[_t]      , lit(6)                                ); // Accumulator B    saturation disabled
            $bclr       (   CORCON[_t]      , lit(5)                                ); // Data space write saturation disabled

            $setm       (   ADPCFG[_t]                                              ); // ADPCFG = #0xFFFF
                                                                                       //        ▶ set all analog input pins to digital mode

            $mov        (   SR[_t]          , W0                                    ); //
            $mov        (   lit(0xFF1F)     , W1                                    ); //
            $and        (   W0              , W1             , W0                   ); //      ******** IPL *****
            $ior        (   lit(0x00A0)     , W0                                    ); // SR |= 00000000 101 00000
            $mov        (   W0              , SR[_t]                                ); //     ▶ CPU interrupt priority is set to level 5

            $mov        (   lit(0x0C00)     , W0                                    ); //           . FRMEN SPIFSD   DISSDO MODE16 SMP CKE SSEN CKP MSTEN SPRE PPRE
            $mov        (   W0              , SPI1CON[_t]                           ); // SPI1CON = 0 0     0      0 1      1      0   0   0    0   0     000  00
                                                                                       //         ▶ 16-bit SPI slave mode 1 without SS; SDO controlled by port register

            $clr        (   IPC2[_t]                                                ); //        . ADIP . U1TXIP . U1RXIP . SPI1IP
            $bset       (   IPC2[_t]        , lit(0)                                ); // IPC2 = 0 000  0 000    0 000    0 001
                                                                                       //      ▶ SPI1 interrupt priority is set to level 1

            $mov        (   lit(0x8000)     , W0                                    ); //            SPIEN . SPISIDL ...... SPIROV .... SPITBF SPIRBF
            $mov        (   W0              , SPI1STAT[_t]                          ); // SPI1STAT = 1     0 0       000000 0      0000 0      0

            $bclr       (   IEC0[_t]        , lit(8)                                ); // Disable SPI1 interrupt

            $clr        (   NVMADR[_t]                                              ); // Clear the NVM address registers
            $clr        (   NVMADRU[_t]                                             ); //

        label("0");                                                                    // The main loop
            $rcall      (   "peReadCommand"                                         ); //
            $rcall      (   "peProcessCommand"                                      ); //
            $bra        (   "0b"                                                    ); //

        // ----- ----- ----- Functions ----- ----- -----
        label("peReadCommand");
            _spiReadToW0(                                                           ); // W0 = <1st_word> (<opcode4_length12>)
            $mov        (   lit(BUF_ADR)    , W7                                    ); // W7 = BUF_ADR (address of the buffer)
            $mov        (   W0              , ind_pp(W7)                            ); // Store the <1st_word> to the buffer
            $mov        (   lit(0x0FFF)     , W3                                    ); // Get the length
            $and        (   W0              , W3            , W3                    ); //     W3 = <length12>
            $dec        (   W3              , W3                                    ); //     W3 = <length12> - 1
            $bra        (   Z               , "1f"                                  ); // Exit if the length is zero
        label("0");
            _spiReadToW0(                                                           ); // W0 = <next_word>
            $mov        (   W0              , ind_pp(W7)                            ); // Store the <next_word> to the buffer
            $dec        (   W3              , W3                                    ); // Decrement the counter
            $bra        (   NZ              , "0b"                                  ); // Loop until all the command data has been read
        label("1");
            $bclr       (   SPI1STAT[_t]    , lit(15)                               ); // Disable SPI
            $mov        (   lit(0x0500)     , W0                                    ); //           . FRMEN SPIFSD   DISSDO MODE16 SMP CKE SSEN CKP MSTEN SPRE PPRE
            $mov        (   W0              , SPI1CON[_t]                           ); // SPI1CON = 0 0     0      0 0      1      0   1   0    0   0     000  00
                                                                                       //         ▶ 16-bit SPI slave mode 0 without SS; SDO controlled by module
            $bset       (   SPI1STAT[_t]    , lit(15)                               ); // Enable SPI
            $setm       (   SPI1BUF[_t]                                             ); // SPI1BUF = #0xFFFF - P9a is 10uS minimum (~20 cycles)
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peProcessCommand");
            $mov        (   BUF_ADR         , W0                                    ); // W0 = [BUF_ADR] = <opcode4_length12>
            $lsr        (   W0              , lit(12)       , W0                    ); // W0 = <opcode4> (0x0 - 0xF)
            $bra        (   W0                                                      ); // Perform computed branch
            $bra        (   "peCMD_SCHECK"                                          ); //     0x0 - SCHECK
            $bra        (   "peCMD_READP_READD"                                     ); //     0x1 - READD
            $bra        (   "peCMD_READP_READD"                                     ); //     0x2 - READP
            $bra        (   "peCMD_INVALID"                                         ); //     0x3 - <reserved> - invalid command
            $bra        (   "peCMD_PROGD"                                           ); //     0x4 - PROGD
            $bra        (   "peCMD_PROGP"                                           ); //     0x5 - PROGP
            $bra        (   "peCMD_PROGC"                                           ); //     0x6 - PROGC
            $bra        (   "peCMD_ERASEB"                                          ); //     0x7 - ERASEB
            $bra        (   "peCMD_ERASED"                                          ); //     0x8 - ERASED
            $bra        (   "peCMD_ERASEP"                                          ); //     0x9 - ERASEP
            $bra        (   "peCMD_QBLANK"                                          ); //     0xA - QBLANK
            $bra        (   "peCMD_QVER"                                            ); //     0xB - QVER
            $bra        (   "peCMD_INVALID"                                         ); //     0xC - <reserved> - invalid command
            $bra        (   "peCMD_INVALID"                                         ); //     0xD - <reserved> - invalid command
            $bra        (   "peCMD_INVALID"                                         ); //     0xE - <reserved> - invalid command
            $bra        (   "peCMD_INVALID"                                         ); //     0xF - <reserved> - invalid command
            // There is no need for a 'RETURN' statement here because the handlers above already end with 'RETURN'

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peSendResponse");
            $nop        (                                                           );
            $nop        (                                                           );
            $mov        (   ind(W11)        , W0                                    ); // W0           = [TMP_AVAR1]
            $lsr        (   W0              , lit(12)       , W0                    ); // W0           = <res4> (0x1, 0x2, 0x3)
            $cp         (   W0              , lit(0x1)                              ); // Compare
            $bra        (   Z               , "0f"                                  ); //     --- and jump if <res4> == 0x1
            $cp         (   W0              , lit(0x3)                              ); // Compare
            $bra        (   Z               , "0f"                                  ); //     --- and jump if <res4> == 0x3
            $mov_b      (   lit(0x01)       , W0                                    ); //
            $mov_b      (   W0              , ind(W11)                              ); // [TMP_AVAR1]  = 0x0001
            $bra        (   "1f"                                                    );
        label("0");
            $clr_b      (   ind(W11)                                                ); // [TMP_AVAR1] &= 0xFF00
        label("1");
            $nop        (                                                           );
            $mov        (   BUF_ADR         , W1                                    ); // W1          = [BUF_ADR] = <opcode4_length12>
            $lsr        (   W1              , lit(12)       , W1                    ); // W1          = <opcode4> (0x000l)
            $sl         (   W1              , lit(8)        , W1                    ); // W1          = 0x0l00
            $mov        (   lit(0xF0FF)     , W0                                    ); // W0          = #0xF0FF
            $and        (   W0              , ind(W11)      , W0                    ); // W0          = [TMP_AVAR1] & 0xF0FF = 0xo0qq
            $ior        (   W0              , W1            , W0                    ); // W0          = 0xolqq
            $mov        (   W0              , ind(W11)                              ); // [TMP_AVAR1] = 0xolqq
            $lsr        (   W0              , lit(8)        , W0                    ); // W0          = 0x00ol
            $and        (   W0              , lit(0x0F)     , W0                    ); // W0          = 0x000l
            $cp         (   W0              , lit(0x1)                              ); // Compare
            $bra        (   Z               , "3f"                                  ); //     --- and jump if 0x000l == 0x1 (READD)
            $cp         (   W0              , lit(0x2)                              ); // Compare
            $bra        (   Z               , "2f"                                  ); //     --- and jump if 0x000l == 0x2 (READP)
            $mov        (   lit(0x0002)     , W0                                    ); // W0          = #0x0002
            $mov        (   W0              , TMP_AVAR2                             ); // [TMP_AVAR2] = #0x0002
            $bra        (   "9f"                                                    );
        label("2");
            $mov        (   BUF_ADR + 2     , W0                                    ); // W0          = [BUF_ADR + 2] = <length16>
            $lsr        (   W0              , W2                                    ); // W2          = W0 / 2
            $mul_uu     (   W2              , lit(3)        , W2                    ); // W2          = W2 * 3
            $inc2       (   W2              , W2                                    ); // W2          = W2 + 2
            $mov        (   W2              , TMP_AVAR2                             ); // [TMP_AVAR2] = <length16> / 2 * 3 + 2
            $btst_z     (   W0              , lit(0)                                ); // Check if bit #0 of W0
            $bra        (   Z               , "9f"                                  ); //     --- and jump if it is zero
        label("3");
            $mov        (   BUF_ADR + 2     , W0                                    ); // W0          = [BUF_ADR + 2] = <length12>
            $mov        (   lit(0x0FFF)     , W1                                    ); // W1          = #0x0FFF
            $and        (   W0              , W1            , W0                    ); // W0          = [BUF_ADR + 2] & 0x0FFF = 0x0lll
            $inc2       (   W0              , W0                                    ); // W0          = 0x0lll + 2
            $mov        (   W0              , TMP_AVAR2                             ); // [TMP_AVAR2] = 0x0lll + 2
        label("9");
            $rcall      (   "spiSend"                                               ); // Send the bytes
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peSendResponseNV");
            $mov        (   W11             , W7                                    ); // W7 = #TMP_AVAR1
            $mov        (   lit(0x0FFF)     , W0                                    ); // W0 = 0x0FFF
            $and        (   W0              , ind(W7)       , W0                    ); // W0 = 0x0lqq
            $bset       (   W0              , lit(12)                               ); // W0 = 0x1lqq
            $mov        (   W0              , ind(W7)                               ); // [TMP_AVAR1] = 0x1lqq
            $rcall      (   "peSendResponse"                                        ); // Send the response
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("spiSend");
            $clr        (   SPI1BUF[_t]                                             ); // Clear the SPI1BUF
            _delayUS    (   15                                                      ); // Delay ~16.3uS (or ~15uS) - P9b is 15uS minimum (~30 cycles)

            $bclr       (   SPI1STAT[_t]    , lit(15)                               ); // Disable SPI
            $mov        (   lit(0x0400)     , W0                                    ); //           . FRMEN SPIFSD   DISSDO MODE16 SMP CKE SSEN CKP MSTEN SPRE PPRE
            $mov        (   W0              , SPI1CON[_t]                           ); // SPI1CON = 0 0     0      0 0      1      0   0   0    0   0     000  00
                                                                                       //         ▶ 16-bit SPI slave mode 1 without SS; SDO controlled by module
            $bset       (   SPI1STAT[_t]    , lit(15)                               ); // Enable SPI

            $mov        (   W11             , W7                                    ); // W7     = #TMP_AVAR1

            $mov        (   ind_pp(W7)      , W0                                    ); // W0     = [TMP_AVAR1]
            $mov        (   W0              , SPI1BUF[_t]                           ); // Send [TMP_AVAR1] (0xolqq)

            $mov        (   ind_pp(W7)      , W0                                    ); // W0     = [TMP_AVAR2]
            $rcall      (   "_spiSend_d1s1"                                         ); // Send [TMP_AVAR2] (0xllll)

            $mov        (   W11             , W0                                    ); // W0     = #TMP_AVAR1
            $mov        (   ind(W0, 2)      , W5                                    ); // W5     = [TMP_AVAR2] (0xllll) (<length16>/<length12>)
            $dec2       (   W5              , W5                                    ); // W5     = 0xllll - 2
            $bra        (   Z               , "9f"                                  ); //     --- and jump if it is zero (no program/data memory word to send)

            $mov_b      (   ind(W0, 1)      , W0                                    ); // W0     = ( [TMP_AVAR1] & 0xFF00 ) >> 8 = 0xol
            $and_b      (   lit(0x0F)       , W0                                    ); // W0     = 0x0l
            $mov_b      (   lit(0x01)       , W1                                    ); // W1     = #0x01
            $cp_b       (   W0              , W1                                    ); // Compare
            $bra        (   NZ              , "1f"                                  ); //     --- and jump if 0xl != 0x1 (READD)

            /*
             * READD    : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x1           ;   length12    = 0x4
             *            rrrrnnnnnnnnnnnn      reserved4   = 0x0           ;   length12    = length of data memory to read (in 16-bit words)
             *            rrrrrrrrhhhhhhhh      reserved8   = 0x0           ;   haddr8      = address MSB
             *            llllllllllllllll                                      laddr16     = address LSB
             */
            $mov        (   lit(BUF_ADR)    , W0                                    ); // W0     = #BUF_ADR
            $mov_b      (   ind(W0, 4)      , W1                                    ); // W1[LO] = [BUF_ADR + 4] = <haddr8>
            $ze         (   W1              , W1                                    ); // W1[HI] = 0x00
            $mov        (   W1              , TBLPAG[_t]                            ); // TBLPAG = <haddr8>
            $mov        (   ind(W0, 6)      , W7                                    ); // W7     = <laddr16>
        label("0");
            $tblrdl     (   ind_pp(W7)      , W0                                    ); // W0     = [TBLPAG:W7]
            $rcall      (   "_spiSend_d1s1"                                         ); // Send the data memory word
            $dec        (   W5              , W5                                    ); // Decrement the counter
            $bra        (   NZ              , "0b"                                  ); // Loop until all the data words have been sent
            $bra        (   "9f"                                                    ); // Done

        label("1");
            $mov_b      (   lit(0x02)       , W1                                    ); // W1 = #0x02
            $cp_b       (   W0              , W1                                    ); // Compare
            $bra        (   NZ              , "9f"                                  ); //     --- and jump if 0xl != 0x2 (READP)

            /*
             * READP    : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x2           ;   length12    = 0x4
             *            nnnnnnnnnnnnnnnn                                      length16    = length of program memory to read (in 24-bit words)
             *            rrrrrrrrhhhhhhhh      reserved8   = 0x0           ;   haddr8      = address MSB
             *            llllllllllllllll                                      laddr16     = address LSB
             */
            $mov        (   lit(BUF_ADR)    , W0                                    ); // W0     = #BUF_ADR
            $mov        (   ind(W0, 2)      , W5                                    ); // W5     = <length16>
            $lsr        (   W5              , W6                                    ); // W6     = W5 / 2
            /*
             *   ACCA       [ ACCAU ACCAH  ACCAL  ]   address couter     0xHH_LLLL_0000
             * [ 40-bit ]   [ 8-bit 16-bit 16-bit ]
             *
             *   ACCB       [ ACCBU ACCBH  ACCBL  ]   increment factor   0x00_0002_0000
             * [ 40-bit ]   [ 8-bit 16-bit 16-bit ]
             */
            $clr        (   W1                                                      ); // W1     = 0x0000
            $mov        (   W1              , ACCAL[_t]                             ); // ACCAL  = 0x0000
            $mov        (   ind(W0, 6)      , W1                                    ); // W1     = <laddr16>
            $mov        (   W1              , ACCAH[_t]                             ); // ACCAH  = <laddr16>
            $mov_b      (   ind(W0, 4)      , W1                                    ); // W1     = <reserved8_haddr8> & 0x00FF = <haddr8>
            $mov        (   W1              , ACCAU[_t]                             ); // ACCAU  = <haddr8>

            $dec        (   W5              , W0                                    ); // W0     = W5 - 1 = 0xllll - 1
            $bra        (   Z               , "3f"                                  ); //     --- and jump if it is zero (meaning, only one word needs to be read)

            $mov        (   lit(0x0002)     , W0                                    ); // W0     = #0x0002
            $lac        (   W0              , B                                     ); // ACCB   = 0x00_0002_0000

            $mov        (   lit(WREG0[_t])  , W1                                    ); // W1     = #WREG0

            // Read an even number of words
            /*
             * Packed data format
             *       1111110000000000
             *       5432109876543210
             *     0 ------LSW0------
             *     1 --MSB1----MSB0--
             *     2 ------LSW1------
             */
        label("2");
            $mov        (   ACCAU[_t]       , W2                                    ); // W2     = <haddr8>
            $mov        (   W2              , TBLPAG[_t]                            ); // TBLPAG = <haddr8>
            $mov        (   ACCAH[_t]       , W7                                    ); // W7     = <laddr16>
            $nop        (                                                           );
            $tblrdl     (   ind(W7)         , W0                                    ); // W0     = <LSW0>
            $rcall      (   "_spiSend_d1s1"                                         ); // Send the program memory word
            $tblrdh_b   (   ind(W7)         , ind_pp(W1)                            ); // W0[LO] = <MSB0>

            $add        (   A                                                       ); // ACCA   = ACCA + ACCB
            $mov        (   ACCAU[_t]       , W2                                    ); // W2     = <haddr8>
            $mov        (   W2              , TBLPAG[_t]                            ); // TBLPAG = <haddr8>
            $mov        (   ACCAH[_t]       , W7                                    ); // W7     = <laddr16>
            $nop        (                                                           );
            $tblrdh_b   (   ind(W7)         , ind_mm(W1)                            ); // W0[HI] = <MSB1>
            $rcall      (   "_spiSend_d1s1"                                         ); // Send the program memory word

            $tblrdl     (   ind(W7)         , W0                                    ); // W0     = <LSW1>
            $rcall      (   "_spiSend_d1s1"                                         ); // Send the program memory word
            $add        (   A                                                       ); // ACCA   = ACCA + ACCB
            $dec        (   W6              , W6                                    ); // Decrement the counter
            $bra        (   NZ              , "2b"                                  ); // Loop until all the data words have been sent

            $btss       (   W5              , lit(0)                                ); // Check bit #0 of the original counter
            $bra        (   "9f"                                                    ); //    --- and jump if it is zero (meaning, no more words need to be read)

            // Read one (more) word
        label("3");
            $mov        (   ACCAU[_t]       , W2                                    ); // W2     = <haddr8>
            $mov        (   W2              , TBLPAG[_t]                            ); // TBLPAG = <haddr8>
            $mov        (   ACCAH[_t]       , W7                                    ); // W7     = <laddr16>
            $nop        (                                                           );

            $tblrdl     (   ind(W7)         , W0                                    ); // W0     = <LSW0>
            $rcall      (   "_spiSend_d1s1"                                         ); // Send the program memory word
            $tblrdh_b   (   ind(W7)         , W0                                    ); // W0[LO] = <MSB0>
            $ze         (   W0              , W0                                    ); // W0[HI] = 0x00
            $rcall      (   "_spiSend_d1s1"                                         ); // Send the program memory word

        label("9");
            _spiReadToW9(                                                           ); // Discard one word
            $bclr       (   SPI1STAT[_t]    , lit(15)                               ); // Disable SPI
            $bset       (   SPI1CON[_t]     , lit(11)                               ); // Make SDO controlled by port register
            $bset       (   SPI1STAT[_t]    , lit(15)                               ); // Enable SPI
            $return     (                                                           );

        label("_spiSend_d1s1");
            _spiReadToW9(                                                           ); // Discard one word
            $mov        (   W0              , SPI1BUF[_t]                           ); // Send the program/data memory word
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("executeNVMCMD");
            $push       (   W0                                                      ); //
            $push       (   SR[_t]                                                  ); //

            $mov        (   lit(0x00E0)     , W0                                    ); //
            $ior        (   SR[_t]                                                  ); // Disable interrupts

            $bset       (   NVMCON[_t]      , lit(14)                               ); // Enable an erase or program operation

            $mov        (   lit(0x0055)     , W0                                    ); //
            $mov        (   W0              , NVMKEY[_t]                            ); // NVMKEY = #0x0055
            $mov        (   lit(0x00AA)     , W0                                    ); //
            $mov        (   W0              , NVMKEY[_t]                            ); // NVMKEY = #0x0055

            $bset       (   NVMCON[_t]      , lit(15)                               ); // Initiate a or program flash or data EEPROM erase or write cycle
            $nop        (                                                           ); // Insert two NOPs after the erase cycle (required)
            $nop        (                                                           ); //

            _delayMS    (   1                                                       ); // Delay ~1.09mS (or ~1mS)
        label("0");
            $mov        (   NVMCON[_t]      , W7                                    ); // W7 = NVMCON
            $btsc       (   W7              , lit(15)                               ); // Check bit #15 of NVMCON
            $bra        (   "0b"                                                    ); //    --- and jump if it is not zero (meaning, the operation is not yet complete)

            $pop        (   SR[_t]                                                  ); // Re-enable interrupts
            $pop        (   W0                                                      ); //
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_INVALID");
            /*
             * Response : 0x3l00
             *            0x0002
             */
            $mov        (   lit(0x3000)     , W0                                    ); // W0          = #0x3000 (NACK)
            $mov        (   W0              , ind(W11)                              ); // [TMP_AVAR1] = #0x3000
            $rcall      (   "peSendResponse"                                        ); // Send the response
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_SCHECK");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x0           ;   length12    = 0x1
             *
             * Response : 0x1000
             *            0x0002
             */
            $mov        (   W11             , W7                                    ); // W7 = #TMP_AVAR1
            $mov        (   lit(0x1000)     , W0                                    ); //
            $mov        (   W0              , ind_pp(W7)                            ); // [TMP_AVAR1] = #0x1000
            $mov        (   lit(0x0002)     , W0                                    ); //
            $mov        (   W0              , ind(W7)                               ); // [TMP_AVAR2] = #0x0002
            _delayUS    (   5                                                       ); // Delay ~5.4uS (or ~5uS) - additional delay to satisfy P9a
            $rcall      (   "spiSend"                                               ); // Send the bytes
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_QVER");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0xB           ;   length12    = 0x1
             *
             * Response : 0x1Bmn
             *            0x0002
             */
            $mov        (   W11             , W7                                    ); // W7 = #TMP_AVAR1
            $mov        (   lit(0x1BF1)     , W0                                    ); //
            $mov        (   W0              , ind_pp(W7)                            ); // [TMP_AVAR1] = #0x1BF1 (use 0xF1 as the version number here)
            $mov        (   lit(0x0002)     , W0                                    ); //
            $mov        (   W0              , ind(W7)                               ); // [TMP_AVAR2] = #0x0002
            _delayUS    (   5                                                       ); // Delay ~5.4uS (or ~5uS) - additional delay to satisfy P9a
            $rcall      (   "spiSend"                                               ); // Send the bytes
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_READP_READD");
            /*
             * READP
             *
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x2           ;   length12    = 0x4
             *            nnnnnnnnnnnnnnnn                                      length16    = length of program memory to read (in 24-bit words)
             *            rrrrrrrrhhhhhhhh      reserved8   = 0x0           ;   haddr8      = address MSB
             *            llllllllllllllll                                      laddr16     = address LSB
             *
             * Response : 0x1200                (for even 'length16')
             *            2 + 3 * N / 2
             *            lsbprogmemword_1
             *            ...
             *            lsbprogmemword_N
             *
             *            0x1200                (for odd  'length16')
             *            4 + 3 * (N - 1) / 2
             *            lsbprogmemword_1
             *            ...
             *            msbprogmemword_N      (zero padded)
             *
             * ----------------------------------------------------------------------------------------------------
             *
             * READD
             *
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x1           ;   length12    = 0x4
             *            rrrrnnnnnnnnnnnn      reserved4   = 0x0           ;   length12    = length of data memory to read (in 16-bit words)
             *            rrrrrrrrhhhhhhhh      reserved8   = 0x0           ;   haddr8      = address MSB
             *            llllllllllllllll                                      laddr16     = address LSB
             *
             * Response : 0x1100
             *            N + 2
             *            dataword16bit_1
             *            ...
             *            dataword16bit_N
             */
            $mov        (   lit(0x1000)     , W0                                    ); // W0          = #0x1000 (PASS)
            $mov        (   W0              , ind(W11)                              ); // [TMP_AVAR1] = #0x1000
            $rcall      (   "peSendResponse"                                        ); // Send the response and the program/data memory words
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_ERASEB");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll   opcode4        = 0x7           ; length12      = 0x2
             *            rrrrrrrrrrrrrmmm   reserved13     = 0x0           ; mmm           = 0x0 : all code in general segment
             *                                                                                0x1 : all data EEPROM in general segment
             *                                                                                0x2 : all code and data EEPROM in general segment - interrupt vectors - FGS configuration register
             *                                                                                0x3 : full chip erase
             *                                                                                0x4 : all code and data EEPROM in boot, secure, and general segments - FBS, FSS, and FGS configuration registers
             *                                                                                0x5 : all code and data EEPROM in secure and general segments - FSS and FGS configuration registers
             *                                                                                0x6 : all data EEPROM in boot segment
             *                                                                                0x7 : all data EEPROM in secure segment
             *
             * Response : 0x1700
             *            0x0002
             */
            // ##### !!! TODO : Implement it (only the important mode)? !!! #####
        for(int i = 0; i < 25; ++i) {
            $nopr       (                                                           );
        }
            $bra        (   "peCMD_INVALID"                                         );
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_ERASEP");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4    = 0x9            ;   length12    = 0x3
             *            nnnnnnnnhhhhhhhh      numrows8   = number of      ;   haddr8      = address MSB
             *            llllllllllllllll                   rows to erase      laddr16     = address LSB
             *
             * Response : 0x1900
             *            0x0002
             */
            $mov        (   lit(0x4041)     , W0                                    ); // Erase 1 row (32 instruction words) from 1 panel of code memory
            $mov        (   W0              , NVMCON[_t]                            ); //

            $mov        (   lit(BUF_ADR)    , W7                                    ); // W7      = #BUF_ADR
            $nop        (                                                           );

            $mov_b      (   ind(W7, 2)      , W0                                    ); // W0      = <numrows8_haddr8> & 0x00FF = <haddr8>
            $ze         (   W0              , W1                                    ); // W1      = <haddr8>
            $mov        (   ind(W7, 4)      , W2                                    ); // W2      = <laddr16>

            $mov        (   W1              , W5                                    ); // W5      = <haddr8>
            $mov        (   W2              , W4                                    ); // W4      = <laddr16>

            $mov_b      (   ind(W7, 3)      , W3                                    ); // W3      = <numrows8>
            $cp0_b      (   W3                                                      ); // Check <numrows8>
            $bra        (   Z               , "9f"                                  ); //     --- and jump if it is zero (no row to erase)

        label("0");
            $mov        (   W5              , W0                                    ); //
            $mov_b      (   WREG            , NVMADRU[_t]                           ); // NVMADRU = <haddr8>
            $mov        (   W4              , NVMADR[_t]                            ); // NVMADR  = <laddr16>
            $rcall      (   "executeNVMCMD"                                         ); // Execute the NVM command
            $add        (   lit(64)         , W4                                    ); // W4      = <orig_laddr16> + 64 (each row is 32 words)
            $addc       (   lit(0)          , W5                                    ); // W5      = <orig_haddr8>  + C
            $dec        (   W3              , W3                                    ); // Decrement the counter
            $bra        (   NZ              , "0b"                                  ); // Loop until all the rows have been read

        label("9");
            $rcall      (   "peSendResponseNV"                                      ); // Send the response
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_ERASED");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x8           ;   length12    = 0x3
             *            nnnnnnnnhhhhhhhh      numrows8    = number of     ;   haddr8      = address MSB
             *            llllllllllllllll                    rows to erase     laddr16     = address LSB
             *
             * Response : 0x1800
             *            0x0002
             */
            $mov        (   lit(0x4045)     , W0                                    ); // Erase 1 row (16 words) of data memory
            $mov        (   W0              , NVMCON[_t]                            ); //

            $mov_b      (   lit(0x7F)       , W0                                    ); // W0      = 0x7F (the NVM upper address of the EEPROM is always 0x7F)
            $mov_b      (   WREG            , NVMADRU[_t]                           ); // NVMADRU = 0x7F
            $mov        (   BUF_ADR + 4     , W0                                    ); // W0      = <laddr16>
            $mov        (   W0              , NVMADR[_t]                            ); // NVMADR  = <laddr16>

            $mov        (   BUF_ADR + 2     , W3                                    ); // W3      = <numrows8_haddr8>
            $lsr        (   W3              , lit(8)        , W3                    ); // W3      = <numrows8>
            $and        (   lit(0x007F)     , W3                                    ); // W3      = <numrows8> & 0x7F

            $cp0_b      (   W3                                                      ); // Check <numrows8>
            $bra        (   Z               , "9f"                                  ); //     --- and jump if it is zero (no row to erase)

        label("0");
            $rcall      (   "executeNVMCMD"                                         ); // Execute the NVM command
            $mov        (   lit(32)         , W0                                    ); //
            $add        (   NVMADR[_t]                                              ); // NVMADR  = NVMADR + 32 (each row is 16 words)
            $dec        (   W3              , W3                                    ); // Decrement the counter
            $bra        (   NZ              , "0b"                                  ); // Loop until all the rows have been read

        label("9");
            $rcall      (   "peSendResponseNV"                                      ); // Send the response
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_PROGP");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x5           ;   length12    = 0x33
             *            rrrrrrrrhhhhhhhh      reserved8   = 0x0           ;   haddr8      = address MSB
             *            llllllllllllllll                                      laddr16     = address LSB
             *            dataword16bit_01
             *            dataword16bit_02
             *            ...
             *            dataword16bit_48
             *
             * Response : 0x1500
             *            0x0002
             */
            $mov        (   lit(0x4001)     , W0                                    ); // Write 1 row (32 instruction words) into 1 panel of program memory
            $mov        (   W0              , NVMCON[_t]                            ); //

            $mov        (   lit(BUF_ADR)    , W7                                    ); // W7         = #BUF_ADR
            $clr        (   W0                                                      ); //
            $mov_b      (   ind(W7, 2)      , W0                                    ); // W0         = <haddr8>
            $mov        (   W0              , ACCAU[_t]                             ); // ACCAU      = <haddr8>
            $mov        (   ind(W7, 4)      , W0                                    ); // W0         = <laddr16>
            $mov        (   W0              , ACCAH[_t]                             ); // ACCAH      = <laddr16>
            $add        (   W7              , lit(6)        , W6                    ); // W6         = #BUF_ADR + 6 (the beginning of row data)

            $mov        (   ACCAU[_t]       , W0                                    ); //
            $mov        (   W0              , TBLPAG[_t]                            ); // TBLPAG      = <haddr8>
            $mov        (   ACCAH[_t]       , W7                                    ); // W7          = <laddr16>

            $mov        (   lit(8)          , W3                                    ); // Repeat 8 times
        label("0");
        for(int i = 0; i < 2; ++i) {
            $tblwtl     (   ind_pp(W6)      ,    ind   (W7)                         ); // Send a program memory word
            $tblwth_b   (   ind_pp(W6)      ,    ind_pp(W7)                         ); // ---
            $tblwth_b   (   ind_pp(W6)      , pp_ind   (W7)                         ); // ---
            $tblwtl     (   ind_pp(W6)      ,    ind_pp(W7)                         ); // ---
        }
            $dec        (   W3              , W3                                    ); // Decrement the counter
            $bra        (   NZ              , "0b"                                  ); // Loop until all the program memory words have been sent

            $rcall      (   "executeNVMCMD"                                         ); // Execute the NVM command

            $rcall      (   "_peCMD_VERIFYP"                                        ); // Verify
            $sl         (   W0              , lit(12)       , W0                    ); // W0          = 0xl000
            $mov        (   W11             , W1                                    ); // W1          = #TMP_AVAR1
            $mov        (   W0              , ind(W1)                               ); // [TMP_AVAR1] = 0xl000
            $rcall      (   "peSendResponse"                                        ); // Send the response
            $return     (                                                           );

        label("_peCMD_VERIFYP");
            $mov        (   lit(BUF_ADR)    , W0                                    ); // W0          = #BUF_ADR
            $clr        (   W1                                                      ); //
            $mov_b      (   ind(W0, 2)      , W1                                    ); // W1          = <haddr8>
            $mov        (   W1              , ACCAU[_t]                             ); // ACCAU       = <haddr8>
            $mov        (   ind(W0, 4)      , W1                                    ); // W1          = <laddr16>
            $mov        (   W1              , ACCAH[_t]                             ); // ACCAH       = <laddr16>
            $add        (   W0              , lit(6)        , W6                    ); // W6          = #BUF_ADR + 6 (the beginning of row data)
            $mov        (   lit(WREG3[_t])  , W7                                    ); // W7          = #WREG3

            $mov        (   ACCAU[_t]       , W0                                    ); //
            $mov        (   W0              , TBLPAG[_t]                            ); // TBLPAG      = <haddr8>
            $mov        (   ACCAH[_t]       , W5                                    ); // W5          = <laddr16>

            $mov        (   lit(16)         , W4                                    ); // Repeat 16 times
        label("0");
            $tblrdl     (   ind(W5)         , W3                                    ); // W3          = <LSW0>
            $cp         (   W3              , ind_pp(W6)                            ); // Compare with the reference word
            $bra        (   NZ              , "1f"                                  ); //    --- and jump if not equal
            $tblrdh_b   (   ind_pp(W5)      , ind_pp(W7)                            ); // W3[LO]      = <MSB0>
            $tblrdh_b   (   pp_ind(W5)      , ind_mm(W7)                            ); // W3[HI]      = <MSB1>
            $cp         (   W3              , ind_pp(W6)                            ); // Compare with the reference word
            $bra        (   NZ              , "1f"                                  ); //    --- and jump if not equal
            $tblrdl     (   ind_pp(W5)      , W3                                    ); // W3          = <LSW1>
            $cp         (   W3              , ind_pp(W6)                            ); // Compare with the reference word
            $bra        (   NZ              , "1f"                                  ); //    --- and jump if not equal
            $dec        (   W4              , W4                                    ); // Decrement the counter
            $bra        (   NZ              , "0b"                                  ); // Loop until all the words have been verified

            $mov        (   lit(0x0001)     , W0                                    ); // W0      = #0x0001 (PASS)
            $bra        (   "9f"                                                    ); // Done
        label("1");
            $mov        (   lit(0x0002)     , W0                                    ); // W0      = #0x0002 (FAIL)
        label("9");
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_PROGD");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x4           ;   length12    = 0x13
             *            rrrrrrrrhhhhhhhh      reserved8   = 0x0           ;   haddr8      = address MSB
             *            llllllllllllllll                                      laddr16     = address LSB
             *            dataword16bit_01
             *            dataword16bit_02
             *            ...
             *            dataword16bit_16
             *
             * Response : 0x1400
             *            0x0002
             */
            $mov        (   lit(0x4005)     , W0                                    ); // Write 1 row (16 words) to data memory
            $mov        (   W0              , NVMCON[_t]                            ); //

            $mov        (   lit(BUF_ADR + 6), W6                                    ); // W6          = #BUF_ADR + 6 (the beginning of row data)

            $mov_b      (   lit(0x7F)       , W0                                    ); // W0          = 0x7F (the NVM upper address of the EEPROM is always 0x7F)
            $mov_b      (   WREG            , TBLPAG[_t]                            ); // TBLPAG      = 0x7F
            $mov        (   BUF_ADR + 4     , W7                                    ); // W7          = <laddr16>

            $repeat     (   lit(15)                                                 ); // Store 16 data memory words
            $tblwtl     (   ind_pp(W6)      , ind_pp(W7)                            ); //

            $rcall      (   "executeNVMCMD"                                         ); // Execute the NVM command

            $rcall      (   "_peCMD_VERIFYD"                                        ); // Verify
            $sl         (   W0              , lit(12)       , W0                    ); // W0          = 0xl000
            $mov        (   W11             , W1                                    ); // W1          = #TMP_AVAR1
            $mov        (   W0              , ind(W1)                               ); // [TMP_AVAR1] = 0xl000
            $rcall      (   "peSendResponse"                                        ); // Send the response
            $return     (                                                           );

        label("_peCMD_VERIFYD");
            $mov        (   lit(BUF_ADR)    , W7                                    ); // W7          = #BUF_ADR
            $add        (   W7              , lit(6)        , W1                    ); // W1          = #BUF_ADR + 6

            $mov_b      (   lit(0x7F)       , W0                                    ); // W0          = 0x7F (the NVM upper address of the EEPROM is always 0x7F)
            $mov_b      (   WREG            , TBLPAG[_t]                            ); // TBLPAG      = 0x7F
            $mov        (   ind(W7, 4)      , W7                                    ); // W7          = <laddr16>

            $mov        (   lit(0x0002)     , W0                                    ); // W0          = #0x0002 (FAIL)
            $mov        (   lit(16)         , W3                                    ); // Repeat 16 times
        label("1");
            $tblrdl     (   ind_pp(W7)      , W2                                    ); // Read one word
            $cp         (   W2              , ind_pp(W1)                            ); // Compare with the reference word
            $bra        (   NZ              , "9f"                                  ); //    --- and jump if not equal
            $dec        (   W3              , W3                                    ); // Decrement the counter
            $bra        (   NZ              , "1b"                                  ); // Loop until all the words have been verified

            $mov        (   lit(0x0001)     , W0                                    ); // W0          = #0x0001 (PASS)
        label("9");
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_PROGC");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0x6   ;   length12    = 0x4
             *            rrrrrrrrhhhhhhhh      reserved8   = 0x0   ;   haddr8      = address MSB
             *            llllllllllllllll                              laddr16     = address LSB
             *            cfgdataword16bit
             *
             * Response : 0x1600
             *            0x0002
             */
            // ##### !!! TODO : Implement it? !!! #####
        for(int i = 0; i < 15; ++i) {
            $nopr       (                                                           );
        }
            $bra        (   "peCMD_INVALID"                                         );
            $return     (                                                           );

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        label("peCMD_QBLANK");
            /*
             * Command  : 1111110000000000
             *            5432109876543210
             *            oooollllllllllll      opcode4     = 0xA   ;   length12    = 0x3
             *            pppppppppppppppp                              psize16     = length of program memory to check (in 24-bit words)
             *            rrrrdddddddddddd      reserved4   = 0x0   ;   dsize12     = length of data    memory to check (in 16-bit words)
             *
             * Response : 0x1AF0   (for     blank device)
             *            0x0002
             *
             *            0x1A0F   (for non-blank device)
             *            0x0002
             */
            // ##### !!! TODO : Implement it? !!! #####
        for(int i = 0; i < 50; ++i) {
            $nopr       (                                                           );
        }
            $bra        (   "peCMD_INVALID"                                         );
            $return     (                                                           );

        // ----- ----- ----- Application ID ----- ----- -----
        // PE instruction address range : 0x800000 - 0x8005BE -> 1472 instructions (2208 bytes)
        // Application ID               :            0x8005BE  [0xBB]
        $_org(0x05BE, true);
            $_word(0x00BB);

        // ===== ===== ===== Link and return the result ===== ===== =====
        final int[] pe = link(PE_START);

        if(pe.length > PE_MAX_SIZE) throw XCom.newJXMAsmError( Texts.ASM_ProgramSizeTooLarge, ClassName, pe.length, PE_MAX_SIZE);

        return pe;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int         TYPE_DSPIC30F      = 0;
    private static int         TYPE_DSPIC30F_SMPS = 1;

    private static dsPIC30F_PE _pe                = new dsPIC30F_PE();

    private static int[] getPE_dsPIC30(final int type) throws JXMAsmError
    { return _pe._getPE(type); }

    private static int[] getArray24Bits_dsPIC30(final int type, final boolean dumpArray) throws JXMAsmError
    {
        final int[] buff24 = PIC16BitEPC.toArray24Bits( getPE_dsPIC30(type) );

        if(dumpArray) PIC16BitEPC.dumpArray24Bits(buff24);

        return buff24;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int getPE_dsPIC30F_startAddress() throws JXMAsmError
    { return PE_START; }

    public static int[] getPE_dsPIC30F() throws JXMAsmError
    { return getPE_dsPIC30(TYPE_DSPIC30F); }

    public static int[] getArray24Bits_dsPIC30F(final boolean dumpArray) throws JXMAsmError
    { return getArray24Bits_dsPIC30(TYPE_DSPIC30F, dumpArray); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int getPE_dsPIC30F_SMPS_startAddress() throws JXMAsmError
    { return PE_START; }

    public static int[] getPE_dsPIC30F_SMPS() throws JXMAsmError
    { return getPE_dsPIC30(TYPE_DSPIC30F_SMPS); }

    public static int[] getArray24Bits_dsPIC30F_SMPS(final boolean dumpArray) throws JXMAsmError
    { return getArray24Bits_dsPIC30(TYPE_DSPIC30F_SMPS, dumpArray); }

} // class dsPIC30F_PE
