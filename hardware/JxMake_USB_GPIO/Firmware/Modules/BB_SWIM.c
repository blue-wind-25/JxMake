/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <stdbool.h>
#include <stdint.h>

#include <avr/io.h>
#include <avr/wdt.h>

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
#define SWIM_SF_MIN 15
#define SWIM_SF_MAX 45


// Watchdog macros
#define WD_ENA() do {              wdt_enable (WDTO_120MS); wdt_reset(); wdtEnSetKey(); } while(0)
#define WD_DIS() do { wdt_reset(); wdt_disable(          );              wdtEnClrKey(); } while(0)
#define WD_RST() do { wdt_reset()                                                       } while(0)


// SPI constants
static uint8_t SPCR_SPI_DIS =            _BV(MSTR);
static uint8_t SPCR_SPI_ENA = _BV(SPE) | _BV(MSTR);

static uint8_t SPSR_SPI_C8M = _BV(SPI2X);
static uint8_t SPSR_SPI_C4M = 0;


// State variables
static bool _bbswimEnabled = false;


////////////////////////////////////////////////////////////////////////////////////////////////////


/*
 * SWIM sampling clock : ~10MHz (~100.0nS)
 * AVR  F_CPU          :  16MHz (  62.5nS)
 *
 *           Low Time               High Time
 *
 * Bit '0'     20        (clocks)      2        (SWIM clocks)   ┐                    ┌──
 *           1600 - 2400 (nS    )    150 -  250 (SWIM nS    )   └────────────────────┘
 *             26 -   38 (cycles)      3 -    4 (AVR  cycles)
 *                [32]                   [ 3]
 *
 * Bit '1'      2        (clocks)     20        (SWIM clocks)   ┐  ┌────────────────────
 *            150 -  250 (nS    )   1600 - 2400 (SWIM nS    )   └──┘
 *              3 -    4 (cycles)     26 -   38 (AVR  cycles)
 *                [ 3]                   [32]
 *
 * Read as bit 0 if samples at logic 0 is >= 9
 * Read as bit 1 if samples at logic 0 is <= 8
 */


static inline ATTR_ALWAYS_INLINE bool __bbswim_sdi(void)
{ return BB_SWIM_PIN & _BV(BB_SWIM_SDI_BIT); }


static inline ATTR_ALWAYS_INLINE void __bbswim_sdo(bool value)
{
	if(value) BB_SWIM_PORT |=  _BV(BB_SWIM_SDO_BIT);
	else      BB_SWIM_PORT &= ~_BV(BB_SWIM_SDO_BIT);
}


#define DELAY_3_WDR()                                           \
		"    nop                             \n\t" /* [ 1  ] */ \
		"    nop                             \n\t" /* [ 1  ] */ \
		"    wdr                             \n\t" /* [ 1  ] */

