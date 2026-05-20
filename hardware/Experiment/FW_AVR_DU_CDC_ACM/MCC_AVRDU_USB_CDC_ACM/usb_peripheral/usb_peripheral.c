/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBPERIPHERAL Peripheral Source File
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

#include "usb_peripheral.h"
#include "usb_peripheral_avr_du.h"


#include "../usb_common/usb_core.h"

USB_CONTROL_TRANSFER_t controlTransfer __attribute__( ( aligned( 2 ) ) ) = {
    .transferDataPtr = controlTransfer.buffer
};

void USB_ControlSetupReceived( void )
{
    USB_SetupInterruptClear();

    if( USB_CONTROL_STALL_REQ == controlTransfer.status )
        // Stall events are handled by the EventHandler.
        return;

    // Acks Setup Received on the control endpoints.
    USB_EndpointOutSetupReceivedAck( 0u );
    USB_EndpointInSetupCompleteAck( 0u );

    // Clears bytes received and sent
    USB_NumberBytesToSendReset( 0u );
    USB_NumberBytesSentReset( 0u );
    USB_NumberBytesToReceiveReset( 0u );
    USB_NumberBytesReceivedReset( 0u );

    // Copies setup packet out of buffer to make it available for a data stage.
    (void) memcpy( (uint8_t*) ( &controlTransfer.setupRequest ), controlTransfer.buffer, sizeof( USB_SETUP_REQUEST_t ) );

    RETURN_CODE_t setup_status = USB_SetupProcess( &controlTransfer.setupRequest );

    if( UNSUPPORTED == setup_status ) {
        // Setup Request unknown or rejected, stalls the next control transaction.
        controlTransfer.status = USB_CONTROL_STALL_REQ;
        USB_EndpointInStall( 0u );
        USB_EndpointOutStall( 0u );
    }
    else if( SUCCESS == setup_status ) {
        if( 0u == controlTransfer.transferDataSize ) {
            // Request did not contain a data stage, sends ZLP directly.
            USB_ControlTransferZLP( USB_EP_DIR_IN );
        }
        else {
            // Sends or Receives data in next stage of request.
            USB_PIPE_t controlPipe = {
                .address = 0u, .direction = USB_EP_DIR_IN
            };
            if( ( controlTransfer.setupRequest.bmRequestType.dataPhaseTransferDirection ) == USB_REQUEST_DIR_IN ) {
                controlTransfer.status = USB_CONTROL_DATA_IN;
            }
            else {
                // Control OUT data transactions are controlled by the IN.DATAPTR so set specifically here.
                EndpointBufferSet( controlPipe, controlTransfer.transferDataPtr );

                controlPipe.direction  = USB_EP_DIR_OUT;
                controlTransfer.status = USB_CONTROL_DATA_OUT;
            }

            // Sets up the pipe variables.
            USB_PipeDataTransferredSizeReset( controlPipe );
            USB_PipeDataPtrSet( controlPipe, controlTransfer.transferDataPtr );
            USB_PipeDataToTransferSizeSet( controlPipe, controlTransfer.transferDataSize );

            // Start data stage transaction.
            USB_ControlTransactionComplete( controlPipe );
        }
    }
    // else: setup error — no action, stall will occur on next transaction
}

