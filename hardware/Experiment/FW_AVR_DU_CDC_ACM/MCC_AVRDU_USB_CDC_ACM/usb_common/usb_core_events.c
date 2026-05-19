/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCOREEVENTS USB Core Events Source File
 * Event handling for the USB Core Stack.
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
#include <avr/interrupt.h>

#include "usb_core.h"
#include "usb_core_events.h"


static void USB_EventHandler( void )
{
    if( USB_EventResetIsReceived() == true ) {
        USB_EventResetClear();
        USB_Reset();
    }
    uint8_t eventOverUnderflow = USB_EventOverUnderflowIsReceived();
    if( 0u < eventOverUnderflow ) {
        USB_EventOverUnderflowClear();
        uint8_t controlOverUnderflow = USB_ControlOverUnderflowIsReceived();
        if( 0u < controlOverUnderflow ) USB_ControlProcessOverUnderflow( controlOverUnderflow );
    }
    if( USB_EventStalledIsReceived() == true ) {
        USB_EventStalledClear();
        USB_HandleEventStalled( (USB_PIPE_t) { .address = 0x00, .direction = USB_EP_DIR_OUT } );
    }
}

ISR( USB0_BUSEVENT_vect )
{
    USB_EventHandler();
}