#define DELAY_3D4_L(DELAY, LABEL)                                                   \
		"    push   r16                      \n\t" /* [ 2  ] --- Delay ---       */ \
		"    ldi    r16        , " #DELAY "  \n\t" /* [ 1  ] ---                 */ \
		"delay3d4_" #LABEL ":                \n\t" /*        --- Σ = 3*D - 1 + 5 */ \
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
	 * Preferred short low pulse :  3 -  4 cycles
	 * Preferred long  low pulse : 30 - 34 cycles
	 */

	uint8_t one;

	__asm__ __volatile__ (

		// Initialization
		"    clr    __tmp_reg__              \n\t" // [ 1  ] Clear __tmp_reg__ (it will be used to calculate parity)
		"    ldi    %[one]     , 1           \n\t" // [ 1  ] Set   %[one] to 1 (it will be used as the constant 1  )

		// Send the start bit - bit '0'
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		DELAY_3D4_L(9, TXS)                        // [31  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		DELAY_3_WDR()                              // [ 3  ]

		// Send the value bits
		"tx_s_vbits_loop:                    \n\t" //
		"    mov    r17        , %[val]      \n\t" // [ 1  ]
		"    and    r17        , %[msk]      \n\t" // [ 1  ]
		"    breq   tx_s_vbit_0              \n\t" // [ 2/1]
		// Send the value bit - bit '1'
		"tx_s_vbit_1:                        \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		"    eor    __tmp_reg__, %[one]      \n\t" // [ 1  ] __tmp_reg__ ^= 0x01
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		DELAY_3D4_L(7, TXV1)                       // [25  ]
		"    rjmp   tx_s_vbits_next          \n\t" // [ 2  ]
		// Send the value bit - bit '0'
		"tx_s_vbit_0:                        \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		DELAY_3D4_L(9, TXV0)                       // [31  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		"tx_s_vbits_next:                    \n\t" //
		"    lsl    %[val]                   \n\t" // [ 1  ]
		"    dec    %[cnt]                   \n\t" // [ 1  ]
		"    brne   tx_s_vbits_loop          \n\t" // [ 2/1]

		// Send the parity bit
		"    sbrs   __tmp_reg__, 0           \n\t" // [ 2/1]
		"    rjmp   tx_s_pbit_0              \n\t" // [ 2  ]
		// Send the parity bit - bit '1'
		"tx_s_pbit_1:                        \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		"    eor    __tmp_reg__, %[one]      \n\t" // [ 1  ] __tmp_reg__ ^= 0x01
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		DELAY_3D4_L(6, TXP1)                       // [22  ] Use a shorter delay time to avoid being late in reading the ACK bit
		"    rjmp   tx_s_pbit_done           \n\t" // [ 2  ]
		// Send the parity bit - bit '0'
		"tx_s_pbit_0:                        \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		DELAY_3D4_L(9, TXP0)                       // [31  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		"tx_s_pbit_done:                     \n\t" //

		// Read and check the ACK bit
		"    clr    %[val]                   \n\t" // [ 1  ]
		"tx_r_abit_l:                        \n\t"
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   tx_r_abit_l              \n\t" // [ 2  ]
		"    nop                             \n\t" // [ 1  ]
		"    wdr                             \n\t" // [ 1  ]
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   tx_r_abit_h              \n\t" // [ 2  ]
		"    ldi    %[val]     , 1           \n\t" // [ 1  ]
		"tx_r_abit_h:                        \n\t"
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   tx_r_abit_h              \n\t" // [ 2  ]

		// Output
		: [one] "=&d"(one    ),
		  [val]  "+r"(value  ),
		  [cnt]  "+r"(numBits)
		// Input
		: [msk] "r"( _BV(numBits - 1)           ),
		  [out] "I"( _SFR_IO_ADDR(BB_SWIM_PORT) ),
		  [inp] "I"( _SFR_IO_ADDR(BB_SWIM_PIN ) ),
		  [bwr] "I"( BB_SWIM_SDO_BIT            ),
		  [brd] "I"( BB_SWIM_SDI_BIT            )
		// Clobber
		: "r17"

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
	 * Preferred short low pulse :  3 -  4 cycles
	 * Preferred long  low pulse : 30 - 34 cycles
	 */

	uint8_t one;
	uint8_t cnt;
	uint8_t err;
	uint8_t zct;

	__asm__ __volatile__ (

		// Initialization
		"    clr    __tmp_reg__              \n\t" // [ 1  ] Clear __tmp_reg__ (it will be used to calculate parity)
		"    ldi    %[one]     , 1           \n\t" // [ 1  ] Set   %[one] to 1 (it will be used as the constant 1     )
		"    ldi    %[cnt]     , 8           \n\t" // [ 1  ] Set   %[cnt] to 8 (it will be used to count the bits     )
		"    clr    %[err]                   \n\t" // [ 1  ] Clear %[err]      (it will be used to count parity errors)

		// Send the start bit - bit '0'
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		DELAY_3D4_L(9, TRXS)                       // [31  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		DELAY_3_WDR()                              // [ 3  ]

		// Send the value bits
		"txrx_s_vbits_loop:                  \n\t" //
		"    sbrs   %[val]     , 7           \n\t" // [ 2/1]
		"    rjmp   txrx_s_vbit_0            \n\t" // [ 2  ]
		// Send the value bit - bit '1'
		"txrx_s_vbit_1:                      \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		"    eor    __tmp_reg__, %[one]      \n\t" // [ 1  ] __tmp_reg__ ^= 0x01
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		DELAY_3D4_L(7, TRXV1)                      // [25  ]
		"    rjmp   txrx_s_vbits_next        \n\t" // [ 2  ]
		// Send the value bit - bit '0'
		"txrx_s_vbit_0:                      \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		DELAY_3D4_L(9, TRXV0)                      // [31  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		"txrx_s_vbits_next:                  \n\t" //
		"    lsl    %[val]                   \n\t" // [ 1  ]
		"    dec    %[cnt]                   \n\t" // [ 1  ]
		"    brne   txrx_s_vbits_loop        \n\t" // [ 2/1]

		// Send the parity bit
		"    sbrs   __tmp_reg__, 0           \n\t" // [ 2/1]
		"    rjmp   txrx_s_pbit_0            \n\t" // [ 2  ]
		// Send the parity bit - bit '1'
		"txrx_s_pbit_1:                      \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		"    eor    __tmp_reg__, %[one]      \n\t" // [ 1  ] __tmp_reg__ ^= 0x01
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		DELAY_3D4_L(6, TRXP1)                      // [22  ] Use a shorter delay time to avoid being late in reading the ACK bit
		"    rjmp   txrx_s_pbit_done         \n\t" // [ 2/1]
		// Send the parity bit - bit '0'
		"txrx_s_pbit_0:                      \n\t" //
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		DELAY_3D4_L(9, TRXP0)                      // [31  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		"txrx_s_pbit_done:                   \n\t" //

		// Read and check the ACK bit
		"txrx_r_abit_l:                      \n\t" //
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_abit_l            \n\t" // [ 2  ]
		"    nop                             \n\t" // [ 1  ]
		"    wdr                             \n\t" // [ 1  ]
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_error             \n\t" // [ 2  ]
		"txrx_r_abit_h:                      \n\t" //
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_abit_h            \n\t" // [ 2  ]

		////////////////////////////////////////////////////////////////////////////////////////////////////

		// Always NACK the 1st received byte for better synchronization
		// NOTE : The target should always resend the same bytes, but because under certain conditions this may not happen,
		//        it may be necessary to read the data starting from the address before the address of the desired data.
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		"    clr    %[cnt]                   \n\t" // [ 1  ] Clear %[cnt]      (it will be used to check for timeout  )
		"txrx_r_ibit_l:                      \n\t" //
		"    inc    %[cnt]                   \n\t" // [ 1  ]
		"    cpi    %[cnt]     , 200         \n\t" // [ 1  ]
		"    brsh   txrx_r_error_trampoline  \n\t" // [ 2/1]
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_ibit_l            \n\t" // [ 2  ]
		DELAY_3D4_L(255, TRXI1A)                   // [769 ] Use a much longer delay time to ensure the target has finished transmitting
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		DELAY_3D4_L(  9, TRXI1B)                   // [31  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		"    wdr                             \n\t" // [ 1  ]
		"    rjmp   txrx_r_loop              \n\t" // [ 2  ]
		"txrx_r_error_trampoline:            \n\t"
		"    rjmp   txrx_r_error             \n\t" // [ 2  ]

		// Read the byte(s)
		"txrx_r_loop:                        \n\t"

		// Read and check the start bit - bit '1'
		"txrx_r_sbit_l:                      \n\t" //
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_sbit_l            \n\t" // [ 2  ]
		"    clr    __tmp_reg__              \n\t" // [ 1  ] Clear __tmp_reg__ (it will be used to receive the byte   )
		"    ldi    %[cnt]     , 8           \n\t" // [ 1  ] Set   %[cnt] to 8 (it will be used to count the bits     )
		"    clr    %[val]                   \n\t" // [ 1  ] Clear %[val]      (it will be used to calculate parity   )
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"txrx_r_sbit_0:                      \n\t" //
		"    rjmp   txrx_r_value_error       \n\t" // [ 2  ]
		"txrx_r_sbit_1:                      \n\t" //
		"    rjmp   txrx_r_vbit_l            \n\t" // [ 2  ]

		// Read the value bits
		"txrx_r_vbits_loop:                  \n\t" //
		"    lsl    __tmp_reg__              \n\t" // [ 1  ]
		"txrx_r_vbit_l:                      \n\t" //
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_vbit_l            \n\t" // [ 2  ]
		"    clr    %[zct]                   \n\t" // [ 1  ] Clear %[zct]      (it will be used to determine the logic)
		"txrx_r_vbit_h:                      \n\t" //
		"    nop                             \n\t" // [ 1  ]
		"    nop                             \n\t" // [ 1  ]
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_vbit_1            \n\t" // [ 2  ]
		"    inc    %[zct]                   \n\t" // [ 1  ]
		"    cpi    %[zct]     , 4           \n\t" // [ 1  ]
		"    brsh   txrx_r_vbit_0            \n\t" // [ 2/1]
		"    rjmp   txrx_r_vbit_h            \n\t" // [ 2  ]
		// Read the value bit - bit '1'
		"txrx_r_vbit_1:                      \n\t" //
		"    or     __tmp_reg__, %[one]      \n\t" // [ 1  ]
		"    eor    %[val]     , %[one]      \n\t" // [ 1  ] %[val] ^= 0x01
		"    wdr                             \n\t" // [ 1  ]
		// Read the value bit - bit '0'
		"txrx_r_vbit_0:                      \n\t" //
		"    dec    %[cnt]                   \n\t" // [ 1  ]
		"    brne   txrx_r_vbits_loop        \n\t" // [ 2/1]

		// Read and check the parity bit
		"txrx_r_pbit_l:                      \n\t" //
		"    sbic   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_pbit_l            \n\t" // [ 2  ]
		"    nop                             \n\t" // [ 1  ]
		"    wdr                             \n\t" // [ 1  ]
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 2/1]
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
		"    sbis   %[inp]     , %[brd]      \n\t" // [ 2/1]
		"    rjmp   txrx_r_pbit_h            \n\t" // [ 2  ]
		"    rjmp   txrx_r_value_done        \n\t" // [ 2  ]

		// Start bit or parity error
		"txrx_r_value_error:                 \n\t" //
		// Increment and check the error counter
		"    inc    %[err]                   \n\t" // [ 1  ]
		"    cpi    %[err]     , 16          \n\t" // [ 1  ]
		"    brsh   txrx_r_error             \n\t" // [ 2/1]
		"    wdr                             \n\t" // [ 1  ]

		// Send the NACK bit
		DELAY_3D4_L(31, TRXN1A)                    // [97  ] Use a much longer delay time to ensure the target has finished transmitting
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		DELAY_3D4_L( 9, TRXN1B)                    // [31  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		"    wdr                             \n\t" // [ 1  ]
		// Loop to read the same byte again
		"    wdr                             \n\t" // [ 1  ]
		"    rjmp   txrx_r_loop              \n\t" // [ 2  ]

		// Store the value
		"txrx_r_value_done:                  \n\t" //
		"    st     %a[ptr]+   , __tmp_reg__ \n\t" // [ 2  ]
		"    wdr                             \n\t" // [ 1  ]

		// Check if all bytes have been read
		"    dec    %[byt]                   \n\t" // [ 1  ]
		"    breq   txrx_r_done              \n\t" // [ 2/1]

		// Not all bytes have been read
		"txrx_r_next:                        \n\t"
		// Clear the error counter
		"    clr    %[err]                   \n\t" // [ 1  ] Clear %[err]      (it will be used to count parity errors)
		// Send the ACK bit
		DELAY_3D4_L(2, TRXAI)                      // [10  ] Ensure the target has finished transmitting
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		"    wdr                             \n\t" // [ 1  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		// Loop to read the next byte
		"    rjmp   txrx_r_loop              \n\t" // [ 2  ]

		// All bytes have been read
		"txrx_r_done:                        \n\t"
		// Send the last ACK bit
		DELAY_3D4_L(2, TRXAL)                      // [10  ] Ensure the target has finished transmitting
		"    cbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO low
		"    wdr                             \n\t" // [ 1  ]
		"    sbi    %[out]     , %[bwr]      \n\t" // [ 2  ] SDO high
		// Set the success flag and exit
		"    ldi    %[val]     , 1           \n\t" // [ 1  ]
		"    rjmp   txrx_r_exit              \n\t" // [ 2  ]

		// Error condition - set the error flag
		"txrx_r_error:                       \n\t"
		"    clr    %[val]                   \n\t" // [ 1  ]

		// Exit
		"txrx_r_exit:                        \n\t"
		"    wdr                             \n\t" // [ 1  ]

		// Output
		: [one] "=&d"(one     ),
		  [val]  "+r"(wValue  ),
		  [cnt] "=&r"(cnt     ),
		  [zct] "=&r"(zct     ),
		  [err] "=&r"(err     ),
		  [byt]  "+r"(numBytes)
		// Input
		: [out] "I"( _SFR_IO_ADDR(BB_SWIM_PORT) ),
		  [inp] "I"( _SFR_IO_ADDR(BB_SWIM_PIN ) ),
		  [bwr] "I"( BB_SWIM_SDO_BIT            ),
		  [brd] "I"( BB_SWIM_SDI_BIT            ),
		  [ptr] "e"( rValue                     )
		// Clobber
		:

	); // asm

	return wValue;
}


