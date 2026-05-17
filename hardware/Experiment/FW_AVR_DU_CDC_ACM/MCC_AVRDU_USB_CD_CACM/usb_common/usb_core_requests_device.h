/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREREQUESTSDEVICE USB Core Requests Device Header File
 * usb_core_requests_device.h
 * usb_core_requests
 * USB Device Core Requests handling.
 * USB Device Core Version 1.0.0
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

#ifndef USB_CORE_REQUESTS_DEVICE_H
#define USB_CORE_REQUESTS_DEVICE_H


#include "usb_protocol_headers.h"


/*
 * usb_core_requests
 * Returns the status of the device features.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t SetupDeviceRequestGetStatus(void);

/*
 * usb_core_requests
 * Sets the device address.
 *     address - Address to be set
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t SetupDeviceRequestSetAddress(uint8_t address);

/*
 * usb_core_requests
 * Callback function for the address.
 *     None.
 * return None.
 */
void SetupDeviceAddressCallback(void);

/*
 * usb_core_requests
 * Gets the device descriptor.
 *     *setupRequestPtr - Pointer to the setup request
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t SetupDeviceRequestGetDescriptor(USB_SETUP_REQUEST_t *setupRequestPtr);

/*
 * usb_core_requests
 * Gets the device configuration.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t SetupDeviceRequestGetConfiguration(void);

/*
 * usb_core_requests
 * Sets the device configuration.
 *     configurationValue - Configuration value to be set
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t SetupDeviceRequestSetConfiguration(uint8_t configurationValue);


#endif	/* USB_CORE_REQUESTS_DEVICE_H */

