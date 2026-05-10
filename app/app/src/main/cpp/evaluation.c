#include "evaluation.h"
#include <stdio.h>

#ifdef USE_NNUE
// NNUE evaluation implementation

void evaluator_init(Evaluator* eval, FastBoard* board) {
    nnue_init();
    board->nnue_context.accumulator_valid = false;
    eval->nnue_context.accumulator_valid = false;
    fast_evaluation_init(&eval->hce_evaluator, board);
    eval->use_nnue = true;
}

int16_t evaluator_evaluate(Evaluator* eval, FastBoard* board) {
    if (eval->use_nnue) {
        return nnue_evaluate(board, &eval->nnue_context);
    }
    eval->hce_evaluator.board = board;
    eval->hce_evaluator.move_generator.board = board;
    return fast_evaluation_evaluate(&eval->hce_evaluator);
}

const char* evaluator_get_name(void) {
    return "NNUE";
}

#else
// HCE (Hand-Crafted Evaluation) implementation

void evaluator_init(Evaluator* eval, FastBoard* board) {
    fast_evaluation_init(&eval->hce_evaluator, board);
}

int16_t evaluator_evaluate(Evaluator* eval, FastBoard* board) {
    // Update board pointer in case it changed
    eval->hce_evaluator.board = board;
    eval->hce_evaluator.move_generator.board = board;

    return fast_evaluation_evaluate(&eval->hce_evaluator);
}

const char* evaluator_get_name(void) {
    return "HCE";
}

#endif