////////////////////////////////////////////////////////////////////////////////////////////////////


#define BBSWIM_BB_ENTER() do { \
		/* Disable SPI */      \
		__bbswim_sdo(true);    \
		SPCR = SPCR_SPI_DIS;   \
		SPSR = SPSR_SPI_C4M;   \
		/* Enable watchdog */  \
		WD_ENA();              \
	} while(0)


#define BBSWIM_BB_LEAVE() do { \
		/* Disable watchdog */ \
		WD_DIS();              \
		/* Enable SPI */       \
		SPCR = SPCR_SPI_ENA;   \
	} while(0)


#define BBSWIM_BB_ERROR() do {   \
		/* Enable interrupt */   \
		sei();                   \
		/* Disable watchdog */   \
		WD_DIS();                \
		/* Enable SPI */         \
		SPCR = SPCR_SPI_ENA;     \
		/* Perform line-reset */ \
		bbswim_lineReset();      \
	} while(0)


bool bbswim_begin(void)
{
	__DPRINTF_DECL_PREFIX("bbswim_begin");

	// Simply exit if the SWIM is already enabled
	if( bbswim_isEnabled() ) return true;

	// Initialize HW-SPI because BB-SWIM is hardware-assisted by HW-SPI
	/*
	 * SPR1   SPR0   ~SPI2X   Frequency
	 * 0      0       1       F_CPU / 4 (4MHz)
	 * 0      0       0       F_CPU / 2 (8MHz)   <- start with this one
	 */
	if( !hwspi_begin(0, false, 0) ) return false;

	// Send the SWIM activation sequence
	uint8_t syncCntH = 0;
	uint8_t syncCntL = 0;

	__bbswim_sdo(true);                                                                 // (0)
	SPCR = SPCR_SPI_DIS;                                                                //
	SPSR = SPSR_SPI_C4M;                                                                //
	_delay_us(100);                                                                     //
	hwspi_selectSlave();                                                                //
	_delay_ms(100);                                                                     //

	cli();

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
		__asm__ __volatile__ ( "nop \n\t" );                                            //

		while(  __bbswim_sdi() ) { if(++syncCntH > 200) goto bbswim_begin_error; }      // (3)

		while( !__bbswim_sdi() ) { if(++syncCntL > 200) goto bbswim_begin_error; }      // (4)

		__bbswim_sdo(true );                                                            // (5)
		__asm__ __volatile__ ( "nop \n\t nop \n\t nop \n\t nop \n\t" );                 //
		__asm__ __volatile__ ( "nop \n\t nop \n\t nop \n\t nop \n\t" );                 //

		// WOTF 0x01 0x007F80 0xA0
		if( !__bbswim_tx(0b010, 3) ) goto bbswim_begin_error;                           // (6)
		if( !__bbswim_tx(0x01 , 8) ) goto bbswim_begin_error;                           //
		if( !__bbswim_tx(0x00 , 8) ) goto bbswim_begin_error;                           //
		if( !__bbswim_tx(0x7F , 8) ) goto bbswim_begin_error;                           //
		if( !__bbswim_tx(0x80 , 8) ) goto bbswim_begin_error;                           //
		if( !__bbswim_tx(0xA0 , 8) ) goto bbswim_begin_error;                           //
		SPCR = SPCR_SPI_ENA;                                                            //

	sei();

	__DPRINTFS_X("%d | %d [%d %d]", syncCntH, syncCntL, SWIM_SF_MIN, SWIM_SF_MAX);

	if(syncCntL < SWIM_SF_MIN || syncCntL > SWIM_SF_MAX) goto bbswim_begin_error;

	_delay_us(100);                                                                     // (7)
	hwspi_deselectSlave();                                                              //
	_delay_ms(100);                                                                     //

	// Set flag
	_bbswimEnabled = true;

	// Done
	return true;

	// Error
bbswim_begin_error:
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
		SPCR = SPCR_SPI_DIS;
		SPSR = SPSR_SPI_C4M;
		__bbswim_sdo(false);

		_delay_us( (128 + 64) / 8 );

		__bbswim_sdo(true );
		__asm__ __volatile__ ( "nop \n\t nop \n\t nop \n\t nop \n\t" );

		while(  __bbswim_sdi() ) { if(++syncCntH > 200) goto bbswim_lineReset_error; }

		while( !__bbswim_sdi() ) { if(++syncCntL > 200) goto bbswim_lineReset_error; }

		__asm__ __volatile__ ( "nop \n\t nop \n\t nop \n\t nop \n\t" );
		SPCR = SPCR_SPI_ENA;

	sei();

	__DPRINTFS_X("%d | %d [%d %d]", syncCntH, syncCntL, SWIM_SF_MIN, SWIM_SF_MAX);

	// Done
	return (syncCntL >= SWIM_SF_MIN && syncCntL <= SWIM_SF_MAX);

	// Error
bbswim_lineReset_error:
	sei();               // Enable interrupt
	SPCR = SPCR_SPI_ENA; // Enable SPI
	return false;        // Exit error
}


