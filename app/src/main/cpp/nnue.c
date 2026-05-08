#include "nnue.h"
#include "nnue_model.h"
#include "fast_evaluation.h"
#include <string.h>
#include <math.h>
#include <stdio.h>

#ifdef USE_NNUE

void nnue_init(void) {
    // No-op: side-to-move plane removed with perspective encoding
}

// Quantized SCReLU activation: clamp(x, 0, QA)^2 / QA
static inline int32_t screlu_quantized(int32_t x, int32_t scale_in) {
    int32_t scaled = (x * NNUE_QA) / scale_in;
    if (scaled <= 0) return 0;
    if (scaled >= NNUE_QA) scaled = NNUE_QA;
    return (scaled * scaled) / NNUE_QA;
}

// Returns feature index for a piece on a square from a given perspective.
// Plane ordering matches FastType values (FAST_PAWN=1..FAST_KING=6), so plane = type-1.
// For black's perspective the square is mirrored vertically (sq ^ 56).
static int piece_feature_idx(FastType type, FastColor color, int square, FastColor perspective) {
    int plane = (int)type - 1 + ((color == perspective) ? 0 : 6);
    int sq = (perspective == FAST_BLACK) ? (square ^ 56) : square;
    return plane * 64 + sq;
}

// Returns feature index for a frozen piece from a given perspective.
static int frozen_feature_idx(FastColor piece_color, int square, FastColor perspective) {
    bool is_own = (piece_color == perspective);
    int plane = is_own ? NNUE_PLANE_OWN_FROZEN : NNUE_PLANE_OPP_FROZEN;
    int sq = (perspective == FAST_BLACK) ? (square ^ 56) : square;
    return plane * 64 + sq;
}

// Add layer_0_weights[idx] to accumulator[persp].
static inline void accum_add(NNUEContext* ctx, int persp, int idx) {
    const int8_t* w = layer_0_weights[idx];
    int32_t* acc = ctx->accumulator[persp];
    for (int i = 0; i < NNUE_LAYER1_SIZE; i++)
        acc[i] += w[i];
}

// Subtract layer_0_weights[idx] from accumulator[persp].
static inline void accum_sub(NNUEContext* ctx, int persp, int idx) {
    const int8_t* w = layer_0_weights[idx];
    int32_t* acc = ctx->accumulator[persp];
    for (int i = 0; i < NNUE_LAYER1_SIZE; i++)
        acc[i] -= w[i];
}

void nnue_refresh_accumulator(const FastBoard* board, NNUEContext* ctx) {
    // Initialize both accumulators with biases
    for (int i = 0; i < NNUE_LAYER1_SIZE; i++) {
        ctx->accumulator[0][i] = layer_0_biases[i];
        ctx->accumulator[1][i] = layer_0_biases[i];
    }

    for (int square = 0; square < ARRAY_SIZE; square++) {
        FastPieceState piece = board->pieces[square];
        if (piece.type == FAST_NONE) continue;

        accum_add(ctx, 0, piece_feature_idx(piece.type, piece.color, square, FAST_WHITE));
        accum_add(ctx, 1, piece_feature_idx(piece.type, piece.color, square, FAST_BLACK));

        if (piece.frozen == FAST_FROZEN) {
            int w_frozen = frozen_feature_idx(piece.color, square, FAST_WHITE);
            int b_frozen = frozen_feature_idx(piece.color, square, FAST_BLACK);
            accum_add(ctx, 0, w_frozen);
            accum_add(ctx, 1, b_frozen);
        }
    }

    ctx->accumulator_valid = true;
}

void nnue_accumulator_add_piece(NNUEContext* ctx, FastType type, FastColor color, int square, bool frozen) {
    accum_add(ctx, 0, piece_feature_idx(type, color, square, FAST_WHITE));
    accum_add(ctx, 1, piece_feature_idx(type, color, square, FAST_BLACK));
    if (frozen) {
        accum_add(ctx, 0, frozen_feature_idx(color, square, FAST_WHITE));
        accum_add(ctx, 1, frozen_feature_idx(color, square, FAST_BLACK));
    }
}

void nnue_accumulator_sub_piece(NNUEContext* ctx, FastType type, FastColor color, int square, bool frozen) {
    accum_sub(ctx, 0, piece_feature_idx(type, color, square, FAST_WHITE));
    accum_sub(ctx, 1, piece_feature_idx(type, color, square, FAST_BLACK));
    if (frozen) {
        accum_sub(ctx, 0, frozen_feature_idx(color, square, FAST_WHITE));
        accum_sub(ctx, 1, frozen_feature_idx(color, square, FAST_BLACK));
    }
}

int16_t nnue_evaluate(const FastBoard* board, NNUEContext* context) {
    if (fast_board_has_won(board, FAST_WHITE)) return MATE_SCORE;
    if (fast_board_has_won(board, FAST_BLACK)) return -MATE_SCORE;

    FastBoard* mutable_board = (FastBoard*)board;
    NNUEContext* active_context = &mutable_board->nnue_context;

    if (!active_context->accumulator_valid) {
        nnue_refresh_accumulator(board, active_context);
    }

    // Select accumulator for the side to move (0=white, 1=black)
    int persp = (board->at_move == FAST_WHITE) ? 0 : 1;
    const int32_t* accum = active_context->accumulator[persp];

    // Apply SCReLU to get layer 1 output
    int32_t layer1_activated[NNUE_LAYER1_SIZE];
    for (int i = 0; i < NNUE_LAYER1_SIZE; i++) {
        layer1_activated[i] = screlu_quantized(accum[i], NNUE_SCALE);
    }

    // Layer 2: 128 -> 32 (with SCReLU)
    for (int i = 0; i < NNUE_LAYER2_SIZE; i++) {
        int32_t sum = layer_1_biases[i];
        const int8_t* weight_row = &layer_1_weights[i][0];
        for (int j = 0; j < NNUE_LAYER1_SIZE; j++) {
            sum += weight_row[j] * layer1_activated[j];
        }
        active_context->layer2_output[i] = (int16_t)screlu_quantized(sum, NNUE_QA * NNUE_QB);
    }

    // Output layer: 32 -> 1 (no activation)
    int32_t output_sum = layer_2_biases[0];
    const int16_t* weight_row = &layer_2_weights[0][0];
    for (int j = 0; j < NNUE_LAYER2_SIZE; j++) {
        output_sum += weight_row[j] * active_context->layer2_output[j];
    }

    // Network output is in side-to-move perspective; convert to white perspective
    double winrate = (double)output_sum / (NNUE_QA * NNUE_QB);
    if (board->at_move == FAST_BLACK) winrate = -winrate;

    if (winrate >= 1.0) return EVAL_SCALE;
    if (winrate <= -1.0) return -EVAL_SCALE;

    return (int16_t)(winrate * EVAL_SCALE);
}

#endif
