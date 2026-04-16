/*
 * This firmware is written for:
 *     Seeed Studio XIAO nRF52840 (Sense)
 *     https://www.seeedstudio.com/Seeed-XIAO-BLE-nRF52840-p-5201.html
 *     https://www.seeedstudio.com/Seeed-XIAO-BLE-Sense-nRF52840-p-5253.html
 *     https://wiki.seeedstudio.com/XIAO_BLE
 *
 * This example utilizes the JxMake nRF52 bare-metal library (C++XBuildTool_NRF52BM) to build the
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
#include <nrfx_systick.h>


// Define the pins (all pins are on port 0)
#define USER_LEDR 26
#define USER_LEDG 30
#define USER_LEDB  6
#define USER_LEDC 17

#define USER_SW   29


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        nrfx_systick_delay_ms(10);
        // Check if the switch is pressed
        if( !nrf_gpio_pin_read(USER_SW) ) {
            // Debouncing
            nrfx_systick_delay_ms(100);
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
    // Set the REGOUT0 register as needed to ensure that the SWD port works with 3.3V programmer/debugger
    // https://devzone.nordicsemi.com/guides/short-range-guides/b/getting-started/posts/nrf52840-dongle-programming-tutorial
    if( (NRF_UICR->REGOUT0 & UICR_REGOUT0_VOUT_Msk) == (UICR_REGOUT0_VOUT_DEFAULT << UICR_REGOUT0_VOUT_Pos) ) {
        // Enable write access
        NRF_NVMC->CONFIG = NVMC_CONFIG_WEN_Wen;
        while(NRF_NVMC->READY == NVMC_READY_READY_Busy){}
        // Set the output voltage of REG0 to 3.0V
        NRF_UICR->REGOUT0 = ( NRF_UICR->REGOUT0 & ~( (uint32_t)UICR_REGOUT0_VOUT_Msk) ) | (UICR_REGOUT0_VOUT_3V0 << UICR_REGOUT0_VOUT_Pos);
        // Disable write access
        NRF_NVMC->CONFIG = NVMC_CONFIG_WEN_Ren;
        while(NRF_NVMC->READY == NVMC_READY_READY_Busy){}
        // System reset is needed to update the UICR registers
        NVIC_SystemReset();
    }

    // ##### !!! TODO : Test UART? !!! #####

    // Initialize SysTick
    nrfx_systick_init();

    // Set output direction for the LED pins
    nrf_gpio_cfg_output(USER_LEDR); nrf_gpio_pin_set(USER_LEDR);
    nrf_gpio_cfg_output(USER_LEDG); nrf_gpio_pin_set(USER_LEDG);
    nrf_gpio_cfg_output(USER_LEDB); nrf_gpio_pin_set(USER_LEDB);
    nrf_gpio_cfg_output(USER_LEDC); nrf_gpio_pin_set(USER_LEDC);

    // Set input direction for the switch pin (without pull-up)
    nrf_gpio_cfg_input(USER_SW, NRF_GPIO_PIN_NOPULL);

    // Loop forever
    fast = !fast;

    for(;;) {

        nrf_gpio_pin_clear(USER_LEDR);
        nrf_gpio_pin_set  (USER_LEDG);
        nrf_gpio_pin_set  (USER_LEDB);
        nrf_gpio_pin_set  (USER_LEDC);
        delayAndCheckSwitch();

        nrf_gpio_pin_set  (USER_LEDR);
        nrf_gpio_pin_clear(USER_LEDG);
        nrf_gpio_pin_set  (USER_LEDB);
        nrf_gpio_pin_set  (USER_LEDC);
        delayAndCheckSwitch();

        nrf_gpio_pin_set  (USER_LEDR);
        nrf_gpio_pin_set  (USER_LEDG);
        nrf_gpio_pin_clear(USER_LEDB);
        nrf_gpio_pin_set  (USER_LEDC);
        delayAndCheckSwitch();

        nrf_gpio_pin_set  (USER_LEDR);
        nrf_gpio_pin_set  (USER_LEDG);
        nrf_gpio_pin_set  (USER_LEDB);
        nrf_gpio_pin_clear(USER_LEDC);
        delayAndCheckSwitch();

    } // for

    return 0;
}
