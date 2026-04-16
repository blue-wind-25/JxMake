/*
 * This firmware is written for:
 *     LilyGO(R) TTGO VGA32 V1.4 Development Board
 *
 * Please refer to these URLs for further information and details regarding the development board
 * (last accessed 2023-10-21):
 *
 *     https://ae01.alicdn.com/kf/H76eebd74dfa54dff9a2c0c2e931e09ebe.jpg
 *     https://ae01.alicdn.com/kf/Hd09579a138ae4e9fb22a925ae7428d51X.jpg
 *
 *     https://www.cnx-software.com/2018/07/09/ttgo-micro-32-tiny-esp32-pico-d4-module
 *     https://www.tindie.com/products/lilygo/lilygo-ttgo-micro-32-v20
 *     https://i.stack.imgur.com/iIYYA.jpg
 *
 *     https://github.com/LilyGO/FabGL/blob/master/Schematic/vga32_v1.4.pdf
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This firmware is written based on the source code and information from:
 *
 *     A Bare-Metal, Single Header ESP32/ESP32-C3 SDK
 *         https://github.com/cpq/mdk
 *         https://github.com/cpq/mdk/tree/main/esp32
 *         Copyright (C) 2021 Cesanta. All rights reserved.
 *         MIT License.
 *
 *     ESP32 Bare Metal AppCPU
 *         https://github.com/Winkelkatze/ESP32-Bare-Metal-AppCPU
 *         https://hackaday.io/project/174521-bare-metal-second-core-on-esp32
 *         Copyright (C) 2020 Daniel Frejek.
 *         MIT License.
 *
 *     Configuring GPIO using registers in the stub code
 *         https://esp32.com/viewtopic.php?t=1674#p7795
 *
 *     ESP32 Technical Reference Manual
 *         https://www.espressif.com/sites/default/files/documentation/esp32_technical_reference_manual_en.pdf
 *
 *     ESP32 Introduction and Initial process Flow
 *         https://embetronicx.com/tutorials/wireless/esp32/idf/esp32-introduction-and-initial-process-flow/amp
 *
 * This example utilizes the JxMake ESP32 bare-metal library (C++XBuildTool_BM_ESP32) to build the
 * firmware using a very minimal set of ESP-IDF headers.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * The files in the 'arduino/bin' directory are generated/copied from distribution packages that can be
 * downloaded from:
 *     Arduino core for the ESP32, ESP32-S2, ESP32-S3, ESP32-C3, ESP32-C6 and ESP32-H2
 *         https://github.com/espressif/arduino-esp32
 *         https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
 *         GNU Lesser General Public License version 2.1 or any later version.
 *
 * These are the commands used to produce those files:
 *     python3 ~/.arduino15/packages/esp32/tools/esptool_py/4.5.1/esptool.py --chip esp32 elf2image --flash_mode dio --flash_freq 80m --flash_size 4MB -o arduino/bin/bootloader_qio_80m.elf-0x1000.bin ~/.arduino15/packages/esp32/hardware/esp32/2.0.14/tools/sdk/esp32/bin/bootloader_qio_80m.elf
 *     python3 ~/.arduino15/packages/esp32/hardware/esp32/2.0.14/tools/gen_esp32part.py -q ~/.arduino15/packages/esp32/hardware/esp32/2.0.14/tools/partitions/huge_app.csv arduino/bin/partitions-0x8000.bin
 *     cp ~/.arduino15/packages/esp32/hardware/esp32/2.0.14/tools/partitions/boot_app0.bin arduino/bin/boot_app0-0xe000.bin
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * The files in the 'arduino/esp-idf' directory are downloaded from:
 *     Espressif IoT Development Framework
 *         https://github.com/espressif/esp-idf
 *         Copyright (C) 2015-2023 Espressif Systems.
 *         Apache License Version 2.0.
 *
 * The files from Arduino core v2.0.14 have been used as reference:
 *     Arduino core for the ESP32, ESP32-S2, ESP32-S3, ESP32-C3, ESP32-C6 and ESP32-H2
 *         Arduino Release v2.0.14 based on ESP-IDF v4.4.6
 *         https://github.com/espressif/arduino-esp32/releases/tag/2.0.14
 *     ESP-IDF Release v4.4.6
 *         https://github.com/espressif/esp-idf/tree/release/v4.4
 *         https://github.com/espressif/esp-idf/tree/357290093430e41e7e3338227a61ef5162f2deed
 */

#include "util.h"


// Use external LEDs
#define USER_LED1 13
#define USER_LED2 14

// Use external switch
#define USER_SW   34


static volatile bool fast = true;


void app_cpu_main()
{
    // Strings
    static const char* DFLASH msgEntry  = ">>> Entering app_cpu_main() ...\n\n";
    static const char* DFLASH msgFormat = "fast=%d\n";

    // Print the entry message
    uart_tx(msgEntry);

    for(;;) {

        // Skip if the switch is not pressed
        if( gpio_read(USER_SW) ) continue;

        // Debouncing
        delay_ms(100);

        // Change the speed flag
        fast = !fast;

        // Print the status
        global_lock();
        ets_printf(msgFormat, fast);
        global_unlock();

        // Wait until the switch is no longer pressed
        while( !gpio_read(USER_SW) );

        // Debouncing
        delay_ms(100);

    } // for
}


static void delay()
{ delay_ms(fast ? 100 : 500); }


void kernel_main()
{
    // Strings
    static const char* DFLASH msgEntry       = "\n\n\n>>> Entering kernel_main() ...\n\n";
    static const char* DFLASH msgInitGPIO    = "Initializing GPIO ...\n";
    static const char* DFLASH msgStartAppCPU = "Starting the APP_CPU ...\n\n";
    static const char* DFLASH msgStartMLoop  = "Starting the main loop ...\n\n";
    static const char* DFLASH msgLoopTick    = "#\n";

    // Print the entry message
    uart_tx(msgEntry);

    // Initialize GPIO
    uart_tx(msgInitGPIO);

    gpio_output(USER_LED1);
    gpio_output(USER_LED2);
    gpio_input (USER_SW  );

    // Start the APP_CPU
    uart_tx(msgStartAppCPU);

    fast = !fast;

    start_app_cpu(&app_cpu_main);

    // Loop forever
    uart_tx(msgStartMLoop);

    for(;;) {

        gpio_write(USER_LED1, 0);
        gpio_write(USER_LED2, 1);
        delay();

        gpio_write(USER_LED1, 1);
        gpio_write(USER_LED2, 0);
        delay();

        global_lock();
        uart_tx(msgLoopTick);
        global_unlock();

    } // for
}
