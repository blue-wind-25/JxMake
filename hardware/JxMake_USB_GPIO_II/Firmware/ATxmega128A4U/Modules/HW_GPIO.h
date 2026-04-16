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

#define PWM_CTRL TCC0.CTRLB

#define PWM_CTRL_INIT() do {                                                 \
	TCC0.CTRLA    = TC_CLKSEL_DIV64_gc; /* Enable timer with prescaler 64 */ \
	TCC0.CTRLB    = TC_WGMODE_DSTOP_gc; /* Dual Slope PWM - update on TOP */ \
	TCC0.CTRLC    = 0;                  /* Clear all WG                   */ \
	TCC0.CTRLD    = 0;                  /* Disable event system           */ \
	TCC0.CTRLE    = 0;                  /* Timer type 0                   */ \
	TCC0.PER      = 0xFF;               /* Use 8-bit resolution           */ \
	TCC0.INTCTRLA = 0;                  /* Disable interrupt              */ \
	TCC0.INTCTRLB = 0;                  /* Disable interrupt              */ \
} while(0)

static const uint8_t PROGMEM _GPIO_NUMBER_TO_CTRL_BIT[] = { // For PWM
	/* GPIO 0 */ TC0_CCAEN_bm,
	/* GPIO 1 */ TC0_CCBEN_bm,
	/* GPIO 2 */ TC0_CCCEN_bm,
	/* GPIO 3 */ TC0_CCDEN_bm,
	/* GPIO 4 */ 0,
	/* GPIO 5 */ 0,
	/* GPIO 6 */ 0,
	/* GPIO 7 */ 0
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_CCX[] = {
	/* GPIO 0 */ (uint16_t) &TCC0.CCA,
	/* GPIO 1 */ (uint16_t) &TCC0.CCB,
	/* GPIO 2 */ (uint16_t) &TCC0.CCC,
	/* GPIO 3 */ (uint16_t) &TCC0.CCD,
	/* GPIO 4 */ 0,
	/* GPIO 5 */ 0,
	/* GPIO 6 */ 0,
	/* GPIO 7 */ 0
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_PINCTL[] = {
	/* GPIO 0 */ (uint16_t) &PORTA.PIN0CTRL,
	/* GPIO 1 */ (uint16_t) &PORTA.PIN1CTRL,
	/* GPIO 2 */ (uint16_t) &PORTA.PIN2CTRL,
	/* GPIO 3 */ (uint16_t) &PORTA.PIN3CTRL,
	/* GPIO 4 */ (uint16_t) &PORTA.PIN4CTRL,
	/* GPIO 5 */ (uint16_t) &PORTA.PIN5CTRL,
	/* GPIO 6 */ (uint16_t) &PORTA.PIN6CTRL,
	/* GPIO 7 */ (uint16_t) &PORTA.PIN7CTRL
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_DIR[] = {
	/* GPIO 0 */ (uint16_t) &PORTA.DIR,
	/* GPIO 1 */ (uint16_t) &PORTA.DIR,
	/* GPIO 2 */ (uint16_t) &PORTA.DIR,
	/* GPIO 3 */ (uint16_t) &PORTA.DIR,
	/* GPIO 4 */ (uint16_t) &PORTA.DIR,
	/* GPIO 5 */ (uint16_t) &PORTA.DIR,
	/* GPIO 6 */ (uint16_t) &PORTA.DIR,
	/* GPIO 7 */ (uint16_t) &PORTA.DIR
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_DIRCLR[] = {
	/* GPIO 0 */ (uint16_t) &PORTA.DIRCLR,
	/* GPIO 1 */ (uint16_t) &PORTA.DIRCLR,
	/* GPIO 2 */ (uint16_t) &PORTA.DIRCLR,
	/* GPIO 3 */ (uint16_t) &PORTA.DIRCLR,
	/* GPIO 4 */ (uint16_t) &PORTA.DIRCLR,
	/* GPIO 5 */ (uint16_t) &PORTA.DIRCLR,
	/* GPIO 6 */ (uint16_t) &PORTA.DIRCLR,
	/* GPIO 7 */ (uint16_t) &PORTA.DIRCLR
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_DIRSET[] = {
	/* GPIO 0 */ (uint16_t) &PORTA.DIRSET,
	/* GPIO 1 */ (uint16_t) &PORTA.DIRSET,
	/* GPIO 2 */ (uint16_t) &PORTA.DIRSET,
	/* GPIO 3 */ (uint16_t) &PORTA.DIRSET,
	/* GPIO 4 */ (uint16_t) &PORTA.DIRSET,
	/* GPIO 5 */ (uint16_t) &PORTA.DIRSET,
	/* GPIO 6 */ (uint16_t) &PORTA.DIRSET,
	/* GPIO 7 */ (uint16_t) &PORTA.DIRSET
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_DIR_PWM[] = {
	/* GPIO 0 */ (uint16_t) &PORTC.DIR,
	/* GPIO 1 */ (uint16_t) &PORTC.DIR,
	/* GPIO 2 */ (uint16_t) &PORTC.DIR,
	/* GPIO 3 */ (uint16_t) &PORTC.DIR,
	/* GPIO 4 */ 0,
	/* GPIO 5 */ 0,
	/* GPIO 6 */ 0,
	/* GPIO 7 */ 0
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_DIRCLR_PWM[] = {
	/* GPIO 0 */ (uint16_t) &PORTC.DIRCLR,
	/* GPIO 1 */ (uint16_t) &PORTC.DIRCLR,
	/* GPIO 2 */ (uint16_t) &PORTC.DIRCLR,
	/* GPIO 3 */ (uint16_t) &PORTC.DIRCLR,
	/* GPIO 4 */ 0,
	/* GPIO 5 */ 0,
	/* GPIO 6 */ 0,
	/* GPIO 7 */ 0
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_DIRSET_PWM[] = {
	/* GPIO 0 */ (uint16_t) &PORTC.DIRSET,
	/* GPIO 1 */ (uint16_t) &PORTC.DIRSET,
	/* GPIO 2 */ (uint16_t) &PORTC.DIRSET,
	/* GPIO 3 */ (uint16_t) &PORTC.DIRSET,
	/* GPIO 4 */ 0,
	/* GPIO 5 */ 0,
	/* GPIO 6 */ 0,
	/* GPIO 7 */ 0
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_OUTCLR[] = {
	/* GPIO 0 */ (uint16_t) &PORTA.OUTCLR,
	/* GPIO 1 */ (uint16_t) &PORTA.OUTCLR,
	/* GPIO 2 */ (uint16_t) &PORTA.OUTCLR,
	/* GPIO 3 */ (uint16_t) &PORTA.OUTCLR,
	/* GPIO 4 */ (uint16_t) &PORTA.OUTCLR,
	/* GPIO 5 */ (uint16_t) &PORTA.OUTCLR,
	/* GPIO 6 */ (uint16_t) &PORTA.OUTCLR,
	/* GPIO 7 */ (uint16_t) &PORTA.OUTCLR
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_OUTSET[] = {
	/* GPIO 0 */ (uint16_t) &PORTA.OUTSET,
	/* GPIO 1 */ (uint16_t) &PORTA.OUTSET,
	/* GPIO 2 */ (uint16_t) &PORTA.OUTSET,
	/* GPIO 3 */ (uint16_t) &PORTA.OUTSET,
	/* GPIO 4 */ (uint16_t) &PORTA.OUTSET,
	/* GPIO 5 */ (uint16_t) &PORTA.OUTSET,
	/* GPIO 6 */ (uint16_t) &PORTA.OUTSET,
	/* GPIO 7 */ (uint16_t) &PORTA.OUTSET
};

static const uint16_t PROGMEM _GPIO_NUMBER_TO_IN[] = {
	/* GPIO 0 */ (uint16_t) &PORTA.IN,
	/* GPIO 1 */ (uint16_t) &PORTA.IN,
	/* GPIO 2 */ (uint16_t) &PORTA.IN,
	/* GPIO 3 */ (uint16_t) &PORTA.IN,
	/* GPIO 4 */ (uint16_t) &PORTA.IN,
	/* GPIO 5 */ (uint16_t) &PORTA.IN,
	/* GPIO 6 */ (uint16_t) &PORTA.IN,
	/* GPIO 7 */ (uint16_t) &PORTA.IN
};

static const uint8_t PROGMEM _GPIO_NUMBER_TO_BITMASK[] = {
	/* GPIO 0 */ _BV(0),
	/* GPIO 1 */ _BV(1),
	/* GPIO 2 */ _BV(2),
	/* GPIO 3 */ _BV(3),
	/* GPIO 4 */ _BV(4),
	/* GPIO 5 */ _BV(5),
	/* GPIO 6 */ _BV(6),
	/* GPIO 7 */ _BV(7)
};

static const uint8_t PROGMEM _GPIO_NUMBER_TO_ADC_CHANNEL[] = {
	/* GPIO 0 */ ADC_CH_MUXPOS_PIN0_gc,
	/* GPIO 1 */ ADC_CH_MUXPOS_PIN1_gc,
	/* GPIO 2 */ ADC_CH_MUXPOS_PIN2_gc,
	/* GPIO 3 */ ADC_CH_MUXPOS_PIN3_gc,
	/* GPIO 4 */ ADC_CH_MUXPOS_PIN4_gc,
	/* GPIO 5 */ ADC_CH_MUXPOS_PIN5_gc,
	/* GPIO 6 */ ADC_CH_MUXPOS_PIN6_gc,
	/* GPIO 7 */ ADC_CH_MUXPOS_PIN7_gc
};

#define _portNumberToCTRLBit(N)    (                   pgm_read_byte( _GPIO_NUMBER_TO_CTRL_BIT    + (N) )   )
#define _portNumberToCCX(N)        ( (register16_t*) ( pgm_read_word( _GPIO_NUMBER_TO_CCX         + (N) ) ) )
#define _portNumberToPINCTL(N)     ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_PINCTL      + (N) ) ) )
#define _portNumberToDIR(N)        ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_DIR         + (N) ) ) )
#define _portNumberToDIRCLR(N)     ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_DIRCLR      + (N) ) ) )
#define _portNumberToDIRSET(N)     ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_DIRSET      + (N) ) ) )
#define _portNumberToDIR_PWM(N)    ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_DIR_PWM     + (N) ) ) )
#define _portNumberToDIRCLR_PWM(N) ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_DIRCLR_PWM  + (N) ) ) )
#define _portNumberToDIRSET_PWM(N) ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_DIRSET_PWM  + (N) ) ) )
#define _portNumberToOUTCLR(N)     ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_OUTCLR      + (N) ) ) )
#define _portNumberToOUTSET(N)     ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_OUTSET      + (N) ) ) )
#define _portNumberToIN(N)         ( (register8_t* ) ( pgm_read_word( _GPIO_NUMBER_TO_IN          + (N) ) ) )
#define _portNumberToBitMask(N)    (                   pgm_read_byte( _GPIO_NUMBER_TO_BITMASK     + (N) )   )
#define _portNumberToADCChannel(N) (                   pgm_read_byte( _GPIO_NUMBER_TO_ADC_CHANNEL + (N) )   )

#endif


////////////////////////////////////////////////////////////////////////////////////////////////////


extern void hwgpio_init(void);

extern void hwgpio_setMode(uint8_t modeBits74, uint8_t modeBits30, uint8_t initVals70);
extern void hwgpio_setValues(uint8_t mask70, uint8_t vals70);
extern uint8_t hwgpio_getValues(void);

extern void hwgpio_setPWM(uint8_t gpioNumber, uint8_t val70);

extern uint16_t hwgpio_getADC(uint8_t gpioNumber);

extern uint16_t hwgpio_getVREAD(void);
extern bool hwgpio_calibrateVREAD(uint16_t expectedValue);

extern uint16_t hwgpio_getInternalTemperature(void);


#endif // __MODULES__HW_GPIO_H__
