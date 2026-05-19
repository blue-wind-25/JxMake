/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREREQUESTSDEVICE USB Core Requests Device Core File
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


#include "usb_core_requests_device.h"


static uint8_t deviceAddress = 0;

RETURN_CODE_t SetupDeviceRequestSetAddress( uint8_t address )
{
    // Must register the callback here since device address must be set after completion of status stage.
    deviceAddress = address;
    USB_ControlEndOfRequestCallbackRegister( &SetupDeviceAddressCallback );

    return SUCCESS;
}

void SetupDeviceAddressCallback( void )
{
    USB_DeviceAddressConfigure( deviceAddress );
    USB_ControlEndOfRequestCallbackRegister( NULL );
}

RETURN_CODE_t SetupDeviceRequestSetConfiguration( uint8_t configurationValue )
{
    RETURN_CODE_t status = UNINITIALIZED;

    if( ( deviceAddress == 0u ) ) {
        status = USB_CONNECTION_ERROR;
    }
    else {
        // Enables configuration, clears it if configurationValue is zero.
        status = USB_DescriptorConfigurationEnable( configurationValue );
    }

    return status;
}
