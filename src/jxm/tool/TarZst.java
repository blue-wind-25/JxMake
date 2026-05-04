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

import com.github.luben.zstd.*;
import org.kamranzafar.jtar.*;


public class TarZst extends TarGen {

    private static class ZstdOutputStreamExt extends ZstdOutputStream {
        private ZstdOutputStreamExt(final OutputStream os) throws IOException
        { super( os ); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected TarInputStreamExt _getTarInputStream(final String tarzstdFileAbsPath) throws IOException
    {
        return new TarInputStreamExt( new ZstdInputStream(
                       new BufferedInputStream( new FileInputStream(tarzstdFileAbsPath) )
                   ) );
    }

    @Override
    protected TarOutputStreamExt _getTarOutputStream(final String tarzstdFileAbsPath) throws IOException
    {
        return new TarOutputStreamExt( new ZstdOutputStreamExt(
                       new BufferedOutputStream( new FileOutputStream(tarzstdFileAbsPath) )
                   ) );
    }

    @Override
    protected void _finTarInputStream(final TarInputStreamExt tis) throws IOException
    { /* Nothing to do here! */ }

    @Override
    protected void _finTarOutputStream(final TarOutputStreamExt tos) throws IOException
    { /* Nothing to do here! */ }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static TarZst _tarZst = new TarZst();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void compressDir(final String tarzstdFilePath, final String srcDirPath, final String appendParentDirName) throws IOException
    { _tarZst._compressDir(tarzstdFilePath, srcDirPath, appendParentDirName); }

    public static void uncompressDir(final String tarzstdFilePath, final String dstDirPath) throws IOException
    { _tarZst._uncompressDir(tarzstdFilePath, dstDirPath); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void zstd(final String inpFilePath, final String outFilePath) throws IOException
    {
        // Open the output file
        final ZstdOutputStream zos = new ZstdOutputStreamExt( new BufferedOutputStream( new FileOutputStream(outFilePath) ) );

        // Open the input file
        final BufferedInputStream bis = new BufferedInputStream( new FileInputStream(inpFilePath) );

        // Compress the file data
        final byte data[] = new byte[TarGen.CDBufferSize];
        while(true) {
            final int cnt = bis.read(data);
            if(cnt == -1) break;
            zos.write(data, 0, cnt);
        }

        // Close the input file
        bis.close();

        // Flush and close the output file
        zos.flush();
        zos.close();
    }

    public static void unzstd(final String inpFilePath, final String outFilePath) throws IOException
    {
        // Open the input file
        final ZstdInputStream zis = new ZstdInputStream( new BufferedInputStream( new FileInputStream(inpFilePath) ) );

        // Open the output file
        final BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(outFilePath) );

        // Uncompress the file data
        final byte data[] = new byte[TarGen.CDBufferSize];
        while(true) {
            final int cnt = zis.read(data);
            if(cnt == -1) break;
            bos.write(data, 0, cnt);
        }

        // Close the input file
        zis.close();

        // Flush and close the output file
        bos.flush();
        bos.close();
    }

} // class TarZst
