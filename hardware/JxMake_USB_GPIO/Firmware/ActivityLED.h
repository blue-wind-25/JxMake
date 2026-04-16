/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __ACTIVITY_LED_H__
#define __ACTIVITY_LED_H__


#include "IOPairParser.h"


#if !!ENABLE_DEBUG_STREAM

	#define ALED_SETUP()  do {} while(0)
	#define ALED_OFF()    do {} while(0)
	#define ALED_ON()     do {} while(0)
	#define ALED_TOGGLE() do {} while(0)

#else

	#define ALED_DDR           IO_PAIR_DDR_VAL(ACTIVITY_LED_IO)
	#define ALED_PORT          IO_PAIR_PRT_VAL(ACTIVITY_LED_IO)
	#define ALED_BIT_MASK _BV( IO_PAIR_BIT_VAL(ACTIVITY_LED_IO) )

	#define ALED_SETUP()  do { ALED_DDR  |=  ALED_BIT_MASK; } while(0)
	#define ALED_OFF()    do { ALED_PORT &= ~ALED_BIT_MASK; } while(0)
	#define ALED_ON()     do { ALED_PORT |=  ALED_BIT_MASK; } while(0)
	#define ALED_TOGGLE() do { ALED_PORT ^=  ALED_BIT_MASK; } while(0)

#endif


#endif // __ACTIVITY_LED_H__
