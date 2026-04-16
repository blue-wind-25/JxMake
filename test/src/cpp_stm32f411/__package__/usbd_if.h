#ifdef STM32_ENABLE_USB_DEVICE_CDC
    #include "device_cdc/usbd_cdc_if.h"
#endif

#ifdef STM32_ENABLE_USB_DEVICE_HID
    #include "usbd_hid.h"
    extern USBD_HandleTypeDef hUsbDeviceFS;
#endif
