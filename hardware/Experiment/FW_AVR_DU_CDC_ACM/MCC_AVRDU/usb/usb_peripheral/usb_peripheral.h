/*
 * USBPERIPHERAL Peripheral Header File
 * usb_peripheral.h
 * usb_peripheral USB Peripheral Hardware Abstraction Layer (HAL)
 * Interface for a USB peripheral module that needs to be implemented by a device-specific USB module driver.
 * USB Device Stack HAL Driver Version 1.0.0
 */

/*
    (c) 2021 Microchip Technology Inc. and its subsidiaries.

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
// cppcheck-suppress misra-c2012-2.5
#define USB_PERIPHERAL_H

#include <stdbool.h>
#include <stdint.h>

#include <usb_common_elements.h>
#include <usb_peripheral_endpoint.h>
#include <usb_peripheral_read_write.h>
#include <usb_protocol_headers.h>

/*
 * usb_peripheral
 * USB_CONTROL_TRANSFER_t
 * The data structure for internally handling control transfers, either IN or OUT.
 */
typedef struct USB_CONTROL_TRANSFER_struct
{
    uint8_t buffer[64];                                     /*<Default buffer for control data transfers*/
    volatile USB_CONTROL_STATUS_t status;                   /*<The status of a transfer on this pipe*/
    uint8_t *transferDataPtr;                               /*<Location in RAM to send or fill during transfer*/
    uint16_t transferDataSize;                              /*<Number of bytes to transfer to or from RAM location*/
    uint16_t totalBytesTransferred;                         /*<Number of data transfered last transaction*/
    USB_SETUP_OVERUNDERRUN_CALLBACK_t overUnderRunCallback; /*<Callback to call on a control overrun or underrun*/
    USB_SETUP_ENDOFREQUEST_CALLBACK_t endOfRequestCallback; /*<Callback to call when a setup request is complete*/
    USB_SETUP_REQUEST_t setupRequest;                       /*<Setup request packet*/
} USB_CONTROL_TRANSFER_t;

/*
 * usb_peripheral
 * Detects if the Setup event was received.
 *     None.
 * 0 - Setup event was not received
 * 1 - Setup event was received
 */
bool USB_SetupIsReceived(void);

/*
 * usb_peripheral
 * Detects if the Start-of-Frame (SOF) event was received.
 *     None.
 * 0 - SOF event was not received
 * 1 - SOF event was received
 */
bool USB_EventSOFIsReceived(void);

/*
 * usb_peripheral
 * Clears the SOF event.
 *     None.
 * return None.
 */
void USB_EventSOFClear(void);

/*
 * usb_peripheral
 * Detects if the Reset event was received.
 *     None.
 * 1 - Reset event was received
 * 0 - Reset event was not received
 */
bool USB_EventResetIsReceived(void);

/*
 * usb_peripheral
 * Clears the Reset event.
 *     None.
 * return None.
 */
void USB_EventResetClear(void);

/*
 * usb_peripheral
 * Detects if an Overflow and/or Underflow event was received.
 *     None.
 * return A value representing the events received
 */
uint8_t USB_EventOverUnderflowIsReceived(void);

/*
 * usb_peripheral
 * Detects if an Overflow and/or Underflow event was received on the control endpoints.
 *     None.
 * return A value representing the events received
 */
uint8_t USB_ControlOverUnderflowIsReceived(void);

/*
 * usb_peripheral
 * Clears the Over/Underflow event.
 *     None.
 * return None.
 */
void USB_EventOverUnderflowClear(void);

/*
 * usb_peripheral
 * Detects if a Suspend event was received.
 *     None.
 * return A boolean value representing the Suspend event received condition
 * 0 - Suspend event was not received
 * 1 - Suspend event was received
 */
bool USB_EventSuspendIsReceived(void);

/*
 * usb_peripheral
 * Clears the Suspend event.
 *     None.
 * return None.
 */
void USB_EventSuspendClear(void);

/*
 * usb_peripheral
 * Detects if a Resume event was received.
 *     None.
 * 0 - Resume event was not received
 * 1 - Resume event was received
 */
bool USB_EventResumeIsReceived(void);

