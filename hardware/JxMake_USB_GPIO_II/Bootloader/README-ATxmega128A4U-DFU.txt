====================================================================================================
ATxmega128A4U DFU Bootloader (AVR1916)
====================================================================================================

The documentation of the DFU bootloader 'Atmel AVR1916: USB DFU Boot Loader for XMEGA' can be
downloaded from:

    https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ApplicationNotes/ApplicationNotes/doc8429.pdf

The documentation of the FLIP protocol 'Atmel AVR4023: FLIP USB DFU' can be downloaded from:

    https://ww1.microchip.com/downloads/aemDocuments/documents/OTH/ApplicationNotes/ApplicationNotes/doc8457.pdf

The full precompiled binary and source code files of the DFU bootloader can be downloaded from:

    https://ww1.microchip.com/downloads/en/DeviceDoc/AVR1916.zip

You can obtain the URLs mentioned above by using the search feature:

    https://www.microchip.com/en-us/search?searchQuery=AVR1916
    https://www.microchip.com/en-us/search?searchQuery=AVR4023

----------------------------------------------------------------------------------------------------

Extract the ZIP file and burn this HEX file using a PDI programmer:

    XMEGA_bootloaders_v104/binaries/atxmega128a4u_104.hex

The 'FUSEBYTE2.BOOTRST' fuse bit must be programmed to 0 to allow the device to reset from the
bootloader section.

You can program the HEX file with the command:

    avrdude -p atxmega128a4u -P usb -c atmelice_pdi -e -U flash:w:ATxmega128A4U-DFU/atxmega128a4u_104.hex:i -U fuse2:w:0xBF:m

Some ATxmega128A4U MCUs come with the DFU bootloader pre-programmed from the factory. You may want
to verify this first before actually programming the bootloader and fuse.

Once an application is loaded by DFU, the bootloader execution can be forced at power-on/reset by
connecting 'PC.3' (the 'PA3_ADC3' pin of the 'JxMake USB-GPIO II Module' board) to ground through
a 1kΩ resistor.

----------------------------------------------------------------------------------------------------

The source code is contained within a ZIP file inside the ZIP file mentioned above:

    XMEGA_bootloaders_v104/source_code/common.services.usb.class.dfu_atmel.device.bootloader.atxmega128a4u.zip

Atmel Software Framework (ASF) License
Copyright (C) 2009-2012 Atmel Corporation. All rights reserved.

====================================================================================================

The cross-platform DFU programmer can be downloaded from:
    https://github.com/dfu-programmer/dfu-programmer
    https://github.com/dfu-programmer/dfu-programmer/releases

You may want to try v1.1.0/v1.0.0/v.0.9.0 first.

====================================================================================================
