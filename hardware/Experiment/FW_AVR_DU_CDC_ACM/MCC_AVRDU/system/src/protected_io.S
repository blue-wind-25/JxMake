/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * protected_io Source Code File
 *
 * protected_io.S
 *
 * config_bitsdriver
 *
 * This file contains the generated protected_io source code file for the CONFIGURATION BITS.
 *
 * Driver Version 1.0.0
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


#include "../utils/assembler.h"

    PUBLIC_FUNCTION(protected_write_io)

#if !defined(__GNUC__)
#error This file requires GCC assembler
#endif

#ifdef RAMPZ
    out    _SFR_IO_ADDR(RAMPZ), r1   // Clear bits 23:16 of Z
#endif
    movw   r30, r24                  // Load addr into Z
    out    CCP, r22                  // Start CCP handshake
    st     Z  , r20                  // Write value to I/O register
    ret                              // Return to caller

    END_FUNC(protected_write_io)
    END_FILE()
