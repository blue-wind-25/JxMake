/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdbool.h>
#include <stdint.h>

#include <avr/io.h>

#include "../Utils.h"
#include "HW_SPI.h"
#include "HW_USRT_Specific.h"
#include "JTAG.h"


// HW-SPI helper macros
#define HW_SPI_DIS() do {                         \
	if(_swspiDelay == 0) {                        \
		/* Ensure TCK is in its inactive state */ \
		_jtag_tck(false);                         \
		/* Disable HW-SPI */                      \
		HW_SPI_SPI.CTRL &= ~SPI_ENABLE_bm;        \
	}                                             \
} while(0)


#define HW_SPI_ENA() do {                                  \
	/* Enable HW-SPI as needed */                          \
	if(_swspiDelay == 0) HW_SPI_SPI.CTRL |= SPI_ENABLE_bm; \
} while(0)


// State variables
static bool     _jtagEnabled    = false;
static uint16_t _jtagSWSPIDelay = 0;


////////////////////////////////////////////////////////////////////////////////////////////////////


static inline ATTR_ALWAYS_INLINE void _jtag_nrst(bool value)
{
	if(value) IO_SET_VALUE_1(BB_JTAG_M_PORT, BB_JTAG_M_NRST_BIT );
	else      IO_SET_VALUE_0(BB_JTAG_M_PORT, BB_JTAG_M_NRST_BIT );
}


static inline ATTR_ALWAYS_INLINE void _jtag_ntrst(bool value)
{
	if(value) IO_SET_VALUE_1(BB_JTAG_S_PORT, BB_JTAG_S_NTRST_BIT);
	else      IO_SET_VALUE_0(BB_JTAG_S_PORT, BB_JTAG_S_NTRST_BIT);
}


static inline ATTR_ALWAYS_INLINE void _jtag_tck(bool value)
{
	if(value) IO_SET_VALUE_1(BB_JTAG_M_PORT, BB_JTAG_M_TCK_BIT);
	else      IO_SET_VALUE_0(BB_JTAG_M_PORT, BB_JTAG_M_TCK_BIT);

	for(uint16_t i = 0; i < _jtagSWSPIDelay; ++i) __asm__ __volatile__ ( "nop \n\t nop \n\t" );
}


static inline ATTR_ALWAYS_INLINE void _jtag_tms(bool value)
{
	if(value) IO_SET_VALUE_1(BB_JTAG_S_PORT, BB_JTAG_S_TMS_BIT);
	else      IO_SET_VALUE_0(BB_JTAG_S_PORT, BB_JTAG_S_TMS_BIT);
}


static inline ATTR_ALWAYS_INLINE void _jtag_tms_pulse_tck(bool value)
{
	_jtag_tms(value);
	_jtag_tck(true );
	_jtag_tck(false);
}


static inline ATTR_ALWAYS_INLINE void _jtag_tdi(bool value)
{
	if(value) IO_SET_VALUE_1(BB_JTAG_M_PORT, BB_JTAG_M_TDI_BIT);
	else      IO_SET_VALUE_0(BB_JTAG_M_PORT, BB_JTAG_M_TDI_BIT);
}


static inline ATTR_ALWAYS_INLINE uint8_t _jtag_tdo_7(void)
{ return IO_GET_VALUE_X(BB_JTAG_M_PORT, BB_JTAG_M_TDO_BIT) ? 0x80 : 0x00; }


static inline ATTR_ALWAYS_INLINE void _jtag_transfer_bits(const uint8_t nbit, uint8_t* data)
{
	for(int b = 0; b < nbit; ++b) {

		// Get the TDI bit value to be sent (LSB first)
		const bool tdiBitValue = (*data & 0b00000001) != 0;

		// Shift the bit
		*data = (*data >> 1) & 0xFF;

		// Transfer the bit
		_jtag_tdi(tdiBitValue);        // Output the TDI bit
		_jtag_tck(true);               // Make the TCK active
		*data = *data | _jtag_tdo_7(); // Read and store the TDO bit that has been received (LSB first)
		_jtag_tck(false);              // Make the TCK inactive

	} // for
}


static inline ATTR_ALWAYS_INLINE void _jtag_postshift_bits(const uint8_t nbit, uint8_t* data)
{ if(nbit < 8) *data = ( *data >> (8 - nbit) ) & 0xFF; }


////////////////////////////////////////////////////////////////////////////////////////////////////


static inline ATTR_ALWAYS_INLINE void _jtag_spi_afterConfig(void)
{
	// JTAG bits are sent LSB first
	HW_SPI_SPI.CTRL |= SPI_DORD_bm;

	// Copy the SW-SPI delay and ensure that it meets the specifications
	_jtagSWSPIDelay = _swspiDelay;

	if(_jtagSWSPIDelay < 2) _jtagSWSPIDelay = 2;
}


