import jxm.WindowsDriverInstaller;
import jxm.WindowsDriverInstaller_PS1;
import jxm.WindowsDriverInstaller_FFM;
import jxm.xb.XCom;

public class WDI_CITest {

    private static final String PROVIDER = "JxMake_CITest";

    public static void main(final String[] args) throws Exception {
        final String  backend      = System.getProperty("wdi.backend", "auto");
        final boolean runMutating  = Boolean.getBoolean("wdi.mutating");

        int failures = 0;

        switch( backend ) {
            case "ps1":
                failures += test("PS1", new WindowsDriverInstaller_PS1(), runMutating);
                break;

            case "ffm":
                failures += test("FFM", new WindowsDriverInstaller_FFM(), runMutating);
                break;

            case "compare":
                compare();
                break;

            // Tests exactly one backend directly (never via WindowsDriverInstaller.create(),
            // which would only ever exercise whichever one it prefers on this workflow's one
            // fixed JDK version). Which backend is chosen by -Dwdi.driverbackend=PS1|FFM, set by
            // the test-drivers job below - each backend gets run in this mode on its own fresh
            // VM, deliberately never both in the same VM/process (see that job's own comment).
            case "drivers": {
                final String driverBackend = System.getProperty("wdi.driverbackend", "");
                final WindowsDriverInstaller wdi;
                     if( "PS1".equals(driverBackend) ) wdi = new WindowsDriverInstaller_PS1();
                else if( "FFM".equals(driverBackend) ) wdi = new WindowsDriverInstaller_FFM();
                else throw new IllegalArgumentException("drivers mode requires -Dwdi.driverbackend=PS1|FFM, got: " + driverBackend);

                failures += testDriverKindsOn(driverBackend, wdi, runMutating);
                break;
            }

            case "auto": {
                final WindowsDriverInstaller picked = WindowsDriverInstaller.create();
                final String label = "AUTO_" + (picked == null ? "null" : picked.getClass().getSimpleName());
                System.out.println("=== create() factory pick ===");
                System.out.println("create() returned: " + (picked == null ? "null" : picked.getClass().getSimpleName()));

                if( picked == null ) {
                    System.out.println("No backend usable on this VM");
                    failures++;
                } else {
                    failures += test(label, picked, runMutating);
                }
                break;
            }

            default:
                throw new IllegalArgumentException("Unknown wdi.backend: " + backend);
        }

        if( failures > 0 ) {
            System.out.println();
            System.out.println(failures + " backend(s) reported a hard failure - see above");
            System.exit(1);
        }
    }

    private static int test(final String label, final WindowsDriverInstaller wdi, final boolean runMutating) {
        System.out.println();
        System.out.println("=== " + label + " backend ===");

        // Distinct per-backend provider name: PS1 and FFM both run against the same
        // CurrentUser\My store in this one process/VM, and CertAddCertificateContextToStore's
        // CERT_STORE_ADD_REPLACE_EXISTING only replaces a certificate with the same
        // issuer+serial, not merely the same subject text - so sharing PROVIDER here left two
        // unrelated self-signed certificates both named "JxMake_CITest" in the store by the
        // time FFM ran, and CertFindCertificateInStore's match order against that name is not
        // guaranteed (per Microsoft's own note on CertAddCertificateContextToStore). Giving
        // each backend its own name removes that cross-backend ambiguity from the test.
        final String provider = PROVIDER + "_" + label;

        try {
            final boolean usable = wdi.isUsable();
            System.out.println(label + ".isUsable() = " + usable);

            if( !usable ) {
                if( wdi instanceof WindowsDriverInstaller_FFM ) {
                    final String diag = WindowsDriverInstaller_FFM.initErrorDiagnostic();
                    if( diag != null ) System.out.println(label + ": static init failed - " + diag);
                }
                System.out.println(label + " not usable on this VM - skipping remaining checks");
                return 0;
            }

            printPair(label + ".isProviderAlreadyTrusted", wdi.isProviderAlreadyTrusted(provider));

            if( runMutating ) {
                printPair(label + ".createAndTrustProvider", wdi.createAndTrustProvider(provider));
                printPair(label + ".isProviderAlreadyTrusted (after trust)", wdi.isProviderAlreadyTrusted(provider));

                final java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("wdi_ci");
                final String catName = "wdi_ci_test.cat";
                final java.nio.file.Path inf = dir.resolve("wdi_ci_test.inf");
                java.nio.file.Files.writeString(inf, WindowsDriverInstaller.generateWinUSBInf("1234", "5678", catName));

                printPair(label + ".createAndSignCatalog", wdi.createAndSignCatalog(inf.toString(), provider));
                printPair(label + ".installDriver", wdi.installDriver(inf.toString()));
            } else {
                System.out.println(label + ": skipping mutating/elevated calls (run_mutating=false)");
            }

            return 0;
        }
        catch(final Throwable t) {
            System.out.println(label + ": EXCEPTION - " + t);
            t.printStackTrace(System.out);
            return 1;
        }
    }

