#ifdef STM32_ENABLE_USB_DEVICE_CDC
    #include "device_cdc/usbd_cdc_if.c"
#endif

#ifdef STM32_ENABLE_USB_DEVICE_HID
    #include "usbd_if.h"
    USBD_HandleTypeDef hUsbDeviceFS;
#endif
