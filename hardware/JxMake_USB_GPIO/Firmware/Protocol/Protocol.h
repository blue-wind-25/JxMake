/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __PROTOCOL_PROTOCOL_H__
#define __PROTOCOL_PROTOCOL_H__


#include "../BufferSize.h"


// Protocol version number
static const uint8_t PROTOCOL_VERSION_M = 1;
static const uint8_t PROTOCOL_VERSION_N = 2;
static const uint8_t PROTOCOL_VERSION_R = 0;

// Firmware version number
static const uint8_t FIRMWARE_VERSION_M = 1;
static const uint8_t FIRMWARE_VERSION_N = 0;
static const uint8_t FIRMWARE_VERSION_R = 9;


/*
 * ##############################################################################################################
 * !!! NOTE !!!
 * ##############################################################################################################
 * Synchronize these with:
 *     '<jxmake_src_root>/hardware/JxMake_USB_GPIO-Protocol_Manual.txt'
 *     '<jxmake_src_root>/src/jxm/ugc/USB_GPIO.java'
 * ##############################################################################################################
 */

static const uint8_t         CMD_PREFIX                      = 0xAA; // All commands must be prefixed with this character

static const uint8_t         CMD_RES_ACK                     = 0xF0; // Command status result
static const uint8_t         CMD_RES_NAK                     = 0xFF;

static const uint8_t         CMD_INVALID                     = 0xFF; // Invalid command

static const uint8_t         CMD_PING                        = 0x00; // System   commands - starts from 0x00
static const uint8_t         CMD_GET_PROTOCOL_VERSION        = 0x01;
static const uint8_t         CMD_GET_FIRMWARE_VERSION        = 0x02;
static const uint8_t         CMD_ENABLE_DEBUG_MESSAGE        = 0x05;
static const uint8_t         CMD_RESET                       = 0x08;
static const uint8_t         CMD_RESET_TO_BOOTLOADER         = 0x09;
static const uint8_t         CMD_DETECT                      = 0x0D;

static const uint8_t         CMD_HW_GPIO_SET_MODE            = 0x20; // HW-GPIO  commands - starts from 0x20
static const uint8_t             HW_GPIO_SET_MODE_INP        = 0x00;
static const uint8_t             HW_GPIO_SET_MODE_INP_PU     = 0x01;
static const uint8_t             HW_GPIO_SET_MODE_OUT        = 0x02;
static const uint8_t             HW_GPIO_SET_MODE_NO_CHG     = 0x03;
static const uint8_t         CMD_HW_GPIO_SET_VALUES          = 0x21;
static const uint8_t         CMD_HW_GPIO_GET_VALUES          = 0x22;
static const uint8_t         CMD_HW_GPIO_SET_PWM             = 0x23;
static const uint8_t         CMD_HW_GPIO_GET_ADC             = 0x24;

static const uint8_t         CMD_HW_SPI_ENABLE               = 0x40; // HW-SPI   commands - starts from 0x40
static const uint8_t         CMD_HW_SPI_DISABLE              = 0x41;
static const uint8_t         CMD_HW_SPI_SELECT_SLAVE         = 0x42;
static const uint8_t         CMD_HW_SPI_DESELECT_SLAVE       = 0x43;
static const uint8_t         CMD_HW_SPI_TRANSFER             = 0x44;
static const uint8_t         CMD_HW_SPI_SET_SCK_FREQUENCY    = 0x45;
static const uint8_t         CMD_HW_SPI_SET_SPI_MODE         = 0x46;
static const uint8_t         CMD_HW_SPI_SET_CLR_BREAK        = 0x48;
static const uint8_t         CMD_HW_SPI_SET_CLR_BREAK_EXT    = 0x49;
static const uint8_t         CMD_HW_SPI_XB_TRANSFER          = 0x4A;
static const uint8_t         CMD_HW_SPI_XB_SPECIAL           = 0x4C;
static const uint8_t             HW_SPI_XBS_DSPIC30_HV_ESQ   = 1;
static const uint8_t             HW_SPI_XBS_DSPIC33_LV_ESQ   = 2;
static const uint8_t         CMD_HW_SPI_TRANSFER_W16ND_R16DN = 0x50;
static const uint8_t             HW_SPI_TRANSFER_MAX_SIZE    = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;

static const uint8_t         CMD_HW_TWI_ENABLE               = 0x60; // HW-TWI   commands - starts from 0x60
static const uint8_t         CMD_HW_TWI_DISABLE              = 0x61;
static const uint8_t         CMD_HW_TWI_WRITE                = 0x62;
static const uint8_t         CMD_HW_TWI_WRITE_NO_STOP        = 0x63;
static const uint8_t         CMD_HW_TWI_READ                 = 0x64;
static const uint8_t         CMD_HW_TWI_READ_NO_STOP         = 0x65;
static const uint8_t             HW_TWI_TRANSFER_MAX_SIZE    = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;

