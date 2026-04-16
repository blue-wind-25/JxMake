/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>

#include <avr/wdt.h>

#include <LUFA/Drivers/USB/USB.h>

#include "../ActivityLED.h"
#include "Descriptors.h"
#include "Utils.h"

#if !!ENABLE_DEBUG_STREAM
	#if !!DEBUG_STREAM_USE_AVR_TINY_UART
		#include <util/atomic.h>
		#include "Modules/avr-tinyuart/tinyuart.c"
	#endif
#endif


static uint8_t _GenericRWBuff_ClaimerID  = 0;
static uint8_t _GenericRWBuff_ClaimerCnt = 0;

uint8_t GenericRBuff[UXRT_PASS_SHARED_TX_RX_BUFFER_SIZE];
uint8_t GenericWBuff[UXRT_PASS_SHARED_TX_RX_BUFFER_SIZE];


bool claimGenericRWBuff(uint8_t claimerID)
{
	if(_GenericRWBuff_ClaimerID == 0) {
		_GenericRWBuff_ClaimerID  = claimerID;
		_GenericRWBuff_ClaimerCnt = 1;
		return true;
	}

	else if(_GenericRWBuff_ClaimerID == claimerID) {
		++_GenericRWBuff_ClaimerCnt;
		return true;
	}

	return false;
}


bool unclaimGenericRWBuff(uint8_t claimerID)
{
	if(_GenericRWBuff_ClaimerID == claimerID) {
		if(!--_GenericRWBuff_ClaimerCnt) _GenericRWBuff_ClaimerID = 0;
		return true;
	}

	return false;
}


////////////////////////////////////////////////////////////////////////////////////////////////////


static const    uint16_t        _bootKey    = 0x7777;
static volatile uint16_t* const _bootKeyPtr = (volatile uint16_t*) 0x0800;


void _reset_impl(bool toBL)
{
	USB_Disable();

	Delay_MS(100);
	for(int i = 0; i < 5; ++i) {
		ALED_ON (); Delay_MS(100);
		ALED_OFF(); Delay_MS(100);
	}
	Delay_MS(100);

	GlobalInterruptDisable();

	if(toBL) *_bootKeyPtr = _bootKey;

	wdt_enable(WDTO_250MS);
	for(;;);
}


void resetSystem(void)
{ _reset_impl(false); }


void resetToBootloader(void)
{ _reset_impl(true); }


////////////////////////////////////////////////////////////////////////////////////////////////////

static const uint16_t _wdtEnKey = 0xAAAA;
static       uint16_t _wdtEnKeyVal ATTR_NO_INIT;


void wdtEnSetKey(void)
{ _wdtEnKeyVal = _wdtEnKey; }


void wdtEnClrKey(void)
{ _wdtEnKeyVal = 0;  }


bool wdtEnIsKeySet(void)
{ return _wdtEnKeyVal == _wdtEnKey; }


////////////////////////////////////////////////////////////////////////////////////////////////////


void waitUntil_Endpoint_IsINReady(USB_ClassInfo_CDC_Device_t* const dev)
{
	// Select the endpoint address
	Endpoint_SelectEndpoint(dev->Config.DataINEndpoint.Address);

	// Wait until the selected IN endpoint is ready for a new packet to be sent to the host
	while( !Endpoint_IsINReady() ) CDC_Device_USBTask(dev);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


#if !!ENABLE_DEBUG_STREAM

#if !!!DEBUG_STREAM_USE_AVR_TINY_UART
extern USB_ClassInfo_CDC_Device_t VirtualSerial2_CDC_Interface;
#endif

static FILE*   _debug_stream = 0;

static uint8_t _dmsgTypeMask = 0x00;


static int _debugPutChar(char ch, FILE* stream)
{
#if !!DEBUG_STREAM_USE_AVR_TINY_UART

	tinyuart_send_uint8(ch);

	return 0;

#else

	waitUntil_Endpoint_IsINReady(&VirtualSerial2_CDC_Interface);

	return ( CDC_Device_SendByte(&VirtualSerial2_CDC_Interface, ch) == ENDPOINT_READYWAIT_NoError ) ? 0 : -1;

#endif
}


void debug_initStream(void)
{
	static FILE stream = FDEV_SETUP_STREAM(_debugPutChar, 0, _FDEV_SETUP_RW);

	_debug_stream = &stream;
}


void debug_setMsgTypeMask(uint8_t dmsgTypeMask)
{ _dmsgTypeMask = dmsgTypeMask; }


void debug_printf(uint8_t dmsgTypeBit, __flash_string format, ...)
{
	if(_debug_stream == 0) return;

	if( (dmsgTypeBit != DMSG_TYPE_BIT_ALWAYS) && !(_dmsgTypeMask & dmsgTypeBit) ) return;

	va_list argptr;

	                                                    _debugPutChar('[', 0);
	     if(dmsgTypeBit == DMSG_TYPE_BIT_ALWAYS     ) { _debugPutChar('X', 0); }
	else if(dmsgTypeBit &  DMSG_TYPE_BIT_ERROR      ) { _debugPutChar('E', 0); }
	else if(dmsgTypeBit &  DMSG_TYPE_BIT_WARNING    ) { _debugPutChar('W', 0); }
	else if(dmsgTypeBit &  DMSG_TYPE_BIT_NOTICE     ) { _debugPutChar('N', 0); }
	else if(dmsgTypeBit &  DMSG_TYPE_BIT_INFORMATION) { _debugPutChar('I', 0); }
	                                                    _debugPutChar(']', 0);
	                                                    _debugPutChar(' ', 0);

	va_start(argptr, format);
#if !!DEBUG_STREAM_USE_AVR_TINY_UART
		ATOMIC_BLOCK(ATOMIC_FORCEON) {
#endif
		vfprintf_P( _debug_stream, _CCCF(format), argptr );
#if !!DEBUG_STREAM_USE_AVR_TINY_UART
		}
#endif
	va_end(argptr);
}

#endif
