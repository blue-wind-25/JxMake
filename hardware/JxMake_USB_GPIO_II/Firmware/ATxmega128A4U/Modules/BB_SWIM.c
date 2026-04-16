/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include "../Utils.h"
#include "BB_SWIM.h"


/*
 * This file is written based on the algorithms and information found from:
 *     UM0470
 *     STM8 SWIM Communication Protocol and Debug Module
 *     https://www.st.com/resource/en/user_manual/um0470-stm8-swim-communication-protocol-and-debug-module-stmicroelectronics.pdf
 *
 *     Atmel-0856L
 *     AVR Instruction Set Manual
 *     https://ww1.microchip.com/downloads/en/devicedoc/atmel-0856-avr-instruction-set-manual.pdf
 *
 *     AVR-GCC Inline Assembler Cookbook v1.6
 *     https://web.stanford.edu/class/ee281/projects/aut2002/yingzong-mouse/media/GCCAVRInlAsmCB.pdf
 *
 * ~~~ Last accessed & checked on 2024-05-02 ~~~
 */


// Possible range of SWIM synchronization frame length that can be detected by this module
#define SWIM_SF_MIN 30
#define SWIM_SF_MAX 90


// Watchdog macros
#define WD_ENA() do {              WDT_Enable (WDT_PER_128MS); WDT_Reset(); wdtEnSetKey(); } while(0)
#define WD_DIS() do { WDT_Reset(); WDT_Disable(             );              wdtEnClrKey(); } while(0)


// SPI helper macros
// ##### ??? TODO : FULLY REMOVE THESE MACROS LATER - ONLY USED BY 'bbswim_transfer()' ??? #####
#define SPI_DIS() do { HW_SPI_SPI.CTRL &= ~SPI_ENABLE_bm; } while(0)
#define SPI_ENA() do { HW_SPI_SPI.CTRL |=  SPI_ENABLE_bm; } while(0)

#define SPI_C8M() do { HW_SPI_SPI.CTRL = ( HW_SPI_SPI.CTRL & ~(SPI_CLK2X_bm | SPI_PRESCALER_gm) )                                          ; } while(0)
#define SPI_C4M() do { HW_SPI_SPI.CTRL = ( HW_SPI_SPI.CTRL & ~(               SPI_PRESCALER_gm) ) | (SPI_CLK2X_bm | SPI_PRESCALER_DIV16_gc); } while(0)


// State variables
static bool _bbswimEnabled = false;


////////////////////////////////////////////////////////////////////////////////////////////////////


/*
 * SWIM sampling clock : ~10MHz (~100.00nS)
 * AVR  F_CPU          :  32MHz (  31.25nS)
 *
 *           Low Time               High Time
 *
 * Bit '0'     20        (clocks)      2        (SWIM clocks)   ┐                    ┌──
 *           1600 - 2400 (nS    )    150 -  250 (SWIM nS    )   └────────────────────┘
 *             52 -   76 (cycles)      5 -    8 (AVR  cycles)
 *                [64]                   [ 7]
 *
 * Bit '1'      2        (clocks)     20        (SWIM clocks)   ┐  ┌────────────────────
 *            150 -  250 (nS    )   1600 - 2400 (SWIM nS    )   └──┘
 *              5 -    8 (cycles)     52 -   76 (AVR  cycles)
 *                [ 7]                   [64]
 *
 * Read as bit 0 if samples at logic 0 is >= 9
 * Read as bit 1 if samples at logic 0 is <= 8
 */


static inline ATTR_ALWAYS_INLINE bool __bbswim_sdi(void)
{ return IO_GET_VALUE_X(BB_SWIM_PORT, BB_SWIM_SDI_BIT); }


static inline ATTR_ALWAYS_INLINE void __bbswim_sdo(bool value)
{
	if(value) IO_SET_VALUE_1(BB_SWIM_PORT, BB_SWIM_SDO_BIT);
	else      IO_SET_VALUE_0(BB_SWIM_PORT, BB_SWIM_SDO_BIT);
}


