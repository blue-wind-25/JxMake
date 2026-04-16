#include "stm8l15x.h"


#ifndef F_CPU
#define F_CPU 16000000UL
#endif


// Define the pins
#define LED_1_GPIO  GPIOB      // External
#define LED_1_PIN   GPIO_Pin_0

#define LED_2_GPIO  GPIOB      // External
#define LED_2_PIN   GPIO_Pin_1

#define SW_GPIO     GPIOB      // External
#define SW_PIN      GPIO_Pin_3


#define T_COUNT(X) (   (   ( (unsigned long) (F_CPU) ) * ( (unsigned long) (X) ) / 1000000UL + 1UL   ) / 2UL   )

static inline void __delay_cycle(unsigned short __ticks)
{
    __asm__("nop");
    do { __ticks--; } while(__ticks);
    __asm__("nop");
    __asm__("nop");
}

static inline void __delay_us(unsigned short __us)
{ __delay_cycle( (unsigned short) T_COUNT(__us) ); }

static inline void __delay_ms(unsigned short __ms)
{ while(__ms--) __delay_us(1000U); }


void assert_failed(uint8_t* file, uint32_t line)
{
    (void) file;
    (void) line;

    // Go to infinite loop
    while(1);
}


static char fast = 1;

static void delayAndCheckSwitch(char cnt)
{
    // Delay and check the switch
    for(char i = 0; i < cnt; ++i) {
        // Delay for a while
        __delay_ms(10U);
        // Check if the switch is pressed
        if( GPIO_ReadInputDataBit(SW_GPIO, SW_PIN) ) {
            // Debouncing
            __delay_ms(100U);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( GPIO_ReadInputDataBit(SW_GPIO, SW_PIN) );
}


void main()
{
    // Set clock to full speed (16MHz)
    CLK_DeInit();
    CLK_SYSCLKSourceSwitchCmd(ENABLE);
    CLK_SYSCLKSourceConfig(CLK_SYSCLKSource_HSI);
    CLK_SYSCLKDivConfig(CLK_SYSCLKDiv_1);
    while( CLK_GetSYSCLKSource() != CLK_SYSCLKSource_HSI );

    // Delay the start to ensure the SWIM pin can be accessed by the programmer
    // (in case the program use the SWIM pin as GPIO later)
    __delay_ms(2500U);

    // Setup GPIO
    GPIO_Init(LED_1_GPIO, LED_1_PIN, GPIO_Mode_Out_PP_Low_Fast);
    GPIO_Init(LED_2_GPIO, LED_2_PIN, GPIO_Mode_Out_PP_Low_Fast);
    GPIO_Init(SW_GPIO   , SW_PIN   , GPIO_Mode_In_FL_No_IT    ); // Active high

    // Infinite loop
    static const char delay_s_s =  5;
    static const char delay_s_l = 10;

    static const char delay_l_s = 15;
    static const char delay_l_l = 30;

    fast = !fast;

    while(1) {

        for(char i = 0; i < 3; ++i) {
            GPIO_SetBits  (LED_1_GPIO, LED_1_PIN); delayAndCheckSwitch(fast ? delay_s_s : delay_s_l);
            GPIO_ResetBits(LED_1_GPIO, LED_1_PIN); delayAndCheckSwitch(fast ? delay_s_s : delay_s_l);
        }

        for(char i = 0; i < 3; ++i) {
            GPIO_SetBits  (LED_2_GPIO, LED_2_PIN); delayAndCheckSwitch(fast ? delay_s_s : delay_s_l);
            GPIO_ResetBits(LED_2_GPIO, LED_2_PIN); delayAndCheckSwitch(fast ? delay_s_s : delay_s_l);
        }

        delayAndCheckSwitch(fast ? delay_l_s : delay_l_l);

    } // While
}
