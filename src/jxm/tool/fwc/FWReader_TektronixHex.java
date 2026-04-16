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
class FWReader_TektronixHex extends FWReaderTextRL {

    private static final Pattern _pmParseLineS = Pattern.compile("^/(\\p{XDigit}{4})(\\p{XDigit}{2})(\\p{XDigit}{2})((?:\\p{XDigit}{2})*?)((?:\\p{XDigit}{2})?$)");
    private static final Pattern _pmParseLineX = Pattern.compile("^%(\\p{XDigit}{2})(\\d)(\\p{XDigit}{2})(\\d)(.*$)");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean _checkChecksum1(final long address, final int byteCount, final int checksum1)
    {
        int chk = 0;

        chk += (int) (address   >> 12) & 0x0F;
        chk += (int) (address   >>  8) & 0x0F;
        chk += (int) (address   >>  4) & 0x0F;
        chk += (int) (address        ) & 0x0F;
        chk +=       (byteCount >>  4) & 0x0F;
        chk +=       (byteCount      ) & 0x0F;

        return ( (chk & 0xFF) == checksum1 );
    }

    private static boolean _checkChecksum2(final byte[] data, final int checksum2)
    {
        if(data == null) return true;

        int chk = 0;

        for(int v : data) chk += v & 0xFF;

        return ( (chk & 0xFF) == checksum2 );
    }

    private static boolean _checkChecksumX(final int blockLength, final char blockType, final int addressLength, final long address, final byte[] data, final int checksumX)
    {
        int chk = 0;

        chk += (blockLength >>  4) & 0x0F;
        chk += (blockLength      ) & 0x0F;
        chk += FWUtil.hexChrToDec(blockType);
        chk += addressLength;

        long addr = address;
        for(int i = 0; i < addressLength; ++i) {
            chk   += (int) (addr & 0x0F);
            addr >>= 4;
        }

        if(data != null) {
            for(int v : data) {
                chk += (v >> 4) & 0x0F;
                chk += (v     ) & 0x0F;
            }
        }

        return ( (chk & 0xFF) == checksumX );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWReader_TektronixHex(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.THEX_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.THEX_RName_S; }

    @Override
    public byte[] readDataBlock() throws Exception
    {
        // Open the text file as needed
        _openTextFileAsNeeded();

        // Process the record lines
        char tag = 0;

        while(true) {

            // Read one record line
            final String recordLine = _readRecordLine();

            if( recordLine == null   ) break;
            if( recordLine.isEmpty() ) continue;

            // Determine if the file is in 'Tektronix Hex' or 'Extended Tektronix Hex' format
            if(tag == 0) {
                tag = recordLine.charAt(0);
                if(tag != '/' && tag != '%') _throwInvalidRecLineDFormat();
            }

            // Variables for holding the values
            long   address = 0;
            byte[] data    = null;

            // Parse 'Tektronix Hex'
            if(tag == '/') {
                // Parse the record line
                final Matcher matcher = _pmParseLineS.matcher(recordLine);
                if( !matcher.find() || matcher.groupCount() != 5 ) _throwInvalidRecLineDFormat();
                // Extract the values
                                  address      =                       Long   .parseLong ( matcher.group(1), 16 );
                    final int     dataLength   =                       Integer.parseInt  ( matcher.group(2), 16 );
                    final boolean isTermRecord = (dataLength == 0);
                    final int     checksum1    =                       Integer.parseInt  ( matcher.group(3), 16 );
                if(!isTermRecord) data         =                       FWUtil .parse2Hexs( matcher.group(4)     );
                    final int     checksum2    = (data == null) ? -1 : Integer.parseInt  ( matcher.group(5), 16 );
                // Check the byte count
                if(data != null && data.length != dataLength) _throwInvalidRecLineByteCount();
                // Check the 1st checksum and 2nd checksum
                if( !_checkChecksum1(address, dataLength, checksum1) ) _throwInvalidRecLineChecksum();
                if( !_checkChecksum2(data   ,             checksum2) ) _throwInvalidRecLineChecksum();
                // If it is a termination record, close the file so that all text after the this record will be ignored
                if(isTermRecord) _closeTextFile();
            }

            // Parse 'Extended Tektronix Hex'
            else {
                // Parse the record line
                final Matcher matcher = _pmParseLineX.matcher(recordLine);
                if( !matcher.find() || matcher.groupCount() != 5 ) _throwInvalidRecLineDFormat();
                // Extract the values
                final int     blockLength   = Integer.parseInt( matcher.group(1), 16 );
                final char    blockType     =                   matcher.group(2).charAt(0);
                final int     checksum      = Integer.parseInt( matcher.group(3), 16 );
                final boolean isDataRecord  = (blockType == FWUtil.THEX_RT_Data       );
                final boolean isTermRecord  = (blockType == FWUtil.THEX_RT_Termination);
                final boolean isUsedRecord  = (isDataRecord || isTermRecord);
                final int     addressLength = Integer.parseInt( matcher.group(4), 16 );
                final boolean parseAddress  = ( isUsedRecord && (addressLength != 0) );
                if(parseAddress) address = Long  .parseLong ( matcher.group(5).substring(0, addressLength), 16 );
                if(isDataRecord) data    = FWUtil.parse2Hexs( matcher.group(5).substring(addressLength   )     );
                // Check the record type
                if(!isUsedRecord && blockType != FWUtil.THEX_RT_ExtraInformation) _throwInvalidRecLineRecType();
                // Check the block length
                if( blockLength != recordLine.length() - 1 ) _throwInvalidRecLineByteCount();
                // Check the checksum as needed
                if( isUsedRecord && !_checkChecksumX(blockLength, blockType, addressLength, address, data, checksum) ) _throwInvalidRecLineChecksum();
                // If it is a termination record, close the file so that all text after the this record will be ignored
                if(isTermRecord) _closeTextFile();
            }

            // Store the data bytes
            if( data != null && !_storeDataBlockBytes(recordLine, address, data, 0) ) break;

        } // while

        // Return the data bytes or null in case of EOF
        return _getDataBlockBytes();
    }

    @Override
    public long dataBlockStartAddress()
    { return _thisDataBlockStartAddress; }

} // class FWReader_TektronixHex
