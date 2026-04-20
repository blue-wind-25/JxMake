/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Ha Thach (tinyusb.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */


#include <bsp/board.h>
#include <hardware/watchdog.h>
#include <lwip/ip4_addr.h>
#include <lwip/netif.h>
#include <pico/cyw43_arch.h>
#include <tusb.h>

#include "led.h"


#define RUN_MODE_GPIO             22
#define SYNC_RUN_GPIO             15

#define CDC_BUFF_SIZE             16384
#define MON_BUFF_SIZE             128

#define WIFI_CONNECT_TIMEOUT_MS   25000 // 25 seconds
#define WIFI_GET_IP_TIMEOUT_MS    15000 // 15 seconds
#define SERVER_CONNECT_TIMEOUT_MS 10000 // 10 seconds
#define USB_CONNECT_TIMEOUT_MS    20000 // 20 seconds


static void usbCDCACMTask(void);
static __no_return__ void cleanupAndBlinkErrorLED(ErrorBlinkPattern err_led);

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


// A flag that indicates if it is running in Console mode
static bool consoleMode = false;


// CDC #0 - Bridge #0 or Bridge #2
static struct tcp_pcb*       cdc0PCB       = NULL;
static        bool           cdc0Connected = false;
static        uint8_t        cdc0TXStorage[CDC_BUFF_SIZE];
static        uint8_t        cdc0RXStorage[CDC_BUFF_SIZE];
static        cdc_tcp_pair_t cdc0TCPPair;

static err_t cdc0ConnectedCB(void* arg, struct tcp_pcb* tpcb, err_t err)
{
    if(err == ERR_OK) cdc0Connected = true;
    else              tcp_client_disconnect(tpcb); // Connection failed

    return ERR_OK;
}

static void cdc0ErrorCB(void* arg, err_t err)
{
    (void) err;

    cdc_tcp_pair_t* pair = (cdc_tcp_pair_t*) arg;

    cdc0Connected = false;
    cdc0PCB       = NULL;

    if(pair) pair->tcp_pcb = NULL;
}


// CDC #1 - Bridge #1 or Console
static struct tcp_pcb*       cdc1PCB       = NULL;
static        bool           cdc1Connected = false;
static        uint8_t        cdc1TXStorage[CDC_BUFF_SIZE];
static        uint8_t        cdc1RXStorage[CDC_BUFF_SIZE];
static        cdc_tcp_pair_t cdc1TCPPair;

static err_t cdc1ConnectedCB(void* arg, struct tcp_pcb* tpcb, err_t err)
{
    if(err == ERR_OK) cdc1Connected = true;
    else              tcp_client_disconnect(tpcb); // Connection failed

    return ERR_OK;
}

static void cdc1ErrorCB(void* arg, err_t err)
{
    (void) err;

    cdc_tcp_pair_t* pair = (cdc_tcp_pair_t*) arg;

    cdc1Connected = false;
    cdc1PCB       = NULL;

    if(pair) pair->tcp_pcb = NULL;
}

// Monitoring
static struct tcp_pcb*       monPCB       = NULL;
static        bool           monAvailable = false;
static        bool           monConnected = false;
static        uint8_t        monTXStorage[MON_BUFF_SIZE];
static        uint8_t        monRXStorage[MON_BUFF_SIZE];
static        cdc_tcp_pair_t monTCPPair;

static err_t monConnectedCB(void* arg, struct tcp_pcb* tpcb, err_t err)
{
    if(err == ERR_OK) monConnected = true;
    else              tcp_client_disconnect(tpcb); // Connection failed

    return ERR_OK;
}

static void monErrorCB(void* arg, err_t err)
{
    (void) err;

    cdc_tcp_pair_t* pair = (cdc_tcp_pair_t*) arg;

    monConnected = false;
    monPCB       = NULL;

    if(pair) pair->tcp_pcb = NULL;
}

////////////////////////////////////////////////////////////////////////////////////////////////////


static bool usbConnected = false;


static void usbDisconnect()
{
    if(!usbConnected) return;

    tud_disconnect();
    sleep_ms(500);

    usbConnected = false;
}


static void usbConnect()
{
    if(usbConnected) return;

    tud_connect();

    const uint32_t usbTimeoutMS = USB_CONNECT_TIMEOUT_MS;
    const uint32_t usbStart     = millis();

    while( !tud_mounted() ) {

        blinkActivityLED(BLINK_HEARTBEAT);
        tud_task();

        sleep_ms(10);

        if( millis() - usbStart > usbTimeoutMS ) cleanupAndBlinkErrorLED(ERR_USB_RESTART_FAIL);

    } // while

    usbConnected = true;
}


static void rebootDevice()
{
    // Disconnect USB
    usbDisconnect();

    // Disconnect TCPs
    if(cdc0PCB) tcp_client_disconnect(cdc0PCB);
    if(cdc1PCB) tcp_client_disconnect(cdc1PCB);
    if(monPCB ) tcp_client_disconnect(monPCB );
    sleep_ms(100);

    cyw43_wifi_leave(&cyw43_state, CYW43_ITF_STA);
    sleep_ms(100);

    // Wait ~5 seconds before reboot
    const uint32_t startMillis = millis();

    cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, 0);

    while( millis() - startMillis < 5000 ) {

        for(int i = 0; i < 3; ++i) {
            cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, 1); sleep_ms(100);
            cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, 0); sleep_ms(100);
        }

        sleep_ms(300);

    } // while

    // Reboot
    cyw43_arch_deinit();

    watchdog_reboot(0, 0, 0);
}


