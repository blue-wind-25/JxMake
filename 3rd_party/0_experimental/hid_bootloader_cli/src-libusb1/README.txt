----------------------------------------------------------------------------------------------------
----- GitHub CI Build ------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

Workflow: .github/workflows/build-hid-bootloader-cli.yml (repo root, since this is a monorepo)
Tmp workflow: .github/workflows/build-hid-bootloader-cli-tmp.yml - scoped to just the new/changed
targets below, for iterating on a toolchain without touching the (already-verified) linux jobs
or the `package` job in the main workflow. Delete it once those targets are green.

Builds the full portability matrix without needing any of the local cross-toolchains below:

    - Linux   : x86, amd64, arm32, arm64     : static, musl libc (no glibc version dependency)
    - Windows : x86, amd64, arm64            : HidUsb, native MSVC on windows-2022, /MT + Win7
                                               API floor (see notes below)
    - Windows : x86, amd64                   : WinUSB via libusb, native MSVC on windows-2022,
                                               same /MT + Win7 floor
    - MacOS   : x86_64 only                  : native Xcode clang on macos-latest,
                                               -mmacosx-version-min=10.13
    - MacOS   : universal (x86_64 + arm64)   : native Xcode clang on macos-latest,
                                               -mmacosx-version-min=11.0
    - FreeBSD : x64                          : static, libusb built from source, FreeBSD 13.2
                                               (vmactions/freebsd-vm)
    - OpenBSD : x64                          : static, libusb built from source, OpenBSD 7.8
                                               (vmactions/openbsd-vm)

Artifact/`build/ci/<target>/` naming mirrors the local `make` targets below:
    - `-static` suffix   : matches the local `-static` mkblob suffix - Linux, FreeBSD and OpenBSD
                           CI builds are all fully static, so they all carry it.
    - `-libusb` suffix   : matches the local `xwin-libusb` target - the WinUSB/libusb Windows
                           build, as opposed to the default HidUsb one.
    - everything else    : dynamically linked (Windows' MSVC CRT is still dynamic even with
                           `/MT` statically linking the *C* runtime - `/MT` only removes the
                           redistributable-version dependency, see notes below) or, for MacOS,
                           not meaningfully "static" at all on that platform.

The `windows-x86-libusb`, `windows-x64-libusb` and `macos-x86_64` targets are new and have not
yet been built by CI - their `build/ci/` subdirectories hold a `PENDING.txt` placeholder until a
`target: all` (or per-target) dispatch populates them; delete the placeholder once that happens.
Re-dispatching `target: all` and re-copying `build/ci/` from the downloaded artifact is also how
to refresh the checked-in binaries generally, since they will drift as runner images and
toolchains get updated upstream (e.g. a newer windows-2022 VS servicing update, a newer
macos-latest Xcode) - don't hand-edit the binaries in place.

