/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Debounce delay
#define SW_DEBOUNCE_DELAY_MS 100


// Pins for reading the trigger buttons
#define BTN_TRG_RST_HLV_DDR  DDRB
#define BTN_TRG_RST_HLV_PORT PORTB
#define BTN_TRG_RST_HLV_PIN  PINB
#define BTN_TRG_RST_HLV_BIT  PB0   // Active low (shared by the trigger button and the 'nRST/nTRIGGER' signal)

#define BTN_TRG_UPDI_NV_DDR  DDRB
#define BTN_TRG_UPDI_NV_PORT PORTB
#define BTN_TRG_UPDI_NV_PIN  PINB
#define BTN_TRG_UPDI_NV_BIT  PB7   // Active low

#define BTN_TRG_UPDI_CE_DDR  DDRC
#define BTN_TRG_UPDI_CE_PORT PORTC
#define BTN_TRG_UPDI_CE_PIN  PINC
#define BTN_TRG_UPDI_CE_BIT  PC0   // Active low


// Initialize the buttons
static inline void btnInit()
{
	// Configure the pins used for the buttons as inputs with pull-ups
	BTN_TRG_RST_HLV_DDR  &= ~_BV(BTN_TRG_RST_HLV_BIT);
	BTN_TRG_RST_HLV_PORT |=  _BV(BTN_TRG_RST_HLV_BIT);

	BTN_TRG_UPDI_NV_DDR  &= ~_BV(BTN_TRG_UPDI_NV_BIT);
	BTN_TRG_UPDI_NV_PORT |=  _BV(BTN_TRG_UPDI_NV_BIT);

	BTN_TRG_UPDI_CE_DDR  &= ~_BV(BTN_TRG_UPDI_CE_BIT);
	BTN_TRG_UPDI_CE_PORT |=  _BV(BTN_TRG_UPDI_CE_BIT);

	// Print message
	printIMsgDone( PSTR("Buttons") );
}


// Read the nRST signal (shared by the trigger button and the 'nRST/nTRIGGER' signal; hence, no debouncing)
static __force_inline bool btnRead_nRST_nTrg() { return ( BTN_TRG_RST_HLV_PIN & _BV(BTN_TRG_RST_HLV_BIT) ) == 0; }


// Read the UPDI nTRIGGERs
static __force_inline bool __btnRead_loWithDebouncing(volatile uint8_t* pinX, uint8_t bitMask)
{
	if( ( *pinX & bitMask ) != 0 ) return false;

	delayMS(SW_DEBOUNCE_DELAY_MS);

	if( ( *pinX & bitMask ) != 0 ) return false;

	while( ( *pinX & bitMask ) == 0 ) wdt_reset();

	return true;
}

static __force_inline bool btnRead_nUPDI_NV() { return __btnRead_loWithDebouncing( &BTN_TRG_UPDI_NV_PIN, _BV(BTN_TRG_UPDI_NV_BIT) ); }
static __force_inline bool btnRead_nUPDI_CE() { return __btnRead_loWithDebouncing( &BTN_TRG_UPDI_CE_PIN, _BV(BTN_TRG_UPDI_CE_BIT) ); }
