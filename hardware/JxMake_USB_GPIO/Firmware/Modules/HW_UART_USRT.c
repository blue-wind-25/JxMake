/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <avr/interrupt.h>
#include <avr/io.h>
#include <util/atomic.h>

#include <LUFA/Drivers/Misc/RingBuffer.h>
#include <LUFA/Drivers/USB/USB.h>

#include "../Utils.h"
#include "HW_UART_USRT.h"


// State variables
static       bool               _hwuxrtEnabled          = false;
static       bool               _hwuxrtSyncMode         = false;
static       bool               _hwuxrtInBreakMode      = false;
static       bool               _hwuxrtXMegaPDIMode     = false;
static       bool               _hwuxrtXMegaPDIInitDone = false;
static       bool               _hwuxrtTxEnabled        = false;
             uint8_t            _hwuxrtDisableTxAfter   = 0;

static       RingBuffer_t*      _hwuxrtRxBuff           = 0;

static const uint32_t           _hwusrtBaudrateMax      = (F_CPU) / 2UL;
static const uint32_t           _hwuaartBaudrateMax     = (F_CPU) / 8UL;
static const uint32_t           _hwuxrtBaudrateDefault  = 115200UL;
static       uint32_t           _hwuxrtBaudrateCur      = _hwuxrtBaudrateDefault;

static       uint32_t           _hwuxrtDfrBaudrate      = _hwuxrtBaudrateDefault;
static       UXRT_ParityMode    _hwuxrtDfrParityMode    = UXRT_Parity_None;
static       UXRT_NumStopBit    _hwuxrtDfrNumStopBit    = UXRT_StopBit_1;
static       UXRT_CharacterSize _hwuxrtDfrCharacterSize = UXRT_CharSize_8;

////////////////////////////////////////////////////////////////////////////////////////////////////


/*
// UART/USRT transmit complete ISR
ISR(USART1_TX_vect, ISR_BLOCK)
{
	// Disable the transmitter
	UCSR1B &= ~_BV(TXEN1);
}
*/


