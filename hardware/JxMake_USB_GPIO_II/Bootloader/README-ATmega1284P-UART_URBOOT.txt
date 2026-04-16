====================================================================================================
ATmega1284P UART Bootloader (URBOOT)
====================================================================================================

The complete source code for this bootloader can be downloaded from the GitHub repository:

    https://github.com/stefanrueger/urboot

The precompiled binary of the bootloader variant used by JxMake can be directly downloaded from:

    https://raw.githubusercontent.com/stefanrueger/urboot.hex/main/mcus/atmega1284p/watchdog_1_s/autobaud/uart0_rxd0_txd1/no-led/urboot_m1284p_1s_autobaud_uart0_rxd0_txd1_no-led_pr_ee_ce.hex

or:

    https://raw.githubusercontent.com/stefanrueger/urboot.hex/main/mcus/atmega1284p/watchdog_2_s/autobaud/uart0_rxd0_txd1/no-led/urboot_m1284p_2s_autobaud_uart0_rxd0_txd1_no-led_pr_ee_ce.hex

~~~ Last accessed & checked on 2025-07-22 ~~~

----------------------------------------------------------------------------------------------------

The URBOOT bootloader is licensed under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or any later version:

    https://github.com/stefanrueger/urboot/blob/main/LICENSE

JxMake is not linked with any URBOOT binary and does not copy or use code from the URBOOT source
code.

----------------------------------------------------------------------------------------------------

You can program the HEX file with the command:

BOD 2.7V (may cause false detection when a higher VBoost is selected):
    avrdude -p atmega1284p -P usb -c usbasp -B 5 -e -U flash:w:ATmega1284P-UART_URBOOT/urboot_m1284p_1s_autobaud_uart0_rxd0_txd1_no-led_pr_ee_ce.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDF:m -U efuse:w:0xFD:m

BOD 1.8V (recommended):
    avrdude -p atmega1284p -P usb -c usbasp -B 5 -e -U flash:w:ATmega1284P-UART_URBOOT/urboot_m1284p_1s_autobaud_uart0_rxd0_txd1_no-led_pr_ee_ce.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDF:m -U efuse:w:0xFE:m

BOD disabled (not recommended):
    avrdude -p atmega1284p -P usb -c usbasp -B 5 -e -U flash:w:ATmega1284P-UART_URBOOT/urboot_m1284p_1s_autobaud_uart0_rxd0_txd1_no-led_pr_ee_ce.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDF:m -U efuse:w:0xFF:m

Replace the file with 'urboot_m1284p_2s_autobaud_uart0_rxd0_txd1_no-led_pr_ee_ce.hex' if you prefer
to use the variant with a 2-second timeout.

====================================================================================================
