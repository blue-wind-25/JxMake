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
    static uint32_t lastTick = 0;
    const  uint32_t interval = active ? 100 : 1000;

    // Check if enough time has passed
    if( HAL_GetTick() - lastTick >= interval ) {
        lastTick = HAL_GetTick();
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


#define UART3_RX_BUFFER_SIZE 128

static          uint8_t            uart3_rx_buffer[UART3_RX_BUFFER_SIZE];
static volatile uint16_t           uart3_rx_head = 0;
static volatile uint16_t           uart3_rx_tail = 0;
static          uint8_t            uart3_rxByte;

static volatile bool               uart3_tx_busy = false;

static          UART_HandleTypeDef huart3;
static          DMA_HandleTypeDef  hdma_usart3_tx;

extern "C" void USART3_IRQHandler()
{ HAL_UART_IRQHandler(&huart3); }

extern "C" void DMA1_Channel2_IRQHandler(void)
{ HAL_DMA_IRQHandler(&hdma_usart3_tx); }

void HAL_UART_TxCpltCallback(UART_HandleTypeDef *huart)
{ if(huart->Instance == USART3) uart3_tx_busy = false; }

void HAL_UART_RxCpltCallback(UART_HandleTypeDef* huart)
{
    if(huart->Instance != USART3) return;

    uart3_rx_buffer[uart3_rx_head] = uart3_rxByte;
    uart3_rx_head                  = (uart3_rx_head + 1) % UART3_RX_BUFFER_SIZE;

    HAL_UART_Receive_IT(&huart3, &uart3_rxByte, 1);
}

void HAL_UART_MspInit(UART_HandleTypeDef* huart)
{
    if(huart->Instance != USART3) return;

    GPIO_InitTypeDef GPIO_InitStruct = {};

    GPIO_InitStruct.Pin   = UART_TXD_PIN;
    GPIO_InitStruct.Mode  = GPIO_MODE_AF_PP;
    GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_HIGH;

    HAL_GPIO_Init(UART_TXD_GPIO, &GPIO_InitStruct);

    GPIO_InitStruct.Pin   = UART_RXD_PIN;
    GPIO_InitStruct.Mode  = GPIO_MODE_AF_INPUT;
    GPIO_InitStruct.Pull  = GPIO_NOPULL;

    HAL_GPIO_Init(UART_RXD_GPIO, &GPIO_InitStruct);

    HAL_NVIC_SetPriority(USART3_IRQn, 0, 0);
    HAL_NVIC_EnableIRQ(USART3_IRQn);
}

static void UART3_Init(uint32_t baudrate)
{
    __HAL_RCC_USART3_CLK_ENABLE();

    huart3.Instance          = USART3;
    huart3.Init.BaudRate     = baudrate;
    huart3.Init.WordLength   = UART_WORDLENGTH_8B;
    huart3.Init.StopBits     = UART_STOPBITS_1;
    huart3.Init.Parity       = UART_PARITY_NONE;
    huart3.Init.Mode         = UART_MODE_TX_RX;
    huart3.Init.HwFlowCtl    = UART_HWCONTROL_NONE;
    huart3.Init.OverSampling = UART_OVERSAMPLING_16;

    if( HAL_UART_Init(&huart3) != HAL_OK ) Error_Handler();

    __HAL_RCC_DMA1_CLK_ENABLE();

    hdma_usart3_tx.Instance                 = DMA1_Channel2; // USART3_TX is mapped to Channel 2
    hdma_usart3_tx.Init.Direction           = DMA_MEMORY_TO_PERIPH;
    hdma_usart3_tx.Init.PeriphInc           = DMA_PINC_DISABLE;
    hdma_usart3_tx.Init.MemInc              = DMA_MINC_ENABLE;
    hdma_usart3_tx.Init.PeriphDataAlignment = DMA_PDATAALIGN_BYTE;
    hdma_usart3_tx.Init.MemDataAlignment    = DMA_MDATAALIGN_BYTE;
    hdma_usart3_tx.Init.Mode                = DMA_NORMAL;
    hdma_usart3_tx.Init.Priority            = DMA_PRIORITY_LOW;

    if( HAL_DMA_Init(&hdma_usart3_tx) != HAL_OK ) Error_Handler();

    __HAL_LINKDMA(&huart3, hdmatx, hdma_usart3_tx);

    HAL_NVIC_SetPriority(DMA1_Channel2_IRQn, 0, 0);
    HAL_NVIC_EnableIRQ(DMA1_Channel2_IRQn);
}

static void UART3_SetBaudrate(uint32_t baudrate)
{
    huart3.Init.BaudRate = baudrate;

    if( HAL_UART_Init(&huart3) != HAL_OK ) Error_Handler();
}

static void UART3_StartReception()
{
    static uint8_t dummy;
    HAL_UART_Receive_IT(&huart3, &dummy, 1);
}

static bool UART3_ReadByte(uint8_t* data)
{
    if(uart3_rx_head == uart3_rx_tail) return false;

    *data         = uart3_rx_buffer[uart3_rx_tail];
    uart3_rx_tail = (uart3_rx_tail + 1) % UART3_RX_BUFFER_SIZE;

    return true;
}

static bool UART3_WriteBuffer_DMA(uint8_t* buf, uint16_t len)
{
    if(uart3_tx_busy) return false;

    if( HAL_UART_Transmit_DMA(&huart3, buf, len) == HAL_OK ) {
        uart3_tx_busy = true;
        return true;
    }

    return false;
}


#define USB_PRE_INIT_DELAY_MS  1000
#define USB_REINIT_DELAY_MS     500
#define USB_REINIT_MAX_RETRIES    5

static bool initUSB()
{
    // The 'JxMake USB Serial Hub GLST' module has a pull up resistor on the USB_DP (PA12) pin which
    // is connected to another GPIO
    HAL_GPIO_WritePin(DP_EN1K5PU_GPIO, DP_EN1K5PU_PIN, GPIO_PIN_RESET); HAL_Delay(300);
    HAL_GPIO_WritePin(DP_EN1K5PU_GPIO, DP_EN1K5PU_PIN, GPIO_PIN_SET  ); HAL_Delay(100);

    if( USBD_Init                 (&hUsbDeviceFS, &FS_Desc, 0            ) != USBD_OK ) return false;
    if( USBD_RegisterClass        (&hUsbDeviceFS, &USBD_CDC              ) != USBD_OK ) return false;
    if( USBD_CDC_RegisterInterface(&hUsbDeviceFS, &USBD_Interface_fops_FS) != USBD_OK ) return false;
    if( USBD_Start                (&hUsbDeviceFS                         ) != USBD_OK ) return false;

    return true;
}

void initUSBWithRetry()
{
    // ##### ??? TODO : VBUS_DETECT ??? #####

    int attempt = 0;

    while(attempt < USB_REINIT_MAX_RETRIES) {

        if( initUSB() ) return;

        ++attempt;

        USBD_Stop  (&hUsbDeviceFS);
        USBD_DeInit(&hUsbDeviceFS);

        HAL_Delay(USB_REINIT_DELAY_MS);

    } // while

    blinkErrorLED();
}


#define CDC_ACM_BUFFER_SIZE 128

static void handleUSB_CDC_ACM()
{
    static uint8_t cdcBuffer[CDC_ACM_BUFFER_SIZE];

    const uint16_t bytesAvailable = CDC_GetRxBufferBytesAvailable_FS();

    // ##### !!! TODO !!! #####
    (void) UART3_SetBaudrate;

    // ##### !!! TODO : XCK RTS CTS DTR !!! #####

#if 0

    (void) UART3_ReadByte;
    (void) UART3_WriteBuffer_DMA;

    if(bytesAvailable <= 0) return;

    const uint16_t bytesToRead = ( bytesAvailable >= sizeof(cdcBuffer) ) ? sizeof(cdcBuffer) : bytesAvailable;
    if( CDC_ReadRxBuffer_FS(cdcBuffer, bytesToRead) == USB_CDC_RX_BUFFER_OK ) {
        while( CDC_Transmit_FS(cdcBuffer, bytesToRead) == USBD_BUSY );
        blinkLED(true);
    }

#else

    // Check if USB has data available
    if(bytesAvailable > 0) {
        const uint16_t bytesToRead = ( bytesAvailable >= sizeof(cdcBuffer) ) ? sizeof(cdcBuffer) : bytesAvailable;
        if( CDC_ReadRxBuffer_FS(cdcBuffer, bytesToRead) == USB_CDC_RX_BUFFER_OK ) {
            UART3_WriteBuffer_DMA(cdcBuffer, bytesToRead);
            blinkLED(true);
        }
    }

    // Check if UART3 has data in its ring buffer
    uint8_t rxData;

    while( UART3_ReadByte(&rxData) ) {
        while( CDC_Transmit_FS(&rxData, 1) == USBD_BUSY );
        blinkLED(true);
    }

    // Wait until the DMA is complete
    while(uart3_tx_busy);

#endif
}


//
// Main program
//
int main()
{
    // Initialize GPIOs
    gpioInitOutput(LED_GPIO        , LED_PIN        , false);
    gpioInitInput (VBUS_DETECT_GPIO, VBUS_DETECT_PIN, false);
    gpioInitOutput(DP_EN1K5PU_GPIO , DP_EN1K5PU_PIN , false);

    // Intialize UART with the default baudrate
    (void) UART3_Init;
    (void) UART3_StartReception;
    UART3_Init(9600);
    UART3_StartReception();

#if 0
    // The 'BluePill' board  has a pull up resistor on the USB_DP (PA12) pin, set it low initially
    // to force host to re-enumerate the device
    GPIO_InitTypeDef GPIO_InitStruct = {};

    GPIO_InitStruct.Pin   = GPIO_PIN_12;
    GPIO_InitStruct.Mode  = GPIO_MODE_OUTPUT_PP;
    GPIO_InitStruct.Pull  = GPIO_PULLDOWN;
    GPIO_InitStruct.Speed = GPIO_SPEED_HIGH;

    HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);
    HAL_GPIO_WritePin(GPIOA, GPIO_PIN_12, GPIO_PIN_RESET);

    HAL_Delay(100);

    // Initialize USB
    if( USBD_Init                 (&hUsbDeviceFS, &FS_Desc, 0            ) != USBD_OK ) return false;
    if( USBD_RegisterClass        (&hUsbDeviceFS, &USBD_CDC              ) != USBD_OK ) return false;
    if( USBD_CDC_RegisterInterface(&hUsbDeviceFS, &USBD_Interface_fops_FS) != USBD_OK ) return false;
    if( USBD_Start                (&hUsbDeviceFS                         ) != USBD_OK ) return false;
#else
    // Delay for a while before initializing USB
    for(int i = 0; i < (USB_PRE_INIT_DELAY_MS / 100); ++i) {
        setLED(true ); HAL_Delay(50);
        setLED(false); HAL_Delay(50);
    }
    setLED(false);
    HAL_Delay(100);

    // Initialize USB
    initUSBWithRetry();
#endif

    // Infinite loop
    while(1) {
        handleUSB_CDC_ACM();
        blinkLED(false);
    }

    // The program should never get here
    return 0;
}
