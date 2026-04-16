#include <btstack.h>

#include <pico/cyw43_arch.h>


#define HEARTBEAT_PERIOD_MS 1000


extern async_at_time_worker_t heartbeat_worker;

extern void packet_handler(uint8_t packet_type, uint16_t channel, uint8_t* packet, uint16_t size);

extern uint16_t att_read_callback(hci_con_handle_t connection_handle, uint16_t att_handle, uint16_t offset, uint8_t* buffer, uint16_t buffer_size);
extern int att_write_callback(hci_con_handle_t connection_handle, uint16_t att_handle, uint16_t transaction_mode, uint16_t offset, uint8_t* buffer, uint16_t buffer_size);
