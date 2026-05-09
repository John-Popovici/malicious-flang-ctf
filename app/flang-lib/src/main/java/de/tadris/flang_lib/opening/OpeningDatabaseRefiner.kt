package de.tadris.flang_lib.opening

import de.tadris.flang_lib.bot.CFlangEngine
import java.io.File
import java.util.concurrent.Executors

private const val FILE_INPUT = "opening_database_13_compact.txt"
private const val FILE_OUTPUT = "opening_database_13_only_best.txt"
private const val DEPTH = 13

fun main(){
    val cacheMB = 2 * 1024
    println("Initialized FlangBot with ${cacheMB}MB transposition table")

    val mappedEntries = mutableListOf<OpeningDatabaseEntry>()
    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val db = loadDatabase()
    db.forEach { entry ->
        executor.submit {
            val bot = CFlangEngine(1, DEPTH, ttSizeMB = cacheMB, threads = 1, onlyFindBest = true)
            val result = bot.findBestMoveWithFixedDepth(entry.game.currentState, true, DEPTH)
            synchronized(mappedEntries){
                mappedEntries.add(entry.copy(
                    evaluations = result.evaluations.map { it.move to it.evaluation }
                ))
                println("${mappedEntries.size}/${db.size}")
            }
        }
    }

    save(mappedEntries)
}

private fun loadDatabase(): List<OpeningDatabaseEntry> {
    val database = mutableListOf<OpeningDatabaseEntry>()

    // Load existing database if available
    val databaseFile = File(FILE_INPUT)
    if (databaseFile.exists()) {
        println("Loading existing database from $FILE_INPUT")
        databaseFile.readLines().forEach { line ->
            if (line.isNotBlank()) {
                database.add(OpeningDatabaseEntry.fromFileString(line))
            }
        }
        println("Loaded ${database.size} entries from database")
    } else {
        println("Database file not found, starting with empty database")
    }

    return database
}

private fun save(database: List<OpeningDatabaseEntry>){
    val analyzedCount = database.count { it.isAnalyzed }
    val unanalyzedCount = database.size - analyzedCount
    println("Database status: ${database.size} total entries ($analyzedCount analyzed, $unanalyzedCount pending)")
    println("Saving database to $FILE_OUTPUT")
    File(FILE_OUTPUT).writeText(database.joinToString("\n") { it.toFileString() })
}