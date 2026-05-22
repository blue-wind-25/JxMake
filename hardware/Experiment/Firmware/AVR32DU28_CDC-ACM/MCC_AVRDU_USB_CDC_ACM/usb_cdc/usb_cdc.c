/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCDC CDC Source File
 * This file contains implementation for CDC
 * USB Device Stack Driver Version 1.0.0
 */

/*
    (C) 2021 Microchip Technology Inc. and its subsidiaries.

    Subject to your compliance with these terms, you may use Microchip software and any
    derivatives exclusively with Microchip products. It is your responsibility to comply with third party
    license terms applicable to your use of third party software (including open source software) that
    may accompany Microchip software.

    THIS SOFTWARE IS SUPPLIED BY MICROCHIP "AS IS". NO WARRANTIES, WHETHER
    EXPRESS, IMPLIED OR STATUTORY, APPLY TO THIS SOFTWARE, INCLUDING ANY
    IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY, AND FITNESS
    FOR A PARTICULAR PURPOSE.

    IN NO EVENT WILL MICROCHIP BE LIABLE FOR ANY INDIRECT, SPECIAL, PUNITIVE,
    INCIDENTAL OR CONSEQUENTIAL LOSS, DAMAGE, COST OR EXPENSE OF ANY KIND
    WHATSOEVER RELATED TO THE SOFTWARE, HOWEVER CAUSED, EVEN IF MICROCHIP
    HAS BEEN ADVISED OF THE POSSIBILITY OR THE DAMAGES ARE FORESEEABLE. TO
    THE FULLEST EXTENT ALLOWED BY LAW, MICROCHIP'S TOTAL LIABILITY ON ALL
    CLAIMS IN ANY WAY RELATED TO THIS SOFTWARE WILL NOT EXCEED THE AMOUNT
    OF FEES, IF ANY, THAT YOU HAVE PAID DIRECTLY TO MICROCHIP FOR THIS
    SOFTWARE.
 */


#include <stddef.h>

#include "usb_cdc.h"
#include "usb_cdc_virtual_serial_port.h"


// Line state and setup
volatile uint16_t                          usbCDCControlLineState;
volatile USB_CDC_LINE_CODING_t             usbCDCLineCoding;

volatile uint16_t                          usbCdcBreakDuration         = 0;
volatile bool                              usbCdcBreakActive           = false;

volatile USB_SETUP_ENDOFREQUEST_CALLBACK_t usbCdcSetLineCodingCallback = NULL;

// USB Pipes
USB_PIPE_t CDCTxPipe = {
    .address   = USB_CDC_BULK_EP_IN,
    .direction = USB_EP_DIR_IN,
};

USB_PIPE_t CDCRxPipe = {
    .address   = USB_CDC_BULK_EP_OUT,
    .direction = USB_EP_DIR_OUT,
};

// RX Buffer
uint8_t           usbCDCReceiveTempBuffer[USB_CDC_RX_PACKET_SIZE] __attribute__( (aligned( 2 ) ) );
static uint8_t    usbCDCReceiveArray[USB_CDC_RX_BUFFER_SIZE];
CIRCULAR_BUFFER_t usbCDCReceiveBuffer = {
    .content   = usbCDCReceiveArray,
    .head      =                      0,
    .tail      =                      0,
    .maxLength = USB_CDC_RX_BUFFER_SIZE,
};

// TX Buffer
static uint8_t    usbCDCTransmitArray[USB_CDC_TX_BUFFER_SIZE];
CIRCULAR_BUFFER_t usbCDCTransmitBuffer = {
    .content   = usbCDCTransmitArray,
    .head      =                      0,
    .tail      =                      0,
    .maxLength = USB_CDC_TX_BUFFER_SIZE,
};

void USB_CDCDataReceived( USB_PIPE_t pipe, USB_TRANSFER_STATUS_t status, uint16_t bytesTransferred )
{
    (void) (pipe);

    if( USB_PIPE_TRANSFER_OK == status )
        for( uint16_t i = 0; i < bytesTransferred; i++ ) CIRCBUF_Enqueue( &usbCDCReceiveBuffer, usbCDCReceiveTempBuffer[i] );
}

void USB_CDCDataTransmitted( USB_PIPE_t pipe, USB_TRANSFER_STATUS_t status, uint16_t bytesTransferred )
{
    (void) (pipe);
    (void) (bytesTransferred);

    if( USB_PIPE_TRANSFER_OK == status ) {
        usbCDCTransmitBuffer.head = 0;
        usbCDCTransmitBuffer.tail = 0;
    }
}
