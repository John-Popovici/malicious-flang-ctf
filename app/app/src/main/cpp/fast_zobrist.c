#include "fast_zobrist.h"
#include <stdint.h>
#include <stdbool.h>
#include <assert.h>

// Java Random for consistency with Kotlin implementation
typedef struct {
    uint64_t seed48;
} JavaRand;

// Zobrist keys - match Kotlin's dimensions/order: [piece][square][color]
static uint32_t piece_square_keys[7][64][2];
static uint32_t side_to_move_key;
static uint32_t move_number_keys[200];
static uint32_t frozen_white_keys[64];
static uint32_t frozen_black_keys[64];
static bool zobrist_initialized = false;

// Java Random implementation for consistent key generation
static void jrand_init(JavaRand* r, uint64_t seed) {
    // Java seeds with (seed ^ 0x5DEECE66D) & ((1<<48)-1)
    r->seed48 = (seed ^ 0x5DEECE66DULL) & ((1ULL << 48) - 1);
}

static int32_t jrand_next(JavaRand* r, int bits) {
    r->seed48 = (r->seed48 * 0x5DEECE66DULL + 0xBULL) & ((1ULL << 48) - 1);
    return (int32_t)(r->seed48 >> (48 - bits));
}

void zobrist_init(void) {
    if (zobrist_initialized) return;

    JavaRand rng;
    jrand_init(&rng, 43ULL);

    // piece-square-color
    for (int piece = 0; piece < 7; ++piece) {
        for (int sq = 0; sq < 64; ++sq) {
            for (int color = 0; color < 2; ++color) {
                piece_square_keys[piece][sq][color] = (uint32_t)jrand_next(&rng, 32);
            }
        }
    }

    // frozen piece keys
    for (int sq = 0; sq < 64; ++sq) {
        frozen_white_keys[sq] = (uint32_t)jrand_next(&rng, 32);
        frozen_black_keys[sq] = (uint32_t)jrand_next(&rng, 32);
    }

    // move number keys (0..199)
    for (int m = 0; m < 200; ++m) {
        move_number_keys[m] = (uint32_t)jrand_next(&rng, 32);
    }

    side_to_move_key = (uint32_t)jrand_next(&rng, 32);

    zobrist_initialized = true;
}

uint32_t zobrist_hash(const FastBoard* board) {
    return board->cached_hash;
}

// Zobrist key accessor functions for incremental updates
uint32_t zobrist_get_piece_key(FastType type, FastBoardIndex square, FastColor color) {
    if (!zobrist_initialized) zobrist_init();
    int color_idx = color ? 1 : 0; // FAST_BLACK = true = 1, FAST_WHITE = false = 0
    return piece_square_keys[type][square][color_idx];
}

uint32_t zobrist_get_side_to_move_key(void) {
    if (!zobrist_initialized) zobrist_init();
    return side_to_move_key;
}

uint32_t zobrist_get_frozen_key(FastColor color, FastBoardIndex index) {
    if (!zobrist_initialized) zobrist_init();
    return color ? frozen_white_keys[index] : frozen_black_keys[index];
}

uint32_t zobrist_get_move_number_key(int move_number) {
    if (!zobrist_initialized) zobrist_init();
    return move_number_keys[move_number % 200];
}

// Debug function to compute hash from scratch (ignoring cache)
uint32_t zobrist_compute_hash_from_scratch(const FastBoard* board) {
    if (!zobrist_initialized) zobrist_init();

    uint32_t hash = 0;

    // pieces on board
    for (int sq = 0; sq < ARRAY_SIZE; ++sq) {
        FastPieceState piece = fast_board_get_at(board, sq);
        FastType type = piece.type;
        if (type != FAST_NONE) {
            FastColor color = piece.color;
            int color_idx = color ? 1 : 0; // FAST_BLACK = true = 1, FAST_WHITE = false = 0
            hash ^= piece_square_keys[type][sq][color_idx];
        }
    }

    // side to move
    if (board->at_move) {
        hash ^= side_to_move_key;
    }

    // frozen pieces
    if (board->frozen_white_index != -1) {
        hash ^= frozen_white_keys[board->frozen_white_index];
    }
    if (board->frozen_black_index != -1) {
        hash ^= frozen_black_keys[board->frozen_black_index];
    }

    // move number
    int move_num = board->move_number % 200;
    hash ^= move_number_keys[move_num];

    return hash;
}

// Debug function to verify cached hash matches computed hash
bool zobrist_verify_cached_hash(const FastBoard* board) {
    uint32_t cached = board->cached_hash;
    uint32_t computed = zobrist_compute_hash_from_scratch(board);
    return cached == computed;
}