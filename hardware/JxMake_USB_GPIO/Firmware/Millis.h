/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MILLIS_H__
#define __MILLIS_H__


extern void init_millis(void);
extern uint32_t millis(void);


#if !ENABLE_DEBUG_STREAM

extern void blink_aled(void);

#else

static inline void blink_aled(void) {}

#endif


#endif // __MILLIS_H__
