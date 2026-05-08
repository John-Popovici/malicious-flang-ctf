package de.tadris.flang.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.tadris.flang.R
import de.tadris.flang.game.GameController
import de.tadris.flang.game.OnlineGameController
import de.tadris.flang.network.CredentialsStorage
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.ui.dialog.GameEndDialog
import de.tadris.flang.ui.dialog.GameRequestAddDialog
import de.tadris.flang.util.GameCache
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnlineGameFragment : GameFragment(), GameEndDialog.GameEndDialogCallback {

    companion object {
        const val EXTRA_GAME_ID = "gameId"
    }

    private var isParticipant = false
    private var myColor: Color? = null
    private val gameCache by lazy { GameCache.getInstance(requireContext()) }
    
    override fun createGameController(): GameController {
        return OnlineGameController(requireActivity(), requireArguments().getLong(EXTRA_GAME_ID))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        root.findViewById<View>(R.id.player1InfoParent).setOnClickListener {
            if(lastGameInfo != null){
                showProfile(player1ViewController.color)
            }
        }
        root.findViewById<View>(R.id.player2InfoParent).setOnClickListener {
            if(lastGameInfo != null){
                showProfile(player2ViewController.color)
            }
        }

        setupPremoveEditorButton()

        return root
    }
    
    override fun onGameRequestSuccess(info: GameInfo, isParticipant: Boolean, color: Color?, board: Game?) {
        super.onGameRequestSuccess(info, isParticipant, color, board)

        myColor = color
        this.isParticipant = isParticipant
        val gameId = requireArguments().getLong(EXTRA_GAME_ID)

        if (isParticipant) {
            if (info.running) {
                gameCache.addGame(gameId)
            } else if (gameCache.isGameCached(gameId)) {
                showGameEndDialog()
            }
            updatePremoveEditorButton()
        }
    }

    override fun onUpdate(gameInfo: GameInfo) {
        super.onUpdate(gameInfo)
        // Update premove editor button when game status changes
        if (isParticipant) {
            updatePremoveEditorButton()
        }
    }

    private fun setupPremoveEditorButton() {
        binding.hintButton.setOnClickListener {
            openPremoveEditor()
        }
    }

    private fun updatePremoveEditorButton() {
        val gameInfo = lastGameInfo
        if (gameInfo != null && gameInfo.configuration.isDailyGame() && gameInfo.running) {
            binding.hintButton.setImageResource(R.drawable.ic_premoves)
            binding.hintButton.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val premoves = withContext(Dispatchers.IO){ DataRepository.getInstance().accessRestrictedAPI(requireContext()).getPremoves(gameInfo.gameId) }
                    if(premoves.isNotEmpty()) binding.hintButton.setImageResource(R.drawable.ic_premoves_active)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        } else {
            binding.hintButton.visibility = View.GONE
        }
    }

    private fun openPremoveEditor() {
        val gameInfo = lastGameInfo ?: return
        val myColor = myColor ?: return

        val gameId = requireArguments().getLong(EXTRA_GAME_ID)
        val fmn = gameInfo.fmn
        val fbn = null // We can use null for the base board in most cases

        val bundle = PremoveEditorFragment.createArguments(gameId, myColor, fmn, fbn, boardView.isFlipped())
        findNavController().navigate(R.id.action_nav_game_to_nav_premove_editor, bundle)
    }

    private fun showProfile(color: Color){
        showProfile(if(color) lastGameInfo!!.white.username else lastGameInfo!!.black.username)
    }

    private fun showProfile(username: String){
        val bundle = Bundle()
        bundle.putString(ProfileFragment.ARGUMENT_USERNAME, username)
        findNavController().navigate(R.id.action_nav_game_to_nav_profile, bundle)
    }

    override fun getNavigationLinkToAnalysis() = R.id.action_nav_game_to_nav_analysis
    override fun getNavigationLinkToChat() = R.id.action_nav_game_to_nav_chat
    
    override fun onGameCompleted() {
        super.onGameCompleted()
        showGameEndDialog()
    }

    private fun showGameEndDialog(){
        val credentialsStorage = CredentialsStorage(requireContext())
        val username = credentialsStorage.getUsername()
        val gameInfo = lastGameInfo

        if (username.isNotEmpty() && gameInfo != null) {
            val isWhitePlayer = username == gameInfo.white.username
            val isBlackPlayer = username == gameInfo.black.username

            if (isWhitePlayer || isBlackPlayer) {
                val gameId = requireArguments().getLong(EXTRA_GAME_ID)
                gameCache.removeGame(gameId)
                
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(1500)
                    GameEndDialog(
                        context = requireContext(),
                        gameInfo = gameInfo,
                        game = gameBoard,
                        endReason = determineGameEndReason(),
                        isWhitePlayer = isWhitePlayer,
                        isBlackPlayer = isBlackPlayer,
                        callback = this@OnlineGameFragment
                    ).show()
                }
            }
        }
    }

    override fun onAnalysisRequested(fmn: String) {
        val bundle = Bundle()
        bundle.putString("fmn", fmn)
        findNavController().navigate(R.id.action_nav_game_to_nav_computer_analysis, bundle)
    }
    
    override fun onNewGameRequested(configuration: GameConfiguration) {
        requestNewGame(configuration)
    }
    
    private fun requestNewGame(configuration: GameConfiguration) {
        GameRequestAddDialog(requireActivity(), R.id.action_nav_game_to_nav_game, configuration)
    }
}