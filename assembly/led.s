        .section .text
        .globl _start

_start:
        li      t0, 0x20000000   # Base address for LEDs
        li      t1, 0x30000000   # Base address for 7-segment

loop:
        # ----------------------------
        # Blink LEDs: simple pattern
        # ----------------------------
        li      t2, 0x55          # LED pattern 01010101
        sw      t2, 0(t0)         # Write to LED MMIO
        call    delay

        li      t2, 0xAA          # LED pattern 10101010
        sw      t2, 0(t0)
        call    delay

        # ----------------------------
        # Update 7-segment: count 0–9
        # ----------------------------
        li      t3, 0             # counter 0..9

seg_loop:
        li      t4, 0x30000000    # 7-seg base
        add     t5, t3, zero      # current digit
        sw      t5, 0(t4)         # write to 7-seg MMIO
        call    delay
        addi    t3, t3, 1
        li      t6, 10
        blt     t3, t6, seg_loop

        j       loop              # repeat forever

# ----------------------------
# Simple delay routine
# ----------------------------
delay:
        li      s0, 500000
1:
        addi    s0, s0, -1
        bnez    s0, 1b
        ret
