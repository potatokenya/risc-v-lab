import sys

for line in sys.stdin:
    w = line.strip()
    if len(w) != 8:
        continue
    # split into bytes
    b0 = w[0:2]
    b1 = w[2:4]
    b2 = w[4:6]
    b3 = w[6:8]
    # reverse byte order
    print(b3 + b2 + b1 + b0)