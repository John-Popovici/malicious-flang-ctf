#ifndef FAST_BOARD_H
#define FAST_BOARD_H

#include <stdint.h>
#include <stdbool.h>
#include "bitboard_utils.h"

#ifdef USE_NNUE
#include "nnue_types.h"
#endif

// Constants matching the Kotlin implementation
#define BOARD_SIZE 8
#define ARRAY_SIZE 64  // 8x8 with extra data per square
#define MAX_MOVES 200

// Fast type definitions
typedef uint8_t FastType;
typedef bool FastColor;
typedef bool FastFrozenState;
typedef struct {
    FastType type : 3;
    FastColor color : 1;
    FastFrozenState frozen : 1;
} FastPieceState;
typedef int8_t FastBoardIndex;
// Move structure
typedef struct {
    FastBoardIndex from;
    FastBoardIndex to;
    FastPieceState from_piece;
    FastPieceState to_piece;
    FastBoardIndex frozen_index;
} FastMove;

// Null move constant
#define NULL_MOVE ((FastMove){.from = -1, .to = -1, .from_piece = {0}, .to_piece = {0}, .frozen_index = -1})

// Piece types
#define FAST_NONE 0
#define FAST_PAWN 1
#define FAST_HORSE 2
#define FAST_ROOK 3
#define FAST_FLANGER 4
#define FAST_UNI 5
#define FAST_KING 6

// Colors
#define FAST_WHITE true
#define FAST_BLACK false

// Frozen states
#define FAST_NORMAL false
#define FAST_FROZEN true

// Helper macros for PieceState creation
#define MAKE_PIECE_STATE(t, c, f) ((FastPieceState){.type = (t), .color = (c), .frozen = (f)})

// Board structure - hybrid with both arrays and bitboards
typedef struct {
    // Original array representation (maintained for compatibility)
    FastPieceState pieces[ARRAY_SIZE];
    
    // Bitboard representation - 6 piece types per color
    Bitboard white_pawns;
    Bitboard white_horses;
    Bitboard white_rooks;
    Bitboard white_flangers;
    Bitboard white_unis;
    Bitboard white_king;
    
    Bitboard black_pawns;
    Bitboard black_horses;
    Bitboard black_rooks;
    Bitboard black_flangers;
    Bitboard black_unis;
    Bitboard black_king;
    
    // Occupancy bitboards for fast lookup
    Bitboard white_pieces;
    Bitboard black_pieces;
    Bitboard all_pieces;
    
    // Existing metadata
    FastColor at_move;
    FastBoardIndex frozen_white_index;
    FastBoardIndex frozen_black_index;
    int move_number;
    uint32_t cached_hash;

#ifdef USE_NNUE
    // NNUE evaluation context embedded directly (copied with board)
    NNUEContext nnue_context;
#endif
} FastBoard;

// Move buffer for efficient move generation
typedef struct {
    FastMove moves[MAX_MOVES];
    int count;
    int capacity;
} MoveBuffer;

// Board functions
void fast_board_init(FastBoard* board);
void fast_board_copy(const FastBoard* src, FastBoard* dst);
void fast_board_execute_move(FastBoard* board, FastMove move);
void fast_board_revert_move(FastBoard* board, FastMove move);
void fast_board_make_null_move(FastBoard* board);
void fast_board_unmake_null_move(FastBoard* board);
bool fast_board_game_complete(const FastBoard* board);
bool fast_board_has_won(const FastBoard* board, FastColor color);
FastBoardIndex fast_board_find_king(const FastBoard* board, FastColor color);
void fast_board_unfreeze(FastBoard* board, FastColor color);
FastBoardIndex fast_board_get_frozen_piece_index(const FastBoard* board, FastColor color);
void fast_board_set_frozen_piece_index(FastBoard* board, FastColor color, FastBoardIndex index);
void fast_board_update_at(FastBoard* board, FastBoardIndex index, FastType type, FastColor color);
void fast_board_rebuild_hash_cache(FastBoard* board);

// Piece state helper functions (for compatibility)
static inline bool piece_is_empty(FastPieceState state) { return state.type == FAST_NONE; }
static inline bool piece_state_equals(FastPieceState a, FastPieceState b) { 
    return a.type == b.type && a.color == b.color && a.frozen == b.frozen; 
}

// Board access functions
FastPieceState fast_board_get_at(const FastBoard* board, FastBoardIndex index);
FastPieceState fast_board_get_at_xy(const FastBoard* board, int x, int y);
void fast_board_set_at(FastBoard* board, FastBoardIndex index, FastPieceState state);
void fast_board_clear_at(FastBoard* board, FastBoardIndex index);

// Utility functions
FastBoardIndex index_of(int x, int y);
int get_x(FastBoardIndex index);
int get_y(FastBoardIndex index);
FastColor get_opponent(FastColor color);
int get_evaluation_number(FastColor color);
int get_winning_y(FastColor color);

// Move creation and utilities
FastMove pack_move(FastBoardIndex from, FastBoardIndex to, FastPieceState from_piece, FastPieceState to_piece, FastBoardIndex frozen_index);
bool move_equals(FastMove a, FastMove b);

// Move buffer functions
void move_buffer_init(MoveBuffer* buffer);
void move_buffer_clear(MoveBuffer* buffer);
void move_buffer_add(MoveBuffer* buffer, FastMove move);
FastMove move_buffer_get(const MoveBuffer* buffer, int index);

// Bitboard helper functions
Bitboard* fast_board_get_piece_bitboard(FastBoard* board, FastType type, FastColor color);
const Bitboard* fast_board_get_piece_bitboard_const(const FastBoard* board, FastType type, FastColor color);
void fast_board_clear_bitboards(FastBoard* board);

#endif // FAST_BOARD_H
