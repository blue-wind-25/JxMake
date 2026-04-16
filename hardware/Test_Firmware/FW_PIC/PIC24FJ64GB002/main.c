#include <p24FJ64GB002.h>


// ====== Configuration words =====
// CW1
#pragma config JTAGEN   = OFF
#pragma config GCP      = OFF
#pragma config GWRP     = OFF
#pragma config ICS      = PGx1
#pragma config FWDTEN   = OFF
#pragma config WINDIS   = OFF
#pragma config FWPSA    = PR128
#pragma config WDTPS    = PS32768

// CW2
#pragma config IESO     = ON
#pragma config PLLDIV   = NODIV
#pragma config PLL96MHZ = OFF
#pragma config FNOSC    = FRCDIV
#pragma config FCKSM    = CSDCMD
#pragma config OSCIOFNC = ON
#pragma config IOL1WAY  = OFF
#pragma config I2C1SEL  = PRI
#pragma config POSCMOD  = NONE

// CW3
#pragma config WPEND    = WPSTARTMEM
#pragma config WPCFG    = WPCFGDIS
#pragma config WPDIS    = WPDIS
#pragma config WUTSEL   = LEG
#pragma config SOSCSEL  = IO
#pragma config WPFP     = WPFP63

// CW4
#pragma config DSWDTEN  = OFF
#pragma config DSBOREN  = OFF
#pragma config RTCOSC   = LPRC
#pragma config DSWDTOSC = LPRC
#pragma config DSWDTPS  = DSWDTPSF


// ===== Pins =====
#define LED_1_LAT  LATBbits .LATB7
#define LED_1_TRIS TRISBbits.TRISB7

#define LED_2_LAT  LATBbits .LATB8
#define LED_2_TRIS TRISBbits.TRISB8

#define SW_PORT    PORTBbits.RB9
#define SW_TRIS    TRISBbits.TRISB9


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
