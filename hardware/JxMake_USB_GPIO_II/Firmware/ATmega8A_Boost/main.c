/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Include the primary include file
#include "util.h"


/*
 * ##### SPECIAL NOTES - VR CALIBRATION MODE #####
 * ##### SPECIAL NOTES - VR CALIBRATION MODE #####
 * ##### SPECIAL NOTES - VR CALIBRATION MODE #####
 *
 * 1st Stage
 *     1. Ensure that no firmware is present in the flash memory (having just the bootloader is
 *        acceptable).
 *     2. Measure the Vdd using a multimeter and note its value for reference.
 *     3. Adjust the VR until the voltage on PC.0 (ADC0) reads (Vdd / 6.25).
 *
 * 2nd Stage
 *     1. Compile and upload the firmware with CALIBRATE_VR set to 1 in the Makefile.
 *     2. Use a terminal program to read the resulting value, and then adjust the VR to further
 *        refine it, as necessary.
 *
 * 3rd Stage
 *     1. Compile and upload the firmware with CALIBRATE_VR set to 0 in the Makefile to enable
 *        normal firmware operation.
 *     2. Select 8V from the DIP switch and attach a multimeter to the boost converter output.
 *     3. Adjust the VR so the multimeter reads ~8.25V.
 *
 * Example:
 *     Vdd        = ~5.125
 *     Vdd / 6.25 = ~0.82                                         ->   use this value for the 1st stage
 *
 *     ADC Conversion Result = 0.82 / 2.56 * 1023 = 328 (0x148)   ->   use this value for the 2nd stage
 *
 * The VR adjustments in the 2nd stage should be relatively minor. Assuming the total tolerance of your
 * fixed resistor and VR is T%, if the changes exceed ±T%, it may be worth verifying that everything is
 * properly assembled.
 *
 * The 3rd stage is necessary due to variations in the characteristics of the inductor, ADC, and other
 * components. The VR adjustments in this stage should remain within ±5% the original value; otherwise,
 * it may be worth verifying that everything is properly assembled.
 *
 * ##### SPECIAL NOTES - VR CALIBRATION MODE #####
 * ##### SPECIAL NOTES - VR CALIBRATION MODE #####
 * ##### SPECIAL NOTES - VR CALIBRATION MODE #####
 */


// Main program entry point
#define MANUAL_TRIGGER_LENGTH_MS 5000

int main(void)
{
	// Initialize UART and stream
	uartInit();
	streamInit();

	// Initialize tick
	tickInit();

	// Initialize ADC
	adcInit();

	// Print the 1st informational message
	printIMsg1( PSTR("Boost Converter and AVR-ISP/TPI & PIC-ICSP") );

#if CALIBRATE_VR

	// ===== VR calibration mode =====

	// Print the 2nd informational message
	printIMsg2( PSTR("## VR CALIBRATION MODE ##") );

	// Enable interrupt
	sei();

	// Initialize watchdog timer
	wdtInit();

	// Loop forever
	for(;;) wdt_reset();

#else

	// ===== Normal operation mode =====

	// Initialize GPIO
	gpioInit();

	// Get the initial boost voltage
	boostVoltage = boostGetDIPVoltage();

	// Enable interrupt
	sei();

	// Initialize PWM
	pwmInit();

	// Print the 2nd informational message
	printIMsg2(0);

	// Initialize watchdog timer
	wdtInit();

	// The main loop
	for(;;) {

		/*
		 * Process the Vpp/Vdd sequence triggered by the nRST signal.
		 *
		 *     nRST       ──────┐                                                ┌─────────── High
		 *                      └──────────────────────┈┈┈┈┈┈┈───────────────────┘            Low
		 *
		 *                        ┌────────────────────┈┈┈┈┈┈┈─────────────────────┐          Vpp
		 *                        │                                                │
		 *                        │                                                │
		 *     VRst-HV    ────────┘                                                └───────── 0 or Vdd
		 *                        ↦↤ ≥ 90nS                                       ↦↤ ≥ 90nS
		 *                         ┌───────────────────┈┈┈┈┈┈┈────────────────────┐           Vdd
		 *     VTarget    ─────────┘                                              └────────── 0
		 *
		 * If the 'Vdd_Trg-Always-On' jumper is connected, then VTarget is always on.
		 */
		if( gget_nRST() ) {
			printf_P( PSTR(">>> Reset signal - begin (ADC_Ref = %d)\n"), boostVoltage );
			enaVppVdd();
			// Wait for nRST to go high, resetting watchdog without delay
			while( gget_nRST() ) delayMS(0);
			disVppVdd();
			printf_P( PSTR(">>> Reset signal - end\n\n") );
		}

		/*
		 * Process the Vpp/Vdd sequence triggered by the nTRIGGER button.
		 *
		 *     nTRIGGER   ──────┐   ┌──────────────────────────────────────────────────────── High
		 *                      └───┘                                                         Low
		 *
		 *                            ┌────────────────────────────────────────────┐          Vpp
		 *                            │                                            │
		 *                            │                                            │
		 *     VRst-HV   ─────────────┘                                            └───────── 0 or Vdd
		 *                            ↦↤ ≥ 90nS                                   ↦↤ ≥ 90nS
		 *                             ┌──────────────────────────────────────────┐           Vdd
		 *     VTarget   ──────────────┘ ←────── MANUAL_TRIGGER_LENGTH_MS ──────→ └────────── 0
		 *
		 * If the 'Vdd_Trg-Always-On' jumper is connected, then VTarget is always on.
		 */
		else if( gget_nTRIGGER() ) {
			printf_P( PSTR(">>> Manual trigger - begin (ADC_Ref = %d) (Trigger_MS = %d)\n"), boostVoltage, MANUAL_TRIGGER_LENGTH_MS );
			enaVppVdd();
			delayMS(MANUAL_TRIGGER_LENGTH_MS);
			disVppVdd();
			printf_P( PSTR(">>> Manual trigger - end\n\n") );
		}

		// Update the boosted voltage
		else {
			boostVoltage = gget_nBoostStop() ? 0 : boostGetDIPVoltage();
		}

		// Reset watchdog
		wdt_reset();

	} // for

#endif

	return 0;
}
