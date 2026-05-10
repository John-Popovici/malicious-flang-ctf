#define _POSIX_C_SOURCE 199309L
#include "fast_bot.h"
#include "fast_transposition.h"
#include "fast_move_order.h"
#include "fast_zobrist.h"
#include "bitboard_utils.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <time.h>
#include <math.h>
#include <assert.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>
#include <stdatomic.h>
#include <ctype.h>

// Convert piece type to character
char piece_type_to_char(FastType type, FastColor color) {
    char c = ' ';
    switch (type) {
        case FAST_PAWN: c = 'p'; break;
        case FAST_HORSE: c = 'h'; break;
        case FAST_ROOK: c = 'r'; break;
        case FAST_FLANGER: c = 'f'; break;
        case FAST_UNI: c = 'u'; break;
        case FAST_KING: c = 'k'; break;
        default: return ' ';
    }
    return color == FAST_WHITE ? toupper(c) : c;
}

// Format a move as string (e.g., "UG1-F3")
void format_move(FastMove move, char* buffer, size_t buffer_size) {
    FastBoardIndex from = move.from;
    FastBoardIndex to = move.to;
    FastPieceState from_piece = move.from_piece;

    // Convert indices to board coordinates
    int from_x = get_x(from);
    int from_y = get_y(from);
    int to_x = get_x(to);
    int to_y = get_y(to);

    // Get piece character
    char piece_char = piece_type_to_char(from_piece.type, from_piece.color);

    // Format as "PieceFromPos-ToPos" (e.g., "UG1-F3")
    snprintf(buffer, buffer_size, "%c%c%d-%c%d",
             piece_char,
             'A' + from_x,
             from_y + 1,
             'A' + to_x,
             to_y + 1);
}

// Constants
#define DEFAULT_TT_SIZE_MB 2048

static _Thread_local Evaluator tls_evaluation;
static _Thread_local FastMoveGenerator  tls_movegen;
static _Thread_local bool               tls_inited = false;
static _Thread_local int                lme_counter = 0;
static _Thread_local const atomic_bool* tls_stop_flag = NULL;
static _Thread_local uint32_t           tls_node_count = 0;
static _Thread_local bool               tls_aborted = false;

static inline int64_t now_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

// Forward declarations
static bool is_zugzwang_prone(FastBoard* board);
static int calculate_lmr_reduction(int depth, int move_index);
static bool should_not_reduce(FastMove move);
static BotResult fast_bot_find_best_move_lazy_smp(FastFlangBot* bot, FastBoard* board, bool print_time, long max_time_ms, int64_t start_ms);
static BotResult fast_bot_find_best_move_search_all(FastFlangBot* bot, FastBoard* board, bool print_time, long max_time_ms, int64_t start_ms);
static void print_pv_line(FastFlangBot* bot, FastBoard* board, int max_depth);

// Transposition table functions
// Transposition table functions moved to fast_transposition.c

// Bot implementation
void fast_bot_init(FastFlangBot* bot, int min_depth, int max_depth, int threads, int ttsize_mb, bool use_lme, int lme_max_extension, bool only_best_move, bool use_nnue) {
    bot->min_depth = min_depth;
    bot->max_depth = max_depth;
    bot->use_opening_database = false; // Not implemented in C version
    bot->total_evaluations = 0;
    bot->threads = threads;
    bot->use_lme = use_lme;
    bot->lme_max_extension = lme_max_extension;
    bot->only_best_move = only_best_move;
    bot->use_nnue = use_nnue;

    tt_init(&bot->tt, ttsize_mb);
    zobrist_init();
}

void fast_bot_destroy(FastFlangBot* bot) {
    tt_destroy(&bot->tt);
}

static inline void ensure_tls_search_state(FastBoard* board, bool use_nnue) {
    if (!tls_inited) {
        fast_move_generator_init(&tls_movegen, board, false, false, 1);
        evaluator_init(&tls_evaluation, board);
        tls_inited = true;
    }
    tls_movegen.board = board;
#ifdef USE_NNUE
    tls_evaluation.use_nnue = use_nnue;
#else
    (void)use_nnue;
#endif
}

