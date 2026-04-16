/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import jxm.*;


public abstract class ExecBlock {

    private String _path; // JxMake file path   string
    private int    _lNum; // JxMake file line   number
    private int    _cNum; // JxMake file column number

    private String _eStr; // Error message related to this token (in case of error only)

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ExecBlock(final String path, final int lNum, final int cNum)
    {
        _path = path;
        _lNum = lNum;
        _cNum = cNum;
        _eStr = null;
    }

    public String jxmSpecFile_absPath()
    { return _path; }

    public int jxmSpecFileLNum()
    { return _lNum; }

    public int jxmSpecFileCNum()
    { return _cNum; }

    public String getPath()
    { return _path; }

    public int getLNum()
    { return _lNum; }

    public int getCNum()
    { return _cNum; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setErrorFromBlock(final ExecBlock execBlock, final String errMsg, final Object... args)
    {
        _path = execBlock._path;
        _lNum = execBlock._lNum;
        _cNum = execBlock._cNum;
        _eStr = XCom.errorString(errMsg, args);
    }

    public void setErrorFromBlock(final ExecBlock execBlock)
    { setErrorFromBlock(execBlock, "%s", execBlock._eStr); }

    public void setErrorFromString(final String errMsg, final Object... args)
    { _eStr = XCom.errorString(errMsg, args); }

    public void setErrorFromString(final String errMsg)
    { _eStr = errMsg; }

    public String getErrorString()
    { return _eStr; }

    public boolean isErrorStringSet()
    { return _eStr != null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public String sprintError() // Get the error message
    { return SysUtil.sprintError(_path, _lNum, _cNum, _eStr); }

    public boolean printError() // Print the error message
    { return SysUtil.printError(_path, _lNum, _cNum, _eStr); }

    public void printSuppressedError() // Print and clear the error message
    {
        final String eStr = _eStr;
        _eStr = null;

        SysUtil.printSuppressedError(_path, _lNum, _cNum, eStr);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class LoadPLC {
        public final String path;
        public final int    lNum;
        public final int    cNum;

        public LoadPLC(final String path_, final int lNum_, final int cNum_)
        {
            path = path_;
            lNum = lNum_;
            cNum = cNum_;
        }
    };

    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        XSaver.saveString(dos, _path);
        dos.writeInt(_lNum);
        dos.writeInt(_cNum);
    }

    public static LoadPLC loadPLCFromStream(final DataInputStream dis) throws Exception
    { return new LoadPLC( XLoader.loadString(dis), dis.readInt(), dis.readInt() ); }


    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract ExecBlock deepClone();

} // class ExecBlock
