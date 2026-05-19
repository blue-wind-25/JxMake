/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBPERIPHERALENDPOINT Peripheral Endpoint Source File
 * API module for usb_peripheral covering endpoint related functions.
 * USB Device Stack HAL Driver Version 1.0.0
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


#include <stdbool.h>

#include "usb_peripheral_avr_du.h"
#include "usb_peripheral_endpoint.h"


#if defined( USB_EP_NUM ) && defined( USB_MAX_ENDPOINTS )
    #if USB_EP_NUM > USB_MAX_ENDPOINTS
        #error "USB_EP_NUM is too large, max is USB_MAX_ENDPOINTS"
    #endif
#else
    #error "USB_EP_NUM not configured"
#endif

/*
 * Algorithm to detect if a given number is a power of two.
 * A number is a power of two if it has exactly one '1' in its binary representation. This is true if subtracting '1' from the number
 * and doing an AND operation on the result with the number itself returns 0.
 *     number 8-bit unsigned integer
 * True - The given number is a power of two
 * False - The given number is not a power of two
 */
#define IsPowerOfTwo( number ) ( ( 0u != ( number ) ) && ( ( ( number ) & ( ( number ) - 1u ) ) == 0u ) )

/*
 * SRAM tables for the FIFO and endpoint registers, as well as the FRAMENUM register.
 * Represents the endpoint configuration table based on the number of endpoints in use.
 * This line instantiates an object using the data structure type.
 */
USB_ENDPOINT_TABLE_t endpointTable __attribute__( ( aligned( 2 ) ) );

// Endpoint sizes are always powers of 2 and ≤ MAX_ENDPOINT_SIZE_DEFAULT — no validation needed
static inline void ConvertEndpointSizeToMask( uint16_t endpointSize, uint8_t* endpointMaskPtr )
{
    uint8_t  mask = 0;
    uint16_t baseSize = 8;
    while( baseSize < endpointSize ) {
        mask++;
        baseSize <<= 1;
    }
    *endpointMaskPtr = mask << USB_BUFSIZE_DEFAULT_gp;
}

void USB_EndpointConfigure( USB_PIPE_t pipe, uint16_t endpointSize, USB_ENDPOINT_t endpointType )
{
    uint8_t endpointConfiguration = 0;
    ConvertEndpointSizeToMask( endpointSize, &endpointConfiguration );

    // No ISO or invalid types used — CONTROL vs BULK/INTERRUPT only
    if( CONTROL == endpointType ) {
        endpointConfiguration |= USB_TYPE_CONTROL_gc;
    }
    else {
        endpointConfiguration |= USB_TYPE_BULKINT_gc;
    }

    if( USB_EP_DIR_OUT == pipe.direction ) {
        USB_EndpointOutNAKSet( pipe.address );
        USB_EndpointOutStatusClear( pipe.address );
        USB_NumberBytesReceivedReset( pipe.address );
        USB_EndpointOutControlSet( pipe.address, endpointConfiguration );

        // OutAzlpEnable=0 for all endpoints; OutTrncInterruptEnable=1 for all; OutMultipkt=1 for all configured OUT
        USB_EndpointOutMultipktEnable( pipe.address );
    }
    else {
        USB_EndpointInNAKSet( pipe.address );
        USB_EndpointInStatusClear( pipe.address );
        USB_NumberBytesToSendReset( pipe.address );
        USB_EndpointInControlSet( pipe.address, endpointConfiguration );

        // EP1 IN (interrupt) has InMultipktEnable=0; all others=1
        if( pipe.address != 1u ) USB_EndpointInMultipktEnable( pipe.address );
    }
}

uint16_t USB_EndpointSizeGet( USB_PIPE_t pipe )
{
    // No ISO endpoints — always use default size register
    uint8_t endpointSizeConfig;
    if( USB_EP_DIR_OUT == pipe.direction ) {
        endpointSizeConfig = USB_EndpointOutDefaultSizeGet( pipe.address );
    }
    else {
        endpointSizeConfig = USB_EndpointInDefaultSizeGet( pipe.address );
    }
    return 8U << (uint16_t) endpointSizeConfig;
}

USB_ENDPOINT_t USB_EndpointTypeGet( USB_PIPE_t pipe )
{
    USB_TYPE_t endpointConfigType;
    if( USB_EP_DIR_OUT == pipe.direction ) {
        endpointConfigType = USB_EndPointOutTypeConfigGet( pipe.address );
    }
    else { // USB_EP_DIR_IN
        endpointConfigType = USB_EndPointInTypeConfigGet( pipe.address );
    }

    USB_ENDPOINT_t endpointType = DISABLED;
    switch( endpointConfigType ) {
        case USB_TYPE_CONTROL_gc:
            endpointType = CONTROL;
            break;

        case USB_TYPE_BULKINT_gc:
            // Peripheral does not distinguish between BULK and INTERRUPT, returning BULK
            endpointType = BULK;
            break;

        case USB_TYPE_ISO_gc:
            endpointType = ISOCHRONOUS;
            break;

        default:
            // endpointType = DISABLED;
            break;
    } // switch

    return endpointType;
}

RETURN_CODE_t USB_EndpointStalledConditionAck( USB_PIPE_t pipe )
{
    RETURN_CODE_t status = UNINITIALIZED;

    if( (uint8_t) USB_EP_NUM <= pipe.address ) {
        status = ENDPOINT_ADDRESS_ERROR;
    }
    else {
        if( USB_EP_DIR_OUT == pipe.direction ) {
            USB_EndpointOutStallAck( pipe.address );
        }
        else {
            USB_EndpointInStallAck( pipe.address );
        }

        status = SUCCESS;
    }

    return status;
}

RETURN_CODE_t USB_DataToggle( USB_PIPE_t pipe )
{
    RETURN_CODE_t status = UNINITIALIZED;

    if( (uint8_t) USB_EP_NUM <= pipe.address ) {
        status = ENDPOINT_ADDRESS_ERROR;
    }
    else {
        if( USB_EP_DIR_OUT == pipe.direction ) {
            ( USB_EndpointOutDataToggleIsSet( pipe.address ) ) ? USB_DataToggleClear( pipe ) : USB_DataToggleSet( pipe );
        }
        else {
            ( USB_EndpointInDataToggleIsSet( pipe.address ) ) ? USB_DataToggleClear( pipe ) : USB_DataToggleSet( pipe );
        }

        status = SUCCESS;
    }

    return status;
}

void EndpointBufferSet( USB_PIPE_t pipe, uint8_t* bufAddress )
{
    if( USB_EP_DIR_OUT == pipe.direction ) {
        // Errata: Out transactions must be word aligned when using multipacket (always enabled for OUT)
        if( ( (uint16_t) bufAddress & 0x0001 ) != 0u ) return;
        USB_EndpointOutBufferSet( pipe.address, bufAddress );
    }
    else {
        USB_EndpointInBufferSet( pipe.address, bufAddress );
    }
}
