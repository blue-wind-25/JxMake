====================================================================================================
USBasp
====================================================================================================

----------------------------------------------------------------------------------------------------
USBasp is not part of the JxMake program and is not covered by the JxMake licenses.
----------------------------------------------------------------------------------------------------

USBasp - USB Programmer for Atmel AVR Controllers
https://www.fischl.de/usbasp

USBasp Mods for PDI programming
https://skeatz.github.io/FabPDI/usbasp-mods.html

ATxmega Programmer for $0.50
http://szulat.blogspot.com/2012/08/atxmega-programmer-for-050.html
    http://www.fischl.de/usbasp/usbasp.2011-05-28.tar.gz
    http://sz.toyspring.com/usbasp-pdi-usbaspfirmware-20120816.diff

Programming Xmega with USBasp & AVRDUDE
https://ketturi.kapsi.fi/2013/05/programming-xmega-with-usbasp-avrdude

USBasp SPI Fix
    https://openrcforums.com/forum/viewtopic.php?t=1363
    https://openrcforums.com/forum/viewtopic.php?p=19315#p19315
    https://openrcforums.com/forum/download/file.php?id=1726

----------------------------------------------------------------------------------------------------

How to apply patch:

    wget http://www.fischl.de/usbasp/usbasp.2011-05-28.tar.gz
    tar xvf usbasp.2011-05-28.tar.gz
    cd usbasp.2011-05-28
    patch -p1 < ../usbasp-pdi-usbaspfirmware-20120816-FIXED.diff

How to build and flash:

    cd firmware
    make TARGET=atmega8 main.hex
    avrdude -c usbasp -B 1 -p m8 -e -D -U flash:w:main.hex:i -U lfuse:w:0xBF:m -U hfuse:w:0xD9:m

or use the precompiled firmware:

    avrdude -c usbasp -B 1 -p m8 -e -D -U flash:w:usbasp-pdi-usbaspfirmware-20110528-atmega8-12mhz.hex:i -U lfuse:w:0xBF:m -U hfuse:w:0xD9:m

====================================================================================================
