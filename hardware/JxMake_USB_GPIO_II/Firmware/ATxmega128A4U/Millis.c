/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdint.h>

#include <avr/interrupt.h>
#include <avr/io.h>
#include <util/atomic.h>

#include "../ActivityLED.h"
#include "Millis.h"


static const    uint16_t _timer1_milisResolution          = 100; //   10mS
static volatile uint32_t _timer1_millisCnt                =   0;

static const     uint8_t _timer1_sledBlinkActiveSet       =   4;
static const    uint16_t _timer1_sledBlinkDelayMax_Active =  10; //  100mS
static volatile uint16_t _timer1_sledBlinkDelayCnt        =   0;
static volatile  uint8_t _timer1_sledBlinkState           =   0;
static volatile  uint8_t _timer1_sledBlinkActiveCnt       =   0;

static const     uint8_t _timer1_aledBlinkActiveSet       =   4;
static const    uint16_t _timer1_aledBlinkDelayMax_Idle   = 100; // 1000mS
static const    uint16_t _timer1_aledBlinkDelayMax_Active =  10; //  100mS
static volatile uint16_t _timer1_aledBlinkDelayCnt        =   0;
static volatile  uint8_t _timer1_aledBlinkState           =   0;
static volatile  uint8_t _timer1_aledBlinkActiveCnt       =   0;


ISR(TCC1_OVF_vect)
{
	// Increment the millis
	_timer1_millisCnt += 1000U / _timer1_milisResolution;

	// Process the system LED
	if(_timer1_sledBlinkActiveCnt) {
		if(++_timer1_sledBlinkDelayCnt >= _timer1_sledBlinkDelayMax_Active) {
			// Invert the state
			_timer1_sledBlinkState = !_timer1_sledBlinkState;
			// Turn the LED on or off
			if(_timer1_sledBlinkState) SLED_ON ();
			else                       SLED_OFF();
			// Reset the delay counter
			_timer1_sledBlinkDelayCnt = 0;
			// Decrement the activity counter
			--_timer1_sledBlinkActiveCnt;
		}
	}
	else if(_timer1_sledBlinkState) {
		if(++_timer1_sledBlinkDelayCnt >= _timer1_sledBlinkDelayMax_Active) {
			// Clear the state
			_timer1_sledBlinkState = 0;
			// Turn the LED off
			SLED_OFF();
			// Reset the delay counter
			_timer1_sledBlinkDelayCnt = 0;
		}
	}

	// Process the activity LED
	const uint16_t aledBlinkDelayMax = _timer1_aledBlinkActiveCnt ? _timer1_aledBlinkDelayMax_Active : _timer1_aledBlinkDelayMax_Idle;

	if(++_timer1_aledBlinkDelayCnt >= aledBlinkDelayMax) {
		// Invert the state
		_timer1_aledBlinkState = !_timer1_aledBlinkState;
		// Turn the LED on or off
		if(_timer1_aledBlinkState) ALED_ON ();
		else                       ALED_OFF();
		// Adjust the delay counter
		if(_timer1_aledBlinkActiveCnt) {
			// On activity, the on time is shorter than the off time
			_timer1_aledBlinkDelayCnt = _timer1_aledBlinkState ? (aledBlinkDelayMax * 3 / 4) : 0;
			// Decrement the activity counter
			--_timer1_aledBlinkActiveCnt;
		}
		else {
			// On idle, the on time is the same as the off time
			_timer1_aledBlinkDelayCnt = 0;
		}
	}
}


void init_millis(void)
{
	TCC1.PER      = ( (F_CPU / 1024U) + (_timer1_milisResolution / 2) ) / _timer1_milisResolution - 1U;
	TCC1.CNT      = 0;                    // Clear the counter
	TCC1.CTRLE    = 0;                    // Timer type 0
	TCC1.CTRLD    = 0;                    // Disable event system
	TCC1.CTRLC    = 0;                    // Clear all WG
	TCC1.CTRLB    = TC_WGMODE_NORMAL_gc;  // Normal mode
	TCC1.CTRLA    = TC_CLKSEL_DIV1024_gc; // Enable timer with prescaler 1024
	TCC1.INTCTRLA = TC_OVFINTLVL_LO_gc;   // Enabled overflow interrupt with low priority
	TCC1.INTCTRLB = 0;                    // Disable capture/compare interrupts
}


uint32_t millis(void)
{
	uint32_t retVal;

	ATOMIC_BLOCK(ATOMIC_FORCEON) {
		retVal = _timer1_millisCnt;
	}

	return retVal;
}


////////////////////////////////////////////////////////////////////////////////////////////////////


void blink_sled(void)
{ _timer1_sledBlinkActiveCnt = _timer1_sledBlinkActiveSet; }

void blink_aled(void)
{ _timer1_aledBlinkActiveCnt = _timer1_aledBlinkActiveSet; }
