#include "fast_board.h"
#include "fast_zobrist.h"
#include <string.h>
#include <stdio.h>
#include <assert.h>

#ifdef USE_NNUE
#include "nnue.h"
#endif

// Bitboard helper functions
Bitboard* fast_board_get_piece_bitboard(FastBoard* board, FastType type, FastColor color) {
    if (color == FAST_WHITE) {
        switch (type) {
            case FAST_PAWN: return &board->white_pawns;
            case FAST_HORSE: return &board->white_horses;
            case FAST_ROOK: return &board->white_rooks;
            case FAST_FLANGER: return &board->white_flangers;
            case FAST_UNI: return &board->white_unis;
            case FAST_KING: return &board->white_king;
            default: return NULL;
        }
    } else {
        switch (type) {
            case FAST_PAWN: return &board->black_pawns;
            case FAST_HORSE: return &board->black_horses;
            case FAST_ROOK: return &board->black_rooks;
            case FAST_FLANGER: return &board->black_flangers;
            case FAST_UNI: return &board->black_unis;
            case FAST_KING: return &board->black_king;
            default: return NULL;
        }
    }
}

const Bitboard* fast_board_get_piece_bitboard_const(const FastBoard* board, FastType type, FastColor color) {
    if (color == FAST_WHITE) {
        switch (type) {
            case FAST_PAWN: return &board->white_pawns;
            case FAST_HORSE: return &board->white_horses;
            case FAST_ROOK: return &board->white_rooks;
            case FAST_FLANGER: return &board->white_flangers;
            case FAST_UNI: return &board->white_unis;
            case FAST_KING: return &board->white_king;
            default: return NULL;
        }
    } else {
        switch (type) {
            case FAST_PAWN: return &board->black_pawns;
            case FAST_HORSE: return &board->black_horses;
            case FAST_ROOK: return &board->black_rooks;
            case FAST_FLANGER: return &board->black_flangers;
            case FAST_UNI: return &board->black_unis;
            case FAST_KING: return &board->black_king;
            default: return NULL;
        }
    }
}

void fast_board_clear_bitboards(FastBoard* board) {
    board->white_pawns = EMPTY_BITBOARD;
    board->white_horses = EMPTY_BITBOARD;
    board->white_rooks = EMPTY_BITBOARD;
    board->white_flangers = EMPTY_BITBOARD;
    board->white_unis = EMPTY_BITBOARD;
    board->white_king = EMPTY_BITBOARD;
    
    board->black_pawns = EMPTY_BITBOARD;
    board->black_horses = EMPTY_BITBOARD;
    board->black_rooks = EMPTY_BITBOARD;
    board->black_flangers = EMPTY_BITBOARD;
    board->black_unis = EMPTY_BITBOARD;
    board->black_king = EMPTY_BITBOARD;
    
    board->white_pieces = EMPTY_BITBOARD;
    board->black_pieces = EMPTY_BITBOARD;
    board->all_pieces = EMPTY_BITBOARD;
}

void fast_board_init(FastBoard* board) {
    memset(board->pieces, 0, ARRAY_SIZE * sizeof(FastPieceState));
    fast_board_clear_bitboards(board);
    board->at_move = FAST_WHITE;
    board->frozen_white_index = -1;
    board->frozen_black_index = -1;
    board->move_number = 0;
    board->cached_hash = 0;
#ifdef USE_NNUE
    board->nnue_context.accumulator_valid = false;
#endif
}

void fast_board_copy(const FastBoard* src, FastBoard* dst) {
    memcpy(dst, src, sizeof(FastBoard));
    // NNUE context is embedded in the struct, so it gets copied automatically
}

void fast_board_execute_move(FastBoard* board, FastMove move) {
    FastBoardIndex from_index = move.from;
    FastBoardIndex to_index = move.to;
    FastPieceState piece = move.from_piece;

    // Unfreeze current player's frozen piece
    fast_board_unfreeze(board, piece.color);

    // Clear from position and set to position
    fast_board_clear_at(board, from_index);
    fast_board_update_at(board, to_index, piece.type, piece.color);

    // Switch turns and increment move number
    board->at_move = get_opponent(board->at_move);
    board->cached_hash ^= zobrist_get_side_to_move_key();

    // Update hash for move number change and new side to move
    // Remove old move number hash
    board->cached_hash ^= zobrist_get_move_number_key(board->move_number);
    // Increment move number
    board->move_number++;
    // Add new move number hash
    board->cached_hash ^= zobrist_get_move_number_key(board->move_number);
}

