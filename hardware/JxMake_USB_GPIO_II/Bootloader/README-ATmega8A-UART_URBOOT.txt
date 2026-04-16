====================================================================================================
ATmega8A UART Bootloader (URBOOT)
====================================================================================================

The complete source code for this bootloader can be downloaded from the GitHub repository:

    https://github.com/stefanrueger/urboot

The precompiled binary of the bootloader variant used by JxMake can be directly downloaded from:

    https://github.com/stefanrueger/urboot.hex/blob/main/mcus/atmega8a/watchdog_2_s/autobaud/uart0_rxd0_txd1/no-led/urboot_m8a_2s_autobaud_uart0_rxd0_txd1_no-led.hex

~~~ Last accessed & checked on 2025-03-16 ~~~

----------------------------------------------------------------------------------------------------

The URBOOT bootloader is licensed under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or any later version:

    https://github.com/stefanrueger/urboot/blob/main/LICENSE

JxMake is not linked with any URBOOT binary and does not copy or use code from the URBOOT source
code.

----------------------------------------------------------------------------------------------------

You can program the HEX file with the command:

    avrdude -p atmega8a -P usb -c usbasp -B 5 -e -U flash:w:ATmega8A-UART_URBOOT/urboot_m8a_2s_autobaud_uart0_rxd0_txd1_no-led.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDF:m

====================================================================================================
