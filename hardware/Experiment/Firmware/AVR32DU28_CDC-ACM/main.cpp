/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <usb_cdc_acm.h>


// The LED is connected to PD.5
#define LED_PORT PORTD
#define LED_PIN  PIN5_bm


/*
 * ##### !!! TODO !!! #####
 *
 * AVR16/32/64DU14/28
 *
 * SSOP-28                     SOIC-14
 *
 *  5   PC.3   DTR    (OUT)   DTR    9
 *
 * 10   PD.4   TXD0   (OUT)   RTS   10
 * 11   PD.5   RXD0   (INP)   CTS   11
 * 12   PD.6   RTS    (OUT)   TXD1  12
 * 13   PD.7   CTS    (INP)   RXD1  13
 *
 * 22   PA.0   LED    (OUT)   PA.0   4
 * 23   PA.1   LED    (OUT)   PA.1   5
 *
 * USART0 -> ALT3
 * USART1 -> ALT1
 */


int main()
{
    // Set OSCHF as main clock source
    // NOTE : This is not actually required on the AVR DU series, but it is retained for clarity
    _PROTECTED_WRITE( CLKCTRL.MCLKCTRLA, CLKCTRL_CLKSEL_OSCHF_gc );

    while( CLKCTRL.MCLKSTATUS & CLKCTRL_SOSC_bm );

    // Disable the prescaler
    // NOTE : This is not actually required on the AVR DU series, but it is retained for clarity
    _PROTECTED_WRITE( CLKCTRL_MCLKCTRLB,  0x00 );

    // Set the OSCHF frequency to 24MHz
    _PROTECTED_WRITE( CLKCTRL.OSCHFCTRLA, CLKCTRL_FRQSEL_24M_gc );

    while( !(CLKCTRL.MCLKSTATUS & CLKCTRL_OSCHFS_bm) );

    // Initialize system and USB
    system_usb_init();

    // Initialize the LED pin
    LED_PORT.DIRSET = LED_PIN;

    // Animate the LEDs
    for(;;) {
        for( unsigned i = 0; i < 3; ++i ) {
            LED_PORT.OUTSET = LED_PIN; delayMS( 100 );
            LED_PORT.OUTCLR = LED_PIN; delayMS( 100 );
        }
        delayMS( 300 );
    }

    return 0;
}
