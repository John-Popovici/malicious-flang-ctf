package de.tadris.flang_lib.analysis

interface AnalysisListener {
    
    fun onAnalysisStarted(totalMoves: Int) {}
    
    fun onMoveAnalyzed(currentMove: Int, totalMoves: Int, moveInfo: MoveInfo?) {}
    
    fun onAnalysisCompleted(result: AnalysisResult) {}

}