/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdbool.h>
#include <stdint.h>

#include <LUFA/Drivers/Peripheral/TWI.h>

#include "../Utils.h"
#include "HW_TWI.h"


// State variables
static bool    _hwtwiEnabled = false;
static uint8_t _timeoutMS    = 10;


////////////////////////////////////////////////////////////////////////////////////////////////////


bool hwtwi_begin(uint32_t sclFrequency, uint8_t timeoutMS, bool enableExternalPullUp)
{
	__DPRINTF_DECL_PREFIX("hwtwi_begin");

	(void) enableExternalPullUp;

	// Simply exit if the TWI is already enabled
	if( hwtwi_isEnabled() ) return true;

	// Initialize TWI
	int32_t baud = TWI_BAUD_FROM_FREQ(sclFrequency);

	if(baud <   0) { baud =   0; __DPRINTFS_W("requested SCL frequency (%d) too high; using supported maximum SCL frequency", sclFrequency); }
	if(baud > 255) { baud = 255; __DPRINTFS_W("requested SCL frequency (%d) too low; using supported minimum SCL frequency" , sclFrequency); }

	// ##### !!! TODO: XMEGA AU manual specifies additional frequency and duty cycle requirements !!! #####

	TWI_Init(&TWI_TWI, baud);

	// Save  and limit the timeout value
	_timeoutMS = timeoutMS;

	     if(_timeoutMS <  1) _timeoutMS =  1;
	else if(_timeoutMS > 15) _timeoutMS = 15;

	// Set flag
	_hwtwiEnabled = true;

	// Done
	return true;
}


void hwtwi_end(void)
{
	// Simply exit if the TWI is not enabled
	if( !hwtwi_isEnabled() ) return;

	// Disable TWI
	TWI_Disable(&TWI_TWI);

	// Clear flag
	_hwtwiEnabled = false;
}


bool hwtwi_isEnabled(void)
{ return _hwtwiEnabled; }


bool hwtwi_write(uint8_t slaveAddress, const uint8_t* data, _SInt_IZArg_t len, bool sendStop)
{
	// Simply exit if the TWI is not enabled
	if( !hwtwi_isEnabled() ) return false;

	// Begin TWI write
	if(len > TWI_TRANSCEIVER_BUFFER_SIZE) return false;

	const enum TWI_ErrorCodes_t res = TWI_StartTransmission( &TWI_TWI, (slaveAddress << 1) | TWI_ADDRESS_WRITE, _timeoutMS );

	if(res != TWI_ERROR_NoError) return false;

	// Send the bytes
	for(_SInt_IZArg_t i = 0; i < len; ++i) TWI_SendByte(&TWI_TWI, *data++);

	// Send stop as needed
	if(sendStop) TWI_StopTransmission(&TWI_TWI);

	// Done
	return true;
}


bool hwtwi_read(uint8_t slaveAddress, uint8_t* data, _SInt_IZArg_t len, bool sendStop)
{
	// Simply exit if the TWI is not enabled
	if( !hwtwi_isEnabled() ) return false;

	// Begin TWI read
	if(len > TWI_TRANSCEIVER_BUFFER_SIZE) return false;

	const enum TWI_ErrorCodes_t res = TWI_StartTransmission( &TWI_TWI, (slaveAddress << 1) | TWI_ADDRESS_READ, _timeoutMS );

	if(res != TWI_ERROR_NoError) return false;

	// Receive the bytes
	for(_SInt_IZArg_t i = 0; i < len; ++i) TWI_ReceiveByte( &TWI_TWI, data + i, i == (len - 1) );

	// Send stop as needed
	if(sendStop) TWI_StopTransmission(&TWI_TWI);

	// Done
	return true;
}


bool hwtwi_scan(uint8_t* result128)
{
	// Simply exit if the TWI is not enabled
	if( !hwtwi_isEnabled() ) return false;

	// Check address #0 to #127
	for(uint8_t slaveAddress = 0; slaveAddress <= 127; ++slaveAddress) {

		// Reset watchdog
		WDT_Reset();

		// Check if there is a slave device at the current address
		if( TWI_StartTransmission( &TWI_TWI, (slaveAddress << 1) | TWI_ADDRESS_READ, _timeoutMS ) == TWI_ERROR_NoError ) {
			TWI_StopTransmission(&TWI_TWI);
			result128[slaveAddress] = 1;
		}
		else {
			result128[slaveAddress] = 0;
		}

		// Keep USB alive
		USB_USBTask();

	} // for

	// Done
	return true;
}


bool hwtwi_write_one_cf(uint32_t sclFrequency, uint8_t slaveAddress, uint8_t data)
{
	__DPRINTF_DECL_PREFIX("hwtwi_write_one_cf");

	// Simply exit if the TWI is not enabled
	if( !hwtwi_isEnabled() ) return false;

	// Calculate the new TWI baudrate
	const int32_t newBaud = TWI_BAUD_FROM_FREQ(sclFrequency);

	if(newBaud <   0) { __DPRINTFS_E("requested SCL frequency (%d) too high", sclFrequency); return false; }
	if(newBaud > 255) { __DPRINTFS_E("requested SCL frequency (%d) too low" , sclFrequency); return false; }

	// ##### !!! TODO: XMEGA AU manual specifies additional frequency and duty cycle requirements !!! #####

	// Set the new baudrate
	const uint8_t orgBaud = TWI_TWI.MASTER.BAUD;

	TWI_TWI.MASTER.BAUD = newBaud;

	// Perform a single TWI write
	if( TWI_StartTransmission( &TWI_TWI, (slaveAddress << 1) | TWI_ADDRESS_WRITE, _timeoutMS ) == TWI_ERROR_NoError ) {
		TWI_SendByte(&TWI_TWI, data);
		TWI_StopTransmission(&TWI_TWI);
	}

	// Restore the original baudrate
	TWI_TWI.MASTER.BAUD = orgBaud;

	// Done
	return true;
}


bool hwtwi_read_one_cf(uint32_t sclFrequency, uint8_t slaveAddress, uint8_t* data)
{
	__DPRINTF_DECL_PREFIX("hwtwi_read_one_cf");

	// Simply exit if the TWI is not enabled
	if( !hwtwi_isEnabled() ) return false;

	// Calculate the new TWI baudrate
	const int32_t newBaud = TWI_BAUD_FROM_FREQ(sclFrequency);

	if(newBaud <   0) { __DPRINTFS_E("requested SCL frequency (%d) too high", sclFrequency); return false; }
	if(newBaud > 255) { __DPRINTFS_E("requested SCL frequency (%d) too low" , sclFrequency); return false; }

	// ##### !!! TODO: XMEGA AU manual specifies additional frequency and duty cycle requirements !!! #####

	// Set the new baudrate
	const uint8_t orgBaud = TWI_TWI.MASTER.BAUD;

	TWI_TWI.MASTER.BAUD = newBaud;

	// Perform a single TWI write
	if( TWI_StartTransmission( &TWI_TWI, (slaveAddress << 1) | TWI_ADDRESS_READ, _timeoutMS ) == TWI_ERROR_NoError ) {
		TWI_ReceiveByte(&TWI_TWI, data, true);
		TWI_StopTransmission(&TWI_TWI);
	}

	// Restore the original baudrate
	TWI_TWI.MASTER.BAUD = orgBaud;

	// Done
	return true;
}
