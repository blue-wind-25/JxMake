/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __PROTOCOL_HANDLER_SECONDARY_H__
#define __PROTOCOL_HANDLER_SECONDARY_H__


extern bool initSecondary(uint8_t grwbClaimerID); // This function will also set the UART/USRT receive buffer
extern void uninitSecondary(uint8_t grwbClaimerID);

extern void handleSecondary(USB_ClassInfo_CDC_Device_t* const dev);

extern bool secondaryRdBufIsEmpty(void);
extern bool secondaryWrBufIsEmpty(void);


#endif

