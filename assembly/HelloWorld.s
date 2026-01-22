# Hello World -> Memory mapped UART + LED demo
# UART base address = 0x10000000
# LED base address  = 0x20000000

# store 'H', 'i', '!' in memory
la t0, msg      # t0 -> msg address
lw t1, 0(t0)    # load first char
sw t1, 0x10000000  # UART transmit
addi t0, t0, 4
lw t1, 0(t0)
sw t1, 0x10000000
addi t0, t0, 4
lw t1, 0(t0)
sw t1, 0x10000000

# blink LED
li t2, 0x01
sw t2, 0x20000000

# infinite loop
loop:
  bge t2, zero, loop

msg:
  .word 0x48      # 'H'
  .word 0x69      # 'i'
  .word 0x21      # '!'