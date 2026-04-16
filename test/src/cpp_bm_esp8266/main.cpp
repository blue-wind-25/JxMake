/*
 * This firmware is written for:
 *     NodeMCU DEVKIT 1.0
 *     https://en.wikipedia.org/wiki/NodeMCU
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This firmware is written based on the source code and information from:
 *
 *     https://stackoverflow.com/a/63908556
 *     https://esp8266.ru/esp8266-gpio-register
 *
 *     https://github.com/esp8266/esp8266-wiki/wiki/Memory-Map
 *     https://github.com/esp8266/Arduino/blob/master/cores/esp8266/esp8266_peri.h
 *         Copyright (C) 2015 Hristo Gochkov. All rights reserved.
 *         This file is part of the esp8266 core for Arduino environment.
 *         GNU Lesser General Public License version 2.1 or any later version.
 *
 *     https://github.com/esp8266/Arduino/blob/master/cores/esp8266/core_esp8266_wiring_digital.cpp
 *         Copyright (C) 2015 Hristo Gochkov. All rights reserved.
 *         This file is part of the esp8266 core for Arduino environment.
 *         GNU Lesser General Public License version 2.1 or any later version.
 */

#include <stdint.h>


// Define a register
#define _REG(A) ( 0x60000000 + (A) )

// CPU register
#define CPU2X _REG(0x14) // Bit 0 : 0=80MHz ; 1=160MHz

// GPIO (0-15) control registers
#define GPO   _REG(0x300) // GPIO_OUT        RW (Output Level)
#define GPOS  _REG(0x304) // GPIO_OUT_SET    WO
#define GPOC  _REG(0x308) // GPIO_OUT_CLR    WO
#define GPE   _REG(0x30C) // GPIO_ENABLE     RW (Enable)
#define GPES  _REG(0x310) // GPIO_ENABLE_SET WO
#define GPEC  _REG(0x314) // GPIO_ENABLE_CLR WO
#define GPI   _REG(0x318) // GPIO_IN         RO (Read Input Level)
#define GPIE  _REG(0x31C) // GPIO_STATUS     RW (Interrupt Enable)
#define GPIES _REG(0x320) // GPIO_STATUS_SET WO
#define GPIEC _REG(0x324) // GPIO_STATUS_CLR WO

// GPIO (0-15) pin control registers
#define GPC(P) _REG( 0x328 + ( ( (P) & 0xF) * 4 ) )

// GPIO (0-15) pin control bits
#define GPCWE 10 // WAKEUP_ENABLE : can be 1 only when INT_TYPE is high or low
#define GPCI   7 // INT_TYPE      : 0=disable   ; 1=rising     ; 2=falling ; 3=change ; 4=low ; 5=high
#define GPCD   2 // DRIVER        : 0=normal    ; 1=open drain
#define GPCS   0 // SOURCE        : 0=GPIO_DATA ; 1=SigmaDelta

// GPIO (0-15) pin function registers
#define GPF0    _REG(0x834)
#define GPF1    _REG(0x818)
#define GPF2    _REG(0x838)
#define GPF3    _REG(0x814)
#define GPF4    _REG(0x83C)
#define GPF5    _REG(0x840)
#define GPF6    _REG(0x81C)
#define GPF7    _REG(0x820)
#define GPF8    _REG(0x824)
#define GPF9    _REG(0x828)
#define GPF10   _REG(0x82C)
#define GPF11   _REG(0x830)
#define GPF12   _REG(0x804)
#define GPF13   _REG(0x808)
#define GPF14   _REG(0x80C)
#define GPF15   _REG(0x810)

#define GPFN(N) GPF##N

// GPIO (0-15) pin function bits
#define GPFSOE 0 // Sleep OE
#define GPFSS  1 // Sleep select
#define GPFSPD 2 // Sleep pulldown
#define GPFSPU 3 // Sleep pullup
#define GPFFS0 4 // Function select bit 0
#define GPFFS1 5 // Function select bit 1
#define GPFPD  6 // Pull-down
#define GPFPU  7 // Pull-up
#define GPFFS2 8 // Function select bit 2

