/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import java.nio.ByteOrder;

import java.nio.charset.Charset;

import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.Instant;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.ImageIcon;

import com.j256.simplemagic.ContentInfoUtil;

import org.fusesource.jansi.AnsiConsole;

import jxm.xb.*;
import jxm.xb.fci.*;
import jxm.xioe.*;


//
// System utility class
//
public class SysUtil {

    // ##### !!! NOTE : Synchronize with '../Makefile' !!!
    private static final long   _JXM_Ver_Major = 0;
    private static final long   _JXM_Ver_Minor = 9;
    private static final long   _JXM_Ver_Patch = 9;
    private static final long   _JXM_Ver_Value = (_JXM_Ver_Major << 16) | (_JXM_Ver_Minor << 8) | _JXM_Ver_Patch;
    private static final String _JXM_Ver_Devel = "tp2";

    private static final String _JXM_Copyright = "Copyright (C) 2022-2026 Aloysius Indrayanto\n\n"
                                               + "License LGPLv3+ : GNU LGPL version 3 or later <http://gnu.org/licenses/lgpl.html>\n\n"
                                               + "This is free software: you are free to change and redistribute it.\n"
                                               + "There is NO WARRANTY, to the extent permitted by law.\n";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _gel(final String envVarName, final String defaultValue)
    { return getEnv(envVarName, defaultValue).toLowerCase(); }

    private static String _geu(final String envVarName, final String defaultValue)
    { return getEnv(envVarName, defaultValue).toUpperCase(); }

    public static final String  _CharEncoding              = "UTF-8";                                    // !!! DO NOT CHANGE !!!
    public static final Charset _CharSet                   = Charset.forName(_CharEncoding);             // !!! DO NOT CHANGE !!!

    public static final Locale  _SystemLocale              = Locale.getDefault        ();                // !!! DO NOT CHANGE !!!
    public static final String  _SystemLanguage            = _SystemLocale.getLanguage();                // !!! DO NOT CHANGE !!!
    public static final String  _SystemCountry             = _SystemLocale.getCountry ();                // !!! DO NOT CHANGE !!!

    public static final String  _JxMakeLanguage            = _gel("JXMAKE_LANGUAGE", _SystemLanguage);   // !!! DO NOT CHANGE !!!
    public static final String  _JxMakeCountry             = _geu("JXMAKE_COUNTRY" , _SystemCountry );   // !!! DO NOT CHANGE !!!
    public static final String  _JxMakeLanguageCountry     = _JxMakeLanguage + '_' + _JxMakeCountry;     // !!! DO NOT CHANGE !!!
    public static final String  _JxMakeLanguageCountryFB   =       _JxMakeCountry.toLowerCase()          // !!! DO NOT CHANGE !!!
                                                           + '_' + _JxMakeCountry;
    public static final String  _FallbackLanguage          = "en";                                       // !!! DO NOT CHANGE !!!
    public static final String  _FallbackLanguageCountry   = "en_US";                                    // !!! DO NOT CHANGE !!!

    public static final char    _InternalDirSep            = '/';                                        // !!! DO NOT CHANGE !!!
    public static final String  _InternalDirSepStr         = "" + _InternalDirSep;                       // !!! DO NOT CHANGE !!!

    public static final String  _JxMakeProgramBinFileExt   = ".bin";                                     // !!! DO NOT CHANGE !!!
    public static final String  _JxMakeProgramJARResPrefix = "jar://";                                   // !!! DO NOT CHANGE !!!

    public static final String  _JxMakeTmpDirRoot          = "__jxmake__";      // Root temporary   directory (relative to the system root temporary directory)
    public static final String  _JxMakeUserDotDirRoot      = ".jxmake";         // User        data directory (relative to the user home directory)
    public static final String  _JxMakeAppDataDirRoot      = ".jxmake/appdata"; // Application data directory (relative to the user home directory)
    public static final String  _JxMakeDataRoot            = "0-JxMake";        // JxMake      data directory (relative to the user home directory, user data directory, certain system-specific directories, and the JxMake JAR file)

    public static final String  _JxMakeJARResRoot          = "res/";            // Root resource  directory (relative to the JxMake JAR file)
    public static final String  _JxMakeJARResDocTXTRoot    = _JxMakeJARResRoot  // Embedded documentation    files (*.txt ) directory (relative to the root resource directory)
                                                           + "docs/txt/";
    public static final String  _JxMakeJARResDocHTMLRoot   = _JxMakeJARResRoot  // Embedded documentation    files (*.html) directory (relative to the root resource directory)
                                                           + "docs/html/";
    public static final String  _JxMakeJARResLibRoot       = _JxMakeJARResRoot  // Embedded loadable-library files (*.jxm ) directory (relative to the root resource directory)
                                                           + _JxMakeDataRoot
                                                           + "/lib/";
    public static final String  _JxMakeMarkerFile          = "_MARKER_";        // Name of the "marker" file used to locate the resource directory

    public static final String  _JxMakeDistDir             = "jxmake_dist";     // JxMake distribution directory for third-party libraries and resource files, which may be updated independently by the user

    public static final long    _SerialVersionUID          = _JXM_Ver_Value;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static       String            _userHomeDir        = null;
    private static       String            _jxmakeUDataDir     = null;
    private static       String            _jxmakeUToolsDir    = null;
    private static       String            _jxmakeUDotDir      = null;
    private static       String            _jxmakeADatDir      = null;
    private static       String            _jxmakeExeDir       = null;

    private static       String            _curWorkDir         = null;
    private static final Stack<String>     _cwdStack           = new Stack<>();

    private static       String            _rootTmpDir         = null;
    private static       String            _savedProjectTmpDir = null;

    private static       ArrayList<String> _userAppDataDir     = new ArrayList<>();

    private static       String            _osArch             = null;
    private static       int               _osBitCount         = 0;
    private static       boolean           _osIs32Bit          = false;
    private static       boolean           _osIs64Bit          = false;
    private static       boolean           _osIsBE             = false;
    private static       boolean           _osIsLE             = false;

    private static       String            _osName             = null;
    private static       String            _osNameActual       = null;

    private static       boolean           _osIsWindows        = false;

    private static       boolean           _osIsPOSIX          = false;
    private static       boolean           _osIsLinux          = false;
    private static       boolean           _osIsMac            = false;
    private static       boolean           _osIsMacLegacy      = false; // MacOS 9 and earlier
    private static       boolean           _osIsBSD            = false;

    private static       boolean           _osIsCygwin         = false;
    private static       boolean           _osIsMinGW          = false;
    private static       boolean           _osIsMSys           = false;

    private static       boolean           _osIsPOSIXCompat    = false;

    private static       boolean           _silentSErrMsg      = false;
    private static       boolean           _silentSErrMsgSaved = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long jxmVerMajor()
    { return _JXM_Ver_Major; }

    public static long jxmVerMinor()
    { return _JXM_Ver_Minor; }

    public static long jxmVerPatch()
    { return _JXM_Ver_Patch; }

    public static long jxmVerValue()
    { return _JXM_Ver_Value; }

    public static String jxmVerDevel()
    { return _JXM_Ver_Devel; }

    public static String jxmVerString()
    { return String.format("%d.%d.%d-%s", _JXM_Ver_Major, _JXM_Ver_Minor, _JXM_Ver_Patch, _JXM_Ver_Devel); }

    public static String jxmCopyright()
    { return _JXM_Copyright; }

    public static String jxmVersionCopyright()
    { return "JxMake v" + jxmVerString() +'\n' + jxmCopyright(); }

    public static String jxmAboutString()
    { return Texts.JxMakeAboutStringText(); }

    public static String jxmAboutStringHTML()
    { return "<html><pre>" + XCom.escapeHTML( jxmAboutString() ) + "</pre></html>"; }

    private static long __jxmCompileParseVerStr(final String verStr) throws JXMException, NumberFormatException
    {
        final ArrayList<String> vt = XCom.explode(verStr, ".");

        if( vt.size() != 3 ) throw XCom.newJXMException(Texts.EMsg_JxMakeInvalidVer, verStr);

        final long major = Long.decode( vt.get(0) );
        final long minor = Long.decode( vt.get(1) );
        final long patch = Long.decode( vt.get(2) );

        return (major << 16) | (minor << 8) | patch;
    }

    public static boolean jxmCompileCheckMinVer(final String verStr) throws Exception
    { return _JXM_Ver_Value >= __jxmCompileParseVerStr(verStr); }

    public static boolean jxmCompileCheckMaxVer(final String verStr) throws Exception
    { return _JXM_Ver_Value <= __jxmCompileParseVerStr(verStr); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String JxMakeSerialVersionUID_FieldName = "__0_JxMake_SerialVersionUID__";

    public static final long extSerialVersionUID(final int extVer)
    { return (SysUtil._SerialVersionUID << 32) | extVer; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * The code for normalization of operating system strings is developed based on:
     *     https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java
     *     Copyright (C) 2014 Trustin Heuiseung Lee.
     *     Licensed under the Apache License, Version 2.0 (the "License").
     *     http://www.apache.org/licenses/LICENSE-2.0
     */

    private static final Pattern _pmNormalize = Pattern.compile("[^a-z0-9]+");

    private static String _osNormalizeString(final String str)
    { return (str == null) ? "" : _pmNormalize.matcher( str.toLowerCase(Locale.ENGLISH) ).replaceAll(""); }

    private static String _osNormalizeName(final String str_)
    {
        final String str = _osNormalizeString(str_);

        if( str.startsWith("aix"    )                                                                 ) return "aix";
        if( str.startsWith("hpux"   )                                                                 ) return "hpux";
        if( str.startsWith("os400"  ) && ( str.length() <= 5 || !Character.isDigit( str.charAt(5) ) ) ) return "os400";
        if( str.startsWith("linux"  )                                                                 ) return "linux";
        if( str.startsWith("mac"    ) || str.startsWith("osx"  ) || str.startsWith("darwin")          ) return "macos";
        if( str.startsWith("freebsd")                                                                 ) return "freebsd";
        if( str.startsWith("openbsd")                                                                 ) return "openbsd";
        if( str.startsWith("netbsd" )                                                                 ) return "netbsd";
        if( str.startsWith("solaris") || str.startsWith("sunos")                                      ) return "sunos";
        if( str.startsWith("windows")                                                                 ) return "windows";
        if( str.startsWith("zos"    )                                                                 ) return "zos";

        return "unknown";
    }

    private static String _osNormalizeArch(final String str_)
    {
        final String str = _osNormalizeString(str_);

        if( str.matches("^(x8664|amd64|ia32e|em64t|x64)$") ) return "x86_64";
        if( str.matches("^(x8632|x86|i[3-6]86|ia32|x32)$") ) return "x86_32";
        if( str.matches("^(ia64w?|itanium64)$"           ) ) return "itanium_64";
        if( str.equals ("ia64n"                          ) ) return "itanium_32";
        if( str.matches("^(sparc|sparc32)$"              ) ) return "sparc_32";
        if( str.matches("^(sparcv9|sparc64)$"            ) ) return "sparc_64";
        if( str.matches("^(arm|arm32)$"                  ) ) return "arm_32";
        if( str.equals ("aarch64"                        ) ) return "aarch_64";
        if( str.matches("^(mips|mips32)$"                ) ) return "mips_32";
        if( str.matches("^(mipsel|mips32el)$"            ) ) return "mipsel_32";
        if( str.equals ("mips64"                         ) ) return "mips_64";
        if( str.equals ("mips64el"                       ) ) return "mipsel_64";
        if( str.matches("^(ppc|ppc32)$"                  ) ) return "ppc_32";
        if( str.matches("^(ppcle|ppc32le)$"              ) ) return "ppcle_32";
        if( str.equals ("ppc64"                          ) ) return "ppc_64";
        if( str.equals ("ppc64le"                        ) ) return "ppcle_64";
        if( str.equals ("s390"                           ) ) return "s390_32";
        if( str.equals ("s390x"                          ) ) return "s390_64";
        if( str.matches("^(riscv|riscv32)$"              ) ) return "riscv";
        if( str.equals ("riscv64"                        ) ) return "riscv64";
        if( str.equals ("e2k"                            ) ) return "e2k";
        if( str.equals ("loongarch64"                    ) ) return "loongarch_64";

        return "unknown";
    }

    private static void _getOSNameArch()
    {
        // Get the OS name and set the corresponding flags
        final String OSName = System.getProperty("os.name", null);

        _osName = _osNormalizeName(OSName);

             if( _osName.equals("macos"  ) ) _osIsMac     = true;
        else if( _osName.equals("windows") ) _osIsWindows = true;
        else if( _osName.equals("linux"  ) ) _osIsLinux   = true;
        else if( _osName.equals("freebsd") ) _osIsBSD     = true;
        else if( _osName.equals("openbsd") ) _osIsBSD     = true;
        else if( _osName.equals("netbsd" ) ) _osIsBSD     = true;

        _osNameActual = OSName;

        if(_osIsWindows) {
                           String uname = execlp("uname", "-s"       ).toLowerCase();
            if( uname.isEmpty() ) uname = execlp("gcc"  , "--version").toLowerCase();
            _osIsCygwin = ( uname.indexOf("cygwin") >= 0 );
            _osIsMinGW  = ( uname.indexOf("mingw" ) >= 0 );
            _osIsMSys   = ( uname.indexOf("msys"  ) >= 0 );
        }

        if(_osIsMac) {
            if( _osNormalizeString(OSName).indexOf("x") < 0 ) _osIsMacLegacy = true;
        }

        _osIsPOSIX       = !_osIsWindows;
        _osIsPOSIXCompat =  _osIsCygwin || _osIsMinGW || _osIsMSys;

             if(_osIsCygwin) _osNameActual += " (Cygwin)";
        else if(_osIsMinGW ) _osNameActual += " (MinGW)";
        else if(_osIsMSys  ) _osNameActual += " (MSys)";

        // Get the OS architecture
        _osArch = _osNormalizeArch( System.getProperty("os.arch", "unknown") );

        final String dataModel = System.getProperty( "sun.arch.data.model", System.getProperty("com.ibm.vm.bitmode", null) );

        _osIs64Bit = (dataModel != null) ? dataModel.trim().equals("64") : _osArch.contains("64");
        _osIs32Bit = !_osIs64Bit;

        _osBitCount = _osIs64Bit ? 64 : 32;

        if( ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN   ) ) _osIsBE = true;
        if( ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN) ) _osIsLE = true;
    }

