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

import org.itadaki.bzip2.*;
import org.kamranzafar.jtar.*;


public class TarBz2 extends TarGen {

    private static class BZip2InputStreamExt extends BZip2InputStream {
        private BZip2InputStreamExt(final InputStream is) throws IOException
        { super(is, false); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected TarInputStreamExt _getTarInputStream(final String tarbz2FileAbsPath) throws IOException
    {
        return new TarInputStreamExt( new BZip2InputStreamExt(
                       new BufferedInputStream( new FileInputStream(tarbz2FileAbsPath) )
                   ) );
    }

    @Override
    protected TarOutputStreamExt _getTarOutputStream(final String tarbz2FileAbsPath) throws IOException
    {
        return new TarOutputStreamExt( new BZip2OutputStream(
                       new BufferedOutputStream( new FileOutputStream(tarbz2FileAbsPath) )
                   ) );
    }

    @Override
    protected void _finTarInputStream(final TarInputStreamExt tis) throws IOException
    { /* Nothing to do here! */ }

    @Override
    protected void _finTarOutputStream(final TarOutputStreamExt tos) throws IOException
    { /* Nothing to do here! */ }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static TarBz2 _tarBz2 = new TarBz2();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void compressDir(final String tarbz2FilePath, final String srcDirPath, final String appendParentDirName) throws IOException
    { _tarBz2._compressDir(tarbz2FilePath, srcDirPath, appendParentDirName); }

    public static void uncompressDir(final String tarbz2FilePath, final String dstDirPath) throws IOException
    { _tarBz2._uncompressDir(tarbz2FilePath, dstDirPath); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void bzip2(final String inpFilePath, final String outFilePath) throws IOException
    {
        // Open the output file
        final BZip2OutputStream bos = new BZip2OutputStream( new BufferedOutputStream( new FileOutputStream(outFilePath) ) );

        // Open the input file
        final BufferedInputStream bis = new BufferedInputStream( new FileInputStream(inpFilePath) );

        // Compress the file data
        final byte data[] = new byte[TarGen.CDBufferSize];
        while(true) {
            final int cnt = bis.read(data);
            if(cnt == -1) break;
            bos.write(data, 0, cnt);
        }

        // Close the input file
        bis.close();

        // Flush and close the output file
        bos.flush();
        bos.close();
    }

    public static void bunzip2(final String inpFilePath, final String outFilePath) throws IOException
    {
        // Open the input file
        final BZip2InputStream bis = new BZip2InputStreamExt( new BufferedInputStream( new FileInputStream(inpFilePath) ) );

        // Open the output file
        final BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(outFilePath) );

        // Uncompress the file data
        final byte data[] = new byte[TarGen.CDBufferSize];
        while(true) {
            final int cnt = bis.read(data);
            if(cnt == -1) break;
            bos.write(data, 0, cnt);
        }

        // Close the input file
        bis.close();

        // Flush and close the output file
        bos.flush();
        bos.close();
    }

} // class TarBz2
