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
	const uint16_t it = hwgpio_getInternalTemperature();

	_SYSTEM_ID[0] = boot_signature_byte_get(0);
	_SYSTEM_ID[1] = boot_signature_byte_get(2);
	_SYSTEM_ID[2] = boot_signature_byte_get(4);
	_SYSTEM_ID[3] = boot_signature_byte_get(1);
	_SYSTEM_ID[4] = it >> 8;
	_SYSTEM_ID[5] = it & 0xFF;
	_SYSTEM_ID[6] = PROTOCOL_VERSION_M;
	_SYSTEM_ID[7] = PROTOCOL_VERSION_N;
	_SYSTEM_ID[8] = PROTOCOL_VERSION_R;

	RingBuffer_InitBuffer( &_rbuff, _rbuffData, sizeof(_rbuffData) );

	_resetCmd();
}


void handlePrimary(USB_ClassInfo_CDC_Device_t* const dev)
{
	// Set this to 0 to disable all '__DPRINTFS_I()' in this function
#if 1
	#define __DPRINTF_EN__
	#define __DPRINTFS_I__(...) __DPRINTFS_I(__VA_ARGS__)
#else
	#define __DPRINTFS_I__(...) do {} while(0)
#endif

	// Set this to 1 to enable all '__DPRINTFF_E()' and '__DPRINTFS_E()' in this function (they have actually been covered by '_ebuffPtr_P')
#if 0
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

	/*
	const int16_t test = CDC_Device_ReceiveByte(dev);
	if(test >= 0) CDC_Device_SendByte(dev, test);
	return;
	//*/

	// Error messages that are used more than once
	_SPGM(failInitSInterface, "failed to initialize the secondary (pass-through) interface"                                                        );
//	_SPGM(failBfNESInterface, "the secondary (pass-through) interface read and/or write buffer is not empty"                                       );
	_SPGM(failRdNESInterface, "the secondary (pass-through) interface read buffer is not empty"                                                    );
//	_SPGM(failWrNESInterface, "the secondary (pass-through) interface write buffer is not empty"                                                   );
	_SPGM(failInitHWUSRT_SS , "failed to initialize slave selection support for HW-USRT"                                                           );
	_SPGM(hwusrtNoSupportSS , "in the current state, HW-USRT does not support slave selection because the signal is already used by HW-SPI/BB-SWIM");

	_SPGM(hwspisIsNotEnabled, "HW-SPI is not enabled"                                                                                              );
	_SPGM(hwuartIsNotEnabled, "HW-UART is not enabled"                                                                                             );
	_SPGM(hwusrtIsNotEnabld , "HW-USRT is not enabled"                                                                                             );
	_SPGM(bbswimIsNotEnabled, "BB-SWIM is not enabled"                                                                                             );

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

			case CMD_PING:
				__DPRINTFS_I__("CMD_PING");
				break;

			case CMD_GET_PROTOCOL_VERSION:
				__DPRINTFS_I__("CMD_GET_PROTOCOL_VERSION");
				_wbuffData[0] = PROTOCOL_VERSION_M;
				_wbuffData[1] = PROTOCOL_VERSION_N;
				_wbuffData[2] = PROTOCOL_VERSION_R;
		#if !!ENABLE_DEBUG_STREAM
			#if !!DEBUG_STREAM_USE_AVR_TINY_UART
				_wbuffData[3] = 2;
			#else
				_wbuffData[3] = 1;
			#endif
		#else
				_wbuffData[3] = 0;
		#endif
				_wbuffLen     = 4;
				break;

			case CMD_GET_FIRMWARE_VERSION:
				__DPRINTFS_I__("CMD_GET_FIRMWARE_VERSION");
				_wbuffData[0] = FIRMWARE_VERSION_M;
				_wbuffData[1] = FIRMWARE_VERSION_N;
				_wbuffData[2] = FIRMWARE_VERSION_R;
		#if !!ENABLE_DEBUG_STREAM
			#if !!DEBUG_STREAM_USE_AVR_TINY_UART
				_wbuffData[3] = 2;
			#else
				_wbuffData[3] = 1;
			#endif
		#else
				_wbuffData[3] = 0;
		#endif
				_wbuffLen     = 4;
				break;

			case CMD_ENABLE_DEBUG_MESSAGE:
				if(true) {
					const int dmsgTypeMask = _abuffData[0] & 0x0F;
					__DPRINTFS_I__("CMD_ENABLE_DEBUG_MESSAGE %02X", dmsgTypeMask);
					debug_setMsgTypeMask(dmsgTypeMask);
				}
				break;

			case CMD_RESET:
				__DPRINTFS_I__("CMD_RESET");
				_resetSystem_setMillis = millis() + RESET_DELAY_MS;
				break;

			case CMD_RESET_TO_BOOTLOADER:
				__DPRINTFS_I__("CMD_RESET_TO_BOOTLOADER");
				_resetToBL_bps1200Millis = millis() + RESET_DELAY_MS;
				break;

			case CMD_DETECT:
				if( !hwuxrt_isEnabled() &&
					_abuffData[0] == 'h' && _abuffData[1] == 'E' && _abuffData[2] == 'L' && _abuffData[3] == 'L' &&
					_abuffData[4] == 'o' && _abuffData[5] == '\n'
				) {
					_wbuffData[ 0] = 'J'; _wbuffData[ 1] = 'x'; _wbuffData[ 2] = 'M'; _wbuffData[ 3] = 'a';
					_wbuffData[ 4] = 'k'; _wbuffData[ 5] = 'e'; _wbuffData[ 6] = ' '; _wbuffData[ 7] = 'U';
					_wbuffData[ 8] = 'S'; _wbuffData[ 9] = 'B'; _wbuffData[10] = '-'; _wbuffData[11] = 'G';
					_wbuffData[12] = 'P'; _wbuffData[13] = 'I'; _wbuffData[14] = 'O'; _wbuffData[15] = ' ';
					_wbuffData[16] = 'M'; _wbuffData[17] = 'o'; _wbuffData[18] = 'd'; _wbuffData[19] = 'u';
					_wbuffData[20] = 'l'; _wbuffData[21] = 'e'; _wbuffData[22] = '\n';
					_wbuffData[23] = _SYSTEM_ID[0]; _wbuffData[24] = _SYSTEM_ID[1];
					_wbuffData[25] = _SYSTEM_ID[2]; _wbuffData[26] = _SYSTEM_ID[3];
					_wbuffData[27] = _SYSTEM_ID[4]; _wbuffData[28] = _SYSTEM_ID[5];
					_wbuffData[29] = _SYSTEM_ID[6]; _wbuffData[30] = _SYSTEM_ID[7];
					_wbuffData[31] = _SYSTEM_ID[8];
					_wbuffLen      = 32;
					_G_CMD_DETECT  = true;
				}
				else {
					_wbuffLen   = -1;
					_ebuffPtr_P =  0;
				}
				break;

			////////////////////////////////////////////////////////////////////////////////////////////////////

			case CMD_HW_GPIO_SET_MODE:
				__DPRINTFS_I__("CMD_HW_GPIO_SET_MODE %02X %02X %02X", _abuffData[0], _abuffData[1], _abuffData[2]);
				hwgpio_setMode(_abuffData[0], _abuffData[1], _abuffData[2]);
				break;

			case CMD_HW_GPIO_SET_VALUES:
				__DPRINTFS_I__("CMD_HW_GPIO_SET_VALUES %02X %02X", _abuffData[0], _abuffData[1]);
				hwgpio_setValues(_abuffData[0], _abuffData[1]);
				break;

			case CMD_HW_GPIO_GET_VALUES:
				_wbuffData[0] = hwgpio_getValues();
				_wbuffLen     = 1;
				__DPRINTFS_I__("CMD_HW_GPIO_GET_VALUES %02X", _wbuffData[0]);
				break;

			case CMD_HW_GPIO_SET_PWM:
				__DPRINTFS_I__("CMD_HW_GPIO_SET_PWM %02X %02X", _abuffData[0], _abuffData[1]);
				hwgpio_setPWM(_abuffData[0], _abuffData[1]);
				break;

			case CMD_HW_GPIO_GET_ADC:
				if(true) {
					const uint16_t res = hwgpio_getADC(_abuffData[0]);
					__DPRINTFS_I__("CMD_HW_GPIO_GET_ADC %04X", res);
					_wbuffData[0] = (res >> 8) & 0xFF;
					_wbuffData[1] = (res >> 0) & 0xFF;
					_wbuffLen     = 2;
				}
				break;

			////////////////////////////////////////////////////////////////////////////////////////////////////

			case CMD_HW_SPI_ENABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_ENABLE, "CMD_HW_SPI_ENABLE");
				if( hwuxrt_ss_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: BB-USRT is enabled or HW-USRT has enabled SS support", SPGM_CMD_HW_SPI_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("cannot enable HW-SPI because BB-USRT is already enabled or HW-USRT has enabled slave selection support");
				}
				else if( bbswim_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: BB-SWIM is enabled", SPGM_CMD_HW_SPI_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("cannot enable HW-SPI because BB-SWIM is already enabled");
				}
				else if( hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_HW_SPI_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("HW-SPI is already enabled");
				}
				else {
					const uint8_t spiMode = (_abuffData[0] >> 5) & 0x03;
					const bool    ssMode  = (_abuffData[0] >> 4) & 0x01;
					const uint8_t clkDiv  = (_abuffData[0]     ) & 0x0F;
					__DPRINTFS_I__("%S %d %d %d", SPGM_CMD_HW_SPI_ENABLE, spiMode, ssMode, clkDiv);
					if( !hwspi_begin(spiMode, ssMode, clkDiv) ) {
						__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_HW_SPI_ENABLE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("failed to initialize HW-SPI");
					}
				}
				break;

			case CMD_HW_SPI_DISABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_DISABLE, "CMD_HW_SPI_DISABLE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_DISABLE);
				if( !hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_DISABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
				}
				else {
					hwspi_end();
				}
				break;

			case CMD_HW_SPI_SELECT_SLAVE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_SELECT_SLAVE, "CMD_HW_SPI_SELECT_SLAVE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_SELECT_SLAVE);
				if( !hwspi_selectSlave() ) {
					__DPRINTFS_E__("%S - failed", SPGM_CMD_HW_SPI_SELECT_SLAVE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("HW-SPI failed to select slave");
				}
				break;

			case CMD_HW_SPI_DESELECT_SLAVE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_DESELECT_SLAVE, "CMD_HW_SPI_DESELECT_SLAVE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_DESELECT_SLAVE);
				if( !hwspi_deselectSlave() ) {
					__DPRINTFS_E__("%S - failed", SPGM_CMD_HW_SPI_DESELECT_SLAVE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("HW-SPI failed to deselect slave");
				}
				break;

			case CMD_HW_SPI_TRANSFER:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_TRANSFER, "CMD_HW_SPI_TRANSFER");
				if(!_dataTransferSize) {
					__DPRINTFS_I__("%S - PHASE 1 [%d]", SPGM_CMD_HW_SPI_TRANSFER, _abuffData[0]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
					if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
						__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_SPI_TRANSFER, _abuffData[0]);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FPGM(hwspiNByOutOfRange);
					}
					else {
						// Save the number of bytes
						_dataTransferSize = _abuffData[0];
						// Get more parameter byte(s)
						_cmdPBLTot    = _abuffData[0];
						_cmdPBLIdx    = 0;
						_cmdPBLMillis = millis();
						return;
					}
				}
				else {
					__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_HW_SPI_TRANSFER, _dataTransferSize);
					// Perform SPI transfer
					if( !hwspi_transfer(_abuffData, _dataTransferSize) ) {
						__DPRINTFS_E__("%S - failed: transfer error", SPGM_CMD_HW_SPI_TRANSFER);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FPGM(hwspiTransferFaild);
					}
					// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
					else {
						_wbuffLenArg = _dataTransferSize;
					}
					/*
					// If there is no error, send back the response
					else {
						_wbuffLen = _dataTransferSize;
						memcpy(_wbuffData, _abuffData, _dataTransferSize);
					}
					*/
				}
				break;

			case CMD_HW_SPI_TRANSFER_W16ND_R16DN:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, "CMD_HW_SPI_TRANSFER_W16ND_R16DN");
				if(!_wSize && !_rSize) {
					__DPRINTFS_I__("%S - PHASE 1 [%d] [%d]", SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, _abuffData[1], _abuffData[3]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
					if( ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ||
						( (_SInt_IZArg_t) _abuffData[3] ) > sizeof(_abuffData)
					) {
						if( ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ) __DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, _abuffData[1]);
						if( ( (_SInt_IZArg_t) _abuffData[3] ) > sizeof(_abuffData) ) __DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, _abuffData[3]);
#pragma GCC diagnostic pop
						_wbuffLen   = -1;
						_ebuffPtr_P = _FPGM(hwspiNByOutOfRange);
					}
					else {
						// Save the parameters
						_wAfterDelayUs25_wSPIMode = _abuffData[0];
						_wSize                    = _abuffData[1];
						_rInterDelayUs10_rSPIMode = _abuffData[2];
						_rSize                    = _abuffData[3];
						_rDummyValue              = _abuffData[4];
						// Get more parameter byte(s) as needed
						if(_wSize > 0) {
							_cmdPBLTot    = _wSize;
							_cmdPBLIdx    = 0;
							_cmdPBLMillis = millis();
							return;
						}
					}
				}
				if(_wSize || _rSize) {
					__DPRINTFS_I__("%S - PHASE 2 [%d] [%d]", SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN, _wSize, _rSize);
					if( !hwspi_transfer_w16Nd_r16dN(_wAfterDelayUs25_wSPIMode, _wSize, _rInterDelayUs10_rSPIMode, _rSize, _rDummyValue, _abuffData) ) {
						__DPRINTFS_E__("%S - failed: transfer error", SPGM_CMD_HW_SPI_TRANSFER_W16ND_R16DN);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FPGM(hwspiTransferFaild);
					}
					// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
					else {
						_wbuffLenArg = _rSize;
					}
				}
				break;

			case CMD_HW_SPI_SET_SCK_FREQUENCY:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY, "CMD_HW_SPI_SET_SCK_FREQUENCY");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY);
				if( !hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
				}
				else {
					const uint8_t clkDiv = _abuffData[0] & 0x7F;
					__DPRINTFS_I__("%S %d", SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY, clkDiv);
					if( !hwspi_setClkDiv(clkDiv) ) {
						__DPRINTFS_E__("%S - failed: set clock divider error", SPGM_CMD_HW_SPI_SET_SCK_FREQUENCY);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("failed to set HW-SPI clock divider");
					}
				}
				break;

			case CMD_HW_SPI_SET_SPI_MODE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_SET_SPI_MODE, "CMD_HW_SPI_SET_SPI_MODE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_SET_SPI_MODE);
				if( !hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_SET_SPI_MODE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
				}
				else {
					const uint8_t spiMode = _abuffData[0] & 0x03;
					__DPRINTFS_I__("%S %d", SPGM_CMD_HW_SPI_SET_SPI_MODE, spiMode);
					if( !hwspi_setSPIMode(spiMode) ) {
						__DPRINTFS_E__("%S - failed: set SPI mode error", SPGM_CMD_HW_SPI_SET_SPI_MODE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("failed to set HW-SPI SPI mode");
					}
				}
				break;

			case CMD_HW_SPI_SET_CLR_BREAK:  /* FALLTHROUGH */
			case CMD_HW_SPI_SET_CLR_BREAK_EXT:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_SET_CLR_BREAK, "CMD_HW_SPI_SET_CLR_BREAK[_EXT]");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
				if( !hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
				}
				else {
					const uint8_t b = _abuffData[0] & 0x80;
					__DPRINTFS_I__("%S %08X", SPGM_CMD_HW_SPI_SET_CLR_BREAK, _abuffData[0]);
					if(b) {
						const uint8_t o = _abuffData[0] & 0x02;
						const uint8_t c = _abuffData[0] & 0x01;
						if( _cmd == CMD_HW_SPI_SET_CLR_BREAK_EXT ) {
							const int8_t r = hwspi_setBreakExt(o, c);
							if(r < 0) {
								__DPRINTFS_E__("%S - failed: set break", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
								_wbuffLen   = -1;
								_ebuffPtr_P = _FSTR("failed to set HW-SPI break");
							}
							_wbuffData[0] = r;
							_wbuffLen     = 1;
						}
						else {
							if( !hwspi_setBreak(o, c) ) {
								__DPRINTFS_E__("%S - failed: set break", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
								_wbuffLen   = -1;
								_ebuffPtr_P = _FSTR("failed to set HW-SPI break");
							}
						}
					}
					else {
						if( !hwspi_clrBreak() ) {
							__DPRINTFS_E__("%S - failed: clear break", SPGM_CMD_HW_SPI_SET_CLR_BREAK);
							_wbuffLen   = -1;
							_ebuffPtr_P = _FSTR("failed to clear HW-SPI break");
						}
						else if( _cmd == CMD_HW_SPI_SET_CLR_BREAK_EXT ) {
							_wbuffData[0] = 0;
							_wbuffLen     = 1;
						}
					}
				}
				break;

			case CMD_HW_SPI_XB_TRANSFER:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_XB_TRANSFER, "CMD_HW_SPI_XB_TRANSFER");
				if(!_dataTransferSize) {
					__DPRINTFS_I__("%S - PHASE 1 [%02X:%02X] [%d]", SPGM_CMD_HW_SPI_XB_TRANSFER, _abuffData[0], _abuffData[1], _abuffData[2]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
					if( !_abuffData[2] || ( (_SInt_IZArg_t) (_abuffData[2] * 2) ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
						__DPRINTFS_E__("%S - failed: the number of pairs (%d) is out of range", SPGM_CMD_HW_SPI_XB_TRANSFER, _abuffData[2]);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("HW-SPI the number of pairs is out of the allowed range");
					}
					else {
						// Save the parameters
						_hwspi_xbt_ioocc_eooc = _abuffData[0];
						_hwspi_xbt_vvvvvvvv   = _abuffData[1];
						_dataTransferSize     = _abuffData[2];
						// Get more parameter byte(s)
						_cmdPBLTot    = _abuffData[2] * 2;
						_cmdPBLIdx    = 0;
						_cmdPBLMillis = millis();
						return;
					}
				}
				else {
					__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_HW_SPI_XB_TRANSFER, _dataTransferSize);
					// Perform SPI transfer
					if( !hwspi_xb_transfer(_hwspi_xbt_ioocc_eooc, _hwspi_xbt_vvvvvvvv, _abuffData, _dataTransferSize) ) {
						__DPRINTFS_E__("%S - failed: XB transfer error", SPGM_CMD_HW_SPI_XB_TRANSFER);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("HW-SPI XB transfer failed");
					}
					// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
					else {
						_wbuffLenArg = _dataTransferSize * 2;
					}
				}
				break;

			case CMD_HW_SPI_XB_SPECIAL:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_SPI_XB_SPECIAL, "CMD_HW_SPI_XB_SPECIAL");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_SPI_XB_SPECIAL);
				if( !hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_SPI_XB_SPECIAL);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwspisIsNotEnabled);
				}
				else {
					__DPRINTFS_I__("%S %08X", SPGM_CMD_HW_SPI_XB_SPECIAL, _abuffData[0]);
					const int8_t res = hwspi_xb_special(_abuffData[0]);
					if(res < 0) {
						__DPRINTFS_E__("%S - failed: XB special error", SPGM_CMD_HW_SPI_XB_SPECIAL);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("HW-SPI XB special failed");
					}
					else {
						_wbuffData[0] = res;
						_wbuffLen     = 1;
					}
				}
				break;

			////////////////////////////////////////////////////////////////////////////////////////////////////

			case CMD_HW_TWI_ENABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_ENABLE, "CMD_HW_TWI_ENABLE");
				if( hwtwi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_HW_TWI_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("HW-TWI is already enabled");
				}
				else {
					const uint32_t sclFreq = ( ( (uint32_t) _abuffData[0] ) << 16 )
					                       | ( ( (uint32_t) _abuffData[1] ) <<  8 )
					                       | ( ( (uint32_t) _abuffData[2] ) <<  0 );
					const uint8_t timeout  = (_abuffData[3] >> 4) & 0x0F;
					const bool    enExtPU  = _abuffData[3] & 0x01;
					__DPRINTFS_I__("%S %lu %d %d", SPGM_CMD_HW_TWI_ENABLE, sclFreq, timeout, enExtPU);
					if( !hwtwi_begin(sclFreq, timeout, enExtPU) ) {
						__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_HW_TWI_ENABLE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("failed to initialize HW-TWI");
					}
				}
				break;

			case CMD_HW_TWI_DISABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_DISABLE, "CMD_HW_TWI_DISABLE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_TWI_DISABLE);
				if( !hwtwi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_TWI_DISABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("HW-TWI is not enabled");
				}
				else {
					hwtwi_end();
				}
				break;

			case CMD_HW_TWI_WRITE: /* FALLTHROUGH */
			case CMD_HW_TWI_WRITE_NO_STOP:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_WRITE_NO_STOP, "CMD_HW_TWI_WRITE[_NO_STOP]");
				if(!_dataTransferSize) {
					__DPRINTFS_I__("%S - PHASE 1 [%d]", SPGM_CMD_HW_TWI_WRITE_NO_STOP, _abuffData[1]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
					if( !_abuffData[1] || ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
						__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_TWI_WRITE_NO_STOP, _abuffData[1]);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("HW-TWI the number of bytes is out of the allowed range");
					}
					else {
						// Save the slave address and number of bytes
						_twiSlaveAddress  = _abuffData[0];
						_dataTransferSize = _abuffData[1];
						// Get more parameter byte(s)
						_cmdPBLTot    = _abuffData[1];
						_cmdPBLIdx    = 0;
						_cmdPBLMillis = millis();
						return;
					}
				}
				else {
					__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_HW_TWI_WRITE_NO_STOP, _dataTransferSize);
					// Perform TWI send
					if( !hwtwi_write( _twiSlaveAddress, _abuffData, _dataTransferSize, (_cmd != CMD_HW_TWI_WRITE_NO_STOP) ) ) {
						__DPRINTFS_E__("%S - failed: write error", SPGM_CMD_HW_TWI_WRITE_NO_STOP);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("HW-TWI write failed");
					}
				}
				break;

			case CMD_HW_TWI_READ: /* FALLTHROUGH */
			case CMD_HW_TWI_READ_NO_STOP:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_TWI_READ_NO_STOP, "CMD_HW_TWI_READ[_NO_STOP]");
				__DPRINTFS_I__("%S [%d]", SPGM_CMD_HW_TWI_READ_NO_STOP, _abuffData[1]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
				if( !_abuffData[1] || ( (_SInt_IZArg_t) _abuffData[1] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
					__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_HW_TWI_READ_NO_STOP, _abuffData[1]);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("HW-TWI the number of bytes is out of the allowed range");
				}
				else {
					// Save the slave address and number of bytes
					_twiSlaveAddress  = _abuffData[0];
					_dataTransferSize = _abuffData[1];
					// Perform TWI read
					if( !hwtwi_read( _twiSlaveAddress, _abuffData, _dataTransferSize, (_cmd != CMD_HW_TWI_READ_NO_STOP) ) ) {
						__DPRINTFS_E__("%S - failed: read error", SPGM_CMD_HW_TWI_READ_NO_STOP);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("HW-TWI read failed");
					}
					// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
					else {
						_wbuffLenArg = _dataTransferSize;
					}
					/*
					// If there is no error, send back the response
					else {
						_wbuffLen = _dataTransferSize;
						memcpy(_wbuffData, _abuffData, _dataTransferSize);
					}
					*/
				}
				break;

			////////////////////////////////////////////////////////////////////////////////////////////////////

			case CMD_HW_UART_ENABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_ENABLE, "CMD_HW_UART_ENABLE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_ENABLE);
				if( hwuxrt_isEnabled() ) {
					if( hwuxrt_isInSyncMode() ) __DPRINTFS_E__("%S - failed: HW-USRT is enabled", SPGM_CMD_HW_UART_ENABLE);
					else                        __DPRINTFS_E__("%S - failed: already enabled"   , SPGM_CMD_HW_UART_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = hwuxrt_isInSyncMode()
					            ? _FSTR("cannot enable HW-UART because HW-USRT is already enabled")
					            : _FSTR("HW-UART is already enabled");
				}
				else {
					if( !initSecondary(GBCID_HW_UART) ) {
						__DPRINTFS_E__("%S - failed: secondary virtual serial port error", SPGM_CMD_HW_UART_ENABLE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FPGM(failInitSInterface);
					}
					else if( !hwuart_begin() ) {
						__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_HW_UART_ENABLE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("failed to initialize HW-UART");
					}
				}
				break;

			case CMD_HW_UART_DISABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_DISABLE, "CMD_HW_UART_DISABLE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_DISABLE);
				if( !hwuxrt_isEnabled() || hwuxrt_isInSyncMode() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_UART_DISABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwuartIsNotEnabled);
				}
				else {
					hwuart_end();
					uninitSecondary(GBCID_HW_UART);
				}
				break;

			case CMD_HW_UART_ENABLE_TX:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_ENABLE_TX, "CMD_HW_UART_ENABLE_TX");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_ENABLE_TX);
				if( !hwuxrt_isEnabled() || hwuxrt_isInSyncMode() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_UART_ENABLE_TX);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwuartIsNotEnabled);
				}
				else {
					hwuxrt_enableTx();
				}
				break;

			case CMD_HW_UART_DISABLE_TX:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_DISABLE_TX, "CMD_HW_UART_DISABLE_TX");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_DISABLE_TX);
				if( !hwuxrt_isEnabled() || hwuxrt_isInSyncMode() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_UART_DISABLE_TX);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwuartIsNotEnabled);
				}
				else {
					hwuxrt_disableTx();
				}
				break;

			case CMD_HW_UART_DISABLE_TX_AFTER:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_UART_DISABLE_TX_AFTER, "CMD_HW_UART_DISABLE_TX_AFTER");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_UART_DISABLE_TX_AFTER);
				if( !hwuxrt_isEnabled() || hwuxrt_isInSyncMode() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_UART_DISABLE_TX_AFTER);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwuartIsNotEnabled);
				}
				else if( !secondaryRdBufIsEmpty() ) {
					__DPRINTFS_E__("%S - failed: buffer not empty", SPGM_CMD_HW_UART_DISABLE_TX_AFTER);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(failRdNESInterface);
				}
				/*
				else if( !secondaryRdBufIsEmpty() || !secondaryWrBufIsEmpty() ) {
					__DPRINTFS_E__("%S - failed: buffer not empty", SPGM_CMD_HW_UART_DISABLE_TX_AFTER);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(failRdNESInterfaceBfNE);
				}
				*/
				else {
					const uint8_t nb = _abuffData[0] & 0xFF;
					__DPRINTFS_I__("%S %d", SPGM_CMD_HW_UART_DISABLE_TX_AFTER, nb);
					hwuxrt_disableTxAfter(nb);
				}
				break;

			////////////////////////////////////////////////////////////////////////////////////////////////////

			case CMD_HW_USRT_ENABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_ENABLE, "CMD_HW_USRT_ENABLE");
				if( hwuxrt_isEnabled() ) {
					if( !hwuxrt_isInSyncMode() ) __DPRINTFS_E__("%S - failed: HW-UART is enabled", SPGM_CMD_HW_USRT_ENABLE);
					else                          __DPRINTFS_E__("%S - failed: already enabled"   , SPGM_CMD_HW_USRT_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = !hwuxrt_isInSyncMode()
					            ? _FSTR("cannot enable HW-USRT because HW-UART is already enabled")
					            : _FSTR("HW-USRT is already enabled");
				}
				else {
					if( !initSecondary(GBCID_HW_USRT) ) {
						__DPRINTFS_E__("%S - failed: secondary virtual serial port initialization error", SPGM_CMD_HW_USRT_ENABLE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FPGM(failInitSInterface);
					}
					else {
						const bool usePDIMode   = (_abuffData[0] & 0x80) != 0;
						const bool ssActiveHigh = (_abuffData[0] & 0x01) != 0;
						__DPRINTFS_I__("%S %d %d", SPGM_CMD_HW_USRT_ENABLE, usePDIMode, ssActiveHigh);
						if( !( usePDIMode ? hwusrt_begin_pdi() : hwusrt_begin() ) ) {
							__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_HW_USRT_ENABLE);
							_wbuffLen   = -1;
							_ebuffPtr_P = _FSTR("failed to initialize HW-USRT");
						}
						_hwusrtSSMode = ssActiveHigh;
					}
				}
				break;

			case CMD_HW_USRT_DISABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_DISABLE, "CMD_HW_USRT_DISABLE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_DISABLE);
				if( !hwuxrt_isEnabled() || !hwuxrt_isInSyncMode() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_USRT_DISABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwusrtIsNotEnabld);
				}
				else {
					if( hwuxrt_ss_isEnabled() ) hwuxrt_ss_end();
					hwusrt_end();
					uninitSecondary(GBCID_HW_USRT);
				}
				break;

			case CMD_HW_USRT_SELECT_SLAVE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_SELECT_SLAVE, "CMD_HW_USRT_SELECT_SLAVE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_SELECT_SLAVE);
				if( bbswim_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: SS is used by BB-SWIM", SPGM_CMD_HW_USRT_SELECT_SLAVE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwusrtNoSupportSS);
				}
				else if( hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: SS is used by HW-SPI", SPGM_CMD_HW_USRT_SELECT_SLAVE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwusrtNoSupportSS);
				}
				else {
					if( !hwuxrt_ss_isEnabled() ) {
						if( !hwuxrt_ss_begin(_hwusrtSSMode) ) {
							__DPRINTFS_E__("%S - failed: SS support initialization error", SPGM_CMD_HW_USRT_SELECT_SLAVE);
							_wbuffLen   = -1;
							_ebuffPtr_P = _FPGM(failInitHWUSRT_SS);
						}
					}
					if( !hwuxrt_ss_selectSlave() ) {
						__DPRINTFS_E__("%S - failed", SPGM_CMD_HW_USRT_SELECT_SLAVE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("HW-USRT failed to deselect slave");
					}
				}
				break;

			case CMD_HW_USRT_DESELECT_SLAVE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_DESELECT_SLAVE, "CMD_HW_USRT_DESELECT_SLAVE");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
				if( bbswim_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: SS is used by BB-SWIM", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwusrtNoSupportSS);
				}
				else if( hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: SS is used by HW-SPI", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwusrtNoSupportSS);
				}
				else {
					if( !hwuxrt_ss_isEnabled() ) {
						if( !hwuxrt_ss_begin(_hwusrtSSMode) ) {
							__DPRINTFS_E__("%S - failed: SS support initialization error", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
							_wbuffLen   = -1;
							_ebuffPtr_P = _FPGM(failInitHWUSRT_SS);
						}
					}
					if( !hwuxrt_ss_deselectSlave() ) {
						__DPRINTFS_E__("%S - failed", SPGM_CMD_HW_USRT_DESELECT_SLAVE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("HW-USRT failed to deselect slave");
					}
				}
				break;

			case CMD_HW_USRT_ENABLE_TX:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_ENABLE_TX, "CMD_HW_USRT_ENABLE_TX");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_ENABLE_TX);
				if( !hwuxrt_isEnabled() || !hwuxrt_isInSyncMode() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_USRT_ENABLE_TX);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwusrtIsNotEnabld);
				}
				else {
					hwuxrt_enableTx();
				}
				break;

			case CMD_HW_USRT_DISABLE_TX:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_DISABLE_TX, "CMD_HW_USRT_DISABLE_TX");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_DISABLE_TX);
				if( !hwuxrt_isEnabled() || !hwuxrt_isInSyncMode() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_USRT_DISABLE_TX);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwusrtIsNotEnabld);
				}
				else {
					hwuxrt_disableTx();
				}
				break;

			case CMD_HW_USRT_DISABLE_TX_AFTER:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_HW_USRT_DISABLE_TX_AFTER, "CMD_HW_USRT_DISABLE_TX_AFTER");
				__DPRINTFS_I__("%S", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER);
				if( !hwuxrt_isEnabled() || !hwuxrt_isInSyncMode() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(hwusrtIsNotEnabld);
				}
				else if( !secondaryRdBufIsEmpty() ) {
					__DPRINTFS_E__("%S - failed: buffer not empty", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(failRdNESInterface);
				}
				/*
				else if( !secondaryRdBufIsEmpty() || !secondaryWrBufIsEmpty() ) {
					__DPRINTFS_E__("%S - failed: buffer not empty", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(failRdNESInterfaceBfNE);
				}
				*/
				else {
					const uint8_t nb = _abuffData[0] & 0xFF;
					__DPRINTFS_I__("%S %d", SPGM_CMD_HW_USRT_DISABLE_TX_AFTER, nb);
					hwuxrt_disableTxAfter(nb);
					/*
					extern uint8_t _hwuxrtDisableTxAfter;
					_wbuffData[0] = _hwuxrtDisableTxAfter;
					_wbuffLen     = 1;
					//*/
				}
				break;

			////////////////////////////////////////////////////////////////////////////////////////////////////

			case CMD_BB_USRT_ENABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_ENABLE, "CMD_BB_USRT_ENABLE");
				if( bbswim_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: BB-SWIM is enabled", SPGM_CMD_BB_USRT_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("cannot enable BB-USRT because BB-SWIM is already enabled");
				}
				else if( hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: HW-SPI is enabled", SPGM_CMD_BB_USRT_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("cannot enable BB-USRT because HW-SPI is already enabled");
				}
				else if( bbusrt_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_BB_USRT_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-USRT is already enabled");
				}
				else {
					const uint32_t baudrate    = ( ( (uint32_t) _abuffData[0] ) << 16 )
					                           | ( ( (uint32_t) _abuffData[1] ) <<  8 )
					                           | ( ( (uint32_t) _abuffData[2] ) <<  0 );
					const uint8_t  parityMode  = (_abuffData[3] >> 4) & 0x03;
					const uint8_t  numStopBits = (_abuffData[3] >> 3) & 0x01;
					const bool     ssMode      = (_abuffData[3] >> 0) & 0x01;
					__DPRINTFS_I__("%S %lu %d %d %d", SPGM_CMD_BB_USRT_ENABLE, baudrate, parityMode, numStopBits, ssMode);
					if( !bbusrt_begin(parityMode, numStopBits + 1, ssMode, baudrate) ) {
						__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_BB_USRT_ENABLE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("failed to initialize BB-USRT");
					}
				}
				break;

			case CMD_BB_USRT_DISABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_DISABLE, "CMD_BB_USRT_DISABLE");
				__DPRINTFS_I__("%S", SPGM_CMD_BB_USRT_DISABLE);
				if( !bbusrt_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_BB_USRT_DISABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-USRT is not enabled");
				}
				else {
					bbusrt_end();
				}
				break;

			case CMD_BB_USRT_SELECT_SLAVE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_SELECT_SLAVE, "CMD_BB_USRT_SELECT_SLAVE");
				__DPRINTFS_I__("%S", SPGM_CMD_BB_USRT_SELECT_SLAVE);
				if( !bbusrt_selectSlave() ) {
					__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_USRT_SELECT_SLAVE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-USRT failed to deselect slave");
				}
				break;

			case CMD_BB_USRT_DESELECT_SLAVE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_DESELECT_SLAVE, "CMD_BB_USRT_DESELECT_SLAVE");
				__DPRINTFS_I__("%S", SPGM_CMD_BB_USRT_DESELECT_SLAVE);
				if( !bbusrt_deselectSlave() ) {
					__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_USRT_DESELECT_SLAVE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-USRT failed to deselect slave");
				}
				break;

			case CMD_BB_USRT_PULSE_XCK:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_PULSE_XCK, "CMD_BB_USRT_PULSE_XCK");
				__DPRINTFS_I__("%S %d %d", SPGM_CMD_BB_USRT_PULSE_XCK, _abuffData[0], _abuffData[1]);
				if( !bbusrt_pulseXck(_abuffData[0], _abuffData[1] != 0) ) {
					__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_USRT_PULSE_XCK);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-USRT failed to pulse the serial clock line");
				}
				break;

			case CMD_BB_USRT_TX:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_TX, "CMD_BB_USRT_TX");
				if(!_dataTransferSize) {
					__DPRINTFS_I__("%S - PHASE 1 [%d]", SPGM_CMD_BB_USRT_TX, _abuffData[0]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
					if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
						__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_BB_USRT_TX, _abuffData[0]);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-USRT the number of bytes is out of the allowed range");
					}
					else {
						// Save the number of bytes
						_dataTransferSize = _abuffData[0];
						// Get more parameter byte(s)
						_cmdPBLTot    = _abuffData[0];
						_cmdPBLIdx    = 0;
						_cmdPBLMillis = millis();
						return;
					}
				}
				else {
					__DPRINTFS_I__("%S - PHASE 2 [%d]", SPGM_CMD_BB_USRT_TX, _dataTransferSize);
					// Perform USRT send
					if( !bbusrt_tx(_abuffData, _dataTransferSize) ) {
						__DPRINTFS_E__("%S - failed: transmit error", SPGM_CMD_BB_USRT_TX);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-USRT transmit failed");
					}
				}
				break;

			case CMD_BB_USRT_RX:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_USRT_RX, "CMD_BB_USRT_RX");
				__DPRINTFS_I__("%S [%d]", SPGM_CMD_BB_USRT_RX, _abuffData[0]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
				if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
					__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_BB_USRT_RX, _abuffData[0]);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-USRT the number of bytes is out of the allowed range");
				}
				else {
					// Save the data transfer size
					_dataTransferSize = _abuffData[0];
					// Perform USRT receive
					if( !bbusrt_rx(_abuffData, _dataTransferSize) ) {
						__DPRINTFS_E__("%S - failed: receive error", SPGM_CMD_BB_USRT_RX);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-USRT receive failed");
					}
					// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
					else {
						_wbuffLenArg = _dataTransferSize;
					}
					/*
					// If there is no error, send back the response
					else {
						_wbuffLen = _dataTransferSize;
						memcpy(_wbuffData, _abuffData, _dataTransferSize);
					}
					*/
				}
				break;

			////////////////////////////////////////////////////////////////////////////////////////////////////

			case CMD_BB_SWIM_ENABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_ENABLE, "CMD_BB_SWIM_ENABLE");
				if( hwuxrt_ss_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: BB-USRT is enabled or HW-USRT has enabled SS support", SPGM_CMD_BB_SWIM_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("cannot enable BB-SWIM because BB-USRT is already enabled or HW-USRT has enabled slave selection support");
				}
				else if( hwspi_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: HW-SPI is enabled", SPGM_CMD_BB_SWIM_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("cannot enable BB-SWIM because HW-SPI is already enabled");
				}
				else if( bbswim_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: already enabled", SPGM_CMD_BB_SWIM_ENABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-SWIM is already enabled");
				}
				else {
					__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_ENABLE);
					if( !bbswim_begin() ) {
						__DPRINTFS_E__("%S - failed: initialization error", SPGM_CMD_BB_SWIM_ENABLE);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("failed to initialize BB-SWIM");
					}
				}
				break;

			case CMD_BB_SWIM_DISABLE:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_DISABLE, "CMD_BB_SWIM_DISABLE");
				__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_DISABLE);
				if( !bbswim_isEnabled() ) {
					__DPRINTFS_E__("%S - failed: not enabled", SPGM_CMD_BB_SWIM_DISABLE);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FPGM(bbswimIsNotEnabled);
				}
				else {
					bbswim_end();
				}
				break;

			case CMD_BB_SWIM_LINE_RESET:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_LINE_RESET, "CMD_BB_SWIM_LINE_RESET");
				__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_LINE_RESET);
				if( !bbswim_lineReset() ) {
					__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_SWIM_LINE_RESET);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-SWIM failed to line reset");
				}
				break;

			case CMD_BB_SWIM_TRANSFER:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_TRANSFER, "CMD_BB_SWIM_TRANSFER");
				if(!_dataTransferSize) {
					__DPRINTFS_I__("%S - PHASE 1 [%d] [%d]", SPGM_CMD_BB_SWIM_TRANSFER, _abuffData[0], _abuffData[1]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
					if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
						__DPRINTFS_E__("%S - failed: the number of bits (%d) is out of range", SPGM_CMD_BB_SWIM_TRANSFER, _abuffData[0]);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-SWIM the number of bits is out of the allowed range");
					}
					else if(_abuffData[1] >= _abuffData[0]) {
						__DPRINTFS_E__("%S - failed: the number of 2X bits (%d) is out of range", SPGM_CMD_BB_SWIM_TRANSFER, _abuffData[1]);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-SWIM the number of 2X bits is out of the allowed range");
					}
					else {
						// Save the number of bits
						_dataTransferSize  = _abuffData[0];
						_dataTransferSize2 = _abuffData[1];
						// Get more parameter byte(s)
						_cmdPBLTot    = _abuffData[0];
						_cmdPBLIdx    = 0;
						_cmdPBLMillis = millis();
						return;
					}
				}
				else {
					__DPRINTFS_I__("%S - PHASE 2 [%d] [%d]", SPGM_CMD_BB_SWIM_TRANSFER, _dataTransferSize, _dataTransferSize2);
					// Perform SWIM transfer
					if( !bbswim_transfer(_abuffData, _dataTransferSize, _dataTransferSize2) ) {
						__DPRINTFS_E__("%S - failed: transfer error", SPGM_CMD_BB_SWIM_TRANSFER);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-SWIM transfer failed");
					}
					// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
					else {
						_wbuffLenArg = _dataTransferSize;
					}
				}
				break;

			case CMD_BB_SWIM_SRST:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_SRST, "CMD_BB_SWIM_SRST");
				__DPRINTFS_I__("%S", SPGM_CMD_BB_SWIM_SRST);
				if( !bbswim_cmd_srst() ) {
					__DPRINTFS_E__("%S - failed", SPGM_CMD_BB_SWIM_SRST);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-SWIM command SRST failed");
				}
				break;

			case CMD_BB_SWIM_ROTF:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_ROTF, "CMD_BB_SWIM_ROTF");
				__DPRINTFS_I__("%S [%d] %02X %02X %02X", SPGM_CMD_BB_SWIM_ROTF, _abuffData[0], _abuffData[1], _abuffData[2], _abuffData[3]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
					if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
					__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_BB_SWIM_ROTF, _abuffData[0]);
					_wbuffLen   = -1;
					_ebuffPtr_P = _FSTR("BB-SWIM the number of bytes is out of the allowed range");
				}
				else {
					// Save the address and number of bytes
					_dataTransferSize = _abuffData[0];
					_swimEAddress     = _abuffData[1];
					_swimHAddress     = _abuffData[2];
					_swimLAddress     = _abuffData[3];
					// Perform SWIM command ROTF
					if( !bbswim_cmd_rotf(_abuffData, _dataTransferSize, _swimEAddress, _swimHAddress, _swimLAddress) ) {
						__DPRINTFS_E__("%S - failed: receive error", SPGM_CMD_BB_SWIM_ROTF);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-SWIM command ROTF failed");
					}
					// If there is no error, send back the response via '_abuffData' and '_wbuffLenArg'
					else {
						_wbuffLenArg = _dataTransferSize;
					}
				}
				break;

			case CMD_BB_SWIM_WOTF:
				__DPRINTF_DECL_SPGMVR__(SPGM_CMD_BB_SWIM_WOTF, "CMD_BB_SWIM_WOTF");
				if(!_dataTransferSize) {
					__DPRINTFS_I__("%S - PHASE 1 [%d] %02X %02X %02X", SPGM_CMD_BB_SWIM_WOTF, _abuffData[0], _abuffData[1], _abuffData[2], _abuffData[3]);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
					if( !_abuffData[0] || ( (_SInt_IZArg_t) _abuffData[0] ) > sizeof(_abuffData) ) {
#pragma GCC diagnostic pop
						__DPRINTFF_E__(xdstrNByOutOfRange, SPGM_CMD_BB_SWIM_WOTF, _abuffData[0]);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-SWIM the number of bytes is out of the allowed range");
					}
					else {
						// Save the address and number of bytes
						_dataTransferSize = _abuffData[0];
						_swimEAddress     = _abuffData[1];
						_swimHAddress     = _abuffData[2];
						_swimLAddress     = _abuffData[3];
						// Get more parameter byte(s)
						_cmdPBLTot    = _abuffData[0];
						_cmdPBLIdx    = 0;
						_cmdPBLMillis = millis();
						return;
					}
				}
				else {
					__DPRINTFS_I__("%S - PHASE 2 [%d] %02X %02X %02X", SPGM_CMD_BB_SWIM_WOTF, _dataTransferSize, _swimEAddress, _swimHAddress, _swimLAddress);
					// Perform SWIM command WOTF
					if( !bbswim_cmd_wotf(_abuffData, _dataTransferSize, _swimEAddress, _swimHAddress, _swimLAddress) ) {
						__DPRINTFS_E__("%S - failed: write error", SPGM_CMD_BB_SWIM_WOTF);
						_wbuffLen   = -1;
						_ebuffPtr_P = _FSTR("BB-SWIM command WOTF failed");
					}
				}
				break;

			////////////////////////////////////////////////////////////////////////////////////////////////////

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

