/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __BUFFER_SIZE_H__
#define __BUFFER_SIZE_H__


// DMA buffer size
#define DEBUG_UART_DMA_BUFFER_SIZE 512


#if 0

// Buffer size for handling protocol - the 1st (primary) CDC interface
#define PROTOCOL_USB_READ_WRITE_BUFFER_SIZE  64
#define PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE 255

// Buffer size for handling UART and USRT passthrough - the 2nd (secondary) CDC interface
#define UXRT_PASS_SHARED_TX_RX_BUFFER_SIZE 128

// Buffer size for TWI transceiver (without the SLA+RW̅ byte)
#define TWI_TRANSCEIVER_BUFFER_SIZE PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE

// Check the buffer size
#if PROTOCOL_USB_READ_WRITE_BUFFER_SIZE > 255
#error "PROTOCOL_USB_READ_WRITE_BUFFER_SIZE must be <= 255"
#endif

#if PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE > 255
#error "PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE must be <= 255"
#endif

#else

// Buffer size for handling protocol - the 1st (primary) CDC interface
#define PROTOCOL_USB_READ_WRITE_BUFFER_SIZE 128
#define PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE 512

// Buffer size for handling UART and USRT passthrough - the 2nd (secondary) CDC interface
#define UXRT_PASS_SHARED_TX_RX_BUFFER_SIZE 256

// Buffer size for TWI transceiver (without the SLA+RW̅ byte)
#define TWI_TRANSCEIVER_BUFFER_SIZE PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE

// Check the buffer size
#if PROTOCOL_USB_READ_WRITE_BUFFER_SIZE > 512
#error "PROTOCOL_USB_READ_WRITE_BUFFER_SIZE must be <= 512"
#endif

#if PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE > 512
#error "PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE must be <= 512"
#endif

#endif


// Determine the data types for counters, indexes, etc.
#if PROTOCOL_USB_READ_WRITE_BUFFER_SIZE >= 128
typedef int16_t _SInt_IZUSB_t;
#else
typedef  int8_t _SInt_IZUSB_t;
#endif

#if PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE >= 128
typedef int16_t _SInt_IZArg_t;
#else
typedef  int8_t _SInt_IZArg_t;
#endif


#endif // __BUFFER_SIZE_H__
