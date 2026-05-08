package de.tadris.flang.game

import android.app.Activity
import android.util.Log
import de.tadris.flang.R
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.GamePlayerInfo
import de.tadris.flang.network_api.model.Puzzle
import de.tadris.flang.network_api.model.UserInfo
import de.tadris.flang.ui.dialog.LoadingDialogViewController
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getNotationV1
import de.tadris.flang_lib.parseMove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PuzzleGameController(activity: Activity, private val puzzleCallback: PuzzleCallback) : AbstractGameController(activity) {
    
    companion object {
        private const val TAG = "PuzzleGameController"
    }
    
    private var puzzleList = mutableListOf<Puzzle>()
    private var me = UserInfo("", 0f, false, "")
    val currentPuzzle get() = puzzleList.first()
    private var expectedMoves: List<Move> = emptyList()
    private var moveIndex = 0
    private var isProcessingMove = false
    private var puzzleFailed = false
    private var loadingDialog: LoadingDialogViewController? = null
    private var newRating: Float? = null
    private var customPuzzleMode = false

    override fun requestGame() {
        // Show loading dialog
        loadingDialog = LoadingDialogViewController(activity).apply {
            setText(activity.getString(R.string.loadingPuzzles))
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (puzzleList.isEmpty()) {
                    val puzzleResult = DataRepository.getInstance().accessRestrictedAPI(activity).getPuzzles()
                    puzzleList.addAll(puzzleResult.puzzles)
                    me = puzzleResult.me
                }
                
                withContext(Dispatchers.Main) {
                    loadingDialog?.hide()
                    loadingDialog = null
                }
                
                if (puzzleList.isNotEmpty()) {
                    loadFirstPuzzle()
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onGameRequestFailed(activity.getString(R.string.noPuzzlesAvailable))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load puzzles", e)
                withContext(Dispatchers.Main) {
                    loadingDialog?.hide()
                    loadingDialog = null
                    callback.onGameRequestFailed(activity.getString(R.string.failedToLoadPuzzles, e.message ?: ""))
                }
            }
        }
    }
    
    private suspend fun loadFirstPuzzle() {
        Log.d(TAG, "Loading puzzle ${currentPuzzle.id}: startFMN=${currentPuzzle.startFMN}, puzzleFMN=${currentPuzzle.puzzleFMN}")
        
        try {
            val fullGame = Game.fromFMN(currentPuzzle.startFMN)
            
            // Get the board state one move before the starting position
            val previousGame = if (fullGame.moveList.isNotEmpty()) {
                val tempBoard = fullGame.copy()
                tempBoard.rewind()
                tempBoard
            } else {
                fullGame.copy()
            }
            
            // Parse expected moves from puzzleFMN
            expectedMoves = parsePuzzleMoves()
            moveIndex = 0
            isProcessingMove = true // Disable input during initial animation
            puzzleFailed = false

            Log.d(TAG, "Loaded ${expectedMoves.size} expected moves")
            
            withContext(Dispatchers.Main) {
                val gameInfo = createFakeGameInfo(currentPuzzle, previousGame)
                callback.onGameRequestSuccess(
                    info = gameInfo,
                    isParticipant = true,
                    color = fullGame.currentState.atMove,
                    game = previousGame
                )
                

                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    
                    if (fullGame.moveList.isNotEmpty()) {
                        val lastMove = fullGame.moveList[fullGame.moveList.size - 1]
                        Log.d(TAG, "Playing intro move: $lastMove")
                        callback.onUpdate(lastMove)
                    }
                    
                    // Re-enable input after showing the last move
                    Log.d(TAG, "Re-enabling input after intro animation")
                    isProcessingMove = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load puzzle ${currentPuzzle.id}", e)
            withContext(Dispatchers.Main) {
                callback.onGameRequestFailed(activity.getString(R.string.failedToLoadPuzzle, e.message ?: ""))
            }
        }
    }

    private fun parsePuzzleMoves(): List<Move> {
        val moves = mutableListOf<Move>()
        val tempBoard = Game.fromFMN(currentPuzzle.startFMN)
        try {
            val moveStrings = currentPuzzle.puzzleFMN.split(" ").filter { it.isNotBlank() }
            for (moveString in moveStrings) {
                val move = parseMove(tempBoard.currentState, moveString)
                moves.add(move)
                tempBoard.execute(move)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse puzzle moves: ${currentPuzzle.puzzleFMN}", e)
        }

        return moves
    }

    private fun createFakeGameInfo(puzzle: Puzzle, board: Game): GameInfo {
        val me = GamePlayerInfo(me.username, me.rating, 0L, 0f, me.isBot, me.title)
        val played = activity.getString(R.string.puzzleTitlePlayed, puzzle.views)
        val puzzleInfo = GamePlayerInfo("Puzzle #${puzzle.id}\n$played", puzzle.elo.toFloat(), 0L, 0f, false, "")

        val start = Game.fromFMN(puzzle.startFMN).currentState
        val white = if(start.atMove) me else puzzleInfo
        val black = if(start.atMove) puzzleInfo else me

        return GameInfo(
            gameId = -1,
            white = white,
            black = black,
            configuration = GameConfiguration(isRated = true, infiniteTime = true, time = 0L),
            running = true,
            moves = board.currentState.moveNumber,
            lastAction = System.currentTimeMillis(),
            spectatorCount = 0,
            fmn = board.getFMNv1(),
            won = 0,
        )
    }
    
    override fun onMoveRequested(move: Move, newBoardRequest: Game?, onCancel: (() -> Unit)?) {
        Log.d(TAG, "Move requested: ${move.getNotationV1()}, isProcessingMove: $isProcessingMove")
        
        if (isProcessingMove) {
            Log.d(TAG, "Move rejected - already processing")
            return
        }

        val expectedMove = expectedMoves.getOrNull(moveIndex)
        
        Log.d(TAG, "Expected moves count: ${expectedMoves.size}, current moveIndex: $moveIndex")
        Log.d(TAG, "Expected move: ${expectedMove?.getNotationV1() ?: "null"}")
        
        if (expectedMove == null) {
            Log.d(TAG, "No expected move at index $moveIndex")
            return
        }
        
        isProcessingMove = true
        
        // Always accept the move first (show it on board immediately)
        callback.onUpdate(move)
        
        // Check if it's the correct move after 500ms
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            isProcessingMove = false
            
            val isCorrect = isMoveCorrect(move, expectedMove)
            Log.d(TAG, "Move ${move.getNotationV1()} vs ${expectedMove.getNotationV1()}: $isCorrect")
            
            if (isCorrect) {
                handleCorrectMove()
            } else {
                handleIncorrectMove()
            }
        }
    }
    
    private fun isMoveCorrect(actualMove: Move, expectedMove: Move): Boolean {
        return actualMove.getNotationV1() == expectedMove.getNotationV1()
    }

    private fun handleCorrectMove() {
        puzzleCallback.notifyMoveCorrectness(correct = true)
        moveIndex++
        
        if (moveIndex < expectedMoves.size) {
            // There are more moves in this puzzle - apply opponent's response
            val opponentMove = expectedMoves[moveIndex]
            callback.onUpdate(opponentMove)
            moveIndex++
        } else { // puzzle solved
            CoroutineScope(Dispatchers.Main).launch {
                if(!puzzleFailed){
                    sendSolvePuzzle(solved = true)
                }
                notifyPuzzleCompleted()
            }
        }
    }
    
    private fun handleIncorrectMove() {
        if(!puzzleFailed){ // hasn't send fail yet
            puzzleFailed = true
            CoroutineScope(Dispatchers.IO).launch {
                sendSolvePuzzle(solved = false)
            }
        }

        puzzleCallback.notifyMoveCorrectness(correct = false)

        // Revert the move
        try {
            Log.d(TAG, "Reverting move")

            val currentBoard = getCurrentlyExpectedBoard()
            val fakeGameInfo = createFakeGameInfo(currentPuzzle, currentBoard)
            callback.onGameRequestSuccess(
                info = fakeGameInfo,
                isParticipant = true,
                color = currentBoard.currentState.atMove,
                game = currentBoard
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to revert move", e)
        }
        
        isProcessingMove = false
    }
    
    private fun notifyPuzzleCompleted() {
        callback.onUpdate(createFakeGameInfo(currentPuzzle, getCurrentlyExpectedBoard()))
        puzzleCallback.notifyPuzzleCompleted(!puzzleFailed)
    }

    private fun getCurrentlyExpectedBoard(): Game {
        val currentBoard = Game.fromFMN(currentPuzzle.startFMN)
        repeat(moveIndex){ currentBoard.execute(expectedMoves[it]) }
        return currentBoard
    }
    
    fun isPuzzleSolved(): Boolean {
        return moveIndex >= expectedMoves.size && !isProcessingMove
    }
    
    fun getRatingChange(): Triple<Float, Float, Float> {
        val newRating = newRating ?: return Triple(me.rating, me.rating, 0f)
        return Triple(me.rating, newRating, newRating - me.rating)
    }
    
    fun getCurrentUserRating(): Float = me.rating

    fun isCustomPuzzleMode(): Boolean = customPuzzleMode

    fun resetToNormalMode() {
        customPuzzleMode = false
    }
    
    fun nextPuzzle() {
        if (customPuzzleMode) {
            // In custom puzzle mode, don't load next puzzle automatically
            return
        }

        // Remove the current puzzle (first element) and move to next
        if (puzzleList.isNotEmpty()) {
            puzzleList.removeAt(0)
        }

        // Update me with new rating
        if(newRating != null){
            me = UserInfo(me.username, newRating!!, me.isBot, me.title)
            newRating = null
        }

        requestGame()
    }
    
    fun sendRatePuzzle(rating: Int) {
        if (customPuzzleMode) {
            // In custom puzzle mode, don't send rating to server
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                DataRepository.getInstance().accessRestrictedAPI(activity).ratePuzzle(currentPuzzle.id, rating)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rate puzzle ${currentPuzzle.id}", e)
            }
        }
    }
    
    suspend fun sendSolvePuzzle(solved: Boolean) {
        if (customPuzzleMode) {
            // In custom puzzle mode, don't send solve/fail to server or update rating
            return
        }

        withContext(Dispatchers.IO){
            try {
                val result = DataRepository.getInstance().accessRestrictedAPI(activity).solvePuzzle(currentPuzzle.id, solved)
                newRating = result.newRating
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark puzzle ${currentPuzzle.id} as solved", e)
            }
        }
    }
    
    override fun resignGame() { }
    
    override fun isCreativeGame(): Boolean = false

    suspend fun loadPuzzleById(puzzleId: Long) {
        customPuzzleMode = true

        val puzzle = withContext(Dispatchers.IO) {
            DataRepository.getInstance().accessRestrictedAPI(activity).getPuzzleById(puzzleId)
        }

        // Clear existing puzzles and add the custom one
        puzzleList.clear()
        puzzleList.add(puzzle)

        loadFirstPuzzle()
    }

    interface PuzzleCallback {

        fun notifyMoveCorrectness(correct: Boolean)

        fun notifyPuzzleCompleted(correct: Boolean)

    }
}