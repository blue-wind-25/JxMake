/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBCORETRANSFER USB Core Transfer Source File
 * USB core layer implementation file.
 * USB Core Version 1.0.0
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


#include <avr/interrupt.h>

#include "usb_core_transfer.h"


void USB_TransferAbort( USB_PIPE_t pipe )
{
    if( USB_PipeStatusIsBusy( pipe ) == true ) {
        USB_TransactionAbort( pipe );
        USB_PipeTransferEndCallback( pipe );
    }
}

static inline ALWAYS_INLINE void USB_TransferHandler( void )
{
    // If it's the initial setup packet, handle that separately.
    if( USB_SetupIsReceived() == true ) {
        USB_ControlSetupReceived();
    }
    // If a transaction is complete, handle that one.
    else if( USB_TransactionIsCompleted() == true ) {
        USB_PIPE_t pipe;
        if( SUCCESS == USB_TransactionCompletedPipeGet( &pipe ) ) {
            USB_TransactionCompleteAck( pipe );
            if( pipe.address == 0U ) {
                USB_ControlTransactionComplete( pipe );
            }
            else {
                USB_PipeTransactionComplete( pipe );
            }
        }
    }
}

ISR( USB0_TRNCOMPL_vect )
{
    USB_TransferHandler();
}
