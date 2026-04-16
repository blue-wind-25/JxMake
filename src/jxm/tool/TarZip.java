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
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.kamranzafar.jtar.*;


public class TarZip extends TarGen {

    private static class ZipInputStreamSF extends ZipInputStream {
        private ZipInputStreamSF(final InputStream is) throws IOException
        {
            super(is);
            this.getNextEntry();
        }
    }

    private static class ZipOutputStreamSF extends ZipOutputStream {
        private ZipOutputStreamSF(final OutputStream os) throws IOException
        {
            super(os);
            this.setLevel(Deflater.BEST_COMPRESSION);
            this.putNextEntry( new ZipEntry(".tar") );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected TarInputStreamExt _getTarInputStream(final String targzFileAbsPath) throws IOException
    {
        return new TarInputStreamExt( new ZipInputStreamSF(
                       new BufferedInputStream( new FileInputStream(targzFileAbsPath) )
                   ) );
    }

    @Override
    protected TarOutputStreamExt _getTarOutputStream(final String targzFileAbsPath) throws IOException
    {
        return new TarOutputStreamExt( new ZipOutputStreamSF(
                       new BufferedOutputStream( new FileOutputStream(targzFileAbsPath) )
                   ) );
    }

    @Override
    protected void _finTarInputStream(final TarInputStreamExt tis) throws IOException
    { ( (ZipInputStream) tis._is ).closeEntry(); }

    @Override
    protected void _finTarOutputStream(final TarOutputStreamExt tos) throws IOException
    { /* Nothing to do here! */ }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static TarZip _tarZip = new TarZip();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void compressDir(final String targzFilePath, final String srcDirPath, final String appendParentDirName) throws IOException
    { _tarZip._compressDir(targzFilePath, srcDirPath, appendParentDirName); }

    public static void uncompressDir(final String targzFilePath, final String dstDirPath) throws IOException
    { _tarZip._uncompressDir(targzFilePath, dstDirPath); }

} // class TarZip
