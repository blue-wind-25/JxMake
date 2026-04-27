/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

// Pins for controlling the analog switch module
// WARNING : Only one of these switches may be activated at a time!
#define EN_RST_HV_DDR    DDRD
#define EN_RST_HV_PORT   PORTD
#define EN_RST_HV_PIN    PIND
#define EN_RST_HV_BIT    PD6   // Active high (DG412DY) or low (DG411DY)

#define EN_UPDI_HVP_DDR  DDRD
#define EN_UPDI_HVP_PORT PORTD
#define EN_UPDI_HVP_PIN  PIND
#define EN_UPDI_HVP_BIT  PD4   // Active high (DG412DY) or low (DG411DY)

#define EN_UPDI_KEY_DDR  DDRB
#define EN_UPDI_KEY_PORT PORTB
#define EN_UPDI_KEY_PIN  PINB
#define EN_UPDI_KEY_BIT  PB4   // Active high (DG412DY) or low (DG411DY)

#define EN_UPDI_UPS_DDR  DDRB
#define EN_UPDI_UPS_PORT PORTB
#define EN_UPDI_UPS_PIN  PINB
#define EN_UPDI_UPS_BIT  PB3   // Active high (DG412DY) or low (DG411DY)


// Pins for controlling the MOSFETs
#define EN_VTG_DDR       DDRD
#define EN_VTG_PORT      PORTD
#define EN_VTG_BIT       PD7   // Active high

#define EN_RST_LV_DDR    DDRD
#define EN_RST_LV_PORT   PORTD
#define EN_RST_LV_BIT    PD5   // Active high


// Pins designated for additional signaling between this section and the upstream programmer
#define USC_DDR          DDRB
#define USC_PORT         PORTB
#define USC_PIN          PINB
#define USC_BIT_PGC      PB2
#define USC_BIT_PGD      PB1


// Initialize the analog switch module controller
static bool __dg412 = true;

static inline void aswInit()
{
	// Check whether a DG412DY or DG411DY is being used
	EN_RST_HV_PORT   &= ~_BV(EN_RST_HV_BIT  ); // Disable pull-up
	EN_RST_HV_DDR    &= ~_BV(EN_RST_HV_BIT  ); // Configure as input

	EN_UPDI_HVP_PORT &= ~_BV(EN_UPDI_HVP_BIT); // Disable pull-up
	EN_UPDI_HVP_DDR  &= ~_BV(EN_UPDI_HVP_BIT); // Configure as input

	EN_UPDI_KEY_PORT &= ~_BV(EN_UPDI_KEY_BIT); // Disable pull-up
	EN_UPDI_KEY_DDR  &= ~_BV(EN_UPDI_KEY_BIT); // Configure as input

	EN_UPDI_UPS_PORT &= ~_BV(EN_UPDI_UPS_BIT); // Disable pull-up
	EN_UPDI_UPS_DDR  &= ~_BV(EN_UPDI_UPS_BIT); // Configure as input

	const bool ctl1 = !!( EN_UPDI_KEY_PIN & _BV( EN_UPDI_KEY_BIT) );
	const bool ctl2 = !!( EN_RST_HV_PIN   & _BV( EN_RST_HV_BIT  ) );
	const bool ctl3 = !!( EN_UPDI_HVP_PIN & _BV( EN_UPDI_HVP_BIT) );
	const bool ctl4 = !!( EN_UPDI_UPS_PIN & _BV( EN_UPDI_UPS_BIT) );

	// Check if all control pins read the same value (all high or all low)
	// This indicates which chip variant is installed: DG411DY (active-low) or DG412DY (active-high)
	const bool aEql = !( (ctl1 ^ ctl2) | (ctl1 ^ ctl3) | (ctl1 ^ ctl4) );

	/*
	printIMsg( PSTR("%d %d %d %d : %d %d %d | %d \n"), ctl1, ctl2, ctl3, ctl4, (ctl1 ^ ctl2), (ctl1 ^ ctl3), (ctl1 ^ ctl4), aEql );
	//*/

	if(!aEql) {
		printIMsgFELine( PSTR("Unable to determine whether the 'Analog Switch Module' contains a DG411DY or DG412DY") );
		ledFEHalt();
	}

	if(ctl1) __dg412 = false;
	else     __dg412 = true;

	// Configure the pins as outputs initialized with their inactive states
	if(__dg412) {
		EN_RST_HV_PORT   &= ~_BV(EN_RST_HV_BIT  ); // Output 0
		EN_UPDI_HVP_PORT &= ~_BV(EN_UPDI_HVP_BIT); // ---
		EN_UPDI_KEY_PORT &= ~_BV(EN_UPDI_KEY_BIT); // ---
		EN_UPDI_UPS_PORT &= ~_BV(EN_UPDI_UPS_BIT); // ---
	}
	else {
		EN_RST_HV_PORT   |=  _BV(EN_RST_HV_BIT  ); // Output 1
		EN_UPDI_HVP_PORT |=  _BV(EN_UPDI_HVP_BIT); // ---
		EN_UPDI_KEY_PORT |=  _BV(EN_UPDI_KEY_BIT); // ---
		EN_UPDI_UPS_PORT |=  _BV(EN_UPDI_UPS_BIT); // ---
	}
	EN_UPDI_HVP_DDR  |= _BV(EN_UPDI_HVP_BIT); // Configure as output
	EN_RST_HV_DDR    |= _BV(EN_RST_HV_BIT  ); // ---
	EN_UPDI_KEY_DDR  |= _BV(EN_UPDI_KEY_BIT); // ---
	EN_UPDI_UPS_DDR  |= _BV(EN_UPDI_UPS_BIT); // ---

	EN_VTG_PORT      &= ~_BV(EN_VTG_BIT    ); // Output 0
	EN_VTG_DDR       |=  _BV(EN_VTG_BIT    ); // Configure as output

	EN_RST_LV_PORT   &= ~_BV(EN_RST_LV_BIT ); // Output 0
	EN_RST_LV_DDR    |=  _BV(EN_RST_LV_BIT ); // Configure as output

	// Configure the pins for signaling between this section and the upstream programmer as inputs with pull-ups
	USC_DDR  &= ~( _BV(USC_BIT_PGC) | _BV(USC_BIT_PGD) );
	USC_PORT |=  ( _BV(USC_BIT_PGC) | _BV(USC_BIT_PGD) );

	// Print message
	printIMsgDone( PSTR("Analog Switch Module Controller and Upstream Signaling (PGC/PGD)") );
	if(__dg412) printIMsgln( PSTR("    DG412DY detected") );
	else        printIMsgln( PSTR("    DG411DY detected") );
}


