/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import java.nio.file.attribute.PosixFilePermission;

import java.time.Instant;

import java.util.HashSet;
import java.util.List;

import org.kamranzafar.jtar.*;

import jxm.*;
import jxm.xb.*;


public abstract class TarGen {

    protected static final int CDBufferSize = 8192;

    protected static class TarInputStreamExt extends TarInputStream {
        protected final InputStream _is;

        protected TarInputStreamExt(final InputStream is)
        {
            super(is);
            _is = is;
        }
    }

    protected static class TarOutputStreamExt extends TarOutputStream {
        protected final OutputStream _os;

        protected TarOutputStreamExt(final OutputStream os)
        {
            super(os);
            _os = os;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract TarInputStreamExt _getTarInputStream(final String targenFileAbsPath) throws IOException;
    protected abstract TarOutputStreamExt _getTarOutputStream(final String targenFileAbsPath) throws IOException;

    protected abstract void _finTarInputStream(final TarInputStreamExt tis) throws IOException;
    protected abstract void _finTarOutputStream(final TarOutputStreamExt tos) throws IOException;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _compressDir(final String targenFilePath, final String srcDirPath, final String appendParentDirName) throws IOException
    {
        // List the source directory
        // NOTE : Only regular files and directories are included !!!
        final String       srcDirAbsPath = SysUtil.resolveAbsolutePath(srcDirPath);
        final List<String> strList       = SysUtil.cu_lsfd_rec(srcDirAbsPath);

        strList.remove(srcDirAbsPath);

        // ##### !!! TODO : Symbolic and hard links? !!! #####
        // public static TarHeader createHeader(String entryName, long size, long modTime, boolean dir, int permissions, byte linkFlag, String linkName);

        // Get the inputs
        final File[] inFiles = new File[ strList.size() ];

        for(int i = 0; i < strList.size(); ++i) {
            inFiles[i] = new File( strList.get(i) );
        }

        // Remove the output file first in case it already exists
        final String targenFileAbsPath = SysUtil.resolveAbsolutePath(targenFilePath);

        SysUtil.cu_rmfile(targenFileAbsPath);

        // Prepare the compression
        final TarOutputStreamExt tos = _getTarOutputStream(targenFileAbsPath);

        // Append the parent directory as needed
        String apdn = (appendParentDirName != null) ? appendParentDirName.trim() : "";

        if( !apdn.isEmpty() ) {
            if( apdn.charAt( apdn.length() - 1 ) != '/' ) apdn += '/';
            tos.putNextEntry( new TarEntry(
                TarHeader.createHeader( apdn, 0, Instant.now().getEpochSecond(), true, SysUtil.osIsWindows() ? 0770 : 0775 )
            ) );
        }

        // Compress the input files and directories
        final int  startIdx = srcDirAbsPath.length() + 1;
        final byte data[]   = new byte[CDBufferSize];

        for(final File file : inFiles){

            // Determine the entry name
            final String entryName = apdn + file.getPath().substring( startIdx, file.getPath().length() );

            // Put the entry
            tos.putNextEntry( new TarEntry(file, entryName) );

            // Compress the file data
            if( !file.isDirectory() ) {
                final BufferedInputStream bis  = new BufferedInputStream( new FileInputStream(file) );
                while(true) {
                    final int cnt = bis.read(data);
                    if(cnt == -1) break;
                    tos.write(data, 0, cnt);
                }
                bis.close();
            }

            // Flush the output file
            tos.flush();

        } // for

        // End the compression
        _finTarOutputStream(tos);
        tos.close();
    }

    private static void _uncompressAndWriteFile(final String dstName, final TarInputStreamExt tis, final byte[] data) throws IOException
    {
        // Remove the destination file first in case it already exists
        SysUtil.cu_rmfile(dstName);

        // Decompress the file data
        final BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(dstName) );

        while(true) {
            final int cnt = tis.read(data);
            if(cnt == -1) break;
            bos.write(data, 0, cnt);
        }

        bos.flush();
        bos.close();
    }

