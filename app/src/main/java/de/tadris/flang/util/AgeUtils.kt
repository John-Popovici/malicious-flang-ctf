package de.tadris.flang.util

import android.content.Context
import de.tadris.flang.R
import java.util.concurrent.TimeUnit

object AgeUtils {

    private val YEAR = TimeUnit.DAYS.toMillis(365)
    private val MONTH = TimeUnit.DAYS.toMillis(30)
    private val WEEK = TimeUnit.DAYS.toMillis(7)
    private val DAY = TimeUnit.DAYS.toMillis(1)
    private val HOUR = TimeUnit.HOURS.toMillis(1)
    private val MINUTE = TimeUnit.MINUTES.toMillis(1)

    fun getAgeString(context: Context, age: Long): String {
        return when {
            age > YEAR -> {
                val years = (age / YEAR).toInt()
                context.resources.getQuantityString(R.plurals.yearsAgo, years, years)
            }
            age > MONTH -> {
                val months = (age / MONTH).toInt()
                context.resources.getQuantityString(R.plurals.monthsAgo, months, months)
            }
            age > WEEK -> {
                val weeks = (age / WEEK).toInt()
                context.resources.getQuantityString(R.plurals.weeksAgo, weeks, weeks)
            }
            age > DAY -> {
                val days = (age / DAY).toInt()
                context.resources.getQuantityString(R.plurals.daysAgo, days, days)
            }
            age > HOUR -> {
                val hours = (age / HOUR).toInt()
                context.resources.getQuantityString(R.plurals.hoursAgo, hours, hours)
            }
            age > MINUTE -> {
                val minutes = (age / MINUTE).toInt()
                context.resources.getQuantityString(R.plurals.minutesAgo, minutes, minutes)
            }
            else -> {
                context.resources.getString(R.string.momentsAgo)
            }
        }
    }

}