/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __UTILS_H__
#define __UTILS_H__


#include <avr/pgmspace.h>

#include <LUFA/Drivers/USB/USB.h>

#include "BufferSize.h"


#define RESET_DELAY_MS 250


#define GBCID_HW_UART 1
#define GBCID_HW_USRT 2

extern uint8_t GenericRBuff[UXRT_PASS_SHARED_TX_RX_BUFFER_SIZE]; // Shared buffer for UART and USRT
extern uint8_t GenericWBuff[UXRT_PASS_SHARED_TX_RX_BUFFER_SIZE]; // ---

extern bool claimGenericRWBuff(uint8_t claimerID);
extern bool unclaimGenericRWBuff(uint8_t claimerID);


extern void resetSystem(void);
extern void resetToBootloader(void);


// ##### ??? TODO : The function 'wdtEnSetKey()' is currently only used in 'Modules/BB_SWIM.c'; should it be applied more broadly ??? #####
extern void wdtEnSetKey(void);
extern void wdtEnClrKey(void);
extern bool wdtEnIsKeySet(void);


extern void waitUntil_Endpoint_IsINReady(USB_ClassInfo_CDC_Device_t* const dev);


#define ATTR_USED   __attribute__ ((used))
#define ATTR_UNUSED __attribute__ ((unused))


#define _SPGM(N, S) static const char PROGMEM ATTR_UNUSED N[] = S
#define _FPGM(P)    ( (__flash_string   )     (P) )

#define _FSTR(S)    ( (__flash_string   ) PSTR(S) )

#define _CCCF(F)    ( (const char* const)     (F) )


typedef const struct __flash_string__{}* const* __flash_string;


#if !!ENABLE_DEBUG_STREAM

	#define DMSG_TYPE_BIT_ERROR       0x01
	#define DMSG_TYPE_BIT_WARNING     0x02
	#define DMSG_TYPE_BIT_NOTICE      0x04
	#define DMSG_TYPE_BIT_INFORMATION 0x08
	#define DMSG_TYPE_BIT_ALWAYS      0xFF

	extern void debug_initStream(void);
	extern void debug_setMsgTypeMask(uint8_t dmsgTypeMask);
	extern void debug_printf(uint8_t dmsgTypeBit, __flash_string format, ...);

	#define __DPRINTF_DECL_PREFIX(FNAME)         static const char PROGMEM __dprintf_prefix[] = FNAME "() - "; do { (void) __dprintf_prefix; } while(0)
	#define __DPRINTF_DECL_PRX_FN(FNAME)         static const char PROGMEM __dprintf_prefix[] = FNAME "()"   ; do { (void) __dprintf_prefix; } while(0)

	#define __DPRINTF_DECL_FORMAT(FNAME, FORMAT) _SPGM(FNAME, "%S" FORMAT "\n")
	#define __DPRINTF_DECL_SPGMVR(VNAME, VVALUE) _SPGM(VNAME, VVALUE)

	#define __DPRINTFF_E(FNAME , ...) debug_printf( DMSG_TYPE_BIT_ERROR      , _FPGM(     FNAME      ), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFF_W(FNAME , ...) debug_printf( DMSG_TYPE_BIT_WARNING    , _FPGM(     FNAME      ), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFF_N(FNAME , ...) debug_printf( DMSG_TYPE_BIT_NOTICE     , _FPGM(     FNAME      ), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFF_I(FNAME , ...) debug_printf( DMSG_TYPE_BIT_INFORMATION, _FPGM(     FNAME      ), __dprintf_prefix, ## __VA_ARGS__ )
#if !!DEBUG_STREAM_USE_AVR_TINY_UART
	#define __DPRINTFF_X(FNAME , ...) debug_printf( DMSG_TYPE_BIT_ALWAYS     , _FPGM(     FNAME      ), __dprintf_prefix, ## __VA_ARGS__ )
#else
	#define __DPRINTFF_X(FNAME , ...) do {} while(0)
#endif

	#define __DPRINTFS_E(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_ERROR      , _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFS_W(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_WARNING    , _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFS_N(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_NOTICE     , _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFS_I(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_INFORMATION, _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
#if !!DEBUG_STREAM_USE_AVR_TINY_UART
	#define __DPRINTFS_X(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_ALWAYS     , _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
#else
	#define __DPRINTFS_X(FORMAT, ...) do {} while(0)
#endif

#else

	static inline void debug_initStream(void) {}
	static inline void debug_setMsgTypeMask(uint8_t dmsgTypeMask) { (void) dmsgTypeMask; }
	static inline void debug_printf(uint8_t dmsgTypeBit, __flash_string format, ...) { (void) dmsgTypeBit; (void) format; }

	#define __DPRINTF_DECL_PREFIX(FNAME)
	#define __DPRINTF_DECL_PRX_FN(FNAME)

	#define __DPRINTF_DECL_FORMAT(FNAME, FORMAT)
	#define __DPRINTF_DECL_SPGMVR(VNAME, VVALUE)

	#define __DPRINTFF_E(FNAME , ...) do {} while(0)
	#define __DPRINTFF_W(FNAME , ...) do {} while(0)
	#define __DPRINTFF_N(FNAME , ...) do {} while(0)
	#define __DPRINTFF_I(FNAME , ...) do {} while(0)
	#define __DPRINTFF_X(FNAME , ...) do {} while(0)

	#define __DPRINTFS_E(FORMAT, ...) do {} while(0)
	#define __DPRINTFS_W(FORMAT, ...) do {} while(0)
	#define __DPRINTFS_N(FORMAT, ...) do {} while(0)
	#define __DPRINTFS_I(FORMAT, ...) do {} while(0)
	#define __DPRINTFS_X(FORMAT, ...) do {} while(0)

#endif


#endif // __UTILS_H__
