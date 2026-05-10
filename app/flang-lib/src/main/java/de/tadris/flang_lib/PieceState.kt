package de.tadris.flang_lib

/**
 * Byte 8 bit
 * - 3 bit type
 * - 1 bit color
 * - 1 bit frozen
 */
typealias PieceState = Byte

// Bit masks for each field
private const val TYPE_MASK: Byte = 0b11100000.toByte()    // bits 5-7
private const val COLOR_MASK: Byte = 0b00010000.toByte()   // bit 4
private const val FROZEN_MASK: Byte = 0b00001000.toByte()  // bit 3

// Bit shifts for each field
private const val TYPE_SHIFT = 5
private const val COLOR_SHIFT = 4
private const val FROZEN_SHIFT = 3

/**
 * Creates a PieceState from individual components
 * @param type 3-bit type value (0-7)
 * @param color true for one color, false for another
 * @param frozen true if piece is frozen, false otherwise
 * @return PieceState byte with encoded values
 */
fun packPieceState(type: FastType, color: Color, frozen: FrozenState): PieceState {
    require(type in 0..7) { "Type must be between 0 and 7 (3 bits)" }

    var state: Byte = 0

    // Set type (3 bits, shifted to positions 5-7)
    state = (state.toInt() or ((type.toInt() and 0b111) shl TYPE_SHIFT)).toByte()

    // Set color (1 bit, shifted to position 4)
    if (color) {
        state = (state.toInt() or (1 shl COLOR_SHIFT)).toByte()
    }

    // Set frozen (1 bit, shifted to position 3)
    if (frozen) {
        state = (state.toInt() or (1 shl FROZEN_SHIFT)).toByte()
    }

    return state
}

/**
 * Extracts the type from PieceState
 * @return 3-bit type value (0-7)
 */
fun PieceState.getType(): FastType {
    return (((this.toInt() and 0xFF) and (TYPE_MASK.toInt() and 0xFF)) ushr TYPE_SHIFT).toByte()
}

/**
 * Extracts the color from PieceState
 * @return true if color bit is set, false otherwise
 */
fun PieceState.getColor(): Color {
    return ((this.toInt() and 0xFF) and (COLOR_MASK.toInt() and 0xFF)) != 0
}

/**
 * Extracts the frozen state from PieceState
 * @return true if frozen bit is set, false otherwise
 */
fun PieceState.getFrozen(): FrozenState {
    return ((this.toInt() and 0xFF) and (FROZEN_MASK.toInt() and 0xFF)) != 0
}

inline fun PieceState.isEmpty() = this == 0.toByte()