package de.tadris.flang_lib

import de.tadris.flang_lib.utils.TimeUtils

class GameClock(val color: Color) {

    private var timeLeft: Long = 0
    private var timestamp: Long = 0

    fun setTimeLeft(timeLeft: Long){
        this.timeLeft = timeLeft
        this.timestamp = System.currentTimeMillis()
    }

    fun getTimeLeft(atMove: Boolean): Long {
        return if(atMove){
            timeLeft - (System.currentTimeMillis() - timestamp)
        }else{
            timeLeft
        }
    }

    fun toString(atMove: Boolean): String {
        return TimeUtils.getTimeAsString(getTimeLeft(atMove))
    }

}