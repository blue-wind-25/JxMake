/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;

import java.util.ArrayList;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


@package_private
abstract class FWWriter {

    private String _filePath = null;
    private String _fileName = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected FWWriter(final String filePath) throws IOException
    {
        _filePath = filePath;
        _fileName = (filePath != null) ? SysUtil.getFileName(filePath) : null;
    }

    protected String filePath() { return _filePath; }
    protected String fileName() { return _fileName; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _throwAddrSpcInvalid(final int addrSpaceSize) throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_AddrSpcInvalid, filePath(), addrSpaceSize, __L_RNAME() ); }

    protected void _throwAddrSpcTooLargeFFormat() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_AddrSpcTooLargeFFormat, filePath(), __L_RNAME() ); }

    protected void _throwAddrSpcTooLargeFSetting() throws JXMException
    { throw XCom.newJXMException( Texts.EMsg_AddrSpcTooLargeFSetting , filePath(), __L_RNAME() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private BufferedWriter _buffTextWriter = null;

    protected void _openTextFile() throws IOException
    { _buffTextWriter = new BufferedWriter( new OutputStreamWriter ( new FileOutputStream (_filePath), SysUtil._CharEncoding ) ); }

    protected void _writeTextLine(final String line) throws IOException
    {
        if(_buffTextWriter == null) return;

        _buffTextWriter.write(line);
        _buffTextWriter.newLine();
    }

    protected void _closeTextFile() throws IOException
    {
        if(_buffTextWriter == null) return;

        _buffTextWriter.close();
        _buffTextWriter = null;
    }

    protected BufferedWriter _bufferedTextWriter()
    { return _buffTextWriter; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private OutputStream _binaryOutputStream = null; // FileOutputStream or ByteArrayOutputStream
    private byte[]       _binaryByteArray    = null;

    protected byte[] _getBinaryByteArray()
    {
        final byte[] bba = _binaryByteArray;

        _binaryByteArray = null;

        return bba;
    }

    protected void _openBinaryFile() throws IOException
    {
        if(_filePath != null) _binaryOutputStream = new FileOutputStream     (_filePath);
        else                  _binaryOutputStream = new ByteArrayOutputStream(         );
    }

    protected void _writeBinaryData(final byte data, final long cnt) throws IOException
    {
        if(_binaryOutputStream == null || cnt <= 0) return;

        // ##### !!! TODO : Optimize !!! #####

        final int d = data & 0xFF;

        for(long i = 0; i < cnt; ++i) _binaryOutputStream.write(d);
                                      _binaryOutputStream.flush();
    }

    protected void _writeBinaryData(final byte[] data) throws IOException
    {
        if(_binaryOutputStream == null) return;

        _binaryOutputStream.write(data);
        _binaryOutputStream.flush();
    }

    protected void _closeBinaryFile() throws IOException
    {
        if(_binaryOutputStream == null) return;

        if(_binaryOutputStream instanceof ByteArrayOutputStream) {
            _binaryByteArray = ( (ByteArrayOutputStream) _binaryOutputStream ).toByteArray();
        }

        _binaryOutputStream.close();
        _binaryOutputStream = null;
    }

    /*
     * ??? TODO : Implement this also ???
     *     protected void _writeAllBinaryData(byte[]) throws IOException
     */

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract String __L_RNAME();

    protected abstract String __S_RNAME();

    public abstract void writeDataBlocks(final ArrayList<FWBlock> fwBlocks, byte nullByte) throws IOException, JXMException;

} // class FWWriter