/*
 * NOTE : This additional 1-cycle delay seems necessary to make the programmer more stable for the STM8L series
 *        (and probably the STM8AL and STM8T series as well).
 */
#define DELAY_1()                                               \
        "    nop                             \n\t" /* [ 1  ] */

#define DELAY_2()                                               \
		"    rjmp .+0                        \n\t" /* [ 2  ] */

#define DELAY_6_WDR()                                           \
		"    rjmp .+0                        \n\t" /* [ 2  ] */ \
		"    rjmp .+0                        \n\t" /* [ 2  ] */ \
		"    nop                             \n\t" /* [ 1  ] */ \
		"    wdr                             \n\t" /* [ 1  ] */

#define DELAY_8()                                               \
		"    rjmp .+0                        \n\t" /* [ 2  ] */ \
		"    rjmp .+0                        \n\t" /* [ 2  ] */ \
		"    rjmp .+0                        \n\t" /* [ 2  ] */ \
		"    rjmp .+0                        \n\t" /* [ 2  ] */

#define DELAY_3D3_L(DELAY, LABEL)                                                   \
		"    push   r16                      \n\t" /* [ 1  ] --- Delay ---       */ \
		"    ldi    r16        , " #DELAY "  \n\t" /* [ 1  ] ---                 */ \
		"delay3d4_" #LABEL ":                \n\t" /*        --- Σ = 3*D - 1 + 4 */ \
		"    dec    r16                      \n\t" /* [ 1  ] ---                 */ \
		"    brne   delay3d4_" #LABEL "      \n\t" /* [ 2/1] ---                 */ \
		"    pop    r16                      \n\t" /* [ 2  ] -------------       */


static ATTR_NO_INLINE uint8_t __bbswim_tx(uint8_t value, uint8_t numBits)
{
	/*
	 * Command format   ->   HOST          TARGET
	 *                       S B2 B1 B0 PB A̲C̲K̲
	 *                       0 ?  ?  ?  ?  1
	 *
	 * Data    format   ->   HOST                         TARGET
	 *                       S B7 B6 B5 B4 B3 B2 B1 B0 PB A̲C̲K̲
	 *                       0 ?  ?  ?  ?  ?  ?  ?  ?  ?  1
	 *
	 * Preferred long  low pulse (bit 0) : 60 - 68 cycles
	 * Preferred short low pulse (bit 1) :  7 -  8 cycles
	 */

	uint8_t one;

	__asm__ __volatile__ (
		// Initialization
		"    clr    __tmp_reg__              \n\t" // [ 1  ] Clear __tmp_reg__ (it will be used to calculate parity)
		"    ldi    %[one]     , 1           \n\t" // [ 1  ] Set   %[one] to 1 (it will be used as the constant 1  )

		// Send the start bit - bit '0'
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		DELAY_3D3_L(20, TXS)                       // [63  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		DELAY_6_WDR()                              // [ 6  ]

		// Send the value bits
		"tx_s_vbits_loop:                    \n\t" //
		"    mov    r17        , %[val]      \n\t" // [ 1  ]
		"    and    r17        , %[msk]      \n\t" // [ 1  ]
		"    breq   tx_s_vbit_0              \n\t" // [ 2/1]
		// Send the value bit - bit '1'
		"tx_s_vbit_1:                        \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		"    eor    __tmp_reg__, %[one]      \n\t" // [ 1  ] __tmp_reg__ ^= 0x01
		DELAY_6_WDR()                              // [ 6  ]
		DELAY_1()                                  // [ 1  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		DELAY_3D3_L(17, TXV1)                      // [54  ]
		"    rjmp   tx_s_vbits_next          \n\t" // [ 2  ]
		// Send the value bit - bit '0'
		"tx_s_vbit_0:                        \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		DELAY_3D3_L(20, TXV0)                      // [63  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		DELAY_6_WDR()                              // [ 6  ]
		"tx_s_vbits_next:                    \n\t" //
		"    lsl    %[val]                   \n\t" // [ 1  ]
		"    dec    %[cnt]                   \n\t" // [ 1  ]
		"    brne   tx_s_vbits_loop          \n\t" // [ 2/1]

		// Send the parity bit
		"    sbrs   __tmp_reg__, 0           \n\t" // [ 2/1]
		"    rjmp   tx_s_pbit_0              \n\t" // [ 2  ]
		// Send the parity bit - bit '1'
		"tx_s_pbit_1:                        \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		DELAY_6_WDR()                              // [ 6  ]
		DELAY_1()                                  // [ 1  ]
		DELAY_1()                                  // [ 1  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		DELAY_3D3_L(12, TXP1)                      // [39  ] Use a shorter delay time to avoid being late in reading the ACK bit
		"    rjmp   tx_s_pbit_done           \n\t" // [ 2  ]
		// Send the parity bit - bit '0'
		"tx_s_pbit_0:                        \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		DELAY_3D3_L(20, TXP0)                      // [63  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		"tx_s_pbit_done:                     \n\t" //        Use no delay time to avoid being late in reading the ACK bit

		// Read and check the ACK bit
		"tx_r_abit_l:                        \n\t"
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   tx_r_abit_l              \n\t" // [ 2  ]
		"    clr    %[val]                   \n\t" // [ 1  ]
		DELAY_6_WDR()                              // [ 6  ]
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   tx_r_abit_h              \n\t" // [ 2  ]
		"    ldi    %[val]     , 1           \n\t" // [ 1  ]
		"tx_r_abit_h:                        \n\t"
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   tx_r_abit_h              \n\t" // [ 2  ]

		// Output
		: [one] "=&d"(one    ),
		  [val]  "+r"(value  ),
		  [cnt]  "+r"(numBits)
		// Input
		: [msk] "r"( _BV(numBits - 1)                ),
		  [out] "I"( _SFR_IO_ADDR(BB_SWIM_VPORT.OUT) ),
		  [inp] "I"( _SFR_IO_ADDR(BB_SWIM_VPORT.IN ) ),
		  [bwr] "I"( BB_SWIM_SDO_BIT                 ),
		  [brd] "I"( BB_SWIM_SDI_BIT                 )
		// Clobber
		: "r17", "memory"

	); // asm

	return value;
}


