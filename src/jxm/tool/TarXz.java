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
import java.io.OutputStream;

import org.kamranzafar.jtar.*;
import org.tukaani.xz.*;


public class TarXz extends TarGen {

    private static class XZOutputStreamExt extends XZOutputStream {
        private XZOutputStreamExt(final OutputStream os) throws IOException
        { super( os, new LZMA2Options() ); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected TarInputStreamExt _getTarInputStream(final String tarxzFileAbsPath) throws IOException
    {
        return new TarInputStreamExt( new XZInputStream(
                       new BufferedInputStream( new FileInputStream(tarxzFileAbsPath) )
                   ) );
    }

    @Override
    protected TarOutputStreamExt _getTarOutputStream(final String tarxzFileAbsPath) throws IOException
    {
        return new TarOutputStreamExt( new XZOutputStreamExt(
                       new BufferedOutputStream( new FileOutputStream(tarxzFileAbsPath) )
                   ) );
    }

    @Override
    protected void _finTarInputStream(final TarInputStreamExt tis) throws IOException
    { /* Nothing to do here! */ }

    @Override
    protected void _finTarOutputStream(final TarOutputStreamExt tos) throws IOException
    { /* Nothing to do here! */ }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static TarXz _tarXz = new TarXz();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void compressDir(final String tarxzFilePath, final String srcDirPath, final String appendParentDirName) throws IOException
    { _tarXz._compressDir(tarxzFilePath, srcDirPath, appendParentDirName); }

    public static void uncompressDir(final String tarxzFilePath, final String dstDirPath) throws IOException
    { _tarXz._uncompressDir(tarxzFilePath, dstDirPath); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void xz(final String inpFilePath, final String outFilePath) throws IOException
    {
        // Open the output file
        final XZOutputStream xos = new XZOutputStreamExt( new BufferedOutputStream( new FileOutputStream(outFilePath) ) );

        // Open the input file
        final BufferedInputStream bis = new BufferedInputStream( new FileInputStream(inpFilePath) );

        // Compress the file data
        final byte data[] = new byte[TarGen.CDBufferSize];
        while(true) {
            final int cnt = bis.read(data);
            if(cnt == -1) break;
            xos.write(data, 0, cnt);
        }

        // Close the input file
        bis.close();

        // Flush and close the output file
        xos.flush();
        xos.close();
    }

    public static void unxz(final String inpFilePath, final String outFilePath) throws IOException
    {
        // Open the input file
        final XZInputStream xis = new XZInputStream( new BufferedInputStream( new FileInputStream(inpFilePath) ) );

        // Open the output file
        final BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(outFilePath) );

        // Uncompress the file data
        final byte data[] = new byte[TarGen.CDBufferSize];
        while(true) {
            final int cnt = xis.read(data);
            if(cnt == -1) break;
            bos.write(data, 0, cnt);
        }

        // Close the input file
        xis.close();

        // Flush and close the output file
        bos.flush();
        bos.close();
    }

} // class TarXz
