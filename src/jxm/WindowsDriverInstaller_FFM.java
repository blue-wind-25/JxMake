/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.File;
import java.io.IOException;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;

import java.lang.invoke.MethodHandle;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.SecureRandom;

import java.util.Locale;
import java.util.regex.Pattern;

import jxm.xb.*;


/*
 * WINDOWS 10+/JAVA 25+ NOTE
 *
 * This backend calls the relevant Win32 APIs directly through the Foreign Function and Memory
 * (FFM) API instead of spawning powershell.exe. It intentionally targets Windows 10 and later
 * only - see WindowsDriverInstaller_PS1 for a backend that also supports Windows 7/8/8.1.
 *
 * Two operations require an elevated (administrator) token : creating/trusting the self-signed
 * certificate (writes to the LocalMachine "Root"/"TrustedPublisher" stores) and signing the
 * catalog. Since a non-elevated JVM cannot simply "elevate a native call", this class relaunches
 * ITSELF through ShellExecuteExW with lpVerb="runas" (java.exe/javaw.exe, same classpath, with a
 * hidden marker argument - see main() below), and the elevated child process performs the actual
 * native work, writing its result to a temporary log file that the parent process reads back once
 * the child exits. This mirrors the elevated-child-process pattern used by WindowsDriverInstaller_PS1
 * (Start-Process -Verb RunAs), but without needing PowerShell.
 *
 * Because none of this can be exercised on a non-Windows build machine, every native call site
 * below is commented with the Windows SDK header (wincrypt.h / ncrypt.h / mscat.h / shellapi.h)
 * that documents it. The catalog-creation path (createAndSignCatalog) is the highest-risk area :
 * it deliberately avoids building a full SPC_INDIRECT_DATA_CONTENT/SIP_SUBJECTINFO blob (that
 * struct is undocumented in terms of exact field layout) and instead stores the file hash as a
 * plain CryptCATPutAttrInfo "HASH" attribute. If pnputil/Device Installer rejects catalogs built
 * this way on a real Windows 10+ box, that is the first place to look.
 */
@SuppressWarnings("restricted")
public final class WindowsDriverInstaller_FFM extends WindowsDriverInstaller {

    // Marker argument that switches main() into "elevated helper op" mode - never used interactively
    private static final String ELEVATED_OP_ARG = "--__wdi_ffm_elevated_op__";

    public WindowsDriverInstaller_FFM() {}

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final ValueLayout.OfInt DW  = ValueLayout.JAVA_INT; // DWORD/BOOL/LONG/HRESULT/ALG_ID
    private static final AddressLayout     PTR = ValueLayout.ADDRESS;  // Any Win32 handle or pointer type

    private static Throwable    _initError;
    private static Arena        _arena;

    private static MethodHandle _CreateFileW;
    private static MethodHandle _CloseHandle;
    private static MethodHandle _GetLastError;
    private static MethodHandle _WaitForSingleObject;
    private static MethodHandle _GetExitCodeProcess;
    private static MethodHandle _TerminateProcess;

    private static MethodHandle _ShellExecuteExW;

    private static MethodHandle _CertOpenStore;
    private static MethodHandle _CertCloseStore;
    private static MethodHandle _CertFindCertificateInStore;
    private static MethodHandle _CertFreeCertificateContext;
    private static MethodHandle _CertStrToNameW;
    private static MethodHandle _CertCreateSelfSignCertificate;
    private static MethodHandle _CertAddCertificateContextToStore;
    private static MethodHandle _CryptEncodeObjectEx;
    private static MethodHandle _CryptSIPRetrieveSubjectGuid;
    private static MethodHandle _LocalFree;

    private static MethodHandle _NCryptOpenStorageProvider;
    private static MethodHandle _NCryptCreatePersistedKey;
    private static MethodHandle _NCryptSetProperty;
    private static MethodHandle _NCryptFinalizeKey;
    private static MethodHandle _NCryptFreeObject;

    private static MethodHandle _CryptCATAdminAcquireContext2;
    private static MethodHandle _CryptCATAdminCalcHashFromFileHandle2;
    private static MethodHandle _CryptCATAdminReleaseContext;
    private static MethodHandle _CryptCATOpen;
    private static MethodHandle _CryptCATClose;
    private static MethodHandle _CryptCATPutMemberInfo;
    private static MethodHandle _CryptCATPutAttrInfo;

    private static MethodHandle _SignerSignEx2;
    private static MethodHandle _SignerFreeSignerContext;

