/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCORETRANSFER USB Core Transfer Header File
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


#ifndef USB_CORE_TRANSFER_H
#define	USB_CORE_TRANSFER_H


#include "../usb_peripheral/usb_peripheral.h"
#include "usb_core_requests.h"
#include "usb_core_descriptors.h"


/*
 * Sets up the pipe for the write transfers.
 *
 * Sets up the pipe for the write transfers and checks to see if it is busy. If it is not busy, then the routine resets the pipe, setting it up for write transfer, then starts the transfer.
 *
 *     pipe - A combination of endpoint address and direction
 *     *dataPtr - The pointer to the data to write
 *     dataSize - The size of the data to write
 *     useZLP - Enable zero-length package at the end of transfer
 *     callback - A combination of pipe, status and transferred bytes
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_TransferWriteStart(USB_PIPE_t pipe, uint8_t *dataPtr, uint16_t dataSize, USB_TRANSFER_END_CALLBACK_t callback);

/*
 * Sets up the pipe for the read transfers.
 *
 * Sets up the pipe for the write transfers and checks to see if it is busy. If it is not busy, then the routine resets the pipe, setting it up for read transfer, then starts the transfer.
 *
 *     pipe - A combination of endpoint address and direction
 *     *dataPtr - The pointer to the data to read
 *     dataSize - The size of the data to read
 *     useZLP - Enable zero-length package at the end of transfer
 *     callback - A combination of pipe, status and transferred bytes
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_TransferReadStart(USB_PIPE_t pipe, uint8_t *dataPtr, uint16_t dataSize, USB_TRANSFER_END_CALLBACK_t callback);

/*
 * Sets up vendor or class control request data transfers.
 *
 * Sets up the pointer and size of the read or write trnasfer in the control data stage.
 *
 *     *dataPtr - The pointer to the data to read or write
 *     dataSize - The size of the data to read or write
 *     callback - Pointer to a function to be called at the end of the control request
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_TransferControlDataSet(uint8_t *dataPtr, uint16_t dataSize, USB_SETUP_ENDOFREQUEST_CALLBACK_t callback);

/*
 * Aborts an ongoing transfer.
 *
 * Will call the pipe transferEndCallback with the abort status, if configured.
 *
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_TransferAbort(USB_PIPE_t pipe);

/*
 * Handles the different types of packages received or transferred.
 *
 * Checks if a setup package is received or if a transaction is completed and which pipe has a completed transaction, then it handles them accordingly.
 * Sends an ACK upon completed transaction confirmation.
 *
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_TransferHandler(void);


#endif /* USB_CORE_TRANSFER_H */