bool bbswim_transfer(uint8_t* buff, _SInt_IZArg_t size, _SInt_IZArg_t size2)
{
	// Error if the SWIM is not enabled
	if( !bbswim_isEnabled() ) return false;

	// Transfer the byte(s)
	cli();

	SPDR = *buff;

	if(!size2) {
		while(--size) {

			const uint8_t outVal = *(buff + 1);

			while( !( SPSR & _BV(SPIF) ) );

			const uint8_t inpVal = SPDR  ;
			              SPDR   = outVal;

			*buff++ = inpVal;

		} // while
	}
	else {
		while(--size) {

			const uint8_t outVal = *(buff + 1);

			while( !( SPSR & _BV(SPIF) ) );

			const uint8_t inpVal = SPDR  ;
			              SPDR   = outVal;

			if(size == size2) SPSR = SPSR_SPI_C8M;

			*buff++ = inpVal;

		} // while
	}

	while( !( SPSR & _BV(SPIF) ) );
	*buff = SPDR;

	if(size2) SPSR = SPSR_SPI_C4M;

	sei();

	// Done
	return true;
}


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
				if( __bbswim_tx(*buff, 8) ) break;
			}
			if(retryCount == 255) goto bbswim_cmd_wotf_error;
		sei();
		++buff;
	} // while

	BBSWIM_BB_LEAVE();

	// Done
	return true;

	// Error
bbswim_cmd_wotf_error:
	BBSWIM_BB_ERROR();
	return false;
}
