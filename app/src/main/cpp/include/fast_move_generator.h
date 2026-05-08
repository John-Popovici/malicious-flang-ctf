#ifndef FAST_MOVE_GENERATOR_H
#define FAST_MOVE_GENERATOR_H

#include "fast_board.h"

// Move generator structure
typedef struct {
    FastBoard* board;
    bool include_own_pieces;
    bool ignore_freeze;
    int king_range;
} FastMoveGenerator;

// Move generator functions
void fast_move_generator_init(FastMoveGenerator* gen, FastBoard* board, bool include_own_pieces, bool ignore_freeze, int king_range);
void fast_move_generator_load_moves(FastMoveGenerator* gen, FastColor color, MoveBuffer* buffer);
Bitboard fast_move_generator_get_targets(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece);

// Specialized move generation functions
Bitboard generate_pawn_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece);
Bitboard generate_king_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece);

// Context for move generation
struct MoveGenerationContext {
    FastMoveGenerator* gen;
    FastBoardIndex from_index;
    FastPieceState piece;
    MoveBuffer* buffer;
};

// Individual piece move generation functions
void generate_moves_for_piece(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece, MoveBuffer* buffer);
Bitboard generate_horse_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece);
Bitboard generate_rook_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece);
Bitboard generate_flanger_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece);
Bitboard generate_bishop_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece);
Bitboard generate_uni_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece);

// Helper functions
Bitboard check_targets(FastMoveGenerator* gen, Bitboard targets, FastColor color);
bool check_target(FastMoveGenerator* gen, int x, int y, FastColor color);
bool is_valid_position(int x, int y);
bool is_empty_position(FastMoveGenerator* gen, int x, int y);

#endif // FAST_MOVE_GENERATOR_H