// UART/USRT receive complete ISR
ISR(USART1_RX_vect, ISR_BLOCK)
{
	// Simply exit if the receive buffer is not set
	if(!_hwuxrtRxBuff) return;

	// Receive one byte and store it to the receive buffer
	const uint8_t recv = UDR1;

	if( (USB_DeviceState == DEVICE_STATE_Configured) && !RingBuffer_IsFull(_hwuxrtRxBuff) ) RingBuffer_Insert(_hwuxrtRxBuff, recv);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


void hwuxrt_setReceiveBuffer(RingBuffer_t* rbuff)
{ _hwuxrtRxBuff = rbuff; }


bool hwuxrt_hasReceiveBuffer(void)
{ return !!_hwuxrtRxBuff; }


////////////////////////////////////////////////////////////////////////////////////////////////////


static void _hwuxrt_enable(void)
{
	// Enable the interrupt(s)
	UCSR1B |= /*_BV(TXCIE1) |*/ _BV(RXCIE1);

	// Enable the serial clock as needed
	if(_hwuxrtSyncMode) {
		if(_hwuxrtXMegaPDIMode && !_hwuxrtXMegaPDIInitDone) {
			HW_UXRT_PORT &= ~_BV(HW_UXRT_TXD_BIT);       // Set Txd to 0
			HW_UXRT_DDR  |=  _BV(HW_UXRT_TXD_BIT);       // Set Txd to output
			HW_UXRT_PORT |=  _BV(HW_UXRT_XCK_BIT);       // Set Xck to 1
			HW_UXRT_DDR  |=  _BV(HW_UXRT_XCK_BIT);       // Set Xck to output
			_delay_ms(110);                              // Delay for a while
			HW_UXRT_PORT |=  _BV(HW_UXRT_TXD_BIT);       // Set Txd to 1
			_delay_us(  1);                              // Delay for a while
		}
			UCSR1C       |=  _BV(UMSEL10) | _BV(UCPOL1); // Enable the serial clock
			HW_UXRT_DDR  |=  _BV(HW_UXRT_XCK_BIT);       // Set Xck to output
			_delay_ms(  2);                              // Delay for a while
	}

	// Enable the transmitter and receiver
	if(!_hwuxrtXMegaPDIMode) { UCSR1B |= _BV(TXEN1); }
	                           UCSR1B |= _BV(RXEN1);

	// Release the Txd line as needed
	if(_hwuxrtSyncMode) {
		if(_hwuxrtXMegaPDIMode && !_hwuxrtXMegaPDIInitDone) {
			HW_UXRT_DDR  &= ~_BV(HW_UXRT_TXD_BIT);       // Set Txd pin as input
			HW_UXRT_PORT &= ~_BV(HW_UXRT_TXD_BIT);       // Disable Txd pull-up
		}
	}

	// Set flag
	_hwuxrtEnabled          =  true;
	_hwuxrtInBreakMode      =  false;
	_hwuxrtXMegaPDIInitDone =  _hwuxrtXMegaPDIMode;
	_hwuxrtTxEnabled        = !_hwuxrtXMegaPDIMode;
}


static void _hwuxrt_disable(bool shutdown)
{
	// Disable the interrupt(s)
	UCSR1B &= ~( /*_BV(TXCIE1) |*/ _BV(RXCIE1) );

	// Disable the UART
	UCSR1B &= ~_BV(TXEN1);
	UCSR1B &= ~_BV(RXEN1);

	// Disable the clock as needed
	if( _hwuxrtSyncMode && (shutdown || !_hwuxrtXMegaPDIMode) ) {
		UCSR1C      &= ~( _BV(UMSEL10) | _BV(UCPOL1) );
		HW_UXRT_DDR &= ~_BV(HW_UXRT_XCK_BIT);
	}

	// Clear flag
	_hwuxrtEnabled = false;
}


bool hwuxrt_isEnabled(void)
{ return _hwuxrtEnabled; }


bool hwuxrt_isInSyncMode(void)
{ return _hwuxrtSyncMode; }


////////////////////////////////////////////////////////////////////////////////////////////////////


void hwuxrt_deferSetBaudrate(uint32_t br)
{ _hwuxrtDfrBaudrate = br; }


void hwuxrt_deferSetParityMode(UXRT_ParityMode pm)
{ _hwuxrtDfrParityMode = pm; }


void hwuxrt_deferSetNumStopBit(UXRT_NumStopBit nsb)
{ _hwuxrtDfrNumStopBit = nsb; }


void hwuxrt_deferSetCharacterSize(UXRT_CharacterSize cs)
{ _hwuxrtDfrCharacterSize = cs; }


////////////////////////////////////////////////////////////////////////////////////////////////////

__DPRINTF_DECL_FORMAT(_reqBaudrateTooHigh, "requested baudrate (%lu) too high; baudrate unchanged");
__DPRINTF_DECL_FORMAT(_reqBaudrateTooLow , "requested baudrate (%lu) too low; baudrate unchanged" );
__DPRINTF_DECL_FORMAT(_baudrateChangedTo , "baudrate changed to %lu"                              );


static bool _hwusrt_setBaudrate_impl(uint32_t baudrate)
{
	// !!! NOTE : Always synchronize with the protocol manual and Java driver implementation !!!

	__DPRINTF_DECL_PREFIX("_hwusrt_setBaudrate_impl");

	// Check the baudrate
	if(baudrate > _hwusrtBaudrateMax) {
		__DPRINTFF_E(_reqBaudrateTooHigh, baudrate);
		return false;
	}

	// Calculate the UBBR value
	const uint16_t baudSetting = (F_CPU + baudrate) / (2UL * baudrate) - 1UL;

	// If the result larger than 4095, then the requested baudrate is too low
	if(baudSetting > 4095UL) {
		__DPRINTFF_E(_reqBaudrateTooLow, baudrate);
		return false;
	}

	ATOMIC_BLOCK(ATOMIC_FORCEON) {
	    UBRR1 = baudSetting;
	}

	// Save the given baudrate as the current baudrate
	_hwuxrtBaudrateCur = baudrate;

	__DPRINTFF_N(_baudrateChangedTo, baudrate);

	// Done
	return true;
}


static bool _hwuart_setBaudrate_impl(uint32_t baudrate)
{
	// !!! NOTE : Always synchronize with the protocol manual and Java driver implementation !!!

	__DPRINTF_DECL_PREFIX("_hwuart_setBaudrate_impl");

	// Check the baudrate
	if(baudrate > _hwuaartBaudrateMax) {
		__DPRINTFF_E(_reqBaudrateTooHigh, baudrate );
		return false;
	}

	// Try using 2X mode first
	uint16_t baudSetting = (F_CPU + 4UL * baudrate) / (8UL * baudrate) - 1UL;

	// If the result is larger than 4095, fallback to 1X mode (because UBBR is a 12-bit register)
	const bool use2XMode = (baudSetting <= 4095UL);

	if(!use2XMode) baudSetting = (F_CPU + 8UL * baudrate) / (16UL * baudrate) - 1UL;

	// If the result is still larger than 4095, then the requested baudrate is too low
	if(baudSetting > 4095UL) {
		__DPRINTFF_E(_reqBaudrateTooLow, baudrate );
		return false;
	}

	// Set the baudrate
	if(use2XMode) UCSR1A |= _BV(U2X1);

	ATOMIC_BLOCK(ATOMIC_FORCEON) {
	    UBRR1 = baudSetting;
	}

	// Save the given baudrate as the current baudrate
	_hwuxrtBaudrateCur = baudrate;

	__DPRINTFF_N(_baudrateChangedTo, baudrate);

	// Done
	return true;
}


static bool _hwuxrt_setBaudrate_impl(uint32_t baudrate)
{
	if(_hwuxrtSyncMode) return _hwusrt_setBaudrate_impl(baudrate);
	else                return _hwuart_setBaudrate_impl(baudrate);
}


static void _hwxuart_updateFromDefferedConfiguration(void)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	// Keep the Txd line held high (idle) while the UART/USRT is reconfigured
	HW_UXRT_PORT |= _BV(HW_UXRT_TXD_BIT);
	HW_UXRT_DDR  |= _BV(HW_UXRT_TXD_BIT);

	// Disable the UART/USRT and interrupt
	_hwuxrt_disable(false);

	// Set the baudrate
	if( !_hwuxrt_setBaudrate_impl(_hwuxrtDfrBaudrate) ) {
		// In case of error, use the previous baudrate
		_hwuxrtDfrBaudrate = _hwuxrtBaudrateCur;
		_hwuxrt_setBaudrate_impl(_hwuxrtDfrBaudrate);
	}

	// Set the data format
	UCSR1C = (_hwuxrtDfrParityMode | _hwuxrtDfrNumStopBit | _hwuxrtDfrCharacterSize) & UXRT_DataFormatMask;

	// Enable the UART/USRT and interrupt
	_hwuxrt_enable();

	// Release the Txd line after the USRT has been reconfigured
	HW_UXRT_DDR  &= ~_BV(HW_UXRT_TXD_BIT);
	HW_UXRT_PORT &= ~_BV(HW_UXRT_TXD_BIT);
}


