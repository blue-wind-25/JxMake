/*
 * Please refer to the notes at the top of the 'main.cpp' file for more information.
 */

#include <stdint.h>

#include "soc/timer_group_reg.h"


// Helper macros
#define _BV(BIT_POS)  ( (uint32_t) (               1U     << ( (uint32_t) (BIT_POS) ) ) )
#define _VV(VAL, POS) ( (uint32_t) ( ( (uint32_t) (VAL) ) << ( (uint32_t) (POS)     ) ) )

#ifdef ENABLE_CACHE_ACCESS
    // ##### !!! TODO : Is this actually correct? !!! #####
    #define DFLASH __attribute__(( section(".dflash"), aligned(4) ))
#else
    #define DFLASH
#endif


#ifdef __cplusplus
extern "C" {
#endif


// The user entry function
extern void kernel_main();


// ROM functions
extern void mmu_init(int cpu_no);
extern unsigned int cache_flash_mmu_set_rom(int cpu_no, int pid, unsigned int vaddr, unsigned int paddr, int psize, int num);

extern void Cache_Flush_rom(int cpu_no);
extern void Cache_Read_Disable_rom(int cpu_no);
extern void Cache_Read_Enable_rom(int cpu_no);
extern void Cache_Read_Init_rom(int cpu_no);

extern void uartAttach();

extern int uart_rx_one_char(uint8_t* pRxChar);
extern int uart_tx_one_char(uint8_t);
extern void uart_tx_flush(uint8_t uart_no);
extern void uart_tx_switch(uint8_t uart_no);

extern void ets_install_uart_printf();
extern int ets_printf(const char* fmt, ...);


// GPIO functions
extern void gpio_output(uint32_t pin);
extern void gpio_input(uint32_t pin);
extern void gpio_write(uint32_t pin, uint32_t val);
extern uint32_t gpio_read(uint32_t pin);


// UART functions
extern void uart_tx(const char* str);


// APP_CPU functions
typedef void(*APP_CPU_MAIN)();

extern volatile APP_CPU_MAIN f_cpu1_main;

void start_app_cpu(APP_CPU_MAIN fnAppCPUMain);


// CPU functions
extern void global_lock();
extern void global_unlock();

static inline __attribute__(( always_inline )) uint32_t get_core_id()
{
    uint32_t id;

    __asm__ volatile(
        "rsr.prid %0         \n\t"
        "extui %0, %0, 13, 1 \n\t"
        : "=r"(id)
    );
    return id;
}


// Timing functions
extern volatile uint32_t __timerLockOwner;

static inline __attribute__(( always_inline )) void nop1()
{ __asm__ volatile("nop"); }

static inline __attribute__(( always_inline )) uint64_t systick()
{
    // Lock access to this function so that the timer value can be read exclusively
    const uint32_t currentCoreID = get_core_id() + 1;

    while(__timerLockOwner != 0 && __timerLockOwner != currentCoreID);
    __timerLockOwner = currentCoreID;

    // Read the timer value
    REG_WRITE( TIMG_T0UPDATE_REG(0), 1 );

    nop1();

    const uint64_t tick = ( ( (uint64_t) REG_READ( TIMG_T0HI_REG(0) ) ) << 32U ) | REG_READ( TIMG_T0LO_REG(0) );

    // Unlock the access
    __timerLockOwner = 0;

    // Return the timer value
    return tick ;
}

static inline __attribute__(( always_inline )) uint64_t uptime_us()
{ return systick() >> 5; }

static inline __attribute__(( always_inline )) void delay_us(uint64_t us)
{
    const uint64_t until = uptime_us() + us;
    while( uptime_us() < until ) nop1();
}

static inline __attribute__(( always_inline )) void delay_ms(uint64_t ms)
{ delay_us(ms * 1000U); }


#ifdef __cplusplus
}
#endif
