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
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;

import java.lang.invoke.MethodHandle;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.SecureRandom;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.HexFormat;
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
    private static MethodHandle _CertGetCertificateContextProperty;
    private static MethodHandle _CryptEncodeObjectEx;
    private static MethodHandle _CryptExportPublicKeyInfo;
    private static MethodHandle _CryptHashPublicKeyInfo;
  //private static MethodHandle _CryptSIPRetrieveSubjectGuid; // Unused - catalog members use a fixed INF-type subject GUID
    private static MethodHandle _CertDeleteCertificateFromStore;
    private static MethodHandle _LocalFree;

    private static MethodHandle _CryptAcquireContextW;
    private static MethodHandle _CryptGenKey;
    private static MethodHandle _CryptDestroyKey;
    private static MethodHandle _CryptReleaseContext;

    private static MethodHandle _CryptCATAdminAcquireContext2;
    private static MethodHandle _CryptCATAdminCalcHashFromFileHandle2;
    private static MethodHandle _CryptCATAdminReleaseContext;
    private static MethodHandle _CryptCATAdminAddCatalog;
    private static MethodHandle _CryptCATAdminReleaseCatalogContext;
    private static MethodHandle _CryptCATAdminEnumCatalogFromHash; // Diagnostic-only; see _diagCatalogLookup()
    private static MethodHandle _CryptCATCatalogInfoFromContext;   // Diagnostic-only; see _diagCatalogLookup()
    private static MethodHandle _WinVerifyTrust;                   // Diagnostic-only; see _diagVerifyTrustCatalog()
    private static MethodHandle _CryptCATOpen;
    private static MethodHandle _CryptCATClose;
    private static MethodHandle _CryptCATPersistStore;
    private static MethodHandle _CryptCATPutMemberInfo;
    private static MethodHandle _CryptCATPutAttrInfo;

    private static MethodHandle _SignerSignEx;
    private static MethodHandle _SignerFreeSignerContext;

    private static MethodHandle _SetupCopyOEMInfW;
    private static MethodHandle _SetupSetNonInteractiveMode;
    private static MethodHandle _CM_WaitNoPendingInstallEvents;

    static {
        try {
            _arena = Arena.ofShared();

            final Linker       linker   = Linker.nativeLinker();
            final SymbolLookup kernel32 = SymbolLookup.libraryLookup("Kernel32.dll", _arena);
            final SymbolLookup shell32  = SymbolLookup.libraryLookup("Shell32.dll" , _arena);
            final SymbolLookup crypt32  = SymbolLookup.libraryLookup("Crypt32.dll" , _arena);
            final SymbolLookup kernelB  = SymbolLookup.libraryLookup("Kernel32.dll", _arena);
            final SymbolLookup wintrust = SymbolLookup.libraryLookup("Wintrust.dll", _arena);
            final SymbolLookup mssign32 = SymbolLookup.libraryLookup("Mssign32.dll", _arena);
            final SymbolLookup advapi32 = SymbolLookup.libraryLookup("Advapi32.dll", _arena);
            final SymbolLookup setupapi = SymbolLookup.libraryLookup("Setupapi.dll", _arena);
            final SymbolLookup cfgmgr32 = SymbolLookup.libraryLookup("Cfgmgr32.dll", _arena);

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
            _CertOpenStore                     = _bind( linker, crypt32, "CertOpenStore"                    , FunctionDescriptor.of(PTR, PTR, DW, PTR, DW, PTR) );
            _CertCloseStore                    = _bind( linker, crypt32, "CertCloseStore"                   , FunctionDescriptor.of(DW , PTR, DW) );
            _CertFindCertificateInStore        = _bind( linker, crypt32, "CertFindCertificateInStore"       , FunctionDescriptor.of(PTR, PTR, DW, DW, DW, PTR, PTR) );
            _CertFreeCertificateContext        = _bind( linker, crypt32, "CertFreeCertificateContext"       , FunctionDescriptor.of(DW , PTR) );
            _CertStrToNameW                    = _bind( linker, crypt32, "CertStrToNameW"                   , FunctionDescriptor.of(DW , DW, PTR, DW, PTR, PTR, PTR, PTR) );
            _CertCreateSelfSignCertificate     = _bind( linker, crypt32, "CertCreateSelfSignCertificate"    , FunctionDescriptor.of(PTR, PTR, PTR, DW, PTR, PTR, PTR, PTR, PTR) );
            _CertAddCertificateContextToStore  = _bind( linker, crypt32, "CertAddCertificateContextToStore" , FunctionDescriptor.of(DW , PTR, PTR, DW, PTR) );
            _CertGetCertificateContextProperty = _bind( linker, crypt32, "CertGetCertificateContextProperty", FunctionDescriptor.of(DW , PTR, DW, PTR, PTR) );
            _CryptEncodeObjectEx               = _bind( linker, crypt32, "CryptEncodeObjectEx"              , FunctionDescriptor.of(DW , DW, PTR, PTR, DW, PTR, PTR, PTR) );
            _CryptExportPublicKeyInfo          = _bind( linker, crypt32, "CryptExportPublicKeyInfo"         , FunctionDescriptor.of(DW , PTR, DW, DW, PTR, PTR) );
            _CryptHashPublicKeyInfo            = _bind( linker, crypt32, "CryptHashPublicKeyInfo"           , FunctionDescriptor.of(DW , PTR, DW, DW, DW, PTR, PTR, PTR) );
          //_CryptSIPRetrieveSubjectGuid       = _bind( linker, crypt32, "CryptSIPRetrieveSubjectGuid"      , FunctionDescriptor.of(DW , PTR, PTR, PTR) );
            _CertDeleteCertificateFromStore    = _bind( linker, crypt32, "CertDeleteCertificateFromStore"   , FunctionDescriptor.of(DW , PTR) );

            // Advapi32.dll (wincrypt.h) - legacy CryptoAPI (CAPI1) used to provision the self-signed
            // certificate's private key, since SignerSignEx's internal CryptAcquireCertificatePrivateKey
            // call only follows the legacy CryptAcquireContext path when not given an NCrypt-allowing
            // flag - see WindowsDriverInstaller_FFM-Win32API.txt
            _CryptAcquireContextW = _bind( linker, advapi32, "CryptAcquireContextW", FunctionDescriptor.of(DW, PTR, PTR, PTR, DW, DW) );
            _CryptGenKey          = _bind( linker, advapi32, "CryptGenKey"         , FunctionDescriptor.of(DW, PTR, DW, DW, PTR) );
            _CryptDestroyKey      = _bind( linker, advapi32, "CryptDestroyKey"     , FunctionDescriptor.of(DW, PTR) );
            _CryptReleaseContext  = _bind( linker, advapi32, "CryptReleaseContext" , FunctionDescriptor.of(DW, PTR, DW) );

            // Wintrust.dll (mscat.h)
            _CryptCATAdminAcquireContext2         = _bind( linker, wintrust, "CryptCATAdminAcquireContext2"        , FunctionDescriptor.of(DW , PTR, PTR, PTR, PTR, DW) );
            _CryptCATAdminCalcHashFromFileHandle2 = _bind( linker, wintrust, "CryptCATAdminCalcHashFromFileHandle2", FunctionDescriptor.of(DW , PTR, PTR, PTR, PTR, DW) );
            _CryptCATAdminReleaseContext          = _bind( linker, wintrust, "CryptCATAdminReleaseContext"         , FunctionDescriptor.of(DW , PTR, DW) );
            _CryptCATAdminAddCatalog              = _bind( linker, wintrust, "CryptCATAdminAddCatalog"             , FunctionDescriptor.of(PTR, PTR, PTR, PTR, DW) );
            _CryptCATAdminReleaseCatalogContext   = _bind( linker, wintrust, "CryptCATAdminReleaseCatalogContext"  , FunctionDescriptor.of(DW , PTR, PTR, DW) );
            _CryptCATAdminEnumCatalogFromHash     = _bind( linker, wintrust, "CryptCATAdminEnumCatalogFromHash"    , FunctionDescriptor.of(PTR, PTR, PTR, DW, DW, PTR) );
            _CryptCATCatalogInfoFromContext       = _bind( linker, wintrust, "CryptCATCatalogInfoFromContext"      , FunctionDescriptor.of(DW , PTR, PTR, DW) );
            _WinVerifyTrust                       = _bind( linker, wintrust, "WinVerifyTrust"                      , FunctionDescriptor.of(DW , PTR, PTR, PTR) );
            _CryptCATOpen                         = _bind( linker, wintrust, "CryptCATOpen"                        , FunctionDescriptor.of(PTR, PTR, DW, PTR, DW, DW) );
            _CryptCATClose                        = _bind( linker, wintrust, "CryptCATClose"                       , FunctionDescriptor.of(DW , PTR) );
            _CryptCATPersistStore                 = _bind( linker, wintrust, "CryptCATPersistStore"                , FunctionDescriptor.of(DW , PTR) );
            _CryptCATPutMemberInfo                = _bind( linker, wintrust, "CryptCATPutMemberInfo"               , FunctionDescriptor.of(PTR, PTR, PTR, PTR, PTR, DW, DW, PTR) );
            _CryptCATPutAttrInfo                  = _bind( linker, wintrust, "CryptCATPutAttrInfo"                 , FunctionDescriptor.of(PTR, PTR, PTR, PTR, DW, DW, PTR) );

            // Mssign32.dll (not declared in any SDK header - see SignerSignEx/SignerFreeSignerContext docs)
            _SignerSignEx            = _bind( linker, mssign32, "SignerSignEx", FunctionDescriptor.of(DW, DW, PTR, PTR, PTR, PTR, PTR, PTR, PTR, PTR) );
            _SignerFreeSignerContext = _bind( linker, mssign32, "SignerFreeSignerContext", FunctionDescriptor.of(DW, PTR) );

            // Setupapi.dll (setupapi.h) - stages an INF into %windir%\Inf without a physically-present
            // device (installDriver's case: the CI/normal use here is "make this driver available for
            // whenever a matching device is plugged in", not "install onto a device that's already
            // attached").
            //
            // Bound with Linker.Option.captureCallState("GetLastError") : GetLastError() is thread-local
            // (Win32 TLS), and this call does real file I/O to %windir%\Inf that can run long enough for
            // the JVM to intervene (e.g. a GC/safepoint poll invoking its own Win32 APIs on this same OS
            // thread) between this downcall returning and a separate, later GetLastError() downcall,
            // silently clobbering the value. captureCallState atomically snapshots GetLastError as part
            // of this same downcall, before the JVM gets a chance to run anything else - see
            // _CAPTURE_STATE_LAYOUT/_capturedLastError() below.
            _SetupCopyOEMInfW = _bind( linker, setupapi, "SetupCopyOEMInfW", FunctionDescriptor.of(DW, PTR, PTR, DW, DW, PTR, DW, PTR, PTR), Linker.Option.captureCallState("GetLastError") );

            // Setupapi.dll (setupapi.h) - suppresses SetupAPI's ability to show interactive UI (e.g. an
            // unverified-publisher/unsigned-driver confirmation dialog) in the caller's context.
            //
            // Called right before SetupCopyOEMInfW in _elevatedInstallDriver: on a headless CI runner there
            // is no interactive desktop session and the elevated child's thread has no Windows message pump,
            // so if SetupCopyOEMInfW ever tries to raise such a dialog it blocks forever waiting for input
            // nothing can ever supply.
            _SetupSetNonInteractiveMode = _bind( linker, setupapi, "SetupSetNonInteractiveMode", FunctionDescriptor.of(DW, DW) );

            // Cfgmgr32.dll (cfgmgr32.h) - bound against its own, canonical DLL, not Setupapi.dll (which
            // Microsoft's api_location metadata also lists as an exporter, but that does not hold at
            // runtime on real Windows).
            //
            // Exported symbol name on x64 is "CMP_WaitNoPendingInstallEvents" (extra "P"), not the
            // documented "CM_WaitNoPendingInstallEvents" - a well-known, publicly-documented quirk of how
            // Cfgmgr32.dll's x64 export table was built, not an undocumented API. See
            // WindowsDriverInstaller_FFM-Win32API.txt for details.
            _CM_WaitNoPendingInstallEvents = _bind( linker, cfgmgr32, "CMP_WaitNoPendingInstallEvents", FunctionDescriptor.of(DW, DW) );
        }
        catch(final Throwable t) {
            // Do not throw out of a static initializer with anything worse than what we capture here;
            // isUsable() below reports this failure through the normal (non-exceptional) return path
            _initError = t;
        }
    }

    private static MethodHandle _bind(final Linker linker, final SymbolLookup lib, final String name, final FunctionDescriptor fd)
    { return linker.downcallHandle( lib.find(name).orElseThrow( () -> new UnsatisfiedLinkError(name) ), fd ); }

    private static MethodHandle _bind(final Linker linker, final SymbolLookup lib, final String name, final FunctionDescriptor fd, final Linker.Option... options)
    { return linker.downcallHandle( lib.find(name).orElseThrow( () -> new UnsatisfiedLinkError(name) ), fd, options ); }

    // Layout of the call-state segment produced by Linker.Option.captureCallState("GetLastError") -
    // a downcall handle bound with that option takes this as an extra, prepended MemorySegment
    // argument - allocate one per call via Linker.Option.captureStateLayout()
    private static final GroupLayout _CAPTURE_STATE_LAYOUT = Linker.Option.captureStateLayout();

    private static int _capturedLastError(final MemorySegment captureState)
    {
        return (int) _CAPTURE_STATE_LAYOUT
            .varHandle( MemoryLayout.PathElement.groupElement("GetLastError") )
            .get(captureState, 0L);
    }

    // Diagnostic-only : lets a caller (e.g. a CI test driver) find out WHY isUsable() returned false
    // due to the static initializer failing, instead of just seeing a bare "false" with no explanation
    public static String initErrorDiagnostic()
    { return _initError == null ? null : _initError.toString(); }

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

            // Confirm the handles we actually rely on were resolved successfully. Catalog signing is
            // performed directly via SignerSignEx (see _signCatalog), and driver staging directly via
            // SetupCopyOEMInfW (see _elevatedInstallDriver) - no external Windows SDK tool is required.
            return _CertOpenStore != null && _CryptCATOpen != null && _SignerSignEx != null && _CryptAcquireContextW != null
                && _ShellExecuteExW != null && _SetupCopyOEMInfW != null && _SetupSetNonInteractiveMode != null
                && _CM_WaitNoPendingInstallEvents != null;
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

    // Diagnostic-only: renders a struct's raw bytes as hex, for hand-checking field offsets/values
    // against WindowsDriverInstaller_FFM-Win32API.txt when a native call fails for no apparent reason
    private static String _hexDump(final MemorySegment seg, final long size)
    { return HexFormat.of().formatHex( seg.reinterpret(size).toArray(ValueLayout.JAVA_BYTE) ); }

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
    private static final int  CERT_KEY_PROV_INFO_PROP_ID       = 2;
    private static final int  CERT_STORE_ADD_REPLACE_EXISTING  = 3;

    @Override
    public XCom.Pair<Integer, String> isProviderAlreadyTrusted(final String providerName)
    {
        return _isProviderAlreadyTrusted(providerName);
    }

    // Static so _elevatedInstallSelfSignedDriver() (running in the elevated child, no instance) can call
    // it directly too - see the NOTE below on why it needs no elevation itself
    private static XCom.Pair<Integer, String> _isProviderAlreadyTrusted(final String providerName)
    {
        // NOTE : isProviderAlreadyTrusted () is the only operation that does not require elevation

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
        // Dispatch to the elevated child - stages infPath directly via SetupCopyOEMInfW (see
        // _elevatedInstallDriver), not by shelling out to any external tool.
        //
        // The path/extension validation this used to do inline now happens in the elevated child
        // (_elevatedInstallDriver) instead, matching createAndTrustProvider/createAndSignCatalog's
        // pattern of validating everything on the far side of the elevation boundary.

        try {
            return _runElevatedSelf("install", new String[]{ infPath }, 5);
        }
        catch(final Throwable t) {
            if( XCom.enableAllExceptionStackTrace() ) t.printStackTrace();
            return new XCom.Pair<Integer, String>( RETCODE_EXCEPTION, t.toString() );
        }
    }

    @Override
    protected XCom.Pair<Integer, String> installSelfSignedDriver(final String infPath)
    {
        // Overrides WindowsDriverInstaller's default 3-separate-elevated-calls sequence: this backend can
        // relaunch itself elevated once and run trust+sign+install all inside that single elevated child
        // (see _elevatedInstallSelfSignedDriver() below), so the end user only ever sees one UAC prompt.

        try {
            return _runElevatedSelf("all", new String[]{ infPath, PROVIDER_NAME }, 5);
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
    // SHELLEXECUTEINFOW (x64) field byte offsets - see shellapi.h. Struct size = 112, align = 8

    private static final int SEE_MASK_NOCLOSEPROCESS = 0x00000040;
    private static final int SEE_MASK_NOASYNC        = 0x00000100;
    private static final int SEE_MASK_NO_CONSOLE     = 0x00008000;
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
     * logFile (written by the elevated child itself).
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

            sei.set( ValueLayout.JAVA_INT, SEI_cbSize      , SEI_SIZE                                                         );
            sei.set( ValueLayout.JAVA_INT, SEI_fMask       , SEE_MASK_NOCLOSEPROCESS | SEE_MASK_NOASYNC | SEE_MASK_NO_CONSOLE );
            sei.set( PTR                 , SEI_hwnd        , MemorySegment.NULL                                               );
            sei.set( PTR                 , SEI_lpVerb      , _wstr(arena, "runas")                                            );
            sei.set( PTR                 , SEI_lpFile      , _wstr(arena, file   )                                            );
            sei.set( PTR                 , SEI_lpParameters, _wstr(arena, params )                                            );
            sei.set( PTR                 , SEI_lpDirectory , _wstr(arena, workDir)                                            );
            sei.set( ValueLayout.JAVA_INT, SEI_nShow       , SW_HIDE                                                          );
            sei.set( PTR                 , SEI_hInstApp    , MemorySegment.NULL                                               );
            sei.set( PTR                 , SEI_lpIDList    , MemorySegment.NULL                                               );
            sei.set( PTR                 , SEI_lpClass     , MemorySegment.NULL                                               );
            sei.set( PTR                 , SEI_hkeyClass   , MemorySegment.NULL                                               );
            sei.set( ValueLayout.JAVA_INT, SEI_dwHotKey    , 0                                                                );
            sei.set( PTR                 , SEI_hIcon       , MemorySegment.NULL                                               );
            sei.set( PTR                 , SEI_hProcess    , MemorySegment.NULL                                               );

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

            return _shellExecuteElevatedAndWait( javaExe, params.toString(), System.getProperty("user.dir"), waitTimeMinutes, logFile );
        }
        finally {
            try { Files.deleteIfExists(logFile); }
            catch(final Exception ignored) {}
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
                case "install" -> exitCode = _elevatedInstallDriver(args[3], log);
                case "all"     -> exitCode = _elevatedInstallSelfSignedDriver(args[3], args[4], log);
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

    // wincrypt.h - legacy CryptoAPI (CAPI1) provider/key constants used to provision the self-signed
    // certificate's private key (see _elevatedCreateAndTrustProvider)
    private static final int    PROV_RSA_FULL         = 1;
    private static final int    CRYPT_EXPORTABLE      = 0x00000001;
    private static final int    CRYPT_NEWKEYSET       = 0x00000008;
    private static final int    CRYPT_DELETEKEYSET    = 0x00000010;
    private static final int    CRYPT_MACHINE_KEYSET  = 0x00000020;
    private static final int    CRYPT_SILENT          = 0x00000040;
    private static final int    AT_SIGNATURE          = 2;

    // wincrypt.h
    // X509_ENHANCED_KEY_USAGE is a small-integer "predefined" lpszStructType, cast to LPCSTR - not a real string pointer
    //
    // Forces CertStrToNameW to encode RDN values as UTF8String (ASN.1 tag 0x0C) instead of its
    // default choice (BMPString, tag 0x1E) - CI comparison against WindowsDriverInstaller_PS1's
    // New-SelfSignedCertificate-generated cert showed PS1 uses UTF8String while FFM's
    // CertStrToNameW defaulted to BMPString encoded with little-endian byte pairs (non-compliant
    // with X.690's big-endian BMPString requirement, though self-consistent within CryptoAPI)
    private static final int    CERT_X500_NAME_STR                    = 3;
    private static final int    CERT_NAME_STR_FORCE_UTF8_DIR_STR_FLAG = 0x00080000;
    private static final int    CRYPT_ENCODE_ALLOC_FLAG               = 0x8000;
    private static final long   X509_ENHANCED_KEY_USAGE               = 36L;
    private static final String szOID_ENHANCED_KEY_USAGE              = "2.5.29.37";
    private static final String szOID_PKIX_KP_CODE_SIGNING            = "1.3.6.1.5.5.7.3.3";
    private static final String szOID_RSA_SHA256RSA                   = "1.2.840.113549.1.1.11";
    private static final String szOID_KEY_USAGE                       = "2.5.29.15";
    private static final String szOID_SUBJECT_KEY_IDENTIFIER          = "2.5.29.14";
    private static final int    CALG_SHA1                             = 0x00008004;

    // CRYPT_ATTR_BLOB / CERT_NAME_BLOB (wincrypt.h) : { DWORD cbData; BYTE *pbData; } - size 16, align 8
    private static MemorySegment _blob(final Arena arena, final int cbData, final MemorySegment pbData)
    {
        final MemorySegment b = arena.allocate(16, 8);
        b.set(ValueLayout.JAVA_INT, 0, cbData);
        b.set(PTR                 , 8, pbData);
        return b;
    }

    // Hand-encodes a minimal X.501 Name { RDNSequence { RDN { AttributeTypeAndValue {
    // commonName, UTF8String value } } } } DER blob for "CN=<cn>", bypassing CertStrToNameW entirely.
    // CertStrToNameW's CERT_NAME_STR_FORCE_UTF8_DIR_STR_FLAG does not reliably force UTF8String
    // (ASN.1 tag 0x0C) encoding on this API in practice - it can still emit BMPString (tag 0x1E) -
    // so this hand-encoding removes the dependency on that undocumented behavior and matches
    // WindowsDriverInstaller_PS1's New-SelfSignedCertificate output. Only ASCII providerName values
    // are ever passed in this codebase (CI test names / caller-controlled strings), so plain
    // UTF8String bytes (== ASCII bytes for the ASCII subset) suffice
    private static byte[] _derEncodeCNUtf8(final String cn)
    {
        final byte[] cnBytes    = cn.getBytes(StandardCharsets.UTF_8);
        final byte[] cnValue    = _derTLV( (byte) 0x0C, cnBytes );                     // UTF8String
        final byte[] atv        = _derTLV( (byte) 0x30, _concat(                       // SEQUENCE
                                       _derTLV(                                        // OID 2.5.4.3 (commonName)
                                           (byte) 0x06, new byte[]{ 0x55, 0x04, 0x03 }
                                       ),
                                       cnValue
                                   ) );
        final byte[] rdn        = _derTLV( (byte) 0x31, atv );                         // SET
        return _derTLV( (byte) 0x30, rdn );                                            // SEQUENCE (Name)
    }

    private static byte[] _derTLV(final byte tag, final byte[] content)
    {
        final byte[] len = _derLen(content.length);
        final byte[] out = new byte[1 + len.length + content.length];
        out[0] = tag;
        System.arraycopy(len    , 0, out, 1             , len.length    );
        System.arraycopy(content, 0, out, 1 + len.length, content.length);
        return out;
    }

    private static byte[] _derLen(final int len)
    {
        if( len < 0x80 ) return new byte[]{ (byte) len };

        int n = 0;
        for(int t = len; t > 0; t >>= 8) n++;

        final byte[] out = new byte[1 + n];
        out[0] = (byte) (0x80 | n);

        for(int i = 0; i < n; i++) out[1 + i] = (byte) ( len >> ( 8 * (n - 1 - i) ) );

        return out;
    }

    private static byte[] _concat(final byte[] a, final byte[] b)
    {
        final byte[] out = new byte[a.length + b.length];

        System.arraycopy(a, 0, out, 0       , a.length);
        System.arraycopy(b, 0, out, a.length, b.length);

        return out;
    }

    // Opens a system certificate store (CERT_STORE_PROV_SYSTEM_W) - see wincrypt.h
    private static MemorySegment _certStoreOpen(final Arena arena, final int locationFlag, final String storeName) throws Throwable
    {
        final MemorySegment h = (MemorySegment) _CertOpenStore.invoke(
            _handleOf(CERT_STORE_PROV_SYSTEM_W), 0, MemorySegment.NULL, locationFlag, _wstr(arena, storeName)
        );

        return (h == null || h.address() == 0L) ? null : h;
    }

    /*
     * Permanently deletes the legacy CAPI1 key container backing the given certificate, via
     * CryptAcquireContextW's CRYPT_DELETEKEYSET flag, so the private key can never be recovered or reused.
     *
     * The certificate's public key stays embedded in the cert itself and remains valid for verifying anything
     * already signed with it. This is required because this backend installs its self-signed certificate as
     * both a Trusted Root CA and a Trusted Publisher: leaving the private key alive after signing would let
     * anything with access to CurrentUser\My mint new, fully-trusted signatures on this machine.
     */
    private static void _destroyPrivateKey(final Arena arena, final MemorySegment cert, final StringBuilder log) throws Throwable
    {
        final MemorySegment cbKeyProvInfo = arena.allocate(ValueLayout.JAVA_INT);
        if( (int) _CertGetCertificateContextProperty.invoke(cert, CERT_KEY_PROV_INFO_PROP_ID, MemorySegment.NULL, cbKeyProvInfo) == 0 ) {
            return; // No CRYPT_KEY_PROV_INFO property - nothing to destroy (e.g. a Root/TrustedPublisher-only copy)
        }

        final MemorySegment keyProvInfo = arena.allocate( cbKeyProvInfo.get(ValueLayout.JAVA_INT, 0) );
        if( (int) _CertGetCertificateContextProperty.invoke(cert, CERT_KEY_PROV_INFO_PROP_ID, keyProvInfo, cbKeyProvInfo) == 0 ) {
            log.append("CertGetCertificateContextProperty(CERT_KEY_PROV_INFO_PROP_ID) failed, GetLastError=").append( _lastError() ).append('\n');
            return;
        }

        // CRYPT_KEY_PROV_INFO : { LPWSTR pwszContainerName; LPWSTR pwszProvName; DWORD dwProvType; DWORD dwFlags; ... }
        final MemorySegment containerName = keyProvInfo.get(PTR                 ,  0);
        final int           provFlags     = keyProvInfo.get(ValueLayout.JAVA_INT, 20);

        final MemorySegment hProviderOut = arena.allocate(PTR);
        final int           deleteFlags  = CRYPT_DELETEKEYSET | (provFlags & CRYPT_MACHINE_KEYSET);
        if( (int) _CryptAcquireContextW.invoke(hProviderOut, containerName, MemorySegment.NULL, PROV_RSA_FULL, deleteFlags) == 0 ) {
            log.append("CryptAcquireContextW(CRYPT_DELETEKEYSET) failed, GetLastError=").append( _lastError() ).append('\n');
        }
    }

    /*
     * Deletes every certificate in the given store whose subject matches providerName so repeated runs never
     * accumulate duplicate certificates under the same provider name.
     *
     * When the store is CurrentUser\My, also destroys the CAPI1 private key container backing each match first
     * (see _destroyPrivateKey) - Root/TrustedPublisher only ever hold the public-only copy, so there is no key
     * to destroy there.
     */
    private static void _deleteExistingCerts(final Arena arena, final String providerName, final int locationFlag, final String storeName, final StringBuilder log) throws Throwable
    {
        final MemorySegment hStore = _certStoreOpen(arena, locationFlag, storeName);
        if(hStore == null) return;

        try {
            while(true) {
                // Re-queried from scratch (pPrevCertContext=NULL) every iteration, since
                // CertDeleteCertificateFromStore invalidates any enumeration position
                final MemorySegment cert = (MemorySegment) _CertFindCertificateInStore.invoke(
                    hStore, CRYPT_ASN_ENCODING, 0, CERT_FIND_SUBJECT_STR_W, _wstr(arena, providerName), MemorySegment.NULL
                );
                if(cert == null || cert.address() == 0L) break;

                if(locationFlag == CERT_SYSTEM_STORE_CURRENT_USER) _destroyPrivateKey(arena, cert, log);

                // CertDeleteCertificateFromStore always frees the passed certificate context, whether it succeeds or fails
                if( (int) _CertDeleteCertificateFromStore.invoke(cert) == 0 ) {
                    log.append("CertDeleteCertificateFromStore(").append(storeName).append(") failed, GetLastError=").append( _lastError() ).append('\n');
                }
            }
        }
        finally {
            _CertCloseStore.invoke(hStore, 0);
        }
    }

    /*
     * Runs trust+sign+install inside a single elevated child process (see installSelfSignedDriver()
     * above), instead of the 3 separate elevated relaunches (and up to 3 UAC prompts) the default
     * WindowsDriverInstaller.installSelfSignedDriver() sequence does.
     *
     * isProviderAlreadyTrusted() is called directly here (not via a further relaunch) since it does
     * not require elevation itself - see its NOTE - and this method already runs elevated.
     */
    private static int _elevatedInstallSelfSignedDriver(final String infPath, final String providerName, final StringBuilder log) throws Throwable
    {
        if( _isProviderAlreadyTrusted(providerName).first() == RETCODE_OK ) {
            final int trustResult = _elevatedCreateAndTrustProvider(providerName, log);
            if(trustResult != RETCODE_OK) return trustResult;
        }

        final int catalogResult = _elevatedCreateAndSignCatalog(infPath, providerName, log);
        if(catalogResult != RETCODE_OK) return catalogResult;

        return _elevatedInstallDriver(infPath, log);
    }

    private static int _elevatedCreateAndTrustProvider(final String providerName, final StringBuilder log) throws Throwable
    {
        try(
            final Arena arena = Arena.ofConfined()
        ) {
            // Tracked across the whole method (not just the tail end) so the finally below can release
            // whatever was actually allocated, regardless of which step below returns early on failure
            MemorySegment hProvider  = MemorySegment.NULL;
            MemorySegment hKey       = MemorySegment.NULL;
            MemorySegment certCtx    = MemorySegment.NULL;
            MemorySegment ekuEncoded = MemorySegment.NULL;

            try {
                // 0. Delete any existing certificate(s) under this provider name from every store this
                //    method touches, so re-running never accumulates duplicates and never leaves an
                //    orphaned private key behind.
                _deleteExistingCerts(arena, providerName, CERT_SYSTEM_STORE_CURRENT_USER,  "My"              , log);
                _deleteExistingCerts(arena, providerName, CERT_SYSTEM_STORE_LOCAL_MACHINE, "Root"            , log);
                _deleteExistingCerts(arena, providerName, CERT_SYSTEM_STORE_LOCAL_MACHINE, "TrustedPublisher", log);

                // 1. Acquire a legacy CryptoAPI (CAPI1) provider context and generate a 2048-bit RSA
                //    signing key in it. A fresh, randomly-named key container is created every call, so
                //    CRYPT_NEWKEYSET is always correct here - there is never an existing container to
                //    reopen
                final String        containerName = providerName + "_" + Long.toHexString( new SecureRandom().nextLong() );
                final MemorySegment hProviderOut  = arena.allocate(PTR);
                if( (int) _CryptAcquireContextW.invoke(
                    hProviderOut, _wstr(arena, containerName), MemorySegment.NULL, PROV_RSA_FULL,
                    CRYPT_NEWKEYSET | CRYPT_MACHINE_KEYSET | CRYPT_SILENT
                ) == 0 ) {
                    log.append("CryptAcquireContextW failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }
                hProvider = hProviderOut.get(PTR, 0);

                final MemorySegment hKeyOut = arena.allocate(PTR);
                if( (int) _CryptGenKey.invoke( hProvider, AT_SIGNATURE, (2048 << 16) | CRYPT_EXPORTABLE, hKeyOut ) == 0 ) {
                    log.append("CryptGenKey failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }
                hKey = hKeyOut.get(PTR, 0);

                // 2. Encode the "CN=<providerName>" subject name - hand-built DER (UTF8String), not
                // CertStrToNameW; see _derEncodeCNUtf8's header comment for why
                final byte[]         subjectDer = _derEncodeCNUtf8(providerName);
                final int            subjectLen = subjectDer.length;
                final MemorySegment  subjectBuf = arena.allocate(subjectLen);
                MemorySegment.copy(subjectDer, 0, subjectBuf, ValueLayout.JAVA_BYTE, 0, subjectLen);
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
                ekuEncoded = ekuEncodedPtrOut.get(PTR, 0);
                final int ekuEncodedLen = ekuEncodedLenOut.get(ValueLayout.JAVA_INT, 0);

                // 3b. Build the Key Usage (critical, digitalSignature only) and Subject Key Identifier
                //     extensions, matching what WindowsDriverInstaller_PS1's New-SelfSignedCertificate emits.
                //     Both extension VALUEs (KeyUsage BIT STRING, SKI OCTET STRING) are simple enough to
                //     hand-encode directly (reusing _derTLV) rather than going through CryptEncodeObjectEx.

                // KeyUsage ::= BIT STRING - one content byte (0x80 = bit 0 = digitalSignature), 7 unused bits.
                final byte[] keyUsageDer = _derTLV( (byte) 0x03, new byte[]{ 0x07, (byte) 0x80 } );

                // SubjectKeyIdentifier ::= KeyIdentifier (OCTET STRING) - RFC 5280 method 1: SHA1 hash of the
                // encoded SubjectPublicKeyInfo. Export it via CryptExportPublicKeyInfo (opaque blob, never
                // decoded here) then hash it with CryptHashPublicKeyInfo (two-call size/fill pattern, both).
                final MemorySegment pubKeyInfoLenOut = arena.allocate(ValueLayout.JAVA_INT);
                if( (int) _CryptExportPublicKeyInfo.invoke( hProvider, AT_SIGNATURE, X509_ASN_ENCODING, MemorySegment.NULL, pubKeyInfoLenOut ) == 0 ) {
                    log.append("CryptExportPublicKeyInfo (sizing) failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }
                final MemorySegment pubKeyInfo = arena.allocate( pubKeyInfoLenOut.get(ValueLayout.JAVA_INT, 0), 8 );
                if( (int) _CryptExportPublicKeyInfo.invoke( hProvider, AT_SIGNATURE, X509_ASN_ENCODING, pubKeyInfo, pubKeyInfoLenOut ) == 0 ) {
                    log.append("CryptExportPublicKeyInfo failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }

                final MemorySegment skiHashBuf    = arena.allocate(20); // SHA1 = 20 bytes
                final MemorySegment skiHashLenOut = arena.allocate(ValueLayout.JAVA_INT);
                skiHashLenOut.set(ValueLayout.JAVA_INT, 0, 20);
                if( (int) _CryptHashPublicKeyInfo.invoke( hProvider, CALG_SHA1, 0, X509_ASN_ENCODING, pubKeyInfo, skiHashBuf, skiHashLenOut ) == 0 ) {
                    log.append("CryptHashPublicKeyInfo failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }
                final byte[] skiHash = skiHashBuf.reinterpret( skiHashLenOut.get(ValueLayout.JAVA_INT, 0) ).toArray(ValueLayout.JAVA_BYTE);
                final byte[] skiDer  = _derTLV( (byte) 0x04, skiHash );

                // CERT_EXTENSION : { LPSTR pszObjId; BOOL fCritical; CRYPT_ATTR_BLOB Value; } - size 32, align 8
                final MemorySegment keyUsageBuf = arena.allocate(keyUsageDer.length);
                MemorySegment.copy(keyUsageDer, 0, keyUsageBuf, ValueLayout.JAVA_BYTE, 0, keyUsageDer.length);
                final MemorySegment skiBuf = arena.allocate(skiDer.length);
                MemorySegment.copy(skiDer, 0, skiBuf, ValueLayout.JAVA_BYTE, 0, skiDer.length);

                // 3c. Extensions: EKU (Code Signing), Key Usage (critical, Digital Signature), Subject Key Identifier
                final MemorySegment extensionArr = arena.allocate(32L * 3, 8);

                final MemorySegment ext0 = extensionArr.asSlice(0);
                ext0.set( PTR                 ,  0, _astr(arena, szOID_ENHANCED_KEY_USAGE) );
                ext0.set( ValueLayout.JAVA_INT,  8, 0                                      );
                ext0.set( ValueLayout.JAVA_INT, 16, ekuEncodedLen                          );
                ext0.set( PTR                 , 24, ekuEncoded                             );

                final MemorySegment ext1 = extensionArr.asSlice(32);
                ext1.set( PTR                 ,  0, _astr(arena, szOID_KEY_USAGE) );
                ext1.set( ValueLayout.JAVA_INT,  8, 1 /* critical */              );
                ext1.set( ValueLayout.JAVA_INT, 16, keyUsageDer.length            );
                ext1.set( PTR                 , 24, keyUsageBuf                   );

                final MemorySegment ext2 = extensionArr.asSlice(64);
                ext2.set( PTR                 ,  0, _astr(arena, szOID_SUBJECT_KEY_IDENTIFIER) );
                ext2.set( ValueLayout.JAVA_INT,  8, 0                                          );
                ext2.set( ValueLayout.JAVA_INT, 16, skiDer.length                              );
                ext2.set( PTR                 , 24, skiBuf                                     );

                // CERT_EXTENSIONS : { DWORD cExtension; PCERT_EXTENSION rgExtension; } - size 16, align 8
                final MemorySegment extensions = arena.allocate(16, 8);
                extensions.set(ValueLayout.JAVA_INT, 0, 3           );
                extensions.set(PTR                 , 8, extensionArr);

                // 4. CRYPT_KEY_PROV_INFO - links the returned cert context back to the CAPI1 key above
                //    { LPWSTR pwszContainerName; LPWSTR pwszProvName; DWORD dwProvType; DWORD dwFlags;
                //      DWORD cProvParam; PVOID rgProvParam; DWORD dwKeySpec; } - size 48, align 8
                //    pwszProvName=NULL selects the default RSA Full (PROV_RSA_FULL) CSP rather than naming one
                //    explicitly; dwKeySpec=AT_SIGNATURE since this private key was created via legacy
                //    CryptGenKey against a CAPI1 provider context (see WindowsDriverInstaller_FFM-Win32API.txt's
                //    CRYPT_KEY_PROV_INFO entry for why this backend uses CAPI1 rather than a CNG key)
                final MemorySegment keyProvInfo = arena.allocate(48, 8);
                keyProvInfo.set( PTR                 ,  0, _wstr(arena, containerName) );
                keyProvInfo.set( PTR                 ,  8, MemorySegment.NULL          );
                keyProvInfo.set( ValueLayout.JAVA_INT, 16, PROV_RSA_FULL               );
                keyProvInfo.set( ValueLayout.JAVA_INT, 20, CRYPT_MACHINE_KEYSET        );
                keyProvInfo.set( ValueLayout.JAVA_INT, 24, 0                           );
                keyProvInfo.set( PTR                 , 32, MemorySegment.NULL          );
                keyProvInfo.set( ValueLayout.JAVA_INT, 40, AT_SIGNATURE                );

                // 5. Create the self-signed certificate (explicit SHA256RSA signature algorithm - CertCreateSelfSignCertificate
                //    defaults pSignatureAlgorithm=NULL to SHA1RSA, which this Windows-10+-only backend must not rely on
                //    given every other signing/hashing step in this file is deliberately pinned to SHA-256)
                //    CRYPT_ALGORITHM_IDENTIFIER : { LPSTR pszObjId; CRYPT_OBJID_BLOB Parameters; } - size 24, align 8
                final MemorySegment sigAlgId = arena.allocate(24, 8);
                sigAlgId.set( PTR                 ,  0, _astr(arena, szOID_RSA_SHA256RSA) );
                sigAlgId.set( ValueLayout.JAVA_INT,  8, 0                                 );
                sigAlgId.set( PTR                 , 16, MemorySegment.NULL                );

                // pEndTime: pStartTime=NULL below defaults to the current system time, but pEndTime=NULL
                // would default to just 1 year past that - too short-lived, since this cert's expiry is
                // also the catalog signature's trust boundary (SignerSignEx below does not timestamp the
                // signature, so validation is always against the live clock, not sign-time - see
                // installSelfSignedDriver()'s header comment). 20 years out instead, in UTC to match what
                // CertCreateSelfSignCertificate expects (per its pStartTime/pEndTime documentation).
                // SYSTEMTIME : { WORD wYear; WORD wMonth; WORD wDayOfWeek; WORD wDay; WORD wHour;
                //                WORD wMinute; WORD wSecond; WORD wMilliseconds; } - size 16, align 2
                // wDayOfWeek is not consulted when a SYSTEMTIME is only ever converted to a FILETIME (as
                // CertCreateSelfSignCertificate does internally), so it is left 0 here.
                final ZonedDateTime endUtc  = ZonedDateTime.now(ZoneOffset.UTC).plusYears(20);
                final MemorySegment endTime = arena.allocate(16, 2);
                endTime.set( ValueLayout.JAVA_SHORT,  0, (short) endUtc.getYear()       );
                endTime.set( ValueLayout.JAVA_SHORT,  2, (short) endUtc.getMonthValue() );
                endTime.set( ValueLayout.JAVA_SHORT,  4, (short) 0                      );
                endTime.set( ValueLayout.JAVA_SHORT,  6, (short) endUtc.getDayOfMonth() );
                endTime.set( ValueLayout.JAVA_SHORT,  8, (short) endUtc.getHour()       );
                endTime.set( ValueLayout.JAVA_SHORT, 10, (short) endUtc.getMinute()     );
                endTime.set( ValueLayout.JAVA_SHORT, 12, (short) endUtc.getSecond()     );
                endTime.set( ValueLayout.JAVA_SHORT, 14, (short) 0                      );

                // hCryptProvOrNCryptKey=NULL: pKeyProvInfo above fully describes how to acquire the key,
                // so CertCreateSelfSignCertificate does not need an already-open handle passed in
                certCtx = (MemorySegment) _CertCreateSelfSignCertificate.invoke(
                    MemorySegment.NULL, subjectBlob, 0, keyProvInfo, sigAlgId, MemorySegment.NULL, endTime, extensions
                );
                if( certCtx == null || certCtx.address() == 0L ) {
                    log.append("CertCreateSelfSignCertificate failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }

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
                final MemorySegment pbCertEncoded = certCtxView.get(PTR                 ,  8);
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

                // Written to the same %TEMP%\<providerName>.cer path WindowsDriverInstaller_PS1's
                // createAndTrustProvider() uses (Export-Certificate), so both backends' output is
                // directly comparable byte-for-byte / name-for-name
                final Path certFile = Paths.get( System.getProperty("java.io.tmpdir"), providerName + ".cer" );
                try {
                    Files.write(certFile, derBytes);
                }
                catch(final IOException e) {
                    log.append("Failed to write certificate file: ").append(e).append('\n');
                }

                return RETCODE_OK;
            }
            finally {
                // Release exactly what was allocated above, however far execution got before returning
                if( certCtx.address()    != 0L ) _CertFreeCertificateContext.invoke(certCtx);
                if( ekuEncoded.address() != 0L ) _LocalFree.invoke(ekuEncoded);
                if( hKey.address()       != 0L ) _CryptDestroyKey.invoke(hKey);
                if( hProvider.address()  != 0L ) _CryptReleaseContext.invoke(hProvider, 0);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Elevated op : create a v2 catalog for the INF and Authenticode-sign it with the trusted cert

    // mscat.h
    // DRIVER_ACTION_VERIFY (softpub.h) - the SIP subsystem used for driver catalog validation
    private static final int    CRYPTCAT_OPEN_CREATENEW     = 0x00000001;
    private static final int    CRYPTCAT_VERSION_2          = 0x00000200;
    private static final int    CRYPTCAT_ATTR_AUTHENTICATED = 0x10000000;
    private static final int    CRYPTCAT_ATTR_NAMEASCII     = 0x00000001;
    private static final int    CRYPTCAT_ATTR_DATAASCII     = 0x00010000;
    private static final long[] DRIVER_ACTION_VERIFY_PARTS  = { 0xF750E6C3L, 0x38EEL, 0x11d1L, 0x85L, 0xE5L, 0x00L, 0xC0L, 0x4FL, 0xC2L, 0x95L, 0xEEL };

    // wintrust.h - identifies the standard Authenticode trust-verification policy for WinVerifyTrust
    private static final long[] WINTRUST_ACTION_GENERIC_VERIFY_V2_PARTS = { 0x00AAC56BL, 0xCD44L, 0x11D0L, 0x8CL, 0xC2L, 0x00L, 0xC0L, 0x4FL, 0xC2L, 0x95L, 0xEEL };
    private static final int    WTD_UI_NONE            = 2;
    private static final int    WTD_REVOKE_NONE        = 0;
    private static final int    WTD_CHOICE_FILE        = 1;
    private static final int    WTD_STATEACTION_VERIFY = 1;
    private static final int    WTD_STATEACTION_CLOSE  = 2;

    // wincrypt.h / mssign32 SPC_LINK "Obsolete" placeholder - the standard way a non-PE (INF/CAB) catalog
    // member's SIP-indirect data references its subject file, per the SPC_INDIRECT_DATA_CONTENT scheme
    private static final int    SPC_FILE_LINK_CHOICE = 3;
    private static final String SPC_CAB_DATA_OBJID   = "1.3.6.1.4.1.311.2.1.25";
    private static final String szOID_NIST_sha256    = "2.16.840.1.101.3.4.2.1";
    private static final String szOID_OIWSEC_sha1    = "1.3.14.3.2.26";

    // SHA1-only. A catalog member's hash algorithm must match its header version (see CryptCATOpen's
    // dwPublicVersion=0 below) - a SHA256 member alongside SHA1, or SHA256 alone, both make
    // SetupCopyOEMInfW reject the catalog with SPAPI_E_FILE_HASH_NOT_IN_CATALOG. SHA1 is deprecated
    // for general cryptographic use, but this specific legacy catalog member-hash format is a separate
    // mechanism from that deprecation - dual SHA1+SHA256 kept below, commented out, for reference
    private static final String[] CATALOG_HASH_ALGS     = { "SHA1" };
    private static final String[] CATALOG_HASH_ALG_OIDS = { szOID_OIWSEC_sha1 };
  //private static final String[] CATALOG_HASH_ALGS     = { "SHA1", "SHA256" };
  //private static final String[] CATALOG_HASH_ALG_OIDS = { szOID_OIWSEC_sha1, szOID_NIST_sha256 };

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

            // Saved off per-algorithm so _diagCatalogLookup (called much further down, after signing and
            // database registration) can re-query the catalog database with the exact same hash bytes -
            // these MemorySegments are arena-allocated so they stay valid past hInfFile's own close
            final MemorySegment[] savedHash    = new MemorySegment[CATALOG_HASH_ALGS.length];
            final int[]           savedHashLen = new int[CATALOG_HASH_ALGS.length];

            // Open the INF and, once, encode the SIP-indirect "SPC_LINK" placeholder every member below shares
            // - it doesn't depend on the hash/algorithm, only on there being a non-PE/CAB subject file at all
            final MemorySegment hInfFile = (MemorySegment) _CreateFileW.invoke(
                _wstr(arena, infPath), GENERIC_READ, FILE_SHARE_READ, MemorySegment.NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, MemorySegment.NULL
            );
            if(hInfFile == null || hInfFile.address() == INVALID_HANDLE_VALUE) {
                log.append("CreateFileW(inf) failed, GetLastError=").append( _lastError() );
                return RETCODE_EXCEPTION;
            }

            boolean catCloseOk = false;
            try {
                // subjectGuid is the fixed "this is an INF" subject-type GUID (SPC_INC_SPL_INF_ style),
                // not a value derived from CryptSIPRetrieveSubjectGuid/DRIVER_ACTION_VERIFY
                final long[] INF_SUBJECT_TYPE_PARTS = { 0xDE351A42L, 0x8E59L, 0x11D0L, 0x8CL, 0x47L, 0x00L, 0xC0L, 0x4FL, 0xC2L, 0x95L, 0xEEL };
                final MemorySegment subjectGuid = _guid(arena, INF_SUBJECT_TYPE_PARTS);

                // Build the SIP-indirect data CryptCATPutMemberInfo requires : an ASN.1-encoded SPC_LINK
                // (the standard "<<<Obsolete>>>" file-link placeholder used for a non-PE/CAB-typed subject
                // such as an INF) wrapped in a SIP_INDIRECT_DATA struct together with the digest algorithm/hash
                // SPC_LINK : { DWORD dwLinkChoice; union{ LPWSTR pwszUrl; SPC_SERIALIZED_OBJECT Moniker; LPWSTR pwszFile; }; }
                final MemorySegment spcLink = arena.allocate(16, 8);
                spcLink.set( ValueLayout.JAVA_INT, 0, SPC_FILE_LINK_CHOICE            );
                spcLink.set( PTR                 , 8, _wstr( arena, "<<<Obsolete>>>") );

                final MemorySegment spcLinkEncPtrOut = arena.allocate(PTR);
                final MemorySegment spcLinkEncLenOut = arena.allocate(ValueLayout.JAVA_INT);
                if( (int) _CryptEncodeObjectEx.invoke( X509_ASN_ENCODING, _astr(arena, SPC_CAB_DATA_OBJID), spcLink, CRYPT_ENCODE_ALLOC_FLAG, MemorySegment.NULL, spcLinkEncPtrOut, spcLinkEncLenOut ) == 0 ) {
                    log.append("CryptEncodeObjectEx(SPC_CAB_DATA) failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }
                final MemorySegment spcLinkEnc    = spcLinkEncPtrOut.get(PTR, 0);
                final int           spcLinkEncLen = spcLinkEncLenOut.get(ValueLayout.JAVA_INT, 0);

                // Create the catalog, then add one member per algorithm in CATALOG_HASH_ALGS below.
                //
                // dwPublicVersion=0 (NOT CRYPTCAT_VERSION_2/0x200) - a v2 header unconditionally makes
                // SetupCopyOEMInfW reject the catalog with SPAPI_E_FILE_HASH_NOT_IN_CATALOG, regardless
                // of member dwCertVersion or subjectGuid. Must match dwCertVersion=0 in
                // CryptCATPutMemberInfo below
                final MemorySegment hCatalog = (MemorySegment) _CryptCATOpen.invoke(
                    _wstr(arena, catPath), CRYPTCAT_OPEN_CREATENEW, MemorySegment.NULL, 0, 0
                );
                if(hCatalog == null || hCatalog.address() == 0L || hCatalog.address() == INVALID_HANDLE_VALUE) {
                    log.append("CryptCATOpen failed, GetLastError=").append( _lastError() );
                    return RETCODE_EXCEPTION;
                }

                try {
                    for(int alg = 0; alg < CATALOG_HASH_ALGS.length; ++alg) {
                        final String algName = CATALOG_HASH_ALGS[alg];
                        final String algOid  = CATALOG_HASH_ALG_OIDS[alg];

                        final MemorySegment hCatAdminOut = arena.allocate(PTR);
                        if( (int) _CryptCATAdminAcquireContext2.invoke(hCatAdminOut, _guid(arena, DRIVER_ACTION_VERIFY_PARTS), _wstr(arena, algName), MemorySegment.NULL, 0) == 0 ) {
                            log.append("CryptCATAdminAcquireContext2(").append(algName).append(") failed, GetLastError=").append( _lastError() );
                            return RETCODE_EXCEPTION;
                        }
                        final MemorySegment hCatAdmin = hCatAdminOut.get(PTR, 0);

                        try {
                            final MemorySegment cbHashOut = arena.allocate(ValueLayout.JAVA_INT);
                            _CryptCATAdminCalcHashFromFileHandle2.invoke(hCatAdmin, hInfFile, cbHashOut, MemorySegment.NULL, 0);

                            final int cbHash = cbHashOut.get(ValueLayout.JAVA_INT, 0);
                            if(cbHash <= 0) {
                                log.append("CryptCATAdminCalcHashFromFileHandle2(").append(algName).append(") (sizing) failed, GetLastError=").append( _lastError() );
                                return RETCODE_EXCEPTION;
                            }

                            final MemorySegment hashBuf = arena.allocate(cbHash);
                            if( (int) _CryptCATAdminCalcHashFromFileHandle2.invoke(hCatAdmin, hInfFile, cbHashOut, hashBuf, 0) == 0 ) {
                                log.append("CryptCATAdminCalcHashFromFileHandle2(").append(algName).append(") failed, GetLastError=").append( _lastError() );
                                return RETCODE_EXCEPTION;
                            }

                            final byte[]        hashBytes = hashBuf.toArray(ValueLayout.JAVA_BYTE);
                            final StringBuilder hexTag    = new StringBuilder(hashBytes.length * 2);
                            for(final byte b : hashBytes) hexTag.append( String.format("%02X", b) );

                            log.append("[diag] INF hash (").append(algName).append("): cbHash=").append(cbHash).append(" hexTag=").append(hexTag).append("; ");
                            savedHash[alg]    = hashBuf;
                            savedHashLen[alg] = cbHash;

                            // SIP_INDIRECT_DATA : { CRYPT_ATTRIBUTE_TYPE_VALUE Data; CRYPT_ALGORITHM_IDENTIFIER DigestAlgorithm;
                            //                       CRYPT_HASH_BLOB Digest; } - size 64, align 8 (three {ptr/DWORD-and-pad,ptr}
                            //                       pairs of 24, 24, and 16 bytes respectively)
                            final MemorySegment sipData = arena.allocate(64, 8);
                            sipData.set( PTR                 ,  0, _astr(arena, SPC_CAB_DATA_OBJID) );
                            sipData.set( ValueLayout.JAVA_INT,  8, spcLinkEncLen                    );
                            sipData.set( PTR                 , 16, spcLinkEnc                       );
                            sipData.set( PTR                 , 24, _astr(arena, algOid)             );
                            sipData.set( ValueLayout.JAVA_INT, 32, 0                                );
                            sipData.set( PTR                 , 40, MemorySegment.NULL               );
                            sipData.set( ValueLayout.JAVA_INT, 48, cbHash                           );
                            sipData.set( PTR                 , 56, hashBuf                          );

                            // pwszFileName is passed NULL, not fileName - the member is identified by its
                            // tag/hash, and the filename is separately conveyed via the "File" attribute
                            // below. dwCertVersion=0, matching CryptCATOpen's header version above
                            final MemorySegment pMember = (MemorySegment) _CryptCATPutMemberInfo.invoke(
                                hCatalog, MemorySegment.NULL, _wstr(arena, hexTag.toString() ), subjectGuid, 0, 64, sipData
                            );
                            if(pMember == null || pMember.address() == 0L) {
                                log.append("CryptCATPutMemberInfo(").append(algName).append(") failed, GetLastError=").append( _lastError() );
                                return RETCODE_EXCEPTION;
                            }

                            // "File" member attribute - matches what catalog-verification tooling expects alongside
                            // the SIP-indirect digest, and what WindowsDriverInstaller_PS1's New-FileCatalog
                            // produces.
                            //
                            // No "OSAttr" attribute is added: adding an OS-version-scoped member makes
                            // SetupCopyOEMInfW reject the member entirely (SPAPI_E_FILE_HASH_NOT_IN_CATALOG) rather
                            // than just narrowing its applicability.
                            final MemorySegment fileAttr = (MemorySegment) _CryptCATPutAttrInfo.invoke(
                                hCatalog, pMember, _wstr(arena, "File"),
                                CRYPTCAT_ATTR_AUTHENTICATED | CRYPTCAT_ATTR_NAMEASCII | CRYPTCAT_ATTR_DATAASCII,
                                (fileName.length() + 1) * 2, _wstr( arena, fileName.toLowerCase(Locale.ROOT) )
                            );
                            if(fileAttr == null || fileAttr.address() == 0L) {
                                log.append("CryptCATPutAttrInfo(File,").append(algName).append(") failed, GetLastError=").append( _lastError() );
                                return RETCODE_EXCEPTION;
                            }
                        }
                        finally {
                            _CryptCATAdminReleaseContext.invoke(hCatAdmin, 0);
                        }
                    }
                }
                finally {
                    // CryptCATClose alone does NOT flush the accumulated members/attributes to catPath -
                    // CryptCATPersistStore must be called first to actually write the in-memory catalog
                    // object out to the physical file; skipping it left CryptCATOpen's freshly-created file
                    // on disk at 0 bytes even though every Put* call above reported success, which is why
                    // signtool.exe rejected it as an unrecognized file format.
                    //
                    // Both return values are checked, since either failing here would leave a missing/corrupt
                    // .cat file on disk that would later fail to sign with an unrelated-looking error instead
                    // of surfacing the real problem at its source.
                    catCloseOk = (int) _CryptCATPersistStore.invoke(hCatalog) != 0;
                    catCloseOk = ( (int) _CryptCATClose.invoke(hCatalog) != 0 ) && catCloseOk;
                    _LocalFree.invoke(spcLinkEnc);
                }
            }
            finally {
                _CloseHandle.invoke(hInfFile);
            }
            if(!catCloseOk) {
                log.append("CryptCATClose failed, GetLastError=").append( _lastError() );
                return RETCODE_EXCEPTION;
            }

            // Locate the trusted signing cert (with its persisted private key) in CurrentUser\My
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
                    // Signing is done directly via SignerSignEx (see _signCatalog). Regardless of the
                    // signing outcome, the private key is destroyed immediately afterward - see
                    // _destroyPrivateKey.
                    final int rc = _signCatalog(arena, catPath, signingCert, hMy, log);
                    _destroyPrivateKey(arena, signingCert, log);
                    if(rc != RETCODE_OK) return rc;

                    // Register the finished, signed .cat into the system's catalog DATABASE (an index
                    // mapping file hashes -> the catalogs that contain them) via CryptCATAdminAddCatalog -
                    // documented as "the only supported way to programmatically add catalogs to the
                    // Windows catalog database".
                    //
                    // Writing a valid, correctly-hashed, signed .cat file to disk is not enough on its own:
                    // SetupCopyOEMInfW's own file-hash lookup consults that database, not just any .cat
                    // file sitting next to the INF.
                    final int addRc = _addCatalogToDatabase(arena, catPath, log);
                    if(addRc != RETCODE_OK) return addRc;

                    // Diagnostic only, does not affect the return code: independently ask Windows's own
                    // catalog-database lookup (CryptCATAdminEnumCatalogFromHash - the same API
                    // SetupCopyOEMInfW's internal hash check ultimately calls) whether it can find our
                    // freshly-registered hash, for each algorithm.
                    //
                    // Isolates "the catalog DB entry itself is missing/wrong" from "the DB entry is fine
                    // but SetupCopyOEMInfW rejects it for an unrelated reason (e.g. signature trust)".
                    for(int alg = 0; alg < CATALOG_HASH_ALGS.length; ++alg) {
                        _diagCatalogLookup(arena, CATALOG_HASH_ALGS[alg], savedHash[alg], savedHashLen[alg], log);
                    }
                    _diagVerifyTrustCatalog(arena, catPath, log);
                    return RETCODE_OK;
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
    private static final int SIGNER_CERT_POLICY_CHAIN  = 0x2;
    private static final int SIGNER_NO_ATTR            = 0;
    private static final int SIGNER_AUTHCODE_ATTR      = 1;
    private static final int CALG_SHA_256              = 0x0000800c;

    // SIGNER_SIGNATURE_INFO.psAuthenticated attribute OIDs (wintrust.h) - see the SPC_STATEMENT_TYPE /
    // SPC_SP_OPUS_INFO DER-encoding comments in _signCatalog for the values attached under these OIDs
    private static final String SPC_SP_OPUS_INFO_OBJID   = "1.3.6.1.4.1.311.2.1.12";
    private static final String SPC_STATEMENT_TYPE_OBJID = "1.3.6.1.4.1.311.2.1.11";

    /*
     * Registers a finished, signed .cat file into the system catalog database via CryptCATAdminAddCatalog -
     * see the call site's comment above for why this is needed at all.
     *
     * The algorithm the acquired hCatAdmin context is bound to doesn't matter here (CryptCATAdminAddCatalog
     * reads whichever hash algorithm(s) the catalog file itself declares per-member), so "SHA1" is used
     * arbitrarily.
     */
    private static int _addCatalogToDatabase(final Arena arena, final String catPath, final StringBuilder log) throws Throwable
    {
        final MemorySegment hCatAdminOut = arena.allocate(PTR);
        if( (int) _CryptCATAdminAcquireContext2.invoke(hCatAdminOut, _guid(arena, DRIVER_ACTION_VERIFY_PARTS), _wstr(arena, "SHA1"), MemorySegment.NULL, 0) == 0 ) {
            log.append("CryptCATAdminAcquireContext2(AddCatalog) failed, GetLastError=").append( _lastError() );
            return RETCODE_EXCEPTION;
        }
        final MemorySegment hCatAdmin = hCatAdminOut.get(PTR, 0);

        try {
            final MemorySegment hCatInfo = (MemorySegment) _CryptCATAdminAddCatalog.invoke(
                hCatAdmin, _wstr(arena, catPath), MemorySegment.NULL, 0
            );
            if(hCatInfo == null || hCatInfo.address() == 0L) {
                log.append("CryptCATAdminAddCatalog failed, GetLastError=").append( _lastError() );
                return RETCODE_EXCEPTION;
            }
            _CryptCATAdminReleaseCatalogContext.invoke(hCatAdmin, hCatInfo, 0);
            return RETCODE_OK;
        }
        finally {
            _CryptCATAdminReleaseContext.invoke(hCatAdmin, 0);
        }
    }

    /*
     * Diagnostic helper (see call site's comment) - queries the catalog database directly via
     * CryptCATAdminEnumCatalogFromHash for the given hash/algorithm, logging whether a catalog was
     * found and, if so, which .cat file it points at.
     *
     * Never returns a failure code: any error here is logged and swallowed so it can never mask/replace
     * the real result of catalog creation.
     */
    private static void _diagCatalogLookup(final Arena arena, final String algName, final MemorySegment hashBuf, final int cbHash, final StringBuilder log) throws Throwable
    {
        final MemorySegment hCatAdminOut = arena.allocate(PTR);
        if( (int) _CryptCATAdminAcquireContext2.invoke(hCatAdminOut, _guid(arena, DRIVER_ACTION_VERIFY_PARTS), _wstr(arena, algName), MemorySegment.NULL, 0) == 0 ) {
            log.append("[diag] CryptCATAdminAcquireContext2(lookup,").append(algName).append(") failed, GetLastError=").append( _lastError() ).append("; ");
            return;
        }
        final MemorySegment hCatAdmin = hCatAdminOut.get(PTR, 0);

        try {
            final MemorySegment hCatInfo = (MemorySegment) _CryptCATAdminEnumCatalogFromHash.invoke(
                hCatAdmin, hashBuf, cbHash, 0, MemorySegment.NULL
            );
            if(hCatInfo == null || hCatInfo.address() == 0L) {
                log.append("[diag] CryptCATAdminEnumCatalogFromHash(").append(algName).append("): NOT FOUND, GetLastError=").append( _lastError() ).append("; ");
                return;
            }

            try {
                // CATALOG_INFO : { DWORD cbStruct; WCHAR wszCatalogFile[MAX_PATH]; } - size 4 + 2*MAX_PATH,
                // rounded up to 8-byte alignment since it's passed by pointer only
                final MemorySegment catInfo = arena.allocate(8 + 2L * MAX_PATH, 8);
                catInfo.set( ValueLayout.JAVA_INT, 0, (int) catInfo.byteSize() );
                if( (int) _CryptCATCatalogInfoFromContext.invoke(hCatInfo, catInfo, 0) != 0 ) {
                    final short[] chars = catInfo.asSlice(8, 2L * MAX_PATH).toArray(ValueLayout.JAVA_SHORT);
                    int           nul   = 0;
                    while(nul < chars.length && chars[nul] != 0) nul++;
                    final byte[] utf16Bytes = new byte[nul * 2];
                    for(int i = 0; i < nul; ++i) {
                        utf16Bytes[2 * i]     = (byte) (chars[i] & 0xFF);
                        utf16Bytes[2 * i + 1] = (byte) (chars[i] >> 8);
                    }
                    final String matchedCat = new String(utf16Bytes, java.nio.charset.StandardCharsets.UTF_16LE);
                    log.append("[diag] CryptCATAdminEnumCatalogFromHash(").append(algName).append("): FOUND, catalog=").append(matchedCat).append("; ");
                }
                else {
                    log.append("[diag] CryptCATAdminEnumCatalogFromHash(").append(algName).append("): FOUND (catalog name unavailable); ");
                }
            }
            finally {
                _CryptCATAdminReleaseCatalogContext.invoke(hCatAdmin, hCatInfo, 0);
            }
        }
        finally {
            _CryptCATAdminReleaseContext.invoke(hCatAdmin, 0);
        }
    }

    /*
     * Diagnostic helper (see call site's comment) - runs WinVerifyTrust's standard Authenticode
     * verification policy directly against the finished .cat file on disk, independent of
     * SetupCopyOEMInfW and the catalog database entirely.
     *
     * A non-zero return here means Windows itself does not consider this .cat file's signature trusted
     * (e.g. a chain-trust or EKU problem), which would explain SPAPI_E_FILE_HASH_NOT_IN_CATALOG even
     * though the hash is registered and findable (per _diagCatalogLookup) - SetupCopyOEMInfW's own
     * driver-catalog validation is understood to require BOTH a matching hash AND a trusted signature,
     * and may surface a signature failure as the same generic "hash not in catalog" error rather than
     * a distinct one.
     *
     * Logs the result only, never returns a failure code - this can never mask/replace the real result
     * of catalog creation.
     */
    private static void _diagVerifyTrustCatalog(final Arena arena, final String catPath, final StringBuilder log) throws Throwable
    {
        // WINTRUST_FILE_INFO : { DWORD cbStruct; LPCWSTR pcwszFilePath; HANDLE hFile; GUID *pgKnownSubject; }
        // - size 32, align 8
        final MemorySegment fileInfo = arena.allocate(32, 8);
        fileInfo.set( ValueLayout.JAVA_INT, 0, (int) fileInfo.byteSize() );
        fileInfo.set( PTR,  8, _wstr(arena, catPath) );
        fileInfo.set( PTR, 16, MemorySegment.NULL    );
        fileInfo.set( PTR, 24, MemorySegment.NULL    );

        // WINTRUST_DATA : { DWORD cbStruct; LPVOID pPolicyCallbackData; LPVOID pSIPClientData;
        //                   DWORD dwUIChoice; DWORD fdwRevocationChecks; DWORD dwUnionChoice;
        //                   [4 bytes padding]; LPVOID pFile; DWORD dwStateAction; [4 bytes padding];
        //                   HANDLE hWVTStateData; LPWSTR pwszURLReference; DWORD dwProvFlags;
        //                   DWORD dwUIContext; LPVOID pSignatureSettings; } - size 88, align 8
        final MemorySegment wtData = arena.allocate(88, 8);
        wtData.set( ValueLayout.JAVA_INT,  0, (int) wtData.byteSize() );
        wtData.set( PTR                 ,  8, MemorySegment.NULL      );
        wtData.set( PTR                 , 16, MemorySegment.NULL      );
        wtData.set( ValueLayout.JAVA_INT, 24, WTD_UI_NONE             );
        wtData.set( ValueLayout.JAVA_INT, 28, WTD_REVOKE_NONE         );
        wtData.set( ValueLayout.JAVA_INT, 32, WTD_CHOICE_FILE         );
        wtData.set( PTR                 , 40, fileInfo                );
        wtData.set( ValueLayout.JAVA_INT, 48, WTD_STATEACTION_VERIFY  );
        wtData.set( PTR                 , 56, MemorySegment.NULL      );
        wtData.set( PTR                 , 64, MemorySegment.NULL      );
        wtData.set( ValueLayout.JAVA_INT, 72, 0                       );
        wtData.set( ValueLayout.JAVA_INT, 76, 0                       );
        wtData.set( PTR                 , 80, MemorySegment.NULL      );

        final MemorySegment actionGuid = _guid(arena, WINTRUST_ACTION_GENERIC_VERIFY_V2_PARTS);
        final int            trustResult = (int) _WinVerifyTrust.invoke(MemorySegment.NULL, actionGuid, wtData);
        log.append("[diag] WinVerifyTrust(catalog): result=").append(trustResult).append( trustResult == 0 ? " (TRUSTED)" : " (NOT TRUSTED)" ).append("; ");

        // Release the state WinVerifyTrust allocated for this verification, per its documented pattern
        wtData.set(ValueLayout.JAVA_INT, 48, WTD_STATEACTION_CLOSE);
        _WinVerifyTrust.invoke(MemorySegment.NULL, actionGuid, wtData);
    }

    // Applies the Authenticode signature to the finished .cat file directly via SignerSignEx - see the
    // struct-field comments below and WindowsDriverInstaller_FFM-Win32API.txt's "SignerSignEx" entry.
    private static int _signCatalog(final Arena arena, final String catPath, final MemorySegment signingCert, final MemorySegment hMy, final StringBuilder log) throws Throwable
    {
        // SIGNER_FILE_INFO : { DWORD cbSize; LPCWSTR pwszFileName; HANDLE hFile; } - size 24, align 8
        final MemorySegment fileInfo = arena.allocate(24, 8);
        fileInfo.set( ValueLayout.JAVA_INT,  0, 24                    );
        fileInfo.set( PTR                 ,  8, _wstr(arena, catPath) );
        fileInfo.set( PTR                 , 16, MemorySegment.NULL    );

        // SIGNER_SUBJECT_INFO : { DWORD cbSize; DWORD *pdwIndex; DWORD dwSubjectChoice; union{ SIGNER_FILE_INFO* }; } - size 32, align 8
        // "must be set to zero" describes the pointed-to DWORD's *value*, not the pointer field itself:
        // pdwIndex must point at a real, writable zero-valued DWORD, not be NULL - see
        // WindowsDriverInstaller_FFM-Win32API.txt's "SignerSignEx" entry
        final MemorySegment pdwIndex = arena.allocate(ValueLayout.JAVA_INT);
        pdwIndex.set(ValueLayout.JAVA_INT, 0, 0);

        final MemorySegment subjectInfo = arena.allocate(32, 8);
        subjectInfo.set(ValueLayout.JAVA_INT,  0, 32                 );
        subjectInfo.set( PTR                ,  8, pdwIndex           );
        subjectInfo.set(ValueLayout.JAVA_INT, 16, SIGNER_SUBJECT_FILE);
        subjectInfo.set(PTR                 , 24, fileInfo           );

        // SIGNER_CERT_STORE_INFO : { DWORD cbSize; PCCERT_CONTEXT pSigningCert; DWORD dwCertPolicy; HCERTSTORE hCertStore; } - size 32, align 8
        // hCertStore is only consulted when dwCertPolicy is SIGNER_CERT_POLICY_STORE (search hCertStore for
        // a matching cert) or SIGNER_CERT_POLICY_CHAIN_NO_ROOT; with dwCertPolicy=SIGNER_CERT_POLICY_CHAIN
        // (build the chain from pSigningCert alone) it is unused and must be NULL - VERIFIED against
        // https://learn.microsoft.com/en-us/windows/win32/seccrypto/signer-cert-store-info and
        // WindowsDriverInstaller_FFM-Win32API.txt's "SignerSignEx" entry
        final MemorySegment certStoreInfo = arena.allocate(32, 8);
        certStoreInfo.set(ValueLayout.JAVA_INT,  0, 32                      );
        certStoreInfo.set(PTR                 ,  8, signingCert             );
        certStoreInfo.set(ValueLayout.JAVA_INT, 16, SIGNER_CERT_POLICY_CHAIN);
        certStoreInfo.set(PTR                 , 24, MemorySegment.NULL     );

        // SIGNER_CERT : { DWORD cbSize; DWORD dwCertChoice; union{...}; HWND hwnd; } - size 24, align 8
        // VERIFIED against https://learn.microsoft.com/en-us/windows/win32/seccrypto/signer-cert
        // (see WindowsDriverInstaller_FFM-Win32API.txt) - this layout was already correct
        final MemorySegment signerCert = arena.allocate(24, 8);
        signerCert.set(ValueLayout.JAVA_INT,  0, 24                );
        signerCert.set(ValueLayout.JAVA_INT,  4, SIGNER_CERT_STORE );
        signerCert.set(PTR                 ,  8, certStoreInfo     );
        signerCert.set(PTR                 , 16, MemorySegment.NULL);

        // SPC_STATEMENT_TYPE ::= SEQUENCE OF OBJECT IDENTIFIER - DER-encoded SEQUENCE containing exactly
        // SPC_INDIVIDUAL_SP_KEY_PURPOSE_OBJID (1.3.6.1.4.1.311.2.1.21), the "individual" (non-commercial)
        // key-purpose signtool.exe emits when signing with a non-commercial/self-signed certificate
        // Source: "Windows Authenticode Portable Executable Signature Format" (Microsoft) -
        // https://download.microsoft.com/download/9/c/5/9c5b2167-8017-4bae-9fde-d599bac8184a/authenticode_pe.docx
        final byte[] statementTypeDer = {
            (byte) 0x30, (byte) 0x0C, (byte) 0x06, (byte) 0x0A, (byte) 0x2B, (byte) 0x06,
            (byte) 0x01, (byte) 0x04, (byte) 0x01, (byte) 0x82, (byte) 0x37, (byte) 0x02,
            (byte) 0x01, (byte) 0x15
        };
        // SPC_SP_OPUS_INFO ::= SEQUENCE { programName [0] EXPLICIT SpcString OPTIONAL;
        //                                 moreInfo [1] EXPLICIT SpcLink OPTIONAL; } - both fields are
        // OPTIONAL and this backend has neither a program name nor a more-info URL to offer, so the
        // minimal valid encoding is an empty SEQUENCE. Same Microsoft source as above
        final byte[] opusInfoDer = { (byte) 0x30, (byte) 0x00 };

        final MemorySegment statementTypeBuf = arena.allocate(statementTypeDer.length);
        MemorySegment.copy(statementTypeDer, 0, statementTypeBuf, ValueLayout.JAVA_BYTE, 0, statementTypeDer.length);

        final MemorySegment opusInfoBuf = arena.allocate(opusInfoDer.length);
        MemorySegment.copy(opusInfoDer, 0, opusInfoBuf, ValueLayout.JAVA_BYTE, 0, opusInfoDer.length);

        final MemorySegment opusInfoBlob      = _blob(arena, opusInfoDer.length, opusInfoBuf);
        final MemorySegment statementTypeBlob = _blob(arena, statementTypeDer.length, statementTypeBuf);

        // CRYPT_ATTRIBUTE : { LPSTR pszObjId; DWORD cValue; PCRYPT_ATTR_BLOB rgValue; } - size 24, align 8
        final MemorySegment opusInfoAttr = arena.allocate(24, 8);
        opusInfoAttr.set(PTR                 ,  0, _astr(arena, SPC_SP_OPUS_INFO_OBJID) );
        opusInfoAttr.set(ValueLayout.JAVA_INT,  8, 1                                    );
        opusInfoAttr.set(PTR                 , 16, opusInfoBlob                         );

        final MemorySegment statementTypeAttr = arena.allocate(24, 8);
        statementTypeAttr.set(PTR                 ,  0, _astr(arena, SPC_STATEMENT_TYPE_OBJID) );
        statementTypeAttr.set(ValueLayout.JAVA_INT,  8, 1                                      );
        statementTypeAttr.set(PTR                 , 16, statementTypeBlob                      );

        // CRYPT_ATTRIBUTES.rgAttr is a contiguous array (not a linked list) of CRYPT_ATTRIBUTE
        final MemorySegment attrArray = arena.allocate(24L * 2, 8);
        MemorySegment.copy(opusInfoAttr,      0, attrArray,  0, 24);
        MemorySegment.copy(statementTypeAttr, 0, attrArray, 24, 24);

        // CRYPT_ATTRIBUTES : { DWORD cAttr; PCRYPT_ATTRIBUTE rgAttr; } - size 16, align 8
        final MemorySegment authenticatedAttrs = arena.allocate(16, 8);
        authenticatedAttrs.set(ValueLayout.JAVA_INT, 0, 2        );
        authenticatedAttrs.set(PTR                 , 8, attrArray);

        // SIGNER_SIGNATURE_INFO : { DWORD cbSize; ALG_ID algidHash; DWORD dwAttrChoice; union{...};
        //                           PCRYPT_ATTRIBUTES_ARRAY psAuthenticated; PCRYPT_ATTRIBUTES_ARRAY psUnauthenticated; } - size 40, align 8
        // dwAttrChoice stays SIGNER_NO_ATTR (union at offset 16 unused; a real value there is only needed
        // when dwAttrChoice=SIGNER_AUTHCODE_ATTR). The OPUS_INFO/STATEMENT_TYPE authenticated attributes
        // built above are attached at psAuthenticated (offset 24) - see
        // WindowsDriverInstaller_FFM-Win32API.txt's "SignerSignEx" entry.
        final MemorySegment sigInfo = arena.allocate(40, 8);
        sigInfo.set(ValueLayout.JAVA_INT,  0, 40                );
        sigInfo.set(ValueLayout.JAVA_INT,  4, CALG_SHA_256      );
        sigInfo.set(ValueLayout.JAVA_INT,  8, SIGNER_NO_ATTR    );
        sigInfo.set(PTR                 , 16, MemorySegment.NULL);
        sigInfo.set(PTR                 , 24, authenticatedAttrs);
        sigInfo.set(PTR                 , 32, MemorySegment.NULL);

        final MemorySegment ppSignerContext = arena.allocate(PTR);

        // SignerSignEx (the original, simplest entry point in this family - no timestamp-flags DWORD,
        // no crypto-policy or digest-sign-info parameters)
        final int hr = (int) _SignerSignEx.invoke(
            0, subjectInfo, signerCert, sigInfo, MemorySegment.NULL,
            MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL, ppSignerContext
        );

        // Captured unconditionally, before SignerFreeSignerContext can touch anything, so the CI log
        // carries GetLastError() and the raw struct bytes regardless of which failure mode this is
        final int lastError = _lastError();

        final MemorySegment signerContext = ppSignerContext.get(PTR, 0);
        if(signerContext != null && signerContext.address() != 0L) _SignerFreeSignerContext.invoke(signerContext);

        if(hr != 0) {
            log.append("SignerSignEx failed, HRESULT=0x").append( Integer.toHexString(hr) )
               .append(", GetLastError=").append( lastError ).append('\n')
               .append("  subjectInfo    : ").append( _hexDump(subjectInfo, 32)        ).append('\n')
               .append("  signerCert     : ").append( _hexDump(signerCert, 24)         ).append('\n')
               .append("  certStoreInfo  : ").append( _hexDump(certStoreInfo, 32)      ).append('\n')
               .append("  sigInfo        : ").append( _hexDump(sigInfo, 40)            ).append('\n')
               .append("  fileInfo       : ").append( _hexDump(fileInfo, 24)           ).append('\n')
               .append("  authAttrs      : ").append( _hexDump(authenticatedAttrs, 16) ).append('\n')
               .append("  attrArray      : ").append( _hexDump(attrArray, 48)          );
            return RETCODE_EXCEPTION;
        }

        return RETCODE_OK;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int MAX_PATH   = 260; // minwindef.h
    private static final int SPOST_NONE = 0;   // setupapi.h - no OEMSourceMediaLocation applies
    private static final int SPOST_PATH = 1;   // setupapi.h

    /*
     * Stages infPath into %windir%\Inf via SetupCopyOEMInfW (setupapi.h), guarded by
     * CM_WaitNoPendingInstallEvents (cfgmgr32.h) so this doesn't race a concurrent PnP device
     * installation already in progress.
     *
     * No device needs to be physically present for this to succeed - staging alone makes the driver
     * available to Windows, and an already-connected matching device simply needs to be replugged to
     * pick it up (PnP does this automatically), matching the behavior WindowsDriverInstaller_PS1
     * already falls back to on Windows 7, where "install onto an already-connected device immediately"
     * isn't supported at all.
     */
    private static int _elevatedInstallDriver(final String infPath, final StringBuilder log) throws Throwable
    {
        final Path path = Paths.get(infPath);

        if( !infPath.toLowerCase(Locale.ROOT).endsWith(".inf") || !path.isAbsolute() || !Files.exists(path) ) {
            log.append( String.format(Texts.EMsg_WDriverInstallInvInfPth, infPath) );
            return RETCODE_INVALID_PATH;
        }

        /*
        // An earlier revision of this method shelled out to pnputil.exe /add-driver, matching
        // WindowsDriverInstaller_PS1's approach, but that reliably hung waiting for the process to exit
        // regardless of how its output was captured. The implementation below instead calls
        // SetupCopyOEMInfW in-process directly, with OEMSourceMediaType=SPOST_PATH and the INF's own
        // parent directory as the source location - confirmed working end-to-end. (This parameter
        // combination was identified by reading Microsoft's own SetupCopyOEMInfW/OEMSourceMediaType
        // documentation and observing how other open-source driver installers call this same
        // documented Win32 API; no code was copied from anywhere.) The old pnputil.exe-based
        // implementation is kept below, commented out rather than deleted, in case this needs to be
        // swapped back in.

        final Path tmpOutLog = Paths.get( System.getenv("TEMP"), "pnp_out_" + ProcessHandle.current().pid() + ".log" );

        try {
            final ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/v:on", "/c",
                "pnputil.exe /add-driver \"" + infPath + "\" > \"" + tmpOutLog + "\" 2>&1 & exit !errorlevel!"
            );
            final Process process = pb.start();

            // Same finite-timeout rationale as the old CM_WaitNoPendingInstallEvents guard below : never
            // wait indefinitely, in case pnputil.exe itself ever blocks on something headlessly undismissable.
            if( !process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES) ) {
                process.destroyForcibly();
                log.append("pnputil.exe /add-driver timed out after 5 minutes\n");
                return RETCODE_EXCEPTION;
            }

            final int exitCode = process.exitValue();

            if( Files.exists(tmpOutLog) ) {
                log.append( Files.readString(tmpOutLog, StandardCharsets.UTF_8) );
                Files.deleteIfExists(tmpOutLog);
            }

            if(exitCode != 0) {
                log.append("pnputil.exe /add-driver failed, exit code=").append(exitCode);
                return RETCODE_EXCEPTION;
            }

            return RETCODE_OK;
        }
        catch(final IOException e) {
            log.append("pnputil.exe /add-driver failed to launch: ").append(e);
            return RETCODE_EXCEPTION;
        }
        */

        try(
            final Arena arena = Arena.ofConfined()
        ) {
            // CM_WaitNoPendingInstallEvents(dwTimeout) : blocks until the PnP manager reports no
            // install activity pending, or dwTimeout milliseconds elapse - whichever comes first.
            //
            // Returns 0 (WAIT_OBJECT_0) if satisfied, nonzero (WAIT_TIMEOUT=0x102, or WAIT_FAILED=
            // 0xFFFFFFFF) on timeout/failure - the standard Win32 wait-function convention.
            //
            // A finite timeout is used (not INFINITE) so a stuck concurrent installer elsewhere on
            // the machine cannot cause an indefinite hang here; a timeout is logged but does not
            // abort the SetupCopyOEMInfW call below.
            final int waitResult = (int) _CM_WaitNoPendingInstallEvents.invoke(60_000);
            if(waitResult != 0) {
                log.append("CM_WaitNoPendingInstallEvents timed out after 60000ms - proceeding anyway\n");
            }

            // SetupSetNonInteractiveMode(TRUE) : suppresses any interactive UI SetupCopyOEMInfW might
            // otherwise try to raise (e.g. an unverified-publisher/unsigned-driver confirmation dialog).
            //
            // A headless CI runner has no interactive desktop session and this thread has no Windows
            // message pump, so such a dialog would block forever with nothing able to dismiss it.
            _SetupSetNonInteractiveMode.invoke(1);

            final MemorySegment sourceInfFileName      = _wstr(arena, infPath);
            final MemorySegment sourceMediaLocation    = _wstr( arena, path.getParent().toString() );
            final MemorySegment destinationInfFileName = arena.allocate(2L * MAX_PATH, 2);
            final MemorySegment requiredSize           = arena.allocate(ValueLayout.JAVA_INT);
            final MemorySegment captureState           = arena.allocate(_CAPTURE_STATE_LAYOUT);

            // SetupCopyOEMInfW(SourceInfFileName, OEMSourceMediaLocation, OEMSourceMediaType, CopyStyle,
            //                  DestinationInfFileName, DestinationInfFileNameSize, RequiredSize,
            //                  DestinationInfFileNameComponent)
            // OEMSourceMediaLocation=infPath's own parent directory / OEMSourceMediaType=SPOST_PATH -
            // NOT SPOST_NONE/NULL, which SetupCopyOEMInfW rejects with SPAPI_E_FILE_HASH_NOT_IN_CATALOG.
            //
            // A real source directory with SPOST_PATH is required even when the INF path is already
            // fully qualified - see WindowsDriverInstaller_FFM-Win32API.txt for background. CopyStyle=0 : default
            // behavior (overwrite an existing same-named staged copy, auto-rename the staged copy to
            // OEMnnnn.inf).
            //
            // captureState is a leading, implicit extra argument - see the Linker.Option.captureCallState
            // binding above and _capturedLastError() below (do NOT use _lastError() here, it reads GetLastError()
            // via a separate downcall that is not guaranteed to still hold this call's error - see the static
            // initializer comment on this binding).
            final int ok = (int) _SetupCopyOEMInfW.invoke(
                captureState,
                sourceInfFileName, sourceMediaLocation, SPOST_PATH, 0,
                destinationInfFileName, MAX_PATH, requiredSize, MemorySegment.NULL
            );

            if(ok == 0) {
                final int err = _capturedLastError(captureState);
                log.append("SetupCopyOEMInfW failed, GetLastError=").append(err);
                return RETCODE_EXCEPTION;
            }

            // DestinationInfFileName is UTF-16LE, NUL-terminated - MemorySegment has no getUtf16String(),
            // so decode manually up to the first NUL char within the buffer we allocated.
            final short[] chars = destinationInfFileName.toArray(ValueLayout.JAVA_SHORT);
            int           nul   = 0;
            while(nul < chars.length && chars[nul] != 0) nul++;
            final byte[] utf16Bytes = new byte[nul * 2];
            for(int i = 0; i < nul; i++) {
                utf16Bytes[2 * i    ] = (byte) (  chars[i]       & 0xFF );
                utf16Bytes[2 * i + 1] = (byte) ( (chars[i] >> 8) & 0xFF );
            }
            log.append( new String(utf16Bytes, StandardCharsets.UTF_16LE) );

            return RETCODE_OK;
        }
    }

} // WindowsDriverInstaller_FFM
