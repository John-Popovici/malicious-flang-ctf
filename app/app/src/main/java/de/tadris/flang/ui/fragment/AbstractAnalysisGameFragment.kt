package de.tadris.flang.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.tadris.flang.game.AnalysisGameController
import de.tadris.flang.game.GameController
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Board

abstract class AbstractAnalysisGameFragment : GameFragment(), GameFragment.BoardChangeListener {

    companion object {
        const val ARGUMENT_BOARD_FMN = "fmn"
        const val ARGUMENT_BOARD_FBN = "fbn"
        const val ARGUMENT_RUNNING_GAME = "running"
        const val ARGUMENT_FLIPPED = "flipped"
    }

    protected lateinit var fmn: String
    protected var running: Boolean = false
    protected lateinit var firstBoard: Game
    protected var isFlippedByDefault: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        fmn = arguments?.getString(ARGUMENT_BOARD_FMN) ?: ""
        running = arguments?.getBoolean(ARGUMENT_RUNNING_GAME) ?: false
        arguments?.getString(ARGUMENT_BOARD_FBN)?.let {
            baseBoard = Board.fromFBN(it)
        }
        firstBoard = Game.fromFMN(fmn, baseBoard)
        isFlippedByDefault = arguments?.getBoolean(ARGUMENT_FLIPPED) ?: false
        registerBoardChangeListener(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        boardView.setFlipped(isFlippedByDefault)

        binding.player1InfoParent.visibility = View.GONE
        binding.player2InfoParent.visibility = View.GONE
        binding.player1TimeParent.visibility = View.GONE
        binding.player2TimeParent.visibility = View.GONE

        binding.resignButton.visibility = View.GONE // Hide resign button because it's useless

        return view
    }

    override fun createGameController(): GameController {
        return AnalysisGameController(requireActivity(), firstBoard, isGameClickable())
    }

    abstract fun isGameClickable(): Boolean

}