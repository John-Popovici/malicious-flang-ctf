"""
FBN2 (Flang Board Notation v2) parser for chess variant positions.

FBN2 format: +/-[pieces][frozen_state]
- First character: + (white to move) or - (black to move)
- Pieces: P(pawn), R(rook), H(horse), F(flanger), U(uni), K(king)
- Uppercase = white, lowercase = black
- Numbers indicate empty squares
- '-' after piece = frozen piece
"""

import numpy as np

# Piece types
PIECE_NONE = 0
PIECE_PAWN = 1
PIECE_HORSE = 2
PIECE_ROOK = 3
PIECE_FLANGER = 4
PIECE_UNI = 5
PIECE_KING = 6

# Colors
COLOR_WHITE = 0
COLOR_BLACK = 1

PIECE_CHARS = {
    'P': (PIECE_PAWN, COLOR_WHITE),
    'p': (PIECE_PAWN, COLOR_BLACK),
    'R': (PIECE_ROOK, COLOR_WHITE),
    'r': (PIECE_ROOK, COLOR_BLACK),
    'H': (PIECE_HORSE, COLOR_WHITE),
    'h': (PIECE_HORSE, COLOR_BLACK),
    'F': (PIECE_FLANGER, COLOR_WHITE),
    'f': (PIECE_FLANGER, COLOR_BLACK),
    'U': (PIECE_UNI, COLOR_WHITE),
    'u': (PIECE_UNI, COLOR_BLACK),
    'K': (PIECE_KING, COLOR_WHITE),
    'k': (PIECE_KING, COLOR_BLACK),
}


def parse_fbn2(fbn2: str):
    """
    Parse FBN2 notation into a board representation.

    Returns:
        tuple: (board_array, side_to_move, frozen_mask)
            - board_array: 64-element array with piece types
            - side_to_move: 0 (white) or 1 (black)
            - frozen_mask: 64-element boolean array indicating frozen pieces
    """
    if not fbn2 or fbn2[0] not in ['+', '-']:
        raise ValueError(f"Invalid FBN2: must start with + or -")

    side_to_move = COLOR_WHITE if fbn2[0] == '+' else COLOR_BLACK

    # Board stores (type, color) as a 2-element array per square
    # type: 0=none, 1=pawn, 2=rook, 3=horse, 4=flanger, 5=uni, 6=king
    # color: 0=white, 1=black
    board = np.zeros((64, 2), dtype=np.int8)  # [type, color]
    frozen = np.zeros(64, dtype=bool)

    position = 0
    i = 1
    digit_buffer = ""

    while i < len(fbn2) and position < 64:
        c = fbn2[i]

        # Handle numbers (empty squares)
        if c.isdigit():
            digit_buffer += c
            i += 1
            continue

        # Process accumulated digits
        if digit_buffer:
            empty_count = int(digit_buffer)
            position += empty_count
            digit_buffer = ""

        # Handle piece characters
        if c.upper() in 'PRHFUK':
            if position >= 64:
                break

            piece_type, color = PIECE_CHARS[c]
            board[position] = [piece_type, color]

            # Check if next character is '-' (frozen)
            if i + 1 < len(fbn2) and fbn2[i + 1] == '-':
                frozen[position] = True
                i += 2  # Skip both piece and '-'
            else:
                i += 1  # Skip just piece

            position += 1
            # Don't continue to else clause after handling piece
        elif c == '-':
            # Skip standalone '-' (shouldn't happen in valid FBN2)
            i += 1
        else:
            i += 1

    return board, side_to_move, frozen


def board_to_features(board, side_to_move, frozen):
    """
    Convert board representation to neural network input features.

    Perspective encoding: the board is always shown from the side-to-move's view.
    For black, squares are mirrored vertically (sq ^ 56).

    Planes:
    - 0-5:  own pieces (pawn, rook, horse, flanger, uni, king)
    - 6-11: opponent pieces
    - 12:   own frozen pieces
    - 13:   opponent frozen pieces

    Total: 14 planes × 64 squares = 896 features
    """
    features = np.zeros((14, 8, 8), dtype=np.int8)

    for square in range(64):
        # Mirror the square vertically for black's perspective
        persp_sq = square ^ 56 if side_to_move == COLOR_BLACK else square
        persp_row = persp_sq // 8
        persp_col = persp_sq % 8

        piece_type, piece_color = board[square]

        if piece_type != PIECE_NONE:
            is_own = (piece_color == side_to_move)
            plane = (piece_type - 1) + (0 if is_own else 6)
            features[plane, persp_row, persp_col] = 1

        if frozen[square] and piece_type != PIECE_NONE:
            is_own_frozen = (piece_color == side_to_move)
            features[12 if is_own_frozen else 13, persp_row, persp_col] = 1

    return features


def parse_position(fbn2_str: str):
    """
    Parse FBN2 string and convert to neural network features.

    Args:
        fbn2_str: FBN2 notation string

    Returns:
        np.ndarray: Feature tensor of shape (14, 8, 8)
    """
    board, side_to_move, frozen = parse_fbn2(fbn2_str)
    features = board_to_features(board, side_to_move, frozen)
    return features


if __name__ == "__main__":
    # Test with example positions
    test_positions = [
        "+2PRHFUK2PPPPPP32pppppp2kufhrp2",  # Starting position
        "-2PRHFUK2PPPPP7P-25pppppp2kufhrp2",  # After one move
    ]

    for pos in test_positions:
        print(f"\nParsing: {pos}")
        features = parse_position(pos)
        print(f"Feature shape: {features.shape}")
        print(f"Non-zero elements: {np.count_nonzero(features)}")