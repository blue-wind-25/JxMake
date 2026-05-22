/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <usb_cdc_acm.h>


/*
 * AVR16/32/64DU14/28
 *
 * SOIC-14   SSOP-28   PORT   Function      Direction
 *  9          5       PC.3   DTR           OUT
 *
 * 10         10       PD.4   TXD0 (ALT3)   OUT
 * 11         11       PD.5   RXD0 (ALT3)   INP
 *
 * 12         12       PD.6   RTS           OUT
 * 13         13       PD.7   CTS           INP
 *
 *  4         22       PA.0   LED           OUT
 *  5         23       PA.1   LED           OUT
 */

// The TXD LED is connected to PA.0
#define TXD_PORT PORTA
#define TXD_PIN  PIN0_bm

// The RXD LED is connected to PA.1
#define RXD_PORT PORTA
#define RXD_PIN  PIN1_bm


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
  //  LED_PORT.DIRSET = LED_PIN;

    // Animate the LEDs
    for(;;) {
        /*
        for( unsigned i = 0; i < 3; ++i ) {
            LED_PORT.OUTSET = LED_PIN; delayMS( 100 );
            LED_PORT.OUTCLR = LED_PIN; delayMS( 100 );
        }
        delayMS( 300 );
        */

        /*
                        // Line coding get test
                        const uint32_t baud     = usbCDCLineCoding.dwDTERate;
                        const uint8_t  stopBits = usbCDCLineCoding.bCharFormat;
                        const uint8_t  parity   = usbCDCLineCoding.bParityType;
                        const uint8_t  dataBits = usbCDCLineCoding.bDataBits;
                        (void) stopBits;
                        (void) parity;
                        (void) dataBits;
                        // Special baudrate check test
                        if( baud == 300 )
                            for(;;);  // Hung on purpose

                        // Line state get test
                        const bool dtr = (usbCDCControlLineState & 0x01) != 0;
                        const bool rts = (usbCDCControlLineState & 0x02) != 0;
                        (void) dtr;
                        (void) rts;

        */

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
    }

    return 0;
}