#define GPFFS(F)      ( ( ( ( (F) & 4 ) != 0 ) << GPFFS2 ) | ( ( ( (F) & 2 ) != 0 ) << GPFFS1 ) | ( ( ( (F) & 1 ) != 0 ) << GPFFS0 ) )
#define GPFFS_GPIO(P) ( ( (P) == 0 || (P) == 2 || (P) == 4 || (P) == 5 ) ? 0 : ( (P) == 16 ) ? 1 : 3 )

// Helper macros
#define _BV(N)           ( 1U << (N) )

#define GPIO_MODE_OUT(P) PUT32( GPFN(P), GPFFS( GPFFS_GPIO(P) )                           ); \
                         PUT32( GPC(P) , ( GET32( GPC(P) ) & (0x0F << GPCI) )             ); \
                         PUT32( GPES   , _BV(P)                                           )

#define GPIO_MODE_INP(P) PUT32( GPFN(P), GPFFS( GPFFS_GPIO(P) )                           ); \
                         PUT32( GPC(P) , ( GET32( GPC(P) ) & (0x0F << GPCI) ) | _BV(GPCD) ); \
                         PUT32( GPEC   , _BV(P)                                           )


// C function prototypes
extern "C" {

    // ROM functions
    extern void ets_delay_us            (uint32_t us);
    extern void ets_update_cpu_frequency(uint32_t fr);

    // System functions
    extern void     PUT32(uint32_t, uint32_t);
    extern uint32_t GET32(uint32_t);

    // Entry point function
    extern void kernel_main();

} // extern "C"


// Define the pins
#define USER_LED0  2
#define USER_LED1 12
#define USER_LED2 13

#define USER_SW   14


// Helper functions
static uint32_t fast = !0;

static inline void  __attribute__(( always_inline )) delay_ms(uint32_t ms)
{
#if 1
    for(uint32_t i = 0; i < ms; ++i) {
       __asm__ volatile(
           "   movi   %0, 20000  \n\t"
           "1: addi   %0, %0, -1 \n\t"
           "   bnez.n %0, 1b     \n\t"
           : : "a"(0)
       );
    }
#else
    ets_delay_us(ms * 1000);
#endif
}

static inline void  __attribute__(( always_inline )) delayAndCheckSwitch()
{
    // Delay and check the switch
    for(uint32_t i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay_ms(10);
        // Check if the switch is pressed
        if( !( GET32(GPI) & _BV(USER_SW) ) ) {
            // Debouncing
            delay_ms(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !( GET32(GPI) & _BV(USER_SW) ) );
}


// ##### ??? TODO : Add SysTick support ??? #####


// The main program
void kernel_main()
{
    ets_update_cpu_frequency(80);
    PUT32( CPU2X, GET32(CPU2X) & 0xFFFFFFFEU );

    GPIO_MODE_OUT(USER_LED0);
    GPIO_MODE_OUT(USER_LED1);
    GPIO_MODE_OUT(USER_LED2);
    GPIO_MODE_INP(USER_SW  );

    fast = !fast;

    for(;;) {

        PUT32( GPOS, _BV(USER_LED0) );
            PUT32( GPOS, _BV(USER_LED1) );
            PUT32( GPOC, _BV(USER_LED2) );
            delayAndCheckSwitch();
            PUT32( GPOC, _BV(USER_LED1) );
            PUT32( GPOS, _BV(USER_LED2) );
            delayAndCheckSwitch();

        PUT32( GPOC, _BV(USER_LED0) );
            PUT32( GPOS, _BV(USER_LED1) );
            PUT32( GPOC, _BV(USER_LED2) );
            delayAndCheckSwitch();
            PUT32( GPOC, _BV(USER_LED1) );
            PUT32( GPOS, _BV(USER_LED2) );
            delayAndCheckSwitch();

    } // for
}
