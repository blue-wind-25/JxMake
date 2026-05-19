/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCORE CORE Source File
 * Core functionality for the USB stack.
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


void USB_Stop( void )
{
    USB_BusDetach();
    USB_PeripheralDisable();

    USB_PIPE_t pipe = {
        .address = 0
    };
    while( pipe.address < USB_EP_NUM ) {
        pipe.direction = USB_EP_DIR_OUT;
        USB_TransferAbort( pipe );
        pipe.direction = USB_EP_DIR_IN;
        USB_TransferAbort( pipe );
        pipe.address++;
    }
}

void USB_Reset( void )
{
    USB_Stop();
    USB_PeripheralInitialize();
    USB_ControlEndpointsInit();
    USB_ControlTransferReset();
    USB_BusAttach();
}
