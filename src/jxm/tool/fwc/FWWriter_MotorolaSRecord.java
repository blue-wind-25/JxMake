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
class FWWriter_MotorolaSRecord extends FWWriter {

    private static int _calcChecksum(final byte[] data, final int... others)
    {
        int res = 0;

        for(int v : data  ) res += v & 0xFF;
        for(int v : others) res += v;

        return ( 0x0FF - (res & 0xFF) ) & 0xFF;
    }

    private void _putStartCode(final StringBuilder dst, final char recordType)
    {
        dst.setLength(0);

        dst.append('S'       );
        dst.append(recordType);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWWriter_MotorolaSRecord(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.SREC_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.SREC_RName_S; }

    // NOTE : 'nullByte' is not currently used by 'Motorola S-Record' writer!
    @Override
    public void writeDataBlocks(final ArrayList<FWBlock> fwBlocks, byte nullByte) throws IOException, JXMException
    {
        // ##### !!! TODO : Optimize !!! #####

        // ##### !!! TODO : Allow this to be user selectable (convert this constant into a function parameter)? !!! #####
        final int addrSpaceSize = 0;

        // Determine whether to use 16/24/32-bit address
        int addrLen = 2;

        for(final FWBlock fwBlock : fwBlocks) {
            if( fwBlock.finalAddress() > FWUtil._64kiB_Min1 )   addrLen = Math.max(addrLen, 3);
            if( fwBlock.finalAddress() > FWUtil._16MiB_Min1 ) { addrLen = Math.max(addrLen, 4); break; }
        }

        if(addrSpaceSize != 0) {
                 if(addrSpaceSize == 16) { if(addrLen > 2) _throwAddrSpcTooLargeFSetting(); addrLen = 2; }
            else if(addrSpaceSize == 24) { if(addrLen > 3) _throwAddrSpcTooLargeFSetting(); addrLen = 3; }
            else if(addrSpaceSize == 32) { /* No checking is needed here */                 addrLen = 4; }
            else                         { _throwAddrSpcInvalid(addrSpaceSize);                          }
        } // if

        // Open the text file
        _openTextFile();

        // Write the header record
        //                      J x M a k e
        _writeTextLine("S00900004A784D616B65B6");

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

                // A variable for storing the checksum
                int checksum = 0xFF;

                // Store with 16-bit address
                if(addrLen == 2) {
                    // Calculate the checksum
                    final int address1 = (int) (address >>  8) & 0xFF;
                    final int address0 = (int) (address      ) & 0xFF;
                              checksum = _calcChecksum(data, len + 2 + 1, address1, address0);
                    // Prepare the data record - store the start code, byte count, and address
                           _putStartCode  (recordLine, FWUtil.SREC_RT_Data16          );
                    FWUtil._putValues2Hexs(recordLine, len + 2 + 1, address1, address0);
                }

                // Store with 24-bit address
                if(addrLen == 3) {
                    // Calculate the checksum
                    final int address2 = (int) (address >> 16) & 0xFF;
                    final int address1 = (int) (address >>  8) & 0xFF;
                    final int address0 = (int) (address      ) & 0xFF;
                              checksum = _calcChecksum(data, len + 3 + 1, address2, address1, address0);
                    // Prepare the data record - store the start code, byte count, and address
                           _putStartCode  (recordLine, FWUtil.SREC_RT_Data24                    );
                    FWUtil._putValues2Hexs(recordLine, len + 3 + 1, address2, address1, address0);
                }

                // Store with 32-bit address
                if(addrLen == 4) {
                    // Calculate the checksum
                    final int address3 = (int) (address >> 24) & 0xFF;
                    final int address2 = (int) (address >> 16) & 0xFF;
                    final int address1 = (int) (address >>  8) & 0xFF;
                    final int address0 = (int) (address      ) & 0xFF;
                              checksum = _calcChecksum(data, len + 4 + 1, address3, address2, address1, address0);
                    // Prepare the data record - store the start code, byte count, and address
                           _putStartCode  (recordLine, FWUtil.SREC_RT_Data32                              );
                    FWUtil._putValues2Hexs(recordLine, len + 4 + 1, address3, address2, address1, address0);
                }

                // Prepare the data record - store the data and checksum
                FWUtil._putData2Hexs(recordLine, data, checksum);

                // Write the 'data' record
                _writeTextLine( recordLine.toString() );

                // Update the counters
                byteCount -= len;
                address   += len;
                beg       += len;

            } // while

        } // for

        // Write the 'start address (termination)' record
             if(addrLen == 2) _writeTextLine("S9030000FC"    );
        else if(addrLen == 3) _writeTextLine("S804000000FB"  );
        else if(addrLen == 4) _writeTextLine("S70500000000FA");

        // Close the text file
        _closeTextFile();
    }

} // class FWWriter_MotorolaSRecord
