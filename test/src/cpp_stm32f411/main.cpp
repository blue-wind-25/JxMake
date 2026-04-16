#include "main.h"

#include "usbd_if.h"
#include "usbd_desc.h"


/*
 * The source code files in this directory and its subdirectories are written based on:
 *     STM32Cube_FW_F4_V1.27.1/Drivers/CMSIS/Device/ST/STM32F4xx/Source/Templates
 *     STM32Cube_FW_F4_V1.27.1/Projects/STM32F411E-Discovery/Templates
 *     STM32Cube_FW_F4_V1.27.1/Projects/STM32F412ZG-Nucleo/Applications/USB_Device/HID_Standalone
 *         Copyright (C) 2017 STMicroelectronics
 *         All rights reserved
 *         BSD-3-Clause license
 *         https://opensource.org/licenses/BSD-3-Clause
 *
 *     Getting Started with STM32 BlackPill and STM32CubeIDE and USB CDC Serial Picture of Dave Bennett
 *         https://www.bennettnotes.com/notes/stm32-blackpill-with-stmcubeide-usb-serial
 *         https://github.com/DaveBben/STM32F411_Blackpill_Serial_USB_CDC_Example
 *
 *     Arduino core support for STM32 based boards
 *         https://github.com/stm32duino/Arduino_Core_STM32
 *         https://github.com/stm32duino/Arduino_Core_STM32/blob/main/variants/STM32F4xx/F411C(C-E)(U-Y)/ldscript.ld
 *
 * See also:
 *     https://github.com/STM32-base/STM32-base
 *     https://github.com/STM32-base/STM32-base-STM32Cube
 *
 *     https://stm32-base.org/boards/STM32F411CEU6-WeAct-Black-Pill-V2.0.html
 *
 * NOTE : If your STM32 'BlackPill' cannot enter DFU mode reliably, try adding a 10k Ohm pull-down on PA10 (USB_FS_ID).
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
        // https://github.com/stm32duino/Arduino_Core_STM32/blob/main/variants/STM32F4xx/F411C(C-E)(U-Y)/variant_BLACKPILL_F411CE.cpp

        // Enable Power Control clock
        __HAL_RCC_PWR_CLK_ENABLE();

        // The voltage scaling allows optimizing the power consumption when the device is
        // clocked below the maximum system frequency
        __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

        // Enable HSE Oscillator and activate PLL with HSE as source
        RCC_OscInitTypeDef RCC_OscInitStruct = {};

        RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
        RCC_OscInitStruct.HSEState       = RCC_HSE_ON;
        RCC_OscInitStruct.PLL.PLLState   = RCC_PLL_ON;
        RCC_OscInitStruct.PLL.PLLSource  = RCC_PLLSOURCE_HSE;
        RCC_OscInitStruct.PLL.PLLM       = 25;
        RCC_OscInitStruct.PLL.PLLN       = 192;
        RCC_OscInitStruct.PLL.PLLP       = RCC_PLLP_DIV2;
        RCC_OscInitStruct.PLL.PLLQ       = 4;

        if( HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK ) Error_Handler();

        // Select PLL as system clock source and configure the HCLK, PCLK1, and PCLK2 clocks dividers
        RCC_ClkInitTypeDef RCC_ClkInitStruct = {};

        RCC_ClkInitStruct.ClockType      = (RCC_CLOCKTYPE_SYSCLK | RCC_CLOCKTYPE_HCLK | RCC_CLOCKTYPE_PCLK1 | RCC_CLOCKTYPE_PCLK2);
        RCC_ClkInitStruct.SYSCLKSource   = RCC_SYSCLKSOURCE_PLLCLK;
        RCC_ClkInitStruct.AHBCLKDivider  = RCC_SYSCLK_DIV1;
        RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
        RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

        if( HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_3) != HAL_OK ) Error_Handler();

        // Prefetch is supported on STM32F405x/407x/415x/417x Revision Z devices
        if(HAL_GetREVID() == 0x1001) {
            // Enable the Flash prefetch
            __HAL_FLASH_PREFETCH_BUFFER_ENABLE();
        }
    }

} // extern "C"


extern void premain() __attribute__(( constructor(101) ));
extern void premain()
{
    // https://github.com/stm32duino/Arduino_Core_STM32/tree/main/cores/arduino/stm32
    // https://github.com/stm32duino/Arduino_Core_STM32/tree/main/libraries/SrcWrapper/src/stm32

    HAL_NVIC_SetPriorityGrouping(NVIC_PRIORITYGROUP_4);

    __HAL_RCC_PWR_CLK_ENABLE();
    __HAL_RCC_SYSCFG_CLK_ENABLE();
    __HAL_RCC_CRC_CLK_ENABLE();

    // Enable use of DWT
    if( !(CoreDebug->DEMCR & CoreDebug_DEMCR_TRCENA_Msk) ) CoreDebug->DEMCR |= CoreDebug_DEMCR_TRCENA_Msk;

    DWT->CYCCNT  = 0;                      // Reset  the clock cycle counter value
    DWT->CTRL   |= DWT_CTRL_CYCCNTENA_Msk; // Enable the clock cycle counter

   __asm volatile("nop\n\tnop\n\tnop\n\t");

    // Initialize HAL
    HAL_Init();

    // Initialize system clock
    SystemClock_Config();

    // Initialize CRC
    CRC_HandleTypeDef hcrc = {
                          .Instance = CRC,
                          .Lock     = HAL_UNLOCKED,
                          .State    = HAL_CRC_STATE_RESET
                      };
    HAL_CRC_Init(&hcrc);

    // Enable all GPIOs
    __HAL_RCC_GPIOA_CLK_ENABLE();
    __HAL_RCC_GPIOB_CLK_ENABLE();
    __HAL_RCC_GPIOC_CLK_ENABLE();

    // ### !!! TODO : Test other USB modes? !!! ###

#ifdef STM32_ENABLE_USB_DEVICE_CDC
    /*
     * Initialize as USB device CDC; on Linux it will result in something like:
     *     dmesg
     *         ...
     *         usb 3-9: Product: STM32 Virtual ComPort
     *         usb 3-9: Manufacturer: STMicroelectronics
     *         usb 3-9: SerialNumber: 3595388A3137
     *         cdc_acm 3-9:1.0: ttyACM0: USB ACM device
     *         ...
     *     lsusb
     *         ...
     *         Bus 003 Device 084: ID 0483:5740 STMicroelectronics STM32F407
     *         ...
     *     ls /dev/ttyACM*
     *         ...
     *         /dev/ttyACM0
     *         ...
     * When opened (e.g. using 'miniterm.py'), the serial port will act as a loopback device.
     */
    if( USBD_Init                 (&hUsbDeviceFS, &FS_Desc, 0            ) != USBD_OK ) Error_Handler();
    if( USBD_RegisterClass        (&hUsbDeviceFS, &USBD_CDC              ) != USBD_OK ) Error_Handler();
    if( USBD_CDC_RegisterInterface(&hUsbDeviceFS, &USBD_Interface_fops_FS) != USBD_OK ) Error_Handler();
    if( USBD_Start                (&hUsbDeviceFS                         ) != USBD_OK ) Error_Handler();
