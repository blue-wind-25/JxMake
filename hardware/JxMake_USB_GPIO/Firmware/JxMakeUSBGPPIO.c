/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
             LUFA Library
     Copyright (C) Dean Camera, 2021.

  dean [at] fourwalledcubicle [dot] com
           www.lufa-lib.org
*/

/*
  Copyright 2021  Dean Camera (dean [at] fourwalledcubicle [dot] com)

  Permission to use, copy, modify, distribute, and sell this
  software and its documentation for any purpose is hereby granted
  without fee, provided that the above copyright notice appear in
  all copies and that both that the copyright notice and this
  permission notice and warranty disclaimer appear in supporting
  documentation, and that the name of the author not be used in
  advertising or publicity pertaining to distribution of the
  software without specific, written prior permission.

  The author disclaims all warranties with regard to this
  software, including all implied warranties of merchantability
  and fitness.  In no event shall the author be liable for any
  special, indirect or consequential damages or any damages
  whatsoever resulting from loss of use, data or profits, whether
  in an action of contract, negligence or other tortious action,
  arising out of or in connection with the use or performance of
  this software.
*/


#include "JxMakeUSBGPPIO.h"


//  LUFA CDC Class driver interface configuration and state information - 1st (primary) CDC interface
USB_ClassInfo_CDC_Device_t VirtualSerial1_CDC_Interface =
	{
		.Config =
			{
				.ControlInterfaceNumber   = INTERFACE_ID_CDC1_CCI,
				.DataINEndpoint           =
					{
						.Address          = CDC1_TX_EPADDR,
						.Size             = CDC_TXRX_EPSIZE,
						.Banks            = 1,
					},
				.DataOUTEndpoint =
					{
						.Address          = CDC1_RX_EPADDR,
						.Size             = CDC_TXRX_EPSIZE,
						.Banks            = 1,
					},
				.NotificationEndpoint =
					{
						.Address          = CDC1_NOTIFICATION_EPADDR,
						.Size             = CDC_NOTIFICATION_EPSIZE,
						.Banks            = 1,
					},
			},
	};


//  LUFA CDC Class driver interface configuration and state information - 2nd (secondary) CDC interface
USB_ClassInfo_CDC_Device_t VirtualSerial2_CDC_Interface =
	{
		.Config =
			{
				.ControlInterfaceNumber   = INTERFACE_ID_CDC2_CCI,
				.DataINEndpoint           =
					{
						.Address          = CDC2_TX_EPADDR,
						.Size             = CDC_TXRX_EPSIZE,
						.Banks            = 1,
					},
				.DataOUTEndpoint =
					{
						.Address          = CDC2_RX_EPADDR,
						.Size             = CDC_TXRX_EPSIZE,
						.Banks            = 1,
					},
				.NotificationEndpoint =
					{
						.Address          = CDC2_NOTIFICATION_EPADDR,
						.Size             = CDC_NOTIFICATION_EPSIZE,
						.Banks            = 1,
					},

			},
	};


// Handle watchdog on reset
void __handleMCUSR(void) ATTR_INIT_SECTION(3);
void __handleMCUSR(void)
{
	// Disable watchdog
	MCUSR = 0;
	wdt_disable();

	// Delay a few seconds if watchdog is enabled because we might get here from watchdog reset (for better USB re-enumeration)
	if( wdtEnIsKeySet() ) Delay_MS(5000);

	// Clear the key
	wdtEnClrKey();
}


// Configures the board hardware and chip peripherals
static void _setupHardware(void)
{
	// Disable clock division
	clock_prescale_set(clock_div_1);

	// Hardware Initialization
	LEDs_Init();
	USB_Init();

	// Initialize JxMake USB-GPIO Module peripherals
	ALED_SETUP();

	init_millis();
	hwgpio_init();

	initPrimary();

	// Enable debug stream
	debug_initStream();
}


// Main program entry point
volatile uint32_t _resetSystem_setMillis   = 0;
volatile uint32_t _resetToBL_bps1200Millis = 0;

int main(void)
{
	__DPRINTF_DECL_PREFIX("main");

	// Initialize hardware
	_setupHardware();

	LEDs_SetAllLEDs(LEDMASK_USB_NOTREADY);

	GlobalInterruptEnable();

	Delay_MS(100);

	__DPRINTFS_X("entering the main loop");

	// Main loop
	for (;;) {

		// Handle the primary interface
		handlePrimary(&VirtualSerial1_CDC_Interface);

		// Handle the secondary interface
		handleSecondary(&VirtualSerial2_CDC_Interface);

		// Handle the CDC tasks
		CDC_Device_USBTask(&VirtualSerial1_CDC_Interface);
		CDC_Device_USBTask(&VirtualSerial2_CDC_Interface);

		// Handle reset system and reset to bootloader
		const uint32_t curMillis = millis();
		      bool     resetSys  = false;
		      bool     resetToBL = false;
		ATOMIC_BLOCK(ATOMIC_FORCEON) {
			if(_resetSystem_setMillis   && curMillis > _resetSystem_setMillis  ) resetSys  = true;
			if(_resetToBL_bps1200Millis && curMillis > _resetToBL_bps1200Millis) resetToBL = true;
		}
		if(resetSys) {
			__DPRINTFS_X("resetSystem()");
			resetSystem();
		}
		if(resetToBL) {
			__DPRINTFS_X("resetToBootloader()");
			resetToBootloader();
		}

	} // for

	return 0;
}


