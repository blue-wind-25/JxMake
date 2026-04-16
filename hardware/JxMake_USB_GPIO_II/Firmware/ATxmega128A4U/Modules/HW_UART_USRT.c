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
static       bool               _hwuxrtTxEnabled        = false;
             uint8_t            _hwuxrtDisableTxAfter   = 0;

static       RingBuffer_t*      _hwuxrtRxBuff           = 0;

static const uint32_t           _hwusrtBaudrateMax      = (F_CPU) / 2UL;
static const uint32_t           _hwuartBaudrateMax      = (F_CPU) / 8UL;
static const uint32_t           _hwuxrtBaudrateDefault  = 115200UL;
static       uint32_t           _hwuxrtBaudrateCur      = _hwuxrtBaudrateDefault;

static       uint32_t           _hwuxrtDfrBaudrate      = _hwuxrtBaudrateDefault;
static       UXRT_ParityMode    _hwuxrtDfrParityMode    = UXRT_Parity_None;
static       UXRT_NumStopBit    _hwuxrtDfrNumStopBit    = UXRT_StopBit_1;
static       UXRT_CharacterSize _hwuxrtDfrCharacterSize = UXRT_CharSize_8;

////////////////////////////////////////////////////////////////////////////////////////////////////


// UART/USRT receive complete ISR
ISR(HW_UXRT_ISR_RXC, ISR_BLOCK)
{
	// Simply exit if the receive buffer is not set
	if(!_hwuxrtRxBuff) return;

	// Receive one byte and store it to the receive buffer
	const uint8_t recv = HW_UXRT.DATA;

	if( (USB_DeviceState == DEVICE_STATE_Configured) && !RingBuffer_IsFull(_hwuxrtRxBuff) ) RingBuffer_Insert(_hwuxrtRxBuff, recv);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


void hwuxrt_setReceiveBuffer(RingBuffer_t* rbuff)
{ _hwuxrtRxBuff = rbuff; }


bool hwuxrt_hasReceiveBuffer(void)
{ return !!_hwuxrtRxBuff; }


////////////////////////////////////////////////////////////////////////////////////////////////////


static void _hwuxrt_enable(bool initLLT)
{
	// Enable the interrupt(s)
	HW_UXRT.CTRLA |= USART_RXCINTLVL_MED_gc/* | USART_TXCINTLVL_MED_gc*/;

	// Enable the serial clock as needed
	if(_hwuxrtSyncMode) {
		// If this is an initialization request, configure both the GPIO and LLT
		if(initLLT) {
			// If the USRT is in PDI mode, send the PDI entry sequence during configuration
			if(_hwuxrtXMegaPDIMode) {
				IO_SET_PDNVAL0    (HW_UXRT_PORT, HW_UXRT_TXD_BIT                 ); // Set Txd to 0
				IO_SETMODE_OUT_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT); // Set Txd to output
				IO_SET_PUPVAL1    (HW_UXRT_PORT, HW_UXRT_XCK_BIT                 ); // Set Xck to 1
				IO_SETMODE_OUT_LLT(HW_UXRT_PORT, HW_UXRT_XCK_BIT, HW_UXRT_XCK_LLT); // Set Xck to output
				_delay_ms(110);                                                     // Delay for a while (reset)
				IO_SET_VALUE_1    (HW_UXRT_PORT, HW_UXRT_TXD_BIT                 ); // Set Txd to 1
				_delay_us(  1);                                                     // Delay for a while (disable reset)
				HW_UXRT.CTRLC |= USART_CMODE_SYNCHRONOUS_gc;                        // Enable the serial clock
				IO_INVERIO_ENA    (HW_UXRT_PORT, HW_UXRT_XCK_BIT                 ); // Txd will be changed at the falling edge of Xck and Rxd will be sampled at the rising edge of Xck
				_delay_ms(  2);                                                     // Delay for a while (activate PDI)
			}
			// Otherwise, simply enable Xck and configure the LLT
			else {
				HW_UXRT.CTRLC |= USART_CMODE_SYNCHRONOUS_gc;                        // Enable the serial clock
				IO_INVERIO_ENA    (HW_UXRT_PORT, HW_UXRT_XCK_BIT                 ); // Txd will be changed at the falling edge of Xck and Rxd will be sampled at the rising edge of Xck
				IO_SETMODE_OUT_LLT(HW_UXRT_PORT, HW_UXRT_XCK_BIT, HW_UXRT_XCK_LLT); // Set Xck to output
			}
		}
		// Otherwise, configure the GPIO only
		else {
			// If the USRT is in PDI mode, the Xck will always be active; hence, skip this step
			if(!_hwuxrtXMegaPDIMode) {
				HW_UXRT.CTRLC |= USART_CMODE_SYNCHRONOUS_gc;                        // Enable the serial clock
				IO_INVERIO_ENA    (HW_UXRT_PORT, HW_UXRT_XCK_BIT                 ); // Txd will be changed at the falling edge of Xck and Rxd will be sampled at the rising edge of Xck
				IO_SETMODE_OUT    (HW_UXRT_PORT, HW_UXRT_XCK_BIT                 ); // Set Xck to output
			}
		}
	}

	// If this is an initialization request, configure both the GPIO and LLT
	if(initLLT) {
		// If the USRT is in PDI mode, enable only Rxd
		if(_hwuxrtXMegaPDIMode) {
			HW_UXRT.CTRLB |=  USART_RXEN_bm;                                    // Enable the receiver
			IO_MODINP_NPXX_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT); // Set Txd to input
			IO_SETMODE_INP    (HW_UXRT_PORT, HW_UXRT_RXD_BIT                 ); // Set Rxd to input
		}
		// Otherwise, enable all
		else {
			HW_UXRT.CTRLB |= (USART_RXEN_bm | USART_TXEN_bm);                   // Enable the receiver and transmitter
			IO_SETMODE_OUT_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT); // Set Txd to output
			IO_SETMODE_INP    (HW_UXRT_PORT, HW_UXRT_RXD_BIT                 ); // Set Rxd to input
		}
	}
	// Otherwise, configure the GPIO only
	else {
		// If the USRT is in PDI mode, disable Txd by default
		if(_hwuxrtXMegaPDIMode) {
			HW_UXRT.CTRLB |=  USART_RXEN_bm;                                    // Enable the receiver
			IO_SETMODE_INP    (HW_UXRT_PORT, HW_UXRT_TXD_BIT                 ); // Set Txd to input
			IO_SETMODE_INP    (HW_UXRT_PORT, HW_UXRT_RXD_BIT                 ); // Set Rxd to input
		}
		// Otherwise, enable all
		else {
			HW_UXRT.CTRLB |= (USART_RXEN_bm | USART_TXEN_bm);                   // Enable the receiver and transmitter
			IO_SETMODE_OUT    (HW_UXRT_PORT, HW_UXRT_TXD_BIT                 ); // Set Txd to output
			IO_SETMODE_INP    (HW_UXRT_PORT, HW_UXRT_RXD_BIT                 ); // Set Rxd to input
		}
	}

	// Set flags
	_hwuxrtEnabled     =  true;
	_hwuxrtInBreakMode =  false;
	_hwuxrtTxEnabled   = !_hwuxrtXMegaPDIMode;
}