static const uint8_t         CMD_HW_UART_ENABLE              = 0x80; // HW-UART  commands - starts from 0x80
static const uint8_t         CMD_HW_UART_DISABLE             = 0x81;
static const uint8_t         CMD_HW_UART_ENABLE_TX           = 0x84;
static const uint8_t         CMD_HW_UART_DISABLE_TX          = 0x85;
static const uint8_t         CMD_HW_UART_DISABLE_TX_AFTER    = 0x86;

static const uint8_t         CMD_HW_USRT_ENABLE              = 0xA0; // HW-USRT commands - starts from 0xA0
static const uint8_t         CMD_HW_USRT_DISABLE             = 0xA1;
static const uint8_t         CMD_HW_USRT_SELECT_SLAVE        = 0xA2;
static const uint8_t         CMD_HW_USRT_DESELECT_SLAVE      = 0xA3;
static const uint8_t         CMD_HW_USRT_ENABLE_TX           = 0xA4;
static const uint8_t         CMD_HW_USRT_DISABLE_TX          = 0xA5;
static const uint8_t         CMD_HW_USRT_DISABLE_TX_AFTER    = 0xA6;

static const uint8_t         CMD_BB_USRT_ENABLE              = 0xB0; // BB-USRT commands - starts from 0xB0
static const uint8_t             BB_USRT_PARITY_NONE         = 0x00;
static const uint8_t             BB_USRT_PARITY_EVEN         = 0x20;
static const uint8_t             BB_USRT_PARITY_ODD          = 0x30;
static const uint8_t             BB_USRT_STOP_BIT_1          = 0x00;
static const uint8_t             BB_USRT_STOP_BIT_2          = 0x08;
static const uint8_t             BB_USRT_SS_ACTIVE_LOW       = 0x00;
static const uint8_t             BB_USRT_SS_ACTIVE_HIGH      = 0x01;
static const uint8_t         CMD_BB_USRT_DISABLE             = 0xB1;
static const uint8_t         CMD_BB_USRT_SELECT_SLAVE        = 0xB2;
static const uint8_t         CMD_BB_USRT_DESELECT_SLAVE      = 0xB3;
static const uint8_t         CMD_BB_USRT_PULSE_XCK           = 0xB4;
static const uint8_t         CMD_BB_USRT_TX                  = 0xB5;
static const uint8_t         CMD_BB_USRT_RX                  = 0xB6;
static const uint8_t             BB_USRT_TX_RX_MAX_SIZE      = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;

static const uint8_t         CMD_BB_SWIM_ENABLE              = 0xD0; // BB-SWIM commands - starts from 0xD0
static const uint8_t         CMD_BB_SWIM_DISABLE             = 0xD1;
static const uint8_t         CMD_BB_SWIM_LINE_RESET          = 0xD2;
static const uint8_t         CMD_BB_SWIM_TRANSFER            = 0xD3;
static const uint8_t         CMD_BB_SWIM_SRST                = 0xD4;
static const uint8_t         CMD_BB_SWIM_ROTF                = 0xD5;
static const uint8_t         CMD_BB_SWIM_WOTF                = 0xD6;
static const uint8_t             BB_SWIM_TRANSFER_MAX_SIZE   = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;


////////////////////////////////////////////////////////////////////////////////////////////////////


#ifdef __PROTOCOL_HANDLER_PRIMARY_C__

