/*
 * Please refer to the notes at the top of the 'main.cpp' file for more information.
 */

#include <stddef.h>

#include "soc/dport_reg.h"
#include "soc/rtc_cntl_reg.h"
#include "soc/uart_reg.h"

#include "util.h"


extern char _vector_beg;
extern char _dram_end;

extern char _stext;
extern char _etext;

extern char _sdata;
extern char _edata;

extern char _sbss;
extern char _ebss;

extern char _user_bytes_end;

extern char _sdflash;
extern char _edflash;


static char* s_heap_start;
static char* s_heap_end;
static char* s_brk;


void* sbrk(int diff)
{
    char* old = s_brk;

    if( &s_brk[diff] > s_heap_end ) return NULL;

    s_brk += diff;

    return old;
}


////////////////////////////////////////////////////////////////////////////////////////////////////


static          char         s_cpu1_stack_space[4096];
static volatile uint32_t     s_cpu1_stack_ptr;
static volatile uint32_t     f_cpu1_initialized;
       volatile APP_CPU_MAIN f_cpu1_main;


void _start_cpu1()
{
    // https://github.com/Winkelkatze/ESP32-Bare-Metal-AppCPU

    // Initialize the interrupt handler
    __asm__ volatile(
        "wsr %0, vecbase\n\t"
        : : "r"(&_vector_beg)
    );

    // Initialize debugging
    uartAttach();

    // Initialize the stack pointer
    __asm__ volatile(
        "l32i a1, %0, 0\n\t"
        : : "r"(&s_cpu1_stack_ptr)
    );

    // Set the flag
    f_cpu1_initialized = 1;

    // Loop forever
    for(;;) {

        // Turn the CPU clock off
        DPORT_REG_CLR_BIT(DPORT_APPCPU_CTRL_B_REG, DPORT_APPCPU_CLKGATE_EN);

        // The CPU would still execute 1 instruction after the clock gate is turned off;
        // therefore, put some NOPs here
        __asm__ volatile(
            "nop\n\t" \
            "nop\n\t" \
            "nop\n\t" \
            "nop\n\t" \
            "nop\n\t" \
        );

        // Call the main
        if(f_cpu1_main != NULL) (*f_cpu1_main)();

    } // for
}


////////////////////////////////////////////////////////////////////////////////////////////////////


