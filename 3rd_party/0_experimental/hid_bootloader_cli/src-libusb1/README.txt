----------------------------------------------------------------------------------------------------
----- GitHub CI Build ------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

Workflow: .github/workflows/build-hid-bootloader-cli.yml (repo root, since this is a monorepo)

Builds the full portability matrix without needing any of the local cross-toolchains below:

    Linux   : x86, amd64, arm32, arm64   - static, musl libc (no glibc version dependency at all)
    Windows : x86, amd64, arm64          - native MSVC, cross-compiled via VS Build Tools on
                                            windows-latest (ilammy/msvc-dev-cmd action)
    MacOS   : universal (x86_64 + arm64) - native Xcode clang on macos-latest,
                                            -mmacosx-version-min=10.13
    FreeBSD : x64                        - static, libusb built from source (vmactions/freebsd-vm)
    OpenBSD : x64                        - static, libusb built from source (vmactions/openbsd-vm)

Every CI target above is fully static except Windows (which dynamically links the MSVC CRT, as
usual for that platform) - so unlike the local `make` targets below, none of the `-static` /
`-libusb` suffixes apply here; there's only one flavor of each target.

The binaries checked into `build/ci/` were built by run 33824894707 (2026-09-04) using:
    linux-x86/x64/arm32/arm64 : messense/rust-musl-cross Docker images, libusb 1.0.27
                                (i686/x86_64/armv7/aarch64-unknown-linux-musl)
    windows-x86/x64/arm64     : windows-2025-vs2026 runner image, Visual Studio "18" Enterprise,
                                MSVC 14.51.36231, Windows SDK 10.0.26100.0
    macos-universal           : macos-26-arm64 runner image, native Xcode clang,
                                -mmacosx-version-min=10.13
    freebsd-x64               : ubuntu-24.04 host + vmactions/freebsd-vm, FreeBSD 15.1,
                                libusb 1.0.27 built from source, static
    openbsd-x64               : ubuntu-24.04 host + vmactions/openbsd-vm, OpenBSD 7.9,
                                libusb 1.0.27 built from source, static (note: OpenBSD's `-static`
                                produces a static-PIE - `file` reports "dynamically linked" / ELF
                                type DYN, but `readelf -d` shows no NEEDED entries and there's no
                                PT_INTERP, confirming it has no actual runtime library dependency)
These binaries will drift from what a fresh CI run produces as runner images and toolchains get
updated upstream (e.g. a newer windows-latest VS release, a newer macos-latest Xcode) - re-run the
workflow with `target: all` and re-copy `build/ci/` from the downloaded artifact if you need a
refresh; don't hand-edit the binaries in place.

Notes / known limitations:
    - Windows ARM32 is NOT built: the GitHub-hosted windows-latest VS install no longer ships a
      32-bit ARM toolset at all (only x86/amd64/arm64 - confirmed via VC\Auxiliary\Build listing,
      Sept 2026), and 32-bit ARM Windows hardware is essentially extinct anyway (superseded by
      ARM64 since ~2017).
    - MacOS is built on real GitHub-hosted macOS runners rather than osxcross, because osxcross
      needs the Xcode SDK, which can't be auto-downloaded in CI (Apple EULA/licensing).
    - Linux targets use static musl builds (same "fully static, runs anywhere" idea as this
      Makefile's `mkblob`/`-static` trick for linux-x64) instead of chasing "oldest glibc",
      which is fragile because glibc symbol versioning ties the binary to the build host.
    - FreeBSD/OpenBSD build libusb from source and link it (and libc) statically rather than
      using the base/port libusb.so, since that .so is tied to the exact release it was built
      on - OpenBSD in particular gives no ABI stability guarantee across releases.
    - Windows binaries use HidUsb (-DUSE_WIN32, SetupAPI/hid.lib), NOT WinUSB/libusb - same as
      this Makefile's `xwin` target, not `xwin-libusb`. HidUsb is the standard HID class driver
      built into Windows, so the binary works immediately on any Windows 7-11 machine with no
      driver changes. A WinUSB/libusb build would only see the device after the end user
      manually re-drivers it with Zadig (HidUsb -> WinUSB), which isn't reasonable to ask of a
      downstream user for a prebuilt binary, so CI does not build that variant at all.

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
