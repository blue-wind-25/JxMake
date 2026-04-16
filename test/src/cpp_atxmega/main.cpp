#include <avr/io.h>
#include <util/delay.h>


#if F_CPU != 32000000UL
    #error "F_CPU must be set to 32000000UL"
#endif


static char index = 0;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < 10; ++i) {
        // Delay for a while
        _delay_ms(10);
        // Check if the switch is pressed
        if( !(PORTC.IN & PIN7_bm) ) {
            // Debouncing
            _delay_ms(100);
            // Update the index and break
            ++index;
            if(index > 2) index = 0;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !(PORTC.IN & PIN7_bm) );
}


int main()
{
    // Configure the clock source
    OSC_CTRL |= OSC_RC32MEN_bm;                       // Enabled the internal 32MHz oscillator
    while( !(OSC_STATUS & OSC_RC32MRDY_bm) );         // Wait for the oscillator to finish starting
    _PROTECTED_WRITE(CLK_CTRL, CLK_SCLKSEL_RC32M_gc); // Select the 32MHz oscillator as the system clock

    // Set PD.0/1/2 to output and PC.7 to input
    PORTC.DIRCLR = PIN7_bm;
    PORTD.DIRSET = PIN0_bm | PIN1_bm | PIN2_bm;

    // Blink PD.0/1/2
    for(;;) {
        for(unsigned i = 0; i < 3; ++i) {
            // Turn on one LED (the LEDs are active low)
                 if(index == 0) PORTD.OUTCLR = PIN0_bm;
            else if(index == 1) PORTD.OUTCLR = PIN1_bm;
            else if(index == 2) PORTD.OUTCLR = PIN2_bm;
            // Delay and check the switch
            delayAndCheckSwitch();
            // Turn off all LEDs
            PORTD.OUTSET = PIN0_bm | PIN1_bm | PIN2_bm;
            // Delay and check the switch
            delayAndCheckSwitch();
        }
        // Delay and check the switch
        delayAndCheckSwitch();
        delayAndCheckSwitch();
        delayAndCheckSwitch();
    }

    return 0;
}
