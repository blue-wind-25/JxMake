/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// NOTE : 'ISR(INT0_vect)' is defined in 'updi.h'


// INT0 pin
#define INT0_DDR  DDRD
#define INT0_PORT PORTD
#define INT0_PIN  PIND
#define INT0_BIT  PD2


// Initialize INT0
static inline void int0Init()
{
	// Configure the INT0 pin as an input with pull-up
	INT0_DDR  &= ~_BV(INT0_BIT);
	INT0_PORT |=  _BV(INT0_BIT);

	// Configure INT0 to trigger on the falling edge
	EICRA |=  _BV(ISC01);
	EICRA &= ~_BV(ISC00);

	// Clear the INT0 interrupt flag and enable the interrupt
	EIFR  |= _BV(INTF0);
	EIMSK |= _BV(INT0);

	// Print message
	printIMsgDone( PSTR("INT0") );
}


// Manipulate the INT0 pin
static __force_inline void int0EnaPU() { INT0_PORT |=  _BV(INT0_BIT); }
static __force_inline void int0DisPU() { INT0_PORT &= ~_BV(INT0_BIT); }

static __force_inline bool int0Read () { return ( INT0_PIN & _BV(INT0_BIT) ) == 0; }
