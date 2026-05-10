#include "bitboard_utils.h"
#include <stdio.h>

// Precomputed attack tables
Bitboard WHITE_PAWN_ATTACKS[64];
Bitboard BLACK_PAWN_ATTACKS[64];

// King attack tables for different ranges
Bitboard KING_ATTACKS_RANGE_1[64];
Bitboard KING_ATTACKS_RANGE_2[64];
Bitboard KING_ATTACKS_RANGE_3[64];

// Array of pointers for branchless access (index 0 unused, 1-3 for ranges)
Bitboard* KING_ATTACKS_BY_RANGE[4];

// Horse (knight) attack table
Bitboard HORSE_ATTACKS[64];

// Built-in bit counting functions for better performance
int bb_count_bits(Bitboard bb) {
    return __builtin_popcountll(bb);
}

int bb_lsb_index(Bitboard bb) {
    return bb ? __builtin_ctzll(bb) : -1;
}

int bb_msb_index(Bitboard bb) {
    return bb ? 63 - __builtin_clzll(bb) : -1;
}

int bb_pop_lsb(Bitboard* bb) {
    int index = bb_lsb_index(*bb);
    *bb &= (*bb - 1);  // Clear LSB
    return index;
}

Bitboard bb_isolate_lsb(Bitboard bb) {
    return bb & -bb;
}

// Shifting operations (with bounds checking)
Bitboard bb_shift_north(Bitboard bb) {
    return bb << 8;
}

Bitboard bb_shift_south(Bitboard bb) {
    return bb >> 8;
}

Bitboard bb_shift_east(Bitboard bb) {
    return (bb << 1) & ~FILE_A;
}

Bitboard bb_shift_west(Bitboard bb) {
    return (bb >> 1) & ~FILE_H;
}

Bitboard bb_shift_northeast(Bitboard bb) {
    return (bb << 9) & ~FILE_A;
}

Bitboard bb_shift_northwest(Bitboard bb) {
    return (bb << 7) & ~FILE_H;
}

Bitboard bb_shift_southeast(Bitboard bb) {
    return (bb >> 7) & ~FILE_A;
}

Bitboard bb_shift_southwest(Bitboard bb) {
    return (bb >> 9) & ~FILE_H;
}

// Debug functions
void bb_print(Bitboard bb) {
    printf("Bitboard: 0x%016llX\n", (unsigned long long)bb);
    for (int rank = 7; rank >= 0; rank--) {
        printf("%d ", rank + 1);
        for (int file = 0; file < 8; file++) {
            int square = rank * 8 + file;
            printf("%c ", bb_is_set(bb, square) ? '1' : '.');
        }
        printf("\n");
    }
    printf("  a b c d e f g h\n");
}

void bb_print_binary(Bitboard bb) {
    printf("0b");
    for (int i = 63; i >= 0; i--) {
        printf("%c", (bb & (1ULL << i)) ? '1' : '0');
        if (i % 8 == 0 && i > 0) printf("_");
    }
    printf("\n");
}

// Initialize precomputed attack tables
void bb_init_attack_tables(void) {
    // Initialize pawn attack tables for each square
    for (int square = 0; square < 64; square++) {
        int file = square & 7;
        int rank = square >> 3;
        
        WHITE_PAWN_ATTACKS[square] = EMPTY_BITBOARD;
        BLACK_PAWN_ATTACKS[square] = EMPTY_BITBOARD;
        
        // White pawn attacks (move north/up)
        if (rank < 7) { // Not on 8th rank
            // Forward move (straight north)
            WHITE_PAWN_ATTACKS[square] |= bb_single_bit(square + 8);
            
            // Diagonal captures
            if (file > 0) { // Not on a-file, can capture northwest  
                WHITE_PAWN_ATTACKS[square] |= bb_single_bit(square + 7);
            }
            if (file < 7) { // Not on h-file, can capture northeast
                WHITE_PAWN_ATTACKS[square] |= bb_single_bit(square + 9);
            }
        }
        
        // Black pawn attacks (move south/down)
        if (rank > 0) { // Not on 1st rank
            // Forward move (straight south)
            BLACK_PAWN_ATTACKS[square] |= bb_single_bit(square - 8);
            
            // Diagonal captures
            if (file > 0) { // Not on a-file, can capture southwest
                BLACK_PAWN_ATTACKS[square] |= bb_single_bit(square - 9);
            }
            if (file < 7) { // Not on h-file, can capture southeast
                BLACK_PAWN_ATTACKS[square] |= bb_single_bit(square - 7);
            }
        }
    }
    
    // Initialize king attack tables for different ranges
    for (int range = 1; range <= 3; range++) {
        Bitboard* king_attacks = (range == 1) ? KING_ATTACKS_RANGE_1 :
                                (range == 2) ? KING_ATTACKS_RANGE_2 : 
                                              KING_ATTACKS_RANGE_3;
        
        for (int square = 0; square < 64; square++) {
            int file = square & 7;
            int rank = square >> 3;
            
            king_attacks[square] = EMPTY_BITBOARD;
            
            // Generate all king moves within the specified range
            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -range; dy <= range; dy++) {
                    if (dx == 0 && dy == 0) continue; // Skip the king's current position
                    
                    int new_file = file + dx;
                    int new_rank = rank + dy;
                    
                    // Check if the target square is within board bounds
                    if (new_file >= 0 && new_file < 8 && new_rank >= 0 && new_rank < 8) {
                        int target_square = new_rank * 8 + new_file;
                        king_attacks[square] |= bb_single_bit(target_square);
                    }
                }
            }
        }
    }
    
    // Initialize pointer array for branchless access
    KING_ATTACKS_BY_RANGE[0] = NULL;  // Index 0 unused
    KING_ATTACKS_BY_RANGE[1] = KING_ATTACKS_RANGE_1;
    KING_ATTACKS_BY_RANGE[2] = KING_ATTACKS_RANGE_2;
    KING_ATTACKS_BY_RANGE[3] = KING_ATTACKS_RANGE_3;
    
    // Initialize horse attack table
    // Horse move vectors (knight moves)
    const int horse_dx[] = {-2, -2, -1, -1, 1, 1, 2, 2};
    const int horse_dy[] = {-1, 1, -2, 2, -2, 2, -1, 1};
    const int horse_move_count = 8;
    
    for (int square = 0; square < 64; square++) {
        int file = square & 7;
        int rank = square >> 3;
        
        HORSE_ATTACKS[square] = EMPTY_BITBOARD;
        
        // Generate all horse moves for this square
        for (int i = 0; i < horse_move_count; i++) {
            int new_file = file + horse_dx[i];
            int new_rank = rank + horse_dy[i];
            
            // Check if the target square is within board bounds
            if (new_file >= 0 && new_file < 8 && new_rank >= 0 && new_rank < 8) {
                int target_square = new_rank * 8 + new_file;
                HORSE_ATTACKS[square] |= bb_single_bit(target_square);
            }
        }
    }
}