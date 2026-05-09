#ifndef EVALUATION_H
#define EVALUATION_H

#include "fast_board.h"
#include "fast_evaluation.h"

// Compile-time configuration for evaluation method
// Define USE_NNUE during compilation to use NNUE, otherwise uses HCE
// Example: gcc -DUSE_NNUE ...

#ifdef USE_NNUE
#include "nnue.h"
#endif

// Unified evaluation structure that can use either NNUE or HCE
typedef struct {
#ifdef USE_NNUE
    NNUEContext nnue_context;
    FastBoardEvaluation hce_evaluator;
    bool use_nnue;
#else
    FastBoardEvaluation hce_evaluator;
#endif
} Evaluator;

// Initialize evaluator (call once per thread)
void evaluator_init(Evaluator* eval, FastBoard* board);

// Evaluate a position
// Returns evaluation score from white's perspective
int16_t evaluator_evaluate(Evaluator* eval, FastBoard* board);

// Get evaluation method name (for debugging)
const char* evaluator_get_name(void);

#endif // EVALUATION_H