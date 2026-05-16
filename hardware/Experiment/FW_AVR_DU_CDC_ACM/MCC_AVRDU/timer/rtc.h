/*
 * RTC Generated Driver API Header File.
 *
 * rtc.h
 *
 * rtc RTC
 *
 * This file contains the API prototypes of the RTC driver.
 *
 * RTC Driver Version 2.1.0
*/
/*
 [2025] Microchip Technology Inc. and its subsidiaries.

    Subject to your compliance with these terms, you may use Microchip 
    software and any derivatives exclusively with Microchip products. 
    You are responsible for complying with 3rd party license terms  
    applicable to your use of 3rd party software (including open source  
    software) that may accompany Microchip software. SOFTWARE IS ?AS IS.? 
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


#ifndef RTCDRIVER_H
#define RTCDRIVER_H

#include "../system/utils/compiler.h"
#include <stdint.h>
#include <stdbool.h>

/*
 * rtc
 * void RTC_cb_t
 * Function pointer to callback function called by the RTC. The default value is set to NULL which means that no callback function will be used.
 */ 
typedef void (*RTC_cb_t)(void);
/*
 * rtc
 * Interrupt Service Routine (ISR) callback function to be called if Overflow (OVF) Interrupt flag is set.
 *     RTC_cb_t cb - Callback function to be called on Overflow event.
 * return None.
 */ 
void RTC_SetOVFIsrCallback(RTC_cb_t cb);
/*
 * rtc
 * ISR callback function to be called if Compare (CMP) match Interrupt flag is set.
 *     RTC_cb_t cb - Callback function to be called on compare match event.
 * return None.
 */ 
void RTC_SetCMPIsrCallback(RTC_cb_t cb);
/*
 * rtc
 * ISR callback function to be called if the Periodic Interrupt Timer (PIT) Interrupt flag is set.
 *     RTC_cb_t cb - Callback function to be called on periodic interrupt event.
 * return None.
 */ 
void RTC_SetPITIsrCallback(RTC_cb_t cb);
/*
 * rtc
 * Initializes the RTC module.
 *     None.
 * 0 - the RTC initialization is successful
 * 1 - the RTC initialization is not successful
 */ 
int8_t RTC_Initialize(void);
/*
 * rtc
 * Starts the counter register for the RTC module.
 *     None.
 * return None.
 */
void RTC_Start(void);
/*
 * rtc
 * Stops the counter register for the RTC module.
 *     None.
 * return None.
 */
void RTC_Stop(void);
/*
 * rtc
 * Writes a value to the Counter register of the RTC module.
 *     uint16_t timerVal - Value to be written to the Counter register of the RTC.
 * return None.
 */
void RTC_WriteCounter(uint16_t timerVal);
/*
 * rtc
 * Writes a value to the Period register of the RTC module.
 *     uint16_t timerVal - Value to be written to the Period register of the RTC.
 * return None.
 */
void RTC_WritePeriod(uint16_t timerVal);
/*
 * rtc
 * Returns the counter value from the Counter register.
 *     None.
 * return uint16_t - Value of the Counter register.
 */
uint16_t RTC_ReadCounter(void);
/*
 * rtc
 * Returns the value of the Period register.
 *     None.
 * return uint16_t - Value of the Period register.
 */
uint16_t RTC_ReadPeriod(void);
/*
 * rtc
 * Enables the Compare (CMP) Interrupt.
 *     None.
 * return None.
 */
void RTC_EnableCMPInterrupt(void);
/*
 * rtc
 * Disables the CMP Interrupt.
 *     None.
 * return None.
 */
void RTC_DisableCMPInterrupt(void);
/*
 * rtc
 * Enables the Overflow (OVF) Interrupt. 
 *     None.
 * return None.
 */
void RTC_EnableOVFInterrupt(void);
/*
 * rtc
 * Disables the OVF Interrupt for the RTC module.
 *     None.
 * return None.
 */
void RTC_DisableOVFInterrupt(void);
/*
 * rtc
 * Enables the Periodic Interrupt Timer (PIT) interrupt for the RTC module. 
 *     None.
 * return None.
 */
void RTC_EnablePITInterrupt(void);
/*
 * rtc
 * Disables the PIT Interrupt for the RTC module.
 *     None.
 * return None.
 */
void RTC_DisablePITInterrupt(void);
/*
 * rtc
 * Clears the OVF Interrupt flag.
 *     None.
 * return None.
 */
void RTC_ClearOVFInterruptFlag(void);
/*
 * rtc
 * Checks if Overflow interrupt has occurred.
 *     None.
 * True - Interrupt is enabled.
 * False - Interrupt is disabled.
 */
bool RTC_IsOVFInterruptEnabled(void);
/*
 * rtc
 * Writes a value to the CMP register of the RTC module.
 *     uint16_t value - Value to be written to the CMP register of the RTC.
 * return None.
 */
void RTC_WriteCMPRegister(uint16_t value);
/*
 * rtc
 * Returns the value from the CMP register.
 *     None.
 * return uint16_t - Value of the Compare register.
 */
uint16_t RTC_ReadCMPRegister(void);


#endif /* RTCDRIVER_H */

/* */