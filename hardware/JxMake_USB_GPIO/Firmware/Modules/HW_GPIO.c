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


void hwgpio_init(void)
{
	// Initialize PWM timer
	PWM_TCCR_INIT();

	// Set the ADC prescaler to get the desired range of 50kHz - 200kHz
	#if F_CPU >= 16000000
		ADCSRA = _BV(ADPS2) | _BV(ADPS1) | _BV(ADPS0); //  16MHz / 128 = 125kHz
	#elif F_CPU >= 8000000
		ADCSRA = _BV(ADPS2) | _BV(ADPS1)             ; //   8MHz /  64 = 125kHz
	#elif F_CPU >= 4000000
		ADCSRA = _BV(ADPS2) |              _BV(ADPS0); //   4MHz /  32 = 125kHz
	#elif F_CPU >= 2000000
		ADCSRA = _BV(ADPS2)                          ; //   2MHz /  16 = 125kHz
	#elif F_CPU >= 1000000
		ADCSRA =              _BV(ADPS1) | _BV(ADPS0); //   1MHz /   8 = 125kHz
	#else
		ADCSRA =                           _BV(ADPS0); // 128kHz /   2 =  64kHz
	#endif

	// Enable ADC conversions
	ADCSRA |= _BV(ADEN);
}


static void _hwgpio_setMode_impl(uint8_t gpioNumber, uint8_t modeBits, uint8_t initVal)
{
	volatile uint8_t* ddr  = _portNumberToDDR    (gpioNumber);
	volatile uint8_t* port = _portNumberToPORT   (gpioNumber);
	const    uint8_t  bit  = _portNumberToBitMask(gpioNumber);

	switch(modeBits) {

		case HW_GPIO_SET_MODE_INP:
			*ddr  &= ~bit;
			*port &= ~bit;
			break;

		case HW_GPIO_SET_MODE_INP_PU:
			*ddr  &= ~bit;
			*port |=  bit;
			break;

		case HW_GPIO_SET_MODE_OUT:
			            *ddr  |=  bit;
			if(initVal) *port |=  bit;
			else        *port &= ~bit;
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
	volatile uint8_t  tccrBits = _portNumberToTCCRBits(gpioNumber);
	volatile uint8_t* ddr      = _portNumberToDDR     (gpioNumber);
	volatile uint8_t* port     = _portNumberToPORT    (gpioNumber);
	const    uint8_t  bit      = _portNumberToBitMask (gpioNumber);

	if( !(*ddr & bit) ) return; // Exit if the GPIO is not in output mode

	if(tccrBits) PWM_TCCR &= ~tccrBits; // Turn off PWM as needed

	if(val) *port |=  bit;
	else    *port &= ~bit;
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
	volatile uint8_t  tccrBits = _portNumberToTCCRBits(gpioNumber);
	volatile uint8_t* ddr      = _portNumberToDDR     (gpioNumber);
	volatile uint8_t* pin      = _portNumberToPIN     (gpioNumber);
	const    uint8_t  bit      = _portNumberToBitMask (gpioNumber);

	if( (*ddr & bit) ) return false; // Exit if the GPIO is not in input mode

	if(tccrBits) PWM_TCCR &= ~tccrBits; // Turn off PWM as needed

	// ##### ??? TODO : Implement debouncing ??? #####
	return (*pin & bit) != 0;
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
	volatile uint8_t  tccrBits = _portNumberToTCCRBits(gpioNumber);
	volatile uint8_t* ocr      = _portNumberToOCR     (gpioNumber);
	volatile uint8_t* ddr      = _portNumberToDDR     (gpioNumber);
	const    uint8_t  bit      = _portNumberToBitMask (gpioNumber);

	if( !(*ddr & bit) ) return; // Exit if the GPIO is not in output mode
	if( !tccrBits     ) return; // Exit if the GPIO does not support PWM

	*ocr      =  val70;   // Set duty cycle
	PWM_TCCR |= tccrBits; // Turn on PWM
}


uint16_t hwgpio_getADC(uint8_t gpioNumber)
{
	volatile uint8_t* ddr  = _portNumberToDDR       (gpioNumber);
	const    uint8_t  bit  = _portNumberToBitMask   (gpioNumber);
	const    uint8_t  adcc = _portNumberToADCChannel(gpioNumber);

	if( (*ddr & bit) ) return 0; // Exit if the GPIO is not in input mode
	if( adcc == 0xFF ) return 0; // Exit if the GPIO does not support ADC

	ADCSRB = ( ADCSRB & ~_BV(MUX5) ) | ( ( (adcc >> 3) & 0x01 ) << MUX5 );
	ADMUX  = _BV(REFS0) | (adcc & 0x07);

	ADCSRA |= _BV(ADSC);

	while( ADCSRA & _BV(ADSC) );

	return ADC;
}


uint16_t hwgpio_getInternalTemperature(void)
{
    ADCSRB |= _BV(MUX5);
    ADMUX   = _BV(REFS1) | _BV(REFS0) | _BV(MUX2) | _BV(MUX1) | _BV(MUX0);

    ADCSRA |= _BV(ADSC);

    while( ADCSRA & _BV(ADSC) );

    return ADC;
}
