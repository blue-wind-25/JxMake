/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * USBPERIPHERAL Peripheral Header File
 * Interface for a USB peripheral module that needs to be implemented by a device-specific USB module driver.
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


#ifndef USB_PERIPHERAL_H
#define USB_PERIPHERAL_H


#include "usb_peripheral_avr_du.h"
#include "usb_peripheral_endpoint.h"
#include "usb_peripheral_read_write.h"


/*
 * The data structure for internally handling control transfers, either IN or OUT.
 */
typedef struct USB_CONTROL_TRANSFER_struct {
    uint8_t buffer[64];                                     // Default buffer for control data transfers
    volatile USB_CONTROL_STATUS_t status;                   // The status of a transfer on this pipe
    uint8_t* transferDataPtr;                               // Location in RAM to send or fill during transfer
    uint16_t transferDataSize;                              // Number of bytes to transfer to or from RAM location
    USB_SETUP_ENDOFREQUEST_CALLBACK_t endOfRequestCallback; // Callback to call when a setup request is complete
    USB_SETUP_REQUEST_t setupRequest;                       // Setup request packet
} USB_CONTROL_TRANSFER_t;

// Shared control transfer state — defined in usb_peripheral.c
extern USB_CONTROL_TRANSFER_t controlTransfer;

/*
 * Detects if the Setup event was received.
 *     None.
 * 0 - Setup event was not received
 * 1 - Setup event was received
 */
static inline bool USB_SetupIsReceived( void ) { return USB_SetupInterruptIs(); }

/*
 * Detects if the Start-of-Frame (SOF) event was received.
 *     None.
 * 0 - SOF event was not received
 * 1 - SOF event was received
 */
bool USB_EventSOFIsReceived( void );

/*
 * Clears the SOF event.
 *     None.
 * return None.
 */
void USB_EventSOFClear( void );

/*
 * Detects if the Reset event was received.
 *     None.
 * 1 - Reset event was received
 * 0 - Reset event was not received
 */
static inline bool USB_EventResetIsReceived( void ) { return USB_ResetInterruptIs(); }

/*
 * Clears the Reset event.
 *     None.
 * return None.
 */
static inline void USB_EventResetClear( void ) { USB_ResetInterruptClear(); }

/*
 * Detects if an Overflow and/or Underflow event was received.
 *     None.
 * return A value representing the events received
 */
uint8_t USB_EventOverUnderflowIsReceived( void );

/*
 * Detects if an Overflow and/or Underflow event was received on the control endpoints.
 *     None.
 * return A value representing the events received
 */
uint8_t USB_ControlOverUnderflowIsReceived( void );

/*
 * Clears the Over/Underflow event.
 *     None.
 * return None.
 */
static inline void USB_EventOverUnderflowClear( void ) { USB_OverflowInterruptClear(); USB_UnderflowInterruptClear(); }

/*
 * Detects if a Suspend event was received.
 *     None.
 * return A boolean value representing the Suspend event received condition
 * 0 - Suspend event was not received
 * 1 - Suspend event was received
 */
bool USB_EventSuspendIsReceived( void );

/*
 * Clears the Suspend event.
 *     None.
 * return None.
 */
void USB_EventSuspendClear( void );

/*
 * Detects if a Resume event was received.
 *     None.
 * 0 - Resume event was not received
 * 1 - Resume event was received
 */
bool USB_EventResumeIsReceived( void );

/*
 * Clears the Resume event.
 *     None.
 * return None.
 */
void USB_EventResumeClear( void );

/*
 * Detects if a Stalled event was received.
 *     None.
 * 0 - Stalled event was not received
 * 1 - Stalled event was received
 */
static inline bool USB_EventStalledIsReceived( void ) { return USB_StalledInterruptIs(); }

/*
 * Clears the Stalled event.
 *     None.
 * return None.
 */
static inline void USB_EventStalledClear( void ) { USB_StalledInterruptClear(); }

/*
 * Attaches the device to the USB bus.
 *     None.
 * return None.
 */
static inline void USB_BusAttach( void ) { USB_ConnectionAttach(); }

/*
 * Detaches the device from the USB bus.
 *     None.
 * return None.
 */
static inline void USB_BusDetach( void ) { USB_ConnectionDetach(); }

