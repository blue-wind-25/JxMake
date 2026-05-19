/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREREQUESTSINTERFACE USB Core Requests Interface Source File
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


#include "usb_core.h"
#include "usb_core_requests_interface.h"


/*
 * GET_INTERFACE_REQUEST_NUMBER_MASK
 * Mask for the interface number in the wIndex field of the setup request.
 */
#define GET_INTERFACE_REQUEST_NUMBER_MASK ( 0xFFu )

/*
 * GET_INTERFACE_REQUEST_WVALUE
 * Value for the wValue field of the setup request.
 */
#define GET_INTERFACE_REQUEST_WVALUE 0u

/*
 * GET_INTERFACE_RESPONSE_SIZE
 * Size of the response to the Get Interface request.
 */
#define GET_INTERFACE_RESPONSE_SIZE 1u

RETURN_CODE_t USB_SetupInterfaceRequestGetStatus( void )
{
    uint8_t data[] = {
        0, 0
    };

    return USB_ControlTransferDataWriteBuffer( data, sizeof( data ) );
}

RETURN_CODE_t USB_SetupInterfaceRequestGetInterface( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    // All interfaces have only alternate setting 0
    (void) setupRequestPtr;
    uint8_t alternateSetting = 0;
    return USB_ControlTransferDataWriteBuffer( &alternateSetting, sizeof( alternateSetting ) );
}

RETURN_CODE_t USB_SetupInterfaceRequestSetInterface( USB_SETUP_REQUEST_t* setupRequestPtr )
{
    return USB_DescriptorInterfaceConfigure( setupRequestPtr->wIndex, setupRequestPtr->wValue, true );
}
