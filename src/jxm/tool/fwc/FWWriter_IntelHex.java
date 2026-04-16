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


public class FWWriter_IntelHex extends FWWriter {

    public static int calcChecksum(final byte[] data, final int... others)
    {
        int res = 0;

        if(data != null) for(int v : data  ) res += v & 0xFF;
                         for(int v : others) res += v;

        return ( 0x100 - (res & 0xFF) ) & 0xFF;
    }

    private void _putStartCode(final StringBuilder dst)
    {
        dst.setLength(0);
        dst.append(':');
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWWriter_IntelHex(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.IHEX_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.IHEX_RName_S; }

    // NOTE : 'nullByte' is not currently used by 'Intel Hex' writer!
    @Override
    public void writeDataBlocks(final ArrayList<FWBlock> fwBlocks, byte nullByte) throws IOException, JXMException
    {
        // ##### !!! TODO : Optimize !!! #####

        // ##### !!! TODO : Allow this to be user selectable (convert this constant into a function parameter)? !!! #####
        final int addrSpaceSize = 0;

        // Determine whether to use 'extended segment address' or 'extended linear address'
        boolean ua20 = false;
        boolean ua32 = false;

        for(final FWBlock fwBlock : fwBlocks) {
            if( fwBlock.finalAddress() > FWUtil._64kiB_Min1 )    ua20 = true;
            if( fwBlock.finalAddress() > FWUtil._01MiB_Min1  ) { ua32 = true; break; }
        }

        if(addrSpaceSize != 0) {
                 if(addrSpaceSize == 16) { if(ua20 || ua32) _throwAddrSpcTooLargeFSetting(); ua20 = false; ua32 = false; }
            else if(addrSpaceSize == 20) { if(        ua32) _throwAddrSpcTooLargeFSetting(); ua20 =  true; ua32 = false; }
            else if(addrSpaceSize == 32) { /* No checking is needed here */                  ua20 =  true; ua32 =  true; }
            else                         { _throwAddrSpcInvalid(addrSpaceSize);                                          }
        } // if

        // Open the text file
        _openTextFile();

        // Write the data block(s)
        final StringBuilder recordLine   =  new StringBuilder();
              int           eaddressPrev = -1;

        for(final FWBlock fwBlock : fwBlocks) {

            // Get the block's byte count and various addresses
                  int  byteCount = fwBlock.length();
            final long laddress  = fwBlock.startAddress();                               // Long     [full   ] address
            final int  xaddress  = (int) ( laddress / FWUtil._64kiB                   ); // Extended [initial] address
                  int  raddress  = (int) ( laddress - (long) xaddress * FWUtil._64kiB ); // Running  [counter] address

            // Write the block's data
            int beg = 0;

            while(byteCount != 0) {

                // Get the subarray of the data
                final int    end  = Math.min(beg + FWUtil.MaxRecLineDByteCnt, beg + byteCount);
                final int    len  = end - beg;
                final byte[] data = Arrays.copyOfRange( fwBlock.data(), beg, end );

                // Determine final the 16-bit extended address and 16-bit write address
                int eaddress = xaddress;
                int waddress = raddress;

                if(raddress > FWUtil._64kiB_Min1) {
                    eaddress  = (raddress / FWUtil._64kiB);
                    waddress  = (raddress - eaddress * FWUtil._64kiB);
                    eaddress += xaddress;
                }

                // Write the extended address as needed
                if(eaddress > 0 && eaddressPrev != eaddress) {
                    // Determine the record type
                    final int recordType = ua32 ? FWUtil.IHEX_RT_ExtendedLinearAddress : FWUtil.IHEX_RT_ExtendedSegmentAddress;
                    // Calculate the extended address
                    final int waddrExt = ua32 ? eaddress : (eaddress << 12);
                    final int waddrMSB = (waddrExt >> 8) & 0xFF;
                    final int waddrLSB = (waddrExt     ) & 0xFF;
                    // Calculate the checksum
                    final int checksum = calcChecksum(null, 2, 0, 0, recordType, waddrMSB, waddrLSB);
                    // Prepare the data record
                           _putStartCode  (recordLine                                                   );
                    FWUtil._putValues2Hexs(recordLine, 2, 0, 0, recordType, waddrMSB, waddrLSB, checksum);
                    // Write the 'data' record
                    _writeTextLine( recordLine.toString() );
                    // Save the extended address as the previous extended address
                    eaddressPrev = eaddress;
                }

                // Calculate the checksum
                final int waddrMSB = (waddress >> 8) & 0xFF;
                final int waddrLSB = (waddress     ) & 0xFF;
                final int checksum = calcChecksum(data, len, waddrMSB, waddrLSB, FWUtil.IHEX_RT_Data);

                // Prepare the data record
                       _putStartCode  (recordLine                                               );
                FWUtil._putValues2Hexs(recordLine, len , waddrMSB, waddrLSB, FWUtil.IHEX_RT_Data);
                FWUtil._putData2Hexs  (recordLine, data, checksum                               );

                // Write the 'data' record
                _writeTextLine( recordLine.toString() );

                // Update the counters
                byteCount -= len;
                raddress  += len;
                beg       += len;

            } // while

        } // for

        // Write the 'end of file' record
        _writeTextLine(":00000001FF");

        // Close the text file
        _closeTextFile();
    }

} // class FWWriter_IntelHex