// Check if a move should be extended by LME
static bool should_extend_move(FastFlangBot* bot, FastMove move, int depth) {
    if (!bot->use_lme || depth <= 1) {
        return false;
    }
    if (lme_counter >= bot->lme_max_extension) {
        return false;
    }
    
    FastPieceState from_piece = move.from_piece;
    if (from_piece.type == FAST_KING) {
        FastBoardIndex from_index = move.from;
        FastBoardIndex to_index = move.to;
        FastColor color = from_piece.color;
        
        int from_y = get_y(from_index);
        int to_y = get_y(to_index);
        int winning_y = (color == FAST_WHITE) ? 7 : 0;
        
        // Check if king moves forward (toward winning y)
        int forward_movement = (to_y - from_y) * get_evaluation_number(color);
        
        // Check distance to winning position
        int distance_to_win = abs(to_y - winning_y);
        
        return (forward_movement > 0) && (distance_to_win <= 3);
    }
    
    return false;
}

static int order_root_moves(FastFlangBot* bot, FastBoard* board, MoveBuffer* src, FastMove* out, int out_cap) {
    // Probe TT once at the root to get hash move
    uint32_t h = zobrist_hash(board);
    int16_t tt_val; FastMove tt_best = NULL_MOVE;
    (void)tt_probe(&bot->tt, h, bot->max_depth, -MATE_SCORE, MATE_SCORE, &tt_val, &tt_best);
    // Ensure thread-local heuristics are reset in this thread
    mo_clear();
    return mo_order_moves(src, tt_best, /*ply=*/0, out, out_cap);
}

// Lazy SMP thread context
typedef struct {
    FastFlangBot* bot;
    FastBoard root_board;
    int thread_id;
    int thread_count;
    int base_min_depth;
    int base_max_depth;
    long max_time_ms;
    int64_t start_time;
    atomic_bool* global_stop;
    bool print_time;

    // Shared results (protected by mutex)
    pthread_mutex_t* result_lock;
    FastMove* best_move;
    int16_t* best_eval;
    int* best_depth;
    FastColor side_to_move;

    // Per-thread result for final selection
    FastMove thread_best_move;
    int16_t thread_best_eval;
    int thread_best_depth;
} LazySMPThreadContext;

static void* lazy_smp_worker(void* arg) {
    LazySMPThreadContext* ctx = (LazySMPThreadContext*)arg;

    // Per-thread heuristics
    mo_clear();

    // Thread-specific depth offset for search diversity
    // Threads start at slightly different depths
    int depth_offset = ctx->thread_id % 6;
    int start_depth = ctx->base_min_depth + depth_offset;
    if (start_depth < 1 || start_depth > ctx->base_max_depth) start_depth = 1;

    FastMove local_best = NULL_MOVE;
    int16_t local_eval = (int16_t)(-MATE_SCORE * get_evaluation_number(ctx->side_to_move));
    int local_depth = 0;

    tls_stop_flag = ctx->global_stop;
    tls_node_count = 0;
    tls_aborted = false;

    // Each thread does its own iterative deepening
    for (int depth = start_depth; depth <= ctx->base_max_depth; depth++) {
        if (atomic_load_explicit(ctx->global_stop, memory_order_relaxed))
            break;

        // Generate and order root moves
        MoveBuffer move_buffer;
        move_buffer_init(&move_buffer);

        // Need thread-local move generator
        FastMoveGenerator thread_movegen;
        fast_move_generator_init(&thread_movegen, &ctx->root_board, false, false, 1);
        fast_move_generator_load_moves(&thread_movegen, ctx->root_board.at_move, &move_buffer);

        if (move_buffer.count == 0) break;

        // Probe TT for ordering hint
        uint32_t hash = zobrist_hash(&ctx->root_board);
        int16_t tt_val;
        FastMove tt_best = NULL_MOVE;
        (void)tt_probe(&ctx->bot->tt, hash, depth, -MATE_SCORE, MATE_SCORE, &tt_val, &tt_best);

        // Order moves - thread_id creates diversity in move ordering
        FastMove ordered[MAX_MOVES];
        int move_count = mo_order_moves(&move_buffer, tt_best, ctx->thread_id, ordered, MAX_MOVES);

        int16_t alpha = -MATE_SCORE;
        int16_t beta = MATE_SCORE;
        FastMove best_this_depth = NULL_MOVE;
        int16_t best_eval_this_depth = -MATE_SCORE;

        // Search all moves at this depth
        for (int i = 0; i < move_count; i++) {
            if (atomic_load_explicit(ctx->global_stop, memory_order_relaxed))
                break;

            FastMove move = ordered[i];
            FastBoard temp;
            fast_board_copy(&ctx->root_board, &temp);
            fast_board_execute_move(&temp, move);

            int16_t eval = -fast_bot_evaluate_move(ctx->bot, &temp, depth - 1,
                                                   -beta, -alpha, 1);

            if (atomic_load_explicit(ctx->global_stop, memory_order_relaxed))
                break;

            if (eval > best_eval_this_depth) {
                best_eval_this_depth = eval;
                best_this_depth = move;
            }

            if (eval > alpha) {
                alpha = eval;
            }
        }

        // Update thread-local best
        if (best_this_depth.from != NULL_MOVE.from) {
            local_best = best_this_depth;
            local_eval = (ctx->side_to_move == FAST_WHITE) ? best_eval_this_depth : -best_eval_this_depth;
            local_depth = depth;

            // Update shared best if this thread found something better
            pthread_mutex_lock(ctx->result_lock);
            bool better = (ctx->side_to_move == FAST_WHITE) ?
                         (local_eval > *ctx->best_eval) :
                         (local_eval < *ctx->best_eval);
            bool was_mate_eval = is_mate_score(*ctx->best_eval); // don't accidentally overwrite mate lines -> only if better was found
            if (better || (!was_mate_eval && *ctx->best_depth < depth)) {
                *ctx->best_eval = local_eval;
                *ctx->best_move = local_best;
                *ctx->best_depth = depth;
            }
            pthread_mutex_unlock(ctx->result_lock);

            // Store in TT
            uint32_t h = zobrist_hash(&ctx->root_board);
            tt_store(&ctx->bot->tt, h, best_eval_this_depth, depth, best_this_depth, TT_NODE_EXACT);
        }
    }

    // Store final thread results
    ctx->thread_best_move = local_best;
    ctx->thread_best_eval = local_eval;
    ctx->thread_best_depth = local_depth;

    return NULL;
}

