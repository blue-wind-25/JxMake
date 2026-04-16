.section .text, "ax", @progbits

.global halt
.type   halt, "function"
halt:
    cli
    jmp halt
.size  halt, . - halt
