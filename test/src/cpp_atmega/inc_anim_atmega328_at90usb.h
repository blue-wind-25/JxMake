#if 1
// Blink the LED on PB.5 (Arduino pin 13 for ATmega328P)
DDRB |= _BV(5);

for(;;) {
    for(unsigned i = 0; i < 3; ++i) {
        PORTB |=  _BV(5); _delay_ms(delay_s);
        PORTB &= ~_BV(5); _delay_ms(delay_s);
    }
    _delay_ms(delay_l);
}
#else

// Alternate the logic levels on PC.2 and PC.3 (Arduino pins 16 and 17 for ATmega328P)
DDRC |= _BV(2);
DDRC |= _BV(3);

for(;;) {
    for(unsigned i = 0; i < 3; ++i) {
        PORTC |=  _BV(2); PORTC &= ~_BV(3); _delay_ms(delay_s);
        PORTC &= ~_BV(2); PORTC |=  _BV(3); _delay_ms(delay_s);
    }
    _delay_ms(delay_l);
}
#endif
