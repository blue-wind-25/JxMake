/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBPERIPHERALREADWRITE Peripheral Read/Write Source File
 * API module for usb_peripheral covering low level USB transaction functions.
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


#include <stdbool.h>
#include <stddef.h>

#include "usb_peripheral_avr_du.h"
#include "usb_peripheral_endpoint.h"
#include "usb_peripheral_read_write.h"


USB_PIPE_TRANSFER_t pipeTransfer[USB_EP_NUM * 2];

void USB_TransactionAbort( USB_PIPE_t pipe )
{
    if( USB_EP_DIR_OUT == pipe.direction ) {
        USB_EndpointOutNAKSet( pipe.address );
    }
    else {
        USB_EndpointInNAKSet( pipe.address );
    }

    pipeTransfer[PipeTransferIndexGet( pipe )].status = USB_PIPE_TRANSFER_ABORTED;
}

void USB_PipeReset( USB_PIPE_t pipe )
{
    USB_PIPE_TRANSFER_t* pipeTransferPtr = &pipeTransfer[PipeTransferIndexGet( pipe )];
    pipeTransferPtr->status = USB_PIPE_TRANSFER_OK;
    pipeTransferPtr->transferDataPtr  = NULL;
    pipeTransferPtr->transferDataSize = 0;
    pipeTransferPtr->bytesTransferred = 0;
    pipeTransferPtr->transferEndCallback = NULL;
    pipeTransferPtr->ZLPEnable = false;
}



void USB_PipeTransferEndCallback( USB_PIPE_t pipe )
{
    USB_PIPE_TRANSFER_t* pipeTransferPtr = &pipeTransfer[PipeTransferIndexGet( pipe )];

    if( NULL != pipeTransferPtr->transferEndCallback ) pipeTransferPtr->transferEndCallback( pipe, pipeTransferPtr->status, pipeTransferPtr->bytesTransferred );
}

void USB_InTransactionRun( USB_PIPE_t pipe )
{
    USB_PIPE_TRANSFER_t* pipeTransferPtr = &pipeTransfer[PipeTransferIndexGet( pipe )];
    uint16_t             nextTransactionSize;

    pipeTransferPtr->status = USB_PIPE_TRANSFER_BUSY;

    nextTransactionSize = pipeTransferPtr->transferDataSize - pipeTransferPtr->bytesTransferred;
    if( 0U == nextTransactionSize ) {
        if( true == pipeTransferPtr->ZLPEnable ) {
            USB_NumberBytesToSendSet( pipe.address, 0u );
            USB_NumberBytesSentReset( pipe.address );
            USB_EndpointInNAKClear( pipe.address );
            pipeTransferPtr->ZLPEnable = false;
        }
        else {
            pipeTransferPtr->status = USB_PIPE_TRANSFER_OK;
        }
    }
    else {
        uint16_t endpointSize = USB_EndpointSizeGet( pipe );
        // InMultipktEnable=1 for all used endpoints (EP0, EP2)
        // Endpoint sizes are always powers of 2, so % endpointSize ≡ & (endpointSize-1)
        pipeTransferPtr->ZLPEnable = ( pipeTransferPtr->ZLPEnable ) && ( 0U == ( nextTransactionSize & ( (uint16_t) endpointSize - 1U ) ) );

        EndpointBufferSet( pipe, &pipeTransferPtr->transferDataPtr[pipeTransferPtr->bytesTransferred] );
        USB_NumberBytesToSendSet( pipe.address, nextTransactionSize );
        USB_NumberBytesSentReset( pipe.address );
        USB_EndpointInNAKClear( pipe.address );
    }
}

void USB_OutTransactionRun( USB_PIPE_t pipe )
{
    USB_PIPE_TRANSFER_t* pipeTransferPtr = &pipeTransfer[PipeTransferIndexGet( pipe )];

    pipeTransferPtr->status = USB_PIPE_TRANSFER_BUSY;
    EndpointBufferSet( pipe, &pipeTransferPtr->transferDataPtr[pipeTransferPtr->bytesTransferred] );

    uint16_t endpointSize = USB_EndpointSizeGet( pipe );
    uint16_t nextTransactionSize = pipeTransferPtr->transferDataSize - pipeTransferPtr->bytesTransferred;
    // OutMultipktEnable=1 for all used endpoints (EP0, EP2)
    // Endpoint sizes are always powers of 2, so % endpointSize ≡ & (endpointSize-1)

    if( 0u == nextTransactionSize ) {
        pipeTransferPtr->ZLPEnable = false;
        pipeTransferPtr->status = USB_PIPE_TRANSFER_OK;
    }
    else {
        pipeTransferPtr->ZLPEnable = pipeTransferPtr->ZLPEnable && ( 0u == ( nextTransactionSize & ( endpointSize - 1u ) ) );
    }

    USB_NumberBytesReceivedReset( pipe.address );
    USB_NumberBytesToReceiveSet( pipe.address, nextTransactionSize );
    USB_EndpointOutNAKClear( pipe.address );
}

void USB_PipeTransactionComplete( USB_PIPE_t pipe )
{
    USB_PIPE_TRANSFER_t* pipeTransferPtr = &pipeTransfer[PipeTransferIndexGet( pipe )];
    uint16_t             transactionSize;

    if( USB_EP_DIR_IN == pipe.direction ) {
        // Transaction complete on IN — InMultipktEnable=1 for EP0 and EP2.
        transactionSize = USB_NumberBytesSentGet( pipe.address );

        // Check if we need to send more data, or ZLP.
        pipeTransferPtr->bytesTransferred += transactionSize;
        if( ( pipeTransferPtr->bytesTransferred != pipeTransferPtr->transferDataSize ) || ( pipeTransferPtr->ZLPEnable ) ) {
            USB_InTransactionRun( pipe );
        }
        else {
            pipeTransferPtr->status = USB_PIPE_TRANSFER_OK;
        }
    }
    else {
        // Transaction complete on OUT.
        transactionSize = USB_NumberBytesReceivedGet( pipe.address );

        // Checks if we have transferred more than we wanted.
        uint16_t expectedTransferRemainingSize = pipeTransferPtr->transferDataSize - pipeTransferPtr->bytesTransferred;
        if( expectedTransferRemainingSize < transactionSize ) {
            // We may have overflowed the receive location!
            if( USB_NumberBytesToReceiveGet( pipe.address ) == expectedTransferRemainingSize ) {
                // Multipacket has limited what we received, even if transactionSize is larger.
                transactionSize = expectedTransferRemainingSize;
            }
            else {
                pipeTransferPtr->status = USB_PIPE_TRANSFER_ERROR;
                USB_PipeTransferEndCallback( pipe );
                return;
            }
        }

        // Updates bytes transferred and check if we need to run more transactions.
        pipeTransferPtr->bytesTransferred += transactionSize;

        if( ( ( pipeTransferPtr->bytesTransferred < pipeTransferPtr->transferDataSize ) || pipeTransferPtr->ZLPEnable ) && ( 0u == ( transactionSize & ( USB_EndpointSizeGet( pipe ) - 1u ) ) ) ) {
            USB_OutTransactionRun( pipe );
        }
        else {
            pipeTransferPtr->status = USB_PIPE_TRANSFER_OK;
        }
    }

    // Checks if transfer is completed and cleans up.
    if( USB_PIPE_TRANSFER_BUSY != pipeTransferPtr->status ) USB_PipeTransferEndCallback( pipe );
}
