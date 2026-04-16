/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Include the standard header files
#include "../ATmega8A_Common/std_includes.h"

// Include the other header files
#include "../ATmega8A_Common/attributes.h"
#include "../ATmega8A_Common/serial.h"
#include "../ATmega8A_Common/tick.h"
#include "../ATmega8A_Common/watchdog.h"

#include "gpio.h"
#include "int0.h"

#include "bbs_8e2.h"
#include "updi.h"


// Firmware version numbers
#define FIRMWARE_VERSION_M 1
#define FIRMWARE_VERSION_N 0
#define FIRMWARE_VERSION_R 8


// Include the other header files
#include "../ATmega8A_Common/imsg.h"


// Detect trigger from PGC and PGD signals
#define DETECT_PGC_TIMEOUT_MS 100

static volatile uint8_t _pgcdCmd = 0;

static __never_inline bool detectPGCD()
{
	// NOTE : Please refer to '../../../JxMake_USB_GPIO-Protocol_Manual.txt' for the protocol details.

	const uint8_t triggerKeyPrefix[] = { 0x48, 0x56, 0x2D, 0x55, 0x50, 0x44, 0x49, 0x3A }; // "HV-UPDI:"

	// Simply return if PGC is 0
	if( !gget_PGC() ) return false;

	wdt_reset();

	// Check the trigger character sequence
	for(uint8_t i = 0; i < sizeof(triggerKeyPrefix); ++i) {

		// Get the trigger character
		const uint8_t tkch = triggerKeyPrefix[i];

		// Check the trigger character
		uint8_t mask = 0b10000000;

		for(uint8_t b = 0; b < 8; ++b) {

			// Get PGD
			const bool pgd = gget_PGD();

			// Wait for PGC ╽
			uint32_t begTick = tickGet();

			while( gget_PGC() ) {
				wdt_reset();
				if( ( tickGet() - begTick ) > DETECT_PGC_TIMEOUT_MS ) return false;
			}

			// Compare the bit
			const bool ref = (tkch & mask) != 0;

			if( (pgd && !ref) || (!pgd && ref) ) {
				wdt_reset();
				return false;
			}

			// Shift the mask
			mask >>= 1;

			// Wait for PGC ╿
			begTick = tickGet();

			while( !gget_PGC() ) {
				wdt_reset();
				if( ( tickGet() - begTick ) > DETECT_PGC_TIMEOUT_MS ) return false;
			}

		} // for

	} // for

	// Get the command type (the '?')
	_pgcdCmd = 0;

	for(uint8_t b = 0; b < 8; ++b) {

		// Get PGD
		const bool pgd = gget_PGD();

		// Wait for PGC ╽
		uint32_t begTick = tickGet();

		while( gget_PGC() ) {
			wdt_reset();
			if( ( tickGet() - begTick ) > DETECT_PGC_TIMEOUT_MS ) return false;
		}

		// Set the bit
		        _pgcdCmd <<= 1;
		if(pgd) _pgcdCmd  |= 0b00000001;

		// Wait for PGC ╿
		begTick = tickGet();

		while( !gget_PGC() ) {
			wdt_reset();
			if( ( tickGet() - begTick ) > DETECT_PGC_TIMEOUT_MS ) return false;
		}

	} // for

	// Check the command type
	const bool match = (_pgcdCmd == 0x4E) || (_pgcdCmd == 0x45); // 'N' or 'E'

	wdt_reset();

	// Return the result
	return match;
}
