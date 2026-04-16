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
#include "IODefs.h"
#include "IOPairParser.h"


// Delay for reset system and reset to bootloader
#define RESET_DELAY_MS 250


// Additional attributes not defined by LUFA
#define ATTR_USED   __attribute__ ((used))
#define ATTR_UNUSED __attribute__ ((unused))


// Macros for wrapping 'case' block
#define __CASE_BEG__ {
#define __CASE_END__ } break;


// Macros for calculating the UXRT BSEL value
#define UXRT_BSEL_1X(Baudrate) ( (F_CPU + 8UL * Baudrate) / (16UL * Baudrate) - 1UL )
#define UXRT_BSEL_2X(Baudrate) ( (F_CPU + 4UL * Baudrate) / ( 8UL * Baudrate) - 1UL )
#define UXRT_BSEL_SY(Baudrate) ( (F_CPU +       Baudrate) / ( 2UL * Baudrate) - 1UL )


// Macros for reading values from the production signature row
#define BEG_READ_PRODROW()      NVM.CMD = NVM_CMD_READ_CALIB_ROW_gc
#define END_READ_PRODROW()      NVM.CMD = 0
#define READ_PRODROW_VAL(Field) pgm_read_byte( offsetof(NVM_PROD_SIGNATURES_t, Field) )


// Functions for claiming and releasing the generic R/W buffer
#define GBCID_HW_UART 1
#define GBCID_HW_USRT 2
#define GBCID_JTAG    3

extern uint8_t GenericRBuff[UXRT_PASS_SHARED_TX_RX_BUFFER_SIZE]; // Shared buffer for UART and USRT
extern uint8_t GenericWBuff[UXRT_PASS_SHARED_TX_RX_BUFFER_SIZE]; // ---

extern bool claimGenericRWBuff(uint8_t claimerID);
extern bool unclaimGenericRWBuff(uint8_t claimerID);


// Functions to manage the WDT enable key (flag)
// ##### ??? TODO : The function 'wdtEnSetKey()' is currently only used in 'Modules/BB_SWIM.c'; should it be applied more broadly ??? #####
extern void wdtEnSetKey(void);
extern void wdtEnClrKey(void);
extern bool wdtEnIsKeySet(void);


// Functions to reset the system
extern void resetSystem(void);
extern void resetToBootloader(void);


// Function to wait until the selected IN endpoint is ready for a new packet to be sent to the host
extern void waitUntil_Endpoint_IsINReady(USB_ClassInfo_CDC_Device_t* const dev);


// Macros for working with WDT
#define WDT_Enable(PERIOD) do {                                                                      \
	_PROTECTED_WRITE(WDT.WINCTRL,                       WDT_WCEN_bm); /* Disable window mode      */ \
	while(WDT.STATUS & WDT_SYNCBUSY_bm);                              /* Wait for synchronization */ \
	_PROTECTED_WRITE(WDT.CTRL, PERIOD | WDT_ENABLE_bm | WDT_CEN_bm ); /* Enable WDT               */ \
	while(WDT.STATUS & WDT_SYNCBUSY_bm);                              /* Wait for synchronization */ \
} while(0)

#define WDT_Disable() do {                                                 \
	_PROTECTED_WRITE(WDT.CTRL, WDT_CEN_bm); /* Disable WDT */              \
	while(WDT.STATUS & WDT_SYNCBUSY_bm);    /* Wait for synchronization */ \
} while(0)

#define WDT_Reset() do { __asm__ __volatile__ ("wdr \n\t"); } while(0)

#define WDT_PER_8MS    WDT_WPER_8CLK_gc
#define WDT_PER_16MS   WDT_WPER_16CLK_gc
#define WDT_PER_32MS   WDT_WPER_32CLK_gc
#define WDT_PER_64MS   WDT_WPER_64CLK_gc
#define WDT_PER_128MS  WDT_WPER_128CLK_gc
#define WDT_PER_256MS  WDT_WPER_256CLK_gc
#define WDT_PER_512MS  WDT_WPER_512CLK_gc
#define WDT_PER_1S     WDT_WPER_1KCLK_gc
#define WDT_PER_2S     WDT_WPER_2KCLK_gc
#define WDT_PER_4S     WDT_WPER_4KCLK_gc
#define WDT_PER_8S     WDT_WPER_8KCLK_gc


/*
 * Macros and functions for configuring the 'Logic Level Translator' module
 * NOTE : # The pins RXD0← and MISO← are always in input mode.
 *        # 'lltEna_XXX()' configures the 'XXX' pin as an output.
 *        # 'lltDis_XXX()' configures the 'XXX' pin as an input; the default after 'LLT_SETUP()'.
 */
#define LLT_SETUP()                                                                   \
	do {                                                                              \
		IO_SET_VALUE_0(NCTL0_PORT, NCTL0_BIT); IO_SETMODE_OUT(NCTL0_PORT, NCTL0_BIT); \
		IO_SET_VALUE_0( CTL0_PORT,  CTL0_BIT); IO_SETMODE_OUT( CTL0_PORT,  CTL0_BIT); \
		IO_SET_VALUE_0( CTL1_PORT,  CTL1_BIT); IO_SETMODE_OUT( CTL1_PORT,  CTL1_BIT); \
		IO_SET_VALUE_0( CTL2_PORT,  CTL2_BIT); IO_SETMODE_OUT( CTL2_PORT,  CTL2_BIT); \
	} while(0)

