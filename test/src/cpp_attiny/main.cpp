#include <avr/io.h>
#include <util/delay.h>


static bool fast = false;

static inline bool __readSwitch()
{
#if defined(__AVR_ATtiny3226__)
    return PORTA.IN & PIN6_bm;
#else
    return PINB & _BV(2);
#endif
}

static void checkSwitch()
{
    // Simply return if the switch is not pressed
    if( __readSwitch() ) return;

    // Debouncing
    _delay_ms(100);

    // Wait until the switch is no longer pressed
    while( !__readSwitch() );

    // Change the speed flag
    fast = !fast;
}


int main()
{
#if defined(__AVR_ATtiny3226__)

    // Set CPU frequency
    #if (F_CPU == 16000000UL) || (F_CPU == 20000000UL)
        // The actual CPU frequency depends on FUSE.OSCCFG[1:0] : 0x1 = 16MHz ; 0x2 = 20MHz
        _PROTECTED_WRITE(CLKCTRL.MCLKCTRLA, CLKCTRL_CLKSEL_OSC20M_gc);
        _PROTECTED_WRITE(CLKCTRL_MCLKCTRLB, 0x00                    );
    #else
        #error "unsupported CPU frequency"
    #endif

    // Set PA.4/5 to output and PA.6 to input with pull-up
    PORTA.DIRSET = PIN4_bm;
    PORTA.DIRSET = PIN5_bm;
    PORTA.DIRCLR = PIN6_bm; PORTA.PIN6CTRL |= PORT_PULLUPEN_bm;

    // Initialize TCA0 for PWM so that for every 1MHz CPU clock, it will produce a PWM frequency of ~122Hz
    PORTMUX.TCAROUTEA = 0x00;                                                // Route signal to PB [0:2] and PA [3:5]
    TCA0.SPLIT.CTRLD  = TCA_SINGLE_SPLITM_bm;                                // Enable split mode
    TCA0.SPLIT.LPER   = 0xFF;                                                // Set the maximum 8-bit period
    TCA0.SPLIT.HPER   = 0xFF;                                                // Set the maximum 8-bit period
    TCA0.SPLIT.LCMP0  = 0x00;                                                // Set 0% duty cycle on start
    TCA0.SPLIT.HCMP0  = 0x00;                                                // Set 0% duty cycle on start
    TCA0.SPLIT.LCMP1  = 0x00;                                                // Set 0% duty cycle on start
    TCA0.SPLIT.HCMP1  = 0x00;                                                // Set 0% duty cycle on start
    TCA0.SPLIT.LCMP2  = 0x00;                                                // Set 0% duty cycle on start
    TCA0.SPLIT.HCMP2  = 0x00;                                                // Set 0% duty cycle on start
    TCA0.SPLIT.CTRLA  = (TCA_SPLIT_CLKSEL_DIV16_gc) | (TCA_SPLIT_ENABLE_bm); // Set prescaler to 16 and enable the timer
    TCA0.SPLIT.CTRLB |= TCA_SPLIT_HCMP1EN_bm;                                // Enable WO4 (PA.4)
    TCA0.SPLIT.CTRLB |= TCA_SPLIT_HCMP2EN_bm;                                // Enable WO5 (PA.5)

    // Animate PA.4/5 (WO4/5) (the LEDs are active low)
    for(;;) {
        for(int i = 0; i <= 255; ++i) {
            TCA0.SPLIT.HCMP1  =       i;
            TCA0.SPLIT.HCMP2  = 255 - i;
            if(fast) _delay_ms(1);
            else     _delay_ms(5);
            checkSwitch();
        }
        for(int i = 255; i >= 0; --i) {
            TCA0.SPLIT.HCMP1  =       i;
            TCA0.SPLIT.HCMP2  = 255 - i;
            if(fast) _delay_ms(1);
            else     _delay_ms(5);
            checkSwitch();
        }
    }

#else

    // Standard test
    #ifndef MINIMAL

        // Set PB.0/1 to output and PB.2 to input without pull-up
        DDRB |=  _BV(0);
        DDRB |=  _BV(1);
        DDRB &= ~_BV(2);

        // Initialize PWM for PB.0/1 (the LEDs are active low)
        TCCR0A = _BV(COM0A1) | _BV(COM0A0) | _BV(COM0B1) | _BV(COM0B0) | _BV(WGM00); // Set OC0A and OC0B on compare match; 8-bit phase correct PWM
        TCCR0B = _BV(CS00  );                                                        // No prescaler

        // Animate PB.0/1 (the LEDs are active low)
        for(;;) {
            for(int i = 0; i <= 255; ++i) {
                OCR0A =       i;
                OCR0B = 255 - i;
                if(fast) _delay_ms(1);
                else     _delay_ms(5);
                checkSwitch();
            }
            for(int i = 255; i >= 0; --i) {
                OCR0A =       i;
                OCR0B = 255 - i;
                if(fast) _delay_ms(1);
                else     _delay_ms(5);
                checkSwitch();
            }
        }

    // Minimal test
    #else

        (void) checkSwitch;

        // Set PB.2 to output
        DDRB |= _BV(2);

        // Animate PB.2 (the LED is active high)
        for(;;) {
            PORTB |=  _BV(2); _delay_ms(250);
            PORTB &= ~_BV(2); _delay_ms(250);
        }

    #endif

#endif

    return 0;
}
