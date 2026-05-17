/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * AC0 Generated Driver API Header File
 *
 * ac0.h
 *
 *  ac0 AC0
 *
 * Contains the API prototypes for the AC0 driver.
 *
 * AC0 Driver Version 1.0.0
*/
/*
(C) [2025] Microchip Technology Inc. and its subsidiaries.

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


#ifndef AC0_H_INCLUDED
#define AC0_H_INCLUDED

#include "../system/utils/compiler.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * ac0
 * Pointer to a function to be used as a callback handler when a comparator interrupt occurs.
 *     None.
 * return None.
 */
typedef void (*ac_cb_t)(void);

/*
 * ac0
 * Initializes the AC0. This routine is called only once during system initialization, before calling other APIs.
 *     None.
 * return None.
*/
int8_t AC0_Initialize(void);

/*
 * ac0
 * Controls the input signal to the positive and negative inputs of the AC0.
 * Initialize the AC0 with AC0_Initialize() before calling this API.
 *     uint8_t Mode.
 * return None.
 */
void AC0_MuxSet(uint8_t);

/*
 * ac0
 * Returns the OUT signal state for the AC0.
 * Initialize the AC0 with AC0_Initialize() before calling this API.
 *     None.
 * True - The AC OUT signal is high.
 * False - the AC OUT signal is low.
 */
bool AC0_Read(void);

/*
 * ac0
 * Setter function for the AC0 comparator callback.
 * None.
 *     CallbackHandler - Pointer to custom callback.
 * return None.
 */
void AC0_CallbackRegister(ac_cb_t);

/*
 * ac0
 * Sets the DAC voltage reference (DACREF) data value for the AC0.
 * Initialize the AC0 with AC0_Initialize() before calling this API.
 *     uint8_t- Value to be written to the DACREF register.
 * return None.
 */
void AC0_DACRefValueSet(uint8_t);

#ifdef __cplusplus
}
#endif

#endif  /* AC0_H_INCLUDED */