/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;

import jxm.*;
import jxm.annotation.*;


@package_private
class FWWriter_TITextHex extends FWWriter {

    public FWWriter_TITextHex(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.MOST_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.MOST_RName_S; }

    // NOTE : 'nullByte' is not currently used by 'TI-TXT Hex' writer!
    @Override
    public void writeDataBlocks(final ArrayList<FWBlock> fwBlocks, byte nullByte) throws IOException, JXMException
    {
        // ##### !!! TODO : Optimize !!! #####

        // Check the address space
        for(final FWBlock fwBlock : fwBlocks) {
            if( fwBlock.finalAddress() > FWUtil._64kiB_Min1 ) _throwAddrSpcTooLargeFFormat();
        }

        // Open the text file
        _openTextFile();

        // Write the data block(s)
        for(final FWBlock fwBlock : fwBlocks) {

            // Get the block's byte count and start address
                  int byteCount =       fwBlock.length();
            final int address   = (int) fwBlock.startAddress(); // NOTE : The 'TI-TXT Hex' format only support 16-bit address!

            // Write the section address
            // ##### !!! TODO : How if the address is not even? !!! #####

           final String strAddress = Integer.toHexString(address).toUpperCase();

            _bufferedTextWriter().write('@');
            for(int i = 0; i < 4 - strAddress.length(); ++i) _bufferedTextWriter().write('0'); // Pad with zeroes as needed
            _writeTextLine(strAddress );

            // Write the block's data
            int beg = 0;

            while(byteCount != 0) {

                // Get the subarray of the data
                final int    end  = Math.min(beg + FWUtil.TIXH_StdMaxRecLineDByteCnt, beg + byteCount);
                final int    len  = end - beg;
                final byte[] data = Arrays.copyOfRange( fwBlock.data(), beg, end );

                // Write the data
                FWUtil._putData2HexsWithSpaces( _bufferedTextWriter(), data );
                _bufferedTextWriter().newLine();

                // Update the counters
                byteCount -= len;
                beg       += len;

            } // while

        } // for

        // Write the 'termination' record
        _writeTextLine("Q");

        // Close the text file
        _closeTextFile();
    }

} // class FWWriter_TITextHex
