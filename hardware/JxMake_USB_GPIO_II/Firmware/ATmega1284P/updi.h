/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// INT0 ISR
static volatile uint32_t __updi_tick  = 0;
static volatile bool     __updi_disI0 = false;
static volatile bool     __updi_sense = false;

ISR(INT0_vect)
{
	// Update the UPDI tick (no need to disable interrupt here)
	if(__updi_tick != 0) {
		__updi_tick = __timer0_tick;
		if(__updi_tick == 0) __updi_tick = 1;
	}

	// Simply exit if the UPDI sensing is disabled
	else if(__updi_disI0) {
		return;
	}

	// Sense UPDI from the upstream programmer
	else if(!__updi_sense) {
		if( int0Read() ) __updi_sense = true;
	}
}


// Get and clear UPDI tick (atomic)
static __force_inline uint32_t __updiGetTick()
{
	cli();
	const uint32_t tick = __updi_tick;
	sei();

	return tick;
}


static __force_inline void __updiClrTick()
{
	cli();
	__updi_tick = 0;
	sei();
}


// Sense UPDI
static __force_inline bool updiSense()
{ return __updi_sense; }


// Check UPDI state
#define UPDI_TIMEOUT_MS    1000

#define UPDI_STATE_IDLE    0
#define UPDI_STATE_ACTIVE  1
#define UPDI_STATE_TIMEOUT 2

static __force_inline uint8_t updiGetState()
{
	const uint32_t beg = __updiGetTick();

	if(beg == 0) return UPDI_STATE_IDLE;

	const uint32_t cur = tickGet();

	if( (cur - beg) >= UPDI_TIMEOUT_MS ) return UPDI_STATE_TIMEOUT;

	return UPDI_STATE_ACTIVE;
}


// Update the begin message for the function 'updiBegin()'
static char __updiBeginMsg[96];

static inline void updiUpdateBeginMessage()
{
	wdt_reset();

	if( !boostEnabled() ) {
		sprintf_P( __updiBeginMsg, PSTR("... updiBegin() - using LV mode\n") );
	}

	else {
		char vS[VOLTAGE_TO_STRING_BUFFER_SIZE];
		char vR[VOLTAGE_TO_STRING_BUFFER_SIZE];
		utilStrPrintVoltage( vS, __prev_VBoostSet );
		utilStrPrintVoltage( vR, adcReadVBoost()  );
		sprintf_P( __updiBeginMsg, PSTR("... updiBegin() - using HV mode (Set = %s; Read = %s)\n"), vS, vR );
	}

	wdt_reset();
}


