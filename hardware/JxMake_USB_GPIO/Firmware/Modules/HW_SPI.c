/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdbool.h>
#include <stdint.h>

#include <avr/io.h>

#include "../Protocol/Protocol.h"
#include "../Utils.h"
#include "HW_SPI.h"


// State variables
static bool     _hwspiEnabled  = false;
static bool     _hwspiInvertSS = false;
static uint8_t  _hwspiSPCR     = 0;

static uint16_t _swspiDelay    = 0;
static bool     _swspiCPOL     = false;
static bool     _swspiCPHA     = false;

static bool     _xwspiBreak    = false;


////////////////////////////////////////////////////////////////////////////////////////////////////


static void _hwspi_setClkDiv_andEnable(uint8_t clkDiv)
{
	// !!! NOTE : Always synchronize with the protocol manual and Java driver implementation !!!

	// Use hardware-assisted bit-banging SPI
	if(clkDiv >= 8) {

		// Disable hardware SPI as needed
		if(!_swspiDelay) SPCR &= ~_BV(SPE);

		// WARNING : These values must be synchronized with how the '_swspi_*()' functions are implemented!
		switch(clkDiv) {
			// Standard values
			/*
			 * clkDiv   Frequency
			 *  8       32kHz ± 2%
			 *  9       16kHz ± 2%
			 * 10        8kHz ± 1%
			 * 11        4kHz ± 1%
			 * 12        2kHz ± 1%
			 * 13        1kHz ± 1%
			 * 14       500Hz ± 1%
			 * 15       250Hz ± 1%
			 */
			case  8 : _swspiDelay =   16U; break;
			case  9 : _swspiDelay =   33U; break;
			case 10 : _swspiDelay =   69U; break;
			case 11 : _swspiDelay =  141U; break;
			case 12 : _swspiDelay =  284U; break;
			case 13 : _swspiDelay =  569U; break;
			case 14 : _swspiDelay = 1141U; break;
			case 15 : _swspiDelay = 2280U; break;
			// Special/custom values
			/*
			 * clkDiv   Frequency
			 * 101      160kHz ± 5%
			 * 102      125kHz ± 5%
			 * 103      100kHz ± 5%
			 * 104      90kHz  ± 3%
			 * 105      80kHz  ± 3%
			 * 106      70kHz  ± 3%
			 * 107      60kHz  ± 3%
			 * 108      55kHz  ± 3%
			 * 109      50kHz  ± 3%
			 * 110      45kHz  ± 3%
			 * 111      40kHz  ± 3%
			 */
			default : {
				     if(clkDiv <= 101) _swspiDelay =  1;
				else if(clkDiv >= 111) _swspiDelay = 11;
				else                   _swspiDelay = clkDiv - 100;
				break;
			}
		} // switch

	}

	// Use hardware SPI
	else {

		/*
		 * clkDiv    Frequency
		 * 0 0b000     8MHz
		 * 1 0b001     8MHz
		 * 2 0b010     4MHz
		 * 3 0b011     2MHz
		 * 4 0b100     1MHz
		 * 5 0b101   500kHz
		 * 6 0b110   250kHz
		 * 7 0b111   125kHz
	     *
		 * SPR1   SPR0   ~SPI2X   Frequency              |   clkDiv
		 * 0      0       0       F_CPU /   2 (  8MHz)   |   0 0b000   = clkDiv
		 * 0      0       1       F_CPU /   4 (  4MHz)   |   1 0b001   = clkDiv - 1
		 * 0      1       0       F_CPU /   8 (  2MHz)   |   2 0b010   = clkDiv - 1
		 * 0      1       1       F_CPU /  16 (  1MHz)   |   3 0b011   = clkDiv - 1
		 * 1      0       0       F_CPU /  32 (500kHz)   |   4 0b100   = clkDiv - 1
		 * 1      0       1       F_CPU /  64 (250kHz)   |   5 0b101   = clkDiv - 1
		 * 1      1       0       F_CPU /  64 (250kHz)   |
		 * 1      1       1       F_CPU / 128 (125kHz)   |   7 0b111   = clkDiv
		 */
		if(clkDiv && clkDiv <= 6) --clkDiv;

		clkDiv ^= 0x01; // Invert the SPI2X bit

		// Set clock rate via 'clkDiv'
		SPCR = _hwspiSPCR | ( (clkDiv >> 1) & 0x03 );
		SPSR =              (  clkDiv       & 0x01 );

		// Clear the software SPI delay (disable software SPI)
		_swspiDelay = 0;

	}
}


