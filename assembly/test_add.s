    .section .text
    .globl _start

_start:
    addi x1, x0, 0x111   # x1 = 0x111
    addi x2, x0, 0x222   # x2 = 0x222

    # missing forwarding
    nop                  # addi x0, x0, 0
    nop                  # addi x0, x0, 0
    nop                  # addi x0, x0, 0

    add  x3, x1, x2      # x3 = 0x333

    nop                  # to keep simulator alive