/*
 * usb_peripheral
 * Clears the Resume event.
 *     None.
 * return None.
 */
void USB_EventResumeClear(void);

/*
 * usb_peripheral
 * Detects if a Stalled event was received.
 *     None.
 * 0 - Stalled event was not received
 * 1 - Stalled event was received
 */
bool USB_EventStalledIsReceived(void);

/*
 * usb_peripheral
 * Clears the Stalled event.
 *     None.
 * return None.
 */
void USB_EventStalledClear(void);

/*
 * usb_peripheral
 * Attaches the device to the USB bus.
 *     None.
 * return None.
 */
void USB_BusAttach(void);

/*
 * usb_peripheral
 * Detaches the device from the USB bus.
 *     None.
 * return None.
 */
void USB_BusDetach(void);

/*
 * usb_peripheral
 * Checks if the device is attached to the USB bus not.
 *     None.
 * 0 - USB bus is not attached
 * 1 - USB bus is attached
 */
bool USB_IsBusAttached(void);

/*
 * usb_peripheral
 * Sets the device address.
 *     deviceAddress - Device address to set
 * return None.
 */
void USB_DeviceAddressConfigure(uint8_t deviceAddress);

/*
 * usb_peripheral
 * Gets the current frame number.
 *     None.
 * return 15-bit frame number
 */
uint16_t USB_FrameNumberGet(void);

/*
 * usb_peripheral
 * Ensures correct control endpoint initialization.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlEndpointsInit(void);

/*
 * usb_peripheral
 * Verifies the received control setup.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlSetupReceived(void);

/*
 * usb_peripheral
 * Handles completed transactions on the control endpoints. Checks and verifies data OUT, data IN, ZLP OUT and ZLP IN.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlTransactionComplete(USB_PIPE_t pipe);

/*
 * usb_peripheral
 * Sends ZLP OUT and ZLP IN transactions on the control endpoints.
 *     direction - The endpoint direction to send the ZLP
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlTransferZLP(uint8_t direction);

/*
 * usb_peripheral
 * Ensures correct control transfer reset.
 *     None.
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlTransferReset(void);

/*
 * usb_peripheral
 * Updates the transfer data pointer and size in ControlTransfer.
 *     *dataPtr - Pointer to new data
 *     dataSize - Number of elements in the array
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlTransferDataSet(uint8_t *dataPtr, uint16_t dataSize);

/*
 * usb_peripheral
 * Copies data to the transfer buffer and sets the transfer data pointer and size in ControlTransfer.
 *     *dataPtr - Pointer to data to copy
 *     dataSize - Number of elements in the array
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlTransferDataWriteBuffer(uint8_t *dataPtr, uint8_t dataSize);

/*
 * usb_peripheral
 * Sets the callback for end of a control request.
 *     callback - The function to call for the end of a control request
 * return None.
 */
void USB_ControlEndOfRequestCallbackRegister(USB_SETUP_ENDOFREQUEST_CALLBACK_t callback);

/*
 * usb_peripheral
 * Sets the callback for a control overrun or underrun.
 *     callback - The function to call on a control overrun or underrun
 * return None.
 */
void USB_ControlOverUnderRunCallbackRegister(USB_SETUP_OVERUNDERRUN_CALLBACK_t callback);

/*
 * usb_peripheral
 * Handles the control Over/Underflow events.
 *     overunderflow - A value representing overflow or underflow
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_ControlProcessOverUnderflow(uint8_t overunderflow);

/*
 * usb_peripheral
 * Handles the Stall events.
 *     pipe - A combination of endpoint address and direction
 * return SUCCESS or an Error code according to RETURN_CODE_t
 */
RETURN_CODE_t USB_HandleEventStalled(USB_PIPE_t pipe);

/*
 * usb_peripheral
 * Enables the peripheral and the frame number, enables and resets FIFO, sets the endpoint table address and max endpoints.
 *     None.
 * return None.
 */
void USB_PeripheralInitialize(void);

/*
 * usb_peripheral
 * Disables the USB peripheral and aborts any ongoing transaction.
 *     None.
 * return None.
 */
void USB_PeripheralDisable(void);

#endif /* USB_PERIPHERAL_H */