static void _hwuxrt_disable(bool shutdown)
{
	// Disable the interrupt(s)
	HW_UXRT.CTRLA &= ~( USART_RXCINTLVL_gm/* | USART_TXCINTLVL_gm*/ );

	// Disable the receiver and transmitter
	HW_UXRT.CTRLB &= ~(USART_RXEN_bm | USART_TXEN_bm); // NOTE : Disabling the transmitter will automatically set the Txd pin direction to input

	// Disable the serial clock if this is a shutdown request or USRT is not in PDI mode
	if( _hwuxrtSyncMode && (shutdown || !_hwuxrtXMegaPDIMode) ) {
		IO_INVERIO_DIS(HW_UXRT_PORT, HW_UXRT_XCK_BIT);
		HW_UXRT.CTRLC &= ~USART_CMODE_SYNCHRONOUS_gc;
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

	// Calculate the BSEL value
	const uint16_t baudSetting = UXRT_BSEL_SY(baudrate);

	// If the result larger than 4095, then the requested baudrate is too low
	if(baudSetting > 4095UL) {
		__DPRINTFF_E(_reqBaudrateTooLow, baudrate);
		return false;
	}

	ATOMIC_BLOCK(ATOMIC_FORCEON) {
		HW_UXRT.CTRLB     &= ~USART_CLK2X_bm;
		HW_UXRT.BAUDCTRLB  = (baudSetting >> 8   );
		HW_UXRT.BAUDCTRLA  = (baudSetting &  0xFF);
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
	if(baudrate > _hwuartBaudrateMax) {
		__DPRINTFF_E(_reqBaudrateTooHigh, baudrate );
		return false;
	}

#ifndef USE_HEURISTIC_BAUDRATE_CALCULATION
	// Try using 2X mode first
	uint16_t baudSetting = UXRT_BSEL_2X(baudrate);

	// If the result is larger than 4095, fallback to 1X mode (because BSEL is a 12-bit register)
	const bool use2XMode = (baudSetting <= 4095UL);

	if(!use2XMode) baudSetting = UXRT_BSEL_1X(baudrate);

	// If the result is still larger than 4095, then the requested baudrate is too low
	if(baudSetting > 4095UL) {
		__DPRINTFF_E(_reqBaudrateTooLow, baudrate );
		return false;
	}

	// Generate the configuration data (CLK2X, BAUDCTRLB, and BAUDCTRLA)
	const XUARTConfig cfg = {
		.CLK2X     = use2XMode            ,
		.BAUDCTRLB = (baudSetting >> 8   ),
		.BAUDCTRLA = (baudSetting &  0xFF)
	};
#else
	// Calculate CLK2X, BAUDCTRLB, and BAUDCTRLA
	const XUARTConfig cfg = calc_BSCALE_BSEL(baudrate);

	// Check if the requested baudrate is too low
	if(!cfg.CLK2X && !cfg.BAUDCTRLB && !cfg.BAUDCTRLA) {
		__DPRINTFF_E(_reqBaudrateTooLow, baudrate );
		return false;
	}
#endif

	// Set the baudrate
	ATOMIC_BLOCK(ATOMIC_FORCEON) {
		if(cfg.CLK2X) HW_UXRT.CTRLB     |=  USART_CLK2X_bm;
		else          HW_UXRT.CTRLB     &= ~USART_CLK2X_bm;
		              HW_UXRT.BAUDCTRLB  = cfg.BAUDCTRLB;
		              HW_UXRT.BAUDCTRLA  = cfg.BAUDCTRLA;
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

	// If the USRT is not in PDI mode, hold Txd and Xck at their idle states while the UART/USRT is being reconfigured.
	if(!_hwuxrtXMegaPDIMode) {
		IO_SET_PUPVAL1(HW_UXRT_PORT, HW_UXRT_TXD_BIT); // Hold the Txd line high
		IO_SET_PDNVAL0(HW_UXRT_PORT, HW_UXRT_XCK_BIT); // Hold the Xck line low
	}

	// Disable the UART/USRT and interrupt
	_hwuxrt_disable(false);

	// Set the baudrate
	if( !_hwuxrt_setBaudrate_impl(_hwuxrtDfrBaudrate) ) {
		// In case of error, use the previous baudrate
		_hwuxrtDfrBaudrate = _hwuxrtBaudrateCur;
		_hwuxrt_setBaudrate_impl(_hwuxrtDfrBaudrate);
	}

	// Set the data format
	HW_UXRT.CTRLC = ( _hwuxrtSyncMode ? USART_CMODE_SYNCHRONOUS_gc : USART_CMODE_ASYNCHRONOUS_gc                    )
	              | ( (_hwuxrtDfrParityMode | _hwuxrtDfrNumStopBit | _hwuxrtDfrCharacterSize) & UXRT_DataFormatMask );

	// Enable the UART/USRT and interrupt
	_hwuxrt_enable(false);
}


static bool _hwxuart_begin(bool useSyncMode, bool useXMegaPDIMode)
{
	// Simply exit if the UART/USRT is already enabled
	if( hwuxrt_isEnabled() ) return true;

	// Error if the receive buffer is not set
	if( !hwuxrt_hasReceiveBuffer() ) return false;

	// Save the flags
	_hwuxrtSyncMode     = useSyncMode | useXMegaPDIMode;
	_hwuxrtXMegaPDIMode =               useXMegaPDIMode;

	// Clear the number of bytes
	_hwuxrtDisableTxAfter = 0;

	// Set the baudrate
	if( !_hwuxrt_setBaudrate_impl(_hwuxrtDfrBaudrate) ) return false;

	// Set the data format
	HW_UXRT.CTRLC = ( _hwuxrtSyncMode ? USART_CMODE_SYNCHRONOUS_gc : USART_CMODE_ASYNCHRONOUS_gc                    )
	              | ( (_hwuxrtDfrParityMode | _hwuxrtDfrNumStopBit | _hwuxrtDfrCharacterSize) & UXRT_DataFormatMask );

	// Enable the UART/USRT and interrupt
	_hwuxrt_enable(true);

	// Done
	return true;
}


static void _hwxuart_end(void)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	// If the USRT is in PDI mode, hold Txd and Xck at their idle states
	if(_hwuxrtXMegaPDIMode) {
		IO_SET_PUPVAL1(HW_UXRT_PORT, HW_UXRT_TXD_BIT); // Hold the Txd line high
		IO_SET_PDNVAL0(HW_UXRT_PORT, HW_UXRT_XCK_BIT); // Hold the Xck line low
	}

	// Disable the UART/USRT and interrupt
	_hwuxrt_disable(true);

	// If the USRT is in PDI mode, delay briefly to exit PDI mode
	if(_hwuxrtXMegaPDIMode) _delay_ms(100);

	// Release the Txd and Xck lines
	IO_MODINP_NPXX_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT);
	IO_MODINP_NPXX_LLT(HW_UXRT_PORT, HW_UXRT_XCK_BIT, HW_UXRT_XCK_LLT);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


#define HWUXRT_DISABLE_TXD_LLT() do {                                       \
		/* Set Txd pin as input */                                          \
		IO_MODINP_NPXX_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT); \
		/* Disable the transmitter */                                       \
		HW_UXRT.CTRLB  &= ~USART_TXEN_bm;                                   \
		HW_UXRT.STATUS |=  USART_TXCIF_bm;                                  \
		/* Clear flag */                                                    \
		_hwuxrtTxEnabled = false;                                           \
	} while(0)


