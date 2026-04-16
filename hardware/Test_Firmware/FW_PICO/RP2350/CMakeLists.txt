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

cmake_minimum_required(VERSION 3.12)


set(ENV{PICO_SDK_PATH}       $ENV{HOME}/xsdk/raspberry-pi-pico-2024-11-30-SDKv2.1.0/pico-sdk )
set(ENV{PICO_TOOLCHAIN_PATH} $ENV{HOME}/xsdk/raspberry-pi-pico/gcc-arm-none-eabi-10.3-2021.10)

set(PICO_BOARD pico2)

include($ENV{PICO_SDK_PATH}/external/pico_sdk_import.cmake)

project(test C CXX ASM)

set(CMAKE_C_STANDARD   11)
set(CMAKE_CXX_STANDARD 17)

add_executable(
    ${PROJECT_NAME}
    ${CMAKE_CURRENT_LIST_DIR}/main.c
)

target_include_directories(
    ${PROJECT_NAME} PRIVATE
)

target_link_libraries(
    ${PROJECT_NAME}
    hardware_pio
    pico_multicore
    pico_stdlib
)

pico_sdk_init         (                 )
pico_add_extra_outputs(${PROJECT_NAME}  ) # Create MAP, BIN, HEX, and UF2 files
pico_add_hex_output   (${PROJECT_NAME}  ) # ---
pico_enable_stdio_usb (${PROJECT_NAME} 1) # Enable USB output
pico_enable_stdio_uart(${PROJECT_NAME} 0) # Disable UART output

target_include_directories(${PROJECT_NAME} PRIVATE ${CMAKE_BINARY_DIR})

pico_generate_pio_header(${PROJECT_NAME} ${CMAKE_CURRENT_LIST_DIR}/blink.pio OUTPUT_DIR ${CMAKE_BINARY_DIR})
