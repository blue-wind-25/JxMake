/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


@package_private
class FWReader_VerilogVMem extends FWReaderTextRLSB {

    private static final Pattern _pmParseLine = Pattern.compile("^@(\\p{XDigit}+)\\s+(.+)$");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final StringBuilder _recordLine     = new StringBuilder();
    private long                _sectionAddress = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWReader_VerilogVMem(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.VMEM_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.VMEM_RName_S; }

    @Override
    public byte[] readDataBlock() throws Exception
    {
        // Open the text file as needed
        _openTextFileAsNeeded();

        // Check if there is a data block saved from the previous function call
        final byte[] sbData = _getSavedDataBlockBytes();

        if(sbData != null) return sbData; // Return the saved data bytes

        // Process the record lines
        while(true) {

            // Read one line
            final String line = _readRecordLine();

            if( line == null && _recordLine.length() == 0 ) break;
            if( line != null && line.isEmpty()            ) continue;

            // Read and combine the lines
            if(line != null) {
                if( line.charAt(0) == '@') {
                    if( _recordLine.length() == 0 ) {
                        _recordLine.append(line);
                        _recordLine.append(' ' );
                        continue;
                    }
                    else {
                        if( !_storeBackOneRecordLine(line) ) _throwInvalidRecLineDFormat();
                    }
                }
                else {
                    _recordLine.append(line);
                    continue;
                }
            }

            final String combLine = _recordLine.toString();
            _recordLine.setLength(0);

            if( combLine.isEmpty() ) continue;

            // Parse the line
            final Matcher matcher = _pmParseLine.matcher(combLine);
            if( !matcher.find() || matcher.groupCount() != 2 ) _throwInvalidRecLineDFormat();

            // Extract the values
                         _sectionAddress = Long   .parseLong ( matcher.group(1), 16 );
            final byte[] data            = FWUtil .parse2Hexs( XCom.re_removeAllWhitespaces( matcher.group(2) ) );

            // Store the data bytes
            if( !_storeOrSaveDataBlockBytes(_sectionAddress, data) ) break;

        } // while

        // Return the data bytes or null in case of EOF
        return _getDataBlockBytes();
    }

    @Override
    public long dataBlockStartAddress()
    { return _thisDataBlockStartAddress; }

} // class FWReader_VerilogVMem