#define _LLT_ENA_XCK_NSS_SCK_impl() IO_SET_VALUE_1(NCTL0_PORT, NCTL0_BIT)
#define _LLT_DIS_XCK_NSS_SCK_impl() IO_SET_VALUE_0(NCTL0_PORT, NCTL0_BIT)

#define _LLT_ENA_TXD_impl()         IO_SET_VALUE_1( CTL1_PORT,  CTL1_BIT)
#define _LLT_DIS_TXD_impl()         IO_SET_VALUE_0( CTL1_PORT,  CTL1_BIT)

#define _LLT_ENA_MOSI_impl()        IO_SET_VALUE_1( CTL2_PORT,  CTL2_BIT)
#define _LLT_DIS_MOSI_impl()        IO_SET_VALUE_0( CTL2_PORT,  CTL2_BIT)

extern uint8_t _llt_enaCnt_XCK_nSS_SCK;
extern bool    _llt_enaFlg_XCK;
extern bool    _llt_enaFlg_nSS;
extern bool    _llt_enaFlg_SCK;

inline ATTR_ALWAYS_INLINE void _lltEna_XCK_nSS_SCK_impl(void)
{
	if(!_llt_enaCnt_XCK_nSS_SCK) _LLT_ENA_XCK_NSS_SCK_impl();
	++_llt_enaCnt_XCK_nSS_SCK;
}


inline ATTR_ALWAYS_INLINE void _lltDis_XCK_nSS_SCK_impl(void)
{
	--_llt_enaCnt_XCK_nSS_SCK;
	if(!_llt_enaCnt_XCK_nSS_SCK) _LLT_DIS_XCK_NSS_SCK_impl();
}

inline ATTR_ALWAYS_INLINE void lltEna_HW_XCK (void) { if( _llt_enaFlg_XCK) return; _lltEna_XCK_nSS_SCK_impl(); _llt_enaFlg_XCK = true ; }
inline ATTR_ALWAYS_INLINE void lltDis_HW_XCK (void) { if(!_llt_enaFlg_XCK) return; _lltDis_XCK_nSS_SCK_impl(); _llt_enaFlg_XCK = false; }

inline ATTR_ALWAYS_INLINE void lltEna_HW_TXD (void) { _LLT_ENA_TXD_impl(); }
inline ATTR_ALWAYS_INLINE void lltDis_HW_TXD (void) { _LLT_DIS_TXD_impl(); }

inline ATTR_ALWAYS_INLINE void lltEna_HW_nSS (void) { if( _llt_enaFlg_nSS) return; _lltEna_XCK_nSS_SCK_impl(); _llt_enaFlg_nSS = true ; }
inline ATTR_ALWAYS_INLINE void lltDis_HW_nSS (void) { if(!_llt_enaFlg_nSS) return; _lltDis_XCK_nSS_SCK_impl(); _llt_enaFlg_nSS = false; }

inline ATTR_ALWAYS_INLINE void lltEna_HW_SCK (void) { if( _llt_enaFlg_SCK) return; _lltEna_XCK_nSS_SCK_impl(); _llt_enaFlg_SCK = true ; }
inline ATTR_ALWAYS_INLINE void lltDis_HW_SCK (void) { if(!_llt_enaFlg_SCK) return; _lltDis_XCK_nSS_SCK_impl(); _llt_enaFlg_SCK = false; }

inline ATTR_ALWAYS_INLINE void lltEna_HW_MOSI(void) { _LLT_ENA_MOSI_impl(); }
inline ATTR_ALWAYS_INLINE void lltDis_HW_MOSI(void) { _LLT_DIS_MOSI_impl(); }


// Functions for managing configuration data
#define CFGU32_EEPROM_PAGE 63

#define CFGU32_IDX_VREAD   0

extern void writeEEPROMU08(uint8_t pageAddr, uint8_t byteAddr, uint8_t value);

extern uint32_t loadConfigU32(uint8_t idx);
extern void saveConfigU32(uint8_t idx, uint32_t value);


// Macros for defining and using strings in flash
typedef const struct __flash_string__{}* const* __flash_string;

#define _SPGM(N, S) static const char PROGMEM ATTR_UNUSED N[] = S
#define _FPGM(P)    ( (__flash_string   )     (P) )

#define _FSTR(S)    ( (__flash_string   ) PSTR(S) )

#define _CCCF(F)    ( (const char* const)     (F) )


// A function for calculating CLK2X, BAUDCTRLB, and BAUDCTRLA
typedef struct _XUARTConfig {
	bool    CLK2X;
	uint8_t BAUDCTRLB;
	uint8_t BAUDCTRLA;
} XUARTConfig;

extern XUARTConfig calc_BSCALE_BSEL(uint32_t baudrate);


// Macros and functions for debugging
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
	#define __DPRINTFF_X(FNAME , ...) debug_printf( DMSG_TYPE_BIT_ALWAYS     , _FPGM(     FNAME      ), __dprintf_prefix, ## __VA_ARGS__ )

	#define __DPRINTFS_E(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_ERROR      , _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFS_W(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_WARNING    , _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFS_N(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_NOTICE     , _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFS_I(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_INFORMATION, _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )
	#define __DPRINTFS_X(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_ALWAYS     , _FSTR("%S" FORMAT "\n"), __dprintf_prefix, ## __VA_ARGS__ )

	#define __DPRINTFS__(FORMAT, ...) debug_printf( DMSG_TYPE_BIT_ALWAYS     , _FSTR(     FORMAT "\n"),                   ## __VA_ARGS__ )

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

	#define __DPRINTFS__(FORMAT, ...) do {} while(0)

#endif


#endif // __UTILS_H__
