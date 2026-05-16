/*
 * USBCORE CORE Source File
 * usb_core.h
 * usb_core
 * Core functionality for the USB stack.
 * USB Device Core Version 1.0.0
 */

/*
    (c) 2021 Microchip Technology Inc. and its subsidiaries.

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

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>

#include <usb_common_elements.h>
#include <usb_config.h>
#include <usb_core.h>
#include <usb_core_descriptors.h>
#include <usb_core_events.h>
#include <usb_core_requests.h>
#include <usb_core_transfer.h>
#include <usb_peripheral.h>
#include <usb_protocol_headers.h>
#include <usb_cdc.h>

RETURN_CODE_t USB_SetupProcess(USB_SETUP_REQUEST_t *setupRequestPtr)
{
    RETURN_CODE_t status = UNINITIALIZED;

    if (USB_REQUEST_TYPE_STANDARD == (USB_REQUEST_TYPE_t)setupRequestPtr->bmRequestType.type)
    {
        // Checks that an IN request actually requests data.
        if ((USB_REQUEST_DIR_IN == (USB_REQUEST_DIR_t)setupRequestPtr->bmRequestType.dataPhaseTransferDirection) && (0u == setupRequestPtr->wLength))
        {
            status = CONTROL_SETUP_DIRECTION_ERROR;
        }
        else
        {
            // Makes sure the data out transfer is reset before handling requests.
            USB_ControlTransferDataSet(NULL, 0u);

            switch (setupRequestPtr->bmRequestType.recipient)
            {
            case USB_REQUEST_RECIPIENT_DEVICE:
            {
                status = USB_SetupProcessDeviceRequest(setupRequestPtr);
                break;
            }
            case USB_REQUEST_RECIPIENT_ENDPOINT:
            {
                status = USB_SetupProcessEndpointRequest(setupRequestPtr);
                break;
            }
            case USB_REQUEST_RECIPIENT_INTERFACE:
            {
                status = USB_SetupProcessInterfaceRequest(setupRequestPtr);
                break;
            }
            default:
                status = UNSUPPORTED;
                break;
            }
        }
    }
    else if (USB_REQUEST_TYPE_CLASS == (USB_REQUEST_TYPE_t)setupRequestPtr->bmRequestType.type)
    {
        status = USB_CDCRequestHandler(setupRequestPtr);
    }
    else
    {
        status = UNSUPPORTED;
    }

    return status;
}

RETURN_CODE_t USB_Stop(void)
{
    RETURN_CODE_t status = SUCCESS;

    // Detaches from the bus and disables peripheral.
    USB_BusDetach();
    USB_PeripheralDisable();

    // Aborts any ongoing transfers.
    USB_PIPE_t pipe = { .address = 0 };
    while (pipe.address < USB_EP_NUM)
    {
        if (status == SUCCESS)
        {
            pipe.direction = USB_EP_DIR_OUT;
            status = USB_TransferAbort(pipe);
        }
        if (status == SUCCESS)
        {
            pipe.direction = USB_EP_DIR_IN;
            status = USB_TransferAbort(pipe);
        }
        pipe.address++;
    }

    return status;
}

RETURN_CODE_t USB_Reset(void)
{
    RETURN_CODE_t status = USB_Stop();
    if (status == SUCCESS)
    {
        USB_PeripheralInitialize();
        status = USB_ControlEndpointsInit();
    }
    if (status == SUCCESS)
    {
        status = USB_ControlTransferReset();
    }
    if (status == SUCCESS)
    {
        USB_BusAttach();
    }
    return status;
}
