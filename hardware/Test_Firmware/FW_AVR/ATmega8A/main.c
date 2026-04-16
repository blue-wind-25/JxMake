#include <avr/io.h>
#include <util/delay.h>


#define LED_DDR  DDRB
#define LED_PORT PORTB
#define LED_BIT  PB5


int main()
{
    LED_DDR |= _BV(LED_BIT);

    for(;;) {
        for(uint8_t i = 0; i < 3; ++i) {
            LED_PORT |=  _BV(LED_BIT); _delay_ms(250);
            LED_PORT &= ~_BV(LED_BIT); _delay_ms(250);
        }
        _delay_ms(500);
    }

    return 0;
}
