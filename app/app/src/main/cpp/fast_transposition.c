#include "fast_transposition.h"
#include <stdlib.h>
#include <string.h>

void tt_init(TranspositionTable* tt, size_t size_mb) {
    size_t num_entries = (size_mb * 1024 * 1024) / sizeof(TTEntry);
    // Round down to nearest power of 2 for efficient indexing
    size_t power = 1;
    while (power <= num_entries) power <<= 1;
    power >>= 1;

    tt->size = power;
    tt->entries = (TTEntry*)calloc(tt->size, sizeof(TTEntry));
    tt->hits = 0;
    tt->probes = 0;
}

void tt_destroy(TranspositionTable* tt) {
    free(tt->entries);
    tt->entries = NULL;
    tt->size = 0;
}

void tt_clear(TranspositionTable* tt) {
    // memset(tt->entries, 0, tt->size * sizeof(TTEntry));
    tt->hits = 0;
    tt->probes = 0;
}

void tt_store(TranspositionTable* tt, uint32_t hash, int16_t value, uint8_t depth, FastMove best_move, uint8_t node_type) {
    size_t index = hash & (tt->size - 1);
    TTEntry* entry = &tt->entries[index];

    bool is_empty = (entry->depth == 0 && entry->zobrist_hash == 0);

    // Replacement rules (mirroring Kotlin)
    bool same_hash = (entry->zobrist_hash == hash);
    bool deeper_or_equal = (depth >= entry->depth);

    bool should_replace = is_empty || same_hash || deeper_or_equal;

    if (should_replace) {
        entry->zobrist_hash = hash;
        entry->value        = value;
        entry->depth        = depth;
        entry->best_move    = best_move;
        entry->node_type    = node_type;
    }
}

bool tt_probe(TranspositionTable* tt, uint32_t hash, uint8_t depth, int16_t alpha, int16_t beta, int16_t* value, FastMove* best_move) {
    tt->probes++;
    size_t index = hash & (tt->size - 1);
    TTEntry* entry = &tt->entries[index];

    if (entry->zobrist_hash != hash) return false;

    *best_move = entry->best_move;

    if (entry->depth >= depth) {
        tt->hits++;
        *value = entry->value;

        switch (entry->node_type) {
            case TT_NODE_EXACT: // EXACT
                return true;
            case TT_NODE_UPPER_BOUND: // UPPER_BOUND
                if (entry->value <= alpha) return true;
                break;
            case TT_NODE_LOWER_BOUND: // LOWER_BOUND
                if (entry->value >= beta) return true;
                break;
        }
    }

    return false;
}

// Direct TT entry lookup - returns entry if hash matches, NULL otherwise
TTEntry* tt_probe_entry(TranspositionTable* tt, uint32_t hash) {
    size_t index = hash & (tt->size - 1);
    TTEntry* entry = &tt->entries[index];

    if (entry->zobrist_hash == hash) {
        return entry;
    }

    return NULL;
}