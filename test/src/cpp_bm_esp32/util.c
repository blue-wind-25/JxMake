/*
 * Please refer to the notes at the top of the 'main.cpp' file for more information.
 */

#include <stddef.h>

#include "soc/dport_reg.h"
#include "soc/gpio_reg.h"
#include "soc/io_mux_reg.h"

#include "util.h"


// Index lookup table for IO_MUX_GPIOx_REG
static const uint8_t IOMuxMap[40] = {
    17, 34, 16, 33, 18, 27, 24, 25, 26, 21, // 00-09
    22, 23, 13, 14, 12, 15, 19, 20, 28, 29, // 10-19
    30, 31, 32, 35, 36,  9,  10, 11, 0,  0, // 20-29
     0,  0,  7,  8,  5,  6,  1,  2,  3,  4  // 30-39
};

static inline uint32_t __attribute__(( always_inline )) __GPIO_PIN_REG_n(uint32_t pin)
{ return DR_REG_IO_MUX_BASE + IOMuxMap[pin] * 4U; }


////////////////////////////////////////////////////////////////////////////////////////////////////


void gpio_output(uint32_t pin)
{
    const uint32_t GPIO_PIN_REG = __GPIO_PIN_REG_n(pin);

    // Function select
    PIN_FUNC_SELECT(GPIO_PIN_REG, PIN_FUNC_GPIO);

    // Disable pull-up and pull-down
    REG_CLR_BIT(GPIO_PIN_REG, FUN_PU);
    REG_CLR_BIT(GPIO_PIN_REG, FUN_PD);

    // Set direction to output
    if(pin >= 32) {
        pin -= 32;
        REG_WRITE( GPIO_ENABLE1_W1TC_REG, _BV(pin) );
        REG_WRITE( GPIO_ENABLE1_W1TS_REG, _BV(pin) );
    }
    else {
        REG_WRITE( GPIO_ENABLE_W1TC_REG , _BV(pin) );
        REG_WRITE( GPIO_ENABLE_W1TS_REG , _BV(pin) );
    }
}


void gpio_input(uint32_t pin)
{
    const uint32_t GPIO_PIN_REG = __GPIO_PIN_REG_n(pin);

    // Function select
    PIN_FUNC_SELECT(GPIO_PIN_REG, PIN_FUNC_GPIO);

    // Disable pull-up and pull-down
    REG_CLR_BIT(GPIO_PIN_REG, FUN_PU);
    REG_CLR_BIT(GPIO_PIN_REG, FUN_PD);

    // Set direction to input
    if(pin >= 32) {
        pin -= 32;
        REG_WRITE( GPIO_ENABLE1_W1TC_REG, _BV(pin) );
    }
    else {
        REG_WRITE( GPIO_ENABLE_W1TC_REG , _BV(pin) );
    }
    PIN_INPUT_ENABLE(GPIO_PIN_REG);
}

void gpio_write(uint32_t pin, uint32_t val)
{
    if(pin >= 32) {
        pin -= 32;
        if(val) REG_WRITE( GPIO_OUT1_W1TS_REG, _BV(pin) );
        else    REG_WRITE( GPIO_OUT1_W1TC_REG, _BV(pin) );
    }
    else {
        if(val) REG_WRITE( GPIO_OUT_W1TS_REG , _BV(pin) );
        else    REG_WRITE( GPIO_OUT_W1TC_REG , _BV(pin) );
    }
}


uint32_t gpio_read(uint32_t pin)
{
    if(pin >= 32) {
        pin -= 32;
        return REG_GET_BIT( GPIO_IN1_REG, _BV(pin) ) ? 1 : 0;
    }
    else {
        return REG_GET_BIT( GPIO_IN_REG , _BV(pin) ) ? 1 : 0;
    }
}


////////////////////////////////////////////////////////////////////////////////////////////////////


void uart_tx(const char* str)
{
    while(*str) uart_tx_one_char(*str++);
    uart_tx_flush(0);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


void start_app_cpu(APP_CPU_MAIN fnAppCPUMain)
{
    // Check if the CPU is already running
    if( DPORT_REG_GET_BIT(DPORT_APPCPU_CTRL_B_REG, DPORT_APPCPU_CLKGATE_EN) ) return;

    // Set the pointer to the main function
    f_cpu1_main = fnAppCPUMain;

    // Turn the CPU clock on
    DPORT_SET_PERI_REG_MASK(DPORT_APPCPU_CTRL_B_REG, DPORT_APPCPU_CLKGATE_EN);

    // Wait for a while
    delay_ms(500);
}
