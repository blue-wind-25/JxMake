/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Delay mS
static __force_inline void delayMS(double ms)
{
	wdt_reset();

	if(ms <= 10) {
		_delay_ms(10);
		wdt_reset();
	}

	else {
		for( uint16_t i = 0; i < ( (uint16_t) (ms / 10) ); ++i ) {
			_delay_ms(10);
			wdt_reset();
		}
	}
}


// Delay uS
#define delayUS(T) _delay_us(T)
