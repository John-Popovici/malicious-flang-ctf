#include "nnue.h"
#include "fast_board.h"
#include <stdio.h>
#include <stdlib.h>

// Helper function to parse FBN2 position
void parse_fbn2_to_board(const char* fbn2, FastBoard* board) {
    fast_board_init(board);

    // Parse side to move
    if (fbn2[0] == '+') {
        board->at_move = FAST_WHITE;
    } else if (fbn2[0] == '-') {
        board->at_move = FAST_BLACK;
    }

    int position = 0;
    int i = 1;
    char digit_buffer[4] = {0};
    int digit_len = 0;

    while (fbn2[i] != '\0' && position < ARRAY_SIZE) {
        char c = fbn2[i];

        // Handle numbers (empty squares)
        if (c >= '0' && c <= '9') {
            digit_buffer[digit_len++] = c;
            i++;
            continue;
        }

        // Process accumulated digits
        if (digit_len > 0) {
            digit_buffer[digit_len] = '\0';
            int empty_count = atoi(digit_buffer);
            position += empty_count;
            digit_len = 0;
            digit_buffer[0] = '\0';
        }

        // Handle piece characters
        FastType type = FAST_NONE;
        FastColor color = FAST_WHITE;

        if (c == 'P' || c == 'p') type = FAST_PAWN;
        else if (c == 'R' || c == 'r') type = FAST_ROOK;
        else if (c == 'H' || c == 'h') type = FAST_HORSE;
        else if (c == 'F' || c == 'f') type = FAST_FLANGER;
        else if (c == 'U' || c == 'u') type = FAST_UNI;
        else if (c == 'K' || c == 'k') type = FAST_KING;

        if (type != FAST_NONE) {
            // Determine color (lowercase = black)
            color = (c >= 'a' && c <= 'z') ? FAST_BLACK : FAST_WHITE;

            // Check if next character is '-' (frozen)
            bool frozen = false;
            if (fbn2[i + 1] == '-') {
                frozen = true;
                i += 2;
            } else {
                i++;
            }

            // Set piece on board
            board->pieces[position] = MAKE_PIECE_STATE(type, color, frozen ? FAST_FROZEN : FAST_NORMAL);
            position++;
        } else {
            i++;
        }
    }
}

int main() {
    printf("=== NNUE Integration Test ===\n\n");

    // Test position: starting position with one frozen black pawn
    const char* test_fbn2 = "+2PRHFUK2PPPPPP32ppp-ppp2kufhrp2";
    printf("Test position: %s\n\n", test_fbn2);

    // Parse FBN2 to board
    FastBoard board;
    parse_fbn2_to_board(test_fbn2, &board);

    // Create NNUE context
    NNUEContext* context = (NNUEContext*)malloc(sizeof(NNUEContext));
    if (!context) {
        fprintf(stderr, "Failed to allocate NNUE context\n");
        return 1;
    }

    // Initialize NNUE
    nnue_init();

    // Evaluate position
    printf("Evaluating position...\n");
    int16_t eval = nnue_evaluate(&board, context);
    printf("Evaluation: %d\n\n", (int)eval);

    // Test feature extraction
    printf("Feature extraction:\n");
    float features[NNUE_INPUT_SIZE];
    nnue_extract_features(&board, features);

    // Count non-zero features
    int non_zero = 0;
    for (int i = 0; i < NNUE_INPUT_SIZE; i++) {
        if (features[i] != 0.0f) {
            non_zero++;
        }
    }
    printf("  Non-zero features: %d / %d\n", non_zero, NNUE_INPUT_SIZE);

    free(context);

    printf("\n=== Test Complete ===\n");
    printf("\nTo compare with Python:\n");
    printf("  cd ../../../nnue-training\n");
    printf("  python evaluate_position.py --position \"%s\"\n", test_fbn2);

    return 0;
}