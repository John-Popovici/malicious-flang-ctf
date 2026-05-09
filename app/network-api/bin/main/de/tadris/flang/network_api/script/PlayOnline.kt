package de.tadris.flang.network_api.script

import de.tadris.flang.network_api.FlangAPI
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.getNotationV2
import java.io.File

fun main() {
    val api = FlangAPI("www.tadris.de", 443, "api/flang", useSSL = true, loggingEnabled = false)
    val credentials = File("doc/auth_neo.txt").readText().split(":")
    val username = credentials[0]
    api.login(username, credentials[1])

    var gameId: Long
    while (true){
        println("Waiting for opponent...")
        try {
            val result = api.requestGame(15000, GameConfiguration(true, false, 15*60000, -1, false, 0))
            gameId = result.gameId
            println("-> Found game: $gameId")
            break
        }catch (e: Exception){
            println("-> ${e.message}")
        }
    }

    val engine = CFlangEngine(
        minDepth = 1,
        maxDepth = 20,
        cflangPath = "./cflang/cflang_nnue",
        threads = 24,
        ttSizeMB = 2048,
        useLME = true,
        lmeMaxExtension = 3,
        onlyFindBest = true
    )

    var moves = -1
    var retries = 0
    var gameRunning = true

    while (gameRunning && retries < 3){
        try {
            val info = api.getGameInfo(gameId, moves, 5000)
            gameRunning = info.running
            moves = info.moves
            val myColor = info.white.username == username
            val myInfo = if(myColor) info.white else info.black
            val game = info.toGame()
            if(info.running && game.currentState.atMove == myColor){
                // My move
                val time = estimateTimeForMove(myInfo.time)
                println("Got game: ${game.getFMNv2()}")
                println("Board: ${game.currentState.getFBN2()}")
                val result = engine.findBestMoveIterative(game.currentState, true, time)
                val bestMove = result.bestMove.move
                api.executeMove(gameId, bestMove)
                println("Executing move: ${bestMove.getNotationV2(game.currentState)} (eval: ${result.bestMove.evaluation})")
            }
        }catch (e: Exception){
            e.printStackTrace()
            Thread.sleep(1000)
            retries++
        }
    }
}

private fun estimateTimeForMove(totalTimeLeft: Long) = when {
    totalTimeLeft < 10_000 -> 100L
    totalTimeLeft < 30_000 -> 1000L
    totalTimeLeft < 60_000 -> 3000L
    totalTimeLeft < 120_000 -> 7_000L
    totalTimeLeft < 300_000 -> 15_000L
    else -> 20_000L
}