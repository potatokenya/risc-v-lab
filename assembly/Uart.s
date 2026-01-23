li t0, 0x20000000
li t1, 0xAA
sw t1, 0(t0)        # LEDs

li t0, 0x30000000
li t1, 0x1234
sw t1, 0(t0)        # Seven segment

li t0, 0x10000000
li t1, 'A'
sw t1, 0(t0)        # UART