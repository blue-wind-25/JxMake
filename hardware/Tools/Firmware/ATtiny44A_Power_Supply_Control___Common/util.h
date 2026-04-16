/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Include the standard header files
#include "std_includes.h"

// Include the other header files
#include "attributes.h"
#include "delay.h"
#include "watchdog.h"


// Read pin without debouncing
static __force_inline bool readPin(volatile uint8_t* pin_reg, uint8_t bit_mask)
{ return (*pin_reg & bit_mask) != 0; }


// Read pin with debouncing
#define RPDB_DEBOUNCE_DELAY_MS 20
#define RPDB_STABLE_COUNT      5

static __never_inline bool readPinDB(volatile uint8_t* pin_reg, uint8_t bit_mask)
{
	uint8_t stable     = 0;
	bool    last_state = (*pin_reg & bit_mask) != 0;

	while(stable < RPDB_STABLE_COUNT) {

		delayMS(RPDB_DEBOUNCE_DELAY_MS);

		const bool current_state = (*pin_reg & bit_mask) != 0;

		if(current_state == last_state) {
			++stable;
		}
		else {
			stable = 0;
			last_state = current_state;
		}

	} // while

	return last_state;
}


// Initialize ADC
static __never_inline void adcInit()
{
	ADMUX  = _BV(REFS1);                                       // Internal 1.1V voltage reference
	ADCSRA = _BV(ADEN) | _BV(ADPS2) | _BV(ADPS1) | _BV(ADPS0); // Prescaler 128
}


// Read ADC
static __never_inline uint16_t adcRead(uint8_t bit_mask)
{
	ADMUX = (ADMUX & 0b11000000) | bit_mask;

	ADCSRA |= _BV(ADSC);                     // Start conversion
	while( ADCSRA & _BV(ADSC) ) wdt_reset(); // Wait until conversion completes

	return ADC;
}


// Read (VIn * 100)
static __force_inline uint16_t v100Read(uint8_t bit_mask)
{
	const uint8_t REP_CNT = 4;

	uint16_t sum = 0;

	for(uint8_t r = 0; r < REP_CNT; ++r) {

		delayMS(10);

		const uint32_t val = adcRead(bit_mask);

		sum += (uint16_t) ( ( (5.7f * 1100UL) * val + 5120UL ) / 10240UL );

	} // for

	return sum / REP_CNT;
}
