#ifndef FAST_ZOBRIST_H
#define FAST_ZOBRIST_H

#include "fast_board.h"
#include <stdint.h>

// Zobrist hashing functions
uint32_t zobrist_hash(const FastBoard* board);
void zobrist_init(void);
uint32_t zobrist_get_piece_key(FastType type, FastBoardIndex square, FastColor color);
uint32_t zobrist_get_side_to_move_key(void);
uint32_t zobrist_get_frozen_key(FastColor color, FastBoardIndex index);
uint32_t zobrist_get_move_number_key(int move_number);

// Debug functions
uint32_t zobrist_compute_hash_from_scratch(const FastBoard* board);
bool zobrist_verify_cached_hash(const FastBoard* board);

#endif // FAST_ZOBRIST_H