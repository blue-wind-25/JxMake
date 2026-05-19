/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREREQUESTSINTERFACE USB Core Requests Interface Header File
 * USB Interface Core Requests handling.
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


#ifndef USB_CORE_REQUESTS_INTERFACE_H
#define USB_CORE_REQUESTS_INTERFACE_H


/*
 * Returns status for the specified interface.
 *
 * Get status from interface request according to USB 2.0 specification Ch. 9.4.5.
 * | bRequest   | wValue | wIndex      | wLength | Data      |
 * |------------|--------|-------------|---------|-----------|
 * | GET_STATUS | Zero   |  Interface  | Two     | Interface |
 *
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_SetupInterfaceRequestGetStatus( void );

/*
 * Returns the alternate setting for the specified interface.
 *
 * Format for GET_INTERFACE request according to USB 2.0 specification Ch 9.4.4.
 * Document: Universal Serial Bus Specification for USB 2.0.
 * | bRequest      | wValue | wIndex    | wLength | Data              |
 * |---------------|--------|-----------|---------|-------------------|
 * | GET_INTERFACE | Zero   | Interface | One     | Alternate setting |
 *
 *     *setupRequestPtr - Pointer to the request and its data
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_SetupInterfaceRequestGetInterface( USB_SETUP_REQUEST_t* setupRequestPtr );

/*
 * Setup function for the interface request to select the alternate setting.
 *
 * A request to set interface according to USB 2.0 specification Ch. 9.4.10.
 * Document: Universal Serial Bus Specification for USB 2.0
 * | bRequest      | wValue            | wIndex    | wLength | Data |
 * |---------------|-------------------|-----------|---------|------|
 * | SET_INTERFACE | Alternate setting | Interface | Zero    | None |
 *
 *     *setupRequestPtr - Pointer to the request and its data
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_SetupInterfaceRequestSetInterface( USB_SETUP_REQUEST_t* setupRequestPtr );


#endif // USB_CORE_REQUESTS_INTERFACE_H
