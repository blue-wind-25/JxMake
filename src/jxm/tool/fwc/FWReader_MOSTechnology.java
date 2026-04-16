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


@package_private
class FWReader_MOSTechnology extends FWReaderTextRL {

    private static final Pattern _pmParseLine = Pattern.compile("(^;)(\\p{XDigit}{2})(\\p{XDigit}{4})((?:\\p{XDigit}{2})*?)(\\p{XDigit}{4}$)");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean _checkChecksum(final int length, final int address, final byte[] data, final int checksum)
    {
        int chk = 0;

        chk += (length  >> 8) & 0xFF;
        chk += (length      ) & 0xFF;
        chk += (address >> 8) & 0xFF;
        chk += (address     ) & 0xFF;

        if(data != null) for(int v : data) chk += v & 0xFF;

        return ( (chk & 0xFFFF) == checksum );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWReader_MOSTechnology(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.MOST_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.MOST_RName_S; }

    @Override
    public byte[] readDataBlock() throws Exception
    {
        // Open the text file as needed
        _openTextFileAsNeeded();

        // Process the record lines
        while(true) {

            // Read one record line
            final String recordLine = _readRecordLine();

            if( recordLine == null   ) break;
            if( recordLine.isEmpty() ) continue;

            // Parse the record line
            final Matcher matcher = _pmParseLine.matcher(recordLine);
            if( !matcher.find() || matcher.groupCount() != 5 ) _throwInvalidRecLineDFormat();

            // Get and check the start code
            final String startCode = matcher.group(1);
            if( !startCode.equals(";") ) _throwInvalidRecLineStartCode();

            // Extract the values
            final int     length       = Integer.parseInt  ( matcher.group(2), 16 );
            final int     address      = Integer.parseInt  ( matcher.group(3), 16 ); // NOTE : The 'MOS Technology' format only support 16-bit address!
            final byte[]  data         = FWUtil .parse2Hexs( matcher.group(4)     );
            final int     checksum     = Integer.parseInt  ( matcher.group(5), 16 );
            final boolean isTermRecord = (length == 0);

            // Check the byte count
            if(data != null && data.length != length) _throwInvalidRecLineByteCount();

            // Check the checksum
            if( !_checkChecksum(length, address, data, checksum) ) _throwInvalidRecLineChecksum();

            // Store the data bytes
            if( data != null && !_storeDataBlockBytes(recordLine, address, data, 0) ) break;

            // If it is a termination record, close the file so that all text after the this record will be ignored
            if(isTermRecord) _closeTextFile();

        } // while

        // Return the data bytes or null in case of EOF
        return _getDataBlockBytes();
    }

    @Override
    public long dataBlockStartAddress()
    { return _thisDataBlockStartAddress; }

} // class FWReader_MOSTechnology
