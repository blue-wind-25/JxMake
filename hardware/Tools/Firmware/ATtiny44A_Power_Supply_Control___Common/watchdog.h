/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Handle watchdog on reset
void __handleMCUSR(void) ATTR_INIT_SECTION(3);
void __handleMCUSR(void)
{
	// Disable interrupt
	cli();

	// Disable watchdog
	wdt_reset();
	MCUSR   = 0x00;
	WDTCSR |= _BV(WDCE) | _BV(WDE);
	WDTCSR  = 0x00;
}


// Initialize watchdog timer
static __force_inline void wdtInit()
{
	// Enable watchdog
	WDTCSR = _BV(WDCE) | _BV(WDE);
	WDTCSR = _BV(WDP2);            // ~250mS
}