#endif

#ifdef STM32_ENABLE_USB_DEVICE_HID
    /*
     * Initialize as USB device HID; on Linux it will result in something like:
     *     dmesg
     *         ...
     *         usb 3-9.3: Product: HID Joystick in FS Mode
     *         usb 3-9.3: Manufacturer: STMicroelectronics
     *         usb 3-9.3: SerialNumber: 3595388A3137
     *         input: STMicroelectronics HID Joystick in FS Mode as /devices/pci0000:00/0000:00:14.0/usb3/3-9/3-9.3/3-9.3:1.0/input/input20
     *         hid-generic 0003:0483:5710.000B: input,hidraw3: USB HID v1.11 Mouse [STMicroelectronics HID Joystick in FS Mode] on usb-0000:00:14.0-9.3/input0
     *         ...
     *     lsusb
     *         ...
     *         Bus 003 Device 080: ID 0483:5710 STMicroelectronics Joystick in FS Mode
     *         ...
     *     ls /dev/hidraw*
     *         ...
     *         /dev/hidraw3
     *         ...
     * Press the on-board user button (KEY) to make your PC mouse cursor move.
     */
    if( USBD_Init          (&hUsbDeviceFS, &HID_Desc, 0) != USBD_OK ) Error_Handler();
    if( USBD_RegisterClass (&hUsbDeviceFS, &USBD_HID   ) != USBD_OK ) Error_Handler();
    if( USBD_Start         (&hUsbDeviceFS              ) != USBD_OK ) Error_Handler();
