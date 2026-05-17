/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBPERIPHERAL Peripheral Source File
 * usb_peripheral.c
 * usb_peripheral
 * Interface for a usb_peripheral module that needs to be implemented by a device specific USB module driver.
 * USB Device Stack HAL Driver Version 1.0.0
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
#include <string.h>

#include "usb_peripheral.h"
#include "usb_peripheral_avr_du.h"


RETURN_CODE_t USB_SetupProcess(USB_SETUP_REQUEST_t *setupRequestPtr);

STATIC USB_CONTROL_TRANSFER_t controlTransfer __attribute__((aligned(2))) = { .transferDataPtr = controlTransfer.buffer };

bool USB_SetupIsReceived(void)
{
    return USB_SetupInterruptIs();
}

bool USB_EventSOFIsReceived(void)
{
    return USB_SOFInterruptIs();
}

void USB_EventSOFClear(void)
{
    USB_SOFInterruptClear();
}

bool USB_EventResetIsReceived(void)
{
    return USB_ResetInterruptIs();
}

void USB_EventResetClear(void)
{
    USB_ResetInterruptClear();
}

uint8_t USB_EventOverUnderflowIsReceived(void)
{
    uint8_t eventOverUnderflow = 0;
    if (USB_OverflowInterruptIs() == true)
    {
        eventOverUnderflow |= (uint8_t)OVERFLOW_EVENT;
    }
    if (USB_UnderflowInterruptIs() == true)
    {
        eventOverUnderflow |= (uint8_t)UNDERFLOW_EVENT;
    }
    return eventOverUnderflow;
}

uint8_t USB_ControlOverUnderflowIsReceived(void)
{
    uint8_t eventOverUnderflow = 0;
    if (USB_EndpointOutOverUnderflowIsSet(0) == true)
    {
        eventOverUnderflow |= (uint8_t)OVERFLOW_EVENT;
        USB_EndpointOutOverUnderflowAck(0);
    }
    if (USB_EndpointInOverUnderflowIsSet(0) == true)
    {
        eventOverUnderflow |= (uint8_t)UNDERFLOW_EVENT;
        USB_EndpointInOverUnderflowAck(0);
    }
    return eventOverUnderflow;
}

void USB_EventOverUnderflowClear(void)
{
    USB_OverflowInterruptClear();
    USB_UnderflowInterruptClear();
}

bool USB_EventSuspendIsReceived(void)
{
    return USB_SuspendInterruptIs();
}

void USB_EventSuspendClear(void)
{
    USB_SuspendInterruptClear();
}

bool USB_EventResumeIsReceived(void)
{
    return USB_ResumeInterruptIs();
}

void USB_EventResumeClear(void)
{
    USB_ResumeInterruptClear();
}

bool USB_EventStalledIsReceived(void)
{
    return USB_StalledInterruptIs();
}

void USB_EventStalledClear(void)
{
    USB_StalledInterruptClear();
}

void USB_BusAttach(void)
{
    USB_ConnectionAttach();
}

void USB_BusDetach(void)
{
    USB_ConnectionDetach();
}

bool USB_IsBusAttached(void)
{
    return USB_ConnectionIsAttach();
}

void USB_DeviceAddressConfigure(uint8_t deviceAddress)
{
    USB_DeviceAddressSet(deviceAddress);
}

uint16_t USB_FrameNumberGet(void)
{
    return (USB_FrameNumGet());
}

void USB_ControlEndpointsInit(void)
{
    USB_PIPE_t controlPipeOut = { .address = 0, .direction = USB_EP_DIR_OUT };
    USB_PIPE_t controlPipeIn = { .address = 0, .direction = USB_EP_DIR_IN };

    USB_EndpointConfigure(controlPipeOut, USB_EP0_SIZE, CONTROL);
    USB_EndpointConfigure(controlPipeIn, USB_EP0_SIZE, CONTROL);
    EndpointBufferSet(controlPipeOut, controlTransfer.buffer);
    EndpointBufferSet(controlPipeIn, controlTransfer.buffer);
    USB_DataToggleClear(controlPipeOut);
    USB_DataToggleSet(controlPipeIn);
    controlTransfer.status = USB_CONTROL_SETUP;
}

