// Blink the LED on PC.7 (Arduino pin 13)
DDRC |= _BV(7);

for(;;) {
    for(unsigned i = 0; i < 3; ++i) {
        PORTC |=  _BV(7); _delay_ms(delay_s);
        PORTC &= ~_BV(7); _delay_ms(delay_s);
    }
    _delay_ms(delay_l);
}
