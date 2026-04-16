====================================================================================================
Pre-Compiled u8.0/u8.1 URBOOT Bootloaders
====================================================================================================

The complete source code for this bootloader can be downloaded from the GitHub repository:

    https://github.com/stefanrueger/urboot

The precompiled binaries for the bootloader variants used here are available for direct download
from:

    https://raw.githubusercontent.com/stefanrueger/urboot.hex/main/mcus/atmega8/watchdog_1_s/autobaud/uart0_rxd0_txd1/led%2Bb5/urboot_m8_1s_autobaud_uart0_rxd0_txd1_led%2Bb5_pr_ee_ce.hex

    https://raw.githubusercontent.com/stefanrueger/urboot.hex/main/mcus/atmega328p/watchdog_1_s/external_oscillator_x/%2B8m000000_hz/%2B%2B57k6_baud/uart0_rxd0_txd1/led%2Bb5/urboot_m328p_1s_x8m0_57k6_uart0_rxd0_txd1_led%2Bb5_pr_ee_ce.hex
    https://raw.githubusercontent.com/stefanrueger/urboot.hex/main/mcus/atmega328p/watchdog_1_s/external_oscillator_x/16m000000_hz/%2B115k2_baud/uart0_rxd0_txd1/led%2Bb5/urboot_m328p_1s_x16m0_115k2_uart0_rxd0_txd1_led%2Bb5_pr_ee_ce.hex

~~~ Last accessed & checked on 2025-03-16 ~~~

----------------------------------------------------------------------------------------------------

The URBOOT bootloader is licensed under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or any later version:

    https://github.com/stefanrueger/urboot/blob/main/LICENSE

JxMake is not linked with any URBOOT binary and does not copy or use code from the URBOOT source
code.

----------------------------------------------------------------------------------------------------

You can program the HEX files using the following commands:

    avrdude -p atmega8    -P usb -c usbasp -B 5 -e -U flash:w:<hex_file_name>:i -U lfuse:w:0xFF:m -U hfuse:w:0xDF:m

    avrdude -p atmega328p -P usb -c usbasp -B 5 -e -U flash:w:<hex_file_name>:i -U lfuse:w:0xFF:m -U hfuse:w:0xDF:m -U efuse:w:0x06:m

====================================================================================================
