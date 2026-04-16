// ~/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/bin/arm-none-eabi-objcopy -O ihex STM32L431C8T6.ino.elf STM32L431C8T6.ino.hex


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
#define LED_1_GPIO GPIOA       // External LED
#define LED_1_PIN  GPIO_PIN_11

#define LED_2_GPIO GPIOA       // External LED
#define LED_2_PIN  GPIO_PIN_12

#define SW_GPIO    GPIOA       // External SW
#define SW_PIN     GPIO_PIN_0


void setup()
{
    __HAL_RCC_GPIOA_CLK_ENABLE();

    gpioInitOutput(LED_1_GPIO, LED_1_PIN, true );
    gpioInitOutput(LED_2_GPIO, LED_2_PIN, true );
    gpioInitInput (SW_GPIO   , SW_PIN   , false);
}


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


void loop()
{
    HAL_GPIO_WritePin(LED_1_GPIO, LED_1_PIN, GPIO_PIN_SET  ); // Active high
    HAL_GPIO_WritePin(LED_2_GPIO, LED_2_PIN, GPIO_PIN_RESET); // Active high
    delayAndCheckSwitch();

    HAL_GPIO_WritePin(LED_1_GPIO, LED_1_PIN, GPIO_PIN_RESET); // Active high
    HAL_GPIO_WritePin(LED_2_GPIO, LED_2_PIN, GPIO_PIN_SET  ); // Active high
    delayAndCheckSwitch();
}
