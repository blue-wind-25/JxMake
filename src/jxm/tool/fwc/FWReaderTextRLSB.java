/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.IOException;

import jxm.*;
import jxm.annotation.*;


@package_private
abstract class FWReaderTextRLSB extends FWReaderTextRL {

    protected Bytes _savedDataBlockBytes        = new Bytes();
    protected long  _savedDataBlockStartAddress = -1;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected FWReaderTextRLSB(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected byte[] _getSavedDataBlockBytes() throws Exception
    {
        // Simply return if there is no saved data block
        if(_savedDataBlockStartAddress == -1) return null;

        // Error if there is a pending data block in the main data block buffer
        if( !_thisDataBlockBytes.empty() ) _throwInvalidRecLineDFormat();

        // Get the data bytes and address
        final byte[] data = _savedDataBlockBytes.data();
        _thisDataBlockStartAddress = _savedDataBlockStartAddress;

        // Clear the saved data block
        _savedDataBlockBytes.clear();
        _savedDataBlockStartAddress = -1;

        // Return the data bytes
        return data;
    }

    protected boolean _storeOrSaveDataBlockBytes(final long address, final byte[] buff)
    {
        // Store the data bytes to the main data block buffer
        if( _storeDataBlockBytes(null, address, buff, 0) ) return true;

        // Store the data bytes to the saved data block buffer
        _savedDataBlockBytes.appendAfter(buff);
        _savedDataBlockStartAddress = address;

        // Done
        return false;
    }

} // class FWReaderTextRLSB
