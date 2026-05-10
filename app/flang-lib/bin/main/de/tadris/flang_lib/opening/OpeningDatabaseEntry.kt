package de.tadris.flang_lib.opening

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getNotationV2
import de.tadris.flang_lib.parseMove

data class OpeningDatabaseEntry(
    val fmn: String,
    var depth: Int,
    val totalProbability: Double,
    var evaluations: List<Pair<Move, Double>>,
){

    val isAnalyzed get() = depth > 0

    val game get() = Game.fromFMN(fmn)

    fun toFileString(): String {
        val evalString = evaluations.joinToString(";") { "${it.first.getNotationV2(game.currentState)}:${it.second.toFloat()}" }
        return "$fmn|$depth|${totalProbability.toFloat()}|$evalString"
    }

    companion object {
        fun fromFileString(line: String): OpeningDatabaseEntry {
            val parts = line.split("|")
            val fmn = parts[0]
            val depth = parts[1].toInt()
            val totalProbability = parts[2].toDouble()
            val evaluations = if (parts[3].isEmpty()) emptyList() else {
                parts[3].split(";").map { eval ->
                    val (notation, score) = eval.split(":")
                    parseMove(Game.fromFMN(fmn).currentState, notation) to score.toDouble()
                }
            }
            return OpeningDatabaseEntry(fmn, depth, totalProbability, evaluations)
        }
    }

}