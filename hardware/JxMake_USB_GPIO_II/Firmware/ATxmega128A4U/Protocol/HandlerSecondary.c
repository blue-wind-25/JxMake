/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdint.h>

#include <LUFA/Drivers/Misc/RingBuffer.h>
#include <LUFA/Drivers/USB/USB.h>

#include "../Descriptors.h"
#include "../Millis.h"
#include "../Utils.h"
#include "../Modules/HW_UART_USRT.h"
#include "HandlerSecondary.h"


// Buffers
static RingBuffer_t _rbuff;
static RingBuffer_t _wbuff;

// Defined in 'HandlerPrimary.c'
extern volatile uint8_t _SYSTEM_ID[9];
extern volatile bool    _G_CMD_DETECT;


////////////////////////////////////////////////////////////////////////////////////////////////////


bool initSecondary(uint8_t grwbClaimerID)
{
	// Claim the generic read and write buffers
	if( !claimGenericRWBuff(grwbClaimerID) ) return false;

	// Initialize ring buffers
	RingBuffer_InitBuffer( &_rbuff, GenericRBuff, sizeof(GenericRBuff) );
	RingBuffer_InitBuffer( &_wbuff, GenericWBuff, sizeof(GenericWBuff) );

	// Set the write buffer as the UART/USRT receive buffer
	hwuxrt_setReceiveBuffer(&_wbuff);

	// Done
	return true;
}


void uninitSecondary(uint8_t grwbClaimerID)
{
	// Simply exit if the UART/USRT does not have a receive buffer
	if( !hwuxrt_hasReceiveBuffer() ) return;

	// Clear the UART/USRT receive buffer
	hwuxrt_setReceiveBuffer(0);

	// Unclaim the generic read and write buffers
	unclaimGenericRWBuff(grwbClaimerID);
}


void handleSecondary(USB_ClassInfo_CDC_Device_t* const dev)
{
	/*
	const int16_t test = CDC_Device_ReceiveByte(dev);
	if(test >= 0) CDC_Device_SendByte(dev, test);
	return;
	//*/

	// Check if the UART/USRT does not have a receive buffer or the UART/USRT is not enabled
	if( !hwuxrt_hasReceiveBuffer() || !hwuxrt_isEnabled() ) {
		// Exit if no byte is received
		if( CDC_Device_BytesReceived(dev) <= 0 ) return;
		// Discard all received bytes
		while( CDC_Device_ReceiveByte(dev) >= 0 ) {};
		// Simply exit if CMD_DETECT has not been received and replied properly
		if(!_G_CMD_DETECT) return;
		// Send the system ID
		CDC_Device_SendData( dev, (const char*) _SYSTEM_ID, sizeof(_SYSTEM_ID) );
		CDC_Device_Flush(dev);
		// Reset the flag
		_G_CMD_DETECT = false;
		// Exit
		return;
	}

	/*
	const int16_t recv = CDC_Device_ReceiveByte(dev);
	if(recv >= 0) {
		hwuxrt_sendByte(recv);
		blink_aled();
	}
	return;
	//*/

	// Read bytes into the read buffer
	if( !RingBuffer_IsFull(&_rbuff) ) {
		const int16_t recv = CDC_Device_ReceiveByte(dev);
		if(recv >= 0) RingBuffer_Insert(&_rbuff, recv);
	}

	// Get and check the number of bytes in the write buffer
	const uint8_t wSize = RingBuffer_GetCount(&_wbuff);

	// Only proceed if there are bytes to be written and the IN endpoint is ready for a new packet to be sent to the host
	Endpoint_SelectEndpoint(dev->Config.DataINEndpoint.Address);

	if( wSize && Endpoint_IsINReady() ) {

		// Determine the number of bytes that can be sent without blocking
		uint8_t bytesToSend = MIN(wSize, CDC_TXRX_EPSIZE - 1);

		// Send the bytes
		while(bytesToSend--) {

			// Try to send the next byte of data to the host, break if there is an error
			if( CDC_Device_SendByte( dev, RingBuffer_Peek(&_wbuff) ) != ENDPOINT_READYWAIT_NoError ) break;

			// Dequeue the already sent byte from the write buffer
			RingBuffer_Remove(&_wbuff);

		} // while
	} // if

	// Send one byte to the UART/USRT
	// ##### ??? TODO : Create a temporary buffer and use DMA ??? #####
	while( !RingBuffer_IsEmpty(&_rbuff) /*&& hwuxrt_isSendReady()*/ ) {
		hwuxrt_sendByte( RingBuffer_Remove(&_rbuff) );
		blink_aled();
	}
}


bool secondaryRdBufIsEmpty(void)
{ return RingBuffer_IsEmpty(&_rbuff); }


bool secondaryWrBufIsEmpty(void)
{ return RingBuffer_IsEmpty(&_wbuff); }
