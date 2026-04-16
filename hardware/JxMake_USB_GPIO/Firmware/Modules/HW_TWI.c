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


static inline int32_t ATTR_ALWAYS_INLINE _calc_twbr(uint8_t prescaler, int32_t sclFrequency)
{
	const int32_t fcpuDiv = ( ( (int32_t) (F_CPU) ) + (sclFrequency / 2) ) / sclFrequency;
	const int32_t fcpuSub = ( (fcpuDiv - 16) + 1 ) / 2;
	const int32_t precVal = prescaler;

	return ( fcpuSub + (precVal - 1) ) / precVal;
}


static inline uint32_t ATTR_ALWAYS_INLINE _calc_scl_freq(uint8_t prescaler, uint32_t twbr)
{
	const int32_t divFac = 16 + 2 * twbr * prescaler;

	return (F_CPU) / divFac;
}


////////////////////////////////////////////////////////////////////////////////////////////////////


// State variables
static bool    _hwtwiEnabled = false;
static uint8_t _timeoutMS    = 10;


////////////////////////////////////////////////////////////////////////////////////////////////////


bool hwtwi_begin(uint32_t sclFrequency, uint8_t timeoutMS, bool enableExternalPullUp)
{
	__DPRINTF_DECL_PREFIX("hwtwi_begin");

	// Simply exit if the TWI is already enabled
	if( hwtwi_isEnabled() ) return true;

	// Calculate the TWBR and TWPS values
	const uint8_t prescaler[4] = { 1, 4, 16, 64 };

	uint8_t twbr =   0;
	uint8_t twps = 255;

	for(uint8_t p = 0; p < sizeof(prescaler); ++ p) {
		int32_t twbrCalc = _calc_twbr( prescaler[p], sclFrequency );
		if(twbrCalc >= 10 && twbrCalc <= 255) {
			twbr = twbrCalc;
			twps = p;
			break;
		}
	}

	if(twbr == 0 || twps == 255) {
		const int32_t freq_l = _calc_scl_freq( prescaler[0                    ], 255 );
		      int32_t delta_l = freq_l - sclFrequency;
		              delta_l = (delta_l < 0) ? -delta_l : delta_l;
		const int32_t freq_h = _calc_scl_freq( prescaler[sizeof(prescaler) - 1],  10 );
		      int32_t delta_h = freq_h - sclFrequency;
		              delta_h = (delta_h < 0) ? -delta_h : delta_h;
		if(delta_l <= delta_h) { // Use maximum frequency
			twbr = 10;
			twps =  0;
			__DPRINTFS_W("requested SCL frequency (%d) too high; using supported maximum SCL frequency", sclFrequency);

		}
		else { // Use minimum frequency
			twbr = 255;
			twps = sizeof(prescaler) - 1;
			__DPRINTFS_W("requested SCL frequency (%d) too low; using supported minimum SCL frequency", sclFrequency);
		}
	}

	// Enable the pull-up resistors as requested
	if(enableExternalPullUp) {
		TWI_PULLUP_PORT |= _BV(TWI_PULLUP_SDA_BIT) | _BV(TWI_PULLUP_SCL_BIT);
		TWI_PULLUP_DDR  |= _BV(TWI_PULLUP_SDA_BIT) | _BV(TWI_PULLUP_SCL_BIT);
	}

	// Initialize TWI
	TWI_Init(
		  (twps == 0) ? TWI_BIT_PRESCALE_1
		: (twps == 1) ? TWI_BIT_PRESCALE_4
		: (twps == 2) ? TWI_BIT_PRESCALE_16
		:               TWI_BIT_PRESCALE_64,
		twbr
	);

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
	TWI_Disable();

	// Disable the pull-up resistors just in case they were enabled
	TWI_PULLUP_DDR  &= ~( _BV(TWI_PULLUP_SDA_BIT) | _BV(TWI_PULLUP_SCL_BIT) );
	TWI_PULLUP_PORT &= ~( _BV(TWI_PULLUP_SDA_BIT) | _BV(TWI_PULLUP_SCL_BIT) );

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

	const enum TWI_ErrorCodes_t res = TWI_StartTransmission( (slaveAddress << 1) | TWI_ADDRESS_WRITE, _timeoutMS );

	if(res != TWI_ERROR_NoError) return false;

	// Send the bytes
	for(_SInt_IZArg_t i = 0; i < len; ++i) TWI_SendByte(*data++);

	// Send stop as needed
	if(sendStop) TWI_StopTransmission();

	// Done
	return true;
}


bool hwtwi_read(uint8_t slaveAddress, uint8_t* data, _SInt_IZArg_t len, bool sendStop)
{
	// Simply exit if the TWI is not enabled
	if( !hwtwi_isEnabled() ) return false;

	// Begin TWI read
	if(len > TWI_TRANSCEIVER_BUFFER_SIZE) return false;

	const enum TWI_ErrorCodes_t res = TWI_StartTransmission( (slaveAddress << 1) | TWI_ADDRESS_READ, _timeoutMS );

	if(res != TWI_ERROR_NoError) return false;

	// Receive the bytes
	for(_SInt_IZArg_t i = 0; i < len; ++i) TWI_ReceiveByte( data + i, i == (len - 1) );

	// Send stop as needed
	if(sendStop) TWI_StopTransmission();

	// Done
	return true;
}
