.text

.globl atest
#if !defined(__CYGWIN__) && !defined(__MINGW32__) && !defined(__MSYS__)
.type  atest, @function
#endif
atest:
#if __x86_64__
    push %rax
    xor  %rax, %rax
    pop  %rax
#else
    push %eax
    xor  %eax, %eax
    pop  %eax
#endif
    ret
