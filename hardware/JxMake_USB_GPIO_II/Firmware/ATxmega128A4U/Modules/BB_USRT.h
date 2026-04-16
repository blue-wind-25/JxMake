/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__BB_USRT_H__
#define __MODULES__BB_USRT_H__


#define BB_USRT_P_NONE 0
#define BB_USRT_P_EVEN 2
#define BB_USRT_P_ODD  3


////////////////////////////////////////////////////////////////////////////////////////////////////


extern bool bbusrt_begin(uint8_t parityMode, uint8_t numStopBits, bool ssMode, uint32_t baudrate);
extern void bbusrt_end(void);

extern bool bbusrt_isEnabled(void);

extern bool bbusrt_selectSlave(void);
extern bool bbusrt_deselectSlave(void);

extern bool bbusrt_pulseXck(uint8_t count, bool txValue);

extern bool bbusrt_tx(const uint8_t* buff, _SInt_IZArg_t size);
extern bool bbusrt_rx(uint8_t* buff, _SInt_IZArg_t size);


#endif // __MODULES__BB_USRT_H__
