/* Linker script to configure memory regions. */

SEARCH_DIR(.)
GROUP(-lgcc -lc -lnosys)

MEMORY
{
    /* https://infocenter.nordicsemi.com/index.jsp?topic=%2Fsdk_nrf5_v17.0.2%2Flib_bootloader.html */
    FLASH (RX ) : ORIGIN = 0x00027000, LENGTH = 0x000ED000 - 0x00027000
    RAM   (RWX) : ORIGIN = 0x20006000, LENGTH = 0x20040000 - 0x20006000
}

INCLUDE "nrf_common.ld"
