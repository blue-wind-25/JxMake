/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__HW_UART_USRT_H__
#define __MODULES__HW_UART_USRT_H__


#define HW_UXRT_DDR     DDRD
#define HW_UXRT_PORT    PORTD
#define HW_UXRT_PIN     PIND

#define HW_UXRT_XCK_BIT 5
#define HW_UXRT_TXD_BIT 3
#define HW_UXRT_RXD_BIT 2


// 7        6        5      4      3      2       1       0
// UMSEL1   UMSEL0   UPM1   UPM0   USBS   UCSZ1   UCSZ0   UCPOL
// .        .        P      P      S      C       C       .

typedef enum _UXRT_ParityMode {
    //                   ..PPSCC.
	UXRT_Parity_None = 0b00000000,
	UXRT_Parity_Even = 0b00100000,
	UXRT_Parity_Odd  = 0b00110000
} UXRT_ParityMode;

typedef enum _UXRT_NumStopBit {
    //                   ..PPSCC.
	UXRT_StopBit_1   = 0b00000000,
	UXRT_StopBit_2   = 0b00001000
} UXRT_NumStopBit;

typedef enum _UXRT_CharacterSize {
    //                    ..PPSCC.
	UXRT_CharSize_5   = 0b00000000,
	UXRT_CharSize_6   = 0b00000010,
	UXRT_CharSize_7   = 0b00000100,
	UXRT_CharSize_8   = 0b00000110
} UXRT_CharacterSize;

//                                           ..PPSCC.
static const uint8_t UXRT_DataFormatMask = 0b00111110;


////////////////////////////////////////////////////////////////////////////////////////////////////


extern void hwuxrt_setReceiveBuffer(RingBuffer_t* rbuff);
extern bool hwuxrt_hasReceiveBuffer(void);

extern bool hwuxrt_isEnabled(void);
extern bool hwuxrt_isInSyncMode(void);

extern void hwuxrt_deferSetBaudrate(uint32_t br);
extern void hwuxrt_deferSetParityMode(UXRT_ParityMode pm);
extern void hwuxrt_deferSetNumStopBit(UXRT_NumStopBit nsb);
extern void hwuxrt_deferSetCharacterSize(UXRT_CharacterSize cs);

//extern bool hwuxrt_isSendReady(void);
extern void hwuxrt_sendByte(uint8_t value);

extern void hwuxrt_setBreak(void);
extern void hwuxrt_clearBreak(void);

extern void hwuxrt_enableTx(void);
extern void hwuxrt_disableTx(void);
extern void hwuxrt_disableTxAfter(uint8_t nb);


////////////////////////////////////////////////////////////////////////////////////////////////////


// Include the specific header files
#include "HW_UART_Specific.h"
#include "HW_USRT_Specific.h"

#include "SS_USRT_Support.h"


#endif // __MODULES__HW_UART_USRT_H__
