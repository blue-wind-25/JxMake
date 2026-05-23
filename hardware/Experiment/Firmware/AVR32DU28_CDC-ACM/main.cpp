/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <usb_cdc_acm.h>


static volatile bool usbCdcLineCodingChanged = false;

static void onLineCodingChanged()
{ usbCdcLineCodingChanged = true; }

static void UART_config()
{
    // Configure the frame format
    uint8_t frameFormat = 0;

    switch(usbCDCLineCoding.bParityType) {

        case USB_CDC_LINE_CODING_PARITY_NONE  : frameFormat |= USART_PMODE_DISABLED_gc; break;
        case USB_CDC_LINE_CODING_PARITY_ODD   : frameFormat |= USART_PMODE_ODD_gc;      break;
        case USB_CDC_LINE_CODING_PARITY_EVEN  : frameFormat |= USART_PMODE_EVEN_gc;     break;

        default :
            // USB_CDC_LINE_CODING_PARITY_MARK, USB_CDC_LINE_CODING_PARITY_SPACE, and any unknown - fall back to USB_CDC_LINE_CODING_PARITY_NONE
            frameFormat                  |= USART_PMODE_DISABLED_gc;
            usbCDCLineCoding.bParityType  = USB_CDC_LINE_CODING_PARITY_NONE;
            break;

    } // switch

    switch(usbCDCLineCoding.bCharFormat) {

        case USB_CDC_LINE_CODING_ONE_STOP_BIT : frameFormat |= USART_SBMODE_1BIT_gc; break;
        case USB_CDC_LINE_CODING_TWO_STOP_BITS: frameFormat |= USART_SBMODE_2BIT_gc; break;

        default:
            // ONE_AND_ONE_HALF - no hardware support, approximate as 2USB_CDC_LINE_CODING_TWO_STOP_BITS
            frameFormat                  |= USART_SBMODE_2BIT_gc;
            usbCDCLineCoding.bCharFormat  = USB_CDC_LINE_CODING_TWO_STOP_BITS;
            break;

    } // switch

    switch(usbCDCLineCoding.bDataBits) {

        case USB_CDC_LINE_CODING_5_DATA_BITS  : frameFormat |= USART_CHSIZE_5BIT_gc; break;
        case USB_CDC_LINE_CODING_6_DATA_BITS  : frameFormat |= USART_CHSIZE_6BIT_gc; break;
        case USB_CDC_LINE_CODING_7_DATA_BITS  : frameFormat |= USART_CHSIZE_7BIT_gc; break;
        case USB_CDC_LINE_CODING_8_DATA_BITS  : frameFormat |= USART_CHSIZE_8BIT_gc; break;

        default :
            // 16-bit and any unknown - fall back to 8-bit
            frameFormat                |= USART_CHSIZE_8BIT_gc;
            usbCDCLineCoding.bDataBits  = USB_CDC_LINE_CODING_8_DATA_BITS;
            break;

    } // switch

    // Disable TXD and RXD before changing frame format, as required by the datasheet
    UART_DEVICE.CTRLB  = 0;
    UART_PORT  .OUTSET = UART_TXD_PIN;
    UART_PORT  .DIRSET = UART_TXD_PIN;

    // Change the frame format
    UART_DEVICE.CTRLC = USART_CMODE_ASYNCHRONOUS_gc | frameFormat;

    // Configure the baud rate
    const uint32_t baudRate = (usbCDCLineCoding.dwDTERate <    2400UL) ?    2400UL
                            : (usbCDCLineCoding.dwDTERate > 2000000UL) ? 2000000UL
                            :  usbCDCLineCoding.dwDTERate;
    const uint32_t baudReg  = ( (4UL * F_CPU) + (baudRate / 2) ) / baudRate;

    UART_DEVICE.BAUD           = (uint16_t) baudReg;
    usbCDCLineCoding.dwDTERate = baudRate;

    // Re-enable RXD always; re-enable TXD only if not in a break
    UART_DEVICE.CTRLB = USART_RXEN_bm;

    if(!usbCdcBreakActive) {
        UART_DEVICE.CTRLB  |= USART_TXEN_bm;
    }
    else {
        UART_PORT  .OUTCLR  = UART_TXD_PIN; // OUTSET above briefly released break — restore LOW
    }
}

