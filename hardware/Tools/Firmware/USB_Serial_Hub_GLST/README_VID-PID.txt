====================================================================================================
USB Vendor and Product IDs
====================================================================================================

The firmware source code in this directory originally used the VID and PID from the original
'STMicroelectronics Virtual COM Port' demo firmware (0x0483 and 0x5740).

It now uses the VID and PID allocated by 'pid.codes' at 'https://pid.codes/1209/25F0'
(0x1209 and 0x25F0).

If you derive an OSHW project from this design and firmware, using the same VID and PID should be
fine. However, 'pid.codes' allocations are intended for open-source hardware projects. Commercial
products, or other uses that do not meet the 'pid.codes' requirements, may not reuse these values.

Please edit '__package__/device_cdc/usbd_desc.c' and replace them with your own VID and PID as
required.

====================================================================================================
