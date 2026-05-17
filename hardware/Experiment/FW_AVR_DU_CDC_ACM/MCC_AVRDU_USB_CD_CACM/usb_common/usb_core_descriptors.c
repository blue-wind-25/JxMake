/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREDESCRIPTOR Core Descriptors Source File
 * usb_core_descriptors.h
 * usb_core_descriptors
 * descriptors for the USB Core Stack.
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


#include <stddef.h>

#include "../usb_peripheral/usb_peripheral.h"
#include "usb_core_descriptors.h"
#include "usb_core_events.h"


/*
 * usb_core_descriptors
 * USB_DEFAULT_INTERFACE
 * Default interface number.
 */
#define USB_DEFAULT_INTERFACE 0u

/*
 * usb_core_descriptors
 * USB_DEFAULT_ALTERNATE_SETTING
 * Default alternate setting.
 */
#define USB_DEFAULT_ALTERNATE_SETTING 0u

/*
 * usb_core_descriptors
 * USB_DESCRIPTOR_SEARCH_LIMIT
 * The number of descriptors NextDescriptorPointerGet will search through before returning an error.
 */
#define USB_DESCRIPTOR_SEARCH_LIMIT 30u

/*
 *  USB_DESCRIPTOR_PTR_t
 * Union of a uint8_t pointer and pointers to the different descriptor types.
 * ,19.2} Needed for the stack to parse through the configuration descriptors
 * without pointer casting between the different descriptor types and uint8_t.
 */
typedef union USB_DESCRIPTOR_PTR_union
{
    uint8_t *bytePtr;
    USB_DESCRIPTOR_HEADER_t *headerPtr;
    USB_ENDPOINT_DESCRIPTOR_t *endpointPtr;
    USB_INTERFACE_DESCRIPTOR_t *interfacePtr;
    USB_CONFIGURATION_DESCRIPTOR_t *configurationPtr;
} USB_DESCRIPTOR_PTR_t;


STATIC USB_CONFIGURATION_DESCRIPTOR_t *activeConfigurationPtr = NULL;
STATIC uint8_t activeInterfaces[USB_INTERFACE_NUM];
USB_DESCRIPTOR_POINTERS_t *applicationPointers = NULL;

RETURN_CODE_t USB_DescriptorConfigurationEnable(uint8_t configurationValue)
{
    USB_DESCRIPTOR_PTR_t currentDescriptor;
    RETURN_CODE_t status = SUCCESS;

    if (NULL != activeConfigurationPtr)
    {
        // Find and disable all active interfaces in the current configuration
        currentDescriptor.configurationPtr = activeConfigurationPtr;
        uint8_t numInterfaces = activeConfigurationPtr->bNumInterfaces;
        while ((SUCCESS == status) && (numInterfaces > 0u))
        {
            status = NextDescriptorPointerGet(USB_DESCRIPTOR_TYPE_INTERFACE, &currentDescriptor.headerPtr);

            if (SUCCESS == status)
            {
                if (activeInterfaces[currentDescriptor.interfacePtr->bInterfaceNumber] == currentDescriptor.interfacePtr->bAlternateSetting)
                {
                    status = USB_DescriptorInterfaceConfigure(currentDescriptor.interfacePtr->bInterfaceNumber, USB_DEFAULT_ALTERNATE_SETTING, false);
                    numInterfaces--;
                }
            }
        }
    }

    if (SUCCESS == status)
    {
        if (USB_REQUEST_DEVICE_DISABLE_CONFIGURATION == configurationValue)
        {
            // Active configuration is disabled, so clear pointer
            activeConfigurationPtr = NULL;
        }
        else
        {
            // Get new configuration pointer and enable its interfaces
            status = ConfigurationPointerGet(configurationValue, &activeConfigurationPtr);

            if (SUCCESS == status)
            {
                // Find and enable all interfaces in the set configuration with bAlternateSetting == 0
                currentDescriptor.configurationPtr = activeConfigurationPtr;
                uint8_t numInterfaces = activeConfigurationPtr->bNumInterfaces;
                while ((SUCCESS == status) && (numInterfaces > 0u))
                {
                    status = NextDescriptorPointerGet(USB_DESCRIPTOR_TYPE_INTERFACE, &currentDescriptor.headerPtr);
                    if (SUCCESS == status)
                    {
                        if (USB_DEFAULT_ALTERNATE_SETTING == currentDescriptor.interfacePtr->bAlternateSetting)
                        {
                            status = USB_DescriptorInterfaceConfigure(currentDescriptor.interfacePtr->bInterfaceNumber, USB_DEFAULT_ALTERNATE_SETTING, true);
                            numInterfaces--;
                        }
                    }
                }
            }
        }
    }

    return status;
}

