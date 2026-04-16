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
class FWWriter_TektronixHex extends FWWriter {

    private static int _calcChecksumStr(final String str)
    {
        int res = 0;

        for( int i = 0; i < str.length(); ++i ) res += FWUtil.hexChrToDec( str.charAt(i) );

        return res;
    }

    private static int _calcChecksumX(final String strBlockLength, final char blockType, final String strAddressLength, final String strAddress, final byte[] data)
    {
        int res = _calcChecksumStr  (strBlockLength  )
                + FWUtil.hexChrToDec(blockType       )
                + _calcChecksumStr  (strAddressLength)
                + _calcChecksumStr  (strAddress      );

        for(int v : data) {
            res += (v >> 4) & 0x0F;
            res += (v     ) & 0x0F;
        }

        return (res & 0xFF);
    }

    private void _putStartCode(final StringBuilder dst)
    {
        dst.setLength(0);
        dst.append('%');
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWWriter_TektronixHex(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.THEX_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.THEX_RName_S; }

    // NOTE : # 'nullByte' is not currently used by 'Tektronix Hex' writer!
    //        # This function will only write in 'Extended Tektronix Hex' format.
    @Override
    public void writeDataBlocks(final ArrayList<FWBlock> fwBlocks, byte nullByte) throws IOException, JXMException
    {
        // ##### !!! TODO : Optimize !!! #####

        // Open the text file
        _openTextFile();

        // Write the data block(s)
        final StringBuilder recordLine = new StringBuilder();

        for(final FWBlock fwBlock : fwBlocks) {

            // Get the block's byte count and start address
            int  byteCount = fwBlock.length();
            long address   = fwBlock.startAddress();

            // Write the block's data
            int beg = 0;

            while(byteCount != 0) {

                // Get the subarray of the data
                final int    end  = Math.min(beg + FWUtil.MaxRecLineDByteCnt, beg + byteCount);
                final int    len  = end - beg;
                final byte[] data = Arrays.copyOfRange( fwBlock.data(), beg, end );

                // Format the address, address length, and block length
                /*
                final String strAddress       = String.format( "%X", address                                               );
                final String strAddressLength = String.format( "%X",                 strAddress.length()                   );
                final String strBlockLength   = String.format( "%X", 2 + 1 + 2 + 1 + strAddress.length() + data.length * 2 );
                //*/
                //*
                // NOTE : This method seems a bit faster!
                final String strAddress       = Long   .toHexString(                 address                               ).toUpperCase();
                final String strAddressLength = Integer.toHexString(                 strAddress.length()                   ).toUpperCase();
                final String strBlockLength   = Integer.toHexString( 2 + 1 + 2 + 1 + strAddress.length() + data.length * 2 ).toUpperCase();
                //*/

                // Calculate the checksum
                final int checksum = _calcChecksumX(strBlockLength, FWUtil.THEX_RT_Data, strAddressLength, strAddress, data);

                // Store the start code
                _putStartCode(recordLine);

                // Store the block length
                recordLine.append(strBlockLength);

                // Store the block type
                recordLine.append(FWUtil.THEX_RT_Data);

                // Store the checksum
                FWUtil._put2Hexs(recordLine, checksum);

                // Store the address length
                recordLine.append(strAddressLength);

                // Store the address
                recordLine.append(strAddress);

                // Store the data
                FWUtil._putData2Hexs(recordLine, data);

                // Write the 'data' record
                _writeTextLine( recordLine.toString() );

                // Update the counters
                byteCount -= len;
                address   += len;
                beg       += len;

            } // while

        } // for

        // Write the 'termination' record
        _writeTextLine("%0781010");

        // Close the text file
        _closeTextFile();
    }

} // class FWWriter_TektronixHex
