package de.tadris.flang.bot

data class NativeBotResult(
    val bestMoveString: String,
    val evaluation: Double,
    val depth: Int,
    val totalEvaluations: Long,
    val allMoveStrings: Array<String>,
    val allEvaluations: DoubleArray,
    val allDepths: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NativeBotResult

        if (bestMoveString != other.bestMoveString) return false
        if (evaluation != other.evaluation) return false
        if (depth != other.depth) return false
        if (totalEvaluations != other.totalEvaluations) return false
        if (!allMoveStrings.contentEquals(other.allMoveStrings)) return false
        if (!allEvaluations.contentEquals(other.allEvaluations)) return false
        if (!allDepths.contentEquals(other.allDepths)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bestMoveString.hashCode()
        result = 31 * result + evaluation.hashCode()
        result = 31 * result + depth
        result = 31 * result + totalEvaluations.hashCode()
        result = 31 * result + allMoveStrings.contentHashCode()
        result = 31 * result + allEvaluations.contentHashCode()
        result = 31 * result + allDepths.contentHashCode()
        return result
    }
}