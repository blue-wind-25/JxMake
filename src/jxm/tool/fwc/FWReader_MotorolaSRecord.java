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
class FWReader_MotorolaSRecord extends FWReaderTextRL {

    private static final Pattern _pmParseLine = Pattern.compile("(^S)(\\d)(\\p{XDigit}{2})((?:\\p{XDigit}{2})*?)(\\p{XDigit}{2}$)");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean _checkChecksum(final byte[] addr_data, final int... others)
    {
        int chk = 0;

        for(int v : addr_data) chk += v & 0xFF;
        for(int v : others   ) chk += v;

        return ( (chk & 0xFF) == 0xFF );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWReader_MotorolaSRecord(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.SREC_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.SREC_RName_S; }

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
            if( !startCode.equals("S") ) _throwInvalidRecLineStartCode();

            // Extract the values
            final char   recordType = ( matcher.group(2).length() == 1 ) ? matcher.group(2).charAt(0) : '\0';
            final int    byteCount  = Integer.parseInt  ( matcher.group(3), 16 );
            final byte[] addr_data  = FWUtil .parse2Hexs( matcher.group(4)     );
            final int    checksum   = Integer.parseInt  ( matcher.group(5), 16 );

            // Check the byte count
            if(addr_data.length != byteCount - 1) _throwInvalidRecLineByteCount();

            // Check the checksum
            if( !_checkChecksum( addr_data, byteCount, checksum ) ) _throwInvalidRecLineChecksum();

            // Process according to the record type
            long address  = -1;
            int  dataSkip = -1;

            switch(recordType) {

                // Header
                case FWUtil.SREC_RT_Header:
                    // ##### !!! TODO : Error if this record is not present? !!! #####
                    if( FWUtil.cnv2BEHexsToAddress(addr_data) != 0 ) _throwInvalidRecLineAddresss();
                    continue; // !!! This record type is IGNORED !!!

                // Data with 16-bit address
                case FWUtil.SREC_RT_Data16:
                    address  = FWUtil.cnv2BEHexsToAddress(addr_data);
                    dataSkip = 2;
                    break; // Process the data  below

                // Data with 24-bit address
                case FWUtil.SREC_RT_Data24:
                    address  = FWUtil.cnv3BEHexsToAddress(addr_data);
                    dataSkip = 3;
                    break; // Process the data  below

                // Data with 32-bit address
                case FWUtil.SREC_RT_Data32:
                    address  = FWUtil.cnv4BEHexsToAddress(addr_data);
                    dataSkip = 4;
                    break; // Process the data  below

                // Data record count with 16-bit counter
                case FWUtil.SREC_RT_Count16:
                    if( FWUtil.cnv2BEHexsToAddress(addr_data) == 0 ) _throwInvalidRecLineAddresss();
                    continue; // !!! This record type is IGNORED !!!

                // Data record count with 24-bit counter
                case FWUtil.SREC_RT_Count24:
                    if( FWUtil.cnv3BEHexsToAddress(addr_data) == 0 ) _throwInvalidRecLineAddresss();
                    continue; // !!! This record type is IGNORED !!!

                // Start address (termination) with 32-bit address
                case FWUtil.SREC_RT_StartAddress32:
                    // ##### !!! TODO : Error if this record is not present? !!! #####
                    _closeTextFile(); // Close the file so that all text after the this record will be ignored
                    continue; // !!! This record type is IGNORED !!!

                // Start address (termination) with 24-bit address
                case FWUtil.SREC_RT_StartAddress24:
                    // ##### !!! TODO : Error if this record is not present? !!! #####
                    _closeTextFile(); // Close the file so that all text after the this record will be ignored
                    continue; // !!! This record type is IGNORED !!!

                // Start address (termination) with 16-bit address
                case FWUtil.SREC_RT_StartAddress16:
                    // ##### !!! TODO : Error if this record is not present? !!! #####
                    _closeTextFile(); // Close the file so that all text after the this record will be ignored
                    continue; // !!! This record type is IGNORED !!!

                // Invalid record type
                default:
                    _throwInvalidRecLineRecType();
                    break;

            } // switch

            // Store the data bytes
            if( !_storeDataBlockBytes(recordLine, address, addr_data, dataSkip) ) break;

        } // while

        // Return the data bytes or null in case of EOF
        return _getDataBlockBytes();
    }

    @Override
    public long dataBlockStartAddress()
    { return _thisDataBlockStartAddress; }

} // class FWReader_MotorolaSRecord
