package de.tadris.flang.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import de.tadris.flang.R
import de.tadris.flang.audio.AudioController
import de.tadris.flang.game.GameController
import de.tadris.flang.game.TutorialGameController
import de.tadris.flang.game.TutorialInfo
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Move


class TutorialGameFragment : GameFragment() {

    companion object {
        const val ARGUMENT_INDEX = "index"
    }

    lateinit var tutorial: TutorialInfo

    var conditionTrue = false

    override fun onCreate(savedInstanceState: Bundle?) {
        tutorial = TutorialInfo.findByIndex(arguments?.getInt(ARGUMENT_INDEX, 0) ?: 0)
        if(!tutorial.clickable){
            isBoardDisabled = true
        }
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        onUpdate()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        binding.resignButton.visibility = View.GONE
        binding.analysisButton.visibility = View.GONE
        binding.shareButton.visibility = View.GONE
        binding.backButton.visibility = View.GONE
        binding.forwardButton.visibility = View.GONE
        binding.swapSidesButton.visibility = View.GONE

        binding.abstractButton.visibility = View.VISIBLE
        binding.abstractButton.setOnClickListener {
            next()
        }

        binding.hintButton.visibility = if(tutorial.hintsEnabled) View.VISIBLE else View.GONE

        showDialog()

        return v
    }

    private fun showDialog(){
        AlertDialog.Builder(activity)
                .setTitle(tutorial.title)
                .setMessage(tutorial.description)
                .setPositiveButton(R.string.okay, null)
                .show()
    }

    private fun next(){
        checkCondition()
        if(conditionTrue){
            if(tutorial.finish){
                findNavController().navigateUp()
            }else{
                val bundle = Bundle()
                bundle.putInt(ARGUMENT_INDEX, tutorial.index + 1)
                findNavController().navigate(R.id.action_nav_tutorial_self, bundle)
            }
        }else{
            Toast.makeText(context, tutorial.goal.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCondition(){
        conditionTrue = tutorial.goal.condition(gameBoard.currentState)
    }

    override fun onUpdate(action: Move) {
        checkCondition()
        val oldConditionState = conditionTrue
        super.onUpdate(action)
        onUpdate()
        checkCondition()
        if(!oldConditionState && conditionTrue){
            AudioController.getInstance(requireContext()).playSound(AudioController.SOUND_ENERGY)
        }
    }

    override fun onUpdate(gameInfo: GameInfo) {
        super.onUpdate(gameInfo)
        onUpdate()
    }

    private fun onUpdate() {
        if(!tutorial.freezeEnabled){
            displayedBoard.currentState.unfreezeOnBoard(COLOR_WHITE)
            displayedBoard.currentState.unfreezeOnBoard(COLOR_BLACK)
            gameBoard.currentState.unfreezeOnBoard(COLOR_WHITE)
            gameBoard.currentState.unfreezeOnBoard(COLOR_BLACK)
            refreshBoardView()
        }
        if(!tutorial.botTurns){
            gameBoard.currentState.atMove = COLOR_WHITE
            displayedBoard.currentState.atMove = COLOR_WHITE
            refreshBoardView()
        }
        checkCondition()
        if(conditionTrue){
            binding.abstractButton.isPressed = true
        }
    }

    override fun createGameController(): GameController {
        return TutorialGameController(tutorial, requireActivity())
    }

    override fun getNavigationLinkToAnalysis() = -1
    override fun getNavigationLinkToChat() = -1
}