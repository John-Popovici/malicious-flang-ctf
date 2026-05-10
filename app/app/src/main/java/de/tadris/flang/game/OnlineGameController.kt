package de.tadris.flang.game

import android.app.Activity
import android.widget.Toast
import de.tadris.flang.network.CredentialsStorage
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.Premove
import de.tadris.flang.ui.dialog.LoadingDialogViewController
import de.tadris.flang.ui.dialog.SendMoveConfirmationBottomSheet
import de.tadris.flang.util.PremoveStorage
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Color
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getNotationV1
import java.lang.Exception
import kotlin.concurrent.thread

open class OnlineGameController(activity: Activity, gameId: Long) : AbstractGameController(activity) {

    var gameId = gameId
        protected set

    private var moves = 0
    private var game = Game()
    private var gameFinished = false
    protected var fragmentPaused = false
    private var currentGameInfo: GameInfo? = null
    private var isParticipant = false
    private val premoveStorage = PremoveStorage()

    override fun requestGame() {
        val dialog = LoadingDialogViewController(activity)
        thread {
            try {
                initGame()
                startGameRefreshThread()
                loadPremoves()
            } catch (e: Exception){
                e.printStackTrace()
                showError(e.message)
                Thread.sleep(3000)
                activity.runOnUiThread {
                    requestGame()
                }
            } finally {
                dialog.hide()
            }
        }
    }

    private fun initGame(){
        val info = DataRepository.getInstance().accessOpenAPI().getGameInfo(gameId)
        currentGameInfo = info
        val username = CredentialsStorage(activity).getUsername()
        val isWhite = username == info.white.username
        val isBlack = username == info.black.username
        val color: Color? = if(isWhite && isBlack) null else if(isWhite) COLOR_WHITE else COLOR_BLACK
        game = info.toGame()
        moves = game.moveList.size
        isParticipant = isWhite || isBlack
        activity.runOnUiThread {
            callback.onGameRequestSuccess(info, isParticipant, color)
        }
    }

    private fun startGameRefreshThread(){
        fragmentPaused = false
        gameFinished = false
        thread {
            while (!game.currentState.gameIsComplete() && !fragmentPaused && !gameFinished){
                try{
                    val info = DataRepository.getInstance().accessOpenAPI().getGameInfo(gameId, moves)
                    if(!info.running){
                        gameFinished = true
                    }
                    if(fragmentPaused){
                        continue
                    }
                    currentGameInfo = info
                    activity.runOnUiThread {
                        callback.onUpdate(info)
                    }
                    game = info.toGame()
                    if(game.moveList.size >= moves){
                        game.moveList.subList(moves, game.moveList.size).forEach { action ->
                            activity.runOnUiThread {
                                callback.onUpdate(action)
                                onMoveExecuted()
                            }
                        }
                        moves = game.moveList.size
                    }else{
                        // we know more moves than the server, so we give him a bit time to keep up
                        Thread.sleep(200)
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                    showError(e.message)
                    Thread.sleep(5000)
                }
            }
        }
    }

    override fun stop() {
        fragmentPaused = true
    }

    override fun resume() {
        fragmentPaused = false
        startGameRefreshThread()
    }

    override fun resignGame() {
        asyncAction { DataRepository.getInstance().accessRestrictedAPI(activity).resign(gameId) }
    }

    override fun onMoveRequested(move: Move, newBoardRequest: Game?, onCancel: (() -> Unit)?) {
        // Check if this is a daily game
        currentGameInfo?.let { info ->
            if (info.configuration.isDailyGame()) {
                activity.runOnUiThread {
                    val confirmationDialog = SendMoveConfirmationBottomSheet(activity)
                    confirmationDialog.show(
                        onConfirm = {
                            executeMove(move)
                        },
                        onCancel = onCancel
                    )
                }
                return
            }
        }
        
        // For non-daily games, execute move immediately
        executeMove(move)
    }

    private fun loadPremoves(){
        if(!arePremovesAllowed()) return // dont load premoves if not allowed
        asyncAction {
            premoveStorage.onPremovesLoaded(DataRepository.getInstance().accessRestrictedAPI(activity).getPremoves(gameId))
            activity.runOnUiThread {
                callback.onVisiblePremoveChanged(premoveStorage.getVisiblePremove())
            }
        }
    }

    override fun onPremoveRequested(move: Premove) {
        if(!arePremovesAllowed()) return

        super.onPremoveRequested(move)
        asyncAction {
            try {
                val result = DataRepository.getInstance().accessRestrictedAPI(activity).addPremove(
                    gameId, move.moveCount, move.move.getNotationV1(), move.fmnCondition
                )
                premoveStorage.addPremove(move.copy(id = result.id))
            }catch (e: Exception){
                e.printStackTrace()
                activity.runOnUiThread {
                    callback.onVisiblePremoveChanged(null)
                }
            }
        }
        callback.onVisiblePremoveChanged(move)
    }

    private fun onMoveExecuted(){
        if(!arePremovesAllowed()) return

        if(premoveStorage.hasPremoves()){
            premoveStorage.clear()
            callback.onVisiblePremoveChanged(null)
        }
    }

    override fun onPremoveClearRequested() {
        if(!arePremovesAllowed()) return
        super.onPremoveClearRequested()
        premoveStorage.getVisiblePremove()?.let { premove ->
            asyncAction {
                DataRepository.getInstance().accessRestrictedAPI(activity).removePremove(premove.id)
            }
        }
        premoveStorage.clear()
        callback.onVisiblePremoveChanged(null)
    }

    override fun arePremovesAllowed(): Boolean {
        return isParticipant && currentGameInfo?.configuration?.isDailyGame() == false
    }
    
    private fun executeMove(move: Move) {
        asyncAction { DataRepository.getInstance().accessRestrictedAPI(activity).executeMove(gameId, move) }
    }

    private fun asyncAction(action: () -> Unit){
        thread {
            try{
                action()
            }catch(e: Exception){
                e.printStackTrace()
                showError(e.message)
            }
        }
    }

    protected fun showError(message: String?){
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun isCreativeGame() = false

}