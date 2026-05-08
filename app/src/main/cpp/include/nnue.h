#ifndef NNUE_H
#define NNUE_H

#include "fast_board.h"
#include "nnue_types.h"
#include <stdint.h>

// Initialize NNUE (no-op for embedded weights, but keeping for future use)
void nnue_init(void);

// Evaluate position using NNUE (with quantized inference)
int16_t nnue_evaluate(const FastBoard* board, NNUEContext* context);

// SCReLU activation function: clamp(x, 0, 1)^2
// Float version (for reference/compatibility)
static inline float screlu(float x) {
    if (x <= 0.0f) return 0.0f;
    if (x >= 1.0f) return 1.0f;
    return x * x;
}

// Accumulator management for efficient updates
void nnue_refresh_accumulator(const FastBoard* board, NNUEContext* context);
void nnue_accumulator_add_piece(NNUEContext* context, FastType type, FastColor color, int square, bool frozen);
void nnue_accumulator_sub_piece(NNUEContext* context, FastType type, FastColor color, int square, bool frozen);

#endif // NNUE_H