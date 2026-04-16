Arduino Core for SAMD21 and SAMD51 CPU

https://github.com/Seeed-Studio/ArduinoCore-samd/tree/master
https://github.com/Seeed-Studio/ArduinoCore-samd/tree/master/bootloaders/XIAOM0
https://raw.githubusercontent.com/Seeed-Studio/ArduinoCore-samd/master/bootloaders/XIAOM0/bootloader-XIAO_m0-v3.7.0-33-g90ff611-dirty.bin

Copyright (C) 2015 Arduino LLC - All right reserved
GNU LGPL version 2 or later

----------------------------------------------------------------------------------------------------

How to Unbrick a Dead XIAO Using ST-LINK and OpenOCD
    https://forum.seeedstudio.com/t/how-to-unbrick-a-dead-xiao-using-st-link-and-openocd/255562/4

Unbricking a Seeeduino Xiao SAMD21
    https://emalliab.wordpress.com/2023/03/12/unbricking-a-seeed-xiao-samd21

----------------------------------------------------------------------------------------------------

To restore SAM-BA bootloader, please execute this command (on Linux):
    ~/0-JxMake/data/xpackopenocd/bin/openocd -f XIAO_openocd.cfg
