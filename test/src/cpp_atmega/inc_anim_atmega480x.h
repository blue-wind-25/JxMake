// Set CPU frequency
#if (F_CPU == 16000000UL) || (F_CPU == 20000000UL)
    // The actual CPU frequency depends on FUSE.OSCCFG[1:0] : 0x1 = 16MHz ; 0x2 = 20MHz
    _PROTECTED_WRITE(CLKCTRL.MCLKCTRLA, CLKCTRL_CLKSEL_OSC20M_gc);
    _PROTECTED_WRITE(CLKCTRL_MCLKCTRLB, 0x00                    );
#else
    #error "unsupported CPU frequency"
#endif

#if defined(__AVR_ATmega4809__)
    // The LED is connected to PE.2 (Arduino pin 13)
    #define LED_PORT PORTE
#else
    // The LED is connected to PC.2 (Thinary pin 13)
    #define LED_PORT PORTC
#endif
    #define LED_PIN  PIN2_bm

// Blink the LED
LED_PORT.DIRSET = LED_PIN;

for(;;) {
    for(unsigned i = 0; i < 3; ++i) {
        LED_PORT.OUTSET = LED_PIN; _delay_ms(delay_s);
        LED_PORT.OUTCLR = LED_PIN; _delay_ms(delay_s);
    }
    _delay_ms(delay_l);
}
