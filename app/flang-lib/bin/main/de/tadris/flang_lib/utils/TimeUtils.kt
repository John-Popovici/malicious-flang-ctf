package de.tadris.flang_lib.utils

import kotlin.math.max

object TimeUtils {

    fun getTimeAsString(time: Long): String {
        return if(time < (60 * 60 * 1000)){
            val timeLeft = max(time / 1000, 0)
            val min = timeLeft / 60
            val sec = timeLeft % 60
            "$min:${if(sec < 10) "0" else ""}$sec"
        } else getSmartTimeDisplay(time)
    }

    fun getSmartTimeDisplay(time: Long): String {
        val days = time / (24 * 60 * 60 * 1000)
        if (days >= 1) {
            val remainingTime = time % (24 * 60 * 60 * 1000)
            val hours = remainingTime / (60 * 60 * 1000)
            return if (hours > 0) "${days}d ${hours}h" else "${days}d"
        }

        if (time >= 60 * 60 * 1000) {
            val hours = time / (60 * 60 * 1000)
            return "${hours}h"
        }

        if (time >= 60 * 1000) {
            val minutes = time / (60 * 1000)
            return "${minutes}m"
        }

        val seconds = time / 1000
        return "${seconds}s"
    }

    fun getTimeControlDisplay(time: Long, increment: Long): String {
        if (increment == 0L) {
            // Use old format when no increment
            if (time == 30000L) {
                return "½ min"
            }
            val baseMinutes = time / (60 * 1000)
            return "$baseMinutes min"
        } else {
            // Use new format when there's an increment
            if (time == 30000L) {
                val incrementSeconds = increment / 1000
                return "½+$incrementSeconds"
            }
            val baseMinutes = time / (60 * 1000)
            val incrementSeconds = increment / 1000
            return "$baseMinutes+$incrementSeconds"
        }
    }

    enum class TimeControlZone(val displayName: String, val smallerThan: Long) {

        BULLET("Bullet", 60_000 * 3),
        BLITZ("Blitz", 60_000 * 10),
        RAPID("Rapid", 60_000 * 20),
        CLASSICAL("Classical", 60_000 * 60),
        DAILY("Daily", -1);

        companion object {
            fun getZone(infiniteTime: Boolean, time: Long) = when{
                infiniteTime -> DAILY
                time < BULLET.smallerThan -> BULLET
                time < BLITZ.smallerThan -> BLITZ
                time < RAPID.smallerThan -> RAPID
                else -> CLASSICAL
            }
        }

    }

}