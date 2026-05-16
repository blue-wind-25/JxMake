/**
 * USBCOREEVENTS USB Core Events Source File
 * @file usb_core_events.h
 * @ingroup usb_core_events
 * @brief Event handling for the USB Core Stack.
 * @version USB Device Core Version 1.0.0
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
#include <usb_core_events.h>
#include <usb_peripheral.h>
#include <usb_protocol_headers.h>

RETURN_CODE_t USB_EventHandler(void)
{
    RETURN_CODE_t status = SUCCESS;

    if (USB_EventResetIsReceived() == true)
    {
        USB_EventResetClear();
        USB_PIPE_t pipe = { .address = 0 };
        while (pipe.address < USB_EP_NUM)
        {
            pipe.direction = USB_EP_DIR_IN;
            if (SUCCESS == status)
            {
                status = USB_TransferAbort(pipe);
            }
            pipe.direction = USB_EP_DIR_OUT;
            if (SUCCESS == status)
            {
                status = USB_TransferAbort(pipe);
            }
            pipe.address++;
        }
        status = USB_Reset();
    }
    uint8_t eventOverUnderflow = USB_EventOverUnderflowIsReceived();
    if (0u < eventOverUnderflow)
    {
        USB_EventOverUnderflowClear();
        uint8_t controlOverUnderflow = USB_ControlOverUnderflowIsReceived();
        if (0u < controlOverUnderflow)
        {
            status = USB_ControlProcessOverUnderflow(controlOverUnderflow);
        }
        else
        {
            status = SUCCESS;
        }
    }
    if (USB_EventStalledIsReceived() == true)
    {
        USB_EventStalledClear();
        USB_PIPE_t pipe = { .address = 0x00, .direction = USB_EP_DIR_OUT };
        status = USB_HandleEventStalled(pipe);
    }
    return status;
}
