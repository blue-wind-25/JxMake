/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __ACTIVITY_LED_H__
#define __ACTIVITY_LED_H__


#include "IOPairParser.h"


// The value 'SYSTEM_LED_IO' is defined through a command line option in the 'Makefile'
#define SLED_SETUP()  do { IO_PAIR_SETMODE_OUT(SYSTEM_LED_IO  ); } while(0)
#define SLED_OFF()    do { IO_PAIR_SET_VALUE_0(SYSTEM_LED_IO  ); } while(0)
#define SLED_ON()     do { IO_PAIR_SET_VALUE_1(SYSTEM_LED_IO  ); } while(0)
#define SLED_TOGGLE() do { IO_PAIR_SET_VALUE_T(SYSTEM_LED_IO  ); } while(0)


// The value 'ACTIVITY_LED_IO' is defined through a command line option in the 'Makefile'
#define ALED_SETUP()  do { IO_PAIR_SETMODE_OUT(ACTIVITY_LED_IO); } while(0)
#define ALED_OFF()    do { IO_PAIR_SET_VALUE_0(ACTIVITY_LED_IO); } while(0)
#define ALED_ON()     do { IO_PAIR_SET_VALUE_1(ACTIVITY_LED_IO); } while(0)
#define ALED_TOGGLE() do { IO_PAIR_SET_VALUE_T(ACTIVITY_LED_IO); } while(0)


#endif // __ACTIVITY_LED_H__
