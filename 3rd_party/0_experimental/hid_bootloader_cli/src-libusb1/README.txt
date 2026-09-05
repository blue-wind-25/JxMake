========================================================================================================================
GitHub CI Build
========================================================================================================================

Workflow: .github/workflows/build-hid-bootloader-cli.yml (repo root, since this is a monorepo)

Builds the full portability matrix below without needing any of the local cross-toolchains
described further down in this file:

Target                   Arch                      Notes
----------------------   ------------------------  ---------------------------------------------------------------------
Linux                    x86, amd64, arm32, arm64  static, musl libc (no glibc version dependency)


Windows (HidUsb)         x86, amd64, arm64         native MSVC on windows-2022, /MT + Win7 API floor (see notes below)
Windows (WinUSB/libusb)  x86, amd64                native MSVC on windows-2022, same /MT + Win7 floor

MacOS                    x86_64                    native Xcode clang on macos-latest, -mmacosx-version-min=10.13
MacOS (universal)        x86_64 + arm64            native Xcode clang on macos-latest, -mmacosx-version-min=11.0

FreeBSD                  x64                       static, libusb built from source, FreeBSD 13.2 (vmactions/freebsd-vm)
OpenBSD                  x64                       static, libusb built from source, OpenBSD 7.8 (vmactions/openbsd-vm)

All targets have been built by CI at least once; their binaries are checked in under `build/ci/`.

------------------------------------------------------------------------------------------------------------------------
Artifact naming
------------------------------------------------------------------------------------------------------------------------

`build/ci/<target>/` naming mirrors the local `make` targets described in the rest of this file:

    - `-static` suffix : matches the local `-static` mkblob suffix - Linux, FreeBSD and OpenBSD CI builds are all fully
                         static, so they all carry it.

    - `-libusb` suffix : matches the local `xwin-libusb` target - the WinUSB/libusb Windows build, as opposed to the
                         default HidUsb one.

    - everything else  : dynamically linked (Windows' MSVC CRT is still dynamic even with `/MT` statically linking the
                         *C* runtime - `/MT` only removes the redistributable-version dependency, see notes below) or,
                         for MacOS, not meaningfully "static" at all on that platform.

------------------------------------------------------------------------------------------------------------------------
Dispatching the workflow
------------------------------------------------------------------------------------------------------------------------

