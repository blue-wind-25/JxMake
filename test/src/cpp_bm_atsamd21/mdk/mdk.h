/*
 * This source code is written based on:
 *     Adafruit Trinket M0 Bare Metal C Programming
 *     https://github.com/konimarti/trinketm0
 *     Copyright (C) 2020 konimarti (https://github.com/konimarti)
 *     No License Specified
 *
 * Some information from the following projects is also used to modify the source code:
 *     Bare Metal Examples for the Atmel SAM D Family
 *         https://github.com/dwelch67/atsamd_samples
 *         Copyright (C) 2020 David Welch
 *     Bare-Metal MCU Starter Projects
 *         https://github.com/ataradov/mcu-starter-projects/tree/master/samd21
 *         Copyright (C) 2022 Alex Taradov. All rights reserved
 *
 * Some information from the URLs below is also used to modify the source code:
 *     https://ww1.microchip.com/downloads/aemDocuments/documents/MCU32/ProductDocuments/DataSheets/SAM-D21-DA1-Family-Data-Sheet-DS40001882H.pdf
 *     https://microchipdeveloper.com/32arm:samd21-systick
 *     https://developer.arm.com/documentation/dui0662/b/Cortex-M0--Peripherals/System-timer--SysTick?lang=en
 *     https://developer.arm.com/documentation/dui0662/b/Cortex-M0--Peripherals/Nested-Vectored-Interrupt-Controller?lang=en
 */

#pragma once


#include <stdint.h>


#define SYSCTRL_BASE        0x40000800U
#define SYSCTRL_OSC8M       (SYSCTRL_BASE + 0x20U)

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
#define PM_IRQn              0U
#define SYSCTRL_IRQn         1U
#define WDT_IRQn             2U
#define RTC_IRQn             3U
#define EIC_IRQn             4U
#define NVMCTRL_IRQn         5U
#define DMAC_IRQn            6U
#define USB_IRQn             7U
#define EVSYS_IRQn           8U
#define SERCOM0_IRQn         9U
#define SERCOM1_IRQn        10U
#define SERCOM2_IRQn        11U
#define SERCOM3_IRQn        12U
#define SERCOM4_IRQn        13U
#define SERCOM5_IRQn        14U
#define TCC0_IRQn           15U
#define TCC1_IRQn           16U
#define TCC2_IRQn           17U
#define TC3_IRQn            18U
#define TC4_IRQn            19U
#define TC5_IRQn            20U
#define TC6_IRQn            21U
#define TC7_IRQn            22U
#define ADC_IRQn            23U
#define AC_IRQn             24U
#define DAC_IRQn            25U
#define PTC_IRQn            26U
#define I2S_IRQn            27U


#define PORTBASE            0x41004400U

#define PORTA_DIRCLR        ( PORTBASE + 0x04U                    )
#define PORTA_DIRSET        ( PORTBASE + 0x08U                    )
#define PORTA_OUTCLR        ( PORTBASE + 0x14U                    )
#define PORTA_OUTSET        ( PORTBASE + 0x18U                    )
#define PORTA_IN            ( PORTBASE + 0x20U                    )
#define PORTA_PINCFG(N)     ( PORTBASE + 0x40U + ( (uint32_t) N ) )

#define PORTB_DIRCLR        ( PORTBASE + 0x84U                    )
#define PORTB_DIRSET        ( PORTBASE + 0x88U                    )
#define PORTB_OUTCLR        ( PORTBASE + 0x94U                    )
#define PORTB_OUTSET        ( PORTBASE + 0x98U                    )
#define PORTB_IN            ( PORTBASE + 0xA0U                    )
#define PORTB_PINCFG(N)     ( PORTBASE + 0xC0U + ( (uint32_t) N ) )


#define _SFR(ADDRESS) ( *( (volatile uint32_t*) (ADDRESS) ) )

#define _BV(BIT_POS)  ( (uint32_t) (               1U     << ( (uint32_t) (BIT_POS) ) ) )
#define _VV(VAL, POS) ( (uint32_t) ( ( (uint32_t) (VAL) ) << ( (uint32_t) (POS)     ) ) )


#ifdef __cplusplus
extern "C" {
#endif


extern void kernel_main();
extern void sys_init();


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
