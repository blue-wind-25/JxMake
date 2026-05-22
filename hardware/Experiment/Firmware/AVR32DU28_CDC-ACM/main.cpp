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

// The UART is connected to USART0 at PD.4 and PD.5
#define UART_DEVICE     USART0
#define UART_PORTMUX_GM PORTMUX_USART0_gm
#define UART_PORTMUX_GC PORTMUX_USART0_ALT3_gc
#define UART_PORT       PORTD
#define UART_TXD_PIN    PIN4_bm
#define UART_RXD_PIN    PIN5_bm

// The TXD LED is connected to PA.0
#define TXD_LED_PORT    PORTA
#define TXD_LED_PIN     PIN0_bm

// The RXD LED is connected to PA.1
#define RXD_LED_PORT    PORTA
#define RXD_LED_PIN     PIN1_bm


static inline void UART_config()
{
   // const uint32_t baud     = usbCDCLineCoding.dwDTERate;
   // const uint8_t  stopBits = usbCDCLineCoding.bCharFormat;
  //  const uint8_t  parity   = usbCDCLineCoding.bParityType;
 //   const uint8_t  dataBits = usbCDCLineCoding.bDataBits;

    // 4. Configure Frame Format
    // Asynchronous mode, No Parity, 1 Stop Bit, 8 Data Bits (8N1)
//    USART0.CTRLC = USART_CMODE_ASYNCHRONOUS_gc | USART_PMODE_DISABLED_gc | USART_SBMODE_1BIT_gc | USART_CHSIZE_8BIT_gc;

}


static inline void UART_init()
{
    // Configure the port multiplexer
    PORTMUX.USARTROUTEA = (PORTMUX.USARTROUTEA & ~UART_PORTMUX_GM) | UART_PORTMUX_GC;

    // Configure pin directions
    UART_PORT.DIRSET = UART_TXD_PIN;
    UART_PORT.DIRCLR = UART_RXD_PIN;

    // Configure the uart
    UART_config();

    /*

    // 3. Set Baud Rate
    // The modern AVR architecture uses a 16-bit baud register with a fractional component
    USART0.BAUD = (uint16_t)USART0_BAUD_RATE(baud);

    // 4. Configure Frame Format
    // Asynchronous mode, No Parity, 1 Stop Bit, 8 Data Bits (8N1)
    USART0.CTRLC = USART_CMODE_ASYNCHRONOUS_gc | USART_PMODE_DISABLED_gc | USART_SBMODE_1BIT_gc | USART_CHSIZE_8BIT_gc;

    // 5. Enable Transmitter and Receiver
    USART0.CTRLB = USART_TXEN_bm | USART_RXEN_bm;
    */
}


/*
void USART0_sendChar(char c)
{
    // Wait until the transmit data register empty flag is high
    while (!(USART0.STATUS & USART_DREIF_bm));

    // Put data into the data register
    USART0.TXDATAL = c;
}

void USART0_sendString(const char *str)
{
    while (*str)
    {
        USART0_sendChar(*str++);
    }
}

int main(void)
{
    // Initialize USART0 at 9600 baud rate on PD4/PD5
    USART0_init(9600);

    while (1)
    {
        USART0_sendString("Hello from AVR32DU28 on PD4/PD5!\r\n");
        _delay_ms(1000);
    }
}
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
