/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __RING_BUFFER_H__
#define __RING_BUFFER_H__


#include <stdbool.h>
#include <stdint.h>

#include <pico/sync.h>


typedef struct {
             uint8_t*     buffer;
             uint32_t     size;
    volatile uint32_t     head;
    volatile uint32_t     tail;
             spin_lock_t* lock;
             uint32_t     lock_num;
} ring_buffer_t;


extern void ring_buffer_init(ring_buffer_t* rb, uint8_t* storage, uint32_t size, uint32_t lock_num);
extern void ring_buffer_clear(ring_buffer_t* rb);

extern bool ring_buffer_push(ring_buffer_t* rb, uint8_t data);
extern bool ring_buffer_pop(ring_buffer_t* rb, uint8_t* data);

extern uint32_t ring_buffer_count(ring_buffer_t* rb);
extern bool ring_buffer_empty(ring_buffer_t* rb);
extern bool ring_buffer_full(ring_buffer_t* rb);


#endif // __RING_BUFFER_H__
