====================================================================================================
Chip45Boot2 and Chip45Boot3 Bootloaders
====================================================================================================

The complete source code for Chip45Boot2 bootloader can be downloaded from the GitHub repository:

    https://github.com/eriklins/chip45boot2
    Copyright (C) 2023 Erik Lins

The complete source code for Chip45Boot3 bootloader can be downloaded from the GitHub repository:

    https://github.com/eriklins/chip45boot3
    Copyright (C) 2023 ER!K

~~~ Last accessed & checked on 2025-09-16 ~~~

----------------------------------------------------------------------------------------------------

The Chip45Boot2 bootloader is licensed under the terms of the MIT License:

    https://github.com/eriklins/chip45boot2/blob/main/LICENSE

The Chip45Boot3 bootloader is licensed under the terms of the MIT License:

    https://github.com/eriklins/chip45boot3/blob/main/LICENSE

----------------------------------------------------------------------------------------------------

The precompiled Chip45Boot2 bootloader binaries used here can be downloaded directly from:

    https://raw.githubusercontent.com/eriklins/chip45boot2/refs/heads/main/bootloader/build/chip45boot2_atmega328p_uart0_v2.9Q.hex
    https://raw.githubusercontent.com/eriklins/chip45boot2/refs/heads/main/bootloader/build/chip45boot2_atmega328p_uart0_rs485_v2.9Q.hex

The RS485 direction pin should be PD.4 (T0).

----------------------------------------------------------------------------------------------------

The precompiled binaries of the Chip45Boot3 bootloader used here were built using Atmel Studio 6.1,
which can be downloaded from:

    https://ww1.microchip.com/downloads/archive/AStudio61sp1_1net.exe

The firmware file 'chip45boot3_atmega2560_uart0.hex' was built using the following
options:
    BOOT_SECTION_START=0x3F000
    HOST_USART=0
    USE_AUTOBAUD
    PROVIDE_FIRMWARE_VERSION
    PROVIDE_FLASH_WRITE
    PROVIDE_FLASH_READ
    PROVIDE_EEPROM_WRITE
    PROVIDE_EEPROM_READ

The firmware file 'chip45boot3_atmega2560_uart0_rs485_pb7_0x25.hex' was built using the
following options:
    BOOT_SECTION_START=0x3F000
    HOST_USART=0
    USE_AUTOBAUD
    USE_RS485
    RS485_ADDR=0x25
    PROVIDE_FIRMWARE_VERSION
    PROVIDE_FLASH_WRITE
    PROVIDE_FLASH_READ
    PROVIDE_EEPROM_WRITE
    PROVIDE_EEPROM_READ

    #define myRS485_DDR    DDRB
    #define myRS485_PORT   PORTB
    #define myRS485_DIRPIN PB7

The firmware file 'chip45boot3_atmega2560_uart0_xtae.hex' was built using
the following options:
    BOOT_SECTION_START=0x3F000
    HOST_USART=0
    USE_AUTOBAUD
    PROVIDE_FIRMWARE_VERSION
    PROVIDE_FLASH_WRITE
    PROVIDE_FLASH_READ
    PROVIDE_EEPROM_WRITE
    PROVIDE_EEPROM_READ
    USE_ENCRYPTION
    ENCRYPTION_KEY={0xFEDCBA98UL,0x76543210UL,0x01234567UL,0x89ABCDEFUL}

The firmware file 'chip45boot3_atmega2560_uart0_xtae_noread.hex' was built using
the following options:
    BOOT_SECTION_START=0x3F000
    HOST_USART=0
    USE_AUTOBAUD
    PROVIDE_FIRMWARE_VERSION
    PROVIDE_FLASH_WRITE
    PROVIDE_EEPROM_WRITE
    USE_ENCRYPTION
    ENCRYPTION_KEY={0xFEDCBA98UL,0x76543210UL,0x01234567UL,0x89ABCDEFUL}

----------------------------------------------------------------------------------------------------

You can program the HEX files for ATmega328P using the following commands:

    avrdude -p atmega328p -P usb -c usbasp -B 5 -e -U flash:w:chip45boot2_atmega328p_uart0_v2.9Q.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0x06:m

    avrdude -p atmega328p -P usb -c usbasp -B 5 -e -U flash:w:chip45boot2_atmega328p_uart0_rs485_v2.9Q.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0x06:m

You can program the HEX files for ATmega2560 using the following commands:

    avrdude -p atmega2560 -P usb -c usbasp -B 5 -e -U flash:w:chip45boot3_atmega2560_uart0.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0xFE:m

    avrdude -p atmega2560 -P usb -c usbasp -B 5 -e -U flash:w:chip45boot3_atmega2560_uart0_rs485_pb7_0x25.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0xFE:m

    avrdude -p atmega2560 -P usb -c usbasp -B 5 -e -U flash:w:chip45boot3_atmega2560_uart0_xtae.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0xFE:m

    avrdude -p atmega2560 -P usb -c usbasp -B 5 -e -U flash:w:chip45boot3_atmega2560_uart0_xtae_noread.hex:i -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0xFE:m

====================================================================================================
