====================================================================================================
Arduino Bootloaders
====================================================================================================

The complete source code and documentation for the Arduino Bootloaders can be downloaded from:
    https://github.com/arduino/ArduinoCore-avr/tree/master/bootloaders

----------------------------------------------------------------------------------------------------

These bootloaders are not part of JxMake and are not covered by the JxMake license.

The bootloader in the 'atmega' directory is licensed under the terms of the GNU General Public
License version 2 or later.

The bootloader in the 'caterina' directory is presumed to be licensed under the same terms as LUFA.

----------------------------------------------------------------------------------------------------

The 'ATmegaBOOT_168_atmega328_pro_8MHz.hex' was built using the command:
    cd atmega
    make atmega328_pro8
    mv ATmegaBOOT_168_atmega328_pro_8MHz.hex ..
    cd ..

The 'ATmegaBOOT_168_atmega328_pro_8MHz.hex' can then be uploaded using the commands:
    avrdude -c usbasp -B 1.3 -p m328p -e -D -V -U flash:w:ATmegaBOOT_168_atmega328_pro_8MHz.hex:i
    avrdude -c usbasp -B 1.3 -p m328p -e -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0x05:m -U lock:w:0x3F:m

----------------------------------------------------------------------------------------------------

The '../LUFA/LUFA-111009_BootloaderCaterina_AT90USB1286_16MHz.hex' was built using the command:
    cp -Rv caterina caterina_at90usb1286
    cd caterina_at90usb1286
    patch -p1 < ../../LUFA/LUFA-111009_BootloaderCaterina_AT90USB1286_16MHz.diff
    make
    mv Caterina.hex ../../LUFA/LUFA-111009_BootloaderCaterina_AT90USB1286_16MHz.hex
    cd ..
    rm -rvf caterina_at90usb1286

The 'LUFA-111009_BootloaderCaterina_AT90USB1286_16MHz.hex' can then be uploaded using the commands:
    avrdude -c usbasp -B 1.3 -p at90usb1286 -e -D -V -U flash:w:LUFA-111009_BootloaderCaterina_AT90USB1286_16MHz.hex:i
    avrdude -c usbasp -B 1.3 -p at90usb1286 -e -U lfuse:w:0xFF:m -U hfuse:w:0xDA:m -U efuse:w:0xF3:m -U lock:w:0x3F:m

====================================================================================================
