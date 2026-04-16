/*
 * Please refer to the notes at the top of the 'mdk.h' file for more information.
 */

ENTRY(startup)

SECTIONS
{
    . = 0x00002000;       /* Flash - from 0x00002000 */

    .text :
    {
        FILL(0xff)
        KEEP(*(.vectors)) /* Vector table */
        *(.text*)         /* Program code */
        *(.rodata)        /* Read only data */
        *(.rodata.*)
        . = ALIGN(4);
    }

    . = ALIGN(4);
    _DATA_ROM_START = .;

    . = 0x20000000;       /* RAM - from 0x20000000 */

    _DATA_RAM_START = .;
    .data : AT(_DATA_ROM_START)
    {
        FILL(0xff)
        *(.ramfunc .ramfunc.*);
        *(vtable)
        *(.data*)
        . = ALIGN(4);
    }
    . = ALIGN(4);
    _DATA_RAM_END = .;

    .uninit_RESERVED :
    {
        KEEP(*(.bss.$RESERVED*))
    }

    . = ALIGN(4);
    _BSS_START = .;       /* Indicates where BSS section starts in RAM */
    .bss :
    {
        *(.bss*)          /* Zero-filled run time allocate data memory */
        *(COMMON)
        . = ALIGN(4);
    }
    _BSS_END = .;         /* Indicates where BSS section ends in RAM */
}