BotResult fast_bot_find_best_move(FastFlangBot* bot, FastBoard* board, bool print_time) {
    return fast_bot_find_best_move_iterative(bot, board, print_time, LONG_MAX);
}

BotResult fast_bot_find_best_move_iterative(FastFlangBot* bot, FastBoard* board, bool print_time, long max_time_ms) {
    // Init engine components before starting the clock so tt_clear (memset) doesn't eat into the time budget
    fast_move_generator_init(&bot->move_generator, board, false, false, 1);
    evaluator_init(&bot->evaluator, board);

    bot->total_evaluations = 0;
    atomic_store(&bot->se_candidates, 0);
    atomic_store(&bot->se_probes,     0);
    atomic_store(&bot->se_extensions, 0);
    atomic_store(&bot->se_multi_cuts, 0);
    tt_clear(&bot->tt);

    int64_t start_ms = now_ms();

    // Choose search strategy based on mode
    if (bot->only_best_move) {
        // Only Best mode: Use Lazy SMP for maximum speed
        return fast_bot_find_best_move_lazy_smp(bot, board, print_time, max_time_ms, start_ms);
    } else {
        // Search All mode: Sequential search of each root move
        return fast_bot_find_best_move_search_all(bot, board, print_time, max_time_ms, start_ms);
    }
}

