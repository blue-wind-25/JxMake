/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Handle watchdog on reset
uint8_t __mcusr        ATTR_NO_INIT;
uint8_t __urboot_mcusr ATTR_NO_INIT;

void __handleMCUSR(void) ATTR_INIT_SECTION(0);
void __handleMCUSR(void)
{
	// URBOOT saves the reset flags in R2
	// NOTE : It may always contains 0b00001000!
	__asm__ __volatile__ ( "sts %0, r2 \n\t" : "=m" (__urboot_mcusr) : );

	cli();
	wdt_reset();

	WDTCSR |= _BV(WDCE) | _BV(WDE);
	WDTCSR  = _BV(WDCE);

	__mcusr = MCUSR;
	MCUSR   = 0;

	if(__mcusr == 0) __mcusr = __urboot_mcusr;
}


// Initialize watchdog timer
static inline void wdtInit()
{
//	WDTCSR = _BV(WDCE) | _BV(WDE) | _BV(WDP2)            ; // ~ 250mS
	WDTCSR = _BV(WDCE) | _BV(WDE) | _BV(WDP1) | _BV(WDP0); // ~ 500mS
//	WDTCSR = _BV(WDCE) | _BV(WDE) | _BV(WDP2) | _BV(WDP1); // ~1000mS

	// Print message
	printIMsgDone( PSTR("Watchdog Timer") );
}
