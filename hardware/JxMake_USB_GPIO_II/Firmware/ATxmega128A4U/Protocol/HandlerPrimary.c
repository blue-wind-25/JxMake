/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#define __PROTOCOL_HANDLER_PRIMARY_C__


#include <stdint.h>

#include <avr/pgmspace.h>

#include <LUFA/Drivers/Misc/RingBuffer.h>
#include <LUFA/Drivers/USB/USB.h>

#include "../Descriptors.h"
#include "../Millis.h"
#include "../Utils.h"
#include "../Modules/HW_GPIO.h"
#include "../Modules/HW_SPI.h"
#include "../Modules/HW_TWI.h"
#include "../Modules/HW_UART_USRT.h"
#include "../Modules/BB_USRT.h"
#include "../Modules/BB_SWIM.h"
#include "../Modules/JTAG.h"
#include "HandlerPrimary.h"
#include "HandlerSecondary.h"
#include "Protocol.h"


// Timeout value for receiving command parameter byte(s)
static const uint32_t _cmdPBLTimeout_MS = 200;

// Read buffer
static uint8_t        _rbuffData[PROTOCOL_USB_READ_WRITE_BUFFER_SIZE];
static RingBuffer_t   _rbuff;

static uint8_t        _abuffData[PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE];

// State variables, write buffer, and error message buffer
static  uint8_t       _cmd;
static _SInt_IZArg_t  _cmdPBLTot;
static _SInt_IZArg_t  _cmdPBLIdx;
static uint32_t       _cmdPBLMillis;

static  uint8_t       _wbuffData[PROTOCOL_USB_READ_WRITE_BUFFER_SIZE];
static _SInt_IZUSB_t  _wbuffLen;
static _SInt_IZArg_t  _wbuffLenArg;

static __flash_string _ebuffPtr_P;

static bool           _hwusrtSSMode;
static uint8_t        _dataTransferSize;
static uint8_t        _dataTransferSize2;
static uint8_t        _twiSlaveAddress;
static uint8_t        _swimEAddress;
static uint8_t        _swimHAddress;
static uint8_t        _swimLAddress;
static uint8_t        _hwspi_xbt_ioocc_eooc;
static uint8_t        _hwspi_xbt_vvvvvvvv;

static uint8_t        _wAfterDelayUs25_wSPIMode ATTR_ALIAS(_swimEAddress     );
static uint8_t        _wSize                    ATTR_ALIAS(_dataTransferSize );
static uint8_t        _rInterDelayUs10_rSPIMode ATTR_ALIAS(_swimHAddress     );
static uint8_t        _rSize                    ATTR_ALIAS(_dataTransferSize2);
static uint8_t        _rDummyValue              ATTR_ALIAS(_swimLAddress     );
static uint8_t        _jtagConfigBits           ATTR_ALIAS(_dataTransferSize2);


////////////////////////////////////////////////////////////////////////////////////////////////////


static inline void _resetCmd(void)
{
	_cmd               = CMD_INVALID;
	_cmdPBLTot         = -1;
	_cmdPBLIdx         =  0;

	_wbuffLen          =  0;
	_wbuffLenArg       =  0;
	_ebuffPtr_P        =  0;

	_dataTransferSize  =  0;
	_dataTransferSize2 =  0;
	_twiSlaveAddress   =  0;
	_swimEAddress      =  0;
	_swimHAddress      =  0;
	_swimLAddress      =  0;
}


