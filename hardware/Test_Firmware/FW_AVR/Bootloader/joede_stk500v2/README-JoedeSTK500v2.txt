====================================================================================================
Joede STK500v2 Bootloader
====================================================================================================

The complete source code for Joede bootloader can be downloaded from the GitHub repository:

    https://github.com/joede/stk500v2-bootloader
    Copyright (C) 2006 Peter Fleury
    Copyright (C) 2016 Joerg Desch

~~~ Last accessed & checked on 2025-10-17 ~~~

----------------------------------------------------------------------------------------------------

The Joede STK500v2 bootloader is licensed under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the License, or any later version:

    https://github.com/joede/stk500v2-bootloader/blob/master/License.txt

JxMake is not linked with any Joede STK500v2 bootloader binary and does not copy or use code from
the Joede STK500v2 bootloader source code.

----------------------------------------------------------------------------------------------------

The firmware file 'joede_stk500v2_atmega328p_8mhz_57600_led+b5.hex' was built using the following
defines:
    #define REMOVE_PROG_PIN_ENTER
    #define REMOVE_FORCED_MODE_ENTER

    #define ENABLE_LEAVE_BOOTLADER

    #define PROGLED_PORT               PORTB
    #define PROGLED_DDR                DDRB
    #define PROGLED_PIN                PB5

    #define BAUDRATE                   57600
    #define UART_BAUDRATE_DOUBLE_SPEED 1

NOTE : # This bootloader may not be compatible with AVRDUDE if any additional REMOVE_* macros are
         defined beyond those explicitly listed above.
       # This bootloader does not appear to support a compatible EEPROM writing protocol.

----------------------------------------------------------------------------------------------------

You can program the HEX file using the following command:

    avrdude -p atmega328p -P usb -c usbasp -B 5 -e -U flash:w:joede_stk500v2_atmega328p_8mhz_57600_led+b5.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xD8:m -U efuse:w:0x06:m

====================================================================================================