////////////////////////////////////////////////////////////////////////////////////////////////////


static __no_return__ void cleanupAndBlinkErrorLED(ErrorBlinkPattern err_led)
{
    if(cdc0PCB) tcp_client_disconnect(cdc0PCB);
    if(cdc1PCB) tcp_client_disconnect(cdc1PCB);
    if(monPCB ) tcp_client_disconnect(monPCB );

    /*
    // NOTE : Cannot deinitialize because the LED will become inaccessible
    cyw43_arch_deinit();
    //*/

    blinkErrorLED(err_led);
}


static struct tcp_pcb* connect_and_setup_pair(
    const char*       ip_str,
    u16_t             port,
    tcp_connected_fn  connected_cb,
    bool*             connected_flag,
    tcp_err_fn        error_cb,
    uint8_t           cdc_itf,
    uint8_t*          tx_storage,
    uint8_t*          rx_storage,
    uint32_t          buf_size,
    uint32_t          tx_lock,
    uint32_t          rx_lock,
    cdc_tcp_pair_t*   pair,
    ErrorBlinkPattern err_led
)
{
    struct tcp_pcb* pcb = tcp_client_connect(ip_str, port, NULL, connected_cb, NULL, NULL, NULL);

    if(!pcb) cleanupAndBlinkErrorLED(err_led);

    setup_tcp_pair(pair, pcb, cdc_itf, tx_storage, rx_storage, buf_size, tx_lock, rx_lock);
    if(error_cb) tcp_err(pcb, error_cb);

    const uint32_t svcTimeoutMS = SERVER_CONNECT_TIMEOUT_MS;
    const uint32_t svcStart     = millis();

    while(!*connected_flag) {

        blinkActivityLED(BLINK_SERVER_WAIT);

        sleep_ms(10);

        if( millis() - svcStart > svcTimeoutMS ) {
            if(cdc_itf == -1) return NULL; // If no CDC-ACM interface is defined, this connection is optional
            cleanupAndBlinkErrorLED(err_led);
        }

    } // while

    return pcb;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


int main(void)
{
    // Ensure clean (re-)start
    if( watchdog_caused_reboot() ) {
        watchdog_hw->scratch[0] = 0;
    }
    else {
        watchdog_hw->scratch[0] = 0xA5A5F00D;
        watchdog_reboot(0, 0, 10);
        while(true) tight_loop_contents();
    }

    // Initialize board
    board_init();

    // Initialize RUN_MODE_GPIO as input with pull-up
    gpio_init   (RUN_MODE_GPIO       );
    gpio_set_dir(RUN_MODE_GPIO, false);
    gpio_pull_up(RUN_MODE_GPIO       );

    sleep_ms(100);

    const uint8_t RUN_MODE_IO0_IO1 = 1;
    const uint8_t RUN_MODE_DBG_CON = 2;
    const uint8_t runMode          = gpio_get(RUN_MODE_GPIO) ? RUN_MODE_IO0_IO1 : RUN_MODE_DBG_CON;

    // Initialized WiFi
    if( cyw43_arch_init_with_country(CYW43_COUNTRY_WORLDWIDE) ) {
        cleanupAndBlinkErrorLED(ERR_WIFI_INIT);
    }

    // Synchronize RUN_MODE_IO0_IO1 and RUN_MODE_DBG_CON
    if(runMode == RUN_MODE_IO0_IO1) {
        // Initialize SYNC_RUN_GPIO as an output with an initial value of high
        gpio_init   (SYNC_RUN_GPIO          );
        gpio_put    (SYNC_RUN_GPIO, true    ); // Set SYNC_RUN_GPIO high to defer initialization of the paired device with RUN_MODE_DBG_CON
        gpio_set_dir(SYNC_RUN_GPIO, GPIO_OUT);
    }
    else { // RUN_MODE_DBG_CON
        // Wait for a while so the RUN_MODE_IO0_IO1 mode can start its initialization first
        sleep_ms(1000);
        // Initialize SYNC_RUN_GPIO as input with pull-up
        gpio_init   (SYNC_RUN_GPIO       );
        gpio_set_dir(SYNC_RUN_GPIO, false);
        gpio_pull_up(SYNC_RUN_GPIO       );
        // Wait for the RUN_MODE_IO0_IO1 mode done its initialization
        int32_t  brightness     = 0;
        int32_t  direction      = 5;
        uint32_t last_step_time = to_ms_since_boot( get_absolute_time() );
        while( gpio_get(SYNC_RUN_GPIO) ) { // Active low
            // Get current time in a small window (0 to 500 microseconds) - this defines our PWM frequency (2kHz)
            uint32_t current_time_us = time_us_32() % 500;
            // Map brightness (0-255) to that 1000us window
            if( current_time_us < (brightness * 500 / 100) ) cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, 1);
            else                                             cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, 0);
            // Update brightness every 5 milliseconds
            const uint32_t now = to_ms_since_boot( get_absolute_time() );
            if( now - last_step_time > 5 ) {
                last_step_time   = now;
                brightness      += direction;
                     if(brightness <=   0) { brightness =   0; direction = -direction; }
                else if(brightness >= 100) { brightness = 100; direction = -direction; }
            }
        } // while
        cyw43_arch_gpio_put(CYW43_WL_GPIO_LED_PIN, 0);
    }

    sleep_ms(100);

    // Enable station mode
    cyw43_arch_enable_sta_mode();

    sleep_ms(100);

    cyw43_wifi_pm(&cyw43_state, CYW43_PERFORMANCE_PM);

    sleep_ms(100);

    // Connect to the AP
    // NOTE : Please refer to '../../RPiOS/InstallOS.txt' for more details
    const char* ssid = "JxMake RPi WiFi Access Point";
    const char* pass = "cR11d8a2w4VDjq3d";

    if( cyw43_arch_wifi_connect_async(ssid, pass, CYW43_AUTH_WPA2_AES_PSK) ) {
        cleanupAndBlinkErrorLED(ERR_WIFI_CONNECT_START);
    }

    const uint32_t wcaTimeoutMS = WIFI_CONNECT_TIMEOUT_MS;
          uint32_t wcaStart     = millis();
          int      wcaLStat     = 0;

    while(true) {

        blinkActivityLED(BLINK_WIFI_WAIT);

        wcaLStat = cyw43_tcpip_link_status(&cyw43_state, CYW43_ITF_STA);
        if(wcaLStat == CYW43_LINK_UP) break;

        sleep_ms(10);

        if( millis() - wcaStart > wcaTimeoutMS ) cleanupAndBlinkErrorLED(ERR_WIFI_CONNECT_FAIL);

    } // while

    sleep_ms(1000);

    // Wait for IP
    const uint32_t ipTimeoutMS = WIFI_GET_IP_TIMEOUT_MS;
    const uint32_t ipStart     = millis();

    while(true) {

        const ip4_addr_t addr = *netif_ip4_addr( &cyw43_state.netif[CYW43_ITF_STA] );

        if( !ip4_addr_isany_val(addr) ) break;

        blinkActivityLED(BLINK_WIFI_WAIT);

        sleep_ms(10);

        if( millis() - ipStart > ipTimeoutMS ) cleanupAndBlinkErrorLED(ERR_WIFI_GET_IP_FAIL);

    } // while

    sleep_ms(100);

    // ===== Connect to servers =====
    // NOTE : Please refer to '../../RPiOS/InstallOS.txt' for more details

    if(runMode == RUN_MODE_IO0_IO1) {
        // Try connecting to Serial over WiFi Bridge Server #0 (io0)
        cdc0PCB = connect_and_setup_pair(
            "172.25.0.1"           ,
            2520                   ,
            cdc0ConnectedCB        ,
            &cdc0Connected         ,
            cdc0ErrorCB            ,
            0                      , // CDC-ACM #0
            cdc0TXStorage          ,
            cdc0RXStorage          ,
            CDC_BUFF_SIZE          ,
            0                      , // Lock #0
            1                      , // Lock #1
            &cdc0TCPPair           ,
            ERR_BRIDGE0_SERVER_FAIL
        );

        // Try connecting to Serial over WiFi Bridge Server #1 (io1)
        cdc1PCB = connect_and_setup_pair(
            "172.25.0.1"           ,
            2521                   ,
            cdc1ConnectedCB        ,
            &cdc1Connected         ,
            cdc1ErrorCB            ,
            1                      , // CDC-ACM #1
            cdc1TXStorage          ,
            cdc1RXStorage          ,
            CDC_BUFF_SIZE          ,
            2                      , // Lock #2
            3                      , // Lock #3
            &cdc1TCPPair           ,
            ERR_BRIDGE1_SERVER_FAIL
        );
    }
    else { // RUN_MODE_DBG_CON
        // Try connecting to Serial over WiFi Bridge Server #2 (dbg)
        cdc0PCB = connect_and_setup_pair(
            "172.25.0.1"           ,
            2522                   ,
            cdc0ConnectedCB        ,
            &cdc0Connected         ,
            cdc0ErrorCB            ,
            0                      , // CDC-ACM #0
            cdc0TXStorage          ,
            cdc0RXStorage          ,
            CDC_BUFF_SIZE          ,
            0                      , // Lock #0
            1                      , // Lock #1
            &cdc0TCPPair           ,
            ERR_BRIDGE2_SERVER_FAIL
        );

        // Try connecting to Console server
        cdc1PCB = connect_and_setup_pair(
            "172.25.0.1"           ,
            2525                   ,
            cdc1ConnectedCB        ,
            &cdc1Connected         ,
            cdc1ErrorCB            ,
            1                      , // CDC-ACM #1
            cdc1TXStorage          ,
            cdc1RXStorage          ,
            CDC_BUFF_SIZE          ,
            2                      , // Lock #2
            3                      , // Lock #3
            &cdc1TCPPair           ,
            ERR_CONSOLE_SERVER_FAIL
        );
        consoleMode = true; // Set flag
    }

    sleep_ms(100);

    // Try connecting to Monitoring server
    monPCB = connect_and_setup_pair(
        "172.25.0.1"           ,
        2500                   ,
        monConnectedCB         ,
        &monConnected          ,
        monErrorCB             ,
        -1                     , // No CDC-ACM
        monTXStorage           ,
        monRXStorage           ,
        MON_BUFF_SIZE          ,
        4                      , // Lock #4
        5                      , // Lock #5
        &monTCPPair            ,
        ERR_MONITOR_SERVER_FAIL
    );

    monAvailable = (monPCB != NULL); // Set flag

