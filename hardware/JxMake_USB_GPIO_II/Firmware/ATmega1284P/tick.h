/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Timer0 ISR
static volatile uint32_t __timer0_tick = 0;

ISR(TIMER0_OVF_vect)
{ ++__timer0_tick; }


// Initialize tick using Timer0
static inline void tickInit()
{
	// Enable Timer0 with prescaler 64x to generate a ~1mS tick
	TCNT0  = 173;
	TCCR0B = _BV(CS01) | _BV(CS00);

	// Enable Timer0 interrupt
	TIMSK0 |= _BV(TOIE0);

	// Print message
	printIMsgDone( PSTR("Tick (Timer0)") );
}


// Get tick (atomic)
static __force_inline uint32_t tickGet()
{
	cli();
	const uint32_t tick = __timer0_tick;
	sei();

	return tick;
}

static __force_inline void tickGetTo(volatile uint32_t* tick)
{
	cli();
	*tick = __timer0_tick;
	sei();
}


// Delays in milliseconds while periodically resetting the watchdog timer during extended wait periods
static __force_inline void __delayMS_impl(const uint8_t ms)
{
	const uint32_t beg = tickGet();

	for(;;) {

		wdt_reset();

		const uint32_t cur = tickGet();

		if( (cur - beg) >= ms ) break;

	} // for
}

static __force_inline void delayMS(const double ms)
{
	if(ms <= 200) {
		__delayMS_impl(ms);
	}

	else {

		for( uint16_t i = 0; i < ( (uint16_t) (ms / 200) ); ++i ) {
			__delayMS_impl(200);
		}
	}
}


// Microsecond delay
#define delayUS(T) _delay_us(T)
