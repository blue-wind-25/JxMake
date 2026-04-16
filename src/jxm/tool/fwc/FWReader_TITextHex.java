/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.IOException;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


@package_private
class FWReader_TITextHex extends FWReaderTextRL {

    private int _sectionAddress = -1;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWReader_TITextHex(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.TIXH_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.TIXH_RName_S; }

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

            final char rlfChr = recordLine.charAt(0);

            // Check if the record specify the start address of a section
            if( rlfChr == '@' ) {
                _sectionAddress = Integer.parseInt( recordLine.substring(1), 16 );
                continue;
            }

            // Check if the record specify a termination marker
            if( recordLine.length() == 1 && Character.toUpperCase(rlfChr) == 'Q' ){
                _closeTextFile(); // Close the file so that all text after the this record will be ignored
                continue;
            }

            // Process the data
            if(_sectionAddress < 0) _throwInvalidRecLineDFormat();

            String dataLine = recordLine;
            int    address  = _sectionAddress; // NOTE : The 'MOS Technology' format only support 16-bit address!

            while(true) {

                // Extract the data
                final byte[]  data         = FWUtil.parse2Hexs( XCom.re_removeAllWhitespaces(dataLine) );
                      boolean sectionBreak = (data.length < FWUtil.TIXH_StdMaxRecLineDByteCnt);

                if(data == null) _throwInvalidRecLineDFormat();

                // Read the next data line and check
                dataLine = _readRecordLine();

                // If the next data line actually specifies a directive, store back the data line and set the section-break flag
                if(dataLine != null) {
                    final char dlfChr = dataLine.charAt(0);
                    if( dlfChr == '@' ||  Character.toUpperCase(dlfChr) == 'Q' ) {
                        if( !_storeBackOneRecordLine(dataLine) ) _throwInvalidRecLineDFormat();
                        sectionBreak = true;
                    }
                }

                // Store the data bytes
                _storeDataBlockBytes(address, data, sectionBreak);

                // If the section-break flag is set, reset the section address and break
                if(sectionBreak) {
                    _sectionAddress = -1;
                    break;
                }

                // Increment the address
                address += data.length;

            } // while

            // Break on section change
            if(_sectionAddress < 0) break;

        } // while

        // Return the data bytes or null in case of EOF
        return _getDataBlockBytes();
    }

    @Override
    public long dataBlockStartAddress()
    { return _thisDataBlockStartAddress; }

} // class FWReader_TITextHex
