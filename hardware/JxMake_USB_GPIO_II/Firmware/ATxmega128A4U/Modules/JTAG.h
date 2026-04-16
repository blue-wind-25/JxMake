/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__JTAG_H__
#define __MODULES__JTAG_H__


extern bool jtag_begin(uint8_t clkDiv);
extern void jtag_end(void);

extern bool jtag_setClkDiv(uint8_t clkDiv);

extern bool jtag_isEnabled(void);

extern bool jtag_setReset(bool nRST, bool nTRST, bool TDI);

extern bool jtag_tms(bool nRST, bool nTRST, bool TDI, uint8_t bitCnt, uint8_t value);

extern bool jtag_transfer(bool xUpdate, bool drShift, bool irShift, uint8_t bitCntLast, uint8_t* buff, _SInt_IZArg_t size);

extern bool jtag_xb_transfer(bool xUpdate, bool drShift, bool irShift, uint8_t* buff, _SInt_IZArg_t nPairs);


#endif // __MODULES__JTAG_H__

