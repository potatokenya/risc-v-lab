jal x1, func
li x5, 0

func:
li x5, 123
jalr x0, x1, 4

li x6, 77