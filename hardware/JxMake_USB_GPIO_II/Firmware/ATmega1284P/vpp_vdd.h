/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Enable and disable Vpp-Vdd
static __force_inline void __vxxEnaVppVdd()
{
	if( !boostEnabled() ) aswEna_RstLV();
	else                  aswEna_RstHV();

	// ##### ??? TODO : Do we need a delay at this point ??? #####
	//delayUS(1);

	aswEna_VddTrg();
}

static __force_inline void __vxxDisVppVdd()
{
	aswDis_VddTrg();

	// ##### ??? TODO : Do we need a delay at this point ??? #####
	//delayUS(1);

	if( !boostEnabled() ) aswDis_RstLV();
	else                  aswDis_RstHV();
}


// Update the begin message for the function 'vxxHandle_nRST()'
static char __vxxBeginMsg[96];

static inline void vxxUpdateBeginMessage_vxxHandle_nRST()
{
	wdt_reset();

	if( !boostEnabled() ) {
		sprintf_P( __vxxBeginMsg, PSTR(">>> Reset signal/trigger - begin : LV\n" ) );
	}

	else {
		char vS[VOLTAGE_TO_STRING_BUFFER_SIZE];
		char vR[VOLTAGE_TO_STRING_BUFFER_SIZE];
		utilStrPrintVoltage( vS, __prev_VBoostSet );
		utilStrPrintVoltage( vR, adcReadVBoost()  );
		sprintf_P( __vxxBeginMsg, PSTR(">>> Reset signal/trigger - begin : HV (Set = %s; Read = %s)\n"), vS, vR );
	}

	wdt_reset();
}


// Handles the Vpp/Vdd sequence triggered by the nRST signal or button press (user trigger)
static inline void vxxHandle_nRST()
{
	// Print message
	// ##### !!! TODO : Verify that this really does not interfere with special entry sequences !!! #####
	printIMsgRaw(__vxxBeginMsg);

	// Enable Vpp-Vdd
	__vxxEnaVppVdd();

#if 0
	/*
	 * ##### !!! TODO !!! #####
	 * This interferes with fast-changing signals, such as special entry sequences like:
	 *     + dsPIC30 HV ICSP entry sequence
	 *     + Part of the dsPIC33 LV (E)ICSP entry sequence
	 * Are there alternative debouncing methods that could be used?
	 */
	// Apply debounce delay because the source can be from external signal or button press (user trigger)
	delayMS(SW_DEBOUNCE_DELAY_MS);
#endif

#if 0

	// Wait for operation to complete
	while( btnRead_nRST_nTrg() ) delayMS(0);

	// Disable Vpp-Vdd
	__vxxDisVppVdd();

	// Print message
	// ##### !!! TODO : Verify that this really does not interfere with special entry sequences !!! #####
	printIMsgln( PSTR(">>> Reset signal/trigger - end\n") );

#else

	// Wait for operation to complete
	// NOTE : Some MCUs (e.g., dsPIC30 HV ICSP entry sequence) require multiple pulses
	uint8_t pulseCnt = 1;

	for(;;) {

		// Wait for operation to complete
		while( btnRead_nRST_nTrg() ) delayMS(0);

		// Disable Vpp-Vdd
		__vxxDisVppVdd();

		// Wait briefly and check if additional pulses need to be sent
		delayUS(10);
		if( !btnRead_nRST_nTrg() ) break;

		++pulseCnt;

		// Enable Vpp-Vdd
		__vxxEnaVppVdd();

	} // for

	// Print message
	// ##### !!! TODO : Verify that this really does not interfere with special entry sequences !!! #####
	printIMsgln( PSTR(">>> Reset signal/trigger - end : pulse(s) = %u\n"), pulseCnt );

#endif
}
