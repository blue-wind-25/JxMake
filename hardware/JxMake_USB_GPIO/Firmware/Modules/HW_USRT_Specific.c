/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include "HW_UART_USRT.h"


bool hwusrt_begin(void)
{ return _hwxuart_begin(true, false); }


bool hwusrt_begin_pdi(void)
{ return _hwxuart_begin(true, true); }


void hwusrt_end(void)
{ return _hwxuart_end(); }


void hwusrt_updateFromDefferedConfiguration(void)
{ _hwxuart_updateFromDefferedConfiguration(); }
