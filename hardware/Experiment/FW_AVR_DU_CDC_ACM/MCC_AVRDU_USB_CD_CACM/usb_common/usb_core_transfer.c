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


#include <avr/interrupt.h>

#include "usb_core_transfer.h"


ISR(USB0_TRNCOMPL_vect)
{
    USB_TransferHandler();
}


RETURN_CODE_t USB_TransferWriteStart(USB_PIPE_t pipe, uint8_t *dataPtr, uint16_t dataSize, USB_TRANSFER_END_CALLBACK_t callback)
{
    if (USB_PipeStatusIsBusy(pipe) == true)
    {
        return PIPE_BUSY_ERROR;
    }
    USB_PipeReset(pipe);
    USB_PipeDataPtrSet(pipe, dataPtr);
    USB_PipeDataToTransferSizeSet(pipe, dataSize);
    USB_PipeDataTransferredSizeReset(pipe);
    USB_PipeTransferZLP_Enable(pipe);
    USB_PipeTransferEndCallbackRegister(pipe, callback);
    USB_InTransactionRun(pipe);
    return SUCCESS;
}

RETURN_CODE_t USB_TransferReadStart(USB_PIPE_t pipe, uint8_t *dataPtr, uint16_t dataSize, USB_TRANSFER_END_CALLBACK_t callback)
{
    if (USB_PipeStatusIsBusy(pipe) == true)
    {
        return PIPE_BUSY_ERROR;
    }
    USB_PipeReset(pipe);
    USB_PipeDataPtrSet(pipe, dataPtr);
    USB_PipeDataToTransferSizeSet(pipe, dataSize);
    USB_PipeDataTransferredSizeReset(pipe);
    USB_PipeTransferEndCallbackRegister(pipe, callback);
    USB_OutTransactionRun(pipe);
    return SUCCESS;
}

RETURN_CODE_t USB_TransferControlDataSet(uint8_t *dataPtr, uint16_t dataSize, USB_SETUP_ENDOFREQUEST_CALLBACK_t callback)
{
    USB_ControlEndOfRequestCallbackRegister(callback);
    return USB_ControlTransferDataSet(dataPtr, dataSize);
}

void USB_TransferAbort(USB_PIPE_t pipe)
{
    if (USB_PipeStatusIsBusy(pipe) == true)
    {
        USB_TransactionAbort(pipe);
        USB_PipeTransferEndCallback(pipe);
    }
}

void USB_TransferHandler(void)
{
    // If it's the initial setup packet, handle that separately.
    if (USB_SetupIsReceived() == true)
    {
        USB_ControlSetupReceived();
    }
    // If a transaction is complete, handle that one.
    else if (USB_TransactionIsCompleted() == true)
    {
        USB_PIPE_t pipe;
        if (SUCCESS == USB_TransactionCompletedPipeGet(&pipe))
        {
            USB_TransactionCompleteAck(pipe);
            if (pipe.address == 0U)
            {
                USB_ControlTransactionComplete(pipe);
            }
            else
            {
                USB_PipeTransactionComplete(pipe);
            }
        }
    }
}