/*
 * Checks if the device is attached to the USB bus not.
 *     None.
 * 0 - USB bus is not attached
 * 1 - USB bus is attached
 */
bool USB_IsBusAttached( void );

/*
 * Sets the device address.
 *     deviceAddress - Device address to set
 * return None.
 */
void USB_DeviceAddressConfigure( uint8_t deviceAddress );

/*
 * Gets the current frame number.
 *     None.
 * return 15-bit frame number
 */
uint16_t USB_FrameNumberGet( void );

/*
 * Ensures correct control endpoint initialization.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
static inline void USB_ControlEndpointsInit( void )
{
    USB_PIPE_t controlPipeOut;
    controlPipeOut.address   = 0u;
    controlPipeOut.direction = USB_EP_DIR_OUT;
    USB_PIPE_t controlPipeIn;
    controlPipeIn.address   = 0u;
    controlPipeIn.direction = USB_EP_DIR_IN;

    USB_EndpointConfigure( controlPipeOut, USB_EP0_SIZE, CONTROL );
    USB_EndpointConfigure( controlPipeIn,  USB_EP0_SIZE, CONTROL );
    EndpointBufferSet( controlPipeOut, controlTransfer.buffer );
    EndpointBufferSet( controlPipeIn,  controlTransfer.buffer );
    USB_DataToggleClear( controlPipeOut );
    USB_DataToggleSet( controlPipeIn );
    controlTransfer.status = USB_CONTROL_SETUP;
}

/*
 * Verifies the received control setup.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_ControlSetupReceived( void );

/*
 * Handles completed transactions on the control endpoints. Checks and verifies data OUT, data IN, ZLP OUT and ZLP IN.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_ControlTransactionComplete( USB_PIPE_t pipe );

/*
 * Sends ZLP OUT and ZLP IN transactions on the control endpoints.
 *     direction - The endpoint direction to send the ZLP
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_ControlTransferZLP( uint8_t direction );

/*
 * Ensures correct control transfer reset.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_ControlTransferReset( void );

/*
 * Updates the transfer data pointer and size in ControlTransfer.
 *     *dataPtr - Pointer to new data
 *     dataSize - Number of elements in the array
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
static inline RETURN_CODE_t USB_ControlTransferDataSet( uint8_t* dataPtr, uint16_t dataSize ) { controlTransfer.transferDataPtr = dataPtr; controlTransfer.transferDataSize = dataSize; return SUCCESS; }

/*
 * Copies data to the transfer buffer and sets the transfer data pointer and size in ControlTransfer.
 *     *dataPtr - Pointer to data to copy
 *     dataSize - Number of elements in the array
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlTransferDataWriteBuffer( uint8_t* dataPtr, uint8_t dataSize );

/*
 * Sets the callback for end of a control request.
 *     callback - The function to call for the end of a control request
 * return None.
 */
static inline void USB_ControlEndOfRequestCallbackRegister( USB_SETUP_ENDOFREQUEST_CALLBACK_t callback ) { controlTransfer.endOfRequestCallback = callback; }

/*
 * Sets the callback for a control overrun or underrun.
 *     callback - The function to call on a control overrun or underrun
 * return None.
 */

/*
 * Handles the control Over/Underflow events.
 *     overunderflow - A value representing overflow or underflow
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
void USB_ControlProcessOverUnderflow( uint8_t overunderflow );

/*
 * Handles the Stall events.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
static inline void USB_HandleEventStalled( USB_PIPE_t pipe )
{
    USB_EndpointInStallAck( pipe.address );
    USB_EndpointOutStallAck( pipe.address );
    USB_EndpointInStallClear( pipe.address );
    USB_EndpointOutStallClear( pipe.address );
    USB_ControlTransferReset();
}

/*
 * Enables the peripheral and the frame number, enables and resets FIFO, sets the endpoint table address and max endpoints.
 *     None.
 * return None.
 */
void USB_PeripheralInitialize( void );

/*
 * Disables the USB peripheral and aborts any ongoing transaction.
 *     None.
 * return None.
 */
static inline void USB_PeripheralDisable( void ) { USB_Disable(); USB_DeviceAddressReset(); }


#endif // USB_PERIPHERAL_H
