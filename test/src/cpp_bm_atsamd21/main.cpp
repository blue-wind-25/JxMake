/*
 * This firmware is written for:
 *     Seeed Studio XIAO SAMD21 (Seeeduino XIAO)
 *     https://www.seeedstudio.com/Seeeduino-XIAO-Arduino-Microcontroller-SAMD21-Cortex-M0+-p-4426.html
 *     https://wiki.seeedstudio.com/Seeeduino-XIAO
 */

#include "mdk/mdk.h"


// Define the pins
#define USER_LED1 18
#define USER_LED2 19

#define USER_SW   11


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay_ms(10);
        // Check if the switch is pressed
        if( !( _SFR(PORTA_IN) & _BV(USER_SW) ) ) {
            // Debouncing
            delay_ms(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !( _SFR(PORTA_IN) & _BV(USER_SW) ) );
}


void kernel_main()
{
    // Initialize system
    sys_init();

    // ##### !!! TODO : Test UART? !!! #####

    // Set output direction for the LED pins
    _SFR(PORTA_DIRSET) = _BV(USER_LED1) | _BV(USER_LED2);

    // Set input direction for the switch pin (without pull-up)
    _SFR(PORTA_DIRCLR)          = _BV(USER_SW);
    _SFR(PORTA_PINCFG(USER_SW)) = _BV(1);

    // Loop forever
    fast = !fast;

    for(;;) {

        _SFR(PORTA_OUTCLR) = _BV(USER_LED1);
        _SFR(PORTA_OUTSET) = _BV(USER_LED2);
        delayAndCheckSwitch();

        _SFR(PORTA_OUTCLR) = _BV(USER_LED2);
        _SFR(PORTA_OUTSET) = _BV(USER_LED1);
        delayAndCheckSwitch();

    } // for
}
