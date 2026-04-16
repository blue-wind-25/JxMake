/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#ifndef __UTIL_H__
#define __UTIL_H__


#include <stdbool.h>
#include <stdint.h>

#include <lwip/tcp.h>
#include <pico/stdlib.h>
#include <pico/time.h>

#include "ring_buffer.h"


#define __no_return__                   __attribute__((noreturn))
#define __always_inline__ static inline __attribute__((always_inline))


__always_inline__ uint32_t millis(void)
{ return to_ms_since_boot( get_absolute_time() ); }


typedef err_t (*tcp_recv_fn)(void* arg, struct tcp_pcb* tpcb, struct pbuf* p, err_t err);
typedef err_t (*tcp_sent_fn)(void* arg, struct tcp_pcb* tpcb, u16_t len);
typedef err_t (*tcp_connected_fn)(void* arg, struct tcp_pcb* tpcb, err_t err);
typedef void  (*tcp_err_fn)(void* arg, err_t err);
typedef err_t (*tcp_poll_fn)(void* arg, struct tcp_pcb* tpcb);

extern struct tcp_pcb* tcp_client_connect(const char* ip_str, u16_t port, void* user_arg, tcp_connected_fn conn_cb, tcp_recv_fn recv_cb, tcp_sent_fn sent_cb, tcp_err_fn err_cb);
extern void tcp_client_disconnect(struct tcp_pcb* pcb);
extern err_t tcp_client_send(struct tcp_pcb* pcb, const void* data, u16_t len);


typedef struct {
           uint8_t       cdc_itf;   // CDC interface number
    struct tcp_pcb*      tcp_pcb;   // Associated TCP connection
           ring_buffer_t tx_buffer; // TCP → CDC
           ring_buffer_t rx_buffer; // CDC → TCP
           bool          sig_int;   // Flag for ^C
} cdc_tcp_pair_t;

extern void setup_tcp_pair(cdc_tcp_pair_t* pair, struct tcp_pcb* pcb, uint8_t cdc_itf, uint8_t* tx_storage, uint8_t* rx_storage, uint32_t buf_size, uint32_t tx_lock, uint32_t rx_lock);
extern void cdc_tcp_clear_buffer(cdc_tcp_pair_t* pair);

extern err_t cdc_tcp_recv(void* arg, struct tcp_pcb* tpcb, struct pbuf* p, err_t err);
extern void cdc_tcp_service(cdc_tcp_pair_t* pair, bool consoleMode);

extern ring_buffer_t* msg_tcp_service(cdc_tcp_pair_t* pair, uint8_t* data, uint16_t len);


#endif // __UTIL_H__

