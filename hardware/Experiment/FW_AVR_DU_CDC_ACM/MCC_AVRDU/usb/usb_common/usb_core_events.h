/*
 * USBCOREEVENTS USB Core Events Header File
 * usb_core_events.h
 * usb_core_events USB Core Events
 * usb_core
 * Event handling for the USB Core Stack.
 * USB Device Core Version 1.0.0
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

#ifndef USB_CORE_EVENTS_H
// cppcheck-suppress misra-c2012-2.5
#define USB_CORE_EVENTS_H

#include <stdbool.h>
#include <stdint.h>

#include <usb_common_elements.h>

#include <usb_core_descriptors.h>
#include <usb_core_requests.h>
#include <usb_core_transfer.h>
#include <usb_peripheral.h>
#include <usb_protocol_headers.h>

/*
 * usb_core
 * Handles the different types of events.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_EventHandler(void);

#endif /* USB_CORE_EVENTS_H */
