/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Firmware version numbers
#define FIRMWARE_VERSION_M 1
#define FIRMWARE_VERSION_N 0
#define FIRMWARE_VERSION_R 0


// Include the system header files
#include "std_includes.h"

// Include the low-level header files
#include "attributes.h"
#include "imsg.h"
#include "tick.h"

#include "adc.h"
#include "buttons.h"
#include "int0.h"
#include "leds.h"
#include "uart0.h"
#include "uart1.h"
#include "watchdog.h"

// Include the high-level header files
#include "asw.h"
#include "boost.h"
#include "util.h"

#include "bbs_8e2.h"
#include "updi.h"
#include "vpp_vdd.h"


// The main program
int main(void)
{
	// Initialize UART0 for debugging output
	uart0DebugStreamInit();

	// Initialize tick
	tickInit();

	// Save the starting tick value
	const uint32_t begInitTick = tickGet();

	// Print the reset reason
	printIMsg( PSTR("Reset reason : [%s]"), u8ToBin8(__mcusr) );

	if( __mcusr & _BV(JTRF ) ) printIMsg( PSTR(" JTAG"     ) );
	if( __mcusr & _BV(WDRF ) ) printIMsg( PSTR(" Watchdog" ) );
	if( __mcusr & _BV(BORF ) ) printIMsg( PSTR(" Brown-out") );
	if( __mcusr & _BV(EXTRF) ) printIMsg( PSTR(" External" ) );
	if( __mcusr & _BV(PORF ) ) printIMsg( PSTR(" Power-on" ) );

	printIMsgln( PSTR(".") );

	// Interrupt must be enabled here, as the tick mechanism is interrupt-driven
	sei();

	// Initialize LEDs
	ledInit();

	// Initialize the analog switch module controller
	aswInit();

	// Initialize the boost converter module controller
	boostInit();

	// Initialize ADC
	adcInit();
	utilCheckAVcc();
	utilCheckVBoostIdle();

	// Check the output voltage of the boost converter module
	utilCheckVBoostRange();

	// Initalize buttons
	btnInit();

	// Initialize INT0
	int0Init();

	// Initialize UART1 for UPDI output
	uart1Init();

	// Initialize watchdog timer
	wdtInit();

	// Print the total initialization time in mS
	printIMsgln( PSTR("System initialized and ready in ~%u milliseconds.\n"), tickGet() - begInitTick );

	// The main loop
	uint32_t prvTick = 0;

	for(;;) {

		// Get the UPDI state
		const uint8_t us = updiGetState();

		// Monitor the UPDI line to check if the upstream programmer has completed the programming process
		if(us != UPDI_STATE_IDLE) {
			// WARNING : If the target device is not attached, spurious interrupts may prevent the
			//           UPDI passthrough system from becoming idle!
			if(us == UPDI_STATE_TIMEOUT) {
				updiEnd();
				printIMsgln( PSTR(">>> End\n") );
			}
			else {
				const uint32_t curTick = tickGet();
				if( prvTick != curTick && (curTick % 1000) == 0 ) {
					printIMsgln( PSTR("...") );
					prvTick = curTick;
				}
			}
		}

		/*
		 * Handles the Vpp/Vdd sequence triggered by the nRST signal or button press (user trigger).
		 *
		 * ━━━ High Voltage Reset ━━━
		 *
		 *     nRST/nTrg   ──────┐                                                ┌─────────── High
		 *                       └──────────────────────┈┈┈┈┈┈┈───────────────────┘            Low
		 *                         ┌────────────────────┈┈┈┈┈┈┈─────────────────────┐          Vpp
		 *                         │                                                │
		 *                         │                                                │
		 *     VRst-HV     ────────┘                                                └───────── 0 or Vdd
		 *                         ↦↤ ≥ 90nS                                       ↦↤ ≥ 90nS
		 *                          ┌───────────────────┈┈┈┈┈┈┈────────────────────┐           Vdd
		 *     VTarget     ─────────┘                                              └────────── 0
		 *
		 * ━━━ Low Voltage Reset ━━━
		 *
		 *     nRST/nTrg   ──────┐                                                ┌─────────── High
		 *                       └──────────────────────┈┈┈┈┈┈┈───────────────────┘            Low
		 *     VRst-LV     ────────┐                                                ┌───────── High
		 *                         └────────────────────┈┈┈┈┈┈┈─────────────────────┘          Low
		 *                         ↦↤ ≥ 90nS                                       ↦↤ ≥ 90nS
		 *                          ┌───────────────────┈┈┈┈┈┈┈────────────────────┐           Vdd
		 *     VTarget     ─────────┘                                              └────────── 0
		 *
		 * NOTE : If the 'Vdd_Trg-Always-On' jumper is connected, VTarget remains constantly on.
		 */
		else if( btnRead_nRST_nTrg() ) {
			vxxHandle_nRST();
		}

		// Process the UPDI begin triggered by the nTRIGGER buttons
		else if( btnRead_nUPDI_NV() ) {
			printIMsgln( PSTR(">>> UPDI : Manual trigger (NVMPROG)") );
			updiBegin(false);
		}
		else if( btnRead_nUPDI_CE() ) {
			printIMsgln( PSTR(">>> UPDI : Manual trigger (CHIP_ERASE)") );
			updiBegin(true );
		}

		// Process the UPDI begin triggered by the PGC and PGD signals
		else if( utilDetectPGCD() ) {
			printIMsgln( PSTR(">>> UPDI : PGC-PGD trigger") );
			updiBegin(_pgcdCmd == 0x45); // 0x45 is 'E'
		}

		// Process the UPDI begin triggered by the the upstream programmer
		else if( updiSense() ) {
			printIMsgln( PSTR(">>> UPDI : Upstream trigger") );
			updiBegin(false);
		}

		// Update the VBoost
		else {
			if( utilUpdateVBoost() ) {
				vxxUpdateBeginMessage_vxxHandle_nRST();
				updiUpdateBeginMessage();
			}
		}

		// Reset watchdog
		wdt_reset();

	} // for

	return 0;
}
