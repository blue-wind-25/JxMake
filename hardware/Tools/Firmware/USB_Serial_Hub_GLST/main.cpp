/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

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


#include "main.h"

#include "usbd_if.h"
#include "usbd_desc.h"


// The firmware version string
const char FIRMWARE_VERSION[] = "JxMake USB-to-Serial Converter v1.0.0";


extern "C" {

    // Reports the name of the source file and the source line number where the assert_param error has occurred
    extern void assert_failed(uint8_t* file, uint32_t line)
    {
        (void) file;
        (void) line;

        // Go to infinite loop
        __disable_irq();
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
}


////////////////////////////////////////////////////////////////////////////////////////////////////


//
// Define the pins
//
#define DP_EN1K5PU_GPIO  GPIOA
#define DP_EN1K5PU_PIN   GPIO_PIN_10

#define VBUS_DETECT_GPIO GPIOA
#define VBUS_DETECT_PIN  GPIO_PIN_9

#define LED_GPIO         GPIOC
#define LED_PIN          GPIO_PIN_13

#define UART_TXD_GPIO    GPIOB
#define UART_TXD_PIN     GPIO_PIN_10

#define UART_RXD_GPIO    GPIOB
#define UART_RXD_PIN     GPIO_PIN_11

#define UART_XCK_GPIO    GPIOB
#define UART_XCK_PIN     GPIO_PIN_12

#define UART_CTS_GPIO    GPIOB
#define UART_CTS_PIN     GPIO_PIN_13

#define UART_RTS_GPIO    GPIOB
#define UART_RTS_PIN     GPIO_PIN_14

#define UART_DTR_GPIO    GPIOB
#define UART_DTR_PIN     GPIO_PIN_15


//
// Helper functions
//
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


static GPIO_PinState ledState = GPIO_PIN_SET;

static inline void setLED(bool on)
{
    ledState = on ? GPIO_PIN_SET : GPIO_PIN_RESET;

    HAL_GPIO_WritePin(LED_GPIO, LED_PIN, ledState);
}

static void blinkLED(bool active)
{
    static uint32_t lastTick    = 0;
    const  uint32_t interval    = active ? 100 : 1000;
    const  uint32_t currentTick = HAL_GetTick();

    // Check if enough time has passed
    if( (currentTick - lastTick) >= interval ) {
        lastTick = currentTick;
        ledState = (ledState == GPIO_PIN_SET) ? GPIO_PIN_RESET : GPIO_PIN_SET;
        HAL_GPIO_WritePin(LED_GPIO, LED_PIN, ledState);
    }
}

static void blinkErrorLED()
{
    setLED(false);

    while(1) {
        for(int i = 0; i < 3; ++i) {
            setLED(true ); HAL_Delay(100);
            setLED(false); HAL_Delay(100);
        }
        HAL_Delay(200);
    }
}


#define UART_DEFAULT_BAUD 9600

typedef union {

    uint8_t      lcBuffer[7]; // Raw byte buffer

    struct {
        uint32_t dwDTERate;   // Data terminal rate, in bits per second
        uint8_t  bCharFormat; // Stop bits : 0=1, 1=1.5, 2=2
        uint8_t  bParityType; // Parity    : 0=None, 1=Odd, 2=Even, 3=Mark, 4=Space
        uint8_t  bDataBits;   // Data bits : 5, 6, 7, 8, 9, 16
    } lineCoding;

} CDC_LineCoding;

static CDC_LineCoding     cdcLineCoding;
static UART_HandleTypeDef huart3;

void HAL_UART_MspInit(UART_HandleTypeDef* huart)
{
    if(huart->Instance != USART3) return;

    GPIO_InitTypeDef GPIO_InitStruct = {};

    HAL_GPIO_WritePin(UART_TXD_GPIO, UART_TXD_PIN, GPIO_PIN_SET);
    GPIO_InitStruct.Pin   = UART_TXD_PIN;
    GPIO_InitStruct.Mode  = GPIO_MODE_AF_PP;
    GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(UART_TXD_GPIO, &GPIO_InitStruct);

    GPIO_InitStruct.Pin   = UART_RXD_PIN;
    GPIO_InitStruct.Mode  = GPIO_MODE_AF_INPUT;
    GPIO_InitStruct.Pull  = GPIO_PULLUP;
    HAL_GPIO_Init(UART_RXD_GPIO, &GPIO_InitStruct);

    HAL_GPIO_WritePin(UART_TXD_GPIO, UART_XCK_PIN, GPIO_PIN_RESET);
    GPIO_InitStruct.Pin   = UART_XCK_PIN;
    GPIO_InitStruct.Mode  = GPIO_MODE_AF_PP;
    GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(UART_XCK_GPIO, &GPIO_InitStruct);

    GPIO_InitStruct.Pin   = UART_CTS_PIN;
    GPIO_InitStruct.Mode  = GPIO_MODE_AF_INPUT;
    GPIO_InitStruct.Pull  = GPIO_PULLDOWN;
    HAL_GPIO_Init(UART_CTS_GPIO, &GPIO_InitStruct);

    HAL_GPIO_WritePin(UART_RTS_GPIO, UART_RTS_PIN, GPIO_PIN_SET);
    GPIO_InitStruct.Pin   = UART_RTS_PIN;
    GPIO_InitStruct.Mode  = GPIO_MODE_AF_PP;
    GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(UART_RTS_GPIO, &GPIO_InitStruct);

    HAL_GPIO_WritePin(UART_DTR_GPIO, UART_DTR_PIN, GPIO_PIN_SET);
    GPIO_InitStruct.Pin   = UART_DTR_PIN;
    GPIO_InitStruct.Mode  = GPIO_MODE_OUTPUT_PP;
    GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(UART_DTR_GPIO, &GPIO_InitStruct);
}

static void UART3_Init(uint32_t baudrate, bool syncMode)
{
    __HAL_RCC_USART3_CLK_ENABLE();

    // Initialize line coding structure
    cdcLineCoding.lineCoding.dwDTERate   = baudrate;
    cdcLineCoding.lineCoding.bCharFormat = UART_STOPBITS_1;
    cdcLineCoding.lineCoding.bParityType = UART_PARITY_NONE;
    cdcLineCoding.lineCoding.bDataBits   = UART_WORDLENGTH_8B;

    // Initialize UART handle
    huart3.Instance          = USART3;
    huart3.Init.BaudRate     = baudrate;
    huart3.Init.WordLength   = UART_WORDLENGTH_8B;
    huart3.Init.StopBits     = UART_STOPBITS_1;
    huart3.Init.Parity       = UART_PARITY_NONE;
    huart3.Init.Mode         = UART_MODE_TX_RX;
    huart3.Init.HwFlowCtl    = UART_HWCONTROL_RTS_CTS; // UART_HWCONTROL_NONE
    huart3.Init.OverSampling = UART_OVERSAMPLING_16;

    if(syncMode) {
        USART3->CR2 |=  USART_CR2_CLKEN; // Enable synchronous clock
        USART3->CR2 &= ~USART_CR2_CPOL;  // Idle clock low
        USART3->CR2 &= ~USART_CR2_CPHA;  // Capture on first edge
        USART3->CR2 |=  USART_CR2_LBCL;  // Pulse clock for last bit
    }

    if( HAL_UART_Init(&huart3) != HAL_OK ) Error_Handler();
}

static inline void UART3_SetBaudrate(uint32_t baudrate)
{
    huart3.Init.BaudRate = baudrate;

    if( HAL_UART_Init(&huart3) != HAL_OK ) Error_Handler();
}

static inline bool UART3_ReadByte(uint8_t* data)
{  return HAL_UART_Receive(&huart3, data, 1, 0) == HAL_OK; }

static inline bool UART3_WriteBuffer(uint8_t* buff, uint16_t len)
{ return HAL_UART_Transmit(&huart3, buff, len, HAL_MAX_DELAY) == HAL_OK; }


#define USB_PRE_INIT_DELAY_MS  1000
#define USB_REINIT_DELAY_MS     500
#define USB_REINIT_MAX_RETRIES    5

int8_t CDC_Control_FS(uint8_t cmd, uint8_t* pbuf, uint16_t length)
{
    switch(cmd) {
        case CDC_SEND_ENCAPSULATED_COMMAND : // Not used in CDC-ACM
        case CDC_GET_ENCAPSULATED_RESPONSE : // ---
        case CDC_SET_COMM_FEATURE          : // Optional modem features, usually unused
        case CDC_GET_COMM_FEATURE          : // ---
        case CDC_CLEAR_COMM_FEATURE        : // ---
            break;

        case CDC_SET_LINE_CODING: {
            if(length != 7) return USBD_FAIL;

            // Copy line coding data
            for(int i = 0; i < 7; ++i) cdcLineCoding.lcBuffer[i] = pbuf[i];

            const uint32_t baudrate  = cdcLineCoding.lineCoding.dwDTERate;
            const uint8_t  stopBits  = cdcLineCoding.lineCoding.bCharFormat;
            const uint8_t  parity    = cdcLineCoding.lineCoding.bParityType;
            const uint8_t  dataBits  = cdcLineCoding.lineCoding.bDataBits;

            // Validate and set baudrate
            if(baudrate < 1200 || baudrate > 1843200) return USBD_FAIL; // Not supported
            huart3.Init.BaudRate = baudrate;

            // Validate and set stop bits
                 if(stopBits == 0) huart3.Init.StopBits   = UART_STOPBITS_1;
            else if(stopBits == 2) huart3.Init.StopBits   = UART_STOPBITS_2;
            else                   return USBD_FAIL; // Not supported

            // Validate and set parity
                 if(parity   == 0) huart3.Init.Parity     = UART_PARITY_NONE;
            else if(parity   == 1) huart3.Init.Parity     = UART_PARITY_ODD;
            else if(parity   == 2) huart3.Init.Parity     = UART_PARITY_EVEN;
            else                   return USBD_FAIL; // Not supported

            // Validate and set data bits
                 if(dataBits == 8) huart3.Init.WordLength = UART_WORDLENGTH_8B;
            else if(dataBits == 9) huart3.Init.WordLength = UART_WORDLENGTH_9B;
            else                   return USBD_FAIL; // Not supported

            if( HAL_UART_Init(&huart3) != HAL_OK ) blinkErrorLED();
            break;
        }

        case CDC_GET_LINE_CODING: {
            if(length != 7) return USBD_FAIL;
            for(int i = 0; i < 7; ++i) pbuf[i] = cdcLineCoding.lcBuffer[i];
            CDC_FlushRxBuffer_FS();
            break;
        }

        case CDC_SET_CONTROL_LINE_STATE: {
            if(length != 0) return USBD_FAIL;
            const uint16_t controlLineState = hUsbDeviceFS.request.wValue;
            const bool     dtr              = (controlLineState & 0x01) != 0;
         // const bool     rts              = (controlLineState & 0x02) != 0;
         // HAL_GPIO_WritePin(UART_RTS_GPIO, UART_RTS_PIN, rts ? GPIO_PIN_RESET : GPIO_PIN_SET); // Not possible due to UART_HWCONTROL_RTS_CTS
            HAL_GPIO_WritePin(UART_DTR_GPIO, UART_DTR_PIN, dtr ? GPIO_PIN_RESET : GPIO_PIN_SET);
            break;
        }

        case CDC_SEND_BREAK: {
            if(length != 0) return USBD_FAIL;
            const uint16_t breakDuration = hUsbDeviceFS.request.wValue;
                 if(breakDuration == 0xFFFF) SET_BIT  (huart3.Instance->CR1, USART_CR1_SBK);
            else if(breakDuration == 0x0000) CLEAR_BIT(huart3.Instance->CR1, USART_CR1_SBK);
            else return USBD_FAIL; // Not supported
            break;
        }

        default:
            break;
    }

    return USBD_OK;
}

static bool initUSB()
{
    // The 'JxMake USB Serial Hub GLST' module has a pull up resistor on the USB_DP (PA12) pin which
    // is connected to another GPIO
    HAL_GPIO_WritePin(DP_EN1K5PU_GPIO, DP_EN1K5PU_PIN, GPIO_PIN_RESET); HAL_Delay(300);
    HAL_GPIO_WritePin(DP_EN1K5PU_GPIO, DP_EN1K5PU_PIN, GPIO_PIN_SET  ); HAL_Delay(100);

    USBD_Interface_fops_FS.Control = CDC_Control_FS;

    if( USBD_Init                 (&hUsbDeviceFS, &FS_Desc, 0            ) != USBD_OK ) return false;
    if( USBD_RegisterClass        (&hUsbDeviceFS, &USBD_CDC              ) != USBD_OK ) return false;
    if( USBD_CDC_RegisterInterface(&hUsbDeviceFS, &USBD_Interface_fops_FS) != USBD_OK ) return false;
    if( USBD_Start                (&hUsbDeviceFS                         ) != USBD_OK ) return false;

    return true;
}

static void initUSBWithRetry()
{
    // Ensure VBUS is present before attempting USB initialization
    if( !HAL_GPIO_ReadPin(VBUS_DETECT_GPIO, VBUS_DETECT_PIN) ) blinkErrorLED();

    int attempt = 0;

    while(attempt < USB_REINIT_MAX_RETRIES) {

        if( initUSB() ) return;

        ++attempt;

        // Clean up before retry
        USBD_Stop  (&hUsbDeviceFS);
        USBD_DeInit(&hUsbDeviceFS);

        HAL_Delay(USB_REINIT_DELAY_MS);

    } // while

    // All retry attempts failed
    blinkErrorLED();
}


#define CDC_ACM_BUFFER_SIZE 128

static void handleUSB_CDC_ACM()
{
    static uint8_t cdcBuffer[CDC_ACM_BUFFER_SIZE];

    const uint16_t bytesAvailable = CDC_GetRxBufferBytesAvailable_FS();

    // USB -> TXD
    if(bytesAvailable > 0) {
        const uint16_t bytesToRead = ( bytesAvailable >= sizeof(cdcBuffer) ) ? sizeof(cdcBuffer) : bytesAvailable;
        if( CDC_ReadRxBuffer_FS(cdcBuffer, bytesToRead) == USB_CDC_RX_BUFFER_OK ) {
            UART3_WriteBuffer(cdcBuffer, bytesToRead);
            blinkLED(true);
        }
    }

    // RXD -> USB
    uint8_t data;

    while( UART3_ReadByte(&data) ) {
        while( CDC_Transmit_FS(&data, 1) == USBD_BUSY );
        blinkLED(true);
    }
}


//
// Main program
//
int main()
{
    // Prevent FIRMWARE_VERSION from being removed by the linker
    asm volatile ( "" : : "m"(FIRMWARE_VERSION) );

    // Initialize GPIOs
    gpioInitOutput(LED_GPIO        , LED_PIN        , false);
    gpioInitInput (VBUS_DETECT_GPIO, VBUS_DETECT_PIN, false);
    gpioInitOutput(DP_EN1K5PU_GPIO , DP_EN1K5PU_PIN , false);

    // Intialize UART with the default baudrate
    UART3_Init(UART_DEFAULT_BAUD, false);

    // Delay for a while before initializing USB
    for(int i = 0; i < (USB_PRE_INIT_DELAY_MS / 100); ++i) {
        setLED(true ); HAL_Delay(50);
        setLED(false); HAL_Delay(50);
    }
    setLED(false);
    HAL_Delay(100);

    // Initialize USB
    initUSBWithRetry();

    // Infinite loop
    while(1) {
        handleUSB_CDC_ACM();
        blinkLED(false);
    }

    // The program should never get here
    return 0;
}