// Lazy SMP search - for finding best move quickly
static BotResult fast_bot_find_best_move_lazy_smp(FastFlangBot* bot, FastBoard* board, bool print_time, long max_time_ms, int64_t start_ms) {
    BotResult best_result = (BotResult){0};
    best_result.best_move.evaluation = -MATE_SCORE;

    // Set threads (configurable)
    int thread_count = bot->threads > 0 ? bot->threads : 1;
    if (thread_count < 1) thread_count = 1;
    if (thread_count > 64) thread_count = 64;

    // Shared results
    FastMove shared_best_move = NULL_MOVE;
    int16_t shared_best_eval = (int16_t)(-MATE_SCORE * get_evaluation_number(board->at_move));
    int shared_best_depth = 0;
    pthread_mutex_t result_lock;
    pthread_mutex_init(&result_lock, NULL);

    atomic_bool global_stop = ATOMIC_VAR_INIT(false);

    // Create Lazy SMP contexts for each thread
    LazySMPThreadContext* contexts = malloc(thread_count * sizeof(LazySMPThreadContext));
    pthread_t* threads = malloc(thread_count * sizeof(pthread_t));

    for (int t = 0; t < thread_count; t++) {
        contexts[t] = (LazySMPThreadContext){
            .bot = bot,
            .thread_id = t,
            .thread_count = thread_count,
            .base_min_depth = bot->min_depth,
            .base_max_depth = bot->max_depth,
            .max_time_ms = max_time_ms,
            .start_time = start_ms,
            .global_stop = &global_stop,
            .print_time = print_time,
            .result_lock = &result_lock,
            .best_move = &shared_best_move,
            .best_eval = &shared_best_eval,
            .best_depth = &shared_best_depth,
            .side_to_move = board->at_move,
            .thread_best_move = NULL_MOVE,
            .thread_best_eval = -MATE_SCORE,
            .thread_best_depth = 0
        };
        // Each thread gets its own board copy
        fast_board_copy(board, &contexts[t].root_board);
    }

    // Special case: single-threaded mode, call directly without thread overhead
    if (thread_count <= 1) {
        lazy_smp_worker(&contexts[0]);
    } else {
        // Launch all Lazy SMP worker threads
        for (int t = 0; t < thread_count; t++) {
            pthread_create(&threads[t], NULL, lazy_smp_worker, &contexts[t]);
        }

        // Monitor time and stop when limit reached
        int debug_counter = 0;
        while (!atomic_load_explicit(&global_stop, memory_order_relaxed)) {
            struct timespec ts = {.tv_sec=0, .tv_nsec=10000000}; // 10ms
            nanosleep(&ts, NULL);

            int64_t elapsed = now_ms() - start_ms;
            if (elapsed >= max_time_ms) {
                atomic_store_explicit(&global_stop, true, memory_order_relaxed);
                break;
            }

            // Check if any thread has found a good enough depth or mate score
            pthread_mutex_lock(&result_lock);
            bool good_depth = (shared_best_depth >= bot->max_depth);
            bool found_mate = is_mate_score(shared_best_eval);
            int current_depth = shared_best_depth;
            int16_t current_eval = shared_best_eval;
            pthread_mutex_unlock(&result_lock);

            // Debug output: show live stats
            if (print_time && elapsed > 0 && debug_counter % 10 == 0) {
                double tt_hit_rate = bot->tt.probes > 0 ?
                    (100.0 * bot->tt.hits / bot->tt.probes) : 0.0;
                double knps = bot->total_evaluations / (double)elapsed;

                printf("\r[SMP] D:%d E:%d N:%luM T:%lums NPS:%.0fK TT:%.1f%% PV: ",
                       current_depth, (int)current_eval,
                       bot->total_evaluations / 1000000,
                       elapsed, knps, tt_hit_rate);
                print_pv_line(bot, board, 5);  // Show first 5 moves
                printf("         ");
                fflush(stdout);
            }
            debug_counter++;

            if (good_depth || found_mate) {
                atomic_store_explicit(&global_stop, true, memory_order_relaxed);
                break;
            }
        }

        // Clear the debug line if it was printed
        if (print_time) {
            printf("\n");
        }

        // Join all threads
        for (int t = 0; t < thread_count; t++) {
            pthread_join(threads[t], NULL);
        }
    }

    // Select best result from all threads
    FastMove final_best_move = shared_best_move;
    int16_t final_best_eval = shared_best_eval;
    int final_best_depth = shared_best_depth;

    // Optional: could do voting here based on depth and eval
    // For now, just use the shared best which was updated by all threads

    pthread_mutex_destroy(&result_lock);

    // Only best mode - no move evaluations collected
    best_result.best_move.move = final_best_move;
    best_result.best_move.evaluation = final_best_eval;
    best_result.best_move.depth = final_best_depth;
    best_result.total_evaluations = bot->total_evaluations;
    best_result.all_evaluations = NULL;
    best_result.evaluation_count = 0;

    if (print_time) {
        long time_ms = now_ms() - start_ms;

        // Format best move
        char move_str[32];
        format_move(final_best_move, move_str, sizeof(move_str));

        printf("Lazy SMP: %d threads, Depth: %d, Time: %ldms\n",
               thread_count, final_best_depth, time_ms);
        printf("Best: %s (%d)\n", move_str, (int)final_best_eval);

        // Print principal variation
        printf("PV: ");
        print_pv_line(bot, board, 10);  // Max 10 moves in PV line
        printf("\n");

        printf("Evals: %ldK, EPms: %.0f\n",
               bot->total_evaluations / 1000,
               bot->total_evaluations / (time_ms > 0 ? (double)time_ms : 1.0));
        printf("TT: %lu/%lu hits (%.1f%%)\n",
               bot->tt.hits, bot->tt.probes,
               bot->tt.probes > 0 ? (100.0 * bot->tt.hits / bot->tt.probes) : 0.0);
    }

    free(contexts);
    free(threads);

    return best_result;
}

