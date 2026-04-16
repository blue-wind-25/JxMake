/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// The pin used to send the nUPDI_TRIGGER signal to the nRST pin of the 'Boost Converter and AVR-ISP/TPI & PIC-ICSP Section'
#define TRG_DDR       DDRB
#define TRG_PORT      PORTB
#define TRG_BIT       PB0   // Active low

// The pin used to control the UPDI high-voltage pulse directed to the target
#define HVP_DDR       DDRB
#define HVP_PORT      PORTB
#define HVP_BIT       PB1   // Active high

// The pin used to read the nTRIGGER button
#define TRI_DDR       DDRB
#define TRI_PORT      PORTB
#define TRI_PIN       PINB
#define TRI_BIT       PB2   // Active low

// The pin used to sense the UPDI_PHY_BREAK signal from upstream programmer
#define UPB_DDR       DDRD
#define UPB_PORT      PORTD
#define UPB_PIN       PIND
#define UPB_BIT       PD2

// The pin used to send the UPDI key to the target
#define UPK_DDR       DDRD
#define UPK_PORT      PORTD
#define UPK_BIT       PD3

// Pins designated for additional signaling between this section and the 'Boost Converter and AVR-ISP/TPI & PIC-ICSP Section'
#define SPC_DDR       DDRC
#define SPC_PORT      PORTC
#define SPC_PIN       PINC
#define SPC_BIT_STOP  PC1   // Active low
#define SPC_BIT_READY PC2   // Active low

// Pins designated for additional signaling between this section and the upstream programmer
#define USC_DDR       DDRD
#define USC_PORT      PORTD
#define USC_PIN       PIND
#define USC_BIT_PGC   PD6
#define USC_BIT_PGD   PD7


// Initialize GPIOs
static inline void gpioInit()
{
	// Configure the pin used to send the nUPDI_TRIGGER signal as output and set it
	// to its inactive state
	TRG_PORT |=  _BV(TRG_BIT);
	TRG_DDR  |=  _BV(TRG_BIT);

	// Configure the pin used to control the UPDI high-voltage pulse directed to the
	// target and set it to its inactive state
	HVP_PORT &= ~_BV(HVP_BIT);
	HVP_DDR  |=  _BV(HVP_BIT);

	// Configure the pin used to read the nTRIGGER button as an input with pull-up
	TRI_DDR  &= ~_BV(TRI_BIT);
	TRI_PORT |=  _BV(TRI_BIT);

#if 1
	// Configure the pin used to read the UPDI_PHY_BREAK signal as an input with pull-up
	UPB_DDR  &= ~_BV(UPB_BIT);
	UPB_PORT |=  _BV(UPB_BIT);
#else
	// Configure the pin used to read the UPDI_PHY_BREAK signal as an input without pull-up
	UPB_DDR  &= ~_BV(UPB_BIT);
	UPB_PORT &= ~_BV(UPB_BIT);
#endif

	// Configure the pin used to send the UPDI key signal an input without pull-up for now;
	// it will be reconfigured as output when required
	UPK_DDR  &= ~_BV(UPK_BIT);
	UPK_PORT &= ~_BV(UPK_BIT);

	// Configure the pins for signaling between this section and the 'Boost Converter and AVR-ISP/TPI & PIC-ICSP Section'
	SPC_DDR  |=  _BV(SPC_BIT_STOP ); // Output in inactive state
	SPC_PORT |=  _BV(SPC_BIT_STOP ); // ---

	SPC_DDR  &= ~_BV(SPC_BIT_READY); // Input with pull-up
	SPC_PORT |=  _BV(SPC_BIT_READY); // ---

	// Configure the pins for signaling between this section and the upstream programmer
	USC_DDR  &= ~( _BV(USC_BIT_PGC) | _BV(USC_BIT_PGD) ); // Input with pull-up
	USC_PORT |=  ( _BV(USC_BIT_PGC) | _BV(USC_BIT_PGD) ); // ---
}

static __force_inline void gena_UPDI_KEY() { UPK_PORT |= _BV(UPK_BIT); UPK_DDR |=  _BV(UPK_BIT);                            }
static __force_inline void gdis_UPDI_KEY() {                           UPK_DDR &= ~_BV(UPK_BIT); UPK_PORT &= ~_BV(UPK_BIT); }


// Read and write GPIOs
static __force_inline bool __gget_loWithDebouncing(volatile uint8_t* pinX, uint8_t bitMask)
{
	if( ( *pinX & bitMask ) != 0 ) return false;

	delayMS(SW_DEBOUNCE_DELAY_MS);

	if( ( *pinX & bitMask ) != 0 ) return false;

	while( ( *pinX & bitMask ) == 0 ) wdt_reset();

	return true;
}

static __force_inline void    gset_nUPDI_Trg_Ena() { TRG_PORT &= ~_BV(TRG_BIT); }
static __force_inline void    gset_nUPDI_Trg_Dis() { TRG_PORT |=  _BV(TRG_BIT); }

static __force_inline void    gset_UPDI_HVP_Ena () { HVP_PORT |=  _BV(HVP_BIT); }
static __force_inline void    gset_UPDI_HVP_Dis () { HVP_PORT &= ~_BV(HVP_BIT); }

static __force_inline bool    gget_nTRIGGER     () { return __gget_loWithDebouncing( &TRI_PIN, _BV(TRI_BIT) ); }

static __force_inline void    gena_UPDI_SENSE_PU() { UPB_PORT |=  _BV(UPB_BIT); }
static __force_inline void    gdis_UPDI_SENSE_PU() { UPB_PORT &= ~_BV(UPB_BIT); }
static __force_inline bool    gget_UPDI_SENSE   () { return ( UPB_PIN & _BV(UPB_BIT) ) == 0; }

static __force_inline void    gset_UPDI_KEY_0   () { UPK_PORT &= ~_BV(UPK_BIT); }
static __force_inline void    gset_UPDI_KEY_1   () { UPK_PORT |=  _BV(UPK_BIT); }

static __force_inline void    gset_nBoostStop   () { SPC_PORT &=  ~_BV(SPC_BIT_STOP);              }
static __force_inline void    gset_nBoostNotStop() { SPC_PORT |=   _BV(SPC_BIT_STOP);              }
static __force_inline bool    gget_nBoostReady  () { return ( SPC_PIN & _BV(SPC_BIT_READY) ) == 0; }

static __force_inline bool    gget_PGC          () { return ( USC_PIN & _BV(USC_BIT_PGC) ) != 0; }
static __force_inline bool    gget_PGD          () { return ( USC_PIN & _BV(USC_BIT_PGD) ) != 0; }
