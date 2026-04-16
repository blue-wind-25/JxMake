/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


@package_private
abstract class FWReader {

    private String _filePath   = null;
    private String _fileName   = null;
    private int    _lineNumber = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected FWReader(final String filePath) throws IOException
    {
        _filePath   = filePath;
        _fileName   = SysUtil.getFileName(filePath);
        _lineNumber = 0;
    }

    protected String filePath  () { return _filePath;   }
    protected String fileName  () { return _fileName;   }
    protected int    lineNumber() { return _lineNumber; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _throwInvalidBinFileFormat() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_InvalidBinFileFormat, filePath(), __L_RNAME() ); }

    protected void _throwInvalidBinFileEOF() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_InvalidBinFileEOF, filePath(), __L_RNAME() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _throwInvalidRecLineDFormat() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_InvalidRecLineDFormat  , filePath(), lineNumber(), __L_RNAME() ); }

    protected void _throwInvalidRecLineStartCode() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_InvalidRecLineStartCode, filePath(), lineNumber(), __L_RNAME() ); }

    protected void _throwInvalidRecLineRecType() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_InvalidRecLineRecType  , filePath(), lineNumber(), __L_RNAME() ); }

    protected void _throwInvalidRecLineByteCount() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_InvalidRecLineByteCount, filePath(), lineNumber(), __L_RNAME() ); }

    protected void _throwInvalidRecLineAddresss() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_InvalidRecLineAddresss , filePath(), lineNumber(), __L_RNAME() ); }

    protected void _throwInvalidRecLineChecksum() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_InvalidRecLineChecksum , filePath(), lineNumber(), __L_RNAME() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private BufferedReader _buffTextReader = null;

    protected void _openTextFile() throws IOException
    { _buffTextReader = new BufferedReader( new InputStreamReader ( new FileInputStream (_filePath), SysUtil._CharEncoding ) ); }

    protected String _readTextLine() throws IOException
    {
        if(_buffTextReader == null) return null;

        final String line = _buffTextReader.readLine();

        if(line == null) {
            _buffTextReader.close();
            _buffTextReader = null;
            return null;
        }

        ++_lineNumber;

        return line;
    }

    protected void _closeTextFile() throws IOException
    {
        if(_buffTextReader == null) return;

        _buffTextReader.close();
        _buffTextReader = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected byte[] _readAllBinaryData() throws IOException
    { return SysUtil.readTextFileAsByteArray(_filePath); }

    /*
     * ??? TODO : Implement these also ???
     *     protected void _openBinaryFile() throws IOException
     *     protected byte[] _readBinaryData(final int cnt) throws IOException
     *     protected void _closeBinaryFile() throws IOException
     */

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract String __L_RNAME();

    protected abstract String __S_RNAME();

    public abstract byte[] readDataBlock() throws Exception;

    public abstract long dataBlockStartAddress();

} // class FWReader
