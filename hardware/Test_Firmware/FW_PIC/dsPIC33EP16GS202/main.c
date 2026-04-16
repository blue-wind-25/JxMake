#include <p33EP16GS202.h>


// ====== Configuration words =====
// FSEC
#pragma config AIVTDIS  = OFF
#pragma config CSS      = DISABLED
#pragma config CWRP     = OFF
#pragma config GSS      = DISABLED
#pragma config GWRP     = OFF
#pragma config BSEN     = OFF
#pragma config BSS      = NO_BOOT
#pragma config BWRP     = OFF

// FBSLIM
#pragma config BSLIM    = 0x1FFF

// FOSCSEL
#pragma config IESO     = OFF
#pragma config FNOSC    = FRC

// FOSC
#pragma config PLLKEN   = OFF
#pragma config FCKSM    = CSDCMD
#pragma config IOL1WAY  = OFF
#pragma config OSCIOFNC = ON
#pragma config POSCMD   = NONE

// FWDT
#pragma config WDTWIN   = WIN50
#pragma config WINDIS   = OFF
#pragma config WDTEN    = OFF
#pragma config WDTPRE   = PR128
#pragma config WDTPOST  = PS32768

// FICD
#pragma config JTAGEN   = OFF
#pragma config ICS      = PGD1

// FDEVOPT
#pragma config PWMLOCK  = OFF

// FALTREG
#pragma config CTXT2    = IPL2
#pragma config CTXT1    = IPL1


// ===== Pins =====
#define LED_1_LAT  LATBbits .LATB13
#define LED_1_TRIS TRISBbits.TRISB13

#define LED_2_LAT  LATBbits .LATB14
#define LED_2_TRIS TRISBbits.TRISB14

#define SW_TRIS    TRISBbits.TRISB12
#define SW_PORT    PORTBbits.RB12


// ===== Blink delay =====
#define BLINK_DELAY_SLOW 10
#define BLINK_DELAY_FAST  3


// ===== Helper functions =====
extern void __delay32(unsigned long cycles);

void delay_and_check_switch()
{
    unsigned int delay = (SW_PORT == 0) ? BLINK_DELAY_FAST : BLINK_DELAY_SLOW;

    while(delay--) __delay32(8000000 / 100);
}


// ===== Main program =====
int main(void)
{
    ANSELA     = 0; // Disable ADC
    ANSELB     = 0; // ---

    LED_1_TRIS = 0; // Set LED1 as output
    LED_2_TRIS = 0; // Set LED2 as output
    SW_TRIS    = 1; // Set SW   as input

    while(1) {

        // Turn on  LED1 and turn off LED2
        LED_1_LAT = 1;
        LED_2_LAT = 0;

        // Delay and check the switch
        delay_and_check_switch();

        // Turn off LED1 and turn on  LED2
        LED_1_LAT = 0;
        LED_2_LAT = 1;

        // Delay and check the switch
        delay_and_check_switch();

    } // while

    return 0;
}
