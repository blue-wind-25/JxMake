/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// NOTE : 'ISR(INT0_vect)' is defined in 'updi.h'


// Initialize INT0
static inline void int0Init()
{
	// Configure INT0 to trigger on the falling edge
	MCUCR |=  _BV(ISC01);
	MCUCR &= ~_BV(ISC00);

	// Clear the INT0 interrupt flag and enable the interrupt
	GIFR |= _BV(INTF0);
	GICR |= _BV(INT0);
}
