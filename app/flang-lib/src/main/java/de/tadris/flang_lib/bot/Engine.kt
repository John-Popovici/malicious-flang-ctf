package de.tadris.flang_lib.bot

import de.tadris.flang_lib.Board

interface Engine {

    fun findBestMove(board: Board, printTime: Boolean = true): BotResult

    fun findBestMoveWithFixedDepth(
        board: Board,
        printTime: Boolean,
        depth: Int
    ) = findBestMove(board, printTime)

    fun findBestMoveIterative(
        board: Board,
        printTime: Boolean = true,
        maxTimeMs: Long = Long.MAX_VALUE
    ) = findBestMove(board, printTime)

    fun destroy(){ }

}