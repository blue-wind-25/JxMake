/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__BB_SWIM_H__
#define __MODULES__BB_SWIM_H__


#include "HW_SPI.h"


extern bool bbswim_begin(void);
extern void bbswim_end(void);

extern bool bbswim_isEnabled(void);

extern bool bbswim_lineReset(void);

/*
extern bool bbswim_transfer(uint8_t* buff, _SInt_IZArg_t size, _SInt_IZArg_t size2);
*/

extern bool bbswim_cmd_srst(void);
extern bool bbswim_cmd_rotf(uint8_t* buff, _SInt_IZArg_t size, uint8_t addrE, uint8_t addrH, uint8_t addrL);
extern bool bbswim_cmd_wotf(const uint8_t* buff, _SInt_IZArg_t size, uint8_t addrE, uint8_t addrH, uint8_t addrL);


#endif // __MODULES__BB_SWIM_H__

