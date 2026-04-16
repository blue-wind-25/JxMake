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
class FWWriter_MOSTechnology extends FWWriter {

    private static int _calcChecksum(final int length, final long address, final byte[] data)
    {
        int chk = 0;

        chk +=       (length  >> 8) & 0xFF;
        chk +=       (length      ) & 0xFF;
        chk += (int) (address >> 8) & 0xFF;
        chk += (int) (address     ) & 0xFF;

        for(int v : data) chk += v & 0xFF;

        return (chk & 0xFFFF);
    }

    private void _putStartCode(final StringBuilder dst)
    {
        dst.setLength(0);
        dst.append(';');
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWWriter_MOSTechnology(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.MOST_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.MOST_RName_S; }

    // NOTE : 'nullByte' is not currently used by 'MOS Technology' writer!
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
        final StringBuilder recordLine = new StringBuilder();

        for(final FWBlock fwBlock : fwBlocks) {

            // Get the block's byte count and start address
            int byteCount =       fwBlock.length();
            int address   = (int) fwBlock.startAddress(); // NOTE : The 'MOS Technology' format only support 16-bit address!

            // Write the block's data
            int beg = 0;

            while(byteCount != 0) {

                // Get the subarray of the data
                final int    end  = Math.min(beg + FWUtil.MaxRecLineDByteCnt, beg + byteCount);
                final int    len  = end - beg;
                final byte[] data = Arrays.copyOfRange( fwBlock.data(), beg, end );

                // Calculate the checksum
                final int checksum = _calcChecksum(len, address, data);

                // Prepare the data record
                       _putStartCode  (recordLine                                               );
                FWUtil._putValues2Hexs(recordLine, len , (address  >> 8) & 0xFF, address  & 0xFF);
                FWUtil._putData2Hexs  (recordLine, data, (checksum >> 8) & 0xFF, checksum & 0xFF);

                // Write the 'data' record
                _writeTextLine( recordLine.toString() );

                // Update the counters
                byteCount -= len;
                address   += len;
                beg       += len;

            } // while

        } // for

        // Write the 'termination' record
        _writeTextLine(";0000010001");

        // Close the text file
        _closeTextFile();
    }

} // class FWWriter_MOSTechnology
