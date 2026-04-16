/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__HW_TWI_H__
#define __MODULES__HW_TWI_H__


#define TWI_PORT           PORTD
#define TWI_SDA_BIT        1
#define TWI_SCL_BIT        0

#define TWI_PULLUP_DDR     DDRB
#define TWI_PULLUP_PORT    PORTB
#define TWI_PULLUP_SDA_BIT 4
#define TWI_PULLUP_SCL_BIT 5


extern bool hwtwi_begin(uint32_t sclFrequency, uint8_t timeoutMS, bool enableExternalPullUp);
extern void hwtwi_end(void);

extern bool hwtwi_isEnabled(void);

extern bool hwtwi_write(uint8_t slaveAddress, const uint8_t* data, _SInt_IZArg_t len, bool sendStop);
extern bool hwtwi_read(uint8_t slaveAddress, uint8_t* data, _SInt_IZArg_t len, bool sendStop);


#endif // __MODULES__HW_TWI_H__