// Thread context for parallel root move search
typedef struct {
    FastFlangBot* bot;
    FastBoard board_after_move;
    FastMove move;
    int depth;
    int move_index;

    // Output
    int16_t eval;
} SearchAllThreadContext;

static void* search_all_worker(void* arg) {
    SearchAllThreadContext* ctx = (SearchAllThreadContext*)arg;

    // Per-thread heuristics
    mo_clear();

    // Search this move
    ctx->eval = -fast_bot_evaluate_move(ctx->bot, &ctx->board_after_move, ctx->depth - 1,
                                        -MATE_SCORE, MATE_SCORE, 1);

    return NULL;
}

// Search All mode - Parallel search of each root move for accurate evaluations
static BotResult fast_bot_find_best_move_search_all(FastFlangBot* bot, FastBoard* board, bool print_time, long max_time_ms, int64_t start_ms) {
    BotResult best_result = (BotResult){0};
    best_result.best_move.evaluation = -MATE_SCORE;

    // Get thread count
    int thread_count = bot->threads > 0 ? bot->threads : 1;
    if (thread_count < 1) thread_count = 1;
    if (thread_count > 64) thread_count = 64;

    // Allocate move evaluations array
    MoveEvaluation* move_evaluations = malloc(MAX_MOVES * sizeof(MoveEvaluation));
    int final_move_count = 0;

    // Prepare root move list
    MoveBuffer root_buf;
    move_buffer_init(&root_buf);

    for (int depth = bot->min_depth; depth <= bot->max_depth; depth++) {
        int64_t iter_start = now_ms();
        int64_t elapsed = now_ms() - start_ms;
        if (depth > bot->min_depth && elapsed * 3 >= max_time_ms) break;

        // Generate and order root moves
        move_buffer_clear(&root_buf);
        bot->move_generator.board = board;
        fast_move_generator_load_moves(&bot->move_generator, board->at_move, &root_buf);

        if (root_buf.count == 0) {
            if (print_time) printf("No moves available\n");
            break;
        }

        FastMove ordered[MAX_MOVES];
        int root_count = order_root_moves(bot, board, &root_buf, ordered, MAX_MOVES);

        // Allocate contexts and threads for parallel search
        SearchAllThreadContext* contexts = malloc(root_count * sizeof(SearchAllThreadContext));
        pthread_t* threads = malloc(root_count * sizeof(pthread_t));

        // Prepare contexts for each move
        for (int i = 0; i < root_count; i++) {
            contexts[i].bot = bot;
            contexts[i].move = ordered[i];
            contexts[i].depth = depth;
            contexts[i].move_index = i;

            // Create board after move
            fast_board_copy(board, &contexts[i].board_after_move);
            fast_board_execute_move(&contexts[i].board_after_move, ordered[i]);
        }

        // Launch threads or execute sequentially
        if (thread_count <= 1) {
            // Single-threaded: call directly
            for (int i = 0; i < root_count; i++) {
                search_all_worker(&contexts[i]);
            }
        } else {
            // Multi-threaded: process in batches
            for (int batch_start = 0; batch_start < root_count; batch_start += thread_count) {
                int batch_size = (batch_start + thread_count > root_count) ?
                                (root_count - batch_start) : thread_count;

                // Launch batch of threads
                for (int i = 0; i < batch_size; i++) {
                    pthread_create(&threads[i], NULL, search_all_worker, &contexts[batch_start + i]);
                }

                // Wait for batch to complete
                for (int i = 0; i < batch_size; i++) {
                    pthread_join(threads[i], NULL);
                }
            }
        }

        // Collect results and find best move
        FastMove best_move = NULL_MOVE;
        int16_t best_eval = (int16_t)(-MATE_SCORE * get_evaluation_number(board->at_move));

        for (int i = 0; i < root_count; i++) {
            int16_t eval = contexts[i].eval;

            // Convert to absolute eval (WHITE's perspective)
            int16_t absolute_eval = (board->at_move == FAST_WHITE) ? eval : (int16_t)-eval;

            // Store evaluation for this move
            move_evaluations[i].move = ordered[i];
            move_evaluations[i].evaluation = absolute_eval;
            move_evaluations[i].depth = depth;

            // Update best
            bool better = (board->at_move == FAST_WHITE) ?
                         (absolute_eval > best_eval) :
                         (absolute_eval < best_eval);
            if (better) {
                best_eval = absolute_eval;
                best_move = ordered[i];
            }
        }

        free(contexts);
        free(threads);

        // Update result for this depth
        best_result.best_move.move = best_move;
        best_result.best_move.evaluation = best_eval;
        best_result.best_move.depth = depth;
        best_result.total_evaluations = bot->total_evaluations;
        final_move_count = root_count;

        // Store root in TT
        uint32_t h = zobrist_hash(board);
        tt_store(&bot->tt, h, best_eval, depth, best_move, TT_NODE_EXACT);

        if (print_time) {
            long iter_ms = now_ms() - iter_start;
            printf("Iteration depth=%d done in %ldms (%d threads)\n", depth, iter_ms, thread_count);
        }
    }

    best_result.all_evaluations = move_evaluations;
    best_result.evaluation_count = final_move_count;

    if (print_time) {
        long time_ms = now_ms() - start_ms;
        printf("Evals: %ldK, Depth: %d, Time: %ldms, EPms: %.0f\n",
               bot->total_evaluations / 1000,
               best_result.best_move.depth,
               time_ms,
               bot->total_evaluations / (time_ms > 0 ? (double)time_ms : 1.0));
        printf("TT: %lu/%lu hits (%.1f%%)\n",
               bot->tt.hits, bot->tt.probes,
               bot->tt.probes > 0 ? (100.0 * bot->tt.hits / bot->tt.probes) : 0.0);
        long se_cand = atomic_load(&bot->se_candidates);
        long se_prob = atomic_load(&bot->se_probes);
        long se_ext  = atomic_load(&bot->se_extensions);
        long se_mc   = atomic_load(&bot->se_multi_cuts);
        printf("SE: candidates=%ld probes=%ld extensions=%ld multi-cuts=%ld\n",
               se_cand, se_prob, se_ext, se_mc);
    }

    return best_result;
}

