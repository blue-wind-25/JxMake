/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>

#include <LUFA/Drivers/USB/USB.h>

#include "../ActivityLED.h"
#include "../EEPROM/eeprom_driver.h"
#include "Descriptors.h"
#include "Utils.h"


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


static const    uint16_t _wdtEnKey = 0xAAAA;
static volatile uint16_t _wdtEnKeyVal ATTR_NO_INIT;


void wdtEnSetKey(void)
{ _wdtEnKeyVal = _wdtEnKey; }


void wdtEnClrKey(void)
{ _wdtEnKeyVal = 0;  }


bool wdtEnIsKeySet(void)
{ return _wdtEnKeyVal == _wdtEnKey; }


////////////////////////////////////////////////////////////////////////////////////////////////////


void _reset_impl(bool toBL)
{
#if !!ENABLE_DEBUG_STREAM
	debug_printf(DMSG_TYPE_BIT_ALWAYS, 0);
#endif

	USB_Disable();

	_delay_ms(100);
	for(int i = 0; i < 5; ++i) {
		ALED_ON (); _delay_ms(100);
		ALED_OFF(); _delay_ms(100);
	}
	_delay_ms(100);

	GlobalInterruptDisable();

	if(toBL) {
/*
#if defined(BL_QUARK_ONE)
		#define BOOTLOADER_SPICE_EEPROM_PAGE 0x00
		#define BOOTLOADER_SPICE_EEPROM_BYTE 0x00
		#define BOOTLOADER_SPICE_VALUE       0x66
		writeEEPROMU08(BOOTLOADER_SPICE_EEPROM_PAGE, BOOTLOADER_SPICE_EEPROM_BYTE, BOOTLOADER_SPICE_VALUE);
#elif defined(BL_ATMEL_DFU)
*/
		// Jump to (BOOT_SECTION_START + 0x00FE => 0x0100FE)
		__asm__ __volatile__ (
			"ldi   r30 , 0xFE \n\t" // Z    = 0x00FE
			"ldi   r31 , 0x00 \n\t" // ---
			"ldi   r25 , 0x01 \n\t" // EIND = 0x01
			"out   0x3C, r25  \n\t" // ---
			"eijmp            \n\t" // Perform extended indirect jump
			::: "r25", "r30", "r31"
		);
		for(;;);
/*
#endif
*/
	}

	_PROTECTED_WRITE(RST.CTRL, RST_SWRST_bm);
	for(;;);
}


void resetSystem(void)
{ _reset_impl(false); }


void resetToBootloader(void)
{ _reset_impl(true); }


////////////////////////////////////////////////////////////////////////////////////////////////////


