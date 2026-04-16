#if 0
static constexpr uint32_t ELEM_COUNT                                           = 32767;
static const     uint8_t  __dummy_0__[ELEM_COUNT] __attribute__((__progmem__)) = { 0 };
static const     uint8_t  __dummy_1__[ELEM_COUNT] __attribute__((__progmem__)) = { 1 };
static const     uint8_t  __dummy_2__[ELEM_COUNT] __attribute__((__progmem__)) = { 2 };
static const     uint8_t  __dummy_3__[ELEM_COUNT] __attribute__((__progmem__)) = { 3 };
static const     uint8_t  __dummy_4__[ELEM_COUNT] __attribute__((__progmem__)) = { 4 };
static const     uint8_t  __dummy_5__[ELEM_COUNT] __attribute__((__progmem__)) = { 5 };

for(uint32_t i = 0; i < ELEM_COUNT; ++i) {
    DDRA = __dummy_0__[i] & __dummy_1__[i] & __dummy_2__[i];
    DDRB = __dummy_3__[i] & __dummy_4__[i] & __dummy_5__[i];
}
#endif

// Blink the LED on PB.7 (Arduino pin 13)
DDRB |= _BV(7);

for(;;) {
    for(unsigned i = 0; i < 3; ++i) {
        PORTB |=  _BV(7); _delay_ms(delay_s);
        PORTB &= ~_BV(7); _delay_ms(delay_s);
    }
    _delay_ms(delay_l);
}
