/**
 * USBCDCVIRTUALSERIAL CDC Virtual Serial Port Header File
 * @file usb_cdc_virtual_serial_port.h
 * @ingroup usb_cdc
 * @brief This file contains prototypes and datatypes for a CDC application
 * @version USB Device Stack Driver Version 1.0.0
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

#ifndef USB_CDC_VIRTUAL_SERIAL_PORT_H
#define USB_CDC_VIRTUAL_SERIAL_PORT_H

#include <stdint.h>
#include <stdbool.h>
#include <usb_core.h>
#include <usb_common_elements.h>
#include <usb_protocol_cdc.h>
#include <circular_buffer.h>

/**
 * @ingroup usb_cdc
 * @enum CDC_RETURN_CODE_t
 * @brief Type define for the CDC return codes.
 */
typedef enum CDC_RETURN_CODE_enum
{
    CDC_SUCCESS = 0,      /**<Action successfully executed*/
    CDC_BUFFER_FULL = -1, /**<Error triggered by full CDC buffer*/
    CDC_BUFFER_EMPTY = -2 /**<Error triggered by empty CDC buffer*/
} CDC_RETURN_CODE_t;

extern USB_PIPE_t CDCTxPipe;
extern USB_PIPE_t CDCRxPipe;
extern uint8_t usbCDCReceiveTempBuffer[];
extern CIRCULAR_BUFFER_t usbCDCReceiveBuffer;
extern CIRCULAR_BUFFER_t usbCDCTransmitBuffer;

/**
 * @ingroup usb_cdc
 * @brief Callback function called after the USB IN transaction started.
 * @param pipe - USB pipe used for the started transaction
 * @param status - Transfer status
 * @param bytesTransferred - Number of bytes transmitted in the transaction
 * @return None.
 */
void USB_CDCDataTransmitted(USB_PIPE_t pipe, USB_TRANSFER_STATUS_t status, uint16_t bytesTransferred);

/**
 * @ingroup usb_cdc
 * @brief Callback function called after the USB OUT transaction started.
 * @param pipe - USB pipe used for the started transaction
 * @param status - Transfer status
 * @param bytesTransferred - Number of bytes received in the transaction
 * @return None.
 */
void USB_CDCDataReceived(USB_PIPE_t pipe, USB_TRANSFER_STATUS_t status, uint16_t bytesTransferred);

#endif /* USB_CDC_VIRTUAL_SERIAL_PORT_H */