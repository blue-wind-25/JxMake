/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREREQUESTSENDPOINT USB Core Requests Endpoint Source  File
 * usb_core_requests_endpoint.c
 * usb_core_requests
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


#include "../usb_peripheral/usb_peripheral.h"
#include "usb_core_requests_endpoint.h"
#include "usb_core_transfer.h"


/*
 * usb_core_requests
 * GET_STATUS_ENDPOINT_STALLED
 * Mask for the endpoint stall status in the first byte of the data stage of the setup request.
 */
#define GET_STATUS_ENDPOINT_STALLED (1u << 0u)

/*
 * usb_core_requests
 * ENDPOINT_ADDRESS_MASK
 * Mask for the endpoint address in the wIndex field of the setup request.
 */
#define ENDPOINT_ADDRESS_MASK (0x7fu)

/*
 * usb_core_requests
 * ENDPOINT_DIRECTION_BITPOSITION
 * Bit position for the endpoint direction in the wIndex field of the setup request.
 */
#define ENDPOINT_DIRECTION_BITPOSITION (7u)

USB_PIPE_t EndpointFromRequestGet(uint16_t wIndex)
{
    USB_PIPE_t endpoint;
    endpoint.address = (uint8_t)wIndex & ENDPOINT_ADDRESS_MASK;
    endpoint.direction = (uint8_t)wIndex >> ENDPOINT_DIRECTION_BITPOSITION;

    return endpoint;
}

RETURN_CODE_t SetupEndpointRequestGetStatus(USB_SETUP_REQUEST_t *setupRequestPtr)
{
    // Return IN transaction with ENDPOINT_HALT status (2 bytes)
    USB_PIPE_t endpoint = EndpointFromRequestGet(setupRequestPtr->wIndex);

    uint8_t data[] = {0, 0};
    if (USB_EndpointIsStalled(endpoint) == true)
    {

        data[0] |= GET_STATUS_ENDPOINT_STALLED;
    }

    return USB_ControlTransferDataWriteBuffer(data, sizeof (data));
}

RETURN_CODE_t SetupEndpointRequestClearFeature(USB_SETUP_REQUEST_t *setupRequestPtr)
{
    if (setupRequestPtr->wValue == USB_ENDPOINT_FEATURE_HALT)
    {
        USB_EndpointStallClear(EndpointFromRequestGet(setupRequestPtr->wIndex));
        return SUCCESS;
    }
    return UNSUPPORTED;
}

RETURN_CODE_t SetupEndpointRequestSetFeature(USB_SETUP_REQUEST_t *setupRequestPtr)
{
    if (setupRequestPtr->wValue == USB_ENDPOINT_FEATURE_HALT)
    {
        USB_PIPE_t endpoint = EndpointFromRequestGet(setupRequestPtr->wIndex);
        USB_TransferAbort(endpoint);
        USB_EndpointStall(endpoint);
        return SUCCESS;
    }
    return UNSUPPORTED;
}