static ATTR_NO_INLINE uint8_t __bbswim_tx_rx(uint8_t wValue, uint8_t* rValue, uint8_t numBytes)
{
	/*
	 * Data format   ->   HOST                         TARGET
	 *                    S B7 B6 B5 B4 B3 B2 B1 B0 PB A̲C̲K̲
	 *                    0 ?  ?  ?  ?  ?  ?  ?  ?  ?  1
	 *
	 * Data format   ->   TARGET                       HOST
	 *                    S B7 B6 B5 B4 B3 B2 B1 B0 PB A̲C̲K̲
	 *                    1 ?  ?  ?  ?  ?  ?  ?  ?  ?  1
	 *
	 * Preferred long  low pulse (bit 0) : 60 - 68 cycles
	 * Preferred short low pulse (bit 1) :  7 -  8 cycles
	 */

	uint8_t one;
	uint8_t cnt;
	uint8_t err;
	uint8_t zct;

	__bbswim_tx(wValue, 8);

	__asm__ __volatile__ (

		// Initialization
		"    ldi    %[one]     , 1           \n\t" // [ 1  ] Set   %[one] to 1 (it will be used as the constant 1     )
		"    clr    %[err]                   \n\t" // [ 1  ] Clear %[err]      (it will be used to count parity errors)

		// Always NACK the 1st received byte for better synchronization
		// NOTE : The target should always resend the same bytes, but because under certain conditions this may not happen,
		//        it may be necessary to read the data starting from the address before the address of the desired data.
#if 0
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		"    clr    %[cnt]                   \n\t" // [ 1  ] Clear %[cnt]      (it will be used to check for timeout  )
		"txrx_r_ibit_l:                      \n\t" //
		"    inc    %[cnt]                   \n\t" // [ 1  ]
		"    cpi    %[cnt]     , 200         \n\t" // [ 1  ]
		"    brsh   txrx_r_error_trampoline  \n\t" // [ 2/1]
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   txrx_r_ibit_l            \n\t" // [ 2  ]
#endif
		DELAY_3D3_L(255, TRXI1A1)                  // [768 ] Use a much longer delay time to ensure the target has finished transmitting
		DELAY_3D3_L(255, TRXI1A2)                  // [768 ] ---
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		DELAY_3D3_L( 20, TRXI1B)                   // [63  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		DELAY_6_WDR()                              // [ 6  ]
		"    rjmp   txrx_r_loop              \n\t" // [ 2  ]
#if 0
		"txrx_r_error_trampoline:            \n\t"
		"    rjmp   txrx_r_error             \n\t" // [ 2  ]
#endif

		// Read the byte(s)
		"txrx_r_loop:                        \n\t"

		// Read and check the start bit - bit '1'
		"txrx_r_sbit_l:                      \n\t" //
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   txrx_r_sbit_l            \n\t" // [ 2  ]
		"    clr    __tmp_reg__              \n\t" // [ 1  ] Clear __tmp_reg__ (it will be used to receive the byte   )
		"    ldi    %[cnt]     , 8           \n\t" // [ 1  ] Set   %[cnt] to 8 (it will be used to count the bits     )
		"    clr    %[val]                   \n\t" // [ 1  ] Clear %[val]      (it will be used to calculate parity   )
		DELAY_6_WDR()                              // [ 2  ]
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"txrx_r_sbit_0:                      \n\t" //
		"    rjmp   txrx_r_value_error       \n\t" // [ 2  ]
		"txrx_r_sbit_1:                      \n\t" //
		"    rjmp   txrx_r_vbit_l            \n\t" // [ 2  ]

		// Read the value bits
		"txrx_r_vbits_loop:                  \n\t" //
		"    lsl    __tmp_reg__              \n\t" // [ 1  ]
		"txrx_r_vbit_l:                      \n\t" //
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   txrx_r_vbit_l            \n\t" // [ 2  ]
		"    clr    %[zct]                   \n\t" // [ 1  ] Clear %[zct]      (it will be used to determine the logic)
		"txrx_r_vbit_h:                      \n\t" //
		DELAY_2()                                  // [ 2  ]
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   txrx_r_vbit_1            \n\t" // [ 2  ]
		"    inc    %[zct]                   \n\t" // [ 1  ]
		"    cpi    %[zct]     , 8           \n\t" // [ 1  ]
		"    brsh   txrx_r_vbit_0            \n\t" // [ 2/1]
		"    rjmp   txrx_r_vbit_h            \n\t" // [ 2  ]
		// Read the value bit - bit '1'
		"txrx_r_vbit_1:                      \n\t" //
		"    or     __tmp_reg__, %[one]      \n\t" // [ 1  ]
		"    eor    %[val]     , %[one]      \n\t" // [ 1  ] %[val] ^= 0x01
		// Read the value bit - bit '0'
		"txrx_r_vbit_0:                      \n\t" //
		"    dec    %[cnt]                   \n\t" // [ 1  ]
		"    brne   txrx_r_vbits_loop        \n\t" // [ 2/1]

		// Read and check the parity bit
		"txrx_r_pbit_l:                      \n\t" //
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   txrx_r_pbit_l            \n\t" // [ 2  ]
		DELAY_6_WDR()                              // [ 6  ]
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   txrx_r_pbit_0            \n\t" // [ 2  ]
		// Check the parity bit - bit '1'
		"txrx_r_pbit_1:                      \n\t" //
		"    tst    %[val]                   \n\t" // [ 1  ]
		"    breq   txrx_r_value_error       \n\t" // [ 2/1]
		"    rjmp   txrx_r_value_done        \n\t" // [ 2  ]
		// Check the parity bit - bit '0'
		"txrx_r_pbit_0:                      \n\t" //
		"    tst    %[val]                   \n\t" // [ 1  ]
		"    brne   txrx_r_value_error       \n\t" // [ 2/1]
		"txrx_r_pbit_h:                      \n\t" //
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 3/2]
		"    rjmp   txrx_r_pbit_h            \n\t" // [ 2  ]
		"    rjmp   txrx_r_value_done        \n\t" // [ 2  ]

		// Got a start or parity bit error
		"txrx_r_value_error:                 \n\t" //
		// Increment and check the error counter
		"    inc    %[err]                   \n\t" // [ 1  ]
		"    cpi    %[err]     , 16          \n\t" // [ 1  ]
		"    brsh   txrx_r_error             \n\t" // [ 2/1]

		// Send the NACK bit
		DELAY_3D3_L(65, TRXN1A)                    // [198 ] Use a much longer delay time to ensure the target has finished transmitting
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		DELAY_3D3_L(20, TRXN1B)                    // [63  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		DELAY_6_WDR()                              // [ 6  ] Use a shorter delay time to avoid being late in reading the ACK bit
//		DELAY_1()                                  // [ 1  ]
		// Loop to read the same byte again
		"    rjmp   txrx_r_loop              \n\t" // [ 2  ] Use no delay time to avoid being late in reading the ACK bit

		// Store the value
		"txrx_r_value_done:                  \n\t" //
		"    st     %a[ptr]+   , __tmp_reg__ \n\t" // [ 1  ]

		// Check if all bytes have been read
		"    dec    %[byt]                   \n\t" // [ 1  ]
		"    breq   txrx_r_done              \n\t" // [ 2/1]

		// Not all bytes have been read
		"txrx_r_next:                        \n\t"
		// Clear the error counter
		"    clr    %[err]                   \n\t" // [ 1  ] Clear %[err]      (it will be used to count parity errors)
		// Send the ACK bit
		DELAY_3D3_L(6, TRXAI)                      // [21  ] Ensure the target has finished transmitting
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		DELAY_6_WDR()                              // [ 6  ]
		DELAY_1()                                  // [ 1  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		DELAY_6_WDR()                              // [ 6  ] Use a shorter delay time to avoid being late in reading the ACK bit
		// Loop to read the next byte
		"    rjmp   txrx_r_loop              \n\t" // [ 2  ] Use no delay time to avoid being late in reading the ACK bit

		// All bytes have been read
		"txrx_r_done:                        \n\t"
		// Send the last ACK bit
		DELAY_3D3_L(6, TRXAL)                      // [21  ] Ensure the target has finished transmitting
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO low
		DELAY_6_WDR()                              // [ 6  ]
		DELAY_1()                                  // [ 1  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 1  ] SDO high
		DELAY_6_WDR()                              // [ 6  ]

		// Set the success flag and exit
		"    ldi    %[val]     , 1           \n\t" // [ 1  ]
		"    rjmp   txrx_r_exit              \n\t" // [ 2  ]

		// Error condition - set the error flag
		"txrx_r_error:                       \n\t"
		"    clr    %[val]                   \n\t" // [ 1  ]

		// Exit
		"txrx_r_exit:                        \n\t"

		// Output
		: [one] "=&d"(one     ),
		  [val]  "+r"(wValue  ),
		  [cnt] "=&r"(cnt     ),
		  [zct] "=&r"(zct     ),
		  [err] "=&r"(err     ),
		  [byt]  "+r"(numBytes)
		// Input
		: [out] "I"( _SFR_IO_ADDR(BB_SWIM_VPORT.OUT) ),
		  [inp] "I"( _SFR_IO_ADDR(BB_SWIM_VPORT.IN ) ),
		  [bwr] "I"( BB_SWIM_SDO_BIT                 ),
		  [brd] "I"( BB_SWIM_SDI_BIT                 ),
		  [ptr] "e"( rValue                          )
		// Clobber
		: "memory"

	); // asm

	return wValue;
}


