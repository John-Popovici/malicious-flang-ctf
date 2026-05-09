#include "fast_move_generator.h"
#include "bitboard_utils.h"
#include <assert.h>
#include <string.h>

// Generate sliding moves in a specific direction using bitboards
// For normal sliding pieces (rook, bishop), pass the same function for both shift_func_1 and shift_func_2
// For flanger zigzag, pass two different functions that will be alternated
// Returns a bitboard of all valid target squares
static Bitboard generate_sliding_direction_bb(FastMoveGenerator* gen, int square, FastColor color,
                                              Bitboard (*shift_func_1)(Bitboard), Bitboard (*shift_func_2)(Bitboard)) {
    Bitboard occupied = gen->board->all_pieces;
    Bitboard own_pieces = color == FAST_WHITE ? gen->board->white_pieces : gen->board->black_pieces;
    
    Bitboard piece_bb = bb_single_bit(square);
    Bitboard attacks = EMPTY_BITBOARD;
    bool use_first = true;
    
    // Slide in the direction until we hit a piece or edge
    while (true) {
        // Alternate between the two shift functions
        piece_bb = use_first ? shift_func_1(piece_bb) : shift_func_2(piece_bb);
        use_first = !use_first;
        
        if (piece_bb == EMPTY_BITBOARD) break; // Hit edge of board
        
        attacks |= piece_bb;
        
        if (piece_bb & occupied) break; // Hit a piece, stop sliding
    }
    
    // Remove our own pieces from attacks (unless include_own_pieces is set)
    if (!gen->include_own_pieces) {
        attacks &= ~own_pieces;
    }
    
    return attacks;
}

// Move vectors for different piece types
typedef struct {
    int x, y;
} Vector;

void fast_move_generator_init(FastMoveGenerator* gen, FastBoard* board, bool include_own_pieces, bool ignore_freeze, int king_range) {
    gen->board = board;
    gen->include_own_pieces = include_own_pieces;
    gen->ignore_freeze = ignore_freeze;
    gen->king_range = king_range;
}

void fast_move_generator_load_moves(FastMoveGenerator* gen, FastColor color, MoveBuffer* buffer) {
    move_buffer_clear(buffer);
    
    if (fast_board_game_complete(gen->board)) return;
    
    // Use bitboard to iterate only through pieces of the specific color
    Bitboard color_pieces = color == FAST_WHITE ? gen->board->white_pieces : gen->board->black_pieces;
    while (color_pieces != 0) {
        int index = bb_pop_lsb(&color_pieces);
        FastPieceState piece = fast_board_get_at(gen->board, index);
        generate_moves_for_piece(gen, index, piece, buffer);
    }
}

void generate_moves_for_piece(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece, MoveBuffer* buffer) {
    Bitboard targets = fast_move_generator_get_targets(gen, from_index, piece);
    
    while (targets != 0) {
        int target_index = bb_pop_lsb(&targets);
        FastMove move = pack_move(
                from_index,
                target_index,
                piece,
                fast_board_get_at(gen->board, target_index),
                fast_board_get_frozen_piece_index(gen->board, piece.color)
        );
        move_buffer_add(buffer, move);
    }
}

Bitboard fast_move_generator_get_targets(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece) {
    if (!gen->ignore_freeze && piece.frozen) return EMPTY_BITBOARD;
    
    FastType type = piece.type;
    
    switch (type) {
        case FAST_PAWN:
            return generate_pawn_moves(gen, from_index, piece);
        case FAST_KING:
            return generate_king_moves(gen, from_index, piece);
        case FAST_HORSE:
            return generate_horse_moves(gen, from_index, piece);
        case FAST_ROOK:
            return generate_rook_moves(gen, from_index, piece);
        case FAST_FLANGER:
            return generate_flanger_moves(gen, from_index, piece);
        case FAST_UNI:
            return generate_uni_moves(gen, from_index, piece);
        default:
            return EMPTY_BITBOARD;
    }
}

