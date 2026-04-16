/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Timer0 ISR
static volatile uint32_t _timer0_tick = 0;

ISR(TIMER0_OVF_vect)
{ ++_timer0_tick; }


// Initialize tick
static inline void tickInit()
{
	// Enable Timer0 with prescaler 64x to generate a ~1mS tick
	TCNT0 = 173;
	TCCR0 = _BV(CS01) | _BV(CS00);

	// Enable Timer0 interrupt
	TIMSK |= _BV(TOV0);
}


// Get tick (atomic)
static __force_inline uint32_t tickGet()
{
	cli();
	const uint32_t tick = _timer0_tick;
	sei();

	return tick;
}

static __force_inline void tickGetTo(volatile uint32_t* tick)
{
	cli();
	*tick = _timer0_tick;
	sei();
}


// Function to perform a delay while regularly resetting the watchdog timer during sufficiently long delays
static __force_inline void _delayMS_impl(uint8_t ms)
{
	const uint32_t beg = tickGet();

	for(;;) {

		wdt_reset();

		const uint32_t cur = tickGet();

		if( (cur - beg) >= ms ) break;

	} // for
}

static __force_inline void delayMS(double ms)
{
	if(ms <= 200) {
		_delayMS_impl(ms);
	}

	else {

		for( uint16_t i = 0; i < ( (uint16_t) (ms / 200) ); ++i ) {
			_delayMS_impl(200);
		}
	}
}


// Delay uS
#define delayUS(T) _delay_us(T)