static bool _hwxuart_begin(bool useSyncMode, bool useXMegaPDIMode)
{
	// Simply exit if the UART/USRT is already enabled
	if( hwuxrt_isEnabled() ) return true;

	// Error if the receive buffer is not set
	if( !hwuxrt_hasReceiveBuffer() ) return false;

	// Save the flags
	_hwuxrtSyncMode         = useSyncMode | useXMegaPDIMode;
	_hwuxrtXMegaPDIMode     = useXMegaPDIMode;
	_hwuxrtXMegaPDIInitDone = false;

	// Clear the number of bytes
	_hwuxrtDisableTxAfter = 0;

	// Set the baudrate
	if( !_hwuxrt_setBaudrate_impl(_hwuxrtDfrBaudrate) ) return false;

	// Set the data format
	UCSR1C = (_hwuxrtDfrParityMode | _hwuxrtDfrNumStopBit | _hwuxrtDfrCharacterSize) & UXRT_DataFormatMask;

	// Enable the UART/USRT and interrupt
	_hwuxrt_enable();

	// Done
	return true;
}


static void _hwxuart_end(void)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	if(_hwuxrtXMegaPDIMode) {
		// Hold the Txd line and Xck line
		HW_UXRT_PORT |= ( _BV(HW_UXRT_TXD_BIT) | _BV(HW_UXRT_XCK_BIT) );
		HW_UXRT_DDR  |= ( _BV(HW_UXRT_TXD_BIT) | _BV(HW_UXRT_XCK_BIT) );
	}
	else {
		// Release the Txd line
		HW_UXRT_DDR  &= ~_BV(HW_UXRT_TXD_BIT);
		HW_UXRT_PORT &= ~_BV(HW_UXRT_TXD_BIT);
	}

	// Disable the UART/USRT and interrupt
	_hwuxrt_disable(true);

	if(_hwuxrtXMegaPDIMode) {
		// Delay for a while
		Delay_MS(100);
		// Release the Txd line and Xck line
		HW_UXRT_DDR  &= ~( _BV(HW_UXRT_TXD_BIT) | _BV(HW_UXRT_XCK_BIT) );
		HW_UXRT_PORT &= ~( _BV(HW_UXRT_TXD_BIT) | _BV(HW_UXRT_XCK_BIT) );
	}
}


