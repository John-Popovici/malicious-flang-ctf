package de.tadris.flang.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.tadris.flang.R
import de.tadris.flang.game.PersistentAnalysisGameController
import de.tadris.flang.game.GameController

class PlayOverBoardFragment : GameFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        boardView.setBlackPiecesRotated(true)
        binding.resignButton.visibility = View.GONE

        return root
    }

    override fun createGameController(): GameController {
        return PersistentAnalysisGameController(requireActivity(), active = true)
    }

    override fun getNavigationLinkToAnalysis() = R.id.action_nav_play_over_board_to_nav_analysis
    override fun getNavigationLinkToChat() = R.id.action_nav_play_over_board_to_nav_chat

}