void fast_board_revert_move(FastBoard* board, FastMove move) {
    // Restore pieces (to first then from -> important for king indices)
    fast_board_set_at(board, move.to, move.to_piece);
    fast_board_set_at(board, move.from, move.from_piece);


    // Restore frozen piece if there was one
    FastBoardIndex frozen_index = move.frozen_index;
    fast_board_set_frozen_piece_index(board, move.from_piece.color, frozen_index);
    if (frozen_index != -1) {
        FastPieceState frozen_piece = fast_board_get_at(board, frozen_index);
        FastPieceState new_frozen = MAKE_PIECE_STATE(frozen_piece.type, frozen_piece.color, FAST_FROZEN);
        fast_board_set_at(board, frozen_index, new_frozen);
    }

    // Switch turns back and decrement move number
    board->at_move = get_opponent(board->at_move);
    board->cached_hash ^= zobrist_get_side_to_move_key();

    // Update hash for move number change
    // Remove current move number hash
    board->cached_hash ^= zobrist_get_move_number_key(board->move_number);
    // Decrement move number
    board->move_number--;
    // Add old move number hash back
    board->cached_hash ^= zobrist_get_move_number_key(board->move_number);
}

void fast_board_make_null_move(FastBoard* board) {
    // Switch side to move
    board->at_move = get_opponent(board->at_move);
    board->cached_hash ^= zobrist_get_side_to_move_key();

    // Update hash for move number change
    board->cached_hash ^= zobrist_get_move_number_key(board->move_number);
    board->move_number++;
    board->cached_hash ^= zobrist_get_move_number_key(board->move_number);
}

void fast_board_unmake_null_move(FastBoard* board) {
    // Switch side to move back
    board->at_move = get_opponent(board->at_move);
    board->cached_hash ^= zobrist_get_side_to_move_key();

    // Update hash for move number change
    board->cached_hash ^= zobrist_get_move_number_key(board->move_number);
    board->move_number--;
    board->cached_hash ^= zobrist_get_move_number_key(board->move_number);
}

void fast_board_unfreeze(FastBoard* board, FastColor color) {
    FastBoardIndex index = fast_board_get_frozen_piece_index(board, color);
    if (index == -1) return;
    
    FastPieceState current_piece = fast_board_get_at(board, index);
    FastPieceState new_piece = MAKE_PIECE_STATE(current_piece.type, current_piece.color, FAST_NORMAL);
    fast_board_set_at(board, index, new_piece);
}

FastBoardIndex fast_board_get_frozen_piece_index(const FastBoard* board, FastColor color) {
    return color ? board->frozen_white_index : board->frozen_black_index;
}

void fast_board_set_frozen_piece_index(FastBoard* board, FastColor color, FastBoardIndex index) {
    // Update hash cache for frozen piece changes
    FastBoardIndex* saved_index = color ? &board->frozen_white_index : &board->frozen_black_index;

    // Remove old frozen piece hash
    if (*saved_index != -1) {
        board->cached_hash ^= zobrist_get_frozen_key(color, *saved_index);
    }

    // Add new frozen piece hash
    if (index != -1) {
        board->cached_hash ^= zobrist_get_frozen_key(color, index);
    }

    *saved_index = index;
}

bool fast_board_game_complete(const FastBoard* board) {
    return fast_board_has_won(board, FAST_WHITE) || fast_board_has_won(board, FAST_BLACK);
}

bool fast_board_has_won(const FastBoard* board, FastColor color) {
    FastBoardIndex king_index = fast_board_find_king(board, color);
    if (king_index == -1) return false; // No king found
    
    FastBoardIndex opponent_king = fast_board_find_king(board, get_opponent(color));
    if (opponent_king == -1) return true; // Opponent has no king
    
    return get_y(king_index) == get_winning_y(color);
}

FastBoardIndex fast_board_find_king(const FastBoard* board, FastColor color) {
    // Use cached king positions for O(1) lookup
    Bitboard bb = color == FAST_WHITE ? board->white_king : board->black_king;
    return bb_lsb_index(bb);
}

// Board access functions
FastPieceState fast_board_get_at(const FastBoard* board, FastBoardIndex index) {
    assert(index >= 0);
    return board->pieces[index];
}

FastPieceState fast_board_get_at_xy(const FastBoard* board, int x, int y) {
    return fast_board_get_at(board, index_of(x, y));
}

