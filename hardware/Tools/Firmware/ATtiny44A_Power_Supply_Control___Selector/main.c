/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Include the primary header file
#include "../ATtiny44A_Power_Supply_Control___Common/util.h"


// Define the GPIO usage
/*
 * Port-A#   7 6 5 4 3 2        1        0
 *           I I I I I ADC2     ADC1     I
 *           X X X X X ADC_VIn2 ADC_VIn1 X
 *
 * Port-B    7 6 5 4 3 2        1        0
 *           I I I I I I        O        O
 *           X X X X X X        EN_VIn2  EN_VIn1
 */
#define INIT_BM_DDRA  0b00000000
#define INIT_BM_PORTA 0b00000000

#define INIT_BM_DIDR0 0b00000110

#define ADC_MUX_VIn1  _BV(MUX0)   // ADC input (VIn1 / 5.7)
#define ADC_MUX_VIn2  _BV(MUX1)   // ADC input (VIn2 / 5.7)


#define INIT_BM_DDRB  0b00000011
#define INIT_BM_PORTB 0b00000000

#define EN_VIn1_Port      PORTB
#define EN_VIn1_BM    _BV(PORTB0) // Output(active high)

#define EN_VIn2_Port      PORTB
#define EN_VIn2_BM    _BV(PORTB1) // Output(active high)

#include <avr/io.h>


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
	DIDR0 = INIT_BM_DIDR0;

	PORTB = INIT_BM_PORTB;
	DDRB  = INIT_BM_DDRB;

	// Initialize ADC
	adcInit();

	// The main loop
	uint8_t selectedVIn = 0;

	for(;;) {

		wdt_reset();

		// Read VIn1 and VIn2
		const uint16_t vIn1 = v100Read(ADC_MUX_VIn1);
		const uint16_t vIn2 = v100Read(ADC_MUX_VIn2);

		wdt_reset();

		// Select one of the VIn
		uint8_t vSel = 0;

#if defined(VIN1_PRIMARY)
		     if(vIn1 >= 420) vSel = 1; // Priority : VIn1
		else if(vIn2 >= 420) vSel = 2;
#elif defined(VIN2_PRIMARY)
		     if(vIn2 >= 420) vSel = 2; // Priority : VIn2
		else if(vIn1 >= 420) vSel = 1;
#else
		#error "Please define VIN1_PRIMARY or VIN2_PRIMARY"
#endif

		wdt_reset();

		if(selectedVIn == vSel) continue;
		selectedVIn = vSel;

		// Turn off first
		EN_VIn1_Port &= ~EN_VIn1_BM;
		EN_VIn2_Port &= ~EN_VIn2_BM;

		delayMS(250);

		if(selectedVIn == 0) continue;

		delayMS(500);

		// Turn on
		     if(selectedVIn == 1) EN_VIn1_Port |=  EN_VIn1_BM; // Select VIn1
		else if(selectedVIn == 2) EN_VIn2_Port |=  EN_VIn2_BM; // Select VIn2

		delayMS(250);

	} // for

	return 0;
}
