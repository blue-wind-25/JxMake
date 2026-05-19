/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBPERIPHERALREADWRITE Peripheral Read/Write Header File
 * API module for usb_peripheral covering low-level USB transaction functions.
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


#ifndef USB_PERIPHERAL_READ_WRITE_H
#define USB_PERIPHERAL_READ_WRITE_H


// Pipe transfer index: (address * 2) + direction
#define PipeTransferIndexGet( pipe ) ( ( ( pipe ).address * 2 ) + ( pipe ).direction )

// Shared pipe transfer state array — defined in usb_peripheral_read_write.c
extern USB_PIPE_TRANSFER_t pipeTransfer[];


/*
 * Aborts the next transaction on an endpoint by setting BUSNACK.
 * Used to stop exchanging data on an endpoint. The device will start NAKing requests from the host after calling this API.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_TransactionAbort( USB_PIPE_t pipe );

/*
 * Acknowledges the Transaction Complete status condition by clearing the Transaction Complete status bit.
 * Used to clear the Transaction Complete status bit after a transaction has successfully completed.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_TransactionCompleteAck( USB_PIPE_t pipe );

/*
 * Helper function to return the endpoint transaction complete condition.
 *     None.
 * 0 - Transaction not complete or pipe address is out of bounds
 * 1 - Transaction is complete
 */
static inline bool USB_TransactionIsCompleted( void ) { return USB_TransactionCompleteInterruptIs(); }

/*
 * Returns the pipe address and direction for the latest completed transaction.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_TransactionCompletedPipeGet( USB_PIPE_t* pipe );

/*
 * Resets the pipe.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_PipeReset( USB_PIPE_t pipe );

/*
 * Checks if the pipe status is busy.
 *     pipe - A combination of endpoint address and direction
 * 0  -  Pipe status not busy
 * 1  -  Pipe status is busy
 */
static inline bool USB_PipeStatusIsBusy( USB_PIPE_t pipe ) { return pipeTransfer[PipeTransferIndexGet( pipe )].status == USB_PIPE_TRANSFER_BUSY; }

/*
 * Configures the pointer for the data transfer in a given pipe.
 *     pipe - A combination of endpoint address and direction
 *     *dataPtr - The pointer to the data location
 * return None.
 */
static inline void USB_PipeDataPtrSet( USB_PIPE_t pipe, uint8_t* dataPtr ) { pipeTransfer[PipeTransferIndexGet( pipe )].transferDataPtr = dataPtr; }

/*
 * Sets the size of pipe data to transfer.
 *     pipe - A combination of endpoint address and direction
 *     dataSize - The size of pipe data to transfer
 * return None.
 */
static inline void USB_PipeDataToTransferSizeSet( USB_PIPE_t pipe, uint16_t dataSize ) { pipeTransfer[PipeTransferIndexGet( pipe )].transferDataSize = dataSize; }

/*
 * Gets the size of pipe data to transfer.
 *     pipe - A combination of endpoint address and direction
 * return The size of pipe data to transfer
 */
static inline uint16_t USB_PipeDataToTransferSizeGet( USB_PIPE_t pipe ) { return pipeTransfer[PipeTransferIndexGet( pipe )].transferDataSize; }

/*
 * Gets the size of the transferred pipe data.
 *     pipe - A combination of endpoint address and direction
 * return The size of transferred pipe data
 */
static inline uint16_t USB_PipeDataTransferredSizeGet( USB_PIPE_t pipe ) { return pipeTransfer[PipeTransferIndexGet( pipe )].bytesTransferred; }

/*
 * Sets the size of the transferred pipe data.
 *     pipe - A combination of endpoint address and direction
 *     dataSize - The size of pipe data transferred
 * return None.
 */
static inline void USB_PipeDataTransferredSizeSet( USB_PIPE_t pipe, uint16_t dataSize ) { pipeTransfer[PipeTransferIndexGet( pipe )].bytesTransferred = dataSize; }

/*
 * Resets the size of transferred pipe data.
 *     pipe - A combination of endpoint address and direction
 * return None.
 */
static inline void USB_PipeDataTransferredSizeReset( USB_PIPE_t pipe ) { pipeTransfer[PipeTransferIndexGet( pipe )].bytesTransferred = 0; }

/*
 * Enables a ZLP on a transfer.
 * It is enabled by default if the AZLP static config is enabled for the pipe.
 *     pipe - A combination of endpoint address and direction
 * return None.
 */
// InAzlpEnable=0 and OutAzlpEnable=0 for all endpoints — always use manual ZLP
static inline void USB_PipeTransferZLP_Enable( USB_PIPE_t pipe ) { pipeTransfer[PipeTransferIndexGet( pipe )].ZLPEnable = true; }

/*
 * Sets the callback for transfer end.
 *     pipe - A combination of endpoint address and direction
 *     callback - A combination of pipe, status and transferred bytes
 * return None.
 */
static inline void USB_PipeTransferEndCallbackRegister( USB_PIPE_t pipe, USB_TRANSFER_END_CALLBACK_t callback ) { pipeTransfer[PipeTransferIndexGet( pipe )].transferEndCallback = callback; }

/*
 * Calls the callback for transfer end.
 *     pipe - A combination of endpoint address and direction
 * return None.
 */
void USB_PipeTransferEndCallback( USB_PIPE_t pipe );

/*
 * Checks the correctness of IN transactions and runs them.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_InTransactionRun( USB_PIPE_t pipe );

/*
 * Checks the correctness OUT transactions and runs them.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_OutTransactionRun( USB_PIPE_t pipe );

/*
 * Handles completed IN and OUT transactions.
 * Processes the completed transaction and either completes the transfer or runs the next transaction.
 * Will call the pipe transferEndCallback at the end of transfer, if configured.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_PipeTransactionComplete( USB_PIPE_t pipe );


#endif // USB_PERIPHERAL_READ_WRITE_H
