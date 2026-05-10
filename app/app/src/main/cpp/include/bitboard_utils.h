#ifndef BITBOARD_UTILS_H
#define BITBOARD_UTILS_H

#include <stdint.h>
#include <stdbool.h>

// Bitboard type definition
typedef uint64_t Bitboard;

// Constants for bitboard operations
#define EMPTY_BITBOARD 0ULL
#define FULL_BITBOARD 0xFFFFFFFFFFFFFFFFULL

// Board geometry constants
#define RANK_1 0x00000000000000FFULL
#define RANK_2 0x000000000000FF00ULL
#define RANK_3 0x0000000000FF0000ULL
#define RANK_4 0x00000000FF000000ULL
#define RANK_5 0x000000FF00000000ULL
#define RANK_6 0x0000FF0000000000ULL
#define RANK_7 0x00FF000000000000ULL
#define RANK_8 0xFF00000000000000ULL

#define FILE_A 0x0101010101010101ULL
#define FILE_B 0x0202020202020202ULL
#define FILE_C 0x0404040404040404ULL
#define FILE_D 0x0808080808080808ULL
#define FILE_E 0x1010101010101010ULL
#define FILE_F 0x2020202020202020ULL
#define FILE_G 0x4040404040404040ULL
#define FILE_H 0x8080808080808080ULL

// Basic bitboard operations
static inline bool bb_is_set(Bitboard bb, int square) {
    return (bb & (1ULL << square)) != 0;
}

static inline Bitboard bb_set_bit(Bitboard bb, int square) {
    return bb | (1ULL << square);
}

static inline Bitboard bb_clear_bit(Bitboard bb, int square) {
    return bb & ~(1ULL << square);
}

static inline Bitboard bb_toggle_bit(Bitboard bb, int square) {
    return bb ^ (1ULL << square);
}

static inline Bitboard bb_single_bit(int square) {
    return 1ULL << square;
}

// Advanced bitboard operations
int bb_count_bits(Bitboard bb);
int bb_lsb_index(Bitboard bb);  // Least significant bit index
int bb_msb_index(Bitboard bb);  // Most significant bit index
int bb_pop_lsb(Bitboard* bb);   // Pop and return LSB index
Bitboard bb_isolate_lsb(Bitboard bb);  // Isolate LSB

// Bit manipulation utilities
static inline bool bb_is_empty(Bitboard bb) {
    return bb == 0;
}

static inline bool bb_is_not_empty(Bitboard bb) {
    return bb != 0;
}

static inline bool bb_single_bit_set(Bitboard bb) {
    return bb != 0 && (bb & (bb - 1)) == 0;
}

// Square index conversion (maintains compatibility with existing index_of/get_x/get_y)
static inline int bb_square_to_index(int x, int y) {
    return y * 8 + x;
}

static inline int bb_index_to_x(int index) {
    return index & 7;
}

static inline int bb_index_to_y(int index) {
    return index >> 3;
}

// Bitboard shifting operations
Bitboard bb_shift_north(Bitboard bb);
Bitboard bb_shift_south(Bitboard bb);
Bitboard bb_shift_east(Bitboard bb);
Bitboard bb_shift_west(Bitboard bb);
Bitboard bb_shift_northeast(Bitboard bb);
Bitboard bb_shift_northwest(Bitboard bb);
Bitboard bb_shift_southeast(Bitboard bb);
Bitboard bb_shift_southwest(Bitboard bb);

// Rank and file operations
static inline Bitboard bb_get_rank(int rank) {
    return RANK_1 << (rank * 8);
}

static inline Bitboard bb_get_file(int file) {
    return FILE_A << file;
}

static inline int bb_get_rank_of_square(int square) {
    return square >> 3;
}

static inline int bb_get_file_of_square(int square) {
    return square & 7;
}

// Precomputed attack tables
extern Bitboard WHITE_PAWN_ATTACKS[64];
extern Bitboard BLACK_PAWN_ATTACKS[64];

// King attack tables for different ranges
extern Bitboard KING_ATTACKS_RANGE_1[64];
extern Bitboard KING_ATTACKS_RANGE_2[64];
extern Bitboard KING_ATTACKS_RANGE_3[64];

// Array of pointers for branchless access (index 0 unused, 1-3 for ranges)
extern Bitboard* KING_ATTACKS_BY_RANGE[4];

// Horse (knight) attack table
extern Bitboard HORSE_ATTACKS[64];

// Initialize precomputed tables
void bb_init_attack_tables(void);

// Debugging and display
void bb_print(Bitboard bb);
void bb_print_binary(Bitboard bb);

#endif // BITBOARD_UTILS_H