#include <hardware/clocks.h>
#include <hardware/pio.h>

#include "pio/blink.pio.h"


extern void initPIOProgram()
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
