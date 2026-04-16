/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __PROTOCOL_HANDLER_PRIMARY_H__
#define __PROTOCOL_HANDLER_PRIMARY_H__


extern void initPrimary(void);

extern void handlePrimary(USB_ClassInfo_CDC_Device_t* const dev);


#endif

