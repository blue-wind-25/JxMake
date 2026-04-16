/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __MODULES__HW_UART_SPECIFIC_H__
#define __MODULES__HW_UART_SPECIFIC_H__


extern bool hwuart_begin(void);
extern void hwuart_end(void);

extern void hwuart_updateFromDefferedConfiguration(void);


#endif // __MODULES__HW_UART_SPECIFIC_H__
