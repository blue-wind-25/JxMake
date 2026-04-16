/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Pins for reading DIP switch values
#define DIP_DDR         DDRA
#define DIP_PORT        PORTA
#define DIP_PIN         PINA
#define DIP_BIT_13V     PA7   // Active low
#define DIP_BIT_12V     PA6   // ---
#define DIP_BIT_11V     PA5   // ---
#define DIP_BIT_10V     PA4   // ---
#define DIP_BIT_09V     PA3   // ---
#define DIP_BIT_08V     PA2   // ---
#define DIP_BIT_MASK    0xFC  // 0b11111100


// Pins for controlling the boost converter module
#define BCV_DDR         DDRC
#define BCV_PORT        PORTC
#define BCV_SEL_13V_PD  PC7   // Active high
#define BCV_SEL_12V_PD  PC6   // ---
#define BCV_SEL_11V_PD  PC5   // ---
#define BCV_SEL_10V_PD  PC4   // ---
#define BCV_SEL_09V_PD  PC3   // ---
#define BCV_SEL_08V_PD  PC2   // ---
#define BCV_DES_MIN_PU  PC1   // Active low
#define BCV_BIT_MASK    0xFC  // 0b11111100


// Macro for defining the boost converter output voltage range (tolerance -10% and +05%)
#define UV_TOL_P     10UL
#define OV_TOL_P      5UL

#define UV_TOL_F     ( (100UL - UV_TOL_P) * 512UL + 50UL ) / 100UL
#define OV_TOL_F     ( (100UL + OV_TOL_P) * 512UL + 50UL ) / 100UL

#define UV_TOL_V(V)  V_TOL( ( (uint32_t) (V) ) * UV_TOL_F / 512UL )
#define OV_TOL_V(V)  V_TOL( ( (uint32_t) (V) ) * OV_TOL_F / 512UL )

#define V_BOOST_R(V) UV_TOL_V(V), OV_TOL_V(V)


// Set the output voltage of the boost converter module
static bool __boost_Enabled = false;

static inline void boostSetV(const uint8_t vselMask)
{
	/*          7   6   5   4   3   2   1   0
	 * vselMask 13V 12V 11V 10V 09V 08V DNM DNM
	 */

	// Turn off
	BCV_PORT |= _BV(BCV_DES_MIN_PU);
	BCV_PORT &=    ~BCV_BIT_MASK;

	// Set the voltage
	const uint8_t vsm = (vselMask & BCV_BIT_MASK);
	BCV_PORT |= vsm;

	// Turn on as needed
	__boost_Enabled = (vsm != 0);

	if(__boost_Enabled) BCV_PORT &= ~_BV(BCV_DES_MIN_PU);
}


// Check if the boost converter module is enabled
static inline bool boostEnabled()
{ return __boost_Enabled; }


// Initialize the boost converter module controller
static inline void boostInit()
{
	// Configure the pins used for reading DIP switch values as inputs with pull-ups
	DIP_DDR  &= ~( _BV(DIP_BIT_13V) | _BV(DIP_BIT_12V) | _BV(DIP_BIT_11V) | _BV(DIP_BIT_10V) | _BV(DIP_BIT_09V) | _BV(DIP_BIT_08V) );
	DIP_PORT |=  ( _BV(DIP_BIT_13V) | _BV(DIP_BIT_12V) | _BV(DIP_BIT_11V) | _BV(DIP_BIT_10V) | _BV(DIP_BIT_09V) | _BV(DIP_BIT_08V) );

	// Configure the pins used for controlling the boost converter module as outputs initialized with their inactive states
	BCV_PORT &= ~( _BV(BCV_SEL_13V_PD) | _BV(BCV_SEL_12V_PD) | _BV(BCV_SEL_11V_PD) | _BV(BCV_SEL_10V_PD) | _BV(BCV_SEL_09V_PD) | _BV(BCV_SEL_08V_PD)                       );
	BCV_PORT |=  (                                                                                                                                     _BV(BCV_DES_MIN_PU) );
	BCV_DDR  |=  ( _BV(BCV_SEL_13V_PD) | _BV(BCV_SEL_12V_PD) | _BV(BCV_SEL_11V_PD) | _BV(BCV_SEL_10V_PD) | _BV(BCV_SEL_09V_PD) | _BV(BCV_SEL_08V_PD) | _BV(BCV_DES_MIN_PU) );

	// Print message
	printIMsgDone( PSTR("Boost Converter Module Controller") );
}


// Read the DIP switch state
static __force_inline uint8_t boostGetDIPSwitchState()
{
#if 1

	return DIP_PIN & DIP_BIT_MASK;

#else

	// ##### !!! TODO : This interferes with fast-changing signals. Are there alternative debouncing methods that could be used? !!! #####

	uint8_t dsw1 = DIP_PIN & DIP_BIT_MASK;
	uint8_t cnt  = 0;

	for(;;) {

		delayMS(SW_DEBOUNCE_DELAY_MS);
		wdt_reset();

		const uint8_t dsw2 = DIP_PIN & DIP_BIT_MASK;
		if(dsw1 == dsw2) {
			wdt_reset();
			break;
		}

		dsw1 = dsw2;

		if( ++cnt > (2500 / SW_DEBOUNCE_DELAY_MS) ) ledFEHalt();

	} // for

	return dsw1;

#endif
}