////////////////////////////////////////////////////////////////////////////////////////////////////


#define HWUXRT_DISABLE_TXD() do {              \
		/* Disable the transmitter */          \
		UCSR1B &= ~_BV(TXEN1);                 \
		/* Set Txd pin as input */             \
		HW_UXRT_DDR  &= ~_BV(HW_UXRT_TXD_BIT); \
		HW_UXRT_PORT &= ~_BV(HW_UXRT_TXD_BIT); \
		/* Clear flag */                       \
		_hwuxrtTxEnabled = false;              \
	} while(0)

#define HWUXRT_TXBUF_READY() ( ( UCSR1A & _BV(UDRE1) ) != 0 )
#define HWUXRT_TX_COMPLETE() ( ( UCSR1A & _BV(TXC1 ) ) != 0 )

#define HWUXRT_WAIT_TXBUF_READY() do { while( !HWUXRT_TXBUF_READY() );                       } while(0)
#define HWUXRT_WAIT_TX_COMPLETE() do { while( !HWUXRT_TX_COMPLETE() ); UCSR1A |= _BV(TXC1 ); } while(0)

#define HWUXRT_WAIT_ANY_TX_DONE() do { \
	if( !HWUXRT_TXBUF_READY() ) {    \
		HWUXRT_WAIT_TXBUF_READY();   \
		HWUXRT_WAIT_TX_COMPLETE();   \
	}                                \
} while(0)


void hwuxrt_sendByte(uint8_t value)
{
	// Wait until the send buffer is ready
	HWUXRT_WAIT_TXBUF_READY();

	// Send the data
	UDR1 = value;

	// Check if the Txd needs to be disabled
	if(_hwuxrtDisableTxAfter) {
		if(--_hwuxrtDisableTxAfter == 0) {
			HWUXRT_WAIT_TXBUF_READY();
			HWUXRT_WAIT_TX_COMPLETE();
			_delay_us(25);
			HWUXRT_DISABLE_TXD();
		}
	}
}


void hwuxrt_setBreak(void)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	// Simply exit if already in break mode
	if(_hwuxrtInBreakMode) return;

	// Wait until any existing transmission is completed
	HWUXRT_WAIT_ANY_TX_DONE();

	// Set Txd pin to output low
	HW_UXRT_PORT &= ~_BV(HW_UXRT_TXD_BIT);
	HW_UXRT_DDR  |=  _BV(HW_UXRT_TXD_BIT);

	// Disable the transmitter
	UCSR1B &= ~_BV(TXEN1);
	UCSR1A |=  _BV(TXC1 );

	// Set flag
	_hwuxrtInBreakMode = true;
}


