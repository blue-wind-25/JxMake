/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <usb_cdc_acm.h>




static inline void UART_config()
{
    // Configure the frame format
    uint8_t frameFormat = 0;

    switch(usbCDCLineCoding.bParityType) {
        case USB_CDC_LINE_CODING_PARITY_NONE               : frameFormat |= USART_PMODE_DISABLED_gc; break;
        case USB_CDC_LINE_CODING_PARITY_ODD                : frameFormat |= USART_PMODE_ODD_gc     ; break;
        case USB_CDC_LINE_CODING_PARITY_EVEN               : frameFormat |= USART_PMODE_EVEN_gc    ; break;
        case USB_CDC_LINE_CODING_PARITY_MARK               : frameFormat |= USART_PMODE_DISABLED_gc; break;
        case USB_CDC_LINE_CODING_PARITY_SPACE              : frameFormat |= USART_PMODE_DISABLED_gc; break;
    }

    switch(usbCDCLineCoding.bCharFormat) {
        case USB_CDC_LINE_CODING_ONE_STOP_BIT              : frameFormat |= USART_SBMODE_1BIT_gc   ; break;
        case USB_CDC_LINE_CODING_ONE_AND_ONE_HALF_STOP_BIT : frameFormat |= USART_SBMODE_2BIT_gc   ; break;
        case USB_CDC_LINE_CODING_TWO_STOP_BITS             : frameFormat |= USART_SBMODE_2BIT_gc   ; break;
    }

    switch(usbCDCLineCoding.bDataBits) {
        case USB_CDC_LINE_CODING_5_DATA_BITS               : frameFormat |= USART_CHSIZE_5BIT_gc   ; break;
        case USB_CDC_LINE_CODING_6_DATA_BITS               : frameFormat |= USART_CHSIZE_6BIT_gc   ; break;
        case USB_CDC_LINE_CODING_7_DATA_BITS               : frameFormat |= USART_CHSIZE_7BIT_gc   ; break;
        case USB_CDC_LINE_CODING_8_DATA_BITS               : frameFormat |= USART_CHSIZE_8BIT_gc   ; break;
        case USB_CDC_LINE_CODING_16_DATA_BITS              : frameFormat |= USART_CHSIZE_8BIT_gc   ; break;
        default                                            : frameFormat |= USART_CHSIZE_8BIT_gc   ; break;
    }

    UART_DEVICE.CTRLC = USART_CMODE_ASYNCHRONOUS_gc | frameFormat;

    // Configure the baud rate
    const uint32_t baudRate = (usbCDCLineCoding.dwDTERate < 2400UL   ) ? 2400UL
                            : (usbCDCLineCoding.dwDTERate > 2000000UL) ? 2000000UL
                            :  usbCDCLineCoding.dwDTERate;
    const uint32_t baudReg  = ( (4UL * F_CPU) + (baudRate / 2) ) / baudRate;

    UART_DEVICE.BAUD = (uint16_t) baudReg;

    usbCDCLineCoding.dwDTERate = baudRate;
}


static inline void UART_init()
{
    // Configure the port multiplexer
    PORTMUX.USARTROUTEA = (PORTMUX.USARTROUTEA & ~UART_PORTMUX_GM) | UART_PORTMUX_GC;

    // Configure TXD pin to output and RXD pin to input
    UART_PORT.DIRSET = UART_TXD_PIN;
    UART_PORT.DIRCLR = UART_RXD_PIN;

    // Configure the uart
    UART_config();

    // Enable Transmitter and Receiver
    UART_DEVICE.CTRLB = USART_TXEN_bm | USART_RXEN_bm;

    // Initialize the DTR pin to output
    UART_DTR_PORT.DIRSET = UART_DTR_PIN;
    UART_DTR_PORT.OUTSET = UART_DTR_PIN;

    // Initialize the RTS pin to output
    UART_RTS_PORT.DIRSET = UART_RTS_PIN;
    UART_RTS_PORT.OUTSET = UART_RTS_PIN;

    // Initialize the CTS pin to input with pullup
    UART_CTS_PORT.DIRCLR        = UART_CTS_PIN;
    UART_CTS_PORT.UART_CTS_CTRL = PORT_INLVL_TTL_gc | PORT_PULLUPEN_bm;
}

/*
void UART_sendChar(char c)
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
*/

int main()
{
    // Set OSCHF as main clock source (NOTE : this is not actually required on the AVR DU series, but it is retained for clarity)
    _PROTECTED_WRITE( CLKCTRL.MCLKCTRLA, CLKCTRL_CLKSEL_OSCHF_gc );

    while( CLKCTRL.MCLKSTATUS & CLKCTRL_SOSC_bm );

    // Disable the prescaler (NOTE : this is not actually required on the AVR DU series, but it is retained for clarity)
    _PROTECTED_WRITE( CLKCTRL_MCLKCTRLB,  0x00 );

    // Set the OSCHF frequency to 24MHz
    _PROTECTED_WRITE( CLKCTRL.OSCHFCTRLA, CLKCTRL_FRQSEL_24M_gc );

    while( !(CLKCTRL.MCLKSTATUS & CLKCTRL_OSCHFS_bm) );

    // Initialize system and USB
    system_usb_init();

    // Initialize LED pins to output
    TXD_LED_PORT.DIRSET = TXD_LED_PIN;
    TXD_LED_PORT.OUTCLR = TXD_LED_PIN;

    RXD_LED_PORT.DIRSET = RXD_LED_PIN;
    RXD_LED_PORT.OUTCLR = RXD_LED_PIN;

    // Initialize UART
    UART_init();

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
    }

    return 0;
}