    // "drivers" mode: unlike every other mode (which only ever exercises generateWinUSBInf()
    // as a driver-kind-agnostic fixture for the PS1/FFM primitives), this calls the
    // installLibusbKInf()/installLibusb0Inf() convenience wrappers directly - the only path
    // that exercises SysUtil.findWindowsDriverDir() actually resolving the bundled .sys next
    // to the checked-out 3rd_party/windows_driver directory on a real runner, and copying it
    // alongside the generated INF before signing (see WindowsDriverInstaller._copyBundledSysFile()).
    //
    // Called with exactly one backend, on its own fresh VM (see the "drivers" case above and the
    // test-drivers job) - no cross-backend provider-name/PID collision concerns here, unlike
    // compare()'s same-VM PS1-then-FFM run below.
    private static int testDriverKindsOn(final String label, final WindowsDriverInstaller wdi, final boolean runMutating) {
        System.out.println();
        System.out.println("=== " + label + " driver-kind wrappers (libusbK, libusb0) ===");

        final boolean usable = wdi.isUsable();
        System.out.println(label + ".isUsable() = " + usable);
        if( !usable ) return 0;

        if( !runMutating ) {
            System.out.println("skipping (run_mutating=false) - installLibusbKInf()/installLibusb0Inf() are always mutating (trust+sign+install)");
            return 0;
        }

        int failures = 0;

        try {
            printPair(label + " installLibusbKInf", wdi.installLibusbKInf("1234", "5678"));
        }
        catch(final Throwable t) {
            System.out.println(label + " installLibusbKInf: EXCEPTION - " + t);
            t.printStackTrace(System.out);
            failures++;
        }

        try {
            printPair(label + " installLibusb0Inf", wdi.installLibusb0Inf("1234", "5679"));
        }
        catch(final Throwable t) {
            System.out.println(label + " installLibusb0Inf: EXCEPTION - " + t);
            t.printStackTrace(System.out);
            failures++;
        }

        return failures;
    }

    private static void printPair(final String label, final XCom.Pair<Integer, String> p) {
        System.out.println(label + " = [" + p.first() + "] " + p.second());
    }

    // "compare" mode: runs PS1 then FFM against distinct provider names, but with both
    // producing an identically-named INF/CAT pair (only the output directory differs) and an
    // identically-named cert file (%TEMP%\<provider>.cer, same convention both backends use),
    // deliberately never calling installDriver() - the point is comparing what each backend
    // GENERATES (INF, .cat, .cer), not exercising the still-unresolved install path. Every
    // output file is copied into wdi_compare_out/ with a PS1-/FFM- prefix so a later workflow
    // step can upload the whole directory as one artifact for offline diffing.
    private static void compare() throws Exception {
        final java.nio.file.Path outDir = java.nio.file.Paths.get("wdi_compare_out");
        java.nio.file.Files.createDirectories(outDir);

        compareOne("PS1", new WindowsDriverInstaller_PS1(), outDir);
        compareOne("FFM", new WindowsDriverInstaller_FFM(), outDir);

        System.out.println();
        System.out.println("Compare output written to: " + outDir.toAbsolutePath());
    }

    private static void compareOne(final String prefix, final WindowsDriverInstaller wdi, final java.nio.file.Path outDir) throws Exception {
        System.out.println();
        System.out.println("=== " + prefix + " compare ===");

        final boolean usable = wdi.isUsable();
        System.out.println(prefix + ".isUsable() = " + usable);
        if( !usable ) {
            if( wdi instanceof WindowsDriverInstaller_FFM ) {
                final String diag = WindowsDriverInstaller_FFM.initErrorDiagnostic();
                if( diag != null ) System.out.println(prefix + ": static init failed - " + diag);
            }
            System.out.println(prefix + " not usable on this VM - skipping");
            return;
        }

        // Distinct provider name per backend for the same reason "test" uses one - see its
        // own comment above - but same PROVIDER prefix and a fixed "_CMP_" tag so re-running
        // this mode never accumulates unrelated certs across runs/modes.
        final String provider = PROVIDER + "_CMP_" + prefix;

        printPair(prefix + ".createAndTrustProvider", wdi.createAndTrustProvider(provider));

        // Both backends write the public cert here - WindowsDriverInstaller_PS1's
        // createAndTrustProvider() always has; WindowsDriverInstaller_FFM's now does too.
        final java.nio.file.Path certSrc = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), provider + ".cer");
        copyIfExists(certSrc, outDir.resolve(prefix + "-cert.cer"));

        final java.nio.file.Path dir     = java.nio.file.Files.createTempDirectory("wdi_cmp_" + prefix);
        final String             catName = "wdi_cmp.cat";
        final java.nio.file.Path inf     = dir.resolve("wdi_cmp.inf");
        java.nio.file.Files.writeString(inf, WindowsDriverInstaller.generateWinUSBInf("1234", "5678", catName));

        printPair(prefix + ".createAndSignCatalog", wdi.createAndSignCatalog(inf.toString(), provider));

        copyIfExists(inf, outDir.resolve(prefix + "-wdi_cmp.inf"));
        copyIfExists(dir.resolve(catName), outDir.resolve(prefix + "-wdi_cmp.cat"));

        // Deliberately no installDriver() call here - see this method's own header comment.
    }

    private static void copyIfExists(final java.nio.file.Path src, final java.nio.file.Path dst) throws java.io.IOException {
        if( java.nio.file.Files.exists(src) ) {
            java.nio.file.Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied " + src + " -> " + dst);
        } else {
            System.out.println("MISSING: " + src);
        }
    }

} // class WDI_CITest