void hwuxrt_clearBreak(void)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	// Simply exit if not in break mode
	if(!_hwuxrtInBreakMode) return;

	// Set Txd pin to output high
	HW_UXRT_PORT |= _BV(HW_UXRT_TXD_BIT);
	HW_UXRT_DDR  |= _BV(HW_UXRT_TXD_BIT);

	// Release the Txd line if USRT is in PDI mode
	if( _hwuxrtXMegaPDIMode ) {
		_delay_ms(1);
		HW_UXRT_DDR      &= ~_BV(HW_UXRT_TXD_BIT);
		HW_UXRT_PORT     &= ~_BV(HW_UXRT_TXD_BIT);
		_hwuxrtTxEnabled  = false;
	}
	// Otherwise, enable the transmitter
	else {
		UCSR1B           |=  _BV(TXEN1);
		HW_UXRT_PORT     &= ~_BV(HW_UXRT_TXD_BIT);
		_hwuxrtTxEnabled  = true;
	}

	// Clear flag
	_hwuxrtInBreakMode = false;
}


////////////////////////////////////////////////////////////////////////////////////////////////////


void hwuxrt_enableTx(void)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	// Simply exit if the Txd is already enabled
	if(_hwuxrtTxEnabled) return;

	/*
	// Wait until the send buffer is ready (or, in this case, empty)
	HWUXRT_WAIT_TXBUF_READY();

	// Synchronize with the serial clock as needed
	if(_hwuxrtSyncMode) {
		while(    HW_UXRT_PIN & _BV(HW_UXRT_XCK_BIT)   );
		while( !( HW_UXRT_PIN & _BV(HW_UXRT_XCK_BIT) ) );
		while(    HW_UXRT_PIN & _BV(HW_UXRT_XCK_BIT)   );
	}
	//*/

	// Set Txd pin as output
	HW_UXRT_PORT |= _BV(HW_UXRT_TXD_BIT);
	HW_UXRT_DDR  |= _BV(HW_UXRT_TXD_BIT);

	// Enable the transmitter
	UCSR1B |= _BV(TXEN1);

	// Disable Txd pull-up
	HW_UXRT_PORT &= ~_BV(HW_UXRT_TXD_BIT);

	// Delay for a short while
	_delay_us(25);

	/*
	// Synchronize with the serial clock as needed
	if(_hwuxrtSyncMode) {
		while(    HW_UXRT_PIN & _BV(HW_UXRT_XCK_BIT)   );
		while( !( HW_UXRT_PIN & _BV(HW_UXRT_XCK_BIT) ) );
		while(    HW_UXRT_PIN & _BV(HW_UXRT_XCK_BIT)   );
	}
	//*/

	// Wait until the send buffer is ready
	HWUXRT_WAIT_TXBUF_READY();

	// Set flag
	_hwuxrtTxEnabled = true;

	// Clear the number of bytes
	_hwuxrtDisableTxAfter = 0;
}


void hwuxrt_disableTx(void)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	// Simply exit if the Txd is not enabled
	if(!_hwuxrtTxEnabled) return;

	// Wait until any existing transmission is completed
	HWUXRT_WAIT_ANY_TX_DONE();

	// Disable Txd
	HWUXRT_DISABLE_TXD();
}


void hwuxrt_disableTxAfter(uint8_t nb)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	// Simply exit if the Txd is not enabled
	if(!_hwuxrtTxEnabled) return;

	// Set the number of bytes
	_hwuxrtDisableTxAfter = nb;
}


////////////////////////////////////////////////////////////////////////////////////////////////////


// Include the specific source files
#include "HW_UART_Specific.c"
#include "HW_USRT_Specific.c"