#define HWUXRT_TXBUF_READY() ( (HW_UXRT.STATUS & USART_DREIF_bm) != 0 )
#define HWUXRT_TX_COMPLETE() ( (HW_UXRT.STATUS & USART_TXCIF_bm) != 0 )

#define HWUXRT_WAIT_TXBUF_READY() do { while( !HWUXRT_TXBUF_READY() );                                   } while(0)
#define HWUXRT_WAIT_TX_COMPLETE() do { while( !HWUXRT_TX_COMPLETE() ); HW_UXRT.STATUS |= USART_TXCIF_bm; } while(0)

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
	HW_UXRT.DATA = value;

	// Check if the Txd needs to be disabled
	if(_hwuxrtDisableTxAfter) {
		if(--_hwuxrtDisableTxAfter == 0) {
			HWUXRT_WAIT_TXBUF_READY();
			HWUXRT_WAIT_TX_COMPLETE();
			_delay_us(25);
			HWUXRT_DISABLE_TXD_LLT();
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

	// Set Txd pin to 0 to indicates break state
	IO_SET_PDNVAL0    (HW_UXRT_PORT, HW_UXRT_TXD_BIT                 ); // Set Txd to 0
	IO_SETMODE_OUT_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT); // Set Txd to output

	// Disable the transmitter
	HW_UXRT.CTRLB  &= ~USART_TXEN_bm;
	HW_UXRT.STATUS |=  USART_TXCIF_bm;

	// Set Txd to output again because disabling the transmitter will automatically set the Txd pin direction to input
	IO_SETMODE_OUT(HW_UXRT_PORT, HW_UXRT_TXD_BIT);

	// Set flag
	_hwuxrtInBreakMode = true;
}


