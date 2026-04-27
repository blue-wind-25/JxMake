#!/bin/bash

##############################################################################
##### ------------------------------ NOTE ------------------------------ #####
##############################################################################
##### For easier uploading, add an udev rule:                            #####
#####     nano /etc/udev/rules.d/99-rpi-pico.rules                       #####
##### fill it with:                                                      #####
#####     SUBSYSTEM=="usb", ATTR{idVendor}=="2e8a", MODE="0666"          #####
#####     SUBSYSTEM=="usb", ATTR{idVendor}=="cafe", MODE="0666"          ##### NOTE : Synchronize this with the value in 'CMakeLists.txt'
##### then run:                                                          #####
#####     udevadm control --reload-rules                                 #####
#####     udevadm trigger                                                #####
##############################################################################

# Default configuration (can be overridden by environment variables)
HOST_COMPILER_PATH="${HOST_COMPILER_PATH:-/opt/gcc-7.5.0/bin}"
HOST_GCC="${HOST_GCC:-$HOST_COMPILER_PATH/gcc}"
HOST_GPP="${HOST_GPP:-$HOST_COMPILER_PATH/g++}"

HOST_CMAKE_PATH="${HOST_CMAKE_PATH:-/opt/cmake-3.28.1/bin}"
HOST_CMAKE="${HOST_CMAKE:-$HOST_CMAKE_PATH/cmake}"

# Prepare build directory
mkdir -p build
cd       build

# Export toolchain variables
export CC="$HOST_GCC"
export CXX="$HOST_GPP"

# Execute CMake
"$HOST_CMAKE" ..

# Spawn a new interactive shell with this environment
echo
echo -e '\033[1;37mType "exit" to leave this session and then "./clean.sh" to delete all generated files\033[0m'
echo
exec bash
