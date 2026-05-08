#ifndef FAST_BOT_H
#define FAST_BOT_H

#include "fast_board.h"
#include "evaluation.h"
#include "fast_move_generator.h"
#include "fast_transposition.h"
#include <stdbool.h>
#include <stddef.h>
#include <pthread.h>
#include <stdatomic.h>
#include <stdatomic.h>
#include <stdbool.h>

// Search constants
#define QUIESCENCE_DEPTH 5
#define NULL_WINDOW_SIZE 1  // Size of null window for PVS in EVAL_SCALE units

// Move evaluation structure
typedef struct {
    FastMove move;
    int16_t evaluation;
    int depth;
} MoveEvaluation;

// Bot result structure
typedef struct {
    MoveEvaluation best_move;
    MoveEvaluation* all_evaluations;
    int evaluation_count;
    long total_evaluations;
} BotResult;

// Transposition table types moved to fast_transposition.h

// Fast Flang Bot structure
typedef struct {
    int min_depth;
    int max_depth;
    bool use_opening_database;
    int threads;
    bool use_lme;
    int lme_max_extension;
    bool only_best_move;  // If true, only search for best move (skip all move evaluations)
    bool use_nnue;        // If true (and built with USE_NNUE), use NNUE; otherwise use HCE

    TranspositionTable tt;
    Evaluator evaluator;
    FastMoveGenerator move_generator;
    long total_evaluations;

    atomic_long se_candidates;  // positions that qualified for SE check
    atomic_long se_probes;      // singular probes actually run
    atomic_long se_extensions;  // singular extensions applied
    atomic_long se_multi_cuts;  // multi-cut prunes from SE probe
} FastFlangBot;

// Bot functions
void fast_bot_init(FastFlangBot* bot, int min_depth, int max_depth, int threads, int ttsize_mb, bool use_lme, int lme_max_extension, bool only_best_move, bool use_nnue);
void fast_bot_destroy(FastFlangBot* bot);
BotResult fast_bot_find_best_move(FastFlangBot* bot, FastBoard* board, bool print_time);
BotResult fast_bot_find_best_move_iterative(FastFlangBot* bot, FastBoard* board, bool print_time, long max_time_ms);

// Search functions
int16_t fast_bot_evaluate_move_ex(FastFlangBot* bot, FastBoard* board, uint8_t depth, int16_t alpha, int16_t beta, int ply, FastMove exclude_move);
static inline int16_t fast_bot_evaluate_move(FastFlangBot* bot, FastBoard* board, uint8_t depth, int16_t alpha, int16_t beta, int ply) {
    return fast_bot_evaluate_move_ex(bot, board, depth, alpha, beta, ply, NULL_MOVE);
}
int16_t fast_bot_quiescence(FastFlangBot* bot, FastBoard* board, int16_t alpha, int16_t beta, int depth);

// Transposition table functions moved to fast_transposition.h


// Helper functions
bool is_mate_score(int16_t value);
FastColor get_color_at_move(const FastBoard* board);

#endif // FAST_BOT_H