    public static String osName()
    {
        if(_osName == null) _getOSNameArch();
        return _osName;
    }

    public static String osNameActual()
    {
        if(_osNameActual == null) _getOSNameArch();
        return _osNameActual;
    }

    public static boolean osIsWindows()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsWindows;
    }

    public static boolean osIsPOSIX()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsPOSIX;
    }

    public static boolean osIsLinux()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsLinux;
    }

    public static boolean osIsBSD()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsBSD;
    }

    public static boolean osIsMac()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsMac;
    }

    public static boolean osIsMacLegacy()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsMacLegacy;
    }

    public static boolean osIsCygwin()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsCygwin;
    }

    public static boolean osIsMinGW()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsMinGW;
    }

    public static boolean osIsMSys()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsMSys;
    }

    public static boolean osIsPOSIXCompat()
    {
        if(_osName == null) _getOSNameArch();
        return _osIsPOSIXCompat;
    }

    public static String osArch()
    {
        if(_osArch == null) _getOSNameArch();
        return _osArch;
    }

    public static int osBitCount()
    {
        if(_osArch == null) _getOSNameArch();
        return _osBitCount;
    }

    public static boolean osIs32Bit()
    {
        if(_osArch == null) _getOSNameArch();
        return _osIs32Bit;
    }

    public static boolean osIs64Bit()
    {
        if(_osArch == null) _getOSNameArch();
        return _osIs64Bit;
    }

    public static boolean osIsBE()
    {
        if(_osArch == null) _getOSNameArch();
        return _osIsBE;
    }

    public static boolean osIsLE()
    {
        if(_osArch == null) _getOSNameArch();
        return _osIsLE;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int random8()
    { return (int) ( Math.random() * 0x100L ); }

    public static int random16()
    { return (int) ( Math.random() * 0x10000L ); }

    public static long random32()
    { return (long) ( Math.random() * 0x100000000L ); }

    public static long random64()
    { return (long) ( Math.random() * 0x7FFFFFFFFFFFFFFFL ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmHandleID = Pattern.compile("(?:\\000\\000)(.+?)(?:\\000)");
    private static final String  _rsHandleID = "──$1─";

    public static String createHandleID(final String idName, final long idSeq, final long idMask)
    { return String.format("\0\0@%s#%08x\0", idName, idSeq ^ idMask); }

    public static boolean isValidHandleID(final String idName, final String handle)
    {
        try {
            return ReCache._reGetMatcher( String.format("^\0\0@%s#[0-9a-f]{8}\0$", idName), handle ).matches();
        }
        catch(final JXMException e) {
            return false;
        }
    }

    public static String strHandleID(final String handle)
    { return _pmHandleID.matcher(handle).replaceAll(_rsHandleID); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long getNS()
    { return System.nanoTime(); }

    public static long getUS()
    { return System.nanoTime() / 1000; }

    public static long getMS()
    { return System.currentTimeMillis(); }

    public static void sleepUS(long timeout)
    {
        try {
            TimeUnit.MICROSECONDS.sleep(timeout);
        }
        catch(final Exception e) {}
    }

    public static void sleepMS(long timeout)
    {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        }
        catch(final Exception e) {}
    }

    public static String getEnv(final String envVarName)
    { return System.getenv(envVarName); }

    public static String getEnv(final String envVarName, final String defaultValue)
    {
        final String envVal = System.getenv(envVarName);

        return (envVal != null) ? envVal : defaultValue;
    }

    public static HashMap<String, String> getAllEnv()
    { return new HashMap<>( System.getenv() ); }

    public static void showWaitCursor(final Component component)
    {
        component.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
        Thread.yield();
    }

    public static void showDefaultCursor(final Component component)
    {
        component.setCursor( Cursor.getDefaultCursor() );
        Thread.yield();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String bytesToHexString(final byte[] bytes)
    {
        final StringBuilder sb = new StringBuilder();

        for(final byte b : bytes) {
            final int value = b & 0xFF;
            if(value < 16) sb.append("0");
            sb.append( Integer.toHexString(value) );
        }

        return sb.toString();
    }

    public static String computeCRC32(final String str)
    {
        final byte[]   bytes = str.getBytes();
        final Checksum crc32 = new CRC32();

        crc32.update(bytes, 0, bytes.length);

        return String.format( "%08x", crc32.getValue() );
    }

    public static String computeMD2Hash(final String str)
    {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD2");
            return bytesToHexString( md.digest( str.getBytes() ) );
        }
        catch(final NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String computeStringHash(final String str) throws NoSuchAlgorithmException
    {
        MessageDigest md = null;

        try { if(md == null) md = MessageDigest.getInstance("SHA-512"); } catch(final NoSuchAlgorithmException e) {}
        try { if(md == null) md = MessageDigest.getInstance("SHA-384"); } catch(final NoSuchAlgorithmException e) {}
        try { if(md == null) md = MessageDigest.getInstance("SHA-256"); } catch(final NoSuchAlgorithmException e) {}
        try { if(md == null) md = MessageDigest.getInstance("SHA-224"); } catch(final NoSuchAlgorithmException e) {}
        try { if(md == null) md = MessageDigest.getInstance("SHA-1"  ); } catch(final NoSuchAlgorithmException e) {}
        try { if(md == null) md = MessageDigest.getInstance("MD5"    ); } catch(final NoSuchAlgorithmException e) {}
        try { if(md == null) md = MessageDigest.getInstance("MD2"    ); } catch(final NoSuchAlgorithmException e) {}
              if(md == null) throw XCom.newNoSuchAlgorithmException("none of the message digest algorithms (SHA2-512/384/256/224, SHA1, MD5, and MD2) are available for use");

        return bytesToHexString( md.digest( str.getBytes() ) );
    }

    public static String computeFileHash(final String filePath, final String algorithm) throws IOException, NoSuchAlgorithmException
    {
        final MessageDigest     md  = MessageDigest.getInstance(algorithm);
        final FileInputStream   fis = new FileInputStream(filePath);
        final DigestInputStream dis = new DigestInputStream(fis, md);

        final byte[] bytes = new byte[8192];
        while( dis.read(bytes) > 0 );

        return bytesToHexString( md.digest() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    // ##### ??? TODO : Remove this later ??? #####
    // The regular expression is developed based on the information from:
    //     https://learn.microsoft.com/en-us/dotnet/standard/io/file-path-formats
    private static Pattern _pmWinPath = Pattern.compile("(?:(?:\\\\\\\\(?:[.?]\\\\)?)?(?:[a-zA-Z]:\\\\?)?)(?:[^<>:\"/\\\\|?*\\u0000-\\u001F]+\\\\?)*");
    */

    public static String normalizeDirectorySeparators(final String path)
    {
        // Prepare the string builder
        final StringBuilder sb = new StringBuilder();

        // Get the length of the path string
        final int pathLen = path.length();

        // JxMake uses POSIX directory separators and the token reader allows escaping naked space characters
        for(int i = 0; i < pathLen; ++i) {
            final char ch = path.charAt(i);
            switch(ch) {
                case '\\':
                    final char nch = ( (i + 1) < pathLen ) ? path.charAt(i + 1) : 0;
                    if(nch == ' ') {
                        // Currently, '\ ' is the only valid escape sequence
                        sb.append(' ');
                        ++i;
                    }
                    else {
                        // Assume any other occurrence of '\' as a directory separator
                        sb.append(_InternalDirSep);
                    }
                    break;
                default:
                    sb.append(ch);
                    break;
            } // switch
        } // for

        // Return the normalized path string
        return sb.toString();

        /*
        // Prepare the string builder
        final StringBuilder sb = new StringBuilder();

        // Get the length of the path string
        final int pathLen = path.length();

        // The operating system uses '\' as the directory separator (e.g. Windows)
        if( File.separatorChar == '\\' ) {
            for(int i = 0; i < pathLen; ++i) {
                final char ch = path.charAt(i);
                switch(ch) {
                    case '\\':
                        // Convert the directory separator
                        sb.append(_InternalDirSep);
                        break;
                    case '`': /* FALLTHROUGH * /
                    case '^':
                        final char nch = ( (i + 1) < pathLen ) ? path.charAt(i + 1) : 0;
                        if(nch != ' ') forceStackDumpAndExit("invalid escape sequence '%c%c' in path \"%s\"", ch, nch, path);
                        sb.append(' ');
                        ++i;
                        break;
                    default:
                        sb.append(ch);
                        break;
                } // switch
            } // for
        }

        // The operating system uses '/' as the directory separator (e.g. POSIX)
        else {
            for(int i = 0; i < pathLen; ++i) {
                final char ch = path.charAt(i);
                switch(ch) {
                    case '\\':
                        final char nch = ( (i + 1) < pathLen ) ? path.charAt(i + 1) : 0;
                        if(nch == ' ') {
                            // Assume '\ ' as the only valid escape sequence
                            sb.append(' ');
                            ++i;
                        }
                        else {
                            // Assume any other occurrences of '\' as the directory separators
                            sb.append(_InternalDirSep);
                        }
                        break;
                    default:
                        sb.append(ch);
                        break;
                } // switch
            } // for
        }
        */
    }

    public static String toNativeDirectorySeparators(final String path)
    {
        // Assume the path already has normalized directory separators
        if(File.separatorChar == _InternalDirSep) return path;

        // Convert the separators
        return path.replace(_InternalDirSep, File.separatorChar);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean pathIsAbsolute(final String path)
    { return Paths.get(path).isAbsolute(); }

    public static boolean pathIsRelative(final String path)
    { return !Paths.get(path).isAbsolute(); }

    /*
     * NOTE : # On Windows, if this function (and some others) reports that a file does not exist, please
     *          double-check if the correct file extension has been supplied.
     *        # This is especially important when checking executable files because POSIX uses permissions
     *          whereas Windows uses extensions (e.g. 'avrdude' vs 'avrdude.exe').
     */
    public static boolean pathIsValid(final String path)
    { return Files.exists( Paths.get(path), LinkOption.NOFOLLOW_LINKS ); }

    public static boolean pathIsSame(final String path1, final String path2) throws IOException
    { return Files.isSameFile( Paths.get(path1), Paths.get(path2) ); }

    public static boolean pathIsFile(final String path)
    { return !Files.isDirectory( Paths.get(path) ); }

    public static boolean pathIsDirectory(final String path)
    { return Files.isDirectory( Paths.get(path) ); }

    public static boolean pathIsValidFile(final String path)
    {
        final Path p = Paths.get(path);
        return Files.exists(p) && !Files.isDirectory(p);
    }

    public static boolean pathIsValidDirectory(final String path)
    {
        final Path p = Paths.get(path);
        return Files.exists(p) && Files.isDirectory(p);
    }

    public static boolean pathIsReadableFile(final String path)
    {
        final Path p = Paths.get(path);
        return Files.exists(p) && !Files.isDirectory(p) && Files.isReadable(p);
    }

    public static boolean pathIsWritableFile(final String path)
    {
        final Path p = Paths.get(path);
        return Files.exists(p) && !Files.isDirectory(p) && Files.isWritable(p);
    }

    public static boolean pathIsExecutableFile(final String path)
    {
        final Path p = Paths.get(path);
        return Files.exists(p) && !Files.isDirectory(p) && Files.isExecutable(p);
    }

    public static long pathGetFileSize(final String path) throws IOException
    {
        final Path p = Paths.get(path);
        return Files.size(p);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean pathIsSymLink(final String path)
    { return Files.isSymbolicLink( Paths.get(path) ); }

    public static boolean pathIsReadable(final String path)
    { return Files.isReadable( Paths.get(path) ); }

    public static boolean pathIsWritable(final String path)
    { return Files.isWritable( Paths.get(path) ); }

    public static boolean pathIsExecutable(final String path)
    { return Files.isExecutable( Paths.get(path) ); }

    public static boolean pathIsValidSymLink(final String path_)
    {
        final Path path = Paths.get(path_);

        try {
            return Files.isSymbolicLink(path) && Files.exists( path.toRealPath(), LinkOption.NOFOLLOW_LINKS );
        }
        catch(final IOException e) {
            return false;
        }
    }

    public static String pathGetSymLinkTarget(final String path) throws IOException
    { return Files.readSymbolicLink( Paths.get(path) ).toString(); }

    public static String pathGetSymLinkRealPath_ignoreNonExistentTarget(final String path_) throws IOException
    {
        final Path path = Paths.get(path_);

        Files.readSymbolicLink(path); // Ensure the symbolic link itself actually exists

        try {
            return path.toRealPath().toString();
        }
        catch(final IOException e) {
            return "";
        }
    }

    public static String pathGetRealPath(final String path) throws IOException
    {
        final boolean isRelPath = SysUtil.pathIsRelative(path);
        final String  realPath  = Paths.get( resolveAbsolutePath(path) ).toRealPath().toString();

        return isRelPath ? resolveRelativePath(realPath) : realPath;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean pathIsEmptyDirectory(final String path)
    {
        final Path p = Paths.get(path);
        if( !Files.isDirectory(p) ) return false;

        try(
            final DirectoryStream<Path> ds = Files.newDirectoryStream(p);
        ) {
            // There is no need to call 'ds.close()' because it is a auto-closeable resource
            final boolean empty = !ds.iterator().hasNext();
            return empty;
        }
        catch(final Exception e) {}

        return false;
    }

    public static FileTime pathGetTime(final String path) throws IOException
    { return Files.getLastModifiedTime( Paths.get(path) ); }

    public static boolean isPathMoreRecent(final String pathChk, final String pathRef)
    {
        final boolean chkValid = pathIsValid(pathChk);
        final boolean refValid = pathIsValid(pathRef);

        if( !chkValid && !refValid ) return false;
        if(  chkValid && !refValid ) return true;
        if( !chkValid &&  refValid ) return false;

        try {
            final FileTime chkFileTime = Files.getLastModifiedTime( Paths.get(pathChk) );
            final FileTime refFileTime = Files.getLastModifiedTime( Paths.get(pathRef) );
            return chkFileTime.compareTo(refFileTime) > 0;
        }
        catch(final Exception e) {}

        return false;
    }

    public static boolean isPathMoreRecent(final String pathChk, final Iterable<String> pathRefs)
    {
        try {

            final boolean  chkValid    = pathIsValid(pathChk);
                  FileTime chkFileTime = null;
                  boolean  chkRecent   = true;

            for(final String pathRef : pathRefs) {

                final boolean  refValid    = pathIsValid(pathRef);
                      FileTime refFileTime = null;

                if( !chkValid && !refValid ) { chkRecent = false; break; }
                if( !chkValid &&  refValid ) { chkRecent = false; break; }
                if(  chkValid &&  refValid ) {
                    if(chkFileTime == null) chkFileTime = Files.getLastModifiedTime( Paths.get(pathChk) );
                                            refFileTime = Files.getLastModifiedTime( Paths.get(pathRef) );
                    if( chkFileTime.compareTo(refFileTime) <= 0 ) { chkRecent = false; break; }
                }

            } // for

            return chkRecent;

        }
        catch(final Exception e) {}

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmWinDrive = Pattern.compile("^(?:[\\\\/]{2}(?:[.?][\\\\/])?)?[a-zA-Z]:[\\\\/]?");
    private static       int     _invUMask   = -1;

    private static int _getInvUMask()
    {
        // Determine the inverse file mode creation mask
        if(_invUMask < 0) {
            // Get the file mode creation mask
            String umask = execlp("umask");
                 if( umask.isEmpty()     ) umask = "0002";
            else if( umask.length () > 4 ) umask = umask.substring(0, 4);
            // Calculate its inverse
            _invUMask = 0777 - Integer.parseInt(umask, 8);
        }

        // Return the inverse file mode creation mask
        return _invUMask;
    }

    private static final Set<PosixFilePermission> _pfpStd = EnumSet.noneOf(PosixFilePermission.class);
    private static final Set<PosixFilePermission> _pfpExe = EnumSet.noneOf(PosixFilePermission.class);

    static {
        final int ium = _getInvUMask();
        if( (ium & 0100) != 0 ) {                                                _pfpExe.add(PosixFilePermission.OWNER_EXECUTE ); }
        if( (ium & 0200) != 0 ) { _pfpStd.add(PosixFilePermission.OWNER_WRITE ); _pfpExe.add(PosixFilePermission.OWNER_WRITE   ); }
        if( (ium & 0400) != 0 ) { _pfpStd.add(PosixFilePermission.OWNER_READ  ); _pfpExe.add(PosixFilePermission.OWNER_READ    ); }
        if( (ium & 0010) != 0 ) {                                                _pfpExe.add(PosixFilePermission.GROUP_EXECUTE ); }
        if( (ium & 0020) != 0 ) { _pfpStd.add(PosixFilePermission.GROUP_WRITE ); _pfpExe.add(PosixFilePermission.GROUP_WRITE   ); }
        if( (ium & 0040) != 0 ) { _pfpStd.add(PosixFilePermission.GROUP_READ  ); _pfpExe.add(PosixFilePermission.GROUP_READ    ); }
        if( (ium & 0001) != 0 ) {                                                _pfpExe.add(PosixFilePermission.OTHERS_EXECUTE); }
        if( (ium & 0002) != 0 ) { _pfpStd.add(PosixFilePermission.OTHERS_WRITE); _pfpExe.add(PosixFilePermission.OTHERS_WRITE  ); }
        if( (ium & 0003) != 0 ) { _pfpStd.add(PosixFilePermission.OTHERS_READ ); _pfpExe.add(PosixFilePermission.OTHERS_READ   ); }
    }

    private static final String[] DANGEROUS_PATH_PREFIXES = {
        // Windows system roots
        "C:\\Windows",
        "C:\\Program Files",
        "C:\\ProgramData",
        "C:\\Users\\Default",

        // Unix system roots
        "/bin",
        "/sbin",
        "/etc",
        "/usr",
        "/var",
        "/lib",
        "/root",

        // Unix device nodes
        "/dev"
    };

    public static boolean _isDangerousPath(final String path)
    {
        if(path == null) return true;

        final String tpath = path.trim();

        if( tpath.isEmpty(      ) ) return true;

        if( tpath.equals ("."   ) ) return true;
        if( tpath.equals (".."  ) ) return true;

        if( tpath.equals ("\\"  ) ) return true;
        if( tpath.equals ("\\." ) ) return true;
        if( tpath.equals ("\\..") ) return true;

        if( tpath.equals ("/"   ) ) return true;
        if( tpath.equals ("/."  ) ) return true;
        if( tpath.equals ("/.." ) ) return true;

        try {
            final String absPath = resolveAbsolutePath(path, true);

            if( absPath.equalsIgnoreCase( getCWD() ) ) return true;
            if( absPath.equalsIgnoreCase( getUHD() ) ) return true;
            if( absPath.equalsIgnoreCase( getJDD() ) ) return true;
            if( absPath.equalsIgnoreCase( getJTD() ) ) return true;
            if( absPath.equalsIgnoreCase( getJXD() ) ) return true;
            if( absPath.equalsIgnoreCase( getUDD() ) ) return true;
            if( absPath.equalsIgnoreCase( getADD() ) ) return true;

            for(final String prefix : DANGEROUS_PATH_PREFIXES) {
                if( absPath.equalsIgnoreCase(prefix) || absPath.toLowerCase().startsWith( prefix.toLowerCase() + _InternalDirSep ) ) {
                    return true;
                }
            }

            return _pmWinDrive.matcher(absPath).matches();
        }
        catch(final IOException e) {
            return true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ContentInfoUtil _defaultContentInfoUtil = null;

    private static ContentInfoUtil _getDefaultContentInfoUtil() throws IOException
    {
        if(_defaultContentInfoUtil == null) _defaultContentInfoUtil = new ContentInfoUtil("/com/j256/simplemagic/resources/magic", null);
        return _defaultContentInfoUtil;
    }

    public synchronized static String cu_file_mimetype(final String path) throws IOException
    {
        try {
            final String res = _getDefaultContentInfoUtil().findMatch(path).getMimeType();
            if(res != null) return res;
        }
        catch(final NullPointerException e) {
            if( !SysUtil.pathIsValidFile(path) ) throw e;
        }

        return "";
    }

    public synchronized static String cu_file_message(final String path) throws IOException
    {
        try {
            final String res = _getDefaultContentInfoUtil().findMatch(path).getMessage();
            if(res != null) return res;
        }
        catch(final NullPointerException e) {
            if( !SysUtil.pathIsValidFile(path) ) throw e;
        }

        return "";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final HashMap<Path, String> _fsTypeCache = new HashMap<>();

    private static String _getFSType(final Path p)
    {
        final Path root = p.getRoot();

        return _fsTypeCache.computeIfAbsent( root, r -> {
            try {
                return Files.getFileStore(r).type().toLowerCase();
            }
            catch(final IOException ignored) {
                return null;
            }
        } );
    }

    public static void cu_touch(final String path) throws IOException
    {
        final Path p = Paths.get(path);

        if( !Files.exists(p) ) {
            Files.createFile(p);
            return;
        }

        if( !osIsWindows() ) {
            Files.setLastModifiedTime( p, FileTime.from( Instant.now() ) );
            return;
        }

        final String fsType  = _getFSType(p);
        final int    resTime = ( fsType != null && fsType.contains("ntfs") ) ? 10 : 2000;

        final long   incTime = Files.getLastModifiedTime(p).toMillis() + resTime;
        final long   nowTime = Instant.now().toEpochMilli();

        Files.setLastModifiedTime( p, FileTime.fromMillis( Math.max(incTime, nowTime) ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void cu_ln_h(final String pathHardLink, final String pathTarget, final boolean replaceExisting) throws IOException
    {
        // Check the paths
        if( _isDangerousPath(pathHardLink) || _isDangerousPath(pathTarget) ) return;

        // Remove the destination path as needed
        if( replaceExisting && pathIsValid(pathHardLink) ) cu_rmfile(pathHardLink);

        // Create hard link
        Files.createLink( Paths.get(pathHardLink), Paths.get(pathTarget) );
    }

    public static void cu_ln_s(final String pathSymLink, final String pathTarget, final boolean replaceExisting) throws IOException
    {
        // Check the paths
        if( _isDangerousPath(pathSymLink) || _isDangerousPath(pathTarget) ) return;

        // Remove the destination path as needed
        if( replaceExisting && pathIsValid(pathSymLink) ) cu_rmfile(pathSymLink);

        // Create symbolic link
        Files.createSymbolicLink( Paths.get(pathSymLink), Paths.get(pathTarget) );
    }

    public static void cu_rmfile(final String path) throws IOException
    {
        if( _isDangerousPath(path) || !pathIsValidFile(path) ) return;

        Files.delete( Paths.get(path) );
    }

    public static void cu_rmfile_strict(final String path) throws IOException
    {
        if( _isDangerousPath(path) ) return;

        if( !pathIsValid    (path) ) throw new NoSuchFileException(path);
        if(  pathIsDirectory(path) ) throw new NotFileException   (path);

        Files.delete( Paths.get(path) );
    }

    public static void cu_cpfile(final String pathSrc, final String pathDst, final boolean replaceExisting, final boolean normalizePFP) throws IOException
    {
        // Check the paths
        if( _isDangerousPath(pathSrc) || !pathIsValidFile     (pathSrc) ) return;
        if( _isDangerousPath(pathDst) ||  pathIsValidDirectory(pathDst) ) return;

        // Check if the source path is a symbolic link
        if( pathIsSymLink(pathSrc) ) {
            cu_ln_s( pathDst, pathGetSymLinkTarget(pathSrc), replaceExisting );
            return;
        }

        // Prepare the copy options
        final CopyOption[] options = new CopyOption[replaceExisting ? 2 : 1];

                            options[0] = StandardCopyOption.COPY_ATTRIBUTES;
        if(replaceExisting) options[1] = StandardCopyOption.REPLACE_EXISTING;

        // Copy the file
        Files.copy( Paths.get(pathSrc), Paths.get(pathDst), options );

        // Normalize the POSIX file permissions
        if( normalizePFP && SysUtil.osIsPOSIX() ) {
            final Path                     path = Paths.get(pathDst);
            final Set<PosixFilePermission> pfp  = Files.getPosixFilePermissions(path);
            final boolean                  exe  = pfp.contains(PosixFilePermission.OWNER_EXECUTE) || pfp.contains(PosixFilePermission.GROUP_EXECUTE) || pfp.contains(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(path, exe ? _pfpExe : _pfpStd);
        }
    }

    public static void cu_mvfile(final String pathSrc, final String pathDst, final boolean replaceExisting) throws IOException
    {
        // Check the paths
        if( _isDangerousPath(pathSrc) || !pathIsValidFile     (pathSrc) ) return;
        if( _isDangerousPath(pathDst) ||  pathIsValidDirectory(pathDst) ) return;

        // Move the file
        if(replaceExisting) Files.move( Paths.get(pathSrc), Paths.get(pathDst), StandardCopyOption.REPLACE_EXISTING );
        else                Files.move( Paths.get(pathSrc), Paths.get(pathDst)                                      );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String _getExt(final String fileName)
    {
        final int idx = fileName.lastIndexOf('.');

        return (idx > 0) ? fileName.substring(idx + 1) : "";
    }

    public static List<String> cu_lsfile(final String path, final Pattern pmMatchExt) throws IOException
    {
        // ===== List files non-recursively =====

        try( final Stream<Path> stream = Files.list( Paths.get(path) ) ) {
            return stream.filter ( f -> !Files.isDirectory(f)                                                    )
                         .map    ( Path::getFileName                                                             )
                         .map    ( Path::toString                                                                )
                         .filter ( s -> (pmMatchExt == null) ? true : pmMatchExt.matcher( _getExt(s) ).matches() )
                         .map    ( s -> normalizeDirectorySeparators(s)                                          )
                         .collect( Collectors.toList()                                                           );
        }
    }

    public static List<String> cu_lsfile(final String path) throws IOException
    { return cu_lsfile(path, null); }

    public static List<String> cu_lsfile_rec(final String path, final Pattern pmMatchExt) throws IOException
    {
        // ===== List files recursively =====

        try( final Stream<Path> stream = Files.walk( Paths.get(path) ) ) {
            return stream.filter ( f -> !Files.isDirectory(f)                                                    )
                         .map    ( Path::toString                                                                )
                         .filter ( s -> (pmMatchExt == null) ? true : pmMatchExt.matcher( _getExt(s) ).matches() )
                         .map    ( s -> normalizeDirectorySeparators(s)                                          )
                         .sorted (                                                                               )
                         .collect( Collectors.toList()                                                           );
        }
    }

    public static List<String> cu_lsfile_rec(final String path) throws IOException
    { return cu_lsfile_rec(path, null); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static char LSFD_Type_All       = 'a';
    public static char LSFD_Type_File      = 'f';
    public static char LSFD_Type_Directory = 'd';

    private static List<String> _cu_lsfd_impl(final String path, final char type, final int depthMax) throws IOException
    {
        // ===== List files and directories non-recursively or recursively =====

        // This function should list the subdirectories before the files inside the subdirectories

        // Prepare the result list
        final ArrayList<String> resList = new ArrayList<>();

        // Get the absolute path
        final Path absPath = Paths.get( resolveAbsolutePath(path, true) );

        // Depth counter
        final XCom.IntegerRef depthCnt = new XCom.IntegerRef();

        // Walk the tree
        Files.walkFileTree(
            absPath,
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path src, final BasicFileAttributes attrs) throws IOException
                {
                    // Store the directory name as needed if it is not the original path
                    final boolean isOrgDir = ( src.compareTo(absPath) == 0 );
                    if(!isOrgDir) {
                        if(type == LSFD_Type_All || type == LSFD_Type_Directory) resList.add( normalizeDirectorySeparators( src.toString() ) );
                    }
                    // Determine if the directory should be visited or not
                    if( Files.isSymbolicLink(src) ) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    else {
                        if( depthCnt.get() >= depthMax ) return FileVisitResult.SKIP_SUBTREE;
                        depthCnt.inc();
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path src, final IOException e) throws IOException
                {
                    depthCnt.dec();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path src, final BasicFileAttributes attrs) throws IOException
                {
                    // Store the file name as needed
                    if(type == LSFD_Type_All || type == LSFD_Type_File) resList.add( normalizeDirectorySeparators( src.toString() ) );
                    // Continue visiting
                    return FileVisitResult.CONTINUE;
                }
            }
        );

        // Return the result
        return resList;
    }

    public static List<String> cu_lsfd(final String path, final char type) throws IOException
    { return _cu_lsfd_impl(path, type, 1); }

    public static List<String> cu_lsfd(final String path) throws IOException
    { return _cu_lsfd_impl(path, LSFD_Type_All, 1); }

    public static List<String> cu_lsfd_rec(final String path, final char type, final int depthMax) throws IOException
    { return _cu_lsfd_impl( path, type, (depthMax <= 0) ? Integer.MAX_VALUE : depthMax ); }

    public static List<String> cu_lsfd_rec(final String path, final int depthMax) throws IOException
    { return cu_lsfd_rec(path, LSFD_Type_All, depthMax); }

    public static List<String> cu_lsfd_rec(final String path, final char type) throws IOException
    { return cu_lsfd_rec(path, type, -1); }

    public static List<String> cu_lsfd_rec(final String path) throws IOException
    { return cu_lsfd_rec(path, LSFD_Type_All, -1); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ArrayList<String> _cu_find_file_impl(final String path, final Pattern searchRegExp, final int depthMax) throws IOException
    {
        // ===== Find files non-recursively or recursively =====

        // Prepare the result list
        final ArrayList<String> resList = new ArrayList<>();

        // Get the absolute path
        final Path absPath = Paths.get( resolveAbsolutePath(path, true) );

        // Depth counter
        final XCom.IntegerRef depthCnt = new XCom.IntegerRef();

        // Walk the tree
        Files.walkFileTree(
            absPath,
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path src, final BasicFileAttributes attrs) throws IOException
                {
                    // Determine if the directory should be visited or not
                    if( Files.isSymbolicLink(src) ) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    else {
                        if( depthCnt.get() >= depthMax ) return FileVisitResult.SKIP_SUBTREE;
                        depthCnt.inc();
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path src, final IOException e) throws IOException
                {
                    depthCnt.dec();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path src, final BasicFileAttributes attrs) throws IOException
                {
                    // Store the file name if it matches
                    if( searchRegExp.matcher( absPath.relativize(src).toString() ).matches() ) resList.add( normalizeDirectorySeparators( src.toString() ) );
                    // Continue visiting
                    return FileVisitResult.CONTINUE;
                }
            }
        );

        // Return the result
        return resList;
    }

    public static ArrayList<String> cu_find_file(final String path, final Pattern searchRegExp) throws IOException
    { return _cu_find_file_impl(path, searchRegExp, 1); }

    public static ArrayList<String> cu_find_file_recursive(final String path, final Pattern searchRegExp, final int depthMax) throws IOException
    { return _cu_find_file_impl( path, searchRegExp, (depthMax <= 0) ? Integer.MAX_VALUE : depthMax ); }

    public static ArrayList<String> cu_find_file_recursive(final String path, final Pattern searchRegExp) throws IOException
    { return cu_find_file_recursive(path, searchRegExp, -1); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void cu_mkdir(final String path) throws IOException
    { Files.createDirectories( Paths.get(path) ); }

    public static void cu_rmdir(final String path) throws IOException
    {
        if( _isDangerousPath(path) || !pathIsValidDirectory(path) ) return;

        Files.delete( Paths.get(path) );
    }

    private static boolean _cu_rmdir_empty_recursive_impl(Path dir) throws IOException
    {
        try(final DirectoryStream<Path> stream = Files.newDirectoryStream(dir) ) {

            for(final Path entry : stream) {

                if( Files.isDirectory(entry) ) {
                    // Recurse into subdirectory
                    if( !_cu_rmdir_empty_recursive_impl(entry) ) {
                        // Found non-empty content
                        return false;
                    }
                }
                else {
                    // Found something that is not a directory -> fail
                    throw new DirectoryNotEmptyException( dir.toString() );
                }

            } // for

        } // try

        // If we reach here, all children were empty directories and deleted
        Files.delete(dir);

        return true;
    }

    public static void cu_rmdir_empty_recursive_strict(final String path) throws IOException
    {
        if( _isDangerousPath(path) ) return;

        if( !pathIsValid(path) ) throw new NoSuchDirectoryException(path);
        if(  pathIsFile (path) ) throw new NotDirectoryException   (path);

        _cu_rmdir_empty_recursive_impl( Paths.get(path) );
    }

    public static void cu_rmdir_recursive(final String path) throws IOException
    {
        if( _isDangerousPath(path) || !pathIsValidDirectory(path) ) return;

        try(
            final Stream<Path> ds = Files.walk( Paths.get(path) )
        ) {
            ds.map    ( Path::toFile              )
              .sorted ( Comparator.reverseOrder() )
              .forEach( File::delete              );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ArrayList<String> _cu_find_dir_impl(final String path, final Pattern searchRegExp, final int depthMax) throws IOException
    {
        // ===== Find directories non-recursively or recursively =====

        // Prepare the result list
        final ArrayList<String> resList = new ArrayList<>();

        // Get the absolute path
        final Path absPath = Paths.get( resolveAbsolutePath(path, true) );

        // Depth counter
        final XCom.IntegerRef depthCnt = new XCom.IntegerRef();

        // Walk the tree
        Files.walkFileTree(
            absPath,
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path src, final BasicFileAttributes attrs) throws IOException
                {
                    // Store if match
                    if( searchRegExp.matcher( absPath.relativize(src).toString() ).matches() ) resList.add( normalizeDirectorySeparators( src.toString() ) );
                    // Determine if the directory should be visited or not
                    if( Files.isSymbolicLink(src) ) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    else {
                        if( depthCnt.get() >= depthMax ) return FileVisitResult.SKIP_SUBTREE;
                        depthCnt.inc();
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path src, final IOException e) throws IOException
                {
                    depthCnt.dec();
                    return FileVisitResult.CONTINUE;
                }
            }
        );

        // Return the result
        return resList;
    }

    public static ArrayList<String> cu_find_dir(final String path, final Pattern searchRegExp) throws IOException
    { return _cu_find_dir_impl(path, searchRegExp, 1); }

    public static ArrayList<String> cu_find_dir_recursive(final String path, final Pattern searchRegExp, final int depthMax) throws IOException
    { return _cu_find_dir_impl( path, searchRegExp, (depthMax <= 0) ? Integer.MAX_VALUE : depthMax ); }

    public static ArrayList<String> cu_find_dir_recursive(final String path, final Pattern searchRegExp) throws IOException
    { return cu_find_dir_recursive(path, searchRegExp, -1); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * ### WARNING ###
     *     Please be very careful when copying directories of a POSIX system which are mapped to a Windows virtual machine
     *     from within the Windows virtual machine because symbolic links will be treated as regular files and directories!
     * ### WARNING ###
     */
    public static void cu_cpdir_rec(final String srcPath, final String dstPath, final boolean replaceExisting) throws IOException
    {
        /*
         * The code in this function is developed based on the information from:
         *     https://stackoverflow.com/a/60621544
         */

        // Get the absolute paths
        final Path srcAbsPath = Paths.get( resolveAbsolutePath(srcPath, true) );
        final Path dstAbsPath = Paths.get( resolveAbsolutePath(dstPath, true) );

        // Prepare the copy options
        final CopyOption[] options = new CopyOption[replaceExisting ? 2 : 1];

                            options[0] = StandardCopyOption.COPY_ATTRIBUTES;
        if(replaceExisting) options[1] = StandardCopyOption.REPLACE_EXISTING;

        // Copy the directory recursively
        Files.walkFileTree(

            srcAbsPath,
            EnumSet.of(FileVisitOption.FOLLOW_LINKS),
            Integer.MAX_VALUE,

            new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(final Path src, final BasicFileAttributes attrs) throws IOException
                {
                    // Get the new path
                    final Path newPath = dstAbsPath.resolve( srcAbsPath.relativize(src).toString() );
                    // Check if it is a symbolic link
                    if( Files.isSymbolicLink(src) ) {
                        // Delete the symbolic link if it exists and the 'replaceExisting' parameter is set
                        if( Files.exists(newPath) ) {
                            if(!replaceExisting) return FileVisitResult.SKIP_SUBTREE;
                            Files.delete(newPath);
                        }
                        // Create the symbolic link
                        Files.createSymbolicLink( newPath, src.toRealPath() );
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    // It is not a symbolic link
                    else {
                        Files.createDirectories(newPath);
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult visitFile(final Path src, final BasicFileAttributes attrs) throws IOException
                {
                    // Get the new path
                    final Path newPath = dstAbsPath.resolve( srcAbsPath.relativize(src).toString() );
                    // Check if it is a symbolic link
                    if( Files.isSymbolicLink(src) ) {
                        // Delete the symbolic link if it exists and the 'replaceExisting' parameter is set
                        if( Files.exists(newPath) ) {
                            if(!replaceExisting) return FileVisitResult.SKIP_SUBTREE;
                            Files.delete(newPath);
                        }
                        // Create the symbolic link
                        Files.createSymbolicLink( newPath, src.toRealPath() );
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    // It is not a symbolic link
                    else {
                        Files.copy(src, newPath, options);
                        return FileVisitResult.CONTINUE;
                    }
                }

            } // SimpleFileVisitor

        ); // Files.walkFileTree
    }

    public static void cu_set_rwx3(final String path, final String rwx3) throws IOException
    {
        if( !SysUtil.osIsPOSIX() ) return;

        Files.setPosixFilePermissions( Paths.get(path), PosixFilePermissions.fromString(rwx3) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getCWD()
    {
        if(_curWorkDir == null) {
            _curWorkDir = normalizeDirectorySeparators( System.getProperty("user.dir") );
        }

        return _curWorkDir;
    }

    public static void setCWD(final String path) throws IOException
    {
        final String newCWD = resolveAbsolutePath(path, true);

        if( !pathIsValidDirectory(newCWD) ) {
            throw XCom.newIOException("The path '%s' cannot be used as a new working directory!", path);
        }

        /*
        // Must not set this property
        System.setProperty("user.dir", newCWD);
        */

        _curWorkDir = newCWD;
    }

    public static void pushCWD()
    { _cwdStack.push( getCWD() ); }

    public static boolean popCWD() throws IOException
    {
        if( _cwdStack.empty() ) return false;

        setCWD( _cwdStack.pop() );

        return true;
    }

    public static void pushSetCWD(final String path) throws IOException
    {
        _cwdStack.push( getCWD() );

        try {
            setCWD(path);
        }
        catch(final IOException e) {
            // Pop the stack
            _cwdStack.pop();
            // Re-throw the exception
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getUHD()
    {
        if(_userHomeDir == null) {
            _userHomeDir = normalizeDirectorySeparators( System.getProperty("user.home") );
        }

        return _userHomeDir;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String __getJXD_nocache(final String dirName)
    {
        try {
            // Resolve the directory path
            final String dirPath = resolvePath( _JxMakeDataRoot + _InternalDirSep + dirName, getUHD() );
            // Make the directory path as needed
            if( !pathIsValidDirectory(dirPath) ) cu_mkdir(dirPath);
            // Return the directory path
            return dirPath;
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        // The directory path is not available
        return null;
    }

    public static String getJDD()
    {
        if(_jxmakeUDataDir == null) _jxmakeUDataDir = __getJXD_nocache("data");
        return _jxmakeUDataDir;
    }

    public static String getJTD()
    {
        if(_jxmakeUToolsDir == null) _jxmakeUToolsDir = __getJXD_nocache("tools");
        return _jxmakeUToolsDir;
    }

    public static String getUDD() throws IOException
    {
        if(_jxmakeUDotDir == null) _jxmakeUDotDir = getUHD() + _InternalDirSep + _JxMakeUserDotDirRoot;
        if( !pathIsValidDirectory(_jxmakeUDotDir) ) cu_mkdir(_jxmakeUDotDir);
        return _jxmakeUDotDir;
    }

    public static String getADD() throws IOException
    {
        if(_jxmakeADatDir == null) _jxmakeADatDir = getUHD() + _InternalDirSep + _JxMakeAppDataDirRoot;
        if( !pathIsValidDirectory(_jxmakeADatDir) ) cu_mkdir(_jxmakeADatDir);
        return _jxmakeADatDir;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getJXD()
    {
        if(_jxmakeExeDir == null) {
            try {
                if( isRunFromClassFile() ) _jxmakeExeDir = normalizeDirectorySeparators( getJxMakeClassDir().getParent().toString() ); // Run from the main class file
                else                       _jxmakeExeDir = getDirName( getJxMakeJarFile().getPath() );                                 // Run from a JAR file
            }
            catch(final Exception e) {}
        }

        return _jxmakeExeDir;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _resolveAbsolutePath_rp(final String path)
    {
        if( pathIsAbsolute(path) ) return                              normalizeDirectorySeparators(path);
        else                       return getCWD() + _InternalDirSep + normalizeDirectorySeparators(path);
    }

    private static String _resolveAbsolutePath_nl(final String path)
    {
        final String resPath = _resolveAbsolutePath_rp(path);

        return normalizeDirectorySeparators( Paths.get(resPath).normalize().toString() );
    }

    private static String _resolveAbsolutePath_fl(final String path) throws IOException
    {
        final Path resPath = Paths.get( _resolveAbsolutePath_rp(path) );

        if( !Files.exists(resPath, LinkOption.NOFOLLOW_LINKS) ) return resPath.normalize().toString();

        return normalizeDirectorySeparators( resPath.toRealPath().normalize().toString() );
    }

    public static String resolveAbsolutePath(final String path, boolean followLinks) throws IOException
    { return followLinks ? _resolveAbsolutePath_fl(path) : _resolveAbsolutePath_nl(path); }

    public static String resolveAbsolutePath(final String path)
    { return _resolveAbsolutePath_nl(path); }

    public static String resolveRelativePath(final String path)
    {
        final Path source = Paths.get( getCWD()                           );
        final Path target = Paths.get( normalizeDirectorySeparators(path) );

        String resPath;

        try {
            resPath = source.relativize(target).toString();
        }
        catch(final IllegalArgumentException e) {
            resPath = path;
        }

        return normalizeDirectorySeparators(resPath);
    }

    public static String resolvePath(final String path, final String pathRef)
    {
        if( pathIsAbsolute(path) || pathRef == null || pathRef.isEmpty() ) return normalizeDirectorySeparators(path);

        return normalizeDirectorySeparators(
            Paths.get(
                  normalizeDirectorySeparators(pathRef)
                + _InternalDirSep
                + normalizeDirectorySeparators(path   )
            ).normalize().toString()
        );
    }

    public static String removeLastPathPart(final String path)
    {
        final String str = normalizeDirectorySeparators(path);

        return str.substring( 0, str.lastIndexOf(_InternalDirSep) );
    }

    public static String extractLastPathPart(final String path)
    {
        final String str = normalizeDirectorySeparators(path);

        return str.substring( str.lastIndexOf(_InternalDirSep) + 1, str.length() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String ellipsisPath(final String path_)
    {
              String  path = normalizeDirectorySeparators(path_);
        final boolean sw   = path.startsWith(_InternalDirSepStr);
        final boolean ew   = path.endsWith  (_InternalDirSepStr);

        if(sw) path = path.substring( 1                    );
        if(ew) path = path.substring( 0, path.length() - 1 );

        final String[] parts = path.split(_InternalDirSepStr);
        if(parts.length <= 2) return (sw ? _InternalDirSepStr : "") + path;

        final String first = parts[0];
        final String last  = parts[parts.length - 1];

        return (sw ? _InternalDirSepStr : "") + first + _InternalDirSepStr + '…' + _InternalDirSep + last;
    }

    public static String getDirName(final String path)
    {
        if( pathIsDirectory(path) ) return normalizeDirectorySeparators(path);

        final Path parent = Paths.get(path).getParent();
        if(parent == null) return "";

        return normalizeDirectorySeparators( parent.toString() );
    }

    public static String getFinalDirName(final String path)
    {
        if(! pathIsDirectory(path) ) return "";

        final Path lastDirName = Paths.get( normalizeDirectorySeparators(path) ).getFileName();
        if(lastDirName == null) return "";

        return lastDirName.toString();
    }

    public static String getFileName(final String path)
    {
        if( !pathIsFile(path) ) return "";

        final Path fileName = Paths.get( normalizeDirectorySeparators(path) ).getFileName();
        if(fileName == null) return "";

        return fileName.toString();
    }

    public static String getFileNameWithoutExtension(final String path)
    {
        final String fileName = getFileName(path);
        final int    idx      = fileName.lastIndexOf('.');

        return (idx > 0) ? fileName.substring(0, idx) : fileName;
    }

    public static String getFileExtension(final String path)
    { return _getExt( getFileName(path) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _getRootTmpDirName()
    {
        if(_rootTmpDir == null) {
            _rootTmpDir = normalizeDirectorySeparators( System.getProperty("java.io.tmpdir") ) + _InternalDirSep + _JxMakeTmpDirRoot;
        }

        return _rootTmpDir;
    }

    public static String getRootTmpDir() throws IOException
    {
        final String path = _getRootTmpDirName();

        cu_mkdir(path);

        return path;
    }

    public static void delRootTmpDir() throws IOException
    { cu_rmdir_recursive( _getRootTmpDirName() ); }

    private static String _getProjectTmpDirName(final String mainJXMSpecFile_absPath) throws NoSuchAlgorithmException
    { return resolvePath( computeStringHash( resolveAbsolutePath(mainJXMSpecFile_absPath) ), _getRootTmpDirName() ); }

    public static String getProjectTmpDir(final String mainJXMSpecFile_absPath) throws NoSuchAlgorithmException, IOException
    {
        final String path = _getProjectTmpDirName(mainJXMSpecFile_absPath);

        cu_mkdir(path);

        return path;
    }

    public static void delProjectTmpDir(final String mainJXMSpecFile_absPath, boolean printMsg) throws NoSuchAlgorithmException, IOException
    {
        final String path = _getProjectTmpDirName(mainJXMSpecFile_absPath);

        if(printMsg) printf_stdErr(Texts.IMsg_DeletingProjectCacheTmpDir, path);

        cu_rmdir_recursive(path);
    }

    public static boolean setSavedProjectTmpDir(final String mainJXMSpecFile_absPath) throws NoSuchAlgorithmException, IOException
    {
        if(_savedProjectTmpDir != null) return false;

        _savedProjectTmpDir = getProjectTmpDir(mainJXMSpecFile_absPath);

        return true;
    }

    public static void clrSavedProjectTmpDir()
    { _savedProjectTmpDir = null; }

    public static String getSavedProjectTmpDir()
    { return _savedProjectTmpDir; }

    public static boolean clrProjectCacheDir(boolean printMsg) throws IOException
    {
        final String path = getSavedProjectTmpDir();
        if(path == null) return false;

        if(printMsg) printf_stdErr(Texts.IMsg_ClearingProjectCacheDir, path);

        cu_rmdir_recursive(path);
        cu_mkdir(path);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getCacheFileName_dependencyList(final String filePath) throws NoSuchAlgorithmException
    {
        final String tmpDir = getSavedProjectTmpDir();
        if(tmpDir == null) return null;

        return resolvePath( computeStringHash(filePath) + ".dep.cache", tmpDir );
    }

    public static String getCacheFileName_compiledSpecFile(final String filePath) throws NoSuchAlgorithmException
    {
        final String tmpDir = getSavedProjectTmpDir();
        if(tmpDir == null) return null;

        return resolvePath( computeStringHash(filePath) + ".jxm_spec_file.cache", tmpDir );
    }

    public static String getCacheFileName_compiledSpecFile_dependencyList(final String cacheFilePath)
    { return cacheFilePath + ".dep"; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _sunJavaCmd = null;

    public synchronized static String getSunJavaCommand()
    {
        // Check if the command is already obtained before
        if(_sunJavaCmd != null) return _sunJavaCmd;

        // Get and return the command
        final String cmd = System.getProperty("sun.java.command");

        if(cmd != null) {
            // Save the command
            _sunJavaCmd = cmd.split(" ")[0];
            // Return the command
            return _sunJavaCmd;
        }

        try {
            // Fallback - use code source
            final File source = new File( JxMake.class.getProtectionDomain()
                                                      .getCodeSource()
                                                      .getLocation()
                                                      .toURI()
                                        ).getCanonicalFile();
            // Save the command
            _sunJavaCmd = source.getAbsolutePath();
            // Return the command
            return _sunJavaCmd;
        }
        catch(final Exception e) {}

        // Error
        return null;
    }

    private synchronized static boolean _isMainLoadedFromJar()
    {
        final String resource = JxMake.class.getName().replace('.', '/') + ".class";
        final URL    url      = JxMake.class.getClassLoader().getResource(resource);

        return url != null && "jar".equalsIgnoreCase( url.getProtocol() );
    }

    private synchronized static boolean _isMainLoadedFromFile()
    {
        final String resource = JxMake.class.getName().replace('.', '/') + ".class";
        final URL    url      = JxMake.class.getClassLoader().getResource(resource);

        return url != null && "file".equalsIgnoreCase( url.getProtocol() );
    }

    public static URL getJxMakeClassURL()
    { return JxMake.class.getResource("JxMake.class"); }

    public static Path getJxMakeClassDir()
    {
        try {
            final URL url = getJxMakeClassURL();

            // If loaded from a JAR, the URL will be like: jar:file:/...!/JxMake.class
            if( "jar".equalsIgnoreCase( url.getProtocol() ) ) {

                String jarPath = url.getPath();

                // Strip the "!/JxMake.class" part
                jarPath = jarPath.substring( 0, jarPath.indexOf("!") );

                // Convert to URI and then Path
                return Paths.get( new URI(jarPath) ).getParent();

            }

            // If loaded from filesystem (protocol=file)
            return Paths.get( url.toURI() ).getParent();

        }
        catch(final Exception e) {
            return null;
        }
    }

    public static boolean isRunFromJAR()
    {
        // JVM started with -jar

        final String cmd = getSunJavaCommand();

        return (cmd != null) && cmd.endsWith(".jar");
    }

    public static boolean isRunFromJARClass()
    {
        // JVM started with -cp and but the main class bytes came from a JAR on the classpath

        final String cmd = getSunJavaCommand();

        return (cmd != null) && !cmd.endsWith(".jar") && _isMainLoadedFromJar();
    }

    public static boolean isRunFromClassFile()
    {
        // JVM started with -cp and main class is not a JAR file

        final String cmd = getSunJavaCommand();

        return (cmd != null) && !cmd.endsWith(".jar") && _isMainLoadedFromFile();
    }

    /*
     * The code in the functions below is developed based on the information from:
     *     https://stackoverflow.com/a/4194224
     */

    public static URI getJxMakeJarURI() throws URISyntaxException
    { return JxMake.class.getProtectionDomain().getCodeSource().getLocation().toURI(); }

    public static File getJxMakeJarFile() throws URISyntaxException
    { return new File( getJxMakeJarURI() ).getAbsoluteFile(); }

    public static File getJxMakeJarFile_noexcept()
    {
        try {
            return new File( getJxMakeJarURI() );
        }
        catch(final URISyntaxException e) {
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _addJxMakeUserAppDataDir(final String absDirRoot, final String userDir)
    {
        final String absDirPath = SysUtil.resolvePath(userDir, absDirRoot);

        if( !SysUtil.pathIsValidDirectory(absDirPath) ) return;

        if( !_userAppDataDir.contains(absDirPath) ) _userAppDataDir.add(absDirPath);
    }

    private synchronized static ArrayList<String> _getJxMakeUserAppDataDirs()
    {
        // Build the list as needed
        if( _userAppDataDir.isEmpty() ) {
            // NOTE : Synchronize them with what is listed in '../../docs/txt/en_US/01-Introduction.txt' (and its translations) !!!
            if( SysUtil.osIsWindows() ) {
                _addJxMakeUserAppDataDir( getUHD()                     ,                                     _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( getUHD()                     , "AppData/"                        + _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( getUHD()                     , "AppData/Local/"                  + _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( getUHD()                     , "AppData/Roaming/"                + _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( getUHD()                     , "Local Settings/ApplicationData/" + _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( getUHD()                     , _JxMakeUserDotDirRoot             + _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( System.getenv("APPDATA"     ),                                     _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( System.getenv("LOCALAPPDATA"),                                     _JxMakeDataRoot );
            }
            else {
                _addJxMakeUserAppDataDir( getUHD()                     ,                                     _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( getUHD()                     , _JxMakeUserDotDirRoot             + _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( getUHD()                     , ".share/"                         + _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( getUHD()                     , ".share/jxmake/"                  + _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( "/usr/local/share/jxmake"    ,                                     _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( "/usr/share/jxmake"          ,                                     _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( "/var/lib/jxmake"            ,                                     _JxMakeDataRoot );
                _addJxMakeUserAppDataDir( "/opt/jxmake"                ,                                     _JxMakeDataRoot );
            }
            try {
                if( isRunFromClassFile() ) {
                    /*
                    String curClassPath = getJxMakeClassURL().getPath();
                    if( SysUtil.osIsWindows() && curClassPath.charAt(0) == _InternalDirSep ) curClassPath = curClassPath.substring( 1, curClassPath.length() );
                    _addJxMakeUserAppDataDir( SysUtil.resolvePath( "..", SysUtil.getDirName( curClassPath       ) ), _JxMakeDataRoot );
                    */
                    final Path classFile = Paths.get( getJxMakeClassURL().toURI() );
                    final Path classDir  = classFile.getParent();
                    final Path parentDir = classDir .getParent();
                    _addJxMakeUserAppDataDir( parentDir.toString(), _JxMakeDataRoot );
                }
                else {
                    final File curJar = getJxMakeJarFile();
                    if( curJar.getName().endsWith(".jar") ) {
                        _addJxMakeUserAppDataDir( SysUtil.getDirName( curJar.getPath() ), _JxMakeDataRoot );
                    }
                }
            }
            catch(final Exception e) {}
        }

        // Return the list
        return new ArrayList<>(_userAppDataDir);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String ArduinoBoardsTxtDecRuleFile = "ArduinoBoardsTxt_Dec.RULES.txt";
    public static final String DefaultABoardTxtDecRuleFile = "/jxm/tool/" + ArduinoBoardsTxtDecRuleFile;

    public static String findJXMLibAbsFilePath(final String libName)
    {
        // Search from the user application data directories first
        for(final String refPath : SysUtil._getJxMakeUserAppDataDirs() ) {
            final String absFilePath = SysUtil.resolvePath("lib/" + libName + ".jxm", refPath);
            if( SysUtil.pathIsReadableFile(absFilePath) ) return absFilePath;
        }

        // Search from the JAR file resources next
        // NOTE : '_JxMakeJARResRoot' ends with '/'
        final String jarResource = String.format("/%s%s/lib/%s.jxm", _JxMakeJARResRoot, _JxMakeDataRoot, libName);
        if( JxMake.class.getResource(jarResource) != null ) return _JxMakeProgramJARResPrefix + jarResource;

        // It was not found
        return null;
    }

    public static String findArduinoBoardsTxtDecRULESFilePath()
    {
        for(final String refPath : SysUtil._getJxMakeUserAppDataDirs() ) {
            final String absFilePath = SysUtil.resolvePath(ArduinoBoardsTxtDecRuleFile, refPath);
            if( SysUtil.pathIsReadableFile(absFilePath) ) return absFilePath;
        }

        return null;
    }

    public static List<String> getJARResFilePaths(final String rootDir_) throws URISyntaxException, IOException
    {
        final String jarPath = JxMake.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

        final String rootDir = rootDir_.startsWith("jar:file:") ?   rootDir_.substring( rootDir_.indexOf("!") +1 )
                                                                : ( rootDir_.startsWith(_JxMakeJARResRoot)
                                                                    ? rootDir_
                                                                    : (_JxMakeJARResRoot + _InternalDirSep + rootDir_)
                                                                  );

        final String fsUri   = rootDir_.startsWith("jar:file:") ? rootDir_.substring( 0, rootDir_.indexOf("!") )
                                                                : ("jar:file:" + jarPath);

        try( final FileSystem fs = FileSystems.newFileSystem( URI.create(fsUri), Collections.emptyMap() ) ) {
            return Files.walk   ( fs.getPath(rootDir)                    )
                        .filter ( Files::isRegularFile                   )
                        .map    ( Path::toString                         )
                        .map    ( s -> s.startsWith("/") ? s : ('/' + s) )
                        .sorted (                                        )
                        .collect( Collectors.toList()                    );
        }
        catch(final Exception e) {
            return null;
        }
    }

    private static void _copyTextFile(final InputStream srcInputStream, final Path dstPathObj, boolean cnvLineEnding) throws IOException
    {
        if(cnvLineEnding) {
            final BufferedReader bfr = new BufferedReader( new InputStreamReader ( srcInputStream                   , SysUtil._CharEncoding ) );
            final BufferedWriter bfw = new BufferedWriter( new OutputStreamWriter( Files.newOutputStream(dstPathObj), SysUtil._CharEncoding ) );
            while(true) {
                final String line = bfr.readLine();
                if(line == null) break;
                bfw.write(line);
                bfw.newLine();
            }
            bfr.close();
            bfw.close();
        }

        else {
            Files.copy(srcInputStream, dstPathObj, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean _copyJARRes_prepareDstDir(final boolean emptyDestDir, final String dstAbsPath, final boolean printMsg) throws URISyntaxException, IOException
    {
        if( !pathIsAbsolute (dstAbsPath) ) return false;
        if( _isDangerousPath(dstAbsPath) ) return false;

        printf_stdErr("\n");

        if(emptyDestDir) {
            if(printMsg) printf_stdErr(Texts.IMsg_CJFDeletingDestDir, dstAbsPath);
            cu_rmdir_recursive(dstAbsPath);
        }

        if(printMsg) printf_stdErr(Texts.IMsg_CJFCreatingDestDir, dstAbsPath);
        cu_mkdir(dstAbsPath);

        return true;
    }

    private static void _copyJARRes_copyRes(final String resName, final String dstAbsPath, final boolean printMsg) throws URISyntaxException, IOException
    {
        if( resName.endsWith(_JxMakeMarkerFile) ) return;

        final String  fileExt       = getFileExtension(resName);
        final boolean cnvLineEnding = osIsWindows() && ( fileExt.equals("txt") || fileExt.equals("jxm") );
        final Path    dstFilePath   = Paths.get( resolvePath( getFileName(resName), dstAbsPath ) );

        if(printMsg) printf_stdErr( Texts.IMsg_CJFWritingFile, dstFilePath.toString() );

        _copyTextFile( JxMake.class.getResourceAsStream(resName), dstFilePath, cnvLineEnding );
    }

    private static void _copyJARRes_finalize(final boolean printMsg)
    { if(printMsg) printf_stdErr("\n"); }

    public static void copyJARResFilesFromPath(final String resRootDir, final String dstAbsPath, final boolean printMsg) throws URISyntaxException, IOException
    {
        if( !_copyJARRes_prepareDstDir(true , dstAbsPath, printMsg) ) return;

        for( final String resName : getJARResFilePaths(resRootDir) ) {
            _copyJARRes_copyRes(resName, dstAbsPath, printMsg);
        }

        _copyJARRes_finalize(printMsg);
    }

    public static void copyJARResFileFromPath(final String resName, final String dstAbsPath, final boolean printMsg) throws URISyntaxException, IOException
    {
        if( !_copyJARRes_prepareDstDir(false, dstAbsPath, printMsg) ) return;

        _copyJARRes_copyRes(resName, dstAbsPath, printMsg);

        _copyJARRes_finalize(printMsg);
    }

    public static void copyJARRes_embeddedDoc(final String dstAbsPath_, final boolean printMsg) throws URISyntaxException, IOException
    {
        final String dstAbsPath = (dstAbsPath_ != null) ? dstAbsPath_
                                                        : resolvePath( _JxMakeDataRoot + _InternalDirSep + "docs", getUHD() );

        copyJARResFilesFromPath( SysUtil.get_JxMakeJARResDocTXTRoot(), dstAbsPath, printMsg );
    }

    public static void copyJARRes_embeddedLib(final String dstAbsPath_, final boolean printMsg) throws URISyntaxException, IOException
    {
        final String dstAbsPath = (dstAbsPath_ != null) ? dstAbsPath_
                                                        : resolvePath( _JxMakeDataRoot + _InternalDirSep + "lib" , getUHD() );

        copyJARResFilesFromPath( SysUtil._JxMakeJARResLibRoot, dstAbsPath, printMsg );
    }

    public static void copyJARRes_embeddedABrdDec(final String dstAbsPath_, final boolean printMsg) throws URISyntaxException, IOException
    {
        final String dstAbsPath = (dstAbsPath_ != null) ? dstAbsPath_
                                                        : resolvePath( _JxMakeDataRoot, getUHD() );

        copyJARResFileFromPath( DefaultABoardTxtDecRuleFile, dstAbsPath, printMsg );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String resolveIP(final String hostname) throws UnknownHostException
    {
        final InetAddress address = InetAddress.getByName(hostname);

        return address.getHostAddress();
    }

    public static byte[] readTextFileAsByteArray(final Path filePath) throws IOException
    { return Files.readAllBytes(filePath); }

    public static byte[] readTextFileAsByteArray(final String filePath) throws IOException
    { return readTextFileAsByteArray( Paths.get(filePath) ); }

    public static String readTextFileAsString(final Path filePath) throws IOException, UnsupportedEncodingException
    { return new String( readTextFileAsByteArray(filePath), SysUtil._CharEncoding ); }

    public static String readTextFileAsString(final String filePath) throws IOException, UnsupportedEncodingException
    { return readTextFileAsString( Paths.get(filePath) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static PrintStream _psErr = null;
    private static PrintStream _psOut = null;

    private static PrintStream _newStdPS(final PrintStream dst)
    {
        if( osIsCygwin() ) {
            try {
                return new PrintStream(dst, true, _CharEncoding);
            }
            catch(final Exception e) {}
        }

        return dst;
    }

    public static PrintStream stdErr()
    {
        if(_psErr == null) _psErr = _newStdPS(System.err);
        return _psErr;
    }

    public static PrintStream stdOut()
    {
        if(_psOut == null) _psOut = _newStdPS(System.out);
        return _psOut;
    }

    public static PrintStream stdDbg()
    { return stdOut(); }

    public static InputStream stdInp()
    { return System.in; }

    public static Console stdCon()
    { return System.console(); }

    public static void setSilentSErrMsg(boolean silent)
    { _silentSErrMsg = silent; }

    public static boolean getSilentSErrMsg()
    { return _silentSErrMsg; }

    public static void forceSilentSErrMsg()
    {
        _silentSErrMsgSaved = _silentSErrMsg;
        _silentSErrMsg      = true;
    }

    public static void restoreSilentSErrMsg()
    { _silentSErrMsg = _silentSErrMsgSaved; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class StringPrintStream {
        private final ByteArrayOutputStream _baos;
        private final PrintStream           _ps;

        public StringPrintStream() throws UnsupportedEncodingException
        {
            _baos = new ByteArrayOutputStream();
            _ps   = new PrintStream(_baos, true, _CharEncoding);
        }

        public void clear()
        { _baos.reset(); }

        public PrintStream printStream()
        { return _ps; }

        public String string() throws UnsupportedEncodingException
        { return _baos.toString(_CharEncoding); }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int DefaultExitErrorCode = -1;

    public static void jansiSystemInstall()
    { if( osIsWindows() ) AnsiConsole.systemInstall();  }

    public static void systemExit(final int status)
    {
        if( osIsWindows() ) AnsiConsole.systemUninstall();

        System.exit(status);
    }

    public static void systemExit()
    {
        if( osIsWindows() ) AnsiConsole.systemUninstall();

        System.exit(0);
    }

    public static void systemExitError(final Exception e)
    {
        if(e != null) e.printStackTrace();

        if( osIsWindows() ) AnsiConsole.systemUninstall();

        System.exit(DefaultExitErrorCode);
    }

    public static void systemExitError()
    { systemExitError(null); }

    public static RuntimeException systemExitError_runtimeException(final Exception e)
    {
        systemExitError(e);

        return new RuntimeException(e);
    }

    public static RuntimeException systemExitError_staticInitializationBlock(final Exception e)
    {
        systemExitError(e);

        // The caller must throw this or javac will produce 'error: variable <XXX> not initialized in the default constructor'
        return new RuntimeException(e);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Pattern _jxmErrorPrefix = Pattern.compile("(jxm\\.JXM\\w+Error: \\w+ ERROR:)(?:\\s*\\1)+");

    private static String _colorErrMsgDetail(final String errMsg)
    {
        final String fixMsg = _jxmErrorPrefix.matcher(errMsg).replaceAll("$1");

        return fixMsg.replaceAll( '(' + Texts._JxMakeErrorPrefix + ')'     , XCom.AC_c_red() + "$1"  + XCom.AC_c_white()                                                                  )
                     .replaceAll( '(' + Texts._RTExecErrorPrefix + " )(.+)", "$1\n$2"                                                                                                     )
                     .replaceAll( '(' + Texts._RTExecErrorPrefix + ')'     , XCom.AC_c_yellow () + "$1" + XCom.AC_c_white()                                                               )
                     .replaceAll( "(jxm\\..+?:)(.+?:)?"                    , XCom.AC_c_magenta() + "$1$2" + XCom.AC_c_white()                                                             )
                     .replaceAll( ":(\\d+):(\\d+):"                        , ':' + XCom.AC_c_green() + "$1" + XCom.AC_c_white() + ':' + XCom.AC_c_cyan() + "$2" + XCom.AC_c_white() + ':' )
               + XCom.AC_c_reset();
    }

    public static void printf_stdOut(final String errMsg, final Object... args)
    {
        if(errMsg == null) return;

        stdOut().printf(errMsg, args);
    }

    public static void printf_stdErr(final String errMsg, final Object... args)
    {
        if(_silentSErrMsg || errMsg == null) return;

        stdErr().printf(errMsg, args);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void printfWarning(final String path, final int lNum, final int cNum, final String errMsg, final Object... args)
    {
        printf_stdErr(
            "%s%s%s%s:%s%d%s:%s%d%s: %s%s\n",
            XCom.AC_c_yellow(), Texts._JxMakeWarningPrefix, XCom.AC_c_white(),
            path, XCom.AC_c_green(), lNum, XCom.AC_c_white(), XCom.AC_c_cyan(), cNum, XCom.AC_c_white(),
            String.format(errMsg, args), XCom.AC_c_reset()
        );
    }

    public static void printfSimpleWarning(final String errMsg, final Object... args)
    {
        printf_stdErr(
            "%s%s%s%s%s\n",
            XCom.AC_c_yellow(), Texts._JxMakeWarningPrefix, XCom.AC_c_white(),
            String.format(errMsg, args), XCom.AC_c_reset()
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String sprintError(final String path, final int lNum, final int cNum, final String errMsg)
    {
        if(path == null && lNum < 0 && cNum < 0)
           return String.format("%s%s"          , Texts._JxMakeErrorPrefix, errMsg);
        else
           return String.format("%s%s:%d:%d: %s", Texts._JxMakeErrorPrefix, path, lNum, cNum, errMsg);
    }

    public static String sprintError(final String errMsg)
    { return sprintError(null, -1, -1, errMsg); }

    public static boolean printError(final String path, final int lNum, final int cNum, final String errMsg)
    {
        // Simply return if there is no message
        if(errMsg == null) return false;

        // Format the message
        final String fmtMsg = sprintError(path, lNum, cNum, errMsg);

        // Rebuild the message
        final StringBuilder sb     = new StringBuilder();
        final String[]      tokens = fmtMsg.split( Pattern.quote(Texts._JxMakeErrorPrefix) );
              boolean       first  = true;

        for(final String t : tokens) {
            if( t.isEmpty() ) continue;
            if(first) {
                first = false;
                sb.append(Texts._JxMakeErrorPrefix);
            }
            sb.append("\n");
            sb.append(t);
        }

        // Print the error message
        stdOut().println( _colorErrMsgDetail( sb.toString() ) );

        // Done
        return true;
    }

    public static boolean printError(final String errMsg)
    { return printError(null, -1, -1, errMsg); }

    public static void printSuppressedError(final String path, final int lNum, final int cNum, final String errMsg)
    {
        if(_silentSErrMsg || errMsg == null) return;

        final String fmtMsg = String.format(
            XCom.AC_c_yellow() + Texts._WarningSuppressedError + XCom.AC_c_white() + "%s\n%s:%d:%d: %s\n",
            Texts._JxMakeErrorPrefix, path, lNum, cNum, errMsg
        );

        stdErr().print( _colorErrMsgDetail(fmtMsg) );
    }

    public static void forceStackDumpAndExit(final String errMsg, final Object... args)
    {
        try{
            throw new Exception( XCom.errorString(errMsg, args) );
        }
        catch(final Exception e) {
            stdOut().println( XCom.AC_c_green() + Texts.IMsg_IntentionalStackDumpBeg + XCom.AC_c_white() );
            e.printStackTrace();
            stdOut().println( XCom.AC_c_green() + Texts.IMsg_IntentionalStackDumpEnd + XCom.AC_c_reset() );
            systemExitError();
        }
    }

    public static String stringFromStackTrace(final Exception e)
    {
        final StringWriter sw = new StringWriter();

        e.printStackTrace( new PrintWriter (sw) );

        return sw.toString();
    }

    public static String htmlStringFromStackTrace(final Exception e)
    {
        return "<html><pre><b>"
             + XCom.escapeHTML( e.getMessage() )
             + "</b><br><br>"
             + XCom.escapeHTML( SysUtil.stringFromStackTrace(e) )
             + "</pre></html>";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final ArrayList<String> _javaCmd     = new ArrayList<>();
    private static       String            _javaBinPath = null;
    private static       String            _javaBinVer  = null;
    private static       String            _javaAbsCP   = null;

    public synchronized static long getPID()
    {
        try {
            // Try Java 9+ API via reflection
            final Class<?> phClass = Class.forName("java.lang.ProcessHandle");
            final Object   ph      = phClass.getMethod("current").invoke(null);
            return (Long) phClass.getMethod("pid").invoke(ph);
        }
        catch(final Throwable t) {
            // Fallback for Java 8
            final String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong( jvmName.split("@")[0] );
        }
    }

    public synchronized static String _getJavaBinaryPath_impl_Windows_VBS()
    {
        /*
         * The code in this function is developed based on the information from:
         *     https://www.rgagnon.com/javadetails/java-get-running-jvm-path.html
         */

        final long   pid  = getPID();
              String path = null;

        try {
            final File file = File.createTempFile("getJavaPath", ".vbs");
            file.deleteOnExit();

            final String vbs = "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")                                   \n"
                             + "Set service = locator.ConnectServer()                                                        \n"
                             + "Set procs   = service.ExecQuery(\"select * from Win32_Process where ProcessId='" + pid +"'\")\n"
                             + "For Each proc in procs                                                                       \n"
                             + "    Wscript.echo proc.ExecutablePath                                                         \n"
                             + "Next                                                                                         \n";

            final FileWriter fw = new FileWriter(file);
            fw.write(vbs);
            fw.close();

          //final Process        proc = Runtime.getRuntime().exec( "cscript //NoLogo " + file.getPath() );
            final Process        proc = new ProcessBuilder( "cscript", "//NoLogo", file.getPath() ).start();
            final BufferedReader br   = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            path = br.readLine();
            br.close();
        }
        catch(final Exception e) {
            return null;
        }

        return path;
    }

    public synchronized static String getJavaBinaryPath()
    {
        // Check if the path is already obtained before
        if(_javaBinPath != null) return _javaBinPath;

        // Try Java 9+ ProcessHandle via reflection
        try {
            final Class<?> phClass   = Class.forName("java.lang.ProcessHandle"     );
            final Class<?> infoClass = Class.forName("java.lang.ProcessHandle$Info");
            final Object   ph        = phClass  .getMethod("current").invoke(null);
            final Object   info      = phClass  .getMethod("info"   ).invoke(ph  );
            final Object   cmdOpt    = infoClass.getMethod("command").invoke(info);
            if(cmdOpt != null) {
                final Class<?> optClass = Class.forName("java.util.Optional"); // 'command()' returns 'Optional<String>'
                if( (Boolean) optClass.getMethod("isPresent").invoke(cmdOpt) ) {
                    // Save the path
                    _javaBinPath = (String) optClass.getMethod("get").invoke(cmdOpt);
                    // Return the path
                    return _javaBinPath;
                }
            }
        }
        catch(final Throwable t) {
            // Ignore and fallback
        }

        // Get the path
        if( osIsWindows() ) _javaBinPath = _getJavaBinaryPath_impl_Windows_VBS();

        if(_javaBinPath == null) _javaBinPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        // Return the path
        return _javaBinPath;
    }

    public synchronized static String getJavaBinaryVersion()
    {
        // Check if the version is already obtained before
        if(_javaBinVer != null) return _javaBinVer;

        // Get the version
        try {

            // Execute the command
          //final Process        proc = Runtime.getRuntime().exec( getJavaBinaryPath() + " " + "-XshowSettings:properties -version" );
            final Process        proc = new ProcessBuilder( getJavaBinaryPath(), "-XshowSettings:properties", "-version" ).start();
            final BufferedReader bro  = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            final BufferedReader bre  = new BufferedReader( new InputStreamReader( proc.getErrorStream() ) );

            // Get the version string
            String line    = null;
            String version = null;

                while( ( line = bro.readLine() ) != null ) {
                    if( line.contains("java.specification.version") ) {
                        version = line.split("=")[1].trim();
                        break;
                    }
                }

            if(version == null) {
                while( ( line = bre.readLine() ) != null ) {
                    if( line.contains("java.specification.version") ) {
                        version = line.split("=")[1].trim();
                        break;
                    }
                }
            }

            // Ensure process completes
            proc.waitFor();

            // Save the version string
            if(version != null) _javaBinVer = version;

            // Return the version string
            return _javaBinVer;

        }
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Error
            return "0";
        }
    }

    public synchronized static int getJavaBinaryVersionMajor()
    {
        final String javaVer = getJavaBinaryVersion();

        return javaVer.startsWith("1.") ? Integer.parseInt( javaVer.substring(2) ) : Integer.parseInt(javaVer);
    }

    public synchronized static String getAbsClassPath()
    {
        // Check if the absolute class path is already obtained before
        if(_javaAbsCP != null) return _javaAbsCP;

        // Get the class path
        final String        cp      = System.getProperty("java.class.path");
        final String[]      entries = cp.split(File.pathSeparator);
        final StringBuilder absCp   = new StringBuilder();

        for(int i = 0; i < entries.length; ++i) {

            try {
                final File   f   = new File(entries[i]);
                final String abs = f.getCanonicalPath();
                absCp.append(abs);
            }
            catch(final IOException e) {
                absCp.append( new File(entries[i]).getAbsolutePath() );
            }

            if(i < entries.length - 1) absCp.append(File.pathSeparator);

        } // for

        // Save the class path
        _javaAbsCP = absCp.toString();

        // Return the class path
        return _javaAbsCP;
    }

    public synchronized static ArrayList<String> getJavaCmd(final boolean withColorTheme)
    {
        if( !_javaCmd.isEmpty() ) return  new ArrayList<>(_javaCmd);

        try {
            final String javaBin = getJavaBinaryPath();
            final int    mjrVer  = getJavaBinaryVersionMajor();

            // Running with java -jar
            if( isRunFromJAR() ) {
                final File curJar = getJxMakeJarFile();

                if( curJar.getName().endsWith(".jar") ) {
                                     _javaCmd.add( javaBin                              );
                    if(mjrVer >= 22) _javaCmd.add( "--enable-native-access=ALL-UNNAMED" );
                                     _javaCmd.add( "-jar"                               );
                                     _javaCmd.add( curJar.getPath()                     );
                }
            }

            // Running with -cp but main class loaded from a JAR
            else if( isRunFromJARClass() ) {

                final String cp = getAbsClassPath();

                                 _javaCmd.add( javaBin                              );
                if(mjrVer >= 22) _javaCmd.add( "--enable-native-access=ALL-UNNAMED" );
                                 _javaCmd.add( "-cp"                                );
                                 _javaCmd.add( cp                                   );

                final String cmd = getSunJavaCommand();
                if( cmd != null && !cmd.endsWith(".jar") ) {
                  //_javaCmd.add( ( new File(cmd) ).getAbsoluteFile().toString() );
                    _javaCmd.add( ( new File(cmd) ).getName() );
                }
            }

            // Running from loose .class files
            else if( isRunFromClassFile() ) {
                String cp = getAbsClassPath();
                if( !cp.isEmpty() ) cp = File.pathSeparator + cp;

                                 _javaCmd.add( javaBin                                         );
                if(mjrVer >= 22) _javaCmd.add( "--enable-native-access=ALL-UNNAMED"            );
                                 _javaCmd.add( "-cp"                                           );
                                 _javaCmd.add( getJxMakeClassDir().getParent().toString() + cp );

                final String cmd = getSunJavaCommand();
                if( cmd != null && !cmd.endsWith(".jar") ) {
                  //_javaCmd.add( ( new File(cmd) ).getAbsoluteFile().toString() );
                    _javaCmd.add( ( new File(cmd) ).getName() );
                }
            }

            // Add the enable headless mode as needed
            if( "true".equals( System.getProperty("java.awt.headless") ) ) _javaCmd.add( "--en-headless" );

            // Add the color theme flag as needed
            if(withColorTheme) _javaCmd.add( XCom.useLightColorTheme() ? "--lct" : "--dct" );
        }
        catch(final Exception e) {
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return  new ArrayList<>(_javaCmd);
    }

    public synchronized static ArrayList<String> getJavaCmd()
    { return getJavaCmd(true); }

    public static boolean restartApplication(final String newCWDAbsPath, final List<String> args)
    {
        try {

            // Get the Java command
            final ArrayList<String> cmd = getJavaCmd();

            // Check for error
            if( cmd.isEmpty() ) return false;

            // Add the arguments
            for(final String arg : args) cmd.add(arg);

            /*
            for(final String s : cmd) stdDbg().println(s);
            //*/

            // Prepare the process builder
            final ProcessBuilder pb = new ProcessBuilder();

            pb.command(cmd);
            pb.directory( new File(newCWDAbsPath) );
            pb.inheritIO();

            // Replace the current process with a new process and pass the exit code
            systemExit( pb.start().waitFor() );

        } // try
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        // Error if it got here
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String execlp(final String... cmd_and_args)
    {
        final StringBuilder strBuilder = new StringBuilder();

        try {
            final Process        process    = new ProcessBuilder(cmd_and_args).start();
            final BufferedReader procStdErr = new BufferedReader( new InputStreamReader ( process.getErrorStream () ) );
            final BufferedReader procStdOut = new BufferedReader( new InputStreamReader ( process.getInputStream () ) );

            String line;
            while( ( line = procStdErr.readLine() ) != null ) strBuilder.append(line + "\n");
            while( ( line = procStdOut.readLine() ) != null ) strBuilder.append(line + "\n");

            process.waitFor();

            procStdErr.close();
            procStdOut.close();
        }
        catch(final IOException | InterruptedException e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return strBuilder.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _init_ll_test_app(final String[] args) throws Exception
    {
        // Parse the argument(s)
        final ArgParser argParser = new ArgParser(args);

        // Enable printing all exception stack trace on error if asked
        if( argParser.enableAllExceptionStackTrace() ) XCom.setEnableAllExceptionStackTrace(true);

        // Enable hardware headless mode if asked
        if( argParser.enableHeadless() ) System.setProperty("java.awt.headless", "true");

        // Enable hardware acceleration via OpenGL and Direct3D unconditionally
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.d3d"   , "true");

        // Initialize global
        JxMake.initializeGlobal();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String __selectPreferredFont(final String curFontName, final String prefFontName, final List<String> availFontNames)
    {
        if(curFontName != null) return curFontName;

        if( availFontNames.contains(prefFontName) ) return prefFontName;

        return null;
    }

    public static XCom.Pair<String, Integer> getTextAreaMonospacedFontSpec(final String envVarFontName, final String envVarFontSize)
    {
        /*
        // https://fontmeme.com/fonts/freemono-font
        // https://github.com/wheybags/freeablo/tree/master/resources/fonts/FreeMono
        // https://fonts.google.com/specimen/Source+Code+Pro
        // https://fonts.google.com/specimen/Fira+Code
        // https://dejavu-fonts.github.io/Download.html
        // https://github.com/liberationfonts/liberation-fonts/releases
        //*/

        final List<String> fontList = Arrays.asList( GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames() );
              String       fontName =                   SysUtil.getEnv(envVarFontName, null)  ;
              int          fontSize = Integer.parseInt( SysUtil.getEnv(envVarFontSize, "0" ) );

        fontName = __selectPreferredFont(null, fontName, fontList); // Ensure that the user-selected font actually exists

        if(fontName == null) {
            if( SysUtil.osIsWindows() ) {
                fontName = __selectPreferredFont(fontName, "FreeMono"        , fontList);
                fontName = __selectPreferredFont(fontName, "Source Code Pro" , fontList);
                fontName = __selectPreferredFont(fontName, "Fira Code"       , fontList);
                fontName = __selectPreferredFont(fontName, "DejaVu Sans Mono", fontList);
                fontName = __selectPreferredFont(fontName, "Liberation Mono" , fontList);
                fontName = __selectPreferredFont(fontName, "Courier New"     , fontList);
            }
            if(fontName == null) fontName = Font.MONOSPACED;
        }

        if(fontSize <= 0) {
                 if( fontName.equals("FreeMono"       ) ) fontSize = 15;
            else if( fontName.equals("Source Code Pro") ) fontSize = 13;
            else                                          fontSize = 12;
        }

        return new XCom.Pair<String, Integer>(fontName, fontSize);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # 'jxm/resource/*'        is for icon files, audio files, and translation files
     *      : # '<_JxMakeJARResRoot>/*' is for documentation files (*.txt, *.html, etc.) and 'JxMake Loadable Libraries' (*.jxm) files
     */

    public static URL getResourceURL(final String name)
    {
        final ClassLoader cl = JxMake.class.getClassLoader();

        URL url = (cl != null) ? cl.getResource(name) : ClassLoader.getSystemResource(name);
        if(url == null) url = JxMake.class.getResource("/" + name);

        return url;
    }

    public static URL getResourceDirectory(final String dirName)
    {
        // Look for the marker file inside the directory
        final String markerName = dirName.endsWith(_InternalDirSepStr)
                                ? dirName +                   _JxMakeMarkerFile
                                : dirName + _InternalDirSep + _JxMakeMarkerFile;

        final URL    markerUrl  = getResourceURL(markerName);

        if(markerUrl == null) return null;

        // Extract the directory part
        try {
            String uriStr = markerUrl.toURI().toString();

            if(  uriStr.endsWith(_JxMakeMarkerFile ) ) uriStr  = uriStr.substring( 0, uriStr.length() - _JxMakeMarkerFile.length() );
            if( !uriStr.endsWith(_InternalDirSepStr) ) uriStr += _InternalDirSep;

            return ( new URI(uriStr) ).toURL();
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return null;
    }

    public static String get3rdPartyRootDirectory()
    {
        String rootPath = null;

        try {
            // Running with java -jar
            if( SysUtil.isRunFromJAR() ) {
                final File curJar = SysUtil.getJxMakeJarFile();
                rootPath = curJar.getParent().toString();
            }
            // Running with -cp but main class loaded from a JAR or running from loose .class files
            else if( SysUtil.isRunFromJARClass() || SysUtil.isRunFromClassFile() ) {
                final String cmd = SysUtil.getSunJavaCommand();
                rootPath = ( new File(cmd) ).getAbsoluteFile().getParent().toString();
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return rootPath;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String loadTextResource(final URL url) throws Exception
    {
        try(
            final BufferedReader br = new BufferedReader( new InputStreamReader( url.openStream(), _CharSet ) )
        ) {

            final StringBuilder sb = new StringBuilder();
                  String        line;
            while( ( line = br.readLine() ) != null ) sb.append(line).append('\n');
            return sb.toString();

        }
        catch(final Exception e) {
            throw e;
        }
    }

    public static String loadTextResource(final String name) throws Exception
    { return loadTextResource( getResourceURL(name) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String[] get_JxMakeDocTXTPossibleDirs() throws Exception
    {
        final String  jxd = SysUtil.getJXD();

        return new String[] {
            jxd +    "/docs/txt/" + _JxMakeLanguageCountry  ,
            jxd + "/../docs/txt/" + _JxMakeLanguageCountry  ,
            jxd +    "/docs/txt/" + _JxMakeLanguageCountryFB,
            jxd + "/../docs/txt/" + _JxMakeLanguageCountryFB,
            jxd +    "/docs/txt/" + _FallbackLanguageCountry,
            jxd + "/../docs/txt/" + _FallbackLanguageCountry
        };
    }

    public static String get_JxMakeJARResDocTXTRoot()
    {
                    URL url = getResourceDirectory(_JxMakeJARResDocTXTRoot + _JxMakeLanguageCountry  );
        if(url == null) url = getResourceDirectory(_JxMakeJARResDocTXTRoot + _JxMakeLanguageCountryFB);
        if(url == null) url = getResourceDirectory(_JxMakeJARResDocTXTRoot + _FallbackLanguageCountry);

        return (url != null) ? url.toString() : null;
    }

    public static URL getTextResourceURL_defDocTXT(final String name)
    {
                    URL url = getResourceURL(_JxMakeJARResDocTXTRoot + _JxMakeLanguageCountry   + '/' + name);
        if(url == null) url = getResourceURL(_JxMakeJARResDocTXTRoot + _JxMakeLanguageCountryFB + '/' + name);
        if(url == null) url = getResourceURL(_JxMakeJARResDocTXTRoot + _FallbackLanguageCountry + '/' + name);

        return url;
    }

    public static String loadTextResource_defDocTXT(final String name) throws Exception
    { return loadTextResource( getTextResourceURL_defDocTXT(name) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static URL getTextResourceURL_defDocHTML(final String name)
    {
                    URL url = getResourceURL(_JxMakeJARResDocHTMLRoot + _JxMakeLanguageCountry   + '/' + name);
        if(url == null) url = getResourceURL(_JxMakeJARResDocHTMLRoot + _JxMakeLanguageCountryFB + '/' + name);
        if(url == null) url = getResourceURL(_JxMakeJARResDocHTMLRoot + _FallbackLanguageCountry + '/' + name);

        return url;
    }

    public static String loadTextResource_defDocHTML(final String name) throws Exception
    { return loadTextResource( getTextResourceURL_defDocHTML(name) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _spellingParserDicDir = null;

    public static String findSpellingParserDicDir()
    {
        if(_spellingParserDicDir != null) return _spellingParserDicDir;

        // Get the root path
        final String rootPath = get3rdPartyRootDirectory();

        if(rootPath == null) return null;

        // Check some directories
        final String[] paths = new String[] {
            "dics"                 ,
            _JxMakeDistDir + "dics",
            "3rd_party/dics"       ,
            "../3rd_party/dics"    ,
            "../../3rd_party/dics"
        };

        for(final String p : paths) {
            final String path = SysUtil.resolvePath(p, rootPath);
            if( SysUtil.pathIsValidDirectory(path) ) {
                _spellingParserDicDir = path;
                return _spellingParserDicDir;
            }
        }

        // Not found
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _unicodeTextDir = null;

    public static String findUnicodeTextDir()
    {
        if(_unicodeTextDir != null) return _unicodeTextDir;

        // Get the root path
        final String rootPath = get3rdPartyRootDirectory();

        if(rootPath == null) return null;

        // Check some directories
        final String[] paths = new String[] {
            "unicode"                 ,
            _JxMakeDistDir + "unicode",
            "3rd_party/unicode"       ,
            "../3rd_party/unicode"    ,
            "../../3rd_party/unicode"
        };

        for(final String p : paths) {
            final String path = SysUtil.resolvePath(p, rootPath);
            if( SysUtil.pathIsValidDirectory(path) ) {
                _unicodeTextDir = path;
                return _unicodeTextDir;
            }
        }

        // Not found
        return null;
    }

    public static URL getUnicodeTextURL_defLoc(final String name) throws Exception
    {
        final String rootPath = findUnicodeTextDir();

        return (rootPath == null) ? null : new File(rootPath + '/' + name + ".txt").toURI().toURL();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static URL getTranslationFileURL_defLoc(final String name)
    { return getResourceURL("jxm/resource/l10n/text/" + name + ".po"); }

    public static URL getUsageHelpFileURL_defLoc(final String name)
    { return getResourceURL("jxm/resource/l10n/usage/" + name + ".txt"); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ImageIcon newImageIcon(final URL url)
    { return (url == null) ? null : new ImageIcon(url); }

    public static ImageIcon newImageIcon(final String name)
    { return newImageIcon( getResourceURL(name) ); }

    public static URL getImageIconURL_defLoc(final String name)
    { return getResourceURL("jxm/resource/icon/" + name); }

    public static ImageIcon newImageIcon_defLoc(final String name)
    { return newImageIcon( getImageIconURL_defLoc(name) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static URL getAudioURL_defLoc(final String name)
    { return getResourceURL("jxm/resource/audio/" + name); }

} // class SysUtil
