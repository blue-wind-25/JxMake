/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREREQUESTSENDPOINT USB Core Requests Endpoint Header File
 * USB Endpoint Core Requests handling.
 * USB Device Core Version USB 1.0.0
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


#ifndef USB_CORE_REQUESTS_ENDPOINT_H
#define USB_CORE_REQUESTS_ENDPOINT_H


#include "../usb_peripheral/usb_peripheral.h"

// Forward declaration to avoid circular include with usb_core_transfer.h
void USB_TransferAbort( USB_PIPE_t pipe );

#define GET_STATUS_ENDPOINT_STALLED ( 1u << 0u )

/*
 * Gets the endpoint pipe from the wIndex field of the setup request.
 *     wIndex - Endpoint address and direction
 * return A structure with the endpoint address and direction
 */
static inline USB_PIPE_t EndpointFromRequestGet( uint16_t wIndex )
{
    USB_PIPE_t endpoint;
    endpoint.address   = (uint8_t) wIndex & 0x7Fu;
    endpoint.direction = (uint8_t) wIndex >> 7u;
    return endpoint;
}

/*
 * Gets the endpoint status.
 *     *setupRequestPtr - Pointer to the setup request
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
static inline RETURN_CODE_t SetupEndpointRequestGetStatus( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    USB_PIPE_t endpoint = EndpointFromRequestGet( setupRequestPtr->wIndex );
    uint8_t    data[]   = { 0, 0 };
    if( USB_EndpointIsStalled( endpoint ) == true ) data[0] |= GET_STATUS_ENDPOINT_STALLED;
    return USB_ControlTransferDataWriteBuffer( data, sizeof( data ) );
}

/*
 * Clears the endpoint feature.
 *     *setupRequestPtr - Pointer to the setup request
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
static inline RETURN_CODE_t SetupEndpointRequestClearFeature( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    if( setupRequestPtr->wValue == USB_ENDPOINT_FEATURE_HALT ) {
        USB_EndpointStallClear( EndpointFromRequestGet( setupRequestPtr->wIndex ) );
        return SUCCESS;
    }
    return UNSUPPORTED;
}

/*
 * Sets the endpoint feature.
 *     *setupRequestPtr - Pointer to the setup request
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
static inline RETURN_CODE_t SetupEndpointRequestSetFeature( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    if( setupRequestPtr->wValue == USB_ENDPOINT_FEATURE_HALT ) {
        USB_PIPE_t endpoint = EndpointFromRequestGet( setupRequestPtr->wIndex );
        USB_TransferAbort( endpoint );
        USB_EndpointStall( endpoint );
        return SUCCESS;
    }
    return UNSUPPORTED;
}


#endif // USB_CORE_REQUESTS_ENDPOINT_H