bool hwspi_begin(uint8_t spiMode, bool ssMode, uint8_t clkDiv)
{
	// Simply exit if the SPI is already enabled
	if( hwspi_isEnabled() ) return true;

	// Save the SS mode
	_hwspiInvertSS = !!ssMode;

	// Save the hardware SPI parameter - set MSB first, master mode, and CPOL and CPHA via 'spiMode'
	_hwspiSPCR = _BV(SPE) | _BV(MSTR) | (spiMode << 2);

	// Save the software SPI parameter - set CPOL and CPHA via 'spiMode'
	_swspiCPOL = spiMode & 0x02;
	_swspiCPHA = spiMode & 0x01;

	// Ensure the slave is deselected
	if(_hwspiInvertSS) HW_SPI_PORT &= ~_BV(HW_SPI_SS_BIT);
	else               HW_SPI_PORT |=  _BV(HW_SPI_SS_BIT);

	// Ensure MOSI and SCK start in their inactive state
	               HW_SPI_PORT |=  _BV(HW_SPI_MOSI_BIT);
	if(_swspiCPOL) HW_SPI_PORT |=  _BV(HW_SPI_SCK_BIT );
	else           HW_SPI_PORT &= ~_BV(HW_SPI_SCK_BIT );

	// Set GPIO directions to input or output as needed
	HW_SPI_DDR &= ~_BV(HW_SPI_MISO_BIT);
	HW_SPI_DDR |=  _BV(HW_SPI_MOSI_BIT) | _BV(HW_SPI_SCK_BIT) | _BV(HW_SPI_SS_BIT);

	// Set the clock divider and enable SPI
	_hwspi_setClkDiv_andEnable(clkDiv);

	// Set flag
	_hwspiEnabled = true;

	// Done
	return true;
}


void hwspi_end(void)
{
	// Simply exit if the SPI is not enabled
	if( !hwspi_isEnabled() ) return;

	// Set all GPIO directions to input
	HW_SPI_DDR &= ~( _BV(HW_SPI_MISO_BIT) | _BV(HW_SPI_MOSI_BIT) | _BV(HW_SPI_SCK_BIT) | _BV(HW_SPI_SS_BIT) );

	// Disable SPI
	if(!_swspiDelay) SPCR &= ~_BV(SPE);

	// Clear flags
	_hwspiEnabled = false;
	_xwspiBreak   = false;
}


bool hwspi_setClkDiv(uint8_t clkDiv)
{
	// Error if the SPI is not enabled
	if( !hwspi_isEnabled() ) return false;

	// Error if it is currently in break state
	if(_xwspiBreak) return false;

	// Set the clock divider and enable SPI
	_hwspi_setClkDiv_andEnable(clkDiv);

	// Done
	return true;
}


bool hwspi_setSPIMode(uint8_t spiMode)
{
	// Error if the SPI is not enabled
	if( !hwspi_isEnabled() ) return false;

	// Save the hardware SPI parameter - set CPOL and CPHA via 'spiMode'
	_hwspiSPCR = (_hwspiSPCR & 0b11110011) | (spiMode << 2);

	// Save the software SPI parameter - set CPOL and CPHA via 'spiMode'
	_swspiCPOL = spiMode & 0x02;
	_swspiCPHA = spiMode & 0x01;

	// Realize the hardware SPI parameter
	if(!_swspiDelay) {
		// Save the clock rate select
		const int spr10 = SPCR & 0x03;
		// Disble SPI
		SPCR &= ~_BV(SPE);
		// Enable SPI with the new mode and original clock rate select
		SPCR  = _hwspiSPCR | spr10;
	}

	// Done
	return true;
}


bool hwspi_isEnabled(void)
{ return _hwspiEnabled; }


bool hwspi_selectSlave(void)
{
	// Error if the SPI is not enabled
	if( !hwspi_isEnabled() ) return false;

	// Select slave
	if(_hwspiInvertSS) HW_SPI_PORT |=  _BV(HW_SPI_SS_BIT);
	else               HW_SPI_PORT &= ~_BV(HW_SPI_SS_BIT);

	// Done
	return true;
}


bool hwspi_deselectSlave(void)
{
	// Error if the SPI is not enabled
	if( !hwspi_isEnabled() ) return false;

	// Deselect slave
	if(_hwspiInvertSS) HW_SPI_PORT &= ~_BV(HW_SPI_SS_BIT);
	else               HW_SPI_PORT |=  _BV(HW_SPI_SS_BIT);

	// Done
	return true;
}


