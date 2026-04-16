/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import java.util.regex.Pattern;

import jxm.*;
import jxm.gcomp.*;
import jxm.xb.*;
import jxm.xioe.*;


public class CommandShell {

    public static final HashSet<String> DEFAULT_ARCHIVE_EXTS = new HashSet<>( Arrays.asList(
        // Legacy archive/container formats
        "arj",   // ARJ     (DOS/Windows)
        "arc",   // ARC     (early DOS)
        "ace",   // ACE     (Windows proprietary)
        "zoo",   // ZOO     (Unix legacy)
        "lha",   // LHA     (Amiga/DOS, Japan)
        "lzh",   // LZH     (variant of LHA)
        "sit",   // StuffIt (classic MacOS)

        // Common archive/container formats
        "zip",   // ZIP   (ubiquitous)
        "rar",   // RAR   (Windows/Unix)
        "7z",    // 7-Zip (modern high compression)
        "cab",   // Windows Cabinet
        "wim",   // Windows Imaging Format
        "iso",   // Optical disk image
        "dmg",   // MacOS   disk image

        // Tar-based and compression layers
        "tar",   // tar archive
        "gz",    // gzip
        "bz2",   // bzip2
        "xz",    // xz (LZMA2)
        "lz",    // lzip
        "lzma",  // LZMA
        "tgz",   // tar.gz   shorthand
        "tbz",   // tar.bz2  shorthand
        "tbz2",  // tar.bz2  shorthand
        "txz",   // tar.xz   shorthand
        "tlz",   // tar.lz   shorthand
        "tlzma", // tar.lzma shorthand
        "cpio",  // cpio archive
        "z",     // compress (.Z)
        "a",     // static library archive (.a)

        // Installer package formats
        "deb",   // Debian package
        "rpm",   // Red Hat package
        "msi",   // Windows installer
        "pkg",   // MacOS installer package
        "apk",   // Android package

        // Application/module bundles
        "jar",   // Java archive
        "war",   // Web application archive
        "ear",   // Enterprise application archive
        "app",   // macOS application bundle
        "pak"    // Game/asset package
    ) );

    public static String lsDefault_getSpecialColor(final Path path, final BasicFileAttributes attrs, final String mark, final String ATTR_COLOR_ARCHIVE_FILE)
    {
         final String ext = SysUtil.getFileExtension( path.toString() ).toLowerCase();

         return DEFAULT_ARCHIVE_EXTS.contains(ext) ? ATTR_COLOR_ARCHIVE_FILE : null;
    }

    public static abstract class ConfigLS {

        public final String ATTR_COLOR_RESET;

        public final String ATTR_COLOR_BLINK;
        public final String ATTR_COLOR_NO_BLINK;

        public final String ATTR_COLOR_DIRECTORY;
        public final String ATTR_COLOR_FILE;
        public final String ATTR_COLOR_EXECUTABLE_FILE;
        public final String ATTR_COLOR_ARCHIVE_FILE;    // NOTE : By default, it is not used by the 'ls()' functions below; override 'getSpecialColor()' to enable its usage
        public final String ATTR_COLOR_SYMLINK;
        public final String ATTR_COLOR_BROKEN_SYMLINK;
        public final String ATTR_COLOR_OTHER;

        public final int    COLUMN_SPACING;
        public final int    CONSOLE_COLUMNS;

        public ConfigLS(final int consoleColumns)
        {
            ATTR_COLOR_RESET           = ANSIScreenBuffer.ASeq_Attr_RstAll;

            ATTR_COLOR_BLINK           = ANSIScreenBuffer.ASeq_Attr_SetBlink;
            ATTR_COLOR_NO_BLINK        = ANSIScreenBuffer.ASeq_Attr_RstBlink;

            ATTR_COLOR_DIRECTORY       = ANSIScreenBuffer.ASeq_Attr_SetBrightBlue;
            ATTR_COLOR_FILE            = ANSIScreenBuffer.ASeq_Attr_SetBrightWhite;
            ATTR_COLOR_EXECUTABLE_FILE = ANSIScreenBuffer.ASeq_Attr_SetBrightGreen;
            ATTR_COLOR_ARCHIVE_FILE    = ANSIScreenBuffer.ASeq_Attr_SetBrightMagenta;
            ATTR_COLOR_SYMLINK         = ANSIScreenBuffer.ASeq_Attr_SetBrightCyan;
            ATTR_COLOR_BROKEN_SYMLINK  = ANSIScreenBuffer.ASeq_Attr_SetBrightRed;
            ATTR_COLOR_OTHER           = ANSIScreenBuffer.ASeq_Attr_SetBrightYellow;

            COLUMN_SPACING             = 2;
            CONSOLE_COLUMNS            = (consoleColumns > COLUMN_SPACING) ? (consoleColumns - COLUMN_SPACING) : 0;
        }

