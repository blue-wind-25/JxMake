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

#if !ENABLE_DEBUG_STREAM

static const     uint8_t _timer1_aledBlinkActiveSet       =   4;
static const    uint16_t _timer1_aledBlinkDelayMax_Idle   = 100; // 1000mS
static const    uint16_t _timer1_aledBlinkDelayMax_Active =  10; //  100mS
static volatile uint16_t _timer1_aledBlinkDelayCnt        =   0;
static volatile  uint8_t _timer1_aledBlinkState           =   0;
static volatile  uint8_t _timer1_aledBlinkActiveCnt       =   0;

#endif


ISR(TIMER1_COMPA_vect)
{
	// Increment the millis
	_timer1_millisCnt += 1000U / _timer1_milisResolution;

#if !ENABLE_DEBUG_STREAM

	// Determine the blink delay for the activity LED
	const uint16_t aledBlinkDelayMax = _timer1_aledBlinkActiveCnt ? _timer1_aledBlinkDelayMax_Active : _timer1_aledBlinkDelayMax_Idle;

	// Increment and check the delay counter
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

#endif
}


void init_millis(void)
{
	TCCR1A  = 0;
	TCCR1B |= _BV(WGM12) | _BV(CS11) | _BV(CS10); // CTC mode ; prescaler = 64

	OCR1A   = ( (F_CPU / 64U) + (_timer1_milisResolution / 2) ) / _timer1_milisResolution - 1U;

	TCNT1   = 0;
	TIMSK1 |= (1 << OCIE1A);
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


#if !ENABLE_DEBUG_STREAM

void blink_aled(void)
{ _timer1_aledBlinkActiveCnt = _timer1_aledBlinkActiveSet; }

#endif
