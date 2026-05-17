/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCDC CDC Header File
 * usb_cdc.h
 * usb_cdc USB Communications Device Class (CDC)
 * This file contains prototypes and data types for a CDC application
 * USB Device Stack Driver Version 1.0.0
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

#ifndef USB_CDC_H
#define USB_CDC_H


#include "../usb_common/usb_protocol_headers.h"
#include "../usb_cdc/usb_protocol_cdc.h"


extern uint16_t usbCDCControlLineState;
extern USB_CDC_LINE_CODING_t usbCDCLineCoding;

/*
 * usb_cdc
 * Performs handling of control transfers.
 *     setupRequestPtr - Pointer to the Setup Request struct
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_CDCRequestHandler(USB_SETUP_REQUEST_t *setupRequestPtr);


#endif /* USB_CDC_H */
