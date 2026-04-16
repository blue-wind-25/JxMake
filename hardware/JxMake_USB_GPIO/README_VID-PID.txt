====================================================================================================
USB Vendor and Product IDs
====================================================================================================

The bootloader source code in this directory uses the VID and PID from the official 'Caterina'
bootloader from Arduino (0x2341 and 0x0037).

While this is acceptable for private use or limited distribution, commercial or public reuse of
this VID and PID by others may be prohibited by USB-IF.

Please edit 'Bootloader/Makefile' and replace them with your own VID and PID as required.

----------------------------------------------------------------------------------------------------

The firmware source code in this directory uses the VID and PID from the original 'LUFA Dual CDC
Demo Application' (0x03EB and 0x204E).

While this is acceptable for private use or limited distribution, commercial or public reuse of
this VID and PID by others may be prohibited by USB-IF, even if the 'JxMake USB-GPIO Module' is
technically still a 'LUFA Dual CDC' device.

Please edit 'Firmware/Descriptors.c' and replace them with your own VID and PID as required.

====================================================================================================
