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

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jxm.*;


public class UnZip {

    protected static final int CDBufferSize = 8192;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void uncompressDir(final String zipFilePath, final String dstDirPath) throws IOException
    {
        // Get the absolute path of the input file
        final String zipFileAbsPath = SysUtil.resolveAbsolutePath(zipFilePath);

        // Prepare the decompression
        final ZipInputStream zis = new ZipInputStream( new BufferedInputStream( new FileInputStream(zipFileAbsPath) ) );

        // Create the destination directory
        final String dstDirAbsPath = SysUtil.resolveAbsolutePath(dstDirPath);

        SysUtil.cu_mkdir(dstDirAbsPath);

        // Uncompress the files and directories
        final byte data[] = new byte[CDBufferSize];

        while(true) {

            // Get the entry and check for EOF
            final ZipEntry zipEntry = zis.getNextEntry();

            if(zipEntry == null) break;

            // Get the file/directory name
            final String dstName = SysUtil.resolvePath( zipEntry.getName(), dstDirAbsPath );

            // If the entry is a directory, create it
            if( zipEntry.isDirectory() ) {
                SysUtil.cu_mkdir(dstName);
                continue;
            }

            // If the entry is a file, decompress it
            else {
                // Remove the destination file first in case it already exists
                SysUtil.cu_rmfile(dstName);
                // Decompress the file data
                final BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(dstName) );
                while(true) {
                    final int cnt = zis.read(data);
                    if(cnt == -1) break;
                    bos.write(data, 0, cnt);
                }
                bos.flush();
                bos.close();
            }

        } // while

        // End the decompression
        zis.close();
    }

} // class UnZip