void USB_ControlTransactionComplete( USB_PIPE_t pipe )
{
    // Always called with pipe.address == 0 (control endpoint)
    USB_DataToggleSet( pipe );

    switch( controlTransfer.status ) {
        case USB_CONTROL_DATA_IN: {
            pipe.direction = USB_EP_DIR_IN;

            // Updates bytes sent and to be sent.
            uint16_t bytesSent = USB_PipeDataTransferredSizeGet( pipe );
            bytesSent += USB_NumberBytesSentGet( pipe.address );
            USB_PipeDataTransferredSizeSet( pipe, bytesSent );
            uint16_t transferDataSize = USB_PipeDataToTransferSizeGet( pipe );

            // Checks remaining data to send.
            if( 0U == ( transferDataSize - bytesSent ) ) {
                // Data stage complete — send OUT ZLP for status stage.
                USB_ControlTransferZLP( USB_REQUEST_DIR_OUT );
            }
            else {
                USB_InTransactionRun( pipe );
            }

            USB_EndpointInOverUnderflowAck( 0 );
            break;
        }

        case USB_CONTROL_DATA_OUT: {
            pipe.direction = USB_EP_DIR_OUT;

            // Updates bytes received and to be received.
            uint16_t bytesReceived = USB_PipeDataTransferredSizeGet( pipe );
            bytesReceived += USB_NumberBytesReceivedGet( pipe.address );
            USB_PipeDataTransferredSizeSet( pipe, bytesReceived );
            uint16_t transferDataSize = USB_PipeDataToTransferSizeGet( pipe );

            if( 0U == ( transferDataSize - bytesReceived ) ) {
                // Data stage complete — send IN ZLP for status stage.
                USB_ControlTransferZLP( USB_REQUEST_DIR_IN );
            }
            else {
                USB_OutTransactionRun( pipe );
            }

            USB_EndpointOutOverUnderflowAck( 0 );
            break;
        }

        case USB_CONTROL_ZLP: {
            // Valid end of setup request.
            if( controlTransfer.endOfRequestCallback != NULL ) controlTransfer.endOfRequestCallback();
            USB_ControlTransferReset();
            break;
        }

        case USB_CONTROL_SETUP: {
            USB_ControlTransferReset();
            break;
        }

        default:
            break;
    } // switch
}

void USB_ControlTransferZLP( uint8_t direction )
{
    USB_NumberBytesToSendReset( 0 );
    USB_NumberBytesSentReset( 0 );
    USB_NumberBytesToReceiveReset( 0u );
    USB_NumberBytesReceivedReset( 0u );

    // Prepare to receive a new setup package in case the host decides to ignore the ZLP stage
    USB_PipeDataPtrSet( (USB_PIPE_t) { .address = 0, .direction = USB_REQUEST_DIR_OUT }, controlTransfer.buffer );
    EndpointBufferSet( (USB_PIPE_t) { .address = 0, .direction = USB_REQUEST_DIR_OUT }, controlTransfer.buffer );

    controlTransfer.status = USB_CONTROL_ZLP;

    // Starts the ZLP transaction
    if( direction == USB_REQUEST_DIR_IN ) {
        USB_EndpointInNAKClear( 0 );
        USB_EndpointInOverUnderflowAck( 0 );
    }
    else {
        USB_EndpointOutNAKClear( 0 );
        USB_EndpointOutOverUnderflowAck( 0 );
    }
}

void USB_ControlTransferReset( void )
{
    USB_PIPE_t controlPipeOut = {
        .address = 0u, .direction = USB_EP_DIR_OUT
    };
    USB_TransactionAbort( controlPipeOut );
    USB_TransactionAbort( (USB_PIPE_t) { .address = 0u, .direction = USB_EP_DIR_IN } );

    USB_EndpointOutStatusClear( 0u );
    USB_EndpointInStatusClear( 0u );

    USB_PipeDataPtrSet( controlPipeOut, controlTransfer.buffer );
    USB_PipeDataToTransferSizeSet( controlPipeOut, sizeof( USB_SETUP_REQUEST_t ) );
    USB_PipeDataTransferredSizeReset( controlPipeOut );
    USB_PipeTransferEndCallbackRegister( controlPipeOut, NULL );

    USB_NumberBytesToSendReset( 0u );
    USB_NumberBytesSentReset( 0u );
    USB_NumberBytesToReceiveReset( 0u );
    USB_NumberBytesReceivedReset( 0u );

    controlTransfer.endOfRequestCallback = NULL;
    controlTransfer.transferDataSize = 0u;
    controlTransfer.status = USB_CONTROL_SETUP;
}


void USB_PeripheralInitialize( void )
{
    USB_Enable();
    USB_FrameNumEnable();
    USB_FifoEnable();
    USB_FifoReadPointerReset();
    USB_FifoWritePointerReset();
    USB_EndpointTableAddressSet( endpointTable.EP );
    USB_MaxEndpointsSet( USB_EP_NUM - 1u );
    USB_InterruptFlagsClear();
    // Reset endpoints table (CTRL, STATUS, DATAPTR, CNT all zero; endpoints disabled until USB_EndpointConfigure)
    memset( endpointTable.EP, 0, sizeof( endpointTable.EP ) );
    // Reset software pipe transfer state (USB_PIPE_TRANSFER_OK == 0, all pointer/size fields NULL/0)
    memset( pipeTransfer, 0, USB_EP_NUM * 2u * sizeof( USB_PIPE_TRANSFER_t ) );
}

