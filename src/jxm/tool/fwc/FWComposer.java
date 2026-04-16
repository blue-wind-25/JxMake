/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.regex.Pattern;

import jxm.*;
import jxm.annotation.*;


public class FWComposer {

    final ArrayList<FWBlock> _fwBlocks = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _sort()
    { _fwBlocks.sort( (o1, o2) -> o1.compareTo(o2) ); }

    public ArrayList<FWBlock> fwBlocks()
    { return _fwBlocks; }

    public FWComposer deepClone()
    {
        final FWComposer fwc = new FWComposer();

        for(final FWBlock fwb : this._fwBlocks) fwc._fwBlocks.add( fwb.deepClone() );

        return fwc;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class ELF_ISecRule extends SerializableDeepClone<ELF_ISecRule> {

        // NOTE : No '__0_JxMake_SerialVersionUID__' field here because it will be included in the 'ELF_ISecRules' class

                          public Pattern reName;
                          public String  compositeName;
        @DataFormat.Hex08 public long    addressOffset;
                          public String  appendAfter;

        public ELF_ISecRule()
        {
            reName        = null;
            compositeName = null;
            addressOffset = 0;
            appendAfter   = null;
        }

        public ELF_ISecRule(final String regexpNameStr_, final String compositeName_, final long addressOffset_, final String appendAfter_)
        {
            reName        = (regexpNameStr_ == null) ? null : Pattern.compile(regexpNameStr_);
            compositeName = compositeName_;
            addressOffset = addressOffset_;
            appendAfter   = appendAfter_;
        }

        public ELF_ISecRule(final String regexpNameStr, final String compositeName, final long addressOffset)
        { this(regexpNameStr, compositeName, addressOffset, null); }

        public ELF_ISecRule(final String regexpNameStr, final String compositeName)
        { this(regexpNameStr, compositeName, 0, null); }

        public ELF_ISecRule(final String regexpNameStr)
        { this(regexpNameStr, null, 0, null); }

        public ELF_ISecRule(final String regexpNameStr, final long addressOffset, final String appendAfter)
        { this(regexpNameStr, null, addressOffset, appendAfter); }

        public ELF_ISecRule(final String regexpNameStr, final long addressOffset)
        { this(regexpNameStr, null, addressOffset, null); }

    } // class ELF_ISecRule

    @SuppressWarnings("serial")
    public static class ELF_ESecRule extends SerializableDeepClone<ELF_ESecRule> {

        // NOTE : No '__0_JxMake_SerialVersionUID__' field here because it will be included in the 'ELF_ESecRules' class

        public Pattern reName;

        public ELF_ESecRule()
        { reName = null; }

        public ELF_ESecRule(final String regexpNameStr_)
        { reName = (regexpNameStr_ == null) ? null : Pattern.compile(regexpNameStr_); }

    } // class ELF_ESecRule

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class ELF_ISecRules extends ArrayList<ELF_ISecRule> {
        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        public ELF_ISecRules()
        {}

        public ELF_ISecRules(final List<ELF_ISecRule> rules)
        { super(rules); }
    }

    @SuppressWarnings("serial")
    public static class ELF_ESecRules extends ArrayList<ELF_ESecRule> {
        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        public ELF_ESecRules()
        {}

        public ELF_ESecRules(final List<ELF_ESecRule> rules)
        { super(rules); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : This example inclusion rule will only work for a very simple program
    public static ELF_ISecRules example_ELF_ISecRules_SAMD21(final long appCodeStartAddress)
    {
        return new ELF_ISecRules( Arrays.asList(
            new ELF_ISecRule("\\.text"     , ".code", appCodeStartAddress         ),
            new ELF_ISecRule("\\.ARM.exidx", ".code", 0                  , ".text"),
            new ELF_ISecRule("\\.data"     , ".code", 0                  , ".code")
        ) );
    }

    // NOTE : This example inclusion rule will only work for a very simple program
    public static ELF_ISecRules example_ELF_ISecRules_SAM3X8(final long appCodeStartAddress)
    {
        return new ELF_ISecRules( Arrays.asList(
            new ELF_ISecRule("\\.text"    , ".code", appCodeStartAddress         ),
            new ELF_ISecRule("\\.relocate", ".code", 0                  , ".text"),
            new ELF_ISecRule("\\.data"    , ".code", 0                  , ".code")
        ) );
    }

    // NOTE : This is the default exclusion rule
    public static ELF_ESecRules default_ELF_ESecRules()
    {
        return new ELF_ESecRules( Arrays.asList(
            //*
            new ELF_ESecRule("(\\.[a-zA-Z0-9_]+?)?\\.attributes"),
            new ELF_ESecRule("\\.bss"                           ),
            new ELF_ESecRule("\\.comment"                       ),
          //new ELF_ESecRule("\\.data"                          ),
            new ELF_ESecRule("\\.debug.*"                       ),
            new ELF_ESecRule("\\.eeprom"                        ),
            new ELF_ESecRule("\\.gnu.*"                         ),
            new ELF_ESecRule("\\.heap"                          ),
            new ELF_ESecRule("(\\.[a-zA-Z0-9_]+?)?\\.info"      ),
          //new ELF_ESecRule("\\.init"                          ),
          //new ELF_ESecRule("\\.literal"                       ),
            new ELF_ESecRule("\\.note.*"                        ),
            new ELF_ESecRule("\\.stack_dummy"                   ),
            new ELF_ESecRule("\\.[a-zA-Z0-9_]+tab$"             )
            //*/
        ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * ELF files from some architectures cannot be loaded and converted to binary files correctly.
     * These architectures are (but not limited to):
     *     # ESP8266 and all ESP32 variants, because their binary formats contain additional data.
     *     # Non MCU ELF files, because they contains complicated data.
     */
    public void loadELFBinaryFile(final String filePath, final long startAddressOffset, ELF_ISecRules includeSections, final ELF_ESecRules excludeSections) throws Exception
    {
        // Instantiate the reader
        final FWReader fwr = new FWReader_ELFBin(filePath, includeSections, excludeSections);

        // Clear all first
        clear();

        // Read and store the block(s)
        while(true) {

            // Read one block
            final byte[] data = fwr.readDataBlock();
            if(data == null) break;

            // Store one block
            _fwBlocks.add( new FWBlock( data, startAddressOffset + fwr.dataBlockStartAddress() ) );

            // Print some debugging information as needed
            if(FWUtil.PRINT_LOAD_SAVE_DEBUG) {
                SysUtil.stdDbg().printf( "%4s LOAD %08x %d\n", fwr.__S_RNAME(), _fwBlocks.get(_fwBlocks.size() - 1 ).startAddress(), _fwBlocks.get(_fwBlocks.size() - 1 ).length() );
            }

        } // while

        // Sort the blocks
        _sort();
    }

    public void loadELFBinaryFile(final String filePath, final long startAddressOffset, ELF_ISecRules includeSections) throws Exception
    { loadELFBinaryFile( filePath, startAddressOffset, includeSections, default_ELF_ESecRules() ); }

    public void loadELFBinaryFile(final String filePath, ELF_ISecRules includeSections) throws Exception
    { loadELFBinaryFile( filePath, 0, includeSections, default_ELF_ESecRules() ); }

    public void loadELFBinaryFile(final String filePath, final long startAddressOffset, final ELF_ESecRules excludeSections) throws Exception
    { loadELFBinaryFile( filePath, startAddressOffset, null, excludeSections ); }

    public void loadELFBinaryFile(final String filePath, final ELF_ESecRules excludeSections) throws Exception
    { loadELFBinaryFile( filePath, 0, null, excludeSections ); }

    public void loadELFBinaryFile(final String filePath, final long startAddressOffset) throws Exception
    { loadELFBinaryFile( filePath, startAddressOffset, null, default_ELF_ESecRules() ); }

    public void loadELFBinaryFile(final String filePath) throws Exception
    { loadELFBinaryFile( filePath, 0, null, default_ELF_ESecRules() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadRawBinaryData(final byte[] data, final long startAddressOffset)
    {
        // Clear all first
        clear();

        // Store one whole block
        _fwBlocks.add( new FWBlock( data, startAddressOffset ) );

        // Print some debugging information as needed
        if(FWUtil.PRINT_LOAD_SAVE_DEBUG) {
            SysUtil.stdDbg().printf( "%4s LOAD %08x %d\n", FWUtil.RBIN_RName_S, _fwBlocks.get(_fwBlocks.size() - 1 ).startAddress(), _fwBlocks.get(_fwBlocks.size() - 1 ).length() );
        }
    }

    public void loadRawBinaryFile(final String filePath, final long startAddressOffset) throws Exception
    {
        // Clear all first
        clear();

        // Instantiate the reader
        final FWReader fwr = new FWReader_RawBin(filePath);

        // Read and store one whole block
        _fwBlocks.add( new FWBlock( fwr.readDataBlock(), startAddressOffset ) );

        // Print some debugging information as needed
        if(FWUtil.PRINT_LOAD_SAVE_DEBUG) {
            SysUtil.stdDbg().printf( "%4s LOAD %08x %d\n", fwr.__S_RNAME(), _fwBlocks.get(_fwBlocks.size() - 1 ).startAddress(), _fwBlocks.get(_fwBlocks.size() - 1 ).length() );
        }
    }

    public void loadRawBinaryFile(final String filePath) throws Exception
    { loadRawBinaryFile(filePath, 0); }

    public void saveRawBinaryFile(final String filePath, byte nullByte) throws Exception
    {
        // Instantiate the writer
        final FWWriter fww = new FWWriter_RawBin(filePath);

        // Read and store the block(s)
        fww.writeDataBlocks(_fwBlocks, nullByte);
    }

    public void saveRawBinaryFile(final String filePath) throws Exception
    { saveRawBinaryFile( filePath, (byte) 0 ); }

    // ##### ??? TODO : Export this as a JxMake built-in function that returns a new 'byte-stream-editor' object ??? #####
    public byte[] getFlattenedBinaryData(final byte nullByte) throws Exception
    {
        // Instantiate the writer
        final FWWriter fww = new FWWriter_RawBin(null);

        // Read and store the block(s)
        fww.writeDataBlocks(_fwBlocks, nullByte);

        // Return the byte array
        return fww._getBinaryByteArray();
    }

    // ##### ??? TODO : Export this as a JxMake built-in function that returns a new 'byte-stream-editor' object ??? #####
    public byte[] getFlattenedBinaryData() throws Exception
    { return getFlattenedBinaryData( (byte) 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _loadTextFile(final FWReader fwr, final long startAddressOffset) throws Exception
    {
        // Clear all first
        clear();

        // Read and store the block(s)
        while(true) {

            // Read one block
            final byte[] data = fwr.readDataBlock();
            if(data == null) break;

            // Store one block
            _fwBlocks.add( new FWBlock( data, startAddressOffset + fwr.dataBlockStartAddress() ) );

            // Print some debugging information as needed
            if(FWUtil.PRINT_LOAD_SAVE_DEBUG) {
                SysUtil.stdDbg().printf( "%4s LOAD %08x %d\n", fwr.__S_RNAME(), _fwBlocks.get(_fwBlocks.size() - 1 ).startAddress(), _fwBlocks.get(_fwBlocks.size() - 1 ).length() );
            }

        } // while

        // Sort the blocks
        _sort();
    }

    private void _saveTextFile(final FWWriter fww, byte nullByte) throws Exception
    { fww.writeDataBlocks(_fwBlocks, nullByte); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadIntelHexFile(final String filePath, final long startAddressOffset) throws Exception
    { _loadTextFile( new FWReader_IntelHex(filePath), startAddressOffset ); }

    public void loadIntelHexFile(final String filePath) throws Exception
    { loadIntelHexFile(filePath, 0); }

    public void saveIntelHexFile(final String filePath, byte nullByte) throws Exception
    { _saveTextFile( new FWWriter_IntelHex(filePath), nullByte ); }

    public void saveIntelHexFile(final String filePath) throws Exception
    { saveIntelHexFile( filePath, (byte) 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadMotorolaSRecordFile(final String filePath, final long startAddressOffset) throws Exception
    { _loadTextFile( new FWReader_MotorolaSRecord(filePath), startAddressOffset ); }

    public void loadMotorolaSRecordFile(final String filePath) throws Exception
    { loadMotorolaSRecordFile(filePath, 0); }

    public void saveMotorolaSRecordFile(final String filePath, byte nullByte) throws Exception
    { _saveTextFile( new FWWriter_MotorolaSRecord(filePath), nullByte ); }

    public void saveMotorolaSRecordFile(final String filePath) throws Exception
    { saveMotorolaSRecordFile( filePath, (byte) 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadTektronixHexFile(final String filePath, final long startAddressOffset) throws Exception
    { _loadTextFile( new FWReader_TektronixHex(filePath), startAddressOffset ); }

    public void loadTektronixHexFile(final String filePath) throws Exception
    { loadTektronixHexFile(filePath, 0); }

    public void saveTektronixHexFile(final String filePath, byte nullByte) throws Exception
    { _saveTextFile( new FWWriter_TektronixHex(filePath), nullByte ); }

    public void saveTektronixHexFile(final String filePath) throws Exception
    { saveTektronixHexFile( filePath, (byte) 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadMOSTechnologyFile(final String filePath, final long startAddressOffset) throws Exception
    { _loadTextFile( new FWReader_MOSTechnology(filePath), startAddressOffset ); }

    public void loadMOSTechnologyFile(final String filePath) throws Exception
    { loadMOSTechnologyFile(filePath, 0); }

    public void saveMOSTechnologyFile(final String filePath, byte nullByte) throws Exception
    { _saveTextFile( new FWWriter_MOSTechnology(filePath), nullByte ); }

    public void saveMOSTechnologyFile(final String filePath) throws Exception
    { saveMOSTechnologyFile( filePath, (byte) 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadTITextHexFile(final String filePath, final long startAddressOffset) throws Exception
    { _loadTextFile( new FWReader_TITextHex(filePath), startAddressOffset ); }

    public void loadTITextHexFile(final String filePath) throws Exception
    { loadTITextHexFile(filePath, 0); }

    public void saveTITextHexFile(final String filePath, byte nullByte) throws Exception
    { _saveTextFile( new FWWriter_TITextHex(filePath), nullByte ); }

    public void saveTITextHexFile(final String filePath) throws Exception
    { saveTITextHexFile( filePath, (byte) 0 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadASCIIHexFile(final String filePath, final long startAddressOffset) throws Exception
    { _loadTextFile( new FWReader_ASCIIHex(filePath), startAddressOffset ); }

    public void loadASCIIHexFile(final String filePath) throws Exception
    { loadASCIIHexFile(filePath, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void loadVerilogVMemFile(final String filePath, final long startAddressOffset) throws Exception
    { _loadTextFile( new FWReader_VerilogVMem(filePath), startAddressOffset ); }

    public void loadVerilogVMemFile(final String filePath) throws Exception
    { loadVerilogVMemFile(filePath, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void clear()
    { _fwBlocks.clear(); }

    // ##### !!! TODO : Add 'equals()' with normalize and ignore start address offset (without flattening) ? !!! #####

    public boolean equals(final FWComposer fwComposer, final boolean flattened)
    {
        // Compare directly (without flattening)
        if(!flattened) {

            if( _fwBlocks.size() != fwComposer._fwBlocks.size() ) return false;

            for(int i = 0; i < _fwBlocks.size(); ++i) {
                if( !_fwBlocks.get(i).equals( fwComposer._fwBlocks.get(i) ) ) return false;
            }

            return true;

        }

        // Flatten then compare
        else {

            if( fwBlocks().get(0).startAddress() != fwComposer.fwBlocks().get(0).startAddress() ) return false;

            try {
                final byte[] a = this      .getFlattenedBinaryData( (byte) 0 );
                final byte[] b = fwComposer.getFlattenedBinaryData( (byte) 0 );
                return Arrays.equals(a, b);
            }
            catch(final Exception e) {
                return false;
            }

        }
    }

    public boolean equals(final FWComposer fwComposer)
    { return equals(fwComposer, false); }

    public long minStartAddress()
    {
        long res = Long.MAX_VALUE;

        for(final FWBlock ref : _fwBlocks) {
            res = Math.min( res, ref.startAddress() );
        }

        return (res == Long.MAX_VALUE) ? -1 : res;
    }

    public long maxFinalAddress()
    {
        long res = Long.MIN_VALUE;

        for(final FWBlock ref : _fwBlocks) {
            res = Math.max( res, ref.finalAddress() );
        }

        return (res == Long.MIN_VALUE) ? -1 : res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean compose(final FWComposer fwComposer, final long addressOffset)
    {
        // Cannot compose if there is any overlap between the blocks
        for(final FWBlock ref : _fwBlocks) {
            for(final FWBlock chk : fwComposer._fwBlocks) {
                if( ref.overlapWith(chk, addressOffset) ) return false;
            }
        }

        // Compose (combine/add the blocks)
        for(final FWBlock add : fwComposer._fwBlocks) {

            // Try append before/after first
            boolean added = false;

            for(final FWBlock cur : _fwBlocks) {
                if( cur.appendBefore(add, addressOffset) ) { added = true; break; }
                if( cur.appendAfter (add, addressOffset) ) { added = true; break; }
            }

            if(added) continue;

            // Simply add to the list
            _fwBlocks.add( new FWBlock(add, addressOffset) );

        } // for

        // Sort the blocks
        _sort();

        // Done
        return true;
    }

    public boolean compose(final FWComposer fwComposer)
    { return compose(fwComposer, 0); }

    public FWComposer decomposeRange(final long begAddress, final long endAddress)
    {
        final ArrayList<FWBlock> inc = new ArrayList<>();
        final FWComposer         exc = new FWComposer();

        for(final FWBlock fwb : _fwBlocks) {

            // Cannot decompose if there is any overlap between the addresses
            if( fwb.startAddress() <  begAddress && fwb.finalAddress() >= begAddress && fwb.finalAddress() <= endAddress ) return null;
            if( fwb.finalAddress() >  endAddress && fwb.startAddress() >= begAddress && fwb.startAddress() <= endAddress ) return null;
            if( fwb.startAddress() <  begAddress &&                                     fwb.finalAddress() >  endAddress ) return null;

            // Extract the block if it is inside the range
            if( fwb.startAddress() >= begAddress && fwb.finalAddress() <= endAddress ) {
                exc._fwBlocks.add(fwb);
            }
            // Otherwise, do not extract the block
            else {
                inc.add(fwb);
            }

        } // for

        // Store only the non decomposed blocks
        _fwBlocks.clear();
        _fwBlocks.addAll(inc);

        // Return the FWComposer instance that contains the decomposed blocks
        return exc;
    }

} // class FWComposer
