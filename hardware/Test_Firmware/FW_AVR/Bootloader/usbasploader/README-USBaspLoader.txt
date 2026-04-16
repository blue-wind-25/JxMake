====================================================================================================
USBaspLoader Bootloader
====================================================================================================

The complete source code for USBaspLoader bootloader can be downloaded from the GitHub repository:

    USBaspLoader: USBasp-Compatible Bootloader for AVR
    https://github.com/gblargg/usbasploader

which was based on:

    USBaspLoader
    https://obdev.at/products/vusb/usbasploader.html

~~~ Last accessed & checked on 2025-10-16 ~~~

----------------------------------------------------------------------------------------------------

The USBaspLoader bootloader is licensed under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 2 of the License, or any later version:

    https://github.com/gblargg/usbasploader/blob/master/License.txt
    https://obdev.at/products/vusb/license.html

JxMake is not linked with any USBaspLoader binary and does not copy or use code from the
USBaspLoader source code.

----------------------------------------------------------------------------------------------------

The precompiled binaries of the USBaspLoader bootloader used here were built for ATmega8 at 12MHz
using AVR GCC 4.9.2.

The firmware file 'usbasploader_atmega8_12mhz_boot+b4_led+b3.hex' was built using the following
defines:
    #define USB_CFG_IOPORTNAME         B
    #define USB_CFG_DMINUS_BIT         0
    #define USB_CFG_DPLUS_BIT          1

    #define BOOTLOADER_ON_JUMPER       1
    #define BOOTLOADER_JUMPER_PORT     B
    #define BOOTLOADER_JUMPER_BIT      4

    #define LED_PRESENT                1
    #define LED_PORT                   B
    #define LED_BIT                    3

    #define HAVE_CHIP_ERASE            1
    #define HAVE_FLASH_BYTE_READACCESS 0
    #define HAVE_FLASH_PAGED_READ      1
    #define HAVE_EEPROM_BYTE_ACCESS    0
    #define HAVE_EEPROM_PAGED_ACCESS   1
    #define HAVE_READ_LOCK_FUSE        0

----------------------------------------------------------------------------------------------------

You can program the HEX files using the following commands:

    avrdude -p m8 -P usb -c usbasp -B 5 -e -U flash:w:usbasploader_atmega8_12mhz_boot+b4_led+b3.hex:i -U lfuse:w:0xBF:m -U hfuse:w:0xC8:m

====================================================================================================
