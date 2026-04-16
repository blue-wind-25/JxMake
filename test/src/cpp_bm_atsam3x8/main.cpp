/*
 * This firmware is written for:
 *     Arduino Due
 *     https://docs.arduino.cc/hardware/due
 */

#include "mdk/mdk.h"


// Define the pins
#define USER_LED1_PIO PIOB  // Active high
#define USER_LED1_BIT 27

#define USER_LED2_PIO PIOC  // Active low
#define USER_LED2_BIT 30

#define USER_SW_PIO   PIOB  // Active low
#define USER_SW_BIT   21


#if 0
// https://stackoverflow.com/a/8556436
#define REP0(X)
#define REP1(X)  X
#define REP2(X)  REP1(X) X
#define REP3(X)  REP2(X) X
#define REP4(X)  REP3(X) X
#define REP5(X)  REP4(X) X
#define REP6(X)  REP5(X) X
#define REP7(X)  REP6(X) X
#define REP8(X)  REP7(X) X
#define REP9(X)  REP8(X) X
#define REP10(X) REP9(X) X
#define REP(THOUSANDS,HUNDREDS,TENS,ONES,X) REP##THOUSANDS(REP10(REP10(REP10(X)))) REP##HUNDREDS(REP10(REP10(X))) REP##TENS(REP10(X)) REP##ONES(X)
const char* __HUGE_DATA__ = REP(9,9,9,9, "0123456789ABCDEF0123456789ABCDEF");
      char  __b__;
      char  __e__;
#define INC_HUGE_DATA
#endif


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay_ms(10);
        // Check if the switch is pressed
        if( !( USER_SW_PIO->PIO_PDSR & _BV(USER_SW_BIT) ) ) {
            // Debouncing
            delay_ms(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !( USER_SW_PIO->PIO_PDSR & _BV(USER_SW_BIT) ) );
}


void kernel_main()
{
    // Initialize system
    HardwareInit();

    // ##### !!! TODO : Test UART? !!! #####

    // Set output direction for the LED pins
    USER_LED1_PIO->PIO_PER  = _BV(USER_LED1_BIT);
    USER_LED1_PIO->PIO_OER  = _BV(USER_LED1_BIT);
    USER_LED1_PIO->PIO_OWER = _BV(USER_LED1_BIT);

    USER_LED2_PIO->PIO_PER  = _BV(USER_LED2_BIT);
    USER_LED2_PIO->PIO_OER  = _BV(USER_LED2_BIT);
    USER_LED2_PIO->PIO_OWER = _BV(USER_LED2_BIT);

    // Set input direction for the switch pin (with pull-up)
    USER_SW_PIO->PIO_PER  = _BV(USER_SW_BIT);
    USER_SW_PIO->PIO_ODR  = _BV(USER_SW_BIT);
    USER_SW_PIO->PIO_PUER = _BV(USER_SW_BIT);

#ifdef INC_HUGE_DATA
    __b__ = __HUGE_DATA__[0];
    __e__ = __HUGE_DATA__[sizeof(__HUGE_DATA__) - 1];
#endif

    // Loop forever
    fast = !fast;

    for(;;) {

        USER_LED1_PIO->PIO_SODR = _BV(USER_LED1_BIT);
        USER_LED2_PIO->PIO_SODR = _BV(USER_LED2_BIT);
        delayAndCheckSwitch();

        USER_LED1_PIO->PIO_CODR = _BV(USER_LED1_BIT);
        USER_LED2_PIO->PIO_CODR = _BV(USER_LED2_BIT);
        delayAndCheckSwitch();

    } // for
}
