====================================================================================================
TinySafeBoot Bootloader
====================================================================================================

The complete source code for TinySafeBoot bootloader can be downloaded from the GitHub repository:

    https://github.com/seedrobotics/tinysafeboot/tree/master
    Written in 2011-2015 by Julien Thomas
    Extended by Seed Robotics from 2017

~~~ Last accessed & checked on 2025-09-16 ~~~

----------------------------------------------------------------------------------------------------

The TinySafeBoot bootloader is licensed under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the License, or any later version:

    https://github.com/seedrobotics/tinysafeboot/blob/master/LICENSE

JxMake is not linked with any TinySafeBoot binary and does not copy or use code from the
TinySafeBoot source code.

----------------------------------------------------------------------------------------------------

The precompiled binaries of the TinySafeBoot bootloader used here were built using Atmel Studio 6.1,
which can be downloaded from:

    https://ww1.microchip.com/downloads/archive/AStudio61sp1_1net.exe

The firmware file 'tsb20170626_attiny13_bitbang_pb3_pb4_autobaud.hex' was built using the following
options:
    .set TSBINSTALLER = 0
    .equ RXPORT       = PORTB
    .equ RXPIN        = PINB
    .equ RXDDR        = DDRB
    .equ RXBIT        = 3
    .equ TXPORT       = PORTB
    .equ TXDDR        = DDRB
    .equ TXBIT        = 4

The firmware file 'tsb20200727_atmega328p_uart0_8mhz_19200.hex' was built using the
following options:
    .set TSBINSTALLER = 0
    .equ F_CPU        = 8000000
    .equ BAUD         = 19200

    .equ BOOTSTART    = (FLASHEND+1)-256     ; = 512 Bytes
    .equ LASTPAGE     = BOOTSTART - PAGESIZE ; = 1 page below TSB

----------------------------------------------------------------------------------------------------

You can program the HEX files using the following commands:

    avrdude -p t13 -P usb -c usbasp -B 5 -e -U flash:w:tsb20170626_attiny13_bitbang_pb3_pb4_autobaud.hex:i -U lfuse:w:0x7A:m -U hfuse:w:0xED:m

    avrdude -p atmega328p -P usb -c usbasp -B 5 -e -U flash:w:tsb20200727_atmega328p_uart0_8mhz_19200.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDE:m -U efuse:w:0x06:m

====================================================================================================