// Save the GCC options and make sure this section of code is optimized for size; "-Os" enables all "-O2" optimizations except those that often increase code size
#pragma GCC push_options
#pragma GCC optimize("-Os")


static inline ATTR_ALWAYS_INLINE bool _swspi_miso(void)
{ return HW_SPI_PIN & _BV(HW_SPI_MISO_BIT) ? true : false; }

static inline ATTR_ALWAYS_INLINE void _swspi_mosi(bool value)
{
	if(value) HW_SPI_PORT |=  _BV(HW_SPI_MOSI_BIT);
	else      HW_SPI_PORT &= ~_BV(HW_SPI_MOSI_BIT);
}

static inline ATTR_ALWAYS_INLINE void _swspi_sclk(bool value)
{
	if(_swspiCPOL) value = !value;

	if(value) HW_SPI_PORT |=  _BV(HW_SPI_SCK_BIT);
	else      HW_SPI_PORT &= ~_BV(HW_SPI_SCK_BIT);

	for(uint16_t i = 0; i < _swspiDelay; ++i) __asm__ __volatile__ ( "nop \n\t nop \n\t" );
}

static inline ATTR_ALWAYS_INLINE void _swspi_transfer(uint8_t* buff, _SInt_IZArg_t size)
{
	// Loop through the values
	for(_SInt_IZArg_t i = 0; i < size; ++i) {

		// Loop through the bits
		for(int b = 0; b < 8; ++b) {

			// Get the MOSI bit value to be sent (MSB first)
			const bool mosiBitValue = (buff[i] & 0b10000000) != 0;

			// Shift the bit
			buff[i] = (buff[i] << 1) & 0xFF;

			// SPI mode 1 and 3
			if(_swspiCPHA) {
				_swspi_sclk(true);                 // Make the SCK active
				_swspi_mosi(mosiBitValue);         // Output the bit
				_swspi_sclk(false);                // Make the SCK inactive
				buff[i] = buff[i] | _swspi_miso(); // Read the response and store the bit that has been received (MSB first)
			}
			// SPI mode 0 and 2
			else {
				_swspi_mosi(mosiBitValue);         // Output the bit
				_swspi_sclk(true);                 // Make the SCK active
				buff[i] = buff[i] | _swspi_miso(); // Read the response and store the bit that has been received (MSB first)
				_swspi_sclk(false);                // Make the SCK inactive
			}

		} // for b

	} // for i
}


bool hwspi_transfer(uint8_t* buff, _SInt_IZArg_t size)
{
	// Error if the SPI is not enabled
	if( !hwspi_isEnabled() ) return false;

	// Error if it is currently in break state
	if(_xwspiBreak) return false;

	// Simply exit if the size is zero
	if(!size) return true;

	// Check we need to use hardware-assisted bit-banging SPI
	if(_swspiDelay) {
		_swspi_transfer(buff, size);
		return true;
	}

	// Transfer the byte(s)
	cli();

#if 0

	while(size--) {

		SPDR = *buff;
		while( !( SPSR & _BV(SPIF) ) );

		*buff++ = SPDR;

	} // while

#else

	// NOTE : This code block should have better performance than the code block above

	SPDR = *buff;

	while(--size) {

		const uint8_t outVal = *(buff + 1);

		while( !( SPSR & _BV(SPIF) ) );

		const uint8_t inpVal = SPDR  ;
		              SPDR   = outVal;

		*buff++ = inpVal;

	} // while

	while( !( SPSR & _BV(SPIF) ) );
	*buff = SPDR;

#endif

	sei();

	// Done
	return true;
}


bool hwspi_transfer_w16Nd_r16dN(
	uint8_t       wAfterDelayUs25_wSPIMode,
	_SInt_IZArg_t wSize,
	uint8_t       rInterDelayUs10_rSPIMode,
	_SInt_IZArg_t rSize,
	uint8_t       rDummyValue,
	uint8_t*      rwBuff
)
{
	if(wSize > 0) {
		if( !hwspi_setSPIMode(wAfterDelayUs25_wSPIMode & 0x03) ) return false;
		if( !hwspi_transfer(rwBuff, wSize) ) return false;
		for(uint8_t i = 0; i < (wAfterDelayUs25_wSPIMode >> 4); ++i) _delay_us(25);
	}

	if(rSize > 0) {
		if( !hwspi_setSPIMode(rInterDelayUs10_rSPIMode & 0x03) ) return false;
		for(_SInt_IZArg_t r = 0; r < rSize; r += 2) {
			rwBuff[r + 0] = rDummyValue;
			rwBuff[r + 1] = rDummyValue;
			if( !hwspi_transfer(rwBuff + r, 2) ) return false;
			for(int i = 0; i < (rInterDelayUs10_rSPIMode >> 4); ++i) _delay_us(10);
		}
	}

	return true;
}


