/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __LED_H__
#define __LED_H__


#include "util.h"


typedef enum {
    BLINK_WIFI_WAIT   = 0,
    BLINK_SERVER_WAIT = 1,
    BLINK_HEARTBEAT   = 2
} BlinkPattern;


typedef enum  {
    ERR_WIFI_INIT           =  2,
    ERR_WIFI_CONNECT_START  =  3,
    ERR_WIFI_CONNECT_FAIL   =  4,
    ERR_WIFI_GET_IP_FAIL    =  5,
    ERR_BRIDGE0_SERVER_FAIL =  6,
    ERR_BRIDGE1_SERVER_FAIL =  7,
    ERR_BRIDGE2_SERVER_FAIL =  8,
    ERR_CONSOLE_SERVER_FAIL =  9,
    ERR_MONITOR_SERVER_FAIL = 10,
    ERR_USB_ENUM_FAIL       = 11,
    ERR_USB_RESTART_FAIL    = 12
} ErrorBlinkPattern;


extern void blinkActivityLED(BlinkPattern mode);

extern __no_return__ void blinkErrorLED(ErrorBlinkPattern pattern);


#endif // __LED_H__
