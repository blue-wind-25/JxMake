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

import java.util.zip.Deflater;
import java.util.zip.ZipException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.kamranzafar.jtar.*;


public class TarGz extends TarGen {

    private static class GZIPOutputStreamExt extends GZIPOutputStream {
        private GZIPOutputStreamExt(final OutputStream os) throws IOException
        {
            super(os);
            this.def.setLevel(Deflater.BEST_COMPRESSION);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected TarInputStreamExt _getTarInputStream(final String targzFileAbsPath) throws IOException
    {
        /*
        return new TarInputStreamExt( new GZIPInputStream(
                       new BufferedInputStream( new FileInputStream(targzFileAbsPath) )
                   ) );
        //*/

        // ##### ??? Is there a better and more efficient method than this ??? #####
        // ##### ??? How about the other file types '*.tar.zip/bz2/xz'     ??? #####

        // Determine whether the '*.tar.gz' file has actually been compressed multiple times
        BufferedInputStream bis  = new BufferedInputStream( new FileInputStream(targzFileAbsPath) );
        GZIPInputStream     gis  = new GZIPInputStream(bis);
        int                 mcnt = 0;

        while(true) {
            try {
                GZIPInputStream rec = new GZIPInputStream(gis);
                                gis = rec;
                ++mcnt;
            }
            catch(final ZipException e) {
                break;
            }
        }

        gis.close();

        // Decompress the '*.tar.gz' file as many times as required
        bis = new BufferedInputStream( new FileInputStream(targzFileAbsPath) );
        gis = new GZIPInputStream(bis);

        for(int i = 0 ; i < mcnt; ++i) gis = new GZIPInputStream(gis);

        return new TarInputStreamExt(gis);
    }

    @Override
    protected TarOutputStreamExt _getTarOutputStream(final String targzFileAbsPath) throws IOException
    {
        return new TarOutputStreamExt( new GZIPOutputStreamExt(
                       new BufferedOutputStream( new FileOutputStream(targzFileAbsPath) )
                   ) );
    }

    @Override
    protected void _finTarInputStream(final TarInputStreamExt tis) throws IOException
    { /* Nothing to do here! */ }

    @Override
    protected void _finTarOutputStream(final TarOutputStreamExt tos) throws IOException
    { /* Nothing to do here! */ }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static TarGz _tarGz = new TarGz();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void compressDir(final String targzFilePath, final String srcDirPath, final String appendParentDirName) throws IOException
    { _tarGz._compressDir(targzFilePath, srcDirPath, appendParentDirName); }

    public static void uncompressDir(final String targzFilePath, final String dstDirPath) throws IOException
    { _tarGz._uncompressDir(targzFilePath, dstDirPath); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void gzip(final String inpFilePath, final String outFilePath) throws IOException
    {
        // Open the output file
        final GZIPOutputStream gos = new GZIPOutputStreamExt( new BufferedOutputStream( new FileOutputStream(outFilePath) ) );

        // Open the input file
        final BufferedInputStream bis = new BufferedInputStream( new FileInputStream(inpFilePath) );

        // Compress the file data
        final byte data[] = new byte[TarGen.CDBufferSize];
        while(true) {
            final int cnt = bis.read(data);
            if(cnt == -1) break;
            gos.write(data, 0, cnt);
        }

        // Close the input file
        bis.close();

        // Flush and close the output file
        gos.flush();
        gos.close();
    }

    public static void gunzip(final String inpFilePath, final String outFilePath) throws IOException
    {
        // Open the input file
        final GZIPInputStream gis = new GZIPInputStream( new BufferedInputStream( new FileInputStream(inpFilePath) ) );

        // Open the output file
        final BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(outFilePath) );

        // Uncompress the file data
        final byte data[] = new byte[TarGen.CDBufferSize];
        while(true) {
            final int cnt = gis.read(data);
            if(cnt == -1) break;
            bos.write(data, 0, cnt);
        }

        // Close the input file
        gis.close();

        // Flush and close the output file
        bos.flush();
        bos.close();
    }

} // class TarGz
