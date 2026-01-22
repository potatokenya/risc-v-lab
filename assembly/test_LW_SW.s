    .section .text
    .globl _start

_start:
    addi x1, x0, 0x111   # x1 = 0x111
    addi x2, x0, 0x222   # x2 = 0x222
    sw   x2, 0(x1)       # store 123 to mem[16]
    nop                  # addi x0, x0, 0
    nop                  # addi x0, x0, 0
    lw   x3, 0(x1)       # load back into x3

    # missing forwarding
    nop                  # addi x0, x0, 0
    nop                  # addi x0, x0, 0
    nop                  # addi x0, x0, 0

    add  x4, x1, x2      # x4 = 0x444

    nop                  # to keep simulator alive