static void _sendAck(USB_ClassInfo_CDC_Device_t* const dev)
{
	// Wait until the endpoint is ready
	waitUntil_Endpoint_IsINReady(dev);

	// Send ACK
	CDC_Device_SendByte(dev, CMD_RES_ACK);

	// Send the response data bytes from '_wbuffData' as needed
	_SInt_IZUSB_t _wbuffIdx = 0;

	while(_wbuffLen) {

		// Determine the number of bytes that can be sent without blocking
		uint8_t bytesToSend = MIN( _wbuffLen, CDC_TXRX_EPSIZE - (_wbuffIdx ? 1 : 2) );

		// Send the bytes
		while(bytesToSend--) {

			// Try to send the next byte of data to the host, break if there is an error
			if( CDC_Device_SendByte(dev, _wbuffData[_wbuffIdx]) != ENDPOINT_READYWAIT_NoError ) break;

			// Adjust the counters
			--_wbuffLen;
			++_wbuffIdx;

		} // while

		// Blink the activity LED
	    blink_aled();

		// Wait until the endpoint is ready again
		if(_wbuffLen) waitUntil_Endpoint_IsINReady(dev);

	} // while

	// Send the response data bytes from '_abuffData' as needed
	_SInt_IZArg_t _abuffIdx = 0;

	while(_wbuffLenArg) {

		// Determine the number of bytes that can be sent without blocking
		uint8_t bytesToSend = MIN( _wbuffLenArg, CDC_TXRX_EPSIZE - (_abuffIdx ? 1 : 2) );

		// Send the bytes
		while(bytesToSend--) {

			// Try to send the next byte of data to the host, break if there is an error
			if( CDC_Device_SendByte(dev, _abuffData[_abuffIdx]) != ENDPOINT_READYWAIT_NoError ) break;

			// Adjust the counters
			--_wbuffLenArg;
			++_abuffIdx;

		} // while

		// Blink the activity LED
	    blink_aled();

		// Wait until the endpoint is ready again
		if(_wbuffLenArg) waitUntil_Endpoint_IsINReady(dev);

	} // while

}


static void _sendNak(USB_ClassInfo_CDC_Device_t* const dev)
{
	// Get the length of the error message
	uint8_t ebufLen  = _ebuffPtr_P ? strlen_P( _CCCF(_ebuffPtr_P) ) : 0;
	uint8_t ebuffIdx = 0;

	// Wait until the endpoint is ready
	waitUntil_Endpoint_IsINReady(dev);

	// Send NAK
	CDC_Device_SendByte(dev, CMD_RES_NAK);

	// Send the length of the error message
	CDC_Device_SendByte(dev, ebufLen);

	// Send the error message bytes as needed
	while(ebufLen) {

		// Determine the number of bytes that can be sent without blocking
		uint8_t bytesToSend = MIN( ebufLen, CDC_TXRX_EPSIZE - (ebuffIdx ? 1 : 3) );

		// Send the bytes
		while(bytesToSend--) {

			// Try to send the next byte of data to the host, break if there is an error
			if( CDC_Device_SendByte( dev, pgm_read_byte( _CCCF(_ebuffPtr_P) + ebuffIdx ) ) != ENDPOINT_READYWAIT_NoError ) break;

			// Adjust the counters
			--ebufLen;
			++ebuffIdx;

		} // while

		// Blink the activity LED
	    blink_aled();

		// Wait until the endpoint is ready again
		if(ebufLen) waitUntil_Endpoint_IsINReady(dev);

	} // while
}


static inline void _sendAck_resetCmd(USB_ClassInfo_CDC_Device_t* const dev)
{
	_sendAck(dev);
	_resetCmd();
}


static inline void _sendNak_resetCmd(USB_ClassInfo_CDC_Device_t* const dev)
{
	__DPRINTF_DECL_PREFIX("_sendNak_resetCmd");

	if(_ebuffPtr_P) __DPRINTFS_N("%S", _ebuffPtr_P);
	else            __DPRINTFS_N("?"              );

	_sendNak(dev);
	_resetCmd();
}


////////////////////////////////////////////////////////////////////////////////////////////////////


// Defined in '../JxMakeUSBGPPIO.c'
extern volatile uint32_t _resetSystem_setMillis;
extern volatile uint32_t _resetToBL_bps1200Millis;

