#include "main.h"

#include "usbd_if.h"
#include "usbd_desc.h"


/*
 * The source code files in this directory and its subdirectories are written based on:
 *     STM32Cube_FW_F1_V1.8.4/Drivers/CMSIS/Device/ST/STM32F1xx/Source/Templates
 *     STM32Cube_FW_F1_V1.8.4/Projects/STM32VL-Discovery/Templates
 *         Copyright (C) 2017 STMicroelectronics
 *         All rights reserved
 *         BSD-3-Clause license
 *         https://opensource.org/licenses/BSD-3-Clause
 *
 * BluePill USB-CDC (Virtual COM Port) Test
 *     https://github.com/philrawlings/bluepill-usb-cdc-test
 *
 * See also:
 *     https://github.com/STM32-base/STM32-base
 *     https://github.com/STM32-base/STM32-base-STM32Cube
 *
 *     https://stm32-base.org/boards/STM32F103C8T6-Blue-Pill
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
        // https://github.com/stm32duino/Arduino_Core_STM32/blob/main/variants/STM32F1xx/F103C8T_F103CB(T-U)/variant_PILL_F103Cx.cpp

        // Enable HSE Oscillator and activate PLL with HSE as source
        RCC_OscInitTypeDef RCC_OscInitStruct = {};

        RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
        RCC_OscInitStruct.HSEState       = RCC_HSE_ON;
        RCC_OscInitStruct.HSEPredivValue = RCC_HSE_PREDIV_DIV1;
        RCC_OscInitStruct.PLL.PLLState   = RCC_PLL_ON;
        RCC_OscInitStruct.PLL.PLLSource  = RCC_PLLSOURCE_HSE;
        RCC_OscInitStruct.PLL.PLLMUL     = RCC_PLL_MUL9;

        if( HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK ) Error_Handler();

        // Select PLL as system clock source and configure the HCLK, PCLK1, and PCLK2 clocks dividers
        RCC_ClkInitTypeDef RCC_ClkInitStruct = {};

        RCC_ClkInitStruct.ClockType      = (RCC_CLOCKTYPE_SYSCLK | RCC_CLOCKTYPE_HCLK | RCC_CLOCKTYPE_PCLK1 | RCC_CLOCKTYPE_PCLK2);
        RCC_ClkInitStruct.SYSCLKSource   = RCC_SYSCLKSOURCE_PLLCLK;
        RCC_ClkInitStruct.AHBCLKDivider  = RCC_SYSCLK_DIV1;
        RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
        RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

        if( HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_2) != HAL_OK ) Error_Handler();

        // USB clock selection
        RCC_PeriphCLKInitTypeDef rccperiphclkinit = {};

        rccperiphclkinit.PeriphClockSelection = RCC_PERIPHCLK_USB;
        rccperiphclkinit.UsbClockSelection    = RCC_USBCLKSOURCE_PLL_DIV1_5;

        if( HAL_RCCEx_PeriphCLKConfig(&rccperiphclkinit) != HAL_OK ) Error_Handler();
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
     *         usb 3-9: Product: Test Product
     *         usb 3-9: Manufacturer: STMicroelectronics
     *         usb 3-9: SerialNumber: 4E6A417B4600
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

    // Force host to re-enumerate the device; the 'BluePill' board  has a pull up resistor on the USB_DP (PA12) line,
    // so we set it low initially
    GPIO_InitTypeDef GPIO_InitStruct = {};
    GPIO_InitStruct.Pin   = GPIO_PIN_12;
    GPIO_InitStruct.Mode  = GPIO_MODE_OUTPUT_PP;
    GPIO_InitStruct.Pull  = GPIO_PULLDOWN;
    GPIO_InitStruct.Speed = GPIO_SPEED_HIGH;
    HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);
    HAL_GPIO_WritePin(GPIOA, GPIO_PIN_12, GPIO_PIN_RESET);
    HAL_Delay(100);

    if( USBD_Init                 (&hUsbDeviceFS, &FS_Desc, 0            ) != USBD_OK ) Error_Handler();
    if( USBD_RegisterClass        (&hUsbDeviceFS, &USBD_CDC              ) != USBD_OK ) Error_Handler();
    if( USBD_CDC_RegisterInterface(&hUsbDeviceFS, &USBD_Interface_fops_FS) != USBD_OK ) Error_Handler();
    if( USBD_Start                (&hUsbDeviceFS                         ) != USBD_OK ) Error_Handler();
#endif
}


////////////////////////////////////////////////////////////////////////////////////////////////////


static void gpioInitOutput(GPIO_TypeDef* GPIOx, uint16_t gpioPin, const bool initialState)
{
    // https://embeddedexplorer.com/stm32-gpio-tutorial

    HAL_GPIO_WritePin(GPIOx, gpioPin, initialState ? GPIO_PIN_SET : GPIO_PIN_RESET);

    GPIO_InitTypeDef gpioInitStruct = {};

    // Configure the pin as output
    gpioInitStruct.Pin   = gpioPin;
    gpioInitStruct.Mode  = GPIO_MODE_OUTPUT_PP;
    gpioInitStruct.Pull  = GPIO_NOPULL;
    gpioInitStruct.Speed = GPIO_SPEED_FREQ_LOW;

    HAL_GPIO_Init(GPIOx, &gpioInitStruct);
}


static void gpioInitInput(GPIO_TypeDef* GPIOx, const uint16_t gpioPin, const bool pullUp)
{
    GPIO_InitTypeDef gpioInitStruct = {};

    gpioInitStruct.Pin   = gpioPin;
    gpioInitStruct.Mode  = GPIO_MODE_INPUT;
    gpioInitStruct.Pull  = pullUp ? GPIO_PULLUP : GPIO_NOPULL;
    gpioInitStruct.Speed = GPIO_SPEED_FREQ_LOW;

    HAL_GPIO_Init(GPIOx, &gpioInitStruct);
}


// Define the pins
#define LED_1_GPIO GPIOC       // Built-in
#define LED_1_PIN  GPIO_PIN_13

#define LED_2_GPIO GPIOB       // External
#define LED_2_PIN  GPIO_PIN_7

#define SW_GPIO    GPIOB       // External
#define SW_PIN     GPIO_PIN_9


static bool fast = true;

static void checkSwitch()
{
    // Return if the switch is not pressed
    if( HAL_GPIO_ReadPin(SW_GPIO, SW_PIN) ) return;

    // Debouncing
    HAL_Delay(100);

    // Change the speed flag and break
    fast = !fast;

    // Wait until the switch is no longer pressed
    while( !HAL_GPIO_ReadPin(SW_GPIO, SW_PIN) );

    // Debouncing
    HAL_Delay(100);
}

static void delayAndHandleUSBCDC()
{
#ifdef STM32_ENABLE_USB_DEVICE_CDC
    static uint8_t rxData[8];
#endif

    for(char i = 0; i < (fast ? 10 : 50); ++i) {

        const unsigned int last_mS = HAL_GetTick();

        while( ( HAL_GetTick() - last_mS ) < 10 ) {

            checkSwitch();

#ifdef STM32_ENABLE_USB_DEVICE_CDC
            // Handle USB CDC
            const uint16_t bytesAvailable = CDC_GetRxBufferBytesAvailable_FS();

            if(bytesAvailable <= 0) continue;

            const uint16_t bytesToRead = bytesAvailable >= 8 ? 8 : bytesAvailable;
            if( CDC_ReadRxBuffer_FS(rxData, bytesToRead) == USB_CDC_RX_BUFFER_OK ) {
                while( CDC_Transmit_FS(rxData, bytesToRead) == USBD_BUSY ) checkSwitch();
            }
#endif

        } // while

    } // for
}


int main()
{

#if 0

    __asm__ __volatile__ ("nop \n\t nop \n\t nop \n\t");
    __asm__ __volatile__ ("nop \n\t nop \n\t nop \n\t");

    *( (uint32_t*) 0x08000000 ) = (uint32_t) 0x00;
    *( (uint32_t*) 0x08000000 ) = (uint32_t) 0x12345678;

    __asm__ __volatile__ ("nop \n\t nop \n\t nop \n\t");

    *( (uint32_t*) 0x20000000 ) = (uint32_t) 0x00;
    *( (uint32_t*) 0x20000000 ) = (uint32_t) 0x12345678;

    __asm__ __volatile__ ("nop \n\t nop \n\t nop \n\t");

    __asm__ __volatile__ (
        "mov.w r0, #536870912 \n\t" // r0 = 0x20000000
        "mov.w r1, #134217728 \n\t" // r1 = 0x08000000
        "ldr   r2, [r0, #5]   \n\t" // r2 = SRAM[r0 + 4 * 4]
        "str   r2, [r1, #0]   \n\t"
        "nop                  \n\t"
        "nop                  \n\t"
        "nop                  \n\t"
        ".long 0x12345678     \n\t"
    );

    __asm__ __volatile__ ("nop \n\t nop \n\t nop \n\t");

    __asm__ __volatile__ (
        "mov.w r0, #536870912 \n\t" // r0 = 0x20000000
        "mov.w r1, #536870912 \n\t" // r1 = 0x08000000
        "ldr   r2, [r0, #5]   \n\t" // r2 = SRAM[r0 + 4 * 4]
        "str   r2, [r1, #8]   \n\t"
        "nop                  \n\t"
        "nop                  \n\t"
        "nop                  \n\t"
        ".long 0x12345678     \n\t"
    );

    __asm__ __volatile__ ("nop \n\t nop \n\t nop \n\t");

    __asm__ __volatile__ (
        "     str r0, [r1, #0] \n\t"
        "rep: b   rep          \n\t"
    );

    // f04f 5000
    // f04f 6100
    // f8d0 2005
    // 600a
    // bf00
    // bf00
    // bf00
    // 12345678

    // http://www.ethernut.de/en/documents/arm-inline-asm.html

/*
 80005b0:   f04f 5000   mov.w   r0, #536870912  ; 0x20000000
 80005b4:   f04f 6100   mov.w   r1, #134217728  ; 0x8000000
 80005b8:   f8d0 2005   ldr.w   r2, [r0, #5]
 80005bc:   600a        str r2, [r1, #0]
 80005be:   bf00        nop
 80005c0:   bf00        nop
 80005c2:   bf00        nop
 80005c4:   12345678    eorsne  r5, r4, #120, 12    ; 0x7800000

 80005c8:   f04f 5000   mov.w   r0, #536870912  ; 0x20000000
 80005cc:   f04f 5100   mov.w   r1, #536870912  ; 0x20000000
 80005d0:   f8d0 2005   ldr.w   r2, [r0, #5]
 80005d4:   608a        str r2, [r1, #8]
 80005d6:   bf00        nop
 80005d8:   bf00        nop
 80005da:   bf00        nop
 80005dc:   12345678    eorsne  r5, r4, #120, 12    ; 0x7800000

080005ea <rep>:
 80005ea:   e7fe        b.n 80005ea <rep>

*/
    __asm__ __volatile__ ("nop \n\t nop \n\t nop \n\t");
    __asm__ __volatile__ ("nop \n\t nop \n\t nop \n\t");

    // make s32f1test && ~/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/arm-none-eabi/bin/objdump -D ../test/src/cpp_stm32f103/build/main.elf > ../test/src/cpp_stm32f103/build/main.lst

#endif

    // Initialize GPIO
    gpioInitOutput(LED_1_GPIO, LED_1_PIN, true); // Active low
    gpioInitOutput(LED_2_GPIO, LED_2_PIN, true);
    gpioInitInput (SW_GPIO   , SW_PIN   , true);

    // Infinite loop
    fast = !fast;

    while(1) {

        HAL_GPIO_WritePin(LED_1_GPIO, LED_1_PIN, GPIO_PIN_SET  ); // Active low
        HAL_GPIO_WritePin(LED_2_GPIO, LED_2_PIN, GPIO_PIN_SET  );
        delayAndHandleUSBCDC();

        HAL_GPIO_WritePin(LED_1_GPIO, LED_1_PIN, GPIO_PIN_RESET); // Active low
        HAL_GPIO_WritePin(LED_2_GPIO, LED_2_PIN, GPIO_PIN_RESET);
        delayAndHandleUSBCDC();

    } // while

    // The program should never get here
    return 0;
}
