/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.math.BigInteger;

import java.util.ArrayList;

import jxm.*;
import jxm.xb.*;


public class BSEUtil {

    @FunctionalInterface private static interface M_Void_Void   <ObjectT> { void       apply(final ObjectT obj                                       ) throws JXMException; }
  //@FunctionalInterface private static interface M_Void_PByt   <ObjectT> { void       apply(final ObjectT obj, final byte   par1                    ) throws JXMException; }
  //@FunctionalInterface private static interface M_Void_PChr   <ObjectT> { void       apply(final ObjectT obj, final char   par1                    ) throws JXMException; }
    @FunctionalInterface private static interface M_Void_PInt   <ObjectT> { void       apply(final ObjectT obj, final int    par1                    ) throws JXMException; }
    @FunctionalInterface private static interface M_Void_PStr   <ObjectT> { void       apply(final ObjectT obj, final String par1                    ) throws JXMException; }
    @FunctionalInterface private static interface M_Void_PStrBol<ObjectT> { void       apply(final ObjectT obj, final String par1, final boolean par2) throws JXMException; }

    @FunctionalInterface private static interface M_RByt_Void   <ObjectT> { byte       apply(final ObjectT obj                                       ) throws JXMException; }
    @FunctionalInterface private static interface M_RInt_Void   <ObjectT> { int        apply(final ObjectT obj                                       ) throws JXMException; }
    @FunctionalInterface private static interface M_RLng_Void   <ObjectT> { long       apply(final ObjectT obj                                       ) throws JXMException; }
    @FunctionalInterface private static interface M_RBig_Void   <ObjectT> { BigInteger apply(final ObjectT obj                                       ) throws JXMException; }
    @FunctionalInterface private static interface M_RFlt_Void   <ObjectT> { float      apply(final ObjectT obj                                       ) throws JXMException; }
    @FunctionalInterface private static interface M_RDbl_Void   <ObjectT> { double     apply(final ObjectT obj                                       ) throws JXMException; }
    @FunctionalInterface private static interface M_RStr_Void   <ObjectT> { String     apply(final ObjectT obj                                       ) throws JXMException; }
    @FunctionalInterface private static interface M_RStr_PBol   <ObjectT> { String     apply(final ObjectT obj,                    final boolean par1) throws JXMException; }
    @FunctionalInterface private static interface M_RStr_PInt   <ObjectT> { String     apply(final ObjectT obj,                    final int     par1) throws JXMException; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static BSEList.ByteStreamEditor __xGetBSE(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the BSE handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Get and return the BSE object
        return BSEList.bseGet(handle);
    }

    private static int __xGetFlattenedP1Int(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get and return the integer parameter
        return XCom.toLong( execBlock, execData, XCom.flatten( evalVals.get(1), "" ) ).intValue();
    }

    private static String __xGetFlattenedP1Str(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get and return the string parameter
        return XCom.flatten( evalVals.get(1), "" );
    }

