/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.IOException;

import java.util.ArrayList;

import jxm.*;
import jxm.annotation.*;


@package_private
class FWWriter_RawBin extends FWWriter {

    public FWWriter_RawBin(final String binFilePath) throws IOException
    { super(binFilePath); }

    @Override
    protected String __L_RNAME()
    { return FWUtil.RBIN_RName_L; }

    @Override
    protected String __S_RNAME()
    { return FWUtil.RBIN_RName_S; }

    @Override
    public void writeDataBlocks(final ArrayList<FWBlock> fwBlocks, byte nullByte) throws IOException, JXMException
    {
        // Open the binary file
        _openBinaryFile();

        // Write the data block(s)
        long prevAddress = 0;

        for(final FWBlock fwBlock : fwBlocks) {

            // Write the null byte(s) as needed
            if( prevAddress != 0 && fwBlock.startAddress() > prevAddress ) {
                // Write the null byte(s)
                _writeBinaryData( nullByte, fwBlock.startAddress() - prevAddress );
                // Print some debugging information as needed
                if(FWUtil.PRINT_LOAD_SAVE_DEBUG) {
                    SysUtil.stdDbg().printf( "%4s NPAD %08x %d\n", __S_RNAME(), prevAddress, fwBlock.startAddress() - prevAddress );
                }
            }

            // Save the address
            prevAddress = fwBlock.finalAddress() + 1;

            // Write the data byte(s)
            _writeBinaryData( fwBlock.data() );

            // Print some debugging information as needed
            if(FWUtil.PRINT_LOAD_SAVE_DEBUG) {
                SysUtil.stdDbg().printf( "%4s SAVE %08x %d\n", __S_RNAME(), fwBlock.startAddress(), fwBlock.length() );
            }

        } // for

        // Close the binary file
        _closeBinaryFile();
    }

} // class FWWriter_RawBin