// Enable or disable Vdd_Trg, UPDI HVP, UPDI KEY, and UPDI upstream
static __force_inline void __asw_DisableAll_DGSW()
{
	if(__dg412) {
		EN_RST_HV_PORT   &= ~_BV(EN_RST_HV_BIT  ); // Output 0
		EN_UPDI_HVP_PORT &= ~_BV(EN_UPDI_HVP_BIT); // ---
		EN_UPDI_KEY_PORT &= ~_BV(EN_UPDI_KEY_BIT); // ---
		EN_UPDI_UPS_PORT &= ~_BV(EN_UPDI_UPS_BIT); // ---
	}
	else {
		EN_RST_HV_PORT   |=  _BV(EN_RST_HV_BIT  ); // Output 1
		EN_UPDI_HVP_PORT |=  _BV(EN_UPDI_HVP_BIT); // ---
		EN_UPDI_KEY_PORT |=  _BV(EN_UPDI_KEY_BIT); // ---
		EN_UPDI_UPS_PORT |=  _BV(EN_UPDI_UPS_BIT); // ---
	}
}

static __force_inline void aswEna_VddTrg  () {             EN_VTG_PORT      |=  _BV(EN_VTG_BIT     ); } // Output 1
static __force_inline void aswDis_VddTrg  () {             EN_VTG_PORT      &= ~_BV(EN_VTG_BIT     ); } // Output 0

static __force_inline void aswEna_RstLV   () { __asw_DisableAll_DGSW();
	                                                       EN_RST_LV_PORT   |=  _BV(EN_RST_LV_BIT  ); } // Output 1
static __force_inline void aswDis_RstLV   () {             EN_RST_LV_PORT   &= ~_BV(EN_RST_LV_BIT  ); } // Output 0

static __force_inline void aswEna_RstHV   () { __asw_DisableAll_DGSW();
	                                           if(__dg412) EN_RST_HV_PORT   |=  _BV(EN_RST_HV_BIT  );   // Output 1
	                                           else        EN_RST_HV_PORT   &= ~_BV(EN_RST_HV_BIT  ); } // Output 0
static __force_inline void aswDis_RstHV   () { if(__dg412) EN_RST_HV_PORT   &= ~_BV(EN_RST_HV_BIT  );   // Output 0
	                                           else        EN_RST_HV_PORT   |=  _BV(EN_RST_HV_BIT  ); } // Output 1

static __force_inline void aswEna_UPDI_HVP() { __asw_DisableAll_DGSW();
	                                           if(__dg412) EN_UPDI_HVP_PORT |=  _BV(EN_UPDI_HVP_BIT);   // Output 1
	                                           else        EN_UPDI_HVP_PORT &= ~_BV(EN_UPDI_HVP_BIT); } // Output 0
static __force_inline void aswDis_UPDI_HVP() { if(__dg412) EN_UPDI_HVP_PORT &= ~_BV(EN_UPDI_HVP_BIT);   // Output 0
	                                           else        EN_UPDI_HVP_PORT |=  _BV(EN_UPDI_HVP_BIT); } // Output 1

static __force_inline void aswEna_UPDI_KEY() { __asw_DisableAll_DGSW();
	                                           if(__dg412) EN_UPDI_KEY_PORT |=  _BV(EN_UPDI_KEY_BIT);   // Output 1
	                                           else        EN_UPDI_KEY_PORT &= ~_BV(EN_UPDI_KEY_BIT); } // Output 0
static __force_inline void aswDis_UPDI_KEY() { if(__dg412) EN_UPDI_KEY_PORT &= ~_BV(EN_UPDI_KEY_BIT);   // Output 0
	                                           else        EN_UPDI_KEY_PORT |=  _BV(EN_UPDI_KEY_BIT); } // Output 1

static __force_inline void aswEna_UPDI_UPS() { __asw_DisableAll_DGSW();
	                                           if(__dg412) EN_UPDI_UPS_PORT |=  _BV(EN_UPDI_UPS_BIT);   // Output 1
	                                           else        EN_UPDI_UPS_PORT &= ~_BV(EN_UPDI_UPS_BIT); } // Output 0
static __force_inline void aswDis_UPDI_UPS() { if(__dg412) EN_UPDI_UPS_PORT &= ~_BV(EN_UPDI_UPS_BIT);   // Output 0
	                                           else        EN_UPDI_UPS_PORT |=  _BV(EN_UPDI_UPS_BIT); } // Output 1


// Read PGC and PGD
static __force_inline bool aswRead_PGC() { return ( USC_PIN & _BV(USC_BIT_PGC) ) != 0; }
static __force_inline bool aswRead_PGD() { return ( USC_PIN & _BV(USC_BIT_PGD) ) != 0; }
