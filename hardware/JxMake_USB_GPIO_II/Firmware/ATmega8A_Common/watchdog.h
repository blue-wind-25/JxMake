/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Handle watchdog on reset
void __handleMCUCSR(void) ATTR_INIT_SECTION(3);
void __handleMCUCSR(void)
{
	// Disable watchdog
	wdt_reset();
	WDTCR  = _BV(WDCE);
	MCUCSR = 0;
}


// Initialize watchdog timer
static __force_inline void wdtInit()
{
//	WDTCR = _BV(WDCE) | _BV(WDE) | _BV(WDP2)            ; // ~ 250mS
	WDTCR = _BV(WDCE) | _BV(WDE) | _BV(WDP1) | _BV(WDP0); // ~ 500mS
//	WDTCR = _BV(WDCE) | _BV(WDE) | _BV(WDP2) | _BV(WDP1); // ~1000mS
}
