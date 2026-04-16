/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// LED pins
#define LED_IND_DDR  DDRA
#define LED_IND_PORT PORTA
#define LED_IND_BIT  PA1   // Active high

#define LED_ERR_DDR  DDRB
#define LED_ERR_PORT PORTB
#define LED_ERR_BIT1 PB5   // OV : A=5 C=6
#define LED_ERR_BIT2 PB6   // UV : C=5 A=6


// Initialize LEDs
static inline void ledInit()
{
	// Configure the pins used for controlling the LEDs as outputs initialized with their inactive states
	LED_IND_DDR  |=  _BV(LED_IND_BIT);
	LED_IND_PORT &= ~_BV(LED_IND_BIT);

	LED_ERR_DDR  |=  ( _BV(LED_ERR_BIT1) | _BV(LED_ERR_BIT2) );
	LED_ERR_PORT &= ~( _BV(LED_ERR_BIT1) | _BV(LED_ERR_BIT2) );

	// Print message
	printIMsgDone( PSTR("LEDs") );
}


// Animate the indicator LED
static bool __indLEDState = false;

static __force_inline void ledIndOn ()
{
	LED_IND_PORT |=  _BV(LED_IND_BIT);
	__indLEDState =  true;
}

static __force_inline void ledIndOff()
{
	LED_IND_PORT &= ~_BV(LED_IND_BIT);
	__indLEDState =  false;
}

static __force_inline void ledIndToggle()
{
	if(__indLEDState) ledIndOff();
	else              ledIndOn ();
}


// Helper function to display the 'System Halted' message and disable interrupt
static inline void __print_SystemHalted()
{
	printIMsgln( PSTR("\n##### SYSTEM HALTED #####\n") );
	cli();
}


// Blink the LEDs that indicate fatal error and halt
static inline void ledFEHalt()
{
	__print_SystemHalted();

	for(;;) {

		LED_ERR_PORT |=  _BV(LED_ERR_BIT1);
		delayMS(200);

		LED_ERR_PORT &= ~_BV(LED_ERR_BIT1);
		delayMS(200);

		LED_ERR_PORT |=  _BV(LED_ERR_BIT2);
		delayMS(200);

		LED_ERR_PORT &= ~_BV(LED_ERR_BIT2);
		delayMS(200);

	} // for
}


// Blink the LED that indicates overvoltage and halt
static inline void ledOVHalt()
{
	__print_SystemHalted();

	for(;;) {

		LED_ERR_PORT |=  _BV(LED_ERR_BIT1);
		delayMS(200);

		LED_ERR_PORT &= ~_BV(LED_ERR_BIT1);
		delayMS(200);

	} // for
}


// Blink the LED that indicates undervoltage and halt
static inline void ledUVHalt()
{
	__print_SystemHalted();

	for(;;) {

		LED_ERR_PORT |=  _BV(LED_ERR_BIT2);
		delayMS(200);

		LED_ERR_PORT &= ~_BV(LED_ERR_BIT2);
		delayMS(200);

	} // for
}
