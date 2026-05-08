"""Test FBN2 parser step by step"""

from board_parser import parse_fbn2

test_fbn2 = "+2PRHFUK2PPPPPP32ppp-ppp2kufhrp2"

print(f"Parsing: {test_fbn2}\n")

board, side_to_move, frozen = parse_fbn2(test_fbn2)

print(f"Side to move: {'WHITE' if side_to_move == 0 else 'BLACK'}\n")

print("Board contents:")
for i in range(64):
    piece_value = board[i]
    if piece_value != 6:  # Not empty
        piece_names = ["Pawn", "Rook", "Horse", "Flanger", "Uni", "King"]
        if piece_value < 6:
            piece_type = piece_value
            color = "White"
        else:
            piece_type = piece_value - 6
            color = "Black"

        frozen_str = " (FROZEN)" if frozen[i] else ""
        print(f"  Square {i:2d}: {color} {piece_names[piece_type]}{frozen_str}")

print(f"\nTotal pieces: {(board != 6).sum()}")
print(f"Frozen pieces: {frozen.sum()}")
