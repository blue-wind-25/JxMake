#include <stdio.h>

#include <pico/bootrom.h>
#include <pico/multicore.h>
#include <pico/stdio.h>
#include <pico/time.h>

#include <hardware/clocks.h>
#include <hardware/pio.h>

#include "build/blink.pio.h"


// Define the pins
#define LED_1_GPIO  25 // Built-in
#define LED_2_GPIO  14 // External

#define SW_GPIO     15 // External


static bool fast = true;

static void delayAndCheckSwitch(char cnt)
{
    // Delay and check the switch
    for(char i = 0; i < cnt; ++i) {
        // Delay for a while
        sleep_ms(10);
        // Check if the switch is pressed
        if( !gpio_get(SW_GPIO) ) {
            // Debouncing
            sleep_ms(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !gpio_get(SW_GPIO) );
}


static void initPIOProgram()
{
    /*
     * This code is written based on:
     *     https://github.com/raspberrypi/pico-examples/blob/master/pio/pio_blink/blink.c
     *     Copyright (C) 2020 Raspberry Pi (Trading) Ltd.
     *     SPDX-License-Identifier: BSD-3-Clause
     */

    const PIO  pio = pio0;
    const uint sm  = 0;
    const uint ofs = pio_add_program(pio, &blink_program);

    blink_program_init(pio, sm, ofs, 16); // GP16 is the bottom-right most pin (header pin #21)
                                          // https://www.raspberrypi.com/documentation/microcontrollers/images/picow-pinout.svg

    const uint freq = 1;
    pio->txf[sm] = ( clock_get_hz(clk_sys) / (2 * freq) ) - 3;
}


static void core1_program()
{
    static const char delay_s_s =  5;
    static const char delay_s_l = 10;

    static const char delay_l_s = 15;
    static const char delay_l_l = 30;

    // Loop to blink the built-in LED
    fast = !fast;

    for(;;) {

        // Blink the 1st LED
        for(char i = 0; i < 3; ++i) {

            gpio_put(LED_1_GPIO, 1);

            delayAndCheckSwitch(fast ? delay_s_s : delay_s_l); // Short delay

            gpio_put(LED_1_GPIO, 0);

            delayAndCheckSwitch(fast ? delay_s_s : delay_s_l); // Short delay

        } // for

        // Blink the 2nd LED
        for(char i = 0; i < 3; ++i) {

            gpio_put(LED_2_GPIO, 1);

            delayAndCheckSwitch(fast ? delay_s_s : delay_s_l); // Short delay

            gpio_put(LED_2_GPIO, 0);

            delayAndCheckSwitch(fast ? delay_s_s : delay_s_l); // Short delay

        } // for

        // Long delay
        delayAndCheckSwitch(fast ? delay_l_s : delay_l_l);

    } // for
}


int main()
{
    // Initialize system
    stdio_init_all();

    gpio_init     (LED_1_GPIO             );
    gpio_set_dir  (LED_1_GPIO, GPIO_OUT   );

    gpio_init     (LED_2_GPIO             );
    gpio_set_dir  (LED_2_GPIO, GPIO_OUT   );

    gpio_init     (SW_GPIO                );
    gpio_set_dir  (SW_GPIO   , GPIO_IN    );
    gpio_set_pulls(SW_GPIO   , true, false);

    initPIOProgram();

    // Run program to blink the built-in LED in CORE1
    multicore_launch_core1(core1_program);

    // Loop to check input from UART
    for(;;) {

        const int ch = getchar_timeout_us(1000);

             if(ch == 'p') printf( "\nThe board is 'Raspberry Pi Pico 2'\n\n" );
        else if(ch == 'b') reset_usb_boot(0, 0);

    } // for

    // The program should never get here
    return 0;
}
