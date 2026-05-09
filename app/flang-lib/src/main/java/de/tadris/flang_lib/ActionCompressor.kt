package de.tadris.flang_lib

// TODO rewrite with fast board
/*
object ActionCompressor {

    val positions = (('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('+', '-'))
    val positionsReversed = positions.mapIndexed { index, char -> char to index }.toMap()

    fun compress(board: Board) = buildString {
        append("!")
        board.moveList.forEachGameState { board, action ->
            if(action == null) return@forEachGameState
            when(action){
                is Move -> {
                    val possibleActions = board.findAllMoves(board.atMove).toSorted()
                    if(possibleActions.size <= positions.size){
                        append(positions[possibleActions.indexOf(action)])
                    }else{
                        append(positions[action.piece.location.fast()])
                        append(positions[action.target.fast()])
                    }
                }
                is Resign -> {
                    append(action.toString())
                }
            }
        }
    }

    fun uncompress(input: String): Board {
        if(input.first() != '!') throw Exception("???")
        val actionList = ActionList()
        var idx = 1
        while (idx < input.length){
            val char = input[idx]
            val moves = actionList.board.findAllMoves(actionList.board.atMove).toSorted()
            when {
                char == '#' -> {
                    val second = input[++idx]
                    val combined = char.toString() + second
                    actionList.addMove(Resign(combined))
                }
                moves.size > positions.size -> {
                    val second = input[++idx]
                    val fromIdx = positionsReversed[char]!!
                    val toIdx = positionsReversed[second]!!
                    val piece = actionList.board.getAt(fromIdx * 2)!!
                    actionList.addMove(Move(piece, Vector(toIdx)))
                }
                else -> {
                    val movedIdx = positionsReversed[char]!!
                    actionList.addMove(moves[movedIdx])
                }
            }

            idx++
        }

        return actionList.board
    }

    private fun List<Move>.toSorted() = sortedBy { it.piece.location.index * 64 + it.target.index }

}

fun main() {
    var sumFMN1 = 0
    var sumFMN2 = 0
    var sumCompressed = 0
    var total = 0
    File("doc/games2.txt").forEachLine { fmn1 ->
        val board = Board.fromFMN(fmn1)
        val fmn2 = board.getFMN2()
        val out = ActionCompressor.compress(board)
        // println(fmn1)
        // println(fmn2)
        // println(out)
        // println("=======================================")

        sumFMN1 += fmn1.length
        sumFMN2 += fmn2.length
        sumCompressed += out.length
        total++

        val fmnRe = ActionCompressor.uncompress(out).getFMN2()
        if(fmn2 != fmnRe) throw Exception("FMN doesnt match: $fmn2")
    }

    println("\n")
    val avgFMN1 = sumFMN1.toDouble() / total
    val avgFMN2 = sumFMN2.toDouble() / total
    val avgCompressed = sumCompressed.toDouble() / total
    val savingsPercent = (1 - (avgCompressed / avgFMN2)) * 100
    println("FMN1 avg: $avgFMN1")
    println("FMN2 avg: $avgFMN2")
    println("Compressed avg: $avgCompressed")
    println("Savings (compared to FMNv2): -${savingsPercent.roundToInt()}%")
    println("Positions: ${ActionCompressor.positions.size}")
}*/