#endif
}


////////////////////////////////////////////////////////////////////////////////////////////////////

static const IRQn_Type __NoIRQ = static_cast<IRQn_Type>(-255);


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


static void gpioInitInput(GPIO_TypeDef* GPIOx, const uint16_t gpioPin, const bool pullUp, const IRQn_Type nExtI)
{
    GPIO_InitTypeDef gpioInitStruct = {};

    // Configure the pin as input for external interrupt
    if(nExtI != __NoIRQ) {
        gpioInitStruct.Pin   = gpioPin;
        gpioInitStruct.Mode  = GPIO_MODE_IT_FALLING;
        gpioInitStruct.Pull  = GPIO_PULLUP;
        gpioInitStruct.Speed = GPIO_SPEED_FAST;

        HAL_GPIO_Init(GPIOx, &gpioInitStruct);

        // Enable and set it to the lowest priority
        HAL_NVIC_SetPriority(nExtI, 0x0F, 0x00);
        HAL_NVIC_EnableIRQ(nExtI);
    }

    // Configure the pin as input
    else {
        gpioInitStruct.Pin   = gpioPin;
        gpioInitStruct.Mode  = GPIO_MODE_INPUT;
        gpioInitStruct.Pull  = pullUp ? GPIO_PULLUP : GPIO_NOPULL;
        gpioInitStruct.Speed = GPIO_SPEED_FREQ_LOW;

        HAL_GPIO_Init(GPIOx, &gpioInitStruct);
    }
}


// Define the pins
#define LED_1_GPIO GPIOC       // Built-in
#define LED_1_PIN  GPIO_PIN_13

#define LED_2_GPIO GPIOB       // External
#define LED_2_PIN  GPIO_PIN_7

#define KEY_GPIO   GPIOA       // Built-in
#define KEY_PIN    GPIO_PIN_0

#define SW_GPIO    GPIOB       // External
#define SW_PIN     GPIO_PIN_9


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
    gpioInitOutput(LED_1_GPIO, LED_1_PIN, true             ); // Active low
    gpioInitOutput(LED_2_GPIO, LED_2_PIN, false            );
#ifdef STM32_ENABLE_USB_DEVICE_HID
    gpioInitInput (KEY_GPIO  , KEY_PIN  , true , EXTI0_IRQn);
#endif
    gpioInitInput (SW_GPIO   , SW_PIN   , true , __NoIRQ   );

    // Infinite loop
    fast = !fast;

    while(1) {

        HAL_GPIO_WritePin(LED_1_GPIO, LED_1_PIN, GPIO_PIN_SET  ); // Active low
        HAL_GPIO_WritePin(LED_2_GPIO, LED_2_PIN, GPIO_PIN_SET  );
        delayAndCheckSwitch();

        HAL_GPIO_WritePin(LED_1_GPIO, LED_1_PIN, GPIO_PIN_RESET); // Active low
        HAL_GPIO_WritePin(LED_2_GPIO, LED_2_PIN, GPIO_PIN_RESET);
        delayAndCheckSwitch();

    } // while

    // The program should never get here
    return 0;
}
