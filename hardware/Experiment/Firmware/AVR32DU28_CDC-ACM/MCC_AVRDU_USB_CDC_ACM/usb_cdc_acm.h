/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * Most of the code flow in this file are adapted from Microchip examples:
 *
 *     USB Communication Device Class (CDC) Data Logger with AVR DU
 *     https://github.com/microchip-pic-avr-examples/avr64du32-cnano-usb-cdc-datalogger-mplab-mcc
 *     https://github.com/microchip-pic-avr-examples/avr64du32-cnano-usb-cdc-datalogger-mplab-mcc/blob/main/LICENSE.txt
 *
 *     (C) 2024 Microchip Technology Inc. and its subsidiaries.
 *
 *     Subject to your compliance with these terms, you may use Microchip software
 *     and any derivatives exclusively with Microchip products. You're responsible
 *     for complying with 3rd party license terms applicable to your use of 3rd
 *     party software (including open source software) that may accompany Microchip
 *     software.
 *
 *     SOFTWARE IS "AS IS". NO WARRANTIES, WHETHER EXPRESS, IMPLIED OR STATUTORY,
 *     APPLY TO THIS SOFTWARE, INCLUDING ANY IMPLIED WARRANTIES OF NON-INFRINGEMENT,
 *     MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 *     IN NO EVENT WILL MICROCHIP BE LIABLE FOR ANY INDIRECT, SPECIAL, PUNITIVE,
 *     INCIDENTAL OR CONSEQUENTIAL LOSS, DAMAGE, COST OR EXPENSE OF ANY KIND
 *     WHATSOEVER RELATED TO THE SOFTWARE, HOWEVER CAUSED, EVEN IF MICROCHIP
 *     HAS BEEN ADVISED OF THE POSSIBILITY OR THE DAMAGES ARE FORESEEABLE. TO
 *     THE FULLEST EXTENT ALLOWED BY LAW, MICROCHIP'S TOTAL LIABILITY ON ALL
 *     CLAIMS RELATED TO THE SOFTWARE WILL NOT EXCEED AMOUNT OF FEES, IF ANY,
 *     YOU PAID DIRECTLY TO MICROCHIP FOR THIS SOFTWARE.
 */



#ifndef USB_CDC_ACM_H
#define USB_CDC_ACM_H


#include <util/atomic.h>

#include "../config.h"


extern "C" {
    #include <usb_descriptors.h>
    #include <usb_cdc/usb_cdc.h>
    #include <usb_cdc/usb_cdc_virtual_serial_port.h>
}


static void USBDevice_CDCACMHandler();


volatile uint32_t millisCnt = 0;

ISR( TCA0_OVF_vect )
{
    ++millisCnt;
    TCA0.SINGLE.INTFLAGS = TCA_SINGLE_OVF_bm;
}

static inline uint32_t millis()
{
    uint32_t m;

    ATOMIC_BLOCK( ATOMIC_RESTORESTATE )
    {
        m = millisCnt;
    }

    return m;
}

static inline void delayMS( uint32_t mS )
{
    const uint32_t start = millis();

    while( millis() - start < mS ) USBDevice_CDCACMHandler();
}


static volatile bool txActivityFlag = false;
static volatile bool rxActivityFlag = false;

