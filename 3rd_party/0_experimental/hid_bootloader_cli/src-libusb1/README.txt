----------------------------------------------------------------------------------------------------
----- CI Build --------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

Workflow: .github/workflows/build-hid-bootloader-cli.yml (repo root, since this is a monorepo)

Builds the full portability matrix without needing any of the local cross-toolchains below:

    Linux   : x86, amd64, arm32, arm64   - static, musl libc (no glibc version dependency at all)
    Windows : x86, amd64, arm32, arm64   - native MSVC, cross-compiled via VS Build Tools on
                                            windows-latest (ilammy/msvc-dev-cmd action)
    MacOS   : universal (x86_64 + arm64) - native Xcode clang on macos-latest,
                                            -mmacosx-version-min=10.13
    FreeBSD : x64                        - built inside a QEMU VM (vmactions/freebsd-vm)
    OpenBSD : x64                        - built inside a QEMU VM (vmactions/openbsd-vm)

Notes / known limitations:
    - Windows ARM32 has no usable mingw/MXE HID+SetupAPI support, so it's built with MSVC's
      x64_arm cross toolset instead of the xwin/xwin-libusb mingw path used below.
    - MacOS is built on real GitHub-hosted macOS runners rather than osxcross, because osxcross
      needs the Xcode SDK, which can't be auto-downloaded in CI (Apple EULA/licensing).
    - Linux targets use static musl builds (same "fully static, runs anywhere" idea as this
      Makefile's `mkblob`/`-static` trick for linux-x64) instead of chasing "oldest glibc",
      which is fragile because glibc symbol versioning ties the binary to the build host.

Manual dispatch only (Actions tab -> "Build hid_bootloader_cli" -> "Run workflow"), gated to the
repo owner. The `target` input lets you build ONE target at a time while debugging a toolchain
issue (fix it, re-dispatch with that same target until it's green) instead of the whole matrix
failing together. Once every individual target is green, dispatch with `target: all` to build
everything and produce a single packaged download: each target's binary is collected into its
own `build/ci/<target>/` subdirectory (mirroring the layout below) and the whole tree is zipped
into `build/hid_bootloader_cli-<version>-all-targets.zip`, uploaded as the `package` job's
artifact.


----------------------------------------------------------------------------------------------------
----- Linux Build ----------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

https://github.com/sigurd-dev/mkblob/tree/master
https://github.com/sigurd-dev/mkblob/blob/master/binary_i386/mkblob.i386
https://github.com/sigurd-dev/mkblob/blob/master/binary_x86_64/mkblob

https://github.com/oufm/packelf/tree/master
https://github.com/oufm/packelf/blob/master/packelf.sh


----------------------------------------------------------------------------------------------------
----- Windows Build --------------------------------------------------------------------------------
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
----- MacOS Build ----------------------------------------------------------------------------------
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
