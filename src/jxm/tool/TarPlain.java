/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.kamranzafar.jtar.*;


public class TarPlain extends TarGen {

    @Override
    protected TarInputStreamExt _getTarInputStream(final String tarplainFileAbsPath) throws IOException
    { return new TarInputStreamExt( new BufferedInputStream( new FileInputStream(tarplainFileAbsPath) ) ); }

    @Override
    protected TarOutputStreamExt _getTarOutputStream(final String tarplainFileAbsPath) throws IOException
    { return new TarOutputStreamExt( new BufferedOutputStream( new FileOutputStream(tarplainFileAbsPath) ) ); }

    @Override
    protected void _finTarInputStream(final TarInputStreamExt tis) throws IOException
    { /* Nothing to do here! */ }

    @Override
    protected void _finTarOutputStream(final TarOutputStreamExt tos) throws IOException
    { /* Nothing to do here! */ }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static TarPlain _tarPlain = new TarPlain();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void compressDir(final String tarplainFilePath, final String srcDirPath, final String appendParentDirName) throws IOException
    { _tarPlain._compressDir(tarplainFilePath, srcDirPath, appendParentDirName); }

    public static void uncompressDir(final String tarplainFilePath, final String dstDirPath) throws IOException
    { _tarPlain._uncompressDir(tarplainFilePath, dstDirPath); }

} // class TarPlain