int16_t fast_bot_evaluate_move_ex(FastFlangBot* bot, FastBoard* board, uint8_t depth, int16_t alpha, int16_t beta, int ply, FastMove exclude_move) {
    if (depth <= 0) {
        // At depth 0, switch to quiescence search instead of plain eval.
        return fast_bot_quiescence(bot, board, alpha, beta, QUIESCENCE_DEPTH);
    }

    if (tls_aborted) return alpha;
    if (tls_stop_flag != NULL && (++tls_node_count & 1023) == 0) {
        if (atomic_load_explicit(tls_stop_flag, memory_order_relaxed)) {
            tls_aborted = true;
            return alpha;
        }
    }

    int16_t alpha_orig = alpha;

    // Probe TT
    uint32_t hash = zobrist_hash(board);
    int16_t tt_value;
    FastMove tt_best_move = NULL_MOVE;
    bool is_singular_probe = exclude_move.from != NULL_MOVE.from;
    if (!is_singular_probe && tt_probe(&bot->tt, hash, depth, alpha, beta, &tt_value, &tt_best_move)) {
        return tt_value;
    } else if (is_singular_probe) {
        TTEntry* entry = tt_probe_entry(&bot->tt, hash);
        if (entry && entry->zobrist_hash == hash) tt_best_move = entry->best_move;
    }

    // Null Move Pruning
    // Conditions: depth >= 3, not zugzwang-prone, not at root (ply > 0)
    const int NULL_MOVE_R = 2;
    if (depth >= 3 && ply > 0 && !is_zugzwang_prone(board)) {
        // Make null move
        fast_board_make_null_move(board);

        // Search with reduced depth and null window
        int16_t null_score = -fast_bot_evaluate_move(bot, board, depth - 1 - NULL_MOVE_R, -beta, (int16_t)(-beta + NULL_WINDOW_SIZE), ply + 1);

        // Unmake null move
        fast_board_unmake_null_move(board);

        // If null move caused beta cutoff, prune this branch
        if (null_score >= beta) {
            // Don't store null move cutoffs in TT to avoid pollution
            return beta;
        }
    }

    // Generate moves
    MoveBuffer move_buffer;
    move_buffer_init(&move_buffer);
    ensure_tls_search_state(board, bot->use_nnue);
    fast_move_generator_load_moves(&tls_movegen, board->at_move, &move_buffer);

    if (move_buffer.count == 0) {
        // No legal moves → evaluate terminal
        return fast_bot_evaluate_move(bot, board, 0, alpha, beta, ply);
    }

    // Order moves
    FastMove ordered[MAX_MOVES];
    int mcount = mo_order_moves(&move_buffer, tt_best_move, ply, ordered, MAX_MOVES);

    // Singular extension setup: check if TT move is the only good move
    bool singular_extension_candidate = false;
    int16_t singular_beta = 0;
    if (depth >= 6 && ply > 0 && tt_best_move.from != NULL_MOVE.from) {
        TTEntry* entry = tt_probe_entry(&bot->tt, hash);
        if (entry && entry->zobrist_hash == hash
                && !is_mate_score(entry->value)
                && (entry->node_type == TT_NODE_LOWER_BOUND || entry->node_type == TT_NODE_EXACT)
                && entry->depth >= depth - 1) {
            singular_beta = entry->value - 9 * depth;
            singular_extension_candidate = true;
            atomic_fetch_add(&bot->se_candidates, 1);
        }
    }

    int16_t best_eval = -MATE_SCORE;
    FastMove best_move = NULL_MOVE;

    for (int i = 0; i < mcount; i++) {
        FastMove move = ordered[i];

        if (move_equals(move, exclude_move)) continue;

        // Singular extension: verify TT move is uniquely good
        bool singular = false;
        if (singular_extension_candidate && move_equals(move, tt_best_move)) {
            atomic_fetch_add(&bot->se_probes, 1);
            int16_t singular_result = fast_bot_evaluate_move_ex(
                bot, board, depth / 2,
                singular_beta - 1, singular_beta,
                ply, tt_best_move
            );

            singular = (singular_result < singular_beta);
            if (singular) atomic_fetch_add(&bot->se_extensions, 1);
        }

        // Make move on current board
        fast_board_execute_move(board, move);

        int16_t eval;
        uint8_t search_depth = depth - 1;

        // Check for Late Move Extensions (LME)
        bool extended = should_extend_move(bot, move, depth);
        if (extended) {
            lme_counter++;
            search_depth = depth;  // Extend by 1
        } else if (singular) {
            search_depth = depth;  // Singular extension
        }

        // Late Move Reductions (LMR)
        int reduction = 0;
        bool do_lmr = false;
        bool is_pv_node = (beta - alpha) > NULL_WINDOW_SIZE;
        if (!extended && !is_pv_node && i >= 2 && depth >= 3 && !should_not_reduce(move)) {
            reduction = calculate_lmr_reduction(depth, i);
            do_lmr = (reduction > 0);
        }

        // Principal Variation Search (PVS)
        if (i == 0) {
            // First move: full window, full depth
            eval = -fast_bot_evaluate_move(bot, board, search_depth, -beta, -alpha, ply + 1);
        } else {
            // Late moves: try with null window first
            uint8_t reduced_depth = search_depth;
            if (do_lmr) {
                reduced_depth = (search_depth > reduction) ? (search_depth - reduction) : 0;
            }

            eval = -fast_bot_evaluate_move(bot, board, reduced_depth,
                                           -alpha - NULL_WINDOW_SIZE, -alpha, ply + 1);

            // If LMR search fails high, re-search at full depth with null window
            if (do_lmr && eval > alpha) {
                eval = -fast_bot_evaluate_move(bot, board, search_depth,
                                               -alpha - NULL_WINDOW_SIZE, -alpha, ply + 1);
            }

            // If null window search beats alpha, do full re-search
            if (eval > alpha && eval < beta) {
                eval = -fast_bot_evaluate_move(bot, board, search_depth, -beta, -alpha, ply + 1);
            }
        }

        if (eval > EVAL_SCALE)        eval -= MATE_STEP_LOSS;
        else if (eval < -EVAL_SCALE)  eval += MATE_STEP_LOSS;

        if (extended) {
            lme_counter--;
        }

        // Unmake move
        fast_board_revert_move(board, move);

        // Negamax: always maximizing
        if (eval > best_eval) {
            best_eval = eval;
            best_move = move;
        }

        if (best_eval > alpha) {
            alpha = best_eval;
        }

        if (alpha >= beta) {
            mo_update_history(move, depth);
            mo_update_killer(move, ply);
            break;
        }
    }

    uint8_t node_type;
    if (best_eval <= alpha_orig) {
        node_type = TT_NODE_UPPER_BOUND;    // fail-low
    } else if (best_eval >= beta) {
        node_type = TT_NODE_LOWER_BOUND;    // fail-high
    } else {
        node_type = TT_NODE_EXACT;          // exact score
    }

    // Skip TT store if search was aborted mid-subtree (result is incomplete)
    if (!tls_aborted) {
        uint8_t store_depth = is_mate_score(best_eval) ? MATE_TT_DEPTH : depth;
        tt_store(&bot->tt, hash, best_eval, store_depth, best_move, node_type);
    }

    return best_eval;
}

