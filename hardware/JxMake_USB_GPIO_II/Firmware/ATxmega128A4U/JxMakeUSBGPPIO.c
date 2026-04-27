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
static volatile uint16_t _RST_STATUS ATTR_NO_INIT;

void __handleWDT(void) ATTR_INIT_SECTION(3);
void __handleWDT(void)
{
	// Save 'RST_STATUS'
	_RST_STATUS = RST.STATUS;
	 RST.STATUS = 0xFF;

	// Disable watchdog
	WDT_Reset();
	WDT_Disable();
}


// Configures the board hardware and chip peripherals
static void _setupHardware(void)
{
	__DPRINTF_DECL_PREFIX("_setupHardware");

	// Initialize the system clock (start the external oscillator with an F_XTAL crystal and start the PLL to increase it to F_CPU)
#if F_XTAL   <=  2000000UL
	XMEGACLK_StartExternalOscillator( EXOSC_FREQ_2MHZ_MAX, EXOSC_START_16KCLK);
#elif F_XTAL <=  9000000UL
	XMEGACLK_StartExternalOscillator( EXOSC_FREQ_9MHZ_MAX, EXOSC_START_16KCLK);
#elif F_XTAL <= 12000000UL
	XMEGACLK_StartExternalOscillator(EXOSC_FREQ_12MHZ_MAX, EXOSC_START_16KCLK);
#else
	XMEGACLK_StartExternalOscillator(EXOSC_FREQ_16MHZ_MAX, EXOSC_START_16KCLK);
#endif
	/*
	XMEGACLK_StartPLL(CLOCK_SRC_INT_RC2MHZ, 2000000UL, F_CPU);
	//*/
	XMEGACLK_StartPLL(CLOCK_SRC_XOSC, F_XTAL, F_CPU);
	XMEGACLK_SetCPUClockSource(CLOCK_SRC_PLL);

	// Initialize the USB clock (start the 32MHz internal RC oscillator and start the DFLL to increase it to F_USB using the USB SOF as a reference)
	XMEGACLK_StartInternalOscillator(CLOCK_SRC_INT_RC32MHZ);
	XMEGACLK_StartDFLL(CLOCK_SRC_INT_RC32MHZ, DFLL_REF_INT_USBSOF, F_USB);

	// Enable all interrupt levels
	PMIC.CTRL = PMIC_LOLVLEN_bm | PMIC_MEDLVLEN_bm | PMIC_HILVLEN_bm;

	// Enable DMA with channel prority 0 > 1 > 2 > 3
	DMA.CTRL = DMA_ENABLE_bm | DMA_DBUFMODE_DISABLED_gc | DMA_PRIMODE_CH0123_gc;

	// Initialize JxMake USB-GPIO Module peripherals
	SLED_SETUP();
	ALED_SETUP();
	LLT_SETUP ();

	init_millis();
	hwgpio_init();

	initPrimary();

	// Initialize the debug stream
	debug_initStream();

	// Delay a few seconds if watchdog is enabled because we might get here from watchdog reset (for better USB re-enumeration)
	if( wdtEnIsKeySet() ) {
		if( ( _RST_STATUS & (RST_SRF_bm | RST_PDIRF_bm | RST_BORF_bm | RST_EXTRF_bm | RST_PORF_bm) ) == 0 ) {
			const int m = 5;
				__DPRINTFS_X("\n\n\n");
				SLED_OFF(); ALED_OFF();
			for(uint8_t i = 1; i <= m; ++i) {
				__DPRINTFS_X(">>> [Watchdog Reset] - wait %d/%d", i, m);
				for(uint8_t b = 0; b < 5; ++b) { SLED_ON(); _delay_ms(75); SLED_OFF(); _delay_ms(75); }
				_delay_ms(250);
				for(uint8_t b = 0; b < 5; ++b) { ALED_ON(); _delay_ms(75); ALED_OFF(); _delay_ms(75); }
				_delay_ms(250);
			}
				__DPRINTFS_X(">>> [Watchdog Reset] - wait done\n\n\n");
		}
		wdtEnClrKey();
	}

#ifdef USE_USB_INITIALIZATION_DELAY

// The default initialization delay is 1000mS; the recommended value is between 500mS to 2500mS
#ifndef USB_INITIALIZATION_DELAY_MS
#define USB_INITIALIZATION_DELAY_MS 1000
#endif

	/*
	 * Delay for a while before initializing USB
	 * NOTE : Without this delay, if this device is plugged first into a USB hub along with another device
	 *        (such as an FTDI serial adapter) and then the hub is subsequently plugged into a PC USB port,
	 *        the devices on the hub might be difficult to detect properly (if they are even detected at
	 *        all); although, this may depend on the make and model of the USB hub.
	 */
	// Blink the LEDs
	for(uint8_t i = 0; i < (USB_INITIALIZATION_DELAY_MS / 100); ++i) {
		SLED_ON (); ALED_OFF(); _delay_ms(50);
		SLED_OFF(); ALED_ON (); _delay_ms(50);
	}

	SLED_OFF(); ALED_OFF();

#endif // USE_USB_INITIALIZATION_DELAY

	// Initialize USB
	USB_Init();
}


