/*
 * Please refer to the notes at the top of the 'main.cpp' file for more information.
 */

MEMORY {
    /* ESP32 Embedded Memory Segments */
    /* https://www.espressif.com/sites/default/files/documentation/esp32_technical_reference_manual_en.pdf */
    irom0 (rwx)  : ORIGIN = 0x40000000, LENGTH =   384k /* 0x40000000 - 0x4005ffff - IBUS            */
    irom1 (rw )  : ORIGIN = 0x3ff90000, LENGTH =    64k /* 0x3ff90000 - 0x3ff9ffff - DBUS            */
    isram0(rwx)  : ORIGIN = 0x40070000, LENGTH =   192k /* 0x40070000 - 0x4009ffff                   */
    isram1(rw )  : ORIGIN = 0x3ffe0000, LENGTH =   128k /* 0x3ffe0000 - 0x3fffffff - DMA             */
    isram2(rw )  : ORIGIN = 0x3ffae000, LENGTH =   200k /* 0x3ffae000 - 0x3ffdffff - DMA             */

    dflash(r  )  : ORIGIN = 0x3f400000, LENGTH =  4096k /* 0x3f400000 - 0x3f7fffff - DBUS - External */
    psram (rw )  : ORIGIN = 0x3f800000, LENGTH =  8192k /* 0x3f800000 - 0x3fffffff - DBUS - External */
    iflash(rx )  : ORIGIN = 0x400c2000, LENGTH = 11512k /* 0x400c2000 - 0x3ffdffff - IBUS - External */

    /* Internal SRAM 0 Distribution */
    /* https://blog.espressif.com/esp32-programmers-memory-model-259444d89387 */
    cache0(rwx)  : ORIGIN = 0x40070000, LENGTH =    32k /* 0x40070000 - 0x40077fff - CPU 0 Cache     */
    cache1(rwx)  : ORIGIN = 0x40078000, LENGTH =    32k /* 0x40078000 - 0x4007ffff - CPU 1 Cache     */
    vector(rwx)  : ORIGIN = 0x40080000, LENGTH =     1k /* 0x40080000 - 0x400803ff                   */
    iram  (rwx)  : ORIGIN = 0x40080400, LENGTH =   127k /* 0x40080400 - 0x4009ffff                   */

    /* Internal SRAM 2 and SRAM 1 Distribution */
    /* https://blog.espressif.com/esp32-programmers-memory-model-259444d89387 */
    _rev1_(r  )  : ORIGIN = 0x3ffae000, LENGTH =     8k /* 0x3ffae000 - 0x3ffaffff - Used by ROM     */
    dram  (rw )  : ORIGIN = 0x3ffb0000, LENGTH =   320k /* 0x3ffb0000 - 0x3fffffff                   */
}

_vector_beg = ORIGIN(vector);
_dram_end   = ORIGIN(dram  ) + LENGTH(dram);

ENTRY(_start_cpu0)

SECTIONS {
    /*
    .vectors : { KEEP(*(.vectors)) } > iram
    */

    .text : {
        . = ALIGN(4);
        _stext = .;
        *(.text*)
        *(.text)
        . = ALIGN(4);
        _etext = .;
    } > iram

    .data : {
        . = ALIGN(4);
        _sdata = .;
        *(.data)
        *(.data*)
        *(.rodata)
        *(.rodata*)
        *(.gnu.linkonce.r.*)
        *(.rodata1)
        . = ALIGN(4);
        _edata = .;
    } > dram

    .bss (NOLOAD) : {
        . = ALIGN(4);
        _sbss = .;
        *(.bss)
        *(.bss*)
        *(COMMON)
        . = ALIGN(4);
        _ebss = .;
    } > dram

    . = ALIGN(4);
    PROVIDE(_user_bytes_end = .);

    /* ##### !!! TODO : Is this actually correct because using '.dflash (NOLOAD)' does not work? !!! ##### */
    .dflash : {
        . = ALIGN(4);
        _sdflash = .;
        *(.dflash*)
        . = ALIGN(4);
        _edflash = .;
    } > dflash

    /*
    /DISCARD/ : { *(.debug) *(.debug*) *(.xtensa.*) *(.comment) }
    */
}

PROVIDE(mmu_init = 0x400095a4);

PROVIDE(cache_flash_mmu_set_rom = 0x400095e0);
PROVIDE(Cache_Flush_rom = 0x40009a14 );
PROVIDE(Cache_Read_Disable_rom = 0x40009ab8);
PROVIDE(Cache_Read_Enable_rom = 0x40009a84);
PROVIDE(Cache_Read_Init_rom = 0x40009950);

PROVIDE(uartAttach = 0x40008fd0);
PROVIDE(uart_rx_one_char = 0x400092d0);
PROVIDE(uart_tx_one_char = 0x40009200);
PROVIDE(uart_tx_flush = 0x40009258);
PROVIDE(uart_tx_switch = 0x40009028);
PROVIDE(ets_install_uart_printf = 0x40007d28);
PROVIDE(ets_printf = 0x40007d54);

PROVIDE(memchr = 0x4000c244);
PROVIDE(memcmp = 0x4000c260);
PROVIDE(memcpy = 0x4000c2c8);
PROVIDE(memmove = 0x4000c3c0);
PROVIDE(memrchr = 0x4000c400);
PROVIDE(memset = 0x4000c44c);
PROVIDE(qsort = 0x40056424);
PROVIDE(rand = 0x40001058);
PROVIDE(srand = 0x40001004);
PROVIDE(strcasecmp = 0x400011cc);
PROVIDE(strcasestr = 0x40001210);
PROVIDE(strcat = 0x4000c518);
PROVIDE(strchr = 0x4000c53c);
PROVIDE(strcmp = 0x40001274);
PROVIDE(strcpy = 0x400013ac);
PROVIDE(strcspn = 0x4000c558);
PROVIDE(strdup = 0x4000143c);
PROVIDE(strlen = 0x400014c0);
PROVIDE(strncasecmp = 0x40001550);
PROVIDE(strncat = 0x4000c5c4);
PROVIDE(strncmp = 0x4000c5f4);
PROVIDE(strncpy = 0x400015d4);
PROVIDE(strndup = 0x400016b0);
PROVIDE(strnlen = 0x4000c628);
PROVIDE(strrchr = 0x40001708);
PROVIDE(strsep = 0x40001734);
PROVIDE(strspn = 0x4000c648);
PROVIDE(strstr = 0x4000c674);
PROVIDE(strtol = 0x4005681c);
