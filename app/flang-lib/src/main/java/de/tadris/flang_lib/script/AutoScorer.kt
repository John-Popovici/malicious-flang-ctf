package de.tadris.flang_lib.script

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.bot.Engine
import de.tadris.flang_lib.bot.fast.FastFlangBot
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

fun main(){
    println("=== Enhanced AutoScorer ===\n")

    println("Loading starting positions...")
    val startingPositions = File("doc/startingPos.txt")
        .readLines()
        .map { Game.fromFMN(it) }
        .take(200)  // Adjust based on desired test size
    println("Loaded ${startingPositions.size} starting positions\n")

    // Define bot configurations to test
    data class BotConfig(
        val name: String,
        val createEngine: () -> Engine
    )

    // Candidate bot (the one you're testing)
    val candidateBot = BotConfig(
        name = "Candidate",
        createEngine = {
            CFlangEngine(minDepth = 1, maxDepth = 10, threads = 1, cflangPath = "./cflang/cflang_nnue", useLME = true, lmeMaxExtension = 3)
        }
    )

    // Multiple baselines to test against
    val baselines = listOf(
        BotConfig(
            name = "Baseline",
            createEngine = {
                CFlangEngine(minDepth = 1, maxDepth = 10, threads = 1, cflangPath = "./cflang/cflang_nnue_v4", useLME = true, lmeMaxExtension = 3)
            }
        ),
    )

    // Multiple time controls
    val timeControls = listOf(2000L)

    // Statistics tracking
    data class Stats(
        var wins: Int = 0,
        var losses: Int = 0,
        var draws: Int = 0,
        var winsAsWhite: Int = 0,
        var winsAsBlack: Int = 0,
        var gameLengths: MutableList<Int> = mutableListOf(),
        var depths: MutableList<Int> = mutableListOf(),
        var evals: Long = 0L,
    ) {
        val totalGames get() = wins + losses + draws
        val winRate get() = if (totalGames > 0) wins.toDouble() / totalGames else 0.0
        val drawRate get() = if (totalGames > 0) draws.toDouble() / totalGames else 0.0
        val avgGameLength get() = if (gameLengths.isNotEmpty()) gameLengths.average() else 0.0
        val avgDepth get() = if (depths.isNotEmpty()) depths.average() else 0.0
    }

    data class MatchupResult(
        val candidateStats: Stats,
        val baselineStats: Stats,
        val baselineName: String,
        val timeControl: Long
    )

    val allResults = mutableListOf<MatchupResult>()

    fun playGame(
        board: Game,
        white: Engine,
        black: Engine,
        timePerMove: Long,
        candidateIsWhite: Boolean
    ): Triple<Boolean?, Int, List<Int>> {
        // Returns: (candidateWon?, gameLength, depthsReached)
        val depths = mutableListOf<Int>()

        try {
            while(!board.currentState.gameIsComplete()){
                val bot = if(board.currentState.atMove) white else black
                val moveEval = bot.findBestMoveIterative(board.currentState, false, maxTimeMs = timePerMove)
                depths.add(moveEval.bestMove.depth)
                board.execute(moveEval.bestMove.move)

                if(board.currentState.moveNumber > 150){
                    // Draw by move limit
                    return Triple(null, board.currentState.moveNumber, depths)
                }
            }

            val winner = board.currentState.getWinningColor()
            val candidateWon = when {
                winner == null -> null
                winner == candidateIsWhite -> true
                else -> false
            }
            println("\r${board.getFMNv2()}")
            return Triple(candidateWon, board.currentState.moveNumber, depths)

        } catch (e: Exception) {
            e.printStackTrace()
            return Triple(null, board.currentState.moveNumber, depths)
        }
    }

    fun testMatchup(
        candidateConfig: BotConfig,
        baselineConfig: BotConfig,
        positions: List<Game>,
        timePerMove: Long,
        threads: Int
    ): MatchupResult {
        val candidateStats = Stats()
        val baselineStats = Stats()
        val gamesPlayed = AtomicInteger(0)
        val totalGames = positions.size * 2

        val lock = Object()
        val executor = Executors.newFixedThreadPool(threads)

        positions.forEach { startPos ->
            executor.submit {
                try {
                    // Game 1: Candidate is White
                    val game1 = startPos.copy()
                    val result1 = playGame(
                        game1,
                        candidateConfig.createEngine(),
                        baselineConfig.createEngine(),
                        timePerMove,
                        candidateIsWhite = true
                    )

                    // Game 2: Candidate is Black
                    val game2 = startPos.copy()
                    val result2 = playGame(
                        game2,
                        baselineConfig.createEngine(),
                        candidateConfig.createEngine(),
                        timePerMove,
                        candidateIsWhite = false
                    )

                    synchronized(lock) {
                        // Process game 1
                        when (result1.first) {
                            true -> {
                                candidateStats.wins++
                                candidateStats.winsAsWhite++
                                baselineStats.losses++
                            }
                            false -> {
                                candidateStats.losses++
                                baselineStats.wins++
                                baselineStats.winsAsBlack++
                            }
                            null -> {
                                candidateStats.draws++
                                baselineStats.draws++
                            }
                        }
                        candidateStats.gameLengths.add(result1.second)
                        candidateStats.depths.addAll(result1.third)

                        // Process game 2
                        when (result2.first) {
                            true -> {
                                candidateStats.wins++
                                candidateStats.winsAsBlack++
                                baselineStats.losses++
                            }
                            false -> {
                                candidateStats.losses++
                                baselineStats.wins++
                                baselineStats.winsAsWhite++
                            }
                            null -> {
                                candidateStats.draws++
                                baselineStats.draws++
                            }
                        }
                        candidateStats.gameLengths.add(result2.second)
                        candidateStats.depths.addAll(result2.third)

                        gamesPlayed.addAndGet(2)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        print("  Testing ${baselineConfig.name} @ ${timePerMove}ms: ")
        while (gamesPlayed.get() < totalGames) {
            print("\r  Testing ${baselineConfig.name} @ ${timePerMove}ms: ${gamesPlayed.get()}/$totalGames games...")
            Thread.sleep(500)
        }
        println("\r  Testing ${baselineConfig.name} @ ${timePerMove}ms: ✓ Complete (${totalGames} games)")

        executor.shutdown()

        return MatchupResult(candidateStats, baselineStats, baselineConfig.name, timePerMove)
    }

    // Calculate Elo difference and confidence interval
    fun calculateElo(winRate: Double, totalGames: Int): Pair<Double, Double> {
        val clampedWinRate = winRate.coerceIn(0.01, 0.99)
        val eloDiff = -400 * log10(1.0 / clampedWinRate - 1.0)

        // 95% confidence interval
        val stderr = sqrt(clampedWinRate * (1 - clampedWinRate) / totalGames)
        val marginOfError = 1.96 * stderr * 800

        return Pair(eloDiff, marginOfError)
    }

    fun printResults(results: List<MatchupResult>) {
        println("\n" + "=".repeat(80))
        println("RESULTS SUMMARY")
        println("=".repeat(80))

        // Group by baseline
        val byBaseline = results.groupBy { it.baselineName }

        byBaseline.forEach { (baselineName, baselineResults) ->
            println("\n--- vs $baselineName ---")

            baselineResults.forEach { result ->
                val stats = result.candidateStats
                val totalGames = stats.totalGames

                println("\nTime Control: ${result.timeControl}ms/move")
                println("  Games: $totalGames (W: ${stats.wins}, L: ${stats.losses}, D: ${stats.draws})")
                println("  Win Rate: ${(stats.winRate * 100).roundToInt()}% " +
                        "(White: ${stats.winsAsWhite}/${totalGames/2}, Black: ${stats.winsAsBlack}/${totalGames/2})")
                println("  Draw Rate: ${(stats.drawRate * 100).roundToInt()}%")

                if (totalGames > 0) {
                    val (elo, margin) = calculateElo(stats.winRate, totalGames)
                    val sign = if (elo >= 0) "+" else ""
                    println("  Elo Difference: $sign${elo.roundToInt()} ± ${margin.roundToInt()}")

                    // Statistical significance
                    val zScore = abs(stats.winRate - 0.5) / sqrt(0.25 / totalGames)
                    val pValue = 2 * (1 - 0.5 * (1 + erf(zScore / sqrt(2.0))))
                    val significant = pValue < 0.05
                    println("  Significance: ${if (significant) "YES (p < 0.05)" else "NO (p = ${"%.3f".format(pValue)})"}")
                }

                println("  Avg Game Length: ${"%.1f".format(stats.avgGameLength)} moves")
                println("  Avg Depth: ${"%.2f".format(stats.avgDepth)}")
            }
        }

        // Overall summary across all time controls
        println("\n" + "=".repeat(80))
        println("OVERALL PERFORMANCE")
        println("=".repeat(80))

        byBaseline.forEach { (baselineName, baselineResults) ->
            val combinedStats = Stats()
            baselineResults.forEach { result ->
                combinedStats.wins += result.candidateStats.wins
                combinedStats.losses += result.candidateStats.losses
                combinedStats.draws += result.candidateStats.draws
                combinedStats.winsAsWhite += result.candidateStats.winsAsWhite
                combinedStats.winsAsBlack += result.candidateStats.winsAsBlack
                combinedStats.gameLengths.addAll(result.candidateStats.gameLengths)
                combinedStats.depths.addAll(result.candidateStats.depths)
            }

            val totalGames = combinedStats.totalGames
            val (elo, margin) = calculateElo(combinedStats.winRate, totalGames)
            val sign = if (elo >= 0) "+" else ""

            println("\nvs $baselineName (all time controls):")
            println("  Win Rate: ${(combinedStats.winRate * 100).roundToInt()}% ($totalGames games)")
            println("  Elo: $sign${elo.roundToInt()} ± ${margin.roundToInt()}")
        }

        println("\n" + "=".repeat(80))
    }

    // Run all tests
    println("\nStarting tests...")
    println("Candidate: ${candidateBot.name}")
    println("Baselines: ${baselines.joinToString(", ") { it.name }}")
    println("Time Controls: ${timeControls.joinToString(", ") { "${it}ms" }}")
    println("Positions: ${startingPositions.size} (${startingPositions.size * 2} games per matchup)")
    println()

    baselines.forEach { baseline ->
        timeControls.forEach { timeControl ->
            val result = testMatchup(
                candidateBot,
                baseline,
                startingPositions,
                timeControl,
                threads = 12
            )
            allResults.add(result)
        }
    }

    printResults(allResults)
}

// Error function approximation for p-value calculation
private fun erf(x: Double): Double {
    val sign = if (x >= 0) 1 else -1
    val absX = abs(x)

    val a1 = 0.254829592
    val a2 = -0.284496736
    val a3 = 1.421413741
    val a4 = -1.453152027
    val a5 = 1.061405429
    val p = 0.3275911

    val t = 1.0 / (1.0 + p * absX)
    val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)

    return sign * y
}