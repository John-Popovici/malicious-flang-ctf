#ifndef FAST_EVALUATION_H
#define FAST_EVALUATION_H

#include "fast_board.h"
#include "fast_move_generator.h"
#include "bitboard_utils.h"
#include <math.h>

// Evaluation constants
#define EVAL_SCALE   30000
#define MATE_SCORE   32000
#define MATE_THRESHOLD 30500
#define MATE_STEP_LOSS 1

// Piece values for evaluation
#define PIECE_VALUE_PAWN 110
#define PIECE_VALUE_HORSE 200
#define PIECE_VALUE_ROOK 400
#define PIECE_VALUE_FLANGER 400
#define PIECE_VALUE_UNI 900
#define PIECE_VALUE_KING 400

// Evaluation hyper parameters
#define EVAL_WEIGHT_MATRIX 2.0
#define EVAL_WEIGHT_MOVEMENT 10.0
#define EVAL_WEIGHT_PIECE_VALUE 140.0
#define EVAL_WEIGHT_KINGS_EVAL 2.0
#define EVAL_KINGS_WEIGHT_MULTIPLIER 0.6

// Location evaluation structure
typedef struct {
    int16_t occupied_by;
    uint16_t white_control;
    uint16_t black_control;
    uint16_t weight;
} LocationEvaluation;

// Overall statistics for each color
typedef struct {
    int movements;
    int piece_value;
} OverallStats;

// Fast board evaluation structure
typedef struct {
    FastBoard* board;
    FastMoveGenerator move_generator;  // Own move generator for evaluation
    LocationEvaluation evaluation_matrix[ARRAY_SIZE];
    OverallStats white_stats;
    OverallStats black_stats;
} FastBoardEvaluation;

// Score conversion helpers
// CP → internal int16_t score (same formula as NNUE winrate scaling)
static inline int16_t cp_to_score(double cp) {
    double t = cp / (200.0 + 0.47 * fabs(cp));
    double winrate = tanh(t);
    if (winrate >= 1.0)  return  EVAL_SCALE;
    if (winrate <= -1.0) return -EVAL_SCALE;
    return (int16_t)(winrate * EVAL_SCALE);
}

// Internal int16_t score → CP (inverse of cp_to_score, mate scores map to ±(10000 - steps))
static inline double score_to_cp(int16_t score) {
    if (score > EVAL_SCALE) {
        int steps = MATE_SCORE - score;
        return 10000.0 - steps;
    }
    if (score < -EVAL_SCALE) {
        int steps = MATE_SCORE + score;
        return -(10000.0 - steps);
    }
    // Clamp to safe range before atanh: formula singularity at winrate=tanh(1/0.47)≈0.972
    double winrate = (double)score / EVAL_SCALE;
    if (winrate >=  0.96) return  4999.0;
    if (winrate <= -0.96) return -4999.0;
    double t = atanh(winrate);
    return t * 200.0 / (1.0 - fabs(t) * 0.47);
}

// Evaluation functions
void fast_evaluation_init(FastBoardEvaluation* eval, FastBoard* board);
int16_t fast_evaluation_evaluate(FastBoardEvaluation* eval);
void fast_evaluation_prepare(FastBoardEvaluation* eval);
void fast_evaluation_evaluate_location(FastBoardEvaluation* eval, FastBoardIndex index);
double fast_evaluation_get_kings_eval(FastBoardEvaluation* eval);

// Location evaluation functions
void location_evaluation_add_threat(LocationEvaluation* loc_eval, FastColor color, int threat);
double location_evaluation_evaluate_field(const LocationEvaluation* loc_eval);

// Statistics functions
void overall_stats_reset(OverallStats* stats);
OverallStats* get_stats(FastBoardEvaluation* eval, FastColor color);

#endif // FAST_EVALUATION_H
