#ifndef FAST_MOVE_ORDER_H
#define FAST_MOVE_ORDER_H

#include <stdint.h>
#include <stdbool.h>
#include <string.h> // memset
#include <stdlib.h> // qsort

#include "fast_board.h"

/*
    Mirrors your Kotlin priorities:
    Hash move > Captures (MVV-LVA) > Killer moves > History > Quiet moves (+tiny king advance nudge)

    Notes:
    - Uses ARRAY_SIZE (128) for the history table (since your indices are 0..127).
    - Keeps two killer moves per ply; MAX_PLY can be tuned (64 works for most searches).
    - Uses your getters: get_move_from/To, get_move_from_piece/To_piece, get_piece_type, get_y, get_evaluation_number, etc.
*/

#ifndef MAX_PLY
#define MAX_PLY 256
#endif

#define HISTORY_MAX 10000

// Move scoring structure for sorting
typedef struct {
    FastMove m;
    int      score;
    int      idx;   // for stable tie-break
} ScoredMove;

static _Thread_local FastMove tl_killer_moves[MAX_PLY][2];
static _Thread_local int      tl_history_table[ARRAY_SIZE][ARRAY_SIZE];
static _Thread_local ScoredMove tl_scored_moves[MAX_MOVES];  // Reusable buffer for move ordering

// Call once per root search
static inline void mo_clear(void) {
    memset(tl_killer_moves, 0, sizeof(tl_killer_moves));
    memset(tl_history_table, 0, sizeof(tl_history_table));
}

static inline int mo_is_capture(FastMove m) {
    return m.to_piece.type != FAST_NONE;
}

static inline int mo_piece_value(FastType t) {
    switch (t) {
        case FAST_PAWN:   return 100;
        case FAST_HORSE:  return 300;
        case FAST_FLANGER:return 400;
        case FAST_ROOK:   return 500;
        case FAST_UNI:    return 900;
        case FAST_KING:   return 10000;
        default:          return 0;
    }
}

static inline int mo_killer_score(FastMove m, int ply) {
    if (ply < 0 || ply >= MAX_PLY) return 0;
    if (move_equals(tl_killer_moves[ply][0], m)) return 900;
    if (move_equals(tl_killer_moves[ply][1], m)) return 890;
    return 0;
}

static inline int mo_history_score(FastBoardIndex from, FastBoardIndex to) {
    return tl_history_table[from][to] / 10;
}

// Update on beta-cutoff (quiet-only inside)
static inline void mo_update_killer(FastMove m, int ply) {
    if (ply < 0 || ply >= MAX_PLY) return;
    if (mo_is_capture(m)) return;
    if (!move_equals(tl_killer_moves[ply][0], m)) {
        tl_killer_moves[ply][1] = tl_killer_moves[ply][0];
        tl_killer_moves[ply][0] = m;
    }
}

// Update on beta-cutoff
static inline void mo_update_history(FastMove m, int remaining_depth) {
    FastBoardIndex from = m.from;
    FastBoardIndex to   = m.to;
    int bonus = remaining_depth * remaining_depth;
    int v = tl_history_table[from][to] + bonus;
    if (v > HISTORY_MAX) {
        for (int i = 0; i < ARRAY_SIZE; ++i)
            for (int j = 0; j < ARRAY_SIZE; ++j)
                tl_history_table[i][j] /= 2;
        v = tl_history_table[from][to] + bonus;
    }
    tl_history_table[from][to] = v;
}

// Optional tiny nudge for king advancing toward winning side (mirrors your Kotlin)
static inline int mo_king_advance_bonus(FastMove m) {
    FastPieceState from_ps = m.from_piece;
    if (from_ps.type != FAST_KING) return 0;

    FastBoardIndex from = m.from;
    FastBoardIndex to   = m.to;
    int from_y = get_y(from);
    int to_y   = get_y(to);

    // evaluationNumber: +1 (white) / -1 (black)
    int eval_num = get_evaluation_number(from_ps.color);
    // If moving toward the winning rank, reward slightly
    if ((from_y - to_y) * eval_num > 0) return 10;
    return 0;
}

// Score move (higher = better)
static inline int mo_score_move(FastMove m, FastMove hash_move, int ply) {
    int score = 0;

    // 1) Hash move
    if (move_equals(m, hash_move)) score += 10000;

    // 2) Captures: MVV-LVA
    FastPieceState to_ps   = m.to_piece;
    FastType to_type = to_ps.type;
    if (to_type != FAST_NONE) {
        FastPieceState from_ps = m.from_piece;
        FastType from_type = from_ps.type;
        int victim   = mo_piece_value(to_type);
        int attacker = mo_piece_value(from_type);
        score += 1000 + (victim - attacker);
    }

    // 3) Killers
    score += mo_killer_score(m, ply);

    // 4) History
    FastBoardIndex from = m.from;
    FastBoardIndex to   = m.to;
    score += mo_history_score(from, to);

    // 5) Tiny king-advance nudge
    score += mo_king_advance_bonus(m);

    return score;
}

// ---------- Sorting support ----------

static inline void sort_desc_stable(ScoredMove *a, int n) {
    // Stable insertion sort: O(n^2) but tiny constant factors and branch-friendly.
    for (int i = 1; i < n; ++i) {
        ScoredMove x = a[i];
        int j = i - 1;
        // Desc by score; if equal, idx ASC
        while (j >= 0 && (a[j].score < x.score ||
                          (a[j].score == x.score && a[j].idx > x.idx))) {
            a[j + 1] = a[j];
            --j;
        }
        a[j + 1] = x;
    }
}

/*
    Orders moves from `moves` into `out_moves` (capacity out_cap).
    Returns the number of moves written.
*/
static inline int mo_order_moves(const MoveBuffer* moves,
                                 FastMove hash_move,
                                 int ply,
                                 FastMove* out_moves,
                                 int out_cap)
{
    int n = moves->count;
    if (n <= 0) return 0;
    if (n > out_cap) n = out_cap;
    if (n == 1) { out_moves[0] = moves->moves[0]; return 1; }

    // Use thread-local buffer to avoid stack allocation on every call
    ScoredMove* tmp = tl_scored_moves;
    for (int i = 0; i < n; ++i) {
        tmp[i].m     = moves->moves[i];
        tmp[i].score = mo_score_move(tmp[i].m, hash_move, ply);
        tmp[i].idx   = i; // for stable tie-break
    }

    sort_desc_stable(tmp, n);

    for (int i = 0; i < n; ++i) out_moves[i] = tmp[i].m;
    return n;
}


#endif // FAST_MOVE_ORDER_H
