//#include <SEGGER/SEGGER_RTT.h>

#include <stm32l1xx.h>
#include <stm32l1xx_gpio.h>
#include <stm32l1xx_rcc.h>


static void gpioInitOutput(GPIO_TypeDef* GPIOx, uint16_t gpioPin, const char initialState)
{
    GPIO_WriteBit(GPIOx, gpioPin, initialState ? Bit_SET : Bit_RESET);

    GPIO_InitTypeDef gpioInitStruct = {};

    // Configure the pin as output
    gpioInitStruct.GPIO_Pin   = gpioPin;
    gpioInitStruct.GPIO_Mode  = GPIO_Mode_OUT;
    gpioInitStruct.GPIO_Speed = GPIO_Speed_400KHz;
    gpioInitStruct.GPIO_OType = GPIO_OType_PP;
    gpioInitStruct.GPIO_PuPd  = GPIO_PuPd_NOPULL;

    GPIO_Init(GPIOx, &gpioInitStruct);
}


static void gpioInitInput(GPIO_TypeDef* GPIOx, const uint16_t gpioPin, const char pullUp)
{
    GPIO_InitTypeDef gpioInitStruct = {};

    // Configure the pin as output
    gpioInitStruct.GPIO_Pin   = gpioPin;
    gpioInitStruct.GPIO_Mode  = GPIO_Mode_IN;
    gpioInitStruct.GPIO_Speed = GPIO_Speed_400KHz;
    gpioInitStruct.GPIO_OType = GPIO_OType_OD;
    gpioInitStruct.GPIO_PuPd  = pullUp ? GPIO_PuPd_UP : GPIO_PuPd_NOPULL;

    GPIO_Init(GPIOx, &gpioInitStruct);
}


// Define the pins
#define LED_0_GPIO GPIOC       // Built-in LED
#define LED_0_PIN  GPIO_Pin_13

#define LED_1_GPIO GPIOA       // External LED
#define LED_1_PIN  GPIO_Pin_5

#define LED_2_GPIO GPIOA       // External LED
#define LED_2_PIN  GPIO_Pin_7

#define SW_GPIO    GPIOB       // External SW
#define SW_PIN     GPIO_Pin_1

#define V33_GPIO   GPIOB       // Enable 3V3
#define V33_PIN    GPIO_Pin_15


static char fast = 0;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        for(int i = 0; i < 5000; ++i) __asm __volatile ( "nop\n\t" );
        // Check if the switch is pressed
        if( !GPIO_ReadInputDataBit(SW_GPIO, SW_PIN) ) {
            // Debouncing
            for(int i = 0; i < 50000; ++i) __asm __volatile ( "nop\n\t" );
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !GPIO_ReadInputDataBit(SW_GPIO, SW_PIN) );
}


int main(void)
{
    //Disables write buffering - makes all bus faults precise, but slows everything down
    SCnSCB->ACTLR |= SCnSCB_ACTLR_DISDEFWBUF_Msk;

    //SEGGER_RTT_Init();
    //SEGGER_RTT_printf(0, "Hello from STM32!\n");

    RCC_AHBPeriphClockCmd(RCC_AHBPeriph_GPIOA, ENABLE);
    RCC_AHBPeriphClockCmd(RCC_AHBPeriph_GPIOB, ENABLE);
    RCC_AHBPeriphClockCmd(RCC_AHBPeriph_GPIOC, ENABLE);

    gpioInitOutput(LED_0_GPIO, LED_0_PIN, 1);
    gpioInitOutput(LED_1_GPIO, LED_1_PIN, 1);
    gpioInitOutput(LED_2_GPIO, LED_2_PIN, 1);
    gpioInitInput (SW_GPIO   , SW_PIN   , 0);
    gpioInitOutput(V33_GPIO  , V33_PIN  , 1);

    GPIO_WriteBit(V33_GPIO, V33_PIN, Bit_RESET); // Active low

    while(1) {

        GPIO_WriteBit(LED_0_GPIO, LED_0_PIN, Bit_RESET); // Active low

        GPIO_WriteBit(LED_1_GPIO, LED_1_PIN, Bit_SET  ); // Active high
        GPIO_WriteBit(LED_2_GPIO, LED_2_PIN, Bit_RESET); // Active high
        delayAndCheckSwitch();

        GPIO_WriteBit(LED_1_GPIO, LED_1_PIN, Bit_RESET); // Active high
        GPIO_WriteBit(LED_2_GPIO, LED_2_PIN, Bit_SET  ); // Active high
        delayAndCheckSwitch();

        GPIO_WriteBit(LED_0_GPIO, LED_0_PIN, Bit_SET  ); // Active low

        GPIO_WriteBit(LED_1_GPIO, LED_1_PIN, Bit_SET  ); // Active high
        GPIO_WriteBit(LED_2_GPIO, LED_2_PIN, Bit_RESET); // Active high
        delayAndCheckSwitch();

        GPIO_WriteBit(LED_1_GPIO, LED_1_PIN, Bit_RESET); // Active high
        GPIO_WriteBit(LED_2_GPIO, LED_2_PIN, Bit_SET  ); // Active high
        delayAndCheckSwitch();

    } // while

    __builtin_unreachable();
}
