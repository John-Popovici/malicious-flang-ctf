package de.tadris.flang.util

import android.content.Context
import de.tadris.flang.R
import de.tadris.flang_lib.Game
import kotlin.concurrent.thread

class Positions(val context: Context) {

    @Volatile
    private var isLoaded = false

    private val positions = mutableMapOf<String, String>()

    init {
        thread {
            loadFile()
            isLoaded = true
        }
    }

    private fun loadFile(){
        context.resources.openRawResource(R.raw.positions)
            .bufferedReader()
            .readLines()
            .forEach { readLine(it) }
    }

    private fun readLine(line: String){
        if(line.isEmpty() || line.startsWith("#")) return
        val data = line.split(":")
        positions[data[1].trim()] = data[0].trim()
    }

    fun findPositionName(game: Game): String? {
        val copy = game.copy()
        while (copy.moveList.isNotEmpty()){
            copy.rewind()
            findCurrentPositionName(copy)?.let { return it }
        }
        return null
    }

    fun findCurrentPositionName(game: Game) = findPositionName(game.getFMNv2())

    private fun findPositionName(fbn: String) = if(isLoaded) positions[fbn] else null

}