void hwuxrt_clearBreak(void)
{
	// Simply exit if the UART/USRT is not enabled
	if( !hwuxrt_isEnabled() ) return;

	// Simply exit if not in break mode
	if(!_hwuxrtInBreakMode) return;

	// Set Txd pin to 1 to indicates idle state
	IO_SET_PUPVAL1    (HW_UXRT_PORT, HW_UXRT_TXD_BIT                 ); // Set Txd to 1
	IO_SETMODE_OUT_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT); // Set Txd to output

	// Release the Txd line if USRT is in PDI mode
	if( _hwuxrtXMegaPDIMode ) {
		_delay_ms(1);
		IO_MODINP_NPXX_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT);
		_hwuxrtTxEnabled = false;
	}
	// Otherwise, enable the transmitter
	else {
		HW_UXRT.CTRLB     |= USART_TXEN_bm;
		_hwuxrtTxEnabled   = true;
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

	// Set Txd pin to output 1 (idle)
	IO_SET_PUPVAL1    (HW_UXRT_PORT, HW_UXRT_TXD_BIT                 ); // Set Txd to 1
	IO_SETMODE_OUT_LLT(HW_UXRT_PORT, HW_UXRT_TXD_BIT, HW_UXRT_TXD_LLT); // Set Txd to output

	// Enable the transmitter
	HW_UXRT.CTRLB |= USART_TXEN_bm;

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
	HWUXRT_DISABLE_TXD_LLT();
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