void USB_ControlSetupReceived(void)
{
    USB_SetupInterruptClear();

    if (USB_CONTROL_STALL_REQ == controlTransfer.status)
    {
        // Stall events are handled by the EventHandler.
        return;
    }

    // Acks Setup Received on the control endpoints.
    USB_EndpointOutSetupReceivedAck(0u);
    USB_EndpointInSetupCompleteAck(0u);

    // Clears bytes received and sent
    USB_NumberBytesToSendReset(0u);
    USB_NumberBytesSentReset(0u);
    USB_NumberBytesToReceiveReset(0u);
    USB_NumberBytesReceivedReset(0u);

    // Copies setup packet out of buffer to make it available for a data stage.
    (void)memcpy((uint8_t *)(&controlTransfer.setupRequest), controlTransfer.buffer, sizeof(USB_SETUP_REQUEST_t));

    RETURN_CODE_t setup_status = USB_SetupProcess(&controlTransfer.setupRequest);

    if (UNSUPPORTED == setup_status)
    {
        // Setup Request unknown or rejected, stalls the next control transaction.
        controlTransfer.status = USB_CONTROL_STALL_REQ;
        USB_EndpointInStall(0u);
        USB_EndpointOutStall(0u);
    }
    else if (SUCCESS == setup_status)
    {
        if (0u == controlTransfer.transferDataSize)
        {
            // Request did not contain a data stage, sends ZLP directly.
            USB_ControlTransferZLP(USB_EP_DIR_IN);
        }
        else
        {
            // Sends or Receives data in next stage of request.
            controlTransfer.totalBytesTransferred = 0;
            USB_PIPE_t controlPipe = { .address = 0u, .direction = USB_EP_DIR_IN };
            if ((controlTransfer.setupRequest.bmRequestType.dataPhaseTransferDirection) == USB_REQUEST_DIR_IN)
            {
                controlTransfer.status = USB_CONTROL_DATA_IN;
            }
            else
            {
                // Control OUT data transactions are controlled by the IN.DATAPTR so set specifically here.
                EndpointBufferSet(controlPipe, controlTransfer.transferDataPtr);

                controlPipe.direction = USB_EP_DIR_OUT;
                controlTransfer.status = USB_CONTROL_DATA_OUT;
            }

            // Sets up the pipe variables.
            USB_PipeDataTransferredSizeReset(controlPipe);
            USB_PipeDataPtrSet(controlPipe, controlTransfer.transferDataPtr);
            USB_PipeDataToTransferSizeSet(controlPipe, controlTransfer.transferDataSize);

            // Start data stage transaction.
            USB_ControlTransactionComplete(controlPipe);
        }
    }
    // else: setup error — no action, stall will occur on next transaction
}

