/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// UART0 pins
#define RXD0_DDR  DDRD
#define RXD0_PORT PORTD
#define RXD0_BIT  PD0

#define TXD0_DDR  DDRD
#define TXD0_PORT PORTD
#define TXD0_BIT  PD1


// Initialize UART0
static __force_inline void __uart0Init()
{
	// Set frame format: no parity, 1 stop bit, 8-bit character size
	UCSR0C = _BV(UCSZ01) | _BV(UCSZ00);

	// Set baudrate (2X mode);
	const uint16_t ubrr = ( (F_CPU + 4UL * DEBUG_HW_UART_BAUD) / (8UL * DEBUG_HW_UART_BAUD) - 1UL );

	UCSR0A = _BV(U2X0);

	UBRR0H = (uint8_t) (ubrr >> 8);
	UBRR0L = (uint8_t) (ubrr >> 0);

	// Enable the transmitter only
	UCSR0B = _BV(TXEN0);
}


// Transmit one character to the UART0
static __force_inline void uart0Tx(const char data)
{
	while( !( UCSR0A & _BV(UDRE0) ) ) wdt_reset();

	UDR0 = data;
}


// Initialize UART0 and stream
static __never_inline int __uart0PutChar(const char ch, FILE* stream)
{
	uart0Tx(ch);

	wdt_reset();

	return 0;
}

static inline void uart0DebugStreamInit()
{
	// Initialize UART0
	__uart0Init();

	// Initialize stream
	static FILE stream = FDEV_SETUP_STREAM(__uart0PutChar, 0, _FDEV_SETUP_RW);

	stdout = &stream;

	// Print message
	printIMsgTitle();
	printIMsgDone( PSTR("UART0 and Debug Stream") );
}
