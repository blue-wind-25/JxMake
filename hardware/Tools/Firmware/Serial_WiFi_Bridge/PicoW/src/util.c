/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#include <lwip/ip_addr.h>
#include <tusb.h>

#include "util.h"


struct tcp_pcb* tcp_client_connect(const char* ip_str, u16_t port, void* user_arg, tcp_connected_fn conn_cb, tcp_recv_fn recv_cb, tcp_sent_fn sent_cb, tcp_err_fn err_cb)
{
    struct tcp_pcb* pcb = tcp_new();
    if(!pcb) return NULL;

    ip_addr_t ip;
    if( !ipaddr_aton(ip_str, &ip) ) {
        tcp_close(pcb);
        return NULL;
    }

    tcp_arg(pcb, user_arg);

    if(recv_cb) tcp_recv(pcb, recv_cb);
    if(sent_cb) tcp_sent(pcb, sent_cb);
    if(err_cb ) tcp_err (pcb, err_cb );

    pcb->so_options |= SOF_KEEPALIVE; // Enable keep-alive
    pcb->keep_idle   = 2500;          // 2.5 seconds idle before first probe
    pcb->keep_intvl  = 1500;          // 1.5 second between probes
    pcb->keep_cnt    = 32;            // Send 32 probes before giving up

    const err_t err = tcp_connect(pcb, &ip, port, conn_cb);

    if(err != ERR_OK) {
        tcp_close(pcb);
        return NULL;
    }

    return pcb;
}


void tcp_client_disconnect(struct tcp_pcb* pcb)
{
    if(!pcb) return;

    tcp_arg (pcb, NULL);
    tcp_recv(pcb, NULL);
    tcp_sent(pcb, NULL);
    tcp_err (pcb, NULL);

    if( tcp_close(pcb) != ERR_OK ) tcp_abort(pcb);
}


