#include "fast_evaluation.h"
#include <string.h>
#include <math.h>
#include <assert.h>
#include <stdio.h>

// Piece values array using the defined macros
static const int PIECE_VALUES[] = {
    0, // FAST_NONE
    PIECE_VALUE_PAWN, // FAST_PAWN
    PIECE_VALUE_HORSE, // FAST_HORSE
    PIECE_VALUE_ROOK, // FAST_ROOK
    PIECE_VALUE_FLANGER, // FAST_FLANGER
    PIECE_VALUE_UNI, // FAST_UNI
    PIECE_VALUE_KING  // FAST_KING
};

// Get piece value by type
static inline int get_piece_value(FastType type) {
    assert(type <= FAST_KING);
    return PIECE_VALUES[type];
}

void fast_evaluation_init(FastBoardEvaluation* eval, FastBoard* board) {
    eval->board = board;
    // Initialize own move generator with evaluation parameters (matching Kotlin)
    fast_move_generator_init(&eval->move_generator, eval->board, true, true, 2);

    // Initialize evaluation matrix
    memset(eval->evaluation_matrix, 0, sizeof(eval->evaluation_matrix));

    // Initialize statistics
    overall_stats_reset(&eval->white_stats);
    overall_stats_reset(&eval->black_stats);
}

int16_t fast_evaluation_evaluate(FastBoardEvaluation* eval) {
    if (fast_board_has_won(eval->board, FAST_WHITE)) return MATE_SCORE;
    if (fast_board_has_won(eval->board, FAST_BLACK)) return -MATE_SCORE;

    fast_evaluation_prepare(eval);

    double matrix_eval = 0.0;

    // Use bitboards to iterate only over squares with pieces
    Bitboard all_pieces = eval->board->all_pieces;
    while (all_pieces != 0) {
        int square = bb_pop_lsb(&all_pieces);
        fast_evaluation_evaluate_location(eval, square);
    }

    // 4) Move king-weighting BEFORE scoring or do a tiny second pass only over affected files
    double kings_eval_num = fast_evaluation_get_kings_eval(eval);

    // Now score fields once
    for (int i = 0; i < ARRAY_SIZE; ++i){
        matrix_eval += location_evaluation_evaluate_field(&eval->evaluation_matrix[i]);
    }

    const double wm = (double)eval->white_stats.movements;
    const double bm = (double)eval->black_stats.movements;
    const double wp = (double)eval->white_stats.piece_value;
    const double bp = (double)eval->black_stats.piece_value;

    // One division each using the same trick:
    const double mov_num = (wm - bm) * (wm + bm);
    const double mov_den = wm * bm;
    const double movement_eval = mov_num / mov_den;

    const double pv_num = (wp - bp) * (wp + bp);
    const double pv_den = wp * bp;
    double piece_value_eval = pv_num / pv_den;

    double evaluation = 0.0;
    evaluation += EVAL_WEIGHT_MATRIX  * matrix_eval;
    evaluation += EVAL_WEIGHT_MOVEMENT * movement_eval;
    evaluation += EVAL_WEIGHT_PIECE_VALUE * piece_value_eval;
    evaluation += EVAL_WEIGHT_KINGS_EVAL  * kings_eval_num;

    return cp_to_score(evaluation);
}


void fast_evaluation_prepare(FastBoardEvaluation* eval) {
    // Reset evaluation matrix - single memset is much faster than loop
    memset(eval->evaluation_matrix, 0, sizeof(eval->evaluation_matrix));

    // Reset statistics
    overall_stats_reset(&eval->white_stats);
    overall_stats_reset(&eval->black_stats);
}

void fast_evaluation_evaluate_location(FastBoardEvaluation* eval, FastBoardIndex index) {
    LocationEvaluation* loc_eval = &eval->evaluation_matrix[index];
    FastPieceState piece = fast_board_get_at(eval->board, index);
    FastType type = piece.type;
    
    if (type != FAST_NONE) {
        FastColor color = piece.color;
        OverallStats* stats = get_stats(eval, color);
        int piece_value = get_piece_value(type);

        stats->piece_value += piece_value;
        loc_eval->occupied_by = piece_value * get_evaluation_number(color);
        
        // Generate moves for this piece to calculate threats
        Bitboard targets = fast_move_generator_get_targets(&eval->move_generator, index, piece);
        while (targets != 0) {
            int target_index = bb_pop_lsb(&targets);
            location_evaluation_add_threat(&eval->evaluation_matrix[target_index], color, 1000000 / piece_value);
            stats->movements++;
        }
    }
}

double fast_evaluation_get_kings_eval(FastBoardEvaluation* eval) {
    const FastBoardIndex wk = fast_board_find_king(eval->board, FAST_WHITE);
    const FastBoardIndex bk = fast_board_find_king(eval->board, FAST_BLACK);
    if (wk == -1 || bk == -1) return 0.0;

    const int wx = get_x(wk), wy = get_y(wk);
    const int bx = get_x(bk), by = get_y(bk);

    const int white_eval = 1 << wy;
    const int black_eval = 1 << (7 - by);

    for (int y = wy; y < BOARD_SIZE - 1; ++y) {
        eval->evaluation_matrix[index_of(wx, y)].weight += white_eval;
    }
    for (int y = by; y >= 1; --y) {
        eval->evaluation_matrix[index_of(bx, y)].weight += black_eval;
    }

    // (a/b - b/a) -> single division variant
    const double white_eval_d = (double) white_eval;
    const double black_eval_d = (double) black_eval;
    const double num = (white_eval_d - black_eval_d) * (white_eval_d + black_eval_d);
    const double den = (white_eval_d * black_eval_d);
    return num / den;
}

// Location evaluation functions
void location_evaluation_add_threat(LocationEvaluation* loc_eval, FastColor color, int threat) {
    loc_eval->white_control += threat * color;
    loc_eval->black_control += threat * (1 - color);
}

static inline double eval_control_rate(double wc, double bc) {
    // Control values have base value added in caller
    const double w_plus_b  = wc + bc;
    const double w_minus_b = wc - bc;
    const double denom     = wc * bc;
    return (w_minus_b * w_plus_b) / denom;   // 1 division instead of 2
}

double location_evaluation_evaluate_field(const LocationEvaluation* loc_eval) {
    const double wc = (double)loc_eval->white_control + 10000.0;
    const double bc = (double)loc_eval->black_control + 10000.0;

    const double control_rate = eval_control_rate(wc, bc);
    const double w = (loc_eval->weight * EVAL_KINGS_WEIGHT_MULTIPLIER) + 1;

    const int occ = loc_eval->occupied_by;
    double result;

    if (occ > 0) {
        // White piece
        const int factor = (bc > wc) ? occ : 100;
        result = (1.0 + control_rate) * factor / 100.0;
    } else if (occ < 0) {
        // Black piece
        const int factor = (wc > bc) ? -occ : 100; // -occ == fabs(occ)
        result = (-1.0 + control_rate) * factor / 100.0;
    } else {
        // Empty square
        result = control_rate;
    }

    return result * w;
}

// Statistics functions
void overall_stats_reset(OverallStats* stats) {
    stats->movements = 1;    // Start with 1 to avoid division by zero
    stats->piece_value = 1;  // Start with 1 to avoid division by zero
}

OverallStats* get_stats(FastBoardEvaluation* eval, FastColor color) {
    return color ? &eval->white_stats : &eval->black_stats;
}