volatile uint8_t _SYSTEM_ID[9] = { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
volatile bool    _G_CMD_DETECT = false;


void initPrimary(void)
{
	BEG_READ_PRODROW();
	_SYSTEM_ID[0] = READ_PRODROW_VAL(LOTNUM0) + READ_PRODROW_VAL(LOTNUM1);
	_SYSTEM_ID[1] = READ_PRODROW_VAL(LOTNUM2) + READ_PRODROW_VAL(LOTNUM3);
	_SYSTEM_ID[2] = READ_PRODROW_VAL(LOTNUM4) + READ_PRODROW_VAL(LOTNUM5);
	_SYSTEM_ID[3] = READ_PRODROW_VAL(WAFNUM );
	_SYSTEM_ID[4] = READ_PRODROW_VAL(COORDX0) + READ_PRODROW_VAL(COORDX1);
	_SYSTEM_ID[5] = READ_PRODROW_VAL(COORDY0) + READ_PRODROW_VAL(COORDY1);
	END_READ_PRODROW();
	_SYSTEM_ID[6] = PROTOCOL_VERSION_M;
	_SYSTEM_ID[7] = PROTOCOL_VERSION_N;
	_SYSTEM_ID[8] = PROTOCOL_VERSION_R;

	RingBuffer_InitBuffer( &_rbuff, _rbuffData, sizeof(_rbuffData) );

	_resetCmd();
}


////////////////////////////////////////////////////////////////////////////////////////////////////


void handlePrimary(USB_ClassInfo_CDC_Device_t* const dev)
{
	/*
	const int16_t test = CDC_Device_ReceiveByte(dev);
	if(test >= 0) CDC_Device_SendByte(dev, test);
	return;
	//*/

	// Set this to 0 to disable all '__DPRINTFS_I()' in this function
#if 1
	#define __DPRINTF_EN__
	#define __DPRINTFS_I__(...) __DPRINTFS_I(__VA_ARGS__)
#else
	#define __DPRINTFS_I__(...) do {} while(0)
#endif

	// Set this to 1 to disable all '__DPRINTFF_E()' and '__DPRINTFS_E()' in this function (they have actually been covered by '_ebuffPtr_P')
#if 1
	#define __DPRINTF_EN__
	#define __DPRINTFF_E__(...) __DPRINTFF_E(__VA_ARGS__)
	#define __DPRINTFS_E__(...) __DPRINTFS_E(__VA_ARGS__)
#else
	#define __DPRINTFF_E__(...) do {} while(0)
	#define __DPRINTFS_E__(...) do {} while(0)
#endif

	// This section is enabled only if at least one of the above sections is enabled
#ifdef __DPRINTF_EN__
	__DPRINTF_DECL_PREFIX("handlePrimary");
	#define __DPRINTF_DECL_SPGMVR__(...) __DPRINTF_DECL_SPGMVR(__VA_ARGS__)
#else
	#define __DPRINTF_DECL_SPGMVR__(...) do {} while(0)
#endif

	// Error messages that are used more than once
	_SPGM(failInitSInterface, "failed to initialize the secondary (pass-through) interface"                                                        );
	_SPGM(failRdNESInterface, "the secondary (pass-through) interface read buffer is not empty"                                                    );
	_SPGM(failInitHWUSRT_SS , "failed to initialize slave selection support for HW-USRT"                                                           );
	_SPGM(hwusrtNoSupportSS , "in the current state, HW-USRT does not support slave selection because the signal is already used by HW-SPI/BB-SWIM");

	_SPGM(hwspisIsNotEnabled, "HW-SPI is not enabled"                                                                                              );
	_SPGM(hwuartIsNotEnabled, "HW-UART is not enabled"                                                                                             );
	_SPGM(hwusrtIsNotEnabld , "HW-USRT is not enabled"                                                                                             );
	_SPGM(bbswimIsNotEnabled, "BB-SWIM is not enabled"                                                                                             );
	_SPGM(jtagIsNotEnabled  , "JTAG is not enabled"                                                                                                );

	_SPGM(hwspiNByOutOfRange, "HW-SPI the number of bytes is out of the allowed range"                                                             );
	_SPGM(hwspiTransferFaild, "HW-SPI transfer failed"                                                                                             );
	_SPGM(xdstrNByOutOfRange, "%S%S - failed: the number of bytes (%d) is out of range\n"                                                          );

	// Receive the bytes
	while( !RingBuffer_IsFull(&_rbuff) ) {

		// Receive byte
		const int16_t recv = CDC_Device_ReceiveByte(dev);

		// Check if no byte was received
		if(recv < 0) {
			// Break if the ring buffer is not empty
			if( !RingBuffer_IsEmpty(&_rbuff) ) {
				blink_aled(); // Blink the activity LED
				break;
			}
			// Break if there are parameter byte(s) to be received
			if(_cmdPBLTot > 0) break;
			// Otherwise, exit the function
			return;
		}

		// Store the byte
		RingBuffer_Insert(&_rbuff, recv);

	} // while

	// Get the command and the number of parameter byte(s)
	if(_cmd == CMD_INVALID) {

		// A command is at least 2 bytes long
		while( RingBuffer_GetCount(&_rbuff) >= 2 ) {

			// Ignore everything before CMD_PREFIX
			const uint8_t rprefix = RingBuffer_Remove(&_rbuff);
			if(rprefix != CMD_PREFIX) continue;

			// Get the command byte
			const uint8_t rcmd = RingBuffer_Remove(&_rbuff);

			// Get the number of parameter byte(s) and check for error
			const uint8_t rcmdPBLTot = pgm_read_byte(_CMD_PAR_BYTE_LEN + rcmd);

			if(rcmdPBLTot == 0xFF) {
				__DPRINTFS_E__("received invalid command %02X", rcmd);
				_wbuffLen   = -1;
				_ebuffPtr_P = _FSTR("invalid command");
				_sendNak_resetCmd(dev);
				return;
			}
			else {
				__DPRINTFS_I__("received valid command %02X ; number of parameter byte(s) = %d", rcmd, rcmdPBLTot);
			}

			// Store the command and the number of parameter byte(s)
			_cmd          = rcmd;
			_cmdPBLTot    = rcmdPBLTot;
			_cmdPBLIdx    = 0;
			_cmdPBLMillis = millis();
			break;

		} // while

	} // if

	// Get the parameter byte(s)
	if(_cmdPBLTot > 0 && _cmdPBLIdx < _cmdPBLTot) {

		// Get and store the byte(s)
		bool gotByte = false;
		while( !RingBuffer_IsEmpty(&_rbuff) && _cmdPBLIdx < _cmdPBLTot ) {
			gotByte = true;
			_abuffData[_cmdPBLIdx++] = RingBuffer_Remove(&_rbuff);
		}

		// Check if not all parameter byte(s) have been received
		if(_cmdPBLIdx < _cmdPBLTot) {
			// Update the time if there was any bytes recevied
			if(gotByte) _cmdPBLMillis = millis();
			// Check for timeout
			if( millis() - _cmdPBLMillis > _cmdPBLTimeout_MS ) {
				_ebuffPtr_P = _FSTR("receive command parameter byte(s) timeout");
				_sendNak_resetCmd(dev);
				return;
			}
		}
		else {
			__DPRINTFS_I__("received %d command parameter byte(s)", _cmdPBLIdx);
		}

	} // if

	// Process the command
	if(_cmd != CMD_INVALID && _cmdPBLIdx == _cmdPBLTot) {

		// Process according to the command
		switch(_cmd) {

			#include "HandlerPrimary_0_System.c"
			#include "HandlerPrimary_1_GPIO.c"
			#include "HandlerPrimary_2_HWSPI.c"
			#include "HandlerPrimary_3_HWTWI.c"
			#include "HandlerPrimary_4_HWUART.c"
			#include "HandlerPrimary_5_HWUSRT.c"
			#include "HandlerPrimary_6_BBUSRT.c"
			#include "HandlerPrimary_7_BBSWIM.c"
			#include "HandlerPrimary_8_JTAG.c"

			default:
				__DPRINTFS_E__("command %02X is not implemented yet", _cmd);
				_wbuffLen   = -1;
				_ebuffPtr_P = _FSTR("not implemented yet");
				break;

		} // switch

		// Send the response (and optionally the response data bytes)
		if(_wbuffLen < 0) _sendNak_resetCmd(dev);
		else              _sendAck_resetCmd(dev);

	} // if

	// Undefine the macros defined at the beginning of the function
	#undef __DPRINTF_EN__
	#undef __DPRINTFS_I__
	#undef __DPRINTFS_E__
	#undef __DPRINTF_DECL_SPGMVR__
}

