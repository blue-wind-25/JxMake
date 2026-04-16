#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####


cmake_minimum_required(VERSION 3.12)

set(CMAKE_BUILD_TYPE Release)


set(ENV{PICO_SDK_PATH}       $ENV{HOME}/xsdk/raspberry-pi-pico-2023-06-23-SDKv1.5.1/pico-sdk )
set(ENV{PICO_TOOLCHAIN_PATH} $ENV{HOME}/xsdk/raspberry-pi-pico/gcc-arm-none-eabi-10.3-2021.10)

set(PICOTOOL_PATH      $ENV{PICO_SDK_PATH}/../picotool-linux64)
set(PICO_BOARD         pico_w                                 )
set(CMAKE_C_STANDARD   11                                     )
set(CMAKE_CXX_STANDARD 17                                     )

include($ENV{PICO_SDK_PATH}/external/pico_sdk_import.cmake)


project(RP2040_SERIAL_WIFI_BRIDGE C CXX ASM)

set(PICO_USB_VID 0xcafe)
set(PICO_USB_PID 0x4002)

add_executable(
    ${PROJECT_NAME}
    ${CMAKE_CURRENT_LIST_DIR}/src/led.c
    ${CMAKE_CURRENT_LIST_DIR}/src/main.c
    ${CMAKE_CURRENT_LIST_DIR}/src/ring_buffer.c
    ${CMAKE_CURRENT_LIST_DIR}/src/usb_descriptors.c
    ${CMAKE_CURRENT_LIST_DIR}/src/util.c
)

target_include_directories(
    ${PROJECT_NAME} PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/src
    $ENV{PICO_SDK_PATH}/lib/tinyusb/src
)

target_link_libraries(
    ${PROJECT_NAME}
    pico_cyw43_arch_lwip_threadsafe_background
    pico_multicore
    pico_stdlib
    tinyusb_board
    tinyusb_device
)

target_compile_definitions(${PROJECT_NAME} PRIVATE
    NDEBUG
    PICO_USB_VID=${PICO_USB_VID}
    PICO_USB_PID=${PICO_USB_PID}
)

add_custom_target(
    upload
    COMMAND ${PICOTOOL_PATH} reboot -u --vid ${PICO_USB_VID} --pid ${PICO_USB_PID} -f
    COMMAND ${CMAKE_COMMAND} -E sleep 1
    COMMAND ${PICOTOOL_PATH} load -f ${CMAKE_BINARY_DIR}/${PROJECT_NAME}.uf2
    COMMAND ${CMAKE_COMMAND} -E sleep 1
    COMMAND ${PICOTOOL_PATH} reboot
    DEPENDS ${PROJECT_NAME}
)

add_custom_target(
    nrb_upload
    COMMAND ${PICOTOOL_PATH} load -f ${CMAKE_BINARY_DIR}/${PROJECT_NAME}.uf2
    COMMAND ${CMAKE_COMMAND} -E sleep 1
    COMMAND ${PICOTOOL_PATH} reboot
    DEPENDS ${PROJECT_NAME}
)


pico_sdk_init         (                 )
pico_add_extra_outputs(${PROJECT_NAME}  ) # Create MAP, BIN, HEX, and UF2 files
pico_enable_stdio_usb (${PROJECT_NAME} 0) # Disable the default USB  output
pico_enable_stdio_uart(${PROJECT_NAME} 0) # Disable the default UART output