bool hwspi_setBreak(uint8_t mosi, uint8_t sclk)
{
	// Simply exit if the SPI is not enabled
	if( !hwspi_isEnabled() ) return false;

	// Set MOSI and SCK logic levels
	if(mosi) HW_SPI_PORT |=  _BV(HW_SPI_MOSI_BIT);
	else     HW_SPI_PORT &= ~_BV(HW_SPI_MOSI_BIT);

	if(sclk) HW_SPI_PORT |=  _BV(HW_SPI_SCK_BIT );
	else     HW_SPI_PORT &= ~_BV(HW_SPI_SCK_BIT );

	// Exit here if already in break state
	if(_xwspiBreak) return true;

	// Disable hardware SPI as needed
	if(!_swspiDelay) SPCR &= ~_BV(SPE);

	// Set flag
	_xwspiBreak = true;

	// Done
	return true;
}


int8_t hwspi_setBreakExt(uint8_t mosi, uint8_t sclk)
{
	// Set break
	if( !hwspi_setBreak(mosi, sclk) ) return -1;

	// Read the MISO
	return _swspi_miso() ? 1 : 0;
}


bool hwspi_clrBreak(void)
{
	// Simply exit if the SPI is not enabled
	if( !hwspi_isEnabled() ) return false;

	// Simply exit if not in break state
	if(!_xwspiBreak) return false;

	// Restore MOSI and SCK to their inactive state
	               HW_SPI_PORT |=  _BV(HW_SPI_MOSI_BIT);

	if(_swspiCPOL) HW_SPI_PORT |=  _BV(HW_SPI_SCK_BIT );
	else           HW_SPI_PORT &= ~_BV(HW_SPI_SCK_BIT );

	// Re-enable hardware SPI as needed
	if(!_swspiDelay) SPCR |= _BV(SPE);

	// Clear flag
	_xwspiBreak = false;

	// Done
	return true;
}


bool hwspi_xb_transfer(uint8_t ioocc_eoocc, uint8_t vvvvvvvv, uint8_t* buff, _SInt_IZArg_t nPairs)
{
	// Simply exit if the SPI is not enabled
	if( !hwspi_isEnabled() ) return false;

	// Simply exit if not in break state
	if(!_xwspiBreak) return false;

	// Extract the configuration bits
	const uint8_t i_oo   = (ioocc_eoocc >> 6) & 0b00000011;
	const uint8_t i_cc   = (ioocc_eoocc >> 4) & 0b00000011;
	const uint8_t e_oo   = (ioocc_eoocc >> 2) & 0b00000011;
	const uint8_t e_cc   = (ioocc_eoocc >> 0) & 0b00000011;

	const bool    useECC = (e_cc & 0b10) != 0;

	// Set initial values and then delay as needed
	if(i_oo & 0b10) _swspi_mosi(i_oo & 0b01);
	if(i_cc & 0b10) _swspi_sclk(i_cc & 0b01); // ##### ??? TODO : Is it really OK to use '_swspi_sclk()' if '_swspiDelay' is zero ??? #####

	for(uint16_t i = 0; i < (uint16_t) vvvvvvvv; ++i) _delay_ms(1);

	// Loop through the values
	uint8_t* const eBuff = buff + (nPairs * 2);

	while(buff < eBuff) {

		// Get the number of bits and the data byte
		const uint8_t  nbit = (*buff++ & 0b00000111) + 1;
		      uint8_t* data =   buff++;

		// Preshift the bits as needed
		if(nbit < 8) *data = ( *data << (8 - nbit) ) & 0xFF;

		// Check if we will transmit the last SCK pulse
		const bool lastByte = useECC && (buff >= eBuff);

		// Loop through the bits
		for(int b = 0; b < nbit; ++b) {

			// Check if we will transmit the last SCK pulse
			const bool lastSCK = lastByte && (b == nbit - 1);

			// Get the MOSI bit value to be sent (MSB first)
			const bool mosiBitValue = (*data & 0b10000000) != 0;

			// Shift the bit
			*data = (*data << 1) & 0xFF;

			// ##### ??? TODO : Is it really OK to use '_swspi_sclk()' if '_swspiDelay' is zero ??? #####

			// SPI mode 1 and 3
			if(_swspiCPHA) {
					_swspi_sclk(true);             // Make the SCK active
					_swspi_mosi(mosiBitValue);     // Output the bit
				if(!lastSCK) {
					_swspi_sclk(false);            // Make the SCK inactive
				}
				else {
					_swspi_sclk(e_cc & 0b01);      // Make the SCK active/inactive as per the configuration bits
				}
					*data = *data | _swspi_miso(); // Read the response and store the bit that has been received (MSB first)
			}
			// SPI mode 0 and 2
			else {
					_swspi_mosi(mosiBitValue);     // Output the bit
					_swspi_sclk(true);             // Make the SCK active
					*data = *data | _swspi_miso(); // Read the response and store the bit that has been received (MSB first)
				if(!lastSCK) {
					_swspi_sclk(false);            // Make the SCK inactive
				}
				else {
					_swspi_sclk(e_cc & 0b01);      // Make the SCK active/inactive as per the configuration bits
				}
			}

		} // for

	} // while

	// Set end MOSI value
	if(e_oo & 0b10) _swspi_mosi(e_oo & 0b01);

	// Done
	return true;
}


