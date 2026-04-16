/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <pico/cyw43_arch.h>

#include "led.h"


static uint32_t last_blink = 0;
static int      led_state  = 0;


void blinkActivityLED(BlinkPattern mode)
{
    uint32_t interval = 500;

         if(mode == BLINK_WIFI_WAIT  ) interval =  300;
    else if(mode == BLINK_SERVER_WAIT) interval =   50;
    else if(mode == BLINK_HEARTBEAT  ) interval = 1000;

    const uint32_t now = millis();

    if(now - last_blink >= interval) {
        last_blink = now;
        led_state  = !led_state;
        cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, led_state);
    }
}


__no_return__ void blinkErrorLED(ErrorBlinkPattern pattern)
{
    while(true) {

        for(int i = 0; i < pattern; ++i) {

            cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, 1); sleep_ms(200);
            cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, 0); sleep_ms(200);

        } // for

        sleep_ms(1000);

    } // while
}
