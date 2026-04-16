/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__HW_USRT_SPECIFIC_H__
#define __MODULES__HW_USRT_SPECIFIC_H__


extern bool hwusrt_begin(void);
extern bool hwusrt_begin_pdi(void);
extern void hwusrt_end(void);

extern void hwusrt_updateFromDefferedConfiguration(void);


#endif // __MODULES__HW_USRT_SPECIFIC_H__
