/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREREQUESTS USB Core Requests Header File
 * USB Device Core Requests handling.
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


#ifndef USB_CORE_REQUESTS_H
#define USB_CORE_REQUESTS_H


#include "usb_core_requests_device.h"
#include "usb_core_requests_endpoint.h"
#include "usb_core_requests_interface.h"


static inline RETURN_CODE_t USB_SetupProcessDeviceRequest( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    RETURN_CODE_t status = UNINITIALIZED;
    switch( setupRequestPtr->bRequest ) {
        case USB_REQUEST_GET_STATUS:
            status = SetupDeviceRequestGetStatus();
            break;

        case USB_REQUEST_CLEAR_FEATURE:
        case USB_REQUEST_SET_FEATURE:
        case USB_REQUEST_SET_DESCRIPTOR:
            status = UNSUPPORTED;
            break;

        case USB_REQUEST_SET_ADDRESS:
            status = SetupDeviceRequestSetAddress( (uint8_t) setupRequestPtr->wValue & 0xFFu );
            break;

        case USB_REQUEST_GET_DESCRIPTOR:
            status = SetupDeviceRequestGetDescriptor( setupRequestPtr );
            break;

        case USB_REQUEST_GET_CONFIGURATION:
            status = SetupDeviceRequestGetConfiguration();
            break;

        case USB_REQUEST_SET_CONFIGURATION:
            status = SetupDeviceRequestSetConfiguration( (uint8_t) ( setupRequestPtr->wValue & 0xFFu ) );
            break;

        default:
            status = UNSUPPORTED;
            break;
    } // switch
    return status;
}

static inline RETURN_CODE_t USB_SetupProcessEndpointRequest( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    RETURN_CODE_t status = UNINITIALIZED;
    USB_PIPE_t    endpoint = EndpointFromRequestGet( setupRequestPtr->wIndex );
    if( endpoint.address >= (uint8_t) USB_EP_NUM ) {
        status = ENDPOINT_ADDRESS_ERROR;
    }
    else {
        switch( setupRequestPtr->bRequest ) {
            case USB_REQUEST_GET_STATUS:
                status = SetupEndpointRequestGetStatus( setupRequestPtr );
                break;

            case USB_REQUEST_CLEAR_FEATURE:
                status = SetupEndpointRequestClearFeature( setupRequestPtr );
                break;

            case USB_REQUEST_SET_FEATURE:
                status = SetupEndpointRequestSetFeature( setupRequestPtr );
                break;

            default:
                status = UNSUPPORTED;
                break;
        } // switch
    }
    return status;
}

static inline RETURN_CODE_t USB_SetupProcessInterfaceRequest( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    RETURN_CODE_t status = UNINITIALIZED;
    switch( setupRequestPtr->bRequest ) {
        case USB_REQUEST_GET_STATUS:
            status = USB_SetupInterfaceRequestGetStatus();
            break;

        case USB_REQUEST_CLEAR_FEATURE:
        case USB_REQUEST_SET_FEATURE:
        case USB_REQUEST_GET_DESCRIPTOR:
            status = UNSUPPORTED;
            break;

        case USB_REQUEST_GET_INTERFACE:
            status = USB_SetupInterfaceRequestGetInterface( setupRequestPtr );
            break;

        case USB_REQUEST_SET_INTERFACE:
            status = USB_SetupInterfaceRequestSetInterface( setupRequestPtr );
            break;

        default:
            status = UNSUPPORTED;
            break;
    } // switch
    return status;
}


#endif // USB_CORE_REQUESTS_H
