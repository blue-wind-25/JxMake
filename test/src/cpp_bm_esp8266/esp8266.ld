MEMORY
{
    dport0_0_seg : org = 0x3FF00000, len = 0x00000010
    dram0_0_seg  : org = 0x3FFE8000, len = 0x00014000
    iram1_0_seg  : org = 0x40100000, len = 0x00008000
    irom0_0_seg  : org = 0x40240000, len = 0x0003C000
}


SECTIONS
{
    .init    : { KEEP(*(.init*)   ) } > iram1_0_seg
    .literal : {      *(.literal*)  } > iram1_0_seg
    .text    : {      *(.text*)     } > iram1_0_seg
    .rodata  : {      *(.rodata*)   } > iram1_0_seg

    .data    : {      *(.data*)     } > dram0_0_seg
    .bss     : {      *(.bss*)      } > dram0_0_seg
}

PROVIDE ( ets_delay_us = 0x40002ecc );
PROVIDE ( ets_update_cpu_frequency = 0x40002f04 );