int16_t fast_bot_quiescence(FastFlangBot* bot, FastBoard* board, int16_t alpha, int16_t beta, int depth) {
    // Static eval (stand pat)
    bot->total_evaluations++;
    ensure_tls_search_state(board, bot->use_nnue);

    int16_t eval = evaluator_evaluate(&tls_evaluation, board);

    // Limit depth
    if(depth <= 0) return eval;

    int16_t stand_pat = (board->at_move == FAST_WHITE) ? eval : (int16_t)-eval;

    // Fail-high?
    if (stand_pat >= beta) {
        return beta;
    }

    // Raise alpha if better
    if (stand_pat > alpha) {
        alpha = stand_pat;
    }

    // Generate only noisy moves (captures, promotions)
    MoveBuffer move_buffer;
    move_buffer_init(&move_buffer);
    ensure_tls_search_state(board, bot->use_nnue);
    fast_move_generator_load_moves(&tls_movegen, board->at_move, &move_buffer);

    int16_t best = stand_pat;

    for (int i = 0; i < move_buffer.count; i++) {
        FastMove move = move_buffer.moves[i];

        if (move.to_piece.type == FAST_NONE) {
            continue;
        }

        // Make move on current board
        fast_board_execute_move(board, move);

        int16_t score = -fast_bot_quiescence(bot, board, -beta, -alpha, depth - 1);

        // Unmake move
        fast_board_revert_move(board, move);

        if (score > EVAL_SCALE)        score -= MATE_STEP_LOSS;
        else if (score < -EVAL_SCALE)  score += MATE_STEP_LOSS;

        if (score > best) {
            best = score;
        }

        if (score > alpha) {
            alpha = score;
        }

        if (alpha >= beta) {
            // Fail-high
            return beta;
        }
    }

    return best;
}




