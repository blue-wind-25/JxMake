/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// UART1 pins
#define RXD1_DDR  DDRD
#define RXD1_PORT PORTD
#define RXD1_BIT  PD2

#define TXD1_DDR  DDRD
#define TXD1_PORT PORTD
#define TXD1_BIT  PD3


// Initialize UART1
static inline void uart1Init()
{
	// Set frame format: even parity, 2 stop bit, 8-bit character size
	UCSR1C = _BV(UPM11) | _BV(USBS1) | _BV(UCSZ11) | _BV(UCSZ10);

	// Set baudrate (1X mode);
	const uint16_t ubrr = ( (F_CPU + 8UL * UPDI_HW_UART_BAUD) / (16UL * UPDI_HW_UART_BAUD) - 1UL );

	UBRR1H = (uint8_t) (ubrr >> 8);
	UBRR1L = (uint8_t) (ubrr >> 0);

	// Set UART1 TX pin as output and drive it high
	TXD1_PORT |= _BV(TXD1_BIT);
	TXD1_DDR  |= _BV(TXD1_BIT);

	// Print message
	printIMsgDone( PSTR("UART1") );
}


// Enable or disable UART1 TX
static __force_inline void uart1Ena_Tx() { UCSR1B |=  _BV(TXEN1); }
static __force_inline void uart1Dis_Tx() { UCSR1B &= ~_BV(TXEN1); }


// Set the value of UART1 TX pin manually
static __force_inline void uart1_Tx1() { TXD1_PORT |=  _BV(TXD1_BIT); } // Output 1
static __force_inline void uart1_Tx0() { TXD1_PORT &= ~_BV(TXD1_BIT); } // Output 0


// Transmit one character to the UART1
static __force_inline void uart1Tx(const char data)
{
	while( !( UCSR1A & _BV(UDRE1) ) ) wdt_reset();

	UDR1 = data;
}


// Transmit multiple characters to the UART1
static inline void uart1TxMulti(const uint8_t* value, uint8_t len)
{
	//cli();
	wdt_reset();

	// NOTE : When sending the maximum number of bytes (254) at UPDI_HW_UART_BAUD,
	//        this line should finish within the watchdog timeout period
	for(uint8_t i = 0; i < len; ++i) uart1Tx(*value++);

	wdt_reset();
	//sei();
}