void _start_cpu0()
{
#ifdef ENABLE_CACHE_ACCESS
    // Enable CPU cache access
    Cache_Flush_rom(0);
    Cache_Flush_rom(1);
    Cache_Read_Enable_rom(0);
    Cache_Read_Enable_rom(1);
#endif

    // Initialize APP_CPU
    s_cpu1_stack_ptr   = ( (uint32_t) &s_cpu1_stack_space[0] ) + sizeof(s_cpu1_stack_space) - sizeof(size_t);
    f_cpu1_initialized = 0;
    f_cpu1_main        = NULL;

    DPORT_REG_CLR_BIT      ( DPORT_APPCPU_CTRL_B_REG, DPORT_APPCPU_CLKGATE_EN ); // Ensure the CPU clock is turned off
    DPORT_REG_SET_BIT      ( DPORT_APPCPU_CTRL_A_REG, DPORT_APPCPU_RESETTING  ); // Reset the CPU
    DPORT_REG_CLR_BIT      ( DPORT_APPCPU_CTRL_A_REG, DPORT_APPCPU_RESETTING  ); // ---
    DPORT_WRITE_PERI_REG   ( DPORT_APPCPU_CTRL_D_REG, (uint32_t) &_start_cpu1 ); // Set the entry vector
    DPORT_SET_PERI_REG_MASK( DPORT_APPCPU_CTRL_B_REG, DPORT_APPCPU_CLKGATE_EN ); // Turn the CPU clock on

    while(!f_cpu1_initialized); // Wait for the CPU to be initialized

    // Initialize memory
    s_heap_start = &_user_bytes_end;
    s_heap_end   = &_dram_end;
    s_brk        = &_user_bytes_end;

    for(char* p = &_sbss; p < &_ebss;) *p++ = '\0';

    // Disable WDT
    REG_WRITE  ( RTC_CNTL_WDTWPROTECT_REG, RTC_CNTL_WDT_WKEY_VALUE ); // Disable the RTC WDT write protection
    REG_SET_BIT( RTC_CNTL_WDTFEED_REG    , RTC_CNTL_WDT_FEED       ); // Feed    the RTC WDT
    REG_WRITE  ( RTC_CNTL_WDTCONFIG0_REG , 0                       ); // Disable the RTC WDT
    REG_WRITE  ( TIMG_WDTCONFIG0_REG(0)  , 0                       ); // Disable the task WDT
    REG_WRITE  ( TIMG_WDTCONFIG0_REG(1)  , 0                       ); // Disable the task WDT

    /*
    // Configure clock and UART0
    REG_WRITE( SYSCON_SYSCLK_CONF_REG   , 0x00002000 ); // CPU_CLK  = (XTL_CLK or RC_FAST_CLK) / 8193 ; when the source of CPU_CLK is XTL_CLK or RC_FAST_CLK
    REG_WRITE( SYSCON_XTAL_TICK_CONF_REG, 0x00000027 ); // REF_TICK = APB_CLK /  40                   ; when the source of APB_CLK is XTL_CLK
    REG_WRITE( SYSCON_PLL_TICK_CONF_REG , 0x0000004f ); // REF_TICK = APB_CLK /  80                   ; when the source of APB_CLK is PLL_CLK
    REG_WRITE( SYSCON_CK8M_TICK_CONF_REG, 0x0000000b ); // REF_TICK = APB_CLK /  12                   ; when the source of APB_CLK is FOSC_CLK
    REG_WRITE( SYSCON_APLL_TICK_CONF_REG, 0x00000063 ); // REF_TICK = APB_CLK / 100                   ; when the source of APB_CLK is APLL_CLK
    REG_WRITE( RTC_CNTL_CLK_CONF_REG    , 0x09580010 );
    REG_WRITE( DPORT_CPU_PER_CONF_REG   , 0x00000000 );
    REG_WRITE( UART_CONF0_REG (0)       , 0x0c00001c );
    REG_WRITE( UART_CONF1_REG (0)       , 0x00000001 );
    REG_WRITE( UART_CLKDIV_REG(0)       , 0x007002b6 );
    //*/

    // Configure clock (CPU_CLK = PLL_CLK / 2 = 160MHz) (APB_CLK = 80MHz)
    REG_WRITE( RTC_CNTL_CLK_CONF_REG, _VV(RTC_CNTL_SOC_CLK_SEL_PLL   , RTC_CNTL_SOC_CLK_SEL_S) |  // SEL_0 = 1 : 0=XTAL ; 1=PLL  ; 2=CK8M - 3=APLL
                                      _VV(RTC_CNTL_CK8M_DFREQ_DEFAULT, RTC_CNTL_CK8M_DFREQ_S ) |
                                      _VV(1                          , RTC_CNTL_CK8M_DIV_S   ) );
    REG_WRITE( DPORT_CPU_PER_CONF_REG, DPORT_CPUPERIOD_SEL_160                                 ); // SEL_1 = 0 : 0=80m  ; 1=160m ; 2=240m
  //REG_WRITE( DPORT_CPU_PER_CONF_REG, DPORT_CPUPERIOD_SEL_240                                 );

    // Configure UART0
    // ##### !!! TODO : UART0 does not work when not using bootloader and CPU_CLK is set to 240MHz !!! #####
    REG_WRITE( UART_CONF0_REG (0), UART_TICK_REF_ALWAYS_ON     |  // UART_CLK = APB_CLK (80MHz)
                                   _VV(1, UART_STOP_BIT_NUM_S) |  // 1 stop bit
                                   _VV(3, UART_BIT_NUM_S     ) ); // 8 data bit
    REG_WRITE( UART_CLKDIV_REG(0), 0x007002b6                  ); // Set the clock divider to 0111 00000000001010110110 ( 7  694) to get 115200 baud ; when UART_CLK = APB_CLK
  //REG_WRITE( UART_CLKDIV_REG(0), 0x00A00008                  ); // Set the clock divider to 0101 00000000100000100011 (10    8) to get 115200 baud ; when UART_CLK = REF_TICK

    /*
    #define RTC_APB_FREQ_REG        RTC_CNTL_STORE5_REG

    uint32_t freq_hz   = ( ( READ_PERI_REG(RTC_APB_FREQ_REG) & UINT16_MAX ) << 12U ) + 500000U;
    uint32_t remainder = freq_hz % 1000000U;
             freq_hz   = (freq_hz - remainder);
    uint32_t clk_div   = (freq_hz << 4) / 115200U;

    REG_WRITE( UART_CLKDIV_REG(0), ( (clk_div &  0x0fU) << 20U ) | (clk_div >> 4U) );
    */

    /*
    REG_WRITE  ( UART_CONF1_REG(0)    , _VV(0x60, UART_TXFIFO_EMPTY_THRHD_S) | _VV(0x60, UART_RXFIFO_FULL_THRHD_S) );
    REG_WRITE  ( UART_FLOW_CONF_REG(0), 0                                                                          );
    REG_WRITE  ( UART_MEM_CONF_REG(0) , _VV(1, UART_TX_SIZE_S) | _VV(1, UART_RX_SIZE_S)                            );
    REG_WRITE  ( UART_INT_ENA_REG(0)  , 0                                                                          );
    REG_WRITE  ( UART_INT_CLR_REG(0)  , 0x0007ffff                                                                 );
    REG_CLR_BIT( UART_AUTOBAUD_REG(0) , UART_AUTOBAUD_EN                                                           );
    //*/

    // Enable TIMG0 (TIMG = APB_CLK)
    REG_WRITE( TIMG_T0CONFIG_REG(0), TIMG_T0_EN | TIMG_T0_INCREASE | TIMG_T0_AUTORELOAD | _VV(1, TIMG_T0_DIVIDER_S) );

    // Call the main
    kernel_main();

    // Loop forever
    for(;;);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


// ##### !!! TODO : Is this actually working properly? !!! #####


       volatile uint32_t __timerLockOwner  = 0;

static volatile uint32_t __globalLockOwner = 0;
static volatile uint32_t __globalLockCnt   = 0;


static uint32_t __global_lock()
{
    const uint32_t currentCoreID = get_core_id() + 1;

    if(__globalLockOwner != 0 && __globalLockOwner != currentCoreID) return 0;

    __globalLockOwner = currentCoreID;
    ++__globalLockCnt;

    return 1;
}

void global_lock()
{ while( !__global_lock() ); }


static uint32_t __global_unlock()
{
    const uint32_t currentCoreID = get_core_id() + 1;

    if(__globalLockOwner == 0 || __globalLockOwner != currentCoreID) return 0;

    --__globalLockCnt;
    if(__globalLockCnt == 0) __globalLockOwner = 0;

    return 1;
}

void global_unlock()
{ __global_unlock(); }
