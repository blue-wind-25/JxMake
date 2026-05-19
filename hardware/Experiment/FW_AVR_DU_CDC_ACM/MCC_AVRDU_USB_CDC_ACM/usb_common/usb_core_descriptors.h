/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREDESCRIPTOR Core Descriptors Header File
 * In this file the active configuration and interfaces can be changed.
 * The active configuration and active interface is referenced by a pointer as two global variables.
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


#ifndef USB_CORE_DESCRIPTORS_H
#define USB_CORE_DESCRIPTORS_H


extern USB_DESCRIPTOR_POINTERS_t* applicationPointers;

/*
 * Enables endpoint configuration descriptor.
 *
 * The USB Device Enable Endpoint function, from USB 2.0 Specification Ch. 9.6.6.
 * | Offset | Field            | Size | Value    | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
 * |--------|------------------|------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
 * | 0      | bLength          | 1    | Number   | Size of this descriptor in bytes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
 * | 1      | bDescriptorType  | 1    | Constant | CONFIGURATION Descriptor Type                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
 * | 2      | bEndpointAddress | 1    | Endpoint | Address of the endpoint on the USB device described by this descriptor. Address is encoded by the following: Bit 3-0: The endpoint number, Bit 6-4: Reserved, reset to zero, Bit 7: Direction, ignored for control endpoint. 0 = OUT endpoint, 1 = IN endpoint                                                                                                                                                                                                                                                                                                          |
 * | 3      | bmAttributes     | 1    | Bitmap   | Describes the endpoint attributes when it is configured using the bConfigurationValue. Bit 1-0: Transfer type, 00=Control, 01=Isochronous, 10=Bulk, 11=Interrupt. If not isochronous endpoint, bit 5-2 are reserved and must be set to zero. If isochronous: Bit 3-2: Synchronization type, 00 = No Sync, 01 = Async, 10 = Adaptive, 11 = Sync. Bit 5-4: Usage type, 00 = Data endpoint, 01 = Feedback endpoint, 10 = Implicit feedback Data endpoint, 11 = Reserved. All other bits are reserved and must be reset to zero. Reserved bits must be ignored by the host. |
 * | 4      | wMaxPacketSize   | 2    | Number   | Maximum packet size this endpoint is capable of RX/TX when this configuration is selected. For isochronous endpoints, this value is used to reserve bus time in the schedule. For all endpoints, bit 10-0 specify maximum packet size. For high-speed isochronous and interrupt endpoints: Bit 12-11 number of additional transaction opportunities per microframe, 00 = None (1 transaction per microframe), 01 = 1 additional, 10 = 2 additional, 11 = Reserved. Bits 15-13 are reserved and must be set to zero.                                                     |
 *
 *     configurationValue - The value of the configuration to be enabled
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_DescriptorConfigurationEnable( uint8_t configurationValue );

/*
 * Gets the active configuration value.
 *     None.
 * return The active configuration value
 */
uint8_t USB_DescriptorActiveConfigurationValueGet( void );

/*
 * Enables or Disables an Interface Descriptor.
 *
 * The USB Device Enable Interface Descriptor, from USB 2.0 Specification Ch. 9.6.5.
 * | Offset | Field               | Size | Value    | Description                                                                                                                           |
 * |--------|---------------------|------|----------|---------------------------------------------------------------------------------------------------------------------------------------|
 * | 2      | bInterfaceNumber    | 1    | Number   | Number of this interface. Zero-based value identifying the index in the array of concurrent interfaces supported by this configuration|
 * | 3      | bAlternateSetting   | 1    | Number   | Value used to select this alternate setting for the interface identified in the prior field                                           |
 *
 *     interfaceNumber - Interface number value
 *     alternateSetting - Alternative settings value, ignored if enable is false
 *     enable - Enable or disable the interface
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_DescriptorInterfaceConfigure( uint8_t interfaceNumber, uint8_t alternateSetting, bool enable );

/*
 * Gets the pointer to the descriptor.
 *     descriptor - Descriptor type
 *     attribute - Attribute type
 *     **descriptorPtr - Pointer to the descriptor
 *     *descriptorLength - Length of the descriptor
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_DescriptorPointerGet( USB_DESCRIPTOR_TYPE_t descriptor, uint8_t attribute, uint8_t** descriptorPtr, uint16_t* descriptorLength );

/*
 * Gets the pointer to the string descriptor.
 *     stringIndex - Index of the string
 *     langID - Language ID
 *     **descriptorAddressPtr - Pointer to the descriptor
 *     *descriptorLength - Length of the descriptor
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_DescriptorStringPointerGet( uint8_t stringIndex, uint16_t langID, uint8_t** descriptorAddressPtr, uint16_t* descriptorLength );

/*
 * Collects the configuration pointer.
 *     configurationValue - Value of the referenced configuration
 *     **configurationPtr - Pointer to the configuration
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t ConfigurationPointerGet( uint8_t configurationValue, USB_CONFIGURATION_DESCRIPTOR_t** configurationPtr );

/*
 * Configures the endpoints as given in the descriptor.
 *     *interfacePtr - Pointer to an interface
 *     enable - Boolean to enable or disable the endpoint
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void DescriptorEndpointsConfigure( USB_INTERFACE_DESCRIPTOR_t* interfacePtr, bool enable );

/*
 * Gets the next descriptor.
 *     descriptorType - Selected descriptor type
 *     **descriptorHeaderPtr - Pointer to the descriptor header
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t NextDescriptorPointerGet( USB_DESCRIPTOR_TYPE_t descriptorType, USB_DESCRIPTOR_HEADER_t** descriptorHeaderPtr );


#endif // USB_CORE_DESCRIPTORS_H
