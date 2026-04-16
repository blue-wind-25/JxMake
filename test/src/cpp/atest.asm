IFNDEF __x86_64__
.686
.MODEL FLAT, C
ENDIF

.STACK
.DATA
.CODE

atest PROC public
IFDEF __x86_64__
    push rax
    xor  rax, rax
    pop  rax
ELSE
    push eax
    xor  eax, eax
    pop  eax
ENDIF
    ret
atest ENDP

END
