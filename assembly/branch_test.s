.section .text
.globl _start
_start:

# BEQ
addi x1, x0, 5
addi x2, x0, 5
beq  x1, x2, beq_ok
addi x10, x0, 0
beq_ok:
addi x10, x0, 1

# BNE
addi x1, x0, 5
addi x2, x0, 6
bne  x1, x2, bne_ok
addi x11, x0, 0
bne_ok:
addi x11, x0, 1

# BLT
addi x1, x0, 3
addi x2, x0, 7
blt  x1, x2, blt_ok
addi x12, x0, 0
blt_ok:
addi x12, x0, 1

# BGE
addi x1, x0, 7
addi x2, x0, 3
bge  x1, x2, bge_ok
addi x13, x0, 0
bge_ok:
addi x13, x0, 1

# BLTU
addi x1, x0, 1
addi x2, x0, 2
bltu x1, x2, bltu_ok
addi x14, x0, 0
bltu_ok:
addi x14, x0, 1

# BGEU
addi x1, x0, 3
addi x2, x0, 1
bgeu x1, x2, bgeu_ok
addi x15, x0, 0
bgeu_ok:
addi x15, x0, 1

loop:
j loop