uint8_t USB_DescriptorActiveConfigurationValueGet(void)
{
    uint8_t configurationValue = USB_REQUEST_DEVICE_DISABLE_CONFIGURATION;

    if (NULL != activeConfigurationPtr)
    {
        configurationValue = activeConfigurationPtr->bConfigurationValue;
    }

    return configurationValue;
}

RETURN_CODE_t ConfigurationPointerGet(uint8_t configurationValue, USB_CONFIGURATION_DESCRIPTOR_t **configurationPtr)
{
    // Single configuration device (bNumConfigurations == 1)
    if (configurationValue > 1u)
    {
        return DESCRIPTOR_CONFIGURATIONS_ERROR;
    }
    *configurationPtr = applicationPointers->configurationsPtr;
    return SUCCESS;
}

RETURN_CODE_t NextDescriptorPointerGet(USB_DESCRIPTOR_TYPE_t descriptorType, USB_DESCRIPTOR_HEADER_t **descriptorHeaderPtr)
{
    RETURN_CODE_t status = UNINITIALIZED;

    USB_DESCRIPTOR_PTR_t currentDescriptor = { .headerPtr = *descriptorHeaderPtr };

    uint8_t incrementCount = 0u;
    while (UNINITIALIZED == status)
    {
        // Advance by the current descriptor's own length (CONFIGURATION type never passed by callers)
        currentDescriptor.bytePtr = &currentDescriptor.bytePtr[currentDescriptor.headerPtr->bLength];

        // Checks whether it has found the correct descriptor type or if it needs to continue looping.
        if (descriptorType == (USB_DESCRIPTOR_TYPE_t)currentDescriptor.headerPtr->bDescriptorType)
        {
            status = SUCCESS;
            *descriptorHeaderPtr = currentDescriptor.headerPtr;
        }
        else
        {
            // If it has looped through too many descriptors, it is assumed that the descriptor structure is set up incorrectly and the loop is exited.
            if (incrementCount++ > USB_DESCRIPTOR_SEARCH_LIMIT)
            {
                status = DESCRIPTOR_SEARCH_ERROR;
            }
        }
    }

    return status;
}

RETURN_CODE_t USB_DescriptorInterfaceConfigure(uint8_t interfaceNumber, uint8_t alternateSetting, bool enable)
{
    RETURN_CODE_t status = UNINITIALIZED;

    if (NULL != activeConfigurationPtr)
    {
        // Pointer initialized to the address of the active configuration descriptor
        USB_DESCRIPTOR_PTR_t currentDescriptor = { .configurationPtr = activeConfigurationPtr };

        // Limit to end of configuration to make sure the loop does not overflow
        uint8_t *endOfConfiguration = &currentDescriptor.bytePtr[currentDescriptor.configurationPtr->wTotalLength];

        // Find first interface descriptor before entering loop
        status = NextDescriptorPointerGet(USB_DESCRIPTOR_TYPE_INTERFACE, &currentDescriptor.headerPtr);

        // Loop through all the descriptors in the current configuration
        USB_INTERFACE_DESCRIPTOR_t *enableInterfacePtr = NULL;
        while ((SUCCESS == status) && (currentDescriptor.bytePtr < endOfConfiguration))
        {
            // Check if interface number and alternate setting correspond to the active interface before disabling endpoints
            if ((interfaceNumber == currentDescriptor.interfacePtr->bInterfaceNumber) && (activeInterfaces[interfaceNumber] == currentDescriptor.interfacePtr->bAlternateSetting))
            {
                // Disable endpoints for the active alternate interface
                DescriptorEndpointsConfigure(currentDescriptor.interfacePtr, false);
                activeInterfaces[interfaceNumber] = USB_DEFAULT_ALTERNATE_SETTING;
            }

            if (enable)
            {
                if (interfaceNumber == currentDescriptor.interfacePtr->bInterfaceNumber)
                {
                    if (alternateSetting == currentDescriptor.interfacePtr->bAlternateSetting)
                    {
                        // Requested interface found
                        enableInterfacePtr = currentDescriptor.interfacePtr;
                    }
                }
            }

            status = NextDescriptorPointerGet(USB_DESCRIPTOR_TYPE_INTERFACE, &currentDescriptor.headerPtr);
            if (DESCRIPTOR_SEARCH_ERROR == status)
            {
                // Search error from NextDescriptorPointerGet means search is complete
                if ((false == enable) || (NULL != enableInterfacePtr))
                {
                    // Search was successful, correcting status
                    status = SUCCESS;

                    // Set byte pointer to end of config to exit loop
                    currentDescriptor.bytePtr = endOfConfiguration;
                }
            }
        }

        if (SUCCESS == status)
        {
            if (true == enable)
            {
                if (NULL != enableInterfacePtr)
                {
                    // Enable the endpoints for the activated interface
                    DescriptorEndpointsConfigure(enableInterfacePtr, true);
                    activeInterfaces[interfaceNumber] = alternateSetting;
                }
                else
                {
                    status = DESCRIPTOR_SEARCH_ERROR;
                }
            }
        }
    }
    else
    {
        status = DESCRIPTOR_POINTER_ERROR;
    }

    return status;
}

