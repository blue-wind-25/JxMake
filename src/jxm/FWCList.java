/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.HashMap;

import jxm.*;
import jxm.tool.*;
import jxm.tool.fwc.*;


public class FWCList {

    //                           Handle  Value
    private static final HashMap<String, FWComposer> _fwcList      = new HashMap<>();
    private static       long                        _fwcHandleCnt = 0;

    private static final long                        FWCIDMMask    = SysUtil.random32();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized static String _newFWCHandle()
    { return SysUtil.createHandleID("fwcomposer", ++_fwcHandleCnt, FWCIDMMask); }

    public synchronized static boolean fwcIsValidHandle(final String handle)
    { return SysUtil.isValidHandleID("fwcomposer", handle) && _fwcList.containsKey(handle); }

    public synchronized static String fwcNew()
    {
        final String     handle = _newFWCHandle();
        final FWComposer newFWC = new FWComposer();

        _fwcList.put(handle, newFWC);

        return handle;
    }

    public synchronized static boolean fwcDelete(final String handle)
    { return _fwcList.remove(handle) != null; }

    public synchronized static FWComposer fwcGet(final String handle)
    { return _fwcList.get(handle); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadRawBinaryFile(final String handle, final String filePath, final long startAddressOffset) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.loadRawBinaryFile(filePath, startAddressOffset);

        return true;
    }

    public synchronized static boolean fwcSaveRawBinaryFile(final String handle, final String filePath, byte nullByte) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.saveRawBinaryFile(filePath, nullByte);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadELFBinaryFile(final String handle, final String filePath, final long startAddressOffset, final String isrJSONStr, final String esrJSONStr) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        final FWComposer.ELF_ISecRules isr = isrJSONStr.isEmpty()                        ? null
                                           :                                               JSONDecoder.deserialize(isrJSONStr, FWComposer.ELF_ISecRules.class);

        final FWComposer.ELF_ESecRules esr = esrJSONStr.isEmpty()                        ? null
                                           : esrJSONStr.toUpperCase().equals ("DEFAULT") ? FWComposer.default_ELF_ESecRules()
                                           :                                               JSONDecoder.deserialize(esrJSONStr, FWComposer.ELF_ESecRules.class);

        fwc.loadELFBinaryFile(filePath, startAddressOffset, isr, esr);

        /*
        final byte[] fwDataBuff     =       fwc.getFlattenedBinaryData( (byte) 0xFF );
        final int    fwStartAddress = (int) fwc.fwBlocks().get(0).startAddress();
        final int    fwLength       =       fwDataBuff.length;
              int    i              = 0;
              int    z              = 0;
                            SysUtil.stdDbg().printf ("----- %s -----\n", filePath);
        SysUtil.stdDbg().printf("Blk# Address  Size\n");
        for( final FWBlock f : fwc.fwBlocks() ) {
            SysUtil.stdDbg().printf( "[%02d] %08X %04X (%d)\n", i++, f.startAddress(), f.length(), f.length() );
            z += f.length();
        }
        SysUtil.stdDbg().println();
        SysUtil.stdDbg().printf ("[Σ∊] %08X %04X (%d)\n", fwStartAddress, z       , z       );
        SysUtil.stdDbg().printf ("[Σᶠ] %08X %04X (%d)\n", fwStartAddress, fwLength, fwLength);
        SysUtil.stdDbg().println();
        //*/

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadIntelHexFile(final String handle, final String filePath, final long startAddressOffset) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.loadIntelHexFile(filePath, startAddressOffset);

        return true;
    }

    public synchronized static boolean fwcSaveIntelHexFile(final String handle, final String filePath, byte nullByte) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.saveIntelHexFile(filePath, nullByte);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadMotorolaSRecordFile(final String handle, final String filePath, final long startAddressOffset) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.loadMotorolaSRecordFile(filePath, startAddressOffset);

        return true;
    }

    public synchronized static boolean fwcSaveMotorolaSRecordFile(final String handle, final String filePath, byte nullByte) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.saveMotorolaSRecordFile(filePath, nullByte);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadTektronixHexFile(final String handle, final String filePath, final long startAddressOffset) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.loadTektronixHexFile(filePath, startAddressOffset);

        return true;
    }

    public synchronized static boolean fwcSaveTektronixHexFile(final String handle, final String filePath, byte nullByte) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.saveTektronixHexFile(filePath, nullByte);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadMOSTechnologyFile(final String handle, final String filePath, final long startAddressOffset) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.loadMOSTechnologyFile(filePath, startAddressOffset);

        return true;
    }

    public synchronized static boolean fwcSaveMOSTechnologyFile(final String handle, final String filePath, byte nullByte) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.saveMOSTechnologyFile(filePath, nullByte);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadTITextHexFile(final String handle, final String filePath, final long startAddressOffset) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.loadTITextHexFile(filePath, startAddressOffset);

        return true;
    }

    public synchronized static boolean fwcSaveTITextHexFile(final String handle, final String filePath, byte nullByte) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.saveTITextHexFile(filePath, nullByte);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadASCIIHexFile(final String handle, final String filePath, final long startAddressOffset) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.loadASCIIHexFile(filePath, startAddressOffset);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcLoadVerilogVMemFile(final String handle, final String filePath, final long startAddressOffset) throws Exception
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.loadVerilogVMemFile(filePath, startAddressOffset);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean fwcClear(final String handle)
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return false;

        fwc.clear();

        return true;
    }

    public synchronized static boolean fwcEquals(final String handle1, final String handle2, final boolean flattened)
    {
        final FWComposer fwc1 = _fwcList.get(handle1);
        if(fwc1 == null) return false;

        final FWComposer fwc2 = _fwcList.get(handle2);
        if(fwc2 == null) return false;

        return fwc1.equals(fwc2, flattened);
    }

    public synchronized static long fwcMinStartAddress(final String handle)
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return -1;

        return fwc.minStartAddress();
    }

    public synchronized static long fwcMaxFinalAddress(final String handle)
    {
        final FWComposer fwc = _fwcList.get(handle);
        if(fwc == null) return -1;

        return fwc.maxFinalAddress();
    }

    public synchronized static boolean fwcCompose(final String handleDst, final String handleSrc, final long addressOffset)
    {
        final FWComposer fwcDst = _fwcList.get(handleDst);
        if(fwcDst == null) return false;

        final FWComposer fwcSrc = _fwcList.get(handleSrc);
        if(fwcSrc == null) return false;

        return fwcDst.compose(fwcSrc, addressOffset);
    }

} // class FWCList