err_t tcp_client_send(struct tcp_pcb* pcb, const void* data, u16_t len)
{
    if(!pcb || !data || !len) return ERR_ARG;

    const err_t err = tcp_write(pcb, data, len, TCP_WRITE_FLAG_COPY);

    if(err != ERR_OK) return err;

    return tcp_output(pcb);
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


void setup_tcp_pair(cdc_tcp_pair_t* pair, struct tcp_pcb* pcb, uint8_t cdc_itf, uint8_t* tx_storage, uint8_t* rx_storage, uint32_t buf_size, uint32_t tx_lock, uint32_t rx_lock)
{
    ring_buffer_init(&pair->tx_buffer, tx_storage, buf_size, tx_lock);
    ring_buffer_init(&pair->rx_buffer, rx_storage, buf_size, rx_lock);

    pair->cdc_itf = cdc_itf;
    pair->tcp_pcb = pcb;
    pair->sig_int = false;

    tcp_arg (pcb, pair        );
    tcp_recv(pcb, cdc_tcp_recv);
}


void cdc_tcp_clear_buffer(cdc_tcp_pair_t* pair)
{
    pair->sig_int = false;

    ring_buffer_clear(&pair->tx_buffer);
    ring_buffer_clear(&pair->rx_buffer);
}


err_t cdc_tcp_recv(void* arg, struct tcp_pcb* tpcb, struct pbuf* p, err_t err)
{
    cdc_tcp_pair_t* pair = (cdc_tcp_pair_t*) arg;

    if(!pair) {
        if(p) pbuf_free(p);
        return ERR_ARG;
    }

    if(!p) {
        tcp_client_disconnect(tpcb);
        pair->tcp_pcb = NULL;
        return ERR_OK;
    }

    if(err != ERR_OK) {
        pbuf_free(p);
        return err;
    }

    uint16_t accepted = 0;

    for(struct pbuf* q = p; q != NULL; q = q->next) {

        uint8_t* payload = (uint8_t*) q->payload;

        for(uint16_t i = 0; i < q->len; ++i) {

            if( ring_buffer_push(&pair->tx_buffer, payload[i]) ) {
                ++accepted;
            }
            else {
                tcp_recved(tpcb, accepted);
                return ERR_MEM;
            }

        } // for

    } // for

    tcp_recved(tpcb, accepted);
    pbuf_free(p);

    return ERR_OK;
}


void cdc_tcp_service(cdc_tcp_pair_t* pair, bool consoleMode)
{
    if( !pair || !pair->tcp_pcb             ) return;
    if( !tud_mounted()                      ) return;
    if( !tud_cdc_n_connected(pair->cdc_itf) ) return;

    // Buffer
    uint8_t buff[ (CFG_TUD_CDC_RX_BUFSIZE > CFG_TUD_CDC_TX_BUFSIZE) ? CFG_TUD_CDC_RX_BUFSIZE : CFG_TUD_CDC_TX_BUFSIZE ];

    // Step 1 - drain RX buffer into TCP first
    while( !ring_buffer_empty(&pair->rx_buffer) ) {

        uint16_t chunk_len = 0;

        while( chunk_len < sizeof(buff) && !ring_buffer_empty(&pair->rx_buffer) ) {
            ring_buffer_pop( &pair->rx_buffer, &buff[chunk_len] );
            ++chunk_len;
        } // while

        if(chunk_len > 0) {
            const err_t err = tcp_client_send(pair->tcp_pcb, buff, chunk_len);
            if(err != ERR_OK) {
                // Push back if TCP window full
                for(int i = chunk_len - 1; i >= 0; --i) ring_buffer_push(&pair->rx_buffer, buff[i]);
                break;
            }
        }

    } // while

    // Step 2 - read new data from CDC and try direct send
    if( tud_cdc_n_available(pair->cdc_itf) ) {

              uint8_t  tmpBuff[sizeof(buff)];
        const uint16_t count = tud_cdc_n_read( pair->cdc_itf, tmpBuff, sizeof(tmpBuff) );

        if(count > 0) {
            uint16_t send_len = 0;

            // Handle ^C and mouse filtering only in console mode
            if(consoleMode) {
                for(uint16_t i = 0; i < count; ++i) {

                    uint8_t c = tmpBuff[i];

                    // Handle Ctrl+C
                    if(c == 0x03) {
                        pair->sig_int = true;
                        buff[send_len++] = c;
                        break;
                    }

                    /* Mouse filtering logic:
                     *     ESC [ M ...
                     *     ESC [ < ...
                     * Colors (ESC [ 31 m) are ignored by these checks and pass through
                     *
                     * ##### !!! TODO : Improve it !!! #####
                     */
                    if( c == 0x1B && (i + 2 < count) && tmpBuff[i+1] == '[' ) {
                        if(tmpBuff[i + 2] == 'M') {
                            i += 5; // Skip 6-byte X10 sequence
                            continue;
                        }
                        else if(tmpBuff[i + 2] == '<') {
                            // SGR mouse: Skip until 'm' or 'M'
                            i += 2;
                            while(i < count && tmpBuff[i] != 'm' && tmpBuff[i] != 'M') ++i;
                            continue;
                        }
                    }

                    buff[send_len++] = c;

                } // for
            }
            else {
                // Raw bridge mode - no filtering, direct copy
                send_len = count;
                memcpy(buff, tmpBuff, count);
            }

            if(send_len > 0) {
                // Always send up to send_len (including ^C if found)
                const err_t err = tcp_client_send(pair->tcp_pcb, buff, send_len);
                if(err != ERR_OK) {
                    // TCP window full → buffer for later
                    for(uint16_t i = 0; i < send_len; ++i) {
                        if( !ring_buffer_push(&pair->rx_buffer, buff[i]) ) {
                            // RX buffer full → stop, remaining bytes stay in TinyUSB FIFO
                            break;
                        }
                    }
                }
                // If ^C was found, ignore the remainder of buff (they stay in TinyUSB FIFO or can be discarded)
            }
        }

    } // if

    // Step 3 - drain TX buffer into CDC host
    if(consoleMode && pair->sig_int) {
        // ^C received
        ring_buffer_clear(&pair->tx_buffer); // Clear buffer once
        pair->sig_int = false;               // Reset flag so normal operation resumes
    }
    else {
        while( !ring_buffer_empty(&pair->tx_buffer) ) {

            const uint16_t avail = tud_cdc_n_write_available(pair->cdc_itf);
            if(avail == 0) break;

            uint16_t chunk_len = 0;

            while( chunk_len < avail && chunk_len < sizeof(buff) && !ring_buffer_empty(&pair->tx_buffer) ) {
                ring_buffer_pop(&pair->tx_buffer, &buff[chunk_len]);
                ++chunk_len;
            }

            if(chunk_len > 0) {
                tud_cdc_n_write(pair->cdc_itf, buff, chunk_len);
                tud_cdc_n_write_flush(pair->cdc_itf);
            }

        } // while
    }
}


ring_buffer_t* msg_tcp_service(cdc_tcp_pair_t* pair, uint8_t* data, uint16_t len)
{
    if( !pair || !pair->tcp_pcb ) return NULL;

    // Buffer
    uint8_t buff[ (CFG_TUD_CDC_RX_BUFSIZE > CFG_TUD_CDC_TX_BUFSIZE) ? CFG_TUD_CDC_RX_BUFSIZE : CFG_TUD_CDC_TX_BUFSIZE ];

    // Step 1 - drain RX buffer into TCP first
    while( !ring_buffer_empty(&pair->rx_buffer) ) {

        uint16_t chunk_len = 0;

        while( chunk_len < sizeof(buff) && !ring_buffer_empty(&pair->rx_buffer) ) {
            ring_buffer_pop( &pair->rx_buffer, &buff[chunk_len] );
            ++chunk_len;
        } // while

        if(chunk_len > 0) {
            const err_t err = tcp_client_send(pair->tcp_pcb, buff, chunk_len);
            if(err != ERR_OK) {
                // Push back if TCP window full
                for(int i = chunk_len - 1; i >= 0; --i) ring_buffer_push(&pair->rx_buffer, buff[i]);
                break;
            }
        }

    } // while

    // Step 2 - process data from function parameter and try direct send
    if(data && len > 0) {
        const err_t err = tcp_client_send(pair->tcp_pcb, data, len);
        if(err != ERR_OK) {
            // TCP window full → buffer for later
            for(uint16_t i = 0; i < len; ++i) {
                if( !ring_buffer_push(&pair->rx_buffer, data[i]) ) {
                    // RX buffer full → stop, remaining bytes stay in TinyUSB FIFO
                    break;
                }
            }
        }
    }

    // Step 3 - Return the pointer to the tx_buffer for the caller to process
    return &pair->tx_buffer;
}