////////////////////////////////////////////////////////////////////////////////////////////////////


#define BBSWIM_BB_ENTER() do { \
		/* Disable SPI */      \
		__bbswim_sdo(true);    \
		SPI_DIS();             \
		SPI_C4M();             \
		/* Enable watchdog */  \
		WD_ENA();              \
	} while(0)


#define BBSWIM_BB_LEAVE() do { \
		/* Disable watchdog */ \
		WD_DIS();              \
		/* Enable SPI */       \
		SPI_ENA();             \
	} while(0)


#define BBSWIM_BB_ERROR() do {   \
		/* Enable interrupt */   \
		sei();                   \
		/* Disable watchdog */   \
		WD_DIS();                \
		/* Enable SPI */         \
		SPI_ENA();               \
		/* Perform line-reset */ \
		bbswim_lineReset();      \
	} while(0)


bool bbswim_begin(void)
{
	__DPRINTF_DECL_PREFIX("bbswim_begin");

	// Simply exit if the SWIM is already enabled
	if( bbswim_isEnabled() ) return true;

	// Initialize the virtual port
	BB_SWIM_VPORTCFG();

	// Initialize HW-SPI because BB-SWIM is hardware-assisted by HW-SPI
	/*
	 * PRESCALER   ~CLK2X   Frequency
	 * 0    1       0       F_CPU / 8 (4MHz)
	 * 0    0       1       F_CPU / 4 (8MHz)   <- start with this one
	 */
	if( !hwspi_begin(0, false, 1) ) return false;

	// Send the SWIM activation sequence
	uint16_t syncCntH = 0;
	uint16_t syncCntL = 0;

	__bbswim_sdo(true);                                                                 // (0)
	SPI_DIS();                                                                          //
	SPI_C4M();                                                                          //
	_delay_us(100);                                                                     //
	hwspi_selectSlave();                                                                //
	_delay_ms(100);                                                                     //

	cli();

		/*
		// ##### ??? TODO : REMOVE THIS BLOCK LATER ??? #####
		for(;;) {
			__asm__ __volatile__ (
				"    cbi %[out], %[bwr] \n\t"
				DELAY_3D3_L(255, XXX1)
				"    sbi %[out], %[bwr] \n\t"
				DELAY_3D3_L(255, XXX2)
				: : [out] "I"( _SFR_IO_ADDR(BB_SWIM_VPORT.OUT) ) ,
				    [bwr] "I"( BB_SWIM_SDO_BIT                 ) :
			); // asm
		}
		//*/

		/*
		// ##### ??? TODO : REMOVE THIS BLOCK LATER ??? #####
		for(;;) {
			__DPRINTFS_X( "### 1" );
			__DPRINTFS_X( "%d", __bbswim_tx(0xFF, 3) );
			__DPRINTFS_X( "### 2" );
			__asm__ __volatile__ ( DELAY_6_WDR() );
		}
		//*/

		__bbswim_sdo(true ); _delay_us( 16);                                            // (1)
		__bbswim_sdo(false); _delay_us( 16);                                            //

		__bbswim_sdo(true ); _delay_us(500); __bbswim_sdo(false); _delay_us(500);       // (2)
		__bbswim_sdo(true ); _delay_us(500); __bbswim_sdo(false); _delay_us(500);       //
		__bbswim_sdo(true ); _delay_us(500); __bbswim_sdo(false); _delay_us(500);       //
		__bbswim_sdo(true ); _delay_us(500); __bbswim_sdo(false); _delay_us(500);       //
		__bbswim_sdo(true ); _delay_us(250); __bbswim_sdo(false); _delay_us(250);       //
		__bbswim_sdo(true ); _delay_us(250); __bbswim_sdo(false); _delay_us(250);       //
		__bbswim_sdo(true ); _delay_us(250); __bbswim_sdo(false); _delay_us(250);       //
		__bbswim_sdo(true ); _delay_us(250); __bbswim_sdo(false); _delay_us(250);       //
		__bbswim_sdo(true );                                                            //
		__asm__ __volatile__ ( DELAY_8() );                                             //

		                                                                                //               syncCntH   syncCntL
		while(  __bbswim_sdi() ) { if(++syncCntH > 500) goto bbswim_begin_error; }      // (3)   STM8S    65 ±  5   45 ± 5
		while( !__bbswim_sdi() ) { if(++syncCntL > 250) goto bbswim_begin_error; }      // (4)   STM8L   195 ± 25   45 ± 5

		__bbswim_sdo(true );                                                            // (5)
		_delay_us(1);                                                                   //

		// WOTF 0x01 0x007F80 0xA0
		if( !__bbswim_tx(0b010, 3) ) goto bbswim_begin_error;                           // (6)
		if( !__bbswim_tx(0x01 , 8) ) goto bbswim_begin_error;                           //
		if( !__bbswim_tx(0x00 , 8) ) goto bbswim_begin_error;                           //
		if( !__bbswim_tx(0x7F , 8) ) goto bbswim_begin_error;                           //
		if( !__bbswim_tx(0x80 , 8) ) goto bbswim_begin_error;                           //
		if( !__bbswim_tx(0xA0 , 8) ) goto bbswim_begin_error;                           //
		SPI_ENA();                                                                      //

	sei();

	__DPRINTFS_I("%d | %d [%d %d]", syncCntH, syncCntL, SWIM_SF_MIN, SWIM_SF_MAX);

	if(syncCntL < SWIM_SF_MIN || syncCntL > SWIM_SF_MAX) goto bbswim_begin_error;

	_delay_us(100);                                                                     // (7)
	hwspi_deselectSlave();                                                              //

	_delay_ms(100);                                                                     // (8)

	// Set flag
	_bbswimEnabled = true;

	// Done
	return true;

	// Error
bbswim_begin_error:
	__DPRINTFS_E("%d | %d [%d %d]", syncCntH, syncCntL, SWIM_SF_MIN, SWIM_SF_MAX);
	sei();        // Enable interrupt
	hwspi_end();  // Unitialize SPI
	return false; // Exit error
}


