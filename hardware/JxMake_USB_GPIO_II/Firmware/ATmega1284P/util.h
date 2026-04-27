/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Macro for defining safe voltage tolerance (~−0.4%) to compensate for ADC inaccuracies
#define V_TOL(V) ( ( ( (uint32_t) (V) ) * 1020UL + 512UL ) / 1024UL )

// Macro for defining the AVcc voltage range
#define V_D_SUP_R() V_TOL(330), V_TOL(550)

// Macro for defining the idle VBoost voltage range
#define V_BOOST_I() V_TOL(400 - 30), V_TOL(700 - 30)


// Convert 6-bit unsigned integer to zero-padded 6-character binary string
static inline const char* u6ToBin6(const uint8_t value) // WARNING : This function returns a pointer to a static buffer!
{
    static char buffer[7]; // 6 bits + null terminator

    for(int8_t i = 5; i >= 0; --i) {
        buffer[5 - i] = ( value & (1U << i) ) ? '1' : '0';
    }
    buffer[6] = '\0'; // Null-terminate the string

    return buffer;
}


// Convert 8-bit unsigned integer to zero-padded 8-character binary string
static inline const char* u8ToBin8(const uint8_t value) // WARNING : This function returns a pointer to a static buffer!
{
    static char buffer[9]; // 8 bits + null terminator

    for(int8_t i = 7; i >= 0; --i) {
        buffer[7 - i] = ( value & (1U << i) ) ? '1' : '0';
    }
    buffer[8] = '\0'; // Null-terminate the string

    return buffer;
}


// Convert voltage (scaled by 100) to zero-padded 6-digit "ii.ffV" string
#define VOLTAGE_TO_STRING_BUFFER_SIZE 7 /* 6 digits "ii.ffV" + null terminator */

static inline void utilStrPrintVoltage(char dstBuff[VOLTAGE_TO_STRING_BUFFER_SIZE], const uint16_t v100)
{
	const uint16_t ints = v100 / 100;
	const uint16_t frac = v100 - ints * 100;

	wdt_reset();
	sprintf_P( dstBuff, PSTR("%02u.%02uV"), ints, frac );
	wdt_reset();
}

static inline const char* utilV00ToStr02d0d(const uint16_t v100) // WARNING : This function returns a pointer to a static buffer!
{
	static char buffer[VOLTAGE_TO_STRING_BUFFER_SIZE];

	utilStrPrintVoltage(buffer, v100);

	return buffer;
}


// Check voltage
static inline int8_t utilCheckVoltage(const uint16_t v100, const uint16_t min100, const uint16_t max100)
{
	if(v100 < min100) return -1;
	if(v100 > max100) return  1;
	                  return  0;
}


// Print and check voltage
static inline void utilPrintCheckVoltage(const char* pstrName, const uint16_t v100, const uint16_t min100, const uint16_t max100, const bool turnOffVBoost)
{
	const char*    status = (v100 < min100) ? PSTR(" (UNDERVOLTAGE)") :
	                        (v100 > max100) ? PSTR(" (OVERVOLTAGE)" ) :
	                                          PSTR(""               );

	printIMsgln( PSTR("    %S = %s%S."), pstrName, utilV00ToStr02d0d(v100), status );

	if(v100 < min100) {
		if(turnOffVBoost) boostSetV(0b00000000);
		ledUVHalt();
	}

	if(v100 > max100)  {
		if(turnOffVBoost) boostSetV(0b00000000);
		ledOVHalt();
	}
}


// Check AVcc
// WARNING : This function always check from the global variable 'g_AVcc'!
static inline void utilCheckAVcc()
{
	// Vdd (and AVcc) must be within 3.3 V to 5.5 V for proper operation
	utilPrintCheckVoltage( PSTR("Digital Supply"), g_AVcc, V_D_SUP_R(), false );
}


// Check VBoost (Idle)
// WARNING : This function always check from the global variable 'g_VBoostIdle'!
static inline void utilCheckVBoostIdle()
{
	// Vdd47 must be within 4.0 V to 7.0 V for proper operation
	// Due to the Schottky diode in the boost converter module, the thresholds below are reduced by 30 (i.e., 0.3 V)
	utilPrintCheckVoltage( PSTR("VBoost (Idle )"), g_VBoostIdle, V_BOOST_I(), false );
}


// Check the output voltage of the boost converter module
static bool __bcvSupportAveragedVoltageOutput = false;

