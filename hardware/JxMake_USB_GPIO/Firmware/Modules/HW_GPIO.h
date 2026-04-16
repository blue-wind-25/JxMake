/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__HW_GPIO_H__
#define __MODULES__HW_GPIO_H__



#ifdef __MODULES__HW_GPIO_C__

/*
 * ##############################################################################################################
 * !!! NOTE !!!
 * ##############################################################################################################
 * Synchronize these with:
 *     '<jxmake_src_root>/hardware/JxMake_USB_GPIO-Protocol_Manual.txt'
 *     '<jxmake_src_root>/src/jxm/ugc/USB_GPIO.java'
 * ##############################################################################################################
 */

#define PWM_TCCR TCCR4A

#define PWM_TCCR_INIT() do {                                                           \
	TCCR4B = _BV(CS42 ) | _BV(CS41) | _BV(CS40); /* Prescaler = 64                  */ \
	TCCR4D = _BV(WGM40)                        ; /* Phase and frequency correct PWM */ \
	TCCR4A = _BV(PWM4A) | _BV(PWM4B)           ; /* Enable PWM for OCR4A and OCR4B  */ \
} while(false)

static const uint8_t PROGMEM _GPIO_NUMBER_TO_TCCR_BITS[] = {
	/* GPIO 0 */ _BV(COM4B1),
	/* GPIO 1 */ _BV(COM4A0),
	/* GPIO 2 */ 0,
	/* GPIO 3 */ 0,
	/* GPIO 4 */ 0,
	/* GPIO 5 */ 0,
	/* GPIO 6 */ 0,
	/* GPIO 7 */ 0
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_OCR[] = {
	/* GPIO 0 */ (uint16_t) &OCR4B,
	/* GPIO 1 */ (uint16_t) &OCR4A,
	/* GPIO 2 */ 0,
	/* GPIO 3 */ 0,
	/* GPIO 4 */ 0,
	/* GPIO 5 */ 0,
	/* GPIO 6 */ 0,
	/* GPIO 7 */ 0
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_DDR[] = {
	/* GPIO 0 */ (uint16_t) &DDRB,
	/* GPIO 1 */ (uint16_t) &DDRC,
	/* GPIO 2 */ (uint16_t) &DDRD,
	/* GPIO 3 */ (uint16_t) &DDRE,
	/* GPIO 4 */ (uint16_t) &DDRF,
	/* GPIO 5 */ (uint16_t) &DDRF,
	/* GPIO 6 */ (uint16_t) &DDRF,
	/* GPIO 7 */ (uint16_t) &DDRF
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_PORT[] = {
	/* GPIO 0 */ (uint16_t) &PORTB,
	/* GPIO 1 */ (uint16_t) &PORTC,
	/* GPIO 2 */ (uint16_t) &PORTD,
	/* GPIO 3 */ (uint16_t) &PORTE,
	/* GPIO 4 */ (uint16_t) &PORTF,
	/* GPIO 5 */ (uint16_t) &PORTF,
	/* GPIO 6 */ (uint16_t) &PORTF,
	/* GPIO 7 */ (uint16_t) &PORTF
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_PIN[] = {
	/* GPIO 0 */ (uint16_t) &PINB,
	/* GPIO 1 */ (uint16_t) &PINC,
	/* GPIO 2 */ (uint16_t) &PIND,
	/* GPIO 3 */ (uint16_t) &PINE,
	/* GPIO 4 */ (uint16_t) &PINF,
	/* GPIO 5 */ (uint16_t) &PINF,
	/* GPIO 6 */ (uint16_t) &PINF,
	/* GPIO 7 */ (uint16_t) &PINF
};

static const uint8_t PROGMEM _GPIO_NUMBER_TO_BITMASK[] = {
	/* GPIO 0 */ _BV(6),
	/* GPIO 1 */ _BV(6),
	/* GPIO 2 */ _BV(4),
	/* GPIO 3 */ _BV(6),
	/* GPIO 4 */ _BV(4),
	/* GPIO 5 */ _BV(5),
	/* GPIO 6 */ _BV(6),
	/* GPIO 7 */ _BV(7)
};

static const uint8_t PROGMEM _GPIO_NUMBER_TO_ADC_CHANNEL[] = {
	/* GPIO 0 */   13,
	/* GPIO 1 */ 0xFF, // No ADC
	/* GPIO 2 */    8,
	/* GPIO 3 */ 0xFF, // No ADC
	/* GPIO 4 */    4,
	/* GPIO 5 */    5,
	/* GPIO 6 */    6,
	/* GPIO 7 */    7,
};

#define _portNumberToTCCRBits(N)   (                     ( pgm_read_byte( _GPIO_NUMBER_TO_TCCR_BITS   + (N) ) ) )
#define _portNumberToOCR(N)        ( (volatile uint8_t*) ( pgm_read_word( _GPIO_NUMBER_TO_OCR         + (N) ) ) )
#define _portNumberToDDR(N)        ( (volatile uint8_t*) ( pgm_read_word( _GPIO_NUMBER_TO_DDR         + (N) ) ) )
#define _portNumberToPORT(N)       ( (volatile uint8_t*) ( pgm_read_word( _GPIO_NUMBER_TO_PORT        + (N) ) ) )
#define _portNumberToPIN(N)        ( (volatile uint8_t*) ( pgm_read_word( _GPIO_NUMBER_TO_PIN         + (N) ) ) )
#define _portNumberToBitMask(N)    (                     ( pgm_read_byte( _GPIO_NUMBER_TO_BITMASK     + (N) ) ) )
#define _portNumberToADCChannel(N) (                     ( pgm_read_byte( _GPIO_NUMBER_TO_ADC_CHANNEL + (N) ) ) )

#endif


////////////////////////////////////////////////////////////////////////////////////////////////////


extern void hwgpio_init(void);

extern void hwgpio_setMode(uint8_t modeBits74, uint8_t modeBits30, uint8_t initVals70);
extern void hwgpio_setValues(uint8_t mask70, uint8_t vals70);
extern uint8_t hwgpio_getValues(void);

extern void hwgpio_setPWM(uint8_t gpioNumber, uint8_t val70);

extern uint16_t hwgpio_getADC(uint8_t gpioNumber);
extern uint16_t hwgpio_getInternalTemperature(void);


#endif // __MODULES__HW_GPIO_H__
