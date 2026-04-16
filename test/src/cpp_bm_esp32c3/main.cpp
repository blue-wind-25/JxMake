/*
 * This firmware is written for:
 *     LuatOS ESP32-C3 Development Board (CORE-ESP32)
 *     https://templates.blakadder.com/luatos_CORE-ESP32.html
 *     https://universal-solder.ca/downloads/luaTOS%20ESP32%20C3%20core%20board.pdf
 */

#include "mdk/mdk.h"


#if 1

    // Use external LEDs
    #define USER_LED1     0
    #define USER_LED2     1

    // Use external switch
    #define USER_SW      10

#else

    // Use the built-in LEDs on the development board
    #define USER_LED1    12
    #define USER_LED2    13

    // Use the built-in switch on the development board
    #define USER_SW       9

#endif

    // There is no built-in WS2812B LED on the development board
    #define USER_WS2812B  3


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay_ms(10);
        // Check if the switch is pressed
        if( !gpio_read(USER_SW) ) {
            // Debouncing
            delay_ms(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !gpio_read(USER_SW) );
}

int main()
{
    wdt_disable();

    gpio_output(USER_LED1   );
    gpio_output(USER_LED2   );
    gpio_input (USER_SW     );

    gpio_output(USER_WS2812B);

    static const uint8_t ws2812b_c[3][3] = { {   0, 255,   0 },    // Red
                                             { 255,   0,   0 },    // Green
                                             {   0,   0, 255 }  }; // Blue
                 uint8_t ws2812b_i       = 0;

    fast = !fast;

    for(;;) {

        ws2812_show( USER_WS2812B, ws2812b_c[ws2812b_i], sizeof(ws2812b_c[ws2812b_i]) );
        ++ws2812b_i;
        if(ws2812b_i > 2) ws2812b_i = 0;

        gpio_write(USER_LED1, 1); gpio_write(USER_LED2, 0); delayAndCheckSwitch();
        gpio_write(USER_LED1, 0); gpio_write(USER_LED2, 1); delayAndCheckSwitch();

        /*
         * NOTE : This message will only be displayed when using the newer ESP32-C3 development board which connect to the
         *        host PC using its native USB port (without using the CH343 USB to serial bridge chip).
         */
        printf("fast=%d   ws2812b_i=%d\n", fast, ws2812b_i);

    } // for

    return 0;
}
