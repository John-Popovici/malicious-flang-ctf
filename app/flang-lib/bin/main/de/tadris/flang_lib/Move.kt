package de.tadris.flang_lib

/**
 * Move Int 64bit = 8 bytes
 *
 * Bit 0: Resign flag (1 if resign, 0 if normal move)
 * Bits 1-7: Reserved for future flags
 *
 * Normal move (bit 0 = 0):
 * - Bits 8-15: from index (byte 1)
 * - Bits 16-23: to index (byte 2)
 * - Bits 24-31: from piece state (byte 3)
 * - Bits 32-39: to piece state (byte 4)
 * - Bits 40-47: previously frozen piece index (byte 5)
 * - Bits 48-63: unused (bytes 6-7)
 *
 * Resign move (bit 0 = 1):
 * - Bit 8: color that resigned (0 = FAST_BLACK, 1 = FAST_WHITE)
 * - Other bits: unused
 */
typealias Move = Long

// Flag bit masks
private const val RESIGN_FLAG_MASK: Long = 0x0000000000000001L      // bit 0
private const val RESIGN_COLOR_MASK: Long = 0x0000000000000100L     // bit 8

// Bit masks for normal moves (shifted by 8 bits)
private const val FROM_INDEX_MASK: Long = 0x000000000000FF00L        // bits 8-15
private const val TO_INDEX_MASK: Long = 0x0000000000FF0000L          // bits 16-23
private const val FROM_PIECE_STATE_MASK: Long = 0x00000000FF000000L  // bits 24-31
private const val TO_PIECE_STATE_MASK: Long = 0x000000FF00000000L    // bits 32-39
private const val PREV_FROZEN_INDEX_MASK: Long = 0x0000FF0000000000L // bits 40-47

// Bit shifts for each field
private const val FROM_INDEX_SHIFT = 8
private const val TO_INDEX_SHIFT = 16
private const val FROM_PIECE_STATE_SHIFT = 24
private const val TO_PIECE_STATE_SHIFT = 32
private const val PREV_FROZEN_INDEX_SHIFT = 40
private const val RESIGN_COLOR_SHIFT = 8

// Resign move constants
const val RESIGN_WHITE: Move = RESIGN_FLAG_MASK or RESIGN_COLOR_MASK   // bit 0 = 1, bit 8 = 1
const val RESIGN_BLACK: Move = RESIGN_FLAG_MASK                        // bit 0 = 1, bit 8 = 0

fun packMove(board: Board, fromIndex: BoardIndex, toIndex: BoardIndex): Move {
    val fromPiece = board.getAt(fromIndex)
    if(fromPiece.isEmpty()) throw IllegalArgumentException("Cannot move piece at ${fromIndex.getIndexNotation()} on board $board")
    return packMove(
        fromIndex,
        toIndex,
        fromPiece,
        board.getAt(toIndex),
        board.getFrozenPieceIndex(fromPiece.getColor())
    )
}

/**
 * Creates a Move from individual components
 * @param fromIndex source position index (0-255)
 * @param toIndex destination position index (0-255)
 * @param fromPieceState state of piece at source position
 * @param toPieceState state of piece at destination position
 * @param previouslyFrozenPieceIndex index of previously frozen piece (0-254) or -1 if no piece was frozen
 * @return Move Long with encoded values
 */
fun packMove(
    fromIndex: BoardIndex,
    toIndex: BoardIndex,
    fromPieceState: PieceState,
    toPieceState: PieceState,
    previouslyFrozenPieceIndex: BoardIndex
): Move {
    var move = 0L

    // Bit 0 is 0 for normal moves
    // Bits 1-7 reserved for future flags

    // Set from index (bits 8-15)
    move = move or ((fromIndex.toLong() and 0xFF) shl FROM_INDEX_SHIFT)

    // Set to index (bits 16-23)
    move = move or ((toIndex.toLong() and 0xFF) shl TO_INDEX_SHIFT)

    // Set from piece state (bits 24-31)
    move = move or ((fromPieceState.toLong() and 0xFF) shl FROM_PIECE_STATE_SHIFT)

    // Set to piece state (bits 32-39)
    move = move or ((toPieceState.toLong() and 0xFF) shl TO_PIECE_STATE_SHIFT)

    // Set previously frozen piece index (bits 40-47)
    move = move or ((previouslyFrozenPieceIndex.toLong() and 0xFF) shl PREV_FROZEN_INDEX_SHIFT)

    return move
}

/**
 * Extracts the from index from Move
 * @return source position index (0-255)
 */
fun Move.getFromIndex(): BoardIndex {
    return ((this and FROM_INDEX_MASK) shr FROM_INDEX_SHIFT).toInt()
}

/**
 * Extracts the to index from Move
 * @return destination position index (0-255)
 */
fun Move.getToIndex(): BoardIndex {
    return ((this and TO_INDEX_MASK) shr TO_INDEX_SHIFT).toInt()
}

/**
 * Extracts the from piece state from Move
 * @return state of piece at source position
 */
fun Move.getFromPieceState(): PieceState {
    return ((this and FROM_PIECE_STATE_MASK) shr FROM_PIECE_STATE_SHIFT).toByte()
}

/**
 * Extracts the to piece state from Move
 * @return state of piece at destination position
 */
fun Move.getToPieceState(): PieceState {
    return ((this and TO_PIECE_STATE_MASK) shr TO_PIECE_STATE_SHIFT).toByte()
}

/**
 * Extracts the previously frozen piece index from Move
 * @return index of previously frozen piece (0-254) or -1 if no piece was frozen
 */
fun Move.getPreviouslyFrozenPieceIndex(): BoardIndex {
    val value = ((this and PREV_FROZEN_INDEX_MASK) shr PREV_FROZEN_INDEX_SHIFT).toInt()
    return if (value == 255) -1 else value
}

/**
 * Checks if this Move is a resign move
 * @return true if bit 0 is set (resign flag), false otherwise
 */
fun Move.isResign(): Boolean {
    return (this and RESIGN_FLAG_MASK) != 0L
}

/**
 * Extracts the color that resigned from a resign move
 * @return FAST_WHITE if bit 8 is set, FAST_BLACK if bit 8 is clear
 * @throws IllegalStateException if called on a non-resign move
 */
fun Move.getResignColor(): Color {
    require(isResign()) { "getResignColor() can only be called on resign moves" }
    return (this and RESIGN_COLOR_MASK) != 0L  // bit 8: 1 = FAST_WHITE, 0 = FAST_BLACK
}

/**
 * Creates a resign move for the specified color
 * @param color the color that is resigning (FAST_WHITE or FAST_BLACK)
 * @return Move with resign flag set and color encoded
 */
fun packResignMove(color: Color): Move {
    return if (color) RESIGN_WHITE else RESIGN_BLACK
}

/**
 * Creates a string representation of the Move for debugging
 * @return formatted string showing all components
 */
fun Move.toDebugString(): String {
    return if (isResign()) {
        val color = if (getResignColor()) "WHITE" else "BLACK"
        "Move(RESIGN by $color)"
    } else {
        "Move(fromIndex=${getFromIndex()}, toIndex=${getToIndex()}, " +
                "fromPieceState=0x${getFromPieceState().toString(16).padStart(2, '0')}, " +
                "toPieceState=0x${getToPieceState().toString(16).padStart(2, '0')}, " +
                "previouslyFrozenPieceIndex=${getPreviouslyFrozenPieceIndex()})"
    }
}