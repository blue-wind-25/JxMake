.module src0a
.optsdcc -mmcs51 --model-small

.area CSEG (CODE)
.globl _dummy_asm
_dummy_asm:
    mov dptr, #0x0000
    ret