void USB_ControlTransactionComplete(USB_PIPE_t pipe)
{
    // Always called with pipe.address == 0 (control endpoint)
    USB_DataToggleSet(pipe);

    switch (controlTransfer.status)
    {
        case USB_CONTROL_DATA_IN:
        {
            pipe.direction = USB_EP_DIR_IN;

            // Updates bytes sent and to be sent.
            uint16_t bytesSent = USB_PipeDataTransferredSizeGet(pipe);
            bytesSent += USB_NumberBytesSentGet(pipe.address);
            USB_PipeDataTransferredSizeSet(pipe, bytesSent);
            uint16_t transferDataSize = USB_PipeDataToTransferSizeGet(pipe);

            // Checks remaining data to send.
            if (0U == (transferDataSize - bytesSent))
            {
                controlTransfer.totalBytesTransferred += bytesSent;
                if (controlTransfer.transferDataSize == controlTransfer.totalBytesTransferred)
                {
                    // Data stage is complete, sends an OUT ZLP for status stage.
                    USB_ControlTransferZLP(USB_REQUEST_DIR_OUT);
                }
                else
                {
                    // No overUnderRunCallback registered — send ZLP for data stage.
                    USB_InTransactionRun(pipe);
                }
            }
            else
            {
                // Starts next transaction in data stage.
                USB_InTransactionRun(pipe);
            }

            USB_EndpointInOverUnderflowAck(0);
            break;
        }
        case USB_CONTROL_DATA_OUT:
        {
            pipe.direction = USB_EP_DIR_OUT;

            // Updates bytes received and to be received.
            uint16_t bytesReceived = USB_PipeDataTransferredSizeGet(pipe);
            bytesReceived += USB_NumberBytesReceivedGet(pipe.address);
            USB_PipeDataTransferredSizeSet(pipe, bytesReceived);
            uint16_t transferDataSize = USB_PipeDataToTransferSizeGet(pipe);

            if (0U == (transferDataSize - bytesReceived))
            {
                controlTransfer.totalBytesTransferred += bytesReceived;
                if (controlTransfer.transferDataSize == controlTransfer.totalBytesTransferred)
                {
                    // Data stage is complete, sends an IN ZLP for status stage.
                    USB_ControlTransferZLP(USB_REQUEST_DIR_IN);
                }
                else
                {
                    // No overUnderRunCallback registered — stall.
                    controlTransfer.status = USB_CONTROL_STALL_REQ;
                    USB_EndpointInStall(0);
                    USB_EndpointOutStall(0);
                }
            }
            else
            {
                // Starts next transaction in data stage.
                USB_OutTransactionRun(pipe);
            }

            USB_EndpointOutOverUnderflowAck(0);
            break;
        }
        case USB_CONTROL_ZLP:
        {
            // Valid end of setup request.
            if (controlTransfer.endOfRequestCallback != NULL)
            {
                controlTransfer.endOfRequestCallback();
            }
            USB_ControlTransferReset();
            break;
        }
        case USB_CONTROL_SETUP:
        {
            USB_ControlTransferReset();
            break;
        }
        default:
            break;
    }
}

void USB_ControlTransferZLP(uint8_t direction)
{
    USB_NumberBytesToSendReset(0);
    USB_NumberBytesSentReset(0);
    USB_NumberBytesToReceiveReset(0u);
    USB_NumberBytesReceivedReset(0u);

    // Prepare to receive a new setup package in case the host decides to ignore the ZLP stage
    USB_PipeDataPtrSet((USB_PIPE_t){ .address = 0, .direction = USB_REQUEST_DIR_OUT }, controlTransfer.buffer);
    EndpointBufferSet((USB_PIPE_t){ .address = 0, .direction = USB_REQUEST_DIR_OUT }, controlTransfer.buffer);

    controlTransfer.status = USB_CONTROL_ZLP;

    // Starts the ZLP transaction
    if (direction == USB_REQUEST_DIR_IN)
    {
        USB_EndpointInNAKClear(0);
        USB_EndpointInOverUnderflowAck(0);
    }
    else
    {
        USB_EndpointOutNAKClear(0);
        USB_EndpointOutOverUnderflowAck(0);
    }
}

void USB_ControlTransferReset(void)
{
    USB_PIPE_t controlPipeOut = { .address = 0u, .direction = USB_EP_DIR_OUT };
    USB_TransactionAbort(controlPipeOut);
    USB_TransactionAbort((USB_PIPE_t){ .address = 0u, .direction = USB_EP_DIR_IN });

    USB_EndpointOutStatusClear(0u);
    USB_EndpointInStatusClear(0u);

    USB_PipeDataPtrSet(controlPipeOut, controlTransfer.buffer);
    USB_PipeDataToTransferSizeSet(controlPipeOut, sizeof(USB_SETUP_REQUEST_t));
    USB_PipeDataTransferredSizeReset(controlPipeOut);
    USB_PipeTransferEndCallbackRegister(controlPipeOut, NULL);

    USB_NumberBytesToSendReset(0u);
    USB_NumberBytesSentReset(0u);
    USB_NumberBytesToReceiveReset(0u);
    USB_NumberBytesReceivedReset(0u);

    controlTransfer.endOfRequestCallback = NULL;
    controlTransfer.transferDataSize = 0u;
    controlTransfer.status = USB_CONTROL_SETUP;
}