RETURN_CODE_t DescriptorEndpointsConfigure(USB_INTERFACE_DESCRIPTOR_t *interfacePtr, bool enable)
{
    // The number of endpoints to enable/disable is found from the interface.
    uint8_t numEndpoints = interfacePtr->bNumEndpoints;

    USB_DESCRIPTOR_PTR_t currentDescriptor = { .interfacePtr = interfacePtr };

    while (numEndpoints > 0u)
    {
        // Pre-increments to the next descriptor, since the initial descriptor is the interface.
        currentDescriptor.bytePtr = &currentDescriptor.bytePtr[currentDescriptor.headerPtr->bLength];

        if (USB_DESCRIPTOR_TYPE_ENDPOINT == (USB_DESCRIPTOR_TYPE_t)currentDescriptor.headerPtr->bDescriptorType)
        {
            if (true == enable)
            {
                // Configures endpoint according to descriptor (sizes 8/64 always valid).
                USB_EndpointConfigure(currentDescriptor.endpointPtr->bEndpointAddress, currentDescriptor.endpointPtr->wMaxPacketSize, currentDescriptor.endpointPtr->bmAttributes.type);
            }
            else
            {
                // Aborts any ongoing transfer and disable endpoint.
                USB_TransferAbort(currentDescriptor.endpointPtr->bEndpointAddress);
                USB_EndpointDisable(currentDescriptor.endpointPtr->bEndpointAddress);
            }

            numEndpoints--;
        }
    }

    return SUCCESS;
}

RETURN_CODE_t USB_DescriptorPointerGet(USB_DESCRIPTOR_TYPE_t descriptor, uint8_t attribute, uint8_t **descriptorPtr, uint16_t *descriptorLength)
{
    RETURN_CODE_t status = UNINITIALIZED;

    USB_DESCRIPTOR_PTR_t localDescriptorPtr;

    switch (descriptor)
    {
    case USB_DESCRIPTOR_TYPE_DEVICE:
        *descriptorPtr = (uint8_t *)applicationPointers->devicePtr;
        *descriptorLength = (uint16_t)applicationPointers->devicePtr->header.bLength;
        status = SUCCESS;
        break;
    case USB_DESCRIPTOR_TYPE_CONFIGURATION:;
        // Returns pointer to configuration, with the total length.

        status = ConfigurationPointerGet(attribute, &localDescriptorPtr.configurationPtr);
        if (SUCCESS == status)
        {
            *descriptorPtr = localDescriptorPtr.bytePtr;
            *descriptorLength = localDescriptorPtr.configurationPtr->wTotalLength;
        }
        break;
    case USB_DESCRIPTOR_TYPE_DEVICE_QUALIFIER:
    case USB_DESCRIPTOR_TYPE_OTHER_SPEED_CONFIGURATION:
    case USB_DESCRIPTOR_TYPE_BOS:
        // Not supported: high-speed only or not present
        status = UNSUPPORTED;
        break;
    default:
        // Class/vendor descriptors are filtered before this function is called
        status = DESCRIPTOR_REQUEST_ERROR;
        break;
    }

    if (SUCCESS != status)
    {
        *descriptorPtr = NULL;
        *descriptorLength = 0u;
    }

    return status;
}

RETURN_CODE_t USB_DescriptorStringPointerGet(uint8_t stringIndex, uint16_t langID, uint8_t **descriptorAddressPtr, uint16_t *descriptorLength)
{
    if (stringIndex == 0u)
    {
        // Index 0 returns the language ID descriptor
        *descriptorAddressPtr = (uint8_t *)applicationPointers->langIDptr;
        *descriptorLength = (uint16_t)applicationPointers->langIDptr->header.bLength;
        return SUCCESS;
    }

    // LANG_ID_NUM == 1: only id_array[0] exists
    if (langID != applicationPointers->langIDptr->id_array[0])
    {
        return UNSUPPORTED;
    }

    // stringPtrs[0] is always initialized (statically set in usb_descriptors.c)
    USB_DESCRIPTOR_HEADER_t *stringHeader = applicationPointers->stringPtrs[0];
    if (stringIndex > 1u)
    {
        RETURN_CODE_t status = UNINITIALIZED;
        for (uint8_t i = 1u; i < stringIndex; i++)
        {
            status = NextDescriptorPointerGet(USB_DESCRIPTOR_TYPE_STRING, &stringHeader);
        }
        if (SUCCESS != status)
        {
            return status;
        }
    }

    *descriptorAddressPtr = (uint8_t *)stringHeader;
    *descriptorLength = (uint16_t)stringHeader->bLength;
    return SUCCESS;
}
