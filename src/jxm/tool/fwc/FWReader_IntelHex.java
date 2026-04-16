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
class FWReader_IntelHex extends FWReaderTextRL {

    private static final Pattern _pmParseLine = Pattern.compile("(^:)(\\p{XDigit}{2})(\\p{XDigit}{4})(\\p{XDigit}{2})((?:\\p{XDigit}{2})*?)(\\p{XDigit}{2}$)");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean checkChecksum(final byte[] data, final int... others)
    {
        int chk = 0;

        if(data != null) for(int v : data  ) chk += v & 0xFF;
                         for(int v : others) chk += v;

        return ( (chk & 0xFF) == 0x00 );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long _extendedSegmentAddress =  0;
    private long _extendedLinearAddress  =  0;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWReader_IntelHex(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.IHEX_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.IHEX_RName_S; }

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
            if( !matcher.find() || matcher.groupCount() != 6 ) _throwInvalidRecLineDFormat();

            // Get and check the start code
            final String startCode = matcher.group(1);
            if( !startCode.equals(":") ) _throwInvalidRecLineStartCode();

            // Extract the values
            final int    byteCount  = Integer.parseInt  ( matcher.group(2), 16 );
                  long   address    = Long   .parseLong ( matcher.group(3), 16 );
            final int    recordType = Integer.parseInt  ( matcher.group(4), 16 );
            final byte[] data       = FWUtil .parse2Hexs( matcher.group(5)     );
            final int    checksum   = Integer.parseInt  ( matcher.group(6), 16 );

            // Check the byte count
            if(data != null && data.length != byteCount) _throwInvalidRecLineByteCount();

            // Check the checksum
            if( !checkChecksum( data, byteCount, (int) (address >> 8) & 0xFF, (int) (address & 0xFF), recordType, checksum ) ) _throwInvalidRecLineChecksum();

            // Adjust the address
            address += _extendedSegmentAddress;
            address += _extendedLinearAddress;

            // Process according to the record type
            switch(recordType) {

                // Data
                case FWUtil.IHEX_RT_Data:
                    // Process it below
                    break;

                // End of file
                case FWUtil.IHEX_RT_EndOfFile:
                    // ##### !!! TODO : Error if this record is not present? !!! #####
                    if(byteCount != 0) _throwInvalidRecLineByteCount();
                    _closeTextFile(); // Close the file so that all text after the this record will be ignored
                    break;

                // Extended segment address
                case FWUtil.IHEX_RT_ExtendedSegmentAddress:
                    if(byteCount != 2) _throwInvalidRecLineByteCount();
                    _extendedSegmentAddress = FWUtil.cnv2BEHexsToAddress(data) << 4;
                    continue;

                // Start segment address
                case FWUtil.IHEX_RT_StartSegmentAddress:
                    if(byteCount != 4) _throwInvalidRecLineByteCount();
                    continue; // !!! This record type is IGNORED !!!

                // Extended linear address
                case FWUtil.IHEX_RT_ExtendedLinearAddress:
                    if(byteCount != 2) _throwInvalidRecLineByteCount();
                    _extendedLinearAddress = FWUtil.cnv2BEHexsToAddress(data) << 16;
                    continue;

                // Start linear address
                case FWUtil.IHEX_RT_StartLinearAddress:
                    if(byteCount != 4) _throwInvalidRecLineByteCount();
                    continue; // !!! This record type is IGNORED !!!

                // Invalid record type
                default:
                    _throwInvalidRecLineRecType();
                    break;

            } // switch

            // Store the data bytes
            if( data != null && !_storeDataBlockBytes(recordLine, address, data, 0) ) break;

        } // while

        // Return the data bytes or null in case of EOF
        return _getDataBlockBytes();
    }

    @Override
    public long dataBlockStartAddress()
    { return _thisDataBlockStartAddress; }

} // class FWReader_IntelHex