void bbswim_end(void)
{
	// Simply exit if the SWIM is not enabled
	if( !bbswim_isEnabled() ) return;

	// BB-SWIM is hardware-assisted by HW-SPI, therefore, uninitialize HW-SPI
	hwspi_end();

	// Clear flag
	_bbswimEnabled = false;
}


bool bbswim_isEnabled(void)
{ return _bbswimEnabled; }


bool bbswim_lineReset(void)
{
	__DPRINTF_DECL_PREFIX("bbswim_lineReset");

	// Error if the SWIM is not enabled
	if( !bbswim_isEnabled() ) return false;

	// Perform line-reset
	uint8_t syncCntH = 0;
	uint8_t syncCntL = 0;

	cli();

		__bbswim_sdo(true );
		SPI_DIS();
		SPI_C4M();
		__bbswim_sdo(false);

		_delay_us( (128 + 64) / 8 ); // Delay at least 128 SWIM clocks

		__bbswim_sdo(true );
		__asm__ __volatile__ ( DELAY_8() );

		while(  __bbswim_sdi() ) { if(++syncCntH > 200) goto bbswim_lineReset_error; }

		while( !__bbswim_sdi() ) { if(++syncCntL > 200) goto bbswim_lineReset_error; }

		__asm__ __volatile__ ( DELAY_8() );
		SPI_ENA();

	sei();

	__DPRINTFS_I("%d | %d [%d %d]", syncCntH, syncCntL, SWIM_SF_MIN, SWIM_SF_MAX);

	// Done
	return (syncCntL >= SWIM_SF_MIN && syncCntL <= SWIM_SF_MAX);

	// Error
bbswim_lineReset_error:
	__DPRINTFS_E("%d | %d [%d %d]", syncCntH, syncCntL, SWIM_SF_MIN, SWIM_SF_MAX);
	sei();        // Enable interrupt
	SPI_ENA();    // Enable SPI
	return false; // Exit error
}

