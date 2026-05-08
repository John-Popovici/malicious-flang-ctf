package de.tadris.flang_lib.analysis

data class AnalysisResult(
    val fmn: String,
    val moves: List<MoveInfo>,
    val whiteAccuracy: PlayerAccuracy,
    val blackAccuracy: PlayerAccuracy,
    val analysisDepth: Int,
    val nodesAnalyzed: Long,
    val analysisTimeMs: Long
) {
    val totalMoves: Int get() = moves.size
    
    val whiteMoves: List<MoveInfo> get() = moves.filter { it.isWhiteMove }
    
    val blackMoves: List<MoveInfo> get() = moves.filter { !it.isWhiteMove }
    
    val errorMoves: List<MoveInfo> get() = moves.filter { it.isError }
    
    val whiteErrors: List<MoveInfo> get() = whiteMoves.filter { it.isError }
    
    val blackErrors: List<MoveInfo> get() = blackMoves.filter { it.isError }
    
    fun getMoveAt(ply: Int): MoveInfo? = moves.find { it.ply == ply }
    
    fun getMovesAfter(ply: Int): List<MoveInfo> = moves.filter { it.ply > ply }
    
    fun getMovesBefore(ply: Int): List<MoveInfo> = moves.filter { it.ply < ply }
}