/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#define __MODULES__HW_GPIO_C__


#include <stdbool.h>
#include <stdint.h>

#include <avr/io.h>
#include <avr/pgmspace.h>

#include "../Protocol/Protocol.h"
#include "HW_GPIO.h"
#include "Utils.h"


void hwgpio_init(void)
{
	// Initialize PWM timer
	PWM_CTRL_INIT();

	// Initialize ADC
	ADCA.EVCTRL    = 0;                                                     // Disable event system
	ADCA.INTFLAGS  = 0;                                                     // Disable interrupt
	ADCA.REFCTRL   = ADC_REFSEL_INT1V_gc | ADC_BANDGAP_bm | ADC_TEMPREF_bm; // Internal 1.0V reference, enable bandgap, enable internal temperature sensor
#if F_CPU >= 64000000
	ADCA.PRESCALER = ADC_PRESCALER_DIV512_gc;                               // 64MHz / 512 = 125.0kHz
#elif F_CPU >= 48000000
	ADCA.PRESCALER = ADC_PRESCALER_DIV256_gc;                               // 32MHz / 256 = 187.5kHz
#elif F_CPU >= 32000000
	ADCA.PRESCALER = ADC_PRESCALER_DIV256_gc;                               // 32MHz / 256 = 125.0kHz
#elif F_CPU >= 16000000
	ADCA.PRESCALER = ADC_PRESCALER_DIV128_gc;                               // 16MHz / 128 = 125.0kHz
#elif F_CPU >= 8000000
	ADCA.PRESCALER = ADC_PRESCALER_DIV64_gc;                                //  8MHz /  64 = 125.0kHz
#elif F_CPU >= 4000000
	ADCA.PRESCALER = ADC_PRESCALER_DIV32_gc;                                //  4MHz /  32 = 125.0kHz
#elif F_CPU >= 2000000
	ADCA.PRESCALER = ADC_PRESCALER_DIV16_gc;                                //  2MHz /  16 = 125.0kHz
#elif F_CPU >= 1000000
	ADCA.PRESCALER = ADC_PRESCALER_DIV8_gc;                                 //  1MHz /   8 = 125.0kHz
#else
	ADCA.PRESCALER = ADC_PRESCALER_DIV4_gc;                                 // Lowest frequency
#endif
	BEG_READ_PRODROW();
	ADCA.CALL      = READ_PRODROW_VAL(ADCACAL0);                            // Set the calibration value
	ADCA.CALH      = READ_PRODROW_VAL(ADCACAL1);                            // ---
	END_READ_PRODROW();
	ADCA.CTRLB     = ADC_CURRLIMIT_HIGH_gc | ADC_RESOLUTION_12BIT_gc;       // High impedance, high current limit, unsigned mode, single-conversion mode, 12-bit resolution (right adjusted)
	ADCA.CTRLA     = ADC_DMASEL_OFF_gc | ADC_ENABLE_bm;                     // Enable ADC without DMA
	ADCA.CH0.CTRL  = ADC_CH_GAIN_1X_gc | ADC_CH_INPUTMODE_SINGLEENDED_gc;   // No gain, single-ended positive input signal
}


static void _hwgpio_setMode_impl(uint8_t gpioNumber, uint8_t modeBits, uint8_t initVal)
{
	const uint8_t ctrlBit = _portNumberToCTRLBit(gpioNumber);
	const uint8_t bit     = _portNumberToBitMask(gpioNumber);

	if(ctrlBit) { // Turn off PWM as needed
		PWM_CTRL &= ~ctrlBit;
		*_portNumberToDIRCLR_PWM(gpioNumber) = bit;
	}

	switch(modeBits) {

		case HW_GPIO_SET_MODE_INP:
			*_portNumberToDIRCLR(gpioNumber)  = bit;
			*_portNumberToPINCTL(gpioNumber) &= ~PORT_OPC_gm;
			break;

		case HW_GPIO_SET_MODE_INP_PU:
			*_portNumberToDIRCLR(gpioNumber)  = bit;
			*_portNumberToPINCTL(gpioNumber) |= PORT_OPC_PULLUP_gc;
			break;

		case HW_GPIO_SET_MODE_OUT:
			            *_portNumberToDIRSET(gpioNumber) = bit;
			if(initVal) *_portNumberToOUTSET(gpioNumber) = bit;
			else        *_portNumberToOUTCLR(gpioNumber) = bit;
			break;

		case HW_GPIO_SET_MODE_NO_CHG:
			break;

	} // switch
}