        public abstract String getSpecialMark(final Path path, final BasicFileAttributes attrs);
        public abstract String getSpecialColor(final Path path, final BasicFileAttributes attrs, final String mark);

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static class Default extends ConfigLS  {

            public Default(final int consoleColumns)
            { super(consoleColumns); }

            @Override
            public String getSpecialMark(final Path path, final BasicFileAttributes attrs)
            { return null; }

            @Override
            public String getSpecialColor(final Path path, final BasicFileAttributes attrs, final String mark)
            { return lsDefault_getSpecialColor(path, attrs, mark, ATTR_COLOR_ARCHIVE_FILE); }

        } // class Default

    } // class ConfigLS

    private static final Pattern _pmWhitespacesExceptESC = Pattern.compile("[\\s&&[^\\u001B]]+"     );

    private static List<File> _sortedFiles(final File dir, final boolean acceptFile) throws IOException
    {
        if( !dir.exists() ) throw new FileNotFoundException( dir.toString() );

        if( !dir.isDirectory() ) {
            if(acceptFile) return Collections.singletonList(dir);
            throw new NotDirectoryException( dir.toString() );
        }

        final File[] files = dir.listFiles();

        if(files == null) return Collections.emptyList();

        final List<File> list = new ArrayList<>( Arrays.asList(files) );

        list.sort( new Comparator<File>() {
            @Override
            public int compare(final File a, final File b)
            {
                if(  a.isDirectory() && !b.isDirectory() ) return -1;
                if( !a.isDirectory() &&  b.isDirectory() ) return  1;
                return a.getName().compareToIgnoreCase( b.getName() );
            }
        } );

        return list;
    }

