.section .text, "ax", @progbits

.global nop
.type   nop, "function"
nop:
    push r0
    pop  r0
    ret
.size  nop, . - nop
