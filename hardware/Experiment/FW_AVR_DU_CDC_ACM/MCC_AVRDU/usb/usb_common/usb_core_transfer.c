/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCORETRANSFER USB Core Transfer Source File
 * usb_core_transfer.c
 * usb_core_transfer
 * USB core layer implementation file.
 * USB Core Version 1.0.0
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
#include <stdint.h>
#include <stddef.h>
#include <string.h>

#include <usb_core.h>
#include <usb_protocol_headers.h>
#include <usb_common_elements.h>
#include <usb_config.h>
#include <usb_peripheral.h>

RETURN_CODE_t USB_TransferWriteStart(USB_PIPE_t pipe, uint8_t *dataPtr, uint16_t dataSize, USB_TRANSFER_END_CALLBACK_t callback)
{
    RETURN_CODE_t status = UNINITIALIZED;

    if (USB_PipeStatusIsBusy(pipe) == true)
    {
        status = PIPE_BUSY_ERROR;
    }
    else
    {
        status = USB_PipeReset(pipe);
    }

    if (status == SUCCESS)
    {
        USB_PipeDataPtrSet(pipe, dataPtr);
        USB_PipeDataToTransferSizeSet(pipe, dataSize);
        USB_PipeDataTransferredSizeReset(pipe);
        USB_PipeTransferZLP_Enable(pipe);
        USB_PipeTransferEndCallbackRegister(pipe, callback);
        status = USB_InTransactionRun(pipe);
    }
    return status;
}

RETURN_CODE_t USB_TransferReadStart(USB_PIPE_t pipe, uint8_t *dataPtr, uint16_t dataSize, USB_TRANSFER_END_CALLBACK_t callback)
{
    RETURN_CODE_t status = UNINITIALIZED;

    if (USB_PipeStatusIsBusy(pipe) == true)
    {
        status = PIPE_BUSY_ERROR;
    }
    else
    {
        status = USB_PipeReset(pipe);
    }

    if (status == SUCCESS)
    {
        USB_PipeDataPtrSet(pipe, dataPtr);
        USB_PipeDataToTransferSizeSet(pipe, dataSize);
        USB_PipeDataTransferredSizeReset(pipe);
        USB_PipeTransferEndCallbackRegister(pipe, callback);
        status = USB_OutTransactionRun(pipe);
    }

    return status;
}

RETURN_CODE_t USB_TransferControlDataSet(uint8_t *dataPtr, uint16_t dataSize, USB_SETUP_ENDOFREQUEST_CALLBACK_t callback)
{
    USB_ControlEndOfRequestCallbackRegister(callback);
    return USB_ControlTransferDataSet(dataPtr, dataSize);
}

RETURN_CODE_t USB_TransferAbort(USB_PIPE_t pipe)
{
    if (USB_PipeStatusIsBusy(pipe) == true)
    {
        USB_TransactionAbort(pipe);
        USB_PipeTransferEndCallback(pipe);
    }
    return SUCCESS;
}

RETURN_CODE_t USB_TransferHandler(void)
{
    RETURN_CODE_t status = UNINITIALIZED;

    // If it's the initial setup packet, handle that separately.
    if (USB_SetupIsReceived() == true)
    {
        status = USB_ControlSetupReceived();
    }
    // If a transaction is complete, handle that one.
    else if (USB_TransactionIsCompleted() == true)
    {
        // Finds out which pipe has a completed transaction.
        USB_PIPE_t pipe;

        status = USB_TransactionCompletedPipeGet(&pipe);

        if (status == SUCCESS)
        {
            // Acks the transaction.
            status = USB_TransactionCompleteAck(pipe);
        }

        if (status == SUCCESS)
        {
            // Handles control transactions separately.
            if (pipe.address == 0U)
            {
                status = USB_ControlTransactionComplete(pipe);
            }
            else
            {
                // Regular handling of all regular endpoints.
                status = USB_PipeTransactionComplete(pipe);
            }
        }
    }
    else
    {
        // No handling needed, return SUCCESS.
        status = SUCCESS;
    }

    return status;
}
