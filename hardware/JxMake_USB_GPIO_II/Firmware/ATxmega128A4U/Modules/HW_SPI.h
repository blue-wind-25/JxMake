/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__HW_SPI_H__
#define __MODULES__HW_SPI_H__


extern uint16_t _swspiDelay;


extern bool hwspi_begin(uint8_t spiMode, bool ssMode, uint8_t clkDiv);
extern void hwspi_end(void);

extern bool hwspi_setClkDiv(uint8_t clkDiv);
extern bool hwspi_setSPIMode(uint8_t spiMode);

extern bool hwspi_isEnabled(void);

extern bool hwspi_selectSlave(void);
extern bool hwspi_deselectSlave(void);

extern void swspi_transfer_msb(uint8_t* buff, _SInt_IZArg_t size);
extern void swspi_transfer_lsb(uint8_t* buff, _SInt_IZArg_t size);

extern bool hwspi_transfer(uint8_t* buff, _SInt_IZArg_t size);

extern bool hwspi_transfer_w16Nd_r16dN(
	uint8_t       wAfterDelayUs25_wSPIMode,
	_SInt_IZArg_t wSize,
	uint8_t       rInterDelayUs10_rSPIMode,
	_SInt_IZArg_t rSize,
	uint8_t       rDummyValue,
	uint8_t*      rwBuff
);

extern bool hwspi_setBreak(uint8_t mosi, uint8_t sclk);
extern int8_t hwspi_setBreakExt(uint8_t mosi, uint8_t sclk);
extern bool hwspi_clrBreak(void);

extern bool hwspi_xb_transfer(uint8_t ioocc_eoocc, uint8_t vvvvvvvv, uint8_t* buff, _SInt_IZArg_t nPairs);
extern int8_t hwspi_xb_special(uint8_t type);


#endif // __MODULES__HW_SPI_H__
