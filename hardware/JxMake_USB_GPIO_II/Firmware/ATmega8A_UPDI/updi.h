/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// INT0 ISR
static volatile uint8_t  _updi_disI0 = 0;
static volatile uint32_t _updi_tick  = 0;
static volatile uint8_t  _updi_sense = 0;

ISR(INT0_vect)
{
	// Update the UPDI tick (no need to disable interrupt here)
	if(_updi_tick != 0) {
		_updi_tick = _timer0_tick;
		if(_updi_tick == 0) _updi_tick = 1;
	}

	// Simply exit if the UPDI sensing is disabled
	else if(_updi_disI0 != 0) {
		return;
	}

	// Sense UPDI from the upstream programmer
	else if(_updi_sense == 0) {
		if( gget_UPDI_SENSE() ) _updi_sense = 1;
	}
}


// Get and clear UPDI tick (atomic)
static __force_inline uint32_t _updiGetTick()
{
	cli();
	const uint32_t tick = _updi_tick;
	sei();

	return tick;
}


static __force_inline void _updiClrTick()
{
	cli();
	_updi_tick = 0;
	sei();
}


// Check UPDI state
#define UPDI_TIMEOUT_MS    1000

#define UPDI_STATE_IDLE    0
#define UPDI_STATE_ACTIVE  1
#define UPDI_STATE_TIMEOUT 2

static __force_inline uint8_t updiGetState()
{
	const uint32_t beg = _updiGetTick();

	if(beg == 0) return UPDI_STATE_IDLE;

	const uint32_t cur = tickGet();

	if( (cur - beg) >= UPDI_TIMEOUT_MS ) return UPDI_STATE_TIMEOUT;

	return UPDI_STATE_ACTIVE;
}