    protected void _uncompressDir(final String targenFilePath, final String dstDirPath) throws IOException
    {
        try {

            // Get the absolute path of the input file
            final String targenFileAbsPath = SysUtil.resolveAbsolutePath(targenFilePath);

            // Prepare the decompression
            final TarInputStreamExt tis = _getTarInputStream(targenFileAbsPath);

            // Create the destination directory
            final String dstDirAbsPath = SysUtil.resolveAbsolutePath(dstDirPath);

            SysUtil.cu_mkdir(dstDirAbsPath);

            // Uncompress the files and directories
            final byte data[] = new byte[CDBufferSize];

            while(true) {

                // Get the entry and check for EOF
                final TarEntry tagEntry = tis.getNextEntry();

                if(tagEntry == null) break;

                // Get the file/directory name
                final TarHeader tarHeader = tagEntry.getHeader();
                final String    dstName   = SysUtil.resolvePath( tagEntry.getName(), dstDirAbsPath );

                // If the entry is a directory, create it
                if(tarHeader.linkFlag == TarHeader.LF_DIR) {
                    // Create the directory
                    /*
                    SysUtil.stdDbg().printf("[DIR] '%s'\n", dstName);
                    //*/
                    SysUtil.cu_mkdir(dstName);
                    continue;
                }

                // If the entry is a symbolic link, create it
                else if(tarHeader.linkFlag == TarHeader.LF_SYMLINK) {
                    // Ignore if the OS is not POSIX
                    // ##### !!! TODO : How about Windows with NTFS? !!! #####
                    if( !SysUtil.osIsPOSIX() ) continue;
                    // Create the symbolic link
                    /*
                    SysUtil.stdDbg().printf("[SYM] '%s'\n", dstName);
                    //*/
                    SysUtil.cu_ln_s( dstName, tarHeader.linkName.toString(), true );
                    // Continue to the next entry after creating the symbolic link
                    continue;
                }

                // If the entry is a hard link, create it
                else if(tarHeader.linkFlag == TarHeader.LF_LINK) {
                    // Ignore if the OS is not POSIX
                    // ##### !!! TODO : How about Windows with NTFS? !!! #####
                    if( !SysUtil.osIsPOSIX() ) continue;
                    // Create the hard link
                    /*
                    SysUtil.stdDbg().printf("[LNK] '%s'\n", dstName);
                    //*/
                    SysUtil.cu_ln_h( dstName, SysUtil.resolvePath( tarHeader.linkName.toString(), dstDirAbsPath ), true );
                    // Continue to the next entry after creating the hard link
                    continue;
                }

                // If the entry is a normal/contigous file, decompress it
                else if(tarHeader.linkFlag == '\0' || tarHeader.linkFlag == TarHeader.LF_NORMAL || tarHeader.linkFlag == TarHeader.LF_CONTIG) {
                    // Check if the destination directory does exist
                    final String dstDir = SysUtil.getDirName(dstName);
                    if( SysUtil.pathIsValidDirectory(dstDir) )  {
                        // Remove the destination file first in case it already exists
                        SysUtil.cu_rmfile(dstName);
                    }
                    else {
                        // Create the directory because it does not exist yet
                        SysUtil.cu_mkdir(dstDir);
                    }
                    // Uncompress and write the file
                    /*
                    SysUtil.stdDbg().printf("[FIL] '%s'\n", dstName);
                    //*/
                    _uncompressAndWriteFile(dstName, tis, data);
                }

                /* Special entries
                 *     'g' and 'x' entries (POSIX.1-2001)
                 *     'A'  -  'Z' entries (POSIX.1-1988)
                 * Please refer to:
                 *     https://en.wikipedia.org/wiki/Tar_(computing)#UStar_format
                 * for more details.
                 */
                else if( ( tarHeader.linkFlag == (byte) 'g'                                     ) ||
                         ( tarHeader.linkFlag == (byte) 'x'                                     ) ||
                         ( tarHeader.linkFlag >= (byte) 'A' && tarHeader.linkFlag <= (byte) 'Z' )
                ) {
                    // Ignore all the special entries
                    if(true) {
                        continue;
                    }
                    // Process the 'g' and 'x' entries
                    else if( tarHeader.linkFlag == (byte) 'g' || tarHeader.linkFlag == (byte) 'x' ) {
                        // Make directory, it should be something like /PaxHeaders\\.\d+/
                        SysUtil.cu_mkdir( SysUtil.getDirName(dstName) );
                        // Uncompress and write the file
                        _uncompressAndWriteFile(dstName, tis, data);
                    }
                    // Ignore the 'A' - 'Z' entries
                    else {
                        continue;
                    }
                }

                // Unsupported entry (LF_CHR, LF_BLK, LF_FIFO, etc.)
                else {
                    // ##### !!! TODO !!! #####
                    if(tarHeader.linkFlag >= 32 && tarHeader.linkFlag <= 126) {
                        throw XCom.newIOException("unsupported tar entry type '%c'", (char) tarHeader.linkFlag);
                    }
                    else {
                        throw XCom.newIOException("unsupported tar entry type #%d", tarHeader.linkFlag & 0xFF);
                    }
                }

                // Set the permission as needed
                if( SysUtil.osIsPOSIX() ) {
                    // NOTE : Special modes are not supported !!!
                    final int                          mode = tarHeader.mode;
                    final HashSet<PosixFilePermission> pfp  = new HashSet<>();
                    if( (mode & 0100) != 0 ) pfp.add(PosixFilePermission.OWNER_EXECUTE );
                    if( (mode & 0200) != 0 ) pfp.add(PosixFilePermission.OWNER_WRITE   );
                    if( (mode & 0400) != 0 ) pfp.add(PosixFilePermission.OWNER_READ    );
                    if( (mode & 0010) != 0 ) pfp.add(PosixFilePermission.GROUP_EXECUTE );
                    if( (mode & 0020) != 0 ) pfp.add(PosixFilePermission.GROUP_WRITE   );
                    if( (mode & 0040) != 0 ) pfp.add(PosixFilePermission.GROUP_READ    );
                    if( (mode & 0001) != 0 ) pfp.add(PosixFilePermission.OTHERS_EXECUTE);
                    if( (mode & 0002) != 0 ) pfp.add(PosixFilePermission.OTHERS_WRITE  );
                    if( (mode & 0004) != 0 ) pfp.add(PosixFilePermission.OTHERS_READ   );
                    Files.setPosixFilePermissions( Paths.get(dstName), pfp );
                }

            } // while

            // End the decompression
            _finTarInputStream(tis);
            tis.close();

        } // try
        catch(final InvalidPathException e) {
            throw XCom.newIOException( "InvalidPathException: %s", e.getMessage() );
        }

    }

} // class TarGen
