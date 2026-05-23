/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USB_CONFIG Generated Config Header File
 *
 * This is a device-specific USB static configuration file that will be editable
 * through a code composer tool, build script or manual entry by a user.
 *
 * The file encompasses static settings like number of endpoints, features to be enabled, etc.
 *
 * USB_DEVICE_STACK Driver Version 1.0.0
 */
/*
    (C) 2025 Microchip Technology Inc. and its subsidiaries.

    Subject to your compliance with these terms, you may use Microchip
    software and any derivatives exclusively with Microchip products.
    You are responsible for complying with 3rd party license terms
    applicable to your use of third party software (including open source
    software) that may accompany Microchip software. SOFTWARE IS "AS IS".

    NO WARRANTIES, WHETHER EXPRESS, IMPLIED OR STATUTORY, APPLY TO THIS
    SOFTWARE, INCLUDING ANY IMPLIED WARRANTIES OF NON-INFRINGEMENT,
    MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT
    WILL MICROCHIP BE LIABLE FOR ANY INDIRECT, SPECIAL, PUNITIVE,
    INCIDENTAL OR CONSEQUENTIAL LOSS, DAMAGE, COST OR EXPENSE OF ANY
    KIND WHATSOEVER RELATED TO THE SOFTWARE, HOWEVER CAUSED, EVEN IF
    MICROCHIP HAS BEEN ADVISED OF THE POSSIBILITY OR THE DAMAGES ARE
    FORESEEABLE. TO THE FULLEST EXTENT ALLOWED BY LAW, MICROCHIP?S
    TOTAL LIABILITY ON ALL CLAIMS RELATED TO THE SOFTWARE WILL NOT
    EXCEED AMOUNT OF FEES, IF ANY, YOU PAID DIRECTLY TO MICROCHIP FOR
    THIS SOFTWARE.
 */


#ifndef USB_CONFIG_H
#define USB_CONFIG_H


#include <stdint.h>

#include "usb_common/usb_common_elements.h"


/*
 * USB_EP_NUM
 * Limits the size of the endpoint table and transfer array in the RAM
 * to 1 + the highest endpoint address used by the application.
 */
#define USB_EP_NUM 3U

/*
 * USB_EP0_SIZE
 * Controls the packet size of endpoint 0 and must correspond to bMaxPacketSize0 in the device descriptor.
 */
#define USB_EP0_SIZE 64U

/*
 * LANG_ID_NUM
 * Controls the number of language IDs supported by the application.
 */
#define LANG_ID_NUM 1U

/*
 * USB Endpoint Addresses
 * Macros for the endpoint addresses.
 */
//
#define CDC_COMMUNICATION_INTERFACE_INTERRUPT_EP1_IN 1U
#define CDC_DATA_INTERFACE_BULK_EP2_IN 2U
#define CDC_DATA_INTERFACE_BULK_EP2_OUT 2U
//

/*
 * USB Endpoint Packet Sizes
 * Macros for the endpoint packet sizes.
 */
//
#define CDC_COMMUNICATION_INTERFACE_INTERRUPT_EP1_IN_SIZE 8U
#define CDC_DATA_INTERFACE_BULK_EP2_IN_SIZE 64U
#define CDC_DATA_INTERFACE_BULK_EP2_OUT_SIZE 64U
//

/*
 * USB_CDC_COMM_INTERFACE_NUM
 * The interface number for the CDC communication (control) interface.
 */
#define USB_CDC_COMM_INTERFACE_NUM 0U

/*
 * USB_CDC_INTERRUPT_EP
 * The address for the Communication Device Class (CDC) interrupt notification endpoint.
 */
#define USB_CDC_INTERRUPT_EP CDC_COMMUNICATION_INTERFACE_INTERRUPT_EP1_IN

/*
 * USB_CDC_BULK_EP_IN
 * The address for the CDC bulk IN endpoint.
 */
#define USB_CDC_BULK_EP_IN CDC_DATA_INTERFACE_BULK_EP2_IN

/*
 * USB_CDC_BULK_EP_OUT
 * The address for the CDC bulk OUT endpoint.
 */
#define USB_CDC_BULK_EP_OUT CDC_DATA_INTERFACE_BULK_EP2_OUT

/*
 * USB_CDC_DATA_ENDPOINT_SIZE
 * Controls the size of the CDC data endpoints.
 */
#define USB_CDC_DATA_ENDPOINT_SIZE CDC_DATA_INTERFACE_BULK_EP2_OUT_SIZE

/*
 * USB_CDC_TX_BUFFER_SIZE
 * Macro for the transmit buffer size.
 *
 * NOTE : Sized for sustained high-baud UART→USB throughput. The TX buffer accumulates bytes from
 *        the UART RX pin while a USB IN transfer is in flight. At 2 Mbps, ~200 bytes arrive per
 *        1 ms USB polling interval; 256 bytes gives a comfortable margin. The original 128 bytes
 *        (2 × MAX_ENDPOINT_SIZE_DEFAULT) was tight at 921600 baud and insufficient at 2 Mbps.
 */
#define USB_CDC_TX_BUFFER_SIZE 256U

/*
 * USB_CDC_RX_BUFFER_SIZE
 * Macro for the receive buffer size.
 *
 * NOTE : Sized to absorb burst USB OUT traffic (4 full 64-byte packets) while the UART TX drains
 *        it. Matched to USB_CDC_TX_BUFFER_SIZE so the bridge is symmetric under heavy bidirectional
 *        load. The original 128 bytes could stall the host at sustained 2 Mbps rates.
 */
#define USB_CDC_RX_BUFFER_SIZE 256U

/*
 * USB_CDC_RX_PACKET_SIZE
 * Macro for the receive packet size.
 */
#define USB_CDC_RX_PACKET_SIZE USB_CDC_DATA_ENDPOINT_SIZE

/*
 * USB_CDC_UNION_SUBORDINATE_NUM
 * Macro for the maximum number of configured subordinate interfaces of union functional descriptors.
 */
#define USB_CDC_UNION_SUBORDINATE_NUM 1u

/*
 * USB_INTERFACE_NUM
 * The number of interfaces used by a configuration, excluding alternate interfaces.
 */
#define USB_INTERFACE_NUM 2U


#endif // USB_CONFIG_H