/*
ss -tuln
netstat -tnp

lsusb -v -d cafe:


screen /dev/ttyACM0 115200
^A K y


stty -F /dev/ttyACM1 -hupcl
screen /dev/ttyACM1 115200
^A K y
AVOID execute commands with long output because ^C will only takes effect after the long output are completely streamed.
DO NOT USE THE MOUSE SCROLL WHEEL, as it may cause the console to hang.

stty -F /dev/ttyACM1 -hupcl
minicom -D /dev/ttyACM1 -b 115200 -R utf-8
^A X Yes
AVOID execute commands with long output because ^C will only takes effect after the long output are completely streamed.
DO NOT USE THE MOUSE SCROLL WHEEL, as it may cause the console to hang.
*/

    // Init device stack on configured roothub port
    tud_init(BOARD_TUD_RHPORT);

    const uint32_t usbTimeoutMS = USB_CONNECT_TIMEOUT_MS;
    const uint32_t usbStart     = millis();

    while( !tud_mounted() ) {

        blinkActivityLED(BLINK_HEARTBEAT);
        tud_task();

        sleep_ms(10);

        if( millis() - usbStart > usbTimeoutMS ) cleanupAndBlinkErrorLED(ERR_USB_ENUM_FAIL);

    } // while

    usbConnected = true;

    // Synchronize RUN_MODE_IO0_IO1 and RUN_MODE_DBG_CON
    if(runMode == RUN_MODE_IO0_IO1) {
        // Set SYNC_RUN_GPIO low to allow the paired device with RUN_MODE_DBG_CON to begin initialization
        gpio_put(SYNC_RUN_GPIO, false);
        sleep_ms(10);
    }

    // The main loop
    while(true) {

        blinkActivityLED(BLINK_HEARTBEAT);

        tud_task();
        usbCDCACMTask();

        sleep_ms(1);

        if(runMode == RUN_MODE_DBG_CON) {
            // Restart this device if the paired device restarts
            if( gpio_get(SYNC_RUN_GPIO) ) rebootDevice();
        }

    } // while

    return 0;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

