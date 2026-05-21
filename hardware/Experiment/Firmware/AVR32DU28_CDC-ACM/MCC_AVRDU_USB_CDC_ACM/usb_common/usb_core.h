/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCORE CORE Header File
 * Core functionality for the USB stack.
 * USB Device Core Version 1.0.0
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


#ifndef USB_CORE_H
#define USB_CORE_H


#include <stddef.h>

#include "usb_core_transfer.h"
#include "../usb_cdc/usb_cdc.h"


static inline RETURN_CODE_t USB_SetupProcess( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    RETURN_CODE_t status = UNINITIALIZED;
    if( USB_REQUEST_TYPE_STANDARD == (USB_REQUEST_TYPE_t) setupRequestPtr->bmRequestType.type ) {
        if( (USB_REQUEST_DIR_IN == (USB_REQUEST_DIR_t) setupRequestPtr->bmRequestType.dataPhaseTransferDirection) && (0u == setupRequestPtr->wLength) ) {
            status = CONTROL_SETUP_DIRECTION_ERROR;
        }
        else {
            USB_ControlTransferDataSet( NULL, 0u );
            switch( setupRequestPtr->bmRequestType.recipient ) {
                case USB_REQUEST_RECIPIENT_DEVICE:
                    status = USB_SetupProcessDeviceRequest( setupRequestPtr );
                    break;

                case USB_REQUEST_RECIPIENT_ENDPOINT:
                    status = USB_SetupProcessEndpointRequest( setupRequestPtr );
                    break;

                case USB_REQUEST_RECIPIENT_INTERFACE:
                    status = USB_SetupProcessInterfaceRequest( setupRequestPtr );
                    break;

                default:
                    status = UNSUPPORTED;
                    break;
            } // switch
        }
    }
    else if( USB_REQUEST_TYPE_CLASS == (USB_REQUEST_TYPE_t) setupRequestPtr->bmRequestType.type ) {
        status = USB_CDCRequestHandler( setupRequestPtr );
    }
    else {
        status = UNSUPPORTED;
    }
    return status;
}

static inline void USB_Start()
{
    USB_PeripheralInitialize();
    USB_ControlEndpointsInit();
    USB_ControlTransferReset();
    USB_BusAttach();
}

static inline void USB_Stop( void )
{
    USB_BusDetach();
    USB_PeripheralDisable();
    USB_PIPE_t pipe;
    pipe.address   = 0;
    pipe.direction = 0;
    while( pipe.address < USB_EP_NUM ) {
        pipe.direction = USB_EP_DIR_OUT; USB_TransferAbort( pipe );
        pipe.direction = USB_EP_DIR_IN;  USB_TransferAbort( pipe );
        pipe.address++;
    }
}

static inline void USB_Reset( void )
{
    USB_Stop();
    USB_Start();
}


#endif // USB_CORE_H
