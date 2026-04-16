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
class FWReader_ASCIIHex extends FWReaderTextRLSB {

    private static final Pattern _pmParseLine     = Pattern.compile("(?:^\\$A(\\p{XDigit}+)[\\s+%',]*?[,.][\\s+%',]*?)?(.+)$");
    private static final Pattern _pmRemoveCheksum = Pattern.compile("[\\s+%',]*\\$S\\p{XDigit}{4}[\\s+%',]*"                 );
    private static final Pattern _pmSeparator     = Pattern.compile("[\\s+%',]"                                              );

    private static final String  __STX__          = "\u0002";
    private static final String  __ETX__          = "\u0003";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final StringBuilder _recordLine     = new StringBuilder();
    private long                _sectionAddress = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWReader_ASCIIHex(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.ASCH_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.ASCH_RName_S; }

    @Override
    public byte[] readDataBlock() throws Exception
    {
        // https://hexed.it

        // Open the text file as needed
        _openTextFileAsNeeded();

        // Check if there is a data block saved from the previous function call
        final byte[] sbData = _getSavedDataBlockBytes();

        if(sbData != null) return sbData; // Return the saved data bytes

        // Process the record lines
        while(true) {

            // Read one line
            final String line = _readRecordLine(true);

            if( line == null && _recordLine.length() == 0 ) break;
            if( line != null && line.isEmpty()            ) continue;

            // Read and combine the lines
            if(line != null) _recordLine.append(line);

            final int idxSTX = _recordLine.indexOf(__STX__);
            final int idxETX = _recordLine.indexOf(__ETX__);

            String combLine = null;

            if(idxSTX >= 0 && idxETX >= 0) {
                if(idxETX <= idxSTX) _throwInvalidRecLineDFormat();
                combLine = _recordLine.substring(idxSTX + 1, idxETX    ).trim(); // Get the record
                           _recordLine.delete   (0         , idxETX + 1)       ; // Remove the consummed characters
            }

            if(combLine == null) continue;

            if( _recordLine.indexOf("$S") > 0 ) {
                // Remove the checksum part
                final String removed = _pmRemoveCheksum.matcher( _recordLine.toString() ).replaceAll("");
                _recordLine.setLength(0);
                _recordLine.append(removed);
            }

            // Parse the line
            final Matcher matcher = _pmParseLine.matcher(combLine);
            if( !matcher.find() || matcher.groupCount() != 2 ) _throwInvalidRecLineDFormat();

            // Extract the values (remove the checksum part as needed)
            final String strMGrpD = matcher.group(2);
                  String dataLine = ( strMGrpD.indexOf("$S") < 0 ) ? strMGrpD : _pmRemoveCheksum.matcher(strMGrpD).replaceAll("");
                         dataLine = _pmSeparator.matcher(dataLine).replaceAll("");

                         _sectionAddress = Long   .parseLong ( matcher.group(1), 16 );
            final byte[] data            = FWUtil .parse2Hexs( dataLine             );

            // Store the data bytes
            if( !_storeOrSaveDataBlockBytes(_sectionAddress, data) ) break;

        } // while

        // Return the data bytes or null in case of EOF
        return _getDataBlockBytes();
    }

    @Override
    public long dataBlockStartAddress()
    { return _thisDataBlockStartAddress; }

} // class FWReader_ASCIIHex