    private static boolean __xGetFlattenedOptP2Bol(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get and return the optional boolean parameter
        return XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "false") );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void __xNotNull_Void_Void(final ArrayList<XCom.VariableValue> evalVals, final M_Void_Void<BSEList.ByteStreamEditor> method) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method
        method.apply(bse);
    }

    private static void __xNotNull_Void_PInt(final ArrayList<XCom.VariableValue> evalVals, final M_Void_PInt<BSEList.ByteStreamEditor> method, final int par1) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method
        method.apply(bse, par1);
    }

    /*
    private static void __xNotNull_Void_XByt(final ArrayList<XCom.VariableValue> evalVals, final M_Void_PByt<BSEList.ByteStreamEditor> method, final XCom.VariableValue par1) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method
        for(final XCom.VariableStore chk : par1) {
            method.apply( bse, (byte) chk.value.charAt(0) );
        }
    }

    private static void __xNotNull_Void_XChr(final ArrayList<XCom.VariableValue> evalVals, final M_Void_PChr<BSEList.ByteStreamEditor> method, final XCom.VariableValue par1) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method
        for(final XCom.VariableStore chk : par1) {
            method.apply( bse, chk.value.charAt(0) );
        }
    }
   */

    private static void __xNotNull_Void_PStr(final ArrayList<XCom.VariableValue> evalVals, final M_Void_PStr<BSEList.ByteStreamEditor> method, final String par1) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method
        method.apply(bse, par1);
    }

    private static void __xNotNull_Void_XStr(final ArrayList<XCom.VariableValue> evalVals, final M_Void_PStr<BSEList.ByteStreamEditor> method, final XCom.VariableValue par1) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method
        for(final XCom.VariableStore chk : par1) {
            method.apply(bse, chk.value);
        }
    }

    private static void __xNotNull_Void_XStrBol(final ArrayList<XCom.VariableValue> evalVals, final M_Void_PStrBol<BSEList.ByteStreamEditor> method, final XCom.VariableValue par1, final boolean par2) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method
        for(final XCom.VariableStore chk : par1) {
            method.apply(bse, chk.value, par2);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void __xNotNull_RByt_Void(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RByt_Void<BSEList.ByteStreamEditor> method) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        retVal.add( new XCom.VariableStore( true, String.valueOf( method.apply(bse) ) ) );
    }

    private static void __xNotNull_RInt_Void(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RInt_Void<BSEList.ByteStreamEditor> method, boolean addIdxValOfs) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        final int res = method.apply(bse);                                    // Index starts from 1
        retVal.add( new XCom.VariableStore( true, String.valueOf( addIdxValOfs ? (res + 1) : res ) ) );
    }

    private static void __xNotNull_RInt_Void(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RInt_Void<BSEList.ByteStreamEditor> method) throws JXMException
    { __xNotNull_RInt_Void(evalVals, retVal, method, false); }

    private static void __xNotNull_RLng_Void(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RLng_Void<BSEList.ByteStreamEditor> method) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        retVal.add( new XCom.VariableStore( true, String.valueOf( method.apply(bse) ) ) );
    }

    private static void __xNotNull_RBig_Void(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RBig_Void<BSEList.ByteStreamEditor> method) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        retVal.add( new XCom.VariableStore( true, String.valueOf( method.apply(bse) ) ) );
    }

    private static void __xNotNull_RFlt_Void(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RFlt_Void<BSEList.ByteStreamEditor> method) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        retVal.add( new XCom.VariableStore( true, String.valueOf( method.apply(bse) ) ) );
    }

    private static void __xNotNull_RDbl_Void(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RDbl_Void<BSEList.ByteStreamEditor> method) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        retVal.add( new XCom.VariableStore( true, String.valueOf( method.apply(bse) ) ) );
    }

    private static void __xNotNull_RStr_Void(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RStr_Void<BSEList.ByteStreamEditor> method) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        retVal.add( new XCom.VariableStore( true, String.valueOf( method.apply(bse) ) ) );
    }

    private static void __xNotNull_RStr_PBol(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RStr_PBol<BSEList.ByteStreamEditor> method, final boolean par1) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        retVal.add( new XCom.VariableStore( true, String.valueOf( method.apply(bse, par1) ) ) );
    }

    private static void __xNotNull_RStr_PInt(final ArrayList<XCom.VariableValue> evalVals, final XCom.VariableValue retVal, final M_RStr_PInt<BSEList.ByteStreamEditor> method, final int par1) throws JXMException
    {
        // Get the BSE object
        final BSEList.ByteStreamEditor bse = __xGetBSE(evalVals);
        if(bse == null) return;

        // Call the method and store the result
        retVal.add( new XCom.VariableStore( true, String.valueOf( method.apply(bse, par1) ) ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_bse_new(final XCom.VariableValue retVal) throws JXMException
    {
        // Create a new BSE and store its handle
        retVal.add( new XCom.VariableStore( true, BSEList.bseNew() ) );
    }

    public static void _execute_bse_delete(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the BSE handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Delete the BSE
        BSEList.bseDelete(handle);
    }

    public static void _execute_bse_size(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RInt_Void(evalVals, retVal, BSEList.ByteStreamEditor::size); }

    public static void _execute_bse_cursor(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RInt_Void(evalVals, retVal, BSEList.ByteStreamEditor::cursor, true); }

    public static void _execute_bse_seek_abs(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_Void_PInt( evalVals, BSEList.ByteStreamEditor::seekBeg, __xGetFlattenedP1Int(evalVals, execBlock, execData) - 1 ); }

    public static void _execute_bse_seek_beg(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_Void_PInt( evalVals, BSEList.ByteStreamEditor::seekBeg, __xGetFlattenedP1Int(evalVals, execBlock, execData) ); }

    public static void _execute_bse_seek_end(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_Void_PInt( evalVals, BSEList.ByteStreamEditor::seekEnd, __xGetFlattenedP1Int(evalVals, execBlock, execData) ); }

    public static void _execute_bse_seek_cur(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_Void_PInt( evalVals, BSEList.ByteStreamEditor::seekCur, __xGetFlattenedP1Int(evalVals, execBlock, execData) ); }

    public static void _execute_bse_truncate(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_Void(evalVals, BSEList.ByteStreamEditor::truncate); }

    public static void _execute_bse_save_file(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Check if there are multiple destination files
        if( evalVals.get(1).size() != 1 ) throw XCom.newJXMRuntimeError(Texts.EMsg_WriteMultipleDstFile, "bse_save_file");

        // Execute the method
        __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::saveFile_jxm, evalVals.get(1) );
    }

    public static void _execute_bse_load_file(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::loadFile_jxm, evalVals.get(1) ); }

    public static void _execute_bse_save_b64s(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RStr_Void(evalVals, retVal, BSEList.ByteStreamEditor::saveBase64); }

    public static void _execute_bse_load_b64s(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::loadBase64, evalVals.get(1) ); }

    public static void _execute_bse_set_be(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_Void(evalVals, BSEList.ByteStreamEditor::setBE); }

    public static void _execute_bse_set_le(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_Void(evalVals, BSEList.ByteStreamEditor::setLE); }

    public static void _execute_bse_wr_byte(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrByte, evalVals.get(1) ); }

    public static void _execute_bse_rd_byte(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RByt_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdByte); }

    public static void _execute_bse_wr_uint08(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrUInt08, evalVals.get(1) ); }

    public static void _execute_bse_wr_sint08(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrSInt08, evalVals.get(1) ); }

    public static void _execute_bse_rd_uint08(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RInt_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdUInt08); }

    public static void _execute_bse_rd_sint08(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RInt_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdSInt08); }

    public static void _execute_bse_wr_uint16(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrUInt16, evalVals.get(1) ); }

    public static void _execute_bse_wr_sint16(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrSInt16, evalVals.get(1) ); }

    public static void _execute_bse_rd_uint16(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RInt_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdUInt16); }

    public static void _execute_bse_rd_sint16(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RInt_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdSInt16); }

    public static void _execute_bse_wr_uint32(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrUInt32, evalVals.get(1) ); }

    public static void _execute_bse_wr_sint32(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrSInt32, evalVals.get(1) ); }

    public static void _execute_bse_rd_uint32(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RLng_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdUInt32); }

    public static void _execute_bse_rd_sint32(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RLng_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdSInt32); }

    public static void _execute_bse_wr_uint64(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrUInt64, evalVals.get(1) ); }

    public static void _execute_bse_wr_sint64(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrSInt64, evalVals.get(1) ); }

    public static void _execute_bse_rd_uint64(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RBig_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdUInt64); }

    public static void _execute_bse_rd_sint64(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RLng_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdSInt64); }

    public static void _execute_bse_wr_flt32(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrFlt32, evalVals.get(1) ); }

    public static void _execute_bse_rd_flt32(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RFlt_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdFlt32); }

    public static void _execute_bse_wr_dbl64(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_XStr( evalVals, BSEList.ByteStreamEditor::wrDbl64, evalVals.get(1) ); }

    public static void _execute_bse_rd_dbl64(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_RDbl_Void(evalVals, retVal, BSEList.ByteStreamEditor::rdDbl64); }

    public static void _execute_bse_wr_utf8(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_Void_XStrBol( evalVals, BSEList.ByteStreamEditor::wrUTF8_jxm, evalVals.get(1), !__xGetFlattenedOptP2Bol(evalVals, execBlock, execData) ); }

    public static void _execute_bse_rd_utf8(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_RStr_PBol( evalVals, retVal, BSEList.ByteStreamEditor::rdUTF8_jxm, !__xGetFlattenedOptP2Bol(evalVals, execBlock, execData) ); }

    public static void _execute_bse_wr_utf16(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_Void_XStrBol( evalVals, BSEList.ByteStreamEditor::wrUTF16_jxm, evalVals.get(1), !__xGetFlattenedOptP2Bol(evalVals, execBlock, execData) ); }

    public static void _execute_bse_rd_utf16(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_RStr_PBol( evalVals, retVal, BSEList.ByteStreamEditor::rdUTF16_jxm, !__xGetFlattenedOptP2Bol(evalVals, execBlock, execData) ); }

    public static void _execute_bse_wr_utf32(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_Void_XStrBol( evalVals, BSEList.ByteStreamEditor::wrUTF32_jxm, evalVals.get(1), !__xGetFlattenedOptP2Bol(evalVals, execBlock, execData) ); }

    public static void _execute_bse_rd_utf32(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_RStr_PBol( evalVals, retVal, BSEList.ByteStreamEditor::rdUTF32_jxm, !__xGetFlattenedOptP2Bol(evalVals, execBlock, execData) ); }

    public static void _execute_bse_range_clr(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_Void_PInt( evalVals, BSEList.ByteStreamEditor::rangeClear, __xGetFlattenedP1Int(evalVals, execBlock, execData) ); }

    public static void _execute_bse_range_cpy(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_RStr_PInt( evalVals, retVal, BSEList.ByteStreamEditor::rangeCopy, __xGetFlattenedP1Int(evalVals, execBlock, execData) ); }

    public static void _execute_bse_range_cut(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { __xNotNull_RStr_PInt( evalVals, retVal, BSEList.ByteStreamEditor::rangeCut, __xGetFlattenedP1Int(evalVals, execBlock, execData) ); }

    public static void _execute_bse_range_pst(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_PStr( evalVals, BSEList.ByteStreamEditor::rangePaste, __xGetFlattenedP1Str(evalVals) ); }

    public static void _execute_bse_range_ovr(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { __xNotNull_Void_PStr( evalVals, BSEList.ByteStreamEditor::rangeOverwrite, __xGetFlattenedP1Str(evalVals) ); }

    public static void _execute_bse_vl_handle(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the BSE handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Check if the given handle is valid
        retVal.add( new XCom.VariableStore( true, BSEList.bseIsValidHandle(handle) ? XCom.Str_T : XCom.Str_F ) );
    }

} // class BSEUtil
