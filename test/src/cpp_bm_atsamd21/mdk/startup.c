/*
 * Please refer to the notes at the top of the 'mdk.h' file for more information.
 */

#include "mdk.h"


extern unsigned int _DATA_ROM_START;
extern unsigned int _DATA_RAM_START;
extern unsigned int _DATA_RAM_END;
extern unsigned int _BSS_START;
extern unsigned int _BSS_END;


#define STACK_TOP 0x20008000


void startup();
void irq_handler_dummy();
void irq_handler_sys_tick();


#define DUMMY_IRQ_HANDLER(NAME) void NAME() __attribute__(( weak, alias ("irq_handler_dummy") ));

DUMMY_IRQ_HANDLER(irq_handler_nmi       );
DUMMY_IRQ_HANDLER(irq_handler_hard_fault);
DUMMY_IRQ_HANDLER(irq_handler_sv_call   );
DUMMY_IRQ_HANDLER(irq_handler_pend_sv   );

DUMMY_IRQ_HANDLER(irq_handler_pm        );
DUMMY_IRQ_HANDLER(irq_handler_sysctrl   );
DUMMY_IRQ_HANDLER(irq_handler_wdt       );
DUMMY_IRQ_HANDLER(irq_handler_rtc       );
DUMMY_IRQ_HANDLER(irq_handler_eic       );
DUMMY_IRQ_HANDLER(irq_handler_nvmctrl   );
DUMMY_IRQ_HANDLER(irq_handler_dmac      );
DUMMY_IRQ_HANDLER(irq_handler_usb       );
DUMMY_IRQ_HANDLER(irq_handler_evsys     );
DUMMY_IRQ_HANDLER(irq_handler_sercom0   );
DUMMY_IRQ_HANDLER(irq_handler_sercom1   );
DUMMY_IRQ_HANDLER(irq_handler_sercom2   );
DUMMY_IRQ_HANDLER(irq_handler_sercom3   );
DUMMY_IRQ_HANDLER(irq_handler_sercom4   );
DUMMY_IRQ_HANDLER(irq_handler_sercom5   );
DUMMY_IRQ_HANDLER(irq_handler_tcc0      );
DUMMY_IRQ_HANDLER(irq_handler_tcc1      );
DUMMY_IRQ_HANDLER(irq_handler_tcc2      );
DUMMY_IRQ_HANDLER(irq_handler_tc3       );
DUMMY_IRQ_HANDLER(irq_handler_tc4       );
DUMMY_IRQ_HANDLER(irq_handler_tc5       );
DUMMY_IRQ_HANDLER(irq_handler_tc6       );
DUMMY_IRQ_HANDLER(irq_handler_tc7       );
DUMMY_IRQ_HANDLER(irq_handler_adc       );
DUMMY_IRQ_HANDLER(irq_handler_ac        );
DUMMY_IRQ_HANDLER(irq_handler_dac       );
DUMMY_IRQ_HANDLER(irq_handler_ptc       );
DUMMY_IRQ_HANDLER(irq_handler_i2s       );


void* myvectors[] __attribute__(( section(".vectors") )) = {
    (void*) STACK_TOP,      //  0 - Stack pointer
    // Cortex-M0+ handlers
    startup,                //  1 - Reset (code entry point)
    irq_handler_nmi,        //  2 - NMI
    irq_handler_hard_fault, //  3 - Hard Fault
    0,                      //  4 - Reserved
    0,                      //  5 - Reserved
    0,                      //  6 - Reserved
    0,                      //  7 - Reserved
    0,                      //  8 - Reserved
    0,                      //  9 - Reserved
    0,                      // 10 - Reserved
    irq_handler_sv_call,    // 11 - SVCall
    0,                      // 12 - Reserved
    0,                      // 13 - Reserved
    irq_handler_pend_sv,    // 14 - PendSV
    irq_handler_sys_tick,   // 15 - SysTick
    // Peripheral handlers
    irq_handler_pm,         //  0 - Power Manager
    irq_handler_sysctrl,    //  1 - System Controller
    irq_handler_wdt,        //  2 - Watchdog Timer
    irq_handler_rtc,        //  3 - Real Time Counter
    irq_handler_eic,        //  4 - External Interrupt Controller
    irq_handler_nvmctrl,    //  5 - Non-Volatile Memory Controller
    irq_handler_dmac,       //  6 - Direct Memory Access Controller
    irq_handler_usb,        //  7 - Universal Serial Bus Controller
    irq_handler_evsys,      //  8 - Event System
    irq_handler_sercom0,    //  9 - Serial Communication Interface 0
    irq_handler_sercom1,    // 10 - Serial Communication Interface 1
    irq_handler_sercom2,    // 11 - Serial Communication Interface 2
    irq_handler_sercom3,    // 12 - Serial Communication Interface 3
    irq_handler_sercom4,    // 13 - Serial Communication Interface 4
    irq_handler_sercom5,    // 14 - Serial Communication Interface 5
    irq_handler_tcc0,       // 15 - Timer/Counter for Control 0
    irq_handler_tcc1,       // 16 - Timer/Counter for Control 1
    irq_handler_tcc2,       // 17 - Timer/Counter for Control 2
    irq_handler_tc3,        // 18 - Timer/Counter 3
    irq_handler_tc4,        // 19 - Timer/Counter 4
    irq_handler_tc5,        // 20 - Timer/Counter 5
    irq_handler_tc6,        // 21 - Timer/Counter 6
    irq_handler_tc7,        // 22 - Timer/Counter 7
    irq_handler_adc,        // 23 - Analog-to-Digital Converter
    irq_handler_ac,         // 24 - Analog Comparator
    irq_handler_dac,        // 25 - Digital-to-Analog Converter
    irq_handler_ptc,        // 26 - Peripheral Touch Controller
    irq_handler_i2s         // 27 - Inter-IC Sound Interface
};


void startup()
{
    // Copy data belonging to the '.data' section from its load time position on flash (ROM) to its run time position in SRAM
    unsigned int* data_rom_start_p = &_DATA_ROM_START;
    unsigned int* data_ram_start_p = &_DATA_RAM_START;
    unsigned int* data_ram_end_p   = &_DATA_RAM_END;

    while(data_ram_start_p != data_ram_end_p) {
        *data_ram_start_p = *data_rom_start_p;
         data_ram_start_p++;
         data_rom_start_p++;
    }

    // Initialize data in the '.bss' section to zeroes
    unsigned int* bss_start_p = &_BSS_START;
    unsigned int* bss_end_p   = &_BSS_END;

    while(bss_start_p != bss_end_p) {
        *bss_start_p = 0;
         bss_start_p++;
    }

    // Call the 'kernel_main()' function
    kernel_main();

    // Make sure this program does never exit
    while(1) {}
}


void irq_handler_dummy()
{ for(;;); }


volatile uint32_t systick_value_ms = 0;

void irq_handler_sys_tick()
{ ++systick_value_ms; }


void sys_init()
{
    // Switch to 8 MHz clock (disable prescaler)
    _SFR(SYSCTRL_OSC8M) &= ~_VV(3, 8);

    // Configure SysTick
    _SFR(STK_CSR)  = _BV(2) | _BV(1); // Set    SysTick clock source to the processor clock and enables SysTick exception request
    _SFR(STK_RVR)  = 8000 - 1;        // Set    SysTick reload value - set period to 1 mS (8 MHz / 8000 = 1000 Hz)
    _SFR(STK_CVR)  = 0;               // Clear  SysTick current value
    _SFR(STK_CSR) |= _BV(0);          // Enable SysTick

    enable_irq(SysTick_IRQn);
}
