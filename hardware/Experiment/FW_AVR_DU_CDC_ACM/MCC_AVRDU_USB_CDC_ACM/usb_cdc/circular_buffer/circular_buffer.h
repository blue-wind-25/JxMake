/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * CIRCULARBUFFER CDC Circular Buffer Header File
 * This file contains prototypes and datatypes for a circular buffer.
 * USB Device Stack Driver Version 1.0.0
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


#ifndef CIRCULAR_BUFFER_H_
#define CIRCULAR_BUFFER_H_


#include <stdint.h>
#include <stdbool.h>


/*
 * Type define for circular buffer return codes.
 */
typedef enum BUFFER_RETURN_CODE_enum {
    BUFFER_SUCCESS = 0,  // Action successfully executed
    BUFFER_FULL    = -1, // Error triggered by full buffer
    BUFFER_EMPTY   = -2  // Error triggered by empty buffer
} BUFFER_RETURN_CODE_t;

/*
 * Type define for circular buffers of varying length.
 */
typedef struct CIRCULAR_BUFFER_struct {
    uint8_t*       content;   // Actual buffer data
    uint16_t       head;      // Index of first empty slot in buffer
    uint16_t       tail;      // Index of first occupied buffer slot
    const uint16_t maxLength; // Maximum length of buffer
} CIRCULAR_BUFFER_t;

/*
 * Adds input data to circular buffer if there is space available.
 *     buffer - Circular buffer address
 *     data - Intput data
 * return status - Result of the addition process
 */
static inline void CIRCBUF_Enqueue( CIRCULAR_BUFFER_t* buffer, uint8_t data )
{
    // maxLength is always a power of 2 (128); bitwise-AND replaces branch+compare
    uint16_t nextHead = (buffer->head + 1U) & (buffer->maxLength - 1U);
    if( buffer->tail == nextHead ) return;
    buffer->content[buffer->head] = data;
    buffer->head                  = nextHead;
}

/*
 * Pulls data from the circular buffer if it's available.
 *     buffer - Circular buffer address
 *     data - Output data variable address
 * return status - Result of the retrieval process
 */
static inline BUFFER_RETURN_CODE_t CIRCBUF_Dequeue( CIRCULAR_BUFFER_t* buffer, uint8_t* data )
{
    if( buffer->head == buffer->tail ) return BUFFER_EMPTY;
    // maxLength is always a power of 2 (128); bitwise-AND replaces branch+compare
    uint16_t nextTail = (buffer->tail + 1U) & (buffer->maxLength - 1U);
    *data        = buffer->content[buffer->tail];
    buffer->tail = nextTail;
    return BUFFER_SUCCESS;
}

/*
 * Checks if the circular buffer is empty.
 *     buffer - Circular buffer address
 * 0 - Buffer not full
 * 1 - Buffer full
 */
static inline bool CIRCBUF_Empty( CIRCULAR_BUFFER_t* buffer ) { return buffer->head == buffer->tail; }

/*
 * Checks if the circular buffer is full.
 *     buffer - Circular buffer address
 * 0 - Buffer not full
 * 1 - Buffer full
 */
static inline bool CIRCBUF_Full( CIRCULAR_BUFFER_t* buffer )
{
    // maxLength is always a power of 2 (128); bitwise-AND replaces branch+compare
    uint16_t nextHead = (buffer->head + 1U) & (buffer->maxLength - 1U);
    return buffer->tail == nextHead;
}

/*
 * Returns the number of available bytes in the circular buffer.
 *     buffer - Circular buffer address
 * return freeSpace - Available bytes
 */
static inline uint16_t CIRCBUF_FreeSpace( CIRCULAR_BUFFER_t* buffer )
{
    if( buffer->head >= buffer->tail ) return buffer->tail + buffer->maxLength - buffer->head - 1U;
    return buffer->tail - buffer->head - 1U;
}


#endif // CIRCULAR_BUFFER_H_