Notes/known limitations:
    - Linux targets use static musl builds (same "fully static, runs anywhere" idea as this
      Makefile's `mkblob`/`-static` trick for linux-x64) instead of chasing "oldest glibc",
      which is fragile because glibc symbol versioning ties the binary to the build host.
    - Windows runs on windows-2022 (Server 2022 / VS2022 toolset), not windows-latest (currently
      windows-2025 / VS "18") - an older, still-supported hosted image whose toolset lineage is
      closer to a Windows 10 end-user machine. Windows 7 compatibility is then handled
      explicitly at compile time:
        - `/MT` statically links the MSVC CRT, so the binary doesn't depend on the matching
          Visual C++ Redistributable being installed on the target machine (the actual #1 cause
          of "won't run on an old Windows box").
        - `/D_WIN32_WINNT=0x0601` caps the Windows API level compiled against at Windows 7, so
          no Windows 8+-only API accidentally slips in.
        - `/SUBSYSTEM:CONSOLE,6.01` stamps the PE header's minimum OS version as Windows 7
          instead of whatever the toolset defaults to, so Windows won't refuse to launch it.
      These floors are moot for windows-arm64 (ARM64 Windows only ever shipped as Windows 10+),
      but `/MT` still applies there for the same redistributable reason.
    - Windows builds both HidUsb (-DUSE_WIN32, SetupAPI/hid.lib - same as this Makefile's `xwin`
      target) and WinUSB via libusb (-DUSE_LIBUSB - same as `xwin-libusb`), the latter built with
      a vcpkg-built static libusb (x86/x64 only, see below) using the `<arch>-windows-static`
      triplet - vcpkg is preinstalled on the GitHub-hosted Windows runner, so this needs no extra
      toolchain install. HidUsb is the standard HID class driver built into Windows, so it works
      immediately on any Windows 7-11 machine with no driver changes; WinUSB only sees the device
      after the end user manually re-drivers it with Zadig (HidUsb -> WinUSB), so it's published
      as an explicit opt-in alternative, not the default.
    - Windows WinUSB/libusb is x86/x64 only: an `arm64-windows-static` libusb build via vcpkg is
      unverified here. ARM64 already gets HidUsb, which needs no extra driver on the target
      machine anyway, so this isn't a real gap worth chasing.
    - Windows ARM32 is NOT built at all: the GitHub-hosted Windows VS installs no longer ship a
      32-bit ARM toolset (only x86/amd64/arm64), and 32-bit ARM Windows hardware is essentially
      extinct anyway (superseded by ARM64 since ~2017).
    - MacOS ships two artifacts because a universal (fat) binary can't declare a version floor
      lower than its oldest-supported slice, and Apple Silicon never shipped a macOS older than
      11.0: `macos-x86_64` (Intel-only, floor 10.13) covers older/non-Apple-Silicon Macs, while
      `macos-universal` (x86_64 + arm64, floor 11.0) covers everything from Big Sur on.
    - MacOS is built on real GitHub-hosted macOS runners rather than osxcross, because osxcross
      needs the Xcode SDK, which can't be auto-downloaded in CI (Apple EULA/licensing).
    - FreeBSD/OpenBSD build libusb from source and link it (and libc) statically rather than
      using the base/port libusb.so, since that .so is tied to the exact release it was built
      on - OpenBSD in particular gives no ABI stability guarantee across releases.
    - FreeBSD/OpenBSD are pinned to older point releases (13.2 / 7.8) rather than each project's
      newest stable, purely for build-toolchain portability/reproducibility - the output itself
      is fully static (libc included), so it doesn't actually depend on the release it was built
      on. OpenBSD only supports its two most recent releases and DELETES the package mirror for
      anything older (unlike FreeBSD, which archives it), so the OpenBSD pin must always be the
      older of the two current releases, never an arbitrary older one, or `pkg_add` breaks.

Manual dispatch only (Actions tab -> "Build hid_bootloader_cli" -> "Run workflow"), gated to the
repo owner. The `target` input lets you build ONE target at a time while debugging a toolchain
issue (fix it, re-dispatch with that same target until it's green) instead of the whole matrix
failing together. Once every individual target is green, dispatch with `target: all` to build
everything and produce a single packaged download: each target's binary is collected into its
own `build/ci/<target>/` subdirectory (mirroring the layout below), the Unix executable bit is
restored (round-tripping through GitHub artifacts / a zip doesn't reliably preserve it), and the
whole tree is packed into `build/hid_bootloader_cli-<version>-all-targets.tar.bz2` (tar instead
of zip, so +x actually survives extraction), uploaded as the `package` job's artifact.


----------------------------------------------------------------------------------------------------
----- Linux Build ----------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

https://github.com/sigurd-dev/mkblob/tree/master
https://github.com/sigurd-dev/mkblob/blob/master/binary_i386/mkblob.i386
https://github.com/sigurd-dev/mkblob/blob/master/binary_x86_64/mkblob

https://github.com/oufm/packelf/tree/master
https://github.com/oufm/packelf/blob/master/packelf.sh

Simply run the 'Makefile'.


----------------------------------------------------------------------------------------------------
----- Windows Build (from Linux) -------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

https://mxe.cc
https://mxe.cc/#tutorial

git clone https://github.com/mxe/mxe.git

----------------------------------------------------------------------------------------------------

Edit 'Makefile' and:
    1. Change 'MXE_TARGETS' to 'x86_64-w64-mingw32.static'
    2. Add '--no-check-certificate' to 'WGET'

----------------------------------------------------------------------------------------------------

export LD_LIBRARY_PATH=/opt/isl-0.16.1/lib:/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib
export PATH=/opt/gcc-7.5.0/bin:$PATH

make cc
make libusb

cd /opt
ln -s /run/media/aloysius/old_data/aloysius/mxe/usr mxe

----------------------------------------------------------------------------------------------------

On Windows, if using 'libusb', you will need to replace the driver from 'HidUsb' to 'WinUSB' using:
    https://zadig.akeo.ie
    https://github.com/pbatard/libwdi/releases/download/v1.5.0/zadig-2.8.exe


----------------------------------------------------------------------------------------------------
----- MacOS Build (from Linux) ---------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

https://developer.apple.com/download/all/?q=xcode
https://download.developer.apple.com/Developer_Tools/Xcode_12.5_beta_3/Xcode_12.5_beta_3.xip

https://github.com/tpoechtrager/osxcross

git clone https://github.com/tpoechtrager/osxcross.git

----------------------------------------------------------------------------------------------------

export LD_LIBRARY_PATH=/opt/isl-0.16.1/lib:/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/clang-15.0.6/lib
export PATH=/opt/gcc-7.5.0/bin:/opt/clang-15.0.6/bin:/opt/cmake-3.4.3/bin:$PATH

export BUILD_DIR=/run/media/aloysius/old_data/aloysius/osxcross/build
export TARGET_DIR=/run/media/aloysius/old_data/aloysius/osxcross/install
export INSTALLPREFIX=/run/media/aloysius/old_data/aloysius/osxcross/install

export CFLAGS='--ld-path=/opt/clang-15.0.6/bin/ld.lld'
export CXXFLAGS='--ld-path=/opt/clang-15.0.6/bin/ld.lld'

ln -s /opt/clang-15.0.6/bin/lld /opt/clang-15.0.6/bin/ld

./tools/gen_sdk_package_pbzx.sh ../Xcode_12.5_beta_3.xip
mv MacOSX11.3.sdk.tar.xz tarballs
./build.sh

rm /opt/clang-15.0.6/bin/ld

cd /opt
ln -s /run/media/aloysius/old_data/aloysius/osxcross/install osxcross