bool jtag_begin(uint8_t clkDiv)
{
	// Simply exit if the JTAG is already enabled
	if( jtag_isEnabled() ) return true;

	// Initalize SPI and USRT
	if( !hwspi_begin(0, false, clkDiv) ) return false;

	if( !hwusrt_begin() ) {
		hwspi_end();
		return false;
	}

	// Disable the USRT module because JTAG will use bit-banging on it
	HW_UXRT.CTRLA &= ~( USART_RXCINTLVL_gm/* | USART_TXCINTLVL_gm*/ ); // Disable the interrupt(s)
	HW_UXRT.CTRLB &= ~(USART_RXEN_bm | USART_TXEN_bm);                 // Disable the receiver and transmitter
	HW_UXRT.CTRLC &= ~USART_CMODE_SYNCHRONOUS_gc;                      // Disable the serial clock

	// Perform the after-SPI-configuration steps
	_jtag_spi_afterConfig();

	// Disable hardware SPI by default
	HW_SPI_DIS();

	// Set Txd back to output (because disabling the transmitter will automatically set the Txd pin direction to input)
	IO_SETMODE_OUT(HW_UXRT_PORT, HW_UXRT_TXD_BIT);

	// Ensure these signals are in their inactive state
	jtag_setReset(true, true, true);

	// Set flag
	_jtagEnabled = true;

	// Done
	return true;
}


void jtag_end(void)
{
	// Simply exit if the JTAG is not enabled
	if( !jtag_isEnabled() ) return;

	// Disable SPI and USRT
	hwspi_end();
	hwusrt_end();

	// Clear flag
	_jtagEnabled = false;
}


bool jtag_setClkDiv(uint8_t clkDiv)
{
	// Error if the JTAG is not enabled
	if( !jtag_isEnabled() ) return false;

	// Use the SPI function to set the clock divider
	if( !hwspi_setClkDiv(clkDiv) ) return false;

	// Perform the after-SPI-configuration steps
	_jtag_spi_afterConfig();

	// Disable hardware SPI by default
	HW_SPI_DIS();

	// Done
	return true;
}


bool jtag_isEnabled(void)
{ return _jtagEnabled; }


bool jtag_setReset(bool nRST, bool nTRST, bool TDI)
{
	// Error if the JTAG is not enabled
	if( !jtag_isEnabled() ) return false;

	// Set the values
	_jtag_nrst (nRST );
	_jtag_ntrst(nTRST);
	_jtag_tdi  (TDI  );

	// Done
	return true;
}


bool jtag_tms(bool nRST, bool nTRST, bool TDI, uint8_t bitCnt, uint8_t value)
{
	// Error if the JTAG is not enabled
	if( !jtag_isEnabled() ) return false;

	// Set the static values
	_jtag_nrst (nRST );
	_jtag_ntrst(nTRST);
	_jtag_tdi  (TDI  );

	// Get the number of bits and the data byte
	const uint8_t nbit = (bitCnt & 0b00000111) + 1;

	// Send the bits (LSB first)
	for(int b = 0; b < nbit; ++b) {
		_jtag_tms_pulse_tck( (value & 0b00000001) != 0 );
		value >>= 1;
	}

	// Done
	return true;
}


bool jtag_transfer(bool xUpdate, bool drShift, bool irShift, uint8_t bitCntLast, uint8_t* buff, _SInt_IZArg_t size)
{
	// Error if the JTAG is not enabled
	if( !jtag_isEnabled() ) return false;

	// These values are mutually exclusive when set
	if(drShift && irShift) return false;

	// Simply exit if the number of bytes is zero
	if(!size) return true;

	// Perform state transition as needed
	if(drShift || irShift) {

		            _jtag_tms_pulse_tck(true ); // IDLE      -> DRSELECT
		if(irShift) _jtag_tms_pulse_tck(true ); // DRSELECT  -> IRSELECT
					_jtag_tms_pulse_tck(false); // xRSELECT  -> xRCAPTURE
					_jtag_tms_pulse_tck(false); // xRCAPTURE -> xRSHIFT

	} // if

	// Adjust the number of last bits
	bitCntLast = ( (bitCntLast & 0b00000111) + 1 );

	// Determine the last address of the buffer
	uint8_t* lBuff = buff + size - 1;

	// If the 'xUpdate' flag is not set, transfer all the bytes; otherwise, transfer all except the last byte
	if(!xUpdate || buff < lBuff) {

		// Determine the number of bytes
		const _SInt_IZArg_t mCnt = lBuff - buff + (xUpdate ? 0 : 1);

		// Transfer the bytes (all the data are 8 bits wide; hence, use the SPI function)
		if(_swspiDelay) {
			// At this point, if '_swspiDelay' is not zero, it means hardware-assisted bit-banging SPI is used.
			// Call the LSB-first transfer function directly because the primary function 'hwspi_transfer()' below
			// only supports MSB-first transfer in bit-banging mode.
			swspi_transfer_lsb(buff, mCnt);
		}
		else {
			/*
			for(int i = 0; i < mCnt; ++i) {
				_jtag_transfer_bits(8, buff++);
			}
			//*/
			//*
			HW_SPI_ENA(); // Enable hardware SPI
			if( !hwspi_transfer(buff, mCnt) ) return false; // Transfer the bits (LSB first)
			HW_SPI_DIS(); // Disable hardware SPI
			//*/
		}

	} // if

	// If the 'xUpdate' flag is set, transfer the last byte with a state transition at the last bit
	if(xUpdate) {

		// Transfer the bits (LSB first)
		_jtag_transfer_bits(bitCntLast - 1, lBuff);

		// Perform state transition when transferring the last bit
		_jtag_tms          (true    ); // xRSHIFT  -> xREXIT1  (deferred)
		_jtag_transfer_bits(1, lBuff);
		_jtag_tms_pulse_tck(true    ); // xREXIT1  -> xRUPDATE
		_jtag_tms_pulse_tck(false   ); // xRUPDATE -> IDLE

		// Postshift the bits as needed
		_jtag_postshift_bits(bitCntLast, lBuff);

	} // if

	// Done
	return true;
}


