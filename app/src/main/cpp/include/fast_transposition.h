#ifndef FAST_TRANSPOSITION_H
#define FAST_TRANSPOSITION_H

#include "fast_board.h"
#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

// Node types for transposition table
#define TT_NODE_EXACT 0
#define TT_NODE_UPPER_BOUND 1
#define TT_NODE_LOWER_BOUND 2

// Special depth for mate scores
#define MATE_TT_DEPTH 99

// Transposition table entry
typedef struct {
    uint32_t zobrist_hash;
    FastMove best_move;
    uint8_t depth;
    int16_t value;
    uint8_t node_type; // 0=exact, 1=upper_bound, 2=lower_bound
} TTEntry;

// Transposition table
typedef struct {
    TTEntry* entries;
    size_t size;
    uint64_t hits;
    uint64_t probes;
} TranspositionTable;

// Transposition table functions
void tt_init(TranspositionTable* tt, size_t size_mb);
void tt_destroy(TranspositionTable* tt);
void tt_clear(TranspositionTable* tt);
void tt_store(TranspositionTable* tt, uint32_t hash, int16_t value, uint8_t depth, FastMove best_move, uint8_t node_type);
bool tt_probe(TranspositionTable* tt, uint32_t hash, uint8_t depth, int16_t alpha, int16_t beta, int16_t* value, FastMove* best_move);
TTEntry* tt_probe_entry(TranspositionTable* tt, uint32_t hash);  // Direct entry lookup

// Statistics
static inline double tt_hit_rate(const TranspositionTable* tt) {
    return tt->probes > 0 ? (double)tt->hits / tt->probes : 0.0;
}

#endif // FAST_TRANSPOSITION_H