static void USBDevice_CDCACMHandler()
{
    /*
     * NOTE : USB_CDC_REQUEST_SET_LINE_CODING and USB_CDC_REQUEST_SET_CONTROL_LINE_STATE are handled
     *        by 'USB_CDCRequestHandler()' in 'usb_cdc/usb_cdc.h'
     */

    // USB CDC-ACM TX service (device to host)
    if( !CIRCBUF_Empty( &usbCDCTransmitBuffer ) ) {
        if( !USB_PipeStatusIsBusy( CDCTxPipe ) ) {
            USB_TransferWriteStart( CDCTxPipe, usbCDCTransmitBuffer.content, usbCDCTransmitBuffer.head, USB_CDCDataTransmitted );
        }
    }

    // USB CDC-ACM RX service (host to device)
    if( USB_CDC_RX_PACKET_SIZE <= CIRCBUF_FreeSpace( &usbCDCReceiveBuffer ) ) {
        if( !USB_PipeStatusIsBusy( CDCRxPipe ) ) {
            USB_TransferReadStart( CDCRxPipe, usbCDCReceiveTempBuffer, USB_CDC_RX_PACKET_SIZE, USB_CDCDataReceived );
        }
    }

    // Break state handling
    static uint32_t breakStartTime = 0;
    static bool     breakInflight  = false;

    if( usbCdcBreakActive ) {
        // If it is a timed break, check if time has run out
        if( usbCdcBreakDuration != 0xFFFF ) {
            if( !breakInflight ) {
                breakStartTime = millis();
                breakInflight  = true;
            }
            else if( millis() - breakStartTime >= usbCdcBreakDuration ) {
                // Timer expired - turn off break state
                usbCdcBreakActive = false;
                breakInflight     = false;
            }
        }
        // If the break state is still supposed to be active, force the hardware line low
        if(usbCdcBreakActive) {
            UART_DEVICE.CTRLB   &= ~USART_TXEN_bm;
            UART_PORT  .DIRSET   =  UART_TXD_PIN;
            UART_PORT  .OUTCLR   =  UART_TXD_PIN;
        }
        else {
            UART_DEVICE  .CTRLB |=  USART_TXEN_bm;
        }
    }
    else {
        breakInflight = false;
    }

    // USB receive buffer -> physical UART TX wire
    uint8_t cdcData;

    if( !usbCdcBreakActive) {
        while(    ( UART_DEVICE.STATUS & USART_DREIF_bm )
               && ( CIRCBUF_Dequeue( &usbCDCReceiveBuffer, &cdcData ) == BUFFER_SUCCESS)
        ) {
            UART_DEVICE.TXDATAL = cdcData;
            txActivityFlag      = true;
        }
    }

    // Physical UART RX wire -> USB transmit buffer
    while( !CIRCBUF_Full( &usbCDCTransmitBuffer) ) {
        if( (UART_DEVICE.STATUS & USART_RXCIF_bm) ) {
            cdcData        = UART_DEVICE.RXDATAL;
            rxActivityFlag = true;
            CIRCBUF_Enqueue( &usbCDCTransmitBuffer, cdcData );
        }
        else {
            break;
        }
    }

    // Handle CTS pin
    static uint16_t lastState = 0;
    uint16_t        uartState = 0;

    /*
     *  Bit | Mask                       | Meaning
     *  ----+----------------------------+--------------------
     *  0   | CDC_SERIAL_STATE_RXCARRIER | Data Carrier Detect
     *  1   | CDC_SERIAL_STATE_TXCARRIER | Data Set Ready
     *  2   | CDC_SERIAL_STATE_BREAK     | Break Signal State
     *  3   | CDC_SERIAL_STATE_RING      | Ring Indicator
     *  4   | CDC_SERIAL_STATE_FRAMING   | Framing Error
     *  5   | CDC_SERIAL_STATE_PARITY    | Parity Error
     *  6   | CDC_SERIAL_STATE_OVERRUN   | Overrun Error
     *  7-15| Reserved                   | Unused
     */
    if( !(UART_CTS_PORT.IN & UART_CTS_PIN) ) uartState |= 0b00001011; // Set   all to   DCD, DSR, and RI
    else                                     uartState &= 0b11110100; // Clear all from DCD, DSR, and RI

    if( uartState != lastState ) {
        if( USB_CDCSendSerialState( uartState ) == SUCCESS ) lastState = uartState;
    }
}


static void system_usb_init()
{
    // Reinitialize OSCHF
    _PROTECTED_WRITE( CLKCTRL.OSCHFCTRLA, CLKCTRL.OSCHFCTRLA | CLKCTRL_ALGSEL_BIN_gc | CLKCTRL_AUTOTUNE_SOF_gc );
    _PROTECTED_WRITE( CLKCTRL.OSCHFTUNE , 0x00                                                                 );

    while( !(CLKCTRL.MCLKSTATUS & CLKCTRL_OSCHFS_bm) );

    // Start USB
    SYSCFG.VUSBCTRL              = ~SYSCFG_USBVREG_bm; // USBVREG disable
    usbCDCControlLineState       = 0;
    usbCDCLineCoding.dwDTERate   = 0;
    usbCDCLineCoding.bCharFormat = USB_CDC_LINE_CODING_ONE_STOP_BIT;
    usbCDCLineCoding.bParityType = USB_CDC_LINE_CODING_PARITY_NONE;
    usbCDCLineCoding.bDataBits   = USB_CDC_LINE_CODING_8_DATA_BITS;

    USB0.INTCTRLA                = USB_RESET_bm | USB_STALLED_bm | USB_UNF_bm | USB_OVF_bm;
    USB0.INTCTRLB                = USB_TRNCOMPL_bm | USB_GNDONE_bm | USB_SETUP_bm;

    SYSCFG.VUSBCTRL              = SYSCFG_USBVREG_bm;

    USB_Start();

    // Enable TCA0 in normal mode
    TCA0.SINGLE.CTRLA   = 0;
    TCA0.SINGLE.CNT     = 0;
    TCA0.SINGLE.PER     = (F_CPU / 64 / 1000) - 1;
    TCA0.SINGLE.INTCTRL = TCA_SINGLE_OVF_bm;
    TCA0.SINGLE.CTRLA   = TCA_SINGLE_CLKSEL_DIV64_gc | TCA_SINGLE_ENABLE_bm;

    // Initialize interrupt
    _PROTECTED_WRITE( CPUINT.CTRLA, 0x00 );

    CPUINT.LVL0PRI = 0x00;
    CPUINT.LVL1VEC = 0x00;

    sei();
}


#endif // USB_CDC_ACM_H