// Begin UPDI
static inline void updiBegin(const bool erase)
{
	// ##### !!! TODO : VERIFY FURTHER !!! #####

	static const uint8_t updiKey_NVMPROG[] = {
		// Send the UPDI key
		0x55,                                          // UPDI_PHY_SYNCH
		0xE0,                                          // UPDI_CMD_KEY(UPDI_SEND_KEY, UPDI_KEY_064)
		0x20, 0x67, 0x6F, 0x72, 0x50, 0x4D, 0x56, 0x4E // KEY_NVMPROG
#if 1
		// ##### !!! TODO : Is it really necessary to send these? !!! #####
		,
		// Write the reset signature
		0x55,                                          // UPDI_PHY_SYNCH
		0xC8,                                          // UPDI_CMD_STCS(UPDI_ASI_RESET_REQ)
		0x59,                                          // ASI_RESET_REQ_RSTREQ
		// Clear the reset signature
		0x55,                                          // UPDI_PHY_SYNCH
		0xC8,                                          // UPDI_CMD_STCS(UPDI_ASI_RESET_REQ)
		0x00                                           // 0x00
#endif
	};

	static const uint8_t updiKey_CHIPERASE[] = {
		// Send the UPDI key
		0x55,                                          // UPDI_PHY_SYNCH
		0xE0,                                          // UPDI_CMD_KEY(UPDI_SEND_KEY, UPDI_KEY_064)
		0x65, 0x73, 0x61, 0x72, 0x45, 0x4D, 0x56, 0x4E // KEY_CHIP_ERASE
#if 1
		// ##### !!! TODO : Is it really necessary to send these? !!! #####
		,
		// Write the reset signature
		0x55,                                          // UPDI_PHY_SYNCH
		0xC8,                                          // UPDI_CMD_STCS(UPDI_ASI_RESET_REQ)
		0x59,                                          // ASI_RESET_REQ_RSTREQ
		// Clear the reset signature
		0x55,                                          // UPDI_PHY_SYNCH
		0xC8,                                          // UPDI_CMD_STCS(UPDI_ASI_RESET_REQ)
		0x00                                           // 0x00
#endif
	};

	const uint8_t* updiKey     = erase ?        updiKey_CHIPERASE  :        updiKey_NVMPROG ;
	const uint8_t  updiKeySize = erase ? sizeof(updiKey_CHIPERASE) : sizeof(updiKey_NVMPROG);

	const uint32_t begTickF    = tickGet();

		__updi_disI0 = true;          // Disable the UPDI sensing

		printIMsgln( PSTR("... updiBegin() - using [%S]"), erase ? PSTR("KEY_CHIP_ERASE") : PSTR("KEY_NVMPROG") );

		printIMsgRaw(__updiBeginMsg);

	if( boostEnabled() ) {
		printIMsgln( PSTR("... updiBegin() - enabling Vdd, sending HV pulse, and sending UPDI key") );
	}
	else {
		printIMsgln( PSTR("... updiBegin() - enabling Vdd and sending UPDI key") );
	}

		aswEna_VddTrg();              // Turn on the target's Vdd
		delayMS(2);                   // Delay to ensure stable Vdd (according to the datasheet, the GPIO output driver is only disabled for a maximum of 8.8mS)

	if( boostEnabled() ) {
		aswEna_UPDI_HVP();            // Apply high voltage to the target's UPDI pin
		delayUS(500);                 // The datasheet recommends applying a high-voltage pulse for a duration of 100uS to 1mS
		aswDis_UPDI_HVP();            // Remove high voltage from the target's UPDI pin
	}

	const uint32_t begTickK    = tickGet();

		aswEna_UPDI_KEY();            // Connect the UART1 TX pin

		delayUS(10);                  // The datasheet recommends waiting for a duration of 1uS to 10uS
		uart1_Tx0();                  // Initiate a release of the UPDI reset
		__asm__ __volatile__ (        // The datasheet recommends applying a logic 0 pulse for a duration of 200nS to 1uS
			"rjmp .+0 \n\t nop \n\t"  // ---
		);                            // ---
		uart1_Tx1();                  // Remove the logic 0 pulse
		delayUS(200);                 // The datasheet recommends waiting for a duration of  10uS to 200uS
		delayUS(500);                 // The datasheet recommends waiting for a duration of 200uS to  14mS

	#ifdef UPDI_USE_HW_UART
		uart1Ena_Tx();                // Enable UART1 hardware TX
		uart1TxMulti(                 // Send the UPDI key (must be clocked in within a 16.4mS window after the high-voltage event)
			updiKey, updiKeySize      // ---
		);                            // ---
		uart1Dis_Tx();                // Disable UART1 hardware TX
	#else
		bbtx_8e2_multi(               // Send the UPDI key (must be clocked in within a 16.4mS window after the high-voltage event)
			updiKey, updiKeySize      // ---
		);
	#endif

	const uint32_t endTickK    = tickGet();

		uart1_Tx0();                  // Send a UPDI_PHY_BREAK
		delayMS(25);                  // ---
		uart1_Tx1();                  // ---

		printIMsgln( PSTR("... updiBegin() - connecting the upstream UPDI signal from the programmer") );

		aswDis_UPDI_KEY();            // Disconnect the UART1 TX pin
		aswEna_UPDI_UPS();            // Connect the upstream UPDI signal from the programmer

		printIMsgln( PSTR("... updiBegin() - disabling UPDI-SENSE (INT0) pull-up") );

		int0DisPU();                  // Disable UPDI-SENSE (INT0) pull-up

		printIMsgln( PSTR("... updiBegin() - enabling UPDI-TICK (via INT0) by storing the inital the tick value") );

		tickGetTo(&__updi_tick);      // Save the initial UPDI tick

		__updi_sense = false;         // Clear the flag

	const uint32_t endTickF    = tickGet();

		printIMsgln( PSTR("... updiBegin() - done [UPDI key time = ~= %lumS; function execution time ~= %lumS]"), endTickK - begTickK, endTickF - begTickF );
}


// End UPDI
static inline void updiEnd()
{
	const uint32_t endTick = tickGet();

		printIMsgln( PSTR("... updiEnd() - disabling UPDI-TICK (via INT0) by clearing the tick value") );

		__updiClrTick();              // Clear the UPDI tick

		printIMsgln( PSTR("... updiEnd() - enabling UPDI-SENSE (INT0) pull-up") );

		int0EnaPU();                  // Enable UPDI-SENSE (INT0) pull-up

		printIMsgln( PSTR("... updiEnd() - disconnecting the upstream UPDI signal from the programmer") );

		aswDis_UPDI_UPS();            // Disconnect the upstream UPDI signal from the programmer

		printIMsgln( PSTR("... updiEnd() - disabling Vdd") );

		aswDis_VddTrg();              // Turn off the target's Vdd

		printIMsgln( PSTR("... updiEnd() - pausing momentarily to ensure the UPDI line reaches an idle state") );

		delayMS(500);                 // Delay to ensure that the UPDI line is in an idle state

		__updi_disI0 = false;         // Enable the UPDI sensing

		printIMsgln( PSTR("... updiEnd() - done [total function execution time ~= %lumS]"), tickGet() - endTick );
}
