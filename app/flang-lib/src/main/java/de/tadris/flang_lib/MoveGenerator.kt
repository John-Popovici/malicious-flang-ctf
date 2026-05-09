package de.tadris.flang_lib

typealias MoveAction = (index: BoardIndex) -> Unit

class MoveBuffer(val capacity: Int = 200) {
    private val buffer = LongArray(capacity)
    private var count = 0
    
    fun clear() {
        count = 0
    }
    
    fun add(move: Move) {
        if (count >= capacity) {
            throw IllegalStateException("Move buffer overflow: $count >= $capacity")
        }
        buffer[count++] = move
    }

    fun get(index: Int) = buffer[index]
    
    fun toList(): List<Move> = buffer.take(count)

    fun size(): Int = count
}

class FastMoveGenerator(var board: Board, val includeOwnPieces: Boolean, val kingRange: Int = 1, val ignoreFreeze: Boolean = false) {

    fun getAllMoves(color: Color?): List<Move> {
        if(board.gameIsComplete()) return emptyList()

        return buildList {
            board.eachPiece(color){ index, piece ->
                forEachMove(index, piece){ move ->
                    add(move)
                }
            }
        }
    }
    
    fun loadMovesToBuffer(color: Color?, moveBuffer: MoveBuffer) {
        moveBuffer.clear()
        if(board.gameIsComplete()) return

        board.eachPiece(color){ index, piece ->
            forEachMove(index, piece){ move ->
                moveBuffer.add(move)
            }
        }
    }

    inline fun forEachMove(fromIndex: BoardIndex, state: PieceState, action: (Move) -> Unit){
        forEachTargetLocation(fromIndex, state){ toIndex ->
            action(packMove(fromIndex, toIndex, state, board.getAt(toIndex), board.getFrozenPieceIndex(state.getColor())))
        }
    }

    inline fun forEachTargetLocation(fromIndex: BoardIndex, state: PieceState, action: MoveAction) {
        if(!ignoreFreeze && state.getFrozen()) return
        when(state.getType()){
            TYPE_PAWN -> forEachMoveForPawn(fromIndex, state, action)
            TYPE_KING -> forEachKingMove(fromIndex, state, action)
            TYPE_RIDER -> forEachMoveFastRider(fromIndex, state, action)
            else -> forEachPossibleTargetLocation(fromIndex, state, action)
        }
    }

    inline fun forEachMoveFastRider(fromIndex: BoardIndex, state: PieceState, action: MoveAction){
        forEachMoveForPawn(fromIndex, state, action)
        forEachPossibleTargetLocation(fromIndex, packPieceState(TYPE_HORSE, state.getColor(), state.getFrozen()), action)
    }

    inline fun forEachMoveForPawn(fromIndex: BoardIndex, state: PieceState, action: MoveAction) {
        // Optimized method for pawns

        val color = state.getColor()
        val yDirection = color.evaluationNumber
        val x = fromIndex.x
        val y = fromIndex.y

        if(checkPawnTarget(x, y + yDirection, color)){
            action(indexOf(x, y + yDirection))
        }
        if(checkPawnTarget(x + 1, y + yDirection, color)){
            action(indexOf(x + 1, y + yDirection))
        }
        if(checkPawnTarget(x - 1, y + yDirection, color)){
            action(indexOf(x - 1, y + yDirection))
        }

        if(board.variant == Variant.NEXT){
            // pawn dash - active when there is another pawn in the front
            if(isValid(x, y + yDirection) && isWhite(x, y + yDirection) == color && board.getAt(x, y + yDirection).getType() == TYPE_PAWN){
                if(checkPawnTarget(x, y + yDirection * 2, color)){
                    action(indexOf(x, y + yDirection * 2))
                }
            }
        }
    }

    inline fun forEachKingMove(fromIndex: BoardIndex, state: PieceState, action: MoveAction) {
        // Optimized method for kings

        val x = fromIndex.x
        val y = fromIndex.y
        val color = state.getColor()

        for(dx in -kingRange..kingRange){
            for(dy in -kingRange..kingRange){
                if(dx == 0 && dy == 0) continue
                if(checkTarget(x + dx, y + dy, color)){
                    action(indexOf(x + dx, y + dy))
                }
            }
        }
    }

    val targets = IntArray(32){ -1 }
    var targetsSize = 0

    inline fun forEachPossibleTargetLocation(fromIndex: BoardIndex, state: PieceState, action: MoveAction) {
        val color = state.getColor()
        val type = state.getType()
        if(type.hasDoubleMoves) {
            for(i in targets.indices){
                if(targets[i] != -1){
                    targets[i] = -1
                } else break
            }
            targetsSize = 0
        }

        type.moves.forEach { batch ->
            forEachPossibleTargetLocation(fromIndex, color, batch) { toIndex ->
                var targetAlreadyCalled = false
                if(type.hasDoubleMoves){
                    for(i in 0..<targetsSize){
                        if(targets[i] == toIndex) {
                            targetAlreadyCalled = true
                            break
                        }
                    }
                }

                if(!targetAlreadyCalled){
                    if(type.hasDoubleMoves) {
                        targets[targetsSize] = toIndex
                        targetsSize++
                    }
                    action(toIndex)
                }
            }
        }
    }

    inline fun forEachPossibleTargetLocation(pieceIndex: BoardIndex, color: Color, batch: IntArray, action: MoveAction) {
        for(idx in 0..<(batch.size / 2)){
            val x = pieceIndex.x + batch[idx * 2]
            val y = pieceIndex.y + batch[idx * 2 + 1]
            if(checkTarget(x, y, color)){
                action(indexOf(x, y))
                if(!isEmpty(x, y)){
                    return
                }
            }else{
                return
            }
        }
    }

    fun checkPawnTarget(x: Int, y: Int, color: Color): Boolean {
        return isValid(x, y) && (includeOwnPieces || isEmpty(x, y) || (isWhite(x, y) != color) || (board.variant == Variant.NEXT && board.getAt(x, y).getType() == TYPE_HORSE))
    }

    /**
     * Checks if a given color piece can go to this target field
     */
    fun checkTarget(x: Int, y: Int, color: Color): Boolean {
        return isValid(x, y) && (includeOwnPieces || isEmpty(x, y) || (isWhite(x, y) != color))
    }

    fun isEmpty(x: Int, y: Int): Boolean {
        return board.getAt(x, y).getType() == TYPE_NONE
    }

    fun isWhite(x: Int, y: Int): Boolean {
        return board.getAt(x, y).getColor()
    }

    fun isValid(x: Int, y: Int) = x >= 0 && y >= 0 && x < Board.BOARD_SIZE && y < Board.BOARD_SIZE

}