static inline void UART_init()
{
    // Configure the port multiplexer
    PORTMUX.USARTROUTEA = (PORTMUX.USARTROUTEA & ~UART_PORTMUX_GM) | UART_PORTMUX_GC;

    // Configure TXD pin to output HIGH (idle) and RXD pin to input
    UART_PORT.OUTSET        = UART_TXD_PIN;
    UART_PORT.DIRSET        = UART_TXD_PIN;
    UART_PORT.UART_TXD_CTRL = PORT_PULLUPEN_bm;

    UART_PORT.DIRCLR        = UART_RXD_PIN;
    UART_PORT.UART_RXD_CTRL = PORT_PULLUPEN_bm;

    // Configure the uart
    UART_config();

    // Initialize the DTR pin to output
    UART_DTR_PORT.DIRSET = UART_DTR_PIN;
    UART_DTR_PORT.OUTSET = UART_DTR_PIN;

    // Initialize the RTS pin to output
    UART_RTS_PORT.DIRSET = UART_RTS_PIN;
    UART_RTS_PORT.OUTSET = UART_RTS_PIN;

    // Initialize the CTS pin to input with pullup
    UART_CTS_PORT.DIRCLR        = UART_CTS_PIN;
    UART_CTS_PORT.UART_CTS_CTRL = PORT_INLVL_TTL_gc | PORT_PULLUPEN_bm;

    // Register the ISR-safe flag-setter; UART_config() runs from the main loop
    usbCdcSetLineCodingCallback = onLineCodingChanged;
}


#define LED_BLINK_DURATION_MS 50

static bool     txLedActive    = false;
static uint32_t txLastActivity = 0;

static bool     rxLedActive    = false;
static uint32_t rxLastActivity = 0;

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

        // Call USB CDC-ACM handler
        USBDevice_CDCACMHandler();

        // Apply line coding changes requested by the host (deferred from ISR context)
        if( usbCdcLineCodingChanged ) {
            usbCdcLineCodingChanged = false;
            UART_config();
        }

        // Set DTR and RTS pins
        const bool dtr = (usbCDCControlLineState & 0x01) != 0;
        const bool rts = (usbCDCControlLineState & 0x02) != 0;

        if(dtr) UART_DTR_PORT.OUTCLR = UART_DTR_PIN; // DTR is active low
        else    UART_DTR_PORT.OUTSET = UART_DTR_PIN;

        if(rts) UART_RTS_PORT.OUTCLR = UART_RTS_PIN; // RTS is active low
        else    UART_RTS_PORT.OUTSET = UART_RTS_PIN;

        // Animate the LED
        const uint32_t curTime = millis();

        if( txActivityFlag ) {
                txActivityFlag      = false;
            if( !txLedActive ) {
                TXD_LED_PORT.OUTSET = TXD_LED_PIN;
                txLastActivity      = curTime;
                txLedActive         = true;
            }
        }
        else if( txLedActive && (curTime - txLastActivity >= LED_BLINK_DURATION_MS) ) {
                TXD_LED_PORT.OUTCLR = TXD_LED_PIN;
                txLedActive         = false;
        }

        if( rxActivityFlag ) {
                rxActivityFlag      = false;
            if( !rxLedActive ) {
                RXD_LED_PORT.OUTSET = RXD_LED_PIN;
                rxLastActivity      = curTime;
                rxLedActive         = true;
            }
        }
        else if( rxLedActive && (curTime - rxLastActivity >= LED_BLINK_DURATION_MS) ) {
                RXD_LED_PORT.OUTCLR = RXD_LED_PIN;
                rxLedActive         = false;
        }

    } // for

    return 0;
}