/*
bool bbswim_transfer(uint8_t* buff, _SInt_IZArg_t size, _SInt_IZArg_t size2)
{
	// ##### ??? TODO : FULLY REMOVE THIS FUNCTION LATER ??? #####

	// Error if the SWIM is not enabled
	if( !bbswim_isEnabled() ) return false;

	// Transfer the byte(s)
	cli();

	HW_SPI_SPI.DATA = *buff;

	if(!size2) {
		while(--size) {

			const uint8_t outVal = *(buff + 1);

			while( !(HW_SPI_SPI.STATUS & SPI_IF_bm) );

			const uint8_t inpVal          = HW_SPI_SPI.DATA  ;
			              HW_SPI_SPI.DATA = outVal;

			*buff++ = inpVal;

		} // while
	}
	else {
		while(--size) {

			const uint8_t outVal = *(buff + 1);

			while( !(HW_SPI_SPI.STATUS & SPI_IF_bm) );

			const uint8_t inpVal          = HW_SPI_SPI.DATA  ;
			              HW_SPI_SPI.DATA = outVal;

			if(size == size2) SPI_C8M();

			*buff++ = inpVal;

		} // while
	}

	while( !(HW_SPI_SPI.STATUS & SPI_IF_bm) );
	*buff = HW_SPI_SPI.DATA;

	if(size2) SPI_C4M();

	sei();

	// Done
	return true;
}
*/