Bitboard generate_pawn_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece) {
    FastColor color = piece.color;

    Bitboard pawn_attacks = color == FAST_WHITE ? WHITE_PAWN_ATTACKS[from_index] : BLACK_PAWN_ATTACKS[from_index];

    // filter illegal moves
    return check_targets(gen, pawn_attacks, piece.color);
}

Bitboard generate_king_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece) {
    FastColor color = piece.color;
    
    // Branchless lookup using array of pointers (with bounds check)
    assert(gen->king_range >= 1 && gen->king_range <= 3 && "Unsupported king range");
    Bitboard king_attacks = KING_ATTACKS_BY_RANGE[gen->king_range][from_index];
    
    // Filter illegal moves (same as pawn implementation)
    return check_targets(gen, king_attacks, color);
}

Bitboard generate_horse_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece) {
    FastColor color = piece.color;
    
    Bitboard horse_attacks = HORSE_ATTACKS[from_index];
    
    // Filter illegal moves
    return check_targets(gen, horse_attacks, color);
}

Bitboard generate_rook_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece) {
    FastColor color = piece.color;
    
    // Generate moves in all 4 rook directions and combine bitboards
    Bitboard all_attacks = EMPTY_BITBOARD;
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_north, bb_shift_north);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_east, bb_shift_east);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_south, bb_shift_south);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_west, bb_shift_west);
    
    return all_attacks;
}

Bitboard generate_flanger_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece) {
    FastColor color = piece.color;
    
    // Generate moves for each of the 8 flanger zigzag patterns and combine bitboards to eliminate duplicates
    Bitboard all_attacks = EMPTY_BITBOARD;
    
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_northwest, bb_shift_southwest);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_northwest, bb_shift_northeast);

    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_southwest, bb_shift_northwest);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_southwest, bb_shift_southeast);

    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_northeast, bb_shift_southeast);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_northeast, bb_shift_northwest);

    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_southeast, bb_shift_northeast);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_southeast, bb_shift_southwest);

    return all_attacks;
}

Bitboard generate_bishop_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece) {
    FastColor color = piece.color;
    
    // Generate moves in all 4 diagonal directions and combine bitboards
    Bitboard all_attacks = EMPTY_BITBOARD;
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_northeast, bb_shift_northeast);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_southeast, bb_shift_southeast);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_southwest, bb_shift_southwest);
    all_attacks |= generate_sliding_direction_bb(gen, from_index, color, bb_shift_northwest, bb_shift_northwest);
    
    return all_attacks;
}

Bitboard generate_uni_moves(FastMoveGenerator* gen, FastBoardIndex from_index, FastPieceState piece) {
    // UNI combines rook, horse, and bishop (diagonal) moves = Queen + Horse
    Bitboard all_attacks = EMPTY_BITBOARD;
    all_attacks |= generate_rook_moves(gen, from_index, piece);
    all_attacks |= generate_horse_moves(gen, from_index, piece);
    all_attacks |= generate_bishop_moves(gen, from_index, piece);  // Simple diagonal moves, not flanger zigzag
    
    return all_attacks;
}

Bitboard check_targets(FastMoveGenerator* gen, Bitboard targets, FastColor color) {
    const Bitboard own_pieces = (color == FAST_WHITE)
                                ? gen->board->white_pieces
                                : gen->board->black_pieces;

    if (gen->include_own_pieces) {
        return targets;
    } else {
        return targets & ~own_pieces;
    }
}

bool check_target(FastMoveGenerator* gen, int x, int y, FastColor color) {
    return is_valid_position(x, y) && 
           (gen->include_own_pieces || 
            is_empty_position(gen, x, y) || 
            (fast_board_get_at_xy(gen->board, x, y).color != color));
}

inline bool is_empty_position(FastMoveGenerator* gen, int x, int y) {
    return fast_board_get_at_xy(gen->board, x, y).type == FAST_NONE;
}

inline bool is_valid_position(int x, int y) {
    return x >= 0 && y >= 0 && x < BOARD_SIZE && y < BOARD_SIZE;
}