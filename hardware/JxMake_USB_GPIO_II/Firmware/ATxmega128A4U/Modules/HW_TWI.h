/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__HW_TWI_H__
#define __MODULES__HW_TWI_H__


extern bool hwtwi_begin(uint32_t sclFrequency, uint8_t timeoutMS, bool enableExternalPullUp);
extern void hwtwi_end(void);

extern bool hwtwi_isEnabled(void);

extern bool hwtwi_write(uint8_t slaveAddress, const uint8_t* data, _SInt_IZArg_t len, bool sendStop);
extern bool hwtwi_read(uint8_t slaveAddress, uint8_t* data, _SInt_IZArg_t len, bool sendStop);

extern bool hwtwi_scan(uint8_t* result128);

extern bool hwtwi_write_one_cf(uint32_t sclFrequency, uint8_t slaveAddress, uint8_t data);
extern bool hwtwi_read_one_cf(uint32_t sclFrequency, uint8_t slaveAddress, uint8_t* data);

#endif // __MODULES__HW_TWI_H__