RETURN_CODE_t USB_ControlTransferDataSet(uint8_t *dataPtr, uint16_t dataSize)
{
    // NULL callers always pass dataSize=0 (USB_SetupProcess reset path)
    controlTransfer.transferDataPtr = (dataPtr != NULL) ? dataPtr : controlTransfer.buffer;
    controlTransfer.transferDataSize = dataSize;
    return SUCCESS;
}

RETURN_CODE_t USB_ControlTransferDataWriteBuffer(uint8_t *dataPtr, uint8_t dataSize)
{
    (void)memcpy(controlTransfer.buffer, dataPtr, dataSize);
    controlTransfer.transferDataPtr = controlTransfer.buffer;
    controlTransfer.transferDataSize = dataSize;
    return SUCCESS;
}

void USB_ControlEndOfRequestCallbackRegister(USB_SETUP_ENDOFREQUEST_CALLBACK_t callback)
{
    controlTransfer.endOfRequestCallback = callback;
}


void USB_ControlProcessOverUnderflow(uint8_t overunderflow)
{
    if (USB_CONTROL_DATA_IN == controlTransfer.status)
    {
        if (OVERFLOW_EVENT == overunderflow)
        {
            // Host is done with the data stage and expects an OUT ZLP
            USB_ControlTransferZLP(USB_REQUEST_DIR_OUT);
        }
        // else: host is too eager, let transfer handler deal with it
    }
    else if (USB_CONTROL_DATA_OUT == controlTransfer.status)
    {
        if (UNDERFLOW_EVENT == overunderflow)
        {
            // Host is done with the data stage and expects an IN ZLP
            USB_ControlTransferZLP(USB_REQUEST_DIR_IN);
        }
        // else: host is too eager, let transfer handler deal with it
    }
}

void USB_HandleEventStalled(USB_PIPE_t pipe)
{
    // Always called with pipe.address == 0 (control endpoint)
    USB_EndpointInStallAck(pipe.address);
    USB_EndpointOutStallAck(pipe.address);
    USB_EndpointInStallClear(pipe.address);
    USB_EndpointOutStallClear(pipe.address);
    USB_ControlTransferReset();
}

void USB_PeripheralInitialize(void)
{
    USB_Enable();
    USB_FrameNumEnable();
    USB_FifoEnable();
    USB_FifoReadPointerReset();
    USB_FifoWritePointerReset();
    USB_EndpointTableAddressSet(endpointTable.EP);
    USB_MaxEndpointsSet(USB_EP_NUM - 1u);
    USB_InterruptFlagsClear();
    // Reset endpoints table
    for (uint8_t endpoint = 0; endpoint < (uint8_t)USB_EP_NUM; endpoint++)
    {
        endpointTable.EP[endpoint].OUT.CTRL = 0;
        endpointTable.EP[endpoint].OUT.STATUS = 0;
        endpointTable.EP[endpoint].IN.CTRL = 0;
        endpointTable.EP[endpoint].IN.STATUS = 0;
    }
    // Reset software pipe transfer state
    USB_PIPE_t pipe;
    for (pipe.address = 0; pipe.address < USB_EP_NUM; pipe.address++)
    {
        pipe.direction = USB_EP_DIR_OUT; USB_PipeReset(pipe);
        pipe.direction = USB_EP_DIR_IN;  USB_PipeReset(pipe);
    }
}

void USB_PeripheralDisable(void)
{
    USB_Disable();
    USB_DeviceAddressReset();
}
