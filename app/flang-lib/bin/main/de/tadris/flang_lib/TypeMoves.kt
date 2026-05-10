package de.tadris.flang_lib

internal val MOVES_PAWN = arrayOf(
    intArrayOf(-1, 1),
    intArrayOf(0, 1),
    intArrayOf(1, 1),
)

internal val MOVES_HORSE = arrayOf(
    intArrayOf(-1, 2),
    intArrayOf(-1, -2),
    intArrayOf(-2, 1),
    intArrayOf(-2, -1),
    intArrayOf(1, 2),
    intArrayOf(1, -2),
    intArrayOf(2, -1),
    intArrayOf(2, 1),
)

internal val MOVES_ROOK = arrayOf(
    intArrayOf(
        0, 1,
        0, 2,
        0, 3,
        0, 4,
        0, 5,
        0, 6,
        0, 7,
    ),
    intArrayOf(
        0, -1,
        0, -2,
        0, -3,
        0, -4,
        0, -5,
        0, -6,
        0, -7,
    ),
    intArrayOf(
        1, 0,
        2, 0,
        3, 0,
        4, 0,
        5, 0,
        6, 0,
        7, 0,
    ),
    intArrayOf(
        -1, 0,
        -2, 0,
        -3, 0,
        -4, 0,
        -5, 0,
        -6, 0,
        -7, 0,
    ),
)

internal val MOVES_UNI = arrayOf(
    intArrayOf(
        0, 1,
        0, 2,
        0, 3,
        0, 4,
        0, 5,
        0, 6,
        0, 7,
    ),
    intArrayOf(
        0, -1,
        0, -2,
        0, -3,
        0, -4,
        0, -5,
        0, -6,
        0, -7,
    ),
    intArrayOf(
        1, 0,
        2, 0,
        3, 0,
        4, 0,
        5, 0,
        6, 0,
        7, 0,
    ),
    intArrayOf(
        -1, 0,
        -2, 0,
        -3, 0,
        -4, 0,
        -5, 0,
        -6, 0,
        -7, 0,
    ),
    intArrayOf(-1, 2),
    intArrayOf(-1, -2),
    intArrayOf(-2, 1),
    intArrayOf(-2, -1),
    intArrayOf(1, 2),
    intArrayOf(1, -2),
    intArrayOf(2, -1),
    intArrayOf(2, 1),

    intArrayOf(
        -1, -1,
        -2, -2,
        -3, -3,
        -4, -4,
        -5, -5,
        -6, -6,
        -7, -7,
    ),

    intArrayOf(
        1, 1,
        2, 2,
        3, 3,
        4, 4,
        5, 5,
        6, 6,
        7, 7,
    ),

    intArrayOf(
        1, -1,
        2, -2,
        3, -3,
        4, -4,
        5, -5,
        6, -6,
        7, -7,
    ),

    intArrayOf(
        -1, 1,
        -2, 2,
        -3, 3,
        -4, 4,
        -5, 5,
        -6, 6,
        -7, 7,
    ),
)

internal val MOVES_FLANGER = arrayOf(
    intArrayOf(
        -1, 1,
        -2, 0,
        -3, 1,
        -4, 0,
        -5, 1,
        -6, 0,
        -7, 1,
    ),
    intArrayOf(
        -1, -1,
        -2, 0,
        -3, -1,
        -4, 0,
        -5, -1,
        -6, 0,
        -7, -1,
    ),
    intArrayOf(
        1, 1,
        2, 0,
        3, 1,
        4, 0,
        5, 1,
        6, 0,
        7, 1,
    ),
    intArrayOf(
        1, -1,
        2, 0,
        3, -1,
        4, 0,
        5, -1,
        6, 0,
        7, -1,
    ),

    intArrayOf(
        1, -1,
        0, -2,
        1, -3,
        0, -4,
        1, -5,
        0, -6,
        1, -7,
    ),
    intArrayOf(
        -1, -1,
        0, -2,
        -1, -3,
        0, -4,
        -1, -5,
        0, -6,
        -1, -7,
    ),
    intArrayOf(
        1, 1,
        0, 2,
        1, 3,
        0, 4,
        1, 5,
        0, 6,
        1, 7,
    ),
    intArrayOf(
        -1, 1,
        0, 2,
        -1, 3,
        0, 4,
        -1, 5,
        0, 6,
        -1, 7,
    ),
)

internal val MOVES_KING = arrayOf(
    intArrayOf(-1, -1),
    intArrayOf(-1, 0),
    intArrayOf(-1, 1),
    intArrayOf(0, -1),
    intArrayOf(0, 1),
    intArrayOf(1, -1),
    intArrayOf(1, 0),
    intArrayOf(1, 1),
)

internal val MOVES_RIDER = MOVES_HORSE + MOVES_PAWN