bool bbswim_cmd_srst(void)
{
	BBSWIM_BB_ENTER();

	// Send the command
	cli();
		const uint8_t res = __bbswim_tx(0b000, 3);
	sei();

	BBSWIM_BB_LEAVE();

	// Perform line-reset as needed
	if(!res) bbswim_lineReset();

	// Done
	return res;
}


bool bbswim_cmd_rotf(uint8_t* buff, _SInt_IZArg_t size, uint8_t addrE, uint8_t addrH, uint8_t addrL)
{
	// SWIM can only handle at most 255 bytes in one ROTF
	if(size > 255) return false;

	BBSWIM_BB_ENTER();

	// Send the command and receive the data
	cli();
		if( !__bbswim_tx   (0b001, 3         ) ) goto bbswim_cmd_rotf_error;
		if( !__bbswim_tx   (size , 8         ) ) goto bbswim_cmd_rotf_error;
		if( !__bbswim_tx   (addrE, 8         ) ) goto bbswim_cmd_rotf_error;
		if( !__bbswim_tx   (addrH, 8         ) ) goto bbswim_cmd_rotf_error;
		if( !__bbswim_tx_rx(addrL, buff, size) ) goto bbswim_cmd_rotf_error;
	sei();

	BBSWIM_BB_LEAVE();

	// Done
	return true;

	// Error
bbswim_cmd_rotf_error:
	BBSWIM_BB_ERROR();
	return false;
}


