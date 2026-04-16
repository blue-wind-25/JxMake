/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// UART pins
#define RXD_DDR DDRD
#define RXD_BIT PD0

#define TXD_DDR DDRD
#define TXD_BIT PD1


// Initialize UART
static inline void uartInit()
{
	// Set frame format: no parity, 1 stop bit, 8-bit character size
	UCSRC = _BV(URSEL) | _BV(UCSZ1) | _BV(UCSZ0);

	// Set baudrate
	const uint16_t ubrr = ( (F_CPU + 8UL * UART_BAUD) / (16UL * UART_BAUD) - 1UL );

	UBRRH = (uint8_t) (ubrr >> 8);
	UBRRL = (uint8_t) (ubrr >> 0);

	// Enable the receiver and transmitter
	UCSRB = _BV(RXEN) | _BV(TXEN);
}


// Transmit one character to the UART
static __force_inline void uartTx(char data)
{
	while( !( UCSRA & _BV(UDRE) ) );

	UDR = data;
}


// Initialize stream
static __never_inline int __uartPutChar(char ch, FILE* stream)
{
    uartTx(ch);

    return 0;
}

static inline void streamInit()
{
	static FILE stream = FDEV_SETUP_STREAM(__uartPutChar, 0, _FDEV_SETUP_RW);

	stdout = &stream;
}