Manual dispatch only (Actions tab -> "Build hid_bootloader_cli" -> "Run workflow"), gated to the repo owner.

    - `target: <one target>` builds just that one target, for debugging a single toolchain issue without the whole
      matrix failing together. Fix it, re-dispatch with the same target, repeat until green.
    - `target: all` builds everything and produces one packaged download: each target's binary is collected into its
      own `build/ci/<target>/` subdirectory (mirroring the layout above), the Unix executable bit is restored
      (round-tripping through GitHub artifacts / a zip doesn't reliably preserve it), and the whole tree is packed into
      `build/hid_bootloader_cli-<version>-all-targets.tar.bz2` (tar instead of zip, so `+x` actually survives
      extraction), uploaded as the `package` job's artifact.

Re-dispatching `target: all` and re-copying `build/ci/` from the downloaded artifact is how to refresh the checked-in
binaries as runner images and toolchains drift upstream (e.g. a newer windows-2022 VS servicing update, a newer
macos-latest Xcode) - don't hand-edit the binaries in place.

------------------------------------------------------------------------------------------------------------------------
Notes / known limitations
------------------------------------------------------------------------------------------------------------------------

  - Linux targets use static musl builds (same "fully static, runs anywhere" idea as this Makefile's `mkblob`/`-static`
    trick for linux-x64) instead of chasing "oldest glibc", which is fragile because glibc symbol versioning ties the
    binary to the build host.

  - Windows runs on windows-2022 (Server 2022 / VS2022 toolset), not windows-latest (currently windows-2025 / VS "18")
    - an older, still-supported hosted image whose toolset lineage is closer to a Windows 10 end-user machine. Windows 7
    compatibility is then handled explicitly at compile time:
      - `/MT` statically links the MSVC CRT, so the binary doesn't depend on the matching Visual C++ Redistributable
        being installed on the target machine (the actual #1 cause of "won't run on an old Windows box").
      - `/D_WIN32_WINNT=0x0601` caps the Windows API level compiled against at Windows 7, so no Windows 8+-only API
        accidentally slips in.
      - `/SUBSYSTEM:CONSOLE,6.01` stamps the PE header's minimum OS version as Windows 7 instead of whatever the toolset
        defaults to, so Windows won't refuse to launch it.
    These floors are moot for windows-arm64 (ARM64 Windows only ever shipped as Windows 10+), but `/MT` still applies
    there for the same redistributable reason.

  - Windows builds both HidUsb (-DUSE_WIN32, SetupAPI/hid.lib - same as this Makefile's `xwin` target) and WinUSB via
    libusb (-DUSE_LIBUSB - same as `xwin-libusb`), the latter built with a vcpkg-built static libusb (x86/x64 only,
    see below) using the `<arch>-windows-static` triplet - vcpkg is preinstalled on the GitHub-hosted Windows runner,
    so this needs no extra toolchain install. HidUsb is the standard HID class driver built into Windows, so it works
    immediately on any Windows 7-11 machine with no driver changes; WinUSB only sees the device after the end user
    manually re-drivers it with Zadig (HidUsb -> WinUSB), so it's published as an explicit opt-in alternative, not the
    default.

  - Windows WinUSB/libusb is x86/x64 only: an `arm64-windows-static` libusb build via vcpkg is unverified here. ARM64
    already gets HidUsb, which needs no extra driver on the target machine anyway, so this isn't a real gap worth
    chasing.

  - Windows ARM32 is NOT built at all: the GitHub-hosted Windows VS installs no longer ship a 32-bit ARM toolset (only
    x86/amd64/arm64), and 32-bit ARM Windows hardware is essentially extinct anyway (superseded by ARM64 since ~2017).

  - MacOS ships two artifacts because a universal (fat) binary can't declare a version floor lower than its
    oldest-supported slice, and Apple Silicon never shipped a macOS older than 11.0: `macos-x86_64` (Intel-only,
    floor 10.13) covers older/non-Apple-Silicon Macs, while `macos-universal` (x86_64 + arm64, floor 11.0) covers
    everything from Big Sur on.

  - MacOS is built on real GitHub-hosted macOS runners rather than osxcross, because osxcross needs the Xcode SDK,
    which can't be auto-downloaded in CI (Apple EULA/licensing).

  - FreeBSD/OpenBSD build libusb from source and link it (and libc) statically rather than using the base/port
    libusb.so, since that .so is tied to the exact release it was built on - OpenBSD in particular gives no ABI
    stability guarantee across releases.

  - FreeBSD/OpenBSD are pinned to older point releases (13.2 / 7.8) rather than each project's newest stable, purely
    for build-toolchain portability/reproducibility - the output itself is fully static (libc included), so it doesn't
    actually depend on the release it was built on. OpenBSD only supports its two most recent releases and DELETES
    the package mirror for anything older (unlike FreeBSD, which archives it), so the OpenBSD pin must always be the
    older of the two current releases, never an arbitrary older one, or `pkg_add` breaks.


========================================================================================================================
Linux Build
========================================================================================================================

Builds directly on Linux with the local `Makefile` - no cross-toolchain setup needed, but you do need one prebuilt
third-party helper binary first (from GitHub, pick the file matching your host arch, and put it somewhere on your PATH
or reference it by full path from the Makefile):

    mkblob - wraps the compiled ELF into a self-contained blob for the `-static` target (`lin` builds both a normal
    dynamically-linked binary and, via `mkblob ... -static -dae`, a genuinely statically-linked one - confirmed by
    `ldd`/`file` reporting it as non-dynamic, and by runtime `/proc/<pid>/maps` showing no `.so` mappings and no
    extraction to `/tmp` while running):
        https://github.com/sigurd-dev/mkblob/tree/master
        https://github.com/sigurd-dev/mkblob/blob/master/binary_i386/mkblob.i386
        https://github.com/sigurd-dev/mkblob/blob/master/binary_x86_64/mkblob

Once downloaded (mark it executable with `chmod +x`) and reachable, simply run the `Makefile` from this directory - no
further configuration needed.

Alternative for the `-static` target: packelf (https://github.com/oufm/packelf/tree/master, packelf.sh) is NOT used by
this Makefile, but is worth knowing about as an alternative to mkblob's `-static` mode if you ever need one. Unlike
mkblob's true static link, packelf instead bundles the normal dynamic binary together with its `.so` dependencies into
a self-extracting shell script that unpacks to a temporary directory and re-execs itself through a bundled `ld.so` on
every run - useful if mkblob's `-static` mode ever fails for a given libc/libusb combination, at the cost of a slower
startup (extract-then-exec instead of one direct exec) and a runtime dependency on being able to write/exec from
a temporary directory. To wire it in as a second `-static`-style variant, add something like this to the `lin` target's
recipe in place of (or alongside) the `mkblob` line, after the first `$(LIN_CC)` build step produces the dynamic
`$(OUTPUT_EXE_NAME)`:
    packelf.sh $(OUTPUT_EXE_NAME) $(OUTPUT_EXE_NAME)_static


========================================================================================================================
Windows Build (from Linux)
========================================================================================================================

Cross-compiles Windows binaries from Linux using MXE (M cross environment), which builds its own mingw-w64 gcc toolchain
from source - expect this to take a long time (an hour or more) and several GB of disk space on first build.

Prerequisites (must already be installed/built before starting):
  - A working native gcc/g++ (MXE's own build process needs one to bootstrap). The paths below (`/opt/gcc-7.5.0`,
    `/opt/isl-0.16.1`) are this project's author's own pre-built toolchain locations on their machine - substitute your
    own compiler's actual install paths, or simply omit the `LD_LIBRARY_PATH`/`PATH` exports below entirely if your
    system's default gcc/g++ is new enough (MXE documents its own minimum host-compiler requirements).
  - git, and the usual MXE build dependencies for your distro (autoconf, bison, flex, gperf, etc. - see
    https://mxe.cc/#requirements for the full list, which varies by distro).

Steps:

  1. Clone MXE:
       git clone https://github.com/mxe/mxe.git

  2. Edit MXE's `Makefile` (MXE's own, inside the cloned mxe/ directory - not this project's):
       a. Change `MXE_TARGETS` to `x86_64-w64-mingw32.static`
       b. Add `--no-check-certificate` to `WGET` (only needed if your environment has trouble validating TLS certs when
          MXE fetches its own sources)

  3. If using a custom host toolchain (see Prerequisites above), point PATH/LD_LIBRARY_PATH at it before building -
     replace these example paths with your own:
       export LD_LIBRARY_PATH=/opt/isl-0.16.1/lib:/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib
       export PATH=/opt/gcc-7.5.0/bin:$PATH

  4. Build MXE's cross-gcc and a static libusb for it (run from inside the cloned mxe/ directory):
       make cc
       make libusb

  5. Make MXE's cross-toolchain output discoverable wherever this project's own `Makefile` expects to find it (adjust
     both sides of the symlink to your own actual paths - the target below is just where this project's author happened
     to keep their MXE checkout):
       cd /opt
       ln -s /run/media/aloysius/old_data/aloysius/mxe/usr mxe

  6. Run this project's own `Makefile` as normal; its Windows target picks up the MXE toolchain via the symlink from
     step 5.

On the target Windows machine, if using the `libusb` build, the end user will need to replace the
driver from `HidUsb` to `WinUSB` using Zadig:
  https://zadig.akeo.ie
  https://github.com/pbatard/libwdi/releases/download/v1.5.1/zadig-2.9.exe


========================================================================================================================
MacOS Build (from Linux)
========================================================================================================================

Cross-compiles macOS binaries from Linux using osxcross, which needs an actual copy of Apple's Xcode SDK to build
against - Apple's EULA means this can't be auto-downloaded, so it has to be fetched manually from a logged-in Apple
Developer account first.

Prerequisites (must already be installed/built before starting):
  - An Apple ID enrolled (free tier is fine) at https://developer.apple.com, to download the Xcode .xip below.
  - A working host clang/LLVM and cmake (osxcross's build process needs both). The paths below (`/opt/clang-15.0.6`,
    `/opt/cmake-3.4.3`, `/opt/gcc-7.5.0`) are this project's author's own pre-built toolchain locations - substitute
    your own install paths, or omit the exports entirely if your system's default clang/cmake are new enough (see
    osxcross's own requirements: https://github.com/tpoechtrager/osxcross#packaging-the-sdk).
  - Disk space for the extracted SDK and osxcross's own build output (several GB).

Steps:

  1. Download an Xcode .xip from Apple (requires being logged into an Apple Developer account in your browser first -
     this URL alone will not work unauthenticated):
       https://developer.apple.com/download/all/?q=xcode
       https://download.developer.apple.com/Developer_Tools/Xcode_12.5_beta_3/Xcode_12.5_beta_3.xip
    (any reasonably recent Xcode version works; the version above is simply what this project was last built against.)

  2. Clone osxcross:
       git clone https://github.com/tpoechtrager/osxcross.git

  3. Point PATH/LD_LIBRARY_PATH at your host clang/cmake toolchain (see Prerequisites above) - replace these example
     paths with your own:
       export LD_LIBRARY_PATH=/opt/isl-0.16.1/lib:/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/clang-15.0.6/lib
       export PATH=/opt/gcc-7.5.0/bin:/opt/clang-15.0.6/bin:/opt/cmake-3.4.3/bin:$PATH

  4. Tell osxcross where to build and install itself (replace with your own directories):
       export BUILD_DIR=/run/media/aloysius/old_data/aloysius/osxcross/build
       export TARGET_DIR=/run/media/aloysius/old_data/aloysius/osxcross/install
       export INSTALLPREFIX=/run/media/aloysius/old_data/aloysius/osxcross/install

  5. osxcross's linker wrapper needs `ld.lld` from your clang install, referenced explicitly:
       export CFLAGS='--ld-path=/opt/clang-15.0.6/bin/ld.lld'
       export CXXFLAGS='--ld-path=/opt/clang-15.0.6/bin/ld.lld'

  6. osxcross's build scripts expect an `ld` binary alongside clang, which the host toolchain above may not have
     provided under that exact name - symlink it, matching your clang path:
       ln -s /opt/clang-15.0.6/bin/lld /opt/clang-15.0.6/bin/ld

  7. Package the SDK out of the downloaded .xip (run from inside the cloned osxcross/ directory; replace the filename
     with whatever you actually downloaded in step 1):
       ./tools/gen_sdk_package_pbzx.sh ../Xcode_12.5_beta_3.xip
       mv MacOSX11.3.sdk.tar.xz tarballs

  8. Build osxcross itself:
       ./build.sh

  9. Undo the temporary symlink from step 6, since it's only needed during osxcross's own build:
       rm /opt/clang-15.0.6/bin/ld

  10. Make osxcross's output discoverable wherever this project's own `Makefile` expects to find it (adjust both sides
      of the symlink to your own actual paths - the target below is just where this project's author happened to keep
      their osxcross checkout):
        cd /opt
        ln -s /run/media/aloysius/old_data/aloysius/osxcross/install osxcross

  11. Run this project's own `Makefile` as normal; its MacOS target picks up the osxcross toolchain via the symlink from
      step 10.
