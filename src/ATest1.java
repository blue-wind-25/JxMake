/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


import jxm.*;
import jxm.ugc.*;
import jxm.ugc.fl.*;
import jxm.ugc.pe.*;
import jxm.xb.*;

import static jxm.ugc.ARMCortexMThumb.CoProc;
import static jxm.ugc.ARMCortexMThumb.CPU;
import static jxm.ugc.ARMCortexMThumb.CReg;
import static jxm.ugc.ARMCortexMThumb.Reg;
import static jxm.ugc.ARMCortexMThumb.Shift;
import static jxm.ugc.ARMCortexMThumb.SYSm;

import static jxm.ugc.PIC16BitEPC.CPU.*;
import static jxm.ugc.PIC16BitEPC.Cond.*;
import static jxm.ugc.PIC16BitEPC.Reg.*;
import static jxm.ugc.PIC16BitEPC.Expr.*;


//
// Test class (the test application entry point)
//
public class ATest1 {

    public static void main(final String[] args)
    {

        /*
        SysUtil.stdDbg().println( String.join(" ", SysUtil.getJavaCmd() ) );
        SysUtil.systemExit();
        //*/

        try {

            SysUtil.stdDbg().println();
            SysUtil._init_ll_test_app(args);

            // Test 'PIC16BitEPC' - all instructions
            PIC16BitEPC.setPICObjDumpBinary(
                !true
                ? ( SysUtil.getUHD() + "/xsdk/pic/xc16/v2.10/bin/xc16-objdump" ) // can be used for PIC24* and dsPIC* (it may have disassembly bugs)
                : ( SysUtil.getUHD() + "/xsdk/pic/xc-dsc/bin/xc-dsc-objdump"   ) // best        for dsPIC*
            );

            if(!true) {

                final boolean           xal = !true;               // 0       1       2       3         4         5         6         7
                final PIC16BitEPC.CPU[] cpu = new PIC16BitEPC.CPU[] { PIC24F, PIC24E, PIC24H, dsPIC30F, dsPIC33F, dsPIC33E, dsPIC33C, _ANY_};
                final PIC16BitEPC       asm = new PIC16BitEPC(cpu[6]) {{
                        $goto       ( Label_Start                 );

                        //*
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $goto       ( 0x0200                      );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $mov        ( lit(0xAA55), W0             );
                        $nop        (                             );
                        $mov        ( W0         , 0x0F88         );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $clr        ( W7                          );
                        $tblwtl     ( W0, ind   (W7)              );
                        $tblwth     ( W1, ind_pp(W7)              );
                        $tblwtl     ( W2, ind   (W7)              );
                        $tblwth     ( W3, ind_pp(W7)              );
                        //*/

                        //*
                        $nopr       (                             );
                        $call       ( W7                          );
                        $call_l     ( W6                          );
                        $nopr       (                             );
                        $goto       ( W7                          );
                        $goto_l     ( W6                          );
                        $nopr       (                             );
                        $bra        ( W7                          );
                        $bra        ( "-2"                        );
                        $bra        ( "+0"                        );
                        $bra        ( "+2"                        );
                        $nopr       (                             );
                        $rcall      ( W7                          );
                        $rcall      ( "-2"                        );
                        $rcall      ( "+0"                        );
                        $rcall      ( "+2"                        );
                        $nopr       (                             );
                        $repeat     ( lit(true ? 32767 : 16383)   );
                        $nopr       (                             );
                        $repeat     ( W7                          );
                        $nopr       (                             );
                        $lac_d      (          W2    , lit(-8), B );
                        $lac_d      (   ind   (W3   ), lit( 7), A );
                        $lac_d      (   ind_pp(W3   ), lit( 5), A );
                        $lac_d      (   ind_mm(W3   ), lit( 3), A );
                        $lac_d      ( pp_ind  (W3   ), lit(-3), A );
                        $lac_d      ( mm_ind  (W3   ), lit(-5), A );
                        $nopr       (                             );
                        $sac_d      ( B, lit(-8),           W2    );
                        $sac_d      ( A, lit( 7),    ind   (W3)   );
                        $sac_d      ( A, lit( 5),    ind_pp(W3)   );
                        $sac_d      ( A, lit( 3),    ind_mm(W3)   );
                        $sac_d      ( A, lit(-3), pp_ind   (W3)   );
                        $sac_d      ( A, lit(-5), mm_ind   (W3)   );
                        $nopr       (                             );
                        $nopr       (                             );
                        $nopr       (                             );
                        $xdiv2_s    ( W3, W4                      );
                        $xdiv2_s    ( W6, W9                      );
                        $nop        (                             );
                        $xdiv2_sd   ( W2, W12                     );
                        $xdiv2_sd   ( W8, W10                     );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $xdiv2_u    ( W3, W4                      );
                        $xdiv2_u    ( W6, W9                      );
                        $nop        (                             );
                        $xdiv2_ud   ( W2, W12                     );
                        $xdiv2_ud   ( W8, W10                     );
                        $nop        (                             );
                        $xdivf2     ( W8, W9                      );
                        //*/

                        /*
                        $nopr       (                             );
                        $nopr       (                             );
                        $nopr       (                             );
                        $movpag     ( lit(1023), DSRPAG           );
                        $movpag     ( lit( 511), DSWPAG           );
                        $movpag     ( lit( 255), TBLPAG           );
                        $movpag     ( W9       , DSRPAG           );
                        $movpag     ( W9       , DSWPAG           );
                        $movpag     ( W9       , TBLPAG           );
                        $nopr       (                             );
                        $nopr       (                             );
                        $nopr       (                             );
                        $sftac      ( A, lit(-16)                 );
                        $sftac      ( B, lit( 16)                 );
                        $sftac      ( A, W7                       );
                        $sftac      ( B, W8                       );
                        $nopr       (                             );
                        $nopr       (                             );
                        $nopr       (                             );
                        $xdivf      ( W8, W9                      );
                        $nopr       (                             );
                        $cpbeq      ( W7, W9, "1f"                );
                      //$cpbeq      ( W7, W9, "+2"                );
                        $cpseq      ( W7, W9                      );
                    label("1");
                        $nopr       (                             );
                        $nopr       (                             );
                        $cpbeq      ( W7, W9, "1b"                );
                        $nopr       (                             );
                        $nopr       (                             );
                        $cpbeq      ( W7, W9, "1b"                );
                        //*/
                        /*
                        00000000 <__reset>:
                           0:   14 00 04        goto      0x800014 <__SPLIM_init+0x7ff724>
                           2:   80 00 00
                           4:   19 b8 e7        cpbeq.w   w7, w9, 0x8 <L11>
                           6:   19 b8 e7        cpbeq.w   w7, w9, 0xa

                        00000008 <L1^B1>:
                           8:   00 00 ff        nopr
                           a:   00 00 ff        nopr
                           c:   d9 bb e7        cpbeq.w   w7, w9, 0x8 <L11>
                           e:   00 00 ff        nopr
                          10:   00 00 ff        nopr
                          12:   a9 bb e7        cpbeq.w   w7, w9, 0x8 <L11>

                        00000014 <__main>:
                          14:   00 00 ff        nopr
                          16:   00 00 ff        nopr
                          18:   00 00 ff        nopr
                        */
                        /*
                        $nopr       (                             );
                        $nopr       (                             );
                        $_word      ( "1b"                        );
                        $_word      ( 0x002211                    );
                        $nopr       (                             );
                        $nopr       (                             );
                        $_pword     ( "1b"                        );
                        $_pword     ( 0x332211                    );
                        //*/

                        /*
                        $do         ( lit(16383), 4               );
                        $do         ( lit(32767), 4               );

                        $cpseq      ( W7, W9                      );
                        $cpsne      ( W7, W9                      );
                        $cpslt      ( W7, W9                      );
                        $cpsgt      ( W7, W9                      );

                        $xdivf      ( W8, W9                      );

                        $add        ( A                           );
                        $sub        ( B                           );
                        $add        ( mm_ind  (W7), lit(-8), A    );
                        $add        (          W7 ,          B    );

                        $lac        (          W3    , lit(-8), A );
                        $lac        (   ind   (W3   ), lit( 7), A );

                        $bra        ( OV , "3f"                   );
                        $bra        ( NOV, "3f"                   );
                        $bra        ( C  , "3f"                   );
                        $bra        ( NC , "3f"                   );
                        $bra        ( Z  , "3f"                   );
                        $bra        ( NZ , "3f"                   );
                        $bra        ( N  , "3f"                   );
                        $bra        ( NN , "3f"                   );
                        $bra        ( LT , "3f"                   );
                        $bra        ( LE , "3f"                   );
                        $bra        ( LEU, "3f"                   );
                        $bra        ( GT , "3f"                   );
                        $bra        ( GE , "3f"                   );
                        $bra        ( GTU, "3f"                   );
                        $bra        ( ANY, "3f"                   );
                        $bra        ( OA , "3f"                   );
                        $bra        ( OB , "3f"                   );
                        $bra        ( SA , "3f"                   );
                        $bra        ( SB , "3f"                   );
                    label("3");
                        //*/

                        /*
                        $goto       ( "3f"                        );
                        $call       ( "3f"                        );
                        $bra        ( W1                          );
                        $nopr       (                             );
                        $bra        ( 0                           );
                        $bra        ( 32766                       );
                        $nopr       (                             );
                        $bra        ( "3f"                        );
                        $bra        ( "3f"                        );
                        $nop        (                             );
                        $_word      ( "3f"                        );
                    label("3");
                        $nopr       (                             );
                        $_word      ( "3b"                        );
                        $nopr       (                             );
                        $bra        ( "3b"                        );
                        $bra        ( "3b"                        );
                        $goto       ( 0x800088                    );
                        $goto       ( "3b"                        );
                        $call       ( 0x800088                    );
                        $call       ( "3b"                        );
                        $repeat     ( W7                          );
                        $nop        (                             );
                        $repeat     ( lit(16383)                  );
                        $nop        (                             );
                        $do         ( lit(5), 4                   );
                        $do         ( W1    , 4                   );
                        $nopr       (                             );
                        $nopr       (                             );
                        $do         ( lit(5), 65534               );
                        $do         ( W1    , 65534               );
                        $nopr       (                             );
                        $nopr       (                             );
                        $do         ( lit(9), "3b"                );
                        $do         ( W1    , "3b"                );
                        $nopr       (                             );
                        $nopr       (                             );
                        $do         ( lit(9), "3f"                );
                        $do         ( W1    , "3f"                );
                        $nopr       (                             );
                        $nopr       (                             );
                        $rcall      ( "3f"                        );
                    label("3");
                        $nop        (                             );
                        $nop        (                             );
                        $rcall      ( "3b"                        );
                        $rcall      ( 65534                       );
                        $retlw      ( lit(0xFF), W1               );
                        $retlw_b    ( lit(0xFF), W1               );
                        $return     (                             );
                        $retfie     (                             );
                        //*/

                        /*
                        $nopr       (                             );
                        $bra        ( OV , "3f"                   );
                        $bra        ( NOV, "3f"                   );
                        $bra        ( C  , "3f"                   );
                        $bra        ( NC , "3f"                   );
                        $bra        ( Z  , "3f"                   );
                        $bra        ( NZ , "3f"                   );
                        $bra        ( N  , "3f"                   );
                        $bra        ( NN , "3f"                   );
                        $bra        ( LT , "3f"                   );
                        $bra        ( LE , "3f"                   );
                        $bra        ( LEU, "3f"                   );
                        $bra        ( GT , "3f"                   );
                        $bra        ( GE , "3f"                   );
                        $bra        ( GTU, "3f"                   );
                        $bra        ( ANY, "3f"                   );
                        $bra        ( OA , "3f"                   );
                        $bra        ( OB , "3f"                   );
                        $bra        ( SA , "3f"                   );
                        $bra        ( SB , "3f"                   );
                      label("3");
                        $nopr       (                             );
                        /*/

                        /*
                        $mov        ( 8190                        );
                        $mov        ( 8190, WREG                  );
                        $mov_b      ( 8191                        );
                        $mov_b      ( 8191, WREG                  );
                        $mov        ( WREG, 8190                  );
                        $mov_b      ( WREG, 8191                  );
                        $mov        (     0, W7                   );
                        $mov        ( 32768, W7                   );
                        $mov        ( 65534, W7                   );
                        $mov        ( W7,     0                   );
                        $mov        ( W7, 32768                   );
                        $mov        ( W7, 65534                   );
                        $mov        ( lit(    0), W9              );
                        $mov_b      ( lit(    0), W9              );
                        $mov        ( lit(  255), W9              );
                        $mov_b      ( lit(  255), W9              );
                        $mov        ( lit(  256), W9              );
                        $mov        ( lit(32768), W9              );
                        $mov        ( lit(65535), W9              );
                        //*/

                        /*
                        $mov        ( W7           , W9            );
                        $mov_b      ( W7           , W9            );
                        $nop        (                              );
                        $mov        ( ind(W7      ), W9            );
                        $mov        ( ind(W7, 0   ), W9            );
                        $mov        ( ind(W7, 2   ), W9            );
                        $mov        ( ind(W7, 1022), W9            );
                        $mov_b      (     W7       , ind(W9     )  );
                        $mov_b      (     W7       , ind(W9, 0  )  );
                        $mov_b      (     W7       , ind(W9,-2  )  );
                        $mov_b      (     W7       , ind(W9,-512)  );
                        $nop        (                                    );
                        $mov        (    ind   (W7   ),    ind   (W9   ) );
                        $mov        (    ind_pp(W7   ),    ind_pp(W9   ) );
                        $mov        (    ind_mm(W7   ),    ind_mm(W9   ) );
                        $mov        ( pp_ind   (W7   ), pp_ind   (W9   ) );
                        $mov        ( mm_ind   (W7   ), mm_ind   (W9   ) );
                        $mov        (    ind   (W7,W1),    ind   (W9,W1) );
                        $nop        (                                    );
                        $mov_b      (    ind   (W7   ),    ind   (W9   ) );
                        $mov_b      (    ind_pp(W7   ),    ind_pp(W9   ) );
                        $mov_b      (    ind_mm(W7   ),    ind_mm(W9   ) );
                        $mov_b      ( pp_ind   (W7   ), pp_ind   (W9   ) );
                        $mov_b      ( mm_ind   (W7   ), mm_ind   (W9   ) );
                        $mov_b      (    ind   (W7,W1),    ind   (W9,W1) );
                        //*/

                        /*
                        $add        ( 8190                        );
                        $add_b      ( 8191                        );
                        $add        ( 8190, WREG                  );
                        $add_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $addc       ( 8190                        );
                        $addc_b     ( 8191                        );
                        $addc       ( 8190, WREG                  );
                        $addc_b     ( 8191, WREG                  );
                        $nop        (                             );
                        $sub        ( 8190                        );
                        $sub_b      ( 8191                        );
                        $sub        ( 8190, WREG                  );
                        $sub_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $subb       ( 8190                        );
                        $subb_b     ( 8191                        );
                        $subb       ( 8190, WREG                  );
                        $subb_b     ( 8191, WREG                  );
                        $nop        (                             );
                        $subr       ( 8190                        );
                        $subr_b     ( 8191                        );
                        $subr       ( 8190, WREG                  );
                        $subr_b     ( 8191, WREG                  );
                        $nop        (                             );
                        $subbr      ( 8190                        );
                        $subbr_b    ( 8191                        );
                        $subbr      ( 8190, WREG                  );
                        $subbr_b    ( 8191, WREG                  );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $and        ( 8190                        );
                        $and_b      ( 8191                        );
                        $and        ( 8190, WREG                  );
                        $and_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $ior        ( 8190                        );
                        $ior_b      ( 8191                        );
                        $ior        ( 8190, WREG                  );
                        $ior_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $xor        ( 8190                        );
                        $xor_b      ( 8191                        );
                        $xor        ( 8190, WREG                  );
                        $xor_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $sl         ( 8190                        );
                        $sl_b       ( 8191                        );
                        $sl         ( 8190, WREG                  );
                        $sl_b       ( 8191, WREG                  );
                        $nop        (                             );
                        $lsr        ( 8190                        );
                        $lsr_b      ( 8191                        );
                        $lsr        ( 8190, WREG                  );
                        $lsr_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $asr        ( 8190                        );
                        $asr_b      ( 8191                        );
                        $asr        ( 8190, WREG                  );
                        $asr_b      ( 8191, WREG                  );
                        //*/

                        /*
                        $rlnc       ( 8190                        );
                        $rlnc_b     ( 8191                        );
                        $rlnc       ( 8190, WREG                  );
                        $rlnc_b     ( 8191, WREG                  );
                        $nop        (                             );
                        $rlc        ( 8190                        );
                        $rlc_b      ( 8191                        );
                        $rlc        ( 8190, WREG                  );
                        $rlc_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $rrnc       ( 8190                        );
                        $rrnc_b     ( 8191                        );
                        $rrnc       ( 8190, WREG                  );
                        $rrnc_b     ( 8191, WREG                  );
                        $nop        (                             );
                        $rrc        ( 8190                        );
                        $rrc_b      ( 8191                        );
                        $rrc        ( 8190, WREG                  );
                        $rrc_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $inc        ( 8190                        );
                        $inc_b      ( 8191                        );
                        $inc        ( 8190, WREG                  );
                        $inc_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $inc2       ( 8190                        );
                        $inc2_b     ( 8191                        );
                        $inc2       ( 8190, WREG                  );
                        $inc2_b     ( 8191, WREG                  );
                        $nop        (                             );
                        $dec        ( 8190                        );
                        $dec_b      ( 8191                        );
                        $dec        ( 8190, WREG                  );
                        $dec_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $dec2       ( 8190                        );
                        $dec2_b     ( 8191                        );
                        $dec2       ( 8190, WREG                  );
                        $dec2_b     ( 8191, WREG                  );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $neg        ( 8190                        );
                        $neg_b      ( 8191                        );
                        $neg        ( 8190, WREG                  );
                        $neg_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $com        ( 8190                        );
                        $com_b      ( 8191                        );
                        $com        ( 8190, WREG                  );
                        $com_b      ( 8191, WREG                  );
                        $nop        (                             );
                        $clr        ( 8190                        );
                        $clr_b      ( 8191                        );
                        $clr        ( WREG                        );
                        $clr_b      ( WREG                        );
                        $nop        (                             );
                        $setm       ( 8190                        );
                        $setm_b     ( 8191                        );
                        $setm       ( WREG                        );
                        $setm_b     ( WREG                        );
                        //*/

                        /*
                        $add        ( lit(1023), W5               );
                        $add_b      ( lit(255 ), W5               );
                        $addc       ( lit(1023), W5               );
                        $addc_b     ( lit(255 ), W5               );
                        $sub        ( lit(1023), W5               );
                        $sub_b      ( lit(255 ), W5               );
                        $subb       ( lit(1023), W5               );
                        $subb_b     ( lit(255 ), W5               );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $and        ( lit(1023), W5               );
                        $and_b      ( lit(255 ), W5               );
                        $ior        ( lit(1023), W5               );
                        $ior_b      ( lit(255 ), W5               );
                        $xor        ( lit(1023), W5               );
                        $xor_b      ( lit(255 ), W5               );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $add_b      ( W1, lit(31),           W5   );
                        $add        ( W1, lit(31),           W5   );
                        $add        ( W1, lit(31),    ind   (W5)  );
                        $add        ( W1, lit(31),    ind_pp(W5)  );
                        $add        ( W1, lit(31),    ind_mm(W5)  );
                        $add        ( W1, lit(31), pp_ind   (W5)  );
                        $add        ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $addc       ( W1, lit(31),           W5   );
                        $addc_b     ( W1, lit(31),           W5   );
                        $addc_b     ( W1, lit(31),    ind   (W5)  );
                        $addc_b     ( W1, lit(31),    ind_pp(W5)  );
                        $addc_b     ( W1, lit(31),    ind_mm(W5)  );
                        $addc_b     ( W1, lit(31), pp_ind   (W5)  );
                        $addc_b     ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $sub_b      ( W1, lit(31),           W5   );
                        $sub        ( W1, lit(31),           W5   );
                        $sub        ( W1, lit(31),    ind   (W5)  );
                        $sub        ( W1, lit(31),    ind_pp(W5)  );
                        $sub        ( W1, lit(31),    ind_mm(W5)  );
                        $sub        ( W1, lit(31), pp_ind   (W5)  );
                        $sub        ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $subb       ( W1, lit(31),           W5   );
                        $subb_b     ( W1, lit(31),           W5   );
                        $subb_b     ( W1, lit(31),    ind   (W5)  );
                        $subb_b     ( W1, lit(31),    ind_pp(W5)  );
                        $subb_b     ( W1, lit(31),    ind_mm(W5)  );
                        $subb_b     ( W1, lit(31), pp_ind   (W5)  );
                        $subb_b     ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $subr       ( W1, lit(31),           W5   );
                        $subr_b     ( W1, lit(31),           W5   );
                        $subr_b     ( W1, lit(31),    ind   (W5)  );
                        $subr_b     ( W1, lit(31),    ind_pp(W5)  );
                        $subr_b     ( W1, lit(31),    ind_mm(W5)  );
                        $subr_b     ( W1, lit(31), pp_ind   (W5)  );
                        $subr_b     ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $subbr      ( W1, lit(31),           W5   );
                        $subbr_b    ( W1, lit(31),           W5   );
                        $subbr_b    ( W1, lit(31),    ind   (W5)  );
                        $subbr_b    ( W1, lit(31),    ind_pp(W5)  );
                        $subbr_b    ( W1, lit(31),    ind_mm(W5)  );
                        $subbr_b    ( W1, lit(31), pp_ind   (W5)  );
                        $subbr_b    ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $and_b      ( W1, lit(31),           W5   );
                        $and        ( W1, lit(31),           W5   );
                        $and        ( W1, lit(31),    ind   (W5)  );
                        $and        ( W1, lit(31),    ind_pp(W5)  );
                        $and        ( W1, lit(31),    ind_mm(W5)  );
                        $and        ( W1, lit(31), pp_ind   (W5)  );
                        $and        ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $ior_b      ( W1, lit(31),           W5   );
                        $ior        ( W1, lit(31),           W5   );
                        $ior        ( W1, lit(31),    ind   (W5)  );
                        $ior        ( W1, lit(31),    ind_pp(W5)  );
                        $ior        ( W1, lit(31),    ind_mm(W5)  );
                        $ior        ( W1, lit(31), pp_ind   (W5)  );
                        $ior        ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $xor_b      ( W1, lit(31),           W5   );
                        $xor        ( W1, lit(31),           W5   );
                        $xor        ( W1, lit(31),    ind   (W5)  );
                        $xor        ( W1, lit(31),    ind_pp(W5)  );
                        $xor        ( W1, lit(31),    ind_mm(W5)  );
                        $xor        ( W1, lit(31), pp_ind   (W5)  );
                        $xor        ( W1, lit(31), mm_ind   (W5)  );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        //*/

                        /*
                        $add_b      ( W1,           W2 ,           W3  );
                        $add        ( W1,           W2 ,           W3  );
                        $add        ( W1,    ind   (W2),    ind   (W3) );
                        $add        ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $add        ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $add        ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $add        ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $addc       ( W1,           W2 ,           W3  );
                        $addc_b     ( W1,           W2 ,           W3  );
                        $addc_b     ( W1,    ind   (W2),    ind   (W3) );
                        $addc_b     ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $addc_b     ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $addc_b     ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $addc_b     ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $sub_b      ( W1,           W2 ,           W3  );
                        $sub        ( W1,           W2 ,           W3  );
                        $sub        ( W1,    ind   (W2),    ind   (W3) );
                        $sub        ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $sub        ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $sub        ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $sub        ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $subb_b     ( W1,           W2 ,           W3  );
                        $subb       ( W1,           W2 ,           W3  );
                        $subb       ( W1,    ind   (W2),    ind   (W3) );
                        $subb       ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $subb       ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $subb       ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $subb       ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $subr_b     ( W1,           W2 ,           W3  );
                        $subr       ( W1,           W2 ,           W3  );
                        $subr       ( W1,    ind   (W2),    ind   (W3) );
                        $subr       ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $subr       ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $subr       ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $subr       ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $subbr_b    ( W1,           W2 ,           W3  );
                        $subbr      ( W1,           W2 ,           W3  );
                        $subbr      ( W1,    ind   (W2),    ind   (W3) );
                        $subbr      ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $subbr      ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $subbr      ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $subbr      ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $nop        (                                  );
                        $nop        (                                  );
                        $and_b      ( W1,           W2 ,           W3  );
                        $and        ( W1,           W2 ,           W3  );
                        $and        ( W1,    ind   (W2),    ind   (W3) );
                        $and        ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $and        ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $and        ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $and        ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $ior_b      ( W1,           W2 ,           W3  );
                        $ior        ( W1,           W2 ,           W3  );
                        $ior        ( W1,    ind   (W2),    ind   (W3) );
                        $ior        ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $ior        ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $ior        ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $ior        ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $xor_b      ( W1,           W2 ,           W3  );
                        $xor        ( W1,           W2 ,           W3  );
                        $xor        ( W1,    ind   (W2),    ind   (W3) );
                        $xor        ( W1,    ind_pp(W2),    ind_mm(W3) );
                        $xor        ( W1,    ind_mm(W2),    ind_pp(W3) );
                        $xor        ( W1, mm_ind   (W2), pp_ind   (W3) );
                        $xor        ( W1, pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $nop        (                                  );
                        $nop        (                                  );
                        $sl_b       (               W2 ,           W3  );
                        $sl         (               W2 ,           W3  );
                        $sl         (        ind   (W2),    ind   (W3) );
                        $sl         (        ind_pp(W2),    ind_mm(W3) );
                        $sl         (        ind_mm(W2),    ind_pp(W3) );
                        $sl         (     mm_ind   (W2), pp_ind   (W3) );
                        $sl         (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $lsr_b      (               W2 ,           W3  );
                        $lsr        (               W2 ,           W3  );
                        $lsr        (        ind   (W2),    ind   (W3) );
                        $lsr        (        ind_pp(W2),    ind_mm(W3) );
                        $lsr        (        ind_mm(W2),    ind_pp(W3) );
                        $lsr        (     mm_ind   (W2), pp_ind   (W3) );
                        $lsr        (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $asr_b      (               W2 ,           W3  );
                        $asr        (               W2 ,           W3  );
                        $asr        (        ind   (W2),    ind   (W3) );
                        $asr        (        ind_pp(W2),    ind_mm(W3) );
                        $asr        (        ind_mm(W2),    ind_pp(W3) );
                        $asr        (     mm_ind   (W2), pp_ind   (W3) );
                        $asr        (     pp_ind   (W2), mm_ind   (W3) );
                        //*/

                        /*
                        $rlnc_b     (               W2 ,           W3  );
                        $rlnc       (               W2 ,           W3  );
                        $rlnc       (        ind   (W2),    ind   (W3) );
                        $rlnc       (        ind_pp(W2),    ind_mm(W3) );
                        $rlnc       (        ind_mm(W2),    ind_pp(W3) );
                        $rlnc       (     mm_ind   (W2), pp_ind   (W3) );
                        $rlnc       (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $rlc_b      (               W2 ,           W3  );
                        $rlc        (               W2 ,           W3  );
                        $rlc        (        ind   (W2),    ind   (W3) );
                        $rlc        (        ind_pp(W2),    ind_mm(W3) );
                        $rlc        (        ind_mm(W2),    ind_pp(W3) );
                        $rlc        (     mm_ind   (W2), pp_ind   (W3) );
                        $rlc        (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $rrnc_b     (               W2 ,           W3  );
                        $rrnc       (               W2 ,           W3  );
                        $rrnc       (        ind   (W2),    ind   (W3) );
                        $rrnc       (        ind_pp(W2),    ind_mm(W3) );
                        $rrnc       (        ind_mm(W2),    ind_pp(W3) );
                        $rrnc       (     mm_ind   (W2), pp_ind   (W3) );
                        $rrnc       (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $rrc_b      (               W2 ,           W3  );
                        $rrc        (               W2 ,           W3  );
                        $rrc        (        ind   (W2),    ind   (W3) );
                        $rrc        (        ind_pp(W2),    ind_mm(W3) );
                        $rrc        (        ind_mm(W2),    ind_pp(W3) );
                        $rrc        (     mm_ind   (W2), pp_ind   (W3) );
                        $rrc        (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $nop        (                                  );
                        $nop        (                                  );
                        $inc_b      (               W2 ,           W3  );
                        $inc        (               W2 ,           W3  );
                        $inc        (        ind   (W2),    ind   (W3) );
                        $inc        (        ind_pp(W2),    ind_mm(W3) );
                        $inc        (        ind_mm(W2),    ind_pp(W3) );
                        $inc        (     mm_ind   (W2), pp_ind   (W3) );
                        $inc        (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $inc2_b     (               W2 ,           W3  );
                        $inc2       (               W2 ,           W3  );
                        $inc2       (        ind   (W2),    ind   (W3) );
                        $inc2       (        ind_pp(W2),    ind_mm(W3) );
                        $inc2       (        ind_mm(W2),    ind_pp(W3) );
                        $inc2       (     mm_ind   (W2), pp_ind   (W3) );
                        $inc2       (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $dec_b      (               W2 ,           W3  );
                        $dec        (               W2 ,           W3  );
                        $dec        (        ind   (W2),    ind   (W3) );
                        $dec        (        ind_pp(W2),    ind_mm(W3) );
                        $dec        (        ind_mm(W2),    ind_pp(W3) );
                        $dec        (     mm_ind   (W2), pp_ind   (W3) );
                        $dec        (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $dec2_b     (               W2 ,           W3  );
                        $dec2       (               W2 ,           W3  );
                        $dec2       (        ind   (W2),    ind   (W3) );
                        $dec2       (        ind_pp(W2),    ind_mm(W3) );
                        $dec2       (        ind_mm(W2),    ind_pp(W3) );
                        $dec2       (     mm_ind   (W2), pp_ind   (W3) );
                        $dec2       (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $nop        (                                  );
                        $nop        (                                  );
                        $neg_b      (               W2 ,           W3  );
                        $neg        (               W2 ,           W3  );
                        $neg        (        ind   (W2),    ind   (W3) );
                        $neg        (        ind_pp(W2),    ind_mm(W3) );
                        $neg        (        ind_mm(W2),    ind_pp(W3) );
                        $neg        (     mm_ind   (W2), pp_ind   (W3) );
                        $neg        (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $com_b      (               W2 ,           W3  );
                        $com        (               W2 ,           W3  );
                        $com        (        ind   (W2),    ind   (W3) );
                        $com        (        ind_pp(W2),    ind_mm(W3) );
                        $com        (        ind_mm(W2),    ind_pp(W3) );
                        $com        (     mm_ind   (W2), pp_ind   (W3) );
                        $com        (     pp_ind   (W2), mm_ind   (W3) );
                        $nop        (                                  );
                        $clr_b      (               W2                 );
                        $clr        (               W2                 );
                        $clr        (        ind   (W2)                );
                        $clr        (        ind_pp(W2)                );
                        $clr        (        ind_mm(W2)                );
                        $clr        (     mm_ind   (W2)                );
                        $clr        (     pp_ind   (W2)                );
                        $nop        (                                  );
                        $setm_b     (               W2                 );
                        $setm       (               W2                 );
                        $setm       (        ind   (W2)                );
                        $setm       (        ind_pp(W2)                );
                        $setm       (        ind_mm(W2)                );
                        $setm       (     mm_ind   (W2)                );
                        $setm       (     pp_ind   (W2)                );
                        $nop        (                                  );
                        $nop        (                                  );
                        $nop        (                                  );
                        $ze         (               W2 , W3            );
                        $ze         (        ind   (W2), W3            );
                        $ze         (        ind_pp(W2), W3            );
                        $ze         (        ind_mm(W2), W3            );
                        $ze         (     mm_ind   (W2), W3            );
                        $ze         (     pp_ind   (W2), W3            );
                        $nop        (                                  );
                        $se         (               W2 , W3            );
                        $se         (        ind   (W2), W3            );
                        $se         (        ind_pp(W2), W3            );
                        $se         (        ind_mm(W2), W3            );
                        $se         (     mm_ind   (W2), W3            );
                        $se         (     pp_ind   (W2), W3            );
                        //*/

                        /*
                        $clrwdt     (                             );
                        $reset      (                             );
                        $lnk        ( lit(16382)                  );
                        $ulnk       (                             );
                        $disi       ( lit(16383)                  );
                        $exch       ( W7, W8                      );
                        $swap       ( W9                          );
                        $swap_b     ( W9                          );
                        $daw_b      ( W9                          );
                        $pwrsav     ( lit(1)                      );
                        $pwrsav     ( lit(0)                      );
                        $add        ( A                           );
                        $add        ( B                           );
                        $sub        ( A                           );
                        $sub        ( B                           );
                        $neg        ( A                           );
                        $neg        ( B                           );
                        $nop        (                             );
                        $lac        (          W3    , lit(-8), A );
                        $lac        (   ind   (W3   ), lit( 7), A );
                        $lac        (   ind_pp(W3   ), lit( 5), A );
                        $lac        (   ind_mm(W3   ), lit( 3), A );
                        $lac        ( pp_ind  (W3   ), lit(-3), A );
                        $lac        ( mm_ind  (W3   ), lit(-5), A );
                        $lac        (   ind   (W3,W7), lit( 0), A );
                        $lac        (   ind   (W3,W7), lit( 0), B );
                        $lac        (   ind   (W3,W7),          B );
                        $nop        (                             );
                        $sac        ( A, lit(-8),          W3     );
                        $sac        ( A, lit( 7),   ind   (W3   ) );
                        $sac        ( A, lit( 5),   ind_pp(W3   ) );
                        $sac        ( A, lit( 3),   ind_mm(W3   ) );
                        $sac        ( A, lit(-3), pp_ind  (W3   ) );
                        $sac        ( A, lit(-5), mm_ind  (W3   ) );
                        $sac        ( A, lit( 0),   ind   (W3,W7) );
                        $sac        ( B, lit( 0),   ind   (W3,W7) );
                        $sac        ( B,            ind   (W3,W7) );
                        $nop        (                             );
                        $sac_r      ( A, lit(-8),          W3     );
                        $sac_r      ( A, lit( 7),   ind   (W3   ) );
                        $sac_r      ( A, lit( 5),   ind_pp(W3   ) );
                        $sac_r      ( A, lit( 3),   ind_mm(W3   ) );
                        $sac_r      ( A, lit(-3), pp_ind  (W3   ) );
                        $sac_r      ( A, lit(-5), mm_ind  (W3   ) );
                        $sac_r      ( A, lit( 0),   ind   (W3,W7) );
                        $sac_r      ( B, lit( 0),   ind   (W3,W7) );
                        $sac_r      ( B,            ind   (W3,W7) );
                        //*/

                        /*
                        $bset       ( 8190        , lit( 0)       );
                        $bset       ( 8190        , lit( 7)       );
                        $bset       ( 8190        , lit( 8)       );
                        $bset       ( 8190        , lit(15)       );
                        $bset_b     ( 8191        , lit( 0)       );
                        $bset_b     ( 8191        , lit( 7)       );
                        $bset       (          W7 , lit( 0)       );
                        $bset       (   ind   (W7), lit( 3)       );
                        $bset       (   ind_pp(W7), lit( 7)       );
                        $bset       (   ind_mm(W7), lit( 8)       );
                        $bset       ( pp_ind  (W7), lit(11)       );
                        $bset       ( mm_ind  (W7), lit(15)       );
                        $bset_b     (          W7 , lit( 0)       );
                        $bset_b     (   ind   (W7), lit( 3)       );
                        $bset_b     (   ind_pp(W7), lit( 7)       );
                        $bset_b     (   ind_mm(W7), lit( 0)       );
                        $bset_b     ( pp_ind  (W7), lit( 3)       );
                        $bset_b     ( mm_ind  (W7), lit( 7)       );
                        $nop        (                             );
                        $bclr       ( 8190        , lit( 0)       );
                        $bclr       ( 8190        , lit( 7)       );
                        $bclr       ( 8190        , lit( 8)       );
                        $bclr       ( 8190        , lit(15)       );
                        $bclr_b     ( 8191        , lit( 0)       );
                        $bclr_b     ( 8191        , lit( 7)       );
                        $bclr       (          W7 , lit( 0)       );
                        $bclr       (   ind   (W7), lit( 3)       );
                        $bclr       (   ind_pp(W7), lit( 7)       );
                        $bclr       (   ind_mm(W7), lit( 8)       );
                        $bclr       ( pp_ind  (W7), lit(11)       );
                        $bclr       ( mm_ind  (W7), lit(15)       );
                        $bclr_b     (          W7 , lit( 0)       );
                        $bclr_b     (   ind   (W7), lit( 3)       );
                        $bclr_b     (   ind_pp(W7), lit( 7)       );
                        $bclr_b     (   ind_mm(W7), lit( 0)       );
                        $bclr_b     ( pp_ind  (W7), lit( 3)       );
                        $bclr_b     ( mm_ind  (W7), lit( 7)       );
                        $nop        (                             );
                        $btg        ( 8190        , lit( 0)       );
                        $btg        ( 8190        , lit( 7)       );
                        $btg        ( 8190        , lit( 8)       );
                        $btg        ( 8190        , lit(15)       );
                        $btg_b      ( 8191        , lit( 0)       );
                        $btg_b      ( 8191        , lit( 7)       );
                        $btg        (          W7 , lit( 0)       );
                        $btg        (   ind   (W7), lit( 3)       );
                        $btg        (   ind_pp(W7), lit( 7)       );
                        $btg        (   ind_mm(W7), lit( 8)       );
                        $btg        ( pp_ind  (W7), lit(11)       );
                        $btg        ( mm_ind  (W7), lit(15)       );
                        $btg_b      (          W7 , lit( 0)       );
                        $btg_b      (   ind   (W7), lit( 3)       );
                        $btg_b      (   ind_pp(W7), lit( 7)       );
                        $btg_b      (   ind_mm(W7), lit( 0)       );
                        $btg_b      ( pp_ind  (W7), lit( 3)       );
                        $btg_b      ( mm_ind  (W7), lit( 7)       );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $btst       ( 8190        , lit( 0)       );
                        $btst       ( 8190        , lit( 7)       );
                        $btst       ( 8190        , lit( 8)       );
                        $btst       ( 8190        , lit(15)       );
                        $btst_b     ( 8191        , lit( 0)       );
                        $btst_b     ( 8191        , lit( 7)       );
                        $btst_c     (          W7 , lit( 0)       );
                        $btst_c     (   ind   (W7), lit( 3)       );
                        $btst_c     (   ind_pp(W7), lit( 7)       );
                        $btst_c     (   ind_mm(W7), lit( 8)       );
                        $btst_c     ( pp_ind  (W7), lit(11)       );
                        $btst_c     ( mm_ind  (W7), lit(15)       );
                        $btst_z     (          W7 , lit( 0)       );
                        $btst_z     (   ind   (W7), lit( 3)       );
                        $btst_z     (   ind_pp(W7), lit( 7)       );
                        $btst_z     (   ind_mm(W7), lit( 8)       );
                        $btst_z     ( pp_ind  (W7), lit(11)       );
                        $btst_z     ( mm_ind  (W7), lit(15)       );
                        $nop        (                             );
                        $btst_c     (          W7 , W8            );
                        $btst_c     (   ind   (W7), W8            );
                        $btst_c     (   ind_pp(W7), W8            );
                        $btst_c     (   ind_mm(W7), W8            );
                        $btst_c     ( pp_ind  (W7), W8            );
                        $btst_c     ( mm_ind  (W7), W8            );
                        $btst_z     (          W7 , W8            );
                        $btst_z     (   ind   (W7), W8            );
                        $btst_z     (   ind_pp(W7), W8            );
                        $btst_z     (   ind_mm(W7), W8            );
                        $btst_z     ( pp_ind  (W7), W8            );
                        $btst_z     ( mm_ind  (W7), W8            );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $bsw_c      (          W7 , W8            );
                        $bsw_c      (   ind   (W7), W8            );
                        $bsw_c      (   ind_pp(W7), W8            );
                        $bsw_c      (   ind_mm(W7), W8            );
                        $bsw_c      ( pp_ind  (W7), W8            );
                        $bsw_c      ( mm_ind  (W7), W8            );
                        $bsw_z      (          W7 , W8            );
                        $bsw_z      (   ind   (W7), W8            );
                        $bsw_z      (   ind_pp(W7), W8            );
                        $bsw_z      (   ind_mm(W7), W8            );
                        $bsw_z      ( pp_ind  (W7), W8            );
                        $bsw_z      ( mm_ind  (W7), W8            );
                        //*/

                        /*
                        $btss       ( 8190        , lit( 0)       );
                        $btss       ( 8190        , lit( 7)       );
                        $btss       ( 8190        , lit( 8)       );
                        $btss       ( 8190        , lit(15)       );
                        $btss_b     ( 8191        , lit( 0)       );
                        $btss_b     ( 8191        , lit( 7)       );
                        $btss       (          W7 , lit( 0)       );
                        $btss       (   ind   (W7), lit( 3)       );
                        $btss       (   ind_pp(W7), lit( 7)       );
                        $btss       (   ind_mm(W7), lit( 8)       );
                        $btss       ( pp_ind  (W7), lit(11)       );
                        $btss       ( mm_ind  (W7), lit(15)       );
                        $nop        (                             );
                        $btsc       ( 8190        , lit( 0)       );
                        $btsc       ( 8190        , lit( 7)       );
                        $btsc       ( 8190        , lit( 8)       );
                        $btsc       ( 8190        , lit(15)       );
                        $btsc_b     ( 8191        , lit( 0)       );
                        $btsc_b     ( 8191        , lit( 7)       );
                        $btsc       (          W7 , lit( 0)       );
                        $btsc       (   ind   (W7), lit( 3)       );
                        $btsc       (   ind_pp(W7), lit( 7)       );
                        $btsc       (   ind_mm(W7), lit( 8)       );
                        $btsc       ( pp_ind  (W7), lit(11)       );
                        $btsc       ( mm_ind  (W7), lit(15)       );
                        $nop        (                             );
                        $nop        (                             );
                        $nop        (                             );
                        $btsts      ( 8190        , lit( 0)       );
                        $btsts      ( 8190        , lit( 7)       );
                        $btsts      ( 8190        , lit( 8)       );
                        $btsts      ( 8190        , lit(15)       );
                        $btsts_b    ( 8191        , lit( 0)       );
                        $btsts_b    ( 8191        , lit( 7)       );
                        $btsts_c    (          W7 , lit( 0)       );
                        $btsts_c    (   ind   (W7), lit( 3)       );
                        $btsts_c    (   ind_pp(W7), lit( 7)       );
                        $btsts_c    (   ind_mm(W7), lit( 8)       );
                        $btsts_c    ( pp_ind  (W7), lit(11)       );
                        $btsts_c    ( mm_ind  (W7), lit(15)       );
                        $btsts_z    (          W7 , lit( 0)       );
                        $btsts_z    (   ind   (W7), lit( 3)       );
                        $btsts_z    (   ind_pp(W7), lit( 7)       );
                        $btsts_z    (   ind_mm(W7), lit( 8)       );
                        $btsts_z    ( pp_ind  (W7), lit(11)       );
                        $btsts_z    ( mm_ind  (W7), lit(15)       );
                        //*/

                        /*
                        $mul        ( 8190                        );
                        $mul_b      ( 8191                        );
                        $nop        (                             );
                        $mul_ss     ( W6,          W7 , W8        );
                        $mul_ss     ( W6,   ind   (W7), W8        );
                        $mul_ss     ( W6,   ind_pp(W7), W8        );
                        $mul_ss     ( W6,   ind_mm(W7), W8        );
                        $mul_ss     ( W6, pp_ind  (W7), W8        );
                        $mul_ss     ( W6, mm_ind  (W7), W8        );
                        $nop        (                             );
                        $mul_su     ( W6,   lit   (31), W8        );
                        $mul_su     ( W6,          W7 , W8        );
                        $mul_su     ( W6,   ind   (W7), W8        );
                        $mul_su     ( W6,   ind_pp(W7), W8        );
                        $mul_su     ( W6,   ind_mm(W7), W8        );
                        $mul_su     ( W6, pp_ind  (W7), W8        );
                        $mul_su     ( W6, mm_ind  (W7), W8        );
                        $nop        (                             );
                        $mul_us     ( W6,          W7 , W8        );
                        $mul_us     ( W6,   ind   (W7), W8        );
                        $mul_us     ( W6,   ind_pp(W7), W8        );
                        $mul_us     ( W6,   ind_mm(W7), W8        );
                        $mul_us     ( W6, pp_ind  (W7), W8        );
                        $mul_us     ( W6, mm_ind  (W7), W8        );
                        $nop        (                             );
                        $mul_uu     ( W6,   lit   (31), W8        );
                        $mul_uu     ( W6,          W7 , W8        );
                        $mul_uu     ( W6,   ind   (W7), W8        );
                        $mul_uu     ( W6,   ind_pp(W7), W8        );
                        $mul_uu     ( W6,   ind_mm(W7), W8        );
                        $mul_uu     ( W6, pp_ind  (W7), W8        );
                        $mul_uu     ( W6, mm_ind  (W7), W8        );
                        $nop        (                             );
                        $mulw_ss    ( W6,          W7 , W8        );
                        $mulw_ss    ( W6,   ind   (W7), W8        );
                        $mulw_ss    ( W6,   ind_pp(W7), W8        );
                        $mulw_ss    ( W6,   ind_mm(W7), W8        );
                        $mulw_ss    ( W6, pp_ind  (W7), W8        );
                        $mulw_ss    ( W6, mm_ind  (W7), W8        );
                        $nop        (                             );
                        $mulw_su    ( W6,   lit   (31), W8        );
                        $mulw_su    ( W6,          W7 , W8        );
                        $mulw_su    ( W6,   ind   (W7), W8        );
                        $mulw_su    ( W6,   ind_pp(W7), W8        );
                        $mulw_su    ( W6,   ind_mm(W7), W8        );
                        $mulw_su    ( W6, pp_ind  (W7), W8        );
                        $mulw_su    ( W6, mm_ind  (W7), W8        );
                        $nop        (                             );
                        $mulw_us    ( W6,          W7 , W8        );
                        $mulw_us    ( W6,   ind   (W7), W8        );
                        $mulw_us    ( W6,   ind_pp(W7), W8        );
                        $mulw_us    ( W6,   ind_mm(W7), W8        );
                        $mulw_us    ( W6, pp_ind  (W7), W8        );
                        $mulw_us    ( W6, mm_ind  (W7), W8        );
                        $nop        (                             );
                        $mulw_uu    ( W6,   lit   (31), W8        );
                        $mulw_uu    ( W6,          W7 , W8        );
                        $mulw_uu    ( W6,   ind   (W7), W8        );
                        $mulw_uu    ( W6,   ind_pp(W7), W8        );
                        $mulw_uu    ( W6,   ind_mm(W7), W8        );
                        $mulw_uu    ( W6, pp_ind  (W7), W8        );
                        $mulw_uu    ( W6, mm_ind  (W7), W8        );
                        //*/

                        /*
                        $tblrdl     (    ind   (W7),           W8  );
                        $tblrdl_b   (    ind   (W7),           W8  );
                        $tblrdl     (    ind   (W7),    ind   (W8) );
                        $tblrdl_b   (    ind   (W7),    ind   (W8) );
                        $tblrdl     (    ind_pp(W7), mm_ind   (W8) );
                        $tblrdl     (    ind_mm(W7), pp_ind   (W8) );
                        $tblrdl     ( pp_ind   (W7),    ind_mm(W8) );
                        $tblrdl     ( mm_ind   (W7),    ind_pp(W8) );
                        $nop        (                              );
                        $tblrdh     (    ind   (W7),           W8  );
                        $tblrdh_b   (    ind   (W7),           W8  );
                        $tblrdh     (    ind   (W7),    ind   (W8) );
                        $tblrdh_b   (    ind   (W7),    ind   (W8) );
                        $tblrdh     (    ind_pp(W7), mm_ind   (W8) );
                        $tblrdh     (    ind_mm(W7), pp_ind   (W8) );
                        $tblrdh     ( pp_ind   (W7),    ind_mm(W8) );
                        $tblrdh     ( mm_ind   (W7),    ind_pp(W8) );
                        $nop        (                              );
                        $tblwtl     (           W7 ,    ind   (W8) );
                        $tblwtl_b   (           W7 ,    ind   (W8) );
                        $tblwtl     (    ind   (W7),    ind   (W8) );
                        $tblwtl_b   (    ind   (W7),    ind   (W8) );
                        $tblwtl     (    ind_pp(W7), mm_ind   (W8) );
                        $tblwtl     (    ind_mm(W7), pp_ind   (W8) );
                        $tblwtl     ( pp_ind   (W7),    ind_mm(W8) );
                        $tblwtl     ( mm_ind   (W7),    ind_pp(W8) );
                        $nop        (                              );
                        $tblwth     (           W7 ,    ind   (W8) );
                        $tblwth_b   (           W7 ,    ind   (W8) );
                        $tblwth     (    ind   (W7),    ind   (W8) );
                        $tblwth_b   (    ind   (W7),    ind   (W8) );
                        $tblwth     (    ind_pp(W7), mm_ind   (W8) );
                        $tblwth     (    ind_mm(W7), pp_ind   (W8) );
                        $tblwth     ( pp_ind   (W7),    ind_mm(W8) );
                        $tblwth     ( mm_ind   (W7),    ind_pp(W8) );
                        //*/

                        /*
                        $mov_d      (           W2 ,           W8  );
                        $mov_d      (           W8 ,           W2  );
                        $nop        (                              );
                        $mov_d      (           W2 ,    ind   (W7) );
                        $mov_d      (           W2 ,    ind_pp(W7) );
                        $mov_d      (           W2 ,    ind_mm(W7) );
                        $mov_d      (           W2 , pp_ind   (W7) );
                        $mov_d      (           W2 , mm_ind   (W7) );
                        $nop        (                              );
                        $mov_d      (    ind   (W7),           W2  );
                        $mov_d      (    ind_pp(W7),           W2  );
                        $mov_d      (    ind_mm(W7),           W2  );
                        $mov_d      ( pp_ind   (W7),           W2  );
                        $mov_d      ( mm_ind   (W7),           W2  );
                        $nop        (                              );
                        $nop        (                              );
                        $nop        (                              );
                        $push       ( 65534                        );
                        $pop        ( 65534                        );
                        $nop        (                              );
                        $push       (           W3                 );
                        $push       (    ind   (W3   )             );
                        $push       (    ind_pp(W3   )             );
                        $push       (    ind_mm(W3   )             );
                        $push       ( pp_ind   (W3   )             );
                        $push       ( mm_ind   (W3   )             );
                        $push       (    ind   (W3,W7)             );
                        $pop        (           W3                 );
                        $pop        (    ind   (W3   )             );
                        $pop        (    ind_pp(W3   )             );
                        $pop        (    ind_mm(W3   )             );
                        $pop        ( pp_ind   (W3   )             );
                        $pop        ( mm_ind   (W3   )             );
                        $pop        (    ind   (W3,W7)             );
                        $nop        (                              );
                        $repeat     ( lit(10)                      );
                        $push_d     ( W2                           );
                        $repeat     ( lit(10)                      );
                        $pop_d      ( W2                           );
                        $nop        (                              );
                        $push_s     (                              );
                        $pop_s      (                              );
                        $nop        (                              );
                        $nop        (                              );
                        $nop        (                              );
                        $sl         ( W1, lit(15), W2              );
                        $lsr        ( W1, lit(15), W2              );
                        $asr        ( W1, lit(15), W2              );
                        $sl         ( W1, W7     , W2              );
                        $lsr        ( W1, W7     , W2              );
                        $asr        ( W1, W7     , W2              );
                        //*/

                        /*
                        $xdiv_s     ( W3, W4                       );
                        $xdiv_s     ( W6, W9                       );
                        $nop        (                              );
                        $xdiv_sd    ( W2, W12                      );
                        $xdiv_sd    ( W8, W10                      );
                        $nop        (                              );
                        $nop        (                              );
                        $nop        (                              );
                        $xdiv_u     ( W3, W4                       );
                        $xdiv_u     ( W6, W9                       );
                        $nop        (                              );
                        $xdiv_ud    ( W2, W12                      );
                        $xdiv_ud    ( W8, W10                      );
                        $nop        (                              );
                        $nop        (                              );
                        $nop        (                              );
                        $xdivf      ( W8, W9                       );
                        $nop        (                              );
                        $nop        (                              );
                        $nop        (                              );
                        $fbcl       (           W3 , W5            );
                        $fbcl       (    ind   (W3), W5            );
                        $fbcl       (    ind_pp(W3), W5            );
                        $fbcl       (    ind_mm(W3), W5            );
                        $fbcl       ( pp_ind   (W3), W5            );
                        $fbcl       ( mm_ind   (W3), W5            );
                        $nop        (                              );
                        $ff1l       (           W3 , W5            );
                        $ff1l       (    ind   (W3), W5            );
                        $ff1l       (    ind_pp(W3), W5            );
                        $ff1l       (    ind_mm(W3), W5            );
                        $ff1l       ( pp_ind   (W3), W5            );
                        $ff1l       ( mm_ind   (W3), W5            );
                        $nop        (                              );
                        $ff1r       (           W3 , W5            );
                        $ff1r       (    ind   (W3), W5            );
                        $ff1r       (    ind_pp(W3), W5            );
                        $ff1r       (    ind_mm(W3), W5            );
                        $ff1r       ( pp_ind   (W3), W5            );
                        $ff1r       ( mm_ind   (W3), W5            );
                        $nop        (                              );
                        $nop        (                              );
                        $nop        (                              );
                        $cp         ( 8190                         );
                        $cp_b       ( 8191                         );
                        $cp0        ( 8190                         );
                        $cp0_b      ( 8191                         );
                        $cpb        ( 8190                         );
                        $cpb_b      ( 8191                         );
                        $nop        (                              );
                        $cp         ( W5, lit(15)                  );
                        $cp_b       ( W5, lit(15)                  );
                        $cpb        ( W5, lit(15)                  );
                        $cpb_b      ( W5, lit(15)                  );
                        $nop        (                              );
                        $cp         ( W2,          W7              );
                        $cp_b       ( W2,          W7              );
                        $cp         ( W2,   ind   (W7)             );
                        $cp         ( W2,   ind_pp(W7)             );
                        $cp         ( W2,   ind_mm(W7)             );
                        $cp         ( W2, pp_ind  (W7)             );
                        $cp         ( W2, mm_ind  (W7)             );
                        $nop        (                              );
                        $cp0        (              W7              );
                        $cp0_b      (              W7              );
                        $cp0        (       ind   (W7)             );
                        $cp0        (       ind_pp(W7)             );
                        $cp0        (       ind_mm(W7)             );
                        $cp0        (     pp_ind  (W7)             );
                        $cp0        (     mm_ind  (W7)             );
                        $nop        (                              );
                        $cpb        ( W2,          W7              );
                        $cpb_b      ( W2,          W7              );
                        $cpb        ( W2,   ind   (W7)             );
                        $cpb        ( W2,   ind_pp(W7)             );
                        $cpb        ( W2,   ind_mm(W7)             );
                        $cpb        ( W2, pp_ind  (W7)             );
                        $cpb        ( W2, mm_ind  (W7)             );
                        $nop        (                              );
                        $cpseq      ( W7, W9                       );
                        $cpsne      ( W7, W9                       );
                        $cpslt      ( W7, W9                       );
                        $cpsgt      ( W7, W9                       );
                        $cpseq_b    ( W7, W9                       );
                        $cpsne_b    ( W7, W9                       );
                        $cpslt_b    ( W7, W9                       );
                        $cpsgt_b    ( W7, W9                       );
                        //*/

                        /*
                        $add        (          W7 , lit(-8), A    );
                        $add        (   ind   (W7), lit(-8), A    );
                        $add        (   ind_pp(W7), lit(-8), A    );
                        $add        (   ind_mm(W7), lit(-8), A    );
                        $add        ( pp_ind  (W7), lit(-8), A    );
                        $add        ( mm_ind  (W7), lit(-8), A    );
                        $nop        (                             );
                        $add        (          W7 ,          B    );
                        $add        (   ind   (W7),          B    );
                        $add        (   ind_pp(W7),          B    );
                        $add        (   ind_mm(W7),          B    );
                        $add        ( pp_ind  (W7),          B    );
                        $add        ( mm_ind  (W7),          B    );
                        //*/

                    label_Start();
                        $nopr       (                             );
                        $nopr       (                             );
                        $nopr       (                             );
                }};

                PIC16BitEPC.dumpArray24Bits( PIC16BitEPC.toArray24Bits( asm.link(0x800000, true) ) );

                SysUtil.stdDbg().println();
                if(xal) SysUtil.systemExit();

            } // if

            // Test 'ARMCortexMThumb' - all instructions
            ARMCortexMThumb.setARMObjDumpBinary( SysUtil.getUHD() + "/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/bin/arm-none-eabi-objdump" );

            /*
            ( new ARMCortexMThumbC(CPU.M0plus) {{
                    $adr ( Reg.R7, "boot2_exit"               );
                    $movs( Reg.R6, 1                          );
                    $orrs( Reg.R7, Reg.R6                     );
                    $mov ( Reg.LR, Reg.R7                     );
                    $ldri( Reg.R7, ( 0x20000000L + 1024 ) | 1 );
                    $bx  ( Reg.R7                             );
                    $bkpt( 0                                  );
                    $bkpt( 0                                  );
                label    ( "boot2_exit"                       );
                    $bkpt( 0                                  );
                    $bkpt( 0                                  );
                    $bkpt( 0                                  );
            }} ).link(ProgSWD.DefaultCortexM_SRAMStart, true);
            //*/

            if(!true) {

                final boolean         xal = !true;
                final ARMCortexMThumb asm = new ARMCortexMThumbC(CPU.M33) {{

                        $_word  (0x20001000L                           ); // SP
                        $_word  (Label_Start                           ); // PC
                        $_word  (Label_NMI_Handler                     ); // [NMI_Handler]
                        $_word  (Label_HardFault_Handler               ); // [HardFault_Handler]

                    function_NMI_Handler();
                        $b_dot  (                                      );

                    function_HardFault_Handler();
                        $b_dot  (                                      );

                    function_Start();

                        /*
                        $itete    (Cond.EQ                             );
                        $adceq    (Reg.R0, Reg.R1                      );
                        $addne    (Reg.R0, Reg.R1, 1                   );
                        $addeq    (Reg.R0, 132                         );
                        $addne    (Reg.R0, Reg.R1, Reg.R2              );
                        $itete    (Cond.EQ                             );
                        $addeq    (Reg.R0, Reg.R1                      );
                        $addne    (Reg.SP, Reg.SP, 132                 );
                        $addeq_sp (                256                 );
                        $addwne   (Reg.R8, Reg.R9, 0x3FF               );
                        $itete    (Cond.EQ                             );
                        $adreq    (Reg.R0, "test_it1"                  );
                        $andne    (Reg.R0, Reg.R1                      );
                    label("test_it1");
                        $asreq    (Reg.R0, Reg.R1, 30                  );
                        $asrsne   (Reg.R0, Reg.R1, 30                  );
                        $itete    (Cond.EQ                             );
                        $asreq    (Reg.R0, Reg.R1, Reg.R2              );
                        $asrsne   (Reg.R0, Reg.R1, Reg.R2              );
                        $asreq    (Reg.R1,         Reg.R2              );
                        $asrsne   (Reg.R1,         Reg.R2              );
                        $itete    (Cond.EQ                             );
                        $nop      (                                    );
                        $nop      (                                    );
                        $nop      (                                    );
                        $bne      ("test_it2"                          );
                        $bne      ("test_it2"                          );
                    label("test_it2");
                        $itete    (Cond.EQ                             );
                        $nop      (                                    );
                        $nop      (                                    );
                        $nop      (                                    );
                        $bne_w    ("test_it3"                          );
                        $bne_w    ("test_it3"                          );
                    label("test_it3");
                        $itete    (Cond.EQ                             );
                        $biceq    (Reg.R1, Reg.R2                      );
                        $bkpt     (0                                   );
                        $nop      (                                    );
                        $blne     ("test_it2"                          );
                        $itete    (Cond.EQ                             );
                        $clrexeq  (                                    );
                        $clrexne  (                                    );
                        $nop      (                                    );
                        $blxne    (Reg.R0                              );
                        $itete    (Cond.EQ                             );
                        $cmneq    (Reg.R0, -2147483648                 );
                        $cmnne    (Reg.R0, Reg.R1                      );
                        $nop      (                                    );
                        $bxne     (Reg.R0                              );
                        $itete    (Cond.EQ                             );
                        $cmpeq    (Reg.R0, 3                           );
                        $cmpne    (Reg.R0, -2147483648                 );
                        $cmpeq    (Reg.R0, Reg.R1                      );
                        $cpyne    (Reg.R0, Reg.R1                      );
                        $itete    (Cond.EQ                             );
                        $dec1eq   (Reg.R0                              );
                        $dec1wne  (Reg.R0                              );
                        $dec16eq  (Reg.R0                              );
                        $dec16wne (Reg.R0                              );
                        $itete    (Cond.EQ                             );
                        $dmbeq_sy (                                    );
                        $dsbne_sy (                                    );
                        $isbeq_sy (                                    );
                        $eorne    (Reg.R0, Reg.R1                      );
                        $itete    (Cond.EQ                             );
                        $ldmiaeq  (Reg.R0   , Reg.R0, Reg.R1, Reg.R2   );
                        $ldmiane  (Reg.R0.wb,         Reg.R1, Reg.R2   );
                        $stmiaeq  (Reg.R0.wb,         Reg.R1           );
                        $stmiane  (Reg.R0.wb, Reg.R0, Reg.R1           );
                        $itete    (Cond.EQ                             );
                        $ldreq_pst(Reg.R0, Reg.R2, 4 *  1              );
                        $strne_pst(Reg.R0, Reg.R2, 4 * -1              );
                        $ldreq    (Reg.R0, Reg.R1, 4 *  3              );
                        $ldrne    (Reg.R0, Reg.R1, 4 * -3              );
                        $itete    (Cond.EQ                             );
                        $nop      (                                    );
                        $ldrne    (Reg.R0, Reg.R1                      );
                        $ldreq    (Reg.R0, "test_it4"                  );
                        $ldrne_w  (Reg.R0, "test_it4"                  );
                        $itete    (Cond.EQ                             );
                        $ldreq    (Reg.R0, Reg.R1, Reg.R2              );
                        $ldrne    (Reg.R0, 2147483647L                 );
                        $ldrieq   (Reg.R0, 2147483647L                 );
                    label("test_it4");
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $ldrbeq   (Reg.R0, Reg.R1                      );
                        $ldrbne   (Reg.R0, Reg.R1, 1                   );
                        $ldrbeq   (Reg.R0, Reg.R1, Reg.R2              );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $ldrheq   (Reg.R0, Reg.R1                      );
                        $ldrhne   (Reg.R0, Reg.R1, 2                   );
                        $ldrheq   (Reg.R0, Reg.R1, Reg.R2              );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $strbeq   (Reg.R0, Reg.R1                      );
                        $strbne   (Reg.R0, Reg.R1, 1                   );
                        $strbeq   (Reg.R0, Reg.R1, Reg.R2              );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $strheq   (Reg.R0, Reg.R1                      );
                        $strhne   (Reg.R0, Reg.R1, 2                   );
                        $strheq   (Reg.R0, Reg.R1, Reg.R2              );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $streq    (Reg.R0, Reg.R1, 4 *  1              );
                        $strne    (Reg.R0, Reg.R1, 4 * -1              );
                        $streq    (Reg.R0, Reg.R1                      );
                        $strne    (Reg.R0, Reg.R1, Reg.R2              );
                        $itete    (Cond.EQ                             );
                        $ldrsbeq  (Reg.R0, Reg.R1, Reg.R2              );
                        $ldrshne  (Reg.R0, Reg.R1, Reg.R2              );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $ldaeq    (Reg.R0, Reg.R9                      );
                        $ldabne   (Reg.R0, Reg.R9                      );
                        $ldaheq   (Reg.R0, Reg.R9                      );
                        $ldaexne  (Reg.R0, Reg.R9                      );
                        $itete    (Cond.EQ                             );
                        $ldaexbeq (Reg.R0, Reg.R9                      );
                        $ldaexhne (Reg.R0, Reg.R9                      );
                        $ldrexeq  (Reg.R0, Reg.R9                      );
                        $ldrexne  (Reg.R0, Reg.R9, 4 * 1               );
                        $itete    (Cond.EQ                             );
                        $ldrexbeq (Reg.R0, Reg.R9                      );
                        $ldrexhne (Reg.R0, Reg.R9                      );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $stleq    (Reg.R0,         Reg.R9              );
                        $stlbne   (Reg.R0,         Reg.R9              );
                        $stlheq   (Reg.R0,         Reg.R9              );
                        $stlexne  (Reg.R0, Reg.R1, Reg.R9              );
                        $itete    (Cond.EQ                             );
                        $stlexbeq (Reg.R0, Reg.R1, Reg.R9              );
                        $stlexhne (Reg.R0, Reg.R1, Reg.R9              );
                        $strexeq  (Reg.R0, Reg.R1, Reg.R9              );
                        $strexne  (Reg.R0, Reg.R1, Reg.R9, 4 * 1       );
                        $itete    (Cond.EQ                             );
                        $strexbeq (Reg.R0, Reg.R1, Reg.R9              );
                        $strexhne (Reg.R0, Reg.R1, Reg.R9              );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $movteq   (Reg.R0, 32767                       );
                        $mrsne    (Reg.R0, SYSm.IEPSR                  );
                        $msreq    (SYSm.IEPSR  , Reg.R0                );
                        $mulne    (Reg.R0, Reg.R1                      );
                        $itete    (Cond.EQ                             );
                        $negeq    (Reg.R0, Reg.R1                      );
                        $orrne    (Reg.R0, Reg.R1                      );
                        $popeq    (Reg.R0, Reg.R7, Reg.R8, Reg.LR      );
                        $pushne   (Reg.R0, Reg.R7, Reg.R8, Reg.LR      );
                        $pop      (Reg.R0, Reg.R7, Reg.R8, Reg.LR      );
                        $push     (Reg.R0, Reg.R7, Reg.R8, Reg.LR      );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $reveq    (Reg.R0, Reg.R1                      );
                        $rev16ne  (Reg.R0, Reg.R1                      );
                        $revsheq  (Reg.R0, Reg.R1                      );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $rsbeq    (Reg.R0, Reg.R1, 0                   );
                        $rsb0ne   (Reg.R0, Reg.R1                      );
                        $sbceq    (Reg.R0, Reg.R1                      );
                        $sdivne   (Reg.R0, Reg.R1, Reg.R8              );
                        $itete    (Cond.EQ                             );
                        $seveq    (                                    );
                        $wfene    (                                    );
                        $wfieq    (                                    );
                        $yieldne  (                                    );
                        $itete    (Cond.EQ                             );
                        $svceq    (111                                 );
                        $sxtbne   (Reg.R0, Reg.R1                      );
                        $sxtheq   (Reg.R0, Reg.R1                      );
                        $udfne    (222                                 );
                        $itete    (Cond.EQ                             );
                        $udiveq   (Reg.R0, Reg.R1, Reg.R8              );
                        $uxtbne   (Reg.R0, Reg.R1                      );
                        $uxtheq   (Reg.R0, Reg.R1                      );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $rrxeq    (Reg.R0, Reg.R8                      );
                        $rrxsne   (Reg.R0, Reg.R8                      );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $tsteq    (Reg.R0, -2147483648                 );
                        $tstne    (Reg.R0, Reg.R1                      );
                        $tsteq    (Reg.R0, Reg.R1, Shift.RRX           );
                        $tstne    (Reg.R0, Reg.R1, Shift.LSL, 1        );
                        $itete    (Cond.EQ                             );
                        $subeq    (Reg.R0, Reg.R1,   7                 );
                        $subne    (Reg.SP, Reg.SP, 256                 );
                        $subeq    (Reg.R0, 240                         );
                        $subne    (Reg.R0, Reg.R1, Reg.R2              );
                        $itete    (Cond.EQ                             );
                        $subeq_sp (256                                 );
                        $subwne   (Reg.R0, Reg.R1, 0x3FF               );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $roreq    (Reg.R0, Reg.R1, 30                  );
                        $rorsne   (Reg.R0, Reg.R1, 30                  );
                        $roreq    (Reg.R0, Reg.R1, Reg.R2              );
                        $rorsne   (Reg.R0, Reg.R1, Reg.R2              );
                        $itete    (Cond.EQ                             );
                        $roreq    (Reg.R0,         Reg.R2              );
                        $rorsne   (Reg.R0,         Reg.R2              );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $lsleq    (Reg.R0, Reg.R1, 30                  );
                        $lslsne   (Reg.R0, Reg.R1, 30                  );
                        $lsleq    (Reg.R0, Reg.R1, Reg.R2              );
                        $lslsne   (Reg.R0, Reg.R1, Reg.R2              );
                        $itete    (Cond.EQ                             );
                        $lsleq    (Reg.R0,         Reg.R2              );
                        $lslsne   (Reg.R0,         Reg.R2              );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $lsreq    (Reg.R0, Reg.R1, 30                  );
                        $lsrsne   (Reg.R0, Reg.R1, 30                  );
                        $lsreq    (Reg.R0, Reg.R1, Reg.R2              );
                        $lsrsne   (Reg.R0, Reg.R1, Reg.R2              );
                        $itete    (Cond.EQ                             );
                        $lsreq    (Reg.R0,         Reg.R2              );
                        $lsrsne   (Reg.R0,         Reg.R2              );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $mvneq    (Reg.R0, -2147483648                 );
                        $mvnsne   (Reg.R0, -2147483648                 );
                        $mvneq    (Reg.R0, Reg.R1                      );
                        $mvnsne   (Reg.R0, Reg.R1                      );
                        $itete    (Cond.EQ                             );
                        $mvneq    (Reg.R0, Reg.R1, Shift.LSL, 3        );
                        $mvnsne   (Reg.R0, Reg.R1, Shift.LSL, 3        );
                        $mvneq    (Reg.R0, Reg.R1, Shift.RRX           );
                        $mvnsne   (Reg.R0, Reg.R1, Shift.RRX           );
                        $itete    (Cond.EQ                             );
                        $moveq    (Reg.R0, 2147483647                  );
                        $movsne   (Reg.R0, 2147483647                  );
                        $nop      (                                    );
                        $nop      (                                    );
                        $itete    (Cond.EQ                             );
                        $moveq    (Reg.R0, Reg.R1                      );
                        $movne    (Reg.R0, Reg.R9                      );
                        $movseq   (Reg.R0, Reg.R1                      );
                        $movsne   (Reg.R0, Reg.R9                      );
                        $itete    (Cond.EQ                             );
                        $moveq    (Reg.R0, Reg.R1, Shift.ASR, 30       );
                        $movsne   (Reg.R0, Reg.R1, Shift.ASR, 30       );
                        $moveq    (Reg.R0, Reg.R1, Shift.RRX           );
                        $movsne   (Reg.R0, Reg.R1, Shift.RRX           );
                        $itete    (Cond.EQ                             );
                        $moveq    (Reg.R0, Reg.R1, Shift.ASR, Reg.R2   );
                        $movsne   (Reg.R0, Reg.R1, Shift.ASR, Reg.R2   );
                        $moveq    (Reg.R0,         Shift.ASR, Reg.R2   );
                        $movsne   (Reg.R0,         Shift.ASR, Reg.R2   );
                        $itete    (Cond.EQ                             );
                        $movweq   (Reg.R0, 0                           );
                        $movwne   (Reg.R0, 32767                       );
                        $movweq   (Reg.R9, 0                           );
                        $movwne   (Reg.R9, 65535                       );
                        //*/

                        /*
                        $addw   (Reg.R8, Reg.R9, 0x001                 );
                        $addw   (Reg.R8, Reg.R9, 0x3FF                 );
                        $addw   (Reg.R8, Reg.R9, 0xFFF                 );
                        $nop    (                                      );
                        $subw   (Reg.R8, Reg.R9, 0x001                 );
                        $subw   (Reg.R8, Reg.R9, 0x3FF                 );
                        $subw   (Reg.R8, Reg.R9, 0xFFF                 );
                        $nop    (                                      );
                        $tst    (Reg.R8, Reg.R9, Shift.LSL, 1          );
                        $tst    (Reg.R8, Reg.R9, Shift.LSR, 1          );
                        $tst    (Reg.R8, Reg.R9, Shift.ASR, 1          );
                        $tst    (Reg.R8, Reg.R9, Shift.ROR, 1          );
                        $tst    (Reg.R8, Reg.R9, Shift.RRX             );
                        $tst    (Reg.R8, Reg.R9                        );
                        //*/

                        /*
                        $clrex  (                                      );
                        $lda    (Reg.R0, Reg.R9                        );
                        $ldab   (Reg.R0, Reg.R9                        );
                        $ldah   (Reg.R0, Reg.R9                        );
                        $ldaex  (Reg.R0, Reg.R9                        );
                        $ldaexb (Reg.R0, Reg.R9                        );
                        $ldaexh (Reg.R0, Reg.R9                        );
                        $clrex  (                                      );
                        $stl    (Reg.R0,         Reg.R9                );
                        $stlb   (Reg.R0,         Reg.R9                );
                        $stlh   (Reg.R0,         Reg.R9                );
                        $stlex  (Reg.R0, Reg.R1, Reg.R9                );
                        $stlexb (Reg.R0, Reg.R1, Reg.R9                );
                        $stlexh (Reg.R0, Reg.R1, Reg.R9                );
                        $clrex  (                                      );
                        $ldrex  (Reg.R0, Reg.R9                        );
                        $ldrex  (Reg.R0, Reg.R9, 4 * 1                 );
                        $ldrexb (Reg.R0, Reg.R9                        );
                        $ldrexh (Reg.R0, Reg.R9                        );
                        $clrex  (                                      );
                        $strex  (Reg.R0, Reg.R1, Reg.R9                );
                        $strex  (Reg.R0, Reg.R1, Reg.R9, 4 * 1         );
                        $strexb (Reg.R0, Reg.R1, Reg.R9                );
                        $strexh (Reg.R0, Reg.R1, Reg.R9                );
                        $clrex  (                                      );
                        //*/

                        /*
                        $nop    (                                      );
                        $pop    (Reg.R0, Reg.R7, Reg.R8, Reg.R9, Reg.LR);
                        $pop    (Reg.R0, Reg.R7, Reg.R8, Reg.R9, Reg.PC);
                        $pop    (Reg.LR                                );
                        $pop    (Reg.PC                                );
                        $pop    (Reg.R7                                );
                        $pop    (Reg.R8                                );
                        $nop    (                                      );
                        $push   (Reg.R0, Reg.R7, Reg.R8, Reg.R9, Reg.LR);
                        $push   (Reg.LR                                );
                        $push   (Reg.R7                                );
                        $push   (Reg.R8                                );
                        $nop    (                                      );
                        $nop    (                                      );
                        $nop    (                                      );
                        $mvns   (Reg.R0, Reg.R1, Shift.LSL,  0         );
                        $mvns   (Reg.R0, Reg.R1, Shift.LSL,  1         );
                        $mvns   (Reg.R0, Reg.R1, Shift.LSL, 31         );
                        $mvns   (Reg.R0, Reg.R1, Shift.LSR,  0         );
                        $mvns   (Reg.R0, Reg.R1, Shift.LSR,  1         );
                        $mvns   (Reg.R0, Reg.R1, Shift.LSR, 31         );
                        $mvns   (Reg.R0, Reg.R1, Shift.LSR, 32         );
                        $mvns   (Reg.R0, Reg.R1, Shift.ASR,  0         );
                        $mvns   (Reg.R0, Reg.R1, Shift.ASR,  1         );
                        $mvns   (Reg.R0, Reg.R1, Shift.ASR, 31         );
                        $mvns   (Reg.R0, Reg.R1, Shift.ASR, 32         );
                        $mvns   (Reg.R0, Reg.R1, Shift.ROR,  0         );
                        $mvns   (Reg.R0, Reg.R1, Shift.ROR,  1         );
                        $mvns   (Reg.R0, Reg.R1, Shift.ROR, 31         );
                        $mvns   (Reg.R0, Reg.R1, Shift.RRX             );
                        $nop    (                                      );
                        $mvn    (Reg.R0, Reg.R1, Shift.LSL,  0         );
                        $mvn    (Reg.R0, Reg.R1, Shift.LSL,  1         );
                        $mvn    (Reg.R0, Reg.R1, Shift.LSL, 31         );
                        $mvn    (Reg.R0, Reg.R1, Shift.LSR,  0         );
                        $mvn    (Reg.R0, Reg.R1, Shift.LSR,  1         );
                        $mvn    (Reg.R0, Reg.R1, Shift.LSR, 31         );
                        $mvn    (Reg.R0, Reg.R1, Shift.LSR, 32         );
                        $mvn    (Reg.R0, Reg.R1, Shift.ASR,  0         );
                        $mvn    (Reg.R0, Reg.R1, Shift.ASR,  1         );
                        $mvn    (Reg.R0, Reg.R1, Shift.ASR, 31         );
                        $mvn    (Reg.R0, Reg.R1, Shift.ASR, 32         );
                        $mvn    (Reg.R0, Reg.R1, Shift.ROR,  0         );
                        $mvn    (Reg.R0, Reg.R1, Shift.ROR,  1         );
                        $mvn    (Reg.R0, Reg.R1, Shift.ROR, 31         );
                        $mvn    (Reg.R0, Reg.R1, Shift.RRX             );
                        $nop    (                                      );
                        $nop_w  (                                      );
                        $nop    (                                      );
                        //*/

                        /*
                        $cmn    (Reg.R0, -2147483648                   );
                        $cmp    (Reg.R0, -2147483648                   );
                    label("testw0b");
                        $ldr_w  (Reg.R0, "testw0b"                     );
                        $ldr_w  (Reg.R0, "testw0a"                     );
                    label("testw0a");
                        $nop    (                                      );
                    label("testw1b");
                        $ldr_w  (Reg.R0, "testw1b"                     );
                        $nop    (                                      );
                        $ldr_w  (Reg.R0, "testw1a"                     );
                    label("testw1a");
                        $nop    (                                      );
                    label("testw2b");
                        $nop    (                                      );
                        $ldr_w  (Reg.R0, "testw2b"                     );
                        $nop    (                                      );
                        $ldr_w  (Reg.R0, "testw2a"                     );
                    label("testw2a");
                        $nop    (                                      );
                        $sdiv   (Reg.R0, Reg.R1, Reg.R8                );
                        $udiv   (Reg.R0, Reg.R1, Reg.R8                );
                        $tst    (Reg.R0, -2147483648                   );
                        $nop    (                                      );
                    label("testb3b");
                        $bl     ("testb3b"                             );
                        $nop    (                                      );
                        $bl     ("testb3a"                             );
                        $nop    (                                      );
                        $nop    (                                      );
                        $nop    (                                      );
                        $b_w    ("testb3b"                             );
                        $nop    (                                      );
                        $b_w    ("testb3a"                             );
                        $nop    (                                      );
                        $nop    (                                      );
                        $nop    (                                      );
                        $ble    ("testb3b"                             );
                        $ble_w  ("testb3b"                             );
                        $ble    ("testb3a"                             );
                        $ble_w  ("testb3a"                             );
                        $nop    (                                      );
                        $bmi    ("testb3b"                             );
                        $bmi_w  ("testb3b"                             );
                        $bmi    ("testb3a"                             );
                        $bmi_w  ("testb3a"                             );
                    label("testb3a");
                        $nop    (                                      );
                        //*/

                        /*
                        // IT block without explicit condition-specifiers
                        $itete  (Cond.EQ                               );
                        $ands   (Reg.R0, Reg.R1                        ); // andeq    r0, r1
                        $cmp    (Reg.R0, Reg.R1                        ); // cmpne    r0, r1
                        $nop    (                                      ); // nopeq
                        $b      ("testit0"                             ); // bne.n    ...
                        $itttt  (Cond.EQ                               );
                        $movs   (Reg.R0, 1                             ); // moveq    r0, #1          (the <S> is removed because it produces 16-bit opcode)
                        $movs_w (Reg.R0, 1                             ); // movseq.w r0, #1
                        $mov    (Reg.R0, 2147483647                    ); // mvneq.w  r0, #2147483648
                        $movs   (Reg.R0, 2147483647                    ); // mvnseq.w r0, #2147483648
                        $itttt  (Cond.NE                               );
                        $mvn    (Reg.R0, 1                             ); // mvneq.w  r0, #1
                        $mvns   (Reg.R0, 1                             ); // mvnseq.w r0, #1
                        $mvn    (Reg.R0, 2147483647                    ); // moveq.w  r0, #2147483648
                        $mvns   (Reg.R0, 2147483647                    ); // movseq.w r0, #2147483648
                    label("testit0");
                        $nop    (                                      );
                        //*/

                        /*
                        $movt   (Reg.R0, 0                             );
                        $movt   (Reg.R0, 255                           );
                        $movt   (Reg.R0, 32767                         );
                        $movt   (Reg.R0, 65535                         );
                        $nop    (                                      );
                        $movs   (Reg.R0, Reg.R1                        );
                        $mov    (Reg.R0, Reg.R1                        );
                        $mov_w  (Reg.R0, Reg.R1                        );
                        $movs   (Reg.R0, Reg.R9                        );
                        $mov    (Reg.R0, Reg.R9                        );
                      //$movs   (Reg.R0, Reg.PC                        ); //  Error: ... not allowed here
                      //$movs   (Reg.SP, Reg.R1                        ); //  Error: ... not allowed here
                        $mov_w  (Reg.R0, Reg.R9                        );
                        $nop    (                                      );
                        //*/

                        /*
                        $nop    (                                      );
                        $asrs   (Reg.R0, Reg.R1,            30         );
                        $movs   (Reg.R0, Reg.R1, Shift.ASR, 30         );
                        $asr    (Reg.R0, Reg.R1,            30         );
                        $mov    (Reg.R0, Reg.R1, Shift.ASR, 30         );
                        $nop    (                                      );
                        $asrs   (Reg.R0, Reg.R9,            30         );
                        $movs   (Reg.R0, Reg.R9, Shift.ASR, 30         );
                        $asr    (Reg.R0, Reg.R9,            30         );
                        $mov    (Reg.R0, Reg.R9, Shift.ASR, 30         );
                        $nop    (                                      );
                        $asrs   (Reg.R0, Reg.R1                        );
                        $movs   (Reg.R0, Reg.R0, Shift.ASR, Reg.R2     );
                        $movs   (Reg.R0, Reg.R1, Shift.ASR, Reg.R2     );
                        $asr    (Reg.R0, Reg.R1                        );
                        $mov    (Reg.R0, Reg.R0, Shift.ASR, Reg.R2     );
                        $mov    (Reg.R0, Reg.R1, Shift.ASR, Reg.R2     );
                        $nop    (                                      );
                        $asrs   (Reg.R0, Reg.R9                        );
                        $movs   (Reg.R0, Reg.R0, Shift.ASR, Reg.R8     );
                        $movs   (Reg.R0, Reg.R9, Shift.ASR, Reg.R8     );
                        $asr    (Reg.R0, Reg.R9                        );
                        $mov    (Reg.R0, Reg.R0, Shift.ASR, Reg.R8     );
                        $mov    (Reg.R0, Reg.R9, Shift.ASR, Reg.R8     );
                        $nop    (                                      );
                        //*/

                        /*
                        $nop    (                                      );
                        $lsls   (Reg.R0, Reg.R1,            30         );
                        $movs   (Reg.R0, Reg.R1, Shift.LSL, 30         );
                        $lsl    (Reg.R0, Reg.R1,            30         );
                        $mov    (Reg.R0, Reg.R1, Shift.LSL, 30         );
                        $nop    (                                      );
                        $lsls   (Reg.R0, Reg.R9,            30         );
                        $movs   (Reg.R0, Reg.R9, Shift.LSL, 30         );
                        $lsl    (Reg.R0, Reg.R9,            30         );
                        $mov    (Reg.R0, Reg.R9, Shift.LSL, 30         );
                        $nop    (                                      );
                        $lsls   (Reg.R0, Reg.R1                        );
                        $movs   (Reg.R0, Reg.R0, Shift.LSL, Reg.R2     );
                        $movs   (Reg.R0, Reg.R1, Shift.LSL, Reg.R2     );
                        $lsl    (Reg.R0, Reg.R1                        );
                        $mov    (Reg.R0, Reg.R0, Shift.LSL, Reg.R2     );
                        $mov    (Reg.R0, Reg.R1, Shift.LSL, Reg.R2     );
                        $nop    (                                      );
                        $lsls   (Reg.R0, Reg.R9                        );
                        $movs   (Reg.R0, Reg.R0, Shift.LSL, Reg.R8     );
                        $movs   (Reg.R0, Reg.R9, Shift.LSL, Reg.R8     );
                        $lsl    (Reg.R0, Reg.R9                        );
                        $mov    (Reg.R0, Reg.R0, Shift.LSL, Reg.R8     );
                        $mov    (Reg.R0, Reg.R9, Shift.LSL, Reg.R8     );
                        $nop    (                                      );
                        //*/

                        /*
                        $nop    (                                      );
                        $lsrs   (Reg.R0, Reg.R1,            30         );
                        $movs   (Reg.R0, Reg.R1, Shift.LSR, 30         );
                        $lsr    (Reg.R0, Reg.R1,            30         );
                        $mov    (Reg.R0, Reg.R1, Shift.LSR, 30         );
                        $nop    (                                      );
                        $lsrs   (Reg.R0, Reg.R9,            30         );
                        $movs   (Reg.R0, Reg.R9, Shift.LSR, 30         );
                        $lsr    (Reg.R0, Reg.R9,            30         );
                        $mov    (Reg.R0, Reg.R9, Shift.LSR, 30         );
                        $nop    (                                      );
                        $lsrs   (Reg.R0, Reg.R1                        );
                        $movs   (Reg.R0, Reg.R0, Shift.LSR, Reg.R2     );
                        $movs   (Reg.R0, Reg.R1, Shift.LSR, Reg.R2     );
                        $lsr    (Reg.R0, Reg.R1                        );
                        $mov    (Reg.R0, Reg.R0, Shift.LSR, Reg.R2     );
                        $mov    (Reg.R0, Reg.R1, Shift.LSR, Reg.R2     );
                        $nop    (                                      );
                        $lsrs   (Reg.R0, Reg.R9                        );
                        $movs   (Reg.R0, Reg.R0, Shift.LSR, Reg.R8     );
                        $movs   (Reg.R0, Reg.R9, Shift.LSR, Reg.R8     );
                        $lsr    (Reg.R0, Reg.R9                        );
                        $mov    (Reg.R0, Reg.R0, Shift.LSR, Reg.R8     );
                        $mov    (Reg.R0, Reg.R9, Shift.LSR, Reg.R8     );
                        $nop    (                                      );
                        //*/

                        /*
                        $nop    (                                      );
                        $rors   (Reg.R0, Reg.R1,            30         );
                        $movs   (Reg.R0, Reg.R1, Shift.ROR, 30         );
                        $ror    (Reg.R0, Reg.R1,            30         );
                        $mov    (Reg.R0, Reg.R1, Shift.ROR, 30         );
                        $nop    (                                      );
                        $rors   (Reg.R0, Reg.R9,            30         );
                        $movs   (Reg.R0, Reg.R9, Shift.ROR, 30         );
                        $ror    (Reg.R0, Reg.R9,            30         );
                        $mov    (Reg.R0, Reg.R9, Shift.ROR, 30         );
                        $nop    (                                      );
                        $rors   (Reg.R0, Reg.R1                        );
                        $movs   (Reg.R0, Reg.R0, Shift.ROR, Reg.R2     );
                        $movs   (Reg.R0, Reg.R1, Shift.ROR, Reg.R2     );
                        $ror    (Reg.R0, Reg.R1                        );
                        $mov    (Reg.R0, Reg.R0, Shift.ROR, Reg.R2     );
                        $mov    (Reg.R0, Reg.R1, Shift.ROR, Reg.R2     );
                        $nop    (                                      );
                        $rors   (Reg.R0, Reg.R9                        );
                        $movs   (Reg.R0, Reg.R0, Shift.ROR, Reg.R8     );
                        $movs   (Reg.R0, Reg.R9, Shift.ROR, Reg.R8     );
                        $ror    (Reg.R0, Reg.R9                        );
                        $mov    (Reg.R0, Reg.R0, Shift.ROR, Reg.R8     );
                        $mov    (Reg.R0, Reg.R9, Shift.ROR, Reg.R8     );
                        $nop    (                                      );
                        //*/

                        /*
                        $nop    (                                      );
                        $rrxs   (Reg.R0, Reg.R1                        );
                        $movs   (Reg.R0, Reg.R1, Shift.RRX             );
                        $rrx    (Reg.R0, Reg.R1                        );
                        $mov    (Reg.R0, Reg.R1, Shift.RRX             );
                        $nop    (                                      );
                        $rrxs   (Reg.R0, Reg.R9                        );
                        $movs   (Reg.R0, Reg.R9, Shift.RRX             );
                        $rrx    (Reg.R0, Reg.R9                        );
                        $mov    (Reg.R0, Reg.R9, Shift.RRX             );
                        $nop    (                                      );
                        //*/

                        /*
                        $mov    (Reg.R5,  255                          );
                        $movs   (Reg.R5,  255                          );
                        $mov    (Reg.R5, -255                          );
                        $movs   (Reg.R5, -255                          );
                        $nop    (                                      );
                        $mov    (Reg.R5,  2147483647                   );
                        $movs   (Reg.R5,  2147483647                   );
                        $mvn    (Reg.R5, -2147483648                   );
                        $mvns   (Reg.R5, -2147483648                   );
                        $nop    (                                      );
                        $nop    (                                      );
                        $mvn    (Reg.R5,  255                          );
                        $mvns   (Reg.R5,  255                          );
                        $mvn    (Reg.R5, -255                          );
                        $mvns   (Reg.R5, -255                          );
                        $nop    (                                      );
                        $mvn    (Reg.R5,  2147483647                   );
                        $mvns   (Reg.R5,  2147483647                   );
                        $mov    (Reg.R5, -2147483648                   );
                        $movs   (Reg.R5, -2147483648                   );
                        $nop    (                                      );
                        $nop    (                                      );
                      //$movs   (Reg.R5,  1234567890L                  ); // Error: invalid constant ... after fixup
                      //$mvns   (Reg.R5,  1234567890L                  ); // Error: invalid constant ... after fixup
                      //$movs   (Reg.R5, -1234567890L                  ); // Error: invalid constant ... after fixup
                      //$mvns   (Reg.R5, -1234567890L                  ); // Error: invalid constant ... after fixup
                        $nop    (                                      );
                        $movw   (Reg.R5,  0                            );
                        $movw   (Reg.R5,  32767                        );
                        $movw   (Reg.R5,  65535                        );
                        $nop    (                                      );
                        //*/

                        /*
                        $nop    (                                      );
                        $ldr    (Reg.R5,  0xFFFFFFFFL                  );
                        $ldr    (Reg.R5,  2147483647L                  );
                        $ldr    (Reg.R5,  2L                           );
                        $ldr    (Reg.R5,  0x00000000L                  );
                        $ldr    (Reg.R5, -2L                           );
                        $ldr    (Reg.R5, -2147483648L                  );
                        $nop    (                                      );
                        $ldr    (Reg.R5,  1000L                        );
                        $ldr    (Reg.R5, -1000L                        );
                        $ldr    (Reg.R5,  65535L                       );
                        $ldr    (Reg.R5, -65535L                       );
                        $nop    (                                      );
                        $ldr    (Reg.R5,  1234567890L                  );
                        $ldr    (Reg.R5, -1234567890L                  );
                        $nop    (                                      );
                        $nop    (                                      );
                        $nop    (                                      );
                        //*/

                        //*
                        $ldr    (Reg.R0, Reg.R2   , 4 * 31             ); // ldr r0, [r2, 4 * 31]
                        $ldr    (Reg.R0, Reg.R2   , 4 * 32             ); // ldr r0, [r2, 4 * 32]
                        $ldr    (Reg.R0, Reg.R2   , 4 * -1             ); // ldr r0, [r2, 4 * -1]
                        $ldr    (Reg.R0, Reg.R2.wb, 4 *  1             ); // ldr r0, [r2, 4 *  1]!
                        $ldr    (Reg.R0, Reg.R2.wb, 4 * -1             ); // ldr r0, [r2, 4 * -1]!
                        $ldr_pst(Reg.R0, Reg.R2   , 4 *  1             ); // ldr r0, [r2        ], 4 *  1
                        $ldr_pst(Reg.R0, Reg.R2   , 4 * -1             ); // ldr r0, [r2        ], 4 * -1
                        $nop    (                                      );
                        $nop    (                                      );
                        $nop    (                                      );
                        $str    (Reg.R0, Reg.R2   , 4 * 31             ); // str r0, [r2, 4 * 31]
                        $str    (Reg.R0, Reg.R2   , 4 * 32             ); // str r0, [r2, 4 * 32]
                        $str    (Reg.R0, Reg.R2   , 4 * -1             ); // str r0, [r2, 4 * -1]
                        $str    (Reg.R0, Reg.R2.wb, 4 *  1             ); // str r0, [r2, 4 *  1]!
                        $str    (Reg.R0, Reg.R2.wb, 4 * -1             ); // str r0, [r2, 4 * -1]!
                        $str_pst(Reg.R0, Reg.R2   , 4 *  1             ); // str r0, [r2        ], 4 *  1
                        $str_pst(Reg.R0, Reg.R2   , 4 * -1             ); // str r0, [r2        ], 4 * -1
                        //*/

                        /*
                        $cbz    (Reg.R0, "test0"                       );
                        $cbnz   (Reg.R0, "test0"                       );
                        $nop    (                                      );
                    label("test0");
                        $sev    (                                      );
                        $wfe    (                                      );
                        $wfi    (                                      );
                        $_align2(                                      );
                        //*/

                        /*
                        $adcs   (Reg.R0, Reg.R3                        );
                        $adds   (Reg.R0, Reg.R1, 7                     );
                        $adds   (Reg.R0, 240                           );
                        $adds   (Reg.R0, Reg.R1, Reg.R2                );
                        $add    (Reg.R8, Reg.R9                        );
                        $add    (Reg.R0, Reg.SP, 240                   );
                        $add    (Reg.SP, Reg.SP, 100                   );
                        $add    (Reg.R8, Reg.SP                        );
                        $add    (Reg.SP, Reg.R8                        );
                        //*/

                        /*
                        $adr    (Reg.R0, "test1"                       );
                        $add    (Reg.R0, Reg.PC, 200                   );
                        $add    (Reg.R0, Reg.PC, 220                   );
                        $add    (Reg.R0, Reg.PC, 240                   );
                        $add    (Reg.R0, Reg.PC, 120                   );
                        $_align4(                                      );
                    label("test1");
                        $add    (Reg.R0, Reg.PC, 100                   );
                        //*/

                        /*
                        $ands   (Reg.R2, Reg.R3                        );
                        $asrs   (Reg.R2, Reg.R3, 30                    );
                        $asrs   (Reg.R2, Reg.R3                        );
                        $b      ("test2"                               );
                        $beq    ("test2"                               );
                        $bne    ("test2"                               );
                        $bcs    ("test2"                               );
                        $bhs    ("test2"                               );
                        $bcc    ("test2"                               );
                        $blo    ("test2"                               );
                        $bmi    ("test2"                               );
                        $bpl    ("test2"                               );
                        $bvs    ("test2"                               );
                        $bvc    ("test2"                               );
                        $bhi    ("test2"                               );
                        $bls    ("test2"                               );
                        $bge    ("test2"                               );
                        $blt    ("test2"                               );
                        $bgt    ("test2"                               );
                        $blt    ("test2"                               );
                        $nop    (                                      );
                    label("test2");
                        $nop    (                                      );
                        $b      ("test2"                               );
                        $nop    (                                      );
                        //*/

                        /*
                        $bl     ("test3"                               );
                        $nop    (                                      );
                    label("test3");
                        $bics   (Reg.R2, Reg.R3                        );
                        $bkpt   (0                                     );
                        $bkpt   (255                                   );
                        $bl     ("test3"                               );
                        $nop    (                                      );
                        $blx    (Reg.R0                                );
                        $bx     (Reg.R0                                );
                        $cmn    (Reg.R2, Reg.R3                        );
                        $cmp    (Reg.R2, Reg.R3                        );
                        $cmp    (Reg.R2, Reg.R8                        );
                        $cmp    (Reg.R8, Reg.R3                        );
                        $cpsie_i(                                      );
                        $cpsid_i(                                      );
                        $b_dot  (                                      );
                        //*/

                        /*
                        $ldmia  (Reg.R0.wb,         Reg.R2                        );
                        $ldmia  (Reg.R0.wb,         Reg.R2, Reg.R3, Reg.R4, Reg.R5);
                        $stmia  (Reg.R0.wb,         Reg.R2                        );
                        $stmia  (Reg.R0.wb,         Reg.R2, Reg.R3, Reg.R4, Reg.R5);
                        $nop    (                                                 );
                        $ldmia  (Reg.R0   , Reg.R0, Reg.R2                        );
                        $ldmia  (Reg.R0   , Reg.R0, Reg.R2, Reg.R3, Reg.R4, Reg.R5);
                        $stmia  (Reg.R0.wb, Reg.R0, Reg.R2                        );
                        $stmia  (Reg.R0.wb, Reg.R0, Reg.R2, Reg.R3, Reg.R4, Reg.R5);
                        //*/

                        //*
                        $dmb_sy (                                      );
                        $dsb_sy (                                      );
                        $eors   (Reg.R2, Reg.R3                        );
                        $isb_sy (                                      );
                        $ldr    (Reg.R0, Reg.R2                        );
                        $ldr    (Reg.R0, Reg.R2, 4 *   1               );
                        $ldr    (Reg.R0, Reg.R2, 4 *  31               );
                        $ldr    (Reg.R0, Reg.SP                        );
                        $ldr    (Reg.R0, Reg.SP, 4 *   1               );
                        $ldr    (Reg.R0, Reg.SP, 4 *  31               );
                        $ldr    (Reg.R0, Reg.SP, 4 * 255               );
                        $ldr    (Reg.R0, "test4"                       );
                        $nop    (                                      );
                        $nop    (                                      );
                        $ldr    (Reg.R0, Reg.R1, Reg.R2                );
                        $ldr    (Reg.R5,  0xFFFFFFFFL                  );
                        $ldr    (Reg.R5,  2147483647L                  );
                        $ldr    (Reg.R5, -2147483648L                  );
                        $ldr    (Reg.R5,  0x00000000L                  );
                        $b_dot  (                                      );
                        $ldr    (Reg.R0, Reg.R1, Reg.R2                );
                        $_align4(                                      );
                    label("test4");
                        $_word  (0x76543210                            );
                        //*/

                        /*
                        $ldrb   (Reg.R0, Reg.R2                        );
                        $ldrb   (Reg.R0, Reg.R2,  1                    );
                        $ldrb   (Reg.R0, Reg.R2, 31                    );
                        $ldrb   (Reg.R0, Reg.R1, Reg.R2                );
                        $ldrh   (Reg.R0, Reg.R2                        );
                        $ldrh   (Reg.R0, Reg.R2, 2 *  1                );
                        $ldrh   (Reg.R0, Reg.R2, 2 * 31                );
                        $ldrh   (Reg.R0, Reg.R1, Reg.R2                );
                        $ldrsb  (Reg.R0, Reg.R1, Reg.R2                );
                        $ldrsh  (Reg.R0, Reg.R1, Reg.R2                );
                        //*/

                        /*
                        $lsls   (Reg.R2, Reg.R3, 30                    );
                        $lsls   (Reg.R2, Reg.R3                        );
                        $lsrs   (Reg.R2, Reg.R3, 30                    );
                        $lsrs   (Reg.R2, Reg.R3                        );
                        $movs   (Reg.R0, 0                             );
                        $movs   (Reg.R0, 255                           );
                        $movs   (Reg.R0, Reg.R1                        );
                        $mov    (Reg.R0, Reg.R8                        );
                        $mov    (Reg.R8, Reg.R1                        );
                        $nop    (                                      );
                        $movs   (Reg.R2, Reg.R3, Shift.ASR, 30         );
                        $movs   (Reg.R2, Reg.R3, Shift.LSL, 30         );
                        $movs   (Reg.R2, Reg.R3, Shift.LSR, 30         );
                        $nop    (                                      );
                        $movs   (Reg.R2, Shift.ASR, Reg.R3             );
                        $movs   (Reg.R2, Shift.LSL, Reg.R3             );
                        $movs   (Reg.R2, Shift.LSR, Reg.R3             );
                        $movs   (Reg.R2, Shift.ROR, Reg.R3             );
                        $nop    (                                      );
                        //*/

                        /*
                        $mrs    (Reg.R0, SYSm.APSR                     );
                        $mrs    (Reg.R0, SYSm.IAPSR                    );
                        $mrs    (Reg.R0, SYSm.EAPSR                    );
                        $mrs    (Reg.R0, SYSm.XPSR                     );
                        $mrs    (Reg.R0, SYSm.IPSR                     );
                        $mrs    (Reg.R0, SYSm.EPSR                     );
                        $mrs    (Reg.R0, SYSm.IEPSR                    );
                        $mrs    (Reg.R0, SYSm.MSP                      );
                        $mrs    (Reg.R0, SYSm.PSP                      );
                        $mrs    (Reg.R0, SYSm.PRIMASK                  );
                        $mrs    (Reg.R0, SYSm.CONTROL                  );
                        $nop    (                                      );
                        $msr    (SYSm.APSR   , Reg.R0                  );
                        $msr    (SYSm.IAPSR  , Reg.R0                  );
                        $msr    (SYSm.EAPSR  , Reg.R0                  );
                        $msr    (SYSm.XPSR   , Reg.R0                  );
                        $msr    (SYSm.IPSR   , Reg.R0                  );
                        $msr    (SYSm.EPSR   , Reg.R0                  );
                        $msr    (SYSm.IEPSR  , Reg.R0                  );
                        $msr    (SYSm.MSP    , Reg.R0                  );
                        $msr    (SYSm.PSP    , Reg.R0                  );
                        $msr    (SYSm.PRIMASK, Reg.R0                  );
                        $msr    (SYSm.CONTROL, Reg.R0                  );
                        //*/

                        /*
                        $cpy    (Reg.R0, Reg.R1                        );
                        $cpy    (Reg.R0, Reg.R8                        );
                        $cpy    (Reg.R8, Reg.R1                        );
                        $muls   (Reg.R0, Reg.R1                        );
                        $mvns   (Reg.R2, Reg.R3                        );
                        $negs   (Reg.R2, Reg.R3                        );
                        $orrs   (Reg.R2, Reg.R3                        );
                        $pop    (Reg.R2                                );
                        $pop    (Reg.R2, Reg.R3, Reg.R4, Reg.R5        );
                        $pop    (Reg.R2, Reg.R3, Reg.R4, Reg.R5, Reg.R7);
                        $pop    (Reg.R2, Reg.R3, Reg.R4, Reg.R5, Reg.PC);
                        $push   (Reg.R2                                );
                        $push   (Reg.R2, Reg.R3, Reg.R4, Reg.R5        );
                        $push   (Reg.R2, Reg.R3, Reg.R4, Reg.R5, Reg.R7);
                        $push   (Reg.R2, Reg.R3, Reg.R4, Reg.R5, Reg.LR);
                        $rev    (Reg.R2, Reg.R3                        );
                        $rev16  (Reg.R2, Reg.R3                        );
                        $revsh  (Reg.R2, Reg.R3                        );
                        $rors   (Reg.R2, Reg.R3                        );
                        $rsbs   (Reg.R2, Reg.R3, 0                     );
                        //*/

                        /*
                        $sbcs   (Reg.R2, Reg.R3                        );
                        $str    (Reg.R0, Reg.R2                        );
                        $str    (Reg.R0, Reg.R2, 4 *   1               );
                        $str    (Reg.R0, Reg.R2, 4 *  31               );
                        $str    (Reg.R0, Reg.SP                        );
                        $str    (Reg.R0, Reg.SP, 4 *   1               );
                        $str    (Reg.R0, Reg.SP, 4 *  31               );
                        $str    (Reg.R0, Reg.SP, 4 * 255               );
                        $str    (Reg.R0, Reg.R1, Reg.R2                );
                        $strb   (Reg.R0, Reg.R2                        );
                        $strb   (Reg.R0, Reg.R2,  1                    );
                        $strb   (Reg.R0, Reg.R2, 31                    );
                        $strb   (Reg.R0, Reg.R1, Reg.R2                );
                        $strh   (Reg.R0, Reg.R2                        );
                        $strh   (Reg.R0, Reg.R2, 2 *  1                );
                        $strh   (Reg.R0, Reg.R2, 2 * 31                );
                        $strh   (Reg.R0, Reg.R1, Reg.R2                );
                        $subs   (Reg.R0, Reg.R1, 7                     );
                        $subs   (Reg.R0, 240                           );
                        $subs   (Reg.R0, Reg.R1, Reg.R2                );
                        $add    (Reg.SP, Reg.SP, 256                   );
                        $add_sp (                256                   );
                        $sub    (Reg.SP, Reg.SP, 256                   );
                        $sub_sp (                256                   );
                        $svc    (111                                   );
                        //*/

                        /*
                        $sxtb   (Reg.R2, Reg.R3                        );
                        $sxth   (Reg.R2, Reg.R3                        );
                        $tst    (Reg.R2, Reg.R3                        );
                        $udf    (222                                   );
                        $uxtb   (Reg.R2, Reg.R3                        );
                        $uxth   (Reg.R2, Reg.R3                        );
                        $yield  (                                      );
                        $movs   (Reg.R0, Reg.R0                        );
                        $mov    (Reg.R0, Reg.R0                        );
                        //*/
                }};

                asm.link(ProgSWD.DefaultCortexM_SRAMStart, true);

                SysUtil.stdDbg().println();
                if(xal) SysUtil.systemExit();

            } // if

            final boolean dumpDisassemblyAndArray = false;

            // Test 'ARMCortexMThumb' - STM32F1x
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_STM32.flProgram_stm32f1x(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x42A04C20L, 0x680CD907L, 0x30046004L, 0x45883104L, 0x601AD1F9L, 0xF000E7FEL, 0x4D1BF82DL, 0x42372614L,
                        0x685CD126L, 0x68DE689DL, 0x60266025L, 0xF822F000L, 0x2501691CL, 0xF0006025L, 0x680CF81DL, 0xF3BF8004L,
                        0xF0008F4FL, 0x4D11F817L, 0x42372614L, 0x3002D110L, 0x80040C24L, 0x8F4FF3BFL, 0xF80CF000L, 0x26144D0CL,
                        0xD1054237L, 0x31043002L, 0xD1E64588L, 0xE7FE601AL, 0xE7FE601DL, 0x695CB430L, 0x68272501L, 0xD1FC422FL,
                        0x4770BC30L, 0x20000000L, 0xFFFFFF9CL, 0xFFFFFF38L, 0xFFFFFED4L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_stm32f1x_flProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

            // Test 'ARMCortexMThumb' - STM32F2x
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_STM32.flProgram_stm32f2x(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x42A04C20L, 0x680CD907L, 0x30046004L, 0x45883104L, 0x601AD1F9L, 0xF000E7FEL, 0x4D1BF82DL, 0x423726F0L,
                        0x685CD126L, 0x68DE689DL, 0x60266025L, 0xF822F000L, 0x4D16691CL, 0xF0006025L, 0x680CF81DL, 0xF3BF8004L,
                        0xF0008F4FL, 0x4D12F817L, 0x423726F0L, 0x3002D110L, 0x80040C24L, 0x8F4FF3BFL, 0xF80CF000L, 0x26F04D0DL,
                        0xD1054237L, 0x31043002L, 0xD1E64588L, 0xE7FE601AL, 0xE7FE601DL, 0x695CB430L, 0x68274D07L, 0xD1FC422FL,
                        0x4770BC30L, 0x20000000L, 0xFFFFFF9CL, 0x00000101L, 0xFFFFFF38L, 0xFFFFFED4L, 0x00010000L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_stm32f2x_flProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

            // Test 'ARMCortexMThumb' - STM32L4x
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_STM32.flProgram_stm32l4x(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x42A04C1BL, 0x680CD907L, 0x30046004L, 0x45883104L, 0x601AD1F9L, 0xF000E7FEL, 0x4D16F822L, 0x423726FAL,
                        0x685CD11BL, 0x68DE689DL, 0x60266025L, 0xF817F000L, 0x2501691CL, 0xF0006025L, 0x680CF812L, 0x684C6004L,
                        0xF0006044L, 0x4D0CF80CL, 0x423726FAL, 0x3008D105L, 0x45883108L, 0x601AD1F1L, 0x601DE7FEL, 0xB430E7FEL,
                        0x4D06695CL, 0x422F6827L, 0xBC30D1FCL, 0x46C04770L, 0x20000000L, 0xFFFFFF9CL, 0xFFFFFF38L, 0x00010000L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_stm32l4x_flProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

            // Test 'ARMCortexMThumb' - STM32L*x
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_STM32.flProgram_stm32lx(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x42A04C1CL, 0x680CD907L, 0x30046004L, 0x45883104L, 0x601AD1F9L, 0xF000E7FEL, 0x4D17F825L, 0x42374E17L,
                        0x685CD11EL, 0x68DE689DL, 0x60266025L, 0xF81AF000L, 0x695D691CL, 0x6025699EL, 0xF0006026L, 0x69DCF813L,
                        0x60254D0FL, 0xF80EF000L, 0xC010C910L, 0xD1FB4588L, 0xF808F000L, 0x4E094D0BL, 0xD1014237L, 0xE7FE601AL,
                        0xE7FE601DL, 0x6A1CB430L, 0x68272501L, 0xD1FC422FL, 0x4770BC30L, 0x20000000L, 0xFFFFFF9CL, 0x00030700L,
                        0x00000408L, 0xFFFFFF38L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_stm32lx_flProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_STM32.elProgram_stm32l0x(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x42A04C1CL, 0x680CD907L, 0x30046004L, 0x45883104L, 0x601AD1F9L, 0xF000E7FEL, 0x4D17F825L, 0x42374E17L,
                        0x685CD11EL, 0x68DE689DL, 0x60266025L, 0xF81AF000L, 0x695D691CL, 0x6025699EL, 0xF0006026L, 0x69DCF813L,
                        0x60254D0FL, 0xF80EF000L, 0xC010C910L, 0xF80AF000L, 0x4E0A4D0CL, 0xD1034237L, 0xD1F54588L, 0xE7FE601AL,
                        0xE7FE601DL, 0x6A1CB430L, 0x68272501L, 0xD1FC422FL, 0x4770BC30L, 0x20000000L, 0xFFFFFF9CL, 0x00020500L,
                        0x00000110L, 0xFFFFFF38L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_stm32l0x_elProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_STM32.elProgram_stm32l1x(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x42A04C1CL, 0x680CD907L, 0x30046004L, 0x45883104L, 0x601AD1F9L, 0xF000E7FEL, 0x4D17F825L, 0x42374E17L,
                        0x685CD11EL, 0x68DE689DL, 0x60266025L, 0xF81AF000L, 0x695D691CL, 0x6025699EL, 0xF0006026L, 0x69DCF813L,
                        0x60254D0FL, 0xF80EF000L, 0xC010C910L, 0xF80AF000L, 0x4E0A4D0CL, 0xD1034237L, 0xD1F54588L, 0xE7FE601AL,
                        0xE7FE601DL, 0x6A1CB430L, 0x68272501L, 0xD1FC422FL, 0x4770BC30L, 0x20000000L, 0xFFFFFF9CL, 0x00000500L,
                        0x00000100L, 0xFFFFFF38L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_stm32l0x_elProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

            // Test 'ARMCortexMThumb' - STM32H7x
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_STM32.flProgram_stm32h7x(false, dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x5400F04FL, 0xD90742A0L, 0x6004680CL, 0x31043004L, 0xD1F94588L, 0xE7FE601AL, 0xF82EF000L, 0x0563F06FL,
                        0x42374E1AL, 0x685CD126L, 0x68DE689DL, 0x60266025L, 0xF822F000L, 0xF04F691CL, 0x60250532L, 0xF81CF000L,
                        0x0908F04FL, 0x8F4FF3BFL, 0x4B04F851L, 0x4B04F840L, 0x8F4FF3BFL, 0x0901F2A9L, 0x0F09EA19L, 0xF000D1F2L,
                        0xF06FF80BL, 0x4E0905C7L, 0xD1034237L, 0xD1E74588L, 0xE7FE601AL, 0xE7FE601DL, 0x695CB430L, 0x0504F04FL,
                        0x422F6827L, 0xBC30D1FCL, 0xBF004770L, 0x07EE0000L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_stm32h7_2345_x_flProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_STM32.flProgram_stm32h7x(true , dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x5400F04FL, 0xD90742A0L, 0x6004680CL, 0x31043004L, 0xD1F94588L, 0xE7FE601AL, 0xF82EF000L, 0x0563F06FL,
                        0x42374E1AL, 0x685CD126L, 0x68DE689DL, 0x60266025L, 0xF822F000L, 0xF04F691CL, 0x60250502L, 0xF81CF000L,
                        0x0904F04FL, 0x8F4FF3BFL, 0x4B04F851L, 0x4B04F840L, 0x8F4FF3BFL, 0x0901F2A9L, 0x0F09EA19L, 0xF000D1F2L,
                        0xF06FF80BL, 0x4E0905C7L, 0xD1034237L, 0xD1E74588L, 0xE7FE601AL, 0xE7FE601DL, 0x695CB430L, 0x0504F04FL,
                        0x422F6827L, 0xBC30D1FCL, 0xBF004770L, 0x07EE0000L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_stm32h7_ab_x_flProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

            // Test 'ARMCortexMThumb' - NRF5x
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_NRF5.flProgram_nrf5x(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x2400B672L, 0x4688601CL, 0x22014490L,
                        0x42A04C15L, 0x685CD908L, 0x6025689DL, 0xC010C910L, 0xD1F84588L, 0xE7FE601AL, 0xF815F000L, 0x250168DCL,
                        0xF0006025L, 0x685CF810L, 0x6025689DL, 0xC010C910L, 0xF809F000L, 0xD1F64588L, 0x250068DCL, 0xF0006025L,
                        0x601AF802L, 0xB470E7FEL, 0x2501691CL, 0x422E6826L, 0xBC70D0FCL, 0x46C04770L, 0x20000000L
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_nrf5x_flProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

            // Test 'ARMCortexMThumb' - RxA4M1x
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_RenMF3_I.flProgram_renesas_mf3(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x886A8829L, 0x882B3504L, 0x3504886CL,
                        0x80014811L, 0x80024811L, 0x80034811L, 0x80044811L, 0x21814811L, 0x48117001L, 0x78012240L, 0xD0FC4211L,
                        0x2100480DL, 0x480D7001L, 0x78012240L, 0xD1FC4211L, 0x2212480BL, 0x42118801L, 0x4F0AD001L, 0x42B5E002L,
                        0x2700D3D8L, 0xE7D5BE00L, 0x407EC130L, 0x407EC138L, 0x407EC140L, 0x407EC144L, 0x407EC114L, 0x407EC12CL,
                        0x407EC1F0L, 0xFFFFFF9CL
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_renesas_mf3_flProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if
            if(true) {
                if( !java.util.Arrays.equals(
                    SWDAsmARM_RenMF3_I.elProgram_renesas_mf3(dumpDisassemblyAndArray),
                    new long[] {
                        0x20000400L, 0x20000015L, 0x20000011L, 0x20000013L, 0xE7FEE7FEL, 0x35017829L, 0x8001480EL, 0x2181480EL,
                        0x480E7001L, 0x78012240L, 0xD0FC4211L, 0x2100480AL, 0x480A7001L, 0x78012240L, 0xD1FC4211L, 0x22124808L,
                        0x42118801L, 0x4F07D001L, 0x42B5E002L, 0x2700D3E2L, 0xE7DFBE00L, 0x407EC130L, 0x407EC114L, 0x407EC12CL,
                        0x407EC1F0L, 0xFFFFFF9CL
                    }
                ) ) throw XCom.newJXMFatalInitError("_fl_renesas_mf3_elProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

            // Test 'PIC16BitEPC' - dsPIC30
            if(true) {
                if( !XCom.arrayCompareExt(
                    true, "_CHK", "_REF", "%03d", "0x%06X",
                    dsPIC30F_PE.getArray24Bits_dsPIC30F(dumpDisassemblyAndArray),
                    new int[] {
                        0x040080, 0x000080, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0x20880A, 0x20882B, 0x20884C, 0x208A0F, 0x208FE0, 0x880100, 0x0907D0, 0x000000, 0xA9E044, 0xA9C044, 0xA9A044, 0xEFA2A8, 0x800210, 0x2FF1F1, 0x600001, 0xB30A00,
                        0x880210, 0x20C000, 0x881110, 0xEF2098, 0xA80098, 0x280000, 0x881100, 0xA9008D, 0xEF2762, 0xEF2764, 0x070002, 0x070016, 0x37FFFD, 0xAE0220, 0x37FFFE, 0x801120,
                        0x208007, 0x781B80, 0x20FFF3, 0x600183, 0xE90183, 0x320006, 0xAE0220, 0x37FFFE, 0x801120, 0x781B80, 0xE90183, 0x3AFFFA, 0xA9E221, 0x205000, 0x881110, 0xA8E221,
                        0xEFA224, 0x060000, 0x804000, 0xDE004C, 0x016000, 0x3700B3, 0x3700C4, 0x3700C3, 0x3700AC, 0x370142, 0x370107, 0x37015D, 0x3700C2, 0x3700F2, 0x3700DB, 0x37016A,
                        0x3700B1, 0x3700A3, 0x3700A2, 0x3700A1, 0x3700A0, 0x000000, 0x000000, 0x78001B, 0xDE004C, 0xE10061, 0x320005, 0xE10063, 0x320003, 0xB3C010, 0x784D80, 0x370001,
                        0xEB4D80, 0x000000, 0x804001, 0xDE08CC, 0xDD08C8, 0x2F0FF0, 0x60001B, 0x700001, 0x780D80, 0xDE0048, 0x60006F, 0xE10061, 0x32000C, 0xE10062, 0x320003, 0x200020,
                        0x884420, 0x37000C, 0x804010, 0xD10100, 0xB81163, 0xE88102, 0x884422, 0xA30800, 0x320005, 0x804010, 0x20FFF1, 0x600001, 0xE88000, 0x884420, 0x070008, 0x060000,
                        0x78038B, 0x20FFF0, 0x600017, 0xA0C000, 0x780B80, 0x07FFCF, 0x060000, 0xEF2224, 0x09001E, 0x000000, 0xA9E221, 0x204000, 0x881110, 0xA8E221, 0x78038B, 0x780037,
                        0x881120, 0x780037, 0x070049, 0x78000B, 0x900290, 0xE98285, 0x32003E, 0x904010, 0xB240F0, 0xB3C011, 0xE10401, 0x3A000A, 0x208000, 0x9040C0, 0xFB8081, 0x880191,
                        0x9003B0, 0xBA0037, 0x070039, 0xE90285, 0x3AFFFC, 0x37002F, 0xB3C021, 0xE10401, 0x3A002C, 0x208000, 0x900290, 0xD10305, 0xEB0080, 0x880111, 0x9000B0, 0x880121,
                        0x9040C0, 0x880131, 0xE90005, 0x320018, 0x200020, 0xCA8000, 0x200001, 0x800132, 0x880192, 0x800127, 0x000000, 0xBA0017, 0x07001F, 0xBAD897, 0xCB0000, 0x800132,
                        0x880192, 0x800127, 0x000000, 0xBAD097, 0x070017, 0xBA0017, 0x070015, 0xCB0000, 0xE90306, 0x3AFFED, 0xA60005, 0x370009, 0x800132, 0x880192, 0x800127, 0x000000,
                        0xBA0017, 0x07000A, 0xBAC017, 0xFB8000, 0x070007, 0xAE0220, 0x37FFFE, 0x801129, 0xA9E221, 0xA86223, 0xA8E221, 0x060000, 0xAE0220, 0x37FFFE, 0x801129, 0x881120,
                        0x060000, 0x781F80, 0xF80042, 0x200E00, 0xB72042, 0xA8C761, 0x200550, 0x883B30, 0x200AA0, 0x883B30, 0xA8E761, 0x000000, 0x000000, 0x0907D0, 0x000000, 0x803B07,
                        0xA7F007, 0x37FFFD, 0xF90042, 0x78004F, 0x060000, 0x230000, 0x780D80, 0x07FF5D, 0x060000, 0x78038B, 0x210000, 0x781B80, 0x200020, 0x780B80, 0x09000A, 0x000000,
                        0x07FF86, 0x060000, 0x78038B, 0x21BF10, 0x781B80, 0x200020, 0x780B80, 0x09000A, 0x000000, 0x07FF7D, 0x060000, 0x210000, 0x780D80, 0x07FF47, 0x060000, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x37FFCC, 0x060000, 0x240410, 0x883B00, 0x208007, 0x000000, 0x904027, 0xFB8080,
                        0x900127, 0x780281, 0x780202, 0x9041B7, 0xE00403, 0x320008, 0x780005, 0xB7E764, 0x883B14, 0x07FFA7, 0xB00404, 0xB08005, 0xE90183, 0x3AFFF8, 0x07FF41, 0x060000,
                        0x240450, 0x883B00, 0xB3C7F0, 0xB7E764, 0x804020, 0x883B10, 0x804013, 0xDE19C8, 0xB207F3, 0xE00403, 0x320005, 0x07FF95, 0x200200, 0xB42762, 0xE90183, 0x3AFFFB,
                        0x07FF2F, 0x060000, 0x240010, 0x883B00, 0x208007, 0xEB0000, 0x904027, 0x880130, 0x900027, 0x880120, 0x438366, 0x800130, 0x880190, 0x800127, 0x200083, 0xBB0BB6,
                        0xBBDBB6, 0xBBEBB6, 0xBB1BB6, 0xBB0BB6, 0xBBDBB6, 0xBBEBB6, 0xBB1BB6, 0xE90183, 0x3AFFF6, 0x07FF77, 0x070005, 0xDD004C, 0x78008B, 0x780880, 0x07FEE6, 0x060000,
                        0x208000, 0xEB0080, 0x9040A0, 0x880131, 0x9000A0, 0x880121, 0x400366, 0x200067, 0x800130, 0x880190, 0x800125, 0x200104, 0xBA0195, 0xE11836, 0x3A000B, 0xBADBB5,
                        0xBAD3D5, 0xE11836, 0x3A0007, 0xBA01B5, 0xE11836, 0x3A0004, 0xE90204, 0x3AFFF4, 0x200010, 0x370001, 0x200020, 0x060000, 0x240050, 0x883B00, 0x208066, 0xB3C7F0,
                        0xB7E032, 0x804027, 0x09000F, 0xBB1BB6, 0x07FF4C, 0x070005, 0xDD004C, 0x78008B, 0x780880, 0x07FEBB, 0x060000, 0x208007, 0x4380E6, 0xB3C7F0, 0xB7E032, 0x9003A7,
                        0x200020, 0x200103, 0xBA0137, 0xE11031, 0x3A0003, 0xE90183, 0x3AFFFB, 0x200010, 0x060000, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x37FF3C, 0x060000, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x37FF08, 0x060000, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x0000BB
                    }
                ) ) throw XCom.newJXMFatalInitError("_pe_dsPIC30F_peProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

            // Test 'PIC16BitEPC' - dsPIC33EP_GS25
            if(true) {
                if( !XCom.arrayCompareExt(
                    true, "_CHK", "_REF", "%03d", "0x%06X",
                    dsPIC33E_L1_PE.getArray24Bits_dsPIC33EP_GS25(dumpDisassemblyAndArray),
                    new int[] {
                        0x040080, 0x000080, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0x21200A, 0x21202B, 0x21204C, 0x21280F, 0x213FE0, 0x880100, 0x0907D0, 0x000000, 0xA9E045, 0xA9E044, 0xA9C044, 0xA9A044, 0xEF2E0E, 0xEF2E1E, 0x800210, 0x2FF1F1,
                        0x600001, 0xB30A00, 0x880210, 0x20C000, 0x881210, 0x200000, 0x881220, 0x201100, 0x884220, 0x280000, 0x881200, 0xA92821, 0xA94821, 0xEF272A, 0xEF272C, 0x070002,
                        0x070016, 0x37FFFD, 0xAE0240, 0x37FFFE, 0x801240, 0x210007, 0x781B80, 0x20FFF3, 0x600183, 0xE90183, 0x320006, 0xAE0240, 0x37FFFE, 0x801240, 0x781B80, 0xE90183,
                        0x3AFFFA, 0xA9E241, 0x205000, 0x881210, 0xA8E241, 0xEFA248, 0x060000, 0x808000, 0xDE004C, 0x010600, 0x3700A3, 0x37009E, 0x3700B3, 0x370110, 0x37009B, 0x3700DE,
                        0x370099, 0x3700B2, 0x370097, 0x3700C1, 0x370095, 0x3700A1, 0x370118, 0x370092, 0x370131, 0x370090, 0x000000, 0x000000, 0x78001B, 0xDE004C, 0xE10061, 0x320005,
                        0xE10063, 0x320003, 0xB3C010, 0x784D80, 0x370001, 0xEB4D80, 0x000000, 0x808001, 0xDE08CC, 0xDD08C8, 0x2F0FF0, 0x60001B, 0x700001, 0x780D80, 0xDE0048, 0x60006F,
                        0xE10062, 0x320003, 0x200020, 0x889020, 0x37000C, 0x808010, 0xD10100, 0xB81163, 0xE88102, 0x889022, 0xA30800, 0x320005, 0x808010, 0x20FFF1, 0x600001, 0xE88000,
                        0x889020, 0x070001, 0x060000, 0xEF2248, 0x09001E, 0x000000, 0xA9E241, 0x204000, 0x881210, 0xA8E241, 0x78038B, 0x780037, 0x881240, 0x780037, 0x07003C, 0x78000B,
                        0x900290, 0xE98285, 0x320031, 0x904010, 0xB240F0, 0xB3C021, 0xE10401, 0x3A002C, 0x210000, 0x900290, 0xD10305, 0xEB0080, 0x880111, 0x9000B0, 0x880121, 0x9040C0,
                        0x880131, 0xE90005, 0x320018, 0x200020, 0xCA8000, 0x200001, 0x800132, 0x8802A2, 0x800127, 0x000000, 0xBA0017, 0x07001F, 0xBAD897, 0xCB0000, 0x800132, 0x8802A2,
                        0x800127, 0x000000, 0xBAD097, 0x070017, 0xBA0017, 0x070015, 0xCB0000, 0xE90306, 0x3AFFED, 0xA60005, 0x370009, 0x800132, 0x8802A2, 0x800127, 0x000000, 0xBA0017,
                        0x07000A, 0xBAC017, 0xFB8000, 0x070007, 0xAE0240, 0x37FFFE, 0x801249, 0xA9E241, 0xA86243, 0xA8E241, 0x060000, 0xAE0240, 0x37FFFE, 0x801249, 0x881240, 0x060000,
                        0x781F80, 0xF80042, 0x200E00, 0xB72042, 0xA8C729, 0x000000, 0x000000, 0x200550, 0x883970, 0x200AA0, 0x883970, 0xA8E729, 0x000000, 0x000000, 0x000000, 0x000000,
                        0x000000, 0x000000, 0x0907D0, 0x000000, 0x803947, 0xA7F007, 0x37FFFD, 0xF90042, 0x78004F, 0x060000, 0x230000, 0x780D80, 0x07FF6D, 0x060000, 0x78038B, 0x210000,
                        0x781B80, 0x200020, 0x780B80, 0x09000A, 0x000000, 0x07FF8D, 0x060000, 0x78038B, 0x21BF10, 0x781B80, 0x200020, 0x780B80, 0x09000A, 0x000000, 0x07FF84, 0x060000,
                        0x210000, 0x780D80, 0x07FF57, 0x060000, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x37FFD6, 0x060000, 0x240030, 0x883940, 0x210007, 0x000000, 0xEB0180, 0x9041B7, 0xEB0280, 0x9042A7, 0x900227, 0xE00403, 0x320009,
                        0x883965, 0x883954, 0x07FFAD, 0xB02004, 0xB08005, 0xB02004, 0xB08005, 0xE90183, 0x3AFFF7, 0x210000, 0x78008B, 0x780880, 0x07FF2D, 0x060000, 0x242020, 0x883940,
                        0x210007, 0x000000, 0xEB0000, 0x904027, 0x883960, 0x900027, 0x883950, 0xEB0000, 0x883990, 0x438066, 0x883980, 0x07FF94, 0x070005, 0xDD004C, 0x78008B, 0x780880,
                        0x07FF19, 0x060000, 0x210000, 0xEB0080, 0x9040A0, 0x880131, 0x9000A0, 0x880121, 0x400366, 0x200067, 0x800130, 0x8802A0, 0x800125, 0x200204, 0xBA0195, 0xE11836,
                        0x3A000B, 0xBADBB5, 0xBAD3D5, 0xE11836, 0x3A0007, 0xBA01B5, 0xE11836, 0x3A0004, 0xE90204, 0x3AFFF4, 0x200010, 0x370001, 0x200020, 0x060000, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x37FF7C, 0x060000, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x37FF61, 0x060000, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x37FF2D, 0x060000, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x0000DF
                    }
                ) ) throw XCom.newJXMFatalInitError("_pe_dsPIC33EP_GS25_peProgram[] != _REF[]");
                if(dumpDisassemblyAndArray) SysUtil.stdDbg().println();
            } // if

        } // try
        catch(final Exception e) {
            e.printStackTrace();
            SysUtil.systemExitError();
        }

    } // void main()

} // class ATest1
