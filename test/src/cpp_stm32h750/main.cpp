#include "main.h"


/*
 * The source code files in this directory and its subdirectories are written based on:
 *     STM32Cube_FW_H7_V1.11.0/Projects/STM32H750B-DK/Examples/GPIO/GPIO_IOToggle
 *         Copyright (C) 2017 STMicroelectronics
 *         All rights reserved
 *         BSD-3-Clause license
 *         https://opensource.org/licenses/BSD-3-Clause
 *
 *     Arduino core support for STM32 based boards
 *         https://github.com/stm32duino/Arduino_Core_STM32
 *         https://github.com/stm32duino/Arduino_Core_STM32/blob/main/variants/STM32H7xx/H742V(G-I)(H-T)_H743V(G-I)(H-T)_H750VBT_H753VI(H-T)/ldscript.ld
 *
 * NOTE : You may need to press the reset button manually when uploading via SWD.
 */


extern "C" {

    // Reports the name of the source file and the source line number where the assert_param error has occurred
    extern void assert_failed(uint8_t* file, uint32_t line)
    {
        (void) file;
        (void) line;

        // Go to infinite loop
        while(1);
    }

    extern void Error_Handler()
    {
        // Go to infinite loop
        __disable_irq();
        while(1);
    }

    extern void SystemClock_Config()
    {
        // https://github.com/stm32duino/Arduino_Core_STM32/blob/main/variants/STM32H7xx/H742V(G-I)(H-T)_H743V(G-I)(H-T)_H750VBT_H753VI(H-T)/variant_WeActMiniH7xx.cpp

        // Supply configuration update enable
        HAL_PWREx_ConfigSupply(PWR_LDO_SUPPLY);

        // Configure the main internal regulator output voltage
        __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

        while( !__HAL_PWR_GET_FLAG(PWR_FLAG_VOSRDY) );

        // Enable HSE Oscillator and activate PLL with HSE as source
        RCC_OscInitTypeDef RCC_OscInitStruct = {};

        RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
        RCC_OscInitStruct.HSEState       = RCC_HSE_ON;
        RCC_OscInitStruct.PLL.PLLState   = RCC_PLL_ON;
        RCC_OscInitStruct.PLL.PLLSource  = RCC_PLLSOURCE_HSE;
        RCC_OscInitStruct.PLL.PLLM       = 5;
        RCC_OscInitStruct.PLL.PLLN       = 96;
        RCC_OscInitStruct.PLL.PLLP       = 1;
        RCC_OscInitStruct.PLL.PLLQ       = 10;
        RCC_OscInitStruct.PLL.PLLR       = 10;
        RCC_OscInitStruct.PLL.PLLRGE     = RCC_PLL1VCIRANGE_2;
        RCC_OscInitStruct.PLL.PLLVCOSEL  = RCC_PLL1VCOWIDE;
        RCC_OscInitStruct.PLL.PLLFRACN   = 0;

        if( HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK ) Error_Handler();

        // Select PLL as system clock source and configure the HCLK, PCLK1, PCLK2, PCLK3, and PCLK4 clocks dividers
        RCC_ClkInitTypeDef RCC_ClkInitStruct = {};

        RCC_ClkInitStruct.ClockType      = (RCC_CLOCKTYPE_HCLK | RCC_CLOCKTYPE_SYSCLK | RCC_CLOCKTYPE_PCLK1 | RCC_CLOCKTYPE_PCLK2 | RCC_CLOCKTYPE_D3PCLK1 | RCC_CLOCKTYPE_D1PCLK1);
        RCC_ClkInitStruct.SYSCLKSource   = RCC_SYSCLKSOURCE_PLLCLK;
        RCC_ClkInitStruct.SYSCLKDivider  = RCC_SYSCLK_DIV1;
        RCC_ClkInitStruct.AHBCLKDivider  = RCC_HCLK_DIV2;
        RCC_ClkInitStruct.APB3CLKDivider = RCC_APB3_DIV2;
        RCC_ClkInitStruct.APB1CLKDivider = RCC_APB1_DIV2;
        RCC_ClkInitStruct.APB2CLKDivider = RCC_APB2_DIV2;
        RCC_ClkInitStruct.APB4CLKDivider = RCC_APB4_DIV2;

        if( HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_4) != HAL_OK ) Error_Handler();
    }

} // extern "C"


