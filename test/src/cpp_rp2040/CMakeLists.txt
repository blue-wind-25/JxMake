# cd <PICO_SDK_DIRECTORY>
# git pull
# git submodule update --init
#
# mkdir build && cd build
#
# export CC=/opt/gcc-7.5.0/bin/gcc
# export CXX=/opt/gcc-7.5.0/bin/g++
# rm -rvf * && cmake3 ..
# make
#
# export CC=/opt/gcc-7.5.0/bin/gcc
# export CXX=/opt/gcc-7.5.0/bin/g++
# rm -rvf * && cmake3 -DCMAKE_RULE_MESSAGES:BOOL=OFF -DCMAKE_VERBOSE_MAKEFILE:BOOL=ON ..
# make --no-print-directory
#
# export CC=/opt/gcc-7.5.0/bin/gcc
# export CXX=/opt/gcc-7.5.0/bin/g++
# rm -rvf * && cmake3 ..
# script -q -c 'make --no-print-directory VERBOSE=1' RP2040-Build-CMake-1.5.1.txt
# cat RP2040-Build-CMake-1.5.1.txt | sed -e 's/\x1B\[[0-9;]*[JKmsu]//g' > NO_ANSI-C.txt
#

cmake_minimum_required(VERSION 3.12)


#set(ENV{PICO_SDK_PATH}       $ENV{HOME}/xsdk/raspberry-pi-pico-2023-03-03-SDKv1.5.0/pico-sdk ) # 1.5.0
 set(ENV{PICO_SDK_PATH}       $ENV{HOME}/xsdk/raspberry-pi-pico-2023-06-23-SDKv1.5.1/pico-sdk ) # 1.5.1
 set(ENV{PICO_TOOLCHAIN_PATH} $ENV{HOME}/xsdk/raspberry-pi-pico/gcc-arm-none-eabi-10.3-2021.10)

set(PICO_BOARD pico_w)

include($ENV{PICO_SDK_PATH}/external/pico_sdk_import.cmake)

project(test C CXX ASM)

set(CMAKE_C_STANDARD   11)
set(CMAKE_CXX_STANDARD 17)

add_executable(
    ${PROJECT_NAME}
    ${CMAKE_CURRENT_LIST_DIR}/main.cpp
    ${CMAKE_CURRENT_LIST_DIR}/ulib/src1.cpp
    ${CMAKE_CURRENT_LIST_DIR}/ulib/src2.cpp
    ${CMAKE_CURRENT_LIST_DIR}/bt/bt.c
    ${CMAKE_CURRENT_LIST_DIR}/wifi/dhcpserver.c
    ${CMAKE_CURRENT_LIST_DIR}/wifi/dnsserver.c
)

target_include_directories(
    ${PROJECT_NAME} PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/bt
    ${CMAKE_CURRENT_LIST_DIR}/wifi
)

target_link_libraries(
    ${PROJECT_NAME}
    hardware_adc
    pico_btstack_ble
    pico_btstack_cyw43
    pico_cyw43_arch_lwip_threadsafe_background
    pico_multicore
    pico_stdlib
)

pico_sdk_init         (                 )
pico_add_extra_outputs(${PROJECT_NAME}  ) # Create MAP, BIN, HEX, and UF2 files
pico_enable_stdio_usb (${PROJECT_NAME} 1) # Enable USB output
pico_enable_stdio_uart(${PROJECT_NAME} 0) # Disable UART output

target_include_directories(${PROJECT_NAME} PRIVATE ${CMAKE_BINARY_DIR})

file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/pio)
pico_generate_pio_header(${PROJECT_NAME} ${CMAKE_CURRENT_LIST_DIR}/pio/blink.pio OUTPUT_DIR ${CMAKE_BINARY_DIR}/pio)

file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/gatt)
add_custom_command(
        OUTPUT            ${CMAKE_BINARY_DIR}/gatt/dummy_temp.gatt.h
        DEPENDS           ${CMAKE_CURRENT_LIST_DIR}/gatt/dummy_temp.gatt
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/gatt
        COMMAND ${Python3_EXECUTABLE} ${PICO_SDK_PATH}/lib/btstack/tool/compile_gatt.py ${CMAKE_CURRENT_LIST_DIR}/gatt/dummy_temp.gatt ${CMAKE_BINARY_DIR}/gatt/dummy_temp.gatt.h
        VERBATIM
)
add_custom_target(generate_gatt_header DEPENDS ${CMAKE_BINARY_DIR}/gatt/dummy_temp.gatt.h)
add_dependencies(${PROJECT_NAME} generate_gatt_header)


set(PICOTOOL_PATH $ENV{PICO_SDK_PATH}/../picotool-linux64)

add_custom_target(
    upload
    COMMAND ${PICOTOOL_PATH} load -f ${CMAKE_BINARY_DIR}/${PROJECT_NAME}.uf2
    COMMAND ${CMAKE_COMMAND} -E sleep 1
    COMMAND ${PICOTOOL_PATH} reboot
    DEPENDS ${PROJECT_NAME}
)
