"""Trace FBN2 parser execution"""

import numpy as np

PIECE_CHARS = {
    'P': (0, 0), 'p': (0, 1),
    'R': (1, 0), 'r': (1, 1),
    'H': (2, 0), 'h': (2, 1),
    'F': (3, 0), 'f': (3, 1),
    'U': (4, 0), 'u': (4, 1),
    'K': (5, 0), 'k': (5, 1),
}

fbn2 = "+2PRHFUK2PPPPPP32ppp-ppp2kufhrp2"

board = np.full(64, 6, dtype=np.int8)
frozen = np.zeros(64, dtype=bool)

position = 0
i = 1
digit_buffer = ""

print(f"Parsing: {fbn2}\n")

while i < len(fbn2) and position < 64:
    c = fbn2[i]
    print(f"i={i:2d}, pos={position:2d}, char='{c}'", end="")

    # Handle numbers (empty squares)
    if c.isdigit():
        digit_buffer += c
        print(f" -> accumulating digit: '{digit_buffer}'")
        i += 1
        continue

    # Process accumulated digits
    if digit_buffer:
        empty_count = int(digit_buffer)
        print(f" -> processing {empty_count} empty squares (pos {position} -> {position + empty_count})")
        position += empty_count
        digit_buffer = ""

    # Handle piece characters
    if c.upper() in 'PRHFUK':
        if position >= 64:
            break

        piece_type, color = PIECE_CHARS[c]
        board[position] = piece_type + (6 * color)

        # Check if next character is '-' (frozen)
        if i + 1 < len(fbn2) and fbn2[i + 1] == '-':
            frozen[position] = True
            print(f" -> piece '{c}' at pos {position} (FROZEN), next is '-', i += 2")
            i += 2
        else:
            print(f" -> piece '{c}' at pos {position}, i += 1")
            i += 1

        position += 1
    elif c == '-':
        print(f" -> standalone '-', skipping")
        i += 1
    else:
        print(f" -> unknown char, skipping")
        i += 1

print(f"\nFinal position: {position}")
print(f"Total pieces: {(board != 6).sum()}")
