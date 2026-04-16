/*
 * Please refer to the notes at the top of the 'mdk.h' file for more information.
 */

#include "mdk.h"


// These must be defined in the linker script
extern unsigned long _etext;
extern unsigned long _srelocate;
extern unsigned long _erelocate;
extern unsigned long _sbss;
extern unsigned long _ebss;
extern unsigned long _estack;


// Interrupt vectors
typedef void(*const intfunc)();

void Reset_Handler  () __attribute__(( __interrupt__ ));
void SysTick_Handler() __attribute__(( __interrupt__ ));
void Default_Handler() __attribute__(( __interrupt__ ));

#define NMI_Handler         Default_Handler
#define HardFault_Handler   Default_Handler
#define MemManage_Handler   Default_Handler
#define BusFault_Handler    Default_Handler
#define UsageFault_Handler  Default_Handler
#define MemManage_Handler   Default_Handler
#define SVC_Handler         Default_Handler
#define DebugMon_Handler    Default_Handler
#define PendSV_Handler      Default_Handler

void (*const g_pfnVectors[])() __attribute__ (( section(".vectors") )) = {
    (intfunc)((unsigned long)&_estack), /* The stack pointer after relocation */
    Reset_Handler,                      /* Reset Handler */
    NMI_Handler,                        /* NMI Handler */
    HardFault_Handler,                  /* Hard Fault Handler */
    MemManage_Handler,                  /* MPU Fault Handler */
    BusFault_Handler,                   /* Bus Fault Handler */
    UsageFault_Handler,                 /* Usage Fault Handler */
    0,                                  /* Reserved */
    0,                                  /* Reserved */
    0,                                  /* Reserved */
    0,                                  /* Reserved */
    SVC_Handler,                        /* SVCall Handler */
    DebugMon_Handler,                   /* Debug Monitor Handler */
    0,                                  /* Reserved */
    PendSV_Handler,                     /* PendSV Handler */
    SysTick_Handler                     /* SysTick Handler */
};


// Interrupt handlers
#define SCB_VTOR_ADDR 0xE000ED08
#define SCB_VTOR      ( *(volatile uint32_t*) SCB_VTOR_ADDR )

void Reset_Handler()
{
    /* Init Data:
     * - Loads data from addresses defined in linker file into RAM
     * - Zero bss (statically allocated uninitialized variables)
     */
    unsigned long *src, *dst;

    /* Copy the data segment into RAM */
    src = &_etext;
    dst = &_srelocate;
    if(src != dst) {
        while(dst < &_erelocate) *(dst++) = *(src++);
    }

    /* Zero the bss segment */
    dst = &_sbss;
    while(dst < &_ebss) *(dst++) = 0;

    SCB_VTOR = ( (uint32_t) g_pfnVectors ) & 0x1FFFFF80U;

    kernel_main();
    while(1) {}
}


volatile uint32_t systick_value_ms = 0;

void SysTick_Handler()
{ ++systick_value_ms; }


void Default_Handler()
{ for(;;); }


void HardwareInit()
{
    // NOTE : With a 12 MHz crystal, the default CPU clock (if we do nothing here) should be 3 MHz

    // Enable peripheral clock for all PIOs
    PMC_PCER0 = _BV(ID_PIOA) | _BV(ID_PIOB) | _BV(ID_PIOC) | _BV(ID_PIOD) | _BV(ID_PIOE) | _BV(ID_PIOF);

    // Configure SysTick
    _SFR(STK_CSR)  = _BV(2) | _BV(1); // Set    SysTick clock source to the processor clock and enables SysTick exception request
    _SFR(STK_RVR)  = 3000 - 1;        // Set    SysTick reload value - set period to 1 mS (3 MHz / 3000 = 1000 Hz)
    _SFR(STK_CVR)  = 0;               // Clear  SysTick current value
    _SFR(STK_CSR) |= _BV(0);          // Enable SysTick

    enable_irq(SysTick_IRQn);
}
