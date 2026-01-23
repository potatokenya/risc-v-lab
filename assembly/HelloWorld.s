# Hello World -> Memory mapped UART + LED demo
# UART base address = 0x10000000
# LED base address  = 0x20000000

    .section .text
    .globl _start
_start:
    # ------------------------
    # Example: Write 'H' to UART
    # ------------------------
    li t1, 72          # ASCII 'H' = 72
    lui t0, 0x10000    # upper 20 bits of UART base address
    sw t1, 0(t0)       # store 'H' to UART

    # Write 'e'
    li t1, 101         # ASCII 'e'
    sw t1, 0(t0)

    # Write 'l'
    li t1, 108
    sw t1, 0(t0)

    # Write second 'l'
    li t1, 108
    sw t1, 0(t0)

    # Write 'o'
    li t1, 111
    sw t1, 0(t0)

    # ------------------------
    # Example: Turn on LED 0xFF
    # ------------------------
    li t2, 0xFF        # LED pattern
    lui t0, 0x20000    # upper 20 bits of LED base address
    sw t2, 0(t0)       # write to LEDs

    # ------------------------
    # Infinite loop to halt
    # ------------------------
loop:
    j loop