void waitUntil_Endpoint_IsINReady(USB_ClassInfo_CDC_Device_t* const dev)
{
	// Select the endpoint address
	Endpoint_SelectEndpoint(dev->Config.DataINEndpoint.Address);

	// Wait until the selected IN endpoint is ready for a new packet to be sent to the host
	while( !Endpoint_IsINReady() ) CDC_Device_USBTask(dev);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


uint8_t _llt_enaCnt_XCK_nSS_SCK = 0;

bool    _llt_enaFlg_XCK         = false;
bool    _llt_enaFlg_nSS         = false;
bool    _llt_enaFlg_SCK         = false;


////////////////////////////////////////////////////////////////////////////////////////////////////


void writeEEPROMU08(uint8_t pageAddr, uint8_t byteAddr, uint8_t value)
{
	EEPROM_EnableMapping();
	EEPROM_WaitForNVM();

	EEPROM(pageAddr, byteAddr) = value;
	EEPROM_AtomicWritePage(pageAddr);
}


uint32_t loadConfigU32(uint8_t idx)
{
	EEPROM_EnableMapping();
	EEPROM_WaitForNVM();

	const uint32_t v3 = EEPROM(CFGU32_EEPROM_PAGE, idx * 4 + 0);
	const uint32_t v2 = EEPROM(CFGU32_EEPROM_PAGE, idx * 4 + 1);
	const uint32_t v1 = EEPROM(CFGU32_EEPROM_PAGE, idx * 4 + 2);
	const uint32_t v0 = EEPROM(CFGU32_EEPROM_PAGE, idx * 4 + 3);

	return (v3 << 24) | (v2 << 16) | (v1 << 8) | v0;
}


void saveConfigU32(uint8_t idx, uint32_t value)
{
	EEPROM_EnableMapping();
	EEPROM_WaitForNVM();

	EEPROM(CFGU32_EEPROM_PAGE, idx * 4 + 0) = (value >> 24) & 0xFF;
	EEPROM(CFGU32_EEPROM_PAGE, idx * 4 + 1) = (value >> 16) & 0xFF;
	EEPROM(CFGU32_EEPROM_PAGE, idx * 4 + 2) = (value >>  8) & 0xFF;
	EEPROM(CFGU32_EEPROM_PAGE, idx * 4 + 3) = (value      ) & 0xFF;

	EEPROM_AtomicWritePage(CFGU32_EEPROM_PAGE);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


#if 0
// gcc -Wall -Wextra -std=c99 -lm brt.c -o brt && ./brt && rm brt
#define TEST_CALC_BSCALE_BSEL
#include <stdbool.h>
#include <stdio.h>
#include <stdint.h>
#define F_CPU 32000000UL
typedef struct _XUARTConfig {
	bool    CLK2X;
	uint8_t BAUDCTRLB;
	uint8_t BAUDCTRLA;
} XUARTConfig;
#endif

XUARTConfig calc_BSCALE_BSEL(uint32_t baudrate)
{
	// 1111        1100        00000000
	// 5432        1098        76543210
	// BSCALE[3:0] BSEL[11:08] BSEL[07:00]
	// -------BAUDCTRLB------- -BAUDCTRLA-

	// Scaling factor for fixed-point arithmetic
	const uint32_t FP_SCALE = 1024UL;

	// Find the best BSCALE and BSEL
	bool     sel2XMode = false;
	int8_t   selBSCALE = 0;
	uint32_t selBSEL   = 0UL;
	uint32_t selErr    = F_CPU;

	for(int8_t bscale = -7; bscale <= 7; ++bscale) { // BSCALE is from -7 to +7

		for(uint32_t mf = 1; mf <= 2; ++mf) { // 2X mode and 1X mode

			// Calculate BSEL and the actual baudrate
			uint32_t bsel, baud;

			if(bscale >= 0) {
				bsel = ( ( (F_CPU * FP_SCALE) >> bscale ) / 8UL / mf / baudrate - FP_SCALE  ) / FP_SCALE;
				if(bsel > 4095UL) continue;
				baud =   ( (F_CPU * FP_SCALE) >> bscale ) / 8UL / mf / (bsel * FP_SCALE + FP_SCALE);
			}
			else {
				bsel = ( F_CPU * (FP_SCALE / 8UL / mf) / baudrate - FP_SCALE ) / ( FP_SCALE >> (-bscale) );
				if(bsel > 4095UL) continue;
				baud =   F_CPU * (FP_SCALE / 8UL / mf) / ( ( (bsel * FP_SCALE) >> (-bscale) ) + FP_SCALE );
			}

			// Check if these settings yield a better (smaller) error
			const uint32_t err = (baudrate >= baud) ? (baudrate - baud) : (baud - baudrate);

			if(err < selErr) {
				sel2XMode = (mf == 1);
				selBSCALE = bscale;
				selBSEL   = bsel;
				selErr    = err;
				if(selErr == 0) break; // Exit if the perfect settings are found
			}

		} // for

		// Exit if the perfect settings are found
		if(selErr == 0) break;

	} // for

#ifdef TEST_CALC_BSCALE_BSEL
	printf("baudrate = %7u [%d %+2d | %4u] ERR = %f\n", baudrate, sel2XMode, selBSCALE, selBSEL, (float) selErr / baudrate * 100.0f);
#endif

	// Generate and return the result
	const XUARTConfig cfg = {
		.CLK2X     = sel2XMode                                                  ,
		.BAUDCTRLB = ( (selBSCALE & 0x000F) << 4 ) | ( (selBSEL & 0x0F00) >> 8 ),
		.BAUDCTRLA =                                   (selBSEL & 0x00FF)
	};

	return cfg;
}

#ifdef TEST_CALC_BSCALE_BSEL
#define _c calc_BSCALE_BSEL
int main(void)
{
	_c(2000000); _c(1500000); _c(1000000); _c( 921600); _c( 460800); _c( 230400);
	_c( 115200); _c(  74880); _c(  64000); _c(  57600); _c(  50000); _c(  38400);
	_c(  32000); _c(  28800); _c(  19200); _c(  16000); _c(  14400); _c(   9600);
	_c(   8000); _c(   7200); _c(   4800); _c(   4000); _c(   3600); _c(   2400);
	_c(   2000); _c(   1800); _c(   1200); _c(   1000); _c(    900); _c(    800);
	_c(    700); _c(    600); _c(    500); _c(    450); _c(    400); _c(    300);
	_c(    225); _c(    150); _c(    120); _c(    100); _c(     75); _c(     60);
	_c(     50); _c(     40); _c(     30); _c(     20); _c(     10); _c(      8);
	return 0;
}
#endif


////////////////////////////////////////////////////////////////////////////////////////////////////


#if !!ENABLE_DEBUG_STREAM

static FILE*    _debug_stream      = 0;
static uint8_t  _dmsgTypeMask      = 0x00;

static uint16_t _dmsgDMABufferWPos = 0;
static char     _dmsgDMABuffer[DEBUG_UART_DMA_BUFFER_SIZE];


static void _debugFlushDMA(void)
{
	// Wait until previous DMA transfer is complete
	while(DEBUG_UART_DMA.CTRLB & DMA_CH_CHBUSY_bm);

	// Check if there is nothing to send
	if(!_dmsgDMABufferWPos) return;

	// Set transfer count and reset the write position
	DEBUG_UART_DMA.TRFCNT = _dmsgDMABufferWPos;
	_dmsgDMABufferWPos    = 0;

	// Enable the DMA channel to start the transfer
	DEBUG_UART_DMA.CTRLA |= DMA_CH_ENABLE_bm;
}


static int _debugPutChar(char ch, FILE* stream)
{
	if(_dmsgDMABufferWPos >= DEBUG_UART_DMA_BUFFER_SIZE) _debugFlushDMA();

	_dmsgDMABuffer[_dmsgDMABufferWPos++] = ch;

	return 0;
}


void debug_initStream(void)
{
	// Initialize the debugging UART
	const uint16_t baudSetting = UXRT_BSEL_1X(DEBUG_UART_BAUD);

	DEBUG_UART.BAUDCTRLB = (baudSetting >> 8   );
	DEBUG_UART.BAUDCTRLA = (baudSetting &  0xFF);

	DEBUG_UART.CTRLA     = 0;
	DEBUG_UART.CTRLC     = USART_CMODE_ASYNCHRONOUS_gc | USART_PMODE_DISABLED_gc | USART_CHSIZE_8BIT_gc;
	DEBUG_UART.CTRLB     = USART_TXEN_bm;

	IO_SETMODE_INP(DEBUG_UART_PORT, DEBUG_UART_RX_BIT);
	IO_SETMODE_OUT(DEBUG_UART_PORT, DEBUG_UART_TX_BIT);

	// Reset the DMA channel
	DEBUG_UART_DMA.CTRLA  = DMA_CH_RESET_bm;
	while(DEBUG_UART_DMA.CTRLA & DMA_CH_RESET_bm);

	// Initalize the DMA channel
	DEBUG_UART_DMA.CTRLA    = DMA_CH_SINGLE_bm | DMA_CH_BURSTLEN_1BYTE_gc;
	DEBUG_UART_DMA.CTRLB    = 0;
	DEBUG_UART_DMA.ADDRCTRL = DMA_CH_SRCRELOAD_TRANSACTION_gc | DMA_CH_SRCDIR_INC_gc | DMA_CH_DESTRELOAD_NONE_gc | DMA_CH_DESTDIR_FIXED_gc;
	DEBUG_UART_DMA.TRIGSRC  = DEBUG_UART_DMA_TRIG;

	DEBUG_UART_DMA.SRCADDR0  = ( ( (uint16_t)  _dmsgDMABuffer  ) >> 0 ) & 0xFF;
	DEBUG_UART_DMA.SRCADDR1  = ( ( (uint16_t)  _dmsgDMABuffer  ) >> 8 ) & 0xFF;
	DEBUG_UART_DMA.SRCADDR2  = 0;

	DEBUG_UART_DMA.DESTADDR0 = ( ( (uint16_t) &DEBUG_UART.DATA ) >> 0 ) & 0xFF;
	DEBUG_UART_DMA.DESTADDR1 = ( ( (uint16_t) &DEBUG_UART.DATA ) >> 8 ) & 0xFF;
	DEBUG_UART_DMA.DESTADDR2 = 0;

	// Initialize the debugging stream
	static FILE stream = FDEV_SETUP_STREAM(_debugPutChar, 0, _FDEV_SETUP_RW);

	_debug_stream = &stream;
}


void debug_setMsgTypeMask(uint8_t dmsgTypeMask)
{ _dmsgTypeMask = dmsgTypeMask; }


void debug_printf(uint8_t dmsgTypeBit, __flash_string format, ...)
{
	// NOTE : Use DMA so this function does not have to wait until all bytes are transmitted by the UART
	//        (unless, of course, it is called repeatedly in short intervals)

	if(_debug_stream == 0) return;

	if( (dmsgTypeBit != DMSG_TYPE_BIT_ALWAYS) && !(_dmsgTypeMask & dmsgTypeBit) ) return;

	_debugFlushDMA();

	if(!format) return;

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
		vfprintf_P( _debug_stream, _CCCF(format), argptr );
	va_end(argptr);

	_debugFlushDMA();
}

#endif
