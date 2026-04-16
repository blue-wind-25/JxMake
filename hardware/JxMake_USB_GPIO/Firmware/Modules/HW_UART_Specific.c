/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include "HW_UART_USRT.h"


bool hwuart_begin(void)
{ return _hwxuart_begin(false, false); }


void hwuart_end(void)
{ return _hwxuart_end(); }


void hwuart_updateFromDefferedConfiguration(void)
{ _hwxuart_updateFromDefferedConfiguration(); }