    private static BasicFileAttributes _readAttrs(final Path p) throws IOException
    { return Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); }

    private static String _resolveType(final Path p, final BasicFileAttributes a)
    {
        String type;

             if( a.isSymbolicLink ( ) ) type = "l";
        else if( a.isOther        ( ) ) type = "o";
        else if( Files.isDirectory(p) ) type = "d";
        else                            type = "-";

        type += Files.isReadable  (p) ? 'r' : '-';
        type += Files.isWritable  (p) ? 'w' : '-';
        type += Files.isExecutable(p) ? 'x' : '-';

        return type;
    }

    private static String _getMark(final ConfigLS cfg, final Path p, final BasicFileAttributes a)
    {
        final String mark = cfg.getSpecialMark(p, a);

        return (mark == null) ? "" : mark.trim();
    }

    public static Path _getSymLinkTarget(final Path path, final BasicFileAttributes attrs)
    {
        if( !attrs.isSymbolicLink() ) return null;

        try {
            return Files.readSymbolicLink(path);
        }
        catch(final IOException e) {
            return Paths.get("?");
        }
    }

    private static String _resolveColor(final ConfigLS cfg, final Path p, final boolean slGood, final BasicFileAttributes a, final String mark) throws IOException
    {
        final String c = cfg.getSpecialColor(p, a, mark);
        if( c != null ) {
            final String t = XCom.re_replace(c, _pmWhitespacesExceptESC, "");
            if( !t.isEmpty() ) return t;
        }

        if(     a.isSymbolicLink( ) ) return slGood ? cfg.ATTR_COLOR_SYMLINK : cfg.ATTR_COLOR_BROKEN_SYMLINK;
        if(     a.isOther       ( ) ) return cfg.ATTR_COLOR_OTHER;
        if( Files.isDirectory   (p) ) return cfg.ATTR_COLOR_DIRECTORY;
        if( Files.isExecutable  (p) ) return cfg.ATTR_COLOR_EXECUTABLE_FILE;
                                      return cfg.ATTR_COLOR_FILE;
    }

    // NOTE : Use a uniform, global column width
    private static String _lsShort_u(final List<File> files, final ConfigLS cfg) throws IOException
    {
        // List files
        final List<String[]> entries = new ArrayList<>();
              int            longest = 0;

        for(final File f : files) {

            final Path                p     = f.toPath();
            final BasicFileAttributes attrs = _readAttrs(p);

            final Path                slTrg  = _getSymLinkTarget(p, attrs);
            final Path                slRslv = (slTrg  != null) ? p.resolveSibling(slTrg ) : null;
            final Boolean             slGood = (slRslv != null) ? Files.exists    (slRslv) : false;

            final String              name  = f.getName();
            final String              mark  = _getMark(cfg, p, attrs);

            final String              color = _resolveColor(cfg, p, slGood, attrs, mark);

            entries.add( new String[] { name, color } );

            longest = Math.max( longest, name.length() );

        } // for

        // Calculate the number of columns and rows
        final int    colWidth = longest + cfg.COLUMN_SPACING;
        final int    cols     = Math.max(1, cfg.CONSOLE_COLUMNS / colWidth);
        final int    rows     = (int) Math.ceil( (float) entries.size() / cols );
        final String fmt      = "%-" + colWidth + "s";

        // Print the list
        final StringBuilder sb = new StringBuilder();

        for(int r = 0; r < rows; ++r) {

            for(int c = 0; c < cols; ++c) {

                final int idx = r + c * rows;

                if( idx >= entries.size() ) continue;

                final String[] e = entries.get(idx);

                sb.append(                    e[1]  )
                  .append( String.format(fmt, e[0]) )
                  .append( cfg.ATTR_COLOR_RESET     );

            } // for

            sb.append("\n");

        } // for

        return sb.toString();
    }

    // NOTE : Use a specific, per‑column width
    private static String _lsShort_s(final List<File> files, final ConfigLS cfg) throws IOException
    {
        // List files
        final List<String[]> entries = new ArrayList<>();

        for(final File f : files) {

            final Path                p     = f.toPath();
            final BasicFileAttributes attrs = _readAttrs(p);

            final Path                slTrg  = _getSymLinkTarget(p, attrs);
            final Path                slRslv = (slTrg  != null) ? p.resolveSibling(slTrg ) : null;
            final Boolean             slGood = (slRslv != null) ? Files.exists    (slRslv) : false;

            final String              name  = f.getName();
            final String              mark  = _getMark(cfg, p, attrs);

            final String              color = _resolveColor(cfg, p, slGood, attrs, mark);

            entries.add( new String[] { name, color } );

        } // for

        if( entries.isEmpty() ) return "";

        // Compute per-column widths
        int           maxCols   = entries.size(); // Worst case
        int           cols      = 1;
        List<Integer> colWidths = null;

        for(int cGuess = 1; cGuess <= maxCols; ++cGuess) {

            final int           rows       = (int) Math.ceil( (float) entries.size() / cGuess );
            final List<Integer> widths     = new ArrayList<>();
                  int           totalWidth = 0;

            for(int c = 0; c < cGuess; ++c) {

                int maxLen = 0;

                for(int r = 0; r < rows; ++r) {

                    final int idx = r + c * rows;

                    if( idx >= entries.size() ) break;

                    maxLen = Math.max( maxLen, entries.get(idx)[0].length() );

                } // for

                final int w = maxLen + cfg.COLUMN_SPACING;
                widths.add(w);
                totalWidth += w;

            } // for

            if(totalWidth > cfg.CONSOLE_COLUMNS) break;

            cols      = cGuess;
            colWidths = widths;

        } // for

        if(colWidths == null) colWidths = new ArrayList<>();

        if( colWidths.isEmpty() ) {
            cols = 1;
            colWidths.add(1);
        }

        final int rows = (int) Math.ceil( (float) entries.size() / cols );

        // Format the list and return the result as a string
        final StringBuilder sb = new StringBuilder();

        for(int r = 0; r < rows; ++r) {

            for(int c = 0; c < cols; ++c) {

                final int    idx = r + c * rows;
                final String fmt = "%-" + colWidths.get(c) + "s";

                if( idx >= entries.size() ) continue;

                final String[] e = entries.get(idx);

                sb.append(                    e[1]  )
                  .append( String.format(fmt, e[0]) )
                  .append( cfg.ATTR_COLOR_RESET     );

            } // for

            sb.append("\n");

        } // for

        return sb.toString();
    }

    private static String _lsLong(final List<File> files, final ConfigLS cfg) throws IOException
    {
        // Generate the spaces
        final String spcs = String.join( "", Collections.nCopies(cfg.COLUMN_SPACING, " ") );

        // List files
        final DateTimeFormatter dtf       = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final StringBuilder     sb        = new StringBuilder();
        final List<Object[]>    rows      = new ArrayList<>();
              int               maxName   = 0;
              int               maxSize   = 0;
              int               maxModify = 0;
              int               maxCreate = 0;
              int               maxMark   = 0;

        for(final File f : files) {

            final Path                p      = f.toPath();
            final BasicFileAttributes attrs  = _readAttrs(p);

            final Path                slTrg  = _getSymLinkTarget(p, attrs);
            final Path                slRslv = (slTrg  != null) ? p.resolveSibling(slTrg ) : null;
            final Boolean             slGood = (slRslv != null) ? Files.exists    (slRslv) : false;

            final String              type   = _resolveType(p, attrs);
            final String              name   = f.getName();
            final String              size   = String.valueOf( attrs.size() );
            final String              modify = attrs.lastModifiedTime().toInstant().atZone( TimeZone.getDefault().toZoneId() ).toLocalDateTime().format(dtf);
            final String              create = attrs.creationTime    ().toInstant().atZone( TimeZone.getDefault().toZoneId() ).toLocalDateTime().format(dtf);
            final String              mark   = _getMark(cfg, p, attrs);

            maxName   = Math.max( maxName  , name  .length() );
            maxSize   = Math.max( maxSize  , size  .length() );
            maxModify = Math.max( maxModify, modify.length() );
            maxCreate = Math.max( maxCreate, create.length() );
            maxMark   = Math.max( maxMark  , mark  .length() );

            String slDesc = "";
            if(slTrg != null) {
                if(slGood)  {
                    slDesc  = "->" + spcs;
                    slDesc += _resolveColor( cfg, slRslv, slGood, _readAttrs(slRslv), mark );
                    slDesc += slTrg;
                    slDesc += cfg.ATTR_COLOR_RESET;
                }
                else {
                    slDesc  = "->" + spcs;
                    slDesc += cfg.ATTR_COLOR_BLINK;
                    slDesc += slTrg;
                    slDesc += cfg.ATTR_COLOR_NO_BLINK;
                }
            }

            //                       0     1     2     3       4       5     6       7  8      9
            rows.add( new Object[] { type, name, size, modify, create, mark, slDesc, p, attrs, slGood } );

        } // for

        if( rows.isEmpty() ) return "";

        // Format the list and return the result as a string
        final String fmt1 = "%-" + maxName   + "s" + spcs;
        final String fmt2 = "%"  + maxSize   + "s" + spcs
                          + "%-" + maxCreate + "s" + spcs
                          + "%-" + maxModify + ( (maxMark > 0) ? ("s" + spcs + "%-" + maxMark + "s") : "s%s" ) + spcs
                          + "%s";

        for(final Object[] row : rows) {

            final Path   p     = (Path) row[7];
            final String color = _resolveColor( cfg, p, (Boolean) row[9], (BasicFileAttributes) row[8], (String) row[5] );

            sb.append( row[0] + spcs                                                 )
              .append( color                                                         )
              .append( String.format( fmt1, row[1]                                 ) )
              .append( cfg.ATTR_COLOR_RESET                                          )
              .append( String.format( fmt2, row[2], row[3], row[4], row[5], row[6] ) )
              .append( "\n"                                                          );

        } // for

        return sb.toString();
    }

    public static String ls(final File dir, final boolean longMode, final ConfigLS config) throws IOException
    {
        final List<File> files = _sortedFiles(dir, longMode);
        if( files.isEmpty() ) return "";

      //return longMode ? _lsLong(files, config) : _lsShort_u(files, config);
        return longMode ? _lsLong(files, config) : _lsShort_s(files, config);
    }

    public static String ls(final String path_, final boolean longMode, final ConfigLS cfg) throws IOException
    {
        String path = (path_ != null) ? path_.trim() : "";
        if( path.isEmpty() ) path = ".";

        return ls( new File(path), longMode, cfg );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String CMD_READ_TEXT_FILE  = "rdfile";
    public static final String CMD_WRITE_TEXT_FILE = "wrfile";
    public static final String CMD_MAKE_TEXT_FILE  = "mkfile";

    public static final String CMD_LIST_FULL       = "lsrec";
    public static final String CMD_COPY_FULL       = "cprec";
    public static final String CMD_REMOVE_FULL     = "rmrec";
    public static final String CMD_MOVE_FULL       = "mvrec";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _CMD_READ_TEXT_FILE_impl(final String filePath) throws IOException
    {
        try(
            final FileInputStream fis = new FileInputStream(filePath)
        ) {
            int b;
            while( ( b = fis.read() ) != -1) System.out.write(b);
            System.out.flush();
        }
        catch(final IOException e) {
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _CMD_WRITE_TEXT_FILE_impl(final String filePath) throws IOException
    {
        try (
            final FileOutputStream fos = new FileOutputStream(filePath)
        ) {
            int b;
            while( ( b = System.in.read() ) != -1 ) fos.write(b);
            fos.flush();
        }
        catch(final IOException e) {
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _CMD_MAKE_TEXT_FILE_impl(final String filePath, final String data) throws IOException
    {
        try (
            final FileOutputStream fos = new FileOutputStream(filePath)
        ) {
            final byte[] bytes = data.getBytes();
            fos.write(bytes);
            fos.flush();
        }
        catch(final IOException e) {
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _CMD_LIST_FULL_impl_rec(final Path cwd, final boolean longMode, final ConfigLS configLS, final File dirPath) throws IOException
    {
        final List<File> files = _sortedFiles(dirPath, longMode);
        if( files.isEmpty() ) return;

      //final String res = longMode ? _lsLong(files, configLS) : _lsShort_u(files, configLS);
        final String res = longMode ? _lsLong(files, configLS) : _lsShort_s(files, configLS);

        Path infoPath = dirPath.toPath();
        if( !dirPath.isDirectory() ) infoPath = infoPath.getParent();

        if(cwd != null) System.out.println( SysUtil.normalizeDirectorySeparators( "./" + cwd.relativize(infoPath).toString() ) + ':' );
        else            System.out.println( SysUtil.normalizeDirectorySeparators(                       infoPath .toString() ) + ':' );

        System.out.print(res);
        System.out.println();

        for(final File f : files) {

            if( !f.isDirectory() ) continue;

            _CMD_LIST_FULL_impl_rec(cwd, longMode, configLS, f);

        } // for
    }

    private static void _CMD_LIST_FULL_impl(final String mode, final String colCnt, final String dirPath) throws IOException
    {
        final boolean  longMode = "-l".equals(mode);
        final ConfigLS configLS = new ConfigLS.Default( Integer.parseInt(colCnt) );
        final String   cwdStr   = SysUtil.getCWD();

        _CMD_LIST_FULL_impl_rec(
            dirPath.startsWith(cwdStr) ? Paths.get(cwdStr) : null,
            longMode,
            configLS,
            new File(dirPath)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add confirmation feature ??? #####

    private static void _CMD_COPY_FULL_impl(final String srcPath, final String dstPath) throws IOException
    {
        if( srcPath == null || srcPath.isEmpty() ) throw new IllegalArgumentException("<srcPath>");
        if( dstPath == null || dstPath.isEmpty() ) throw new IllegalArgumentException("<dstPath>");

        if( SysUtil._isDangerousPath(srcPath) ) throw new UnsafePathException("<srcPath>");
        if( SysUtil._isDangerousPath(dstPath) ) throw new UnsafePathException("<dstPath>");

        final Path src = Paths.get(srcPath);
        final Path dst = Paths.get(dstPath);

        if( !Files.exists(src, LinkOption.NOFOLLOW_LINKS)          ) throw new FileNotFoundException          ( src.toString() );
        if(  Files.exists(dst, LinkOption.NOFOLLOW_LINKS) ) {
            if( !Files.isDirectory(dst, LinkOption.NOFOLLOW_LINKS) ) throw new DirectoryAlreadyExistsException( dst.toString() );
            else                                                     throw new FileAlreadyExistsException     ( dst.toString() );
        }

        // Copy single file
        if( Files.isRegularFile(src, LinkOption.NOFOLLOW_LINKS) ) {

            System.out.printf("%s -> %s\n", src, dst);

            Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES);

        }

        // Recursive directory copy
        else if( Files.isDirectory(src, LinkOption.NOFOLLOW_LINKS) ) {

            Files.walkFileTree(src, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException
                {
                    if( Files.isSymbolicLink(dir) ) return FileVisitResult.SKIP_SUBTREE;

                    if( dst.startsWith(src) ) throw new IllegalArgumentException( dst.toString() + '⊂' + src.toString() );

                    final Path trg = dst.resolve( src.relativize(dir) );

                    if( !Files.exists(trg) ) {

                        System.out.printf("▤ %s -> %s\n", dir, trg);

                        Files.createDirectory(trg);
                        Files.setLastModifiedTime( trg, Files.getLastModifiedTime(dir) );

                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
                {
                    final Path trg = dst.resolve( src.relativize(file) );

                    if( Files.isSymbolicLink(file) ) {
                        System.out.printf("∞ %s -> %s\n", file, trg);
                        final Path lnk = Files.readSymbolicLink(file);
                        Files.createSymbolicLink(trg, lnk);
                    }
                    else if( attrs.isRegularFile() ) {
                        System.out.printf("• %s -> %s\n", file, trg);
                        Files.copy(file, trg, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                    else if( attrs.isOther() ) {
                        System.out.printf("‼ %s -> ✗\n", file, trg);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException e)
                {
                    System.err.printf( "✖ %s\n", e.toString() );

                    return FileVisitResult.CONTINUE;
                }

            } );

        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add confirmation feature ??? #####

    private static void _CMD_REMOVE_FULL_impl(final String path) throws IOException
    {
        if( path == null || path.isEmpty() ) throw new IllegalArgumentException("<path>");

        if( SysUtil._isDangerousPath(path) ) throw new UnsafePathException("<path>");

        final Path trg = Paths.get(path);

        if( !Files.exists(trg, LinkOption.NOFOLLOW_LINKS) ) throw new FileNotFoundException( trg.toString() );

        // If it iss a symlink or a regular file, just delete it
        if( Files.isRegularFile(trg, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(trg) ) {
            System.out.printf("␡ %s\n", trg);
            Files.delete(trg);
            return;
        }

        // If it is a directory, walk recursively but never follow symlinks
        if( Files.isDirectory(trg, LinkOption.NOFOLLOW_LINKS) ) {

            Files.walkFileTree(trg, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
                {
                    System.out.printf("␡ %s\n", file);

                    Files.delete(file);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException
                {
                    System.out.printf("␡ %s\n", dir);

                    Files.delete(dir);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException e) throws IOException
                {
                    System.err.printf( "✖ %s\n", e.toString() );

                    return FileVisitResult.CONTINUE;
                }

            } );

        }

        // Catch-all - delete anything else
        else {
            System.out.printf("␡ %s\n", trg);

            Files.delete(trg);
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add confirmation feature ??? #####

    private static void _CMD_MOVE_FULL_impl(final String[] srcPaths, final String dstPath) throws IOException
    {
        if( srcPaths == null || srcPaths.length == 0) throw new IllegalArgumentException("<srcPaths[]>");
        if( dstPath  == null || dstPath.isEmpty()   ) throw new IllegalArgumentException("<dstPath>"   );

        for(int i = 0; i < srcPaths.length; ++i) {
            if( SysUtil._isDangerousPath(srcPaths[i]) ) throw new UnsafePathException("<srcPaths[" + i + "]>");
        }
            if( SysUtil._isDangerousPath(dstPath    ) ) throw new UnsafePathException("<dstPath>"            );

        final Path dst = Paths.get(dstPath);

        // Rename
        if(srcPaths.length == 1) {

            final Path src = Paths.get( srcPaths[0] );

            if( !Files.exists(src, LinkOption.NOFOLLOW_LINKS)          ) throw new FileNotFoundException          ( src.toString() );
            if(  Files.exists(dst, LinkOption.NOFOLLOW_LINKS) ) {
                if( !Files.isDirectory(dst, LinkOption.NOFOLLOW_LINKS) ) throw new DirectoryAlreadyExistsException( dst.toString() );
                else                                                     throw new FileAlreadyExistsException     ( dst.toString() );
            }

            System.out.printf("» %s -> %s\n", src, dst);

            Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE);
        }

        // Move multiple into directory
        else {
            if( !Files.exists     (dst) ) throw new NoSuchDirectoryException( dst.toString() );
            if( !Files.isDirectory(dst) ) throw new NotDirectoryException   ( dst.toString() );

            for(final String srcPath : srcPaths) {

                final Path src = Paths.get(srcPath);
                if( !Files.exists(src, LinkOption.NOFOLLOW_LINKS)          ) throw new FileNotFoundException          ( src.toString() );

                final Path trg = dst.resolve( src.getFileName() );
                if(  Files.exists(trg, LinkOption.NOFOLLOW_LINKS) ) {
                    if( !Files.isDirectory(trg, LinkOption.NOFOLLOW_LINKS) ) throw new DirectoryAlreadyExistsException( trg.toString() );
                    else                                                     throw new FileAlreadyExistsException     ( trg.toString() );
                }

                System.out.printf("» %s -> %s\n", src, trg);

                Files.move(src, trg, StandardCopyOption.ATOMIC_MOVE);

            } // for
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String[] _cmdInvokeOtherMain_impl(final String otherClass, final String function, final String[] params)
    {
        // Copy the reconstructed command
        final ArrayList<String> cmd = SysUtil.getJavaCmd();

        // Remove everything after the JVM options up to the main class/JAR
        int    mainIndex = -1;
        String jar       = null;

        for( int i = 0; i < cmd.size(); ++i ) {

            if( "-cp".equals( cmd.get(i) ) ) {
                mainIndex = i + 2; // Skip -cp path and main class
                break;
            }

            else if( "-jar".equals( cmd.get(i) ) ) {
                jar       = cmd.get(i + 1); // Save the JAR file
                mainIndex = i;              // Skip the JAR file
                break;
            }

        } // for

        if( mainIndex > 0 && mainIndex < cmd.size() ) {
            // Trim off the original main class/JAR and its arguments
            while( cmd.size() > mainIndex ) cmd.remove( cmd.size() - 1 );
        }

        if(jar != null) {
            cmd.add("-cp");
            cmd.add(jar);
        }

        // Add the new main class
        cmd.add(otherClass);

        // Add new parameters
        if(function != null) cmd.add   ( function              );
        if(params   != null) cmd.addAll( Arrays.asList(params) );

        // Return the command
        return cmd.toArray( new String[0] );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void main(final String[] args)
    {
        try {

            switch( args[0] ) {

                case CMD_READ_TEXT_FILE  : for(int i = 1; i < args.length; ++i) _CMD_READ_TEXT_FILE_impl ( args[i]          ); break;
                case CMD_WRITE_TEXT_FILE : for(int i = 1; i < args.length; ++i) _CMD_WRITE_TEXT_FILE_impl( args[i]          ); break;
                case CMD_MAKE_TEXT_FILE  :                                      _CMD_MAKE_TEXT_FILE_impl ( args[1], args[2] ); break;

                case CMD_LIST_FULL       : _CMD_LIST_FULL_impl  ( args[1], args[2], args[3]                                           ); break;

                case CMD_COPY_FULL       : _CMD_COPY_FULL_impl  ( args[1]                                     , args[2]               ); break;
                case CMD_REMOVE_FULL     : _CMD_REMOVE_FULL_impl( args[1]                                                             ); break;
                case CMD_MOVE_FULL       : _CMD_MOVE_FULL_impl  ( Arrays.copyOfRange(args, 1, args.length - 1), args[args.length - 1] ); break;

            } // switch

        } // try
        catch(final Exception e) {
            // If we got here, print the stack trace unconditionally and exit
            System.err.println( e.toString() );
          //e.printStackTrace();
            SysUtil.systemExitError();
        }
    }

    public static String[] cmdInvokeFunction(final String function, final String[] params)
    { return _cmdInvokeOtherMain_impl( CommandShell.class.getName(), function, params ); }

    public static String[] cmdInvokeFunction(final String function, final String param)
    { return _cmdInvokeOtherMain_impl( CommandShell.class.getName(), function, new String[] { param } ); }

} // class CommandShell
