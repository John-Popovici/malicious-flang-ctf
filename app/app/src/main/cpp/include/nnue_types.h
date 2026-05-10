#ifndef NNUE_TYPES_H
#define NNUE_TYPES_H

#include <stdbool.h>
#include <stdint.h>

// NNUE Architecture (perspective encoding)
// Input: 896 features (14 planes × 8 × 8), encoded from side-to-move perspective
// Hidden: 128 -> 32
// Output: 1 (evaluation score)

#define NNUE_INPUT_SIZE 896
#define NNUE_LAYER1_SIZE 128
#define NNUE_LAYER2_SIZE 32
#define NNUE_OUTPUT_SIZE 1

// Feature planes (perspective-relative: own = side to move, opp = opponent)
// For black's perspective, squares are mirrored vertically (sq ^ 56)
// Ordering matches FastType values (FAST_PAWN=1..FAST_KING=6), so plane = type-1
#define NNUE_PLANE_OWN_PAWN 0
#define NNUE_PLANE_OWN_HORSE 1
#define NNUE_PLANE_OWN_ROOK 2
#define NNUE_PLANE_OWN_FLANGER 3
#define NNUE_PLANE_OWN_UNI 4
#define NNUE_PLANE_OWN_KING 5
#define NNUE_PLANE_OPP_PAWN 6
#define NNUE_PLANE_OPP_HORSE 7
#define NNUE_PLANE_OPP_ROOK 8
#define NNUE_PLANE_OPP_FLANGER 9
#define NNUE_PLANE_OPP_UNI 10
#define NNUE_PLANE_OPP_KING 11
#define NNUE_PLANE_OWN_FROZEN 12
#define NNUE_PLANE_OPP_FROZEN 13

// NNUE evaluation context with dual accumulator for efficient updates.
// accumulator[0] = white's perspective, accumulator[1] = black's perspective.
// Both are kept in sync; evaluation uses the side-to-move's accumulator.
typedef struct NNUEContext {
    int32_t accumulator[2][NNUE_LAYER1_SIZE];  // [perspective][neuron] - quantized
    int16_t layer2_output[NNUE_LAYER2_SIZE];    // working memory during evaluation
    bool accumulator_valid;
} NNUEContext;

#endif // NNUE_TYPES_H
