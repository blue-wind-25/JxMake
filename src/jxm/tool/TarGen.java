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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;

import java.time.Instant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final int DEFAULT_PERMISSION = 0644;

    private int _getPOSIXPermissions(final Path path)
    {
        if( !SysUtil.osIsPOSIX() ) return DEFAULT_PERMISSION; // Fallback for Windows or non-POSIX filesystems

        try {
            final Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
                  int                      mode  = 0;

            if( perms.contains(PosixFilePermission.OWNER_READ    ) ) mode |= 0400;
            if( perms.contains(PosixFilePermission.OWNER_WRITE   ) ) mode |= 0200;
            if( perms.contains(PosixFilePermission.OWNER_EXECUTE ) ) mode |= 0100;

            if( perms.contains(PosixFilePermission.GROUP_READ    ) ) mode |= 0040;
            if( perms.contains(PosixFilePermission.GROUP_WRITE   ) ) mode |= 0020;
            if( perms.contains(PosixFilePermission.GROUP_EXECUTE ) ) mode |= 0010;

            if( perms.contains(PosixFilePermission.OTHERS_READ   ) ) mode |= 0004;
            if( perms.contains(PosixFilePermission.OTHERS_WRITE  ) ) mode |= 0002;
            if( perms.contains(PosixFilePermission.OTHERS_EXECUTE) ) mode |= 0001;

            return mode;
        }
        catch(final IOException e) {
            return DEFAULT_PERMISSION;
        }
    }

    private void _setPOSIXPermission(final Path path, final int mode) throws IOException
    {
        // NOTE : Special modes (setuid, setgid, sticky bit) are not supported !!!

        final HashSet<PosixFilePermission> pfp  = new HashSet<>();

        if( (mode & 0400) != 0 ) pfp.add(PosixFilePermission.OWNER_READ    );
        if( (mode & 0200) != 0 ) pfp.add(PosixFilePermission.OWNER_WRITE   );
        if( (mode & 0100) != 0 ) pfp.add(PosixFilePermission.OWNER_EXECUTE );

        if( (mode & 0040) != 0 ) pfp.add(PosixFilePermission.GROUP_READ    );
        if( (mode & 0020) != 0 ) pfp.add(PosixFilePermission.GROUP_WRITE   );
        if( (mode & 0010) != 0 ) pfp.add(PosixFilePermission.GROUP_EXECUTE );

        if( (mode & 0004) != 0 ) pfp.add(PosixFilePermission.OTHERS_READ   );
        if( (mode & 0002) != 0 ) pfp.add(PosixFilePermission.OTHERS_WRITE  );
        if( (mode & 0001) != 0 ) pfp.add(PosixFilePermission.OTHERS_EXECUTE);

        Files.setPosixFilePermissions(path, pfp);
    }

    protected void _compressDir(final String targenFilePath, final String srcDirPath, final String appendParentDirName) throws IOException
    {
        // List the source directory
        final String       srcDirAbsPath = SysUtil.resolveAbsolutePath(srcDirPath);
        final List<String> strList       = SysUtil.cu_lsfd_rec(srcDirAbsPath);

        strList.remove(srcDirAbsPath);

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

        // Map to store <FileKey, FirstEntryName> for Hard Link detection
        final Map<Object, String> hardLinkMap = new HashMap<>();

        // Append the parent directory as needed
        String apdn = (appendParentDirName != null) ? appendParentDirName.trim() : "";

        if( !apdn.isEmpty() ) {
            if( apdn.charAt( apdn.length() - 1 ) != SysUtil._InternalDirSep ) apdn += SysUtil._InternalDirSep;
            tos.putNextEntry( new TarEntry(
                TarHeader.createHeader( apdn, 0, Instant.now().getEpochSecond(), true, SysUtil.osIsWindows() ? 0770 : 0775 )
            ) );
        }

        // Compress the input files and directories
        final Path sourceBase = Paths.get(srcDirAbsPath);
        final byte data[]     = new byte[CDBufferSize];

        for(final File file : inFiles) {

            // Determine the entry name
            final Path   path         = file.toPath();
            final String relativePath = SysUtil.normalizeDirectorySeparators( sourceBase.relativize(path).toString() );
            final String entryName    = apdn + relativePath;

            // Detect links
            final BasicFileAttributes attrs        = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            final Object              fileKey      = attrs.fileKey();
            final boolean             isSymLink    = attrs.isSymbolicLink();
            final String              hardLinkPath = (fileKey != null) ? hardLinkMap.get(fileKey) : null;

            // Symbolic link
            if(isSymLink) {
                final String target = Files.readSymbolicLink(path).toString();
                /*
                SysUtil.stdDbg().printf("[C-SYM] '%s' -> '%s'\n", entryName, target);
                //*/
                tos.putNextEntry( new TarEntry(
                    TarHeader.createHeader(entryName, 0, file.lastModified() / 1000, false, 0777, TarHeader.LF_SYMLINK, target)
                ) );
            }
            // Hard link
            else if( hardLinkPath != null && !attrs.isDirectory() ) {
                final int mode = _getPOSIXPermissions(path);
                /*
                SysUtil.stdDbg().printf("[C-LNK] '%s' -> '%s'\n", entryName, hardLinkPath);
                //*/
                tos.putNextEntry( new TarEntry(
                    TarHeader.createHeader(entryName, 0, file.lastModified() / 1000, false, mode, TarHeader.LF_LINK, hardLinkPath)
                ) );
            }
            // Normal file or directory (skips LF_CHR, LF_BLK, LF_FIFO, etc.)
            else if( attrs.isRegularFile() || attrs.isDirectory() ) {
                final String mapEntry = ( entryName.startsWith(SysUtil._InternalDirSepStr) ) ? entryName.substring(1) : entryName;

                /*
                SysUtil.stdDbg().printf("[C-FIL] '%s'\n", mapEntry);
                //*/

                if( fileKey != null && !attrs.isDirectory() ) hardLinkMap.put(fileKey, mapEntry);

                final boolean isDir     = attrs.isDirectory();
                final long    entrySize = isDir ? 0 : file.length();
                final int     mode      = _getPOSIXPermissions(path);

                tos.putNextEntry( new TarEntry(
                    TarHeader.createHeader(entryName, entrySize, file.lastModified() / 1000, isDir, mode)
                ) );

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
        if( SysUtil._isDangerousPath(dstName) ) return;

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
                    SysUtil.stdDbg().printf("[D-DIR] '%s'\n", dstName);
                    //*/
                    SysUtil.cu_mkdir(dstName);
                }

                // If the entry is a symbolic link, create it
                else if(tarHeader.linkFlag == TarHeader.LF_SYMLINK) {
                    final String linkTarget = tarHeader.linkName.toString();
                    try {
                        // Try to create the symbolic link
                        /*
                        SysUtil.stdDbg().printf("[D-SYM] '%s' -> '%s'\n", dstName, linkTarget);
                        //*/
                        SysUtil.cu_ln_s(dstName, linkTarget, true);
                    }
                    catch(final IOException e) {
                        // Fallback - copy the target file to 'dstName'
                        try {
                            // Find where the target is supposed to be relative to the link
                            final String targetAbs = SysUtil.resolveAbsolutePath(
                                SysUtil.resolvePath( linkTarget, SysUtil.getDirName(dstName) )
                            );
                            // Only copy if the target exists as a file
                            if( SysUtil.pathIsValidFile(targetAbs) ) {
                                // Copy the file
                                SysUtil.cu_cpfile(targetAbs, dstName, true, true);
                                // Set the POSIX permission as needed
                                if( SysUtil.osIsPOSIX() ) {
                                    _setPOSIXPermission( Paths.get(dstName), _getPOSIXPermissions( Paths.get(targetAbs) ) );
                                }
                            }
                        }
                        catch(final IOException ignored) {
                            // Ignore fallback failure
                        }
                    }
                    continue;
                }

                // If the entry is a hard link, create it
                else if(tarHeader.linkFlag == TarHeader.LF_LINK) {
                    final String linkTarget1 = SysUtil.resolvePath( tarHeader.linkName.toString(), dstDirAbsPath               );
                    final String linkTarget2 = SysUtil.resolvePath( tarHeader.linkName.toString(), SysUtil.getDirName(dstName) );
                    final String linkTarget  = SysUtil.pathIsValid(linkTarget1) ? linkTarget1 : linkTarget2;
                    try {
                        // Try to create the hard link
                        /*
                        SysUtil.stdDbg().printf( "[D-LNK] '%s' -> '%s' (%b)\n", dstName, linkTarget, SysUtil.pathIsValidFile(linkTarget) );
                        //*/
                        SysUtil.cu_ln_h(dstName, linkTarget, true);
                    }
                    catch(final IOException e) {
                        // Fallback - copy the target file to 'dstName'
                        try {
                            final String linkTargetAbs = SysUtil.resolveAbsolutePath(linkTarget);
                            // Only copy if the target exists as a file
                            if( SysUtil.pathIsValidFile(linkTargetAbs) ) {
                                // Copy the file
                                SysUtil.cu_cpfile( linkTargetAbs, dstName, true, true );
                                // Set the POSIX permission as needed
                                if( SysUtil.osIsPOSIX() ) {
                                    _setPOSIXPermission( Paths.get(dstName), _getPOSIXPermissions( Paths.get(linkTargetAbs) ) );
                                }
                            }
                        }
                        catch(final IOException ignored) {
                            // Ignore fallback failure
                        }
                    }
                    continue;
                }

                // If the entry is a normal/contigous file, decompress it
                else if(tarHeader.linkFlag == '\0' || tarHeader.linkFlag == TarHeader.LF_NORMAL || tarHeader.linkFlag == TarHeader.LF_CONTIG) {
                    // Check if the destination directory does exist
                    final String dstDir = SysUtil.getDirName(dstName);
                    if( !SysUtil.pathIsValidDirectory(dstDir) )  {
                        // Create the directory because it does not exist yet
                        /*
                        SysUtil.stdDbg().printf("[D-DIR] '%s'\n", dstDir);
                        //*/
                        SysUtil.cu_mkdir(dstDir);
                    }
                    // Uncompress and write the file
                    /*
                    SysUtil.stdDbg().printf("[D-FIL] '%s'\n", dstName);
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
                    // Process the 'g' and 'x' entries
                    if( tarHeader.linkFlag == (byte) 'g' || tarHeader.linkFlag == (byte) 'x' ) {
                        if(true) {
                            // Discard the data
                            while( tis.read(data) != -1 );
                            // ##### ??? TODO : Parse and use ??? #####
                            // <total-length> <key>=<value>\n
                        }
                        else {
                            // Make directory, it should be something like /PaxHeaders\\.\d+/
                            SysUtil.cu_mkdir( SysUtil.getDirName(dstName) );
                            // Uncompress and write the file
                            _uncompressAndWriteFile(dstName, tis, data);
                        }
                    }
                    // Ignore the 'A' - 'Z' entries
                    else {
                        // Discard the data
                        while( tis.read(data) != -1 );
                        /*
                         * ##### ??? TODO ??? #####
                         * 'A' : Solaris ACL data
                         * 'E' : Solaris extended header
                         * 'I' : Inode metadata (some implementations)
                         * 'N' : GNU tar old long filename (pre-L/K, obsolete)
                         * 'S' : Sparse file descriptor (GNU tar)
                         * 'V' : Volume label (GNU tar)
                         * 'X' : POSIX.1-2001 extended header (some older implementations, similar to 'x')
                         */
                        /*
                            // After creating the file, for each (offset, size) pair in the sparse map:
                            try(
                                final FileChannel fc = FileChannel.open( Paths.get(dstName), StandardOpenOption.WRITE, StandardOpenOption.CREATE )
                            ) {
                                // Set the final file size first (creates implicit hole from 0 to size)
                                fc.truncate(totalSize);
                                // Write only the real data regions at their correct offsets
                                for(each sparse region) {
                                    fc.position(regionOffset);
                                    fc.write(dataBuffer);
                                }
                            }
                         */
                    }
                    continue;
                }

                // Unsupported entry (LF_CHR, LF_BLK, LF_FIFO, etc.)
                else {
                    if(tarHeader.linkFlag >= 32 && tarHeader.linkFlag <= 126) {
                        throw XCom.newIOException("unsupported tar entry type '%c'", (char) tarHeader.linkFlag);
                    }
                    else {
                        throw XCom.newIOException("unsupported tar entry type #%d", tarHeader.linkFlag & 0xFF);
                    }
                }

                // Set the POSIX permission as needed
                if( SysUtil.osIsPOSIX() ) _setPOSIXPermission( Paths.get(dstName), tarHeader.mode );

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


/*
$tgzdir_rec('test.tar.gz', '/tmp/z', '/tmp/z')
$untgz_rec ('test.tar.gz', '/tmp/q')
$exit(0)


rm -rvf /tmp/z
mkdir /tmp/z
echo "12345" > /tmp/z/batch1.txt
echo "ABCDE" > /tmp/z/batch2.txt
ln    /tmp/z/batch1.txt /tmp/z/batch1a.txt
ln -s        batch2.txt /tmp/z/batch2a.txt
ls -li /tmp/z


In 'JxMake/test' directory
    rm -rvf tmp && tar -xzvpf test.tar.gz && ls -li tmp/z && rm -rvf tmp && ls -li /tmp/z && ls -li /tmp/q/tmp/z
*/
