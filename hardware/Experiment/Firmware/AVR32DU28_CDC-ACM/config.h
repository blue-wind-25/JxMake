/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef CONFIG_H
#define CONFIG_H


/*
 * AVR16/32/64DU14/28
 *
 * SOIC-14   SSOP-28   PORT   Function      Direction
 *  9          5       PC.3   DTR           OUT
 *
 * 10         10       PD.4   TXD0 (ALT3)   OUT
 * 11         11       PD.5   RXD0 (ALT3)   INP
 *
 * 12         12       PD.6   RTS           OUT
 * 13         13       PD.7   CTS           INP
 *
 *  4         22       PA.0   LED           OUT
 *  5         23       PA.1   LED           OUT
 */

/*
 * NOTE : # This firmware does not support hardware flow control.
 *        # It attempts to emulate direct access to DTR, RTS, and CTS, as well as the break condition,
 *          as accurately as possible so it can be used to program Arduinos, AVR MCUs, and Espressif MCUs.
 */


// The CPU is always run at 24MHz
#define F_CPU           24000000UL

// The UART is connected to USART0 at PD.4 and PD.5
#define UART_DEVICE     USART0
#define UART_PORTMUX_GM PORTMUX_USART0_gm
#define UART_PORTMUX_GC PORTMUX_USART0_ALT3_gc
#define UART_PORT       PORTD
#define UART_TXD_PIN    PIN4_bm
#define UART_TXD_CTRL   PIN4CTRL
#define UART_RXD_PIN    PIN5_bm
#define UART_RXD_CTRL   PIN5CTRL

// The UART DTR pin is connected to PC.3
#define UART_DTR_PORT   PORTC
#define UART_DTR_PIN    PIN3_bm

// The UART RTS pin is connected to PD.6
#define UART_RTS_PORT   PORTD
#define UART_RTS_PIN    PIN6_bm

// The UART CTS pin is connected to PD.7 (the note CTS bits is routed to the DCD, DSR, and RI bits)
#define UART_CTS_PORT   PORTD
#define UART_CTS_PIN    PIN7_bm
#define UART_CTS_CTRL   PIN7CTRL

// The TXD LED is connected to PA.0
#define TXD_LED_PORT    PORTA
#define TXD_LED_PIN     PIN0_bm

// The RXD LED is connected to PA.1
#define RXD_LED_PORT    PORTA
#define RXD_LED_PIN     PIN1_bm


#endif // CONFIG_H
