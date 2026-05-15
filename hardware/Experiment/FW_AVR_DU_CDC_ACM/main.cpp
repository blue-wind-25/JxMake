#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/pgmspace.h>

#include <util/delay.h>

#include "inc_usb_avr_du.h"


// The LED is connected to PD.5
#define LED_PORT PORTD
#define LED_PIN  PIN5_bm


int main()
{
    // Set OSCHF as main clock source
    // NOTE : This is not actually required on the AVR Dx/Ex/Sx series, but it is retained for clarity
    _PROTECTED_WRITE(CLKCTRL.MCLKCTRLA, CLKCTRL_CLKSEL_OSCHF_gc);

    while(CLKCTRL.MCLKSTATUS & CLKCTRL_SOSC_bm);

    // Disable the prescaler
    // NOTE : This is not actually required on the AVR Dx/Sx series, but it is retained for clarity
    _PROTECTED_WRITE(CLKCTRL_MCLKCTRLB, 0x00);

    /*
    // Change the OSCHF frequency (F_CPU is 24MHz)
    _PROTECTED_WRITE(CLKCTRL.OSCHFCTRLA, CLKCTRL_FRQSEL_24M_gc);

    while( !(CLKCTRL.MCLKSTATUS & CLKCTRL_OSCHFS_bm) );
    */

    // Initialize USB
    usb_init();

    // Initialize IO
    LED_PORT.DIRSET  = LED_PIN;

    // Animate the LEDs
    for(;;) {
        for(unsigned i = 0; i < 3; ++i) {
            LED_PORT.OUTSET = LED_PIN; delayMS(100);
            LED_PORT.OUTCLR = LED_PIN; delayMS(100);
        }
        delayMS(300);
    }

    return 0;
}
