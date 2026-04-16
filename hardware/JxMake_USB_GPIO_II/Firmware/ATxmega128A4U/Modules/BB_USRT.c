/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdbool.h>
#include <stdint.h>

#include <avr/io.h>

#include "../Millis.h"
#include "../Utils.h"
#include "BB_USRT.h"
#include "SS_USRT_Support.h"


#define RX_WAIT_START_BIT_TIMEOUT_MS 1000


// State variables
static bool _bbusrtEnabled     = false;

static bool _bbusrtParityEven  = false;
static bool _bbusrtParityOdd   = false;

static bool _bbusrtTwoStopBits = false;


////////////////////////////////////////////////////////////////////////////////////////////////////


static volatile uint16_t _xckDelay = 1;


static inline void ATTR_ALWAYS_INLINE _xck_delay_us_impl(uint8_t count)
{
	__asm__ __volatile__ (
		"1:       \n\t"
		"   dec %0\n\t"
		"brne 1b  \n\t"
		: "=r"(count)
		:  "0"(count)
	);
}


static inline void ATTR_ALWAYS_INLINE _xck_delay_us(uint16_t count)
{
	while(count > 255) {
		_xck_delay_us_impl(255);
		count -= 255;
	}

	if(count) _xck_delay_us_impl(count);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


static inline ATTR_ALWAYS_INLINE void _txd(bool value)
{
	if(value) IO_SET_VALUE_1(BB_USRT_PORT, BB_USRT_TXD_BIT);
	else      IO_SET_VALUE_0(BB_USRT_PORT, BB_USRT_TXD_BIT);
}


static inline ATTR_ALWAYS_INLINE bool _rxd(void)
{ return IO_GET_VALUE_X(BB_USRT_PORT, BB_USRT_RXD_BIT); }


static inline ATTR_ALWAYS_INLINE void _xck(bool value)
{
	if(value) IO_SET_VALUE_1(BB_USRT_PORT, BB_USRT_XCK_BIT);
	else      IO_SET_VALUE_0(BB_USRT_PORT, BB_USRT_XCK_BIT);
}


static inline ATTR_ALWAYS_INLINE void _pulse_xck(void)
{
	_xck(false); _xck_delay_us(_xckDelay);
	_xck(true ); _xck_delay_us(_xckDelay);
}


static inline ATTR_ALWAYS_INLINE void _txd_and_pulse_xck(bool txdValue)
{
	_xck(false   );
	_txd(txdValue); _xck_delay_us(_xckDelay);
	_xck(true    ); _xck_delay_us(_xckDelay);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


bool bbusrt_begin(uint8_t parityMode, uint8_t numStopBits, bool ssMode, uint32_t baudrate)
{
	// Simply exit if the USRT is already enabled
	if( bbusrt_isEnabled() ) return true;

	// Initialize SS support first
	if( !hwuxrt_ss_begin(ssMode) ) return false;

	// Ensure the slave is deselected
	hwuxrt_ss_deselectSlave();

	// Set all signals to their inactive (idle) states
	IO_SET_PDNVAL0(BB_USRT_PORT, BB_USRT_TXD_BIT);
	IO_SET_PDNVAL0(BB_USRT_PORT, BB_USRT_XCK_BIT);

	// Set GPIO directions to input or output as needed
	IO_SETMODE_INP    (BB_USRT_PORT, BB_USRT_RXD_BIT                 );
	IO_SETMODE_OUT_LLT(BB_USRT_PORT, BB_USRT_TXD_BIT, BB_USRT_TXD_LLT);
	IO_SETMODE_OUT_LLT(BB_USRT_PORT, BB_USRT_XCK_BIT, BB_USRT_XCK_LLT);

	// Determine the delay factor based on the baudrate
	/*
	 * _xckDelay   ~Measured Frequency (Hz)
	 *     0         630000
	 *     1         610000
	 *     2         550000
	 *     4         450000
	 *     6         380000
	 *     8         330000
	 *    10         300000
	 *    25         160000
	 *    50          93000
	 *    75          65000
	 *   100          50000
	 *   250          21000
	 *   500          10900
	 *   750           6900
	 *  1000           5300
	 *  2500           2100
	 *  5000           1100
	 *  7500            700
	 * 10000            500
	 * 25000            200
	 */
	if(true) {
		// LUTs
		static const int16_t PROGMEM delay_lut  [] = {     0,     1,     2,     4,     6,     8,    10,    25,   50,   75,  100,  250,  500, 750, 1000, 2500, 5000, 7500, 10000, 25000 };
		static const int32_t PROGMEM freq_lut_10[] = { 63000, 61000, 55000, 45000, 38000, 33000, 30000, 16000, 9300, 6500, 5000, 2100, 1090, 690,  530,  210,  110,   70,    50,    20 };
		// Allowed baudrate range
		static const uint32_t min_freq =    200U;
		static const uint32_t max_freq = 630000U;
		// Ensure the baudrate is within range
		if(baudrate < min_freq) baudrate = min_freq;
		if(baudrate > max_freq) baudrate = max_freq;
		// Calculate baudrate / 10
		const int32_t baudrate10 = (baudrate + 5U) / 10U;
		// Walk through the LUTs' elements
		for(uint8_t i = 0; i < sizeof(delay_lut) / sizeof(delay_lut[0]) - 1; ++i) {
			// Get the low and high frequency limit
			const int32_t hfb = pgm_read_dword(freq_lut_10 + i    );
			const int32_t lfb = pgm_read_dword(freq_lut_10 + i + 1);
			// Check if the baudrate is inside the limit
			if(baudrate10 <= hfb && baudrate10 >= lfb) {
				// Get the low and high delay limit
				const int32_t hdv = pgm_read_word(delay_lut + i    );
				const int32_t ldv = pgm_read_word(delay_lut + i + 1);
				// Linear interpolation between two known points
				const int32_t rfb = hfb - lfb;
				_xckDelay = ( ldv * (hfb - baudrate10) + hdv * (baudrate10 - lfb) + (rfb / 2U) ) / rfb;
				break;
			}
		}
	}

	// Save the configuration data
	_bbusrtParityEven  = (parityMode  == BB_USRT_P_EVEN);
	_bbusrtParityOdd   = (parityMode  == BB_USRT_P_ODD );
	_bbusrtTwoStopBits = (numStopBits >= 2             );

	// Set flag
	_bbusrtEnabled = true;

	// Done
	return true;
}


void bbusrt_end(void)
{
	// Simply exit if the USRT is not enabled
	if( !bbusrt_isEnabled() ) return;

	// Uninitialize SS support first
	hwuxrt_ss_end();

	// Set all GPIO directions to input
	IO_SETMODE_INP    (BB_USRT_PORT, BB_USRT_RXD_BIT                 );
	IO_MODINP_NPXX_LLT(BB_USRT_PORT, BB_USRT_TXD_BIT, BB_USRT_TXD_LLT);
	IO_MODINP_NPXX_LLT(BB_USRT_PORT, BB_USRT_XCK_BIT, BB_USRT_XCK_LLT);

	// Clear flag
	_bbusrtEnabled = false;
}


bool bbusrt_isEnabled(void)
{ return _bbusrtEnabled; }


bool bbusrt_selectSlave(void)
{
	// Error if the USRT is not enabled
	if( !bbusrt_isEnabled() ) return false;

	// Call the SS support function
	return hwuxrt_ss_selectSlave();
}


bool bbusrt_deselectSlave(void)
{
	// Error if the USRT is not enabled
	if( !bbusrt_isEnabled() ) return false;

	// Call the SS support function
	return hwuxrt_ss_deselectSlave();
}


bool bbusrt_pulseXck(uint8_t count, bool txValue)
{
	// Error if the USRT is not enabled or not selected
	if( !bbusrt_isEnabled() || !hwuxrt_ss_isSlaveSelected() ) return false;

	// Set the Tx pin signal to the specified value
	_txd(txValue);

	// Pulse the clock
	for(uint8_t i = 0; i < count; ++i) _pulse_xck();

	// Done
	return true;
}


bool bbusrt_tx(const uint8_t* buff, _SInt_IZArg_t size)
{
	// Error if the USRT is not enabled or not selected
	if( !bbusrt_isEnabled() || !hwuxrt_ss_isSlaveSelected() ) return false;

	// Loop through the values
	for(_SInt_IZArg_t i = 0; i < size; ++i) {

		// Send the start bit
		_txd_and_pulse_xck(false);

		// Loop through the bits
		uint8_t value  = buff[i];
		bool    parity = false;

		for(uint8_t b = 0; b < 8; ++b) {

			// Get the bit value to be sent (LSB first)
			const bool db = (value & 0b00000001) != 0;

			value >>= 1;

			// Calculate the parity
			parity ^= db;

			// Send the data bit
			_txd_and_pulse_xck(db);

		} // for b

		// Send the parity bit as needed
		if(_bbusrtParityOdd) parity = !parity;

		if(_bbusrtParityEven || _bbusrtParityOdd) _txd_and_pulse_xck(parity);

		// Send the stop bit(s)
		                       _txd_and_pulse_xck(true);
		if(_bbusrtTwoStopBits) _txd_and_pulse_xck(true);

	} // for i

	// Done
	return true;
}


bool bbusrt_rx(uint8_t* buff, _SInt_IZArg_t size)
{
	__DPRINTF_DECL_PREFIX("bbusrt_rx");

	// Error if the USRT is not enabled or not selected
	if( !bbusrt_isEnabled() || !hwuxrt_ss_isSlaveSelected() ) return false;

	// Loop as many as the number of requested values
	for(_SInt_IZArg_t i = 0; i < size; ++i) {

		// Receive the start bit
		const uint32_t startMillis = millis();

		while(true) {
			_pulse_xck();
			if( !_rxd() ) break;
			if(  millis() -  startMillis > RX_WAIT_START_BIT_TIMEOUT_MS ) {
				__DPRINTFS_E("timeout while waiting for start bit");
				return false;
			}
		}

		// Loop through the bits
		uint8_t value  = 0;
		bool    parity = false;

		for(uint8_t b = 0; b < 8; ++b) {

			// Receive the data bit
			_pulse_xck();
			const bool db = _rxd();

			// Store the bit that has been received (LSB first)
			value >>= 1;

			if(db) value = value | 0b10000000;

			// Calculate the parity
			parity ^= db;

		} // for b

		buff[i] = value; // Store the value

		// Receive and check the parity bit as needed
		if(_bbusrtParityEven || _bbusrtParityOdd) {
			// Receive the parity bit
			_pulse_xck();
			const bool pb = _rxd();
			// Update and compare the parity bit
			if(_bbusrtParityOdd) parity = !parity;
			if(pb != parity) {
				__DPRINTFS_E("parity error");
				return false;
			}
		}

		// Receive the stop bit(s)
		if(true) {
			_pulse_xck();
			const bool ob = _rxd();
			if(ob != true) {
				__DPRINTFS_E("stop bit 1 error");
				return false;
			}
		}
		if(_bbusrtTwoStopBits) {
			_pulse_xck();
			const bool ob = _rxd();
			if(ob != true) {
				__DPRINTFS_E("stop bit 2 error");
				return false;
			}
		}

	} // for i

	// Done
	return true;
}
