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
