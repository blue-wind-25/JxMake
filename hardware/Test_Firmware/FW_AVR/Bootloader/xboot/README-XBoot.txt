====================================================================================================
XBoot Bootloader
====================================================================================================

The complete source code for XBoot bootloader can be downloaded from the GitHub repository:

    https://github.com/alexforencich/xboot
    Copyright (C) 2010 Alex Forencich

~~~ Last accessed & checked on 2025-10-17 ~~~

----------------------------------------------------------------------------------------------------

The XBoot bootloader is licensed under the terms of the MIT License.

JxMake is not linked with any XBoot binary and does not copy or use code from the XBoot source code.

----------------------------------------------------------------------------------------------------

You can program the HEX file using the following command:

    avrdude -p atmega328p -P usb -c usbasp -B 5 -e -U flash:w:xboot_atmega328p_8mhz_57600_led+b5.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xD8:m -U efuse:w:0x06:m

====================================================================================================
