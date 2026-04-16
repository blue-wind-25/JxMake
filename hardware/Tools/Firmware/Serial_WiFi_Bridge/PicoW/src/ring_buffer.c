/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include "ring_buffer.h"


void ring_buffer_init(ring_buffer_t* rb, uint8_t* storage, uint32_t size, uint32_t lock_num)
{
    rb->buffer   = storage;
    rb->size     = size;
    rb->head     = 0;
    rb->tail     = 0;
    rb->lock_num = lock_num;
    rb->lock     = spin_lock_init(lock_num);
}


void ring_buffer_clear(ring_buffer_t* rb)
{
    const uint32_t save = spin_lock_blocking(rb->lock);

    rb->head = 0;
    rb->tail = 0;

    spin_unlock(rb->lock, save);
}


bool ring_buffer_push(ring_buffer_t* rb, uint8_t data)
{
          bool     result = false;
    const uint32_t save   = spin_lock_blocking(rb->lock);
    const uint32_t next   = (rb->head + 1) % rb->size;

    if(next != rb->tail) {
        rb->buffer[rb->head] = data;
        rb->head             = next;
        result               = true;
    }

    spin_unlock(rb->lock, save);

    return result;
}


bool ring_buffer_pop(ring_buffer_t* rb, uint8_t* data)
{
          bool     result = false;
    const uint32_t save   = spin_lock_blocking(rb->lock);

    if(rb->head != rb->tail) {
        *data    = rb->buffer[rb->tail];
        rb->tail = (rb->tail + 1) % rb->size;
        result   = true;
    }

    spin_unlock(rb->lock, save);

    return result;
}


uint32_t ring_buffer_count(ring_buffer_t* rb)
{
    const uint32_t save   = spin_lock_blocking(rb->lock);
    const uint32_t result = (rb->head >= rb->tail)
                          ? ( rb->head -  rb->tail             )
                          : ( rb->size - (rb->tail - rb->head) );

    spin_unlock(rb->lock, save);

    return result;
}


bool ring_buffer_empty(ring_buffer_t* rb)
{
    const uint32_t save   = spin_lock_blocking(rb->lock);
    const bool     result = (rb->head == rb->tail);

    spin_unlock(rb->lock, save);

    return result;
}


bool ring_buffer_full(ring_buffer_t* rb)
{
    const uint32_t save   = spin_lock_blocking(rb->lock);
    const bool     result = ( ( (rb->head + 1) % rb->size ) == rb->tail );

    spin_unlock(rb->lock, save);

    return result;
}
