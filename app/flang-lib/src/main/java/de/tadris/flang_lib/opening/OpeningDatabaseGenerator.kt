package de.tadris.flang_lib.opening

import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.getNotationV2
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt

private const val ANALYSIS_DEPTH_MAX = 13
private const val ANALYSIS_DEPTH_MIN = 10
private const val MIN_PROBABILITY_THRESHOLD = 0.15 // Minimum probability to consider a line
private const val MAX_ITERATIONS = 1000 // Maximum number of expansion iterations
private const val DATABASE_FILE = "opening_database_$ANALYSIS_DEPTH_MAX.txt"
private const val DATABASE_FILE_COMPACT = "opening_database_${ANALYSIS_DEPTH_MAX}_compact.txt"

fun main(){
    println("Starting OpeningDatabaseGenerator with depth max $ANALYSIS_DEPTH_MAX")
    println("Configuration: MIN_PROBABILITY_THRESHOLD=$MIN_PROBABILITY_THRESHOLD, MAX_ITERATIONS=$MAX_ITERATIONS")

    val cacheMB = 4 * 1024
    val bot = CFlangEngine(1, ANALYSIS_DEPTH_MAX, ttSizeMB = cacheMB)
    println("Initialized FlangBot with ${cacheMB}MB transposition table")
    
    val database = mutableListOf<OpeningDatabaseEntry>()
    
    // Load existing database if available
    val databaseFile = File(DATABASE_FILE)
    if (databaseFile.exists()) {
        println("Loading existing database from $DATABASE_FILE")
        databaseFile.readLines().forEach { line ->
            if (line.isNotBlank()) {
                database.add(OpeningDatabaseEntry.fromFileString(line))
            }
        }
        println("Loaded ${database.size} entries from database")
    } else {
        println("Database file not found, starting with empty database")
    }

    fun save(){
        val analyzedCount = database.count { it.isAnalyzed }
        val unanalyzedCount = database.size - analyzedCount
        println("Database status: ${database.size} total entries ($analyzedCount analyzed, $unanalyzedCount pending)")
        println("Saving database to $DATABASE_FILE")
        databaseFile.writeText(database.joinToString("\n") { it.toFileString() })
        File(DATABASE_FILE_COMPACT).writeText(database.filter { it.isAnalyzed }.joinToString("\n") { it.toFileString() })
    }
    
    if(database.isEmpty()){
        println("Database empty, adding root node")
        database += OpeningDatabaseEntry("", -1, 1.0, emptyList())
    }

    println("Starting analysis iterations...")
    for(iteration in 0 until MAX_ITERATIONS){
        println("\n=== Iteration ${iteration + 1}/$MAX_ITERATIONS ===")
        val unanalyzedEntries = database.filter { !it.isAnalyzed && (it.totalProbability > MIN_PROBABILITY_THRESHOLD || it.game.currentState.moveNumber <= 1) }
        println("Found ${unanalyzedEntries.size} unanalyzed entries above threshold")

        val nextEntry = unanalyzedEntries.maxByOrNull { it.totalProbability }
        if(nextEntry == null){
            println("No more entries to analyze, stopping iterations")
            break
        }

        println("Selected entry with probability ${nextEntry.totalProbability} and FMN: '${nextEntry.fmn}'")
        if(nextEntry.game.currentState.gameIsComplete()){
            println("Game is complete, removing entry from database")
            database.remove(nextEntry)
            continue
        }

        val depth = (ANALYSIS_DEPTH_MAX + ln(nextEntry.totalProbability).roundToInt()).coerceAtLeast(ANALYSIS_DEPTH_MIN)
        println("Analyzing position using depth=$depth")
        val result = bot.findBestMoveWithFixedDepth(nextEntry.game.currentState, true, depth)
        nextEntry.evaluations = result.evaluations.map { it.move to it.evaluation }
        nextEntry.depth = depth

        println("Analysis complete. Found ${result.evaluations.size} candidate moves:")
        result.evaluations.take(3).forEach { eval ->
            println("  ${eval.move.getNotationV2(nextEntry.game.currentState)}: ${eval.evaluation}")
        }
        if (result.evaluations.size > 3) {
            println("  ... and ${result.evaluations.size - 3} more moves")
        }

        var newEntriesAdded = 0
        result.evaluations.forEach { evaluation ->
            val game = nextEntry.game.copy()
            game.execute(evaluation.move)
            val fmnAfterMove = game.getFMNv2()
            if(fmnAfterMove == "e2e3 e6 g2g3"){
                throw Exception("Here we go again")
            }
            if(database.none { it.fmn == fmnAfterMove }){
                val centipawnLoss = (evaluation.evaluation - result.bestMove.evaluation).absoluteValue
                val myProbability = if(centipawnLoss == 0.0) 1.0 else min(1.0, 6 / centipawnLoss)
                val newProbability = nextEntry.totalProbability * myProbability * 0.5

                database.add(
                    OpeningDatabaseEntry(
                    fmnAfterMove,
                    -1,
                    newProbability,
                    emptyList()
                )
                )
                newEntriesAdded++
            }
        }
        println("Added $newEntriesAdded new positions to database")

        if(iteration % 10 == 0){
            save()
        }
    }

    save()

    // Final summary
    println("\n=== Final Summary ===")
    val finalAnalyzedCount = database.count { it.isAnalyzed }
    val finalUnanalyzedCount = database.size - finalAnalyzedCount
    val maxDepth = database.filter { it.isAnalyzed }.maxOf { it.game.currentState.moveNumber + 1 }
    
    println("Database generation complete!")
    println("Total entries: ${database.size}")
    println("Max depth: $maxDepth")
    println("Analyzed entries: $finalAnalyzedCount")
    println("Unanalyzed entries: $finalUnanalyzedCount")
    println("Database saved to: $DATABASE_FILE")
}