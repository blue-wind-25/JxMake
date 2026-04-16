/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Include the primary include file
#include "util.h"


// Main program entry point
int main(void)
{
	// Initialize UART and stream
	uartInit();
	streamInit();

	// Initialize tick
	tickInit();

	// Print the 1st informational message
	printIMsg1( PSTR("AVR-UPDI") );

	// Initialize GPIO
	gpioInit();

	// Initialize INT0
	int0Init();

	// Enable interrupt
	sei();

	// Print the 2nd informational message
	printIMsg2(0);

	// Initialize watchdog timer
	wdtInit();

	// The main loop
	uint32_t prvTick = 0;

	for(;;) {

		// Get the UPDI state
		const uint8_t us = updiGetState();

		// Monitor the UPDI line to check if the upstream programmer has completed the programming process
		if(us != UPDI_STATE_IDLE) {
			if(us == UPDI_STATE_TIMEOUT) {
				updiEnd();
				printf_P( PSTR(">>> End\n\n") );
			}
			else {
				const uint32_t curTick = tickGet();
				if( prvTick != curTick && (curTick % 1000) == 0 ) {
					printf_P( PSTR("...\n") );
					prvTick = curTick;
				}
			}
		}

		// Process the UPDI begin triggered by the nTRIGGER button
		else if( gget_nTRIGGER() ) {
			printf_P( PSTR(">>> Manual trigger\n") );
			updiBegin(true);
		}

		// Process the UPDI begin triggered by the PGC and PGD signals
		else if( detectPGCD() ) {
			printf_P( PSTR(">>> PGC-PGD trigger\n") );
			updiBegin(_pgcdCmd == 0x45); // 'E'
		}

		// Process the UPDI begin triggered by the the upstream programmer
		else if(_updi_sense != 0) {
			printf_P( PSTR(">>> Upstream UPDI trigger\n") );
			updiBegin(false);
			_updi_sense = 0;
		}

		// Reset watchdog
		wdt_reset();

	} // for

	return 0;
}