void hwgpio_setMode(uint8_t modeBits74, uint8_t modeBits30, uint8_t initVals70)
{
	uint8_t bit = 0b10000000;

	for(int8_t i = 7; i >= 0; --i) {
		const uint8_t highNibble = (i >= 4);
		const uint8_t modeBits   = highNibble ? modeBits74 : modeBits30;
		const uint8_t shiftSize  = highNibble ? (i - 4) : i;
		_hwgpio_setMode_impl( i, ( modeBits >> (shiftSize * 2) ) & 0x03, initVals70 & bit );
		bit >>= 1;
	}
}


static void _hwgpio_setValue_impl(uint8_t gpioNumber, uint8_t val)
{
	const    uint8_t  ctrlBit = _portNumberToCTRLBit(gpioNumber);
	volatile uint8_t* dir     = _portNumberToDIR    (gpioNumber);
	volatile uint8_t* dirPWM  = _portNumberToDIR_PWM(gpioNumber);
	const    uint8_t  bit     = _portNumberToBitMask(gpioNumber);

	if( !(*dir & bit) && !(*dirPWM & bit) ) return; // Exit if the GPIO is not in output mode

	if(ctrlBit) { // Turn off PWM and turn on digital I/O as needed
		PWM_CTRL &= ~ctrlBit;
		*_portNumberToDIRCLR_PWM(gpioNumber) = bit;
		*_portNumberToDIRSET    (gpioNumber) = bit;
	}

	if(val) *_portNumberToOUTSET(gpioNumber) = bit;
	else    *_portNumberToOUTCLR(gpioNumber) = bit;
}


void hwgpio_setValues(uint8_t mask70, uint8_t vals70)
{
	uint8_t bit = 0b10000000;

	for(int8_t i = 7; i >= 0; --i) {
		if(mask70 & bit) _hwgpio_setValue_impl(i, vals70 & bit);
		bit >>= 1;
	}
}


static bool _hwgpio_getValue_impl(uint8_t gpioNumber)
{
	const    uint8_t  ctrlBit = _portNumberToCTRLBit(gpioNumber);
	volatile uint8_t* dir     = _portNumberToDIR    (gpioNumber);
	volatile uint8_t* dirPWM  = _portNumberToDIR_PWM(gpioNumber);
	volatile uint8_t* in      = _portNumberToIN     (gpioNumber);
	const    uint8_t  bit     = _portNumberToBitMask(gpioNumber);

	if( (*dir & bit) || (*dirPWM & bit) ) return false; // Exit if the GPIO is not in input mode

	if(ctrlBit) { // Turn off PWM and turn on digital I/O as needed
		PWM_CTRL &= ~ctrlBit;
		*_portNumberToDIRCLR_PWM(gpioNumber) = bit;
		*_portNumberToDIRSET    (gpioNumber) = bit;
	}

	// ##### ??? TODO : Implement debouncing ??? #####
	return (*in & bit) != 0;
}

uint8_t hwgpio_getValues(void)
{
	uint8_t mask70 = 0;
	uint8_t bit    = 0b10000000;

	for(int8_t i = 7; i >= 0; --i) {
		if( _hwgpio_getValue_impl(i) ) mask70 |= bit;
		bit >>= 1;
	}

	return mask70;
}


void hwgpio_setPWM(uint8_t gpioNumber, uint8_t val70)
{
	const    uint8_t  ctrlBit = _portNumberToCTRLBit(gpioNumber);
	volatile uint8_t* dir     = _portNumberToDIR    (gpioNumber);
	volatile uint8_t* dirPWM  = _portNumberToDIR_PWM(gpioNumber);
	const    uint8_t  bit     = _portNumberToBitMask(gpioNumber);

	if( !ctrlBit ) return; // Exit if the GPIO does not support PWM
	if( !(*dir & bit) && !(*dirPWM & bit) ) return; // Exit if the GPIO is not in output mode

	// Turn off digital I/O and turn on PWM
	*_portNumberToDIRCLR    (gpioNumber) = bit;
	*_portNumberToDIRSET_PWM(gpioNumber) = bit;

	// Set the duty cycle an turn on PWM
	*_portNumberToCCX(gpioNumber) = val70;
	PWM_CTRL |= ctrlBit;
}


