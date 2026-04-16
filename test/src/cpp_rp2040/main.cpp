extern "C" {

    #include <btstack.h>

    #include <pico/bootrom.h>
    #include <pico/cyw43_arch.h>
    #include <pico/multicore.h>
    #include <pico/stdlib.h>
    #include <pico/time.h>

    #include <lwip/pbuf.h>
    #include <lwip/tcp.h>

    #include "bt.h"

    #include "dhcpserver.h"
    #include "dnsserver.h"

    #include "gatt/dummy_temp.gatt.h"

}


// Define the pins
#define LED_1_GPIO  25 // Built-in
#define LED_1_CYW43  0 // Built-in

#define LED_2_GPIO  14 // External

#define SW_GPIO     15 // External


extern bool checkPicoW();
extern void initPIOProgram();


static bool                                   isPicoW = false;

static btstack_packet_callback_registration_t hci_event_callback_registration;

static dhcp_server_t                          dhcpServer;
static dns_server_t                           dnsServer;


static bool fast = true;

static void delayAndCheckSwitch(char cnt)
{
    // Delay and check the switch
    for(char i = 0; i < cnt; ++i) {
        // Delay for a while
        sleep_ms(10);
        // Check if the switch is pressed
        if( !gpio_get(SW_GPIO) ) {
            // Debouncing
            sleep_ms(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !gpio_get(SW_GPIO) );
}

static void core1_program()
{
    static constexpr char delay_s_s =  5;
    static constexpr char delay_s_l = 10;

    static constexpr char delay_l_s = 15;
    static constexpr char delay_l_l = 30;

    // Loop to blink the built-in LED
    fast = !fast;

    for(;;) {

        // Blink the 1st LED
        for(char i = 0; i < 3; ++i) {

            if(isPicoW) cyw43_arch_gpio_put(LED_1_CYW43, 1);
            else        gpio_put           (LED_1_GPIO , 1);

            delayAndCheckSwitch(fast ? delay_s_s : delay_s_l); // Short delay

            if(isPicoW) cyw43_arch_gpio_put(LED_1_CYW43, 0);
            else        gpio_put           (LED_1_GPIO , 0);

            delayAndCheckSwitch(fast ? delay_s_s : delay_s_l); // Short delay

        } // for

        // Blink the 2nd LED
        for(char i = 0; i < 3; ++i) {

            gpio_put(LED_2_GPIO , 1);

            delayAndCheckSwitch(fast ? delay_s_s : delay_s_l); // Short delay

            gpio_put(LED_2_GPIO , 0);

            delayAndCheckSwitch(fast ? delay_s_s : delay_s_l); // Short delay

        } // for

        // Long delay
        delayAndCheckSwitch(fast ? delay_l_s : delay_l_l);

    } // for
}


int main()
{
    // Detect R-Pi Pico W
    isPicoW = checkPicoW();

    // Initialize system
    stdio_init_all();

    if(isPicoW) {
        // Initialize WiFi and enable AP
        if( cyw43_arch_init_with_country(CYW43_COUNTRY_WORLDWIDE) ) {
            printf("failed to WiFi\n");
            return -1;
        }
        cyw43_arch_enable_ap_mode("Pico-W Test AP", "password", CYW43_AUTH_WPA2_AES_PSK);
        // Start the DHCP server
        ip_addr_t  gw;
        ip4_addr_t mask;
        IP4_ADDR( ip_2_ip4(&gw  ), 192, 168,   2, 1 );
        IP4_ADDR( ip_2_ip4(&mask), 255, 255, 255, 0 );
        dhcp_server_init(&dhcpServer, &gw, &mask);
        // Start the DNS server
        dns_server_init(&dnsServer, &gw);
        /*
         * Initialize Bluetooth
         * ----------------------------------------------------------------------------------------------------
         * This Pico device will be visible as something like "Pico 43:43:A2:12:1F:AC" in your mobile phone.
         *
         * You can explore the device and read its status/values by installing an app such as 'GATTBrowser'
         * by 'Renesas' from the 'Google Play Store'.
         * ----------------------------------------------------------------------------------------------------
         * There are very few Bluetooth USB dongles that can operate in peripheral mode. If you happen to
         * have one, please refer to:
         *     https://ubuntu.com/core/docs/bluez/reference/accessing-gatt-services
         * for more details on how to access this Pico device from Linux.
         */
        l2cap_init();
        sm_init();
        att_server_init(profile_data, att_read_callback, att_write_callback);
        hci_event_callback_registration.callback = &packet_handler;
        hci_add_event_handler(&hci_event_callback_registration);
        att_server_register_packet_handler(packet_handler);
        async_context_add_at_time_worker_in_ms(cyw43_arch_async_context(), &heartbeat_worker, HEARTBEAT_PERIOD_MS);
        hci_power_control(HCI_POWER_ON);
    }
    else {
        gpio_init   (LED_1_GPIO          );
        gpio_set_dir(LED_1_GPIO, GPIO_OUT);
    }

    gpio_init     (LED_2_GPIO             );
    gpio_set_dir  (LED_2_GPIO, GPIO_OUT   );

    gpio_init     (SW_GPIO                );
    gpio_set_dir  (SW_GPIO   , GPIO_IN    );
    gpio_set_pulls(SW_GPIO   , true, false);

    initPIOProgram();

    // Run program to blink the built-in LED in CORE1
    multicore_launch_core1(core1_program);

    // Loop to check input from UART
    for(;;) {

        const int ch = getchar_timeout_us(1000);

             if(ch == 'p') printf( "\nThe board is 'Raspberry Pi Pico%s'\n\n", isPicoW ? " W" : "" );
        else if(ch == 'b') reset_usb_boot(0, 0);

    } // for

    // The program should never get here
    if(isPicoW) {
        dns_server_deinit (&dnsServer );
        dhcp_server_deinit(&dhcpServer);
        cyw43_arch_deinit (           );
    }

    return 0;
}
