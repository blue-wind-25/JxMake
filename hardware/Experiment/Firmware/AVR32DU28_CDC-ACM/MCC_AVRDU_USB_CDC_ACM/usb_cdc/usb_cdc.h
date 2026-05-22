/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCDC CDC Header File
 * This file contains prototypes and data types for a CDC application
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


#ifndef USB_CDC_H
#define USB_CDC_H


#include "../usb_common/usb_protocol_headers.h"
#include "../usb_cdc/usb_protocol_cdc.h"
#include "../usb_common/usb_core_transfer.h"


extern volatile uint16_t                          usbCDCControlLineState;
extern volatile USB_CDC_LINE_CODING_t             usbCDCLineCoding;

extern volatile uint16_t                          usbCdcBreakDuration;
extern volatile bool                              usbCdcBreakActive;

extern volatile USB_SETUP_ENDOFREQUEST_CALLBACK_t usbCdcSetLineCodingCallback;

static inline RETURN_CODE_t USB_CDCRequestHandler( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    RETURN_CODE_t status = UNINITIALIZED;

    if( USB_REQUEST_RECIPIENT_INTERFACE == (USB_REQUEST_RECIPIENT_t) setupRequestPtr->bmRequestType.recipient ) {
        if( USB_REQUEST_DIR_IN == setupRequestPtr->bmRequestType.dataPhaseTransferDirection ) {
            switch( setupRequestPtr->bRequest ) {
                case USB_CDC_REQUEST_GET_LINE_CODING:
                    status = USB_TransferControlDataSet( (uint8_t*) (volatile void*) &usbCDCLineCoding, sizeof(USB_CDC_LINE_CODING_t), NULL );
                    break;

                default:
                    status = UNSUPPORTED;
                    break;
            } // switch
        }
        else {
            switch( setupRequestPtr->bRequest ) {
                case USB_CDC_REQUEST_SET_LINE_CODING:
                    status = USB_TransferControlDataSet( (uint8_t*) (volatile void*) &usbCDCLineCoding, sizeof(USB_CDC_LINE_CODING_t), usbCdcSetLineCodingCallback );
                    break;

                case USB_CDC_REQUEST_SET_CONTROL_LINE_STATE:
                    usbCDCControlLineState = setupRequestPtr->wValue;
                    status                 = SUCCESS;
                    break;

                case USB_CDC_REQUEST_SEND_BREAK:
                    usbCdcBreakDuration = setupRequestPtr->wValue;
                    if(usbCdcBreakDuration > 0) usbCdcBreakActive = true;
                    else                        usbCdcBreakActive = false;
                    status = SUCCESS;
                    break;

                default:
                    status = UNSUPPORTED;
                    break;
            } // switch
        }
    }
    else {
        status = UNSUPPORTED;
    }

    return status;
}

static inline RETURN_CODE_t USB_CDCSendSerialState( uint16_t uartStateBitmap )
{
    // Use static because buffer must outlive the async DMA transfer
    static uint8_t packet[10] = {
        0xA1,                       // bmRequestType: Notification, Class, Interface
        0x20,                       // bNotification: SERIAL_STATE
        0x00,                       // wValue  LSB
        0x00,                       // wValue  MSB
        USB_CDC_COMM_INTERFACE_NUM, // wIndex  LSB (communication interface)
        0x00,                       // wIndex  MSB
        0x02,                       // wLength LSB
        0x00,                       // wLength MSB
        0x00,                       // uartStateBitmap LSB (filled below)
        0x00,                       // uartStateBitmap MSB (filled below)
    };

    USB_PIPE_t notifPipe = {
        .address   = USB_CDC_INTERRUPT_EP,
        .direction = USB_EP_DIR_IN,
    };

    // Guard - do not touch the static buffer while a prior transfer is still in flight
    if( USB_PipeStatusIsBusy( notifPipe ) ) return UNINITIALIZED;

    packet[8] = (uint8_t) (uartStateBitmap & 0xFF);
    packet[9] = (uint8_t) (uartStateBitmap >> 8);

    return USB_TransferWriteStart( notifPipe, packet, sizeof(packet), NULL );
}


#endif // USB_CDC_H
