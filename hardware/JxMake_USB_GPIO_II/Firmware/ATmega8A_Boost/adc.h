/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// ADC pin
#define ADC_DDR DDRC
#define ADC_BIT PC0

// Voltage constants; please refer to '../../KiCad8/High_Voltage_Attachment/NOTES.txt' for more details
#define ADC_13V 847
#define ADC_12V 783
#define ADC_11V 719
#define ADC_10V 655
#define ADC_09V 591
#define ADC_08V 527


// NOTE : 'ISR(ADC_vect)' is defined in 'boost.h'


// Initialize ADC
static inline void adcInit()
{
	// Set PC.0 (ADC0) as input
	ADC_DDR &= ~_BV(ADC_BIT);

	/*
	 * Configure ADC : ADC interrupt enabled, prescaler 128x
	 *
	 * With F_CPU = 11.0592MHz, the ADC clock will be 86.4kHz.
	 *
	 * It requires 13 clock cycles for a single conversion; hence, the interrupt will fire at
	 * most ~6646.15 times per second.
	 */
	ADCSRA = _BV(ADIE) | _BV(ADPS2) | _BV(ADPS1) | _BV(ADPS0);

	// Select PC.0 (ADC0) with internal 2.56V reference
	ADMUX = _BV(REFS1) | _BV(REFS0);

	// Enable the ADC and start the first conversion
	ADCSRA |= _BV(ADEN) | _BV(ADSC);
}
