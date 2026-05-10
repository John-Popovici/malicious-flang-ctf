package de.tadris.flang.ui.dialog

import android.app.Activity
import android.app.AlertDialog
import android.preference.PreferenceManager
import android.view.View
import android.widget.ToggleButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.tadris.flang.R
import de.tadris.flang.network_api.model.GameConfiguration

class CustomGameConfigurationDialog(context: Activity, val listener: CustomGameConfigurationListener)  {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val dialog = AlertDialog.Builder(context)
        .setTitle(R.string.customGameTitle)
        .setView(R.layout.dialog_custom_game_configuration)
        .setPositiveButton(R.string.actionRequest) { _, _ -> listener.onChoose(getConfiguration()) }
        .setNegativeButton(R.string.actionCancel, null)
        .setOnDismissListener { savePrefs() }
        .show()

    private val liveTimeMap = mapOf(
        30 to R.id.chipTime30s,
        60 to R.id.chipTime1m,
        60*2 to R.id.chipTime2m,
        60*3 to R.id.chipTime3m,
        60*5 to R.id.chipTime5m,
        60*10 to R.id.chipTime10m,
        60*15 to R.id.chipTime15m,
        60*20 to R.id.chipTime20m,
        60*30 to R.id.chipTime30m,
        60*60 to R.id.chipTime1h,
    )

    private val dailyTimeMap = mapOf(
        86400 to R.id.chipTime1d,      // 1 day in seconds
        172800 to R.id.chipTime2d,      // 2 days in seconds
        259200 to R.id.chipTime3d,     // 3 days in seconds
        604800 to R.id.chipTime7d,     // 7 days in seconds
    )

    private val ratingMap = mapOf(
        100 to R.id.chipRating100,
        200 to R.id.chipRating200,
        300 to R.id.chipRating300,
        400 to R.id.chipRating400,
        500 to R.id.chipRating500,
        -1 to R.id.chipRatingInfinite,
    )

    private val incrementMap = mapOf(
        0 to R.id.chipIncrement0,
        1000 to R.id.chipIncrement1,
        2000 to R.id.chipIncrement2,
        3000 to R.id.chipIncrement3,
        5000 to R.id.chipIncrement5,
        10000 to R.id.chipIncrement10,
    )

    private val gameType: MaterialButtonToggleGroup = dialog.findViewById(R.id.customGameType)
    private val ratedMode: MaterialButtonToggleGroup = dialog.findViewById(R.id.customGameMode)
    private val timeChips: ChipGroup = dialog.findViewById(R.id.timeChips)
    private val incrementChips: ChipGroup = dialog.findViewById(R.id.incrementChips)
    private val incrementLabel: View = dialog.findViewById(R.id.incrementLabel)
    private val ratingChips: ChipGroup = dialog.findViewById(R.id.ratingChips)

    // Live time chips
    private val liveTimeChips = listOf(
        R.id.chipTime30s, R.id.chipTime1m, R.id.chipTime2m, R.id.chipTime3m,
        R.id.chipTime5m, R.id.chipTime10m, R.id.chipTime15m, R.id.chipTime20m,
        R.id.chipTime30m, R.id.chipTime1h
    )

    // Daily time chips
    private val dailyTimeChips = listOf(
        R.id.chipTime1d, R.id.chipTime3d, R.id.chipTime7d
    )

    init {
        // Initialize game type
        val isDaily = prefs.getBoolean("customIsDaily", false)
        gameType.check(if(isDaily) R.id.customGameTypeDaily else R.id.customGameTypeLive)

        // Initialize time based on game type
        val time = prefs.getInt("customTime", 5*60_000)
        if(isDaily) {
            val timeInSeconds = time / 1000
            timeChips.check(dailyTimeMap[timeInSeconds] ?: R.id.chipTime1d)
        } else {
            timeChips.check(liveTimeMap[time / 1000] ?: R.id.chipTime5m)
        }

        val ratingDiff = prefs.getInt("customRating", 300)
        ratingChips.check(ratingMap[ratingDiff] ?: R.id.chipRating300)

        val timeIncrement = prefs.getInt("customIncrement", 0)
        incrementChips.check(incrementMap[timeIncrement] ?: R.id.chipIncrement0)

        // Game type change listener
        gameType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if(isChecked) {
                val isDailySelected = checkedId == R.id.customGameTypeDaily
                updateTimeChipsVisibility(isDailySelected)
                if(isDailySelected) {
                    // Switch to daily game default
                    timeChips.check(R.id.chipTime1d)
                } else {
                    // Switch to live game default
                    timeChips.check(R.id.chipTime5m)
                }
            }
        }

        ratedMode.check(
            if(prefs.getBoolean("customRated", true)) R.id.customGameModeRated else R.id.customGameModeCasual
        )

        updateTimeChipsVisibility(isDaily)
    }

    private fun updateTimeChipsVisibility(isDailyGame: Boolean) {
        // Hide/show live time chips
        liveTimeChips.forEach { chipId ->
            dialog.findViewById<Chip>(chipId).visibility = if(isDailyGame) View.GONE else View.VISIBLE
        }

        // Hide/show daily time chips
        dailyTimeChips.forEach { chipId ->
            dialog.findViewById<Chip>(chipId).visibility = if(isDailyGame) View.VISIBLE else View.GONE
        }

        // Hide/show increment selection for live games only
        incrementLabel.visibility = if(isDailyGame) View.GONE else View.VISIBLE
        incrementChips.visibility = if(isDailyGame) View.GONE else View.VISIBLE
    }

    private fun savePrefs(){
        prefs.edit()
            .putInt("customTime", getTime())
            .putInt("customRating", getRatingDiff())
            .putInt("customIncrement", getTimeIncrement())
            .putBoolean("customRated", isRated())
            .putBoolean("customIsDaily", isDailyGame())
            .apply()
    }

    private fun isDailyGame() = gameType.checkedButtonId == R.id.customGameTypeDaily

    private fun isRated() = ratedMode.checkedButtonId == R.id.customGameModeRated

    private fun getTime(): Int {
        return if(isDailyGame()) {
            // For daily games, return time in milliseconds
            val timeInSeconds = dailyTimeMap.entries.find { it.value == timeChips.checkedChipId }?.key ?: 86400
            timeInSeconds * 1000
        } else {
            // For live games, return time in milliseconds
            (liveTimeMap.entries.find { it.value == timeChips.checkedChipId }?.key ?: 5*60) * 1000
        }
    }

    private fun getRatingDiff() = ratingMap.entries.find { it.value == ratingChips.checkedChipId }?.key ?: 300

    private fun getTimeIncrement() = incrementMap.entries.find { it.value == incrementChips.checkedChipId }?.key ?: 0

    private fun getConfiguration() = GameConfiguration(
        isRated(),
        isDailyGame() || getTime() == -1000,  // Daily games or infinite time
        kotlin.math.max(0, getTime()).toLong(),
        getRatingDiff(),
        false, // isBotRequest
        if(isDailyGame()) 0 else getTimeIncrement().toLong() // Only add increment for live games
    )

    interface CustomGameConfigurationListener {
        fun onChoose(configuration: GameConfiguration)
    }

}