static bool     cdc1LineOpen   = false;
static bool     cdc1NeedKick   = false;
static uint32_t cdc1LastKickMS = 0;

void tud_cdc_line_state_cb(uint8_t itf, bool dtr, bool rts)
{
    (void)rts;

    if(itf == 1) {
        cdc1LineOpen = dtr;
        if(dtr) cdc1NeedKick = true;
    }
}


static void usbCDCACMTask(void)
{
    // If any of the connected servers go down, restart the dongle
    if( !cdc0Connected || !cdc1Connected || (monAvailable && !monConnected) ) {
        rebootDevice();
    }

    // CDC #0 - Bridge #0 or Bridge #2
    cdc_tcp_service(&cdc0TCPPair, false);

    // CDC #1 - Bridge #1 or Console
    if(consoleMode && cdc1LineOpen && cdc1TCPPair.tcp_pcb) {
        const uint32_t now = millis();
        if( cdc1NeedKick && (now - cdc1LastKickMS >= 500) ) {
            cdc1NeedKick   = false;
            cdc1LastKickMS = now;
            tcp_client_send(cdc1TCPPair.tcp_pcb, "\n", 1);
        }
    }
    cdc_tcp_service(&cdc1TCPPair, consoleMode);

    // Monitoring
    if(monPCB) {
        #define MSG_BUFF_SIZE 8
        static uint8_t  msg[MSG_BUFF_SIZE];
        static uint32_t msgIdx     = 0;
        static bool     collecting = false;
        static bool     overflow   = false;
        ring_buffer_t*  rb         = msg_tcp_service(&monTCPPair, 0, 0); // ##### ??? TODO : Send any character every once a while so the server is forced to resend the state ??? #####
        // Read the frame
        while( !ring_buffer_empty(rb) ) {

            uint8_t byte;
            ring_buffer_pop(rb, &byte);

            if(byte == 0x02) { // STX
                msgIdx     = 0;
                collecting = true;
                overflow   = false;
                continue;
            }

            if(collecting) {
                if(byte == 0x03) { // ETX
                    if(!overflow && msgIdx > 0) {
                        // Success - msg now contains data between STX and ETX
                        //    Index                   0                    1                    2
                        //    Device                  /dev/ttySerial_io0   /dev/ttySerial_io1   /dev/ttySerial_dbg
                        //    State (Not Connected)   0x30 ('0')           0x30 ('0')           0x30 ('0')
                        //    State (    Connected)   0x31 ('1')           0x31 ('1')           0x31 ('1')
                        uint8_t* begPtr = &msg[0         ];
                        uint8_t* endPtr = &msg[msgIdx - 1];
                        // Process the state
                        if(!consoleMode) { // RUN_MODE_IO0_IO1
                            // Check io0 and io1
                            if( *(begPtr + 0) != '1' || *(begPtr + 1) != '1' ) {
                                usbDisconnect();
                            }
                            else {
                                usbConnect();
                            }
                        }
                        else { // RUN_MODE_DBG_CON
                            // Check dbg
                            if(*endPtr != '1') {
                                usbDisconnect();
                            }
                            else {
                                usbConnect();
                            }
                        }
                    }
                    // Reset state for next potential frame
                    collecting = false;
                    msgIdx     = 0;
                }
                else {
                    if(msgIdx < MSG_BUFF_SIZE) msg[msgIdx++] = byte;
                    else                       overflow      = true; // Too long - enter overflow mode to discard until next STX
                }
            }

        } // while
    }
}