void fast_board_set_at(FastBoard* board, FastBoardIndex index, FastPieceState state) {
    assert(index >= 0);

    // Get old piece for king cache and hash maintenance
    FastPieceState old_state = board->pieces[index];

    // Remove old piece from hash
    FastType old_type = old_state.type;
    FastColor old_color = old_state.color;
    if (old_type != FAST_NONE) {
        board->cached_hash ^= zobrist_get_piece_key(old_type, index, old_color);
    }

    // Add new piece to hash
    FastType new_type = state.type;
    if (new_type != FAST_NONE) {
        FastColor new_color = state.color;
        board->cached_hash ^= zobrist_get_piece_key(new_type, index, new_color);
    }

    // Update bitboards - remove old piece
    if (old_state.type != FAST_NONE) {
        Bitboard* old_piece_bb = fast_board_get_piece_bitboard(board, old_state.type, old_state.color);
        if (old_piece_bb) {
            *old_piece_bb = bb_clear_bit(*old_piece_bb, index);
        }
        Bitboard* pieces = old_color == FAST_WHITE ? &board->white_pieces : &board->black_pieces;
        *pieces = bb_clear_bit(*pieces, index);
    }

    // Update bitboards - add new piece
    if (state.type != FAST_NONE) {
        Bitboard* piece_bb = fast_board_get_piece_bitboard(board, state.type, state.color);
        if (piece_bb) {
            *piece_bb = bb_set_bit(*piece_bb, index);
        }

        Bitboard* pieces = state.color == FAST_WHITE ? &board->white_pieces : &board->black_pieces;
        *pieces = bb_set_bit(*pieces, index);
    }

    // Update all pieces bitboard
    board->all_pieces = board->white_pieces | board->black_pieces;

#ifdef USE_NNUE
    if (board->nnue_context.accumulator_valid) {
        if (old_state.type != FAST_NONE)
            nnue_accumulator_sub_piece(&board->nnue_context,
                old_state.type, old_state.color, index, old_state.frozen == FAST_FROZEN);
        if (state.type != FAST_NONE)
            nnue_accumulator_add_piece(&board->nnue_context,
                state.type, state.color, index, state.frozen == FAST_FROZEN);
    }
#endif

    board->pieces[index] = state;

    // Update frozen indices if needed
    if (index == board->frozen_white_index) {
        bool still_frozen = state.color == FAST_WHITE && state.frozen;
        if (!still_frozen) fast_board_set_frozen_piece_index(board, FAST_WHITE, -1);
    } else if (index == board->frozen_black_index) {
        bool still_frozen = state.color == FAST_BLACK && state.frozen;
        if (!still_frozen) fast_board_set_frozen_piece_index(board, FAST_BLACK, -1);
    }

    // If piece is frozen, update frozen index
    if (state.frozen) {
        fast_board_set_frozen_piece_index(board, state.color, index);
    }
}

void fast_board_clear_at(FastBoard* board, FastBoardIndex index) {
    fast_board_set_at(board, index, MAKE_PIECE_STATE(FAST_NONE, FAST_WHITE, FAST_NORMAL));
}

void fast_board_update_at(FastBoard* board, FastBoardIndex index, FastType type, FastColor color) {
    if (type != FAST_NONE) {
        // Handle pawn promotion
        FastType written_type = type;
        if (type == FAST_PAWN && get_y(index) == get_winning_y(color)) {
            written_type = FAST_UNI;
        }
        
        // Determine if piece should be frozen (all pieces except king can be frozen)
        FastFrozenState has_freeze = (written_type != FAST_NONE && written_type != FAST_KING) ? FAST_FROZEN : FAST_NORMAL;
        fast_board_set_at(board, index, MAKE_PIECE_STATE(written_type, color, has_freeze));
    } else {
        fast_board_clear_at(board, index);
    }
}

void fast_board_rebuild_hash_cache(FastBoard* board) {
    board->cached_hash = zobrist_compute_hash_from_scratch(board);
}

// Utility functions
FastBoardIndex index_of(int x, int y) {
    assert(x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE);
    return y * BOARD_SIZE + x;
}

int get_x(FastBoardIndex index) {
    assert(index >= 0);
    return index % BOARD_SIZE;
}

int get_y(FastBoardIndex index) {
    assert(index >= 0);
    return index / BOARD_SIZE;
}

FastColor get_opponent(FastColor color) {
    return !color;
}

int get_evaluation_number(FastColor color) {
    return color ? 1 : -1;
}

int get_winning_y(FastColor color) {
    return color ? 7 : 0;
}

// Move creation and utilities
FastMove pack_move(FastBoardIndex from, FastBoardIndex to, FastPieceState from_piece, FastPieceState to_piece, FastBoardIndex frozen_index) {
    FastMove move = {
        .from = from,
        .to = to,
        .from_piece = from_piece,
        .to_piece = to_piece,
        .frozen_index = frozen_index
    };
    return move;
}


bool move_equals(FastMove a, FastMove b) {
    return a.from == b.from && 
           a.to == b.to && 
           piece_state_equals(a.from_piece, b.from_piece) && 
           piece_state_equals(a.to_piece, b.to_piece) && 
           a.frozen_index == b.frozen_index;
}

// Move buffer functions
void move_buffer_init(MoveBuffer* buffer) {
    buffer->count = 0;
    buffer->capacity = MAX_MOVES;
}

void move_buffer_clear(MoveBuffer* buffer) {
    buffer->count = 0;
}

void move_buffer_add(MoveBuffer* buffer, FastMove move) {
    assert(buffer->count < buffer->capacity);
    buffer->moves[buffer->count++] = move;
}

FastMove move_buffer_get(const MoveBuffer* buffer, int index) {
    assert(index >= 0 && index < buffer->count);
    return buffer->moves[index];
}