static const uint16_t ADC_VOFFSET = 190; // ΔV

uint16_t _hwgpio_readADC_impl(uint8_t channel)
{
	ADCA.CH0.MUXCTRL  = channel;
	ADCA.CH0.CTRL    |= ADC_CH_START_bm;

	while( !(ADCA.CH0.INTFLAGS & ADC_CH0IF_bm) );

	uint16_t res = ADCA.CH0.RES;
	res = (res > ADC_VOFFSET) ? (res - ADC_VOFFSET) : 0;

	ADCA.CH0.INTFLAGS = ADC_CH0IF_bm;

	return res;
}

uint16_t hwgpio_getADC(uint8_t gpioNumber)
{
	// Read normal ADC channel
	volatile uint8_t* dir    = _portNumberToDIR       (gpioNumber);
	volatile uint8_t* dirPWM = _portNumberToDIR_PWM   (gpioNumber);
	const    uint8_t  bit    = _portNumberToBitMask   (gpioNumber);
	const    uint8_t  adcc   = _portNumberToADCChannel(gpioNumber);

	if( adcc == 0xFF ) return 0; // Exit if the GPIO does not support ADC
	if( (*dir & bit) || (*dirPWM & bit) ) return 0; // Exit if the GPIO is not in input mode

	return _hwgpio_readADC_impl(adcc);
}


// Please refer to '../../../KiCad8/USB_GPIO_Module/PB0_ADC8_VREAD.txt' for more details
static       uint32_t VREAD_CALIB_VAL = 0xFFFFFFFFUL;
static const uint32_t VREAD_CALIB_DEF = 145920UL;
static const uint32_t VREAD_CALIB_MIN = VREAD_CALIB_DEF - VREAD_CALIB_DEF / 10UL;
static const uint32_t VREAD_CALIB_MAX = VREAD_CALIB_DEF + VREAD_CALIB_DEF / 10UL;

uint16_t _hwgpio_getVREAD_impl(void)
{
	IO_SETMODE_INP(ADC_VREAD_PORT, ADC_VREAD_BIT);

	return _hwgpio_readADC_impl(ADC_VREAD_CHANNEL);
}

uint16_t hwgpio_getVREAD(void)
{
	if(VREAD_CALIB_VAL == 0xFFFFFFFFUL) {
		VREAD_CALIB_VAL = loadConfigU32(CFGU32_IDX_VREAD);
		if(VREAD_CALIB_VAL < VREAD_CALIB_MIN || VREAD_CALIB_VAL > VREAD_CALIB_MAX ) VREAD_CALIB_VAL = VREAD_CALIB_DEF;
	}

	return ( ( _hwgpio_getVREAD_impl() * VREAD_CALIB_VAL ) + 524288UL ) >> 20;
}

bool hwgpio_calibrateVREAD(uint16_t expectedValue_)
{
	const uint32_t expectedValue = expectedValue_;
	      uint32_t calibValue    = 0;

	for(int i = 0; i < 8; ++i) {
		calibValue += ( (expectedValue << 20) - 524288UL ) / _hwgpio_getVREAD_impl();
	}

	calibValue = (calibValue + 4) / 8;

	if(calibValue < VREAD_CALIB_MIN || calibValue > VREAD_CALIB_MAX ) return false;

	VREAD_CALIB_VAL = calibValue;
	saveConfigU32(CFGU32_IDX_VREAD, VREAD_CALIB_VAL);

	return true;
}


uint16_t hwgpio_getInternalTemperature(void)
{
	ADCA.CH0.CTRL = ADC_CH_GAIN_1X_gc | ADC_CH_INPUTMODE_INTERNAL_gc;

	const uint16_t res = _hwgpio_readADC_impl(ADC_CH_MUXINT_TEMP_gc);

	ADCA.CH0.CTRL = ADC_CH_GAIN_1X_gc | ADC_CH_INPUTMODE_SINGLEENDED_gc;

	return res;
}
