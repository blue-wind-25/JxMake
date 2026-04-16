/*
 * This firmware is written for:
 *     Waveshare BLE400 with Core51822
 *     https://www.waveshare.com/wiki/BLE400
 *     https://www.waveshare.com/wiki/Core51822
 *
 * This example utilizes the JxMake nRF51 bare-metal library (C++XBuildTool_NRF51BM) to build the
 * firmware using the standalone set of drivers for peripherals present in Nordic Semiconductor's
 * SoCs and SiPs that can be downloaded from:
 *     https://github.com/NordicSemiconductor/nrfx
 *     https://github.com/NordicSemiconductor/nrfx/tree/233c96307e0946fc44035424c410bd44ddf2d75e
 * as well as CMSIS Version 5 that can be downloaded from:
 *     https://github.com/ARM-software/CMSIS_5
 *     https://github.com/ARM-software/CMSIS_5/tree/0f1587564506b385d57a58baed8c2c6a1e2b959d
 *
 * Please refer to the 'README.txt' file for more information.
 */

#include <nrf_gpio.h>
#include <nrfx_clock.h>
#include <nrfx_rtc.h>


// Define the pins
#define USER_LED1 18
#define USER_LED2 20

#define USER_SW    6


static          nrfx_rtc_t        rtc1Inst = NRFX_RTC_INSTANCE(1);
static          nrfx_rtc_config_t rtc1Conf = NRFX_RTC_DEFAULT_CONFIG;
static volatile uint32_t          rtc1OFls = 0;

static void irqHandler_clock(nrfx_clock_evt_type_t event)
{}

static void irqHandler_rtc1(nrfx_rtc_int_type_t int_type)
{ if(int_type == NRFX_RTC_INT_OVERFLOW) rtc1OFls = (rtc1OFls + 1U) & 0xFFU; }

static inline uint64_t ticks()
{ return static_cast<uint64_t>( static_cast<uint64_t>(rtc1OFls) << 24U ) | static_cast<uint64_t>( nrfx_rtc_counter_get(&rtc1Inst) ); }

static inline uint32_t millis()
{ return ( ticks() * 1000U ) / 32768U; }

static inline void delay_ms(uint32_t ms)
{
    if(!ms) return;

    const uint32_t start = millis();
    while( millis() - start < ms );
}


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay_ms(10);
        // Check if the switch is pressed
        if( !nrf_gpio_pin_read(USER_SW) ) {
            // Debouncing
            delay_ms(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !nrf_gpio_pin_read(USER_SW) );
}


int main()
{
    // ##### !!! TODO : Test UART? !!! #####

    // Initialize clock
    nrfx_clock_init(&irqHandler_clock);
    nrfx_clock_enable();
    nrfx_clock_lfclk_start();

    // Initialize RTC1
    nrfx_rtc_init           (&rtc1Inst, &rtc1Conf, &irqHandler_rtc1);
    nrfx_rtc_tick_enable    (&rtc1Inst, false                      );
    nrfx_rtc_overflow_enable(&rtc1Inst, true                       );
    nrfx_rtc_enable         (&rtc1Inst                             );

    // Set output direction for the LED pins
    nrf_gpio_cfg_output(USER_LED1); nrf_gpio_pin_clear(USER_LED1);
    nrf_gpio_cfg_output(USER_LED2); nrf_gpio_pin_clear(USER_LED2);

    // Set input direction for the switch pin (without pull-up)
    nrf_gpio_cfg_input(USER_SW, NRF_GPIO_PIN_NOPULL);

    // Loop forever
    fast = !fast;

    for(;;) {

        nrf_gpio_pin_clear(USER_LED1);
        nrf_gpio_pin_set  (USER_LED2);
        delayAndCheckSwitch();

        nrf_gpio_pin_set  (USER_LED1);
        nrf_gpio_pin_clear(USER_LED2);
        delayAndCheckSwitch();

    } // for

    return 0;
}
