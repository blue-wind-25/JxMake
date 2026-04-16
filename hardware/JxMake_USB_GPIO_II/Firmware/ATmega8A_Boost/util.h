/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Include the standard header files
#include "../ATmega8A_Common/std_includes.h"

// Include the other header files
#include "../ATmega8A_Common/attributes.h"
#include "../ATmega8A_Common/serial.h"
#include "../ATmega8A_Common/tick.h"
#include "../ATmega8A_Common/watchdog.h"

#include "adc.h"
#include "gpio.h"
#include "pwm.h"

#include "boost.h"


// Firmware version numbers
#define FIRMWARE_VERSION_M 1
#define FIRMWARE_VERSION_N 0
#define FIRMWARE_VERSION_R 5


// Include the other header files
#include "../ATmega8A_Common/imsg.h"


// Enable and disable Vpp-Vdd
static inline void enaVppVdd()
{
	if(boostVoltage == 0) {
		gset_trgEnaRstLV(); //delayUS(1);
		gset_trgEnaVdd  ();
	}
	else {
		gset_trgEnaRstHV(); //delayUS(1);
		gset_trgEnaVdd  ();
	}
}

static inline void disVppVdd()
{
	if(boostVoltage == 0) {
		gset_trgDisVdd  (); //delayUS(1);
		gset_trgDisRstLV();
	}
	else {
		gset_trgDisVdd  (); //delayUS(1);
		gset_trgDisRstHV();
	}
}
