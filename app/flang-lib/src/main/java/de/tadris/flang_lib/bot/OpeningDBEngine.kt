package de.tadris.flang_lib.bot

import de.tadris.flang_lib.Board
import de.tadris.flang_lib.opening.DefaultOpeningDatabase

class OpeningDBEngine(private val searchEngine: Engine): Engine {

    override fun findBestMove(board: Board, printTime: Boolean): BotResult {
        DefaultOpeningDatabase.db.query(board)?.let { return it }
        return searchEngine.findBestMove(board, printTime)
    }

    override fun findBestMoveWithFixedDepth(board: Board, printTime: Boolean, depth: Int): BotResult {
        DefaultOpeningDatabase.db.query(board, depth)?.let { return it }
        return searchEngine.findBestMoveWithFixedDepth(board, printTime, depth)
    }

    override fun findBestMoveIterative(board: Board, printTime: Boolean, maxTimeMs: Long): BotResult {
        DefaultOpeningDatabase.db.query(board)?.let { return it }
        return searchEngine.findBestMoveIterative(board, printTime, maxTimeMs)
    }

    override fun destroy() {
        searchEngine.destroy()
    }
}