// Helper function to detect zugzwang-prone positions (pawn endgames)
static bool is_zugzwang_prone(FastBoard* board) {
    // Count non-pawn material using bitboards
    int total_pieces = bb_count_bits(board->all_pieces);
    int pawn_count = bb_count_bits(board->white_pawns) + bb_count_bits(board->black_pawns);
    int non_pawn_pieces = total_pieces - pawn_count - 2;

    // Zugzwang is likely when only kings and pawns remain (or very few pieces)
    // 2 kings + very few other pieces = zugzwang prone
    return non_pawn_pieces <= 3;  // 3 other piece or less
}

// Calculate LMR reduction amount based on depth and move number
static int calculate_lmr_reduction(int depth, int move_index) {
    // Formula based on Fruit Reloaded approach
    // reduction = sqrt(depth - 1) + sqrt(move_index - 1)
    double reduction = sqrt((double)(depth - 1)) + sqrt((double)(move_index - 1));

    // Clamp reduction to reasonable bounds
    int r = (int)reduction;
    if (r > depth - 1) {
        r = depth - 1;
    }

    return r;
}

// Check if a move should NOT be reduced (tactical/important moves)
static bool should_not_reduce(FastMove move) {
    if (move.to_piece.type != FAST_NONE) {
        return true;
    }

    return false;
}

// Print principal variation line by recursively following TT best moves
static void print_pv_line(FastFlangBot* bot, FastBoard* board, int max_depth) {
    if (max_depth <= 0) return;

    // Probe TT for best move
    uint32_t hash = zobrist_hash(board);
    int16_t tt_val;
    FastMove tt_best = NULL_MOVE;

    if (!tt_probe(&bot->tt, hash, 0, -MATE_SCORE, MATE_SCORE, &tt_val, &tt_best)) {
        return;  // No TT entry
    }

    if (tt_best.from == NULL_MOVE.from) {
        return;  // No best move stored
    }

    // Print this move
    char move_str[32];
    format_move(tt_best, move_str, sizeof(move_str));
    printf("%s ", move_str);

    // Copy board and make move
    FastBoard next_board;
    fast_board_copy(board, &next_board);
    fast_board_execute_move(&next_board, tt_best);

    // Recurse
    print_pv_line(bot, &next_board, max_depth - 1);
}

// Helper functions
bool is_mate_score(int16_t value) {
    return value > EVAL_SCALE || value < -EVAL_SCALE;
}

FastColor get_color_at_move(const FastBoard* board) {
    return board->at_move;
}