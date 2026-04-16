/*
 * This source code is written based on:
 *     Bare Bones Projects for Microchip SAM3X8E (Arduino Due) - Minimal Blink for SAM3X (Arduino Due)
 *     https://github.com/bobc/bare-metal-sam3x
 *     Copyright (C) 2017 Bob Cousins
 *     MIT License
 *
 * Some information from the URL below is also used to modify the source code:
 *     https://ww1.microchip.com/downloads/en/devicedoc/atmel-11057-32-bit-cortex-m3-microcontroller-sam3x-sam3a_datasheet.pdf
 */

#pragma once


#include <stdint.h>


#define SCS_BASE            0xE000E000U
#define STK_BASE            (SCS_BASE + 0x0010U)
#define NVIC_BASE           (SCS_BASE + 0x0100U)
#define SCB_BASE            (SCS_BASE + 0x0D00U)

#define STK_CSR             (STK_BASE  + 0x00U ) // SysTick Control and Status register
#define STK_RVR             (STK_BASE  + 0x04U ) // SysTick Reload Value       register
#define STK_CVR             (STK_BASE  + 0x08U ) // SysTick Current Value      register
#define STK_CALIB           (STK_BASE  + 0x0CU ) // SysTick Calibration Value  register
#define STK_MASK            0x00FFFFFF           // SysTick value mask

#define NVIC_ISER           (NVIC_BASE + 0x000U) // Interrupt Set-Enable    register
#define NVIC_ICER           (NVIC_BASE + 0x080U) // Interrupt Clear-Enable  register
#define NVIC_ISPR           (NVIC_BASE + 0x100U) // Interrupt Set-Pending   register
#define NVIC_ICPR           (NVIC_BASE + 0x180U) // Interrupt Clear-Pending register

#define NonMaskableInt_IRQn ( (uint32_t) -14 )
#define HardFault_IRQn      ( (uint32_t) -13 )
#define SVCall_IRQn         ( (uint32_t)  -5 )
#define PendSV_IRQn         ( (uint32_t)  -2 )
#define SysTick_IRQn        ( (uint32_t)  -1 )


#define _SFR(ADDRESS) ( *( (volatile uint32_t*) (ADDRESS) ) )

#define _BV(BIT_POS)  ( (uint32_t) (               1U     << ( (uint32_t) (BIT_POS) ) ) )
#define _VV(VAL, POS) ( (uint32_t) ( ( (uint32_t) (VAL) ) << ( (uint32_t) (POS)     ) ) )


// PMC definitions
#define PMC_PCER0 ( *(volatile uint32_t*) 0x400E0610 )

#define ID_PIOA   11
#define ID_PIOB   12
#define ID_PIOC   13
#define ID_PIOD   14
#define ID_PIOE   15
#define ID_PIOF   16

#define PMC_WPMR  ( *(volatile uint32_t*) 0x400E06E4 )
#define PMC_WPKEY 0x504D43


// PIO definitions
struct gpio {
    // +0x00
    volatile uint32_t PIO_PER;
    volatile uint32_t PIO_PDR;
    volatile uint32_t PIO_PSR;
    volatile uint32_t res1;
    // +0x10
    volatile uint32_t PIO_OER;
    volatile uint32_t PIO_ODR;
    volatile uint32_t PIO_OSR;
    volatile uint32_t res2;
    // +0x20
    volatile uint32_t PIO_IFER;
    volatile uint32_t PIO_IFDR;
    volatile uint32_t PIO_IFSR;
    volatile uint32_t res3;
    // +0x30
    volatile uint32_t PIO_SODR;
    volatile uint32_t PIO_CODR;
    volatile uint32_t PIO_ODSR;
    volatile uint32_t PIO_PDSR;
    // +0x40
    volatile uint32_t PIO_IER;
    volatile uint32_t PIO_IDR;
    volatile uint32_t PIO_IMR;
    volatile uint32_t PIO_ISR;
    // +0x50
    volatile uint32_t PIO_MDER;
    volatile uint32_t PIO_MDDR;
    volatile uint32_t PIO_MDSR;
    volatile uint32_t res4;
    // +0x60
    volatile uint32_t PIO_PUDR;
    volatile uint32_t PIO_PUER;
    volatile uint32_t PIO_PUSR;
    volatile uint32_t res5;
    // +0x70
    volatile uint32_t PIO_ABSR;
    volatile uint32_t res6[3];
    // +0x80
    volatile uint32_t PIO_SCIFSR;
    volatile uint32_t PIO_DIFSR;
    volatile uint32_t PIO_IFDGSR;
    volatile uint32_t PIO_SCDR;
    // +0x90
    volatile uint32_t res7[4];
    // +0xA0
    volatile uint32_t PIO_OWER;
    volatile uint32_t PIO_OWDR;
    volatile uint32_t PIO_OWSR;
    volatile uint32_t res8;
    // ...
};

#define PIOA      ( (volatile struct gpio*) 0x400E0E00 )
#define PIOB      ( (volatile struct gpio*) 0x400E1000 )
#define PIOC      ( (volatile struct gpio*) 0x400E1200 )
#define PIOD      ( (volatile struct gpio*) 0x400E1400 )
#define PIOE      ( (volatile struct gpio*) 0x400E1600 )
#define PIOF      ( (volatile struct gpio*) 0x400E1800 )

#define PIOA_WPMR ( *(volatile uint32_t*) 0x400E0EE4 )
#define PIOB_WPMR ( *(volatile uint32_t*) 0x400E10E4 )
#define PIOC_WPMR ( *(volatile uint32_t*) 0x400E12E4 )
#define PIOD_WPMR ( *(volatile uint32_t*) 0x400E14E4 )
#define PIOE_WPMR ( *(volatile uint32_t*) 0x400E16E4 )

#define PIO_WPKEY 0x50494F


#ifdef __cplusplus
extern "C" {
#endif


extern void kernel_main();

extern void HardwareInit();


static inline void __spin_delay(uint32_t time)
{ while(time--) __asm volatile("nop"); }


static inline void enable_irq(uint32_t IRQn)
{ _SFR(NVIC_ISER) = _BV(IRQn & 0x1FU); }

static inline void disable_irq(uint32_t IRQn)
{ _SFR(NVIC_ICER) = _BV(IRQn & 0x1FU); }

static inline void set_pending_irq(uint32_t IRQn)
{ _SFR(NVIC_ISPR) = _BV(IRQn & 0x1FU); }

static inline void clear_pending_irq(uint32_t IRQn)
{ _SFR(NVIC_ICPR) = _BV(IRQn & 0x1FU); }

static inline uint32_t get_pending_irq(uint32_t IRQn)
{ return ( _SFR(NVIC_ISPR) & _BV(IRQn & 0x1FU) ) ? 1U : 0U; }


extern volatile uint32_t systick_value_ms;
static inline uint32_t systick_ms()
{ return systick_value_ms; }


static inline void delay_ms(uint32_t ms)
{
    const uint32_t until = systick_ms() + ms;

    while( systick_ms() < until ) {
        __asm__ volatile("nop");
    }
}


#ifdef __cplusplus
} // extern "C"
#endif