bool bbswim_cmd_wotf(const uint8_t* buff, _SInt_IZArg_t size, uint8_t addrE, uint8_t addrH, uint8_t addrL)
{
	// SWIM can only handle at most 255 bytes in one WOTF
	if(size > 255) return false;

	BBSWIM_BB_ENTER();

	// Send the command and data
	cli();
		if( !__bbswim_tx(0b010, 3) ) goto bbswim_cmd_wotf_error;
		if( !__bbswim_tx(size , 8) ) goto bbswim_cmd_wotf_error;
		if( !__bbswim_tx(addrE, 8) ) goto bbswim_cmd_wotf_error;
		if( !__bbswim_tx(addrH, 8) ) goto bbswim_cmd_wotf_error;
		if( !__bbswim_tx(addrL, 8) ) goto bbswim_cmd_wotf_error;
	sei();

	while(size--) {
		cli();
			uint8_t retryCount = 8;
			while(retryCount--) {
				if( __bbswim_tx(*buff++, 8) ) break;
			}
			if(!retryCount) goto bbswim_cmd_wotf_error;
		sei();
	} // while

	BBSWIM_BB_LEAVE();

	// Done
	return true;

	// Error
bbswim_cmd_wotf_error:
	BBSWIM_BB_ERROR();
	return false;
}
