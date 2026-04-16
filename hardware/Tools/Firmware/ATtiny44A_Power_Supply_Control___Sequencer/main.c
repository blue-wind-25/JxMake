/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Include the primary header file
#include "../ATtiny44A_Power_Supply_Control___Common/util.h"


// Define the GPIO usage
/*
 * Port-A#   7        6 5 4 3         2         1         0
 *           I        I I I O         O         O         O
 *           nLongDly X X X nEN_VOut1 nEN_VOut2 nEN_VOut3 nEN_VOut4
 *
 * Port-B    7        6 5 4 3         2         1         0
 *           I        I I I I         I         I         I
 *           X        X X X X         nPEn      X         X
 */
#define INIT_BM_DDRA   0b00001111
#define INIT_BM_PORTA  0b10001111

#define nEN_VOut1_Port     PORTA
#define nEN_VOut1_BM   _BV(PORTA3) // Output        (active low)

#define nEN_VOut2_Port     PORTA
#define nEN_VOut2_BM   _BV(PORTA2) // Output        (active low)

#define nEN_VOut3_Port     PORTA
#define nEN_VOut3_BM   _BV(PORTA1) // Output        (active low)

#define nEN_VOut4_Port     PORTA
#define nEN_VOut4_BM   _BV(PORTA0) // Output        (active low)

#define nLongDly_Pin       PINA
#define nLongDly_BM    _BV(PORTA7) // Input pull-up (active low)


#define INIT_BM_DDRB   0b00000000
#define INIT_BM_PORTB  0b00000100

#define nPEn_Pin           PINB
#define nPEn_BM        _BV(PINB2)  // Input pull-up (active low)


// Main program entry point
int main(void)
{
	// Embed firmware version in flash
	static const char ATTR_USED PROGMEM __firmware_version__[] = "FW_VERSION=1.0.2";
	asm volatile( "" :: "r"(__firmware_version__) );

	// Initialize watchdog timer
	wdtInit();

    // Initialize GPIO
	PORTA = INIT_BM_PORTA;
	DDRA  = INIT_BM_DDRA;

	PORTB = INIT_BM_PORTB;
	DDRB  = INIT_BM_DDRB;

	// The main loop
	bool powerOn = false;

	for(;;) {

		wdt_reset();

		// Check if the power should be enabled
		const bool PEn = !readPinDB(&nPEn_Pin, nPEn_BM);

		wdt_reset();

		if(powerOn == PEn) continue;
		powerOn = PEn;

		// Determine whether to apply longer delay
		const bool longerDelay = !readPin(&nLongDly_Pin, nLongDly_BM);

		wdt_reset();

		// Sequence the power off as fast as possible
		if(!powerOn) {
			nEN_VOut4_Port |= nEN_VOut4_BM; wdt_reset();
			nEN_VOut3_Port |= nEN_VOut3_BM; wdt_reset();
			nEN_VOut2_Port |= nEN_VOut2_BM; wdt_reset();
			nEN_VOut1_Port |= nEN_VOut1_BM; wdt_reset();
		}

		// Sequence the power on with delay
		else {
			delayMS(longerDelay ? 1500 : 750); nEN_VOut1_Port &= ~nEN_VOut1_BM; wdt_reset();
			delayMS(longerDelay ? 1500 : 750); nEN_VOut2_Port &= ~nEN_VOut2_BM; wdt_reset();
			delayMS(longerDelay ? 1500 : 750); nEN_VOut3_Port &= ~nEN_VOut3_BM; wdt_reset();
			delayMS(longerDelay ? 1500 : 750); nEN_VOut4_Port &= ~nEN_VOut4_BM; wdt_reset();
		}

	} // for

	return 0;
}