// Event handler for the library USB Connection event
void EVENT_USB_Device_Connect(void)
{ LEDs_SetAllLEDs(LEDMASK_USB_ENUMERATING); }


// Event handler for the library USB Disconnection event
void EVENT_USB_Device_Disconnect(void)
{ LEDs_SetAllLEDs(LEDMASK_USB_NOTREADY); }


// Event handler for the library USB Configuration Changed event
void EVENT_USB_Device_ConfigurationChanged(void)
{
	bool ConfigSuccess = true;

	ConfigSuccess &= CDC_Device_ConfigureEndpoints(&VirtualSerial1_CDC_Interface);
	ConfigSuccess &= CDC_Device_ConfigureEndpoints(&VirtualSerial2_CDC_Interface);

	LEDs_SetAllLEDs(ConfigSuccess ? LEDMASK_USB_READY : LEDMASK_USB_ERROR);
}


// Event handler for the library USB Control Request reception event
void EVENT_USB_Device_ControlRequest(void)
{
	CDC_Device_ProcessControlRequest(&VirtualSerial1_CDC_Interface);
	CDC_Device_ProcessControlRequest(&VirtualSerial2_CDC_Interface);
}


// Event handler for the CDC Class driver Break Send event
void EVENT_CDC_Device_BreakSent(USB_ClassInfo_CDC_Device_t* const CDCInterfaceInfo, const uint8_t Duration)
{
	if(CDCInterfaceInfo != &VirtualSerial2_CDC_Interface) return;

	if( !hwuxrt_isEnabled() ) return;

	if(Duration) hwuxrt_setBreak  ();
	else         hwuxrt_clearBreak();
}


// Event handler for the CDC Class driver Line State Changed event
void EVENT_CDC_Device_ControLineStateChanged(USB_ClassInfo_CDC_Device_t* const CDCInterfaceInfo)
{
	//const bool hostReady = (CDCInterfaceInfo->State.ControlLineStates.HostToDevice & CDC_CONTROL_LINE_OUT_DTR) != 0;
}


// Event handler for the CDC Class driver Line Encoding Changed event
void EVENT_CDC_Device_LineEncodingChanged(USB_ClassInfo_CDC_Device_t* const CDCInterfaceInfo)
{
	// CDC interface 1 is the primary interface that will be used for most of the operation
	if(CDCInterfaceInfo == &VirtualSerial1_CDC_Interface) {
		if(CDCInterfaceInfo->State.LineEncoding.BaudRateBPS == 1200) _resetToBL_bps1200Millis = millis() + RESET_DELAY_MS;
		else                                                         _resetToBL_bps1200Millis = 0;
	}

	// CDC interface 2 is the secondary interface that will be used for serial passthrough only
	else {
		// Handle baudrate
		hwuxrt_deferSetBaudrate(CDCInterfaceInfo->State.LineEncoding.BaudRateBPS);

		// Handle parity type
		switch(CDCInterfaceInfo->State.LineEncoding.ParityType) {
			case CDC_PARITY_Even : hwuxrt_deferSetParityMode(UXRT_Parity_Even);
			                       break;
			case CDC_PARITY_Odd  : hwuxrt_deferSetParityMode(UXRT_Parity_Odd );
			                       break;
			case CDC_PARITY_None : /* FALLTHROUGH */
			case CDC_PARITY_Mark : /* FALLTHROUGH */
			case CDC_PARITY_Space: hwuxrt_deferSetParityMode(UXRT_Parity_None);
			                       CDCInterfaceInfo->State.LineEncoding.ParityType = CDC_PARITY_None;
			                       break;
		} // case

		// Handle number of stop bits
		switch(CDCInterfaceInfo->State.LineEncoding.CharFormat) {
			case CDC_LINEENCODING_OneStopBit          : hwuxrt_deferSetNumStopBit(UXRT_StopBit_1);
			                                            break;
			case CDC_LINEENCODING_OneAndAHalfStopBits : /* FALLTHROUGH */
			case CDC_LINEENCODING_TwoStopBits         : hwuxrt_deferSetNumStopBit(UXRT_StopBit_2);
			                                            CDCInterfaceInfo->State.LineEncoding.CharFormat = CDC_LINEENCODING_TwoStopBits;
			                                            break;
		} // case

		// Handle number of data bits
		switch(CDCInterfaceInfo->State.LineEncoding.DataBits) {
			case 5 : hwuxrt_deferSetCharacterSize(UXRT_CharSize_5);
			         break;
			case 6 : hwuxrt_deferSetCharacterSize(UXRT_CharSize_6);
			         break;
			case 7 : hwuxrt_deferSetCharacterSize(UXRT_CharSize_7);
			         break;
			case 8 : /* FALLTHROUGH */
			case 9 : hwuxrt_deferSetCharacterSize(UXRT_CharSize_8);
			         CDCInterfaceInfo->State.LineEncoding.DataBits = 8;
			         break;
		} // case

		// Apply the new configuration if the UART/USRT is enabled
		if( hwuxrt_isEnabled() ) {
			if( hwuxrt_isInSyncMode() ) hwusrt_updateFromDefferedConfiguration();
			else                        hwuart_updateFromDefferedConfiguration();
		}
	}
}