// Begin UPDI
static inline void updiBegin(bool erase)
{
	/*
	static const uint8_t updiInit[] = {
		// Disable the collision detection
		0x55,                                          // UPDI_PHY_SYNCH
		0xC3,                                          // UPDI_CMD_STCS(UPDI_CS_CTRLB)
		0x08,                                          // CTRLB_CCDETDIS

		// Enable inter-byte delay and reduce the guard time value (refer to the UPDI_CTRLA_VALUE constant definition)
		0x55,                                          // UPDI_PHY_SYNCH
		0xC2,                                          // UPDI_CMD_STCS(UPDI_CS_CTRLA)
		0x83,                                          // CTRLA_IBDLY | CTRLA_GTVAL_016B
	};
	*/

	static const uint8_t updiKey_NVMPROG[] = {
		// Send the UPDI key
		0x55,                                          // UPDI_PHY_SYNCH
		0xE0,                                          // UPDI_CMD_KEY(UPDI_SEND_KEY, UPDI_KEY_064)
		0x20, 0x67, 0x6F, 0x72, 0x50, 0x4D, 0x56, 0x4E // KEY_NVMPROG
	};

	static const uint8_t updiKey_CHIPERASE[] = {
		// Send the UPDI key
		0x55,                                          // UPDI_PHY_SYNCH
		0xE0,                                          // UPDI_CMD_KEY(UPDI_SEND_KEY, UPDI_KEY_064)
		0x65, 0x73, 0x61, 0x72, 0x45, 0x4D, 0x56, 0x4E // KEY_CHIP_ERASE
	};

	// ##### !!! TODO : VERIFY & STABILIZE !!! #####

	const uint8_t* updiKey     = erase ?        updiKey_CHIPERASE  :        updiKey_NVMPROG ;
	const uint8_t  updiKeySize = erase ? sizeof(updiKey_CHIPERASE) : sizeof(updiKey_NVMPROG);

	const uint32_t begTick     = tickGet();

	_updi_disI0 = 1;              // Disable the UPDI sensing

	wdt_reset(); printf_P( PSTR("... updiBegin() - using [%S]\n"), erase ? PSTR("KEY_CHIP_ERASE") : PSTR("KEY_NVMPROG") );

	wdt_reset(); printf_P( PSTR("... updiBegin() - enabling UART and disabling UPDI-SENSE pull-up\n") );

	gena_UPDI_KEY();              // Enable the software UART pin (it will override the upstream UPDI signal from the programmer)

	gdis_UPDI_SENSE_PU();         // Disable pull-up

	wdt_reset(); printf_P( PSTR("... updiBegin() - enabling Vdd, sending HV pulse, and sending UPDI key\n") );

	gset_nUPDI_Trg_Ena();         // Instruct the 'Boost Converter and AVR-ISP/TPI & PIC-ICSP Section' to turn on the target's Vdd
	delayMS(2);                   // Delay to ensure stable Vdd (according to the datasheet, the GPIO output driver is only disabled for a maximum of 8.8mS)

	gset_UPDI_HVP_Ena();          // Apply high voltage to the target's UPDI pin
	delayUS(500);                 // The datasheet recommends applying a high-voltage pulse for a duration of 100uS to 1mS

	gset_nBoostStop();            // Instruct the 'Boost Converter and AVR-ISP/TPI & PIC-ICSP Section' to stop boosting for added safety
	while( gget_nBoostReady() ) { // Wait for it to stop boosting
		wdt_reset();
	}

	gset_UPDI_HVP_Dis();          // Remove high voltage from the target's UPDI pin
	gset_nBoostNotStop();         // Instruct the 'Boost Converter and AVR-ISP/TPI & PIC-ICSP Section' to start boosting again

	delayUS(10);                  // The datasheet recommends waiting for a duration of 1uS to 10uS
	gset_UPDI_KEY_0();            // Initiate a release of the UPDI reset
	__asm__ __volatile__ (        // The datasheet recommends applying a logic 0 pulse for a duration of 200nS to 1uS
		"rjmp .+0 \n\t nop \n\t"  // ---
	);                            // ---
	gset_UPDI_KEY_1();            // Remove the logic 0 pulse
	delayUS(200);                 // The datasheet recommends waiting for a duration of  10uS to 200uS
	delayUS(200);                 // The datasheet recommends waiting for a duration of 200uS to  14mS

	bbtx_8e2_multi(               // Send the UPDI key (must be clocked in within a 16.4mS window after the high-voltage event)
		updiKey, updiKeySize      // ---
	);                            // ---

	gset_UPDI_KEY_0();            // Send a UPDI_PHY_BREAK
	delayMS(25);                  // ---
	gset_UPDI_KEY_1();            // ---

	wdt_reset(); printf_P( PSTR("... updiBegin() - disabling UART\n") );

	gdis_UPDI_KEY();              // Disable the software UART pin (this will allow the upstream UPDI signal from the programmer to reconnect to the target)

	wdt_reset(); printf_P( PSTR("... updiBegin() - enabling INT0 by storing the inital the tick value\n") );

	tickGetTo(&_updi_tick);       // Save the initial UPDI tick

	wdt_reset(); printf_P( PSTR("... updiBegin() - done [total function execution time ~= %lumS]\n"), tickGet() - begTick );
}


// End UPDI
static inline void updiEnd()
{
	const uint32_t endTick = tickGet();

	wdt_reset(); printf_P( PSTR("... updiEnd() - disabling INT0 by clearing the tick value and enabling UPDI-SENSE pull-up\n") );

	_updiClrTick();               // Clear the UPDI tick

	gena_UPDI_SENSE_PU();         // Enable pull-up

	wdt_reset(); printf_P( PSTR("... updiEnd() - disabling Vdd\n") );

	gset_nUPDI_Trg_Dis();         // Instruct the 'Boost Converter and AVR-ISP/TPI & PIC-ICSP Section' to turn off the target's Vdd

	wdt_reset(); printf_P( PSTR("... updiEnd() - pausing momentarily to ensure the UPDI line reaches an idle state\n") );

	delayMS(500);                 // Delay to ensure that the UPDI line is in an idle state

	_updi_disI0 = 0;              // Enable the UPDI sensing

	wdt_reset(); printf_P( PSTR("... updiEnd() - done [total function execution time ~= %lumS]\n"), tickGet() - endTick );
}
