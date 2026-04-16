#if defined( __MCS51_AT89S52__)
    #include <at89x52.h>
#elif defined( __MCS51_AT89S8253__)
    #include <at89s8253.h>
#else
    #error "unsupported MCU"
#endif

#include <stdint.h>


#define LED_1  P0_0
#define LED_2  P0_1
#define SWITCH P0_7


// Function from the user assembly code
extern int dummy_asm();


#define T_UNIT  147456UL
#define T_COUNT ( ( (F_CPU) + (T_UNIT / 2UL) ) / T_UNIT )

void __delay_ms(unsigned int value)
{
    for(unsigned int x = 0; x < value; ++x) {
        for(unsigned int y = 0; y < T_COUNT; ++y);
    }
}


static char led = 1; // Blink LED #2 on start

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < 10; ++i) {
        // Delay for a while
        __delay_ms(10);
        // Simply continue if the switch is not pressed
        if(SWITCH != 0) continue;
        // Switch the LED to be blinked
        led = (led == 0) ? 1 : 0;
        // Debouncing and wait until the switch is no longer pressed
        __delay_ms(100);
        while(SWITCH == 0);
        __delay_ms(100);
        // Break here
        break;
    }
}


void main()
{
    // Set all ports to input mode
    P0 = 0xFF;
    P1 = 0xFF;
    P2 = 0xFF;
    P3 = 0xFF;

    // Test function call from the assembly code
    dummy_asm();

    // Infinite loop
    while(1) {
        // Blink the LED, delay, and check the switch
        if(led == 0) { LED_1 = 1; LED_2 = 0; } else { LED_1 = 0; LED_2 = 0; }
        delayAndCheckSwitch();
        if(led == 1) { LED_1 = 0; LED_2 = 1; } else { LED_1 = 0; LED_2 = 0; }
        delayAndCheckSwitch();
    }
}

