====================================================================================================
USB Vendor and Product IDs
====================================================================================================

The firmware in this directory is based on:

    Device CDC Dual Ports Example
        <pico-sdk-root>/lib/tinyusb/examples/device/cdc_dual_ports

    Source:
        Raspberry Pi Pico SDK Version 1.5.1
        https://github.com/raspberrypi/pico-sdk/tree/1.5.1
        https://github.com/raspberrypi/pico-sdk/releases/tag/1.5.1
        https://github.com/raspberrypi/pico-sdk/archive/refs/tags/1.5.1.tar.gz

The firmware source code in this directory uses the VID and PID from the original example (0xCAFE
and 0x4002).

While this is acceptable for private use or limited distribution, commercial or public reuse of
this VID and PID by others may be prohibited by USB-IF, even if this firmware technically still
implements a 'TinyUSB CDC-ACM' device.

Please edit 'src/usb_descriptors.c' and replace them with your own VID and PID as required.

====================================================================================================