static inline void __utilSetVBoost(const uint8_t vselMask, const uint16_t target)
{
	// Set the voltage
	ledIndToggle();

	boostSetV(vselMask);

	const uint16_t minV = UV_TOL_V(target);

	// Check the voltage
	for(uint8_t i = 0; i < 16; ++i) {

		// Delay for a while
		delayMS(25);

		// Read the voltage
		ledIndToggle();

		const uint16_t read = adcReadVBoost();

		// Check the voltage
		if(read >= minV) return;

	} // for
}

static inline uint16_t __utilReadVBoost()
{
	ledIndToggle();

	return adcReadVBoost();
}

static inline void utilCheckVBoostRange()
{
	// Print message
	printIMsgln( PSTR("Checking the output voltage of the Boost Converter Module ...") );

	// Check the voltage
	__utilSetVBoost(0b00000100,  840); utilPrintCheckVoltage( PSTR("VBoost (08.4V)"), __utilReadVBoost(), V_BOOST_R( 840), true );
	__utilSetVBoost(0b00001000,  940); utilPrintCheckVoltage( PSTR("VBoost (09.4V)"), __utilReadVBoost(), V_BOOST_R( 940), true );
	__utilSetVBoost(0b00010000, 1040); utilPrintCheckVoltage( PSTR("VBoost (10.4V)"), __utilReadVBoost(), V_BOOST_R(1040), true );
	__utilSetVBoost(0b00100000, 1140); utilPrintCheckVoltage( PSTR("VBoost (11.4V)"), __utilReadVBoost(), V_BOOST_R(1140), true );
	__utilSetVBoost(0b01000000, 1240); utilPrintCheckVoltage( PSTR("VBoost (12.4V)"), __utilReadVBoost(), V_BOOST_R(1240), true );
	__utilSetVBoost(0b10000000, 1340); utilPrintCheckVoltage( PSTR("VBoost (13.4V)"), __utilReadVBoost(), V_BOOST_R(1340), true );
	__utilSetVBoost(0b00000000,    0); utilPrintCheckVoltage( PSTR("VBoost (Idle )"), __utilReadVBoost(), V_BOOST_I(    ), true );

	// Check whether the module supports averaged voltage output
	__utilSetVBoost(0b10000100, 1090);
	if( utilCheckVoltage( __utilReadVBoost(), V_BOOST_R(1090) ) == 0 ) __bcvSupportAveragedVoltageOutput = true;

	__utilSetVBoost(0b00000000,    0); utilPrintCheckVoltage( PSTR("VBoost (Idle )"), __utilReadVBoost(), V_BOOST_I(    ), true );

	// Print message
	if(__bcvSupportAveragedVoltageOutput) printIMsgln( PSTR("    Averaged VBoost output is supported."    ) );
	else                                  printIMsgln( PSTR("    Averaged VBoost output is not supported.") );

	ledIndOff();
}


// Update the output voltage of the boost converter module
static uint8_t  __prev_VBoostDSW = 255;
static uint16_t __prev_VBoostSet =   0;