int8_t hwspi_xb_special(uint8_t type)
{
	// Simply exit if the SPI is not enabled
	if( !hwspi_isEnabled() ) return -1;

	// Simply exit if not in break state
	if(!_xwspiBreak) return -1;

	// dsPIC30 HV ICSP entry sequence (nMCLR->Vpp pulse & 2 NOP instructions)
	if(type == HW_SPI_XBS_DSPIC30_HV_ESQ) {
		// Send the initial sequence
			HW_SPI_PORT |=  _BV(HW_SPI_SS_BIT  );                // SS   high (nMCLR->N/A)
			HW_SPI_PORT &= ~_BV(HW_SPI_MOSI_BIT);                // MOSI low
			HW_SPI_PORT &= ~_BV(HW_SPI_SCK_BIT ); _delay_ms(50); // SCK  low
			HW_SPI_PORT &= ~_BV(HW_SPI_SS_BIT  ); _delay_ms( 4); // SS   low  (nMCLR->Vpp)
			HW_SPI_PORT |=  _BV(HW_SPI_SS_BIT  ); _delay_us(10); // SS   high (nMCLR->N/A)
			HW_SPI_PORT &= ~_BV(HW_SPI_SS_BIT  ); _delay_ms( 4); // SS   low  (nMCLR->Vpp)
		// Send the two NOP instructions
		for(int i = 0; i < (4 + 6 * 4) * 2; ++i) {
			HW_SPI_PORT |=  _BV(HW_SPI_SCK_BIT ); _delay_us(10); // SCK  high
			HW_SPI_PORT &= ~_BV(HW_SPI_SCK_BIT ); _delay_us(10); // SCK  low
		}
		// Done
		return _hwspiInvertSS ? 0 : 1;
	}

	// Part of dsPIC33 LV (E)ICSP entry sequence (only the nMCLR pulse before the key)
	else if(type == HW_SPI_XBS_DSPIC33_LV_ESQ) {
		// Send the pulse (the maximum pulse width is 500uS)
		HW_SPI_PORT &= ~_BV(HW_SPI_SS_BIT); _delay_ms( 10); // SS low
		HW_SPI_PORT |=  _BV(HW_SPI_SS_BIT); _delay_us(350); // SS high
		HW_SPI_PORT &= ~_BV(HW_SPI_SS_BIT); _delay_ms( 10); // SS low
		// Done
		return _hwspiInvertSS ? 0 : 1;
	}

	/*
	// ##### EXPERIMENT #####
	else if(type == 100) {
		HW_SPI_DDR &= ~_BV(HW_SPI_MOSI_BIT);
		if( HW_SPI_PORT & _BV(HW_SPI_SS_BIT) ) return _hwspiInvertSS ? 1 : 0;
		else                                   return _hwspiInvertSS ? 0 : 1;
	}
	else if(type == 101) {
		HW_SPI_DDR |=  _BV(HW_SPI_MOSI_BIT);
		if( HW_SPI_PORT & _BV(HW_SPI_SS_BIT) ) return _hwspiInvertSS ? 1 : 0;
		else                                   return _hwspiInvertSS ? 0 : 1;
	}
	// ##### EXPERIMENT #####
	//*/

	// Invalid type
	return -1;
}
