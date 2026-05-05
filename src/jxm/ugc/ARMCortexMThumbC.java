/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import jxm.*;


/*
 * Please refer to the comment block before the 'ARMCortexMThumb' class definition in the 'ARMCortexMThumb.java' file for more details and information.
 */
public class ARMCortexMThumbC extends ARMCortexMThumb {

    public ARMCortexMThumbC(final CPU cpu) throws JXMAsmError
    { super(cpu); }

    public ARMCortexMThumbC() throws JXMAsmError
    { super(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Recheck all and prefer to use the 16-bit instructions whenever possible? !!! #####

    // [T1] ADCS <Rdn>, <Rm>
    /*
    public ARMCortexMThumb $_adc_c_impl( final Cond cond, final Reg Rdn, final Reg Rm ) throws JXMAsmError
    { return __itcd__( "adc_c", cond, ()->{ return $adcs( Rdn, Rm ); }, Rdn, Rm ); }

    public ARMCortexMThumb $adceq( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.EQ, Rdn, Rm ); }
    ...
    public ARMCortexMThumb $adcvc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.VC, Rdn, Rm ); }
    //*/
    public ARMCortexMThumb $_adc_c_impl( final Cond cond, final Reg Rdn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "adc", cond, ()->{ return $adcs( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $adceq( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $adczr( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $adcne( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $adcnz( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $adchi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $adchs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $adclo( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $adcls( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $adcgt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $adcge( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $adclt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $adcle( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $adccs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $adccc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $adcmi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $adcpl( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $adcvs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $adcvc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_adc_c_impl( Cond.VC, Rdn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ADDS <Rd>, <Rn>, #<imm3>
    // [T1] ADD  <Rd>,  SP , #<imm8>
    // [T2] ADD   SP ,  SP , #<imm7>
    public ARMCortexMThumb $_add_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError                                               
    { return __itcd__( "add", cond, ()->{ return (Rd.regNum == Reg.SP.regNum || Rn.regNum == Reg.SP.regNum) ? $add( Rd, Rn, imm3_imm7_imm8 ) : $adds( Rd, Rn, imm3_imm7_imm8 ); }, Rd, Rn, imm3_imm7_imm8 ); } 
    
    public ARMCortexMThumb $addeq( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.EQ, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addzr( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.EQ, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addne( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.NE, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addnz( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.NE, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addhi( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.HI, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addhs( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.HS, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addlo( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.LO, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addls( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.LS, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addgt( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.GT, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addge( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.GE, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addlt( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.LT, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addle( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.LE, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addcs( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.CS, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addcc( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.CC, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addmi( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.MI, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addpl( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.PL, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addvs( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.VS, Rd, Rn, imm3_imm7_imm8 ); } 
    public ARMCortexMThumb $addvc( final Reg Rd, final Reg Rn, final int imm3_imm7_imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.VC, Rd, Rn, imm3_imm7_imm8 ); }

    // [T2] ADDS <Rdn>, #<imm8>
    public ARMCortexMThumb $_add_c_impl( final Cond cond, final Reg Rdn, final int imm8 ) throws JXMAsmError        
    { return __itcd__( "add", cond, ()->{ return $adds( Rdn, imm8 ); }, Rdn, imm8 ); } 
    
    public ARMCortexMThumb $addeq( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.EQ, Rdn, imm8 ); } 
    public ARMCortexMThumb $addzr( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.EQ, Rdn, imm8 ); } 
    public ARMCortexMThumb $addne( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.NE, Rdn, imm8 ); } 
    public ARMCortexMThumb $addnz( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.NE, Rdn, imm8 ); } 
    public ARMCortexMThumb $addhi( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.HI, Rdn, imm8 ); } 
    public ARMCortexMThumb $addhs( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.HS, Rdn, imm8 ); } 
    public ARMCortexMThumb $addlo( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.LO, Rdn, imm8 ); } 
    public ARMCortexMThumb $addls( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.LS, Rdn, imm8 ); } 
    public ARMCortexMThumb $addgt( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.GT, Rdn, imm8 ); } 
    public ARMCortexMThumb $addge( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.GE, Rdn, imm8 ); } 
    public ARMCortexMThumb $addlt( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.LT, Rdn, imm8 ); } 
    public ARMCortexMThumb $addle( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.LE, Rdn, imm8 ); } 
    public ARMCortexMThumb $addcs( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.CS, Rdn, imm8 ); } 
    public ARMCortexMThumb $addcc( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.CC, Rdn, imm8 ); } 
    public ARMCortexMThumb $addmi( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.MI, Rdn, imm8 ); } 
    public ARMCortexMThumb $addpl( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.PL, Rdn, imm8 ); } 
    public ARMCortexMThumb $addvs( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.VS, Rdn, imm8 ); } 
    public ARMCortexMThumb $addvc( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_add_c_impl( Cond.VC, Rdn, imm8 ); }

    // [T1] ADDS <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_add_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "add", cond, ()->{ return $adds( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $addeq( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addzr( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addne( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addnz( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addhi( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addhs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addlo( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addls( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addgt( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addge( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addlt( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addle( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addcs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addcc( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addmi( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addpl( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addvs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $addvc( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] ADD <Rdn>, <Rm>
    // [T1] ADD <Rdm>,  SP , <Rdm>
    // [T2] ADD  SP  , <Rm>
    public ARMCortexMThumb $_add_c_impl( final Cond cond, final Reg Rdn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "add", cond, ()->{ return $add( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $addeq( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $addzr( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $addne( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $addnz( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $addhi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $addhs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $addlo( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $addls( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $addgt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $addge( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $addlt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $addle( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $addcs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $addcc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $addmi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $addpl( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $addvs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $addvc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_add_c_impl( Cond.VC, Rdn, Rm ); }

    // [PI] ADD SP, SP, #<imm7>
    public ARMCortexMThumb $_add_sp_c_impl( final Cond cond, final int imm7 ) throws JXMAsmError        
    { return __itcd__( "add", cond, ()->{ return $add_sp( imm7 ); }, imm7 ); } 
    
    public ARMCortexMThumb $addeq_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.EQ, imm7 ); } 
    public ARMCortexMThumb $addzr_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.EQ, imm7 ); } 
    public ARMCortexMThumb $addne_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.NE, imm7 ); } 
    public ARMCortexMThumb $addnz_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.NE, imm7 ); } 
    public ARMCortexMThumb $addhi_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.HI, imm7 ); } 
    public ARMCortexMThumb $addhs_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.HS, imm7 ); } 
    public ARMCortexMThumb $addlo_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.LO, imm7 ); } 
    public ARMCortexMThumb $addls_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.LS, imm7 ); } 
    public ARMCortexMThumb $addgt_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.GT, imm7 ); } 
    public ARMCortexMThumb $addge_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.GE, imm7 ); } 
    public ARMCortexMThumb $addlt_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.LT, imm7 ); } 
    public ARMCortexMThumb $addle_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.LE, imm7 ); } 
    public ARMCortexMThumb $addcs_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.CS, imm7 ); } 
    public ARMCortexMThumb $addcc_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.CC, imm7 ); } 
    public ARMCortexMThumb $addmi_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.MI, imm7 ); } 
    public ARMCortexMThumb $addpl_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.PL, imm7 ); } 
    public ARMCortexMThumb $addvs_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.VS, imm7 ); } 
    public ARMCortexMThumb $addvc_sp( final int imm7 ) throws JXMAsmError { return $_add_sp_c_impl( Cond.VC, imm7 ); }

    // [T4] ADDW <Rd>, <Rn>, #<imm12>
    public ARMCortexMThumb $_addw_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError        
    { return __itcd__( "addw", cond, ()->{ return $addw( Rd, Rn, imm12 ); }, Rd, Rn, imm12 ); } 
    
    public ARMCortexMThumb $addweq( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.EQ, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwzr( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.EQ, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwne( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.NE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwnz( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.NE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwhi( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.HI, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwhs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.HS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwlo( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.LO, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwls( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.LS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwgt( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.GT, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwge( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.GE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwlt( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.LT, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwle( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.LE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwcs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.CS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwcc( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.CC, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwmi( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.MI, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwpl( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.PL, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwvs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.VS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $addwvc( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_addw_c_impl( Cond.VC, Rd, Rn, imm12 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ADR <Rd>, #<imm8>
    // [T1] ADR <Rd>, <label>
    public ARMCortexMThumb $_adr_c_impl( final Cond cond, final Reg Rd, final String label ) throws JXMAsmError        
    { return __itcd__( "adr", cond, ()->{ return $adr( Rd, label ); }, Rd, label ); } 
    
    public ARMCortexMThumb $adreq( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.EQ, Rd, label ); } 
    public ARMCortexMThumb $adrzr( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.EQ, Rd, label ); } 
    public ARMCortexMThumb $adrne( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.NE, Rd, label ); } 
    public ARMCortexMThumb $adrnz( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.NE, Rd, label ); } 
    public ARMCortexMThumb $adrhi( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.HI, Rd, label ); } 
    public ARMCortexMThumb $adrhs( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.HS, Rd, label ); } 
    public ARMCortexMThumb $adrlo( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.LO, Rd, label ); } 
    public ARMCortexMThumb $adrls( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.LS, Rd, label ); } 
    public ARMCortexMThumb $adrgt( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.GT, Rd, label ); } 
    public ARMCortexMThumb $adrge( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.GE, Rd, label ); } 
    public ARMCortexMThumb $adrlt( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.LT, Rd, label ); } 
    public ARMCortexMThumb $adrle( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.LE, Rd, label ); } 
    public ARMCortexMThumb $adrcs( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.CS, Rd, label ); } 
    public ARMCortexMThumb $adrcc( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.CC, Rd, label ); } 
    public ARMCortexMThumb $adrmi( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.MI, Rd, label ); } 
    public ARMCortexMThumb $adrpl( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.PL, Rd, label ); } 
    public ARMCortexMThumb $adrvs( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.VS, Rd, label ); } 
    public ARMCortexMThumb $adrvc( final Reg Rd, final String label ) throws JXMAsmError { return $_adr_c_impl( Cond.VC, Rd, label ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] AND<C> <Rdn>, <Rm>
    public ARMCortexMThumb $_and_c_impl( final Cond cond, final Reg Rdn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "and", cond, ()->{ return $ands( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $andeq( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $andzr( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $andne( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $andnz( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $andhi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $andhs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $andlo( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $andls( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $andgt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $andge( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $andlt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $andle( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $andcs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $andcc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $andmi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $andpl( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $andvs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $andvc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_and_c_impl( Cond.VC, Rdn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb _$asrs_w_impl_f(final Reg Rd , final Reg Rm, final int imm5) throws JXMAsmError
    { return _$asrs_w_impl(false, Rd , Rm , imm5); }

    private ARMCortexMThumb _$asrs_w_impl_t(final Reg Rd , final Reg Rm, final int imm5) throws JXMAsmError
    { return _$asrs_w_impl(true , Rd , Rm , imm5); }

    private ARMCortexMThumb _$asrs_w_impl_f(final Reg Rd , final Reg Rn, final Reg Rm  ) throws JXMAsmError
    { return _$asrs_w_impl(false, Rd , Rn , Rm  ); }

    private ARMCortexMThumb _$asrs_w_impl_t(final Reg Rd , final Reg Rn, final Reg Rm  ) throws JXMAsmError
    { return _$asrs_w_impl(true , Rd , Rn , Rm  ); }

    private ARMCortexMThumb _$asrs_w_impl_f(final Reg Rdn,               final Reg Rm  ) throws JXMAsmError
    { return _$asrs_w_impl(false, Rdn, Rdn, Rm  ); }

    private ARMCortexMThumb _$asrs_w_impl_t(final Reg Rdn,               final Reg Rm  ) throws JXMAsmError
    { return _$asrs_w_impl(true , Rdn, Rdn, Rm)  ; }

    // [T2] ASR.W  <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $_asr_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "asr", cond, ()->{ return _$asrs_w_impl_f( Rd , Rm, imm5 ); }, Rd , Rm, imm5 ); } 
    
    public ARMCortexMThumb $asreq( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrzr( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrne( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrnz( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrhi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.HI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrhs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.HS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrlo( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.LO, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrls( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.LS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrgt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.GT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrge( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.GE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrlt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.LT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrle( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.LE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrcs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.CS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrcc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.CC, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrmi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.MI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrpl( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.PL, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrvs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.VS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrvc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asr_c_impl( Cond.VC, Rd , Rm, imm5 ); }

    // [T2] ASRS.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $_asrs_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "asrs", cond, ()->{ return _$asrs_w_impl_t( Rd , Rm, imm5 ); }, Rd , Rm, imm5 ); } 
    
    public ARMCortexMThumb $asrseq( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrszr( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsne( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsnz( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrshi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.HI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrshs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.HS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrslo( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.LO, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsls( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.LS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsgt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.GT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsge( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.GE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrslt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.LT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsle( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.LE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrscs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.CS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrscc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.CC, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsmi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.MI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrspl( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.PL, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsvs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.VS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $asrsvc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_asrs_c_impl( Cond.VC, Rd , Rm, imm5 ); }

    // [T2] ASR.W  <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_asr_c_impl( final Cond cond, final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "asr", cond, ()->{ return _$asrs_w_impl_f( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $asreq( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrzr( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrne( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrnz( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrhi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrhs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrlo( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrls( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrgt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrge( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrlt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrle( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrcs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrcc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrmi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrpl( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrvs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrvc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] ASRS.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_asrs_c_impl( final Cond cond, final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "asrs", cond, ()->{ return _$asrs_w_impl_t( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $asrseq( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrszr( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsne( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsnz( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrshi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrshs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrslo( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsls( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsgt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsge( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrslt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsle( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrscs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrscc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsmi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrspl( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsvs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $asrsvc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] ASR.W  <Rdn>, <Rm>
    public ARMCortexMThumb $_asr_c_impl( final Cond cond, final Reg Rdn,               final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "asr", cond, ()->{ return _$asrs_w_impl_f( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $asreq( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $asrzr( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $asrne( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $asrnz( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $asrhi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $asrhs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $asrlo( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $asrls( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $asrgt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $asrge( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $asrlt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $asrle( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $asrcs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $asrcc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $asrmi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $asrpl( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $asrvs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $asrvc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asr_c_impl( Cond.VC, Rdn, Rm ); }

    // [T2] ASRS.W <Rdn>, <Rm>
    public ARMCortexMThumb $_asrs_c_impl( final Cond cond, final Reg Rdn,               final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "asrs", cond, ()->{ return _$asrs_w_impl_t( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $asrseq( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $asrszr( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsne( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsnz( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $asrshi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $asrshs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $asrslo( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsls( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsgt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsge( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $asrslt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsle( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $asrscs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $asrscc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsmi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $asrspl( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsvs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $asrsvc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_asrs_c_impl( Cond.VC, Rdn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T2] B <label>
    public ARMCortexMThumb $_b_c_impl( final Cond cond, final String label ) throws JXMAsmError        
    { return __itcd__( "b", cond, ()->{ return $b( label ); }, label ); } 
    
    public ARMCortexMThumb $beq( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.EQ, label ) : super.$beq( label ); } 
    public ARMCortexMThumb $bzr( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.EQ, label ) : super.$beq( label ); } 
    public ARMCortexMThumb $bne( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.NE, label ) : super.$bne( label ); } 
    public ARMCortexMThumb $bnz( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.NE, label ) : super.$bne( label ); } 
    public ARMCortexMThumb $bhi( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.HI, label ) : super.$bhi( label ); } 
    public ARMCortexMThumb $bhs( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.HS, label ) : super.$bhs( label ); } 
    public ARMCortexMThumb $blo( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.LO, label ) : super.$blo( label ); } 
    public ARMCortexMThumb $bls( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.LS, label ) : super.$bls( label ); } 
    public ARMCortexMThumb $bgt( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.GT, label ) : super.$bgt( label ); } 
    public ARMCortexMThumb $bge( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.GE, label ) : super.$bge( label ); } 
    public ARMCortexMThumb $blt( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.LT, label ) : super.$blt( label ); } 
    public ARMCortexMThumb $ble( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.LE, label ) : super.$ble( label ); } 
    public ARMCortexMThumb $bcs( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.CS, label ) : super.$bcs( label ); } 
    public ARMCortexMThumb $bcc( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.CC, label ) : super.$bcc( label ); } 
    public ARMCortexMThumb $bmi( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.MI, label ) : super.$bmi( label ); } 
    public ARMCortexMThumb $bpl( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.PL, label ) : super.$bpl( label ); } 
    public ARMCortexMThumb $bvs( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.VS, label ) : super.$bvs( label ); } 
    public ARMCortexMThumb $bvc( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_c_impl( Cond.VC, label ) : super.$bvc( label ); }

    // [T4] B.W <label>
    public ARMCortexMThumb $_b_w_c_impl( final Cond cond, final String label ) throws JXMAsmError        
    { return __itcd__( "b", cond, ()->{ return $b_w( label ); }, label ); } 
    
    public ARMCortexMThumb $beq_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.EQ, label ) : super.$beq_w( label ); } 
    public ARMCortexMThumb $bzr_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.EQ, label ) : super.$beq_w( label ); } 
    public ARMCortexMThumb $bne_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.NE, label ) : super.$bne_w( label ); } 
    public ARMCortexMThumb $bnz_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.NE, label ) : super.$bne_w( label ); } 
    public ARMCortexMThumb $bhi_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.HI, label ) : super.$bhi_w( label ); } 
    public ARMCortexMThumb $bhs_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.HS, label ) : super.$bhs_w( label ); } 
    public ARMCortexMThumb $blo_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.LO, label ) : super.$blo_w( label ); } 
    public ARMCortexMThumb $bls_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.LS, label ) : super.$bls_w( label ); } 
    public ARMCortexMThumb $bgt_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.GT, label ) : super.$bgt_w( label ); } 
    public ARMCortexMThumb $bge_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.GE, label ) : super.$bge_w( label ); } 
    public ARMCortexMThumb $blt_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.LT, label ) : super.$blt_w( label ); } 
    public ARMCortexMThumb $ble_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.LE, label ) : super.$ble_w( label ); } 
    public ARMCortexMThumb $bcs_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.CS, label ) : super.$bcs_w( label ); } 
    public ARMCortexMThumb $bcc_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.CC, label ) : super.$bcc_w( label ); } 
    public ARMCortexMThumb $bmi_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.MI, label ) : super.$bmi_w( label ); } 
    public ARMCortexMThumb $bpl_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.PL, label ) : super.$bpl_w( label ); } 
    public ARMCortexMThumb $bvs_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.VS, label ) : super.$bvs_w( label ); } 
    public ARMCortexMThumb $bvc_w( final String label ) throws JXMAsmError { return insideITBlock() ? $_b_w_c_impl( Cond.VC, label ) : super.$bvc_w( label ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BICS <Rdn>, <Rm>
    public ARMCortexMThumb $_bic_c_impl( final Cond cond, final Reg Rdn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "bic", cond, ()->{ return $bics( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $biceq( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $biczr( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $bicne( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $bicnz( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $bichi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $bichs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $biclo( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $bicls( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $bicgt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $bicge( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $biclt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $bicle( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $biccs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $biccc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $bicmi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $bicpl( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $bicvs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $bicvc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_bic_c_impl( Cond.VC, Rdn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BL <label>
    public ARMCortexMThumb $_bl_c_impl( final Cond cond, final String label ) throws JXMAsmError        
    { return __itcd__( "bl", cond, ()->{ return $bl( label ); }, label ); } 
    
    public ARMCortexMThumb $bleq( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.EQ, label ); } 
    public ARMCortexMThumb $blzr( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.EQ, label ); } 
    public ARMCortexMThumb $blne( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.NE, label ); } 
    public ARMCortexMThumb $blnz( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.NE, label ); } 
    public ARMCortexMThumb $blhi( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.HI, label ); } 
    public ARMCortexMThumb $blhs( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.HS, label ); } 
    public ARMCortexMThumb $bllo( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.LO, label ); } 
    public ARMCortexMThumb $blls( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.LS, label ); } 
    public ARMCortexMThumb $blgt( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.GT, label ); } 
    public ARMCortexMThumb $blge( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.GE, label ); } 
    public ARMCortexMThumb $bllt( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.LT, label ); } 
    public ARMCortexMThumb $blle( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.LE, label ); } 
    public ARMCortexMThumb $blcs( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.CS, label ); } 
    public ARMCortexMThumb $blcc( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.CC, label ); } 
    public ARMCortexMThumb $blmi( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.MI, label ); } 
    public ARMCortexMThumb $blpl( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.PL, label ); } 
    public ARMCortexMThumb $blvs( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.VS, label ); } 
    public ARMCortexMThumb $blvc( final String label ) throws JXMAsmError { return $_bl_c_impl( Cond.VC, label ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BLX <Rm>
    public ARMCortexMThumb $_blx_c_impl( final Cond cond, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "blx", cond, ()->{ return $blx( Rm ); }, Rm ); } 
    
    public ARMCortexMThumb $blxeq( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.EQ, Rm ); } 
    public ARMCortexMThumb $blxzr( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.EQ, Rm ); } 
    public ARMCortexMThumb $blxne( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.NE, Rm ); } 
    public ARMCortexMThumb $blxnz( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.NE, Rm ); } 
    public ARMCortexMThumb $blxhi( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.HI, Rm ); } 
    public ARMCortexMThumb $blxhs( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.HS, Rm ); } 
    public ARMCortexMThumb $blxlo( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.LO, Rm ); } 
    public ARMCortexMThumb $blxls( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.LS, Rm ); } 
    public ARMCortexMThumb $blxgt( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.GT, Rm ); } 
    public ARMCortexMThumb $blxge( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.GE, Rm ); } 
    public ARMCortexMThumb $blxlt( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.LT, Rm ); } 
    public ARMCortexMThumb $blxle( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.LE, Rm ); } 
    public ARMCortexMThumb $blxcs( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.CS, Rm ); } 
    public ARMCortexMThumb $blxcc( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.CC, Rm ); } 
    public ARMCortexMThumb $blxmi( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.MI, Rm ); } 
    public ARMCortexMThumb $blxpl( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.PL, Rm ); } 
    public ARMCortexMThumb $blxvs( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.VS, Rm ); } 
    public ARMCortexMThumb $blxvc( final Reg Rm ) throws JXMAsmError { return $_blx_c_impl( Cond.VC, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] BX <Rm>
    public ARMCortexMThumb $_bx_c_impl( final Cond cond, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "bx", cond, ()->{ return $bx( Rm ); }, Rm ); } 
    
    public ARMCortexMThumb $bxeq( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.EQ, Rm ); } 
    public ARMCortexMThumb $bxzr( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.EQ, Rm ); } 
    public ARMCortexMThumb $bxne( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.NE, Rm ); } 
    public ARMCortexMThumb $bxnz( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.NE, Rm ); } 
    public ARMCortexMThumb $bxhi( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.HI, Rm ); } 
    public ARMCortexMThumb $bxhs( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.HS, Rm ); } 
    public ARMCortexMThumb $bxlo( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.LO, Rm ); } 
    public ARMCortexMThumb $bxls( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.LS, Rm ); } 
    public ARMCortexMThumb $bxgt( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.GT, Rm ); } 
    public ARMCortexMThumb $bxge( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.GE, Rm ); } 
    public ARMCortexMThumb $bxlt( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.LT, Rm ); } 
    public ARMCortexMThumb $bxle( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.LE, Rm ); } 
    public ARMCortexMThumb $bxcs( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.CS, Rm ); } 
    public ARMCortexMThumb $bxcc( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.CC, Rm ); } 
    public ARMCortexMThumb $bxmi( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.MI, Rm ); } 
    public ARMCortexMThumb $bxpl( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.PL, Rm ); } 
    public ARMCortexMThumb $bxvs( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.VS, Rm ); } 
    public ARMCortexMThumb $bxvc( final Reg Rm ) throws JXMAsmError { return $_bx_c_impl( Cond.VC, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] CLREX
    public ARMCortexMThumb $_clrex_c_impl( final Cond cond ) throws JXMAsmError 
    { return __itcd__( "clrex", cond, ()->{ return $clrex(); } ); }         
    
    public ARMCortexMThumb $clrexeq() throws JXMAsmError { return $_clrex_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $clrexzr() throws JXMAsmError { return $_clrex_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $clrexne() throws JXMAsmError { return $_clrex_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $clrexnz() throws JXMAsmError { return $_clrex_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $clrexhi() throws JXMAsmError { return $_clrex_c_impl( Cond.HI ); } 
    public ARMCortexMThumb $clrexhs() throws JXMAsmError { return $_clrex_c_impl( Cond.HS ); } 
    public ARMCortexMThumb $clrexlo() throws JXMAsmError { return $_clrex_c_impl( Cond.LO ); } 
    public ARMCortexMThumb $clrexls() throws JXMAsmError { return $_clrex_c_impl( Cond.LS ); } 
    public ARMCortexMThumb $clrexgt() throws JXMAsmError { return $_clrex_c_impl( Cond.GT ); } 
    public ARMCortexMThumb $clrexge() throws JXMAsmError { return $_clrex_c_impl( Cond.GE ); } 
    public ARMCortexMThumb $clrexlt() throws JXMAsmError { return $_clrex_c_impl( Cond.LT ); } 
    public ARMCortexMThumb $clrexle() throws JXMAsmError { return $_clrex_c_impl( Cond.LE ); } 
    public ARMCortexMThumb $clrexcs() throws JXMAsmError { return $_clrex_c_impl( Cond.CS ); } 
    public ARMCortexMThumb $clrexcc() throws JXMAsmError { return $_clrex_c_impl( Cond.CC ); } 
    public ARMCortexMThumb $clrexmi() throws JXMAsmError { return $_clrex_c_impl( Cond.MI ); } 
    public ARMCortexMThumb $clrexpl() throws JXMAsmError { return $_clrex_c_impl( Cond.PL ); } 
    public ARMCortexMThumb $clrexvs() throws JXMAsmError { return $_clrex_c_impl( Cond.VS ); } 
    public ARMCortexMThumb $clrexvc() throws JXMAsmError { return $_clrex_c_impl( Cond.VC ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] CMN <Rn>, #<const>
    public ARMCortexMThumb $_cmn_c_impl( final Cond cond, final Reg Rn, final long const_ ) throws JXMAsmError        
    { return __itcd__( "cmn", cond, ()->{ return $cmn( Rn, const_ ); }, Rn, const_ ); } 
    
    public ARMCortexMThumb $cmneq( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.EQ, Rn, const_ ); } 
    public ARMCortexMThumb $cmnzr( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.EQ, Rn, const_ ); } 
    public ARMCortexMThumb $cmnne( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.NE, Rn, const_ ); } 
    public ARMCortexMThumb $cmnnz( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.NE, Rn, const_ ); } 
    public ARMCortexMThumb $cmnhi( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.HI, Rn, const_ ); } 
    public ARMCortexMThumb $cmnhs( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.HS, Rn, const_ ); } 
    public ARMCortexMThumb $cmnlo( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.LO, Rn, const_ ); } 
    public ARMCortexMThumb $cmnls( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.LS, Rn, const_ ); } 
    public ARMCortexMThumb $cmngt( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.GT, Rn, const_ ); } 
    public ARMCortexMThumb $cmnge( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.GE, Rn, const_ ); } 
    public ARMCortexMThumb $cmnlt( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.LT, Rn, const_ ); } 
    public ARMCortexMThumb $cmnle( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.LE, Rn, const_ ); } 
    public ARMCortexMThumb $cmncs( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.CS, Rn, const_ ); } 
    public ARMCortexMThumb $cmncc( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.CC, Rn, const_ ); } 
    public ARMCortexMThumb $cmnmi( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.MI, Rn, const_ ); } 
    public ARMCortexMThumb $cmnpl( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.PL, Rn, const_ ); } 
    public ARMCortexMThumb $cmnvs( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.VS, Rn, const_ ); } 
    public ARMCortexMThumb $cmnvc( final Reg Rn, final long const_ ) throws JXMAsmError { return $_cmn_c_impl( Cond.VC, Rn, const_ ); }

    // [T1] CMN <Rn>, <Rm>
    public ARMCortexMThumb $_cmn_c_impl( final Cond cond, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "cmn", cond, ()->{ return $cmn( Rn, Rm ); }, Rn, Rm ); } 
    
    public ARMCortexMThumb $cmneq( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.EQ, Rn, Rm ); } 
    public ARMCortexMThumb $cmnzr( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.EQ, Rn, Rm ); } 
    public ARMCortexMThumb $cmnne( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.NE, Rn, Rm ); } 
    public ARMCortexMThumb $cmnnz( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.NE, Rn, Rm ); } 
    public ARMCortexMThumb $cmnhi( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.HI, Rn, Rm ); } 
    public ARMCortexMThumb $cmnhs( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.HS, Rn, Rm ); } 
    public ARMCortexMThumb $cmnlo( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.LO, Rn, Rm ); } 
    public ARMCortexMThumb $cmnls( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.LS, Rn, Rm ); } 
    public ARMCortexMThumb $cmngt( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.GT, Rn, Rm ); } 
    public ARMCortexMThumb $cmnge( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.GE, Rn, Rm ); } 
    public ARMCortexMThumb $cmnlt( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.LT, Rn, Rm ); } 
    public ARMCortexMThumb $cmnle( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.LE, Rn, Rm ); } 
    public ARMCortexMThumb $cmncs( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.CS, Rn, Rm ); } 
    public ARMCortexMThumb $cmncc( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.CC, Rn, Rm ); } 
    public ARMCortexMThumb $cmnmi( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.MI, Rn, Rm ); } 
    public ARMCortexMThumb $cmnpl( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.PL, Rn, Rm ); } 
    public ARMCortexMThumb $cmnvs( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.VS, Rn, Rm ); } 
    public ARMCortexMThumb $cmnvc( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmn_c_impl( Cond.VC, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] CMP   <Rn>, #<imm8>
    // [T2] CMP.W <Rn>, #<const>
    public ARMCortexMThumb $_cmp_c_impl( final Cond cond, final Reg Rn, final long imm8_const ) throws JXMAsmError        
    { return __itcd__( "cmp", cond, ()->{ return $cmp( Rn, imm8_const ); }, Rn, imm8_const ); } 
    
    public ARMCortexMThumb $cmpeq( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.EQ, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpzr( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.EQ, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpne( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.NE, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpnz( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.NE, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmphi( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.HI, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmphs( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.HS, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmplo( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.LO, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpls( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.LS, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpgt( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.GT, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpge( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.GE, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmplt( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.LT, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmple( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.LE, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpcs( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.CS, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpcc( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.CC, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpmi( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.MI, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmppl( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.PL, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpvs( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.VS, Rn, imm8_const ); } 
    public ARMCortexMThumb $cmpvc( final Reg Rn, final long imm8_const ) throws JXMAsmError { return $_cmp_c_impl( Cond.VC, Rn, imm8_const ); }

    // [T1] CMP <Rn>, <Rm>
    // [T2] CMP <Rn>, <Rm>
    public ARMCortexMThumb $_cmp_c_impl( final Cond cond, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "cmp", cond, ()->{ return $cmp( Rn, Rm ); }, Rn, Rm ); } 
    
    public ARMCortexMThumb $cmpeq( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.EQ, Rn, Rm ); } 
    public ARMCortexMThumb $cmpzr( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.EQ, Rn, Rm ); } 
    public ARMCortexMThumb $cmpne( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.NE, Rn, Rm ); } 
    public ARMCortexMThumb $cmpnz( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.NE, Rn, Rm ); } 
    public ARMCortexMThumb $cmphi( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.HI, Rn, Rm ); } 
    public ARMCortexMThumb $cmphs( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.HS, Rn, Rm ); } 
    public ARMCortexMThumb $cmplo( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.LO, Rn, Rm ); } 
    public ARMCortexMThumb $cmpls( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.LS, Rn, Rm ); } 
    public ARMCortexMThumb $cmpgt( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.GT, Rn, Rm ); } 
    public ARMCortexMThumb $cmpge( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.GE, Rn, Rm ); } 
    public ARMCortexMThumb $cmplt( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.LT, Rn, Rm ); } 
    public ARMCortexMThumb $cmple( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.LE, Rn, Rm ); } 
    public ARMCortexMThumb $cmpcs( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.CS, Rn, Rm ); } 
    public ARMCortexMThumb $cmpcc( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.CC, Rn, Rm ); } 
    public ARMCortexMThumb $cmpmi( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.MI, Rn, Rm ); } 
    public ARMCortexMThumb $cmppl( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.PL, Rn, Rm ); } 
    public ARMCortexMThumb $cmpvs( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.VS, Rn, Rm ); } 
    public ARMCortexMThumb $cmpvc( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_cmp_c_impl( Cond.VC, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [PI] MOV <Rd>, <Rm>
    public ARMCortexMThumb $_cpy_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "cpy", cond, ()->{ return $cpy( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $cpyeq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $cpyzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $cpyne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $cpynz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $cpyhi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $cpyhs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $cpylo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $cpyls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $cpygt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $cpyge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $cpylt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $cpyle( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $cpycs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $cpycc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $cpymi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $cpypl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $cpyvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $cpyvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_cpy_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [PI] DEC1S <Rdn>
    public ARMCortexMThumb $_dec1_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec1", cond, ()->{ return $dec1s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec1eq( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec1zr( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec1ne( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec1nz( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec1hi( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec1hs( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec1lo( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec1ls( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec1gt( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec1ge( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec1lt( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec1le( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec1cs( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec1cc( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec1mi( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec1pl( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec1vs( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec1vc( final Reg Rdn ) throws JXMAsmError { return $_dec1_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC2S <Rdn>
    public ARMCortexMThumb $_dec2_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec2", cond, ()->{ return $dec2s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec2eq( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec2zr( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec2ne( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec2nz( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec2hi( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec2hs( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec2lo( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec2ls( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec2gt( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec2ge( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec2lt( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec2le( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec2cs( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec2cc( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec2mi( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec2pl( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec2vs( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec2vc( final Reg Rdn ) throws JXMAsmError { return $_dec2_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC4S <Rdn>
    public ARMCortexMThumb $_dec4_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec4", cond, ()->{ return $dec4s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec4eq( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec4zr( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec4ne( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec4nz( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec4hi( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec4hs( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec4lo( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec4ls( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec4gt( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec4ge( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec4lt( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec4le( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec4cs( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec4cc( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec4mi( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec4pl( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec4vs( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec4vc( final Reg Rdn ) throws JXMAsmError { return $_dec4_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC8S <Rdn>
    public ARMCortexMThumb $_dec8_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec8", cond, ()->{ return $dec8s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec8eq( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec8zr( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec8ne( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec8nz( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec8hi( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec8hs( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec8lo( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec8ls( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec8gt( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec8ge( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec8lt( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec8le( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec8cs( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec8cc( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec8mi( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec8pl( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec8vs( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec8vc( final Reg Rdn ) throws JXMAsmError { return $_dec8_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC16S <Rdn>
    public ARMCortexMThumb $_dec16_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec16", cond, ()->{ return $dec16s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec16eq( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec16zr( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec16ne( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec16nz( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec16hi( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec16hs( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec16lo( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec16ls( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec16gt( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec16ge( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec16lt( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec16le( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec16cs( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec16cc( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec16mi( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec16pl( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec16vs( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec16vc( final Reg Rdn ) throws JXMAsmError { return $_dec16_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC1W <Rdn>
    public ARMCortexMThumb $_dec1w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec1w", cond, ()->{ return $dec1w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec1weq( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec1wzr( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec1wne( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec1wnz( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec1whi( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec1whs( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec1wlo( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec1wls( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec1wgt( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec1wge( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec1wlt( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec1wle( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec1wcs( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec1wcc( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec1wmi( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec1wpl( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec1wvs( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec1wvc( final Reg Rdn ) throws JXMAsmError { return $_dec1w_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC2W <Rdn>
    public ARMCortexMThumb $_dec2w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec2w", cond, ()->{ return $dec2w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec2weq( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec2wzr( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec2wne( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec2wnz( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec2whi( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec2whs( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec2wlo( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec2wls( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec2wgt( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec2wge( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec2wlt( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec2wle( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec2wcs( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec2wcc( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec2wmi( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec2wpl( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec2wvs( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec2wvc( final Reg Rdn ) throws JXMAsmError { return $_dec2w_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC4W <Rdn>
    public ARMCortexMThumb $_dec4w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec4w", cond, ()->{ return $dec4w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec4weq( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec4wzr( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec4wne( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec4wnz( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec4whi( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec4whs( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec4wlo( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec4wls( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec4wgt( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec4wge( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec4wlt( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec4wle( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec4wcs( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec4wcc( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec4wmi( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec4wpl( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec4wvs( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec4wvc( final Reg Rdn ) throws JXMAsmError { return $_dec4w_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC8W <Rdn>
    public ARMCortexMThumb $_dec8w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec8w", cond, ()->{ return $dec8w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec8weq( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec8wzr( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec8wne( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec8wnz( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec8whi( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec8whs( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec8wlo( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec8wls( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec8wgt( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec8wge( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec8wlt( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec8wle( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec8wcs( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec8wcc( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec8wmi( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec8wpl( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec8wvs( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec8wvc( final Reg Rdn ) throws JXMAsmError { return $_dec8w_c_impl( Cond.VC, Rdn ); }

    // [PI] DEC16W <Rdn>
    public ARMCortexMThumb $_dec16w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "dec16w", cond, ()->{ return $dec16w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $dec16weq( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec16wzr( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $dec16wne( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec16wnz( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $dec16whi( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $dec16whs( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $dec16wlo( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $dec16wls( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $dec16wgt( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $dec16wge( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $dec16wlt( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $dec16wle( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $dec16wcs( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $dec16wcc( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $dec16wmi( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $dec16wpl( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $dec16wvs( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $dec16wvc( final Reg Rdn ) throws JXMAsmError { return $_dec16w_c_impl( Cond.VC, Rdn ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] DMB sy
    public ARMCortexMThumb $_dmb_sy_c_impl( final Cond cond ) throws JXMAsmError 
    { return __itcd__( "dmb", cond, ()->{ return $dmb_sy(); } ); }         
    
    public ARMCortexMThumb $dmbeq_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $dmbzr_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $dmbne_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $dmbnz_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $dmbhi_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.HI ); } 
    public ARMCortexMThumb $dmbhs_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.HS ); } 
    public ARMCortexMThumb $dmblo_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.LO ); } 
    public ARMCortexMThumb $dmbls_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.LS ); } 
    public ARMCortexMThumb $dmbgt_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.GT ); } 
    public ARMCortexMThumb $dmbge_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.GE ); } 
    public ARMCortexMThumb $dmblt_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.LT ); } 
    public ARMCortexMThumb $dmble_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.LE ); } 
    public ARMCortexMThumb $dmbcs_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.CS ); } 
    public ARMCortexMThumb $dmbcc_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.CC ); } 
    public ARMCortexMThumb $dmbmi_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.MI ); } 
    public ARMCortexMThumb $dmbpl_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.PL ); } 
    public ARMCortexMThumb $dmbvs_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.VS ); } 
    public ARMCortexMThumb $dmbvc_sy() throws JXMAsmError { return $_dmb_sy_c_impl( Cond.VC ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] DSB sy
    public ARMCortexMThumb $_dsb_sy_c_impl( final Cond cond ) throws JXMAsmError 
    { return __itcd__( "dsb", cond, ()->{ return $dsb_sy(); } ); }         
    
    public ARMCortexMThumb $dsbeq_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $dsbzr_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $dsbne_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $dsbnz_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $dsbhi_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.HI ); } 
    public ARMCortexMThumb $dsbhs_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.HS ); } 
    public ARMCortexMThumb $dsblo_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.LO ); } 
    public ARMCortexMThumb $dsbls_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.LS ); } 
    public ARMCortexMThumb $dsbgt_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.GT ); } 
    public ARMCortexMThumb $dsbge_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.GE ); } 
    public ARMCortexMThumb $dsblt_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.LT ); } 
    public ARMCortexMThumb $dsble_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.LE ); } 
    public ARMCortexMThumb $dsbcs_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.CS ); } 
    public ARMCortexMThumb $dsbcc_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.CC ); } 
    public ARMCortexMThumb $dsbmi_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.MI ); } 
    public ARMCortexMThumb $dsbpl_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.PL ); } 
    public ARMCortexMThumb $dsbvs_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.VS ); } 
    public ARMCortexMThumb $dsbvc_sy() throws JXMAsmError { return $_dsb_sy_c_impl( Cond.VC ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] EORS <Rdn>, <Rm>
    public ARMCortexMThumb $_eor_c_impl( final Cond cond, final Reg Rdn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "eor", cond, ()->{ return $eors( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $eoreq( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $eorzr( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $eorne( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $eornz( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $eorhi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $eorhs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $eorlo( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $eorls( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $eorgt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $eorge( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $eorlt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $eorle( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $eorcs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $eorcc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $eormi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $eorpl( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $eorvs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $eorvc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_eor_c_impl( Cond.VC, Rdn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [PI] INC1S <Rdn>
    public ARMCortexMThumb $_inc1_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc1", cond, ()->{ return $inc1s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc1eq( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc1zr( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc1ne( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc1nz( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc1hi( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc1hs( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc1lo( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc1ls( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc1gt( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc1ge( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc1lt( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc1le( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc1cs( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc1cc( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc1mi( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc1pl( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc1vs( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc1vc( final Reg Rdn ) throws JXMAsmError { return $_inc1_c_impl( Cond.VC, Rdn ); }

    // [PI] INC2S <Rdn>
    public ARMCortexMThumb $_inc2_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc2", cond, ()->{ return $inc2s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc2eq( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc2zr( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc2ne( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc2nz( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc2hi( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc2hs( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc2lo( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc2ls( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc2gt( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc2ge( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc2lt( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc2le( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc2cs( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc2cc( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc2mi( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc2pl( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc2vs( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc2vc( final Reg Rdn ) throws JXMAsmError { return $_inc2_c_impl( Cond.VC, Rdn ); }

    // [PI] INC4S <Rdn>
    public ARMCortexMThumb $_inc4_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc4", cond, ()->{ return $inc4s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc4eq( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc4zr( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc4ne( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc4nz( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc4hi( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc4hs( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc4lo( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc4ls( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc4gt( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc4ge( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc4lt( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc4le( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc4cs( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc4cc( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc4mi( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc4pl( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc4vs( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc4vc( final Reg Rdn ) throws JXMAsmError { return $_inc4_c_impl( Cond.VC, Rdn ); }

    // [PI] INC8S <Rdn>
    public ARMCortexMThumb $_inc8_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc8", cond, ()->{ return $inc8s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc8eq( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc8zr( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc8ne( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc8nz( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc8hi( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc8hs( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc8lo( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc8ls( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc8gt( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc8ge( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc8lt( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc8le( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc8cs( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc8cc( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc8mi( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc8pl( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc8vs( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc8vc( final Reg Rdn ) throws JXMAsmError { return $_inc8_c_impl( Cond.VC, Rdn ); }

    // [PI] INC16S <Rdn>
    public ARMCortexMThumb $_inc16_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc16", cond, ()->{ return $inc16s( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc16eq( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc16zr( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc16ne( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc16nz( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc16hi( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc16hs( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc16lo( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc16ls( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc16gt( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc16ge( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc16lt( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc16le( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc16cs( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc16cc( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc16mi( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc16pl( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc16vs( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc16vc( final Reg Rdn ) throws JXMAsmError { return $_inc16_c_impl( Cond.VC, Rdn ); }

    // [PI] INC1W <Rdn>
    public ARMCortexMThumb $_inc1w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc1w", cond, ()->{ return $inc1w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc1weq( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc1wzr( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc1wne( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc1wnz( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc1whi( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc1whs( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc1wlo( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc1wls( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc1wgt( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc1wge( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc1wlt( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc1wle( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc1wcs( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc1wcc( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc1wmi( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc1wpl( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc1wvs( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc1wvc( final Reg Rdn ) throws JXMAsmError { return $_inc1w_c_impl( Cond.VC, Rdn ); }

    // [PI] INC2W <Rdn>
    public ARMCortexMThumb $_inc2w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc2w", cond, ()->{ return $inc2w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc2weq( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc2wzr( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc2wne( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc2wnz( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc2whi( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc2whs( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc2wlo( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc2wls( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc2wgt( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc2wge( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc2wlt( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc2wle( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc2wcs( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc2wcc( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc2wmi( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc2wpl( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc2wvs( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc2wvc( final Reg Rdn ) throws JXMAsmError { return $_inc2w_c_impl( Cond.VC, Rdn ); }

    // [PI] INC4W <Rdn>
    public ARMCortexMThumb $_inc4w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc4w", cond, ()->{ return $inc4w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc4weq( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc4wzr( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc4wne( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc4wnz( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc4whi( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc4whs( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc4wlo( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc4wls( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc4wgt( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc4wge( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc4wlt( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc4wle( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc4wcs( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc4wcc( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc4wmi( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc4wpl( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc4wvs( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc4wvc( final Reg Rdn ) throws JXMAsmError { return $_inc4w_c_impl( Cond.VC, Rdn ); }

    // [PI] INC8W <Rdn>
    public ARMCortexMThumb $_inc8w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc8w", cond, ()->{ return $inc8w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc8weq( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc8wzr( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc8wne( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc8wnz( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc8whi( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc8whs( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc8wlo( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc8wls( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc8wgt( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc8wge( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc8wlt( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc8wle( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc8wcs( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc8wcc( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc8wmi( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc8wpl( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc8wvs( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc8wvc( final Reg Rdn ) throws JXMAsmError { return $_inc8w_c_impl( Cond.VC, Rdn ); }

    // [PI] INC16W <Rdn>
    public ARMCortexMThumb $_inc16w_c_impl( final Cond cond, final Reg Rdn ) throws JXMAsmError        
    { return __itcd__( "inc16w", cond, ()->{ return $inc16w( Rdn ); }, Rdn ); } 
    
    public ARMCortexMThumb $inc16weq( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc16wzr( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.EQ, Rdn ); } 
    public ARMCortexMThumb $inc16wne( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc16wnz( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.NE, Rdn ); } 
    public ARMCortexMThumb $inc16whi( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.HI, Rdn ); } 
    public ARMCortexMThumb $inc16whs( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.HS, Rdn ); } 
    public ARMCortexMThumb $inc16wlo( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.LO, Rdn ); } 
    public ARMCortexMThumb $inc16wls( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.LS, Rdn ); } 
    public ARMCortexMThumb $inc16wgt( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.GT, Rdn ); } 
    public ARMCortexMThumb $inc16wge( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.GE, Rdn ); } 
    public ARMCortexMThumb $inc16wlt( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.LT, Rdn ); } 
    public ARMCortexMThumb $inc16wle( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.LE, Rdn ); } 
    public ARMCortexMThumb $inc16wcs( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.CS, Rdn ); } 
    public ARMCortexMThumb $inc16wcc( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.CC, Rdn ); } 
    public ARMCortexMThumb $inc16wmi( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.MI, Rdn ); } 
    public ARMCortexMThumb $inc16wpl( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.PL, Rdn ); } 
    public ARMCortexMThumb $inc16wvs( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.VS, Rdn ); } 
    public ARMCortexMThumb $inc16wvc( final Reg Rdn ) throws JXMAsmError { return $_inc16w_c_impl( Cond.VC, Rdn ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ISB sy
    public ARMCortexMThumb $_isb_sy_c_impl( final Cond cond ) throws JXMAsmError 
    { return __itcd__( "isb", cond, ()->{ return $isb_sy(); } ); }         
    
    public ARMCortexMThumb $isbeq_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $isbzr_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $isbne_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $isbnz_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $isbhi_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.HI ); } 
    public ARMCortexMThumb $isbhs_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.HS ); } 
    public ARMCortexMThumb $isblo_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.LO ); } 
    public ARMCortexMThumb $isbls_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.LS ); } 
    public ARMCortexMThumb $isbgt_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.GT ); } 
    public ARMCortexMThumb $isbge_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.GE ); } 
    public ARMCortexMThumb $isblt_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.LT ); } 
    public ARMCortexMThumb $isble_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.LE ); } 
    public ARMCortexMThumb $isbcs_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.CS ); } 
    public ARMCortexMThumb $isbcc_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.CC ); } 
    public ARMCortexMThumb $isbmi_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.MI ); } 
    public ARMCortexMThumb $isbpl_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.PL ); } 
    public ARMCortexMThumb $isbvs_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.VS ); } 
    public ARMCortexMThumb $isbvc_sy() throws JXMAsmError { return $_isb_sy_c_impl( Cond.VC ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [1] LDMIA <Rn>{!}, <registers>
    public ARMCortexMThumb $_ldmia_c_impl( final Cond cond, final RegGen Rn, final Reg... Regs ) throws JXMAsmError        
    { return __itcd__( "ldmia", cond, ()->{ return $ldmia( Rn, Regs ); }, Rn, Regs ); } 
    
    public ARMCortexMThumb $ldmiaeq( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.EQ, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiazr( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.EQ, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiane( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.NE, Rn, Regs ); } 
    public ARMCortexMThumb $ldmianz( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.NE, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiahi( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.HI, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiahs( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.HS, Rn, Regs ); } 
    public ARMCortexMThumb $ldmialo( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.LO, Rn, Regs ); } 
    public ARMCortexMThumb $ldmials( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.LS, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiagt( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.GT, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiage( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.GE, Rn, Regs ); } 
    public ARMCortexMThumb $ldmialt( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.LT, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiale( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.LE, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiacs( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.CS, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiacc( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.CC, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiami( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.MI, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiapl( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.PL, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiavs( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.VS, Rn, Regs ); } 
    public ARMCortexMThumb $ldmiavc( final RegGen Rn, final Reg... Regs ) throws JXMAsmError { return $_ldmia_c_impl( Cond.VC, Rn, Regs ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T4] LDR.W <Rt>, [<Rn>], ±#<imm8>
    public ARMCortexMThumb $_ldr_pst_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError        
    { return __itcd__( "ldr", cond, ()->{ return $ldr_pst( Rt, Rn, imm8 ); }, Rt, Rn, imm8 ); } 
    
    public ARMCortexMThumb $ldreq_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.EQ, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrzr_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.EQ, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrne_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.NE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrnz_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.NE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrhi_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.HI, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrhs_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.HS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrlo_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.LO, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrls_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.LS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrgt_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.GT, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrge_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.GE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrlt_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.LT, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrle_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.LE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrcs_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.CS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrcc_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.CC, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrmi_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.MI, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrpl_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.PL, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrvs_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.VS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrvc_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldr_pst_c_impl( Cond.VC, Rt, Rn, imm8 ); }

    // [T1] LDR <Rt>, [<Rn>, #<imm5> ]
    // [T2] LDR <Rt>, [ SP , #<imm8> ]
    public ARMCortexMThumb $_ldr_c_impl( final Cond cond, final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError        
    { return __itcd__( "ldr", cond, ()->{ return $ldr( Rt, Rn, imm5_imm8_imm12 ); }, Rt, Rn, imm5_imm8_imm12 ); } 
    
    public ARMCortexMThumb $ldreq( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrzr( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrne( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrnz( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrhi( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.HI, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrhs( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.HS, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrlo( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.LO, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrls( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.LS, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrgt( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.GT, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrge( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.GE, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrlt( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.LT, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrle( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.LE, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrcs( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.CS, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrcc( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.CC, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrmi( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.MI, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrpl( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.PL, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrvs( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.VS, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $ldrvc( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_ldr_c_impl( Cond.VC, Rt, Rn, imm5_imm8_imm12 ); }

    // [T1] LDR <Rt>, [<Rn>]
    // [T2] LDR <Rt>, [ SP ]
    public ARMCortexMThumb $_ldr_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldr", cond, ()->{ return $ldr( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldreq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldrls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldrmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldrvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldr_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDR <Rt>, <label>
    public ARMCortexMThumb $_ldr_c_impl( final Cond cond, final Reg Rt, final String label ) throws JXMAsmError        
    { return __itcd__( "ldr", cond, ()->{ return $ldr( Rt, label ); }, Rt, label ); } 
    
    public ARMCortexMThumb $ldreq( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, label ); } 
    public ARMCortexMThumb $ldrzr( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, label ); } 
    public ARMCortexMThumb $ldrne( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, label ); } 
    public ARMCortexMThumb $ldrnz( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, label ); } 
    public ARMCortexMThumb $ldrhi( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.HI, Rt, label ); } 
    public ARMCortexMThumb $ldrhs( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.HS, Rt, label ); } 
    public ARMCortexMThumb $ldrlo( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.LO, Rt, label ); } 
    public ARMCortexMThumb $ldrls( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.LS, Rt, label ); } 
    public ARMCortexMThumb $ldrgt( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.GT, Rt, label ); } 
    public ARMCortexMThumb $ldrge( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.GE, Rt, label ); } 
    public ARMCortexMThumb $ldrlt( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.LT, Rt, label ); } 
    public ARMCortexMThumb $ldrle( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.LE, Rt, label ); } 
    public ARMCortexMThumb $ldrcs( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.CS, Rt, label ); } 
    public ARMCortexMThumb $ldrcc( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.CC, Rt, label ); } 
    public ARMCortexMThumb $ldrmi( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.MI, Rt, label ); } 
    public ARMCortexMThumb $ldrpl( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.PL, Rt, label ); } 
    public ARMCortexMThumb $ldrvs( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.VS, Rt, label ); } 
    public ARMCortexMThumb $ldrvc( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_c_impl( Cond.VC, Rt, label ); }

    // [T2] LDR.W <Rt>, <label>
    public ARMCortexMThumb $_ldr_w_c_impl( final Cond cond, final Reg Rt, final String label ) throws JXMAsmError        
    { return __itcd__( "ldr", cond, ()->{ return $ldr_w( Rt, label ); }, Rt, label ); } 
    
    public ARMCortexMThumb $ldreq_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.EQ, Rt, label ); } 
    public ARMCortexMThumb $ldrzr_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.EQ, Rt, label ); } 
    public ARMCortexMThumb $ldrne_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.NE, Rt, label ); } 
    public ARMCortexMThumb $ldrnz_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.NE, Rt, label ); } 
    public ARMCortexMThumb $ldrhi_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.HI, Rt, label ); } 
    public ARMCortexMThumb $ldrhs_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.HS, Rt, label ); } 
    public ARMCortexMThumb $ldrlo_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.LO, Rt, label ); } 
    public ARMCortexMThumb $ldrls_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.LS, Rt, label ); } 
    public ARMCortexMThumb $ldrgt_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.GT, Rt, label ); } 
    public ARMCortexMThumb $ldrge_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.GE, Rt, label ); } 
    public ARMCortexMThumb $ldrlt_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.LT, Rt, label ); } 
    public ARMCortexMThumb $ldrle_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.LE, Rt, label ); } 
    public ARMCortexMThumb $ldrcs_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.CS, Rt, label ); } 
    public ARMCortexMThumb $ldrcc_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.CC, Rt, label ); } 
    public ARMCortexMThumb $ldrmi_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.MI, Rt, label ); } 
    public ARMCortexMThumb $ldrpl_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.PL, Rt, label ); } 
    public ARMCortexMThumb $ldrvs_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.VS, Rt, label ); } 
    public ARMCortexMThumb $ldrvc_w( final Reg Rt, final String label ) throws JXMAsmError { return $_ldr_w_c_impl( Cond.VC, Rt, label ); }

    // [T1] LDR <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $_ldr_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "ldr", cond, ()->{ return $ldr( Rt, Rn, Rm ); }, Rt, Rn, Rm ); } 
    
    public ARMCortexMThumb $ldreq( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrzr( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrne( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrnz( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.HI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.HS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrlo( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.LO, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrls( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.LS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrgt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.GT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrge( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.GE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrlt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.LT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrle( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.LE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrcs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.CS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrcc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.CC, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrmi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.MI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrpl( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.PL, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrvs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.VS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrvc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldr_c_impl( Cond.VC, Rt, Rn, Rm ); }

    // [PI] LDR <Rt>, =#<imm32>
    public ARMCortexMThumb $_ldr_c_impl( final Cond cond, final Reg Rt, final long imm32 ) throws JXMAsmError        
    { return __itcd__( "ldr", cond, ()->{ return $ldr( Rt, imm32 ); }, Rt, imm32 ); } 
    
    public ARMCortexMThumb $ldreq( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrzr( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.EQ, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrne( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrnz( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.NE, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrhi( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.HI, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrhs( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.HS, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrlo( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.LO, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrls( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.LS, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrgt( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.GT, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrge( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.GE, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrlt( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.LT, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrle( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.LE, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrcs( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.CS, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrcc( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.CC, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrmi( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.MI, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrpl( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.PL, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrvs( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.VS, Rt, imm32 ); } 
    public ARMCortexMThumb $ldrvc( final Reg Rt, final long imm32 ) throws JXMAsmError { return $_ldr_c_impl( Cond.VC, Rt, imm32 ); }

    /*
     * [PI] ldri <Rd>, #<imm32>
     *
     * It will be converted to one of these instructions:
     *     [T2] MOV  <Rd>,  #<const>
     *     [T1] MOVS <Rd>,  #<imm8>
     *     [T2] MOVS <Rd>,  #<const>
     *     [PI] LDR  <Rt>, =#<imm32>
     */
    public ARMCortexMThumb $_ldri_c_impl( final Cond cond, final Reg Rd, final long imm32 ) throws JXMAsmError        
    { return __itcd__( "ldri", cond, ()->{ return $ldri( Rd, imm32 ); }, Rd, imm32 ); } 
    
    public ARMCortexMThumb $ldrieq( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.EQ, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrizr( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.EQ, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrine( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.NE, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrinz( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.NE, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrihi( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.HI, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrihs( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.HS, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrilo( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.LO, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrils( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.LS, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrigt( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.GT, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrige( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.GE, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrilt( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.LT, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrile( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.LE, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrics( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.CS, Rd, imm32 ); } 
    public ARMCortexMThumb $ldricc( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.CC, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrimi( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.MI, Rd, imm32 ); } 
    public ARMCortexMThumb $ldripl( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.PL, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrivs( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.VS, Rd, imm32 ); } 
    public ARMCortexMThumb $ldrivc( final Reg Rd, final long imm32 ) throws JXMAsmError { return $_ldri_c_impl( Cond.VC, Rd, imm32 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDRB <Rt>, [<Rn>, #<imm5>]
    public ARMCortexMThumb $_ldrb_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "ldrb", cond, ()->{ return $ldrb( Rt, Rn, imm5 ); }, Rt, Rn, imm5 ); } 
    
    public ARMCortexMThumb $ldrbeq( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.EQ, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbzr( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.EQ, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbne( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.NE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbnz( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.NE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbhi( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.HI, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbhs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.HS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrblo( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LO, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbls( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbgt( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.GT, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbge( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.GE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrblt( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LT, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrble( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbcs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.CS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbcc( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.CC, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbmi( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.MI, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbpl( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.PL, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbvs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.VS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrbvc( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrb_c_impl( Cond.VC, Rt, Rn, imm5 ); }

    // [T1] LDRB <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldrb_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldrb", cond, ()->{ return $ldrb( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldrbeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrblo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrblt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrble( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrbvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrb_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDRB <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $_ldrb_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "ldrb", cond, ()->{ return $ldrb( Rt, Rn, Rm ); }, Rt, Rn, Rm ); } 
    
    public ARMCortexMThumb $ldrbeq( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbzr( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbne( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbnz( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbhi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.HI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbhs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.HS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrblo( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LO, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbls( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbgt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.GT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbge( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.GE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrblt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrble( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.LE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbcs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.CS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbcc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.CC, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbmi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.MI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbpl( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.PL, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbvs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.VS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrbvc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrb_c_impl( Cond.VC, Rt, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDRH <Rt>, [<Rn>, #<imm5>]
    public ARMCortexMThumb $_ldrh_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "ldrh", cond, ()->{ return $ldrh( Rt, Rn, imm5 ); }, Rt, Rn, imm5 ); } 
    
    public ARMCortexMThumb $ldrheq( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.EQ, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhzr( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.EQ, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhne( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.NE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhnz( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.NE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhhi( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.HI, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhhs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.HS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhlo( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LO, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhls( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhgt( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.GT, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhge( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.GE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhlt( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LT, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhle( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhcs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.CS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhcc( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.CC, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhmi( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.MI, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhpl( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.PL, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhvs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.VS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $ldrhvc( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_ldrh_c_impl( Cond.VC, Rt, Rn, imm5 ); }

    // [T1] LDRH <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldrh_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldrh", cond, ()->{ return $ldrh( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldrheq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrhvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrh_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDRH <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $_ldrh_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "ldrh", cond, ()->{ return $ldrh( Rt, Rn, Rm ); }, Rt, Rn, Rm ); } 
    
    public ARMCortexMThumb $ldrheq( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhzr( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhne( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhnz( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhhi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.HI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhhs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.HS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhlo( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LO, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhls( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhgt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.GT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhge( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.GE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhlt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhle( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.LE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhcs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.CS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhcc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.CC, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhmi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.MI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhpl( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.PL, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhvs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.VS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrhvc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrh_c_impl( Cond.VC, Rt, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDRSB <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $_ldrsb_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "ldrsb", cond, ()->{ return $ldrsb( Rt, Rn, Rm ); }, Rt, Rn, Rm ); } 
    
    public ARMCortexMThumb $ldrsbeq( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbzr( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbne( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbnz( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbhi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.HI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbhs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.HS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsblo( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.LO, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbls( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.LS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbgt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.GT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbge( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.GE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsblt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.LT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsble( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.LE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbcs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.CS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbcc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.CC, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbmi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.MI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbpl( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.PL, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbvs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.VS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrsbvc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsb_c_impl( Cond.VC, Rt, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDRSH <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $_ldrsh_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "ldrsh", cond, ()->{ return $ldrsh( Rt, Rn, Rm ); }, Rt, Rn, Rm ); } 
    
    public ARMCortexMThumb $ldrsheq( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshzr( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshne( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshnz( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshhi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.HI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshhs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.HS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshlo( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.LO, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshls( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.LS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshgt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.GT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshge( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.GE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshlt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.LT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshle( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.LE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshcs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.CS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshcc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.CC, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshmi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.MI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshpl( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.PL, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshvs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.VS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $ldrshvc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ldrsh_c_impl( Cond.VC, Rt, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] LDA <Rt>, [<Rn>]
    public ARMCortexMThumb $_lda_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "lda", cond, ()->{ return $lda( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldaeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldazr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldane( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldanz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldahi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldahs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldalo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldals( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldagt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldage( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldalt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldale( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldacs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldacc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldami( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldapl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldavs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldavc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_lda_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDAB <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldab_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldab", cond, ()->{ return $ldab( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldabeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldabzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldabne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldabnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldabhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldabhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldablo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldabls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldabgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldabge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldablt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldable( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldabcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldabcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldabmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldabpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldabvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldabvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldab_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDAH <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldah_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldah", cond, ()->{ return $ldah( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldaheq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldahzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldahne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldahnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldahhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldahhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldahlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldahls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldahgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldahge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldahlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldahle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldahcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldahcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldahmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldahpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldahvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldahvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldah_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDAEX <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldaex_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldaex", cond, ()->{ return $ldaex( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldaexeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaex_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDAEXB <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldaexb_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldaexb", cond, ()->{ return $ldaexb( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldaexbeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexblo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexblt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexble( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexbvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexb_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDAEXH <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldaexh_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldaexh", cond, ()->{ return $ldaexh( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldaexheq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldaexhvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldaexh_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDREX <Rt>, [<Rn>, #<imm8>]
    public ARMCortexMThumb $_ldrex_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError        
    { return __itcd__( "ldrex", cond, ()->{ return $ldrex( Rt, Rn, imm8 ); }, Rt, Rn, imm8 ); } 
    
    public ARMCortexMThumb $ldrexeq( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.EQ, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexzr( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.EQ, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexne( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.NE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexnz( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.NE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexhi( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.HI, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexhs( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.HS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexlo( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.LO, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexls( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.LS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexgt( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.GT, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexge( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.GE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexlt( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.LT, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexle( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.LE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexcs( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.CS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexcc( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.CC, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexmi( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.MI, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexpl( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.PL, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexvs( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.VS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $ldrexvc( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_ldrex_c_impl( Cond.VC, Rt, Rn, imm8 ); }

    // [T1] LDREX <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldrex_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldrex", cond, ()->{ return $ldrex( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldrexeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrex_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDREXB <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldrexb_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldrexb", cond, ()->{ return $ldrexb( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldrexbeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexblo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexblt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexble( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexbvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexb_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] LDREXH <Rt>, [<Rn>]
    public ARMCortexMThumb $_ldrexh_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "ldrexh", cond, ()->{ return $ldrexh( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $ldrexheq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $ldrexhvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_ldrexh_c_impl( Cond.VC, Rt, Rn ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb _$lsls_w_impl_f(final Reg Rd , final Reg Rm, final int imm5) throws JXMAsmError
    { return _$lsls_w_impl(false, Rd , Rm , imm5); }

    private ARMCortexMThumb _$lsls_w_impl_t(final Reg Rd , final Reg Rm, final int imm5) throws JXMAsmError
    { return _$lsls_w_impl(true , Rd , Rm , imm5); }

    private ARMCortexMThumb _$lsls_w_impl_f(final Reg Rd , final Reg Rn, final Reg Rm  ) throws JXMAsmError
    { return _$lsls_w_impl(false, Rd , Rn , Rm  ); }

    private ARMCortexMThumb _$lsls_w_impl_t(final Reg Rd , final Reg Rn, final Reg Rm  ) throws JXMAsmError
    { return _$lsls_w_impl(true , Rd , Rn , Rm  ); }

    private ARMCortexMThumb _$lsls_w_impl_f(final Reg Rdn,               final Reg Rm  ) throws JXMAsmError
    { return _$lsls_w_impl(false, Rdn, Rdn, Rm  ); }

    private ARMCortexMThumb _$lsls_w_impl_t(final Reg Rdn,               final Reg Rm  ) throws JXMAsmError
    { return _$lsls_w_impl(true , Rdn, Rdn, Rm  ); }

    // [T2] LSL.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $_lsl_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "lsl", cond, ()->{ return _$lsls_w_impl_f( Rd , Rm, imm5 ); }, Rd , Rm, imm5 ); } 
    
    public ARMCortexMThumb $lsleq( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslzr( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslne( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslnz( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslhi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.HI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslhs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.HS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsllo( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.LO, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslls( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.LS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslgt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.GT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslge( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.GE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsllt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.LT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslle( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.LE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslcs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.CS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslcc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.CC, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslmi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.MI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslpl( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.PL, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslvs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.VS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslvc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsl_c_impl( Cond.VC, Rd , Rm, imm5 ); }

    // [T2] LSLS.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $_lsls_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "lsls", cond, ()->{ return _$lsls_w_impl_t( Rd , Rm, imm5 ); }, Rd , Rm, imm5 ); } 
    
    public ARMCortexMThumb $lslseq( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslszr( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsne( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsnz( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslshi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.HI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslshs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.HS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslslo( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.LO, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsls( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.LS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsgt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.GT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsge( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.GE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslslt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.LT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsle( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.LE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslscs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.CS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslscc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.CC, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsmi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.MI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslspl( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.PL, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsvs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.VS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lslsvc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsls_c_impl( Cond.VC, Rd , Rm, imm5 ); }

    // [T2] LSL.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_lsl_c_impl( final Cond cond, final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "lsl", cond, ()->{ return _$lsls_w_impl_f( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $lsleq( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslzr( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslne( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslnz( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslhi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslhs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsllo( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslls( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslgt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslge( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsllt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslle( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslcs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslcc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslmi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslpl( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslvs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslvc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] LSLS.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_lsls_c_impl( final Cond cond, final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "lsls", cond, ()->{ return _$lsls_w_impl_t( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $lslseq( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslszr( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsne( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsnz( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslshi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslshs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslslo( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsls( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsgt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsge( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslslt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsle( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslscs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslscc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsmi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslspl( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsvs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lslsvc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] LSL.W <Rdn>, <Rm>
    public ARMCortexMThumb $_lsl_c_impl( final Cond cond, final Reg Rdn,               final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "lsl", cond, ()->{ return _$lsls_w_impl_f( Rdn,     Rm ); }, Rdn,     Rm ); } 
    
    public ARMCortexMThumb $lsleq( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslzr( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslne( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslnz( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslhi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.HI, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslhs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.HS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsllo( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.LO, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslls( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.LS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslgt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.GT, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslge( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.GE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsllt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.LT, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslle( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.LE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslcs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.CS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslcc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.CC, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslmi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.MI, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslpl( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.PL, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslvs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.VS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslvc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsl_c_impl( Cond.VC, Rdn,     Rm ); }

    // [T2] LSLS.W <Rdn>, <Rm>
    public ARMCortexMThumb $_lsls_c_impl( final Cond cond, final Reg Rdn,               final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "lsls", cond, ()->{ return _$lsls_w_impl_t( Rdn,     Rm ); }, Rdn,     Rm ); } 
    
    public ARMCortexMThumb $lslseq( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslszr( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsne( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsnz( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslshi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.HI, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslshs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.HS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslslo( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.LO, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsls( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.LS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsgt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.GT, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsge( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.GE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslslt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.LT, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsle( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.LE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslscs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.CS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslscc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.CC, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsmi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.MI, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslspl( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.PL, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsvs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.VS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lslsvc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsls_c_impl( Cond.VC, Rdn,     Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb _$lsrs_w_impl_f(final Reg Rd , final Reg Rm, final int imm5) throws JXMAsmError
    { return _$lsrs_w_impl(false, Rd , Rm , imm5); }

    private ARMCortexMThumb _$lsrs_w_impl_t(final Reg Rd , final Reg Rm, final int imm5) throws JXMAsmError
    { return _$lsrs_w_impl(true , Rd , Rm , imm5); }

    private ARMCortexMThumb _$lsrs_w_impl_f(final Reg Rd , final Reg Rn, final Reg Rm  ) throws JXMAsmError
    { return _$lsrs_w_impl(false, Rd , Rn , Rm  ); }

    private ARMCortexMThumb _$lsrs_w_impl_t(final Reg Rd , final Reg Rn, final Reg Rm  ) throws JXMAsmError
    { return _$lsrs_w_impl(true , Rd , Rn , Rm  ); }

    private ARMCortexMThumb _$lsrs_w_impl_f(final Reg Rdn,               final Reg Rm  ) throws JXMAsmError
    { return _$lsrs_w_impl(false, Rdn, Rdn, Rm  ); }

    private ARMCortexMThumb _$lsrs_w_impl_t(final Reg Rdn,               final Reg Rm  ) throws JXMAsmError
    { return _$lsrs_w_impl(true , Rdn, Rdn, Rm  ); }

    // [T2] LSR.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $_lsr_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "lsr", cond, ()->{ return _$lsrs_w_impl_f( Rd , Rm, imm5 ); }, Rd , Rm, imm5 ); } 
    
    public ARMCortexMThumb $lsreq( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrzr( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrne( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrnz( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrhi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.HI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrhs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.HS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrlo( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.LO, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrls( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.LS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrgt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.GT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrge( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.GE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrlt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.LT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrle( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.LE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrcs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.CS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrcc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.CC, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrmi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.MI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrpl( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.PL, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrvs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.VS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrvc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsr_c_impl( Cond.VC, Rd , Rm, imm5 ); }

    // [T2] LSRS.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $_lsrs_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "lsrs", cond, ()->{ return _$lsrs_w_impl_t( Rd , Rm, imm5 ); }, Rd , Rm, imm5 ); } 
    
    public ARMCortexMThumb $lsrseq( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrszr( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsne( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsnz( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrshi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.HI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrshs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.HS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrslo( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LO, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsls( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsgt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.GT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsge( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.GE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrslt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsle( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrscs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.CS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrscc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.CC, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsmi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.MI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrspl( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.PL, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsvs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.VS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $lsrsvc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_lsrs_c_impl( Cond.VC, Rd , Rm, imm5 ); }

    // [T2] LSR.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_lsr_c_impl( final Cond cond, final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "lsr", cond, ()->{ return _$lsrs_w_impl_f( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $lsreq( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrzr( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrne( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrnz( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrhi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrhs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrlo( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrls( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrgt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrge( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrlt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrle( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrcs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrcc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrmi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrpl( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrvs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrvc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] LSRS.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_lsrs_c_impl( final Cond cond, final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "lsrs", cond, ()->{ return _$lsrs_w_impl_t( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $lsrseq( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrszr( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsne( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsnz( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrshi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrshs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrslo( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsls( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsgt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsge( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrslt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsle( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrscs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrscc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsmi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrspl( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsvs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $lsrsvc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] LSR.W <Rdn>, <Rm>
    public ARMCortexMThumb $_lsr_c_impl( final Cond cond, final Reg Rdn,               final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "lsr", cond, ()->{ return _$lsrs_w_impl_f( Rdn,     Rm ); }, Rdn,     Rm ); } 
    
    public ARMCortexMThumb $lsreq( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrzr( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrne( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrnz( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrhi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.HI, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrhs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.HS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrlo( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.LO, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrls( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.LS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrgt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.GT, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrge( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.GE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrlt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.LT, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrle( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.LE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrcs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.CS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrcc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.CC, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrmi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.MI, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrpl( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.PL, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrvs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.VS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrvc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsr_c_impl( Cond.VC, Rdn,     Rm ); }

    // [T2] LSRS.W <Rdn>, <Rm>
    public ARMCortexMThumb $_lsrs_c_impl( final Cond cond, final Reg Rdn,               final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "lsrs", cond, ()->{ return _$lsrs_w_impl_t( Rdn,     Rm ); }, Rdn,     Rm ); } 
    
    public ARMCortexMThumb $lsrseq( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrszr( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsne( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsnz( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrshi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.HI, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrshs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.HS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrslo( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LO, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsls( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsgt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.GT, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsge( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.GE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrslt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LT, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsle( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.LE, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrscs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.CS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrscc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.CC, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsmi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.MI, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrspl( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.PL, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsvs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.VS, Rdn,     Rm ); } 
    public ARMCortexMThumb $lsrsvc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_lsrs_c_impl( Cond.VC, Rdn,     Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb _$movx_w_impl_f(final Reg Rd, final long const_) throws JXMAsmError
    { return _$movx_w_impl(false, Rd, const_); }

    private ARMCortexMThumb _$movx_w_impl_t(final Reg Rd, final long const_) throws JXMAsmError
    { return _$movx_w_impl(true , Rd, const_); }

    // [T2] MOV.W <Rd>, #<const>
    public ARMCortexMThumb $_mov_c_impl( final Cond cond, final Reg Rd , final long const_ ) throws JXMAsmError        
    { return __itcd__( "mov", cond, ()->{ return _$movx_w_impl_f( Rd, const_ ); }, Rd, const_ ); } 
    
    public ARMCortexMThumb $moveq( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd, const_ ); } 
    public ARMCortexMThumb $movzr( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd, const_ ); } 
    public ARMCortexMThumb $movne( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd, const_ ); } 
    public ARMCortexMThumb $movnz( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd, const_ ); } 
    public ARMCortexMThumb $movhi( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.HI, Rd, const_ ); } 
    public ARMCortexMThumb $movhs( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.HS, Rd, const_ ); } 
    public ARMCortexMThumb $movlo( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.LO, Rd, const_ ); } 
    public ARMCortexMThumb $movls( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.LS, Rd, const_ ); } 
    public ARMCortexMThumb $movgt( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.GT, Rd, const_ ); } 
    public ARMCortexMThumb $movge( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.GE, Rd, const_ ); } 
    public ARMCortexMThumb $movlt( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.LT, Rd, const_ ); } 
    public ARMCortexMThumb $movle( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.LE, Rd, const_ ); } 
    public ARMCortexMThumb $movcs( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.CS, Rd, const_ ); } 
    public ARMCortexMThumb $movcc( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.CC, Rd, const_ ); } 
    public ARMCortexMThumb $movmi( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.MI, Rd, const_ ); } 
    public ARMCortexMThumb $movpl( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.PL, Rd, const_ ); } 
    public ARMCortexMThumb $movvs( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.VS, Rd, const_ ); } 
    public ARMCortexMThumb $movvc( final Reg Rd , final long const_ ) throws JXMAsmError { return $_mov_c_impl( Cond.VC, Rd, const_ ); }

    // [T2] MOVS.W <Rd>, #<const>
    public ARMCortexMThumb $_movs_c_impl( final Cond cond, final Reg Rd , final long const_ ) throws JXMAsmError        
    { return __itcd__( "movs", cond, ()->{ return _$movx_w_impl_t( Rd, const_ ); }, Rd, const_ ); } 
    
    public ARMCortexMThumb $movseq( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd, const_ ); } 
    public ARMCortexMThumb $movszr( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd, const_ ); } 
    public ARMCortexMThumb $movsne( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd, const_ ); } 
    public ARMCortexMThumb $movsnz( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd, const_ ); } 
    public ARMCortexMThumb $movshi( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.HI, Rd, const_ ); } 
    public ARMCortexMThumb $movshs( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.HS, Rd, const_ ); } 
    public ARMCortexMThumb $movslo( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.LO, Rd, const_ ); } 
    public ARMCortexMThumb $movsls( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.LS, Rd, const_ ); } 
    public ARMCortexMThumb $movsgt( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.GT, Rd, const_ ); } 
    public ARMCortexMThumb $movsge( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.GE, Rd, const_ ); } 
    public ARMCortexMThumb $movslt( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.LT, Rd, const_ ); } 
    public ARMCortexMThumb $movsle( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.LE, Rd, const_ ); } 
    public ARMCortexMThumb $movscs( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.CS, Rd, const_ ); } 
    public ARMCortexMThumb $movscc( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.CC, Rd, const_ ); } 
    public ARMCortexMThumb $movsmi( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.MI, Rd, const_ ); } 
    public ARMCortexMThumb $movspl( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.PL, Rd, const_ ); } 
    public ARMCortexMThumb $movsvs( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.VS, Rd, const_ ); } 
    public ARMCortexMThumb $movsvc( final Reg Rd , final long const_ ) throws JXMAsmError { return $_movs_c_impl( Cond.VC, Rd, const_ ); }

    // [T3] MOV.W <Rd>, <Rm>
    public ARMCortexMThumb $_mov_c_impl( final Cond cond, final Reg Rd , final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "mov", cond, ()->{ return $mov_w( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $moveq( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $movzr( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $movne( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $movnz( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $movhi( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $movhs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $movlo( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $movls( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $movgt( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $movge( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $movlt( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $movle( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $movcs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $movcc( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $movmi( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $movpl( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $movvs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $movvc( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_mov_c_impl( Cond.VC, Rd, Rm ); }

    // [T3] MOVS.W <Rd>, <Rm>
    public ARMCortexMThumb $_movs_c_impl( final Cond cond, final Reg Rd , final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "movs", cond, ()->{ return $movs_w( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $movseq( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $movszr( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $movsne( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $movsnz( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $movshi( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $movshs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $movslo( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $movsls( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $movsgt( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $movsge( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $movslt( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $movsle( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $movscs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $movscc( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $movsmi( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $movspl( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $movsvs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $movsvc( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_movs_c_impl( Cond.VC, Rd, Rm ); }

    // [T3] MOVW <Rd>, #<imm16>
    public ARMCortexMThumb $_movw_c_impl( final Cond cond, final Reg Rd, final int imm16 ) throws JXMAsmError        
    { return __itcd__( "movw", cond, ()->{ return $movw( Rd, imm16 ); }, Rd, imm16 ); } 
    
    public ARMCortexMThumb $movweq( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.EQ, Rd, imm16 ); } 
    public ARMCortexMThumb $movwzr( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.EQ, Rd, imm16 ); } 
    public ARMCortexMThumb $movwne( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.NE, Rd, imm16 ); } 
    public ARMCortexMThumb $movwnz( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.NE, Rd, imm16 ); } 
    public ARMCortexMThumb $movwhi( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.HI, Rd, imm16 ); } 
    public ARMCortexMThumb $movwhs( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.HS, Rd, imm16 ); } 
    public ARMCortexMThumb $movwlo( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.LO, Rd, imm16 ); } 
    public ARMCortexMThumb $movwls( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.LS, Rd, imm16 ); } 
    public ARMCortexMThumb $movwgt( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.GT, Rd, imm16 ); } 
    public ARMCortexMThumb $movwge( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.GE, Rd, imm16 ); } 
    public ARMCortexMThumb $movwlt( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.LT, Rd, imm16 ); } 
    public ARMCortexMThumb $movwle( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.LE, Rd, imm16 ); } 
    public ARMCortexMThumb $movwcs( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.CS, Rd, imm16 ); } 
    public ARMCortexMThumb $movwcc( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.CC, Rd, imm16 ); } 
    public ARMCortexMThumb $movwmi( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.MI, Rd, imm16 ); } 
    public ARMCortexMThumb $movwpl( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.PL, Rd, imm16 ); } 
    public ARMCortexMThumb $movwvs( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.VS, Rd, imm16 ); } 
    public ARMCortexMThumb $movwvc( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movw_c_impl( Cond.VC, Rd, imm16 ); }

    // [PI] MOV <Rd>, <Rm>, <shift_operand> #<imm5>
    public ARMCortexMThumb $_mov_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "mov", cond, ()->{ return $mov( Rd , Rm , shift, imm5 ); }, Rd , Rm , shift, imm5 ); } 
    
    public ARMCortexMThumb $moveq( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movzr( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movne( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movnz( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movhi( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.HI, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movhs( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.HS, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movlo( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.LO, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movls( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.LS, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movgt( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.GT, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movge( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.GE, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movlt( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.LT, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movle( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.LE, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movcs( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.CS, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movcc( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.CC, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movmi( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.MI, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movpl( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.PL, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movvs( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.VS, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movvc( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mov_c_impl( Cond.VC, Rd , Rm , shift, imm5 ); }

    // [PI] MOVS <Rd>, <Rm>, <shift_operand> #<imm5>
    public ARMCortexMThumb $_movs_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "movs", cond, ()->{ return $movs( Rd , Rm , shift, imm5 ); }, Rd , Rm , shift, imm5 ); } 
    
    public ARMCortexMThumb $movseq( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movszr( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsne( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsnz( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movshi( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.HI, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movshs( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.HS, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movslo( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.LO, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsls( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.LS, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsgt( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.GT, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsge( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.GE, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movslt( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.LT, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsle( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.LE, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movscs( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.CS, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movscc( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.CC, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsmi( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.MI, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movspl( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.PL, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsvs( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.VS, Rd , Rm , shift, imm5 ); } 
    public ARMCortexMThumb $movsvc( final Reg Rd , final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_movs_c_impl( Cond.VC, Rd , Rm , shift, imm5 ); }

    // [PI] MOV <Rd>, <Rm>, <shift_operand>
    public ARMCortexMThumb $_mov_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError        
    { return __itcd__( "mov", cond, ()->{ return $mov( Rd , Rm , shift ); }, Rd , Rm , shift ); } 
    
    public ARMCortexMThumb $moveq( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movzr( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movne( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movnz( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movhi( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.HI, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movhs( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.HS, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movlo( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.LO, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movls( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.LS, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movgt( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.GT, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movge( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.GE, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movlt( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.LT, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movle( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.LE, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movcs( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.CS, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movcc( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.CC, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movmi( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.MI, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movpl( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.PL, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movvs( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.VS, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movvc( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mov_c_impl( Cond.VC, Rd , Rm , shift ); }

    // [PI] MOVS <Rd>, <Rm>, <shift_operand>
    public ARMCortexMThumb $_movs_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError        
    { return __itcd__( "movs", cond, ()->{ return $movs( Rd , Rm , shift ); }, Rd , Rm , shift ); } 
    
    public ARMCortexMThumb $movseq( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movszr( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsne( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsnz( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movshi( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.HI, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movshs( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.HS, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movslo( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.LO, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsls( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.LS, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsgt( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.GT, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsge( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.GE, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movslt( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.LT, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsle( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.LE, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movscs( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.CS, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movscc( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.CC, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsmi( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.MI, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movspl( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.PL, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsvs( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.VS, Rd , Rm , shift ); } 
    public ARMCortexMThumb $movsvc( final Reg Rd , final Reg Rm, final Shift shift ) throws JXMAsmError { return $_movs_c_impl( Cond.VC, Rd , Rm , shift ); }

    // [PI] MOV <Rd>, <Rm>, <shift_operand> <Rs>
    public ARMCortexMThumb $_mov_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError        
    { return __itcd__( "mov", cond, ()->{ return $mov( Rd , Rm , shift, Rs ); }, Rd , Rm , shift, Rs ); } 
    
    public ARMCortexMThumb $moveq( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movzr( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movne( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movnz( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movhi( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.HI, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movhs( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.HS, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movlo( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.LO, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movls( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.LS, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movgt( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.GT, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movge( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.GE, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movlt( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.LT, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movle( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.LE, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movcs( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.CS, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movcc( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.CC, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movmi( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.MI, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movpl( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.PL, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movvs( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.VS, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movvc( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.VC, Rd , Rm , shift, Rs ); }

    // [PI] MOVS <Rd>, <Rm>, <shift_operand> <Rs>
    public ARMCortexMThumb $_movs_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError        
    { return __itcd__( "movs", cond, ()->{ return $movs( Rd , Rm , shift, Rs ); }, Rd , Rm , shift, Rs ); } 
    
    public ARMCortexMThumb $movseq( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movszr( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsne( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsnz( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movshi( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.HI, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movshs( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.HS, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movslo( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.LO, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsls( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.LS, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsgt( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.GT, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsge( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.GE, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movslt( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.LT, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsle( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.LE, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movscs( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.CS, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movscc( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.CC, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsmi( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.MI, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movspl( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.PL, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsvs( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.VS, Rd , Rm , shift, Rs ); } 
    public ARMCortexMThumb $movsvc( final Reg Rd , final Reg Rm, final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.VC, Rd , Rm , shift, Rs ); }

    // [PI] MOV <Rdm>, <shift_operand> <Rs>
    public ARMCortexMThumb $_mov_c_impl( final Cond cond, final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError        
    { return __itcd__( "mov", cond, ()->{ return $mov( Rdm, Rdm, shift, Rs ); }, Rdm, Rdm, shift, Rs ); } 
    
    public ARMCortexMThumb $moveq( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movzr( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.EQ, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movne( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movnz( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.NE, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movhi( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.HI, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movhs( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.HS, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movlo( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.LO, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movls( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.LS, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movgt( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.GT, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movge( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.GE, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movlt( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.LT, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movle( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.LE, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movcs( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.CS, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movcc( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.CC, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movmi( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.MI, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movpl( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.PL, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movvs( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.VS, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movvc( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_mov_c_impl( Cond.VC, Rdm, Rdm, shift, Rs ); }

    // [PI] MOVS <Rdm>, <shift_operand> <Rs>
    public ARMCortexMThumb $_movs_c_impl( final Cond cond, final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError        
    { return __itcd__( "movs", cond, ()->{ return $movs( Rdm, Rdm, shift, Rs ); }, Rdm, Rdm, shift, Rs ); } 
    
    public ARMCortexMThumb $movseq( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movszr( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.EQ, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsne( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsnz( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.NE, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movshi( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.HI, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movshs( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.HS, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movslo( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.LO, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsls( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.LS, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsgt( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.GT, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsge( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.GE, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movslt( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.LT, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsle( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.LE, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movscs( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.CS, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movscc( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.CC, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsmi( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.MI, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movspl( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.PL, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsvs( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.VS, Rdm, Rdm, shift, Rs ); } 
    public ARMCortexMThumb $movsvc( final Reg Rdm,               final Shift shift, final Reg Rs ) throws JXMAsmError { return $_movs_c_impl( Cond.VC, Rdm, Rdm, shift, Rs ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MOVT <Rd>, <imm16>
    public ARMCortexMThumb $_movt_c_impl( final Cond cond, final Reg Rd, final int imm16 ) throws JXMAsmError        
    { return __itcd__( "movt", cond, ()->{ return $movt( Rd, imm16 ); }, Rd, imm16 ); } 
    
    public ARMCortexMThumb $movteq( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.EQ, Rd, imm16 ); } 
    public ARMCortexMThumb $movtzr( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.EQ, Rd, imm16 ); } 
    public ARMCortexMThumb $movtne( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.NE, Rd, imm16 ); } 
    public ARMCortexMThumb $movtnz( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.NE, Rd, imm16 ); } 
    public ARMCortexMThumb $movthi( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.HI, Rd, imm16 ); } 
    public ARMCortexMThumb $movths( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.HS, Rd, imm16 ); } 
    public ARMCortexMThumb $movtlo( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.LO, Rd, imm16 ); } 
    public ARMCortexMThumb $movtls( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.LS, Rd, imm16 ); } 
    public ARMCortexMThumb $movtgt( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.GT, Rd, imm16 ); } 
    public ARMCortexMThumb $movtge( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.GE, Rd, imm16 ); } 
    public ARMCortexMThumb $movtlt( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.LT, Rd, imm16 ); } 
    public ARMCortexMThumb $movtle( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.LE, Rd, imm16 ); } 
    public ARMCortexMThumb $movtcs( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.CS, Rd, imm16 ); } 
    public ARMCortexMThumb $movtcc( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.CC, Rd, imm16 ); } 
    public ARMCortexMThumb $movtmi( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.MI, Rd, imm16 ); } 
    public ARMCortexMThumb $movtpl( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.PL, Rd, imm16 ); } 
    public ARMCortexMThumb $movtvs( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.VS, Rd, imm16 ); } 
    public ARMCortexMThumb $movtvc( final Reg Rd, final int imm16 ) throws JXMAsmError { return $_movt_c_impl( Cond.VC, Rd, imm16 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MRS <Rd>, <SYSm>
    public ARMCortexMThumb $_mrs_c_impl( final Cond cond, final Reg Rd, final SYSm sysm ) throws JXMAsmError        
    { return __itcd__( "mrs", cond, ()->{ return $mrs( Rd, sysm ); }, Rd, sysm ); } 
    
    public ARMCortexMThumb $mrseq( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.EQ, Rd, sysm ); } 
    public ARMCortexMThumb $mrszr( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.EQ, Rd, sysm ); } 
    public ARMCortexMThumb $mrsne( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.NE, Rd, sysm ); } 
    public ARMCortexMThumb $mrsnz( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.NE, Rd, sysm ); } 
    public ARMCortexMThumb $mrshi( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.HI, Rd, sysm ); } 
    public ARMCortexMThumb $mrshs( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.HS, Rd, sysm ); } 
    public ARMCortexMThumb $mrslo( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.LO, Rd, sysm ); } 
    public ARMCortexMThumb $mrsls( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.LS, Rd, sysm ); } 
    public ARMCortexMThumb $mrsgt( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.GT, Rd, sysm ); } 
    public ARMCortexMThumb $mrsge( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.GE, Rd, sysm ); } 
    public ARMCortexMThumb $mrslt( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.LT, Rd, sysm ); } 
    public ARMCortexMThumb $mrsle( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.LE, Rd, sysm ); } 
    public ARMCortexMThumb $mrscs( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.CS, Rd, sysm ); } 
    public ARMCortexMThumb $mrscc( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.CC, Rd, sysm ); } 
    public ARMCortexMThumb $mrsmi( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.MI, Rd, sysm ); } 
    public ARMCortexMThumb $mrspl( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.PL, Rd, sysm ); } 
    public ARMCortexMThumb $mrsvs( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.VS, Rd, sysm ); } 
    public ARMCortexMThumb $mrsvc( final Reg Rd, final SYSm sysm ) throws JXMAsmError { return $_mrs_c_impl( Cond.VC, Rd, sysm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MSR <SYSm>, <Rn>
    public ARMCortexMThumb $_msr_c_impl( final Cond cond, final SYSm sysm, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "msr", cond, ()->{ return $msr( sysm, Rn ); }, sysm, Rn ); } 
    
    public ARMCortexMThumb $msreq( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.EQ, sysm, Rn ); } 
    public ARMCortexMThumb $msrzr( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.EQ, sysm, Rn ); } 
    public ARMCortexMThumb $msrne( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.NE, sysm, Rn ); } 
    public ARMCortexMThumb $msrnz( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.NE, sysm, Rn ); } 
    public ARMCortexMThumb $msrhi( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.HI, sysm, Rn ); } 
    public ARMCortexMThumb $msrhs( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.HS, sysm, Rn ); } 
    public ARMCortexMThumb $msrlo( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.LO, sysm, Rn ); } 
    public ARMCortexMThumb $msrls( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.LS, sysm, Rn ); } 
    public ARMCortexMThumb $msrgt( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.GT, sysm, Rn ); } 
    public ARMCortexMThumb $msrge( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.GE, sysm, Rn ); } 
    public ARMCortexMThumb $msrlt( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.LT, sysm, Rn ); } 
    public ARMCortexMThumb $msrle( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.LE, sysm, Rn ); } 
    public ARMCortexMThumb $msrcs( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.CS, sysm, Rn ); } 
    public ARMCortexMThumb $msrcc( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.CC, sysm, Rn ); } 
    public ARMCortexMThumb $msrmi( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.MI, sysm, Rn ); } 
    public ARMCortexMThumb $msrpl( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.PL, sysm, Rn ); } 
    public ARMCortexMThumb $msrvs( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.VS, sysm, Rn ); } 
    public ARMCortexMThumb $msrvc( final SYSm sysm, final Reg Rn ) throws JXMAsmError { return $_msr_c_impl( Cond.VC, sysm, Rn ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] MULS <Rdm>, <Rn>, <Rdm>
    public ARMCortexMThumb $_mul_c_impl( final Cond cond, final Reg Rdm, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "mul", cond, ()->{ return $muls( Rdm, Rn ); }, Rdm, Rn ); } 
    
    public ARMCortexMThumb $muleq( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.EQ, Rdm, Rn ); } 
    public ARMCortexMThumb $mulzr( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.EQ, Rdm, Rn ); } 
    public ARMCortexMThumb $mulne( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.NE, Rdm, Rn ); } 
    public ARMCortexMThumb $mulnz( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.NE, Rdm, Rn ); } 
    public ARMCortexMThumb $mulhi( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.HI, Rdm, Rn ); } 
    public ARMCortexMThumb $mulhs( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.HS, Rdm, Rn ); } 
    public ARMCortexMThumb $mullo( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.LO, Rdm, Rn ); } 
    public ARMCortexMThumb $mulls( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.LS, Rdm, Rn ); } 
    public ARMCortexMThumb $mulgt( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.GT, Rdm, Rn ); } 
    public ARMCortexMThumb $mulge( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.GE, Rdm, Rn ); } 
    public ARMCortexMThumb $mullt( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.LT, Rdm, Rn ); } 
    public ARMCortexMThumb $mulle( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.LE, Rdm, Rn ); } 
    public ARMCortexMThumb $mulcs( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.CS, Rdm, Rn ); } 
    public ARMCortexMThumb $mulcc( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.CC, Rdm, Rn ); } 
    public ARMCortexMThumb $mulmi( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.MI, Rdm, Rn ); } 
    public ARMCortexMThumb $mulpl( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.PL, Rdm, Rn ); } 
    public ARMCortexMThumb $mulvs( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.VS, Rdm, Rn ); } 
    public ARMCortexMThumb $mulvc( final Reg Rdm, final Reg Rn ) throws JXMAsmError { return $_mul_c_impl( Cond.VC, Rdm, Rn ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb _$mvnx_w_impl_f(final Reg Rd, final long const_) throws JXMAsmError
    { return _$mvnx_w_impl(false, Rd, const_); }

    private ARMCortexMThumb _$mvnx_w_impl_t(final Reg Rd, final long const_) throws JXMAsmError
    { return _$mvnx_w_impl(true , Rd, const_); }

    // [T1] MVN.W <Rd>, #<const>
    public ARMCortexMThumb $_mvn_c_impl( final Cond cond, final Reg Rd, final long const_ ) throws JXMAsmError        
    { return __itcd__( "mvn", cond, ()->{ return _$mvnx_w_impl_f( Rd, const_ ); }, Rd, const_ ); } 
    
    public ARMCortexMThumb $mvneq( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.EQ, Rd, const_ ); } 
    public ARMCortexMThumb $mvnzr( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.EQ, Rd, const_ ); } 
    public ARMCortexMThumb $mvnne( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.NE, Rd, const_ ); } 
    public ARMCortexMThumb $mvnnz( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.NE, Rd, const_ ); } 
    public ARMCortexMThumb $mvnhi( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.HI, Rd, const_ ); } 
    public ARMCortexMThumb $mvnhs( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.HS, Rd, const_ ); } 
    public ARMCortexMThumb $mvnlo( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.LO, Rd, const_ ); } 
    public ARMCortexMThumb $mvnls( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.LS, Rd, const_ ); } 
    public ARMCortexMThumb $mvngt( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.GT, Rd, const_ ); } 
    public ARMCortexMThumb $mvnge( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.GE, Rd, const_ ); } 
    public ARMCortexMThumb $mvnlt( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.LT, Rd, const_ ); } 
    public ARMCortexMThumb $mvnle( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.LE, Rd, const_ ); } 
    public ARMCortexMThumb $mvncs( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.CS, Rd, const_ ); } 
    public ARMCortexMThumb $mvncc( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.CC, Rd, const_ ); } 
    public ARMCortexMThumb $mvnmi( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.MI, Rd, const_ ); } 
    public ARMCortexMThumb $mvnpl( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.PL, Rd, const_ ); } 
    public ARMCortexMThumb $mvnvs( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.VS, Rd, const_ ); } 
    public ARMCortexMThumb $mvnvc( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvn_c_impl( Cond.VC, Rd, const_ ); }

    // [T1] MVNS.W <Rd>, #<const>
    public ARMCortexMThumb $_mvns_c_impl( final Cond cond, final Reg Rd, final long const_ ) throws JXMAsmError        
    { return __itcd__( "mvns", cond, ()->{ return _$mvnx_w_impl_t( Rd, const_ ); }, Rd, const_ ); } 
    
    public ARMCortexMThumb $mvnseq( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.EQ, Rd, const_ ); } 
    public ARMCortexMThumb $mvnszr( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.EQ, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsne( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.NE, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsnz( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.NE, Rd, const_ ); } 
    public ARMCortexMThumb $mvnshi( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.HI, Rd, const_ ); } 
    public ARMCortexMThumb $mvnshs( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.HS, Rd, const_ ); } 
    public ARMCortexMThumb $mvnslo( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.LO, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsls( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.LS, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsgt( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.GT, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsge( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.GE, Rd, const_ ); } 
    public ARMCortexMThumb $mvnslt( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.LT, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsle( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.LE, Rd, const_ ); } 
    public ARMCortexMThumb $mvnscs( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.CS, Rd, const_ ); } 
    public ARMCortexMThumb $mvnscc( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.CC, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsmi( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.MI, Rd, const_ ); } 
    public ARMCortexMThumb $mvnspl( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.PL, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsvs( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.VS, Rd, const_ ); } 
    public ARMCortexMThumb $mvnsvc( final Reg Rd, final long const_ ) throws JXMAsmError { return $_mvns_c_impl( Cond.VC, Rd, const_ ); }

    private ARMCortexMThumb _$mvnx_w_shift_impl_f(final Reg Rd, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    { return _$mvnx_w_shift_impl(false, Rd, Rm, shift, imm5); }

    private ARMCortexMThumb _$mvnx_w_shift_impl_t(final Reg Rd, final Reg Rm, final Shift shift, final int imm5) throws JXMAsmError
    { return _$mvnx_w_shift_impl(true , Rd, Rm, shift, imm5); }

    // [T2] MVN.W <Rd>, <Rm>
    public ARMCortexMThumb $_mvn_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "mvn", cond, ()->{ return _$mvnx_w_shift_impl_f( Rd , Rm, Shift.LSL, 0 ); }, Rd , Rm, Shift.LSL, 0 ); } 
    
    public ARMCortexMThumb $mvneq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.EQ, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.EQ, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.NE, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.NE, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnhi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.HI, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnhs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.HS, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnlo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.LO, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.LS, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvngt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.GT, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.GE, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnlt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.LT, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnle( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.LE, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvncs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.CS, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvncc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.CC, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.MI, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnpl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.PL, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.VS, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvn_c_impl( Cond.VC, Rd , Rm, Shift.LSL, 0 ); }

    // [T2] MVNS.W <Rd>, <Rm>
    public ARMCortexMThumb $_mvns_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "mvns", cond, ()->{ return _$mvnx_w_shift_impl_t( Rd , Rm, Shift.LSL, 0 ); }, Rd , Rm, Shift.LSL, 0 ); } 
    
    public ARMCortexMThumb $mvnseq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.EQ, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnszr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.EQ, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.NE, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.NE, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnshi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.HI, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnshs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.HS, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnslo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.LO, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.LS, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsgt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.GT, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.GE, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnslt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.LT, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsle( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.LE, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnscs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.CS, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnscc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.CC, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.MI, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnspl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.PL, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.VS, Rd , Rm, Shift.LSL, 0 ); } 
    public ARMCortexMThumb $mvnsvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_mvns_c_impl( Cond.VC, Rd , Rm, Shift.LSL, 0 ); }

    // [T2] MVN.W <Rd>, <Rm>, <shift_operand> #<imm5>
    public ARMCortexMThumb $_mvn_c_impl( final Cond cond, final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "mvn", cond, ()->{ return _$mvnx_w_shift_impl_f( Rd , Rm, shift, imm5 ); }, Rd , Rm, shift, imm5 ); } 
    
    public ARMCortexMThumb $mvneq( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.EQ, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnzr( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.EQ, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnne( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.NE, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnnz( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.NE, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnhi( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.HI, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnhs( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.HS, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnlo( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.LO, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnls( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.LS, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvngt( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.GT, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnge( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.GE, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnlt( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.LT, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnle( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.LE, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvncs( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.CS, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvncc( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.CC, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnmi( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.MI, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnpl( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.PL, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnvs( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.VS, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnvc( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvn_c_impl( Cond.VC, Rd , Rm, shift, imm5 ); }

    // [T2] MVNS.W <Rd>, <Rm>, <shift_operand> #<imm5>
    public ARMCortexMThumb $_mvns_c_impl( final Cond cond, final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "mvns", cond, ()->{ return _$mvnx_w_shift_impl_t( Rd , Rm, shift, imm5 ); }, Rd , Rm, shift, imm5 ); } 
    
    public ARMCortexMThumb $mvnseq( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.EQ, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnszr( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.EQ, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsne( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.NE, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsnz( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.NE, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnshi( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.HI, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnshs( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.HS, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnslo( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.LO, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsls( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.LS, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsgt( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.GT, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsge( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.GE, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnslt( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.LT, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsle( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.LE, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnscs( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.CS, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnscc( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.CC, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsmi( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.MI, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnspl( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.PL, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsvs( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.VS, Rd , Rm, shift, imm5 ); } 
    public ARMCortexMThumb $mvnsvc( final Reg Rd, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_mvns_c_impl( Cond.VC, Rd , Rm, shift, imm5 ); }

    // [T2] MVN.W <Rd>, <Rm>, <shift_operand>
    public ARMCortexMThumb $_mvn_c_impl( final Cond cond, final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError        
    { return __itcd__( "mvn", cond, ()->{ return _$mvnx_w_shift_impl_f( Rd , Rm, shift, -1 ); }, Rd , Rm, shift, -1 ); } 
    
    public ARMCortexMThumb $mvneq( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.EQ, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnzr( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.EQ, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnne( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.NE, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnnz( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.NE, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnhi( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.HI, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnhs( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.HS, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnlo( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.LO, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnls( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.LS, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvngt( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.GT, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnge( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.GE, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnlt( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.LT, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnle( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.LE, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvncs( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.CS, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvncc( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.CC, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnmi( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.MI, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnpl( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.PL, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnvs( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.VS, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnvc( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvn_c_impl( Cond.VC, Rd , Rm, shift, -1 ); }

    // [T2] MVNS.W <Rd>, <Rm>, <shift_operand>
    public ARMCortexMThumb $_mvns_c_impl( final Cond cond, final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError        
    { return __itcd__( "mvns", cond, ()->{ return _$mvnx_w_shift_impl_t( Rd , Rm, shift, -1 ); }, Rd , Rm, shift, -1 ); } 
    
    public ARMCortexMThumb $mvnseq( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.EQ, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnszr( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.EQ, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsne( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.NE, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsnz( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.NE, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnshi( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.HI, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnshs( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.HS, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnslo( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.LO, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsls( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.LS, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsgt( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.GT, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsge( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.GE, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnslt( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.LT, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsle( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.LE, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnscs( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.CS, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnscc( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.CC, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsmi( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.MI, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnspl( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.PL, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsvs( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.VS, Rd , Rm, shift, -1 ); } 
    public ARMCortexMThumb $mvnsvc( final Reg Rd, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_mvns_c_impl( Cond.VC, Rd , Rm, shift, -1 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [PI] RSBS <Rd>, <Rm>, #0
    public ARMCortexMThumb $_neg_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "neg", cond, ()->{ return $negs( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $negeq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $negzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $negne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $negnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $neghi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $neghs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $neglo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $negls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $neggt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $negge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $neglt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $negle( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $negcs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $negcc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $negmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $negpl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $negvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $negvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_neg_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] ORRS <Rdn>, <Rm>
    public ARMCortexMThumb $_orr_c_impl( final Cond cond, final Reg Rdn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "orr", cond, ()->{ return $orrs( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $orreq( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $orrzr( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $orrne( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $orrnz( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $orrhi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $orrhs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $orrlo( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $orrls( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $orrgt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $orrge( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $orrlt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $orrle( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $orrcs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $orrcc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $orrmi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $orrpl( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $orrvs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $orrvc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_orr_c_impl( Cond.VC, Rdn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] POP   <registers>
    // [T2] POP.W <registers>
    // [T3] POP.W <Rt>
    public ARMCortexMThumb $_pop_c_impl( final Cond cond, final Reg... Regs ) throws JXMAsmError                   
    { return __itcd__( "pop", cond, ()->{ return $pop( Regs ); }, (Object[]) Regs ); } 
    
    public ARMCortexMThumb $popeq( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.EQ, Regs ); } 
    public ARMCortexMThumb $popzr( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.EQ, Regs ); } 
    public ARMCortexMThumb $popne( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.NE, Regs ); } 
    public ARMCortexMThumb $popnz( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.NE, Regs ); } 
    public ARMCortexMThumb $pophi( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.HI, Regs ); } 
    public ARMCortexMThumb $pophs( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.HS, Regs ); } 
    public ARMCortexMThumb $poplo( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.LO, Regs ); } 
    public ARMCortexMThumb $popls( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.LS, Regs ); } 
    public ARMCortexMThumb $popgt( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.GT, Regs ); } 
    public ARMCortexMThumb $popge( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.GE, Regs ); } 
    public ARMCortexMThumb $poplt( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.LT, Regs ); } 
    public ARMCortexMThumb $pople( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.LE, Regs ); } 
    public ARMCortexMThumb $popcs( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.CS, Regs ); } 
    public ARMCortexMThumb $popcc( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.CC, Regs ); } 
    public ARMCortexMThumb $popmi( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.MI, Regs ); } 
    public ARMCortexMThumb $poppl( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.PL, Regs ); } 
    public ARMCortexMThumb $popvs( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.VS, Regs ); } 
    public ARMCortexMThumb $popvc( final Reg... Regs ) throws JXMAsmError { return $_pop_c_impl( Cond.VC, Regs ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] PUSH   <registers>
    // [T2] PUSH.W <registers>
    // [T3] PUSH.W <Rt>
    public ARMCortexMThumb $_push_c_impl( final Cond cond, final Reg... Regs ) throws JXMAsmError                   
    { return __itcd__( "push", cond, ()->{ return $push( Regs ); }, (Object[]) Regs ); } 
    
    public ARMCortexMThumb $pusheq( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.EQ, Regs ); } 
    public ARMCortexMThumb $pushzr( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.EQ, Regs ); } 
    public ARMCortexMThumb $pushne( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.NE, Regs ); } 
    public ARMCortexMThumb $pushnz( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.NE, Regs ); } 
    public ARMCortexMThumb $pushhi( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.HI, Regs ); } 
    public ARMCortexMThumb $pushhs( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.HS, Regs ); } 
    public ARMCortexMThumb $pushlo( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.LO, Regs ); } 
    public ARMCortexMThumb $pushls( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.LS, Regs ); } 
    public ARMCortexMThumb $pushgt( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.GT, Regs ); } 
    public ARMCortexMThumb $pushge( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.GE, Regs ); } 
    public ARMCortexMThumb $pushlt( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.LT, Regs ); } 
    public ARMCortexMThumb $pushle( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.LE, Regs ); } 
    public ARMCortexMThumb $pushcs( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.CS, Regs ); } 
    public ARMCortexMThumb $pushcc( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.CC, Regs ); } 
    public ARMCortexMThumb $pushmi( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.MI, Regs ); } 
    public ARMCortexMThumb $pushpl( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.PL, Regs ); } 
    public ARMCortexMThumb $pushvs( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.VS, Regs ); } 
    public ARMCortexMThumb $pushvc( final Reg... Regs ) throws JXMAsmError { return $_push_c_impl( Cond.VC, Regs ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] REV <Rd>, <Rm>
    public ARMCortexMThumb $_rev_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "rev", cond, ()->{ return $rev( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $reveq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $revzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $revne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $revnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $revhi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $revhs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $revlo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $revls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $revgt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $revge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $revlt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $revle( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $revcs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $revcc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $revmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $revpl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $revvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $revvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] REV16 <Rd>, <Rm>
    public ARMCortexMThumb $_rev16_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "rev16", cond, ()->{ return $rev16( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $rev16eq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $rev16zr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $rev16ne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $rev16nz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $rev16hi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $rev16hs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $rev16lo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $rev16ls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $rev16gt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $rev16ge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $rev16lt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $rev16le( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $rev16cs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $rev16cc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $rev16mi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $rev16pl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $rev16vs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $rev16vc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_rev16_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] REVSH <Rd>, <Rm>
    public ARMCortexMThumb $_revsh_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "revsh", cond, ()->{ return $revsh( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $revsheq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $revshzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $revshne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $revshnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $revshhi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $revshhs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $revshlo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $revshls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $revshgt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $revshge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $revshlt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $revshle( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $revshcs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $revshcc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $revshmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $revshpl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $revshvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $revshvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_revsh_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb _$rors_w_impl_f(final Reg Rd , final Reg Rm, final int imm5) throws JXMAsmError
    { return  _$rors_w_impl(false, Rd , Rm , imm5); }

    private ARMCortexMThumb _$rors_w_impl_t(final Reg Rd , final Reg Rm, final int imm5) throws JXMAsmError
    { return  _$rors_w_impl(true , Rd , Rm , imm5); }

    private ARMCortexMThumb _$rors_w_impl_f(final Reg Rd , final Reg Rn, final Reg Rm  ) throws JXMAsmError
    { return  _$rors_w_impl(false, Rd , Rn , Rm  ); }

    private ARMCortexMThumb _$rors_w_impl_t(final Reg Rd , final Reg Rn, final Reg Rm  ) throws JXMAsmError
    { return  _$rors_w_impl(true , Rd , Rn , Rm  ); }

    private ARMCortexMThumb _$rors_w_impl_f(final Reg Rdn,               final Reg Rm  ) throws JXMAsmError
    { return  _$rors_w_impl(false, Rdn, Rdn, Rm  ); }

    private ARMCortexMThumb _$rors_w_impl_t(final Reg Rdn,               final Reg Rm  ) throws JXMAsmError
    { return  _$rors_w_impl(true , Rdn, Rdn, Rm  ); }

    // [T2] ROR.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $_ror_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "ror", cond, ()->{ return _$rors_w_impl_f( Rd , Rm, imm5 ); }, Rd , Rm, imm5 ); } 
    
    public ARMCortexMThumb $roreq( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorzr( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorne( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rornz( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorhi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.HI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorhs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.HS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorlo( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.LO, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorls( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.LS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorgt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.GT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorge( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.GE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorlt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.LT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorle( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.LE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorcs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.CS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorcc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.CC, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rormi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.MI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorpl( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.PL, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorvs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.VS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorvc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_ror_c_impl( Cond.VC, Rd , Rm, imm5 ); }

    // [T2] RORS.W <Rd>, <Rm>, #<imm5>
    public ARMCortexMThumb $_rors_c_impl( final Cond cond, final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "rors", cond, ()->{ return _$rors_w_impl_t( Rd , Rm, imm5 ); }, Rd , Rm, imm5 ); } 
    
    public ARMCortexMThumb $rorseq( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorszr( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.EQ, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsne( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsnz( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.NE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorshi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.HI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorshs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.HS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorslo( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.LO, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsls( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.LS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsgt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.GT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsge( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.GE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorslt( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.LT, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsle( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.LE, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorscs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.CS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorscc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.CC, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsmi( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.MI, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorspl( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.PL, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsvs( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.VS, Rd , Rm, imm5 ); } 
    public ARMCortexMThumb $rorsvc( final Reg Rd , final Reg Rm, final int imm5 ) throws JXMAsmError { return $_rors_c_impl( Cond.VC, Rd , Rm, imm5 ); }

    // [T2] ROR.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_ror_c_impl( final Cond cond, final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "ror", cond, ()->{ return _$rors_w_impl_f( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $roreq( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorzr( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorne( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rornz( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorhi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorhs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorlo( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorls( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorgt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorge( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorlt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorle( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorcs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorcc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rormi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorpl( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorvs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorvc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] RORS.W <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_rors_c_impl( final Cond cond, final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "rors", cond, ()->{ return _$rors_w_impl_t( Rd , Rn, Rm ); }, Rd , Rn, Rm ); } 
    
    public ARMCortexMThumb $rorseq( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorszr( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.EQ, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsne( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsnz( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.NE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorshi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.HI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorshs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.HS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorslo( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.LO, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsls( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.LS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsgt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.GT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsge( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.GE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorslt( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.LT, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsle( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.LE, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorscs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.CS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorscc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.CC, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsmi( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.MI, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorspl( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.PL, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsvs( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.VS, Rd , Rn, Rm ); } 
    public ARMCortexMThumb $rorsvc( final Reg Rd , final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.VC, Rd , Rn, Rm ); }

    // [T2] ROR.W <Rdn>, <Rm>
    public ARMCortexMThumb $_ror_c_impl( final Cond cond, final Reg Rdn,               final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "ror", cond, ()->{ return _$rors_w_impl_f( Rdn,     Rm ); }, Rdn,     Rm ); } 
    
    public ARMCortexMThumb $roreq( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorzr( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorne( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $rornz( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorhi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.HI, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorhs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.HS, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorlo( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.LO, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorls( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.LS, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorgt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.GT, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorge( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.GE, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorlt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.LT, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorle( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.LE, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorcs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.CS, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorcc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.CC, Rdn,     Rm ); } 
    public ARMCortexMThumb $rormi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.MI, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorpl( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.PL, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorvs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.VS, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorvc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_ror_c_impl( Cond.VC, Rdn,     Rm ); }

    // [T2] RORS.W <Rdn>, <Rm>
    public ARMCortexMThumb $_rors_c_impl( final Cond cond, final Reg Rdn,               final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "rors", cond, ()->{ return _$rors_w_impl_t( Rdn,     Rm ); }, Rdn,     Rm ); } 
    
    public ARMCortexMThumb $rorseq( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorszr( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.EQ, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsne( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsnz( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.NE, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorshi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.HI, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorshs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.HS, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorslo( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.LO, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsls( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.LS, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsgt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.GT, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsge( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.GE, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorslt( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.LT, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsle( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.LE, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorscs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.CS, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorscc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.CC, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsmi( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.MI, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorspl( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.PL, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsvs( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.VS, Rdn,     Rm ); } 
    public ARMCortexMThumb $rorsvc( final Reg Rdn,               final Reg Rm ) throws JXMAsmError { return $_rors_c_impl( Cond.VC, Rdn,     Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ARMCortexMThumb _$rrxs_w_impl_f(final Reg Rd , final Reg Rm) throws JXMAsmError
    { return _$rrxs_w_impl(false, Rd , Rm); }

    private ARMCortexMThumb _$rrxs_w_impl_t(final Reg Rd , final Reg Rm) throws JXMAsmError
    { return _$rrxs_w_impl(true , Rd , Rm); }

    // [T1] RRX.W <Rd>, <Rm>
    public ARMCortexMThumb $_rrx_c_impl( final Cond cond, final Reg Rd , final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "rrx", cond, ()->{ return _$rrxs_w_impl_f( Rd , Rm ); }, Rd , Rm ); } 
    
    public ARMCortexMThumb $rrxeq( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.EQ, Rd , Rm ); } 
    public ARMCortexMThumb $rrxzr( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.EQ, Rd , Rm ); } 
    public ARMCortexMThumb $rrxne( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.NE, Rd , Rm ); } 
    public ARMCortexMThumb $rrxnz( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.NE, Rd , Rm ); } 
    public ARMCortexMThumb $rrxhi( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.HI, Rd , Rm ); } 
    public ARMCortexMThumb $rrxhs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.HS, Rd , Rm ); } 
    public ARMCortexMThumb $rrxlo( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.LO, Rd , Rm ); } 
    public ARMCortexMThumb $rrxls( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.LS, Rd , Rm ); } 
    public ARMCortexMThumb $rrxgt( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.GT, Rd , Rm ); } 
    public ARMCortexMThumb $rrxge( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.GE, Rd , Rm ); } 
    public ARMCortexMThumb $rrxlt( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.LT, Rd , Rm ); } 
    public ARMCortexMThumb $rrxle( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.LE, Rd , Rm ); } 
    public ARMCortexMThumb $rrxcs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.CS, Rd , Rm ); } 
    public ARMCortexMThumb $rrxcc( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.CC, Rd , Rm ); } 
    public ARMCortexMThumb $rrxmi( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.MI, Rd , Rm ); } 
    public ARMCortexMThumb $rrxpl( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.PL, Rd , Rm ); } 
    public ARMCortexMThumb $rrxvs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.VS, Rd , Rm ); } 
    public ARMCortexMThumb $rrxvc( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrx_c_impl( Cond.VC, Rd , Rm ); }

    // [T1] RRXS.W <Rd>, <Rm>
    public ARMCortexMThumb $_rrxs_c_impl( final Cond cond, final Reg Rd , final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "rrxs", cond, ()->{ return _$rrxs_w_impl_t( Rd , Rm ); }, Rd , Rm ); } 
    
    public ARMCortexMThumb $rrxseq( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.EQ, Rd , Rm ); } 
    public ARMCortexMThumb $rrxszr( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.EQ, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsne( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.NE, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsnz( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.NE, Rd , Rm ); } 
    public ARMCortexMThumb $rrxshi( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.HI, Rd , Rm ); } 
    public ARMCortexMThumb $rrxshs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.HS, Rd , Rm ); } 
    public ARMCortexMThumb $rrxslo( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.LO, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsls( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.LS, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsgt( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.GT, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsge( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.GE, Rd , Rm ); } 
    public ARMCortexMThumb $rrxslt( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.LT, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsle( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.LE, Rd , Rm ); } 
    public ARMCortexMThumb $rrxscs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.CS, Rd , Rm ); } 
    public ARMCortexMThumb $rrxscc( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.CC, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsmi( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.MI, Rd , Rm ); } 
    public ARMCortexMThumb $rrxspl( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.PL, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsvs( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.VS, Rd , Rm ); } 
    public ARMCortexMThumb $rrxsvc( final Reg Rd , final Reg Rm ) throws JXMAsmError { return $_rrxs_c_impl( Cond.VC, Rd , Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] RSBS <Rd>, <Rm>, #<imm12>
    public ARMCortexMThumb $_rsbs_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError        
    { return __itcd__( "rsb", cond, ()->{ return $rsbs( Rd, Rn, imm12 ); }, Rd, Rn, imm12 ); } 
    
    public ARMCortexMThumb $rsbeq( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.EQ, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbzr( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.EQ, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbne( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.NE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbnz( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.NE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbhi( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.HI, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbhs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.HS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsblo( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.LO, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbls( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.LS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbgt( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.GT, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbge( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.GE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsblt( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.LT, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsble( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.LE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbcs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.CS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbcc( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.CC, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbmi( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.MI, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbpl( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.PL, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbvs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.VS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $rsbvc( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_rsbs_c_impl( Cond.VC, Rd, Rn, imm12 ); }

    // [T1] RSBS <Rd>, <Rm>, #0
    public ARMCortexMThumb $_rsbs0_c_impl( final Cond cond, final Reg Rd, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "rsb0", cond, ()->{ return $rsbs0( Rd, Rn ); }, Rd, Rn ); } 
    
    public ARMCortexMThumb $rsb0eq( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.EQ, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0zr( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.EQ, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0ne( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.NE, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0nz( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.NE, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0hi( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.HI, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0hs( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.HS, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0lo( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.LO, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0ls( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.LS, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0gt( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.GT, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0ge( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.GE, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0lt( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.LT, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0le( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.LE, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0cs( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.CS, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0cc( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.CC, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0mi( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.MI, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0pl( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.PL, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0vs( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.VS, Rd, Rn ); } 
    public ARMCortexMThumb $rsb0vc( final Reg Rd, final Reg Rn ) throws JXMAsmError { return $_rsbs0_c_impl( Cond.VC, Rd, Rn ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SBCS <Rdn>, <Rm>
    public ARMCortexMThumb $_sbc_c_impl( final Cond cond, final Reg Rdn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "sbc", cond, ()->{ return $sbcs( Rdn, Rm ); }, Rdn, Rm ); } 
    
    public ARMCortexMThumb $sbceq( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $sbczr( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.EQ, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcne( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcnz( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.NE, Rdn, Rm ); } 
    public ARMCortexMThumb $sbchi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.HI, Rdn, Rm ); } 
    public ARMCortexMThumb $sbchs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.HS, Rdn, Rm ); } 
    public ARMCortexMThumb $sbclo( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.LO, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcls( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.LS, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcgt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.GT, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcge( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.GE, Rdn, Rm ); } 
    public ARMCortexMThumb $sbclt( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.LT, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcle( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.LE, Rdn, Rm ); } 
    public ARMCortexMThumb $sbccs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.CS, Rdn, Rm ); } 
    public ARMCortexMThumb $sbccc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.CC, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcmi( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.MI, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcpl( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.PL, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcvs( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.VS, Rdn, Rm ); } 
    public ARMCortexMThumb $sbcvc( final Reg Rdn, final Reg Rm ) throws JXMAsmError { return $_sbc_c_impl( Cond.VC, Rdn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SDIV <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_sdiv_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "sdiv", cond, ()->{ return $sdiv( Rd, Rn, Rm ); }, Rd, Rn, Rm ); } 
    
    public ARMCortexMThumb $sdiveq( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.EQ, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivzr( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.EQ, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivne( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.NE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivnz( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.NE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivhi( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.HI, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivhs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.HS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivlo( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.LO, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivls( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.LS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivgt( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.GT, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivge( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.GE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivlt( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.LT, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivle( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.LE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivcs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.CS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivcc( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.CC, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivmi( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.MI, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivpl( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.PL, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivvs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.VS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sdivvc( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sdiv_c_impl( Cond.VC, Rd, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SEV
    public ARMCortexMThumb $_sev_c_impl( final Cond cond ) throws JXMAsmError 
    { return __itcd__( "sev", cond, ()->{ return $sev(); } ); }         
    
    public ARMCortexMThumb $seveq() throws JXMAsmError { return $_sev_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $sevzr() throws JXMAsmError { return $_sev_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $sevne() throws JXMAsmError { return $_sev_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $sevnz() throws JXMAsmError { return $_sev_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $sevhi() throws JXMAsmError { return $_sev_c_impl( Cond.HI ); } 
    public ARMCortexMThumb $sevhs() throws JXMAsmError { return $_sev_c_impl( Cond.HS ); } 
    public ARMCortexMThumb $sevlo() throws JXMAsmError { return $_sev_c_impl( Cond.LO ); } 
    public ARMCortexMThumb $sevls() throws JXMAsmError { return $_sev_c_impl( Cond.LS ); } 
    public ARMCortexMThumb $sevgt() throws JXMAsmError { return $_sev_c_impl( Cond.GT ); } 
    public ARMCortexMThumb $sevge() throws JXMAsmError { return $_sev_c_impl( Cond.GE ); } 
    public ARMCortexMThumb $sevlt() throws JXMAsmError { return $_sev_c_impl( Cond.LT ); } 
    public ARMCortexMThumb $sevle() throws JXMAsmError { return $_sev_c_impl( Cond.LE ); } 
    public ARMCortexMThumb $sevcs() throws JXMAsmError { return $_sev_c_impl( Cond.CS ); } 
    public ARMCortexMThumb $sevcc() throws JXMAsmError { return $_sev_c_impl( Cond.CC ); } 
    public ARMCortexMThumb $sevmi() throws JXMAsmError { return $_sev_c_impl( Cond.MI ); } 
    public ARMCortexMThumb $sevpl() throws JXMAsmError { return $_sev_c_impl( Cond.PL ); } 
    public ARMCortexMThumb $sevvs() throws JXMAsmError { return $_sev_c_impl( Cond.VS ); } 
    public ARMCortexMThumb $sevvc() throws JXMAsmError { return $_sev_c_impl( Cond.VC ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] STMIA <Rn>!, <registers>
    public ARMCortexMThumb $_stmia_c_impl( final Cond cond, final RegWB Rn, final Reg... Regs ) throws JXMAsmError        
    { return __itcd__( "stmia", cond, ()->{ return $stmia( Rn, Regs ); }, Rn, Regs ); } 
    
    public ARMCortexMThumb $stmiaeq( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.EQ, Rn, Regs ); } 
    public ARMCortexMThumb $stmiazr( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.EQ, Rn, Regs ); } 
    public ARMCortexMThumb $stmiane( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.NE, Rn, Regs ); } 
    public ARMCortexMThumb $stmianz( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.NE, Rn, Regs ); } 
    public ARMCortexMThumb $stmiahi( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.HI, Rn, Regs ); } 
    public ARMCortexMThumb $stmiahs( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.HS, Rn, Regs ); } 
    public ARMCortexMThumb $stmialo( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.LO, Rn, Regs ); } 
    public ARMCortexMThumb $stmials( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.LS, Rn, Regs ); } 
    public ARMCortexMThumb $stmiagt( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.GT, Rn, Regs ); } 
    public ARMCortexMThumb $stmiage( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.GE, Rn, Regs ); } 
    public ARMCortexMThumb $stmialt( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.LT, Rn, Regs ); } 
    public ARMCortexMThumb $stmiale( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.LE, Rn, Regs ); } 
    public ARMCortexMThumb $stmiacs( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.CS, Rn, Regs ); } 
    public ARMCortexMThumb $stmiacc( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.CC, Rn, Regs ); } 
    public ARMCortexMThumb $stmiami( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.MI, Rn, Regs ); } 
    public ARMCortexMThumb $stmiapl( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.PL, Rn, Regs ); } 
    public ARMCortexMThumb $stmiavs( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.VS, Rn, Regs ); } 
    public ARMCortexMThumb $stmiavc( final RegWB Rn, final Reg... Regs ) throws JXMAsmError { return $_stmia_c_impl( Cond.VC, Rn, Regs ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T4] STR.W <Rt>, [<Rn>], ±#<imm8>
    public ARMCortexMThumb $_str_pst_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError        
    { return __itcd__( "str", cond, ()->{ return $str_pst( Rt, Rn, imm8 ); }, Rt, Rn, imm8 ); } 
    
    public ARMCortexMThumb $streq_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.EQ, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strzr_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.EQ, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strne_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.NE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strnz_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.NE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strhi_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.HI, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strhs_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.HS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strlo_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.LO, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strls_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.LS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strgt_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.GT, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strge_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.GE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strlt_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.LT, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strle_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.LE, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strcs_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.CS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strcc_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.CC, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strmi_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.MI, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strpl_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.PL, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strvs_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.VS, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strvc_pst( final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_str_pst_c_impl( Cond.VC, Rt, Rn, imm8 ); }

    // [T1] STR <Rt>, [<Rn>, #<imm5>]
    // [T2] STR <Rt>, [ SP , #<imm8>]
    public ARMCortexMThumb $_str_c_impl( final Cond cond, final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError        
    { return __itcd__( "str", cond, ()->{ return $str( Rt, Rn, imm5_imm8_imm12 ); }, Rt, Rn, imm5_imm8_imm12 ); } 
    
    public ARMCortexMThumb $streq( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.EQ, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strzr( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.EQ, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strne( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.NE, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strnz( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.NE, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strhi( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.HI, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strhs( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.HS, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strlo( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.LO, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strls( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.LS, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strgt( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.GT, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strge( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.GE, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strlt( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.LT, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strle( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.LE, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strcs( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.CS, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strcc( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.CC, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strmi( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.MI, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strpl( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.PL, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strvs( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.VS, Rt, Rn, imm5_imm8_imm12 ); } 
    public ARMCortexMThumb $strvc( final Reg Rt, final RegGen Rn, final int imm5_imm8_imm12 ) throws JXMAsmError { return $_str_c_impl( Cond.VC, Rt, Rn, imm5_imm8_imm12 ); }

    // [T1] STR <Rt>, [<Rn>]
    // [T2] STR <Rt>, [ SP ]
    public ARMCortexMThumb $_str_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "str", cond, ()->{ return $str( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $streq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $strzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $strne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $strnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $strhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $strhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $strlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $strls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $strgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $strge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $strlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $strle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $strcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $strcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $strmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $strpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $strvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $strvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_str_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] STR <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $_str_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "str", cond, ()->{ return $str( Rt, Rn, Rm ); }, Rt, Rn, Rm ); } 
    
    public ARMCortexMThumb $streq( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strzr( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strne( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strnz( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.HI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.HS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strlo( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.LO, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strls( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.LS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strgt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.GT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strge( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.GE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strlt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.LT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strle( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.LE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strcs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.CS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strcc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.CC, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strmi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.MI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strpl( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.PL, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strvs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.VS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strvc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_str_c_impl( Cond.VC, Rt, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] STRB <Rt>, [<Rn>, #<imm5>]
    public ARMCortexMThumb $_strb_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "strb", cond, ()->{ return $strb( Rt, Rn, imm5 ); }, Rt, Rn, imm5 ); } 
    
    public ARMCortexMThumb $strbeq( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.EQ, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbzr( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.EQ, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbne( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.NE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbnz( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.NE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbhi( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.HI, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbhs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.HS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strblo( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.LO, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbls( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.LS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbgt( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.GT, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbge( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.GE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strblt( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.LT, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strble( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.LE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbcs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.CS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbcc( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.CC, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbmi( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.MI, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbpl( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.PL, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbvs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.VS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strbvc( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strb_c_impl( Cond.VC, Rt, Rn, imm5 ); }

    // [T1] STRB <Rt>, [<Rn>]
    public ARMCortexMThumb $_strb_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "strb", cond, ()->{ return $strb( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $strbeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $strbzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $strbne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $strbnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $strbhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $strbhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $strblo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $strbls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $strbgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $strbge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $strblt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $strble( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $strbcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $strbcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $strbmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $strbpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $strbvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $strbvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strb_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] STRB <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $_strb_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "strb", cond, ()->{ return $strb( Rt, Rn, Rm ); }, Rt, Rn, Rm ); } 
    
    public ARMCortexMThumb $strbeq( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbzr( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbne( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbnz( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbhi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.HI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbhs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.HS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strblo( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.LO, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbls( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.LS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbgt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.GT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbge( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.GE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strblt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.LT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strble( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.LE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbcs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.CS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbcc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.CC, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbmi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.MI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbpl( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.PL, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbvs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.VS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strbvc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strb_c_impl( Cond.VC, Rt, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] STRH <Rt>, [<Rn>, #<imm5>]
    public ARMCortexMThumb $_strh_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "strh", cond, ()->{ return $strh( Rt, Rn, imm5 ); }, Rt, Rn, imm5 ); } 
    
    public ARMCortexMThumb $strheq( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.EQ, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhzr( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.EQ, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhne( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.NE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhnz( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.NE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhhi( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.HI, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhhs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.HS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhlo( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.LO, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhls( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.LS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhgt( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.GT, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhge( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.GE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhlt( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.LT, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhle( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.LE, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhcs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.CS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhcc( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.CC, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhmi( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.MI, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhpl( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.PL, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhvs( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.VS, Rt, Rn, imm5 ); } 
    public ARMCortexMThumb $strhvc( final Reg Rt, final Reg Rn, final int imm5 ) throws JXMAsmError { return $_strh_c_impl( Cond.VC, Rt, Rn, imm5 ); }

    // [T1] STRH <Rt>, [<Rn>]
    public ARMCortexMThumb $_strh_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "strh", cond, ()->{ return $strh( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $strheq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $strhzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $strhne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $strhnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $strhhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $strhhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $strhlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $strhls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $strhgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $strhge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $strhlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $strhle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $strhcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $strhcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $strhmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $strhpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $strhvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $strhvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strh_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] STRH <Rt>, [<Rn>, <Rm>]
    public ARMCortexMThumb $_strh_c_impl( final Cond cond, final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "strh", cond, ()->{ return $strh( Rt, Rn, Rm ); }, Rt, Rn, Rm ); } 
    
    public ARMCortexMThumb $strheq( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhzr( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.EQ, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhne( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhnz( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.NE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhhi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.HI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhhs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.HS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhlo( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.LO, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhls( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.LS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhgt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.GT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhge( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.GE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhlt( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.LT, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhle( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.LE, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhcs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.CS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhcc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.CC, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhmi( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.MI, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhpl( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.PL, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhvs( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.VS, Rt, Rn, Rm ); } 
    public ARMCortexMThumb $strhvc( final Reg Rt, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_strh_c_impl( Cond.VC, Rt, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] STL <Rt>, [<Rn>]
    public ARMCortexMThumb $_stl_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "stl", cond, ()->{ return $stl( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $stleq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $stlzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $stlne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $stlnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $stlhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $stlhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $stllo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $stlls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $stlgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $stlge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $stllt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $stlle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $stlcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $stlcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $stlmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $stlpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $stlvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $stlvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stl_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] STLB <Rt>, [<Rn>]
    public ARMCortexMThumb $_stlb_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "stlb", cond, ()->{ return $stlb( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $stlbeq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $stlbzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $stlbne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $stlbnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $stlbhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $stlbhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $stlblo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $stlbls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $stlbgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $stlbge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $stlblt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $stlble( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $stlbcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $stlbcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $stlbmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $stlbpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $stlbvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $stlbvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlb_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] STLH <Rt>, [<Rn>]
    public ARMCortexMThumb $_stlh_c_impl( final Cond cond, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "stlh", cond, ()->{ return $stlh( Rt, Rn ); }, Rt, Rn ); } 
    
    public ARMCortexMThumb $stlheq( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $stlhzr( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.EQ, Rt, Rn ); } 
    public ARMCortexMThumb $stlhne( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $stlhnz( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.NE, Rt, Rn ); } 
    public ARMCortexMThumb $stlhhi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.HI, Rt, Rn ); } 
    public ARMCortexMThumb $stlhhs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.HS, Rt, Rn ); } 
    public ARMCortexMThumb $stlhlo( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.LO, Rt, Rn ); } 
    public ARMCortexMThumb $stlhls( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.LS, Rt, Rn ); } 
    public ARMCortexMThumb $stlhgt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.GT, Rt, Rn ); } 
    public ARMCortexMThumb $stlhge( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.GE, Rt, Rn ); } 
    public ARMCortexMThumb $stlhlt( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.LT, Rt, Rn ); } 
    public ARMCortexMThumb $stlhle( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.LE, Rt, Rn ); } 
    public ARMCortexMThumb $stlhcs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.CS, Rt, Rn ); } 
    public ARMCortexMThumb $stlhcc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.CC, Rt, Rn ); } 
    public ARMCortexMThumb $stlhmi( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.MI, Rt, Rn ); } 
    public ARMCortexMThumb $stlhpl( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.PL, Rt, Rn ); } 
    public ARMCortexMThumb $stlhvs( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.VS, Rt, Rn ); } 
    public ARMCortexMThumb $stlhvc( final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlh_c_impl( Cond.VC, Rt, Rn ); }

    // [T1] STLEX <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $_stlex_c_impl( final Cond cond, final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "stlex", cond, ()->{ return $stlex( Rd, Rt, Rn ); }, Rd, Rt, Rn ); } 
    
    public ARMCortexMThumb $stlexeq( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexzr( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexne( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexnz( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.HI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.HS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexlo( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.LO, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexls( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.LS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexgt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.GT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexge( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.GE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexlt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.LT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexle( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.LE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexcs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.CS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexcc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.CC, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexmi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.MI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexpl( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.PL, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexvs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.VS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexvc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlex_c_impl( Cond.VC, Rd, Rt, Rn ); }

    // [T1] STLEXB <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $_stlexb_c_impl( final Cond cond, final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "stlexb", cond, ()->{ return $stlexb( Rd, Rt, Rn ); }, Rd, Rt, Rn ); } 
    
    public ARMCortexMThumb $stlexbeq( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbzr( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbne( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbnz( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbhi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.HI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbhs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.HS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexblo( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.LO, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbls( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.LS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbgt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.GT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbge( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.GE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexblt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.LT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexble( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.LE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbcs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.CS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbcc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.CC, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbmi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.MI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbpl( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.PL, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbvs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.VS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexbvc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexb_c_impl( Cond.VC, Rd, Rt, Rn ); }

    // [T1] STLEXH <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $_stlexh_c_impl( final Cond cond, final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "stlexh", cond, ()->{ return $stlexh( Rd, Rt, Rn ); }, Rd, Rt, Rn ); } 
    
    public ARMCortexMThumb $stlexheq( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhzr( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhne( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhnz( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhhi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.HI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhhs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.HS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhlo( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.LO, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhls( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.LS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhgt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.GT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhge( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.GE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhlt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.LT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhle( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.LE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhcs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.CS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhcc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.CC, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhmi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.MI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhpl( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.PL, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhvs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.VS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $stlexhvc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_stlexh_c_impl( Cond.VC, Rd, Rt, Rn ); }

    // [T1] STREX <Rd>, <Rt>, [<Rn>, #<imm8>]
    public ARMCortexMThumb $_strex_c_impl( final Cond cond, final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError        
    { return __itcd__( "strex", cond, ()->{ return $strex( Rd, Rt, Rn, imm8 ); }, Rd, Rt, Rn, imm8 ); } 
    
    public ARMCortexMThumb $strexeq( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.EQ, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexzr( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.EQ, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexne( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.NE, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexnz( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.NE, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexhi( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.HI, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexhs( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.HS, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexlo( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.LO, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexls( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.LS, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexgt( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.GT, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexge( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.GE, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexlt( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.LT, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexle( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.LE, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexcs( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.CS, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexcc( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.CC, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexmi( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.MI, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexpl( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.PL, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexvs( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.VS, Rd, Rt, Rn, imm8 ); } 
    public ARMCortexMThumb $strexvc( final Reg Rd, final Reg Rt, final Reg Rn, final int imm8 ) throws JXMAsmError { return $_strex_c_impl( Cond.VC, Rd, Rt, Rn, imm8 ); }

    // [T1] STREX <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $_strex_c_impl( final Cond cond, final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "strex", cond, ()->{ return $strex( Rd, Rt, Rn ); }, Rd, Rt, Rn ); } 
    
    public ARMCortexMThumb $strexeq( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexzr( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexne( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexnz( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.HI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.HS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexlo( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.LO, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexls( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.LS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexgt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.GT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexge( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.GE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexlt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.LT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexle( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.LE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexcs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.CS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexcc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.CC, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexmi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.MI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexpl( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.PL, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexvs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.VS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexvc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strex_c_impl( Cond.VC, Rd, Rt, Rn ); }

    // [T1] STREXB <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $_strexb_c_impl( final Cond cond, final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "strexb", cond, ()->{ return $strexb( Rd, Rt, Rn ); }, Rd, Rt, Rn ); } 
    
    public ARMCortexMThumb $strexbeq( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbzr( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbne( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbnz( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbhi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.HI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbhs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.HS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexblo( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.LO, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbls( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.LS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbgt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.GT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbge( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.GE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexblt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.LT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexble( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.LE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbcs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.CS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbcc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.CC, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbmi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.MI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbpl( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.PL, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbvs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.VS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexbvc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexb_c_impl( Cond.VC, Rd, Rt, Rn ); }

    // [T1] STREXH <Rd>, <Rt>, [<Rn>]
    public ARMCortexMThumb $_strexh_c_impl( final Cond cond, final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError        
    { return __itcd__( "strexh", cond, ()->{ return $strexh( Rd, Rt, Rn ); }, Rd, Rt, Rn ); } 
    
    public ARMCortexMThumb $strexheq( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhzr( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.EQ, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhne( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhnz( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.NE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhhi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.HI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhhs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.HS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhlo( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.LO, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhls( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.LS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhgt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.GT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhge( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.GE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhlt( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.LT, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhle( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.LE, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhcs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.CS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhcc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.CC, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhmi( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.MI, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhpl( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.PL, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhvs( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.VS, Rd, Rt, Rn ); } 
    public ARMCortexMThumb $strexhvc( final Reg Rd, final Reg Rt, final Reg Rn ) throws JXMAsmError { return $_strexh_c_impl( Cond.VC, Rd, Rt, Rn ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SUBS <Rd>, <Rn>, #<imm3>
    // [T1] SUB   SP ,  SP , #<imm7>
    public ARMCortexMThumb $_sub_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError                                               
    { return __itcd__( "sub", cond, ()->{ return (Rd.regNum == Reg.SP.regNum || Rn.regNum == Reg.SP.regNum) ? $sub( Rd, Rn, imm3_imm7 ) : $subs( Rd, Rn, imm3_imm7 ); }, Rd, Rn, imm3_imm7 ); } 
    
    public ARMCortexMThumb $subeq( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.EQ, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subzr( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.EQ, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subne( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.NE, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subnz( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.NE, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subhi( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.HI, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subhs( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.HS, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $sublo( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.LO, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subls( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.LS, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subgt( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.GT, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subge( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.GE, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $sublt( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.LT, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $suble( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.LE, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subcs( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.CS, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subcc( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.CC, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $submi( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.MI, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subpl( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.PL, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subvs( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.VS, Rd, Rn, imm3_imm7 ); } 
    public ARMCortexMThumb $subvc( final Reg Rd, final Reg Rn, final int imm3_imm7 ) throws JXMAsmError { return $_sub_c_impl( Cond.VC, Rd, Rn, imm3_imm7 ); }

    // [T2] SUBS <Rdn>, #<imm8>
    public ARMCortexMThumb $_sub_c_impl( final Cond cond, final Reg Rdn, final int imm8 ) throws JXMAsmError        
    { return __itcd__( "sub", cond, ()->{ return $subs( Rdn, imm8 ); }, Rdn, imm8 ); } 
    
    public ARMCortexMThumb $subeq( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.EQ, Rdn, imm8 ); } 
    public ARMCortexMThumb $subzr( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.EQ, Rdn, imm8 ); } 
    public ARMCortexMThumb $subne( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.NE, Rdn, imm8 ); } 
    public ARMCortexMThumb $subnz( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.NE, Rdn, imm8 ); } 
    public ARMCortexMThumb $subhi( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.HI, Rdn, imm8 ); } 
    public ARMCortexMThumb $subhs( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.HS, Rdn, imm8 ); } 
    public ARMCortexMThumb $sublo( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.LO, Rdn, imm8 ); } 
    public ARMCortexMThumb $subls( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.LS, Rdn, imm8 ); } 
    public ARMCortexMThumb $subgt( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.GT, Rdn, imm8 ); } 
    public ARMCortexMThumb $subge( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.GE, Rdn, imm8 ); } 
    public ARMCortexMThumb $sublt( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.LT, Rdn, imm8 ); } 
    public ARMCortexMThumb $suble( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.LE, Rdn, imm8 ); } 
    public ARMCortexMThumb $subcs( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.CS, Rdn, imm8 ); } 
    public ARMCortexMThumb $subcc( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.CC, Rdn, imm8 ); } 
    public ARMCortexMThumb $submi( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.MI, Rdn, imm8 ); } 
    public ARMCortexMThumb $subpl( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.PL, Rdn, imm8 ); } 
    public ARMCortexMThumb $subvs( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.VS, Rdn, imm8 ); } 
    public ARMCortexMThumb $subvc( final Reg Rdn, final int imm8 ) throws JXMAsmError { return $_sub_c_impl( Cond.VC, Rdn, imm8 ); }

    // [T1] SUBS <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_sub_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "sub", cond, ()->{ return $subs( Rd, Rn, Rm ); }, Rd, Rn, Rm ); } 
    
    public ARMCortexMThumb $subeq( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.EQ, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subzr( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.EQ, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subne( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.NE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subnz( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.NE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subhi( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.HI, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subhs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.HS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sublo( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.LO, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subls( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.LS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subgt( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.GT, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subge( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.GE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $sublt( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.LT, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $suble( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.LE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subcs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.CS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subcc( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.CC, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $submi( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.MI, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subpl( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.PL, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subvs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.VS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $subvc( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_sub_c_impl( Cond.VC, Rd, Rn, Rm ); }

    // [PI] SUB SP, SP, #<imm7>
    public ARMCortexMThumb $_sub_sp_c_impl( final Cond cond, final int imm7 ) throws JXMAsmError        
    { return __itcd__( "sub", cond, ()->{ return $sub_sp( imm7 ); }, imm7 ); } 
    
    public ARMCortexMThumb $subeq_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.EQ, imm7 ); } 
    public ARMCortexMThumb $subzr_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.EQ, imm7 ); } 
    public ARMCortexMThumb $subne_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.NE, imm7 ); } 
    public ARMCortexMThumb $subnz_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.NE, imm7 ); } 
    public ARMCortexMThumb $subhi_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.HI, imm7 ); } 
    public ARMCortexMThumb $subhs_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.HS, imm7 ); } 
    public ARMCortexMThumb $sublo_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.LO, imm7 ); } 
    public ARMCortexMThumb $subls_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.LS, imm7 ); } 
    public ARMCortexMThumb $subgt_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.GT, imm7 ); } 
    public ARMCortexMThumb $subge_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.GE, imm7 ); } 
    public ARMCortexMThumb $sublt_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.LT, imm7 ); } 
    public ARMCortexMThumb $suble_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.LE, imm7 ); } 
    public ARMCortexMThumb $subcs_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.CS, imm7 ); } 
    public ARMCortexMThumb $subcc_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.CC, imm7 ); } 
    public ARMCortexMThumb $submi_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.MI, imm7 ); } 
    public ARMCortexMThumb $subpl_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.PL, imm7 ); } 
    public ARMCortexMThumb $subvs_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.VS, imm7 ); } 
    public ARMCortexMThumb $subvc_sp( final int imm7 ) throws JXMAsmError { return $_sub_sp_c_impl( Cond.VC, imm7 ); }

    // [T4] SUBW <Rd>, <Rn>, #<imm12>
    public ARMCortexMThumb $_subw_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError        
    { return __itcd__( "subw", cond, ()->{ return $subw( Rd, Rn, imm12 ); }, Rd, Rn, imm12 ); } 
    
    public ARMCortexMThumb $subweq( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.EQ, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwzr( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.EQ, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwne( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.NE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwnz( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.NE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwhi( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.HI, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwhs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.HS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwlo( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.LO, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwls( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.LS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwgt( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.GT, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwge( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.GE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwlt( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.LT, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwle( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.LE, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwcs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.CS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwcc( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.CC, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwmi( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.MI, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwpl( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.PL, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwvs( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.VS, Rd, Rn, imm12 ); } 
    public ARMCortexMThumb $subwvc( final Reg Rd, final Reg Rn, final int imm12 ) throws JXMAsmError { return $_subw_c_impl( Cond.VC, Rd, Rn, imm12 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SVC #<imm8>
    public ARMCortexMThumb $_svc_c_impl( final Cond cond, final int imm8 ) throws JXMAsmError        
    { return __itcd__( "svc", cond, ()->{ return $svc( imm8 ); }, imm8 ); } 
    
    public ARMCortexMThumb $svceq( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.EQ, imm8 ); } 
    public ARMCortexMThumb $svczr( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.EQ, imm8 ); } 
    public ARMCortexMThumb $svcne( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.NE, imm8 ); } 
    public ARMCortexMThumb $svcnz( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.NE, imm8 ); } 
    public ARMCortexMThumb $svchi( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.HI, imm8 ); } 
    public ARMCortexMThumb $svchs( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.HS, imm8 ); } 
    public ARMCortexMThumb $svclo( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.LO, imm8 ); } 
    public ARMCortexMThumb $svcls( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.LS, imm8 ); } 
    public ARMCortexMThumb $svcgt( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.GT, imm8 ); } 
    public ARMCortexMThumb $svcge( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.GE, imm8 ); } 
    public ARMCortexMThumb $svclt( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.LT, imm8 ); } 
    public ARMCortexMThumb $svcle( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.LE, imm8 ); } 
    public ARMCortexMThumb $svccs( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.CS, imm8 ); } 
    public ARMCortexMThumb $svccc( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.CC, imm8 ); } 
    public ARMCortexMThumb $svcmi( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.MI, imm8 ); } 
    public ARMCortexMThumb $svcpl( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.PL, imm8 ); } 
    public ARMCortexMThumb $svcvs( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.VS, imm8 ); } 
    public ARMCortexMThumb $svcvc( final int imm8 ) throws JXMAsmError { return $_svc_c_impl( Cond.VC, imm8 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SXTB <Rd>, <Rm>
    public ARMCortexMThumb $_sxtb_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "sxtb", cond, ()->{ return $sxtb( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $sxtbeq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbhi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbhs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $sxtblo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbgt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $sxtblt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $sxtble( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbcs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbcc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbpl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $sxtbvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxtb_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] SXTH <Rd>, <Rm>
    public ARMCortexMThumb $_sxth_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "sxth", cond, ()->{ return $sxth( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $sxtheq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $sxthzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $sxthne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $sxthnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $sxthhi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $sxthhs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $sxthlo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $sxthls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $sxthgt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $sxthge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $sxthlt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $sxthle( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $sxthcs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $sxthcc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $sxthmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $sxthpl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $sxthvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $sxthvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_sxth_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] TST <Rn>, #<const>
    public ARMCortexMThumb $_tst_c_impl( final Cond cond, final Reg Rn, final long const_ ) throws JXMAsmError        
    { return __itcd__( "tst", cond, ()->{ return $tst( Rn, const_ ); }, Rn, const_ ); } 
    
    public ARMCortexMThumb $tsteq( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.EQ, Rn, const_ ); } 
    public ARMCortexMThumb $tstzr( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.EQ, Rn, const_ ); } 
    public ARMCortexMThumb $tstne( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.NE, Rn, const_ ); } 
    public ARMCortexMThumb $tstnz( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.NE, Rn, const_ ); } 
    public ARMCortexMThumb $tsthi( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.HI, Rn, const_ ); } 
    public ARMCortexMThumb $tsths( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.HS, Rn, const_ ); } 
    public ARMCortexMThumb $tstlo( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.LO, Rn, const_ ); } 
    public ARMCortexMThumb $tstls( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.LS, Rn, const_ ); } 
    public ARMCortexMThumb $tstgt( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.GT, Rn, const_ ); } 
    public ARMCortexMThumb $tstge( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.GE, Rn, const_ ); } 
    public ARMCortexMThumb $tstlt( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.LT, Rn, const_ ); } 
    public ARMCortexMThumb $tstle( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.LE, Rn, const_ ); } 
    public ARMCortexMThumb $tstcs( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.CS, Rn, const_ ); } 
    public ARMCortexMThumb $tstcc( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.CC, Rn, const_ ); } 
    public ARMCortexMThumb $tstmi( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.MI, Rn, const_ ); } 
    public ARMCortexMThumb $tstpl( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.PL, Rn, const_ ); } 
    public ARMCortexMThumb $tstvs( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.VS, Rn, const_ ); } 
    public ARMCortexMThumb $tstvc( final Reg Rn, final long const_ ) throws JXMAsmError { return $_tst_c_impl( Cond.VC, Rn, const_ ); }

    // [T1] TST   <Rn>, <Rm>
    // [T2] TST.W <Rn>, <Rm>
    public ARMCortexMThumb $_tst_c_impl( final Cond cond, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "tst", cond, ()->{ return $tst( Rn, Rm ); }, Rn, Rm ); } 
    
    public ARMCortexMThumb $tsteq( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.EQ, Rn, Rm ); } 
    public ARMCortexMThumb $tstzr( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.EQ, Rn, Rm ); } 
    public ARMCortexMThumb $tstne( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.NE, Rn, Rm ); } 
    public ARMCortexMThumb $tstnz( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.NE, Rn, Rm ); } 
    public ARMCortexMThumb $tsthi( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.HI, Rn, Rm ); } 
    public ARMCortexMThumb $tsths( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.HS, Rn, Rm ); } 
    public ARMCortexMThumb $tstlo( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.LO, Rn, Rm ); } 
    public ARMCortexMThumb $tstls( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.LS, Rn, Rm ); } 
    public ARMCortexMThumb $tstgt( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.GT, Rn, Rm ); } 
    public ARMCortexMThumb $tstge( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.GE, Rn, Rm ); } 
    public ARMCortexMThumb $tstlt( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.LT, Rn, Rm ); } 
    public ARMCortexMThumb $tstle( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.LE, Rn, Rm ); } 
    public ARMCortexMThumb $tstcs( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.CS, Rn, Rm ); } 
    public ARMCortexMThumb $tstcc( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.CC, Rn, Rm ); } 
    public ARMCortexMThumb $tstmi( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.MI, Rn, Rm ); } 
    public ARMCortexMThumb $tstpl( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.PL, Rn, Rm ); } 
    public ARMCortexMThumb $tstvs( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.VS, Rn, Rm ); } 
    public ARMCortexMThumb $tstvc( final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_tst_c_impl( Cond.VC, Rn, Rm ); }

    // [T2] TST.W <Rn>, <Rm>, <shift_operand> {#<imm5>}
    public ARMCortexMThumb $_tst_c_impl( final Cond cond, final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError        
    { return __itcd__( "tst", cond, ()->{ return $tst( Rn, Rm, shift, imm5 ); }, Rn, Rm, shift, imm5 ); } 
    
    public ARMCortexMThumb $tsteq( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.EQ, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstzr( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.EQ, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstne( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.NE, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstnz( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.NE, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tsthi( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.HI, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tsths( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.HS, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstlo( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.LO, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstls( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.LS, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstgt( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.GT, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstge( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.GE, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstlt( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.LT, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstle( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.LE, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstcs( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.CS, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstcc( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.CC, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstmi( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.MI, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstpl( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.PL, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstvs( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.VS, Rn, Rm, shift, imm5 ); } 
    public ARMCortexMThumb $tstvc( final Reg Rn, final Reg Rm, final Shift shift, final int imm5 ) throws JXMAsmError { return $_tst_c_impl( Cond.VC, Rn, Rm, shift, imm5 ); }

    // [T2] TST.W <Rn>, <Rm>, <shift_operand>
    public ARMCortexMThumb $_tst_c_impl( final Cond cond, final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError        
    { return __itcd__( "tst", cond, ()->{ return $tst( Rn, Rm, shift ); }, Rn, Rm, shift ); } 
    
    public ARMCortexMThumb $tsteq( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.EQ, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstzr( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.EQ, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstne( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.NE, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstnz( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.NE, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tsthi( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.HI, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tsths( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.HS, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstlo( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.LO, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstls( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.LS, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstgt( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.GT, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstge( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.GE, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstlt( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.LT, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstle( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.LE, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstcs( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.CS, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstcc( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.CC, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstmi( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.MI, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstpl( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.PL, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstvs( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.VS, Rn, Rm, shift ); } 
    public ARMCortexMThumb $tstvc( final Reg Rn, final Reg Rm, final Shift shift ) throws JXMAsmError { return $_tst_c_impl( Cond.VC, Rn, Rm, shift ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] UDF #<imm8>
    public ARMCortexMThumb $_udf_c_impl( final Cond cond, final int imm8 ) throws JXMAsmError        
    { return __itcd__( "udf", cond, ()->{ return $udf( imm8 ); }, imm8 ); } 
    
    public ARMCortexMThumb $udfeq( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.EQ, imm8 ); } 
    public ARMCortexMThumb $udfzr( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.EQ, imm8 ); } 
    public ARMCortexMThumb $udfne( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.NE, imm8 ); } 
    public ARMCortexMThumb $udfnz( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.NE, imm8 ); } 
    public ARMCortexMThumb $udfhi( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.HI, imm8 ); } 
    public ARMCortexMThumb $udfhs( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.HS, imm8 ); } 
    public ARMCortexMThumb $udflo( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.LO, imm8 ); } 
    public ARMCortexMThumb $udfls( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.LS, imm8 ); } 
    public ARMCortexMThumb $udfgt( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.GT, imm8 ); } 
    public ARMCortexMThumb $udfge( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.GE, imm8 ); } 
    public ARMCortexMThumb $udflt( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.LT, imm8 ); } 
    public ARMCortexMThumb $udfle( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.LE, imm8 ); } 
    public ARMCortexMThumb $udfcs( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.CS, imm8 ); } 
    public ARMCortexMThumb $udfcc( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.CC, imm8 ); } 
    public ARMCortexMThumb $udfmi( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.MI, imm8 ); } 
    public ARMCortexMThumb $udfpl( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.PL, imm8 ); } 
    public ARMCortexMThumb $udfvs( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.VS, imm8 ); } 
    public ARMCortexMThumb $udfvc( final int imm8 ) throws JXMAsmError { return $_udf_c_impl( Cond.VC, imm8 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] UDIV <Rd>, <Rn>, <Rm>
    public ARMCortexMThumb $_udiv_c_impl( final Cond cond, final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "udiv", cond, ()->{ return $udiv( Rd, Rn, Rm ); }, Rd, Rn, Rm ); } 
    
    public ARMCortexMThumb $udiveq( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.EQ, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivzr( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.EQ, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivne( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.NE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivnz( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.NE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivhi( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.HI, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivhs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.HS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivlo( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.LO, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivls( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.LS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivgt( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.GT, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivge( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.GE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivlt( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.LT, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivle( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.LE, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivcs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.CS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivcc( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.CC, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivmi( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.MI, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivpl( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.PL, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivvs( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.VS, Rd, Rn, Rm ); } 
    public ARMCortexMThumb $udivvc( final Reg Rd, final Reg Rn, final Reg Rm ) throws JXMAsmError { return $_udiv_c_impl( Cond.VC, Rd, Rn, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] UXTB <Rd>, <Rm>
    public ARMCortexMThumb $_uxtb_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "uxtb", cond, ()->{ return $uxtb( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $uxtbeq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbhi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbhs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $uxtblo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbgt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $uxtblt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $uxtble( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbcs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbcc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbpl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $uxtbvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxtb_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] UXTH <Rd>, <Rm>
    public ARMCortexMThumb $_uxth_c_impl( final Cond cond, final Reg Rd, final Reg Rm ) throws JXMAsmError        
    { return __itcd__( "uxth", cond, ()->{ return $uxth( Rd, Rm ); }, Rd, Rm ); } 
    
    public ARMCortexMThumb $uxtheq( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $uxthzr( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.EQ, Rd, Rm ); } 
    public ARMCortexMThumb $uxthne( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $uxthnz( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.NE, Rd, Rm ); } 
    public ARMCortexMThumb $uxthhi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.HI, Rd, Rm ); } 
    public ARMCortexMThumb $uxthhs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.HS, Rd, Rm ); } 
    public ARMCortexMThumb $uxthlo( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.LO, Rd, Rm ); } 
    public ARMCortexMThumb $uxthls( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.LS, Rd, Rm ); } 
    public ARMCortexMThumb $uxthgt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.GT, Rd, Rm ); } 
    public ARMCortexMThumb $uxthge( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.GE, Rd, Rm ); } 
    public ARMCortexMThumb $uxthlt( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.LT, Rd, Rm ); } 
    public ARMCortexMThumb $uxthle( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.LE, Rd, Rm ); } 
    public ARMCortexMThumb $uxthcs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.CS, Rd, Rm ); } 
    public ARMCortexMThumb $uxthcc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.CC, Rd, Rm ); } 
    public ARMCortexMThumb $uxthmi( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.MI, Rd, Rm ); } 
    public ARMCortexMThumb $uxthpl( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.PL, Rd, Rm ); } 
    public ARMCortexMThumb $uxthvs( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.VS, Rd, Rm ); } 
    public ARMCortexMThumb $uxthvc( final Reg Rd, final Reg Rm ) throws JXMAsmError { return $_uxth_c_impl( Cond.VC, Rd, Rm ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] WFE
    public ARMCortexMThumb $_wfe_c_impl( final Cond cond ) throws JXMAsmError 
    { return __itcd__( "wfe", cond, ()->{ return $wfe(); } ); }         
    
    public ARMCortexMThumb $wfeeq() throws JXMAsmError { return $_wfe_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $wfezr() throws JXMAsmError { return $_wfe_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $wfene() throws JXMAsmError { return $_wfe_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $wfenz() throws JXMAsmError { return $_wfe_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $wfehi() throws JXMAsmError { return $_wfe_c_impl( Cond.HI ); } 
    public ARMCortexMThumb $wfehs() throws JXMAsmError { return $_wfe_c_impl( Cond.HS ); } 
    public ARMCortexMThumb $wfelo() throws JXMAsmError { return $_wfe_c_impl( Cond.LO ); } 
    public ARMCortexMThumb $wfels() throws JXMAsmError { return $_wfe_c_impl( Cond.LS ); } 
    public ARMCortexMThumb $wfegt() throws JXMAsmError { return $_wfe_c_impl( Cond.GT ); } 
    public ARMCortexMThumb $wfege() throws JXMAsmError { return $_wfe_c_impl( Cond.GE ); } 
    public ARMCortexMThumb $wfelt() throws JXMAsmError { return $_wfe_c_impl( Cond.LT ); } 
    public ARMCortexMThumb $wfele() throws JXMAsmError { return $_wfe_c_impl( Cond.LE ); } 
    public ARMCortexMThumb $wfecs() throws JXMAsmError { return $_wfe_c_impl( Cond.CS ); } 
    public ARMCortexMThumb $wfecc() throws JXMAsmError { return $_wfe_c_impl( Cond.CC ); } 
    public ARMCortexMThumb $wfemi() throws JXMAsmError { return $_wfe_c_impl( Cond.MI ); } 
    public ARMCortexMThumb $wfepl() throws JXMAsmError { return $_wfe_c_impl( Cond.PL ); } 
    public ARMCortexMThumb $wfevs() throws JXMAsmError { return $_wfe_c_impl( Cond.VS ); } 
    public ARMCortexMThumb $wfevc() throws JXMAsmError { return $_wfe_c_impl( Cond.VC ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] WFI
    public ARMCortexMThumb $_wfi_c_impl( final Cond cond ) throws JXMAsmError 
    { return __itcd__( "wfi", cond, ()->{ return $wfi(); } ); }         
    
    public ARMCortexMThumb $wfieq() throws JXMAsmError { return $_wfi_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $wfizr() throws JXMAsmError { return $_wfi_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $wfine() throws JXMAsmError { return $_wfi_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $wfinz() throws JXMAsmError { return $_wfi_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $wfihi() throws JXMAsmError { return $_wfi_c_impl( Cond.HI ); } 
    public ARMCortexMThumb $wfihs() throws JXMAsmError { return $_wfi_c_impl( Cond.HS ); } 
    public ARMCortexMThumb $wfilo() throws JXMAsmError { return $_wfi_c_impl( Cond.LO ); } 
    public ARMCortexMThumb $wfils() throws JXMAsmError { return $_wfi_c_impl( Cond.LS ); } 
    public ARMCortexMThumb $wfigt() throws JXMAsmError { return $_wfi_c_impl( Cond.GT ); } 
    public ARMCortexMThumb $wfige() throws JXMAsmError { return $_wfi_c_impl( Cond.GE ); } 
    public ARMCortexMThumb $wfilt() throws JXMAsmError { return $_wfi_c_impl( Cond.LT ); } 
    public ARMCortexMThumb $wfile() throws JXMAsmError { return $_wfi_c_impl( Cond.LE ); } 
    public ARMCortexMThumb $wfics() throws JXMAsmError { return $_wfi_c_impl( Cond.CS ); } 
    public ARMCortexMThumb $wficc() throws JXMAsmError { return $_wfi_c_impl( Cond.CC ); } 
    public ARMCortexMThumb $wfimi() throws JXMAsmError { return $_wfi_c_impl( Cond.MI ); } 
    public ARMCortexMThumb $wfipl() throws JXMAsmError { return $_wfi_c_impl( Cond.PL ); } 
    public ARMCortexMThumb $wfivs() throws JXMAsmError { return $_wfi_c_impl( Cond.VS ); } 
    public ARMCortexMThumb $wfivc() throws JXMAsmError { return $_wfi_c_impl( Cond.VC ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // [T1] YIELD
    public ARMCortexMThumb $_yield_c_impl( final Cond cond ) throws JXMAsmError 
    { return __itcd__( "yield", cond, ()->{ return $yield(); } ); }         
    
    public ARMCortexMThumb $yieldeq() throws JXMAsmError { return $_yield_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $yieldzr() throws JXMAsmError { return $_yield_c_impl( Cond.EQ ); } 
    public ARMCortexMThumb $yieldne() throws JXMAsmError { return $_yield_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $yieldnz() throws JXMAsmError { return $_yield_c_impl( Cond.NE ); } 
    public ARMCortexMThumb $yieldhi() throws JXMAsmError { return $_yield_c_impl( Cond.HI ); } 
    public ARMCortexMThumb $yieldhs() throws JXMAsmError { return $_yield_c_impl( Cond.HS ); } 
    public ARMCortexMThumb $yieldlo() throws JXMAsmError { return $_yield_c_impl( Cond.LO ); } 
    public ARMCortexMThumb $yieldls() throws JXMAsmError { return $_yield_c_impl( Cond.LS ); } 
    public ARMCortexMThumb $yieldgt() throws JXMAsmError { return $_yield_c_impl( Cond.GT ); } 
    public ARMCortexMThumb $yieldge() throws JXMAsmError { return $_yield_c_impl( Cond.GE ); } 
    public ARMCortexMThumb $yieldlt() throws JXMAsmError { return $_yield_c_impl( Cond.LT ); } 
    public ARMCortexMThumb $yieldle() throws JXMAsmError { return $_yield_c_impl( Cond.LE ); } 
    public ARMCortexMThumb $yieldcs() throws JXMAsmError { return $_yield_c_impl( Cond.CS ); } 
    public ARMCortexMThumb $yieldcc() throws JXMAsmError { return $_yield_c_impl( Cond.CC ); } 
    public ARMCortexMThumb $yieldmi() throws JXMAsmError { return $_yield_c_impl( Cond.MI ); } 
    public ARMCortexMThumb $yieldpl() throws JXMAsmError { return $_yield_c_impl( Cond.PL ); } 
    public ARMCortexMThumb $yieldvs() throws JXMAsmError { return $_yield_c_impl( Cond.VS ); } 
    public ARMCortexMThumb $yieldvc() throws JXMAsmError { return $_yield_c_impl( Cond.VC ); }

} // class ARMCortexMThumbC
