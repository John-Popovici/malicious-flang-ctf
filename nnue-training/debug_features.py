"""
Debug feature extraction to see exactly what features are generated.
"""

import numpy as np
from board_parser import parse_position

# Test position
test_fbn2 = "+2PRHFUK2PPPPPP32ppp-ppp2kufhrp2"

print("=== Debug Feature Extraction ===\n")
print(f"Position: {test_fbn2}\n")

# Parse position
features = parse_position(test_fbn2)

print(f"Feature shape: {features.shape}")
print(f"Total features: {features.size}")
print(f"Non-zero features: {np.count_nonzero(features)}\n")

# Show which planes have pieces
plane_names = [
    "White Pawn", "White Rook", "White Horse", "White Flanger", "White Uni", "White King",
    "Black Pawn", "Black Rook", "Black Horse", "Black Flanger", "Black Uni", "Black King",
    "Frozen", "Side to Move"
]

for plane_idx in range(14):
    plane = features[plane_idx]
    non_zero = np.count_nonzero(plane)
    if non_zero > 0:
        print(f"Plane {plane_idx:2d} ({plane_names[plane_idx]:15s}): {non_zero:2d} non-zero")
        # Show positions for piece planes
        if plane_idx < 12:
            positions = np.argwhere(plane == 1.0)
            squares = [pos[0] * 8 + pos[1] for pos in positions]
            print(f"           Squares: {squares}")
        elif plane_idx == 12:  # Frozen
            positions = np.argwhere(plane == 1.0)
            squares = [pos[0] * 8 + pos[1] for pos in positions]
            print(f"           Frozen squares: {squares}")
        elif plane_idx == 13:  # Side to move
            unique_values = np.unique(plane)
            print(f"           Value: {unique_values[0]:.1f}")

print("\nDetailed board representation:")
print("Expected pieces (FBN2 order, square 0-63):")
print("  Squares 2-7: PRHFUK (white)")
print("  Squares 8-13: PPPPPP (white pawns)")
print("  Squares 32-34: ppp (black pawns)")
print("  Squares 35: p- (frozen black pawn)")
print("  Squares 36-38: ppp (black pawns)")
print("  Squares 56-61: kufhrp (black)")