// Main program entry point
volatile bool     _usbConfigSuccess        = false;
volatile bool     _cdc1Connected           = false;
volatile bool     _cdc2Connected           = false;

volatile uint32_t _resetSystem_setMillis   = 0;
volatile uint32_t _resetToBL_bps1200Millis = 0;

int main(void)
{
	__DPRINTF_DECL_PREFIX("main");

	// Initialize hardware
	_setupHardware();

	GlobalInterruptEnable();

	blink_sled();

	__DPRINTFS_X("\n\n\n");
	__DPRINTFS_X("===== entering the main loop (RST.STATUS=0x%02X) =====\n\n\n", _RST_STATUS);

#ifdef USE_USB_INITIALIZATION_CHECK

// The default check timeout delay is 2500mS; the recommended value is between 1000mS to 25000mS
#ifndef USB_INITIALIZATION_CHECK_MS
#define USB_INITIALIZATION_CHECK_MS 2500
#endif

	// Wait until the USB configuration process is successful or reset the system
	for(;;) {

		// Wait until the USB configuration process is successful
		const uint32_t refMillis = millis();
		      uint32_t ledMillis = refMillis;
		      bool     ledState  = false;

		for(;;) {
			// Handle the USB tasks
			CDC_Device_USBTask(&VirtualSerial1_CDC_Interface);
			CDC_Device_USBTask(&VirtualSerial2_CDC_Interface);
			USB_USBTask();
			// Break if the USB configuration process is successful
			if(_usbConfigSuccess) break;
			// Check the time
			const uint32_t curMillis = millis();
			if(curMillis - refMillis > USB_INITIALIZATION_CHECK_MS) break;
			// Blink the LEDs faster
			if(curMillis - ledMillis < 25) continue;
			ledMillis = curMillis;
			ledState  = !ledState;
			if(ledState) { SLED_ON (); ALED_OFF(); }
			else         { SLED_OFF(); ALED_ON (); }
		}

		// Break if the USB configuration process is successful
		if(_usbConfigSuccess) break;

		// Disable USB
		USB_Disable();

		// Blink the LEDs slower
		for(uint8_t i = 0; i < 5; ++i) {
			SLED_ON (); ALED_OFF(); _delay_ms(50);
			SLED_OFF(); ALED_ON (); _delay_ms(50);
		}

		SLED_OFF(); ALED_OFF();

		// Reset system
		_PROTECTED_WRITE(RST.CTRL, RST_SWRST_bm);
		for(;;);

	} // for

#endif // USE_USB_INITIALIZATION_CHECK

	// Main loop
	for (;;) {

		// Handle the primary interface
		handlePrimary(&VirtualSerial1_CDC_Interface);

		// Handle the secondary interface
		handleSecondary(&VirtualSerial2_CDC_Interface);

		// Handle the USB tasks
		CDC_Device_USBTask(&VirtualSerial1_CDC_Interface);
		CDC_Device_USBTask(&VirtualSerial2_CDC_Interface);
		USB_USBTask();

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


// Event handler for the library 'USB Connection' event
void EVENT_USB_Device_Connect(void)
{ blink_sled(); }


// Event handler for the library 'USB Disconnection' event
void EVENT_USB_Device_Disconnect(void)
{ blink_sled(); }


// Event handler for the library 'USB Configuration Changed' event
void EVENT_USB_Device_ConfigurationChanged(void)
{
	__DPRINTF_DECL_PREFIX("EVENT_USB_Device_ConfigurationChanged");

	bool ConfigSuccess = true;

	ConfigSuccess &= CDC_Device_ConfigureEndpoints(&VirtualSerial1_CDC_Interface);
	ConfigSuccess &= CDC_Device_ConfigureEndpoints(&VirtualSerial2_CDC_Interface);

	if(ConfigSuccess) {
		_usbConfigSuccess = true;
		__DPRINTFS_X("USB configuration success");
	}

	blink_sled();
}


// Event handler for the library 'USB Control Request' reception event
void EVENT_USB_Device_ControlRequest(void)
{
	CDC_Device_ProcessControlRequest(&VirtualSerial1_CDC_Interface);
	CDC_Device_ProcessControlRequest(&VirtualSerial2_CDC_Interface);
}


// Event handler for the 'CDC Class' driver 'Break Send' event
void EVENT_CDC_Device_BreakSent(USB_ClassInfo_CDC_Device_t* const CDCInterfaceInfo, const uint8_t Duration)
{
	__DPRINTF_DECL_PREFIX("EVENT_CDC_Device_BreakSent");

	if( !hwuxrt_isEnabled() ) return;

	if(CDCInterfaceInfo == &VirtualSerial1_CDC_Interface) {
		__DPRINTFS_X("CDC-ACM 1 = %d", Duration);
	}

	else if(CDCInterfaceInfo == &VirtualSerial2_CDC_Interface) {
		__DPRINTFS_X("CDC-ACM 2 = %d", Duration);
		if(Duration) { hwuxrt_setBreak  (); __DPRINTFS_X("hwuxrt_setBreak  ()"); }
		else         { hwuxrt_clearBreak(); __DPRINTFS_X("hwuxrt_clearBreak()"); }
	}

	else {
		__DPRINTFS_X("CDC-ACM ???");
	}

	blink_sled();
}


// Event handler for the 'CDC Class' driver 'Line State Changed' event
void EVENT_CDC_Device_ControLineStateChanged(USB_ClassInfo_CDC_Device_t* const CDCInterfaceInfo)
{
	__DPRINTF_DECL_PREFIX("EVENT_CDC_Device_ControLineStateChanged");

	if(CDCInterfaceInfo == &VirtualSerial1_CDC_Interface) {
		// Get the state
		_cdc1Connected = (CDCInterfaceInfo->State.ControlLineStates.HostToDevice & CDC_CONTROL_LINE_OUT_DTR) != 0;
		// Print message
		if(_cdc1Connected) __DPRINTFS_X("CDC-ACM 1 connected"   );
		else               __DPRINTFS_X("CDC-ACM 1 disconnected");
	}

	else if(CDCInterfaceInfo == &VirtualSerial2_CDC_Interface) {
		// Get the state
		_cdc2Connected = (CDCInterfaceInfo->State.ControlLineStates.HostToDevice & CDC_CONTROL_LINE_OUT_DTR) != 0;
		// Print message
		if(_cdc2Connected) __DPRINTFS_X("CDC-ACM 2 connected"   );
		else               __DPRINTFS_X("CDC-ACM 2 disconnected");
	}

	else {
		__DPRINTFS_X("CDC-ACM ???");
	}

	blink_sled();
}


// Event handler for the 'CDC Class' driver 'Line Encoding Changed' event
void EVENT_CDC_Device_LineEncodingChanged(USB_ClassInfo_CDC_Device_t* const CDCInterfaceInfo)
{
	__DPRINTF_DECL_PREFIX("EVENT_CDC_Device_LineEncodingChanged");

	// CDC interface 1 is the primary interface that will be used for most of the operation
	if(CDCInterfaceInfo == &VirtualSerial1_CDC_Interface) {

		__DPRINTFS_X("CDC-ACM 1 : BaudRateBPS = %lu", CDCInterfaceInfo->State.LineEncoding.BaudRateBPS);
		if(CDCInterfaceInfo->State.LineEncoding.BaudRateBPS == 1200) _resetToBL_bps1200Millis = millis() + RESET_DELAY_MS;
		else                                                         _resetToBL_bps1200Millis = 0;

	}

	// CDC interface 2 is the secondary interface that will be used for serial passthrough only
	else if(CDCInterfaceInfo == &VirtualSerial2_CDC_Interface) {

		// Handle baudrate
		__DPRINTFS_X("CDC-ACM 2 : BaudRateBPS = %lu", CDCInterfaceInfo->State.LineEncoding.BaudRateBPS);
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

		// Apply the new configuration if the UART/USRT is enabled and JTAG is not enabled
		if( hwuxrt_isEnabled() && !jtag_isEnabled() ) {
			if( hwuxrt_isInSyncMode() ) hwusrt_updateFromDefferedConfiguration();
			else                        hwuart_updateFromDefferedConfiguration();
		}

	}

	else {
		__DPRINTFS_X("CDC-ACM ???");
	}

	blink_sled();
}
