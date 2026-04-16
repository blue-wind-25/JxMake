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
abstract class FWReaderTextRL extends FWReader {

    protected boolean _fileOpen                   = false;

    protected String  _prevRecordLine             = null;
    protected long    _prevDataBlockFinalAddress  = -1;

    protected Bytes   _thisDataBlockBytes         = new Bytes();
    protected long    _thisDataBlockStartAddress  = -1;

    protected boolean _resetStartFinalAddress     = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected FWReaderTextRL(final String txtFilePath) throws IOException
    { super(txtFilePath); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _openTextFileAsNeeded() throws IOException
    {
        if(_fileOpen) return;

        _openTextFile();
        _fileOpen = true;
    }

    protected String _readRecordLine(final boolean trimWhitespacesOnly) throws IOException
    {
        // Reset the start and final address as needed
        if(_resetStartFinalAddress) {
            _resetStartFinalAddress    = false;
            _prevDataBlockFinalAddress = -1;
            _thisDataBlockStartAddress = -1;
        }

        // Read one record line
        String recordLine = null;

        if(_prevRecordLine != null) {
            // Read from the previously saved record line
            recordLine      = _prevRecordLine;
            _prevRecordLine = null;
        }
        else {
            // Read from the file and check for EOF
            recordLine = _readTextLine();
            if(recordLine == null) return null;
            // Trim the line
            recordLine = trimWhitespacesOnly ? XCom.re_trimWhitespacesOnly(recordLine) : recordLine.trim();
            /*
            SysUtil.stdDbg().println(recordLine);
            //*/
        }

        // Return the record line
        return recordLine;
    }

    protected String _readRecordLine() throws IOException
    { return _readRecordLine(false); }

    protected boolean _storeBackOneRecordLine(final String recordLine)
    {
        if(_prevRecordLine != null) return false;

        _prevRecordLine = recordLine;

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _storeDataBlockBytes(final String curRecordLine, final long address, final byte[] buff, final int buffSkip)
    {
        // Check against the previous block final address
        if(_prevDataBlockFinalAddress != -1) {
            if(address != _prevDataBlockFinalAddress + 1) {
                if(_prevRecordLine != null) {
                    if(curRecordLine != null) {
                        // ##### !!! TODO : ERROR !!! #####
                    }
                }
                else {
                    _prevRecordLine = curRecordLine;
                }
                _resetStartFinalAddress = true;
                if( !_thisDataBlockBytes.empty() ) return false;
                return true;
            }
        }

        // Determine this block start address
        if(_thisDataBlockStartAddress == -1) _thisDataBlockStartAddress = address;
        else                                 _thisDataBlockStartAddress = Math.min(_thisDataBlockStartAddress, address);

        // Append the bytes
        _thisDataBlockBytes.appendAfter(buff, buffSkip);

        // Set the previous block final address
        _prevDataBlockFinalAddress = address + buff.length - buffSkip - 1;

        // Done
        return true;
    }

    protected void _storeDataBlockBytes(final long address, final byte[] buff, final boolean resetStartFinalAddress)
    {
        // Determine this block start address
        if(_thisDataBlockStartAddress == -1) _thisDataBlockStartAddress = address;

        // Append the bytes
        _thisDataBlockBytes.appendAfter(buff, 0);

        // Set the previous block final address
        _prevDataBlockFinalAddress = address + buff.length - 1;

        // Set the flag
        _resetStartFinalAddress = resetStartFinalAddress;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected byte[] _getDataBlockBytes()
    {
        // Simply return if there is no data
        if( _thisDataBlockBytes.empty() ) return null;

        // Get the data bytes
        final byte[] data = _thisDataBlockBytes.data();

        // Clear the data block buffer
        _thisDataBlockBytes.clear();

        // Return the data bytes
        return data;
    }

} // class FWReaderTextRL