static inline bool utilUpdateVBoost()
{
	// Get the voltage setting
	uint8_t  dsw    = boostGetDIPSwitchState();
	uint16_t vboost = 0;

	if(dsw == __prev_VBoostDSW) return false;
	__prev_VBoostDSW = dsw;

	if(__bcvSupportAveragedVoltageOutput) {
		uint8_t  cnt = 0;
		uint16_t sum = 0;
		if( ( dsw & _BV(DIP_BIT_13V) ) == 0 ) { ++cnt; sum += 1340; }
		if( ( dsw & _BV(DIP_BIT_12V) ) == 0 ) { ++cnt; sum += 1240; }
		if( ( dsw & _BV(DIP_BIT_11V) ) == 0 ) { ++cnt; sum += 1140; }
		if( ( dsw & _BV(DIP_BIT_10V) ) == 0 ) { ++cnt; sum += 1040; }
		if( ( dsw & _BV(DIP_BIT_09V) ) == 0 ) { ++cnt; sum +=  940; }
		if( ( dsw & _BV(DIP_BIT_08V) ) == 0 ) { ++cnt; sum +=  840; }
		vboost = (cnt == 0) ? 0 : (sum / cnt);
	}
	else {
		     if( ( dsw & _BV(DIP_BIT_08V) ) == 0 ) { dsw = _BV(DIP_BIT_08V); vboost =  840; }
		else if( ( dsw & _BV(DIP_BIT_09V) ) == 0 ) { dsw = _BV(DIP_BIT_09V); vboost =  940; }
		else if( ( dsw & _BV(DIP_BIT_10V) ) == 0 ) { dsw = _BV(DIP_BIT_10V); vboost = 1040; }
		else if( ( dsw & _BV(DIP_BIT_11V) ) == 0 ) { dsw = _BV(DIP_BIT_11V); vboost = 1140; }
		else if( ( dsw & _BV(DIP_BIT_12V) ) == 0 ) { dsw = _BV(DIP_BIT_12V); vboost = 1240; }
		else if( ( dsw & _BV(DIP_BIT_13V) ) == 0 ) { dsw = _BV(DIP_BIT_13V); vboost = 1340; }
		else                                       { dsw = 0               ; vboost =    0; }
	}

	if(vboost == 0) vboost = (g_VBoostIdle + 5U) / 10U * 10U;

	if(__prev_VBoostSet == vboost) return false;

	// Set the voltage
	__utilSetVBoost(dsw, vboost);

	// Check the voltage and print message
	ledIndOff();

	printIMsgln( PSTR("Changing the output voltage of the Boost Converter Module to [%s] ..."), u6ToBin6(dsw >> 2) );
		utilPrintCheckVoltage( PSTR("VBoost (Prev )"), __prev_VBoostSet  , V_BOOST_R(__prev_VBoostSet), true );
	if(dsw) {
		utilPrintCheckVoltage( PSTR("VBoost (Set  )"), vboost            , V_BOOST_R(vboost          ), true );
		utilPrintCheckVoltage( PSTR("VBoost (Check)"), __utilReadVBoost(), V_BOOST_R(vboost          ), true );
	}
	else {
		utilPrintCheckVoltage( PSTR("VBoost (Idle )"), vboost            , V_BOOST_R(vboost          ), true );
		utilPrintCheckVoltage( PSTR("VBoost (Check)"), __utilReadVBoost(), V_BOOST_R(g_VBoostIdle    ), true );
	}
	printIMsgNLC();

	ledIndOff();

	// Save the voltage
	__prev_VBoostSet = vboost;

	// Done
	return true;
}


// Detect trigger from PGC and PGD signals
#define DETECT_PGC_TIMEOUT_MS 100

static volatile uint8_t _pgcdCmd = 0;

static __never_inline bool utilDetectPGCD()
{
	// NOTE : Please refer to '../../../JxMake_USB_GPIO-Protocol_Manual.txt' for the protocol details.

	const uint8_t triggerKeyPrefix[] = { 0x48, 0x56, 0x2D, 0x55, 0x50, 0x44, 0x49, 0x3A }; // "HV-UPDI:"

	// Simply return if PGC is 0
	if( !aswRead_PGC() ) return false;

	wdt_reset();

	// Check the trigger character sequence
	for(uint8_t i = 0; i < sizeof(triggerKeyPrefix); ++i) {

		// Get the trigger character
		const uint8_t tkch = triggerKeyPrefix[i];

		// Check the trigger character
		uint8_t mask = 0b10000000;

		for(uint8_t b = 0; b < 8; ++b) {

			// Get PGD
			const bool pgd = aswRead_PGD();

			// Wait for PGC ╽
			uint32_t begTick = tickGet();

			while( aswRead_PGC() ) {
				wdt_reset();
				if( ( tickGet() - begTick ) > DETECT_PGC_TIMEOUT_MS ) return false;
			}

			// Compare the bit
			const bool ref = (tkch & mask) != 0;

			if( (pgd && !ref) || (!pgd && ref) ) {
				wdt_reset();
				return false;
			}

			// Shift the mask
			mask >>= 1;

			// Wait for PGC ╿
			begTick = tickGet();

			while( !aswRead_PGC() ) {
				wdt_reset();
				if( ( tickGet() - begTick ) > DETECT_PGC_TIMEOUT_MS ) return false;
			}

		} // for

	} // for

	// Get the command type (the '?')
	_pgcdCmd = 0;

	for(uint8_t b = 0; b < 8; ++b) {

		// Get PGD
		const bool pgd = aswRead_PGD();

		// Wait for PGC ╽
		uint32_t begTick = tickGet();

		while( aswRead_PGC() ) {
			wdt_reset();
			if( ( tickGet() - begTick ) > DETECT_PGC_TIMEOUT_MS ) return false;
		}

		// Set the bit
		        _pgcdCmd <<= 1;
		if(pgd) _pgcdCmd  |= 0b00000001;

		// Wait for PGC ╿
		begTick = tickGet();

		while( !aswRead_PGC() ) {
			wdt_reset();
			if( ( tickGet() - begTick ) > DETECT_PGC_TIMEOUT_MS ) return false;
		}

	} // for

	// Check the command type
	const bool match = (_pgcdCmd == 0x4E) || (_pgcdCmd == 0x45); // 'N' or 'E'

	wdt_reset();

	// Return the result
	return match;
}
