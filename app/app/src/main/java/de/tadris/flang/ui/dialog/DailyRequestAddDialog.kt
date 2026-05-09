package de.tadris.flang.ui.dialog

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.findNavController
import de.tadris.flang.R
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameRequestResult
import de.tadris.flang.ui.fragment.OnlineGameFragment
import de.tadris.flang.network_api.exception.ForbiddenException
import kotlin.concurrent.thread

class DailyRequestAddDialog(private val context: Activity, private val configuration: GameConfiguration) {

    private val dialog = AlertDialog.Builder(context)
        .setTitle(R.string.requestingGame)
        .setMessage(R.string.creatingDailyGameRequest)
        .setCancelable(false)
        .show()

    init {
        thread {
            try {
                val result = DataRepository.getInstance().createDailyGameRequest(
                    context,
                    configuration.isRated,
                    configuration.time,
                    configuration.ratingDiff
                )
                
                context.runOnUiThread {
                    dialog.dismiss()
                    
                    if (result.gameId > 0) {
                        // Game started immediately
                        Toast.makeText(context, R.string.dailyGameStarted, Toast.LENGTH_SHORT).show()
                        navigateToGame(result.gameId)
                    } else if (result.requestId != null) {
                        // Request created, waiting for opponent
                        Toast.makeText(context, R.string.dailyGameRequestCreated, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ForbiddenException) {
                e.printStackTrace()
                context.runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(context, R.string.tooManyActiveRequests, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                context.runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(context, context.getString(R.string.errorCreatingDailyGame, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToGame(gameId: Long) {
        val bundle = Bundle()
        bundle.putLong(OnlineGameFragment.EXTRA_GAME_ID, gameId)
        context.findNavController(R.id.nav_host_fragment).navigate(
            R.id.action_nav_home_to_nav_game,
            bundle
        )
    }
}