    static {
        try {
            _arena = Arena.ofShared();

            final Linker      linker    = Linker.nativeLinker();
            final SymbolLookup kernel32 = SymbolLookup.libraryLookup("Kernel32.dll", _arena);
            final SymbolLookup shell32  = SymbolLookup.libraryLookup("Shell32.dll" , _arena);
            final SymbolLookup crypt32  = SymbolLookup.libraryLookup("Crypt32.dll" , _arena);
            final SymbolLookup kernelB  = SymbolLookup.libraryLookup("Kernel32.dll", _arena);
            final SymbolLookup ncrypt   = SymbolLookup.libraryLookup("Ncrypt.dll"  , _arena);
            final SymbolLookup wintrust = SymbolLookup.libraryLookup("Wintrust.dll", _arena);
            final SymbolLookup mssign32 = SymbolLookup.libraryLookup("Mssign32.dll", _arena);

            // Kernel32.dll (fileapi.h / handleapi.h / synchapi.h / processthreadsapi.h / errhandlingapi.h)
            _CreateFileW         = _bind( linker, kernel32, "CreateFileW"        , FunctionDescriptor.of(PTR, PTR, DW, DW, PTR, DW, DW, PTR) );
            _CloseHandle         = _bind( linker, kernel32, "CloseHandle"        , FunctionDescriptor.of(DW , PTR) );
            _GetLastError        = _bind( linker, kernel32, "GetLastError"       , FunctionDescriptor.of(DW) );
            _WaitForSingleObject = _bind( linker, kernel32, "WaitForSingleObject", FunctionDescriptor.of(DW , PTR, DW) );
            _GetExitCodeProcess  = _bind( linker, kernel32, "GetExitCodeProcess" , FunctionDescriptor.of(DW , PTR, PTR) );
            _TerminateProcess    = _bind( linker, kernel32, "TerminateProcess"   , FunctionDescriptor.of(DW , PTR, DW) );
            _LocalFree           = _bind( linker, kernelB , "LocalFree"          , FunctionDescriptor.of(PTR, PTR) );

            // Shell32.dll (shellapi.h)
            _ShellExecuteExW = _bind( linker, shell32, "ShellExecuteExW", FunctionDescriptor.of(DW, PTR) );

            // Crypt32.dll (wincrypt.h)
            _CertOpenStore                    = _bind( linker, crypt32, "CertOpenStore"                   , FunctionDescriptor.of(PTR, PTR, DW, PTR, DW, PTR) );
            _CertCloseStore                   = _bind( linker, crypt32, "CertCloseStore"                  , FunctionDescriptor.of(DW , PTR, DW) );
            _CertFindCertificateInStore       = _bind( linker, crypt32, "CertFindCertificateInStore"      , FunctionDescriptor.of(PTR, PTR, DW, DW, DW, PTR, PTR) );
            _CertFreeCertificateContext       = _bind( linker, crypt32, "CertFreeCertificateContext"      , FunctionDescriptor.of(DW , PTR) );
            _CertStrToNameW                   = _bind( linker, crypt32, "CertStrToNameW"                  , FunctionDescriptor.of(DW , DW, PTR, DW, PTR, PTR, PTR, PTR) );
            _CertCreateSelfSignCertificate    = _bind( linker, crypt32, "CertCreateSelfSignCertificate"   , FunctionDescriptor.of(PTR, PTR, PTR, DW, PTR, PTR, PTR, PTR, PTR) );
            _CertAddCertificateContextToStore = _bind( linker, crypt32, "CertAddCertificateContextToStore", FunctionDescriptor.of(DW , PTR, PTR, DW, PTR) );
            _CryptEncodeObjectEx              = _bind( linker, crypt32, "CryptEncodeObjectEx"             , FunctionDescriptor.of(DW , DW, PTR, PTR, DW, PTR, PTR, PTR) );
            _CryptSIPRetrieveSubjectGuid      = _bind( linker, crypt32, "CryptSIPRetrieveSubjectGuid"     , FunctionDescriptor.of(DW , PTR, PTR, PTR) );

            // Ncrypt.dll (ncrypt.h)
            _NCryptOpenStorageProvider = _bind( linker, ncrypt, "NCryptOpenStorageProvider", FunctionDescriptor.of(DW, PTR, PTR, DW) );
            _NCryptCreatePersistedKey  = _bind( linker, ncrypt, "NCryptCreatePersistedKey" , FunctionDescriptor.of(DW, PTR, PTR, PTR, PTR, DW, DW) );
            _NCryptSetProperty         = _bind( linker, ncrypt, "NCryptSetProperty"        , FunctionDescriptor.of(DW, PTR, PTR, PTR, DW, DW) );
            _NCryptFinalizeKey         = _bind( linker, ncrypt, "NCryptFinalizeKey"        , FunctionDescriptor.of(DW, PTR, DW) );
            _NCryptFreeObject          = _bind( linker, ncrypt, "NCryptFreeObject"         , FunctionDescriptor.of(DW, PTR) );

            // Wintrust.dll (mscat.h)
            _CryptCATAdminAcquireContext2         = _bind( linker, wintrust, "CryptCATAdminAcquireContext2"        , FunctionDescriptor.of(DW , PTR, PTR, PTR, PTR, DW) );
            _CryptCATAdminCalcHashFromFileHandle2 = _bind( linker, wintrust, "CryptCATAdminCalcHashFromFileHandle2", FunctionDescriptor.of(DW , PTR, PTR, PTR, PTR, DW) );
            _CryptCATAdminReleaseContext          = _bind( linker, wintrust, "CryptCATAdminReleaseContext"         , FunctionDescriptor.of(DW , PTR, DW) );
            _CryptCATOpen                         = _bind( linker, wintrust, "CryptCATOpen"                        , FunctionDescriptor.of(PTR, PTR, DW, PTR, DW, DW) );
            _CryptCATClose                        = _bind( linker, wintrust, "CryptCATClose"                       , FunctionDescriptor.of(DW , PTR) );
            _CryptCATPutMemberInfo                = _bind( linker, wintrust, "CryptCATPutMemberInfo"               , FunctionDescriptor.of(PTR, PTR, PTR, PTR, PTR, DW, DW, PTR) );
            _CryptCATPutAttrInfo                  = _bind( linker, wintrust, "CryptCATPutAttrInfo"                 , FunctionDescriptor.of(PTR, PTR, PTR, PTR, DW, DW, PTR) );

            // Mssign32.dll (not declared in any SDK header - see SignerSignEx2/SignerFreeSignerContext docs)
            _SignerSignEx2           = _bind( linker, mssign32, "SignerSignEx2", FunctionDescriptor.of(DW, DW, PTR, PTR, PTR, PTR, DW, PTR, PTR, PTR, PTR, PTR, PTR, PTR) );
            _SignerFreeSignerContext = _bind( linker, mssign32, "SignerFreeSignerContext", FunctionDescriptor.of(DW, PTR) );
        }
        catch(final Throwable t) {
            // Do not throw out of a static initializer with anything worse than what we capture here;
            // isUsable() below reports this failure through the normal (non-exceptional) return path
            _initError = t;
        }
    }

    private static MethodHandle _bind(final Linker linker, final SymbolLookup lib, final String name, final FunctionDescriptor fd)
    { return linker.downcallHandle( lib.find(name).orElseThrow( () -> new UnsatisfiedLinkError(name) ), fd ); }

