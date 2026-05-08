package de.tadris.flang.ui.dialog

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.NavigationRes
import androidx.navigation.findNavController
import de.tadris.flang.R
import de.tadris.flang.network_api.model.GameRequestResult
import de.tadris.flang.ui.fragment.OnlineGameFragment
import kotlin.concurrent.thread


abstract class GameRequestDialog(protected val context: Activity, @IdRes private val navigation: Int, private val isRepeating: Boolean = true) {

    private var requestRunning = true

    private val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.requestingGame)
            .setMessage(R.string.waitingForOpponent)
            .setNegativeButton(R.string.actionCancel, null)
            .setCancelable(false)
            .show()

    init {
        if(isRepeating){
            val button: Button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE)
            button.setOnClickListener {
                button.isEnabled = false
                onCancelRequested()
            }
        }

        thread {
            var first = true
            while (requestRunning && (isRepeating || first)){
                first = false
                try{
                    val result = tryRequest()
                    requestRunning = false
                    context.runOnUiThread { onComplete(result) }
                }catch (e: Exception){
                    e.printStackTrace()
                    if(!onError(e)){
                        context.runOnUiThread {
                            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                        }
                        requestRunning = false
                    }
                }
                Thread.sleep(500)
            }
            context.runOnUiThread { dialog.cancel() }
        }
    }

    protected abstract fun tryRequest(): GameRequestResult

    protected abstract fun onError(e: java.lang.Exception): Boolean

    private fun onCancelRequested(){
        dialog.setMessage(context.getString(R.string.cancellingRequest))
        requestRunning = false
    }

    private fun onComplete(result: GameRequestResult){
        val bundle = Bundle()
        bundle.putLong(OnlineGameFragment.EXTRA_GAME_ID, result.gameId)
        context.findNavController(R.id.nav_host_fragment).navigate(
            navigation,
            bundle
        )
    }

}