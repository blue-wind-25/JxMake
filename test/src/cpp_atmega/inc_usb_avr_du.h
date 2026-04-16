/*
 * Standard USB enums and structs in this file follow the USB 2.0 specification.
 *
 * Other enums, structs, SFR usages, and most of the code flow in this file are adapted from
 * Microchip examples:
 *
 *     USB Communication Device Class (CDC) Data Logger with AVR DU
 *     https://github.com/microchip-pic-avr-examples/avr64du32-cnano-usb-cdc-datalogger-mplab-mcc
 *     https://github.com/microchip-pic-avr-examples/avr64du32-cnano-usb-cdc-datalogger-mplab-mcc/blob/main/LICENSE.txt
 *
 *     (C) 2024 Microchip Technology Inc. and its subsidiaries.
 *
 *     Subject to your compliance with these terms, you may use Microchip software
 *     and any derivatives exclusively with Microchip products. You're responsible
 *     for complying with 3rd party license terms applicable to your use of 3rd
 *     party software (including open source software) that may accompany Microchip
 *     software.
 *
 *     SOFTWARE IS "AS IS." NO WARRANTIES, WHETHER EXPRESS, IMPLIED OR STATUTORY,
 *     APPLY TO THIS SOFTWARE, INCLUDING ANY IMPLIED WARRANTIES OF NON-INFRINGEMENT,
 *     MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 *     IN NO EVENT WILL MICROCHIP BE LIABLE FOR ANY INDIRECT, SPECIAL, PUNITIVE,
 *     INCIDENTAL OR CONSEQUENTIAL LOSS, DAMAGE, COST OR EXPENSE OF ANY KIND
 *     WHATSOEVER RELATED TO THE SOFTWARE, HOWEVER CAUSED, EVEN IF MICROCHIP
 *     HAS BEEN ADVISED OF THE POSSIBILITY OR THE DAMAGES ARE FORESEEABLE. TO
 *     THE FULLEST EXTENT ALLOWED BY LAW, MICROCHIP'S TOTAL LIABILITY ON ALL
 *     CLAIMS RELATED TO THE SOFTWARE WILL NOT EXCEED AMOUNT OF FEES, IF ANY,
 *     YOU PAID DIRECTLY TO MICROCHIP FOR THIS SOFTWARE.
 */


#include <util/atomic.h>

extern "C" {
    #include "system.h"
    #include "usb_cdc.h"
    #include "usb_cdc_virtual_serial_port.h"
}


static volatile RETURN_CODE_t     status    = SUCCESS;
static volatile CDC_RETURN_CODE_t cdcStatus = CDC_SUCCESS;


static void USBDevice_TransferHandler()
{ USB_TransferHandler(); }

static void USBDevice_EventHandler()
{ USB_EventHandler(); }


static void USBDevice_CDCACMHandler()
{
    if(  USB_CDCVirtualSerialPortHandler() != SUCCESS ) return;
    if( !USB_CDCDataTerminalReady       ()            ) return;
    if(  USB_CDCTxBusy                  ()            ) return;

    static uint8_t cdcData;
    if( USB_CDCRead(&cdcData) == CDC_SUCCESS ) USB_CDCWrite(cdcData);
}


volatile uint32_t millisCnt = 0;

ISR(TCA0_OVF_vect)
{
    ++millisCnt;
    TCA0.SINGLE.INTFLAGS = TCA_SINGLE_OVF_bm;
}

static inline uint32_t millis()
{
    uint32_t m;

    ATOMIC_BLOCK(ATOMIC_RESTORESTATE)
    {
        m = millisCnt;
    }

    return m;
}

static inline void delayMS(uint32_t mS)
{
    const uint32_t start = millis();

    USBDevice_CDCACMHandler();

    while( millis() - start < mS );
}


static void usb_init()
{
    // Reinitialize OSCHF
    _PROTECTED_WRITE(CLKCTRL.OSCHFCTRLA, CLKCTRL.OSCHFCTRLA | CLKCTRL_ALGSEL_BIN_gc | CLKCTRL_AUTOTUNE_SOF_gc);
    _PROTECTED_WRITE(CLKCTRL.OSCHFTUNE , 0x00                                                                );

    while( !(CLKCTRL.MCLKSTATUS & CLKCTRL_OSCHFS_bm) );

    // Start USB
    SYSCFG_Initialize   ();
    USB0_Initialize     ();
    USBDevice_Initialize();

    USB0_TrnComplCallbackRegister(USBDevice_TransferHandler);
    USB0_BusEventCallbackRegister(USBDevice_EventHandler   );

    USB0.INTCTRLA |= USB_RESET_bm;    // Enable RESET    interrupt
    USB0.INTCTRLA |= USB_STALLED_bm;  // Enable STALLED  interrupt
    USB0.INTCTRLA |= USB_UNF_bm;      // Enable UNF      interrupt
    USB0.INTCTRLA |= USB_OVF_bm;      // Enable OVF      interrupt

    USB0.INTCTRLB |= USB_TRNCOMPL_bm; // Enable TRNCOMPL interrupt
    USB0.INTCTRLB |= USB_GNDONE_bm;   // Enable GNDONE   interrupt
    USB0.INTCTRLB |= USB_SETUP_bm;    // Enable SETUP    interrupt

    SYSCFG_UsbVregEnable();
    USB_Start           ();

    // Enable TCA0 in normal mode
    TCA0.SINGLE.CTRLA   = 0;
    TCA0.SINGLE.CNT     = 0;
    TCA0.SINGLE.PER     = (F_CPU / 64 / 1000) - 1;
    TCA0.SINGLE.INTCTRL = TCA_SINGLE_OVF_bm;
    TCA0.SINGLE.CTRLA   = TCA_SINGLE_CLKSEL_DIV64_gc | TCA_SINGLE_ENABLE_bm;

    // Initialize interrupt
    _PROTECTED_WRITE(CPUINT.CTRLA, 0x00);

    CPUINT.LVL0PRI = 0x00;
    CPUINT.LVL1VEC = 0x00;

    sei();
}