// The number of parameter bytes for each command
static const int8_t PROGMEM _CMD_PAR_BYTE_LEN[] = {
	/* 0x00 */  0, // CMD_PING
	/* 0x01 */  0, // CMD_GET_PROTOCOL_VERSION
	/* 0x02 */  0, // CMD_GET_FIRMWARE_VERSION
	/* 0x03 */ -1, // <reserved>
	/* 0x04 */ -1, // <reserved>
	/* 0x05 */  1, // CMD_ENABLE_DEBUG_MESSAGE
	/* 0x06 */ -1, // <reserved>
	/* 0x07 */ -1, // <reserved>
	/* 0x08 */  0, // CMD_RESET
	/* 0x09 */  0, // CMD_RESET_TO_BOOTLOADER
	/* 0x0A */ -1, // <reserved>
	/* 0x0B */ -1, // <reserved>
	/* 0x0C */ -1, // <reserved>
	/* 0x0D */  6, // CMD_DETECT
	/* 0x0E */ -1, // <reserved>
	/* 0x0F */ -1, // <reserved>
	/* 0x10 */ -1, // <reserved>
	/* 0x11 */ -1, // <reserved>
	/* 0x12 */ -1, // <reserved>
	/* 0x13 */ -1, // <reserved>
	/* 0x14 */ -1, // <reserved>
	/* 0x15 */ -1, // <reserved>
	/* 0x16 */ -1, // <reserved>
	/* 0x17 */ -1, // <reserved>
	/* 0x18 */ -1, // <reserved>
	/* 0x19 */ -1, // <reserved>
	/* 0x1A */ -1, // <reserved>
	/* 0x1B */ -1, // <reserved>
	/* 0x1C */ -1, // <reserved>
	/* 0x1D */ -1, // <reserved>
	/* 0x1E */ -1, // <reserved>
	/* 0x1F */ -1, // <reserved>
	/* 0x20 */  3, // CMD_HW_GPIO_SET_MODE
	/* 0x21 */  2, // CMD_HW_GPIO_SET_VALUES
	/* 0x22 */  0, // CMD_HW_GPIO_GET_VALUES
	/* 0x23 */  2, // CMD_HW_GPIO_SET_PWM
	/* 0x24 */  1, // CMD_HW_GPIO_GET_ADC
	/* 0x25 */ -1, // <reserved>
	/* 0x26 */ -1, // <reserved>
	/* 0x27 */ -1, // <reserved>
	/* 0x28 */ -1, // <reserved>
	/* 0x29 */ -1, // <reserved>
	/* 0x2A */ -1, // <reserved>
	/* 0x2B */ -1, // <reserved>
	/* 0x2C */ -1, // <reserved>
	/* 0x2D */ -1, // <reserved>
	/* 0x2E */ -1, // <reserved>
	/* 0x2F */ -1, // <reserved>
	/* 0x30 */ -1, // <reserved>
	/* 0x31 */ -1, // <reserved>
	/* 0x32 */ -1, // <reserved>
	/* 0x33 */ -1, // <reserved>
	/* 0x34 */ -1, // <reserved>
	/* 0x35 */ -1, // <reserved>
	/* 0x36 */ -1, // <reserved>
	/* 0x37 */ -1, // <reserved>
	/* 0x38 */ -1, // <reserved>
	/* 0x39 */ -1, // <reserved>
	/* 0x3A */ -1, // <reserved>
	/* 0x3B */ -1, // <reserved>
	/* 0x3C */ -1, // <reserved>
	/* 0x3D */ -1, // <reserved>
	/* 0x3E */ -1, // <reserved>
	/* 0x3F */ -1, // <reserved>
	/* 0x40 */  1, // CMD_HW_SPI_ENABLE
	/* 0x41 */  0, // CMD_HW_SPI_DISABLE
	/* 0x42 */  0, // CMD_HW_SPI_SELECT_SLAVE
	/* 0x43 */  0, // CMD_HW_SPI_DESELECT_SLAVE
	/* 0x44 */  1, // CMD_HW_SPI_TRANSFER
	/* 0x45 */  1, // CMD_HW_SPI_SET_SCK_FREQUENCY
	/* 0x46 */  1, // CMD_HW_SPI_SET_SPI_MODE
	/* 0x47 */ -1, // <reserved>
	/* 0x48 */  1, // CMD_HW_SPI_SET_CLR_BREAK
	/* 0x49 */  1, // CMD_HW_SPI_SET_CLR_BREAK_EXT
	/* 0x4A */  3, // CMD_HW_SPI_XB_TRANSFER
	/* 0x4B */ -1, // <reserved>
	/* 0x4C */  1, // CMD_HW_SPI_XB_SPECIAL
	/* 0x4D */ -1, // <reserved>
	/* 0x4E */ -1, // <reserved>
	/* 0x4F */ -1, // <reserved>
	/* 0x50 */  5, // CMD_HW_SPI_TRANSFER_W16ND_R16DN
	/* 0x51 */ -1, // <reserved>
	/* 0x52 */ -1, // <reserved>
	/* 0x53 */ -1, // <reserved>
	/* 0x54 */ -1, // <reserved>
	/* 0x55 */ -1, // <reserved>
	/* 0x56 */ -1, // <reserved>
	/* 0x57 */ -1, // <reserved>
	/* 0x58 */ -1, // <reserved>
	/* 0x59 */ -1, // <reserved>
	/* 0x5A */ -1, // <reserved>
	/* 0x5B */ -1, // <reserved>
	/* 0x5C */ -1, // <reserved>
	/* 0x5D */ -1, // <reserved>
	/* 0x5E */ -1, // <reserved>
	/* 0x5F */ -1, // <reserved>
	/* 0x60 */  4, // CMD_HW_TWI_ENABLE
	/* 0x61 */  0, // CMD_HW_TWI_DISABLE
	/* 0x62 */  2, // CMD_HW_TWI_WRITE
	/* 0x63 */  2, // CMD_HW_TWI_WRITE_NO_STOP
	/* 0x64 */  2, // CMD_HW_TWI_READ
	/* 0x65 */  2, // CMD_HW_TWI_READ_NO_STOP
	/* 0x66 */ -1, // <reserved>
	/* 0x67 */ -1, // <reserved>
	/* 0x68 */ -1, // <reserved>
	/* 0x69 */ -1, // <reserved>
	/* 0x6A */ -1, // <reserved>
	/* 0x6B */ -1, // <reserved>
	/* 0x6C */ -1, // <reserved>
	/* 0x6D */ -1, // <reserved>
	/* 0x6E */ -1, // <reserved>
	/* 0x6F */ -1, // <reserved>
	/* 0x70 */ -1, // <reserved>
	/* 0x71 */ -1, // <reserved>
	/* 0x72 */ -1, // <reserved>
	/* 0x73 */ -1, // <reserved>
	/* 0x74 */ -1, // <reserved>
	/* 0x75 */ -1, // <reserved>
	/* 0x76 */ -1, // <reserved>
	/* 0x77 */ -1, // <reserved>
	/* 0x78 */ -1, // <reserved>
	/* 0x79 */ -1, // <reserved>
	/* 0x7A */ -1, // <reserved>
	/* 0x7B */ -1, // <reserved>
	/* 0x7C */ -1, // <reserved>
	/* 0x7D */ -1, // <reserved>
	/* 0x7E */ -1, // <reserved>
	/* 0x7F */ -1, // <reserved>
	/* 0x80 */  0, // CMD_HW_UART_ENABLE
	/* 0x81 */  0, // CMD_HW_UART_DISABLE
	/* 0x82 */ -1, // <reserved>
	/* 0x83 */ -1, // <reserved>
	/* 0x84 */  0, // CMD_HW_UART_ENABLE_TX
	/* 0x85 */  0, // CMD_HW_UART_DISABLE_TX
	/* 0x86 */  1, // CMD_HW_UART_DISABLE_TX_AFTER
	/* 0x87 */ -1, // <reserved>
	/* 0x88 */ -1, // <reserved>
	/* 0x89 */ -1, // <reserved>
	/* 0x8A */ -1, // <reserved>
	/* 0x8B */ -1, // <reserved>
	/* 0x8C */ -1, // <reserved>
	/* 0x8D */ -1, // <reserved>
	/* 0x8E */ -1, // <reserved>
	/* 0x8F */ -1, // <reserved>
	/* 0x90 */ -1, // <reserved>
	/* 0x91 */ -1, // <reserved>
	/* 0x92 */ -1, // <reserved>
	/* 0x93 */ -1, // <reserved>
	/* 0x94 */ -1, // <reserved>
	/* 0x95 */ -1, // <reserved>
	/* 0x96 */ -1, // <reserved>
	/* 0x97 */ -1, // <reserved>
	/* 0x98 */ -1, // <reserved>
	/* 0x99 */ -1, // <reserved>
	/* 0x9A */ -1, // <reserved>
	/* 0x9B */ -1, // <reserved>
	/* 0x9C */ -1, // <reserved>
	/* 0x9D */ -1, // <reserved>
	/* 0x9E */ -1, // <reserved>
	/* 0x9F */ -1, // <reserved>
	/* 0xA0 */  1, // CMD_HW_USRT_ENABLE
	/* 0xA1 */  0, // CMD_HW_USRT_DISABLE
	/* 0xA2 */  0, // CMD_HW_USRT_SELECT_SLAVE
	/* 0xA3 */  0, // CMD_HW_USRT_DESELECT_SLAVE
	/* 0xA4 */  0, // CMD_HW_USRT_ENABLE_TX
	/* 0xA5 */  0, // CMD_HW_USRT_DISABLE_TX
	/* 0xA6 */  1, // CMD_HW_USRT_DISABLE_TX_AFTER
	/* 0xA7 */ -1, // <reserved>
	/* 0xA8 */ -1, // <reserved>
	/* 0xA9 */ -1, // <reserved>
	/* 0xAA */ -1, // <reserved>
	/* 0xAB */ -1, // <reserved>
	/* 0xAC */ -1, // <reserved>
	/* 0xAD */ -1, // <reserved>
	/* 0xAE */ -1, // <reserved>
	/* 0xAF */ -1, // <reserved>
	/* 0xB0 */  4, // CMD_BB_USRT_ENABLE
	/* 0xB1 */  0, // CMD_BB_USRT_DISABLE
	/* 0xB2 */  0, // CMD_BB_USRT_SELECT_SLAVE
	/* 0xB3 */  0, // CMD_BB_USRT_DESELECT_SLAVE
	/* 0xB4 */  2, // CMD_BB_USRT_PULSE_XCK
	/* 0xB5 */  1, // CMD_BB_USRT_TX
	/* 0xB6 */  1, // CMD_BB_USRT_RX
	/* 0xB7 */ -1, // <reserved>
	/* 0xB8 */ -1, // <reserved>
	/* 0xB9 */ -1, // <reserved>
	/* 0xBA */ -1, // <reserved>
	/* 0xBB */ -1, // <reserved>
	/* 0xBC */ -1, // <reserved>
	/* 0xBD */ -1, // <reserved>
	/* 0xBE */ -1, // <reserved>
	/* 0xBF */ -1, // <reserved>
	/* 0xC0 */ -1, // <reserved>
	/* 0xC1 */ -1, // <reserved>
	/* 0xC2 */ -1, // <reserved>
	/* 0xC3 */ -1, // <reserved>
	/* 0xC4 */ -1, // <reserved>
	/* 0xC5 */ -1, // <reserved>
	/* 0xC6 */ -1, // <reserved>
	/* 0xC7 */ -1, // <reserved>
	/* 0xC8 */ -1, // <reserved>
	/* 0xC9 */ -1, // <reserved>
	/* 0xCA */ -1, // <reserved>
	/* 0xCB */ -1, // <reserved>
	/* 0xCC */ -1, // <reserved>
	/* 0xCD */ -1, // <reserved>
	/* 0xCE */ -1, // <reserved>
	/* 0xCF */ -1, // <reserved>
	/* 0xD0 */  0, // CMD_BB_SWIM_ENABLE
	/* 0xD1 */  0, // CMD_BB_SWIM_DISABLE
	/* 0xD2 */  0, // CMD_BB_SWIM_LINE_RESET
	/* 0xD3 */  2, // CMD_BB_SWIM_TRANSFER
	/* 0xD4 */  0, // CMD_BB_SWIM_SRST
	/* 0xD5 */  4, // CMD_BB_SWIM_ROTF
	/* 0xD6 */  4, // CMD_BB_SWIM_WOTF
	/* 0xD7 */ -1, // <reserved>
	/* 0xD8 */ -1, // <reserved>
	/* 0xD9 */ -1, // <reserved>
	/* 0xDA */ -1, // <reserved>
	/* 0xDB */ -1, // <reserved>
	/* 0xDC */ -1, // <reserved>
	/* 0xDD */ -1, // <reserved>
	/* 0xDE */ -1, // <reserved>
	/* 0xDF */ -1, // <reserved>
	/* 0xE0 */ -1, // <reserved>
	/* 0xE1 */ -1, // <reserved>
	/* 0xE2 */ -1, // <reserved>
	/* 0xE3 */ -1, // <reserved>
	/* 0xE4 */ -1, // <reserved>
	/* 0xE5 */ -1, // <reserved>
	/* 0xE6 */ -1, // <reserved>
	/* 0xE7 */ -1, // <reserved>
	/* 0xE8 */ -1, // <reserved>
	/* 0xE9 */ -1, // <reserved>
	/* 0xEA */ -1, // <reserved>
	/* 0xEB */ -1, // <reserved>
	/* 0xEC */ -1, // <reserved>
	/* 0xED */ -1, // <reserved>
	/* 0xEE */ -1, // <reserved>
	/* 0xEF */ -1, // <reserved>
	/* 0xF0 */ -1, // <reserved>
	/* 0xF1 */ -1, // <reserved>
	/* 0xF2 */ -1, // <reserved>
	/* 0xF3 */ -1, // <reserved>
	/* 0xF4 */ -1, // <reserved>
	/* 0xF5 */ -1, // <reserved>
	/* 0xF6 */ -1, // <reserved>
	/* 0xF7 */ -1, // <reserved>
	/* 0xF8 */ -1, // <reserved>
	/* 0xF9 */ -1, // <reserved>
	/* 0xFA */ -1, // <reserved>
	/* 0xFB */ -1, // <reserved>
	/* 0xFC */ -1, // <reserved>
	/* 0xFD */ -1, // <reserved>
	/* 0xFE */ -1, // <reserved>
	/* 0xFF */ -1  // CMD_INVALID
};

#endif


#endif

