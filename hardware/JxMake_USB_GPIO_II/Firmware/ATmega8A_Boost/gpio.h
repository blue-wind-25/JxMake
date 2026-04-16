/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// The pin used to monitor the nRST signal from upstream
#define RST_DDR           DDRB
#define RST_PORT          PORTB
#define RST_PIN           PINB
#define RST_BIT           PB0   // Active low

// The pin used to read the nTRIGGER button
#define TRI_DDR           DDRB
#define TRI_PORT          PORTB
#define TRI_PIN           PINB
#define TRI_BIT           PB2   // Active low

// The pins used to control the target's power and reset
#define TRG_DDR           DDRC
#define TRG_PORT          PORTC
#define TRG_PIN           PINC
#define TRG_BIT_EN_VDD    PC3   // Active high
#define TRG_BIT_RST_HV    PC4   // ---
#define TRG_BIT_RST_LV    PC5   // ---

// The pins used to read the values of the DIP switch
#define DIP_DDR           DDRD
#define DIP_PORT          PORTD
#define DIP_PIN           PIND
#define DIP_BIT_13V       PD2   // Active low
#define DIP_BIT_12V       PD3   // ---
#define DIP_BIT_11V       PD4   // ---
#define DIP_BIT_10V       PD5   // ---
#define DIP_BIT_09V       PD6   // ---
#define DIP_BIT_08V       PD7   // ---
#define DIP_BIT_MASK      0xFC  // 0b11111100

// Pins designated for additional signaling between this section and the 'AVR-UPDI Section'
#define SPC_DDR           DDRC
#define SPC_PORT          PORTC
#define SPC_PIN           PINC
#define SPC_BIT_STOP      PC1   // Active low
#define SPC_BIT_READY     PC2   // Active low


// Initialize GPIOs
static inline void gpioInit()
{
	// Configure the pin used to monitor the nRST signal from upstream as an input with pull-up
	RST_DDR  &= ~_BV(RST_BIT);
	RST_PORT |=  _BV(RST_BIT);

	// Configure the pin used to read the nTRIGGER button as an input with pull-up
	TRI_DDR  &= ~_BV(TRI_BIT);
	TRI_PORT |=  _BV(TRI_BIT);

	// Configure the pins used to control the target's power and reset as outputs
	// and set them to their inactive states
	TRG_PORT &= ~( _BV(TRG_BIT_EN_VDD) | _BV(TRG_BIT_RST_HV) | _BV(TRG_BIT_RST_LV) );
	TRG_DDR  |=  ( _BV(TRG_BIT_EN_VDD) | _BV(TRG_BIT_RST_HV) | _BV(TRG_BIT_RST_LV) );

	// Configure the pins used to read the values of the DIP switch as inputs with pull-ups
	DIP_DDR  &= ~( _BV(DIP_BIT_13V) | _BV(DIP_BIT_12V) | _BV(DIP_BIT_11V) | _BV(DIP_BIT_10V) | _BV(DIP_BIT_09V) | _BV(DIP_BIT_08V) );
	DIP_PORT |=  ( _BV(DIP_BIT_13V) | _BV(DIP_BIT_12V) | _BV(DIP_BIT_11V) | _BV(DIP_BIT_10V) | _BV(DIP_BIT_09V) | _BV(DIP_BIT_08V) );

	// Configure the pins for signaling between this section and the 'AVR-UPDI Section'
	SPC_DDR  &= ~_BV(SPC_BIT_STOP ); // Input with pull-up
	SPC_PORT |=  _BV(SPC_BIT_STOP ); // ---

	SPC_PORT |=  _BV(SPC_BIT_READY); // Output in inactive state
	SPC_DDR  |=  _BV(SPC_BIT_READY); // ---
}


// Read and write GPIOs
static __force_inline bool __gget_loWithDebouncing(volatile uint8_t* pinX, uint8_t bitMask)
{
	if( ( *pinX & bitMask ) != 0 ) return false;

	delayMS(SW_DEBOUNCE_DELAY_MS);

	if( ( *pinX & bitMask ) != 0 ) return false;

	while( ( *pinX & bitMask ) == 0 ) wdt_reset();

	return true;
}

static __force_inline bool    gget_nRST          () { return ( RST_PIN & _BV(RST_BIT) ) == 0; }

static __force_inline bool    gget_nTRIGGER      () { return __gget_loWithDebouncing( &TRI_PIN, _BV(TRI_BIT) ); }

static __force_inline void    gset_trgEnaVdd     () { TRG_PORT |=  _BV(TRG_BIT_EN_VDD); }
static __force_inline void    gset_trgDisVdd     () { TRG_PORT &= ~_BV(TRG_BIT_EN_VDD); }

static __force_inline void    gset_trgEnaRstHV   () { TRG_PORT |=  _BV(TRG_BIT_RST_HV); }
static __force_inline void    gset_trgDisRstHV   () { TRG_PORT &= ~_BV(TRG_BIT_RST_HV); }

static __force_inline void    gset_trgEnaRstLV   () { TRG_PORT |=  _BV(TRG_BIT_RST_LV); }
static __force_inline void    gset_trgDisRstLV   () { TRG_PORT &= ~_BV(TRG_BIT_RST_LV); }

static __force_inline uint8_t gget_dipAll        () { return   DIP_PIN & DIP_BIT_MASK           ; }
static __force_inline bool    gget_dipn13V       () { return ( DIP_PIN & _BV(DIP_BIT_13V) ) == 0; }
static __force_inline bool    gget_dipn12V       () { return ( DIP_PIN & _BV(DIP_BIT_12V) ) == 0; }
static __force_inline bool    gget_dipn11V       () { return ( DIP_PIN & _BV(DIP_BIT_11V) ) == 0; }
static __force_inline bool    gget_dipn10V       () { return ( DIP_PIN & _BV(DIP_BIT_10V) ) == 0; }
static __force_inline bool    gget_dipn09V       () { return ( DIP_PIN & _BV(DIP_BIT_09V) ) == 0; }
static __force_inline bool    gget_dipn08V       () { return ( DIP_PIN & _BV(DIP_BIT_08V) ) == 0; }

static __force_inline bool    gget_nBoostStop    () { return ( SPC_PIN & _BV(SPC_BIT_STOP) ) == 0; }
static __force_inline void    gset_nBoostReady   () { SPC_PORT &=  ~_BV(SPC_BIT_READY);            }
static __force_inline void    gset_nBoostNotReady() { SPC_PORT |=   _BV(SPC_BIT_READY);            }