bool jtag_xb_transfer(bool xUpdate, bool drShift, bool irShift, uint8_t* buff, _SInt_IZArg_t nPairs)
{
	// ##### !!! TODO : VERIFY !!! #####

	// ##### ??? TODO : Remove CMD_JTAG_XB_TRANSFER because it does not seem to be needed ??? #####

	// Error if the JTAG is not enabled
	if( !jtag_isEnabled() ) return false;

	// These values are mutually exclusive when set
	if(drShift && irShift) return false;

	// Simply exit if the number of pairs is zero
	if(!nPairs) return true;

	// Perform state transition as needed
	if(drShift || irShift) {

		            _jtag_tms_pulse_tck(true ); // IDLE      -> DRSELECT
		if(irShift) _jtag_tms_pulse_tck(true ); // DRSELECT  -> IRSELECT
					_jtag_tms_pulse_tck(false); // xRSELECT  -> xRCAPTURE
					_jtag_tms_pulse_tck(false); // xRCAPTURE -> xRSHIFT

	} // if

	/*
	 * Example nPairs = 3
	 *
	 * Byte#     0123456
	 * Content   NDNDND
	 * xBuff         l e
	 */

	// Loop through the values
	uint8_t* const eBuff = buff + (nPairs * 2);
	uint8_t* const lBuff = eBuff - 2;
	bool           res   = true;

	while(buff < eBuff) {

		// Get the number of bits and the data byte
		const uint8_t  nbit = (*buff++ & 0b00000111) + 1;
		      uint8_t* data =   buff++;

		// If the 'xUpdate' flag is not set or it is not the last byte, transfer the byte without a state transition
		if(!xUpdate || buff <= lBuff) {

			// If the data is 8 bits wide, use the SPI function
			if(nbit == 8) {
				if(_swspiDelay) {
					// At this point, if '_swspiDelay' is not zero, it means hardware-assisted bit-banging SPI is used.
					// Call the LSB-first transfer function directly because the primary function 'hwspi_transfer()' below
					// only supports MSB-first transfer in bit-banging mode.
					swspi_transfer_lsb(data, 1);
				}
				else {
					HW_SPI_ENA(); // Enable hardware SPI
					res = hwspi_transfer(data, 1); // // Transfer the bits (LSB first)
					HW_SPI_DIS(); // Disable hardware SPI
					if(!res) break;
				}
			}
			// Otherwise, use the special JTAG bit-banging macro
			else {
				// Transfer the bits (LSB first)
				_jtag_transfer_bits(nbit, data);
				// Postshift the bits as needed
				_jtag_postshift_bits(nbit, data);
			}

			// Continue
			continue;

		} // if

		// If the 'xUpdate' flag is set, transfer the last byte with a state transition at the last bit
		if(xUpdate && buff == eBuff) {

			// Transfer the bits (LSB first)
			_jtag_transfer_bits(nbit - 1, data);

			// Perform state transition when transferring the last bit
			_jtag_tms          (true   ); // xRSHIFT  -> xREXIT1  (deferred)
			_jtag_transfer_bits(1, data);
			_jtag_tms_pulse_tck(true   ); // xREXIT1  -> xRUPDATE
			_jtag_tms_pulse_tck(false  ); // xRUPDATE -> IDLE

			// Postshift the bits as needed
			_jtag_postshift_bits(nbit, data);

		} // if

	} // while

	// Done
	return res;
}