extern void premain() __attribute__(( constructor(101) ));
extern void premain()
{
    // Enable the CPU Cache
    SCB_EnableICache();
    SCB_EnableDCache();

    // Initialize HAL
    HAL_Init();

    // Initialize system clock
    SystemClock_Config();

    // Enable all GPIOs
    __HAL_RCC_GPIOA_CLK_ENABLE();
    __HAL_RCC_GPIOB_CLK_ENABLE();
    __HAL_RCC_GPIOC_CLK_ENABLE();
    __HAL_RCC_GPIOD_CLK_ENABLE();
    __HAL_RCC_GPIOE_CLK_ENABLE();
}


////////////////////////////////////////////////////////////////////////////////////////////////////


static void gpioInitOutput(GPIO_TypeDef* GPIOx, uint16_t gpioPin, const bool initialState)
{
    // https://embeddedexplorer.com/stm32-gpio-tutorial

    HAL_GPIO_WritePin(GPIOx, gpioPin, initialState ? GPIO_PIN_SET : GPIO_PIN_RESET);

    GPIO_InitTypeDef gpioInitStruct = {};

    // Configure the pin as output
    gpioInitStruct.Pin       = gpioPin;
    gpioInitStruct.Mode      = GPIO_MODE_OUTPUT_PP;
    gpioInitStruct.Pull      = GPIO_NOPULL;
    gpioInitStruct.Speed     = GPIO_SPEED_FREQ_LOW;
    gpioInitStruct.Alternate = 0;

    HAL_GPIO_Init(GPIOx, &gpioInitStruct);
}


static void gpioInitInput(GPIO_TypeDef* GPIOx, const uint16_t gpioPin, const bool pullUp)
{
    GPIO_InitTypeDef gpioInitStruct = {};

    // Configure the pin as input
    gpioInitStruct.Pin   = gpioPin;
    gpioInitStruct.Mode  = GPIO_MODE_INPUT;
    gpioInitStruct.Pull  = pullUp ? GPIO_PULLUP : GPIO_NOPULL;
    gpioInitStruct.Speed = GPIO_SPEED_FREQ_LOW;

    HAL_GPIO_Init(GPIOx, &gpioInitStruct);
}


// Define the pins
#define LED_1_GPIO GPIOA       // Built-in
#define LED_1_PIN  GPIO_PIN_1

#define LED_2_GPIO GPIOB       // External
#define LED_2_PIN  GPIO_PIN_3

#define SW_GPIO    GPIOC       // Built-in
#define SW_PIN     GPIO_PIN_5


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        HAL_Delay(10);
        // Check if the switch is pressed
        if( !HAL_GPIO_ReadPin(SW_GPIO, SW_PIN) ) {
            // Debouncing
            HAL_Delay(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !HAL_GPIO_ReadPin(SW_GPIO, SW_PIN) );
}


int main()
{
    // Initialize GPIO
    gpioInitOutput(LED_1_GPIO, LED_1_PIN, true); // Active low
    gpioInitOutput(LED_2_GPIO, LED_2_PIN, true); // Active low
    gpioInitInput (SW_GPIO   , SW_PIN   , true);

    // Infinite loop
    fast = !fast;

    while(1) {

        HAL_GPIO_WritePin(LED_1_GPIO, LED_1_PIN, GPIO_PIN_SET  ); // Active low
        HAL_GPIO_WritePin(LED_2_GPIO, LED_2_PIN, GPIO_PIN_RESET); // Active low
        delayAndCheckSwitch();

        HAL_GPIO_WritePin(LED_1_GPIO, LED_1_PIN, GPIO_PIN_RESET); // Active low
        HAL_GPIO_WritePin(LED_2_GPIO, LED_2_PIN, GPIO_PIN_SET  ); // Active low
        delayAndCheckSwitch();

    } // while

    // The program should never get here
    return 0;
}