    @Override
    public boolean isUsable()
    {
        try {
            if(_initError != null) return false;

            if( !System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows") ) return false;

            // Windows 10 reports os.version "10.0" (as does Windows 11 - Java does not distinguish them).
            // Anything below "10.0" (7 = 6.1, 8 = 6.2, 8.1 = 6.3) is rejected.
            final String[] parts = System.getProperty("os.version", "0.0").split("\\.");
            final int      major = Integer.parseInt(parts[0]);
            if(major < 10) return false;

            // Confirm the handles we actually rely on were resolved successfully
            return _CertOpenStore != null && _CryptCATOpen != null && _SignerSignEx2 != null && _NCryptOpenStorageProvider != null && _ShellExecuteExW != null;
        }
        catch(final Throwable ignored) {
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Allocates a NUL-terminated UTF-16LE (Windows WCHAR) string in the given arena
    private static MemorySegment _wstr(final Arena arena, final String s)
    {
        if(s == null) return MemorySegment.NULL;

        final byte[]        utf16 = s.getBytes(StandardCharsets.UTF_16LE);
        final MemorySegment seg   = arena.allocate(utf16.length + 2L, 2);

        MemorySegment.copy(utf16, 0, seg, ValueLayout.JAVA_BYTE, 0, utf16.length);
        seg.set(ValueLayout.JAVA_SHORT, utf16.length, (short) 0);

        return seg;
    }

    // Allocates a NUL-terminated single-byte (ANSI/UTF-8 - identical for the plain-ASCII OID
    // strings this class deals with) string in the given arena, for LPSTR-typed parameters
    private static MemorySegment _astr(final Arena arena, final String s)
    {
        final byte[]        ascii = s.getBytes(StandardCharsets.US_ASCII);
        final MemorySegment seg   = arena.allocate(ascii.length + 1L, 1);

        MemorySegment.copy(ascii, 0, seg, ValueLayout.JAVA_BYTE, 0, ascii.length);
        seg.set(ValueLayout.JAVA_BYTE, ascii.length, (byte) 0);

        return seg;
    }

    private static MemorySegment _handleOf(final long value)
    { return MemorySegment.ofAddress(value); }

    private static int _lastError() throws Throwable
    { return (int) _GetLastError.invoke(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // wincrypt.h : CERT_STORE_PROV_SYSTEM_W = 10 (passed as an integer cast to LPCSTR, not a real string)
    //              CERT_FIND_SUBJECT_STR_W  = ( CERT_COMPARE_NAME_STR_W(8) << CERT_COMPARE_SHIFT(16) ) | CERT_INFO_SUBJECT_FLAG(7)
    private static final long CERT_STORE_PROV_SYSTEM_W         = 10L;
    private static final int  CERT_SYSTEM_STORE_LOCAL_MACHINE  = 0x00020000;
    private static final int  CERT_SYSTEM_STORE_CURRENT_USER   = 0x00010000;
    private static final int  X509_ASN_ENCODING                = 0x00000001;
    private static final int  PKCS_7_ASN_ENCODING              = 0x00010000;
    private static final int  CRYPT_ASN_ENCODING               = X509_ASN_ENCODING | PKCS_7_ASN_ENCODING;
    private static final int  CERT_FIND_SUBJECT_STR_W          = (8 << 16) | 7;
    private static final int  CERT_STORE_ADD_REPLACE_EXISTING  = 3;

    // NOTE : isProviderAlreadyTrusted () is the only operation that does not require elevation

    @Override
    public XCom.Pair<Integer, String> isProviderAlreadyTrusted(final String providerName)
    {
        try(
            final Arena arena = Arena.ofConfined()
        ) {
            final MemorySegment subject = _wstr(arena, providerName);

            for( final String storeName : new String[]{ "Root", "TrustedPublisher" } ) {

                final MemorySegment hStore = (MemorySegment) _CertOpenStore.invoke(
                    _handleOf(CERT_STORE_PROV_SYSTEM_W), 0, MemorySegment.NULL, CERT_SYSTEM_STORE_LOCAL_MACHINE, _wstr(arena, storeName)
                );

                if(hStore == null || hStore.address() == 0L) continue;

                try {
                    final MemorySegment found = (MemorySegment) _CertFindCertificateInStore.invoke(
                        hStore, CRYPT_ASN_ENCODING, 0, CERT_FIND_SUBJECT_STR_W, subject, MemorySegment.NULL
                    );
                    if(found != null && found.address() != 0L) {
                        _CertFreeCertificateContext.invoke(found);
                        return new XCom.Pair<Integer, String>(1, ""); // Already trusted (matches WindowsDriverInstaller_PS1's "exit 1")
                    }
                }
                finally {
                    _CertCloseStore.invoke(hStore, 0);
                }

            } // for

            return new XCom.Pair<Integer, String>(RETCODE_OK, ""); // Not trusted yet

        }
        catch(final Throwable t) {
            if( XCom.enableAllExceptionStackTrace() ) t.printStackTrace();
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, t.toString() );
        }
    }

    @Override
    public XCom.Pair<Integer, String> installDriver(final String infPath)
    {
        //  Use pnputil.exe directly to elevate

        try {
            final Path path = Paths.get(infPath);

            if( !infPath.toLowerCase(Locale.ROOT).endsWith(".inf") || !path.isAbsolute() || !Files.exists(path) ) {
                return new XCom.Pair<Integer, String>( RETCODE_INVALID_PATH, String.format(Texts.EMsg_WDriverInstallInvInfPth, infPath) );
            }

            final Path logFile = Files.createTempFile("wdi_ffm_pnp_", ".log");
            try {
                // Windows 10+ always supports pnputil /add-driver /install (unlike Windows 7)
                final String params = String.format( "/c pnputil.exe /add-driver \"%s\" /install > \"%s\" 2>&1", infPath, logFile.toAbsolutePath() );

                return _shellExecuteElevatedAndWait("cmd.exe", params, System.getProperty("user.dir"), 5, logFile);
            }
            finally {
                try { Files.deleteIfExists(logFile); }
                catch(final Exception ignored) {}
            }
        }
        catch(final Throwable t) {
            if( XCom.enableAllExceptionStackTrace() ) t.printStackTrace();
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, t.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Elevation plumbing (ShellExecuteExW, shellapi.h)
    // SHELLEXECUTEINFOW (x64) field byte offsets - see shellapi.h. Struct size = 112, align = 8.

    private static final int SEE_MASK_NOCLOSEPROCESS = 0x00000040;
    private static final int SEE_MASK_NOASYNC        = 0x00000100;
    private static final int SW_HIDE                 = 0;
    private static final int WAIT_TIMEOUT            = 0x102;

    private static final int SEI_cbSize       =   0;
    private static final int SEI_fMask        =   4;
    private static final int SEI_hwnd         =   8;
    private static final int SEI_lpVerb       =  16;
    private static final int SEI_lpFile       =  24;
    private static final int SEI_lpParameters =  32;
    private static final int SEI_lpDirectory  =  40;
    private static final int SEI_nShow        =  48;
    private static final int SEI_hInstApp     =  56;
    private static final int SEI_lpIDList     =  64;
    private static final int SEI_lpClass      =  72;
    private static final int SEI_hkeyClass    =  80;
    private static final int SEI_dwHotKey     =  88;
    private static final int SEI_hIcon        =  96;
    private static final int SEI_hProcess     = 104;
    private static final int SEI_SIZE         = 112;

    /*
     * Launches 'file params' elevated (UAC prompt via lpVerb="runas"), waits up to waitTimeMinutes, then reads back
     * logFile (written by the elevated child itself/
     *
     * The stdout/stderr cannot be piped across the UAC elevation boundary) exactly like WindowsDriverInstaller_PS1's
     * temp-log convention.
     *
     * Hence, workDir is passed through as lpDirectory - ShellExecuteExW does NOT guarantee an elevated child otherwise
     * inherits the caller's current working directory; this matters whenever 'params' embeds a relative path, e.g.
     * a relative -cp entry when self-relaunching - see _runElevatedSelf() below.
     */
    private static XCom.Pair<Integer, String> _shellExecuteElevatedAndWait(final String file, final String params, final String workDir, final int waitTimeMinutes, final Path logFile) throws Throwable
    {
        try(
            final Arena arena = Arena.ofConfined()
        ) {
            final MemorySegment sei = arena.allocate(SEI_SIZE, 8);

            sei.set( ValueLayout.JAVA_INT, SEI_cbSize      , SEI_SIZE                                   );
            sei.set( ValueLayout.JAVA_INT, SEI_fMask       , SEE_MASK_NOCLOSEPROCESS | SEE_MASK_NOASYNC );
            sei.set( PTR                 , SEI_hwnd        , MemorySegment.NULL                         );
            sei.set( PTR                 , SEI_lpVerb      , _wstr(arena, "runas")                      );
            sei.set( PTR                 , SEI_lpFile      , _wstr(arena, file   )                      );
            sei.set( PTR                 , SEI_lpParameters, _wstr(arena, params )                      );
            sei.set( PTR                 , SEI_lpDirectory , _wstr(arena, workDir)                      );
            sei.set( ValueLayout.JAVA_INT, SEI_nShow       , SW_HIDE                                    );
            sei.set( PTR                 , SEI_hInstApp    , MemorySegment.NULL                         );
            sei.set( PTR                 , SEI_lpIDList    , MemorySegment.NULL                         );
            sei.set( PTR                 , SEI_lpClass     , MemorySegment.NULL                         );
            sei.set( PTR                 , SEI_hkeyClass   , MemorySegment.NULL                         );
            sei.set( ValueLayout.JAVA_INT, SEI_dwHotKey    , 0                                          );
            sei.set( PTR                 , SEI_hIcon       , MemorySegment.NULL                         );
            sei.set( PTR                 , SEI_hProcess    , MemorySegment.NULL                         );

            final int ok = (int) _ShellExecuteExW.invoke(sei);
            if(ok == 0) {
                final int err = _lastError();
                // ERROR_CANCELLED (1223) : user declined the UAC prompt
                return new XCom.Pair<Integer, String>( err == 1223 ? RETCODE_UAC_DECLINED : RETCODE_PH_NULL, "ShellExecuteExW failed, GetLastError=" + err );
            }

            final MemorySegment hProcess = sei.get(PTR, SEI_hProcess);
            if(hProcess == null || hProcess.address() == 0L) {
                return new XCom.Pair<Integer, String>( RETCODE_PH_NULL, "ShellExecuteExW returned no process handle" );
            }

            try {
                final int waitResult = (int) _WaitForSingleObject.invoke(hProcess, waitTimeMinutes * 60_000);
                if(waitResult == WAIT_TIMEOUT) {
                    _TerminateProcess.invoke(hProcess, 1);
                    return new XCom.Pair<Integer, String>( RETCODE_TIMEOUT, String.format(Texts.EMsg_WDriverInstallTimeoutMN, waitTimeMinutes) );
                }

                final MemorySegment exitCodeOut = arena.allocate(ValueLayout.JAVA_INT);
                _GetExitCodeProcess.invoke(hProcess, exitCodeOut);
                final int exitCode = exitCodeOut.get(ValueLayout.JAVA_INT, 0);

                String log = "";
                try {
                    if( Files.exists(logFile) ) log = Files.readString(logFile, StandardCharsets.UTF_8).trim();
                }
                catch(final IOException ignored) {}

                return new XCom.Pair<Integer, String>(exitCode, log);
            }
            finally {
                _CloseHandle.invoke(hProcess);
            }
        }
    }

    /*
     * Relaunches this JVM (same classpath) elevated, running main() below in "elevated op" mode.
     *
     * Works regardless of how this process itself was launched (exploded class files with a relative "." classpath entry,
     * a jar referenced via -cp <name> <mainClass>, or a jar referenced via -jar <name>) - java.class.path reports exactly
     * what was resolved in each case, which is commonly a path relative to the parent's working directory.
     *
     * Since an elevated child's default working directory is not guaranteed to match the parent's even when lpDirectory is
     * set correctly on every Windows version, every classpath entry is resolved to an absolute path up front instead of
     * relying on that alone.
     */
    private XCom.Pair<Integer, String> _runElevatedSelf(final String op, final String[] extraArgs, final int waitTimeMinutes) throws Throwable
    {
        final Path logFile = Files.createTempFile("wdi_ffm_" + op + "_", ".log");
        try {
            final String javaExe = _findJavaLauncher();
            final String cp      = _absoluteClassPath();

            final StringBuilder params = new StringBuilder();

            params.append("-cp \"").append(cp).append("\" jxm.WindowsDriverInstaller_FFM ")
                  .append(ELEVATED_OP_ARG).append(' ').append(op).append(" \"").append(logFile.toAbsolutePath() ).append('"');

            for(final String a : extraArgs) params.append(" \"").append(a).append('"');

            return _shellExecuteElevatedAndWait(javaExe, params.toString(), System.getProperty("user.dir"), waitTimeMinutes, logFile);
        }
        finally {
            try { Files.deleteIfExists(logFile); } catch(final Exception ignored) {}
        }
    }

    private static String _findJavaLauncher()
    {
        final String   javaHome = System.getProperty("java.home");
        final Path     bin      = Paths.get(javaHome, "bin");
        final Path     javaw    = bin.resolve("javaw.exe");

        return ( Files.exists(javaw) ? javaw : bin.resolve("java.exe") ).toAbsolutePath().toString();
    }

    // Resolves every entry of java.class.path against this process's current working directory, regardless of whether
    // this process was itself launched from exploded class files ("-cp .[..]"), a jar via "-cp somejar.jar SomeClass",
    // or a jar via "-jar somejar.jar" - see _runElevatedSelf()
    private static String _absoluteClassPath()
    {
        final String        cp     = System.getProperty("java.class.path", ".");
        final StringBuilder result = new StringBuilder();

        for( final String entry : cp.split(Pattern.quote(File.pathSeparator), -1) ) {
            if( result.length() > 0 ) result.append(File.pathSeparatorChar);
            result.append( entry.isEmpty() ? "." : Paths.get(entry).toAbsolutePath().normalize().toString() );
        }

        return result.toString();
    }

    @Override
    public XCom.Pair<Integer, String> createAndTrustProvider(final String providerName)
    {
        // Dispatch to the elevated child

        try {
            return _runElevatedSelf("trust", new String[]{ providerName }, 5);
        }
        catch(final Throwable t) {
            if( XCom.enableAllExceptionStackTrace() ) t.printStackTrace();
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, t.toString() );
        }
    }

    @Override
    public XCom.Pair<Integer, String> createAndSignCatalog(final String infPath, final String providerName)
    {
        // Dispatch to the elevated child

        try {
            return _runElevatedSelf("catalog", new String[]{ infPath, providerName }, 5);
        }
        catch(final Throwable t) {
            if( XCom.enableAllExceptionStackTrace() ) t.printStackTrace();
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, t.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void main(final String[] args)
    {
        //  Elevated child entry point - launched via _runElevatedSelf() above, never called directly

        final StringBuilder log = new StringBuilder();

        if(args.length < 3 || !ELEVATED_OP_ARG.equals(args[0]) ) {
            System.exit(RETCODE_INVALID_PATH);
            return;
        }

        int exitCode;

        try {
            switch(args[1]) {
                case "trust"   -> exitCode = _elevatedCreateAndTrustProvider(args[3], log);
                case "catalog" -> exitCode = _elevatedCreateAndSignCatalog(args[3], args[4], log);
                default        -> { log.append("Unknown elevated op: ").append(args[1]); exitCode = RETCODE_EXCEPTION; }
            }
        }
        catch(final Throwable t) {
            log.append(t);
            exitCode = RETCODE_EXCEPTION;
        }

        try {
            Files.writeString( Paths.get(args[2]), log.toString(), StandardCharsets.UTF_8 );
        }
        catch(final IOException ignored) {}

        System.exit(exitCode);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ncrypt.h
    private static final String MS_KEY_STORAGE_PROVIDER   = "Microsoft Software Key Storage Provider";
    private static final String BCRYPT_RSA_ALGORITHM      = "RSA";
    private static final int    NCRYPT_ALLOW_SIGNING_FLAG = 0x00000002;
    private static final int    AT_SIGNATURE              = 2;

    // wincrypt.h
    // X509_ENHANCED_KEY_USAGE is a small-integer "predefined" lpszStructType, cast to LPCSTR - not a real string pointer
    private static final int    CERT_X500_NAME_STR         = 3;
    private static final int    CRYPT_ENCODE_ALLOC_FLAG    = 0x8000;
    private static final long   X509_ENHANCED_KEY_USAGE    = 36L;
    private static final String szOID_ENHANCED_KEY_USAGE   = "2.5.29.37";
    private static final String szOID_PKIX_KP_CODE_SIGNING = "1.3.6.1.5.5.7.3.3";

    // CRYPT_ATTR_BLOB / CERT_NAME_BLOB (wincrypt.h) : { DWORD cbData; BYTE *pbData; } - size 16, align 8
    private static MemorySegment _blob(final Arena arena, final int cbData, final MemorySegment pbData)
    {
        final MemorySegment b = arena.allocate(16, 8);
        b.set(ValueLayout.JAVA_INT, 0, cbData);
        b.set(PTR                 , 8, pbData);
        return b;
    }

    // Opens a system certificate store (CERT_STORE_PROV_SYSTEM_W) - see wincrypt.h
    private static MemorySegment _certStoreOpen(final Arena arena, final int locationFlag, final String storeName) throws Throwable
    {
        final MemorySegment h = (MemorySegment) _CertOpenStore.invoke(
            _handleOf(CERT_STORE_PROV_SYSTEM_W), 0, MemorySegment.NULL, locationFlag, _wstr(arena, storeName)
        );
        return (h == null || h.address() == 0L) ? null : h;
    }

    private static int _elevatedCreateAndTrustProvider(final String providerName, final StringBuilder log) throws Throwable
    {
        try(
            final Arena arena = Arena.ofConfined()
        ) {
            // 1. Open the CNG key storage provider and create a persisted 2048-bit RSA signing key
            final MemorySegment hProviderOut = arena.allocate(PTR);
            if( (int) _NCryptOpenStorageProvider.invoke( hProviderOut, _wstr(arena, MS_KEY_STORAGE_PROVIDER), 0 ) != 0 ) {
                log.append("NCryptOpenStorageProvider failed");
                return RETCODE_EXCEPTION;
            }
            final MemorySegment hProvider = hProviderOut.get(PTR, 0);

            final String        containerName = providerName + "_" + Long.toHexString( new SecureRandom().nextLong() );
            final MemorySegment hKeyOut       = arena.allocate(PTR);
            if( (int) _NCryptCreatePersistedKey.invoke( hProvider, hKeyOut, _wstr(arena, BCRYPT_RSA_ALGORITHM), _wstr(arena, containerName), AT_SIGNATURE, 0 ) != 0 ) {
                log.append("NCryptCreatePersistedKey failed");
                _NCryptFreeObject.invoke(hProvider);
                return RETCODE_EXCEPTION;
            }
            final MemorySegment hKey = hKeyOut.get(PTR, 0);

            final MemorySegment keyLen = arena.allocate(ValueLayout.JAVA_INT);
            keyLen.set(ValueLayout.JAVA_INT, 0, 2048);
            _NCryptSetProperty.invoke( hKey, _wstr(arena, "Length"), keyLen, 4, 0 );

            final MemorySegment keyUsage = arena.allocate(ValueLayout.JAVA_INT);
            keyUsage.set(ValueLayout.JAVA_INT, 0, NCRYPT_ALLOW_SIGNING_FLAG);
            _NCryptSetProperty.invoke( hKey, _wstr(arena, "Key Usage"), keyUsage, 4, 0 );

            if( (int) _NCryptFinalizeKey.invoke(hKey, 0) != 0 ) {
                log.append("NCryptFinalizeKey failed");
                _NCryptFreeObject.invoke(hKey);
                _NCryptFreeObject.invoke(hProvider);
                return RETCODE_EXCEPTION;
            }

            // 2. Encode the "CN=<providerName>" subject name (CertStrToNameW, two-call size/fill pattern)
            final MemorySegment subjectStr = _wstr(arena, "CN=" + providerName);
            final MemorySegment cbSubject  = arena.allocate(ValueLayout.JAVA_INT);
            if( (int) _CertStrToNameW.invoke(X509_ASN_ENCODING, subjectStr, CERT_X500_NAME_STR, MemorySegment.NULL, MemorySegment.NULL, cbSubject, MemorySegment.NULL) == 0 ) {
                log.append("CertStrToNameW (sizing) failed, GetLastError=").append( _lastError() );
                return RETCODE_EXCEPTION;
            }

            final int            subjectLen = cbSubject.get(ValueLayout.JAVA_INT, 0);
            final MemorySegment  subjectBuf = arena.allocate(subjectLen);
            if( (int) _CertStrToNameW.invoke(X509_ASN_ENCODING, subjectStr, CERT_X500_NAME_STR, MemorySegment.NULL, subjectBuf, cbSubject, MemorySegment.NULL) == 0 ) {
                log.append("CertStrToNameW failed, GetLastError=").append( _lastError() );
                return RETCODE_EXCEPTION;
            }
            final MemorySegment subjectBlob = _blob(arena, subjectLen, subjectBuf);

            // 3. Build the "Code Signing" Enhanced Key Usage extension (CryptEncodeObjectEx w/ CRYPT_ENCODE_ALLOC_FLAG)
            final MemorySegment oidStr    = _astr(arena, szOID_PKIX_KP_CODE_SIGNING);
            final MemorySegment oidArray  = arena.allocate(PTR);
            oidArray.set(PTR, 0, oidStr);

            // CERT_ENHKEY_USAGE : { DWORD cUsageIdentifier; LPSTR *rgpszUsageIdentifier; } - size 16, align 8
            final MemorySegment ekuStruct = arena.allocate(16, 8);
            ekuStruct.set(ValueLayout.JAVA_INT, 0, 1       );
            ekuStruct.set(PTR                 , 8, oidArray);

            final MemorySegment ekuEncodedPtrOut = arena.allocate(PTR);
            final MemorySegment ekuEncodedLenOut = arena.allocate(ValueLayout.JAVA_INT);
            if( (int) _CryptEncodeObjectEx.invoke( X509_ASN_ENCODING, _handleOf(X509_ENHANCED_KEY_USAGE), ekuStruct, CRYPT_ENCODE_ALLOC_FLAG, MemorySegment.NULL, ekuEncodedPtrOut, ekuEncodedLenOut ) == 0 ) {
                log.append("CryptEncodeObjectEx(EKU) failed, GetLastError=").append( _lastError() );
                return RETCODE_EXCEPTION;
            }
            final MemorySegment ekuEncoded    = ekuEncodedPtrOut.get(PTR, 0);
            final int           ekuEncodedLen = ekuEncodedLenOut.get(ValueLayout.JAVA_INT, 0);

            // CERT_EXTENSION : { LPSTR pszObjId; BOOL fCritical; CRYPT_ATTR_BLOB Value; } - size 32, align 8
            final MemorySegment extension = arena.allocate(32, 8);
            extension.set( PTR                 , 0 , _astr(arena, szOID_ENHANCED_KEY_USAGE) );
            extension.set( ValueLayout.JAVA_INT, 8 , 0                                      );
            extension.set( ValueLayout.JAVA_INT, 16, ekuEncodedLen                          );
            extension.set( PTR                 , 24, ekuEncoded                             );

            // CERT_EXTENSIONS : { DWORD cExtension; PCERT_EXTENSION rgExtension; } - size 16, align 8
            final MemorySegment extensions = arena.allocate(16, 8);
            extensions.set(ValueLayout.JAVA_INT, 0, 1        );
            extensions.set(PTR                 , 8, extension);

            // 4. CRYPT_KEY_PROV_INFO - links the returned cert context back to the persisted CNG key
            //    { LPWSTR pwszContainerName; LPWSTR pwszProvName; DWORD dwProvType; DWORD dwFlags;
            //      DWORD cProvParam; PVOID rgProvParam; DWORD dwKeySpec; } - size 48, align 8
            final MemorySegment keyProvInfo = arena.allocate(48, 8);
            keyProvInfo.set( PTR                 ,  0, _wstr(arena, containerName          ) );
            keyProvInfo.set( PTR                 ,  8, _wstr(arena, MS_KEY_STORAGE_PROVIDER) );
            keyProvInfo.set( ValueLayout.JAVA_INT, 16, 0                                     );
            keyProvInfo.set( ValueLayout.JAVA_INT, 20, 0                                     );
            keyProvInfo.set( ValueLayout.JAVA_INT, 24, 0                                     );
            keyProvInfo.set( PTR                 , 32, MemorySegment.NULL                    );
            keyProvInfo.set( ValueLayout.JAVA_INT, 40, AT_SIGNATURE                          );

            // 5. Create the self-signed certificate (default SHA256RSA signature algorithm, 1 year validity)
            final MemorySegment certCtx = (MemorySegment) _CertCreateSelfSignCertificate.invoke(
                hKey, subjectBlob, 0, keyProvInfo, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL, extensions
            );
            if( certCtx == null || certCtx.address() == 0L ) {
                log.append("CertCreateSelfSignCertificate failed, GetLastError=").append( _lastError() );
                _NCryptFreeObject.invoke(hKey);
                _NCryptFreeObject.invoke(hProvider);
                return RETCODE_EXCEPTION;
            }

            try {
                // 6. Add the cert (with its private-key linkage) to CurrentUser\My
                final MemorySegment hMy = _certStoreOpen(arena, CERT_SYSTEM_STORE_CURRENT_USER, "My");
                if(hMy != null) {
                    _CertAddCertificateContextToStore.invoke(hMy, certCtx, CERT_STORE_ADD_REPLACE_EXISTING, MemorySegment.NULL);
                    _CertCloseStore.invoke(hMy, 0);
                }

                // 7. Export the public cert (DER) to a temp file, so certutil-equivalent stores can be populated
                //    CERT_CONTEXT : { DWORD dwCertEncodingType; BYTE *pbCertEncoded; DWORD cbCertEncoded;
                //                     PCERT_INFO pCertInfo; HCERTSTORE hCertStore; } - size 40, align 8
                final MemorySegment certCtxView   = certCtx.reinterpret(40);
                final MemorySegment pbCertEncoded = certCtxView.get(PTR                 , 8);
                final int           cbCertEncoded = certCtxView.get(ValueLayout.JAVA_INT, 16);
                final byte[]        derBytes      = pbCertEncoded.reinterpret(cbCertEncoded).toArray(ValueLayout.JAVA_BYTE);

                // 8. Add the (public-only) cert to LocalMachine Root and TrustedPublisher
                for(final String storeName : new String[]{ "Root", "TrustedPublisher" }) {

                    final MemorySegment hStore = _certStoreOpen(arena, CERT_SYSTEM_STORE_LOCAL_MACHINE, storeName);

                    if(hStore == null) {
                        log.append("Could not open LocalMachine\\").append(storeName).append(", GetLastError=").append( _lastError() ).append('\n');
                        continue;
                    }

                    if( (int) _CertAddCertificateContextToStore.invoke(hStore, certCtx, CERT_STORE_ADD_REPLACE_EXISTING, MemorySegment.NULL) == 0 ) {
                        log.append("CertAddCertificateContextToStore(").append(storeName).append(") failed, GetLastError=").append( _lastError() ).append('\n');
                    }
                    _CertCloseStore.invoke(hStore, 0);

                } // for

                if(derBytes.length == 0) log.append("Warning: exported certificate was empty\n");

                return RETCODE_OK;
            }
            finally {
                _CertFreeCertificateContext.invoke(certCtx);
                _LocalFree.invoke(ekuEncoded);
                _NCryptFreeObject.invoke(hKey);
                _NCryptFreeObject.invoke(hProvider);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Elevated op : create a v2 catalog for the INF and Authenticode-sign it with the trusted cert

    // mscat.h
    // DRIVER_ACTION_VERIFY (softpub.h) - the SIP subsystem used for driver catalog validation
    private static final int   CRYPTCAT_OPEN_CREATENEW     = 0x00000001;
    private static final int   CRYPTCAT_VERSION_2          = 0x00000200;
    private static final int   CRYPTCAT_ATTR_AUTHENTICATED = 0x10000000;
    private static final long[] DRIVER_ACTION_VERIFY_PARTS = { 0xF750E6C3L, 0x38EEL, 0x11d1L, 0x85L, 0xE5L, 0x00L, 0xC0L, 0x4FL, 0xC2L, 0x95L, 0xEEL };

    // GUID (guiddef.h) : { DWORD Data1; WORD Data2; WORD Data3; BYTE Data4[8]; } - size 16, align 4
    private static MemorySegment _guid(final Arena arena, final long[] parts)
    {
        final MemorySegment g = arena.allocate(16, 4);

        g.set( ValueLayout.JAVA_INT  , 0, (int  ) parts[0] );
        g.set( ValueLayout.JAVA_SHORT, 4, (short) parts[1] );
        g.set( ValueLayout.JAVA_SHORT, 6, (short) parts[2] );
        for(int i = 0; i < 8; ++i) g.set( ValueLayout.JAVA_BYTE, 8 + i, (byte) parts[3 + i] );

        return g;
    }

    // kernel32.dll (fileapi.h)
    private static final int  GENERIC_READ          = 0x80000000;
    private static final int  FILE_SHARE_READ       = 0x00000001;
    private static final int  OPEN_EXISTING         = 3;
    private static final int  FILE_ATTRIBUTE_NORMAL = 0x00000080;
    private static final long INVALID_HANDLE_VALUE  = -1L;

    private static int _elevatedCreateAndSignCatalog(final String infPath, final String providerName, final StringBuilder log) throws Throwable
    {
        try(
            final Arena arena = Arena.ofConfined()
        ) {
            final String catPath  = infPath.substring( 0, infPath.lastIndexOf('.') ) + ".cat";
            final String fileName = Paths.get(infPath).getFileName().toString();

            // 1. Open a catalog-admin context and hash the INF (SHA-256, matching WindowsDriverInstaller_PS1's New-FileCatalog default)
            final MemorySegment hCatAdminOut = arena.allocate(PTR);
            if( (int) _CryptCATAdminAcquireContext2.invoke(hCatAdminOut, _guid(arena, DRIVER_ACTION_VERIFY_PARTS), _wstr(arena, "SHA256"), MemorySegment.NULL, 0) == 0 ) {
                log.append("CryptCATAdminAcquireContext2 failed, GetLastError=").append( _lastError() );
                return RETCODE_EXCEPTION;
            }
            final MemorySegment hCatAdmin = hCatAdminOut.get(PTR, 0);

            final MemorySegment hInfFile = (MemorySegment) _CreateFileW.invoke(
                _wstr(arena, infPath), GENERIC_READ, FILE_SHARE_READ, MemorySegment.NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, MemorySegment.NULL
            );
            if(hInfFile == null || hInfFile.address() == INVALID_HANDLE_VALUE) {
                log.append("CreateFileW(inf) failed, GetLastError=").append( _lastError() );
                _CryptCATAdminReleaseContext.invoke(hCatAdmin, 0);
                return RETCODE_EXCEPTION;
            }

            try {
                final MemorySegment cbHashOut = arena.allocate(ValueLayout.JAVA_INT);
                _CryptCATAdminCalcHashFromFileHandle2.invoke(hCatAdmin, hInfFile, cbHashOut, MemorySegment.NULL, 0);

                final int cbHash = cbHashOut.get(ValueLayout.JAVA_INT, 0);
                if(cbHash <= 0) {
                    log.append("CryptCATAdminCalcHashFromFileHandle2 (sizing) failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }

                final MemorySegment hashBuf = arena.allocate(cbHash);
                if( (int) _CryptCATAdminCalcHashFromFileHandle2.invoke(hCatAdmin, hInfFile, cbHashOut, hashBuf, 0) == 0 ) {
                    log.append("CryptCATAdminCalcHashFromFileHandle2 failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }

                final byte[]       hashBytes = hashBuf.toArray(ValueLayout.JAVA_BYTE);
                final StringBuilder hexTag   = new StringBuilder(hashBytes.length * 2);
                for(final byte b : hashBytes) hexTag.append( String.format("%02X", b) );

                // 2. Determine the SIP subject GUID for this file - fall back to DRIVER_ACTION_VERIFY on failure
                final MemorySegment subjectGuid = arena.allocate(16, 4);
                if( (int) _CryptSIPRetrieveSubjectGuid.invoke(_wstr(arena, infPath), hInfFile, subjectGuid) == 0 ) {
                    MemorySegment.copy(_guid(arena, DRIVER_ACTION_VERIFY_PARTS), 0, subjectGuid, 0, 16);
                }

                // 3. Create the v2 catalog and add a single hash-only member for the INF
                final MemorySegment hCatalog = (MemorySegment) _CryptCATOpen.invoke(
                    _wstr(arena, catPath), CRYPTCAT_OPEN_CREATENEW, MemorySegment.NULL, CRYPTCAT_VERSION_2, 0
                );
                if(hCatalog == null || hCatalog.address() == 0L || hCatalog.address() == INVALID_HANDLE_VALUE) {
                    log.append("CryptCATOpen failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }

                try {
                    final MemorySegment pMember = (MemorySegment) _CryptCATPutMemberInfo.invoke(
                        hCatalog, _wstr(arena, fileName), _wstr(arena, hexTag.toString() ), subjectGuid, 0, 0, MemorySegment.NULL
                    );
                    if(pMember == null || pMember.address() == 0L) {
                        log.append("CryptCATPutMemberInfo failed, GetLastError=").append( _lastError() );
                        return RETCODE_EXCEPTION;
                    }

                    final MemorySegment attr = (MemorySegment) _CryptCATPutAttrInfo.invoke(
                        hCatalog, pMember, _wstr(arena, "HASH"), CRYPTCAT_ATTR_AUTHENTICATED, cbHash, hashBuf
                    );
                    if(attr == null || attr.address() == 0L) {
                        log.append("CryptCATPutAttrInfo failed, GetLastError=").append( _lastError() );
                        return RETCODE_EXCEPTION;
                    }
                }
                finally {
                    _CryptCATClose.invoke(hCatalog);
                }
            }
            finally {
                _CloseHandle.invoke(hInfFile);
                _CryptCATAdminReleaseContext.invoke(hCatAdmin, 0);
            }

            // 4. Locate the trusted signing cert (with its persisted private key) in CurrentUser\My
            final MemorySegment hMy = _certStoreOpen(arena, CERT_SYSTEM_STORE_CURRENT_USER, "My");
            if(hMy == null) {
                log.append("Could not open CurrentUser\\My, GetLastError=").append( _lastError() );
                return RETCODE_EXCEPTION;
            }

            try {
                final MemorySegment signingCert = (MemorySegment) _CertFindCertificateInStore.invoke(
                    hMy, CRYPT_ASN_ENCODING, 0, CERT_FIND_SUBJECT_STR_W, _wstr(arena, providerName), MemorySegment.NULL
                );
                if(signingCert == null || signingCert.address() == 0L) {
                    log.append("Signing certificate not found in CurrentUser\\My");
                    return RETCODE_EXCEPTION;
                }

                try {
                    return _signCatalog(arena, catPath, signingCert, hMy, log);
                }
                finally {
                    _CertFreeCertificateContext.invoke(signingCert);
                }
            }
            finally {
                _CertCloseStore.invoke(hMy, 0);
            }
        }
    }

    // mssign32.dll (not declared in any SDK header - struct layouts per the SIGNER_* documentation pages)
    private static final int SIGNER_SUBJECT_FILE       = 1;
    private static final int SIGNER_CERT_STORE         = 2;
    private static final int SIGNER_CERT_POLICY_STORE  = 0x1;
    private static final int SIGNER_NO_ATTR            = 0;
    private static final int CALG_SHA_256              = 0x0000800c;

    private static int _signCatalog(final Arena arena, final String catPath, final MemorySegment signingCert, final MemorySegment hMy, final StringBuilder log) throws Throwable
    {
        // SIGNER_FILE_INFO : { DWORD cbSize; LPCWSTR pwszFileName; HANDLE hFile; } - size 24, align 8
        final MemorySegment fileInfo = arena.allocate(24, 8);
        fileInfo.set( ValueLayout.JAVA_INT,  0, 24                    );
        fileInfo.set( PTR                 ,  8, _wstr(arena, catPath) );
        fileInfo.set( PTR                 , 16, MemorySegment.NULL    );

        // SIGNER_SUBJECT_INFO : { DWORD cbSize; DWORD *pdwIndex; DWORD dwSubjectChoice; union{ SIGNER_FILE_INFO* }; } - size 32, align 8
        final MemorySegment indexZero = arena.allocate(ValueLayout.JAVA_INT);
        indexZero.set(ValueLayout.JAVA_INT, 0, 0);

        final MemorySegment subjectInfo = arena.allocate(32, 8);
        subjectInfo.set(ValueLayout.JAVA_INT,  0, 32                 );
        subjectInfo.set( PTR                 ,  8, indexZero         );
        subjectInfo.set(ValueLayout.JAVA_INT, 16, SIGNER_SUBJECT_FILE);
        subjectInfo.set(PTR                 , 24, fileInfo           );

        // SIGNER_CERT_STORE_INFO : { DWORD cbSize; PCCERT_CONTEXT pSigningCert; DWORD dwCertPolicy; HCERTSTORE hCertStore; } - size 32, align 8
        final MemorySegment certStoreInfo = arena.allocate(32, 8);
        certStoreInfo.set(ValueLayout.JAVA_INT,  0, 32                      );
        certStoreInfo.set(PTR                 ,  8, signingCert             );
        certStoreInfo.set(ValueLayout.JAVA_INT, 16, SIGNER_CERT_POLICY_STORE);
        certStoreInfo.set(PTR                 , 24, hMy                     );

        // SIGNER_CERT : { DWORD cbSize; DWORD dwCertChoice; union{...}; HWND hwnd; } - size 24, align 8
        final MemorySegment signerCert = arena.allocate(24, 8);
        signerCert.set(ValueLayout.JAVA_INT,  0, 24                );
        signerCert.set(ValueLayout.JAVA_INT,  4, SIGNER_CERT_STORE );
        signerCert.set(PTR                 ,  8, certStoreInfo     );
        signerCert.set(PTR                 , 16, MemorySegment.NULL);

        // SIGNER_SIGNATURE_INFO : { DWORD cbSize; ALG_ID algidHash; DWORD dwAttrChoice; union{...};
        //                           PCRYPT_ATTRIBUTES psAuthenticated; PCRYPT_ATTRIBUTES psUnauthenticated; } - size 40, align 8
        final MemorySegment sigInfo = arena.allocate(40, 8);
        sigInfo.set(ValueLayout.JAVA_INT,  0, 40                );
        sigInfo.set(ValueLayout.JAVA_INT,  4, CALG_SHA_256      );
        sigInfo.set(ValueLayout.JAVA_INT,  8, SIGNER_NO_ATTR    );
        sigInfo.set(PTR                 , 16, MemorySegment.NULL);
        sigInfo.set(PTR                 , 24, MemorySegment.NULL);
        sigInfo.set(PTR                 , 32, MemorySegment.NULL);

        final MemorySegment ppSignerContext = arena.allocate(PTR);

        final int hr = (int) _SignerSignEx2.invoke(
            0, subjectInfo, signerCert, sigInfo, MemorySegment.NULL,
            0, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL,
            ppSignerContext, MemorySegment.NULL, MemorySegment.NULL
        );

        final MemorySegment signerContext = ppSignerContext.get(PTR, 0);
        if(signerContext != null && signerContext.address() != 0L) _SignerFreeSignerContext.invoke(signerContext);

        if(hr != 0) {
            log.append("SignerSignEx2 failed, HRESULT=0x").append( Integer.toHexString(hr) );
            return RETCODE_EXCEPTION;
        }

        return RETCODE_OK;
    }

} // WindowsDriverInstaller_FFM
