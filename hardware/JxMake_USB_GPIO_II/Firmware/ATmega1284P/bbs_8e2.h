/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


// Bit banging serial 8E2

/*
 * According to:
 *     AVR Baud Rate Tables
 *     https://cache.amobbs.com/bbs_upload782111/files_22/ourdev_508497.html
 * 7200 or 19200 is the best initial baudrate.
 *
 * Therefore with F_CPU 11.0592MHz -> 11059200 /  7200 = 1536 cycles delay
 *                                 -> 11059200 / 19200 =  576 cycles delay
 */

#define DELAY_3D4_L(DELAY, LABEL1, LABEL2)                                            \
		"    push r16                          \n\t" /* [ 2  ] --- Delay ---       */ \
		"    ldi  r16        , " #DELAY "      \n\t" /* [ 1  ] ---                 */ \
		"delay3d4_" #LABEL1 #LABEL2 ":         \n\t" /*        --- Σ = 3*D - 1 + 5 */ \
		"    dec  r16                          \n\t" /* [ 1  ] ---                 */ \
		"    brne delay3d4_" #LABEL1 #LABEL2 " \n\t" /* [ 2/1] ---                 */ \
		"    pop  r16                          \n\t" /* [ 2  ] -------------       */

#define DELAY_BIT_576(LABEL)                                      \
		DELAY_3D4_L(188, LABEL, A) /* [568 ] 576 - 6 - 2 = 568 */

#define DELAY_BIT_1536(LABEL)                                     \
		DELAY_3D4_L(252, LABEL, A) /* [760 ] 768 - 6 - 2 = 760 */ \
		"    wdr             \n\t" /* [ 1  ]                   */ \
		DELAY_3D4_L(254, LABEL, B) /* [766 ]                   */ \
		"    wdr             \n\t" /* [ 1  ]                   */

#if 1
	#define DELAY_BIT(LABEL) DELAY_BIT_576 (LABEL) // 19200
#else
	#define DELAY_BIT(LABEL) DELAY_BIT_1536(LABEL) //  7200
#endif

#define SEND_BIT_6_2(REG, BIT, LABEL)                                                                     \
		"    sbrs " #REG    ", " #BIT"    \n\t" /* [ 2/1] Skip the next instruction if the bit is zero */ \
		"    rjmp .+2                     \n\t" /* [ 2  ] Send zero                                    */ \
		"    rjmp .+6                     \n\t" /* [ 2  ] Send one                                     */ \
		"    nop                          \n\t" /* [ 1  ]                                              */ \
		"    cbi  %[out]     , %[bit]     \n\t" /* [ 2  ] Txd low  - pre delay 6 - post delay 2        */ \
		"    rjmp .+6                     \n\t" /* [ 2  ]                                              */ \
		"    sbi  %[out]     , %[bit]     \n\t" /* [ 2  ] Txd high - pre delay 6 - post delay 2        */ \
		"    nop                          \n\t" /* [ 1  ]                                              */ \
		"    nop                          \n\t" /* [ 1  ]                                              */ \
		DELAY_BIT(LABEL)                        /* [--- ] Delay bit (see above)                        */


// Transmit one character using bit banging
static inline void bbtx_8e2(uint8_t value)
{
	// Calculate the parity and prepare the special value
	uint8_t special = value;

	special ^= special >> 4;
	special ^= special >> 2;
	special ^= special >> 1;
	special <<= 1;

	special |= 0b00000100;

	__asm__ __volatile__ (

		// Disable interrupt
		"    cli                          \n\t" // [ 1  ]

		// Ensure idle state
		"    sbi  %[out]     , %[bit]     \n\t" // [ 2  ] Txd high

		// Initialization
		"    mov  r16        , %[spc]     \n\t" // [ 1  ]
		"    mov  r17        , %[val]     \n\t" // [ 1  ]

		// Send the start bit
		SEND_BIT_6_2(r16, 0, BSTART)

		// Send the data bits
		SEND_BIT_6_2(r17, 0, BDATA0)
		SEND_BIT_6_2(r17, 1, BDATA1)
		SEND_BIT_6_2(r17, 2, BDATA2)
		SEND_BIT_6_2(r17, 3, BDATA3)
		SEND_BIT_6_2(r17, 4, BDATA4)
		SEND_BIT_6_2(r17, 5, BDATA5)
		SEND_BIT_6_2(r17, 6, BDATA6)
		SEND_BIT_6_2(r17, 7, BDATA7)

		// Send the parity bit
		SEND_BIT_6_2(r16, 1, BEPRTY)

		// Send the stop bits
		SEND_BIT_6_2(r16, 2, BSTOP1)
		SEND_BIT_6_2(r16, 2, BSTOP2)

		// Enable interrupt
		"    sei                          \n\t" // [ 1  ]

		// Output
		:
		// Input
		: [spc] "r"( special                 ),
		  [val] "r"( value                   ),
		  [out] "I"( _SFR_IO_ADDR(TXD1_PORT) ),
		  [bit] "I"( TXD1_BIT                )
		// Clobber
		: "r16", "r17"

	); // asm
}


// Transmit multiple characters using bit banging
static inline void bbtx_8e2_multi(const uint8_t* value, uint8_t len)
{
	//cli();
	wdt_reset();

	// NOTE : When sending the maximum number of bytes (254), this line should
	//        not take more than ~160mS/~425mS to complete
	for(uint8_t i = 0; i < len; ++i) bbtx_8e2(*value++);

	wdt_reset();
	//sei();
}
