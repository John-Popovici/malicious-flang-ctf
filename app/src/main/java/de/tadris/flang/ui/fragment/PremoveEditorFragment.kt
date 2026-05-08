package de.tadris.flang.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.Premove
import de.tadris.flang.ui.adapter.PremoveAdapter
import de.tadris.flang.ui.board.BoardMoveDetector
import de.tadris.flang.ui.dialog.AddPremoveConfirmationBottomSheet
import de.tadris.flang.util.ConditionalPremoves
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.Color
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFromPieceState
import de.tadris.flang_lib.getNotationV2
import kotlinx.coroutines.launch

class PremoveEditorFragment : AbstractAnalysisGameFragment(), BoardMoveDetector.MoveListener {

    companion object {
        const val ARGUMENT_GAME_ID = "gameId"
        const val ARGUMENT_MY_COLOR = "myColor"

        fun createArguments(gameId: Long, myColor: Color, fmn: String, fbn: String?, flipped: Boolean): Bundle {
            return Bundle().apply {
                putLong(ARGUMENT_GAME_ID, gameId)
                putBoolean(ARGUMENT_MY_COLOR, myColor)
                putString(ARGUMENT_BOARD_FMN, fmn)
                putBoolean(ARGUMENT_RUNNING_GAME, true)
                putBoolean(ARGUMENT_FLIPPED, true)
                fbn?.let { putString(ARGUMENT_BOARD_FBN, it) }
            }
        }
    }

    private lateinit var premovesRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView

    private lateinit var conditionalPremoves: ConditionalPremoves
    private lateinit var premoveAdapter: PremoveAdapter
    private lateinit var addPremoveBottomSheet: AddPremoveConfirmationBottomSheet

    private var gameId: Long = 0
    private var myColor: Color? = null
    private var currentPremoves = emptyList<Premove>()
    private lateinit var referenceBoard: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        referenceBoard = firstBoard.copy()
        gameId = arguments?.getLong(ARGUMENT_GAME_ID) ?: 0
        myColor = arguments?.getBoolean(ARGUMENT_MY_COLOR)

        addPremoveBottomSheet = AddPremoveConfirmationBottomSheet(requireContext())

        conditionalPremoves = ConditionalPremoves(
            context = requireContext(),
            gameId = gameId,
            gameFMN = referenceBoard.getFMNv1(),
            gameMoveCount = referenceBoard.currentState.moveNumber,
        ) { premoves ->
            currentPremoves = premoves
            premoveAdapter.updatePremoves(premoves)
            updateEmptyState()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        binding.gameAdditionalContentParent.visibility = View.VISIBLE
        binding.hintButton.visibility = View.GONE
        binding.positionName.visibility = View.GONE
        binding.analysisButton.visibility = View.GONE

        val premoveView = inflater.inflate(R.layout.view_premove_editor, binding.gameAdditionalContentParent, true)

        premovesRecyclerView = premoveView.findViewById(R.id.premovesRecyclerView)
        emptyStateText = premoveView.findViewById(R.id.emptyStateText)

        setupRecyclerView()

        // Load existing premoves
        lifecycleScope.launch {
            try {
                conditionalPremoves.fetchPremoves()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        premoveAdapter = PremoveAdapter(referenceBoard, currentPremoves) { premove ->
            deletePremove(premove)
        }

        premovesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = premoveAdapter
        }
    }

    private fun updateEmptyState() {
        if (currentPremoves.isEmpty()) {
            premovesRecyclerView.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
        } else {
            premovesRecyclerView.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
        }
    }

    private fun deletePremove(premove: Premove) {
        lifecycleScope.launch {
            try {
                conditionalPremoves.removePremove(premove)
            } catch (e: Exception) {
                e.printStackTrace()
                // Could show error message to user
            }
        }
    }

    override fun onMoveRequested(move: Move) {
        // Only allow moves for the user's color
        if (move.getFromPieceState().getColor() == myColor) {
            val moveCount = displayedBoard.moveList.size
            val fmnCondition = displayedBoard.getFMNv1()

            val displayedCondition = fmnCondition.replaceFirst(referenceBoard.getFMNv1(), "").trim()

            val message = getString(R.string.confirmAddPremoveWithCondition, move.getNotationV2(displayedBoard.currentState), moveCount + 1, displayedCondition)

            addPremoveBottomSheet.show(
                move = move,
                moveCount = moveCount,
                customMessage = message,
                onConfirm = {
                    addPremove(move, moveCount, fmnCondition)
                }
            )
        }

        super.onMoveRequested(move)
    }

    private fun addPremove(move: Move, moveCount: Int, fmnCondition: String?) {
        lifecycleScope.launch {
            try {
                val premove = Premove(
                    id = 0, // Will be set by the API
                    moveCount = moveCount,
                    move = move,
                    fmnCondition = fmnCondition
                )
                conditionalPremoves.addPremove(premove)
            } catch (e: Exception) {
                e.printStackTrace()
                // Could show error message to user
            }
        }
    }

    override fun isGameClickable() = true

    override fun getNavigationLinkToAnalysis() = R.id.action_nav_premove_editor_to_nav_analysis
    override fun getNavigationLinkToChat() = R.id.action_nav_premove_editor_to_nav_chat

    override fun onDisplayedBoardChange(board: Board) { }
}